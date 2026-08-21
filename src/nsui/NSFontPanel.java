package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSFontPanel — the system Font panel.
/// Thin 1:1 wrapper over native `NSFontPanel` (an NSPanel subclass).
public final class NSFontPanel extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hGetId;   // (id, SEL) -> id
    private static MethodHandle hBool;    // (id, SEL) -> bool
    private static MethodHandle hSetBool; // (id, SEL, bool) -> void
    private static MethodHandle hSetFont; // (id, SEL, id, bool) -> void

    private NSFontPanel(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSFontPanel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFontPanel(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hSetFont = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.BOOL));
        initialized = true;
    }

    // ---- shared ----

    /// `+[NSFontPanel sharedFontPanel]`
    public static NSFontPanel sharedFontPanel() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSFontPanel"), ObjC.sel("sharedFontPanel"));
        return wrap(p);
    }

    /// `+[NSFontPanel sharedFontPanelExists]`
    public static boolean sharedFontPanelExists() {
        ensureInit();
        try {
            return (boolean) hBool.invokeExact(ObjC.cls("NSFontPanel"), ObjC.sel("sharedFontPanelExists"));
        } catch (Throwable t) {
            throw new RuntimeException("sharedFontPanelExists failed", t);
        }
    }

    // ---- font ----

    /// [panel panelConvertFont:] -> NSFont
    public NSFont panelConvertFont(NSFont font) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("panelConvertFont:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
            return NSFont.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("panelConvertFont: failed", t);
        }
    }

    /// [panel setPanelFont:isMultiple:]
    public void setPanelFont(NSFont font, boolean isMultiple) {
        ensureInit();
        try {
            hSetFont.invokeExact(peer, ObjC.sel("setPanelFont:isMultiple:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()), isMultiple);
        } catch (Throwable t) {
            throw new RuntimeException("setPanelFont:isMultiple: failed", t);
        }
    }

    /// [panel isEnabled]
    public boolean isEnabled() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isEnabled")); } catch (Throwable t) { throw new RuntimeException("isEnabled failed", t); }
    }
    public void setEnabled(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setEnabled:"), flag); } catch (Throwable t) { throw new RuntimeException("setEnabled: failed", t); }
    }

    // ---- panel behavior ----

    /// [panel isVisible]
    public boolean isVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVisible"));
    }

    /// [panel orderFront:]
    public void orderFront(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [panel orderOut:]
    public void orderOut(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderOut:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [panel makeKeyAndOrderFront:]
    public void makeKeyAndOrderFront(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("makeKeyAndOrderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [panel accessoryView]
    public NSView accessoryView() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("accessoryView"));
            return NSView.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("accessoryView failed", t);
        }
    }
    public void setAccessoryView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAccessoryView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
    }

    // ---- working with NSFontManager ----

    /// [panel worksWhenModal]
    public boolean worksWhenModal() {
        return ObjC.msgSendBool(peer, ObjC.sel("worksWhenModal"));
    }

    /// [panel setWorksWhenModal:]
    public void setWorksWhenModal(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setWorksWhenModal:"), flag);
    }
}
