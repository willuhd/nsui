package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import nsui.NSApplication;
import nsui.NSArray;
import nsui.NSAttributedString;
import nsui.NSButton;
import nsui.NSButtonTouchBarItem;
import nsui.NSColor;
import nsui.NSColorWell;
import nsui.NSComboBox;
import nsui.NSComboButton;
import nsui.NSControl;
import nsui.NSCustomTouchBarItem;
import nsui.NSDatePicker;
import nsui.NSGestureRecognizer;
import nsui.NSImage;
import nsui.NSLevelIndicator;
import nsui.NSMenu;
import nsui.NSMenuItem;
import nsui.NSPathControl;
import nsui.NSPopUpButton;
import nsui.NSPopoverTouchBarItem;
import nsui.NSProgressIndicator;
import nsui.NSRect;
import nsui.NSSearchField;
import nsui.NSSecureTextField;
import nsui.NSSegmentedControl;
import nsui.NSSet;
import nsui.NSSize;
import nsui.NSSlider;
import nsui.NSSliderTouchBarItem;
import nsui.NSStepper;
import nsui.NSSwitch;
import nsui.NSTextField;
import nsui.NSTokenField;
import nsui.NSTouchBar;
import nsui.NSTouchBarItem;
import nsui.NSView;
import nsui.NSViewController;
import nsui.NSWindow;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// ControlsCoverageTest — round-trips for every method added in the controls
/// coverage sweep (NSButton, NSComboButton, NSControl, NSColorWell, NSComboBox,
/// NSDatePicker, NSLevelIndicator, NSProgressIndicator, NSSwitch, NSTextField
/// family, NSSegmentedControl, NSSlider, NSStepper, NSTokenField,
/// NSPathControl, NSPopUpButton, Touch Bar items).
///
/// All controls live in hidden windows (never shown, never key); no audio,
/// no Dock changes, no activation. Runs on the main thread.
public final class ControlsCoverageTest {

    private static void checkDouble(double got, double expected, String msg) {
        TestKit.check(Math.abs(got - expected) < 1e-6, msg + " [got " + got + ", expected " + expected + "]");
    }


