package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTableHeaderView — the column header strip of a table: drag/resize state.
/// Thin stateless wrapper. (headerRectOfColumn:/columnAtPoint: need struct
/// shapes outside the current vocabulary — omitted, not load-bearing.)
public final class NSTableHeaderView extends NSView {

    private record Handles(MethodHandle hInitFrame, MethodHandle hGetDouble) {}
    private static volatile Handles handles;

    private NSTableHeaderView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSTableHeaderView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTableHeaderView(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)));
    }

    /// [[NSTableHeaderView alloc] initWithFrame:].
    public static NSTableHeaderView create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSTableHeaderView"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitFrame().invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSTableHeaderView", t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithFrame: returned nil for NSTableHeaderView");
        return new NSTableHeaderView(p);
    }

    /// tableView (raw peer; NSTableView has no wrap).
    public MemorySegment tableView() {
        return ObjC.msgSendId(peer, ObjC.sel("tableView"));
    }

    /// draggedColumn (-1 when no drag in flight).
    public long draggedColumn() {
        return ObjC.msgSendLong(peer, ObjC.sel("draggedColumn"));
    }

    /// draggedDistance, in points.
    public double draggedDistance() {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("draggedDistance"));
        } catch (Throwable t) {
            throw new RuntimeException("draggedDistance failed", t);
        }
    }
}
