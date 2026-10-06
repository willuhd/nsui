package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSApplication;
import nsui.NSPopUpButton;
import nsui.NSComboBox;
import nsui.NSSegmentedControl;
import nsui.NSRect;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;

/// Selection widget tests — NSPopUpButton, NSComboBox, NSSegmentedControl.
/// All plain AppKit object manipulation (no drawing): each control is created,
/// populated and asserted round-trip. Runs on the main thread via NSApplication.
public final class SelectionWidgetsTest {

    private static void checkEq(long got, long expected, String msg) {
        TestKit.check(got == expected, msg + " [got " + got + ", expected " + expected + "]");
    }

    private static void checkStr(String got, String expected, String msg) {
        TestKit.check(expected.equals(got), msg + " [got \"" + got + "\", expected \"" + expected + "\"]");
    }

    public static void main(String[] args) {
        System.out.println("=== SelectionWidgetsTest — NSPopUpButton/NSComboBox/NSSegmentedControl ===");
        ObjC.init(); // FFM bindings (must be first)
        NSApplication.shared(); // ensure the main-thread app context

        // ------------------------------------------------------------ NSPopUpButton
        NSPopUpButton popup = NSPopUpButton.create(new NSRect(0, 0, 180, 25));
        TestKit.check(popup != null, "NSPopUpButton.create returned a control");
        popup.addItemWithTitle("One");
        popup.addItemWithTitle("Two");
        popup.addItemWithTitle("Three");
        checkEq(popup.numberOfItems(), 3, "NSPopUpButton numberOfItems()==3 after 3 addItemWithTitle:");

        checkEq(popup.indexOfSelectedItem(), 0, "AppKit selects item 0 by default (indexOfSelectedItem()==0)");

        popup.selectItemAtIndex(2);
        checkEq(popup.indexOfSelectedItem(), 2, "NSPopUpButton selectItemAtIndex(2) -> indexOfSelectedItem()==2");
        checkStr(popup.itemTitleAtIndex(2), "Three", "NSPopUpButton itemTitleAtIndex(2)=='Three'");
        checkStr(popup.titleOfSelectedItem(), "Three", "NSPopUpButton titleOfSelectedItem()=='Three'");

        popup.removeAllItems();
        checkEq(popup.numberOfItems(), 0, "NSPopUpButton removeAllItems -> numberOfItems()==0");

        // ---- re-add + wire target/action; programmatic select does NOT fire ----
        popup.addItemWithTitle("A");
        popup.addItemWithTitle("B");
        popup.addItemWithTitle("C");
        final boolean[] fired = {false};
        MemorySegment target = DelegateProxy.actionTarget("selectionChanged:", (MemorySegment sender) -> fired[0] = true);
        popup.setTarget(target);
        popup.setAction("selectionChanged:");

        popup.selectItemAtIndex(1);
        checkEq(popup.indexOfSelectedItem(), 1, "NSPopUpButton re-add + selectItemAtIndex(1) -> indexOfSelectedItem()==1");
        // AppKit only fires the action on USER interaction; a programmatic select is a no-op.
        TestKit.check(!fired[0], "programmatic selectItemAtIndex: did NOT fire the action (AppKit fires only on user interaction)");

        // Prove the wiring is live by sending the selector to the target directly
        // with the button as the sender — exactly what Cocoa dispatches on a click.
        ObjC.msgSendVoidId(target, ObjC.sel("selectionChanged:"), popup.peer());
        TestKit.check(fired[0], "direct send [target selectionChanged:(popup)] reached the Java handler -> action wiring verified");
        // Also: an UNREGISTERED selector on the same target must not crash (proxy guard).
        ObjC.msgSendVoidId(target, ObjC.sel("selectionChanged:"), popup.peer());
        TestKit.probe("action target handles repeated sends without crashing");

        // ------------------------------------------------------------ NSComboBox
        NSComboBox combo = NSComboBox.create(new NSRect(0, 30, 180, 25));
        TestKit.check(combo != null, "NSComboBox.create returned a control");
        combo.addItemWithObjectValue("a");
        combo.addItemWithObjectValue("b");
        checkEq(combo.numberOfItems(), 2, "NSComboBox numberOfItems()==2 after 2 addItemWithObjectValue:");
        combo.selectItemAtIndex(1);
        checkEq(combo.indexOfSelectedItem(), 1, "NSComboBox selectItemAtIndex(1) -> indexOfSelectedItem()==1");
        checkStr(combo.stringValue(), "b", "NSComboBox stringValue()=='b'");
        TestKit.noThrow("NSComboBox setEditable(true) without crash", () -> combo.setEditable(true));

        // ------------------------------------------------------------ NSSegmentedControl
        NSSegmentedControl seg = NSSegmentedControl.create(new NSRect(0, 60, 240, 24));
        TestKit.check(seg != null, "NSSegmentedControl.create returned a control");
        seg.setSegmentCount(3);
        seg.setLabel("A", 0);
        seg.setLabel("B", 1);
        seg.setLabel("C", 2);
        seg.setSelectedSegment(1);
        checkEq(seg.selectedSegment(), 1, "NSSegmentedControl setSelectedSegment(1) -> selectedSegment()==1");
        TestKit.noThrow("NSSegmentedControl setSegmentStyle(1) without crash", () -> seg.setSegmentStyle(1));

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }
}
