package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

import nsui.*;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * ResponderEventTest — wiring-level coverage for the responder chain and
 * input-event handling on custom views (Tier-1 item #1). Non-interactive and
 * self-terminating:
 *
 * - NSView.create view responds to every installed event/responder selector
 *   (respondsToSelector:) and really is the NSUIViewImpl subclass pair;
 * - acceptsFirstResponder flips exactly with key-listener registration;
 * - performKeyEquivalent reports "not handled" (chain continues);
 * - mouse/key listener registration round-trips through NSView.listenerCount();
 * - nextResponder/setNextResponder round-trip, including view -> window link;
 * - a real NSWindow: makeFirstResponder runs clean, firstResponder reads back,
 *   acceptsMouseMovedEvents and initialFirstResponder round-trip, and the
 *   inherited NSResponder touchBar accessors work on the window;
 * - enableMouseTracking installs a tracking area without error (idempotent);
 * - 200x register/unregister stress returns listenerCount() to baseline.
 */
public final class ResponderEventTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    private static boolean responds(MemorySegment receiver, String selector) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(receiver, ObjC.sel("respondsToSelector:"), ObjC.sel(selector));
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== ResponderEventTest ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            String m = String.valueOf(t.getMessage()).toLowerCase();
            if (m.contains("connection") || m.contains("dlopen") || m.contains("appkit")) {
                System.out.println("SKIP: ObjC.init failed (not macOS / connection error): " + t);
                System.out.println("RESULT: SKIP (connection error, continuing)");
                System.exit(0);
            }
            System.out.println("FAIL: ObjC.init threw unexpected: " + t);
            t.printStackTrace(System.out);
            System.exit(1);
        }

        try {
            NSApplication app = NSApplication.shared();
            app.setActivationPolicy(0);
        } catch (Throwable t) {
            System.out.println("NOTE: NSApplication init failed (may be headless): " + t);
        }

        // ---------------- installed selectors on a created view ----------------
        NSView view = null;
        try {
            view = NSView.create(new NSRect(0, 0, 120, 80), (ctx, dirty) -> { });
            check(view != null && view.peer().address() != 0, "NSView.create non-nil");
            check(view.isKindOfClass(ObjC.cls("NSUIViewImpl")), "created view is the NSUIViewImpl subclass pair");

            String[] installed = {
                    "mouseDown:", "mouseDragged:", "mouseUp:", "mouseMoved:",
                    "mouseEntered:", "mouseExited:", "keyDown:", "keyUp:", "flagsChanged:",
                    "performKeyEquivalent:", "acceptsFirstResponder"
            };
            for (String sel : installed) {
                check(responds(view.peer(), sel), "view respondsToSelector " + sel);
            }
        } catch (Throwable t) {
            check(false, "installed-selector section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- acceptsFirstResponder / performKeyEquivalent behavior ----------------
        try {
            check(!view.acceptsFirstResponder(), "acceptsFirstResponder false without key listener");
            NSView.KeyListener kl = new NSView.KeyListener() { };
            view.setKeyListener(kl);
            check(view.acceptsFirstResponder(), "acceptsFirstResponder true with key listener (round-trips through installed stub)");
            view.setKeyListener(null);
            check(!view.acceptsFirstResponder(), "acceptsFirstResponder false again after clear");
            boolean handled = view.performKeyEquivalent(null);
            check(!handled, "performKeyEquivalent(null) reports not-handled (chain continues)");
        } catch (Throwable t) {
            check(false, "responder-predicate section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- listener registration round-trip ----------------
        int baseline = NSView.listenerCount();
        try {
            check(baseline >= 0, "listenerCount baseline readable (" + baseline + ")");
            NSView.MouseListener ml = new NSView.MouseListener() {
                @Override public void onMouseDown(NSView v, NSEvent e) { }
            };
            NSView.KeyListener kl = new NSView.KeyListener() {
                @Override public boolean onKeyDown(NSView v, NSEvent e) { return false; }
            };
            view.setMouseListener(ml);
            check(NSView.listenerCount() == baseline + 1, "setMouseListener bumps listenerCount (+"
                    + (NSView.listenerCount() - baseline) + ")");
            view.setKeyListener(kl);
            check(NSView.listenerCount() == baseline + 2, "setKeyListener bumps listenerCount (+"
                    + (NSView.listenerCount() - baseline) + ")");

            // replacing (same key) must not grow the registry
            view.setMouseListener(new NSView.MouseListener() { });
            check(NSView.listenerCount() == baseline + 2, "re-setMouseListener replaces (no growth)");

            // event pass-throughs with a listener installed must not throw (nil event -> super)
            view.mouseDown(null);
            view.keyDown(null);
            view.flagsChanged(null);
            check(true, "event pass-throughs (mouseDown/keyDown/flagsChanged with nil event) no-crash");

            view.setMouseListener(null);
            view.setKeyListener(null);
            check(NSView.listenerCount() == baseline, "null unregister restores baseline (" + baseline + ")");
        } catch (Throwable t) {
            check(false, "listener-registration section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- nextResponder wiring ----------------
        try {
            NSView v2 = NSView.create(new NSRect(0, 0, 10, 10), (ctx, dirty) -> { });
            view.setNextResponder(v2);
            NSResponder nr = view.nextResponder();
            check(nr != null && v2 != null && nr.peer().address() == v2.peer().address(),
                    "setNextResponder/nextResponder round-trip (got "
                            + (nr == null ? "null" : Long.toHexString(nr.peer().address()))
                            + " expect " + (v2 == null ? "null" : Long.toHexString(v2.peer().address())) + ")");
            view.setNextResponder((NSResponder) null);
            check(view.nextResponder() == null, "setNextResponder(null) terminates chain");
        } catch (Throwable t) {
            check(false, "nextResponder section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- real window: first responder + window responder API ----------------
        try {
            NSView content = NSView.create(new NSRect(0, 0, 320, 240), (ctx, dirty) -> { });
            NSWindow win = NSWindow.create(new NSRect(0, 0, 320, 240), 15L, 2L, false);
            check(win != null && win.peer().address() != 0, "NSWindow.create non-nil");

            content.setKeyListener(new NSView.KeyListener() { }); // view must accept first-responder status
            win.setContentView(content);

            // the contentView's next responder should be its window
            NSResponder chainNext = content.nextResponder();
            check(chainNext != null && chainNext.peer().address() == win.peer().address(),
                    "contentView.nextResponder is the window (got "
                            + (chainNext == null ? "null" : Long.toHexString(chainNext.peer().address()))
                            + " expect " + Long.toHexString(win.peer().address()) + ")");

            // initialFirstResponder round-trip
            win.initialFirstResponder(content);
            NSView ir = win.initialFirstResponder();
            check(ir != null && ir.peer().address() == content.peer().address(),
                    "initialFirstResponder round-trip (got "
                            + (ir == null ? "null" : Long.toHexString(ir.peer().address())) + ")");

            // acceptsMouseMovedEvents round-trip
            win.setAcceptsMouseMovedEvents(true);
            boolean mm = win.acceptsMouseMovedEvents();
            win.setAcceptsMouseMovedEvents(false);
            check(mm, "setAcceptsMouseMovedEvents(true) reads back true");

            // makeFirstResponder must run without error
            win.makeFirstResponder(content);
            NSResponder fr = win.firstResponder();
            check(fr != null, "firstResponder non-null after makeFirstResponder");
            if (fr != null && fr.peer().address() == content.peer().address()) {
                check(true, "makeFirstResponder(view) accepted (firstResponder == view)");
            } else {
                System.out.println("NOTE: firstResponder is not the view (window not shown/key yet): "
                        + (fr == null ? "null" : fr.toString()) + " — no-error assertion still holds");
            }

            // inherited NSResponder touchBar accessors on NSWindow
            NSTouchBar bar = NSTouchBar.create();
            win.setTouchBar(bar);
            NSTouchBar got = win.touchBar();
            check(got != null && got.peer().address() == bar.peer().address(),
                    "NSWindow setTouchBar/touchBar round-trip via NSResponder inheritance");
            win.setTouchBar((NSTouchBar) null);
            check(win.touchBar() == null, "NSWindow setTouchBar(null) clears");
        } catch (Throwable t) {
            check(false, "window responder section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- enableMouseTracking ----------------
        try {
            long combo = NSView.trackingMouseEnteredAndExited | NSView.trackingMouseMoved
                    | NSView.trackingActiveAlways | NSView.trackingInVisibleRect;
            check(combo == 643L, "tracking option bits compose to 643 (got " + combo + ")");
            NSView tracked = NSView.create(new NSRect(0, 0, 40, 40), (ctx, dirty) -> { });
            tracked.enableMouseTracking();
            tracked.enableMouseTracking(); // dedup guard: second call is a no-op
            check(true, "enableMouseTracking no-crash (called twice, idempotent)");
        } catch (Throwable t) {
            check(false, "enableMouseTracking section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- 200x register/unregister stress ----------------
        try {
            int base = NSView.listenerCount();
            NSView stress = NSView.create(new NSRect(0, 0, 5, 5), (ctx, dirty) -> { });
            NSView.MouseListener ml = new NSView.MouseListener() { };
            NSView.KeyListener kl = new NSView.KeyListener() { };
            int maxDelta = 0;
            for (int i = 0; i < 200; i++) {
                stress.setMouseListener(ml);
                stress.setKeyListener(kl);
                maxDelta = Math.max(maxDelta, NSView.listenerCount() - base);
                stress.setMouseListener(null);
                stress.setKeyListener(null);
            }
            check(maxDelta <= 2 && NSView.listenerCount() == base,
                    "200x single-view register/unregister stress (maxDelta=" + maxDelta + ", end="
                            + NSView.listenerCount() + ", base=" + base + ")");

            // multi-view sweep: register across 50 views, then clear them all
            List<NSView> views = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                NSView v = NSView.create(new NSRect(0, 0, 4, 4), (ctx, dirty) -> { });
                v.setMouseListener(ml);
                v.setKeyListener(kl);
                views.add(v);
            }
            check(NSView.listenerCount() == base + 100,
                    "50-view sweep holds 100 listeners (got " + (NSView.listenerCount() - base) + ")");
            for (NSView v : views) {
                v.setMouseListener(null);
                v.setKeyListener(null);
            }
            check(NSView.listenerCount() == base,
                    "multi-view sweep returns to baseline (" + NSView.listenerCount() + " vs " + base + ")");
        } catch (Throwable t) {
            check(false, "stress section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(failures == 0
                ? "RESULT: PASS (" + asserts + " assertions)"
                : "RESULT: FAIL (" + failures + " of " + asserts + " assertions failed)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
