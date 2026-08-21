package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSImage — an AppKit image. Thin, 1:1, stateless wrapper over a native
/// `NSImage`: every method maps to one `objc_msgSend` selector and no
/// Java state is cached beyond the peer. An NSImage lives independently of any
/// view; it is drawn either directly (via `drawInRect` from a
/// `Drawable` callback) or through an `NSImageView` control.
public final class NSImage extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hInitFromFile, MethodHandle hInitFromURL, MethodHandle hSize, MethodHandle hDrawRect, MethodHandle hTIFF, MethodHandle hName, MethodHandle hSetName, MethodHandle hCapInsets, MethodHandle hSetCapInsets, MethodHandle hTemplate, MethodHandle hSetTemplate, MethodHandle hResizingMode, MethodHandle hSetResizingMode, MethodHandle hAccDesc, MethodHandle hSetAccDesc) {}
    private static volatile Handles H;

    private NSImage(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSImage wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSImage(peer);
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
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
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)));
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
            MemorySegment s = (MemorySegment) H.hSize().invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("size"));
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

    /// [image capInsets] — edge insets for 9-part scaling (NSEdgeInsets ~ 4 doubles, returned as NSRect).
    public NSRect capInsets() {
        try {
            MemorySegment s = (MemorySegment) H.hCapInsets().invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("capInsets"));
            // NSEdgeInsets is {top,left,bottom,right} -> map to NSRect {x=top,y=left,w=bottom,h=right}
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("capInsets failed", t);
        }
    }

    /// [image setCapInsets:] — edge insets for 9-part scaling.
    public void setCapInsets(NSRect insets) {
        try {
            H.hSetCapInsets().invokeExact(peer, ObjC.sel("setCapInsets:"), insets.toSegment());
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

    /// [image resizingMode] — NSImageResizingMode (0=tile, 1=stretch).
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
}
