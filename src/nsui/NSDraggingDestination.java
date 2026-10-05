package nsui;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;

import nsui.objc.DelegateProxy;

/// NSDraggingDestination — minimal protocol marker for drop targets.
///
/// Coverage notes (header: NSDragging.h, destination section, wins on API truth):
/// - Wired: draggingEntered:/Updated:/Exited:, prepare/perform/conclude,
///   draggingEnded:, updateDraggingItemsForDrag: and the spring-loading
///   trio, all via the exact DelegateProxy single-sender shapes.
/// - Omitted: slideDraggedImageTo: (NSPoint argument has no DelegateProxy
///   dispatch shape — requested `of(Ret.VOID, Arg.POINT)` delegate form);
///   enumerateDraggingItemsWithOptions:… (block); springLoadingActivated:/
///   springLoadingHighlightChanged: (two-sender shapes have no DelegateProxy
///   form); namesOfPromisedFilesDroppedAtDestination: (deprecated).
///   `wantsPeriodicDraggingUpdates` is a no-arg BOOL with no DelegateProxy
///   shape, so it stays a documented default and is intentionally unwired.
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

    /// updateDraggingItemsForDrag:.
    default void updateDraggingItemsForDrag(NSDraggingSession session) {}

    /// springLoadingEntered: — return NSSpringLoadingOptions.
    default long springLoadingEntered(NSDraggingSession session) { return 0; }

    /// springLoadingUpdated: — return NSSpringLoadingOptions.
    default long springLoadingUpdated(NSDraggingSession session) { return 0; }

    /// springLoadingExited:.
    default void springLoadingExited(NSDraggingSession session) {}

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
        voids.put("updateDraggingItemsForDrag:", sender -> {
            dest.updateDraggingItemsForDrag(NSDraggingSession.wrap(sender));
        });
        ints.put("springLoadingEntered:", sender -> dest.springLoadingEntered(NSDraggingSession.wrap(sender)));
        ints.put("springLoadingUpdated:", sender -> dest.springLoadingUpdated(NSDraggingSession.wrap(sender)));
        voids.put("springLoadingExited:", sender -> {
            dest.springLoadingExited(NSDraggingSession.wrap(sender));
        });

        return DelegateProxy.delegate(
                "NSObject", "NSUIDraggingDestination",
                bools, voids, ints, Map.of(), Map.of(), Map.of(), Map.of());
    }
}
