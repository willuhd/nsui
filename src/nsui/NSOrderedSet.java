package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSOrderedSet — minimal wrapper over native `NSOrderedSet` / `NSMutableOrderedSet`.
///
/// Header-completeness (`NSOrderedSet.h`, immutable): every safe method whose shape is in
/// the Sig vocabulary is wrapped below. OMITTED — orderedSetWithOrderedSet:range:copyItems:,
/// orderedSetWithArray:range:copyItems:, initWithOrderedSet:...range:copyItems:, initWithArray:...
/// range:copyItems: (need of(ID,ID,RANGE,BOOL) / of(ID,ID,RANGE...), not in Sig);
/// orderedSetWithObjects:count: and initWithObjects:count: (C object arrays; covered by
/// orderedSetWithArray:/orderedSetWithObject:); getObjects:range: (out-buffer plumbing);
/// enumerateObjects.../indexOfObject...PassingTest:/indexesOfObjects.../sortedArrayUsingComparator:/
/// sortedArrayWithOptions:.../indexOfObject:inSortedRange:.../differenceFromOrderedSet:/
/// orderedSetByApplyingDifference: (blocks / comparators / undiffable peers); init* otherwise
/// (covered by the orderedSetWith* factories, except coder variants needing NSCoder).
public class NSOrderedSet extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hCount;        // (id, SEL) -> long
    private static MethodHandle hObjectAt;     // (id, SEL, long) -> id
    private static MethodHandle hContains;     // (id, SEL, id) -> bool
    private static MethodHandle hIndexOf;      // (id, SEL, id) -> long
    private static MethodHandle hFirstObject;  // (id, SEL) -> id
    private static MethodHandle hLastObject;   // (id, SEL) -> id
    private static MethodHandle hArray;        // (id, SEL) -> id [array]

    protected NSOrderedSet(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSOrderedSet wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSOrderedSet(peer);
    }

    /// [NSOrderedSet orderedSet]
    public static NSOrderedSet orderedSet() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSOrderedSet"), ObjC.sel("orderedSet"));
        return wrap(s);
    }

    /// [NSOrderedSet orderedSetWithObject:]
    public static NSOrderedSet orderedSetWithObject(NSObject object) {
        ensureInit();
        if (object == null) return orderedSet();
        MemorySegment s = ObjC.msgSendIdId(ObjC.cls("NSOrderedSet"), ObjC.sel("orderedSetWithObject:"), object.peer());
        return wrap(s);
    }

    /// [NSOrderedSet orderedSetWithArray:]
    public static NSOrderedSet orderedSetWithArray(NSArray array) {
        ensureInit();
        if (array == null) return orderedSet();
        MemorySegment s = ObjC.msgSendIdId(ObjC.cls("NSOrderedSet"), ObjC.sel("orderedSetWithArray:"), array.peer());
        return wrap(s);
    }

    private static void ensureInit() {
        if (initialized) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (initialized) return;
        hCount = ObjC.handle(Sig.of(Ret.INT));
        hObjectAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hContains = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        hIndexOf = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
        hFirstObject = ObjC.handle(Sig.of(Ret.ID));
        hLastObject = ObjC.handle(Sig.of(Ret.ID));
        hArray = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
    }

    /// count
    public long count() {
        ensureInit();
        try { return (long) hCount.invokeExact(peer, ObjC.sel("count")); }
        catch (Throwable t) { throw new RuntimeException("NSOrderedSet count failed", t); }
    }

    public boolean isEmpty() { return count() == 0; }

    /// objectAtIndex:
    public MemorySegment objectAtIndex(long index) {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hObjectAt.invokeExact(peer, ObjC.sel("objectAtIndex:"), index);
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("objectAtIndex: failed", t); }
    }

    public NSObject objectAt(long index) {
        MemorySegment seg = objectAtIndex(index);
        return seg == null ? null : NSObject.wrap(seg);
    }

    /// containsObject:
    public boolean containsObject(NSObject object) {
        ensureInit();
        if (object == null) return false;
        try { return (boolean) hContains.invokeExact(peer, ObjC.sel("containsObject:"), object.peer()); }
        catch (Throwable t) { throw new RuntimeException("containsObject: failed", t); }
    }

    public boolean containsObject(MemorySegment object) {
        ensureInit();
        if (object == null || object.address() == 0) return false;
        try { return (boolean) hContains.invokeExact(peer, ObjC.sel("containsObject:"), object); }
        catch (Throwable t) { throw new RuntimeException("containsObject: failed", t); }
    }

    /// indexOfObject: — NSNotFound if absent.
    public long indexOfObject(NSObject object) {
        ensureInit();
        if (object == null) return NSRange.NOT_FOUND;
        try { return (long) hIndexOf.invokeExact(peer, ObjC.sel("indexOfObject:"), object.peer()); }
        catch (Throwable t) { throw new RuntimeException("indexOfObject: failed", t); }
    }

    /// firstObject
    public MemorySegment firstObject() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hFirstObject.invokeExact(peer, ObjC.sel("firstObject"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("firstObject failed", t); }
    }

    /// lastObject
    public MemorySegment lastObject() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hLastObject.invokeExact(peer, ObjC.sel("lastObject"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("lastObject failed", t); }
    }

    /// array — ordered contents as NSArray.
    public NSArray array() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hArray.invokeExact(peer, ObjC.sel("array"));
            return NSArray.wrap(r);
        } catch (Throwable t) { throw new RuntimeException("array failed", t); }
    }

    /// objectAtIndexedSubscript: — native subscript read (same element as objectAtIndex:).
    public MemorySegment objectAtIndexedSubscript(long index) {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hObjectAt.invokeExact(peer, ObjC.sel("objectAtIndexedSubscript:"), index);
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("objectAtIndexedSubscript: failed", t); }
    }

    /// [NSOrderedSet orderedSetWithOrderedSet:].
    public static NSOrderedSet orderedSetWithOrderedSet(NSOrderedSet other) {
        ensureInit();
        if (other == null) return orderedSet();
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSOrderedSet"), ObjC.sel("orderedSetWithOrderedSet:"), other.peer()));
    }

    /// [NSOrderedSet orderedSetWithSet:].
    public static NSOrderedSet orderedSetWithSet(NSSet other) {
        ensureInit();
        if (other == null) return orderedSet();
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSOrderedSet"), ObjC.sel("orderedSetWithSet:"), other.peer()));
    }

    /// [NSOrderedSet orderedSetWithSet:copyItems:].
    public static NSOrderedSet orderedSetWithSetCopyItems(NSSet other, boolean copyItems) {
        ensureInit();
        if (other == null) return orderedSet();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.BOOL));
            MemorySegment s = (MemorySegment) h.invokeExact(
                    ObjC.cls("NSOrderedSet"), ObjC.sel("orderedSetWithSet:copyItems:"), other.peer(), copyItems);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("orderedSetWithSet:copyItems: failed", t); }
    }

    /// objectsAtIndexes:.
    public NSArray objectsAtIndexes(NSIndexSet indexes) {
        ensureInit();
        if (indexes == null) return NSArray.array();
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("objectsAtIndexes:"), indexes.peer()));
    }

    /// isEqualToOrderedSet:.
    public boolean isEqualToOrderedSet(NSOrderedSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToOrderedSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isEqualToOrderedSet: failed", t); }
    }

    /// intersectsOrderedSet:.
    public boolean intersectsOrderedSet(NSOrderedSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("intersectsOrderedSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("intersectsOrderedSet: failed", t); }
    }

    /// intersectsSet:.
    public boolean intersectsSet(NSSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("intersectsSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("intersectsSet: failed", t); }
    }

    /// isSubsetOfOrderedSet:.
    public boolean isSubsetOfOrderedSet(NSOrderedSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isSubsetOfOrderedSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isSubsetOfOrderedSet: failed", t); }
    }

    /// isSubsetOfSet:.
    public boolean isSubsetOfSet(NSSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isSubsetOfSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isSubsetOfSet: failed", t); }
    }

    /// objectEnumerator — NSEnumerator peer or null (no NSEnumerator wrapper in this batch).
    public MemorySegment objectEnumerator() {
        ensureInit();
        MemorySegment r = ObjC.msgSendId(peer, ObjC.sel("objectEnumerator"));
        return (r == null || r.address() == 0) ? null : r;
    }

    /// reverseObjectEnumerator — NSEnumerator peer or null.
    public MemorySegment reverseObjectEnumerator() {
        ensureInit();
        MemorySegment r = ObjC.msgSendId(peer, ObjC.sel("reverseObjectEnumerator"));
        return (r == null || r.address() == 0) ? null : r;
    }

    /// reversedOrderedSet.
    public NSOrderedSet reversedOrderedSet() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("reversedOrderedSet")));
    }

    /// set — unordered facade (mutations to the receiver show through; not a copy).
    public NSSet set() {
        ensureInit();
        return NSSet.wrap(ObjC.msgSendId(peer, ObjC.sel("set")));
    }

    /// descriptionWithLocale: — NSLocale peer, or NULL for the canonical description.
    public NSString descriptionWithLocale(MemorySegment locale) {
        ensureInit();
        return NSString.wrap(ObjC.msgSendIdId(peer, ObjC.sel("descriptionWithLocale:"),
                (locale == null ? MemorySegment.NULL : locale)));
    }
}
