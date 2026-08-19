package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSComboBox — an AppKit editable combo box (text field + drop-down list).
 * Thin, 1:1, stateless wrapper over a native {@code NSComboBox} (SWT-style):
 * every method maps to one {@code objc_msgSend} selector, no cached Java state
 * beyond the peer. It is an {@link NSControl} (an {@link NSView}), so it fits
 * any view hierarchy and supports enable/disable via {@link #setEnabled}.
 */
public final class NSComboBox extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hAddItem;     // (id, SEL, id) -> void     [addItemWithObjectValue:]
    private static MethodHandle hSelect;      // (id, SEL, long) -> void   [selectItemAtIndex:]
    private static MethodHandle hInsert;      // (id, SEL, id, long) -> void [insertItemWithObjectValue:atIndex:]
    private static MethodHandle hIndexOf;     // (id, SEL, id) -> long     [indexOfItemWithObjectValue:]
    private static MethodHandle hItemValue;   // (id, SEL, long) -> id     [itemObjectValueAtIndex:]

    private NSComboBox(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hAddItem = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hSelect = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hInsert = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hIndexOf = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
        hItemValue = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        initialized = true;
    }

    /** {@code [[NSComboBox alloc] initWithFrame:frame]} — a new combo box at the given rect. */
    public static NSComboBox create(NSRect frame) {
        ensureInit();
        MemorySegment b = ObjC.msgSendId(ObjC.cls("NSComboBox"), ObjC.sel("alloc"));
        try {
            b = (MemorySegment) hInitFrame.invokeExact(b, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSComboBox", t);
        }
        if (b.address() == 0) {
            throw new IllegalStateException("NSComboBox alloc/initWithFrame: returned nil");
        }
        return new NSComboBox(b);
    }

    // ---------------------------------------------------------------- instance API

    /** [combo addItemWithObjectValue:] — append a value to the item list. */
    public void addItemWithObjectValue(String value) {
        try {
            hAddItem.invokeExact(peer, ObjC.sel("addItemWithObjectValue:"), ObjC.nsstring(value));
        } catch (Throwable t) {
            throw new RuntimeException("addItemWithObjectValue: failed", t);
        }
    }

    /** [combo insertItemWithObjectValue:atIndex:] — insert a value at index. */
    public void insertItemWithObjectValueAtIndex(String value, long index) {
        try {
            hInsert.invokeExact(peer, ObjC.sel("insertItemWithObjectValue:atIndex:"), ObjC.nsstring(value), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertItemWithObjectValue:atIndex: failed", t);
        }
    }

    /** [combo removeItemAtIndex:] — remove the item at index. */
    public void removeItemAtIndex(long index) {
        try {
            hSelect.invokeExact(peer, ObjC.sel("removeItemAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("removeItemAtIndex: failed", t);
        }
    }

    /** [combo removeItemWithObjectValue:] — remove the item matching object. */
    public void removeItemWithObjectValue(String value) {
        try {
            hAddItem.invokeExact(peer, ObjC.sel("removeItemWithObjectValue:"), ObjC.nsstring(value));
        } catch (Throwable t) {
            throw new RuntimeException("removeItemWithObjectValue: failed", t);
        }
    }

    /** [combo removeAllItems] — remove all items. */
    public void removeAllItems() {
        ObjC.msgSendVoid(peer, ObjC.sel("removeAllItems"));
    }

    /** [combo selectItemAtIndex:] — select the item at the given index. */
    public void selectItemAtIndex(long index) {
        try {
            hSelect.invokeExact(peer, ObjC.sel("selectItemAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("selectItemAtIndex: failed", t);
        }
    }

    /** [combo deselectItemAtIndex:] — deselect the item at index. */
    public void deselectItemAtIndex(long index) {
        try {
            hSelect.invokeExact(peer, ObjC.sel("deselectItemAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("deselectItemAtIndex: failed", t);
        }
    }

    /** [combo selectItemWithObjectValue:] — select the item matching object. */
    public void selectItemWithObjectValue(String value) {
        try {
            hAddItem.invokeExact(peer, ObjC.sel("selectItemWithObjectValue:"), ObjC.nsstring(value));
        } catch (Throwable t) {
            throw new RuntimeException("selectItemWithObjectValue: failed", t);
        }
    }

    /** [combo indexOfSelectedItem] — index of the current selection, or -1 if none. */
    public long indexOfSelectedItem() {
        return ObjC.msgSendLong(peer, ObjC.sel("indexOfSelectedItem"));
    }

    /** [combo objectValueOfSelectedItem] — the object value of the selected item (or nil). */
    public MemorySegment objectValueOfSelectedItem() {
        return ObjC.msgSendId(peer, ObjC.sel("objectValueOfSelectedItem"));
    }

    /** [combo objectValueOfSelectedItem] as String (convenience). */
    public String objectValueOfSelectedItemString() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("objectValueOfSelectedItem"));
        if (v == null || v.address() == 0) return null;
        // object may be NSString; try toString via ObjC.toString after UTF8String
        // For NSString, toString works; for other objects, description fallback
        String s = ObjC.toString(v);
        if (s != null) return s;
        return ObjC.toString(ObjC.msgSendId(v, ObjC.sel("description")));
    }

    /** [combo itemObjectValueAtIndex:] — object value at index. */
    public MemorySegment itemObjectValueAtIndex(long index) {
        try {
            return (MemorySegment) hItemValue.invokeExact(peer, ObjC.sel("itemObjectValueAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("itemObjectValueAtIndex: failed", t);
        }
    }

    /** [combo indexOfItemWithObjectValue:] — index of item with object value, or NSNotFound. */
    public long indexOfItemWithObjectValue(String value) {
        try {
            return (long) hIndexOf.invokeExact(peer, ObjC.sel("indexOfItemWithObjectValue:"), ObjC.nsstring(value));
        } catch (Throwable t) {
            throw new RuntimeException("indexOfItemWithObjectValue: failed", t);
        }
    }

    /** [combo stringValue] — the string shown/edited in the field. */
    public String stringValue() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("stringValue")));
    }

    /** [combo setEditable:] — YES lets the user type a free-form value. */
    public void setEditable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEditable:"), flag);
    }

    /** [combo numberOfItems] — number of items in the list. */
    public long numberOfItems() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfItems"));
    }

    /** [combo completes] — whether completes as you type. */
    public boolean completes() {
        return ObjC.msgSendBool(peer, ObjC.sel("completes"));
    }

    /** [combo setCompletes:] — set completes behavior. */
    public void setCompletes(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setCompletes:"), flag);
    }

    /** [combo hasVerticalScroller] — whether vertical scroller is shown. */
    public boolean hasVerticalScroller() {
        return ObjC.msgSendBool(peer, ObjC.sel("hasVerticalScroller"));
    }

    /** [combo setHasVerticalScroller:] — set scroller visibility. */
    public void setHasVerticalScroller(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setHasVerticalScroller:"), flag);
    }

    /** [combo usesDataSource] — whether data source drives items. */
    public boolean usesDataSource() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesDataSource"));
    }

    /** [combo setUsesDataSource:] — set data source mode. */
    public void setUsesDataSource(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesDataSource:"), flag);
    }

    /** [combo numberOfVisibleItems] — number of visible items in dropdown. */
    public long numberOfVisibleItems() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfVisibleItems"));
    }

    /** [combo setNumberOfVisibleItems:] — set visible count. */
    public void setNumberOfVisibleItems(long n) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setNumberOfVisibleItems:"), n);
    }

    /** [combo isButtonBordered] — whether button border is shown. */
    public boolean isButtonBordered() {
        return ObjC.msgSendBool(peer, ObjC.sel("isButtonBordered"));
    }

    /** [combo setButtonBordered:] — set button border. */
    public void setButtonBordered(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setButtonBordered:"), flag);
    }

    /** [combo reloadData] — reload from data source. */
    public void reloadData() {
        ObjC.msgSendVoid(peer, ObjC.sel("reloadData"));
    }

    /** [combo noteNumberOfItemsChanged] — inform that count changed (data source). */
    public void noteNumberOfItemsChanged() {
        ObjC.msgSendVoid(peer, ObjC.sel("noteNumberOfItemsChanged"));
    }

    /** [combo itemHeight] — height of each item. */
    public double itemHeight() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("itemHeight"));
        } catch (Throwable t) {
            throw new RuntimeException("itemHeight failed", t);
        }
    }

    /** [combo setItemHeight:] — set item height. */
    public void setItemHeight(double h) {
        try {
            MethodHandle mh = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
            mh.invokeExact(peer, ObjC.sel("setItemHeight:"), h);
        } catch (Throwable t) {
            throw new RuntimeException("setItemHeight: failed", t);
        }
    }

    /** [combo objectValues] — copy of object values array id. */
    public MemorySegment objectValues() {
        return ObjC.msgSendId(peer, ObjC.sel("objectValues"));
    }
}
