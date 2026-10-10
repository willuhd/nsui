package nsui.tests;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/// Parallel JVM test runner (one OS process per test, pooled threads).
///
/// Every test stays a plain main() in its own JVM exactly as before, so a
/// native abort still fails only its own test. Threads here merely supervise
/// processes: launch, wait, file the exit code. Each test's output goes to
/// out/logs/<Test>.log; only launch lines and SKIP/FAIL lines print live.
/// Exit codes keep the tests.sh protocol: 0 pass, 2 setup-impossible skip,
/// anything else fail. Zero matched tests exits 1. Focus suites
/// (NSUI_FOCUS_TESTS=1) force one job: key/focus cannot be shared.
public final class RunSuite {

    private RunSuite() {}

    private static final class Outcome {
        final String name;
        final int code;
        Outcome(String name, int code) {
            this.name = name;
            this.code = code;
        }
    }

    public static void main(String[] args) throws Exception {
        String filter = "";
        String classesDir = "out/classes";
        String testsDir = "out/tests";
        String logsDir = "out/logs";
        int jobs = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("-j") || a.equals("--jobs")) {
                if (i + 1 >= args.length) {
                    System.out.println("missing count after " + a);
                    System.exit(1);
                    return;
                }
                i++;
                jobs = Integer.parseInt(args[i]);
            } else if (a.startsWith("--jobs=")) {
                jobs = Integer.parseInt(a.substring("--jobs=".length()));
            } else if (a.equals("--classes") && i + 1 < args.length) {
                i++;
                classesDir = args[i];
            } else if (a.equals("--tests") && i + 1 < args.length) {
                i++;
                testsDir = args[i];
            } else if (a.equals("--logs") && i + 1 < args.length) {
                i++;
                logsDir = args[i];
            } else if (!a.startsWith("-") && filter.isEmpty()) {
                filter = a;
            } else {
                System.out.println("unknown argument: " + a);
                System.exit(1);
                return;
            }
        }
        if (jobs < 1) {
            jobs = 1;
        }
        if ("1".equals(System.getenv("NSUI_FOCUS_TESTS"))) {
            System.out.println("NSUI_FOCUS_TESTS=1 forces one job: focus cannot be shared");
            jobs = 1;
        }

        List<String> tests = discover(testsDir, filter);
        if (tests.isEmpty()) {
            System.out.println("no test matched filter: " + filter);
            System.exit(1);
            return;
        }
        Files.createDirectories(Path.of(logsDir));
        System.out.println("running " + tests.size() + " tests on " + jobs + " jobs"
                + (filter.isEmpty() ? "" : " (filter " + filter + ")"));

        String javaBin = System.getProperty("java.home") + "/bin/java";
        String cp = classesDir + ":" + testsDir;
        List<String> extraFlags = List.of("-XstartOnFirstThread", "--enable-native-access=ALL-UNNAMED");
        String finalLogsDir = logsDir;
        ExecutorService pool = Executors.newFixedThreadPool(jobs);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (String t : tests) {
            System.out.println("== " + t);
            Callable<Outcome> task = () -> {
                Path log = Path.of(finalLogsDir, t + ".log");
                ProcessBuilder pb = new ProcessBuilder();
                List<String> cmd = new ArrayList<>();
                cmd.add(javaBin);
                cmd.addAll(extraFlags);
                cmd.add("-cp");
                cmd.add(cp);
                cmd.add("nsui.tests." + t);
                pb.command(cmd);
                pb.redirectOutput(log.toFile());
                pb.redirectErrorStream(true);
                int code;
                try {
                    code = pb.start().waitFor();
                } catch (IOException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    code = 1;
                }
                return new Outcome(t, code);
            };
            futures.add(pool.submit(task));
        }
        pool.shutdown();

        int passed = 0;
        int failed = 0;
        int skipped = 0;
        StringBuilder failedList = new StringBuilder();
        StringBuilder skippedList = new StringBuilder();
        for (Future<Outcome> f : futures) {
            Outcome o;
            try {
                o = f.get();
            } catch (Exception e) {
                System.out.println("FAIL: runner could not collect a result (" + e + ")");
                failed++;
                continue;
            }
            if (o.code == 2) {
                System.out.println("SKIP: " + o.name + " (setup impossible, unproven)");
                skipped++;
                skippedList.append(" ").append(o.name);
            } else if (o.code != 0) {
                System.out.println("FAIL: " + o.name + " exited with " + o.code);
                failed++;
                failedList.append(" ").append(o.name);
            } else {
                passed++;
            }
        }
        System.out.println("----------------------------------------");
        System.out.println("PASSED: " + passed + "  FAILED: " + failed + failedList
                + "  SKIPPED: " + skipped + skippedList);
        if (failed != 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
            return;
        }
        if (skipped != 0) {
            System.out.println("ALL RAN TESTS PASSED (" + skipped + " SKIPPED, UNPROVEN)");
            System.exit(0);
            return;
        }
        System.out.println("ALL TESTS PASSED");
    }

    /// Test names discovered from compiled classes: top-level *Test only.
    static List<String> discover(String testsDir, String filter) throws IOException {
        List<String> out = new ArrayList<>();
        Path root = Path.of(testsDir);
        if (!Files.isDirectory(root)) {
            return out;
        }
        String want = filter;
        try (java.util.stream.Stream<Path> s = Files.walk(root)) {
            s.filter(f -> f.getFileName() != null
                    && f.getFileName().toString().endsWith("Test.class")).forEach(p -> {
                String name = p.getFileName().toString();
                if (name.contains("$")) {
                    return;
                }
                String t = name.substring(0, name.length() - ".class".length());
                if (want.isEmpty() || t.contains(want)) {
                    out.add(t);
                }
            });
        }
        Collections.sort(out);
        return out;
    }
}
