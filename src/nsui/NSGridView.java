package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSGridView — minimal wrapper over AppKit NSGridView (macOS 10.13+).
/// Thin 1:1 grid layout view; rows/columns managed via view hierarchy.
public final class NSGridView extends NSView {

            private record Handles(MethodHandle hInitFrame, MethodHandle hGridInit, MethodHandle hVoidId, MethodHandle hId, MethodHandle hInt, MethodHandle hVoidInt, MethodHandle hGetDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    private NSGridView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSGridView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSGridView(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE))
        );
    }

    /// [[NSGridView alloc] initWithFrame:]
    public static NSGridView create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSGridView"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitFrame().invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSGridView", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSGridView alloc/initWithFrame: returned nil");
        return new NSGridView(p);
    }

    /// +[NSGridView gridViewWithNumberOfColumns:rows:] — convenience factory
    public static NSGridView gridViewWithNumberOfColumnsRows(long cols, long rows) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGridInit().invokeExact(ObjC.cls("NSGridView"), ObjC.sel("gridViewWithNumberOfColumns:rows:"), cols, rows);
            if (p == null || p.address() == 0) throw new IllegalStateException("gridViewWithNumberOfColumns:rows: returned nil");
            return new NSGridView(p);
        } catch (Throwable t) {
            throw new RuntimeException("gridViewWithNumberOfColumns:rows: failed", t);
        }
    }

    /// [grid numberOfColumns]
    public long numberOfColumns() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, ObjC.sel("numberOfColumns"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfColumns failed", t);
        }
    }

    /// [grid numberOfRows]
    public long numberOfRows() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, ObjC.sel("numberOfRows"));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfRows failed", t);
        }
    }

    /// [grid columnAtIndex:] -> NSGridColumn (as NSObject)
    public NSObject columnAtIndex(long index) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment c = (MemorySegment) h.invokeExact(peer, ObjC.sel("columnAtIndex:"), index);
            return NSObject.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("columnAtIndex: failed", t);
        }
    }

    /// [grid rowAtIndex:] -> NSGridRow (as NSObject)
    public NSObject rowAtIndex(long index) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("rowAtIndex:"), index);
            return NSObject.wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("rowAtIndex: failed", t);
        }
    }

    /// [grid addRowWithViews:]
    public NSObject addRowWithViews(NSArray views) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("addRowWithViews:"), views.peer());
            return NSObject.wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("addRowWithViews: failed", t);
        }
    }

    /// [grid addColumnWithViews:]
    public NSObject addColumnWithViews(NSArray views) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment c = (MemorySegment) h.invokeExact(peer, ObjC.sel("addColumnWithViews:"), views.peer());
            return NSObject.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("addColumnWithViews: failed", t);
        }
    }

    /// [grid rowSpacing]
    public double rowSpacing() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("rowSpacing"));
        } catch (Throwable t) {
            throw new RuntimeException("rowSpacing failed", t);
        }
    }

    /// [grid setRowSpacing:]
    public void setRowSpacing(double v) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setRowSpacing:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setRowSpacing: failed", t);
        }
    }

    /// [grid columnSpacing]
    public double columnSpacing() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("columnSpacing"));
        } catch (Throwable t) {
            throw new RuntimeException("columnSpacing failed", t);
        }
    }

    /// [grid setColumnSpacing:]
    public void setColumnSpacing(double v) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setColumnSpacing:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setColumnSpacing: failed", t);
        }
    }
}
