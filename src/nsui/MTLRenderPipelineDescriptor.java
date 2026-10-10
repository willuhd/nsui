package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLRenderPipelineDescriptor — vertex/fragment functions + pixel format.
/// Built once per pipeline (long-lived).
public final class MTLRenderPipelineDescriptor extends NSObject {

    private record Handles(MethodHandle hAt) {}
    private static volatile Handles handles;

    private MTLRenderPipelineDescriptor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLRenderPipelineDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLRenderPipelineDescriptor(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("Metal"); } catch (Throwable ignored) {}
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.INT)));
    }

    /// [[MTLRenderPipelineDescriptor alloc] init].
    public static MTLRenderPipelineDescriptor create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("MTLRenderPipelineDescriptor"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for MTLRenderPipelineDescriptor");
        return new MTLRenderPipelineDescriptor(p);
    }

    /// setVertexFunction:.
    public void setVertexFunction(MTLFunction fn) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setVertexFunction:"),
                (MemorySegment) (fn == null ? MemorySegment.NULL : fn.peer()));
    }

    /// setFragmentFunction:.
    public void setFragmentFunction(MTLFunction fn) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFragmentFunction:"),
                (MemorySegment) (fn == null ? MemorySegment.NULL : fn.peer()));
    }

    /// depthAttachmentPixelFormat (MTLPixelFormat; 0 == invalid).
    public long depthAttachmentPixelFormat() {
        return ObjC.msgSendLong(peer, ObjC.sel("depthAttachmentPixelFormat"));
    }

    /// setDepthAttachmentPixelFormat: — must match the pass's depth texture.
    public void setDepthAttachmentPixelFormat(long format) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDepthAttachmentPixelFormat:"), format);
    }

    /// stencilAttachmentPixelFormat (MTLPixelFormat; 0 == invalid).
    public long stencilAttachmentPixelFormat() {
        return ObjC.msgSendLong(peer, ObjC.sel("stencilAttachmentPixelFormat"));
    }

    /// setStencilAttachmentPixelFormat: — must match the pass's stencil texture.
    public void setStencilAttachmentPixelFormat(long format) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStencilAttachmentPixelFormat:"), format);
    }

    /// colorAttachments[i] (v1 uses 0).
    /// Negative indices fail fast in Java. There is no upper-bound check:
    /// the attachment array exposes no count, and an out-of-range index
    /// raises natively (uncatchable), so callers must stay in range.
    public MTLRenderPipelineColorAttachmentDescriptor colorAttachment(long index) {
        ensureInit();
        if (index < 0)
            throw new IllegalArgumentException("colorAttachment: index " + index + " out of bounds (negative)");
        try {
            MemorySegment arr = ObjC.msgSendId(peer, ObjC.sel("colorAttachments"));
            if (arr.address() == 0) return null;
            return MTLRenderPipelineColorAttachmentDescriptor.wrap((MemorySegment) handles.hAt().invokeExact(
                    arr, ObjC.sel("objectAtIndexedSubscript:"), index));
        } catch (Throwable t) {
            throw new RuntimeException("colorAttachment failed", t);
        }
    }
}
