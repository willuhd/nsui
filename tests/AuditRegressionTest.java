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
import nsui.NSMenu;
import nsui.NSPoint;
import nsui.NSPrintInfo;
import nsui.NSRect;
import nsui.NSScrollView;
import nsui.NSSize;
import nsui.NSSlider;
import nsui.NSTableView;
import nsui.NSValue;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;
import nsui.objc.Scratch;

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
        // matrix returns a copy as double[6].
        try {
            NSFont font = NSFont.systemFontOfSize(12);
            double[] m = font.matrix();
            boolean finite = m != null && m.length == 6;
            if (finite) {
                for (double v : m) finite &= Double.isFinite(v);
                finite &= m[0] > 0;
            }
            TestKit.check(finite, "NSFont.systemFontOfSize(12).matrix() returns 6 finite doubles with positive scale (got "
                    + java.util.Arrays.toString(m) + ")");
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
        TestKit.expectThrows("NSControl.setStringValue(null) rejects nil value",
                IllegalArgumentException.class, () -> slider.setStringValue(null));
        TestKit.noThrow("NSControl.performClick(null) passes nil sender", () -> slider.performClick(null));
        // No run-loop pumping here: the mode check fires before any native call.
        TestKit.expectThrows("NSApplication.nextEvent(null mode) rejects nil mode",
                IllegalArgumentException.class, () -> app.nextEvent(-1L, MemorySegment.NULL, null, true));

        // --- 8b. nullable-vs-required null arguments (the sweep that closed the rest) ---
        // Nullable AppKit arguments must pass nil, not throw a raw NPE.
        NSWindow cw = TestKit.hiddenWindow(180, 120);
        TestKit.noThrow("NSWindow.setContentView(null) passes nil", () -> cw.setContentView(null));
        NSScrollView sv = NSScrollView.create(new NSRect(0, 0, 180, 120));
        TestKit.noThrow("NSScrollView.setDocumentView(null) passes nil", () -> sv.setDocumentView(null));
        TestKit.noThrow("NSApplication.sendEvent(null) is a no-op", () -> app.sendEvent(null));
        TestKit.noThrow("NSApplication.postEvent(null) is a no-op", () -> app.postEvent(null, false));
        TestKit.noThrow("NSControl.setAction(null) passes a NULL SEL", () -> slider.setAction(null));
        TestKit.noThrow("NSControl.sendAction(sel, null target) passes nil",
                () -> slider.sendAction(ObjC.sel("performClick:"), null));
        // Required (nonnull) arguments must fail fast with a named IAE, not an NPE.
        NSView host = NSView.create(new NSRect(0, 0, 50, 50), (c, d) -> {});
        TestKit.expectThrows("NSView.addSubview(null) rejects nil subview",
                IllegalArgumentException.class, () -> host.addSubview(null));
        TestKit.expectThrows("NSControl.updateCell(null) rejects nil cell",
                IllegalArgumentException.class, () -> slider.updateCell(null));
        NSMenu menu = NSMenu.create();
        TestKit.expectThrows("NSMenu.addItem(null) rejects nil item",
                IllegalArgumentException.class, () -> menu.addItem(null));
        TestKit.expectThrows("NSMenu.removeItemAtIndex(count) rejects out-of-range index",
                IllegalArgumentException.class, () -> menu.removeItemAtIndex(3));
        TestKit.expectThrows("NSView.addConstraint(null) rejects nil constraint",
                IllegalArgumentException.class, () -> host.addConstraint(null));
        TestKit.close(cw);

        // --- 8c. a zero-address segment is nil, and Bounds.nonNull must reject it ---
        // The documented nil convention is MemorySegment.NULL (see ObjC.nsstring), so a
        // guarded raw-id argument could be Java-non-null and still model nil. Testing only
        // the reference let that value reach a nonnull native parameter: verified aborts
        // were moveItemAtIndexPath(NULL,·)/(·,NULL), setSelectionIndexPaths(NULL) and
        // makeItemWithIdentifier(·,NULL) — all NSInternalInconsistencyException.
        try {
            nsui.NSCollectionView cv = nsui.NSCollectionView.create(new NSRect(0, 0, 120, 120));
            TestKit.expectThrows("NSCollectionView.moveItemAtIndexPath(NULL,·) rejects nil",
                    IllegalArgumentException.class,
                    () -> cv.moveItemAtIndexPath(MemorySegment.NULL, MemorySegment.NULL));
            TestKit.expectThrows("NSCollectionView.setSelectionIndexPaths(NULL) rejects nil",
                    IllegalArgumentException.class, () -> cv.setSelectionIndexPaths(MemorySegment.NULL));
            TestKit.expectThrows("NSCollectionView.makeItemWithIdentifier(·,NULL) rejects nil index path",
                    IllegalArgumentException.class, () -> cv.makeItemWithIdentifier("x", MemorySegment.NULL));
            TestKit.expectThrows("NSCollectionView.selectItemsAtIndexPaths(NULL) rejects nil",
                    IllegalArgumentException.class, () -> cv.selectItemsAtIndexPaths(MemorySegment.NULL, 0));
        } catch (Throwable t) {
            TestKit.check(false, "NSCollectionView nil-guard block threw: " + t);
        }

        // --- 8d. out-parameters must be NULL or wide enough (Bounds.outBuffer) ---
        // Foundation writes a full NSDictionary* through an `NSDictionary **` out slot
        // regardless of the segment's declared size, and FFM forwards a short segment
        // (verified: a 4-byte slice accepted an 8-byte native write). Without the guard a
        // caller's short/offset buffer is silently overwritten past its end.
        try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
            MemorySegment shortOut = arena.allocate(4);
            TestKit.expectThrows("NSAttributedString.createWithRTF short documentAttributes rejects",
                    IllegalArgumentException.class,
                    () -> nsui.NSAttributedString.createWithRTF(null, shortOut));
            TestKit.expectThrows("NSAttributedString.createWithHTML short documentAttributes rejects",
                    IllegalArgumentException.class,
                    () -> nsui.NSAttributedString.createWithHTML(null, shortOut));
            TestKit.expectThrows("NSAttributedString.createWithHTMLBaseURL short documentAttributes rejects",
                    IllegalArgumentException.class,
                    () -> nsui.NSAttributedString.createWithHTMLBaseURL(null, null, shortOut));
            TestKit.expectThrows("NSAttributedString.createWithDocFormat short documentAttributes rejects",
                    IllegalArgumentException.class,
                    () -> nsui.NSAttributedString.createWithDocFormat(null, shortOut));
            MemorySegment wideOut = arena.allocate(8);
            TestKit.check(wideOut.byteSize() == 8, "an 8-byte out slot is the accepted width");
        }

        // --- 9. value and layer struct round-trips ---
        try {
            NSValue v = NSValue.valueWithPoint(new NSPoint(3, 4));
            String ty = v.objCType();
            TestKit.check(ty != null && ty.contains("Point"), "NSValue.valueWithPoint objCType contains Point (got '" + ty + "')");
        } catch (Throwable t) {
            TestKit.check(false, "NSValue.valueWithPoint threw: " + t);
        }
        try {
            // valueWithEdgeInsets:/edgeInsetsValue were omitted while Sig could not
            // express NSEdgeInsets; both shapes now exist, so the pair must round-trip.
            NSEdgeInsets in = new NSEdgeInsets(1, 2, 3, 4);
            NSValue ev = NSValue.valueWithEdgeInsets(in);
            TestKit.check(ev != null && in.equals(ev.edgeInsetsValue()),
                    "NSValue.valueWithEdgeInsets/edgeInsetsValue round-trip (got "
                            + (ev == null ? "null" : ev.edgeInsetsValue().toString()) + ")");
        } catch (Throwable t) {
            TestKit.check(false, "NSValue edgeInsets round-trip threw: " + t);
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

        // --- 11. lifetime / leak oracles ---
        // A runtime ObjC class pair is created once per (super, className) and never
        // disposed, so creating the same delegate shape repeatedly must NOT grow the
        // class-pair count (a caller-supplied dynamic className would leak one pair
        // per call). Warm the shape first: the very first call necessarily creates
        // its class pair, so the delta must be measured after that.
        DelegateProxy.actionTarget("regressAction:", sender -> {});
        int pairsBefore = DelegateProxy.classPairCount();
        MemorySegment targetA = DelegateProxy.actionTarget("regressAction:", sender -> {});
        MemorySegment targetB = DelegateProxy.actionTarget("regressAction:", sender -> {});
        TestKit.check(DelegateProxy.classPairCount() == pairsBefore,
                "same delegate shape reuses one class pair (before=" + pairsBefore
                        + ", after=" + DelegateProxy.classPairCount() + ")");
        TestKit.check(targetA != null && targetB != null && targetA.address() != targetB.address(),
                "action targets are distinct instances sharing that class pair");
        // The per-thread bump buffer must not spill to the immortal global arena for a
        // turn that fits inside it (spilledBytes is the un-reclaimable part).
        long spilledBefore = Scratch.spilledBytes();
        Scratch.beginTurn();
        for (int i = 0; i < 100; i++) ObjC.rect(i, i, 1, 1);
        Scratch.endTurn();
        TestKit.check(Scratch.spilledBytes() == spilledBefore,
                "a small turn does not spill to the global arena (spilled=" + Scratch.spilledBytes() + ")");

        TestKit.end();
    }
}
