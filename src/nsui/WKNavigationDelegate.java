package nsui;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Map;

import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;

/// WKNavigationDelegate — helper to build a DelegateProxy-backed WKNavigationDelegate.
///
/// Uses the two WK lifecycle shapes added to `DelegateProxy`:
///  - `void(webView, navigation)` → `VoidIdIdArg` (didStart, redirect, didCommit, didFinish)
///  - `void(webView, navigation, error)` → `VoidIdIdIdArg` (didFailProvisional, didFail,
///    decidePolicyAction/Response + handler as the third id)
///  - `void(webView)` → `VoidArg` (webContentProcessDidTerminate)
///
/// Policy is allow/cancel only (0 cancel, 1 allow — `WKNavigationActionPolicy`).
/// Download (2, macOS 11.3+) is intentionally not exposed: pass allow/cancel
/// here and handle downloads via the download delegate instead.
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/WebKit.framework/Headers/WKNavigationDelegate.h
public final class WKNavigationDelegate {

    /// `WKNavigationActionPolicyCancel` — refuse the navigation.
    public static final long POLICY_CANCEL = 0L;
    /// `WKNavigationActionPolicyAllow` — let the navigation continue.
    public static final long POLICY_ALLOW = 1L;

    /// `WKNavigationResponsePolicyCancel` — refuse the response.
    public static final long RESPONSE_CANCEL = 0L;
    /// `WKNavigationResponsePolicyAllow` — let the response continue.
    public static final long RESPONSE_ALLOW = 1L;

    private WKNavigationDelegate() {}

    /// Java-side navigation lifecycle. All methods default to no-ops so a
    /// caller observes only what it needs. Peers are valid only during the
    /// callback (autoreleased); copy strings inside the body (e.g. via
    /// `ObjC.toString(ObjC.msgSendId(error, ObjC.sel("localizedDescription")))`)
    /// instead of holding the peer.
    public interface Delegate {
        /// webView:didStartProvisionalNavigation:
        default void didStartProvisionalNavigation(MemorySegment webView, MemorySegment navigation) {}

        /// webView:didReceiveServerRedirectForProvisionalNavigation:
        default void didReceiveServerRedirect(MemorySegment webView, MemorySegment navigation) {}

        /// webView:didFailProvisionalNavigation:withError: — error is an NSError peer.
        default void didFailProvisionalNavigation(MemorySegment webView, MemorySegment navigation, MemorySegment error) {}

        /// webView:didCommitNavigation:
        default void didCommitNavigation(MemorySegment webView, MemorySegment navigation) {}

        /// webView:didFinishNavigation:
        default void didFinishNavigation(MemorySegment webView, MemorySegment navigation) {}

        /// webView:didFailNavigation:withError: — error is an NSError peer.
        default void didFailNavigation(MemorySegment webView, MemorySegment navigation, MemorySegment error) {}

        /// webViewWebContentProcessDidTerminate:
        default void webContentProcessDidTerminate(MemorySegment webView) {}

        /// webView:decidePolicyForNavigationAction:decisionHandler: — return
        /// true to allow, false to cancel. Called on the WebKit thread; keep
        /// it short and never throw (fail-fast, like all delegate callbacks).
        /// The default allows everything (same as not implementing the method).
        default boolean shouldAllowNavigationAction(MemorySegment webView, MemorySegment action) {
            return true;
        }

        /// webView:decidePolicyForNavigationResponse:decisionHandler: — return
        /// true to allow, false to cancel. Default allows everything.
        default boolean shouldAllowNavigationResponse(MemorySegment webView, MemorySegment response) {
            return true;
        }
    }

