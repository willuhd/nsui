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

    private NSPopUpButton(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hAddItem = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hSelect = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hItemTitle = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hInsert = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hSelectTitle = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hItemWithTitle = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        initialized = true;
    }

    /// `[[NSPopUpButton alloc] initWithFrame:frame]` — a new popup at the given rect.
    public static NSPopUpButton create(NSRect frame) {
        ensureInit();
        MemorySegment b = ObjC.msgSendId(ObjC.cls("NSPopUpButton"), ObjC.sel("alloc"));
        try {
            b = (MemorySegment) hInitFrame.invokeExact(b, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSPopUpButton", t);
        }
        if (b.address() == 0) {
            throw new IllegalStateException("NSPopUpButton alloc/initWithFrame: returned nil");
        }
        return new NSPopUpButton(b);
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

    /// [popup menu] — the NSMenu id.
    public MemorySegment menu() {
        return ObjC.msgSendId(peer, ObjC.sel("menu"));
    }

    /// [popup setMenu:] — set the menu.
    public void setMenu(MemorySegment menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMenu:"), menu);
    }

    /// Typed menu accessor.
    public NSMenu menuTyped() {
        MemorySegment m = ObjC.msgSendId(peer, ObjC.sel("menu"));
        return (m == null || m.address() == 0) ? null : NSMenu.wrap(m);
    }

    /// [popup preferredEdge] — edge the menu presents from.
    public long preferredEdge() {
        return ObjC.msgSendLong(peer, ObjC.sel("preferredEdge"));
    }

    /// [popup setPreferredEdge:] — set the preferred edge (NSRectEdge).
    public void setPreferredEdge(long edge) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setPreferredEdge:"), edge);
    }
}
