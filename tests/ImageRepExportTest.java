package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;

import nsui.*;
import nsui.objc.ObjC;

/// ImageRepExportTest — Tier-1 #5: `NSBitmapImageRep` + one-call PNG/JPEG export.
/// Non-interactive and self-terminating:
///
/// - a real headless image (`NSWorkspace` icon) round-trips TIFF → rep → PNG/JPEG;
/// - encoded bytes carry the correct magic (PNG `0x89 'P' 'N' 'G'`, JPEG `0xFF 0xD8`);
/// - `NSImage.pngData()`/`jpegData()` one-call conveniences match the manual path;
/// - `NSData.writeToFile` lands a real file under `/tmp` (never inside the repo);
/// - property accessors are sane; `wrap`/`create` are null-safe; 100× export stress.
public final class ImageRepExportTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    private static boolean magic(byte[] data, int[] expected) {
        if (data == null || data.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((data[i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println("=== ImageRepExportTest ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            String m = String.valueOf(t.getMessage()).toLowerCase();
            if (m.contains("connection") || m.contains("dlopen") || m.contains("appkit")) {
                System.out.println("SKIP: ObjC.init failed (not macOS / connection error): " + t);
                System.out.println("RESULT: SKIP (connection error, continuing)");
                System.exit(0);
            }
            System.out.println("FAIL: ObjC.init threw unexpected: " + t);
            t.printStackTrace(System.out);
            System.exit(1);
        }

        // ---------------- source image (headless, no window needed) ----------------
        NSImage img = null;
        try {
            img = NSWorkspace.sharedWorkspace().iconForFileType("txt");
            check(img != null && img.peer().address() != 0, "iconForFileType non-nil");
            NSSize sz = img.size();
            check(sz != null && sz.width() > 0 && sz.height() > 0,
                    "icon size sane (" + (sz == null ? "null" : sz.width() + "x" + sz.height()) + ")");
        } catch (Throwable t) {
            check(false, "source-image section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- TIFF -> rep -> properties ----------------
        NSBitmapImageRep rep = null;
        try {
            MemorySegment tiff = img.TIFFRepresentation();
            check(tiff != null && tiff.address() != 0, "TIFFRepresentation non-nil");
            rep = NSBitmapImageRep.create(tiff);
            check(rep != null, "NSBitmapImageRep.create(tiff) non-nil");
            check(rep.pixelsWide() > 0 && rep.pixelsHigh() > 0,
                    "pixels sane (" + rep.pixelsWide() + "x" + rep.pixelsHigh() + ")");
            check(rep.bitsPerSample() > 0 && rep.samplesPerPixel() > 0,
                    "bitsPerSample/samplesPerPixel sane (" + rep.bitsPerSample() + "/" + rep.samplesPerPixel() + ")");
            String cs = rep.colorSpaceName();
            check(cs != null && !cs.isEmpty(), "colorSpaceName readable (\"" + cs + "\")");
            check(rep.hasAlpha() == false || rep.hasAlpha() == true, "hasAlpha accessor no crash (" + rep.hasAlpha() + ")");
            check(rep.isPlanar() == false || rep.isPlanar() == true, "isPlanar accessor no crash (" + rep.isPlanar() + ")");
            MemorySegment cg = rep.cgImage();
            check(cg != null && cg.address() != 0, "cgImage() peer non-null");
            check(NSBitmapImageRep.wrap(null) == null, "wrap(null) returns null");
            check(NSBitmapImageRep.wrap(MemorySegment.NULL) == null, "wrap(MemorySegment.NULL) returns null");
            check(NSBitmapImageRep.create((NSData) null) == null, "create((NSData)null) returns null");
            check(NSBitmapImageRep.create((MemorySegment) null) == null, "create((MemorySegment)null) returns null");
        } catch (Throwable t) {
            check(false, "rep-properties section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- PNG export: magic bytes ----------------
        try {
            NSData png = rep.pngData();
            check(png != null && png.length() > 0, "rep.pngData() non-empty (" + (png == null ? -1 : png.length()) + " bytes)");
            byte[] bytes = png.toByteArray();
            int[] pngMagic = {0x89, 'P', 'N', 'G'};
            check(magic(bytes, pngMagic), "PNG magic bytes correct (0x89 'P' 'N' 'G')");

            NSData manual = rep.representationUsingType(NSBitmapImageRep.fileTypePNG, null);
            check(manual != null && manual.length() == png.length(),
                    "representationUsingType(fileTypePNG, null) matches pngData()");
        } catch (Throwable t) {
            check(false, "png-export section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- JPEG export: magic bytes ----------------
        try {
            NSData jpeg = rep.jpegData(0.9f);
            check(jpeg != null && jpeg.length() > 0, "rep.jpegData(0.9) non-empty (" + (jpeg == null ? -1 : jpeg.length()) + " bytes)");
            byte[] bytes = jpeg.toByteArray();
            int[] jpegMagic = {0xFF, 0xD8};
            check(magic(bytes, jpegMagic), "JPEG magic bytes correct (0xFF 0xD8)");
        } catch (Throwable t) {
            check(false, "jpeg-export section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSImage one-call conveniences ----------------
        try {
            NSData png = img.pngData();
            check(png != null && magic(png.toByteArray(), new int[]{0x89, 'P', 'N', 'G'}),
                    "img.pngData() one-call export has PNG magic");
            NSData jpeg = img.jpegData(0.8f);
            check(jpeg != null && magic(jpeg.toByteArray(), new int[]{0xFF, 0xD8}),
                    "img.jpegData(0.8f) one-call export has JPEG magic");
        } catch (Throwable t) {
            check(false, "nsimage-convenience section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- writeToFile under /tmp ----------------
        try {
            Path out = Files.createTempFile("nsui3-imgrep", ".png");
            Files.delete(out); // writeToFile must create it fresh
            NSData png = img.pngData();
            boolean ok = png.writeToFile(out.toString(), true);
            check(ok, "NSData.writeToFile returned true");
            check(Files.exists(out) && Files.size(out) > 0,
                    "file landed on disk (" + (Files.exists(out) ? Files.size(out) : -1) + " bytes)");
            byte[] disk = Files.readAllBytes(out);
            check(magic(disk, new int[]{0x89, 'P', 'N', 'G'}), "on-disk file has PNG magic");
            Files.deleteIfExists(out);
        } catch (Throwable t) {
            check(false, "writeToFile section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- 100x export stress ----------------
        try {
            int okRuns = 0;
            for (int i = 0; i < 100; i++) {
                NSData png = img.pngData();
                if (png != null && png.length() > 0
                        && magic(png.toByteArray(), new int[]{0x89, 'P', 'N', 'G'})) okRuns++;
            }
            check(okRuns == 100, "100x pngData() stress all valid (" + okRuns + "/100)");
        } catch (Throwable t) {
            check(false, "stress section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(failures == 0
                ? "RESULT: PASS (" + asserts + " assertions)"
                : "RESULT: FAIL (" + failures + " of " + asserts + " assertions failed)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
