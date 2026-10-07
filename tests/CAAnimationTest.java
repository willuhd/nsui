package nsui.tests;

import nsui.CABasicAnimation;
import nsui.CAKeyframeAnimation;
import nsui.CALayer;
import nsui.CAMediaTimingFunction;
import nsui.CASpringAnimation;
import nsui.CATransaction;
import nsui.CATransition;
import nsui.CAValueFunction;
import nsui.NSApplication;
import nsui.NSArray;
import nsui.NSRect;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// Committed coverage for the animation tier: keyframe/spring/transition
/// construction plus timing-function and value-function round-trips.
/// Pure objects (no windows, no runloop); mirrors the pre-commit smoke run.
public final class CAAnimationTest {

    public static void main(String[] args) {
        System.out.println("=== CAAnimationTest — animation tier ===");
        ObjC.init();

        try {
            CAKeyframeAnimation kf = CAKeyframeAnimation.create("position");
            TestKit.check(kf != null, "keyframe create");
            kf.setCalculationMode(CAKeyframeAnimation.CALCULATION_PACED);
            TestKit.check(CAKeyframeAnimation.CALCULATION_PACED.equals(kf.calculationMode()),
                    "calculationMode round-trip");
            kf.setRotationMode(CAKeyframeAnimation.ROTATION_AUTO);
            TestKit.check(CAKeyframeAnimation.ROTATION_AUTO.equals(kf.rotationMode()),
                    "rotationMode round-trip");
        } catch (Throwable t) {
            TestKit.check(false, "keyframe section threw: " + t);
        }

        try {
            CASpringAnimation sp = CASpringAnimation.create("position");
            sp.setMass(2.0);
            TestKit.check(sp.mass() == 2.0, "spring mass round-trip");
            sp.setStiffness(100.0);
            TestKit.check(sp.stiffness() == 100.0, "spring stiffness round-trip");
            sp.setDamping(10.0);
            TestKit.check(sp.damping() == 10.0, "spring damping round-trip");
            sp.setInitialVelocity(1.5);
            TestKit.check(sp.initialVelocity() == 1.5, "spring initialVelocity round-trip");
            sp.setAllowsOverdamping(true);
            TestKit.check(sp.allowsOverdamping(), "spring allowsOverdamping round-trip");
            TestKit.check(sp.settlingDuration() > 0,
                    "spring settlingDuration positive (got " + sp.settlingDuration() + ")");
        } catch (Throwable t) {
            TestKit.check(false, "spring section threw: " + t);
        }

        try {
            CATransition tr = CATransition.create();
            TestKit.check(tr != null, "transition create");
            tr.setType(CATransition.TYPE_FADE);
            TestKit.check(CATransition.TYPE_FADE.equals(tr.type()), "transition type round-trip");
            tr.setSubtype(CATransition.FROM_LEFT);
            TestKit.check(CATransition.FROM_LEFT.equals(tr.subtype()), "transition subtype round-trip");
            tr.setStartProgress(0.25f);
            TestKit.check(tr.startProgress() == 0.25f, "transition startProgress round-trip");
            tr.setEndProgress(0.75f);
            TestKit.check(tr.endProgress() == 0.75f, "transition endProgress round-trip");
        } catch (Throwable t) {
            TestKit.check(false, "transition section threw: " + t);
        }

        try {
            CAMediaTimingFunction named =
                    CAMediaTimingFunction.functionWithName(CAMediaTimingFunction.NAME_EASE_IN_EASE_OUT);
            TestKit.check(named != null, "named timing function");
            CAMediaTimingFunction bez =
                    CAMediaTimingFunction.functionWithControlPoints(0.25f, 0.1f, 0.25f, 1.0f);
            TestKit.check(bez != null, "bezier timing function");
            float[] cp = bez.controlPointAtIndex(1);
            TestKit.check(cp.length == 2 && Math.abs(cp[0] - 0.25f) < 0.001 && Math.abs(cp[1] - 0.1f) < 0.001,
                    "control point reads back (got " + cp[0] + "," + cp[1] + ")");
        } catch (Throwable t) {
            TestKit.check(false, "timing section threw: " + t);
        }

        try {
            CAValueFunction vf = CAValueFunction.functionWithName("rotateX");
            TestKit.check(vf != null && "rotateX".equals(vf.name()), "value function round-trip");
        } catch (Throwable t) {
            TestKit.check(false, "value-function section threw: " + t);
        }

        try {
            CABasicAnimation b = CABasicAnimation.create("opacity");
            TestKit.check(b != null, "basic animation still creates (regression)");
        } catch (Throwable t) {
            TestKit.check(false, "basic regression threw: " + t);
        }

        try {
            liveTransactionProof();
        } catch (Throwable t) {
            TestKit.check(false, "live animation section threw: " + t);
        }

        TestKit.end();
    }

    /// A real animation through an explicit CATransaction: begin, short
    /// duration, add an opacity 0->1 CABasicAnimation, commit — then prove the
    /// engine ran via animationKeys and a mid-flight presentationLayer read.
    private static void liveTransactionProof() throws InterruptedException {
        NSApplication app = TestKit.app();
        NSWindow win = TestKit.hiddenWindow(200, 200);
        NSView view = NSView.create(new NSRect(0, 0, 200, 200), (ctx, d) -> {});
        view.setWantsLayer(true);
        win.setContentView(view);
        TestKit.show(win);
        try {
            CALayer host = view.layer();
            TestKit.check(host != null, "host view has a layer to animate under");
            if (host == null) return;
            CALayer l = CALayer.create();
            l.setBounds(new NSRect(0, 0, 100, 100));
            host.addSublayer(l);
            l.setOpacity(0.0f);

            CABasicAnimation anim = CABasicAnimation.create("opacity");
            anim.setFromDouble(0);
            anim.setToDouble(1);
            anim.setDuration(0.5);
            CATransaction.begin();
            CATransaction.setAnimationDuration(0.5);
            l.addAnimation(anim, "teeth-opacity");
            CATransaction.commit();

            NSArray keys = l.animationKeys();
            boolean found = false;
            if (keys != null) {
                for (long i = 0; i < keys.count(); i++) {
                    if ("teeth-opacity".equals(ObjC.toString(keys.objectAtIndex(i)))) { found = true; break; }
                }
            }
            TestKit.check(found, "animationKeys contains the committed key (engine accepted the add)");

            app.pumpFor(150);
            java.lang.foreign.MemorySegment presPeer =
                    ObjC.msgSendId(l.peer(), ObjC.sel("presentationLayer"));
            CALayer pres = CALayer.wrap(presPeer);
            double model = l.opacity();
            double shown = pres == null ? Double.NaN : pres.opacity();
            TestKit.check(pres != null && Math.abs(shown - model) > 0.02,
                    "presentationLayer mid-flight differs from the model (model=" + model + ", shown=" + shown + ")");
            l.removeAnimationForKey("teeth-opacity");
        } finally {
            TestKit.close(win);
        }
    }
}
