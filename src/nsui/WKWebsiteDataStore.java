package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// WKWebsiteDataStore — the cookies, caches and persistent data backing web views.
///
/// Thin wrapper. `-init`/`+new` are NS_UNAVAILABLE, so there is no `alloc`/`init`
/// factory here: `create()` returns a fresh non-persistent store (the test-safe
/// default — nothing is written to the file system). `defaultDataStore()` exposes
/// the shared persistent store; prefer the non-persistent form in tests.
/// Every method maps to one `objc_msgSend` selector. macOS 10.11+.
public class WKWebsiteDataStore extends NSObject {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment defaultDataStore;
        static MemorySegment nonPersistentDataStore;
        static MemorySegment isPersistent;
        static MemorySegment httpCookieStore;
        static MemorySegment allWebsiteDataTypes;
        static MemorySegment identifier;
        static MemorySegment respondsToSelector;
        static void populate() {
            defaultDataStore = ObjC.sel("defaultDataStore");
            nonPersistentDataStore = ObjC.sel("nonPersistentDataStore");
            isPersistent = ObjC.sel("isPersistent");
            httpCookieStore = ObjC.sel("httpCookieStore");
            allWebsiteDataTypes = ObjC.sel("allWebsiteDataTypes");
            identifier = ObjC.sel("identifier");
            respondsToSelector = ObjC.sel("respondsToSelector:");
        }
    }

    private record Handles(MethodHandle hId, MethodHandle hBool, MethodHandle hResponds) {}
    private static volatile Handles handles;

    protected WKWebsiteDataStore(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an id as WKWebsiteDataStore (null for nil).
    public static WKWebsiteDataStore wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new WKWebsiteDataStore(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        // WKWebsiteDataStore lives in WebKit.framework; ObjC.ensureFramework loads it lazily.
        try { ObjC.ensureFramework("WebKit"); } catch (Throwable ignored) {}
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
        Sels.populate();
        handles = h;
    }

    /// Fresh non-persistent store (`+nonPersistentDataStore`) — macOS 10.11+.
    ///
    /// This is the hand-written factory for this class (`-init`/`+new` are
    /// NS_UNAVAILABLE, so an `alloc`/`init` `create()` cannot exist). Nothing is
    /// written to the file system for views using this store ("private browsing" mode).
    public static WKWebsiteDataStore create() {
        return nonPersistentDataStore();
    }

    /// `+nonPersistentDataStore` — a new store that writes nothing to disk — macOS 10.11+.
    public static WKWebsiteDataStore nonPersistentDataStore() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(
                    ObjC.cls("WKWebsiteDataStore"), Sels.nonPersistentDataStore);
            if (p.address() == 0) throw new IllegalStateException("nonPersistentDataStore returned nil");
            return new WKWebsiteDataStore(p);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("nonPersistentDataStore failed", t);
        }
    }

    /// `+defaultDataStore` — the shared persistent store — macOS 10.11+.
    ///
    /// Prefer `create()` (non-persistent) in tests: the default store reads and writes
    /// the app's WebsiteData on disk.
    public static WKWebsiteDataStore defaultDataStore() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(
                    ObjC.cls("WKWebsiteDataStore"), Sels.defaultDataStore);
            if (p.address() == 0) throw new IllegalStateException("defaultDataStore returned nil");
            return new WKWebsiteDataStore(p);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("defaultDataStore failed", t);
        }
    }

    /// `[store isPersistent]` — false for non-persistent stores — macOS 10.11+.
    public boolean isPersistent() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.isPersistent);
        } catch (Throwable t) {
            throw new RuntimeException("isPersistent failed", t);
        }
    }

    /// `[store httpCookieStore]` — the cookie store (raw id, never nil on a live store)
    /// — macOS 10.13+ (below the guard threshold, sent unconditionally).
    public MemorySegment httpCookieStore() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.httpCookieStore);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("httpCookieStore failed", t);
        }
    }

    /// `[store httpCookieStore]` as a typed wrapper — macOS 10.13+.
    ///
    /// Same peer as `httpCookieStore()`, wrapped for the cookie-sync helpers
    /// (`getAllCookies:`, `setCookie:`, `deleteCookie:`). The store is owned
    /// by this data store; do not retain it beyond the store.
    public WKHTTPCookieStore cookieStore() {
        return WKHTTPCookieStore.wrap(httpCookieStore());
    }

    /// `+allWebsiteDataTypes` — every website data type the store knows — macOS 10.11+.
    public static NSSet allWebsiteDataTypes() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(
                    ObjC.cls("WKWebsiteDataStore"), Sels.allWebsiteDataTypes);
            return NSSet.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("allWebsiteDataTypes failed", t);
        }
    }

    /// `[store identifier]` — the persistent identifier, or null — macOS 14.0+; null when absent.
    ///
    /// Nil for the default and non-persistent stores even where present, so a null
    /// return is the expected value for every store this toolkit creates.
    public MemorySegment identifier() {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.identifier);
            if (!responds) return null;
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.identifier);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("identifier failed", t);
        }
    }
}
