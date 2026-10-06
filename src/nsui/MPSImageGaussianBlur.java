package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MPSImageGaussianBlur — GPU blur: init with device + sigma, encode
/// source→destination (must not alias). Thin stateless wrapper.
public final class MPSImageGaussianBlur extends NSObject {

    private record Handles(MethodHandle hInit, MethodHandle hEncode) {}
    private static volatile Handles handles;

    private MPSImageGaussianBlur(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MPSImageGaussianBlur wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MPSImageGaussianBlur(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("MetalPerformanceShaders"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID)));
    }

    /// initWithDevice:sigma:.
    public static MPSImageGaussianBlur create(MTLDevice device, float sigma) {
        ensureInit();
        try {
            MemorySegment p = ObjC.msgSendId(ObjC.cls("MPSImageGaussianBlur"), ObjC.sel("alloc"));
            p = (MemorySegment) handles.hInit().invokeExact(p, ObjC.sel("initWithDevice:sigma:"),
                    device.peer(), sigma);
            if (p.address() == 0) throw new IllegalStateException("initWithDevice:sigma: returned nil");
            return new MPSImageGaussianBlur(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithDevice:sigma: failed", t);
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
