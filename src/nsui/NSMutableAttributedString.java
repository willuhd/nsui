package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMutableAttributedString — mutable attributed string.
/// Thin 1:1 wrapper over native `NSMutableAttributedString`: every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer.
/// Follows FFM pattern: no reflection, cached handles, ensureInit.
public class NSMutableAttributedString extends NSAttributedString {

    private record Handles(MethodHandle hInitString, MethodHandle hInitStringAttrs, MethodHandle hAddAttr, MethodHandle hAppend, MethodHandle hSetAttr) {}
    private static volatile Handles handles;

    protected NSMutableAttributedString(MemorySegment peer) {
        super(peer);
        ensureMutInit();
    }

    public static NSMutableAttributedString wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMutableAttributedString(peer);
    }

    private static void ensureMutInit() {
        if (handles != null) return;
        ensureMutInitLocked();
    }

    private static synchronized void ensureMutInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.RANGE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE))
        );
    }

    /// `[[NSMutableAttributedString alloc] initWithString:string]`
    public static NSMutableAttributedString create(String s) {
        ensureMutInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSMutableAttributedString"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInitString().invokeExact(alloc, ObjC.sel("initWithString:"), ObjC.nsstring(s));
            if (p.address() == 0) throw new IllegalStateException("NSMutableAttributedString initWithString: returned nil");
            return new NSMutableAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithString: failed for NSMutableAttributedString", t);
        }
    }

    /// `[[NSMutableAttributedString alloc] initWithString:string attributes:dict]`
    public static NSMutableAttributedString create(String s, MemorySegment attributes) {
        ensureMutInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSMutableAttributedString"), ObjC.sel("alloc"));
        try {
            MemorySegment attrs = (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes);
            MemorySegment p = (MemorySegment) handles.hInitStringAttrs().invokeExact(alloc, ObjC.sel("initWithString:attributes:"), ObjC.nsstring(s), attrs);
            if (p.address() == 0) throw new IllegalStateException("NSMutableAttributedString initWithString:attributes: returned nil");
            return new NSMutableAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithString:attributes: failed for NSMutableAttributedString", t);
        }
    }

    /// `[[NSMutableAttributedString alloc] initWithAttributedString:attrStr]`
    public static NSMutableAttributedString create(NSAttributedString attrStr) {
        ensureMutInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSMutableAttributedString"), ObjC.sel("alloc"));
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(alloc, ObjC.sel("initWithAttributedString:"),
                    (MemorySegment) (attrStr == null ? MemorySegment.NULL : attrStr.peer()));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSMutableAttributedString initWithAttributedString: returned nil");
            return new NSMutableAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithAttributedString: failed for NSMutableAttributedString", t);
        }
    }

    /// [mutable replaceCharactersInRange:withString:]
    public void replaceCharactersInRangeWithString(NSRange range, String str) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("replaceCharactersInRange:withString: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("replaceCharactersInRange:withString: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceCharactersInRange:withString:"),
                    range.toSegment(), ObjC.nsstring(str == null ? "" : str));
        } catch (Throwable t) {
            throw new RuntimeException("replaceCharactersInRange:withString: failed", t);
        }
    }

    /// [mutable replaceCharactersInRange:withAttributedString:]
    public void replaceCharactersInRangeWithAttributedString(NSRange range, NSAttributedString attrStr) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("replaceCharactersInRange:withAttributedString: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("replaceCharactersInRange:withAttributedString: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceCharactersInRange:withAttributedString:"),
                    range.toSegment(), (MemorySegment) (attrStr == null ? MemorySegment.NULL : attrStr.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("replaceCharactersInRange:withAttributedString: failed", t);
        }
    }

    /// [mutable insertAttributedString:atIndex:]
    public void insertAttributedString(NSAttributedString attrStr, long index) {
        ensureMutInit();
        long n = length();
        if (index < 0 || index > n)
            throw new IllegalArgumentException("insertAttributedString:atIndex: index " + index + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("insertAttributedString:atIndex:"),
                    (MemorySegment) (attrStr == null ? MemorySegment.NULL : attrStr.peer()), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertAttributedString:atIndex: failed", t);
        }
    }

    /// [mutable deleteCharactersInRange:]
    public void deleteCharactersInRange(NSRange range) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("deleteCharactersInRange: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("deleteCharactersInRange: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("deleteCharactersInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("deleteCharactersInRange: failed", t);
        }
    }

    /// [mutable setAttributedString:] — replace the whole contents.
    public void setAttributedString(NSAttributedString attrStr) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAttributedString:"),
                (MemorySegment) (attrStr == null ? MemorySegment.NULL : attrStr.peer()));
    }

    /// [mutable beginEditing] — batch changes; must be balanced with endEditing.
    public void beginEditing() {
        ObjC.msgSendVoid(peer, ObjC.sel("beginEditing"));
    }

    /// [mutable endEditing]
    public void endEditing() {
        ObjC.msgSendVoid(peer, ObjC.sel("endEditing"));
    }

    /// [mutable mutableString] -> NSMutableString* (raw segment).
    public MemorySegment mutableString() {
        return ObjC.msgSendId(peer, ObjC.sel("mutableString"));
    }

    // ---- AppKit attribute fixing ----

    /// [mutable fixAttributesInRange:] — fix font/paragraph/attachment inconsistencies.
    public void fixAttributesInRange(NSRange range) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("fixAttributesInRange: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("fixAttributesInRange: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("fixAttributesInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("fixAttributesInRange: failed", t);
        }
    }

    /// [mutable fixFontAttributeInRange:]
    public void fixFontAttributeInRange(NSRange range) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("fixFontAttributeInRange: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("fixFontAttributeInRange: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("fixFontAttributeInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("fixFontAttributeInRange: failed", t);
        }
    }

    /// [mutable fixParagraphStyleAttributeInRange:]
    public void fixParagraphStyleAttributeInRange(NSRange range) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("fixParagraphStyleAttributeInRange: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("fixParagraphStyleAttributeInRange: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("fixParagraphStyleAttributeInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("fixParagraphStyleAttributeInRange: failed", t);
        }
    }

    /// [mutable fixAttachmentAttributeInRange:]
    public void fixAttachmentAttributeInRange(NSRange range) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("fixAttachmentAttributeInRange: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("fixAttachmentAttributeInRange: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("fixAttachmentAttributeInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("fixAttachmentAttributeInRange: failed", t);
        }
    }

    // ---- AppKit script/style conveniences ----

    /// [mutable superscriptRange:]
    public void superscriptRange(NSRange range) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("superscriptRange: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("superscriptRange: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("superscriptRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("superscriptRange: failed", t);
        }
    }

    /// [mutable subscriptRange:]
    public void subscriptRange(NSRange range) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("subscriptRange: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("subscriptRange: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("subscriptRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("subscriptRange: failed", t);
        }
    }

    /// [mutable unscriptRange:]
    public void unscriptRange(NSRange range) {
        ensureMutInit();
        if (range == null) throw new IllegalArgumentException("unscriptRange: null range");
        long n = length();
        if (range.location() < 0 || range.length() < 0 || range.location() > n || range.length() > n - range.location())
            throw new IllegalArgumentException("unscriptRange: range " + range + " out of bounds (length " + n + ")");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("unscriptRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("unscriptRange: failed", t);
        }
    }

    // SDK omissions (no vocabulary shape — verified by grep in Sig.java, reported):
    // -applyFontTraits:range: / -setAlignment:range: / -setBaseWritingDirection:range:
    // (VOID,INT,RANGE); deprecated -readFromURL:.../-readFromData:... (NSError-style
    // multi-id BOOL returns); variadic -appendLocalizedFormat:.

    /// [mutable appendAttributedString:other] — also satisfies task's "append" requirement
    public void append(NSAttributedString other) {
        ensureMutInit();
        try {
            handles.hAppend().invokeExact(peer, ObjC.sel("appendAttributedString:"), (MemorySegment) ((MemorySegment) (other == null ? MemorySegment.NULL : other.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("appendAttributedString: failed", t);
        }
    }

    /// Alias per task description: append
    public void appendAttributedString(NSAttributedString other) { append(other); }

    /// Convenience append with plain string
    public void appendString(String s) {
        append(NSAttributedString.create(s));
    }

    /// [mutable addAttribute:name value:value range:range]
    /// @param name attribute name (e.g. NSFontAttributeName)
    /// @param value attribute value as id (MemorySegment) — pass NSFont.peer(), NSColor.peer(), etc.
    /// @param range range to apply
    public void addAttribute(String name, MemorySegment value, NSRange range) {
        ensureMutInit();
        try {
            MemorySegment v = (MemorySegment) (value == null ? MemorySegment.NULL : value);
            handles.hAddAttr().invokeExact(peer, ObjC.sel("addAttribute:value:range:"), ObjC.nsstring(name), v, range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("addAttribute:value:range: failed", t);
        }
    }

    /// Convenience with location/length longs.
    public void addAttribute(String name, MemorySegment value, long loc, long len) {
        addAttribute(name, value, new NSRange(loc, len));
    }

    /// [mutable addAttributes:range:] — dict is NSDictionary*
    public void addAttributes(MemorySegment attrsDict, NSRange range) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
            MemorySegment arg = (attrsDict == null || attrsDict.address() == 0) ? MemorySegment.NULL : attrsDict;
            h.invokeExact(peer, ObjC.sel("addAttributes:range:"), (MemorySegment) arg, range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("addAttributes:range: failed", t);
        }
    }

    /// [mutable removeAttribute:name range:range]
    public void removeAttribute(String name, NSRange range) {
        ensureMutInit();
        try {
            handles.hSetAttr().invokeExact(peer, ObjC.sel("removeAttribute:range:"), ObjC.nsstring(name), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("removeAttribute:range: failed", t);
        }
    }

    /// [mutable setAttributes:range:]
    public void setAttributes(MemorySegment attrsDict, NSRange range) {
        ensureMutInit();
        try {
            MemorySegment arg = (attrsDict == null || attrsDict.address() == 0) ? MemorySegment.NULL : attrsDict;
            handles.hSetAttr().invokeExact(peer, ObjC.sel("setAttributes:range:"), (MemorySegment) arg, range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setAttributes:range: failed", t);
        }
    }
}
