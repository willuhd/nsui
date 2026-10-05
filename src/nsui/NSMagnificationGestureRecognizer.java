package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMagnificationGestureRecognizer — pinch magnification (KVO-observable live value).
/// Thin stateless wrapper; follows the NSClickGestureRecognizer template.
public final class NSMagnificationGestureRecognizer extends NSGestureRecognizer {

    private record Handles(MethodHandle hInitTargetAction, MethodHandle hGetDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    private NSMagnificationGestureRecognizer(MemorySegment peer) {
        super(peer);
        ensureMagInit();
    }

    /// Wrap an existing peer.
    public static NSMagnificationGestureRecognizer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMagnificationGestureRecognizer(peer);
    }

    private static synchronized void ensureMagInit() {
        if (handles != null) return;
        NSGestureRecognizer.ensureInit();
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)), ObjC.handle(Sig.of(Ret.DOUBLE)), ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
    }

    /// [[NSMagnificationGestureRecognizer alloc] initWithTarget:action:].
    public static NSMagnificationGestureRecognizer create(MemorySegment target, String actionSelector) {
        ensureMagInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSMagnificationGestureRecognizer"), ObjC.sel("alloc"));
        MemorySegment sel = actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector);
        try {
            p = (MemorySegment) handles.hInitTargetAction().invokeExact(p, ObjC.sel("initWithTarget:action:"), (MemorySegment) (target == null ? MemorySegment.NULL : target), sel);
        } catch (Throwable t) {
            throw new RuntimeException("initWithTarget:action: failed for NSMagnificationGestureRecognizer", t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithTarget:action: returned nil for NSMagnificationGestureRecognizer");
        return new NSMagnificationGestureRecognizer(p);
    }

    /// magnification, in unitless scale relative to the gesture start.
    public double magnification() {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("magnification"));
        } catch (Throwable t) {
            throw new RuntimeException("magnification failed", t);
        }
    }

    /// setMagnification:.
    public void setMagnification(double value) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setMagnification:"), value);
        } catch (Throwable t) {
            throw new RuntimeException("setMagnification: failed", t);
        }
    }
}
