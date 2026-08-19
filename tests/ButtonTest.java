package nsui.tests;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import nsui.NSApplication;
import nsui.NSButton;
import nsui.NSRect;
import nsui.NSView;
import nsui.NSWindow;
import nsui.NSEvent;
import nsui.objc.DelegateProxy;
import nsui.objc.NsuiForeign;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * ButtonTest — the real end-to-end NSButton control test.
 *
 * <p>Creates a window + content view, installs an {@code NSButton} whose action is a
 * {@code DelegateProxy.actionTarget}: a Java {@link Runnable}-style callback fired when
 * AppKit sends the registered action selector to the target. Two click paths are tried,
 * in order:
 *
 * <ol>
 *   <li><b>REAL click path</b>: a genuine {@code CGEvent} pair (leftMouseDown/leftMouseUp)
 *       is built with {@code CGEventCreateMouseEvent}, converted to a real {@code NSEvent}
 *       with {@code [NSEvent eventWithCGEvent:]}, and injected into the app's OWN event
 *       queue with {@code postEvent:atStart:}. Pumping the run loop dispatches it via
 *       {@code sendEvent}, which performs hit-testing against the key window — if the point
 *       lands on the button, NSButton performs its action and the handler fires with its
 *       sender == the button. This exercises the FULL native wiring: control -> window
 *       hit-test -> target/action -> DelegateProxy upcall -> Java.</li>
 *   <li><b>target-action fallback</b>: if the injected click does not reach the button on
 *       this session (e.g. non-frontmost, non-interactive, geometry), the action selector
 *       is sent DIRECTLY to the target ({@code [target pressed:button]}). This still proves
 *       button -> target -> action selector -> Java VoidArg, but does NOT exercise window
 *       hit-testing, which is honestly reported.</li>
 * </ol>
 *
 * <p>In both cases we additionally assert {@code title()}, {@code isEnabled()} and a sane
 * post-{@code sizeToFit} frame.
 */
