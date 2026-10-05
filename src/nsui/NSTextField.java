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
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSTextField.h
/// OMITTED: NSTextFieldDelegate candidate methods (textField:textView:candidatesForSelectedRange:,
/// candidates:forSelectedRange:, shouldSelectCandidateAtIndex:) — their (ID,ID,RANGE)/(ID,ID,ID,RANGE)/
/// (BOOL,ID,ID,INT) shapes are NOT in the Sig vocabulary and they need upcall delegate-proxy
/// machinery; setTitleWithMnemonic: (deprecated, use setStringValue:); acceptsFirstResponder
/// (inherited from NSResponder, not redeclared).
public class NSTextField extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hDouble, MethodHandle hSetDouble, MethodHandle hBoolId) {}
    private static volatile Handles H;

    protected NSTextField(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
    }

    /// `[[NSTextField alloc] initWithFrame:frame]` — a new text field at the given rect.
        public static NSTextField create(NSRect frame) {
        ensureInit();
        return new NSTextField(ObjC.newView("NSTextField", frame));
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
    /// +labelWithAttributedString: — non-editable field showing attributed text.
    /// Shape (ID,ID) is in the Sig vocabulary (grep Sig.java: of(Ret.ID, Arg.ID)).
    public static NSTextField labelWithAttributedString(NSAttributedString attr) {
        ensureInit();
        MemorySegment p = ObjC.msgSendIdId(ObjC.cls("NSTextField"), ObjC.sel("labelWithAttributedString:"),
                (MemorySegment) (attr == null ? MemorySegment.NULL : attr.peer()));
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
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("preferredMaxLayoutWidth")); } catch (Throwable t) { throw new RuntimeException("preferredMaxLayoutWidth failed", t); }
    }
    public void setPreferredMaxLayoutWidth(double w) {
        ensureInit();
        try { H.hSetDouble().invokeExact(peer, ObjC.sel("setPreferredMaxLayoutWidth:"), w); } catch (Throwable t) { throw new RuntimeException("setPreferredMaxLayoutWidth: failed", t); }
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

    // ---- lineBreakStrategy setter (getter above; shapes VOID,INT / INT () in vocabulary) ----
    /// [field setLineBreakStrategy:] — line break strategies for layout.
    public void setLineBreakStrategy(long strategy) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLineBreakStrategy:"), strategy);
    }

    // ---- allowsWritingTools / allowsWritingToolsAffordance (BOOL shapes in vocabulary) ----
    /// [field allowsWritingTools] — field editor works with Writing Tools (15.2+).
    public boolean allowsWritingTools() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsWritingTools"));
    }
    /// [field setAllowsWritingTools:].
    public void setAllowsWritingTools(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsWritingTools:"), flag);
    }
    /// [field allowsWritingToolsAffordance] (15.4+).
    public boolean allowsWritingToolsAffordance() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsWritingToolsAffordance"));
    }
    /// [field setAllowsWritingToolsAffordance:].
    public void setAllowsWritingToolsAffordance(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsWritingToolsAffordance:"), flag);
    }

    // ---- allowsCharacterPickerTouchBarItem (NSTouchBar category, BOOL shapes) ----
    /// [field allowsCharacterPickerTouchBarItem].
    public boolean allowsCharacterPickerTouchBarItem() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsCharacterPickerTouchBarItem"));
    }
    /// [field setAllowsCharacterPickerTouchBarItem:].
    public void setAllowsCharacterPickerTouchBarItem(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsCharacterPickerTouchBarItem:"), flag);
    }

    // ---- placeholderStrings / placeholderAttributedStrings (ID shapes in vocabulary) ----
    /// [field placeholderStrings] — animated cycling placeholders (macOS 26+), or nil.
    public NSArray placeholderStrings() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("placeholderStrings")));
    }
    /// [field setPlaceholderStrings:] — pass null to clear.
    public void setPlaceholderStrings(NSArray strings) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPlaceholderStrings:"),
                (MemorySegment) (strings == null ? MemorySegment.NULL : strings.peer()));
    }
    /// [field placeholderAttributedStrings] — attributed variant (macOS 26+), or nil.
    public NSArray placeholderAttributedStrings() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("placeholderAttributedStrings")));
    }
    /// [field setPlaceholderAttributedStrings:].
    public void setPlaceholderAttributedStrings(NSArray strings) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPlaceholderAttributedStrings:"),
                (MemorySegment) (strings == null ? MemorySegment.NULL : strings.peer()));
    }

    // ---- resolvesNaturalAlignmentWithBaseWritingDirection (BOOL shapes) ----
    /// [field resolvesNaturalAlignmentWithBaseWritingDirection] (macOS 26+).
    public boolean resolvesNaturalAlignmentWithBaseWritingDirection() {
        return ObjC.msgSendBool(peer, ObjC.sel("resolvesNaturalAlignmentWithBaseWritingDirection"));
    }
    /// [field setResolvesNaturalAlignmentWithBaseWritingDirection:].
    public void setResolvesNaturalAlignmentWithBaseWritingDirection(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setResolvesNaturalAlignmentWithBaseWritingDirection:"), flag);
    }

    // ---- editing notifications (shapes BOOL,ID / VOID,ID in vocabulary) ----
    /// [field textShouldBeginEditing:] — consult the field before editing starts.
    public boolean textShouldBeginEditing(MemorySegment textObject) {
        ensureInit();
        try {
            return (boolean) H.hBoolId().invokeExact(peer, ObjC.sel("textShouldBeginEditing:"),
                    (MemorySegment) (textObject == null ? MemorySegment.NULL : textObject));
        } catch (Throwable t) { throw new RuntimeException("textShouldBeginEditing: failed", t); }
    }
    /// [field textShouldEndEditing:].
    public boolean textShouldEndEditing(MemorySegment textObject) {
        ensureInit();
        try {
            return (boolean) H.hBoolId().invokeExact(peer, ObjC.sel("textShouldEndEditing:"),
                    (MemorySegment) (textObject == null ? MemorySegment.NULL : textObject));
        } catch (Throwable t) { throw new RuntimeException("textShouldEndEditing: failed", t); }
    }
    /// [field textDidBeginEditing:] — notification post.
    public void textDidBeginEditing(MemorySegment notification) {
        ObjC.msgSendVoidId(peer, ObjC.sel("textDidBeginEditing:"),
                (MemorySegment) (notification == null ? MemorySegment.NULL : notification));
    }
    /// [field textDidEndEditing:].
    public void textDidEndEditing(MemorySegment notification) {
        ObjC.msgSendVoidId(peer, ObjC.sel("textDidEndEditing:"),
                (MemorySegment) (notification == null ? MemorySegment.NULL : notification));
    }
    /// [field textDidChange:].
    public void textDidChange(MemorySegment notification) {
        ObjC.msgSendVoidId(peer, ObjC.sel("textDidChange:"),
                (MemorySegment) (notification == null ? MemorySegment.NULL : notification));
    }
}
