package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSColorPanel — the system Color panel.
 * Thin 1:1 wrapper over native {@code NSColorPanel} (an NSPanel subclass).
 */
public final class NSColorPanel extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hGetId;   // (id, SEL) -> id
    private static MethodHandle hGetBool; // (id, SEL) -> bool
    private static MethodHandle hSetBool; // (id, SEL, bool) -> void
    private static MethodHandle hGetInt;  // (id, SEL) -> long
    private static MethodHandle hSetInt;  // (id, SEL, long) -> void

    private NSColorPanel(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSColorPanel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSColorPanel(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hGetBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hGetInt = ObjC.handle(Sig.of(Ret.INT));
        hSetInt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        initialized = true;
    }

    // ---- shared ----

    /** {@code +[NSColorPanel sharedColorPanel]} */
    public static NSColorPanel sharedColorPanel() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSColorPanel"), ObjC.sel("sharedColorPanel"));
        return wrap(p);
    }

    /** {@code +[NSColorPanel sharedColorPanelExists]} */
    public static boolean sharedColorPanelExists() {
        ensureInit();
        try {
            return (boolean) hGetBool.invokeExact(ObjC.cls("NSColorPanel"), ObjC.sel("sharedColorPanelExists"));
        } catch (Throwable t) {
            throw new RuntimeException("sharedColorPanelExists failed", t);
        }
    }

    // ---- color ----

    /** [panel color] -> NSColor */
    public NSColor color() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("color"));
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("color failed", t);
        }
    }

    /** [panel setColor:] */
    public void setColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /** [panel showsAlpha] */
    public boolean showsAlpha() {
        ensureInit();
        try { return (boolean) hGetBool.invokeExact(peer, ObjC.sel("showsAlpha")); } catch (Throwable t) { throw new RuntimeException("showsAlpha failed", t); }
    }
    public void setShowsAlpha(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setShowsAlpha:"), flag); } catch (Throwable t) { throw new RuntimeException("setShowsAlpha: failed", t); }
    }

    /** [panel isContinuous] */
    public boolean isContinuous() {
        ensureInit();
        try { return (boolean) hGetBool.invokeExact(peer, ObjC.sel("isContinuous")); } catch (Throwable t) { throw new RuntimeException("isContinuous failed", t); }
    }
    public void setContinuous(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setContinuous:"), flag); } catch (Throwable t) { throw new RuntimeException("setContinuous: failed", t); }
    }

    // ---- mode ----

    /** [panel mode] -> long (NSColorPanelMode) */
    public long mode() {
        ensureInit();
        try { return (long) hGetInt.invokeExact(peer, ObjC.sel("mode")); } catch (Throwable t) { throw new RuntimeException("mode failed", t); }
    }
    public void setMode(long mode) {
        ensureInit();
        try { hSetInt.invokeExact(peer, ObjC.sel("setMode:"), mode); } catch (Throwable t) { throw new RuntimeException("setMode: failed", t); }
    }

    // ---- visibility ----

    /** [panel isVisible] */
    public boolean isVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVisible"));
    }

    /** [panel orderFront:] */
    public void orderFront(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /** [panel orderOut:] */
    public void orderOut(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderOut:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /** [panel setAction:] / setTarget: — color well target wiring */
    public void setTarget(MemorySegment target) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTarget:"), (MemorySegment) (target == null ? MemorySegment.NULL : target));
    }
    public void setAction(String action) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"), ObjC.sel(action));
    }

    /** [panel accessoryView] */
    public NSView accessoryView() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("accessoryView"));
            return NSView.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("accessoryView failed", t); }
    }
    public void setAccessoryView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAccessoryView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
    }

    /** [panel attachColorList:] */
    public void attachColorList(MemorySegment colorList) {
        ObjC.msgSendVoidId(peer, ObjC.sel("attachColorList:"), (MemorySegment) (colorList == null ? MemorySegment.NULL : colorList));
    }

    /** [panel detachColorList:] */
    public void detachColorList(MemorySegment colorList) {
        ObjC.msgSendVoidId(peer, ObjC.sel("detachColorList:"), (MemorySegment) (colorList == null ? MemorySegment.NULL : colorList));
    }
}
