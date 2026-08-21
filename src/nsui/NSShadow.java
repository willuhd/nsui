package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSShadow — minimal wrapper over AppKit NSShadow.
/// Provides offset, blur radius, color, and set.
public final class NSShadow extends NSObject {

            private record Handles(MethodHandle hCreate, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hSetId, MethodHandle hGetSize, MethodHandle hSetSize, MethodHandle hVoid) {}
    private static volatile Handles handles;

    private NSShadow(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSShadow wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSShadow(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID))
        );
    }

    /// [[NSShadow alloc] init]
    public static NSShadow create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSShadow"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hCreate().invokeExact(p, ObjC.sel("init"));
        } catch (Throwable t) {
            throw new RuntimeException("init failed for NSShadow", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSShadow alloc/init returned nil");
        return new NSShadow(p);
    }

    /// -set
    public void set() {
        ensureInit();
        try {
            handles.hVoid().invokeExact(peer, ObjC.sel("set"));
        } catch (Throwable t) {
            throw new RuntimeException("set failed", t);
        }
    }

    /// [shadow shadowOffset] -> NSSize
    public NSSize shadowOffset() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetSize().invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("shadowOffset"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("shadowOffset failed", t);
        }
    }

    /// [shadow setShadowOffset:]
    public void setShadowOffset(NSSize offset) {
        ensureInit();
        try {
            handles.hSetSize().invokeExact(peer, ObjC.sel("setShadowOffset:"), offset.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setShadowOffset: failed", t);
        }
    }

    /// [shadow shadowBlurRadius]
    public double shadowBlurRadius() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("shadowBlurRadius"));
        } catch (Throwable t) {
            throw new RuntimeException("shadowBlurRadius failed", t);
        }
    }

    /// [shadow setShadowBlurRadius:]
    public void setShadowBlurRadius(double radius) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setShadowBlurRadius:"), radius);
        } catch (Throwable t) {
            throw new RuntimeException("setShadowBlurRadius: failed", t);
        }
    }

    /// [shadow shadowColor] -> NSColor
    public NSColor shadowColor() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) handles.hCreate().invokeExact(peer, ObjC.sel("shadowColor"));
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("shadowColor failed", t);
        }
    }

    /// [shadow setShadowColor:]
    public void setShadowColor(NSColor color) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setShadowColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setShadowColor: failed", t);
        }
    }
}
