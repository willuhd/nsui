package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSFont — an AppKit font. Thin 1:1 wrapper; constructors map to the message-send
 * shortcuts {@code +fontWithName:size:}, {@code +systemFontOfSize:} and
 * {@code +boldSystemFontOfSize:}.
 */
public final class NSFont extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hFontWithName;  // (id, SEL, id, double) -> id
    private static MethodHandle hFontWithDescriptor; // (id, SEL, id, double) -> id
    private static MethodHandle hSystemWeight;  // (id, SEL, double, double) -> id
    private static MethodHandle hDouble;        // (id, SEL) -> double
    private static MethodHandle hId;            // (id, SEL) -> id
    private static MethodHandle hBool;          // (id, SEL) -> bool
    private static MethodHandle hInt;           // (id, SEL) -> long

    private NSFont(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSFont wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFont(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hFontWithName = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.DOUBLE));
        hFontWithDescriptor = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.DOUBLE));
        hSystemWeight = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE, Arg.DOUBLE));
        hDouble       = ObjC.handle(Sig.of(Ret.DOUBLE));
        hId           = ObjC.handle(Sig.of(Ret.ID));
        hBool         = ObjC.handle(Sig.of(Ret.BOOL));
        hInt          = ObjC.handle(Sig.of(Ret.INT));
        initialized = true;
    }

    /** [+[NSFont fontWithName:size:]] — the named font at a point size. */
    public static NSFont fontWithName(String name, double size) {
        ensureInit();
        try {
            MemorySegment f = (MemorySegment) hFontWithName.invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("fontWithName:size:"), ObjC.nsstring(name), size);
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("fontWithName:size: failed", t);
        }
    }

    /** [+[NSFont systemFontOfSize:]] — the system font at a point size. */
    public static NSFont systemFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("systemFontOfSize:"), size));
    }

    /** [+[NSFont boldSystemFontOfSize:]] — the bold system font at a point size. */
    public static NSFont boldSystemFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("boldSystemFontOfSize:"), size));
    }

    /** [+[NSFont systemFontOfSize:weight:]] — system font with explicit weight (NSFontWeight  -1..1, 0 = regular). */
    public static NSFont systemFontOfSizeWeight(double size, double weight) {
        ensureInit();
        try {
            MemorySegment f = (MemorySegment) hSystemWeight.invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("systemFontOfSize:weight:"), size, weight);
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("systemFontOfSize:weight: failed", t);
        }
    }

    /** [+[NSFont monospacedSystemFontOfSize:weight:]] — monospaced system font. */
    public static NSFont monospacedSystemFontOfSizeWeight(double size, double weight) {
        ensureInit();
        try {
            MemorySegment f = (MemorySegment) hSystemWeight.invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("monospacedSystemFontOfSize:weight:"), size, weight);
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("monospacedSystemFontOfSize:weight: failed", t);
        }
    }

    /** [+[NSFont labelFontOfSize:]] — label font. */
    public static NSFont labelFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("labelFontOfSize:"), size));
    }

    /** [+[NSFont userFontOfSize:]] — application font. */
    public static NSFont userFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("userFontOfSize:"), size));
    }

    /** [+[NSFont fontWithDescriptor:size:]] — font from descriptor. */
    public static NSFont fontWithDescriptor(MemorySegment descriptor, double size) {
        ensureInit();
        try {
            MemorySegment f = (MemorySegment) hFontWithDescriptor.invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("fontWithDescriptor:size:"), descriptor, size);
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("fontWithDescriptor:size: failed", t);
        }
    }

    /** [+[NSFont systemFontSize]] — standard system font size. */
    public static double systemFontSize() {
        ensureInit();
        try {
            MemorySegment cls = ObjC.cls("NSFont");
            // class property systemFontSize is (id,SEL)->double via handle
            return (double) hDouble.invokeExact(cls, ObjC.sel("systemFontSize"));
        } catch (Throwable t) {
            throw new RuntimeException("systemFontSize failed", t);
        }
    }

    /** [+[NSFont smallSystemFontSize]] */
    public static double smallSystemFontSize() {
        ensureInit();
        try {
            return (double) hDouble.invokeExact(ObjC.cls("NSFont"), ObjC.sel("smallSystemFontSize"));
        } catch (Throwable t) {
            throw new RuntimeException("smallSystemFontSize failed", t);
        }
    }

    /** [+[NSFontManager sharedFontManager]] — returns raw NSFontManager peer. */
    public static MemorySegment sharedFontManager() {
        return ObjC.msgSendId(ObjC.cls("NSFontManager"), ObjC.sel("sharedFontManager"));
    }

    /** [font fontName] — the font's PostScript name (NSString -> String). */
    public String fontName() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("fontName")));
    }

    /** [font displayName] — human-readable name. */
    public String displayName() {
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("displayName"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("displayName failed", t);
        }
    }

    /** [font familyName] — family name. */
    public String familyName() {
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("familyName"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("familyName failed", t);
        }
    }

    /** [font pointSize] — the font's size in points. */
    public double pointSize() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("pointSize"));
        } catch (Throwable t) {
            throw new RuntimeException("pointSize failed", t);
        }
    }

    /** [font ascender] */
    public double ascender() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("ascender")); } catch (Throwable t) { throw new RuntimeException("ascender failed", t); }
    }

    /** [font descender] */
    public double descender() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("descender")); } catch (Throwable t) { throw new RuntimeException("descender failed", t); }
    }

    /** [font capHeight] */
    public double capHeight() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("capHeight")); } catch (Throwable t) { throw new RuntimeException("capHeight failed", t); }
    }

    /** [font xHeight] */
    public double xHeight() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("xHeight")); } catch (Throwable t) { throw new RuntimeException("xHeight failed", t); }
    }

    /** [font isFixedPitch] */
    public boolean isFixedPitch() {
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isFixedPitch")); } catch (Throwable t) { throw new RuntimeException("isFixedPitch failed", t); }
    }

    /** [font fontDescriptor] — raw NSFontDescriptor peer. */
    public MemorySegment fontDescriptor() {
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("fontDescriptor")); } catch (Throwable t) { throw new RuntimeException("fontDescriptor failed", t); }
    }

    /** [fontDescriptor symbolicTraits] — bitmask (NSFontDescriptorSymbolicTraits). */
    public long symbolicTraits() {
        MemorySegment desc = fontDescriptor();
        if (desc == null || desc.address() == 0) return 0;
        try { return (long) hInt.invokeExact(desc, ObjC.sel("symbolicTraits")); } catch (Throwable t) { throw new RuntimeException("symbolicTraits failed", t); }
    }

    /** [font textTransform] — NSAffineTransform peer or null. */
    public MemorySegment textTransform() {
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("textTransform")); } catch (Throwable t) { throw new RuntimeException("textTransform failed", t); }
    }

    /** [font boundingRectForFont] — NSRect */
    public NSRect boundingRectForFont() {
        try {
            MemorySegment r = (MemorySegment) ObjC.handle(Sig.of(Ret.RECT)).invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("boundingRectForFont"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) { throw new RuntimeException("boundingRectForFont failed", t); }
    }

    /** [font maximumAdvancement] — NSSize */
    public NSSize maximumAdvancement() {
        try {
            MemorySegment s = (MemorySegment) ObjC.handle(Sig.of(Ret.SIZE)).invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("maximumAdvancement"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("maximumAdvancement failed", t); }
    }

    /** [font fontWithSize:] — same font at different size. */
    public NSFont fontWithSize(double size) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
            MemorySegment f = (MemorySegment) h.invokeExact(peer, ObjC.sel("fontWithSize:"), size);
            return new NSFont(f);
        } catch (Throwable t) { throw new RuntimeException("fontWithSize: failed", t); }
    }

    /** [font set] — make current in graphics context (requires context). */
    public void set() {
        ObjC.msgSendVoid(peer, ObjC.sel("set"));
    }

    // ---- weight constants (NSFontWeight) ----
    public static final double WEIGHT_ULTRA_LIGHT = -0.8;
    public static final double WEIGHT_THIN = -0.6;
    public static final double WEIGHT_LIGHT = -0.4;
    public static final double WEIGHT_REGULAR = 0.0;
    public static final double WEIGHT_MEDIUM = 0.23;
    public static final double WEIGHT_SEMIBOLD = 0.3;
    public static final double WEIGHT_BOLD = 0.4;
    public static final double WEIGHT_HEAVY = 0.56;
    public static final double WEIGHT_BLACK = 0.62;
}
