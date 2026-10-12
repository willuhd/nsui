package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLTexture — GPU image: render target or shader input, with CPU readback.
public final class MTLTexture extends NSObject {

    private record Handles(MethodHandle hGetBytes, MethodHandle hReplaceRegion) {}
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
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT, Arg.REGION, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.REGION, Arg.INT, Arg.ID, Arg.INT)));
    }

    /// width in pixels.
    public long width() { return ObjC.msgSendLong(peer, ObjC.sel("width")); }
    /// height in pixels.
    public long height() { return ObjC.msgSendLong(peer, ObjC.sel("height")); }
    /// pixelFormat (MTLPixelFormat).
    public long pixelFormat() { return ObjC.msgSendLong(peer, ObjC.sel("pixelFormat")); }

    /// Bytes per pixel for the linear pixel formats Metal can copy linearly, or -1
    /// when this toolkit does not know the format.
    ///
    /// A -1 means "the stride cannot be validated" and the caller is REJECTED (fail
    /// closed): `getBytes:`/`replaceRegion:` size their buffers from
    /// `bytesPerRow * height` while Metal moves `width * pixelSize` per row, so an
    /// unvalidated stride is exactly how a too-small one reads or writes past the
    /// buffer. (The previous version returned 0 and *skipped* the check, silently
    /// disabling it for RGBA16Float, BGRA8Unorm_sRGB, RGBA32Float and every other
    /// format outside a six-entry list.) Values are MTLPixelFormat (MTLPixelFormat.h).
    private static long bytesPerPixel(long format) {
        return switch ((int) format) {
            case 1, 10, 11, 12, 13, 14, 253 -> 1;                // A8Unorm; R8*; R8Unorm_sRGB(11); Stencil8
            case 20, 22, 23, 24, 25, 30, 31, 32, 33, 34, 250 -> 2;   // R16*; RG8*; RG8Unorm_sRGB(31); Depth16Unorm
            case 53, 54, 55, 60, 62, 63, 64, 65,
                 70, 71, 72, 73, 74, 80, 81,
                 90, 91, 92, 93, 94, 252, 255, 261, 262 -> 4;   // +BGR10A2Unorm(94); R32*; RG16*; RGBA8*; BGRA8*; Depth32/24+stencil; X*_Stencil8
            case 103, 104, 105, 110, 112, 113, 114, 115, 260 -> 8; // RG32*; RGBA16*; Depth32Float_Stencil8
            case 123, 124, 125 -> 16;                            // RGBA32*
            default -> -1;                                       // compressed / unknown -> reject
        };
    }

    /// mipmapLevelCount — how many mip levels this texture has (1 when it is not mipmapped).
    public long mipmapLevelCount() {
        ensureInit();
        return ObjC.msgSendLong(peer, ObjC.sel("mipmapLevelCount"));
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
        // A level beyond the texture's mip count is NOT an abort natively — it silently returns
        // zeros (measured: a 4-level 8x8 texture answers level 9 with 0,0,0,0) — so it is rejected
        // here as silent data corruption rather than left to look like a legitimately empty read.
        long mipCount = mipmapLevelCount();
        if (level >= mipCount) {
            throw new IllegalArgumentException("mipmap level " + level + " >= mipmapLevelCount "
                    + mipCount + " (native returns zeros instead of failing)");
        }
        // Bounds come from THIS mip level, not the base level (and level >= 32 would
        // overflow a Java shift, so clamp it).
        long tw = level >= 32 ? 1 : Math.max(1, width() >> (int) level);
        long th = level >= 32 ? 1 : Math.max(1, height() >> (int) level);
        if (region.x() < 0 || region.y() < 0 || region.x() + w > tw || region.y() + h > th) {
            throw new IllegalArgumentException("region " + region.x() + "," + region.y() + " " + w + "x" + h
                    + " is outside texture " + tw + "x" + th);
        }
        long bpp = bytesPerPixel(pixelFormat());
        if (bpp < 0) {
            throw new IllegalArgumentException("cannot validate bytesPerRow for pixelFormat " + pixelFormat()
                    + " (unknown to this toolkit; add it to bytesPerPixel before copying)");
        }
        if (bytesPerRow < w * bpp) {
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

    /// replaceRegion:mipmapLevel:withBytes:bytesPerRow: from a Java array.
    /// Mirrors getBytes' validation: the region is checked against the texture
    /// and the row stride against the pixel size before touching the GPU, so an
    /// out-of-range region or a too-small stride fails here rather than writing
    /// out of bounds. Sizing uses long math so a large region cannot silently
    /// overflow. The upload is copied synchronously and the staging buffer comes
    /// from a confined arena closed here, so it is reclaimed; streaming uploads
    /// should prefer a blit encoder instead of repeated small replaces.
    public void replaceRegion(MTLRegion region, long level, byte[] data, int bytesPerRow) {
        ensureInit();
        if (region == null) throw new IllegalArgumentException("region is null");
        if (data == null) throw new IllegalArgumentException("data is null");
        if (bytesPerRow <= 0) throw new IllegalArgumentException("bytesPerRow must be > 0, got " + bytesPerRow);
        long w = region.width(), h = region.height();
        if (w <= 0 || h <= 0) throw new IllegalArgumentException("region is empty: " + w + "x" + h);
        if (region.z() != 0 || region.depth() != 1) {
            throw new IllegalArgumentException("region z/depth must be 0/1 for 2D textures, got z="
                    + region.z() + " depth=" + region.depth());
        }
        if (level < 0) throw new IllegalArgumentException("mipmap level must be >= 0, got " + level);
        // A level beyond the texture's mip count is NOT an abort natively — it silently returns
        // zeros (measured: a 4-level 8x8 texture answers level 9 with 0,0,0,0) — so it is rejected
        // here as silent data corruption rather than left to look like a legitimately empty read.
        long mipCount = mipmapLevelCount();
        if (level >= mipCount) {
            throw new IllegalArgumentException("mipmap level " + level + " >= mipmapLevelCount "
                    + mipCount + " (native returns zeros instead of failing)");
        }
        // Bounds come from THIS mip level, not the base level (and level >= 32 would
        // overflow a Java shift, so clamp it).
        long tw = level >= 32 ? 1 : Math.max(1, width() >> (int) level);
        long th = level >= 32 ? 1 : Math.max(1, height() >> (int) level);
        if (region.x() < 0 || region.y() < 0 || region.x() + w > tw || region.y() + h > th) {
            throw new IllegalArgumentException("region " + region.x() + "," + region.y() + " " + w + "x" + h
                    + " is outside texture " + tw + "x" + th);
        }
        long bpp = bytesPerPixel(pixelFormat());
        if (bpp < 0) {
            throw new IllegalArgumentException("cannot validate bytesPerRow for pixelFormat " + pixelFormat()
                    + " (unknown to this toolkit; add it to bytesPerPixel before copying)");
        }
        if (bytesPerRow < w * bpp) {
            throw new IllegalArgumentException("bytesPerRow " + bytesPerRow + " < width*bytesPerPixel "
                    + (w * bpp) + " (format " + pixelFormat() + ")");
        }
        long len = (long) bytesPerRow * h;
        if (len > Integer.MAX_VALUE) throw new IllegalArgumentException("upload too large: " + len + " bytes");
        if ((long) data.length < len) {
            throw new IllegalArgumentException("data too short: " + data.length + " < bytesPerRow*height "
                    + len + " (bytesPerRow " + bytesPerRow + " height " + h + ")");
        }
        try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(len);
            MemorySegment.copy(data, 0, buf, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, (int) len);
            try {
                handles.hReplaceRegion().invokeExact(peer,
                        ObjC.sel("replaceRegion:mipmapLevel:withBytes:bytesPerRow:"),
                        region.toSegment(), level, buf, (long) bytesPerRow);
            } catch (Throwable t) {
                throw new RuntimeException("replaceRegion:... failed", t);
            }
        }
    }
}
