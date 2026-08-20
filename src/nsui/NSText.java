package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSText — AppKit's abstract text view base (NSView -> NSText -> NSTextView).
/// Thin 1:1 wrapper over native `NSText`: every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer.
///
/// This is the shared surface for `NSTextView`; it exposes the
/// common text attributes so the hierarchy mirrors AppKit (NSText is an
/// NSView, NSTextView is an NSText).
public class NSText extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id

    protected NSText(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSText wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSText(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        initialized = true;
    }

    /// `[[NSText alloc] initWithFrame:frame]` — a new text object at the given rect.
    public static NSText create(NSRect frame) {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(ObjC.cls("NSText"), ObjC.sel("alloc"));
        try {
            v = (MemorySegment) hInitFrame.invokeExact(v, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSText", t);
        }
        if (v.address() == 0) {
            throw new IllegalStateException("NSText alloc/initWithFrame: returned nil");
        }
        return new NSText(v);
    }

    // ---------------------------------------------------------------- string

    /// [text string] — the plain string contents (NSString -> String).
    public String string() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("string")));
    }

    /// [text setString:] — replace the plain string contents.
    public void setString(String s) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setString:"), s == null ? MemorySegment.NULL : ObjC.nsstring(s));
    }

    // ---- rich text / graphics ----

    public boolean isRichText() {
        return ObjC.msgSendBool(peer, ObjC.sel("isRichText"));
    }

    public void setRichText(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setRichText:"), flag);
    }

    public boolean importsGraphics() {
        return ObjC.msgSendBool(peer, ObjC.sel("importsGraphics"));
    }

    public void setImportsGraphics(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setImportsGraphics:"), flag);
    }

    // ---- editable / selectable ----

    public boolean isEditable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEditable"));
    }

    public void setEditable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEditable:"), flag);
    }

    public boolean isSelectable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSelectable"));
    }

    public void setSelectable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setSelectable:"), flag);
    }

    // ---- font / colors ----

    public NSFont font() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("font"));
        return NSFont.wrap(p);
    }

    public void setFont(NSFont font) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFont:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
    }

    public NSColor textColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("textColor")));
    }

    public void setTextColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTextColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    public NSColor backgroundColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("backgroundColor")));
    }

    public void setBackgroundColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBackgroundColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    // ---- attributed string (typed) ----
    public NSAttributedString attributedString() {
        return NSAttributedString.wrap(ObjC.msgSendId(peer, ObjC.sel("attributedString")));
    }
    public void setAttributedString(NSAttributedString s) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAttributedString:"), (MemorySegment) (s == null ? MemorySegment.NULL : s.peer()));
    }
    public NSMutableAttributedString textStorageTyped() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        return NSMutableAttributedString.wrap(p);
    }

    // ---- additional completeness ----

    public boolean isFieldEditor() {
        return ObjC.msgSendBool(peer, ObjC.sel("isFieldEditor"));
    }

    public void setFieldEditor(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setFieldEditor:"), flag);
    }

    public long alignment() {
        return ObjC.msgSendLong(peer, ObjC.sel("alignment"));
    }

    public void setAlignment(long alignment) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setAlignment:"), alignment);
    }

    // ---- layout trio hooks (minimal) ----

    /// [text textStorage] as NSTextStorage (typed) — nil if no storage.
    public NSTextStorage textStorageAsStorage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        return NSTextStorage.wrap(p);
    }

    /// Replace the underlying text storage (via textStorage setAttributedString:).
    public void replaceTextStorage(NSTextStorage storage) {
        if (storage == null) return;
        MemorySegment ts = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        if (ts != null && ts.address() != 0) {
            ObjC.msgSendVoidId(ts, ObjC.sel("setAttributedString:"), storage.peer());
        } else {
            setAttributedString(storage);
        }
    }
}
