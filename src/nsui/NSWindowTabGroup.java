package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSWindowTabGroup — minimal wrapper over AppKit NSWindowTabGroup.
/// Thin 1:1, stateless.
public final class NSWindowTabGroup extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hId;       // (id, SEL) -> id
    private static MethodHandle hBool;     // (id, SEL) -> bool
    private static MethodHandle hVoidId;   // (id, SEL, id) -> void
    private static MethodHandle hInt;      // (id, SEL) -> long

    private NSWindowTabGroup(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSWindowTabGroup wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSWindowTabGroup(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hId = ObjC.handle(Sig.of(Ret.ID));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hInt = ObjC.handle(Sig.of(Ret.INT));
        initialized = true;
    }

    /// [group windows] -> NSArray of NSWindow
    public NSArray windows() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) hId.invokeExact(peer, ObjC.sel("windows"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("windows failed", t);
        }
    }

    /// [group selectedWindow] -> NSWindow
    public NSWindow selectedWindow() {
        ensureInit();
        try {
            MemorySegment w = (MemorySegment) hId.invokeExact(peer, ObjC.sel("selectedWindow"));
            return NSWindow.wrap(w);
        } catch (Throwable t) {
            throw new RuntimeException("selectedWindow failed", t);
        }
    }

    /// [group setSelectedWindow:]
    public void setSelectedWindow(NSWindow window) {
        ensureInit();
        try {
            MemorySegment p = (window == null ? MemorySegment.NULL : window.peer());
            hVoidId.invokeExact(peer, ObjC.sel("setSelectedWindow:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("setSelectedWindow: failed", t);
        }
    }

    /// [group addWindow:]
    public void addWindow(NSWindow window) {
        ensureInit();
        try {
            MemorySegment p = (window == null ? MemorySegment.NULL : window.peer());
            hVoidId.invokeExact(peer, ObjC.sel("addWindow:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("addWindow: failed", t);
        }
    }

    /// [group removeWindow:]
    public void removeWindow(NSWindow window) {
        ensureInit();
        try {
            MemorySegment p = (window == null ? MemorySegment.NULL : window.peer());
            hVoidId.invokeExact(peer, ObjC.sel("removeWindow:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("removeWindow: failed", t);
        }
    }

    /// [group isOverviewVisible]
    public boolean isOverviewVisible() {
        ensureInit();
        try {
            return (boolean) hBool.invokeExact(peer, ObjC.sel("isOverviewVisible"));
        } catch (Throwable t) {
            throw new RuntimeException("isOverviewVisible failed", t);
        }
    }

    /// [group setOverviewVisible:]
    public void setOverviewVisible(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, ObjC.sel("setOverviewVisible:"), flag);
    }

    /// [group count] helper via windows count
    public long count() {
        ensureInit();
        try {
            return (long) hInt.invokeExact(peer, ObjC.sel("count"));
        } catch (Throwable t) {
            // fallback via windows array
            NSArray arr = windows();
            return arr == null ? 0 : arr.count();
        }
    }
}
