package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPopover — thin wrapper over native NSPopover.
/// Thin 1:1 wrapper; every method maps to one objc_msgSend selector.
public final class NSPopover extends NSObject {

            private record Handles(MethodHandle hSetContentSize, MethodHandle hGetContentSize, MethodHandle hShow, MethodHandle hGetBool, MethodHandle hSetBool, MethodHandle hGetInt, MethodHandle hSetInt, MethodHandle hSetId, MethodHandle hGetId, MethodHandle hGetDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;


    private NSPopover(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE))
        );
    }

    public static NSPopover wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPopover(peer);
    }

    /// alloc + init
    public static NSPopover create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPopover"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p == null || p.address() == 0) throw new IllegalStateException("NSPopover alloc/init returned nil");
        return new NSPopover(p);
    }

    // ---- contentViewController ----

    /// setContentViewController:
    public void setContentViewController(NSViewController vc) {
        ensureInit();
        try {
            MemorySegment p = (vc == null ? MemorySegment.NULL : vc.peer());
            handles.hSetId().invokeExact(peer, ObjC.sel("setContentViewController:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("setContentViewController: failed", t);
        }
    }

    /// contentViewController
    public NSViewController contentViewController() {
        ensureInit();
        try {
            MemorySegment v = (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("contentViewController"));
            return NSViewController.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("contentViewController failed", t);
        }
    }

    /// Convenience: setContentView: by wrapping the view in a view controller.
    public void setContentView(NSView view) {
        NSViewController vc = NSViewController.create();
        vc.setView(view);
        setContentViewController(vc);
    }

    // ---- contentSize ----

    /// setContentSize:
    public void setContentSize(NSSize size) {
        ensureInit();
        try {
            handles.hSetContentSize().invokeExact(peer, ObjC.sel("setContentSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentSize: failed", t);
        }
    }

    /// contentSize
    public NSSize contentSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetContentSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("contentSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentSize failed", t);
        }
    }

    // ---- showRelativeToRect:ofView:preferredEdge: ----

    /// showRelativeToRect:ofView:preferredEdge:
    /// edge: 0=minX 1=minY 2=maxX 3=maxY (NSRectEdge)
    public void showRelativeToRect(NSRect rect, NSView view, long edge) {
        ensureInit();
        try {
            MemorySegment vp = (view == null ? MemorySegment.NULL : view.peer());
            handles.hShow().invokeExact(peer, ObjC.sel("showRelativeToRect:ofView:preferredEdge:"), rect.toSegment(), vp, edge);
        } catch (Throwable t) {
            throw new RuntimeException("showRelativeToRect:ofView:preferredEdge: failed", t);
        }
    }

    // ---- status-item convenience (directly from statusItem button click) ----

    /// Show popover anchored to the given view's bounds (preferredEdge = MinY = 1, below the status bar).
    public void showForView(NSView view) {
        if (view == null) return;
        showRelativeToRect(view.bounds(), view, 1L);
    }

    /// Show popover anchored to the status button (NSStatusBarButton is an NSButton).
    public void showForButton(NSButton button) {
        if (button == null) return;
        showRelativeToRect(button.bounds(), button, 1L);
    }

    /// Toggle popover anchored to the given view.
    public void toggleForView(NSView view) {
        if (isShown()) close();
        else showForView(view);
    }

    /// Toggle popover anchored to the status button.
    public void toggleForButton(NSButton button) {
        if (isShown()) close();
        else showForButton(button);
    }

    // ---- close / performClose: ----

    /// close
    public void close() {
        ensureInit();
        try {
            // close is (id, SEL) -> void
            ObjC.msgSendVoid(peer, ObjC.sel("close"));
        } catch (Throwable t) {
            throw new RuntimeException("close failed", t);
        }
    }

    /// performClose:
    public void performClose(Object sender) {
        ensureInit();
        MemorySegment s = MemorySegment.NULL;
        if (sender instanceof NSObject n) s = n.peer();
        else if (sender instanceof MemorySegment ms) s = ms;
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("performClose:"), s);
        } catch (Throwable t) {
            throw new RuntimeException("performClose: failed", t);
        }
    }

    public void performClose(NSObject sender) {
        ensureInit();
        try {
            MemorySegment p = (sender == null ? MemorySegment.NULL : sender.peer());
            handles.hSetId().invokeExact(peer, ObjC.sel("performClose:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("performClose: failed", t);
        }
    }

    // ---- isShown ----

    /// isShown
    public boolean isShown() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("isShown"));
        } catch (Throwable t) {
            throw new RuntimeException("isShown failed", t);
        }
    }

    // ---- animates ----

    /// animates
    public boolean animates() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("animates"));
        } catch (Throwable t) {
            throw new RuntimeException("animates failed", t);
        }
    }

    /// setAnimates:
    public void setAnimates(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, ObjC.sel("setAnimates:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAnimates: failed", t);
        }
    }

    // ---- behavior ----

    /// behavior — 0 applicationDefined 1 transient 2 semitransient
    public long behavior() {
        ensureInit();
        try {
            return (long) handles.hGetInt().invokeExact(peer, ObjC.sel("behavior"));
        } catch (Throwable t) {
            throw new RuntimeException("behavior failed", t);
        }
    }

    /// setBehavior:
    public void setBehavior(long behavior) {
        ensureInit();
        try {
            handles.hSetInt().invokeExact(peer, ObjC.sel("setBehavior:"), behavior);
        } catch (Throwable t) {
            throw new RuntimeException("setBehavior: failed", t);
        }
    }

    // ---- appearance ----

    /// appearance
    public MemorySegment appearancePeer() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("appearance"));
        } catch (Throwable t) {
            throw new RuntimeException("appearance failed", t);
        }
    }

    public NSObject appearance() {
        MemorySegment p = appearancePeer();
        return NSObject.wrap(p);
    }

    /// setAppearance: (nil to clear)
    public void setAppearance(MemorySegment appearance) {
        ensureInit();
        try {
            MemorySegment p = (appearance == null || appearance.address() == 0) ? MemorySegment.NULL : appearance;
            handles.hSetId().invokeExact(peer, ObjC.sel("setAppearance:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("setAppearance: failed", t);
        }
    }

    public void setAppearance(NSObject appearance) {
        setAppearance(appearance == null ? MemorySegment.NULL : appearance.peer());
    }

    // ---------------------------------------------------------------- nested types — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSPopover.h
    //   NSPopoverBehavior: ApplicationDefined 0, Transient 1, Semitransient 2
    //   NSPopoverAppearance (deprecated 10.7-10.10): Minimal 0, HUD 1 — superseded by NSAppearance
    // Docs: https://developer.apple.com/documentation/appkit/nspopover
    /// `NSPopoverBehavior` — 0=ApplicationDefined, 1=Transient, 2=Semitransient.
    public enum Behavior {
        applicationDefined(0), transientPopover(1), semitransient(2);
        public final long value;
        Behavior(long v) { this.value = v; }
        public static Behavior fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }
    /// Typed behavior getter.
    public Behavior behaviorEnum() { return Behavior.fromValue(behavior()); }
    /// Typed behavior setter.
    public void setBehavior(Behavior b) { setBehavior(b.value); }
    /// `NSPopoverAppearance` (deprecated 10.7-10.10) — 0=Minimal, 1=HUD. Kept for header completeness.
    public enum DeprecatedAppearance {
        minimal(0), hud(1);
        public final long value;
        DeprecatedAppearance(long v) { this.value = v; }
        public static DeprecatedAppearance fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    // ---- delegate (id<NSPopoverDelegate>, weak) ----
    /// delegatePeer — raw delegate id (may be NULL).
    public MemorySegment delegatePeer() {
        ensureInit();
        try { return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("delegate")); }
        catch (Throwable t) { throw new RuntimeException("delegate failed", t); }
    }
    /// setDelegate: with raw id.
    public void setDelegate(MemorySegment delegate) {
        ensureInit();
        try {
            MemorySegment p = (delegate == null || delegate.address() == 0) ? MemorySegment.NULL : delegate;
            handles.hSetId().invokeExact(peer, ObjC.sel("setDelegate:"), p);
        } catch (Throwable t) { throw new RuntimeException("setDelegate: failed", t); }
    }
    /// setDelegate: with NSObject.
    public void setDelegate(NSObject delegate) {
        setDelegate(delegate == null ? MemorySegment.NULL : delegate.peer());
    }

    // ---- effectiveAppearance (readonly NSAppearance, macOS 10.10) ----
    /// effectiveAppearancePeer — raw NSAppearance id.
    public MemorySegment effectiveAppearancePeer() {
        ensureInit();
        try { return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("effectiveAppearance")); }
        catch (Throwable t) { throw new RuntimeException("effectiveAppearance failed", t); }
    }
    /// effectiveAppearance — wrapped (NSAppearance wrapper exists in this repo).
    public NSAppearance effectiveAppearance() {
        return NSAppearance.wrap(effectiveAppearancePeer());
    }

    // ---- detached (readonly, macOS 10.10, getter isDetached) ----
    /// isDetached.
    public boolean isDetached() {
        ensureInit();
        try { return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("isDetached")); }
        catch (Throwable t) { throw new RuntimeException("isDetached failed", t); }
    }

    // ---- positioningRect (NSRect) ----
    /// positioningRect.
    public NSRect positioningRect() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) ObjC.handle(Sig.of(Ret.RECT)).invokeExact(ObjC.structSlot(), peer, ObjC.sel("positioningRect"));
            return NSRect.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("positioningRect failed", t); }
    }
    /// setPositioningRect:.
    public void setPositioningRect(NSRect rect) {
        ensureInit();
        if (rect == null) return;
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)).invokeExact(peer, ObjC.sel("setPositioningRect:"), rect.toSegment()); }
        catch (Throwable t) { throw new RuntimeException("setPositioningRect: failed", t); }
    }

    // ---- hasFullSizeContent (BOOL, macOS 14) ----
    /// hasFullSizeContent.
    public boolean hasFullSizeContent() {
        ensureInit();
        try { return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("hasFullSizeContent")); }
        catch (Throwable t) { throw new RuntimeException("hasFullSizeContent failed", t); }
    }
    /// setHasFullSizeContent:.
    public void setHasFullSizeContent(boolean flag) {
        ensureInit();
        try { handles.hSetBool().invokeExact(peer, ObjC.sel("setHasFullSizeContent:"), flag); }
        catch (Throwable t) { throw new RuntimeException("setHasFullSizeContent: failed", t); }
    }

    // ---- showRelativeToToolbarItem: (macOS 14) ----
    /// showRelativeToToolbarItem: — never call from tests (shows visibly); provided for completeness.
    public void showRelativeToToolbarItem(NSToolbarItem item) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("showRelativeToToolbarItem:"),
                    (MemorySegment) (item == null ? MemorySegment.NULL : item.peer()));
        } catch (Throwable t) { throw new RuntimeException("showRelativeToToolbarItem: failed", t); }
    }
    /// showRelativeToToolbarItem: with raw id.
    public void showRelativeToToolbarItem(MemorySegment item) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("showRelativeToToolbarItem:"),
                    (MemorySegment) (item == null ? MemorySegment.NULL : item));
        } catch (Throwable t) { throw new RuntimeException("showRelativeToToolbarItem: failed", t); }
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - -initWithCoder: (NSCoding) — omitted: NSCoder plumbing out of scope.
    // - Deprecated appearance (NSPopoverAppearance int, 10.7-10.10) — documented as DeprecatedAppearance above;
    //   no separate getter/setter (superseded by NSAppearance appearance/effectiveAppearance kept above).
    // - NSPopoverDelegate protocol methods (popoverShouldClose:, popoverShouldDetach, popoverDidDetach,
    //   detachableWindowForPopover:, popoverWill/DidShow/Close:) — delegate side, not NSPopover selectors;
    //   wire via DelegateProxy (BoolArg/VoidArg/IdArg shapes) if needed. No new upcall shape introduced here.
    // - NSPopover close-reason constants / notifications (NSPopoverCloseReason*, NSPopoverWill/DidShow/Close
    //   notifications) — NSNotificationCenter surface, not NSPopover selectors; observe via notification center.
}
