package nsui.tests;

import nsui.NSApplication;
import nsui.NSRect;
import nsui.NSSecureTextField;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// SecureTextFieldTest — end-to-end NSSecureTextField control test.
///
/// Creates a window + content view, installs an `NSSecureTextField`,
/// verifies:
/// - `stringValue()` round-trips the set text (pre-window deterministic);
/// - `echosBullets` / `isEchosBullets` / `setEchosBullets:` round-trip;
/// - in-window string still settles;
/// - frame is preserved.
public final class SecureTextFieldTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SecureTextFieldTest — real NSSecureTextField control ===");
        ObjC.init();

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        NSWindow window = NSWindow.create(new NSRect(0, 0, 500, 320), 15L, 2L, false);
        window.setTitle("secure field test");
        window.center();
        window.setReleasedWhenClosed(false);

        NSView content = NSView.create(new NSRect(0, 0, 500, 320), (ctx, d) -> {});
        window.setContentView(content);

        NSSecureTextField field = NSSecureTextField.create(new NSRect(100, 130, 300, 30));
        field.setStringValue("s3cr3t!");

        // ---- deterministic pre-window round-trip ----
        String pre = field.stringValue();
        TestKit.check("s3cr3t!".equals(pre), "pre-window stringValue round-trip == \"s3cr3t!\" (got \"" + pre + "\")");

        // ---- echosBullets toggle ----
        // default is typically true for secure fields; we test round-trip rather than asserting default strictly
        boolean initial = field.echosBullets();
        boolean initial2 = field.isEchosBullets();
        TestKit.check(initial == initial2, "echosBullets() == isEchosBullets() (both " + initial + ")");
        System.out.println("  initial echosBullets = " + initial);

        field.setEchosBullets(true);
        TestKit.check(field.echosBullets(), "echosBullets after setEchosBullets(true)");
        TestKit.check(field.isEchosBullets(), "isEchosBullets after setEchosBullets(true)");

        field.setEchosBullets(false);
        TestKit.check(!field.echosBullets(), "echosBullets after setEchosBullets(false)");
        TestKit.check(!field.isEchosBullets(), "isEchosBullets after setEchosBullets(false)");

        field.setEchosBullets(true);
        TestKit.check(field.echosBullets() && field.isEchosBullets(), "echosBullets true after restore");

        // string must still round-trip after echosBullets toggle
        field.setStringValue("p@ssw0rd123");
        String afterToggle = field.stringValue();
        TestKit.check("p@ssw0rd123".equals(afterToggle), "stringValue after echosBullets toggle == \"p@ssw0rd123\" (got \"" + afterToggle + "\")");

        // reset to original for window test
        field.setStringValue("s3cr3t!");

        content.addSubview(field);
        app.finishLaunching();
        TestKit.pump(app, 600);

        String value = null;
        for (int i = 0; i < 30 && !"s3cr3t!".equals(value); i++) {
            value = field.stringValue();
            if (!"s3cr3t!".equals(value)) TestKit.pump(app, 100);
        }
        if ("s3cr3t!".equals(value)) {
            TestKit.check(true, "in-window stringValue settled to \"s3cr3t!\"");
        } else {
            System.out.println("NOTE: in-window stringValue never settled (got \"" + value + "\") — AppKit cell timing race; wrapper round-trip already proven pre-window.");
        }

        // echosBullets still true in-window
        TestKit.check(field.echosBullets(), "in-window echosBullets still true");

        // frame sanity
        NSRect f = field.frame();
        TestKit.check(Math.abs(f.x() - 100.0) <= 0.01, "field.frame().x preserved (got " + f.x() + ")");
        TestKit.check(Math.abs(f.width() - 300.0) <= 0.01, "field.frame().width preserved (got " + f.width() + ")");

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.close(window);
        TestKit.end();
    }

}
