package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAEmitterLayer — GPU particle emitter: cells, rates, emitter geometry.
/// Thin stateless wrapper.
public class CAEmitterLayer extends CALayer {

    /// Emitter shapes.
    public static final String SHAPE_POINT = "point";
    public static final String SHAPE_LINE = "line";
    public static final String SHAPE_RECTANGLE = "rectangle";
    public static final String SHAPE_CIRCLE = "circle";
    public static final String SHAPE_CUBOID = "cuboid";
    public static final String SHAPE_BOX = "box";
    public static final String SHAPE_SPHERE = "sphere";
    public static final String SHAPE_CYLINDER = "cylinder";

    /// Emitter modes.
    public static final String MODE_POINTS = "points";
    public static final String MODE_OUTLINE = "outline";
    public static final String MODE_SURFACE = "surface";
    public static final String MODE_VOLUME = "volume";

    /// Render modes.
    public static final String RENDER_UNORDERED = "unordered";
    public static final String RENDER_OLDEST_FIRST = "oldestFirst";
    public static final String RENDER_OLDEST_LAST = "oldestLast";
    public static final String RENDER_BACK_TO_FRONT = "backToFront";
    public static final String RENDER_ADDITIVE = "additive";

    private record Handles(MethodHandle hGetFloat, MethodHandle hSetFloat,
            MethodHandle hGetDouble, MethodHandle hSetDouble,
            MethodHandle hGetPoint, MethodHandle hSetPoint,
            MethodHandle hGetSize, MethodHandle hSetSize) {}
    private static volatile Handles handles;

    protected CAEmitterLayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAEmitterLayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAEmitterLayer(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)));
    }

    /// [[CAEmitterLayer alloc] init].
    public static CAEmitterLayer create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("CAEmitterLayer"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for CAEmitterLayer");
        return new CAEmitterLayer(p);
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

    /// emitterCells.
    public NSArray emitterCells() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("emitterCells")));
    }

    /// setEmitterCells:.
    public void setEmitterCells(NSArray cells) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setEmitterCells:"),
                (MemorySegment) (cells == null ? MemorySegment.NULL : cells.peer()));
    }

    /// birthRate / setBirthRate:.
    public float birthRate() { return getFloat("birthRate"); }
    /// setBirthRate:.
    public void setBirthRate(float v) { setFloat("setBirthRate:", v); }
    /// lifetime / setLifetime:.
    public float lifetime() { return getFloat("lifetime"); }
    /// setLifetime:.
    public void setLifetime(float v) { setFloat("setLifetime:", v); }
    /// emitterPosition / setEmitterPosition:.
    public NSPoint emitterPosition() {
        try {
            return NSPoint.fromSegment((MemorySegment) handles.hGetPoint().invokeExact(ObjC.structSlot(), peer, ObjC.sel("emitterPosition")));
        } catch (Throwable t) {
            throw new RuntimeException("emitterPosition failed", t);
        }
    }

    /// setEmitterPosition:.
    public void setEmitterPosition(NSPoint p) {
        try {
            handles.hSetPoint().invokeExact(peer, ObjC.sel("setEmitterPosition:"), p.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setEmitterPosition: failed", t);
        }
    }

    /// emitterZPosition / setEmitterZPosition:.
    public double emitterZPosition() { return getDouble("emitterZPosition"); }
    /// setEmitterZPosition:.
    public void setEmitterZPosition(double v) { setDouble("setEmitterZPosition:", v); }
    /// emitterSize / setEmitterSize:.
    public NSSize emitterSize() {
        try {
            return NSSize.fromSegment((MemorySegment) handles.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("emitterSize")));
        } catch (Throwable t) {
            throw new RuntimeException("emitterSize failed", t);
        }
    }

    /// setEmitterSize:.
    public void setEmitterSize(NSSize s) {
        try {
            handles.hSetSize().invokeExact(peer, ObjC.sel("setEmitterSize:"), s.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setEmitterSize: failed", t);
        }
    }

    /// emitterDepth / setEmitterDepth:.
    public double emitterDepth() { return getDouble("emitterDepth"); }
    /// setEmitterDepth:.
    public void setEmitterDepth(double v) { setDouble("setEmitterDepth:", v); }

    private String getString(String selector) {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel(selector)));
    }

    private void setString(String selector, String value) {
        ObjC.msgSendVoidId(peer, ObjC.sel(selector), ObjC.nsstring(value));
    }

    /// emitterShape / setEmitterShape:.
    public String emitterShape() { return getString("emitterShape"); }
    /// setEmitterShape:.
    public void setEmitterShape(String shape) { setString("setEmitterShape:", shape); }
    /// emitterMode / setEmitterMode:.
    public String emitterMode() { return getString("emitterMode"); }
    /// setEmitterMode:.
    public void setEmitterMode(String mode) { setString("setEmitterMode:", mode); }
    /// renderMode / setRenderMode:.
    public String renderMode() { return getString("renderMode"); }
    /// setRenderMode:.
    public void setRenderMode(String mode) { setString("setRenderMode:", mode); }
    /// preservesDepth / setPreservesDepth:.
    public boolean preservesDepth() { return ObjC.msgSendBool(peer, ObjC.sel("preservesDepth")); }
    /// setPreservesDepth:.
    public void setPreservesDepth(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setPreservesDepth:"), flag); }
}
