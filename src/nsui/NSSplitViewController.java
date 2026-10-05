package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSplitViewController — container for side-by-side children in a managed
/// NSSplitView (macOS 10.10+). Thin wrapper: each method is one objc_msgSend.
/// Items added must already wrap a view controller (via
/// splitViewItemWithViewController:) or AppKit throws.
/// OMITTED: delegate methods (splitView:canCollapseSubview:, ...) and
/// validateUserInterfaceItem: — they need upcall delegate-proxy machinery
/// (cf. NSToolbarDelegate), out of scope for a stateless wrapper.
public final class NSSplitViewController extends NSViewController {

    private record Handles(MethodHandle hInsert, MethodHandle hDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;
    private NSSplitViewController(MemorySegment peer) { super(peer); ensureInit(); }

    /// Wrap an existing peer (nil-safe).
    public static NSSplitViewController wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSplitViewController(peer);
    }
    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)), ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
    }
    /// [[NSSplitViewController alloc] init].
    public static NSSplitViewController create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSSplitViewController"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("NSSplitViewController alloc/init returned nil");
        return new NSSplitViewController(p);
    }
    /// splitView — managed split view (raw peer; no NSSplitViewItem wrapper exists).
    public MemorySegment splitView() { return ObjC.msgSendId(peer, ObjC.sel("splitView")); }
    /// splitViewItems — one item per child view controller.
    public NSArray splitViewItems() { return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("splitViewItems"))); }
    /// addSplitViewItem: (raw peer).
    public void addSplitViewItem(MemorySegment item) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addSplitViewItem:"), ObjC.nullable(item));
    }
    /// insertSplitViewItem:atIndex:.
    public void insertSplitViewItem(MemorySegment item, long index) {
        ensureInit();
        try {
            handles.hInsert().invokeExact(peer, ObjC.sel("insertSplitViewItem:atIndex:"), ObjC.nullable(item), index);
        } catch (Throwable t) { throw new RuntimeException("insertSplitViewItem:atIndex: failed", t); }
    }
    /// removeSplitViewItem:.
    public void removeSplitViewItem(MemorySegment item) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeSplitViewItem:"), ObjC.nullable(item));
    }
    /// minimumThicknessForInlineSidebars / setter (CGFloat).
    public double minimumThicknessForInlineSidebars() {
        ensureInit();
        try {
            return (double) handles.hDouble().invokeExact(peer, ObjC.sel("minimumThicknessForInlineSidebars"));
        } catch (Throwable t) { throw new RuntimeException("minimumThicknessForInlineSidebars failed", t); }
    }
    public void setMinimumThicknessForInlineSidebars(double thickness) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setMinimumThicknessForInlineSidebars:"), thickness);
        } catch (Throwable t) { throw new RuntimeException("setMinimumThicknessForInlineSidebars: failed", t); }
    }
    /// toggleSidebar: — collapse/expand the first sidebar (no-op when none).
    public void toggleSidebar(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleSidebar:"), ObjC.nullablePeer(sender));
    }
    /// toggleInspector: — collapse/expand the first inspector (no-op when none).
    public void toggleInspector(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleInspector:"), ObjC.nullablePeer(sender));
    }
}
