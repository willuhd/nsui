package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAKeyframeAnimation — multi-stop animation over values/keyTimes (optionally
/// paced, cubic, or rotating). Thin stateless wrapper; follows CABasicAnimation.
/// (CGPathRef path omitted: needs a CGPath wrapper.)
public class CAKeyframeAnimation extends CAPropertyAnimation {

    /// Calculation modes.
    public static final String CALCULATION_LINEAR = "linear";
    public static final String CALCULATION_DISCRETE = "discrete";
    public static final String CALCULATION_PACED = "paced";
    public static final String CALCULATION_CUBIC = "cubic";
    public static final String CALCULATION_CUBIC_PACED = "cubicPaced";

    /// Rotation modes.
    public static final String ROTATION_LINEAR = "linear";
    public static final String ROTATION_AUTO = "auto";
    public static final String ROTATION_AUTO_REVERSE = "autoReverse";

    private record Handles(MethodHandle hWithKeyPath) {}
    private static volatile Handles handles;

    protected CAKeyframeAnimation(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAKeyframeAnimation wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAKeyframeAnimation(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID)));
    }

    /// animationWithKeyPath:.
    public static CAKeyframeAnimation create(String keyPath) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithKeyPath().invokeExact(
                    ObjC.cls("CAKeyframeAnimation"), ObjC.sel("animationWithKeyPath:"),
                    (MemorySegment) (keyPath == null ? MemorySegment.NULL : ObjC.nsstring(keyPath)));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("animationWithKeyPath: failed for CAKeyframeAnimation", t);
        }
    }

    /// values — the keyframe objects.
    public NSArray values() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("values")));
    }

    /// setValues:.
    public void setValues(NSArray values) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setValues:"),
                (MemorySegment) (values == null ? MemorySegment.NULL : values.peer()));
    }

    /// keyTimes — fractional times (NSNumber 0..1).
    public NSArray keyTimes() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("keyTimes")));
    }

    /// setKeyTimes:.
    public void setKeyTimes(NSArray times) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setKeyTimes:"),
                (MemorySegment) (times == null ? MemorySegment.NULL : times.peer()));
    }

    /// timingFunctions — per-segment easing.
    public NSArray timingFunctions() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("timingFunctions")));
    }

    /// setTimingFunctions:.
    public void setTimingFunctions(NSArray functions) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTimingFunctions:"),
                (MemorySegment) (functions == null ? MemorySegment.NULL : functions.peer()));
    }

    /// calculationMode (nil-safe).
    public String calculationMode() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("calculationMode")));
    }

    /// setCalculationMode:.
    public void setCalculationMode(String mode) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setCalculationMode:"), ObjC.nsstring(mode));
    }

    /// rotationMode (nil-safe).
    public String rotationMode() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("rotationMode")));
    }

    /// setRotationMode:.
    public void setRotationMode(String mode) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setRotationMode:"), ObjC.nsstring(mode));
    }
}
