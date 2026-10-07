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

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment addTableColumn;
        static MemorySegment removeTableColumn;
        static MemorySegment setDataSource;
        static MemorySegment dataSource;
        static MemorySegment setDelegate;
        static MemorySegment delegate;
        static MemorySegment reloadData;
        static MemorySegment numberOfRows;
        static MemorySegment numberOfColumns;
        static MemorySegment usesAlternatingRowBackgroundColors;
        static MemorySegment setUsesAlternatingRowBackgroundColors;
        static MemorySegment allowsColumnResizing;
        static MemorySegment setAllowsColumnResizing;
        static MemorySegment rowHeight;
        static MemorySegment setRowHeight;
        static MemorySegment selectedRow;
        static MemorySegment selectedColumn;
        static MemorySegment selectedRowIndexes;
        static MemorySegment selectedColumnIndexes;
        static MemorySegment selectRowIndexes_byExtendingSelection;
        static MemorySegment selectColumnIndexes_byExtendingSelection;
        static MemorySegment deselectRow;
        static MemorySegment deselectColumn;
        static MemorySegment isRowSelected;
        static MemorySegment isColumnSelected;
        static MemorySegment clickedRow;
        static MemorySegment clickedColumn;
        static MemorySegment headerView;
        static MemorySegment setHeaderView;
        static MemorySegment gridStyleMask;
        static MemorySegment setGridStyleMask;
        static MemorySegment allowsMultipleSelection;
        static MemorySegment setAllowsMultipleSelection;
        static MemorySegment allowsEmptySelection;
        static MemorySegment setAllowsEmptySelection;
        static MemorySegment allowsColumnSelection;
        static MemorySegment setAllowsColumnSelection;
        static MemorySegment sortDescriptors;
        static MemorySegment setSortDescriptors;
        static MemorySegment editedRow;
        static MemorySegment editedColumn;
        static MemorySegment editColumn_row_withEvent_select;
        static MemorySegment scrollRowToVisible;
        static MemorySegment scrollColumnToVisible;
        static MemorySegment selectAll;
        static MemorySegment deselectAll;
        static MemorySegment cornerView;
        static MemorySegment setCornerView;
        static MemorySegment allowsColumnReordering;
        static MemorySegment setAllowsColumnReordering;
        static MemorySegment columnAutoresizingStyle;
        static MemorySegment setColumnAutoresizingStyle;
        static MemorySegment intercellSpacing;
        static MemorySegment setIntercellSpacing;
        static MemorySegment backgroundColor;
        static MemorySegment setBackgroundColor;
        static MemorySegment gridColor;
        static MemorySegment setGridColor;
        static MemorySegment rowSizeStyle;
        static MemorySegment setRowSizeStyle;
        static MemorySegment effectiveRowSizeStyle;
        static MemorySegment noteHeightOfRowsWithIndexesChanged;
        static MemorySegment tableColumns;
        static MemorySegment moveColumn_toColumn;
        static MemorySegment columnWithIdentifier;
        static MemorySegment tableColumnWithIdentifier;
        static MemorySegment tile;
        static MemorySegment sizeLastColumnToFit;
        static MemorySegment noteNumberOfRowsChanged;
        static MemorySegment reloadDataForRowIndexes_columnIndexes;
        static MemorySegment doubleAction;
        static MemorySegment setDoubleAction;
        static MemorySegment setIndicatorImage_inTableColumn;
        static MemorySegment indicatorImageInTableColumn;
        static MemorySegment highlightedTableColumn;
        static MemorySegment setHighlightedTableColumn;
        static MemorySegment verticalMotionCanBeginDrag;
        static MemorySegment setVerticalMotionCanBeginDrag;
        static MemorySegment setDropRow_dropOperation;
        static MemorySegment numberOfSelectedColumns;
        static MemorySegment numberOfSelectedRows;
        static MemorySegment allowsTypeSelect;
        static MemorySegment setAllowsTypeSelect;
        static MemorySegment style;
        static MemorySegment setStyle;
        static MemorySegment effectiveStyle;
        static MemorySegment selectionHighlightStyle;
        static MemorySegment setSelectionHighlightStyle;
        static MemorySegment draggingDestinationFeedbackStyle;
        static MemorySegment setDraggingDestinationFeedbackStyle;
        static MemorySegment columnIndexesInRect;
        static MemorySegment autosaveName;
        static MemorySegment setAutosaveName;
        static MemorySegment autosaveTableColumns;
        static MemorySegment setAutosaveTableColumns;
        static MemorySegment rowViewAtRow_makeIfNecessary;
        static MemorySegment rowForView;
        static MemorySegment columnForView;
        static MemorySegment makeViewWithIdentifier_owner;
        static MemorySegment floatsGroupRows;
        static MemorySegment setFloatsGroupRows;
        static MemorySegment rowActionsVisible;
        static MemorySegment setRowActionsVisible;
        static MemorySegment beginUpdates;
        static MemorySegment endUpdates;
        static MemorySegment insertRowsAtIndexes_withAnimation;
        static MemorySegment removeRowsAtIndexes_withAnimation;
        static MemorySegment moveRowAtIndex_toIndex;
        static MemorySegment hideRowsAtIndexes_withAnimation;
        static MemorySegment unhideRowsAtIndexes_withAnimation;
        static MemorySegment hiddenRowIndexes;
        static MemorySegment registerNib_forIdentifier;
        static MemorySegment registeredNibsByIdentifier;
        static MemorySegment usesStaticContents;
        static MemorySegment setUsesStaticContents;
        static MemorySegment usesAutomaticRowHeights;
        static MemorySegment setUsesAutomaticRowHeights;
        static void populate() {
            addTableColumn = ObjC.sel("addTableColumn:");
            removeTableColumn = ObjC.sel("removeTableColumn:");
            setDataSource = ObjC.sel("setDataSource:");
            dataSource = ObjC.sel("dataSource");
            setDelegate = ObjC.sel("setDelegate:");
            delegate = ObjC.sel("delegate");
            reloadData = ObjC.sel("reloadData");
            numberOfRows = ObjC.sel("numberOfRows");
            numberOfColumns = ObjC.sel("numberOfColumns");
            usesAlternatingRowBackgroundColors = ObjC.sel("usesAlternatingRowBackgroundColors");
            setUsesAlternatingRowBackgroundColors = ObjC.sel("setUsesAlternatingRowBackgroundColors:");
            allowsColumnResizing = ObjC.sel("allowsColumnResizing");
            setAllowsColumnResizing = ObjC.sel("setAllowsColumnResizing:");
            rowHeight = ObjC.sel("rowHeight");
            setRowHeight = ObjC.sel("setRowHeight:");
            selectedRow = ObjC.sel("selectedRow");
            selectedColumn = ObjC.sel("selectedColumn");
            selectedRowIndexes = ObjC.sel("selectedRowIndexes");
            selectedColumnIndexes = ObjC.sel("selectedColumnIndexes");
            selectRowIndexes_byExtendingSelection = ObjC.sel("selectRowIndexes:byExtendingSelection:");
            selectColumnIndexes_byExtendingSelection = ObjC.sel("selectColumnIndexes:byExtendingSelection:");
            deselectRow = ObjC.sel("deselectRow:");
            deselectColumn = ObjC.sel("deselectColumn:");
            isRowSelected = ObjC.sel("isRowSelected:");
            isColumnSelected = ObjC.sel("isColumnSelected:");
            clickedRow = ObjC.sel("clickedRow");
            clickedColumn = ObjC.sel("clickedColumn");
            headerView = ObjC.sel("headerView");
            setHeaderView = ObjC.sel("setHeaderView:");
            gridStyleMask = ObjC.sel("gridStyleMask");
            setGridStyleMask = ObjC.sel("setGridStyleMask:");
            allowsMultipleSelection = ObjC.sel("allowsMultipleSelection");
            setAllowsMultipleSelection = ObjC.sel("setAllowsMultipleSelection:");
            allowsEmptySelection = ObjC.sel("allowsEmptySelection");
            setAllowsEmptySelection = ObjC.sel("setAllowsEmptySelection:");
            allowsColumnSelection = ObjC.sel("allowsColumnSelection");
            setAllowsColumnSelection = ObjC.sel("setAllowsColumnSelection:");
            sortDescriptors = ObjC.sel("sortDescriptors");
            setSortDescriptors = ObjC.sel("setSortDescriptors:");
            editedRow = ObjC.sel("editedRow");
            editedColumn = ObjC.sel("editedColumn");
            editColumn_row_withEvent_select = ObjC.sel("editColumn:row:withEvent:select:");
            scrollRowToVisible = ObjC.sel("scrollRowToVisible:");
            scrollColumnToVisible = ObjC.sel("scrollColumnToVisible:");
            selectAll = ObjC.sel("selectAll:");
            deselectAll = ObjC.sel("deselectAll:");
            cornerView = ObjC.sel("cornerView");
            setCornerView = ObjC.sel("setCornerView:");
            allowsColumnReordering = ObjC.sel("allowsColumnReordering");
            setAllowsColumnReordering = ObjC.sel("setAllowsColumnReordering:");
            columnAutoresizingStyle = ObjC.sel("columnAutoresizingStyle");
            setColumnAutoresizingStyle = ObjC.sel("setColumnAutoresizingStyle:");
            intercellSpacing = ObjC.sel("intercellSpacing");
            setIntercellSpacing = ObjC.sel("setIntercellSpacing:");
            backgroundColor = ObjC.sel("backgroundColor");
            setBackgroundColor = ObjC.sel("setBackgroundColor:");
            gridColor = ObjC.sel("gridColor");
            setGridColor = ObjC.sel("setGridColor:");
            rowSizeStyle = ObjC.sel("rowSizeStyle");
            setRowSizeStyle = ObjC.sel("setRowSizeStyle:");
            effectiveRowSizeStyle = ObjC.sel("effectiveRowSizeStyle");
            noteHeightOfRowsWithIndexesChanged = ObjC.sel("noteHeightOfRowsWithIndexesChanged:");
            tableColumns = ObjC.sel("tableColumns");
            moveColumn_toColumn = ObjC.sel("moveColumn:toColumn:");
            columnWithIdentifier = ObjC.sel("columnWithIdentifier:");
            tableColumnWithIdentifier = ObjC.sel("tableColumnWithIdentifier:");
            tile = ObjC.sel("tile");
            sizeLastColumnToFit = ObjC.sel("sizeLastColumnToFit");
            noteNumberOfRowsChanged = ObjC.sel("noteNumberOfRowsChanged");
            reloadDataForRowIndexes_columnIndexes = ObjC.sel("reloadDataForRowIndexes:columnIndexes:");
            doubleAction = ObjC.sel("doubleAction");
            setDoubleAction = ObjC.sel("setDoubleAction:");
            setIndicatorImage_inTableColumn = ObjC.sel("setIndicatorImage:inTableColumn:");
            indicatorImageInTableColumn = ObjC.sel("indicatorImageInTableColumn:");
            highlightedTableColumn = ObjC.sel("highlightedTableColumn");
            setHighlightedTableColumn = ObjC.sel("setHighlightedTableColumn:");
            verticalMotionCanBeginDrag = ObjC.sel("verticalMotionCanBeginDrag");
            setVerticalMotionCanBeginDrag = ObjC.sel("setVerticalMotionCanBeginDrag:");
            setDropRow_dropOperation = ObjC.sel("setDropRow:dropOperation:");
            numberOfSelectedColumns = ObjC.sel("numberOfSelectedColumns");
            numberOfSelectedRows = ObjC.sel("numberOfSelectedRows");
            allowsTypeSelect = ObjC.sel("allowsTypeSelect");
            setAllowsTypeSelect = ObjC.sel("setAllowsTypeSelect:");
            style = ObjC.sel("style");
            setStyle = ObjC.sel("setStyle:");
            effectiveStyle = ObjC.sel("effectiveStyle");
            selectionHighlightStyle = ObjC.sel("selectionHighlightStyle");
            setSelectionHighlightStyle = ObjC.sel("setSelectionHighlightStyle:");
            draggingDestinationFeedbackStyle = ObjC.sel("draggingDestinationFeedbackStyle");
            setDraggingDestinationFeedbackStyle = ObjC.sel("setDraggingDestinationFeedbackStyle:");
            columnIndexesInRect = ObjC.sel("columnIndexesInRect:");
            autosaveName = ObjC.sel("autosaveName");
            setAutosaveName = ObjC.sel("setAutosaveName:");
            autosaveTableColumns = ObjC.sel("autosaveTableColumns");
            setAutosaveTableColumns = ObjC.sel("setAutosaveTableColumns:");
            rowViewAtRow_makeIfNecessary = ObjC.sel("rowViewAtRow:makeIfNecessary:");
            rowForView = ObjC.sel("rowForView:");
            columnForView = ObjC.sel("columnForView:");
            makeViewWithIdentifier_owner = ObjC.sel("makeViewWithIdentifier:owner:");
            floatsGroupRows = ObjC.sel("floatsGroupRows");
            setFloatsGroupRows = ObjC.sel("setFloatsGroupRows:");
            rowActionsVisible = ObjC.sel("rowActionsVisible");
            setRowActionsVisible = ObjC.sel("setRowActionsVisible:");
            beginUpdates = ObjC.sel("beginUpdates");
            endUpdates = ObjC.sel("endUpdates");
            insertRowsAtIndexes_withAnimation = ObjC.sel("insertRowsAtIndexes:withAnimation:");
            removeRowsAtIndexes_withAnimation = ObjC.sel("removeRowsAtIndexes:withAnimation:");
            moveRowAtIndex_toIndex = ObjC.sel("moveRowAtIndex:toIndex:");
            hideRowsAtIndexes_withAnimation = ObjC.sel("hideRowsAtIndexes:withAnimation:");
            unhideRowsAtIndexes_withAnimation = ObjC.sel("unhideRowsAtIndexes:withAnimation:");
            hiddenRowIndexes = ObjC.sel("hiddenRowIndexes");
            registerNib_forIdentifier = ObjC.sel("registerNib:forIdentifier:");
            registeredNibsByIdentifier = ObjC.sel("registeredNibsByIdentifier");
            usesStaticContents = ObjC.sel("usesStaticContents");
            setUsesStaticContents = ObjC.sel("setUsesStaticContents:");
            usesAutomaticRowHeights = ObjC.sel("usesAutomaticRowHeights");
            setUsesAutomaticRowHeights = ObjC.sel("setUsesAutomaticRowHeights:");
        }
    }

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
        Handles h = new Handles(
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
            Sels.populate();
        H = h;
}

    /// `[[NSTableView alloc] initWithFrame:frame]` — a new table view.
        public static NSTableView create(NSRect frame) {
        ensureInit();
        return new NSTableView(ObjC.newView("NSTableView", frame));
    }

    // ---------------------------------------------------------------- instance API

    /// [table addTableColumn:] — append a column.
    public void addTableColumn(NSTableColumn column) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, Sels.addTableColumn, column.peer());
        } catch (Throwable t) {
            throw new RuntimeException("addTableColumn: failed", t);
        }
    }

    /// [table removeTableColumn:] — remove a column.
    public void removeTableColumn(NSTableColumn column) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, Sels.removeTableColumn, column.peer());
        } catch (Throwable t) {
            throw new RuntimeException("removeTableColumn: failed", t);
        }
    }

    /// [table setDataSource:] — the object answering row-count / cell-value queries.
    public void setDataSource(MemorySegment dataSource) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, Sels.setDataSource, dataSource);
        } catch (Throwable t) {
            throw new RuntimeException("setDataSource: failed", t);
        }
    }

    /// [table dataSource]
    public MemorySegment dataSource() {
        ensureInit();
        try {
            return (MemorySegment) H.hId().invokeExact(peer, Sels.dataSource);
        } catch (Throwable t) {
            throw new RuntimeException("dataSource failed", t);
        }
    }

    /// [table setDelegate:] — the object notified of table events.
    public void setDelegate(MemorySegment delegate) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, Sels.setDelegate, delegate);
        } catch (Throwable t) {
            throw new RuntimeException("setDelegate: failed", t);
        }
    }

    /// [table delegate]
    public MemorySegment delegate() {
        ensureInit();
        try {
            return (MemorySegment) H.hId().invokeExact(peer, Sels.delegate);
        } catch (Throwable t) {
            throw new RuntimeException("delegate failed", t);
        }
    }

    /// [table reloadData] — force the table to re-query its data source.
    public void reloadData() {
        ensureInit();
        try {
            H.hVoid().invokeExact(peer, Sels.reloadData);
        } catch (Throwable t) {
            throw new RuntimeException("reloadData failed", t);
        }
    }

    /// [table numberOfRows] — the current number of rows the table is displaying.
    public long numberOfRows() {
        ensureInit();
        try {
            return (long) H.hInt().invokeExact(peer, Sels.numberOfRows);
        } catch (Throwable t) {
            throw new RuntimeException("numberOfRows failed", t);
        }
    }

    /// [table numberOfColumns]
    public long numberOfColumns() {
        ensureInit();
        try {
            return (long) H.hInt().invokeExact(peer, Sels.numberOfColumns);
        } catch (Throwable t) {
            throw new RuntimeException("numberOfColumns failed", t);
        }
    }

    /// [table usesAlternatingRowBackgroundColors]
    public boolean usesAlternatingRowBackgroundColors() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesAlternatingRowBackgroundColors);
    }

    /// [table setUsesAlternatingRowBackgroundColors:] — zebra stripes while drawing.
    public void setUsesAlternatingRowBackgroundColors(boolean flag) {
        ensureInit();
        try {
            H.hVoidBool().invokeExact(peer, Sels.setUsesAlternatingRowBackgroundColors, flag);
        } catch (Throwable t) {
            throw new RuntimeException("setUsesAlternatingRowBackgroundColors: failed", t);
        }
    }

    /// [table allowsColumnResizing]
    public boolean allowsColumnResizing() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsColumnResizing);
    }

    /// [table setAllowsColumnResizing:] — whether the user may drag column widths.
    public void setAllowsColumnResizing(boolean flag) {
        ensureInit();
        try {
            H.hVoidBool().invokeExact(peer, Sels.setAllowsColumnResizing, flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsColumnResizing: failed", t);
        }
    }

    /// [table rowHeight] — the height of each row in points.
    public double rowHeight() {
        ensureInit();
        try {
            return (double) H.hDouble().invokeExact(peer, Sels.rowHeight);
        } catch (Throwable t) {
            throw new RuntimeException("rowHeight failed", t);
        }
    }

    /// [table setRowHeight:] — the height of each row in points.
    public void setRowHeight(double height) {
        ensureInit();
        try {
            H.hVoidDouble().invokeExact(peer, Sels.setRowHeight, height);
        } catch (Throwable t) {
            throw new RuntimeException("setRowHeight: failed", t);
        }
    }

    // ---- added for completeness ----

    /// [table selectedRow] — selected row index, -1 if none.
    public long selectedRow() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.selectedRow); } catch (Throwable t) { throw new RuntimeException("selectedRow failed", t); }
    }

    /// [table selectedColumn] — selected column, -1 if none.
    public long selectedColumn() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.selectedColumn); } catch (Throwable t) { throw new RuntimeException("selectedColumn failed", t); }
    }

    /// [table selectedRowIndexes] — NSIndexSet peer (id).
    public MemorySegment selectedRowIndexes() {
        ensureInit();
        try { return (MemorySegment) H.hId().invokeExact(peer, Sels.selectedRowIndexes); } catch (Throwable t) { throw new RuntimeException("selectedRowIndexes failed", t); }
    }

    /// [table selectedColumnIndexes] — NSIndexSet peer.
    public MemorySegment selectedColumnIndexes() {
        ensureInit();
        try { return (MemorySegment) H.hId().invokeExact(peer, Sels.selectedColumnIndexes); } catch (Throwable t) { throw new RuntimeException("selectedColumnIndexes failed", t); }
    }

    /// [table selectRowIndexes:byExtendingSelection:]
    public void selectRowIndexes(MemorySegment indexes, boolean extend) {
        ensureInit();
        try {
            MemorySegment arg = (indexes == null || indexes.address() == 0) ? MemorySegment.NULL : indexes;
            H.hVoidIdBool().invokeExact(peer, Sels.selectRowIndexes_byExtendingSelection, (MemorySegment) arg, extend);
        } catch (Throwable t) { throw new RuntimeException("selectRowIndexes:byExtendingSelection: failed", t); }
    }

    /// [table selectColumnIndexes:byExtendingSelection:]
    public void selectColumnIndexes(MemorySegment indexes, boolean extend) {
        ensureInit();
        try {
            MemorySegment arg = (indexes == null || indexes.address() == 0) ? MemorySegment.NULL : indexes;
            H.hVoidIdBool().invokeExact(peer, Sels.selectColumnIndexes_byExtendingSelection, (MemorySegment) arg, extend);
        } catch (Throwable t) { throw new RuntimeException("selectColumnIndexes:byExtendingSelection: failed", t); }
    }

    /// [table deselectRow:]
    public void deselectRow(long row) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.deselectRow, row); } catch (Throwable t) { throw new RuntimeException("deselectRow: failed", t); }
    }

    /// [table deselectColumn:]
    public void deselectColumn(long col) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.deselectColumn, col); } catch (Throwable t) { throw new RuntimeException("deselectColumn: failed", t); }
    }

    /// [table isRowSelected:]
    public boolean isRowSelected(long row) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
            return (boolean) h.invokeExact(peer, Sels.isRowSelected, row);
        } catch (Throwable t) { throw new RuntimeException("isRowSelected: failed", t); }
    }

    /// [table isColumnSelected:]
    public boolean isColumnSelected(long col) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
            return (boolean) h.invokeExact(peer, Sels.isColumnSelected, col);
        } catch (Throwable t) { throw new RuntimeException("isColumnSelected: failed", t); }
    }

    /// [table clickedRow] — row clicked last, -1 if none.
    public long clickedRow() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.clickedRow); } catch (Throwable t) { throw new RuntimeException("clickedRow failed", t); }
    }

    /// [table clickedColumn] — column clicked last, -1 if none.
    public long clickedColumn() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.clickedColumn); } catch (Throwable t) { throw new RuntimeException("clickedColumn failed", t); }
    }

    /// [table headerView] — NSTableHeaderView peer or null.
    public MemorySegment headerView() {
        ensureInit();
        try { return (MemorySegment) H.hId().invokeExact(peer, Sels.headerView); } catch (Throwable t) { throw new RuntimeException("headerView failed", t); }
    }

    /// [table setHeaderView:]
    public void setHeaderView(MemorySegment headerView) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.setHeaderView, (MemorySegment) ((MemorySegment) (headerView == null ? MemorySegment.NULL : headerView))); } catch (Throwable t) { throw new RuntimeException("setHeaderView: failed", t); }
    }

    /// [table gridStyleMask] — NSTableViewGridLineStyle (bitmask).
    public long gridStyleMask() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.gridStyleMask); } catch (Throwable t) { throw new RuntimeException("gridStyleMask failed", t); }
    }

    /// [table setGridStyleMask:]
    public void setGridStyleMask(long mask) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.setGridStyleMask, mask); } catch (Throwable t) { throw new RuntimeException("setGridStyleMask: failed", t); }
    }

    /// [table allowsMultipleSelection]
    public boolean allowsMultipleSelection() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsMultipleSelection);
    }

    /// [table setAllowsMultipleSelection:]
    public void setAllowsMultipleSelection(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setAllowsMultipleSelection, flag); } catch (Throwable t) { throw new RuntimeException("setAllowsMultipleSelection: failed", t); }
    }

    /// [table allowsEmptySelection]
    public boolean allowsEmptySelection() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsEmptySelection);
    }

    /// [table setAllowsEmptySelection:]
    public void setAllowsEmptySelection(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setAllowsEmptySelection, flag); } catch (Throwable t) { throw new RuntimeException("setAllowsEmptySelection: failed", t); }
    }

    /// [table allowsColumnSelection]
    public boolean allowsColumnSelection() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsColumnSelection);
    }

    /// [table setAllowsColumnSelection:]
    public void setAllowsColumnSelection(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setAllowsColumnSelection, flag); } catch (Throwable t) { throw new RuntimeException("setAllowsColumnSelection: failed", t); }
    }

    /// [table sortDescriptors] — NSArray of NSSortDescriptor peers (id), or null.
    public MemorySegment sortDescriptors() {
        ensureInit();
        try { return (MemorySegment) H.hId().invokeExact(peer, Sels.sortDescriptors); } catch (Throwable t) { throw new RuntimeException("sortDescriptors failed", t); }
    }

    /// [table setSortDescriptors:]
    public void setSortDescriptors(MemorySegment descriptors) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.setSortDescriptors, (MemorySegment) ((MemorySegment) (descriptors == null ? MemorySegment.NULL : descriptors))); } catch (Throwable t) { throw new RuntimeException("setSortDescriptors: failed", t); }
    }

    /// [table editedRow]
    public long editedRow() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.editedRow); } catch (Throwable t) { throw new RuntimeException("editedRow failed", t); }
    }

    /// [table editedColumn]
    public long editedColumn() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.editedColumn); } catch (Throwable t) { throw new RuntimeException("editedColumn failed", t); }
    }

    /// [table editColumn:row:withEvent:select:] — begin editing.
    public void editColumn(long column, long row, MemorySegment event, boolean select) {
        ensureInit();
        try { H.hEdit().invokeExact(peer, Sels.editColumn_row_withEvent_select, column, row, (MemorySegment) (event == null ? MemorySegment.NULL : event), select); } catch (Throwable t) { throw new RuntimeException("editColumn:row:withEvent:select: failed", t); }
    }

    /// [table scrollRowToVisible:]
    public void scrollRowToVisible(long row) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.scrollRowToVisible, row); } catch (Throwable t) { throw new RuntimeException("scrollRowToVisible: failed", t); }
    }

    /// [table scrollColumnToVisible:]
    public void scrollColumnToVisible(long col) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.scrollColumnToVisible, col); } catch (Throwable t) { throw new RuntimeException("scrollColumnToVisible: failed", t); }
    }

    /// [table selectAll:]
    public void selectAll(MemorySegment sender) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.selectAll, (MemorySegment) ((MemorySegment) (sender == null ? MemorySegment.NULL : sender))); } catch (Throwable t) { throw new RuntimeException("selectAll: failed", t); }
    }

    /// [table deselectAll:]
    public void deselectAll(MemorySegment sender) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.deselectAll, (MemorySegment) ((MemorySegment) (sender == null ? MemorySegment.NULL : sender))); } catch (Throwable t) { throw new RuntimeException("deselectAll: failed", t); }
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
        ensureInit();
        try { return NSView.wrap((MemorySegment) H.hId().invokeExact(peer, Sels.cornerView)); } catch (Throwable t) { throw new RuntimeException("cornerView failed", t); }
    }

    /// [table setCornerView:]
    public void setCornerView(NSView view) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.setCornerView, (MemorySegment) (view == null ? MemorySegment.NULL : view.peer())); } catch (Throwable t) { throw new RuntimeException("setCornerView: failed", t); }
    }

    /// [table allowsColumnReordering]
    public boolean allowsColumnReordering() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsColumnReordering);
    }

    /// [table setAllowsColumnReordering:]
    public void setAllowsColumnReordering(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setAllowsColumnReordering, flag); } catch (Throwable t) { throw new RuntimeException("setAllowsColumnReordering: failed", t); }
    }

    /// [table columnAutoresizingStyle] — NSTableViewColumnAutoresizingStyle (raw).
    public long columnAutoresizingStyle() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.columnAutoresizingStyle); } catch (Throwable t) { throw new RuntimeException("columnAutoresizingStyle failed", t); }
    }

    /// [table setColumnAutoresizingStyle:]
    public void setColumnAutoresizingStyle(long style) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.setColumnAutoresizingStyle, style); } catch (Throwable t) { throw new RuntimeException("setColumnAutoresizingStyle: failed", t); }
    }

    /// [table intercellSpacing]
    public NSSize intercellSpacing() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hSize().invokeExact(ObjC.structSlot(), peer, Sels.intercellSpacing);
            return NSSize.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("intercellSpacing failed", t); }
    }

    /// [table setIntercellSpacing:]
    public void setIntercellSpacing(NSSize spacing) {
        ensureInit();
        if (spacing == null) return;
        try { H.hVoidSize().invokeExact(peer, Sels.setIntercellSpacing, spacing.toSegment()); } catch (Throwable t) { throw new RuntimeException("setIntercellSpacing: failed", t); }
    }

    /// [table backgroundColor]
    public NSColor backgroundColor() {
        ensureInit();
        try { return NSColor.wrap((MemorySegment) H.hId().invokeExact(peer, Sels.backgroundColor)); } catch (Throwable t) { throw new RuntimeException("backgroundColor failed", t); }
    }

    /// [table setBackgroundColor:]
    public void setBackgroundColor(NSColor color) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.setBackgroundColor, (MemorySegment) (color == null ? MemorySegment.NULL : color.peer())); } catch (Throwable t) { throw new RuntimeException("setBackgroundColor: failed", t); }
    }

    /// [table gridColor]
    public NSColor gridColor() {
        ensureInit();
        try { return NSColor.wrap((MemorySegment) H.hId().invokeExact(peer, Sels.gridColor)); } catch (Throwable t) { throw new RuntimeException("gridColor failed", t); }
    }

    /// [table setGridColor:]
    public void setGridColor(NSColor color) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.setGridColor, (MemorySegment) (color == null ? MemorySegment.NULL : color.peer())); } catch (Throwable t) { throw new RuntimeException("setGridColor: failed", t); }
    }

    /// [table rowSizeStyle] — NSTableViewRowSizeStyle (raw).
    public long rowSizeStyle() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.rowSizeStyle); } catch (Throwable t) { throw new RuntimeException("rowSizeStyle failed", t); }
    }

    /// [table setRowSizeStyle:]
    public void setRowSizeStyle(long style) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.setRowSizeStyle, style); } catch (Throwable t) { throw new RuntimeException("setRowSizeStyle: failed", t); }
    }

    /// [table effectiveRowSizeStyle] — resolved style (raw).
    public long effectiveRowSizeStyle() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.effectiveRowSizeStyle); } catch (Throwable t) { throw new RuntimeException("effectiveRowSizeStyle failed", t); }
    }

    /// [table noteHeightOfRowsWithIndexesChanged:] — re-tile using fresh delegate heights.
    public void noteHeightOfRowsWithIndexesChanged(MemorySegment indexes) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.noteHeightOfRowsWithIndexesChanged, (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes)); } catch (Throwable t) { throw new RuntimeException("noteHeightOfRowsWithIndexesChanged: failed", t); }
    }

    /// [table tableColumns] — NSArray of NSTableColumn peers (id).
    public MemorySegment tableColumns() {
        ensureInit();
        try { return (MemorySegment) H.hId().invokeExact(peer, Sels.tableColumns); } catch (Throwable t) { throw new RuntimeException("tableColumns failed", t); }
    }

    /// [table moveColumn:toColumn:]
    public void moveColumn(long oldIndex, long newIndex) {
        ensureInit();
        try { H.hVoidIntInt().invokeExact(peer, Sels.moveColumn_toColumn, oldIndex, newIndex); } catch (Throwable t) { throw new RuntimeException("moveColumn:toColumn: failed", t); }
    }

    /// [table columnWithIdentifier:] — index or -1.
    public long columnWithIdentifier(String identifier) {
        ensureInit();
        try { return (long) H.hIntId().invokeExact(peer, Sels.columnWithIdentifier, (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier))); } catch (Throwable t) { throw new RuntimeException("columnWithIdentifier: failed", t); }
    }

    /// [table tableColumnWithIdentifier:] — NSTableColumn or nil.
    public NSTableColumn tableColumnWithIdentifier(String identifier) {
        ensureInit();
        try { return NSTableColumn.wrap((MemorySegment) H.hIdId().invokeExact(peer, Sels.tableColumnWithIdentifier, (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)))); } catch (Throwable t) { throw new RuntimeException("tableColumnWithIdentifier: failed", t); }
    }

    /// [table tile] — size to fit content.
    public void tile() {
        ensureInit();
        try { H.hVoid().invokeExact(peer, Sels.tile); } catch (Throwable t) { throw new RuntimeException("tile failed", t); }
    }

    /// [table sizeLastColumnToFit]
    public void sizeLastColumnToFit() {
        ensureInit();
        try { H.hVoid().invokeExact(peer, Sels.sizeLastColumnToFit); } catch (Throwable t) { throw new RuntimeException("sizeLastColumnToFit failed", t); }
    }

    /// [table noteNumberOfRowsChanged]
    public void noteNumberOfRowsChanged() {
        ensureInit();
        try { H.hVoid().invokeExact(peer, Sels.noteNumberOfRowsChanged); } catch (Throwable t) { throw new RuntimeException("noteNumberOfRowsChanged failed", t); }
    }

    /// [table reloadDataForRowIndexes:columnIndexes:]
    public void reloadDataForRowIndexes(MemorySegment rows, MemorySegment columns) {
        ensureInit();
        try {
            H.hVoidIdId().invokeExact(peer, Sels.reloadDataForRowIndexes_columnIndexes,
                    (MemorySegment) (rows == null ? MemorySegment.NULL : rows),
                    (MemorySegment) (columns == null ? MemorySegment.NULL : columns));
        } catch (Throwable t) { throw new RuntimeException("reloadDataForRowIndexes:columnIndexes: failed", t); }
    }

    /// [table doubleAction] — SEL id or nil.
    public MemorySegment doubleAction() {
        ensureInit();
        try { return (MemorySegment) H.hId().invokeExact(peer, Sels.doubleAction); } catch (Throwable t) { throw new RuntimeException("doubleAction failed", t); }
    }

    /// [table setDoubleAction:] — SEL for double-click (selector name, nil clears).
    public void setDoubleAction(String selector) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.setDoubleAction, (MemorySegment) (selector == null ? MemorySegment.NULL : ObjC.sel(selector))); } catch (Throwable t) { throw new RuntimeException("setDoubleAction: failed", t); }
    }

    /// [table setIndicatorImage:inTableColumn:]
    public void setIndicatorImage(NSImage image, NSTableColumn column) {
        ensureInit();
        try {
            H.hVoidIdId().invokeExact(peer, Sels.setIndicatorImage_inTableColumn,
                    (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()),
                    (MemorySegment) (column == null ? MemorySegment.NULL : column.peer()));
        } catch (Throwable t) { throw new RuntimeException("setIndicatorImage:inTableColumn: failed", t); }
    }

    /// [table indicatorImageInTableColumn:] — NSImage or nil.
    public NSImage indicatorImageInTableColumn(NSTableColumn column) {
        ensureInit();
        try { return NSImage.wrap((MemorySegment) H.hIdId().invokeExact(peer, Sels.indicatorImageInTableColumn, (MemorySegment) (column == null ? MemorySegment.NULL : column.peer()))); } catch (Throwable t) { throw new RuntimeException("indicatorImageInTableColumn: failed", t); }
    }

    /// [table highlightedTableColumn]
    public NSTableColumn highlightedTableColumn() {
        ensureInit();
        try { return NSTableColumn.wrap((MemorySegment) H.hId().invokeExact(peer, Sels.highlightedTableColumn)); } catch (Throwable t) { throw new RuntimeException("highlightedTableColumn failed", t); }
    }

    /// [table setHighlightedTableColumn:]
    public void setHighlightedTableColumn(NSTableColumn column) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.setHighlightedTableColumn, (MemorySegment) (column == null ? MemorySegment.NULL : column.peer())); } catch (Throwable t) { throw new RuntimeException("setHighlightedTableColumn: failed", t); }
    }

    /// [table verticalMotionCanBeginDrag]
    public boolean verticalMotionCanBeginDrag() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.verticalMotionCanBeginDrag);
    }

    /// [table setVerticalMotionCanBeginDrag:]
    public void setVerticalMotionCanBeginDrag(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setVerticalMotionCanBeginDrag, flag); } catch (Throwable t) { throw new RuntimeException("setVerticalMotionCanBeginDrag: failed", t); }
    }

    /// [table setDropRow:dropOperation:] — re-target a proposed drop.
    public void setDropRow(long row, long operation) {
        ensureInit();
        try { H.hVoidIntInt().invokeExact(peer, Sels.setDropRow_dropOperation, row, operation); } catch (Throwable t) { throw new RuntimeException("setDropRow:dropOperation: failed", t); }
    }

    /// [table numberOfSelectedColumns]
    public long numberOfSelectedColumns() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.numberOfSelectedColumns); } catch (Throwable t) { throw new RuntimeException("numberOfSelectedColumns failed", t); }
    }

    /// [table numberOfSelectedRows]
    public long numberOfSelectedRows() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.numberOfSelectedRows); } catch (Throwable t) { throw new RuntimeException("numberOfSelectedRows failed", t); }
    }

    /// [table allowsTypeSelect]
    public boolean allowsTypeSelect() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsTypeSelect);
    }

    /// [table setAllowsTypeSelect:]
    public void setAllowsTypeSelect(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setAllowsTypeSelect, flag); } catch (Throwable t) { throw new RuntimeException("setAllowsTypeSelect: failed", t); }
    }

    /// [table style] — NSTableViewStyle (raw).
    public long style() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.style); } catch (Throwable t) { throw new RuntimeException("style failed", t); }
    }

    /// [table setStyle:]
    public void setStyle(long style) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.setStyle, style); } catch (Throwable t) { throw new RuntimeException("setStyle: failed", t); }
    }

    /// [table effectiveStyle] — resolved style (raw).
    public long effectiveStyle() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.effectiveStyle); } catch (Throwable t) { throw new RuntimeException("effectiveStyle failed", t); }
    }

    /// [table selectionHighlightStyle] — NSTableViewSelectionHighlightStyle (raw).
    public long selectionHighlightStyle() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.selectionHighlightStyle); } catch (Throwable t) { throw new RuntimeException("selectionHighlightStyle failed", t); }
    }

    /// [table setSelectionHighlightStyle:]
    public void setSelectionHighlightStyle(long style) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.setSelectionHighlightStyle, style); } catch (Throwable t) { throw new RuntimeException("setSelectionHighlightStyle: failed", t); }
    }

    /// [table draggingDestinationFeedbackStyle] — NSTableViewDraggingDestinationFeedbackStyle (raw).
    public long draggingDestinationFeedbackStyle() {
        ensureInit();
        try { return (long) H.hInt().invokeExact(peer, Sels.draggingDestinationFeedbackStyle); } catch (Throwable t) { throw new RuntimeException("draggingDestinationFeedbackStyle failed", t); }
    }

    /// [table setDraggingDestinationFeedbackStyle:]
    public void setDraggingDestinationFeedbackStyle(long style) {
        ensureInit();
        try { H.hVoidInt().invokeExact(peer, Sels.setDraggingDestinationFeedbackStyle, style); } catch (Throwable t) { throw new RuntimeException("setDraggingDestinationFeedbackStyle: failed", t); }
    }

    /// [table columnIndexesInRect:] — NSIndexSet peer (id).
    public MemorySegment columnIndexesInRect(NSRect rect) {
        ensureInit();
        if (rect == null) return MemorySegment.NULL;
        try { return (MemorySegment) H.hIdRect().invokeExact(peer, Sels.columnIndexesInRect, rect.toSegment()); } catch (Throwable t) { throw new RuntimeException("columnIndexesInRect: failed", t); }
    }

    /// [table autosaveName] — persistence name or nil.
    public String autosaveName() {
        ensureInit();
        try { return ObjC.toString((MemorySegment) H.hId().invokeExact(peer, Sels.autosaveName)); } catch (Throwable t) { throw new RuntimeException("autosaveName failed", t); }
    }

    /// [table setAutosaveName:] — nil removes persistence data for the previous name.
    public void setAutosaveName(String name) {
        ensureInit();
        try { H.hVoidId().invokeExact(peer, Sels.setAutosaveName, (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name))); } catch (Throwable t) { throw new RuntimeException("setAutosaveName: failed", t); }
    }

    /// [table autosaveTableColumns]
    public boolean autosaveTableColumns() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.autosaveTableColumns);
    }

    /// [table setAutosaveTableColumns:]
    public void setAutosaveTableColumns(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setAutosaveTableColumns, flag); } catch (Throwable t) { throw new RuntimeException("setAutosaveTableColumns: failed", t); }
    }

    /// [table rowViewAtRow:makeIfNecessary:] — NSTableRowView or nil.
    public NSTableRowView rowViewAtRow(long row, boolean makeIfNecessary) {
        ensureInit();
        try { return NSTableRowView.wrap((MemorySegment) H.hIdIntBool().invokeExact(peer, Sels.rowViewAtRow_makeIfNecessary, row, makeIfNecessary)); } catch (Throwable t) { throw new RuntimeException("rowViewAtRow:makeIfNecessary: failed", t); }
    }

    /// [table rowForView:] — row or -1.
    public long rowForView(NSView view) {
        ensureInit();
        try { return (long) H.hIntId().invokeExact(peer, Sels.rowForView, (MemorySegment) (view == null ? MemorySegment.NULL : view.peer())); } catch (Throwable t) { throw new RuntimeException("rowForView: failed", t); }
    }

    /// [table columnForView:] — column or -1.
    public long columnForView(NSView view) {
        ensureInit();
        try { return (long) H.hIntId().invokeExact(peer, Sels.columnForView, (MemorySegment) (view == null ? MemorySegment.NULL : view.peer())); } catch (Throwable t) { throw new RuntimeException("columnForView: failed", t); }
    }

    /// [table makeViewWithIdentifier:owner:] — reusable NSView or nil.
    public NSView makeViewWithIdentifier(String identifier, MemorySegment owner) {
        ensureInit();
        try {
            return NSView.wrap((MemorySegment) H.hIdIdId().invokeExact(peer, Sels.makeViewWithIdentifier_owner,
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)),
                    (MemorySegment) (owner == null ? MemorySegment.NULL : owner)));
        } catch (Throwable t) { throw new RuntimeException("makeViewWithIdentifier:owner: failed", t); }
    }

    /// [table floatsGroupRows]
    public boolean floatsGroupRows() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.floatsGroupRows);
    }

    /// [table setFloatsGroupRows:]
    public void setFloatsGroupRows(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setFloatsGroupRows, flag); } catch (Throwable t) { throw new RuntimeException("setFloatsGroupRows: failed", t); }
    }

    /// [table rowActionsVisible] — setting YES throws; setting NO hides.
    public boolean rowActionsVisible() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.rowActionsVisible);
    }

    /// [table setRowActionsVisible:] — only NO is supported (YES throws).
    public void setRowActionsVisible(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setRowActionsVisible, flag); } catch (Throwable t) { throw new RuntimeException("setRowActionsVisible: failed", t); }
    }

    /// [table beginUpdates] — open an animated row-change group.
    public void beginUpdates() {
        ensureInit();
        try { H.hVoid().invokeExact(peer, Sels.beginUpdates); } catch (Throwable t) { throw new RuntimeException("beginUpdates failed", t); }
    }

    /// [table endUpdates] — close the group.
    public void endUpdates() {
        ensureInit();
        try { H.hVoid().invokeExact(peer, Sels.endUpdates); } catch (Throwable t) { throw new RuntimeException("endUpdates failed", t); }
    }

    /// [table insertRowsAtIndexes:withAnimation:]
    public void insertRowsAtIndexes(MemorySegment indexes, long animation) {
        ensureInit();
        try { H.hVoidIdInt().invokeExact(peer, Sels.insertRowsAtIndexes_withAnimation, (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes), animation); } catch (Throwable t) { throw new RuntimeException("insertRowsAtIndexes:withAnimation: failed", t); }
    }

    /// [table removeRowsAtIndexes:withAnimation:]
    public void removeRowsAtIndexes(MemorySegment indexes, long animation) {
        ensureInit();
        try { H.hVoidIdInt().invokeExact(peer, Sels.removeRowsAtIndexes_withAnimation, (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes), animation); } catch (Throwable t) { throw new RuntimeException("removeRowsAtIndexes:withAnimation: failed", t); }
    }

    /// [table moveRowAtIndex:toIndex:]
    public void moveRowAtIndex(long oldIndex, long newIndex) {
        ensureInit();
        try { H.hVoidIntInt().invokeExact(peer, Sels.moveRowAtIndex_toIndex, oldIndex, newIndex); } catch (Throwable t) { throw new RuntimeException("moveRowAtIndex:toIndex: failed", t); }
    }

    /// [table hideRowsAtIndexes:withAnimation:]
    public void hideRowsAtIndexes(MemorySegment indexes, long animation) {
        ensureInit();
        try { H.hVoidIdInt().invokeExact(peer, Sels.hideRowsAtIndexes_withAnimation, (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes), animation); } catch (Throwable t) { throw new RuntimeException("hideRowsAtIndexes:withAnimation: failed", t); }
    }

    /// [table unhideRowsAtIndexes:withAnimation:]
    public void unhideRowsAtIndexes(MemorySegment indexes, long animation) {
        ensureInit();
        try { H.hVoidIdInt().invokeExact(peer, Sels.unhideRowsAtIndexes_withAnimation, (MemorySegment) (indexes == null ? MemorySegment.NULL : indexes), animation); } catch (Throwable t) { throw new RuntimeException("unhideRowsAtIndexes:withAnimation: failed", t); }
    }

    /// [table hiddenRowIndexes] — NSIndexSet peer (id).
    public MemorySegment hiddenRowIndexes() {
        ensureInit();
        try { return (MemorySegment) H.hId().invokeExact(peer, Sels.hiddenRowIndexes); } catch (Throwable t) { throw new RuntimeException("hiddenRowIndexes failed", t); }
    }

    /// [table registerNib:forIdentifier:] — associate a nib (or nil to remove) with an identifier.
    public void registerNib(MemorySegment nib, String identifier) {
        ensureInit();
        try {
            H.hVoidIdId().invokeExact(peer, Sels.registerNib_forIdentifier,
                    (MemorySegment) (nib == null ? MemorySegment.NULL : nib),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)));
        } catch (Throwable t) { throw new RuntimeException("registerNib:forIdentifier: failed", t); }
    }

    /// [table registeredNibsByIdentifier] — NSDictionary peer or nil.
    public MemorySegment registeredNibsByIdentifier() {
        ensureInit();
        try { return (MemorySegment) H.hId().invokeExact(peer, Sels.registeredNibsByIdentifier); } catch (Throwable t) { throw new RuntimeException("registeredNibsByIdentifier failed", t); }
    }

    /// [table usesStaticContents]
    public boolean usesStaticContents() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesStaticContents);
    }

    /// [table setUsesStaticContents:]
    public void setUsesStaticContents(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setUsesStaticContents, flag); } catch (Throwable t) { throw new RuntimeException("setUsesStaticContents: failed", t); }
    }

    /// [table usesAutomaticRowHeights]
    public boolean usesAutomaticRowHeights() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesAutomaticRowHeights);
    }

    /// [table setUsesAutomaticRowHeights:]
    public void setUsesAutomaticRowHeights(boolean flag) {
        ensureInit();
        try { H.hVoidBool().invokeExact(peer, Sels.setUsesAutomaticRowHeights, flag); } catch (Throwable t) { throw new RuntimeException("setUsesAutomaticRowHeights: failed", t); }
    }

}
