package nsui;

import java.lang.foreign.MemorySegment;

/// MTLRenderPipelineState — compiled pipeline (long-lived: create once).
public final class MTLRenderPipelineState extends NSObject {

    private MTLRenderPipelineState(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLRenderPipelineState wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLRenderPipelineState(peer);
    }
}
