package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTableView — an AppKit table view driven by a data source. Thin, 1:1,
/// stateless wrapper over the native `NSTableView`: each method maps to one
/// `objc_msgSend` selector, and the data source / delegate are ordinary
/// `DelegateProxy` instances passed as raw ids.
///
/// Created via `[[NSTableView alloc] initWithFrame:]` and typically embedded
/// in an `NSScrollView` via `setDocumentView`.
public class NSTableView extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hInitFrame, MethodHandle hVoidId, MethodHandle hVoid, MethodHandle hInt, MethodHandle hVoidBool, MethodHandle hVoidDouble, MethodHandle hDouble, MethodHandle hId, MethodHandle hVoidIdBool, MethodHandle hVoidInt, MethodHandle hEdit, MethodHandle hBoolInt) {}
    private static volatile Handles H;

    protected NSTableView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    protected static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT, Arg.ID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.INT)));
    }

    /// `[[NSTableView alloc] initWithFrame:frame]` — a new table view.
    public static NSTableView create(NSRect frame) {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(ObjC.cls("NSTableView"), ObjC.sel("alloc"));
        try {
            v = (MemorySegment) H.hInitFrame().invokeExact(v, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSTableView", t);
        }
        if (v.address() == 0) {
            throw new IllegalStateException("NSTableView alloc/initWithFrame: returned nil");
        }
        return new NSTableView(v);
    }

    // ---------------------------------------------------------------- instance API

    /// [table addTableColumn:] — append a column.
    public void addTableColumn(NSTableColumn column) {
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("addTableColumn:"), column.peer());
        } catch (Throwable t) {
            throw new RuntimeException("addTableColumn: failed", t);
        }
    }

    /// [table removeTableColumn:] — remove a column.
    public void removeTableColumn(NSTableColumn column) {
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("removeTableColumn:"), column.peer());
        } catch (Throwable t) {
            throw new RuntimeException("removeTableColumn: failed", t);
        }
    }

    /// [table setDataSource:] — the object answering row-count / cell-value queries.
    public void setDataSource(MemorySegment dataSource) {
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("setDataSource:"), dataSource);
        } catch (Throwable t) {
            throw new RuntimeException("setDataSource: failed", t);
        }
    }

    /// [table dataSource]
    public MemorySegment dataSource() {
        try {
            return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("dataSource"));
        } catch (Throwable t) {
            throw new RuntimeException("dataSource failed", t);
        }
    }

    /// [table setDelegate:] — the object notified of table events.
    public void setDelegate(MemorySegment delegate) {
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("setDelegate:"), delegate);
        } catch (Throwable t) {
            throw new RuntimeException("setDelegate: failed", t);
        }
    }

    /// [table delegate]
    public MemorySegment delegate() {
        try {
            return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("delegate"));
        } catch (Throwable t) {
            throw new RuntimeException("delegate failed", t);
        }
    }

    /// [table reloadData] — force the table to re-query its data source.
    public void reloadData() {
        try {
            H.hVoid().invokeExact(peer, ObjC.sel("reloadData"));
        } catch (Throwable t) {
            throw new RuntimeException("reloadData failed", t);
        }
    }

    /// [table numberOfRows] — the current number of rows the table is displaying.
    public long numberOfRows() {
        try {
            return (long) H.hInt().invokeExact(peer, ObjC.sel("numberOfRows"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfRows failed", t);
        }
    }

    /// [table numberOfColumns]
    public long numberOfColumns() {
        try {
            return (long) H.hInt().invokeExact(peer, ObjC.sel("numberOfColumns"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfColumns failed", t);
        }
    }

    /// [table usesAlternatingRowBackgroundColors]
    public boolean usesAlternatingRowBackgroundColors() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesAlternatingRowBackgroundColors"));
    }

    /// [table setUsesAlternatingRowBackgroundColors:] — zebra stripes while drawing.
    public void setUsesAlternatingRowBackgroundColors(boolean flag) {
        try {
            H.hVoidBool().invokeExact(peer, ObjC.sel("setUsesAlternatingRowBackgroundColors:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setUsesAlternatingRowBackgroundColors: failed", t);
        }
    }

    /// [table allowsColumnResizing]
    public boolean allowsColumnResizing() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsColumnResizing"));
    }

    /// [table setAllowsColumnResizing:] — whether the user may drag column widths.
    public void setAllowsColumnResizing(boolean flag) {
        try {
            H.hVoidBool().invokeExact(peer, ObjC.sel("setAllowsColumnResizing:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsColumnResizing: failed", t);
        }
    }

    /// [table rowHeight] — the height of each row in points.
    public double rowHeight() {
        try {
            return (double) H.hDouble().invokeExact(peer, ObjC.sel("rowHeight"));
        } catch (Throwable t) {
            throw new RuntimeException("rowHeight failed", t);
        }
    }

    /// [table setRowHeight:] — the height of each row in points.
    public void setRowHeight(double height) {
        try {
            H.hVoidDouble().invokeExact(peer, ObjC.sel("setRowHeight:"), height);
        } catch (Throwable t) {
            throw new RuntimeException("setRowHeight: failed", t);
        }
    }

    // ---- added for completeness ----

    /// [table selectedRow] — selected row index, -1 if none.
    public long selectedRow() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("selectedRow")); } catch (Throwable t) { throw new RuntimeException("selectedRow failed", t); }
    }

    /// [table selectedColumn] — selected column, -1 if none.
    public long selectedColumn() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("selectedColumn")); } catch (Throwable t) { throw new RuntimeException("selectedColumn failed", t); }
    }

    /// [table selectedRowIndexes] — NSIndexSet peer (id).
    public MemorySegment selectedRowIndexes() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("selectedRowIndexes")); } catch (Throwable t) { throw new RuntimeException("selectedRowIndexes failed", t); }
    }

    /// [table selectedColumnIndexes] — NSIndexSet peer.
    public MemorySegment selectedColumnIndexes() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("selectedColumnIndexes")); } catch (Throwable t) { throw new RuntimeException("selectedColumnIndexes failed", t); }
    }

    /// [table selectRowIndexes:byExtendingSelection:]
    public void selectRowIndexes(MemorySegment indexes, boolean extend) {
        try {
            MemorySegment arg = (indexes == null || indexes.address() == 0) ? MemorySegment.NULL : indexes;
            H.hVoidIdBool().invokeExact(peer, ObjC.sel("selectRowIndexes:byExtendingSelection:"), (MemorySegment) arg, extend);
        } catch (Throwable t) { throw new RuntimeException("selectRowIndexes:byExtendingSelection: failed", t); }
    }

    /// [table selectColumnIndexes:byExtendingSelection:]
    public void selectColumnIndexes(MemorySegment indexes, boolean extend) {
        try {
            MemorySegment arg = (indexes == null || indexes.address() == 0) ? MemorySegment.NULL : indexes;
            H.hVoidIdBool().invokeExact(peer, ObjC.sel("selectColumnIndexes:byExtendingSelection:"), (MemorySegment) arg, extend);
        } catch (Throwable t) { throw new RuntimeException("selectColumnIndexes:byExtendingSelection: failed", t); }
    }

    /// [table deselectRow:]
    public void deselectRow(long row) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("deselectRow:"), row); } catch (Throwable t) { throw new RuntimeException("deselectRow: failed", t); }
    }

    /// [table deselectColumn:]
    public void deselectColumn(long col) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("deselectColumn:"), col); } catch (Throwable t) { throw new RuntimeException("deselectColumn: failed", t); }
    }

    /// [table isRowSelected:]
    public boolean isRowSelected(long row) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
            return (boolean) h.invokeExact(peer, ObjC.sel("isRowSelected:"), row);
        } catch (Throwable t) { throw new RuntimeException("isRowSelected: failed", t); }
    }

    /// [table isColumnSelected:]
    public boolean isColumnSelected(long col) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
            return (boolean) h.invokeExact(peer, ObjC.sel("isColumnSelected:"), col);
        } catch (Throwable t) { throw new RuntimeException("isColumnSelected: failed", t); }
    }

    /// [table clickedRow] — row clicked last, -1 if none.
    public long clickedRow() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("clickedRow")); } catch (Throwable t) { throw new RuntimeException("clickedRow failed", t); }
    }

    /// [table clickedColumn] — column clicked last, -1 if none.
    public long clickedColumn() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("clickedColumn")); } catch (Throwable t) { throw new RuntimeException("clickedColumn failed", t); }
    }

    /// [table headerView] — NSTableHeaderView peer or null.
    public MemorySegment headerView() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("headerView")); } catch (Throwable t) { throw new RuntimeException("headerView failed", t); }
    }

    /// [table setHeaderView:]
    public void setHeaderView(MemorySegment headerView) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("setHeaderView:"), (MemorySegment) ((MemorySegment) (headerView == null ? MemorySegment.NULL : headerView))); } catch (Throwable t) { throw new RuntimeException("setHeaderView: failed", t); }
    }

    /// [table gridStyleMask] — NSTableViewGridLineStyle (bitmask).
    public long gridStyleMask() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("gridStyleMask")); } catch (Throwable t) { throw new RuntimeException("gridStyleMask failed", t); }
    }

    /// [table setGridStyleMask:]
    public void setGridStyleMask(long mask) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("setGridStyleMask:"), mask); } catch (Throwable t) { throw new RuntimeException("setGridStyleMask: failed", t); }
    }

    /// [table allowsMultipleSelection]
    public boolean allowsMultipleSelection() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsMultipleSelection"));
    }

    /// [table setAllowsMultipleSelection:]
    public void setAllowsMultipleSelection(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setAllowsMultipleSelection:"), flag); } catch (Throwable t) { throw new RuntimeException("setAllowsMultipleSelection: failed", t); }
    }

    /// [table allowsEmptySelection]
    public boolean allowsEmptySelection() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsEmptySelection"));
    }

    /// [table setAllowsEmptySelection:]
    public void setAllowsEmptySelection(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setAllowsEmptySelection:"), flag); } catch (Throwable t) { throw new RuntimeException("setAllowsEmptySelection: failed", t); }
    }

    /// [table allowsColumnSelection]
    public boolean allowsColumnSelection() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsColumnSelection"));
    }

    /// [table setAllowsColumnSelection:]
    public void setAllowsColumnSelection(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setAllowsColumnSelection:"), flag); } catch (Throwable t) { throw new RuntimeException("setAllowsColumnSelection: failed", t); }
    }

    /// [table sortDescriptors] — NSArray of NSSortDescriptor peers (id), or null.
    public MemorySegment sortDescriptors() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("sortDescriptors")); } catch (Throwable t) { throw new RuntimeException("sortDescriptors failed", t); }
    }

    /// [table setSortDescriptors:]
    public void setSortDescriptors(MemorySegment descriptors) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("setSortDescriptors:"), (MemorySegment) ((MemorySegment) (descriptors == null ? MemorySegment.NULL : descriptors))); } catch (Throwable t) { throw new RuntimeException("setSortDescriptors: failed", t); }
    }

    /// [table editedRow]
    public long editedRow() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("editedRow")); } catch (Throwable t) { throw new RuntimeException("editedRow failed", t); }
    }

    /// [table editedColumn]
    public long editedColumn() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("editedColumn")); } catch (Throwable t) { throw new RuntimeException("editedColumn failed", t); }
    }

    /// [table editColumn:row:withEvent:select:] — begin editing.
    public void editColumn(long column, long row, MemorySegment event, boolean select) {
        try { H.hEdit().invokeExact(peer, ObjC.sel("editColumn:row:withEvent:select:"), column, row, (MemorySegment) (event == null ? MemorySegment.NULL : event), select); } catch (Throwable t) { throw new RuntimeException("editColumn:row:withEvent:select: failed", t); }
    }

    /// [table scrollRowToVisible:]
    public void scrollRowToVisible(long row) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("scrollRowToVisible:"), row); } catch (Throwable t) { throw new RuntimeException("scrollRowToVisible: failed", t); }
    }

    /// [table scrollColumnToVisible:]
    public void scrollColumnToVisible(long col) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("scrollColumnToVisible:"), col); } catch (Throwable t) { throw new RuntimeException("scrollColumnToVisible: failed", t); }
    }

    /// [table selectAll:]
    public void selectAll(MemorySegment sender) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("selectAll:"), (MemorySegment) ((MemorySegment) (sender == null ? MemorySegment.NULL : sender))); } catch (Throwable t) { throw new RuntimeException("selectAll: failed", t); }
    }

    /// [table deselectAll:]
    public void deselectAll(MemorySegment sender) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("deselectAll:"), (MemorySegment) ((MemorySegment) (sender == null ? MemorySegment.NULL : sender))); } catch (Throwable t) { throw new RuntimeException("deselectAll: failed", t); }
    }
}
