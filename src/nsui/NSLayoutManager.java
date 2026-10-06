package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSLayoutManager — the central layout engine linking `NSTextStorage`
/// and `NSTextContainer`. Thin 1:1 wrapper over native `NSLayoutManager`.
public final class NSLayoutManager extends NSObject {

            private record Handles(MethodHandle hInit, MethodHandle hAddContainer, MethodHandle hRangeId, MethodHandle hInt, MethodHandle hVoidRange) {}
    private static volatile Handles handles;

    private NSLayoutManager(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSLayoutManager wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSLayoutManager(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.RANGE, Arg.ID)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE))
        );
    }

    /// `[[NSLayoutManager alloc] init]`
    public static NSLayoutManager create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSLayoutManager"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(alloc, ObjC.sel("init"));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSLayoutManager init returned nil");
            return new NSLayoutManager(p);
        } catch (Throwable t) {
            throw new RuntimeException("NSLayoutManager init failed", t);
        }
    }

    // ---- text storage ----

    /// [layoutManager textStorage] — may be nil.
    public NSTextStorage textStorage() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(peer, ObjC.sel("textStorage"));
            return NSTextStorage.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("textStorage failed", t);
        }
    }

    /// [layoutManager setTextStorage:] — rarely set directly; normally via NSTextStorage addLayoutManager.
    public void setTextStorage(NSTextStorage storage) {
        ensureInit();
        try {
            handles.hAddContainer().invokeExact(peer, ObjC.sel("setTextStorage:"), (MemorySegment) (storage == null ? MemorySegment.NULL : storage.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setTextStorage: failed", t);
        }
    }

    /// [layoutManager replaceTextStorage:]
    public void replaceTextStorage(NSTextStorage storage) {
        ObjC.msgSendVoidId(peer, ObjC.sel("replaceTextStorage:"), (MemorySegment) (storage == null ? MemorySegment.NULL : storage.peer()));
    }

    // ---- text containers ----

    /// [layoutManager addTextContainer:]
    public void addTextContainer(NSTextContainer container) {
        ensureInit();
        try {
            handles.hAddContainer().invokeExact(peer, ObjC.sel("addTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("addTextContainer: failed", t);
        }
    }

    /// [layoutManager insertTextContainer:atIndex:]
    public void insertTextContainerAtIndex(NSTextContainer container, long index) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("insertTextContainer:atIndex:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertTextContainer:atIndex: failed", t);
        }
    }

    /// [layoutManager removeTextContainerAtIndex:]
    public void removeTextContainerAtIndex(long index) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("removeTextContainerAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("removeTextContainerAtIndex: failed", t);
        }
    }

    /// [layoutManager textContainers] — NSArray of NSTextContainer
    public java.util.List<NSTextContainer> textContainers() {
        MemorySegment arr = ObjC.msgSendId(peer, ObjC.sel("textContainers"));
        if (arr == null || arr.address() == 0) return java.util.List.of();
        long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
        java.util.List<NSTextContainer> out = new java.util.ArrayList<>((int) count);
        MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        for (long i = 0; i < count; i++) {
            try {
                MemorySegment v = (MemorySegment) h.invokeExact(arr, ObjC.sel("objectAtIndex:"), i);
                if (v != null && v.address() != 0) out.add(NSTextContainer.wrap(v));
            } catch (Throwable t) {
                throw new RuntimeException("textContainers objectAtIndex failed", t);
            }
        }
        return java.util.Collections.unmodifiableList(out);
    }

    // ---- layout ----

    /// [layoutManager ensureLayoutForTextContainer:]
    public void ensureLayoutForTextContainer(NSTextContainer container) {
        ensureInit();
        try {
            handles.hAddContainer().invokeExact(peer, ObjC.sel("ensureLayoutForTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("ensureLayoutForTextContainer: failed", t);
        }
    }

    /// [layoutManager glyphRangeForTextContainer:] -> NSRange
    public NSRange glyphRangeForTextContainer(NSTextContainer container) {
        ensureInit();
        try {
            // NOTE: struct returns take the shared slot as the implicit first argument;
            // the previous revision omitted it (would have thrown WrongMethodTypeException).
            MemorySegment seg = (MemorySegment) handles.hRangeId().invokeExact(ObjC.structSlot(), peer, ObjC.sel("glyphRangeForTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("glyphRangeForTextContainer: failed", t);
        }
    }

    /// [layoutManager numberOfGlyphs]
    public long numberOfGlyphs() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, ObjC.sel("numberOfGlyphs"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfGlyphs failed", t);
        }
    }

    /// [layoutManager characterIndexForGlyphAtIndex:] -> long
    public long characterIndexForGlyphAtIndex(long glyphIndex) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("characterIndexForGlyphAtIndex:"), glyphIndex);
        } catch (Throwable t) {
            throw new RuntimeException("characterIndexForGlyphAtIndex: failed", t);
        }
    }

    /// [layoutManager glyphIndexForCharacterAtIndex:] -> long
    public long glyphIndexForCharacterAtIndex(long charIndex) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("glyphIndexForCharacterAtIndex:"), charIndex);
        } catch (Throwable t) {
            throw new RuntimeException("glyphIndexForCharacterAtIndex: failed", t);
        }
    }

    /// [layoutManager invalidateLayoutForCharacterRange:actualCharacterRange:] simplified untyped
    public void invalidateDisplayForCharacterRange(NSRange range) {
        try {
            handles.hVoidRange().invokeExact(peer, ObjC.sel("invalidateDisplayForCharacterRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("invalidateDisplayForCharacterRange: failed", t);
        }
    }

    /// [layoutManager usedRectForTextContainer:] -> NSRect
    // NOTE: needs of(RECT,ID) — no vocabulary entry (handle() throws at call time).
    // Pre-existing wrapper kept for API stability; not exercised until Sig grows — reported.
    public NSRect usedRectForTextContainer(NSTextContainer container) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RECT, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("usedRectForTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("usedRectForTextContainer: failed", t);
        }
    }

    // ---- delegate (generic id) ----

    /// [layoutManager delegate] raw id.
    public MemorySegment delegate() {
        return ObjC.msgSendId(peer, ObjC.sel("delegate"));
    }

    /// [layoutManager setDelegate:]
    public void setDelegate(MemorySegment delegate) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"), (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate));
    }

    // ---- extras ----

    /// [layoutManager allowsNonContiguousLayout]
    public boolean allowsNonContiguousLayout() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsNonContiguousLayout"));
    }

    /// [layoutManager setAllowsNonContiguousLayout:]
    public void setAllowsNonContiguousLayout(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsNonContiguousLayout:"), flag);
    }

    /// [layoutManager showsInvisibleCharacters]
    public boolean showsInvisibleCharacters() {
        return ObjC.msgSendBool(peer, ObjC.sel("showsInvisibleCharacters"));
    }

    /// [layoutManager setShowsInvisibleCharacters:]
    public void setShowsInvisibleCharacters(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setShowsInvisibleCharacters:"), flag);
    }

    /// [layoutManager showsControlCharacters]
    public boolean showsControlCharacters() {
        return ObjC.msgSendBool(peer, ObjC.sel("showsControlCharacters"));
    }

    /// [layoutManager setShowsControlCharacters:]
    public void setShowsControlCharacters(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setShowsControlCharacters:"), flag);
    }

    /// [layoutManager usesDefaultHyphenation] (macOS 10.15+).
    public boolean usesDefaultHyphenation() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesDefaultHyphenation"));
    }

    /// [layoutManager setUsesDefaultHyphenation:] (macOS 10.15+).
    public void setUsesDefaultHyphenation(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesDefaultHyphenation:"), flag);
    }

    /// [layoutManager usesFontLeading]
    public boolean usesFontLeading() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesFontLeading"));
    }

    /// [layoutManager setUsesFontLeading:]
    public void setUsesFontLeading(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesFontLeading:"), flag);
    }

    /// [layoutManager hasNonContiguousLayout] (readonly, macOS 10.5+).
    public boolean hasNonContiguousLayout() {
        return ObjC.msgSendBool(peer, ObjC.sel("hasNonContiguousLayout"));
    }

    /// [layoutManager limitsLayoutForSuspiciousContents] (macOS 10.14+).
    public boolean limitsLayoutForSuspiciousContents() {
        return ObjC.msgSendBool(peer, ObjC.sel("limitsLayoutForSuspiciousContents"));
    }

    /// [layoutManager setLimitsLayoutForSuspiciousContents:] (macOS 10.14+).
    public void setLimitsLayoutForSuspiciousContents(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setLimitsLayoutForSuspiciousContents:"), flag);
    }

    /// [layoutManager isBackgroundLayoutEnabled] (getter spelling `backgroundLayoutEnabled`).
    public boolean backgroundLayoutEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("backgroundLayoutEnabled"));
    }

    /// [layoutManager setBackgroundLayoutEnabled:]
    public void setBackgroundLayoutEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBackgroundLayoutEnabled:"), flag);
    }

    /// [layoutManager defaultAttachmentScaling] -> NSImageScaling (long).
    public long defaultAttachmentScaling() {
        return ObjC.msgSendLong(peer, ObjC.sel("defaultAttachmentScaling"));
    }

    /// [layoutManager setDefaultAttachmentScaling:]
    public void setDefaultAttachmentScaling(long scaling) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDefaultAttachmentScaling:"), scaling);
    }

    /// [layoutManager typesetter] -> NSTypesetter* (raw; no wrapper in this batch).
    public MemorySegment typesetter() {
        return ObjC.msgSendId(peer, ObjC.sel("typesetter"));
    }

    /// [layoutManager setTypesetter:] — typesetter is NSTypesetter* (may be NULL).
    public void setTypesetter(MemorySegment typesetter) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTypesetter:"),
                (MemorySegment) (typesetter == null ? MemorySegment.NULL : typesetter));
    }

    /// [layoutManager typesetterBehavior] -> NSTypesetterBehavior (long).
    public long typesetterBehavior() {
        return ObjC.msgSendLong(peer, ObjC.sel("typesetterBehavior"));
    }

    /// [layoutManager setTypesetterBehavior:]
    public void setTypesetterBehavior(long behavior) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTypesetterBehavior:"), behavior);
    }

    /// [layoutManager textContainerChangedGeometry:] — container resized/reshaped.
    public void textContainerChangedGeometry(NSTextContainer container) {
        ObjC.msgSendVoidId(peer, ObjC.sel("textContainerChangedGeometry:"),
                (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
    }

    /// [layoutManager textContainerChangedTextView:] — container view changed.
    public void textContainerChangedTextView(NSTextContainer container) {
        ObjC.msgSendVoidId(peer, ObjC.sel("textContainerChangedTextView:"),
                (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
    }

    // -invalidateGlyphsForCharacterRange:... / -invalidateLayoutForCharacterRange:... /
    // -processEditingForTextStorage:... omitted: out-pointer / 5-arg shapes have no
    // vocabulary entry — reported.

    /// [layoutManager invalidateDisplayForGlyphRange:]
    public void invalidateDisplayForGlyphRange(NSRange glyphRange) {
        try {
            handles.hVoidRange().invokeExact(peer, ObjC.sel("invalidateDisplayForGlyphRange:"), glyphRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("invalidateDisplayForGlyphRange: failed", t);
        }
    }

    // ---- causing glyph generation and layout ----

    /// [layoutManager ensureGlyphsForCharacterRange:]
    public void ensureGlyphsForCharacterRange(NSRange charRange) {
        try {
            handles.hVoidRange().invokeExact(peer, ObjC.sel("ensureGlyphsForCharacterRange:"), charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("ensureGlyphsForCharacterRange: failed", t);
        }
    }

    /// [layoutManager ensureGlyphsForGlyphRange:]
    public void ensureGlyphsForGlyphRange(NSRange glyphRange) {
        try {
            handles.hVoidRange().invokeExact(peer, ObjC.sel("ensureGlyphsForGlyphRange:"), glyphRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("ensureGlyphsForGlyphRange: failed", t);
        }
    }

    /// [layoutManager ensureLayoutForCharacterRange:]
    public void ensureLayoutForCharacterRange(NSRange charRange) {
        try {
            handles.hVoidRange().invokeExact(peer, ObjC.sel("ensureLayoutForCharacterRange:"), charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("ensureLayoutForCharacterRange: failed", t);
        }
    }

    /// [layoutManager ensureLayoutForGlyphRange:]
    public void ensureLayoutForGlyphRange(NSRange glyphRange) {
        try {
            handles.hVoidRange().invokeExact(peer, ObjC.sel("ensureLayoutForGlyphRange:"), glyphRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("ensureLayoutForGlyphRange: failed", t);
        }
    }

    /// [layoutManager ensureLayoutForBoundingRect:inTextContainer:]
    public void ensureLayoutForBoundingRectInTextContainer(NSRect bounds, NSTextContainer container) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.ID));
            h.invokeExact(peer, ObjC.sel("ensureLayoutForBoundingRect:inTextContainer:"),
                    bounds.toSegment(), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("ensureLayoutForBoundingRect:inTextContainer: failed", t);
        }
    }

    // -setGlyphs:properties:characterIndexes:font:forGlyphRange: omitted (raw C pointers).
    // -getGlyphsInRange:... / -getLineFragmentInsertionPoints... / block enumeration /
    // drawing primitives omitted (pointers/blocks/multi-arg shapes) — reported.

    /// [layoutManager CGGlyphAtIndex:] -> CGGlyph (uint32 widened to long, macOS 10.11+).
    public long cgGlyphAtIndex(long glyphIndex) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("CGGlyphAtIndex:"), glyphIndex);
        } catch (Throwable t) {
            throw new RuntimeException("CGGlyphAtIndex: failed", t);
        }
    }

    /// [layoutManager isValidGlyphIndex:] (macOS 10.0+/iOS 7+).
    public boolean isValidGlyphIndex(long glyphIndex) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
            return (boolean) h.invokeExact(peer, ObjC.sel("isValidGlyphIndex:"), glyphIndex);
        } catch (Throwable t) {
            throw new RuntimeException("isValidGlyphIndex: failed", t);
        }
    }

    /// [layoutManager propertyForGlyphAtIndex:] -> NSGlyphProperty (long, macOS 10.5+).
    public long propertyForGlyphAtIndex(long glyphIndex) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("propertyForGlyphAtIndex:"), glyphIndex);
        } catch (Throwable t) {
            throw new RuntimeException("propertyForGlyphAtIndex: failed", t);
        }
    }

    /// [layoutManager textContainerForGlyphAtIndex:effectiveRange:] — effectiveRangeOut
    /// is NSRange* (nullable; pass NULL to ignore) -> NSTextContainer (may be nil).
    public NSTextContainer textContainerForGlyphAtIndexEffectiveRange(long glyphIndex, MemorySegment effectiveRangeOutOrNull) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("textContainerForGlyphAtIndex:effectiveRange:"), glyphIndex,
                    (MemorySegment) (effectiveRangeOutOrNull == null ? MemorySegment.NULL : effectiveRangeOutOrNull));
            return NSTextContainer.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("textContainerForGlyphAtIndex:effectiveRange: failed", t);
        }
    }

    /// Convenience without effectiveRange.
    public NSTextContainer textContainerForGlyphAtIndex(long glyphIndex) {
        return textContainerForGlyphAtIndexEffectiveRange(glyphIndex, null);
    }

    // -lineFragmentRectForGlyphAtIndex:... / -lineFragmentUsedRectForGlyphAtIndex:... /
    // -boundingRectForGlyphRange:... / -glyphRangeForBoundingRect:... /
    // -glyphIndexForPoint:... / -characterIndexForPoint:... / -locationForGlyphAtIndex: /
    // -rangeOfNominallySpacedGlyphsContainingIndex: / -glyphRangeForCharacterRange:... /
    // -characterRangeForGlyphRange:... / -setLineFragmentRect:... / -setLocation:... /
    // -setAttachmentSize:... omitted: RECT/POINT-struct or out-pointer shapes with no
    // vocabulary entry (grep MISS set) — reported.

    /// [layoutManager extraLineFragmentRect] -> NSRect (readonly).
    public NSRect extraLineFragmentRect() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RECT));
            MemorySegment r = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("extraLineFragmentRect"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("extraLineFragmentRect failed", t);
        }
    }

    /// [layoutManager extraLineFragmentUsedRect] -> NSRect (readonly).
    public NSRect extraLineFragmentUsedRect() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RECT));
            MemorySegment r = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("extraLineFragmentUsedRect"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("extraLineFragmentUsedRect failed", t);
        }
    }

    /// [layoutManager extraLineFragmentTextContainer] -> NSTextContainer (may be nil).
    public NSTextContainer extraLineFragmentTextContainer() {
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(peer, ObjC.sel("extraLineFragmentTextContainer"));
            return NSTextContainer.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("extraLineFragmentTextContainer failed", t);
        }
    }

    /// [layoutManager getFirstUnlaid...] out-pointer form omitted; scalar forms below.
    /// [layoutManager firstUnlaidCharacterIndex] -> NSUInteger.
    public long firstUnlaidCharacterIndex() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, ObjC.sel("firstUnlaidCharacterIndex"));
        } catch (Throwable t) {
            throw new RuntimeException("firstUnlaidCharacterIndex failed", t);
        }
    }

    /// [layoutManager firstUnlaidGlyphIndex] -> NSUInteger.
    public long firstUnlaidGlyphIndex() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, ObjC.sel("firstUnlaidGlyphIndex"));
        } catch (Throwable t) {
            throw new RuntimeException("firstUnlaidGlyphIndex failed", t);
        }
    }

    /// [layoutManager setTextContainer:forGlyphRange:]
    public void setTextContainerForGlyphRange(NSTextContainer container, NSRange glyphRange) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("setTextContainer:forGlyphRange:"),
                    (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()), glyphRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setTextContainer:forGlyphRange: failed", t);
        }
    }

    /// [layoutManager setNotShownAttribute:forGlyphAtIndex:]
    public void setNotShownAttributeForGlyphAtIndex(boolean flag, long glyphIndex) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL, Arg.INT));
            h.invokeExact(peer, ObjC.sel("setNotShownAttribute:forGlyphAtIndex:"), flag, glyphIndex);
        } catch (Throwable t) {
            throw new RuntimeException("setNotShownAttribute:forGlyphAtIndex: failed", t);
        }
    }

    /// [layoutManager setDrawsOutsideLineFragment:forGlyphAtIndex:]
    public void setDrawsOutsideLineFragmentForGlyphAtIndex(boolean flag, long glyphIndex) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL, Arg.INT));
            h.invokeExact(peer, ObjC.sel("setDrawsOutsideLineFragment:forGlyphAtIndex:"), flag, glyphIndex);
        } catch (Throwable t) {
            throw new RuntimeException("setDrawsOutsideLineFragment:forGlyphAtIndex: failed", t);
        }
    }

    // ---- temporary attributes ----

    /// [layoutManager temporaryAttributesAtCharacterIndex:effectiveRange:] — rangeOut nullable.
    public MemorySegment temporaryAttributesAtCharacterIndexEffectiveRange(long charIndex, MemorySegment effectiveRangeOutOrNull) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID));
            return (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("temporaryAttributesAtCharacterIndex:effectiveRange:"), charIndex,
                    (MemorySegment) (effectiveRangeOutOrNull == null ? MemorySegment.NULL : effectiveRangeOutOrNull));
        } catch (Throwable t) {
            throw new RuntimeException("temporaryAttributesAtCharacterIndex:effectiveRange: failed", t);
        }
    }

    /// [layoutManager setTemporaryAttributes:forCharacterRange:] — attrs is NSDictionary*.
    public void setTemporaryAttributesForCharacterRange(MemorySegment attrs, NSRange charRange) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("setTemporaryAttributes:forCharacterRange:"),
                    (MemorySegment) (attrs == null ? MemorySegment.NULL : attrs), charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setTemporaryAttributes:forCharacterRange: failed", t);
        }
    }

    /// [layoutManager addTemporaryAttributes:forCharacterRange:] — attrs is NSDictionary*.
    public void addTemporaryAttributesForCharacterRange(MemorySegment attrs, NSRange charRange) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("addTemporaryAttributes:forCharacterRange:"),
                    (MemorySegment) (attrs == null ? MemorySegment.NULL : attrs), charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("addTemporaryAttributes:forCharacterRange: failed", t);
        }
    }

    /// [layoutManager removeTemporaryAttribute:forCharacterRange:]
    public void removeTemporaryAttributeForCharacterRange(String attrName, NSRange charRange) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("removeTemporaryAttribute:forCharacterRange:"),
                    ObjC.nsstring(attrName == null ? "" : attrName), charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("removeTemporaryAttribute:forCharacterRange: failed", t);
        }
    }

    /// [layoutManager temporaryAttribute:atCharacterIndex:effectiveRange:] — rangeOut nullable.
    public MemorySegment temporaryAttributeAtCharacterIndexEffectiveRange(String attrName, long location, MemorySegment effectiveRangeOutOrNull) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.INT, Arg.ID));
            return (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("temporaryAttribute:atCharacterIndex:effectiveRange:"),
                    ObjC.nsstring(attrName == null ? "" : attrName), location,
                    (MemorySegment) (effectiveRangeOutOrNull == null ? MemorySegment.NULL : effectiveRangeOutOrNull));
        } catch (Throwable t) {
            throw new RuntimeException("temporaryAttribute:atCharacterIndex:effectiveRange: failed", t);
        }
    }

    // -temporaryAttribute:...longestEffectiveRange:inRange: / -temporaryAttributes...inRange:
    // omitted: (ID,INT,ID,RANGE)->ID has no vocabulary entry — reported.

    /// [layoutManager addTemporaryAttribute:value:forCharacterRange:] (macOS 10.5+).
    public void addTemporaryAttributeValueForCharacterRange(String attrName, MemorySegment value, NSRange charRange) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("addTemporaryAttribute:value:forCharacterRange:"),
                    ObjC.nsstring(attrName == null ? "" : attrName),
                    (MemorySegment) (value == null ? MemorySegment.NULL : value), charRange.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("addTemporaryAttribute:value:forCharacterRange: failed", t);
        }
    }

    // ---- font metrics ----

    /// [layoutManager defaultLineHeightForFont:] -> CGFloat (font is NSFont*).
    public double defaultLineHeightForFont(NSFont font) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE, Arg.ID));
            return (double) h.invokeExact(peer, ObjC.sel("defaultLineHeightForFont:"),
                    (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("defaultLineHeightForFont: failed", t);
        }
    }

    /// [layoutManager defaultBaselineOffsetForFont:] -> CGFloat (font is NSFont*).
    public double defaultBaselineOffsetForFont(NSFont font) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE, Arg.ID));
            return (double) h.invokeExact(peer, ObjC.sel("defaultBaselineOffsetForFont:"),
                    (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("defaultBaselineOffsetForFont: failed", t);
        }
    }

    // ---- ruler / responder ----

    /// [layoutManager rulerMarkersForTextView:paragraphStyle:ruler:] -> NSArray (may be nil).
    public NSArray rulerMarkersForTextViewParagraphStyleRuler(MemorySegment textView, MemorySegment paragraphStyle, MemorySegment ruler) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("rulerMarkersForTextView:paragraphStyle:ruler:"),
                    (MemorySegment) (textView == null ? MemorySegment.NULL : textView),
                    (MemorySegment) (paragraphStyle == null ? MemorySegment.NULL : paragraphStyle),
                    (MemorySegment) (ruler == null ? MemorySegment.NULL : ruler));
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("rulerMarkersForTextView:paragraphStyle:ruler: failed", t);
        }
    }

    // -rulerAccessoryViewForTextView:...enabled: omitted (4-arg shape) — reported.

    /// [layoutManager layoutManagerOwnsFirstResponderInWindow:] — window is NSWindow*.
    public boolean layoutManagerOwnsFirstResponderInWindow(MemorySegment window) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("layoutManagerOwnsFirstResponderInWindow:"),
                    (MemorySegment) (window == null ? MemorySegment.NULL : window));
        } catch (Throwable t) {
            throw new RuntimeException("layoutManagerOwnsFirstResponderInWindow: failed", t);
        }
    }

    /// [layoutManager firstTextView] -> NSTextView (may be nil).
    public NSTextView firstTextView() {
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(peer, ObjC.sel("firstTextView"));
            return NSTextView.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("firstTextView failed", t);
        }
    }

    /// [layoutManager textViewForBeginningOfSelection] -> NSTextView (may be nil).
    public NSTextView textViewForBeginningOfSelection() {
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(peer, ObjC.sel("textViewForBeginningOfSelection"));
            return NSTextView.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("textViewForBeginningOfSelection failed", t);
        }
    }

    // SDK omissions: NSLayoutManagerDelegate protocol (upcall machinery); deprecated
    // NSGlyph-era API skipped.
}
