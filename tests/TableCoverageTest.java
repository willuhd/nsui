package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.NSArray;
import nsui.NSCollectionView;
import nsui.NSCollectionViewItem;
import nsui.NSColor;
import nsui.NSIndexSet;
import nsui.NSOutlineView;
import nsui.NSPoint;
import nsui.NSRect;
import nsui.NSSize;
import nsui.NSTableCellView;
import nsui.NSTableColumn;
import nsui.NSTableHeaderView;
import nsui.NSTableRowView;
import nsui.NSTableView;
import nsui.NSView;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// TableCoverageTest — property round-trips for the Tables batch additions:
/// NSTableView, NSOutlineView, NSCollectionView, NSCollectionViewItem,
/// NSTableColumn, NSTableCellView, NSTableRowView, NSTableHeaderView.
///
/// Views are never attached to a window (fully hidden, no key status, no
/// audio). Every live call below was probed safe: AppKit raises *fatal*
/// (process-aborting) NSExceptions for out-of-range access, so the lookups
/// that need populated stores are deliberately NOT called here:
/// - NSTableView.rowViewAtRow:makeIfNecessary: (throws when the row is out
///   of range; needs rows via a data source),
/// - NSTableRowView.viewAtColumn: (throws when the column is out of range;
///   needs a row with columns),
/// - NSCollectionView.itemAtIndex: / makeItemWithIdentifier:forIndexPath: /
///   layoutAttributesForItemAtIndexPath: / layoutAttributesForSupplementary… /
///   moveItemAtIndexPath: (throw for out-of-bounds or nil index paths;
///   need items via a data source),
/// - NSTableView.setRowActionsVisible: (throws for cell-based tables, even
///   for NO; only the getter is pinned),
/// - NSTableView.setDropRow:dropOperation: is only pinned in its documented
///   whole-table form (row -1, DropOn); other rows log an AppKit error.
/// Data-source / delegate protocols are not covered (need upcall machinery).
public final class TableCoverageTest {

    private static int asserts;

    private static void check(boolean ok, String msg) { asserts++; TestKit.check(ok, msg); }

    private static boolean isNil(MemorySegment s) { return s == null || s.address() == 0; }

