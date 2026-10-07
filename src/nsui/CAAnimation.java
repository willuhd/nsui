package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAAnimation — thin wrapper for QuartzCore CAAnimation.
/// Covers CABasicAnimation-style key-path animations via the same peer.
public class CAAnimation extends NSObject {

            private record Handles(MethodHandle hWithKeyPath, MethodHandle hSetFrom, MethodHandle hSetDuration, MethodHandle hGetId, MethodHandle hGetBool, MethodHandle hSetBool, MethodHandle hGetFloat, MethodHandle hSetFloat) {}
    private static volatile Handles handles;

    protected CAAnimation(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static CAAnimation wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAAnimation(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID)), ObjC.handle(Sig.of(Ret.VOID, Arg.ID)), ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)), ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.BOOL)), ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)), ObjC.handle(Sig.of(Ret.FLOAT)), ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT)));
    }

    /// +[CABasicAnimation animationWithKeyPath:]
    public static CAAnimation animationWithKeyPath(String keyPath) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithKeyPath().invokeExact(ObjC.cls("CABasicAnimation"), ObjC.sel("animationWithKeyPath:"), ObjC.nsstring(keyPath));
            return wrap(p);
        } catch (Throwable t) { throw new RuntimeException("animationWithKeyPath: failed", t); }
    }

    public void setFromValue(MemorySegment value) {
        // NOTE: the (MemorySegment) cast matters — without it, JDK 21+ javac types the
        // ternary argument of the signature-polymorphic invokeExact as Object, producing
        // a WrongMethodTypeException at runtime (invokeExact never converts).
        try { handles.hSetFrom().invokeExact(peer, ObjC.sel("setFromValue:"), (MemorySegment) (value == null ? MemorySegment.NULL : value)); } catch (Throwable t) { throw new RuntimeException("setFromValue: failed", t); }
    }
    public void setToValue(MemorySegment value) {
        try { handles.hSetFrom().invokeExact(peer, ObjC.sel("setToValue:"), (MemorySegment) (value == null ? MemorySegment.NULL : value)); } catch (Throwable t) { throw new RuntimeException("setToValue: failed", t); }
    }
    public void setKeyPath(String kp) {
        try { handles.hSetFrom().invokeExact(peer, ObjC.sel("setKeyPath:"), (MemorySegment) (kp == null ? MemorySegment.NULL : ObjC.nsstring(kp))); } catch (Throwable t) { throw new RuntimeException("setKeyPath: failed", t); }
    }
    public void setDuration(double d) {
        try { handles.hSetDuration().invokeExact(peer, ObjC.sel("setDuration:"), d); } catch (Throwable t) { throw new RuntimeException("setDuration: failed", t); }
    }
    public void setTimingFunction(MemorySegment fn) {
        try { handles.hSetFrom().invokeExact(peer, ObjC.sel("setTimingFunction:"), (MemorySegment) (fn == null ? MemorySegment.NULL : fn)); } catch (Throwable t) { throw new RuntimeException("setTimingFunction: failed", t); }
    }
    public void setTimingFunctionName(String name) {
        // CAMediaTimingFunction functionWithName:
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment fn = (MemorySegment) h.invokeExact(ObjC.cls("CAMediaTimingFunction"), ObjC.sel("functionWithName:"), ObjC.nsstring(name));
            setTimingFunction(fn);
        } catch (Throwable t) { throw new RuntimeException("functionWithName: failed", t); }
    }

    /// [animation autoreverses]
    public boolean autoreverses() {
        ensureInit();
        try { return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("autoreverses")); } catch (Throwable t) { throw new RuntimeException("autoreverses failed", t); }
    }

    /// [animation setAutoreverses:]
    public void setAutoreverses(boolean flag) {
        ensureInit();
        try { handles.hSetBool().invokeExact(peer, ObjC.sel("setAutoreverses:"), flag); } catch (Throwable t) { throw new RuntimeException("setAutoreverses: failed", t); }
    }

    /// [animation repeatCount]
    public float repeatCount() {
        ensureInit();
        try { return (float) handles.hGetFloat().invokeExact(peer, ObjC.sel("repeatCount")); } catch (Throwable t) { throw new RuntimeException("repeatCount failed", t); }
    }

    /// [animation setRepeatCount:]
    public void setRepeatCount(float count) {
        ensureInit();
        try { handles.hSetFloat().invokeExact(peer, ObjC.sel("setRepeatCount:"), count); } catch (Throwable t) { throw new RuntimeException("setRepeatCount: failed", t); }
    }

    /// [animation removedOnCompletion]
    public boolean removedOnCompletion() {
        ensureInit();
        try { return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("isRemovedOnCompletion")); } catch (Throwable t) { throw new RuntimeException("isRemovedOnCompletion failed", t); }
    }

    /// [animation setRemovedOnCompletion:]
    public void setRemovedOnCompletion(boolean flag) {
        ensureInit();
        try { handles.hSetBool().invokeExact(peer, ObjC.sel("setRemovedOnCompletion:"), flag); } catch (Throwable t) { throw new RuntimeException("setRemovedOnCompletion: failed", t); }
    }

    /// [animation fillMode] — e.g. "forwards", "backwards", "both", "removed"
    public String fillMode() {
        ensureInit();
        try { return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("fillMode"))); } catch (Throwable t) { throw new RuntimeException("fillMode failed", t); }
    }

    /// [animation setFillMode:]
    public void setFillMode(String mode) {
        ensureInit();
        try { handles.hSetFrom().invokeExact(peer, ObjC.sel("setFillMode:"), (MemorySegment)(mode==null?MemorySegment.NULL:ObjC.nsstring(mode))); } catch (Throwable t) { throw new RuntimeException("setFillMode: failed", t); }
    }

    /// [animation setByValue:] — same shape as setFromValue:; identical cast discipline.
    public void setByValue(MemorySegment value) {
        try { handles.hSetFrom().invokeExact(peer, ObjC.sel("setByValue:"), (MemorySegment) (value == null ? MemorySegment.NULL : value)); } catch (Throwable t) { throw new RuntimeException("setByValue: failed", t); }
    }

    /// [animation fromValue] — raw value object or null.
    public MemorySegment fromValue() {
        ensureInit();
        try { return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("fromValue")); } catch (Throwable t) { throw new RuntimeException("fromValue failed", t); }
    }

    /// [animation toValue] — raw value object or null.
    public MemorySegment toValue() {
        ensureInit();
        try { return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("toValue")); } catch (Throwable t) { throw new RuntimeException("toValue failed", t); }
    }

    /// [animation byValue] — raw value object or null.
    public MemorySegment byValue() {
        ensureInit();
        try { return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("byValue")); } catch (Throwable t) { throw new RuntimeException("byValue failed", t); }
    }
}
