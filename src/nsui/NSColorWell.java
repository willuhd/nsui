package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSColorWell — an AppKit color picker control ("well"). Thin, 1:1, stateless wrapper
/// over a native `NSColorWell`: every method maps to one `objc_msgSend`
/// selector. It is an `NSControl` (an `NSView`), so it fits any view hierarchy.
///
/// Nothing in `NSColorWell.h` is omitted: every non-deprecated, non-block,
/// non-delegate member is expressible in registered shapes (`isBordered` /
/// `setBordered:` are kept although deprecated — they predate this pass).
///
/// Note: `colorPanel` is not an `NSColorWell` selector (absent from the header
/// and rejected at runtime with `unrecognized selector`), so no accessor for it
/// is provided.
public final class NSColorWell extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSetColor;    // (id, SEL, id) -> void    [setColor:]
    private static MethodHandle hColor;       // (id, SEL) -> id          [color]
    private static MethodHandle hVoid;        // (id, SEL) -> void        [deactivate]
    private static MethodHandle hVoidBool;  // (id, SEL, bool) -> void    [activate:(BOOL)exclusive]
    private static MethodHandle hResponds;  // (id, SEL, id) -> bool    [respondsToSelector:]
    private static MethodHandle hWithStyle; // (id, SEL, long) -> id    [colorWellWithStyle:]
    private static MethodHandle hDrawWell;  // (id, SEL, NSRect) -> void [drawWellInside:]
    private static MethodHandle hDouble;    // (id, SEL) -> double       [maximumLinearExposure]
    private static MethodHandle hSetDouble; // (id, SEL, double) -> void [setMaximumLinearExposure:]

    private NSColorWell(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSetColor = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hColor = ObjC.handle(Sig.of(Ret.ID));
        hVoid = ObjC.handle(Sig.of(Ret.VOID));
        hVoidBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hResponds = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        hWithStyle = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hDrawWell = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
        hDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        initialized = true;
    }

    /// `[[NSColorWell alloc] initWithFrame:frame]` — a new color well at the given rect.
        public static NSColorWell create(NSRect frame) {
        ensureInit();
        return new NSColorWell(ObjC.newView("NSColorWell", frame));
    }

    // ---------------------------------------------------------------- instance API

    /// [well setColor:] — the well's current color (accepts a live NSColor).
    public void setColor(NSColor color) {
        try {
            hSetColor.invokeExact(peer, ObjC.sel("setColor:"), color.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setColor: failed", t);
        }
    }

    /// [well color] — the current color as a typed NSColor (nil if none).
    public NSColor color() {
        try {
            MemorySegment c = (MemorySegment) hColor.invokeExact(peer, ObjC.sel("color"));
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("color failed", t);
        }
    }

    /// [well color] raw id — the underlying NSColor id (for interop where a MemorySegment is needed).
    public MemorySegment colorSegment() {
        try {
            return (MemorySegment) hColor.invokeExact(peer, ObjC.sel("color"));
        } catch (Throwable t) {
            throw new RuntimeException("color failed", t);
        }
    }

    /// [well isActive] — whether the well is in the active (editing) state.
    public boolean isActive() {
        return ObjC.msgSendBool(peer, ObjC.sel("isActive"));
    }

    /// [well isBordered] — whether the well draws a border.
    public boolean isBordered() {
        return ObjC.msgSendBool(peer, ObjC.sel("isBordered"));
    }

    /// [well setBordered:] — set whether the well draws a border.
    public void setBordered(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBordered:"), flag);
    }

    /// [well supportsAlpha] — whether the well supports alpha (macOS 14+). Returns false on older runtimes.
    public boolean supportsAlpha() {
        ensureInit();
        try {
            boolean responds = (boolean) hResponds.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("supportsAlpha"));
            if (!responds) return false;
            return ObjC.msgSendBool(peer, ObjC.sel("supportsAlpha"));
        } catch (Throwable t) {
            return false;
        }
    }

    /// [well setSupportsAlpha:] — set alpha support. No-op on <14 where selector is absent.
    public void setSupportsAlpha(boolean flag) {
        ensureInit();
        try {
            boolean responds = (boolean) hResponds.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("setSupportsAlpha:"));
            if (!responds) return;
            ObjC.msgSendVoidBool(peer, ObjC.sel("setSupportsAlpha:"), flag);
        } catch (Throwable t) {
            // swallow on older runtimes
        }
    }

    /// [well activate:] — begin editing (open the color panel if the style needs it).
    /// The selector is `activate:(BOOL)exclusive` — the BOOL is a real argument,
    /// not optional (a zero-arg send would read a garbage register).
    public void activate(boolean exclusive) {
        try {
            hVoidBool.invokeExact(peer, ObjC.sel("activate:"), exclusive);
        } catch (Throwable t) {
            throw new RuntimeException("activate: failed", t);
        }
    }

    /// [well deactivate] — end editing / dismiss the active state.
    public void deactivate() {
        try {
            hVoid.invokeExact(peer, ObjC.sel("deactivate"));
        } catch (Throwable t) {
            throw new RuntimeException("deactivate failed", t);
        }
    }

    /// [well takeColorFrom:] — take color from sender (sends color message to sender).
    public void takeColorFrom(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("takeColorFrom:"), sender);
    }

    /// [well setColorWellStyle:] — NSColorWellStyle (0=Default,1=Minimal,2=Expanded) macOS 13+.
    public void setColorWellStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setColorWellStyle:"), style);
    }

    /// [well colorWellStyle] — current style.
    public long colorWellStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("colorWellStyle"));
    }

    /// `+colorWellWithStyle:` — a well in the given NSColorWellStyle (0=Default,1=Minimal,2=Expanded).
    public static NSColorWell colorWellWithStyle(long style) {
        ensureInit();
        try {
            MemorySegment w = (MemorySegment) hWithStyle.invokeExact(ObjC.cls("NSColorWell"), ObjC.sel("colorWellWithStyle:"), style);
            if (w == null || w.address() == 0) throw new IllegalStateException("colorWellWithStyle: returned nil");
            return new NSColorWell(w);
        } catch (Throwable t) {
            throw new RuntimeException("colorWellWithStyle: failed", t);
        }
    }

    /// [well drawWellInside:] — draw the well swatch inside the rect.
    public void drawWellInside(NSRect insideRect) {
        ensureInit();
        try {
            hDrawWell.invokeExact(peer, ObjC.sel("drawWellInside:"), insideRect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("drawWellInside: failed", t);
        }
    }

    /// [well image] — image on the expanded-style button portion (nil-safe; nil unless Expanded).
    public NSImage image() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("image"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }

    /// [well setImage:] — nil-safe.
    public void setImage(NSImage image) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setImage:"),
                (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()));
    }

    /// [well pulldownTarget] — target for the pulldown action (raw id; nil becomes NULL).
    public MemorySegment pulldownTarget() {
        return ObjC.msgSendId(peer, ObjC.sel("pulldownTarget"));
    }

    /// [well setPulldownTarget:] — nil-safe.
    public void setPulldownTarget(MemorySegment target) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPulldownTarget:"),
                (MemorySegment) (target == null ? MemorySegment.NULL : target));
    }

    /// [well pulldownAction] — the pulldown selector (SEL as id; NULL when unset).
    public MemorySegment pulldownAction() {
        return ObjC.msgSendId(peer, ObjC.sel("pulldownAction"));
    }

    /// [well setPulldownAction:] — nil/empty clears.
    public void setPulldownAction(String actionSelector) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPulldownAction:"),
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
    }

    /// [well maximumLinearExposure] — macOS 26+; 1.0 when the selector is absent.
    public double maximumLinearExposure() {
        ensureInit();
        try {
            boolean responds = (boolean) hResponds.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("maximumLinearExposure"));
            if (!responds) return 1.0;
            return (double) hDouble.invokeExact(peer, ObjC.sel("maximumLinearExposure"));
        } catch (Throwable t) {
            throw new RuntimeException("maximumLinearExposure failed", t);
        }
    }

    /// [well setMaximumLinearExposure:] — macOS 26+; no-op when absent (values < 1 ignored).
    public void setMaximumLinearExposure(double exposure) {
        ensureInit();
        try {
            boolean responds = (boolean) hResponds.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("setMaximumLinearExposure:"));
            if (!responds) return;
            hSetDouble.invokeExact(peer, ObjC.sel("setMaximumLinearExposure:"), exposure);
        } catch (Throwable t) {
            throw new RuntimeException("setMaximumLinearExposure: failed", t);
        }
    }

}
