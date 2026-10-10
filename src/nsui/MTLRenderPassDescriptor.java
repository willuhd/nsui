package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLRenderPassDescriptor — what a render encoder draws into: color
/// attachments (texture + load/store + clear). Created per frame (cheap,
/// autoreleased: drain via pool per frame).
public final class MTLRenderPassDescriptor extends NSObject {

    private record Handles(MethodHandle hAt) {}
    private static volatile Handles handles;

    private MTLRenderPassDescriptor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLRenderPassDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLRenderPassDescriptor(peer);
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

    /// +renderPassDescriptor (autoreleased: use within a pool per frame).
    public static MTLRenderPassDescriptor create() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("MTLRenderPassDescriptor"),
                ObjC.sel("renderPassDescriptor")));
    }

    /// depthAttachment — never nil (the property is null_resettable and
    /// Metal returns a default descriptor); wrap it to configure a depth target.
    public MTLRenderPassDepthAttachmentDescriptor depthAttachment() {
        return MTLRenderPassDepthAttachmentDescriptor.wrap(ObjC.msgSendId(peer, ObjC.sel("depthAttachment")));
    }

    /// setDepthAttachment: (nil resets to the default descriptor).
    public void setDepthAttachment(MTLRenderPassDepthAttachmentDescriptor attachment) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDepthAttachment:"), ObjC.nullablePeer(attachment));
    }

    /// stencilAttachment — never nil (see depthAttachment).
    public MTLRenderPassStencilAttachmentDescriptor stencilAttachment() {
        return MTLRenderPassStencilAttachmentDescriptor.wrap(ObjC.msgSendId(peer, ObjC.sel("stencilAttachment")));
    }

    /// setStencilAttachment: (nil resets to the default descriptor).
    public void setStencilAttachment(MTLRenderPassStencilAttachmentDescriptor attachment) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setStencilAttachment:"), ObjC.nullablePeer(attachment));
    }

    /// colorAttachments[i] via objectAtIndexedSubscript:.
    /// Negative indices fail fast in Java. There is no upper-bound check:
    /// the attachment array exposes no count, and an out-of-range index
    /// raises natively (uncatchable), so callers must stay in range.
    public MTLRenderPassColorAttachmentDescriptor colorAttachment(long index) {
        ensureInit();
        if (index < 0)
            throw new IllegalArgumentException("colorAttachment: index " + index + " out of bounds (negative)");
        try {
            MemorySegment arr = ObjC.msgSendId(peer, ObjC.sel("colorAttachments"));
            if (arr.address() == 0) return null;
            return MTLRenderPassColorAttachmentDescriptor.wrap((MemorySegment) handles.hAt().invokeExact(
                    arr, ObjC.sel("objectAtIndexedSubscript:"), index));
        } catch (Throwable t) {
            throw new RuntimeException("colorAttachment failed", t);
        }
    }
}
