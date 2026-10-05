package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// One pipeline color attachment: pixel format (v1 needs only this).
public final class MTLRenderPipelineColorAttachmentDescriptor extends NSObject {

    /// Pixel formats.
    public static final long PIXEL_FORMAT_BGRA8_UNORM = 80;
    public static final long PIXEL_FORMAT_RGBA16_FLOAT = 115;

    private MTLRenderPipelineColorAttachmentDescriptor(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLRenderPipelineColorAttachmentDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLRenderPipelineColorAttachmentDescriptor(peer);
    }

    /// pixelFormat.
    public long pixelFormat() {
        return ObjC.msgSendLong(peer, ObjC.sel("pixelFormat"));
    }

    /// setPixelFormat:.
    public void setPixelFormat(long format) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setPixelFormat:"), format);
    }
}
