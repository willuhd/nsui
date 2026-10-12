package nsui.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// `VersionGuardScanTest` — enforce the README's version-guard rule.
///
/// The rule: every accessor in `src/nsui` whose documentation names a macOS version of 11 or
/// later must check `respondsToSelector:` before sending, because this library declares no
/// minimum deployment target and an absent selector ABORTS the process (an ObjC exception from
/// an FFM downcall is uncatchable — measured, exit 134) rather than raising in Java.
///
/// Three checks, in increasing order of what they can see:
///
///  1. **Per-method scan** — a method whose own javadoc/comments name a version must have a guard
///     in its body. This is the bulk of the coverage (~87 annotated methods today).
///  2. **SECTION_GATED** — methods whose version is named ONLY in a section header
///     (`// ---- WritingTools (macOS 15+) ----`) or in a sibling's doc. The scanner cannot
///     attribute those automatically, so they are listed explicitly and each is asserted to have
///     a guard. Adding a version-gated method under such a header means adding it here; that is
///     the scan's known limitation.
///  3. **ALLOW** — prose that merely looks like a version. Every entry must be exercised by the
///     scan (asserted), so a stale suppression fails instead of silently exempting a method.
///
/// The guard test itself is a body-wide marker check (see GUARD_MARKERS), so it can be fooled by
/// one guarded send masking a second unguarded one in the same method. It is a regression net,
/// not a proof.
public final class VersionGuardScanTest {

    /// Methods whose macOS version appears only in a section header or a sibling's doc. Each must
    /// be guarded; the per-method scan is blind to them by construction.
    private static final String[] SECTION_GATED = {
        // NSTextView "// ---- WritingTools (macOS 15+) ----"
        "NSTextView.java:isWritingToolsActive",
        "NSTextView.java:writingToolsBehavior",
        "NSTextView.java:setWritingToolsBehavior",
        "NSTextView.java:allowedWritingToolsResultOptions",
        "NSTextView.java:setAllowedWritingToolsResultOptions",
        // NSPopUpButton's pull-down factories: 15.0 per NSPopUpButton.h, no version in their docs.
        "NSPopUpButton.java:pullDownButtonWithTitle",
        "NSPopUpButton.java:pullDownButtonWithImage",
        "NSPopUpButton.java:pullDownButtonWithTitleImage",
    };

    /// file:method — matched case-sensitively. Every entry must be HIT by the scan (asserted
    /// below), so a stale suppression fails instead of exempting a method forever. An earlier
    /// version carried three entries whose prose could not match VERSION at all — dead
    /// permissions, removed when the count assertion was added.
    private static final String[] ALLOW = {
        // Calls the guarded NSImage factory and falls back to imageNamed: when it returns nil,
        // so it is safe on a host without the SF-symbol selector.
        "NSStatusItem.java:setSFSymbol",
    };

    private static final Pattern VERSION = Pattern.compile(
            "(?:macOS\\s+(1[1-9]|2[0-9])|[(\\s](1[1-9]|2[0-9])\\.\\d+\\+?)");
    private static final Pattern METHOD = Pattern.compile(
            "^\\s*(?:public|protected)\\s+(?:static\\s+)?(?:final\\s+)?[\\w.<>\\[\\]]+\\s+(\\w+)\\s*\\(");

    /// A guard is any of the shared presence/validity validators or object checks. "require" is
    /// deliberately generic: the toolkit's helpers are requireImplemented, requireClassSelector,
    /// requireMutable, requireNotSelfOrAncestor and requireKeyEvent, and each refuses the call in
    /// Java before it can reach AppKit.
    private static final String[] GUARD_MARKERS = {
        "respondsToSelector", "respondsTo(", "responds(", "isKindOfClass", "newView(", "require",
    };

    public static void main(String[] args) throws Exception {
        System.out.println("=== VersionGuardScanTest — every version-annotated accessor is guarded ===");
        Path root = Path.of("src", "nsui");
        if (!Files.isDirectory(root)) TestKit.skip("src/nsui not found (run tests.sh from the repo root)");

        List<Path> files;
        try (Stream<Path> s = Files.list(root)) {
            files = s.filter(f -> f.getFileName().toString().endsWith(".java")).sorted().toList();
        }

        int candidates = 0;
        int allowed = 0;
        List<String> unguarded = new ArrayList<>();
        for (Path f : files) {
            String name = f.getFileName().toString();
            List<String> lines = Files.readAllLines(f);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.trim().startsWith("//") || !VERSION.matcher(line).find()) continue;
                // the contiguous doc block ending here, then the declaration it documents
                int j = i;
                while (j < lines.size() && lines.get(j).trim().startsWith("//")) j++;
                if (j >= lines.size()) continue;
                Matcher m = METHOD.matcher(lines.get(j));
                if (!m.find()) continue;
                candidates++;
                if (!hasGuard(lines, j)) {
                    if (isAllowed(name, m.group(1))) allowed++;
                    else unguarded.add(name + ":" + (j + 1) + " " + m.group(1) + "  <-- " + line.trim());
                }
                i = j - 1;
            }
        }
        for (String entry : SECTION_GATED) {
            int colon = entry.indexOf(':');
            String file = entry.substring(0, colon);
            String method = entry.substring(colon + 1);
            if (!guardedInFile(root.resolve(file), method)) unguarded.add(entry + "  <-- SECTION_GATED");
        }

        System.out.println("version-annotated methods: " + candidates
                + "  section-gated asserted: " + SECTION_GATED.length
                + "  allowlisted prose: " + allowed + " of " + ALLOW.length
                + "  unguarded: " + unguarded.size());
        for (String u : unguarded) System.out.println("  UNGUARDED " + u);

        TestKit.check(unguarded.isEmpty(),
                "every version-annotated accessor checks respondsToSelector: before sending"
                        + (unguarded.isEmpty() ? "" : " — offenders above; add the guard or an ALLOW entry with a reason"));
        // Every ALLOW entry must be exercised: a stale suppression would otherwise hide a method
        // forever, which is how three dead entries survived review.
        TestKit.check(allowed == ALLOW.length,
                "every ALLOW entry is exercised by the scan (" + allowed + " of " + ALLOW.length
                        + " — remove entries that never fire)");
        TestKit.end();
    }

    private static boolean guardedInFile(Path file, String method) throws Exception {
        List<String> lines = Files.readAllLines(file);
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = METHOD.matcher(lines.get(i));
            if (m.find() && m.group(1).equals(method)) return hasGuard(lines, i);
        }
        return false;
    }

    private static boolean hasGuard(List<String> lines, int declLine) {
        String body = bodyOf(lines, declLine);
        for (String marker : GUARD_MARKERS) {
            if (body.contains(marker)) return true;
        }
        return false;
    }

    /// The method body, by brace depth from the declaration line (the toolkit's formatting makes a
    /// naive count reliable; a stray brace in a string would only cause a false PASS via a longer
    /// body).
    private static String bodyOf(List<String> lines, int declLine) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        boolean started = false;
        for (int i = declLine; i < lines.size(); i++) {
            String l = lines.get(i);
            sb.append(l).append('\n');
            for (int c = 0; c < l.length(); c++) {
                char ch = l.charAt(c);
                if (ch == '{') { depth++; started = true; }
                else if (ch == '}') depth--;
            }
            if (started && depth <= 0) break;
        }
        return sb.toString();
    }

    private static boolean isAllowed(String file, String method) {
        for (String a : ALLOW) {
            int colon = a.indexOf(':');
            if (a.substring(0, colon).equals(file) && a.substring(colon + 1).equals(method)) return true;
        }
        return false;
    }
}
