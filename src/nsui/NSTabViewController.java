package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTabViewController — container showing one child view controller at a time
/// (segmented-control, toolbar, or custom tab UI; macOS 10.10+). Thin wrapper:
/// each method is one objc_msgSend. Items added must already wrap a view
/// controller (via tabViewItemWithViewController:) or AppKit throws.
/// OMITTED: delegate/toolbar methods (tabView:willSelectTabViewItem:,
/// toolbar:itemForItemIdentifier:...) — they need upcall delegate-proxy
/// machinery (cf. NSToolbarDelegate), out of scope for a stateless wrapper.
public final class NSTabViewController extends NSViewController {

    private record Handles(MethodHandle hInsert) {}
    private static volatile Handles handles;
    private NSTabViewController(MemorySegment peer) { super(peer); ensureInit(); }

    /// Wrap an existing peer (nil-safe).
    public static NSTabViewController wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTabViewController(peer);
    }
    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)));
    }
    /// [[NSTabViewController alloc] init].
    public static NSTabViewController create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSTabViewController"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("NSTabViewController alloc/init returned nil");
        return new NSTabViewController(p);
    }
    /// tabStyle / setTabStyle: (NSTabViewControllerTabStyle).
    public long tabStyle() { return ObjC.msgSendLong(peer, ObjC.sel("tabStyle")); }
    public void setTabStyle(long style) { ObjC.msgSendVoidLong(peer, ObjC.sel("setTabStyle:"), style); }
    /// tabView — the managed tab view.
    public NSTabView tabView() { return NSTabView.wrap(ObjC.msgSendId(peer, ObjC.sel("tabView"))); }
    /// transitionOptions / setter (NSViewControllerTransitionOptions).
    public long transitionOptions() { return ObjC.msgSendLong(peer, ObjC.sel("transitionOptions")); }
    public void setTransitionOptions(long o) { ObjC.msgSendVoidLong(peer, ObjC.sel("setTransitionOptions:"), o); }
    /// canPropagateSelectedChildViewControllerTitle / setter.
    public boolean canPropagateSelectedChildViewControllerTitle() {
        return ObjC.msgSendBool(peer, ObjC.sel("canPropagateSelectedChildViewControllerTitle"));
    }
    public void setCanPropagateSelectedChildViewControllerTitle(boolean f) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setCanPropagateSelectedChildViewControllerTitle:"), f);
    }
    /// selectedTabViewItemIndex / setter.
    public long selectedTabViewItemIndex() { return ObjC.msgSendLong(peer, ObjC.sel("selectedTabViewItemIndex")); }
    public void setSelectedTabViewItemIndex(long i) { ObjC.msgSendVoidLong(peer, ObjC.sel("setSelectedTabViewItemIndex:"), i); }
    /// tabViewItems — one item per child view controller.
    public NSArray tabViewItems() { return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("tabViewItems"))); }
    /// addTabViewItem:.
    public void addTabViewItem(NSTabViewItem item) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addTabViewItem:"), ObjC.nullablePeer(item));
    }
    /// insertTabViewItem:atIndex:.
    public void insertTabViewItem(NSTabViewItem item, long index) {
        ensureInit();
        try {
            handles.hInsert().invokeExact(peer, ObjC.sel("insertTabViewItem:atIndex:"), ObjC.nullablePeer(item), index);
        } catch (Throwable t) { throw new RuntimeException("insertTabViewItem:atIndex: failed", t); }
    }
    /// removeTabViewItem:.
    public void removeTabViewItem(NSTabViewItem item) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeTabViewItem:"), ObjC.nullablePeer(item));
    }
}
