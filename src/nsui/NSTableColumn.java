package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSTableColumn — a single column of an {@link NSTableView}. Thin, 1:1,
 * stateless wrapper over a native {@code NSTableColumn}: each method maps to one
 * {@code objc_msgSend} selector. Created via {@code [[NSTableColumn alloc]
 * initWithIdentifier:]} and added to a table with {@link NSTableView#addTableColumn}.
 */
public final class NSTableColumn extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitId;       // (id, SEL, id) -> id    [initWithIdentifier:]
    private static MethodHandle hVoidId;       // (id, SEL, id) -> void  [setTitle:]
    private static MethodHandle hId;           // (id, SEL) -> id        [title]
    private static MethodHandle hVoidDouble;   // (id, SEL, double) -> void [setWidth:]
    private static MethodHandle hDouble;       // (id, SEL) -> double    [width]
    private static MethodHandle hInt;          // (id, SEL) -> long      [resizingMask etc]
    private static MethodHandle hVoidInt;      // (id, SEL, long) -> void
    private static MethodHandle hBool;         // (id, SEL) -> bool
    private static MethodHandle hVoidBool;     // (id, SEL, bool) -> void

    private NSTableColumn(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitId = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hId = ObjC.handle(Sig.of(Ret.ID));
        hVoidDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hInt = ObjC.handle(Sig.of(Ret.INT));
        hVoidInt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hVoidBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        initialized = true;
    }

    /** {@code [[NSTableColumn alloc] initWithIdentifier:identifier]} — a new column. */
    public static NSTableColumn create(String identifier) {
        ensureInit();
        MemorySegment c = ObjC.msgSendId(ObjC.cls("NSTableColumn"), ObjC.sel("alloc"));
        try {
            c = (MemorySegment) hInitId.invokeExact(c, ObjC.sel("initWithIdentifier:"),
                    (MemorySegment) ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSTableColumn", t);
        }
        if (c.address() == 0) {
            throw new IllegalStateException("NSTableColumn alloc/initWithIdentifier: returned nil");
        }
        return new NSTableColumn(c);
    }

    // ---------------------------------------------------------------- instance API

    /** [column setTitle:] — the column's header title. */
    public void setTitle(String title) {
        try {
            hVoidId.invokeExact(peer, ObjC.sel("setTitle:"), (MemorySegment) ObjC.nsstring(title));
        } catch (Throwable t) {
            throw new RuntimeException("setTitle: failed", t);
        }
    }

    /** [column title] — the column's header title. */
    public String title() {
        try {
            return ObjC.toString((MemorySegment) hId.invokeExact(peer, ObjC.sel("title")));
        } catch (Throwable t) {
            throw new RuntimeException("title failed", t);
        }
    }

    /** [column setWidth:] — the column's width in points. */
    public void setWidth(double width) {
        try {
            hVoidDouble.invokeExact(peer, ObjC.sel("setWidth:"), width);
        } catch (Throwable t) {
            throw new RuntimeException("setWidth: failed", t);
        }
    }

    /** [column width] — the column's width in points. */
    public double width() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("width"));
        } catch (Throwable t) {
            throw new RuntimeException("width failed", t);
        }
    }

    /** [column identifier] — NSString identifier. */
    public String identifier() {
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("identifier"));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("identifier failed", t); }
    }

    /** [column setIdentifier:] */
    public void setIdentifier(String ident) {
        try { hVoidId.invokeExact(peer, ObjC.sel("setIdentifier:"), ObjC.nsstring(ident)); } catch (Throwable t) { throw new RuntimeException("setIdentifier: failed", t); }
    }

    /** [column minWidth] */
    public double minWidth() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("minWidth")); } catch (Throwable t) { throw new RuntimeException("minWidth failed", t); }
    }

    /** [column setMinWidth:] */
    public void setMinWidth(double w) {
        try { hVoidDouble.invokeExact(peer, ObjC.sel("setMinWidth:"), w); } catch (Throwable t) { throw new RuntimeException("setMinWidth: failed", t); }
    }

    /** [column maxWidth] */
    public double maxWidth() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("maxWidth")); } catch (Throwable t) { throw new RuntimeException("maxWidth failed", t); }
    }

    /** [column setMaxWidth:] */
    public void setMaxWidth(double w) {
        try { hVoidDouble.invokeExact(peer, ObjC.sel("setMaxWidth:"), w); } catch (Throwable t) { throw new RuntimeException("setMaxWidth: failed", t); }
    }

    /** [column isHidden] */
    public boolean isHidden() {
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isHidden")); } catch (Throwable t) { throw new RuntimeException("isHidden failed", t); }
    }

    /** [column setHidden:] */
    public void setHidden(boolean flag) {
        try { hVoidBool.invokeExact(peer, ObjC.sel("setHidden:"), flag); } catch (Throwable t) { throw new RuntimeException("setHidden: failed", t); }
    }

    /** [column headerCell] — NSTableHeaderCell peer (id). */
    public MemorySegment headerCell() {
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("headerCell")); } catch (Throwable t) { throw new RuntimeException("headerCell failed", t); }
    }

    /** [column setHeaderCell:] */
    public void setHeaderCell(MemorySegment cell) {
        try { hVoidId.invokeExact(peer, ObjC.sel("setHeaderCell:"), cell == null ? MemorySegment.NULL : cell); } catch (Throwable t) { throw new RuntimeException("setHeaderCell: failed", t); }
    }

    /** [column dataCell] — NSCell peer (id). Deprecated but still queried by delegate tests. */
    public MemorySegment dataCell() {
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("dataCell")); } catch (Throwable t) { throw new RuntimeException("dataCell failed", t); }
    }

    /** [column setDataCell:] */
    public void setDataCell(MemorySegment cell) {
        try { hVoidId.invokeExact(peer, ObjC.sel("setDataCell:"), cell == null ? MemorySegment.NULL : cell); } catch (Throwable t) { throw new RuntimeException("setDataCell: failed", t); }
    }

    /** [column dataCellForRow:] — per-row data cell (if row-specific). */
    public MemorySegment dataCellForRow(long row) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("dataCellForRow:"), row);
        } catch (Throwable t) { throw new RuntimeException("dataCellForRow: failed", t); }
    }

    /** [column resizingMask] — NSTableColumnResizingOptions bitmask. */
    public long resizingMask() {
        try { return (long) hInt.invokeExact(peer, ObjC.sel("resizingMask")); } catch (Throwable t) { throw new RuntimeException("resizingMask failed", t); }
    }

    /** [column setResizingMask:] */
    public void setResizingMask(long mask) {
        try { hVoidInt.invokeExact(peer, ObjC.sel("setResizingMask:"), mask); } catch (Throwable t) { throw new RuntimeException("setResizingMask: failed", t); }
    }

    /** [column isEditable] */
    public boolean isEditable() {
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isEditable")); } catch (Throwable t) { throw new RuntimeException("isEditable failed", t); }
    }

    /** [column setEditable:] */
    public void setEditable(boolean flag) {
        try { hVoidBool.invokeExact(peer, ObjC.sel("setEditable:"), flag); } catch (Throwable t) { throw new RuntimeException("setEditable: failed", t); }
    }

    /** [column sortDescriptorPrototype] — NSSortDescriptor peer or null. */
    public MemorySegment sortDescriptorPrototype() {
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("sortDescriptorPrototype")); } catch (Throwable t) { throw new RuntimeException("sortDescriptorPrototype failed", t); }
    }

    /** [column setSortDescriptorPrototype:] */
    public void setSortDescriptorPrototype(MemorySegment desc) {
        try { hVoidId.invokeExact(peer, ObjC.sel("setSortDescriptorPrototype:"), desc == null ? MemorySegment.NULL : desc); } catch (Throwable t) { throw new RuntimeException("setSortDescriptorPrototype: failed", t); }
    }

    /** [column headerToolTip] */
    public String headerToolTip() {
        try { MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("headerToolTip")); return ObjC.toString(s); } catch (Throwable t) { throw new RuntimeException("headerToolTip failed", t); }
    }

    /** [column setHeaderToolTip:] */
    public void setHeaderToolTip(String tip) {
        try { hVoidId.invokeExact(peer, ObjC.sel("setHeaderToolTip:"), tip == null ? MemorySegment.NULL : ObjC.nsstring(tip)); } catch (Throwable t) { throw new RuntimeException("setHeaderToolTip: failed", t); }
    }

    /** [column sizeToFit] */
    public void sizeToFit() {
        try { ObjC.handle(Sig.of(Ret.VOID)).invokeExact(peer, ObjC.sel("sizeToFit")); } catch (Throwable t) { throw new RuntimeException("sizeToFit failed", t); }
    }
}
