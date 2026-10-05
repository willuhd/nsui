package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// CAConstraintLayoutManager — lays out a layer's sublayers from CAConstraints.
/// Thin stateless wrapper; attach via CALayer constraints + this manager.
public final class CAConstraintLayoutManager extends NSObject {

    private CAConstraintLayoutManager(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static CAConstraintLayoutManager wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAConstraintLayoutManager(peer);
    }

    /// +layoutManager.
    public static CAConstraintLayoutManager create() {
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        return wrap(ObjC.msgSendId(ObjC.cls("CAConstraintLayoutManager"), ObjC.sel("layoutManager")));
    }
}
