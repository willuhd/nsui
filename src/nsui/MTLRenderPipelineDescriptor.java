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

    /// Maximum color attachment slots (8, the value the API validates against). There is no
    /// SDK constant named MTLMaxColorAttachments; the number is measured from the native assertion.
    public static final long MAX_COLOR_ATTACHMENTS = 8;

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
    /// Rejects negative indices and indices >= MTLMaxColorAttachments (8) in Java;
    /// the attachment array exposes no count, and an out-of-range index raises
    /// natively (uncatchable).
    public MTLRenderPipelineColorAttachmentDescriptor colorAttachment(long index) {
        ensureInit();
        if (index < 0 || index >= MAX_COLOR_ATTACHMENTS)
            throw new IllegalArgumentException("colorAttachment: index " + index
                    + " out of bounds (0.." + (MAX_COLOR_ATTACHMENTS - 1) + ")");
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
