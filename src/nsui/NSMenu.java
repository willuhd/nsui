package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMenu — a native menu (the menu bar itself is an NSMenu with NSMenuItem children).
///
/// Left menubar items (App, File, Edit, View) are standard top-level NSMenus attached to
/// `setMainMenu` via NSMenuItem+submenu — e.g.:
/// ```
/// `NSMenu main = NSMenu.create(); NSMenu appMenu = NSMenu.createWithTitle("NSUI3"); appMenu.addItemWithTitle("About NSUI3", "", ""); NSMenuItem appItem = NSMenuItem.withTitle("NSUI3", "", ""); appItem.setSubmenu(appMenu); main.addItem(appItem); // repeat for File, Edit, View — all via NSMenu, not menubar icons app.setMainMenu(main);`
/// ```
/// Menubar icons (NSStatusItem) are separate (NSStatusBar) — do not add status-bar icons here.
///
/// Menu-list icons: use `setImage` on dropdown items (File → New, etc.).
/// Top-level bar items should remain text-only; menu-list items may carry icons via this helper
/// `attachMenuItemIcon` or directly via NSMenuItem.setImage.
///
/// Help search note: AppKit auto-inserts fn+F fullScreen at the bottom of View/Help and a
/// Help searchbar. For demos, use a custom centered search field in a non-Help menu (Edit/View)
/// via `setView` with `insertGallerySearchFieldItem`.
public final class NSMenu extends NSObject {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment alloc;
        static MemorySegment init;
        static MemorySegment initWithTitle;
        static MemorySegment title;
        static MemorySegment setTitle;
        static MemorySegment supermenu;
        static MemorySegment setSupermenu;
        static MemorySegment array;
        static MemorySegment addObject;
        static MemorySegment setItemArray;
        static MemorySegment addItem;
        static MemorySegment insertItem_atIndex;
        static MemorySegment insertItemWithTitle_action_keyEquivalent_atIndex;
        static MemorySegment addItemWithTitle_action_keyEquivalent;
        static MemorySegment removeItemAtIndex;
        static MemorySegment removeItem;
        static MemorySegment removeAllItems;
        static MemorySegment setSubmenu;
        static MemorySegment setSubmenu_forItem;
        static MemorySegment itemArray;
        static MemorySegment count;
        static MemorySegment objectAtIndex;
        static MemorySegment numberOfItems;
        static MemorySegment itemAtIndex;
        static MemorySegment indexOfItem;
        static MemorySegment indexOfItemWithTitle;
        static MemorySegment indexOfItemWithTag;
        static MemorySegment itemWithTitle;
        static MemorySegment itemWithTag;
        static MemorySegment autoenablesItems;
        static MemorySegment setAutoenablesItems;
        static MemorySegment update;
        static MemorySegment performActionForItemAtIndex;
        static MemorySegment itemChanged;
        static MemorySegment delegate;
        static MemorySegment setDelegate;
        static MemorySegment highlightedItem;
        static MemorySegment minimumWidth;
        static MemorySegment setMinimumWidth;
        static MemorySegment size;
        static MemorySegment font;
        static MemorySegment setFont;
        static MemorySegment showsStateColumn;
        static MemorySegment setShowsStateColumn;
        static MemorySegment allowsContextMenuPlugIns;
        static MemorySegment setAllowsContextMenuPlugIns;
        static MemorySegment respondsToSelector;
        static MemorySegment showsSearchField;
        static MemorySegment setShowsSearchField;
        static MemorySegment popUpMenuPositioningItem_atLocation_inView;
        static MemorySegment popUpContextMenu_withEvent_forView;
        static MemorySegment popUpContextMenu_withEvent_forView_withFont;
        static MemorySegment setMenuBarVisible;
        static MemorySegment menuBarVisible;
        static MemorySegment indexOfItemWithRepresentedObject;
        static MemorySegment indexOfItemWithSubmenu;
        static MemorySegment indexOfItemWithTarget_andAction;
        static MemorySegment performKeyEquivalent;
        static MemorySegment cancelTracking;
        static MemorySegment cancelTrackingWithoutAnimation;
        static MemorySegment menuBarHeight;
        static MemorySegment propertiesToUpdate;
        static MemorySegment userInterfaceLayoutDirection;
        static MemorySegment setUserInterfaceLayoutDirection;
        static MemorySegment automaticallyInsertsWritingToolsItems;
        static MemorySegment setAutomaticallyInsertsWritingToolsItems;
        static MemorySegment presentationStyle;
        static MemorySegment setPresentationStyle;
        static MemorySegment selectionMode;
        static MemorySegment setSelectionMode;
        static MemorySegment selectedItems;
        static MemorySegment setSelectedItems;
        static MemorySegment submenuAction;
        static void populate() {
            alloc = ObjC.sel("alloc");
            init = ObjC.sel("init");
            initWithTitle = ObjC.sel("initWithTitle:");
            title = ObjC.sel("title");
            setTitle = ObjC.sel("setTitle:");
            supermenu = ObjC.sel("supermenu");
            setSupermenu = ObjC.sel("setSupermenu:");
            array = ObjC.sel("array");
            addObject = ObjC.sel("addObject:");
            setItemArray = ObjC.sel("setItemArray:");
            addItem = ObjC.sel("addItem:");
            insertItem_atIndex = ObjC.sel("insertItem:atIndex:");
            insertItemWithTitle_action_keyEquivalent_atIndex = ObjC.sel("insertItemWithTitle:action:keyEquivalent:atIndex:");
            addItemWithTitle_action_keyEquivalent = ObjC.sel("addItemWithTitle:action:keyEquivalent:");
            removeItemAtIndex = ObjC.sel("removeItemAtIndex:");
            removeItem = ObjC.sel("removeItem:");
            removeAllItems = ObjC.sel("removeAllItems");
            setSubmenu = ObjC.sel("setSubmenu:");
            setSubmenu_forItem = ObjC.sel("setSubmenu:forItem:");
            itemArray = ObjC.sel("itemArray");
            count = ObjC.sel("count");
            objectAtIndex = ObjC.sel("objectAtIndex:");
            numberOfItems = ObjC.sel("numberOfItems");
            itemAtIndex = ObjC.sel("itemAtIndex:");
            indexOfItem = ObjC.sel("indexOfItem:");
            indexOfItemWithTitle = ObjC.sel("indexOfItemWithTitle:");
            indexOfItemWithTag = ObjC.sel("indexOfItemWithTag:");
            itemWithTitle = ObjC.sel("itemWithTitle:");
            itemWithTag = ObjC.sel("itemWithTag:");
            autoenablesItems = ObjC.sel("autoenablesItems");
            setAutoenablesItems = ObjC.sel("setAutoenablesItems:");
            update = ObjC.sel("update");
            performActionForItemAtIndex = ObjC.sel("performActionForItemAtIndex:");
            itemChanged = ObjC.sel("itemChanged:");
            delegate = ObjC.sel("delegate");
            setDelegate = ObjC.sel("setDelegate:");
            highlightedItem = ObjC.sel("highlightedItem");
            minimumWidth = ObjC.sel("minimumWidth");
            setMinimumWidth = ObjC.sel("setMinimumWidth:");
            size = ObjC.sel("size");
            font = ObjC.sel("font");
            setFont = ObjC.sel("setFont:");
            showsStateColumn = ObjC.sel("showsStateColumn");
            setShowsStateColumn = ObjC.sel("setShowsStateColumn:");
            allowsContextMenuPlugIns = ObjC.sel("allowsContextMenuPlugIns");
            setAllowsContextMenuPlugIns = ObjC.sel("setAllowsContextMenuPlugIns:");
            respondsToSelector = ObjC.sel("respondsToSelector:");
            showsSearchField = ObjC.sel("showsSearchField");
            setShowsSearchField = ObjC.sel("setShowsSearchField:");
            popUpMenuPositioningItem_atLocation_inView = ObjC.sel("popUpMenuPositioningItem:atLocation:inView:");
            popUpContextMenu_withEvent_forView = ObjC.sel("popUpContextMenu:withEvent:forView:");
            popUpContextMenu_withEvent_forView_withFont = ObjC.sel("popUpContextMenu:withEvent:forView:withFont:");
            setMenuBarVisible = ObjC.sel("setMenuBarVisible:");
            menuBarVisible = ObjC.sel("menuBarVisible");
            indexOfItemWithRepresentedObject = ObjC.sel("indexOfItemWithRepresentedObject:");
            indexOfItemWithSubmenu = ObjC.sel("indexOfItemWithSubmenu:");
            indexOfItemWithTarget_andAction = ObjC.sel("indexOfItemWithTarget:andAction:");
            performKeyEquivalent = ObjC.sel("performKeyEquivalent:");
            cancelTracking = ObjC.sel("cancelTracking");
            cancelTrackingWithoutAnimation = ObjC.sel("cancelTrackingWithoutAnimation");
            menuBarHeight = ObjC.sel("menuBarHeight");
            propertiesToUpdate = ObjC.sel("propertiesToUpdate");
            userInterfaceLayoutDirection = ObjC.sel("userInterfaceLayoutDirection");
            setUserInterfaceLayoutDirection = ObjC.sel("setUserInterfaceLayoutDirection:");
            automaticallyInsertsWritingToolsItems = ObjC.sel("automaticallyInsertsWritingToolsItems");
            setAutomaticallyInsertsWritingToolsItems = ObjC.sel("setAutomaticallyInsertsWritingToolsItems:");
            presentationStyle = ObjC.sel("presentationStyle");
            setPresentationStyle = ObjC.sel("setPresentationStyle:");
            selectionMode = ObjC.sel("selectionMode");
            setSelectionMode = ObjC.sel("setSelectionMode:");
            selectedItems = ObjC.sel("selectedItems");
            setSelectedItems = ObjC.sel("setSelectedItems:");
            submenuAction = ObjC.sel("submenuAction:");
        }
    }

    private record Handles(MethodHandle hIdInt, MethodHandle hVoidIdInt, MethodHandle hIntId, MethodHandle hSize, MethodHandle hPopUp, MethodHandle hInsertTitleActionKEIndex, MethodHandle hSetSubmenuForItem) {}
    private static volatile Handles H;

    private NSMenu(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (H != null) return;
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.POINT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)));
            Sels.populate();
        H = h;
}

    public static NSMenu wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMenu(peer);
    }

    /// alloc + init.
    public static NSMenu create() {
        ensureInit();
        MemorySegment m = ObjC.msgSendId(ObjC.cls("NSMenu"), Sels.alloc);
        return new NSMenu(ObjC.msgSendId(m, Sels.init));
    }

    public static NSMenu createWithTitle(String title) {
        ensureInit();
        MemorySegment m = ObjC.msgSendId(ObjC.cls("NSMenu"), Sels.alloc);
        MemorySegment n = ObjC.msgSendIdId(m, Sels.initWithTitle, ObjC.nsstring(title));
        return new NSMenu(n);
    }

    // ---- title ----
    public String title() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.title));
    }
    public void setTitle(String t) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setTitle, ObjC.nsstring(t));
    }

    // ---- supermenu ----
    public NSMenu supermenu() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, Sels.supermenu));
    }

    /// [menu setSupermenu:] — set the supermenu (rarely set directly).
    public void setSupermenu(NSMenu menu) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setSupermenu, (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
    }

    /// [menu setItemArray:] — replace the item array.
    public void setItemArray(java.util.List<NSMenuItem> items) {
        ensureInit();
        if (items == null) return;
        // Build NSArray from items
        MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSArray"), Sels.alloc);
        // Use initWithObjects:count: via handle if needed, fallback to adding
        // Simpler: create mutable array and add objects
        MemorySegment mArr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), Sels.array);
        for (NSMenuItem it : items) {
            if (it != null) ObjC.msgSendVoidId(mArr, Sels.addObject, it.peer());
        }
        ObjC.msgSendVoidId(peer, Sels.setItemArray, mArr);
    }

    // ---- items ----
    public void addItem(NSMenuItem item) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.addItem, item.peer());
    }
    public void insertItem(NSMenuItem item, long index) {
        ensureInit();
        try { H.hVoidIdInt().invokeExact(peer, Sels.insertItem_atIndex, item.peer(), index); } catch (Throwable t) { throw new RuntimeException("insertItem:atIndex: failed", t); }
    }
    public NSMenuItem insertItemWithTitle(String title, String action, String keyEquivalent, long index) {
        ensureInit();
        try {
            MemorySegment selAction = (action == null || action.isEmpty()) ? MemorySegment.NULL : ObjC.sel(action);
            // null title is valid for search-field placeholder items (empty title + custom view)
            String safeTitle = title == null ? "" : title;
            String safeKE = keyEquivalent == null ? "" : keyEquivalent;
            MemorySegment p = (MemorySegment) H.hInsertTitleActionKEIndex().invokeExact(peer,
                    Sels.insertItemWithTitle_action_keyEquivalent_atIndex,
                    ObjC.nsstring(safeTitle), selAction, ObjC.nsstring(safeKE), index);
            return NSMenuItem.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("insertItemWithTitle:action:keyEquivalent:atIndex: failed", t);
        }
    }
    public NSMenuItem addItemWithTitle(String title, String action, String keyEquivalent) {
        ensureInit();
        String safeTitle = title == null ? "" : title;
        String safeKE = keyEquivalent == null ? "" : keyEquivalent;
        MemorySegment item = ObjC.msgSendIdIdSelId(peer,
                Sels.addItemWithTitle_action_keyEquivalent,
                ObjC.nsstring(safeTitle), action == null || action.isEmpty() ? MemorySegment.NULL : ObjC.sel(action), ObjC.nsstring(safeKE));
        return (item == null || item.address() == 0) ? null : NSMenuItem.wrap(item);
    }

    public void removeItemAtIndex(long index) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.removeItemAtIndex, index);
    }
    public void removeItem(NSMenuItem item) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.removeItem, item.peer());
    }
    public void removeAllItems() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.removeAllItems);
    }

    /// Attach a submenu to a menu item (the item lives in this menu) — [item setSubmenu:submenu].
    public void setSubmenu(NSMenuItem item, NSMenu submenu) {
        ensureInit();
        ObjC.msgSendVoidId(item.peer(), Sels.setSubmenu, (MemorySegment) (submenu == null ? MemorySegment.NULL : submenu.peer()));
    }
    /// [self setSubmenu:submenu forItem:item] — the NSMenu variant (both peers as ID).
    public void setSubmenuForItem(NSMenu submenu, NSMenuItem item) {
        ensureInit();
        try {
            H.hSetSubmenuForItem().invokeExact(peer, Sels.setSubmenu_forItem,
                    (MemorySegment) (submenu == null ? MemorySegment.NULL : submenu.peer()),
                    item.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setSubmenu:forItem: failed", t);
        }
    }
    /// Alias preserving the original ObjC selector order: setSubmenu:forItem:
    public void setSubmenuForItemCompat(NSMenu submenu, NSMenuItem item) {
        setSubmenuForItem(submenu, item);
    }

    // ---- itemArray / numberOfItems / itemAtIndex ----
    public MemorySegment itemArrayId() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.itemArray);
    }
    public java.util.List<NSMenuItem> itemArray() {
        ensureInit();
        MemorySegment arr = ObjC.msgSendId(peer, Sels.itemArray);
        if (arr == null || arr.address() == 0) return java.util.List.of();
        // NSArray -> count + objectAtIndex:
        long count = ObjC.msgSendLong(arr, Sels.count);
        java.util.List<NSMenuItem> out = new java.util.ArrayList<>((int) count);
        MethodHandle hAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        MemorySegment atSel = Sels.objectAtIndex;
        for (long i = 0; i < count; i++) {
            try {
                MemorySegment it = (MemorySegment) hAt.invokeExact(arr, atSel, i);
                out.add(it == null || it.address()==0 ? null : NSMenuItem.wrap(it));
            } catch (Throwable t) { throw new RuntimeException("objectAtIndex: failed", t); }
        }
        return java.util.Collections.unmodifiableList(out);
    }
    public long numberOfItems() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.numberOfItems);
    }
    public NSMenuItem itemAtIndex(long index) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) H.hIdInt().invokeExact(peer, Sels.itemAtIndex, index);
            return NSMenuItem.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("itemAtIndex: failed", t); }
    }
    public long indexOfItem(NSMenuItem item) {
        ensureInit();
        try { return (long) H.hIntId().invokeExact(peer, Sels.indexOfItem, item.peer()); } catch (Throwable t) { throw new RuntimeException("indexOfItem: failed", t); }
    }
    public long indexOfItemWithTitle(String title) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, Sels.indexOfItemWithTitle, ObjC.nsstring(title));
        } catch (Throwable t) { throw new RuntimeException("indexOfItemWithTitle: failed", t); }
    }
    public long indexOfItemWithTag(long tag) {
        ensureInit();
        try {
            return (long) ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.INT, nsui.objc.Sig.Arg.INT)).invokeExact(peer, Sels.indexOfItemWithTag, tag);
        } catch (Throwable e) { throw new RuntimeException("indexOfItemWithTag: failed", e); }
    }
    public NSMenuItem itemWithTitle(String title) {
        ensureInit();
        MemorySegment p = ObjC.msgSendIdId(peer, Sels.itemWithTitle, ObjC.nsstring(title));
        return NSMenuItem.wrap(p);
    }
    public NSMenuItem itemWithTag(long tag) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) H.hIdInt().invokeExact(peer, Sels.itemWithTag, tag);
            return NSMenuItem.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("itemWithTag: failed", t); }
    }

    // ---- autoenablesItems / update ----
    public boolean autoenablesItems() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.autoenablesItems);
    }
    public void setAutoenablesItems(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutoenablesItems, flag);
    }
    public void update() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.update);
    }
    public void performActionForItemAtIndex(long index) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.performActionForItemAtIndex, index);
    }
    public void itemChanged(NSMenuItem item) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.itemChanged, item.peer());
    }

    // ---- delegate / appearance ----
    public MemorySegment delegate() {
    ensureInit(); return ObjC.msgSendId(peer, Sels.delegate); }
    public void setDelegate(MemorySegment d) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.setDelegate, (MemorySegment) (d == null ? MemorySegment.NULL : d)); }
    public NSMenuItem highlightedItem() {
    ensureInit(); return NSMenuItem.wrap(ObjC.msgSendId(peer, Sels.highlightedItem)); }
    public double minimumWidth() {
        ensureInit();
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, Sels.minimumWidth); } catch (Throwable t) { throw new RuntimeException("minimumWidth failed", t); }
    }
    public void setMinimumWidth(double w) {
        ensureInit();
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, Sels.setMinimumWidth, w); } catch (Throwable t) { throw new RuntimeException("setMinimumWidth: failed", t); }
    }
    public NSSize size() {
        ensureInit();
        try { return NSSize.fromSegment((MemorySegment) H.hSize().invokeExact(ObjC.structSlot(), peer, Sels.size)); } catch (Throwable t) { throw new RuntimeException("size failed", t); }
    }
    public MemorySegment font() {
    ensureInit(); return ObjC.msgSendId(peer, Sels.font); }
    public void setFont(NSFont f) {
    ensureInit(); ObjC.msgSendVoidId(peer, Sels.setFont, (MemorySegment) (f == null ? MemorySegment.NULL : f.peer())); }
    public boolean showsStateColumn() {
    ensureInit(); return ObjC.msgSendBool(peer, Sels.showsStateColumn); }
    public void setShowsStateColumn(boolean flag) {
    ensureInit(); ObjC.msgSendVoidBool(peer, Sels.setShowsStateColumn, flag); }
    public boolean allowsContextMenuPlugIns() {
    ensureInit(); return ObjC.msgSendBool(peer, Sels.allowsContextMenuPlugIns); }
    public void setAllowsContextMenuPlugIns(boolean flag) {
ensureInit(); ObjC.msgSendVoidBool(peer, Sels.setAllowsContextMenuPlugIns, flag); }

    // ---- Help-search field (showsSearchField) ----
    // AppKit may not expose this selector on all OS versions; guard via respondsToSelector:
    private boolean respondsTo(String selName) {
        ensureInit();
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer, Sels.respondsToSelector, ObjC.sel(selName));
        } catch (Throwable t) { return false; }
    }
    /// [menu showsSearchField] — Help-menu search field visibility (guarded; false if selector absent).
    public boolean showsSearchField() {
        ensureInit();
        if (!respondsTo("showsSearchField")) return false;
        try { return ObjC.msgSendBool(peer, Sels.showsSearchField); } catch (Throwable t) { return false; }
    }
    /// [menu setShowsSearchField:] — Help-menu search field visibility (no-op if selector absent).
    public void setShowsSearchField(boolean flag) {
        ensureInit();
        if (!respondsTo("setShowsSearchField:")) return;
        try { ObjC.msgSendVoidBool(peer, Sels.setShowsSearchField, flag); } catch (Throwable ignored) {}
    }
    /// Alias for setShowsSearchField — legacy name used by some tests/docs.
    public void setShowsSearchFieldCompat(boolean flag) { setShowsSearchField(flag); }

    // ---- left menubar top-level helpers (App/File/Edit/View via NSMenu, not status icons) ----
    /// Add a top-level menubar menu (App/File/Edit/View) to a mainMenu.
    /// Creates an NSMenuItem with title and attaches the given submenu, then adds it to mainMenu.
    /// Left menubar items must use this NSMenu path — not NSStatusItem bar icons.
    public static NSMenuItem addTopLevelMenu(NSMenu mainMenu, String title, NSMenu submenu) {
        NSMenuItem item = NSMenuItem.withTitle(title == null ? "" : title, "", "");
        if (submenu != null) item.setSubmenu(submenu);
        mainMenu.addItem(item);
        return item;
    }

    /// Convenience factory for an App/File/Edit/View menu with title.
    public static NSMenu createAppMenu(String title) { return createWithTitle(title == null ? "" : title); }
    public static NSMenu createFileMenu() { NSMenu m = createWithTitle("File"); m.setTitle("File"); return m; }
    public static NSMenu createEditMenu() { NSMenu m = createWithTitle("Edit"); m.setTitle("Edit"); return m; }
    public static NSMenu createViewMenu() { NSMenu m = createWithTitle("View"); m.setTitle("View"); return m; }

    // ---- menu-list icon helpers (NSMenuItem.setImage is for dropdown lists, not menubar bar) ----
    /// Attach a system image to a menu-list item (File/Edit/View/App dropdown). Uses
    /// `setImage` which renders in the menu list column, not the menubar bar.
    /// Do NOT use for NSStatusItem bar — that is status-agent owned.
    /// @param item target menu item (dropdown list entry)
    /// @param imageName system image name (e.g. "NSFolder", "NSSearchTemplate"); no-op if not found
    /// @return true if image was found and attached
    public static boolean attachMenuItemIcon(NSMenuItem item, String imageName) {
        if (item == null || imageName == null || imageName.isEmpty()) return false;
        try {
            NSImage img = NSImage.imageNamed(imageName);
            if (img != null) { item.setImage(img); return true; }
        } catch (Throwable ignored) {}
        return false;
    }
    /// Variant that clears the image if imageName == null.
    public static void setMenuItemIconOrClear(NSMenuItem item, String imageName) {
        if (item == null) return;
        if (imageName == null || imageName.isEmpty()) { try { item.setImage(null); } catch (Throwable ignored) {} return; }
        attachMenuItemIcon(item, imageName);
    }

    // ---- search-field embedding helper ----
    /// Insert a placeholder item for a search field and embed the view.
    /// Equivalent to `insertItemWithTitle:"" + item.view = searchField`.
    /// The returned item's view is the supplied `field` (NSSearchField is an NSView).
    /// For menu-list aesthetics, prefer `insertGallerySearchFieldItem` for centered alignment.
    public NSMenuItem insertSearchFieldItem(NSSearchField field, long index) {
        NSMenuItem item = insertItemWithTitle("", "", "", index);
        if (field != null) item.setView(field.peer());
        return item;
    }
    /// Convenience: add search field item at end.
    public NSMenuItem addSearchFieldItem(NSSearchField field) {
        return insertSearchFieldItem(field, numberOfItems());
    }

    // ---- centered Gallery Search (non-Help menu) — avoids Help/View auto fn+F row ----
    /// Insert a centered "Gallery Search" field into a non-Help menu (Edit or View) as a custom view.
    /// Uses `NSMenuItem.setView` with an NSSearchField and aligns it via view frame + indentation.
    /// Title is "Gallery Search" (not "Help") to avoid Apple Help search auto-insertion. The search field's
    /// frame is inset (x=8) and the item's indentationLevel=1 to visually center the field in the menu.
    /// Callers supply the NSSearchField (so they retain target/action); this method frames and centers it.
    public NSMenuItem insertGallerySearchFieldItem(NSSearchField field, long index) {
        NSMenuItem item = insertItemWithTitle("", "", "", index);
        if (field != null) {
            // Centered/aligned: inset field frame horizontally and use indentationLevel to center in menu
            try { field.setFrame(new NSRect(8, 0, 184, 22)); } catch (Throwable ignored) {}
            try { field.setCentersPlaceholder(true); } catch (Throwable ignored) {}
            item.setView(field.peer());
            try { item.setIndentationLevel(1); } catch (Throwable ignored) {}
        }
        return item;
    }
    /// Convenience: add centered Gallery Search field at end of this menu.
    public NSMenuItem addGallerySearchFieldItem(NSSearchField field) {
        return insertGallerySearchFieldItem(field, numberOfItems());
    }
    /// Create and insert a centered Gallery Search field with placeholder "Gallery Search".
    public NSMenuItem insertGallerySearchField(String placeholder, long index) {
        NSSearchField f = NSSearchField.create(new NSRect(8, 0, 184, 22));
        try { f.setPlaceholderString(placeholder == null ? "Gallery Search" : placeholder); } catch (Throwable ignored) {}
        try { f.setCentersPlaceholder(true); } catch (Throwable ignored) {}
        return insertGallerySearchFieldItem(f, index);
    }
    /// Create and add a centered Gallery Search field at end.
    public NSMenuItem addGallerySearchField(String placeholder) {
        return insertGallerySearchField(placeholder, numberOfItems());
    }
    /// Legacy alias: insertCenteredSearchFieldItem — same as insertGallerySearchFieldItem.
    public NSMenuItem insertCenteredSearchFieldItem(NSSearchField field, long index) {
        return insertGallerySearchFieldItem(field, index);
    }

    /// OMITTED: paletteMenuWithColors:... (block selectionHandler, no registered shape);
    /// initWithCoder: (NSCoder); deprecated menuRepresentation/contextMenu/tearOff/menuZone/
    /// attachedMenu/isAttached/sizeToFit/locationForSubmenu:/menuChangedMessages/helpRequested/tornOff.
    // ---- popUp / visible ----
    public boolean popUpMenuPositioningItem(NSMenuItem item, NSPoint loc, NSView view) {
        ensureInit();
        try {
            MemorySegment itemPeer = (item == null) ? MemorySegment.NULL : item.peer();
            MemorySegment viewPeer = (view == null) ? MemorySegment.NULL : view.peer();
            return (boolean) H.hPopUp().invokeExact(peer, Sels.popUpMenuPositioningItem_atLocation_inView, itemPeer, loc.toSegment(), viewPeer);
        } catch (Throwable t) {
            throw new RuntimeException("popUpMenuPositioningItem:atLocation:inView: failed", t);
        }
    }

    /// popUpContextMenu:withEvent:forView: (class).
    public static void popUpContextMenu(NSMenu menu, NSEvent event, NSView view) {
        ensureInit();
        try {
            ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID)).invokeExact(
                    ObjC.cls("NSMenu"), Sels.popUpContextMenu_withEvent_forView,
                    (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()),
                    (MemorySegment) (event == null ? MemorySegment.NULL : event.peer()),
                    (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) { throw new RuntimeException("popUpContextMenu:withEvent:forView: failed", t); }
    }

    /// popUpContextMenu:withEvent:forView:withFont: (4 object args via escape hatch).
    public static void popUpContextMenuWithFont(NSMenu menu, NSEvent event, NSView view, NSFont font) {
        ensureInit();
        ObjC.invokeVoid(ObjC.cls("NSMenu"), Sels.popUpContextMenu_withEvent_forView_withFont,
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()),
                (MemorySegment) (event == null ? MemorySegment.NULL : event.peer()),
                (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()),
                (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
    }

    /// setMenuBarVisible: / menuBarVisible (class).
    public static void setMenuBarVisible(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(ObjC.cls("NSMenu"), Sels.setMenuBarVisible, flag);
    }
    public static boolean menuBarVisible() {
        ensureInit();
        return ObjC.msgSendBool(ObjC.cls("NSMenu"), Sels.menuBarVisible);
    }

    /// indexOfItemWithRepresentedObject: / indexOfItemWithSubmenu:.
    public long indexOfItemWithRepresentedObject(MemorySegment object) {
        ensureInit();
        try {
            return (long) ObjC.handle(Sig.of(Ret.INT, Arg.ID)).invokeExact(peer,
                    Sels.indexOfItemWithRepresentedObject,
                    (MemorySegment) (object == null ? MemorySegment.NULL : object));
        } catch (Throwable t) { throw new RuntimeException("indexOfItemWithRepresentedObject: failed", t); }
    }
    public long indexOfItemWithSubmenu(NSMenu submenu) {
        ensureInit();
        try {
            return (long) ObjC.handle(Sig.of(Ret.INT, Arg.ID)).invokeExact(peer,
                    Sels.indexOfItemWithSubmenu,
                    (MemorySegment) (submenu == null ? MemorySegment.NULL : submenu.peer()));
        } catch (Throwable t) { throw new RuntimeException("indexOfItemWithSubmenu: failed", t); }
    }

    /// indexOfItemWithTarget:andAction: (target id + SEL string).
    public long indexOfItemWithTargetAndAction(MemorySegment target, String action) {
        ensureInit();
        try {
            return (long) ObjC.handle(Sig.of(Ret.INT, Arg.ID, Arg.ID)).invokeExact(peer,
                    Sels.indexOfItemWithTarget_andAction,
                    (MemorySegment) (target == null ? MemorySegment.NULL : target),
                    (MemorySegment) (action == null ? MemorySegment.NULL : ObjC.sel(action)));
        } catch (Throwable t) { throw new RuntimeException("indexOfItemWithTarget:andAction: failed", t); }
    }

    /// performKeyEquivalent:.
    public boolean performKeyEquivalent(NSEvent event) {
        ensureInit();
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer,
                    Sels.performKeyEquivalent,
                    (MemorySegment) (event == null ? MemorySegment.NULL : event.peer()));
        } catch (Throwable t) { throw new RuntimeException("performKeyEquivalent: failed", t); }
    }

    /// cancelTracking / cancelTrackingWithoutAnimation.
    public void cancelTracking() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.cancelTracking);
    }
    public void cancelTrackingWithoutAnimation() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.cancelTrackingWithoutAnimation);
    }

    /// menuBarHeight.
    public double menuBarHeight() {
        ensureInit();
        try {
            return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, Sels.menuBarHeight);
        } catch (Throwable t) { throw new RuntimeException("menuBarHeight failed", t); }
    }

    /// propertiesToUpdate — only valid from within menuNeedsUpdate: (AppKit raises otherwise).
    public long propertiesToUpdate() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.propertiesToUpdate);
    }

    /// userInterfaceLayoutDirection.
    public long userInterfaceLayoutDirection() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.userInterfaceLayoutDirection);
    }
    public void setUserInterfaceLayoutDirection(long dir) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setUserInterfaceLayoutDirection, dir);
    }

    /// automaticallyInsertsWritingToolsItems (macOS 15.2+; guarded by respondsTo).
    public boolean automaticallyInsertsWritingToolsItems() {
        ensureInit();
        if (!respondsTo("automaticallyInsertsWritingToolsItems")) return true;
        return ObjC.msgSendBool(peer, Sels.automaticallyInsertsWritingToolsItems);
    }
    public void setAutomaticallyInsertsWritingToolsItems(boolean flag) {
        ensureInit();
        if (!respondsTo("setAutomaticallyInsertsWritingToolsItems:")) return;
        ObjC.msgSendVoidBool(peer, Sels.setAutomaticallyInsertsWritingToolsItems, flag);
    }

    /// presentationStyle / selectionMode / selectedItems (macOS 14+; guarded).
    public long presentationStyle() {
        ensureInit();
        if (!respondsTo("presentationStyle")) return 0L;
        return ObjC.msgSendLong(peer, Sels.presentationStyle);
    }
    public void setPresentationStyle(long style) {
        ensureInit();
        if (!respondsTo("setPresentationStyle:")) return;
        ObjC.msgSendVoidLong(peer, Sels.setPresentationStyle, style);
    }
    public long selectionMode() {
        ensureInit();
        if (!respondsTo("selectionMode")) return 0L;
        return ObjC.msgSendLong(peer, Sels.selectionMode);
    }
    public void setSelectionMode(long mode) {
        ensureInit();
        if (!respondsTo("setSelectionMode:")) return;
        ObjC.msgSendVoidLong(peer, Sels.setSelectionMode, mode);
    }
    public NSArray selectedItems() {
        ensureInit();
        if (!respondsTo("selectedItems")) return null;
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.selectedItems));
    }
    public void setSelectedItems(NSArray items) {
        ensureInit();
        if (!respondsTo("setSelectedItems:")) return;
        ObjC.msgSendVoidId(peer, Sels.setSelectedItems,
                (MemorySegment) (items == null ? MemorySegment.NULL : items.peer()));
    }

    /// submenuAction:.
    public void submenuAction(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.submenuAction,
                (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
}
