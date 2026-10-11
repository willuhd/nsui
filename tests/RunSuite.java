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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/// Parallel JVM test runner (one OS process per test, pooled threads).
///
/// Every test stays a plain main() in its own JVM exactly as before, so a
/// native abort still fails only its own test. Threads here merely supervise
/// processes: launch, wait, file the exit code. Each test's output goes to
/// out/logs/<Test>.log; only launch lines and SKIP/FAIL lines print live.
/// Exit codes keep the tests.sh protocol: 0 pass, 2 setup-impossible skip,
/// anything else fail. Zero matched tests exits 1. Filter is a substring
/// match by design: filter "Window" runs every Window-prefixed suite.
/// Focus suites (NSUI_FOCUS_TESTS=1) force one job: key/focus cannot be shared.
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
        long timeoutSecs = -1;
        boolean strictSkips = false;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--strict-skips")) {
                // CI opt-in: turns a whole-test TestKit.skip() (exit 2) into a failure, so
                // "green" cannot hide a test that could not set up. It does NOT cover
                // skipCase()/probe()/PARTIAL PASS: those never change a test's exit code,
                // so they are reported (NOTE, below) rather than made fatal.
                strictSkips = true;
            } else if (a.equals("-j") || a.equals("--jobs")) {
                if (i + 1 >= args.length) {
                    System.out.println("missing count after " + a);
                    System.exit(1);
                    return;
                }
                i++;
                try {
                    jobs = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.out.println("invalid job count: " + args[i]);
                    System.out.println("usage: RunSuite [-j N] [--timeout Secs] [filter]");
                    System.exit(1);
                    return;
                }
            } else if (a.startsWith("--jobs=")) {
                String jobsValue = a.substring("--jobs=".length());
                try {
                    jobs = Integer.parseInt(jobsValue);
                } catch (NumberFormatException e) {
                    System.out.println("invalid job count: " + jobsValue);
                    System.out.println("usage: RunSuite [-j N] [--timeout Secs] [filter]");
                    System.exit(1);
                    return;
                }
            } else if (a.equals("--timeout")) {
                if (i + 1 >= args.length) {
                    System.out.println("missing seconds after " + a);
                    System.exit(1);
                    return;
                }
                i++;
                try {
                    timeoutSecs = Long.parseLong(args[i]);
                } catch (NumberFormatException e) {
                    System.out.println("invalid timeout value: " + args[i]);
                    System.out.println("usage: RunSuite [-j N] [--timeout Secs] [filter]");
                    System.exit(1);
                    return;
                }
                if (timeoutSecs < 1) {
                    System.out.println("invalid timeout value: " + args[i]);
                    System.out.println("usage: RunSuite [-j N] [--timeout Secs] [filter]");
                    System.exit(1);
                    return;
                }
            } else if (a.startsWith("--timeout=")) {
                String timeoutValue = a.substring("--timeout=".length());
                try {
                    timeoutSecs = Long.parseLong(timeoutValue);
                } catch (NumberFormatException e) {
                    System.out.println("invalid timeout value: " + timeoutValue);
                    System.out.println("usage: RunSuite [-j N] [--timeout Secs] [filter]");
                    System.exit(1);
                    return;
                }
                if (timeoutSecs < 1) {
                    System.out.println("invalid timeout value: " + timeoutValue);
                    System.out.println("usage: RunSuite [-j N] [--timeout Secs] [filter]");
                    System.exit(1);
                    return;
                }
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
        long limitSecs = timeoutSecs;
        // Children must run with the repo root as their working directory: two gates
        // (SignatureConformanceTest, AssertionQualityTest) locate sources through
        // relative paths (src/nsui, tests). Without this they silently skip() whenever
        // the suite is invoked from anywhere but the repo root, and RunSuite still
        // exits 0. Find the nearest ancestor of the classes dir that looks like the repo
        // root (has both src/ and tests/), so a CI layout that nests the output dir
        // differently still gets a valid working directory.
        Path repoRoot = null;
        for (Path p = Path.of(classesDir).toAbsolutePath().normalize(); p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("src")) && Files.isDirectory(p.resolve("tests"))) {
                repoRoot = p;
                break;
            }
        }
        final Path childCwd = repoRoot;
        Process[] live = new Process[tests.size()];
        ExecutorService pool = Executors.newFixedThreadPool(jobs);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (int k = 0; k < tests.size(); k++) {
            String t = tests.get(k);
            int slot = k;
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
                // Run the child from the repo root so relative-path gates (src/, tests/)
                // resolve no matter where tests.sh / RunSuite was started.
                if (childCwd != null) {
                    pb.directory(childCwd.toFile());
                }
                pb.redirectOutput(log.toFile());
                pb.redirectErrorStream(true);
                int code;
                try {
                    Process p = pb.start();
                    synchronized (live) {
                        live[slot] = p;
                    }
                    code = p.waitFor();
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
        for (int k = 0; k < futures.size(); k++) {
            Future<Outcome> f = futures.get(k);
            Outcome o;
            try {
                if (limitSecs > 0) {
                    o = f.get(limitSecs, TimeUnit.SECONDS);
                } else {
                    o = f.get();
                }
            } catch (TimeoutException e) {
                Process timed;
                synchronized (live) {
                    timed = live[k];
                }
                if (timed != null) {
                    timed.destroyForcibly();
                }
                f.cancel(true);
                System.out.println("FAIL: " + tests.get(k) + " timed out after " + limitSecs + "s");
                failed++;
                failedList.append(" ").append(tests.get(k));
                continue;
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
                // A test that exits 0 without ever printing a verdict must not pass silently.
                // TestKit.end() prints "RESULT:" and every real test either uses it or prints its
                // own (CoreAnimStressTest); TestKit.skip exits 2 and is handled above. Observed
                // failure mode: a new test that called check(false, ...) but forgot end() was
                // reported PASSED while its log contained FAIL lines.
                if (!readText(finalLogsDir, o.name).contains("RESULT:")) {
                    System.out.println("FAIL: " + o.name + " exited 0 without a RESULT line "
                            + "(it did not signal completion, so its verdict is unknown)");
                    failed++;
                    failedList.append(" ").append(o.name);
                } else {
                    passed++;
                }
            }
        }
        System.out.println("----------------------------------------");
        System.out.println("PASSED: " + passed + "  FAILED: " + failed + failedList
                + "  SKIPPED: " + skipped + skippedList);
        // skipCase()/probe()/PARTIAL PASS never affect a test's exit code, so they are
        // counted PASSED above. Surface them: a green summary that hides unproven cases
        // is how several abort-path gaps survived the suite. A whole-test skip() writes
        // no RESULT line, so count those from the SKIP: line too — otherwise the very
        // failure mode this reporting exists for (a gate that skipped) reads as 0/0/0.
        int caseSkips = 0;
        int probes = 0;
        int partials = 0;
        int testSkips = 0;
        for (int k = 0; k < tests.size(); k++) {
            String text;
            try {
                text = Files.readString(Path.of(finalLogsDir, tests.get(k) + ".log"));
            } catch (IOException e) {
                System.out.println("NOTE: no log for " + tests.get(k) + " (" + e + ")");
                continue;
            }
            for (String line : text.split("\n")) {
                if (line.startsWith("RESULT:") && line.contains("PARTIAL PASS")) {
                    partials++;
                } else if (line.startsWith("SKIP:")) {
                    testSkips++;
                } else if (line.startsWith("RESULT:") && line.endsWith("CASE SKIP(S)")) {
                    caseSkips += leadingCount(line);
                } else if (line.startsWith("RESULT:") && line.contains("PROBE(S)")) {
                    probes += leadingCount(line);
                }
            }
        }
        if (caseSkips > 0 || probes > 0 || partials > 0 || testSkips > 0) {
            System.out.println("NOTE: " + testSkips + " whole-test skip(s), " + caseSkips
                    + " skipped case(s), " + probes
                    + " no-oracle probe(s), " + partials + " partial-pass line(s) — counted PASS, not proven");
        }
        if (failed != 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
            return;
        }
        if (skipped != 0) {
            System.out.println("ALL RAN TESTS PASSED (" + skipped + " SKIPPED, UNPROVEN)");
            if (strictSkips) {
                System.out.println("TESTS FAILED (--strict-skips: " + skipped + " skipped case(s) not proven)");
                System.exit(1);
                return;
            }
            System.out.println("(exit 0: skipped cases are environment-dependent, not failures;"
                    + " use --strict-skips to require all of them)");
            System.exit(0);
            return;
        }
        System.out.println("ALL TESTS PASSED");
    }

    /// The test's log text, or "" when the log is missing/unreadable.
    private static String readText(String dir, String test) {
        try {
            return Files.readString(Path.of(dir, test + ".log"));
        } catch (IOException e) {
            return "";
        }
    }

    /// Leading integer of a "RESULT: N ..." summary line (0 when there is none).
    static int leadingCount(String line) {
        String s = line.substring("RESULT:".length()).trim();
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(s.substring(0, i));
        } catch (NumberFormatException e) {
            return 0;
        }
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
