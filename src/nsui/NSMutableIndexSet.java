package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSMutableIndexSet — mutable index set.
 */
public final class NSMutableIndexSet extends NSIndexSet {

    private static volatile boolean initMut;
    private static MethodHandle hAddIndex;       // (id, SEL, long) -> void
    private static MethodHandle hRemoveIndex;    // (id, SEL, long) -> void
    private static MethodHandle hAddRange;       // (id, SEL, NSRange) -> void
    private static MethodHandle hRemoveAll;      // (id, SEL) -> void

    private NSMutableIndexSet(MemorySegment peer) { super(peer); }

    public static NSMutableIndexSet wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMutableIndexSet(peer);
    }

    /** [NSMutableIndexSet indexSet] */
    public static NSMutableIndexSet indexSet() {
        ensureMutInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSMutableIndexSet"), ObjC.sel("indexSet"));
        return wrap(s);
    }

    /** [NSMutableIndexSet indexSetWithIndex:] */
    public static NSMutableIndexSet indexSetWithIndex(long index) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSMutableIndexSet"), ObjC.sel("indexSetWithIndex:"), index);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("indexSetWithIndex: failed", t); }
    }

    private static synchronized void ensureMutInit() {
        if (initMut) return;
        try { NSIndexSet.indexSet(); } catch (Exception ignored) {}
        hAddIndex = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hRemoveIndex = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hAddRange = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
        hRemoveAll = ObjC.handle(Sig.of(Ret.VOID));
        initMut = true;
    }

    /** addIndex: */
    public void addIndex(long index) {
        ensureMutInit();
        try { hAddIndex.invokeExact(peer, ObjC.sel("addIndex:"), index); }
        catch (Throwable t) { throw new RuntimeException("addIndex: failed", t); }
    }

    /** removeIndex: */
    public void removeIndex(long index) {
        ensureMutInit();
        try { hRemoveIndex.invokeExact(peer, ObjC.sel("removeIndex:"), index); }
        catch (Throwable t) { throw new RuntimeException("removeIndex: failed", t); }
    }

    /** addIndexesInRange: */
    public void addIndexesInRange(NSRange range) {
        ensureMutInit();
        try { hAddRange.invokeExact(peer, ObjC.sel("addIndexesInRange:"), range.toSegment()); }
        catch (Throwable t) { throw new RuntimeException("addIndexesInRange: failed", t); }
    }

    /** removeIndexesInRange: */
    public void removeIndexesInRange(NSRange range) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("removeIndexesInRange:"), range.toSegment());
        } catch (Throwable t) { throw new RuntimeException("removeIndexesInRange: failed", t); }
    }

    /** removeAllIndexes */
    public void removeAllIndexes() {
        ensureMutInit();
        try { hRemoveAll.invokeExact(peer, ObjC.sel("removeAllIndexes")); }
        catch (Throwable t) { throw new RuntimeException("removeAllIndexes failed", t); }
    }

    /** addIndexes: */
    public void addIndexes(NSIndexSet other) {
        ensureMutInit();
        if (other == null) return;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("addIndexes:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("addIndexes: failed", t); }
    }

    /** removeIndexes: */
    public void removeIndexes(NSIndexSet other) {
        ensureMutInit();
        if (other == null) return;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("removeIndexes:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("removeIndexes: failed", t); }
    }
}
