package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSAttributedString — immutable attributed string.
/// Thin 1:1 wrapper over native `NSAttributedString`: every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer.
/// Follows FFM pattern: no reflection, cached handles, ensureInit.
public class NSAttributedString extends NSObject {

            private record Handles(MethodHandle hInitString, MethodHandle hInitStringAttrs, MethodHandle hLength, MethodHandle hString, MethodHandle hAttr, MethodHandle hAttrDictAt) {}
    private static volatile Handles handles;

    protected NSAttributedString(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSAttributedString wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSAttributedString(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID))
        );
    }

    /// `[[NSAttributedString alloc] initWithString:string]`
    public static NSAttributedString create(String s) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInitString().invokeExact(alloc, ObjC.sel("initWithString:"), ObjC.nsstring(s));
            if (p.address() == 0) throw new IllegalStateException("NSAttributedString initWithString: returned nil");
            return new NSAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithString: failed", t);
        }
    }

    /// `[[NSAttributedString alloc] initWithString:string attributes:dict]` — dict may be NULL.
    public static NSAttributedString create(String s, MemorySegment attributes) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("alloc"));
        try {
            MemorySegment attrs = (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes);
            MemorySegment p = (MemorySegment) handles.hInitStringAttrs().invokeExact(alloc, ObjC.sel("initWithString:attributes:"), ObjC.nsstring(s), attrs);
            if (p.address() == 0) throw new IllegalStateException("NSAttributedString initWithString:attributes: returned nil");
            return new NSAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithString:attributes: failed", t);
        }
    }

    /// [attributedString length] -> NSUInteger
    public long length() {
        ensureInit();
        try {
            return (long) handles.hLength().invokeExact(peer, ObjC.sel("length"));
        } catch (Throwable t) {
            throw new RuntimeException("length failed", t);
        }
    }

    /// [attributedString string] -> NSString -> String
    public String string() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hString().invokeExact(peer, ObjC.sel("string"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("string failed", t);
        }
    }

    /// [attributedString attribute:name atIndex:index effectiveRange:rangePtr]
    /// @param attrName attribute name (e.g. NSFontAttributeName)
    /// @param index character index
    /// @param effectiveRangeOut 16-byte NSRange* out buffer or NULL — if non-null, filled with effective range
    /// @return attribute value as MemorySegment (id) or NULL
    public MemorySegment attribute(String attrName, long index, MemorySegment effectiveRangeOut) {
        ensureInit();
        if (effectiveRangeOut != null && effectiveRangeOut.address() != 0 && effectiveRangeOut.byteSize() < 16)
            throw new IllegalArgumentException("attribute:atIndex:effectiveRange: effectiveRangeOut must be null or >= 16 bytes (got " + effectiveRangeOut.byteSize() + ")");
        try {
            MemorySegment range = (MemorySegment) (effectiveRangeOut == null ? MemorySegment.NULL : effectiveRangeOut);
            return (MemorySegment) handles.hAttr().invokeExact(peer, ObjC.sel("attribute:atIndex:effectiveRange:"), ObjC.nsstring(attrName), index, range);
        } catch (Throwable t) {
            throw new RuntimeException("attribute:atIndex:effectiveRange: failed", t);
        }
    }

    /// Convenience without effectiveRange.
    public MemorySegment attribute(String attrName, long index) {
        return attribute(attrName, index, null);
    }

    /// Typed helper that returns attribute and fills NSRange if requested.
    /// @param effectiveRange capsule for out range; pass null to ignore
    public MemorySegment attributeAtIndexEffectiveRange(String attrName, long index, MemorySegment effectiveRangeOut) {
        return attribute(attrName, index, effectiveRangeOut);
    }

    /// [attributedString attributesAtIndex:effectiveRange:] -> NSDictionary*
    public MemorySegment attributesAtIndexEffectiveRange(long index, MemorySegment effectiveRangeOut) {
        ensureInit();
        if (effectiveRangeOut != null && effectiveRangeOut.address() != 0 && effectiveRangeOut.byteSize() < 16)
            throw new IllegalArgumentException("attributesAtIndex:effectiveRange: effectiveRangeOut must be null or >= 16 bytes (got " + effectiveRangeOut.byteSize() + ")");
        try {
            MemorySegment range = (MemorySegment) (effectiveRangeOut == null ? MemorySegment.NULL : effectiveRangeOut);
            return (MemorySegment) handles.hAttrDictAt().invokeExact(peer, ObjC.sel("attributesAtIndex:effectiveRange:"), index, range);
        } catch (Throwable t) {
            throw new RuntimeException("attributesAtIndex:effectiveRange: failed", t);
        }
    }

    public MemorySegment attributesAtIndex(long index) {
        return attributesAtIndexEffectiveRange(index, null);
    }

    /// [attributedString attributedSubstringFromRange:] -> NSAttributedString
    public NSAttributedString attributedSubstring(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("attributedSubstringFromRange:"), range.toSegment());
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("attributedSubstringFromRange: failed", t);
        }
    }

    /// [attributedString rangeOfTextBlock:atIndex:] -> NSRange (block is NSTextBlock*).
    public NSRange rangeOfTextBlock(MemorySegment block, long location) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer,
                    ObjC.sel("rangeOfTextBlock:atIndex:"),
                    (MemorySegment) (block == null ? MemorySegment.NULL : block), location);
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeOfTextBlock:atIndex: failed", t);
        }
    }

    /// [attributedString rangeOfTextTable:atIndex:] -> NSRange (table is NSTextTable*).
    public NSRange rangeOfTextTable(MemorySegment table, long location) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer,
                    ObjC.sel("rangeOfTextTable:atIndex:"),
                    (MemorySegment) (table == null ? MemorySegment.NULL : table), location);
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeOfTextTable:atIndex: failed", t);
        }
    }

    /// [attributedString rangeOfTextList:atIndex:] -> NSRange (list is NSTextList*).
    public NSRange rangeOfTextList(MemorySegment list, long location) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer,
                    ObjC.sel("rangeOfTextList:atIndex:"),
                    (MemorySegment) (list == null ? MemorySegment.NULL : list), location);
            return NSRange.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("rangeOfTextList:atIndex: failed", t);
        }
    }

    /// `+[NSAttributedString textTypes]` — pasteboard types loadable as attributed strings.
    public static NSArray textTypes() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("textTypes")));
    }

    /// `+[NSAttributedString textUnfilteredTypes]` — unfiltered pasteboard types.
    public static NSArray textUnfilteredTypes() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("textUnfilteredTypes")));
    }

    /// `[[NSAttributedString alloc] initWithRTF:documentAttributes:]` — dictOut may be NULL.
    public static NSAttributedString createWithRTF(MemorySegment rtfData, MemorySegment dictOutOrNull) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("alloc"));
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(alloc, ObjC.sel("initWithRTF:documentAttributes:"),
                    (MemorySegment) (rtfData == null ? MemorySegment.NULL : rtfData),
                    (MemorySegment) (dictOutOrNull == null ? MemorySegment.NULL : dictOutOrNull));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithRTF:documentAttributes: failed", t);
        }
    }

    /// `[[NSAttributedString alloc] initWithHTML:documentAttributes:]` — dictOut may be NULL.
    public static NSAttributedString createWithHTML(MemorySegment htmlData, MemorySegment dictOutOrNull) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("alloc"));
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(alloc, ObjC.sel("initWithHTML:documentAttributes:"),
                    (MemorySegment) (htmlData == null ? MemorySegment.NULL : htmlData),
                    (MemorySegment) (dictOutOrNull == null ? MemorySegment.NULL : dictOutOrNull));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithHTML:documentAttributes: failed", t);
        }
    }

    /// `[[NSAttributedString alloc] initWithHTML:baseURL:documentAttributes:]`.
    public static NSAttributedString createWithHTMLBaseURL(MemorySegment htmlData, MemorySegment baseURL, MemorySegment dictOutOrNull) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("alloc"));
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(alloc, ObjC.sel("initWithHTML:baseURL:documentAttributes:"),
                    (MemorySegment) (htmlData == null ? MemorySegment.NULL : htmlData),
                    (MemorySegment) (baseURL == null ? MemorySegment.NULL : baseURL),
                    (MemorySegment) (dictOutOrNull == null ? MemorySegment.NULL : dictOutOrNull));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithHTML:baseURL:documentAttributes: failed", t);
        }
    }

    /// `[[NSAttributedString alloc] initWithDocFormat:documentAttributes:]`.
    public static NSAttributedString createWithDocFormat(MemorySegment docData, MemorySegment dictOutOrNull) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("alloc"));
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(alloc, ObjC.sel("initWithDocFormat:documentAttributes:"),
                    (MemorySegment) (docData == null ? MemorySegment.NULL : docData),
                    (MemorySegment) (dictOutOrNull == null ? MemorySegment.NULL : dictOutOrNull));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithDocFormat:documentAttributes: failed", t);
        }
    }

    /// [attributedString RTFFromRange:documentAttributes:] -> NSData* (raw segment).
    public MemorySegment rtfFromRange(NSRange range, MemorySegment docAttributes) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("RTFFromRange:documentAttributes:"),
                    range.toSegment(), (MemorySegment) (docAttributes == null ? MemorySegment.NULL : docAttributes));
        } catch (Throwable t) {
            throw new RuntimeException("RTFFromRange:documentAttributes: failed", t);
        }
    }

    /// [attributedString RTFDFromRange:documentAttributes:] -> NSData* (raw segment).
    public MemorySegment rtfdFromRange(NSRange range, MemorySegment docAttributes) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("RTFDFromRange:documentAttributes:"),
                    range.toSegment(), (MemorySegment) (docAttributes == null ? MemorySegment.NULL : docAttributes));
        } catch (Throwable t) {
            throw new RuntimeException("RTFDFromRange:documentAttributes: failed", t);
        }
    }

    /// [attributedString docFormatFromRange:documentAttributes:] -> NSData* (raw segment).
    public MemorySegment docFormatFromRange(NSRange range, MemorySegment docAttributes) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("docFormatFromRange:documentAttributes:"),
                    range.toSegment(), (MemorySegment) (docAttributes == null ? MemorySegment.NULL : docAttributes));
        } catch (Throwable t) {
            throw new RuntimeException("docFormatFromRange:documentAttributes: failed", t);
        }
    }

    /// [attributedString isEqualToAttributedString:]
    public boolean isEqualToAttributedString(NSAttributedString other) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToAttributedString:"), (MemorySegment) (other == null || other.peer() == null || other.peer().address() == 0 ? MemorySegment.NULL : other.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("isEqualToAttributedString: failed", t);
        }
    }

    /// `[[NSAttributedString alloc] initWithAttributedString:attrStr]`
    public static NSAttributedString create(NSAttributedString attrStr) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("alloc"));
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(alloc, ObjC.sel("initWithAttributedString:"),
                    (MemorySegment) (attrStr == null ? MemorySegment.NULL : attrStr.peer()));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSAttributedString initWithAttributedString: returned nil");
            return new NSAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithAttributedString: failed", t);
        }
    }

    /// [attributedString fontAttributesInRange:] -> NSDictionary* ("copy font" attributes).
    public MemorySegment fontAttributesInRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("fontAttributesInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("fontAttributesInRange: failed", t);
        }
    }

    /// [attributedString rulerAttributesInRange:] -> NSDictionary* ("copy ruler" attributes).
    public MemorySegment rulerAttributesInRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("rulerAttributesInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("rulerAttributesInRange: failed", t);
        }
    }

    /// [attributedString containsAttachmentsInRange:] — YES if an attachment sits in range.
    public boolean containsAttachmentsInRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.RANGE));
            return (boolean) h.invokeExact(peer, ObjC.sel("containsAttachmentsInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("containsAttachmentsInRange: failed", t);
        }
    }

    /// [attributedString containsAttachments] — YES if any attachment exists.
    public boolean containsAttachments() {
        return ObjC.msgSendBool(peer, ObjC.sel("containsAttachments"));
    }

    // SDK omissions (no vocabulary shape — verified by grep in Sig.java, reported):
    // -doubleClickAtIndex: / -nextWordFromIndex:forward: / -lineBreakBeforeIndex:withinRange: /
    // -lineBreakByHyphenatingBeforeIndex:withinRange: / -itemNumberInTextList:atIndex:
    // (RANGE/INT return with INT+RANGE args); -attributesAtIndex:longestEffectiveRange:inRange:
    // and -attribute:atIndex:longestEffectiveRange:inRange: (ID,INT,ID,RANGE); block-based
    // -enumerateAttributesInRange:... / -enumerateAttribute:... (upcall blocks); NSError**
    // document I/O (-initWithData:.../ -dataFromRange:.../ -fileWrapperFromRange:...).

    /// [attributedString mutableCopy] -> NSMutableAttributedString
    public NSMutableAttributedString mutableCopy() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("mutableCopy"));
            return NSMutableAttributedString.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("mutableCopy failed", t);
        }
    }
}
