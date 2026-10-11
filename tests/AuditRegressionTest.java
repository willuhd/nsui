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

            // Row-mutation RANGES, measured inside the required precondition (view-based
            // table + begin/endUpdates, which this asserts nothing about because the guard
            // fires first). Verified natively on this exact setup: insert accepts index ==
            // count and aborts at count+1; remove/hide/unhide require index < count.
            // Without the guard each of these killed the JVM with
            // NSTableViewException "error inserting/removing/moving row N (numberOfRows: 3)".
            MemorySegment set4 = nsui.NSIndexSet.indexSetWithIndex(4).peer();
            MemorySegment set3 = nsui.NSIndexSet.indexSetWithIndex(3).peer();
            TestKit.expectThrows("insertRowsAtIndexes(row 4 > count) rejects",
                    IllegalArgumentException.class, () -> tvRows.insertRowsAtIndexes(set4, 0));
            TestKit.expectThrows("removeRowsAtIndexes(row 3 == count) rejects",
                    IllegalArgumentException.class, () -> tvRows.removeRowsAtIndexes(set3, 0));
            TestKit.expectThrows("hideRowsAtIndexes(row 3 == count) rejects",
                    IllegalArgumentException.class, () -> tvRows.hideRowsAtIndexes(set3, 0));
            TestKit.expectThrows("unhideRowsAtIndexes(row 3 == count) rejects",
                    IllegalArgumentException.class, () -> tvRows.unhideRowsAtIndexes(set3, 0));

            // moveRowAtIndex:toIndex: — both indexes bounded. The DISCRIMINATING assertions
            // are the three expectThrows below: with the bound removed the JVM dies there.
            // The stability check is a baseline/documentation oracle only — under an unbounded
            // guard these same in-range moves would also run — so it is written against
            // numberOfRows() (an 0 == 0 comparison would pass trivially).
            // Measured raw: valid (0,1)/(0,2) repeat as 3,3,3 subviews for 3 rows, while
            // (0,4) and (0,-1) grow 3,4,5 and (0,3) == count grows 3,4,4.
            // The FIRST move realizes the row views (0 -> rows), so the baseline comes after it.
            tvRows.beginUpdates();
            try {
                tvRows.moveRowAtIndex(0, 2);
            } finally {
                tvRows.endUpdates();
            }
            long subviewsStable = tvRows.subviews() == null ? 0 : tvRows.subviews().count();
            tvRows.beginUpdates();
            try {
                tvRows.moveRowAtIndex(0, 2);
            } finally {
                tvRows.endUpdates();
            }
            long subviewsAfterValid = tvRows.subviews() == null ? 0 : tvRows.subviews().count();
            TestKit.check(subviewsAfterValid == tvRows.numberOfRows(),
                    "a valid moveRowAtIndex leaves one row view per row (subviews "
                            + subviewsAfterValid + ", rows " + tvRows.numberOfRows() + ")");
            TestKit.expectThrows("moveRowAtIndex(0, 3) rejects (== count: accumulates row views)",
                    IllegalArgumentException.class, () -> tvRows.moveRowAtIndex(0, 3));
            TestKit.expectThrows("moveRowAtIndex(0, 4) rejects (count+1: accumulates row views)",
                    IllegalArgumentException.class, () -> tvRows.moveRowAtIndex(0, 4));
            TestKit.expectThrows("moveRowAtIndex(0, -1) rejects (negative: accumulates row views)",
                    IllegalArgumentException.class, () -> tvRows.moveRowAtIndex(0, -1));
            TestKit.expectThrows("moveRowAtIndex(3, 0) rejects (oldIndex == count)",
                    IllegalArgumentException.class, () -> tvRows.moveRowAtIndex(3, 0));
            TestKit.expectThrows("moveRowAtIndex(-1, 0) rejects",
                    IllegalArgumentException.class, () -> tvRows.moveRowAtIndex(-1, 0));

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

            // --- 8w. section MUTATIONS tolerate an out-of-range set (live data source) ---
            // Verified natively on a data-source-driven 2-section view: reloadSections: with
            // {0},{1},{2},{50} and deleteSections:/insertSections: with {2},{50} all return.
            // Pinned as noThrow so a future range guard here would fail the suite instead of
            // silently over-rejecting a call AppKit accepts. A fresh view/source is used so
            // these mutations cannot perturb the assertions above.
            MemorySegment mutDs = DelegateProxy.delegate("NSObject", "NSUISectionsOnlyDS3",
                    java.util.Map.of(), java.util.Map.of(),
                    java.util.Map.of("numberOfSectionsInCollectionView:", sender -> 2L),
                    java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
            nsui.NSCollectionView cvMut = nsui.NSCollectionView.create(new NSRect(0, 0, 200, 100));
            cvMut.setDataSource(mutDs);
            cvMut.setCollectionViewLayout(ObjC.msgSendId(
                    ObjC.msgSendId(ObjC.cls("NSCollectionViewFlowLayout"), ObjC.sel("alloc")), ObjC.sel("init")));
            cvMut.reloadData();
            TestKit.check(cvMut.numberOfSections() == 2,
                    "section-mutation view reports 2 sections (got " + cvMut.numberOfSections() + ")");
            MemorySegment sec2 = nsui.NSIndexSet.indexSetWithIndex(2).peer();
            MemorySegment sec50 = nsui.NSIndexSet.indexSetWithIndex(50).peer();
            TestKit.noThrow("reloadSections({2}) tolerated (== sectionCount)",
                    () -> cvMut.reloadSections(sec2));
            TestKit.noThrow("reloadSections({50}) tolerated",
                    () -> cvMut.reloadSections(sec50));
            TestKit.noThrow("deleteSections({50}) tolerated",
                    () -> cvMut.deleteSections(sec50));
            TestKit.noThrow("insertSections({50}) tolerated",
                    () -> cvMut.insertSections(sec50));
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

        // --- 8l. DelegateProxy.dispose actually frees the proxy ---
        // Before dispose existed, the only removal was the ObjC dealloc hook, which can
        // never run because nothing releases the alloc/init +1 and every delegate/target
        // property is weak — so each proxy leaked for the life of the process.
        // registrySize() alone is NOT enough: the first dispose removed the entry before
        // releasing, so the dealloc override could not find its holder and skipped
        // [super dealloc], leaving the object shell allocated. deallocChainedCount() is the
        // oracle for that half. (It pins that the chain RAN, not which superclass source was
        // used: with the dispatchDealloc fallback in place, remove-first would chain too.)
        try {
            int regBefore = DelegateProxy.registrySize();
            long chainedBefore = DelegateProxy.deallocChainedCount();
            MemorySegment disposable = DelegateProxy.actionTarget("disposeProbe:", sender -> {});
            TestKit.check(DelegateProxy.registrySize() == regBefore + 1,
                    "actionTarget registers one entry (before=" + regBefore
                            + ", after=" + DelegateProxy.registrySize() + ")");
            DelegateProxy.dispose(disposable);
            TestKit.check(DelegateProxy.registrySize() == regBefore,
                    "dispose reclaims the registry entry (got " + DelegateProxy.registrySize() + ")");
            TestKit.check(DelegateProxy.deallocChainedCount() == chainedBefore + 1,
                    "dispose drives dealloc through [super dealloc] (chained="
                            + DelegateProxy.deallocChainedCount() + ")");
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
            // The display-link SELECTOR VALUE is not validated: a NULL SEL is accepted
            // natively and yields a link that never calls back (verified), so the earlier
            // "reject a NULL selector" guard here was over-tight. The String overloads
            // still reject a null NAME because the wrapper must resolve it (ObjC.sel(null)
            // would NPE).
            NSView dlView = NSView.create(new NSRect(0, 0, 20, 20), (c, d) -> {});
            TestKit.check(dlView.displayLink(MemorySegment.NULL, MemorySegment.NULL) != null,
                    "NSView.displayLink(NULL selector) is accepted natively (link never fires)");
        } catch (Throwable t) {
            TestKit.check(false, "nil-legality correction block threw: " + t);
        }

        // --- 8n. NSTextView smart* methods abort on an out-of-range replacement range ---
        // Found by sweeping every NSRange-parameter method for missing validation: the
        // invalidate/ensure/temp-attribute families are all tolerant natively, but these
        // three raise NSRangeException (verified, exit 134). The boundary differs per method
        // and is pinned in 8p: smartDeleteRangeForProposedRange uses the full range check
        // (loc + len within the text), while the two smartInsert* methods only read the
        // LOCATION — a length that runs past the end is tolerated.
        try {
            nsui.NSTextView stv = nsui.NSTextView.create(new NSRect(0, 0, 200, 100));
            stv.setString("abc");
            TestKit.expectThrows("NSTextView.smartDeleteRangeForProposedRange out-of-range rejects",
                    IllegalArgumentException.class,
                    () -> stv.smartDeleteRangeForProposedRange(new nsui.NSRange(100, 1)));
            TestKit.noThrow("NSTextView.smartDeleteRangeForProposedRange(0,3) still works",
                    () -> stv.smartDeleteRangeForProposedRange(new nsui.NSRange(0, 3)));
            TestKit.expectThrows("NSTextView.smartInsertBeforeString… out-of-range rejects",
                    IllegalArgumentException.class,
                    () -> stv.smartInsertBeforeStringForStringReplacingRange("x", new nsui.NSRange(100, 1)));
            TestKit.expectThrows("NSTextView.smartInsertAfterString… out-of-range rejects",
                    IllegalArgumentException.class,
                    () -> stv.smartInsertAfterStringForStringReplacingRange("x", new nsui.NSRange(100, 1)));
            TestKit.noThrow("NSTextView.smartInsertBeforeString…(0,3) still works",
                    () -> stv.smartInsertBeforeStringForStringReplacingRange("x", new nsui.NSRange(0, 3)));
        } catch (Throwable t) {
            TestKit.check(false, "smart* range-guard block threw: " + t);
        }

        // --- 8o. NSToolbar item indexes: only the negative half is validated ---
        // The native assertion for remove is 0 <= index < _currentItems count. A count-based
        // Java guard was tried and had to be reverted: an item inserted with an identifier
        // AppKit does not realize leaves -items empty, and whether removeItemAtIndex(0) then
        // succeeds or aborts depends on that unrealized state (it succeeds when nothing
        // queried the toolbar in between, aborts after an items()/itemIdentifiers() query) —
        // two states Java cannot tell apart. So only a negative index is checked; it raises
        // natively ("index != -1" / "index>=0 && …", verified, exit 134).
        try {
            nsui.NSToolbar tb = nsui.NSToolbar.create("round4toolbar");
            TestKit.expectThrows("NSToolbar.removeItemAtIndex(-1) rejects",
                    IllegalArgumentException.class, () -> tb.removeItemAtIndex(-1));
            TestKit.expectThrows("NSToolbar.insertItemWithItemIdentifier(x, -1) rejects",
                    IllegalArgumentException.class,
                    () -> tb.insertItemWithItemIdentifier("NSToolbarFlexibleSpaceItem", -1));
            // "item1" is an identifier AppKit does NOT realize — the exact state in which a
            // count-based guard (items().count() == 0) wrongly rejected the removal. Using a
            // system identifier here would pass under both guards and pin nothing.
            TestKit.noThrow("NSToolbar.insertItemWithItemIdentifier(item1, 0) is legal",
                    () -> tb.insertItemWithItemIdentifier("item1", 0));
            TestKit.noThrow("NSToolbar.removeItemAtIndex(0) after an unrealized insert is legal",
                    () -> tb.removeItemAtIndex(0));
            // Deliberately NOT asserted: querying items()/itemIdentifiers() between those two
            // calls flips the toolbar into the state where removeItemAtIndex(0) aborts the
            // JVM uncatchably. See the NSToolbar block comment.
        } catch (Throwable t) {
            TestKit.check(false, "NSToolbar index-guard block threw: " + t);
        }

        // --- 8q. guards found by the third probe pass ---
        try {
            nsui.NSSegmentedControl seg = nsui.NSSegmentedControl.create(new NSRect(0, 0, 200, 30));
            seg.setSegmentCount(2);
            TestKit.check(seg.segmentCount() == 2, "segmented control has 2 segments");
            // Measured: -1, 0..count-1 accepted; count and above raise NSRangeException.
            TestKit.noThrow("setSelectedSegment(-1) clears the selection (accepted natively)",
                    () -> seg.setSelectedSegment(-1));
            TestKit.noThrow("setSelectedSegment(1) is legal", () -> seg.setSelectedSegment(1));
            TestKit.expectThrows("setSelectedSegment(2) rejects (== segmentCount)",
                    IllegalArgumentException.class, () -> seg.setSelectedSegment(2));
            TestKit.expectThrows("setSelectedSegment(5) rejects",
                    IllegalArgumentException.class, () -> seg.setSelectedSegment(5));

            // Per-segment accessors: only setSelected:/isSelectedForSegment: raise natively
            // (re-probed all of them with segment 9 on this 2-segment control; everything else
            // exited 0). Both are guarded; the tolerance of the rest is pinned so a future
            // sweep does not add guards that would over-reject.
            TestKit.expectThrows("isSelectedForSegment(9) rejects (native NSRangeException)",
                    IllegalArgumentException.class, () -> seg.isSelectedForSegment(9));
            TestKit.noThrow("isSelectedForSegment(1) is legal", () -> seg.isSelectedForSegment(1));
            TestKit.expectThrows("setSelectedForSegment(true, 9) rejects",
                    IllegalArgumentException.class, () -> seg.setSelectedForSegment(true, 9));
            TestKit.noThrow("setEnabledForSegment(true, 9) is tolerated natively",
                    () -> seg.setEnabledForSegment(true, 9));
            TestKit.noThrow("setWidthForSegment(10, 9) is tolerated natively",
                    () -> seg.setWidthForSegment(10, 9));
            TestKit.noThrow("setToolTipForSegment(t, 9) is tolerated natively",
                    () -> seg.setToolTipForSegment("t", 9));
            TestKit.noThrow("setTagForSegment(1, 9) is tolerated natively",
                    () -> seg.setTagForSegment(1, 9));
            TestKit.noThrow("labelForSegment(9) is tolerated natively",
                    () -> seg.labelForSegment(9));
            TestKit.noThrow("widthForSegment(9) is tolerated natively",
                    () -> seg.widthForSegment(9));

            // The native factory asserts "attributedStringValue != nil".
            TestKit.expectThrows("NSTextField.labelWithAttributedString(null) rejects",
                    IllegalArgumentException.class, () -> nsui.NSTextField.labelWithAttributedString(null));
            TestKit.noThrow("NSTextField.labelWithAttributedString(attr) still works",
                    () -> nsui.NSTextField.labelWithAttributedString(nsui.NSAttributedString.create("x")));

            // Null NSRect used to be a bare NPE from toSegment(); now a named IAE.
            TestKit.expectThrows("NSWindow.create(null rect) reports a named IAE",
                    IllegalArgumentException.class, () -> NSWindow.create(null, 0, 2, false));
            TestKit.expectThrows("NSPanel.create(null rect) reports a named IAE",
                    IllegalArgumentException.class, () -> nsui.NSPanel.create(null, 0, 2, false));

            // addTabbedWindow: nil aborts natively; the wrapper used to return silently,
            // suppressing the error. Now a named IAE.
            NSWindow tabWin = TestKit.hiddenWindow(120, 80);
            TestKit.expectThrows("NSWindow.addTabbedWindow(null) rejects",
                    IllegalArgumentException.class, () -> tabWin.addTabbedWindow(null, 0));
        } catch (Throwable t) {
            TestKit.check(false, "probe-found guard block threw: " + t);
        }

        // --- 8r. NSMutableOrderedSet.moveObjectsAtIndexes:toIndex: bound is count - k ---
        // The destination indexes the array AFTER the moved objects are removed (verified on
        // a 3-element set: moving {0,1} accepts toIndex 1, raises for 2; moving {0} accepts 2,
        // raises for 3). The old bound (insertIndex against count()) accepted count, which
        // aborts natively — an under-tight guard rather than an over-tight one.
        try {
            nsui.NSMutableOrderedSet os = nsui.NSMutableOrderedSet.orderedSetWithCapacity(3);
            for (int i = 0; i < 3; i++) os.addObject(nsui.NSString.of("v" + i));
            TestKit.check(os.count() == 3, "ordered set has 3 elements (got " + os.count() + ")");
            nsui.NSMutableIndexSet both = nsui.NSMutableIndexSet.indexSet();
            both.addIndex(0);
            both.addIndex(1);
            TestKit.noThrow("moveObjectsAtIndexes({0,1}, 1) is legal (== count-k)",
                    () -> os.moveObjectsAtIndexesToIndex(both, 1));
            TestKit.expectThrows("moveObjectsAtIndexes({0,1}, 2) rejects (> count-k)",
                    IllegalArgumentException.class, () -> os.moveObjectsAtIndexesToIndex(both, 2));
            nsui.NSMutableIndexSet one = nsui.NSMutableIndexSet.indexSet();
            one.addIndex(0);
            TestKit.expectThrows("moveObjectsAtIndexes({0}, 3) rejects (> count-k)",
                    IllegalArgumentException.class, () -> os.moveObjectsAtIndexesToIndex(one, 3));
            TestKit.noThrow("moveObjectsAtIndexes({0}, 2) is legal (== count-k)",
                    () -> os.moveObjectsAtIndexesToIndex(one, 2));
        } catch (Throwable t) {
            TestKit.check(false, "ordered-set move-guard block threw: " + t);
        }

        // --- 8s. nonnull object arguments that abort (or crash) natively ---
        // CALayer.replaceSublayer:with: with a nil OLD layer is a hard SIGSEGV (exit 139,
        // no exception, no message) — the guard is what keeps this test JVM alive.
        try {
            nsui.CALayer cl = nsui.CALayer.create();
            nsui.CALayer oldL = nsui.CALayer.create();
            cl.addSublayer(oldL);
            TestKit.expectThrows("CALayer.replaceSublayer(null, x) rejects (native SIGSEGV)",
                    IllegalArgumentException.class, () -> cl.replaceSublayer(null, nsui.CALayer.create()));
            TestKit.noThrow("CALayer.replaceSublayer(old, null) is tolerated natively",
                    () -> cl.replaceSublayer(oldL, null));
            TestKit.expectThrows("CALayer.addConstraint(null) rejects",
                    IllegalArgumentException.class, () -> cl.addConstraint(null));

            nsui.NSLayoutManager lm2 = nsui.NSLayoutManager.create();
            TestKit.expectThrows("NSLayoutManager.addTextContainer(null) rejects",
                    IllegalArgumentException.class, () -> lm2.addTextContainer(null));
            TestKit.noThrow("NSLayoutManager.addTextContainer(container) still works",
                    () -> lm2.addTextContainer(nsui.NSTextContainer.create()));

            nsui.NSToolbar tb2 = nsui.NSToolbar.create("round9toolbar");
            TestKit.expectThrows("NSToolbar.insertItemWithItemIdentifier(NULL id, 0) rejects",
                    IllegalArgumentException.class,
                    () -> tb2.insertItemWithItemIdentifier((MemorySegment) null, 0));
            // Exercises Bounds.nonNull's address()==0 branch (the Java-null branch is above).
            TestKit.expectThrows("NSToolbar.insertItemWithItemIdentifier(MemorySegment.NULL, 0) rejects",
                    IllegalArgumentException.class,
                    () -> tb2.insertItemWithItemIdentifier(MemorySegment.NULL, 0));
        } catch (Throwable t) {
            TestKit.check(false, "nonnull-object guard block threw: " + t);
        }

        // --- 8v. NSCollectionView.moveItemAtIndexPath: needs EXISTING items ---
        // Verified natively on a POPULATED 2-item view: moving from (5,0) or to (2,0) / (0,1)
        // raises NSInternalInconsistencyException and aborts, while (0,0)->(1,0) and
        // (1,0)->(0,0) are accepted. The insert/delete/reload siblings tolerate an
        // out-of-range path (also verified on the populated view), so only move is guarded.
        try {
            nsui.NSCollectionView mcv = nsui.NSCollectionView.create(new NSRect(0, 0, 200, 100));
            // The layout is required before setContent: without it AppKit aborts with
            // "NSCollectionView item prototype must not be nil" (measured while writing this).
            mcv.setCollectionViewLayout(ObjC.msgSendId(ObjC.cls("NSCollectionViewFlowLayout"), ObjC.sel("new")));
            MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
            for (int i = 0; i < 2; i++) {
                MemorySegment item = ObjC.msgSendId(
                        ObjC.msgSendId(ObjC.cls("NSCollectionViewItem"), ObjC.sel("alloc")),
                        ObjC.sel("init"));
                ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), item);
            }
            mcv.setContent(arr);
            TestKit.check(mcv.numberOfItemsInSection(0) == 2,
                    "populated collection view reports 2 items (got " + mcv.numberOfItemsInSection(0) + ")");
            var pathHandle = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.ID,
                    nsui.objc.Sig.Arg.INT, nsui.objc.Sig.Arg.INT));
            java.util.function.BiFunction<Long, Long, MemorySegment> path = (it, sec) -> {
                try {
                    return (MemorySegment) pathHandle.invokeExact(ObjC.cls("NSIndexPath"),
                            ObjC.sel("indexPathForItem:inSection:"), it.longValue(), sec.longValue());
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            };
            MemorySegment p00 = path.apply(0L, 0L);
            MemorySegment p10 = path.apply(1L, 0L);
            MemorySegment p20 = path.apply(2L, 0L);
            MemorySegment p50 = path.apply(5L, 0L);
            MemorySegment p01 = path.apply(0L, 1L);
            MemorySegment pNeg = path.apply(-1L, 0L);
            TestKit.noThrow("moveItemAtIndexPath (0,0)->(1,0) is legal",
                    () -> mcv.moveItemAtIndexPath(p00, p10));
            TestKit.expectThrows("moveItemAtIndexPath (0,0)->(2,0) rejects (destination == count)",
                    IllegalArgumentException.class, () -> mcv.moveItemAtIndexPath(p00, p20));
            TestKit.expectThrows("moveItemAtIndexPath (2,0)->(0,0) rejects (source == count)",
                    IllegalArgumentException.class, () -> mcv.moveItemAtIndexPath(p20, p00));
            TestKit.expectThrows("moveItemAtIndexPath (5,0)->(0,0) rejects (no such source)",
                    IllegalArgumentException.class, () -> mcv.moveItemAtIndexPath(p50, p00));
            TestKit.expectThrows("moveItemAtIndexPath (0,0)->(0,1) rejects (no such section)",
                    IllegalArgumentException.class, () -> mcv.moveItemAtIndexPath(p00, p01));
            // Asymmetry pinned: AppKit bounds `item` only ABOVE, so a negative item is
            // accepted (a 144-pair matrix found both directions return without raising),
            // while a negative SECTION does abort. Rejecting the negative item would be a
            // false rejection.
            TestKit.noThrow("moveItemAtIndexPath (0,0)->(-1,0) is tolerated natively",
                    () -> mcv.moveItemAtIndexPath(p00, pNeg));
        } catch (Throwable t) {
            TestKit.check(false, "collection-view move-guard block threw: " + t);
        }

        // --- 8p. range boundaries the first sweep got wrong ---
        // An adversarial review re-probed these. smartInsert* abort only on an out-of-range
        // LOCATION ({3,1} and {2,5} on a 3-char view are accepted natively, so the earlier
        // Bounds.range was a false rejection); NSLayoutManager setTextContainer:forGlyphRange:
        // aborts on a non-empty range past the glyph count but accepts {4,0} (empty);
        // NSText.scrollRangeToVisible: only aborts for a negative location.
        try {
            nsui.NSTextView rv = nsui.NSTextView.create(new NSRect(0, 0, 200, 100));
            rv.setString("abc");
            TestKit.noThrow("smartInsertBefore(3,1) accepted (location == textLength)",
                    () -> rv.smartInsertBeforeStringForStringReplacingRange("x", new nsui.NSRange(3, 1)));
            TestKit.noThrow("smartInsertBefore(2,5) accepted (length runs past the end)",
                    () -> rv.smartInsertBeforeStringForStringReplacingRange("x", new nsui.NSRange(2, 5)));
            TestKit.expectThrows("smartInsertBefore(4,0) rejects (location > textLength)",
                    IllegalArgumentException.class,
                    () -> rv.smartInsertBeforeStringForStringReplacingRange("x", new nsui.NSRange(4, 0)));
            TestKit.expectThrows("smartInsertAfter(-1,5) rejects",
                    IllegalArgumentException.class,
                    () -> rv.smartInsertAfterStringForStringReplacingRange("x", new nsui.NSRange(-1, 5)));
            TestKit.expectThrows("smartDeleteRangeForProposedRange(3,1) rejects",
                    IllegalArgumentException.class,
                    () -> rv.smartDeleteRangeForProposedRange(new nsui.NSRange(3, 1)));

            TestKit.expectThrows("NSText.scrollRangeToVisible(-1,5) rejects",
                    IllegalArgumentException.class, () -> rv.scrollRangeToVisible(new nsui.NSRange(-1, 5)));
            TestKit.noThrow("NSText.scrollRangeToVisible(100,1) is tolerated",
                    () -> rv.scrollRangeToVisible(new nsui.NSRange(100, 1)));

            nsui.NSLayoutManager lm = rv.layoutManager();
            nsui.NSTextContainer tc = rv.textContainer();
            if (lm != null && tc != null) {
                TestKit.check(lm.numberOfGlyphs() == 3,
                        "layout manager reports 3 glyphs (got " + lm.numberOfGlyphs() + ")");
                TestKit.expectThrows("NSLayoutManager.setTextContainerForGlyphRange(2,5) rejects",
                        IllegalArgumentException.class,
                        () -> lm.setTextContainerForGlyphRange(tc, new nsui.NSRange(2, 5)));
                TestKit.noThrow("NSLayoutManager.setTextContainerForGlyphRange(4,0) is accepted (empty)",
                        () -> lm.setTextContainerForGlyphRange(tc, new nsui.NSRange(4, 0)));
            } else {
                TestKit.probe("layout manager / text container unavailable");
            }
        } catch (Throwable t) {
            TestKit.check(false, "range-boundary block threw: " + t);
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

        // --- 8aa. NSSplitView holding priority: float shape, priority > 0 ---
        // Was omitted for a missing Sig shape; the shapes are registered now. Measured: the
        // priority must be > 0 (0.0 and -5.0 abort with "priority > 0", 0.001 and 1.0 are
        // accepted) while the INDEX is unvalidated (0, 1 and 5 all return on a 2-subview view).
        try {
            nsui.NSSplitView spv = nsui.NSSplitView.create(new NSRect(0, 0, 300, 100));
            spv.addArrangedSubview(NSView.create(new NSRect(0, 0, 100, 100), (v, d) -> {}));
            spv.addArrangedSubview(NSView.create(new NSRect(0, 0, 100, 100), (v, d) -> {}));
            TestKit.noThrow("setHoldingPriorityForSubviewAtIndex(250, 0) is legal",
                    () -> spv.setHoldingPriorityForSubviewAtIndex(250.0f, 0));
            TestKit.check(spv.holdingPriorityForSubviewAtIndex(0) == 250.0f,
                    "holding priority round-trips (got " + spv.holdingPriorityForSubviewAtIndex(0) + ")");
            TestKit.expectThrows("setHoldingPriorityForSubviewAtIndex(0, 0) rejects (native assert)",
                    IllegalArgumentException.class,
                    () -> spv.setHoldingPriorityForSubviewAtIndex(0.0f, 0));
            TestKit.expectThrows("setHoldingPriorityForSubviewAtIndex(-5, 0) rejects",
                    IllegalArgumentException.class,
                    () -> spv.setHoldingPriorityForSubviewAtIndex(-5.0f, 0));
            TestKit.expectThrows("setHoldingPriorityForSubviewAtIndex(NaN, 0) rejects",
                    IllegalArgumentException.class,
                    () -> spv.setHoldingPriorityForSubviewAtIndex(Float.NaN, 0));
            TestKit.noThrow("setHoldingPriorityForSubviewAtIndex(250, 5) is a native no-op",
                    () -> spv.setHoldingPriorityForSubviewAtIndex(250.0f, 5));
        } catch (Throwable t) {
            TestKit.check(false, "split-view holding-priority block threw: " + t);
        }

        // --- 8ab. NSStackView priorities: floated shapes + arranged-view membership ---
        // Six methods were omitted for missing Sig shapes; the shapes are registered now.
        // Measured: both visibility accessors abort for a view that is not an ARRANGED
        // subview (a plain addSubview: view aborts too), while priority values 0/-5 and
        // orientations 0,1,2,9 are all tolerated.
        try {
            nsui.NSStackView stk = nsui.NSStackView.create(new NSRect(0, 0, 200, 100));
            NSView arranged = NSView.create(new NSRect(0, 0, 50, 50), (v, d) -> {});
            NSView stranger = NSView.create(new NSRect(0, 0, 50, 50), (v, d) -> {});
            stk.addArrangedSubview(arranged);
            TestKit.noThrow("setVisibilityPriorityForView(500, arranged) is legal",
                    () -> stk.setVisibilityPriorityForView(500.0f, arranged));
            TestKit.check(stk.visibilityPriorityForView(arranged) == 500.0f,
                    "visibility priority round-trips (got " + stk.visibilityPriorityForView(arranged) + ")");
            TestKit.expectThrows("visibilityPriorityForView(stranger) rejects (not arranged)",
                    IllegalArgumentException.class, () -> stk.visibilityPriorityForView(stranger));
            TestKit.expectThrows("setVisibilityPriorityForView(500, stranger) rejects",
                    IllegalArgumentException.class, () -> stk.setVisibilityPriorityForView(500.0f, stranger));
            TestKit.noThrow("setVisibilityPriorityForView(0, arranged) is tolerated",
                    () -> stk.setVisibilityPriorityForView(0.0f, arranged));
            TestKit.noThrow("setHuggingPriorityForOrientation(250, 0) is legal",
                    () -> stk.setHuggingPriorityForOrientation(250.0f, 0));
            TestKit.check(Math.abs(stk.huggingPriorityForOrientation(0) - 250.0f) < 0.01f,
                    "hugging priority round-trips (got " + stk.huggingPriorityForOrientation(0) + ")");
            TestKit.noThrow("setHuggingPriorityForOrientation(250, 9) is tolerated (orientation unvalidated)",
                    () -> stk.setHuggingPriorityForOrientation(250.0f, 9));
            TestKit.noThrow("setClippingResistancePriorityForOrientation(250, 0) is legal",
                    () -> stk.setClippingResistancePriorityForOrientation(250.0f, 0));
            TestKit.check(Math.abs(stk.clippingResistancePriorityForOrientation(0) - 250.0f) < 0.01f,
                    "clipping priority round-trips (got " + stk.clippingResistancePriorityForOrientation(0) + ")");
        } catch (Throwable t) {
            TestKit.check(false, "stack-view priority block threw: " + t);
        }

        // --- 8ac. NSString.rangeOfComposedCharacterSequenceAtIndex: (newly implementable) ---
        // Was omitted for a missing of(RANGE,INT) shape. Measured on "abc": index 2 returns
        // {2,1}; index 3 (== length) and 5 abort with NSInvalidArgumentException.
        try {
            nsui.NSString s3 = nsui.NSString.of("abc");
            nsui.NSRange r0 = s3.rangeOfComposedCharacterSequenceAtIndex(0);
            TestKit.check(r0.location() == 0 && r0.length() == 1,
                    "composed range at 0 is {0,1} (got {" + r0.location() + "," + r0.length() + "})");
            nsui.NSRange r2 = s3.rangeOfComposedCharacterSequenceAtIndex(2);
            TestKit.check(r2.location() == 2 && r2.length() == 1,
                    "composed range at 2 is {2,1} (got {" + r2.location() + "," + r2.length() + "})");
            TestKit.expectThrows("rangeOfComposedCharacterSequenceAtIndex(length) rejects",
                    IllegalArgumentException.class, () -> s3.rangeOfComposedCharacterSequenceAtIndex(3));
            TestKit.expectThrows("rangeOfComposedCharacterSequenceAtIndex(-1) rejects",
                    IllegalArgumentException.class, () -> s3.rangeOfComposedCharacterSequenceAtIndex(-1));
        } catch (Throwable t) {
            TestKit.check(false, "composed-character-range block threw: " + t);
        }

        // --- 8ad. NSView needsToDrawRect:/scrollRectToVisible: (of(BOOL,RECT) now registered) ---
        try {
            NSView rv2 = NSView.create(new NSRect(0, 0, 100, 100), (v, d) -> {});
            TestKit.noThrow("needsToDrawRect(inside) does not raise",
                    () -> rv2.needsToDrawRect(new NSRect(0, 0, 10, 10)));
            TestKit.check(!rv2.scrollRectToVisible(new NSRect(0, 0, 10, 10)),
                    "scrollRectToVisible on a bare view returns false (no enclosing scroll view)");
        } catch (Throwable t) {
            TestKit.check(false, "NSView rect block threw: " + t);
        }

        // --- 8ae. two wrappers previously reported as unusable are functional ---
        // usedRectForTextContainer: and NSFontManager.fontPanel: were documented as throwing
        // at call time because their shapes were missing; both shapes ARE in Sig (listed by
        // these very methods), so assert they work — a future shape removal would now fail.
        try {
            nsui.NSTextView ltv = nsui.NSTextView.create(new NSRect(0, 0, 200, 100));
            ltv.setString("abc");
            NSRect used = ltv.layoutManager().usedRectForTextContainer(ltv.textContainer());
            TestKit.check(used != null, "usedRectForTextContainer returns an NSRect (no throw)");
            TestKit.noThrow("NSFontManager.fontPanel(false) does not throw",
                    () -> nsui.NSFontManager.sharedFontManager().fontPanel(false));
        } catch (Throwable t) {
            TestKit.check(false, "previously-dead wrapper block threw: " + t);
        }

        // --- 8af. NSGridView.mergeCellsInHorizontalRange:verticalRange: (newly implemented) ---
        // of(VOID,RANGE,RANGE) was missing. Measured on a 3x3 grid: locations 0, 1, 2 accepted;
        // 3 (== count) and 5 abort with NSRangeException.
        try {
            nsui.NSGridView mg = nsui.NSGridView.gridViewWithNumberOfColumnsRows(3, 3);
            TestKit.check(mg.numberOfColumns() == 3 && mg.numberOfRows() == 3,
                    "3x3 grid reports 3 columns and rows");
            TestKit.noThrow("mergeCells (0,0) on a 3x3 grid is legal",
                    () -> mg.mergeCellsInHorizontalRange(new nsui.NSRange(0, 1), new nsui.NSRange(0, 1)));
            TestKit.expectThrows("mergeCells horizontal at column count rejects",
                    IllegalArgumentException.class,
                    () -> mg.mergeCellsInHorizontalRange(new nsui.NSRange(3, 1), new nsui.NSRange(0, 1)));
            TestKit.expectThrows("mergeCells vertical at row count rejects",
                    IllegalArgumentException.class,
                    () -> mg.mergeCellsInHorizontalRange(new nsui.NSRange(0, 1), new nsui.NSRange(5, 1)));
            TestKit.expectThrows("mergeCells horizontal spanning past the grid rejects",
                    IllegalArgumentException.class,
                    () -> mg.mergeCellsInHorizontalRange(new nsui.NSRange(2, 2), new nsui.NSRange(0, 1)));
        } catch (Throwable t) {
            TestKit.check(false, "grid merge block threw: " + t);
        }

        // --- 8ag. NSView view-relative coordinate conversions (of(SIZE/RECT,SIZE/RECT,ID) now registered) ---
        // Child at (10,20) inside a parent: parent (0,0) is child-local (-10,-20) and vice
        // versa, so the round-trip is exact for the point family. A nil view (window
        // coordinates) must not raise either.
        try {
            NSView parentV = NSView.create(new NSRect(0, 0, 100, 100), (v, d) -> {});
            NSView childV = NSView.create(new NSRect(10, 20, 30, 30), (v, d) -> {});
            parentV.addSubview(childV);
            NSPoint fromParent = childV.convertPointFromView(new NSPoint(0, 0), parentV);
            TestKit.check(Math.abs(fromParent.x() + 10.0) < 0.001 && Math.abs(fromParent.y() + 20.0) < 0.001,
                    "convertPoint:fromView: maps parent (0,0) to child-local (-10,-20) (got "
                            + fromParent.x() + "," + fromParent.y() + ")");
            NSPoint backToParent = childV.convertPointToView(new NSPoint(0, 0), parentV);
            TestKit.check(Math.abs(backToParent.x() - 10.0) < 0.001 && Math.abs(backToParent.y() - 20.0) < 0.001,
                    "convertPoint:toView: maps child (0,0) to parent-local (10,20) (got "
                            + backToParent.x() + "," + backToParent.y() + ")");
            NSRect rf = childV.convertRectFromView(new NSRect(10, 20, 30, 30), parentV);
            TestKit.check(Math.abs(rf.x()) < 0.001 && Math.abs(rf.y()) < 0.001,
                    "convertRect:fromView: puts the child's own frame at the child origin (got "
                            + rf.x() + "," + rf.y() + ")");
            NSSize sz = childV.convertSizeFromView(new NSSize(30, 30), parentV);
            TestKit.check(Math.abs(sz.width() - 30.0) < 0.001 && Math.abs(sz.height() - 30.0) < 0.001,
                    "convertSize:fromView: is identity for an unscaled view (got "
                            + sz.width() + "," + sz.height() + ")");
            TestKit.noThrow("convertPoint:fromView: with a nil view (window coords) does not raise",
                    () -> childV.convertPointFromView(new NSPoint(1, 2), null));
            TestKit.noThrow("convertRect:toView: with a nil view does not raise",
                    () -> childV.convertRectToView(new NSRect(0, 0, 5, 5), null));
        } catch (Throwable t) {
            TestKit.check(false, "view conversion block threw: " + t);
        }

        // --- 8ah. NSString.rangeOfString:options:range: (newly implementable shape) ---
        // Measured on "abc" searching "b": {0,3} -> {1,1}; {3,0} (empty, at the end) is legal
        // and returns NSNotFound; {0,5}, {2,2} and {5,1} abort with NSRangeException.
        try {
            nsui.NSString hay = nsui.NSString.of("abc");
            nsui.NSRange found = hay.rangeOfString("b", 0L, new nsui.NSRange(0, 3));
            TestKit.check(found.location() == 1 && found.length() == 1,
                    "rangeOfString within {0,3} finds b at 1 (got {" + found.location() + "," + found.length() + "})");
            nsui.NSRange notFound = hay.rangeOfString("c", 0L, new nsui.NSRange(0, 2));
            TestKit.check(notFound.location() == nsui.NSRange.NOT_FOUND,
                    "rangeOfString within {0,2} does not find c (got loc " + notFound.location() + ")");
            TestKit.noThrow("rangeOfString with an empty range at the end is legal",
                    () -> hay.rangeOfString("b", 0L, new nsui.NSRange(3, 0)));
            TestKit.expectThrows("rangeOfString with an overflowing length rejects",
                    IllegalArgumentException.class, () -> hay.rangeOfString("b", 0L, new nsui.NSRange(0, 5)));
            TestKit.expectThrows("rangeOfString with loc+len past the end rejects",
                    IllegalArgumentException.class, () -> hay.rangeOfString("b", 0L, new nsui.NSRange(2, 2)));
        } catch (Throwable t) {
            TestKit.check(false, "rangeOfString range block threw: " + t);
        }

        // --- 8ai. NSView backing-geometry family (of(POINT,POINT) / of(RECT,RECT,INT)) ---
        // Round-trip based so it holds at 1x or 2x: toBacking then fromBacking must be exact,
        // and aligning an already-aligned rect must be idempotent.
        try {
            NSView bv = NSView.create(new NSRect(0, 0, 100, 100), (v, d) -> {});
            NSPoint p0 = new NSPoint(3.5, 7.25);
            NSPoint backPt = bv.convertPointFromBacking(bv.convertPointToBacking(p0));
            TestKit.check(Math.abs(backPt.x() - p0.x()) < 0.0001 && Math.abs(backPt.y() - p0.y()) < 0.0001,
                    "convertPointToBacking/FromBacking round-trips (got " + backPt.x() + "," + backPt.y() + ")");
            // Masks verified natively as VALID: 15, 51, 60, 3840, 983040 (see
            // validateAlignmentOptions). 0 is INVALID (measured) even though it reads like
            // "no alignment", and so are single-attribute and over-specified masks.
            NSRect alignedOnce = bv.backingAlignedRect(new NSRect(0.3, 0.3, 10.7, 10.7),
                    NSView.ALIGN_ALL_EDGES_INWARD);
            NSRect alignedTwice = bv.backingAlignedRect(alignedOnce, NSView.ALIGN_ALL_EDGES_INWARD);
            TestKit.check(Math.abs(alignedOnce.x() - alignedTwice.x()) < 0.0001
                            && Math.abs(alignedOnce.width() - alignedTwice.width()) < 0.0001,
                    "backingAlignedRect is idempotent (once " + alignedOnce + ")");
            TestKit.noThrow("backingAlignedRect accepts 51 (min+size, both axes)",
                    () -> bv.backingAlignedRect(new NSRect(0.3, 0.3, 10.7, 10.7), 51L));
            TestKit.noThrow("backingAlignedRect accepts 60 (max+size, both axes)",
                    () -> bv.backingAlignedRect(new NSRect(0.3, 0.3, 10.7, 10.7), 60L));
            for (long badMask : new long[] {0L, 1L, 5L, 17L, 21L, 48L, 63L, 65535L}) {
                final long b = badMask;
                TestKit.expectThrows("backingAlignedRect rejects invalid mask " + b,
                        IllegalArgumentException.class,
                        () -> bv.backingAlignedRect(new NSRect(0.3, 0.3, 10.7, 10.7), b));
            }
        } catch (Throwable t) {
            TestKit.check(false, "backing-geometry block threw: " + t);
        }

        // --- 8aj. NSString option/pad/replace variants (three more shapes registered) ---
        // Measured: compare:options: ignores unknown bits; stringByPaddingToLength: validates
        // startingAtIndex only when padding happens (newLength > length) and tolerates a
        // negative length; stringByReplacingOccurrencesOfString: needs both strings non-nil and
        // a range with loc+len <= length ({3,0} is legal, {0,5} and {2,2} abort).
        try {
            nsui.NSString base = nsui.NSString.of("abc");
            TestKit.check(base.compare("B", 1L) < 0 && base.compare("B", 0L) > 0,
                    "compare:options: is case-insensitive with option 1");
            TestKit.noThrow("compare:options: ignores an unknown option bit",
                    () -> base.compare("abc", 1L << 30));
            TestKit.check(base.stringByPaddingToLength(5, nsui.NSString.of("xy"), 0).toString().equals("abcxy"),
                    "padding to 5 with xy from 0 gives abcxy (got "
                            + base.stringByPaddingToLength(5, nsui.NSString.of("xy"), 0).toString() + ")");
            TestKit.noThrow("padding with start 5 is legal when no padding is needed",
                    () -> base.stringByPaddingToLength(2, nsui.NSString.of("xy"), 5));
            TestKit.expectThrows("padding with a negative length rejects (native NSMallocException)",
                    IllegalArgumentException.class,
                    () -> base.stringByPaddingToLength(-1, nsui.NSString.of("xy"), 0));
            TestKit.expectThrows("padding with start == pad length rejects when padding is needed",
                    IllegalArgumentException.class,
                    () -> base.stringByPaddingToLength(5, nsui.NSString.of("xy"), 2));
            TestKit.expectThrows("padding with a null pad string rejects",
                    IllegalArgumentException.class,
                    () -> base.stringByPaddingToLength(5, null, 0));
            TestKit.check(base.stringByReplacingOccurrencesOfString("b", "z", 0L, new nsui.NSRange(0, 3))
                            .toString().equals("azc"),
                    "replace within {0,3} gives azc");
            TestKit.noThrow("replace with an empty range at the end is legal",
                    () -> base.stringByReplacingOccurrencesOfString("b", "z", 0L, new nsui.NSRange(3, 0)));
            TestKit.expectThrows("replace with an overflowing range rejects",
                    IllegalArgumentException.class,
                    () -> base.stringByReplacingOccurrencesOfString("b", "z", 0L, new nsui.NSRange(0, 5)));
            TestKit.expectThrows("replace with a null target rejects",
                    IllegalArgumentException.class,
                    () -> base.stringByReplacingOccurrencesOfString(null, "z", 0L, new nsui.NSRange(0, 3)));
            TestKit.expectThrows("replace with a null replacement rejects",
                    IllegalArgumentException.class,
                    () -> base.stringByReplacingOccurrencesOfString("b", null, 0L, new nsui.NSRange(0, 3)));
        } catch (Throwable t) {
            TestKit.check(false, "NSString variant block threw: " + t);
        }

        // --- 8ak. self/ancestor insertion cycles (measured: SIGSEGV for views, 134 for others) ---
        // Raw probes: v.addSubview(v) -> exit 139; stack.addArrangedSubview(stack) -> 139;
        // w.addChildWindow(w) -> 134 "cannot add self as child of itself";
        // layer.addSublayer(layer) -> 134 CALayerInvalid cycle. The indirect forms HANG
        // (>8 s), which the ancestry walk also prevents.
        try {
            NSView cv1 = NSView.create(new NSRect(0, 0, 100, 100), (v, d) -> {});
            NSView cv2 = NSView.create(new NSRect(0, 0, 50, 50), (v, d) -> {});
            NSView cv3 = NSView.create(new NSRect(0, 0, 25, 25), (v, d) -> {});
            cv1.addSubview(cv2);
            cv2.addSubview(cv3);
            TestKit.expectThrows("addSubview(self) rejects (native SIGSEGV)",
                    IllegalArgumentException.class, () -> cv1.addSubview(cv1));
            TestKit.expectThrows("addSubview(ancestor) rejects (would hang)",
                    IllegalArgumentException.class, () -> cv3.addSubview(cv1));
            TestKit.noThrow("re-parenting a descendant stays legal", () -> cv1.addSubview(cv3));

            nsui.NSStackView stk2 = nsui.NSStackView.create(new NSRect(0, 0, 100, 100));
            TestKit.expectThrows("addArrangedSubview(self) rejects (native SIGSEGV)",
                    IllegalArgumentException.class, () -> stk2.addArrangedSubview(stk2));
            NSView arranged2 = NSView.create(new NSRect(0, 0, 20, 20), (v, d) -> {});
            stk2.addArrangedSubview(arranged2);
            TestKit.expectThrows("removeArrangedSubview(twice) rejects (membership precondition)",
                    IllegalArgumentException.class,
                    () -> { stk2.removeArrangedSubview(arranged2); stk2.removeArrangedSubview(arranged2); });

            NSWindow cycleWin = TestKit.hiddenWindow(120, 80);
            TestKit.expectThrows("addChildWindow(self) rejects",
                    IllegalArgumentException.class, () -> cycleWin.addChildWindow(cycleWin, 1));

            nsui.CALayer cl1 = nsui.CALayer.create();
            nsui.CALayer cl2 = nsui.CALayer.create();
            cl1.addSublayer(cl2);
            TestKit.expectThrows("CALayer.addSublayer(self) rejects (native cycle abort)",
                    IllegalArgumentException.class, () -> cl1.addSublayer(cl1));
            TestKit.expectThrows("CALayer.addSublayer(ancestor) rejects",
                    IllegalArgumentException.class, () -> cl2.addSublayer(cl1));
            TestKit.noThrow("inserting a fresh layer stays legal",
                    () -> cl1.insertSublayer(nsui.CALayer.create(), 0));
        } catch (Throwable t) {
            TestKit.check(false, "self/ancestor cycle block threw: " + t);
        }

        // --- 8al. selectors nothing implements (NSText.setAttributedString:, NSMenu.submenuAction:) ---
        // Both were reachable aborts: NSText.create(frame) hands back a Java NSText over an
        // NSTextView peer, so the BASE setAttributedString: (unimplemented by NSText AND
        // NSTextView) was the one dispatched; NSMenu answers NO to submenuAction: on this host.
        try {
            nsui.NSAttributedString as = nsui.NSAttributedString.create("hello");
            nsui.NSText baseText = nsui.NSText.create(new NSRect(0, 0, 100, 60));
            TestKit.noThrow("NSText.setAttributedString(attr) works via textStorage",
                    () -> baseText.setAttributedString(as));
            TestKit.check("hello".equals(baseText.string()),
                    "the attributed string landed in the text (got " + baseText.string() + ")");
            TestKit.expectThrows("NSText.setAttributedString(null) rejects",
                    IllegalArgumentException.class, () -> baseText.setAttributedString(null));
            nsui.NSTextView tvAs = nsui.NSTextView.create(new NSRect(0, 0, 100, 60));
            TestKit.noThrow("NSTextView.setAttributedString(attr) still works",
                    () -> tvAs.setAttributedString(as));
            TestKit.check("hello".equals(tvAs.string()),
                    "NSTextView's attributed string landed too (got " + tvAs.string() + ")");
            NSMenu subMenu = NSMenu.createWithTitle("m");
            boolean hasSubmenuAction = (boolean) ObjC.handle(
                    nsui.objc.Sig.of(nsui.objc.Sig.Ret.BOOL, nsui.objc.Sig.Arg.ID))
                    .invokeExact(subMenu.peer(), ObjC.sel("respondsToSelector:"),
                            ObjC.sel("submenuAction:"));
            if (hasSubmenuAction) {
                TestKit.probe("submenuAction: IS implemented on this host (guard not exercised)");
            } else {
                TestKit.expectThrows("NSMenu.submenuAction() reports unimplemented instead of aborting",
                        IllegalStateException.class, () -> subMenu.submenuAction(null));
            }
        } catch (Throwable t) {
            TestKit.check(false, "unimplemented-selector block threw: " + t);
        }

        // --- 8am. foreign tab item + Metal set*Bytes cap (both measured aborts) ---
        // selectTabViewItem: with an item that is not in the tab view -> 134 natively;
        // setVertexBytes(8192) with MTL_DEBUG_LAYER=1 -> "must be <= 4096" assertion, 134.
        try {
            nsui.NSTabView tabV = nsui.NSTabView.create(new NSRect(0, 0, 200, 120));
            nsui.NSTabViewItem inItem = nsui.NSTabViewItem.create("in");
            nsui.NSTabViewItem foreign = nsui.NSTabViewItem.create("out");
            tabV.addTabViewItem(inItem);
            TestKit.noThrow("selectTabViewItem(added item) is legal", () -> tabV.selectTabViewItem(inItem));
            TestKit.expectThrows("selectTabViewItem(foreign item) rejects (native abort)",
                    IllegalArgumentException.class, () -> tabV.selectTabViewItem(foreign));
            TestKit.expectThrows("selectTabViewItem(null) rejects",
                    IllegalArgumentException.class, () -> tabV.selectTabViewItem(null));

            TestKit.check(nsui.MTLRenderCommandEncoder.MAX_SET_BYTES == 4096L,
                    "Metal set*Bytes cap is documented as 4096");
            if (nsui.MTLDevice.systemDefault() != null) {
                nsui.MTLCommandQueue q = MTLDevice.systemDefault().newCommandQueue();
                nsui.MTLTextureDescriptor td = nsui.MTLTextureDescriptor.texture2D(80L, 8, 8, false);
                td.setUsage(4L);
                nsui.MTLTexture tex = MTLDevice.systemDefault().newTexture(td);
                nsui.MTLRenderPassDescriptor pass = nsui.MTLRenderPassDescriptor.create();
                pass.colorAttachment(0).setTexture(tex);
                nsui.MTLRenderCommandEncoder enc = q.commandBuffer().renderEncoder(pass);
                TestKit.noThrow("setVertexBytes(4096) is accepted",
                        () -> enc.setVertexBytes(new byte[4096], 0));
                TestKit.expectThrows("setVertexBytes(8192) rejects (native assertion with validation)",
                        IllegalArgumentException.class, () -> enc.setVertexBytes(new byte[8192], 0));
                TestKit.expectThrows("setFragmentBytes(8192) rejects",
                        IllegalArgumentException.class, () -> enc.setFragmentBytes(new byte[8192], 0));
                enc.endEncoding();
            } else {
                TestKit.probe("no Metal device; set*Bytes cap checked by constant only");
            }
        } catch (Throwable t) {
            TestKit.check(false, "tab-item/Metal-cap block threw: " + t);
        }

        // --- 8an. subclass-only selectors on base-class peers (CAAnimation, NSLayoutAnchor) ---
        // Measured: a plain CAAnimation answers NO to setFromValue:/setKeyPath:/fromValue; an
        // NSLayoutXAxisAnchor (leadingAnchor) answers NO to the dimension-only constraint
        // methods while widthAnchor (an NSLayoutDimension) answers YES to all of them.
        try {
            nsui.CAAnimation plain = nsui.CAAnimation.wrap(
                    ObjC.msgSendId(ObjC.cls("CAAnimation"), ObjC.sel("animation")));
            TestKit.expectThrows("CAAnimation.setFromValue on a base peer rejects",
                    IllegalStateException.class, () -> plain.setFromValue(null));
            TestKit.expectThrows("CAAnimation.setKeyPath on a base peer rejects",
                    IllegalStateException.class, () -> plain.setKeyPath("opacity"));
            TestKit.expectThrows("CAAnimation.fromValue on a base peer rejects",
                    IllegalStateException.class, () -> plain.fromValue());
            nsui.CABasicAnimation basic = nsui.CABasicAnimation.wrap(
                    nsui.CAAnimation.animationWithKeyPath("opacity").peer());
            TestKit.noThrow("CABasicAnimation.setFromValue still works", () -> basic.setFromValue(null));
            TestKit.noThrow("CABasicAnimation.setKeyPath still works", () -> basic.setKeyPath("opacity"));

            NSView av = NSView.create(new NSRect(0, 0, 100, 100), (v, d) -> {});
            nsui.NSLayoutAnchor xAxis = av.leadingAnchor();
            TestKit.expectThrows("constraintEqualToConstant on an x-axis anchor rejects",
                    IllegalStateException.class, () -> xAxis.constraintEqualToConstant(10));
            TestKit.expectThrows("constraintEqualToAnchor:multiplier: on an x-axis anchor rejects",
                    IllegalStateException.class,
                    () -> xAxis.constraintEqualToAnchorWithMultiplier(av.trailingAnchor(), 2.0));
            TestKit.noThrow("the base constraintEqualToAnchor family still works on an x-axis anchor",
                    () -> xAxis.constraintEqualToAnchor(av.trailingAnchor()));
            nsui.NSLayoutAnchor.Dimension dim = av.widthAnchor();
            TestKit.noThrow("constraintEqualToConstant works on the typed width anchor",
                    () -> dim.constraintEqualToConstant(10));
        } catch (Throwable t) {
            TestKit.check(false, "subclass-only selector block threw: " + t);
        }

        // --- 8ao. selector/class names must not contain a NUL (silent truncation) ---
        try {
            TestKit.expectThrows("ObjC.sel with an embedded NUL rejects instead of truncating",
                    IllegalArgumentException.class, () -> ObjC.sel("addSubview:\u0000junk"));
            TestKit.expectThrows("ObjC.cls with an embedded NUL rejects",
                    IllegalArgumentException.class, () -> ObjC.cls("NSView\u0000junk"));
            TestKit.noThrow("ObjC.sel of a normal name still works",
                    () -> ObjC.sel("addSubview:"));
        } catch (Throwable t) {
            TestKit.check(false, "NUL-name block threw: " + t);
        }

        // --- 8ap. NSResponder action footguns are guarded, not documented ---
        // Measured intent: a plain NSView does not implement performTextFinderAction:,
        // showWritingTools: or newWindowForTab:, and performAction: forwards an arbitrary
        // selector, so any of them could abort with an unrecognized selector.
        try {
            NSView plainResponder = NSView.create(new NSRect(0, 0, 50, 50), (v, d) -> {});
            TestKit.expectThrows("performTextFinderAction on a plain view reports unimplemented",
                    IllegalStateException.class,
                    () -> plainResponder.performTextFinderAction(null));
            TestKit.expectThrows("showWritingTools on a plain view reports unimplemented",
                    IllegalStateException.class, () -> plainResponder.showWritingTools(null));
            TestKit.expectThrows("newWindowForTab on a plain view reports unimplemented",
                    IllegalStateException.class, () -> plainResponder.newWindowForTab(null));
            TestKit.expectThrows("performAction with an unimplemented selector rejects",
                    IllegalStateException.class,
                    () -> plainResponder.performAction("noSuchAction:", null));
            // insertText: is a selector a plain NSView really implements (verified: both
            // respondsToSelector: and a nil-sender call with exit 0), so the guard must let it
            // through — this is the over-tightening half of the check.
            TestKit.check(plainResponder.respondsToSelector(ObjC.sel("insertText:")),
                    "a plain NSView implements insertText: (guard precondition)");
            TestKit.noThrow("performAction with an implemented selector still works",
                    () -> plainResponder.performAction("insertText:", null));
        } catch (Throwable t) {
            TestKit.check(false, "responder action-guard block threw: " + t);
        }

        // --- 8aq. NSTableRowView.viewAtColumn: is guardable after all ---
        // A standalone row view reports 0 columns and aborts for every index (including 0), so
        // Bounds.index(column, numberOfColumns()) rejects them all; a table-owned row view
        // reports N and accepts 0..N-1. Both halves are pinned here.
        try {
            nsui.NSTableRowView lone = nsui.NSTableRowView.create(new NSRect(0, 0, 50, 50));
            TestKit.check(lone == null || lone.numberOfColumns() == 0,
                    "a standalone row view reports 0 columns");
            TestKit.expectThrows("viewAtColumn(0) on a standalone row view rejects (native abort)",
                    IllegalArgumentException.class, () -> lone.viewAtColumn(0));
        } catch (Throwable t) {
            TestKit.check(false, "standalone row-view block threw: " + t);
        }
        try {
            // A fresh populated 3-row/1-column view-based table (tvRows lives in another block).
            MemorySegment rowDs2 = DelegateProxy.delegate("NSObject", "NSUIRowViewDS2",
                    java.util.Map.of(), java.util.Map.of(),
                    java.util.Map.of("numberOfRowsInTableView:", sender -> 3L),
                    java.util.Map.of("tableView:viewForTableColumn:row:",
                            (t, c, r) -> NSView.create(new NSRect(0, 0, 10, 10), (v, d) -> {}).peer()),
                    java.util.Map.of(), java.util.Map.of(), java.util.Map.of());
            nsui.NSTableView ownedTable = nsui.NSTableView.create(new NSRect(0, 0, 200, 100));
            ownedTable.addTableColumn(nsui.NSTableColumn.create("c"));
            ownedTable.setDataSource(rowDs2);
            ownedTable.setDelegate(rowDs2);
            ownedTable.reloadData();
            nsui.NSTableRowView owned = nsui.NSTableRowView.wrap(ownedTable.rowViewAtRow(0, true).peer());
            TestKit.check(owned.numberOfColumns() == 1,
                    "a row view owned by the 1-column table reports 1 column (got "
                            + owned.numberOfColumns() + ")");
            TestKit.noThrow("viewAtColumn(0) on an owned row view is legal", () -> owned.viewAtColumn(0));
            TestKit.expectThrows("viewAtColumn(1) on a 1-column owned row view rejects",
                    IllegalArgumentException.class, () -> owned.viewAtColumn(1));
        } catch (Throwable t) {
            TestKit.check(false, "owned row-view block threw: " + t);
        }

        // --- 8ar. NSFindPanel.sharedFindPanel actually returns the panel (doc said always nil) ---
        try {
            nsui.NSFindPanel fp = nsui.NSFindPanel.sharedFindPanel();
            TestKit.check(fp != null, "NSFindPanel.sharedFindPanel() returns the process find panel");
            TestKit.check(fp.isKindOfClass("NSFindPanel"),
                    "the returned object is an NSFindPanel");
        } catch (Throwable t) {
            TestKit.check(false, "NSFindPanel block threw: " + t);
        }

        // --- 8as. array/dictionary mutators require a MUTABLE peer ---
        // NSArray.array() returns an immutable array and sending addObject: to it aborts
        // ("-[__NSArray0 addObject:]: unrecognized selector", verified exit 134); the same
        // class's mutableArray() is the mutable peer. Both halves are pinned.
        try {
            nsui.NSArray imm = nsui.NSArray.array();
            TestKit.expectThrows("addObject on an immutable NSArray rejects",
                    IllegalStateException.class, () -> imm.addObject(nsui.NSString.of("x")));
            TestKit.expectThrows("removeAllObjects on an immutable NSArray rejects",
                    IllegalStateException.class, () -> imm.removeAllObjects());
            nsui.NSArray mut = nsui.NSArray.mutableArray();
            TestKit.noThrow("addObject on mutableArray() works",
                    () -> mut.addObject(nsui.NSString.of("x")));
            TestKit.check(mut.count() == 1, "the mutable array holds one object");
            nsui.NSDictionary immD = nsui.NSDictionary.dictionary();
            TestKit.expectThrows("setObjectForKey on an immutable NSDictionary rejects",
                    IllegalStateException.class,
                    () -> immD.setObjectForKey(nsui.NSString.of("v"), nsui.NSString.of("k")));
            nsui.NSDictionary mutD = nsui.NSDictionary.mutableDictionary();
            TestKit.noThrow("setObjectForKey on mutableDictionary() works",
                    () -> mutD.setObjectForKey(nsui.NSString.of("v"), nsui.NSString.of("k")));
        } catch (Throwable t) {
            TestKit.check(false, "mutable-peer block threw: " + t);
        }

        // --- 8at. version-gated accessors are guarded (and still work where present) ---
        // These families were inconsistent: NSButton guarded its macOS-26 selectors while
        // NSSlider/NSTextField/NSGestureRecognizer/NSToolbarItem/NSView/NSScreen/NSColorPanel
        // sent them unconditionally (an abort on any older host). They now return a default /
        // no-op when respondsToSelector: is false; on THIS host the selectors exist, so the
        // assertions below also prove the guards do not short-circuit a live call.
        try {
            nsui.NSSlider vSlider = nsui.NSSlider.create(new NSRect(0, 0, 100, 24));
            TestKit.noThrow("NSSlider.tintProminence()/setTintProminence() are guarded but live",
                    () -> { vSlider.setTintProminence(1); vSlider.tintProminence(); });
            TestKit.noThrow("NSSlider.neutralValue()/setNeutralValue() are guarded but live",
                    () -> { vSlider.setNeutralValue(0.5); vSlider.neutralValue(); });
            nsui.NSTextField vField = nsui.NSTextField.create(new NSRect(0, 0, 100, 24));
            TestKit.noThrow("NSTextField.placeholderStrings() is guarded but live",
                    () -> vField.placeholderStrings());
            nsui.NSGestureRecognizer vGr = nsui.NSGestureRecognizer.create(null, "");
            if (vGr != null) {
                TestKit.noThrow("NSGestureRecognizer name round-trip is guarded but live",
                        () -> { vGr.setName("probe"); vGr.name(); vGr.modifierFlags(); });
            } else {
                TestKit.probe("gesture recognizer unavailable");
            }
            nsui.NSToolbarItem vItem = nsui.NSToolbarItem.create("probe.item");
            TestKit.noThrow("NSToolbarItem.style() is guarded but live", () -> vItem.style());
            NSView vView = NSView.create(new NSRect(0, 0, 10, 10), (v, d) -> {});
            TestKit.noThrow("NSView.prefersCompactControlSizeMetrics() is guarded but live",
                    () -> vView.prefersCompactControlSizeMetrics());
            TestKit.noThrow("NSScreen.cgDirectDisplayID() is guarded but live",
                    () -> nsui.NSScreen.mainScreen().cgDirectDisplayID());
            TestKit.noThrow("NSColorPanel.maximumLinearExposure() is guarded but live",
                    () -> nsui.NSColorPanel.sharedColorPanel().maximumLinearExposure());
        } catch (Throwable t) {
            TestKit.check(false, "version-gated accessor block threw: " + t);
        }

        // --- 8z. type-confusion guards on the raw collection-view setters ---
        // AppKit does NOT raise a type error here, it either asserts isKindOfClass: or
        // messages the object: setCollectionViewLayout: with a plain NSView raised
        // "Invalid parameter not satisfying: newCollectionViewLayout == nil ||
        // [newCollectionViewLayout isKindOfClass:[NSCollectionViewLayout class]]", and
        // setItemPrototype: with a plain NSView raised "-[NSView _setCollectionView:]:
        // unrecognized selector" — both uncatchable aborts.
        try {
            nsui.NSCollectionView tcv = nsui.NSCollectionView.create(new NSRect(0, 0, 200, 100));
            MemorySegment plainView = NSView.create(new NSRect(0, 0, 10, 10), (v, d) -> {}).peer();
            TestKit.expectThrows("setCollectionViewLayout(NSView peer) rejects",
                    IllegalArgumentException.class, () -> tcv.setCollectionViewLayout(plainView));
            MemorySegment flow = ObjC.msgSendId(
                    ObjC.msgSendId(ObjC.cls("NSCollectionViewFlowLayout"), ObjC.sel("alloc")), ObjC.sel("init"));
            TestKit.noThrow("setCollectionViewLayout(NSCollectionViewFlowLayout) is accepted",
                    () -> tcv.setCollectionViewLayout(flow));
            TestKit.noThrow("setCollectionViewLayout(null) is accepted (AppKit allows nil)",
                    () -> tcv.setCollectionViewLayout((MemorySegment) null));
            TestKit.expectThrows("setItemPrototype(NSView peer) rejects",
                    IllegalArgumentException.class, () -> tcv.setItemPrototype(plainView));
            TestKit.noThrow("setItemPrototype(NSCollectionViewItem) is accepted",
                    () -> tcv.setItemPrototype(nsui.NSCollectionViewItem.create()));
            TestKit.noThrow("setItemPrototype(null) is accepted",
                    () -> tcv.setItemPrototype((MemorySegment) null));
        } catch (Throwable t) {
            TestKit.check(false, "collection-view type-guard block threw: " + t);
        }

        // --- 8y. tab view CONTROLLER index: -1 allowed, count and above fatal ---
        // Measured: 0 and 1 accepted on a 2-item controller; 2 and 5 raise
        // NSInternalInconsistencyException and abort; -1 is accepted and is the only value a
        // 0-item controller accepts. (NSTabView.selectTabViewItemAtIndex: rejects -1 instead,
        // which is why the two guards differ.)
        try {
            nsui.NSTabViewController tvc = nsui.NSTabViewController.create();
            TestKit.expectThrows("setSelectedTabViewItemIndex(0) on a 0-item controller rejects",
                    IllegalArgumentException.class, () -> tvc.setSelectedTabViewItemIndex(0));
            TestKit.noThrow("setSelectedTabViewItemIndex(-1) on a 0-item controller is legal",
                    () -> tvc.setSelectedTabViewItemIndex(-1));
            for (int i = 0; i < 2; i++) {
                nsui.NSTabViewItem item = nsui.NSTabViewItem.create("tab" + i);
                nsui.NSViewController vc = nsui.NSViewController.create();
                vc.setView(NSView.create(new NSRect(0, 0, 50, 50), (v, d) -> {}));
                item.setViewController(vc);
                tvc.addTabViewItem(item);
            }
            TestKit.check(tvc.tabViewItems() != null && tvc.tabViewItems().count() == 2,
                    "tab view controller reports 2 items");
            TestKit.noThrow("setSelectedTabViewItemIndex(0) is legal", () -> tvc.setSelectedTabViewItemIndex(0));
            TestKit.noThrow("setSelectedTabViewItemIndex(1) is legal", () -> tvc.setSelectedTabViewItemIndex(1));
            TestKit.noThrow("setSelectedTabViewItemIndex(-1) clears", () -> tvc.setSelectedTabViewItemIndex(-1));
            TestKit.expectThrows("setSelectedTabViewItemIndex(2) rejects (== count)",
                    IllegalArgumentException.class, () -> tvc.setSelectedTabViewItemIndex(2));
            TestKit.expectThrows("setSelectedTabViewItemIndex(5) rejects",
                    IllegalArgumentException.class, () -> tvc.setSelectedTabViewItemIndex(5));
            TestKit.expectThrows("setSelectedTabViewItemIndex(-2) rejects",
                    IllegalArgumentException.class, () -> tvc.setSelectedTabViewItemIndex(-2));
        } catch (Throwable t) {
            TestKit.check(false, "tab-view-controller index block threw: " + t);
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
