package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSColorWell — an AppKit color picker control ("well"). Thin, 1:1, stateless wrapper
 * over a native {@code NSColorWell}: every method maps to one {@code objc_msgSend}
 * selector. It is an {@link NSControl} (an {@link NSView}), so it fits any view hierarchy.
 */
public final class NSColorWell extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSetColor;    // (id, SEL, id) -> void    [setColor:]
    private static MethodHandle hColor;       // (id, SEL) -> id          [color]
    private static MethodHandle hVoid;        // (id, SEL) -> void        [deactivate]
    private static MethodHandle hVoidBool;  // (id, SEL, bool) -> void    [activate:(BOOL)exclusive]
    private static MethodHandle hResponds;  // (id, SEL, id) -> bool    [respondsToSelector:]

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
        initialized = true;
    }

    /** {@code [[NSColorWell alloc] initWithFrame:frame]} — a new color well at the given rect. */
    public static NSColorWell create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSColorWell"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSColorWell", t);
        }
        if (p.address() == 0) {
            throw new IllegalStateException("NSColorWell alloc/initWithFrame: returned nil");
        }
        return new NSColorWell(p);
    }

    // ---------------------------------------------------------------- instance API

    /** [well setColor:] — the well's current color (accepts a live NSColor). */
    public void setColor(NSColor color) {
        try {
            hSetColor.invokeExact(peer, ObjC.sel("setColor:"), color.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setColor: failed", t);
        }
    }

    /** [well color] — the current color as a typed NSColor (nil if none). */
    public NSColor color() {
        try {
            MemorySegment c = (MemorySegment) hColor.invokeExact(peer, ObjC.sel("color"));
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("color failed", t);
        }
    }

    /** [well color] raw id — the underlying NSColor id (for interop where a MemorySegment is needed). */
    public MemorySegment colorSegment() {
        try {
            return (MemorySegment) hColor.invokeExact(peer, ObjC.sel("color"));
        } catch (Throwable t) {
            throw new RuntimeException("color failed", t);
        }
    }

    /** [well isActive] — whether the well is in the active (editing) state. */
    public boolean isActive() {
        return ObjC.msgSendBool(peer, ObjC.sel("isActive"));
    }

    /** [well isBordered] — whether the well draws a border. */
    public boolean isBordered() {
        return ObjC.msgSendBool(peer, ObjC.sel("isBordered"));
    }

    /** [well setBordered:] — set whether the well draws a border. */
    public void setBordered(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBordered:"), flag);
    }

    /** [well supportsAlpha] — whether the well supports alpha (macOS 14+). Returns false on older runtimes. */
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

    /** [well setSupportsAlpha:] — set alpha support. No-op on <14 where selector is absent. */
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

    /** [well colorPanel] — the associated NSColorPanel (if any), as raw id. */
    public MemorySegment colorPanel() {
        return ObjC.msgSendId(peer, ObjC.sel("colorPanel"));
    }

    /**
     * [well activate:] — begin editing (open the color panel if the style needs it).
     * The selector is {@code activate:(BOOL)exclusive} — the BOOL is a real argument,
     * not optional (a zero-arg send would read a garbage register).
     */
    public void activate(boolean exclusive) {
        try {
            hVoidBool.invokeExact(peer, ObjC.sel("activate:"), exclusive);
        } catch (Throwable t) {
            throw new RuntimeException("activate: failed", t);
        }
    }

    /** [well deactivate] — end editing / dismiss the active state. */
    public void deactivate() {
        try {
            hVoid.invokeExact(peer, ObjC.sel("deactivate"));
        } catch (Throwable t) {
            throw new RuntimeException("deactivate failed", t);
        }
    }

    /** [well takeColorFrom:] — take color from sender (sends color message to sender). */
    public void takeColorFrom(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("takeColorFrom:"), sender);
    }

    /** [well setColorWellStyle:] — NSColorWellStyle (0=Default,1=Minimal,2=Expanded) macOS 13+. */
    public void setColorWellStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setColorWellStyle:"), style);
    }

    /** [well colorWellStyle] — current style. */
    public long colorWellStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("colorWellStyle"));
    }
}
