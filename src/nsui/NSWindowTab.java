package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSWindowTab — minimal wrapper over AppKit NSWindowTab (macOS 10.13+).
 * Thin 1:1, stateless. Tabs belong to an NSWindowTabGroup.
 */
public final class NSWindowTab extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hId;       // (id, SEL) -> id
    private static MethodHandle hBool;     // (id, SEL) -> bool
    private static MethodHandle hVoidId;   // (id, SEL, id) -> void

    private NSWindowTab(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSWindowTab wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSWindowTab(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hId = ObjC.handle(Sig.of(Ret.ID));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /** [tab window] -> NSWindow */
    public NSWindow window() {
        ensureInit();
        try {
            MemorySegment w = (MemorySegment) hId.invokeExact(peer, ObjC.sel("window"));
            return NSWindow.wrap(w);
        } catch (Throwable t) {
            throw new RuntimeException("window failed", t);
        }
    }

    /** [tab title] */
    public String title() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("title"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("title failed", t);
        }
    }

    /** [tab setTitle:] */
    public void setTitle(String title) {
        ensureInit();
        try {
            hVoidId.invokeExact(peer, ObjC.sel("setTitle:"), ObjC.nsstring(title));
        } catch (Throwable t) {
            throw new RuntimeException("setTitle: failed", t);
        }
    }

    /** [tab isVisible] */
    public boolean isVisible() {
        ensureInit();
        try {
            return (boolean) hBool.invokeExact(peer, ObjC.sel("isVisible"));
        } catch (Throwable t) {
            throw new RuntimeException("isVisible failed", t);
        }
    }

    /** [tab tabGroup] -> NSWindowTabGroup */
    public NSWindowTabGroup tabGroup() {
        ensureInit();
        try {
            MemorySegment g = (MemorySegment) hId.invokeExact(peer, ObjC.sel("tabGroup"));
            return NSWindowTabGroup.wrap(g);
        } catch (Throwable t) {
            throw new RuntimeException("tabGroup failed", t);
        }
    }

    /** [tab identifier] -> NSString */
    public String identifier() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("identifier"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("identifier failed", t);
        }
    }

    /** [tab setIdentifier:] */
    public void setIdentifier(String ident) {
        ensureInit();
        try {
            hVoidId.invokeExact(peer, ObjC.sel("setIdentifier:"), ObjC.nsstring(ident));
        } catch (Throwable t) {
            throw new RuntimeException("setIdentifier: failed", t);
        }
    }
}
