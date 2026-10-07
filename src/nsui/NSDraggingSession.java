package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// NSDraggingSession — minimal wrapper over native `NSDraggingSession`.
///
/// Coverage notes (header: NSDraggingSession.h wins on API truth):
/// - Wrapped: draggingPasteboard, draggingSequenceNumber, draggingLocation,
///   draggingFormation (+ setter), animatesToStartingPositionsOnCancelOrFail
///   (+ setter) and draggingLeaderIndex (+ setter).
/// - Omitted: enumerateDraggingItemsWithOptions:… (block-taking method needs
///   upcall machinery), and sourceOperationMask (it lives on the NSDraggingInfo
///   protocol, not on NSDraggingSession, which does not respond to it -- a
///   wrapper would abort the process).
public final class NSDraggingSession extends NSObject {

            private record Handles(MethodHandle hDraggingPasteboard, MethodHandle hSequenceNumber, MethodHandle hDraggingLocation) {}
    private static volatile Handles handles;

    private NSDraggingSession(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSDraggingSession wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDraggingSession(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
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
        try { return (long) handles.hSequenceNumber().invokeExact(peer, ObjC.sel("draggingSequenceNumber")); }
        catch (Throwable t) { throw new RuntimeException("draggingSequenceNumber failed", t); }
    }

    /// draggingLocation — location in screen coordinates (if available).
    public NSPoint draggingLocation() {
        ensureInit();
        if (handles.hDraggingLocation() == null) return NSPoint.ZERO;
        try {
            MemorySegment seg = (MemorySegment) handles.hDraggingLocation().invokeExact(ObjC.structSlot(), peer, ObjC.sel("draggingLocation"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("draggingLocation failed", t); }
    }

    /// draggingFormation — minimal.
    public long draggingFormation() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("draggingFormation"));
        } catch (Throwable t) { throw new RuntimeException("draggingFormation failed", t); }
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
        } catch (Throwable t) { throw new RuntimeException("animatesToStartingPositionsOnCancelOrFail failed", t); }
    }

    /// setAnimatesToStartingPositionsOnCancelOrFail:
    public void setAnimatesToStartingPositionsOnCancelOrFail(boolean flag) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, nsui.objc.Sig.Arg.BOOL));
            h.invokeExact(peer, ObjC.sel("setAnimatesToStartingPositionsOnCancelOrFail:"), flag);
        } catch (Throwable t) { throw new RuntimeException("setAnimatesToStartingPositionsOnCancelOrFail: failed", t); }
    }

    /// draggingLeaderIndex.
    public long draggingLeaderIndex() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("draggingLeaderIndex"));
        } catch (Throwable t) { throw new RuntimeException("draggingLeaderIndex failed", t); }
    }

    /// setDraggingLeaderIndex:.
    public void setDraggingLeaderIndex(long index) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, nsui.objc.Sig.Arg.INT));
            h.invokeExact(peer, ObjC.sel("setDraggingLeaderIndex:"), index);
        } catch (Throwable t) { throw new RuntimeException("setDraggingLeaderIndex: failed", t); }
    }
}
