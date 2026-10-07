package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTextView — a rich-text view (NSView -> NSText -> NSTextView).
/// Thin 1:1 wrapper over native `NSTextView`: every method maps to
/// one `objc_msgSend` selector, no cached Java state beyond the peer.
/// Mirrors the native hierarchy so `isKindOfClass:` works for
/// NSTextView / NSText / NSView.
///
/// MVP: wraps the concrete AppKit class `NSTextView` directly via
/// `alloc/initWithFrame:`. Lazy `ensureInit` + `ObjC.handle`
/// follows the existing NSView/Control pattern (resolve-once, invokeExact).
public class NSTextView extends NSText {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment usesFontPanel;
        static MemorySegment setUsesFontPanel;
        static MemorySegment textStorage;
        static MemorySegment setAttributedString;
        static MemorySegment attributedString;
        static MemorySegment layoutManager;
        static MemorySegment textContainer;
        static MemorySegment replaceTextContainer;
        static MemorySegment setTextContainer;
        static MemorySegment textContainerInset;
        static MemorySegment setTextContainerInset;
        static MemorySegment textContainerOrigin;
        static MemorySegment invalidateTextContainerOrigin;
        static MemorySegment textLayoutManager;
        static MemorySegment textContentStorage;
        static MemorySegment setConstrainedFrameSize;
        static MemorySegment turnOffKerning;
        static MemorySegment tightenKerning;
        static MemorySegment loosenKerning;
        static MemorySegment useStandardKerning;
        static MemorySegment turnOffLigatures;
        static MemorySegment useStandardLigatures;
        static MemorySegment useAllLigatures;
        static MemorySegment raiseBaseline;
        static MemorySegment lowerBaseline;
        static MemorySegment outline;
        static MemorySegment performFindPanelAction;
        static MemorySegment alignJustified;
        static MemorySegment changeColor;
        static MemorySegment changeAttributes;
        static MemorySegment changeDocumentBackgroundColor;
        static MemorySegment orderFrontSpacingPanel;
        static MemorySegment orderFrontLinkPanel;
        static MemorySegment orderFrontListPanel;
        static MemorySegment orderFrontTablePanel;
        static MemorySegment rulerView_didMoveMarker;
        static MemorySegment rulerView_didRemoveMarker;
        static MemorySegment rulerView_didAddMarker;
        static MemorySegment rulerView_shouldMoveMarker;
        static MemorySegment rulerView_shouldAddMarker;
        static MemorySegment rulerView_shouldRemoveMarker;
        static MemorySegment rulerView_handleMouseDown;
        static MemorySegment setNeedsDisplayInRect_avoidAdditionalLayout;
        static MemorySegment shouldDrawInsertionPoint;
        static MemorySegment drawViewBackgroundInRect;
        static MemorySegment updateRuler;
        static MemorySegment updateFontPanel;
        static MemorySegment updateDragTypeRegistration;
        static MemorySegment clickedOnLink_atIndex;
        static MemorySegment startSpeaking;
        static MemorySegment stopSpeaking;
        static MemorySegment setLayoutOrientation;
        static MemorySegment changeLayoutOrientation;
        static MemorySegment stronglyReferencesTextStorage;
        static MemorySegment usesAdaptiveColorMappingForDarkAppearance;
        static MemorySegment setUsesAdaptiveColorMappingForDarkAppearance;
        static MemorySegment complete;
        static MemorySegment rangeForUserCompletion;
        static MemorySegment completionsForPartialWordRange_indexOfSelectedItem;
        static MemorySegment writablePasteboardTypes;
        static MemorySegment readablePasteboardTypes;
        static MemorySegment writeSelectionToPasteboard_type;
        static MemorySegment writeSelectionToPasteboard_types;
        static MemorySegment preferredPasteboardTypeFromArray_restrictedToTypesFromArray;
        static MemorySegment readSelectionFromPasteboard_type;
        static MemorySegment readSelectionFromPasteboard;
        static MemorySegment registerForServices;
        static MemorySegment validRequestorForSendType_returnType;
        static MemorySegment pasteAsPlainText;
        static MemorySegment pasteAsRichText;
        static MemorySegment dragImageForSelectionWithEvent_origin;
        static MemorySegment acceptableDragTypes;
        static MemorySegment dragOperationForDraggingInfo_type;
        static MemorySegment cleanUpAfterDragOperation;
        static MemorySegment selectedRanges;
        static MemorySegment setSelectedRanges;
        static MemorySegment setSelectedRanges_affinity_stillSelecting;
        static MemorySegment setSelectedRange;
        static MemorySegment selectionAffinity;
        static MemorySegment selectionGranularity;
        static MemorySegment setSelectionGranularity;
        static MemorySegment selectedTextAttributes;
        static MemorySegment setSelectedTextAttributes;
        static MemorySegment insertionPointColor;
        static MemorySegment setInsertionPointColor;
        static MemorySegment markedTextAttributes;
        static MemorySegment setMarkedTextAttributes;
        static MemorySegment linkTextAttributes;
        static MemorySegment setLinkTextAttributes;
        static MemorySegment updateInsertionPointStateAndRestartTimer;
        static MemorySegment displaysLinkToolTips;
        static MemorySegment setDisplaysLinkToolTips;
        static MemorySegment acceptsGlyphInfo;
        static MemorySegment setAcceptsGlyphInfo;
        static MemorySegment usesRuler;
        static MemorySegment setUsesRuler;
        static MemorySegment usesInspectorBar;
        static MemorySegment setUsesInspectorBar;
        static MemorySegment isContinuousSpellCheckingEnabled;
        static MemorySegment setContinuousSpellCheckingEnabled;
        static MemorySegment toggleContinuousSpellChecking;
        static MemorySegment spellCheckerDocumentTag;
        static MemorySegment isGrammarCheckingEnabled;
        static MemorySegment setGrammarCheckingEnabled;
        static MemorySegment toggleGrammarChecking;
        static MemorySegment typingAttributes;
        static MemorySegment setTypingAttributes;
        static MemorySegment rangesForUserTextChange;
        static MemorySegment rangesForUserCharacterAttributeChange;
        static MemorySegment rangesForUserParagraphAttributeChange;
        static MemorySegment rangeForUserTextChange;
        static MemorySegment rangeForUserCharacterAttributeChange;
        static MemorySegment rangeForUserParagraphAttributeChange;
        static MemorySegment shouldChangeTextInRanges_replacementStrings;
        static MemorySegment didChangeText;
        static MemorySegment allowsDocumentBackgroundColorChange;
        static MemorySegment setAllowsDocumentBackgroundColorChange;
        static MemorySegment defaultParagraphStyle;
        static MemorySegment setDefaultParagraphStyle;
        static MemorySegment allowsUndo;
        static MemorySegment setAllowsUndo;
        static MemorySegment breakUndoCoalescing;
        static MemorySegment isCoalescingUndo;
        static MemorySegment allowsImageEditing;
        static MemorySegment setAllowsImageEditing;
        static MemorySegment showFindIndicatorForRange;
        static MemorySegment usesRolloverButtonForSelection;
        static MemorySegment setUsesRolloverButtonForSelection;
        static MemorySegment allowedInputSourceLocales;
        static MemorySegment setAllowedInputSourceLocales;
        static MemorySegment smartInsertDeleteEnabled;
        static MemorySegment setSmartInsertDeleteEnabled;
        static MemorySegment smartDeleteRangeForProposedRange;
        static MemorySegment toggleSmartInsertDelete;
        static MemorySegment smartInsertBeforeStringForString_replacingRange;
        static MemorySegment smartInsertAfterStringForString_replacingRange;
        static MemorySegment isAutomaticQuoteSubstitutionEnabled;
        static MemorySegment setAutomaticQuoteSubstitutionEnabled;
        static MemorySegment toggleAutomaticQuoteSubstitution;
        static MemorySegment isAutomaticLinkDetectionEnabled;
        static MemorySegment setAutomaticLinkDetectionEnabled;
        static MemorySegment toggleAutomaticLinkDetection;
        static MemorySegment isAutomaticDataDetectionEnabled;
        static MemorySegment setAutomaticDataDetectionEnabled;
        static MemorySegment toggleAutomaticDataDetection;
        static MemorySegment isAutomaticDashSubstitutionEnabled;
        static MemorySegment setAutomaticDashSubstitutionEnabled;
        static MemorySegment toggleAutomaticDashSubstitution;
        static MemorySegment isAutomaticTextReplacementEnabled;
        static MemorySegment setAutomaticTextReplacementEnabled;
        static MemorySegment toggleAutomaticTextReplacement;
        static MemorySegment isAutomaticSpellingCorrectionEnabled;
        static MemorySegment setAutomaticSpellingCorrectionEnabled;
        static MemorySegment toggleAutomaticSpellingCorrection;
        static MemorySegment enabledTextCheckingTypes;
        static MemorySegment setEnabledTextCheckingTypes;
        static MemorySegment orderFrontSubstitutionsPanel;
        static MemorySegment checkTextInSelection;
        static MemorySegment checkTextInDocument;
        static MemorySegment usesFindPanel;
        static MemorySegment setUsesFindPanel;
        static MemorySegment usesFindBar;
        static MemorySegment setUsesFindBar;
        static MemorySegment isIncrementalSearchingEnabled;
        static MemorySegment setIncrementalSearchingEnabled;
        static MemorySegment toggleQuickLookPreviewPanel;
        static MemorySegment quickLookPreviewableItemsInRanges;
        static MemorySegment updateQuickLookPreviewPanel;
        static MemorySegment orderFrontSharingServicePicker;
        static MemorySegment isAutomaticTextCompletionEnabled;
        static MemorySegment setAutomaticTextCompletionEnabled;
        static MemorySegment toggleAutomaticTextCompletion;
        static MemorySegment allowsCharacterPickerTouchBarItem;
        static MemorySegment setAllowsCharacterPickerTouchBarItem;
        static MemorySegment updateTouchBarItemIdentifiers;
        static MemorySegment updateTextTouchBarItems;
        static MemorySegment updateCandidates;
        static MemorySegment candidateListTouchBarItem;
        static MemorySegment scrollableTextView;
        static MemorySegment fieldEditor;
        static MemorySegment scrollableDocumentContentTextView;
        static MemorySegment scrollablePlainDocumentContentTextView;
        static MemorySegment isWritingToolsActive;
        static MemorySegment writingToolsBehavior;
        static MemorySegment setWritingToolsBehavior;
        static MemorySegment allowedWritingToolsResultOptions;
        static MemorySegment setAllowedWritingToolsResultOptions;
        static void populate() {
            usesFontPanel = ObjC.sel("usesFontPanel");
            setUsesFontPanel = ObjC.sel("setUsesFontPanel:");
            textStorage = ObjC.sel("textStorage");
            setAttributedString = ObjC.sel("setAttributedString:");
            attributedString = ObjC.sel("attributedString");
            layoutManager = ObjC.sel("layoutManager");
            textContainer = ObjC.sel("textContainer");
            replaceTextContainer = ObjC.sel("replaceTextContainer:");
            setTextContainer = ObjC.sel("setTextContainer:");
            textContainerInset = ObjC.sel("textContainerInset");
            setTextContainerInset = ObjC.sel("setTextContainerInset:");
            textContainerOrigin = ObjC.sel("textContainerOrigin");
            invalidateTextContainerOrigin = ObjC.sel("invalidateTextContainerOrigin");
            textLayoutManager = ObjC.sel("textLayoutManager");
            textContentStorage = ObjC.sel("textContentStorage");
            setConstrainedFrameSize = ObjC.sel("setConstrainedFrameSize:");
            turnOffKerning = ObjC.sel("turnOffKerning:");
            tightenKerning = ObjC.sel("tightenKerning:");
            loosenKerning = ObjC.sel("loosenKerning:");
            useStandardKerning = ObjC.sel("useStandardKerning:");
            turnOffLigatures = ObjC.sel("turnOffLigatures:");
            useStandardLigatures = ObjC.sel("useStandardLigatures:");
            useAllLigatures = ObjC.sel("useAllLigatures:");
            raiseBaseline = ObjC.sel("raiseBaseline:");
            lowerBaseline = ObjC.sel("lowerBaseline:");
            outline = ObjC.sel("outline:");
            performFindPanelAction = ObjC.sel("performFindPanelAction:");
            alignJustified = ObjC.sel("alignJustified:");
            changeColor = ObjC.sel("changeColor:");
            changeAttributes = ObjC.sel("changeAttributes:");
            changeDocumentBackgroundColor = ObjC.sel("changeDocumentBackgroundColor:");
            orderFrontSpacingPanel = ObjC.sel("orderFrontSpacingPanel:");
            orderFrontLinkPanel = ObjC.sel("orderFrontLinkPanel:");
            orderFrontListPanel = ObjC.sel("orderFrontListPanel:");
            orderFrontTablePanel = ObjC.sel("orderFrontTablePanel:");
            rulerView_didMoveMarker = ObjC.sel("rulerView:didMoveMarker:");
            rulerView_didRemoveMarker = ObjC.sel("rulerView:didRemoveMarker:");
            rulerView_didAddMarker = ObjC.sel("rulerView:didAddMarker:");
            rulerView_shouldMoveMarker = ObjC.sel("rulerView:shouldMoveMarker:");
            rulerView_shouldAddMarker = ObjC.sel("rulerView:shouldAddMarker:");
            rulerView_shouldRemoveMarker = ObjC.sel("rulerView:shouldRemoveMarker:");
            rulerView_handleMouseDown = ObjC.sel("rulerView:handleMouseDown:");
            setNeedsDisplayInRect_avoidAdditionalLayout = ObjC.sel("setNeedsDisplayInRect:avoidAdditionalLayout:");
            shouldDrawInsertionPoint = ObjC.sel("shouldDrawInsertionPoint");
            drawViewBackgroundInRect = ObjC.sel("drawViewBackgroundInRect:");
            updateRuler = ObjC.sel("updateRuler");
            updateFontPanel = ObjC.sel("updateFontPanel");
            updateDragTypeRegistration = ObjC.sel("updateDragTypeRegistration");
            clickedOnLink_atIndex = ObjC.sel("clickedOnLink:atIndex:");
            startSpeaking = ObjC.sel("startSpeaking:");
            stopSpeaking = ObjC.sel("stopSpeaking:");
            setLayoutOrientation = ObjC.sel("setLayoutOrientation:");
            changeLayoutOrientation = ObjC.sel("changeLayoutOrientation:");
            stronglyReferencesTextStorage = ObjC.sel("stronglyReferencesTextStorage");
            usesAdaptiveColorMappingForDarkAppearance = ObjC.sel("usesAdaptiveColorMappingForDarkAppearance");
            setUsesAdaptiveColorMappingForDarkAppearance = ObjC.sel("setUsesAdaptiveColorMappingForDarkAppearance:");
            complete = ObjC.sel("complete:");
            rangeForUserCompletion = ObjC.sel("rangeForUserCompletion");
            completionsForPartialWordRange_indexOfSelectedItem = ObjC.sel("completionsForPartialWordRange:indexOfSelectedItem:");
            writablePasteboardTypes = ObjC.sel("writablePasteboardTypes");
            readablePasteboardTypes = ObjC.sel("readablePasteboardTypes");
            writeSelectionToPasteboard_type = ObjC.sel("writeSelectionToPasteboard:type:");
            writeSelectionToPasteboard_types = ObjC.sel("writeSelectionToPasteboard:types:");
            preferredPasteboardTypeFromArray_restrictedToTypesFromArray = ObjC.sel("preferredPasteboardTypeFromArray:restrictedToTypesFromArray:");
            readSelectionFromPasteboard_type = ObjC.sel("readSelectionFromPasteboard:type:");
            readSelectionFromPasteboard = ObjC.sel("readSelectionFromPasteboard:");
            registerForServices = ObjC.sel("registerForServices");
            validRequestorForSendType_returnType = ObjC.sel("validRequestorForSendType:returnType:");
            pasteAsPlainText = ObjC.sel("pasteAsPlainText:");
            pasteAsRichText = ObjC.sel("pasteAsRichText:");
            dragImageForSelectionWithEvent_origin = ObjC.sel("dragImageForSelectionWithEvent:origin:");
            acceptableDragTypes = ObjC.sel("acceptableDragTypes");
            dragOperationForDraggingInfo_type = ObjC.sel("dragOperationForDraggingInfo:type:");
            cleanUpAfterDragOperation = ObjC.sel("cleanUpAfterDragOperation");
            selectedRanges = ObjC.sel("selectedRanges");
            setSelectedRanges = ObjC.sel("setSelectedRanges:");
            setSelectedRanges_affinity_stillSelecting = ObjC.sel("setSelectedRanges:affinity:stillSelecting:");
            setSelectedRange = ObjC.sel("setSelectedRange:");
            selectionAffinity = ObjC.sel("selectionAffinity");
            selectionGranularity = ObjC.sel("selectionGranularity");
            setSelectionGranularity = ObjC.sel("setSelectionGranularity:");
            selectedTextAttributes = ObjC.sel("selectedTextAttributes");
            setSelectedTextAttributes = ObjC.sel("setSelectedTextAttributes:");
            insertionPointColor = ObjC.sel("insertionPointColor");
            setInsertionPointColor = ObjC.sel("setInsertionPointColor:");
            markedTextAttributes = ObjC.sel("markedTextAttributes");
            setMarkedTextAttributes = ObjC.sel("setMarkedTextAttributes:");
            linkTextAttributes = ObjC.sel("linkTextAttributes");
            setLinkTextAttributes = ObjC.sel("setLinkTextAttributes:");
            updateInsertionPointStateAndRestartTimer = ObjC.sel("updateInsertionPointStateAndRestartTimer:");
            displaysLinkToolTips = ObjC.sel("displaysLinkToolTips");
            setDisplaysLinkToolTips = ObjC.sel("setDisplaysLinkToolTips:");
            acceptsGlyphInfo = ObjC.sel("acceptsGlyphInfo");
            setAcceptsGlyphInfo = ObjC.sel("setAcceptsGlyphInfo:");
            usesRuler = ObjC.sel("usesRuler");
            setUsesRuler = ObjC.sel("setUsesRuler:");
            usesInspectorBar = ObjC.sel("usesInspectorBar");
            setUsesInspectorBar = ObjC.sel("setUsesInspectorBar:");
            isContinuousSpellCheckingEnabled = ObjC.sel("isContinuousSpellCheckingEnabled");
            setContinuousSpellCheckingEnabled = ObjC.sel("setContinuousSpellCheckingEnabled:");
            toggleContinuousSpellChecking = ObjC.sel("toggleContinuousSpellChecking:");
            spellCheckerDocumentTag = ObjC.sel("spellCheckerDocumentTag");
            isGrammarCheckingEnabled = ObjC.sel("isGrammarCheckingEnabled");
            setGrammarCheckingEnabled = ObjC.sel("setGrammarCheckingEnabled:");
            toggleGrammarChecking = ObjC.sel("toggleGrammarChecking:");
            typingAttributes = ObjC.sel("typingAttributes");
            setTypingAttributes = ObjC.sel("setTypingAttributes:");
            rangesForUserTextChange = ObjC.sel("rangesForUserTextChange");
            rangesForUserCharacterAttributeChange = ObjC.sel("rangesForUserCharacterAttributeChange");
            rangesForUserParagraphAttributeChange = ObjC.sel("rangesForUserParagraphAttributeChange");
            rangeForUserTextChange = ObjC.sel("rangeForUserTextChange");
            rangeForUserCharacterAttributeChange = ObjC.sel("rangeForUserCharacterAttributeChange");
            rangeForUserParagraphAttributeChange = ObjC.sel("rangeForUserParagraphAttributeChange");
            shouldChangeTextInRanges_replacementStrings = ObjC.sel("shouldChangeTextInRanges:replacementStrings:");
            didChangeText = ObjC.sel("didChangeText");
            allowsDocumentBackgroundColorChange = ObjC.sel("allowsDocumentBackgroundColorChange");
            setAllowsDocumentBackgroundColorChange = ObjC.sel("setAllowsDocumentBackgroundColorChange:");
            defaultParagraphStyle = ObjC.sel("defaultParagraphStyle");
            setDefaultParagraphStyle = ObjC.sel("setDefaultParagraphStyle:");
            allowsUndo = ObjC.sel("allowsUndo");
            setAllowsUndo = ObjC.sel("setAllowsUndo:");
            breakUndoCoalescing = ObjC.sel("breakUndoCoalescing");
            isCoalescingUndo = ObjC.sel("isCoalescingUndo");
            allowsImageEditing = ObjC.sel("allowsImageEditing");
            setAllowsImageEditing = ObjC.sel("setAllowsImageEditing:");
            showFindIndicatorForRange = ObjC.sel("showFindIndicatorForRange:");
            usesRolloverButtonForSelection = ObjC.sel("usesRolloverButtonForSelection");
            setUsesRolloverButtonForSelection = ObjC.sel("setUsesRolloverButtonForSelection:");
            allowedInputSourceLocales = ObjC.sel("allowedInputSourceLocales");
            setAllowedInputSourceLocales = ObjC.sel("setAllowedInputSourceLocales:");
            smartInsertDeleteEnabled = ObjC.sel("smartInsertDeleteEnabled");
            setSmartInsertDeleteEnabled = ObjC.sel("setSmartInsertDeleteEnabled:");
            smartDeleteRangeForProposedRange = ObjC.sel("smartDeleteRangeForProposedRange:");
            toggleSmartInsertDelete = ObjC.sel("toggleSmartInsertDelete:");
            smartInsertBeforeStringForString_replacingRange = ObjC.sel("smartInsertBeforeStringForString:replacingRange:");
            smartInsertAfterStringForString_replacingRange = ObjC.sel("smartInsertAfterStringForString:replacingRange:");
            isAutomaticQuoteSubstitutionEnabled = ObjC.sel("isAutomaticQuoteSubstitutionEnabled");
            setAutomaticQuoteSubstitutionEnabled = ObjC.sel("setAutomaticQuoteSubstitutionEnabled:");
            toggleAutomaticQuoteSubstitution = ObjC.sel("toggleAutomaticQuoteSubstitution:");
            isAutomaticLinkDetectionEnabled = ObjC.sel("isAutomaticLinkDetectionEnabled");
            setAutomaticLinkDetectionEnabled = ObjC.sel("setAutomaticLinkDetectionEnabled:");
            toggleAutomaticLinkDetection = ObjC.sel("toggleAutomaticLinkDetection:");
            isAutomaticDataDetectionEnabled = ObjC.sel("isAutomaticDataDetectionEnabled");
            setAutomaticDataDetectionEnabled = ObjC.sel("setAutomaticDataDetectionEnabled:");
            toggleAutomaticDataDetection = ObjC.sel("toggleAutomaticDataDetection:");
            isAutomaticDashSubstitutionEnabled = ObjC.sel("isAutomaticDashSubstitutionEnabled");
            setAutomaticDashSubstitutionEnabled = ObjC.sel("setAutomaticDashSubstitutionEnabled:");
            toggleAutomaticDashSubstitution = ObjC.sel("toggleAutomaticDashSubstitution:");
            isAutomaticTextReplacementEnabled = ObjC.sel("isAutomaticTextReplacementEnabled");
            setAutomaticTextReplacementEnabled = ObjC.sel("setAutomaticTextReplacementEnabled:");
            toggleAutomaticTextReplacement = ObjC.sel("toggleAutomaticTextReplacement:");
            isAutomaticSpellingCorrectionEnabled = ObjC.sel("isAutomaticSpellingCorrectionEnabled");
            setAutomaticSpellingCorrectionEnabled = ObjC.sel("setAutomaticSpellingCorrectionEnabled:");
            toggleAutomaticSpellingCorrection = ObjC.sel("toggleAutomaticSpellingCorrection:");
            enabledTextCheckingTypes = ObjC.sel("enabledTextCheckingTypes");
            setEnabledTextCheckingTypes = ObjC.sel("setEnabledTextCheckingTypes:");
            orderFrontSubstitutionsPanel = ObjC.sel("orderFrontSubstitutionsPanel:");
            checkTextInSelection = ObjC.sel("checkTextInSelection:");
            checkTextInDocument = ObjC.sel("checkTextInDocument:");
            usesFindPanel = ObjC.sel("usesFindPanel");
            setUsesFindPanel = ObjC.sel("setUsesFindPanel:");
            usesFindBar = ObjC.sel("usesFindBar");
            setUsesFindBar = ObjC.sel("setUsesFindBar:");
            isIncrementalSearchingEnabled = ObjC.sel("isIncrementalSearchingEnabled");
            setIncrementalSearchingEnabled = ObjC.sel("setIncrementalSearchingEnabled:");
            toggleQuickLookPreviewPanel = ObjC.sel("toggleQuickLookPreviewPanel:");
            quickLookPreviewableItemsInRanges = ObjC.sel("quickLookPreviewableItemsInRanges:");
            updateQuickLookPreviewPanel = ObjC.sel("updateQuickLookPreviewPanel");
            orderFrontSharingServicePicker = ObjC.sel("orderFrontSharingServicePicker:");
            isAutomaticTextCompletionEnabled = ObjC.sel("isAutomaticTextCompletionEnabled");
            setAutomaticTextCompletionEnabled = ObjC.sel("setAutomaticTextCompletionEnabled:");
            toggleAutomaticTextCompletion = ObjC.sel("toggleAutomaticTextCompletion:");
            allowsCharacterPickerTouchBarItem = ObjC.sel("allowsCharacterPickerTouchBarItem");
            setAllowsCharacterPickerTouchBarItem = ObjC.sel("setAllowsCharacterPickerTouchBarItem:");
            updateTouchBarItemIdentifiers = ObjC.sel("updateTouchBarItemIdentifiers");
            updateTextTouchBarItems = ObjC.sel("updateTextTouchBarItems");
            updateCandidates = ObjC.sel("updateCandidates");
            candidateListTouchBarItem = ObjC.sel("candidateListTouchBarItem");
            scrollableTextView = ObjC.sel("scrollableTextView");
            fieldEditor = ObjC.sel("fieldEditor");
            scrollableDocumentContentTextView = ObjC.sel("scrollableDocumentContentTextView");
            scrollablePlainDocumentContentTextView = ObjC.sel("scrollablePlainDocumentContentTextView");
            isWritingToolsActive = ObjC.sel("isWritingToolsActive");
            writingToolsBehavior = ObjC.sel("writingToolsBehavior");
            setWritingToolsBehavior = ObjC.sel("setWritingToolsBehavior:");
            allowedWritingToolsResultOptions = ObjC.sel("allowedWritingToolsResultOptions");
            setAllowedWritingToolsResultOptions = ObjC.sel("setAllowedWritingToolsResultOptions:");
        }
    }

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id

    private NSTextView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTextView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTextView(peer);
    }

    private static void ensureInit() {
        if (initialized) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
            Sels.populate();
        initialized = true;
}

    /// `[[NSTextView alloc] initWithFrame:frame]` — a new text view at the given rect.
        public static NSTextView create(NSRect frame) {
        ensureInit();
        return new NSTextView(ObjC.newView("NSTextView", frame));
    }

    // ---------------------------------------------------------------- string (re-expose for discoverability)

    @Override
    public String string() { return super.string(); }

    @Override
    public void setString(String s) { super.setString(s); }

    // ---- rich text / graphics (inherited from NSText, re-expose) ----

    @Override
    public boolean isRichText() { return super.isRichText(); }

    @Override
    public void setRichText(boolean flag) { super.setRichText(flag); }

    @Override
    public boolean importsGraphics() { return super.importsGraphics(); }

    @Override
    public void setImportsGraphics(boolean flag) { super.setImportsGraphics(flag); }

    // ---- NSTextView-specific ----

    /// [textView usesFontPanel] — whether the font panel is used.
    public boolean usesFontPanel() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesFontPanel);
    }

    /// [textView setUsesFontPanel:] — enable/disable the font panel.
    public void setUsesFontPanel(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setUsesFontPanel, flag);
    }

    // ---- editable / selectable (inherited, re-expose) ----

    @Override
    public boolean isEditable() { return super.isEditable(); }

    @Override
    public void setEditable(boolean flag) { super.setEditable(flag); }

    @Override
    public boolean isSelectable() { return super.isSelectable(); }

    @Override
    public void setSelectable(boolean flag) { super.setSelectable(flag); }

    // ---- font / colors (inherited, re-expose) ----

    @Override
    public NSFont font() { return super.font(); }

    @Override
    public void setFont(NSFont font) { super.setFont(font); }

    @Override
    public NSColor textColor() { return super.textColor(); }

    @Override
    public void setTextColor(NSColor color) { super.setTextColor(color); }

    @Override
    public NSColor backgroundColor() { return super.backgroundColor(); }

    @Override
    public void setBackgroundColor(NSColor color) { super.setBackgroundColor(color); }

    // ---- typed NSAttributedString support ----
    @Override
    public NSAttributedString attributedString() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(peer, Sels.textStorage);
        if (p != null && p.address() != 0) return NSAttributedString.wrap(p);
        return super.attributedString();
    }
    @Override
    public void setAttributedString(NSAttributedString s) {
        ensureInit();
        // NSTextView has no setAttributedString: — use its textStorage (NSTextStorage is a NSMutableAttributedString)
        MemorySegment storage = ObjC.msgSendId(peer, Sels.textStorage);
        if (storage != null && storage.address() != 0) {
            ObjC.msgSendVoidId(storage, Sels.setAttributedString, (MemorySegment) (s == null ? MemorySegment.NULL : s.peer()));
            return;
        }
        super.setAttributedString(s);
    }

    public NSAttributedString attributedStringValueTyped() {
        ensureInit();
        return NSAttributedString.wrap(ObjC.msgSendId(peer, Sels.attributedString));
    }
    public void setAttributedStringValue(NSAttributedString value) {
        // NSTextView implements neither setAttributedStringValue: nor setAttributedString:
        // (verified against the runtime) — both route through its textStorage.
        setAttributedString(value);
    }

    /// [textView textStorage] -> NSTextStorage (NSMutableAttributedString)
    public NSMutableAttributedString textStorage() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(peer, Sels.textStorage);
        return NSMutableAttributedString.wrap(p);
    }

    /// [textView setTextColor:range:] convenience via textStorage
    public void setTextColor(NSColor color, NSRange range) {
        NSMutableAttributedString ts = textStorage();
        if (ts != null) {
            ts.addAttribute("NSForegroundColorAttributeName", (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()), range);
        }
    }

    // ---- NSLayoutManager trio (minimal) ----

    /// [textView layoutManager] -> NSLayoutManager (may be nil).
    public NSLayoutManager layoutManager() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(peer, Sels.layoutManager);
        return NSLayoutManager.wrap(p);
    }

    /// [textView textContainer] -> NSTextContainer (may be nil).
    public NSTextContainer textContainer() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(peer, Sels.textContainer);
        return NSTextContainer.wrap(p);
    }

    /// [textView textStorage] as NSTextStorage (typed).
    public NSTextStorage textStorageAsStorage() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(peer, Sels.textStorage);
        return NSTextStorage.wrap(p);
    }

    /// Wire a full trio manually: storage -> layoutManager -> container -> textView.
    /// Minimal helper — callers that need a custom trio can use this instead of relying
    /// on the default NSTextView initialization.
    public void replaceTextContainer(NSTextContainer container) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.replaceTextContainer, (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
    }

    /// [textView setTextContainer:] — the primitive; prefer replaceTextContainer: for web-safe swaps.
    public void setTextContainer(NSTextContainer container) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setTextContainer,
                (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
    }

    /// [textView textContainerInset] -> NSSize (padding around the container).
    public NSSize textContainerInset() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.SIZE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, Sels.textContainerInset);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("textContainerInset failed", t);
        }
    }

    /// [textView setTextContainerInset:]
    public void setTextContainerInset(NSSize inset) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
            h.invokeExact(peer, Sels.setTextContainerInset, inset.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setTextContainerInset: failed", t);
        }
    }

    /// [textView textContainerOrigin] -> NSPoint (readonly).
    public NSPoint textContainerOrigin() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.POINT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, Sels.textContainerOrigin);
            return NSPoint.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("textContainerOrigin failed", t);
        }
    }

    /// [textView invalidateTextContainerOrigin]
    public void invalidateTextContainerOrigin() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.invalidateTextContainerOrigin);
    }

    /// [textView textLayoutManager] -> NSTextLayoutManager* (TextKit 2; raw, may be nil).
    public MemorySegment textLayoutManager() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.textLayoutManager);
    }

    /// [textView textContentStorage] -> NSTextContentStorage* (TextKit 2; raw, may be nil).
    public MemorySegment textContentStorage() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.textContentStorage);
    }

    // -initWithFrame:textContainer: omitted: (RECT,ID)->ID has no vocabulary entry
    // (grep MISS) — reported. -initUsingTextLayoutManager: / +textViewUsingTextLayoutManager:
    // omitted: (BOOL)->ID has no entry (grep MISS) — reported.

    /// [textView setConstrainedFrameSize:] — size within min/max constraints.
    public void setConstrainedFrameSize(NSSize desiredSize) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
            h.invokeExact(peer, Sels.setConstrainedFrameSize, desiredSize.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setConstrainedFrameSize: failed", t);
        }
    }

    // -setAlignment:range: / -setBaseWritingDirection:range: omitted: (INT,RANGE)->VOID
    // has no vocabulary entry (grep MISS) — reported.

    // ---- font menu commands (all take a nullable sender id) ----

    /// [textView turnOffKerning:]
    public void turnOffKerning(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.turnOffKerning, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView tightenKerning:]
    public void tightenKerning(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.tightenKerning, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView loosenKerning:]
    public void loosenKerning(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.loosenKerning, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView useStandardKerning:]
    public void useStandardKerning(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.useStandardKerning, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView turnOffLigatures:]
    public void turnOffLigatures(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.turnOffLigatures, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView useStandardLigatures:]
    public void useStandardLigatures(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.useStandardLigatures, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView useAllLigatures:]
    public void useAllLigatures(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.useAllLigatures, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView raiseBaseline:]
    public void raiseBaseline(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.raiseBaseline, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView lowerBaseline:]
    public void lowerBaseline(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.lowerBaseline, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView outline:]
    public void outline(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.outline, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // -toggleTraditionalCharacterShape: skipped as deprecated.

    /// [textView performFindPanelAction:] — sender tag is an NSFindPanelAction.
    public void performFindPanelAction(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.performFindPanelAction, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- text commands ----

    /// [textView alignJustified:]
    public void alignJustified(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.alignJustified, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView changeColor:]
    public void changeColor(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.changeColor, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView changeAttributes:]
    public void changeAttributes(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.changeAttributes, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView changeDocumentBackgroundColor:]
    public void changeDocumentBackgroundColor(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.changeDocumentBackgroundColor, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView orderFrontSpacingPanel:]
    public void orderFrontSpacingPanel(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.orderFrontSpacingPanel, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView orderFrontLinkPanel:]
    public void orderFrontLinkPanel(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.orderFrontLinkPanel, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView orderFrontListPanel:]
    public void orderFrontListPanel(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.orderFrontListPanel, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView orderFrontTablePanel:]
    public void orderFrontTablePanel(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.orderFrontTablePanel, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- ruler callbacks ----

    /// [textView rulerView:didMoveMarker:] — ruler/marker are NSRulerView*/NSRulerMarker*.
    public void rulerDidMoveMarker(MemorySegment ruler, MemorySegment marker) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, Sels.rulerView_didMoveMarker,
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:didMoveMarker: failed", t);
        }
    }

    /// [textView rulerView:didRemoveMarker:]
    public void rulerDidRemoveMarker(MemorySegment ruler, MemorySegment marker) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, Sels.rulerView_didRemoveMarker,
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:didRemoveMarker: failed", t);
        }
    }

    /// [textView rulerView:didAddMarker:]
    public void rulerDidAddMarker(MemorySegment ruler, MemorySegment marker) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, Sels.rulerView_didAddMarker,
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:didAddMarker: failed", t);
        }
    }

    /// [textView rulerView:shouldMoveMarker:] — ruler/marker raw segments.
    public boolean rulerShouldMoveMarker(MemorySegment ruler, MemorySegment marker) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, Sels.rulerView_shouldMoveMarker,
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:shouldMoveMarker: failed", t);
        }
    }

    /// [textView rulerView:shouldAddMarker:]
    public boolean rulerShouldAddMarker(MemorySegment ruler, MemorySegment marker) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, Sels.rulerView_shouldAddMarker,
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:shouldAddMarker: failed", t);
        }
    }

    /// [textView rulerView:shouldRemoveMarker:]
    public boolean rulerShouldRemoveMarker(MemorySegment ruler, MemorySegment marker) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, Sels.rulerView_shouldRemoveMarker,
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:shouldRemoveMarker: failed", t);
        }
    }

    // -rulerView:willMoveMarker:toLocation: / -rulerView:willAddMarker:atLocation: omitted:
    // (ID,ID,DOUBLE)->DOUBLE has no vocabulary entry (grep pattern has no match) — reported.

    /// [textView rulerView:handleMouseDown:] — event is NSEvent*.
    public void rulerHandleMouseDown(MemorySegment ruler, MemorySegment event) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, Sels.rulerView_handleMouseDown,
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (event == null ? MemorySegment.NULL : event));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:handleMouseDown: failed", t);
        }
    }

    // ---- display control ----

    /// [textView setNeedsDisplayInRect:avoidAdditionalLayout:]
    public void setNeedsDisplayInRectAvoidAdditionalLayout(NSRect rect, boolean flag) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.BOOL));
            h.invokeExact(peer, Sels.setNeedsDisplayInRect_avoidAdditionalLayout, rect.toSegment(), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setNeedsDisplayInRect:avoidAdditionalLayout: failed", t);
        }
    }

    // -drawInsertionPointInRect:color:turnedOn: omitted: (RECT,ID,BOOL)->VOID has no
    // vocabulary entry (grep MISS) — reported.

    /// [textView shouldDrawInsertionPoint]
    public boolean shouldDrawInsertionPoint() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.shouldDrawInsertionPoint);
    }

    /// [textView drawViewBackgroundInRect:] — subclass override point.
    public void drawViewBackgroundInRect(NSRect rect) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
            h.invokeExact(peer, Sels.drawViewBackgroundInRect, rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("drawViewBackgroundInRect: failed", t);
        }
    }

    /// [textView updateRuler]
    public void updateRuler() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.updateRuler);
    }

    /// [textView updateFontPanel]
    public void updateFontPanel() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.updateFontPanel);
    }

    /// [textView updateDragTypeRegistration]
    public void updateDragTypeRegistration() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.updateDragTypeRegistration);
    }

    // -selectionRangeForProposedRange:granularity: omitted: (RANGE,INT)->RANGE has no
    // vocabulary entry (grep MISS for RANGE(RANGE,INT)) — reported.

    /// [textView clickedOnLink:atIndex:] — link is id (NSURL/NSString).
    public void clickedOnLinkAtIndex(MemorySegment link, long charIndex) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, Sels.clickedOnLink_atIndex,
                    (MemorySegment) (link == null ? MemorySegment.NULL : link), charIndex);
        } catch (Throwable t) {
            throw new RuntimeException("clickedOnLink:atIndex: failed", t);
        }
    }

    /// [textView startSpeaking:]
    public void startSpeaking(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.startSpeaking, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView stopSpeaking:]
    public void stopSpeaking(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.stopSpeaking, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView setLayoutOrientation:] — NSTextLayoutOrientation (macOS 10.7+).
    public void setLayoutOrientation(long orientation) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setLayoutOrientation, orientation);
    }

    /// [textView changeLayoutOrientation:] — sender tag is the orientation (macOS 10.7+).
    public void changeLayoutOrientation(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.changeLayoutOrientation, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // -characterIndexForInsertionAtPoint: omitted: (POINT)->INT has no vocabulary entry
    // (grep MISS) — reported. -performValidatedReplacementInRange:withAttributedString:
    // omitted: (RANGE,ID)->BOOL has no entry (grep MISS) — reported.

    /// [textView stronglyReferencesTextStorage] (class property, macOS 10.12+).
    public static boolean stronglyReferencesTextStorage() {
        ensureInit();
        return ObjC.msgSendBool(ObjC.cls("NSTextView"), Sels.stronglyReferencesTextStorage);
    }

    /// [textView usesAdaptiveColorMappingForDarkAppearance] (macOS 10.14+).
    public boolean usesAdaptiveColorMappingForDarkAppearance() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesAdaptiveColorMappingForDarkAppearance);
    }

    /// [textView setUsesAdaptiveColorMappingForDarkAppearance:] (macOS 10.14+).
    public void setUsesAdaptiveColorMappingForDarkAppearance(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setUsesAdaptiveColorMappingForDarkAppearance, flag);
    }

    // ---- completion ----

    /// [textView complete:] — invoke completion programmatically.
    public void complete(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.complete, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView rangeForUserCompletion] -> NSRange (readonly).
    public NSRange rangeForUserCompletion() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, Sels.rangeForUserCompletion);
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeForUserCompletion failed", t);
        }
    }

    /// [textView completionsForPartialWordRange:indexOfSelectedItem:] — indexOut is
    /// NSInteger* (nullable; pass NULL to ignore) -> NSArray of NSString (may be nil).
    public NSArray completionsForPartialWordRange(NSRange charRange, MemorySegment indexOutOrNull) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer,
                    Sels.completionsForPartialWordRange_indexOfSelectedItem,
                    charRange.toSegment(),
                    (MemorySegment) (indexOutOrNull == null ? MemorySegment.NULL : indexOutOrNull));
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("completionsForPartialWordRange:indexOfSelectedItem: failed", t);
        }
    }

    // -insertCompletion:forPartialWordRange:movement:isFinal: omitted:
    // (ID,RANGE,INT,BOOL)->VOID has no vocabulary entry — reported.

    // ---- pasteboard ----

    /// [textView writablePasteboardTypes] -> NSArray.
    public NSArray writablePasteboardTypes() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.writablePasteboardTypes));
    }

    /// [textView readablePasteboardTypes] -> NSArray.
    public NSArray readablePasteboardTypes() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.readablePasteboardTypes));
    }

    /// [textView writeSelectionToPasteboard:type:] — both NSPasteboard*/type (raw segments).
    public boolean writeSelectionToPasteboardType(MemorySegment pboard, MemorySegment type) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, Sels.writeSelectionToPasteboard_type,
                    (MemorySegment) (pboard == null ? MemorySegment.NULL : pboard),
                    (MemorySegment) (type == null ? MemorySegment.NULL : type));
        } catch (Throwable t) {
            throw new RuntimeException("writeSelectionToPasteboard:type: failed", t);
        }
    }

    /// [textView writeSelectionToPasteboard:types:] — types is NSArray*.
    public boolean writeSelectionToPasteboardTypes(MemorySegment pboard, NSArray types) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, Sels.writeSelectionToPasteboard_types,
                    (MemorySegment) (pboard == null ? MemorySegment.NULL : pboard),
                    (MemorySegment) (types == null ? MemorySegment.NULL : types.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("writeSelectionToPasteboard:types: failed", t);
        }
    }

    /// [textView preferredPasteboardTypeFromArray:restrictedToTypesFromArray:] — NSArrays.
    public MemorySegment preferredPasteboardType(NSArray availableTypes, NSArray allowedTypes) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(peer,
                    Sels.preferredPasteboardTypeFromArray_restrictedToTypesFromArray,
                    (MemorySegment) (availableTypes == null ? MemorySegment.NULL : availableTypes.peer()),
                    (MemorySegment) (allowedTypes == null ? MemorySegment.NULL : allowedTypes.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("preferredPasteboardTypeFromArray:restrictedToTypesFromArray: failed", t);
        }
    }

    /// [textView readSelectionFromPasteboard:type:]
    public boolean readSelectionFromPasteboardType(MemorySegment pboard, MemorySegment type) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, Sels.readSelectionFromPasteboard_type,
                    (MemorySegment) (pboard == null ? MemorySegment.NULL : pboard),
                    (MemorySegment) (type == null ? MemorySegment.NULL : type));
        } catch (Throwable t) {
            throw new RuntimeException("readSelectionFromPasteboard:type: failed", t);
        }
    }

    /// [textView readSelectionFromPasteboard:] (services path).
    public boolean readSelectionFromPasteboard(MemorySegment pboard) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, Sels.readSelectionFromPasteboard,
                    (MemorySegment) (pboard == null ? MemorySegment.NULL : pboard));
        } catch (Throwable t) {
            throw new RuntimeException("readSelectionFromPasteboard: failed", t);
        }
    }

    /// `+[NSTextView registerForServices]`
    public static void registerForServices() {
        ensureInit();
        ObjC.msgSendVoid(ObjC.cls("NSTextView"), Sels.registerForServices);
    }

    /// [textView validRequestorForSendType:returnType:] — pasteboard type names (raw segments).
    public MemorySegment validRequestorForSendTypeReturnType(MemorySegment sendType, MemorySegment returnType) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, Sels.validRequestorForSendType_returnType,
                    (MemorySegment) (sendType == null ? MemorySegment.NULL : sendType),
                    (MemorySegment) (returnType == null ? MemorySegment.NULL : returnType));
        } catch (Throwable t) {
            throw new RuntimeException("validRequestorForSendType:returnType: failed", t);
        }
    }

    /// [textView pasteAsPlainText:]
    public void pasteAsPlainText(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.pasteAsPlainText, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView pasteAsRichText:]
    public void pasteAsRichText(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.pasteAsRichText, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- dragging ----

    // -dragSelectionWithEvent:offset:slideBack: omitted: (ID,SIZE,BOOL)->BOOL has no
    // vocabulary entry — reported.

    /// [textView dragImageForSelectionWithEvent:origin:] — origin is NSPoint* (nullable).
    public MemorySegment dragImageForSelectionWithEvent(MemorySegment event, MemorySegment originOutOrNull) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, Sels.dragImageForSelectionWithEvent_origin,
                    (MemorySegment) (event == null ? MemorySegment.NULL : event),
                    (MemorySegment) (originOutOrNull == null ? MemorySegment.NULL : originOutOrNull));
        } catch (Throwable t) {
            throw new RuntimeException("dragImageForSelectionWithEvent:origin: failed", t);
        }
    }

    /// [textView acceptableDragTypes] -> NSArray.
    public NSArray acceptableDragTypes() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.acceptableDragTypes));
    }

    /// [textView dragOperationForDraggingInfo:type:] -> NSDragOperation (long).
    public long dragOperationForDraggingInfoType(MemorySegment dragInfo, MemorySegment type) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID, Arg.ID));
            return (long) h.invokeExact(peer, Sels.dragOperationForDraggingInfo_type,
                    (MemorySegment) (dragInfo == null ? MemorySegment.NULL : dragInfo),
                    (MemorySegment) (type == null ? MemorySegment.NULL : type));
        } catch (Throwable t) {
            throw new RuntimeException("dragOperationForDraggingInfo:type: failed", t);
        }
    }

    /// [textView cleanUpAfterDragOperation]
    public void cleanUpAfterDragOperation() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.cleanUpAfterDragOperation);
    }

    // ---- shared selection state (NSSharing) ----

    /// [textView selectedRanges] -> NSArray of NSValue (rangeValue).
    public NSArray selectedRanges() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.selectedRanges));
    }

    /// [textView setSelectedRanges:] — NSArray of NSValue.
    public void setSelectedRanges(NSArray ranges) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setSelectedRanges,
                (MemorySegment) (ranges == null ? MemorySegment.NULL : ranges.peer()));
    }

    /// [textView setSelectedRanges:affinity:stillSelecting:] — multi-range change.
    public void setSelectedRangesAffinityStillSelecting(NSArray ranges, long affinity, boolean stillSelecting) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT, Arg.BOOL));
            h.invokeExact(peer, Sels.setSelectedRanges_affinity_stillSelecting,
                    (MemorySegment) (ranges == null ? MemorySegment.NULL : ranges.peer()), affinity, stillSelecting);
        } catch (Throwable t) {
            throw new RuntimeException("setSelectedRanges:affinity:stillSelecting: failed", t);
        }
    }

    // -setSelectedRange:affinity:stillSelecting: omitted: (RANGE,INT,BOOL)->VOID has no
    // vocabulary entry — reported.

    /// [textView setSelectedRange:] (single-range NSTextView override).
    public void setSelectedRange(NSRange charRange) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, Sels.setSelectedRange, charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setSelectedRange: failed", t);
        }
    }

    /// [textView selectionAffinity] -> NSSelectionAffinity (long, readonly).
    public long selectionAffinity() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.selectionAffinity);
    }

    /// [textView selectionGranularity] -> NSSelectionGranularity (long).
    public long selectionGranularity() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.selectionGranularity);
    }

    /// [textView setSelectionGranularity:]
    public void setSelectionGranularity(long granularity) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setSelectionGranularity, granularity);
    }

    /// [textView selectedTextAttributes] -> NSDictionary* (raw segment).
    public MemorySegment selectedTextAttributes() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.selectedTextAttributes);
    }

    /// [textView setSelectedTextAttributes:] — NSDictionary* (may be NULL).
    public void setSelectedTextAttributes(MemorySegment attributes) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setSelectedTextAttributes,
                (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
    }

    /// [textView insertionPointColor] -> NSColor.
    public NSColor insertionPointColor() {
        ensureInit();
        return NSColor.wrap(ObjC.msgSendId(peer, Sels.insertionPointColor));
    }

    /// [textView setInsertionPointColor:]
    public void setInsertionPointColor(NSColor color) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setInsertionPointColor,
                (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// [textView markedTextAttributes] -> NSDictionary* (raw segment, may be nil).
    public MemorySegment markedTextAttributes() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.markedTextAttributes);
    }

    /// [textView setMarkedTextAttributes:] — NSDictionary* (may be NULL).
    public void setMarkedTextAttributes(MemorySegment attributes) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setMarkedTextAttributes,
                (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
    }

    /// [textView linkTextAttributes] -> NSDictionary* (raw segment).
    public MemorySegment linkTextAttributes() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.linkTextAttributes);
    }

    /// [textView setLinkTextAttributes:] — NSDictionary* (may be NULL).
    public void setLinkTextAttributes(MemorySegment attributes) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setLinkTextAttributes,
                (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
    }

    /// [textView updateInsertionPointStateAndRestartTimer:]
    public void updateInsertionPointStateAndRestartTimer(boolean restartFlag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.updateInsertionPointStateAndRestartTimer, restartFlag);
    }

    // ---- text conveniences ----

    /// [textView displaysLinkToolTips] (macOS 10.5+).
    public boolean displaysLinkToolTips() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.displaysLinkToolTips);
    }

    /// [textView setDisplaysLinkToolTips:] (macOS 10.5+).
    public void setDisplaysLinkToolTips(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setDisplaysLinkToolTips, flag);
    }

    /// [textView acceptsGlyphInfo]
    public boolean acceptsGlyphInfo() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.acceptsGlyphInfo);
    }

    /// [textView setAcceptsGlyphInfo:]
    public void setAcceptsGlyphInfo(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAcceptsGlyphInfo, flag);
    }

    /// [textView usesRuler]
    public boolean usesRuler() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesRuler);
    }

    /// [textView setUsesRuler:]
    public void setUsesRuler(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setUsesRuler, flag);
    }

    /// [textView usesInspectorBar] (macOS 10.7+).
    public boolean usesInspectorBar() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesInspectorBar);
    }

    /// [textView setUsesInspectorBar:] (macOS 10.7+).
    public void setUsesInspectorBar(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setUsesInspectorBar, flag);
    }

    /// [textView isContinuousSpellCheckingEnabled]
    public boolean isContinuousSpellCheckingEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isContinuousSpellCheckingEnabled);
    }

    /// [textView setContinuousSpellCheckingEnabled:]
    public void setContinuousSpellCheckingEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setContinuousSpellCheckingEnabled, flag);
    }

    /// [textView toggleContinuousSpellChecking:]
    public void toggleContinuousSpellChecking(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleContinuousSpellChecking, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView spellCheckerDocumentTag] -> NSInteger (readonly).
    public long spellCheckerDocumentTag() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.spellCheckerDocumentTag);
    }

    /// [textView isGrammarCheckingEnabled] (macOS 10.5+).
    public boolean isGrammarCheckingEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isGrammarCheckingEnabled);
    }

    /// [textView setGrammarCheckingEnabled:] (macOS 10.5+).
    public void setGrammarCheckingEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setGrammarCheckingEnabled, flag);
    }

    /// [textView toggleGrammarChecking:] (macOS 10.5+).
    public void toggleGrammarChecking(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleGrammarChecking, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // -setSpellingState:range: omitted: (INT,RANGE)->VOID has no vocabulary entry — reported.

    /// [textView typingAttributes] -> NSDictionary* (raw segment).
    public MemorySegment typingAttributes() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.typingAttributes);
    }

    /// [textView setTypingAttributes:] — NSDictionary* (may be NULL).
    public void setTypingAttributes(MemorySegment attributes) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setTypingAttributes,
                (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
    }

    /// [textView rangesForUserTextChange] -> NSArray of NSValue (may be nil).
    public NSArray rangesForUserTextChange() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.rangesForUserTextChange));
    }

    /// [textView rangesForUserCharacterAttributeChange] -> NSArray (may be nil).
    public NSArray rangesForUserCharacterAttributeChange() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.rangesForUserCharacterAttributeChange));
    }

    /// [textView rangesForUserParagraphAttributeChange] -> NSArray (may be nil).
    public NSArray rangesForUserParagraphAttributeChange() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.rangesForUserParagraphAttributeChange));
    }

    /// [textView rangeForUserTextChange] -> NSRange (readonly).
    public NSRange rangeForUserTextChange() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, Sels.rangeForUserTextChange);
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeForUserTextChange failed", t);
        }
    }

    /// [textView rangeForUserCharacterAttributeChange] -> NSRange (readonly).
    public NSRange rangeForUserCharacterAttributeChange() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, Sels.rangeForUserCharacterAttributeChange);
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeForUserCharacterAttributeChange failed", t);
        }
    }

    /// [textView rangeForUserParagraphAttributeChange] -> NSRange (readonly).
    public NSRange rangeForUserParagraphAttributeChange() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, Sels.rangeForUserParagraphAttributeChange);
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeForUserParagraphAttributeChange failed", t);
        }
    }

    /// [textView shouldChangeTextInRanges:replacementStrings:] — validation query.
    public boolean shouldChangeTextInRangesReplacementStrings(NSArray affectedRanges, NSArray replacementStrings) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, Sels.shouldChangeTextInRanges_replacementStrings,
                    (MemorySegment) (affectedRanges == null ? MemorySegment.NULL : affectedRanges.peer()),
                    (MemorySegment) (replacementStrings == null ? MemorySegment.NULL : replacementStrings.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("shouldChangeTextInRanges:replacementStrings: failed", t);
        }
    }

    // -shouldChangeTextInRange:replacementString: omitted: (RANGE,ID)->BOOL has no
    // vocabulary entry (grep MISS) — reported.

    /// [textView didChangeText]
    public void didChangeText() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.didChangeText);
    }

    /// [textView allowsDocumentBackgroundColorChange]
    public boolean allowsDocumentBackgroundColorChange() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsDocumentBackgroundColorChange);
    }

    /// [textView setAllowsDocumentBackgroundColorChange:]
    public void setAllowsDocumentBackgroundColorChange(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAllowsDocumentBackgroundColorChange, flag);
    }

    /// [textView defaultParagraphStyle] -> NSParagraphStyle (may be nil).
    public NSParagraphStyle defaultParagraphStyle() {
        ensureInit();
        return NSParagraphStyle.wrap(ObjC.msgSendId(peer, Sels.defaultParagraphStyle));
    }

    /// [textView setDefaultParagraphStyle:] — may be NULL.
    public void setDefaultParagraphStyle(NSParagraphStyle style) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setDefaultParagraphStyle,
                (MemorySegment) (style == null ? MemorySegment.NULL : style.peer()));
    }

    /// [textView allowsUndo]
    public boolean allowsUndo() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsUndo);
    }

    /// [textView setAllowsUndo:]
    public void setAllowsUndo(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAllowsUndo, flag);
    }

    /// [textView breakUndoCoalescing]
    public void breakUndoCoalescing() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.breakUndoCoalescing);
    }

    /// [textView isCoalescingUndo] (readonly, macOS 10.6+).
    public boolean isCoalescingUndo() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isCoalescingUndo);
    }

    /// [textView allowsImageEditing] (macOS 10.5+).
    public boolean allowsImageEditing() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsImageEditing);
    }

    /// [textView setAllowsImageEditing:] (macOS 10.5+).
    public void setAllowsImageEditing(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAllowsImageEditing, flag);
    }

    /// [textView showFindIndicatorForRange:] (macOS 10.5+).
    public void showFindIndicatorForRange(NSRange charRange) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, Sels.showFindIndicatorForRange, charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("showFindIndicatorForRange: failed", t);
        }
    }

    /// [textView usesRolloverButtonForSelection] (macOS 10.10+).
    public boolean usesRolloverButtonForSelection() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesRolloverButtonForSelection);
    }

    /// [textView setUsesRolloverButtonForSelection:] (macOS 10.10+).
    public void setUsesRolloverButtonForSelection(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setUsesRolloverButtonForSelection, flag);
    }

    /// [textView allowedInputSourceLocales] -> NSArray of NSString (may be nil, macOS 10.5+).
    public NSArray allowedInputSourceLocales() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.allowedInputSourceLocales));
    }

    /// [textView setAllowedInputSourceLocales:] — NSArray (may be NULL, macOS 10.5+).
    public void setAllowedInputSourceLocales(NSArray locales) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setAllowedInputSourceLocales,
                (MemorySegment) (locales == null ? MemorySegment.NULL : locales.peer()));
    }

    // ---- smart insert / substitution ----

    /// [textView smartInsertDeleteEnabled]
    public boolean smartInsertDeleteEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.smartInsertDeleteEnabled);
    }

    /// [textView setSmartInsertDeleteEnabled:]
    public void setSmartInsertDeleteEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setSmartInsertDeleteEnabled, flag);
    }

    /// [textView smartDeleteRangeForProposedRange:] -> NSRange.
    public NSRange smartDeleteRangeForProposedRange(NSRange proposedCharRange) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer,
                    Sels.smartDeleteRangeForProposedRange, proposedCharRange.toSegment());
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("smartDeleteRangeForProposedRange: failed", t);
        }
    }

    /// [textView toggleSmartInsertDelete:]
    public void toggleSmartInsertDelete(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleSmartInsertDelete, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // -smartInsertForString:... (out-pointers) omitted — reported.

    /// [textView smartInsertBeforeStringForString:replacingRange:] — may return nil.
    public String smartInsertBeforeStringForStringReplacingRange(String pasteString, NSRange charRangeToReplace) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(peer,
                    Sels.smartInsertBeforeStringForString_replacingRange,
                    ObjC.nsstring(pasteString == null ? "" : pasteString), charRangeToReplace.toSegment());
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("smartInsertBeforeStringForString:replacingRange: failed", t);
        }
    }

    /// [textView smartInsertAfterStringForString:replacingRange:] — may return nil.
    public String smartInsertAfterStringForStringReplacingRange(String pasteString, NSRange charRangeToReplace) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(peer,
                    Sels.smartInsertAfterStringForString_replacingRange,
                    ObjC.nsstring(pasteString == null ? "" : pasteString), charRangeToReplace.toSegment());
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("smartInsertAfterStringForString:replacingRange: failed", t);
        }
    }

    /// [textView isAutomaticQuoteSubstitutionEnabled] (macOS 10.5+).
    public boolean isAutomaticQuoteSubstitutionEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isAutomaticQuoteSubstitutionEnabled);
    }

    /// [textView setAutomaticQuoteSubstitutionEnabled:] (macOS 10.5+).
    public void setAutomaticQuoteSubstitutionEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutomaticQuoteSubstitutionEnabled, flag);
    }

    /// [textView toggleAutomaticQuoteSubstitution:] (macOS 10.5+).
    public void toggleAutomaticQuoteSubstitution(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleAutomaticQuoteSubstitution, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticLinkDetectionEnabled] (macOS 10.5+).
    public boolean isAutomaticLinkDetectionEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isAutomaticLinkDetectionEnabled);
    }

    /// [textView setAutomaticLinkDetectionEnabled:] (macOS 10.5+).
    public void setAutomaticLinkDetectionEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutomaticLinkDetectionEnabled, flag);
    }

    /// [textView toggleAutomaticLinkDetection:] (macOS 10.5+).
    public void toggleAutomaticLinkDetection(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleAutomaticLinkDetection, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticDataDetectionEnabled] (macOS 10.6+).
    public boolean isAutomaticDataDetectionEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isAutomaticDataDetectionEnabled);
    }

    /// [textView setAutomaticDataDetectionEnabled:] (macOS 10.6+).
    public void setAutomaticDataDetectionEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutomaticDataDetectionEnabled, flag);
    }

    /// [textView toggleAutomaticDataDetection:] (macOS 10.6+).
    public void toggleAutomaticDataDetection(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleAutomaticDataDetection, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticDashSubstitutionEnabled] (macOS 10.6+).
    public boolean isAutomaticDashSubstitutionEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isAutomaticDashSubstitutionEnabled);
    }

    /// [textView setAutomaticDashSubstitutionEnabled:] (macOS 10.6+).
    public void setAutomaticDashSubstitutionEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutomaticDashSubstitutionEnabled, flag);
    }

    /// [textView toggleAutomaticDashSubstitution:] (macOS 10.6+).
    public void toggleAutomaticDashSubstitution(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleAutomaticDashSubstitution, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticTextReplacementEnabled] (macOS 10.6+).
    public boolean isAutomaticTextReplacementEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isAutomaticTextReplacementEnabled);
    }

    /// [textView setAutomaticTextReplacementEnabled:] (macOS 10.6+).
    public void setAutomaticTextReplacementEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutomaticTextReplacementEnabled, flag);
    }

    /// [textView toggleAutomaticTextReplacement:] (macOS 10.6+).
    public void toggleAutomaticTextReplacement(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleAutomaticTextReplacement, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticSpellingCorrectionEnabled] (macOS 10.6+).
    public boolean isAutomaticSpellingCorrectionEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isAutomaticSpellingCorrectionEnabled);
    }

    /// [textView setAutomaticSpellingCorrectionEnabled:] (macOS 10.6+).
    public void setAutomaticSpellingCorrectionEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutomaticSpellingCorrectionEnabled, flag);
    }

    /// [textView toggleAutomaticSpellingCorrection:] (macOS 10.6+).
    public void toggleAutomaticSpellingCorrection(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleAutomaticSpellingCorrection, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView enabledTextCheckingTypes] -> NSTextCheckingTypes (long, macOS 10.6+).
    public long enabledTextCheckingTypes() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.enabledTextCheckingTypes);
    }

    /// [textView setEnabledTextCheckingTypes:] (macOS 10.6+).
    public void setEnabledTextCheckingTypes(long types) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setEnabledTextCheckingTypes, types);
    }

    // -checkTextInRange:types:options: / -handleTextCheckingResults:... omitted (no shape) — reported.

    /// [textView orderFrontSubstitutionsPanel:] (macOS 10.6+).
    public void orderFrontSubstitutionsPanel(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.orderFrontSubstitutionsPanel, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView checkTextInSelection:] (macOS 10.6+).
    public void checkTextInSelection(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.checkTextInSelection, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView checkTextInDocument:] (macOS 10.6+).
    public void checkTextInDocument(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.checkTextInDocument, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView usesFindPanel]
    public boolean usesFindPanel() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesFindPanel);
    }

    /// [textView setUsesFindPanel:]
    public void setUsesFindPanel(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setUsesFindPanel, flag);
    }

    /// [textView usesFindBar] (macOS 10.7+).
    public boolean usesFindBar() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.usesFindBar);
    }

    /// [textView setUsesFindBar:] (macOS 10.7+).
    public void setUsesFindBar(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setUsesFindBar, flag);
    }

    /// [textView isIncrementalSearchingEnabled] (macOS 10.7+).
    public boolean isIncrementalSearchingEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isIncrementalSearchingEnabled);
    }

    /// [textView setIncrementalSearchingEnabled:] (macOS 10.7+).
    public void setIncrementalSearchingEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setIncrementalSearchingEnabled, flag);
    }

    /// [textView toggleQuickLookPreviewPanel:] (macOS 10.7+).
    public void toggleQuickLookPreviewPanel(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleQuickLookPreviewPanel, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView quickLookPreviewableItemsInRanges:] — NSArray of NSValue in, preview items out.
    public NSArray quickLookPreviewableItemsInRanges(NSArray ranges) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer,
                    Sels.quickLookPreviewableItemsInRanges,
                    (MemorySegment) (ranges == null ? MemorySegment.NULL : ranges.peer()));
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("quickLookPreviewableItemsInRanges: failed", t);
        }
    }

    /// [textView updateQuickLookPreviewPanel] (macOS 10.7+).
    public void updateQuickLookPreviewPanel() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.updateQuickLookPreviewPanel);
    }

    /// [textView orderFrontSharingServicePicker:] (macOS 10.8+).
    public void orderFrontSharingServicePicker(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.orderFrontSharingServicePicker, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- TouchBar (macOS 10.12.2+) ----

    /// [textView isAutomaticTextCompletionEnabled]
    public boolean isAutomaticTextCompletionEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isAutomaticTextCompletionEnabled);
    }

    /// [textView setAutomaticTextCompletionEnabled:]
    public void setAutomaticTextCompletionEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutomaticTextCompletionEnabled, flag);
    }

    /// [textView toggleAutomaticTextCompletion:]
    public void toggleAutomaticTextCompletion(MemorySegment sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleAutomaticTextCompletion, (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView allowsCharacterPickerTouchBarItem]
    public boolean allowsCharacterPickerTouchBarItem() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsCharacterPickerTouchBarItem);
    }

    /// [textView setAllowsCharacterPickerTouchBarItem:]
    public void setAllowsCharacterPickerTouchBarItem(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAllowsCharacterPickerTouchBarItem, flag);
    }

    /// [textView updateTouchBarItemIdentifiers]
    public void updateTouchBarItemIdentifiers() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.updateTouchBarItemIdentifiers);
    }

    /// [textView updateTextTouchBarItems]
    public void updateTextTouchBarItems() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.updateTextTouchBarItems);
    }

    /// [textView updateCandidates]
    public void updateCandidates() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.updateCandidates);
    }

    /// [textView candidateListTouchBarItem] -> NSCandidateListTouchBarItem* (raw, may be nil).
    public MemorySegment candidateListTouchBarItem() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.candidateListTouchBarItem);
    }

    // ---- factories ----

    /// `+[NSTextView scrollableTextView]` (macOS 10.14+) — text view inside a scroll view.
    /// Typed as NSView: NSScrollView exposes no wrap in this tree; the peer isKindOfClass:NSScrollView.
    public static NSView scrollableTextView() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(ObjC.cls("NSTextView"), Sels.scrollableTextView));
    }

    /// `+[NSTextView fieldEditor]` (macOS 10.14+) — a field-editor-configured text view.
    public static NSTextView fieldEditor() {
        ensureInit();
        return NSTextView.wrap(ObjC.msgSendId(ObjC.cls("NSTextView"), Sels.fieldEditor));
    }

    /// `+[NSTextView scrollableDocumentContentTextView]` (macOS 10.14+) — typed as NSView (see above).
    public static NSView scrollableDocumentContentTextView() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(ObjC.cls("NSTextView"), Sels.scrollableDocumentContentTextView));
    }

    /// `+[NSTextView scrollablePlainDocumentContentTextView]` (macOS 10.14+) — typed as NSView (see above).
    public static NSView scrollablePlainDocumentContentTextView() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(ObjC.cls("NSTextView"), Sels.scrollablePlainDocumentContentTextView));
    }

    // ---- WritingTools (macOS 15+) ----

    /// [textView isWritingToolsActive] (readonly).
    public boolean isWritingToolsActive() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isWritingToolsActive);
    }

    /// [textView writingToolsBehavior] -> NSWritingToolsBehavior (long).
    public long writingToolsBehavior() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.writingToolsBehavior);
    }

    /// [textView setWritingToolsBehavior:]
    public void setWritingToolsBehavior(long behavior) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setWritingToolsBehavior, behavior);
    }

    /// [textView allowedWritingToolsResultOptions] -> NSWritingToolsResultOptions (long).
    public long allowedWritingToolsResultOptions() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.allowedWritingToolsResultOptions);
    }

    /// [textView setAllowedWritingToolsResultOptions:]
    public void setAllowedWritingToolsResultOptions(long options) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setAllowedWritingToolsResultOptions, options);
    }

    // SDK omissions: NSTextViewDelegate protocol (upcall machinery — skipped, like
    // NSTextStorageDelegate/NSLayoutManagerDelegate); deprecated -insertText: and
    // -toggleTraditionalCharacterShape: / -toggleBaseWritingDirection: skipped.
}
