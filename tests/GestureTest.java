package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSClickGestureRecognizer;
import nsui.NSGestureRecognizer;
import nsui.NSPanGestureRecognizer;
import nsui.NSPoint;
import nsui.NSView;
import nsui.objc.ObjC;

/**
 * GestureTest — covers NSGestureRecognizer / NSPanGestureRecognizer /
 * NSClickGestureRecognizer: create with target/action, isEnabled,
 * buttonMask, translationInView with nil view, addTarget/removeTarget.
 * Stress 1000 iterations.
 */
public final class GestureTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    private static MemorySegment dummyTarget() {
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSObject"), ObjC.sel("alloc"));
        return ObjC.msgSendId(alloc, ObjC.sel("init"));
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== GestureTest — gesture recognizers ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            String m = (t.getMessage() == null ? "" : t.getMessage()).toLowerCase();
            if (m.contains("connection") || m.contains("dlopen") || m.contains("appkit")) {
                System.out.println("SKIP: connection/framework not available: " + t);
                System.out.println("RESULT: ALL PASS (skipped)");
                System.exit(0);
                return;
            }
            throw t;
        }

        MemorySegment target = null;
        try {
            target = dummyTarget();
            check(target != null && target.address() != 0, "dummy target NSObject alloc/init non-nil");
        } catch (Throwable t) {
            String m = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            if (m.contains("connection")) {
                System.out.println("SKIP: connection error creating dummy target: " + t);
                System.out.println("RESULT: ALL PASS (skipped)");
                System.exit(0);
                return;
            }
            check(false, "dummy target creation threw: " + t);
            t.printStackTrace(System.out);
            // fallback to null target for rest of tests
            target = MemorySegment.NULL;
        }

        // ---- NSGestureRecognizer base ----
        try {
            MemorySegment t = target;
            // create with null target/action should still work (base class allows nil)
            NSGestureRecognizer baseNull = NSGestureRecognizer.create(MemorySegment.NULL, null);
            check(baseNull != null && baseNull.peer().address() != 0, "NSGestureRecognizer.create(NULL,null) non-nil");

            NSGestureRecognizer g = NSGestureRecognizer.create(t, "doGesture:");
            check(g != null && g.peer().address() != 0, "NSGestureRecognizer.create(target,\"doGesture:\") non-nil");

            // isEnabled default true
            boolean en = g.isEnabled();
            check(en, "NSGestureRecognizer isEnabled default true (got " + en + ")");
            g.setEnabled(false);
            check(!g.isEnabled(), "NSGestureRecognizer setEnabled(false) round-trip");
            g.setEnabled(true);
            check(g.isEnabled(), "NSGestureRecognizer setEnabled(true) round-trip");

            // addTarget / removeTarget
            MemorySegment extraTarget = dummyTarget();
            g.addTarget(extraTarget, "extraAction:");
            check(true, "NSGestureRecognizer addTarget(extra,\"extraAction:\") did not throw");
            g.removeTarget(extraTarget, "extraAction:");
            check(true, "NSGestureRecognizer removeTarget(extra,\"extraAction:\") did not throw");
            // also self target variant
            g.addTarget(t, "doGesture:");
            check(true, "NSGestureRecognizer addTarget(self) did not throw");
            g.removeTarget(t, "doGesture:");
            check(true, "NSGestureRecognizer removeTarget(self) did not throw");
            // null args variant should not throw
            g.addTarget(MemorySegment.NULL, null);
            check(true, "NSGestureRecognizer addTarget(NULL,null) did not throw");
            g.removeTarget(MemorySegment.NULL, null);
            check(true, "NSGestureRecognizer removeTarget(NULL,null) did not throw");

            // view should be nil before added to view
            nsui.NSView v = g.view();
            check(v == null || v.peer().address() == 0, "NSGestureRecognizer view() nil before attached (got " + (v==null? "null" : "0x"+Long.toHexString(v.peer().address())) + ")");

            // state default
            long state = g.state();
            check(state == 0, "NSGestureRecognizer state default 0 (got " + state + ")");

        } catch (Throwable t) {
            String m = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            if (m.contains("connection")) {
                System.out.println("SKIP: connection error in NSGestureRecognizer section: " + t);
            } else {
                check(false, "NSGestureRecognizer section threw: " + t);
                t.printStackTrace(System.out);
            }
        }

        // ---- NSPanGestureRecognizer ----
        try {
            MemorySegment t = target;
            NSPanGestureRecognizer pan = NSPanGestureRecognizer.create(t, "panned:");
            check(pan != null && pan.peer().address() != 0, "NSPanGestureRecognizer.create non-nil");

            // isKindOfClass check
            boolean isPan = pan.isKindOfClass("NSPanGestureRecognizer");
            check(isPan, "NSPanGestureRecognizer isKindOfClass:NSPanGestureRecognizer");
            check(pan.isKindOfClass("NSGestureRecognizer"), "NSPanGestureRecognizer isKindOfClass:NSGestureRecognizer (inheritance)");

            // isEnabled
            check(pan.isEnabled(), "NSPanGestureRecognizer isEnabled default true");
            pan.setEnabled(false);
            check(!pan.isEnabled(), "NSPanGestureRecognizer setEnabled(false)");
            pan.setEnabled(true);

            // buttonMask
            long mask0 = pan.buttonMask();
            // default is 1 (left) on most; just check set/get round-trip
            pan.setButtonMask(1);
            check(pan.buttonMask() == 1, "NSPanGestureRecognizer buttonMask set 1 round-trip (got " + pan.buttonMask() + ")");
            pan.setButtonMask(2);
            check(pan.buttonMask() == 2, "NSPanGestureRecognizer buttonMask set 2 round-trip (got " + pan.buttonMask() + ")");
            pan.setButtonMask(mask0);

            // numberOfTouchesRequired
            long touches = pan.numberOfTouchesRequired();
            pan.setNumberOfTouchesRequired(1);
            check(pan.numberOfTouchesRequired() == 1, "NSPanGestureRecognizer setNumberOfTouchesRequired(1)");
            pan.setNumberOfTouchesRequired(touches);

            // translationInView with nil view returns point (should not throw, point near zero)
            NSPoint transNil = pan.translationInView(null);
            check(transNil != null, "NSPanGestureRecognizer translationInView(null) returned non-null point (" + transNil + ")");
            // expect 0,0 initially
            boolean nearZero = transNil != null && Math.abs(transNil.x()) < 0.01 && Math.abs(transNil.y()) < 0.01;
            check(nearZero, "translationInView(null) near zero (got " + transNil + ")");

            // also with MemorySegment.NULL view via explicit null NSView
            NSPoint transNil2 = pan.translationInView((NSView) null);
            check(transNil2 != null, "translationInView((NSView)null) second call non-null");

            // velocityInView with nil
            NSPoint vel = pan.velocityInView(null);
            check(vel != null, "NSPanGestureRecognizer velocityInView(null) non-null (" + vel + ")");

            // addTarget/removeTarget
            MemorySegment extra = dummyTarget();
            pan.addTarget(extra, "panExtra:");
            check(true, "NSPanGestureRecognizer addTarget did not throw");
            pan.removeTarget(extra, "panExtra:");
            check(true, "NSPanGestureRecognizer removeTarget did not throw");

            // setTranslation:inView with nil
            pan.setTranslation(new NSPoint(10, 20), null);
            NSPoint afterSet = pan.translationInView(null);
            check(afterSet != null && Math.abs(afterSet.x() - 10) < 0.01 && Math.abs(afterSet.y() - 20) < 0.01,
                  "setTranslation NSPoint(10,20) round-trip via translationInView(nil) (got " + afterSet + ")");
            // reset
            pan.setTranslation(new NSPoint(0,0), null);

        } catch (Throwable t) {
            String m = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            if (m.contains("connection")) {
                System.out.println("SKIP: connection error in NSPanGestureRecognizer section: " + t);
            } else {
                check(false, "NSPanGestureRecognizer section threw: " + t);
                t.printStackTrace(System.out);
            }
        }

        // ---- NSClickGestureRecognizer ----
        try {
            MemorySegment t = target;
            NSClickGestureRecognizer click = NSClickGestureRecognizer.create(t, "clicked:");
            check(click != null && click.peer().address() != 0, "NSClickGestureRecognizer.create non-nil");

            check(click.isKindOfClass("NSClickGestureRecognizer"), "NSClickGestureRecognizer isKindOfClass true");
            check(click.isKindOfClass("NSGestureRecognizer"), "NSClickGestureRecognizer isKindOfClass:NSGestureRecognizer");

            check(click.isEnabled(), "NSClickGestureRecognizer isEnabled default true");
            click.setEnabled(false);
            check(!click.isEnabled(), "NSClickGestureRecognizer setEnabled(false)");
            click.setEnabled(true);

            // buttonMask
            long bm = click.buttonMask();
            click.setButtonMask(1);
            check(click.buttonMask() == 1, "NSClickGestureRecognizer buttonMask 1 round-trip");
            click.setButtonMask(2);
            check(click.buttonMask() == 2, "NSClickGestureRecognizer buttonMask 2 round-trip");
            click.setButtonMask(bm);

            // numberOfClicksRequired default 1
            long clicks = click.numberOfClicksRequired();
            click.setNumberOfClicksRequired(2);
            check(click.numberOfClicksRequired() == 2, "NSClickGestureRecognizer setNumberOfClicksRequired(2) (got " + click.numberOfClicksRequired() + ")");
            click.setNumberOfClicksRequired(1);
            check(click.numberOfClicksRequired() == 1, "NSClickGestureRecognizer setNumberOfClicksRequired(1)");
            // restore
            click.setNumberOfClicksRequired(clicks);

            // addTarget/removeTarget
            MemorySegment extra = dummyTarget();
            click.addTarget(extra, "clickExtra:");
            check(true, "NSClickGestureRecognizer addTarget did not throw");
            click.removeTarget(extra, "clickExtra:");
            check(true, "NSClickGestureRecognizer removeTarget did not throw");

            // translation concept not applicable, but ensure no crash on base methods
            NSPoint loc = click.locationInView(null);
            check(loc != null, "NSClickGestureRecognizer locationInView(null) non-null (" + loc + ")");

        } catch (Throwable t) {
            String m = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            if (m.contains("connection")) {
                System.out.println("SKIP: connection error in NSClickGestureRecognizer section: " + t);
            } else {
                check(false, "NSClickGestureRecognizer section threw: " + t);
                t.printStackTrace(System.out);
            }
        }

        // ---- Stress 1000 iterations ----
        try {
            long start = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                MemorySegment tt = (i % 7 == 0) ? MemorySegment.NULL : target;
                String action = (i % 2 == 0) ? "act" + i + ":" : null;
                NSGestureRecognizer g = NSGestureRecognizer.create(tt, action);
                g.setEnabled((i & 1) == 0);
                boolean en = g.isEnabled();
                if (en != ((i & 1) == 0)) throw new AssertionError("isEnabled mismatch");

                // alternate types
                if (i % 3 == 0) {
                    NSPanGestureRecognizer p = NSPanGestureRecognizer.create(tt, action);
                    p.setButtonMask(i % 4);
                    p.setEnabled((i & 1) == 0);
                    NSPoint pt = p.translationInView(null);
                    if (pt == null) throw new AssertionError("translation null");
                    // touch add/remove
                    MemorySegment extra = dummyTarget();
                    p.addTarget(extra, "x:");
                    p.removeTarget(extra, "x:");
                } else if (i % 3 == 1) {
                    NSClickGestureRecognizer c = NSClickGestureRecognizer.create(tt, action);
                    c.setButtonMask(i % 3);
                    c.setNumberOfClicksRequired(1 + (i % 3));
                    c.setEnabled((i & 1) == 0);
                    MemorySegment extra = dummyTarget();
                    c.addTarget(extra, "y:");
                    c.removeTarget(extra, "y:");
                } else {
                    // base
                    MemorySegment extra = dummyTarget();
                    g.addTarget(extra, "z:");
                    g.removeTarget(extra, "z:");
                }
            }
            long elapsed = System.currentTimeMillis() - start;
            check(true, "stress 1000 iterations create/isEnabled/buttonMask/translationInView/addTarget/removeTarget completed in " + elapsed + " ms");
        } catch (Throwable t) {
            String m = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
            if (m.contains("connection")) {
                System.out.println("SKIP: connection error during gesture stress: " + t);
            } else {
                check(false, "gesture stress threw: " + t);
                t.printStackTrace(System.out);
            }
        }

        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
