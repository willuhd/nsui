package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSColor — an AppKit color in the sRGB extended color space. Thin 1:1 wrapper
/// over a native `NSColor`; components are read back with
/// `getRed:green:blue:alpha:`.
public final class NSColor extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hCreate;  // (id, SEL, double, double, double, double) -> id
    private static MethodHandle hClassColor; // (id, SEL) -> id  for class colors like redColor
    private static MethodHandle hPattern; // (id, SEL, id) -> id [colorWithPatternImage:]
    private static MethodHandle hCatalog; // (id, SEL, id, id) -> id [colorWithCatalogName:colorName:]
    private static MethodHandle hAlpha;   // (id, SEL) -> double [alphaComponent]
    private static MethodHandle hWithAlpha; // (id, SEL, double) -> id [colorWithAlphaComponent:]
    private static MethodHandle hBlended; // (id, SEL, double, id) -> id [blendedColorWithFraction:ofColor:]
    private static MethodHandle hCatalogName; // (id, SEL) -> id [catalogNameComponent]
    private static MethodHandle hColorName;   // (id, SEL) -> id [colorNameComponent]

    private NSColor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hCreate = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE, Arg.DOUBLE, Arg.DOUBLE, Arg.DOUBLE));
        hClassColor = ObjC.handle(Sig.of(Ret.ID));
        hPattern = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hCatalog = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
        hAlpha = ObjC.handle(Sig.of(Ret.DOUBLE));
        hWithAlpha = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
        hBlended = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE, Arg.ID));
        hCatalogName = ObjC.handle(Sig.of(Ret.ID));
        hColorName = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
    }

    /// Wrap a native NSColor id as an NSColor (null for nil). Enables typed bridging from controls that return NSColor.
    public static NSColor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSColor(peer);
    }

    /// [+[NSColor colorWithSRGBRed:green:blue:alpha:]] — sRGB extended color.
    public static NSColor create(double r, double g, double b, double a) {
        ensureInit();
        MemorySegment color;
        try {
            color = (MemorySegment) hCreate.invokeExact(
                    ObjC.cls("NSColor"), ObjC.sel("colorWithSRGBRed:green:blue:alpha:"), r, g, b, a);
        } catch (Throwable t) {
            throw new RuntimeException("colorWithSRGBRed:green:blue:alpha: failed", t);
        }
        return new NSColor(color);
    }

    // ---- system / semantic colors ----

    private static NSColor classColor(String sel) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hClassColor.invokeExact(ObjC.cls("NSColor"), ObjC.sel(sel));
            return new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException(sel + " failed", t);
        }
    }

    public static NSColor blackColor() { return classColor("blackColor"); }
    public static NSColor whiteColor() { return classColor("whiteColor"); }
    public static NSColor redColor() { return classColor("redColor"); }
    public static NSColor greenColor() { return classColor("greenColor"); }
    public static NSColor blueColor() { return classColor("blueColor"); }
    public static NSColor clearColor() { return classColor("clearColor"); }
    public static NSColor grayColor() { return classColor("grayColor"); }
    public static NSColor darkGrayColor() { return classColor("darkGrayColor"); }
    public static NSColor lightGrayColor() { return classColor("lightGrayColor"); }
    public static NSColor cyanColor() { return classColor("cyanColor"); }
    public static NSColor yellowColor() { return classColor("yellowColor"); }
    public static NSColor magentaColor() { return classColor("magentaColor"); }
    public static NSColor orangeColor() { return classColor("orangeColor"); }
    public static NSColor purpleColor() { return classColor("purpleColor"); }
    public static NSColor brownColor() { return classColor("brownColor"); }

    // semantic / system colors (macOS 10.10+)
    public static NSColor labelColor() { return classColor("labelColor"); }
    public static NSColor secondaryLabelColor() { return classColor("secondaryLabelColor"); }
    public static NSColor tertiaryLabelColor() { return classColor("tertiaryLabelColor"); }
    public static NSColor quaternaryLabelColor() { return classColor("quaternaryLabelColor"); }
    public static NSColor linkColor() { return classColor("linkColor"); }
    public static NSColor placeholderTextColor() { return classColor("placeholderTextColor"); }
    public static NSColor windowBackgroundColor() { return classColor("windowBackgroundColor"); }
    public static NSColor controlBackgroundColor() { return classColor("controlBackgroundColor"); }
    public static NSColor selectedContentBackgroundColor() { return classColor("selectedContentBackgroundColor"); }
    public static NSColor textColor() { return classColor("textColor"); }
    public static NSColor textBackgroundColor() { return classColor("textBackgroundColor"); }
    public static NSColor controlColor() { return classColor("controlColor"); }
    public static NSColor controlTextColor() { return classColor("controlTextColor"); }
    public static NSColor selectedControlColor() { return classColor("selectedControlColor"); }
    public static NSColor gridColor() { return classColor("gridColor"); }
    public static NSColor separatorColor() { return classColor("separatorColor"); }
    public static NSColor systemRedColor() { return classColor("systemRedColor"); }
    public static NSColor systemGreenColor() { return classColor("systemGreenColor"); }
    public static NSColor systemBlueColor() { return classColor("systemBlueColor"); }
    public static NSColor systemOrangeColor() { return classColor("systemOrangeColor"); }
    public static NSColor systemYellowColor() { return classColor("systemYellowColor"); }
    public static NSColor systemGrayColor() { return classColor("systemGrayColor"); }
    public static NSColor controlAccentColor() { return classColor("controlAccentColor"); }
    public static NSColor windowFrameTextColor() { return classColor("windowFrameTextColor"); }
    public static NSColor headerTextColor() { return classColor("headerTextColor"); }

    /// [+[NSColor colorWithPatternImage:]] — pattern color tiled from an image.
    public static NSColor colorWithPatternImage(NSImage image) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hPattern.invokeExact(ObjC.cls("NSColor"), ObjC.sel("colorWithPatternImage:"), image.peer());
            return new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorWithPatternImage: failed", t);
        }
    }

    /// [+[NSColor colorWithCatalogName:colorName:]] — catalog color, or null.
    public static NSColor colorWithCatalogName(String catalog, String colorName) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hCatalog.invokeExact(ObjC.cls("NSColor"), ObjC.sel("colorWithCatalogName:colorName:"), ObjC.nsstring(catalog), ObjC.nsstring(colorName));
            return (c == null || c.address() == 0) ? null : new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorWithCatalogName:colorName: failed", t);
        }
    }

    // ---- instance API ----

    /// [color setFill] — set as the current fill color (apps need a graphics context).
    public void setFill() {
        ObjC.msgSendVoid(peer, ObjC.sel("setFill"));
    }

    /// [color setStroke] — set as the current stroke color (apps need a graphics context).
    public void setStroke() {
        ObjC.msgSendVoid(peer, ObjC.sel("setStroke"));
    }

    /// [color set] — set as current fill + stroke.
    public void set() {
        ObjC.msgSendVoid(peer, ObjC.sel("set"));
    }

    /// [color alphaComponent] — alpha in 0..1.
    public double alphaComponent() {
        try {
            return (double) hAlpha.invokeExact(peer, ObjC.sel("alphaComponent"));
        } catch (Throwable t) {
            throw new RuntimeException("alphaComponent failed", t);
        }
    }

    /// [color colorWithAlphaComponent:] — same color with different alpha.
    public NSColor colorWithAlphaComponent(double alpha) {
        try {
            MemorySegment c = (MemorySegment) hWithAlpha.invokeExact(peer, ObjC.sel("colorWithAlphaComponent:"), alpha);
            return new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorWithAlphaComponent: failed", t);
        }
    }

    /// [color blendedColorWithFraction:ofColor:] — blend with another color.
    public NSColor blendedColorWithFraction(double fraction, NSColor other) {
        try {
            MemorySegment c = (MemorySegment) hBlended.invokeExact(peer, ObjC.sel("blendedColorWithFraction:ofColor:"), fraction, other.peer());
            return (c == null || c.address() == 0) ? null : new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException("blendedColorWithFraction:ofColor: failed", t);
        }
    }

    /// [color catalogNameComponent] — catalog name, or null for non-catalog colors.
    public String catalogNameComponent() {
        try {
            MemorySegment s = (MemorySegment) hCatalogName.invokeExact(peer, ObjC.sel("catalogNameComponent"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("catalogNameComponent failed", t);
        }
    }

    /// [color colorNameComponent] — color name within its catalog, or null.
    public String colorNameComponent() {
        try {
            MemorySegment s = (MemorySegment) hColorName.invokeExact(peer, ObjC.sel("colorNameComponent"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("colorNameComponent failed", t);
        }
    }

    /// Read the RGBA components back via `getRed:green:blue:alpha:` (four
    /// CGFloat* out-params). Goes through the `ObjC.invokeVoid` escape hatch
    /// (6-object-arg descriptor, NULL-padded) with four 8-byte out-buffers.
    public double[] rgba() {
        MemorySegment b0 = Arena.global().allocate(8);
        MemorySegment b1 = Arena.global().allocate(8);
        MemorySegment b2 = Arena.global().allocate(8);
        MemorySegment b3 = Arena.global().allocate(8);
        ObjC.invokeVoid(peer, ObjC.sel("getRed:green:blue:alpha:"), b0, b1, b2, b3);
        return new double[]{
            b0.get(ValueLayout.JAVA_DOUBLE, 0),
            b1.get(ValueLayout.JAVA_DOUBLE, 0),
            b2.get(ValueLayout.JAVA_DOUBLE, 0),
            b3.get(ValueLayout.JAVA_DOUBLE, 0)
        };
    }

    /// [color description] — the AppKit description string.
    public String description() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("description")));
    }
}
