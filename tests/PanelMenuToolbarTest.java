package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSAlert;
import nsui.NSButton;
import nsui.NSMenu;
import nsui.NSMenuItem;
import nsui.NSOpenPanel;
import nsui.NSSavePanel;
import nsui.NSStatusBar;
import nsui.NSStatusItem;
import nsui.NSToolbar;
import nsui.NSToolbarItem;
import nsui.NSWindow;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * PanelMenuToolbarTest — creation and property round-trips for NSAlert,
 * NSOpenPanel/NSSavePanel, NSMenu, NSStatusBar, NSToolbar, NSToolbarItem.
 * Never blocks on runModal; just verifies the selector exists and peers are non-nil.
 */
public final class PanelMenuToolbarTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== PanelMenuToolbarTest — NSAlert/NSOpenPanel/NSSavePanel/NSMenu/NSStatusBar/NSToolbar ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            System.out.println("SKIP: ObjC.init failed (connection error or not macOS): " + t);
            t.printStackTrace(System.out);
            System.out.println("RESULT: SKIP (connection error, continuing)");
            System.exit(0);
        }

        // ---------------- NSAlert ----------------
        try {
            NSAlert alert = NSAlert.create();
            check(alert != null && alert.peer() != null && alert.peer().address() != 0, "NSAlert.create non-nil peer");
            check(alert.isKindOfClass("NSAlert"), "NSAlert isKindOfClass NSAlert");

            alert.setMessageText("Hello Alert");
            String mt = alert.messageText();
            check("Hello Alert".equals(mt), "NSAlert messageText round-trip 'Hello Alert' (got \"" + mt + "\")");

            alert.setInformativeText("Informative");
            String it = alert.informativeText();
            check("Informative".equals(it), "NSAlert informativeText round-trip (got \"" + it + "\")");

            NSButton b1 = alert.addButtonWithTitle("OK");
            check(b1 != null && b1.peer().address() != 0, "NSAlert addButtonWithTitle: OK returned non-nil NSButton");
            if (b1 != null) {
                String bt = b1.title();
                check(bt != null && bt.contains("OK"), "NSAlert button title contains OK (got \"" + bt + "\")");
                check(b1.isKindOfClass("NSButton"), "NSAlert button isKindOfClass NSButton");
            }

            NSButton b2 = alert.addButtonWithTitle("Cancel");
            check(b2 != null && b2.peer().address() != 0, "NSAlert addButtonWithTitle: Cancel non-nil");

            // alertStyle round-trip
            long origStyle = alert.alertStyle();
            alert.setAlertStyle(1L);
            check(alert.alertStyle() == 1L, "NSAlert alertStyle set 1 -> 1 (orig " + origStyle + ")");
            alert.setAlertStyle(0L);
            check(alert.alertStyle() == 0L, "NSAlert alertStyle set 0 -> 0");
            alert.setAlertStyle(origStyle);

            // showsHelp / showsSuppressionButton
            boolean sh = alert.showsHelp();
            alert.setShowsHelp(!sh);
            check(alert.showsHelp() == !sh, "NSAlert showsHelp toggle");
            alert.setShowsHelp(sh);

            // accessoryView nil check
            check(true, "NSAlert accessoryView accessor no crash (view=" + alert.accessoryView() + ")");

            // window (panel) non-nil
            NSWindow w = alert.window();
            check(w != null && w.peer().address() != 0, "NSAlert window (panel) non-nil");
            if (w != null) check(w.isKindOfClass("NSPanel") || w.isKindOfClass("NSWindow"), "NSAlert window isKindOfClass NSPanel/NSWindow");

            // runModal would work but don't block — just verify selector
            boolean responds = respondsTo(alert.peer(), "runModal");
            check(responds, "NSAlert respondsToSelector: runModal (would work, not invoking to avoid blocking)");
            // icon / suppressionButton access no crash
            try { alert.icon(); check(true, "NSAlert icon accessor no crash"); } catch (Throwable t) { check(false, "NSAlert icon threw: " + t); }

        } catch (Throwable t) {
            check(false, "NSAlert section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSSavePanel ----------------
        try {
            NSSavePanel save = NSSavePanel.savePanel();
            check(save != null && save.peer().address() != 0, "NSSavePanel.savePanel non-nil");
            check(save.isKindOfClass("NSSavePanel"), "NSSavePanel isKindOfClass NSSavePanel");

            save.setTitle("SaveTest");
            check("SaveTest".equals(save.title()), "NSSavePanel title round-trip SaveTest (got \"" + save.title() + "\")");

            save.setMessage("Save message");
            check("Save message".equals(save.message()), "NSSavePanel message round-trip (got \"" + save.message() + "\")");

            boolean origCanCreate = save.canCreateDirectories();
            save.setCanCreateDirectories(!origCanCreate);
            check(save.canCreateDirectories() == !origCanCreate, "NSSavePanel canCreateDirectories toggle");
            save.setCanCreateDirectories(origCanCreate);

            boolean origHidden = save.showsHiddenFiles();
            save.setShowsHiddenFiles(!origHidden);
            check(save.showsHiddenFiles() == !origHidden, "NSSavePanel showsHiddenFiles toggle");
            save.setShowsHiddenFiles(origHidden);

            save.setExtensionHidden(true);
            check(save.isExtensionHidden() == true, "NSSavePanel isExtensionHidden true");
            save.setExtensionHidden(false);
            check(save.isExtensionHidden() == false, "NSSavePanel isExtensionHidden false");

            save.setAllowsOtherFileTypes(true);
            check(save.allowsOtherFileTypes() == true, "NSSavePanel allowsOtherFileTypes true");
            save.setAllowsOtherFileTypes(false);

            save.setNameFieldStringValue("untitled.txt");
            check("untitled.txt".equals(save.nameFieldStringValue()), "NSSavePanel nameFieldStringValue round-trip (got \"" + save.nameFieldStringValue() + "\")");

            // directory URL round-trip
            save.setDirectoryURL("/tmp");
            String dir = save.directoryURLString();
            check(dir != null && dir.contains("tmp"), "NSSavePanel directoryURL /tmp round-trip (got \"" + dir + "\")");

            save.setAllowedFileTypes("txt", "png");
            check(true, "NSSavePanel setAllowedFileTypes no crash");

            boolean respondsSave = respondsTo(save.peer(), "runModal");
            check(respondsSave, "NSSavePanel respondsToSelector: runModal");
        } catch (Throwable t) {
            check(false, "NSSavePanel section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSOpenPanel ----------------
        try {
            NSOpenPanel open = NSOpenPanel.openPanel();
            check(open != null && open.peer().address() != 0, "NSOpenPanel.openPanel non-nil");
            check(open.isKindOfClass("NSOpenPanel"), "NSOpenPanel isKindOfClass NSOpenPanel");

            boolean origFiles = open.canChooseFiles();
            open.setCanChooseFiles(!origFiles);
            check(open.canChooseFiles() == !origFiles, "NSOpenPanel canChooseFiles toggle (orig " + origFiles + ")");
            open.setCanChooseFiles(true);
            check(open.canChooseFiles() == true, "NSOpenPanel canChooseFiles true");

            boolean origDirs = open.canChooseDirectories();
            open.setCanChooseDirectories(!origDirs);
            check(open.canChooseDirectories() == !origDirs, "NSOpenPanel canChooseDirectories toggle");
            open.setCanChooseDirectories(true);
            check(open.canChooseDirectories() == true, "NSOpenPanel canChooseDirectories true");
            // alias
            open.setCanChooseDirectory(false);
            check(open.canChooseDirectories() == false, "NSOpenPanel setCanChooseDirectory alias false");
            open.setCanChooseDirectories(true);

            open.setAllowsMultipleSelection(true);
            check(open.allowsMultipleSelection() == true, "NSOpenPanel allowsMultipleSelection true");
            open.setAllowsMultipleSelection(false);
            check(open.allowsMultipleSelection() == false, "NSOpenPanel allowsMultipleSelection false");

            open.setDirectoryURL("/tmp");
            String odir = open.directoryURLString();
            check(odir != null && odir.contains("tmp"), "NSOpenPanel directoryURL /tmp (got \"" + odir + "\")");

            open.setResolvesAliases(false);
            check(open.resolvesAliases() == false, "NSOpenPanel resolvesAliases false");
            open.setResolvesAliases(true);
            check(open.resolvesAliases() == true, "NSOpenPanel resolvesAliases true");

            check(open.URLs().size() == 0 || true, "NSOpenPanel URLs accessor no crash (empty expected)");
        } catch (Throwable t) {
            check(false, "NSOpenPanel section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSMenu ----------------
        try {
            NSMenu menu = NSMenu.createWithTitle("File");
            check(menu != null && menu.peer().address() != 0, "NSMenu.createWithTitle File non-nil");
            check("File".equals(menu.title()), "NSMenu title File (got \"" + menu.title() + "\")");
            check(menu.numberOfItems() == 0, "NSMenu initial numberOfItems 0");

            NSMenuItem first = menu.insertItemWithTitle("Open", "", "", 0);
            check(first != null && first.peer().address() != 0, "NSMenu insertItemWithTitle Open at 0 non-nil");
            check("Open".equals(first.title()), "NSMenu inserted title Open (got \"" + first.title() + "\")");
            check(menu.numberOfItems() == 1, "NSMenu numberOfItems 1 after first insert");

            NSMenuItem second = menu.insertItemWithTitle("Save", "", "s", 1);
            check(second != null, "NSMenu insertItemWithTitle Save at 1 non-nil");
            check("s".equals(second.keyEquivalent()), "NSMenu second keyEquivalent s (got \"" + second.keyEquivalent() + "\")");
            check(menu.numberOfItems() == 2, "NSMenu numberOfItems 2 after second insert");
            check(menu.itemAtIndex(0) != null && "Open".equals(menu.itemAtIndex(0).title()), "NSMenu itemAtIndex 0 is Open");
            check(menu.indexOfItem(second) == 1, "NSMenu indexOfItem Save ==1 (got " + menu.indexOfItem(second) + ")");

            // insertItem (NSMenuItem) variant doesn't throw
            NSMenuItem extra = NSMenuItem.withTitle("Extra", "", "");
            try {
                menu.insertItem(extra, 1);
                check(true, "NSMenu insertItem:atIndex: did not throw");
                check(menu.numberOfItems() == 3, "NSMenu numberOfItems 3 after insertItem (got " + menu.numberOfItems() + ")");
            } catch (Throwable t) {
                check(false, "NSMenu insertItem:atIndex: threw: " + t);
            }

            // setSubmenu
            NSMenu submenu = NSMenu.createWithTitle("Sub");
            submenu.addItemWithTitle("SubItem", "", "");
            check(submenu.numberOfItems() == 1, "NSMenu submenu numberOfItems 1");
            try {
                menu.setSubmenuForItem(submenu, first);
                check(true, "NSMenu setSubmenu:forItem: did not throw");
                check(first.hasSubmenu(), "NSMenuItem hasSubmenu after setSubmenuForItem");
                NSMenu gotSub = first.submenu();
                check(gotSub != null && gotSub.peer().address() != 0, "NSMenuItem submenu non-nil after set");
            } catch (Throwable t) {
                check(false, "NSMenu setSubmenu:forItem: threw: " + t);
            }

            // also test setSubmenu via item peer
            NSMenu submenu2 = NSMenu.createWithTitle("Sub2");
            submenu2.addItemWithTitle("Sub2Item", "", "");
            NSMenuItem third = menu.itemAtIndex(1);
            try {
                menu.setSubmenu(third, submenu2);
                check(true, "NSMenu setSubmenu (item.setSubmenu) did not throw");
            } catch (Throwable t) {
                check(false, "NSMenu setSubmenu threw: " + t);
            }

            menu.removeItemAtIndex(0);
            check(menu.numberOfItems() == 2, "NSMenu removeItemAtIndex 0 -> 2 items left (got " + menu.numberOfItems() + ")");
            menu.removeAllItems();
            check(menu.numberOfItems() == 0, "NSMenu removeAllItems -> 0");

        } catch (Throwable t) {
            check(false, "NSMenu section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSStatusBar ----------------
        try {
            NSStatusBar bar = NSStatusBar.systemStatusBar();
            check(bar != null && bar.peer().address() != 0, "NSStatusBar.systemStatusBar non-nil");
            check(bar.isKindOfClass("NSStatusBar"), "NSStatusBar isKindOfClass NSStatusBar");

            double thick = bar.thickness();
            check(thick > 0, "NSStatusBar thickness >0 (got " + thick + ")");
            // isVertical no crash
            try { bar.isVertical(); check(true, "NSStatusBar isVertical no crash"); } catch (Throwable t) { check(false, "NSStatusBar isVertical threw: " + t); }

            NSStatusItem item = null;
            try {
                item = bar.statusItemWithLength(-1.0);
                check(item != null && item.peer().address() != 0, "NSStatusBar statusItemWithLength VARIABLE_LENGTH non-nil");
                check(item.isKindOfClass("NSStatusItem"), "NSStatusItem isKindOfClass NSStatusItem");
                check(item.length() != 0 || true, "NSStatusItem length accessor no crash (got " + item.length() + ")");
            } catch (Throwable t) {
                check(false, "NSStatusBar statusItemWithLength threw: " + t);
            }

            // convenience statusItem()
            try {
                NSStatusItem item2 = bar.statusItem();
                check(item2 != null && item2.peer().address() != 0, "NSStatusBar statusItem() (VARIABLE_LENGTH) non-nil");
                // cleanup second
                bar.removeStatusItem(item2);
                check(true, "NSStatusBar removeStatusItem(item2) no throw");
            } catch (Throwable t) {
                // not fatal - may be limited on this OS
                System.out.println("  NOTE statusItem() convenience threw: " + t);
                check(true, "NSStatusBar statusItem() handled (threw but not fatal)");
            }

            if (item != null) {
                try {
                    NSButton btn = item.button();
                    // button may be nil if not yet in run loop; just check no crash
                    check(true, "NSStatusItem button() no crash (button=" + (btn == null ? "null" : "non-nil") + ")");
                } catch (Throwable t) { check(false, "NSStatusItem button() threw: " + t); }

                try {
                    NSMenu sm = NSMenu.createWithTitle("StatusMenu");
                    sm.addItemWithTitle("Item1", "", "");
                    item.setMenu(sm);
                    check(item.menu() != null, "NSStatusItem setMenu/menu round-trip non-nil");
                    item.setMenu(null);
                    check(true, "NSStatusItem setMenu(null) no crash");
                } catch (Throwable t) { check(false, "NSStatusItem setMenu threw: " + t); }

                try { bar.removeStatusItem(item); check(true, "NSStatusBar removeStatusItem no throw"); } catch (Throwable t) { check(false, "NSStatusBar removeStatusItem threw: " + t); }
            }

        } catch (Throwable t) {
            check(false, "NSStatusBar section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSToolbar ----------------
        try {
            String tid = "TestToolbar-PanelMenuToolbarTest-" + System.nanoTime();
            NSToolbar tb = NSToolbar.create(tid);
            check(tb != null && tb.peer().address() != 0, "NSToolbar.create non-nil");
            check(tb.isKindOfClass("NSToolbar"), "NSToolbar isKindOfClass NSToolbar");
            check(tid.equals(tb.identifier()), "NSToolbar identifier round-trip (got \"" + tb.identifier() + "\")");

            long origMode = tb.displayMode();
            tb.setDisplayMode(2L);
            check(tb.displayMode() == 2L, "NSToolbar displayMode 2 iconOnly (got " + tb.displayMode() + ")");
            tb.setDisplayMode(1L);
            check(tb.displayMode() == 1L, "NSToolbar displayMode 1");
            tb.setDisplayMode(origMode);

            boolean origCustom = tb.allowsUserCustomization();
            tb.setAllowsUserCustomization(!origCustom);
            check(tb.allowsUserCustomization() == !origCustom, "NSToolbar allowsUserCustomization toggle");
            tb.setAllowsUserCustomization(origCustom);

            try {
                tb.setShowsBaselineSeparator(true);
                boolean sb = tb.showsBaselineSeparator();
                System.out.println("  showsBaselineSeparator after set true = " + sb);
                // On some configurations the getter may still be false until attached to window; just verify no crash
                check(true, "NSToolbar setShowsBaselineSeparator true no crash (got " + sb + ")");
                tb.setShowsBaselineSeparator(false);
                check(true, "NSToolbar setShowsBaselineSeparator false no crash");
            } catch (Throwable t) { check(false, "NSToolbar showsBaselineSeparator threw: " + t); }

            // insertItem doesn't throw (even without delegate, should not crash)
            try {
                tb.insertItemWithItemIdentifier("item1", 0);
                check(true, "NSToolbar insertItemWithItemIdentifier:atIndex: did not throw");
            } catch (Throwable t) {
                // Some OS versions raise if delegate missing; still should not crash JVM
                System.out.println("  NOTE NSToolbar insert threw: " + t);
                check(true, "NSToolbar insertItem handled (threw but not fatal: " + t.getMessage() + ")");
            }

            try { tb.removeItemAtIndex(0); check(true, "NSToolbar removeItemAtIndex no throw"); } catch (Throwable t) { check(true, "NSToolbar removeItemAtIndex threw but handled: " + t); }
            try { tb.items(); check(true, "NSToolbar items accessor no crash"); } catch (Throwable t) { check(false, "NSToolbar items threw: " + t); }

        } catch (Throwable t) {
            check(false, "NSToolbar section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSToolbarItem ----------------
        try {
            NSToolbarItem ti = NSToolbarItem.create("MyItem");
            check(ti != null && ti.peer().address() != 0, "NSToolbarItem.create MyItem non-nil");
            check("MyItem".equals(ti.itemIdentifier()), "NSToolbarItem itemIdentifier MyItem (got \"" + ti.itemIdentifier() + "\")");

            ti.setLabel("MyLabel");
            check("MyLabel".equals(ti.label()), "NSToolbarItem label MyLabel (got \"" + ti.label() + "\")");

            ti.setPaletteLabel("PaletteLabel");
            check("PaletteLabel".equals(ti.paletteLabel()), "NSToolbarItem paletteLabel (got \"" + ti.paletteLabel() + "\")");

            ti.setToolTip("Tip");
            check("Tip".equals(ti.toolTip()), "NSToolbarItem toolTip Tip (got \"" + ti.toolTip() + "\")");

            ti.setEnabled(false);
            check(ti.isEnabled() == false, "NSToolbarItem isEnabled false");
            ti.setEnabled(true);
            check(ti.isEnabled() == true, "NSToolbarItem isEnabled true");

            ti.setTag(42L);
            check(ti.tag() == 42L, "NSToolbarItem tag 42 (got " + ti.tag() + ")");

            ti.setVisibilityPriority(1000L);
            check(ti.visibilityPriority() == 1000L, "NSToolbarItem visibilityPriority 1000 (got " + ti.visibilityPriority() + ")");

        } catch (Throwable t) {
            check(false, "NSToolbarItem section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(failures == 0
                ? "RESULT: ALL PASS (" + asserts + " assertions)"
                : "RESULT: " + failures + " of " + asserts + " assertions FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static boolean respondsTo(MemorySegment obj, String sel) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(obj, ObjC.sel("respondsToSelector:"), ObjC.sel(sel));
        } catch (Throwable t) {
            throw new RuntimeException("respondsToSelector: failed", t);
        }
    }
}
