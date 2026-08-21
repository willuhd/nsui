package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSBezierPath — minimal wrapper over AppKit NSBezierPath.
/// Provides construction, point manipulation, stroking/filling.
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

        private static synchronized void ensureInit() {
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
}
