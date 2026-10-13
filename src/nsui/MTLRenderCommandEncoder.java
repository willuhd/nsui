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
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT32, Arg.INT32)),
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
        Bounds.nonNull(state, "setRenderPipelineState:");
        ObjC.msgSendVoidId(peer, ObjC.sel("setRenderPipelineState:"), state.peer());
    }

    /// setDepthStencilState: (nil disables depth/stencil testing).
    public void setDepthStencilState(MTLDepthStencilState state) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDepthStencilState:"), ObjC.nullablePeer(state));
    }

    /// setStencilReferenceValue: (uint32_t, exact 32-bit INT32 call shape).
    public void setStencilReferenceValue(long value) {
        ensureInit();
        try {
            ObjC.handle(Sig.of(Ret.VOID, Arg.INT32)).invokeExact(peer, ObjC.sel("setStencilReferenceValue:"), (int) value);
        } catch (Throwable t) { throw new RuntimeException("setStencilReferenceValue: failed", t); }
    }

    /// setStencilFrontReferenceValue:backReferenceValue: (two uint32_t).
    public void setStencilFrontReferenceValue(long front, long back) {
        ensureInit();
        try {
            handles.hStencilFront().invokeExact(peer, ObjC.sel("setStencilFrontReferenceValue:backReferenceValue:"),
                    (int) front, (int) back);
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
        // Measured: any NaN (or infinity) component aborts — "Viewport Validation viewport.originX
        // must not be infinite or NaN" (exit 134). Negative width/height is tolerated.
        Bounds.finite(viewport.originX(), "setViewport: (originX)");
        Bounds.finite(viewport.originY(), "setViewport: (originY)");
        Bounds.finite(viewport.width(), "setViewport: (width)");
        Bounds.finite(viewport.height(), "setViewport: (height)");
        Bounds.finite(viewport.znear(), "setViewport: (znear)");
        Bounds.finite(viewport.zfar(), "setViewport: (zfar)");
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

    /// Metal's buffer-argument limits, measured with the API validation layer on:
    ///   index  must be < 31  (so 0..30 are the valid slots; 31 aborts for BOTH the vertex and
    ///           fragment stages — "index(31) must be < 31") — the same limit applies to the
    ///           compute encoder's setBuffer:.
    ///   offset must be < the buffer's length ("offset(16) must be < [buffer length](16)");
    ///           offset 15 in a 16-byte buffer is accepted. A nil buffer unbinds the slot and
    ///           takes no offset.
    /// Both are stateless argument checks, so they are enforced here instead of leaving a caller
    /// to discover them as an uncatchable assertion.
    public static final long MAX_BUFFER_SLOTS = 31L;

    private static void checkBufferArgs(MTLBuffer buffer, long offset, long index, String what) {
        Bounds.index(index, MAX_BUFFER_SLOTS, what);
        Bounds.nonNegative(offset, what + " (offset)");
        if (buffer == null) {
            // A nil buffer unbinds the slot, and Metal then REQUIRES offset 0: a nil buffer with a
            // nonzero offset aborts ("offset(999999) must be 0", exit 134). An earlier version of
            // this guard returned early for nil and its comment claimed the offset was ignored —
            // measurably false.
            if (offset != 0) {
                throw new IllegalArgumentException(what + ": a nil (unbinding) buffer requires "
                        + "offset 0, got " + offset);
            }
            return;
        }
        if (offset >= buffer.length()) {
            throw new IllegalArgumentException(what + ": offset " + offset
                    + " must be < the buffer length (" + buffer.length() + ")");
        }
    }

    /// Metal's per-stage sampler and texture slot counts, measured the same way (the assertion
    /// names the bound): sampler indices are 0..15 ("index(16) must be < 16") and texture indices
    /// are 0..127 ("index(128) must be < 128").
    public static final long MAX_SAMPLER_SLOTS = 16L;
    public static final long MAX_TEXTURE_SLOTS = 128L;

    /// setVertexBytes:length:atIndex: — bytes are copied synchronously.
    ///
    /// Metal caps this call at 4096 bytes: with API validation enabled (MTL_DEBUG_LAYER=1) a
    /// larger length aborts with "Set Vertex Bytes Validation length(8192) must be <= 4096"
    /// (verified, exit 134; 4096 itself is accepted), and without validation it is silent
    /// undefined behaviour. Larger data must go through an MTLBuffer instead.
    public static final long MAX_SET_BYTES = 4096L;

    private static void checkSetBytesLength(int length, String what) {
        if (length > MAX_SET_BYTES) {
            throw new IllegalArgumentException(what + ": length " + length
                    + " exceeds Metal's " + MAX_SET_BYTES + "-byte limit for set*Bytes (use an MTLBuffer)");
        }
    }

    public void setVertexBytes(byte[] data, long index) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("vertex data empty");
        checkSetBytesLength(data.length, "setVertexBytes:length:atIndex:");
        Bounds.index(index, MAX_BUFFER_SLOTS, "setVertexBytes:length:atIndex:");   // index 31 aborts
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
        checkBufferArgs(buffer, offset, index, "setVertexBuffer:offset:atIndex:");
        try {
            handles.hVertexBuffer().invokeExact(peer, ObjC.sel("setVertexBuffer:offset:atIndex:"),
                    ObjC.nullablePeer(buffer), offset, index);
        } catch (Throwable t) {
            throw new RuntimeException("setVertexBuffer:offset:atIndex: failed", t);
        }
    }

    /// setFragmentBytes:length:atIndex: — bytes are copied synchronously.
    /// Same 4096-byte Metal cap as setVertexBytes (see checkSetBytesLength).
    public void setFragmentBytes(byte[] data, long index) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("fragment data empty");
        checkSetBytesLength(data.length, "setFragmentBytes:length:atIndex:");
        Bounds.index(index, MAX_BUFFER_SLOTS, "setFragmentBytes:length:atIndex:");   // index 31 aborts
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
        checkBufferArgs(buffer, offset, index, "setFragmentBuffer:offset:atIndex:");
        try {
            handles.hVertexBuffer().invokeExact(peer, ObjC.sel("setFragmentBuffer:offset:atIndex:"),
                    ObjC.nullablePeer(buffer), offset, index);
        } catch (Throwable t) {
            throw new RuntimeException("setFragmentBuffer:offset:atIndex: failed", t);
        }
    }

    /// setVertexTexture:atIndex: (nil unbinds the slot).
    public void setVertexTexture(MTLTexture texture, long index) {
        Bounds.index(index, MAX_TEXTURE_SLOTS, "setVertexTexture:atIndex:");   // measured native slot limit
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
        Bounds.index(index, MAX_TEXTURE_SLOTS, "setFragmentTexture:atIndex:");   // measured native slot limit
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
        Bounds.index(index, MAX_SAMPLER_SLOTS, "setVertexSamplerState:atIndex:");   // measured native slot limit
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
        Bounds.index(index, MAX_SAMPLER_SLOTS, "setFragmentSamplerState:atIndex:");   // measured native slot limit
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
        // Measured: the index range is validated against the buffer before the draw —
        // "indexBufferOffset(0) + (indexCount(5)*4) must be <= [indexBuffer length](16)", exit 134
        // — so an out-of-range draw is rejected here. (Drawing also requires a pipeline state and
        // the draw call itself is stateful, which is why only the stateless part is guarded.)
        Bounds.nonNegative(indexCount, "drawIndexedPrimitives:... (indexCount)");
        Bounds.nonNegative(indexBufferOffset, "drawIndexedPrimitives:... (indexBufferOffset)");
        if (indexType != INDEX_TYPE_UINT16 && indexType != INDEX_TYPE_UINT32) {
            throw new IllegalArgumentException("indexType must be INDEX_TYPE_UINT16 (0) or "
                    + "INDEX_TYPE_UINT32 (1), got " + indexType);
        }
        long indexSize = indexType == INDEX_TYPE_UINT32 ? 4L : 2L;
        long needed;
        try {
            needed = Math.addExact(indexBufferOffset, Math.multiplyExact(indexCount, indexSize));
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("drawIndexedPrimitives: index range overflows a long");
        }
        if (needed > indexBuffer.length()) {
            throw new IllegalArgumentException("drawIndexedPrimitives: offset " + indexBufferOffset
                    + " + indexCount " + indexCount + "*" + indexSize + " needs " + needed
                    + " bytes but the index buffer holds " + indexBuffer.length());
        }
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
    /// endEncoding — call it EXACTLY ONCE per encoder. A second call aborts natively ("endEncoding"
    /// assertion, exit 134 with API validation on; undefined behaviour without it). Guarding it
    /// would need per-encoder state, which this wrapper deliberately does not keep, so the
    /// precondition is documented instead.
    public void endEncoding() {
        ObjC.msgSendVoid(peer, ObjC.sel("endEncoding"));
    }
}
