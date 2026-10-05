package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAEmitterCell — one particle type in an emitter layer: birth rate,
/// lifetime, emission cone. Thin stateless wrapper (float properties use
/// float handles; CGFloat cone angles are doubles).
public final class CAEmitterCell extends NSObject {

    private record Handles(MethodHandle hGetFloat, MethodHandle hSetFloat,
            MethodHandle hGetDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    private CAEmitterCell(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAEmitterCell wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAEmitterCell(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
    }

    /// +emitterCell.
    public static CAEmitterCell create() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("CAEmitterCell"), ObjC.sel("emitterCell")));
    }

    private float getFloat(String selector) {
        try {
            return (float) handles.hGetFloat().invokeExact(peer, ObjC.sel(selector));
        } catch (Throwable t) {
            throw new RuntimeException(selector + " failed", t);
        }
    }

    private void setFloat(String selector, float value) {
        try {
            handles.hSetFloat().invokeExact(peer, ObjC.sel(selector), value);
        } catch (Throwable t) {
            throw new RuntimeException(selector + " failed", t);
        }
    }

    private double getDouble(String selector) {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel(selector));
        } catch (Throwable t) {
            throw new RuntimeException(selector + " failed", t);
        }
    }

    private void setDouble(String selector, double value) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel(selector), value);
        } catch (Throwable t) {
            throw new RuntimeException(selector + " failed", t);
        }
    }

    /// name (nil-safe).
    public String name() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("name")));
    }

    /// setName:.
    public void setName(String name) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setName:"),
                name == null ? MemorySegment.NULL : ObjC.nsstring(name));
    }

    /// isEnabled / setEnabled:.
    public boolean isEnabled() { return ObjC.msgSendBool(peer, ObjC.sel("isEnabled")); }
    /// setEnabled:.
    public void setEnabled(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setEnabled:"), flag); }
    /// birthRate particles/second.
    public float birthRate() { return getFloat("birthRate"); }
    /// setBirthRate:.
    public void setBirthRate(float v) { setFloat("setBirthRate:", v); }
    /// lifetime seconds.
    public float lifetime() { return getFloat("lifetime"); }
    /// setLifetime:.
    public void setLifetime(float v) { setFloat("setLifetime:", v); }
    /// lifetimeRange seconds.
    public float lifetimeRange() { return getFloat("lifetimeRange"); }
    /// setLifetimeRange:.
    public void setLifetimeRange(float v) { setFloat("setLifetimeRange:", v); }
    /// emissionLatitude radians.
    public double emissionLatitude() { return getDouble("emissionLatitude"); }
    /// setEmissionLatitude:.
    public void setEmissionLatitude(double v) { setDouble("setEmissionLatitude:", v); }
    /// emissionLongitude radians.
    public double emissionLongitude() { return getDouble("emissionLongitude"); }
    /// setEmissionLongitude:.
    public void setEmissionLongitude(double v) { setDouble("setEmissionLongitude:", v); }
    /// emissionRange radians.
    public double emissionRange() { return getDouble("emissionRange"); }
    /// setEmissionRange:.
    public void setEmissionRange(double v) { setDouble("setEmissionRange:", v); }
}
