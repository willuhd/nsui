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
///
/// Coverage notes (header: NSGestureRecognizer.h wins on API truth):
/// - Wrapped: target/action (single + add/remove pairs), state, enabled,
///   view, delegate, the whole delays* family, allowedTouchTypes,
///   pressureConfiguration, locationInView:, cancelsTouchesInView, reset,
///   the four prevention/requirement queries, and the mouse/key/tablet/
///   magnify/rotate/pressure/touch event sinks (all `(void,id)`).
/// - Omitted: initWithCoder: (init overloads are covered by create()); the
///   NSGestureRecognizerDelegate protocol methods (need upcall machinery);
///   the state setter (subclass-only by contract). `name`/`modifierFlags`
///   are macOS 26+ like the already-wrapped `mouseCancelled:` and are
///   wrapped unconditionally for the same reason.
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
            MemorySegment seg = (MemorySegment) handles.hLocation().invokeExact(ObjC.structSlot(), peer, ObjC.sel("locationInView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
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

    // ------------------------------------------------- delays* family (bool)

    /// [recognizer delaysSecondaryMouseButtonEvents].
    public boolean delaysSecondaryMouseButtonEvents() {
        return ObjC.msgSendBool(peer, ObjC.sel("delaysSecondaryMouseButtonEvents"));
    }

    /// setDelaysSecondaryMouseButtonEvents:.
    public void setDelaysSecondaryMouseButtonEvents(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDelaysSecondaryMouseButtonEvents:"), flag);
    }

    /// [recognizer delaysOtherMouseButtonEvents].
    public boolean delaysOtherMouseButtonEvents() {
        return ObjC.msgSendBool(peer, ObjC.sel("delaysOtherMouseButtonEvents"));
    }

    /// setDelaysOtherMouseButtonEvents:.
    public void setDelaysOtherMouseButtonEvents(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDelaysOtherMouseButtonEvents:"), flag);
    }

    /// [recognizer delaysKeyEvents].
    public boolean delaysKeyEvents() {
        return ObjC.msgSendBool(peer, ObjC.sel("delaysKeyEvents"));
    }

    /// setDelaysKeyEvents:.
    public void setDelaysKeyEvents(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDelaysKeyEvents:"), flag);
    }

    /// [recognizer delaysMagnificationEvents].
    public boolean delaysMagnificationEvents() {
        return ObjC.msgSendBool(peer, ObjC.sel("delaysMagnificationEvents"));
    }

    /// setDelaysMagnificationEvents:.
    public void setDelaysMagnificationEvents(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDelaysMagnificationEvents:"), flag);
    }

    /// [recognizer delaysRotationEvents].
    public boolean delaysRotationEvents() {
        return ObjC.msgSendBool(peer, ObjC.sel("delaysRotationEvents"));
    }

    /// setDelaysRotationEvents:.
    public void setDelaysRotationEvents(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDelaysRotationEvents:"), flag);
    }

    // ------------------------------------------------- touch configuration

    /// [recognizer allowedTouchTypes] — NSTouchTypeMask (NSInteger).
    public long allowedTouchTypes() {
        return ObjC.msgSendLong(peer, ObjC.sel("allowedTouchTypes"));
    }

    /// setAllowedTouchTypes:.
    public void setAllowedTouchTypes(long mask) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setAllowedTouchTypes:"), mask);
    }

    /// [recognizer pressureConfiguration] — NSPressureConfiguration or null
    /// (no typed wrapper in this batch, so the raw peer).
    public MemorySegment pressureConfiguration() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("pressureConfiguration"));
        return (p == null || p.address() == 0) ? null : p;
    }

    /// setPressureConfiguration: (null clears).
    public void setPressureConfiguration(MemorySegment config) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPressureConfiguration:"),
                config == null ? MemorySegment.NULL : config);
    }

    // ------------------------------------------------- lifecycle / precedence

    /// reset — return the recognizer to its idle state.
    public void reset() {
        ObjC.msgSendVoid(peer, ObjC.sel("reset"));
    }

    private boolean queryRecognizer(String selector, NSGestureRecognizer other) {
        ensureInit();
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer,
                    ObjC.sel(selector),
                    (MemorySegment) (other == null ? MemorySegment.NULL : other.peer()));
        } catch (Throwable t) { throw new RuntimeException(selector + " failed", t); }
    }

    /// canPreventGestureRecognizer:.
    public boolean canPreventGestureRecognizer(NSGestureRecognizer prevented) {
        return queryRecognizer("canPreventGestureRecognizer:", prevented);
    }

    /// canBePreventedByGestureRecognizer:.
    public boolean canBePreventedByGestureRecognizer(NSGestureRecognizer preventing) {
        return queryRecognizer("canBePreventedByGestureRecognizer:", preventing);
    }

    /// shouldRequireFailureOfGestureRecognizer:.
    public boolean shouldRequireFailureOfGestureRecognizer(NSGestureRecognizer other) {
        return queryRecognizer("shouldRequireFailureOfGestureRecognizer:", other);
    }

    /// shouldBeRequiredToFailByGestureRecognizer:.
    public boolean shouldBeRequiredToFailByGestureRecognizer(NSGestureRecognizer other) {
        return queryRecognizer("shouldBeRequiredToFailByGestureRecognizer:", other);
    }

    // ------------------------------------------------- event sinks (send side)
    // Like NSResponder.sendEvent: these SEND the selector; they are the
    // manual-dispatch path for synthesized routing, not the callback path.

    private void sendGestureEvent(String selector, NSEvent event) {
        ensureInit();
        try {
            ObjC.handle(Sig.of(Ret.VOID, Arg.ID)).invokeExact(peer, ObjC.sel(selector),
                    (MemorySegment) (event == null ? MemorySegment.NULL : event.peer()));
        } catch (Throwable t) { throw new RuntimeException(selector + " failed", t); }
    }

    /// mouseDown:.
    public void mouseDown(NSEvent event) { sendGestureEvent("mouseDown:", event); }

    /// rightMouseDown:.
    public void rightMouseDown(NSEvent event) { sendGestureEvent("rightMouseDown:", event); }

    /// otherMouseDown:.
    public void otherMouseDown(NSEvent event) { sendGestureEvent("otherMouseDown:", event); }

    /// mouseUp:.
    public void mouseUp(NSEvent event) { sendGestureEvent("mouseUp:", event); }

    /// rightMouseUp:.
    public void rightMouseUp(NSEvent event) { sendGestureEvent("rightMouseUp:", event); }

    /// otherMouseUp:.
    public void otherMouseUp(NSEvent event) { sendGestureEvent("otherMouseUp:", event); }

    /// mouseDragged:.
    public void mouseDragged(NSEvent event) { sendGestureEvent("mouseDragged:", event); }

    /// rightMouseDragged:.
    public void rightMouseDragged(NSEvent event) { sendGestureEvent("rightMouseDragged:", event); }

    /// otherMouseDragged:.
    public void otherMouseDragged(NSEvent event) { sendGestureEvent("otherMouseDragged:", event); }

    /// mouseCancelled: (macOS 26+).
    public void mouseCancelled(NSEvent event) { sendGestureEvent("mouseCancelled:", event); }

    /// keyDown:.
    public void keyDown(NSEvent event) { sendGestureEvent("keyDown:", event); }

    /// keyUp:.
    public void keyUp(NSEvent event) { sendGestureEvent("keyUp:", event); }

    /// flagsChanged:.
    public void flagsChanged(NSEvent event) { sendGestureEvent("flagsChanged:", event); }

    /// tabletPoint:.
    public void tabletPoint(NSEvent event) { sendGestureEvent("tabletPoint:", event); }

    /// magnifyWithEvent:.
    public void magnifyWithEvent(NSEvent event) { sendGestureEvent("magnifyWithEvent:", event); }

    /// rotateWithEvent:.
    public void rotateWithEvent(NSEvent event) { sendGestureEvent("rotateWithEvent:", event); }

    /// pressureChangeWithEvent:.
    public void pressureChangeWithEvent(NSEvent event) { sendGestureEvent("pressureChangeWithEvent:", event); }

    /// touchesBeganWithEvent:.
    public void touchesBeganWithEvent(NSEvent event) { sendGestureEvent("touchesBeganWithEvent:", event); }

    /// touchesMovedWithEvent:.
    public void touchesMovedWithEvent(NSEvent event) { sendGestureEvent("touchesMovedWithEvent:", event); }

    /// touchesEndedWithEvent:.
    public void touchesEndedWithEvent(NSEvent event) { sendGestureEvent("touchesEndedWithEvent:", event); }

    /// touchesCancelledWithEvent:.
    public void touchesCancelledWithEvent(NSEvent event) { sendGestureEvent("touchesCancelledWithEvent:", event); }

    // ------------------------------------------------- macOS 26 identity

    /// name — the recognizer's developer-assigned name, or null (macOS 26+).
    public String name() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("name"));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("name failed", t); }
    }

    /// setName: — assign a developer name (null clears; macOS 26+).
    public void setName(String name) {
        ensureInit();
        try {
            handles.hSetDelegate().invokeExact(peer, ObjC.sel("setName:"),
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)));
        } catch (Throwable t) { throw new RuntimeException("setName: failed", t); }
    }

    /// modifierFlags — the modifier flags captured for the gesture (macOS 26+).
    public long modifierFlags() {
        return ObjC.msgSendLong(peer, ObjC.sel("modifierFlags"));
    }
}
