package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// MTLDepthStencilState — a compiled depth/stencil state (opaque device
/// object), immutable once created by the device. Long-lived: create once.
public final class MTLDepthStencilState extends NSObject {

    private MTLDepthStencilState(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLDepthStencilState wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLDepthStencilState(peer);
    }

    /// label (the label copied from the descriptor, or null).
    public String label() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("label")));
    }

    /// device — the MTLDevice this state was compiled against.
    public MTLDevice device() {
        return MTLDevice.wrap(ObjC.msgSendId(peer, ObjC.sel("device")));
    }
}
