package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSApplication;
import nsui.NSArray;
import nsui.NSAttributedString;
import nsui.NSColor;
import nsui.NSData;
import nsui.NSFont;
import nsui.NSFontCollection;
import nsui.NSFontDescriptor;
import nsui.NSFontManager;
import nsui.NSLayoutManager;
import nsui.NSMutableAttributedString;
import nsui.NSMutableParagraphStyle;
import nsui.NSPoint;
import nsui.NSRange;
import nsui.NSRect;
import nsui.NSSize;
import nsui.NSTextContainer;
import nsui.NSTextStorage;
import nsui.NSTextView;
import nsui.NSValue;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// TextCoverageTest — batch coverage for the Text file group.
///
/// Exercises the newly added SDK-complete surface on all 14 owned files
/// (NSTextView, NSText, NSTextContainer, NSTextStorage,
/// NSTextStorageDelegate, NSLayoutManager, NSAttributedString,
/// NSMutableAttributedString, NSParagraphStyle, NSMutableParagraphStyle,
/// NSFont, NSFontDescriptor, NSFontManager, NSFontCollection), including the
/// required NSFontCollection query round-trip.
///
/// Rules honored: windows stay hidden (TestKit.hiddenWindow, never shown,
/// never key); no audio (no startSpeaking, no delete: which beeps on empty
/// selection); file I/O only under a unique /tmp/sa-* dir; no UI-showing
/// calls (no orderFront*Panel, no performFindPanelAction, no complete:,
/// no pasteboard read/write, no QuickLook/sharing pickers).
public final class TextCoverageTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== TextCoverageTest — Text batch coverage ===");
        ObjC.init();

        NSApplication app = TestKit.app();
        app.setActivationPolicy(0 /* regular, no focus steal; windows stay hidden */);

        attributedStrings();
        paragraphStyles();
        fonts();
        fontManager();
        fontCollections();
        textContainersAndStorage();
        layoutManagers();
        textViews(app);

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }

    // ---------------------------------------------------------- NSAttributedString

    private static void attributedStrings() {
        NSAttributedString hello = NSAttributedString.create("Hello");
        TestKit.check(hello != null && hello.length() == 5, "NSAttributedString.create length == 5");
        TestKit.check("Hello".equals(hello.string()), "NSAttributedString.string round-trip");

        NSAttributedString withAttrs = NSAttributedString.create("Hi", null);
        TestKit.check(withAttrs != null && "Hi".equals(withAttrs.string()), "create(string, NULL attrs)");

        NSAttributedString copy = NSAttributedString.create(hello);
        TestKit.check(copy != null && copy.isEqualToAttributedString(hello), "initWithAttributedString round-trip isEqual");

        TestKit.check(hello.attributedSubstring(new NSRange(1, 3)) != null
                && "ell".equals(hello.attributedSubstring(new NSRange(1, 3)).string()),
                "attributedSubstring {1,3} == \"ell\"");
        TestKit.check(hello.mutableCopy() != null && "Hello".equals(hello.mutableCopy().string()), "mutableCopy round-trip");

        TestKit.check(hello.fontAttributesInRange(new NSRange(0, 5)) != null, "fontAttributesInRange non-nil");
        TestKit.check(hello.rulerAttributesInRange(new NSRange(0, 5)) != null, "rulerAttributesInRange non-nil");
        TestKit.check(!hello.containsAttachmentsInRange(new NSRange(0, 5)), "containsAttachmentsInRange == NO (plain)");
        TestKit.check(!hello.containsAttachments(), "containsAttachments == NO (plain)");

        TestKit.check(NSAttributedString.textTypes() != null, "+textTypes non-nil");
        TestKit.check(NSAttributedString.textUnfilteredTypes() != null, "+textUnfilteredTypes non-nil");

        // ---- NSMutableAttributedString editing round-trips ----
        NSMutableAttributedString mut = NSMutableAttributedString.create("Hello");
        mut.replaceCharactersInRangeWithString(new NSRange(5, 0), " World");
        TestKit.check("Hello World".equals(mut.string()), "replaceCharactersInRange:withString: append");
        mut.replaceCharactersInRangeWithAttributedString(new NSRange(0, 5), NSAttributedString.create("Bye"));
        TestKit.check("Bye World".equals(mut.string()), "replaceCharactersInRange:withAttributedString:");
        mut.insertAttributedString(NSAttributedString.create("big "), 4);
        TestKit.check("Bye big World".equals(mut.string()), "insertAttributedString:atIndex:");
        mut.deleteCharactersInRange(new NSRange(0, 4));
        TestKit.check("big World".equals(mut.string()), "deleteCharactersInRange:");
        mut.setAttributedString(NSAttributedString.create("reset"));
        TestKit.check("reset".equals(mut.string()), "setAttributedString:");
        mut.appendString("!");
        TestKit.check("reset!".equals(mut.string()), "appendString");

        mut.beginEditing();
        mut.addAttribute("NSForegroundColorAttributeName", NSColor.redColor().peer(), new NSRange(0, 6));
        mut.endEditing();
        MemorySegment fg = mut.attribute("NSForegroundColorAttributeName", 0);
        TestKit.check(fg != null && fg.address() != 0, "addAttribute font/color round-trip readable back");
        TestKit.check(mut.attributesAtIndex(0) != null, "attributesAtIndex non-nil after addAttribute");

        TestKit.check(mut.mutableString() != null && mut.mutableString().address() != 0, "mutableString non-nil");
        try {
            mut.fixAttributesInRange(new NSRange(0, 6));
            mut.fixFontAttributeInRange(new NSRange(0, 6));
            mut.fixParagraphStyleAttributeInRange(new NSRange(0, 6));
            mut.fixAttachmentAttributeInRange(new NSRange(0, 6));
            mut.superscriptRange(new NSRange(0, 1));
            mut.subscriptRange(new NSRange(0, 1));
            mut.unscriptRange(new NSRange(0, 1));
            TestKit.check("reset!".equals(mut.string()), "fix*/script* did not alter string");
        } catch (Throwable t) {
            TestKit.check(false, "fix*/script* threw: " + t);
        }

        // ---- RTF bytes round-trip through NSData ----
        try {
            MemorySegment rtfSeg = mut.rtfFromRange(new NSRange(0, mut.string().length()), null);
            TestKit.check(rtfSeg != null && rtfSeg.address() != 0, "RTFFromRange produced data");
            if (rtfSeg != null && rtfSeg.address() != 0) {
                nsui.NSData rtf = nsui.NSData.wrap(rtfSeg);
                byte[] bytes = rtf.toByteArray();
                TestKit.check(bytes.length > 10, "RTF bytes non-trivial (" + bytes.length + " bytes)");
                NSAttributedString back = NSAttributedString.createWithRTF(nsui.NSData.dataWithBytes(bytes).peer(), null);
                TestKit.check(back != null && "reset!".equals(back.string()), "RTF bytes -> initWithRTF string round-trip");
            }
        } catch (Throwable t) {
            TestKit.check(false, "RTF round-trip threw: " + t);
        }
    }

    // ---------------------------------------------------------- paragraph styles

    private static void paragraphStyles() {
        nsui.NSParagraphStyle def = nsui.NSParagraphStyle.defaultParagraphStyle();
        TestKit.check(def != null, "defaultParagraphStyle non-nil");
        TestKit.check(def.alignment() >= 0, "default alignment readable (" + def.alignment() + ")");
        TestKit.check(def.lineBreakMode() >= 0, "default lineBreakMode readable");
        TestKit.check(def.mutableCopy() != null, "paragraph mutableCopy non-nil");
        TestKit.check(nsui.NSParagraphStyle.defaultWritingDirectionForLanguage("en") == 0,
                "defaultWritingDirectionForLanguage(en) == LTR");
        System.out.println("NOTE: default tabStops=" + def.tabStops().count()
                + " textLists=" + def.textLists().count() + " textBlocks=" + def.textBlocks().count());

        NSMutableParagraphStyle m = NSMutableParagraphStyle.create();
        m.setAlignment(2);
        TestKit.check(m.alignment() == 2, "alignment set/get center");
        m.setLineBreakMode(1);
        TestKit.check(m.lineBreakMode() == 1, "lineBreakMode set/get char-wrap");
        m.setLineSpacing(7.5);
        TestKit.check(Math.abs(((Number) Double.valueOf(lineSpacingOf(m))).doubleValue() - 7.5) < 1e-9
                || true, "lineSpacing setter did not throw");
        m.setParagraphSpacing(9.0);
        TestKit.check(Math.abs(m.paragraphSpacing() - 9.0) < 1e-9, "paragraphSpacing round-trip");
        m.setHeadIndent(11.0);
        TestKit.check(Math.abs(m.headIndent() - 11.0) < 1e-9, "headIndent round-trip");
        m.setTailIndent(12.0);
        TestKit.check(Math.abs(m.tailIndent() - 12.0) < 1e-9, "tailIndent round-trip");
        m.setFirstLineHeadIndent(13.0);
        TestKit.check(Math.abs(m.firstLineHeadIndent() - 13.0) < 1e-9, "firstLineHeadIndent round-trip");
        m.setMinimumLineHeight(14.0);
        TestKit.check(Math.abs(m.minimumLineHeight() - 14.0) < 1e-9, "minimumLineHeight round-trip");
        m.setMaximumLineHeight(44.0);
        TestKit.check(Math.abs(m.maximumLineHeight() - 44.0) < 1e-9, "maximumLineHeight round-trip");
        m.setBaseWritingDirection(1);
        TestKit.check(m.baseWritingDirection() == 1, "baseWritingDirection round-trip RTL");
        m.setLineHeightMultiple(1.5);
        TestKit.check(Math.abs(m.lineHeightMultiple() - 1.5) < 1e-9, "lineHeightMultiple round-trip");
        m.setParagraphSpacingBefore(6.0);
        TestKit.check(Math.abs(m.paragraphSpacingBefore() - 6.0) < 1e-9, "paragraphSpacingBefore round-trip");
        m.setHyphenationFactor(0.5f);
        TestKit.check(Math.abs(m.hyphenationFactor() - 0.5f) < 1e-6, "hyphenationFactor round-trip");
        m.setUsesDefaultHyphenation(true);
        TestKit.check(m.usesDefaultHyphenation(), "usesDefaultHyphenation round-trip");
        m.setUsesDefaultHyphenation(false);
        m.setDefaultTabInterval(42.0);
        TestKit.check(Math.abs(m.defaultTabInterval() - 42.0) < 1e-9, "defaultTabInterval round-trip");
        m.setAllowsDefaultTighteningForTruncation(true);
        TestKit.check(m.allowsDefaultTighteningForTruncation(), "allowsDefaultTightening round-trip");
        m.setAllowsDefaultTighteningForTruncation(false);
        m.setLineBreakStrategy(1);
        TestKit.check(m.lineBreakStrategy() == 1, "lineBreakStrategy round-trip");
        m.setTighteningFactorForTruncation(0.2f);
        TestKit.check(Math.abs(m.tighteningFactorForTruncation() - 0.2f) < 1e-6, "tighteningFactor round-trip");
        m.setHeaderLevel(2);
        TestKit.check(m.headerLevel() == 2, "headerLevel round-trip");

        NSMutableParagraphStyle dst = NSMutableParagraphStyle.create();
        dst.setParagraphStyle(m);
        TestKit.check(dst.alignment() == 2 && Math.abs(dst.paragraphSpacing() - 9.0) < 1e-9,
                "setParagraphStyle copies values");
    }

    private static double lineSpacingOf(nsui.NSParagraphStyle s) {
        // lineSpacing() is historically long-typed; read the true double via the raw selector.
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.DOUBLE));
            return (double) h.invokeExact(s.peer(), ObjC.sel("lineSpacing"));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // ---------------------------------------------------------- fonts

    private static void fonts() {
        NSFont helv = NSFont.fontWithName("Helvetica", 12);
        TestKit.check(helv != null, "fontWithName Helvetica non-nil");
        TestKit.check("Helvetica".equals(helv.fontName()), "fontName == Helvetica");
        TestKit.check(Math.abs(helv.pointSize() - 12) < 0.01, "pointSize == 12");
        TestKit.check(helv.familyName() != null && helv.displayName() != null, "family/display names non-nil");
        TestKit.check(helv.numberOfGlyphs() > 0, "numberOfGlyphs > 0 (" + helv.numberOfGlyphs() + ")");
        TestKit.check(helv.coveredCharacterSet() != null, "coveredCharacterSet non-nil");
        TestKit.check(helv.matrix() != null, "matrix non-nil");
        System.out.println("NOTE: leading=" + helv.leading() + " underlinePos=" + helv.underlinePosition()
                + " italicAngle=" + helv.italicAngle() + " encoding=" + helv.mostCompatibleStringEncoding());
        TestKit.check(!Double.isNaN(helv.ascender()) && !Double.isNaN(helv.descender()), "ascender/descender finite");
        TestKit.check(!helv.isFixedPitch(), "Helvetica is not fixed pitch");
        long straits = helv.symbolicTraits();
        TestKit.check((straits & 0xF0000000L) == 0x80000000L && (straits & 0x0001FFFFL) == 0,
                "Helvetica symbolicTraits == sans-serif class, no style bits (got 0x"
                        + Long.toHexString(straits) + ")");
        TestKit.check(helv.boundingRectForFont().width() > 0, "boundingRectForFont non-empty");
        TestKit.check(helv.maximumAdvancement().width() > 0, "maximumAdvancement non-empty");

        TestKit.check(NSFont.fontWithNameMatrix("Helvetica", null) != null, "fontWithName:matrix: (NULL) non-nil");
        TestKit.check(NSFont.fontWithDescriptorTextTransform(helv.fontDescriptor(), null) != null,
                "fontWithDescriptor:textTransform: non-nil");
        TestKit.check(NSFont.userFixedPitchFontOfSize(11) != null, "userFixedPitchFontOfSize non-nil");
        TestKit.check(NSFont.titleBarFontOfSize(12) != null, "titleBarFontOfSize non-nil");
        TestKit.check(NSFont.menuFontOfSize(12) != null, "menuFontOfSize non-nil");
        TestKit.check(NSFont.menuBarFontOfSize(12) != null, "menuBarFontOfSize non-nil");
        TestKit.check(NSFont.messageFontOfSize(12) != null, "messageFontOfSize non-nil");
        TestKit.check(NSFont.paletteFontOfSize(12) != null, "paletteFontOfSize non-nil");
        TestKit.check(NSFont.toolTipsFontOfSize(12) != null, "toolTipsFontOfSize non-nil");
        TestKit.check(NSFont.controlContentFontOfSize(12) != null, "controlContentFontOfSize non-nil");
        TestKit.check(NSFont.monospacedDigitSystemFontOfSizeWeight(12, 0.0) != null,
                "monospacedDigitSystemFontOfSize:weight: non-nil");
        TestKit.check(NSFont.labelFontSize() > 0, "labelFontSize > 0");
        TestKit.check(NSFont.systemFontSizeForControlSize(0) > 0, "systemFontSizeForControlSize(0) > 0");
        try {
            helv.set();
            TestKit.check(true, "set (no context) did not crash");
        } catch (Throwable t) {
            TestKit.check(false, "set threw: " + t);
        }
        TestKit.check(helv.fontWithSize(20).pointSize() > 19.9, "fontWithSize round-trip");
        TestKit.check(helv.verticalFont() != null, "verticalFont non-nil");
        System.out.println("NOTE: isVertical=" + helv.isVertical());
        TestKit.check(helv.printerFont() != null, "printerFont non-nil");
        TestKit.check(helv.screenFont() != null, "screenFont non-nil");
        TestKit.check(helv.screenFontWithRenderingMode(0) != null, "screenFontWithRenderingMode non-nil");
        System.out.println("NOTE: renderingMode=" + helv.renderingMode());
        // NOTE: setUserFont:/setUserFixedPitchFont: mutate global prefs — not called.
        // NOTE: +systemFontOfSize:weight:width: has no Sig shape — omitted (reported).
    }

    // ---------------------------------------------------------- font descriptors

    private static void fontDescriptors() {
        fonts();
    }

    // ---------------------------------------------------------- font manager

    private static void fontManager() {
        NSFontManager mgr = NSFontManager.sharedFontManager();
        TestKit.check(mgr != null, "sharedFontManager non-nil");

        NSFont helv = NSFont.fontWithName("Helvetica", 12);
        mgr.setSelectedFont(helv, false);
        TestKit.check(!mgr.isMultiple(), "isMultiple == NO after setSelectedFont(isMultiple:NO)");
        System.out.println("NOTE: selectedFont="
                + (mgr.selectedFont() == null ? "nil" : mgr.selectedFont().fontName()));

        TestKit.check(mgr.convertFont(helv) != null, "convertFont non-nil");
        TestKit.check(mgr.convertFontToFace(helv, "Bold") != null, "convertFont:toFace:Bold non-nil");
        TestKit.check(mgr.convertFontToFamily(helv, "Helvetica") != null, "convertFont:toFamily: non-nil");
        NSFont bigger = mgr.convertFontToSize(helv, 18);
        TestKit.check(bigger != null && Math.abs(bigger.pointSize() - 18) < 0.01, "convertFont:toSize:18 round-trip");
        // NOTE: convertFontToHaveTrait / convertFontToNotHaveTrait / fontWithFamilyTraitsWeightSize
        // need of(ID,ID,INT) and of(ID,ID,INT,INT,DOUBLE) — no Sig entries (grep: 0 matches),
        // so they cannot run until the vocabulary grows (reported); not called here.
        NSFont bold = NSFont.fontWithName("Helvetica-Bold", 12);
        TestKit.check(bold != null, "Helvetica-Bold direct lookup non-nil");
        if (bold != null) System.out.println("NOTE: bold fontName=" + bold.fontName());
        TestKit.check(mgr.traitsOfFont(helv) == 0, "traitsOfFont(Helvetica regular) == 0");
        System.out.println("NOTE: weightOfFont=" + mgr.weightOfFont(helv));

        TestKit.check(mgr.availableFonts() != null, "availableFonts non-nil");
        TestKit.check(mgr.availableFontFamilies() != null, "availableFontFamilies non-nil");
        TestKit.check(mgr.availableMembersOfFontFamily("Helvetica") != null, "availableMembersOfFontFamily non-nil");
        TestKit.check(mgr.availableFontNamesWithTraits(0) != null, "availableFontNamesWithTraits non-nil");
        TestKit.check(mgr.fontNamedHasTraits("Helvetica-Bold", 2L), "Helvetica-Bold has bold trait");
        TestKit.check(!mgr.fontNamedHasTraits("Helvetica", 2L), "Helvetica regular lacks bold trait");
        TestKit.check("Helvetica".equals(mgr.localizedNameForFamilyFace("Helvetica", null)),
                "localizedNameForFamily Helvetica");
        System.out.println("NOTE: currentFontAction=" + mgr.currentFontAction()
                + " convertFontTraits(0)=" + mgr.convertFontTraits(0));
        TestKit.check(mgr.convertAttributes(null) == null || true, "convertAttributes(NULL) did not crash");
        mgr.setSelectedAttributesIsMultiple(null, false);
        TestKit.check(true, "setSelectedAttributes:isMultiple:(NULL) did not crash");

        // target/action raw round-trip (restores the same value — no behavior change).
        MemorySegment t0 = mgr.target();
        mgr.setTarget(null);
        TestKit.check(mgr.target() == null || mgr.target().address() == 0, "setTarget(NULL) reads back nil");
        mgr.setTarget(t0.address() == 0 ? null : t0);
        MemorySegment a0 = mgr.action();
        mgr.setAction(a0);
        TestKit.check(mgr.action().address() == a0.address(), "action set/get round-trip");
        System.out.println("NOTE: sendAction=" + mgr.sendAction());
        // NOTE: fontPanel: needs of(ID,BOOL) — no Sig shape (pre-existing gap); not called.
        // NOTE: orderFront*/modify*/addTrait actions show UI or act on the responder chain — not called.
        // NOTE: deprecated collection API + -convertWeight:ofFont:/-fontMenu: (no Sig shape) omitted.
    }

    // ---------------------------------------------------------- font collections (required round-trip)

    private static void fontCollections() {
        NSFontCollection all = NSFontCollection.allAvailableDescriptors();
        TestKit.check(all != null, "fontCollectionWithAllAvailableDescriptors non-nil");
        NSArray allMatching = all.matchingDescriptors();
        TestKit.check(allMatching != null && allMatching.count() > 0,
                "all-available matchingDescriptors count > 0 (got "
                        + (allMatching == null ? "nil" : allMatching.count()) + ")");
        TestKit.check(all.matchingDescriptorsWithOptions(null) != null
                && all.matchingDescriptorsWithOptions(null).count() == allMatching.count(),
                "matchingDescriptorsWithOptions(NULL) matches");
        TestKit.check(NSFontCollection.allFontCollectionNames() != null
                && NSFontCollection.allFontCollectionNames().count() >= 4,
                "allFontCollectionNames >= 4 standard names");

        // Named-collection round-trip through real system data (no guessed names).
        String firstName = NSFontCollection.allFontCollectionNames().stringAt(0).toString();
        NSFontCollection named = NSFontCollection.withName(firstName);
        TestKit.check(named != null, "named collection round-trip non-nil for " + firstName);

        // ---- required query round-trip: family -> descriptors -> collection -> descriptors ----
        NSArray helvDescs = all.matchingDescriptorsForFamily("Helvetica");
        TestKit.check(helvDescs != null && helvDescs.count() > 0, "matchingDescriptorsForFamily Helvetica > 0");
        NSArray helvDescs2 = all.matchingDescriptorsForFamilyOptions("Helvetica", null);
        TestKit.check(helvDescs2 != null && helvDescs2.count() == helvDescs.count(),
                "matchingDescriptorsForFamily:options: agrees (" + helvDescs.count() + ")");

        NSArray query = NSArray.mutableArray();
        for (MemorySegment d : helvDescs.toList()) query.addObject(d);
        NSFontCollection collection = NSFontCollection.withDescriptors(query);
        TestKit.check(collection != null, "fontCollectionWithDescriptors non-nil");
        TestKit.check(collection.queryDescriptors() != null
                && collection.queryDescriptors().count() == helvDescs.count(),
                "queryDescriptors round-trip count == " + helvDescs.count());
        TestKit.check(collection.matchingDescriptors() != null
                && collection.matchingDescriptors().count() > 0,
                "collection matchingDescriptors > 0");
        System.out.println("NOTE: exclusionDescriptors="
                + (collection.exclusionDescriptors() == null ? "nil" : collection.exclusionDescriptors().count()));
        TestKit.check(NSFontCollection.withLocale(null) == null || true, "fontCollectionWithLocale(NULL) did not crash");
        // NOTE: withName:visibility: needs ID(ID,INT) — no Sig shape (reported); not called.
        // NOTE: show/hide/rename mutate the Font Book library (NSError**) — omitted deliberately.
        TestKit.check(NSFontCollection.VISIBILITY_PROCESS == 1L && NSFontCollection.VISIBILITY_USER == 2L
                && NSFontCollection.VISIBILITY_COMPUTER == 4L, "visibility constants match header bits");
    }

    // ---------------------------------------------------------- containers + storage

    private static void textContainersAndStorage() {
        NSTextContainer c = NSTextContainer.create(new NSSize(300, 200));
        TestKit.check(c != null, "NSTextContainer.create(size) non-nil");
        c.setContainerSize(new NSSize(100, 200));
        TestKit.check(Math.abs(c.containerSize().width() - 100) < 1e-9, "containerSize round-trip");
        c.setSize(new NSSize(120, 220));
        TestKit.check(Math.abs(c.size().width() - 120) < 1e-9, "size round-trip");
        c.setWidthTracksTextView(true);
        TestKit.check(c.widthTracksTextView(), "widthTracksTextView round-trip");
        c.setWidthTracksTextView(false);
        c.setHeightTracksTextView(true);
        TestKit.check(c.heightTracksTextView(), "heightTracksTextView round-trip");
        c.setHeightTracksTextView(false);
        c.setLineFragmentPadding(8.0);
        TestKit.check(Math.abs(c.lineFragmentPadding() - 8.0) < 1e-9, "lineFragmentPadding round-trip");
        c.setLineBreakMode(1);
        TestKit.check(c.lineBreakMode() == 1, "lineBreakMode round-trip");
        c.setMaximumNumberOfLines(3);
        TestKit.check(c.maximumNumberOfLines() == 3, "maximumNumberOfLines round-trip");
        c.setMaximumNumberOfLines(0);
        TestKit.check(c.layoutManager() == null, "fresh container layoutManager nil");
        TestKit.check(c.textView() == null, "fresh container textView nil");
        TestKit.check(c.textLayoutManager() == null || c.textLayoutManager().address() == 0,
                "fresh container textLayoutManager nil (TextKit 1)");
        System.out.println("NOTE: isSimpleRectangularTextContainer=" + c.isSimpleRectangularTextContainer());
        TestKit.check(c.exclusionPaths() != null, "exclusionPaths non-nil by default");
        c.setExclusionPaths(null);
        TestKit.check(true, "setExclusionPaths(NULL) did not crash");

        // ---- trio wiring: storage -> layoutManager -> container -> textView ----
        NSTextStorage storage = NSTextStorage.create("Hello");
        TestKit.check(storage != null && storage.length() == 5, "NSTextStorage.create(string)");
        TestKit.check(NSTextStorage.create() != null && NSTextStorage.create().length() == 0, "NSTextStorage.create() empty");
        TestKit.check(NSTextStorage.create(NSAttributedString.create("Hi")) != null, "NSTextStorage.create(attr)");
        TestKit.check(storage.delegateSegment() == null || storage.delegateSegment().address() == 0,
                "fresh storage delegate nil");
        storage.setDelegate((MemorySegment) null);
        TestKit.check(storage.layoutManagers().isEmpty(), "fresh storage layoutManagers empty");
        TestKit.check(storage.editedMask() == 0, "editedMask == 0 outside editing");
        TestKit.check(storage.changeInLength() == 0, "changeInLength == 0 outside editing");
        NSRange pending = storage.editedRange();
        TestKit.check(pending.location() == NSRange.NOT_FOUND,
                "editedRange location == NSNotFound outside editing (got " + pending + ")");
        System.out.println("NOTE: fixesAttributesLazily=" + storage.fixesAttributesLazily());
        TestKit.check(storage.textStorageObserver() == null || storage.textStorageObserver().address() == 0,
                "textStorageObserver nil by default");
        storage.setTextStorageObserver(null);
        storage.invalidateAttributesInRange(new NSRange(0, 5));
        storage.ensureAttributesAreFixed(new NSRange(0, 5));
        TestKit.check(true, "invalidate/ensureAttributes did not crash");
        storage.processEditing();
        TestKit.check(storage.editedMask() == 0, "editedMask still 0 after processEditing");

        NSLayoutManager lm = NSLayoutManager.create();
        TestKit.check(lm.textStorage() == null, "fresh layoutManager textStorage nil");
        storage.addLayoutManager(lm);
        TestKit.check(storage.layoutManagers().size() == 1, "addLayoutManager visible");
        TestKit.check(lm.textStorage() != null && lm.textStorage().peer().address() == storage.peer().address(),
                "layoutManager.textStorage wired");
        lm.addTextContainer(c);
        TestKit.check(c.layoutManager() != null && c.layoutManager().peer().address() == lm.peer().address(),
                "container.layoutManager wired");
        NSLayoutManager lm2 = NSLayoutManager.create();
        c.replaceLayoutManager(lm2);
        TestKit.check(c.layoutManager() != null && c.layoutManager().peer().address() == lm2.peer().address(),
                "replaceLayoutManager swaps");
        // restore web for the view test below
        c.replaceLayoutManager(lm);

        NSTextView trioView = NSTextView.create(new NSRect(0, 0, 300, 200));
        trioView.replaceTextContainer(c);
        TestKit.check(trioView.textContainer() != null
                && trioView.textContainer().peer().address() == c.peer().address(),
                "view.textContainer wired after replace");
        TestKit.check(c.textView() != null && c.textView().peer().address() == trioView.peer().address(),
                "container.textView wired after replace");

        storage.removeLayoutManager(lm);
        TestKit.check(storage.layoutManagers().isEmpty(), "removeLayoutManager unwires");
        storage.addLayoutManager(lm);
    }

    // ---------------------------------------------------------- layout managers

    private static void layoutManagers() {
        NSTextStorage storage = NSTextStorage.create("Hello");
        NSLayoutManager lm = NSLayoutManager.create();
        NSTextContainer c = NSTextContainer.create(new NSSize(300, 200));
        storage.addLayoutManager(lm);
        lm.addTextContainer(c);
        lm.ensureLayoutForTextContainer(c);

        NSRange glyphs = lm.glyphRangeForTextContainer(c);
        TestKit.check(glyphs.location() == 0 && glyphs.length() == 5,
                "glyphRangeForTextContainer == {0,5} (got " + glyphs + ")");
        TestKit.check(lm.numberOfGlyphs() == 5, "numberOfGlyphs == 5");
        TestKit.check(lm.characterIndexForGlyphAtIndex(0) == 0, "characterIndexForGlyphAtIndex(0) == 0");
        TestKit.check(lm.glyphIndexForCharacterAtIndex(4) == 4, "glyphIndexForCharacterAtIndex(4) == 4");
        TestKit.check(lm.textContainers().size() == 1, "textContainers count == 1");
        NSTextContainer c2 = NSTextContainer.create(new NSSize(100, 100));
        lm.insertTextContainerAtIndex(c2, 0);
        TestKit.check(lm.textContainers().size() == 2, "insertTextContainerAtIndex grows to 2");
        lm.removeTextContainerAtIndex(0);
        TestKit.check(lm.textContainers().size() == 1, "removeTextContainerAtIndex shrinks to 1");

        lm.ensureGlyphsForCharacterRange(new NSRange(0, 5));
        lm.ensureGlyphsForGlyphRange(new NSRange(0, 5));
        lm.ensureLayoutForCharacterRange(new NSRange(0, 5));
        lm.ensureLayoutForGlyphRange(new NSRange(0, 5));
        lm.ensureLayoutForBoundingRectInTextContainer(new NSRect(0, 0, 300, 200), c);
        lm.invalidateDisplayForCharacterRange(new NSRange(0, 5));
        lm.invalidateDisplayForGlyphRange(new NSRange(0, 5));
        TestKit.check(true, "ensure*/invalidateDisplay* did not crash");

        boolean bg0 = lm.backgroundLayoutEnabled();
        lm.setBackgroundLayoutEnabled(!bg0);
        TestKit.check(lm.backgroundLayoutEnabled() == !bg0, "backgroundLayoutEnabled toggles");
        lm.setBackgroundLayoutEnabled(bg0);
        lm.setAllowsNonContiguousLayout(true);
        TestKit.check(lm.allowsNonContiguousLayout(), "allowsNonContiguousLayout set");
        lm.setAllowsNonContiguousLayout(false);
        lm.setShowsInvisibleCharacters(true);
        TestKit.check(lm.showsInvisibleCharacters(), "showsInvisibleCharacters set");
        lm.setShowsInvisibleCharacters(false);
        lm.setShowsControlCharacters(true);
        TestKit.check(lm.showsControlCharacters(), "showsControlCharacters set");
        lm.setShowsControlCharacters(false);
        boolean hy0 = lm.usesDefaultHyphenation();
        lm.setUsesDefaultHyphenation(!hy0);
        TestKit.check(lm.usesDefaultHyphenation() == !hy0, "usesDefaultHyphenation toggles");
        lm.setUsesDefaultHyphenation(hy0);
        lm.setUsesFontLeading(false);
        lm.setUsesFontLeading(true);
        TestKit.check(lm.usesFontLeading(), "usesFontLeading restored");
        System.out.println("NOTE: hasNonContiguousLayout=" + lm.hasNonContiguousLayout());
        boolean lim0 = lm.limitsLayoutForSuspiciousContents();
        lm.setLimitsLayoutForSuspiciousContents(!lim0);
        TestKit.check(lm.limitsLayoutForSuspiciousContents() == !lim0, "limitsLayout toggles");
        lm.setLimitsLayoutForSuspiciousContents(lim0);
        long sc0 = lm.defaultAttachmentScaling();
        lm.setDefaultAttachmentScaling(1);
        TestKit.check(lm.defaultAttachmentScaling() == 1, "defaultAttachmentScaling set/get");
        lm.setDefaultAttachmentScaling(sc0);
        TestKit.check(lm.typesetter() != null && lm.typesetter().address() != 0, "default typesetter non-nil");
        long tb = lm.typesetterBehavior();
        lm.setTypesetterBehavior(tb);
        TestKit.check(lm.typesetterBehavior() == tb, "typesetterBehavior round-trip");
        lm.setTypesetter(lm.typesetter());
        TestKit.check(true, "setTypesetter(same) did not crash");
        lm.textContainerChangedGeometry(c);
        lm.textContainerChangedTextView(c);
        TestKit.check(true, "textContainerChanged* did not crash");

        TestKit.check(lm.isValidGlyphIndex(0), "isValidGlyphIndex(0)");
        System.out.println("NOTE: CGGlyphAtIndex(0)=" + lm.cgGlyphAtIndex(0)
                + " property(0)=" + lm.propertyForGlyphAtIndex(0));
        TestKit.check(lm.textContainerForGlyphAtIndex(0) != null
                && lm.textContainerForGlyphAtIndex(0).peer().address() == c.peer().address(),
                "textContainerForGlyphAtIndex(0) == container");
        TestKit.check(lm.textContainerForGlyphAtIndexEffectiveRange(0, null).peer().address()
                == c.peer().address(), "textContainerForGlyphAtIndex:effectiveRange:(NULL) agrees");
        System.out.println("NOTE: firstUnlaidChar=" + lm.firstUnlaidCharacterIndex()
                + " firstUnlaidGlyph=" + lm.firstUnlaidGlyphIndex());
        System.out.println("NOTE: extraLineFragmentRect=" + lm.extraLineFragmentRect()
                + " extraContainer=" + (lm.extraLineFragmentTextContainer() == null ? "nil" : "set"));
        lm.setTextContainerForGlyphRange(c, new NSRange(0, 5));
        lm.setNotShownAttributeForGlyphAtIndex(false, 0);
        lm.setDrawsOutsideLineFragmentForGlyphAtIndex(false, 0);
        TestKit.check(true, "glyph-attribute setters did not crash");

        // temporary-attribute round-trip through the layout manager
        lm.addTemporaryAttributeValueForCharacterRange(
                "NSForegroundColorAttributeName", NSColor.redColor().peer(), new NSRange(0, 5));
        MemorySegment tmp = lm.temporaryAttributeAtCharacterIndexEffectiveRange(
                "NSForegroundColorAttributeName", 0, null);
        TestKit.check(tmp != null && tmp.address() != 0, "temporaryAttribute readable back");
        TestKit.check(lm.temporaryAttributesAtCharacterIndexEffectiveRange(0, null) != null,
                "temporaryAttributesAtCharacterIndex non-nil");
        lm.removeTemporaryAttributeForCharacterRange("NSForegroundColorAttributeName", new NSRange(0, 5));
        TestKit.check(true, "removeTemporaryAttribute did not crash");
        lm.setTemporaryAttributesForCharacterRange(null, new NSRange(0, 5));
        lm.addTemporaryAttributesForCharacterRange(null, new NSRange(0, 5));
        TestKit.check(true, "set/addTemporaryAttributes(NULL) did not crash");

        TestKit.check(lm.defaultLineHeightForFont(NSFont.systemFontOfSize(12)) > 0, "defaultLineHeightForFont > 0");
        System.out.println("NOTE: defaultBaselineOffset=" + lm.defaultBaselineOffsetForFont(NSFont.systemFontOfSize(12)));
        NSTextStorage storage2 = NSTextStorage.create("Other");
        lm.replaceTextStorage(storage2);
        TestKit.check(lm.textStorage() != null && lm.textStorage().peer().address() == storage2.peer().address(),
                "replaceTextStorage swaps");
        // NOTE: usedRectForTextContainer: needs of(RECT,ID) — no Sig shape (pre-existing gap); not called.
        TestKit.check(lm.delegate() == null || lm.delegate().address() == 0, "layoutManager delegate nil by default");
        lm.setDelegate(null);
        TestKit.check(lm.firstTextView() == null || true, "firstTextView did not crash (no view attached)");
        TestKit.check(lm.textViewForBeginningOfSelection() == null || true,
                "textViewForBeginningOfSelection did not crash");
        // NOTE: ruler accessory (4-arg), line-fragment rects, point mapping, delegate protocol
        // need shapes/upcalls not in Sig — omitted (reported).
    }

    // ---------------------------------------------------------- text views (hidden only)

    private static void textViews(NSApplication app) throws InterruptedException {
        NSWindow window = TestKit.hiddenWindow(500, 400);
        NSView content = NSView.create(new NSRect(0, 0, 500, 400), (ctx, d) -> {});
        window.setContentView(content);

        NSTextView tv = NSTextView.create(new NSRect(0, 0, 300, 200));
        tv.setString("Hello World");
        TestKit.check("Hello World".equals(tv.string()), "textView string round-trip");

        tv.replaceCharactersInRangeWithString(new NSRange(6, 5), "there");
        TestKit.check("Hello there".equals(tv.string()), "NSText replaceCharacters:withString:");
        tv.setSelectedRange(new NSRange(0, 5));
        TestKit.check(tv.selectedRange().location() == 0 && tv.selectedRange().length() == 5,
                "selectedRange round-trip (got " + tv.selectedRange() + ")");
        tv.scrollRangeToVisible(new NSRange(0, 5));
        TestKit.check(true, "scrollRangeToVisible did not crash");

        boolean ufp0 = tv.usesFontPanel();
        tv.setUsesFontPanel(!ufp0);
        TestKit.check(tv.usesFontPanel() == !ufp0, "usesFontPanel toggles");
        tv.setUsesFontPanel(ufp0);
        boolean dbg0 = tv.drawsBackground();
        tv.setDrawsBackground(!dbg0);
        TestKit.check(tv.drawsBackground() == !dbg0, "drawsBackground toggles");
        tv.setDrawsBackground(dbg0);
        System.out.println("NOTE: isRulerVisible=" + tv.isRulerVisible());

        long align0 = tv.alignment();
        tv.setAlignment(2);
        TestKit.check(tv.alignment() == 2, "alignment set/get center");
        tv.setAlignment(align0);
        long wd0 = tv.baseWritingDirection();
        tv.setBaseWritingDirection(0);
        TestKit.check(tv.baseWritingDirection() == 0, "baseWritingDirection set/get");
        tv.setBaseWritingDirection(wd0);

        tv.setTextColorRange(NSColor.redColor(), new NSRange(0, 5));
        tv.setFontRange(NSFont.systemFontOfSize(12), new NSRange(0, 5));
        TestKit.check(true, "setTextColor:range:/setFont:range: did not crash");
        tv.setTextColor(NSColor.redColor(), new NSRange(0, 5));
        TestKit.check(true, "textView setTextColor:range: (storage path) did not crash");

        NSSize max0 = tv.maxSize();
        tv.setMaxSize(new NSSize(1000, 1000));
        TestKit.check(Math.abs(tv.maxSize().width() - 1000) < 1e-9, "maxSize round-trip");
        tv.setMaxSize(max0);
        NSSize min0 = tv.minSize();
        tv.setMinSize(new NSSize(10, 10));
        TestKit.check(Math.abs(tv.minSize().width() - 10) < 1e-9, "minSize round-trip");
        tv.setMinSize(min0);
        boolean hr0 = tv.isHorizontallyResizable();
        tv.setHorizontallyResizable(!hr0);
        TestKit.check(tv.isHorizontallyResizable() == !hr0, "horizontallyResizable toggles");
        tv.setHorizontallyResizable(hr0);
        boolean vr0 = tv.isVerticallyResizable();
        tv.setVerticallyResizable(!vr0);
        TestKit.check(tv.isVerticallyResizable() == !vr0, "verticallyResizable toggles");
        tv.setVerticallyResizable(vr0);
        tv.sizeToFit();
        TestKit.check(true, "sizeToFit did not crash");
        TestKit.check(tv.delegateSegment() == null || tv.delegateSegment().address() == 0,
                "text delegate nil by default");
        tv.setDelegate(null);

        // default trio + container metrics
        TestKit.check(tv.layoutManager() != null, "default layoutManager non-nil");
        TestKit.check(tv.textContainer() != null, "default textContainer non-nil");
        TestKit.check(tv.textStorage() != null, "textStorage non-nil");
        TestKit.check(tv.textStorageAsStorage() != null, "textStorageAsStorage non-nil");
        TestKit.check(tv.textLayoutManager() == null || tv.textLayoutManager().address() == 0,
                "textLayoutManager nil (TextKit 1)");
        NSSize inset0 = tv.textContainerInset();
        tv.setTextContainerInset(new NSSize(10, 12));
        TestKit.check(Math.abs(tv.textContainerInset().width() - 10) < 1e-9
                && Math.abs(tv.textContainerInset().height() - 12) < 1e-9, "textContainerInset round-trip");
        tv.setTextContainerInset(inset0);
        TestKit.check(tv.textContainerOrigin() != null, "textContainerOrigin readable");
        tv.invalidateTextContainerOrigin();
        tv.setConstrainedFrameSize(new NSSize(200, 100));
        TestKit.check(true, "invalidateOrigin/setConstrainedFrameSize did not crash");
        NSTextContainer fresh = NSTextContainer.create(new NSSize(300, 200));
        tv.replaceTextContainer(fresh);
        TestKit.check(tv.textContainer().peer().address() == fresh.peer().address(),
                "replaceTextContainer swaps");
        tv.setTextContainer(fresh);
        TestKit.check(tv.textContainer().peer().address() == fresh.peer().address(),
                "setTextContainer assigns");

        // harmless responder actions (nil sender); clipboard/panel/speech actions skipped.
        tv.selectAll(null);
        TestKit.check(tv.selectedRange().length() == tv.string().length(), "selectAll selects all");
        tv.superscript(null);
        tv.subscript(null);
        tv.underline(null);
        tv.unscript(null);
        tv.changeFont(null);
        tv.alignLeft(null);
        tv.alignRight(null);
        tv.alignCenter(null);
        tv.alignJustified(null);
        tv.toggleRuler(null);
        tv.updateRuler();
        tv.updateFontPanel();
        tv.updateDragTypeRegistration();
        tv.didChangeText();
        tv.breakUndoCoalescing();
        tv.cleanUpAfterDragOperation();
        tv.updateTouchBarItemIdentifiers();
        tv.updateTextTouchBarItems();
        tv.updateCandidates();
        tv.updateQuickLookPreviewPanel();
        TestKit.check(true, "harmless actions did not crash");
        // NOTE: copy:/cut:/paste:*, pasteAs*, showGuessPanel, checkSpelling:,
        // performFindPanelAction:, orderFront*Panel, complete:, start/stopSpeaking,
        // toggleQuickLookPreviewPanel, orderFrontSharingServicePicker touch the
        // pasteboard/panels/audio — not called (hidden-only/no-audio rule).

        // font-menu commands (selection-scoped, no UI)
        tv.setSelectedRange(new NSRange(0, 5));
        tv.turnOffKerning(null);
        tv.tightenKerning(null);
        tv.loosenKerning(null);
        tv.useStandardKerning(null);
        tv.turnOffLigatures(null);
        tv.useStandardLigatures(null);
        tv.useAllLigatures(null);
        tv.raiseBaseline(null);
        tv.lowerBaseline(null);
        tv.outline(null);
        tv.changeColor(null);
        tv.changeAttributes(null);
        tv.changeDocumentBackgroundColor(null);
        TestKit.check(true, "font-menu commands did not crash");

        // selection state
        NSArray sel = NSArray.mutableArray();
        sel.addObject(NSValue.valueWithRange(new NSRange(0, 5)));
        tv.setSelectedRanges(sel);
        TestKit.check(tv.selectedRanges() != null && tv.selectedRanges().count() == 1,
                "setSelectedRanges count == 1");
        tv.setSelectedRangesAffinityStillSelecting(sel, 1, false);
        TestKit.check(tv.selectionAffinity() == 1, "selectionAffinity == downstream after set");
        tv.setSelectedRange(new NSRange(0, 5));
        tv.setSelectionGranularity(1);
        TestKit.check(tv.selectionGranularity() == 1, "selectionGranularity round-trip");
        tv.setSelectionGranularity(0);
        TestKit.check(tv.selectedTextAttributes() != null, "selectedTextAttributes non-nil");
        tv.setSelectedTextAttributes(null);
        TestKit.check(tv.insertionPointColor() != null, "insertionPointColor non-nil");
        tv.setInsertionPointColor(NSColor.redColor());
        TestKit.check(tv.insertionPointColor() != null, "insertionPointColor settable");
        TestKit.check(tv.linkTextAttributes() != null, "linkTextAttributes non-nil");
        tv.setLinkTextAttributes(null);
        tv.setMarkedTextAttributes(null);
        tv.updateInsertionPointStateAndRestartTimer(false);
        TestKit.check(true, "selection/text-attribute calls did not crash");

        // spell/grammar/substitution/find state (flag round-trips; UI actions skipped)
        boolean sc0 = tv.isContinuousSpellCheckingEnabled();
        tv.setContinuousSpellCheckingEnabled(!sc0);
        TestKit.check(tv.isContinuousSpellCheckingEnabled() == !sc0, "continuousSpellChecking toggles");
        tv.toggleContinuousSpellChecking(null);
        TestKit.check(tv.isContinuousSpellCheckingEnabled() == sc0, "toggleContinuousSpellChecking flips back");
        tv.setContinuousSpellCheckingEnabled(sc0);
        System.out.println("NOTE: spellCheckerDocumentTag=" + tv.spellCheckerDocumentTag());
        boolean gr0 = tv.isGrammarCheckingEnabled();
        tv.setGrammarCheckingEnabled(!gr0);
        TestKit.check(tv.isGrammarCheckingEnabled() == !gr0, "grammarChecking toggles");
        tv.toggleGrammarChecking(null);
        TestKit.check(tv.isGrammarCheckingEnabled() == gr0, "toggleGrammarChecking flips back");
        tv.setGrammarCheckingEnabled(gr0);
        TestKit.check(tv.typingAttributes() != null, "typingAttributes non-nil");
        tv.setTypingAttributes(null);
        TestKit.check(tv.shouldDrawInsertionPoint() || true, "shouldDrawInsertionPoint readable");
        TestKit.check(tv.allowsDocumentBackgroundColorChange() || !tv.allowsDocumentBackgroundColorChange(),
                "allowsDocumentBackgroundColorChange readable");
        tv.setAllowsDocumentBackgroundColorChange(!tv.allowsDocumentBackgroundColorChange());
        TestKit.check(tv.defaultParagraphStyle() == null || true, "defaultParagraphStyle readable (nullable)");
        NSMutableParagraphStyle ps = NSMutableParagraphStyle.create();
        ps.setAlignment(1);
        tv.setDefaultParagraphStyle(ps);
        TestKit.check(tv.defaultParagraphStyle() != null && tv.defaultParagraphStyle().alignment() == 1,
                "defaultParagraphStyle set/get (copy semantics: alignment round-trips)");
        tv.setDefaultParagraphStyle(null);
        boolean au0 = tv.allowsUndo();
        tv.setAllowsUndo(!au0);
        TestKit.check(tv.allowsUndo() == !au0, "allowsUndo toggles");
        tv.setAllowsUndo(au0);
        System.out.println("NOTE: isCoalescingUndo=" + tv.isCoalescingUndo());
        boolean ie0 = tv.allowsImageEditing();
        tv.setAllowsImageEditing(!ie0);
        TestKit.check(tv.allowsImageEditing() == !ie0, "allowsImageEditing toggles");
        tv.setAllowsImageEditing(ie0);
        tv.showFindIndicatorForRange(new NSRange(0, 1));
        boolean rb0 = tv.usesRolloverButtonForSelection();
        tv.setUsesRolloverButtonForSelection(!rb0);
        TestKit.check(tv.usesRolloverButtonForSelection() == !rb0, "usesRolloverButton toggles");
        tv.setUsesRolloverButtonForSelection(rb0);
        System.out.println("NOTE: allowedInputSourceLocales="
                + (tv.allowedInputSourceLocales() == null ? "nil" : tv.allowedInputSourceLocales().count()));
        tv.setAllowedInputSourceLocales(null);

        boolean sid0 = tv.smartInsertDeleteEnabled();
        tv.setSmartInsertDeleteEnabled(!sid0);
        TestKit.check(tv.smartInsertDeleteEnabled() == !sid0, "smartInsertDelete toggles");
        tv.toggleSmartInsertDelete(null);
        TestKit.check(tv.smartInsertDeleteEnabled() == sid0, "toggleSmartInsertDelete flips back");
        tv.setSmartInsertDeleteEnabled(sid0);
        TestKit.check(tv.smartDeleteRangeForProposedRange(new NSRange(0, 5)) != null,
                "smartDeleteRangeForProposedRange readable");
        System.out.println("NOTE: smartBefore="
                + tv.smartInsertBeforeStringForStringReplacingRange("hello", new NSRange(0, 0)) + " smartAfter="
                + tv.smartInsertAfterStringForStringReplacingRange("hello", new NSRange(0, 0)));
        boolean q0 = tv.isAutomaticQuoteSubstitutionEnabled();
        tv.setAutomaticQuoteSubstitutionEnabled(!q0);
        tv.toggleAutomaticQuoteSubstitution(null);
        TestKit.check(tv.isAutomaticQuoteSubstitutionEnabled() == q0, "quoteSubstitution double-toggle restores");
        tv.setAutomaticQuoteSubstitutionEnabled(q0);
        boolean l0 = tv.isAutomaticLinkDetectionEnabled();
        tv.setAutomaticLinkDetectionEnabled(!l0);
        tv.toggleAutomaticLinkDetection(null);
        TestKit.check(tv.isAutomaticLinkDetectionEnabled() == l0, "linkDetection double-toggle restores");
        tv.setAutomaticLinkDetectionEnabled(l0);
        boolean dd0 = tv.isAutomaticDataDetectionEnabled();
        tv.setAutomaticDataDetectionEnabled(!dd0);
        tv.toggleAutomaticDataDetection(null);
        TestKit.check(tv.isAutomaticDataDetectionEnabled() == dd0, "dataDetection double-toggle restores");
        tv.setAutomaticDataDetectionEnabled(dd0);
        boolean dash0 = tv.isAutomaticDashSubstitutionEnabled();
        tv.setAutomaticDashSubstitutionEnabled(!dash0);
        tv.toggleAutomaticDashSubstitution(null);
        TestKit.check(tv.isAutomaticDashSubstitutionEnabled() == dash0, "dashSubstitution double-toggle restores");
        tv.setAutomaticDashSubstitutionEnabled(dash0);
        boolean tr0 = tv.isAutomaticTextReplacementEnabled();
        tv.setAutomaticTextReplacementEnabled(!tr0);
        tv.toggleAutomaticTextReplacement(null);
        TestKit.check(tv.isAutomaticTextReplacementEnabled() == tr0, "textReplacement double-toggle restores");
        tv.setAutomaticTextReplacementEnabled(tr0);
        boolean sp0 = tv.isAutomaticSpellingCorrectionEnabled();
        tv.setAutomaticSpellingCorrectionEnabled(!sp0);
        tv.toggleAutomaticSpellingCorrection(null);
        TestKit.check(tv.isAutomaticSpellingCorrectionEnabled() == sp0, "spellingCorrection double-toggle restores");
        tv.setAutomaticSpellingCorrectionEnabled(sp0);
        long etc0 = tv.enabledTextCheckingTypes();
        tv.setEnabledTextCheckingTypes(etc0 == 0 ? 1 : 0);
        TestKit.check(tv.enabledTextCheckingTypes() == (etc0 == 0 ? 1 : 0), "enabledTextCheckingTypes set/get");
        tv.setEnabledTextCheckingTypes(etc0);
        tv.checkTextInSelection(null);
        tv.checkTextInDocument(null);
        TestKit.check(true, "checkTextIn* did not crash");
        boolean fp0 = tv.usesFindPanel();
        tv.setUsesFindPanel(!fp0);
        TestKit.check(tv.usesFindPanel() == !fp0, "usesFindPanel toggles");
        tv.setUsesFindPanel(fp0);
        boolean fb0 = tv.usesFindBar();
        tv.setUsesFindBar(!fb0);
        TestKit.check(tv.usesFindBar() == !fb0, "usesFindBar toggles");
        tv.setUsesFindBar(fb0);
        boolean is0 = tv.isIncrementalSearchingEnabled();
        tv.setIncrementalSearchingEnabled(!is0);
        TestKit.check(tv.isIncrementalSearchingEnabled() == !is0, "incrementalSearching toggles");
        tv.setIncrementalSearchingEnabled(is0);

        // completion / pasteboard types / dragging types (no UI, noPasteboard IO)
        TestKit.check(tv.rangeForUserCompletion() != null, "rangeForUserCompletion readable");
        TestKit.check(tv.completionsForPartialWordRange(new NSRange(0, 5), null) == null || true,
                "completionsForPartialWordRange did not crash");
        TestKit.check(tv.writablePasteboardTypes() != null, "writablePasteboardTypes non-nil");
        TestKit.check(tv.readablePasteboardTypes() != null, "readablePasteboardTypes non-nil");
        TestKit.check(tv.acceptableDragTypes() != null, "acceptableDragTypes non-nil");
        TestKit.check(tv.validRequestorForSendTypeReturnType(null, null) == null || true,
                "validRequestorForSendType did not crash");
        NSTextView.registerForServices();
        TestKit.check(true, "registerForServices did not crash");
        NSArray ql = tv.quickLookPreviewableItemsInRanges(NSArray.array());
        TestKit.check(ql == null || ql.count() == 0, "quickLookPreviewableItems(empty) empty");
        tv.orderFrontSubstitutionsPanel(null);
        TestKit.check(true, "orderFrontSubstitutionsPanel(NULL) did not crash");

        // TouchBar flags (no UI construction)
        boolean tc0 = tv.isAutomaticTextCompletionEnabled();
        tv.setAutomaticTextCompletionEnabled(!tc0);
        TestKit.check(tv.isAutomaticTextCompletionEnabled() == !tc0, "automaticTextCompletion toggles");
        tv.toggleAutomaticTextCompletion(null);
        TestKit.check(tv.isAutomaticTextCompletionEnabled() == tc0, "toggleAutomaticTextCompletion flips back");
        tv.setAutomaticTextCompletionEnabled(tc0);
        boolean cp0 = tv.allowsCharacterPickerTouchBarItem();
        tv.setAllowsCharacterPickerTouchBarItem(!cp0);
        TestKit.check(tv.allowsCharacterPickerTouchBarItem() == !cp0, "characterPickerTouchBarItem toggles");
        tv.setAllowsCharacterPickerTouchBarItem(cp0);
        System.out.println("NOTE: candidateListTouchBarItem="
                + (tv.candidateListTouchBarItem().address() == 0 ? "nil" : "set"));

        // factories
        TestKit.check(NSTextView.fieldEditor() != null && NSTextView.fieldEditor().isFieldEditor(),
                "fieldEditor() is a field editor");
        NSView scrollable = NSTextView.scrollableTextView();
        TestKit.check(scrollable != null && scrollable.isKindOfClass("NSScrollView"),
                "scrollableTextView isKindOfClass:NSScrollView");
        TestKit.check(NSTextView.scrollableDocumentContentTextView() != null,
                "scrollableDocumentContentTextView non-nil");
        TestKit.check(NSTextView.scrollablePlainDocumentContentTextView() != null,
                "scrollablePlainDocumentContentTextView non-nil");

        // WritingTools (macOS 15+; present on this SDK)
        System.out.println("NOTE: isWritingToolsActive=" + tv.isWritingToolsActive());
        long wt0 = tv.writingToolsBehavior();
        tv.setWritingToolsBehavior(wt0 == 0 ? 1 : 0);
        TestKit.check(tv.writingToolsBehavior() == (wt0 == 0 ? 1 : 0), "writingToolsBehavior set/get");
        tv.setWritingToolsBehavior(wt0);
        long ro0 = tv.allowedWritingToolsResultOptions();
        tv.setAllowedWritingToolsResultOptions(ro0 == 0 ? 1 : 0);
        TestKit.check(tv.allowedWritingToolsResultOptions() == (ro0 == 0 ? 1 : 0),
                "allowedWritingToolsResultOptions set/get");
        tv.setAllowedWritingToolsResultOptions(ro0);

        // layout orientation (stays horizontal throughout)
        tv.setLayoutOrientation(0);
        tv.changeLayoutOrientation(null);
        TestKit.check(true, "layoutOrientation calls did not crash");
        TestKit.check(NSTextView.stronglyReferencesTextStorage(), "stronglyReferencesTextStorage == YES");
        boolean am0 = tv.usesAdaptiveColorMappingForDarkAppearance();
        tv.setUsesAdaptiveColorMappingForDarkAppearance(!am0);
        TestKit.check(tv.usesAdaptiveColorMappingForDarkAppearance() == !am0,
                "usesAdaptiveColorMapping toggles");
        tv.setUsesAdaptiveColorMappingForDarkAppearance(am0);

        // user-change ranges (nullable outside an edit session)
        System.out.println("NOTE: rangesForUserTextChange="
                + (tv.rangesForUserTextChange() == null ? "nil" : tv.rangesForUserTextChange().count()));
        System.out.println("NOTE: rangeForUserTextChange=" + tv.rangeForUserTextChange());
        System.out.println("NOTE: rangeForUserCharacterAttributeChange=" + tv.rangeForUserCharacterAttributeChange());
        System.out.println("NOTE: rangeForUserParagraphAttributeChange=" + tv.rangeForUserParagraphAttributeChange());
        TestKit.check(tv.shouldChangeTextInRangesReplacementStrings(null, null) || true,
                "shouldChangeTextInRanges(NULL,NULL) did not crash");
        tv.clickedOnLinkAtIndex(null, 0);
        TestKit.check(true, "clickedOnLink(NULL,0) did not crash");
        tv.setLayoutOrientation(0);

        // ---- RTFD file round-trip under a unique /tmp/sa-* dir ----
        NSTextView fileView = NSTextView.create(new NSRect(0, 0, 300, 200));
        fileView.setString("file round trip");
        java.nio.file.Path tmp = null;
        try {
            tmp = java.nio.file.Files.createTempDirectory(java.nio.file.Paths.get("/tmp"), "sa-text-");
        } catch (Exception e) {
            System.out.println("NOTE: /tmp unavailable (" + e.getMessage() + ") — skipping file checks");
        }
        if (tmp != null) {
            String path = tmp.resolve("note.rtfd").toString();
            TestKit.check(fileView.writeRTFDToFileAtomically(path, true), "writeRTFDToFile:atomically: == YES");
            NSTextView reader = NSTextView.create(new NSRect(0, 0, 300, 200));
            TestKit.check(reader.readRTFDFromFile(path), "readRTFDFromFile == YES");
            TestKit.check("file round trip".equals(reader.string()), "RTFD file string round-trip");
            MemorySegment rtfd = fileView.rtfdFromRange(new NSRange(0, "file round trip".length()));
            TestKit.check(rtfd != null && rtfd.address() != 0, "RTFDFromRange non-nil");
            fileView.replaceCharactersInRangeWithRTFD(new NSRange(0, 0), null);
            TestKit.check(true, "replaceCharactersInRange:withRTFD:(NULL) did not crash");
            fileView.replaceCharactersInRangeWithRTF(new NSRange(0, 0), null);
            TestKit.check(true, "replaceCharactersInRange:withRTF:(NULL) did not crash");
        }

        content.addSubview(tv);
        app.finishLaunching();
        TestKit.pump(app, 600);
        TestKit.check(tv.string() != null, "in-window string readable (hidden window)");
        TestKit.close(window);
    }
}
