package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSRotationGestureRecognizer — two-finger rotation in radians (KVO-observable live value).
/// Thin stateless wrapper; follows the NSClickGestureRecognizer template.
public final class NSRotationGestureRecognizer extends NSGestureRecognizer {

    private record Handles(MethodHandle hInitTargetAction, MethodHandle hGetDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    private NSRotationGestureRecognizer(MemorySegment peer) {
        super(peer);
        ensureRotInit();
    }

    /// Wrap an existing peer.
    public static NSRotationGestureRecognizer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSRotationGestureRecognizer(peer);
    }

    private static synchronized void ensureRotInit() {
        if (handles != null) return;
        NSGestureRecognizer.ensureInit();
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)), ObjC.handle(Sig.of(Ret.DOUBLE)), ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
    }

    /// [[NSRotationGestureRecognizer alloc] initWithTarget:action:].
    public static NSRotationGestureRecognizer create(MemorySegment target, String actionSelector) {
        ensureRotInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSRotationGestureRecognizer"), ObjC.sel("alloc"));
        MemorySegment sel = actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector);
        try {
            p = (MemorySegment) handles.hInitTargetAction().invokeExact(p, ObjC.sel("initWithTarget:action:"), (MemorySegment) (target == null ? MemorySegment.NULL : target), sel);
        } catch (Throwable t) {
            throw new RuntimeException("initWithTarget:action: failed for NSRotationGestureRecognizer", t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithTarget:action: returned nil for NSRotationGestureRecognizer");
        return new NSRotationGestureRecognizer(p);
    }

    /// rotation, in radians relative to the gesture start.
    public double rotation() {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("rotation"));
        } catch (Throwable t) {
            throw new RuntimeException("rotation failed", t);
        }
    }

    /// setRotation:.
    public void setRotation(double value) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setRotation:"), value);
        } catch (Throwable t) {
            throw new RuntimeException("setRotation: failed", t);
        }
    }
}
