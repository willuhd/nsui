package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSOrderedSet — minimal wrapper over native {@code NSOrderedSet} / {@code NSMutableOrderedSet}.
 */
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

    /** [NSOrderedSet orderedSet] */
    public static NSOrderedSet orderedSet() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSOrderedSet"), ObjC.sel("orderedSet"));
        return wrap(s);
    }

    /** [NSOrderedSet orderedSetWithObject:] */
    public static NSOrderedSet orderedSetWithObject(NSObject object) {
        ensureInit();
        if (object == null) return orderedSet();
        MemorySegment s = ObjC.msgSendIdId(ObjC.cls("NSOrderedSet"), ObjC.sel("orderedSetWithObject:"), object.peer());
        return wrap(s);
    }

    /** [NSOrderedSet orderedSetWithArray:] */
    public static NSOrderedSet orderedSetWithArray(NSArray array) {
        ensureInit();
        if (array == null) return orderedSet();
        MemorySegment s = ObjC.msgSendIdId(ObjC.cls("NSOrderedSet"), ObjC.sel("orderedSetWithArray:"), array.peer());
        return wrap(s);
    }

    private static synchronized void ensureInit() {
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

    /** count */
    public long count() {
        ensureInit();
        try { return (long) hCount.invokeExact(peer, ObjC.sel("count")); }
        catch (Throwable t) { throw new RuntimeException("NSOrderedSet count failed", t); }
    }

    public boolean isEmpty() { return count() == 0; }

    /** objectAtIndex: */
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

    /** containsObject: */
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

    /** indexOfObject: — NSNotFound if absent. */
    public long indexOfObject(NSObject object) {
        ensureInit();
        if (object == null) return NSRange.NOT_FOUND;
        try { return (long) hIndexOf.invokeExact(peer, ObjC.sel("indexOfObject:"), object.peer()); }
        catch (Throwable t) { throw new RuntimeException("indexOfObject: failed", t); }
    }

    /** firstObject */
    public MemorySegment firstObject() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hFirstObject.invokeExact(peer, ObjC.sel("firstObject"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("firstObject failed", t); }
    }

    /** lastObject */
    public MemorySegment lastObject() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hLastObject.invokeExact(peer, ObjC.sel("lastObject"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("lastObject failed", t); }
    }

    /** array — ordered contents as NSArray. */
    public NSArray array() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hArray.invokeExact(peer, ObjC.sel("array"));
            return NSArray.wrap(r);
        } catch (Throwable t) { throw new RuntimeException("array failed", t); }
    }

    /** objectAtIndexedSubscript: alias for objectAtIndex:. */
    public MemorySegment objectAtIndexedSubscript(long index) { return objectAtIndex(index); }
}
