package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSArray — typed wrapper over a native `NSArray` (id).
/// Thin, stateless: every method maps to one `objc_msgSend`.
/// Works for both NSArray and NSMutableArray (the latter adds mutating selectors).
///
/// Header-completeness (`NSArray.h`, immutable + mutable): every safe method whose shape
/// is in the Sig vocabulary is wrapped below. Mutating selectors raise an (uncatchable,
/// process-fatal — see nsui.objc.Exceptions) NSException on immutable peers, so call them
/// only on `mutableArray()` peers. OMITTED — indexOfObject:inRange: and
/// indexOfObjectIdenticalTo:inRange: (need of(INT,ID,RANGE), not in Sig);
/// indexOfObject:inSortedRange:options:usingComparator: and sortedArrayUsingComparator:/
/// sortedArrayWithOptions:.../sortUsingComparator:/sortWithOptions:... and
/// indexOfObjectPassingTest:/indexesOfObjects.../differenceFromArray:/arrayByApplyingDifference:/
/// applyDifference: (blocks / NSComparator / NSOrderedCollectionDifference peers, none wrapped);
/// getObjects:range: and getObjects: (out-buffer plumbing, deprecated); sortedArrayUsingFunction:...
/// and sortUsingFunction:... (C function pointers); SEL-taking selectors (sortedArrayUsingSelector:,
/// sortUsingSelector:, makeObjectsPerformSelector:...) are KEPT below: a SEL is id-shaped and registered;
/// init* (covered by array()/arrayWithObject:/arrayWithArray:/arrayWithCapacity:,
/// except coder/file variants needing NSCoder/NSURL/NSError**); arrayWithContentsOfFile:/URL:,
/// initWithContentsOfFile:/URL: and writeToFile:/writeToURL: (deprecated or NSError**).
public final class NSArray extends NSObject {

            private record Handles(MethodHandle hCount, MethodHandle hObjectAt, MethodHandle hAddObject, MethodHandle hLastObject) {}
    private static volatile Handles handles;

    private NSArray(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native NSArray id (null for nil).
    public static NSArray wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSArray(peer);
    }

    /// Create an empty IMMUTABLE array via `[NSArray array]`. Use `mutableArray()`
    /// when you intend to add or remove elements — the returned array rejects
    /// mutation with a native exception.
    public static NSArray array() {
        ensureInit();
        MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSArray"), ObjC.sel("array"));
        return wrap(arr);
    }

    /// Create an empty mutable array via `[NSMutableArray array]`.
    public static NSArray mutableArray() {
        ensureInit();
        MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
        return wrap(arr);
    }

