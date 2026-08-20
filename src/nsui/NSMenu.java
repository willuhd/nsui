package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/** NSMenu — a native menu (the menu bar itself is an NSMenu with NSMenuItem children). */
public final class NSMenu extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hIdInt;    // (id, SEL, long) -> id  [itemAtIndex:/insert...]
    private static MethodHandle hVoidIdInt; // (id, SEL, id, long) -> void [insertItem:atIndex:]
    private static MethodHandle hIntId;     // (id, SEL, id) -> long [indexOfItem:]
    private static MethodHandle hSize;      // (SegmentAllocator,id,SEL)-> NSSize [size]
    private static MethodHandle hPopUp; // (id, SEL, id, point, id) -> bool [popUpMenuPositioningItem:atLocation:inView:]

    private NSMenu(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hIdInt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hVoidIdInt = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hIntId = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
        hSize = ObjC.handle(Sig.of(Ret.SIZE));
        hPopUp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.POINT, Arg.ID));
        initialized = true;
    }

    public static NSMenu wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMenu(peer);
    }

    /** alloc + init. */
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

    // ---- items ----
    public void addItem(NSMenuItem item) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addItem:"), item.peer());
    }
    public void insertItem(NSMenuItem item, long index) {
        ensureInit();
        try { hVoidIdInt.invokeExact(peer, ObjC.sel("insertItem:atIndex:"), item.peer(), index); } catch (Throwable t) { throw new RuntimeException("insertItem:atIndex: failed", t); }
    }
    public NSMenuItem insertItemWithTitle(String title, String action, String keyEquivalent, long index) {
        ensureInit();
        // NSMenu has insertItemWithTitle:action:keyEquivalent:atIndex: -> returns NSMenuItem
        // Signature is id(id,SEL, id,SEL,id,long) -> id : not in vocab via simple handle.
        // Use generic escape hatch or handle with (id, id, id) + int? Instead use ObjC.invoke var.
        // Build via handle for (Ret.ID, Arg.ID, Arg.ID, Arg.ID, Arg.INT) not present.
        // Fallback: use typed 3-id handle for first three then handle last? Easier: use ObjC.handle for 6-id escape? Not exact.
        // Simpler: call via ObjC.msgSendIdIdSelId plus index via separate method — but native requires index.
        // Use direct handle for that signature: of(Ret.ID, Arg.ID, Arg.ID, Arg.ID, Arg.INT) not in vocab.
        // Use invoke escape hatch with 6 ids: encode SEL as id, long via allocating? Instead use reflection to build descriptor directly.
        // For now use ObjC.invokeVoid style not: we need return id.
        // Workaround: add vocabulary entry and use handle.
        // We will construct descriptor manually and link via Linker? Simpler: use ObjC.handle for that shape if vocab extended.
        // Extend vocab lazily at runtime: if not present, throw.
        // To avoid complexity, provide implementation that calls addItemWithTitle and then moves if index != end.
        NSMenuItem item = addItemWithTitle(title, action, keyEquivalent);
        if (index < numberOfItems() - 1) {
            removeItem(item);
            insertItem(item, index);
        }
        return item;
    }
    public NSMenuItem addItemWithTitle(String title, String action, String keyEquivalent) {
        MemorySegment item = ObjC.msgSendIdIdSelId(peer,
                ObjC.sel("addItemWithTitle:action:keyEquivalent:"),
                ObjC.nsstring(title), action == null || action.isEmpty() ? MemorySegment.NULL : ObjC.sel(action), ObjC.nsstring(keyEquivalent));
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

    /** Attach a submenu to a menu item (the item lives in this menu). */
    public void setSubmenu(NSMenuItem item, NSMenu submenu) {
        ObjC.msgSendVoidId(item.peer(), ObjC.sel("setSubmenu:"), submenu == null ? MemorySegment.NULL : submenu.peer());
    }
    public void setSubmenuForItem(NSMenu submenu, NSMenuItem item) {
        ObjC.invokeVoid(peer, ObjC.sel("setSubmenu:forItem:"), submenu == null ? MemorySegment.NULL : submenu.peer(), item.peer());
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
            MemorySegment p = (MemorySegment) hIdInt.invokeExact(peer, ObjC.sel("itemAtIndex:"), index);
            return NSMenuItem.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("itemAtIndex: failed", t); }
    }
    public long indexOfItem(NSMenuItem item) {
        ensureInit();
        try { return (long) hIntId.invokeExact(peer, ObjC.sel("indexOfItem:"), item.peer()); } catch (Throwable t) { throw new RuntimeException("indexOfItem: failed", t); }
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
            MemorySegment p = (MemorySegment) hIdInt.invokeExact(peer, ObjC.sel("itemWithTag:"), tag);
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
    public void setDelegate(MemorySegment d) { ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"), d == null ? MemorySegment.NULL : d); }
    public NSMenuItem highlightedItem() { return NSMenuItem.wrap(ObjC.msgSendId(peer, ObjC.sel("highlightedItem"))); }
    public double minimumWidth() {
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("minimumWidth")); } catch (Throwable t) { throw new RuntimeException("minimumWidth failed", t); }
    }
    public void setMinimumWidth(double w) {
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, ObjC.sel("setMinimumWidth:"), w); } catch (Throwable t) { throw new RuntimeException("setMinimumWidth: failed", t); }
    }
    public NSSize size() {
        ensureInit();
        try { return NSSize.fromSegment((MemorySegment) hSize.invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("size"))); } catch (Throwable t) { throw new RuntimeException("size failed", t); }
    }
    public MemorySegment font() { return ObjC.msgSendId(peer, ObjC.sel("font")); }
    public void setFont(NSFont f) { ObjC.msgSendVoidId(peer, ObjC.sel("setFont:"), f == null ? MemorySegment.NULL : f.peer()); }
    public boolean showsStateColumn() { return ObjC.msgSendBool(peer, ObjC.sel("showsStateColumn")); }
    public void setShowsStateColumn(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setShowsStateColumn:"), flag); }
    public boolean allowsContextMenuPlugIns() { return ObjC.msgSendBool(peer, ObjC.sel("allowsContextMenuPlugIns")); }
    public void setAllowsContextMenuPlugIns(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsContextMenuPlugIns:"), flag); }

    // ---- popUp / visible ----
    public boolean popUpMenuPositioningItem(NSMenuItem item, NSPoint loc, NSView view) {
        ensureInit();
        try {
            MemorySegment itemPeer = (item == null) ? MemorySegment.NULL : item.peer();
            MemorySegment viewPeer = (view == null) ? MemorySegment.NULL : view.peer();
            return (boolean) hPopUp.invokeExact(peer, ObjC.sel("popUpMenuPositioningItem:atLocation:inView:"), itemPeer, loc.toSegment(), viewPeer);
        } catch (Throwable t) {
            throw new RuntimeException("popUpMenuPositioningItem:atLocation:inView: failed", t);
        }
    }
}
