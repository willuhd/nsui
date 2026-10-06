package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSUserDefaults (Foundation) — the app preferences domain: typed scalar
/// and string accessors plus removal. Thin stateless wrapper.
/// OMITTED: floatForKey:/setFloat:forKey: — FLOAT shapes (FLOAT,ID / VOID,FLOAT,ID)
/// are NOT in the Sig vocabulary (verified by grep: no FLOAT,ID entry); and
/// initWithUser: (deprecated).
public final class NSUserDefaults extends NSObject {

    private record Handles(MethodHandle hIntId, MethodHandle hDoubleId, MethodHandle hBoolId,
            MethodHandle hSetObj, MethodHandle hSetInt, MethodHandle hSetDouble, MethodHandle hSetBool) {}
    private static volatile Handles handles;

    private NSUserDefaults(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSUserDefaults wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSUserDefaults(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL, Arg.ID)));
    }

    /// standardUserDefaults.
    public static NSUserDefaults standard() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("NSUserDefaults"), ObjC.sel("standardUserDefaults")));
    }

    private static MemorySegment key(String name) {
        return ObjC.nsstring(name);
    }

    /// stringForKey: (nil-safe).
    public String stringForKey(String name) {
        return ObjC.toString(ObjC.msgSendIdId(peer, ObjC.sel("stringForKey:"), key(name)));
    }

    /// integerForKey:.
    public long integerForKey(String name) {
        try {
            return (long) handles.hIntId().invokeExact(peer, ObjC.sel("integerForKey:"), key(name));
        } catch (Throwable t) {
            throw new RuntimeException("integerForKey: failed", t);
        }
    }

    /// doubleForKey:.
    public double doubleForKey(String name) {
        try {
            return (double) handles.hDoubleId().invokeExact(peer, ObjC.sel("doubleForKey:"), key(name));
        } catch (Throwable t) {
            throw new RuntimeException("doubleForKey: failed", t);
        }
    }

    /// boolForKey:.
    public boolean boolForKey(String name) {
        try {
            return (boolean) handles.hBoolId().invokeExact(peer, ObjC.sel("boolForKey:"), key(name));
        } catch (Throwable t) {
            throw new RuntimeException("boolForKey: failed", t);
        }
    }

    /// setObject:forKey: (NSString value).
    public void setStringForKey(String value, String name) {
        try {
            handles.hSetObj().invokeExact(peer, ObjC.sel("setObject:forKey:"), ObjC.nsstring(value), key(name));
        } catch (Throwable t) {
            throw new RuntimeException("setObject:forKey: failed", t);
        }
    }

    /// setInteger:forKey:.
    public void setIntegerForKey(long value, String name) {
        try {
            handles.hSetInt().invokeExact(peer, ObjC.sel("setInteger:forKey:"), value, key(name));
        } catch (Throwable t) {
            throw new RuntimeException("setInteger:forKey: failed", t);
        }
    }

    /// setDouble:forKey:.
    public void setDoubleForKey(double value, String name) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setDouble:forKey:"), value, key(name));
        } catch (Throwable t) {
            throw new RuntimeException("setDouble:forKey: failed", t);
        }
    }

    /// setBool:forKey:.
    public void setBoolForKey(boolean value, String name) {
        try {
            handles.hSetBool().invokeExact(peer, ObjC.sel("setBool:forKey:"), value, key(name));
        } catch (Throwable t) {
            throw new RuntimeException("setBool:forKey: failed", t);
        }
    }

    /// removeObjectForKey:.
    public void removeObjectForKey(String name) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeObjectForKey:"), key(name));
    }

    /// resetStandardUserDefaults (class).
    public static void resetStandardUserDefaults() {
        ObjC.msgSendVoid(ObjC.cls("NSUserDefaults"), ObjC.sel("resetStandardUserDefaults"));
    }

    /// initWithSuiteName: (nil searches the default search list).
    public static NSUserDefaults withSuiteName(String suitename) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSUserDefaults"), ObjC.sel("alloc"));
        try {
            MemorySegment q = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID)).invokeExact(p,
                    ObjC.sel("initWithSuiteName:"),
                    (MemorySegment) (suitename == null ? MemorySegment.NULL : ObjC.nsstring(suitename)));
            return wrap(q);
        } catch (Throwable t) { throw new RuntimeException("initWithSuiteName: failed", t); }
    }

    /// objectForKey: (generic id, nil-safe as NULL peer).
    public MemorySegment objectForKey(String name) {
        return ObjC.msgSendIdId(peer, ObjC.sel("objectForKey:"), key(name));
    }

    /// arrayForKey: / dictionaryForKey: / dataForKey: / stringArrayForKey:.
    public NSArray arrayForKey(String name) {
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("arrayForKey:"), key(name)));
    }
    public NSDictionary dictionaryForKey(String name) {
        return NSDictionary.wrap(ObjC.msgSendIdId(peer, ObjC.sel("dictionaryForKey:"), key(name)));
    }
    public NSData dataForKey(String name) {
        return NSData.wrap(ObjC.msgSendIdId(peer, ObjC.sel("dataForKey:"), key(name)));
    }
    public NSArray stringArrayForKey(String name) {
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("stringArrayForKey:"), key(name)));
    }

    /// URLForKey: (NSURL as generic id, nil-safe).
    public MemorySegment URLForKey(String name) {
        return ObjC.msgSendIdId(peer, ObjC.sel("URLForKey:"), key(name));
    }

    /// setURL:forKey: (NSURL as generic id; nil clears).
    public void setURLForKey(MemorySegment url, String name) {
        try {
            ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)).invokeExact(peer, ObjC.sel("setURL:forKey:"),
                    (MemorySegment) (url == null ? MemorySegment.NULL : url), key(name));
        } catch (Throwable t) { throw new RuntimeException("setURL:forKey: failed", t); }
    }

    /// setObject:forKey: generic id overload (nil removes).
    public void setObjectForKey(MemorySegment value, String name) {
        try {
            handles.hSetObj().invokeExact(peer, ObjC.sel("setObject:forKey:"),
                    (MemorySegment) (value == null ? MemorySegment.NULL : value), key(name));
        } catch (Throwable t) { throw new RuntimeException("setObject:forKey: failed", t); }
    }

    /// registerDefaults:.
    public void registerDefaults(NSDictionary dict) {
        ObjC.msgSendVoidId(peer, ObjC.sel("registerDefaults:"),
                (MemorySegment) (dict == null ? MemorySegment.NULL : dict.peer()));
    }

    /// addSuiteNamed: / removeSuiteNamed:.
    public void addSuiteNamed(String suite) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addSuiteNamed:"),
                (MemorySegment) (suite == null ? MemorySegment.NULL : ObjC.nsstring(suite)));
    }
    public void removeSuiteNamed(String suite) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeSuiteNamed:"),
                (MemorySegment) (suite == null ? MemorySegment.NULL : ObjC.nsstring(suite)));
    }

    /// dictionaryRepresentation.
    public NSDictionary dictionaryRepresentation() {
        return NSDictionary.wrap(ObjC.msgSendId(peer, ObjC.sel("dictionaryRepresentation")));
    }

    /// volatileDomainNames / volatileDomainForName: / setVolatileDomain:forName: / removeVolatileDomainForName:.
    public NSArray volatileDomainNames() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("volatileDomainNames")));
    }
    public NSDictionary volatileDomainForName(String domain) {
        return NSDictionary.wrap(ObjC.msgSendIdId(peer, ObjC.sel("volatileDomainForName:"),
                (MemorySegment) (domain == null ? MemorySegment.NULL : ObjC.nsstring(domain))));
    }
    public void setVolatileDomainForName(NSDictionary domain, String name) {
        try {
            ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)).invokeExact(peer, ObjC.sel("setVolatileDomain:forName:"),
                    (MemorySegment) (domain == null ? MemorySegment.NULL : domain.peer()),
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)));
        } catch (Throwable t) { throw new RuntimeException("setVolatileDomain:forName: failed", t); }
    }
    public void removeVolatileDomainForName(String domain) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeVolatileDomainForName:"),
                (MemorySegment) (domain == null ? MemorySegment.NULL : ObjC.nsstring(domain)));
    }

    /// persistentDomainForName: / setPersistentDomain:forName: / removePersistentDomainForName:.
    public NSDictionary persistentDomainForName(String domain) {
        return NSDictionary.wrap(ObjC.msgSendIdId(peer, ObjC.sel("persistentDomainForName:"),
                (MemorySegment) (domain == null ? MemorySegment.NULL : ObjC.nsstring(domain))));
    }
    public void setPersistentDomainForName(NSDictionary domain, String name) {
        try {
            ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)).invokeExact(peer, ObjC.sel("setPersistentDomain:forName:"),
                    (MemorySegment) (domain == null ? MemorySegment.NULL : domain.peer()),
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)));
        } catch (Throwable t) { throw new RuntimeException("setPersistentDomain:forName: failed", t); }
    }
    public void removePersistentDomainForName(String domain) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removePersistentDomainForName:"),
                (MemorySegment) (domain == null ? MemorySegment.NULL : ObjC.nsstring(domain)));
    }

    /// synchronize (deprecated upstream but still in the header).
    public boolean synchronize() {
        return ObjC.msgSendBool(peer, ObjC.sel("synchronize"));
    }

    /// objectIsForcedForKey:.
    public boolean objectIsForcedForKey(String name) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer,
                    ObjC.sel("objectIsForcedForKey:"), key(name));
        } catch (Throwable t) { throw new RuntimeException("objectIsForcedForKey: failed", t); }
    }
    public boolean objectIsForcedForKeyInDomain(String key, String domain) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("objectIsForcedForKey:inDomain:"),
                    (MemorySegment) (key == null ? MemorySegment.NULL : ObjC.nsstring(key)),
                    (MemorySegment) (domain == null ? MemorySegment.NULL : ObjC.nsstring(domain)));
        } catch (Throwable t) { throw new RuntimeException("objectIsForcedForKey:inDomain: failed", t); }
    }
}
