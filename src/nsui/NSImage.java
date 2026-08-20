package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSImage — an AppKit image. Thin, 1:1, stateless wrapper over a native
 * {@code NSImage}: every method maps to one {@code objc_msgSend} selector and no
 * Java state is cached beyond the peer. An NSImage lives independently of any
 * view; it is drawn either directly (via {@link #drawInRect} from a
 * {@link NSView.Drawable} callback) or through an {@link NSImageView} control.
 */
public final class NSImage extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFromFile; // (id, SEL, id) -> id          [initWithContentsOfFile:]
    private static MethodHandle hInitFromURL;  // (id, SEL, id) -> id          [initWithContentsOfURL:]
    private static MethodHandle hSize;          // (SegmentAllocator, id, SEL) -> NSSize   [SIZE struct return]
    private static MethodHandle hDrawRect;      // (id, SEL, NSRect) -> void   [drawInRect:]
    private static MethodHandle hTIFF;         // (id, SEL) -> id             [TIFFRepresentation]
    private static MethodHandle hName;         // (id, SEL) -> id             [name]
    private static MethodHandle hSetName;      // (id, SEL, id) -> bool       [setName:]
    private static MethodHandle hCapInsets;    // (SegmentAllocator, id, SEL) -> NSRect [capInsets] NSEdgeInsets is 4 doubles like NSRect
    private static MethodHandle hSetCapInsets; // (id, SEL, NSRect) -> void   [setCapInsets:]
    private static MethodHandle hTemplate;     // (id, SEL) -> bool           [isTemplate]
    private static MethodHandle hSetTemplate;  // (id, SEL, bool) -> void     [setTemplate:]
    private static MethodHandle hResizingMode; // (id, SEL) -> long           [resizingMode]
    private static MethodHandle hSetResizingMode; // (id, SEL, long) -> void [setResizingMode:]
    private static MethodHandle hAccDesc;      // (id, SEL) -> id             [accessibilityDescription]
    private static MethodHandle hSetAccDesc;   // (id, SEL, id) -> void       [setAccessibilityDescription:]

    private NSImage(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSImage wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSImage(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFromFile = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hInitFromURL = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hSize = ObjC.handle(Sig.of(Ret.SIZE));
        hDrawRect = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
        hTIFF = ObjC.handle(Sig.of(Ret.ID));
        hName = ObjC.handle(Sig.of(Ret.ID));
        hSetName = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        hCapInsets = ObjC.handle(Sig.of(Ret.RECT));
        hSetCapInsets = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
        hTemplate = ObjC.handle(Sig.of(Ret.BOOL));
        hSetTemplate = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hResizingMode = ObjC.handle(Sig.of(Ret.INT));
        hSetResizingMode = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hAccDesc = ObjC.handle(Sig.of(Ret.ID));
        hSetAccDesc = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /**
     * Load an image from a file on disk. Modern AppKit (macOS SDK) has no
     * {@code +imageWithContentsOfFile:} class method — the file-loading entry
     * points are {@code -initWithContentsOfFile:} and {@code -initWithContentsOfURL:}
     * — so this factory uses {@code [[NSImage alloc] initWithContentsOfFile:path]}.
     * Returns {@code null} if init returns nil (e.g. the file does not exist or is
     * not a supported image format).
     */
    public static NSImage imageWithContentsOfFile(String path) {
        ensureInit();
        MemorySegment img = ObjC.msgSendId(ObjC.cls("NSImage"), ObjC.sel("alloc"));
        try {
            img = (MemorySegment) hInitFromFile.invokeExact(img, ObjC.sel("initWithContentsOfFile:"), ObjC.nsstring(path));
        } catch (Throwable t) {
            throw new RuntimeException("initWithContentsOfFile: failed for NSImage", t);
        }
        return (img == null || img.address() == 0) ? null : new NSImage(img);
    }

    /** {@code [[NSImage alloc] initWithContentsOfURL:url]} — load from a file URL or remote URL. */
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
            img = (MemorySegment) hInitFromURL.invokeExact(img, ObjC.sel("initWithContentsOfURL:"), url);
        } catch (Throwable t) {
            throw new RuntimeException("initWithContentsOfURL: failed for NSImage", t);
        }
        return (img == null || img.address() == 0) ? null : new NSImage(img);
    }

    /** {@code [[NSImage alloc] initWithContentsOfURL:nsURL]} — load from an NSURL peer directly. */
    public static NSImage imageWithContentsOfURL(MemorySegment nsURL) {
        ensureInit();
        if (nsURL == null || nsURL.address() == 0) return null;
        MemorySegment img = ObjC.msgSendId(ObjC.cls("NSImage"), ObjC.sel("alloc"));
        try {
            img = (MemorySegment) hInitFromURL.invokeExact(img, ObjC.sel("initWithContentsOfURL:"), nsURL);
        } catch (Throwable t) {
            throw new RuntimeException("initWithContentsOfURL: failed for NSImage", t);
        }
        return (img == null || img.address() == 0) ? null : new NSImage(img);
    }

    /** {@code [NSImage imageNamed:name]} — system or asset-catalog named image (nil if not found). */
    public static NSImage imageNamed(String name) {
        ensureInit();
        if (name == null || name.isEmpty()) return null;
        MemorySegment p = ObjC.msgSendIdId(ObjC.cls("NSImage"), ObjC.sel("imageNamed:"), ObjC.nsstring(name));
        return (p == null || p.address() == 0) ? null : new NSImage(p);
    }

    /** Convenience alias for {@link #imageNamed}. */
    public static NSImage named(String name) { return imageNamed(name); }

    /** {@code [NSImage imageWithSystemSymbolName:accessibilityDescription:]} — SF Symbol (macOS 11+, nil if not found). */
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

    /** {@code [NSImage imageWithSystemSymbolName:]} convenience with nil description. */
    public static NSImage imageWithSystemSymbolName(String symbolName) {
        return imageWithSystemSymbolName(symbolName, null);
    }

    // ---------------------------------------------------------------- instance API

    /** [image size] — the image's size in points (struct return). */
    public NSSize size() {
        try {
            // FFM gives group-layout returns an implicit leading SegmentAllocator param.
            MemorySegment s = (MemorySegment) hSize.invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("size"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("NSImage size failed", t);
        }
    }

    /** [image isValid] — whether the image contains drawable data (load succeeded). */
    public boolean isValid() {
        return ObjC.msgSendBool(peer, ObjC.sel("isValid"));
    }

    /**
     * [image drawInRect:rect] — composite the image into the CURRENT graphics
     * context (the view's drawing context when called from a {@link NSView.Drawable}),
     * scaled to {@code rect} in the recipient's (typically the view's) coordinates.
     */
    public void drawInRect(NSRect rect) {
        try {
            hDrawRect.invokeExact(peer, ObjC.sel("drawInRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("drawInRect: failed", t);
        }
    }

    // ---- additions for completeness ----

    /** [image TIFFRepresentation] — TIFF data for the image (NSData peer), or null if none. */
    public MemorySegment TIFFRepresentation() {
        try {
            MemorySegment d = (MemorySegment) hTIFF.invokeExact(peer, ObjC.sel("TIFFRepresentation"));
            return (d == null || d.address() == 0) ? null : d;
        } catch (Throwable t) {
            throw new RuntimeException("TIFFRepresentation failed", t);
        }
    }

    /** [image name] — the image's name (registered via setName:), or null. */
    public String name() {
        try {
            MemorySegment s = (MemorySegment) hName.invokeExact(peer, ObjC.sel("name"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("name failed", t);
        }
    }

    /** [image setName:] — register the image under a name; returns whether succeeded. */
    public boolean setName(String name) {
        try {
            MemorySegment ns = name == null ? MemorySegment.NULL : ObjC.nsstring(name);
            return (boolean) hSetName.invokeExact(peer, ObjC.sel("setName:"), ns);
        } catch (Throwable t) {
            throw new RuntimeException("setName: failed", t);
        }
    }

    /** [image capInsets] — edge insets for 9-part scaling (NSEdgeInsets ~ 4 doubles, returned as NSRect). */
    public NSRect capInsets() {
        try {
            MemorySegment s = (MemorySegment) hCapInsets.invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("capInsets"));
            // NSEdgeInsets is {top,left,bottom,right} -> map to NSRect {x=top,y=left,w=bottom,h=right}
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("capInsets failed", t);
        }
    }

    /** [image setCapInsets:] — edge insets for 9-part scaling. */
    public void setCapInsets(NSRect insets) {
        try {
            hSetCapInsets.invokeExact(peer, ObjC.sel("setCapInsets:"), insets.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setCapInsets: failed", t);
        }
    }

    /** [image isTemplate] — whether the image is a template (monochrome, tinted by system). */
    public boolean isTemplate() {
        try {
            return (boolean) hTemplate.invokeExact(peer, ObjC.sel("isTemplate"));
        } catch (Throwable t) {
            throw new RuntimeException("isTemplate failed", t);
        }
    }

    /** [image setTemplate:] — mark as template image. */
    public void setTemplate(boolean flag) {
        try {
            hSetTemplate.invokeExact(peer, ObjC.sel("setTemplate:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setTemplate: failed", t);
        }
    }

    /** [image resizingMode] — NSImageResizingMode (0=tile, 1=stretch). */
    public long resizingMode() {
        try {
            return (long) hResizingMode.invokeExact(peer, ObjC.sel("resizingMode"));
        } catch (Throwable t) {
            throw new RuntimeException("resizingMode failed", t);
        }
    }

    /** [image setResizingMode:] — NSImageResizingMode. */
    public void setResizingMode(long mode) {
        try {
            hSetResizingMode.invokeExact(peer, ObjC.sel("setResizingMode:"), mode);
        } catch (Throwable t) {
            throw new RuntimeException("setResizingMode: failed", t);
        }
    }

    /** [image accessibilityDescription] — description for accessibility clients. */
    public String accessibilityDescription() {
        try {
            MemorySegment s = (MemorySegment) hAccDesc.invokeExact(peer, ObjC.sel("accessibilityDescription"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("accessibilityDescription failed", t);
        }
    }

    /** [image setAccessibilityDescription:] — accessibility description. */
    public void setAccessibilityDescription(String desc) {
        try {
            MemorySegment ns = desc == null ? MemorySegment.NULL : ObjC.nsstring(desc);
            hSetAccDesc.invokeExact(peer, ObjC.sel("setAccessibilityDescription:"), ns);
        } catch (Throwable t) {
            throw new RuntimeException("setAccessibilityDescription: failed", t);
        }
    }
}
