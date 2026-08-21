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

        private static synchronized void ensureInit() {
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
            MemorySegment s = (MemorySegment) handles.hGetContentSize().invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("contentSize"));
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
}
