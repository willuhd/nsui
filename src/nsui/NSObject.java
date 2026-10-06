package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;

/// L1 base class: a thin, stateless wrapper over a native Objective-C object (id).
///
/// Design rules (SWT-style): one wrapper per native object the toolkit owns;
/// transient msgSend results never create wrappers; the peer is the identity —
/// no state, no caching, no reflection. The wrapper exists to give selectors
/// Java-shaped signatures, nothing more.
///
/// Header-completeness (`NSObjCRuntime.h` / `NSObject.h`): every safe method whose
/// shape is in the Sig vocabulary is wrapped below. OMITTED — retain/release/autorelease
/// (arena memory model: factories retain, immortal by design; see NSData/NSValue);
/// performSelector: family (arbitrary-selector dispatch; use typed wrappers);
/// KVO observe/removeObserver: (context-pointer machinery; addObserver:forKeyPath:options:
/// context: also needs of(VOID,ID,ID,INT,ID), not in Sig); NSCoder/NSZone/Protocol plumbing
/// (initWithCoder:, copyWithZone:, classForCoder, replacementObjectForCoder:,
/// awakeAfterUsingCoder:, conformsToProtocol:, no wrapped peer types); IMP/NSInvocation
/// machinery (methodForSelector:, forwardInvocation:, methodSignatureForSelector:);
/// dealloc/finalize/load/initialize/poseAsClass: (runtime-owned); retainCount (discouraged
/// debugging aid); doesNotRecognizeSelector: (raises by design); autoContentAccessingProxy
/// (discardable-content behavior needs a conforming peer; not covered).
public class NSObject {

    /// The native Objective-C object (id).
    protected final MemorySegment peer;

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hIsKind; // (id, SEL, id) -> bool [isKindOfClass:]

    protected NSObject(MemorySegment peer) {
        this.peer = peer;
    }

    public MemorySegment peer() { return peer; }

