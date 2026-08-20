package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// NSCursor — minimal wrapper over AppKit NSCursor.
/// Provides standard cursors and push/pop/set.
public final class NSCursor extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hCursor; // (id, SEL) -> id  class cursor getters
    private static MethodHandle hVoid;   // (id, SEL) -> void [set/push/pop]
    private static MethodHandle hBool;   // (id, SEL) -> bool

    private NSCursor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSCursor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSCursor(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hCursor = ObjC.handle(Sig.of(Ret.ID));
        hVoid = ObjC.handle(Sig.of(Ret.VOID));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        initialized = true;
    }

    private static NSCursor cursorWithSel(String sel) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hCursor.invokeExact(ObjC.cls("NSCursor"), ObjC.sel(sel));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException(sel + " failed", t);
        }
    }

    public static NSCursor arrowCursor() { return cursorWithSel("arrowCursor"); }
    public static NSCursor IBeamCursor() { return cursorWithSel("IBeamCursor"); }
    public static NSCursor crosshairCursor() { return cursorWithSel("crosshairCursor"); }
    public static NSCursor closedHandCursor() { return cursorWithSel("closedHandCursor"); }
    public static NSCursor openHandCursor() { return cursorWithSel("openHandCursor"); }
    public static NSCursor pointingHandCursor() { return cursorWithSel("pointingHandCursor"); }
    public static NSCursor resizeLeftRightCursor() { return cursorWithSel("resizeLeftRightCursor"); }
    public static NSCursor resizeUpDownCursor() { return cursorWithSel("resizeUpDownCursor"); }
    public static NSCursor disappearingItemCursor() { return cursorWithSel("disappearingItemCursor"); }

    /// +[NSCursor currentCursor]
    public static NSCursor currentCursor() {
        return cursorWithSel("currentCursor");
    }

    /// -set — make this the current cursor
    public void set() {
        ensureInit();
        try {
            hVoid.invokeExact(peer, ObjC.sel("set"));
        } catch (Throwable t) {
            throw new RuntimeException("set failed", t);
        }
    }

    /// -push
    public void push() {
        ensureInit();
        try {
            hVoid.invokeExact(peer, ObjC.sel("push"));
        } catch (Throwable t) {
            throw new RuntimeException("push failed", t);
        }
    }

    /// -pop — class method actually, but also instance pop
    public void pop() {
        ensureInit();
        try {
            hVoid.invokeExact(peer, ObjC.sel("pop"));
        } catch (Throwable t) {
            // fallback to class pop
            try {
                MethodHandle h = ObjC.handle(Sig.of(Ret.VOID));
                h.invokeExact(ObjC.cls("NSCursor"), ObjC.sel("pop"));
            } catch (Throwable t2) {
                throw new RuntimeException("pop failed", t2);
            }
        }
    }

    /// +[NSCursor pop] class helper
    public static void popCursor() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID));
            h.invokeExact(ObjC.cls("NSCursor"), ObjC.sel("pop"));
        } catch (Throwable t) {
            throw new RuntimeException("pop failed", t);
        }
    }

    /// -setOnMouseEntered: (bool)
    public boolean isSetOnMouseEntered() {
        ensureInit();
        try {
            return (boolean) hBool.invokeExact(peer, ObjC.sel("isSetOnMouseEntered"));
        } catch (Throwable t) {
            throw new RuntimeException("isSetOnMouseEntered failed", t);
        }
    }

    public void setOnMouseEntered(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, ObjC.sel("setOnMouseEntered:"), flag);
    }

    /// -image -> NSImage
    public NSImage image() {
        ensureInit();
        try {
            MemorySegment img = (MemorySegment) hCursor.invokeExact(peer, ObjC.sel("image"));
            return NSImage.wrap(img);
        } catch (Throwable t) {
            throw new RuntimeException("image failed", t);
        }
    }

    /// -hotSpot -> NSPoint
    public NSPoint hotSpot() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.POINT));
            MemorySegment pt = (MemorySegment) h.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("hotSpot"));
            return NSPoint.fromSegment(pt);
        } catch (Throwable t) {
            throw new RuntimeException("hotSpot failed", t);
        }
    }
}
