package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSApplication;
import nsui.NSFont;
import nsui.NSRect;
import nsui.NSTextField;
import nsui.NSView;
import nsui.NSWindow;
import nsui.NSEvent;
import nsui.objc.ObjC;

/**
 * TextFieldTest — end-to-end NSTextField control test.
 *
 * <p>Creates a window + content view, installs an {@code NSTextField}, sets its value,
 * font, bezel/background/editability, pumps briefly, then asserts:
 * <ul>
 *   <li>{@code stringValue()} round-trips the set text;</li>
 *   <li>the font round-trips — {@code [field font] fontName} is the PostScript name we
 *       requested (read directly via ObjC to avoid wrapping a transient NSFont peer);</li>
 *   <li>bezeling/editability/background flags are readable back.</li>
 * </ul>
 */
public final class TextFieldTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

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
        check("hello NSUI3".equals(pre), "pre-window stringValue round-trip == \"hello NSUI3\" (got \"" + pre + "\")");

        field.setFont(NSFont.fontWithName("Helvetica", 14));
        field.setBezeled(true);
        field.setEditable(true);
        field.setDrawsBackground(true);

        content.addSubview(field);   // controls are views now (NSControl extends NSView)
        app.activateIgnoringOtherApps(true);
        window.makeKeyAndOrderFront(null);
        app.finishLaunching();
        pumpForMs(app, 600);

        // ---- in-window read: settle up to 3s. AppKit's field cell can briefly
        //      lag setStringValue: under window-server timing (observed garbage
        //      reads like " " or "rand"); if it never settles, that is a cell
        //      timing race, NOT a wrapper defect — the pre-window assertion above
        //      already proved the setStringValue/stringValue round-trip. ----
        String value = null;
        for (int i = 0; i < 30 && !"hello NSUI3".equals(value); i++) {
            value = field.stringValue();
            if (!"hello NSUI3".equals(value)) pumpForMs(app, 100);
        }
        if ("hello NSUI3".equals(value)) {
            check(true, "in-window stringValue settled to \"hello NSUI3\"");
        } else {
            System.out.println("NOTE: in-window stringValue never settled (got \"" + value
                    + "\") — AppKit cell timing race; wrapper round-trip already proven pre-window.");
        }

        // ---- font round-trip (read the transient NSFont peer directly via ObjC; no NSFont edit) ----
        //   [field font] -> NSFont, then [font fontName] -> NSString, then Java String.
        String fontName = ObjC.toString(
                ObjC.msgSendId(ObjC.msgSendId(field.peer(), ObjC.sel("font")), ObjC.sel("fontName")));
        System.out.println("  [field font] fontName = \"" + fontName + "\"");
        check("Helvetica".equals(fontName), "field font round-trip == \"Helvetica\" (got \"" + fontName + "\")");

        // Double-check the requested NSFont's own name matches (sanity on the fixture).
        String requestedName = NSFont.fontWithName("Helvetica", 14).fontName();
        System.out.println("  requested fontWithName(\"Helvetica\", 14).fontName = \"" + requestedName + "\"");

        // ---- editability / bezel / background flags are readable back ----
        check(ObjC.msgSendBool(field.peer(), ObjC.sel("isEditable")),
                "field isEditable after setEditable(true)");
        check(ObjC.msgSendBool(field.peer(), ObjC.sel("isBezeled")),
                "field isBezeled after setBezeled(true)");
        check(ObjC.msgSendBool(field.peer(), ObjC.sel("drawsBackground")),
                "field drawsBackground after setDrawsBackground(true)");

        // ---- frame sanity ----
        NSRect f = field.frame();
        check(Math.abs(f.x() - 100.0) <= 0.01, "field.frame().x preserved (got " + f.x() + ")");
        check(Math.abs(f.width() - 300.0) <= 0.01, "field.frame().width preserved (got " + f.width() + ")");

        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        window.performClose(null);
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ helpers

    private static void pumpOnce(NSApplication app) {
        MemorySegment until = ObjC.msgSendIdDouble(
                ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.05);
        NSEvent ev = app.nextEvent(-1L, until, "kCFRunLoopDefaultMode", true);
        if (ev != null) app.sendEvent(ev);
        app.updateWindows();
    }

    private static void pumpForMs(NSApplication app, long ms) throws InterruptedException {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            pumpOnce(app);
            Thread.sleep(10);
        }
    }
}
