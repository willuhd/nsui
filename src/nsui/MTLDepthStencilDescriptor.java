package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLDepthStencilDescriptor — the CPU-side recipe for a compiled
/// MTLDepthStencilState: depth compare + write enable, and one
/// MTLStencilDescriptor per face. Authored per pipeline, then compiled once.
public final class MTLDepthStencilDescriptor extends NSObject {

    /// MTLCompareFunction — verified against MTLDepthStencil.h.
    public static final long COMPARE_NEVER = MTLStencilDescriptor.COMPARE_NEVER;
    public static final long COMPARE_LESS = MTLStencilDescriptor.COMPARE_LESS;
    public static final long COMPARE_EQUAL = MTLStencilDescriptor.COMPARE_EQUAL;
    public static final long COMPARE_LESS_EQUAL = MTLStencilDescriptor.COMPARE_LESS_EQUAL;
    public static final long COMPARE_GREATER = MTLStencilDescriptor.COMPARE_GREATER;
    public static final long COMPARE_NOT_EQUAL = MTLStencilDescriptor.COMPARE_NOT_EQUAL;
    public static final long COMPARE_GREATER_EQUAL = MTLStencilDescriptor.COMPARE_GREATER_EQUAL;
    public static final long COMPARE_ALWAYS = MTLStencilDescriptor.COMPARE_ALWAYS;

    private record Handles(MethodHandle hIsWrite) {}
    private static volatile Handles handles;

    private MTLDepthStencilDescriptor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLDepthStencilDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLDepthStencilDescriptor(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.BOOL)));
    }

    /// [[MTLDepthStencilDescriptor alloc] init].
    public static MTLDepthStencilDescriptor create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("MTLDepthStencilDescriptor"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for MTLDepthStencilDescriptor");
        return new MTLDepthStencilDescriptor(p);
    }

    /// depthCompareFunction.
    public long depthCompareFunction() {
        return ObjC.msgSendLong(peer, ObjC.sel("depthCompareFunction"));
    }

    /// setDepthCompareFunction:.
    public void setDepthCompareFunction(long function) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDepthCompareFunction:"), function);
    }

    /// isDepthWriteEnabled — the getter the header declares (getter=isDepthWriteEnabled).
    public boolean isDepthWriteEnabled() {
        ensureInit();
        try {
            return (boolean) handles.hIsWrite().invokeExact(peer, ObjC.sel("isDepthWriteEnabled"));
        } catch (Throwable t) {
            throw new RuntimeException("isDepthWriteEnabled failed", t);
        }
    }

    /// setDepthWriteEnabled:.
    public void setDepthWriteEnabled(boolean enabled) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDepthWriteEnabled:"), enabled);
    }

    /// frontFaceStencil (non-nil; Metal resets it to a default descriptor).
    public MTLStencilDescriptor frontFaceStencil() {
        return MTLStencilDescriptor.wrap(ObjC.msgSendId(peer, ObjC.sel("frontFaceStencil")));
    }

    /// setFrontFaceStencil: — NULL resets the face to the default state.
    public void setFrontFaceStencil(MTLStencilDescriptor stencil) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFrontFaceStencil:"), ObjC.nullablePeer(stencil));
    }

    /// backFaceStencil (non-nil).
    public MTLStencilDescriptor backFaceStencil() {
        return MTLStencilDescriptor.wrap(ObjC.msgSendId(peer, ObjC.sel("backFaceStencil")));
    }

    /// setBackFaceStencil: — NULL resets the face to the default state.
    public void setBackFaceStencil(MTLStencilDescriptor stencil) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBackFaceStencil:"), ObjC.nullablePeer(stencil));
    }

    /// label.
    public String label() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("label")));
    }

    /// setLabel:.
    public void setLabel(String label) {
        if (label == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("setLabel:"), ObjC.nsstring(label));
    }
}
