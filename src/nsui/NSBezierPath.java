package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSBezierPath — a vector path: construction, dashes, transforms, stroking/filling.
/// Thin stateless wrapper: each method maps to one objc_msgSend selector.
/// OMITTED (all shapes verified missing from Sig.VOCABULARY by grep):
/// containsPoint: (BOOL,POINT); strokeLineFromPoint:toPoint: + the quadratic
/// curveToPoint:controlPoint: pair (VOID,POINT,POINT); bezierPathWithRoundedRect:
/// xRadius:yRadius: (ID,RECT,DOUBLE,DOUBLE) + appendBezierPathWithRoundedRect:
/// (VOID,RECT,DOUBLE,DOUBLE); arc appends (POINT,DOUBLE,DOUBLE,DOUBLE[,BOOL]);
/// appendBezierPathWithPoints:count: + elementAtIndex:associatedPoints: +
/// setAssociatedPoints:atIndex: + getLineDash:setLineDash: (raw C pointers);
/// appendBezierPathWithCGGlyph:[s]: (CGGlyph scalars); drawPackedGlyphs:;
/// and the deprecated cachesBezierPath/glyph appends.
public final class NSBezierPath extends NSObject {

            private record Handles(MethodHandle hCreate, MethodHandle hWithRect, MethodHandle hVoidPoint, MethodHandle hCurve, MethodHandle hVoid, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hBool, MethodHandle hVoidId) {}
    private static volatile Handles handles;

    private NSBezierPath(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSBezierPath wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSBezierPath(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT, Arg.POINT, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.VOID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
        );
    }

