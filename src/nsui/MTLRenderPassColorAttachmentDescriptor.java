package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// One render-pass color target: texture + load/store actions + clear color.
public final class MTLRenderPassColorAttachmentDescriptor extends NSObject {

    /// Load actions.
    public static final long LOAD_DONT_CARE = 0;
    public static final long LOAD_LOAD = 1;
    public static final long LOAD_CLEAR = 2;

    /// Store actions.
    public static final long STORE_DONT_CARE = 0;
    public static final long STORE_STORE = 1;

    private record Handles(MethodHandle hSetClear) {}
    private static volatile Handles handles;

    private MTLRenderPassColorAttachmentDescriptor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)));
    }

    /// Wrap an existing peer.
    public static MTLRenderPassColorAttachmentDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLRenderPassColorAttachmentDescriptor(peer);
    }

    /// setTexture:.
    public void setTexture(MTLTexture texture) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTexture:"),
                (MemorySegment) (texture == null ? MemorySegment.NULL : texture.peer()));
    }

    /// setLoadAction:.
    public void setLoadAction(long action) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLoadAction:"), action);
    }

    /// setStoreAction:.
    public void setStoreAction(long action) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStoreAction:"), action);
    }

    /// setClearColor: (32-byte struct by value).
    public void setClearColor(MTLClearColor color) {
        if (color == null) return;
        try {
            handles.hSetClear().invokeExact(
                    peer, ObjC.sel("setClearColor:"), color.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setClearColor: failed", t);
        }
    }
}
