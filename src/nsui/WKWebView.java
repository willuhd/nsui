package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import nsui.objc.Blocks;
import nsui.objc.NsuiForeign;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// WKWebView — a web view displaying interactive web content (an NSView subclass).
///
/// Thin wrapper: every method maps to one `objc_msgSend` selector. The initializer
/// copies the configuration, so mutating it afterwards has no effect on the view.
/// macOS 10.10+ (individual members note their own floor).
///
/// Loading notes: `loadHTMLString:baseURL:` and `loadRequest:` return the new
/// navigation (raw id, null when nothing started); navigation itself is
/// asynchronous — pump the run loop before reading `title`/`URL`.
public class WKWebView extends NSView {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment alloc;
        static MemorySegment initWithFrame_configuration;
        static MemorySegment configuration;
        static MemorySegment title;
        static MemorySegment URL;
        static MemorySegment isLoading;
        static MemorySegment estimatedProgress;
        static MemorySegment hasOnlySecureContent;
        static MemorySegment canGoBack;
        static MemorySegment canGoForward;
        static MemorySegment goBack;
        static MemorySegment goForward;
        static MemorySegment reload;
        static MemorySegment reloadFromOrigin;
        static MemorySegment stopLoading;
        static MemorySegment loadRequest;
        static MemorySegment loadHTMLString_baseURL;
        static MemorySegment evaluateJavaScript_completionHandler;
        static MemorySegment navigationDelegate;
        static MemorySegment setNavigationDelegate;
        static MemorySegment UIDelegate;
        static MemorySegment setUIDelegate;
        static MemorySegment customUserAgent;
        static MemorySegment setCustomUserAgent;
        static MemorySegment allowsLinkPreview;
        static MemorySegment setAllowsLinkPreview;
        static MemorySegment allowsBackForwardNavigationGestures;
        static MemorySegment setAllowsBackForwardNavigationGestures;
        static MemorySegment pageZoom;
        static MemorySegment setPageZoom;
        static MemorySegment isInspectable;
        static MemorySegment setInspectable;
        static MemorySegment respondsToSelector;
        static void populate() {
            alloc = ObjC.sel("alloc");
            initWithFrame_configuration = ObjC.sel("initWithFrame:configuration:");
            configuration = ObjC.sel("configuration");
            title = ObjC.sel("title");
            URL = ObjC.sel("URL");
            isLoading = ObjC.sel("isLoading");
            estimatedProgress = ObjC.sel("estimatedProgress");
            hasOnlySecureContent = ObjC.sel("hasOnlySecureContent");
            canGoBack = ObjC.sel("canGoBack");
            canGoForward = ObjC.sel("canGoForward");
            goBack = ObjC.sel("goBack");
            goForward = ObjC.sel("goForward");
            reload = ObjC.sel("reload");
            reloadFromOrigin = ObjC.sel("reloadFromOrigin");
            stopLoading = ObjC.sel("stopLoading");
            loadRequest = ObjC.sel("loadRequest:");
            loadHTMLString_baseURL = ObjC.sel("loadHTMLString:baseURL:");
            evaluateJavaScript_completionHandler = ObjC.sel("evaluateJavaScript:completionHandler:");
            navigationDelegate = ObjC.sel("navigationDelegate");
            setNavigationDelegate = ObjC.sel("setNavigationDelegate:");
            UIDelegate = ObjC.sel("UIDelegate");
            setUIDelegate = ObjC.sel("setUIDelegate:");
            customUserAgent = ObjC.sel("customUserAgent");
            setCustomUserAgent = ObjC.sel("setCustomUserAgent:");
            allowsLinkPreview = ObjC.sel("allowsLinkPreview");
            setAllowsLinkPreview = ObjC.sel("setAllowsLinkPreview:");
            allowsBackForwardNavigationGestures = ObjC.sel("allowsBackForwardNavigationGestures");
            setAllowsBackForwardNavigationGestures = ObjC.sel("setAllowsBackForwardNavigationGestures:");
            pageZoom = ObjC.sel("pageZoom");
            setPageZoom = ObjC.sel("setPageZoom:");
            isInspectable = ObjC.sel("isInspectable");
            setInspectable = ObjC.sel("setInspectable:");
            respondsToSelector = ObjC.sel("respondsToSelector:");
        }
    }

    private record Handles(MethodHandle hInitFrameConfig, MethodHandle hId, MethodHandle hSetId,
            MethodHandle hBool, MethodHandle hSetBool, MethodHandle hDouble, MethodHandle hSetDouble,
            MethodHandle hVoid, MethodHandle hLoadHtml, MethodHandle hLoadRequest,
            MethodHandle hEvalJs, MethodHandle hResponds) {}
    private static volatile Handles handles;

    protected WKWebView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an id as WKWebView (null for nil).
    public static WKWebView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new WKWebView(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        // WKWebView lives in WebKit.framework; ObjC.ensureFramework loads it lazily.
        try { ObjC.ensureFramework("WebKit"); } catch (Throwable ignored) {}
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
        Sels.populate();
        handles = h;
    }

    /// `[[WKWebView alloc] initWithFrame:configuration:]` — macOS 10.10+.
    ///
    /// The designated initializer. Uses the given configuration's non-persistent
    /// store when one is set, keeping test runs off the shared WebsiteData on disk.
    public static WKWebView create(NSRect frame, WKWebViewConfiguration configuration) {
        ensureInit();
        Bounds.notNaN(frame, "initWithFrame:configuration:");
        Bounds.nonNull(configuration, "initWithFrame:configuration:");
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("WKWebView"), Sels.alloc);
        try {
            MemorySegment p = (MemorySegment) handles.hInitFrameConfig().invokeExact(
                    alloc, Sels.initWithFrame_configuration, frame.toSegment(),
                    (MemorySegment) (configuration == null ? MemorySegment.NULL : configuration.peer()));
            if (p.address() == 0) throw new IllegalStateException("initWithFrame:configuration: returned nil");
            return new WKWebView(p);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame:configuration: failed", t);
        }
    }

    /// `[[WKWebView alloc] initWithFrame:]` with the default configuration — macOS 10.10+.
    ///
    /// Prefer `create(frame, configuration)` with a non-persistent store in tests:
    /// the default configuration uses the shared persistent data store.
    public static WKWebView create(NSRect frame) {
        ensureInit();
        Bounds.notNaN(frame, "initWithFrame:");
        return new WKWebView(ObjC.newView("WKWebView", frame));
    }

    /// `[webView configuration]` — the copy the view was initialized with — macOS 10.10+.
    public WKWebViewConfiguration configuration() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.configuration);
            return WKWebViewConfiguration.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("configuration failed", t);
        }
    }

    /// `[webView title]` — the page title (nil-safe; empty before any load) — macOS 10.10+.
    public String title() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hId().invokeExact(peer, Sels.title));
        } catch (Throwable t) {
            throw new RuntimeException("title failed", t);
        }
    }

    /// `[webView URL]` — the active URL as a raw NSURL peer (null when none) — macOS 10.10+.
    public MemorySegment url() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.URL);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("URL failed", t);
        }
    }

    /// `[webView isLoading]` — macOS 10.10+.
    public boolean isLoading() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.isLoading);
        } catch (Throwable t) {
            throw new RuntimeException("isLoading failed", t);
        }
    }

    /// `[webView estimatedProgress]` — completed fraction of the current navigation — macOS 10.10+.
    public double estimatedProgress() {
        ensureInit();
        try {
            return (double) handles.hDouble().invokeExact(peer, Sels.estimatedProgress);
        } catch (Throwable t) {
            throw new RuntimeException("estimatedProgress failed", t);
        }
    }

    /// `[webView hasOnlySecureContent]` — macOS 10.10+.
    public boolean hasOnlySecureContent() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.hasOnlySecureContent);
        } catch (Throwable t) {
            throw new RuntimeException("hasOnlySecureContent failed", t);
        }
    }

    /// `[webView canGoBack]` — macOS 10.10+.
    public boolean canGoBack() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.canGoBack);
        } catch (Throwable t) {
            throw new RuntimeException("canGoBack failed", t);
        }
    }

    /// `[webView canGoForward]` — macOS 10.10+.
    public boolean canGoForward() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.canGoForward);
        } catch (Throwable t) {
            throw new RuntimeException("canGoForward failed", t);
        }
    }

    /// `[webView goBack]` — raw navigation peer, or null when there is no back item
    /// — macOS 10.10+.
    public MemorySegment goBack() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.goBack);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("goBack failed", t);
        }
    }

    /// `[webView goForward]` — raw navigation peer, or null when there is no forward item
    /// — macOS 10.10+.
    public MemorySegment goForward() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.goForward);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("goForward failed", t);
        }
    }

    /// `[webView reload]` — raw navigation peer, or null when there is nothing to reload
    /// — macOS 10.10+.
    public MemorySegment reload() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.reload);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("reload failed", t);
        }
    }

    /// `[webView reloadFromOrigin]` — end-to-end revalidation — macOS 10.10+.
    public MemorySegment reloadFromOrigin() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.reloadFromOrigin);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("reloadFromOrigin failed", t);
        }
    }

    /// `[webView stopLoading]` — macOS 10.10+.
    public void stopLoading() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, Sels.stopLoading);
        } catch (Throwable t) {
            throw new RuntimeException("stopLoading failed", t);
        }
    }

    /// `[webView loadRequest:]` — raw navigation peer, or null when nothing started
    /// — macOS 10.10+.
    public MemorySegment loadRequest(MemorySegment request) {
        ensureInit();
        Bounds.nonNull(request, "loadRequest:");
        try {
            MemorySegment p = (MemorySegment) handles.hLoadRequest().invokeExact(
                    peer, Sels.loadRequest, (MemorySegment) request);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("loadRequest: failed", t);
        }
    }

    /// `[webView loadHTMLString:baseURL:]` — raw navigation peer, or null when nothing
    /// started (a null base URL resolves relative URLs against nothing) — macOS 10.10+.
    public MemorySegment loadHTMLString(String html, MemorySegment baseURL) {
        ensureInit();
        if (html == null) throw new IllegalArgumentException("loadHTMLString:baseURL: requires a non-null string");
        try {
            MemorySegment p = (MemorySegment) handles.hLoadHtml().invokeExact(peer,
                    Sels.loadHTMLString_baseURL, ObjC.nsstring(html),
                    (MemorySegment) (baseURL == null ? MemorySegment.NULL : baseURL));
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("loadHTMLString:baseURL: failed", t);
        }
    }

    /// `[webView evaluateJavaScript:completionHandler:]` — the handler is a raw block
    /// peer (pass null for none) — macOS 10.10+.
    public void evaluateJavaScript(String script, MemorySegment completionHandler) {
        ensureInit();
        if (script == null) throw new IllegalArgumentException("evaluateJavaScript:completionHandler: requires a non-null script");
        try {
            handles.hEvalJs().invokeExact(peer, Sels.evaluateJavaScript_completionHandler,
                    ObjC.nsstring(script),
                    (MemorySegment) (completionHandler == null ? MemorySegment.NULL : completionHandler));
        } catch (Throwable t) {
            throw new RuntimeException("evaluateJavaScript:completionHandler: failed", t);
        }
    }

    /// `evaluateJavaScript:` with no completion handler — macOS 10.10+.
    public void evaluateJavaScript(String script) {
        evaluateJavaScript(script, null);
    }

    /// Pending `evaluateJavaScript:` result bodies keyed by block address.
    ///
    /// Each typed call installs its own global block, so concurrent scripts never
    /// take each other's result (the same per-block identity `NSWindow` sheet
    /// handlers and `CATransaction` completions use). Entries are removed when
    /// their block fires; a script whose page never runs it keeps its entry
    /// until delivery, bounded by the number of outstanding evaluates.
    private static final ConcurrentHashMap<Long, BiConsumer<MemorySegment, MemorySegment>> JS_HANDLERS =
            new ConcurrentHashMap<>();

    private static volatile MemorySegment jsResultStub;

    /// `[webView evaluateJavaScript:completionHandler:]` with a Java result callback
    /// — the first arg is the result object (NSNumber/NSString/NSNull), the
    /// second is the NSError peer (null on success) — macOS 10.10+.
    ///
    /// The callback runs on the WebKit callback thread under the FFM upcall
    /// stub; keep it short and never throw (a throwing body aborts the VM, the
    /// same fail-fast policy as `DelegateProxy` callbacks). Named
    /// `...WithResult` (rather than an overload) so a null handler stays
    /// unambiguous: raw `evaluateJavaScript(script, (MemorySegment) null)` is
    /// fire-and-forget, while this form always installs a block.
    public void evaluateJavaScriptWithResult(String script, BiConsumer<MemorySegment, MemorySegment> completion) {
        ensureInit();
        if (script == null) throw new IllegalArgumentException("evaluateJavaScript:completionHandler: requires a non-null script");
        if (completion == null) throw new IllegalArgumentException("evaluateJavaScriptWithResult: requires a non-null completion (use evaluateJavaScript(script) for fire-and-forget)");
        MemorySegment block = Blocks.blockWithStub(jsResultStub());
        long key = block.address();
        JS_HANDLERS.put(key, completion);
        try {
            handles.hEvalJs().invokeExact(peer, Sels.evaluateJavaScript_completionHandler,
                    ObjC.nsstring(script), (MemorySegment) block);
        } catch (Throwable t) {
            JS_HANDLERS.remove(key);
            throw new RuntimeException("evaluateJavaScript:completionHandler: failed", t);
        }
    }

    private static MemorySegment jsResultStub() {
        MemorySegment st = jsResultStub;
        if (st != null) return st;
        synchronized (WKWebView.class) {
            if (jsResultStub == null) {
                try {
                    MethodHandle target = MethodHandles.lookup().findStatic(WKWebView.class,
                            "jsResultThunk",
                            MethodType.methodType(void.class, MemorySegment.class,
                                    MemorySegment.class, MemorySegment.class));
                    jsResultStub = Blocks.stub(target, NsuiForeign.wkJsResultUpcall());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("cannot resolve JS result block target", e);
                }
            }
            return jsResultStub;
        }
    }

    /// Block body for typed `evaluateJavaScript:` — routes via block address.
    ///
    /// CONTRACT: runs under the FFM upcall stub; throwing aborts the VM. Catch
    /// errors inside the completion instead. Null result/error peers stay null
    /// (nil result with nil error means the script produced no value).
    ///
    /// Public because `NsuiFeature` (nsui.objc) resolves it at build time.
    public static void jsResultThunk(MemorySegment blockSelf, MemorySegment result, MemorySegment error) {
        if (blockSelf == null || blockSelf.address() == 0) return;
        BiConsumer<MemorySegment, MemorySegment> body = JS_HANDLERS.remove(blockSelf.address());
        if (body == null) return;
        MemorySegment r = (result == null || result.address() == 0) ? null : result;
        MemorySegment e = (error == null || error.address() == 0) ? null : error;
        body.accept(r, e);
    }

    /// `[webView navigationDelegate]` — raw delegate peer (weak, null when none)
    /// — macOS 10.10+.
    public MemorySegment navigationDelegate() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.navigationDelegate);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("navigationDelegate failed", t);
        }
    }

    /// `[webView setNavigationDelegate:]` — pass null for nil — macOS 10.10+.
    public void setNavigationDelegate(MemorySegment delegate) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setNavigationDelegate,
                    (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate));
        } catch (Throwable t) {
            throw new RuntimeException("setNavigationDelegate: failed", t);
        }
    }

    /// `[webView UIDelegate]` — raw delegate peer (weak, null when none) — macOS 10.10+.
    public MemorySegment uiDelegate() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, Sels.UIDelegate);
            return (p == null || p.address() == 0) ? null : p;
        } catch (Throwable t) {
            throw new RuntimeException("UIDelegate failed", t);
        }
    }

    /// `[webView setUIDelegate:]` — pass null for nil — macOS 10.10+.
    public void setUIDelegate(MemorySegment delegate) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setUIDelegate,
                    (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate));
        } catch (Throwable t) {
            throw new RuntimeException("setUIDelegate: failed", t);
        }
    }

    /// `[webView customUserAgent]` — the custom user-agent override (nil-safe) — macOS 10.11+.
    public String customUserAgent() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hId().invokeExact(peer, Sels.customUserAgent));
        } catch (Throwable t) {
            throw new RuntimeException("customUserAgent failed", t);
        }
    }

    /// `[webView setCustomUserAgent:]` — pass null for nil — macOS 10.11+.
    public void setCustomUserAgent(String agent) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setCustomUserAgent,
                    (MemorySegment) (agent == null ? MemorySegment.NULL : ObjC.nsstring(agent)));
        } catch (Throwable t) {
            throw new RuntimeException("setCustomUserAgent: failed", t);
        }
    }

    /// `[webView allowsLinkPreview]` — macOS 10.11+.
    public boolean allowsLinkPreview() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.allowsLinkPreview);
        } catch (Throwable t) {
            throw new RuntimeException("allowsLinkPreview failed", t);
        }
    }

    /// `[webView setAllowsLinkPreview:]` — macOS 10.11+.
    public void setAllowsLinkPreview(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setAllowsLinkPreview, flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsLinkPreview: failed", t);
        }
    }

    /// `[webView allowsBackForwardNavigationGestures]` — swipe navigation.
    public boolean allowsBackForwardNavigationGestures() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, Sels.allowsBackForwardNavigationGestures);
        } catch (Throwable t) {
            throw new RuntimeException("allowsBackForwardNavigationGestures failed", t);
        }
    }

    /// `[webView setAllowsBackForwardNavigationGestures:]`.
    public void setAllowsBackForwardNavigationGestures(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setAllowsBackForwardNavigationGestures, flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsBackForwardNavigationGestures: failed", t);
        }
    }

    /// `[webView pageZoom]` — the page zoom factor — macOS 11.0+; 1.0 when absent.
    public double pageZoom() {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.pageZoom);
            if (!responds) return 1.0;
            return (double) handles.hDouble().invokeExact(peer, Sels.pageZoom);
        } catch (Throwable t) {
            throw new RuntimeException("pageZoom failed", t);
        }
    }

    /// `[webView setPageZoom:]` — macOS 11.0+; no-op when absent.
    public void setPageZoom(double zoom) {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.setPageZoom);
            if (!responds) return;
            handles.hSetDouble().invokeExact(peer, Sels.setPageZoom, zoom);
        } catch (Throwable t) {
            throw new RuntimeException("setPageZoom: failed", t);
        }
    }

    /// `[webView isInspectable]` — whether web inspector may attach — macOS 13.3+;
    /// false when absent.
    public boolean isInspectable() {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.isInspectable);
            if (!responds) return false;
            return (boolean) handles.hBool().invokeExact(peer, Sels.isInspectable);
        } catch (Throwable t) {
            throw new RuntimeException("isInspectable failed", t);
        }
    }

    /// `[webView setInspectable:]` — macOS 13.3+; no-op when absent.
    public void setInspectable(boolean flag) {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.setInspectable);
            if (!responds) return;
            handles.hSetBool().invokeExact(peer, Sels.setInspectable, flag);
        } catch (Throwable t) {
            throw new RuntimeException("setInspectable: failed", t);
        }
    }
}
