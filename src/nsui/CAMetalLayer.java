package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAMetalLayer — a layer whose content is a Metal drawable: set device and
/// pixel format, pull drawables per frame. Lives in QuartzCore (loaded here).
public class CAMetalLayer extends CALayer {

    private record Handles(MethodHandle hGetSize) {}
    private static volatile Handles handles;

    protected CAMetalLayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAMetalLayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAMetalLayer(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(ObjC.handle(Sig.of(Ret.SIZE)));
    }

    /// [[CAMetalLayer alloc] init].
    public static CAMetalLayer create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("CAMetalLayer"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for CAMetalLayer");
        return new CAMetalLayer(p);
    }

    /// setDevice: (nil detaches).
    public void setDevice(MTLDevice device) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDevice:"),
                (MemorySegment) (device == null ? MemorySegment.NULL : device.peer()));
    }

    /// setPixelFormat: — layer officially supports BGRA8Unorm(_sRGB); anything
    /// else belongs offscreen (render-to-texture instead).
    public void setPixelFormat(long format) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setPixelFormat:"), format);
    }

    /// setFramebufferOnly:.
    public void setFramebufferOnly(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setFramebufferOnly:"), flag);
    }

    /// setDrawableSize:.
    public void setDrawableSize(NSSize size) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDrawableSize:"), size.toSegment());
    }

    /// drawableSize.
    public NSSize drawableSize() {
        try {
            return NSSize.fromSegment((MemorySegment) handles.hGetSize().invokeExact(
                    (java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(),
                    peer, ObjC.sel("drawableSize")));
        } catch (Throwable t) {
            throw new RuntimeException("drawableSize failed", t);
        }
    }

    /// nextDrawable — nil when none available this frame (do not spin on it).
    public CAMetalDrawable nextDrawable() {
        return CAMetalDrawable.wrap(ObjC.msgSendId(peer, ObjC.sel("nextDrawable")));
    }

    /// setPresentsWithTransaction:.
    public void setPresentsWithTransaction(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setPresentsWithTransaction:"), flag);
    }
}
