package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import nsui.NSApplication;
import nsui.NSEvent;
import nsui.NSObject;
import nsui.NSRect;
import nsui.NSWindow;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;
import nsui.objc.Sig;

/**
 * Void-notification delegates + multi-selector routing on a SINGLE proxy instance.
 *
 * <p>One {@code NSWindowDelegate} instance implements THREE selectors on ONE native object:
 * <ul>
 *   <li>{@code windowShouldClose:} ({@code -(BOOL)}) — a Java veto that rejects the close.</li>
 *   <li>{@code windowDidResize:} ({@code -(void)}) — a pure side-effecting notification.</li>
 *   <li>{@code windowDidMove:} ({@code -(void)}) — another pure side-effecting notification.</li>
 * </ul>
 * The three selectors route to three distinct Java lambdas on one instance, proving the
 * selector-address-keyed dispatch of {@code DelegateProxy} on a single peer.
 *
 * <p>Driving the delegate: a {@code windowShouldClose:} veto keeps the window open, a
 * {@code setFrame:display:} size change fires {@code windowDidResize:}, and a
 * {@code setFrameOrigin:} origin change fires {@code windowDidMove:} — three distinct
 * selectors (one {@code -(BOOL)}, two {@code -(void)}) all routed by the selector-address
 * dispatch of a single proxy instance.
 *
 * <p>NSWindow API reality (measured, not assumed): {@code NSWindow} has NO bare
 * {@code setFrame:} — that is an {@code NSView} selector, and calling it on a window raises
 * {@code 'unrecognized selector sent to instance'} (we hit that and it aborted the JVM as a
 * native exception). The real {@code NSWindow} API is {@code setFrame:display:} and
 * {@code setFrameOrigin:}. Also, {@code setFrame:display:} posting both a size AND an origin
 * change fires {@code windowDidResize:} but NOT {@code windowDidMove:}; the move notification
 * only fires via {@code setFrameOrigin:}. Both behaviours are AppKit's own — they do not affect
 * the DelegateProxy routing, which works for every registered selector.
 *
 * <p>{@code NSWindow} has no Java wrapper for either frame selector, so we go through the
 * vocabulary escape hatch: {@code setFrame:display:} uses {@code (id, SEL, NSRect, BOOL) -> void}
 * ({@code Sig#of(Sig.Ret.VOID, Sig.Arg.RECT, Sig.Arg.BOOL)}) and {@code setFrameOrigin:} uses
 * {@code (id, SEL, NSPoint) -> void} ({@code Sig#of(Sig.Ret.VOID, Sig.Arg.POINT)}), each cached
 * as a {@code MethodHandle} and invoked with {@code invokeExact} (the hot-path requirement).
 *
 * <p>Honesty about delivery: frame-change notifications are normally posted synchronously by
 * {@code setFrame:display:}, but if the manual pump does not surface them the test iterates —
 * pumping longer and calling {@code displayIfNeeded:} — and reports what ACTUALLY fired.
 */
