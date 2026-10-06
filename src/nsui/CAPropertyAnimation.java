package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAPropertyAnimation — thin wrapper for QuartzCore CAPropertyAnimation, the
/// key-path base class shared by CABasicAnimation, CAKeyframeAnimation,
/// CASpringAnimation and CAAnimationGroup. Adds keyPath, additive, cumulative
/// and valueFunction; timing behavior is inherited from CAAnimation.
public class CAPropertyAnimation extends CAAnimation {

    private record Handles(MethodHandle hGetId, MethodHandle hSetId, MethodHandle hGetBool, MethodHandle hSetBool) {}
    private static volatile Handles handles;

    protected CAPropertyAnimation(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAPropertyAnimation wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAPropertyAnimation(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)));
    }

    /// [animation keyPath] — the animated property (nil-safe).
    public String keyPath() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("keyPath")));
        } catch (Throwable t) { throw new RuntimeException("keyPath failed", t); }
    }

    /// [animation setKeyPath:] — e.g. "position", "opacity".
    public void setKeyPath(String keyPath) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setKeyPath:"),
                    (MemorySegment) (keyPath == null ? MemorySegment.NULL : ObjC.nsstring(keyPath)));
        } catch (Throwable t) { throw new RuntimeException("setKeyPath: failed", t); }
    }

    /// [animation isAdditive]
    public boolean isAdditive() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("isAdditive"));
        } catch (Throwable t) { throw new RuntimeException("isAdditive failed", t); }
    }

    /// [animation setAdditive:]
    public void setAdditive(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, ObjC.sel("setAdditive:"), flag);
        } catch (Throwable t) { throw new RuntimeException("setAdditive: failed", t); }
    }

    /// [animation isCumulative]
    public boolean isCumulative() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("isCumulative"));
        } catch (Throwable t) { throw new RuntimeException("isCumulative failed", t); }
    }

    /// [animation setCumulative:]
    public void setCumulative(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, ObjC.sel("setCumulative:"), flag);
        } catch (Throwable t) { throw new RuntimeException("setCumulative: failed", t); }
    }

    /// [animation valueFunction] — named value transform, or null.
    public CAValueFunction valueFunction() {
        ensureInit();
        try {
            return CAValueFunction.wrap((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("valueFunction")));
        } catch (Throwable t) { throw new RuntimeException("valueFunction failed", t); }
    }

    /// [animation setValueFunction:]
    public void setValueFunction(CAValueFunction fn) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setValueFunction:"),
                    (MemorySegment) (fn == null ? MemorySegment.NULL : fn.peer()));
        } catch (Throwable t) { throw new RuntimeException("setValueFunction: failed", t); }
    }
}
