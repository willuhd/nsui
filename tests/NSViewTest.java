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
 * Full NSView drawing pipeline with concrete PIXEL verification: install a
 * Java-drawn NSView as a window's content view, pump the run loop, then render
 * the view into an NSBitmapImageRep and assert the actual channel values.
 *
 * <p>Pass: center pixel blue (blue>150, red<100), corner pixel red (red>150,
 * blue<100), and drawRect: fired at least once.
 */
public final class NSViewTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== NSViewTest — drawRect: pipeline + pixel verification ===");
        ObjC.init();           // FFM bindings (must be first)
        CG.ensureInit();       // CoreGraphics 2D downcalls

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        NSWindow window = NSWindow.create(new NSRect(0, 0, 600, 400), 15L, 2L, false);
        window.setTitle("NSView test");
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

        NSView view = NSView.create(new NSRect(0, 0, 600, 400), drawable);
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

        check(drawCount.get() >= 1, "drawRect: fired at least once (count=" + drawCount.get() + ")");
        if (drawCount.get() == 0) {
            System.out.println("NOTE: forcing displayIfNeeded fallback after zero draws");
            ObjC.msgSendVoid(window.peer(), ObjC.sel("displayIfNeeded"));
            check(drawCount.get() >= 1, "drawRect: fired after displayIfNeeded (count=" + drawCount.get() + ")");
        }

        // ---- render the view to a bitmap and read actual pixels ----
        MemPixels px = renderToBitmap(view);

        // Center of the bitmap = center of the view => inside the blue rect.
        // Corner (5,5) = far from center => still red.
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

        // Channel order for premultiplied/color-managed reps can vary; verify by role:
        // a "blue" pixel has the BLUE-ish sample dominant, a "red" pixel the RED-ish one.
        boolean centerIsBlue = center[2] > 150 && center[0] < 100;
        boolean cornerIsRed  = corner[0] > 150 && corner[2] < 100;
        check(centerIsBlue, "center pixel is BLUE (b>150, r<100)");
        check(cornerIsRed, "corner pixel is RED (r>150, b<100)");

        window.performClose(null);
        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Render the view's bounds into an NSBitmapImageRep and read its bitmap data. */
    private static MemPixels renderToBitmap(NSView view) throws Throwable {
        NSRect b = view.bounds();

        // bitmapImageRepForCachingDisplayInRect: (id, NSRect) -> id
        MethodHandle hCache = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        MemorySegment rep = (MemorySegment) hCache.invokeExact(view.peer(), ObjC.sel("bitmapImageRepForCachingDisplayInRect:"), b.toSegment());

        // cacheDisplayInRect:toBitmapImageRep: (id, NSRect, id) -> void
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
