package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSBox — an AppKit titled box/dividing container view. Thin, 1:1, stateless wrapper
 * over a native {@code NSBox}: every method maps to one {@code objc_msgSend} selector.
 * It is an {@link NSView}, so it can host subviews and be placed in any view hierarchy.
 *
 * <p>Only the title/type/border/titlePosition surface is wrapped here — enough for a
 * titled group box. {@code NSBox} inherits {@code setTitle:}/{@code title} from its own
 * title {@code NSCell}; the selectors used below are the real AppKit ones.
 */
public final class NSBox extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSetTitle;    // (id, SEL, id) -> void    [setTitle:]
    private static MethodHandle hTitle;       // (id, SEL) -> id          [title]
    private static MethodHandle hSetInt;      // (id, SEL, int) -> void   [setBoxType: / setBorderType: / setTitlePosition:]
    private static MethodHandle hGetDouble;   // (id, SEL) -> double
    private static MethodHandle hSetDouble;   // (id, SEL, double) -> void
    private static MethodHandle hGetSize;     // (id, SEL) -> NSSize
    private static MethodHandle hSetSize;     // (id, SEL, NSSize) -> void

    private NSBox(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSetTitle = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hTitle = ObjC.handle(Sig.of(Ret.ID));
        hSetInt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hGetDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hGetSize = ObjC.handle(Sig.of(Ret.SIZE));
        hSetSize = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
        initialized = true;
    }

    /** {@code [[NSBox alloc] initWithFrame:frame]} — a new box at the given rect. */
    public static NSBox create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSBox"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSBox", t);
        }
        if (p.address() == 0) {
            throw new IllegalStateException("NSBox alloc/initWithFrame: returned nil");
        }
        return new NSBox(p);
    }

    // ---------------------------------------------------------------- instance API

    /** [box setTitle:] — the box's title text. */
    public void setTitle(String title) {
        try {
            hSetTitle.invokeExact(peer, ObjC.sel("setTitle:"), ObjC.nsstring(title));
        } catch (Throwable t) {
            throw new RuntimeException("setTitle: failed", t);
        }
    }

    /** [box title] — the box's current title. */
    public String title() {
        try {
            return ObjC.toString((MemorySegment) hTitle.invokeExact(peer, ObjC.sel("title")));
        } catch (Throwable t) {
            throw new RuntimeException("title failed", t);
        }
    }

    /** [box setBoxType:] — NSBoxType (0 = NSBoxPrimary). */
    public void setBoxType(long type) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setBoxType:"), type);
        } catch (Throwable t) {
            throw new RuntimeException("setBoxType: failed", t);
        }
    }

    /** [box setBorderType:] — NSBorderType (0 = NSNoBorder). */
    public void setBorderType(long type) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setBorderType:"), type);
        } catch (Throwable t) {
            throw new RuntimeException("setBorderType: failed", t);
        }
    }

    /** [box setTitlePosition:] — NSTitlePosition (0 = NSNoTitle). */
    public void setTitlePosition(long position) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setTitlePosition:"), position);
        } catch (Throwable t) {
            throw new RuntimeException("setTitlePosition: failed", t);
        }
    }

    // ---------------------------------------------------------------- completeness

    /** [box boxType]. */
    public long boxType() {
        return ObjC.msgSendLong(peer, ObjC.sel("boxType"));
    }

    /** [box borderType]. */
    public long borderType() {
        return ObjC.msgSendLong(peer, ObjC.sel("borderType"));
    }

    /** [box titlePosition]. */
    public long titlePosition() {
        return ObjC.msgSendLong(peer, ObjC.sel("titlePosition"));
    }

    /** [box contentView] — the box's content view. */
    public NSView contentView() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("contentView"));
        return NSView.wrap(v);
    }

    /** [box setContentView:]. */
    public void setContentView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setContentView:"), view == null ? MemorySegment.NULL : view.peer());
    }

    /** [box contentViewMargins] — NSSize. */
    public NSSize contentViewMargins() {
        try {
            MemorySegment s = (MemorySegment) hGetSize.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("contentViewMargins"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentViewMargins failed", t);
        }
    }

    /** [box setContentViewMargins:]. */
    public void setContentViewMargins(NSSize margins) {
        try {
            hSetSize.invokeExact(peer, ObjC.sel("setContentViewMargins:"), margins.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentViewMargins: failed", t);
        }
    }

    /** [box isTransparent]. */
    public boolean isTransparent() {
        return ObjC.msgSendBool(peer, ObjC.sel("isTransparent"));
    }

    /** [box setTransparent:]. */
    public void setTransparent(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setTransparent:"), flag);
    }

    /** [box titleFont] — NSFont. */
    public NSFont titleFont() {
        MemorySegment f = ObjC.msgSendId(peer, ObjC.sel("titleFont"));
        if (f == null || f.address() == 0) return null;
        try {
            var ctor = NSFont.class.getDeclaredConstructor(MemorySegment.class);
            ctor.setAccessible(true);
            return ctor.newInstance(f);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("wrap NSFont failed", e);
        }
    }

    /** [box setTitleFont:]. */
    public void setTitleFont(NSFont font) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTitleFont:"), font == null ? MemorySegment.NULL : font.peer());
    }

    /** [box borderColor] — NSColor. */
    public NSColor borderColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("borderColor"));
        if (c == null || c.address() == 0) return null;
        try {
            var ctor = NSColor.class.getDeclaredConstructor(MemorySegment.class);
            ctor.setAccessible(true);
            return ctor.newInstance(c);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("wrap NSColor failed", e);
        }
    }

    /** [box setBorderColor:]. */
    public void setBorderColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBorderColor:"), color == null ? MemorySegment.NULL : color.peer());
    }

    /** [box fillColor]. */
    public NSColor fillColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("fillColor"));
        if (c == null || c.address() == 0) return null;
        try {
            var ctor = NSColor.class.getDeclaredConstructor(MemorySegment.class);
            ctor.setAccessible(true);
            return ctor.newInstance(c);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("wrap NSColor failed", e);
        }
    }

    /** [box setFillColor:]. */
    public void setFillColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFillColor:"), color == null ? MemorySegment.NULL : color.peer());
    }

    /** [box borderWidth] — CGFloat. */
    public double borderWidth() {
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("borderWidth"));
        } catch (Throwable t) {
            throw new RuntimeException("borderWidth failed", t);
        }
    }

    /** [box setBorderWidth:]. */
    public void setBorderWidth(double w) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setBorderWidth:"), w);
        } catch (Throwable t) {
            throw new RuntimeException("setBorderWidth: failed", t);
        }
    }

    /** [box cornerRadius] — CGFloat. */
    public double cornerRadius() {
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("cornerRadius"));
        } catch (Throwable t) {
            throw new RuntimeException("cornerRadius failed", t);
        }
    }

    /** [box setCornerRadius:]. */
    public void setCornerRadius(double r) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setCornerRadius:"), r);
        } catch (Throwable t) {
            throw new RuntimeException("setCornerRadius: failed", t);
        }
    }

    /** [box sizeToFit]. */
    public void sizeToFit() {
        ObjC.msgSendVoid(peer, ObjC.sel("sizeToFit"));
    }
}
