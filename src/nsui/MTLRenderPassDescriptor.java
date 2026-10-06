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
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.INT)));
    }

    /// +renderPassDescriptor (autoreleased: use within a pool per frame).
    public static MTLRenderPassDescriptor create() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("MTLRenderPassDescriptor"),
                ObjC.sel("renderPassDescriptor")));
    }

    /// colorAttachments[i] via objectAtIndexedSubscript:.
    public MTLRenderPassColorAttachmentDescriptor colorAttachment(long index) {
        ensureInit();
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
