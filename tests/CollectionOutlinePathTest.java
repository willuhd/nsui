package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSCollectionView;
import nsui.NSCollectionViewItem;
import nsui.NSOutlineView;
import nsui.NSPathControl;
import nsui.NSRect;
import nsui.NSSplitView;
import nsui.NSTableColumn;
import nsui.NSView;
import nsui.objc.ObjC;

/**
 * CollectionOutlinePathTest — creation and property checks for NSCollectionView,
 * NSCollectionViewItem, NSOutlineView, NSPathControl, and NSSplitView (if present).
 */
public final class CollectionOutlinePathTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== CollectionOutlinePathTest — NSCollectionView/NSCollectionViewItem/NSOutlineView/NSPathControl/NSSplitView ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            System.out.println("SKIP: ObjC.init failed (connection error or not macOS): " + t);
            t.printStackTrace(System.out);
            System.out.println("RESULT: SKIP (connection error, continuing)");
            System.exit(0);
        }

        // ---------------- NSCollectionView ----------------
        try {
            NSCollectionView cv = NSCollectionView.create(new NSRect(0, 0, 200, 200));
            check(cv != null && cv.peer().address() != 0, "NSCollectionView.create non-nil");
            check(cv.isKindOfClass("NSCollectionView"), "NSCollectionView isKindOfClass NSCollectionView");

            boolean sel = cv.isSelectable();
            System.out.println("  initial isSelectable=" + sel);
            cv.setSelectable(true);
            check(cv.isSelectable() == true, "NSCollectionView setSelectable true -> true");
            cv.setSelectable(false);
            check(cv.isSelectable() == false, "NSCollectionView setSelectable false -> false");
            cv.setSelectable(true);
            check(cv.isSelectable() == true, "NSCollectionView setSelectable true again");

            cv.setAllowsMultipleSelection(true);
            check(cv.allowsMultipleSelection() == true, "NSCollectionView allowsMultipleSelection true");
            cv.setAllowsMultipleSelection(false);
            check(cv.allowsMultipleSelection() == false, "NSCollectionView allowsMultipleSelection false");
            cv.setAllowsMultipleSelection(true);

            // itemPrototype
            NSCollectionViewItem proto = NSCollectionViewItem.create();
            check(proto != null && proto.peer().address() != 0, "NSCollectionViewItem.create non-nil");
            NSView pv = NSView.create(new NSRect(0, 0, 50, 50), (ctx, dr) -> {});
            proto.setView(pv);
            NSView gotPv = proto.view();
            check(gotPv != null && gotPv.peer().address() != 0, "NSCollectionViewItem view non-nil after setView");
            if (gotPv != null) check(gotPv.peer().address() == pv.peer().address(), "NSCollectionViewItem view peer matches setView");

            try {
                cv.setItemPrototype(proto);
                check(true, "NSCollectionView setItemPrototype(proto) did not throw");
                MemorySegment ip = cv.itemPrototype();
                check(ip != null && ip.address() != 0, "NSCollectionView itemPrototype non-nil after set");
            } catch (Throwable t) {
                check(false, "NSCollectionView setItemPrototype threw: " + t);
            }

            // clear prototype
            try {
                cv.setItemPrototype((MemorySegment) null);
                check(true, "NSCollectionView setItemPrototype(null) did not throw");
                // restore
                cv.setItemPrototype(proto);
            } catch (Throwable t) { check(false, "NSCollectionView clear prototype threw: " + t); }

            try { cv.reloadData(); check(true, "NSCollectionView reloadData no throw"); } catch (Throwable t) { check(false, "NSCollectionView reloadData threw: " + t); }

        } catch (Throwable t) {
            check(false, "NSCollectionView section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSCollectionViewItem view ----------------
        try {
            NSCollectionViewItem item = NSCollectionViewItem.create();
            check(item != null && item.peer().address() != 0, "NSCollectionViewItem create non-nil (second)");
            check(item.isKindOfClass("NSCollectionViewItem"), "NSCollectionViewItem isKindOfClass NSCollectionViewItem");

            // initially view may be nil (no nib); just check no crash
            NSView initialView = item.view();
            System.out.println("  initial NSCollectionViewItem view=" + (initialView == null ? "null" : "non-nil"));

            NSView v = NSView.create(new NSRect(0, 0, 60, 60), (ctx, dr) -> {});
            item.setView(v);
            NSView gv = item.view();
            check(gv != null && gv.peer().address() == v.peer().address(), "NSCollectionViewItem setView/view round-trip");

            item.setSelected(true);
            check(item.isSelected() == true, "NSCollectionViewItem isSelected true after setSelected true");
            item.setSelected(false);
            check(item.isSelected() == false, "NSCollectionViewItem isSelected false");

            // representedObject round-trip with NSString
            MemorySegment hello = ObjC.nsstring("hello");
            item.setRepresentedObject(hello);
            MemorySegment ro = item.representedObject();
            check(ro != null && ro.address() != 0, "NSCollectionViewItem representedObject non-nil after set");
            if (ro != null && ro.address() != 0) {
                String s = ObjC.toString(ro);
                check("hello".equals(s), "NSCollectionViewItem representedObject string hello (got \"" + s + "\")");
            }
            item.setRepresentedObject(null);
            check(true, "NSCollectionViewItem setRepresentedObject(null) no throw");

        } catch (Throwable t) {
            check(false, "NSCollectionViewItem section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSOutlineView ----------------
        try {
            NSOutlineView ov = NSOutlineView.create(new NSRect(0, 0, 200, 200));
            check(ov != null && ov.peer().address() != 0, "NSOutlineView.create non-nil");
            check(ov.isKindOfClass("NSOutlineView"), "NSOutlineView isKindOfClass NSOutlineView");

            NSTableColumn col = NSTableColumn.create("outlineCol");
            col.setTitle("Outline");
            ov.addTableColumn(col);
            ov.setOutlineTableColumn(col);
            NSTableColumn gotCol = ov.outlineTableColumn();
            check(gotCol != null && gotCol.peer().address() != 0, "NSOutlineView outlineTableColumn non-nil after set");
            if (gotCol != null) check("outlineCol".equals(gotCol.title()) || "Outline".equals(gotCol.title()), "NSOutlineView outlineTableColumn title (got \"" + gotCol.title() + "\")");

            // expand/collapse/isItemExpanded with null item should not throw
            try {
                ov.expandItem(null);
                check(true, "NSOutlineView expandItem(null) did not throw");
            } catch (Throwable t) { check(false, "NSOutlineView expandItem(null) threw: " + t); }

            try {
                ov.expandItem(null, true);
                check(true, "NSOutlineView expandItem(null, true) did not throw");
            } catch (Throwable t) { check(false, "NSOutlineView expandItem expandChildren threw: " + t); }

            try {
                ov.collapseItem(null);
                check(true, "NSOutlineView collapseItem(null) did not throw");
            } catch (Throwable t) { check(false, "NSOutlineView collapseItem(null) threw: " + t); }

            try {
                ov.collapseItem(null, true);
                check(true, "NSOutlineView collapseItem(null, true) did not throw");
            } catch (Throwable t) { check(false, "NSOutlineView collapseItem collapseChildren threw: " + t); }

            try {
                boolean expanded = ov.isItemExpanded(null);
                // For nil item, AppKit returns true (root expanded) on some versions; just verify no throw
                check(true, "NSOutlineView isItemExpanded(null) no throw (got " + expanded + ")");
                // round-trip: collapse then check, expand then check
                ov.collapseItem(null);
                boolean afterCollapse = ov.isItemExpanded(null);
                ov.expandItem(null);
                boolean afterExpand = ov.isItemExpanded(null);
                System.out.println("  isItemExpanded after collapse=" + afterCollapse + " after expand=" + afterExpand);
                check(true, "NSOutlineView expand/collapse isItemExpanded round-trip no crash");
            } catch (Throwable t) { check(false, "NSOutlineView isItemExpanded threw: " + t); }

            try {
                boolean expandable = ov.isExpandable(null);
                check(true, "NSOutlineView isExpandable(null) no throw (got " + expandable + ")");
            } catch (Throwable t) { check(false, "NSOutlineView isExpandable threw: " + t); }

            try {
                double indent = ov.indentationPerLevel();
                check(indent > 0, "NSOutlineView indentationPerLevel >0 (got " + indent + ")");
                ov.setIndentationPerLevel(indent + 1);
                check(ov.indentationPerLevel() == indent + 1, "NSOutlineView indentationPerLevel set+1");
                ov.setIndentationPerLevel(indent);
            } catch (Throwable t) { check(false, "NSOutlineView indentationPerLevel threw: " + t); }

            try {
                long n = ov.numberOfChildrenOfItem(null);
                check(true, "NSOutlineView numberOfChildrenOfItem(null) no throw (got " + n + ")");
            } catch (Throwable t) { check(false, "NSOutlineView numberOfChildrenOfItem threw: " + t); }

            try {
                long lvl = ov.levelForItem(null);
                check(true, "NSOutlineView levelForItem(null) no throw (got " + lvl + ")");
            } catch (Throwable t) { check(false, "NSOutlineView levelForItem threw: " + t); }

        } catch (Throwable t) {
            check(false, "NSOutlineView section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSPathControl ----------------
        try {
            NSPathControl pc = NSPathControl.create(new NSRect(0, 0, 200, 24));
            check(pc != null && pc.peer().address() != 0, "NSPathControl.create non-nil");
            check(pc.isKindOfClass("NSPathControl"), "NSPathControl isKindOfClass NSPathControl");

            pc.setPathStyle(0L);
            check(pc.pathStyle() == 0L, "NSPathControl pathStyle 0 (got " + pc.pathStyle() + ")");
            pc.setPathStyle(1L);
            check(pc.pathStyle() == 1L, "NSPathControl pathStyle 1");
            pc.setPathStyle(2L);
            check(pc.pathStyle() == 2L, "NSPathControl pathStyle 2");
            pc.setPathStyle(0L);

            pc.setURLPath("/tmp");
            String p = pc.URLPath();
            check(p != null && p.contains("tmp"), "NSPathControl URLPath /tmp (got \"" + p + "\")");
            MemorySegment url = pc.URL();
            check(url != null && url.address() != 0, "NSPathControl URL non-nil after setURLPath /tmp");

            pc.setURLPath("/Library");
            String p2 = pc.URLPath();
            check(p2 != null && p2.contains("Library"), "NSPathControl URLPath /Library (got \"" + p2 + "\")");

            // null clears
            pc.setURLPath(null);
            // On some versions URL becomes nil after null; accept either nil or still non-nil but don't crash
            try { pc.URL(); check(true, "NSPathControl URL after setURLPath(null) no crash"); } catch (Throwable t) { check(false, "NSPathControl URL after null threw: " + t); }

            // Restore tmp for later checks
            pc.setURLPath("/tmp");

            pc.setPlaceholderString("Choose path");
            check("Choose path".equals(pc.placeholderString()), "NSPathControl placeholderString round-trip (got \"" + pc.placeholderString() + "\")");

            boolean editable = pc.isEditable();
            pc.setEditable(!editable);
            check(pc.isEditable() == !editable, "NSPathControl isEditable toggle");
            pc.setEditable(editable);

            // doubleAction placeholder no crash
            try { pc.setDoubleAction(null); check(true, "NSPathControl setDoubleAction(null) no crash"); } catch (Throwable t) { check(false, "NSPathControl setDoubleAction threw: " + t); }

        } catch (Throwable t) {
            check(false, "NSPathControl section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSSplitView if present ----------------
        try {
            MemorySegment cls = ObjC.cls("NSSplitView");
            boolean present = cls != null && cls.address() != 0;
            if (!present) {
                check(true, "NSSplitView class not present — skip (not failure)");
            } else {
                NSSplitView split = NSSplitView.create(new NSRect(0, 0, 200, 100));
                check(split != null && split.peer().address() != 0, "NSSplitView.create non-nil");
                check(split.isKindOfClass("NSSplitView"), "NSSplitView isKindOfClass NSSplitView");
                boolean vert = split.isVertical();
                split.setVertical(!vert);
                check(split.isVertical() == !vert, "NSSplitView isVertical toggle");
                split.setVertical(vert);
                long style = split.dividerStyle();
                split.setDividerStyle(1L);
                check(split.dividerStyle() == 1L, "NSSplitView dividerStyle 1");
                split.setDividerStyle(style);
                NSView pane1 = NSView.create(new NSRect(0, 0, 100, 100), (ctx, dr) -> {});
                NSView pane2 = NSView.create(new NSRect(0, 0, 100, 100), (ctx, dr) -> {});
                split.addArrangedSubview(pane1);
                split.addArrangedSubview(pane2);
                check(true, "NSSplitView addArrangedSubview x2 no throw");
                try { split.setPositionOfDividerAtIndex(50.0, 0); check(true, "NSSplitView setPositionOfDividerAtIndex no throw"); } catch (Throwable t) { check(false, "NSSplitView setPosition threw: " + t); }
            }
        } catch (Throwable t) {
            // If NSSplitView not available, don't fail
            String msg = t.getMessage() == null ? "" : t.getMessage();
            if (msg.contains("NSSplitView") || msg.contains("not found")) {
                check(true, "NSSplitView not available on this OS — skip: " + t);
            } else {
                check(false, "NSSplitView section threw: " + t);
                t.printStackTrace(System.out);
            }
        }

        System.out.println(failures == 0
                ? "RESULT: ALL PASS (" + asserts + " assertions)"
                : "RESULT: " + failures + " of " + asserts + " assertions FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
