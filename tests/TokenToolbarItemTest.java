package nsui.tests;

import nsui.NSMenu;
import nsui.NSMenuToolbarItem;
import nsui.NSRect;
import nsui.NSSearchField;
import nsui.NSSearchToolbarItem;
import nsui.NSTokenField;
import nsui.objc.ObjC;

/// TokenToolbarItemTest — round-trips for NSTokenField, NSMenuToolbarItem and
/// NSSearchToolbarItem. Pure-memory: no windows shown, no first-responder theft
/// (begin/endSearchInteraction run windowless and must simply not throw).
public final class TokenToolbarItemTest {

    public static void main(String[] args) {
        ObjC.init();
        TestKit.app();

        // ---------------- NSTokenField ----------------
        NSTokenField token = NSTokenField.create(new NSRect(0, 0, 220, 24));
        TestKit.check(token != null && token.peer().address() != 0, "NSTokenField.create non-nil peer");
        TestKit.check(token.isKindOfClass("NSTokenField"), "NSTokenField isKindOfClass NSTokenField");
        TestKit.check(NSTokenField.wrap(null) == null, "NSTokenField.wrap(null) == null");

        // NOTE: strings containing tokenizing characters (e.g. commas) are normalized into
        // tokens on read-back (probe: "red, green" reads back "red,green"), so round-trip a plain word.
        token.setStringValue("hello");
        TestKit.check("hello".equals(token.stringValue()), "NSTokenField stringValue round-trip");

        long origStyle = token.tokenStyle();
        token.setTokenStyle(2);
        TestKit.check(token.tokenStyle() == 2, "NSTokenField tokenStyle set 2 -> 2");
        token.setTokenStyle(origStyle);
        TestKit.check(token.tokenStyle() == origStyle, "NSTokenField tokenStyle restored to " + origStyle);

        double origDelay = token.completionDelay();
        token.setCompletionDelay(0.25);
        TestKit.check(token.completionDelay() == 0.25, "NSTokenField completionDelay set 0.25 -> 0.25");
        token.setCompletionDelay(origDelay);
        TestKit.check(token.completionDelay() == origDelay, "NSTokenField completionDelay restored to " + origDelay);

        // ---------------- NSMenuToolbarItem ----------------
        NSMenuToolbarItem menuItem = NSMenuToolbarItem.create("nsui-test-menu");
        TestKit.check(menuItem != null && menuItem.peer().address() != 0, "NSMenuToolbarItem.create non-nil peer");
        TestKit.check(menuItem.isKindOfClass("NSMenuToolbarItem"), "NSMenuToolbarItem isKindOfClass");
        TestKit.check(menuItem.isKindOfClass("NSToolbarItem"), "NSMenuToolbarItem native lineage isKindOfClass NSToolbarItem");
        TestKit.check("nsui-test-menu".equals(menuItem.itemIdentifier()), "NSMenuToolbarItem itemIdentifier round-trip");

        NSMenu menu = NSMenu.createWithTitle("TestMenu");
        menuItem.setMenu(menu);
        NSMenu gotMenu = menuItem.menu();
        TestKit.check(gotMenu != null && gotMenu.peer().address() == menu.peer().address(),
                "NSMenuToolbarItem menu round-trip (same peer)");
        TestKit.check("TestMenu".equals(gotMenu.title()), "NSMenuToolbarItem menu title round-trip");
        menuItem.setMenu(null);
        TestKit.check(menuItem.menu() == null, "NSMenuToolbarItem setMenu:null -> menu nil");

        boolean origInd = menuItem.showsIndicator();
        menuItem.setShowsIndicator(!origInd);
        TestKit.check(menuItem.showsIndicator() == !origInd, "NSMenuToolbarItem showsIndicator toggled");
        menuItem.setShowsIndicator(origInd);
        TestKit.check(menuItem.showsIndicator() == origInd, "NSMenuToolbarItem showsIndicator restored");

        // ---------------- NSSearchToolbarItem ----------------
        NSSearchToolbarItem searchItem = NSSearchToolbarItem.create("nsui-test-search");
        TestKit.check(searchItem != null && searchItem.peer().address() != 0, "NSSearchToolbarItem.create non-nil peer");
        TestKit.check(searchItem.isKindOfClass("NSSearchToolbarItem"), "NSSearchToolbarItem isKindOfClass");
        TestKit.check(searchItem.isKindOfClass("NSToolbarItem"), "NSSearchToolbarItem native lineage isKindOfClass NSToolbarItem");

        NSSearchField defField = searchItem.searchField();
        TestKit.check(defField != null && defField.peer().address() != 0, "NSSearchToolbarItem default searchField non-nil");
        TestKit.check(defField.isKindOfClass("NSSearchField"), "NSSearchToolbarItem default searchField isKindOfClass");

        NSSearchField custom = NSSearchField.create(new NSRect(0, 0, 160, 24));
        custom.setPlaceholderString("Search things");
        searchItem.setSearchField(custom);
        NSSearchField gotField = searchItem.searchField();
        TestKit.check(gotField != null && gotField.peer().address() == custom.peer().address(),
                "NSSearchToolbarItem searchField round-trip (same peer)");
        TestKit.check("Search things".equals(gotField.placeholderString()), "NSSearchToolbarItem searchField placeholder kept");
        // NOTE: setSearchField:nil reads back nil while windowless (the header-documented
        // reset to a default field is not observable until the item joins a toolbar).
        searchItem.setSearchField(null);
        TestKit.check(searchItem.searchField() == null, "NSSearchToolbarItem setSearchField:nil -> searchField nil (windowless)");
        searchItem.setSearchField(custom);
        TestKit.check(searchItem.searchField().peer().address() == custom.peer().address(),
                "NSSearchToolbarItem searchField re-set after nil round-trips");

        boolean origResign = searchItem.resignsFirstResponderWithCancel();
        searchItem.setResignsFirstResponderWithCancel(!origResign);
        TestKit.check(searchItem.resignsFirstResponderWithCancel() == !origResign,
                "NSSearchToolbarItem resignsFirstResponderWithCancel toggled");
        searchItem.setResignsFirstResponderWithCancel(origResign);
        TestKit.check(searchItem.resignsFirstResponderWithCancel() == origResign,
                "NSSearchToolbarItem resignsFirstResponderWithCancel restored");

        double origWidth = searchItem.preferredWidthForSearchField();
        searchItem.setPreferredWidthForSearchField(200.0);
        TestKit.check(searchItem.preferredWidthForSearchField() == 200.0,
                "NSSearchToolbarItem preferredWidthForSearchField set 200 -> 200");
        searchItem.setPreferredWidthForSearchField(origWidth);
        TestKit.check(searchItem.preferredWidthForSearchField() == origWidth,
                "NSSearchToolbarItem preferredWidthForSearchField restored to " + origWidth);

        searchItem.beginSearchInteraction();
        TestKit.check(true, "NSSearchToolbarItem beginSearchInteraction no-throw (windowless)");
        searchItem.endSearchInteraction();
        TestKit.check(true, "NSSearchToolbarItem endSearchInteraction no-throw (windowless)");

        TestKit.end();
    }
}
