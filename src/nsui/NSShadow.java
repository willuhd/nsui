package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSShadow — minimal wrapper over AppKit NSShadow.
 * Provides offset, blur radius, color, and set.
 */
public final class NSShadow extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hCreate;     // (id, SEL) -> id [alloc init]
    private static MethodHandle hGetDouble;  // (id, SEL) -> double
    private static MethodHandle hSetDouble;  // (id, SEL, double) -> void
    private static MethodHandle hGetId;      // (id, SEL) -> id
    private static MethodHandle hSetId;      // (id, SEL, id) -> void
    private static MethodHandle hGetSize;    // (SegmentAllocator, id, SEL) -> NSSize
    private static MethodHandle hSetSize;    // (id, SEL, NSSize) -> void
    private static MethodHandle hVoid;       // (id, SEL) -> void

    private NSShadow(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSShadow wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSShadow(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hCreate = ObjC.handle(Sig.of(Ret.ID));
        hGetDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hSetId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hGetSize = ObjC.handle(Sig.of(Ret.SIZE));
        hSetSize = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
        hVoid = ObjC.handle(Sig.of(Ret.VOID));
        initialized = true;
    }

    /** [[NSShadow alloc] init] */
    public static NSShadow create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSShadow"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hCreate.invokeExact(p, ObjC.sel("init"));
        } catch (Throwable t) {
            throw new RuntimeException("init failed for NSShadow", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSShadow alloc/init returned nil");
        return new NSShadow(p);
    }

    /** -set */
    public void set() {
        ensureInit();
        try {
            hVoid.invokeExact(peer, ObjC.sel("set"));
        } catch (Throwable t) {
            throw new RuntimeException("set failed", t);
        }
    }

    /** [shadow shadowOffset] -> NSSize */
    public NSSize shadowOffset() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hGetSize.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("shadowOffset"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("shadowOffset failed", t);
        }
    }

    /** [shadow setShadowOffset:] */
    public void setShadowOffset(NSSize offset) {
        ensureInit();
        try {
            hSetSize.invokeExact(peer, ObjC.sel("setShadowOffset:"), offset.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setShadowOffset: failed", t);
        }
    }

    /** [shadow shadowBlurRadius] */
    public double shadowBlurRadius() {
        ensureInit();
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("shadowBlurRadius"));
        } catch (Throwable t) {
            throw new RuntimeException("shadowBlurRadius failed", t);
        }
    }

    /** [shadow setShadowBlurRadius:] */
    public void setShadowBlurRadius(double radius) {
        ensureInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setShadowBlurRadius:"), radius);
        } catch (Throwable t) {
            throw new RuntimeException("setShadowBlurRadius: failed", t);
        }
    }

    /** [shadow shadowColor] -> NSColor */
    public NSColor shadowColor() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("shadowColor"));
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("shadowColor failed", t);
        }
    }

    /** [shadow setShadowColor:] */
    public void setShadowColor(NSColor color) {
        ensureInit();
        try {
            hSetId.invokeExact(peer, ObjC.sel("setShadowColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setShadowColor: failed", t);
        }
    }
}
