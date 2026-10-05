package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Scratch;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSGradient — a color transition (linear or radial) drawn in the current context.
/// Thin stateless wrapper: each method maps to one objc_msgSend selector.
/// OMITTED: initWithColorsAndLocations: (C varargs need manual va_list
/// marshalling); drawFromPoint:toPoint:options: + drawFromCenter:radius:toCenter:
/// radius:options: (multi-struct shapes NOT in the Sig vocabulary — verified by
/// grep, use drawInRect:angle:); drawInRect:relativeCenterPosition: +
/// drawInBezierPath:relativeCenterPosition: (shapes (VOID,RECT,POINT) and
/// (VOID,ID,POINT) likewise unregistered); getColor:location:atIndex:
/// (raw CGFloat/NSInteger out-params).
public final class NSGradient extends NSObject {

            private record Handles(MethodHandle hInitTwo, MethodHandle hInitColors, MethodHandle hInitThree, MethodHandle hDrawRectAngle, MethodHandle hDrawBezierAngle) {}
    private static volatile Handles handles;

    private NSGradient(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSGradient wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSGradient(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.DOUBLE))
        );
    }

    /// [[NSGradient alloc] initWithStartingColor:endingColor:]
    public static NSGradient initWithStartingColorEndingColor(NSColor starting, NSColor ending) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSGradient"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInitTwo().invokeExact(alloc, ObjC.sel("initWithStartingColor:endingColor:"),
                    (MemorySegment) (starting == null ? MemorySegment.NULL : starting.peer()),
                    (MemorySegment) (ending == null ? MemorySegment.NULL : ending.peer()));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSGradient initWithStartingColor:endingColor: returned nil");
            return new NSGradient(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithStartingColor:endingColor: failed", t);
        }
    }

    /// [[NSGradient alloc] initWithColors:] with NSArray of NSColor
    public static NSGradient initWithColors(NSArray colors) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSGradient"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInitColors().invokeExact(alloc, ObjC.sel("initWithColors:"), colors.peer());
            if (p == null || p.address() == 0) throw new IllegalStateException("NSGradient initWithColors: returned nil");
            return new NSGradient(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithColors: failed", t);
        }
    }

    /// -drawInRect:angle:
    public void drawInRectAngle(NSRect rect, double angle) {
        ensureInit();
        try {
            handles.hDrawRectAngle().invokeExact(peer, ObjC.sel("drawInRect:angle:"), rect.toSegment(), angle);
        } catch (Throwable t) {
            throw new RuntimeException("drawInRect:angle: failed", t);
        }
    }

    /// -drawInBezierPath:angle:
    public void drawInBezierPathAngle(NSBezierPath path, double angle) {
        ensureInit();
        try {
            handles.hDrawBezierAngle().invokeExact(peer, ObjC.sel("drawInBezierPath:angle:"),
                    (MemorySegment) (path == null ? MemorySegment.NULL : path.peer()), angle);
        } catch (Throwable t) {
            throw new RuntimeException("drawInBezierPath:angle: failed", t);
        }
    }

    /// [[NSGradient alloc] initWithColors:atLocations:colorSpace:] — the designated
    /// initializer: colors paired with locations in 0..1. `colorSpacePeer` is a raw
    /// NSColorSpace id (no NSColorSpace wrapper exists); NULL selects the default.
    /// The locations array is copied synchronously, so call-scoped scratch is exact.
    public static NSGradient initWithColorsAtLocationsColorSpace(NSArray colors, double[] locations, MemorySegment colorSpacePeer) {
        ensureInit();
        if (colors == null) throw new IllegalArgumentException("initWithColors:atLocations:colorSpace: null colors");
        MemorySegment loc = MemorySegment.NULL;
        if (locations != null && locations.length > 0) {
            loc = Scratch.allocInput((long) locations.length * 8L);
            for (int i = 0; i < locations.length; i++) {
                loc.set(ValueLayout.JAVA_DOUBLE, (long) i * 8L, locations[i]);
            }
        }
        MemorySegment space = (colorSpacePeer == null || colorSpacePeer.address() == 0) ? MemorySegment.NULL : colorSpacePeer;
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSGradient"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInitThree().invokeExact(alloc,
                    ObjC.sel("initWithColors:atLocations:colorSpace:"), colors.peer(), loc, space);
            if (p == null || p.address() == 0) throw new IllegalStateException("NSGradient initWithColors:atLocations:colorSpace: returned nil");
            return new NSGradient(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithColors:atLocations:colorSpace: failed", t);
        }
    }

    /// Convenience: even spacing in the generic RGB space.
    public static NSGradient initWithColorsAtLocations(NSArray colors, double[] locations) {
        MemorySegment genericRGB = ObjC.msgSendId(ObjC.cls("NSColorSpace"), ObjC.sel("genericRGBColorSpace"));
        return initWithColorsAtLocationsColorSpace(colors, locations, genericRGB);
    }

    /// colorSpace — the interpolation space (raw NSColorSpace peer, nil-safe;
    /// no NSColorSpace wrapper exists in this toolkit).
    public MemorySegment colorSpacePeer() {
        ensureInit();
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("colorSpace"));
        return (c == null || c.address() == 0) ? null : c;
    }

    /// numberOfColorStops.
    public long numberOfColorStops() {
        ensureInit();
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfColorStops"));
    }

    /// drawFromPointToPoint: — NOT WRAPPED (kept only for source compatibility:
    /// FullCoverageTest calls it and asserts the vocabulary-miss message).
    /// Always throws: drawFromPoint:toPoint:options: needs (VOID,POINT,POINT,INT),
    /// which is NOT in the Sig vocabulary (verified by grep). Use drawInRect:angle:.
    public void drawFromPointToPoint(NSPoint start, NSPoint end) {
        throw new RuntimeException(new UnsupportedOperationException(
                "drawFromPoint:toPoint:options: not in minimal vocab — use drawInRect:angle:"));
    }

    /// interpolatedColorAtLocation: — the blended color at 0..1 (nil-safe).
    public NSColor interpolatedColorAtLocation(double location) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
            MemorySegment c = (MemorySegment) h.invokeExact(peer, ObjC.sel("interpolatedColorAtLocation:"), location);
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("interpolatedColorAtLocation: failed", t);
        }
    }
}
