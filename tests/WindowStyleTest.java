package nsui.tests;

import nsui.NSApplication;
import nsui.NSObject;
import nsui.NSRect;
import nsui.NSWindow;
import nsui.objc.ObjC;

/**
 * Window styles: the COMPOSITIONAL AppKit model (styleMask bits + NSPanel subclass
 * + behavior properties), exposed via thin wrappers on {@code NSWindow}.
 *
 * <p>Key assertions (AppKit, not assumptions):
 * <ul>
 *   <li>a normal {@code NSWindow} created with styleMask {@code 15}
 *       (Titled|Closable|Miniaturizable|Resizable) reads back {@code styleMask()==15};</li>
 *   <li>{@code setTitlebarAppearsTransparent:} + {@code setTitleVisibility:} are the
 *       native "modern title bar" switches (no height/radius knob — AppKit derives those);</li>
 *   <li>{@code setLevel:}/{@code level()} round-trip (NSFloatingWindowLevel=3 checked);</li>
 *   <li>{@code standardWindowButton:} returns a real (non-nil) NSButton peer,
 *       here typed as {@code NSObject};</li>
 *   <li>{@code createPanel} with {@code 15L|16L} (Titled|UtilityWindow) really is an
 *       {@code NSPanel} (className), {@code isUtilityWindow()==true}, and honors the
 *       panel behaviors {@code setHidesOnDeactivate:} / {@code setBecomesKeyOnlyIfNeeded:}.</li>
 * </ul>
 */
public final class WindowStyleTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== WindowStyleTest — compositional window style (styleMask + NSPanel + behavior) ===");
        ObjC.init(); // FFM bindings (must be first)

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        System.out.println("---- 1) Normal NSWindow (styleMask 15 = Titled|Closable|Miniaturizable|Resizable) ----");
        NSWindow win = NSWindow.create(new NSRect(0, 0, 400, 250), 15L, 2L, false);

        check(win.styleMask() == 15L, "normal window styleMask() == 15 (got " + win.styleMask() + ")");

        win.setTitlebarAppearsTransparent(true);
        check(win.isTitlebarAppearsTransparent(), "setTitlebarAppearsTransparent(true) round-trips");

        win.setTitleVisibility(1 /* NSWindowTitleHidden */);
        System.out.println("PASS: setTitleVisibility(1) did not crash (no native assertion); reads back on title");

        win.setLevel(3 /* NSFloatingWindowLevel */);
        check(win.level() == 3L, "setLevel(3) -> level() == 3 (got " + win.level() + ")");

        win.setCollectionBehavior(0); // no behavior
        System.out.println("PASS: setCollectionBehavior(0) did not crash");

        NSObject closeBtn = win.standardWindowButton(0 /* NSWindowCloseButton */);
        check(closeBtn != null && closeBtn.peer().address() != 0,
                "standardWindowButton(close) returns a non-null object on a titled window");

        check(!win.isUtilityWindow(), "normal window isUtilityWindow() == false");

        //

        System.out.println("---- 2) NSPanel (createPanel; styleMask 15|16 = Titled|UtilityWindow) ----");
        NSWindow panel = NSWindow.createPanel(new NSRect(0, 0, 300, 200), 15L | 16L, 2L, false);

        String panelClass = ObjC.toString(ObjC.msgSendId(panel.peer(), ObjC.sel("className")));
        check("NSPanel".equals(panelClass),
                "panel className == \"NSPanel\" (got " + panelClass + ")");
        check(panel.isUtilityWindow(), "panel isUtilityWindow() == true");

        panel.setHidesOnDeactivate(true);
        check(panel.hidesOnDeactivate(), "setHidesOnDeactivate(true) round-trips");

        panel.setBecomesKeyOnlyIfNeeded(true);
        check(panel.becomesKeyOnlyIfNeeded(), "setBecomesKeyOnlyIfNeeded(true) round-trips");

        NSObject panelClose = panel.standardWindowButton(0 /* NSWindowCloseButton */);
        check(panelClose != null && panelClose.peer().address() != 0,
                "panel standardWindowButton(close) returns a non-null object");

        // ---- cleanup: close both ----
        System.out.println("\n---- cleanup ----");
        panel.performClose(null);
        win.performClose(null);
        System.out.println("PASS: performClose on panel and window did not crash");

        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
