package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSMutableOrderedSet — mutable ordered set.
 */
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

    /** [NSMutableOrderedSet orderedSet] */
    public static NSMutableOrderedSet orderedSet() {
        ensureMutInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSMutableOrderedSet"), ObjC.sel("orderedSet"));
        return wrap(s);
    }

    /** [NSMutableOrderedSet orderedSetWithCapacity:] */
    public static NSMutableOrderedSet orderedSetWithCapacity(long capacity) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSMutableOrderedSet"), ObjC.sel("orderedSetWithCapacity:"), capacity);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("orderedSetWithCapacity: failed", t); }
    }

    private static synchronized void ensureMutInit() {
        if (initMut) return;
        try { NSOrderedSet.orderedSet(); } catch (Exception ignored) {}
        hAddObject = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hInsertAt = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hRemoveAt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hRemoveObject = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hRemoveAll = ObjC.handle(Sig.of(Ret.VOID));
        initMut = true;
    }

    /** addObject: */
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

    /** insertObject:atIndex: */
    public void insertObjectAtIndex(NSObject object, long index) {
        ensureMutInit();
        if (object == null) throw new IllegalArgumentException("insertObject: null");
        try { hInsertAt.invokeExact(peer, ObjC.sel("insertObject:atIndex:"), object.peer(), index); }
        catch (Throwable t) { throw new RuntimeException("insertObject:atIndex: failed", t); }
    }

    /** removeObjectAtIndex: */
    public void removeObjectAtIndex(long index) {
        ensureMutInit();
        try { hRemoveAt.invokeExact(peer, ObjC.sel("removeObjectAtIndex:"), index); }
        catch (Throwable t) { throw new RuntimeException("removeObjectAtIndex: failed", t); }
    }

    /** removeObject: */
    public void removeObject(NSObject object) {
        ensureMutInit();
        if (object == null) return;
        try { hRemoveObject.invokeExact(peer, ObjC.sel("removeObject:"), object.peer()); }
        catch (Throwable t) { throw new RuntimeException("removeObject: failed", t); }
    }

    /** removeAllObjects */
    public void removeAllObjects() {
        ensureMutInit();
        try { hRemoveAll.invokeExact(peer, ObjC.sel("removeAllObjects")); }
        catch (Throwable t) { throw new RuntimeException("removeAllObjects failed", t); }
    }

    /** addObjectsFromArray: */
    public void addObjectsFromArray(NSArray array) {
        ensureMutInit();
        if (array == null) return;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("addObjectsFromArray:"), array.peer());
        } catch (Throwable t) { throw new RuntimeException("addObjectsFromArray: failed", t); }
    }
}