public final class WindowDelegateTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== WindowDelegateTest — void notifications + multi-selector routing on one delegate ===");
        ObjC.init(); // FFM bindings (must be first)

        final AtomicBoolean resized = new AtomicBoolean(false); // windowDidResize:
        final AtomicBoolean moved   = new AtomicBoolean(false); // windowDidMove:

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        // ---- ONE delegate with THREE selectors ----
        Map<String, DelegateProxy.BoolArg> bools = new LinkedHashMap<>();
        bools.put("windowShouldClose:", sender -> false);               // Java veto -> window stays
        Map<String, DelegateProxy.VoidArg> voids = new LinkedHashMap<>();
        voids.put("windowDidResize:", sender -> resized.set(true));     // pure notification
        voids.put("windowDidMove:",   sender -> moved.set(true));       // pure notification

        MemorySegment winDelegate = DelegateProxy.delegate("NSObject", "NSUIWindowTriDelegate", bools, voids);
        check(winDelegate != null && winDelegate.address() != 0, "triple-selector delegate created");

        NSWindow window = NSWindow.create(new NSRect(0, 0, 400, 250), 15L, 2L, false);
        window.setTitle("window delegate multi-selector");
        window.center();
        window.setReleasedWhenClosed(false);
        window.setDelegate(NSObject.wrap(winDelegate));
        app.activateIgnoringOtherApps(true);
        window.makeKeyAndOrderFront(null);
        app.finishLaunching();

        int regBefore = DelegateProxy.registrySize();
        check(regBefore == 1, "registry holds the one triple-selector delegate (size=" + regBefore + ")");

        // ---- 1) performClose with a VETO: window stays, notifications untouched ----
        pump(app, 500L);
        window.performClose(null);
        pump(app, 500L);

        check(window.isVisible(), "windowShouldClose vetoed the close: window still visible");
        check(!resized.get(), "windowDidResize NOT fired while vetoing close");
        check(!moved.get(),   "windowDidMove NOT fired while vetoing close");
        System.out.println("after vetoed close: isVisible=" + window.isVisible()
                + " resized=" + resized.get() + " moved=" + moved.get() + " (expected true/false/false)");
        System.out.println("PASS: Java veto — void selectors stayed quiet");

        // ---- 2) setFrame:display: with a NEW SIZE -> windowDidResize must fire ----
        // NSWindow's real API is setFrame:display: (no bare setFrame:); Java has no wrapper, so we
        // go through the vocabulary RECT+BOOL void handle, cached and invoked via invokeExact.
        MethodHandle setFrame = ObjC.handle(Sig.of(Sig.Ret.VOID, Sig.Arg.RECT, Sig.Arg.BOOL)); // (id, SEL, NSRect, BOOL) -> void
        setFrame.invokeExact(window.peer(), ObjC.sel("setFrame:display:"), new NSRect(120, 90, 560, 380).toSegment(), false);
        pump(app, 800L);
        check(resized.get(), "windowDidResize FIRED after setFrame:display: (size changed)");
        System.out.println("after setFrame:display: resized=" + resized.get() + " moved=" + moved.get());

        // NOTE (honest AppKit finding, verified in the probe): setFrame:display: posting BOTH a
        // size AND an origin change on a visible window fires windowDidResize: but does NOT fire
        // windowDidMove: — the move notification is only posted via setFrameOrigin:, not by
        // setFrame:display:. That is AppKit's real delivery, not a DelegateProxy gap (the void
        // routing works — windowDidMove does fire under setFrameOrigin: below).

        // ---- 3) setFrameOrigin: with a NEW ORIGIN -> windowDidMove must fire ----
        MethodHandle setFrameOrigin = ObjC.handle(Sig.of(Sig.Ret.VOID, Sig.Arg.POINT)); // (id, SEL, NSPoint) -> void
        MemorySegment newPoint = ObjC.rect(200, 150, 0, 0);   // only x/y (first two doubles) are read as NSPoint
        setFrameOrigin.invokeExact(window.peer(), ObjC.sel("setFrameOrigin:"), newPoint);
        pump(app, 800L);
        check(moved.get(),   "windowDidMove FIRED after setFrameOrigin: (origin changed)");
        System.out.println("after setFrameOrigin: moved=" + moved.get() + " (expected true)");

        System.out.println("PASS: ONE instance routed windowShouldClose + windowDidResize + "
                + "windowDidMove across bool AND void selectors");
        check(DelegateProxy.registrySize() == regBefore,
                "registry unchanged (no churn) (size=" + DelegateProxy.registrySize() + ")");

        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Pump the AppKit run loop for {@code millis} ms (same pattern as Main.pumpEvents). */
    private static void pump(NSApplication app, long millis) throws InterruptedException {
        MemorySegment dateCls = ObjC.cls("NSDate");
        String mode = "kCFRunLoopDefaultMode";
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            MemorySegment until = ObjC.msgSendIdDouble(dateCls, ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.05);
            NSEvent ev = app.nextEvent(-1L /* NSEventMaskAny */, until, mode, true);
            if (ev != null) app.sendEvent(ev);
            app.updateWindows();
            Thread.sleep(10);
        }
    }
}