    public static void main(String[] args) {
        System.out.println("=== TableCoverageTest — Tables batch property round-trips ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            TestKit.skip("ObjC.init failed (connection error or not macOS): " + t);
        }

        // ---------------- NSTableView ----------------
        try {
            NSTableView tv = NSTableView.create(new NSRect(0, 0, 400, 200));
            check(tv != null && tv.peer().address() != 0, "NSTableView.create non-nil");
            NSTableColumn c1 = NSTableColumn.create("Name");
            NSTableColumn c2 = NSTableColumn.create("Score");
            tv.addTableColumn(c1);
            tv.addTableColumn(c2);

            // owning-table link (NSTableColumn addition)
            check(isNil(NSTableColumn.create("Detached").tableView()), "detached NSTableColumn.tableView nil");
            check(!isNil(c1.tableView()) && c1.tableView().address() == tv.peer().address(),
                    "NSTableColumn.tableView peer == table after addTableColumn");

            check(tv.columnWithIdentifier("Name") == 0, "columnWithIdentifier Name == 0");
            check(tv.columnWithIdentifier("Nope") == -1, "columnWithIdentifier unknown == -1");
            check(tv.tableColumnWithIdentifier("Nope") == null, "tableColumnWithIdentifier unknown nil");
            check(tv.tableColumnWithIdentifier("Name") != null
                    && tv.tableColumnWithIdentifier("Name").peer().address() == c1.peer().address(),
                    "tableColumnWithIdentifier Name peer matches");
            check(NSArray.wrap(tv.tableColumns()).count() == 2, "tableColumns count == 2");
            tv.moveColumn(0, 1);
            NSArray moved = NSArray.wrap(tv.tableColumns());
            check(moved.objectAtIndex(0).address() == c2.peer().address()
                    && moved.objectAtIndex(1).address() == c1.peer().address(),
                    "moveColumn(0,1) swaps order");
            tv.moveColumn(1, 0);
            check(NSArray.wrap(tv.tableColumns()).objectAtIndex(0).address() == c1.peer().address(),
                    "moveColumn(1,0) restores order");

            check(tv.style() == 0, "style default Automatic(0)");
            tv.setStyle(4);
            check(tv.style() == 4, "style Plain round-trip");
            tv.setStyle(0);
            check(tv.style() == 0, "style restore Automatic");
            try { TestKit.noThrow("effectiveStyle no throw", () -> tv.effectiveStyle()); }
            catch (Throwable t) { check(false, "effectiveStyle threw: " + t); }

            check(tv.rowSizeStyle() == 0, "rowSizeStyle default Custom(0)");
            try { TestKit.noThrow("effectiveRowSizeStyle no throw", () -> tv.effectiveRowSizeStyle()); }
            catch (Throwable t) { check(false, "effectiveRowSizeStyle threw: " + t); }

            tv.setIntercellSpacing(new NSSize(7, 9));
            check(new NSSize(7, 9).equals(tv.intercellSpacing()), "intercellSpacing (7,9) round-trip");
            tv.setIntercellSpacing(new NSSize(3, 2));
            check(new NSSize(3, 2).equals(tv.intercellSpacing()), "intercellSpacing restore (3,2)");

            check(tv.backgroundColor() != null, "backgroundColor initial non-nil");
            check(tv.gridColor() != null, "gridColor initial non-nil");
            NSColor savedBg = tv.backgroundColor();
            NSColor savedGrid = tv.gridColor();
            tv.setBackgroundColor(NSColor.redColor());
            check(tv.backgroundColor() != null && tv.backgroundColor().isKindOfClass("NSColor"),
                    "backgroundColor set redColor");
            tv.setGridColor(NSColor.blueColor());
            check(tv.gridColor() != null && tv.gridColor().isKindOfClass("NSColor"),
                    "gridColor set blueColor");
            tv.setBackgroundColor(savedBg);
            tv.setGridColor(savedGrid);

            check(tv.allowsColumnReordering(), "allowsColumnReordering default YES");
            tv.setAllowsColumnReordering(false);
            check(!tv.allowsColumnReordering(), "allowsColumnReordering false");
            tv.setAllowsColumnReordering(true);

            check(tv.columnAutoresizingStyle() == 4, "columnAutoresizingStyle default LastColumnOnly(4)");
            tv.setColumnAutoresizingStyle(1);
            check(tv.columnAutoresizingStyle() == 1, "columnAutoresizingStyle Uniform round-trip");
            tv.setColumnAutoresizingStyle(4);

            check(tv.verticalMotionCanBeginDrag(), "verticalMotionCanBeginDrag default YES");
            tv.setVerticalMotionCanBeginDrag(false);
            check(!tv.verticalMotionCanBeginDrag(), "verticalMotionCanBeginDrag false");
            tv.setVerticalMotionCanBeginDrag(true);

            check(tv.allowsTypeSelect(), "allowsTypeSelect default YES");
            tv.setAllowsTypeSelect(false);
            check(!tv.allowsTypeSelect(), "allowsTypeSelect false");
            tv.setAllowsTypeSelect(true);

            check(!tv.autosaveTableColumns(), "autosaveTableColumns default NO");
            tv.setAutosaveTableColumns(true);
            check(tv.autosaveTableColumns(), "autosaveTableColumns true");
            tv.setAutosaveTableColumns(false);

            check(tv.autosaveName() == null, "autosaveName initial nil");
            tv.setAutosaveName("nsui-table-coverage");
            check("nsui-table-coverage".equals(tv.autosaveName()), "autosaveName round-trip");
            tv.setAutosaveName(null);
            check(tv.autosaveName() == null, "autosaveName cleared");

            check(isNil(tv.doubleAction()), "doubleAction initial nil");
            tv.setDoubleAction("copy:");
            check(!isNil(tv.doubleAction()), "doubleAction set copy:");
            tv.setDoubleAction(null);
            check(isNil(tv.doubleAction()), "doubleAction cleared");

            check(tv.highlightedTableColumn() == null, "highlightedTableColumn initial nil");
            tv.setHighlightedTableColumn(c1);
            check(tv.highlightedTableColumn() != null
                    && tv.highlightedTableColumn().peer().address() == c1.peer().address(),
                    "highlightedTableColumn round-trip");
            tv.setHighlightedTableColumn(null);
            check(tv.highlightedTableColumn() == null, "highlightedTableColumn cleared");

            check(tv.indicatorImageInTableColumn(c1) == null, "indicatorImage initial nil");
            try { TestKit.noThrow("setIndicatorImage(null) no throw", () -> tv.setIndicatorImage(null, c1)); }
            catch (Throwable t) { check(false, "setIndicatorImage(null) threw: " + t); }

            check(tv.numberOfSelectedColumns() == 0, "numberOfSelectedColumns 0");
            check(tv.numberOfSelectedRows() == 0, "numberOfSelectedRows 0");
            check(tv.selectedRow() == -1, "selectedRow -1 initially");
            check(!isNil(tv.selectedRowIndexes()), "selectedRowIndexes non-nil");
            check(!isNil(tv.hiddenRowIndexes()), "hiddenRowIndexes non-nil");
            check(isNil(tv.registeredNibsByIdentifier()), "registeredNibsByIdentifier nil");
            try { TestKit.noThrow("registerNib(null) no throw", () -> tv.registerNib(null, "probe")); }
            catch (Throwable t) { check(false, "registerNib(null) threw: " + t); }
            check(!isNil(tv.columnIndexesInRect(new NSRect(0, 0, 400, 200))),
                    "columnIndexesInRect non-nil");

            NSView foreign = NSView.create(new NSRect(0, 0, 10, 10), (ctx, dr) -> {});
            try { TestKit.noThrow("rowForView(foreign view) no throw", () -> tv.rowForView(foreign)); }
            catch (Throwable t) { check(false, "rowForView threw: " + t); }
            try { TestKit.noThrow("columnForView(foreign view) no throw", () -> tv.columnForView(foreign)); }
            catch (Throwable t) { check(false, "columnForView threw: " + t); }
            check(tv.makeViewWithIdentifier("NoSuchIdentifierXYZ", null) == null,
                    "makeViewWithIdentifier unknown nil");

            try { tv.tile(); tv.sizeLastColumnToFit(); TestKit.noThrow("tile/sizeLastColumnToFit/noteNumberOfRowsChanged no throw", () -> tv.noteNumberOfRowsChanged()); }
            catch (Throwable t) { check(false, "tile family threw: " + t); }
            try {
                tv.reloadDataForRowIndexes(NSIndexSet.indexSet().peer(), NSIndexSet.indexSet().peer());
                TestKit.noThrow("reloadDataForRowIndexes/noteHeightOfRows(empty) no throw", () -> tv.noteHeightOfRowsWithIndexesChanged(NSIndexSet.indexSet().peer()));
            } catch (Throwable t) { check(false, "reload family threw: " + t); }
            try { tv.beginUpdates(); TestKit.noThrow("begin/endUpdates no throw", () -> tv.endUpdates()); }
            catch (Throwable t) { check(false, "begin/endUpdates threw: " + t); }
            try { TestKit.noThrow("setDropRow(-1,DropOn) no throw", () -> tv.setDropRow(-1, 0)); }
            catch (Throwable t) { check(false, "setDropRow threw: " + t); }
            tv.selectRowIndexes(NSIndexSet.indexSet().peer(), false);
            check(tv.selectedRow() == -1 && tv.numberOfSelectedRows() == 0,
                    "selectRowIndexes(empty) -> selectedRow -1");

            check(tv.cornerView() != null, "cornerView initial non-nil (internal filler)");
            NSView customCorner = NSView.create(new NSRect(0, 0, 16, 16), (ctx, dr) -> {});
            NSView savedCorner = tv.cornerView();
            tv.setCornerView(customCorner);
            check(tv.cornerView() != null && tv.cornerView().peer().address() == customCorner.peer().address(),
                    "cornerView round-trip");
            tv.setCornerView(null);
            check(tv.cornerView() == null, "cornerView cleared");
            tv.setCornerView(savedCorner);

            check(tv.floatsGroupRows(), "floatsGroupRows default YES");
            tv.setFloatsGroupRows(false);
            check(!tv.floatsGroupRows(), "floatsGroupRows false");
            tv.setFloatsGroupRows(true);
            check(!tv.rowActionsVisible(), "rowActionsVisible default NO");
            check(!tv.usesStaticContents(), "usesStaticContents default NO");
            tv.setUsesStaticContents(true);
            check(tv.usesStaticContents(), "usesStaticContents true");
            tv.setUsesStaticContents(false);
            check(!tv.usesAutomaticRowHeights(), "usesAutomaticRowHeights default NO");
            tv.setUsesAutomaticRowHeights(true);
            check(tv.usesAutomaticRowHeights(), "usesAutomaticRowHeights true");
            tv.setUsesAutomaticRowHeights(false);

            check(tv.selectionHighlightStyle() == 0, "selectionHighlightStyle default Regular(0)");
            tv.setSelectionHighlightStyle(0);
            check(tv.selectionHighlightStyle() == 0, "selectionHighlightStyle round-trip");
            check(tv.draggingDestinationFeedbackStyle() == 0, "draggingDestinationFeedbackStyle default Regular(0)");
            tv.setDraggingDestinationFeedbackStyle(0);
            check(tv.draggingDestinationFeedbackStyle() == 0, "draggingDestinationFeedbackStyle round-trip");
        } catch (Throwable t) {
            check(false, "NSTableView section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSOutlineView ----------------
        try {
            NSOutlineView ov = NSOutlineView.create(new NSRect(0, 0, 200, 200));
            check(ov != null && ov.peer().address() != 0, "NSOutlineView.create non-nil");
            check(ov.indentationMarkerFollowsCell(), "indentationMarkerFollowsCell default YES");
            ov.setIndentationMarkerFollowsCell(false);
            check(!ov.indentationMarkerFollowsCell(), "indentationMarkerFollowsCell false");
            ov.setIndentationMarkerFollowsCell(true);
            check(ov.autoresizesOutlineColumn(), "autoresizesOutlineColumn default YES");
            ov.setAutoresizesOutlineColumn(false);
            check(!ov.autoresizesOutlineColumn(), "autoresizesOutlineColumn false");
            ov.setAutoresizesOutlineColumn(true);
            check(!ov.autosaveExpandedItems(), "autosaveExpandedItems default NO");
            ov.setAutosaveExpandedItems(true);
            check(ov.autosaveExpandedItems(), "autosaveExpandedItems true");
            ov.setAutosaveExpandedItems(false);
            check(ov.stronglyReferencesItems(), "stronglyReferencesItems default YES");
            ov.setStronglyReferencesItems(false);
            check(!ov.stronglyReferencesItems(), "stronglyReferencesItems false");
            ov.setStronglyReferencesItems(true);
            try { TestKit.noThrow("reloadItem(null) no throw", () -> ov.reloadItem(null)); }
            catch (Throwable t) { check(false, "reloadItem(null) threw: " + t); }
            try { TestKit.noThrow("setDropItem(null,-1) no throw", () -> ov.setDropItem(null, -1)); }
            catch (Throwable t) { check(false, "setDropItem threw: " + t); }
            try { ov.expandItem(null); TestKit.noThrow("expand/collapse(null) no throw", () -> ov.collapseItem(null)); }
            catch (Throwable t) { check(false, "expand/collapse(null) threw: " + t); }
            check(ov.rowForItem(null) == -1, "rowForItem(null) == -1");
            MemorySegment item0 = ov.itemAtRow(0);
            check(isNil(item0), "itemAtRow(0) on empty outline nil");
            check(ov.levelForRow(0) == -1, "levelForRow(0) on empty outline == -1");
            check(ov.childIndexForItem(null) == -1, "childIndexForItem(null) == -1");
            check(ov.levelForItem(null) == -1, "levelForItem(null) == -1");
        } catch (Throwable t) {
            check(false, "NSOutlineView section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSCollectionView + NSCollectionViewItem ----------------
        try {
            NSCollectionView cv = NSCollectionView.create(new NSRect(0, 0, 200, 200));
            check(cv != null && cv.peer().address() != 0, "NSCollectionView.create non-nil");
            check(cv.numberOfSections() == 1, "numberOfSections == 1 (got " + cv.numberOfSections() + ")");
            check(cv.numberOfItemsInSection(0) == 0, "numberOfItemsInSection(0) == 0");
            check(isNil(cv.collectionViewLayout()), "collectionViewLayout initial nil (lazy)");
            try { TestKit.noThrow("setCollectionViewLayout(null) no throw", () -> cv.setCollectionViewLayout(null)); }
            catch (Throwable t) { check(false, "setCollectionViewLayout(null) threw: " + t); }
            check(!isNil(cv.backgroundColors()), "backgroundColors initial non-nil");
            try { TestKit.noThrow("setBackgroundColors(null) no throw", () -> cv.setBackgroundColors(null)); }
            catch (Throwable t) { check(false, "setBackgroundColors(null) threw: " + t); }
            check(!isNil(cv.backgroundColors()), "backgroundColors reset to default after null");
            check(!isNil(cv.content()), "content initial non-nil");
            cv.setContent(NSArray.array().peer());
            check(NSArray.wrap(cv.content()).count() == 0, "content set empty -> count 0");
            check(!cv.isFirstResponder(), "isFirstResponder false (no window)");
            check(cv.allowsEmptySelection(), "allowsEmptySelection default YES");
            cv.setAllowsEmptySelection(false);
            check(!cv.allowsEmptySelection(), "allowsEmptySelection false");
            cv.setAllowsEmptySelection(true);
            check(!isNil(cv.selectionIndexPaths()), "selectionIndexPaths non-nil");
            MemorySegment emptySet = ObjC.msgSendId(ObjC.cls("NSSet"), ObjC.sel("set"));
            cv.setSelectionIndexPaths(emptySet);
            check(NSArray.wrap(cv.selectionIndexPaths()).count() == 0, "setSelectionIndexPaths(empty) -> count 0");
            check(!isNil(cv.visibleItems()), "visibleItems non-nil");
            check(!isNil(cv.indexPathsForVisibleItems()), "indexPathsForVisibleItems non-nil");
            check(!isNil(cv.indexPathsForVisibleSupplementaryElementsOfKind("Hd")),
                    "indexPathsForVisibleSupplementaryElementsOfKind non-nil");
            check(!isNil(cv.visibleSupplementaryViewsOfKind("Hd")), "visibleSupplementaryViewsOfKind non-nil");
            check(isNil(cv.prefetchDataSource()), "prefetchDataSource initial nil");
            try { TestKit.noThrow("setPrefetchDataSource(null) no throw", () -> cv.setPrefetchDataSource(null)); }
            catch (Throwable t) { check(false, "setPrefetchDataSource(null) threw: " + t); }
            NSView bgv = NSView.create(new NSRect(0, 0, 40, 40), (ctx, dr) -> {});
            cv.setBackgroundView(bgv);
            check(cv.backgroundView() != null && cv.backgroundView().peer().address() == bgv.peer().address(),
                    "backgroundView round-trip");
            cv.setBackgroundView(null);
            check(cv.backgroundView() == null, "backgroundView cleared");
            check(!cv.backgroundViewScrollsWithContent(), "backgroundViewScrollsWithContent default NO");
            cv.setBackgroundViewScrollsWithContent(true);
            check(cv.backgroundViewScrollsWithContent(), "backgroundViewScrollsWithContent true");
            cv.setBackgroundViewScrollsWithContent(false);
            try {
                cv.registerClassForItem(ObjC.cls("NSCollectionViewItem"), "probeItem");
                cv.registerNibForItem(null, "probeNib");
                cv.registerClassForSupplementaryView(null, "Hd", "supp");
                TestKit.noThrow("registerClass/Nib (item + supplementary) no throw", () -> cv.registerNibForSupplementaryView(null, "Hd", "supp"));
            } catch (Throwable t) { check(false, "register threw: " + t); }
            MethodHandle hPath = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.INT));
            MemorySegment ip = null;
            try {
                ip = (MemorySegment) hPath.invokeExact(ObjC.cls("NSIndexPath"),
                        ObjC.sel("indexPathForItem:inSection:"), 0L, 0L);
            } catch (Throwable t) { check(false, "indexPathForItem:inSection: threw: " + t); }
            if (ip != null && ip.address() != 0) {
                check(cv.itemAtIndexPath(ip) == null, "itemAtIndexPath(uninstantiated) nil");
                MemorySegment atPoint = cv.indexPathForItemAtPoint(new NSPoint(5, 5));
                check(isNil(atPoint), "indexPathForItemAtPoint(empty) nil");
                check(cv.supplementaryViewForElementKind("Hd", ip) == null,
                        "supplementaryViewForElementKind(uninstantiated) nil");
            }
            try {
                cv.selectItemsAtIndexPaths(emptySet, 0);
                cv.deselectItemsAtIndexPaths(emptySet);
                TestKit.noThrow("select/deselect/scroll(empty set) no throw", () -> cv.scrollToItemsAtIndexPaths(emptySet, 0));
            } catch (Throwable t) { check(false, "select family threw: " + t); }
            try {
                cv.insertSections(NSIndexSet.indexSet().peer());
                cv.deleteSections(NSIndexSet.indexSet().peer());
                cv.reloadSections(NSIndexSet.indexSet().peer());
                TestKit.noThrow("section mutations(empty) no throw", () -> cv.moveSection(0, 0));
            } catch (Throwable t) { check(false, "section mutations threw: " + t); }
            try {
                cv.insertItemsAtIndexPaths(null);
                cv.deleteItemsAtIndexPaths(null);
                TestKit.noThrow("item mutations(nil) no throw", () -> cv.reloadItemsAtIndexPaths(null));
            } catch (Throwable t) { check(false, "item mutations threw: " + t); }
            try { cv.selectAll(null); TestKit.noThrow("selectAll/deselectAll(null) no throw", () -> cv.deselectAll(null)); }
            catch (Throwable t) { check(false, "selectAll family threw: " + t); }
            try { TestKit.noThrow("toggleSectionCollapse(view) no throw", () -> cv.toggleSectionCollapse(bgv.peer())); }
            catch (Throwable t) { check(false, "toggleSectionCollapse threw: " + t); }
            try { TestKit.noThrow("reloadData no throw", () -> cv.reloadData()); }
            catch (Throwable t) { check(false, "reloadData threw: " + t); }

            NSCollectionViewItem standalone = NSCollectionViewItem.create();
            check(standalone.collectionView() == null, "standalone item.collectionView nil");
            check(isNil(standalone.imageView()), "standalone item.imageView nil");
            check(isNil(standalone.textField()), "standalone item.textField nil");
            check(!isNil(standalone.draggingImageComponents()), "item.draggingImageComponents non-nil");
            MemorySegment ipForStandalone = cv.indexPathForItem(standalone);
            check(isNil(ipForStandalone), "indexPathForItem(unplaced item) nil");
        } catch (Throwable t) {
            check(false, "NSCollectionView section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSTableCellView (includes required objectValue round-trip) ----------------
        try {
            NSTableCellView cell = NSTableCellView.create(new NSRect(0, 0, 100, 20));
            check(cell != null && cell.peer().address() != 0, "NSTableCellView.create non-nil");
            cell.setObjectValue(ObjC.nsstring("cell-1"));
            MemorySegment got = cell.objectValue();
            check(got != null && got.address() != 0 && "cell-1".equals(ObjC.toString(got)),
                    "NSTableCellView objectValue round-trip cell-1");
            cell.setObjectValue(null);
            check(isNil(cell.objectValue()), "NSTableCellView objectValue cleared");
            check(isNil(cell.textField()), "NSTableCellView.textField nil (no outlets)");
            check(isNil(cell.imageView()), "NSTableCellView.imageView nil (no outlets)");
            check(cell.backgroundStyle() == 0, "backgroundStyle default 0");
            cell.setBackgroundStyle(1);
            check(cell.backgroundStyle() == 1, "backgroundStyle round-trip");
            cell.setBackgroundStyle(0);
            check(cell.rowSizeStyle() == 0, "rowSizeStyle default 0");
            cell.setRowSizeStyle(1);
            check(cell.rowSizeStyle() == 1, "rowSizeStyle round-trip");
            cell.setRowSizeStyle(0);
            check(!isNil(cell.draggingImageComponents()), "draggingImageComponents non-nil");
        } catch (Throwable t) {
            check(false, "NSTableCellView section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSTableRowView (includes required selected round-trip) ----------------
        try {
            NSTableRowView row = NSTableRowView.create(new NSRect(0, 0, 100, 20));
            check(row != null && row.peer().address() != 0, "NSTableRowView.create non-nil");
            check(!row.isSelected(), "isSelected initial false");
            row.setSelected(true);
            check(row.isSelected(), "NSTableRowView selected round-trip true");
            row.setSelected(false);
            check(!row.isSelected(), "NSTableRowView selected round-trip false");
            row.setEmphasized(true);
            check(row.isEmphasized(), "isEmphasized round-trip");
            row.setEmphasized(false);
            row.setGroupRowStyle(true);
            check(row.isGroupRowStyle(), "isGroupRowStyle round-trip");
            row.setGroupRowStyle(false);
            row.setPreviousRowSelected(true);
            check(row.isPreviousRowSelected(), "isPreviousRowSelected round-trip");
            row.setPreviousRowSelected(false);
            row.setNextRowSelected(true);
            check(row.isNextRowSelected(), "isNextRowSelected round-trip");
            row.setNextRowSelected(false);
            row.setTargetForDropOperation(true);
            check(row.isTargetForDropOperation(), "isTargetForDropOperation round-trip");
            row.setTargetForDropOperation(false);
            check(row.numberOfColumns() == 0, "numberOfColumns 0 detached");
            check(row.interiorBackgroundStyle() == 0, "interiorBackgroundStyle 0");
            check(!row.isFloating(), "isFloating false");
            check(row.backgroundColor() == null, "backgroundColor initial nil");
            row.setBackgroundColor(NSColor.redColor());
            check(row.backgroundColor() != null && row.backgroundColor().isKindOfClass("NSColor"),
                    "backgroundColor set redColor");
            check(row.indentationForDropOperation() == 0.0, "indentationForDropOperation 0");
            row.setIndentationForDropOperation(8);
            check(row.indentationForDropOperation() == 8.0, "indentationForDropOperation round-trip");
            row.setIndentationForDropOperation(0);
            check(row.draggingDestinationFeedbackStyle() == 0, "draggingDestinationFeedbackStyle 0");
            row.setDraggingDestinationFeedbackStyle(0);
            check(row.draggingDestinationFeedbackStyle() == 0, "draggingDestinationFeedbackStyle round-trip");
            check(row.selectionHighlightStyle() == 0, "selectionHighlightStyle 0");
            row.setSelectionHighlightStyle(0);
            check(row.selectionHighlightStyle() == 0, "selectionHighlightStyle round-trip");
        } catch (Throwable t) {
            check(false, "NSTableRowView section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSTableHeaderView ----------------
        try {
            NSTableHeaderView hv = NSTableHeaderView.create(new NSRect(0, 0, 200, 22));
            check(hv != null && hv.peer().address() != 0, "NSTableHeaderView.create non-nil");
            check(hv.resizedColumn() == -1, "resizedColumn -1 (no resize in flight)");
            check(hv.draggedColumn() == -1, "draggedColumn -1 (no drag in flight)");
            check(hv.draggedDistance() == 0.0, "draggedDistance 0");
            check(isNil(hv.tableView()), "tableView nil detached");
        } catch (Throwable t) {
            check(false, "NSTableHeaderView section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(TestKit.failures() == 0
                ? "RESULT: ALL PASS (" + asserts + " assertions)"
                : "RESULT: " + TestKit.failures() + " of " + asserts + " assertions FAILED");
        TestKit.end();
    }
}
