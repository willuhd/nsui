package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MPSImageSobel — GPU edge detection. Same encode shape as the blur filter.
public final class MPSImageSobel extends NSObject {

    private record Handles(MethodHandle hEncode) {}
    private static volatile Handles handles;

    private MPSImageSobel(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID)));
    }

    /// Wrap an existing peer.
    public static MPSImageSobel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MPSImageSobel(peer);
    }

    /// initWithDevice:.
    public static MPSImageSobel create(MTLDevice device) {
        try { ObjC.ensureFramework("MetalPerformanceShaders"); } catch (Throwable ignored) {}
        MemorySegment p = ObjC.msgSendId(ObjC.cls("MPSImageSobel"), ObjC.sel("alloc"));
        p = ObjC.msgSendIdId(p, ObjC.sel("initWithDevice:"), device.peer());
        if (p.address() == 0) throw new IllegalStateException("initWithDevice: returned nil for MPSImageSobel");
        return new MPSImageSobel(p);
    }

    /// encodeToCommandBuffer:sourceTexture:destinationTexture:.
    public void encode(MTLCommandBuffer buffer, MTLTexture source, MTLTexture destination) {
        try {
            handles.hEncode().invokeExact(peer, ObjC.sel("encodeToCommandBuffer:sourceTexture:destinationTexture:"),
                    buffer.peer(), source.peer(), destination.peer());
        } catch (Throwable t) {
            throw new RuntimeException("encodeToCommandBuffer:... failed", t);
        }
    }
}
