package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// NSDockTile — the application's Dock tile: badge text and badge visibility.
/// Thin stateless wrapper; all behavior is AppKit's. Reach it via current().
/// OMITTED: NSDockTilePlugIn protocol (setDockTile:, dockMenu) — plugin-host contract,
/// not a tile method; no registered shape is needed here.
public final class NSDockTile extends NSObject {

    private NSDockTile(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer (nil-safe).
    public static NSDockTile wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDockTile(peer);
    }

    /// The shared application's tile ([NSApp dockTile]).
    public static NSDockTile current() {
        return wrap(NSApplication.shared().dockTile());
    }

    /// badgeLabel — text drawn over the icon (nil clears).
    public String badgeLabel() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("badgeLabel")));
    }

    /// setBadgeLabel:.
    public void setBadgeLabel(String label) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBadgeLabel:"),
                (MemorySegment) (label == null ? MemorySegment.NULL : ObjC.nsstring(label)));
    }

    /// showsApplicationBadge.
    public boolean showsApplicationBadge() {
        return ObjC.msgSendBool(peer, ObjC.sel("showsApplicationBadge"));
    }

    /// setShowsApplicationBadge:.
    public void setShowsApplicationBadge(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setShowsApplicationBadge:"), flag);
    }

    /// display — redraw the tile now.
    public void display() {
        ObjC.msgSendVoid(peer, ObjC.sel("display"));
    }

    /// size — tile size in screen coordinates (NSSize struct return).
    public NSSize size() {
        try {
            MemorySegment seg = (MemorySegment) ObjC.handle(Sig.of(Ret.SIZE)).invokeExact(ObjC.structSlot(), peer, ObjC.sel("size"));
            return NSSize.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("size failed", t); }
    }

    /// contentView — custom tile view (nil when none).
    public NSView contentView() {
        return NSView.wrap(ObjC.msgSendId(peer, ObjC.sel("contentView")));
    }

    /// setContentView: (nil clears; call display to redraw).
    public void setContentView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setContentView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
    }

    /// owner — NSApp for the app tile, NSWindow for a miniwindow tile (generic id).
    public MemorySegment owner() {
        return ObjC.msgSendId(peer, ObjC.sel("owner"));
    }
}
