package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSClickGestureRecognizer — a click gesture. Thin, 1:1, stateless wrapper over
/// the native `NSClickGestureRecognizer`. Follows the project template:
/// volatile initialized, synchronized ensureInit, ObjC.handle(Sig.of...),
/// invokeExact, static create/wrap.
///
/// Created via `[[NSClickGestureRecognizer alloc] initWithTarget:action:]`.
/// Adds click-specific state: buttonMask, numberOfClicksRequired.
public final class NSClickGestureRecognizer extends NSGestureRecognizer {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hInitTargetAction, MethodHandle hButtonMask, MethodHandle hSetButtonMask) {}
    private static volatile Handles handles;

    private NSClickGestureRecognizer(MemorySegment peer) {
        super(peer);
        ensureClickInit();
    }

    /// Wrap an existing NSClickGestureRecognizer peer.
    public static NSClickGestureRecognizer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSClickGestureRecognizer(peer);
    }

        private static synchronized void ensureClickInit() {
        if (handles != null) return;
        NSGestureRecognizer.ensureInit();
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)), ObjC.handle(Sig.of(Ret.INT)), ObjC.handle(Sig.of(Ret.VOID, Arg.INT)));
    }

    /// `[[NSClickGestureRecognizer alloc] initWithTarget:action:]` — a new click recognizer.
    public static NSClickGestureRecognizer create(MemorySegment target, String actionSelector) {
        ensureClickInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSClickGestureRecognizer"), ObjC.sel("alloc"));
        MemorySegment sel = actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector);
        try {
            p = (MemorySegment) handles.hInitTargetAction().invokeExact(p, ObjC.sel("initWithTarget:action:"), (MemorySegment) (target == null ? MemorySegment.NULL : target), sel);
        } catch (Throwable t) {
            throw new RuntimeException("initWithTarget:action: failed for NSClickGestureRecognizer", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSClickGestureRecognizer alloc/initWithTarget:action: returned nil");
        return new NSClickGestureRecognizer(p);
    }

    // ---------------------------------------------------------------- click-specific API

    /// [recognizer buttonMask] — NSEventButtonMask (NSInteger).
    public long buttonMask() {
        try {
            return (long) handles.hButtonMask().invokeExact(peer, ObjC.sel("buttonMask"));
        } catch (Throwable t) {
            throw new RuntimeException("buttonMask failed", t);
        }
    }

    /// [recognizer setButtonMask:]
    public void setButtonMask(long mask) {
        try {
            handles.hSetButtonMask().invokeExact(peer, ObjC.sel("setButtonMask:"), mask);
        } catch (Throwable t) {
            throw new RuntimeException("setButtonMask: failed", t);
        }
    }

    /// [recognizer numberOfClicksRequired]
    public long numberOfClicksRequired() {
        try {
            return (long) handles.hButtonMask().invokeExact(peer, ObjC.sel("numberOfClicksRequired"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfClicksRequired failed", t);
        }
    }

    public void setNumberOfClicksRequired(long n) {
        try {
            handles.hSetButtonMask().invokeExact(peer, ObjC.sel("setNumberOfClicksRequired:"), n);
        } catch (Throwable t) {
            throw new RuntimeException("setNumberOfClicksRequired: failed", t);
        }
    }

    /// [recognizer numberOfTouchesRequired] — if supported.
    public long numberOfTouchesRequired() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("numberOfTouchesRequired"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfTouchesRequired failed", t);
        }
    }

    public void setNumberOfTouchesRequired(long n) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("setNumberOfTouchesRequired:"), n);
        } catch (Throwable t) {
            throw new RuntimeException("setNumberOfTouchesRequired: failed", t);
        }
    }
}
