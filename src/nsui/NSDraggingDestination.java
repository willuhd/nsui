package nsui;

import java.lang.foreign.MemorySegment;

/**
 * NSDraggingDestination — minimal protocol marker for drop targets.
 */
public interface NSDraggingDestination {

    /** draggingEntered: — return NSDragOperation. */
    default long draggingEntered(NSDraggingSession session) { return 0; }

    /** draggingUpdated: */
    default long draggingUpdated(NSDraggingSession session) { return 0; }

    /** draggingExited: */
    default void draggingExited(NSDraggingSession session) {}

    /** prepareForDragOperation: */
    default boolean prepareForDragOperation(NSDraggingSession session) { return true; }

    /** performDragOperation: */
    default boolean performDragOperation(NSDraggingSession session) { return false; }

    /** concludeDragOperation: */
    default void concludeDragOperation(NSDraggingSession session) {}

    /** draggingEnded: */
    default void draggingEnded(NSDraggingSession session) {}

    /** wantsPeriodicDraggingUpdates */
    default boolean wantsPeriodicDraggingUpdates() { return true; }

    default MemorySegment peer() { return MemorySegment.NULL; }
}
