package nsui.tests;

import nsui.NSColor;
import nsui.NSFont;
import nsui.objc.ObjC;

/// Color + font round-trip test.
///
/// - `NSColor.create(r,g,b,a)` then `rgba()` must come back ≈ the
/// input within 0.02 (sRGB extended colorspace round-trip channels are cheap).
/// - `description()` is non-null/non-empty.
/// - `setFill`/`setStroke` outside any graphics context must not crash
/// (AppKit may warn; the assertion is "did not throw / did not crash the process").
/// - `NSFont.fontWithName("Helvetica", 12)` -> non-nil, name and size round-trip.
/// - `systemFontOfSize`/`boldSystemFontOfSize` -> non-nil.
public final class ColorFontTest {

    private static boolean near(double a, double b, double tol) {
        return Math.abs(a - b) <= tol;
    }

    public static void main(String[] args) {
        System.out.println("=== ColorFontTest — color + font layer ===");
        ObjC.init();

        // ---- NSColor round-trip ----
        NSColor c = NSColor.create(0.25, 0.5, 0.75, 1.0);
        TestKit.check(c != null, "NSColor.create() returned non-nil");
        double[] rgba = c.rgba();
        System.out.printf("rgba() = [%.4f, %.4f, %.4f, %.4f]%n", rgba[0], rgba[1], rgba[2], rgba[3]);
        TestKit.check(rgba.length == 4, "rgba() returned 4 components");
        TestKit.check(near(rgba[0], 0.25, 0.02), "r ≈ 0.25 (got " + rgba[0] + ")");
        TestKit.check(near(rgba[1], 0.50, 0.02), "g ≈ 0.50 (got " + rgba[1] + ")");
        TestKit.check(near(rgba[2], 0.75, 0.02), "b ≈ 0.75 (got " + rgba[2] + ")");
        TestKit.check(near(rgba[3], 1.00, 0.02), "a ≈ 1.00 (got " + rgba[3] + ")");

        String desc = c.description();
        TestKit.check(desc != null, "description() non-null");
        TestKit.check(desc != null && !desc.isEmpty(), "description() non-empty");

        // setFill / setStroke outside a graphics context must NOT crash (may warn / no-op).
        try {
            c.setFill();
            c.setStroke();
            TestKit.check(true, "setFill()/setStroke() outside a graphics context did not crash");
        } catch (Throwable t) {
            TestKit.check(false, "setFill()/setStroke() threw outside a graphics context: " + t);
        }

        // ---- NSFont ----
        NSFont helv = NSFont.fontWithName("Helvetica", 12);
        TestKit.check(helv != null, "fontWithName(\"Helvetica\", 12) returned non-nil");
        String name = helv.fontName();
        System.out.println("Helvetica fontName() = \"" + name + "\"");
        TestKit.check("Helvetica".equals(name), "fontWithName fontName() == \"Helvetica\" (got \"" + name + "\")");
        double pt = helv.pointSize();
        System.out.println("Helvetica pointSize() = " + pt);
        TestKit.check(near(pt, 12.0, 0.01), "fontWithName pointSize() == 12.0 (got " + pt + ")");

        NSFont sys = NSFont.systemFontOfSize(14);
        TestKit.check(sys != null, "systemFontOfSize(14) returned non-nil");
        System.out.println("systemFontOfSize(14) name=\"" + sys.fontName() + "\" size=" + sys.pointSize());

        NSFont bold = NSFont.boldSystemFontOfSize(16);
        TestKit.check(bold != null, "boldSystemFontOfSize(16) returned non-nil");
        System.out.println("boldSystemFontOfSize(16) name=\"" + bold.fontName() + "\" size=" + bold.pointSize());

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }
}
