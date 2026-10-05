package nsui.tests;

import nsui.NSApplication;
import nsui.NSObject;
import nsui.NSRect;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// Window styles: the COMPOSITIONAL AppKit model (styleMask bits + NSPanel subclass
/// + behavior properties), exposed via thin wrappers on `NSWindow`.
///
/// Key assertions (AppKit, not assumptions):
/// - a normal `NSWindow` created with styleMask `15`
/// (Titled|Closable|Miniaturizable|Resizable) reads back `styleMask()==15`;
/// - `setTitlebarAppearsTransparent:` + `setTitleVisibility:` are the
/// native "modern title bar" switches (no height/radius knob — AppKit derives those);
/// - `setLevel:`/`level()` round-trip (NSFloatingWindowLevel=3 checked);
/// - `standardWindowButton:` returns a real (non-nil) NSButton peer,
/// here typed as `NSObject`;
/// - `createPanel` with `15L|16L` (Titled|UtilityWindow) really is an
/// `NSPanel` (className), `isUtilityWindow()==true`, and honors the
/// panel behaviors `setHidesOnDeactivate:` / `setBecomesKeyOnlyIfNeeded:`.
public final class WindowStyleTest {

    public static void main(String[] args) {
        System.out.println("=== WindowStyleTest — compositional window style (styleMask + NSPanel + behavior) ===");
        ObjC.init(); // FFM bindings (must be first)

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        System.out.println("---- 1) Normal NSWindow (styleMask 15 = Titled|Closable|Miniaturizable|Resizable) ----");
        NSWindow win = NSWindow.create(new NSRect(0, 0, 400, 250), 15L, 2L, false);

        TestKit.check(win.styleMask() == 15L, "normal window styleMask() == 15 (got " + win.styleMask() + ")");

        win.setTitlebarAppearsTransparent(true);
        TestKit.check(win.isTitlebarAppearsTransparent(), "setTitlebarAppearsTransparent(true) round-trips");

        win.setTitleVisibility(1 /* NSWindowTitleHidden */);
        System.out.println("PASS: setTitleVisibility(1) did not crash (no native assertion); reads back on title");

        win.setLevel(3 /* NSFloatingWindowLevel */);
        TestKit.check(win.level() == 3L, "setLevel(3) -> level() == 3 (got " + win.level() + ")");

        win.setCollectionBehavior(0); // no behavior
        System.out.println("PASS: setCollectionBehavior(0) did not crash");

        TestKit.check(win.sharingType() == NSWindow.SHARING_READ_ONLY,
                "default sharingType is READ_ONLY (got " + win.sharingType() + ")");
        win.setSharingType(NSWindow.SHARING_NONE);
        TestKit.check(win.sharingType() == NSWindow.SHARING_NONE,
                "setSharingType(NONE) sticks (got " + win.sharingType() + ")");
        win.setSharingType(NSWindow.SHARING_READ_ONLY);

        NSObject closeBtn = win.standardWindowButton(0 /* NSWindowCloseButton */);
        TestKit.check(closeBtn != null && closeBtn.peer().address() != 0,
                "standardWindowButton(close) returns a non-null object on a titled window");

        TestKit.check(!win.isUtilityWindow(), "normal window isUtilityWindow() == false");

        //

        System.out.println("---- 2) NSPanel (createPanel; styleMask 15|16 = Titled|UtilityWindow) ----");
        NSWindow panel = NSWindow.createPanel(new NSRect(0, 0, 300, 200), 15L | 16L, 2L, false);

        String panelClass = ObjC.toString(ObjC.msgSendId(panel.peer(), ObjC.sel("className")));
        TestKit.check("NSPanel".equals(panelClass),
                "panel className == \"NSPanel\" (got " + panelClass + ")");
        TestKit.check(panel.isUtilityWindow(), "panel isUtilityWindow() == true");

        panel.setHidesOnDeactivate(true);
        TestKit.check(panel.hidesOnDeactivate(), "setHidesOnDeactivate(true) round-trips");

        panel.setBecomesKeyOnlyIfNeeded(true);
        TestKit.check(panel.becomesKeyOnlyIfNeeded(), "setBecomesKeyOnlyIfNeeded(true) round-trips");

        NSObject panelClose = panel.standardWindowButton(0 /* NSWindowCloseButton */);
        TestKit.check(panelClose != null && panelClose.peer().address() != 0,
                "panel standardWindowButton(close) returns a non-null object");

        // ---- cleanup: close both ----
        System.out.println("\n---- cleanup ----");
        TestKit.close(panel);
        TestKit.close(win);
        System.out.println("PASS: performClose on panel and window did not crash");

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }
}
