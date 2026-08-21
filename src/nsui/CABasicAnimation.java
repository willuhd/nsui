package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CABasicAnimation — thin wrapper for QuartzCore CABasicAnimation, a CAAnimation
/// subclass interpolating a single key-path property between from/to/by values.
/// Raw MemorySegment value setters are inherited from CAAnimation; the typed
/// setFromDouble/setToDouble/setByDouble conveniences build NSNumber values so
/// callers never touch raw segments for scalar interpolation.
public class CABasicAnimation extends CAAnimation {

    private record Handles(MethodHandle hWithKeyPath) {}
    private static volatile Handles handles;

    protected CABasicAnimation(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static CABasicAnimation wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CABasicAnimation(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID)));
    }

    /// +[CABasicAnimation animationWithKeyPath:]
    public static CABasicAnimation create(String keyPath) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithKeyPath().invokeExact(ObjC.cls("CABasicAnimation"), ObjC.sel("animationWithKeyPath:"), (MemorySegment) (keyPath == null ? MemorySegment.NULL : ObjC.nsstring(keyPath)));
            return wrap(p);
        } catch (Throwable t) { throw new RuntimeException("animationWithKeyPath: failed", t); }
    }

    /// Typed convenience: set fromValue to an NSNumber built from a double.
    public void setFromDouble(double v) {
        NSNumber n = NSNumber.numberWithDouble(v);
        setFromValue((MemorySegment) (n == null ? MemorySegment.NULL : n.peer()));
    }

    /// Typed convenience: set toValue to an NSNumber built from a double.
    public void setToDouble(double v) {
        NSNumber n = NSNumber.numberWithDouble(v);
        setToValue((MemorySegment) (n == null ? MemorySegment.NULL : n.peer()));
    }

    /// Typed convenience: set byValue to an NSNumber built from a double.
    public void setByDouble(double v) {
        NSNumber n = NSNumber.numberWithDouble(v);
        setByValue((MemorySegment) (n == null ? MemorySegment.NULL : n.peer()));
    }
}
