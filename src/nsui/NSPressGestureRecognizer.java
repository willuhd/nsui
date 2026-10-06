package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPressGestureRecognizer — press-and-hold (Force Touch / long press).
/// Thin stateless wrapper; follows the NSClickGestureRecognizer template.
///
/// Coverage notes (header: NSPressGestureRecognizer.h wins on API truth):
/// complete — `buttonMask`, `minimumPressDuration`, `allowableMovement`
/// and `numberOfTouchesRequired` (+ setters) plus the inherited
/// NSGestureRecognizer surface cover every non-delegate member. Omitted:
/// nothing (no blocks, no NSError**, no delegate protocol members here).
public final class NSPressGestureRecognizer extends NSGestureRecognizer {

    private record Handles(MethodHandle hInitTargetAction, MethodHandle hGetDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    private NSPressGestureRecognizer(MemorySegment peer) {
        super(peer);
        ensurePressInit();
    }

    /// Wrap an existing peer.
    public static NSPressGestureRecognizer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPressGestureRecognizer(peer);
    }

    private static void ensurePressInit() {
        if (handles != null) return;
        ensurePressInitLocked();
    }

    private static synchronized void ensurePressInitLocked() {
        if (handles != null) return;
        NSGestureRecognizer.ensureInit();
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)), ObjC.handle(Sig.of(Ret.DOUBLE)), ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
    }

    /// [[NSPressGestureRecognizer alloc] initWithTarget:action:].
    public static NSPressGestureRecognizer create(MemorySegment target, String actionSelector) {
        ensurePressInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPressGestureRecognizer"), ObjC.sel("alloc"));
        MemorySegment sel = actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector);
        try {
            p = (MemorySegment) handles.hInitTargetAction().invokeExact(p, ObjC.sel("initWithTarget:action:"), (MemorySegment) (target == null ? MemorySegment.NULL : target), sel);
        } catch (Throwable t) {
            throw new RuntimeException("initWithTarget:action: failed for NSPressGestureRecognizer", t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithTarget:action: returned nil for NSPressGestureRecognizer");
        return new NSPressGestureRecognizer(p);
    }

    /// buttonMask — qualifying mouse buttons (default 0x1).
    public long buttonMask() {
        return ObjC.msgSendLong(peer, ObjC.sel("buttonMask"));
    }

    /// setButtonMask:.
    public void setButtonMask(long mask) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setButtonMask:"), mask);
    }

    /// minimumPressDuration, in seconds (default: the double-click interval).
    public double minimumPressDuration() {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("minimumPressDuration"));
        } catch (Throwable t) {
            throw new RuntimeException("minimumPressDuration failed", t);
        }
    }

    /// setMinimumPressDuration:.
    public void setMinimumPressDuration(double seconds) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setMinimumPressDuration:"), seconds);
        } catch (Throwable t) {
            throw new RuntimeException("setMinimumPressDuration: failed", t);
        }
    }

    /// allowableMovement, in screen points (default: double-click distance).
    public double allowableMovement() {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("allowableMovement"));
        } catch (Throwable t) {
            throw new RuntimeException("allowableMovement failed", t);
        }
    }

    /// setAllowableMovement:.
    public void setAllowableMovement(double points) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setAllowableMovement:"), points);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowableMovement: failed", t);
        }
    }

    /// numberOfTouchesRequired.
    public long numberOfTouchesRequired() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfTouchesRequired"));
    }

    /// setNumberOfTouchesRequired:.
    public void setNumberOfTouchesRequired(long n) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setNumberOfTouchesRequired:"), n);
    }
}
