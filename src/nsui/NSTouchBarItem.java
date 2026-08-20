package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTouchBarItem — minimal wrap over AppKit NSTouchBarItem.
/// Thin 1:1, stateless.
public class NSTouchBarItem extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hInitId;   // (id, SEL, id) -> id
    private static MethodHandle hId;       // (id, SEL) -> id
    private static MethodHandle hVoidId;   // (id, SEL, id) -> void
    private static MethodHandle hBool;     // (id, SEL) -> bool
    private static MethodHandle hVoidBool; // (id, SEL, bool) -> void

    protected NSTouchBarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTouchBarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTouchBarItem(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitId = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hId = ObjC.handle(Sig.of(Ret.ID));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hVoidBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        initialized = true;
    }

    /// alloc + initWithIdentifier: — create item with identifier.
    public static NSTouchBarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSTouchBarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitId.invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSTouchBarItem", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSTouchBarItem alloc/initWithIdentifier: returned nil");
        return new NSTouchBarItem(p);
    }

    /// identifier — NSString.
    public String identifier() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("identifier"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("identifier failed", t);
        }
    }

    /// visibilityPriority — long.
    public long visibilityPriority() {
        return ObjC.msgSendLong(peer, ObjC.sel("visibilityPriority"));
    }

    public void setVisibilityPriority(long p) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setVisibilityPriority:"), p);
    }

    /// isVisible — guarded; returns false if selector absent (not all items expose it).
    public boolean isVisible() {
        ensureInit();
        // Guard: not all NSTouchBarItem subclasses respond to isVisible
        try {
            MemorySegment sel = ObjC.sel("isVisible");
            // quick respondsTo check via ObjC
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return false;
            return (boolean) hBool.invokeExact(peer, sel);
        } catch (Throwable t) { return false; }
    }

    public void setVisible(boolean flag) {
        ensureInit();
        try {
            MemorySegment sel = ObjC.sel("setVisible:");
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return;
            hVoidBool.invokeExact(peer, sel, flag);
        } catch (Throwable t) { /* no-op if absent */ }
    }

    /// view — NSView peer or null (guarded; not all items expose view).
    public NSView view() {
        ensureInit();
        try {
            MemorySegment sel = ObjC.sel("view");
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return null;
            MemorySegment v = (MemorySegment) hId.invokeExact(peer, sel);
            return NSView.wrap(v);
        } catch (Throwable t) { return null; }
    }

    public void setView(NSView view) {
        ensureInit();
        try {
            MemorySegment sel = ObjC.sel("setView:");
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return;
            hVoidId.invokeExact(peer, sel, (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) { /* no-op if absent */ }
    }
}