    /// Reject a mutating call on an IMMUTABLE peer.
    ///
    /// This class serves both roles (there is no separate mutable wrapper), and its factories
    /// return either flavor: `array()` hands back an immutable one, while `mutableArray()` is the
    /// mutable peer. Sending a mutator to the immutable one aborts with an
    /// unrecognized-selector NSInvalidArgumentException (verified), so each mutator goes
    /// through this check and reports a named error instead.
    private void requireMutable(String what) {
        if (!isKindOfClass("NSMutableArray")) {
            throw new IllegalStateException(what + " requires a mutable array "
                    + "(use mutableArray(); array() is immutable)");
        }
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID))
        );
    }

    /// count — number of elements.
    public long count() {
        ensureInit();
        try {
            return (long) handles.hCount().invokeExact(peer, ObjC.sel("count"));
        } catch (Throwable t) {
            throw new RuntimeException("NSArray count failed", t);
        }
    }

    /// isEmpty — convenience.
    public boolean isEmpty() { return count() == 0; }

    /// objectAtIndex: — element at index (0-based).
    public MemorySegment objectAtIndex(long index) {
        ensureInit();
        long n = count();
        if (index < 0 || index >= n)
            throw new IllegalArgumentException("objectAtIndex: index " + index + " out of bounds (count " + n + ")");
        try {
            MemorySegment obj = (MemorySegment) handles.hObjectAt().invokeExact(peer, ObjC.sel("objectAtIndex:"), index);
            return (obj == null || obj.address() == 0) ? null : obj;
        } catch (Throwable t) {
            throw new RuntimeException("objectAtIndex: failed", t);
        }
    }

    /// Typed objectAtIndex returning NSObject wrapper.
    public NSObject objectAt(long index) {
        MemorySegment seg = objectAtIndex(index);
        return seg == null ? null : NSObject.wrap(seg);
    }

    /// Typed NSString at index.
    public NSString stringAt(long index) {
        MemorySegment seg = objectAtIndex(index);
        return seg == null ? null : NSString.wrap(seg);
    }

    /// lastObject — last element or null.
    public MemorySegment lastObject() {
        ensureInit();
        try {
            MemorySegment obj = (MemorySegment) handles.hLastObject().invokeExact(peer, ObjC.sel("lastObject"));
            return (obj == null || obj.address() == 0) ? null : obj;
        } catch (Throwable t) {
            throw new RuntimeException("lastObject failed", t);
        }
    }

    /// addObject: — mutating (for NSMutableArray).
    public void addObject(NSObject object) {
        requireMutable("addObject:");
        ensureInit();
        if (object == null) throw new IllegalArgumentException("addObject: null");
        try {
            handles.hAddObject().invokeExact(peer, ObjC.sel("addObject:"), object.peer());
        } catch (Throwable t) {
            throw new RuntimeException("addObject: failed", t);
        }
    }

    /// addObject: with raw segment.
    public void addObject(MemorySegment object) {
        requireMutable("addObject:");
        ensureInit();
        if (object == null || object.address() == 0) throw new IllegalArgumentException("addObject: null");
        try {
            handles.hAddObject().invokeExact(peer, ObjC.sel("addObject:"), object);
        } catch (Throwable t) {
            throw new RuntimeException("addObject: failed", t);
        }
    }

    /// containsObject: — convenience linear search via count/objectAtIndex.
    public boolean containsObject(MemorySegment object) {
        if (object == null || object.address() == 0) return false;
        long n = count();
        for (long i = 0; i < n; i++) {
            MemorySegment o = objectAtIndex(i);
            if (o != null && o.address() == object.address()) return true;
        }
        return false;
    }

    /// toList — snapshot as List.
    public java.util.List<MemorySegment> toList() {
        long n = count();
        java.util.List<MemorySegment> list = new java.util.ArrayList<>((int) n);
        for (long i = 0; i < n; i++) list.add(objectAtIndex(i));
        return java.util.Collections.unmodifiableList(list);
    }

    /// +arrayWithObject:. A null object is a caller error here (`[NSArray
    /// arrayWithObject:nil]` raises natively), matching `arrayByAddingObject`;
    /// it used to silently return an empty array.
    public static NSArray arrayWithObject(NSObject object) {
        ensureInit();
        if (object == null) throw new IllegalArgumentException("arrayWithObject: null object");
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSArray"), ObjC.sel("arrayWithObject:"), object.peer()));
    }

    /// +arrayWithArray:.
    public static NSArray arrayWithArray(NSArray other) {
        ensureInit();
        if (other == null) return array();
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSArray"), ObjC.sel("arrayWithArray:"), other.peer()));
    }

    /// +arrayWithCapacity: (NSMutableArray).
    public static NSArray arrayWithCapacity(long capacity) {
        ensureInit();
        Bounds.nonNegative(capacity, "arrayWithCapacity:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            return wrap((MemorySegment) h.invokeExact(ObjC.cls("NSMutableArray"), ObjC.sel("arrayWithCapacity:"), capacity));
        } catch (Throwable t) {
            throw new RuntimeException("arrayWithCapacity: failed", t);
        }
    }

    /// arrayByAddingObject:.
    public NSArray arrayByAddingObject(NSObject object) {
        ensureInit();
        if (object == null) throw new IllegalArgumentException("arrayByAddingObject: null");
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("arrayByAddingObject:"), object.peer()));
    }

    /// arrayByAddingObjectsFromArray:.
    public NSArray arrayByAddingObjectsFromArray(NSArray other) {
        ensureInit();
        if (other == null) return wrap(peer);
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("arrayByAddingObjectsFromArray:"), other.peer()));
    }

    /// componentsJoinedByString:.
    public NSString componentsJoinedByString(NSString separator) {
        ensureInit();
        if (separator == null) throw new IllegalArgumentException("componentsJoinedByString: null");
        return NSString.wrap(ObjC.msgSendIdId(peer, ObjC.sel("componentsJoinedByString:"), separator.peer()));
    }

    /// componentsJoinedByString: with a Java separator.
    public NSString componentsJoinedByString(String separator) {
        if (separator == null) throw new IllegalArgumentException("componentsJoinedByString: null");
        return componentsJoinedByString(NSString.of(separator));
    }

    /// containsObject: — native (uses isEqual:). Unlike containsObject(MemorySegment),
    /// which is an address-identity scan, this matches equal values at any address.
    public boolean containsObject(NSObject object) {
        ensureInit();
        if (object == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("containsObject:"), object.peer());
        } catch (Throwable t) {
            throw new RuntimeException("containsObject: failed", t);
        }
    }

    /// firstObject — first element or null.
    public MemorySegment firstObject() {
        ensureInit();
        MemorySegment obj = ObjC.msgSendId(peer, ObjC.sel("firstObject"));
        return (obj == null || obj.address() == 0) ? null : obj;
    }

    /// firstObjectCommonWithArray:.
    public MemorySegment firstObjectCommonWithArray(NSArray other) {
        ensureInit();
        if (other == null) return null;
        MemorySegment r = ObjC.msgSendIdId(peer, ObjC.sel("firstObjectCommonWithArray:"), other.peer());
        return (r == null || r.address() == 0) ? null : r;
    }

    /// indexOfObject: — index or NOT_FOUND.
    public long indexOfObject(NSObject object) {
        ensureInit();
        if (object == null) return NSRange.NOT_FOUND;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("indexOfObject:"), object.peer());
        } catch (Throwable t) {
            throw new RuntimeException("indexOfObject: failed", t);
        }
    }

    /// indexOfObjectIdenticalTo: — identity-based index or NOT_FOUND.
    public long indexOfObjectIdenticalTo(NSObject object) {
        ensureInit();
        if (object == null) return NSRange.NOT_FOUND;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("indexOfObjectIdenticalTo:"), object.peer());
        } catch (Throwable t) {
            throw new RuntimeException("indexOfObjectIdenticalTo: failed", t);
        }
    }

    /// isEqualToArray:.
    public boolean isEqualToArray(NSArray other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToArray:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("isEqualToArray: failed", t);
        }
    }

    /// objectAtIndexedSubscript: — native subscript read (same element as objectAtIndex:).
    public MemorySegment objectAtIndexedSubscript(long index) {
        ensureInit();
        long n = count();
        if (index < 0 || index >= n)
            throw new IllegalArgumentException("objectAtIndexedSubscript: index " + index + " out of bounds (count " + n + ")");
        try {
            MemorySegment obj = (MemorySegment) handles.hObjectAt().invokeExact(peer, ObjC.sel("objectAtIndexedSubscript:"), index);
            return (obj == null || obj.address() == 0) ? null : obj;
        } catch (Throwable t) {
            throw new RuntimeException("objectAtIndexedSubscript: failed", t);
        }
    }

    /// objectsAtIndexes: — elements at the given index set.
    public NSArray objectsAtIndexes(NSIndexSet indexes) {
        ensureInit();
        if (indexes == null) return array();
        Bounds.indexSet(indexes, count(), "objectsAtIndexes:");
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("objectsAtIndexes:"), indexes.peer()));
    }

    /// subarrayWithRange:.
    public NSArray subarrayWithRange(NSRange range) {
        ensureInit();
        Bounds.range(range, count(), "subarrayWithRange:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("subarrayWithRange:"), range.toSegment());
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("subarrayWithRange: failed", t);
        }
    }

    /// sortedArrayUsingSelector: — SEL comparator (e.g. ObjC.sel("compare:")); must return
    /// NSComparisonResult and take one object. Raises (fatal) for bad selectors.
    public NSArray sortedArrayUsingSelector(MemorySegment comparator) {
        ensureInit();
        if (comparator == null || comparator.address() == 0)
            throw new IllegalArgumentException("sortedArrayUsingSelector: null");
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("sortedArrayUsingSelector:"), comparator));
    }

    /// descriptionWithLocale: — NSLocale peer, or NULL for the canonical description.
    public NSString descriptionWithLocale(MemorySegment locale) {
        ensureInit();
        return NSString.wrap(ObjC.msgSendIdId(peer, ObjC.sel("descriptionWithLocale:"),
                (locale == null ? MemorySegment.NULL : locale)));
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

    /// sortedArrayHint — opaque sorting hint (nil unless produced by a sortedArrayUsing* call).
    public NSData sortedArrayHint() {
        ensureInit();
        return NSData.wrap(ObjC.msgSendId(peer, ObjC.sel("sortedArrayHint")));
    }

    /// makeObjectsPerformSelector: — send SEL to every element (must take no args and
    /// exist on every element; e.g. ObjC.sel("description")). Raises (fatal) otherwise.
    public void makeObjectsPerformSelector(MemorySegment selector) {
        ensureInit();
        if (selector == null || selector.address() == 0)
            throw new IllegalArgumentException("makeObjectsPerformSelector: null");
        ObjC.msgSendVoidId(peer, ObjC.sel("makeObjectsPerformSelector:"), selector);
    }

    /// makeObjectsPerformSelector:withObject:.
    public void makeObjectsPerformSelectorWithObject(MemorySegment selector, MemorySegment argument) {
        ensureInit();
        if (selector == null || selector.address() == 0)
            throw new IllegalArgumentException("makeObjectsPerformSelector: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("makeObjectsPerformSelector:withObject:"),
                    selector, (MemorySegment) (argument == null ? MemorySegment.NULL : argument));
        } catch (Throwable t) {
            throw new RuntimeException("makeObjectsPerformSelector:withObject: failed", t);
        }
    }

    /// insertObject:atIndex: — mutating (NSMutableArray peers only).
    public void insertObjectAtIndex(NSObject object, long index) {
        requireMutable("insertObjectAtIndex:");
        ensureInit();
        if (object == null) throw new IllegalArgumentException("insertObject: null");
        Bounds.insertIndex(index, count(), "insertObject:atIndex:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("insertObject:atIndex:"), object.peer(), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertObject:atIndex: failed", t);
        }
    }

    /// removeLastObject — mutating.
    public void removeLastObject() {
        requireMutable("removeLastObject:");
        ensureInit();
        ObjC.msgSendVoid(peer, ObjC.sel("removeLastObject"));
    }

    /// removeObjectAtIndex: — mutating.
    public void removeObjectAtIndex(long index) {
        requireMutable("removeObjectAtIndex:");
        ensureInit();
        Bounds.index(index, count(), "removeObjectAtIndex:");
        ObjC.msgSendVoidLong(peer, ObjC.sel("removeObjectAtIndex:"), index);
    }

    /// replaceObjectAtIndex:withObject: — mutating.
    public void replaceObjectAtIndex(long index, NSObject object) {
        requireMutable("replaceObjectAtIndex:");
        ensureInit();
        if (object == null) throw new IllegalArgumentException("replaceObjectAtIndex: null");
        Bounds.index(index, count(), "replaceObjectAtIndex:withObject:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceObjectAtIndex:withObject:"), index, object.peer());
        } catch (Throwable t) {
            throw new RuntimeException("replaceObjectAtIndex:withObject: failed", t);
        }
    }

    /// addObjectsFromArray: — mutating.
    public void addObjectsFromArray(NSArray other) {
        requireMutable("addObjectsFromArray:");
        ensureInit();
        if (other == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("addObjectsFromArray:"), other.peer());
    }

    /// exchangeObjectAtIndex:withObjectAtIndex: — mutating.
    public void exchangeObjectAtIndex(long i, long j) {
        requireMutable("exchangeObjectAtIndex:");
        ensureInit();
        long n = count();
        Bounds.index(i, n, "exchangeObjectAtIndex:withObjectAtIndex: (first)");
        Bounds.index(j, n, "exchangeObjectAtIndex:withObjectAtIndex: (second)");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT));
            h.invokeExact(peer, ObjC.sel("exchangeObjectAtIndex:withObjectAtIndex:"), i, j);
        } catch (Throwable t) {
            throw new RuntimeException("exchangeObjectAtIndex:withObjectAtIndex: failed", t);
        }
    }

    /// removeAllObjects — mutating.
    public void removeAllObjects() {
        requireMutable("removeAllObjects:");
        ensureInit();
        ObjC.msgSendVoid(peer, ObjC.sel("removeAllObjects"));
    }

    /// removeObject: — mutating (all equal occurrences).
    public void removeObject(NSObject object) {
        requireMutable("removeObject:");
        ensureInit();
        if (object == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("removeObject:"), object.peer());
    }

    /// removeObject:inRange: — mutating.
    public void removeObjectInRange(NSObject object, NSRange range) {
        requireMutable("removeObjectInRange:");
        ensureInit();
        if (object == null) return;
        Bounds.range(range, count(), "removeObject:inRange:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("removeObject:inRange:"), object.peer(), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("removeObject:inRange: failed", t);
        }
    }

    /// removeObjectIdenticalTo: — mutating (identity-based).
    public void removeObjectIdenticalTo(NSObject object) {
        requireMutable("removeObjectIdenticalTo:");
        ensureInit();
        if (object == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("removeObjectIdenticalTo:"), object.peer());
    }

    /// removeObjectIdenticalTo:inRange: — mutating.
    public void removeObjectIdenticalToInRange(NSObject object, NSRange range) {
        requireMutable("removeObjectIdenticalToInRange:");
        ensureInit();
        if (object == null) return;
        Bounds.range(range, count(), "removeObjectIdenticalTo:inRange:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("removeObjectIdenticalTo:inRange:"), object.peer(), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("removeObjectIdenticalTo:inRange: failed", t);
        }
    }

    /// removeObjectsInArray: — mutating.
    public void removeObjectsInArray(NSArray other) {
        requireMutable("removeObjectsInArray:");
        ensureInit();
        if (other == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("removeObjectsInArray:"), other.peer());
    }

    /// removeObjectsInRange: — mutating.
    public void removeObjectsInRange(NSRange range) {
        requireMutable("removeObjectsInRange:");
        ensureInit();
        Bounds.range(range, count(), "removeObjectsInRange:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("removeObjectsInRange:"), range.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("removeObjectsInRange: failed", t);
        }
    }

    /// setArray: — mutating (replace contents).
    public void setArray(NSArray other) {
        requireMutable("setArray:");
        ensureInit();
        if (other == null) { removeAllObjects(); return; }
        ObjC.msgSendVoidId(peer, ObjC.sel("setArray:"), other.peer());
    }

    /// insertObjects:atIndexes: — mutating.
    public void insertObjectsAtIndexes(NSArray objects, NSIndexSet indexes) {
        requireMutable("insertObjectsAtIndexes:");
        ensureInit();
        if (objects == null || indexes == null) throw new IllegalArgumentException("insertObjects:atIndexes: null");
        long k = indexes.count();
        Bounds.equalCounts(objects.count(), k, "insertObjects:atIndexes: (objects vs indexes)");
        // The indexes are positions in the RESULT array, so the largest legal index is
        // count + k - 1 (verified natively: {2,3} into a 2-element array yields 4
        // elements, while {3,4} raises). indexSet rejects last >= bound, so pass count + k.
        Bounds.indexSet(indexes, count() + k, "insertObjects:atIndexes:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("insertObjects:atIndexes:"), objects.peer(), indexes.peer());
        } catch (Throwable t) {
            throw new RuntimeException("insertObjects:atIndexes: failed", t);
        }
    }

    /// removeObjectsAtIndexes: — mutating.
    public void removeObjectsAtIndexes(NSIndexSet indexes) {
        requireMutable("removeObjectsAtIndexes:");
        ensureInit();
        if (indexes == null) return;
        Bounds.indexSet(indexes, count(), "removeObjectsAtIndexes:");
        ObjC.msgSendVoidId(peer, ObjC.sel("removeObjectsAtIndexes:"), indexes.peer());
    }

    /// replaceObjectsAtIndexes:withObjects: — mutating.
    public void replaceObjectsAtIndexes(NSIndexSet indexes, NSArray objects) {
        requireMutable("replaceObjectsAtIndexes:");
        ensureInit();
        if (indexes == null || objects == null) throw new IllegalArgumentException("replaceObjectsAtIndexes: null");
        // Replacement cannot grow the array, so the receiver's count() is the right bound
        // (verified: indexes {1,2} into a 2-element array raises "range {1, 2} extends
        // beyond bounds [0 .. 1]"), but the two counts must still be equal or AppKit
        // raises NSInvalidArgumentException and the JVM aborts.
        Bounds.equalCounts(objects.count(), indexes.count(),
                "replaceObjectsAtIndexes:withObjects: (objects vs indexes)");
        Bounds.indexSet(indexes, count(), "replaceObjectsAtIndexes:withObjects:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceObjectsAtIndexes:withObjects:"), indexes.peer(), objects.peer());
        } catch (Throwable t) {
            throw new RuntimeException("replaceObjectsAtIndexes:withObjects: failed", t);
        }
    }

    /// replaceObjectsInRange:withObjectsFromArray: — mutating.
    public void replaceObjectsInRange(NSRange range, NSArray other) {
        requireMutable("replaceObjectsInRange:");
        ensureInit();
        if (other == null) throw new IllegalArgumentException("replaceObjectsInRange: null");
        Bounds.range(range, count(), "replaceObjectsInRange:withObjectsFromArray:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceObjectsInRange:withObjectsFromArray:"), range.toSegment(), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("replaceObjectsInRange:withObjectsFromArray: failed", t);
        }
    }

    /// setObject:atIndexedSubscript: — mutating subscript write.
    public void setObjectAtIndexedSubscript(NSObject object, long index) {
        requireMutable("setObjectAtIndexedSubscript:");
        ensureInit();
        if (object == null) throw new IllegalArgumentException("setObject:atIndexedSubscript: null");
        Bounds.index(index, count(), "setObject:atIndexedSubscript:");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("setObject:atIndexedSubscript:"), object.peer(), index);
        } catch (Throwable t) {
            throw new RuntimeException("setObject:atIndexedSubscript: failed", t);
        }
    }

    /// sortUsingSelector: — mutating SEL sort (e.g. ObjC.sel("compare:")); see sortedArrayUsingSelector:.
    public void sortUsingSelector(MemorySegment comparator) {
        requireMutable("sortUsingSelector:");
        ensureInit();
        if (comparator == null || comparator.address() == 0)
            throw new IllegalArgumentException("sortUsingSelector: null");
        ObjC.msgSendVoidId(peer, ObjC.sel("sortUsingSelector:"), comparator);
    }
}
