package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSControl — the base of interactive controls (NSButton, NSTextField, ...).
/// Mirrors the native hierarchy: NSControl is an NSView, so controls can be
/// added to view hierarchies and positioned like any view.
///
/// Omitted from `NSControl.h`: `editWithFrame:editor:delegate:event:` and
/// `selectWithFrame:editor:delegate:start:length:` (no `void(rect,id,id,id)` /
/// `void(rect,id,id,int,int)` shape — requested); the
/// `NSControlTextEditingDelegate` protocol (needs upcall machinery);
/// deprecated `setFloatingPointFormat:left:right:`, `setNeedsDisplay`, `calcSize`.
public class NSControl extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hSendActionOn, MethodHandle hSizeThatFits, MethodHandle hDouble, MethodHandle hSetDouble, MethodHandle hSendActionTo, MethodHandle hBool, MethodHandle hExpansionFrame, MethodHandle hDrawExpansion, MethodHandle hFloat, MethodHandle hSetFloat) {}
    private static volatile Handles H;

    protected NSControl(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.SIZE, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.RECT, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT)));
    }

    // ---- existing API (kept) ----
    /// setEnabled: — interactive state.
    public void setEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEnabled:"), flag);
    }

    /// isEnabled — interactive state.
    public boolean isEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEnabled"));
    }

    /// setTarget: — the object that receives the action message.
    public void setTarget(MemorySegment target) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTarget:"), target);
    }

    /// [control target] — the action target (id).
    public MemorySegment target() {
        return ObjC.msgSendId(peer, ObjC.sel("target"));
    }

    /// setAction: — the selector sent to the target on activation.
    public void setAction(String actionSelector) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"), ObjC.sel(actionSelector));
    }

    /// [control action] — the action selector (SEL).
    public MemorySegment action() {
        return ObjC.msgSendId(peer, ObjC.sel("action"));
    }

    // ---- tag ----
    public long tag() {
        return ObjC.msgSendLong(peer, ObjC.sel("tag"));
    }
    public void setTag(long tag) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTag:"), tag);
    }

    // ---- continuous ----
    public boolean isContinuous() {
        return ObjC.msgSendBool(peer, ObjC.sel("isContinuous"));
    }
    public void setContinuous(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setContinuous:"), flag);
    }

    // ---- refusesFirstResponder ----
    public boolean refusesFirstResponder() {
        return ObjC.msgSendBool(peer, ObjC.sel("refusesFirstResponder"));
    }
    public void setRefusesFirstResponder(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setRefusesFirstResponder:"), flag);
    }

    // ---- highlighted ----
    public boolean isHighlighted() {
        return ObjC.msgSendBool(peer, ObjC.sel("isHighlighted"));
    }

    // ---- controlSize ----
    public long controlSize() {
        return ObjC.msgSendLong(peer, ObjC.sel("controlSize"));
    }
    public void setControlSize(long size) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setControlSize:"), size);
    }

    // ---- ignoresMultiClick ----
    public boolean ignoresMultiClick() {
        return ObjC.msgSendBool(peer, ObjC.sel("ignoresMultiClick"));
    }
    public void setIgnoresMultiClick(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setIgnoresMultiClick:"), flag);
    }

    // ---- formatter ----
    public MemorySegment formatter() {
        return ObjC.msgSendId(peer, ObjC.sel("formatter"));
    }
    public void setFormatter(MemorySegment formatter) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFormatter:"), formatter);
    }

    // ---- objectValue / stringValue / attributedStringValue ----
    public MemorySegment objectValue() {
        return ObjC.msgSendId(peer, ObjC.sel("objectValue"));
    }
    public void setObjectValue(MemorySegment value) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setObjectValue:"), value);
    }
    public String stringValue() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("stringValue")));
    }
    public void setStringValue(String value) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setStringValue:"), ObjC.nsstring(value));
    }
    public MemorySegment attributedStringValue() {
        return ObjC.msgSendId(peer, ObjC.sel("attributedStringValue"));
    }
    public void setAttributedStringValue(MemorySegment value) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAttributedStringValue:"), value);
    }
    // ---- typed NSAttributedString variants (preferred) ----
    public NSAttributedString attributedStringValueTyped() {
        return NSAttributedString.wrap(ObjC.msgSendId(peer, ObjC.sel("attributedStringValue")));
    }
    public void setAttributedStringValue(NSAttributedString value) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAttributedStringValue:"), (MemorySegment) (value == null ? MemorySegment.NULL : value.peer()));
    }

    // ---- numeric values ----
    public int intValue() {
        return (int) ObjC.msgSendLong(peer, ObjC.sel("intValue"));
    }
    public void setIntValue(int v) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setIntValue:"), v);
    }
    public long integerValue() {
        return ObjC.msgSendLong(peer, ObjC.sel("integerValue"));
    }
    public void setIntegerValue(long v) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setIntegerValue:"), v);
    }
    /// [control floatValue] — `float` in AppKit; reading it through the DOUBLE handle
    /// returned the wrong register. FLOAT handle, no widening round-trip.
    public float floatValue() {
        ensureInit();
        try {
            return (float) H.hFloat().invokeExact(peer, ObjC.sel("floatValue"));
        } catch (Throwable t) { throw new RuntimeException("floatValue failed", t); }
    }
    /// [control setFloatValue:] — `float` in AppKit; a double argument was reinterpreted
    /// (1.5f arrived as 0). FLOAT handle.
    public void setFloatValue(float v) {
        ensureInit();
        try { H.hSetFloat().invokeExact(peer, ObjC.sel("setFloatValue:"), v); } catch (Throwable t) { throw new RuntimeException("setFloatValue: failed", t); }
    }
    public double doubleValue() {
        ensureInit();
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("doubleValue")); } catch (Throwable t) { throw new RuntimeException("doubleValue failed", t); }
    }
    public void setDoubleValue(double v) {
        ensureInit();
        try { H.hSetDouble().invokeExact(peer, ObjC.sel("setDoubleValue:"), v); } catch (Throwable t) { throw new RuntimeException("setDoubleValue: failed", t); }
    }

    // ---- sizeThatFits / sizeToFit ----
    public NSSize sizeThatFits(NSSize size) {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) H.hSizeThatFits().invokeExact(ObjC.structSlot(), peer, ObjC.sel("sizeThatFits:"), size.toSegment());
            return NSSize.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("sizeThatFits: failed", t); }
    }
    public void sizeToFit() {
        ObjC.msgSendVoid(peer, ObjC.sel("sizeToFit"));
    }

    // ---- sendActionOn / sendAction:to: ----
    public long sendActionOn(long mask) {
        ensureInit();
        try { return (long) H.hSendActionOn().invokeExact(peer, ObjC.sel("sendActionOn:"), mask); } catch (Throwable t) { throw new RuntimeException("sendActionOn: failed", t); }
    }
    public boolean sendAction(MemorySegment action, MemorySegment target) {
        ensureInit();
        try { return (boolean) H.hSendActionTo().invokeExact(peer, ObjC.sel("sendAction:to:"), action, target); } catch (Throwable t) { throw new RuntimeException("sendAction:to: failed", t); }
    }

    // ---- take*ValueFrom: ----
    public void takeIntValueFrom(MemorySegment sender) { ObjC.msgSendVoidId(peer, ObjC.sel("takeIntValueFrom:"), sender); }
    public void takeFloatValueFrom(MemorySegment sender) { ObjC.msgSendVoidId(peer, ObjC.sel("takeFloatValueFrom:"), sender); }
    public void takeDoubleValueFrom(MemorySegment sender) { ObjC.msgSendVoidId(peer, ObjC.sel("takeDoubleValueFrom:"), sender); }
    public void takeStringValueFrom(MemorySegment sender) { ObjC.msgSendVoidId(peer, ObjC.sel("takeStringValueFrom:"), sender); }
    public void takeObjectValueFrom(MemorySegment sender) { ObjC.msgSendVoidId(peer, ObjC.sel("takeObjectValueFrom:"), sender); }
    public void takeIntegerValueFrom(MemorySegment sender) { ObjC.msgSendVoidId(peer, ObjC.sel("takeIntegerValueFrom:"), sender); }

    public void performClick(MemorySegment sender) { ObjC.msgSendVoidId(peer, ObjC.sel("performClick:"), sender); }

    // ---- font / alignment / lineBreak / writingDirection ----
    public NSFont font() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("font"));
        return NSFont.wrap(p);
    }
    public void setFont(NSFont font) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFont:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
    }
    public long alignment() {
        return ObjC.msgSendLong(peer, ObjC.sel("alignment"));
    }
    public void setAlignment(long alignment) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setAlignment:"), alignment);
    }
    public long lineBreakMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("lineBreakMode"));
    }
    public void setLineBreakMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLineBreakMode:"), mode);
    }
    public long baseWritingDirection() {
        return ObjC.msgSendLong(peer, ObjC.sel("baseWritingDirection"));
    }
    public void setBaseWritingDirection(long dir) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setBaseWritingDirection:"), dir);
    }
    public boolean usesSingleLineMode() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesSingleLineMode"));
    }
    public void setUsesSingleLineMode(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesSingleLineMode:"), flag);
    }
    public boolean allowsExpansionToolTips() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsExpansionToolTips"));
    }
    public void setAllowsExpansionToolTips(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsExpansionToolTips:"), flag);
    }

    // ---- cell ----
    public MemorySegment cell() {
        return ObjC.msgSendId(peer, ObjC.sel("cell"));
    }
    public void setCell(MemorySegment cell) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setCell:"), cell);
    }

    // ---- field editor ----
    /// [control currentEditor] — the field editor editing this control (nil-safe; null when idle).
    public NSText currentEditor() {
        return NSText.wrap(ObjC.msgSendId(peer, ObjC.sel("currentEditor")));
    }
    /// [control abortEditing] — discard uncommitted edits.
    public boolean abortEditing() {
        ensureInit();
        try { return (boolean) H.hBool().invokeExact(peer, ObjC.sel("abortEditing")); } catch (Throwable t) { throw new RuntimeException("abortEditing failed", t); }
    }
    /// [control validateEditing] — commit the editor value through the formatter.
    public void validateEditing() {
        ObjC.msgSendVoid(peer, ObjC.sel("validateEditing"));
    }
    /// [control endEditing:] — end the given editor's session (nil-safe).
    public void endEditing(NSText editor) {
        ObjC.msgSendVoidId(peer, ObjC.sel("endEditing:"), (MemorySegment) (editor == null ? MemorySegment.NULL : editor.peer()));
    }

    // ---- expansion tool tips ----
    /// [control expansionFrameWithFrame:] — expansion tool tip frame (NSZeroRect when content fits).
    public NSRect expansionFrameWithFrame(NSRect contentFrame) {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) H.hExpansionFrame().invokeExact(ObjC.structSlot(), peer, ObjC.sel("expansionFrameWithFrame:"), contentFrame.toSegment());
            return NSRect.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("expansionFrameWithFrame: failed", t); }
    }
    /// [control drawWithExpansionFrame:inView:] — draw the expansion tool tip content.
    public void drawWithExpansionFrameInView(NSRect contentFrame, NSView view) {
        ensureInit();
        try { H.hDrawExpansion().invokeExact(peer, ObjC.sel("drawWithExpansionFrame:inView:"), contentFrame.toSegment(), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer())); } catch (Throwable t) { throw new RuntimeException("drawWithExpansionFrame:inView: failed", t); }
    }

    // ---- cell queries ----
    /// [NSControl cellClass] — the default cell class for controls (a Class id, never nil).
    public static MemorySegment cellClass() {
        return ObjC.msgSendId(ObjC.cls("NSControl"), ObjC.sel("cellClass"));
    }
    /// [control selectedCell] — the selected cell (raw id; nil becomes NULL segment).
    public MemorySegment selectedCell() {
        return ObjC.msgSendId(peer, ObjC.sel("selectedCell"));
    }
    /// [control selectedTag] — tag of the selected cell (-1 when none).
    public long selectedTag() {
        return ObjC.msgSendLong(peer, ObjC.sel("selectedTag"));
    }
    /// [control updateCell:] — mark the cell dirty (pass `MemorySegment.NULL` for nil).
    public void updateCell(MemorySegment cell) { ObjC.msgSendVoidId(peer, ObjC.sel("updateCell:"), cell); }
    /// [control updateCellInside:] — mark the cell's interior dirty.
    public void updateCellInside(MemorySegment cell) { ObjC.msgSendVoidId(peer, ObjC.sel("updateCellInside:"), cell); }
    /// [control drawCellInside:] — draw the cell's interior immediately.
    public void drawCellInside(MemorySegment cell) { ObjC.msgSendVoidId(peer, ObjC.sel("drawCellInside:"), cell); }
    /// [control drawCell:] — draw the cell immediately.
    public void drawCell(MemorySegment cell) { ObjC.msgSendVoidId(peer, ObjC.sel("drawCell:"), cell); }
    /// [control selectCell:] — select the cell.
    public void selectCell(MemorySegment cell) { ObjC.msgSendVoidId(peer, ObjC.sel("selectCell:"), cell); }
}
