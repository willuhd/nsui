package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSLayoutManager — the central layout engine linking `NSTextStorage`
/// and `NSTextContainer`. Thin 1:1 wrapper over native `NSLayoutManager`.
public final class NSLayoutManager extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hInit;          // (id, SEL) -> id  init
    private static MethodHandle hAddContainer;  // (id, SEL, id) -> void
    private static MethodHandle hSetStorage;    // (id, SEL, id) -> void
    private static MethodHandle hGetId;         // (id, SEL) -> id
    private static MethodHandle hVoidId;        // (id, SEL, id) -> void
    private static MethodHandle hRangeId;       // (id, SEL, id) -> range
    private static MethodHandle hInt;           // (id, SEL) -> long
    private static MethodHandle hVoidRange;     // (id, SEL, NSRange) -> void  scrollRangeToVisible etc

    private NSLayoutManager(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSLayoutManager wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSLayoutManager(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInit = ObjC.handle(Sig.of(Ret.ID));
        hAddContainer = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hSetStorage = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hRangeId = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID));
        hInt = ObjC.handle(Sig.of(Ret.INT));
        hVoidRange = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
        initialized = true;
    }

    /// `[[NSLayoutManager alloc] init]`
    public static NSLayoutManager create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSLayoutManager"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) hInit.invokeExact(alloc, ObjC.sel("init"));
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
            MemorySegment p = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("textStorage"));
            return NSTextStorage.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("textStorage failed", t);
        }
    }

    /// [layoutManager setTextStorage:] — rarely set directly; normally via NSTextStorage addLayoutManager.
    public void setTextStorage(NSTextStorage storage) {
        ensureInit();
        try {
            hSetStorage.invokeExact(peer, ObjC.sel("setTextStorage:"), (MemorySegment) (storage == null ? MemorySegment.NULL : storage.peer()));
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
            hAddContainer.invokeExact(peer, ObjC.sel("addTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
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
            hVoidId.invokeExact(peer, ObjC.sel("ensureLayoutForTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("ensureLayoutForTextContainer: failed", t);
        }
    }

    /// [layoutManager glyphRangeForTextContainer:] -> NSRange
    public NSRange glyphRangeForTextContainer(NSTextContainer container) {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) hRangeId.invokeExact(peer, ObjC.sel("glyphRangeForTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("glyphRangeForTextContainer: failed", t);
        }
    }

    /// [layoutManager numberOfGlyphs]
    public long numberOfGlyphs() {
        ensureInit();
        try {
            return (long) hInt.invokeExact(peer, ObjC.sel("numberOfGlyphs"));
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
            hVoidRange.invokeExact(peer, ObjC.sel("invalidateDisplayForCharacterRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("invalidateDisplayForCharacterRange: failed", t);
        }
    }

    /// [layoutManager usedRectForTextContainer:] -> NSRect
    public NSRect usedRectForTextContainer(NSTextContainer container) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RECT, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact((java.lang.foreign.SegmentAllocator) Arena.global(), peer, ObjC.sel("usedRectForTextContainer:"), (MemorySegment) (container == null ? MemorySegment.NULL : container.peer()));
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
}
