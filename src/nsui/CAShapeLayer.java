package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAShapeLayer — thin wrapper for QuartzCore CAShapeLayer: draws a CGPath with
/// fill/stroke styling. Raw CGPath/CGColor pointers are accepted alongside
/// NSBezierPath/NSColor conveniences (which read the "CGPath"/"CGColor" selectors).
public class CAShapeLayer extends CALayer {

    // Same-shape selectors share handles: [X layer] / id getters are all (id,SEL)->id;
    // every object setter is (id,SEL,id)->void; doubles likewise.
    // NOTE: fillRule is an NSString property on CAShapeLayer ("nonzero"/"evenodd"),
    // NOT a CGPathFillRule integer — hence no long handles here.
    private record Handles(MethodHandle hGetId, MethodHandle hSetId, MethodHandle hGetDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    protected CAShapeLayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static CAShapeLayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAShapeLayer(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE))
        );
    }

    /// +[CAShapeLayer layer]
    public static CAShapeLayer create() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetId().invokeExact(ObjC.cls("CAShapeLayer"), ObjC.sel("layer"));
            return wrap(p);
        } catch (Throwable t) { throw new RuntimeException("CAShapeLayer layer failed", t); }
    }

    /// [layer path] — raw CGPathRef or null.
    public MemorySegment path() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("path"));
        } catch (Throwable t) { throw new RuntimeException("path failed", t); }
    }

    /// [layer setPath:] — raw CGPathRef (NULL clears). Note: CoreAnimation copies
    /// the path on set, so path() returns a different (equal-geometry) pointer.
    public void setPath(MemorySegment cgPath) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setPath:"), (MemorySegment) (cgPath == null ? MemorySegment.NULL : cgPath));
        } catch (Throwable t) { throw new RuntimeException("setPath: failed", t); }
    }

    /// Convenience: set the path from an NSBezierPath via its CGPath accessor.
    public void setPath(NSBezierPath bezierPath) {
        setPath((MemorySegment) (bezierPath == null ? null : bezierPath.cgPath()));
    }

    /// [layer fillColor] — raw CGColorRef or null.
    public MemorySegment fillColor() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("fillColor"));
        } catch (Throwable t) { throw new RuntimeException("fillColor failed", t); }
    }

    /// [layer setFillColor:] — raw CGColorRef (NULL means no fill).
    public void setFillColor(MemorySegment cgColor) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setFillColor:"), (MemorySegment) (cgColor == null ? MemorySegment.NULL : cgColor));
        } catch (Throwable t) { throw new RuntimeException("setFillColor: failed", t); }
    }

    /// Convenience: set fill color from NSColor via its CGColor.
    public void setFillColor(NSColor color) {
        setFillColor((MemorySegment) (color == null ? MemorySegment.NULL : color.cgColor()));
    }

    /// [layer strokeColor] — raw CGColorRef or null.
    public MemorySegment strokeColor() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("strokeColor"));
        } catch (Throwable t) { throw new RuntimeException("strokeColor failed", t); }
    }

    /// [layer setStrokeColor:] — raw CGColorRef (NULL means no stroke).
    public void setStrokeColor(MemorySegment cgColor) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setStrokeColor:"), (MemorySegment) (cgColor == null ? MemorySegment.NULL : cgColor));
        } catch (Throwable t) { throw new RuntimeException("setStrokeColor: failed", t); }
    }

    /// Convenience: set stroke color from NSColor via its CGColor.
    public void setStrokeColor(NSColor color) {
        setStrokeColor((MemorySegment) (color == null ? MemorySegment.NULL : color.cgColor()));
    }

    /// [layer lineWidth] — stroke width in points.
    public double lineWidth() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("lineWidth"));
        } catch (Throwable t) { throw new RuntimeException("lineWidth failed", t); }
    }

    /// [layer setLineWidth:]
    public void setLineWidth(double width) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setLineWidth:"), width);
        } catch (Throwable t) { throw new RuntimeException("setLineWidth: failed", t); }
    }

    /// [layer fillRule] — path-filling rule string: "nonzero" (default) or "evenodd".
    /// DEVIATION from the requested long signature: CAShapeLayer.fillRule is declared
    /// `@property(copy) NSString *fillRule` in the macOS SDK — an integer send here
    /// would be retained as a bogus object pointer and crash.
    public String fillRule() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("fillRule")));
        } catch (Throwable t) { throw new RuntimeException("fillRule failed", t); }
    }

    /// [layer setFillRule:] — "nonzero" or "evenodd".
    public void setFillRule(String rule) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setFillRule:"), (MemorySegment) (rule == null ? MemorySegment.NULL : ObjC.nsstring(rule)));
        } catch (Throwable t) { throw new RuntimeException("setFillRule: failed", t); }
    }

    /// [layer lineCap] — "butt", "round" or "square".
    public String lineCap() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("lineCap")));
        } catch (Throwable t) { throw new RuntimeException("lineCap failed", t); }
    }

    /// [layer setLineCap:]
    public void setLineCap(String cap) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setLineCap:"), (MemorySegment) (cap == null ? MemorySegment.NULL : ObjC.nsstring(cap)));
        } catch (Throwable t) { throw new RuntimeException("setLineCap: failed", t); }
    }

    /// [layer lineJoin] — "miter", "round" or "bevel".
    public String lineJoin() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("lineJoin")));
        } catch (Throwable t) { throw new RuntimeException("lineJoin failed", t); }
    }

    /// [layer setLineJoin:]
    public void setLineJoin(String join) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setLineJoin:"), (MemorySegment) (join == null ? MemorySegment.NULL : ObjC.nsstring(join)));
        } catch (Throwable t) { throw new RuntimeException("setLineJoin: failed", t); }
    }

    /// [layer lineDashPattern] — phase-less dash lengths as NSNumber array, or null.
    public NSArray lineDashPattern() {
        ensureInit();
        try {
            return NSArray.wrap((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("lineDashPattern")));
        } catch (Throwable t) { throw new RuntimeException("lineDashPattern failed", t); }
    }

    /// [layer setLineDashPattern:] — NSNumber array of dash/space lengths.
    public void setLineDashPattern(NSArray pattern) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setLineDashPattern:"), (MemorySegment) (pattern == null ? MemorySegment.NULL : pattern.peer()));
        } catch (Throwable t) { throw new RuntimeException("setLineDashPattern: failed", t); }
    }

    /// [layer strokeStart] — fraction of the path where stroking begins (0..1).
    public double strokeStart() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("strokeStart"));
        } catch (Throwable t) { throw new RuntimeException("strokeStart failed", t); }
    }

    /// [layer setStrokeStart:]
    public void setStrokeStart(double start) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setStrokeStart:"), start);
        } catch (Throwable t) { throw new RuntimeException("setStrokeStart: failed", t); }
    }

    /// [layer strokeEnd] — fraction of the path where stroking ends (0..1).
    public double strokeEnd() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("strokeEnd"));
        } catch (Throwable t) { throw new RuntimeException("strokeEnd failed", t); }
    }

    /// [layer setStrokeEnd:]
    public void setStrokeEnd(double end) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setStrokeEnd:"), end);
        } catch (Throwable t) { throw new RuntimeException("setStrokeEnd: failed", t); }
    }
}