    /// Wrap an id as an NSObject (null for nil).
    public static NSObject wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSObject(peer);
    }

    private static void ensureInit() {
        if (initialized) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (initialized) return;
        hIsKind = ObjC.handle(Sig.of(Sig.Ret.BOOL, Sig.Arg.ID));
        initialized = true;
    }

    /// `[receiver isKindOfClass:cls]` — runtime type check.
    /// Convenience wrapper around the ObjC `isKindOfClass:` selector so tests
    /// can verify the peer's class without reaching into `ObjC` directly.
    /// Moved here from NSSearchField so every NSView/NSControl inherits it.
    public boolean isKindOfClass(MemorySegment clazz) {
        ensureInit();
        try {
            return (boolean) hIsKind.invokeExact(peer, ObjC.sel("isKindOfClass:"), clazz);
        } catch (Throwable t) {
            throw new RuntimeException("isKindOfClass: failed", t);
        }
    }

    /// `isKindOfClass:` by class name (cached via `cls`).
    public boolean isKindOfClass(String className) {
        return isKindOfClass(ObjC.cls(className));
    }

    /// `[receiver description]` — the real ObjC description text (nil becomes null).
    /// Named `describe` per coordinator ruling: `NSColor.description()->String` is established
    /// API (3 committed call sites), so the base-class NSString-returning form takes this name.
    /// Distinct from Java `toString()`: subclasses like NSString override `toString()`
    /// with content, while this always asks the runtime.
    public NSString describe() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("description"));
            return NSString.wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("description failed", t);
        }
    }

    /// `[receiver hash]` — native hash (NSUInteger). Objects equal per `isEqual:`
    /// share a hash. Java `hashCode()` stays peer-identity by design (not overridden).
    public long hash() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("hash"));
        } catch (Throwable t) {
            throw new RuntimeException("hash failed", t);
        }
    }

    /// `[receiver isEqual:other]` — generic ObjC equality (false for nil).
    public boolean isEqual(NSObject other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.BOOL, Sig.Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqual:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("isEqual: failed", t);
        }
    }

    /// `[receiver class]` — the peer's Class object (never nil for a live peer).
    /// Named `objCClass` because Java `getClass()` is final.
    public MemorySegment objCClass() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("class"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) {
            throw new RuntimeException("class failed", t);
        }
    }

    /// `[receiver isMemberOfClass:cls]` — exact-class check (subclasses do NOT count,
    /// unlike `isKindOfClass:`). False for nil.
    public boolean isMemberOfClass(MemorySegment clazz) {
        ensureInit();
        if (clazz == null || clazz.address() == 0) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.BOOL, Sig.Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isMemberOfClass:"), clazz);
        } catch (Throwable t) {
            throw new RuntimeException("isMemberOfClass: failed", t);
        }
    }

    /// `isMemberOfClass:` by class name.
    public boolean isMemberOfClass(String className) {
        if (className == null) return false;
        return isMemberOfClass(ObjC.cls(className));
    }

    /// `[receiver respondsToSelector:sel]` — introspection without sending.
    /// Pass `ObjC.sel("...")`; unknown selectors safely return false (never throws).
    public boolean respondsToSelector(MemorySegment selector) {
        ensureInit();
        if (selector == null || selector.address() == 0) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.BOOL, Sig.Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("respondsToSelector:"), selector);
        } catch (Throwable t) {
            throw new RuntimeException("respondsToSelector: failed", t);
        }
    }

    /// `[receiver isProxy]` — true for NSProxy stand-ins (false for toolkit peers).
    public boolean isProxy() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.BOOL));
            return (boolean) h.invokeExact(peer, ObjC.sel("isProxy"));
        } catch (Throwable t) {
            throw new RuntimeException("isProxy failed", t);
        }
    }

    /// `[receiver copy]` — immutable copy (retained: immortal by design, like every
    /// factory in this toolkit; no side map, no release).
    public NSObject copy() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("copy"));
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("copy failed", t);
        }
    }

    /// `[receiver mutableCopy]` — mutable copy (retained: immortal by design).
    public NSObject mutableCopy() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("mutableCopy"));
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("mutableCopy failed", t);
        }
    }

    /// `[receiver valueForKey:key]` — KVC read (nil key returns nil).
    /// `NSDictionary` looks the key up (keys starting with `@` go to super) and
    /// `NSArray` maps the key over its elements; other classes raise a catchable
    /// `NSUnknownKeyException` for unknown keys.
    public MemorySegment valueForKey(MemorySegment key) {
        ensureInit();
        if (key == null || key.address() == 0) return null;
        MemorySegment r = ObjC.msgSendIdId(peer, ObjC.sel("valueForKey:"), key);
        return (r == null || r.address() == 0) ? null : r;
    }

    /// `valueForKey:` with a Java key string (nil-safe).
    public MemorySegment valueForKey(String key) {
        if (key == null) return null;
        return valueForKey(ObjC.nsstring(key));
    }

    /// `[receiver setValue:forKey:]` — KVC write (nil key is a no-op; a nil value
    /// sends NULL, which `NSMutableDictionary` treats as removal).
    public void setValueForKey(MemorySegment value, MemorySegment key) {
        ensureInit();
        if (key == null || key.address() == 0) return;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.VOID, Sig.Arg.ID, Sig.Arg.ID));
            h.invokeExact(peer, ObjC.sel("setValue:forKey:"),
                    (MemorySegment) (value == null ? MemorySegment.NULL : value), key);
        } catch (Throwable t) {
            throw new RuntimeException("setValue:forKey: failed", t);
        }
    }

    /// `setValue:forKey:` with an NSObject value and a Java key string.
    public void setValueForKey(NSObject value, String key) {
        if (key == null) return;
        setValueForKey(value == null ? null : value.peer(), ObjC.nsstring(key));
    }

    /// `[receiver willChangeValueForKey:]` — manual KVO notification (no-op without observers).
    public void willChangeValueForKey(String key) {
        ensureInit();
        if (key == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("willChangeValueForKey:"), ObjC.nsstring(key));
    }

    /// `[receiver didChangeValueForKey:]` — manual KVO notification (no-op without observers).
    public void didChangeValueForKey(String key) {
        ensureInit();
        if (key == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("didChangeValueForKey:"), ObjC.nsstring(key));
    }

    /// `+[Class version]` — archiving version for the named class.
    public static long version(String className) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.INT));
            return (long) h.invokeExact(ObjC.cls(className), ObjC.sel("version"));
        } catch (Throwable t) {
            throw new RuntimeException("version failed", t);
        }
    }

    /// `+[Class setVersion:]` — archiving version (mutates class state; restore after probing).
    public static void setVersion(String className, long version) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.VOID, Sig.Arg.INT));
            h.invokeExact(ObjC.cls(className), ObjC.sel("setVersion:"), version);
        } catch (Throwable t) {
            throw new RuntimeException("setVersion: failed", t);
        }
    }

    /// `+[Class supportsSecureCoding]` — secure-coding conformance for the named class.
    public static boolean supportsSecureCoding(String className) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.BOOL));
            return (boolean) h.invokeExact(ObjC.cls(className), ObjC.sel("supportsSecureCoding"));
        } catch (Throwable t) {
            throw new RuntimeException("supportsSecureCoding failed", t);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Long.toHexString(peer.address());
    }
}
