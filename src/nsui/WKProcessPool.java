package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// WKProcessPool — a pool of web content processes shared by web views.
///
/// Thin wrapper: the class carries no properties of its own. Web views that share
/// a pool may share processes; a fresh pool per configuration isolates them.
/// Every method maps to one `objc_msgSend` selector. macOS 10.10+.
///
/// Note: creating and using multiple pools no longer has any effect on process
/// sharing in current releases (the pool is effectively shared), but the class
/// itself remains usable — `create()` still returns a live pool.
public class WKProcessPool extends NSObject {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment alloc;
        static MemorySegment init;
        static void populate() {
            alloc = ObjC.sel("alloc");
            init = ObjC.sel("init");
        }
    }

    private record Handles(MethodHandle hId) {}
    private static volatile Handles handles;

    protected WKProcessPool(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an id as WKProcessPool (null for nil).
    public static WKProcessPool wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new WKProcessPool(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        // WKProcessPool lives in WebKit.framework; ObjC.ensureFramework loads it lazily.
        try { ObjC.ensureFramework("WebKit"); } catch (Throwable ignored) {}
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.ID)));
        Sels.populate();
        handles = h;
    }

    /// `[[WKProcessPool alloc] init]` — macOS 10.10+.
    public static WKProcessPool create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("WKProcessPool"), Sels.alloc);
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(alloc, Sels.init);
            if (p.address() == 0) throw new IllegalStateException("WKProcessPool init returned nil");
            return new WKProcessPool(p);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("WKProcessPool init failed", t);
        }
    }
}
