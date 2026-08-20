package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSParagraphStyle — immutable paragraph style.
 * Thin 1:1 wrapper over native {@code NSParagraphStyle}: every method maps to one
 * {@code objc_msgSend} selector, no cached Java state beyond the peer.
 * Follows FFM pattern: no reflection, cached handles, ensureInit.
 */
public class NSParagraphStyle extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hGetLong;   // (id, SEL) -> long

    protected NSParagraphStyle(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSParagraphStyle wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSParagraphStyle(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hGetLong = ObjC.handle(Sig.of(Ret.INT));
        initialized = true;
    }

    /** [NSParagraphStyle defaultParagraphStyle] */
    public static NSParagraphStyle defaultParagraphStyle() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSParagraphStyle"), ObjC.sel("defaultParagraphStyle"));
        return wrap(p);
    }

    /** [style alignment] -> NSTextAlignment (long) */
    public long alignment() {
        ensureInit();
        try {
            return (long) hGetLong.invokeExact(peer, ObjC.sel("alignment"));
        } catch (Throwable t) {
            throw new RuntimeException("alignment failed", t);
        }
    }

    /** [style lineBreakMode] -> NSLineBreakMode (long) */
    public long lineBreakMode() {
        ensureInit();
        try {
            return (long) hGetLong.invokeExact(peer, ObjC.sel("lineBreakMode"));
        } catch (Throwable t) {
            throw new RuntimeException("lineBreakMode failed", t);
        }
    }

    /** [style mutableCopy] -> NSMutableParagraphStyle */
    public NSMutableParagraphStyle mutableCopy() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("mutableCopy"));
            return NSMutableParagraphStyle.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("mutableCopy failed", t);
        }
    }

    // Additional useful getters (completeness, no extra vocabulary needed)
    public long lineSpacing() {
        ensureInit();
        try {
            // Actually lineSpacing is CGFloat (double) — use double handle
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (long) (double) h.invokeExact(peer, ObjC.sel("lineSpacing"));
        } catch (Throwable t) {
            throw new RuntimeException("lineSpacing failed", t);
        }
    }
}
