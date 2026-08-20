package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSIndexSet — minimal wrapper over native `NSIndexSet` / `NSMutableIndexSet`.
public class NSIndexSet extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hCount;          // (id, SEL) -> long
    private static MethodHandle hContains;       // (id, SEL, long) -> bool
    private static MethodHandle hFirstIndex;     // (id, SEL) -> long
    private static MethodHandle hLastIndex;      // (id, SEL) -> long

    protected NSIndexSet(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSIndexSet wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSIndexSet(peer);
    }

    /// [NSIndexSet indexSet] — empty.
    public static NSIndexSet indexSet() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSIndexSet"), ObjC.sel("indexSet"));
        return wrap(s);
    }

    /// [NSIndexSet indexSetWithIndex:]
    public static NSIndexSet indexSetWithIndex(long index) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSIndexSet"), ObjC.sel("indexSetWithIndex:"), index);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("indexSetWithIndex: failed", t); }
    }

    /// [NSIndexSet indexSetWithIndexesInRange:]
    public static NSIndexSet indexSetWithIndexesInRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSIndexSet"), ObjC.sel("indexSetWithIndexesInRange:"), range.toSegment());
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("indexSetWithIndexesInRange: failed", t); }
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hCount = ObjC.handle(Sig.of(Ret.INT));
        hContains = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
        hFirstIndex = ObjC.handle(Sig.of(Ret.INT));
        hLastIndex = ObjC.handle(Sig.of(Ret.INT));
        initialized = true;
    }

    /// count
    public long count() {
        ensureInit();
        try { return (long) hCount.invokeExact(peer, ObjC.sel("count")); }
        catch (Throwable t) { throw new RuntimeException("NSIndexSet count failed", t); }
    }

    public boolean isEmpty() { return count() == 0; }

    /// containsIndex:
    public boolean containsIndex(long index) {
        ensureInit();
        try { return (boolean) hContains.invokeExact(peer, ObjC.sel("containsIndex:"), index); }
        catch (Throwable t) { throw new RuntimeException("containsIndex: failed", t); }
    }

    /// containsIndexesInRange:
    public boolean containsIndexesInRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.RANGE));
            return (boolean) h.invokeExact(peer, ObjC.sel("containsIndexesInRange:"), range.toSegment());
        } catch (Throwable t) { throw new RuntimeException("containsIndexesInRange: failed", t); }
    }

    /// firstIndex — NSNotFound if empty.
    public long firstIndex() {
        ensureInit();
        try { return (long) hFirstIndex.invokeExact(peer, ObjC.sel("firstIndex")); }
        catch (Throwable t) { throw new RuntimeException("firstIndex failed", t); }
    }

    /// lastIndex — NSNotFound if empty.
    public long lastIndex() {
        ensureInit();
        try { return (long) hLastIndex.invokeExact(peer, ObjC.sel("lastIndex")); }
        catch (Throwable t) { throw new RuntimeException("lastIndex failed", t); }
    }

    /// isEqualToIndexSet:
    public boolean isEqualToIndexSet(NSIndexSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToIndexSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isEqualToIndexSet: failed", t); }
    }

    /// enumerateIndexes — convenience callback for testing.
    public void enumerateIndexes(java.util.function.LongConsumer block) {
        ensureInit();
        long n = count();
        if (n == 0) return;
        // Iterate via firstIndex + indexGreaterThanIndex: to stay faithful to native set.
        try {
            MethodHandle hNext = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            long idx = firstIndex();
            while (idx != NSRange.NOT_FOUND) {
                block.accept(idx);
                idx = (long) hNext.invokeExact(peer, ObjC.sel("indexGreaterThanIndex:"), idx);
            }
        } catch (Throwable t) { throw new RuntimeException("enumerateIndexes failed", t); }
    }
}
