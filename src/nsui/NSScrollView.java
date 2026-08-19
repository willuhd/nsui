package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSScrollView — a view that scrolls its document view (e.g. an {@link NSTableView})
 * and draws scroller knobs. Thin, 1:1, stateless wrapper over the native
 * {@code NSScrollView}: each method maps to one {@code objc_msgSend} selector.
 */
public final class NSScrollView extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id  [initWithFrame:]
    private static MethodHandle hVoidId;      // (id, SEL, id) -> void    [setDocumentView:]
    private static MethodHandle hVoidBool;    // (id, SEL, bool) -> void  [setHasVerticalScroller:/setHasHorizontalScroller:/setAutohidesScrollers:]
    private static MethodHandle hVoidInt;     // (id, SEL, long) -> void  [setBorderType:]
    private static MethodHandle hGetDouble;   // (id, SEL) -> double
    private static MethodHandle hSetDouble;   // (id, SEL, double) -> void
    private static MethodHandle hGetSize;     // (id, SEL) -> NSSize
    private static MethodHandle hGetRect;     // (id, SEL) -> NSRect

    private NSScrollView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hVoidBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hVoidInt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hGetDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hGetSize = ObjC.handle(Sig.of(Ret.SIZE));
        hGetRect = ObjC.handle(Sig.of(Ret.RECT));
        initialized = true;
    }

    /** {@code [[NSScrollView alloc] initWithFrame:frame]} — a new scroll view. */
    public static NSScrollView create(NSRect frame) {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(ObjC.cls("NSScrollView"), ObjC.sel("alloc"));
        try {
            v = (MemorySegment) hInitFrame.invokeExact(v, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSScrollView", t);
        }
        if (v.address() == 0) {
            throw new IllegalStateException("NSScrollView alloc/initWithFrame: returned nil");
        }
        return new NSScrollView(v);
    }

    // ---------------------------------------------------------------- instance API

    /** [scroll setDocumentView:] — the scrollable document (the table). */
    public void setDocumentView(NSView documentView) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("setDocumentView:"), documentView.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setDocumentView: failed", t);
        }
    }

    /** [scroll setHasVerticalScroller:] — show/hide the vertical scroller. */
    public void setHasVerticalScroller(boolean flag) {
        try {
            hVoidBool.invokeExact(peer, ObjC.sel("setHasVerticalScroller:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setHasVerticalScroller: failed", t);
        }
    }

    /** [scroll setHasHorizontalScroller:] — show/hide the horizontal scroller. */
    public void setHasHorizontalScroller(boolean flag) {
        try {
            hVoidBool.invokeExact(peer, ObjC.sel("setHasHorizontalScroller:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setHasHorizontalScroller: failed", t);
        }
    }

    /** [scroll setAutohidesScrollers:] — auto-hide scrollers when not needed. */
    public void setAutohidesScrollers(boolean flag) {
        try {
            hVoidBool.invokeExact(peer, ObjC.sel("setAutohidesScrollers:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAutohidesScrollers: failed", t);
        }
    }

    /** [scroll setBorderType:] — the border style (NSBezelBorder=1, NSLineBorder=2, NSNoBorder=0, ...). */
    public void setBorderType(long borderType) {
        try {
            hVoidInt.invokeExact(peer, ObjC.sel("setBorderType:"), borderType);
        } catch (Throwable t) {
            throw new RuntimeException("setBorderType: failed", t);
        }
    }

    // ---------------------------------------------------------------- additional — completeness

    /** [scroll documentView] — the document view (may be nil). */
    public NSView documentView() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("documentView"));
        return NSView.wrap(v);
    }

    /** [scroll contentView] — the clip view. */
    public NSView contentView() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("contentView"));
        return NSView.wrap(v);
    }

    /** [scroll backgroundColor]. */
    public NSColor backgroundColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("backgroundColor"));
        if (c == null || c.address() == 0) return null;
        try {
            var ctor = NSColor.class.getDeclaredConstructor(MemorySegment.class);
            ctor.setAccessible(true);
            return ctor.newInstance(c);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("wrap NSColor failed", e);
        }
    }

    /** [scroll setBackgroundColor:]. */
    public void setBackgroundColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBackgroundColor:"), color == null ? MemorySegment.NULL : color.peer());
    }

    /** [scroll drawsBackground]. */
    public boolean drawsBackground() {
        return ObjC.msgSendBool(peer, ObjC.sel("drawsBackground"));
    }

    /** [scroll setDrawsBackground:]. */
    public void setDrawsBackground(boolean flag) {
        try {
            hVoidBool.invokeExact(peer, ObjC.sel("setDrawsBackground:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setDrawsBackground: failed", t);
        }
    }

    /** [scroll hasVerticalScroller]. */
    public boolean hasVerticalScroller() {
        return ObjC.msgSendBool(peer, ObjC.sel("hasVerticalScroller"));
    }

    /** [scroll hasHorizontalScroller]. */
    public boolean hasHorizontalScroller() {
        return ObjC.msgSendBool(peer, ObjC.sel("hasHorizontalScroller"));
    }

    /** [scroll autohidesScrollers]. */
    public boolean autohidesScrollers() {
        return ObjC.msgSendBool(peer, ObjC.sel("autohidesScrollers"));
    }

    /** [scroll allowsMagnification]. */
    public boolean allowsMagnification() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsMagnification"));
    }

    /** [scroll setAllowsMagnification:]. */
    public void setAllowsMagnification(boolean flag) {
        try {
            hVoidBool.invokeExact(peer, ObjC.sel("setAllowsMagnification:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsMagnification: failed", t);
        }
    }

    /** [scroll magnification] — double. */
    public double magnification() {
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("magnification"));
        } catch (Throwable t) {
            throw new RuntimeException("magnification failed", t);
        }
    }

    /** [scroll setMagnification:]. */
    public void setMagnification(double mag) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMagnification:"), mag);
        } catch (Throwable t) {
            throw new RuntimeException("setMagnification: failed", t);
        }
    }

    /** [scroll maxMagnification]. */
    public double maxMagnification() {
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("maxMagnification"));
        } catch (Throwable t) {
            throw new RuntimeException("maxMagnification failed", t);
        }
    }

    /** [scroll setMaxMagnification:]. */
    public void setMaxMagnification(double mag) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMaxMagnification:"), mag);
        } catch (Throwable t) {
            throw new RuntimeException("setMaxMagnification: failed", t);
        }
    }

    /** [scroll minMagnification]. */
    public double minMagnification() {
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("minMagnification"));
        } catch (Throwable t) {
            throw new RuntimeException("minMagnification failed", t);
        }
    }

    /** [scroll setMinMagnification:]. */
    public void setMinMagnification(double mag) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMinMagnification:"), mag);
        } catch (Throwable t) {
            throw new RuntimeException("setMinMagnification: failed", t);
        }
    }

    /** [scroll contentSize] — NSSize readonly. */
    public NSSize contentSize() {
        try {
            MemorySegment s = (MemorySegment) hGetSize.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("contentSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentSize failed", t);
        }
    }

    /** [scroll documentVisibleRect] — NSRect readonly. */
    public NSRect documentVisibleRect() {
        try {
            MemorySegment r = (MemorySegment) hGetRect.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("documentVisibleRect"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("documentVisibleRect failed", t);
        }
    }

    /** [scroll borderType]. */
    public long borderType() {
        return ObjC.msgSendLong(peer, ObjC.sel("borderType"));
    }
}
