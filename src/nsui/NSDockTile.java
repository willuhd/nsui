package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSDockTile — the application's Dock tile: badge text and badge visibility.
/// Thin stateless wrapper; all behavior is AppKit's. Reach it via current().
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
                label == null ? MemorySegment.NULL : ObjC.nsstring(label));
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
}
