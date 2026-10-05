package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSClipView — minimal wrapper over AppKit NSClipView.
/// The content view of an NSScrollView; clips its document view.
public class NSClipView extends NSView {

            private record Handles(MethodHandle hVoidId, MethodHandle hId, MethodHandle hVoidPoint, MethodHandle hGetRect, MethodHandle hGetPoint, MethodHandle hBool, MethodHandle hVoidBool) {}
    private static volatile Handles handles;

    protected NSClipView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSClipView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSClipView(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.RECT)),
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL))
        );
    }

    /// [[NSClipView alloc] initWithFrame:]
        public static NSClipView create(NSRect frame) {
        ensureInit();
        return new NSClipView(ObjC.newView("NSClipView", frame));
    }

    /// [clip documentView]
    public NSView documentView() {
        ensureInit();
        try {
            MemorySegment v = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("documentView"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("documentView failed", t);
        }
    }

    /// [clip setDocumentView:]
    public void setDocumentView(NSView view) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setDocumentView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setDocumentView: failed", t);
        }
    }

    /// [clip documentRect] -> NSRect
    public NSRect documentRect() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) handles.hGetRect().invokeExact(ObjC.structSlot(), peer, ObjC.sel("documentRect"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("documentRect failed", t);
        }
    }

    /// [clip documentVisibleRect] -> NSRect
    public NSRect documentVisibleRect() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) handles.hGetRect().invokeExact(ObjC.structSlot(), peer, ObjC.sel("documentVisibleRect"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("documentVisibleRect failed", t);
        }
    }

    /// [clip scrollToPoint:]
    public void scrollToPoint(NSPoint point) {
        ensureInit();
        try {
            handles.hVoidPoint().invokeExact(peer, ObjC.sel("scrollToPoint:"), point.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("scrollToPoint: failed", t);
        }
    }

    /// [clip copiesOnScroll]
    public boolean copiesOnScroll() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, ObjC.sel("copiesOnScroll"));
        } catch (Throwable t) {
            throw new RuntimeException("copiesOnScroll failed", t);
        }
    }

    /// [clip setCopiesOnScroll:]
    public void setCopiesOnScroll(boolean flag) {
        ensureInit();
        try {
            handles.hVoidBool().invokeExact(peer, ObjC.sel("setCopiesOnScroll:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setCopiesOnScroll: failed", t);
        }
    }

    /// [clip drawsBackground]
    public boolean drawsBackground() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, ObjC.sel("drawsBackground"));
        } catch (Throwable t) {
            throw new RuntimeException("drawsBackground failed", t);
        }
    }

    /// [clip setDrawsBackground:]
    public void setDrawsBackground(boolean flag) {
        ensureInit();
        try {
            handles.hVoidBool().invokeExact(peer, ObjC.sel("setDrawsBackground:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setDrawsBackground: failed", t);
        }
    }

    /// [clip backgroundColor] -> NSColor
    public NSColor backgroundColor() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("backgroundColor"));
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("backgroundColor failed", t);
        }
    }

    /// [clip setBackgroundColor:]
    public void setBackgroundColor(NSColor color) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setBackgroundColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setBackgroundColor: failed", t);
        }
    }

    // ---------------------------------------------------------------- header-completeness batch (NSClipView.h)
    //
    // Omitted:
    // - deprecated constrainScrollPoint: (use constrainBoundsRect: instead) and copiesOnScroll is already
    //   wrapped (deprecated no-op setter, kept for source compatibility).
    // - inexpressible (absent from Sig vocabulary, requested): scrollClipView:toPoint: (NSView category) needs
    //   of(VOID,ID,POINT).

    /// [clip documentCursor] — NSCursor (may be nil).
    public NSCursor documentCursor() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("documentCursor"));
            return NSCursor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("documentCursor failed", t);
        }
    }

    /// [clip setDocumentCursor:] (nil clears).
    public void setDocumentCursor(NSCursor cursor) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setDocumentCursor:"), (MemorySegment) (cursor == null ? MemorySegment.NULL : cursor.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setDocumentCursor: failed", t);
        }
    }

    /// [clip viewFrameChanged:] — notification that the frame changed.
    public void viewFrameChanged(NSObject notification) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("viewFrameChanged:"), notification.peer());
        } catch (Throwable t) {
            throw new RuntimeException("viewFrameChanged: failed", t);
        }
    }

    /// [clip viewBoundsChanged:] — notification that the bounds changed.
    public void viewBoundsChanged(NSObject notification) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("viewBoundsChanged:"), notification.peer());
        } catch (Throwable t) {
            throw new RuntimeException("viewBoundsChanged: failed", t);
        }
    }

    /// [clip autoscroll:] — auto-scroll given an event.
    public boolean autoscroll(NSEvent event) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("autoscroll:"), event.peer());
        } catch (Throwable t) {
            throw new RuntimeException("autoscroll: failed", t);
        }
    }

    /// [clip constrainBoundsRect:] — constrain a proposed bounds rect.
    public NSRect constrainBoundsRect(NSRect proposedBounds) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RECT, Arg.RECT));
            MemorySegment r = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("constrainBoundsRect:"), proposedBounds.toSegment());
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("constrainBoundsRect: failed", t);
        }
    }

    /// [clip contentInsets] — NSEdgeInsets (32-byte struct, same ABI class as NSRect).
    public NSEdgeInsets contentInsets() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RECT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("contentInsets"));
            return NSEdgeInsets.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentInsets failed", t);
        }
    }

    /// [clip setContentInsets:] — NSEdgeInsets by value (RECT shape).
    public void setContentInsets(NSEdgeInsets insets) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
            h.invokeExact(peer, ObjC.sel("setContentInsets:"), insets.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentInsets: failed", t);
        }
    }

    /// [clip automaticallyAdjustsContentInsets].
    public boolean automaticallyAdjustsContentInsets() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, ObjC.sel("automaticallyAdjustsContentInsets"));
        } catch (Throwable t) {
            throw new RuntimeException("automaticallyAdjustsContentInsets failed", t);
        }
    }

    /// [clip setAutomaticallyAdjustsContentInsets:].
    public void setAutomaticallyAdjustsContentInsets(boolean flag) {
        ensureInit();
        try {
            handles.hVoidBool().invokeExact(peer, ObjC.sel("setAutomaticallyAdjustsContentInsets:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAutomaticallyAdjustsContentInsets: failed", t);
        }
    }
}
