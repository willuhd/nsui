package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSArray — typed wrapper over a native {@code NSArray} (id).
 * Thin, stateless: every method maps to one {@code objc_msgSend}.
 * Works for both NSArray and NSMutableArray (the latter adds mutating selectors).
 */
public final class NSArray extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hCount;        // (id, SEL) -> long [count]
    private static MethodHandle hObjectAt;     // (id, SEL, long) -> id [objectAtIndex:]
    private static MethodHandle hAddObject;    // (id, SEL, id) -> void [addObject:] (NSMutableArray)
    private static MethodHandle hLastObject;   // (id, SEL) -> id [lastObject]

    private NSArray(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /** Wrap a native NSArray id (null for nil). */
    public static NSArray wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSArray(peer);
    }

    /** Create an empty mutable array via {@code [NSMutableArray array]}. */
    public static NSArray array() {
        ensureInit();
        MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSArray"), ObjC.sel("array"));
        return wrap(arr);
    }

    /** Create an empty mutable array via {@code [NSMutableArray array]}. */
    public static NSArray mutableArray() {
        ensureInit();
        MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
        return wrap(arr);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hCount = ObjC.handle(Sig.of(Ret.INT));
        hObjectAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hAddObject = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hLastObject = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
    }

    /** count — number of elements. */
    public long count() {
        ensureInit();
        try {
            return (long) hCount.invokeExact(peer, ObjC.sel("count"));
        } catch (Throwable t) {
            throw new RuntimeException("NSArray count failed", t);
        }
    }

    /** isEmpty — convenience. */
    public boolean isEmpty() { return count() == 0; }

    /** objectAtIndex: — element at index (0-based). */
    public MemorySegment objectAtIndex(long index) {
        ensureInit();
        try {
            MemorySegment obj = (MemorySegment) hObjectAt.invokeExact(peer, ObjC.sel("objectAtIndex:"), index);
            return (obj == null || obj.address() == 0) ? null : obj;
        } catch (Throwable t) {
            throw new RuntimeException("objectAtIndex: failed", t);
        }
    }

    /** Typed objectAtIndex returning NSObject wrapper. */
    public NSObject objectAt(long index) {
        MemorySegment seg = objectAtIndex(index);
        return seg == null ? null : NSObject.wrap(seg);
    }

    /** Typed NSString at index. */
    public NSString stringAt(long index) {
        MemorySegment seg = objectAtIndex(index);
        return seg == null ? null : NSString.wrap(seg);
    }

    /** lastObject — last element or null. */
    public MemorySegment lastObject() {
        ensureInit();
        try {
            MemorySegment obj = (MemorySegment) hLastObject.invokeExact(peer, ObjC.sel("lastObject"));
            return (obj == null || obj.address() == 0) ? null : obj;
        } catch (Throwable t) {
            throw new RuntimeException("lastObject failed", t);
        }
    }

    /** addObject: — mutating (for NSMutableArray). */
    public void addObject(NSObject object) {
        ensureInit();
        if (object == null) throw new IllegalArgumentException("addObject: null");
        try {
            hAddObject.invokeExact(peer, ObjC.sel("addObject:"), object.peer());
        } catch (Throwable t) {
            throw new RuntimeException("addObject: failed", t);
        }
    }

    /** addObject: with raw segment. */
    public void addObject(MemorySegment object) {
        ensureInit();
        if (object == null || object.address() == 0) throw new IllegalArgumentException("addObject: null");
        try {
            hAddObject.invokeExact(peer, ObjC.sel("addObject:"), object);
        } catch (Throwable t) {
            throw new RuntimeException("addObject: failed", t);
        }
    }

    /** containsObject: — convenience linear search via count/objectAtIndex. */
    public boolean containsObject(MemorySegment object) {
        if (object == null || object.address() == 0) return false;
        long n = count();
        for (long i = 0; i < n; i++) {
            MemorySegment o = objectAtIndex(i);
            if (o != null && o.address() == object.address()) return true;
        }
        return false;
    }

    /** toList — snapshot as List<MemorySegment>. */
    public java.util.List<MemorySegment> toList() {
        long n = count();
        java.util.List<MemorySegment> list = new java.util.ArrayList<>((int) n);
        for (long i = 0; i < n; i++) list.add(objectAtIndex(i));
        return java.util.Collections.unmodifiableList(list);
    }
}
