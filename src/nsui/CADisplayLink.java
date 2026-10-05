package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CADisplayLink — screen-synced callback driver for frame loops: attach to a
/// run loop, get timestamped wakes at display rate. Thin stateless wrapper.
/// (The target/selector still routes through DelegateProxy action targets.)
public final class CADisplayLink extends NSObject {

    private record Handles(MethodHandle hGetDouble, MethodHandle hAttach, MethodHandle hFactory) {}
    private static volatile Handles handles;

    private CADisplayLink(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CADisplayLink wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CADisplayLink(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)));
    }

    /// displayLinkWithTarget:selector:.
    public static CADisplayLink create(MemorySegment target, String selector) {
        ensureInit();
        // Locals (not inline ternaries): invokeExact is signature-polymorphic and
        // would otherwise infer Object for a conditional argument.
        MemorySegment t = target == null ? MemorySegment.NULL : target;
        MemorySegment s = selector == null ? MemorySegment.NULL : ObjC.sel(selector);
        try {
            return wrap((MemorySegment) handles.hFactory().invokeExact(ObjC.cls("CADisplayLink"),
                    ObjC.sel("displayLinkWithTarget:selector:"), t, s));
        } catch (Throwable t2) {
            throw new RuntimeException("displayLinkWithTarget:selector: failed", t2);
        }
    }

    private static void runLoopLink(CADisplayLink link, String method, NSRunLoop loop, String mode) {
        try {
            handles.hAttach().invokeExact(link.peer(), ObjC.sel(method),
                    (MemorySegment) (loop == null ? MemorySegment.NULL : loop.peer()),
                    ObjC.nsstring(mode == null ? NSRunLoop.DEFAULT_MODE : mode));
        } catch (Throwable t) {
            throw new RuntimeException(method + " failed", t);
        }
    }

    /// addToRunLoop:forMode:.
    public void addToRunLoop(NSRunLoop loop, String mode) {
        runLoopLink(this, "addToRunLoop:forMode:", loop, mode);
    }

    /// removeFromRunLoop:forMode:.
    public void removeFromRunLoop(NSRunLoop loop, String mode) {
        runLoopLink(this, "removeFromRunLoop:forMode:", loop, mode);
    }

    /// invalidate — stop and detach permanently.
    public void invalidate() {
        ObjC.msgSendVoid(peer, ObjC.sel("invalidate"));
    }

    private double getDouble(String selector) {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel(selector));
        } catch (Throwable t) {
            throw new RuntimeException(selector + " failed", t);
        }
    }

    /// timestamp of the last frame (seconds).
    public double timestamp() { return getDouble("timestamp"); }
    /// duration (nominal seconds per frame).
    public double duration() { return getDouble("duration"); }
    /// targetTimestamp of the next frame (seconds).
    public double targetTimestamp() { return getDouble("targetTimestamp"); }

    /// isPaused / setPaused:.
    public boolean isPaused() { return ObjC.msgSendBool(peer, ObjC.sel("isPaused")); }
    /// setPaused:.
    public void setPaused(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setPaused:"), flag); }
    /// frameInterval / setFrameInterval:.
    public long frameInterval() { return ObjC.msgSendLong(peer, ObjC.sel("frameInterval")); }
    /// setFrameInterval:.
    public void setFrameInterval(long n) { ObjC.msgSendVoidLong(peer, ObjC.sel("setFrameInterval:"), n); }
    /// preferredFramesPerSecond / setter.
    public long preferredFramesPerSecond() { return ObjC.msgSendLong(peer, ObjC.sel("preferredFramesPerSecond")); }
    /// setPreferredFramesPerSecond:.
    public void setPreferredFramesPerSecond(long n) { ObjC.msgSendVoidLong(peer, ObjC.sel("setPreferredFramesPerSecond:"), n); }
}
