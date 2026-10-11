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
/// This is the automated scan the README refers to. It is deliberately source-level: it catches
/// the two ways a guard gets forgotten here — a new accessor added without one, and a guard
/// added to a getter but not to its setter or overload twin (which is how seven sites survived
/// an earlier manual sweep). Because it keys on the version annotation ADJACENT to a method, it
/// cannot see a version documented only in a section header; the name-based twin scan in
/// AuditRegressionTest complements it, and neither replaces the other.
///
/// `ALLOW` lists prose that merely looks like a version. Each entry carries its reason, so a
/// new entry is a deliberate, reviewable decision rather than a silent suppression.
public final class VersionGuardScanTest {

    /// file:method — matched case-sensitively against the scanned candidate.
    private static final String[] ALLOW = {
        // "(10) / KeyUp (11)" are NSEventType numbers in prose, not a macOS version.
        "NSEvent.java:isKeyEvent",
        // "utilityWindow (16)" / "(16) to styleMask" are NSWindowStyleMask bits, not versions.
        "NSPanel.java:create",
        "NSWindow.java:createPanel",
        // Calls the guarded NSImage factory and falls back to imageNamed: when it returns nil,
        // so it is safe on a host without the SF-symbol selector.
        "NSStatusItem.java:setSFSymbol",
    };

    private static final Pattern VERSION = Pattern.compile(
            "(?:macOS\\s+(1[1-9]|2[0-9])|[(\\s](1[1-9]|2[0-9])\\.\\d+\\+?)");
    private static final Pattern METHOD = Pattern.compile(
            "^\\s*(?:public|protected)\\s+(?:static\\s+)?(?:final\\s+)?[\\w.<>\\[\\]]+\\s+(\\w+)\\s*\\(");
    private static final String[] GUARD_MARKERS = {
        "respondsToSelector", "respondsTo(", "responds(", "requireImplemented", "isKindOfClass",
        "newView(",
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
                boolean comment = line.trim().startsWith("//");
                if (!comment || !VERSION.matcher(line).find()) continue;
                // the contiguous doc block ending here, then the declaration it documents
                int j = i;
                while (j < lines.size() && lines.get(j).trim().startsWith("//")) j++;
                if (j >= lines.size()) continue;
                Matcher m = METHOD.matcher(lines.get(j));
                if (!m.find()) continue;
                String method = m.group(1);
                candidates++;
                String body = bodyOf(lines, j);
                boolean guarded = false;
                for (String marker : GUARD_MARKERS) {
                    if (body.contains(marker)) { guarded = true; break; }
                }
                if (guarded) continue;
                if (isAllowed(name, method)) { allowed++; continue; }
                unguarded.add(name + ":" + (j + 1) + " " + method + "  <-- " + line.trim());
            }
        }

        System.out.println("version-annotated methods: " + candidates
                + "  allowlisted prose: " + allowed
                + "  unguarded: " + unguarded.size());
        for (String u : unguarded) System.out.println("  UNGUARDED " + u);

        TestKit.check(unguarded.isEmpty(),
                "every version-annotated accessor checks respondsToSelector: before sending"
                        + (unguarded.isEmpty() ? "" : " — offenders above; add the guard or an ALLOW entry with a reason"));
        TestKit.end();
    }

    /// The method body, by brace depth from the declaration line (the toolkit's formatting makes a
    /// naive count reliable; a stray brace in a string would only cause a false PASS via a longer
    /// body, which the twin scan in AuditRegressionTest would still catch).
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
