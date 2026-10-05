package nsui.tests;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.NSApplication;
import nsui.NSRect;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.CG;
import nsui.objc.ObjC;

/// Dirty-rect redraw: prove that `setNeedsDisplayInRect:` reaches the Java
/// `Drawable` with the SUB-rect (not the full 500-wide bounds), and that a
/// subsequent full `setNeedsDisplay(true)` comes back full-size.
///
/// Pass criteria:
/// - `draws >= 1`
/// - the LAST dirty rect recorded after a sub-rect invalidate is small
/// (`width < 200`, i.e. NOT the full 500-wide bounds);
/// - a full invalidate afterwards yields a full-size dirty rect
/// (`width >= 499`).
public final class DirtyRectTest {

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

        app.finishLaunching();

        // Phase 0: pump until the initial full redraw has happened and drained.
        TestKit.pump(app, 250);
        System.out.printf("after initial pump: draws=%d lastDirty=[%.1f x %.1f]%n",
                draws.get(), lastDirty[0], lastDirty[1]);
        TestKit.check(draws.get() >= 1, "initial full redraw fired (draws=" + draws.get() + ")");

        // Phase 1: invalidate a sub-rect in the view's coordinate system.
        view.setNeedsDisplayInRect(new NSRect(10, 10, 60, 40));
        TestKit.pump(app, 350);
        System.out.printf("after sub-rect invalidate: draws=%d lastDirty=[%.1f x %.1f]%n",
                draws.get(), lastDirty[0], lastDirty[1]);

        TestKit.check(lastDirty[0] < 200, "sub-rect dirty WIDTH " + lastDirty[0] + " is small (<200, not the full 500)");
        TestKit.check(lastDirty[1] < 200, "sub-rect dirty HEIGHT " + lastDirty[1] + " is small (<200)");

        // Phase 2: full invalidate -> dirty rect must come back full-size.
        int beforeFull = draws.get();
        view.setNeedsDisplay(true);
        TestKit.pump(app, 350);
        System.out.printf("after full invalidate: draws=%d lastDirty=[%.1f x %.1f]%n",
                draws.get(), lastDirty[0], lastDirty[1]);
        TestKit.check(draws.get() > beforeFull, "full invalidate produced another draw (draws " + beforeFull + " -> " + draws.get() + ")");
        TestKit.check(lastDirty[0] >= 499, "full invalidate dirty WIDTH " + lastDirty[0] + " is full-size (>=499)");

        TestKit.close(window);
        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }

    /** Manual run-loop pump for the given duration (like NSViewTest). */
    
}
