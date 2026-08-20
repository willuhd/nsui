package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMutableSet — mutable set wrapper.
/// Extends NSSet for convenience; peer is an NSMutableSet instance.
public final class NSMutableSet extends NSSet {

    private static volatile boolean initMut;
    private static MethodHandle hAddObject;      // (id, SEL, id) -> void
    private static MethodHandle hRemoveObject;   // (id, SEL, id) -> void
    private static MethodHandle hRemoveAll;      // (id, SEL) -> void
    private static MethodHandle hUnion;          // (id, SEL, id) -> void
    private static MethodHandle hMinus;          // (id, SEL, id) -> void

    private NSMutableSet(MemorySegment peer) { super(peer); }

    public static NSMutableSet wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMutableSet(peer);
    }

    /// [NSMutableSet set] — empty mutable set.
    public static NSMutableSet set() {
        ensureMutInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSMutableSet"), ObjC.sel("set"));
        return wrap(s);
    }

    /// [NSMutableSet setWithCapacity:]
    public static NSMutableSet setWithCapacity(long capacity) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSMutableSet"), ObjC.sel("setWithCapacity:"), capacity);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("setWithCapacity: failed", t); }
    }

    private static synchronized void ensureMutInit() {
        if (initMut) return;
        // ensure NSSet handles also
        try { NSSet.set(); } catch (Exception ignored) {}
        hAddObject = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hRemoveObject = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hRemoveAll = ObjC.handle(Sig.of(Ret.VOID));
        hUnion = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hMinus = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
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

    /// removeObject:
    public void removeObject(NSObject object) {
        ensureMutInit();
        if (object == null) return;
        try { hRemoveObject.invokeExact(peer, ObjC.sel("removeObject:"), object.peer()); }
        catch (Throwable t) { throw new RuntimeException("removeObject: failed", t); }
    }

    public void removeObject(MemorySegment object) {
        ensureMutInit();
        if (object == null || object.address() == 0) return;
        try { hRemoveObject.invokeExact(peer, ObjC.sel("removeObject:"), object); }
        catch (Throwable t) { throw new RuntimeException("removeObject: failed", t); }
    }

    /// removeAllObjects
    public void removeAllObjects() {
        ensureMutInit();
        try { hRemoveAll.invokeExact(peer, ObjC.sel("removeAllObjects")); }
        catch (Throwable t) { throw new RuntimeException("removeAllObjects failed", t); }
    }

    /// unionSet:
    public void unionSet(NSSet other) {
        ensureMutInit();
        if (other == null) return;
        try { hUnion.invokeExact(peer, ObjC.sel("unionSet:"), other.peer()); }
        catch (Throwable t) { throw new RuntimeException("unionSet: failed", t); }
    }

    /// minusSet:
    public void minusSet(NSSet other) {
        ensureMutInit();
        if (other == null) return;
        try { hMinus.invokeExact(peer, ObjC.sel("minusSet:"), other.peer()); }
        catch (Throwable t) { throw new RuntimeException("minusSet: failed", t); }
    }

    /// intersectSet:
    public void intersectSet(NSSet other) {
        ensureMutInit();
        if (other == null) return;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("intersectSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("intersectSet: failed", t); }
    }

    /// setSet:
    public void setSet(NSSet other) {
        ensureMutInit();
        if (other == null) { removeAllObjects(); return; }
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("setSet: failed", t); }
    }
}
