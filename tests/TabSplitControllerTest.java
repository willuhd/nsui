package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSArray;
import nsui.NSTabView;
import nsui.NSTabViewController;
import nsui.NSTabViewItem;
import nsui.NSRect;
import nsui.NSSplitViewController;
import nsui.NSView;
import nsui.NSViewController;
import nsui.objc.ObjC;

/// TabSplitControllerTest — creation and round-trips for NSTabViewController
/// and NSSplitViewController. Pure-memory: no windows shown.
public final class TabSplitControllerTest {

    public static void main(String[] args) {
        ObjC.init();
        TestKit.app();

        // ---------------- NSTabViewController ----------------
        NSTabViewController tvc = NSTabViewController.create();
        TestKit.check(tvc != null && tvc.peer().address() != 0, "NSTabViewController.create non-nil peer");
        TestKit.check(tvc.isKindOfClass("NSTabViewController"), "NSTabViewController isKindOfClass");
        TestKit.check(NSTabViewController.wrap(null) == null, "NSTabViewController.wrap(null) == null");
        TestKit.check(NSTabViewController.wrap(MemorySegment.NULL) == null, "NSTabViewController.wrap(NULL) == null");

        long origStyle = tvc.tabStyle();
        tvc.setTabStyle(1);
        TestKit.check(tvc.tabStyle() == 1, "NSTabViewController tabStyle set 1 -> 1");
        tvc.setTabStyle(origStyle);
        TestKit.check(tvc.tabStyle() == origStyle, "NSTabViewController tabStyle restored to " + origStyle);

        NSTabView tv = tvc.tabView();
        TestKit.check(tv != null && tv.peer().address() != 0, "NSTabViewController tabView non-nil");
        TestKit.check(tv.isKindOfClass("NSTabView"), "NSTabViewController tabView isKindOfClass NSTabView");

        long origTrans = tvc.transitionOptions();
        long flipTrans = (origTrans == 1) ? 2 : 1;
        tvc.setTransitionOptions(flipTrans);
        TestKit.check(tvc.transitionOptions() == flipTrans, "NSTabViewController transitionOptions round-trip " + flipTrans);
        tvc.setTransitionOptions(origTrans);
        TestKit.check(tvc.transitionOptions() == origTrans, "NSTabViewController transitionOptions restored");

        boolean origProp = tvc.canPropagateSelectedChildViewControllerTitle();
        tvc.setCanPropagateSelectedChildViewControllerTitle(!origProp);
        TestKit.check(tvc.canPropagateSelectedChildViewControllerTitle() == !origProp,
                "NSTabViewController canPropagate... toggled");
        tvc.setCanPropagateSelectedChildViewControllerTitle(origProp);
        TestKit.check(tvc.canPropagateSelectedChildViewControllerTitle() == origProp,
                "NSTabViewController canPropagate... restored");

        // items must wrap a view controller or AppKit throws: use the class factory.
        NSViewController vc1 = NSViewController.withView(NSView.create(new NSRect(0, 0, 100, 60), (ctx, d) -> {}));
        NSViewController vc2 = NSViewController.withView(NSView.create(new NSRect(0, 0, 100, 60), (ctx, d) -> {}));
        NSTabViewItem item1 = NSTabViewItem.wrap(ObjC.msgSendIdId(
                ObjC.cls("NSTabViewItem"), ObjC.sel("tabViewItemWithViewController:"), vc1.peer()));
        NSTabViewItem item2 = NSTabViewItem.wrap(ObjC.msgSendIdId(
                ObjC.cls("NSTabViewItem"), ObjC.sel("tabViewItemWithViewController:"), vc2.peer()));
        TestKit.check(item1 != null && item2 != null, "NSTabViewItem tabViewItemWithViewController: x2 non-nil");

        tvc.addTabViewItem(item1);
        TestKit.check(tvc.tabViewItems().count() == 1, "NSTabViewController addTabViewItem -> count 1");
        tvc.insertTabViewItem(item2, 0);
        NSArray tabItems = tvc.tabViewItems();
        TestKit.check(tabItems.count() == 2, "NSTabViewController insertTabViewItem:atIndex:0 -> count 2");
        tvc.setSelectedTabViewItemIndex(1);
        TestKit.check(tvc.selectedTabViewItemIndex() == 1, "NSTabViewController selectedTabViewItemIndex set 1 -> 1");
        tvc.removeTabViewItem(item1);
        TestKit.check(tvc.tabViewItems().count() == 1, "NSTabViewController removeTabViewItem -> count 1");
        tvc.removeTabViewItem(item2);
        TestKit.check(tvc.tabViewItems().count() == 0, "NSTabViewController removeTabViewItem -> count 0");

        // ---------------- NSSplitViewController ----------------
        NSSplitViewController svc = NSSplitViewController.create();
        TestKit.check(svc != null && svc.peer().address() != 0, "NSSplitViewController.create non-nil peer");
        TestKit.check(svc.isKindOfClass("NSSplitViewController"), "NSSplitViewController isKindOfClass");
        TestKit.check(NSSplitViewController.wrap(null) == null, "NSSplitViewController.wrap(null) == null");

        MemorySegment sv = svc.splitView();
        TestKit.check(sv != null && sv.address() != 0, "NSSplitViewController splitView non-nil");
        TestKit.check(nsui.NSObject.wrap(sv).isKindOfClass("NSSplitView"), "NSSplitViewController splitView isKindOfClass NSSplitView");
        TestKit.check(svc.splitViewItems().count() == 0, "NSSplitViewController splitViewItems initially 0");

        double origThick = svc.minimumThicknessForInlineSidebars();
        svc.setMinimumThicknessForInlineSidebars(250.0);
        TestKit.check(svc.minimumThicknessForInlineSidebars() == 250.0,
                "NSSplitViewController minimumThicknessForInlineSidebars set 250 -> 250");
        svc.setMinimumThicknessForInlineSidebars(origThick);
        TestKit.check(svc.minimumThicknessForInlineSidebars() == origThick,
                "NSSplitViewController minimumThicknessForInlineSidebars restored to " + origThick);

        NSViewController svc1 = NSViewController.withView(NSView.create(new NSRect(0, 0, 120, 200), (ctx, d) -> {}));
        NSViewController svc2 = NSViewController.withView(NSView.create(new NSRect(0, 0, 120, 200), (ctx, d) -> {}));
        MemorySegment sitem1 = ObjC.msgSendIdId(
                ObjC.cls("NSSplitViewItem"), ObjC.sel("splitViewItemWithViewController:"), svc1.peer());
        MemorySegment sitem2 = ObjC.msgSendIdId(
                ObjC.cls("NSSplitViewItem"), ObjC.sel("splitViewItemWithViewController:"), svc2.peer());
        TestKit.check(sitem1.address() != 0 && sitem2.address() != 0, "NSSplitViewItem splitViewItemWithViewController: x2 non-nil");

        svc.addSplitViewItem(sitem1);
        TestKit.check(svc.splitViewItems().count() == 1, "NSSplitViewController addSplitViewItem -> count 1");
        svc.insertSplitViewItem(sitem2, 0);
        TestKit.check(svc.splitViewItems().count() == 2, "NSSplitViewController insertSplitViewItem:atIndex:0 -> count 2");
        svc.removeSplitViewItem(sitem1);
        TestKit.check(svc.splitViewItems().count() == 1, "NSSplitViewController removeSplitViewItem -> count 1");
        svc.removeSplitViewItem(sitem2);
        TestKit.check(svc.splitViewItems().count() == 0, "NSSplitViewController removeSplitViewItem -> count 0");

        TestKit.noThrow("NSSplitViewController toggleSidebar:null no-throw (no sidebar)", () -> svc.toggleSidebar(null));
        TestKit.noThrow("NSSplitViewController toggleInspector:null no-throw (no inspector)", () -> svc.toggleInspector(null));

        TestKit.end();
    }
}
