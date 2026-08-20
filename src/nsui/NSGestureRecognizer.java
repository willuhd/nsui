package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSGestureRecognizer — base class for gesture recognizers. Thin, 1:1,
 * stateless wrapper over the native {@code NSGestureRecognizer}: each method
 * maps to one {@code objc_msgSend} selector. Follows the project template:
 * volatile initialized, synchronized ensureInit, ObjC.handle(Sig.of...),
 * invokeExact, static create/wrap.
 *
 * <p>Created via {@code [[NSGestureRecognizer alloc] initWithTarget:action:]}.
 * Subclasses (NSPanGestureRecognizer, NSClickGestureRecognizer) inherit this
 * machinery. The target is an ObjC id (typically from DelegateProxy.actionTarget)
 * and the action is a selector string like {@code "panned:"}.
 */
public class NSGestureRecognizer extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitTargetAction; // (id, SEL, id, id) -> id [initWithTarget:action:]
    private static MethodHandle hSetEnabled;       // (id, SEL, bool) -> void [setEnabled:]
    private static MethodHandle hSetDelegate;      // (id, SEL, id) -> void [setDelegate:]
    private static MethodHandle hGetId;            // (id, SEL) -> id [view/delegate/target/action]
    private static MethodHandle hGetBool;          // (id, SEL) -> bool [isEnabled]
    private static MethodHandle hLocation;         // (id, SEL, id) -> point [locationInView:]
    private static MethodHandle hSetDelaysPrimary; // (id, SEL, bool) -> void

    protected NSGestureRecognizer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /** Wrap an existing NSGestureRecognizer peer. */
    public static NSGestureRecognizer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSGestureRecognizer(peer);
    }

    protected static synchronized void ensureInit() {
        if (initialized) return;
        hInitTargetAction = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
        hSetEnabled = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hSetDelegate = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hGetBool = ObjC.handle(Sig.of(Ret.BOOL));
        hLocation = ObjC.handle(Sig.of(Ret.POINT, Arg.ID));
        hSetDelaysPrimary = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        initialized = true;
    }

    /** {@code [[NSGestureRecognizer alloc] initWithTarget:action:]} — base recognizer. */
    public static NSGestureRecognizer create(MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSGestureRecognizer"), ObjC.sel("alloc"));
        MemorySegment sel = actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector);
        try {
            p = (MemorySegment) hInitTargetAction.invokeExact(p, ObjC.sel("initWithTarget:action:"), (MemorySegment) (target == null ? MemorySegment.NULL : target), sel);
        } catch (Throwable t) {
            throw new RuntimeException("initWithTarget:action: failed for NSGestureRecognizer", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSGestureRecognizer alloc/initWithTarget:action: returned nil");
        return new NSGestureRecognizer(p);
    }

    // ---------------------------------------------------------------- instance API

    /** [recognizer state] — NSGestureRecognizerState (NSInteger). */
    public long state() {
        return ObjC.msgSendLong(peer, ObjC.sel("state"));
    }

    /** [recognizer isEnabled] */
    public boolean isEnabled() {
        try {
            return (boolean) hGetBool.invokeExact(peer, ObjC.sel("isEnabled"));
        } catch (Throwable t) {
            throw new RuntimeException("isEnabled failed", t);
        }
    }

    /** [recognizer setEnabled:] */
    public void setEnabled(boolean flag) {
        try {
            hSetEnabled.invokeExact(peer, ObjC.sel("setEnabled:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setEnabled: failed", t);
        }
    }

    /** [recognizer view] — NSView peer or nil. */
    public NSView view() {
        try {
            MemorySegment v = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("view"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("view failed", t);
        }
    }

    /** [recognizer delegate] — id or nil. */
    public MemorySegment delegate() {
        try {
            return (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("delegate"));
        } catch (Throwable t) {
            throw new RuntimeException("delegate failed", t);
        }
    }

    /** [recognizer setDelegate:] */
    public void setDelegate(MemorySegment delegate) {
        try {
            hSetDelegate.invokeExact(peer, ObjC.sel("setDelegate:"), (MemorySegment) ((MemorySegment) (delegate == null ? MemorySegment.NULL : delegate)));
        } catch (Throwable t) {
            throw new RuntimeException("setDelegate: failed", t);
        }
    }

    /** [recognizer target] — id or nil (if single target). */
    public MemorySegment target() {
        try {
            return (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("target"));
        } catch (Throwable t) {
            throw new RuntimeException("target failed", t);
        }
    }

    /** [recognizer action] — SEL id or nil. */
    public MemorySegment action() {
        try {
            return (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("action"));
        } catch (Throwable t) {
            throw new RuntimeException("action failed", t);
        }
    }

    /** Add a target/action pair to the recognizer. */
    public void addTarget(MemorySegment target, String actionSelector) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("addTarget:action:"), (MemorySegment) (target == null || target.address()==0 ? MemorySegment.NULL : target), (MemorySegment) (actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector)));
        } catch (Throwable t) {
            throw new RuntimeException("addTarget:action: failed", t);
        }
    }

    /** Remove a target/action pair. */
    public void removeTarget(MemorySegment target, String actionSelector) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("removeTarget:action:"), (MemorySegment) (target == null || target.address()==0 ? MemorySegment.NULL : target), (MemorySegment) (actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector)));
        } catch (Throwable t) {
            throw new RuntimeException("removeTarget:action: failed", t);
        }
    }

    /** [recognizer locationInView:] — point in view's coordinates. */
    public NSPoint locationInView(NSView view) {
        try {
            MemorySegment seg = (MemorySegment) hLocation.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("locationInView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("locationInView: failed", t);
        }
    }

    /** [recognizer cancelsTouchesInView] */
    public boolean cancelsTouchesInView() {
        return ObjC.msgSendBool(peer, ObjC.sel("cancelsTouchesInView"));
    }

    public void setCancelsTouchesInView(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setCancelsTouchesInView:"), flag);
    }

    /** [recognizer delaysPrimaryMouseButtonEvents] */
    public boolean delaysPrimaryMouseButtonEvents() {
        return ObjC.msgSendBool(peer, ObjC.sel("delaysPrimaryMouseButtonEvents"));
    }

    public void setDelaysPrimaryMouseButtonEvents(boolean flag) {
        try {
            hSetDelaysPrimary.invokeExact(peer, ObjC.sel("setDelaysPrimaryMouseButtonEvents:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setDelaysPrimaryMouseButtonEvents: failed", t);
        }
    }
}
