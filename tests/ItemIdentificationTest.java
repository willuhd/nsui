package nsui.tests;

import nsui.NSToolbarItem;
import nsui.NSUserInterfaceItemIdentification;
import nsui.objc.ObjC;

/// NSUserInterfaceItemIdentification round-trip on NSToolbarItem.
///
/// - Create NSToolbarItem with identifier "test.item.1";
/// - Check identifier() returns the creation identifier (or setIdentifier round-trip);
/// - setIdentifier("com.example.second") then identifier() == that string;
/// - Check instanceof NSUserInterfaceItemIdentification;
/// - Stress 200 iterations: set/get loop no crash.
public final class ItemIdentificationTest {

    public static void main(String[] args) throws Throwable {
        System.out.println("=== ItemIdentificationTest — NSToolbarItem identifier round-trip ===");
        ObjC.init();

        NSToolbarItem item = NSToolbarItem.create("test.item.1");
        TestKit.check(item != null && item.peer().address() != 0, "NSToolbarItem.create(\"test.item.1\") non-nil");

        // Interface conformance
        TestKit.check(item instanceof NSUserInterfaceItemIdentification,
                "NSToolbarItem instanceof NSUserInterfaceItemIdentification [got " + (item instanceof NSUserInterfaceItemIdentification) + "]");

        // initial identifier — toolbar item's itemIdentifier is "test.item.1"; identifier may mirror it or be distinct
        // For NSToolbarItem, `identifier` is not the same as `itemIdentifier` — the former is NSUserInterfaceItemIdentification
        // On some OS versions it may initially be nil or equal to itemIdentifier; we set and verify round-trip regardless
        String initial = item.identifier();
        System.out.println("  initial identifier = \"" + initial + "\"");
        // Also verify itemIdentifier remains stable
        String itemId = item.itemIdentifier();
        System.out.println("  itemIdentifier = \"" + itemId + "\"");
        TestKit.check("test.item.1".equals(itemId), "itemIdentifier round-trip == \"test.item.1\" [got \"" + itemId + "\"]");

        // setIdentifier round-trip
        item.setIdentifier("com.example.second");
        String after = item.identifier();
        System.out.println("  after setIdentifier(\"com.example.second\") -> \"" + after + "\"");
        TestKit.check("com.example.second".equals(after), "identifier round-trip after setIdentifier(\"com.example.second\") [got \"" + after + "\"]");

        // second mutation
        item.setIdentifier("com.example.third");
        String after2 = item.identifier();
        TestKit.check("com.example.third".equals(after2), "identifier round-trip second mutation [got \"" + after2 + "\"]");

        // nil clear (should not crash; result may be nil)
        try {
            item.setIdentifier(null);
            String cleared = item.identifier();
            System.out.println("  after setIdentifier(null) -> " + (cleared == null ? "null" : "\"" + cleared + "\""));
            // Accept null or empty as cleared; just ensure no crash and not still previous value
            boolean clearedOk = cleared == null || cleared.isEmpty() || !cleared.equals("com.example.third");
            TestKit.check(clearedOk, "identifier after setIdentifier(null) cleared or nil [got " + (cleared == null ? "null" : "\"" + cleared + "\"") + "]");
            // restore for stress
            item.setIdentifier("com.example.stress");
        } catch (Throwable t) {
            TestKit.check(false, "setIdentifier(null) threw: " + t);
        }

        // Direct NSUserInterfaceItemIdentification interface use
        NSUserInterfaceItemIdentification iid = item;
        iid.setIdentifier("via.interface.id");
        TestKit.check("via.interface.id".equals(iid.identifier()), "NSUserInterfaceItemIdentification interface set/get [got \"" + iid.identifier() + "\"]");

        // Stress 200 iterations
        System.out.println("  stress: 200x setIdentifier/get ...");
        boolean stressOk = true;
        for (int i = 0; i < 200; i++) {
            String id = "stress.id." + i;
            item.setIdentifier(id);
            String got = item.identifier();
            if (!id.equals(got)) {
                stressOk = false;
                TestKit.check(false, "stress iteration " + i + ": expected \"" + id + "\" got \"" + got + "\"");
                break;
            }
        }
        if (stressOk) {
            TestKit.probe("stress 200 iterations identifier round-trip no crash");
        }

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }
}
