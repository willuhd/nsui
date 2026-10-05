package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Scratch;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAMediaTimingFunction — easing curves: named presets or cubic Bezier
/// control points. Thin stateless wrapper.
public final class CAMediaTimingFunction extends NSObject {

    /// Named presets.
    public static final String NAME_LINEAR = "linear";
    public static final String NAME_DEFAULT = "default";
    public static final String NAME_EASE_IN = "easeIn";
    public static final String NAME_EASE_OUT = "easeOut";
    public static final String NAME_EASE_IN_EASE_OUT = "easeInEaseOut";

    private record Handles(MethodHandle hControlPoints, MethodHandle hGetPoint) {}
    private static volatile Handles handles;

    private CAMediaTimingFunction(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAMediaTimingFunction wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAMediaTimingFunction(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.FLOAT, Arg.FLOAT, Arg.FLOAT, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.ID)));
    }

    /// functionWithName:.
    public static CAMediaTimingFunction functionWithName(String name) {
        ensureInit();
        return wrap(ObjC.msgSendIdId(ObjC.cls("CAMediaTimingFunction"),
                ObjC.sel("functionWithName:"), ObjC.nsstring(name)));
    }

    /// functionWithControlPoints:::: — cubic Bezier easing.
    public static CAMediaTimingFunction functionWithControlPoints(float c1x, float c1y, float c2x, float c2y) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hControlPoints().invokeExact(
                    ObjC.cls("CAMediaTimingFunction"), ObjC.sel("functionWithControlPoints::::"),
                    c1x, c1y, c2x, c2y));
        } catch (Throwable t) {
            throw new RuntimeException("functionWithControlPoints:::: failed", t);
        }
    }

    /// getControlPointAtIndex:values: — writes 2 floats into {x, y}.
    public float[] controlPointAtIndex(long index) {
        MemorySegment out = Scratch.allocInput(8);
        try {
            handles.hGetPoint().invokeExact(peer, ObjC.sel("getControlPointAtIndex:values:"), index, out);
        } catch (Throwable t) {
            throw new RuntimeException("getControlPointAtIndex:values: failed", t);
        }
        return new float[] {
            out.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, 0),
            out.get(java.lang.foreign.ValueLayout.JAVA_FLOAT, 4) };
    }
}