    /// +[NSBezierPath bezierPath]
    public static NSBezierPath bezierPath() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hCreate().invokeExact(ObjC.cls("NSBezierPath"), ObjC.sel("bezierPath"));
            return new NSBezierPath(p);
        } catch (Throwable t) {
            throw new RuntimeException("bezierPath failed", t);
        }
    }

    /// +[NSBezierPath bezierPathWithRect:]
    public static NSBezierPath bezierPathWithRect(NSRect rect) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithRect().invokeExact(ObjC.cls("NSBezierPath"), ObjC.sel("bezierPathWithRect:"), rect.toSegment());
            return new NSBezierPath(p);
        } catch (Throwable t) {
            throw new RuntimeException("bezierPathWithRect: failed", t);
        }
    }

    /// +[NSBezierPath bezierPathWithOvalInRect:]
    public static NSBezierPath bezierPathWithOvalInRect(NSRect rect) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithRect().invokeExact(ObjC.cls("NSBezierPath"), ObjC.sel("bezierPathWithOvalInRect:"), rect.toSegment());
            return new NSBezierPath(p);
        } catch (Throwable t) {
            throw new RuntimeException("bezierPathWithOvalInRect: failed", t);
        }
    }

    /// -moveToPoint:
    public void moveToPoint(NSPoint p) {
        ensureInit();
        try {
            handles.hVoidPoint().invokeExact(peer, ObjC.sel("moveToPoint:"), p.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("moveToPoint: failed", t);
        }
    }

    /// -lineToPoint:
    public void lineToPoint(NSPoint p) {
        ensureInit();
        try {
            handles.hVoidPoint().invokeExact(peer, ObjC.sel("lineToPoint:"), p.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("lineToPoint: failed", t);
        }
    }

    /// -curveToPoint:controlPoint1:controlPoint2:
    public void curveToPoint(NSPoint end, NSPoint cp1, NSPoint cp2) {
        ensureInit();
        try {
            handles.hCurve().invokeExact(peer, ObjC.sel("curveToPoint:controlPoint1:controlPoint2:"), end.toSegment(), cp1.toSegment(), cp2.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("curveToPoint:controlPoint1:controlPoint2: failed", t);
        }
    }

    /// -closePath
    public void closePath() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, ObjC.sel("closePath"));
        } catch (Throwable t) {
            throw new RuntimeException("closePath failed", t);
        }
    }

    /// -stroke
    public void stroke() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, ObjC.sel("stroke"));
        } catch (Throwable t) {
            throw new RuntimeException("stroke failed", t);
        }
    }

    /// -fill
    public void fill() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, ObjC.sel("fill"));
        } catch (Throwable t) {
            throw new RuntimeException("fill failed", t);
        }
    }

    /// -lineWidth
    public double lineWidth() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("lineWidth"));
        } catch (Throwable t) {
            throw new RuntimeException("lineWidth failed", t);
        }
    }

    /// -setLineWidth:
    public void setLineWidth(double w) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setLineWidth:"), w);
        } catch (Throwable t) {
            throw new RuntimeException("setLineWidth: failed", t);
        }
    }

    /// -isEmpty
    public boolean isEmpty() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, ObjC.sel("isEmpty"));
        } catch (Throwable t) {
            throw new RuntimeException("isEmpty failed", t);
        }
    }

    /// -appendBezierPath:
    public void appendBezierPath(NSBezierPath other) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("appendBezierPath:"), (MemorySegment) (other == null ? MemorySegment.NULL : other.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("appendBezierPath: failed", t);
        }
    }

    /// -setClip
    public void setClip() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, ObjC.sel("setClip"));
        } catch (Throwable t) {
            throw new RuntimeException("setClip failed", t);
        }
    }

    /// -lineCapStyle / setLineCapStyle:
    public long lineCapStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("lineCapStyle"));
    }

    public void setLineCapStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLineCapStyle:"), style);
    }

    /// -lineJoinStyle / setLineJoinStyle:
    public long lineJoinStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("lineJoinStyle"));
    }

    public void setLineJoinStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLineJoinStyle:"), style);
    }

    /// -CGPath — the CoreGraphics CGPath backing this bezier path (raw pointer,
    /// owned by the path; do not release). Enables CAShapeLayer.setPath(NSBezierPath).
    public MemorySegment cgPath() {
        ensureInit();
        return ObjC.msgSendId(peer, ObjC.sel("CGPath"));
    }

    /// setCGPath: — replace the path contents from a CGPathRef peer (raw, copied).
    public void setCGPath(MemorySegment cgPath) {
        ensureInit();
        ObjC.msgSendVoidId(peer, ObjC.sel("setCGPath:"),
                (MemorySegment) (cgPath == null ? MemorySegment.NULL : cgPath));
    }

    // ---- style constants (NSLineCapStyle / NSLineJoinStyle / NSWindingRule /
    // NSBezierPathElement, verbatim from NSBezierPath.h) ----
    public static final long LINE_CAP_BUTT = 0;
    public static final long LINE_CAP_ROUND = 1;
    public static final long LINE_CAP_SQUARE = 2;
    public static final long LINE_JOIN_MITER = 0;
    public static final long LINE_JOIN_ROUND = 1;
    public static final long LINE_JOIN_BEVEL = 2;
    public static final long WINDING_NON_ZERO = 0;
    public static final long WINDING_EVEN_ODD = 1;
    public static final long ELEMENT_MOVE_TO = 0;
    public static final long ELEMENT_LINE_TO = 1;
    public static final long ELEMENT_CUBIC_CURVE_TO = 2;
    public static final long ELEMENT_CLOSE_PATH = 3;
    public static final long ELEMENT_QUADRATIC_CURVE_TO = 4;

    /// +bezierPathWithCGPath: — path from a CGPathRef peer (raw, copied).
    public static NSBezierPath bezierPathWithCGPath(MemorySegment cgPath) {
        ensureInit();
        if (cgPath == null || cgPath.address() == 0) throw new IllegalArgumentException("bezierPathWithCGPath: null");
        MemorySegment p = ObjC.msgSendIdId(ObjC.cls("NSBezierPath"), ObjC.sel("bezierPathWithCGPath:"), cgPath);
        return wrap(p);
    }

    private static void classFillStrokeClip(String sel, NSRect rect) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
            h.invokeExact(ObjC.cls("NSBezierPath"), ObjC.sel(sel), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException(sel + " failed", t);
        }
    }

    /// +fillRect: — fill a rect immediately in the current context.
    public static void fillRect(NSRect rect) { classFillStrokeClip("fillRect:", rect); }

    /// +strokeRect: — stroke a rect immediately in the current context.
    public static void strokeRect(NSRect rect) { classFillStrokeClip("strokeRect:", rect); }

    /// +clipRect: — intersect the current clip with a rect.
    public static void clipRect(NSRect rect) { classFillStrokeClip("clipRect:", rect); }

    private static double getClassDouble(String sel) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(ObjC.cls("NSBezierPath"), ObjC.sel(sel));
        } catch (Throwable t) {
            throw new RuntimeException(sel + " failed", t);
        }
    }

    private static void setClassDouble(String sel, double v) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
            h.invokeExact(ObjC.cls("NSBezierPath"), ObjC.sel(sel), v);
        } catch (Throwable t) {
            throw new RuntimeException(sel + " failed", t);
        }
    }

    /// defaultMiterLimit / setDefaultMiterLimit: (class).
    public static double defaultMiterLimit() { return getClassDouble("defaultMiterLimit"); }
    public static void setDefaultMiterLimit(double v) { setClassDouble("setDefaultMiterLimit:", v); }

    /// defaultFlatness / setDefaultFlatness: (class).
    public static double defaultFlatness() { return getClassDouble("defaultFlatness"); }
    public static void setDefaultFlatness(double v) { setClassDouble("setDefaultFlatness:", v); }

    /// defaultLineWidth / setDefaultLineWidth: (class).
    public static double defaultLineWidth() { return getClassDouble("defaultLineWidth"); }
    public static void setDefaultLineWidth(double v) { setClassDouble("setDefaultLineWidth:", v); }

    /// defaultWindingRule / setDefaultWindingRule: (class).
    public static long defaultWindingRule() {
        ensureInit();
        return ObjC.msgSendLong(ObjC.cls("NSBezierPath"), ObjC.sel("defaultWindingRule"));
    }
    public static void setDefaultWindingRule(long rule) {
        ensureInit();
        ObjC.msgSendVoidLong(ObjC.cls("NSBezierPath"), ObjC.sel("setDefaultWindingRule:"), rule);
    }

    /// defaultLineCapStyle / setDefaultLineCapStyle: (class).
    public static long defaultLineCapStyle() {
        ensureInit();
        return ObjC.msgSendLong(ObjC.cls("NSBezierPath"), ObjC.sel("defaultLineCapStyle"));
    }
    public static void setDefaultLineCapStyle(long style) {
        ensureInit();
        ObjC.msgSendVoidLong(ObjC.cls("NSBezierPath"), ObjC.sel("setDefaultLineCapStyle:"), style);
    }

    /// defaultLineJoinStyle / setDefaultLineJoinStyle: (class).
    public static long defaultLineJoinStyle() {
        ensureInit();
        return ObjC.msgSendLong(ObjC.cls("NSBezierPath"), ObjC.sel("defaultLineJoinStyle"));
    }
    public static void setDefaultLineJoinStyle(long style) {
        ensureInit();
        ObjC.msgSendVoidLong(ObjC.cls("NSBezierPath"), ObjC.sel("setDefaultLineJoinStyle:"), style);
    }

    /// -removeAllPoints.
    public void removeAllPoints() {
        ensureInit();
        ObjC.msgSendVoid(peer, ObjC.sel("removeAllPoints"));
    }

    /// -relativeMoveToPoint:.
    public void relativeMoveToPoint(NSPoint p) {
        ensureInit();
        try {
            handles.hVoidPoint().invokeExact(peer, ObjC.sel("relativeMoveToPoint:"), p.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("relativeMoveToPoint: failed", t);
        }
    }

    /// -relativeLineToPoint:.
    public void relativeLineToPoint(NSPoint p) {
        ensureInit();
        try {
            handles.hVoidPoint().invokeExact(peer, ObjC.sel("relativeLineToPoint:"), p.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("relativeLineToPoint: failed", t);
        }
    }

    /// -relativeCurveToPoint:controlPoint1:controlPoint2:.
    public void relativeCurveToPoint(NSPoint end, NSPoint cp1, NSPoint cp2) {
        ensureInit();
        try {
            handles.hCurve().invokeExact(peer, ObjC.sel("relativeCurveToPoint:controlPoint1:controlPoint2:"),
                    end.toSegment(), cp1.toSegment(), cp2.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("relativeCurveToPoint:controlPoint1:controlPoint2: failed", t);
        }
    }

    /// windingRule / setWindingRule:.
    public long windingRule() {
        ensureInit();
        return ObjC.msgSendLong(peer, ObjC.sel("windingRule"));
    }
    public void setWindingRule(long rule) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, ObjC.sel("setWindingRule:"), rule);
    }

    /// miterLimit / setMiterLimit:.
    public double miterLimit() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("miterLimit"));
        } catch (Throwable t) {
            throw new RuntimeException("miterLimit failed", t);
        }
    }
    public void setMiterLimit(double v) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setMiterLimit:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setMiterLimit: failed", t);
        }
    }

    /// flatness / setFlatness:.
    public double flatness() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("flatness"));
        } catch (Throwable t) {
            throw new RuntimeException("flatness failed", t);
        }
    }
    public void setFlatness(double v) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setFlatness:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setFlatness: failed", t);
        }
    }

    /// -addClip — intersect the current clip with this path (no-context safe).
    public void addClip() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, ObjC.sel("addClip"));
        } catch (Throwable t) {
            throw new RuntimeException("addClip failed", t);
        }
    }

    /// bezierPathByFlatteningPath — curve-free copy (nil-safe).
    public NSBezierPath bezierPathByFlatteningPath() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hCreate().invokeExact(peer, ObjC.sel("bezierPathByFlatteningPath"));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("bezierPathByFlatteningPath failed", t);
        }
    }

    /// bezierPathByReversingPath — reversed copy (nil-safe).
    public NSBezierPath bezierPathByReversingPath() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hCreate().invokeExact(peer, ObjC.sel("bezierPathByReversingPath"));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("bezierPathByReversingPath failed", t);
        }
    }

    /// transformUsingAffineTransform: — apply a transform (raw NSAffineTransform peer;
    /// no NSAffineTransform wrapper exists in this toolkit).
    public void transformUsingAffineTransform(MemorySegment transform) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("transformUsingAffineTransform:"),
                    (MemorySegment) (transform == null ? MemorySegment.NULL : transform));
        } catch (Throwable t) {
            throw new RuntimeException("transformUsingAffineTransform: failed", t);
        }
    }

    /// currentPoint — the last path point.
    public NSPoint currentPoint() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.POINT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("currentPoint"));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("currentPoint failed", t);
        }
    }

    /// controlPointBounds — bounds including control points.
    public NSRect controlPointBounds() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("controlPointBounds")));
    }

    /// bounds — the path bounding box.
    public NSRect bounds() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("bounds")));
    }

    /// elementCount.
    public long elementCount() {
        ensureInit();
        return ObjC.msgSendLong(peer, ObjC.sel("elementCount"));
    }

    /// elementAtIndex: — the element kind (ELEMENT_*), points variant omitted.
    public long elementAtIndex(long index) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("elementAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("elementAtIndex: failed", t);
        }
    }

    /// appendBezierPathWithRect:.
    public void appendBezierPathWithRect(NSRect rect) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
            h.invokeExact(peer, ObjC.sel("appendBezierPathWithRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("appendBezierPathWithRect: failed", t);
        }
    }

    /// appendBezierPathWithOvalInRect:.
    public void appendBezierPathWithOvalInRect(NSRect rect) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
            h.invokeExact(peer, ObjC.sel("appendBezierPathWithOvalInRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("appendBezierPathWithOvalInRect: failed", t);
        }
    }
}
