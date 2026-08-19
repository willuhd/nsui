package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.NSApplication;
import nsui.NSEvent;
import nsui.NSObject;
import nsui.NSRect;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.CG;
import nsui.objc.ObjC;

/**
 * Dirty-rect redraw: prove that {@code setNeedsDisplayInRect:} reaches the Java
 * {@code Drawable} with the SUB-rect (not the full 500-wide bounds), and that a
 * subsequent full {@code setNeedsDisplay(true)} comes back full-size.
 *
 * <p>Pass criteria:</p>
 * <ul>
 *   <li>{@code draws >= 1}</li>
 *   <li>the LAST dirty rect recorded after a sub-rect invalidate is small
 *       ({@code width < 200}, i.e. NOT the full 500-wide bounds);</li>
 *   <li>a full invalidate afterwards yields a full-size dirty rect</li>
 *       ({@code width >= 499}).</li>
 * </ul>
 */
public final class DirtyRectTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== DirtyRectTest — dirty-rect redraw reaches Drawable with sub-rect ===");
        ObjC.init();           // FFM bindings (must be first)
        CG.ensureInit();       // CoreGraphics 2D downcalls

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        NSWindow window = NSWindow.create(new NSRect(0, 0, 500, 400), 15L, 2L, false);
        window.setTitle("DirtyRect test");
        window.center();
        window.setReleasedWhenClosed(false);

        AtomicInteger draws = new AtomicInteger();
        double[] lastDirty = { -1.0, -1.0 };

        NSView.Drawable drawable = (ctx, dirtyRect) -> {
            draws.incrementAndGet();
            lastDirty[0] = dirtyRect.width();
            lastDirty[1] = dirtyRect.height();
            CG.setRGBFillColor(ctx, 1.0, 0.0, 0.0, 1.0);
            CG.fillRect(ctx, dirtyRect.x(), dirtyRect.y(), dirtyRect.width(), dirtyRect.height());
        };

        // view fills the content view (500x400 points)
        NSView view = NSView.create(new NSRect(0, 0, 500, 400), drawable);
        window.setContentView(view);
        view.setNeedsDisplay(true);

        app.activateIgnoringOtherApps(true);
        window.makeKeyAndOrderFront(null);
        app.finishLaunching();

        // Phase 0: pump until the initial full redraw has happened and drained.
        pump(app, 250);
        System.out.printf("after initial pump: draws=%d lastDirty=[%.1f x %.1f]%n",
                draws.get(), lastDirty[0], lastDirty[1]);
        check(draws.get() >= 1, "initial full redraw fired (draws=" + draws.get() + ")");

        // Phase 1: invalidate a sub-rect in the view's coordinate system.
        view.setNeedsDisplayInRect(new NSRect(10, 10, 60, 40));
        pump(app, 350);
        System.out.printf("after sub-rect invalidate: draws=%d lastDirty=[%.1f x %.1f]%n",
                draws.get(), lastDirty[0], lastDirty[1]);

        check(lastDirty[0] < 200, "sub-rect dirty WIDTH " + lastDirty[0] + " is small (<200, not the full 500)");
        check(lastDirty[1] < 200, "sub-rect dirty HEIGHT " + lastDirty[1] + " is small (<200)");

        // Phase 2: full invalidate -> dirty rect must come back full-size.
        int beforeFull = draws.get();
        view.setNeedsDisplay(true);
        pump(app, 350);
        System.out.printf("after full invalidate: draws=%d lastDirty=[%.1f x %.1f]%n",
                draws.get(), lastDirty[0], lastDirty[1]);
        check(draws.get() > beforeFull, "full invalidate produced another draw (draws " + beforeFull + " -> " + draws.get() + ")");
        check(lastDirty[0] >= 499, "full invalidate dirty WIDTH " + lastDirty[0] + " is full-size (>=499)");

        window.performClose(null);
        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Manual run-loop pump for the given duration (like NSViewTest). */
    private static void pump(NSApplication app, long millis) throws Exception {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            MemorySegment until = ObjC.msgSendIdDouble(
                    ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.05);
            NSEvent ev = app.nextEvent(-1L, until, "kCFRunLoopDefaultMode", true);
            if (ev != null) app.sendEvent(ev);
            app.updateWindows();
            Thread.sleep(10);
        }
    }
}
