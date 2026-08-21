package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAAnimationGroup — thin wrapper for QuartzCore CAAnimationGroup: runs several
/// CAAnimations concurrently on one key path. Timing/duration behavior is
/// inherited from CAAnimation; this wrapper adds the animations array accessors.
public class CAAnimationGroup extends CAAnimation {

    // [CAAnimationGroup animation] and the animations getter share the (id,SEL)->id shape.
    private record Handles(MethodHandle hGetId, MethodHandle hSetId) {}
    private static volatile Handles handles;

    protected CAAnimationGroup(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static CAAnimationGroup wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAAnimationGroup(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
        );
    }

    /// +[CAAnimationGroup animation]
    public static CAAnimationGroup create() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetId().invokeExact(ObjC.cls("CAAnimationGroup"), ObjC.sel("animation"));
            return wrap(p);
        } catch (Throwable t) { throw new RuntimeException("CAAnimationGroup animation failed", t); }
    }

    /// [group animations] — member animations as NSArray, or null.
    public NSArray animations() {
        ensureInit();
        try {
            return NSArray.wrap((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("animations")));
        } catch (Throwable t) { throw new RuntimeException("animations failed", t); }
    }

    /// [group setAnimations:] — NSArray of CAAnimation peers.
    public void setAnimations(NSArray animations) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setAnimations:"), (MemorySegment) (animations == null ? MemorySegment.NULL : animations.peer()));
        } catch (Throwable t) { throw new RuntimeException("setAnimations: failed", t); }
    }
}
