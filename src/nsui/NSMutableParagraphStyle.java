package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMutableParagraphStyle — mutable paragraph style.
/// Thin 1:1 wrapper over native `NSMutableParagraphStyle`: every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer.
/// Follows FFM pattern: no reflection, cached handles, ensureInit.
public class NSMutableParagraphStyle extends NSParagraphStyle {

    private static volatile boolean mutableInitialized;
    private static MethodHandle hSetLong;    // (id, SEL, long) -> void
    private static MethodHandle hSetDouble;  // (id, SEL, double) -> void

    protected NSMutableParagraphStyle(MemorySegment peer) {
        super(peer);
        ensureMutInit();
    }

    public static NSMutableParagraphStyle wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMutableParagraphStyle(peer);
    }

    private static synchronized void ensureMutInit() {
        if (mutableInitialized) return;
        hSetLong = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        mutableInitialized = true;
    }

    /// `[[NSMutableParagraphStyle alloc] init]`
    public static NSMutableParagraphStyle create() {
        ensureMutInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSMutableParagraphStyle"), ObjC.sel("alloc"));
        MemorySegment p = ObjC.msgSendId(alloc, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("NSMutableParagraphStyle init returned nil");
        return new NSMutableParagraphStyle(p);
    }

    /// [style setAlignment:] — NSTextAlignment
    public void setAlignment(long alignment) {
        ensureMutInit();
        try {
            hSetLong.invokeExact(peer, ObjC.sel("setAlignment:"), alignment);
        } catch (Throwable t) {
            throw new RuntimeException("setAlignment: failed", t);
        }
    }

    /// [style setLineBreakMode:] — NSLineBreakMode
    public void setLineBreakMode(long mode) {
        ensureMutInit();
        try {
            hSetLong.invokeExact(peer, ObjC.sel("setLineBreakMode:"), mode);
        } catch (Throwable t) {
            throw new RuntimeException("setLineBreakMode: failed", t);
        }
    }

    // Convenience overrides returning mutable type
    @Override
    public long alignment() { return super.alignment(); }
    @Override
    public long lineBreakMode() { return super.lineBreakMode(); }

    /// [style setLineSpacing:]
    public void setLineSpacing(double spacing) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setLineSpacing:"), spacing);
        } catch (Throwable t) {
            throw new RuntimeException("setLineSpacing: failed", t);
        }
    }
}
