package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// NSDraggingSession — minimal wrapper over native `NSDraggingSession`.
public final class NSDraggingSession extends NSObject {

            private record Handles(MethodHandle hDraggingPasteboard, MethodHandle hSourceOperationMask, MethodHandle hDraggingLocation) {}
    private static volatile Handles handles;

    private NSDraggingSession(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSDraggingSession wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDraggingSession(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        // point/size handles may not exist but we try to cache point getter
        MethodHandle tmp_hDraggingLocation = null;
        try { tmp_hDraggingLocation = ObjC.handle(Sig.of(Ret.POINT)); } catch (Exception ignored) { tmp_hDraggingLocation = null; }
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.INT)), tmp_hDraggingLocation);
    }

    /// draggingPasteboard
    public NSPasteboard draggingPasteboard() {
        ensureInit();
        try {
            MemorySegment pb = (MemorySegment) handles.hDraggingPasteboard().invokeExact(peer, ObjC.sel("draggingPasteboard"));
            return NSPasteboard.wrap(pb);
        } catch (Throwable t) { throw new RuntimeException("draggingPasteboard failed", t); }
    }

    /// draggingSequenceNumber
    public long draggingSequenceNumber() {
        ensureInit();
        try { return (long) handles.hSourceOperationMask().invokeExact(peer, ObjC.sel("draggingSequenceNumber")); }
        catch (Throwable t) { throw new RuntimeException("draggingSequenceNumber failed", t); }
    }

    /// sourceOperationMask
    public long sourceOperationMask() {
        ensureInit();
        try { return (long) handles.hSourceOperationMask().invokeExact(peer, ObjC.sel("sourceOperationMask")); }
        catch (Throwable t) { throw new RuntimeException("sourceOperationMask failed", t); }
    }

    /// draggingLocation — location in screen coordinates (if available).
    public NSPoint draggingLocation() {
        ensureInit();
        if (handles.hDraggingLocation() == null) return NSPoint.ZERO;
        try {
            MemorySegment seg = (MemorySegment) handles.hDraggingLocation().invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("draggingLocation"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { return NSPoint.ZERO; }
    }

    /// draggingFormation — minimal.
    public long draggingFormation() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("draggingFormation"));
        } catch (Throwable t) { return 0; }
    }

    /// setDraggingFormation:
    public void setDraggingFormation(long formation) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, nsui.objc.Sig.Arg.INT));
            h.invokeExact(peer, ObjC.sel("setDraggingFormation:"), formation);
        } catch (Throwable t) { throw new RuntimeException("setDraggingFormation: failed", t); }
    }

    /// animatesToStartingPositionsOnCancelOrFail
    public boolean animatesToStartingPositionsOnCancelOrFail() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL));
            return (boolean) h.invokeExact(peer, ObjC.sel("animatesToStartingPositionsOnCancelOrFail"));
        } catch (Throwable t) { return true; }
    }

    /// setAnimatesToStartingPositionsOnCancelOrFail:
    public void setAnimatesToStartingPositionsOnCancelOrFail(boolean flag) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, nsui.objc.Sig.Arg.BOOL));
            h.invokeExact(peer, ObjC.sel("setAnimatesToStartingPositionsOnCancelOrFail:"), flag);
        } catch (Throwable t) { throw new RuntimeException("setAnimatesToStartingPositionsOnCancelOrFail: failed", t); }
    }
}
