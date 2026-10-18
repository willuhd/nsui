package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSApplication;
import nsui.NSRect;
import nsui.NSSet;
import nsui.NSView;
import nsui.NSWindow;
import nsui.WKProcessPool;
import nsui.WKWebView;
import nsui.WKWebViewConfiguration;
import nsui.WKWebsiteDataStore;
import nsui.objc.ObjC;

/// WKWebViewCoverageTest — coverage for the WebKit batch (4 files).
///
/// Hidden-only, never key, never shown: one never-shown window hosts the web view;
/// every store is non-persistent (never `defaultDataStore`, which would read and
/// write the app's WebsiteData on disk). Ends with TestKit.end().
///
/// Required coverage (batch spec):
/// - WKProcessPool create
/// - WKWebsiteDataStore non-persistent create / isPersistent / httpCookieStore /
///   allWebsiteDataTypes / guarded identifier (nil for non-persistent stores)
/// - WKWebViewConfiguration create + property round-trips (process pool, data store,
///   user-agent name, AirPlay, incremental rendering) + guarded 11.x toggles
/// - WKWebView create(frame, config) + state oracles (configuration, title, URL,
///   loading, progress, back/forward) + HTML load with title settle + script
///   evaluation with a null handler + guarded pageZoom / inspectable round-trips
/// - wrap(null)/wrap(NULL) == null for all four classes + Bounds rejections
///
/// Intentionally NOT exercised: `defaultDataStore` (persistent disk state),
/// navigation/UI delegates (need upcall machinery), cookie-store mutation.
public final class WKWebViewCoverageTest {

    private static void check(boolean ok, String msg) {
        TestKit.check(ok, msg);
        System.out.flush();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== WKWebViewCoverageTest ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            TestKit.skip("ObjC.init failed (not macOS / no AppKit): " + t);
        }
        NSApplication app = TestKit.app();

        System.out.println("---- wrap-null ----");
        check(WKProcessPool.wrap(null) == null, "WKProcessPool.wrap(null)==null");
        check(WKProcessPool.wrap(MemorySegment.NULL) == null, "WKProcessPool.wrap(NULL)==null");
        check(WKWebsiteDataStore.wrap(null) == null, "WKWebsiteDataStore.wrap(null)==null");
        check(WKWebsiteDataStore.wrap(MemorySegment.NULL) == null, "WKWebsiteDataStore.wrap(NULL)==null");
        check(WKWebViewConfiguration.wrap(null) == null, "WKWebViewConfiguration.wrap(null)==null");
        check(WKWebViewConfiguration.wrap(MemorySegment.NULL) == null, "WKWebViewConfiguration.wrap(NULL)==null");
        check(WKWebView.wrap(null) == null, "WKWebView.wrap(null)==null");
        check(WKWebView.wrap(MemorySegment.NULL) == null, "WKWebView.wrap(NULL)==null");

        System.out.println("---- WKProcessPool ----");
        WKProcessPool pool = TestKit.attempt("WKProcessPool.create()", WKProcessPool::create);
        check(pool != null, "process pool created");

        System.out.println("---- WKWebsiteDataStore (non-persistent only) ----");
        WKWebsiteDataStore store = TestKit.attempt("WKWebsiteDataStore.create()", WKWebsiteDataStore::create);
        check(store != null, "non-persistent store created");
        if (store != null) {
            check(!store.isPersistent(), "create() store is not persistent");
            WKWebsiteDataStore again = WKWebsiteDataStore.nonPersistentDataStore();
            check(again != null && !again.isPersistent(), "nonPersistentDataStore() is not persistent");
            check(store.httpCookieStore() != null, "httpCookieStore non-null");
            NSSet types = TestKit.attempt("allWebsiteDataTypes()", WKWebsiteDataStore::allWebsiteDataTypes);
            check(types != null && types.count() > 0, "allWebsiteDataTypes non-empty (count="
                    + (types == null ? "?" : types.count()) + ")");
            check(store.identifier() == null, "identifier() nil for non-persistent store");
        }

