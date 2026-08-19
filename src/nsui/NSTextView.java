package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSTextView — a rich-text view (NSView -> NSText -> NSTextView).
 * Thin 1:1 wrapper over native {@code NSTextView}: every method maps to
 * one {@code objc_msgSend} selector, no cached Java state beyond the peer.
 * Mirrors the native hierarchy so {@code isKindOfClass:} works for
 * NSTextView / NSText / NSView.
 *
 * <p>MVP: wraps the concrete AppKit class {@code NSTextView} directly via
 * {@code alloc/initWithFrame:}. Lazy {@code ensureInit} + {@code ObjC.handle}
 * follows the existing NSView/Control pattern (resolve-once, invokeExact).
 */
public class NSTextView extends NSText {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id

    private NSTextView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTextView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTextView(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        initialized = true;
    }

    /** {@code [[NSTextView alloc] initWithFrame:frame]} — a new text view at the given rect. */
    public static NSTextView create(NSRect frame) {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(ObjC.cls("NSTextView"), ObjC.sel("alloc"));
        try {
            v = (MemorySegment) hInitFrame.invokeExact(v, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSTextView", t);
        }
        if (v.address() == 0) {
            throw new IllegalStateException("NSTextView alloc/initWithFrame: returned nil");
        }
        return new NSTextView(v);
    }

    // ---------------------------------------------------------------- string (re-expose for discoverability)

    @Override
    public String string() { return super.string(); }

    @Override
    public void setString(String s) { super.setString(s); }

    // ---- rich text / graphics (inherited from NSText, re-expose) ----

    @Override
    public boolean isRichText() { return super.isRichText(); }

    @Override
    public void setRichText(boolean flag) { super.setRichText(flag); }

    @Override
    public boolean importsGraphics() { return super.importsGraphics(); }

    @Override
    public void setImportsGraphics(boolean flag) { super.setImportsGraphics(flag); }

    // ---- NSTextView-specific ----

    /** [textView usesFontPanel] — whether the font panel is used. */
    public boolean usesFontPanel() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesFontPanel"));
    }

    /** [textView setUsesFontPanel:] — enable/disable the font panel. */
    public void setUsesFontPanel(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesFontPanel:"), flag);
    }

    // ---- editable / selectable (inherited, re-expose) ----

    @Override
    public boolean isEditable() { return super.isEditable(); }

    @Override
    public void setEditable(boolean flag) { super.setEditable(flag); }

    @Override
    public boolean isSelectable() { return super.isSelectable(); }

    @Override
    public void setSelectable(boolean flag) { super.setSelectable(flag); }

    // ---- font / colors (inherited, re-expose) ----

    @Override
    public NSFont font() { return super.font(); }

    @Override
    public void setFont(NSFont font) { super.setFont(font); }

    @Override
    public NSColor textColor() { return super.textColor(); }

    @Override
    public void setTextColor(NSColor color) { super.setTextColor(color); }

    @Override
    public NSColor backgroundColor() { return super.backgroundColor(); }

    @Override
    public void setBackgroundColor(NSColor color) { super.setBackgroundColor(color); }
}
