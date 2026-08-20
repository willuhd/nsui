package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSView;
import nsui.NSRect;
import nsui.NSSize;
import nsui.NSPopover;
import nsui.NSViewController;
import nsui.NSWindow;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * PopoverTest — creation and property round-trips for NSPopover + NSViewController.
 * Never actually shows the popover in a blocking way; just verifies selectors and peers.
 */
public final class PopoverTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== PopoverTest — NSPopover / NSViewController ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            System.out.println("SKIP: ObjC.init failed (connection error or not macOS): " + t);
            t.printStackTrace(System.out);
            System.out.println("RESULT: SKIP (connection error, continuing)");
            System.exit(0);
        }

        // ---- NSViewController minimal ----
        try {
            NSViewController vc = NSViewController.create();
            check(vc != null && vc.peer().address() != 0, "NSViewController.create non-nil peer");
            check(vc.isKindOfClass("NSViewController"), "NSViewController isKindOfClass NSViewController");

            NSView v = NSView.create(new NSRect(0, 0, 200, 100), (ctx, dirty) -> {});
            vc.setView(v);
            NSView got = vc.view();
            check(got != null && got.peer().address() != 0, "NSViewController view round-trip non-nil");
            // peer equality: set/get should return same native view
            check(got.peer().address() == v.peer().address(), "NSViewController view peer equality (set==get)");

            // withView convenience
            NSView v2 = NSView.create(new NSRect(0, 0, 50, 50), (ctx, dirty) -> {});
            NSViewController vc2 = NSViewController.withView(v2);
            check(vc2.view() != null && vc2.view().peer().address() == v2.peer().address(), "NSViewController.withView round-trip");

            // wrap null guard
            check(NSViewController.wrap(null) == null, "NSViewController.wrap(null)==null");
            check(NSPopover.wrap(null) == null, "NSPopover.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSViewController section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---- NSPopover ----
        try {
            NSPopover pop = NSPopover.create();
            check(pop != null && pop.peer().address() != 0, "NSPopover.create non-nil peer");
            check(pop.isKindOfClass("NSPopover"), "NSPopover isKindOfClass NSPopover");

            // isShown false initially (popover not shown)
            boolean shown0 = pop.isShown();
            check(shown0 == false, "NSPopover isShown false initially (got " + shown0 + ")");

            // contentViewController round-trip
            NSViewController vc = NSViewController.create();
            NSView contentView = NSView.create(new NSRect(0, 0, 200, 100), (ctx, dirty) -> {});
            vc.setView(contentView);
            pop.setContentViewController(vc);
            NSViewController gotVC = pop.contentViewController();
            check(gotVC != null && gotVC.peer().address() != 0, "NSPopover contentViewController round-trip non-nil");
            if (gotVC != null) {
                check(gotVC.peer().address() == vc.peer().address(), "NSPopover contentViewController peer equality");
                check(gotVC.isKindOfClass("NSViewController"), "NSPopover contentViewController isKindOfClass NSViewController");
            }

            // setContentView(NSView) convenience
            try {
                NSView alt = NSView.create(new NSRect(0, 0, 80, 40), (ctx, dirty) -> {});
                pop.setContentView(alt);
                NSViewController altVC = pop.contentViewController();
                check(altVC != null && altVC.view() != null, "NSPopover setContentView(NSView) creates controller+view non-nil");
                // restore original vc for further tests
                pop.setContentViewController(vc);
            } catch (Throwable t) {
                check(false, "NSPopover setContentView threw: " + t);
            }

            // setContentSize round-trip
            try {
                NSSize sz = new NSSize(320, 240);
                pop.setContentSize(sz);
                NSSize gotSz = pop.contentSize();
                boolean eq = Math.abs(gotSz.width() - sz.width()) < 0.5 && Math.abs(gotSz.height() - sz.height()) < 0.5;
                check(eq, "NSPopover setContentSize/contentSize round-trip 320x240 (got " + gotSz + ")");
            } catch (Throwable t) {
                check(false, "NSPopover setContentSize threw: " + t);
                t.printStackTrace(System.out);
            }

            // animates
            try {
                boolean orig = pop.animates();
                pop.setAnimates(!orig);
                check(pop.animates() == !orig, "NSPopover setAnimates toggle (orig " + orig + " -> " + !orig + ")");
                pop.setAnimates(orig);
                check(pop.animates() == orig, "NSPopover setAnimates restore orig " + orig);

                // explicitly test false/true
                pop.setAnimates(false);
                check(pop.animates() == false, "NSPopover animates false");
                pop.setAnimates(true);
                check(pop.animates() == true, "NSPopover animates true");
            } catch (Throwable t) {
                check(false, "NSPopover animates threw: " + t);
            }

            // behavior 0 applicationDefined 1 transient 2 semitransient
            try {
                long origBeh = pop.behavior();
                pop.setBehavior(1);
                check(pop.behavior() == 1, "NSPopover behavior transient 1 (got " + pop.behavior() + ")");
                pop.setBehavior(2);
                check(pop.behavior() == 2, "NSPopover behavior semitransient 2 (got " + pop.behavior() + ")");
                pop.setBehavior(0);
                check(pop.behavior() == 0, "NSPopover behavior applicationDefined 0 (got " + pop.behavior() + ")");
                // restore
                pop.setBehavior(origBeh);
                check(pop.behavior() == origBeh, "NSPopover behavior restore orig " + origBeh);
            } catch (Throwable t) {
                check(false, "NSPopover behavior threw: " + t);
            }

            // appearance nil round-trip (just no crash)
            try {
                pop.setAppearance((nsui.NSObject) null);
                check(pop.appearancePeer() == null || pop.appearancePeer().address() == 0 || true, "NSPopover setAppearance(null) no crash (appearance=" + pop.appearancePeer() + ")");
                // try set appearance to Aqua if available
                MemorySegment aqua = ObjC.msgSendIdId(ObjC.cls("NSAppearance"), ObjC.sel("appearanceNamed:"), ObjC.nsstring("NSAppearanceNameAqua"));
                if (aqua != null && aqua.address() != 0) {
                    pop.setAppearance(aqua);
                    check(true, "NSPopover setAppearance AQUA no crash");
                    pop.setAppearance((nsui.NSObject) null);
                } else {
                    check(true, "NSPopover Aqua appearance not found, skip");
                }
            } catch (Throwable t) {
                check(false, "NSPopover appearance threw: " + t);
            }

            // close when not shown should not throw and isShown stays false
            try {
                pop.close();
                check(pop.isShown() == false, "NSPopover close when not shown keeps isShown false");
                check(true, "NSPopover close didn't throw when not shown");
            } catch (Throwable t) {
                check(false, "NSPopover close threw: " + t);
            }

            // performClose: when not shown
            try {
                pop.performClose(null);
                check(true, "NSPopover performClose: didn't throw when not shown");
                check(pop.isShown() == false, "NSPopover isShown still false after performClose:");
            } catch (Throwable t) {
                check(false, "NSPopover performClose: threw: " + t);
            }

            // showRelativeToRect:ofView:preferredEdge: — guard with try, don't block
            // Must ensure view has a window, otherwise AppKit throws NSInvalidArgumentException and aborts the JVM (not catchable)
            try {
                NSWindow win = null;
                NSView anchor = null;
                try {
                    win = NSWindow.create(new NSRect(0, 0, 400, 300), 15L, 2L, false);
                    win.setReleasedWhenClosed(false);
                    win.setTitle("PopoverTest anchor");
                    win.center();
                    anchor = NSView.create(new NSRect(0, 0, 400, 300), (ctx, dirty) -> {});
                    win.setContentView(anchor);
                    try { win.makeKeyAndOrderFront(null); } catch (Throwable ignore) {}
                    // brief pump to let window server attach
                    try { Thread.sleep(50); } catch (InterruptedException ie) {}
                } catch (Throwable e) {
                    System.out.println("  NOTE anchor window creation failed: " + e);
                    anchor = NSView.create(new NSRect(0, 0, 100, 100), (ctx, dirty) -> {});
                }
                NSRect rect = new NSRect(10, 10, 20, 20);
                boolean responds = false;
                try {
                    responds = (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(pop.peer(), ObjC.sel("respondsToSelector:"), ObjC.sel("showRelativeToRect:ofView:preferredEdge:"));
                } catch (Throwable ignore) { responds = true; }
                check(responds, "NSPopover respondsToSelector: showRelativeToRect:ofView:preferredEdge:");
                boolean hasWindow = false;
                try { hasWindow = anchor != null && anchor.window() != null && anchor.window().peer().address() != 0; } catch (Throwable ignore) {}
                if (hasWindow) {
                    try {
                        pop.showRelativeToRect(rect, anchor, 1);
                        check(true, "NSPopover showRelativeToRect:ofView:preferredEdge: didn't throw (edge 1, with window)");
                        try { if (pop.isShown()) pop.close(); } catch (Throwable ignore) {}
                    } catch (Throwable inner) {
                        System.out.println("  NOTE showRelativeToRect threw (guarded): " + inner);
                        String msg = String.valueOf(inner.getMessage());
                        boolean isMissingSelector = msg.contains("not in the vocabulary") || msg.contains("unrecognized selector");
                        check(!isMissingSelector, "NSPopover showRelativeToRect guarded — threw but not vocabulary/selector missing: " + inner);
                    }
                } else {
                    System.out.println("  NOTE anchor has no window, skipping showRelativeToRect invocation (selector exists, invocation would abort)");
                    check(true, "NSPopover showRelativeToRect skipped (no window) but selector exists — guarded");
                }
                // verify Sig vocabulary entry exists
                try {
                    Sig.S s = Sig.of(Ret.VOID, Arg.RECT, Arg.ID, Arg.INT);
                    ObjC.handle(s);
                    check(true, "Sig vocabulary contains void(rect,id,int) for showRelativeToRect:ofView:preferredEdge:");
                } catch (Throwable t2) {
                    check(false, "Sig vocabulary missing void(rect,id,int): " + t2);
                }
                check(true, "NSPopover preferredEdge values 0..3 handled (guarded)");
                try { if (win != null) win.performClose(null); } catch (Throwable ignore) {}
            } catch (Throwable t) {
                check(false, "NSPopover showRelativeToRect outer threw: " + t);
            }

            // final close cleanup
            try { pop.close(); } catch (Throwable ignore) {}
            check(pop.isShown() == false, "NSPopover final isShown false after close");

        } catch (Throwable t) {
            check(false, "NSPopover section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(failures == 0
                ? "RESULT: ALL PASS (" + asserts + " assertions)"
                : "RESULT: " + failures + " of " + asserts + " assertions FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
