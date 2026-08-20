package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSAttributedString — immutable attributed string.
 * Thin 1:1 wrapper over native {@code NSAttributedString}: every method maps to one
 * {@code objc_msgSend} selector, no cached Java state beyond the peer.
 * Follows FFM pattern: no reflection, cached handles, ensureInit.
 */
public class NSAttributedString extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hInitString;      // (id, SEL, id) -> id  initWithString:
    private static MethodHandle hInitStringAttrs; // (id, SEL, id, id) -> id  initWithString:attributes:
    private static MethodHandle hLength;          // (id, SEL) -> long  length
    private static MethodHandle hString;          // (id, SEL) -> id  string
    private static MethodHandle hAttr;            // (id, SEL, id, long, id) -> id  attribute:atIndex:effectiveRange:
    private static MethodHandle hAttrDictAt;      // (id, SEL, long, id) -> id  attributesAtIndex:effectiveRange:

    protected NSAttributedString(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSAttributedString wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSAttributedString(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitString = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hInitStringAttrs = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
        hLength = ObjC.handle(Sig.of(Ret.INT));
        hString = ObjC.handle(Sig.of(Ret.ID));
        hAttr = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.INT, Arg.ID));
        hAttrDictAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID));
        initialized = true;
    }

    /** {@code [[NSAttributedString alloc] initWithString:string]} */
    public static NSAttributedString create(String s) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) hInitString.invokeExact(alloc, ObjC.sel("initWithString:"), ObjC.nsstring(s));
            if (p.address() == 0) throw new IllegalStateException("NSAttributedString initWithString: returned nil");
            return new NSAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithString: failed", t);
        }
    }

    /** {@code [[NSAttributedString alloc] initWithString:string attributes:dict]} — dict may be NULL. */
    public static NSAttributedString create(String s, MemorySegment attributes) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSAttributedString"), ObjC.sel("alloc"));
        try {
            MemorySegment attrs = (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes);
            MemorySegment p = (MemorySegment) hInitStringAttrs.invokeExact(alloc, ObjC.sel("initWithString:attributes:"), ObjC.nsstring(s), attrs);
            if (p.address() == 0) throw new IllegalStateException("NSAttributedString initWithString:attributes: returned nil");
            return new NSAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithString:attributes: failed", t);
        }
    }

    /** [attributedString length] -> NSUInteger */
    public long length() {
        ensureInit();
        try {
            return (long) hLength.invokeExact(peer, ObjC.sel("length"));
        } catch (Throwable t) {
            throw new RuntimeException("length failed", t);
        }
    }

    /** [attributedString string] -> NSString -> String */
    public String string() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hString.invokeExact(peer, ObjC.sel("string"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("string failed", t);
        }
    }

    /**
     * [attributedString attribute:name atIndex:index effectiveRange:rangePtr]
     * @param attrName attribute name (e.g. NSFontAttributeName)
     * @param index character index
     * @param effectiveRangeOut 16-byte NSRange* out buffer or NULL — if non-null, filled with effective range
     * @return attribute value as MemorySegment (id) or NULL
     */
    public MemorySegment attribute(String attrName, long index, MemorySegment effectiveRangeOut) {
        ensureInit();
        try {
            MemorySegment range = (MemorySegment) (effectiveRangeOut == null ? MemorySegment.NULL : effectiveRangeOut);
            return (MemorySegment) hAttr.invokeExact(peer, ObjC.sel("attribute:atIndex:effectiveRange:"), ObjC.nsstring(attrName), index, range);
        } catch (Throwable t) {
            throw new RuntimeException("attribute:atIndex:effectiveRange: failed", t);
        }
    }

    /** Convenience without effectiveRange. */
    public MemorySegment attribute(String attrName, long index) {
        return attribute(attrName, index, null);
    }

    /**
     * Typed helper that returns attribute and fills NSRange if requested.
     * @param effectiveRange capsule for out range; pass null to ignore
     */
    public MemorySegment attributeAtIndexEffectiveRange(String attrName, long index, MemorySegment effectiveRangeOut) {
        return attribute(attrName, index, effectiveRangeOut);
    }

    /** [attributedString attributesAtIndex:effectiveRange:] -> NSDictionary* */
    public MemorySegment attributesAtIndexEffectiveRange(long index, MemorySegment effectiveRangeOut) {
        ensureInit();
        try {
            MemorySegment range = (MemorySegment) (effectiveRangeOut == null ? MemorySegment.NULL : effectiveRangeOut);
            return (MemorySegment) hAttrDictAt.invokeExact(peer, ObjC.sel("attributesAtIndex:effectiveRange:"), index, range);
        } catch (Throwable t) {
            throw new RuntimeException("attributesAtIndex:effectiveRange: failed", t);
        }
    }

    public MemorySegment attributesAtIndex(long index) {
        return attributesAtIndexEffectiveRange(index, null);
    }

    /** [attributedString attributedSubstringFromRange:] -> NSAttributedString */
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

    /** [attributedString isEqualToAttributedString:] */
    public boolean isEqualToAttributedString(NSAttributedString other) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToAttributedString:"), (MemorySegment) (other == null || other.peer() == null || other.peer().address() == 0 ? MemorySegment.NULL : other.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("isEqualToAttributedString: failed", t);
        }
    }

    /** [attributedString mutableCopy] -> NSMutableAttributedString */
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
