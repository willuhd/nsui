package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
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

    private record Handles(MethodHandle hIdInt, MethodHandle hVoidIdInt, MethodHandle hIntId, MethodHandle hSize, MethodHandle hPopUp, MethodHandle hInsertTitleActionKEIndex, MethodHandle hSetSubmenuForItem) {}
    private static volatile Handles H;

    private NSMenu(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.POINT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)));
    }

    public static NSMenu wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMenu(peer);
    }

    /// alloc + init.
    public static NSMenu create() {
        MemorySegment m = ObjC.msgSendId(ObjC.cls("NSMenu"), ObjC.sel("alloc"));
        return new NSMenu(ObjC.msgSendId(m, ObjC.sel("init")));
    }

    public static NSMenu createWithTitle(String title) {
        MemorySegment m = ObjC.msgSendId(ObjC.cls("NSMenu"), ObjC.sel("alloc"));
        MemorySegment n = ObjC.msgSendIdId(m, ObjC.sel("initWithTitle:"), ObjC.nsstring(title));
        return new NSMenu(n);
    }

    // ---- title ----
    public String title() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("title")));
    }
    public void setTitle(String t) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTitle:"), ObjC.nsstring(t));
    }

    // ---- supermenu ----
    public NSMenu supermenu() {
        return wrap(ObjC.msgSendId(peer, ObjC.sel("supermenu")));
    }

    /// [menu setSupermenu:] — set the supermenu (rarely set directly).
    public void setSupermenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSupermenu:"), (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
    }

    /// [menu setItemArray:] — replace the item array.
    public void setItemArray(java.util.List<NSMenuItem> items) {
        if (items == null) return;
        // Build NSArray from items
        MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSArray"), ObjC.sel("alloc"));
        // Use initWithObjects:count: via handle if needed, fallback to adding
        // Simpler: create mutable array and add objects
        MemorySegment mArr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
        for (NSMenuItem it : items) {
            if (it != null) ObjC.msgSendVoidId(mArr, ObjC.sel("addObject:"), it.peer());
        }
        ObjC.msgSendVoidId(peer, ObjC.sel("setItemArray:"), mArr);
    }

    // ---- items ----
    public void addItem(NSMenuItem item) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addItem:"), item.peer());
    }
    public void insertItem(NSMenuItem item, long index) {
        ensureInit();
        try { H.hVoidIdInt().invokeExact(peer, ObjC.sel("insertItem:atIndex:"), item.peer(), index); } catch (Throwable t) { throw new RuntimeException("insertItem:atIndex: failed", t); }
    }
    public NSMenuItem insertItemWithTitle(String title, String action, String keyEquivalent, long index) {
        ensureInit();
        try {
            MemorySegment selAction = (action == null || action.isEmpty()) ? MemorySegment.NULL : ObjC.sel(action);
            // null title is valid for search-field placeholder items (empty title + custom view)
            String safeTitle = title == null ? "" : title;
            String safeKE = keyEquivalent == null ? "" : keyEquivalent;
            MemorySegment p = (MemorySegment) H.hInsertTitleActionKEIndex().invokeExact(peer,
                    ObjC.sel("insertItemWithTitle:action:keyEquivalent:atIndex:"),
                    ObjC.nsstring(safeTitle), selAction, ObjC.nsstring(safeKE), index);
            return NSMenuItem.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("insertItemWithTitle:action:keyEquivalent:atIndex: failed", t);
        }
    }
    public NSMenuItem addItemWithTitle(String title, String action, String keyEquivalent) {
        String safeTitle = title == null ? "" : title;
        String safeKE = keyEquivalent == null ? "" : keyEquivalent;
        MemorySegment item = ObjC.msgSendIdIdSelId(peer,
                ObjC.sel("addItemWithTitle:action:keyEquivalent:"),
                ObjC.nsstring(safeTitle), action == null || action.isEmpty() ? MemorySegment.NULL : ObjC.sel(action), ObjC.nsstring(safeKE));
        return (item == null || item.address() == 0) ? null : NSMenuItem.wrap(item);
    }

    public void removeItemAtIndex(long index) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("removeItemAtIndex:"), index);
    }
    public void removeItem(NSMenuItem item) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeItem:"), item.peer());
    }
    public void removeAllItems() {
        ObjC.msgSendVoid(peer, ObjC.sel("removeAllItems"));
    }

    /// Attach a submenu to a menu item (the item lives in this menu) — [item setSubmenu:submenu].
    public void setSubmenu(NSMenuItem item, NSMenu submenu) {
        ObjC.msgSendVoidId(item.peer(), ObjC.sel("setSubmenu:"), (MemorySegment) (submenu == null ? MemorySegment.NULL : submenu.peer()));
    }
    /// [self setSubmenu:submenu forItem:item] — the NSMenu variant (both peers as ID).
    public void setSubmenuForItem(NSMenu submenu, NSMenuItem item) {
        ensureInit();
        try {
            H.hSetSubmenuForItem().invokeExact(peer, ObjC.sel("setSubmenu:forItem:"),
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
        return ObjC.msgSendId(peer, ObjC.sel("itemArray"));
    }
    public java.util.List<NSMenuItem> itemArray() {
        MemorySegment arr = ObjC.msgSendId(peer, ObjC.sel("itemArray"));
        if (arr == null || arr.address() == 0) return java.util.List.of();
        // NSArray -> count + objectAtIndex:
        long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
        java.util.List<NSMenuItem> out = new java.util.ArrayList<>((int) count);
        MethodHandle hAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        for (long i = 0; i < count; i++) {
            try {
                MemorySegment it = (MemorySegment) hAt.invokeExact(arr, ObjC.sel("objectAtIndex:"), i);
                out.add(it == null || it.address()==0 ? null : NSMenuItem.wrap(it));
            } catch (Throwable t) { throw new RuntimeException("objectAtIndex: failed", t); }
        }
        return java.util.Collections.unmodifiableList(out);
    }
    public long numberOfItems() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfItems"));
    }
    public NSMenuItem itemAtIndex(long index) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) H.hIdInt().invokeExact(peer, ObjC.sel("itemAtIndex:"), index);
            return NSMenuItem.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("itemAtIndex: failed", t); }
    }
    public long indexOfItem(NSMenuItem item) {
        ensureInit();
        try { return (long) H.hIntId().invokeExact(peer, ObjC.sel("indexOfItem:"), item.peer()); } catch (Throwable t) { throw new RuntimeException("indexOfItem: failed", t); }
    }
    public long indexOfItemWithTitle(String title) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("indexOfItemWithTitle:"), ObjC.nsstring(title));
        } catch (Throwable t) { throw new RuntimeException("indexOfItemWithTitle: failed", t); }
    }
    public long indexOfItemWithTag(long tag) {
        try {
            return (long) ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.INT, nsui.objc.Sig.Arg.INT)).invokeExact(peer, ObjC.sel("indexOfItemWithTag:"), tag);
        } catch (Throwable e) { throw new RuntimeException("indexOfItemWithTag: failed", e); }
    }
    public NSMenuItem itemWithTitle(String title) {
        MemorySegment p = ObjC.msgSendIdId(peer, ObjC.sel("itemWithTitle:"), ObjC.nsstring(title));
        return NSMenuItem.wrap(p);
    }
    public NSMenuItem itemWithTag(long tag) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) H.hIdInt().invokeExact(peer, ObjC.sel("itemWithTag:"), tag);
            return NSMenuItem.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("itemWithTag: failed", t); }
    }

    // ---- autoenablesItems / update ----
    public boolean autoenablesItems() {
        return ObjC.msgSendBool(peer, ObjC.sel("autoenablesItems"));
    }
    public void setAutoenablesItems(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutoenablesItems:"), flag);
    }
    public void update() {
        ObjC.msgSendVoid(peer, ObjC.sel("update"));
    }
    public void performActionForItemAtIndex(long index) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("performActionForItemAtIndex:"), index);
    }
    public void itemChanged(NSMenuItem item) {
        ObjC.msgSendVoidId(peer, ObjC.sel("itemChanged:"), item.peer());
    }

    // ---- delegate / appearance ----
    public MemorySegment delegate() { return ObjC.msgSendId(peer, ObjC.sel("delegate")); }
    public void setDelegate(MemorySegment d) { ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"), (MemorySegment) (d == null ? MemorySegment.NULL : d)); }
    public NSMenuItem highlightedItem() { return NSMenuItem.wrap(ObjC.msgSendId(peer, ObjC.sel("highlightedItem"))); }
    public double minimumWidth() {
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("minimumWidth")); } catch (Throwable t) { throw new RuntimeException("minimumWidth failed", t); }
    }
    public void setMinimumWidth(double w) {
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, ObjC.sel("setMinimumWidth:"), w); } catch (Throwable t) { throw new RuntimeException("setMinimumWidth: failed", t); }
    }
    public NSSize size() {
        ensureInit();
        try { return NSSize.fromSegment((MemorySegment) H.hSize().invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("size"))); } catch (Throwable t) { throw new RuntimeException("size failed", t); }
    }
    public MemorySegment font() { return ObjC.msgSendId(peer, ObjC.sel("font")); }
    public void setFont(NSFont f) { ObjC.msgSendVoidId(peer, ObjC.sel("setFont:"), (MemorySegment) (f == null ? MemorySegment.NULL : f.peer())); }
    public boolean showsStateColumn() { return ObjC.msgSendBool(peer, ObjC.sel("showsStateColumn")); }
    public void setShowsStateColumn(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setShowsStateColumn:"), flag); }
    public boolean allowsContextMenuPlugIns() { return ObjC.msgSendBool(peer, ObjC.sel("allowsContextMenuPlugIns")); }
    public void setAllowsContextMenuPlugIns(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsContextMenuPlugIns:"), flag); }

    // ---- Help-search field (showsSearchField) ----
    // AppKit may not expose this selector on all OS versions; guard via respondsToSelector:
    private boolean respondsTo(String selName) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel(selName));
        } catch (Throwable t) { return false; }
    }
    /// [menu showsSearchField] — Help-menu search field visibility (guarded; false if selector absent).
    public boolean showsSearchField() {
        if (!respondsTo("showsSearchField")) return false;
        try { return ObjC.msgSendBool(peer, ObjC.sel("showsSearchField")); } catch (Throwable t) { return false; }
    }
    /// [menu setShowsSearchField:] — Help-menu search field visibility (no-op if selector absent).
    public void setShowsSearchField(boolean flag) {
        if (!respondsTo("setShowsSearchField:")) return;
        try { ObjC.msgSendVoidBool(peer, ObjC.sel("setShowsSearchField:"), flag); } catch (Throwable ignored) {}
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

    // ---- popUp / visible ----
    public boolean popUpMenuPositioningItem(NSMenuItem item, NSPoint loc, NSView view) {
        ensureInit();
        try {
            MemorySegment itemPeer = (item == null) ? MemorySegment.NULL : item.peer();
            MemorySegment viewPeer = (view == null) ? MemorySegment.NULL : view.peer();
            return (boolean) H.hPopUp().invokeExact(peer, ObjC.sel("popUpMenuPositioningItem:atLocation:inView:"), itemPeer, loc.toSegment(), viewPeer);
        } catch (Throwable t) {
            throw new RuntimeException("popUpMenuPositioningItem:atLocation:inView: failed", t);
        }
    }
}
