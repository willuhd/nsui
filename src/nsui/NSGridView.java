package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSGridView — minimal wrapper over AppKit NSGridView (macOS 10.13+).
/// Thin 1:1 grid layout view; rows/columns managed via view hierarchy.
///
/// Rows (NSGridRow), columns (NSGridColumn) and cells (NSGridCell) are returned unwrapped
/// as NSObject — those classes have no dedicated wrappers. Placement/alignment raw values
/// follow NSGridCellPlacement (inherited 0, none 1, leading/top 2, trailing/bottom 3, center 4,
/// fill 5) and NSGridRowAlignment (inherited 0, none 1, firstBaseline 2, lastBaseline 3).
///
/// Omitted from NSGridView.h: mergeCellsInHorizontalRange:verticalRange: needs of(VOID,RANGE,RANGE)
/// (absent from the Sig vocabulary — requested); NSGridViewSizeForContent constant (extern CGFloat,
/// value not verifiable from headers alone).
public final class NSGridView extends NSView {

            private record Handles(MethodHandle hGridInit, MethodHandle hVoidId, MethodHandle hId, MethodHandle hInt, MethodHandle hVoidInt, MethodHandle hGetDouble, MethodHandle hSetDouble) {}
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
        return new NSGridView(ObjC.newView("NSGridView", frame));
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

    // ---------------------------------------------------------------- header-completeness batch (NSGridView.h)

    /// +[NSGridView gridViewWithViews:] — grid sized to hold the given rows of views.
    public static NSGridView gridViewWithViews(NSArray rows) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(ObjC.cls("NSGridView"), ObjC.sel("gridViewWithViews:"), rows.peer());
            if (p == null || p.address() == 0) throw new IllegalStateException("gridViewWithViews: returned nil");
            return new NSGridView(p);
        } catch (Throwable t) {
            throw new RuntimeException("gridViewWithViews: failed", t);
        }
    }

    /// [grid indexOfRow:] — index of the row (O(numberOfRows)).
    public long indexOfRow(NSObject row) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("indexOfRow:"), row.peer());
        } catch (Throwable t) {
            throw new RuntimeException("indexOfRow: failed", t);
        }
    }

    /// [grid indexOfColumn:] — index of the column (O(numberOfColumns)).
    public long indexOfColumn(NSObject column) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("indexOfColumn:"), column.peer());
        } catch (Throwable t) {
            throw new RuntimeException("indexOfColumn: failed", t);
        }
    }

    /// [grid cellAtColumnIndex:rowIndex:] — the merged cell at those coordinates.
    public NSObject cellAtColumnIndexRowIndex(long columnIndex, long rowIndex) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.INT));
            MemorySegment c = (MemorySegment) h.invokeExact(peer, ObjC.sel("cellAtColumnIndex:rowIndex:"), columnIndex, rowIndex);
            return NSObject.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("cellAtColumnIndex:rowIndex: failed", t);
        }
    }

    /// [grid cellForView:] — the cell containing the view or its ancestor (may be nil).
    public NSObject cellForView(NSView view) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment c = (MemorySegment) h.invokeExact(peer, ObjC.sel("cellForView:"), view.peer());
            return NSObject.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("cellForView: failed", t);
        }
    }

    /// [grid insertRowAtIndex:withViews:].
    public NSObject insertRowAtIndexWithViews(long index, NSArray views) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("insertRowAtIndex:withViews:"), index, views.peer());
            return NSObject.wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("insertRowAtIndex:withViews: failed", t);
        }
    }

    /// [grid moveRowAtIndex:toIndex:].
    public void moveRowAtIndexToIndex(long fromIndex, long toIndex) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT));
            h.invokeExact(peer, ObjC.sel("moveRowAtIndex:toIndex:"), fromIndex, toIndex);
        } catch (Throwable t) {
            throw new RuntimeException("moveRowAtIndex:toIndex: failed", t);
        }
    }

    /// [grid removeRowAtIndex:].
    public void removeRowAtIndex(long index) {
        ensureInit();
        try {
            handles.hVoidInt().invokeExact(peer, ObjC.sel("removeRowAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("removeRowAtIndex: failed", t);
        }
    }

    /// [grid insertColumnAtIndex:withViews:].
    public NSObject insertColumnAtIndexWithViews(long index, NSArray views) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID));
            MemorySegment c = (MemorySegment) h.invokeExact(peer, ObjC.sel("insertColumnAtIndex:withViews:"), index, views.peer());
            return NSObject.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("insertColumnAtIndex:withViews: failed", t);
        }
    }

    /// [grid moveColumnAtIndex:toIndex:].
    public void moveColumnAtIndexToIndex(long fromIndex, long toIndex) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT));
            h.invokeExact(peer, ObjC.sel("moveColumnAtIndex:toIndex:"), fromIndex, toIndex);
        } catch (Throwable t) {
            throw new RuntimeException("moveColumnAtIndex:toIndex: failed", t);
        }
    }

    /// [grid removeColumnAtIndex:].
    public void removeColumnAtIndex(long index) {
        ensureInit();
        try {
            handles.hVoidInt().invokeExact(peer, ObjC.sel("removeColumnAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("removeColumnAtIndex: failed", t);
        }
    }

    /// [grid xPlacement] — NSGridCellPlacement.
    public long xPlacement() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, ObjC.sel("xPlacement"));
        } catch (Throwable t) {
            throw new RuntimeException("xPlacement failed", t);
        }
    }

    /// [grid setXPlacement:].
    public void setXPlacement(long placement) {
        ensureInit();
        try {
            handles.hVoidInt().invokeExact(peer, ObjC.sel("setXPlacement:"), placement);
        } catch (Throwable t) {
            throw new RuntimeException("setXPlacement: failed", t);
        }
    }

    /// [grid yPlacement] — NSGridCellPlacement.
    public long yPlacement() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, ObjC.sel("yPlacement"));
        } catch (Throwable t) {
            throw new RuntimeException("yPlacement failed", t);
        }
    }

    /// [grid setYPlacement:].
    public void setYPlacement(long placement) {
        ensureInit();
        try {
            handles.hVoidInt().invokeExact(peer, ObjC.sel("setYPlacement:"), placement);
        } catch (Throwable t) {
            throw new RuntimeException("setYPlacement: failed", t);
        }
    }

    /// [grid rowAlignment] — NSGridRowAlignment.
    public long rowAlignment() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, ObjC.sel("rowAlignment"));
        } catch (Throwable t) {
            throw new RuntimeException("rowAlignment failed", t);
        }
    }

    /// [grid setRowAlignment:].
    public void setRowAlignment(long alignment) {
        ensureInit();
        try {
            handles.hVoidInt().invokeExact(peer, ObjC.sel("setRowAlignment:"), alignment);
        } catch (Throwable t) {
            throw new RuntimeException("setRowAlignment: failed", t);
        }
    }

}
