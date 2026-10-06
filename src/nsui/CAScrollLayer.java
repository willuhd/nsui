package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAScrollLayer — scrollable layer plane with programmatic scroll verbs.
public class CAScrollLayer extends CALayer {
    private record Handles(MethodHandle hScrollPoint, MethodHandle hScrollRect) {}
    private static volatile Handles handles;

    protected CAScrollLayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAScrollLayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAScrollLayer(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)));
    }

    /// [[CAScrollLayer alloc] init].
    public static CAScrollLayer create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("CAScrollLayer"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for CAScrollLayer");
        return new CAScrollLayer(p);
    }

    /// scrollToPoint:.
    public void scrollToPoint(NSPoint p) {
        try {
            handles.hScrollPoint().invokeExact(peer, ObjC.sel("scrollToPoint:"), p.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("scrollToPoint: failed", t);
        }
    }

    /// scrollToRect:.
    public void scrollToRect(NSRect r) {
        try {
            handles.hScrollRect().invokeExact(peer, ObjC.sel("scrollToRect:"), r.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("scrollToRect: failed", t);
        }
    }

    /// scrollMode (nil-safe).
    public String scrollMode() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("scrollMode")));
    }

    /// setScrollMode:.
    public void setScrollMode(String mode) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setScrollMode:"), ObjC.nsstring(mode));
    }
}
