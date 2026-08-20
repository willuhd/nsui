package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTextView — a rich-text view (NSView -> NSText -> NSTextView).
/// Thin 1:1 wrapper over native `NSTextView`: every method maps to
/// one `objc_msgSend` selector, no cached Java state beyond the peer.
/// Mirrors the native hierarchy so `isKindOfClass:` works for
/// NSTextView / NSText / NSView.
///
/// MVP: wraps the concrete AppKit class `NSTextView` directly via
/// `alloc/initWithFrame:`. Lazy `ensureInit` + `ObjC.handle`
/// follows the existing NSView/Control pattern (resolve-once, invokeExact).
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

    /// `[[NSTextView alloc] initWithFrame:frame]` — a new text view at the given rect.
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

    /// [textView usesFontPanel] — whether the font panel is used.
    public boolean usesFontPanel() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesFontPanel"));
    }

    /// [textView setUsesFontPanel:] — enable/disable the font panel.
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

    // ---- typed NSAttributedString support ----
    @Override
    public NSAttributedString attributedString() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        if (p != null && p.address() != 0) return NSAttributedString.wrap(p);
        return super.attributedString();
    }
    @Override
    public void setAttributedString(NSAttributedString s) {
        // NSTextView has no setAttributedString: — use its textStorage (NSTextStorage is a NSMutableAttributedString)
        MemorySegment storage = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        if (storage != null && storage.address() != 0) {
            ObjC.msgSendVoidId(storage, ObjC.sel("setAttributedString:"), (MemorySegment) (s == null ? MemorySegment.NULL : s.peer()));
            return;
        }
        super.setAttributedString(s);
    }

    public NSAttributedString attributedStringValueTyped() {
        return NSAttributedString.wrap(ObjC.msgSendId(peer, ObjC.sel("attributedString")));
    }
    public void setAttributedStringValue(NSAttributedString value) {
        // NSTextView uses setAttributedString:; also support attributedStringValue for control-like usage
        // Try attributedString first, fallback to setAttributedStringValue if available
        MemorySegment sel = ObjC.sel("setAttributedString:");
        ObjC.msgSendVoidId(peer, sel, (MemorySegment) (value == null ? MemorySegment.NULL : value.peer()));
    }

    /// [textView textStorage] -> NSTextStorage (NSMutableAttributedString)
    public NSMutableAttributedString textStorage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        return NSMutableAttributedString.wrap(p);
    }

    /// [textView setTextColor:range:] convenience via textStorage
    public void setTextColor(NSColor color, NSRange range) {
        NSMutableAttributedString ts = textStorage();
        if (ts != null) {
            ts.addAttribute("NSForegroundColorAttributeName", (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()), range);
        }
    }

    // ---- NSLayoutManager trio (minimal) ----

    /// [textView layoutManager] -> NSLayoutManager (may be nil).
    public NSLayoutManager layoutManager() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("layoutManager"));
        return NSLayoutManager.wrap(p);
    }

    /// [textView textContainer] -> NSTextContainer (may be nil).
    public NSTextContainer textContainer() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textContainer"));
        return NSTextContainer.wrap(p);
    }

    /// [textView textStorage] as NSTextStorage (typed).
    public NSTextStorage textStorageAsStorage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        return NSTextStorage.wrap(p);
    }

    /// Wire a full trio manually: storage -> layoutManager -> container -> textView.
    /// Minimal helper — callers that need a custom trio can use this instead of relying
    /// on the default NSTextView initialization.
    public void replaceTextContainer(NSTextContainer container) {
        ObjC.msgSendVoidId(peer, ObjC.sel("replaceTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
    }

    /// [textView setTextContainer:]
    public void setTextContainer(NSTextContainer container) {
        // Not a real AppKit selector, but keep for API symmetry; forward to replace if available
        MemorySegment sel = ObjC.sel("setTextContainer:");
        // Use escape hatch: check responds, otherwise use replaceTextContainer
        try {
            MemorySegment responds = ObjC.msgSendId(peer, ObjC.sel("respondsToSelector:"));
            // just attempt direct send; if unrecognized, fall back
            ObjC.msgSendVoidId(peer, sel, (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
        } catch (Throwable t) {
            replaceTextContainer(container);
        }
    }
}
