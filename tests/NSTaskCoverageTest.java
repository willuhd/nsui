package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSArray;
import nsui.NSData;
import nsui.NSString;
import nsui.NSTask;
import nsui.objc.ObjC;

/// NSTaskCoverageTest — coverage for the NSTask wrapper (CodeGen 0.1.91, 35 generated).
///
/// Pure-Foundation, windowless: launches the real `/bin/echo` (fast, exits 0, no
/// network, no disk writes) through both the modern `executableURL` path and the
/// `launchedTaskWithExecutableURL:...` factory that carries the new ID(ID,ID,ID,ID)
/// Sig shape. Ends with TestKit.end().
///
/// Required coverage (batch spec):
/// - wrap(null)/wrap(NULL) == null + create() + initial state (!isRunning)
/// - Bounds rejections (null executableURL/arguments/launchPath/currentDirectoryPath)
/// - executableURL launch: launchAndReturnError(NULL) true, waitUntilExit,
///   terminationStatus 0, terminationReason exit(1), !isRunning, pid > 0
/// - qualityOfService + environment round-trips (nullable environment)
/// - 14.4-gated launchRequirementData: set/get round-trip when present,
///   null/absent path otherwise (emission-guarded, no VersionGuardScan tripping)
/// - modern factory launchedTaskWithExecutableURL:... (NULL error/handler)
///
/// Intentionally NOT exercised: suspend/resume (timing-dependent), terminate on a
/// live task, standardInput/Output/Error pipes (fd machinery), launchRequirementData
/// enforcement semantics (system-gated).
public final class NSTaskCoverageTest {

    private static void check(boolean ok, String msg) {
        TestKit.check(ok, msg);
        System.out.flush();
    }

    private static void checkThrowsIAE(Runnable r, String msg) {
        try {
            r.run();
            check(false, msg + " (no throw)");
        } catch (IllegalArgumentException e) {
            System.out.println("PASS: " + msg);
        } catch (Throwable t) {
            check(false, msg + " (wrong throw: " + t + ")");
        }
    }

    private static MemorySegment echoURL() {
        return ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("fileURLWithPath:"),
                ObjC.nsstring("/bin/echo"));
    }

    private static NSArray helloArgs() {
        return NSArray.arrayWithObject(NSString.of("hello"));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== NSTaskCoverageTest ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            TestKit.skip("ObjC.init failed (not macOS / no AppKit): " + t);
        }

        System.out.println("---- wrap-null + create ----");
        check(NSTask.wrap(null) == null, "wrap(null)==null");
        check(NSTask.wrap(MemorySegment.NULL) == null, "wrap(NULL)==null");
        NSTask t = NSTask.create();
        check(t != null, "create() non-null");
        check(!t.isRunning(), "fresh task !isRunning");

        System.out.println("---- Bounds rejections ----");
        NSTask g = NSTask.create();
        checkThrowsIAE(() -> g.setExecutableURL(null), "setExecutableURL(null) IAE");
        checkThrowsIAE(() -> g.setExecutableURL(MemorySegment.NULL), "setExecutableURL(NULL) IAE");
        checkThrowsIAE(() -> g.setArguments(null), "setArguments(null) IAE");
        checkThrowsIAE(() -> g.setLaunchPath(null), "setLaunchPath(null) IAE");
        checkThrowsIAE(() -> g.setCurrentDirectoryPath(null), "setCurrentDirectoryPath(null) IAE");

        System.out.println("---- executableURL launch (/bin/echo) ----");
        NSTask echo = NSTask.create();
        echo.setExecutableURL(echoURL());
        echo.setArguments(helloArgs());
        check(echo.launchAndReturnError(null), "launchAndReturnError(NULL) true");
        echo.waitUntilExit();
        check(!echo.isRunning(), "after wait !isRunning");
        check(echo.terminationStatus() == 0, "terminationStatus==0");
        check(echo.terminationReason() == 1, "terminationReason==exit(1)");
        check(echo.processIdentifier() > 0, "pid>0");

        System.out.println("---- qualityOfService + environment ----");
        NSTask q = NSTask.create();
        long qos = q.qualityOfService();
        q.setQualityOfService(qos);
        check(q.qualityOfService() == qos, "qualityOfService round-trip");
        // nil environment means "inherit the parent's": the getter then returns the
        // inherited dictionary (non-null), not nil — so the oracle is inherit, not clear.
        q.setEnvironment(null);
        check(q.environment() != null, "environment nil inherits parent env");

        System.out.println("---- launchRequirementData (14.4-gated) ----");
        NSTask r = NSTask.create();
        NSData blob = NSData.dataWithBytes(new byte[]{1, 2, 3});
        r.setLaunchRequirementData(blob);
        NSData back = r.launchRequirementData();
        if (back == null) {
            TestKit.skipCase("SKIP launchRequirementData absent (pre-14.4 host)");
        } else {
            check(back.length() == 3, "launchRequirementData round-trip length");
        }

        System.out.println("---- modern factory ----");
        NSTask f = NSTask.launchedTaskWithExecutableURLArgumentsErrorTerminationHandler(
                echoURL(), helloArgs(), null, null);
        check(f != null, "launchedTaskWithExecutableURL... non-null");
        f.waitUntilExit();
        check(f.terminationStatus() == 0, "factory task status==0");
        check(!f.isRunning(), "factory task !isRunning");

        System.out.println("\n=== NSTaskCoverageTest " + (TestKit.failures() == 0 ? "PASS" : "FAIL") + " ===");
        TestKit.end();
    }
}
