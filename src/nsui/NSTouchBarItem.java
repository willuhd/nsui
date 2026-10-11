package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTouchBarItem — minimal wrap over AppKit NSTouchBarItem.
/// Thin 1:1, stateless.
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSTouchBarItem.h
/// Coverage: complete against the NSTouchBarItem.h API this wrapper exposes —
/// identifier/visibilityPriority/view/viewController/customizationLabel/visible plus the four
/// NSTouchBarItemIdentifier* extern constants below. NOTE: `view` and `visible` are readonly, so
/// `setView:`/`setVisible:` are not supported and throw IllegalStateException; view-backed
/// subclasses (NSCustomTouchBarItem) override setView.
/// All shapes are the registered (ID ())/(VOID,ID)/(FLOAT ())/(VOID,FLOAT)/(BOOL ())/(VOID,BOOL) family
/// (grep Sig.java per shape). OMITTED: -initWithCoder: (NSCoding archiving, out of scope for a
/// stateless wrapper); -init is NS_UNAVAILABLE by design (use create(identifier)).
public class NSTouchBarItem extends NSObject {

            private record Handles(MethodHandle hInitId, MethodHandle hId, MethodHandle hBool, MethodHandle hFloat, MethodHandle hSetFloat) {} {}
    private static volatile Handles handles;

    protected NSTouchBarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTouchBarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTouchBarItem(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT))
        );
    }

    /// alloc + initWithIdentifier: — create item with identifier.
    public static NSTouchBarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSTouchBarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitId().invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSTouchBarItem", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSTouchBarItem alloc/initWithIdentifier: returned nil");
        return new NSTouchBarItem(p);
    }

    /// identifier — NSString.
    public String identifier() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("identifier"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("identifier failed", t);
        }
    }

    /// visibilityPriority — `NSTouchBarItem.Priority` is a **float** in AppKit; the
    /// old long read/write hit the integer register and returned garbage.
    public float visibilityPriority() {
        ensureInit();
        try { return (float) handles.hFloat().invokeExact(peer, ObjC.sel("visibilityPriority")); }
        catch (Throwable t) { throw new RuntimeException("visibilityPriority failed", t); }
    }

    public void setVisibilityPriority(float p) {
        ensureInit();
        try { handles.hSetFloat().invokeExact(peer, ObjC.sel("setVisibilityPriority:"), p); }
        catch (Throwable t) { throw new RuntimeException("setVisibilityPriority: failed", t); }
    }

    /// isVisible — guarded; returns false if selector absent (not all items expose it).
    public boolean isVisible() {
        ensureInit();
        // Guard: not all NSTouchBarItem subclasses respond to isVisible
        try {
            MemorySegment sel = ObjC.sel("isVisible");
            // quick respondsTo check via ObjC
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return false;
            return (boolean) handles.hBool().invokeExact(peer, sel);
        } catch (Throwable t) { return false; }
    }

    /// setVisible: — NOT settable: `NSTouchBarItem.visible` is a readonly property, so no
    /// subclass implements `setVisible:`; this always throws rather than silently doing nothing.
    public void setVisible(boolean flag) {
        throw new IllegalStateException("NSTouchBarItem.visible is readonly — setVisible: is not"
                + " supported (change the item's visibilityPriority or remove/add it from the bar)");
    }

    /// view — NSView peer or null (guarded; not all items expose view).
    public NSView view() {
        ensureInit();
        try {
            MemorySegment sel = ObjC.sel("view");
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return null;
            MemorySegment v = (MemorySegment) handles.hId().invokeExact(peer, sel);
            return NSView.wrap(v);
        } catch (Throwable t) { return null; }
    }

    /// setView: — NOT settable on the base class: `NSTouchBarItem.view` is readonly and only
    /// subclasses such as NSCustomTouchBarItem/NSViewTouchBarItem expose a setter; this always
    /// throws rather than silently doing nothing.
    public void setView(NSView view) {
        throw new IllegalStateException("NSTouchBarItem.view is readonly — use a view-backed subclass"
                + " (NSCustomTouchBarItem.setView(NSView)) instead");
    }

    // ---- completeness: remaining header API in registered shapes ----
    /// viewController — the item's view controller (or nil; subclass override point).
    /// Shape (ID ()) is in the vocabulary; guarded like view (not all items expose it).
    public NSViewController viewController() {
        ensureInit();
        try {
            MemorySegment sel = ObjC.sel("viewController");
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return null;
            MemorySegment v = (MemorySegment) handles.hId().invokeExact(peer, sel);
            return NSViewController.wrap(v);
        } catch (Throwable t) { return null; }
    }

    /// customizationLabel — user-visible string during customization (empty string by default).
    /// Shape (ID ()) is in the vocabulary.
    public String customizationLabel() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("customizationLabel"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("customizationLabel failed", t);
        }
    }

    // ---- NSTouchBarItemIdentifier* extern constants (not selectors; plain strings) ----
    /// Identifier for a small fixed space in an NSTouchBar.
    public static final String FIXED_SPACE_SMALL = "NSTouchBarItemIdentifierFixedSpaceSmall";
    /// Identifier for a large fixed space in an NSTouchBar.
    public static final String FIXED_SPACE_LARGE = "NSTouchBarItemIdentifierFixedSpaceLarge";
    /// Identifier for a flexible space in an NSTouchBar.
    public static final String FLEXIBLE_SPACE = "NSTouchBarItemIdentifierFlexibleSpace";
    /// Identifier for the special "other items proxy" (nests nearer-to-first-responder bars).
    public static final String OTHER_ITEMS_PROXY = "NSTouchBarItemIdentifierOtherItemsProxy";
}
