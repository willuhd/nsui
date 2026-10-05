package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAConstraint — one CoreAnimation layout relation (attribute pinned relative
/// to a named sibling layer, scaled and offset). Thin stateless wrapper.
public final class CAConstraint extends NSObject {

    /// Constraint attributes (kCAConstraintMinX..Height, in order).
    public static final long ATTR_MIN_X = 0;
    public static final long ATTR_MID_X = 1;
    public static final long ATTR_MAX_X = 2;
    public static final long ATTR_WIDTH = 3;
    public static final long ATTR_MIN_Y = 4;
    public static final long ATTR_MID_Y = 5;
    public static final long ATTR_MAX_Y = 6;
    public static final long ATTR_HEIGHT = 7;

    private record Handles(MethodHandle hFull, MethodHandle hOffset, MethodHandle hPlain,
            MethodHandle hGetDouble) {}
    private static volatile Handles handles;

    private CAConstraint(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAConstraint wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAConstraint(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID, Arg.INT, Arg.DOUBLE, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID, Arg.INT, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)));
    }

    /// constraintWithAttribute:relativeTo:attribute:scale:offset:.
    public static CAConstraint relativeTo(long attr, String sourceId, long sourceAttr, double scale, double offset) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hFull().invokeExact(ObjC.cls("CAConstraint"),
                    ObjC.sel("constraintWithAttribute:relativeTo:attribute:scale:offset:"),
                    attr, ObjC.nsstring(sourceId), sourceAttr, scale, offset));
        } catch (Throwable t) {
            throw new RuntimeException("constraintWithAttribute:... failed", t);
        }
    }

    /// constraintWithAttribute:relativeTo:attribute:offset:.
    public static CAConstraint relativeTo(long attr, String sourceId, long sourceAttr, double offset) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hOffset().invokeExact(ObjC.cls("CAConstraint"),
                    ObjC.sel("constraintWithAttribute:relativeTo:attribute:offset:"),
                    attr, ObjC.nsstring(sourceId), sourceAttr, offset));
        } catch (Throwable t) {
            throw new RuntimeException("constraintWithAttribute:... failed", t);
        }
    }

    /// constraintWithAttribute:relativeTo:attribute:.
    public static CAConstraint relativeTo(long attr, String sourceId, long sourceAttr) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hPlain().invokeExact(ObjC.cls("CAConstraint"),
                    ObjC.sel("constraintWithAttribute:relativeTo:attribute:"),
                    attr, ObjC.nsstring(sourceId), sourceAttr));
        } catch (Throwable t) {
            throw new RuntimeException("constraintWithAttribute:... failed", t);
        }
    }

    /// attribute / sourceName / sourceAttribute / scale / offset (readonly).
    public long attribute() { return ObjC.msgSendLong(peer, ObjC.sel("attribute")); }
    /// sourceName (nil-safe).
    public String sourceName() { return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("sourceName"))); }
    /// sourceAttribute.
    public long sourceAttribute() { return ObjC.msgSendLong(peer, ObjC.sel("sourceAttribute")); }
    private double getDouble(String selector) {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel(selector));
        } catch (Throwable t) {
            throw new RuntimeException(selector + " failed", t);
        }
    }

    /// scale.
    public double scale() { return getDouble("scale"); }

    /// offset.
    public double offset() { return getDouble("offset"); }
}