        System.out.println("---- WKWebViewConfiguration ----");
        WKWebViewConfiguration config = TestKit.attempt("WKWebViewConfiguration.create()",
                WKWebViewConfiguration::create);
        check(config != null, "configuration created");
        if (config != null && store != null) {
            check(config.processPool() != null, "default processPool non-null");
            check(config.preferences() != null, "default preferences non-null");
            check(config.userContentController() != null, "default userContentController non-null");
            check(config.websiteDataStore() != null, "default websiteDataStore non-null");
            check(config.applicationNameForUserAgent() == null, "default applicationNameForUserAgent nil");
            check(config.allowsAirPlayForMediaPlayback(), "allowsAirPlay defaults true");

            config.setWebsiteDataStore(store);
            check(!config.websiteDataStore().isPersistent(), "websiteDataStore round-trips non-persistent");

            config.setApplicationNameForUserAgent("nsui-test/1.0");
            check("nsui-test/1.0".equals(config.applicationNameForUserAgent()),
                    "applicationNameForUserAgent round-trips");
            config.setApplicationNameForUserAgent(null);
            check(config.applicationNameForUserAgent() == null, "applicationNameForUserAgent nil after clear");

            config.setAllowsAirPlayForMediaPlayback(false);
            check(!config.allowsAirPlayForMediaPlayback(), "allowsAirPlay toggles off");
            config.setAllowsAirPlayForMediaPlayback(true);

            boolean sir0 = config.suppressesIncrementalRendering();
            config.setSuppressesIncrementalRendering(!sir0);
            check(config.suppressesIncrementalRendering() == !sir0, "suppressesIncrementalRendering toggles");
            config.setSuppressesIncrementalRendering(sir0);

            if (pool != null) {
                TestKit.noThrow("setProcessPool(create())", () -> config.setProcessPool(pool));
            }

            if (config.respondsToSelector(ObjC.sel("limitsNavigationsToAppBoundDomains"))) {
                boolean v0 = config.limitsNavigationsToAppBoundDomains();
                config.setLimitsNavigationsToAppBoundDomains(!v0);
                check(config.limitsNavigationsToAppBoundDomains() == !v0,
                        "limitsNavigationsToAppBoundDomains toggles through native");
                config.setLimitsNavigationsToAppBoundDomains(v0);
            } else {
                TestKit.skipCase("limitsNavigationsToAppBoundDomains absent (guard not exercised)");
            }
            if (config.respondsToSelector(ObjC.sel("upgradeKnownHostsToHTTPS"))) {
                boolean v0 = config.upgradeKnownHostsToHTTPS();
                config.setUpgradeKnownHostsToHTTPS(!v0);
                check(config.upgradeKnownHostsToHTTPS() == !v0,
                        "upgradeKnownHostsToHTTPS toggles through native");
                config.setUpgradeKnownHostsToHTTPS(v0);
            } else {
                TestKit.skipCase("upgradeKnownHostsToHTTPS absent (guard not exercised)");
            }
        }

