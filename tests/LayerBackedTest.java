package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.NSApplication;
import nsui.NSEvent;
import nsui.NSObject;
import nsui.NSRect;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.CG;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * Layer-backed rendering: verifies that a view with {@code wantsLayer(true)} still draws
 * correctly through the Java {@code Drawable}, with concrete pixel verification (red fill
 * + blue centered rect), and that {@link NSView#backingScaleFactor()} is positive.
 *
 * <p>{@code setWantsLayer(true)} is applied BEFORE the view is installed as the window's
 * content view. Layer-backed views render into CoreAnimation layers; passing this test
 * requires the manual run-loop pump (with a {@code displayIfNeeded} fallback) to drive
 * the layer commit.
 */
public final class LayerBackedTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== LayerBackedTest — layer-backed rendering + backingScaleFactor ===");
        ObjC.init();           // FFM bindings (must be first)
        CG.ensureInit();       // CoreGraphics 2D downcalls

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        NSWindow window = NSWindow.create(new NSRect(0, 0, 500, 400), 15L, 2L, false);
        window.setTitle("LayerBacked test");
        window.center();
        window.setReleasedWhenClosed(false);

        AtomicInteger drawCount = new AtomicInteger();
        NSView.Drawable drawable = (ctx, dirtyRect) -> {
            drawCount.incrementAndGet();
            CG.setRGBFillColor(ctx, 1.0, 0.0, 0.0, 1.0);   // red background
            CG.fillRect(ctx, dirtyRect.x(), dirtyRect.y(), dirtyRect.width(), dirtyRect.height());
            CG.setRGBFillColor(ctx, 0.0, 0.0, 1.0, 1.0);   // blue centered rect
            CG.fillRect(ctx, dirtyRect.width() / 4, dirtyRect.height() / 4,
                    dirtyRect.width() / 2, dirtyRect.height() / 2);
        };

        NSView view = NSView.create(new NSRect(0, 0, 500, 400), drawable);
        view.setWantsLayer(true);   // layer-backed BEFORE installation
        window.setContentView(view);
        view.setNeedsDisplay(true);

        app.activateIgnoringOtherApps(true);
        window.makeKeyAndOrderFront(null);
        app.finishLaunching();

        long deadline = System.currentTimeMillis() + 1500;
        while (System.currentTimeMillis() < deadline) {
            MemorySegment until = ObjC.msgSendIdDouble(
                    ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.05);
            NSEvent ev = app.nextEvent(-1L, until, "kCFRunLoopDefaultMode", true);
            if (ev != null) app.sendEvent(ev);
            app.updateWindows();
            Thread.sleep(10);
        }

        check(drawCount.get() >= 1, "layer-backed drawRect: fired at least once (count=" + drawCount.get() + ")");
        if (drawCount.get() == 0) {
            System.out.println("NOTE: forcing displayIfNeeded fallback after zero draws");
            ObjC.msgSendVoid(window.peer(), ObjC.sel("displayIfNeeded"));
            check(drawCount.get() >= 1, "layer-backed drawRect: fired after displayIfNeeded (count=" + drawCount.get() + ")");
        }

        double scale = view.backingScaleFactor();
        System.out.printf("backingScaleFactor=%.3f%n", scale);
        check(scale > 0, "backingScaleFactor() > 0 (" + scale + ")");

        // ---- render the layer-backed view to a bitmap and read actual pixels ----
        MemPixels px = renderToBitmap(view);

        int cx = px.pixelsWide / 2;
        int cy = px.pixelsHigh / 2;
        int[] center = px.rgb(cx, cy);
        int[] corner = px.rgb(5, 5);

        System.out.printf("bitmap %dx%d bytesPerRow=%d samplesPerPixel=%d%n",
                px.pixelsWide, px.pixelsHigh, px.bytesPerRow, px.samplesPerPixel);
        System.out.printf("center(%d,%d) channel[0,1,2]=[%d,%d,%d] (expect blue)%n",
                cx, cy, center[0], center[1], center[2]);
        System.out.printf("corner(5,5)  channel[0,1,2]=[%d,%d,%d] (expect red)%n",
                corner[0], corner[1], corner[2]);

        boolean centerIsBlue = center[2] > 150 && center[0] < 100;
        boolean cornerIsRed  = corner[0] > 150 && corner[2] < 100;
        check(centerIsBlue, "layer-backed center pixel is BLUE (b>150, r<100)");
        check(cornerIsRed, "layer-backed corner pixel is RED (r>150, b<100)");

        window.performClose(null);
        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Render the view's bounds into an NSBitmapImageRep and read its bitmap data. */
    private static MemPixels renderToBitmap(NSView view) throws Throwable {
        NSRect b = view.bounds();

        MethodHandle hCache = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        MemorySegment rep = (MemorySegment) hCache.invokeExact(view.peer(),
                ObjC.sel("bitmapImageRepForCachingDisplayInRect:"), b.toSegment());

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
