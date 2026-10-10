package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// MTLStencilDescriptor — one face's stencil test state: compare function,
/// the three stencil operations, and the read/write masks. Authored on the
/// CPU, consumed by MTLDepthStencilDescriptor (never a render target itself).
public final class MTLStencilDescriptor extends NSObject {

    /// MTLCompareFunction — verified against MTLDepthStencil.h.
    public static final long COMPARE_NEVER = 0;
    public static final long COMPARE_LESS = 1;
    public static final long COMPARE_EQUAL = 2;
    public static final long COMPARE_LESS_EQUAL = 3;
    public static final long COMPARE_GREATER = 4;
    public static final long COMPARE_NOT_EQUAL = 5;
    public static final long COMPARE_GREATER_EQUAL = 6;
    public static final long COMPARE_ALWAYS = 7;

    /// MTLStencilOperation — verified against MTLDepthStencil.h.
    public static final long OPERATION_KEEP = 0;
    public static final long OPERATION_ZERO = 1;
    public static final long OPERATION_REPLACE = 2;
    public static final long OPERATION_INCREMENT_CLAMP = 3;
    public static final long OPERATION_DECREMENT_CLAMP = 4;
    public static final long OPERATION_INVERT = 5;
    public static final long OPERATION_INCREMENT_WRAP = 6;
    public static final long OPERATION_DECREMENT_WRAP = 7;

    private MTLStencilDescriptor(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLStencilDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLStencilDescriptor(peer);
    }

    /// [[MTLStencilDescriptor alloc] init].
    public static MTLStencilDescriptor create() {
        MemorySegment p = ObjC.msgSendId(ObjC.cls("MTLStencilDescriptor"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for MTLStencilDescriptor");
        return new MTLStencilDescriptor(p);
    }

    /// stencilCompareFunction.
    public long stencilCompareFunction() {
        return ObjC.msgSendLong(peer, ObjC.sel("stencilCompareFunction"));
    }

    /// setStencilCompareFunction:.
    public void setStencilCompareFunction(long function) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStencilCompareFunction:"), function);
    }

    /// stencilFailureOperation.
    public long stencilFailureOperation() {
        return ObjC.msgSendLong(peer, ObjC.sel("stencilFailureOperation"));
    }

    /// setStencilFailureOperation:.
    public void setStencilFailureOperation(long operation) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStencilFailureOperation:"), operation);
    }

    /// depthFailureOperation.
    public long depthFailureOperation() {
        return ObjC.msgSendLong(peer, ObjC.sel("depthFailureOperation"));
    }

    /// setDepthFailureOperation:.
    public void setDepthFailureOperation(long operation) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDepthFailureOperation:"), operation);
    }

    /// depthStencilPassOperation.
    public long depthStencilPassOperation() {
        return ObjC.msgSendLong(peer, ObjC.sel("depthStencilPassOperation"));
    }

    /// setDepthStencilPassOperation:.
    public void setDepthStencilPassOperation(long operation) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDepthStencilPassOperation:"), operation);
    }

    /// readMask — uint32_t (32-bit unsigned).
    public long readMask() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT32));
            return ((int) h.invokeExact(peer, ObjC.sel("readMask"))) & 0xFFFFFFFFL;
        } catch (Throwable t) {
            throw new RuntimeException("readMask failed", t);
        }
    }

    /// setReadMask: — uint32_t.
    public void setReadMask(long mask) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setReadMask:"), mask & 0xFFFFFFFFL);
    }

    /// writeMask — uint32_t (32-bit unsigned).
    public long writeMask() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT32));
            return ((int) h.invokeExact(peer, ObjC.sel("writeMask"))) & 0xFFFFFFFFL;
        } catch (Throwable t) {
            throw new RuntimeException("writeMask failed", t);
        }
    }

    /// setWriteMask: — uint32_t.
    public void setWriteMask(long mask) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setWriteMask:"), mask & 0xFFFFFFFFL);
    }
}
