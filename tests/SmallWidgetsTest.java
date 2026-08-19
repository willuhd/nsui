package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSBox;
import nsui.NSColor;
import nsui.NSColorWell;
import nsui.NSDatePicker;
import nsui.NSLevelIndicator;
import nsui.NSRect;
import nsui.NSStepper;
import nsui.NSTabView;
import nsui.NSTabViewItem;
import nsui.NSView;
import nsui.objc.ObjC;

/**
 * Small-widget batch: NSDatePicker, NSColorWell, NSBox, NSStepper, NSLevelIndicator,
 * NSTabView + NSTabViewItem.
 *
 * <ul>
 *   <li>NSDatePicker: style/elements/date round-trip, no crash;</li>
 *   <li>NSColorWell: setColor + color() reads back non-nil (raw NSColor id), activate/deactivate;</li>
 *   <li>NSBox: title round-trip, box/border/titlePosition setters;</li>
 *   <li>NSStepper: min/max/increment/value + value beyond max (AppKit clamps);</li>
 *   <li>NSLevelIndicator: style + range/value;</li>
 *   <li>NSTabView + NSTabViewItem: two labeled tabs with content views.</li>
 * </ul>
 *
 * All AppKit activity runs on the main thread ({@code -XstartOnFirstThread}).
 */
public final class SmallWidgetsTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== SmallWidgetsTest — NSDatePicker/NSColorWell/NSBox/NSStepper/NSLevelIndicator/NSTabView ===");
        ObjC.init();           // FFM bindings (must be first)

        // ------------------------------------------------------------ NSDatePicker
        NSDatePicker picker = NSDatePicker.create(new NSRect(0, 0, 180, 27));
        picker.setDatePickerElements(0x3L);                 // NSHourMinuteDatePickerElementFlag
        picker.setDatePickerStyle(1L);                      // NSDatePickerStyleTextFieldAndStepper
        MemorySegment now = ObjC.msgSendIdDouble(
                ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.0);
        check(now != null && now.address() != 0, "NSDate created via dateWithTimeIntervalSinceNow: 0 (non-nil)");
        picker.setDateValue(now);
        MemorySegment gotDate = picker.dateValue();
        check(gotDate != null && gotDate.address() != 0, "NSDatePicker.dateValue() non-nil after setDateValue(now)");
        check(true, "NSDatePicker style(1)/elements(0x3)/setDateValue no crash");

        // ------------------------------------------------------------ NSColorWell
        NSColorWell well = NSColorWell.create(new NSRect(0, 0, 60, 24));
        NSColor c = NSColor.create(0.2, 0.4, 0.6, 1.0);
        well.setColor(c);
        NSColor gotColor = well.color();
        check(gotColor != null && gotColor.peer().address() != 0, "NSColorWell.color() non-nil after setColor");
        if (gotColor != null && gotColor.peer().address() != 0) {
            System.out.println("  color() description = " + gotColor.description());
            // also verify raw segment accessor still works
            MemorySegment seg = well.colorSegment();
            check(seg != null && seg.address() != 0, "NSColorWell.colorSegment() non-nil");
        }
        well.activate(true);
        well.deactivate();
        check(true, "NSColorWell activate(true)/deactivate() no crash");

        // ------------------------------------------------------------ NSBox
        NSBox box = NSBox.create(new NSRect(0, 0, 200, 120));
        box.setTitle("Group");
        String t = box.title();
        check("Group".equals(t), "NSBox.title() == \"Group\" [got \"" + t + "\"]");
        box.setBoxType(0L);         // NSBoxPrimary
        box.setBorderType(0L);      // NSNoBorder
        box.setTitlePosition(0L);   // NSNoTitle
        check(true, "NSBox boxType/borderType/titlePosition setters no crash");
        System.out.println("  NSBox title after setTitlePosition(0)/setBoxType(0) = \"" + box.title() + "\"");

        // ------------------------------------------------------------ NSStepper
        NSStepper stepper = NSStepper.create(new NSRect(0, 0, 19, 27));
        stepper.setMinValue(0.0);
        stepper.setMaxValue(10.0);
        stepper.setIncrement(1.0);
        stepper.setDoubleValue(5.0);
        double v = stepper.doubleValue();
        check(Math.abs(v - 5.0) < 0.01, "NSStepper.doubleValue() == 5.0 [got " + v + "]");
        stepper.setDoubleValue(11.0);
        double vClamp = stepper.doubleValue();
        boolean clamped = Math.abs(vClamp - 10.0) < 0.01;
        check(clamped, "NSStepper clamps 11.0 -> 10.0 (max) [got " + vClamp + "]");
        if (!clamped) System.out.println("  NOTE: AppKit did NOT clamp — observed read-back " + vClamp + " documented as actual behavior");

        // ------------------------------------------------------------ NSLevelIndicator
        NSLevelIndicator ind = NSLevelIndicator.create(new NSRect(0, 0, 120, 16));
        ind.setLevelIndicatorStyle(0L);   // NSLevelIndicatorStyleRelevancy
        ind.setMinValue(0.0);
        ind.setMaxValue(5.0);
        ind.setDoubleValue(3.0);
        double lv = ind.doubleValue();
        check(Math.abs(lv - 3.0) < 0.01, "NSLevelIndicator.doubleValue() == 3.0 [got " + lv + "]");

        // ------------------------------------------------------------ NSTabView + NSTabViewItem
        NSTabView tabView = NSTabView.create(new NSRect(0, 0, 300, 200));
        NSTabViewItem tab1 = NSTabViewItem.create("First");
        tab1.setView(NSView.create(new NSRect(0, 0, 100, 100), (ctx, dr) -> { }));
        NSTabViewItem tab2 = NSTabViewItem.create("Second");
        tab2.setView(NSView.create(new NSRect(0, 0, 100, 100), (ctx, dr) -> { }));
        tabView.addTabViewItem(tab1);
        tabView.addTabViewItem(tab2);
        long n = tabView.numberOfTabViewItems();
        check(n == 2L, "NSTabView.numberOfTabViewItems() == 2 [got " + n + "]");
        String firstLabel = tab1.label();
        check("First".equals(firstLabel), "NSTabViewItem.label() == \"First\" [got \"" + firstLabel + "\"]");

        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
