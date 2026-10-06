package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPopUpButton — an AppKit pull-down / pop-up menu control. Thin, 1:1,
/// stateless wrapper over a native `NSPopUpButton` (SWT-style): every
/// method maps to one `objc_msgSend` selector, no cached Java state
/// beyond the peer. It is an `NSControl` (an `NSView`), so it fits
/// any view hierarchy and supports enable/disable and target/action wiring via
/// `setTarget`/`setAction`.
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSPopUpButton.h
/// (note: header declares NSPopUpButton : NSButton, but this wrapper extends NSControl directly;
/// button-level API like setTitle:/menu comes from the header's own declarations below and is
/// wrapped here 1:1).
/// OMITTED: -initWithFrame:pullsDown: takes (NSRect, BOOL), shape (ID,RECT,BOOL) is NOT in the
/// Sig vocabulary (grep Sig.java: no of(Ret.ID, Arg.RECT, Arg.BOOL)); use create(frame)+setPullsDown
/// instead. NSPopUpButtonWillPopUpNotification is an extern string constant, not a selector.
///
/// Target/action: AppKit fires the button's action (by default the shared
/// `"selectionChanged:"` mechanism) on *user* interaction. A
/// programmatic `selectItemAtIndex:` does NOT trigger the action; the
/// wiring is exercised by sending the selector to the target directly (see the
/// tests). `setTarget`/`setAction` are inherited from NSControl.
public final class NSPopUpButton extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hAddItem;     // (id, SEL, id) -> void     [addItemWithTitle:]
    private static MethodHandle hSelect;      // (id, SEL, long) -> void   [selectItemAtIndex:]
    private static MethodHandle hItemTitle;   // (id, SEL, long) -> id     [itemTitleAtIndex:]
    private static MethodHandle hInsert;      // (id, SEL, id, long) -> void [insertItemWithTitle:atIndex:]
    private static MethodHandle hSelectTitle; // (id, SEL, id) -> void     [selectItemWithTitle:]
    private static MethodHandle hItemWithTitle; // (id, SEL, id) -> id     [itemWithTitle:]
    private static MethodHandle hIntId;      // (id, SEL, id) -> long    [indexOfItem:]
    private static MethodHandle hIntInt;     // (id, SEL, long) -> long  [indexOfItemWithTag:]
    private static MethodHandle hIntIdId;    // (id, SEL, id, id) -> long [indexOfItemWithTarget:andAction:]
    private static MethodHandle hBoolInt;    // (id, SEL, long) -> bool  [selectItemWithTag:]
    private static MethodHandle hFactory2;   // (id, SEL, id, id) -> id  [pullDownButtonWithTitle:menu:]
    private static MethodHandle hFactory3;   // (id, SEL, id, id, id) -> id [popUpButtonWithMenu:target:action:]

    private NSPopUpButton(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static void ensureInit() {
        if (initialized) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hAddItem = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hSelect = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hItemTitle = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hInsert = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hSelectTitle = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hItemWithTitle = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hIntId = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
        hIntInt = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
        hIntIdId = ObjC.handle(Sig.of(Ret.INT, Arg.ID, Arg.ID));
        hBoolInt = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
        hFactory2 = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
        hFactory3 = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID));
        initialized = true;
    }

    /// `[[NSPopUpButton alloc] initWithFrame:frame]` — a new popup at the given rect.
        public static NSPopUpButton create(NSRect frame) {
        ensureInit();
        return new NSPopUpButton(ObjC.newView("NSPopUpButton", frame));
    }

    // ---- class factories (shapes ID,ID,ID / ID,ID,ID,ID in vocabulary) ----
    /// +popUpButtonWithMenu:target:action: — standard pop-up with menu (15.0+).
    /// Shape (ID,ID,ID,ID) is in the vocabulary (grep: of(Ret.ID, Arg.ID, Arg.ID, Arg.ID)).
    public static NSPopUpButton popUpButtonWithMenu(NSMenu menu, MemorySegment target, String actionSelector) {
        ensureInit();
        try {
            MemorySegment m = (menu == null ? MemorySegment.NULL : menu.peer());
            MemorySegment t = (MemorySegment) (target == null ? MemorySegment.NULL : target);
            MemorySegment a = actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector);
            MemorySegment p = (MemorySegment) hFactory3.invokeExact(ObjC.cls("NSPopUpButton"),
                    ObjC.sel("popUpButtonWithMenu:target:action:"), m, t, a);
            if (p == null || p.address() == 0) throw new IllegalStateException("popUpButtonWithMenu:target:action: returned nil");
            return new NSPopUpButton(p);
        } catch (Throwable th) {
            throw new RuntimeException("popUpButtonWithMenu:target:action: failed", th);
        }
    }
    /// +pullDownButtonWithTitle:menu: — pull-down with title (usesItemFromMenu=NO).
    /// Shape (ID,ID,ID) is in the vocabulary (grep: of(Ret.ID, Arg.ID, Arg.ID)).
    public static NSPopUpButton pullDownButtonWithTitle(String title, NSMenu menu) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hFactory2.invokeExact(ObjC.cls("NSPopUpButton"),
                    ObjC.sel("pullDownButtonWithTitle:menu:"),
                    (MemorySegment) (title == null ? MemorySegment.NULL : ObjC.nsstring(title)),
                    (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
            if (p == null || p.address() == 0) throw new IllegalStateException("pullDownButtonWithTitle:menu: returned nil");
            return new NSPopUpButton(p);
        } catch (Throwable th) {
            throw new RuntimeException("pullDownButtonWithTitle:menu: failed", th);
        }
    }
    /// +pullDownButtonWithImage:menu: — pull-down with image.
    /// Shape (ID,ID,ID) is in the vocabulary.
    public static NSPopUpButton pullDownButtonWithImage(NSImage image, NSMenu menu) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hFactory2.invokeExact(ObjC.cls("NSPopUpButton"),
                    ObjC.sel("pullDownButtonWithImage:menu:"),
                    (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()),
                    (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
            if (p == null || p.address() == 0) throw new IllegalStateException("pullDownButtonWithImage:menu: returned nil");
            return new NSPopUpButton(p);
        } catch (Throwable th) {
            throw new RuntimeException("pullDownButtonWithImage:menu: failed", th);
        }
    }
    /// +pullDownButtonWithTitle:image:menu: — pull-down with title and image.
    /// Shape (ID,ID,ID,ID) is in the vocabulary.
    public static NSPopUpButton pullDownButtonWithTitleImage(String title, NSImage image, NSMenu menu) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hFactory3.invokeExact(ObjC.cls("NSPopUpButton"),
                    ObjC.sel("pullDownButtonWithTitle:image:menu:"),
                    (MemorySegment) (title == null ? MemorySegment.NULL : ObjC.nsstring(title)),
                    (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()),
                    (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
            if (p == null || p.address() == 0) throw new IllegalStateException("pullDownButtonWithTitle:image:menu: returned nil");
            return new NSPopUpButton(p);
        } catch (Throwable th) {
            throw new RuntimeException("pullDownButtonWithTitle:image:menu: failed", th);
        }
    }

    // ---------------------------------------------------------------- instance API

    /// [popup addItemWithTitle:] — append an item to the menu.
    public void addItemWithTitle(String title) {
        try {
            hAddItem.invokeExact(peer, ObjC.sel("addItemWithTitle:"), ObjC.nsstring(title));
        } catch (Throwable t) {
            throw new RuntimeException("addItemWithTitle: failed", t);
        }
    }

    /// [popup removeAllItems] — remove every item from the menu.
    public void removeAllItems() {
        ObjC.msgSendVoid(peer, ObjC.sel("removeAllItems"));
    }

    /// [popup removeItemAtIndex:] — remove the item at the given index.
    public void removeItemAtIndex(long index) {
        try {
            hSelect.invokeExact(peer, ObjC.sel("removeItemAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("removeItemAtIndex: failed", t);
        }
    }

    /// [popup removeItemWithTitle:] — remove the first item with the given title.
    public void removeItemWithTitle(String title) {
        try {
            hAddItem.invokeExact(peer, ObjC.sel("removeItemWithTitle:"), ObjC.nsstring(title));
        } catch (Throwable t) {
            throw new RuntimeException("removeItemWithTitle: failed", t);
        }
    }

    /// [popup insertItemWithTitle:atIndex:] — insert an item at the given index.
    public void insertItemWithTitleAtIndex(String title, long index) {
        try {
            hInsert.invokeExact(peer, ObjC.sel("insertItemWithTitle:atIndex:"), ObjC.nsstring(title), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertItemWithTitle:atIndex: failed", t);
        }
    }

    /// [popup selectItemAtIndex:] — select the item at the given index.
    public void selectItemAtIndex(long index) {
        try {
            hSelect.invokeExact(peer, ObjC.sel("selectItemAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("selectItemAtIndex: failed", t);
        }
    }

    /// [popup selectItemWithTitle:] — select the item with the given title.
    public void selectItemWithTitle(String title) {
        try {
            hSelectTitle.invokeExact(peer, ObjC.sel("selectItemWithTitle:"), ObjC.nsstring(title));
        } catch (Throwable t) {
            throw new RuntimeException("selectItemWithTitle: failed", t);
        }
    }

    /// [popup indexOfSelectedItem] — index of the current selection, or -1 if none.
    public long indexOfSelectedItem() {
        return ObjC.msgSendLong(peer, ObjC.sel("indexOfSelectedItem"));
    }

    /// [popup numberOfItems] — number of items in the menu.
    public long numberOfItems() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfItems"));
    }

    /// [popup itemTitleAtIndex:] — the title of the item at the given index.
    public String itemTitleAtIndex(long index) {
        try {
            MemorySegment title = (MemorySegment) hItemTitle.invokeExact(peer, ObjC.sel("itemTitleAtIndex:"), index);
            return ObjC.toString(title);
        } catch (Throwable t) {
            throw new RuntimeException("itemTitleAtIndex: failed", t);
        }
    }

    /// [popup titleOfSelectedItem] — the title of the currently selected item.
    public String titleOfSelectedItem() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("titleOfSelectedItem")));
    }

    /// [popup selectedItem] — the currently selected NSMenuItem id (or nil).
    public MemorySegment selectedItem() {
        return ObjC.msgSendId(peer, ObjC.sel("selectedItem"));
    }

    /// [popup itemArray] — array of NSMenuItem ids.
    public MemorySegment itemArray() {
        return ObjC.msgSendId(peer, ObjC.sel("itemArray"));
    }

    /// [popup itemTitles] — copy of titles array.
    public MemorySegment itemTitles() {
        return ObjC.msgSendId(peer, ObjC.sel("itemTitles"));
    }

    /// [popup lastItem] — the last NSMenuItem id (or nil).
    public MemorySegment lastItem() {
        return ObjC.msgSendId(peer, ObjC.sel("lastItem"));
    }

    /// [popup pullsDown] — YES if pull-down style.
    public boolean isPullsDown() {
        return ObjC.msgSendBool(peer, ObjC.sel("pullsDown"));
    }

    /// [popup setPullsDown:] — YES renders as a pull-down button instead of a pop-up.
    public void setPullsDown(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setPullsDown:"), flag);
    }

    /// [popup autoenablesItems] — whether menu items are auto-enabled.
    public boolean autoenablesItems() {
        return ObjC.msgSendBool(peer, ObjC.sel("autoenablesItems"));
    }

    /// [popup setAutoenablesItems:] — set auto-enables behavior.
    public void setAutoenablesItems(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutoenablesItems:"), flag);
    }

    /// [popup menu] — the NSMenu (matches NSResponder.menu() when present; raw peer via menuSegment()).
    /// Shape (ID ()) is in the vocabulary.
    public NSMenu menu() {
        MemorySegment m = ObjC.msgSendId(peer, ObjC.sel("menu"));
        return (m == null || m.address() == 0) ? null : NSMenu.wrap(m);
    }

    /// Raw peer variant (no NSMenu wrap).
    public MemorySegment menuSegment() {
        return ObjC.msgSendId(peer, ObjC.sel("menu"));
    }

    /// [popup setMenu:] raw — set the menu (nil clears).
    public void setMenu(MemorySegment menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMenu:"), (MemorySegment) (menu == null ? MemorySegment.NULL : menu));
    }

    /// Typed menu setter (matches NSResponder.setMenu(NSMenu) when present).
    public void setMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMenu:"),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
    }

    /// Typed menu accessor alias.
    public NSMenu menuTyped() {
        return menu();
    }

    /// [popup preferredEdge] — edge the menu presents from.
    public long preferredEdge() {
        return ObjC.msgSendLong(peer, ObjC.sel("preferredEdge"));
    }

    /// [popup setPreferredEdge:] — set the preferred edge (NSRectEdge).
    public void setPreferredEdge(long edge) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setPreferredEdge:"), edge);
    }

    // ---- completeness: remaining header API in registered shapes ----
    /// [popup usesItemFromMenu] — pull-down uses first-item title (15.0+).
    /// Shapes (BOOL ()) / (VOID,BOOL) are in the vocabulary.
    public boolean usesItemFromMenu() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesItemFromMenu"));
    }
    /// [popup setUsesItemFromMenu:].
    public void setUsesItemFromMenu(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesItemFromMenu:"), flag);
    }
    /// [popup altersStateOfSelectedItem] — selected item gets NSControlStateValueOn (15.0+).
    public boolean altersStateOfSelectedItem() {
        return ObjC.msgSendBool(peer, ObjC.sel("altersStateOfSelectedItem"));
    }
    /// [popup setAltersStateOfSelectedItem:].
    public void setAltersStateOfSelectedItem(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAltersStateOfSelectedItem:"), flag);
    }

    /// [popup addItemsWithTitles:] — append several items at once.
    /// Shape (VOID,ID) is in the vocabulary; reuses hAddItem (same shape as addItemWithTitle:).
    public void addItemsWithTitles(NSArray titles) {
        try {
            hAddItem.invokeExact(peer, ObjC.sel("addItemsWithTitles:"),
                    (MemorySegment) (titles == null ? MemorySegment.NULL : titles.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("addItemsWithTitles: failed", t);
        }
    }

    /// [popup indexOfItem:] — index of the item, or -1/NSNotFound.
    /// Shape (INT,ID) is in the vocabulary (grep: of(Ret.INT, Arg.ID)).
    public long indexOfItem(NSMenuItem item) {
        try {
            return (long) hIntId.invokeExact(peer, ObjC.sel("indexOfItem:"),
                    (MemorySegment) (item == null ? MemorySegment.NULL : item.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("indexOfItem: failed", t);
        }
    }
    /// [popup indexOfItemWithTitle:] — first index with the title, or -1.
    public long indexOfItemWithTitle(String title) {
        try {
            return (long) hIntId.invokeExact(peer, ObjC.sel("indexOfItemWithTitle:"),
                    (MemorySegment) (title == null ? MemorySegment.NULL : ObjC.nsstring(title)));
        } catch (Throwable t) {
            throw new RuntimeException("indexOfItemWithTitle: failed", t);
        }
    }
    /// [popup indexOfItemWithTag:] — first index with the tag, or -1.
    /// Shape (INT,INT) is in the vocabulary (grep: of(Ret.INT, Arg.INT)).
    public long indexOfItemWithTag(long tag) {
        try {
            return (long) hIntInt.invokeExact(peer, ObjC.sel("indexOfItemWithTag:"), tag);
        } catch (Throwable t) {
            throw new RuntimeException("indexOfItemWithTag: failed", t);
        }
    }
    /// [popup indexOfItemWithRepresentedObject:] — first index with the object, or -1.
    /// Shape (INT,ID) is in the vocabulary.
    public long indexOfItemWithRepresentedObject(MemorySegment obj) {
        try {
            return (long) hIntId.invokeExact(peer, ObjC.sel("indexOfItemWithRepresentedObject:"),
                    (MemorySegment) (obj == null ? MemorySegment.NULL : obj));
        } catch (Throwable t) {
            throw new RuntimeException("indexOfItemWithRepresentedObject: failed", t);
        }
    }
    /// [popup indexOfItemWithTarget:andAction:] — first index with target/action, or -1.
    /// Shape (INT,ID,ID) is in the vocabulary (grep: of(Ret.INT, Arg.ID, Arg.ID)).
    public long indexOfItemWithTargetAndAction(MemorySegment target, String actionSelector) {
        try {
            return (long) hIntIdId.invokeExact(peer, ObjC.sel("indexOfItemWithTarget:andAction:"),
                    (MemorySegment) (target == null ? MemorySegment.NULL : target),
                    (MemorySegment) (actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector)));
        } catch (Throwable t) {
            throw new RuntimeException("indexOfItemWithTarget:andAction: failed", t);
        }
    }

    /// [popup itemAtIndex:] — the NSMenuItem at the index (or nil).
    /// Shape (ID,INT) is in the vocabulary; reuses hItemTitle (same shape as itemTitleAtIndex:).
    public NSMenuItem itemAtIndex(long index) {
        try {
            MemorySegment p = (MemorySegment) hItemTitle.invokeExact(peer, ObjC.sel("itemAtIndex:"), index);
            return NSMenuItem.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("itemAtIndex: failed", t);
        }
    }
    /// [popup itemWithTitle:] — first item with the title (or nil).
    /// Shape (ID,ID) is in the vocabulary; uses the cached hItemWithTitle handle.
    public NSMenuItem itemWithTitle(String title) {
        try {
            MemorySegment p = (MemorySegment) hItemWithTitle.invokeExact(peer, ObjC.sel("itemWithTitle:"),
                    (MemorySegment) (title == null ? MemorySegment.NULL : ObjC.nsstring(title)));
            return NSMenuItem.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("itemWithTitle: failed", t);
        }
    }

    /// [popup selectItem:] — select the given item (nil deselects where allowed).
    /// Shape (VOID,ID) is in the vocabulary; reuses hAddItem (same shape).
    public void selectItem(NSMenuItem item) {
        try {
            hAddItem.invokeExact(peer, ObjC.sel("selectItem:"),
                    (MemorySegment) (item == null ? MemorySegment.NULL : item.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("selectItem: failed", t);
        }
    }
    /// [popup selectItemWithTag:] — select first item with the tag; YES when found.
    /// Shape (BOOL,INT) is in the vocabulary (grep: of(Ret.BOOL, Arg.INT)).
    public boolean selectItemWithTag(long tag) {
        try {
            return (boolean) hBoolInt.invokeExact(peer, ObjC.sel("selectItemWithTag:"), tag);
        } catch (Throwable t) {
            throw new RuntimeException("selectItemWithTag: failed", t);
        }
    }
    /// [popup setTitle:] — the button's title text (pull-down static contents).
    /// Shape (VOID,ID) is in the vocabulary; reuses hAddItem (same shape).
    public void setTitle(String title) {
        try {
            hAddItem.invokeExact(peer, ObjC.sel("setTitle:"),
                    (MemorySegment) (title == null ? MemorySegment.NULL : ObjC.nsstring(title)));
        } catch (Throwable t) {
            throw new RuntimeException("setTitle: failed", t);
        }
    }
    /// [popup selectedTag] — tag of the selected item (or -1).
    /// Shape (INT ()) is in the vocabulary.
    public long selectedTag() {
        return ObjC.msgSendLong(peer, ObjC.sel("selectedTag"));
    }
    /// [popup synchronizeTitleAndSelectedItem] — sync button title to selection.
    /// Shape (VOID ()) is in the vocabulary.
    public void synchronizeTitleAndSelectedItem() {
        ObjC.msgSendVoid(peer, ObjC.sel("synchronizeTitleAndSelectedItem"));
    }
}
