package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLRenderCommandEncoder — records one render pass: pipeline, depth/stencil
/// and rasterizer state, vertex data, draws, end. Transient per frame
/// (autoreleased: drain via pool per frame).
public final class MTLRenderCommandEncoder extends NSObject {

    private record Handles(MethodHandle hVertexBytes, MethodHandle hDraw,
            MethodHandle hStencilFront, MethodHandle hDepthBias,
            MethodHandle hViewport, MethodHandle hScissorRect,
            MethodHandle hVertexBuffer, MethodHandle hDrawIndexed,
            MethodHandle hObjIndex) {}
    private static volatile Handles handles;

    private MTLRenderCommandEncoder(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLRenderCommandEncoder wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLRenderCommandEncoder(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT, Arg.FLOAT, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.MTLVIEWPORT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.MTLSCISSORRECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT, Arg.INT, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)));
    }

    /// Primitive types (MTLPrimitiveType, MTLRenderCommandEncoder.h).
    public static final long PRIMITIVE_POINT = 0;
    public static final long PRIMITIVE_LINE = 1;
    public static final long PRIMITIVE_LINE_STRIP = 2;
    public static final long PRIMITIVE_TRIANGLE = 3;
    public static final long PRIMITIVE_TRIANGLE_STRIP = 4;

    /// Index types (MTLIndexType, MTLArgument.h).
    public static final long INDEX_TYPE_UINT16 = 0;
    public static final long INDEX_TYPE_UINT32 = 1;

    /// Cull modes (MTLCullMode, MTLRenderCommandEncoder.h).
    public static final long CULL_NONE = 0;
    public static final long CULL_FRONT = 1;
    public static final long CULL_BACK = 2;

    /// Winding orders (MTLWinding, MTLRenderCommandEncoder.h).
    public static final long WINDING_CLOCKWISE = 0;
    public static final long WINDING_COUNTER_CLOCKWISE = 1;

    /// Depth clip modes (MTLDepthClipMode, MTLRenderCommandEncoder.h).
    public static final long DEPTH_CLIP_CLIP = 0;
    public static final long DEPTH_CLIP_CLAMP = 1;

    /// setRenderPipelineState:.
    public void setRenderPipelineState(MTLRenderPipelineState state) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setRenderPipelineState:"), ObjC.nullablePeer(state));
    }

    /// setDepthStencilState: (nil disables depth/stencil testing).
    public void setDepthStencilState(MTLDepthStencilState state) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDepthStencilState:"), ObjC.nullablePeer(state));
    }

    /// setStencilReferenceValue: (uint32_t).
    public void setStencilReferenceValue(long value) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStencilReferenceValue:"), value & 0xFFFFFFFFL);
    }

    /// setStencilFrontReferenceValue:backReferenceValue: (two uint32_t).
    public void setStencilFrontReferenceValue(long front, long back) {
        ensureInit();
        try {
            handles.hStencilFront().invokeExact(peer, ObjC.sel("setStencilFrontReferenceValue:backReferenceValue:"),
                    front & 0xFFFFFFFFL, back & 0xFFFFFFFFL);
        } catch (Throwable t) {
            throw new RuntimeException("setStencilFrontReferenceValue:backReferenceValue: failed", t);
        }
    }

    /// setDepthBias:slopeScale:clamp: (three floats).
    public void setDepthBias(float bias, float slopeScale, float clamp) {
        ensureInit();
        try {
            handles.hDepthBias().invokeExact(peer, ObjC.sel("setDepthBias:slopeScale:clamp:"),
                    bias, slopeScale, clamp);
        } catch (Throwable t) {
            throw new RuntimeException("setDepthBias:slopeScale:clamp: failed", t);
        }
    }

    /// setDepthClipMode:.
    public void setDepthClipMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDepthClipMode:"), mode);
    }

    /// setCullMode:.
    public void setCullMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setCullMode:"), mode);
    }

    /// setFrontFacingWinding:.
    public void setFrontFacingWinding(long winding) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setFrontFacingWinding:"), winding);
    }

    /// setViewport: — MTLViewport (6 doubles) passed BY VALUE.
    public void setViewport(MTLViewport viewport) {
        if (viewport == null) throw new IllegalArgumentException("viewport is null");
        ensureInit();
        try {
            handles.hViewport().invokeExact(peer, ObjC.sel("setViewport:"), viewport.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setViewport: failed", t);
        }
    }

    /// setScissorRect: — MTLScissorRect (4 NSUInteger) passed BY VALUE.
    public void setScissorRect(MTLScissorRect rect) {
        if (rect == null) throw new IllegalArgumentException("rect is null");
        ensureInit();
        try {
            handles.hScissorRect().invokeExact(peer, ObjC.sel("setScissorRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setScissorRect: failed", t);
        }
    }

    /// setVertexBytes:length:atIndex: — bytes are copied synchronously.
    public void setVertexBytes(byte[] data, long index) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("vertex data empty");
        ensureInit();
        MemorySegment buf = nsui.objc.Scratch.allocInput(data.length);
        MemorySegment.copy(data, 0, buf, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, data.length);
        try {
            handles.hVertexBytes().invokeExact(peer, ObjC.sel("setVertexBytes:length:atIndex:"),
                    buf, (long) data.length, index);
        } catch (Throwable t) {
            throw new RuntimeException("setVertexBytes:length:atIndex: failed", t);
        }
    }

    /// setVertexBuffer:offset:atIndex: (nil unbinds the slot).
    public void setVertexBuffer(MTLBuffer buffer, long offset, long index) {
        ensureInit();
        try {
            handles.hVertexBuffer().invokeExact(peer, ObjC.sel("setVertexBuffer:offset:atIndex:"),
                    ObjC.nullablePeer(buffer), offset, index);
        } catch (Throwable t) {
            throw new RuntimeException("setVertexBuffer:offset:atIndex: failed", t);
        }
    }

    /// setFragmentBytes:length:atIndex: — bytes are copied synchronously.
    public void setFragmentBytes(byte[] data, long index) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("fragment data empty");
        ensureInit();
        MemorySegment buf = nsui.objc.Scratch.allocInput(data.length);
        MemorySegment.copy(data, 0, buf, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, data.length);
        try {
            handles.hVertexBytes().invokeExact(peer, ObjC.sel("setFragmentBytes:length:atIndex:"),
                    buf, (long) data.length, index);
        } catch (Throwable t) {
            throw new RuntimeException("setFragmentBytes:length:atIndex: failed", t);
        }
    }

    /// setFragmentBuffer:offset:atIndex: (nil unbinds the slot).
    public void setFragmentBuffer(MTLBuffer buffer, long offset, long index) {
        ensureInit();
        try {
            handles.hVertexBuffer().invokeExact(peer, ObjC.sel("setFragmentBuffer:offset:atIndex:"),
                    ObjC.nullablePeer(buffer), offset, index);
        } catch (Throwable t) {
            throw new RuntimeException("setFragmentBuffer:offset:atIndex: failed", t);
        }
    }

    /// setVertexTexture:atIndex: (nil unbinds the slot).
    public void setVertexTexture(MTLTexture texture, long index) {
        ensureInit();
        try {
            handles.hObjIndex().invokeExact(peer, ObjC.sel("setVertexTexture:atIndex:"),
                    ObjC.nullablePeer(texture), index);
        } catch (Throwable t) {
            throw new RuntimeException("setVertexTexture:atIndex: failed", t);
        }
    }

    /// setFragmentTexture:atIndex: (nil unbinds the slot).
    public void setFragmentTexture(MTLTexture texture, long index) {
        ensureInit();
        try {
            handles.hObjIndex().invokeExact(peer, ObjC.sel("setFragmentTexture:atIndex:"),
                    ObjC.nullablePeer(texture), index);
        } catch (Throwable t) {
            throw new RuntimeException("setFragmentTexture:atIndex: failed", t);
        }
    }

    /// setVertexSamplerState:atIndex: (nil unbinds the slot).
    public void setVertexSamplerState(MTLSamplerState sampler, long index) {
        ensureInit();
        try {
            handles.hObjIndex().invokeExact(peer, ObjC.sel("setVertexSamplerState:atIndex:"),
                    ObjC.nullablePeer(sampler), index);
        } catch (Throwable t) {
            throw new RuntimeException("setVertexSamplerState:atIndex: failed", t);
        }
    }

    /// setFragmentSamplerState:atIndex: (nil unbinds the slot).
    public void setFragmentSamplerState(MTLSamplerState sampler, long index) {
        ensureInit();
        try {
            handles.hObjIndex().invokeExact(peer, ObjC.sel("setFragmentSamplerState:atIndex:"),
                    ObjC.nullablePeer(sampler), index);
        } catch (Throwable t) {
            throw new RuntimeException("setFragmentSamplerState:atIndex: failed", t);
        }
    }

    /// drawPrimitives:vertexStart:vertexCount:.
    public void drawTriangles(long start, long count) {
        ensureInit();
        try {
            handles.hDraw().invokeExact(peer, ObjC.sel("drawPrimitives:vertexStart:vertexCount:"),
                    PRIMITIVE_TRIANGLE, start, count);
        } catch (Throwable t) {
            throw new RuntimeException("drawPrimitives:vertexStart:vertexCount: failed", t);
        }
    }

    /// drawIndexedPrimitives:indexCount:indexType:indexBuffer:indexBufferOffset:.
    public void drawIndexed(long indexCount, long indexType, MTLBuffer indexBuffer, long indexBufferOffset) {
        if (indexBuffer == null) throw new IllegalArgumentException("indexBuffer is null");
        ensureInit();
        try {
            handles.hDrawIndexed().invokeExact(peer,
                    ObjC.sel("drawIndexedPrimitives:indexCount:indexType:indexBuffer:indexBufferOffset:"),
                    PRIMITIVE_TRIANGLE, indexCount, indexType, indexBuffer.peer(), indexBufferOffset);
        } catch (Throwable t) {
            throw new RuntimeException("drawIndexedPrimitives:... failed", t);
        }
    }

    /// endEncoding.
    public void endEncoding() {
        ObjC.msgSendVoid(peer, ObjC.sel("endEncoding"));
    }
}
