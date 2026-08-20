package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/**
 * NSDraggingSession — minimal wrapper over native {@code NSDraggingSession}.
 */
public final class NSDraggingSession extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hDraggingPasteboard; // (id, SEL) -> id
    private static MethodHandle hSourceOperationMask; // (id, SEL) -> long
    private static MethodHandle hDraggingLocation;   // (id, SEL) -> point (if available)
    private static MethodHandle hDraggingSequenceNumber; // (id, SEL) -> long

    private NSDraggingSession(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSDraggingSession wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDraggingSession(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hDraggingPasteboard = ObjC.handle(Sig.of(Ret.ID));
        hSourceOperationMask = ObjC.handle(Sig.of(Ret.INT));
        // point/size handles may not exist but we try to cache point getter
        try { hDraggingLocation = ObjC.handle(Sig.of(Ret.POINT)); } catch (Exception ignored) { hDraggingLocation = null; }
        hDraggingSequenceNumber = ObjC.handle(Sig.of(Ret.INT));
        initialized = true;
    }

    /** draggingPasteboard */
    public NSPasteboard draggingPasteboard() {
        ensureInit();
        try {
            MemorySegment pb = (MemorySegment) hDraggingPasteboard.invokeExact(peer, ObjC.sel("draggingPasteboard"));
            return NSPasteboard.wrap(pb);
        } catch (Throwable t) { throw new RuntimeException("draggingPasteboard failed", t); }
    }

    /** draggingSequenceNumber */
    public long draggingSequenceNumber() {
        ensureInit();
        try { return (long) hDraggingSequenceNumber.invokeExact(peer, ObjC.sel("draggingSequenceNumber")); }
        catch (Throwable t) { throw new RuntimeException("draggingSequenceNumber failed", t); }
    }

    /** sourceOperationMask */
    public long sourceOperationMask() {
        ensureInit();
        try { return (long) hSourceOperationMask.invokeExact(peer, ObjC.sel("sourceOperationMask")); }
        catch (Throwable t) { throw new RuntimeException("sourceOperationMask failed", t); }
    }

    /** draggingLocation — location in screen coordinates (if available). */
    public NSPoint draggingLocation() {
        ensureInit();
        if (hDraggingLocation == null) return NSPoint.ZERO;
        try {
            MemorySegment seg = (MemorySegment) hDraggingLocation.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("draggingLocation"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { return NSPoint.ZERO; }
    }

    /** draggingFormation — minimal. */
    public long draggingFormation() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("draggingFormation"));
        } catch (Throwable t) { return 0; }
    }

    /** animatesToStartingPositionsOnCancelOrFail */
    public boolean animatesToStartingPositionsOnCancelOrFail() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL));
            return (boolean) h.invokeExact(peer, ObjC.sel("animatesToStartingPositionsOnCancelOrFail"));
        } catch (Throwable t) { return true; }
    }
}
