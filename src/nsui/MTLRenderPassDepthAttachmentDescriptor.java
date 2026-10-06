package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// One render-pass depth target: texture + load/store actions + clear depth,
/// plus mip level, array slice and the multisample resolve texture (all four
/// inherited from MTLRenderPassAttachmentDescriptor, modelled here directly
/// because the color attachment wrapper omits them).
public final class MTLRenderPassDepthAttachmentDescriptor extends NSObject {

    /// Load actions (MTLLoadAction, MTLRenderPass.h).
    public static final long LOAD_DONT_CARE = 0;
    public static final long LOAD_LOAD = 1;
    public static final long LOAD_CLEAR = 2;

    /// Store actions (MTLStoreAction, MTLRenderPass.h).
    public static final long STORE_DONT_CARE = 0;
    public static final long STORE_STORE = 1;
    public static final long STORE_MULTISAMPLE_RESOLVE = 2;
    public static final long STORE_STORE_AND_MULTISAMPLE_RESOLVE = 3;

    private record Handles(MethodHandle hGetClearDepth, MethodHandle hSetClearDepth) {}
    private static volatile Handles handles;

    private MTLRenderPassDepthAttachmentDescriptor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLRenderPassDepthAttachmentDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLRenderPassDepthAttachmentDescriptor(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.DOUBLE)), ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
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

    /// clearDepth.
    public double clearDepth() {
        ensureInit();
        try {
            return (double) handles.hGetClearDepth().invokeExact(peer, ObjC.sel("clearDepth"));
        } catch (Throwable t) {
            throw new RuntimeException("clearDepth failed", t);
        }
    }

    /// setClearDepth: (double by value in an FP register).
    public void setClearDepth(double depth) {
        ensureInit();
        try {
            handles.hSetClearDepth().invokeExact(peer, ObjC.sel("setClearDepth:"), depth);
        } catch (Throwable t) {
            throw new RuntimeException("setClearDepth: failed", t);
        }
    }

    /// level (mipmap level; NSUInteger).
    public long level() {
        return ObjC.msgSendLong(peer, ObjC.sel("level"));
    }

    /// setLevel:.
    public void setLevel(long level) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLevel:"), level);
    }

    /// slice (array slice; NSUInteger).
    public long slice() {
        return ObjC.msgSendLong(peer, ObjC.sel("slice"));
    }

    /// setSlice:.
    public void setSlice(long slice) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSlice:"), slice);
    }

    /// resolveTexture (multisample resolve target; nil when unused).
    public MTLTexture resolveTexture() {
        return MTLTexture.wrap(ObjC.msgSendId(peer, ObjC.sel("resolveTexture")));
    }

    /// setResolveTexture: — nil clears it.
    public void setResolveTexture(MTLTexture texture) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setResolveTexture:"), ObjC.nullablePeer(texture));
    }
}
