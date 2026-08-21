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

    private static synchronized void ensureMutInit() {
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
