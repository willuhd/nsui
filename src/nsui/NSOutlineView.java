package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSOutlineView — a hierarchical table (outline). Thin, 1:1, stateless wrapper
/// over the native `NSOutlineView`, which is an `NSTableView`
/// subclass. Follows the project template: volatile initialized, synchronized
/// ensureInit, ObjC.handle(Sig.of...), invokeExact, static create/wrap and
/// expands the table API with expand/collapse.
///
/// Created via `[[NSOutlineView alloc] initWithFrame:]` and populated
/// via an outline `dataSource` answering children-count / child / expandable
/// queries; the outline delegate supplies cell values for the outline column.
public final class NSOutlineView extends NSTableView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hInitFrame, MethodHandle hExpand, MethodHandle hExpandChildren, MethodHandle hIsExpanded, MethodHandle hGetId) {}
    private static volatile Handles handles;

    private NSOutlineView(MemorySegment peer) {
        super(peer);
        ensureOutlineInit();
    }

    /// Wrap an existing NSOutlineView peer.
    public static NSOutlineView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSOutlineView(peer);
    }

        private static synchronized void ensureOutlineInit() {
        if (handles != null) return;
        // NSTableView.ensureInit may not have run; ensure base handles exist if called first.
        // We handle our own symbols; base class init is lazy and synchronized separately.
        try { NSTableView.ensureInit(); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID))
        );
    }

    /// `[[NSOutlineView alloc] initWithFrame:frame]` — a new outline view.
    public static NSOutlineView create(NSRect frame) {
        ensureOutlineInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSOutlineView"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitFrame().invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSOutlineView", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSOutlineView alloc/initWithFrame: returned nil");
        return new NSOutlineView(p);
    }

    // ---------------------------------------------------------------- instance API

    /// [outline expandItem:] — expand a single item (no recursion).
    public void expandItem(MemorySegment item) {
        try {
            handles.hExpand().invokeExact(peer, ObjC.sel("expandItem:"), (MemorySegment) ((MemorySegment) (item == null ? MemorySegment.NULL : item)));
        } catch (Throwable t) {
            throw new RuntimeException("expandItem: failed", t);
        }
    }

    /// [outline expandItem:expandChildren:]
    public void expandItem(MemorySegment item, boolean expandChildren) {
        try {
            MemorySegment arg = (item == null || item.address() == 0) ? MemorySegment.NULL : item;
            handles.hExpandChildren().invokeExact(peer, ObjC.sel("expandItem:expandChildren:"), (MemorySegment) arg, expandChildren);
        } catch (Throwable t) {
            throw new RuntimeException("expandItem:expandChildren: failed", t);
        }
    }

    /// [outline collapseItem:]
    public void collapseItem(MemorySegment item) {
        try {
            handles.hExpand().invokeExact(peer, ObjC.sel("collapseItem:"), (MemorySegment) ((MemorySegment) (item == null ? MemorySegment.NULL : item)));
        } catch (Throwable t) {
            throw new RuntimeException("collapseItem: failed", t);
        }
    }

    /// [outline collapseItem:collapseChildren:]
    public void collapseItem(MemorySegment item, boolean collapseChildren) {
        try {
            MemorySegment arg = (item == null || item.address() == 0) ? MemorySegment.NULL : item;
            handles.hExpandChildren().invokeExact(peer, ObjC.sel("collapseItem:collapseChildren:"), (MemorySegment) arg, collapseChildren);
        } catch (Throwable t) {
            throw new RuntimeException("collapseItem:collapseChildren: failed", t);
        }
    }

    /// [outline isItemExpanded:]
    public boolean isItemExpanded(MemorySegment item) {
        try {
            return (boolean) handles.hIsExpanded().invokeExact(peer, ObjC.sel("isItemExpanded:"), (MemorySegment) ((MemorySegment) (item == null ? MemorySegment.NULL : item)));
        } catch (Throwable t) {
            throw new RuntimeException("isItemExpanded: failed", t);
        }
    }

    /// [outline isExpandable:] — whether the item can be expanded.
    public boolean isExpandable(MemorySegment item) {
        try {
            return (boolean) handles.hIsExpanded().invokeExact(peer, ObjC.sel("isExpandable:"), (MemorySegment) ((MemorySegment) (item == null ? MemorySegment.NULL : item)));
        } catch (Throwable t) {
            throw new RuntimeException("isExpandable: failed", t);
        }
    }

    /// [outline outlineTableColumn] — the outline column (NSTableColumn peer).
    public NSTableColumn outlineTableColumn() {
        try {
            MemorySegment c = (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("outlineTableColumn"));
            return (c == null || c.address() == 0) ? null : NSTableColumn.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("outlineTableColumn failed", t);
        }
    }

    /// [outline setOutlineTableColumn:]
    public void setOutlineTableColumn(NSTableColumn column) {
        try {
            handles.hExpand().invokeExact(peer, ObjC.sel("setOutlineTableColumn:"), (MemorySegment) ((MemorySegment) (column == null ? MemorySegment.NULL : column.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("setOutlineTableColumn: failed", t);
        }
    }

    /// [outline levelForItem:] — indentation level (NSInteger).
    public long levelForItem(MemorySegment item) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("levelForItem:"), (MemorySegment) (item == null ? MemorySegment.NULL : item));
        } catch (Throwable t) {
            throw new RuntimeException("levelForItem: failed", t);
        }
    }

    /// [outline parentForItem:] — parent id or nil.
    public MemorySegment parentForItem(MemorySegment item) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("parentForItem:"), (MemorySegment) (item == null ? MemorySegment.NULL : item));
        } catch (Throwable t) {
            throw new RuntimeException("parentForItem: failed", t);
        }
    }

    /// [outline child:ofItem:] — child at index of item.
    public MemorySegment child(long index, MemorySegment item) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("child:ofItem:"), index, (MemorySegment) (item == null ? MemorySegment.NULL : item));
        } catch (Throwable t) {
            throw new RuntimeException("child:ofItem: failed", t);
        }
    }

    /// [outline numberOfChildrenOfItem:]
    public long numberOfChildrenOfItem(MemorySegment item) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("numberOfChildrenOfItem:"), (MemorySegment) (item == null ? MemorySegment.NULL : item));
        } catch (Throwable t) {
            throw new RuntimeException("numberOfChildrenOfItem: failed", t);
        }
    }

    /// [outline reloadItem:reloadChildren:]
    public void reloadItem(MemorySegment item, boolean reloadChildren) {
        try {
            MemorySegment arg = (item == null || item.address() == 0) ? MemorySegment.NULL : item;
            handles.hExpandChildren().invokeExact(peer, ObjC.sel("reloadItem:reloadChildren:"), (MemorySegment) arg, reloadChildren);
        } catch (Throwable t) {
            throw new RuntimeException("reloadItem:reloadChildren: failed", t);
        }
    }

    /// [outline indentationPerLevel]
    public double indentationPerLevel() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("indentationPerLevel"));
        } catch (Throwable t) {
            throw new RuntimeException("indentationPerLevel failed", t);
        }
    }

    public void setIndentationPerLevel(double v) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
            h.invokeExact(peer, ObjC.sel("setIndentationPerLevel:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setIndentationPerLevel: failed", t);
        }
    }
}
