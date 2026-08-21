package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSArray;
import nsui.NSDictionary;
import nsui.NSPanel;
import nsui.NSRect;
import nsui.NSScreen;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// ScreenPanelTest — Tier-1 coverage for the `NSScreen` and `NSPanel`
/// wrappers. Non-interactive and self-terminating: prints PASS:/FAIL: lines,
/// ends with RESULT: PASS or RESULT: FAIL, exits 0 or 1. Skips gracefully when
/// AppKit cannot initialize (non-macOS host / connection error).
///
/// Note: no explicit NSApplication.shared() warm-up — `[NSScreen screens]`
/// and panel creation do not require it, and keeping this test self-contained
/// lets it compile against the NSScreen/NSPanel slice alone.
public final class ScreenPanelTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== ScreenPanelTest ===");
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

        // ---------------- NSScreen ----------------
        try {
            NSArray screens = NSScreen.screens();
            check(screens != null && screens.count() >= 1,
                    "NSScreen.screens count >= 1 (got " + (screens == null ? "nil array" : screens.count()) + ")");

            NSScreen main = NSScreen.mainScreen();
            check(main != null && main.peer().address() != 0, "NSScreen.mainScreen non-nil");
            check(main != null && main.isKindOfClass("NSScreen"), "mainScreen isKindOfClass NSScreen");

            if (main != null) {
                NSRect frame = main.frame();
                check(frame.width() > 0 && frame.height() > 0,
                        "mainScreen frame width>0 && height>0 (got " + frame + ")");

                NSRect visible = main.visibleFrame();
                check(frame.contains(visible),
                        "visibleFrame within frame bounds (frame=" + frame + ", visible=" + visible + ")");

                double scale = main.backingScaleFactor();
                check(scale >= 1.0, "backingScaleFactor >= 1.0 (got " + scale + ")");

                NSDictionary desc = main.deviceDescription();
                check(desc != null && desc.count() > 0,
                        "deviceDescription non-null and non-empty (count=" + (desc == null ? -1 : desc.count()) + ")");
            }

            check(NSScreen.wrap(null) == null && NSScreen.wrap(MemorySegment.NULL) == null,
                    "NSScreen.wrap(null/NULL) returns null");
        } catch (Throwable t) {
            check(false, "NSScreen section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSPanel ----------------
        try {
            long style = NSWindow.StyleMask.titled.value | NSWindow.StyleMask.closable.value;
            NSPanel panel = NSPanel.create(new NSRect(0, 0, 320, 200), style, true);
            check(panel != null && panel.peer().address() != 0, "NSPanel.create non-nil");
            check(panel != null && panel.isKindOfClass("NSPanel"), "panel isKindOfClass NSPanel");

            if (panel != null) {
                // inherited window sanity — proves the NSWindow superclass wiring
                check(panel.styleMask() == style,
                        "inherited styleMask round-trip (got " + panel.styleMask() + " expect " + style + ")");
                panel.setTitle("nsui panel");
                check("nsui panel".equals(panel.title()),
                        "inherited setTitle/title round-trip (got \"" + panel.title() + "\")");

                // becomesKeyOnlyIfNeeded round-trip
                panel.becomesKeyOnlyIfNeeded(true);
                check(panel.becomesKeyOnlyIfNeeded(), "becomesKeyOnlyIfNeeded(true) reads back true");
                panel.becomesKeyOnlyIfNeeded(false);
                check(!panel.becomesKeyOnlyIfNeeded(), "becomesKeyOnlyIfNeeded(false) reads back false");

                // isFloatingPanel round-trip
                panel.setFloatingPanel(true);
                check(panel.isFloatingPanel(), "setFloatingPanel(true) reads back true");
                panel.setFloatingPanel(false);
                check(!panel.isFloatingPanel(), "setFloatingPanel(false) reads back false");

                // worksWhenModal — inherited accessor must respond without crashing (default false)
                boolean works = panel.worksWhenModal();
                check(true, "worksWhenModal accessor no crash (got " + works + ")");
            }

            check(NSPanel.wrap(null) == null && NSPanel.wrap(MemorySegment.NULL) == null,
                    "NSPanel.wrap(null/NULL) returns null");
        } catch (Throwable t) {
            check(false, "NSPanel section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(failures == 0
                ? "RESULT: PASS (" + asserts + " assertions)"
                : "RESULT: FAIL (" + failures + " of " + asserts + " assertions failed)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
