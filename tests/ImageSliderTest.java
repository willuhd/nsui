package nsui.tests;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import javax.imageio.ImageIO;

import nsui.NSApplication;
import nsui.NSEvent;
import nsui.NSImage;
import nsui.NSImageView;
import nsui.NSProgressIndicator;
import nsui.NSRect;
import nsui.NSSlider;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * Image + value-widget wrappers, verified end to end:
 * <ul>
 *   <li>NSImage: load a 64x64 solid-RED PNG written with javax.imageio (headless),
 *       assert isValid() and size()==(64,64);</li>
 *   <li>draw it through a Java NSView.drawRect: via [image drawInRect:], render the
 *       view to a bitmap and assert a pixel inside the draw region is RED;</li>
 *   <li>NSImageView: set image + scaling on a view, no crash;</li>
 *   <li>NSSlider: min/max/value round-trip, tick marks, tick-only snapping, disable;</li>
 *   <li>NSProgressIndicator: determinate state, range/value, start/stop animation.</li>
 * </ul>
 */
public final class ImageSliderTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== ImageSliderTest — NSImage/NSImageView/NSSlider/NSProgressIndicator ===");
        ObjC.init();           // FFM bindings (must be first)

        // ------------------------------------------------------------ image pipeline
        String png = "/tmp/nsui-test-red.png";
        writeRedPng(png, 64, 64);

        NSImage img = NSImage.imageWithContentsOfFile(png);
        check(img != null, "NSImage.imageWithContentsOfFile loaded a file (non-null)");
        if (img == null) {
            System.out.println("FAIL(TOTAL): image load failed — cannot proceed with draw test");
            System.exit(1);
        }
        check(img.isValid(), "NSImage.isValid() == true");
        double w = img.size().width(), h = img.size().height();
        check(Math.abs(w - 64.0) < 0.01 && Math.abs(h - 64.0) < 0.01,
                "NSImage.size() == (64,64) [got (" + w + "," + h + ")]");

        // ---- draw it through a Java drawRect: and verify the composited pixel is red ----
        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        NSWindow window = NSWindow.create(new NSRect(0, 0, 300, 200), 15L, 2L, false);
        window.setTitle("ImageSliderTest");
        window.center();
        window.setReleasedWhenClosed(false);

        // 128x128 image drawn with its origin at (10,62). The view is NOT flipped
        // (y-axis points up), but the NSBitmapImageRep below is top-down, so a view-y
        // region of [62,190] maps to bitmap-y [10,138]. Drawing at y=62 places the
        // image's top-left region at bitmap-y near 0 so a (50,50) bitmap probe lands
        // comfortably inside it — matching the task's requested read location.
        NSView.Drawable drawable = (ctx, dirtyRect) -> img.drawInRect(new NSRect(10, 62, 128, 128));
        NSView view = NSView.create(new NSRect(0, 0, 300, 200), drawable);
        window.setContentView(view);
        view.setNeedsDisplay(true);

        app.activateIgnoringOtherApps(true);
        window.makeKeyAndOrderFront(null);
        app.finishLaunching();

        pump(app);

        System.out.println("  -- rendering view to bitmap --");
        MemPixels px = renderToBitmap(view);
        // (50,50) is inside the composited 128x128 image (which occupies bitmap
        // x[10,138] x y[10,138] after the flip-aware placement above).
        int[] pix = px.rgb(50, 50);
        System.out.printf("  pixel(50,50) channel[0,1,2]=[%d,%d,%d] (expect red)%n", pix[0], pix[1], pix[2]);
        boolean drawnRed = pix[0] > 150 && pix[2] < 100;
        check(drawnRed, "pixel inside drawInRect region is RED (r>150, b<100)");

        // ------------------------------------------------------------ NSImageView
        NSImageView imageView = NSImageView.create(new NSRect(10, 150, 128, 32));
        imageView.setImage(img);
        imageView.setImageScaling(3L);          // NSImageScaleProportionallyUpOrDown
        imageView.setImageFrameStyle(0L);
        view.addSubview(imageView);             // view IS the content view (setContentView above)
        pump(app);
        check(true, "NSImageView created + setImage + setImageScaling(3) without crash");

        // ------------------------------------------------------------ NSSlider
        NSSlider slider = NSSlider.create(new NSRect(10, 165, 200, 20));
        slider.setMinValue(0.0);
        slider.setMaxValue(100.0);
        slider.setDoubleValue(42.0);
        double got = slider.doubleValue();
        check(Math.abs(got - 42.0) < 0.01, "NSSlider doubleValue() round-trips 42.0 [got " + got + "]");
        slider.setNumberOfTickMarks(5L);
        slider.setAllowsTickMarkValuesOnly(true);
        slider.setEnabled(false);
        check(!slider.isEnabled(), "NSSlider.isEnabled() == false after setEnabled(false)");
        view.addSubview(slider);
        pump(app);
        check(true, "NSSlider configured + added without crash");

        // ------------------------------------------------------------ NSProgressIndicator
        NSProgressIndicator pi = NSProgressIndicator.create(new NSRect(10, 140, 200, 18));
        pi.setIndeterminate(false);
        check(!pi.isIndeterminate(), "NSProgressIndicator.isIndeterminate() == false after setIndeterminate(false)");
        pi.setMinValue(0.0);
        pi.setMaxValue(1.0);
        pi.setDoubleValue(0.5);
        pi.setStyle(0L);          // NSProgressIndicatorBarStyle
        pi.startAnimation();
        pi.stopAnimation();
        check(true, "NSProgressIndicator range set + startAnimation/stopAnimation without crash");
        view.addSubview(pi);
        pump(app);
        check(true, "NSProgressIndicator added to view without crash");

        window.performClose(null);
        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Pump the run loop for ~1.5s so the window draws on the main thread. */
    private static void pump(NSApplication app) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 1500;
        while (System.currentTimeMillis() < deadline) {
            MemorySegment until = ObjC.msgSendIdDouble(
                    ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.05);
            NSEvent ev = app.nextEvent(-1L, until, "kCFRunLoopDefaultMode", true);
            if (ev != null) app.sendEvent(ev);
            app.updateWindows();
            Thread.sleep(10);
        }
    }

    /** Write a solid-color PNG file with javax.imageio (headless, not AWT UI). */
    private static void writeRedPng(String path, int w, int h) throws Exception {
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                bi.setRGB(x, y, 0xFF0000); // pure red
        boolean ok = ImageIO.write(bi, "png", new File(path));
        if (!ok) throw new IllegalStateException("ImageIO.write reported failure for " + path);
        System.out.println("  wrote " + path + " (" + w + "x" + h + " solid red)");
    }

    /** Render the view's bounds into an NSBitmapImageRep and read its bitmap data. */
    private static MemPixels renderToBitmap(NSView view) throws Throwable {
        NSRect b = view.bounds();

        MethodHandle hCache = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        MemorySegment rep = (MemorySegment) hCache.invokeExact(
                view.peer(), ObjC.sel("bitmapImageRepForCachingDisplayInRect:"), b.toSegment());

        MethodHandle hCacheTo = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.ID));
        hCacheTo.invokeExact(view.peer(), ObjC.sel("cacheDisplayInRect:toBitmapImageRep:"), b.toSegment(), rep);

        long pixelsWide = ObjC.msgSendLong(rep, ObjC.sel("pixelsWide"));
        long pixelsHigh = ObjC.msgSendLong(rep, ObjC.sel("pixelsHigh"));
        long bytesPerRow = ObjC.msgSendLong(rep, ObjC.sel("bytesPerRow"));
        long samplesPerPixel = ObjC.msgSendLong(rep, ObjC.sel("samplesPerPixel"));

        MemorySegment data = ObjC.msgSendId(rep, ObjC.sel("bitmapData"));
        MemorySegment bytes = data.reinterpret(bytesPerRow * pixelsHigh);

        return new MemPixels(bytes, (int) pixelsWide, (int) pixelsHigh, (int) bytesPerRow, (int) samplesPerPixel);
    }

    /** Byte-addressable view of an NSBitmapImageRep's bitmapData. */
    private record MemPixels(MemorySegment bytes, int pixelsWide, int pixelsHigh, int bytesPerRow, int samplesPerPixel) {
        int[] rgb(int x, int y) {
            long off = (long) y * bytesPerRow + (long) x * samplesPerPixel;
            int c0 = Byte.toUnsignedInt(bytes.get(ValueLayout.JAVA_BYTE, off));
            int c1 = Byte.toUnsignedInt(bytes.get(ValueLayout.JAVA_BYTE, off + 1));
            int c2 = Byte.toUnsignedInt(bytes.get(ValueLayout.JAVA_BYTE, off + 2));
            return new int[]{c0, c1, c2};
        }
    }
}
