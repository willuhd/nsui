package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// CAMetalDrawable — one frame slot: texture to render into, presented via
/// commandBuffer.presentDrawable: (never call present directly as well).
public final class CAMetalDrawable extends NSObject {

    private CAMetalDrawable(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static CAMetalDrawable wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAMetalDrawable(peer);
    }

    /// texture to render into.
    public MTLTexture texture() {
        return MTLTexture.wrap(ObjC.msgSendId(peer, ObjC.sel("texture")));
    }
}
