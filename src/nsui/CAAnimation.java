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

    private static volatile boolean initialized;
    private static MethodHandle hWithKeyPath; // (id,SEL,id)->id [animationWithKeyPath:]
    private static MethodHandle hSetFrom;     // (id,SEL,id)->void [setFromValue:]
    private static MethodHandle hSetTo;       // (id,SEL,id)->void [setToValue:]
    private static MethodHandle hSetKeyPath;  // (id,SEL,id)->void [setKeyPath:]
    private static MethodHandle hSetDuration; // (id,SEL,double)->void [setDuration:]
    private static MethodHandle hSetTiming;   // (id,SEL,id)->void [setTimingFunction:]

    protected CAAnimation(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static CAAnimation wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAAnimation(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        hWithKeyPath = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hSetFrom = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hSetTo = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hSetKeyPath = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hSetDuration = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hSetTiming = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /// +[CABasicAnimation animationWithKeyPath:]
    public static CAAnimation animationWithKeyPath(String keyPath) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hWithKeyPath.invokeExact(ObjC.cls("CABasicAnimation"), ObjC.sel("animationWithKeyPath:"), ObjC.nsstring(keyPath));
            return wrap(p);
        } catch (Throwable t) { throw new RuntimeException("animationWithKeyPath: failed", t); }
    }

    public void setFromValue(MemorySegment value) {
        try { hSetFrom.invokeExact(peer, ObjC.sel("setFromValue:"), value == null ? MemorySegment.NULL : value); } catch (Throwable t) { throw new RuntimeException("setFromValue: failed", t); }
    }
    public void setToValue(MemorySegment value) {
        try { hSetTo.invokeExact(peer, ObjC.sel("setToValue:"), value == null ? MemorySegment.NULL : value); } catch (Throwable t) { throw new RuntimeException("setToValue: failed", t); }
    }
    public void setKeyPath(String kp) {
        try { hSetKeyPath.invokeExact(peer, ObjC.sel("setKeyPath:"), ObjC.nsstring(kp)); } catch (Throwable t) { throw new RuntimeException("setKeyPath: failed", t); }
    }
    public void setDuration(double d) {
        try { hSetDuration.invokeExact(peer, ObjC.sel("setDuration:"), d); } catch (Throwable t) { throw new RuntimeException("setDuration: failed", t); }
    }
    public void setTimingFunction(MemorySegment fn) {
        try { hSetTiming.invokeExact(peer, ObjC.sel("setTimingFunction:"), fn == null ? MemorySegment.NULL : fn); } catch (Throwable t) { throw new RuntimeException("setTimingFunction: failed", t); }
    }
    public void setTimingFunctionName(String name) {
        // CAMediaTimingFunction functionWithName:
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment fn = (MemorySegment) h.invokeExact(ObjC.cls("CAMediaTimingFunction"), ObjC.sel("functionWithName:"), ObjC.nsstring(name));
            setTimingFunction(fn);
        } catch (Throwable t) { throw new RuntimeException("functionWithName: failed", t); }
    }
}
