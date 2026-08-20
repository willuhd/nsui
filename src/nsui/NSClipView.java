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

    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hVoidId;      // (id, SEL, id) -> void
    private static MethodHandle hId;          // (id, SEL) -> id
    private static MethodHandle hVoidPoint;   // (id, SEL, NSPoint) -> void
    private static MethodHandle hGetRect;     // (SegmentAllocator, id, SEL) -> NSRect
    private static MethodHandle hGetPoint;    // (SegmentAllocator, id, SEL) -> NSPoint
    private static MethodHandle hBool;        // (id, SEL) -> bool
    private static MethodHandle hVoidBool;    // (id, SEL, bool) -> void

    protected NSClipView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSClipView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSClipView(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hId = ObjC.handle(Sig.of(Ret.ID));
        hVoidPoint = ObjC.handle(Sig.of(Ret.VOID, Arg.POINT));
        hGetRect = ObjC.handle(Sig.of(Ret.RECT));
        hGetPoint = ObjC.handle(Sig.of(Ret.POINT));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hVoidBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        initialized = true;
    }

    /// [[NSClipView alloc] initWithFrame:]
    public static NSClipView create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSClipView"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSClipView", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSClipView alloc/initWithFrame: returned nil");
        return new NSClipView(p);
    }

    /// [clip documentView]
    public NSView documentView() {
        ensureInit();
        try {
            MemorySegment v = (MemorySegment) hId.invokeExact(peer, ObjC.sel("documentView"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("documentView failed", t);
        }
    }

    /// [clip setDocumentView:]
    public void setDocumentView(NSView view) {
        ensureInit();
        try {
            hVoidId.invokeExact(peer, ObjC.sel("setDocumentView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setDocumentView: failed", t);
        }
    }

    /// [clip documentRect] -> NSRect
    public NSRect documentRect() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hGetRect.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("documentRect"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("documentRect failed", t);
        }
    }

    /// [clip documentVisibleRect] -> NSRect
    public NSRect documentVisibleRect() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hGetRect.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("documentVisibleRect"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("documentVisibleRect failed", t);
        }
    }

    /// [clip scrollToPoint:]
    public void scrollToPoint(NSPoint point) {
        ensureInit();
        try {
            hVoidPoint.invokeExact(peer, ObjC.sel("scrollToPoint:"), point.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("scrollToPoint: failed", t);
        }
    }

    /// [clip copiesOnScroll]
    public boolean copiesOnScroll() {
        ensureInit();
        try {
            return (boolean) hBool.invokeExact(peer, ObjC.sel("copiesOnScroll"));
        } catch (Throwable t) {
            throw new RuntimeException("copiesOnScroll failed", t);
        }
    }

    /// [clip setCopiesOnScroll:]
    public void setCopiesOnScroll(boolean flag) {
        ensureInit();
        try {
            hVoidBool.invokeExact(peer, ObjC.sel("setCopiesOnScroll:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setCopiesOnScroll: failed", t);
        }
    }

    /// [clip drawsBackground]
    public boolean drawsBackground() {
        ensureInit();
        try {
            return (boolean) hBool.invokeExact(peer, ObjC.sel("drawsBackground"));
        } catch (Throwable t) {
            throw new RuntimeException("drawsBackground failed", t);
        }
    }

    /// [clip setDrawsBackground:]
    public void setDrawsBackground(boolean flag) {
        ensureInit();
        try {
            hVoidBool.invokeExact(peer, ObjC.sel("setDrawsBackground:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setDrawsBackground: failed", t);
        }
    }

    /// [clip backgroundColor] -> NSColor
    public NSColor backgroundColor() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hId.invokeExact(peer, ObjC.sel("backgroundColor"));
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("backgroundColor failed", t);
        }
    }

    /// [clip setBackgroundColor:]
    public void setBackgroundColor(NSColor color) {
        ensureInit();
        try {
            hVoidId.invokeExact(peer, ObjC.sel("setBackgroundColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setBackgroundColor: failed", t);
        }
    }
}
