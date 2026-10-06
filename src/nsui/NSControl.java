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

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment setEnabled;
        static MemorySegment isEnabled;
        static MemorySegment setTarget;
        static MemorySegment target;
        static MemorySegment setAction;
        static MemorySegment action;
        static MemorySegment tag;
        static MemorySegment setTag;
        static MemorySegment isContinuous;
        static MemorySegment setContinuous;
        static MemorySegment refusesFirstResponder;
        static MemorySegment setRefusesFirstResponder;
        static MemorySegment isHighlighted;
        static MemorySegment controlSize;
        static MemorySegment setControlSize;
        static MemorySegment ignoresMultiClick;
        static MemorySegment setIgnoresMultiClick;
        static MemorySegment formatter;
        static MemorySegment setFormatter;
        static MemorySegment objectValue;
        static MemorySegment setObjectValue;
        static MemorySegment stringValue;
        static MemorySegment setStringValue;
        static MemorySegment attributedStringValue;
        static MemorySegment setAttributedStringValue;
        static MemorySegment intValue;
        static MemorySegment setIntValue;
        static MemorySegment integerValue;
        static MemorySegment setIntegerValue;
        static MemorySegment floatValue;
        static MemorySegment setFloatValue;
        static MemorySegment doubleValue;
        static MemorySegment setDoubleValue;
        static MemorySegment sizeThatFits;
        static MemorySegment sizeToFit;
        static MemorySegment sendActionOn;
        static MemorySegment sendAction_to;
        static MemorySegment takeIntValueFrom;
        static MemorySegment takeFloatValueFrom;
        static MemorySegment takeDoubleValueFrom;
        static MemorySegment takeStringValueFrom;
        static MemorySegment takeObjectValueFrom;
        static MemorySegment takeIntegerValueFrom;
        static MemorySegment performClick;
        static MemorySegment font;
        static MemorySegment setFont;
        static MemorySegment alignment;
        static MemorySegment setAlignment;
        static MemorySegment lineBreakMode;
        static MemorySegment setLineBreakMode;
        static MemorySegment baseWritingDirection;
        static MemorySegment setBaseWritingDirection;
        static MemorySegment usesSingleLineMode;
        static MemorySegment setUsesSingleLineMode;
        static MemorySegment allowsExpansionToolTips;
        static MemorySegment setAllowsExpansionToolTips;
        static MemorySegment cell;
        static MemorySegment setCell;
        static MemorySegment currentEditor;
        static MemorySegment abortEditing;
        static MemorySegment validateEditing;
        static MemorySegment endEditing;
        static MemorySegment expansionFrameWithFrame;
        static MemorySegment drawWithExpansionFrame_inView;
        static MemorySegment cellClass;
        static MemorySegment selectedCell;
        static MemorySegment selectedTag;
        static MemorySegment updateCell;
        static MemorySegment updateCellInside;
        static MemorySegment drawCellInside;
        static MemorySegment drawCell;
        static MemorySegment selectCell;
        static void populate() {
            setEnabled = ObjC.sel("setEnabled:");
            isEnabled = ObjC.sel("isEnabled");
            setTarget = ObjC.sel("setTarget:");
            target = ObjC.sel("target");
            setAction = ObjC.sel("setAction:");
            action = ObjC.sel("action");
            tag = ObjC.sel("tag");
            setTag = ObjC.sel("setTag:");
            isContinuous = ObjC.sel("isContinuous");
            setContinuous = ObjC.sel("setContinuous:");
            refusesFirstResponder = ObjC.sel("refusesFirstResponder");
            setRefusesFirstResponder = ObjC.sel("setRefusesFirstResponder:");
            isHighlighted = ObjC.sel("isHighlighted");
            controlSize = ObjC.sel("controlSize");
            setControlSize = ObjC.sel("setControlSize:");
            ignoresMultiClick = ObjC.sel("ignoresMultiClick");
            setIgnoresMultiClick = ObjC.sel("setIgnoresMultiClick:");
            formatter = ObjC.sel("formatter");
            setFormatter = ObjC.sel("setFormatter:");
            objectValue = ObjC.sel("objectValue");
            setObjectValue = ObjC.sel("setObjectValue:");
            stringValue = ObjC.sel("stringValue");
            setStringValue = ObjC.sel("setStringValue:");
            attributedStringValue = ObjC.sel("attributedStringValue");
            setAttributedStringValue = ObjC.sel("setAttributedStringValue:");
            intValue = ObjC.sel("intValue");
            setIntValue = ObjC.sel("setIntValue:");
            integerValue = ObjC.sel("integerValue");
            setIntegerValue = ObjC.sel("setIntegerValue:");
            floatValue = ObjC.sel("floatValue");
            setFloatValue = ObjC.sel("setFloatValue:");
            doubleValue = ObjC.sel("doubleValue");
            setDoubleValue = ObjC.sel("setDoubleValue:");
            sizeThatFits = ObjC.sel("sizeThatFits:");
            sizeToFit = ObjC.sel("sizeToFit");
            sendActionOn = ObjC.sel("sendActionOn:");
            sendAction_to = ObjC.sel("sendAction:to:");
            takeIntValueFrom = ObjC.sel("takeIntValueFrom:");
            takeFloatValueFrom = ObjC.sel("takeFloatValueFrom:");
            takeDoubleValueFrom = ObjC.sel("takeDoubleValueFrom:");
            takeStringValueFrom = ObjC.sel("takeStringValueFrom:");
            takeObjectValueFrom = ObjC.sel("takeObjectValueFrom:");
            takeIntegerValueFrom = ObjC.sel("takeIntegerValueFrom:");
            performClick = ObjC.sel("performClick:");
            font = ObjC.sel("font");
            setFont = ObjC.sel("setFont:");
            alignment = ObjC.sel("alignment");
            setAlignment = ObjC.sel("setAlignment:");
            lineBreakMode = ObjC.sel("lineBreakMode");
            setLineBreakMode = ObjC.sel("setLineBreakMode:");
            baseWritingDirection = ObjC.sel("baseWritingDirection");
            setBaseWritingDirection = ObjC.sel("setBaseWritingDirection:");
            usesSingleLineMode = ObjC.sel("usesSingleLineMode");
            setUsesSingleLineMode = ObjC.sel("setUsesSingleLineMode:");
            allowsExpansionToolTips = ObjC.sel("allowsExpansionToolTips");
            setAllowsExpansionToolTips = ObjC.sel("setAllowsExpansionToolTips:");
            cell = ObjC.sel("cell");
            setCell = ObjC.sel("setCell:");
            currentEditor = ObjC.sel("currentEditor");
            abortEditing = ObjC.sel("abortEditing");
            validateEditing = ObjC.sel("validateEditing");
            endEditing = ObjC.sel("endEditing:");
            expansionFrameWithFrame = ObjC.sel("expansionFrameWithFrame:");
            drawWithExpansionFrame_inView = ObjC.sel("drawWithExpansionFrame:inView:");
            cellClass = ObjC.sel("cellClass");
            selectedCell = ObjC.sel("selectedCell");
            selectedTag = ObjC.sel("selectedTag");
            updateCell = ObjC.sel("updateCell:");
            updateCellInside = ObjC.sel("updateCellInside:");
            drawCellInside = ObjC.sel("drawCellInside:");
            drawCell = ObjC.sel("drawCell:");
            selectCell = ObjC.sel("selectCell:");
        }
    }

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hSendActionOn, MethodHandle hSizeThatFits, MethodHandle hDouble, MethodHandle hSetDouble, MethodHandle hSendActionTo, MethodHandle hBool, MethodHandle hExpansionFrame, MethodHandle hDrawExpansion, MethodHandle hFloat, MethodHandle hSetFloat) {}
    private static volatile Handles H;

    protected NSControl(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
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
            Sels.populate();
}

    // ---- existing API (kept) ----
    /// setEnabled: — interactive state.
    public void setEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setEnabled, flag);
    }

    /// isEnabled — interactive state.
    public boolean isEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isEnabled);
    }

    /// setTarget: — the object that receives the action message.
    public void setTarget(MemorySegment target) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setTarget, target);
    }

    /// [control target] — the action target (id).
    public MemorySegment target() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.target);
    }

    /// setAction: — the selector sent to the target on activation.
    public void setAction(String actionSelector) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setAction, ObjC.sel(actionSelector));
    }

    /// [control action] — the action selector (SEL).
    public MemorySegment action() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.action);
    }

    // ---- tag ----
    public long tag() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.tag);
    }
    public void setTag(long tag) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setTag, tag);
    }

    // ---- continuous ----
    public boolean isContinuous() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isContinuous);
    }
    public void setContinuous(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setContinuous, flag);
    }

    // ---- refusesFirstResponder ----
    public boolean refusesFirstResponder() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.refusesFirstResponder);
    }
    public void setRefusesFirstResponder(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setRefusesFirstResponder, flag);
    }

    // ---- highlighted ----
    public boolean isHighlighted() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isHighlighted);
    }

    // ---- controlSize ----
    public long controlSize() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.controlSize);
    }
    public void setControlSize(long size) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setControlSize, size);
    }

    // ---- ignoresMultiClick ----
    public boolean ignoresMultiClick() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.ignoresMultiClick);
    }
    public void setIgnoresMultiClick(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setIgnoresMultiClick, flag);
    }

    // ---- formatter ----
    public MemorySegment formatter() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.formatter);
    }
    public void setFormatter(MemorySegment formatter) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setFormatter, formatter);
    }

    // ---- objectValue / stringValue / attributedStringValue ----
    public MemorySegment objectValue() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.objectValue);
    }
    public void setObjectValue(MemorySegment value) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setObjectValue, value);
    }
    public String stringValue() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.stringValue));
    }
    public void setStringValue(String value) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setStringValue, ObjC.nsstring(value));
    }
    public MemorySegment attributedStringValue() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.attributedStringValue);
    }
    public void setAttributedStringValue(MemorySegment value) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setAttributedStringValue, value);
    }
    // ---- typed NSAttributedString variants (preferred) ----
    public NSAttributedString attributedStringValueTyped() {
        ensureInit();
        return NSAttributedString.wrap(ObjC.msgSendId(peer, Sels.attributedStringValue));
    }
    public void setAttributedStringValue(NSAttributedString value) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setAttributedStringValue, (MemorySegment) (value == null ? MemorySegment.NULL : value.peer()));
    }

    // ---- numeric values ----
    public int intValue() {
        ensureInit();
        return (int) ObjC.msgSendLong(peer, Sels.intValue);
    }
    public void setIntValue(int v) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setIntValue, v);
    }
    public long integerValue() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.integerValue);
    }
    public void setIntegerValue(long v) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setIntegerValue, v);
    }
    /// [control floatValue] — `float` in AppKit; reading it through the DOUBLE handle
    /// returned the wrong register. FLOAT handle, no widening round-trip.
    public float floatValue() {
        ensureInit();
        try {
            return (float) H.hFloat().invokeExact(peer, Sels.floatValue);
        } catch (Throwable t) { throw new RuntimeException("floatValue failed", t); }
    }
    /// [control setFloatValue:] — `float` in AppKit; a double argument was reinterpreted
    /// (1.5f arrived as 0). FLOAT handle.
    public void setFloatValue(float v) {
        ensureInit();
        try { H.hSetFloat().invokeExact(peer, Sels.setFloatValue, v); } catch (Throwable t) { throw new RuntimeException("setFloatValue: failed", t); }
    }
    public double doubleValue() {
        ensureInit();
        try { return (double) H.hDouble().invokeExact(peer, Sels.doubleValue); } catch (Throwable t) { throw new RuntimeException("doubleValue failed", t); }
    }
    public void setDoubleValue(double v) {
        ensureInit();
        try { H.hSetDouble().invokeExact(peer, Sels.setDoubleValue, v); } catch (Throwable t) { throw new RuntimeException("setDoubleValue: failed", t); }
    }

    // ---- sizeThatFits / sizeToFit ----
    public NSSize sizeThatFits(NSSize size) {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) H.hSizeThatFits().invokeExact(ObjC.structSlot(), peer, Sels.sizeThatFits, size.toSegment());
            return NSSize.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("sizeThatFits: failed", t); }
    }
    public void sizeToFit() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.sizeToFit);
    }

    // ---- sendActionOn / sendAction:to: ----
    public long sendActionOn(long mask) {
        ensureInit();
        try { return (long) H.hSendActionOn().invokeExact(peer, Sels.sendActionOn, mask); } catch (Throwable t) { throw new RuntimeException("sendActionOn: failed", t); }
    }
    public boolean sendAction(MemorySegment action, MemorySegment target) {
        ensureInit();
        try { return (boolean) H.hSendActionTo().invokeExact(peer, Sels.sendAction_to, action, target); } catch (Throwable t) { throw new RuntimeException("sendAction:to: failed", t); }
    }

    // ---- take*ValueFrom: ----
    public void takeIntValueFrom(MemorySegment sender) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.takeIntValueFrom, sender); }
    public void takeFloatValueFrom(MemorySegment sender) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.takeFloatValueFrom, sender); }
    public void takeDoubleValueFrom(MemorySegment sender) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.takeDoubleValueFrom, sender); }
    public void takeStringValueFrom(MemorySegment sender) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.takeStringValueFrom, sender); }
    public void takeObjectValueFrom(MemorySegment sender) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.takeObjectValueFrom, sender); }
    public void takeIntegerValueFrom(MemorySegment sender) {
ensureInit(); ObjC.msgSendVoidId(peer, Sels.takeIntegerValueFrom, sender); }

    public void performClick(MemorySegment sender) {
ensureInit(); ObjC.msgSendVoidId(peer, Sels.performClick, sender); }

    // ---- font / alignment / lineBreak / writingDirection ----
    public NSFont font() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(peer, Sels.font);
        return NSFont.wrap(p);
    }
    public void setFont(NSFont font) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setFont, (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
    }
    public long alignment() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.alignment);
    }
    public void setAlignment(long alignment) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setAlignment, alignment);
    }
    public long lineBreakMode() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.lineBreakMode);
    }
    public void setLineBreakMode(long mode) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setLineBreakMode, mode);
    }
    public long baseWritingDirection() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.baseWritingDirection);
    }
    public void setBaseWritingDirection(long dir) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setBaseWritingDirection, dir);
    }
    public boolean usesSingleLineMode() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesSingleLineMode);
    }
    public void setUsesSingleLineMode(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setUsesSingleLineMode, flag);
    }
    public boolean allowsExpansionToolTips() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsExpansionToolTips);
    }
    public void setAllowsExpansionToolTips(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAllowsExpansionToolTips, flag);
    }

    // ---- cell ----
    public MemorySegment cell() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.cell);
    }
    public void setCell(MemorySegment cell) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setCell, cell);
    }

    // ---- field editor ----
    /// [control currentEditor] — the field editor editing this control (nil-safe; null when idle).
    public NSText currentEditor() {
        ensureInit();
        return NSText.wrap(ObjC.msgSendId(peer, Sels.currentEditor));
    }
    /// [control abortEditing] — discard uncommitted edits.
    public boolean abortEditing() {
        ensureInit();
        try { return (boolean) H.hBool().invokeExact(peer, Sels.abortEditing); } catch (Throwable t) { throw new RuntimeException("abortEditing failed", t); }
    }
    /// [control validateEditing] — commit the editor value through the formatter.
    public void validateEditing() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.validateEditing);
    }
    /// [control endEditing:] — end the given editor's session (nil-safe).
    public void endEditing(NSText editor) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.endEditing, (MemorySegment) (editor == null ? MemorySegment.NULL : editor.peer()));
    }

    // ---- expansion tool tips ----
    /// [control expansionFrameWithFrame:] — expansion tool tip frame (NSZeroRect when content fits).
    public NSRect expansionFrameWithFrame(NSRect contentFrame) {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) H.hExpansionFrame().invokeExact(ObjC.structSlot(), peer, Sels.expansionFrameWithFrame, contentFrame.toSegment());
            return NSRect.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("expansionFrameWithFrame: failed", t); }
    }
    /// [control drawWithExpansionFrame:inView:] — draw the expansion tool tip content.
    public void drawWithExpansionFrameInView(NSRect contentFrame, NSView view) {
        ensureInit();
        try { H.hDrawExpansion().invokeExact(peer, Sels.drawWithExpansionFrame_inView, contentFrame.toSegment(), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer())); } catch (Throwable t) { throw new RuntimeException("drawWithExpansionFrame:inView: failed", t); }
    }

    // ---- cell queries ----
    /// [NSControl cellClass] — the default cell class for controls (a Class id, never nil).
    public static MemorySegment cellClass() {
        ensureInit();
        return ObjC.msgSendId(ObjC.cls("NSControl"), Sels.cellClass);
    }
    /// [control selectedCell] — the selected cell (raw id; nil becomes NULL segment).
    public MemorySegment selectedCell() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.selectedCell);
    }
    /// [control selectedTag] — tag of the selected cell (-1 when none).
    public long selectedTag() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.selectedTag);
    }
    /// [control updateCell:] — mark the cell dirty (pass `MemorySegment.NULL` for nil).
    public void updateCell(MemorySegment cell) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.updateCell, cell); }
    /// [control updateCellInside:] — mark the cell's interior dirty.
    public void updateCellInside(MemorySegment cell) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.updateCellInside, cell); }
    /// [control drawCellInside:] — draw the cell's interior immediately.
    public void drawCellInside(MemorySegment cell) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.drawCellInside, cell); }
    /// [control drawCell:] — draw the cell immediately.
    public void drawCell(MemorySegment cell) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.drawCell, cell); }
    /// [control selectCell:] — select the cell.
    public void selectCell(MemorySegment cell) {
ensureInit(); ObjC.msgSendVoidId(peer, Sels.selectCell, cell); }
}
