package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// MTLCommandBuffer — one batch of encoded work: encode, commit, optionally wait.
public final class MTLCommandBuffer extends NSObject {

    private MTLCommandBuffer(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLCommandBuffer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLCommandBuffer(peer);
    }

    /// renderCommandEncoderWithDescriptor:.
    public MTLRenderCommandEncoder renderEncoder(MTLRenderPassDescriptor descriptor) {
        return MTLRenderCommandEncoder.wrap(ObjC.msgSendIdId(peer,
                ObjC.sel("renderCommandEncoderWithDescriptor:"), descriptor.peer()));
    }

    /// commit — submit (async return).
    public void commit() {
        ObjC.msgSendVoid(peer, ObjC.sel("commit"));
    }

    /// waitUntilCompleted — block until done (synchronous path; no blocks needed).
    public void waitUntilCompleted() {
        ObjC.msgSendVoid(peer, ObjC.sel("waitUntilCompleted"));
    }

    /// presentDrawable: — schedule drawable presentation on commit.
    public void presentDrawable(CAMetalDrawable drawable) {
        ObjC.msgSendVoidId(peer, ObjC.sel("presentDrawable:"), drawable.peer());
    }
}
