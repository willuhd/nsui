package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTableRowView — draws one table row: selection, emphasis, drop targeting.
/// Thin stateless wrapper.
public final class NSTableRowView extends NSView {

    private NSTableRowView(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static NSTableRowView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTableRowView(peer);
    }

    /// [[NSTableRowView alloc] initWithFrame:].
    public static NSTableRowView create(NSRect frame) {
        return new NSTableRowView(ObjC.newView("NSTableRowView", frame));
    }

    /// isSelected / setSelected:.
    public boolean isSelected() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSelected"));
    }

    /// setSelected:.
    public void setSelected(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setSelected:"), flag);
    }

    /// isEmphasized / setEmphasized:.
    public boolean isEmphasized() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEmphasized"));
    }

    /// setEmphasized:.
    public void setEmphasized(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEmphasized:"), flag);
    }

    /// isGroupRowStyle / setGroupRowStyle:.
    public boolean isGroupRowStyle() {
        return ObjC.msgSendBool(peer, ObjC.sel("isGroupRowStyle"));
    }

    /// setGroupRowStyle:.
    public void setGroupRowStyle(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setGroupRowStyle:"), flag);
    }

    /// selectionHighlightStyle (NSTableViewSelectionHighlightStyle, raw).
    public long selectionHighlightStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("selectionHighlightStyle"));
    }

    /// setSelectionHighlightStyle:.
    public void setSelectionHighlightStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSelectionHighlightStyle:"), style);
    }

    // ---- batch: Tables — neighbor selection, drop targeting, background and cell access ----
    // (all shapes already in Sig.VOCABULARY: Ret.BOOL / Ret.VOID+Arg.BOOL, Ret.INT / Ret.VOID+Arg.INT,
    // Ret.DOUBLE / Ret.VOID+Arg.DOUBLE, Ret.ID / Ret.VOID+Arg.ID, Ret.ID+Arg.INT).
    // Omitted here (documented, reported to coordinator):
    // - drawBackgroundInRect:/drawSelectionInRect:/drawSeparatorInRect:/drawDraggingDestinationFeedbackInRect:
    //   — subclass override points (plus NSRect args), not direct wrappers.
    // - isFlipped — inherited from NSView.

    /// isPreviousRowSelected / setPreviousRowSelected:.
    public boolean isPreviousRowSelected() {
        return ObjC.msgSendBool(peer, ObjC.sel("isPreviousRowSelected"));
    }

    /// setPreviousRowSelected:.
    public void setPreviousRowSelected(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setPreviousRowSelected:"), flag);
    }

    /// isNextRowSelected / setNextRowSelected:.
    public boolean isNextRowSelected() {
        return ObjC.msgSendBool(peer, ObjC.sel("isNextRowSelected"));
    }

    /// setNextRowSelected:.
    public void setNextRowSelected(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setNextRowSelected:"), flag);
    }

    /// isFloating — YES while a group row floats above others.
    public boolean isFloating() {
        return ObjC.msgSendBool(peer, ObjC.sel("isFloating"));
    }

    /// isTargetForDropOperation / setTargetForDropOperation:.
    public boolean isTargetForDropOperation() {
        return ObjC.msgSendBool(peer, ObjC.sel("isTargetForDropOperation"));
    }

    /// setTargetForDropOperation:.
    public void setTargetForDropOperation(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setTargetForDropOperation:"), flag);
    }

    /// draggingDestinationFeedbackStyle (NSTableViewDraggingDestinationFeedbackStyle, raw).
    public long draggingDestinationFeedbackStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("draggingDestinationFeedbackStyle"));
    }

    /// setDraggingDestinationFeedbackStyle:.
    public void setDraggingDestinationFeedbackStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDraggingDestinationFeedbackStyle:"), style);
    }

    /// indentationForDropOperation, in points.
    public double indentationForDropOperation() {
        try {
            return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("indentationForDropOperation"));
        } catch (Throwable t) {
            throw new RuntimeException("indentationForDropOperation failed", t);
        }
    }

    /// setIndentationForDropOperation:.
    public void setIndentationForDropOperation(double indent) {
        try {
            ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, ObjC.sel("setIndentationForDropOperation:"), indent);
        } catch (Throwable t) {
            throw new RuntimeException("setIndentationForDropOperation: failed", t);
        }
    }

    /// interiorBackgroundStyle (NSBackgroundStyle, raw, readonly).
    public long interiorBackgroundStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("interiorBackgroundStyle"));
    }

    /// backgroundColor — defaults to the table's backgroundColor.
    public NSColor backgroundColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("backgroundColor")));
    }

    /// setBackgroundColor: (animatable).
    public void setBackgroundColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBackgroundColor:"),
                (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// viewAtColumn: — the cell view at a column, or nil.
    public NSView viewAtColumn(long column) {
        try {
            MemorySegment v = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.INT))
                    .invokeExact(peer, ObjC.sel("viewAtColumn:"), column);
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("viewAtColumn: failed", t);
        }
    }

    /// numberOfColumns — views in this row (readonly).
    public long numberOfColumns() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfColumns"));
    }
}