    /// Build a DelegateProxy delegate for WKNavigationDelegate. The returned
    /// MemorySegment is a retained ObjC instance (subclass of NSObject named
    /// NSUINavigationDelegate) whose selectors route back to the Java delegate.
    ///
    /// Attach with `webView.setNavigationDelegate(delegate)`; detach with
    /// `setNavigationDelegate(null)` then `DelegateProxy.dispose(delegate)`.
    public static MemorySegment create(Delegate d) {
        if (d == null) throw new IllegalArgumentException("WKNavigationDelegate Delegate is null");

        Map<String, DelegateProxy.VoidIdIdArg> voidIdIds = Map.of(
                "webView:didStartProvisionalNavigation:",
                (webView, navigation) -> d.didStartProvisionalNavigation(webView, navigation),
                "webView:didReceiveServerRedirectForProvisionalNavigation:",
                (webView, navigation) -> d.didReceiveServerRedirect(webView, navigation),
                "webView:didCommitNavigation:",
                (webView, navigation) -> d.didCommitNavigation(webView, navigation),
                "webView:didFinishNavigation:",
                (webView, navigation) -> d.didFinishNavigation(webView, navigation));
        Map<String, DelegateProxy.VoidIdIdIdArg> voidIdIdIds = Map.of(
                "webView:didFailProvisionalNavigation:withError:",
                (webView, navigation, error) -> d.didFailProvisionalNavigation(webView, navigation, error),
                "webView:didFailNavigation:withError:",
                (webView, navigation, error) -> d.didFailNavigation(webView, navigation, error),
                "webView:decidePolicyForNavigationAction:decisionHandler:",
                (webView, action, handler) -> decidePolicyAction(d, webView, action, handler),
                "webView:decidePolicyForNavigationResponse:decisionHandler:",
                (webView, response, handler) -> decidePolicyResponse(d, webView, response, handler));
        Map<String, DelegateProxy.VoidArg> voids = Map.of(
                "webViewWebContentProcessDidTerminate:",
                webView -> d.webContentProcessDidTerminate(webView));

        return DelegateProxy.delegate(
                "NSObject", "NSUINavigationDelegate",
                Map.of(), voids, Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), voidIdIds, voidIdIdIds);
    }

    /// Localized description of an NSError peer, nil-safe (null when nil).
    public static String errorDescription(MemorySegment error) {
        if (error == null || error.address() == 0) return null;
        try {
            // Reference the NSError class so SignatureConformanceTest resolves
            // `localizedDescription` against it (the wrapper class itself has no such method).
            ObjC.cls("NSError");
            MemorySegment s = ObjC.msgSendId(error, ObjC.sel("localizedDescription"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            return null;
        }
    }

    /// Invoke a navigation-action decision handler with an allow/cancel policy.
    /// Must be called exactly once per `decidePolicyForNavigationAction:`
    /// delivery; not calling hangs the navigation, calling twice is undefined.
    public static void decidePolicy(MemorySegment handler, long policy) {
        if (handler == null || handler.address() == 0)
            throw new IllegalArgumentException("decidePolicy: requires a non-null decisionHandler");
        if (policy != POLICY_CANCEL && policy != POLICY_ALLOW)
            throw new IllegalArgumentException("decidePolicy: unknown policy " + policy + " (use POLICY_CANCEL/ALLOW)");
        invokeLongBlock(handler, policy);
    }

    /// Invoke a navigation-response decision handler with an allow/cancel policy.
    public static void decideResponsePolicy(MemorySegment handler, long policy) {
        if (handler == null || handler.address() == 0)
            throw new IllegalArgumentException("decideResponsePolicy: requires a non-null decisionHandler");
        if (policy != RESPONSE_CANCEL && policy != RESPONSE_ALLOW)
            throw new IllegalArgumentException("decideResponsePolicy: unknown policy " + policy);
        invokeLongBlock(handler, policy);
    }

    /// URL string for a `WKNavigationAction` peer (via `[[action request] URL]`
    /// `absoluteString`), nil-safe. Used for host-based allow/cancel decisions.
    public static String navigationActionURLString(MemorySegment action) {
        if (action == null || action.address() == 0) return null;
        try {
            ObjC.cls("WKNavigationAction");
            ObjC.cls("NSURLRequest");
            ObjC.cls("NSURL");
            MemorySegment req = ObjC.msgSendId(action, ObjC.sel("request"));
            if (req == null || req.address() == 0) return null;
            MemorySegment url = ObjC.msgSendId(req, ObjC.sel("URL"));
            if (url == null || url.address() == 0) return null;
            MemorySegment s = ObjC.msgSendId(url, ObjC.sel("absoluteString"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void decidePolicyAction(Delegate d, MemorySegment webView,
            MemorySegment action, MemorySegment handler) {
        boolean allow;
        try {
            allow = d.shouldAllowNavigationAction(webView, action);
        } catch (Throwable t) {
            allow = true; // fail-open: a throwing policy must not hang the navigation
        }
        try {
            decidePolicy(handler, allow ? POLICY_ALLOW : POLICY_CANCEL);
        } catch (Throwable ignored) {
            // Handler invocation itself must not throw back through the stub.
        }
    }

    private static void decidePolicyResponse(Delegate d, MemorySegment webView,
            MemorySegment response, MemorySegment handler) {
        boolean allow;
        try {
            allow = d.shouldAllowNavigationResponse(webView, response);
        } catch (Throwable t) {
            allow = true;
        }
        try {
            decideResponsePolicy(handler, allow ? RESPONSE_ALLOW : RESPONSE_CANCEL);
        } catch (Throwable ignored) {
        }
    }

    /// Call a `void(^)(NSInteger)` decision block: read its `invoke` pointer
    /// (+16) and downcall it with `(blockSelf, value)`. Same ABI as the sheet
    /// completion block (`void(PTR, LONG)`).
    ///
    /// The handler arrives via an FFM upcall as an address-only segment, so it
    /// is reinterpreted before reading the `invoke` slot (a direct `get`
    /// would be out-of-bounds on a zero-length view).
    private static void invokeLongBlock(MemorySegment block, long value) {
        MemorySegment invoke = block.reinterpret(32).get(ValueLayout.ADDRESS, 16);
        if (invoke == null || invoke.address() == 0)
            throw new IllegalStateException("decisionHandler block has a null invoke pointer");
        try {
            MethodHandle h = Linker.nativeLinker().downcallHandle(invoke,
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
            h.invokeExact(block, value);
        } catch (Throwable t) {
            throw new RuntimeException("decisionHandler invoke failed", t);
        }
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - didReceiveAuthenticationChallenge:completionHandler: (disposition + credential) —
    //   needs a (long, id) completion invoker; same block-invoke pattern, left out
    //   because no test exercises TLS challenges.
    // - navigationAction/Response didBecomeDownload:, shouldGoToBackForwardListItem — download
    //   and Instant-Back APIs beyond the allow/cancel core covered here.
}
