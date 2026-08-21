package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSBox — an AppKit titled box/dividing container view. Thin, 1:1, stateless wrapper
/// over a native `NSBox`: every method maps to one `objc_msgSend` selector.
/// It is an `NSView`, so it can host subviews and be placed in any view hierarchy.
///
/// Only the title/type/border/titlePosition surface is wrapped here — enough for a
/// titled group box. `NSBox` inherits `setTitle:`/`title` from its own
/// title `NSCell`; the selectors used below are the real AppKit ones.
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

    /// `[[NSBox alloc] initWithFrame:frame]` — a new box at the given rect.
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

    /// [box setTitle:] — the box's title text.
    public void setTitle(String title) {
        try {
            hSetTitle.invokeExact(peer, ObjC.sel("setTitle:"), ObjC.nsstring(title));
        } catch (Throwable t) {
            throw new RuntimeException("setTitle: failed", t);
        }
    }

    /// [box title] — the box's current title.
    public String title() {
        try {
            return ObjC.toString((MemorySegment) hTitle.invokeExact(peer, ObjC.sel("title")));
        } catch (Throwable t) {
            throw new RuntimeException("title failed", t);
        }
    }

    // ---------------------------------------------------------------- nested enums — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSBox.h + NSCell.h
    //   NSBoxType: Primary 0, Separator 2, Custom 4
    //   NSTitlePosition: NoTitle 0, AboveTop 1, AtTop 2, BelowTop 3, AboveBottom 4, AtBottom 5, BelowBottom 6
    // Docs: https://developer.apple.com/documentation/appkit/nsboxtype

    /// `NSBoxType` — Primary 0, Separator 2, Custom 4. From `NSBox.h`.
    public enum BoxType {
        primary(0), separator(2), custom(4);
        public final long value;
        BoxType(long v) { this.value = v; }
        public static BoxType fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSTitlePosition` — NoTitle 0, AboveTop 1, AtTop 2, BelowTop 3, AboveBottom 4, AtBottom 5, BelowBottom 6. From `NSCell.h`.
    public enum TitlePosition {
        noTitle(0), aboveTop(1), atTop(2), belowTop(3), aboveBottom(4), atBottom(5), belowBottom(6);
        public final long value;
        TitlePosition(long v) { this.value = v; }
        public static TitlePosition fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// [box setBoxType:] — NSBoxType (0 = NSBoxPrimary).
    public void setBoxType(long type) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setBoxType:"), type);
        } catch (Throwable t) {
            throw new RuntimeException("setBoxType: failed", t);
        }
    }
    /// Typed overload.
    public void setBoxType(BoxType t) { setBoxType(t.value); }

    /// [box setBorderType:] — NSBorderType (0 = NSNoBorder).
    public void setBorderType(long type) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setBorderType:"), type);
        } catch (Throwable t) {
            throw new RuntimeException("setBorderType: failed", t);
        }
    }

    /// [box setTitlePosition:] — NSTitlePosition (0 = NSNoTitle).
    public void setTitlePosition(long position) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setTitlePosition:"), position);
        } catch (Throwable t) {
            throw new RuntimeException("setTitlePosition: failed", t);
        }
    }
    /// Typed overload.
    public void setTitlePosition(TitlePosition p) { setTitlePosition(p.value); }

    // ---------------------------------------------------------------- completeness

    /// [box boxType].
    public long boxType() {
        return ObjC.msgSendLong(peer, ObjC.sel("boxType"));
    }
    /// Typed getter.
    public BoxType boxTypeEnum() { return BoxType.fromValue(boxType()); }

    /// [box borderType].
    public long borderType() {
        return ObjC.msgSendLong(peer, ObjC.sel("borderType"));
    }

    /// [box titlePosition].
    public long titlePosition() {
        return ObjC.msgSendLong(peer, ObjC.sel("titlePosition"));
    }
    /// Typed getter.
    public TitlePosition titlePositionEnum() { return TitlePosition.fromValue(titlePosition()); }

    /// [box contentView] — the box's content view.
    public NSView contentView() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("contentView"));
        return NSView.wrap(v);
    }

    /// [box setContentView:].
    public void setContentView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setContentView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
    }

    /// [box contentViewMargins] — NSSize.
    public NSSize contentViewMargins() {
        try {
            MemorySegment s = (MemorySegment) hGetSize.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("contentViewMargins"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentViewMargins failed", t);
        }
    }

    /// [box setContentViewMargins:].
    public void setContentViewMargins(NSSize margins) {
        try {
            hSetSize.invokeExact(peer, ObjC.sel("setContentViewMargins:"), margins.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentViewMargins: failed", t);
        }
    }

    /// [box isTransparent].
    public boolean isTransparent() {
        return ObjC.msgSendBool(peer, ObjC.sel("isTransparent"));
    }

    /// [box setTransparent:].
    public void setTransparent(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setTransparent:"), flag);
    }

    /// [box titleFont] — NSFont.
    public NSFont titleFont() {
        MemorySegment f = ObjC.msgSendId(peer, ObjC.sel("titleFont"));
        return NSFont.wrap(f);
    }

    /// [box setTitleFont:].
    public void setTitleFont(NSFont font) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTitleFont:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
    }

    /// [box borderColor] — NSColor.
    public NSColor borderColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("borderColor"));
        return NSColor.wrap(c);
    }

    /// [box setBorderColor:].
    public void setBorderColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBorderColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// [box fillColor].
    public NSColor fillColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("fillColor"));
        return NSColor.wrap(c);
    }

    /// [box setFillColor:].
    public void setFillColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFillColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// [box borderWidth] — CGFloat.
    public double borderWidth() {
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("borderWidth"));
        } catch (Throwable t) {
            throw new RuntimeException("borderWidth failed", t);
        }
    }

    /// [box setBorderWidth:].
    public void setBorderWidth(double w) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setBorderWidth:"), w);
        } catch (Throwable t) {
            throw new RuntimeException("setBorderWidth: failed", t);
        }
    }

    /// [box cornerRadius] — CGFloat.
    public double cornerRadius() {
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("cornerRadius"));
        } catch (Throwable t) {
            throw new RuntimeException("cornerRadius failed", t);
        }
    }

    /// [box setCornerRadius:].
    public void setCornerRadius(double r) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setCornerRadius:"), r);
        } catch (Throwable t) {
            throw new RuntimeException("setCornerRadius: failed", t);
        }
    }

    /// [box sizeToFit].
    public void sizeToFit() {
        ObjC.msgSendVoid(peer, ObjC.sel("sizeToFit"));
    }
}
