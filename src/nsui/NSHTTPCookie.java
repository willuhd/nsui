package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSHTTPCookie — a single HTTP cookie (an NSObject subclass).
///
/// Thin wrapper: every method maps to one `objc_msgSend` selector. Cookies are
/// value-like: `cookieWithProperties:` builds one from a property dictionary
/// (keys are the `NSHTTPCookie*` name strings such as "Name", "Value",
/// "Domain", "Path"), and the getters expose the live fields. macOS 10.2+
/// (no version-gated members here).
public final class NSHTTPCookie extends NSObject {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment alloc;
        static MemorySegment initWithProperties;
        static MemorySegment cookieWithProperties;
        static MemorySegment properties;
        static MemorySegment name;
        static MemorySegment value;
        static MemorySegment domain;
        static MemorySegment path;
        static MemorySegment version;
        static MemorySegment expiresDate;
        static MemorySegment isSecure;
        static MemorySegment isHTTPOnly;
        static MemorySegment isSessionOnly;
        static void populate() {
            alloc = ObjC.sel("alloc");
            initWithProperties = ObjC.sel("initWithProperties:");
            cookieWithProperties = ObjC.sel("cookieWithProperties:");
            properties = ObjC.sel("properties");
            name = ObjC.sel("name");
            value = ObjC.sel("value");
            domain = ObjC.sel("domain");
            path = ObjC.sel("path");
            version = ObjC.sel("version");
            expiresDate = ObjC.sel("expiresDate");
            isSecure = ObjC.sel("isSecure");
            isHTTPOnly = ObjC.sel("isHTTPOnly");
            isSessionOnly = ObjC.sel("isSessionOnly");
        }
    }

    private record Handles(MethodHandle hId, MethodHandle hIdId, MethodHandle hBool,
            MethodHandle hInt) {}
    private static volatile Handles handles;

    private NSHTTPCookie(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an id as NSHTTPCookie (null for nil).
    public static NSHTTPCookie wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSHTTPCookie(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.INT)));
        Sels.populate();
        handles = h;
    }

    /// `[[NSHTTPCookie alloc] initWithProperties:]` — nil when the dictionary
    /// lacks a name/value or a domain/origin URL.
    public static NSHTTPCookie create(NSDictionary properties) {
        ensureInit();
        if (properties == null) throw new IllegalArgumentException("initWithProperties: requires non-null properties");
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSHTTPCookie"), Sels.alloc);
        try {
            MemorySegment p = (MemorySegment) handles.hIdId().invokeExact(alloc, Sels.initWithProperties, properties.peer());
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithProperties: failed", t);
        }
    }

    /// `+[NSHTTPCookie cookieWithProperties:]` — autoreleased convenience,
    /// nil under the same conditions as `create`.
    public static NSHTTPCookie cookieWithProperties(NSDictionary properties) {
        ensureInit();
        if (properties == null) throw new IllegalArgumentException("cookieWithProperties: requires non-null properties");
        try {
            MemorySegment p = (MemorySegment) handles.hIdId().invokeExact(
                    ObjC.cls("NSHTTPCookie"), Sels.cookieWithProperties, properties.peer());
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("cookieWithProperties: failed", t);
        }
    }

    /// `[cookie properties]` — the dictionary the cookie was built from.
    public NSDictionary properties() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.properties);
            return NSDictionary.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("properties failed", t);
        }
    }

    /// `[cookie name]` — nil-safe.
    public String name() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hId().invokeExact(peer, Sels.name));
        } catch (Throwable t) {
            throw new RuntimeException("name failed", t);
        }
    }

    /// `[cookie value]` — nil-safe.
    public String value() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hId().invokeExact(peer, Sels.value));
        } catch (Throwable t) {
            throw new RuntimeException("value failed", t);
        }
    }

    /// `[cookie domain]` — nil-safe.
    public String domain() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hId().invokeExact(peer, Sels.domain));
        } catch (Throwable t) {
            throw new RuntimeException("domain failed", t);
        }
    }

    /// `[cookie path]` — nil-safe.
    public String path() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hId().invokeExact(peer, Sels.path));
        } catch (Throwable t) {
            throw new RuntimeException("path failed", t);
        }
    }

    /// `[cookie version]` — the cookie version (0 for Netscape-style).
    public long version() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, Sels.version);
        } catch (Throwable t) {
            throw new RuntimeException("version failed", t);
        }
    }

    /// `[cookie expiresDate]` — raw NSDate peer, or null for session cookies.
    public MemorySegment expiresDate() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.expiresDate);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("expiresDate failed", t);
        }
    }

    /// `[cookie isSecure]`.
    public boolean isSecure() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.isSecure);
        } catch (Throwable t) {
            throw new RuntimeException("isSecure failed", t);
        }
    }

    /// `[cookie isHTTPOnly]`.
    public boolean isHTTPOnly() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.isHTTPOnly);
        } catch (Throwable t) {
            throw new RuntimeException("isHTTPOnly failed", t);
        }
    }

    /// `[cookie isSessionOnly]`.
    public boolean isSessionOnly() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.isSessionOnly);
        } catch (Throwable t) {
            throw new RuntimeException("isSessionOnly failed", t);
        }
    }
}
