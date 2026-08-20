package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSMutableAttributedString — mutable attributed string.
 * Thin 1:1 wrapper over native {@code NSMutableAttributedString}: every method maps to one
 * {@code objc_msgSend} selector, no cached Java state beyond the peer.
 * Follows FFM pattern: no reflection, cached handles, ensureInit.
 */
public class NSMutableAttributedString extends NSAttributedString {

    private static volatile boolean mutableInitialized;
    private static MethodHandle hInitString;       // (id, SEL, id) -> id
    private static MethodHandle hInitStringAttrs;  // (id, SEL, id, id) -> id
    private static MethodHandle hAddAttr;          // (id, SEL, id, id, NSRange) -> void
    private static MethodHandle hAppend;           // (id, SEL, id) -> void  appendAttributedString:
    private static MethodHandle hSetAttr;          // (id, SEL, id, NSRange) -> void
    private static MethodHandle hRemoveAttr;       // (id, SEL, id, NSRange) -> void

    protected NSMutableAttributedString(MemorySegment peer) {
        super(peer);
        ensureMutInit();
    }

    public static NSMutableAttributedString wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMutableAttributedString(peer);
    }

    private static synchronized void ensureMutInit() {
        if (mutableInitialized) return;
        hInitString = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hInitStringAttrs = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
        hAddAttr = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.RANGE));
        hAppend = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hRemoveAttr = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
        hSetAttr = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
        mutableInitialized = true;
    }

    /** {@code [[NSMutableAttributedString alloc] initWithString:string]} */
    public static NSMutableAttributedString create(String s) {
        ensureMutInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSMutableAttributedString"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) hInitString.invokeExact(alloc, ObjC.sel("initWithString:"), ObjC.nsstring(s));
            if (p.address() == 0) throw new IllegalStateException("NSMutableAttributedString initWithString: returned nil");
            return new NSMutableAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithString: failed for NSMutableAttributedString", t);
        }
    }

    /** {@code [[NSMutableAttributedString alloc] initWithString:string attributes:dict]} */
    public static NSMutableAttributedString create(String s, MemorySegment attributes) {
        ensureMutInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSMutableAttributedString"), ObjC.sel("alloc"));
        try {
            MemorySegment attrs = (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes);
            MemorySegment p = (MemorySegment) hInitStringAttrs.invokeExact(alloc, ObjC.sel("initWithString:attributes:"), ObjC.nsstring(s), attrs);
            if (p.address() == 0) throw new IllegalStateException("NSMutableAttributedString initWithString:attributes: returned nil");
            return new NSMutableAttributedString(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithString:attributes: failed for NSMutableAttributedString", t);
        }
    }

    /** [mutable appendAttributedString:other] — also satisfies task's "append" requirement */
    public void append(NSAttributedString other) {
        ensureMutInit();
        try {
            hAppend.invokeExact(peer, ObjC.sel("appendAttributedString:"), (MemorySegment) ((MemorySegment) (other == null ? MemorySegment.NULL : other.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("appendAttributedString: failed", t);
        }
    }

    /** Alias per task description: append */
    public void appendAttributedString(NSAttributedString other) { append(other); }

    /** Convenience append with plain string */
    public void appendString(String s) {
        append(NSAttributedString.create(s));
    }

    /**
     * [mutable addAttribute:name value:value range:range]
     * @param name attribute name (e.g. NSFontAttributeName)
     * @param value attribute value as id (MemorySegment) — pass NSFont.peer(), NSColor.peer(), etc.
     * @param range range to apply
     */
    public void addAttribute(String name, MemorySegment value, NSRange range) {
        ensureMutInit();
        try {
            MemorySegment v = (MemorySegment) (value == null ? MemorySegment.NULL : value);
            hAddAttr.invokeExact(peer, ObjC.sel("addAttribute:value:range:"), ObjC.nsstring(name), v, range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("addAttribute:value:range: failed", t);
        }
    }

    /** Convenience with location/length longs. */
    public void addAttribute(String name, MemorySegment value, long loc, long len) {
        addAttribute(name, value, new NSRange(loc, len));
    }

    /** [mutable addAttributes:range:] — dict is NSDictionary* */
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

    /** [mutable removeAttribute:name range:range] */
    public void removeAttribute(String name, NSRange range) {
        ensureMutInit();
        try {
            hRemoveAttr.invokeExact(peer, ObjC.sel("removeAttribute:range:"), ObjC.nsstring(name), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("removeAttribute:range: failed", t);
        }
    }

    /** [mutable setAttributes:range:] */
    public void setAttributes(MemorySegment attrsDict, NSRange range) {
        ensureMutInit();
        try {
            MemorySegment arg = (attrsDict == null || attrsDict.address() == 0) ? MemorySegment.NULL : attrsDict;
            hSetAttr.invokeExact(peer, ObjC.sel("setAttributes:range:"), (MemorySegment) arg, range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setAttributes:range: failed", t);
        }
    }
}
