package nsui.tests;

import nsui.NSArray;
import nsui.NSAttributedString;
import nsui.NSBundle;
import nsui.NSColor;
import nsui.NSColorList;
import nsui.NSHelpManager;
import nsui.NSViewController;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// HelpColorListTest — round-trips for NSHelpManager and NSColorList.
/// Pure-memory: Help Viewer is never opened (openHelpAnchor:/findString: are
/// verified via respondsToSelector: only); nothing is saved to disk.
public final class HelpColorListTest {

    private static boolean respondsTo(java.lang.foreign.MemorySegment obj, String sel) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID))
                    .invokeExact(obj, ObjC.sel("respondsToSelector:"), ObjC.sel(sel));
        } catch (Throwable t) { throw new RuntimeException("respondsToSelector: failed", t); }
    }

    public static void main(String[] args) {
        ObjC.init();
        TestKit.app();

        // ---------------- NSHelpManager ----------------
        NSHelpManager help = NSHelpManager.sharedHelpManager();
        TestKit.check(help != null && help.peer().address() != 0, "NSHelpManager.sharedHelpManager non-nil");
        TestKit.check(help.isKindOfClass("NSHelpManager"), "NSHelpManager isKindOfClass");
        TestKit.check(NSHelpManager.wrap(null) == null, "NSHelpManager.wrap(null) == null");

        boolean modeActive = NSHelpManager.isContextHelpModeActive();
        NSHelpManager.setContextHelpModeActive(modeActive);
        TestKit.check(NSHelpManager.isContextHelpModeActive() == modeActive,
                "NSHelpManager contextHelpModeActive no-op write reads back " + modeActive);

        NSViewController target = NSViewController.create();
        TestKit.check(help.contextHelpForObject(target) == null, "NSHelpManager contextHelpForObject initially nil");
        NSAttributedString tip = NSAttributedString.create("Test context help");
        help.setContextHelp(tip, target);
        NSAttributedString gotTip = help.contextHelpForObject(target);
        TestKit.check(gotTip != null && gotTip.peer().address() != 0, "NSHelpManager contextHelpForObject non-nil after set");
        TestKit.check(gotTip != null && "Test context help".equals(gotTip.string()),
                "NSHelpManager context help string round-trip");
        help.removeContextHelpForObject(target);
        TestKit.check(help.contextHelpForObject(target) == null, "NSHelpManager contextHelpForObject nil after remove");
        help.removeContextHelpForObject(target);
        TestKit.check(true, "NSHelpManager removeContextHelpForObject twice no-throw");

        TestKit.check(respondsTo(help.peer(), "openHelpAnchor:inBook:"),
                "NSHelpManager respondsTo openHelpAnchor:inBook: (not invoked: opens Help Viewer)");
        TestKit.check(respondsTo(help.peer(), "findString:inBook:"),
                "NSHelpManager respondsTo findString:inBook: (not invoked: opens Help Viewer)");

        NSBundle main = NSBundle.mainBundle();
        if (main == null) {
            TestKit.check(true, "NSHelpManager registerBooksInBundle skipped (no mainBundle)");
        } else {
            boolean registered = help.registerBooksInBundle(main);
            TestKit.check(true, "NSHelpManager registerBooksInBundle(mainBundle) no-throw, returned " + registered);
        }

        // ---------------- NSColorList ----------------
        NSArray available = NSColorList.availableColorLists();
        TestKit.check(available != null, "NSColorList.availableColorLists non-nil (count "
                + (available == null ? -1 : available.count()) + ")");
        TestKit.check(NSColorList.colorListNamed("nsui-no-such-list-xyz") == null,
                "NSColorList.colorListNamed: unknown name -> nil");
        TestKit.check(NSColorList.wrap(null) == null, "NSColorList.wrap(null) == null");

        NSColorList list = NSColorList.create("nsui-test-list");
        TestKit.check(list != null && list.peer().address() != 0, "NSColorList.create non-nil peer");
        TestKit.check(list.isKindOfClass("NSColorList"), "NSColorList isKindOfClass");
        TestKit.check("nsui-test-list".equals(list.name()), "NSColorList name round-trip");
        TestKit.check(list.isEditable(), "NSColorList fresh list isEditable");
        TestKit.check(list.allKeys().count() == 0, "NSColorList allKeys initially 0");

        list.setColor(NSColor.redColor(), "TestRed");
        NSColor got = list.colorWithKey("TestRed");
        TestKit.check(got != null && got.peer().address() != 0, "NSColorList colorWithKey: non-nil after set");
        TestKit.check(got.isKindOfClass("NSColor"), "NSColorList stored color isKindOfClass NSColor");
        NSArray keys = list.allKeys();
        TestKit.check(keys.count() == 1 && "TestRed".equals(keys.stringAt(0).string()),
                "NSColorList allKeys has TestRed");

        list.removeColorWithKey("TestRed");
        TestKit.check(list.colorWithKey("TestRed") == null, "NSColorList colorWithKey nil after remove");
        TestKit.check(list.allKeys().count() == 0, "NSColorList allKeys 0 after remove");
        list.removeColorWithKey("TestRed");
        TestKit.check(true, "NSColorList removeColorWithKey: absent key no-throw");

        TestKit.end();
    }
}
