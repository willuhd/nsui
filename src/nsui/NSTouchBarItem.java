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
/// Coverage: COMPLETE after this batch — identifier/visibilityPriority/view/viewController/
/// customizationLabel/visible plus the four NSTouchBarItemIdentifier* extern constants below.
/// All shapes are the registered (ID ())/(VOID,ID)/(INT ())/(VOID,INT)/(BOOL ())/(VOID,BOOL) family
/// (grep Sig.java per shape). OMITTED: -initWithCoder: (NSCoding archiving, out of scope for a
/// stateless wrapper); -init is NS_UNAVAILABLE by design (use create(identifier)).
public class NSTouchBarItem extends NSObject {

            private record Handles(MethodHandle hInitId, MethodHandle hId, MethodHandle hVoidId, MethodHandle hBool, MethodHandle hVoidBool) {}
    private static volatile Handles handles;

    protected NSTouchBarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTouchBarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTouchBarItem(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL))
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

    /// visibilityPriority — long.
    public long visibilityPriority() {
        return ObjC.msgSendLong(peer, ObjC.sel("visibilityPriority"));
    }

    public void setVisibilityPriority(long p) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setVisibilityPriority:"), p);
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

    public void setVisible(boolean flag) {
        ensureInit();
        try {
            MemorySegment sel = ObjC.sel("setVisible:");
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return;
            handles.hVoidBool().invokeExact(peer, sel, flag);
        } catch (Throwable t) { /* no-op if absent */ }
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

    public void setView(NSView view) {
        ensureInit();
        try {
            MemorySegment sel = ObjC.sel("setView:");
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return;
            handles.hVoidId().invokeExact(peer, sel, (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) { /* no-op if absent */ }
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
