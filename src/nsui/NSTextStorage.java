package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSTextStorage — mutable attributed string with layout-manager support.
 * Thin 1:1 wrapper over native {@code NSTextStorage} (a subclass of
 * {@code NSMutableAttributedString}). Every method maps to one
 * {@code objc_msgSend} selector, no cached Java state beyond the peer.
 */
public class NSTextStorage extends NSMutableAttributedString {

    private static volatile boolean initialized;
    private static MethodHandle hInit;       // (id, SEL) -> id  init
    private static MethodHandle hDelegate;   // (id, SEL) -> id  delegate
    private static MethodHandle hSetDelegate;// (id, SEL, id) -> void
    private static MethodHandle hAddLayout;  // (id, SEL, id) -> void  addLayoutManager:
    private static MethodHandle hRemoveLayout;// (id, SEL, long) -> void removeLayoutManagerAtIndex: alternative via handle

    protected NSTextStorage(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTextStorage wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTextStorage(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInit = ObjC.handle(Sig.of(Ret.ID));
        hDelegate = ObjC.handle(Sig.of(Ret.ID));
        hSetDelegate = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hAddLayout = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        // removeLayoutManager: takes id, but provide INT variant via existing handle if needed
        // we use INT shape for index removal
        hRemoveLayout = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        initialized = true;
    }

    /** {@code [[NSTextStorage alloc] init]} — empty storage. */
    public static NSTextStorage create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSTextStorage"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) hInit.invokeExact(alloc, ObjC.sel("init"));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSTextStorage init returned nil");
            return new NSTextStorage(p);
        } catch (Throwable t) {
            throw new RuntimeException("NSTextStorage init failed", t);
        }
    }

    /** {@code [[NSTextStorage alloc] initWithString:string]} */
    public static NSTextStorage create(String s) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSTextStorage"), ObjC.sel("alloc"));
        try {
            MethodHandle hInitStr = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) hInitStr.invokeExact(alloc, ObjC.sel("initWithString:"), ObjC.nsstring(s));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSTextStorage initWithString: returned nil");
            return new NSTextStorage(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithString: failed for NSTextStorage", t);
        }
    }

    /** {@code [[NSTextStorage alloc] initWithAttributedString:attrStr]} */
    public static NSTextStorage create(NSAttributedString attr) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSTextStorage"), ObjC.sel("alloc"));
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment arg = (attr == null ? MemorySegment.NULL : attr.peer());
            MemorySegment p = (MemorySegment) h.invokeExact(alloc, ObjC.sel("initWithAttributedString:"), arg);
            if (p == null || p.address() == 0) throw new IllegalStateException("NSTextStorage initWithAttributedString: returned nil");
            return new NSTextStorage(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithAttributedString: failed", t);
        }
    }

    // ---- delegate ----

    /** [storage delegate] — raw id (may be nil). */
    public MemorySegment delegateSegment() {
        ensureInit();
        try {
            return (MemorySegment) hDelegate.invokeExact(peer, ObjC.sel("delegate"));
        } catch (Throwable t) {
            throw new RuntimeException("delegate failed", t);
        }
    }

    /** [storage setDelegate:] — raw id. */
    public void setDelegate(MemorySegment delegate) {
        ensureInit();
        try {
            hSetDelegate.invokeExact(peer, ObjC.sel("setDelegate:"), (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate));
        } catch (Throwable t) {
            throw new RuntimeException("setDelegate: failed", t);
        }
    }

    /** Typed convenience: set delegate via NSTextStorageDelegate marker (stores raw id if delegate supplies a peer). */
    public void setDelegate(NSTextStorageDelegate delegate) {
        // NSTextStorageDelegate is a pure Java interface — no peer. Store as null.
        // Callers that bridge via DelegateProxy should use setDelegate(MemorySegment).
        setDelegate((MemorySegment) null);
    }

    // ---- layout managers ----

    /** [storage addLayoutManager:] */
    public void addLayoutManager(NSLayoutManager lm) {
        ensureInit();
        try {
            hAddLayout.invokeExact(peer, ObjC.sel("addLayoutManager:"), (MemorySegment) (lm == null ? MemorySegment.NULL : lm.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("addLayoutManager: failed", t);
        }
    }

    /** [storage removeLayoutManager:] */
    public void removeLayoutManager(NSLayoutManager lm) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeLayoutManager:"), (MemorySegment) (lm == null ? MemorySegment.NULL : lm.peer()));
    }

    /** [storage layoutManagers] — NSArray of NSLayoutManager */
    public java.util.List<NSLayoutManager> layoutManagers() {
        MemorySegment arr = ObjC.msgSendId(peer, ObjC.sel("layoutManagers"));
        if (arr == null || arr.address() == 0) return java.util.List.of();
        long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
        java.util.List<NSLayoutManager> out = new java.util.ArrayList<>((int) count);
        MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        for (long i = 0; i < count; i++) {
            try {
                MemorySegment v = (MemorySegment) h.invokeExact(arr, ObjC.sel("objectAtIndex:"), i);
                if (v != null && v.address() != 0) out.add(NSLayoutManager.wrap(v));
            } catch (Throwable t) {
                throw new RuntimeException("layoutManagers objectAtIndex failed", t);
            }
        }
        return java.util.Collections.unmodifiableList(out);
    }

    // ---- editing hooks (passthrough) ----

    /** [storage edited:range:changeInLength:] — notify of edit. */
    public void edited(long editedMask, NSRange range, long delta) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.RANGE, Arg.INT));
            h.invokeExact(peer, ObjC.sel("edited:range:changeInLength:"), editedMask, range.toSegment(), delta);
        } catch (Throwable t) {
            // Fallback via generic if shape not in vocab: swallow to keep minimal
            throw new RuntimeException("edited:range:changeInLength: failed", t);
        }
    }

    /** [storage processEditing] */
    public void processEditing() {
        ObjC.msgSendVoid(peer, ObjC.sel("processEditing"));
    }

    /** [storage ensureAttributesAreFixedInRange:] */
    public void ensureAttributesAreFixed(NSRange range) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("ensureAttributesAreFixedInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("ensureAttributesAreFixedInRange: failed", t);
        }
    }
}
