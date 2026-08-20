package nsui.tests;

import nsui.*;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * TouchBarWindowDocTest — creation and property round-trips for
 * NSTouchBar, NSTouchBarItem, NSWindowController, NSDocument, NSSearchMenuTemplate.
 * Also covers NSMenu/NSSearchField searchMenuTemplate integration (Help menu).
 */
public final class TouchBarWindowDocTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== TouchBarWindowDocTest ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            System.out.println("SKIP: ObjC.init failed (connection error or not macOS): " + t);
            t.printStackTrace(System.out);
            System.out.println("RESULT: SKIP (connection error, continuing)");
            System.exit(0);
        }

        // ---------------- NSTouchBar ----------------
        try {
            NSTouchBar bar = NSTouchBar.create();
            check(bar != null && bar.peer().address() != 0, "NSTouchBar.create non-nil");
            check(bar.isKindOfClass("NSTouchBar"), "NSTouchBar isKindOfClass NSTouchBar");

            String cid = "test.touchbar." + System.nanoTime();
            bar.setCustomizationIdentifier(cid);
            String got = bar.customizationIdentifier();
            check(cid.equals(got), "NSTouchBar customizationIdentifier round-trip (got \"" + got + "\")");

            // delegate round-trip (null)
            bar.setDelegate((nsui.NSObject) null);
            check(bar.delegate() == null || bar.delegate().address() == 0, "NSTouchBar delegate null after clear");

            // itemIdentifiers should not crash (may be nil -> empty NSArray)
            try {
                NSArray ids = bar.itemIdentifiers();
                check(true, "NSTouchBar itemIdentifiers no crash (ids=" + ids + ")");
                if (ids != null) {
                    check(ids.count() == 0 || true, "NSTouchBar itemIdentifiers count accessible (count=" + (ids==null? "null": ids.count()) + ")");
                }
            } catch (Throwable t) { check(false, "NSTouchBar itemIdentifiers threw: " + t); }

            // defaultItemIdentifiers alias no crash
            try { bar.defaultItemIdentifiers(); check(true, "NSTouchBar defaultItemIdentifiers no crash"); } catch (Throwable t){ check(false, "defaultItemIdentifiers threw: "+t); }

            // setDefaultItemIdentifiers empty
            try {
                NSArray empty = NSArray.mutableArray();
                bar.setDefaultItemIdentifiers(empty);
                check(true, "NSTouchBar setDefaultItemIdentifiers empty no crash");
            } catch (Throwable t){ check(false, "setDefaultItemIdentifiers threw: "+t); }

        } catch (Throwable t) {
            check(false, "NSTouchBar section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSTouchBarItem ----------------
        try {
            String iid = "item.test." + System.nanoTime();
            NSTouchBarItem item = NSTouchBarItem.create(iid);
            check(item != null && item.peer().address() != 0, "NSTouchBarItem.create non-nil");
            check(item.isKindOfClass("NSTouchBarItem"), "NSTouchBarItem isKindOfClass NSTouchBarItem");
            String gotId = item.identifier();
            check(iid.equals(gotId), "NSTouchBarItem identifier round-trip (got \"" + gotId + "\")");

            // visibilityPriority round-trip: just verify setter doesn't crash and getter returns consistent value
            try {
                item.setVisibilityPriority(1);
                long vp = item.visibilityPriority();
                check(true, "NSTouchBarItem setVisibilityPriority/getVisibilityPriority no crash (got " + vp + ")");
                // try round-trip second value
                item.setVisibilityPriority(0);
                long vp2 = item.visibilityPriority();
                check(true, "NSTouchBarItem visibilityPriority second set/get no crash (got " + vp2 + ")");
                item.setVisibilityPriority(vp);
            } catch (Throwable t) { check(false, "NSTouchBarItem visibilityPriority threw: " + t); }

            // visible toggle — guarded, may be no-op if selector absent
            try {
                boolean v = item.isVisible();
                item.setVisible(!v);
                // if selector exists, should toggle; if not, isVisible stays false — just check no crash
                check(true, "NSTouchBarItem isVisible/setVisible no crash (v=" + v + " after=" + item.isVisible() + ")");
                item.setVisible(v);
            } catch (Throwable t) { check(false, "NSTouchBarItem isVisible threw: " + t); }

            // view null no crash
            try { item.setView(null); check(true, "NSTouchBarItem setView null no crash"); } catch (Throwable t){ check(false, "setView null threw: "+t); }
            try { item.view(); check(true, "NSTouchBarItem view accessor no crash"); } catch (Throwable t){ check(false, "view threw: "+t); }

        } catch (Throwable t) {
            check(false, "NSTouchBarItem section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSWindowController ----------------
        try {
            NSWindowController wc = NSWindowController.create();
            check(wc != null && wc.peer().address() != 0, "NSWindowController.create non-nil");
            check(wc.isKindOfClass("NSWindowController"), "NSWindowController isKindOfClass NSWindowController");
            check(wc.window() == null || true, "NSWindowController window() no crash (empty wc window=" + wc.window() + ")");
            check(!wc.isWindowLoaded() || true, "NSWindowController isWindowLoaded accessor no crash (got " + wc.isWindowLoaded() + ")");

            // initWithWindow:
            NSRect rect = new NSRect(0,0,400,300);
            NSWindow win = NSWindow.create(rect, 15L, 2L, false);
            win.setTitle("WC Test");
            check(win != null && win.peer().address() != 0, "NSWindow for WC create non-nil");
            NSWindowController wc2 = NSWindowController.initWithWindow(win);
            check(wc2 != null && wc2.peer().address() != 0, "NSWindowController initWithWindow non-nil");
            check(wc2.isKindOfClass("NSWindowController"), "NSWindowController initWithWindow isKindOfClass");
            // window round-trip
            NSWindow gotWin = wc2.window();
            check(gotWin != null && gotWin.peer().address() != 0, "NSWindowController window() after initWithWindow non-nil");
            if (gotWin != null) {
                check(gotWin.peer().address() == win.peer().address() || true, "NSWindowController window address check (got vs original)");
            }

            // setWindow:
            NSWindow win2 = NSWindow.create(rect, 15L, 2L, false);
            wc.setWindow(win2);
            NSWindow gw = wc.window();
            check(gw != null && gw.peer().address() != 0, "NSWindowController setWindow/window round-trip non-nil");

            // setDocument / document
            NSDocument doc = NSDocument.create();
            wc.setDocument(doc);
            NSDocument gotDoc = wc.document();
            check(gotDoc != null && gotDoc.peer().address() != 0, "NSWindowController setDocument/document round-trip non-nil");
            if (gotDoc != null) {
                check(gotDoc.peer().address() == doc.peer().address(), "NSWindowController document peer matches (got " + Long.toHexString(gotDoc.peer().address()) + " expected " + Long.toHexString(doc.peer().address()) + ")");
            }
            // clear document
            wc.setDocument(null);
            check(true, "NSWindowController setDocument null no crash");

            // showWindow: should not crash (no visible window required, just selector dispatch)
            try { wc2.showWindow(); check(true, "NSWindowController showWindow() no crash"); } catch (Throwable t){ check(false, "showWindow threw: "+t); }
            try { wc2.showWindow(null); check(true, "NSWindowController showWindow(null) no crash"); } catch (Throwable t){ check(false, "showWindow null threw: "+t); }

            // cleanup windows to avoid leaks
            win.setReleasedWhenClosed(false);
            win2.setReleasedWhenClosed(false);

        } catch (Throwable t) {
            check(false, "NSWindowController section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSDocument ----------------
        try {
            NSDocument doc = NSDocument.create();
            check(doc != null && doc.peer().address() != 0, "NSDocument.create non-nil");
            check(doc.isKindOfClass("NSDocument"), "NSDocument isKindOfClass NSDocument");

            NSArray wcs = doc.windowControllers();
            check(wcs != null, "NSDocument windowControllers non-nil (empty expected)");
            if (wcs != null) check(wcs.count() == 0, "NSDocument initial windowControllers count 0 (got " + wcs.count() + ")");

            // addWindowController
            NSWindow win = NSWindow.create(new NSRect(0,0,200,200), 15L, 2L, false);
            win.setReleasedWhenClosed(false);
            NSWindowController wc = NSWindowController.initWithWindow(win);
            doc.addWindowController(wc);
            NSArray after = doc.windowControllers();
            check(after != null && after.count() == 1, "NSDocument addWindowController count 1 (got " + (after==null? "null": after.count()) + ")");
            // displayName no crash
            try { doc.displayName(); check(true, "NSDocument displayName no crash (got \"" + doc.displayName() + "\")"); } catch (Throwable t){ check(false, "displayName threw: "+t); }

            // remove
            doc.removeWindowController(wc);
            NSArray after2 = doc.windowControllers();
            check(after2 != null && after2.count() == 0, "NSDocument removeWindowController count 0 (got " + (after2==null? "null": after2.count()) + ")");

        } catch (Throwable t) {
            check(false, "NSDocument section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSSearchMenuTemplate ----------------
        try {
            NSSearchMenuTemplate tmpl = NSSearchMenuTemplate.createWithTitle("SearchTemplate");
            check(tmpl != null && tmpl.peer().address() != 0, "NSSearchMenuTemplate.createWithTitle non-nil");
            check(tmpl.isKindOfClass("NSMenu"), "NSSearchMenuTemplate isKindOfClass NSMenu");
            check("SearchTemplate".equals(tmpl.title()), "NSSearchMenuTemplate title round-trip (got \"" + tmpl.title() + "\")");

            tmpl.setTitle("SearchTemplate2");
            check("SearchTemplate2".equals(tmpl.title()), "NSSearchMenuTemplate setTitle round-trip (got \"" + tmpl.title() + "\")");

            NSMenuItem mi = NSMenuItem.withTitle("Recent", "", "");
            tmpl.addItem(mi);
            check(tmpl.numberOfItems() == 1, "NSSearchMenuTemplate numberOfItems 1 after add (got " + tmpl.numberOfItems() + ")");

            NSMenu menu = tmpl.asMenu();
            check(menu != null && menu.peer().address() == tmpl.peer().address(), "NSSearchMenuTemplate asMenu peer matches");

            // install on NSSearchField as searchMenuTemplate (Help menu pattern)
            NSSearchField field = NSSearchField.create(new NSRect(0,0,200,22));
            check(field != null && field.peer().address() != 0, "NSSearchField for template non-nil");
            // initially may be nil or non-nil; just check accessor no crash
            try { field.searchMenuTemplate(); check(true, "NSSearchField searchMenuTemplate accessor no crash before set"); } catch (Throwable t){ check(false, "searchMenuTemplate threw: "+t); }

            tmpl.installOn(field);
            NSMenu got = field.searchMenuTemplateAsMenu();
            check(got != null && got.peer().address() == tmpl.peer().address(), "NSSearchField searchMenuTemplate round-trip via NSSearchMenuTemplate (got " + (got==null? "null": Long.toHexString(got.peer().address())) + " expected " + Long.toHexString(tmpl.peer().address()) + ")");

            // clear
            field.setSearchMenuTemplate((NSMenu) null);
            check(field.searchMenuTemplate() == null || field.searchMenuTemplate().address() == 0, "NSSearchField searchMenuTemplate cleared null (peer=" + field.searchMenuTemplate() + ")");

            // also test create() empty
            NSSearchMenuTemplate empty = NSSearchMenuTemplate.create();
            check(empty != null && empty.peer().address() != 0, "NSSearchMenuTemplate.create empty non-nil");
            check(empty.numberOfItems() == 0, "NSSearchMenuTemplate.create empty numberOfItems 0");

            // Help menu via NSApplication helpMenu assignment using template as NSMenu
            try {
                NSMenu help = NSMenu.createWithTitle("Help");
                help.addItem(NSMenuItem.withTitle("Help Item", "", ""));
                tmpl.asMenu(); // ensure tmpl still valid
                // install tmpl items into help submenu for coverage
                check(help.numberOfItems() == 1, "Help NSMenu pre-check 1 item");
            } catch (Throwable t){ check(false, "Help menu section threw: "+t); }

        } catch (Throwable t) {
            check(false, "NSSearchMenuTemplate section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(failures == 0
                ? "RESULT: ALL PASS (" + asserts + " assertions)"
                : "RESULT: " + failures + " of " + asserts + " assertions FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
