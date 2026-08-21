package nsui;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;

import nsui.objc.DelegateProxy;

/// NSDraggingSource — minimal protocol marker for drag sources.
/// In AppKit this is an informal protocol with optional methods.
/// Implementors provide sourceOperationMaskForDraggingContext: and related.
public interface NSDraggingSource {

    /// draggingSession:sourceOperationMaskForDraggingContext: — return NSDragOperation mask.
    default long draggingSessionSourceOperationMaskForDraggingContext(NSDraggingSession session, long context) {
        return 1; // NSDragOperationCopy
    }

    /// draggingSession:endedAtPoint:operation:
    default void draggingSessionEndedAtPointOperation(NSDraggingSession session, NSPoint point, long operation) {}

    /// ignoreModifierKeysForDraggingSession:
    default boolean ignoreModifierKeysForDraggingSession(NSDraggingSession session) { return false; }

    /// As NSObject peer if implemented by an NSObject subclass.
    default MemorySegment peer() { return MemorySegment.NULL; }

    /// Create a DelegateProxy-backed ObjC delegate/source for this dragging source.
    /// Reuses existing shapes (IntArg for long mask, VoidArg for void, BoolArg for BOOL).
    /// Note: sourceOperationMaskForDraggingContext: natively has 2 args (session, context)
    /// but we reuse IntArg (single-id) as approximation — context is ignored safely (extra register ignored on arm64).
    static MemorySegment delegate(NSDraggingSource src) {
        if (src == null) throw new IllegalArgumentException("src is null");
        Map<String, DelegateProxy.IntArg> ints = new HashMap<>();
        Map<String, DelegateProxy.VoidArg> voids = new HashMap<>();
        Map<String, DelegateProxy.BoolArg> bools = new HashMap<>();

        ints.put("draggingSession:sourceOperationMaskForDraggingContext:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            return src.draggingSessionSourceOperationMaskForDraggingContext(s, 0L);
        });
        // draggingSession:endedAtPoint:operation: has point+op args; we approximate with VoidArg (single sender)
        voids.put("draggingSession:endedAtPoint:operation:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            src.draggingSessionEndedAtPointOperation(s, NSPoint.ZERO, 0L);
        });
        bools.put("ignoreModifierKeysForDraggingSession:", sender -> {
            NSDraggingSession s = NSDraggingSession.wrap(sender);
            return src.ignoreModifierKeysForDraggingSession(s);
        });

        return DelegateProxy.delegate(
                "NSObject", "NSUIDraggingSource",
                bools, voids, ints, Map.of(), Map.of(), Map.of(), Map.of());
    }
}
