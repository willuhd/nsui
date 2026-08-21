package nsui;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;

import nsui.objc.DelegateProxy;

/// NSDraggingDestination — minimal protocol marker for drop targets.
public interface NSDraggingDestination {

    /// draggingEntered: — return NSDragOperation.
    default long draggingEntered(NSDraggingSession session) { return 0; }

    /// draggingUpdated:
    default long draggingUpdated(NSDraggingSession session) { return 0; }

    /// draggingExited:
    default void draggingExited(NSDraggingSession session) {}

    /// prepareForDragOperation:
    default boolean prepareForDragOperation(NSDraggingSession session) { return true; }

    /// performDragOperation:
    default boolean performDragOperation(NSDraggingSession session) { return false; }

    /// concludeDragOperation:
    default void concludeDragOperation(NSDraggingSession session) {}

    /// draggingEnded:
    default void draggingEnded(NSDraggingSession session) {}

    /// wantsPeriodicDraggingUpdates
    default boolean wantsPeriodicDraggingUpdates() { return true; }

    default MemorySegment peer() { return MemorySegment.NULL; }

    /// Create a DelegateProxy-backed ObjC delegate for this dragging destination.
    /// Reuses existing DelegateProxy dispatch shapes (IntArg for NSDragOperation/long,
    /// BoolArg for BOOL, VoidArg for void). No new Sig needed.
    static MemorySegment delegate(NSDraggingDestination dest) {
        if (dest == null) throw new IllegalArgumentException("dest is null");
        Map<String, DelegateProxy.IntArg> ints = new HashMap<>();
        Map<String, DelegateProxy.VoidArg> voids = new HashMap<>();
        Map<String, DelegateProxy.BoolArg> bools = new HashMap<>();

        ints.put("draggingEntered:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            // sender is NSDraggingInfo; wrap as session for convenience
            if (s == null) s = NSDraggingSession.wrap(sender);
            return dest.draggingEntered(s);
        });
        ints.put("draggingUpdated:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            if (s == null) s = NSDraggingSession.wrap(sender);
            return dest.draggingUpdated(s);
        });
        voids.put("draggingExited:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            dest.draggingExited(s);
        });
        bools.put("prepareForDragOperation:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            return dest.prepareForDragOperation(s);
        });
        bools.put("performDragOperation:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            return dest.performDragOperation(s);
        });
        voids.put("concludeDragOperation:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            dest.concludeDragOperation(s);
        });
        voids.put("draggingEnded:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            dest.draggingEnded(s);
        });

        return DelegateProxy.delegate(
                "NSObject", "NSUIDraggingDestination",
                bools, voids, ints, Map.of(), Map.of(), Map.of(), Map.of());
    }
}
