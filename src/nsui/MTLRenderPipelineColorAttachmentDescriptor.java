package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// One pipeline color attachment: pixel format (v1 needs only this).
public final class MTLRenderPipelineColorAttachmentDescriptor extends NSObject {

    /// Pixel formats.
    public static final long PIXEL_FORMAT_BGRA8_UNORM = 80;
    public static final long PIXEL_FORMAT_RGBA16_FLOAT = 115;

    /// MTLBlendFactor — verified against MTLRenderPipeline.h.
    public static final long BLEND_FACTOR_ZERO = 0;
    public static final long BLEND_FACTOR_ONE = 1;
    public static final long BLEND_FACTOR_SOURCE_COLOR = 2;
    public static final long BLEND_FACTOR_ONE_MINUS_SOURCE_COLOR = 3;
    public static final long BLEND_FACTOR_SOURCE_ALPHA = 4;
    public static final long BLEND_FACTOR_ONE_MINUS_SOURCE_ALPHA = 5;

    /// MTLBlendOperation — verified against MTLRenderPipeline.h.
    public static final long BLEND_OPERATION_ADD = 0;
    public static final long BLEND_OPERATION_SUBTRACT = 1;
    public static final long BLEND_OPERATION_REVERSE_SUBTRACT = 2;
    public static final long BLEND_OPERATION_MIN = 3;
    public static final long BLEND_OPERATION_MAX = 4;

    /// MTLColorWriteMask — verified against MTLRenderPipeline.h.
    public static final long WRITE_MASK_NONE = 0;
    public static final long WRITE_MASK_RED = 0x1 << 3;
    public static final long WRITE_MASK_GREEN = 0x1 << 2;
    public static final long WRITE_MASK_BLUE = 0x1 << 1;
    public static final long WRITE_MASK_ALPHA = 0x1 << 0;
    public static final long WRITE_MASK_ALL = 0xF;

    private record Handles(MethodHandle hIsBlending) {}
    private static volatile Handles handles;

    private MTLRenderPipelineColorAttachmentDescriptor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.BOOL)));
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

    /// isBlendingEnabled.
    public boolean isBlendingEnabled() {
        ensureInit();
        try {
            return (boolean) handles.hIsBlending().invokeExact(peer, ObjC.sel("isBlendingEnabled"));
        } catch (Throwable t) {
            throw new RuntimeException("isBlendingEnabled failed", t);
        }
    }

    /// setBlendingEnabled:.
    public void setBlendingEnabled(boolean enabled) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBlendingEnabled:"), enabled);
    }

    /// sourceRGBBlendFactor.
    public long sourceRGBBlendFactor() {
        return ObjC.msgSendLong(peer, ObjC.sel("sourceRGBBlendFactor"));
    }

    /// setSourceRGBBlendFactor:.
    public void setSourceRGBBlendFactor(long factor) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSourceRGBBlendFactor:"), factor);
    }

    /// destinationRGBBlendFactor.
    public long destinationRGBBlendFactor() {
        return ObjC.msgSendLong(peer, ObjC.sel("destinationRGBBlendFactor"));
    }

    /// setDestinationRGBBlendFactor:.
    public void setDestinationRGBBlendFactor(long factor) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDestinationRGBBlendFactor:"), factor);
    }

    /// sourceAlphaBlendFactor.
    public long sourceAlphaBlendFactor() {
        return ObjC.msgSendLong(peer, ObjC.sel("sourceAlphaBlendFactor"));
    }

    /// setSourceAlphaBlendFactor:.
    public void setSourceAlphaBlendFactor(long factor) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSourceAlphaBlendFactor:"), factor);
    }

    /// destinationAlphaBlendFactor.
    public long destinationAlphaBlendFactor() {
        return ObjC.msgSendLong(peer, ObjC.sel("destinationAlphaBlendFactor"));
    }

    /// setDestinationAlphaBlendFactor:.
    public void setDestinationAlphaBlendFactor(long factor) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDestinationAlphaBlendFactor:"), factor);
    }

    /// rgbBlendOperation.
    public long rgbBlendOperation() {
        return ObjC.msgSendLong(peer, ObjC.sel("rgbBlendOperation"));
    }

    /// setRgbBlendOperation:.
    public void setRgbBlendOperation(long operation) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setRgbBlendOperation:"), operation);
    }

    /// alphaBlendOperation.
    public long alphaBlendOperation() {
        return ObjC.msgSendLong(peer, ObjC.sel("alphaBlendOperation"));
    }

    /// setAlphaBlendOperation:.
    public void setAlphaBlendOperation(long operation) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setAlphaBlendOperation:"), operation);
    }

    /// writeMask.
    public long writeMask() {
        return ObjC.msgSendLong(peer, ObjC.sel("writeMask"));
    }

    /// setWriteMask:.
    public void setWriteMask(long mask) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setWriteMask:"), mask);
    }
}
