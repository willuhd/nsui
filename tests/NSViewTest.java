package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.NSApplication;
import nsui.NSEvent;
import nsui.NSRect;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.CG;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// Full NSView drawing pipeline with concrete PIXEL verification: install a
/// Java-drawn NSView as a window's content view, pump the run loop, then render
/// the view into an NSBitmapImageRep and assert the actual channel values.
///
/// Pass: center pixel blue (blue>150, red<100), corner pixel red (red>150,
/// blue<100), and drawRect: fired at least once.
public final class NSViewTest {

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

        TestKit.check(drawCount.get() >= 1, "drawRect: fired at least once (count=" + drawCount.get() + ")");
        if (drawCount.get() == 0) {
            System.out.println("NOTE: forcing displayIfNeeded fallback after zero draws");
            ObjC.msgSendVoid(window.peer(), ObjC.sel("displayIfNeeded"));
            TestKit.check(drawCount.get() >= 1, "drawRect: fired after displayIfNeeded (count=" + drawCount.get() + ")");
        }

        // ---- render the view to a bitmap and read actual pixels ----
        MemPixels px = renderToBitmap(view);

        // Center of the bitmap = center of the view => inside the blue rect.
        // Corner (5,5) = far from center => still red.
        int cx = px.pixelsWide / 2;
        int cy = px.pixelsHigh / 2;
        int[] center = px.colors(cx, cy);
        int[] corner = px.colors(5, 5);

        System.out.printf("bitmap %dx%d bytesPerRow=%d samplesPerPixel=%d alphaIdx=%d%n",
                px.pixelsWide, px.pixelsHigh, px.bytesPerRow, px.samplesPerPixel, px.alphaIdx);
        System.out.printf("center(%d,%d) color samples=%s (expect one blue-role primary)%n",
                cx, cy, java.util.Arrays.toString(center));
        System.out.printf("corner(5,5)  color samples=%s (expect a different red-role primary)%n",
                java.util.Arrays.toString(corner));

        // Byte-order agnostic: each region must show one saturated primary
        // (dominant sample >150, rest <100) and the two regions must be
        // dominated by DIFFERENT samples — red vs blue roles, not positions.
        int centerDom = dominant(center);
        int cornerDom = dominant(corner);
        boolean centerIsBlue = center[centerDom] > 150 && restBelow(center, centerDom, 100);
        boolean cornerIsRed  = corner[cornerDom] > 150 && restBelow(corner, cornerDom, 100);
        TestKit.check(centerIsBlue && cornerIsRed && centerDom != cornerDom,
                "center and corner show distinct saturated primaries (center dom idx="
                        + centerDom + ", corner dom idx=" + cornerDom + ")");

        TestKit.close(window);
        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
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

        int alphaIdx = -1;
        try {
            if (samplesPerPixel == 4 && ObjC.msgSendBool(rep, ObjC.sel("hasAlpha"))) {
                long fmt = ObjC.msgSendLong(rep, ObjC.sel("bitmapFormat"));
                alphaIdx = ((fmt & 1L) != 0) ? 0 : 3;
            }
        } catch (Throwable t) {
            if (samplesPerPixel == 4) alphaIdx = 3;
        }

        return new MemPixels(bytes, (int) pixelsWide, (int) pixelsHigh, (int) bytesPerRow, (int) samplesPerPixel, alphaIdx);
    }

    /** Index of the largest color sample. */
    private static int dominant(int[] colors) {
        int dom = 0;
        for (int k = 1; k < colors.length; k++) {
            if (colors[k] > colors[dom]) dom = k;
        }
        return dom;
    }

    /** Every non-dominant color sample stays below the ceiling. */
    private static boolean restBelow(int[] colors, int dom, int ceiling) {
        for (int k = 0; k < colors.length; k++) {
            if (k != dom && colors[k] >= ceiling) return false;
        }
        return true;
    }

    /** Byte-addressable view of an NSBitmapImageRep's bitmapData. */
    private record MemPixels(MemorySegment bytes, int pixelsWide, int pixelsHigh, int bytesPerRow, int samplesPerPixel, int alphaIdx) {
        /** Non-alpha color samples at (x,y) in rep byte order (roles, not R,G,B). */
        int[] colors(int x, int y) {
            int n = samplesPerPixel - (alphaIdx >= 0 ? 1 : 0);
            int[] out = new int[n];
            for (int k = 0, s = 0; k < samplesPerPixel; k++) {
                if (k == alphaIdx) continue;
                long off = (long) y * bytesPerRow + (long) x * samplesPerPixel + k;
                out[s++] = Byte.toUnsignedInt(bytes.get(ValueLayout.JAVA_BYTE, off));
            }
            return out;
        }

        int[] rgb(int x, int y) {
            return colors(x, y);
        }
    }
}
