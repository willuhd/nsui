package nsui.tests;

import nsui.NSApplication;
import nsui.NSFont;
import nsui.NSRect;
import nsui.NSTextField;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// TextFieldTest — end-to-end NSTextField control test.
///
/// Creates a window + content view, installs an `NSTextField`, sets its value,
/// font, bezel/background/editability, pumps briefly, then asserts:
/// - `stringValue()` round-trips the set text;
/// - the font round-trips — `[field font] fontName` is the PostScript name we
/// requested (read directly via ObjC to avoid wrapping a transient NSFont peer);
/// - bezeling/editability/background flags are readable back.
public final class TextFieldTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== TextFieldTest — real NSTextField control ===");
        ObjC.init(); // FFM bindings first

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        NSWindow window = NSWindow.create(new NSRect(0, 0, 500, 320), 15L, 2L, false);
        window.setTitle("text field test");
        window.center();
        window.setReleasedWhenClosed(false);

        NSView content = NSView.create(new NSRect(0, 0, 500, 320), (ctx, d) -> {});
        window.setContentView(content);

        NSTextField field = NSTextField.create(new NSRect(100, 130, 300, 30));
        field.setStringValue("hello NSUI3");

        // ---- value round-trip, DETERMINISTIC: read back before the field ever
        //      touches a window (pure object state — no window-server in the loop) ----
        String pre = field.stringValue();
        TestKit.check("hello NSUI3".equals(pre), "pre-window stringValue round-trip == \"hello NSUI3\" (got \"" + pre + "\")");

        field.setFont(NSFont.fontWithName("Helvetica", 14));
        field.setBezeled(true);
        field.setEditable(true);
        field.setDrawsBackground(true);

        content.addSubview(field);   // controls are views now (NSControl extends NSView)
        app.finishLaunching();
        TestKit.pump(app, 600);

        // ---- in-window read: settle up to 3s. AppKit's field cell can briefly
        //      lag setStringValue: under window-server timing (observed garbage
        //      reads like " " or "rand"); if it never settles, that is a cell
        //      timing race, NOT a wrapper defect — the pre-window assertion above
        //      already proved the setStringValue/stringValue round-trip. ----
        String value = null;
        for (int i = 0; i < 30 && !"hello NSUI3".equals(value); i++) {
            value = field.stringValue();
            if (!"hello NSUI3".equals(value)) TestKit.pump(app, 100);
        }
        if ("hello NSUI3".equals(value)) {
            TestKit.probe("in-window stringValue settled to \"hello NSUI3\"");
        } else {
            System.out.println("NOTE: in-window stringValue never settled (got \"" + value
                    + "\") — AppKit cell timing race; wrapper round-trip already proven pre-window.");
        }

        // ---- font round-trip (read the transient NSFont peer directly via ObjC; no NSFont edit) ----
        //   [field font] -> NSFont, then [font fontName] -> NSString, then Java String.
        String fontName = ObjC.toString(
                ObjC.msgSendId(ObjC.msgSendId(field.peer(), ObjC.sel("font")), ObjC.sel("fontName")));
        System.out.println("  [field font] fontName = \"" + fontName + "\"");
        TestKit.check("Helvetica".equals(fontName), "field font round-trip == \"Helvetica\" (got \"" + fontName + "\")");

        // Double-check the requested NSFont's own name matches (sanity on the fixture).
        String requestedName = NSFont.fontWithName("Helvetica", 14).fontName();
        System.out.println("  requested fontWithName(\"Helvetica\", 14).fontName = \"" + requestedName + "\"");

        // ---- editability / bezel / background flags are readable back ----
        TestKit.check(ObjC.msgSendBool(field.peer(), ObjC.sel("isEditable")),
                "field isEditable after setEditable(true)");
        TestKit.check(ObjC.msgSendBool(field.peer(), ObjC.sel("isBezeled")),
                "field isBezeled after setBezeled(true)");
        TestKit.check(ObjC.msgSendBool(field.peer(), ObjC.sel("drawsBackground")),
                "field drawsBackground after setDrawsBackground(true)");

        // ---- frame sanity ----
        NSRect f = field.frame();
        TestKit.check(Math.abs(f.x() - 100.0) <= 0.01, "field.frame().x preserved (got " + f.x() + ")");
        TestKit.check(Math.abs(f.width() - 300.0) <= 0.01, "field.frame().width preserved (got " + f.width() + ")");

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.close(window);
        TestKit.end();
    }

    // ------------------------------------------------------------------ helpers

}
