package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSButtonTouchBarItem — a Touch Bar item that shows one tappable,
/// title-labeled button wired to a target/action pair.
/// Thin 1:1, stateless wrapper over AppKit `NSButtonTouchBarItem`
/// (macOS 10.15+): every method maps to one `objc_msgSend`; optional
/// selectors are guarded with `respondsToSelector:` like
/// `NSCustomTouchBarItem`.
///
/// Creation paths mirror the Objective-C API:
/// - `create(identifier)` — alloc + `initWithIdentifier:` (bare item;
///   assign a title afterwards).
/// - `create(identifier, title, target, actionSelector)` — the one-shot
///   class factory `+buttonTouchBarItemWithIdentifier:title:target:action:`
///   (four object arguments, sent through the all-object-args escape hatch,
///   NULL-padded).
public class NSButtonTouchBarItem extends NSTouchBarItem {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hInitId, MethodHandle hId, MethodHandle hVoidId) {}
    private static volatile Handles handles;

    protected NSButtonTouchBarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSButtonTouchBarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSButtonTouchBarItem(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
        );
    }

    /// alloc + initWithIdentifier: — bare button item; set a title afterwards.
    public static NSButtonTouchBarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSButtonTouchBarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitId().invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSButtonTouchBarItem", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSButtonTouchBarItem alloc/initWithIdentifier: returned nil");
        return new NSButtonTouchBarItem(p);
    }

    /// +buttonTouchBarItemWithIdentifier:title:target:action: — standard titled
    /// button item wired to target/action in one call. `target` and
    /// `actionSelector` may be null (sent as NULL).
    public static NSButtonTouchBarItem create(String identifier, String title, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment ident = ObjC.nsstring(identifier);
        MemorySegment titleSeg = (title == null) ? MemorySegment.NULL : ObjC.nsstring(title);
        MemorySegment targetSeg = (MemorySegment) (target == null ? MemorySegment.NULL : target);
        MemorySegment actionSeg = (actionSelector == null) ? MemorySegment.NULL : ObjC.sel(actionSelector);
        MemorySegment p = ObjC.invoke(ObjC.cls("NSButtonTouchBarItem"),
                ObjC.sel("buttonTouchBarItemWithIdentifier:title:target:action:"),
                ident, titleSeg, targetSeg, actionSeg);
        if (p == null || p.address() == 0) {
            throw new IllegalStateException("buttonTouchBarItemWithIdentifier:title:target:action: returned nil");
        }
        return new NSButtonTouchBarItem(p);
    }

    /// True when the peer implements the given selector.
    private boolean responds(String selectorName) {
        try {
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel(selectorName));
        } catch (Throwable t) {
            return false;
        }
    }

    /// title — the button's text label.
    public String title() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("title"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("title failed", t);
        }
    }

    /// setTitle: — replace the button's text label (rebuilds the content).
    public void setTitle(String title) {
        ensureInit();
        try {
            MemorySegment s = (title == null) ? MemorySegment.NULL : ObjC.nsstring(title);
            handles.hVoidId().invokeExact(peer, ObjC.sel("setTitle:"), s);
        } catch (Throwable t) {
            throw new RuntimeException("setTitle: failed", t);
        }
    }

    /// setTarget: — the object that receives the action message on tap
    /// (weak property; guarded).
    public void setTarget(MemorySegment target) {
        ensureInit();
        if (!responds("setTarget:")) return;
        MemorySegment t = (MemorySegment) (target == null ? MemorySegment.NULL : target);
        ObjC.msgSendVoidId(peer, ObjC.sel("setTarget:"), t);
    }

    /// setTarget: typed overload.
    public void setTarget(NSObject target) {
        setTarget(target == null ? null : target.peer());
    }

    /// target — the action target (raw id), or null.
    public MemorySegment target() {
        ensureInit();
        if (!responds("target")) return null;
        return ObjC.msgSendId(peer, ObjC.sel("target"));
    }

    /// setAction: — the selector sent to the target on tap (guarded).
    public void setAction(String actionSelector) {
        ensureInit();
        if (actionSelector == null || !responds("setAction:")) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"), ObjC.sel(actionSelector));
    }

    /// action — the action selector (raw SEL), or null. Same shape as
    /// `NSControl.action()`.
    public MemorySegment action() {
        ensureInit();
        if (!responds("action")) return null;
        return ObjC.msgSendId(peer, ObjC.sel("action"));
    }

    /// isEnabled — whether the button responds to taps (guarded).
    public boolean isEnabled() {
        ensureInit();
        if (!responds("isEnabled")) return false;
        return ObjC.msgSendBool(peer, ObjC.sel("isEnabled"));
    }

    /// setEnabled: — enable/disable the button (guarded).
    public void setEnabled(boolean flag) {
        ensureInit();
        if (!responds("setEnabled:")) return;
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEnabled:"), flag);
    }
}
