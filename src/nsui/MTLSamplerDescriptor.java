package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLSamplerDescriptor — the CPU-side recipe for a compiled MTLSamplerState:
/// min/mag/mip filtering, per-axis address modes, LOD clamps and the compare
/// function. Authored per pipeline, then compiled once by the device.
public final class MTLSamplerDescriptor extends NSObject {

    /// MTLSamplerMinMagFilter — verified against MTLSampler.h.
    public static final long MIN_MAG_FILTER_NEAREST = 0;
    public static final long MIN_MAG_FILTER_LINEAR = 1;

    /// MTLSamplerMipFilter — verified against MTLSampler.h.
    public static final long MIP_FILTER_NOT_MIPMAPPED = 0;
    public static final long MIP_FILTER_NEAREST = 1;
    public static final long MIP_FILTER_LINEAR = 2;

    /// MTLSamplerAddressMode — verified against MTLSampler.h.
    public static final long ADDRESS_MODE_CLAMP_TO_EDGE = 0;
    public static final long ADDRESS_MODE_MIRROR_CLAMP_TO_EDGE = 1;
    public static final long ADDRESS_MODE_REPEAT = 2;
    public static final long ADDRESS_MODE_MIRROR_REPEAT = 3;
    public static final long ADDRESS_MODE_CLAMP_TO_ZERO = 4;
    public static final long ADDRESS_MODE_CLAMP_TO_BORDER_COLOR = 5;

    /// MTLSamplerBorderColor — verified against MTLSampler.h.
    public static final long BORDER_COLOR_TRANSPARENT_BLACK = 0;
    public static final long BORDER_COLOR_OPAQUE_BLACK = 1;
    public static final long BORDER_COLOR_OPAQUE_WHITE = 2;

    /// MTLCompareFunction — verified against MTLDepthStencil.h (same enum the
    /// sampler's compareFunction property uses).
    public static final long COMPARE_NEVER = 0;
    public static final long COMPARE_LESS = 1;
    public static final long COMPARE_EQUAL = 2;
    public static final long COMPARE_LESS_EQUAL = 3;
    public static final long COMPARE_GREATER = 4;
    public static final long COMPARE_NOT_EQUAL = 5;
    public static final long COMPARE_GREATER_EQUAL = 6;
    public static final long COMPARE_ALWAYS = 7;

    private record Handles(MethodHandle hGetFloat, MethodHandle hSetFloat) {}
    private static volatile Handles handles;

    private MTLSamplerDescriptor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLSamplerDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLSamplerDescriptor(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT)));
        handles = h;
    }

    /// [[MTLSamplerDescriptor alloc] init].
    public static MTLSamplerDescriptor create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("MTLSamplerDescriptor"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for MTLSamplerDescriptor");
        return new MTLSamplerDescriptor(p);
    }

    /// minFilter.
    public long minFilter() {
        return ObjC.msgSendLong(peer, ObjC.sel("minFilter"));
    }

    /// setMinFilter:.
    public void setMinFilter(long filter) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setMinFilter:"), filter);
    }

    /// magFilter.
    public long magFilter() {
        return ObjC.msgSendLong(peer, ObjC.sel("magFilter"));
    }

    /// setMagFilter:.
    public void setMagFilter(long filter) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setMagFilter:"), filter);
    }

    /// mipFilter.
    public long mipFilter() {
        return ObjC.msgSendLong(peer, ObjC.sel("mipFilter"));
    }

    /// setMipFilter:.
    public void setMipFilter(long filter) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setMipFilter:"), filter);
    }

    /// maxAnisotropy.
    public long maxAnisotropy() {
        return ObjC.msgSendLong(peer, ObjC.sel("maxAnisotropy"));
    }

    /// setMaxAnisotropy:.
    public void setMaxAnisotropy(long anisotropy) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setMaxAnisotropy:"), anisotropy);
    }

    /// sAddressMode.
    public long sAddressMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("sAddressMode"));
    }

    /// setSAddressMode:.
    public void setSAddressMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSAddressMode:"), mode);
    }

    /// tAddressMode.
    public long tAddressMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("tAddressMode"));
    }

    /// setTAddressMode:.
    public void setTAddressMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTAddressMode:"), mode);
    }

    /// rAddressMode.
    public long rAddressMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("rAddressMode"));
    }

    /// setRAddressMode:.
    public void setRAddressMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setRAddressMode:"), mode);
    }

    /// borderColor.
    public long borderColor() {
        return ObjC.msgSendLong(peer, ObjC.sel("borderColor"));
    }

    /// setBorderColor:.
    public void setBorderColor(long color) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setBorderColor:"), color);
    }

    /// compareFunction.
    public long compareFunction() {
        return ObjC.msgSendLong(peer, ObjC.sel("compareFunction"));
    }

    /// setCompareFunction:.
    public void setCompareFunction(long function) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setCompareFunction:"), function);
    }

    /// normalizedCoordinates.
    public boolean normalizedCoordinates() {
        return ObjC.msgSendBool(peer, ObjC.sel("normalizedCoordinates"));
    }

    /// setNormalizedCoordinates:.
    public void setNormalizedCoordinates(boolean normalized) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setNormalizedCoordinates:"), normalized);
    }

    /// lodMinClamp — float in the SDK.
    public float lodMinClamp() {
        ensureInit();
        try {
            return (float) handles.hGetFloat().invokeExact(peer, ObjC.sel("lodMinClamp"));
        } catch (Throwable t) {
            throw new RuntimeException("lodMinClamp failed", t);
        }
    }

    /// setLodMinClamp:.
    public void setLodMinClamp(float clamp) {
        ensureInit();
        try {
            handles.hSetFloat().invokeExact(peer, ObjC.sel("setLodMinClamp:"), clamp);
        } catch (Throwable t) {
            throw new RuntimeException("setLodMinClamp: failed", t);
        }
    }

    /// lodMaxClamp — float in the SDK.
    public float lodMaxClamp() {
        ensureInit();
        try {
            return (float) handles.hGetFloat().invokeExact(peer, ObjC.sel("lodMaxClamp"));
        } catch (Throwable t) {
            throw new RuntimeException("lodMaxClamp failed", t);
        }
    }

    /// setLodMaxClamp:.
    public void setLodMaxClamp(float clamp) {
        ensureInit();
        try {
            handles.hSetFloat().invokeExact(peer, ObjC.sel("setLodMaxClamp:"), clamp);
        } catch (Throwable t) {
            throw new RuntimeException("setLodMaxClamp: failed", t);
        }
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