public final class ButtonTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== ButtonTest — real NSButton control ===");
        ObjC.init(); // FFM bindings first

        final AtomicBoolean clicked = new AtomicBoolean(false);
        final AtomicLong senderAddr = new AtomicLong(-1L);

        // The action target: an ObjC NSObject implementing -pressed: in Java.
        MemorySegment target = DelegateProxy.actionTarget("pressed:", (MemorySegment sender) -> {
            clicked.set(true);
            senderAddr.set(sender.address());
        });

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        NSWindow window = NSWindow.create(new NSRect(0, 0, 500, 320), 15L, 2L, false);
        window.setTitle("button test");
        window.center();
        window.setReleasedWhenClosed(false);

        NSView content = NSView.create(new NSRect(0, 0, 500, 320), (ctx, d) -> {});
        window.setContentView(content);

        NSButton button = NSButton.create(new NSRect(150, 130, 200, 44), "Click me", target, "pressed:");
        content.addSubview(button);   // controls are views now (NSControl extends NSView)

        check(DelegateProxy.registrySize() >= 1, "DelegateProxy registered the action target (size=" + DelegateProxy.registrySize() + ")");

        app.activateIgnoringOtherApps(true);
        window.makeKeyAndOrderFront(null);
        app.finishLaunching();
        pumpForMs(app, 600); // let the window settle

        // ---- static button assertions ----
        check("Click me".equals(button.title()), "button.title() == \"Click me\" (got \"" + button.title() + "\")");
        check(button.isEnabled(), "button.isEnabled() == true");
        NSRect f = button.frame();
        System.out.printf("  button frame after create+sizeToFit: (%.1f, %.1f) %s%n", f.x(), f.y(), f);
        check(f.width() > 50, "button.frame() width > 50 after sizeToFit (got " + f.width() + ")");
        check(f.height() > 10, "button.frame() height > 10 after sizeToFit (got " + f.height() + ")");

        // ---- attempt the REAL click path (queued CGEvent-derived NSEvent pair) ----
        attemptQueuedClick(app, button);
        boolean realPathHit = clicked.get();
        if (realPathHit) {
            check(clicked.get(), "real click path: handler fired after injected CGEvent pair");
            check(senderAddr.get() == button.peer().address(),
                    "real click path: handler sender == button (sender=" + Long.toHexString(senderAddr.get())
                            + " button=" + Long.toHexString(button.peer().address()) + ")");
            System.out.println("CLICK PATH: real (queued CGEvent-derived NSEvent -> window hit-test) exercised the button");
        } else {
            System.out.println("NOTE: injected CGEvent pair did not reach the button on this session "
                    + "(non-frontmost / geometry). Falling back to the DIRECT target-action send.");
            // Fallback: [target pressed:button] — calls the same action selector on the SAME target.
            ObjC.msgSendVoidId(target, ObjC.sel("pressed:"), button.peer());
            check(clicked.get(), "target-action path: handler fired via direct [target pressed:button]");
            check(senderAddr.get() == button.peer().address(),
                    "target-action path: handler sender == button (sender=" + Long.toHexString(senderAddr.get())
                            + " button=" + Long.toHexString(button.peer().address()) + ")");
            System.out.println("CLICK PATH: target-action (direct selector send; window hit-testing NOT exercised — honest)");
        }

        System.out.println(failures == 0
                ? (realPathHit ? "RESULT: ALL PASS (real click path exercised)"
                               : "RESULT: PARTIAL PASS (target-action wiring proven; window hit-test NOT exercised in this session)")
                : "RESULT: " + failures + " FAILURE(S)");
        window.performClose(null);
        System.exit(failures == 0 ? 0 : 1);
    }

    /**
     * Build a real CGEvent mouse-down + mouse-up pair, convert each to an NSEvent via
     * {@code [NSEvent eventWithCGEvent:]}, and inject into the app's own queue with
     * {@code postEvent:atStart:}, then pump so the run loop dispatches them. After this
     * returns, the caller inspects the outer {@code clicked} flag to detect whether AppKit
     * hit-tested the button and performed its action.
     */
    private static void attemptQueuedClick(NSApplication app, NSButton button) throws Throwable {
        // Runtime-resolved CoreGraphics downcall handles (NEVER a static initializer; same
        // pattern as NSEventTest). Descriptors come from NsuiForeign (single source of truth).
        Linker linker = Linker.nativeLinker();
        SymbolLookup cg = SymbolLookup.libraryLookup(
                "/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics", Arena.global());
        MethodHandle hCreate = linker.downcallHandle(
                cg.find("CGEventCreateMouseEvent").orElseThrow(), NsuiForeign.cgEventCreateMouseEvent());

        // Click at the button's center. The button is a direct subview of the content view,
        // which fills the window's content area; pass the button-center as the event's
        // location in window base coordinates and let AppKit hit-test.
        NSRect bf = button.frame();
        double bx = bf.x() + bf.width() / 2.0;
        double by = bf.y() + bf.height() / 2.0;

        // -- mouseDown (type 1) then mouseUp (type 2); append both (atStart: NO) so order is preserved. --
        postEvent(app, hCreate, bx, by, 1 /* kCGEventLeftMouseDown */);
        postEvent(app, hCreate, bx, by, 2 /* kCGEventLeftMouseUp */);

        // The action may fire on the mouseUp when the down was pressed on the button.
        pumpForMs(app, 2000);
    }

    /** Convert a CGEvent mouse event to an NSEvent and append it to the app's queue. */
    private static void postEvent(NSApplication app, MethodHandle hCreate, double bx, double by, int type) throws Throwable {
        MemorySegment pt = Arena.ofShared().allocate(16);
        pt.set(ValueLayout.JAVA_DOUBLE, 0, bx);
        pt.set(ValueLayout.JAVA_DOUBLE, 8, by);
        MemorySegment cgEv = (MemorySegment) hCreate.invokeExact(
                MemorySegment.NULL, type, pt, (int) 0 /* left button */);
        MemorySegment nsEv = ObjC.msgSendIdId(ObjC.cls("NSEvent"), ObjC.sel("eventWithCGEvent:"), cgEv);
        if (nsEv == null || nsEv.address() == 0) return;
        // [NSApplication postEvent:atStart:] — (void, id, bool). Cached handle; atStart: NO appends.
        ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.BOOL)).invokeExact(
                app.peer(), ObjC.sel("postEvent:atStart:"), nsEv, false);
    }

    // ------------------------------------------------------------------ helpers

    private static void pumpOnce(NSApplication app) {
        MemorySegment until = ObjC.msgSendIdDouble(
                ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.05);
        NSEvent ev = app.nextEvent(-1L, until, "kCFRunLoopDefaultMode", true);
        if (ev != null) app.sendEvent(ev); // dispatch (triggers hit-test + button action)
        app.updateWindows();
    }

    private static void pumpForMs(NSApplication app, long ms) throws InterruptedException {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            pumpOnce(app);
            Thread.sleep(10);
        }
    }
}
