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

    /// renderCommandEncoderWithDescriptor: — throws on a nil result.
    /// Metal returns nil (after logging a validation error) when the pass is
    /// inconsistent with the pipeline or the attachments, and that selector has
    /// no NSError out-param: pass the nil through and the caller silently
    /// encodes nothing, so surface it as a hard failure instead.
    public MTLRenderCommandEncoder renderEncoder(MTLRenderPassDescriptor descriptor) {
        if (descriptor == null) throw new IllegalArgumentException("descriptor is null");
        MTLRenderCommandEncoder enc = MTLRenderCommandEncoder.wrap(ObjC.msgSendIdId(peer,
                ObjC.sel("renderCommandEncoderWithDescriptor:"), descriptor.peer()));
        if (enc == null) {
            throw new IllegalStateException("renderCommandEncoderWithDescriptor: returned nil "
                    + "(pass/pipeline attachment mismatch?)");
        }
        return enc;
    }

    /// commit — submit (async return).
    /// commit — call it EXACTLY ONCE per command buffer. A second commit aborts natively
    /// (addScheduledHandler assertion, exit 134 with API validation on; undefined behaviour
    /// without it), and so does waitUntilCompleted before any commit. Both would need per-buffer
    /// state, which this wrapper deliberately does not keep, so the preconditions are documented.
    public void commit() {
        ObjC.msgSendVoid(peer, ObjC.sel("commit"));
    }

    /// waitUntilCompleted — block until done (synchronous path; no blocks needed).
    public void waitUntilCompleted() {
        ObjC.msgSendVoid(peer, ObjC.sel("waitUntilCompleted"));
    }

    /// presentDrawable: — schedule drawable presentation on commit.
    /// A null drawable (nextDrawable: returned nil this frame) is a no-op,
    /// not a NullPointerException: an empty frame is legal.
    public void presentDrawable(CAMetalDrawable drawable) {
        if (drawable == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("presentDrawable:"), drawable.peer());
    }
}
