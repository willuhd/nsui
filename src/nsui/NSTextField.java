package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTextField — an AppKit single/multi-line text field control. Thin 1:1 wrapper
/// over a native `NSTextField`: every method maps to one `objc_msgSend`
/// selector, no cached Java state beyond the peer. Mirrors the native hierarchy:
/// NSTextField is an NSControl is an NSView.
public class NSTextField extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hDouble;      // (id, SEL) -> double
    private static MethodHandle hSetDouble;   // (id, SEL, double) -> void

    protected NSTextField(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        initialized = true;
    }

    /// `[[NSTextField alloc] initWithFrame:frame]` — a new text field at the given rect.
    public static NSTextField create(NSRect frame) {
        ensureInit();
        MemorySegment f = ObjC.msgSendId(ObjC.cls("NSTextField"), ObjC.sel("alloc"));
        try {
            f = (MemorySegment) hInitFrame.invokeExact(f, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSTextField", t);
        }
        if (f.address() == 0) {
            throw new IllegalStateException("NSTextField alloc/initWithFrame: returned nil");
        }
        return new NSTextField(f);
    }

    // factory helpers mirroring NSControl.h convenience constructors
    public static NSTextField labelWithString(String s) {
        ensureInit();
        MemorySegment p = ObjC.msgSendIdId(ObjC.cls("NSTextField"), ObjC.sel("labelWithString:"), ObjC.nsstring(s));
        return new NSTextField(p);
    }
    public static NSTextField wrappingLabelWithString(String s) {
        MemorySegment p = ObjC.msgSendIdId(ObjC.cls("NSTextField"), ObjC.sel("wrappingLabelWithString:"), ObjC.nsstring(s));
        return new NSTextField(p);
    }
    public static NSTextField textFieldWithString(String s) {
        MemorySegment p = ObjC.msgSendIdId(ObjC.cls("NSTextField"), ObjC.sel("textFieldWithString:"), ObjC.nsstring(s));
        return new NSTextField(p);
    }

    // ---------------------------------------------------------------- instance API

    // ---- stringValue already in NSControl, re-expose for discoverability ----
    @Override
    public String stringValue() { return super.stringValue(); }
    @Override
    public void setStringValue(String value) { super.setStringValue(value); }

    /// [field setFont:] — the font used to render the text.
    @Override
    public void setFont(NSFont font) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFont:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
    }

    /// [field setTextColor:] — the color of the text.
    public void setTextColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTextColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }
    public NSColor textColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("textColor")));
    }

    // ---- placeholder ----
    public String placeholderString() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("placeholderString")));
    }
    public void setPlaceholderString(String s) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPlaceholderString:"), s == null ? MemorySegment.NULL : ObjC.nsstring(s));
    }
    public MemorySegment placeholderAttributedString() {
        return ObjC.msgSendId(peer, ObjC.sel("placeholderAttributedString"));
    }
    public void setPlaceholderAttributedString(MemorySegment attr) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPlaceholderAttributedString:"), (MemorySegment) (attr == null ? MemorySegment.NULL : attr));
    }
    // typed variants
    public NSAttributedString placeholderAttributedStringTyped() {
        return NSAttributedString.wrap(ObjC.msgSendId(peer, ObjC.sel("placeholderAttributedString")));
    }
    public void setPlaceholderAttributedString(NSAttributedString attr) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPlaceholderAttributedString:"), (MemorySegment) (attr == null ? MemorySegment.NULL : attr.peer()));
    }

    // ---- typed NSAttributedStringValue (delegates to NSControl) ----
    public NSAttributedString attributedStringValueTyped() {
        return NSAttributedString.wrap(ObjC.msgSendId(peer, ObjC.sel("attributedStringValue")));
    }
    public void setAttributedStringValue(NSAttributedString value) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAttributedStringValue:"), (MemorySegment) (value == null ? MemorySegment.NULL : value.peer()));
    }

    // ---- backgroundColor ----
    public NSColor backgroundColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("backgroundColor")));
    }
    public void setBackgroundColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBackgroundColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    // ---- bezeled / bordered / drawsBackground / editable / selectable (getters + setters) ----
    public boolean isBezeled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isBezeled"));
    }
    /// [field setBezeled:] — draw the field's rounded bezel border.
    public void setBezeled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBezeled:"), flag);
    }
    public boolean isBordered() {
        return ObjC.msgSendBool(peer, ObjC.sel("isBordered"));
    }
    public void setBordered(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBordered:"), flag);
    }
    public boolean drawsBackground() {
        return ObjC.msgSendBool(peer, ObjC.sel("drawsBackground"));
    }
    /// [field setDrawsBackground:] — whether the field fills its background.
    public void setDrawsBackground(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDrawsBackground:"), flag);
    }
    public boolean isEditable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEditable"));
    }
    /// [field setEditable:] — whether the field accepts text editing.
    public void setEditable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEditable:"), flag);
    }
    public boolean isSelectable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSelectable"));
    }
    /// [field setSelectable:] — whether the field's text can be selected.
    public void setSelectable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setSelectable:"), flag);
    }

    // ---- bezelStyle / preferredMaxLayoutWidth / maximumNumberOfLines ----
    public long bezelStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("bezelStyle"));
    }
    public void setBezelStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setBezelStyle:"), style);
    }
    public double preferredMaxLayoutWidth() {
        ensureInit();
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("preferredMaxLayoutWidth")); } catch (Throwable t) { throw new RuntimeException("preferredMaxLayoutWidth failed", t); }
    }
    public void setPreferredMaxLayoutWidth(double w) {
        ensureInit();
        try { hSetDouble.invokeExact(peer, ObjC.sel("setPreferredMaxLayoutWidth:"), w); } catch (Throwable t) { throw new RuntimeException("setPreferredMaxLayoutWidth: failed", t); }
    }
    public long maximumNumberOfLines() {
        return ObjC.msgSendLong(peer, ObjC.sel("maximumNumberOfLines"));
    }
    public void setMaximumNumberOfLines(long n) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setMaximumNumberOfLines:"), n);
    }

    // ---- alignment delegation via NSControl, expose explicitly ----
    @Override
    public long alignment() { return super.alignment(); }
    @Override
    public void setAlignment(long a) { super.setAlignment(a); }

    // ---- delegate ----
    public MemorySegment delegate() {
        return ObjC.msgSendId(peer, ObjC.sel("delegate"));
    }
    public void setDelegate(MemorySegment d) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"), (MemorySegment) (d == null ? MemorySegment.NULL : d));
    }

    // ---- formatter exposed (from NSControl) ----
    @Override
    public MemorySegment formatter() { return super.formatter(); }
    @Override
    public void setFormatter(MemorySegment f) { super.setFormatter(f); }

    // ---- text manipulation ----
    public void selectText(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("selectText:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public boolean allowsEditingTextAttributes() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsEditingTextAttributes"));
    }
    public void setAllowsEditingTextAttributes(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsEditingTextAttributes:"), flag);
    }
    public boolean importsGraphics() {
        return ObjC.msgSendBool(peer, ObjC.sel("importsGraphics"));
    }
    public void setImportsGraphics(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setImportsGraphics:"), flag);
    }
    public long lineBreakStrategy() {
        return ObjC.msgSendLong(peer, ObjC.sel("lineBreakStrategy"));
    }
    public boolean allowsDefaultTighteningForTruncation() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsDefaultTighteningForTruncation"));
    }
    public void setAllowsDefaultTighteningForTruncation(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsDefaultTighteningForTruncation:"), flag);
    }
    public boolean isAutomaticTextCompletionEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isAutomaticTextCompletionEnabled"));
    }
    public void setAutomaticTextCompletionEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutomaticTextCompletionEnabled:"), flag);
    }
}
