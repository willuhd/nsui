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
/// optional content `NSView`. Only the minimal add/count surface is wrapped here.
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
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSTabView"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSTabView", t);
        }
        if (p.address() == 0) {
            throw new IllegalStateException("NSTabView alloc/initWithFrame: returned nil");
        }
        return new NSTabView(p);
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
            MemorySegment s = (MemorySegment) h.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("minimumSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("minimumSize failed", t);
        }
    }

    /// [tabView contentRect].
    public NSRect contentRect() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("contentRect")));
    }
}