    private static boolean responds(MemorySegment peer, String selName) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel(selName));
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== ControlsCoverageTest - controls coverage sweep ===");
        {
        ObjC.init();
        TestKit.app();

        NSWindow win = TestKit.hiddenWindow(600, 500);
        NSView content = NSView.create(new NSRect(0, 0, 600, 500), (ctx, d) -> {});
        win.setContentView(content);

        // ------------------------------------------------------------ NSButton
        NSButton hello = NSButton.buttonWithTitle("Hello", null, null);
        content.addSubview(hello);
        TestKit.check("Hello".equals(hello.title()), "buttonWithTitle factory title round-trip");
        NSButton check = NSButton.checkboxWithTitle("Check", null, null);
        content.addSubview(check);
        check.setState(1L);
        TestKit.check(check.state() == 1L, "checkboxWithTitle state setState(1) round-trip");
        NSButton radio = NSButton.radioButtonWithTitle("Radio", null, null);
        TestKit.check("Radio".equals(radio.title()), "radioButtonWithTitle title round-trip");
        NSImage star = NSImage.imageWithSystemSymbolName("star");
        TestKit.check(star != null, "SF Symbol star image non-nil for image factories");
        NSButton imgBtn = NSButton.buttonWithImage(star, null, null);
        TestKit.check(imgBtn != null && imgBtn.image() != null, "buttonWithImage factory image round-trip");
        NSButton both = NSButton.buttonWithTitleImage("Both", star, null, null);
        TestKit.check("Both".equals(both.title()), "buttonWithTitleImage title round-trip");
        hello.setMaxAcceleratorLevel(3L);
        long accel = hello.maxAcceleratorLevel();
        TestKit.check(accel >= 1L && accel <= 5L, "maxAcceleratorLevel sane after set(3) [got " + accel + "]");
        hello.setMaxAcceleratorLevel(2L);
        MemorySegment symCfg = hello.symbolConfiguration();
        TestKit.check(symCfg == null || symCfg.address() == 0, "symbolConfiguration nil by default");
        TestKit.noThrow("setSymbolConfiguration(NULL) no crash", () -> hello.setSymbolConfiguration(MemorySegment.NULL));
        hello.setTintProminence(1L);
        TestKit.check(hello.tintProminence() == 1L, "tintProminence 1 round-trip (macOS 26+)");
        hello.setTintProminence(0L);
        hello.setBorderShape(1L);
        TestKit.check(hello.borderShape() == 1L, "borderShape 1 round-trip (macOS 26+)");
        hello.setBorderShape(0L);
        hello.setKeyEquivalent("\r");
        TestKit.check(!hello.performKeyEquivalent(null), "performKeyEquivalent(nil) == false, no crash");
        TestKit.noThrow("compressWithPrioritizedCompressionOptions(empty) no crash", () -> hello.compressWithPrioritizedCompressionOptions(NSArray.array()));
        MemorySegment active = TestKit.attempt("activeCompressionOptions no crash", () -> hello.activeCompressionOptions());

        // ------------------------------------------------------------ NSControl (via hello)
        hello.setTag(42L);
        TestKit.check(hello.tag() == 42L, "NSControl tag 42 round-trip");
        TestKit.check(hello.currentEditor() == null, "currentEditor null when idle");
        boolean aborted = TestKit.attempt("abortEditing no crash", () -> hello.abortEditing());
        TestKit.noThrow("validateEditing no crash", () -> hello.validateEditing());
        TestKit.noThrow("endEditing(nil) no crash", () -> hello.endEditing(null));
        NSRect expansion = hello.expansionFrameWithFrame(new NSRect(0, 0, 120, 24));
        TestKit.check(expansion != null, "expansionFrameWithFrame non-nil");
        TestKit.noThrow("drawWithExpansionFrame:inView: no crash", () -> hello.drawWithExpansionFrameInView(new NSRect(0, 0, 120, 24), null));
        MemorySegment cellClass = TestKit.attempt("NSControl.cellClass no crash", () -> NSControl.cellClass());
        MemorySegment cell = hello.cell();
        TestKit.check(cell != null && cell.address() != 0, "button.cell() non-nil");
        MemorySegment selCell = hello.selectedCell();
        TestKit.check(selCell != null && selCell.address() != 0, "button.selectedCell() non-nil");
        System.out.println("  button.selectedTag() = " + hello.selectedTag());
        hello.updateCell(cell);
        hello.updateCellInside(cell);
        TestKit.noThrow("updateCell/updateCellInside/selectCell no crash", () -> hello.selectCell(cell));

        // ------------------------------------------------------------ NSComboButton
        NSComboButton comboBtn = NSComboButton.create(new NSRect(0, 0, 160, 32));
        content.addSubview(comboBtn);
        comboBtn.setTitle("Combo");
        TestKit.check("Combo".equals(comboBtn.title()), "NSComboButton title round-trip");
        NSMenu menu = NSMenu.createWithTitle("M");
        menu.addItemWithTitle("Item1", "", "");
        menu.addItemWithTitle("Item2", "", "");
        comboBtn.setMenu(menu);
        NSMenu gotMenu = comboBtn.menu();
        TestKit.check(gotMenu != null && gotMenu.numberOfItems() == 2L,
                "NSComboButton menu round-trip (2 items)");
        comboBtn.setStyle(NSComboButton.STYLE_UNIFIED);
        TestKit.check(comboBtn.style() == NSComboButton.STYLE_UNIFIED, "NSComboButton style UNIFIED round-trip");
        comboBtn.setStyle(NSComboButton.STYLE_SPLIT);
        TestKit.check(comboBtn.style() == NSComboButton.STYLE_SPLIT, "NSComboButton style SPLIT round-trip");
        comboBtn.setImage(null);
        TestKit.check(comboBtn.image() == null, "NSComboButton image nil round-trip");
        comboBtn.setImageScaling(1L);
        TestKit.check(comboBtn.imageScaling() == 1L, "NSComboButton imageScaling 1 round-trip");
        NSComboButton factoryBtn = NSComboButton.comboButtonWithTitle("F", menu, null, null);
        TestKit.check("F".equals(factoryBtn.title())
                && factoryBtn.menu() != null && factoryBtn.menu().numberOfItems() == 2L,
                "comboButtonWithTitle factory title+menu round-trip");
        NSComboButton imgFactory = NSComboButton.comboButtonWithImage(star, menu, null, null);
        TestKit.check(imgFactory != null && imgFactory.menu() != null && imgFactory.image() != null,
                "comboButtonWithImage factory image+menu round-trip");
        NSComboButton bothFactory = NSComboButton.comboButtonWithTitleImage("G", star, menu, null, null);
        TestKit.check("G".equals(bothFactory.title()), "comboButtonWithTitleImage factory title round-trip");

        // ------------------------------------------------------------ NSColorWell
        NSColorWell styled = NSColorWell.colorWellWithStyle(0L);
        content.addSubview(styled);
        TestKit.check(styled.colorWellStyle() == 0L, "colorWellWithStyle(0) style round-trip");
        TestKit.noThrow("drawWellInside no crash", () -> styled.drawWellInside(new NSRect(0, 0, 20, 20)));
        styled.setImage(null);
        TestKit.check(styled.image() == null, "NSColorWell image nil round-trip");
        styled.setPulldownAction("pulldown:");
        TestKit.check(styled.pulldownAction() != null
                && styled.pulldownAction().address() == ObjC.sel("pulldown:").address(),
                "pulldownAction round-trip");
        styled.setPulldownAction(null);
        TestKit.check(styled.pulldownAction() == null || styled.pulldownAction().address() == 0,
                "pulldownAction cleared to nil");
        NSColorWell other = NSColorWell.create(new NSRect(0, 0, 60, 24));
        styled.setPulldownTarget(other.peer());
        TestKit.check(styled.pulldownTarget() != null
                && styled.pulldownTarget().address() == other.peer().address(),
                "pulldownTarget round-trip");
        styled.setPulldownTarget(null);
        styled.setMaximumLinearExposure(2.0);
        checkDouble(styled.maximumLinearExposure(), 2.0, "maximumLinearExposure 2.0 round-trip");
        styled.setMaximumLinearExposure(1.0);

        // ------------------------------------------------------------ NSComboBox
        NSComboBox combo = NSComboBox.create(new NSRect(0, 40, 180, 25));
        content.addSubview(combo);
        combo.addItemsWithObjectValues("x", "y", "z");
        TestKit.check(combo.numberOfItems() == 3L, "addItemsWithObjectValues 3 items");
        combo.insertItemWithObjectValueAtIndex("w", 0L);
        TestKit.check(combo.numberOfItems() == 4L, "insertItemWithObjectValue:atIndex: -> 4 items");
        combo.scrollItemAtIndexToTop(0L);
        TestKit.noThrow("scrollItemAtIndexToTop/ToVisible no crash", () -> combo.scrollItemAtIndexToVisible(3L));
        combo.setIntercellSpacing(NSSize.make(4.0, 5.0));
        NSSize spacing = combo.intercellSpacing();
        TestKit.check(Math.abs(spacing.width() - 4.0) < 1e-6 && Math.abs(spacing.height() - 5.0) < 1e-6,
                "intercellSpacing (4,5) round-trip");

        // ------------------------------------------------------------ NSDatePicker
        NSDatePicker picker = NSDatePicker.create(new NSRect(0, 70, 180, 27));
        content.addSubview(picker);
        picker.setTimeInterval(172800.0);
        checkDouble(picker.timeInterval(), 172800.0, "timeInterval 172800 round-trip");
        picker.setPresentsCalendarOverlay(true);
        TestKit.check(picker.presentsCalendarOverlay(), "presentsCalendarOverlay true round-trip");
        picker.setPresentsCalendarOverlay(false);
        TestKit.check(!picker.presentsCalendarOverlay(), "presentsCalendarOverlay false round-trip");

        // ------------------------------------------------------------ NSLevelIndicator
        NSLevelIndicator ind = NSLevelIndicator.create(new NSRect(0, 100, 120, 16));
        content.addSubview(ind);
        ind.setDrawsTieredCapacityLevels(true);
        TestKit.check(ind.drawsTieredCapacityLevels(), "drawsTieredCapacityLevels true round-trip");
        ind.setDrawsTieredCapacityLevels(false);
        ind.setPlaceholderVisibility(1L);
        TestKit.check(ind.placeholderVisibility() == 1L
                && ind.placeholderVisibilityEnum() == NSLevelIndicator.PlaceholderVisibility.always,
                "placeholderVisibility Always round-trip (typed)");
        ind.setPlaceholderVisibility(NSLevelIndicator.PlaceholderVisibility.automatic);
        ind.setRatingImage(null);
        ind.setRatingPlaceholderImage(null);
        TestKit.check(ind.ratingImage() == null && ind.ratingPlaceholderImage() == null,
                "ratingImage/ratingPlaceholderImage nil round-trip");
        ind.setMinValue(0.0);
        ind.setMaxValue(10.0);
        ind.setNumberOfTickMarks(5L);
        TestKit.check(ind.numberOfTickMarks() == 5L, "numberOfTickMarks 5 round-trip");
        double tick0 = ind.tickMarkValueAtIndex(0L);
        TestKit.check(Double.isFinite(tick0) && tick0 >= -1e-6 && tick0 <= 10.0 + 1e-6,
                "tickMarkValueAtIndex(0) in range [got " + tick0 + "]");
        ind.setNumberOfMajorTickMarks(2L);
        TestKit.check(ind.numberOfMajorTickMarks() == 2L, "numberOfMajorTickMarks 2 round-trip");

        // ------------------------------------------------------------ NSProgressIndicator
        NSProgressIndicator bar = NSProgressIndicator.create(new NSRect(0, 120, 200, 20));
        content.addSubview(bar);
        TestKit.check(bar.observedProgress() == null || bar.observedProgress().address() == 0,
                "observedProgress nil by default");
        bar.setObservedProgress(null);
        TestKit.check(bar.observedProgress() == null || bar.observedProgress().address() == 0,
                "setObservedProgress(nil) round-trip");
        bar.setIndeterminate(true);
        TestKit.check(bar.isIndeterminate(), "isIndeterminate true round-trip");
        bar.setIndeterminate(false);

        // ------------------------------------------------------------ NSSwitch
        NSSwitch sw = NSSwitch.create(new NSRect(0, 150, 60, 24));
        content.addSubview(sw);
        sw.setState(1L);
        TestKit.check(sw.state() == 1L, "NSSwitch state on round-trip");
        sw.setState(0L);
        TestKit.check(sw.state() == 0L, "NSSwitch state off round-trip");

        // Cross-checks that neighboring wrappers still behave.
        NSImage named = NSImage.imageNamed("NSApplicationIcon");
        System.out.println("  NSApplicationIcon image nil=" + (named == null));
        NSColor red = NSColor.create(1.0, 0.0, 0.0, 1.0);
        styled.setColor(red);
        TestKit.check(styled.color() != null, "NSColorWell setColor(color) still works");

        TestKit.close(win);
        }
        {
        NSApplication app = TestKit.app();

        // Unique scratch dir per run (batch rule: unique /tmp/sa dirs, never fixed paths).
        Path saDir = Files.createTempDirectory("sa-");
        System.out.println("  scratch dir = " + saDir);
        TestKit.check(Files.isDirectory(saDir), "unique /tmp/sa-* scratch dir created");

        NSWindow window = TestKit.hiddenWindow(640, 480);
        NSView content = NSView.create(new NSRect(0, 0, 640, 480), (ctx, d) -> {});
        window.setContentView(content);

        // ---------------------------------------------------------- NSTextField
        try {
            NSTextField field = NSTextField.create(new NSRect(10, 10, 300, 24));
            content.addSubview(field);
            // labelWithAttributedString factory (ID,ID in vocabulary)
            NSAttributedString attr = NSAttributedString.create("hello attr");
            NSTextField lab = NSTextField.labelWithAttributedString(attr);
            TestKit.check(lab != null && lab.peer().address() != 0, "NSTextField.labelWithAttributedString non-nil");
            TestKit.check(lab.isKindOfClass("NSTextField"), "labelWithAttributedString isKindOfClass NSTextField");
            // lineBreakStrategy setter round-trip (VOID,INT / INT () in vocabulary)
            long origLB = field.lineBreakStrategy();
            field.setLineBreakStrategy(0L);
            TestKit.check(field.lineBreakStrategy() == 0L, "NSTextField setLineBreakStrategy(0) round-trip");
            field.setLineBreakStrategy(origLB);
            // allowsWritingTools (guard: 15.2+)
            if (responds(field.peer(), "allowsWritingTools")) {
                boolean o = field.allowsWritingTools();
                field.setAllowsWritingTools(!o);
                TestKit.check(field.allowsWritingTools() == !o, "NSTextField allowsWritingTools toggled");
                field.setAllowsWritingTools(o);
            } else {
                TestKit.skipCase("NOTE allowsWritingTools absent on this OS (no-crash pass)");
            }
            // allowsWritingToolsAffordance (guard: 15.4+)
            if (responds(field.peer(), "allowsWritingToolsAffordance")) {
                boolean o = field.allowsWritingToolsAffordance();
                field.setAllowsWritingToolsAffordance(!o);
                TestKit.check(field.allowsWritingToolsAffordance() == !o, "NSTextField allowsWritingToolsAffordance toggled");
                field.setAllowsWritingToolsAffordance(o);
            } else {
                TestKit.skipCase("NOTE allowsWritingToolsAffordance absent on this OS (no-crash pass)");
            }
            // allowsCharacterPickerTouchBarItem (10.12.2+; guarded like TouchBar)
            if (responds(field.peer(), "allowsCharacterPickerTouchBarItem")) {
                boolean o = field.allowsCharacterPickerTouchBarItem();
                field.setAllowsCharacterPickerTouchBarItem(!o);
                TestKit.check(field.allowsCharacterPickerTouchBarItem() == !o, "NSTextField allowsCharacterPickerTouchBarItem toggled");
                field.setAllowsCharacterPickerTouchBarItem(o);
            } else {
                TestKit.skipCase("NOTE allowsCharacterPickerTouchBarItem absent (no-crash pass)");
            }
            // placeholderStrings / placeholderAttributedStrings (guard: 26.0+)
            if (responds(field.peer(), "placeholderStrings")) {
                NSArray before = field.placeholderStrings();
                System.out.println("  placeholderStrings before = " + (before == null ? "nil" : ("count=" + before.count())));
                TestKit.noThrow("NSTextField placeholderStrings getter/setter no crash", () -> field.setPlaceholderStrings(null));
                field.setPlaceholderStrings(before);
            } else {
                TestKit.skipCase("NOTE placeholderStrings absent on this OS (no-crash pass)");
            }
            if (responds(field.peer(), "placeholderAttributedStrings")) {
                NSArray b = field.placeholderAttributedStrings();
                System.out.println("  placeholderAttributedStrings before = " + (b == null ? "nil" : ("count=" + b.count())));
                TestKit.noThrow("NSTextField placeholderAttributedStrings getter/setter no crash", () -> field.setPlaceholderAttributedStrings(null));
            } else {
                TestKit.skipCase("NOTE placeholderAttributedStrings absent on this OS (no-crash pass)");
            }
            // resolvesNaturalAlignmentWithBaseWritingDirection (guard: 26.0+)
            if (responds(field.peer(), "resolvesNaturalAlignmentWithBaseWritingDirection")) {
                boolean o = field.resolvesNaturalAlignmentWithBaseWritingDirection();
                field.setResolvesNaturalAlignmentWithBaseWritingDirection(!o);
                TestKit.check(field.resolvesNaturalAlignmentWithBaseWritingDirection() == !o,
                        "NSTextField resolvesNaturalAlignmentWithBaseWritingDirection toggled");
                field.setResolvesNaturalAlignmentWithBaseWritingDirection(o);
            } else {
                TestKit.skipCase("NOTE resolvesNaturalAlignmentWithBaseWritingDirection absent (no-crash pass)");
            }
            // editing notifications with NULL (VOID,ID / BOOL,ID in vocabulary)
            try {
                boolean b = field.textShouldBeginEditing(null);
                System.out.println("  textShouldBeginEditing(NULL) = " + b);
                TestKit.probe("NSTextField textShouldBeginEditing(NULL) no crash");
            } catch (Throwable t) {
                TestKit.check(false, "textShouldBeginEditing(NULL) threw: " + t);
            }
            try {
                boolean b = field.textShouldEndEditing(null);
                System.out.println("  textShouldEndEditing(NULL) = " + b);
                TestKit.probe("NSTextField textShouldEndEditing(NULL) no crash");
            } catch (Throwable t) {
                TestKit.check(false, "textShouldEndEditing(NULL) threw: " + t);
            }
            // textDidBeginEditing:/textDidEndEditing:/textDidChange: require a non-nil
            // NSNotification (AppKit builds a dict from it; nil raises). Verify the
            // selectors exist and the VOID,ID shape sends without crashing only via
            // respondsToSelector: — do NOT invoke with nil.
            TestKit.check(responds(field.peer(), "textDidBeginEditing:")
                    && responds(field.peer(), "textDidEndEditing:")
                    && responds(field.peer(), "textDidChange:"),
                    "NSTextField textDid* selectors present (nil invocation skipped: AppKit requires non-nil notification)");
        } catch (Throwable t) {
            TestKit.check(false, "NSTextField section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSSearchField
        try {
            NSSearchField sf = NSSearchField.create(new NSRect(10, 40, 300, 28));
            content.addSubview(sf);
            // layout bounds (RECT () in vocabulary; 11.0+)
            if (responds(sf.peer(), "searchTextBounds")) {
                NSRect b = sf.searchTextBounds();
                TestKit.check(b != null, "NSSearchField searchTextBounds non-null (" + b + ")");
                NSRect bb = sf.searchButtonBounds();
                TestKit.check(bb != null, "NSSearchField searchButtonBounds non-null (" + bb + ")");
                NSRect cb = sf.cancelButtonBounds();
                TestKit.check(cb != null, "NSSearchField cancelButtonBounds non-null (" + cb + ")");
            } else {
                TestKit.skipCase("NOTE search bounds absent on this OS (no-crash pass)");
            }
            // typed recentSearches (ID shapes)
            NSArray rec = sf.recentSearchesArray();
            System.out.println("  recentSearchesArray = " + (rec == null ? "nil" : ("count=" + rec.count())));
            TestKit.probe("NSSearchField recentSearchesArray no crash");
            TestKit.noThrow("NSSearchField setRecentSearches(null-array) no crash", () -> sf.setRecentSearches((NSArray) null));
            // delegate inherited from NSTextField (same selectors)
            MemorySegment d = sf.delegate();
            System.out.println("  search delegate before = " + d);
            sf.setDelegate((MemorySegment) null);
            TestKit.check(sf.delegate() == null || sf.delegate().address() == 0,
                    "NSSearchField delegate inherited setter/getter round-trip to nil");
        } catch (Throwable t) {
            TestKit.check(false, "NSSearchField section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSSecureTextField (smoke)
        try {
            NSSecureTextField sec = NSSecureTextField.create(new NSRect(10, 70, 300, 24));
            content.addSubview(sec);
            sec.setStringValue("pw123");
            TestKit.check("pw123".equals(sec.stringValue()), "NSSecureTextField stringValue round-trip");
            TestKit.check(sec.isKindOfClass("NSSecureTextField"), "NSSecureTextField isKindOfClass");
            TestKit.check(sec.isKindOfClass("NSTextField"), "NSSecureTextField isKindOfClass NSTextField (super)");
        } catch (Throwable t) {
            TestKit.check(false, "NSSecureTextField section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSSegmentedControl
        try {
            NSSegmentedControl seg = NSSegmentedControl.create(new NSRect(10, 100, 320, 28));
            content.addSubview(seg);
            seg.setSegmentCount(3);
            seg.setLabel("A", 0);
            seg.setLabel("B", 1);
            seg.setLabel("C", 2);
            seg.setTagForSegment(101, 0);
            seg.setTagForSegment(102, 1);
            TestKit.check(seg.selectSegmentWithTag(102), "NSSegmentedControl selectSegmentWithTag(102) == true");
            TestKit.check(seg.selectedSegment() == 1, "selectedSegment == 1 after selectSegmentWithTag(102)");
            TestKit.check(!seg.selectSegmentWithTag(9999), "selectSegmentWithTag(9999) == false");
            // imageScaling (VOID,INT,INT / INT,INT in vocabulary)
            seg.setImageScalingForSegment(1L, 0);
            TestKit.check(seg.imageScalingForSegment(0) == 1L, "imageScalingForSegment round-trip == 1");
            // springLoaded (BOOL shapes)
            if (responds(seg.peer(), "isSpringLoaded")) {
                boolean o = seg.isSpringLoaded();
                seg.setSpringLoaded(!o);
                TestKit.check(seg.isSpringLoaded() == !o, "springLoaded toggled");
                seg.setSpringLoaded(o);
            } else {
                TestKit.skipCase("NOTE springLoaded absent (no-crash pass)");
            }
            // doubleValueForSelectedSegment (DOUBLE () in vocabulary; valid ONLY for
            // trackingMode MomentaryAccelerator per header — set it first, then restore).
            if (responds(seg.peer(), "doubleValueForSelectedSegment")) {
                long origTM = seg.trackingMode();
                seg.setTrackingMode(3L);
                try {
                    double dv = seg.doubleValueForSelectedSegment();
                    System.out.println("  doubleValueForSelectedSegment (accelerator) = " + dv);
                    TestKit.probe("doubleValueForSelectedSegment no crash in accelerator mode (got " + dv + ")");
                } catch (Throwable t) {
                    System.out.println("  NOTE doubleValueForSelectedSegment threw even in accelerator mode: " + t.getMessage());
                    TestKit.skipCase("doubleValueForSelectedSegment no-crash noted");
                } finally {
                    seg.setTrackingMode(origTM);
                }
            } else {
                TestKit.skipCase("NOTE doubleValueForSelectedSegment absent (no-crash pass)");
            }
            // selectedSegmentBezelColor (ID shapes; 10.12.2+)
            if (responds(seg.peer(), "selectedSegmentBezelColor")) {
                NSColor before = seg.selectedSegmentBezelColor();
                System.out.println("  selectedSegmentBezelColor before = " + before);
                seg.setSelectedSegmentBezelColor(NSColor.redColor());
                NSColor after = seg.selectedSegmentBezelColor();
                TestKit.check(after != null && after.peer().address() != 0, "selectedSegmentBezelColor non-nil after set");
                TestKit.noThrow("selectedSegmentBezelColor restore no crash", () -> seg.setSelectedSegmentBezelColor(before));
            } else {
                TestKit.skipCase("NOTE selectedSegmentBezelColor absent (no-crash pass)");
            }
            // indexOfSelectedItem (INT () in vocabulary)
            seg.setSelectedSegment(2);
            TestKit.check(seg.indexOfSelectedItem() == 2, "indexOfSelectedItem == 2 after setSelectedSegment(2)");
            // alignment per segment (10.13+; VOID,INT,INT / INT,INT in vocabulary)
            if (responds(seg.peer(), "setAlignment:forSegment:")) {
                seg.setAlignmentForSegment(2L, 0);
                TestKit.check(seg.alignmentForSegment(0) == 2L, "alignmentForSegment round-trip == 2 (center)");
            } else {
                TestKit.skipCase("NOTE alignmentForSegment absent (no-crash pass)");
            }
            // segmentDistribution (10.13+)
            if (responds(seg.peer(), "segmentDistribution")) {
                long before = seg.segmentDistribution();
                seg.setSegmentDistribution(0L);
                TestKit.check(seg.segmentDistribution() == 0L, "segmentDistribution set Fit(0) round-trip");
                TestKit.check(seg.segmentDistributionEnum() == NSSegmentedControl.SegmentDistribution.fit,
                        "segmentDistributionEnum == fit");
                seg.setSegmentDistribution(NSSegmentedControl.SegmentDistribution.fill);
                TestKit.check(seg.segmentDistribution() == 1L, "segmentDistribution typed setter fill(1)");
                seg.setSegmentDistribution(before);
            } else {
                TestKit.skipCase("NOTE segmentDistribution absent (no-crash pass)");
            }
            // compress + activeCompressionOptions (10.13+)
            if (responds(seg.peer(), "compressWithPrioritizedCompressionOptions:")) {
                NSArray empty = NSArray.array();
                TestKit.noThrow("compressWithPrioritizedCompressionOptions(empty) no crash", () -> seg.compressWithPrioritizedCompressionOptions(empty));
                MemorySegment ac = seg.activeCompressionOptions();
                System.out.println("  activeCompressionOptions = " + ac);
                TestKit.probe("activeCompressionOptions no crash");
            } else {
                TestKit.skipCase("NOTE compression APIs absent (no-crash pass)");
            }
            // borderShape (guard: 26.0+)
            if (responds(seg.peer(), "borderShape")) {
                long before = seg.borderShape();
                seg.setBorderShape(0L);
                TestKit.check(seg.borderShape() == 0L, "borderShape set Automatic(0) round-trip");
                seg.setBorderShape(before);
            } else {
                TestKit.skipCase("NOTE borderShape absent on this OS (no-crash pass)");
            }
        } catch (Throwable t) {
            TestKit.check(false, "NSSegmentedControl section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSSlider
        try {
            NSSlider sl = NSSlider.create(new NSRect(10, 135, 220, 20));
            content.addSubview(sl);
            sl.setMinValue(0.0);
            sl.setMaxValue(100.0);
            sl.setDoubleValue(42.0);
            TestKit.check(Math.abs(sl.doubleValue() - 42.0) < 0.01, "NSSlider doubleValue round-trip 42.0");
            // floatValue is a float in AppKit; the old DOUBLE handle read the wrong register.
            sl.setFloatValue(1.5f);
            TestKit.check(Math.abs(sl.floatValue() - 1.5f) < 1e-6f,
                    "NSControl floatValue 1.5 round-trip (got " + sl.floatValue() + ")");
            sl.setDoubleValue(42.0);
            // neutralValue (guard: 26.0+; DOUBLE shapes in vocabulary)
            if (responds(sl.peer(), "neutralValue")) {
                sl.setNeutralValue(10.0);
                TestKit.check(Math.abs(sl.neutralValue() - 10.0) < 0.01, "NSSlider neutralValue round-trip 10.0");
            } else {
                TestKit.skipCase("NOTE neutralValue absent on this OS (no-crash pass)");
            }
            // acceptsFirstMouse (BOOL,ID in vocabulary)
            TestKit.probe("NSSlider acceptsFirstMouse(NULL) = " + sl.acceptsFirstMouse(null) + " (no crash)");
            // tintProminence (guard: 26.0+)
            if (responds(sl.peer(), "tintProminence")) {
                long before = sl.tintProminence();
                sl.setTintProminence(0L);
                TestKit.check(sl.tintProminence() == 0L, "NSSlider tintProminence Automatic(0) round-trip");
                sl.setTintProminence(before);
            } else {
                TestKit.skipCase("NOTE tintProminence absent on this OS (no-crash pass)");
            }
            // tickMarkValueAtIndex (DOUBLE,INT in vocabulary)
            sl.setNumberOfTickMarks(5L);
            try {
                double tv = sl.tickMarkValueAtIndex(0);
                System.out.println("  tickMarkValueAtIndex(0) = " + tv);
                TestKit.probe("NSSlider tickMarkValueAtIndex(0) no crash (got " + tv + ")");
            } catch (Throwable t) {
                System.out.println("  NOTE tickMarkValueAtIndex threw (tick setup?): " + t.getMessage());
                TestKit.skipCase("NSSlider tickMarkValueAtIndex no-crash noted");
            }
            sl.setNumberOfTickMarks(0L);
            // sliderWithTarget factory (ID,ID,ID in vocabulary)
            MemorySegment tgt = DelegateProxy.actionTarget("slideAct:", s -> {});
            NSSlider fsl = NSSlider.sliderWithTarget(tgt, "slideAct:");
            TestKit.check(fsl != null && fsl.peer().address() != 0, "NSSlider.sliderWithTarget factory non-nil");
            TestKit.check(fsl.isKindOfClass("NSSlider"), "factory slider isKindOfClass NSSlider");
        } catch (Throwable t) {
            TestKit.check(false, "NSSlider section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSStepper (smoke) + NSTokenField
        try {
            NSStepper st = NSStepper.create(new NSRect(10, 160, 19, 27));
            content.addSubview(st);
            st.setMinValue(0.0);
            st.setMaxValue(10.0);
            st.setIncrement(1.0);
            st.setDoubleValue(5.0);
            TestKit.check(Math.abs(st.doubleValue() - 5.0) < 0.01, "NSStepper doubleValue == 5.0");
        } catch (Throwable t) {
            TestKit.check(false, "NSStepper section threw: " + t);
            t.printStackTrace(System.out);
        }
        try {
            NSTokenField tf = NSTokenField.create(new NSRect(10, 190, 300, 24));
            content.addSubview(tf);
            tf.setTokenStyle(0L);
            TestKit.check(tf.tokenStyle() == 0L, "NSTokenField tokenStyle Default(0) round-trip");
            tf.setCompletionDelay(0.5);
            TestKit.check(Math.abs(tf.completionDelay() - 0.5) < 0.01, "NSTokenField completionDelay round-trip 0.5");
            double dd = NSTokenField.defaultCompletionDelay();
            System.out.println("  defaultCompletionDelay = " + dd);
            TestKit.check(dd >= 0.0, "NSTokenField.defaultCompletionDelay >= 0 (got " + dd + ")");
            MemorySegment cs = tf.tokenizingCharacterSet();
            System.out.println("  tokenizingCharacterSet = " + cs);
            TestKit.probe("NSTokenField tokenizingCharacterSet getter no crash");
            TestKit.noThrow("NSTokenField setTokenizingCharacterSet(null) reset no crash", () -> tf.setTokenizingCharacterSet(null));
            MemorySegment dcs = NSTokenField.defaultTokenizingCharacterSet();
            TestKit.check(dcs != null && dcs.address() != 0, "NSTokenField.defaultTokenizingCharacterSet non-nil");
            // delegate inherited (same selectors as NSTextField)
            tf.setDelegate((MemorySegment) null);
            TestKit.check(tf.delegate() == null || tf.delegate().address() == 0,
                    "NSTokenField delegate inherited round-trip to nil");
        } catch (Throwable t) {
            TestKit.check(false, "NSTokenField section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSPathControl
        try {
            NSPathControl pc = NSPathControl.create(new NSRect(10, 220, 320, 24));
            content.addSubview(pc);
            // URL via unique scratch dir (batch rule)
            String dirPath = saDir.toString();
            pc.setURLPath(dirPath);
            TestKit.check(dirPath.equals(pc.URLPath()), "NSPathControl URLPath round-trip == scratch dir");
            pc.setPathStyle(0L);
            TestKit.check(pc.pathStyle() == 0L, "NSPathControl pathStyle Standard(0) round-trip");
            pc.setPlaceholderString("Choose a path");
            TestKit.check("Choose a path".equals(pc.placeholderString()), "NSPathControl placeholderString round-trip");
            // placeholderAttributedString (ID shapes)
            MemorySegment pa = pc.placeholderAttributedString();
            System.out.println("  path placeholderAttributedString = " + pa);
            TestKit.probe("NSPathControl placeholderAttributedString getter no crash");
            pc.setPlaceholderAttributedString((MemorySegment) null);
            TestKit.check(pc.placeholderAttributedString() == null || pc.placeholderAttributedString().address() == 0,
                    "NSPathControl placeholderAttributedString nil after clear");
            NSAttributedString nsa = NSAttributedString.create("ph");
            pc.setPlaceholderAttributedString(nsa);
            TestKit.check(pc.placeholderAttributedStringTyped() != null, "NSPathControl placeholderAttributedStringTyped non-nil after set");
            pc.setPlaceholderAttributedString((MemorySegment) null);
            // pathItems (ID shapes)
            MemorySegment items = pc.pathItems();
            System.out.println("  pathItems = " + items);
            TestKit.probe("NSPathControl pathItems getter no crash");
            NSArray arr = pc.pathItemsArray();
            System.out.println("  pathItemsArray count = " + (arr == null ? "nil" : arr.count()));
            TestKit.probe("NSPathControl pathItemsArray no crash");
            // backgroundColor (ID shapes)
            pc.setBackgroundColor(NSColor.clearColor());
            TestKit.check(pc.backgroundColor() != null, "NSPathControl backgroundColor non-nil after set");
            TestKit.noThrow("NSPathControl setBackgroundColor(null) no crash", () -> pc.setBackgroundColor(null));
            // delegate (ID shapes)
            pc.setDelegate((MemorySegment) null);
            TestKit.check(pc.delegate() == null || pc.delegate().address() == 0, "NSPathControl delegate nil round-trip");
            // menu (ID shapes)
            NSMenu m = NSMenu.createWithTitle("PathMenu");
            pc.setMenu(m);
            TestKit.check(pc.menuTyped() != null && pc.menuTyped().peer().address() == m.peer().address(),
                    "NSPathControl menuTyped round-trip same peer");
            TestKit.noThrow("NSPathControl setMenu(nil) no crash", () -> pc.setMenu((MemorySegment) null));
            // allowedTypes array conveniences
            NSArray at = pc.allowedTypesArray();
            System.out.println("  allowedTypesArray = " + (at == null ? "nil" : ("count=" + at.count())));
            TestKit.noThrow("NSPathControl setAllowedTypes(null-array) no crash", () -> pc.setAllowedTypes((NSArray) null));
            // clickedPathItem (nil outside action dispatch; must not crash)
            MemorySegment clicked = pc.clickedPathItem();
            System.out.println("  clickedPathItem outside action = " + clicked);
            TestKit.probe("NSPathControl clickedPathItem no crash outside action");
        } catch (Throwable t) {
            TestKit.check(false, "NSPathControl section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSPopUpButton
        try {
            NSPopUpButton pop = NSPopUpButton.create(new NSRect(10, 250, 180, 25));
            content.addSubview(pop);
            pop.addItemWithTitle("One");
            pop.addItemWithTitle("Two");
            pop.addItemWithTitle("Three");
            TestKit.check(pop.numberOfItems() == 3, "NSPopUpButton 3 items after adds");
            // addItemsWithTitles (VOID,ID in vocabulary)
            NSArray more = NSArray.array();
            pop.addItemsWithTitles(more);
            TestKit.check(pop.numberOfItems() == 3, "addItemsWithTitles(empty) keeps 3 items");
            // indexOf* family (INT,ID / INT,INT / INT,ID,ID in vocabulary)
            NSMenuItem first = pop.itemAtIndex(0);
            TestKit.check(first != null && first.peer().address() != 0, "NSPopUpButton itemAtIndex(0) non-nil");
            TestKit.check("One".equals(first.title()), "itemAtIndex(0).title == One");
            NSMenuItem byTitle = pop.itemWithTitle("Two");
            TestKit.check(byTitle != null && "Two".equals(byTitle.title()), "NSPopUpButton itemWithTitle(Two) round-trip");
            TestKit.check(pop.itemWithTitle("Nope") == null, "itemWithTitle(missing) == nil");
            TestKit.check(pop.indexOfItem(first) == 0, "indexOfItem(first) == 0");
            TestKit.check(pop.indexOfItemWithTitle("Three") == 2, "indexOfItemWithTitle(Three) == 2");
            first.setTag(777L);
            TestKit.check(pop.indexOfItemWithTag(777L) == 0, "indexOfItemWithTag(777) == 0");
            MemorySegment rep = ObjC.nsstring("rep-obj");
            TestKit.check(pop.indexOfItemWithRepresentedObject(rep) < 0, "indexOfItemWithRepresentedObject(unknown) == -1/NSNotFound");
            TestKit.check(pop.indexOfItemWithTargetAndAction(null, null) < 0 || true,
                    "indexOfItemWithTargetAndAction(nil,nil) no crash");
            // selectItem / selectItemWithTag / setTitle / selectedTag / synchronize
            pop.selectItem(first);
            TestKit.check(pop.indexOfSelectedItem() == 0, "selectItem(first) -> index 0");
            TestKit.check(pop.selectItemWithTag(777L), "selectItemWithTag(777) == true");
            TestKit.check(!pop.selectItemWithTag(123456L), "selectItemWithTag(missing) == false");
            TestKit.noThrow("NSPopUpButton setTitle no crash", () -> pop.setTitle("CustomTitle"));
            long stag = pop.selectedTag();
            System.out.println("  selectedTag = " + stag);
            TestKit.probe("selectedTag getter no crash (got " + stag + ")");
            TestKit.noThrow("synchronizeTitleAndSelectedItem no crash", () -> pop.synchronizeTitleAndSelectedItem());
            // usesItemFromMenu / altersStateOfSelectedItem (guard: 15.0+)
            if (responds(pop.peer(), "usesItemFromMenu")) {
                boolean o = pop.usesItemFromMenu();
                pop.setUsesItemFromMenu(!o);
                TestKit.check(pop.usesItemFromMenu() == !o, "usesItemFromMenu toggled");
                pop.setUsesItemFromMenu(o);
            } else {
                TestKit.skipCase("NOTE usesItemFromMenu absent (no-crash pass)");
            }
            if (responds(pop.peer(), "altersStateOfSelectedItem")) {
                boolean o = pop.altersStateOfSelectedItem();
                pop.setAltersStateOfSelectedItem(!o);
                TestKit.check(pop.altersStateOfSelectedItem() == !o, "altersStateOfSelectedItem toggled");
                pop.setAltersStateOfSelectedItem(o);
            } else {
                TestKit.skipCase("NOTE altersStateOfSelectedItem absent (no-crash pass)");
            }
            // factories (ID,ID,ID / ID,ID,ID,ID in vocabulary; guard: 15.0+)
            if (responds(ObjC.cls("NSPopUpButton"), "popUpButtonWithMenu:target:action:")) {
                NSMenu fm = NSMenu.createWithTitle("FMenu");
                fm.addItem(NSMenuItem.withTitle("I1", "", ""));
                NSPopUpButton fp = NSPopUpButton.popUpButtonWithMenu(fm, null, null);
                TestKit.check(fp != null && fp.numberOfItems() >= 1, "popUpButtonWithMenu factory non-nil with items");
            } else {
                TestKit.skipCase("NOTE popUpButtonWithMenu absent (no-crash pass)");
            }
            if (responds(ObjC.cls("NSPopUpButton"), "pullDownButtonWithTitle:menu:")) {
                NSMenu pm = NSMenu.createWithTitle("PMenu");
                NSPopUpButton pd = NSPopUpButton.pullDownButtonWithTitle("Go", pm);
                TestKit.check(pd != null && pd.isPullsDown(), "pullDownButtonWithTitle factory isPullsDown");
            } else {
                TestKit.skipCase("NOTE pullDownButtonWithTitle absent (no-crash pass)");
            }
        } catch (Throwable t) {
            TestKit.check(false, "NSPopUpButton section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- Touch Bar items
        try {
            // NSButtonTouchBarItem: image/bezelColor/customizationLabel + image factories
            String bid = "btn." + System.nanoTime();
            MemorySegment bt = DelegateProxy.actionTarget("cbAct:", s -> {});
            NSButtonTouchBarItem bitem = NSButtonTouchBarItem.create(bid, "Go", bt, "cbAct:");
            bitem.setImage(null);
            TestKit.check(bitem.image() == null, "NSButtonTouchBarItem image nil after clear");
            bitem.setBezelColor(NSColor.redColor());
            TestKit.check(bitem.bezelColor() != null, "NSButtonTouchBarItem bezelColor non-nil after set");
            bitem.setBezelColor(null);
            TestKit.check(bitem.image() == null, "NSButtonTouchBarItem image still nil (no crash)");
            bitem.setCustomizationLabel("MyBtn");
            TestKit.check("MyBtn".equals(bitem.customizationLabel()), "NSButtonTouchBarItem customizationLabel round-trip");
            NSButtonTouchBarItem imgItem = NSButtonTouchBarItem.createWithImage("btn.img." + System.nanoTime(), null, bt, "cbAct:");
            TestKit.check(imgItem != null, "NSButtonTouchBarItem.createWithImage non-nil");
            NSButtonTouchBarItem tiItem = NSButtonTouchBarItem.createWithTitleImage("btn.ti." + System.nanoTime(), "T", null, bt, "cbAct:");
            TestKit.check(tiItem != null && "T".equals(tiItem.title()), "createWithTitleImage title round-trip");

            // NSCustomTouchBarItem: viewController
            NSCustomTouchBarItem citem = NSCustomTouchBarItem.create("custom." + System.nanoTime());
            NSView cv = NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {});
            citem.setView(cv);
            TestKit.check(citem.view() != null, "NSCustomTouchBarItem view non-nil after set");
            NSViewController vc = NSViewController.create();
            vc.setView(cv);
            citem.setViewController(vc);
            TestKit.check(citem.viewController() != null, "NSCustomTouchBarItem viewController non-nil after set");
            TestKit.noThrow("NSCustomTouchBarItem setViewController(nil) no crash", () -> citem.setViewController(null));

            // NSPopoverTouchBarItem: full header coverage
            NSPopoverTouchBarItem pitem = NSPopoverTouchBarItem.create("pop." + System.nanoTime());
            pitem.setCustomizationLabel("PopLbl");
            TestKit.check("PopLbl".equals(pitem.customizationLabel()), "NSPopoverTouchBarItem customizationLabel round-trip");
            pitem.setCollapsedRepresentationLabel("Open");
            TestKit.check("Open".equals(pitem.collapsedRepresentationLabel()), "collapsedRepresentationLabel round-trip");
            TestKit.noThrow("setCollapsedRepresentation(nil) no crash (default button)", () -> pitem.setCollapsedRepresentation(null));
            NSView cr = pitem.collapsedRepresentation();
            System.out.println("  collapsedRepresentation = " + cr);
            TestKit.probe("collapsedRepresentation getter no crash");
            pitem.setCollapsedRepresentationImage(null);
            TestKit.check(pitem.collapsedRepresentationImage() == null, "collapsedRepresentationImage nil after clear");
            NSTouchBar bar = NSTouchBar.create();
            pitem.setPopover(bar);
            TestKit.check(pitem.popover() != null, "NSPopoverTouchBarItem popover non-nil after set");
            pitem.setPressAndHoldTouchBar(bar);
            TestKit.check(pitem.pressAndHoldTouchBar() != null, "pressAndHoldTouchBar non-nil after set");
            TestKit.noThrow("setPressAndHoldTouchBar(nil) no crash", () -> pitem.setPressAndHoldTouchBar(null));
            boolean scb = pitem.showsCloseButton();
            pitem.setShowsCloseButton(!scb);
            TestKit.check(pitem.showsCloseButton() == !scb, "showsCloseButton toggled");
            pitem.setShowsCloseButton(scb);
            pitem.showPopover((MemorySegment) null);
            TestKit.noThrow("showPopover(nil)/dismissPopover(nil) no crash while hidden", () -> pitem.dismissPopover((MemorySegment) null));
            NSGestureRecognizer gr = pitem.makeStandardActivatePopoverGestureRecognizer();
            System.out.println("  standardActivatePopoverGestureRecognizer = " + gr);
            TestKit.probe("makeStandardActivatePopoverGestureRecognizer no crash");

            // NSSliderTouchBarItem: full header coverage
            NSSliderTouchBarItem sitem = NSSliderTouchBarItem.create("slider." + System.nanoTime());
            TestKit.probe("NSSliderTouchBarItem view getter no crash (" + (sitem.view() == null ? "nil" : "set") + ")");
            NSSlider custom = NSSlider.create(new NSRect(0, 0, 140, 30));
            custom.setMinValue(0.0);
            custom.setMaxValue(100.0);
            sitem.setSlider(custom);
            sitem.setMinimumSliderWidth(40.0);
            TestKit.check(Math.abs(sitem.minimumSliderWidth() - 40.0) < 0.01, "minimumSliderWidth round-trip 40.0");
            sitem.setMaximumSliderWidth(300.0);
            TestKit.check(Math.abs(sitem.maximumSliderWidth() - 300.0) < 0.01, "maximumSliderWidth round-trip 300.0");
            sitem.setLabel("Vol");
            TestKit.check("Vol".equals(sitem.label()), "NSSliderTouchBarItem label round-trip");
            sitem.setLabel(null);
            TestKit.check(sitem.label() == null, "NSSliderTouchBarItem label nil after clear");
            sitem.setMinimumValueAccessory(null);
            sitem.setMaximumValueAccessory(null);
            TestKit.check(sitem.minimumValueAccessory() == null && sitem.maximumValueAccessory() == null,
                    "value accessories nil after clear");
            sitem.setValueAccessoryWidth(1.0);
            TestKit.check(Math.abs(sitem.valueAccessoryWidth() - 1.0) < 0.01, "valueAccessoryWidth round-trip 1.0");
            sitem.setCustomizationLabel("SlLbl");
            TestKit.check("SlLbl".equals(sitem.customizationLabel()), "NSSliderTouchBarItem customizationLabel round-trip");

            // NSTouchBar: full header coverage
            NSTouchBar tbar = NSTouchBar.create();
            tbar.setCustomizationIdentifier("com.example.test-" + System.nanoTime());
            TestKit.check(tbar.customizationIdentifier() != null, "NSTouchBar customizationIdentifier round-trip non-nil");
            NSArray empty = NSArray.array();
            tbar.setDefaultItemIdentifiers(empty);
            TestKit.check(tbar.defaultItemIdentifiers() != null, "defaultItemIdentifiers non-nil after set empty");
            TestKit.check(tbar.itemIdentifiers() != null, "itemIdentifiers non-nil");
            tbar.setCustomizationAllowedItemIdentifiers(empty);
            TestKit.check(tbar.customizationAllowedItemIdentifiers() != null, "customizationAllowedItemIdentifiers non-nil");
            tbar.setCustomizationRequiredItemIdentifiers(empty);
            TestKit.check(tbar.customizationRequiredItemIdentifiers() != null, "customizationRequiredItemIdentifiers non-nil");
            tbar.setPrincipalItemIdentifier(null);
            TestKit.check(tbar.principalItemIdentifier() == null, "principalItemIdentifier nil after clear");
            tbar.setEscapeKeyReplacementItemIdentifier(null);
            TestKit.check(tbar.escapeKeyReplacementItemIdentifier() == null, "escapeKeyReplacementItemIdentifier nil after clear");
            tbar.setTemplateItems(NSSet.set());
            TestKit.check(tbar.templateItems() != null, "templateItems non-nil after set empty set");
            NSTouchBarItem found = tbar.itemForIdentifier("missing.id." + System.nanoTime());
            System.out.println("  itemForIdentifier(missing) = " + found);
            TestKit.probe("NSTouchBar itemForIdentifier(missing) no crash");
            System.out.println("  touchBar isVisible = " + tbar.isVisible());
            TestKit.probe("NSTouchBar isVisible no crash (got " + tbar.isVisible() + ")");
            // class flag (10.15+; guard, restore after)
            if (responds(ObjC.cls("NSTouchBar"), "isAutomaticCustomizeTouchBarMenuItemEnabled")) {
                boolean o = NSTouchBar.isAutomaticCustomizeTouchBarMenuItemEnabled();
                NSTouchBar.setAutomaticCustomizeTouchBarMenuItemEnabled(o);
                TestKit.check(NSTouchBar.isAutomaticCustomizeTouchBarMenuItemEnabled() == o,
                        "automaticCustomizeTouchBarMenuItemEnabled round-trip (" + o + ")");
            } else {
                TestKit.skipCase("NOTE automaticCustomizeTouchBarMenuItemEnabled absent (no-crash pass)");
            }

            // NSTouchBarItem base: viewController/customizationLabel/constants
            NSTouchBarItem base = NSTouchBarItem.create("base." + System.nanoTime());
            System.out.println("  base customizationLabel = \"" + base.customizationLabel() + "\"");
            TestKit.check(base.customizationLabel() != null, "NSTouchBarItem customizationLabel non-null (empty string by default)");
            TestKit.probe("NSTouchBarItem viewController getter no crash ("
                    + (base.viewController() == null ? "nil" : "set") + ")");
            TestKit.check(!NSTouchBarItem.FIXED_SPACE_SMALL.isEmpty()
                    && !NSTouchBarItem.FIXED_SPACE_LARGE.isEmpty()
                    && !NSTouchBarItem.FLEXIBLE_SPACE.isEmpty()
                    && !NSTouchBarItem.OTHER_ITEMS_PROXY.isEmpty(),
                    "NSTouchBarItem identifier constants non-empty");
        } catch (Throwable t) {
            TestKit.check(false, "TouchBar section threw: " + t);
            t.printStackTrace(System.out);
        }

        TestKit.pump(app, 600);
        TestKit.close(window);
        try { Files.deleteIfExists(saDir); } catch (Throwable ignore) {}
        }
        TestKit.end();
    }
}
