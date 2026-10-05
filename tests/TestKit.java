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

    /// Record one assertion. Prints immediately so logs stay useful on crash.
    public static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    /// Failures so far (for custom summary lines; prefer {@code end()}).
    public static int failures() {
        return failures;
    }

    /// Print the final verdict and exit honestly: 0 iff every check passed.
    public static void end() {
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
        try {
            MemorySegment dateCls = ObjC.cls("NSDate");
            MemorySegment until = ObjC.msgSendIdDouble(dateCls, ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.05);
            NSEvent ev = app.nextEvent(-1L, until, "kCFRunLoopDefaultMode", true);
            if (ev != null) app.sendEvent(ev);
            app.updateWindows();
        } catch (Throwable t) {
            System.out.println("NOTE: pumpOnce failed: " + t.getMessage());
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
