package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSCursor — the mouse cursor: standard cursors plus push/pop/set.
/// Thin stateless wrapper: each method maps to one objc_msgSend selector.
/// OMITTED: initWithImage:hotSpot: — its (ID,ID,POINT) shape is NOT in the
/// Sig vocabulary (verified by grep); initWithImage:foregroundColorHint:… +
/// mouseEntered:/mouseExited:/setOnMouseExited: (deprecated no-ops per the
/// header) and currentSystemCursor/resize*Cursor (deprecated) likewise.
public final class NSCursor extends NSObject {

            private record Handles(MethodHandle hCursor, MethodHandle hVoid, MethodHandle hBool) {}
    private static volatile Handles handles;

    private NSCursor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSCursor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSCursor(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.VOID)), ObjC.handle(Sig.of(Ret.BOOL)));
    }

    private static NSCursor cursorWithSel(String sel) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hCursor().invokeExact(ObjC.cls("NSCursor"), ObjC.sel(sel));
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
    public static NSCursor operationNotAllowedCursor() { return cursorWithSel("operationNotAllowedCursor"); }
    public static NSCursor dragLinkCursor() { return cursorWithSel("dragLinkCursor"); }
    public static NSCursor dragCopyCursor() { return cursorWithSel("dragCopyCursor"); }
    public static NSCursor contextualMenuCursor() { return cursorWithSel("contextualMenuCursor"); }
    public static NSCursor IBeamCursorForVerticalLayout() { return cursorWithSel("IBeamCursorForVerticalLayout"); }
    public static NSCursor zoomInCursor() { return cursorWithSel("zoomInCursor"); }
    public static NSCursor zoomOutCursor() { return cursorWithSel("zoomOutCursor"); }
    public static NSCursor columnResizeCursor() { return cursorWithSel("columnResizeCursor"); }
    public static NSCursor rowResizeCursor() { return cursorWithSel("rowResizeCursor"); }

    /// +columnResizeCursorInDirections: — resize cursor for the given horizontal directions.
    public static NSCursor columnResizeCursorInDirections(long directions) {
        ensureInit();
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment p = (MemorySegment) h.invokeExact(ObjC.cls("NSCursor"),
                    ObjC.sel("columnResizeCursorInDirections:"), directions);
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("columnResizeCursorInDirections: failed", t);
        }
    }

    /// +rowResizeCursorInDirections: — resize cursor for the given vertical directions.
    public static NSCursor rowResizeCursorInDirections(long directions) {
        ensureInit();
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment p = (MemorySegment) h.invokeExact(ObjC.cls("NSCursor"),
                    ObjC.sel("rowResizeCursorInDirections:"), directions);
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("rowResizeCursorInDirections: failed", t);
        }
    }

    /// +frameResizeCursorFromPosition:inDirections: — frame-resize cursor.
    public static NSCursor frameResizeCursorFromPositionInDirections(long position, long directions) {
        ensureInit();
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.INT));
            MemorySegment p = (MemorySegment) h.invokeExact(ObjC.cls("NSCursor"),
                    ObjC.sel("frameResizeCursorFromPosition:inDirections:"), position, directions);
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("frameResizeCursorFromPosition:inDirections: failed", t);
        }
    }

    /// +hide — hide the cursor (global side effect; tests must NOT call this).
    public static void hide() {
        ensureInit();
        ObjC.msgSendVoid(ObjC.cls("NSCursor"), ObjC.sel("hide"));
    }

    /// +unhide — unhide the cursor (global side effect; tests must NOT call this).
    public static void unhide() {
        ensureInit();
        ObjC.msgSendVoid(ObjC.cls("NSCursor"), ObjC.sel("unhide"));
    }

    /// +setHiddenUntilMouseMoves: — hide until the mouse moves (global side effect).
    public static void setHiddenUntilMouseMoves(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(ObjC.cls("NSCursor"), ObjC.sel("setHiddenUntilMouseMoves:"), flag);
    }

    /// +[NSCursor currentCursor]
    public static NSCursor currentCursor() {
        return cursorWithSel("currentCursor");
    }

    /// -set — make this the current cursor
    public void set() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, ObjC.sel("set"));
        } catch (Throwable t) {
            throw new RuntimeException("set failed", t);
        }
    }

    /// -push
    public void push() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, ObjC.sel("push"));
        } catch (Throwable t) {
            throw new RuntimeException("push failed", t);
        }
    }

    /// -pop — class method actually, but also instance pop
    public void pop() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, ObjC.sel("pop"));
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
            return (boolean) handles.hBool().invokeExact(peer, ObjC.sel("isSetOnMouseEntered"));
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
            MemorySegment img = (MemorySegment) handles.hCursor().invokeExact(peer, ObjC.sel("image"));
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
            MemorySegment pt = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("hotSpot"));
            return NSPoint.fromSegment(pt);
        } catch (Throwable t) {
            throw new RuntimeException("hotSpot failed", t);
        }
    }
}
