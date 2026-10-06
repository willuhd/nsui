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

    /// width / height in pixels.
    public long width() { return ObjC.msgSendLong(peer, ObjC.sel("width")); }
    /// height in pixels.
    public long height() { return ObjC.msgSendLong(peer, ObjC.sel("height")); }

    /// getBytes:bytesPerRow:fromRegion:mipmapLevel: into a Java array.
    /// Region/stride math is the caller's (bytesPerRow >= width * bytesPerPixel).
    public byte[] getBytes(int bytesPerRow, MTLRegion region, long level) {
        ensureInit();
        int h = (int) region.height();
        byte[] out = new byte[bytesPerRow * h];
        MemorySegment buf = nsui.objc.Scratch.allocInput(out.length);
        try {
            handles.hGetBytes().invokeExact(peer, ObjC.sel("getBytes:bytesPerRow:fromRegion:mipmapLevel:"),
                    buf, (long) bytesPerRow, region.toSegment(), level);
        } catch (Throwable t) {
            throw new RuntimeException("getBytes:... failed", t);
        }
        MemorySegment.copy(buf, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, out, 0, out.length);
        return out;
    }
}
