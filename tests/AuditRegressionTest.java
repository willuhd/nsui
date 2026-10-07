package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.CAKeyframeAnimation;
import nsui.CAMetalLayer;
import nsui.CATransition;
import nsui.MTLDevice;
import nsui.MTLRegion;
import nsui.MTLTexture;
import nsui.MTLTextureDescriptor;
import nsui.NSMutableParagraphStyle;
import nsui.NSApplication;
import nsui.NSFont;
import nsui.NSFontDescriptor;
import nsui.NSEdgeInsets;
import nsui.NSPoint;
import nsui.NSPrintInfo;
import nsui.NSRect;
import nsui.NSSize;
import nsui.NSSlider;
import nsui.NSTableView;
import nsui.NSValue;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// Regression guards for the bugs the audit found:
/// - three public methods that sent selectors no class implements, aborting the JVM
///   with an unrecognized-selector exception that try/catch cannot catch;
/// - four methods declared void whose native method returns BOOL (status discarded);
/// - nullable object setters that threw a Java NPE instead of passing nil;
/// - NSEdgeInsets.toSegment using the turn-gated scratch tier.
public final class AuditRegressionTest {

    private AuditRegressionTest() {}

    public static void main(String[] args) {
        System.out.println("=== AuditRegressionTest - regression guards for audit findings ===");
        ObjC.init();
        NSApplication app = TestKit.app();

        // --- 1. previously-aborting selectors ---
        try {
            NSFontDescriptor d = NSFontDescriptor.preferredFontDescriptorWithTextStyle("body");
            TestKit.check(d != null, "preferredFontDescriptorWithTextStyle('body') resolves (was a JVM abort)");
        } catch (Throwable t) {
            TestKit.check(false, "preferredFontDescriptorWithTextStyle threw: " + t);
        }
        try {
            NSFontDescriptor d = NSFontDescriptor.preferredFontDescriptorForTextStyleOptions("body", null);
            TestKit.check(d != null, "preferredFontDescriptorForTextStyle:options: resolves");
        } catch (Throwable t) {
            TestKit.check(false, "preferredFontDescriptorForTextStyle:options: threw: " + t);
        }
        try {
            NSPrintInfo pi = NSPrintInfo.defaultPrintInfo();
            TestKit.check(pi != null, "NSPrintInfo.defaultPrintInfo() resolves via sharedPrintInfo (was a JVM abort)");
        } catch (Throwable t) {
            TestKit.check(false, "defaultPrintInfo threw: " + t);
        }

        // --- 2. BOOL status is now observable ---
        long policy = app.activationPolicy();
        boolean bad = app.setActivationPolicy(99);
        TestKit.check(!bad, "setActivationPolicy(99) returns false for an invalid policy");
        boolean restore = app.setActivationPolicy(policy);
        // Environment-dependent: an unlaunched app may refuse a policy change, so this
        // is the status probe, while the invalid-policy case above is the real guard.
        TestKit.probe("setActivationPolicy(restore) returns " + restore);

        NSWindow win = TestKit.hiddenWindow(200, 200);
        NSView content = NSView.create(new NSRect(0, 0, 200, 200), (c, d) -> {});
        win.setContentView(content);
        boolean focused = win.makeFirstResponder(content);
        TestKit.check(focused, "makeFirstResponder(contentView) returns true");
        TestKit.check(win.firstResponder() != null, "firstResponder non-null after makeFirstResponder");
        boolean autosave = win.setFrameAutosaveName("nsui-audit-regression-" + System.nanoTime());
        TestKit.probe("setFrameAutosaveName returns native status " + autosave);
        TestKit.close(win);

        // --- 3. nullable object setters pass nil instead of NPE ---
        TestKit.noThrow("NSApplication.setDelegate(null) passes nil", () -> app.setDelegate(null));
        TestKit.noThrow("NSApplication.setMainMenu(null) passes nil", () -> app.setMainMenu(null));
        TestKit.noThrow("NSApplication.setHelpMenu(null) passes nil", () -> app.setHelpMenu(null));
        NSWindow w2 = TestKit.hiddenWindow(100, 100);
        TestKit.noThrow("NSWindow.setDelegate(null) passes nil", () -> w2.setDelegate(null));
        TestKit.close(w2);
        NSSlider slider = NSSlider.create(new NSRect(0, 0, 100, 20));
        TestKit.noThrow("NSControl.setTarget(null) passes nil", () -> slider.setTarget(null));

        // --- 4. NSEdgeInsets segment round-trip (scratch-tier regression) ---
        NSEdgeInsets insets = new NSEdgeInsets(1, 2, 3, 4);
        NSEdgeInsets back = NSEdgeInsets.fromSegment(insets.toSegment());
        TestKit.check(back.equals(insets), "NSEdgeInsets toSegment/fromSegment round-trip (" + back + ")");

        // --- 5. struct/double marshalling (CGFloat and matrix return) ---
        // setLineSpacing took a double through the wrong handle width and read back garbage.
        try {
            NSMutableParagraphStyle style = NSMutableParagraphStyle.create();
            style.setLineSpacing(12.5);
            double got = style.lineSpacing();
            TestKit.check(got == 12.5, "NSMutableParagraphStyle setLineSpacing(12.5) reads back 12.5 (got " + got + ")");
        } catch (Throwable t) {
            TestKit.check(false, "NSMutableParagraphStyle lineSpacing round-trip threw: " + t);
        }
        // matrix returns const CGFloat* (6 doubles = 48 bytes) copied to a call-scoped segment.
        try {
            NSFont font = NSFont.systemFontOfSize(12);
            MemorySegment m = font.matrix();
            TestKit.check(m != null && m.address() != 0, "NSFont.systemFontOfSize(12).matrix() reads without throwing");
            if (m != null) {
                TestKit.check(m.byteSize() == 48, "NSFont matrix byteSize is 48 (got " + m.byteSize() + ")");
            }
        } catch (Throwable t) {
            TestKit.check(false, "NSFont matrix() threw: " + t);
        }

        // --- 6. nil-on-wire getters (verified present: NSControl.formatter, NSTableView.dataSource) ---
        // Both getters exist in the wrappers, so a direct nil read is the guard.
        TestKit.noThrow("NSSlider.setFormatter(null) passes nil", () -> slider.setFormatter(null));
        try {
            MemorySegment fmt = slider.formatter();
            TestKit.check(fmt == null || fmt.address() == 0, "NSSlider.formatter() is nil after setFormatter(null)");
        } catch (Throwable t) {
            TestKit.check(false, "NSSlider.formatter() threw: " + t);
        }
        NSTableView table = NSTableView.create(new NSRect(0, 0, 100, 100));
        TestKit.noThrow("NSTableView.setDataSource(null) passes nil", () -> table.setDataSource(null));
        try {
            MemorySegment ds = table.dataSource();
            TestKit.check(ds == null || ds.address() == 0, "NSTableView.dataSource() is nil after setDataSource(null)");
        } catch (Throwable t) {
            TestKit.check(false, "NSTableView.dataSource() threw: " + t);
        }

        // --- 7. nil-string setters are legal no-ops ---
        try {
            CATransition tr = CATransition.create();
            TestKit.noThrow("CATransition.setType(null) passes nil", () -> tr.setType(null));
        } catch (Throwable t) {
            TestKit.check(false, "CATransition.create threw: " + t);
        }
        try {
            CAKeyframeAnimation ka = CAKeyframeAnimation.create("opacity");
            TestKit.noThrow("CAKeyframeAnimation.setCalculationMode(null) passes nil", () -> ka.setCalculationMode(null));
        } catch (Throwable t) {
            TestKit.check(false, "CAKeyframeAnimation.create threw: " + t);
        }

        // --- 8. fail-fast argument guards ---
        TestKit.expectThrows("NSControl.takeStringValueFrom(null) rejects nil sender",
                IllegalArgumentException.class, () -> slider.takeStringValueFrom(null));
        // No run-loop pumping here: the mode check fires before any native call.
        TestKit.expectThrows("NSApplication.nextEvent(null mode) rejects nil mode",
                IllegalArgumentException.class, () -> app.nextEvent(-1L, MemorySegment.NULL, null, true));

        // --- 9. value and layer struct round-trips ---
        try {
            NSValue v = NSValue.valueWithPoint(new NSPoint(3, 4));
            String ty = v.objCType();
            TestKit.check(ty != null && ty.contains("Point"), "NSValue.valueWithPoint objCType contains Point (got '" + ty + "')");
        } catch (Throwable t) {
            TestKit.check(false, "NSValue.valueWithPoint threw: " + t);
        }
        try {
            CAMetalLayer layer = CAMetalLayer.create();
            layer.setDrawableSize(new NSSize(128, 64));
            NSSize got = layer.drawableSize();
            TestKit.check(got.width() == 128 && got.height() == 64,
                    "CAMetalLayer drawableSize round-trip (128,64) (got " + got + ")");
        } catch (Throwable t) {
            TestKit.check(false, "CAMetalLayer drawableSize round-trip threw: " + t);
        }

        // --- 10. texture readback argument guards (GPU-gated) ---
        MTLDevice device = null;
        try {
            device = MTLDevice.systemDefault();
        } catch (Throwable t) {
            TestKit.skipCase("MTLDevice.systemDefault unavailable: " + t.getMessage());
        }
        if (device == null) {
            TestKit.skipCase("no Metal GPU device: MTLTexture.getBytes guards skipped");
        } else {
            try {
                MTLTextureDescriptor desc = MTLTextureDescriptor.texture2D(80, 32, 32, false);
                desc.setUsage(MTLTextureDescriptor.USAGE_SHADER_READ);
                desc.setStorageMode(MTLTextureDescriptor.STORAGE_SHARED);
                MTLTexture tex = device.newTexture(desc);
                TestKit.check(tex != null, "Metal texture created for getBytes guards");
                if (tex != null) {
                    final MTLTexture ft = tex;
                    TestKit.expectThrows("MTLTexture.getBytes depth!=1 rejected",
                            IllegalArgumentException.class,
                            () -> ft.getBytes(32 * 4, new MTLRegion(0, 0, 0, 8, 8, 2), 0));
                    TestKit.expectThrows("MTLTexture.getBytes negative level rejected",
                            IllegalArgumentException.class,
                            () -> ft.getBytes(32 * 4, MTLRegion.of2D(0, 0, 8, 8), -1));
                }
            } catch (Throwable t) {
                TestKit.check(false, "Metal texture guard setup threw: " + t);
            }
        }

        TestKit.end();
    }
}
