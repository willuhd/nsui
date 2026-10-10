package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSDictionary — minimal typed wrapper over a native `NSDictionary` (id).
/// Thin, stateless: every method maps to one `objc_msgSend`.
/// Works for both NSDictionary and NSMutableDictionary.
///
/// Header-completeness (`NSDictionary.h`, immutable + mutable): every safe method whose
/// shape is in the Sig vocabulary is wrapped below. Mutating selectors raise an (uncatchable,
/// process-fatal) NSException on immutable peers — call them only on `mutableDictionary()`
/// peers. OMITTED — getObjects:andKeys:(:count:) (out-buffer plumbing, deprecated);
/// writeToURL:error: and initWithContentsOfURL:error:/dictionaryWithContentsOfURL:error:
/// (NSError** out-params); enumerateKeysAndObjects.../keysSortedByValueUsingComparator:.../
/// keysSortedByValueWithOptions:.../keysOfEntries... (blocks); file/deprecated contents
/// variants (deprecated or NSError**); countByEnumeratingWithState:... (fast-enumeration
/// buffer); init* (covered by dictionary()/dictionaryWithObject:.../dictionaryWithDictionary:/
/// dictionaryWithObjects:forKeys:, except coder/file variants needing NSCoder/NSURL/NSError**).
public final class NSDictionary extends NSObject {

            private record Handles(MethodHandle hCount, MethodHandle hObjectForKey, MethodHandle hSetObjectForKey, MethodHandle hRemoveObject, MethodHandle hAllKeys) {}
    private static volatile Handles handles;

    private NSDictionary(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native NSDictionary id (null for nil).
    public static NSDictionary wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDictionary(peer);
    }

    /// Create an empty dictionary via `[NSDictionary dictionary]`.
    public static NSDictionary dictionary() {
        ensureInit();
        MemorySegment d = ObjC.msgSendId(ObjC.cls("NSDictionary"), ObjC.sel("dictionary"));
        return wrap(d);
    }

