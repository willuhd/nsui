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
        return ObjC.msgSendBool(peer, ObjC.sel("usesFontPanel"));
    }

    /// [textView setUsesFontPanel:] — enable/disable the font panel.
    public void setUsesFontPanel(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesFontPanel:"), flag);
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
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        if (p != null && p.address() != 0) return NSAttributedString.wrap(p);
        return super.attributedString();
    }
    @Override
    public void setAttributedString(NSAttributedString s) {
        // NSTextView has no setAttributedString: — use its textStorage (NSTextStorage is a NSMutableAttributedString)
        MemorySegment storage = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        if (storage != null && storage.address() != 0) {
            ObjC.msgSendVoidId(storage, ObjC.sel("setAttributedString:"), (MemorySegment) (s == null ? MemorySegment.NULL : s.peer()));
            return;
        }
        super.setAttributedString(s);
    }

    public NSAttributedString attributedStringValueTyped() {
        return NSAttributedString.wrap(ObjC.msgSendId(peer, ObjC.sel("attributedString")));
    }
    public void setAttributedStringValue(NSAttributedString value) {
        // NSTextView implements neither setAttributedStringValue: nor setAttributedString:
        // (verified against the runtime) — both route through its textStorage.
        setAttributedString(value);
    }

    /// [textView textStorage] -> NSTextStorage (NSMutableAttributedString)
    public NSMutableAttributedString textStorage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
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
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("layoutManager"));
        return NSLayoutManager.wrap(p);
    }

    /// [textView textContainer] -> NSTextContainer (may be nil).
    public NSTextContainer textContainer() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textContainer"));
        return NSTextContainer.wrap(p);
    }

    /// [textView textStorage] as NSTextStorage (typed).
    public NSTextStorage textStorageAsStorage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("textStorage"));
        return NSTextStorage.wrap(p);
    }

    /// Wire a full trio manually: storage -> layoutManager -> container -> textView.
    /// Minimal helper — callers that need a custom trio can use this instead of relying
    /// on the default NSTextView initialization.
    public void replaceTextContainer(NSTextContainer container) {
        ObjC.msgSendVoidId(peer, ObjC.sel("replaceTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
    }

    /// [textView setTextContainer:] — the primitive; prefer replaceTextContainer: for web-safe swaps.
    public void setTextContainer(NSTextContainer container) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTextContainer:"),
                (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
    }

    /// [textView textContainerInset] -> NSSize (padding around the container).
    public NSSize textContainerInset() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.SIZE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("textContainerInset"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("textContainerInset failed", t);
        }
    }

    /// [textView setTextContainerInset:]
    public void setTextContainerInset(NSSize inset) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
            h.invokeExact(peer, ObjC.sel("setTextContainerInset:"), inset.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setTextContainerInset: failed", t);
        }
    }

    /// [textView textContainerOrigin] -> NSPoint (readonly).
    public NSPoint textContainerOrigin() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.POINT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("textContainerOrigin"));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("textContainerOrigin failed", t);
        }
    }

    /// [textView invalidateTextContainerOrigin]
    public void invalidateTextContainerOrigin() {
        ObjC.msgSendVoid(peer, ObjC.sel("invalidateTextContainerOrigin"));
    }

    /// [textView textLayoutManager] -> NSTextLayoutManager* (TextKit 2; raw, may be nil).
    public MemorySegment textLayoutManager() {
        return ObjC.msgSendId(peer, ObjC.sel("textLayoutManager"));
    }

    /// [textView textContentStorage] -> NSTextContentStorage* (TextKit 2; raw, may be nil).
    public MemorySegment textContentStorage() {
        return ObjC.msgSendId(peer, ObjC.sel("textContentStorage"));
    }

    // -initWithFrame:textContainer: omitted: (RECT,ID)->ID has no vocabulary entry
    // (grep MISS) — reported. -initUsingTextLayoutManager: / +textViewUsingTextLayoutManager:
    // omitted: (BOOL)->ID has no entry (grep MISS) — reported.

    /// [textView setConstrainedFrameSize:] — size within min/max constraints.
    public void setConstrainedFrameSize(NSSize desiredSize) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
            h.invokeExact(peer, ObjC.sel("setConstrainedFrameSize:"), desiredSize.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setConstrainedFrameSize: failed", t);
        }
    }

    // -setAlignment:range: / -setBaseWritingDirection:range: omitted: (INT,RANGE)->VOID
    // has no vocabulary entry (grep MISS) — reported.

    // ---- font menu commands (all take a nullable sender id) ----

    /// [textView turnOffKerning:]
    public void turnOffKerning(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("turnOffKerning:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView tightenKerning:]
    public void tightenKerning(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("tightenKerning:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView loosenKerning:]
    public void loosenKerning(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("loosenKerning:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView useStandardKerning:]
    public void useStandardKerning(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("useStandardKerning:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView turnOffLigatures:]
    public void turnOffLigatures(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("turnOffLigatures:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView useStandardLigatures:]
    public void useStandardLigatures(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("useStandardLigatures:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView useAllLigatures:]
    public void useAllLigatures(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("useAllLigatures:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView raiseBaseline:]
    public void raiseBaseline(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("raiseBaseline:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView lowerBaseline:]
    public void lowerBaseline(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("lowerBaseline:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView outline:]
    public void outline(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("outline:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // -toggleTraditionalCharacterShape: skipped as deprecated.

    /// [textView performFindPanelAction:] — sender tag is an NSFindPanelAction.
    public void performFindPanelAction(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("performFindPanelAction:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- text commands ----

    /// [textView alignJustified:]
    public void alignJustified(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("alignJustified:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView changeColor:]
    public void changeColor(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("changeColor:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView changeAttributes:]
    public void changeAttributes(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("changeAttributes:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView changeDocumentBackgroundColor:]
    public void changeDocumentBackgroundColor(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("changeDocumentBackgroundColor:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView orderFrontSpacingPanel:]
    public void orderFrontSpacingPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontSpacingPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView orderFrontLinkPanel:]
    public void orderFrontLinkPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontLinkPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView orderFrontListPanel:]
    public void orderFrontListPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontListPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView orderFrontTablePanel:]
    public void orderFrontTablePanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontTablePanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- ruler callbacks ----

    /// [textView rulerView:didMoveMarker:] — ruler/marker are NSRulerView*/NSRulerMarker*.
    public void rulerDidMoveMarker(MemorySegment ruler, MemorySegment marker) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("rulerView:didMoveMarker:"),
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:didMoveMarker: failed", t);
        }
    }

    /// [textView rulerView:didRemoveMarker:]
    public void rulerDidRemoveMarker(MemorySegment ruler, MemorySegment marker) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("rulerView:didRemoveMarker:"),
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:didRemoveMarker: failed", t);
        }
    }

    /// [textView rulerView:didAddMarker:]
    public void rulerDidAddMarker(MemorySegment ruler, MemorySegment marker) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("rulerView:didAddMarker:"),
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:didAddMarker: failed", t);
        }
    }

    /// [textView rulerView:shouldMoveMarker:] — ruler/marker raw segments.
    public boolean rulerShouldMoveMarker(MemorySegment ruler, MemorySegment marker) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("rulerView:shouldMoveMarker:"),
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:shouldMoveMarker: failed", t);
        }
    }

    /// [textView rulerView:shouldAddMarker:]
    public boolean rulerShouldAddMarker(MemorySegment ruler, MemorySegment marker) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("rulerView:shouldAddMarker:"),
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (marker == null ? MemorySegment.NULL : marker));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:shouldAddMarker: failed", t);
        }
    }

    /// [textView rulerView:shouldRemoveMarker:]
    public boolean rulerShouldRemoveMarker(MemorySegment ruler, MemorySegment marker) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("rulerView:shouldRemoveMarker:"),
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
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("rulerView:handleMouseDown:"),
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler),
                    (MemorySegment) (event == null ? MemorySegment.NULL : event));
        } catch (Throwable t) {
            throw new RuntimeException("rulerView:handleMouseDown: failed", t);
        }
    }

    // ---- display control ----

    /// [textView setNeedsDisplayInRect:avoidAdditionalLayout:]
    public void setNeedsDisplayInRectAvoidAdditionalLayout(NSRect rect, boolean flag) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.BOOL));
            h.invokeExact(peer, ObjC.sel("setNeedsDisplayInRect:avoidAdditionalLayout:"), rect.toSegment(), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setNeedsDisplayInRect:avoidAdditionalLayout: failed", t);
        }
    }

    // -drawInsertionPointInRect:color:turnedOn: omitted: (RECT,ID,BOOL)->VOID has no
    // vocabulary entry (grep MISS) — reported.

    /// [textView shouldDrawInsertionPoint]
    public boolean shouldDrawInsertionPoint() {
        return ObjC.msgSendBool(peer, ObjC.sel("shouldDrawInsertionPoint"));
    }

    /// [textView drawViewBackgroundInRect:] — subclass override point.
    public void drawViewBackgroundInRect(NSRect rect) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
            h.invokeExact(peer, ObjC.sel("drawViewBackgroundInRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("drawViewBackgroundInRect: failed", t);
        }
    }

    /// [textView updateRuler]
    public void updateRuler() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateRuler"));
    }

    /// [textView updateFontPanel]
    public void updateFontPanel() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateFontPanel"));
    }

    /// [textView updateDragTypeRegistration]
    public void updateDragTypeRegistration() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateDragTypeRegistration"));
    }

    // -selectionRangeForProposedRange:granularity: omitted: (RANGE,INT)->RANGE has no
    // vocabulary entry (grep MISS for RANGE(RANGE,INT)) — reported.

    /// [textView clickedOnLink:atIndex:] — link is id (NSURL/NSString).
    public void clickedOnLinkAtIndex(MemorySegment link, long charIndex) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("clickedOnLink:atIndex:"),
                    (MemorySegment) (link == null ? MemorySegment.NULL : link), charIndex);
        } catch (Throwable t) {
            throw new RuntimeException("clickedOnLink:atIndex: failed", t);
        }
    }

    /// [textView startSpeaking:]
    public void startSpeaking(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("startSpeaking:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView stopSpeaking:]
    public void stopSpeaking(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("stopSpeaking:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView setLayoutOrientation:] — NSTextLayoutOrientation (macOS 10.7+).
    public void setLayoutOrientation(long orientation) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLayoutOrientation:"), orientation);
    }

    /// [textView changeLayoutOrientation:] — sender tag is the orientation (macOS 10.7+).
    public void changeLayoutOrientation(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("changeLayoutOrientation:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // -characterIndexForInsertionAtPoint: omitted: (POINT)->INT has no vocabulary entry
    // (grep MISS) — reported. -performValidatedReplacementInRange:withAttributedString:
    // omitted: (RANGE,ID)->BOOL has no entry (grep MISS) — reported.

    /// [textView stronglyReferencesTextStorage] (class property, macOS 10.12+).
    public static boolean stronglyReferencesTextStorage() {
        return ObjC.msgSendBool(ObjC.cls("NSTextView"), ObjC.sel("stronglyReferencesTextStorage"));
    }

    /// [textView usesAdaptiveColorMappingForDarkAppearance] (macOS 10.14+).
    public boolean usesAdaptiveColorMappingForDarkAppearance() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesAdaptiveColorMappingForDarkAppearance"));
    }

    /// [textView setUsesAdaptiveColorMappingForDarkAppearance:] (macOS 10.14+).
    public void setUsesAdaptiveColorMappingForDarkAppearance(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesAdaptiveColorMappingForDarkAppearance:"), flag);
    }

    // ---- completion ----

    /// [textView complete:] — invoke completion programmatically.
    public void complete(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("complete:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView rangeForUserCompletion] -> NSRange (readonly).
    public NSRange rangeForUserCompletion() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("rangeForUserCompletion"));
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeForUserCompletion failed", t);
        }
    }

    /// [textView completionsForPartialWordRange:indexOfSelectedItem:] — indexOut is
    /// NSInteger* (nullable; pass NULL to ignore) -> NSArray of NSString (may be nil).
    public NSArray completionsForPartialWordRange(NSRange charRange, MemorySegment indexOutOrNull) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("completionsForPartialWordRange:indexOfSelectedItem:"),
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
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("writablePasteboardTypes")));
    }

    /// [textView readablePasteboardTypes] -> NSArray.
    public NSArray readablePasteboardTypes() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("readablePasteboardTypes")));
    }

    /// [textView writeSelectionToPasteboard:type:] — both NSPasteboard*/type (raw segments).
    public boolean writeSelectionToPasteboardType(MemorySegment pboard, MemorySegment type) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("writeSelectionToPasteboard:type:"),
                    (MemorySegment) (pboard == null ? MemorySegment.NULL : pboard),
                    (MemorySegment) (type == null ? MemorySegment.NULL : type));
        } catch (Throwable t) {
            throw new RuntimeException("writeSelectionToPasteboard:type: failed", t);
        }
    }

    /// [textView writeSelectionToPasteboard:types:] — types is NSArray*.
    public boolean writeSelectionToPasteboardTypes(MemorySegment pboard, NSArray types) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("writeSelectionToPasteboard:types:"),
                    (MemorySegment) (pboard == null ? MemorySegment.NULL : pboard),
                    (MemorySegment) (types == null ? MemorySegment.NULL : types.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("writeSelectionToPasteboard:types: failed", t);
        }
    }

    /// [textView preferredPasteboardTypeFromArray:restrictedToTypesFromArray:] — NSArrays.
    public MemorySegment preferredPasteboardType(NSArray availableTypes, NSArray allowedTypes) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("preferredPasteboardTypeFromArray:restrictedToTypesFromArray:"),
                    (MemorySegment) (availableTypes == null ? MemorySegment.NULL : availableTypes.peer()),
                    (MemorySegment) (allowedTypes == null ? MemorySegment.NULL : allowedTypes.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("preferredPasteboardTypeFromArray:restrictedToTypesFromArray: failed", t);
        }
    }

    /// [textView readSelectionFromPasteboard:type:]
    public boolean readSelectionFromPasteboardType(MemorySegment pboard, MemorySegment type) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("readSelectionFromPasteboard:type:"),
                    (MemorySegment) (pboard == null ? MemorySegment.NULL : pboard),
                    (MemorySegment) (type == null ? MemorySegment.NULL : type));
        } catch (Throwable t) {
            throw new RuntimeException("readSelectionFromPasteboard:type: failed", t);
        }
    }

    /// [textView readSelectionFromPasteboard:] (services path).
    public boolean readSelectionFromPasteboard(MemorySegment pboard) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("readSelectionFromPasteboard:"),
                    (MemorySegment) (pboard == null ? MemorySegment.NULL : pboard));
        } catch (Throwable t) {
            throw new RuntimeException("readSelectionFromPasteboard: failed", t);
        }
    }

    /// `+[NSTextView registerForServices]`
    public static void registerForServices() {
        ObjC.msgSendVoid(ObjC.cls("NSTextView"), ObjC.sel("registerForServices"));
    }

    /// [textView validRequestorForSendType:returnType:] — pasteboard type names (raw segments).
    public MemorySegment validRequestorForSendTypeReturnType(MemorySegment sendType, MemorySegment returnType) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("validRequestorForSendType:returnType:"),
                    (MemorySegment) (sendType == null ? MemorySegment.NULL : sendType),
                    (MemorySegment) (returnType == null ? MemorySegment.NULL : returnType));
        } catch (Throwable t) {
            throw new RuntimeException("validRequestorForSendType:returnType: failed", t);
        }
    }

    /// [textView pasteAsPlainText:]
    public void pasteAsPlainText(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("pasteAsPlainText:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView pasteAsRichText:]
    public void pasteAsRichText(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("pasteAsRichText:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- dragging ----

    // -dragSelectionWithEvent:offset:slideBack: omitted: (ID,SIZE,BOOL)->BOOL has no
    // vocabulary entry — reported.

    /// [textView dragImageForSelectionWithEvent:origin:] — origin is NSPoint* (nullable).
    public MemorySegment dragImageForSelectionWithEvent(MemorySegment event, MemorySegment originOutOrNull) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("dragImageForSelectionWithEvent:origin:"),
                    (MemorySegment) (event == null ? MemorySegment.NULL : event),
                    (MemorySegment) (originOutOrNull == null ? MemorySegment.NULL : originOutOrNull));
        } catch (Throwable t) {
            throw new RuntimeException("dragImageForSelectionWithEvent:origin: failed", t);
        }
    }

    /// [textView acceptableDragTypes] -> NSArray.
    public NSArray acceptableDragTypes() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("acceptableDragTypes")));
    }

    /// [textView dragOperationForDraggingInfo:type:] -> NSDragOperation (long).
    public long dragOperationForDraggingInfoType(MemorySegment dragInfo, MemorySegment type) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("dragOperationForDraggingInfo:type:"),
                    (MemorySegment) (dragInfo == null ? MemorySegment.NULL : dragInfo),
                    (MemorySegment) (type == null ? MemorySegment.NULL : type));
        } catch (Throwable t) {
            throw new RuntimeException("dragOperationForDraggingInfo:type: failed", t);
        }
    }

    /// [textView cleanUpAfterDragOperation]
    public void cleanUpAfterDragOperation() {
        ObjC.msgSendVoid(peer, ObjC.sel("cleanUpAfterDragOperation"));
    }

    // ---- shared selection state (NSSharing) ----

    /// [textView selectedRanges] -> NSArray of NSValue (rangeValue).
    public NSArray selectedRanges() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("selectedRanges")));
    }

    /// [textView setSelectedRanges:] — NSArray of NSValue.
    public void setSelectedRanges(NSArray ranges) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSelectedRanges:"),
                (MemorySegment) (ranges == null ? MemorySegment.NULL : ranges.peer()));
    }

    /// [textView setSelectedRanges:affinity:stillSelecting:] — multi-range change.
    public void setSelectedRangesAffinityStillSelecting(NSArray ranges, long affinity, boolean stillSelecting) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT, Arg.BOOL));
            h.invokeExact(peer, ObjC.sel("setSelectedRanges:affinity:stillSelecting:"),
                    (MemorySegment) (ranges == null ? MemorySegment.NULL : ranges.peer()), affinity, stillSelecting);
        } catch (Throwable t) {
            throw new RuntimeException("setSelectedRanges:affinity:stillSelecting: failed", t);
        }
    }

    // -setSelectedRange:affinity:stillSelecting: omitted: (RANGE,INT,BOOL)->VOID has no
    // vocabulary entry — reported.

    /// [textView setSelectedRange:] (single-range NSTextView override).
    public void setSelectedRange(NSRange charRange) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("setSelectedRange:"), charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setSelectedRange: failed", t);
        }
    }

    /// [textView selectionAffinity] -> NSSelectionAffinity (long, readonly).
    public long selectionAffinity() {
        return ObjC.msgSendLong(peer, ObjC.sel("selectionAffinity"));
    }

    /// [textView selectionGranularity] -> NSSelectionGranularity (long).
    public long selectionGranularity() {
        return ObjC.msgSendLong(peer, ObjC.sel("selectionGranularity"));
    }

    /// [textView setSelectionGranularity:]
    public void setSelectionGranularity(long granularity) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSelectionGranularity:"), granularity);
    }

    /// [textView selectedTextAttributes] -> NSDictionary* (raw segment).
    public MemorySegment selectedTextAttributes() {
        return ObjC.msgSendId(peer, ObjC.sel("selectedTextAttributes"));
    }

    /// [textView setSelectedTextAttributes:] — NSDictionary* (may be NULL).
    public void setSelectedTextAttributes(MemorySegment attributes) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSelectedTextAttributes:"),
                (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
    }

    /// [textView insertionPointColor] -> NSColor.
    public NSColor insertionPointColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("insertionPointColor")));
    }

    /// [textView setInsertionPointColor:]
    public void setInsertionPointColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setInsertionPointColor:"),
                (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// [textView markedTextAttributes] -> NSDictionary* (raw segment, may be nil).
    public MemorySegment markedTextAttributes() {
        return ObjC.msgSendId(peer, ObjC.sel("markedTextAttributes"));
    }

    /// [textView setMarkedTextAttributes:] — NSDictionary* (may be NULL).
    public void setMarkedTextAttributes(MemorySegment attributes) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMarkedTextAttributes:"),
                (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
    }

    /// [textView linkTextAttributes] -> NSDictionary* (raw segment).
    public MemorySegment linkTextAttributes() {
        return ObjC.msgSendId(peer, ObjC.sel("linkTextAttributes"));
    }

    /// [textView setLinkTextAttributes:] — NSDictionary* (may be NULL).
    public void setLinkTextAttributes(MemorySegment attributes) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setLinkTextAttributes:"),
                (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
    }

    /// [textView updateInsertionPointStateAndRestartTimer:]
    public void updateInsertionPointStateAndRestartTimer(boolean restartFlag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("updateInsertionPointStateAndRestartTimer:"), restartFlag);
    }

    // ---- text conveniences ----

    /// [textView displaysLinkToolTips] (macOS 10.5+).
    public boolean displaysLinkToolTips() {
        return ObjC.msgSendBool(peer, ObjC.sel("displaysLinkToolTips"));
    }

    /// [textView setDisplaysLinkToolTips:] (macOS 10.5+).
    public void setDisplaysLinkToolTips(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDisplaysLinkToolTips:"), flag);
    }

    /// [textView acceptsGlyphInfo]
    public boolean acceptsGlyphInfo() {
        return ObjC.msgSendBool(peer, ObjC.sel("acceptsGlyphInfo"));
    }

    /// [textView setAcceptsGlyphInfo:]
    public void setAcceptsGlyphInfo(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAcceptsGlyphInfo:"), flag);
    }

    /// [textView usesRuler]
    public boolean usesRuler() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesRuler"));
    }

    /// [textView setUsesRuler:]
    public void setUsesRuler(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesRuler:"), flag);
    }

    /// [textView usesInspectorBar] (macOS 10.7+).
    public boolean usesInspectorBar() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesInspectorBar"));
    }

    /// [textView setUsesInspectorBar:] (macOS 10.7+).
    public void setUsesInspectorBar(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesInspectorBar:"), flag);
    }

    /// [textView isContinuousSpellCheckingEnabled]
    public boolean isContinuousSpellCheckingEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isContinuousSpellCheckingEnabled"));
    }

    /// [textView setContinuousSpellCheckingEnabled:]
    public void setContinuousSpellCheckingEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setContinuousSpellCheckingEnabled:"), flag);
    }

    /// [textView toggleContinuousSpellChecking:]
    public void toggleContinuousSpellChecking(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleContinuousSpellChecking:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView spellCheckerDocumentTag] -> NSInteger (readonly).
    public long spellCheckerDocumentTag() {
        return ObjC.msgSendLong(peer, ObjC.sel("spellCheckerDocumentTag"));
    }

    /// [textView isGrammarCheckingEnabled] (macOS 10.5+).
    public boolean isGrammarCheckingEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isGrammarCheckingEnabled"));
    }

    /// [textView setGrammarCheckingEnabled:] (macOS 10.5+).
    public void setGrammarCheckingEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setGrammarCheckingEnabled:"), flag);
    }

    /// [textView toggleGrammarChecking:] (macOS 10.5+).
    public void toggleGrammarChecking(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleGrammarChecking:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // -setSpellingState:range: omitted: (INT,RANGE)->VOID has no vocabulary entry — reported.

    /// [textView typingAttributes] -> NSDictionary* (raw segment).
    public MemorySegment typingAttributes() {
        return ObjC.msgSendId(peer, ObjC.sel("typingAttributes"));
    }

    /// [textView setTypingAttributes:] — NSDictionary* (may be NULL).
    public void setTypingAttributes(MemorySegment attributes) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTypingAttributes:"),
                (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
    }

    /// [textView rangesForUserTextChange] -> NSArray of NSValue (may be nil).
    public NSArray rangesForUserTextChange() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("rangesForUserTextChange")));
    }

    /// [textView rangesForUserCharacterAttributeChange] -> NSArray (may be nil).
    public NSArray rangesForUserCharacterAttributeChange() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("rangesForUserCharacterAttributeChange")));
    }

    /// [textView rangesForUserParagraphAttributeChange] -> NSArray (may be nil).
    public NSArray rangesForUserParagraphAttributeChange() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("rangesForUserParagraphAttributeChange")));
    }

    /// [textView rangeForUserTextChange] -> NSRange (readonly).
    public NSRange rangeForUserTextChange() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("rangeForUserTextChange"));
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeForUserTextChange failed", t);
        }
    }

    /// [textView rangeForUserCharacterAttributeChange] -> NSRange (readonly).
    public NSRange rangeForUserCharacterAttributeChange() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("rangeForUserCharacterAttributeChange"));
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeForUserCharacterAttributeChange failed", t);
        }
    }

    /// [textView rangeForUserParagraphAttributeChange] -> NSRange (readonly).
    public NSRange rangeForUserParagraphAttributeChange() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("rangeForUserParagraphAttributeChange"));
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeForUserParagraphAttributeChange failed", t);
        }
    }

    /// [textView shouldChangeTextInRanges:replacementStrings:] — validation query.
    public boolean shouldChangeTextInRangesReplacementStrings(NSArray affectedRanges, NSArray replacementStrings) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("shouldChangeTextInRanges:replacementStrings:"),
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
        ObjC.msgSendVoid(peer, ObjC.sel("didChangeText"));
    }

    /// [textView allowsDocumentBackgroundColorChange]
    public boolean allowsDocumentBackgroundColorChange() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsDocumentBackgroundColorChange"));
    }

    /// [textView setAllowsDocumentBackgroundColorChange:]
    public void setAllowsDocumentBackgroundColorChange(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsDocumentBackgroundColorChange:"), flag);
    }

    /// [textView defaultParagraphStyle] -> NSParagraphStyle (may be nil).
    public NSParagraphStyle defaultParagraphStyle() {
        return NSParagraphStyle.wrap(ObjC.msgSendId(peer, ObjC.sel("defaultParagraphStyle")));
    }

    /// [textView setDefaultParagraphStyle:] — may be NULL.
    public void setDefaultParagraphStyle(NSParagraphStyle style) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDefaultParagraphStyle:"),
                (MemorySegment) (style == null ? MemorySegment.NULL : style.peer()));
    }

    /// [textView allowsUndo]
    public boolean allowsUndo() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsUndo"));
    }

    /// [textView setAllowsUndo:]
    public void setAllowsUndo(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsUndo:"), flag);
    }

    /// [textView breakUndoCoalescing]
    public void breakUndoCoalescing() {
        ObjC.msgSendVoid(peer, ObjC.sel("breakUndoCoalescing"));
    }

    /// [textView isCoalescingUndo] (readonly, macOS 10.6+).
    public boolean isCoalescingUndo() {
        return ObjC.msgSendBool(peer, ObjC.sel("isCoalescingUndo"));
    }

    /// [textView allowsImageEditing] (macOS 10.5+).
    public boolean allowsImageEditing() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsImageEditing"));
    }

    /// [textView setAllowsImageEditing:] (macOS 10.5+).
    public void setAllowsImageEditing(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsImageEditing:"), flag);
    }

    /// [textView showFindIndicatorForRange:] (macOS 10.5+).
    public void showFindIndicatorForRange(NSRange charRange) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("showFindIndicatorForRange:"), charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("showFindIndicatorForRange: failed", t);
        }
    }

    /// [textView usesRolloverButtonForSelection] (macOS 10.10+).
    public boolean usesRolloverButtonForSelection() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesRolloverButtonForSelection"));
    }

    /// [textView setUsesRolloverButtonForSelection:] (macOS 10.10+).
    public void setUsesRolloverButtonForSelection(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesRolloverButtonForSelection:"), flag);
    }

    /// [textView allowedInputSourceLocales] -> NSArray of NSString (may be nil, macOS 10.5+).
    public NSArray allowedInputSourceLocales() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("allowedInputSourceLocales")));
    }

    /// [textView setAllowedInputSourceLocales:] — NSArray (may be NULL, macOS 10.5+).
    public void setAllowedInputSourceLocales(NSArray locales) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAllowedInputSourceLocales:"),
                (MemorySegment) (locales == null ? MemorySegment.NULL : locales.peer()));
    }

    // ---- smart insert / substitution ----

    /// [textView smartInsertDeleteEnabled]
    public boolean smartInsertDeleteEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("smartInsertDeleteEnabled"));
    }

    /// [textView setSmartInsertDeleteEnabled:]
    public void setSmartInsertDeleteEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setSmartInsertDeleteEnabled:"), flag);
    }

    /// [textView smartDeleteRangeForProposedRange:] -> NSRange.
    public NSRange smartDeleteRangeForProposedRange(NSRange proposedCharRange) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer,
                    ObjC.sel("smartDeleteRangeForProposedRange:"), proposedCharRange.toSegment());
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("smartDeleteRangeForProposedRange: failed", t);
        }
    }

    /// [textView toggleSmartInsertDelete:]
    public void toggleSmartInsertDelete(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleSmartInsertDelete:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // -smartInsertForString:... (out-pointers) omitted — reported.

    /// [textView smartInsertBeforeStringForString:replacingRange:] — may return nil.
    public String smartInsertBeforeStringForStringReplacingRange(String pasteString, NSRange charRangeToReplace) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("smartInsertBeforeStringForString:replacingRange:"),
                    ObjC.nsstring(pasteString == null ? "" : pasteString), charRangeToReplace.toSegment());
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("smartInsertBeforeStringForString:replacingRange: failed", t);
        }
    }

    /// [textView smartInsertAfterStringForString:replacingRange:] — may return nil.
    public String smartInsertAfterStringForStringReplacingRange(String pasteString, NSRange charRangeToReplace) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("smartInsertAfterStringForString:replacingRange:"),
                    ObjC.nsstring(pasteString == null ? "" : pasteString), charRangeToReplace.toSegment());
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("smartInsertAfterStringForString:replacingRange: failed", t);
        }
    }

    /// [textView isAutomaticQuoteSubstitutionEnabled] (macOS 10.5+).
    public boolean isAutomaticQuoteSubstitutionEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isAutomaticQuoteSubstitutionEnabled"));
    }

    /// [textView setAutomaticQuoteSubstitutionEnabled:] (macOS 10.5+).
    public void setAutomaticQuoteSubstitutionEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutomaticQuoteSubstitutionEnabled:"), flag);
    }

    /// [textView toggleAutomaticQuoteSubstitution:] (macOS 10.5+).
    public void toggleAutomaticQuoteSubstitution(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleAutomaticQuoteSubstitution:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticLinkDetectionEnabled] (macOS 10.5+).
    public boolean isAutomaticLinkDetectionEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isAutomaticLinkDetectionEnabled"));
    }

    /// [textView setAutomaticLinkDetectionEnabled:] (macOS 10.5+).
    public void setAutomaticLinkDetectionEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutomaticLinkDetectionEnabled:"), flag);
    }

    /// [textView toggleAutomaticLinkDetection:] (macOS 10.5+).
    public void toggleAutomaticLinkDetection(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleAutomaticLinkDetection:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticDataDetectionEnabled] (macOS 10.6+).
    public boolean isAutomaticDataDetectionEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isAutomaticDataDetectionEnabled"));
    }

    /// [textView setAutomaticDataDetectionEnabled:] (macOS 10.6+).
    public void setAutomaticDataDetectionEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutomaticDataDetectionEnabled:"), flag);
    }

    /// [textView toggleAutomaticDataDetection:] (macOS 10.6+).
    public void toggleAutomaticDataDetection(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleAutomaticDataDetection:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticDashSubstitutionEnabled] (macOS 10.6+).
    public boolean isAutomaticDashSubstitutionEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isAutomaticDashSubstitutionEnabled"));
    }

    /// [textView setAutomaticDashSubstitutionEnabled:] (macOS 10.6+).
    public void setAutomaticDashSubstitutionEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutomaticDashSubstitutionEnabled:"), flag);
    }

    /// [textView toggleAutomaticDashSubstitution:] (macOS 10.6+).
    public void toggleAutomaticDashSubstitution(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleAutomaticDashSubstitution:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticTextReplacementEnabled] (macOS 10.6+).
    public boolean isAutomaticTextReplacementEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isAutomaticTextReplacementEnabled"));
    }

    /// [textView setAutomaticTextReplacementEnabled:] (macOS 10.6+).
    public void setAutomaticTextReplacementEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutomaticTextReplacementEnabled:"), flag);
    }

    /// [textView toggleAutomaticTextReplacement:] (macOS 10.6+).
    public void toggleAutomaticTextReplacement(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleAutomaticTextReplacement:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView isAutomaticSpellingCorrectionEnabled] (macOS 10.6+).
    public boolean isAutomaticSpellingCorrectionEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isAutomaticSpellingCorrectionEnabled"));
    }

    /// [textView setAutomaticSpellingCorrectionEnabled:] (macOS 10.6+).
    public void setAutomaticSpellingCorrectionEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutomaticSpellingCorrectionEnabled:"), flag);
    }

    /// [textView toggleAutomaticSpellingCorrection:] (macOS 10.6+).
    public void toggleAutomaticSpellingCorrection(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleAutomaticSpellingCorrection:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView enabledTextCheckingTypes] -> NSTextCheckingTypes (long, macOS 10.6+).
    public long enabledTextCheckingTypes() {
        return ObjC.msgSendLong(peer, ObjC.sel("enabledTextCheckingTypes"));
    }

    /// [textView setEnabledTextCheckingTypes:] (macOS 10.6+).
    public void setEnabledTextCheckingTypes(long types) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setEnabledTextCheckingTypes:"), types);
    }

    // -checkTextInRange:types:options: / -handleTextCheckingResults:... omitted (no shape) — reported.

    /// [textView orderFrontSubstitutionsPanel:] (macOS 10.6+).
    public void orderFrontSubstitutionsPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontSubstitutionsPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView checkTextInSelection:] (macOS 10.6+).
    public void checkTextInSelection(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("checkTextInSelection:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView checkTextInDocument:] (macOS 10.6+).
    public void checkTextInDocument(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("checkTextInDocument:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView usesFindPanel]
    public boolean usesFindPanel() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesFindPanel"));
    }

    /// [textView setUsesFindPanel:]
    public void setUsesFindPanel(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesFindPanel:"), flag);
    }

    /// [textView usesFindBar] (macOS 10.7+).
    public boolean usesFindBar() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesFindBar"));
    }

    /// [textView setUsesFindBar:] (macOS 10.7+).
    public void setUsesFindBar(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesFindBar:"), flag);
    }

    /// [textView isIncrementalSearchingEnabled] (macOS 10.7+).
    public boolean isIncrementalSearchingEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isIncrementalSearchingEnabled"));
    }

    /// [textView setIncrementalSearchingEnabled:] (macOS 10.7+).
    public void setIncrementalSearchingEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setIncrementalSearchingEnabled:"), flag);
    }

    /// [textView toggleQuickLookPreviewPanel:] (macOS 10.7+).
    public void toggleQuickLookPreviewPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleQuickLookPreviewPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView quickLookPreviewableItemsInRanges:] — NSArray of NSValue in, preview items out.
    public NSArray quickLookPreviewableItemsInRanges(NSArray ranges) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("quickLookPreviewableItemsInRanges:"),
                    (MemorySegment) (ranges == null ? MemorySegment.NULL : ranges.peer()));
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("quickLookPreviewableItemsInRanges: failed", t);
        }
    }

    /// [textView updateQuickLookPreviewPanel] (macOS 10.7+).
    public void updateQuickLookPreviewPanel() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateQuickLookPreviewPanel"));
    }

    /// [textView orderFrontSharingServicePicker:] (macOS 10.8+).
    public void orderFrontSharingServicePicker(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontSharingServicePicker:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- TouchBar (macOS 10.12.2+) ----

    /// [textView isAutomaticTextCompletionEnabled]
    public boolean isAutomaticTextCompletionEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isAutomaticTextCompletionEnabled"));
    }

    /// [textView setAutomaticTextCompletionEnabled:]
    public void setAutomaticTextCompletionEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutomaticTextCompletionEnabled:"), flag);
    }

    /// [textView toggleAutomaticTextCompletion:]
    public void toggleAutomaticTextCompletion(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleAutomaticTextCompletion:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [textView allowsCharacterPickerTouchBarItem]
    public boolean allowsCharacterPickerTouchBarItem() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsCharacterPickerTouchBarItem"));
    }

    /// [textView setAllowsCharacterPickerTouchBarItem:]
    public void setAllowsCharacterPickerTouchBarItem(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsCharacterPickerTouchBarItem:"), flag);
    }

    /// [textView updateTouchBarItemIdentifiers]
    public void updateTouchBarItemIdentifiers() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateTouchBarItemIdentifiers"));
    }

    /// [textView updateTextTouchBarItems]
    public void updateTextTouchBarItems() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateTextTouchBarItems"));
    }

    /// [textView updateCandidates]
    public void updateCandidates() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateCandidates"));
    }

    /// [textView candidateListTouchBarItem] -> NSCandidateListTouchBarItem* (raw, may be nil).
    public MemorySegment candidateListTouchBarItem() {
        return ObjC.msgSendId(peer, ObjC.sel("candidateListTouchBarItem"));
    }

    // ---- factories ----

    /// `+[NSTextView scrollableTextView]` (macOS 10.14+) — text view inside a scroll view.
    /// Typed as NSView: NSScrollView exposes no wrap in this tree; the peer isKindOfClass:NSScrollView.
    public static NSView scrollableTextView() {
        return NSView.wrap(ObjC.msgSendId(ObjC.cls("NSTextView"), ObjC.sel("scrollableTextView")));
    }

    /// `+[NSTextView fieldEditor]` (macOS 10.14+) — a field-editor-configured text view.
    public static NSTextView fieldEditor() {
        return NSTextView.wrap(ObjC.msgSendId(ObjC.cls("NSTextView"), ObjC.sel("fieldEditor")));
    }

    /// `+[NSTextView scrollableDocumentContentTextView]` (macOS 10.14+) — typed as NSView (see above).
    public static NSView scrollableDocumentContentTextView() {
        return NSView.wrap(ObjC.msgSendId(ObjC.cls("NSTextView"), ObjC.sel("scrollableDocumentContentTextView")));
    }

    /// `+[NSTextView scrollablePlainDocumentContentTextView]` (macOS 10.14+) — typed as NSView (see above).
    public static NSView scrollablePlainDocumentContentTextView() {
        return NSView.wrap(ObjC.msgSendId(ObjC.cls("NSTextView"), ObjC.sel("scrollablePlainDocumentContentTextView")));
    }

    // ---- WritingTools (macOS 15+) ----

    /// [textView isWritingToolsActive] (readonly).
    public boolean isWritingToolsActive() {
        return ObjC.msgSendBool(peer, ObjC.sel("isWritingToolsActive"));
    }

    /// [textView writingToolsBehavior] -> NSWritingToolsBehavior (long).
    public long writingToolsBehavior() {
        return ObjC.msgSendLong(peer, ObjC.sel("writingToolsBehavior"));
    }

    /// [textView setWritingToolsBehavior:]
    public void setWritingToolsBehavior(long behavior) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setWritingToolsBehavior:"), behavior);
    }

    /// [textView allowedWritingToolsResultOptions] -> NSWritingToolsResultOptions (long).
    public long allowedWritingToolsResultOptions() {
        return ObjC.msgSendLong(peer, ObjC.sel("allowedWritingToolsResultOptions"));
    }

    /// [textView setAllowedWritingToolsResultOptions:]
    public void setAllowedWritingToolsResultOptions(long options) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setAllowedWritingToolsResultOptions:"), options);
    }

    // SDK omissions: NSTextViewDelegate protocol (upcall machinery — skipped, like
    // NSTextStorageDelegate/NSLayoutManagerDelegate); deprecated -insertText: and
    // -toggleTraditionalCharacterShape: / -toggleBaseWritingDirection: skipped.
}
