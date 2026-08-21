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

        private static synchronized void ensureInit() {
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
