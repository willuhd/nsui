package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPrintInfo — minimal wrapper over native `NSPrintInfo`.
public final class NSPrintInfo extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hPaperSize; // (id, SEL) -> size
    private static MethodHandle hSetPaperSize; // (id, SEL, size) -> void

    private NSPrintInfo(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSPrintInfo wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPrintInfo(peer);
    }

    /// [NSPrintInfo sharedPrintInfo]
    public static NSPrintInfo sharedPrintInfo() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSPrintInfo"), ObjC.sel("sharedPrintInfo"));
        return wrap(s);
    }

    /// [NSPrintInfo defaultPrintInfo] fallback
    public static NSPrintInfo defaultPrintInfo() {
        ensureInit();
        try {
            MemorySegment s = ObjC.msgSendId(ObjC.cls("NSPrintInfo"), ObjC.sel("defaultPrintInfo"));
            if (s != null && s.address() != 0) return wrap(s);
        } catch (Exception ignored) {}
        return sharedPrintInfo();
    }

    /// [[NSPrintInfo alloc] init]
    public static NSPrintInfo create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSPrintInfo"), ObjC.sel("alloc"));
        MemorySegment peer = ObjC.msgSendId(alloc, ObjC.sel("init"));
        return wrap(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hPaperSize = ObjC.handle(Sig.of(Ret.SIZE));
        hSetPaperSize = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
        initialized = true;
    }

    /// paperSize
    public NSSize paperSize() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) hPaperSize.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("paperSize"));
            return NSSize.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("paperSize failed", t); }
    }

    /// setPaperSize:
    public void setPaperSize(NSSize size) {
        ensureInit();
        if (size == null) return;
        try { hSetPaperSize.invokeExact(peer, ObjC.sel("setPaperSize:"), size.toSegment()); }
        catch (Throwable t) { throw new RuntimeException("setPaperSize: failed", t); }
    }

    /// orientation — 0 portrait, 1 landscape.
    public long orientation() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("orientation"));
        } catch (Throwable t) { throw new RuntimeException("orientation failed", t); }
    }

    /// setOrientation:
    public void setOrientation(long orientation) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("setOrientation:"), orientation);
        } catch (Throwable t) { throw new RuntimeException("setOrientation: failed", t); }
    }

    /// dictionary — underlying printing dictionary.
    public NSDictionary dictionary() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment d = (MemorySegment) h.invokeExact(peer, ObjC.sel("dictionary"));
            return NSDictionary.wrap(d);
        } catch (Throwable t) { throw new RuntimeException("dictionary failed", t); }
    }

    /// jobDisposition — NSString.
    public String jobDisposition() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment s = (MemorySegment) h.invokeExact(peer, ObjC.sel("jobDisposition"));
            return ObjC.toString(s);
        } catch (Throwable t) { return null; }
    }
}
