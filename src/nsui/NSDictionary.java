package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSDictionary — minimal typed wrapper over a native {@code NSDictionary} (id).
 * Thin, stateless: every method maps to one {@code objc_msgSend}.
 * Works for both NSDictionary and NSMutableDictionary.
 */
public final class NSDictionary extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hCount;          // (id, SEL) -> long [count]
    private static MethodHandle hObjectForKey;   // (id, SEL, id) -> id [objectForKey:]
    private static MethodHandle hSetObjectForKey;// (id, SEL, id, id) -> void [setObject:forKey:] (NSMutableDictionary)
    private static MethodHandle hRemoveObject;   // (id, SEL, id) -> void [removeObjectForKey:]
    private static MethodHandle hAllKeys;        // (id, SEL) -> id [allKeys]

    private NSDictionary(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /** Wrap a native NSDictionary id (null for nil). */
    public static NSDictionary wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDictionary(peer);
    }

    /** Create an empty dictionary via {@code [NSDictionary dictionary]}. */
    public static NSDictionary dictionary() {
        ensureInit();
        MemorySegment d = ObjC.msgSendId(ObjC.cls("NSDictionary"), ObjC.sel("dictionary"));
        return wrap(d);
    }

    /** Create an empty mutable dictionary via {@code [NSMutableDictionary dictionary]}. */
    public static NSDictionary mutableDictionary() {
        ensureInit();
        MemorySegment d = ObjC.msgSendId(ObjC.cls("NSMutableDictionary"), ObjC.sel("dictionary"));
        return wrap(d);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hCount = ObjC.handle(Sig.of(Ret.INT));
        hObjectForKey = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hSetObjectForKey = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
        hRemoveObject = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hAllKeys = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
    }

    /** count — number of key/value pairs. */
    public long count() {
        ensureInit();
        try {
            return (long) hCount.invokeExact(peer, ObjC.sel("count"));
        } catch (Throwable t) {
            throw new RuntimeException("NSDictionary count failed", t);
        }
    }

    public boolean isEmpty() { return count() == 0; }

    /** objectForKey: — value for key or null. */
    public MemorySegment objectForKey(MemorySegment key) {
        ensureInit();
        if (key == null || key.address() == 0) return null;
        try {
            MemorySegment v = (MemorySegment) hObjectForKey.invokeExact(peer, ObjC.sel("objectForKey:"), key);
            return (v == null || v.address() == 0) ? null : v;
        } catch (Throwable t) {
            throw new RuntimeException("objectForKey: failed", t);
        }
    }

    /** Typed objectForKey with NSObject key. */
    public MemorySegment objectForKey(NSObject key) {
        return objectForKey(key == null ? null : key.peer());
    }

    /** objectForKey: with NSString key convenience. */
    public MemorySegment objectForKey(String key) {
        if (key == null) return null;
        return objectForKey(ObjC.nsstring(key));
    }

    /** setObject:forKey: — mutating (NSMutableDictionary). */
    public void setObjectForKey(MemorySegment object, MemorySegment key) {
        ensureInit();
        if (object == null || object.address() == 0) throw new IllegalArgumentException("setObject: null");
        if (key == null || key.address() == 0) throw new IllegalArgumentException("forKey: null");
        try {
            hSetObjectForKey.invokeExact(peer, ObjC.sel("setObject:forKey:"), object, key);
        } catch (Throwable t) {
            throw new RuntimeException("setObject:forKey: failed", t);
        }
    }

    /** setObject:forKey: with NSObject args. */
    public void setObjectForKey(NSObject object, NSObject key) {
        setObjectForKey((MemorySegment) (object == null ? MemorySegment.NULL : object.peer()),
                (MemorySegment) (key == null ? MemorySegment.NULL : key.peer()));
    }

    /** removeObjectForKey: — mutating. */
    public void removeObjectForKey(MemorySegment key) {
        ensureInit();
        if (key == null || key.address() == 0) return;
        try {
            hRemoveObject.invokeExact(peer, ObjC.sel("removeObjectForKey:"), key);
        } catch (Throwable t) {
            throw new RuntimeException("removeObjectForKey: failed", t);
        }
    }

    /** allKeys — returns NSArray of keys. */
    public NSArray allKeys() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) hAllKeys.invokeExact(peer, ObjC.sel("allKeys"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("allKeys failed", t);
        }
    }
}
