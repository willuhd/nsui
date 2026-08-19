package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSImageView — an AppKit view that displays an {@link NSImage}. Thin, 1:1,
 * stateless wrapper over a native {@code NSImageView}. It is an {@link NSControl}
 * (an {@link NSView}), so it drops into any view hierarchy and can be positioned
 * with {@link #setFrame(NSRect)}.
 */
public final class NSImageView extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;  // (id, SEL, NSRect) -> id
    private static MethodHandle hImage;      // (id, SEL) -> id [image]
    private static MethodHandle hSetImage;   // (id, SEL, id) -> void
    private static MethodHandle hAnimates;   // (id, SEL) -> bool [animates]
    private static MethodHandle hSetAnimates; // (id, SEL, bool) -> void
    private static MethodHandle hAllowsCut;  // (id, SEL) -> bool
    private static MethodHandle hSetAllowsCut; // (id, SEL, bool) -> void
    private static MethodHandle hEditable;   // (id, SEL) -> bool [isEditable]
    private static MethodHandle hSetEditable; // (id, SEL, bool) -> void
    private static MethodHandle hAlign;      // (id, SEL) -> long [imageAlignment]
    private static MethodHandle hSetAlign;   // (id, SEL, long) -> void
    private static MethodHandle hScaling;    // (id, SEL) -> long
    private static MethodHandle hSetScaling; // (id, SEL, long) -> void
    private static MethodHandle hFrameStyle; // (id, SEL) -> long
    private static MethodHandle hSetFrameStyle; // (id, SEL, long) -> void

    private NSImageView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hImage = ObjC.handle(Sig.of(Ret.ID));
        hSetImage = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hAnimates = ObjC.handle(Sig.of(Ret.BOOL));
        hSetAnimates = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hAllowsCut = ObjC.handle(Sig.of(Ret.BOOL));
        hSetAllowsCut = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hEditable = ObjC.handle(Sig.of(Ret.BOOL));
        hSetEditable = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hAlign = ObjC.handle(Sig.of(Ret.INT));
        hSetAlign = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hScaling = ObjC.handle(Sig.of(Ret.INT));
        hSetScaling = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hFrameStyle = ObjC.handle(Sig.of(Ret.INT));
        hSetFrameStyle = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        initialized = true;
    }

    /** {@code [[NSImageView alloc] initWithFrame:frame]} — a new image view at the given rect. */
    public static NSImageView create(NSRect frame) {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(ObjC.cls("NSImageView"), ObjC.sel("alloc"));
        try {
            v = (MemorySegment) hInitFrame.invokeExact(v, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSImageView", t);
        }
        if (v.address() == 0) {
            throw new IllegalStateException("NSImageView alloc/initWithFrame: returned nil");
        }
        return new NSImageView(v);
    }

    // ---------------------------------------------------------------- instance API

    /** [imageView image] — the displayed image peer, or null. */
    public NSImage image() {
        try {
            MemorySegment p = (MemorySegment) hImage.invokeExact(peer, ObjC.sel("image"));
            return NSImage.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("image failed", t);
        }
    }

    /** Raw peer for [imageView image] — id return without wrapping. */
    public MemorySegment imagePeer() {
        try {
            MemorySegment p = (MemorySegment) hImage.invokeExact(peer, ObjC.sel("image"));
            return p;
        } catch (Throwable t) {
            throw new RuntimeException("image failed", t);
        }
    }

    /** [imageView setImage:] — the image displayed. */
    public void setImage(NSImage image) {
        try {
            MemorySegment p = image == null ? MemorySegment.NULL : image.peer();
            hSetImage.invokeExact(peer, ObjC.sel("setImage:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("setImage: failed", t);
        }
    }

    /** [imageView animates] — whether animated images animate automatically. */
    public boolean animates() {
        try {
            return (boolean) hAnimates.invokeExact(peer, ObjC.sel("animates"));
        } catch (Throwable t) {
            throw new RuntimeException("animates failed", t);
        }
    }

    /** [imageView setAnimates:] — animate automatically. */
    public void setAnimates(boolean flag) {
        try {
            hSetAnimates.invokeExact(peer, ObjC.sel("setAnimates:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAnimates: failed", t);
        }
    }

    /** [imageView allowsCutCopyPaste] — whether cut/copy/paste is allowed. */
    public boolean allowsCutCopyPaste() {
        try {
            return (boolean) hAllowsCut.invokeExact(peer, ObjC.sel("allowsCutCopyPaste"));
        } catch (Throwable t) {
            throw new RuntimeException("allowsCutCopyPaste failed", t);
        }
    }

    /** [imageView setAllowsCutCopyPaste:] — allow cut/copy/paste. */
    public void setAllowsCutCopyPaste(boolean flag) {
        try {
            hSetAllowsCut.invokeExact(peer, ObjC.sel("setAllowsCutCopyPaste:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsCutCopyPaste: failed", t);
        }
    }

    /** [imageView isEditable] — whether the image can be edited/dragged. */
    public boolean isEditable() {
        try {
            return (boolean) hEditable.invokeExact(peer, ObjC.sel("isEditable"));
        } catch (Throwable t) {
            throw new RuntimeException("isEditable failed", t);
        }
    }

    /** [imageView setEditable:] — allow editing/dragging. */
    public void setEditable(boolean flag) {
        try {
            hSetEditable.invokeExact(peer, ObjC.sel("setEditable:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setEditable: failed", t);
        }
    }

    /** [imageView imageAlignment] — NSImageAlignment (0=center,1=top, etc). */
    public long imageAlignment() {
        try {
            return (long) hAlign.invokeExact(peer, ObjC.sel("imageAlignment"));
        } catch (Throwable t) {
            throw new RuntimeException("imageAlignment failed", t);
        }
    }

    /** [imageView setImageAlignment:] — NSImageAlignment. */
    public void setImageAlignment(long alignment) {
        try {
            hSetAlign.invokeExact(peer, ObjC.sel("setImageAlignment:"), alignment);
        } catch (Throwable t) {
            throw new RuntimeException("setImageAlignment: failed", t);
        }
    }

    /**
     * [imageView setImageScaling:] — NSImageScaling.
     * {@code NSImageScaleProportionallyUpOrDown = 3} is the standard choice.
     */
    public void setImageScaling(long scaling) {
        try {
            hSetScaling.invokeExact(peer, ObjC.sel("setImageScaling:"), scaling);
        } catch (Throwable t) {
            throw new RuntimeException("setImageScaling: failed", t);
        }
    }

    /** [imageView imageScaling] — NSImageScaling. */
    public long imageScaling() {
        try {
            return (long) hScaling.invokeExact(peer, ObjC.sel("imageScaling"));
        } catch (Throwable t) {
            throw new RuntimeException("imageScaling failed", t);
        }
    }

    /** [imageView setImageFrameStyle:] — NSImageFrameStyle (0 = none). */
    public void setImageFrameStyle(long style) {
        try {
            hSetFrameStyle.invokeExact(peer, ObjC.sel("setImageFrameStyle:"), style);
        } catch (Throwable t) {
            throw new RuntimeException("setImageFrameStyle: failed", t);
        }
    }

    /** [imageView imageFrameStyle] — NSImageFrameStyle. */
    public long imageFrameStyle() {
        try {
            return (long) hFrameStyle.invokeExact(peer, ObjC.sel("imageFrameStyle"));
        } catch (Throwable t) {
            throw new RuntimeException("imageFrameStyle failed", t);
        }
    }
}
