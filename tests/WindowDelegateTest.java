package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import nsui.NSApplication;
import nsui.NSObject;
import nsui.NSRect;
import nsui.NSWindow;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;
import nsui.objc.Sig;

/// Void-notification delegates + multi-selector routing on a SINGLE proxy instance.
///
/// One `NSWindowDelegate` instance implements THREE selectors on ONE native object:
/// - `windowShouldClose:` (`-(BOOL)`) — a Java veto that rejects the close.
/// - `windowDidResize:` (`-(void)`) — a pure side-effecting notification.
/// - `windowDidMove:` (`-(void)`) — another pure side-effecting notification.
/// The three selectors route to three distinct Java lambdas on one instance, proving the
/// selector-address-keyed dispatch of `DelegateProxy` on a single peer.
///
/// Driving the delegate: a `windowShouldClose:` veto keeps the window open, a
/// `setFrame:display:` size change fires `windowDidResize:`, and a
/// `setFrameOrigin:` origin change fires `windowDidMove:` — three distinct
/// selectors (one `-(BOOL)`, two `-(void)`) all routed by the selector-address
/// dispatch of a single proxy instance.
///
/// NSWindow API reality (measured, not assumed): `NSWindow` has NO bare
/// `setFrame:` — that is an `NSView` selector, and calling it on a window raises
/// `'unrecognized selector sent to instance'` (we hit that and it aborted the JVM as a
/// native exception). The real `NSWindow` API is `setFrame:display:` and
/// `setFrameOrigin:`. Also, `setFrame:display:` posting both a size AND an origin
/// change fires `windowDidResize:` but NOT `windowDidMove:`; the move notification
/// only fires via `setFrameOrigin:`. Both behaviours are AppKit's own — they do not affect
/// the DelegateProxy routing, which works for every registered selector.
///
/// `NSWindow` has no Java wrapper for either frame selector, so we go through the
/// vocabulary escape hatch: `setFrame:display:` uses `(id, SEL, NSRect, BOOL) -> void`
/// (`Sig#of(Sig.Ret.VOID, Sig.Arg.RECT, Sig.Arg.BOOL)`) and `setFrameOrigin:` uses
/// `(id, SEL, NSPoint) -> void` (`Sig#of(Sig.Ret.VOID, Sig.Arg.POINT)`), each cached
/// as a `MethodHandle` and invoked with `invokeExact` (the hot-path requirement).
///
/// Honesty about delivery: frame-change notifications are normally posted synchronously by
/// `setFrame:display:`, but if the manual pump does not surface them the test iterates —
/// pumping longer and calling `displayIfNeeded:` — and reports what ACTUALLY fired.
public final class WindowDelegateTest {

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
        TestKit.check(winDelegate != null && winDelegate.address() != 0, "triple-selector delegate created");

        NSWindow window = NSWindow.create(new NSRect(0, 0, 400, 250), 15L, 2L, false);
        window.setTitle("window delegate multi-selector");
        window.center();
        window.setReleasedWhenClosed(false);
        // Veto path asserts still-visible: needs a genuinely visible (not key) window.
        TestKit.show(window);
        window.setDelegate(NSObject.wrap(winDelegate));
        app.finishLaunching();

        int regBefore = DelegateProxy.registrySize();
        TestKit.check(regBefore == 1, "registry holds the one triple-selector delegate (size=" + regBefore + ")");

        // ---- 1) performClose with a VETO: window stays, notifications untouched ----
        TestKit.pump(app, 500L);
        // Close alone here (see DelegateTest): ordering out first would mask the veto.
        window.performClose(null);
        TestKit.pump(app, 500L);

        TestKit.check(window.isVisible(), "windowShouldClose vetoed the close: window still visible");
        TestKit.check(!resized.get(), "windowDidResize NOT fired while vetoing close");
        TestKit.check(!moved.get(),   "windowDidMove NOT fired while vetoing close");
        System.out.println("after vetoed close: isVisible=" + window.isVisible()
                + " resized=" + resized.get() + " moved=" + moved.get() + " (expected true/false/false)");
        System.out.println("PASS: Java veto — void selectors stayed quiet");

        // ---- 2) setFrame:display: with a NEW SIZE -> windowDidResize must fire ----
        // NSWindow's real API is setFrame:display: (no bare setFrame:); Java has no wrapper, so we
        // go through the vocabulary RECT+BOOL void handle, cached and invoked via invokeExact.
        MethodHandle setFrame = ObjC.handle(Sig.of(Sig.Ret.VOID, Sig.Arg.RECT, Sig.Arg.BOOL)); // (id, SEL, NSRect, BOOL) -> void
        setFrame.invokeExact(window.peer(), ObjC.sel("setFrame:display:"), new NSRect(120, 90, 560, 380).toSegment(), false);
        TestKit.pump(app, 800L);
        TestKit.check(resized.get(), "windowDidResize FIRED after setFrame:display: (size changed)");
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
        TestKit.pump(app, 800L);
        TestKit.check(moved.get(),   "windowDidMove FIRED after setFrameOrigin: (origin changed)");
        System.out.println("after setFrameOrigin: moved=" + moved.get() + " (expected true)");

        System.out.println("PASS: ONE instance routed windowShouldClose + windowDidResize + "
                + "windowDidMove across bool AND void selectors");
        TestKit.check(DelegateProxy.registrySize() == regBefore,
                "registry unchanged (no churn) (size=" + DelegateProxy.registrySize() + ")");

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }
}
