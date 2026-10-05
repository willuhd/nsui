package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CASpringAnimation — physics-based spring over a key path: mass, stiffness,
/// damping, initial velocity. Thin stateless wrapper.
public class CASpringAnimation extends CABasicAnimation {

    private record Handles(MethodHandle hWithKeyPath, MethodHandle hGetDouble, MethodHandle hSetDouble,
            MethodHandle hGetBool, MethodHandle hSetBool) {}
    private static volatile Handles handles;

    protected CASpringAnimation(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CASpringAnimation wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CASpringAnimation(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)));
    }

    /// animationWithKeyPath: (inherited factory returns a spring for this class).
    public static CASpringAnimation create(String keyPath) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithKeyPath().invokeExact(
                    ObjC.cls("CASpringAnimation"), ObjC.sel("animationWithKeyPath:"),
                    (MemorySegment) (keyPath == null ? MemorySegment.NULL : ObjC.nsstring(keyPath)));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("animationWithKeyPath: failed for CASpringAnimation", t);
        }
    }

    private double getDouble(String selector) {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel(selector));
        } catch (Throwable t) {
            throw new RuntimeException(selector + " failed", t);
        }
    }

    private void setDouble(String selector, double value) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel(selector), value);
        } catch (Throwable t) {
            throw new RuntimeException(selector + " failed", t);
        }
    }

    /// mass / stiffness / damping / initialVelocity.
    public double mass() { return getDouble("mass"); }
    /// setMass:.
    public void setMass(double v) { setDouble("setMass:", v); }
    /// stiffness.
    public double stiffness() { return getDouble("stiffness"); }
    /// setStiffness:.
    public void setStiffness(double v) { setDouble("setStiffness:", v); }
    /// damping.
    public double damping() { return getDouble("damping"); }
    /// setDamping:.
    public void setDamping(double v) { setDouble("setDamping:", v); }
    /// initialVelocity.
    public double initialVelocity() { return getDouble("initialVelocity"); }
    /// setInitialVelocity:.
    public void setInitialVelocity(double v) { setDouble("setInitialVelocity:", v); }

    /// allowsOverdamping.
    public boolean allowsOverdamping() {
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("allowsOverdamping"));
        } catch (Throwable t) {
            throw new RuntimeException("allowsOverdamping failed", t);
        }
    }

    /// setAllowsOverdamping:.
    public void setAllowsOverdamping(boolean flag) {
        try {
            handles.hSetBool().invokeExact(peer, ObjC.sel("setAllowsOverdamping:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsOverdamping: failed", t);
        }
    }

    /// settlingDuration (readonly) — rest time for the current parameters.
    public double settlingDuration() {
        return getDouble("settlingDuration");
    }
}
