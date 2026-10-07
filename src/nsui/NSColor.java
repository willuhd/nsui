package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Scratch;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSColor — an AppKit color in the sRGB extended color space. Thin 1:1 wrapper
/// over a native `NSColor`; components are read back with
/// `getRed:green:blue:alpha:`.
///
/// OMITTED (shapes verified missing from Sig.VOCABULARY by grep, or out of
/// scope): colorWithColorSpace:components:count: + getComponents: raw variants
/// beyond the wrapped getComponents (raw CGFloat*); colorWithColorSpace:hue:
/// saturation:brightness:alpha: (ID,ID,DOUBLE,DOUBLE,DOUBLE,DOUBLE);
/// colorWithDeviceCyan:magenta:yellow:black:alpha: (5 doubles);
/// colorWithRed:…exposure:/linearExposure: (macOS 26, 5 doubles);
/// colorWithName:dynamicProvider: (block); colorWithCIColor: (no CIColor
/// wrapper); the deprecated colorSpaceName/colorUsingColorSpaceName:/
/// controlHighlightColor/controlShadowColor/scrollBarColor/knobColor/
/// windowFrameColor/selectedMenuItemColor/headerColor/secondarySelected…
/// aliases + ignoresAlpha. getCyan:… is wrapped (escape hatch) but has no
/// headless CMYK-color factory to exercise it (deviceCMYK shape unregistered).
public final class NSColor extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hCreate, MethodHandle hClassColor, MethodHandle hPattern, MethodHandle hCatalog, MethodHandle hAlpha, MethodHandle hWithAlpha, MethodHandle hBlended, MethodHandle hCatalogName, MethodHandle hColorName, MethodHandle hDouble2, MethodHandle hIdInt, MethodHandle hInt, MethodHandle hVoidRect, MethodHandle hVoidId) {}
    private static volatile Handles H;

    private NSColor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE, Arg.DOUBLE, Arg.DOUBLE, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)));
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
            color = (MemorySegment) H.hCreate().invokeExact(
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
            MemorySegment c = (MemorySegment) H.hClassColor().invokeExact(ObjC.cls("NSColor"), ObjC.sel(sel));
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
    public static NSColor quinaryLabelColor() { return classColor("quinaryLabelColor"); }
    public static NSColor selectedMenuItemTextColor() { return classColor("selectedMenuItemTextColor"); }
    public static NSColor alternateSelectedControlTextColor() { return classColor("alternateSelectedControlTextColor"); }
    public static NSColor underPageBackgroundColor() { return classColor("underPageBackgroundColor"); }
    public static NSColor unemphasizedSelectedContentBackgroundColor() { return classColor("unemphasizedSelectedContentBackgroundColor"); }
    public static NSColor findHighlightColor() { return classColor("findHighlightColor"); }
    public static NSColor textInsertionPointColor() { return classColor("textInsertionPointColor"); }
    public static NSColor selectedTextColor() { return classColor("selectedTextColor"); }
    public static NSColor selectedTextBackgroundColor() { return classColor("selectedTextBackgroundColor"); }
    public static NSColor unemphasizedSelectedTextBackgroundColor() { return classColor("unemphasizedSelectedTextBackgroundColor"); }
    public static NSColor unemphasizedSelectedTextColor() { return classColor("unemphasizedSelectedTextColor"); }
    public static NSColor selectedControlTextColor() { return classColor("selectedControlTextColor"); }
    public static NSColor disabledControlTextColor() { return classColor("disabledControlTextColor"); }
    public static NSColor keyboardFocusIndicatorColor() { return classColor("keyboardFocusIndicatorColor"); }
    public static NSColor scrubberTexturedBackgroundColor() { return classColor("scrubberTexturedBackgroundColor"); }
    public static NSColor systemBrownColor() { return classColor("systemBrownColor"); }
    public static NSColor systemPinkColor() { return classColor("systemPinkColor"); }
    public static NSColor systemTealColor() { return classColor("systemTealColor"); }
    public static NSColor systemIndigoColor() { return classColor("systemIndigoColor"); }
    public static NSColor systemMintColor() { return classColor("systemMintColor"); }
    public static NSColor systemCyanColor() { return classColor("systemCyanColor"); }
    public static NSColor systemFillColor() { return classColor("systemFillColor"); }
    public static NSColor secondarySystemFillColor() { return classColor("secondarySystemFillColor"); }
    public static NSColor tertiarySystemFillColor() { return classColor("tertiarySystemFillColor"); }
    public static NSColor quaternarySystemFillColor() { return classColor("quaternarySystemFillColor"); }
    public static NSColor quinarySystemFillColor() { return classColor("quinarySystemFillColor"); }
    public static NSColor highlightColor() { return classColor("highlightColor"); }
    public static NSColor shadowColor() { return classColor("shadowColor"); }

    /// alternatingContentBackgroundColors — row stripe colors.
    public static NSArray alternatingContentBackgroundColors() {
        ensureInit();
        try {
            MemorySegment a = (MemorySegment) H.hClassColor().invokeExact(ObjC.cls("NSColor"), ObjC.sel("alternatingContentBackgroundColors"));
            return NSArray.wrap(a);
        } catch (Throwable t) {
            throw new RuntimeException("alternatingContentBackgroundColors failed", t);
        }
    }

    private static NSColor colorDouble2(String sel, double v1, double v2) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hDouble2().invokeExact(ObjC.cls("NSColor"), ObjC.sel(sel), v1, v2);
            return new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException(sel + " failed", t);
        }
    }

    private static NSColor colorQuad(String sel, double v1, double v2, double v3, double v4) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hCreate().invokeExact(ObjC.cls("NSColor"), ObjC.sel(sel), v1, v2, v3, v4);
            return new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException(sel + " failed", t);
        }
    }

    /// [+[NSColor colorWithGenericGamma22White:alpha:]].
    public static NSColor colorWithGenericGamma22White(double white, double alpha) {
        return colorDouble2("colorWithGenericGamma22White:alpha:", white, alpha);
    }

    /// [+[NSColor colorWithDisplayP3Red:green:blue:alpha:]].
    public static NSColor colorWithDisplayP3Red(double r, double g, double b, double a) {
        return colorQuad("colorWithDisplayP3Red:green:blue:alpha:", r, g, b, a);
    }

    /// [+[NSColor colorWithWhite:alpha:]] (extended sRGB-compatible).
    public static NSColor colorWithWhite(double white, double alpha) {
        return colorDouble2("colorWithWhite:alpha:", white, alpha);
    }

    /// [+[NSColor colorWithRed:green:blue:alpha:]] (extended sRGB-compatible).
    public static NSColor colorWithRed(double r, double g, double b, double a) {
        return colorQuad("colorWithRed:green:blue:alpha:", r, g, b, a);
    }

    /// [+[NSColor colorWithHue:saturation:brightness:alpha:]].
    public static NSColor colorWithHue(double h, double s, double v, double a) {
        return colorQuad("colorWithHue:saturation:brightness:alpha:", h, s, v, a);
    }

    /// [+[NSColor colorWithDeviceWhite:alpha:]].
    public static NSColor colorWithDeviceWhite(double white, double alpha) {
        return colorDouble2("colorWithDeviceWhite:alpha:", white, alpha);
    }

    /// [+[NSColor colorWithDeviceRed:green:blue:alpha:]].
    public static NSColor colorWithDeviceRed(double r, double g, double b, double a) {
        return colorQuad("colorWithDeviceRed:green:blue:alpha:", r, g, b, a);
    }

    /// [+[NSColor colorWithDeviceHue:saturation:brightness:alpha:]].
    public static NSColor colorWithDeviceHue(double h, double s, double v, double a) {
        return colorQuad("colorWithDeviceHue:saturation:brightness:alpha:", h, s, v, a);
    }

    /// [+[NSColor colorWithCalibratedWhite:alpha:]].
    public static NSColor colorWithCalibratedWhite(double white, double alpha) {
        return colorDouble2("colorWithCalibratedWhite:alpha:", white, alpha);
    }

    /// [+[NSColor colorWithCalibratedRed:green:blue:alpha:]].
    public static NSColor colorWithCalibratedRed(double r, double g, double b, double a) {
        return colorQuad("colorWithCalibratedRed:green:blue:alpha:", r, g, b, a);
    }

    /// [+[NSColor colorWithCalibratedHue:saturation:brightness:alpha:]].
    public static NSColor colorWithCalibratedHue(double h, double s, double v, double a) {
        return colorQuad("colorWithCalibratedHue:saturation:brightness:alpha:", h, s, v, a);
    }

    /// [+[NSColor colorNamed:]] — asset-catalog color in the main bundle (nil-safe).
    public static NSColor colorNamed(String name) {
        ensureInit();
        if (name == null) return null;
        try {
            MemorySegment c = (MemorySegment) H.hPattern().invokeExact(ObjC.cls("NSColor"),
                    ObjC.sel("colorNamed:"), ObjC.nsstring(name));
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorNamed: failed", t);
        }
    }

    /// [+[NSColor colorNamed:bundle:]] — asset-catalog color in `bundle` (nil-safe).
    public static NSColor colorNamedBundle(String name, NSBundle bundle) {
        ensureInit();
        if (name == null) return null;
        try {
            MemorySegment c = (MemorySegment) H.hCatalog().invokeExact(ObjC.cls("NSColor"),
                    ObjC.sel("colorNamed:bundle:"), ObjC.nsstring(name),
                    (MemorySegment) (bundle == null ? MemorySegment.NULL : bundle.peer()));
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorNamed:bundle: failed", t);
        }
    }

    /// [+[NSColor colorWithCGColor:]] — color from a CGColorRef peer (nil-safe).
    public static NSColor colorWithCGColor(MemorySegment cgColor) {
        ensureInit();
        if (cgColor == null || cgColor.address() == 0) return null;
        try {
            MemorySegment c = (MemorySegment) H.hPattern().invokeExact(ObjC.cls("NSColor"),
                    ObjC.sel("colorWithCGColor:"), cgColor);
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorWithCGColor: failed", t);
        }
    }

    /// [+[NSColor colorFromPasteboard:]] — color dragged to the pasteboard (nil-safe).
    public static NSColor colorFromPasteboard(NSPasteboard pasteboard) {
        ensureInit();
        if (pasteboard == null) return null;
        try {
            MemorySegment c = (MemorySegment) H.hPattern().invokeExact(ObjC.cls("NSColor"),
                    ObjC.sel("colorFromPasteboard:"), pasteboard.peer());
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorFromPasteboard: failed", t);
        }
    }

    /// [+[NSColor colorWithPatternImage:]] — pattern color tiled from an image.
    public static NSColor colorWithPatternImage(NSImage image) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hPattern().invokeExact(ObjC.cls("NSColor"), ObjC.sel("colorWithPatternImage:"), image.peer());
            return new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorWithPatternImage: failed", t);
        }
    }

    /// [+[NSColor colorWithCatalogName:colorName:]] — catalog color, or null.
    public static NSColor colorWithCatalogName(String catalog, String colorName) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hCatalog().invokeExact(ObjC.cls("NSColor"), ObjC.sel("colorWithCatalogName:colorName:"), ObjC.nsstring(catalog), ObjC.nsstring(colorName));
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
            return (double) H.hAlpha().invokeExact(peer, ObjC.sel("alphaComponent"));
        } catch (Throwable t) {
            throw new RuntimeException("alphaComponent failed", t);
        }
    }

    /// [color colorWithAlphaComponent:] — same color with different alpha.
    public NSColor colorWithAlphaComponent(double alpha) {
        try {
            MemorySegment c = (MemorySegment) H.hWithAlpha().invokeExact(peer, ObjC.sel("colorWithAlphaComponent:"), alpha);
            return new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorWithAlphaComponent: failed", t);
        }
    }

    /// [color blendedColorWithFraction:ofColor:] — blend with another color.
    public NSColor blendedColorWithFraction(double fraction, NSColor other) {
        try {
            MemorySegment c = (MemorySegment) H.hBlended().invokeExact(peer, ObjC.sel("blendedColorWithFraction:ofColor:"), fraction, other.peer());
            return (c == null || c.address() == 0) ? null : new NSColor(c);
        } catch (Throwable t) {
            throw new RuntimeException("blendedColorWithFraction:ofColor: failed", t);
        }
    }

    /// [color catalogNameComponent] — catalog name, or null for non-catalog colors.
    public String catalogNameComponent() {
        try {
            MemorySegment s = (MemorySegment) H.hCatalogName().invokeExact(peer, ObjC.sel("catalogNameComponent"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("catalogNameComponent failed", t);
        }
    }

    /// [color colorNameComponent] — color name within its catalog, or null.
    public String colorNameComponent() {
        try {
            MemorySegment s = (MemorySegment) H.hColorName().invokeExact(peer, ObjC.sel("colorNameComponent"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("colorNameComponent failed", t);
        }
    }

    /// Component-reader guard: pattern/catalog colors raise natively.
    private void requireRGB() {
        if (type() != 0) throw new IllegalArgumentException("not an RGB-convertible color");
    }

    /// Read the RGBA components back via `getRed:green:blue:alpha:` (four
    /// CGFloat* out-params). Goes through the `ObjC.invokeVoid` escape hatch
    /// (6-object-arg descriptor, NULL-padded) with four 8-byte out-buffers.
    public double[] rgba() {
        requireRGB();
        // out-params synchronously, so call-scoped scratch is exact.
        MemorySegment out = Scratch.allocInput(32);
        MemorySegment b0 = out.asSlice(0, 8);
        MemorySegment b1 = out.asSlice(8, 8);
        MemorySegment b2 = out.asSlice(16, 8);
        MemorySegment b3 = out.asSlice(24, 8);
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

    /// [color CGColor] — the CoreGraphics CGColor backing this color (raw pointer,
    /// autoreleased; suitable for CALayer/CAShapeLayer color properties).
    public MemorySegment cgColor() {
        return ObjC.msgSendId(peer, ObjC.sel("CGColor"));
    }

    /// type — NSColorType (0=component, 1=pattern, 2=catalog).
    public long type() {
        ensureInit();
        try {
            return (long) H.hInt().invokeExact(peer, ObjC.sel("type"));
        } catch (Throwable t) {
            throw new RuntimeException("type failed", t);
        }
    }

    /// colorUsingType: — convert to another NSColorType (nil-safe).
    public NSColor colorUsingType(long type) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hIdInt().invokeExact(peer, ObjC.sel("colorUsingType:"), type);
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorUsingType: failed", t);
        }
    }

    /// colorUsingColorSpace: — convert to the given space (raw NSColorSpace peer,
    /// nil-safe; nil when conversion is impossible).
    public NSColor colorUsingColorSpace(MemorySegment colorSpacePeer) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hPattern().invokeExact(peer, ObjC.sel("colorUsingColorSpace:"),
                    (MemorySegment) (colorSpacePeer == null ? MemorySegment.NULL : colorSpacePeer));
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorUsingColorSpace: failed", t);
        }
    }

    /// colorWithSystemEffect: — base color with a system effect (NSColorSystemEffect).
    public NSColor colorWithSystemEffect(long effect) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hIdInt().invokeExact(peer, ObjC.sel("colorWithSystemEffect:"), effect);
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorWithSystemEffect: failed", t);
        }
    }

    /// highlightWithLevel: — val=0 is receiver, val=1 is highlightColor (nil-safe).
    public NSColor highlightWithLevel(double level) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hWithAlpha().invokeExact(peer, ObjC.sel("highlightWithLevel:"), level);
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("highlightWithLevel: failed", t);
        }
    }

    /// shadowWithLevel: — val=0 is receiver, val=1 is shadowColor (nil-safe).
    public NSColor shadowWithLevel(double level) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hWithAlpha().invokeExact(peer, ObjC.sel("shadowWithLevel:"), level);
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("shadowWithLevel: failed", t);
        }
    }

    /// colorByApplyingContentHeadroom: — reinterpret under a new peak white (HDR).
    public NSColor colorByApplyingContentHeadroom(double headroom) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hWithAlpha().invokeExact(peer,
                    ObjC.sel("colorByApplyingContentHeadroom:"), headroom);
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("colorByApplyingContentHeadroom: failed", t);
        }
    }

    /// standardDynamicRangeColor — the base SDR color (nil-safe).
    public NSColor standardDynamicRangeColor() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hCatalogName().invokeExact(peer, ObjC.sel("standardDynamicRangeColor"));
            return wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("standardDynamicRangeColor failed", t);
        }
    }

    /// linearExposure — HDR brightness multiplier (1.0 for SDR colors).
    public double linearExposure() {
        ensureInit();
        try {
            return (double) H.hAlpha().invokeExact(peer, ObjC.sel("linearExposure"));
        } catch (Throwable t) {
            throw new RuntimeException("linearExposure failed", t);
        }
    }

    private double component(String sel) {
        ensureInit();
        try {
            return (double) H.hAlpha().invokeExact(peer, ObjC.sel(sel));
        } catch (Throwable t) {
            throw new RuntimeException(sel + " failed", t);
        }
    }

    /// redComponent (RGB-model component colors only; raises otherwise).
    public double redComponent() { return component("redComponent"); }
    /// greenComponent (RGB-model component colors only; raises otherwise).
    public double greenComponent() { return component("greenComponent"); }
    /// blueComponent (RGB-model component colors only; raises otherwise).
    public double blueComponent() { return component("blueComponent"); }
    /// hueComponent (RGB-model component colors only; raises otherwise).
    public double hueComponent() { return component("hueComponent"); }
    /// saturationComponent (RGB-model component colors only; raises otherwise).
    public double saturationComponent() { return component("saturationComponent"); }
    /// brightnessComponent (RGB-model component colors only; raises otherwise).
    public double brightnessComponent() { return component("brightnessComponent"); }
    /// whiteComponent (gray-model component colors only; raises otherwise).
    public double whiteComponent() { return component("whiteComponent"); }
    /// cyanComponent (CMYK-model component colors only; raises otherwise).
    public double cyanComponent() { return component("cyanComponent"); }
    /// magentaComponent (CMYK-model component colors only; raises otherwise).
    public double magentaComponent() { return component("magentaComponent"); }
    /// yellowComponent (CMYK-model component colors only; raises otherwise).
    public double yellowComponent() { return component("yellowComponent"); }
    /// blackComponent (CMYK-model component colors only; raises otherwise).
    public double blackComponent() { return component("blackComponent"); }

    /// getHue:saturation:brightness:alpha: via the escape hatch (RGB colors only).
    public double[] hsba() {
        requireRGB();
        MemorySegment out = Scratch.allocInput(32);
        MemorySegment b0 = out.asSlice(0, 8);
        MemorySegment b1 = out.asSlice(8, 8);
        MemorySegment b2 = out.asSlice(16, 8);
        MemorySegment b3 = out.asSlice(24, 8);
        ObjC.invokeVoid(peer, ObjC.sel("getHue:saturation:brightness:alpha:"), b0, b1, b2, b3);
        return new double[]{
            b0.get(ValueLayout.JAVA_DOUBLE, 0),
            b1.get(ValueLayout.JAVA_DOUBLE, 0),
            b2.get(ValueLayout.JAVA_DOUBLE, 0),
            b3.get(ValueLayout.JAVA_DOUBLE, 0)
        };
    }

    /// getWhite:alpha: via the escape hatch (gray-model colors only).
    public double[] whiteAlpha() {
        requireRGB();
        MemorySegment out = Scratch.allocInput(16);
        MemorySegment b0 = out.asSlice(0, 8);
        MemorySegment b1 = out.asSlice(8, 8);
        ObjC.invokeVoid(peer, ObjC.sel("getWhite:alpha:"), b0, b1);
        return new double[]{b0.get(ValueLayout.JAVA_DOUBLE, 0), b1.get(ValueLayout.JAVA_DOUBLE, 0)};
    }

    /// getCyan:magenta:yellow:black:alpha: via the escape hatch (CMYK colors only).
    public double[] cmyka() {
        requireRGB();
        MemorySegment out = Scratch.allocInput(40);
        MemorySegment b0 = out.asSlice(0, 8);
        MemorySegment b1 = out.asSlice(8, 8);
        MemorySegment b2 = out.asSlice(16, 8);
        MemorySegment b3 = out.asSlice(24, 8);
        MemorySegment b4 = out.asSlice(32, 8);
        ObjC.invokeVoid(peer, ObjC.sel("getCyan:magenta:yellow:black:alpha:"), b0, b1, b2, b3, b4);
        return new double[]{
            b0.get(ValueLayout.JAVA_DOUBLE, 0),
            b1.get(ValueLayout.JAVA_DOUBLE, 0),
            b2.get(ValueLayout.JAVA_DOUBLE, 0),
            b3.get(ValueLayout.JAVA_DOUBLE, 0),
            b4.get(ValueLayout.JAVA_DOUBLE, 0)
        };
    }

    /// getComponents: — all floating-point components incl. alpha.
    public double[] getComponents() {
        requireRGB();
        int n = (int) numberOfComponents();
        if (n <= 0) return new double[0];
        MemorySegment out = Scratch.allocInput((long) n * 8L);
        ObjC.invokeVoid(peer, ObjC.sel("getComponents:"), out);
        double[] r = new double[n];
        for (int i = 0; i < n; i++) r[i] = out.get(ValueLayout.JAVA_DOUBLE, (long) i * 8L);
        return r;
    }

    /// numberOfComponents (component colors only; raises otherwise).
    public long numberOfComponents() {
        ensureInit();
        try {
            return (long) H.hInt().invokeExact(peer, ObjC.sel("numberOfComponents"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfComponents failed", t);
        }
    }

    /// colorSpace — raw NSColorSpace peer (nil-safe; no wrapper exists).
    public MemorySegment colorSpacePeer() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hCatalogName().invokeExact(peer, ObjC.sel("colorSpace"));
            return (c == null || c.address() == 0) ? null : c;
        } catch (Throwable t) {
            throw new RuntimeException("colorSpace failed", t);
        }
    }

    /// patternImage — the tiling image (pattern colors only; nil-safe).
    public NSImage patternImage() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hCatalogName().invokeExact(peer, ObjC.sel("patternImage"));
            return NSImage.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("patternImage failed", t);
        }
    }

    /// localizedCatalogNameComponent (catalog colors; null otherwise).
    public String localizedCatalogNameComponent() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hCatalogName().invokeExact(peer, ObjC.sel("localizedCatalogNameComponent"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("localizedCatalogNameComponent failed", t);
        }
    }

    /// localizedColorNameComponent (catalog colors; null otherwise).
    public String localizedColorNameComponent() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hCatalogName().invokeExact(peer, ObjC.sel("localizedColorNameComponent"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("localizedColorNameComponent failed", t);
        }
    }

    /// writeToPasteboard: — publish this color (no-op when pasteboard is null).
    public void writeToPasteboard(NSPasteboard pasteboard) {
        ensureInit();
        if (pasteboard == null) return;
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("writeToPasteboard:"), pasteboard.peer());
        } catch (Throwable t) {
            throw new RuntimeException("writeToPasteboard: failed", t);
        }
    }

    /// drawSwatchInRect: — draw the color swatch (needs a graphics context).
    public void drawSwatchInRect(NSRect rect) {
        ensureInit();
        try {
            H.hVoidRect().invokeExact(peer, ObjC.sel("drawSwatchInRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("drawSwatchInRect: failed", t);
        }
    }
}
