package nsui.tests;

import nsui.CABasicAnimation;
import nsui.CAKeyframeAnimation;
import nsui.CAMediaTimingFunction;
import nsui.CASpringAnimation;
import nsui.CATransition;
import nsui.CAValueFunction;
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

        TestKit.end();
    }
}
