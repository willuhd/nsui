package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSSet — minimal typed wrapper over native {@code NSSet} / {@code NSMutableSet}.
 * Thin, stateless: every method maps to one {@code objc_msgSend}.
 */
public class NSSet extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hCount;          // (id, SEL) -> long
    private static MethodHandle hContains;       // (id, SEL, id) -> bool
    private static MethodHandle hMember;         // (id, SEL, id) -> id
    private static MethodHandle hAnyObject;      // (id, SEL) -> id
    private static MethodHandle hAllObjects;     // (id, SEL) -> id

    protected NSSet(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSSet wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSet(peer);
    }

    /** [NSSet set] — empty immutable set. */
    public static NSSet set() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSSet"), ObjC.sel("set"));
        return wrap(s);
    }

    /** [NSSet setWithObject:] — single object. */
    public static NSSet setWithObject(NSObject object) {
        ensureInit();
        if (object == null) return set();
        MemorySegment s = ObjC.msgSendIdId(ObjC.cls("NSSet"), ObjC.sel("setWithObject:"), object.peer());
        return wrap(s);
    }

    /** [NSSet setWithArray:] */
    public static NSSet setWithArray(NSArray array) {
        ensureInit();
        if (array == null) return set();
        MemorySegment s = ObjC.msgSendIdId(ObjC.cls("NSSet"), ObjC.sel("setWithArray:"), array.peer());
        return wrap(s);
    }

    /** [NSSet setWithObjects:count:] helper via array. */
    public static NSSet setWithObjects(NSObject... objects) {
        ensureInit();
        if (objects == null || objects.length == 0) return set();
        NSMutableSet ms = NSMutableSet.set();
        for (NSObject o : objects) if (o != null) ms.addObject(o);
        // copy to immutable via [NSSet setWithSet:]
        MemorySegment s = ObjC.msgSendIdId(ObjC.cls("NSSet"), ObjC.sel("setWithSet:"), ms.peer());
        return wrap(s);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hCount = ObjC.handle(Sig.of(Ret.INT));
        hContains = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        hMember = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hAnyObject = ObjC.handle(Sig.of(Ret.ID));
        hAllObjects = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
    }

    /** count */
    public long count() {
        ensureInit();
        try { return (long) hCount.invokeExact(peer, ObjC.sel("count")); }
        catch (Throwable t) { throw new RuntimeException("NSSet count failed", t); }
    }

    public boolean isEmpty() { return count() == 0; }

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

    /** member: — returns matching object or nil. */
    public MemorySegment member(MemorySegment object) {
        ensureInit();
        if (object == null || object.address() == 0) return null;
        try {
            MemorySegment r = (MemorySegment) hMember.invokeExact(peer, ObjC.sel("member:"), object);
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("member: failed", t); }
    }

    public NSObject member(NSObject object) {
        MemorySegment seg = member(object == null ? null : object.peer());
        return seg == null ? null : NSObject.wrap(seg);
    }

    /** anyObject */
    public MemorySegment anyObject() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hAnyObject.invokeExact(peer, ObjC.sel("anyObject"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("anyObject failed", t); }
    }

    /** allObjects — NSArray of members. */
    public NSArray allObjects() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) hAllObjects.invokeExact(peer, ObjC.sel("allObjects"));
            return NSArray.wrap(r);
        } catch (Throwable t) { throw new RuntimeException("allObjects failed", t); }
    }

    /** isEqualToSet: */
    public boolean isEqualToSet(NSSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isEqualToSet: failed", t); }
    }

    /** intersectsSet: */
    public boolean intersectsSet(NSSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("intersectsSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("intersectsSet: failed", t); }
    }

    /** isSubsetOfSet: */
    public boolean isSubsetOfSet(NSSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isSubsetOfSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isSubsetOfSet: failed", t); }
    }
}
