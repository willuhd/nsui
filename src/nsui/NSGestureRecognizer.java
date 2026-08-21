package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSGestureRecognizer — base class for gesture recognizers. Thin, 1:1,
/// stateless wrapper over the native `NSGestureRecognizer`: each method
/// maps to one `objc_msgSend` selector. Follows the project template:
/// volatile initialized, synchronized ensureInit, ObjC.handle(Sig.of...),
/// invokeExact, static create/wrap.
///
/// Created via `[[NSGestureRecognizer alloc] initWithTarget:action:]`.
/// Subclasses (NSPanGestureRecognizer, NSClickGestureRecognizer) inherit this
/// machinery. The target is an ObjC id (typically from DelegateProxy.actionTarget)
/// and the action is a selector string like `"panned:"`.
public class NSGestureRecognizer extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hInitTargetAction, MethodHandle hSetEnabled, MethodHandle hSetDelegate, MethodHandle hGetId, MethodHandle hGetBool, MethodHandle hLocation) {}
    private static volatile Handles handles;

    protected NSGestureRecognizer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing NSGestureRecognizer peer.
    public static NSGestureRecognizer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSGestureRecognizer(peer);
    }

        protected static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.POINT, Arg.ID))
        );
    }

    /// `[[NSGestureRecognizer alloc] initWithTarget:action:]` — base recognizer.
    public static NSGestureRecognizer create(MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSGestureRecognizer"), ObjC.sel("alloc"));
        MemorySegment sel = actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector);
        try {
            p = (MemorySegment) handles.hInitTargetAction().invokeExact(p, ObjC.sel("initWithTarget:action:"), (MemorySegment) (target == null ? MemorySegment.NULL : target), sel);
        } catch (Throwable t) {
            throw new RuntimeException("initWithTarget:action: failed for NSGestureRecognizer", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSGestureRecognizer alloc/initWithTarget:action: returned nil");
        return new NSGestureRecognizer(p);
    }

    // ---------------------------------------------------------------- instance API

    /// [recognizer state] — NSGestureRecognizerState (NSInteger).
    public long state() {
        return ObjC.msgSendLong(peer, ObjC.sel("state"));
    }

    /// [recognizer isEnabled]
    public boolean isEnabled() {
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("isEnabled"));
        } catch (Throwable t) {
            throw new RuntimeException("isEnabled failed", t);
        }
    }

    /// [recognizer setEnabled:]
    public void setEnabled(boolean flag) {
        try {
            handles.hSetEnabled().invokeExact(peer, ObjC.sel("setEnabled:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setEnabled: failed", t);
        }
    }

    /// [recognizer view] — NSView peer or nil.
    public NSView view() {
        try {
            MemorySegment v = (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("view"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("view failed", t);
        }
    }

    /// [recognizer delegate] — id or nil.
    public MemorySegment delegate() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("delegate"));
        } catch (Throwable t) {
            throw new RuntimeException("delegate failed", t);
        }
    }

    /// [recognizer setDelegate:]
    public void setDelegate(MemorySegment delegate) {
        try {
            handles.hSetDelegate().invokeExact(peer, ObjC.sel("setDelegate:"), (MemorySegment) ((MemorySegment) (delegate == null ? MemorySegment.NULL : delegate)));
        } catch (Throwable t) {
            throw new RuntimeException("setDelegate: failed", t);
        }
    }

    /// [recognizer target] — id or nil (if single target).
    public MemorySegment target() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("target"));
        } catch (Throwable t) {
            throw new RuntimeException("target failed", t);
        }
    }

    /// [recognizer action] — SEL id or nil.
    public MemorySegment action() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("action"));
        } catch (Throwable t) {
            throw new RuntimeException("action failed", t);
        }
    }

    /// [recognizer setTarget:] — single target variant.
    public void setTarget(MemorySegment target) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setTarget:"), (MemorySegment) (target == null ? MemorySegment.NULL : target));
        } catch (Throwable t) {
            throw new RuntimeException("setTarget: failed", t);
        }
    }

    /// [recognizer setAction:] — single action variant.
    public void setAction(String actionSelector) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setAction:"), (MemorySegment) (actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector)));
        } catch (Throwable t) {
            throw new RuntimeException("setAction: failed", t);
        }
    }

    /// [recognizer setAction:] with raw SEL.
    public void setAction(MemorySegment action) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setAction:"), (MemorySegment) (action == null ? MemorySegment.NULL : action));
        } catch (Throwable t) {
            throw new RuntimeException("setAction: failed", t);
        }
    }

    /// Add a target/action pair to the recognizer.
    public void addTarget(MemorySegment target, String actionSelector) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("addTarget:action:"), (MemorySegment) (target == null || target.address()==0 ? MemorySegment.NULL : target), (MemorySegment) (actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector)));
        } catch (Throwable t) {
            throw new RuntimeException("addTarget:action: failed", t);
        }
    }

    /// Remove a target/action pair.
    public void removeTarget(MemorySegment target, String actionSelector) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("removeTarget:action:"), (MemorySegment) (target == null || target.address()==0 ? MemorySegment.NULL : target), (MemorySegment) (actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector)));
        } catch (Throwable t) {
            throw new RuntimeException("removeTarget:action: failed", t);
        }
    }

    /// [recognizer locationInView:] — point in view's coordinates.
    public NSPoint locationInView(NSView view) {
        try {
            MemorySegment seg = (MemorySegment) handles.hLocation().invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("locationInView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("locationInView: failed", t);
        }
    }

    /// [recognizer cancelsTouchesInView]
    public boolean cancelsTouchesInView() {
        return ObjC.msgSendBool(peer, ObjC.sel("cancelsTouchesInView"));
    }

    public void setCancelsTouchesInView(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setCancelsTouchesInView:"), flag);
    }

    /// [recognizer delaysPrimaryMouseButtonEvents]
    public boolean delaysPrimaryMouseButtonEvents() {
        return ObjC.msgSendBool(peer, ObjC.sel("delaysPrimaryMouseButtonEvents"));
    }

    public void setDelaysPrimaryMouseButtonEvents(boolean flag) {
        try {
            handles.hSetEnabled().invokeExact(peer, ObjC.sel("setDelaysPrimaryMouseButtonEvents:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setDelaysPrimaryMouseButtonEvents: failed", t);
        }
    }
}
