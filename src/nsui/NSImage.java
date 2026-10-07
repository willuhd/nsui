package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSImage — an AppKit image. Thin, 1:1, stateless wrapper over a native
/// `NSImage`: every method maps to one `objc_msgSend` selector and no
/// Java state is cached beyond the peer.
///
/// OMITTED (shapes verified missing from Sig.VOCABULARY by grep, or out of
/// scope): drawAtPoint:fromRect:operation:fraction: and the
/// drawInRect:fromRect:operation:fraction:(:respectFlipped:hints:) family
/// (multi-struct shapes); drawRepresentation:inRect: (BOOL,ID,RECT);
/// TIFFRepresentationUsingCompression:factor: (ID,INT,FLOAT); initWithCGImage:
/// size: (ID,ID,SIZE); CGImageForProposedRect:context:hints: (raw NSRect*);
/// bestRepresentationForRect:context:hints: (ID,RECT,ID,ID); hitTestRect:…;
/// recommendedLayerContentsScale: (DOUBLE,DOUBLE);
/// imageWithSystemSymbolName:variableValue:… + imageWithSymbolName:…
/// (ID,ID,DOUBLE,ID shapes); imageWithSize:flipped:drawingHandler: (block);
/// delegate (needs delegate-proxy machinery); symbolConfiguration/locale
/// (wrappers absent); the deprecated lockFocus/composite/dissolve/scalesWhen
/// Resized/dataRetained/cachedSeparately family.
/// An NSImage lives independently of any
/// view; it is drawn either directly (via `drawInRect` from a
/// `Drawable` callback) or through an `NSImageView` control.
public final class NSImage extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hInitFromFile, MethodHandle hInitFromURL, MethodHandle hInitSize, MethodHandle hLayerContents, MethodHandle hSize, MethodHandle hDrawRect, MethodHandle hTIFF, MethodHandle hName, MethodHandle hSetName, MethodHandle hCapInsets, MethodHandle hSetCapInsets, MethodHandle hTemplate, MethodHandle hSetTemplate, MethodHandle hResizingMode, MethodHandle hSetResizingMode, MethodHandle hAccDesc, MethodHandle hSetAccDesc, MethodHandle hCapEdgeInsets, MethodHandle hSetCapEdgeInsets) {}
    private static volatile Handles H;

    private NSImage(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSImage wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSImage(peer);
    }

    private static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.EDGEINSETS)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.EDGEINSETS)));
    }

    /// Load an image from a file on disk. Modern AppKit (macOS SDK) has no
    /// `+imageWithContentsOfFile:` class method — the file-loading entry
    /// points are `-initWithContentsOfFile:` and `-initWithContentsOfURL:`
    /// — so this factory uses `[[NSImage alloc] initWithContentsOfFile:path]`.
    /// Returns `null` if init returns nil (e.g. the file does not exist or is
    /// not a supported image format).
    public static NSImage imageWithContentsOfFile(String path) {
        ensureInit();
        MemorySegment img = ObjC.msgSendId(ObjC.cls("NSImage"), ObjC.sel("alloc"));
        try {
            img = (MemorySegment) H.hInitFromFile().invokeExact(img, ObjC.sel("initWithContentsOfFile:"), ObjC.nsstring(path));
        } catch (Throwable t) {
            throw new RuntimeException("initWithContentsOfFile: failed for NSImage", t);
        }
        return (img == null || img.address() == 0) ? null : new NSImage(img);
    }

    /// `[[NSImage alloc] initWithContentsOfURL:url]` — load from a file URL or remote URL.
    public static NSImage imageWithContentsOfURL(String urlString) {
        ensureInit();
        // Build NSURL via +[NSURL fileURLWithPath:] if it's a filesystem path, otherwise URLWithString.
        // Heuristic: if string contains "://" treat as URL, else file path.
        MemorySegment url;
        if (urlString.contains("://")) {
            url = ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("URLWithString:"), ObjC.nsstring(urlString));
        } else {
            url = ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("fileURLWithPath:"), ObjC.nsstring(urlString));
        }
        if (url == null || url.address() == 0) return null;
        MemorySegment img = ObjC.msgSendId(ObjC.cls("NSImage"), ObjC.sel("alloc"));
        try {
            img = (MemorySegment) H.hInitFromURL().invokeExact(img, ObjC.sel("initWithContentsOfURL:"), url);
        } catch (Throwable t) {
            throw new RuntimeException("initWithContentsOfURL: failed for NSImage", t);
        }
        return (img == null || img.address() == 0) ? null : new NSImage(img);
    }

    /// `[[NSImage alloc] initWithContentsOfURL:nsURL]` — load from an NSURL peer directly.
    public static NSImage imageWithContentsOfURL(MemorySegment nsURL) {
        ensureInit();
        if (nsURL == null || nsURL.address() == 0) return null;
        MemorySegment img = ObjC.msgSendId(ObjC.cls("NSImage"), ObjC.sel("alloc"));
        try {
            img = (MemorySegment) H.hInitFromURL().invokeExact(img, ObjC.sel("initWithContentsOfURL:"), nsURL);
        } catch (Throwable t) {
            throw new RuntimeException("initWithContentsOfURL: failed for NSImage", t);
        }
        return (img == null || img.address() == 0) ? null : new NSImage(img);
    }

    /// `[NSImage imageNamed:name]` — system or asset-catalog named image (nil if not found).
    public static NSImage imageNamed(String name) {
        ensureInit();
        if (name == null || name.isEmpty()) return null;
        MemorySegment p = ObjC.msgSendIdId(ObjC.cls("NSImage"), ObjC.sel("imageNamed:"), ObjC.nsstring(name));
        return (p == null || p.address() == 0) ? null : new NSImage(p);
    }

    /// Convenience alias for `imageNamed`.
    public static NSImage named(String name) { return imageNamed(name); }

    /// `[NSImage imageWithSystemSymbolName:accessibilityDescription:]` — SF Symbol (macOS 11+, nil if not found).
    public static NSImage imageWithSystemSymbolName(String symbolName, String accessibilityDescription) {
        ensureInit();
        if (symbolName == null || symbolName.isEmpty()) return null;
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment desc = (accessibilityDescription == null ? MemorySegment.NULL : ObjC.nsstring(accessibilityDescription));
            if (desc == null || desc.address() == 0) desc = MemorySegment.NULL;
            MemorySegment sym = ObjC.nsstring(symbolName);
            MemorySegment p = (MemorySegment) h.invokeExact(ObjC.cls("NSImage"), ObjC.sel("imageWithSystemSymbolName:accessibilityDescription:"),
                    (MemorySegment) sym, (MemorySegment) desc);
            return (p == null || p.address() == 0) ? null : new NSImage(p);
        } catch (Throwable t) {
            return null;
        }
    }

    /// `[NSImage imageWithSystemSymbolName:]` convenience with nil description.
    public static NSImage imageWithSystemSymbolName(String symbolName) {
        return imageWithSystemSymbolName(symbolName, null);
    }

    // ---------------------------------------------------------------- instance API

    /// [image size] — the image's size in points (struct return).
    public NSSize size() {
        try {
            // FFM gives group-layout returns an implicit leading SegmentAllocator param.
            MemorySegment s = (MemorySegment) H.hSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("size"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("NSImage size failed", t);
        }
    }

    /// [image setSize:] — set the image's size.
    public void setSize(NSSize size) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
            h.invokeExact(peer, ObjC.sel("setSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setSize: failed", t);
        }
    }

    /// [image isValid] — whether the image contains drawable data (load succeeded).
    public boolean isValid() {
        return ObjC.msgSendBool(peer, ObjC.sel("isValid"));
    }

    /// [image drawInRect:rect] — composite the image into the CURRENT graphics
    /// context (the view's drawing context when called from a `Drawable`),
    /// scaled to `rect` in the recipient's (typically the view's) coordinates.
    public void drawInRect(NSRect rect) {
        try {
            H.hDrawRect().invokeExact(peer, ObjC.sel("drawInRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("drawInRect: failed", t);
        }
    }

    // ---- additions for completeness ----

    /// [image TIFFRepresentation] — TIFF data for the image (NSData peer), or null if none.
    public MemorySegment TIFFRepresentation() {
        try {
            MemorySegment d = (MemorySegment) H.hTIFF().invokeExact(peer, ObjC.sel("TIFFRepresentation"));
            return (d == null || d.address() == 0) ? null : d;
        } catch (Throwable t) {
            throw new RuntimeException("TIFFRepresentation failed", t);
        }
    }

    /// [image name] — the image's name (registered via setName:), or null.
    public String name() {
        try {
            MemorySegment s = (MemorySegment) H.hName().invokeExact(peer, ObjC.sel("name"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("name failed", t);
        }
    }

    /// [image setName:] — register the image under a name; returns whether succeeded.
    public boolean setName(String name) {
        try {
            MemorySegment ns = name == null ? MemorySegment.NULL : ObjC.nsstring(name);
            return (boolean) H.hSetName().invokeExact(peer, ObjC.sel("setName:"), ns);
        } catch (Throwable t) {
            throw new RuntimeException("setName: failed", t);
        }
    }

    /// [image capInsets] — edge insets for 9-part scaling (NSEdgeInsets, 32-byte struct return).
    public NSEdgeInsets capInsets() {
        try {
            MemorySegment s = (MemorySegment) H.hCapEdgeInsets().invokeExact(ObjC.structSlot(), peer, ObjC.sel("capInsets"));
            return NSEdgeInsets.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("capInsets failed", t);
        }
    }

    /// [image setCapInsets:] — edge insets for 9-part scaling (NSEdgeInsets by value).
    public void setCapInsets(NSEdgeInsets insets) {
        try {
            H.hSetCapEdgeInsets().invokeExact(peer, ObjC.sel("setCapInsets:"), insets.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setCapInsets: failed", t);
        }
    }

    /// [image isTemplate] — whether the image is a template (monochrome, tinted by system).
    public boolean isTemplate() {
        try {
            return (boolean) H.hTemplate().invokeExact(peer, ObjC.sel("isTemplate"));
        } catch (Throwable t) {
            throw new RuntimeException("isTemplate failed", t);
        }
    }

    /// [image setTemplate:] — mark as template image.
    public void setTemplate(boolean flag) {
        try {
            H.hSetTemplate().invokeExact(peer, ObjC.sel("setTemplate:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setTemplate: failed", t);
        }
    }

    /// [image resizingMode] — NSImageResizingMode (macOS: 0=stretch, 1=tile).
    public long resizingMode() {
        try {
            return (long) H.hResizingMode().invokeExact(peer, ObjC.sel("resizingMode"));
        } catch (Throwable t) {
            throw new RuntimeException("resizingMode failed", t);
        }
    }

    /// [image setResizingMode:] — NSImageResizingMode.
    public void setResizingMode(long mode) {
        try {
            H.hSetResizingMode().invokeExact(peer, ObjC.sel("setResizingMode:"), mode);
        } catch (Throwable t) {
            throw new RuntimeException("setResizingMode: failed", t);
        }
    }

    /// [image accessibilityDescription] — description for accessibility clients.
    public String accessibilityDescription() {
        try {
            MemorySegment s = (MemorySegment) H.hAccDesc().invokeExact(peer, ObjC.sel("accessibilityDescription"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("accessibilityDescription failed", t);
        }
    }

    /// [image setAccessibilityDescription:] — accessibility description.
    public void setAccessibilityDescription(String desc) {
        try {
            MemorySegment ns = desc == null ? MemorySegment.NULL : ObjC.nsstring(desc);
            H.hSetAccDesc().invokeExact(peer, ObjC.sel("setAccessibilityDescription:"), ns);
        } catch (Throwable t) {
            throw new RuntimeException("setAccessibilityDescription: failed", t);
        }
    }

    // ---------------------------------------------------------------- construction (more)

    /// `[[NSImage alloc] initWithSize:size]` — blank image with the given size.
    public static NSImage createWithSize(NSSize size) {
        ensureInit();
        MemorySegment img = ObjC.msgSendId(ObjC.cls("NSImage"), ObjC.sel("alloc"));
        try {
            img = (MemorySegment) H.hInitSize().invokeExact(img, ObjC.sel("initWithSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithSize: failed for NSImage", t);
        }
        return (img == null || img.address() == 0) ? null : new NSImage(img);
    }

    private static NSImage initOneIdArg(String sel, MemorySegment arg) {
        ensureInit();
        MemorySegment img = ObjC.msgSendId(ObjC.cls("NSImage"), ObjC.sel("alloc"));
        try {
            img = (MemorySegment) H.hInitFromFile().invokeExact(img, ObjC.sel(sel), arg);
        } catch (Throwable t) {
            throw new RuntimeException(sel + " failed for NSImage", t);
        }
        return (img == null || img.address() == 0) ? null : new NSImage(img);
    }

    /// `[[NSImage alloc] initWithData:data]` — image from encoded bytes (nil on bad data).
    public static NSImage createWithData(NSData data) {
        if (data == null) return null;
        return initOneIdArg("initWithData:", data.peer());
    }

    /// `[[NSImage alloc] initByReferencingFile:fileName]` — lazy file-backed image.
    public static NSImage createByReferencingFile(String path) {
        if (path == null) return null;
        return initOneIdArg("initByReferencingFile:", ObjC.nsstring(path));
    }

    /// `[[NSImage alloc] initByReferencingURL:url]` — lazy URL-backed image
    /// (raw NSURL peer; supports progressive loading).
    public static NSImage createByReferencingURL(MemorySegment nsURL) {
        if (nsURL == null || nsURL.address() == 0) return null;
        return initOneIdArg("initByReferencingURL:", nsURL);
    }

    /// `[[NSImage alloc] initWithPasteboard:pasteboard]` — image from pasteboard data.
    public static NSImage createWithPasteboard(NSPasteboard pasteboard) {
        if (pasteboard == null) return null;
        return initOneIdArg("initWithPasteboard:", pasteboard.peer());
    }

    /// `[[NSImage alloc] initWithDataIgnoringOrientation:data]` — ignores EXIF orientation.
    public static NSImage createWithDataIgnoringOrientation(NSData data) {
        if (data == null) return null;
        return initOneIdArg("initWithDataIgnoringOrientation:", data.peer());
    }

    /// `+[NSImage imageTypes]` — all loadable type identifiers.
    public static NSArray imageTypes() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(ObjC.cls("NSImage"), ObjC.sel("imageTypes")));
    }

    /// `+[NSImage imageUnfilteredTypes]` — unfiltered type identifiers.
    public static NSArray imageUnfilteredTypes() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(ObjC.cls("NSImage"), ObjC.sel("imageUnfilteredTypes")));
    }

    /// `+[NSImage canInitWithPasteboard:]`.
    public static boolean canInitWithPasteboard(NSPasteboard pasteboard) {
        ensureInit();
        if (pasteboard == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(ObjC.cls("NSImage"), ObjC.sel("canInitWithPasteboard:"), pasteboard.peer());
        } catch (Throwable t) {
            throw new RuntimeException("canInitWithPasteboard: failed", t);
        }
    }

    // ---------------------------------------------------------------- properties (more)

    /// backgroundColor.
    public NSColor backgroundColor() {
        ensureInit();
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("backgroundColor")));
    }

    /// setBackgroundColor:.
    public void setBackgroundColor(NSColor color) {
        ensureInit();
        ObjC.msgSendVoidId(peer, ObjC.sel("setBackgroundColor:"),
                (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// usesEPSOnResolutionMismatch.
    public boolean usesEPSOnResolutionMismatch() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesEPSOnResolutionMismatch"));
    }

    /// setUsesEPSOnResolutionMismatch:.
    public void setUsesEPSOnResolutionMismatch(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesEPSOnResolutionMismatch:"), flag);
    }

    /// prefersColorMatch.
    public boolean prefersColorMatch() {
        return ObjC.msgSendBool(peer, ObjC.sel("prefersColorMatch"));
    }

    /// setPrefersColorMatch:.
    public void setPrefersColorMatch(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setPrefersColorMatch:"), flag);
    }

    /// matchesOnMultipleResolution.
    public boolean matchesOnMultipleResolution() {
        return ObjC.msgSendBool(peer, ObjC.sel("matchesOnMultipleResolution"));
    }

    /// setMatchesOnMultipleResolution:.
    public void setMatchesOnMultipleResolution(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setMatchesOnMultipleResolution:"), flag);
    }

    /// matchesOnlyOnBestFittingAxis.
    public boolean matchesOnlyOnBestFittingAxis() {
        return ObjC.msgSendBool(peer, ObjC.sel("matchesOnlyOnBestFittingAxis"));
    }

    /// setMatchesOnlyOnBestFittingAxis:.
    public void setMatchesOnlyOnBestFittingAxis(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setMatchesOnlyOnBestFittingAxis:"), flag);
    }

    /// recache — drop caches; the image re-decodes on next draw.
    public void recache() {
        ObjC.msgSendVoid(peer, ObjC.sel("recache"));
    }

    /// representations — the image reps (nil-safe).
    public NSArray representations() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("representations")));
    }

    /// addRepresentations:.
    public void addRepresentations(NSArray reps) {
        ensureInit();
        ObjC.msgSendVoidId(peer, ObjC.sel("addRepresentations:"),
                (MemorySegment) (reps == null ? MemorySegment.NULL : reps.peer()));
    }

    /// addRepresentation:.
    public void addRepresentation(NSBitmapImageRep rep) {
        ensureInit();
        ObjC.msgSendVoidId(peer, ObjC.sel("addRepresentation:"),
                (MemorySegment) (rep == null ? MemorySegment.NULL : rep.peer()));
    }

    /// removeRepresentation:.
    public void removeRepresentation(NSBitmapImageRep rep) {
        ensureInit();
        ObjC.msgSendVoidId(peer, ObjC.sel("removeRepresentation:"),
                (MemorySegment) (rep == null ? MemorySegment.NULL : rep.peer()));
    }

    /// cacheMode / setCacheMode: (NSImageCacheMode 0..3).
    public long cacheMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("cacheMode"));
    }
    public void setCacheMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setCacheMode:"), mode);
    }

    /// alignmentRect — layout metadata (default {{0,0},size}).
    public NSRect alignmentRect() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hCapInsets().invokeExact(ObjC.structSlot(), peer, ObjC.sel("alignmentRect"));
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("alignmentRect failed", t);
        }
    }

    /// setAlignmentRect:.
    public void setAlignmentRect(NSRect rect) {
        ensureInit();
        try {
            H.hSetCapInsets().invokeExact(peer, ObjC.sel("setAlignmentRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setAlignmentRect: failed", t);
        }
    }

    /// layerContentsForContentsScale: — layer content id for the scale (raw peer).
    public MemorySegment layerContentsForContentsScale(double scale) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) H.hLayerContents().invokeExact(peer,
                    ObjC.sel("layerContentsForContentsScale:"), scale);
            return (c == null || c.address() == 0) ? null : c;
        } catch (Throwable t) {
            throw new RuntimeException("layerContentsForContentsScale: failed", t);
        }
    }

    // ---------------------------------------------------------------- encoded export

    /// One-call PNG export: `TIFFRepresentation` → `NSBitmapImageRep` →
    /// `representationUsingType:properties:` (`fileTypePNG`). Returns null when
    /// the image has no representable bitmap content or encoding fails.
    /// Round trip (file → image → PNG bytes → file):
    ///
    /// ```
    /// NSImage img = NSImage.imageWithContentsOfFile("/tmp/in.png");
    /// NSData png = img.pngData();               // bytes start 0x89 'P' 'N' 'G'
    /// png.writeToFile("/tmp/out.png", true);    // leaves the process as a .png
    /// ```
    public NSData pngData() {
        NSBitmapImageRep rep = bitmapRep();
        return rep == null ? null : rep.pngData();
    }

    /// One-call JPEG export with `compression` in 0.0–1.0 (mapped through the
    /// `NSImageCompressionFactor` property). Returns null when the image has no
    /// representable bitmap content or encoding fails.
    /// Round trip (image → JPEG bytes → file):
    ///
    /// ```
    /// NSImage img = NSImage.imageWithSystemSymbolName("folder");
    /// NSData jpg = img.jpegData(0.9f);          // bytes start 0xFF 0xD8
    /// jpg.writeToFile("/tmp/out.jpg", true);
    /// ```
    public NSData jpegData(float compression) {
        NSBitmapImageRep rep = bitmapRep();
        return rep == null ? null : rep.jpegData(compression);
    }

    /// Shared TIFF → bitmap-rep decode step behind `pngData`/`jpegData`.
    private NSBitmapImageRep bitmapRep() {
        return NSBitmapImageRep.create(TIFFRepresentation());
    }
}
