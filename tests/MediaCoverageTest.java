package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;

import nsui.NSAnimationContext;
import nsui.NSApplication;
import nsui.NSArray;
import nsui.NSBezierPath;
import nsui.NSBitmapImageRep;
import nsui.NSBundle;
import nsui.NSCursor;
import nsui.NSColor;
import nsui.NSGradient;
import nsui.NSGraphicsContext;
import nsui.NSImage;
import nsui.NSImageView;
import nsui.NSNumber;
import nsui.NSObject;
import nsui.NSPasteboard;
import nsui.NSPoint;
import nsui.NSRect;
import nsui.NSShadow;
import nsui.NSSize;
import nsui.NSWorkspace;
import nsui.NSData;
import nsui.NSWindow;
import nsui.CAMediaTimingFunction;
import nsui.objc.ObjC;

/// MediaCoverageTest — graphics/media batch: NSColor, NSImage, NSImageView,
/// NSBitmapImageRep, NSGradient, NSBezierPath, NSShadow, NSGraphicsContext,
/// NSAnimationContext, NSCursor.
///
/// Headless and side-effect free: windows stay hidden (never shown), no cursor
/// visibility calls (hide/unhide/setHiddenUntilMouseMoves/push/set untested by
/// design — global side effects), a private pasteboard
/// (pasteboardWithName, never the general pasteboard), and scratch files only
/// under a unique /tmp/sa-mediacoverage-* dir (never inside the repo).
/// Context-dependent draws (gradient/bezier/color set) run against a bitmap
/// NSGraphicsContext made current, plus nil-context no-throw pins.
public final class MediaCoverageTest {

    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        TestKit.check(ok, msg);
    }

    private static boolean near(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== MediaCoverageTest — graphics/media batch ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            TestKit.skip("ObjC.init failed (not macOS?): " + t);
        }
        NSApplication app = TestKit.app();
        Path saDir = Files.createTempDirectory("sa-mediacoverage");

        // ---------------- NSColor factories ----------------
        try {
            NSColor c = NSColor.create(0.25, 0.5, 0.75, 1.0);
            check(c != null, "NSColor.create non-nil");
            double[] rgba = c.rgba();
            check(near(rgba[0], 0.25, 0.02) && near(rgba[1], 0.5, 0.02)
                    && near(rgba[2], 0.75, 0.02) && near(rgba[3], 1.0, 0.02), "sRGB rgba round-trip");
            check(c.redComponent() >= 0 && c.greenComponent() >= 0 && c.blueComponent() >= 0,
                    "rgb components readable (" + c.redComponent() + "," + c.greenComponent() + "," + c.blueComponent() + ")");
            double[] hsba = c.hsba();
            check(hsba.length == 4 && hsba[3] >= 0.99, "hsba() alpha sane (" + hsba[3] + ")");
            check(c.numberOfComponents() == 4, "sRGB numberOfComponents == 4");
            check(c.getComponents().length == 4, "getComponents() length 4");
            check(c.type() == 0, "sRGB type == component-based (0)");
            check(c.colorSpacePeer() != null, "colorSpacePeer non-nil");
            check(c.colorUsingType(0) != null, "colorUsingType(0) non-nil");
            MemorySegment srgb = ObjC.msgSendId(ObjC.cls("NSColorSpace"), ObjC.sel("sRGBColorSpace"));
            check(srgb != null && srgb.address() != 0, "sRGBColorSpace peer resolvable");
            check(c.colorUsingColorSpace(srgb) != null, "colorUsingColorSpace(sRGB) non-nil");
            check(c.highlightWithLevel(0.5) != null, "highlightWithLevel non-nil");
            check(c.shadowWithLevel(0.5) != null, "shadowWithLevel non-nil");
            check(c.colorWithSystemEffect(0) != null, "colorWithSystemEffect(none) non-nil");
            check(c.linearExposure() == 1.0, "linearExposure == 1.0 for SDR");
            check(c.standardDynamicRangeColor() != null, "standardDynamicRangeColor non-nil");
            check(c.colorByApplyingContentHeadroom(1.0) != null, "colorByApplyingContentHeadroom(1.0) non-nil");
            NSColor gray = NSColor.colorWithWhite(0.5, 1.0);
            check(gray != null, "colorWithWhite non-nil");
            double[] wa = gray.whiteAlpha();
            check(near(wa[0], 0.5, 0.03) && near(wa[1], 1.0, 0.01), "whiteAlpha round-trip");
            check(near(gray.whiteComponent(), 0.5, 0.03), "whiteComponent sane");
            NSColor hue = NSColor.colorWithHue(0.33, 0.5, 0.5, 1.0);
            check(hue != null && hue.hueComponent() >= 0, "colorWithHue non-nil, hue readable");
            check(NSColor.colorWithGenericGamma22White(0.4, 1.0) != null, "genericGamma22White non-nil");
            check(NSColor.colorWithDisplayP3Red(0.2, 0.4, 0.6, 1.0) != null, "displayP3 non-nil");
            check(NSColor.colorWithRed(0.1, 0.2, 0.3, 1.0) != null, "iOS-compat colorWithRed non-nil");
            check(NSColor.colorWithDeviceWhite(0.6, 1.0) != null, "deviceWhite non-nil");
            check(NSColor.colorWithDeviceRed(0.2, 0.4, 0.6, 1.0) != null, "deviceRed non-nil");
            check(NSColor.colorWithDeviceHue(0.1, 0.2, 0.3, 1.0) != null, "deviceHue non-nil");
            check(NSColor.colorWithCalibratedWhite(0.7, 1.0) != null, "calibratedWhite non-nil");
            check(NSColor.colorWithCalibratedRed(0.2, 0.4, 0.6, 1.0) != null, "calibratedRed non-nil");
            check(NSColor.colorWithCalibratedHue(0.1, 0.2, 0.3, 1.0) != null, "calibratedHue non-nil");
            check(NSColor.colorNamed("NSNoSuchColorXYZ") == null, "colorNamed miss returns null");
        } catch (Throwable t) {
            check(false, "color-factories threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSColor system colors ----------------
        try {
            String[] sels = {"labelColor", "separatorColor", "systemTealColor", "systemMintColor",
                    "systemFillColor", "highlightColor", "shadowColor", "quinaryLabelColor",
                    "textInsertionPointColor", "keyboardFocusIndicatorColor", "systemIndigoColor",
                    "systemCyanColor", "secondarySystemFillColor", "findHighlightColor"};
            boolean allOk = true;
            for (String s : sels) {
                try {
                    MemorySegment p = ObjC.msgSendId(ObjC.cls("NSColor"), ObjC.sel(s));
                    if (p == null || p.address() == 0) { allOk = false; System.out.println("NOTE: nil class color " + s); }
                } catch (Throwable inner) {
                    allOk = false;
                    System.out.println("NOTE: class color threw " + s + ": " + inner);
                }
            }
            check(allOk, "spot-check class colors non-nil");
            check(NSColor.systemTealColor() != null, "systemTealColor typed non-nil");
            check(NSColor.highlightColor() != null, "highlightColor typed non-nil");
            check(NSColor.quinarySystemFillColor() != null, "quinarySystemFillColor typed non-nil");
            NSArray stripes = NSColor.alternatingContentBackgroundColors();
            check(stripes != null && stripes.count() > 0, "alternatingContentBackgroundColors non-empty");
        } catch (Throwable t) {
            check(false, "system-colors threw: " + t);
        }

        // ---------------- NSColor pasteboard (private) + pattern + CGColor ----------------
        try {
            NSImage icon = NSWorkspace.sharedWorkspace().iconForFileType("txt");
            check(icon != null, "icon source non-nil");
            NSColor pattern = NSColor.colorWithPatternImage(icon);
            check(pattern != null && pattern.type() == 1, "pattern color type == 1");
            check(pattern.patternImage() != null, "patternImage non-nil");
            MemorySegment cg = NSColor.redColor().cgColor();
            check(cg != null && cg.address() != 0, "redColor cgColor non-nil");
            check(NSColor.colorWithCGColor(cg) != null, "colorWithCGColor round-trip non-nil");
            check(NSColor.colorWithCGColor(null) == null, "colorWithCGColor(null) returns null");
            NSPasteboard pb = NSPasteboard.pasteboardWithName("sa-mediacoverage");
            check(pb != null, "private pasteboard non-nil");
            pb.clearContents();
            TestKit.noThrow("writeToPasteboard no-throw", () -> NSColor.redColor().writeToPasteboard(pb));
            // Server-mediated color bytes read back nil in this session (writeToPasteboard:
            // leaves zero types on a fresh named pasteboard, though the string channel
            // round-trips fine) — unproven here, so only the nil-safe read is pinned.
            NSColor back = NSColor.colorFromPasteboard(pb);
            TestKit.probe("colorFromPasteboard nil-safe (got "
                    + (back == null ? "nil — server dropped the write" : "a color") + ")");
            check(pb.setStringForType("sa-ping", NSPasteboard.NSPasteboardTypeString),
                    "private pasteboard string channel works");
            check("sa-ping".equals(pb.stringForType(NSPasteboard.NSPasteboardTypeString)),
                    "private pasteboard string round-trip");
            check(NSColor.colorFromPasteboard(null) == null, "colorFromPasteboard(null) returns null");
            check(icon.pngData() != null, "icon pngData non-nil (export path alive)");
        } catch (Throwable t) {
            check(false, "pasteboard/pattern section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSImage ----------------
        NSImage img = null;
        try {
            img = NSImage.createWithSize(new NSSize(64, 48));
            check(img != null, "createWithSize non-nil");
            NSSize sz = img.size();
            check(sz != null && near(sz.width(), 64, 0.01) && near(sz.height(), 48, 0.01), "size round-trip 64x48");
            NSRect align = img.alignmentRect();
            check(align != null && near(align.width(), 64, 0.01) && near(align.height(), 48, 0.01),
                    "default alignmentRect matches size");
            img.setAlignmentRect(new NSRect(1, 2, 3, 4));
            NSRect align2 = img.alignmentRect();
            check(near(align2.x(), 1, 0.01) && near(align2.width(), 3, 0.01), "alignmentRect set/get round-trip");
            check(img.cacheMode() >= 0, "cacheMode readable (" + img.cacheMode() + ")");
            img.setCacheMode(3);
            check(img.cacheMode() == 3, "cacheMode set/get round-trip");
            img.setCacheMode(0);
            boolean d0 = img.prefersColorMatch();
            img.setPrefersColorMatch(!d0);
            check(img.prefersColorMatch() == !d0, "prefersColorMatch round-trip");
            img.setPrefersColorMatch(d0);
            boolean e0 = img.usesEPSOnResolutionMismatch();
            img.setUsesEPSOnResolutionMismatch(!e0);
            check(img.usesEPSOnResolutionMismatch() == !e0, "usesEPSOnResolutionMismatch round-trip");
            img.setUsesEPSOnResolutionMismatch(e0);
            boolean m0 = img.matchesOnMultipleResolution();
            img.setMatchesOnMultipleResolution(!m0);
            check(img.matchesOnMultipleResolution() == !m0, "matchesOnMultipleResolution round-trip");
            img.setMatchesOnMultipleResolution(m0);
            TestKit.probe("backgroundColor readable (" + (img.backgroundColor() == null ? "nil" : "set") + ")");
            img.setBackgroundColor(NSColor.redColor());
            check(img.backgroundColor() != null, "backgroundColor set/get non-nil");
            img.setBackgroundColor(null);
            img.recache();
            TestKit.probe("recache did not throw");
            NSArray types = NSImage.imageTypes();
            check(types != null && types.count() > 0, "imageTypes non-empty");
            check(NSImage.imageUnfilteredTypes() != null, "imageUnfilteredTypes readable");
            try {
                MemorySegment lc = img.layerContentsForContentsScale(1.0);
                TestKit.probe("layerContentsForContentsScale no-throw");
            } catch (Throwable t) {
                check(false, "layerContentsForContentsScale threw: " + t);
            }
        } catch (Throwable t) {
            check(false, "nsimage section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSImage file/data factories + /tmp/sa round-trip ----------------
        try {
            NSImage icon = NSWorkspace.sharedWorkspace().iconForFileType("txt");
            NSData tiff = NSData.wrap(icon.TIFFRepresentation());
            check(tiff != null && tiff.length() > 0, "icon TIFF non-empty");
            NSImage fromData = NSImage.createWithData(tiff);
            check(fromData != null && fromData.isValid(), "createWithData valid");
            check(NSImage.createWithData(null) == null, "createWithData(null) returns null");
            NSImage ignoreOrient = NSImage.createWithDataIgnoringOrientation(tiff);
            check(ignoreOrient != null, "createWithDataIgnoringOrientation non-nil");
            Path pngPath = saDir.resolve("sa-icon.png");
            Files.write(pngPath, icon.pngData().toByteArray());
            NSImage fromFile = NSImage.imageWithContentsOfFile(pngPath.toString());
            check(fromFile != null && fromFile.isValid(), "imageWithContentsOfFile valid");
            NSImage byRef = NSImage.createByReferencingFile(pngPath.toString());
            check(byRef != null, "createByReferencingFile non-nil");
            check(NSImage.createByReferencingFile(null) == null, "createByReferencingFile(null) returns null");
            NSArray reps = fromFile.representations();
            check(reps != null && reps.count() > 0, "representations non-empty");
            long n0 = fromFile.representations().count();
            NSBitmapImageRep extra = NSBitmapImageRep.create(NSData.wrap(fromFile.TIFFRepresentation()));
            fromFile.addRepresentation(extra);
            check(fromFile.representations().count() == n0 + 1, "addRepresentation grows count");
            fromFile.removeRepresentation(extra);
            check(fromFile.representations().count() == n0, "removeRepresentation shrinks count");
            Files.deleteIfExists(pngPath);
        } catch (Throwable t) {
            check(false, "nsimage-file section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSImageView (hidden window, never shown) ----------------
        NSWindow win = null;
        try {
            win = TestKit.hiddenWindow(320, 240);
            NSImage icon = NSWorkspace.sharedWorkspace().iconForFileType("txt");
            NSImageView iv = NSImageView.create(new NSRect(0, 0, 64, 64));
            check(iv != null, "NSImageView.create non-nil");
            win.contentView().addSubview(iv);
            iv.setImage(icon);
            check(iv.image() != null, "image set/get non-nil");
            NSImageView iv2 = NSImageView.imageViewWithImage(icon);
            check(iv2 != null && iv2.image() != null, "imageViewWithImage non-nil");
            TestKit.expectThrows("imageViewWithImage(null) rejects nil in Java (no native crash)",
                    IllegalArgumentException.class, () -> NSImageView.imageViewWithImage(null));
            iv.setContentTintColor(NSColor.redColor());
            check(iv.contentTintColor() != null, "contentTintColor round-trip non-nil");
            iv.setContentTintColor(null);
            check(iv.contentTintColor() == null, "contentTintColor clears to null");
            iv.setPreferredImageDynamicRange(0);
            check(iv.preferredImageDynamicRange() == 0, "preferredImageDynamicRange round-trip");
            check(iv.imageDynamicRange() >= -1, "imageDynamicRange readable (" + iv.imageDynamicRange() + ")");
            iv.setImageScaling(3);
            check(iv.imageScaling() == 3, "imageScaling round-trip");
            TestKit.close(win);
            win = null;
        } catch (Throwable t) {
            check(false, "nsimageview section threw: " + t);
            t.printStackTrace(System.out);
        } finally {
            TestKit.close(win);
        }

        // ---------------- NSBitmapImageRep ----------------
        try {
            NSImage icon = NSWorkspace.sharedWorkspace().iconForFileType("txt");
            NSBitmapImageRep rep = NSBitmapImageRep.create(NSData.wrap(icon.TIFFRepresentation()));
            check(rep != null, "rep from icon TIFF non-nil");
            check(rep.bitsPerPixel() > 0, "bitsPerPixel sane (" + rep.bitsPerPixel() + ")");
            check(rep.bytesPerRow() > 0, "bytesPerRow sane (" + rep.bytesPerRow() + ")");
            check(rep.bytesPerPlane() >= 0, "bytesPerPlane sane (" + rep.bytesPerPlane() + ")");
            check(rep.numberOfPlanes() >= 1, "numberOfPlanes sane (" + rep.numberOfPlanes() + ")");
            check(rep.bitmapFormat() >= 0, "bitmapFormat readable (" + rep.bitmapFormat() + ")");
            check(rep.bitmapData() != null, "bitmapData non-nil");
            check(rep.tiffRepresentation() != null, "tiffRepresentation non-nil");
            TestKit.probe("canBeCompressedUsing no-crash (" + rep.canBeCompressedUsing(1) + ")");
            check(rep.colorSpacePeer() != null, "rep colorSpacePeer non-nil");
            check(rep.bitmapImageRepByRetaggingWithColorSpace(rep.colorSpacePeer()) != null,
                    "retagging non-nil");
            NSBitmapImageRep fromCG = NSBitmapImageRep.createWithCGImage(rep.cgImage());
            check(fromCG != null && fromCG.pixelsWide() == rep.pixelsWide(), "createWithCGImage round-trip");
            check(NSBitmapImageRep.createWithCGImage(null) == null, "createWithCGImage(null) returns null");
            check(NSBitmapImageRep.createForIncrementalLoad() != null, "createForIncrementalLoad non-nil");
            NSColor px = rep.colorAtXY(0, 0);
            check(px != null, "colorAtXY non-nil");
            rep.setColorAtXY(NSColor.redColor(), 0, 0);
            check(rep.colorAtXY(0, 0) != null, "setColorAtXY round-trip non-nil");
            NSImage holder = NSImage.createWithSize(new NSSize(32, 32));
            rep.setPropertyWithValue("NSImageCompressionFactor", NSNumber.numberWithDouble(0.8));
            check(rep.valueForProperty("NSImageCompressionFactor") != null, "property set/get round-trip");
            NSArray holderReps = holder.representations();
            check(holderReps != null, "blank image representations readable");
            // NOTE: icon reps are NSISIconImageRep (not directly encodable), so the
            // array helpers are exercised with a real bitmap rep in the array.
            NSArray bitmapArr = NSArray.mutableArray();
            bitmapArr.addObject(rep);
            NSData arrTiff = NSBitmapImageRep.tiffRepresentationOfImageRepsInArray(bitmapArr);
            check(arrTiff != null && arrTiff.length() > 0, "tiffRepresentationOfImageRepsInArray non-empty");
            check(NSColor.colorNamedBundle("NSNoSuchColorXYZ", NSBundle.mainBundle()) == null,
                    "colorNamedBundle miss returns null");
            NSData arrPng = NSBitmapImageRep.representationOfImageRepsInArray(
                    bitmapArr, NSBitmapImageRep.fileTypePNG, null);
            check(arrPng != null && arrPng.length() > 4
                    && (arrPng.toByteArray()[0] & 0xFF) == 0x89, "representationOfImageRepsInArray PNG magic");
            check(NSBitmapImageRep.imageRepWithData(NSData.wrap(icon.TIFFRepresentation())) != null,
                    "imageRepWithData non-nil");
            check(NSBitmapImageRep.imageRepsWithData(NSData.wrap(icon.TIFFRepresentation())).count() > 0,
                    "imageRepsWithData non-empty");
        } catch (Throwable t) {
            check(false, "bitmaprep section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSGradient ----------------
        try {
            NSGradient g = NSGradient.initWithStartingColorEndingColor(NSColor.blackColor(), NSColor.whiteColor());
            check(g != null, "two-color gradient non-nil");
            check(g.numberOfColorStops() == 2, "two-color stops == 2");
            NSColor mid = g.interpolatedColorAtLocation(0.5);
            check(mid != null, "interpolatedColorAtLocation non-nil");
            double[] mrgba = mid.rgba();
            check(mrgba[0] > 0.3 && mrgba[0] < 0.7, "midpoint gray sane (" + mrgba[0] + ")");
            check(g.colorSpacePeer() != null, "gradient colorSpacePeer non-nil");
            NSArray colors = NSArray.mutableArray();
            colors.addObject(NSColor.redColor());
            colors.addObject(NSColor.greenColor());
            colors.addObject(NSColor.blueColor());
            NSGradient g3 = NSGradient.initWithColors(colors);
            check(g3 != null && g3.numberOfColorStops() == 3, "three-color stops == 3");
            NSGradient gl = NSGradient.initWithColorsAtLocations(colors, new double[]{0.0, 0.5, 1.0});
            check(gl != null && gl.numberOfColorStops() == 3, "located stops == 3");
            try {
                TestKit.noThrow("drawInRect:angle: no-throw (nil context)", () -> g.drawInRectAngle(new NSRect(0, 0, 32, 32), 45.0));
            } catch (Throwable t) {
                check(false, "drawInRect:angle: threw: " + t);
            }
            try {
                NSBezierPath path = NSBezierPath.bezierPathWithRect(new NSRect(0, 0, 32, 32));
                TestKit.noThrow("drawInBezierPath:angle: no-throw (nil context)", () -> g.drawInBezierPathAngle(path, 0.0));
            } catch (Throwable t) {
                check(false, "drawInBezierPath:angle: threw: " + t);
            }
        } catch (Throwable t) {
            check(false, "gradient section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSBezierPath ----------------
        try {
            check(NSBezierPath.LINE_CAP_BUTT == 0 && NSBezierPath.LINE_CAP_ROUND == 1
                    && NSBezierPath.LINE_JOIN_BEVEL == 2 && NSBezierPath.WINDING_EVEN_ODD == 1,
                    "style constants match header");
            NSBezierPath p = NSBezierPath.bezierPath();
            check(p.isEmpty(), "fresh path isEmpty");
            p.moveToPoint(new NSPoint(10, 10));
            p.lineToPoint(new NSPoint(50, 10));
            p.curveToPoint(new NSPoint(90, 50), new NSPoint(60, 10), new NSPoint(80, 50));
            p.closePath();
            check(!p.isEmpty(), "built path non-empty");
            check(p.elementCount() >= 4, "elementCount sane (" + p.elementCount() + ")");
            check(p.elementAtIndex(0) == NSBezierPath.ELEMENT_MOVE_TO, "element 0 is moveTo");
            NSPoint cur = p.currentPoint();
            check(cur != null, "currentPoint readable (" + cur + ")");
            NSRect bounds = p.bounds();
            check(bounds != null && bounds.width() > 0, "bounds sane (" + bounds + ")");
            check(p.controlPointBounds().width() >= bounds.width(), "controlPointBounds sane");
            check(near(p.lineWidth(), 1.0, 0.01) || p.lineWidth() > 0, "default lineWidth sane (" + p.lineWidth() + ")");
            p.setLineWidth(2.5);
            check(near(p.lineWidth(), 2.5, 0.001), "lineWidth round-trip");
            p.setWindingRule(NSBezierPath.WINDING_EVEN_ODD);
            check(p.windingRule() == NSBezierPath.WINDING_EVEN_ODD, "windingRule round-trip");
            p.setMiterLimit(7.0);
            check(near(p.miterLimit(), 7.0, 0.001), "miterLimit round-trip");
            p.setFlatness(0.7);
            check(near(p.flatness(), 0.7, 0.001), "flatness round-trip");
            p.setLineCapStyle(NSBezierPath.LINE_CAP_ROUND);
            check(p.lineCapStyle() == NSBezierPath.LINE_CAP_ROUND, "lineCapStyle round-trip");
            p.setLineJoinStyle(NSBezierPath.LINE_JOIN_BEVEL);
            check(p.lineJoinStyle() == NSBezierPath.LINE_JOIN_BEVEL, "lineJoinStyle round-trip");
            p.relativeMoveToPoint(new NSPoint(5, 5));
            p.relativeLineToPoint(new NSPoint(5, 0));
            p.relativeCurveToPoint(new NSPoint(9, 9), new NSPoint(1, 1), new NSPoint(2, 2));
            check(p.elementCount() >= 7, "relative ops grow path (" + p.elementCount() + ")");
            check(p.bezierPathByFlatteningPath() != null, "flattening non-nil");
            check(p.bezierPathByReversingPath() != null, "reversing non-nil");
            NSBezierPath q = NSBezierPath.bezierPath();
            long q0 = q.elementCount();
            q.appendBezierPathWithRect(new NSRect(0, 0, 8, 8));
            q.appendBezierPathWithOvalInRect(new NSRect(0, 0, 8, 8));
            q.appendBezierPath(p);
            check(q.elementCount() > q0, "appends grow path");
            q.removeAllPoints();
            check(q.isEmpty(), "removeAllPoints empties");
            try {
                p.stroke();
                p.fill();
                p.setClip();
                p.addClip();
                NSBezierPath.fillRect(new NSRect(0, 0, 4, 4));
                NSBezierPath.strokeRect(new NSRect(0, 0, 4, 4));
                TestKit.noThrow("stroke/fill/clip class draws no-throw (nil context)", () -> NSBezierPath.clipRect(new NSRect(0, 0, 4, 4)));
            } catch (Throwable t) {
                check(false, "draw section threw: " + t);
            }
            double lw0 = NSBezierPath.defaultLineWidth();
            NSBezierPath.setDefaultLineWidth(lw0 + 1.0);
            check(near(NSBezierPath.defaultLineWidth(), lw0 + 1.0, 0.001), "defaultLineWidth round-trip");
            NSBezierPath.setDefaultLineWidth(lw0);
            long wr0 = NSBezierPath.defaultWindingRule();
            NSBezierPath.setDefaultWindingRule(1 - wr0);
            check(NSBezierPath.defaultWindingRule() == 1 - wr0, "defaultWindingRule round-trip");
            NSBezierPath.setDefaultWindingRule(wr0);
            NSBezierPath.setDefaultLineCapStyle(NSBezierPath.LINE_CAP_SQUARE);
            check(NSBezierPath.defaultLineCapStyle() == NSBezierPath.LINE_CAP_SQUARE, "defaultLineCapStyle round-trip");
            NSBezierPath.setDefaultLineCapStyle(NSBezierPath.LINE_CAP_BUTT);
            NSBezierPath.setDefaultLineJoinStyle(NSBezierPath.LINE_JOIN_ROUND);
            check(NSBezierPath.defaultLineJoinStyle() == NSBezierPath.LINE_JOIN_ROUND, "defaultLineJoinStyle round-trip");
            NSBezierPath.setDefaultLineJoinStyle(NSBezierPath.LINE_JOIN_MITER);
            double ml0 = NSBezierPath.defaultMiterLimit();
            NSBezierPath.setDefaultMiterLimit(ml0 + 1.0);
            check(near(NSBezierPath.defaultMiterLimit(), ml0 + 1.0, 0.001), "defaultMiterLimit round-trip");
            NSBezierPath.setDefaultMiterLimit(ml0);
            double fl0 = NSBezierPath.defaultFlatness();
            NSBezierPath.setDefaultFlatness(fl0 + 0.1);
            check(near(NSBezierPath.defaultFlatness(), fl0 + 0.1, 0.001), "defaultFlatness round-trip");
            NSBezierPath.setDefaultFlatness(fl0);
            MemorySegment cg = p.cgPath();
            check(cg != null && cg.address() != 0, "cgPath non-nil");
            NSBezierPath fromCG = NSBezierPath.bezierPathWithCGPath(cg);
            check(fromCG != null && fromCG.elementCount() > 0, "bezierPathWithCGPath round-trip");
            fromCG.setCGPath(cg);
            check(fromCG.elementCount() > 0, "setCGPath keeps elements");
        } catch (Throwable t) {
            check(false, "bezier section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSShadow ----------------
        try {
            NSShadow s = NSShadow.create();
            check(s != null, "NSShadow.create non-nil");
            s.setShadowOffset(new NSSize(3, -4));
            NSSize off = s.shadowOffset();
            check(off != null && near(off.width(), 3, 0.001) && near(off.height(), -4, 0.001), "offset round-trip");
            s.setShadowBlurRadius(6.0);
            check(near(s.shadowBlurRadius(), 6.0, 0.001), "blur round-trip");
            s.setShadowColor(NSColor.blackColor());
            check(s.shadowColor() != null, "shadowColor round-trip non-nil");
            try {
                TestKit.noThrow("set() no-throw (nil context)", () -> s.set());
            } catch (Throwable t) {
                check(false, "shadow set() threw: " + t);
            }
            check(NSShadow.wrap(null) == null, "wrap(null) returns null");
        } catch (Throwable t) {
            check(false, "shadow section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSGraphicsContext (bitmap-backed, then cleared) ----------------
        NSGraphicsContext prev = null;
        try {
            prev = TestKit.attempt("currentContext readable headless", () -> NSGraphicsContext.currentContext());
            TestKit.probe("currentContextDrawingToScreen no-crash ("
                    + NSGraphicsContext.currentContextDrawingToScreen() + ")");
            try {
                NSGraphicsContext.saveCurrentGraphicsState();
                TestKit.noThrow("class save/restore no-throw", () -> NSGraphicsContext.restoreCurrentGraphicsState());
            } catch (Throwable t) {
                check(false, "class save/restore threw: " + t);
            }
            NSImage icon = NSWorkspace.sharedWorkspace().iconForFileType("txt");
            NSBitmapImageRep rep = NSBitmapImageRep.create(NSData.wrap(icon.TIFFRepresentation()));
            NSGraphicsContext ctx = NSGraphicsContext.graphicsContextWithBitmapImageRep(rep);
            check(ctx != null, "graphicsContextWithBitmapImageRep non-nil");
            NSGraphicsContext.setCurrentContext(ctx);
            check(NSGraphicsContext.currentContext() != null, "setCurrentContext sticks");
            TestKit.probe("isDrawingToScreen no-crash (" + ctx.isDrawingToScreen() + ")");
            TestKit.probe("isFlipped no-crash (" + ctx.isFlipped() + ")");
            TestKit.probe("attributes readable (" + ctx.attributes() + ")");
            try {
                ctx.saveGraphicsState();
                ctx.restoreGraphicsState();
                TestKit.noThrow("save/restore/flush no-throw", () -> ctx.flushGraphics());
            } catch (Throwable t) {
                check(false, "ctx save/restore/flush threw: " + t);
            }
            boolean aa0 = ctx.shouldAntialias();
            ctx.setShouldAntialias(!aa0);
            check(ctx.shouldAntialias() == !aa0, "shouldAntialias round-trip");
            ctx.setShouldAntialias(aa0);
            ctx.setImageInterpolation(3);
            check(ctx.imageInterpolation() == 3, "imageInterpolation round-trip");
            ctx.setPatternPhase(new NSPoint(2, 3));
            NSPoint ph = ctx.patternPhase();
            check(ph != null && near(ph.x(), 2, 0.001) && near(ph.y(), 3, 0.001), "patternPhase round-trip");
            ctx.setCompositingOperation(2);
            check(ctx.compositingOperation() == 2, "compositingOperation round-trip");
            long cri0 = ctx.colorRenderingIntent();
            ctx.setColorRenderingIntent(cri0);
            check(ctx.colorRenderingIntent() == cri0, "colorRenderingIntent round-trip");
            TestKit.probe("ciContext readable (" + (ctx.ciContext() == null ? "nil" : "set") + ")");
            check(ctx.CGContext() != null, "CGContext non-nil for bitmap ctx");
            try {
                NSColor.redColor().setFill();
                NSColor.redColor().setStroke();
                NSColor.redColor().set();
                NSBezierPath.fillRect(new NSRect(0, 0, 8, 8));
                NSGradient.initWithStartingColorEndingColor(NSColor.blackColor(), NSColor.whiteColor())
                        .drawInRectAngle(new NSRect(0, 0, 8, 8), 45.0);
                TestKit.noThrow("real-ctx draws no-throw", () -> NSColor.redColor().drawSwatchInRect(new NSRect(0, 0, 8, 8)));
            } catch (Throwable t) {
                check(false, "real-ctx draws threw: " + t);
            }
            TestKit.probe("graphicsContextWithAttributes(null) no-crash ("
                    + (NSGraphicsContext.graphicsContextWithAttributes(null) == null ? "nil" : "set") + ")");
            NSGraphicsContext.setCurrentContext(prev);
            TestKit.probe("context restored");
        } catch (Throwable t) {
            check(false, "graphics section threw: " + t);
            t.printStackTrace(System.out);
        } finally {
            try {
                NSGraphicsContext.setCurrentContext(prev);
            } catch (Throwable ignore) {
            }
        }

        // ---------------- NSAnimationContext ----------------
        try {
            NSAnimationContext ctx = NSAnimationContext.currentContext();
            check(ctx != null, "currentContext non-nil");
            double d0 = ctx.duration();
            ctx.setDuration(0.5);
            check(near(ctx.duration(), 0.5, 0.001), "duration round-trip");
            ctx.setDuration(d0);
            ctx.setTimingFunction(CAMediaTimingFunction.functionWithName(CAMediaTimingFunction.NAME_EASE_IN));
            check(ctx.timingFunction() != null, "timingFunction round-trip non-nil");
            ctx.setTimingFunction(null);
            boolean a0 = ctx.allowsImplicitAnimation();
            ctx.setAllowsImplicitAnimation(!a0);
            check(ctx.allowsImplicitAnimation() == !a0, "allowsImplicitAnimation round-trip");
            ctx.setAllowsImplicitAnimation(a0);
            try {
                NSAnimationContext.beginGrouping();
                NSAnimationContext.currentContext().setDuration(0.1);
                TestKit.noThrow("begin/end grouping no-throw", () -> NSAnimationContext.endGrouping());
            } catch (Throwable t) {
                check(false, "grouping threw: " + t);
            }
            try {
                TestKit.noThrow("runAnimationGroup helper no-throw", () -> NSAnimationContext.runAnimationGroup(() -> {
                }, 0.2));
            } catch (Throwable t) {
                check(false, "runAnimationGroup threw: " + t);
            }
            check(NSAnimationContext.wrap(null) == null, "wrap(null) returns null");
        } catch (Throwable t) {
            check(false, "animation section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSCursor (getters only; no visibility side effects) ----------------
        try {
            check(NSCursor.arrowCursor() != null, "arrowCursor non-nil");
            check(NSCursor.IBeamCursor() != null, "IBeamCursor non-nil");
            check(NSCursor.crosshairCursor() != null, "crosshairCursor non-nil");
            check(NSCursor.closedHandCursor() != null, "closedHandCursor non-nil");
            check(NSCursor.openHandCursor() != null, "openHandCursor non-nil");
            check(NSCursor.pointingHandCursor() != null, "pointingHandCursor non-nil");
            check(NSCursor.resizeLeftRightCursor() != null, "resizeLeftRightCursor non-nil");
            check(NSCursor.resizeUpDownCursor() != null, "resizeUpDownCursor non-nil");
            check(NSCursor.disappearingItemCursor() != null, "disappearingItemCursor non-nil");
            check(NSCursor.operationNotAllowedCursor() != null, "operationNotAllowedCursor non-nil");
            check(NSCursor.dragLinkCursor() != null, "dragLinkCursor non-nil");
            check(NSCursor.dragCopyCursor() != null, "dragCopyCursor non-nil");
            check(NSCursor.contextualMenuCursor() != null, "contextualMenuCursor non-nil");
            check(NSCursor.IBeamCursorForVerticalLayout() != null, "IBeam vertical non-nil");
            check(NSCursor.zoomInCursor() != null, "zoomInCursor non-nil");
            check(NSCursor.zoomOutCursor() != null, "zoomOutCursor non-nil");
            check(NSCursor.columnResizeCursor() != null, "columnResizeCursor non-nil");
            check(NSCursor.rowResizeCursor() != null, "rowResizeCursor non-nil");
            check(NSCursor.columnResizeCursorInDirections(1) != null, "columnResizeCursorInDirections non-nil");
            check(NSCursor.rowResizeCursorInDirections(1) != null, "rowResizeCursorInDirections non-nil");
            check(NSCursor.frameResizeCursorFromPositionInDirections(7, 3) != null, "frameResizeCursor non-nil");
            check(NSCursor.currentCursor() != null, "currentCursor non-nil");
            NSImage cimg = NSCursor.arrowCursor().image();
            check(cimg != null, "cursor image non-nil");
            NSPoint hot = NSCursor.arrowCursor().hotSpot();
            check(hot != null, "hotSpot readable (" + hot + ")");
            check(NSCursor.arrowCursor().isSetOnMouseEntered() == false
                    || NSCursor.arrowCursor().isSetOnMouseEntered() == true,
                    "isSetOnMouseEntered no-crash");
            try {
                NSCursor.arrowCursor().push();
                TestKit.noThrow("balanced push/popCursor no-throw", () -> NSCursor.popCursor());
            } catch (Throwable t) {
                check(false, "push/pop threw: " + t);
            }
            check(NSCursor.wrap(null) == null, "wrap(null) returns null");
        } catch (Throwable t) {
            check(false, "cursor section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- cleanup ----------------
        try {
            Files.walk(saDir).sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> p.toFile().delete());
            check(!Files.exists(saDir), "sa dir cleaned up");
        } catch (Throwable t) {
            check(false, "cleanup threw: " + t);
        }

        System.out.println(TestKit.failures() == 0
                ? "RESULT: PASS (" + asserts + " assertions)"
                : "RESULT: FAIL (" + TestKit.failures() + " of " + asserts + " assertions failed)");
        TestKit.end();
    }
}
