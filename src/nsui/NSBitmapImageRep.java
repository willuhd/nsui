package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSBitmapImageRep — a bitmap image representation with encoded-data export.
/// Thin, 1:1, stateless wrapper over a native `NSBitmapImageRep`: every method
/// maps to one `objc_msgSend` and no Java state is cached beyond the peer and
/// the lazy handle record.
///
/// The typical round trip out of the process is:
///
/// ```
/// NSImage img = ...;
/// NSData tiff = img.TIFFRepresentation();                 // NSImage
/// NSBitmapImageRep rep = NSBitmapImageRep.create(tiff);   // decode TIFF
/// NSData png = rep.representationUsingType(NSBitmapImageRep.fileTypePNG, null);
/// png.writeToFile("/tmp/out.png", true);                  // NSData
/// ```
///
/// or, in one call, `img.pngData()` / `img.jpegData(0.9f)`.
public final class NSBitmapImageRep extends NSObject {

    // ---- file types (NSBitmapImageFileType) ----
    // Raw values VERIFIED against the macOS SDK header
    // AppKit.framework/Headers/NSBitmapImageRep.h:
    //   typedef NS_ENUM(NSUInteger, NSBitmapImageFileType) {
    //       NSBitmapImageFileTypeTIFF,      // 0
    //       NSBitmapImageFileTypeBMP,       // 1
    //       NSBitmapImageFileTypeGIF,       // 2
    //       NSBitmapImageFileTypeJPEG,      // 3
    //       NSBitmapImageFileTypePNG,       // 4
    //       NSBitmapImageFileTypeJPEG2000   // 5
    //   };

    /// `NSBitmapImageFileTypeTIFF` — uncompressed/lossless Tagged Image File Format.
    public static final long fileTypeTIFF = 0;

    /// `NSBitmapImageFileTypeBMP` — Windows bitmap.
    public static final long fileTypeBMP = 1;

    /// `NSBitmapImageFileTypeGIF` — Graphics Interchange Format.
    public static final long fileTypeGIF = 2;

    /// `NSBitmapImageFileTypeJPEG` — lossy JPEG; honors the `NSImageCompressionFactor`
    /// property (0.0–1.0 float in an NSNumber).
    public static final long fileTypeJPEG = 3;

    /// `NSBitmapImageFileTypePNG` — Portable Network Graphics (lossless, alpha-aware).
    public static final long fileTypePNG = 4;

    /// `NSBitmapImageFileTypeJPEG2000` — JPEG 2000.
    public static final long fileTypeJPEG2000 = 5;

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hInitWithData, MethodHandle hRepresentation) {}
    private static volatile Handles H;

    private NSBitmapImageRep(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native `NSBitmapImageRep` id (null for nil).
    public static NSBitmapImageRep wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSBitmapImageRep(peer);
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),            // initWithData:
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID)));  // representationUsingType:properties:
    }

    // ---------------------------------------------------------------- construction

    /// `[[NSBitmapImageRep alloc] initWithData:data]` — build a rep from encoded
    /// image data (typically an `NSImage.TIFFRepresentation()` result). Returns
    /// null when `tiffData` is null or init returns nil (unsupported data).
    public static NSBitmapImageRep create(NSData tiffData) {
        if (tiffData == null) return null;
        return create(tiffData.peer());
    }

    /// Raw-peer variant of `create(NSData)` — `dataPeer` is the native NSData id.
    /// Returns null for a null/NULL peer or when init returns nil.
    public static NSBitmapImageRep create(MemorySegment dataPeer) {
        if (dataPeer == null || dataPeer.address() == 0) return null;
        ensureInit();
        MemorySegment rep = ObjC.msgSendId(ObjC.cls("NSBitmapImageRep"), ObjC.sel("alloc"));
        try {
            rep = (MemorySegment) H.hInitWithData().invokeExact(rep, ObjC.sel("initWithData:"), dataPeer);
        } catch (Throwable t) {
            throw new RuntimeException("initWithData: failed for NSBitmapImageRep", t);
        }
        return wrap(rep);
    }

    // ---------------------------------------------------------------- properties

    /// `[rep pixelsWide]` — pixel width of the bitmap.
    public long pixelsWide() {
        return ObjC.msgSendLong(peer, ObjC.sel("pixelsWide"));
    }

    /// `[rep pixelsHigh]` — pixel height of the bitmap.
    public long pixelsHigh() {
        return ObjC.msgSendLong(peer, ObjC.sel("pixelsHigh"));
    }

    /// `[rep samplesPerPixel]` — number of samples per pixel (e.g. 4 for RGBA).
    public long samplesPerPixel() {
        return ObjC.msgSendLong(peer, ObjC.sel("samplesPerPixel"));
    }

    /// `[rep bitsPerSample]` — bits per sample (e.g. 8).
    public long bitsPerSample() {
        return ObjC.msgSendLong(peer, ObjC.sel("bitsPerSample"));
    }

    /// `[rep hasAlpha]` — whether the bitmap has an alpha channel.
    public boolean hasAlpha() {
        return ObjC.msgSendBool(peer, ObjC.sel("hasAlpha"));
    }

    /// `[rep isPlanar]` — whether the bitmap data is planar rather than chunky.
    public boolean isPlanar() {
        return ObjC.msgSendBool(peer, ObjC.sel("isPlanar"));
    }

    /// `[rep colorSpaceName]` — the color space name (e.g. `"NSCalibratedRGBColorSpace"`), or null.
    public String colorSpaceName() {
        MemorySegment s = ObjC.msgSendId(peer, ObjC.sel("colorSpaceName"));
        return ObjC.toString(s);
    }

    /// `[rep CGImage]` — raw `CGImageRef` peer for CoreGraphics interop
    /// (caller owns nothing; the rep keeps it alive). Null when unavailable.
    public MemorySegment cgImage() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("CGImage"));
        return (c == null || c.address() == 0) ? null : c;
    }

    // ---------------------------------------------------------------- export

    /// `[rep representationUsingType:properties:]` — encode the bitmap as
    /// `fileType` (one of the `fileType*` constants). `properties` may be null
    /// (sent as NULL); for JPEG pass an `NSDictionary` holding
    /// `NSImageCompressionFactor` → NSNumber(0.0–1.0). Returns null when the
    /// encoding fails.
    public NSData representationUsingType(long fileType, NSDictionary properties) {
        ensureInit();
        MemorySegment props = (MemorySegment) (properties == null ? MemorySegment.NULL : properties.peer());
        try {
            MemorySegment d = (MemorySegment) H.hRepresentation()
                    .invokeExact(peer, ObjC.sel("representationUsingType:properties:"), fileType, props);
            return NSData.wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("representationUsingType:properties: failed", t);
        }
    }

    /// Convenience: encode as PNG (`fileTypePNG`, no properties). One-call
    /// counterpart of `representationUsingType(fileTypePNG, null)`.
    public NSData pngData() {
        return representationUsingType(fileTypePNG, null);
    }

    /// Convenience: encode as JPEG (`fileTypeJPEG`) with `compression` in 0.0–1.0
    /// mapped through the `NSImageCompressionFactor` property.
    public NSData jpegData(float compression) {
        NSDictionary props = NSDictionary.mutableDictionary();
        props.setObjectForKey(NSNumber.numberWithDouble(compression),
                NSString.wrap(ObjC.nsstring("NSImageCompressionFactor")));
        return representationUsingType(fileTypeJPEG, props);
    }
}
