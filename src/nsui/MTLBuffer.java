package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// MTLBuffer — a GPU-visible byte buffer, used for vertex/index data.
/// Created with MTLStorageModeShared by default, so contents() is directly
/// writable from the CPU. Long-lived: hold it while the GPU may read it.
public final class MTLBuffer extends NSObject {

    private MTLBuffer(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLBuffer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLBuffer(peer);
    }

    /// length in bytes.
    public long length() {
        return ObjC.msgSendLong(peer, ObjC.sel("length"));
    }

    /// contents — CPU-visible pointer to the buffer's storage, sized to
    /// length(). Writable for shared/managed storage; null when the buffer is private.
    public MemorySegment contents() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("contents"));
        if (p == null || p.address() == 0) return null;
        return p.reinterpret(length());
    }
}
