package nsui;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import nsui.objc.Blocks;
import nsui.objc.NsuiForeign;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// WKHTTPCookieStore — the cookie jar for one WKWebsiteDataStore.
///
/// Thin wrapper: every method maps to one `objc_msgSend` selector. All cookie
/// reads and writes are asynchronous with completion blocks; this file turns
/// those blocks into Java callbacks via capture-less global blocks routed by
/// block address (the same pattern as `NSWindow` sheet handlers and
/// `CATransaction` completion blocks). macOS 10.13+ (individual members note
/// their own floor).
///
/// Lifetime: the store peer is owned by its website data store; do not retain
/// it beyond the store. Pending callbacks live until their block fires, so a
/// store dropped with callbacks in flight keeps those entries until delivery.
public final class WKHTTPCookieStore extends NSObject {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment getAllCookies;
        static MemorySegment setCookie_completionHandler;
        static MemorySegment setCookies_completionHandler;
        static MemorySegment deleteCookie_completionHandler;
        static MemorySegment addObserver;
        static MemorySegment removeObserver;
        static MemorySegment setCookiePolicy_completionHandler;
        static MemorySegment getCookiePolicy;
        static MemorySegment respondsToSelector;
        static void populate() {
            getAllCookies = ObjC.sel("getAllCookies:");
            setCookie_completionHandler = ObjC.sel("setCookie:completionHandler:");
            setCookies_completionHandler = ObjC.sel("setCookies:completionHandler:");
            deleteCookie_completionHandler = ObjC.sel("deleteCookie:completionHandler:");
            addObserver = ObjC.sel("addObserver:");
            removeObserver = ObjC.sel("removeObserver:");
            setCookiePolicy_completionHandler = ObjC.sel("setCookiePolicy:completionHandler:");
            getCookiePolicy = ObjC.sel("getCookiePolicy:");
            respondsToSelector = ObjC.sel("respondsToSelector:");
        }
    }

    private record Handles(MethodHandle hVoidId, MethodHandle hVoidIdId,
            MethodHandle hVoidIntId, MethodHandle hResponds) {}
    private static volatile Handles handles;

    /// Pending `getAllCookies:` bodies keyed by block address.
    private static final ConcurrentHashMap<Long, Consumer<List<NSHTTPCookie>>> COOKIE_HANDLERS =
            new ConcurrentHashMap<>();
    /// Pending `setCookie:`/`deleteCookie:` bodies keyed by block address.
    private static final ConcurrentHashMap<Long, Runnable> VOID_HANDLERS =
            new ConcurrentHashMap<>();
    /// Pending `getCookiePolicy:` bodies keyed by block address.
    private static final ConcurrentHashMap<Long, Consumer<Long>> POLICY_HANDLERS =
            new ConcurrentHashMap<>();

    private static volatile MemorySegment cookieArrayStub;
    private static volatile MemorySegment voidStub;
    private static volatile MemorySegment policyStub;

    private WKHTTPCookieStore(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an id as WKHTTPCookieStore (null for nil).
    public static WKHTTPCookieStore wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new WKHTTPCookieStore(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("WebKit"); } catch (Throwable ignored) {}
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
        Sels.populate();
        handles = h;
    }

    /// `[store getAllCookies:]` — fetch every cookie into a Java callback.
    ///
    /// The callback runs on the WebKit callback thread under the FFM upcall
    /// stub; keep it short and never throw (a throwing body aborts the VM,
    /// the same fail-fast policy as `DelegateProxy` callbacks). The list is a
    /// snapshot; elements are autoreleased peers valid until the pool drains.
    public void getAllCookies(Consumer<List<NSHTTPCookie>> completion) {
        ensureInit();
        if (completion == null) throw new IllegalArgumentException("getAllCookies: requires a non-null completion");
        MemorySegment block = Blocks.blockWithStub(cookieArrayStub());
        long key = block.address();
        COOKIE_HANDLERS.put(key, completion);
        try {
            handles.hVoidId().invokeExact(peer, Sels.getAllCookies, (MemorySegment) block);
        } catch (Throwable t) {
            COOKIE_HANDLERS.remove(key);
            throw new RuntimeException("getAllCookies: failed", t);
        }
    }

    /// `[store setCookie:completionHandler:]` — store one cookie; the runnable
    /// fires when the write lands (null for fire-and-forget).
    public void setCookie(NSHTTPCookie cookie, Runnable completion) {
        ensureInit();
        if (cookie == null) throw new IllegalArgumentException("setCookie:completionHandler: requires a non-null cookie");
        MemorySegment block = MemorySegment.NULL;
        long key = 0L;
        boolean registered = false;
        try {
            if (completion != null) {
                block = Blocks.blockWithStub(voidStub());
                key = block.address();
                VOID_HANDLERS.put(key, completion);
                registered = true;
            }
            handles.hVoidIdId().invokeExact(peer, Sels.setCookie_completionHandler,
                    cookie.peer(), (MemorySegment) block);
        } catch (Throwable t) {
            if (registered) VOID_HANDLERS.remove(key);
            throw new RuntimeException("setCookie:completionHandler: failed", t);
        }
    }

    /// `[store deleteCookie:completionHandler:]` — delete one cookie.
    public void deleteCookie(NSHTTPCookie cookie, Runnable completion) {
        ensureInit();
        if (cookie == null) throw new IllegalArgumentException("deleteCookie:completionHandler: requires a non-null cookie");
        MemorySegment block = MemorySegment.NULL;
        long key = 0L;
        boolean registered = false;
        try {
            if (completion != null) {
                block = Blocks.blockWithStub(voidStub());
                key = block.address();
                VOID_HANDLERS.put(key, completion);
                registered = true;
            }
            handles.hVoidIdId().invokeExact(peer, Sels.deleteCookie_completionHandler,
                    cookie.peer(), (MemorySegment) block);
        } catch (Throwable t) {
            if (registered) VOID_HANDLERS.remove(key);
            throw new RuntimeException("deleteCookie:completionHandler: failed", t);
        }
    }

    /// `[store setCookies:completionHandler:]` — store several cookies at once
    /// — macOS 26.0+; no-op when absent.
    public void setCookies(NSArray cookies, Runnable completion) {
        ensureInit();
        if (cookies == null) throw new IllegalArgumentException("setCookies:completionHandler: requires non-null cookies");
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.setCookies_completionHandler);
            if (!responds) return;
            MemorySegment block = MemorySegment.NULL;
            long key = 0L;
            boolean registered = false;
            try {
                if (completion != null) {
                    block = Blocks.blockWithStub(voidStub());
                    key = block.address();
                    VOID_HANDLERS.put(key, completion);
                    registered = true;
                }
                handles.hVoidIdId().invokeExact(peer, Sels.setCookies_completionHandler,
                        cookies.peer(), (MemorySegment) block);
            } catch (Throwable t) {
                if (registered) VOID_HANDLERS.remove(key);
                throw new RuntimeException("setCookies:completionHandler: failed", t);
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("setCookies:completionHandler: failed", t);
        }
    }

    /// `[store addObserver:]` — the observer is weakly held; remove it before
    /// it becomes invalid.
    public void addObserver(MemorySegment observer) {
        ensureInit();
        Bounds.nonNull(observer, "addObserver:");
        try {
            handles.hVoidId().invokeExact(peer, Sels.addObserver, (MemorySegment) observer);
        } catch (Throwable t) {
            throw new RuntimeException("addObserver: failed", t);
        }
    }

    /// `[store removeObserver:]`.
    public void removeObserver(MemorySegment observer) {
        ensureInit();
        Bounds.nonNull(observer, "removeObserver:");
        try {
            handles.hVoidId().invokeExact(peer, Sels.removeObserver, (MemorySegment) observer);
        } catch (Throwable t) {
            throw new RuntimeException("removeObserver: failed", t);
        }
    }

    /// `[store setCookiePolicy:completionHandler:]` — macOS 14.0+; no-op when absent.
    ///
    /// Policy values come from `WKHTTPCookieStore.h`: 0 allow, 1 disallow.
    public void setCookiePolicy(long policy, Runnable completion) {
        ensureInit();
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.setCookiePolicy_completionHandler);
            if (!responds) return;
            MemorySegment block = MemorySegment.NULL;
            long key = 0L;
            boolean registered = false;
            try {
                if (completion != null) {
                    block = Blocks.blockWithStub(voidStub());
                    key = block.address();
                    VOID_HANDLERS.put(key, completion);
                    registered = true;
                }
                handles.hVoidIntId().invokeExact(peer, Sels.setCookiePolicy_completionHandler,
                        policy, (MemorySegment) block);
            } catch (Throwable t) {
                if (registered) VOID_HANDLERS.remove(key);
                throw new RuntimeException("setCookiePolicy:completionHandler: failed", t);
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("setCookiePolicy:completionHandler: failed", t);
        }
    }

    /// `[store getCookiePolicy:]` — the current cookie policy via callback
    /// — macOS 14.0+; never calls back when absent.
    public void getCookiePolicy(Consumer<Long> completion) {
        ensureInit();
        if (completion == null) throw new IllegalArgumentException("getCookiePolicy: requires a non-null completion");
        try {
            boolean responds = (boolean) handles.hResponds().invokeExact(
                    peer, Sels.respondsToSelector, Sels.getCookiePolicy);
            if (!responds) return;
            MemorySegment block = Blocks.blockWithStub(policyStub());
            long key = block.address();
            POLICY_HANDLERS.put(key, completion);
            try {
                handles.hVoidId().invokeExact(peer, Sels.getCookiePolicy, (MemorySegment) block);
            } catch (Throwable t) {
                POLICY_HANDLERS.remove(key);
                throw new RuntimeException("getCookiePolicy: failed", t);
            }
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("getCookiePolicy: failed", t);
        }
    }

    // ------------------------------------------------------------ block plumbing

    private static MemorySegment cookieArrayStub() {
        MemorySegment st = cookieArrayStub;
        if (st != null) return st;
        synchronized (WKHTTPCookieStore.class) {
            if (cookieArrayStub == null) {
                try {
                    MethodHandle target = MethodHandles.lookup().findStatic(WKHTTPCookieStore.class,
                            "cookieArrayThunk",
                            MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class));
                    cookieArrayStub = Blocks.stub(target, NsuiForeign.wkCookieArrayUpcall());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("cannot resolve cookie array block target", e);
                }
            }
            return cookieArrayStub;
        }
    }

    private static MemorySegment voidStub() {
        MemorySegment st = voidStub;
        if (st != null) return st;
        synchronized (WKHTTPCookieStore.class) {
            if (voidStub == null) {
                voidStub = Blocks.stub(unusedVoidTarget(), NsuiForeign.blockVoidUpcall());
            }
            return voidStub;
        }
    }

    private static MemorySegment policyStub() {
        MemorySegment st = policyStub;
        if (st != null) return st;
        synchronized (WKHTTPCookieStore.class) {
            if (policyStub == null) {
                try {
                    MethodHandle target = MethodHandles.lookup().findStatic(WKHTTPCookieStore.class,
                            "policyThunk",
                            MethodType.methodType(void.class, MemorySegment.class, long.class));
                    policyStub = Blocks.stub(target, NsuiForeign.sheetBlockUpcall());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("cannot resolve cookie policy block target", e);
                }
            }
            return policyStub;
        }
    }

    private static MethodHandle unusedVoidTarget() {
        try {
            return MethodHandles.lookup().findStatic(WKHTTPCookieStore.class,
                    "voidThunk", MethodType.methodType(void.class, MemorySegment.class));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot resolve void block target", e);
        }
    }

    /// Block body for `getAllCookies:` — routes via block address.
    ///
    /// CONTRACT: runs under the FFM upcall stub; throwing aborts the VM. Catch
    /// errors inside the completion instead.
    ///
    /// The array and its cookies are autoreleased: each cookie is retained
    /// (`keepAlive`) before delivery so the list stays valid across the
    /// caller's run-loop pumps (the toolkit's arena contract would otherwise
    /// free them on the next pool drain). The retain is immortal by design,
    /// like every `keepAlive` — cookie lists are small and short-lived.
    public static void cookieArrayThunk(MemorySegment blockSelf, MemorySegment cookies) {
        if (blockSelf == null || blockSelf.address() == 0) return;
        Consumer<List<NSHTTPCookie>> body = COOKIE_HANDLERS.remove(blockSelf.address());
        if (body == null) return;
        List<NSHTTPCookie> out = new ArrayList<>();
        try {
            if (cookies != null && cookies.address() != 0) {
                NSArray arr = NSArray.wrap(cookies);
                if (arr != null) {
                    long n = arr.count();
                    for (long i = 0; i < n; i++) {
                        MemorySegment seg = arr.objectAtIndex(i);
                        NSHTTPCookie c = NSHTTPCookie.wrap(seg);
                        if (c != null) out.add(c.keepAlive());
                    }
                }
            }
        } catch (Throwable t) {
            // Deliver what parsed; a malformed entry must not drop the callback.
        }
        body.accept(Collections.unmodifiableList(out));
    }

    /// Block body for `setCookie:`/`deleteCookie:` void completions.
    public static void voidThunk(MemorySegment blockSelf) {
        if (blockSelf == null || blockSelf.address() == 0) return;
        Runnable body = VOID_HANDLERS.remove(blockSelf.address());
        if (body != null) body.run();
    }

    /// Block body for `getCookiePolicy:` — delivers the NSInteger policy.
    public static void policyThunk(MemorySegment blockSelf, long policy) {
        if (blockSelf == null || blockSelf.address() == 0) return;
        Consumer<Long> body = POLICY_HANDLERS.remove(blockSelf.address());
        if (body != null) body.accept(policy);
    }
}
