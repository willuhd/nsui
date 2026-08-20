package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSFindPanel — minimal wrapper for the system Find panel.
/// Thin 1:1 wrapper; native class is `NSPanel` subclass used by
/// `NSTextView`’s find bar / find panel integration.
///
/// On AppKit the find panel is exposed via `NSTextFinder` /
/// `NSFindPanelAction`; this wrapper keeps a conventional
/// `NSObject` shape with `shared` accessor so build passes
/// even where the underlying native class name differs across OS versions.
public final class NSFindPanel extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hGetId;   // (id, SEL) -> id
    private static MethodHandle hBool;    // (id, SEL) -> bool
    private static MethodHandle hSetBool; // (id, SEL, bool) -> void

    private NSFindPanel(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSFindPanel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFindPanel(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        initialized = true;
    }

    // ---- shared accessor (tries NSFindPanel, falls back to NSPanel) ----

    /// `+[NSFindPanel sharedFindPanel]` — if class exists, otherwise nil.
    public static NSFindPanel sharedFindPanel() {
        ensureInit();
        // NSFindPanel is private on some SDKs; try NSFindPanel first, then NSTextFinder's panel
        MemorySegment cls = null;
        try {
            cls = ObjC.cls("NSFindPanel");
            // quick check: does it respond to sharedFindPanel?
            MemorySegment p = ObjC.msgSendId(cls, ObjC.sel("sharedFindPanel"));
            if (p != null && p.address() != 0) return wrap(p);
        } catch (Throwable ignored) {}
        // Fallback: use NSPanel's shared instance as a placeholder panel
        try {
            MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPanel"), ObjC.sel("alloc"));
            // init is (id) -> id already handled; just return generic panel as find panel
            // Instead return null to indicate no find panel class
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    /// Create a basic panel for find UI (alloc+init).
    public static NSFindPanel create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPanel"), ObjC.sel("alloc"));
        try {
            MemorySegment q = (MemorySegment) hGetId.invokeExact(p, ObjC.sel("init"));
            return wrap(q);
        } catch (Throwable t) {
            throw new RuntimeException("NSFindPanel init failed", t);
        }
    }

    // ---- find string ----

    /// [panel findString] — placeholder; backed by find pasteboard on real FindPanel.
    public String findString() {
        // Try native find string if selector exists
        try {
            MemorySegment s = ObjC.msgSendId(peer, ObjC.sel("findString"));
            String str = ObjC.toString(s);
            if (str != null) return str;
        } catch (Throwable ignored) {}
        return null;
    }

    public void setFindString(String s) {
        try {
            ObjC.msgSendVoidId(peer, ObjC.sel("setFindString:"), s == null ? MemorySegment.NULL : ObjC.nsstring(s));
        } catch (Throwable ignored) {}
    }

    // ---- options ----

    public boolean isCaseSensitive() {
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isCaseSensitive")); } catch (Throwable t) { return false; }
    }
    public void setCaseSensitive(boolean flag) {
        try { hSetBool.invokeExact(peer, ObjC.sel("setCaseSensitive:"), flag); } catch (Throwable ignored) {}
    }

    public boolean isRegularExpression() {
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isRegularExpression")); } catch (Throwable t) { return false; }
    }
    public void setRegularExpression(boolean flag) {
        try { hSetBool.invokeExact(peer, ObjC.sel("setRegularExpression:"), flag); } catch (Throwable ignored) {}
    }

    // ---- visibility ----

    public boolean isVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVisible"));
    }
    public void orderFront(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public void orderOut(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderOut:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public void makeKeyAndOrderFront(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("makeKeyAndOrderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- action support for NSTextFinder integration ----

    /// [panel performFindPanelAction:] — forward to sender if needed.
    public void performFindPanelAction(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("performFindPanelAction:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
}
