package nsui.tests;

import nsui.CALayer;
import nsui.NSApplication;
import nsui.NSRect;
import nsui.NSWindow;
import nsui.objc.ObjC;
import nsui.objc.Scratch;

/// Benchmarks for the toolkit's hottest paths.
///
/// Deliberately NOT JMH: plain `nanoTime` loops driven by
/// `TestKit`'s timing helpers. The budgets are generous fixed tripwires
/// (10x+ headroom over the dev machine) — they catch order-of-magnitude
/// regressions such as a hot path going quadratic, not 20% noise. Timing
/// lines are always printed so trends are visible even when everything passes.
///
/// Covered paths (the audit's worst offenders first):
/// - `ObjC.toString` at 0.5k/5k/20k chars — per-character
/// `reinterpret` cost lives here.
/// - `frame()` in a tight loop — the immortal-arena struct-return
/// path (`msgSendRect`).
/// - By-value INPUT marshalling (`rect` + `cstring`) inside
/// one `Scratch` turn — must stay time-bounded AND memory-bounded
/// (`used() < 1 MiB`, reset to 0).
/// - CALayer tree build/teardown — the compositing workload's unit cost.
///
/// Uses a hidden window only; never activates, never orders front.
public final class BenchTest {

    public static void main(String[] args) {
        System.out.println("=== BenchTest — hot-path tripwires (nanoTime, no JMH) ===");
        ObjC.init();

        NSApplication app = TestKit.app();

        try {
            toStringScaling();
        } catch (Throwable t) {
            TestKit.check(false, "toStringScaling threw: " + t);
        }
        try {
            frameLoop(app);
        } catch (Throwable t) {
            TestKit.check(false, "frameLoop threw: " + t);
        }
        try {
            scratchInputs();
        } catch (Throwable t) {
            TestKit.check(false, "scratchInputs threw: " + t);
        }
        try {
            layerTree();
        } catch (Throwable t) {
            TestKit.check(false, "layerTree threw: " + t);
        }

        TestKit.end();
    }

    /// ObjC.toString cost vs string length. 200 steady-state iterations each
    /// after 50 warmup; correctness asserted every iteration.
    private static void toStringScaling() {
        System.out.println("--- ObjC.toString scaling ---");
        int[] sizes = {500, 5000, 20000};
        long[] budgetsMs = {5000, 15000, 60000};
        for (int k = 0; k < sizes.length; k++) {
            int sz = sizes[k];
            String s = "x".repeat(sz);
            var seg = ObjC.nsstring(s);
            for (int i = 0; i < 50; i++) ObjC.toString(seg); // warmup
            boolean ok = true;
            long t0 = System.nanoTime();
            int iters = 200;
            for (int i = 0; i < iters; i++) {
                if (ObjC.toString(seg).length() != sz) { ok = false; break; }
            }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            TestKit.check(ok, "toString round-trip correct at size " + sz);
            TestKit.noteTime("toString size=" + sz, ms, iters);
            TestKit.checkTime("toString size=" + sz + " x" + iters, ms, budgetsMs[k]);
        }
    }

    /// NSWindow.frame() in a tight loop: exercises msgSendRect (immortal
    /// struct return) plus NSRect.fromSegment copying on every call.
    private static void frameLoop(NSApplication app) {
        System.out.println("--- frame() loop ---");
        NSWindow win = TestKit.hiddenWindow(400, 300);
        try {
            for (int i = 0; i < 100; i++) win.frame(); // warmup
            int iters = 20000;
            long t0 = System.nanoTime();
            double sink = 0;
            for (int i = 0; i < iters; i++) {
                NSRect f = win.frame();
                sink += f.x() + f.width();
            }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            TestKit.check(sink > 0, "frame() loop sank reads (sink=" + sink + ")");
            TestKit.noteTime("frame() hidden window", ms, iters);
            TestKit.checkTime("frame() x" + iters, ms, 60000);
        } finally {
            TestKit.close(win);
        }
    }

    /// 100k by-value inputs inside a single Scratch turn: bounded time and
    /// bounded memory (reused bump buffer, fully rewound after).
    private static void scratchInputs() {
        System.out.println("--- scratch inputs x100k ---");
        int iters = 100000;
        Scratch.beginTurn();
        long ms;
        try {
            for (int i = 0; i < 1000; i++) { ObjC.rect(i, i, 10, 10); ObjC.cstring("x"); } // warmup
            long t0 = System.nanoTime();
            for (int i = 0; i < iters; i++) {
                ObjC.rect(i, i, 10, 10);
                ObjC.cstring("bench-string");
            }
            ms = (System.nanoTime() - t0) / 1_000_000;
            TestKit.check(Scratch.used() < Scratch.BUFFER_BYTES,
                    "scratch 100k used < 1 MiB (" + Scratch.used() + " B)");
        } finally {
            Scratch.endTurn();
        }
        TestKit.check(Scratch.used() == 0, "scratch rewound to 0 after turn");
        TestKit.noteTime("scratch rect+cstring", ms, iters);
        TestKit.checkTime("scratch inputs x" + iters, ms, 30000);
    }

    /// Layer tree build/teardown: root + 8 children, verified then dropped.
    private static void layerTree() {
        System.out.println("--- layer tree x200 ---");
        int iters = 200;
        for (int i = 0; i < 10; i++) buildTree(); // warmup
        long t0 = System.nanoTime();
        int completed = 0;
        for (int i = 0; i < iters; i++) {
            if (buildTree()) completed++;
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        TestKit.check(completed == iters, "layer tree completed " + completed + "/" + iters);
        TestKit.noteTime("layer tree (root+8)", ms, iters);
        TestKit.checkTime("layer tree x" + iters, ms, 60000);
    }

    private static boolean buildTree() {
        CALayer root = CALayer.create();
        CALayer[] kids = new CALayer[8];
        for (int j = 0; j < 8; j++) {
            kids[j] = CALayer.create();
            root.addSublayer(kids[j]);
        }
        CALayer sup = kids[3].superlayer();
        return sup != null && sup.peer().address() == root.peer().address();
    }
}
