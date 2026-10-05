package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLRenderCommandEncoder — records one render pass: pipeline, vertex data,
/// draws, end. Transient per frame (autoreleased: drain via pool per frame).
public final class MTLRenderCommandEncoder extends NSObject {

    private record Handles(MethodHandle hVertexBytes, MethodHandle hDraw) {}
    private static volatile Handles handles;

    private MTLRenderCommandEncoder(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLRenderCommandEncoder wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLRenderCommandEncoder(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT, Arg.INT)));
    }

    /// Primitive types.
    public static final long PRIMITIVE_TRIANGLE = 3;

    /// setRenderPipelineState:.
    public void setRenderPipelineState(MTLRenderPipelineState state) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setRenderPipelineState:"),
                (MemorySegment) (state == null ? MemorySegment.NULL : state.peer()));
    }

    /// setVertexBytes:length:atIndex: — bytes are copied synchronously.
    public void setVertexBytes(byte[] data, long index) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("vertex data empty");
        MemorySegment buf = nsui.objc.Scratch.allocInput(data.length);
        MemorySegment.copy(data, 0, buf, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, data.length);
        try {
            handles.hVertexBytes().invokeExact(peer, ObjC.sel("setVertexBytes:length:atIndex:"),
                    buf, (long) data.length, index);
        } catch (Throwable t) {
            throw new RuntimeException("setVertexBytes:length:atIndex: failed", t);
        }
    }

    /// drawPrimitives:vertexStart:vertexCount:.
    public void drawTriangles(long start, long count) {
        try {
            handles.hDraw().invokeExact(peer, ObjC.sel("drawPrimitives:vertexStart:vertexCount:"),
                    PRIMITIVE_TRIANGLE, start, count);
        } catch (Throwable t) {
            throw new RuntimeException("drawPrimitives:vertexStart:vertexCount: failed", t);
        }
    }

    /// endEncoding.
    public void endEncoding() {
        ObjC.msgSendVoid(peer, ObjC.sel("endEncoding"));
    }
}
