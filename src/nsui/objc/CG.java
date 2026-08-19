package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

/**
 * CoreGraphics 2D drawing shim — the C functions (NOT ObjC) used to paint into
 * an NSGraphicsContext's CGContext. Purely FFM downcalls resolved at runtime;
 * CGFloat is a double on 64-bit, so every C function here is trivial.
 *
 * <p>The two Arg classes this touches are all pre-seeded:
 * <ul>
 *   <li>{@link NsuiForeign#cgSetRGBFillColor()} &amp; friends — the descriptors;</li>
 *   <li>the drawing functions take a {@code CGRect} <em>by value</em>; we allocate
 *       the 4-double struct via {@link ObjC#rect(double,double,double,double)} and
 *       hand the segment to the downcall (FFM copies it, so the caller keeps it).</li>
 * </ul>
 */
public final class CG {

    /** CoreGraphics framework dylib path. All CG_DRAW symbols are confirmed exported. */
    public static final String CORE_GRAPHICS = "/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics";

    // ---- one downcall handle per drawing function, built lazily in ensureInit() ----
    private static MethodHandle hFillColor;
    private static MethodHandle hStrokeColor;
    private static MethodHandle hLineWidth;
    private static MethodHandle hFillRect;
    private static MethodHandle hStrokeRect;
    private static MethodHandle hFillEllipse;
    private static MethodHandle hMoveToPoint;
    private static MethodHandle hAddLineToPoint;
    private static MethodHandle hStrokePath;
    private static MethodHandle hSetAntialias;

    private static volatile boolean initialized;

    private CG() {}

    /**
     * Resolve CoreGraphics and build the downcall handles. Must run at RUNTIME
     * (native-image rule — never in a static initializer). Idempotent.
     */
    public static synchronized void ensureInit() {
        if (initialized) return;
        SymbolLookup cg = SymbolLookup.libraryLookup(CORE_GRAPHICS, Arena.global());
        hFillColor     = down(cg, "CGContextSetRGBFillColor",     NsuiForeign.cgSetRGBFillColor());
        hStrokeColor   = down(cg, "CGContextSetRGBStrokeColor",   NsuiForeign.cgSetRGBStrokeColor());
        hLineWidth     = down(cg, "CGContextSetLineWidth",       NsuiForeign.cgSetLineWidth());
        hFillRect      = down(cg, "CGContextFillRect",           NsuiForeign.cgFillRect());
        hStrokeRect    = down(cg, "CGContextStrokeRect",         NsuiForeign.cgStrokeRect());
        hFillEllipse   = down(cg, "CGContextFillEllipseInRect",  NsuiForeign.cgFillEllipseInRect());
        hMoveToPoint   = down(cg, "CGContextMoveToPoint",        NsuiForeign.cgMoveToPoint());
        hAddLineToPoint= down(cg, "CGContextAddLineToPoint",     NsuiForeign.cgAddLineToPoint());
        hStrokePath    = down(cg, "CGContextStrokePath",         NsuiForeign.cgStrokePath());
        hSetAntialias  = down(cg, "CGContextSetShouldAntialias", NsuiForeign.cgSetShouldAntialias());
        initialized = true;
    }

    private static MethodHandle down(SymbolLookup lookup, String name, java.lang.foreign.FunctionDescriptor d) {
        try {
            return LinkerHolder.LINKER.downcallHandle(
                    lookup.find(name).orElseThrow(() -> new IllegalStateException("symbol not found: " + name)), d);
        } catch (Throwable t) {
            throw new RuntimeException("cannot bind CoreGraphics " + name, t);
        }
    }

    /** Cache the native linker (linking is fine at runtime; nothing in a static init). */
    private static final class LinkerHolder {
        static final java.lang.foreign.Linker LINKER = java.lang.foreign.Linker.nativeLinker();
    }

    // ---------------------------------------------------------------- API

    public static void setRGBFillColor(MemorySegment ctx, double r, double g, double b, double a) {
        try { hFillColor.invokeExact(ctx, r, g, b, a); } catch (Throwable t) { throw fail(t); }
    }

    public static void setRGBStrokeColor(MemorySegment ctx, double r, double g, double b, double a) {
        try { hStrokeColor.invokeExact(ctx, r, g, b, a); } catch (Throwable t) { throw fail(t); }
    }

    public static void setLineWidth(MemorySegment ctx, double w) {
        try { hLineWidth.invokeExact(ctx, w); } catch (Throwable t) { throw fail(t); }
    }

    public static void fillRect(MemorySegment ctx, double x, double y, double w, double h) {
        try { hFillRect.invokeExact(ctx, ObjC.rect(x, y, w, h)); } catch (Throwable t) { throw fail(t); }
    }

    public static void strokeRect(MemorySegment ctx, double x, double y, double w, double h) {
        try { hStrokeRect.invokeExact(ctx, ObjC.rect(x, y, w, h)); } catch (Throwable t) { throw fail(t); }
    }

    public static void fillEllipseInRect(MemorySegment ctx, double x, double y, double w, double h) {
        try { hFillEllipse.invokeExact(ctx, ObjC.rect(x, y, w, h)); } catch (Throwable t) { throw fail(t); }
    }

    public static void moveToPoint(MemorySegment ctx, double x, double y) {
        try { hMoveToPoint.invokeExact(ctx, x, y); } catch (Throwable t) { throw fail(t); }
    }

    public static void addLineToPoint(MemorySegment ctx, double x, double y) {
        try { hAddLineToPoint.invokeExact(ctx, x, y); } catch (Throwable t) { throw fail(t); }
    }

    public static void strokePath(MemorySegment ctx) {
        try { hStrokePath.invokeExact(ctx); } catch (Throwable t) { throw fail(t); }
    }

    public static void setShouldAntialias(MemorySegment ctx, boolean flag) {
        try { hSetAntialias.invokeExact(ctx, flag); } catch (Throwable t) { throw fail(t); }
    }

    private static RuntimeException fail(Throwable t) {
        return new RuntimeException("CoreGraphics call failed", t);
    }
}
