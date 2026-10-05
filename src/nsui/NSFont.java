package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSFont — an AppKit font. Thin 1:1 wrapper; constructors map to the message-send
/// shortcuts `+fontWithName:size:`, `+systemFontOfSize:` and
/// `+boldSystemFontOfSize:`.
public final class NSFont extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hFontWithName, MethodHandle hFontWithDescriptor, MethodHandle hSystemWeight, MethodHandle hDouble, MethodHandle hId, MethodHandle hBool, MethodHandle hInt) {}
    private static volatile Handles H;

    private NSFont(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSFont wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFont(peer);
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.INT)));
    }

    /// [+[NSFont fontWithName:size:]] — the named font at a point size.
    public static NSFont fontWithName(String name, double size) {
        ensureInit();
        try {
            MemorySegment f = (MemorySegment) H.hFontWithName().invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("fontWithName:size:"), ObjC.nsstring(name), size);
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("fontWithName:size: failed", t);
        }
    }

    /// [+[NSFont systemFontOfSize:]] — the system font at a point size.
    public static NSFont systemFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("systemFontOfSize:"), size));
    }

    /// [+[NSFont boldSystemFontOfSize:]] — the bold system font at a point size.
    public static NSFont boldSystemFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("boldSystemFontOfSize:"), size));
    }

    /// [+[NSFont systemFontOfSize:weight:]] — system font with explicit weight (NSFontWeight  -1..1, 0 = regular).
    public static NSFont systemFontOfSizeWeight(double size, double weight) {
        ensureInit();
        try {
            MemorySegment f = (MemorySegment) H.hSystemWeight().invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("systemFontOfSize:weight:"), size, weight);
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("systemFontOfSize:weight: failed", t);
        }
    }

    /// [+[NSFont monospacedSystemFontOfSize:weight:]] — monospaced system font.
    public static NSFont monospacedSystemFontOfSizeWeight(double size, double weight) {
        ensureInit();
        try {
            MemorySegment f = (MemorySegment) H.hSystemWeight().invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("monospacedSystemFontOfSize:weight:"), size, weight);
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("monospacedSystemFontOfSize:weight: failed", t);
        }
    }

    /// [+[NSFont labelFontOfSize:]] — label font.
    public static NSFont labelFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("labelFontOfSize:"), size));
    }

    /// [+[NSFont userFontOfSize:]] — application font.
    public static NSFont userFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("userFontOfSize:"), size));
    }

    /// [+[NSFont fontWithDescriptor:size:]] — font from descriptor.
    public static NSFont fontWithDescriptor(MemorySegment descriptor, double size) {
        ensureInit();
        try {
            MemorySegment f = (MemorySegment) H.hFontWithDescriptor().invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("fontWithDescriptor:size:"), descriptor, size);
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("fontWithDescriptor:size: failed", t);
        }
    }

    /// [+[NSFont systemFontSize]] — standard system font size.
    public static double systemFontSize() {
        ensureInit();
        try {
            MemorySegment cls = ObjC.cls("NSFont");
            // class property systemFontSize is (id,SEL)->double via handle
            return (double) H.hDouble().invokeExact(cls, ObjC.sel("systemFontSize"));
        } catch (Throwable t) {
            throw new RuntimeException("systemFontSize failed", t);
        }
    }

    /// [+[NSFont smallSystemFontSize]]
    public static double smallSystemFontSize() {
        ensureInit();
        try {
            return (double) H.hDouble().invokeExact(ObjC.cls("NSFont"), ObjC.sel("smallSystemFontSize"));
        } catch (Throwable t) {
            throw new RuntimeException("smallSystemFontSize failed", t);
        }
    }

    /// [+[NSFontManager sharedFontManager]] — returns raw NSFontManager peer.
    public static MemorySegment sharedFontManager() {
        return ObjC.msgSendId(ObjC.cls("NSFontManager"), ObjC.sel("sharedFontManager"));
    }

    /// [font fontName] — the font's PostScript name (NSString -> String).
    public String fontName() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("fontName")));
    }

    /// [font displayName] — human-readable name.
    public String displayName() {
        try {
            MemorySegment s = (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("displayName"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("displayName failed", t);
        }
    }

    /// [font familyName] — family name.
    public String familyName() {
        try {
            MemorySegment s = (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("familyName"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("familyName failed", t);
        }
    }

    /// [font pointSize] — the font's size in points.
    public double pointSize() {
        try {
            return (double) H.hDouble().invokeExact(peer, ObjC.sel("pointSize"));
        } catch (Throwable t) {
            throw new RuntimeException("pointSize failed", t);
        }
    }

    /// [font ascender]
    public double ascender() {
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("ascender")); } catch (Throwable t) { throw new RuntimeException("ascender failed", t); }
    }

    /// [font descender]
    public double descender() {
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("descender")); } catch (Throwable t) { throw new RuntimeException("descender failed", t); }
    }

    /// [font capHeight]
    public double capHeight() {
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("capHeight")); } catch (Throwable t) { throw new RuntimeException("capHeight failed", t); }
    }

    /// [font xHeight]
    public double xHeight() {
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("xHeight")); } catch (Throwable t) { throw new RuntimeException("xHeight failed", t); }
    }

    /// [font isFixedPitch]
    public boolean isFixedPitch() {
        try { return (boolean) H.hBool().invokeExact(peer, ObjC.sel("isFixedPitch")); } catch (Throwable t) { throw new RuntimeException("isFixedPitch failed", t); }
    }

    /// [font fontDescriptor] — raw NSFontDescriptor peer.
    public MemorySegment fontDescriptor() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("fontDescriptor")); } catch (Throwable t) { throw new RuntimeException("fontDescriptor failed", t); }
    }

    /// [fontDescriptor symbolicTraits] — bitmask (NSFontDescriptorSymbolicTraits).
    public long symbolicTraits() {
        MemorySegment desc = fontDescriptor();
        if (desc == null || desc.address() == 0) return 0;
        try { return (long) H.hInt().invokeExact(desc, ObjC.sel("symbolicTraits")); } catch (Throwable t) { throw new RuntimeException("symbolicTraits failed", t); }
    }

    /// [font textTransform] — NSAffineTransform peer or null.
    public MemorySegment textTransform() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("textTransform")); } catch (Throwable t) { throw new RuntimeException("textTransform failed", t); }
    }

    /// [font boundingRectForFont] — NSRect
    public NSRect boundingRectForFont() {
        try {
            MemorySegment r = (MemorySegment) ObjC.handle(Sig.of(Ret.RECT)).invokeExact(ObjC.structSlot(), peer, ObjC.sel("boundingRectForFont"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) { throw new RuntimeException("boundingRectForFont failed", t); }
    }

    /// [font maximumAdvancement] — NSSize
    public NSSize maximumAdvancement() {
        try {
            MemorySegment s = (MemorySegment) ObjC.handle(Sig.of(Ret.SIZE)).invokeExact(ObjC.structSlot(), peer, ObjC.sel("maximumAdvancement"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("maximumAdvancement failed", t); }
    }

    /// [font fontWithSize:] — same font at different size.
    public NSFont fontWithSize(double size) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
            MemorySegment f = (MemorySegment) h.invokeExact(peer, ObjC.sel("fontWithSize:"), size);
            return new NSFont(f);
        } catch (Throwable t) { throw new RuntimeException("fontWithSize: failed", t); }
    }

    /// [font set] — make current in graphics context (requires context).
    public void set() {
        ObjC.msgSendVoid(peer, ObjC.sel("set"));
    }

    /// [+[NSFont fontWithName:matrix:]] — matrix is const CGFloat* (raw segment, may be NULL).
    public static NSFont fontWithNameMatrix(String name, MemorySegment matrix) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment f = (MemorySegment) h.invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("fontWithName:matrix:"), ObjC.nsstring(name == null ? "" : name),
                    (MemorySegment) (matrix == null ? MemorySegment.NULL : matrix));
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("fontWithName:matrix: failed", t);
        }
    }

    /// [+[NSFont fontWithDescriptor:textTransform:]] — both raw segments (transform may be NULL).
    public static NSFont fontWithDescriptorTextTransform(MemorySegment descriptor, MemorySegment textTransform) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment f = (MemorySegment) h.invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("fontWithDescriptor:textTransform:"),
                    (MemorySegment) (descriptor == null ? MemorySegment.NULL : descriptor),
                    (MemorySegment) (textTransform == null ? MemorySegment.NULL : textTransform));
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("fontWithDescriptor:textTransform: failed", t);
        }
    }

    /// [+[NSFont userFixedPitchFontOfSize:]] — fixed-pitch application font.
    public static NSFont userFixedPitchFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("userFixedPitchFontOfSize:"), size));
    }

    /// [+[NSFont setUserFont:]] — set the application font preference.
    public static void setUserFont(NSFont font) {
        ObjC.msgSendVoidId(ObjC.cls("NSFont"), ObjC.sel("setUserFont:"),
                (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
    }

    /// [+[NSFont setUserFixedPitchFont:]] — set the fixed-pitch preference.
    public static void setUserFixedPitchFont(NSFont font) {
        ObjC.msgSendVoidId(ObjC.cls("NSFont"), ObjC.sel("setUserFixedPitchFont:"),
                (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
    }

    /// [+[NSFont titleBarFontOfSize:]]
    public static NSFont titleBarFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("titleBarFontOfSize:"), size));
    }

    /// [+[NSFont menuFontOfSize:]]
    public static NSFont menuFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("menuFontOfSize:"), size));
    }

    /// [+[NSFont menuBarFontOfSize:]]
    public static NSFont menuBarFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("menuBarFontOfSize:"), size));
    }

    /// [+[NSFont messageFontOfSize:]]
    public static NSFont messageFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("messageFontOfSize:"), size));
    }

    /// [+[NSFont paletteFontOfSize:]]
    public static NSFont paletteFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("paletteFontOfSize:"), size));
    }

    /// [+[NSFont toolTipsFontOfSize:]]
    public static NSFont toolTipsFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("toolTipsFontOfSize:"), size));
    }

    /// [+[NSFont controlContentFontOfSize:]]
    public static NSFont controlContentFontOfSize(double size) {
        return new NSFont(ObjC.msgSendIdDouble(ObjC.cls("NSFont"), ObjC.sel("controlContentFontOfSize:"), size));
    }

    /// [+[NSFont monospacedDigitSystemFontOfSize:weight:]] — monospaced digits.
    public static NSFont monospacedDigitSystemFontOfSizeWeight(double size, double weight) {
        ensureInit();
        try {
            MemorySegment f = (MemorySegment) H.hSystemWeight().invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("monospacedDigitSystemFontOfSize:weight:"), size, weight);
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("monospacedDigitSystemFontOfSize:weight: failed", t);
        }
    }

    // +systemFontOfSize:weight:width: omitted: (DOUBLE,DOUBLE,DOUBLE)->ID has no
    // vocabulary entry (grep MISS for exact 3-double shape; only the 4-double color
    // entry matches as a substring) — reported to parent.

    /// [+[NSFont labelFontSize]] — standard label font size.
    public static double labelFontSize() {
        ensureInit();
        try {
            return (double) H.hDouble().invokeExact(ObjC.cls("NSFont"), ObjC.sel("labelFontSize"));
        } catch (Throwable t) {
            throw new RuntimeException("labelFontSize failed", t);
        }
    }

    /// [+[NSFont systemFontSizeForControlSize:]] — size for an NSControlSize.
    public static double systemFontSizeForControlSize(long controlSize) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE, Arg.INT));
            return (double) h.invokeExact(ObjC.cls("NSFont"), ObjC.sel("systemFontSizeForControlSize:"), controlSize);
        } catch (Throwable t) {
            throw new RuntimeException("systemFontSizeForControlSize: failed", t);
        }
    }

    /// [+[NSFont preferredFontForTextStyle:options:]] — options may be NULL.
    public static NSFont preferredFontForTextStyle(String style, MemorySegment optionsOrNull) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment f = (MemorySegment) h.invokeExact(
                    ObjC.cls("NSFont"), ObjC.sel("preferredFontForTextStyle:options:"),
                    ObjC.nsstring(style == null ? "" : style),
                    (MemorySegment) (optionsOrNull == null ? MemorySegment.NULL : optionsOrNull));
            if (f == null || f.address() == 0) return null;
            return new NSFont(f);
        } catch (Throwable t) {
            throw new RuntimeException("preferredFontForTextStyle:options: failed", t);
        }
    }

    /// [font matrix] — const CGFloat* (raw segment; NSFontIdentityMatrix layout, 6 doubles).
    public MemorySegment matrix() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("matrix")); } catch (Throwable t) { throw new RuntimeException("matrix failed", t); }
    }

    /// [font numberOfGlyphs].
    public long numberOfGlyphs() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("numberOfGlyphs")); } catch (Throwable t) { throw new RuntimeException("numberOfGlyphs failed", t); }
    }

    /// [font mostCompatibleStringEncoding] -> NSStringEncoding.
    public long mostCompatibleStringEncoding() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("mostCompatibleStringEncoding")); } catch (Throwable t) { throw new RuntimeException("mostCompatibleStringEncoding failed", t); }
    }

    /// [font coveredCharacterSet] -> NSCharacterSet* (raw; no wrapper in this batch).
    public MemorySegment coveredCharacterSet() {
        try { return (MemorySegment) H.hId().invokeExact(peer, ObjC.sel("coveredCharacterSet")); } catch (Throwable t) { throw new RuntimeException("coveredCharacterSet failed", t); }
    }

    /// [font leading]
    public double leading() {
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("leading")); } catch (Throwable t) { throw new RuntimeException("leading failed", t); }
    }

    /// [font underlinePosition]
    public double underlinePosition() {
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("underlinePosition")); } catch (Throwable t) { throw new RuntimeException("underlinePosition failed", t); }
    }

    /// [font underlineThickness]
    public double underlineThickness() {
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("underlineThickness")); } catch (Throwable t) { throw new RuntimeException("underlineThickness failed", t); }
    }

    /// [font italicAngle]
    public double italicAngle() {
        try { return (double) H.hDouble().invokeExact(peer, ObjC.sel("italicAngle")); } catch (Throwable t) { throw new RuntimeException("italicAngle failed", t); }
    }

    // -boundingRectForCGGlyph: / -advancementForCGGlyph: / bulk getBoundingRects:...
    // omitted: struct returns with INT args (RECT(INT), SIZE(INT)) have no vocabulary
    // entry (grep MISS) — reported to parent. Deprecated NSGlyph API skipped likewise.

    /// [font setInContext:] — make current in the given graphics context.
    public void setInContext(MemorySegment graphicsContext) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setInContext:"),
                (MemorySegment) (graphicsContext == null ? MemorySegment.NULL : graphicsContext));
    }

    /// [font verticalFont] — vertical variant, or self if unsupported.
    public NSFont verticalFont() {
        try { return NSFont.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("verticalFont"))); } catch (Throwable t) { throw new RuntimeException("verticalFont failed", t); }
    }

    /// [font isVertical]
    public boolean isVertical() {
        try { return (boolean) H.hBool().invokeExact(peer, ObjC.sel("isVertical")); } catch (Throwable t) { throw new RuntimeException("isVertical failed", t); }
    }

    /// [font printerFont]
    public NSFont printerFont() {
        try { return NSFont.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("printerFont"))); } catch (Throwable t) { throw new RuntimeException("printerFont failed", t); }
    }

    /// [font screenFont]
    public NSFont screenFont() {
        try { return NSFont.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("screenFont"))); } catch (Throwable t) { throw new RuntimeException("screenFont failed", t); }
    }

    /// [font screenFontWithRenderingMode:] — NSFontRenderingMode.
    public NSFont screenFontWithRenderingMode(long mode) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment f = (MemorySegment) h.invokeExact(peer, ObjC.sel("screenFontWithRenderingMode:"), mode);
            return NSFont.wrap(f);
        } catch (Throwable t) {
            throw new RuntimeException("screenFontWithRenderingMode: failed", t);
        }
    }

    /// [font renderingMode] — NSFontRenderingMode.
    public long renderingMode() {
        try { return (long) H.hInt().invokeExact(peer, ObjC.sel("renderingMode")); } catch (Throwable t) { throw new RuntimeException("renderingMode failed", t); }
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
