package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// MTLCommandQueue — serial command stream source. Long-lived: create once.
public final class MTLCommandQueue extends NSObject {

    private MTLCommandQueue(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLCommandQueue wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLCommandQueue(peer);
    }

    /// commandBuffer — one-shot buffer (autoreleased: drain via pool per frame).
    public MTLCommandBuffer commandBuffer() {
        return MTLCommandBuffer.wrap(ObjC.msgSendId(peer, ObjC.sel("commandBuffer")));
    }
}
