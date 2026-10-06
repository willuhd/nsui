package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSApplication;
import nsui.NSEvent;
import nsui.NSPoint;
import nsui.NSRect;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// Shared test primitives for the nsui suite.
///
/// Deliberately dependency-free (no JUnit, no JMH): every test is a plain
/// `main()` in its own JVM. This file holds the boilerplate that used to
/// be copy-pasted into all 51 tests, in four groups:
///
/// - **Reporting** (`check` + failure count + `end`) — the
/// "boolean thing". One counter per JVM; call `end()` exactly once
/// at the end of `main` so the exit code is honest.
/// - **Unobtrusive windows** — windows are created hidden and, by
/// default, *never shown*. `show()` orders front without
/// making key (no focus steal, no activation); `showKey()` is
/// reserved for the two tests where key routing is load-bearing
/// (NSEventTest's window-server click, ButtonTest's hit-test path).
/// - **Run-loop pump** — the same `nextEventMatchingMask` loop
/// every test used to hand-write.
/// - **Bench** — `nanoTime` loops with generous fixed budgets.
/// These are regression tripwires (order-of-magnitude), not JMH-grade
/// measurements; that is intentional (see BenchTest).
public final class TestKit {

    private TestKit() {}

    // ---------------------------------------------------------- reporting

    private static int failures;
    private static int caseSkips;
    private static int probes;

    /// Record one assertion. Prints immediately so logs stay useful on crash.
    public static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    /// A body that may throw anything (AppKit calls can raise).
    public interface Body { void run() throws Throwable; }

    /// A body that produces a value and may throw anything.
    public interface Supply<T> { T get() throws Throwable; }

    /// Assert a call does not throw - a real assertion that can fail, unlike the
    /// old always-true probe on a no-crash message.
    public static void noThrow(String what, Body body) {
        try {
            body.run();
            System.out.println("PASS: " + what);
        } catch (Throwable t) {
            check(false, what + " threw: " + t);
        }
    }

    /// noThrow for the T v = obj.getter() shape: returns the value (null on throw).
    public static <T> T attempt(String what, Supply<T> body) {
        try {
            T v = body.get();
            System.out.println("PASS: " + what);
            return v;
        } catch (Throwable t) {
            check(false, what + " threw: " + t);
            return null;
        }
    }

    /// A case that cannot run on this OS/session (selector absent, headless, nil
    /// precondition). Counted and reported separately: never a pass, never a
    /// failure. Replaces the old always-true SKIP probe.
    public static void skipCase(String why) {
        caseSkips++;
        System.out.println("SKIP-CASE: " + why);
    }

    /// A call exercised but with no assertable oracle (a bare no-crash probe on an
    /// environment-defined getter). Counted and reported separately so it can never
    /// inflate the assertion count a real check would.
    public static void probe(String what) {
        probes++;
        System.out.println("PROBE: " + what);
    }

    /// Negative test: assert the body throws (a subtype of) the expected type.
    public static void expectThrows(String what, Class<? extends Throwable> expected, Body body) {
        try {
            body.run();
            check(false, what + " did not throw (expected " + expected.getSimpleName() + ")");
        } catch (Throwable t) {
            if (expected.isInstance(t)) {
                System.out.println("PASS: " + what + " threw " + t.getClass().getSimpleName());
            } else {
                check(false, what + " threw " + t.getClass().getSimpleName()
                        + " (expected " + expected.getSimpleName() + ")");
            }
        }
    }

    /// Failures so far (for custom summary lines; prefer {@code end()}).
    public static int failures() {
        return failures;
    }

