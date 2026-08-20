package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPanGestureRecognizer — a pan (drag) gesture. Thin, 1:1, stateless wrapper
/// over the native `NSPanGestureRecognizer`. Follows the project template:
/// volatile initialized, synchronized ensureInit, ObjC.handle(Sig.of...),
/// invokeExact, static create/wrap.
///
/// Created via `[[NSPanGestureRecognizer alloc] initWithTarget:action:]`.
/// Adds pan-specific state: buttonMask, numberOfTouchesRequired, translation,
/// velocity.
public final class NSPanGestureRecognizer extends NSGestureRecognizer {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitTargetAction; // (id, SEL, id, id) -> id [initWithTarget:action:]
    private static MethodHandle hButtonMask;       // (id, SEL) -> long [buttonMask]
    private static MethodHandle hSetButtonMask;    // (id, SEL, long) -> void [setButtonMask:]
    private static MethodHandle hNumTouches;       // (id, SEL) -> long [numberOfTouchesRequired]
    private static MethodHandle hSetNumTouches;    // (id, SEL, long) -> void
    private static MethodHandle hTranslation;      // (id, SEL, id) -> point [translationInView:]
    private static MethodHandle hSetTranslation;   // (id, SEL, point, id) -> void [setTranslation:inView:]
    private static MethodHandle hVelocity;         // (id, SEL, id) -> point [velocityInView:]

    private NSPanGestureRecognizer(MemorySegment peer) {
        super(peer);
        ensurePanInit();
    }

    /// Wrap an existing NSPanGestureRecognizer peer.
    public static NSPanGestureRecognizer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPanGestureRecognizer(peer);
    }

    private static synchronized void ensurePanInit() {
        if (initialized) return;
        // Ensure base class handles are ready
        NSGestureRecognizer.ensureInit();
        hInitTargetAction = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
        hButtonMask = ObjC.handle(Sig.of(Ret.INT));
        hSetButtonMask = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hNumTouches = ObjC.handle(Sig.of(Ret.INT));
        hSetNumTouches = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hTranslation = ObjC.handle(Sig.of(Ret.POINT, Arg.ID));
        hSetTranslation = ObjC.handle(Sig.of(Ret.VOID, Arg.POINT, Arg.ID));
        hVelocity = ObjC.handle(Sig.of(Ret.POINT, Arg.ID));
        initialized = true;
    }

    /// `[[NSPanGestureRecognizer alloc] initWithTarget:action:]` — a new pan recognizer.
    public static NSPanGestureRecognizer create(MemorySegment target, String actionSelector) {
        ensurePanInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPanGestureRecognizer"), ObjC.sel("alloc"));
        MemorySegment sel = actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector);
        try {
            MemorySegment t = (target == null || target.address() == 0) ? MemorySegment.NULL : target;
            p = (MemorySegment) hInitTargetAction.invokeExact(p, ObjC.sel("initWithTarget:action:"), (MemorySegment) t, sel);
        } catch (Throwable t) {
            throw new RuntimeException("initWithTarget:action: failed for NSPanGestureRecognizer", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSPanGestureRecognizer alloc/initWithTarget:action: returned nil");
        return new NSPanGestureRecognizer(p);
    }

    // ---------------------------------------------------------------- pan-specific API

    /// [recognizer buttonMask] — NSEventButtonMask (NSInteger).
    public long buttonMask() {
        try {
            return (long) hButtonMask.invokeExact(peer, ObjC.sel("buttonMask"));
        } catch (Throwable t) {
            throw new RuntimeException("buttonMask failed", t);
        }
    }

    /// [recognizer setButtonMask:]
    public void setButtonMask(long mask) {
        try {
            hSetButtonMask.invokeExact(peer, ObjC.sel("setButtonMask:"), mask);
        } catch (Throwable t) {
            throw new RuntimeException("setButtonMask: failed", t);
        }
    }

    /// [recognizer numberOfTouchesRequired]
    public long numberOfTouchesRequired() {
        try {
            return (long) hNumTouches.invokeExact(peer, ObjC.sel("numberOfTouchesRequired"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfTouchesRequired failed", t);
        }
    }

    public void setNumberOfTouchesRequired(long n) {
        try {
            hSetNumTouches.invokeExact(peer, ObjC.sel("setNumberOfTouchesRequired:"), n);
        } catch (Throwable t) {
            throw new RuntimeException("setNumberOfTouchesRequired: failed", t);
        }
    }

    /// [recognizer translationInView:] — delta since last reset.
    public NSPoint translationInView(NSView view) {
        try {
            MemorySegment seg = (MemorySegment) hTranslation.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("translationInView:"), (MemorySegment) ((view == null || view.peer() == null || view.peer().address()==0) ? MemorySegment.NULL : view.peer()));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("translationInView: failed", t);
        }
    }

    /// [recognizer setTranslation:inView:]
    public void setTranslation(NSPoint translation, NSView view) {
        try {
            hSetTranslation.invokeExact(peer, ObjC.sel("setTranslation:inView:"), translation.toSegment(), (MemorySegment) ((view == null || view.peer() == null || view.peer().address()==0) ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setTranslation:inView: failed", t);
        }
    }

    /// [recognizer velocityInView:] — points per second.
    public NSPoint velocityInView(NSView view) {
        try {
            MemorySegment seg = (MemorySegment) hVelocity.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("velocityInView:"), (MemorySegment) ((view == null || view.peer() == null || view.peer().address()==0) ? MemorySegment.NULL : view.peer()));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("velocityInView: failed", t);
        }
    }
}
