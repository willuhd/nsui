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

    private static synchronized void ensureInit() {
        if (handles != null) return;
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

    /// colorAttachments[i] (v1 uses 0).
    public MTLRenderPipelineColorAttachmentDescriptor colorAttachment(long index) {
        ensureInit();
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
