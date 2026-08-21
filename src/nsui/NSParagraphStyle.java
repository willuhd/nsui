package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSParagraphStyle — immutable paragraph style.
/// Thin 1:1 wrapper over native `NSParagraphStyle`: every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer.
/// Follows FFM pattern: no reflection, cached handles, ensureInit.
public class NSParagraphStyle extends NSObject {

            private record Handles(MethodHandle hGetLong) {}
    private static volatile Handles handles;

    protected NSParagraphStyle(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSParagraphStyle wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSParagraphStyle(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.INT)));
    }

    /// [NSParagraphStyle defaultParagraphStyle]
    public static NSParagraphStyle defaultParagraphStyle() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSParagraphStyle"), ObjC.sel("defaultParagraphStyle"));
        return wrap(p);
    }

    /// [style alignment] -> NSTextAlignment (long)
    public long alignment() {
        ensureInit();
        try {
            return (long) handles.hGetLong().invokeExact(peer, ObjC.sel("alignment"));
        } catch (Throwable t) {
            throw new RuntimeException("alignment failed", t);
        }
    }

    /// [style lineBreakMode] -> NSLineBreakMode (long)
    public long lineBreakMode() {
        ensureInit();
        try {
            return (long) handles.hGetLong().invokeExact(peer, ObjC.sel("lineBreakMode"));
        } catch (Throwable t) {
            throw new RuntimeException("lineBreakMode failed", t);
        }
    }

    /// [style mutableCopy] -> NSMutableParagraphStyle
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
