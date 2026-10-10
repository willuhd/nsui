package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// One render-pass stencil target: texture + load/store actions + clear
/// stencil value. Pair it with a depth attachment when the pass uses a
/// combined depth/stencil pixel format (e.g. Depth32Float_Stencil8).
public final class MTLRenderPassStencilAttachmentDescriptor extends NSObject {

    /// Load actions (MTLLoadAction, MTLRenderPass.h).
    public static final long LOAD_DONT_CARE = 0;
    public static final long LOAD_LOAD = 1;
    public static final long LOAD_CLEAR = 2;

    /// Store actions (MTLStoreAction, MTLRenderPass.h).
    public static final long STORE_DONT_CARE = 0;
    public static final long STORE_STORE = 1;
    public static final long STORE_MULTISAMPLE_RESOLVE = 2;
    public static final long STORE_STORE_AND_MULTISAMPLE_RESOLVE = 3;

    private MTLRenderPassStencilAttachmentDescriptor(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLRenderPassStencilAttachmentDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLRenderPassStencilAttachmentDescriptor(peer);
    }

    /// texture.
    public MTLTexture texture() {
        return MTLTexture.wrap(ObjC.msgSendId(peer, ObjC.sel("texture")));
    }

    /// setTexture: — nil detaches.
    public void setTexture(MTLTexture texture) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTexture:"), ObjC.nullablePeer(texture));
    }

    /// loadAction.
    public long loadAction() {
        return ObjC.msgSendLong(peer, ObjC.sel("loadAction"));
    }

    /// setLoadAction:.
    public void setLoadAction(long action) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLoadAction:"), action);
    }

    /// storeAction.
    public long storeAction() {
        return ObjC.msgSendLong(peer, ObjC.sel("storeAction"));
    }

    /// setStoreAction:.
    public void setStoreAction(long action) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStoreAction:"), action);
    }

    /// clearStencil — uint32_t (32-bit unsigned).
    public long clearStencil() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT32));
            return ((int) h.invokeExact(peer, ObjC.sel("clearStencil"))) & 0xFFFFFFFFL;
        } catch (Throwable t) {
            throw new RuntimeException("clearStencil failed", t);
        }
    }

    /// setClearStencil: — uint32_t.
    public void setClearStencil(long value) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setClearStencil:"), value & 0xFFFFFFFFL);
    }
}
