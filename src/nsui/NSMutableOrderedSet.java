package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMutableOrderedSet — mutable ordered set.
///
/// Header-completeness (`NSOrderedSet.h`, mutable): every safe method whose shape is in
/// the Sig vocabulary is wrapped below. OMITTED — addObjects:count: and
/// replaceObjectsInRange:withObjects:count: (C object arrays); sortUsingComparator:/
/// sortWithOptions:.../sortRange:... (blocks); applyDifference: (undiffable peer);
/// initWithCapacity: (covered by orderedSetWithCapacity:).
public final class NSMutableOrderedSet extends NSOrderedSet {

    private static volatile boolean initMut;
    private static MethodHandle hAddObject;        // (id, SEL, id) -> void
    private static MethodHandle hInsertAt;         // (id, SEL, id, long) -> void
    private static MethodHandle hRemoveAt;         // (id, SEL, long) -> void
    private static MethodHandle hRemoveObject;     // (id, SEL, id) -> void
    private static MethodHandle hRemoveAll;        // (id, SEL) -> void

    private NSMutableOrderedSet(MemorySegment peer) { super(peer); }

    public static NSMutableOrderedSet wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMutableOrderedSet(peer);
    }

    /// [NSMutableOrderedSet orderedSet]
    public static NSMutableOrderedSet orderedSet() {
        ensureMutInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSMutableOrderedSet"), ObjC.sel("orderedSet"));
        return wrap(s);
    }

    /// [NSMutableOrderedSet orderedSetWithCapacity:]
    public static NSMutableOrderedSet orderedSetWithCapacity(long capacity) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSMutableOrderedSet"), ObjC.sel("orderedSetWithCapacity:"), capacity);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("orderedSetWithCapacity: failed", t); }
    }

    private static void ensureMutInit() {
        if (initMut) return;
        ensureMutInitLocked();
    }

    private static synchronized void ensureMutInitLocked() {
        if (initMut) return;
        try { NSOrderedSet.orderedSet(); } catch (Exception ignored) {}
        hAddObject = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hInsertAt = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hRemoveAt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hRemoveObject = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hRemoveAll = ObjC.handle(Sig.of(Ret.VOID));
        initMut = true;
    }

    /// addObject:
    public void addObject(NSObject object) {
        ensureMutInit();
        if (object == null) throw new IllegalArgumentException("addObject: null");
        try { hAddObject.invokeExact(peer, ObjC.sel("addObject:"), object.peer()); }
        catch (Throwable t) { throw new RuntimeException("addObject: failed", t); }
    }

    public void addObject(MemorySegment object) {
        ensureMutInit();
        if (object == null || object.address() == 0) throw new IllegalArgumentException("addObject: null");
        try { hAddObject.invokeExact(peer, ObjC.sel("addObject:"), object); }
        catch (Throwable t) { throw new RuntimeException("addObject: failed", t); }
    }

    /// insertObject:atIndex:
    public void insertObjectAtIndex(NSObject object, long index) {
        ensureMutInit();
        if (object == null) throw new IllegalArgumentException("insertObject: null");
        Bounds.insertIndex(index, count(), "insertObject:atIndex:");
        try { hInsertAt.invokeExact(peer, ObjC.sel("insertObject:atIndex:"), object.peer(), index); }
        catch (Throwable t) { throw new RuntimeException("insertObject:atIndex: failed", t); }
    }

    /// removeObjectAtIndex:
    public void removeObjectAtIndex(long index) {
        ensureMutInit();
        Bounds.index(index, count(), "removeObjectAtIndex:");
        try { hRemoveAt.invokeExact(peer, ObjC.sel("removeObjectAtIndex:"), index); }
        catch (Throwable t) { throw new RuntimeException("removeObjectAtIndex: failed", t); }
    }

    /// removeObject:
    public void removeObject(NSObject object) {
        ensureMutInit();
        if (object == null) return;
        try { hRemoveObject.invokeExact(peer, ObjC.sel("removeObject:"), object.peer()); }
        catch (Throwable t) { throw new RuntimeException("removeObject: failed", t); }
    }

    /// removeAllObjects
    public void removeAllObjects() {
        ensureMutInit();
        try { hRemoveAll.invokeExact(peer, ObjC.sel("removeAllObjects")); }
        catch (Throwable t) { throw new RuntimeException("removeAllObjects failed", t); }
    }

    /// addObjectsFromArray:
    public void addObjectsFromArray(NSArray array) {
        ensureMutInit();
        if (array == null) return;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("addObjectsFromArray:"), array.peer());
        } catch (Throwable t) { throw new RuntimeException("addObjectsFromArray: failed", t); }
    }

    /// replaceObjectAtIndex:withObject:.
    public void replaceObjectAtIndex(long index, NSObject object) {
        ensureMutInit();
        if (object == null) throw new IllegalArgumentException("replaceObjectAtIndex: null");
        Bounds.index(index, count(), "replaceObjectAtIndex:withObject:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceObjectAtIndex:withObject:"), index, object.peer());
        } catch (Throwable t) { throw new RuntimeException("replaceObjectAtIndex:withObject: failed", t); }
    }

    /// exchangeObjectAtIndex:withObjectAtIndex:.
    public void exchangeObjectAtIndex(long i, long j) {
        ensureMutInit();
        long n = count();
        Bounds.index(i, n, "exchangeObjectAtIndex:withObjectAtIndex: (first)");
        Bounds.index(j, n, "exchangeObjectAtIndex:withObjectAtIndex: (second)");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT));
            h.invokeExact(peer, ObjC.sel("exchangeObjectAtIndex:withObjectAtIndex:"), i, j);
        } catch (Throwable t) { throw new RuntimeException("exchangeObjectAtIndex:withObjectAtIndex: failed", t); }
    }

    /// moveObjectsAtIndexes:toIndex:.
    ///
    /// The destination is an index in the array AFTER the moved objects are removed, so the
    /// largest legal value is `count - indexes.count()`. Verified on a 3-element set:
    /// moving {0,1} accepts toIndex 1 and raises "index 2 beyond bounds [0 .. 0]" for 2;
    /// moving {0} accepts 2 and raises "index 3 beyond bounds [0 .. 1]" for 3. The previous
    /// Bounds.insertIndex against count() allowed `count`, which aborts natively.
    public void moveObjectsAtIndexesToIndex(NSIndexSet indexes, long index) {
        ensureMutInit();
        if (indexes == null) throw new IllegalArgumentException("moveObjectsAtIndexes: null");
        Bounds.indexSet(indexes, count(), "moveObjectsAtIndexes:toIndex: (indexes)");
        Bounds.insertIndex(index, count() - indexes.count(), "moveObjectsAtIndexes:toIndex:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("moveObjectsAtIndexes:toIndex:"), indexes.peer(), index);
        } catch (Throwable t) { throw new RuntimeException("moveObjectsAtIndexes:toIndex: failed", t); }
    }

    /// insertObjects:atIndexes:.
    public void insertObjectsAtIndexes(NSArray objects, NSIndexSet indexes) {
        ensureMutInit();
        if (objects == null || indexes == null) throw new IllegalArgumentException("insertObjects:atIndexes: null");
        long k = indexes.count();
        Bounds.equalCounts(objects.count(), k, "insertObjects:atIndexes: (objects vs indexes)");
        Bounds.indexSet(indexes, count() + k, "insertObjects:atIndexes:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("insertObjects:atIndexes:"), objects.peer(), indexes.peer());
        } catch (Throwable t) { throw new RuntimeException("insertObjects:atIndexes: failed", t); }
    }

    /// setObject:atIndex:.
    public void setObjectAtIndex(NSObject object, long index) {
        ensureMutInit();
        if (object == null) throw new IllegalArgumentException("setObject:atIndex: null");
        Bounds.index(index, count(), "setObject:atIndex:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("setObject:atIndex:"), object.peer(), index);
        } catch (Throwable t) { throw new RuntimeException("setObject:atIndex: failed", t); }
    }

    /// setObject:atIndexedSubscript:.
    public void setObjectAtIndexedSubscript(NSObject object, long index) {
        ensureMutInit();
        if (object == null) throw new IllegalArgumentException("setObject:atIndexedSubscript: null");
        Bounds.index(index, count(), "setObject:atIndexedSubscript:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("setObject:atIndexedSubscript:"), object.peer(), index);
        } catch (Throwable t) { throw new RuntimeException("setObject:atIndexedSubscript: failed", t); }
    }

    /// replaceObjectsAtIndexes:withObjects:.
    public void replaceObjectsAtIndexes(NSIndexSet indexes, NSArray objects) {
        ensureMutInit();
        if (indexes == null || objects == null) throw new IllegalArgumentException("replaceObjectsAtIndexes: null");
        Bounds.equalCounts(objects.count(), indexes.count(),
                "replaceObjectsAtIndexes:withObjects: (objects vs indexes)");
        Bounds.indexSet(indexes, count(), "replaceObjectsAtIndexes:withObjects:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceObjectsAtIndexes:withObjects:"), indexes.peer(), objects.peer());
        } catch (Throwable t) { throw new RuntimeException("replaceObjectsAtIndexes:withObjects: failed", t); }
    }

    /// removeObjectsInRange:.
    public void removeObjectsInRange(NSRange range) {
        ensureMutInit();
        Bounds.range(range, count(), "removeObjectsInRange:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("removeObjectsInRange:"), range.toSegment());
        } catch (Throwable t) { throw new RuntimeException("removeObjectsInRange: failed", t); }
    }

    /// removeObjectsAtIndexes:.
    public void removeObjectsAtIndexes(NSIndexSet indexes) {
        ensureMutInit();
        if (indexes == null) return;
        Bounds.indexSet(indexes, count(), "removeObjectsAtIndexes:");
        ObjC.msgSendVoidId(peer, ObjC.sel("removeObjectsAtIndexes:"), indexes.peer());
    }

    /// removeObjectsInArray:.
    public void removeObjectsInArray(NSArray array) {
        ensureMutInit();
        if (array == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("removeObjectsInArray:"), array.peer());
    }

    /// intersectOrderedSet:.
    public void intersectOrderedSet(NSOrderedSet other) {
        ensureMutInit();
        if (other == null) { removeAllObjects(); return; }
        ObjC.msgSendVoidId(peer, ObjC.sel("intersectOrderedSet:"), other.peer());
    }

    /// minusOrderedSet:.
    public void minusOrderedSet(NSOrderedSet other) {
        ensureMutInit();
        if (other == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("minusOrderedSet:"), other.peer());
    }

    /// unionOrderedSet:.
    public void unionOrderedSet(NSOrderedSet other) {
        ensureMutInit();
        if (other == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("unionOrderedSet:"), other.peer());
    }

    /// intersectSet:.
    public void intersectSet(NSSet other) {
        ensureMutInit();
        if (other == null) { removeAllObjects(); return; }
        ObjC.msgSendVoidId(peer, ObjC.sel("intersectSet:"), other.peer());
    }

    /// minusSet:.
    public void minusSet(NSSet other) {
        ensureMutInit();
        if (other == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("minusSet:"), other.peer());
    }

    /// unionSet:.
    public void unionSet(NSSet other) {
        ensureMutInit();
        if (other == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("unionSet:"), other.peer());
    }
}
