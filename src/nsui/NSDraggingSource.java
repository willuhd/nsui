package nsui;

import java.lang.foreign.MemorySegment;

/**
 * NSDraggingSource — minimal protocol marker for drag sources.
 * In AppKit this is an informal protocol with optional methods.
 * Implementors provide sourceOperationMaskForDraggingContext: and related.
 */
public interface NSDraggingSource {

    /** draggingSession:sourceOperationMaskForDraggingContext: — return NSDragOperation mask. */
    default long draggingSessionSourceOperationMaskForDraggingContext(NSDraggingSession session, long context) {
        return 1; // NSDragOperationCopy
    }

    /** draggingSession:endedAtPoint:operation: */
    default void draggingSessionEndedAtPointOperation(NSDraggingSession session, NSPoint point, long operation) {}

    /** ignoreModifierKeysForDraggingSession: */
    default boolean ignoreModifierKeysForDraggingSession(NSDraggingSession session) { return false; }

    /** As NSObject peer if implemented by an NSObject subclass. */
    default MemorySegment peer() { return MemorySegment.NULL; }
}
