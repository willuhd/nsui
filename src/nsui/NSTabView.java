package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTabView — an AppKit tabbed pane. Thin, 1:1, stateless wrapper over a native
/// `NSTabView`: every method maps to one `objc_msgSend` selector. It is an
/// `NSView`, so it fits any view hierarchy.
///
/// Tabs are added as `NSTabViewItem`s, each with a `label` and an
/// optional content `NSView`.
///
/// Omitted from NSTabView.h: delegate property + NSTabViewDelegate protocol methods (need upcall
/// machinery); deprecated controlTint; takeSelectedTabViewItemFromSender: IS wrapped (plain action).
public final class NSTabView extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hAddItem;     // (id, SEL, id) -> void    [addTabViewItem:]
    private static MethodHandle hCount;       // (id, SEL) -> long        [numberOfTabViewItems]
    private static MethodHandle hVoidId;      // (id, SEL, id) -> void
    private static MethodHandle hVoidInt;     // (id, SEL, long) -> void
    private static MethodHandle hIdInt;       // (id, SEL, long) -> id
    private static MethodHandle hId;          // (id, SEL) -> id

    private NSTabView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTabView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTabView(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hAddItem = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hCount = ObjC.handle(Sig.of(Ret.INT));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hVoidInt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hIdInt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hId = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
    }

    /// `[[NSTabView alloc] initWithFrame:frame]` — a new tab view at the given rect.
        public static NSTabView create(NSRect frame) {
        ensureInit();
        return new NSTabView(ObjC.newView("NSTabView", frame));
    }

    // ---------------------------------------------------------------- instance API

    /// [tabView addTabViewItem:] — append a tab item.
    public void addTabViewItem(NSTabViewItem item) {
        try {
            hAddItem.invokeExact(peer, ObjC.sel("addTabViewItem:"), item.peer());
        } catch (Throwable t) {
            throw new RuntimeException("addTabViewItem: failed", t);
        }
    }

    /// [tabView numberOfTabViewItems] — how many tabs are present.
    public long numberOfTabViewItems() {
        try {
            return (long) hCount.invokeExact(peer, ObjC.sel("numberOfTabViewItems"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfTabViewItems failed", t);
        }
    }

    // ---------------------------------------------------------------- completeness

    /// [tabView selectTabViewItem:] — select the given item.
    public void selectTabViewItem(NSTabViewItem item) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("selectTabViewItem:"), (MemorySegment) ((MemorySegment) (item == null ? MemorySegment.NULL : item.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("selectTabViewItem: failed", t);
        }
    }

    /// [tabView selectTabViewItemAtIndex:] — select by index.
    public void selectTabViewItemAtIndex(long index) {
        try {
            hVoidInt.invokeExact(peer, ObjC.sel("selectTabViewItemAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("selectTabViewItemAtIndex: failed", t);
        }
    }

    /// [tabView selectTabViewItemWithIdentifier:] — select by identifier.
    public void selectTabViewItemWithIdentifier(String identifier) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("selectTabViewItemWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("selectTabViewItemWithIdentifier: failed", t);
        }
    }

    /// [tabView selectedTabViewItem] — currently selected item or nil.
    public NSTabViewItem selectedTabViewItem() {
        try {
            MemorySegment p = (MemorySegment) hId.invokeExact(peer, ObjC.sel("selectedTabViewItem"));
            return NSTabViewItem.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("selectedTabViewItem failed", t);
        }
    }

    /// [tabView tabViewType] — NSTabViewType.
    public long tabViewType() {
        return ObjC.msgSendLong(peer, ObjC.sel("tabViewType"));
    }

    /// [tabView setTabViewType:].
    public void setTabViewType(long type) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTabViewType:"), type);
    }

    /// [tabView tabPosition] — NSTabPosition.
    public long tabPosition() {
        return ObjC.msgSendLong(peer, ObjC.sel("tabPosition"));
    }

    /// [tabView setTabPosition:].
    public void setTabPosition(long pos) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTabPosition:"), pos);
    }

    /// [tabView tabViewBorderType].
    public long tabViewBorderType() {
        return ObjC.msgSendLong(peer, ObjC.sel("tabViewBorderType"));
    }

    /// [tabView setTabViewBorderType:].
    public void setTabViewBorderType(long t) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTabViewBorderType:"), t);
    }

    /// [tabView tabViewItemAtIndex:].
    public NSTabViewItem tabViewItemAtIndex(long index) {
        try {
            MemorySegment p = (MemorySegment) hIdInt.invokeExact(peer, ObjC.sel("tabViewItemAtIndex:"), index);
            return NSTabViewItem.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("tabViewItemAtIndex: failed", t);
        }
    }

    /// [tabView indexOfTabViewItem:].
    public long indexOfTabViewItem(NSTabViewItem item) {
        // (id, SEL, id) -> long  shape not in vocab? Use handle (Ret.INT, Arg.ID) is present
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("indexOfTabViewItem:"), item.peer());
        } catch (Throwable t) {
            throw new RuntimeException("indexOfTabViewItem: failed", t);
        }
    }

    /// [tabView removeTabViewItem:].
    public void removeTabViewItem(NSTabViewItem item) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("removeTabViewItem:"), item.peer());
        } catch (Throwable t) {
            throw new RuntimeException("removeTabViewItem: failed", t);
        }
    }

    /// [tabView insertTabViewItem:atIndex:].
    public void insertTabViewItem(NSTabViewItem item, long index) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("insertTabViewItem:atIndex:"), item.peer(), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertTabViewItem:atIndex: failed", t);
        }
    }

    /// [tabView allowsTruncatedLabels].
    public boolean allowsTruncatedLabels() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsTruncatedLabels"));
    }

    /// [tabView setAllowsTruncatedLabels:].
    public void setAllowsTruncatedLabels(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsTruncatedLabels:"), flag);
    }

    /// [tabView drawsBackground].
    public boolean drawsBackground() {
        return ObjC.msgSendBool(peer, ObjC.sel("drawsBackground"));
    }

    /// [tabView setDrawsBackground:].
    public void setDrawsBackground(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDrawsBackground:"), flag);
    }

    /// [tabView minimumSize] — NSSize.
    public NSSize minimumSize() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.SIZE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("minimumSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("minimumSize failed", t);
        }
    }

    /// [tabView contentRect].
    public NSRect contentRect() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("contentRect")));
    }

    // ---------------------------------------------------------------- header-completeness batch (NSTabView.h)

    /// [tabView takeSelectedTabViewItemFromSender:] (sender may be nil).
    public void takeSelectedTabViewItemFromSender(NSObject sender) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("takeSelectedTabViewItemFromSender:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("takeSelectedTabViewItemFromSender: failed", t);
        }
    }

    /// [tabView selectFirstTabViewItem:] (sender may be nil).
    public void selectFirstTabViewItem(NSObject sender) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("selectFirstTabViewItem:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("selectFirstTabViewItem: failed", t);
        }
    }

    /// [tabView selectLastTabViewItem:] (sender may be nil).
    public void selectLastTabViewItem(NSObject sender) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("selectLastTabViewItem:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("selectLastTabViewItem: failed", t);
        }
    }

    /// [tabView selectNextTabViewItem:] (sender may be nil).
    public void selectNextTabViewItem(NSObject sender) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("selectNextTabViewItem:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("selectNextTabViewItem: failed", t);
        }
    }

    /// [tabView selectPreviousTabViewItem:] (sender may be nil).
    public void selectPreviousTabViewItem(NSObject sender) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("selectPreviousTabViewItem:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("selectPreviousTabViewItem: failed", t);
        }
    }

    /// [tabView font] — font used for all tab labels.
    public NSFont font() {
        try {
            MemorySegment p = (MemorySegment) hId.invokeExact(peer, ObjC.sel("font"));
            return NSFont.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("font failed", t);
        }
    }

    /// [tabView setFont:].
    public void setFont(NSFont font) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("setFont:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setFont: failed", t);
        }
    }

    /// [tabView tabViewItems] — all tab items.
    public NSArray tabViewItems() {
        try {
            MemorySegment p = (MemorySegment) hId.invokeExact(peer, ObjC.sel("tabViewItems"));
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("tabViewItems failed", t);
        }
    }

    /// [tabView setTabViewItems:].
    public void setTabViewItems(NSArray items) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("setTabViewItems:"), items.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setTabViewItems: failed", t);
        }
    }

    /// [tabView controlSize] — NSControlSize.
    public long controlSize() {
        return ObjC.msgSendLong(peer, ObjC.sel("controlSize"));
    }

    /// [tabView setControlSize:].
    public void setControlSize(long size) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setControlSize:"), size);
    }

    /// [tabView tabViewItemAtPoint:] — item at local point (nil-safe).
    public NSTabViewItem tabViewItemAtPoint(NSPoint point) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.POINT));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("tabViewItemAtPoint:"), point.toSegment());
            return NSTabViewItem.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("tabViewItemAtPoint: failed", t);
        }
    }

    /// [tabView indexOfTabViewItemWithIdentifier:] — NSNotFound when absent.
    public long indexOfTabViewItemWithIdentifier(String identifier) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("indexOfTabViewItemWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("indexOfTabViewItemWithIdentifier: failed", t);
        }
    }

}
