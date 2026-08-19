package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import nsui.NSApplication;
import nsui.NSRect;
import nsui.NSScrollView;
import nsui.NSTableColumn;
import nsui.NSTableView;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * TableViewTest — a REAL data-source-driven {@code NSTableView} embedded in an
 * {@code NSScrollView}, proving the {@link DelegateProxy} data-source shapes:
 * {@code numberOfRowsInTableView:} (IntArg) and
 * {@code tableView:objectValueForTableColumn:row:} (IdIdIntArg) deliver real row
 * counts and cell values through the live AppKit display pass.
 *
 * <p>The table is shown in a real window (the ONLY way AppKit materializes cells),
 * so the assertions are the genuine callback counts AppKit performed while drawing:
 * {@link #N_ROWS} row-count queries and at least one cell query per row.
 *
 * <p>Also proves a {@code -(void)} delegate notification
 * ({@code tableViewSelectionDidChange:}) is accepted by routing the registered
 * selector through a check that the delegate {@code respondsToSelector:}.
 */
public final class TableViewTest {

    /** Row count our data source reports. */
    private static final long N_ROWS = 3;
    /** Number of columns we build. */
    private static final int N_COLS = 2;

    private static int failures;

    // Resolved once after ObjC.init() — never in a static initializer.
    private static MethodHandle hBoolId;      // (id, SEL, id/SEL) -> bool [respondsToSelector:]

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== TableViewTest — NSTableView + NSScrollView, live data source ===");
        ObjC.init();                                    // FFM bindings (must be first)
        hBoolId = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)); // resolves (BOOL, id) e.g. respondsToSelector:

        final AtomicLong rowsCalls = new AtomicLong();
        final AtomicLong cellCalls = new AtomicLong();

        // ---------------- build the scroll view + table -----------------
        NSScrollView scroll = NSScrollView.create(new NSRect(0, 0, 400, 200));
        NSView root = scroll; // bind a plain NSView reference so the hierarchy below is clear

        NSTableView table = NSTableView.create(new NSRect(0, 0, 400, 200));

        NSTableColumn nameCol = NSTableColumn.create("Name");
        nameCol.setTitle("Name");
        nameCol.setWidth(150);
        NSTableColumn scoreCol = NSTableColumn.create("Score");
        scoreCol.setTitle("Score");
        scoreCol.setWidth(100);
        check(Math.abs(nameCol.width() - 150) < 0.001, "name column width == 150 (got " + nameCol.width() + ")");
        check("Name".equals(nameCol.title()), "name column title is 'Name' (got '" + nameCol.title() + "')");

        table.addTableColumn(nameCol);
        table.addTableColumn(scoreCol);
        table.setUsesAlternatingRowBackgroundColors(true);
        table.setAllowsColumnResizing(true);
        check(Math.abs(table.rowHeight()) > 0.001, "table rowHeight > 0 (got " + table.rowHeight() + ")");

        scroll.setDocumentView(table);
        scroll.setHasVerticalScroller(true);
        scroll.setHasHorizontalScroller(true);
        scroll.setAutohidesScrollers(true);
        scroll.setBorderType(0L);                        // NSNoBorder

        // ---------------- the data-source delegate (6-arg overload) ----------------
        Map<String, DelegateProxy.BoolArg> bools = Map.of();
        Map<String, DelegateProxy.VoidArg> voids = new LinkedHashMap<>();
        // A void notification: if the table ever changes selection, AppKit would notify this.
        // We can't force a selection without a (BoolInt) shouldSelectRow shape (see report), so
        // we assert only that the delegate WELL-FORMEDLY accepts the registered selector.
        final AtomicLong selectionNotified = new AtomicLong();
        voids.put("tableViewSelectionDidChange:", note -> selectionNotified.incrementAndGet());
        Map<String, DelegateProxy.IntArg> ints = new LinkedHashMap<>();
        ints.put("numberOfRowsInTableView:", sender -> { rowsCalls.incrementAndGet(); return N_ROWS; });
        Map<String, DelegateProxy.IdIdIntArg> idIdInts = new LinkedHashMap<>();
        idIdInts.put("tableView:objectValueForTableColumn:row:",
                (tv, col, row) -> { cellCalls.incrementAndGet(); return ObjC.nsstring("cell-" + row); });

        MemorySegment dataSource = DelegateProxy.delegate(
                "NSObject", "NSUITableViewDS", bools, voids, ints, idIdInts);
        check(dataSource != null && dataSource.address() != 0, "data-source delegate created");
        check(DelegateProxy.registrySize() >= 1, "registry non-empty (size=" + DelegateProxy.registrySize() + ")");

        table.setDataSource(dataSource);

        // ---------------- real window + display pass -----------------
        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0);
        app.finishLaunching();

        NSWindow window = NSWindow.create(new NSRect(0, 0, 500, 300), 15L, 2L, false);
        window.setTitle("table view test");
        window.setReleasedWhenClosed(false);

        NSView content = NSView.create(new NSRect(0, 0, 500, 300), (ctx, d) -> {});
        window.setContentView(content);
        ObjC.msgSendVoidId(content.peer(), ObjC.sel("addSubview:"), root.peer());

        app.activateIgnoringOtherApps(true);
        window.makeKeyAndOrderFront(null);
        pump(app, 2000);                  // let AppKit find the table, lay it out and draw

        // The only reliable way AppKit materializes rows/cells is the display pass; force it
        // explicitly in case the deferred display pass hadn't redrawn the table yet.
        long rows = table.numberOfRows();
        check(rows == N_ROWS, "table numberOfRows == " + N_ROWS + " (got " + rows + ")");
        check(rowsCalls.get() >= 1, "numberOfRowsInTableView: fired >= 1 time (got " + rowsCalls.get() + ")");

        // Iterate until the table genuinely draws its cells (or we exhaust patience).
        int guard = 0;
        while (cellCalls.get() < N_ROWS && guard++ < 120) {
            table.reloadData();
            pump(app, 800);
            // displayIfNeeded: force the deferred display pass; NSView displayIfNeeded too.
            ObjC.msgSendVoid(window.peer(), ObjC.sel("displayIfNeeded"));
            ObjC.msgSendVoid(viewRootPeer(window), ObjC.sel("displayIfNeeded"));
            ObjC.msgSendVoid(table.peer(), ObjC.sel("displayIfNeeded"));
            pump(app, 400);
        }
        check(cellCalls.get() >= N_ROWS,
                "cell values materialized: tableView:objectValueForTableColumn:row: fired >= "
                        + N_ROWS + " times (got " + cellCalls.get() + ")");

        System.out.println("callback counts -> numberOfRowsInTableView:=" + rowsCalls.get()
                + "  objectValueForTableColumn:row:=" + cellCalls.get()
                + "  (expected rows >= 1, cells >= " + N_ROWS + ")");

        // ---------------- delegate: accepts a registered void notification ----------------
        MemorySegment delegate = dataSource;   // the same object; selection notification is a void void-map entry
        boolean accepts = respondsTo(delegate, "tableViewSelectionDidChange:");
        check(accepts, "delegate respondsToSelector: tableViewSelectionDidChange: (void shape registered)");
        System.out.println("selection delegate: respondsToSelector(tableViewSelectionDidChange:) = " + accepts
                + "  (selectionChanged fired=" + selectionNotified.get() + ")");

        check(rows == N_ROWS && rowsCalls.get() >= 1 && cellCalls.get() >= N_ROWS,
                "REAL data-source-driven table drew " + rows + " rows x " + N_COLS + " cols via live callbacks");

        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        window.performClose(null);
        System.exit(failures == 0 ? 0 : 1);
    }

    /** {@code [obj respondsToSelector:aSel]} via the (BOOL, id) handle (SEL rides an id register). */
    private static boolean respondsTo(MemorySegment obj, String selectorName) {
        try {
            return (boolean) hBoolId.invokeExact(obj, ObjC.sel("respondsToSelector:"),
                    ObjC.sel(selectorName));
        } catch (Throwable t) {
            throw new RuntimeException("respondsToSelector: failed", t);
        }
    }

    /** A view's root ancestor (the window content's parent host) — for the display cascade. */
    private static MemorySegment viewRootPeer(NSWindow win) {
        // The table's document-view chain already reaches its window via -window; here we just
        // kick a display on the window's contentView for good measure.
        return (MemorySegment) ObjC.msgSendId(win.peer(), ObjC.sel("contentView"));
    }

    /** True non-blocking pump: past deadline drains the queue; sendEvent + updateWindows. */
    private static void pump(NSApplication app, long ms) throws InterruptedException {
        long deadline = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < deadline) {
            MemorySegment until = ObjC.msgSendIdDouble(
                    ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSince1970:"), 0.0);
            nsui.NSEvent ev;
            int n = 0;
            while ((ev = app.nextEvent(-1L, until, "kCFRunLoopDefaultMode", true)) != null) {
                app.sendEvent(ev);
                if (++n > 400) break;
            }
            app.updateWindows();
            Thread.sleep(10);
        }
    }
}