    /// Print the final verdict and exit honestly: 0 iff every check passed.
    public static void end() {
        if (caseSkips > 0) System.out.println("RESULT: " + caseSkips + " CASE SKIP(S)");
        if (probes > 0) System.out.println("RESULT: " + probes + " PROBE(S) (exercised, no oracle)");
        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    /// Setup proved impossible (e.g. no GUI session for an AppKit test).
    /// Exits 2 (distinct from failure=1): a skip is unproven, not green.
    /// tests.sh counts skips separately and reports them loudly.
    public static void skip(String why) {
        System.out.println("SKIP: " + why);
        System.exit(2);
    }

    // --------------------------------------------------------------- app

    /// Shared application instance. Deliberately does NOT set an activation
    /// policy or activate: tests must not steal focus or bounce the Dock.
    /// Aborts with SKIP when AppKit is unreachable (headless session).
    public static NSApplication app() {
        try {
            return NSApplication.shared();
        } catch (Throwable t) {
            skip("NSApplication unavailable (headless session?): " + t.getMessage());
            return null; // unreachable
        }
    }

    // ------------------------------------------------------------- windows

    /// A real window that is never shown: suitable for every test that only
    /// needs view hierarchy, frames, delegates, or drawing state.
    public static NSWindow hiddenWindow(double w, double h) {
        return hiddenWindow(new NSRect(0, 0, w, h));
    }

    /// A real window that is never shown, at an explicit frame.
    public static NSWindow hiddenWindow(NSRect frame) {
        NSWindow win = NSWindow.create(frame, 15L, 2L, false);
        win.setTitle("nsui-test");
        win.setReleasedWhenClosed(false);
        return win;
    }

    /// A real (never shown) utility panel, for NSPanel-behavior tests.
    public static NSWindow hiddenPanel(double w, double h) {
        NSWindow panel = NSWindow.createPanel(new NSRect(0, 0, w, h), 15L | 16L, 2L, false);
        panel.setReleasedWhenClosed(false);
        return panel;
    }

/// Park a window where no screen paints it. Called by show()/showKey() so
/// even "visible" test windows never flash on screen. Fires windowDidMove:
/// to an attached delegate — every caller attaches its delegate after
/// showing, so nothing observes it.
    public static void park(NSWindow win) {
        if (win == null) return;
        try {
            win.setFrameOrigin(new NSPoint(-10000, -10000));
        } catch (Throwable t) {
            System.out.println("NOTE: park failed: " + t.getMessage());
        }
    }

/// Make a window visible WITHOUT making it key: `orderFront:`
/// instead of `makeKeyAndOrderFront:`. No activation, no focus
/// steal — and parked offscreen first, so no pixels either. Prefer leaving
/// windows hidden; use this only where AppKit-visibility is load-bearing
/// (close-veto routing, sheet attachment, popover anchoring).
    public static void show(NSWindow win) {
        if (win == null) return;
        park(win);
        try {
            win.orderFront(null);
        } catch (Throwable t) {
            System.out.println("NOTE: orderFront failed: " + t.getMessage());
        }
    }

/// Show AND make key (steals focus, activates the app). Reserved for tests
/// where key-window event routing is the thing under test (NSEventTest's
/// window-server click, ButtonTest's hit-test path). Everywhere else this
/// is disruption without coverage. Does NOT park: both callers need real
/// screen coordinates (NSEventTest’s window-server click, ButtonTest’s
/// screen-coordinate hit-test).
    public static void showKey(NSWindow win) {
        if (win == null) return;
        NSApplication.shared().activateIgnoringOtherApps(true);
        win.makeKeyAndOrderFront(null);
    }

    /// Quiet teardown: order out, then close. Never throws.
    ///
    /// Not for close-veto assertions: ordering out first hides the window
    /// before the delegate verdict, masking the veto — those tests must call
    /// `performClose` alone (see DelegateTest).
    public static void close(NSWindow win) {
        if (win == null) return;
        try {
            win.orderOut(null);
        } catch (Throwable ignore) {
        }
        try {
            win.performClose(null);
        } catch (Throwable ignore) {
        }
    }

    /// Quiet teardown for popovers. Never throws.
    public static void close(nsui.NSPopover pop) {
        if (pop == null) return;
        try {
            pop.close();
        } catch (Throwable ignore) {
        }
    }

    // ------------------------------------------------------------- runloop

    /// Pump the AppKit run loop with the historical 1500 ms default.
    public static void pump(NSApplication app) throws InterruptedException {
        pump(app, 1500);
    }

    /// Pump the AppKit run loop for {@code millis} ms — delegates to the real
    /// `NSApplication.pumpFor`, so there is exactly one pump implementation.
    public static void pump(NSApplication app, long millis) throws InterruptedException {
        app.pumpFor(millis);
    }

    /// One pump iteration: useful for settle-wait loops with custom exits.
    public static void pumpOnce(NSApplication app) {
        MemorySegment pool = nsui.objc.Autorelease.push();
        try {
            MemorySegment dateCls = ObjC.cls("NSDate");
            MemorySegment until = ObjC.msgSendIdDouble(dateCls, ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.05);
            NSEvent ev = app.nextEvent(-1L, until, "kCFRunLoopDefaultMode", true);
            if (ev != null) app.sendEvent(ev);
            app.updateWindows();
        } catch (Throwable t) {
            System.out.println("NOTE: pumpOnce failed: " + t.getMessage());
        } finally {
            try { nsui.objc.Autorelease.pop(pool); } catch (Throwable ignore) {}
        }
    }

    // --------------------------------------------------------------- bench

    /// Wall time of {@code iters} executions, in ms. Caller warms up first;
    /// this measures steady state only.
    public static long measureMs(int iters, Runnable body) {
        long t0 = System.nanoTime();
        for (int i = 0; i < iters; i++) body.run();
        return (System.nanoTime() - t0) / 1_000_000;
    }

    /// Informational timing line (microseconds per op). Never fails.
    public static void noteTime(String what, long ms, int iters) {
        double us = iters == 0 ? 0 : (ms * 1000.0) / iters;
        System.out.printf("TIME: %s — %d ms for %d iters (%.1f us/op)%n", what, ms, iters, us);
    }

/// Regression tripwire: passes iff `ms <= budgetMs`. Budgets are
/// deliberately generous (10x+ headroom over this machine) — they catch
/// order-of-magnitude regressions (e.g. a hot path going quadratic),
/// not 20% noise. This is not JMH and does not pretend to be.
    public static void checkTime(String what, long ms, long budgetMs) {
        check(ms <= budgetMs, what + " took " + ms + " ms (budget " + budgetMs + " ms)");
    }
}
