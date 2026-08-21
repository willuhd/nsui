package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSet — minimal typed wrapper over native `NSSet` / `NSMutableSet`.
/// Thin, stateless: every method maps to one `objc_msgSend`.
public class NSSet extends NSObject {

            private record Handles(MethodHandle hCount, MethodHandle hContains, MethodHandle hMember, MethodHandle hAnyObject) {}
    private static volatile Handles handles;

    protected NSSet(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSSet wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSet(peer);
    }

    /// [NSSet set] — empty immutable set.
    public static NSSet set() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSSet"), ObjC.sel("set"));
        return wrap(s);
    }

    /// [NSSet setWithObject:] — single object.
    public static NSSet setWithObject(NSObject object) {
        ensureInit();
        if (object == null) return set();
        MemorySegment s = ObjC.msgSendIdId(ObjC.cls("NSSet"), ObjC.sel("setWithObject:"), object.peer());
        return wrap(s);
    }

    /// [NSSet setWithArray:]
    public static NSSet setWithArray(NSArray array) {
        ensureInit();
        if (array == null) return set();
        MemorySegment s = ObjC.msgSendIdId(ObjC.cls("NSSet"), ObjC.sel("setWithArray:"), array.peer());
        return wrap(s);
    }

    /// [NSSet setWithObjects:count:] helper via array.
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
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID))
        );
    }

    /// count
    public long count() {
        ensureInit();
        try { return (long) handles.hCount().invokeExact(peer, ObjC.sel("count")); }
        catch (Throwable t) { throw new RuntimeException("NSSet count failed", t); }
    }

    public boolean isEmpty() { return count() == 0; }

    /// containsObject:
    public boolean containsObject(NSObject object) {
        ensureInit();
        if (object == null) return false;
        try { return (boolean) handles.hContains().invokeExact(peer, ObjC.sel("containsObject:"), object.peer()); }
        catch (Throwable t) { throw new RuntimeException("containsObject: failed", t); }
    }

    public boolean containsObject(MemorySegment object) {
        ensureInit();
        if (object == null || object.address() == 0) return false;
        try { return (boolean) handles.hContains().invokeExact(peer, ObjC.sel("containsObject:"), object); }
        catch (Throwable t) { throw new RuntimeException("containsObject: failed", t); }
    }

    /// member: — returns matching object or nil.
    public MemorySegment member(MemorySegment object) {
        ensureInit();
        if (object == null || object.address() == 0) return null;
        try {
            MemorySegment r = (MemorySegment) handles.hMember().invokeExact(peer, ObjC.sel("member:"), object);
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("member: failed", t); }
    }

    public NSObject member(NSObject object) {
        MemorySegment seg = member(object == null ? null : object.peer());
        return seg == null ? null : NSObject.wrap(seg);
    }

    /// anyObject
    public MemorySegment anyObject() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) handles.hAnyObject().invokeExact(peer, ObjC.sel("anyObject"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("anyObject failed", t); }
    }

    /// allObjects — NSArray of members.
    public NSArray allObjects() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) handles.hAnyObject().invokeExact(peer, ObjC.sel("allObjects"));
            return NSArray.wrap(r);
        } catch (Throwable t) { throw new RuntimeException("allObjects failed", t); }
    }

    /// isEqualToSet:
    public boolean isEqualToSet(NSSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isEqualToSet: failed", t); }
    }

    /// intersectsSet:
    public boolean intersectsSet(NSSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("intersectsSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("intersectsSet: failed", t); }
    }

    /// isSubsetOfSet:
    public boolean isSubsetOfSet(NSSet other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isSubsetOfSet:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isSubsetOfSet: failed", t); }
    }
}
