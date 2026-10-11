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
            // A 4-byte slot must be rejected; a valid 8-byte one must still be accepted
            // and populated (this positive is what fails if outBuffer is over-tightened).
            byte[] rtf = "{\\rtf1\\ansi hello}".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            MemorySegment wideOut = arena.allocate(8);
            nsui.NSAttributedString parsed = nsui.NSAttributedString.createWithRTF(
                    nsui.NSData.dataWithBytes(rtf).peer(), wideOut);
            TestKit.check(parsed != null && "hello".equals(parsed.string()),
                    "an 8-byte out slot is accepted and the RTF parses (got " + parsed + ")");
        }

        // --- 8e. parallel-collection counts and the insert bound ---
        // Mismatched counts are not range errors: AppKit raises NSInvalidArgumentException
        // ("count of array (1) differs from count of index set (2)", verified) and the
        // JVM aborts before the Java guard can report anything.
        try {
            nsui.NSArray one = nsui.NSArray.arrayWithObject(nsui.NSString.of("x"));
            nsui.NSArray two = nsui.NSArray.mutableArray();
            two.addObject(nsui.NSString.of("x"));
            two.addObject(nsui.NSString.of("y"));
            nsui.NSIndexSet twoIndexes = nsui.NSIndexSet.indexSetWithIndexesInRange(new nsui.NSRange(0, 2));
            TestKit.expectThrows("insertObjectsAtIndexes rejects mismatched counts",
                    IllegalArgumentException.class, () -> two.insertObjectsAtIndexes(one, twoIndexes));
            TestKit.expectThrows("replaceObjectsAtIndexes rejects mismatched counts",
                    IllegalArgumentException.class, () -> two.replaceObjectsAtIndexes(twoIndexes, one));
            TestKit.expectThrows("dictionaryWithObjectsForKeys rejects mismatched counts",
                    IllegalArgumentException.class, () -> nsui.NSDictionary.dictionaryWithObjectsForKeys(one, two));
            // The insert indexes are positions in the RESULT array, so the contiguous tail
            // {n, n+1} is legal natively (verified: {2,3} into a 2-element array yields 4).
            // The old count()+1 bound rejected it; it must stay accepted.
            nsui.NSArray tail = nsui.NSArray.mutableArray();
            tail.addObject(nsui.NSString.of("p"));
            tail.addObject(nsui.NSString.of("q"));
            nsui.NSArray rs = nsui.NSArray.mutableArray();
            rs.addObject(nsui.NSString.of("r"));
            rs.addObject(nsui.NSString.of("s"));
            tail.insertObjectsAtIndexes(rs, nsui.NSIndexSet.indexSetWithIndexesInRange(new nsui.NSRange(2, 2)));
            TestKit.check(tail.count() == 4 && "s".equals(ObjC.toString(tail.objectAtIndex(3))),
                    "insertObjectsAtIndexes accepts the legal tail {2,3} (count=" + tail.count() + ")");
        } catch (Throwable t) {
            TestKit.check(false, "collection count-guard block threw: " + t);
        }

        // --- 8f. text: nil arguments and out-of-range NSRange ---
        // Each of these aborts the JVM natively (verified) instead of raising, so the
        // guard is the only thing standing between a caller mistake and a dead process.
        try {
            nsui.NSTextStorage storage = nsui.NSTextStorage.create("abc");
            TestKit.expectThrows("NSTextStorage.addLayoutManager(null) rejects nil",
                    IllegalArgumentException.class, () -> storage.addLayoutManager(null));
            TestKit.noThrow("NSTextStorage.removeLayoutManager(null) stays a no-op",
                    () -> storage.removeLayoutManager(null));

            nsui.NSText text = nsui.NSText.create(new NSRect(0, 0, 200, 100));
            text.setString("abc");
            TestKit.expectThrows("NSText.setFontRange out-of-range rejects",
                    IllegalArgumentException.class, () -> text.setFontRange(null, new nsui.NSRange(0, 4)));
            // A nil font is nonnull in NSText.h and aborts natively ("nil NSFont given"),
            // which is easy to miss because the range guard fires first for the case above.
            TestKit.expectThrows("NSText.setFontRange(null font) rejects",
                    IllegalArgumentException.class,
                    () -> text.setFontRange(null, new nsui.NSRange(0, 3)));
            TestKit.noThrow("NSText.setFontRange in-range still works",
                    () -> text.setFontRange(NSFont.systemFontOfSize(12), new nsui.NSRange(0, 3)));

            nsui.NSMutableAttributedString mut = nsui.NSMutableAttributedString.create("abc");
            TestKit.expectThrows("NSMutableAttributedString.append(null) rejects nil",
                    IllegalArgumentException.class, () -> mut.append(null));
            TestKit.expectThrows("NSMutableAttributedString.insertAttributedString(null,0) rejects nil",
                    IllegalArgumentException.class, () -> mut.insertAttributedString(null, 0));
            TestKit.expectThrows("NSMutableAttributedString.setAttributedString(null) rejects nil",
                    IllegalArgumentException.class, () -> mut.setAttributedString(null));
            TestKit.expectThrows("NSMutableAttributedString.replaceCharactersInRangeWithAttributedString(·,null) rejects nil",
                    IllegalArgumentException.class,
                    () -> mut.replaceCharactersInRangeWithAttributedString(new nsui.NSRange(0, 1), null));
            TestKit.expectThrows("NSMutableAttributedString.addAttribute(nil value) rejects",
                    IllegalArgumentException.class,
                    () -> mut.addAttribute("NSFont", null, new nsui.NSRange(0, 1)));
            TestKit.expectThrows("NSMutableAttributedString.addAttributes(nil) rejects",
                    IllegalArgumentException.class,
                    () -> mut.addAttributes(null, new nsui.NSRange(0, 1)));
            // setAttributes: is the documented-nullable one and must stay accepted.
            TestKit.noThrow("NSMutableAttributedString.setAttributes(null) passes nil",
                    () -> mut.setAttributes(null, new nsui.NSRange(0, 1)));

            nsui.NSString s = nsui.NSString.of("abc");
            TestKit.expectThrows("NSString.lineRangeForRange out-of-range rejects",
                    IllegalArgumentException.class, () -> s.lineRangeForRange(new nsui.NSRange(0, 4)));
            TestKit.expectThrows("NSString.paragraphRangeForRange out-of-range rejects",
                    IllegalArgumentException.class, () -> s.paragraphRangeForRange(new nsui.NSRange(0, 4)));
            TestKit.expectThrows("NSString.rangeOfComposedCharacterSequencesForRange out-of-range rejects",
                    IllegalArgumentException.class,
                    () -> s.rangeOfComposedCharacterSequencesForRange(new nsui.NSRange(0, 4)));
        } catch (Throwable t) {
            TestKit.check(false, "text guard block threw: " + t);
        }

        // --- 8g. table / scroll-view / collection-view indexes measured aborting ---
        try {
            NSTableView tv = NSTableView.create(new NSRect(0, 0, 200, 100));
            tv.addTableColumn(nsui.NSTableColumn.create("a"));
            tv.addTableColumn(nsui.NSTableColumn.create("b"));
            // NSTableView.h:232: both indexes must be < numberOfColumns. insertIndex let
            // newIndex == count through and the native side aborted (verified).
            TestKit.expectThrows("NSTableView.moveColumn(newIndex == numberOfColumns) rejects",
                    IllegalArgumentException.class, () -> tv.moveColumn(0, 2));
            TestKit.noThrow("NSTableView.moveColumn(0,1) still works", () -> tv.moveColumn(0, 1));
            TestKit.expectThrows("NSTableView.setRowActionsVisible(true) rejects",
                    IllegalArgumentException.class, () -> tv.setRowActionsVisible(true));
            // No positive setRowActionsVisible(false) here on purpose: on a cell-based
            // table AppKit raises for NO as well ("setRowActionsVisible: is only for view
            // based NSTableView"), so asserting it would abort this test JVM.
            //
            // NSTableView.h:467: a row outside numberOfRows raises — but only on a
            // VIEW-BASED table with rows. Populate it: on the empty table this assertion
            // would pass on the pre-existing Bounds.index(0,0) rather than the row bound
            // this fix added, i.e. it would not be evidence for the fix.
            MemorySegment rowDs = DelegateProxy.delegate("NSObject", "NSUITableRowsDS",
                    java.util.Map.of(), java.util.Map.of(),
                    java.util.Map.of("numberOfRowsInTableView:", sender -> 3L),
                    java.util.Map.of("tableView:viewForTableColumn:row:",
                            (tableView, column, row) -> nsui.NSView.create(new NSRect(0, 0, 10, 10), (c, d) -> {}).peer()),
                    java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
            NSTableView tvRows = NSTableView.create(new NSRect(0, 0, 200, 100));
            tvRows.addTableColumn(nsui.NSTableColumn.create("r"));
            tvRows.setDataSource(rowDs);
            tvRows.setDelegate(rowDs);       // view-based needs the delegate method, not just the count
            tvRows.reloadData();
            TestKit.check(tvRows.numberOfRows() == 3,
                    "3-row view-based table reports 3 rows (got " + tvRows.numberOfRows() + ")");
            TestKit.expectThrows("NSTableView.rowViewAtRow(numberOfRows) rejects on a populated table",
                    IllegalArgumentException.class, () -> tvRows.rowViewAtRow(3, false));
            TestKit.noThrow("NSTableView.rowViewAtRow(0) still works",
                    () -> tvRows.rowViewAtRow(0, false));

            // NSScrollView.h:49 types contentView as NSClipView; a plain NSView aborted
            // ("-[NSView documentView]: unrecognized selector"). Enforcement is the Java
            // signature, so assert the signature itself: with the old NSView parameter
            // getMethod(NSClipView.class) throws NoSuchMethodException. (A plain
            // setContentView(NSClipView) call would pass under either signature.)
            java.lang.reflect.Method setContentView = null;
            try {
                setContentView = NSScrollView.class.getMethod("setContentView", nsui.NSClipView.class);
            } catch (NoSuchMethodException e) {
                // leave null: the parameter type regressed to NSView
            }
            TestKit.check(setContentView != null, "NSScrollView.setContentView takes NSClipView (signature-enforced)");
            NSScrollView sv2 = NSScrollView.create(new NSRect(0, 0, 200, 100));
            TestKit.noThrow("NSScrollView.setContentView(NSClipView) works",
                    () -> sv2.setContentView(nsui.NSClipView.create(new NSRect(0, 0, 100, 100))));

            // moveSection:toSection: raised NSInternalInconsistencyException on a laid-out
            // 2-section view (verified: (0,1) legal, (1,2) raises). Use a real 2-section
            // view so the rejection cannot come from the empty-view item-count path.
            MemorySegment sectionsDs = DelegateProxy.delegate("NSObject", "NSUISectionsOnlyDS2",
                    java.util.Map.of(), java.util.Map.of(),
                    java.util.Map.of("numberOfSectionsInCollectionView:", sender -> 2L),
                    java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
            nsui.NSCollectionView cvSs = nsui.NSCollectionView.create(new NSRect(0, 0, 200, 100));
            cvSs.setDataSource(sectionsDs);
            cvSs.setCollectionViewLayout(ObjC.msgSendId(
                    ObjC.msgSendId(ObjC.cls("NSCollectionViewFlowLayout"), ObjC.sel("alloc")), ObjC.sel("init")));
            cvSs.reloadData();
            TestKit.check(cvSs.numberOfSections() == 2,
                    "2-section collection view reports 2 sections (got " + cvSs.numberOfSections() + ")");
            TestKit.expectThrows("NSCollectionView.moveSection(1, 2) rejects on a 2-section view",
                    IllegalArgumentException.class, () -> cvSs.moveSection(1, 2));
            TestKit.noThrow("NSCollectionView.moveSection(0,1) is legal with 2 sections",
                    () -> cvSs.moveSection(0, 1));
        } catch (Throwable t) {
            TestKit.check(false, "view index-guard block threw: " + t);
        }

        // --- 8h. remaining measured aborts: NSTextStorage.edited:, window border edge ---
        // Both re-probed this round. Deliberately NOT fixed (verified harmless natively):
        // invalidateAttributesInRange:/ensureAttributesAreFixedInRange: with an
        // out-of-range value, NSMutableIndexSet.addIndex(-1)/addIndexesInRange({100,5}),
        // NSCollectionView.registerClass(nil), NSTableView.deselectRow(50)/
        // selectRowIndexes({50}) on an empty table, NSSplitView.setPosition:ofDividerAtIndex:
        // with a bad divider index, NSTextField.setAttributedStringValue(null), and
        // NSWindow.setAspectRatio(0,0) followed by setFrame:display: (no trap observed).
        try {
            nsui.NSTextStorage st = nsui.NSTextStorage.create("abc");
            TestKit.expectThrows("NSTextStorage.edited out-of-range range rejects",
                    IllegalArgumentException.class, () -> st.edited(0, new nsui.NSRange(100, 1), 0));
            TestKit.noThrow("NSTextStorage.edited in-range still works",
                    () -> st.edited(0, new nsui.NSRange(0, 3), 0));

            NSWindow bw = TestKit.hiddenWindow(160, 120);
            TestKit.expectThrows("NSWindow.setContentBorderThicknessForEdge(NSMinXEdge) rejects",
                    IllegalArgumentException.class, () -> bw.setContentBorderThicknessForEdge(24, 0));
            TestKit.expectThrows("NSWindow.setContentBorderThicknessForEdge(NSMaxXEdge) rejects",
                    IllegalArgumentException.class, () -> bw.setContentBorderThicknessForEdge(24, 2));
            TestKit.noThrow("NSWindow.setContentBorderThicknessForEdge(NSMinYEdge) still works",
                    () -> bw.setContentBorderThicknessForEdge(24, 1));
            TestKit.close(bw);
        } catch (Throwable t) {
            TestKit.check(false, "storage/window guard block threw: " + t);
        }

        // --- 8i. itemAtIndex: is single-section only ---
        // With a 2-section data source AppKit raises NSInternalInconsistencyException
        // ("Use -indexPathForItem:, when the CollectionView may contain more than one
        // section", verified, exit 134); a section-0 index check alone did not catch it.
        try {
            nsui.NSCollectionView cv3 = nsui.NSCollectionView.create(new NSRect(0, 0, 200, 100));
            MemorySegment ds = DelegateProxy.delegate("NSObject", "NSUISectionsOnlyDS",
                    java.util.Map.of(), java.util.Map.of(),
                    java.util.Map.of("numberOfSectionsInCollectionView:", sender -> 2L),
                    java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
            cv3.setDataSource(ds);
            // The data source is only consulted once a layout is installed; without this
            // numberOfSections() is 0 and the rejection below would fire for the wrong
            // reason (an empty view) instead of the multi-section guard.
            MemorySegment flow = ObjC.msgSendId(
                    ObjC.msgSendId(ObjC.cls("NSCollectionViewFlowLayout"), ObjC.sel("alloc")), ObjC.sel("init"));
            cv3.setCollectionViewLayout(flow);
            cv3.reloadData();
            TestKit.check(cv3.numberOfSections() == 2,
                    "sections-only data source reports 2 sections (got " + cv3.numberOfSections() + ")");
            // Assert the *reason*, not just the type: with 2 sections the section-0 item
            // count is 0, so Bounds.index would also throw IAE. Only the multi-section
            // guard produces a message naming the section cause.
            try {
                cv3.itemAtIndex(0);
                TestKit.check(false, "itemAtIndex on a 2-section view did not throw");
            } catch (IllegalArgumentException e) {
                String m = e.getMessage();
                TestKit.check(m != null && m.contains("single-section"),
                        "itemAtIndex multi-section rejection names the section cause (got '" + m + "')");
            }
        } catch (Throwable t) {
            TestKit.check(false, "itemAtIndex multi-section block threw: " + t);
        }

        // --- 8j. NSGridView move* rejects toIndex == count (found by auditing the
        // Bounds.insertIndex call sites after the NSTableView.moveColumn fix) ---
        // Unlike NSTableView.moveRowAtIndex:toIndex: (which tolerates == count), the grid
        // raises NSInternalInconsistencyException "toIndex out of range" for it (verified).
        try {
            nsui.NSGridView grid = nsui.NSGridView.gridViewWithNumberOfColumnsRows(2, 2);
            TestKit.check(grid.numberOfRows() == 2 && grid.numberOfColumns() == 2,
                    "gridViewWithNumberOfColumnsRows(2,2) reports 2x2 (got "
                            + grid.numberOfRows() + "x" + grid.numberOfColumns() + ")");
            TestKit.expectThrows("NSGridView.moveRowAtIndexToIndex(0, rows) rejects",
                    IllegalArgumentException.class, () -> grid.moveRowAtIndexToIndex(0, 2));
            TestKit.expectThrows("NSGridView.moveColumnAtIndexToIndex(0, cols) rejects",
                    IllegalArgumentException.class, () -> grid.moveColumnAtIndexToIndex(0, 2));
            TestKit.noThrow("NSGridView.moveRowAtIndexToIndex(0,1) still works",
                    () -> grid.moveRowAtIndexToIndex(0, 1));
        } catch (Throwable t) {
            TestKit.check(false, "NSGridView move-guard block threw: " + t);
        }

        // --- 8k. corrections to two over-tight guards, plus nils that still aborted ---
        // An adversarial review showed two guards from the first text/guard pass rejected
        // calls that succeed natively (both re-verified here); and that four text nils were
        // still forwarded although they abort the JVM.
        try {
            TestKit.noThrow("NSControl.sendAction(NULL, NULL) is a harmless NO",
                    () -> slider.sendAction(null, null));
            nsui.NSMutableAttributedString emptyFromNil =
                    nsui.NSMutableAttributedString.create((nsui.NSAttributedString) null);
            TestKit.check(emptyFromNil != null && emptyFromNil.length() == 0,
                    "initWithAttributedString:nil yields an empty string, not an IAE");

            nsui.NSText t2 = nsui.NSText.create(new NSRect(0, 0, 200, 100));
            TestKit.expectThrows("NSText.setString(null) rejects nil string",
                    IllegalArgumentException.class, () -> t2.setString(null));
            TestKit.noThrow("NSText.setString(\"abc\") still works", () -> t2.setString("abc"));
            TestKit.expectThrows("NSText.replaceCharactersInRangeWithString(·, null) rejects",
                    IllegalArgumentException.class,
                    () -> t2.replaceCharactersInRangeWithString(new nsui.NSRange(0, 1), null));
            nsui.NSMutableAttributedString mut2 = nsui.NSMutableAttributedString.create("abc");
            TestKit.expectThrows("NSMutableAttributedString.replaceCharactersInRangeWithString(·, null) rejects",
                    IllegalArgumentException.class,
                    () -> mut2.replaceCharactersInRangeWithString(new nsui.NSRange(0, 1), null));
            nsui.NSTextView tv2 = nsui.NSTextView.create(new NSRect(0, 0, 200, 100));
            TestKit.expectThrows("NSTextView.setAttributedString(null) rejects nil",
                    IllegalArgumentException.class, () -> tv2.setAttributedString(null));
            TestKit.expectThrows("NSTextView.setAttributedStringValue(null) rejects nil",
                    IllegalArgumentException.class,
                    () -> tv2.setAttributedStringValue((nsui.NSAttributedString) null));
        } catch (Throwable t) {
            TestKit.check(false, "correction/guard block threw: " + t);
        }

        // --- 8l. DelegateProxy.dispose reclaims a registered instance ---
        // Before dispose existed, the only removal was the ObjC dealloc hook, which can
        // never run because nothing releases the alloc/init +1 and every delegate/target
        // property is weak — so each proxy leaked for the life of the process.
        try {
            int regBefore = DelegateProxy.registrySize();
            MemorySegment disposable = DelegateProxy.actionTarget("disposeProbe:", sender -> {});
            TestKit.check(DelegateProxy.registrySize() == regBefore + 1,
                    "actionTarget registers one entry (before=" + regBefore
                            + ", after=" + DelegateProxy.registrySize() + ")");
            DelegateProxy.dispose(disposable);
            TestKit.check(DelegateProxy.registrySize() == regBefore,
                    "dispose reclaims the registry entry (got " + DelegateProxy.registrySize() + ")");
            DelegateProxy.dispose(disposable);
            TestKit.check(DelegateProxy.registrySize() == regBefore, "dispose is idempotent");
            DelegateProxy.dispose(MemorySegment.NULL);
            TestKit.check(DelegateProxy.registrySize() == regBefore, "dispose(NULL) is a no-op");
        } catch (Throwable t) {
            TestKit.check(false, "DelegateProxy.dispose block threw: " + t);
        }

        // --- 8m. nil-legality corrections verified by probing ---
        try {
            // CAAnimation.h:96 marks the key path nullable, and nil is accepted natively
            // (returns a valid animation), so rejecting it was a false rejection.
            TestKit.check(nsui.CAAnimation.animationWithKeyPath(null) != null,
                    "animationWithKeyPath(null) returns an animation");

            // MTLDepthStencil.h / MTLSampler.h mark `label` nullable, but Metal asserts on
            // nil ("label must not be nil"); a silent no-op hid that, so it is now an IAE.
            nsui.MTLDepthStencilDescriptor ds = nsui.MTLDepthStencilDescriptor.create();
            TestKit.expectThrows("MTLDepthStencilDescriptor.setLabel(null) reports the Metal assertion",
                    IllegalArgumentException.class, () -> ds.setLabel(null));
            TestKit.noThrow("MTLDepthStencilDescriptor.setLabel(\"x\") works", () -> ds.setLabel("x"));
            nsui.MTLSamplerDescriptor sd = nsui.MTLSamplerDescriptor.create();
            TestKit.expectThrows("MTLSamplerDescriptor.setLabel(null) reports the Metal assertion",
                    IllegalArgumentException.class, () -> sd.setLabel(null));

            // The display-link factories take a required selector; a NULL SEL used to be
            // handed to the runtime (or NPE'd in ObjC.sel) instead of being reported.
            NSView dlView = NSView.create(new NSRect(0, 0, 20, 20), (c, d) -> {});
            TestKit.expectThrows("NSView.displayLink(NULL selector) rejects",
                    IllegalArgumentException.class,
                    () -> dlView.displayLink(MemorySegment.NULL, MemorySegment.NULL));
        } catch (Throwable t) {
            TestKit.check(false, "nil-legality correction block threw: " + t);
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
