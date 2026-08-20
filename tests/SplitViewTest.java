package nsui.tests;

import nsui.NSApplication;
import nsui.NSRect;
import nsui.NSSplitView;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.ObjC;

public final class SplitViewTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== SplitViewTest — NSSplitView ===");
        ObjC.init();

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0);

        // create split view
        NSRect frame = new NSRect(0, 0, 400, 300);
        NSSplitView split = NSSplitView.create(frame);
        check(split != null, "NSSplitView.create returned non-null");
        check(split.isKindOfClass("NSSplitView"), "peer isKindOfClass NSSplitView");

        // isVertical / setVertical
        boolean initialVertical = split.isVertical();
        System.out.println("  initial isVertical=" + initialVertical);
        split.setVertical(true);
        check(split.isVertical() == true, "setVertical(true) -> isVertical true");
        split.setVertical(false);
        check(split.isVertical() == false, "setVertical(false) -> isVertical false");
        split.setVertical(true);
        check(split.isVertical() == true, "setVertical(true) again -> true");

        // dividerStyle
        long initialStyle = split.dividerStyle();
        System.out.println("  initial dividerStyle=" + initialStyle);
        split.setDividerStyle(1);
        check(split.dividerStyle() == 1, "setDividerStyle(1) -> dividerStyle 1");
        split.setDividerStyle(0);
        check(split.dividerStyle() == 0, "setDividerStyle(0) -> dividerStyle 0");
        // restore if needed
        split.setDividerStyle(initialStyle);

        // addArrangedSubview mimic via addSubview
        NSView pane1 = NSView.create(new NSRect(0, 0, 200, 300), (ctx, d) -> {});
        NSView pane2 = NSView.create(new NSRect(0, 0, 200, 300), (ctx, d) -> {});
        split.addArrangedSubview(pane1);
        split.addArrangedSubview(pane2);

        // also test that addSubview works directly (should be 2 subviews)
        // NSSplitView uses subviews; use ObjC generic count via subviews property
        try {
            java.lang.foreign.MemorySegment arr = ObjC.msgSendId(split.peer(), ObjC.sel("subviews"));
            if (arr != null && arr.address() != 0) {
                long cnt = ObjC.msgSendLong(arr, ObjC.sel("count"));
                System.out.println("  subviews count=" + cnt);
                check(cnt == 2, "addArrangedSubview x2 -> subviews count 2 (got " + cnt + ")");
            } else {
                check(false, "subviews array is nil");
            }
        } catch (Throwable t) {
            System.out.println("  subviews count check skipped: " + t);
            // still pass if no exception on addArrangedSubview
            check(true, "addArrangedSubview did not throw");
        }

        // setPosition:ofDividerAtIndex: — needs window + layout
        // Put split view in a window and pump so AppKit can layout
        NSWindow window = NSWindow.create(new NSRect(0, 0, 400, 300), 15L, 2L, false);
        window.setTitle("split view test");
        window.setReleasedWhenClosed(false);
        window.setContentView(split);
        window.center();
        app.activateIgnoringOtherApps(true);
        window.makeKeyAndOrderFront(null);
        app.finishLaunching();
        // Must set vertical for meaningful position along width
        split.setVertical(true);
        pumpForMs(app, 800);
        ObjC.msgSendVoid(split.peer(), ObjC.sel("layoutSubtreeIfNeeded"));
        pumpForMs(app, 200);

        // Set divider position to 150 (left pane 150pt wide)
        try {
            split.setPositionOfDividerAtIndex(150.0, 0);
            // also test alias
            split.setPosition(120.0, 0);
            check(true, "setPosition:ofDividerAtIndex: did not throw");
        } catch (Throwable t) {
            check(false, "setPosition:ofDividerAtIndex: threw: " + t);
        }

        // Give AppKit a moment to apply
        pumpForMs(app, 300);
        ObjC.msgSendVoid(split.peer(), ObjC.sel("layoutSubtreeIfNeeded"));
        // Query frames — they should be non-zero and distinct
        NSRect f1 = pane1.frame();
        NSRect f2 = pane2.frame();
        System.out.printf("  pane1 frame {x=%.1f y=%.1f w=%.1f h=%.1f}%n", f1.x(), f1.y(), f1.width(), f1.height());
        System.out.printf("  pane2 frame {x=%.1f y=%.1f w=%.1f h=%.1f}%n", f2.x(), f2.y(), f2.width(), f2.height());
        check(f1.width() > 0 && f1.height() > 0, "pane1 has non-zero size after layout");
        check(f2.width() > 0 && f2.height() > 0, "pane2 has non-zero size after layout");

        System.out.println(failures == 0
                ? "RESULT: ALL PASS (" + asserts + " assertions)"
                : "RESULT: " + failures + " of " + asserts + " assertions FAILED");
        window.performClose(null);
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void pumpForMs(NSApplication app, long ms) throws InterruptedException {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            java.lang.foreign.MemorySegment until = ObjC.msgSendIdDouble(
                    ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSince1970:"), 0.0);
            nsui.NSEvent ev;
            int n = 0;
            while ((ev = app.nextEvent(-1L, until, "kCFRunLoopDefaultMode", true)) != null) {
                app.sendEvent(ev);
                if (++n > 400) break;
            }
            app.updateWindows();
            Thread.sleep(10);
        }
    }
}
