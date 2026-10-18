package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// WKWebViewConfiguration — the properties used to initialize a WKWebView.
///
/// Thin wrapper: a value bag copied by `-initWithFrame:configuration:`, so mutating
/// it after the web view exists has no effect on that view. Every method maps to one
/// `objc_msgSend` selector. macOS 10.10+ (individual properties note their own floor).
public class WKWebViewConfiguration extends NSObject {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment alloc;
        static MemorySegment init;
        static MemorySegment processPool;
        static MemorySegment setProcessPool;
        static MemorySegment preferences;
        static MemorySegment setPreferences;
        static MemorySegment userContentController;
        static MemorySegment websiteDataStore;
        static MemorySegment setWebsiteDataStore;
        static MemorySegment applicationNameForUserAgent;
        static MemorySegment setApplicationNameForUserAgent;
        static MemorySegment allowsAirPlayForMediaPlayback;
        static MemorySegment setAllowsAirPlayForMediaPlayback;
        static MemorySegment suppressesIncrementalRendering;
        static MemorySegment setSuppressesIncrementalRendering;
        static MemorySegment limitsNavigationsToAppBoundDomains;
        static MemorySegment setLimitsNavigationsToAppBoundDomains;
        static MemorySegment upgradeKnownHostsToHTTPS;
        static MemorySegment setUpgradeKnownHostsToHTTPS;
        static MemorySegment respondsToSelector;
        static void populate() {
            alloc = ObjC.sel("alloc");
            init = ObjC.sel("init");
            processPool = ObjC.sel("processPool");
            setProcessPool = ObjC.sel("setProcessPool:");
            preferences = ObjC.sel("preferences");
            setPreferences = ObjC.sel("setPreferences:");
            userContentController = ObjC.sel("userContentController");
            websiteDataStore = ObjC.sel("websiteDataStore");
            setWebsiteDataStore = ObjC.sel("setWebsiteDataStore:");
            applicationNameForUserAgent = ObjC.sel("applicationNameForUserAgent");
            setApplicationNameForUserAgent = ObjC.sel("setApplicationNameForUserAgent:");
            allowsAirPlayForMediaPlayback = ObjC.sel("allowsAirPlayForMediaPlayback");
            setAllowsAirPlayForMediaPlayback = ObjC.sel("setAllowsAirPlayForMediaPlayback:");
            suppressesIncrementalRendering = ObjC.sel("suppressesIncrementalRendering");
            setSuppressesIncrementalRendering = ObjC.sel("setSuppressesIncrementalRendering:");
            limitsNavigationsToAppBoundDomains = ObjC.sel("limitsNavigationsToAppBoundDomains");
            setLimitsNavigationsToAppBoundDomains = ObjC.sel("setLimitsNavigationsToAppBoundDomains:");
            upgradeKnownHostsToHTTPS = ObjC.sel("upgradeKnownHostsToHTTPS");
            setUpgradeKnownHostsToHTTPS = ObjC.sel("setUpgradeKnownHostsToHTTPS:");
            respondsToSelector = ObjC.sel("respondsToSelector:");
        }
    }

    private record Handles(MethodHandle hId, MethodHandle hSetId, MethodHandle hBool,
            MethodHandle hSetBool, MethodHandle hResponds) {}
    private static volatile Handles handles;

    protected WKWebViewConfiguration(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an id as WKWebViewConfiguration (null for nil).
    public static WKWebViewConfiguration wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new WKWebViewConfiguration(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        // WKWebViewConfiguration lives in WebKit.framework; ObjC.ensureFramework loads it lazily.
        try { ObjC.ensureFramework("WebKit"); } catch (Throwable ignored) {}
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
        Sels.populate();
        handles = h;
    }

    /// `[[WKWebViewConfiguration alloc] init]` — macOS 10.10+.
    public static WKWebViewConfiguration create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("WKWebViewConfiguration"), Sels.alloc);
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(alloc, Sels.init);
            if (p.address() == 0) throw new IllegalStateException("WKWebViewConfiguration init returned nil");
            return new WKWebViewConfiguration(p);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("WKWebViewConfiguration init failed", t);
        }
    }

    /// `[config processPool]` — the web content process pool — macOS 10.10+.
    public WKProcessPool processPool() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.processPool);
            return WKProcessPool.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("processPool failed", t);
        }
    }

    /// `[config setProcessPool:]` — macOS 10.10+.
    public void setProcessPool(WKProcessPool pool) {
        ensureInit();
        Bounds.nonNull(pool, "setProcessPool:");
        try {
            handles.hSetId().invokeExact(peer, Sels.setProcessPool,
                    (MemorySegment) (pool == null ? MemorySegment.NULL : pool.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setProcessPool: failed", t);
        }
    }

    /// `[config preferences]` — raw preferences peer (never nil on a live config) — macOS 10.10+.
    public MemorySegment preferences() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.preferences);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("preferences failed", t);
        }
    }

    /// `[config userContentController]` — raw user-content-controller peer — macOS 10.10+.
    public MemorySegment userContentController() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.userContentController);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("userContentController failed", t);
        }
    }

    /// `[config websiteDataStore]` — the website data store — macOS 10.11+.
    public WKWebsiteDataStore websiteDataStore() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.websiteDataStore);
            return WKWebsiteDataStore.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("websiteDataStore failed", t);
        }
    }

    /// `[config setWebsiteDataStore:]` — macOS 10.11+.
    public void setWebsiteDataStore(WKWebsiteDataStore store) {
        ensureInit();
        Bounds.nonNull(store, "setWebsiteDataStore:");
        try {
            handles.hSetId().invokeExact(peer, Sels.setWebsiteDataStore,
                    (MemorySegment) (store == null ? MemorySegment.NULL : store.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setWebsiteDataStore: failed", t);
        }
    }

    /// `[config applicationNameForUserAgent]` — appended to the user-agent string (nil-safe)
    /// — macOS 10.11+.
    public String applicationNameForUserAgent() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hId().invokeExact(
                    peer, Sels.applicationNameForUserAgent));
        } catch (Throwable t) {
            throw new RuntimeException("applicationNameForUserAgent failed", t);
        }
    }

    /// `[config setApplicationNameForUserAgent:]` — pass null for nil — macOS 10.11+.
    public void setApplicationNameForUserAgent(String name) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setApplicationNameForUserAgent,
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)));
        } catch (Throwable t) {
            throw new RuntimeException("setApplicationNameForUserAgent: failed", t);
        }
    }

    /// `[config allowsAirPlayForMediaPlayback]` — macOS 10.11+.
    public boolean allowsAirPlayForMediaPlayback() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.allowsAirPlayForMediaPlayback);
        } catch (Throwable t) {
            throw new RuntimeException("allowsAirPlayForMediaPlayback failed", t);
        }
    }

    /// `[config setAllowsAirPlayForMediaPlayback:]` — macOS 10.11+.
    public void setAllowsAirPlayForMediaPlayback(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setAllowsAirPlayForMediaPlayback, flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsAirPlayForMediaPlayback: failed", t);
        }
    }

    /// `[config suppressesIncrementalRendering]` — macOS 10.10+.
    public boolean suppressesIncrementalRendering() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.suppressesIncrementalRendering);
        } catch (Throwable t) {
            throw new RuntimeException("suppressesIncrementalRendering failed", t);
        }
    }

    /// `[config setSuppressesIncrementalRendering:]` — macOS 10.10+.
    public void setSuppressesIncrementalRendering(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setSuppressesIncrementalRendering, flag);
        } catch (Throwable t) {
            throw new RuntimeException("setSuppressesIncrementalRendering: failed", t);
        }
    }

    /// `[config limitsNavigationsToAppBoundDomains]` — macOS 11.0+; false when absent.
    public boolean limitsNavigationsToAppBoundDomains() {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.limitsNavigationsToAppBoundDomains);
            if (!responds) return false;
            return (boolean) handles.hBool().invokeExact(peer, Sels.limitsNavigationsToAppBoundDomains);
        } catch (Throwable t) {
            throw new RuntimeException("limitsNavigationsToAppBoundDomains failed", t);
        }
    }

    /// `[config setLimitsNavigationsToAppBoundDomains:]` — macOS 11.0+; no-op when absent.
    public void setLimitsNavigationsToAppBoundDomains(boolean flag) {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.setLimitsNavigationsToAppBoundDomains);
            if (!responds) return;
            handles.hSetBool().invokeExact(peer, Sels.setLimitsNavigationsToAppBoundDomains, flag);
        } catch (Throwable t) {
            throw new RuntimeException("setLimitsNavigationsToAppBoundDomains: failed", t);
        }
    }

    /// `[config upgradeKnownHostsToHTTPS]` — macOS 11.3+; true (the documented default) when absent.
    public boolean upgradeKnownHostsToHTTPS() {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.upgradeKnownHostsToHTTPS);
            if (!responds) return true;
            return (boolean) handles.hBool().invokeExact(peer, Sels.upgradeKnownHostsToHTTPS);
        } catch (Throwable t) {
            throw new RuntimeException("upgradeKnownHostsToHTTPS failed", t);
        }
    }

    /// `[config setUpgradeKnownHostsToHTTPS:]` — macOS 11.3+; no-op when absent.
    public void setUpgradeKnownHostsToHTTPS(boolean flag) {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.setUpgradeKnownHostsToHTTPS);
            if (!responds) return;
            handles.hSetBool().invokeExact(peer, Sels.setUpgradeKnownHostsToHTTPS, flag);
        } catch (Throwable t) {
            throw new RuntimeException("setUpgradeKnownHostsToHTTPS: failed", t);
        }
    }
}