        System.out.println("---- WKWebView (hidden window) ----");
        NSWindow win = TestKit.hiddenWindow(800, 600);
        try {
            NSRect frame = new NSRect(0, 0, 800, 600);
            final WKWebViewConfiguration cfg = config;
            WKWebView webview = TestKit.attempt("WKWebView.create(frame, config)",
                    () -> WKWebView.create(frame, cfg));
            check(webview != null, "web view created with non-persistent config");
            if (webview == null) {
                TestKit.close(win);
                TestKit.end();
                return;
            }
            check(webview.isKindOfClass("WKWebView"), "isKindOfClass WKWebView");
            check(webview.isKindOfClass("NSView"), "WKWebView is an NSView");
            check(webview.configuration() != null, "configuration() non-null");
            String t0 = webview.title();
            check(t0 == null || t0.isEmpty(), "initial title null-or-empty (got '" + t0 + "')");
            check(webview.url() == null, "initial URL nil");
            check(!webview.isLoading(), "initially not loading");
            check(webview.estimatedProgress() == 0.0, "initial estimatedProgress 0");
            check(!webview.canGoBack() && !webview.canGoForward(), "no back/forward items initially");
            check(webview.goBack() == null, "goBack nil with empty history");
            check(webview.goForward() == null, "goForward nil with empty history");
            check(webview.navigationDelegate() == null, "no navigation delegate initially");
            check(webview.uiDelegate() == null, "no UI delegate initially");
            TestKit.noThrow("setNavigationDelegate(null)", () -> webview.setNavigationDelegate(null));
            TestKit.noThrow("setUIDelegate(null)", () -> webview.setUIDelegate(null));

            NSView content = win.contentView();
            content.addSubview(webview);
            check(webview.superview() != null, "web view attached to hidden window");

            MemorySegment nav = TestKit.attempt("loadHTMLString:baseURL:",
                    () -> webview.loadHTMLString(
                            "<html><head><title>Hello</title></head><body>hi</body></html>", null));
            check(nav != null, "loadHTMLString returns a navigation");
            String seen = null;
            for (int i = 0; i < 100 && !"Hello".equals(seen); i++) {
                TestKit.pumpOnce(app);
                try {
                    seen = webview.title();
                } catch (Throwable ignore) {
                    // keep settling; the assertion below reports the outcome
                }
            }
            check("Hello".equals(seen), "title settles to Hello after HTML load (got '" + seen + "')");
            TestKit.noThrow("evaluateJavaScript with null handler",
                    () -> webview.evaluateJavaScript("1 + 1"));
            TestKit.noThrow("stopLoading", webview::stopLoading);

            MemorySegment url = ObjC.msgSendIdId(ObjC.cls("NSURL"),
                    ObjC.sel("URLWithString:"), ObjC.nsstring("about:blank"));
            MemorySegment request = ObjC.msgSendIdId(ObjC.cls("NSURLRequest"),
                    ObjC.sel("requestWithURL:"), url);
            MemorySegment nav2 = TestKit.attempt("loadRequest: about:blank",
                    () -> webview.loadRequest(request));
            check(nav2 != null, "loadRequest returns a navigation");

            boolean lp0 = webview.allowsLinkPreview();
            webview.setAllowsLinkPreview(!lp0);
            check(webview.allowsLinkPreview() == !lp0, "allowsLinkPreview toggles");
            webview.setAllowsLinkPreview(lp0);

            boolean bf0 = webview.allowsBackForwardNavigationGestures();
            webview.setAllowsBackForwardNavigationGestures(!bf0);
            check(webview.allowsBackForwardNavigationGestures() == !bf0,
                    "allowsBackForwardNavigationGestures toggles");
            webview.setAllowsBackForwardNavigationGestures(bf0);

            webview.setCustomUserAgent("nsui-agent/1.0");
            check("nsui-agent/1.0".equals(webview.customUserAgent()), "customUserAgent round-trips");
            TestKit.noThrow("setCustomUserAgent(null)", () -> webview.setCustomUserAgent(null));

            if (webview.respondsToSelector(ObjC.sel("pageZoom"))) {
                double z0 = webview.pageZoom();
                webview.setPageZoom(1.5);
                check(Math.abs(webview.pageZoom() - 1.5) < 1e-9, "pageZoom round-trips through native");
                webview.setPageZoom(z0);
            } else {
                TestKit.skipCase("pageZoom absent (guard not exercised)");
            }
            if (webview.respondsToSelector(ObjC.sel("isInspectable"))) {
                boolean v0 = webview.isInspectable();
                webview.setInspectable(!v0);
                check(webview.isInspectable() == !v0, "inspectable toggles through native");
                webview.setInspectable(v0);
            } else {
                TestKit.skipCase("isInspectable absent (guard not exercised)");
            }

            TestKit.noThrow("goBack on empty history", webview::goBack);
            TestKit.noThrow("goForward on empty history", webview::goForward);
        } finally {
            TestKit.close(win);
        }

        System.out.println("---- Bounds rejections ----");
        NSRect frame = new NSRect(0, 0, 100, 100);
        final WKWebViewConfiguration cfg2 = config;
        TestKit.expectThrows("create(null, config) rejects", IllegalArgumentException.class,
                () -> WKWebView.create(null, cfg2));
        TestKit.expectThrows("create(frame, null) rejects", IllegalArgumentException.class,
                () -> WKWebView.create(frame, (WKWebViewConfiguration) null));
        TestKit.expectThrows("create(NaN frame) rejects", IllegalArgumentException.class,
                () -> WKWebView.create(new NSRect(Double.NaN, 0, 10, 10), cfg2));
        TestKit.expectThrows("loadRequest(NULL) rejects", IllegalArgumentException.class,
                () -> WKWebView.create(frame, cfg2).loadRequest(MemorySegment.NULL));
        TestKit.expectThrows("loadHTMLString(null) rejects", IllegalArgumentException.class,
                () -> WKWebView.create(frame, cfg2).loadHTMLString(null, null));
        TestKit.expectThrows("setProcessPool(null) rejects", IllegalArgumentException.class,
                () -> WKWebViewConfiguration.create().setProcessPool(null));
        TestKit.expectThrows("setWebsiteDataStore(null) rejects", IllegalArgumentException.class,
                () -> WKWebViewConfiguration.create().setWebsiteDataStore(null));

        TestKit.end();
    }
}
