package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// MTLFunction — one compiled shader entry point.
public final class MTLFunction extends NSObject {

    private MTLFunction(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLFunction wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLFunction(peer);
    }

    /// name (nil-safe).
    public String name() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("name")));
    }
}
