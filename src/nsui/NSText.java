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

    private static void ensureInit() {
        if (initialized) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        initialized = true;
    }

    /// `[[NSText alloc] initWithFrame:frame]` — a new text object at the given rect.
        public static NSText create(NSRect frame) {
        ensureInit();
        return new NSText(ObjC.newView("NSText", frame));
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

    /// [text replaceCharactersInRange:withString:]
    public void replaceCharactersInRangeWithString(NSRange range, String string) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceCharactersInRange:withString:"),
                    range.toSegment(), (MemorySegment) (string == null ? MemorySegment.NULL : ObjC.nsstring(string)));
        } catch (Throwable t) {
            throw new RuntimeException("replaceCharactersInRange:withString: failed", t);
        }
    }

    /// [text replaceCharactersInRange:withRTF:] — rtf is NSData* (raw segment).
    public void replaceCharactersInRangeWithRTF(NSRange range, MemorySegment rtfData) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceCharactersInRange:withRTF:"),
                    range.toSegment(), (MemorySegment) (rtfData == null ? MemorySegment.NULL : rtfData));
        } catch (Throwable t) {
            throw new RuntimeException("replaceCharactersInRange:withRTF: failed", t);
        }
    }

    /// [text replaceCharactersInRange:withRTFD:] — rtfd is NSData* (raw segment).
    public void replaceCharactersInRangeWithRTFD(NSRange range, MemorySegment rtfdData) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceCharactersInRange:withRTFD:"),
                    range.toSegment(), (MemorySegment) (rtfdData == null ? MemorySegment.NULL : rtfdData));
        } catch (Throwable t) {
            throw new RuntimeException("replaceCharactersInRange:withRTFD: failed", t);
        }
    }

    /// [text RTFFromRange:] -> NSData* (raw segment, may be nil).
    public MemorySegment rtfFromRange(NSRange range) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("RTFFromRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("RTFFromRange: failed", t);
        }
    }

    /// [text RTFDFromRange:] -> NSData* (raw segment, may be nil).
    public MemorySegment rtfdFromRange(NSRange range) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("RTFDFromRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("RTFDFromRange: failed", t);
        }
    }

    /// [text writeRTFDToFile:atomically:]
    public boolean writeRTFDToFileAtomically(String path, boolean atomically) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.BOOL));
            return (boolean) h.invokeExact(peer, ObjC.sel("writeRTFDToFile:atomically:"),
                    ObjC.nsstring(path == null ? "" : path), atomically);
        } catch (Throwable t) {
            throw new RuntimeException("writeRTFDToFile:atomically: failed", t);
        }
    }

    /// [text readRTFDFromFile:]
    public boolean readRTFDFromFile(String path) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("readRTFDFromFile:"),
                    ObjC.nsstring(path == null ? "" : path));
        } catch (Throwable t) {
            throw new RuntimeException("readRTFDFromFile: failed", t);
        }
    }

    // ---- delegate (raw id; the NSTextDelegate protocol needs upcall machinery — skipped, reported) ----

    /// [text delegate] — raw id (may be nil).
    public MemorySegment delegateSegment() {
        return ObjC.msgSendId(peer, ObjC.sel("delegate"));
    }

    /// [text setDelegate:] — raw id.
    public void setDelegate(MemorySegment delegate) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"),
                (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate));
    }

    // ---- panel / background ----

    /// [text usesFontPanel]
    public boolean usesFontPanel() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesFontPanel"));
    }

    /// [text setUsesFontPanel:]
    public void setUsesFontPanel(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesFontPanel:"), flag);
    }

    /// [text drawsBackground]
    public boolean drawsBackground() {
        return ObjC.msgSendBool(peer, ObjC.sel("drawsBackground"));
    }

    /// [text setDrawsBackground:]
    public void setDrawsBackground(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDrawsBackground:"), flag);
    }

    /// [text isRulerVisible]
    public boolean isRulerVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isRulerVisible"));
    }

    // ---- selection ----

    /// [text selectedRange] -> NSRange.
    public NSRange selectedRange() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("selectedRange"));
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("selectedRange failed", t);
        }
    }

    /// [text setSelectedRange:]
    public void setSelectedRange(NSRange range) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("setSelectedRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setSelectedRange: failed", t);
        }
    }

    /// [text scrollRangeToVisible:]
    public void scrollRangeToVisible(NSRange range) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("scrollRangeToVisible:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("scrollRangeToVisible: failed", t);
        }
    }

    // ---- direction ----

    /// [text baseWritingDirection] -> NSWritingDirection (long).
    public long baseWritingDirection() {
        return ObjC.msgSendLong(peer, ObjC.sel("baseWritingDirection"));
    }

    /// [text setBaseWritingDirection:]
    public void setBaseWritingDirection(long direction) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setBaseWritingDirection:"), direction);
    }

    // ---- ranged font / color ----

    /// [text setTextColor:range:] — nil color removes NSForegroundColorAttributeName.
    public void setTextColorRange(NSColor color, NSRange range) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("setTextColor:range:"),
                    (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setTextColor:range: failed", t);
        }
    }

    /// [text setFont:range:]
    public void setFontRange(NSFont font, NSRange range) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("setFont:range:"),
                    (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setFont:range: failed", t);
        }
    }

    // ---- size constraints ----

    /// [text maxSize] -> NSSize.
    public NSSize maxSize() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.SIZE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("maxSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("maxSize failed", t);
        }
    }

    /// [text setMaxSize:]
    public void setMaxSize(NSSize size) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
            h.invokeExact(peer, ObjC.sel("setMaxSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMaxSize: failed", t);
        }
    }

    /// [text minSize] -> NSSize.
    public NSSize minSize() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.SIZE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("minSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("minSize failed", t);
        }
    }

    /// [text setMinSize:]
    public void setMinSize(NSSize size) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
            h.invokeExact(peer, ObjC.sel("setMinSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMinSize: failed", t);
        }
    }

    /// [text isHorizontallyResizable]
    public boolean isHorizontallyResizable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isHorizontallyResizable"));
    }

    /// [text setHorizontallyResizable:]
    public void setHorizontallyResizable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setHorizontallyResizable:"), flag);
    }

    /// [text isVerticallyResizable]
    public boolean isVerticallyResizable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVerticallyResizable"));
    }

    /// [text setVerticallyResizable:]
    public void setVerticallyResizable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setVerticallyResizable:"), flag);
    }

    /// [text sizeToFit] — resize to fit the contents.
    public void sizeToFit() {
        ObjC.msgSendVoid(peer, ObjC.sel("sizeToFit"));
    }

    // ---- responder actions (all take a nullable sender id) ----

    /// [text copy:]
    public void copy(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("copy:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text copyFont:]
    public void copyFont(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("copyFont:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text copyRuler:]
    public void copyRuler(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("copyRuler:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text cut:]
    public void cut(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("cut:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text delete:] (`delete` is a Java keyword; named deleteText).
    public void deleteText(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("delete:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text paste:]
    public void paste(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("paste:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text pasteFont:]
    public void pasteFont(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("pasteFont:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text pasteRuler:]
    public void pasteRuler(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("pasteRuler:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text selectAll:]
    public void selectAll(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("selectAll:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text changeFont:]
    public void changeFont(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("changeFont:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text alignLeft:]
    public void alignLeft(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("alignLeft:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text alignRight:]
    public void alignRight(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("alignRight:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text alignCenter:]
    public void alignCenter(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("alignCenter:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text subscript:]
    public void subscript(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("subscript:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text superscript:]
    public void superscript(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("superscript:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text underline:]
    public void underline(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("underline:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text unscript:]
    public void unscript(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("unscript:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text showGuessPanel:]
    public void showGuessPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("showGuessPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text checkSpelling:]
    public void checkSpelling(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("checkSpelling:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [text toggleRuler:]
    public void toggleRuler(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleRuler:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
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
