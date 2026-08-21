package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAGradientLayer — thin wrapper for QuartzCore CAGradientLayer: draws a color
/// gradient along an axis (or radial/conic) across the layer bounds. Colors are
/// raw CGColorRefs held in an NSArray; locations are NSNumbers in 0..1.
public class CAGradientLayer extends CALayer {

    // [X layer] / id getters share one handle; object setters another.
    private record Handles(MethodHandle hGetId, MethodHandle hSetId, MethodHandle hGetPoint, MethodHandle hSetPoint) {}
    private static volatile Handles handles;

    protected CAGradientLayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static CAGradientLayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAGradientLayer(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT))
        );
    }

    /// +[CAGradientLayer layer]
    public static CAGradientLayer create() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetId().invokeExact(ObjC.cls("CAGradientLayer"), ObjC.sel("layer"));
            return wrap(p);
        } catch (Throwable t) { throw new RuntimeException("CAGradientLayer layer failed", t); }
    }

    /// [layer colors] — the gradient stops as an NSArray of CGColorRefs.
    public NSArray colors() {
        ensureInit();
        try {
            return NSArray.wrap((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("colors")));
        } catch (Throwable t) { throw new RuntimeException("colors failed", t); }
    }

    /// [layer setColors:] — NSArray of raw CGColorRefs.
    public void setColors(NSArray colors) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setColors:"), (MemorySegment) (colors == null ? MemorySegment.NULL : colors.peer()));
        } catch (Throwable t) { throw new RuntimeException("setColors: failed", t); }
    }

    /// [layer locations] — stop positions as NSNumber array in 0..1, or null
    /// (null means evenly spaced).
    public NSArray locations() {
        ensureInit();
        try {
            return NSArray.wrap((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("locations")));
        } catch (Throwable t) { throw new RuntimeException("locations failed", t); }
    }

    /// [layer setLocations:] — NSNumber array; count should match colors.
    public void setLocations(NSArray locations) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setLocations:"), (MemorySegment) (locations == null ? MemorySegment.NULL : locations.peer()));
        } catch (Throwable t) { throw new RuntimeException("setLocations: failed", t); }
    }

    /// [layer startPoint] — gradient start in unit coordinates.
    public NSPoint startPoint() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetPoint().invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("startPoint"));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("startPoint failed", t); }
    }

    /// [layer setStartPoint:]
    public void setStartPoint(NSPoint p) {
        ensureInit();
        try {
            handles.hSetPoint().invokeExact(peer, ObjC.sel("setStartPoint:"), p.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setStartPoint: failed", t); }
    }

    /// [layer endPoint] — gradient end in unit coordinates.
    public NSPoint endPoint() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetPoint().invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("endPoint"));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("endPoint failed", t); }
    }

    /// [layer setEndPoint:]
    public void setEndPoint(NSPoint p) {
        ensureInit();
        try {
            handles.hSetPoint().invokeExact(peer, ObjC.sel("setEndPoint:"), p.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setEndPoint: failed", t); }
    }

    /// [layer type] — "axial" (default), "radial" or "conic".
    public String type() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("type")));
        } catch (Throwable t) { throw new RuntimeException("type failed", t); }
    }

    /// [layer setType:]
    public void setType(String type) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setType:"), (MemorySegment) (type == null ? MemorySegment.NULL : ObjC.nsstring(type)));
        } catch (Throwable t) { throw new RuntimeException("setType: failed", t); }
    }
}
