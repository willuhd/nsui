package nsui.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// AssertionQualityTest - guards the suite against tautologies.
///
/// A check(true, ...) cannot fail: it inflates the assertion count without
/// testing anything. Every such site was converted to a real primitive
/// (noThrow / attempt / expectThrows / skipCase / probe), and this test fails
/// if a new one appears. It also prints the shape of the suite so the mix of
/// real assertions vs probes stays visible.
public final class AssertionQualityTest {

    private AssertionQualityTest() {}

    public static void main(String[] args) throws Exception {
        System.out.println("=== AssertionQualityTest - no tautologies, real assertions only ===");
        Path dir = Path.of("tests");
        if (!Files.isDirectory(dir)) TestKit.skip("tests/ not found (run from repo root)");
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(f -> f.getFileName().toString().endsWith(".java")).sorted().toList();
        }

        String tautology = "check(" + "true";
        // Always-true EXPRESSIONS (not just the literal check(true,...)): comparing the
        // same call against both true and false (or both null and non-null) can never fail.
        Pattern tautExpr = Pattern.compile(
                "([A-Za-z_][\\w.]*(?:\\([^)]*\\))?)\\s*(?:"
                + "==\\s*true\\s*\\|\\|\\s*\\1\\s*==\\s*false"
                + "|==\\s*false\\s*\\|\\|\\s*\\1\\s*==\\s*true"
                + "|==\\s*null\\s*\\|\\|\\s*\\1\\s*!=\\s*null"
                + "|!=\\s*null\\s*\\|\\|\\s*\\1\\s*==\\s*null)");
        int checks = 0, noThrow = 0, attempt = 0, expectThrows = 0, skipCase = 0, probe = 0;
        List<String> offenders = new ArrayList<>();
        for (Path p : files) {
            String code = mask(Files.readString(p));
            int t = count(code, tautology);
            Matcher te = tautExpr.matcher(code);
            StringBuilder locs = new StringBuilder();
            while (te.find()) {
                t++;
                if (locs.length() < 160) locs.append(" :").append(lineOf(code, te.start()));
            }
            if (t > 0) offenders.add(p.getFileName() + " x" + t + locs);
            checks += count(code, "check(");
            noThrow += count(code, "noThrow(");
            attempt += count(code, "attempt(");
            expectThrows += count(code, "expectThrows(");
            skipCase += count(code, "skipCase(");
            probe += count(code, "probe(");
        }
        int real = noThrow + attempt + expectThrows;
        System.out.println("suite shape: check=" + checks + " noThrow=" + noThrow + " attempt=" + attempt
                + " expectThrows=" + expectThrows + " probe=" + probe + " skipCase=" + skipCase);

        TestKit.check(offenders.isEmpty(),
                "no tautological always-true check remains" + (offenders.isEmpty() ? "" : " " + offenders));
        TestKit.check(real >= 250, "suite has real no-throw/negative assertions (got " + real + ")");
        TestKit.check(skipCase > 0, "environment-dependent cases are explicit skips (got " + skipCase + ")");
        TestKit.end();
    }

    private static int lineOf(String s, int idx) {
        int n = 1;
        for (int i = 0; i < idx && i < s.length(); i++) if (s.charAt(i) == '\n') n++;
        return n;
    }

    private static int count(String s, String needle) {
        int n = 0, i = 0;
        while ((i = s.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
        return n;
    }

    /// Blank out comments and string/char literals so counts ignore prose.
    private static String mask(String text) {
        char[] out = text.toCharArray();
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                int j = text.indexOf('\n', i);
                j = j < 0 ? n : j;
                for (int k = i; k < j; k++) if (out[k] != '\n') out[k] = ' ';
                i = j;
            } else if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                int j = text.indexOf("*/", i + 2);
                j = j < 0 ? n : j + 2;
                for (int k = i; k < j; k++) if (out[k] != '\n') out[k] = ' ';
                i = j;
            } else if (c == '"') {
                i++;
                while (i < n && text.charAt(i) != '"') { if (text.charAt(i) == '\\') i++; i++; }
                i++;
            } else if (c == '\'') {
                i++;
                while (i < n && text.charAt(i) != '\'') { if (text.charAt(i) == '\\') i++; i++; }
                i++;
            } else {
                i++;
            }
        }
        return new String(out);
    }
}
