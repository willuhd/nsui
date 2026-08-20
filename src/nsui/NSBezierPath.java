package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSBezierPath — minimal wrapper over AppKit NSBezierPath.
 * Provides construction, point manipulation, stroking/filling.
 */
public final class NSBezierPath extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hCreate;       // (id, SEL) -> id  [bezierPath]
    private static MethodHandle hWithRect;     // (id, SEL, NSRect) -> id [bezierPathWithRect:]
    private static MethodHandle hWithOval;     // (id, SEL, NSRect) -> id [bezierPathWithOvalInRect:]
    private static MethodHandle hVoidPoint;    // (id, SEL, NSPoint) -> void [moveToPoint:/lineToPoint:]
    private static MethodHandle hCurve;        // (id, SEL, NSPoint, NSPoint, NSPoint) -> void
    private static MethodHandle hVoid;         // (id, SEL) -> void [stroke/fill/closePath]
    private static MethodHandle hGetDouble;    // (id, SEL) -> double
    private static MethodHandle hSetDouble;    // (id, SEL, double) -> void
    private static MethodHandle hBool;         // (id, SEL) -> bool
    private static MethodHandle hVoidId;       // (id, SEL, id) -> void

    private NSBezierPath(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSBezierPath wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSBezierPath(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hCreate = ObjC.handle(Sig.of(Ret.ID));
        hWithRect = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hWithOval = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hVoidPoint = ObjC.handle(Sig.of(Ret.VOID, Arg.POINT));
        hCurve = ObjC.handle(Sig.of(Ret.VOID, Arg.POINT, Arg.POINT, Arg.POINT));
        hVoid = ObjC.handle(Sig.of(Ret.VOID));
        hGetDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /** +[NSBezierPath bezierPath] */
    public static NSBezierPath bezierPath() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hCreate.invokeExact(ObjC.cls("NSBezierPath"), ObjC.sel("bezierPath"));
            return new NSBezierPath(p);
        } catch (Throwable t) {
            throw new RuntimeException("bezierPath failed", t);
        }
    }

    /** +[NSBezierPath bezierPathWithRect:] */
    public static NSBezierPath bezierPathWithRect(NSRect rect) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hWithRect.invokeExact(ObjC.cls("NSBezierPath"), ObjC.sel("bezierPathWithRect:"), rect.toSegment());
            return new NSBezierPath(p);
        } catch (Throwable t) {
            throw new RuntimeException("bezierPathWithRect: failed", t);
        }
    }

    /** +[NSBezierPath bezierPathWithOvalInRect:] */
    public static NSBezierPath bezierPathWithOvalInRect(NSRect rect) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hWithOval.invokeExact(ObjC.cls("NSBezierPath"), ObjC.sel("bezierPathWithOvalInRect:"), rect.toSegment());
            return new NSBezierPath(p);
        } catch (Throwable t) {
            throw new RuntimeException("bezierPathWithOvalInRect: failed", t);
        }
    }

    /** -moveToPoint: */
    public void moveToPoint(NSPoint p) {
        ensureInit();
        try {
            hVoidPoint.invokeExact(peer, ObjC.sel("moveToPoint:"), p.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("moveToPoint: failed", t);
        }
    }

    /** -lineToPoint: */
    public void lineToPoint(NSPoint p) {
        ensureInit();
        try {
            hVoidPoint.invokeExact(peer, ObjC.sel("lineToPoint:"), p.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("lineToPoint: failed", t);
        }
    }

    /** -curveToPoint:controlPoint1:controlPoint2: */
    public void curveToPoint(NSPoint end, NSPoint cp1, NSPoint cp2) {
        ensureInit();
        try {
            hCurve.invokeExact(peer, ObjC.sel("curveToPoint:controlPoint1:controlPoint2:"), end.toSegment(), cp1.toSegment(), cp2.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("curveToPoint:controlPoint1:controlPoint2: failed", t);
        }
    }

    /** -closePath */
    public void closePath() {
        ensureInit();
        try {
            hVoid.invokeExact(peer, ObjC.sel("closePath"));
        } catch (Throwable t) {
            throw new RuntimeException("closePath failed", t);
        }
    }

    /** -stroke */
    public void stroke() {
        ensureInit();
        try {
            hVoid.invokeExact(peer, ObjC.sel("stroke"));
        } catch (Throwable t) {
            throw new RuntimeException("stroke failed", t);
        }
    }

    /** -fill */
    public void fill() {
        ensureInit();
        try {
            hVoid.invokeExact(peer, ObjC.sel("fill"));
        } catch (Throwable t) {
            throw new RuntimeException("fill failed", t);
        }
    }

    /** -lineWidth */
    public double lineWidth() {
        ensureInit();
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("lineWidth"));
        } catch (Throwable t) {
            throw new RuntimeException("lineWidth failed", t);
        }
    }

    /** -setLineWidth: */
    public void setLineWidth(double w) {
        ensureInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setLineWidth:"), w);
        } catch (Throwable t) {
            throw new RuntimeException("setLineWidth: failed", t);
        }
    }

    /** -isEmpty */
    public boolean isEmpty() {
        ensureInit();
        try {
            return (boolean) hBool.invokeExact(peer, ObjC.sel("isEmpty"));
        } catch (Throwable t) {
            throw new RuntimeException("isEmpty failed", t);
        }
    }

    /** -appendBezierPath: */
    public void appendBezierPath(NSBezierPath other) {
        ensureInit();
        try {
            hVoidId.invokeExact(peer, ObjC.sel("appendBezierPath:"), (MemorySegment) (other == null ? MemorySegment.NULL : other.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("appendBezierPath: failed", t);
        }
    }

    /** -setClip */
    public void setClip() {
        ensureInit();
        try {
            hVoid.invokeExact(peer, ObjC.sel("setClip"));
        } catch (Throwable t) {
            throw new RuntimeException("setClip failed", t);
        }
    }

    /** -lineCapStyle / setLineCapStyle: */
    public long lineCapStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("lineCapStyle"));
    }

    public void setLineCapStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLineCapStyle:"), style);
    }

    /** -lineJoinStyle / setLineJoinStyle: */
    public long lineJoinStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("lineJoinStyle"));
    }

    public void setLineJoinStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLineJoinStyle:"), style);
    }
}
