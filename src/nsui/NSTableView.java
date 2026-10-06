package nsui;
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
    private record Handles(MethodHandle hVoidId, MethodHandle hVoid, MethodHandle hInt, MethodHandle hVoidBool, MethodHandle hVoidDouble, MethodHandle hDouble, MethodHandle hId, MethodHandle hVoidIdBool, MethodHandle hVoidInt, MethodHandle hEdit, MethodHandle hBoolInt, MethodHandle hIdId, MethodHandle hVoidIdId, MethodHandle hVoidIntInt, MethodHandle hIntId, MethodHandle hIdInt, MethodHandle hIdIntBool, MethodHandle hIdIdId, MethodHandle hSize, MethodHandle hVoidSize, MethodHandle hIdRect, MethodHandle hVoidIdInt) {}
    private static volatile Handles H;

    protected NSTableView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    protected static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    protected static synchronized void ensureInitLocked() {
        if (H != null) return;
        H = new Handles(
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
                ObjC.handle(Sig.of(Ret.BOOL, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)));
    }

    /// `[[NSTableView alloc] initWithFrame:frame]` — a new table view.
        public static NSTableView create(NSRect frame) {
        ensureInit();
        return new NSTableView(ObjC.newView("NSTableView", frame));
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

    // ---- batch: Tables — property and affordance coverage (all shapes already in Sig.VOCABULARY) ----
    // Omitted here (documented, reported to coordinator):
    // - rectOfColumn:/rectOfRow:/frameOfCellAtColumn:row: need Ret.RECT with INT args — no such vocabulary entry.
    // - rowsInRect: needs Ret.RANGE with Arg.RECT — no such entry.
    // - columnAtPoint:/rowAtPoint: need Ret.INT with Arg.POINT — no such entry.
    // - canDragRowsWithIndexes:atPoint: needs Ret.BOOL with (ID, POINT) — no such entry.
    // - dragImageForRowsWithIndexes:tableColumns:event:offset: needs multi-id plus an out-pointer — no such entry.
    // - setDraggingSourceOperationMask:forLocal: needs Ret.VOID with (INT, BOOL) — no such entry.
    // - viewAtColumn:row:makeIfNecessary: needs Ret.ID with (INT, INT, BOOL) — no such entry.
    // - enumerateAvailableRowViewsUsingBlock:, all NSTableViewDataSource/Delegate protocol methods — need upcall machinery.
    // - drawRow:clipRect:, highlightSelectionInClipRect:, drawGridInClipRect:, drawBackgroundInClipRect:,
    //   didAddRowView:forRow:, didRemoveRowView:forRow: — subclass override points, not direct wrappers.
    // - userInterfaceLayoutDirection — inherited from NSView (identical selectors), not redeclared.
    // - sizeToFit — inherited from NSControl (identical selector), not redeclared.
    // - Deprecated methods (selectRow:byExtendingSelection:, preparedCellAtColumn:row:, etc.) — skipped by policy.

    /// [table cornerView] — the view in the corner above the vertical scroller (NSView or nil).
    public NSView cornerView() {
        try { return NSView.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("cornerView"))); } catch (Throwable t) { throw new RuntimeException("cornerView failed", t); }
    }

    /// [table setCornerView:]
    public void setCornerView(NSView view) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("setCornerView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer())); } catch (Throwable t) { throw new RuntimeException("setCornerView: failed", t); }
    }

    /// [table allowsColumnReordering]
    public boolean allowsColumnReordering() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsColumnReordering"));
    }

    /// [table setAllowsColumnReordering:]
    public void setAllowsColumnReordering(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setAllowsColumnReordering:"), flag); } catch (Throwable t) { throw new RuntimeException("setAllowsColumnReordering: failed", t); }
    }

    /// [table columnAutoresizingStyle] — NSTableViewColumnAutoresizingStyle (raw).
    public long columnAutoresizingStyle() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("columnAutoresizingStyle")); } catch (Throwable t) { throw new RuntimeException("columnAutoresizingStyle failed", t); }
    }

    /// [table setColumnAutoresizingStyle:]
    public void setColumnAutoresizingStyle(long style) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("setColumnAutoresizingStyle:"), style); } catch (Throwable t) { throw new RuntimeException("setColumnAutoresizingStyle: failed", t); }
    }

    /// [table intercellSpacing]
    public NSSize intercellSpacing() {
        try {
            MemorySegment s = (MemorySegment) H.hSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("intercellSpacing"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("intercellSpacing failed", t); }
    }

    /// [table setIntercellSpacing:]
    public void setIntercellSpacing(NSSize spacing) {
        if (spacing == null) return;
        try { H.hVoidSize().invokeExact(peer, ObjC.sel("setIntercellSpacing:"), spacing.toSegment()); } catch (Throwable t) { throw new RuntimeException("setIntercellSpacing: failed", t); }
    }

    /// [table backgroundColor]
    public NSColor backgroundColor() {
        try { return NSColor.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("backgroundColor"))); } catch (Throwable t) { throw new RuntimeException("backgroundColor failed", t); }
    }

    /// [table setBackgroundColor:]
    public void setBackgroundColor(NSColor color) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("setBackgroundColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer())); } catch (Throwable t) { throw new RuntimeException("setBackgroundColor: failed", t); }
    }

    /// [table gridColor]
    public NSColor gridColor() {
        try { return NSColor.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("gridColor"))); } catch (Throwable t) { throw new RuntimeException("gridColor failed", t); }
    }

    /// [table setGridColor:]
    public void setGridColor(NSColor color) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("setGridColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer())); } catch (Throwable t) { throw new RuntimeException("setGridColor: failed", t); }
    }

    /// [table rowSizeStyle] — NSTableViewRowSizeStyle (raw).
    public long rowSizeStyle() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("rowSizeStyle")); } catch (Throwable t) { throw new RuntimeException("rowSizeStyle failed", t); }
    }

    /// [table setRowSizeStyle:]
    public void setRowSizeStyle(long style) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("setRowSizeStyle:"), style); } catch (Throwable t) { throw new RuntimeException("setRowSizeStyle: failed", t); }
    }

    /// [table effectiveRowSizeStyle] — resolved style (raw).
    public long effectiveRowSizeStyle() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("effectiveRowSizeStyle")); } catch (Throwable t) { throw new RuntimeException("effectiveRowSizeStyle failed", t); }
    }

    /// [table noteHeightOfRowsWithIndexesChanged:] — re-tile using fresh delegate heights.
    public void noteHeightOfRowsWithIndexesChanged(MemorySegment indexes) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("noteHeightOfRowsWithIndexesChanged:"), (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes)); } catch (Throwable t) { throw new RuntimeException("noteHeightOfRowsWithIndexesChanged: failed", t); }
    }

    /// [table tableColumns] — NSArray of NSTableColumn peers (id).
    public MemorySegment tableColumns() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("tableColumns")); } catch (Throwable t) { throw new RuntimeException("tableColumns failed", t); }
    }

    /// [table moveColumn:toColumn:]
    public void moveColumn(long oldIndex, long newIndex) {
        try { H.hVoidIntInt().invokeExact(peer, ObjC.sel("moveColumn:toColumn:"), oldIndex, newIndex); } catch (Throwable t) { throw new RuntimeException("moveColumn:toColumn: failed", t); }
    }

    /// [table columnWithIdentifier:] — index or -1.
    public long columnWithIdentifier(String identifier) {
        try { return (long) H.hIntId().invokeExact(peer, ObjC.sel("columnWithIdentifier:"), (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier))); } catch (Throwable t) { throw new RuntimeException("columnWithIdentifier: failed", t); }
    }

    /// [table tableColumnWithIdentifier:] — NSTableColumn or nil.
    public NSTableColumn tableColumnWithIdentifier(String identifier) {
        try { return NSTableColumn.wrap((MemorySegment) H.hIdId().invokeExact(peer, ObjC.sel("tableColumnWithIdentifier:"), (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)))); } catch (Throwable t) { throw new RuntimeException("tableColumnWithIdentifier: failed", t); }
    }

    /// [table tile] — size to fit content.
    public void tile() {
        try { H.hVoid().invokeExact(peer, ObjC.sel("tile")); } catch (Throwable t) { throw new RuntimeException("tile failed", t); }
    }

    /// [table sizeLastColumnToFit]
    public void sizeLastColumnToFit() {
        try { H.hVoid().invokeExact(peer, ObjC.sel("sizeLastColumnToFit")); } catch (Throwable t) { throw new RuntimeException("sizeLastColumnToFit failed", t); }
    }

    /// [table noteNumberOfRowsChanged]
    public void noteNumberOfRowsChanged() {
        try { H.hVoid().invokeExact(peer, ObjC.sel("noteNumberOfRowsChanged")); } catch (Throwable t) { throw new RuntimeException("noteNumberOfRowsChanged failed", t); }
    }

    /// [table reloadDataForRowIndexes:columnIndexes:]
    public void reloadDataForRowIndexes(MemorySegment rows, MemorySegment columns) {
        try {
            H.hVoidIdId().invokeExact(peer, ObjC.sel("reloadDataForRowIndexes:columnIndexes:"),
                    (MemorySegment) (rows == null ? MemorySegment.NULL : rows),
                    (MemorySegment) (columns == null ? MemorySegment.NULL : columns));
        } catch (Throwable t) { throw new RuntimeException("reloadDataForRowIndexes:columnIndexes: failed", t); }
    }

    /// [table doubleAction] — SEL id or nil.
    public MemorySegment doubleAction() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("doubleAction")); } catch (Throwable t) { throw new RuntimeException("doubleAction failed", t); }
    }

    /// [table setDoubleAction:] — SEL for double-click (selector name, nil clears).
    public void setDoubleAction(String selector) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("setDoubleAction:"), (MemorySegment) (selector == null ? MemorySegment.NULL : ObjC.sel(selector))); } catch (Throwable t) { throw new RuntimeException("setDoubleAction: failed", t); }
    }

    /// [table setIndicatorImage:inTableColumn:]
    public void setIndicatorImage(NSImage image, NSTableColumn column) {
        try {
            H.hVoidIdId().invokeExact(peer, ObjC.sel("setIndicatorImage:inTableColumn:"),
                    (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()),
                    (MemorySegment) (column == null ? MemorySegment.NULL : column.peer()));
        } catch (Throwable t) { throw new RuntimeException("setIndicatorImage:inTableColumn: failed", t); }
    }

    /// [table indicatorImageInTableColumn:] — NSImage or nil.
    public NSImage indicatorImageInTableColumn(NSTableColumn column) {
        try { return NSImage.wrap((MemorySegment) H.hIdId().invokeExact(peer, ObjC.sel("indicatorImageInTableColumn:"), (MemorySegment) (column == null ? MemorySegment.NULL : column.peer()))); } catch (Throwable t) { throw new RuntimeException("indicatorImageInTableColumn: failed", t); }
    }

    /// [table highlightedTableColumn]
    public NSTableColumn highlightedTableColumn() {
        try { return NSTableColumn.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("highlightedTableColumn"))); } catch (Throwable t) { throw new RuntimeException("highlightedTableColumn failed", t); }
    }

    /// [table setHighlightedTableColumn:]
    public void setHighlightedTableColumn(NSTableColumn column) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("setHighlightedTableColumn:"), (MemorySegment) (column == null ? MemorySegment.NULL : column.peer())); } catch (Throwable t) { throw new RuntimeException("setHighlightedTableColumn: failed", t); }
    }

    /// [table verticalMotionCanBeginDrag]
    public boolean verticalMotionCanBeginDrag() {
        return ObjC.msgSendBool(peer, ObjC.sel("verticalMotionCanBeginDrag"));
    }

    /// [table setVerticalMotionCanBeginDrag:]
    public void setVerticalMotionCanBeginDrag(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setVerticalMotionCanBeginDrag:"), flag); } catch (Throwable t) { throw new RuntimeException("setVerticalMotionCanBeginDrag: failed", t); }
    }

    /// [table setDropRow:dropOperation:] — re-target a proposed drop.
    public void setDropRow(long row, long operation) {
        try { H.hVoidIntInt().invokeExact(peer, ObjC.sel("setDropRow:dropOperation:"), row, operation); } catch (Throwable t) { throw new RuntimeException("setDropRow:dropOperation: failed", t); }
    }

    /// [table numberOfSelectedColumns]
    public long numberOfSelectedColumns() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("numberOfSelectedColumns")); } catch (Throwable t) { throw new RuntimeException("numberOfSelectedColumns failed", t); }
    }

    /// [table numberOfSelectedRows]
    public long numberOfSelectedRows() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("numberOfSelectedRows")); } catch (Throwable t) { throw new RuntimeException("numberOfSelectedRows failed", t); }
    }

    /// [table allowsTypeSelect]
    public boolean allowsTypeSelect() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsTypeSelect"));
    }

    /// [table setAllowsTypeSelect:]
    public void setAllowsTypeSelect(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setAllowsTypeSelect:"), flag); } catch (Throwable t) { throw new RuntimeException("setAllowsTypeSelect: failed", t); }
    }

    /// [table style] — NSTableViewStyle (raw).
    public long style() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("style")); } catch (Throwable t) { throw new RuntimeException("style failed", t); }
    }

    /// [table setStyle:]
    public void setStyle(long style) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("setStyle:"), style); } catch (Throwable t) { throw new RuntimeException("setStyle: failed", t); }
    }

    /// [table effectiveStyle] — resolved style (raw).
    public long effectiveStyle() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("effectiveStyle")); } catch (Throwable t) { throw new RuntimeException("effectiveStyle failed", t); }
    }

    /// [table selectionHighlightStyle] — NSTableViewSelectionHighlightStyle (raw).
    public long selectionHighlightStyle() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("selectionHighlightStyle")); } catch (Throwable t) { throw new RuntimeException("selectionHighlightStyle failed", t); }
    }

    /// [table setSelectionHighlightStyle:]
    public void setSelectionHighlightStyle(long style) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("setSelectionHighlightStyle:"), style); } catch (Throwable t) { throw new RuntimeException("setSelectionHighlightStyle: failed", t); }
    }

    /// [table draggingDestinationFeedbackStyle] — NSTableViewDraggingDestinationFeedbackStyle (raw).
    public long draggingDestinationFeedbackStyle() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("draggingDestinationFeedbackStyle")); } catch (Throwable t) { throw new RuntimeException("draggingDestinationFeedbackStyle failed", t); }
    }

    /// [table setDraggingDestinationFeedbackStyle:]
    public void setDraggingDestinationFeedbackStyle(long style) {
        try { H.hVoidInt().invokeExact(peer, ObjC.sel("setDraggingDestinationFeedbackStyle:"), style); } catch (Throwable t) { throw new RuntimeException("setDraggingDestinationFeedbackStyle: failed", t); }
    }

    /// [table columnIndexesInRect:] — NSIndexSet peer (id).
    public MemorySegment columnIndexesInRect(NSRect rect) {
        if (rect == null) return MemorySegment.NULL;
        try { return (MemorySegment) H.hIdRect().invokeExact(peer, ObjC.sel("columnIndexesInRect:"), rect.toSegment()); } catch (Throwable t) { throw new RuntimeException("columnIndexesInRect: failed", t); }
    }

    /// [table autosaveName] — persistence name or nil.
    public String autosaveName() {
        try { return ObjC.toString((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("autosaveName"))); } catch (Throwable t) { throw new RuntimeException("autosaveName failed", t); }
    }

    /// [table setAutosaveName:] — nil removes persistence data for the previous name.
    public void setAutosaveName(String name) {
        try { H.hVoidId().invokeExact(peer, ObjC.sel("setAutosaveName:"), (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name))); } catch (Throwable t) { throw new RuntimeException("setAutosaveName: failed", t); }
    }

    /// [table autosaveTableColumns]
    public boolean autosaveTableColumns() {
        return ObjC.msgSendBool(peer, ObjC.sel("autosaveTableColumns"));
    }

    /// [table setAutosaveTableColumns:]
    public void setAutosaveTableColumns(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setAutosaveTableColumns:"), flag); } catch (Throwable t) { throw new RuntimeException("setAutosaveTableColumns: failed", t); }
    }

    /// [table rowViewAtRow:makeIfNecessary:] — NSTableRowView or nil.
    public NSTableRowView rowViewAtRow(long row, boolean makeIfNecessary) {
        try { return NSTableRowView.wrap((MemorySegment) H.hIdIntBool().invokeExact(peer, ObjC.sel("rowViewAtRow:makeIfNecessary:"), row, makeIfNecessary)); } catch (Throwable t) { throw new RuntimeException("rowViewAtRow:makeIfNecessary: failed", t); }
    }

    /// [table rowForView:] — row or -1.
    public long rowForView(NSView view) {
        try { return (long) H.hIntId().invokeExact(peer, ObjC.sel("rowForView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer())); } catch (Throwable t) { throw new RuntimeException("rowForView: failed", t); }
    }

    /// [table columnForView:] — column or -1.
    public long columnForView(NSView view) {
        try { return (long) H.hIntId().invokeExact(peer, ObjC.sel("columnForView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer())); } catch (Throwable t) { throw new RuntimeException("columnForView: failed", t); }
    }

    /// [table makeViewWithIdentifier:owner:] — reusable NSView or nil.
    public NSView makeViewWithIdentifier(String identifier, MemorySegment owner) {
        try {
            return NSView.wrap((MemorySegment) H.hIdIdId().invokeExact(peer, ObjC.sel("makeViewWithIdentifier:owner:"),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)),
                    (MemorySegment) (owner == null ? MemorySegment.NULL : owner)));
        } catch (Throwable t) { throw new RuntimeException("makeViewWithIdentifier:owner: failed", t); }
    }

    /// [table floatsGroupRows]
    public boolean floatsGroupRows() {
        return ObjC.msgSendBool(peer, ObjC.sel("floatsGroupRows"));
    }

    /// [table setFloatsGroupRows:]
    public void setFloatsGroupRows(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setFloatsGroupRows:"), flag); } catch (Throwable t) { throw new RuntimeException("setFloatsGroupRows: failed", t); }
    }

    /// [table rowActionsVisible] — setting YES throws; setting NO hides.
    public boolean rowActionsVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("rowActionsVisible"));
    }

    /// [table setRowActionsVisible:] — only NO is supported (YES throws).
    public void setRowActionsVisible(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setRowActionsVisible:"), flag); } catch (Throwable t) { throw new RuntimeException("setRowActionsVisible: failed", t); }
    }

    /// [table beginUpdates] — open an animated row-change group.
    public void beginUpdates() {
        try { H.hVoid().invokeExact(peer, ObjC.sel("beginUpdates")); } catch (Throwable t) { throw new RuntimeException("beginUpdates failed", t); }
    }

    /// [table endUpdates] — close the group.
    public void endUpdates() {
        try { H.hVoid().invokeExact(peer, ObjC.sel("endUpdates")); } catch (Throwable t) { throw new RuntimeException("endUpdates failed", t); }
    }

    /// [table insertRowsAtIndexes:withAnimation:]
    public void insertRowsAtIndexes(MemorySegment indexes, long animation) {
        try { H.hVoidIdInt().invokeExact(peer, ObjC.sel("insertRowsAtIndexes:withAnimation:"), (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes), animation); } catch (Throwable t) { throw new RuntimeException("insertRowsAtIndexes:withAnimation: failed", t); }
    }

    /// [table removeRowsAtIndexes:withAnimation:]
    public void removeRowsAtIndexes(MemorySegment indexes, long animation) {
        try { H.hVoidIdInt().invokeExact(peer, ObjC.sel("removeRowsAtIndexes:withAnimation:"), (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes), animation); } catch (Throwable t) { throw new RuntimeException("removeRowsAtIndexes:withAnimation: failed", t); }
    }

    /// [table moveRowAtIndex:toIndex:]
    public void moveRowAtIndex(long oldIndex, long newIndex) {
        try { H.hVoidIntInt().invokeExact(peer, ObjC.sel("moveRowAtIndex:toIndex:"), oldIndex, newIndex); } catch (Throwable t) { throw new RuntimeException("moveRowAtIndex:toIndex: failed", t); }
    }

    /// [table hideRowsAtIndexes:withAnimation:]
    public void hideRowsAtIndexes(MemorySegment indexes, long animation) {
        try { H.hVoidIdInt().invokeExact(peer, ObjC.sel("hideRowsAtIndexes:withAnimation:"), (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes), animation); } catch (Throwable t) { throw new RuntimeException("hideRowsAtIndexes:withAnimation: failed", t); }
    }

    /// [table unhideRowsAtIndexes:withAnimation:]
    public void unhideRowsAtIndexes(MemorySegment indexes, long animation) {
        try { H.hVoidIdInt().invokeExact(peer, ObjC.sel("unhideRowsAtIndexes:withAnimation:"), (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes), animation); } catch (Throwable t) { throw new RuntimeException("unhideRowsAtIndexes:withAnimation: failed", t); }
    }

    /// [table hiddenRowIndexes] — NSIndexSet peer (id).
    public MemorySegment hiddenRowIndexes() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("hiddenRowIndexes")); } catch (Throwable t) { throw new RuntimeException("hiddenRowIndexes failed", t); }
    }

    /// [table registerNib:forIdentifier:] — associate a nib (or nil to remove) with an identifier.
    public void registerNib(MemorySegment nib, String identifier) {
        try {
            H.hVoidIdId().invokeExact(peer, ObjC.sel("registerNib:forIdentifier:"),
                    (MemorySegment) (nib == null ? MemorySegment.NULL : nib),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)));
        } catch (Throwable t) { throw new RuntimeException("registerNib:forIdentifier: failed", t); }
    }

    /// [table registeredNibsByIdentifier] — NSDictionary peer or nil.
    public MemorySegment registeredNibsByIdentifier() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("registeredNibsByIdentifier")); } catch (Throwable t) { throw new RuntimeException("registeredNibsByIdentifier failed", t); }
    }

    /// [table usesStaticContents]
    public boolean usesStaticContents() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesStaticContents"));
    }

    /// [table setUsesStaticContents:]
    public void setUsesStaticContents(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setUsesStaticContents:"), flag); } catch (Throwable t) { throw new RuntimeException("setUsesStaticContents: failed", t); }
    }

    /// [table usesAutomaticRowHeights]
    public boolean usesAutomaticRowHeights() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesAutomaticRowHeights"));
    }

    /// [table setUsesAutomaticRowHeights:]
    public void setUsesAutomaticRowHeights(boolean flag) {
        try { H.hVoidBool().invokeExact(peer, ObjC.sel("setUsesAutomaticRowHeights:"), flag); } catch (Throwable t) { throw new RuntimeException("setUsesAutomaticRowHeights: failed", t); }
    }

}
