package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAReplicatorLayer — replicates its sublayers N times with per-instance
/// offsets (color shifts here are floats; the 128-bit instanceTransform is
/// omitted — it needs a CATransform3D struct shape outside the vocabulary).
public class CAReplicatorLayer extends CALayer {
    private record Handles(MethodHandle hGetFloat, MethodHandle hSetFloat,
            MethodHandle hGetDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    protected CAReplicatorLayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAReplicatorLayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAReplicatorLayer(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(ObjC.handle(Sig.of(Ret.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
    }

    /// [[CAReplicatorLayer alloc] init].
    public static CAReplicatorLayer create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("CAReplicatorLayer"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for CAReplicatorLayer");
        return new CAReplicatorLayer(p);
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

    /// instanceCount.
    public long instanceCount() { return ObjC.msgSendLong(peer, ObjC.sel("instanceCount")); }
    /// setInstanceCount:.
    public void setInstanceCount(long n) { ObjC.msgSendVoidLong(peer, ObjC.sel("setInstanceCount:"), n); }
    /// preservesDepth.
    public boolean preservesDepth() { return ObjC.msgSendBool(peer, ObjC.sel("preservesDepth")); }
    /// setPreservesDepth:.
    public void setPreservesDepth(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setPreservesDepth:"), flag); }
    /// instanceDelay (seconds between copies).
    public double instanceDelay() {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("instanceDelay"));
        } catch (Throwable t) {
            throw new RuntimeException("instanceDelay failed", t);
        }
    }

    /// setInstanceDelay:.
    public void setInstanceDelay(double seconds) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setInstanceDelay:"), seconds);
        } catch (Throwable t) {
            throw new RuntimeException("setInstanceDelay: failed", t);
        }
    }
    /// instanceRedOffset / Green / Blue / Alpha (float color shifts per copy).
    public float instanceRedOffset() { return getFloat("instanceRedOffset"); }
    /// setInstanceRedOffset:.
    public void setInstanceRedOffset(float v) { setFloat("setInstanceRedOffset:", v); }
    /// instanceGreenOffset.
    public float instanceGreenOffset() { return getFloat("instanceGreenOffset"); }
    /// setInstanceGreenOffset:.
    public void setInstanceGreenOffset(float v) { setFloat("setInstanceGreenOffset:", v); }
    /// instanceBlueOffset.
    public float instanceBlueOffset() { return getFloat("instanceBlueOffset"); }
    /// setInstanceBlueOffset:.
    public void setInstanceBlueOffset(float v) { setFloat("setInstanceBlueOffset:", v); }
    /// instanceAlphaOffset.
    public float instanceAlphaOffset() { return getFloat("instanceAlphaOffset"); }
    /// setInstanceAlphaOffset:.
    public void setInstanceAlphaOffset(float v) { setFloat("setInstanceAlphaOffset:", v); }
    /// instanceColor (raw CGColorRef peer).
    public MemorySegment instanceColor() { return ObjC.msgSendId(peer, ObjC.sel("instanceColor")); }
    /// setInstanceColor:.
    public void setInstanceColor(MemorySegment color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setInstanceColor:"),
                (MemorySegment) (color == null ? MemorySegment.NULL : color));
    }
}
