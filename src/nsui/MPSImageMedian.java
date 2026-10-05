package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MPSImageMedian — GPU median denoise with an odd kernel diameter.
public final class MPSImageMedian extends NSObject {

    private record Handles(MethodHandle hInit, MethodHandle hEncode) {}
    private static volatile Handles handles;

    private MPSImageMedian(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MPSImageMedian wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MPSImageMedian(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("MetalPerformanceShaders"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID)));
    }

    /// initWithDevice:kernelDiameter: (odd).
    public static MPSImageMedian create(MTLDevice device, long diameter) {
        ensureInit();
        try {
            MemorySegment p = ObjC.msgSendId(ObjC.cls("MPSImageMedian"), ObjC.sel("alloc"));
            p = (MemorySegment) handles.hInit().invokeExact(p, ObjC.sel("initWithDevice:kernelDiameter:"),
                    device.peer(), diameter);
            if (p.address() == 0) throw new IllegalStateException("initWithDevice:kernelDiameter: returned nil");
            return new MPSImageMedian(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithDevice:kernelDiameter: failed", t);
        }
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