    /// Create an empty mutable dictionary via `[NSMutableDictionary dictionary]`.
    public static NSDictionary mutableDictionary() {
        ensureInit();
        MemorySegment d = ObjC.msgSendId(ObjC.cls("NSMutableDictionary"), ObjC.sel("dictionary"));
        return wrap(d);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID))
        );
    }

    /// count — number of key/value pairs.
    public long count() {
        ensureInit();
        try {
            return (long) handles.hCount().invokeExact(peer, ObjC.sel("count"));
        } catch (Throwable t) {
            throw new RuntimeException("NSDictionary count failed", t);
        }
    }

    public boolean isEmpty() { return count() == 0; }

    /// objectForKey: — value for key or null.
    public MemorySegment objectForKey(MemorySegment key) {
        ensureInit();
        if (key == null || key.address() == 0) return null;
        try {
            MemorySegment v = (MemorySegment) handles.hObjectForKey().invokeExact(peer, ObjC.sel("objectForKey:"), key);
            return (v == null || v.address() == 0) ? null : v;
        } catch (Throwable t) {
            throw new RuntimeException("objectForKey: failed", t);
        }
    }

    /// Typed objectForKey with NSObject key.
    public MemorySegment objectForKey(NSObject key) {
        return objectForKey(key == null ? null : key.peer());
    }

    /// objectForKey: with NSString key convenience.
    public MemorySegment objectForKey(String key) {
        if (key == null) return null;
        return objectForKey(ObjC.nsstring(key));
    }

    /// setObject:forKey: — mutating (NSMutableDictionary).
    public void setObjectForKey(MemorySegment object, MemorySegment key) {
        ensureInit();
        if (object == null || object.address() == 0) throw new IllegalArgumentException("setObject: null");
        if (key == null || key.address() == 0) throw new IllegalArgumentException("forKey: null");
        try {
            handles.hSetObjectForKey().invokeExact(peer, ObjC.sel("setObject:forKey:"), object, key);
        } catch (Throwable t) {
            throw new RuntimeException("setObject:forKey: failed", t);
        }
    }

    /// setObject:forKey: with NSObject args.
    public void setObjectForKey(NSObject object, NSObject key) {
        setObjectForKey((MemorySegment) (object == null ? MemorySegment.NULL : object.peer()),
                (MemorySegment) (key == null ? MemorySegment.NULL : key.peer()));
    }

    /// removeObjectForKey: — mutating.
    public void removeObjectForKey(MemorySegment key) {
        ensureInit();
        if (key == null || key.address() == 0) return;
        try {
            handles.hRemoveObject().invokeExact(peer, ObjC.sel("removeObjectForKey:"), key);
        } catch (Throwable t) {
            throw new RuntimeException("removeObjectForKey: failed", t);
        }
    }

    /// allKeys — returns NSArray of keys.
    public NSArray allKeys() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) handles.hAllKeys().invokeExact(peer, ObjC.sel("allKeys"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("allKeys failed", t);
        }
    }

    /// +dictionaryWithObject:forKey:.
    public static NSDictionary dictionaryWithObjectForKey(NSObject object, NSObject key) {
        ensureInit();
        if (object == null || key == null) throw new IllegalArgumentException("dictionaryWithObject:forKey: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment d = (MemorySegment) h.invokeExact(
                    ObjC.cls("NSDictionary"), ObjC.sel("dictionaryWithObject:forKey:"), object.peer(), key.peer());
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("dictionaryWithObject:forKey: failed", t);
        }
    }

    /// +dictionaryWithDictionary:.
    public static NSDictionary dictionaryWithDictionary(NSDictionary other) {
        ensureInit();
        if (other == null) return dictionary();
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSDictionary"), ObjC.sel("dictionaryWithDictionary:"), other.peer()));
    }

    /// +dictionaryWithObjects:forKeys: (parallel NSArrays).
    /// Note: no dictionaryWithObjects:forKeys:count: wrapper exists in this file
    /// (the Sig vocabulary lists the shape, but no Java method takes a count);
    /// document-only — no new guard. Mismatched NSArray counts raise natively
    /// (caller error; no live bad callers), so behavior is unchanged.
    public static NSDictionary dictionaryWithObjectsForKeys(NSArray objects, NSArray keys) {
        ensureInit();
        if (objects == null || keys == null) throw new IllegalArgumentException("dictionaryWithObjects:forKeys: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment d = (MemorySegment) h.invokeExact(
                    ObjC.cls("NSDictionary"), ObjC.sel("dictionaryWithObjects:forKeys:"), objects.peer(), keys.peer());
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("dictionaryWithObjects:forKeys: failed", t);
        }
    }

    /// +dictionaryWithCapacity: (NSMutableDictionary).
    public static NSDictionary dictionaryWithCapacity(long capacity) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            return wrap((MemorySegment) h.invokeExact(
                    ObjC.cls("NSMutableDictionary"), ObjC.sel("dictionaryWithCapacity:"), capacity));
        } catch (Throwable t) {
            throw new RuntimeException("dictionaryWithCapacity: failed", t);
        }
    }

    /// +sharedKeySetForKeys: — key set for shared-key-set dictionaries.
    public static MemorySegment sharedKeySetForKeys(NSArray keys) {
        ensureInit();
        if (keys == null) throw new IllegalArgumentException("sharedKeySetForKeys: null");
        MemorySegment r = ObjC.msgSendIdId(ObjC.cls("NSDictionary"), ObjC.sel("sharedKeySetForKeys:"), keys.peer());
        return (r == null || r.address() == 0) ? null : r;
    }

    /// +dictionaryWithSharedKeySet: (NSMutableDictionary) — pass a sharedKeySetForKeys: result.
    public static NSDictionary dictionaryWithSharedKeySet(MemorySegment keyset) {
        ensureInit();
        if (keyset == null || keyset.address() == 0) throw new IllegalArgumentException("dictionaryWithSharedKeySet: null");
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSMutableDictionary"), ObjC.sel("dictionaryWithSharedKeySet:"), keyset));
    }

    /// allKeysForObject:.
    public NSArray allKeysForObject(NSObject object) {
        ensureInit();
        if (object == null) return NSArray.array();
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("allKeysForObject:"), object.peer()));
    }

    /// allValues.
    public NSArray allValues() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("allValues")));
    }

    /// descriptionWithLocale: — NSLocale peer, or NULL for the canonical description.
    public NSString descriptionWithLocale(MemorySegment locale) {
        ensureInit();
        return NSString.wrap(ObjC.msgSendIdId(peer, ObjC.sel("descriptionWithLocale:"),
                (locale == null ? MemorySegment.NULL : locale)));
    }

    /// isEqualToDictionary:.
    public boolean isEqualToDictionary(NSDictionary other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToDictionary:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("isEqualToDictionary: failed", t);
        }
    }

    /// objectsForKeys:notFoundMarker: — values for keys, marker for misses.
    public NSArray objectsForKeysNotFoundMarker(NSArray keys, NSObject marker) {
        ensureInit();
        if (keys == null || marker == null) throw new IllegalArgumentException("objectsForKeys:notFoundMarker: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(
                    peer, ObjC.sel("objectsForKeys:notFoundMarker:"), keys.peer(), marker.peer());
            return NSArray.wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("objectsForKeys:notFoundMarker: failed", t);
        }
    }

    /// objectForKeyedSubscript: — native subscript read (same value as objectForKey:).
    public MemorySegment objectForKeyedSubscript(MemorySegment key) {
        ensureInit();
        if (key == null || key.address() == 0) return null;
        MemorySegment r = ObjC.msgSendIdId(peer, ObjC.sel("objectForKeyedSubscript:"), key);
        return (r == null || r.address() == 0) ? null : r;
    }

    /// objectForKeyedSubscript: with a Java key string.
    public MemorySegment objectForKeyedSubscript(String key) {
        if (key == null) return null;
        return objectForKeyedSubscript(ObjC.nsstring(key));
    }

    /// keyEnumerator — NSEnumerator peer or null (no NSEnumerator wrapper in this batch).
    public MemorySegment keyEnumerator() {
        ensureInit();
        MemorySegment r = ObjC.msgSendId(peer, ObjC.sel("keyEnumerator"));
        return (r == null || r.address() == 0) ? null : r;
    }

    /// objectEnumerator — NSEnumerator peer or null.
    public MemorySegment objectEnumerator() {
        ensureInit();
        MemorySegment r = ObjC.msgSendId(peer, ObjC.sel("objectEnumerator"));
        return (r == null || r.address() == 0) ? null : r;
    }

    /// keysSortedByValueUsingSelector: — SEL comparator over values (e.g. ObjC.sel("compare:"));
    /// must return NSComparisonResult. Raises (fatal) for bad selectors.
    public NSArray keysSortedByValueUsingSelector(MemorySegment comparator) {
        ensureInit();
        if (comparator == null || comparator.address() == 0)
            throw new IllegalArgumentException("keysSortedByValueUsingSelector: null");
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("keysSortedByValueUsingSelector:"), comparator));
    }

    /// addEntriesFromDictionary: — mutating.
    public void addEntriesFromDictionary(NSDictionary other) {
        ensureInit();
        if (other == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("addEntriesFromDictionary:"), other.peer());
    }

    /// removeAllObjects — mutating.
    public void removeAllObjects() {
        ensureInit();
        ObjC.msgSendVoid(peer, ObjC.sel("removeAllObjects"));
    }

    /// removeObjectsForKeys: — mutating.
    public void removeObjectsForKeys(NSArray keys) {
        ensureInit();
        if (keys == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("removeObjectsForKeys:"), keys.peer());
    }

    /// removeObjectForKey: with an NSObject key — mutating.
    public void removeObjectForKey(NSObject key) {
        removeObjectForKey(key == null ? null : key.peer());
    }

    /// setDictionary: — mutating (replace contents).
    public void setDictionary(NSDictionary other) {
        ensureInit();
        if (other == null) { removeAllObjects(); return; }
        ObjC.msgSendVoidId(peer, ObjC.sel("setDictionary:"), other.peer());
    }

    /// setObject:forKeyedSubscript: — mutating subscript write.
    public void setObjectForKeyedSubscript(NSObject object, NSObject key) {
        ensureInit();
        if (key == null) throw new IllegalArgumentException("setObject:forKeyedSubscript: null key");
        if (object == null) { removeObjectForKey(key.peer()); return; }
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setObject:forKeyedSubscript:"), object.peer(), key.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setObject:forKeyedSubscript: failed", t);
        }
    }
}
