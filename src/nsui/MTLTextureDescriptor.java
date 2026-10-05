package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLTextureDescriptor — texture shape: format, size, usage, storage.
public final class MTLTextureDescriptor extends NSObject {

    /// Usage flags.
    public static final long USAGE_SHADER_READ = 0x1;
    public static final long USAGE_RENDER_TARGET = 0x4;

    /// Storage modes.
    public static final long STORAGE_SHARED = 0;

    private record Handles(MethodHandle hDesc) {}
    private static volatile Handles handles;

    private MTLTextureDescriptor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLTextureDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLTextureDescriptor(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.INT, Arg.INT, Arg.BOOL)));
    }

    /// texture2DDescriptorWithPixelFormat:width:height:mipmapped:.
    public static MTLTextureDescriptor texture2D(long pixelFormat, long width, long height, boolean mipmapped) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hDesc().invokeExact(
                    ObjC.cls("MTLTextureDescriptor"),
                    ObjC.sel("texture2DDescriptorWithPixelFormat:width:height:mipmapped:"),
                    pixelFormat, width, height, mipmapped));
        } catch (Throwable t) {
            throw new RuntimeException("texture2DDescriptor... failed", t);
        }
    }

    /// setUsage:.
    public void setUsage(long usage) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setUsage:"), usage);
    }

    /// setStorageMode:.
    public void setStorageMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStorageMode:"), mode);
    }
}
