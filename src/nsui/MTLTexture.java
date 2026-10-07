package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLTexture — GPU image: render target or shader input, with CPU readback.
public final class MTLTexture extends NSObject {

    private record Handles(MethodHandle hGetBytes) {}
    private static volatile Handles handles;

    private MTLTexture(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLTexture wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLTexture(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT, Arg.REGION, Arg.INT)));
    }

    /// width in pixels.
    public long width() { return ObjC.msgSendLong(peer, ObjC.sel("width")); }
    /// height in pixels.
    public long height() { return ObjC.msgSendLong(peer, ObjC.sel("height")); }
    /// pixelFormat (MTLPixelFormat).
    public long pixelFormat() { return ObjC.msgSendLong(peer, ObjC.sel("pixelFormat")); }

    /// Bytes per pixel for the formats this toolkit creates. 0 = unknown, in
    /// which case the row-stride check is skipped rather than guessed.
    private static long bytesPerPixel(long format) {
        // RGBA8Unorm (70) / BGRA8Unorm (80) / Depth32Float / Depth24Unorm_Stencil8.
        if (format == 70 || format == MTLRenderPipelineColorAttachmentDescriptor.PIXEL_FORMAT_BGRA8_UNORM
                || format == MTLTextureDescriptor.PIXEL_FORMAT_DEPTH32_FLOAT
                || format == MTLTextureDescriptor.PIXEL_FORMAT_DEPTH24_UNORM_STENCIL8) return 4;
        if (format == MTLTextureDescriptor.PIXEL_FORMAT_DEPTH32_FLOAT_STENCIL8
                || format == MTLTextureDescriptor.PIXEL_FORMAT_X32_STENCIL8
                || format == MTLTextureDescriptor.PIXEL_FORMAT_X24_STENCIL8) return 8;
        if (format == MTLTextureDescriptor.PIXEL_FORMAT_DEPTH16_UNORM) return 2;
        if (format == MTLTextureDescriptor.PIXEL_FORMAT_STENCIL8) return 1;
        return 0;
    }

    /// getBytes:bytesPerRow:fromRegion:mipmapLevel: into a Java array.
    /// Validates the region against the texture and the row stride against the
    /// pixel size before touching the GPU: an out-of-range region or a too-small
    /// stride is a caller error that Metal would otherwise read out of bounds on
    /// (or hand back a truncated/garbage row). Sizing uses long math so a large
    /// region cannot silently overflow to a short array.
    public byte[] getBytes(int bytesPerRow, MTLRegion region, long level) {
        ensureInit();
        if (region == null) throw new IllegalArgumentException("region is null");
        if (bytesPerRow <= 0) throw new IllegalArgumentException("bytesPerRow must be > 0, got " + bytesPerRow);
        long w = region.width(), h = region.height();
        if (w <= 0 || h <= 0) throw new IllegalArgumentException("region is empty: " + w + "x" + h);
        if (region.z() != 0 || region.depth() != 1) {
            throw new IllegalArgumentException("region z/depth must be 0/1 for 2D textures, got z="
                    + region.z() + " depth=" + region.depth());
        }
        if (level < 0) throw new IllegalArgumentException("mipmap level must be >= 0, got " + level);
        long tw = width(), th = height();
        if (region.x() < 0 || region.y() < 0 || region.x() + w > tw || region.y() + h > th) {
            throw new IllegalArgumentException("region " + region.x() + "," + region.y() + " " + w + "x" + h
                    + " is outside texture " + tw + "x" + th);
        }
        long bpp = bytesPerPixel(pixelFormat());
        if (bpp > 0 && bytesPerRow < w * bpp) {
            throw new IllegalArgumentException("bytesPerRow " + bytesPerRow + " < width*bytesPerPixel "
                    + (w * bpp) + " (format " + pixelFormat() + ")");
        }
        long len = (long) bytesPerRow * h;
        if (len > Integer.MAX_VALUE) throw new IllegalArgumentException("readback too large: " + len + " bytes");
        byte[] out = new byte[(int) len];
        try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(out.length);
            try {
                handles.hGetBytes().invokeExact(peer, ObjC.sel("getBytes:bytesPerRow:fromRegion:mipmapLevel:"),
                        buf, (long) bytesPerRow, region.toSegment(), level);
            } catch (Throwable t) {
                throw new RuntimeException("getBytes:... failed", t);
            }
            MemorySegment.copy(buf, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, out, 0, out.length);
        }
        return out;
    }
}
