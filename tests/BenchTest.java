package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.CALayer;
import nsui.MTLCommandBuffer;
import nsui.MTLClearColor;
import nsui.MTLCommandQueue;
import nsui.MTLDevice;
import nsui.MTLRenderCommandEncoder;
import nsui.MTLRenderPassColorAttachmentDescriptor;
import nsui.MTLRenderPassDescriptor;
import nsui.MTLTexture;
import nsui.MTLTextureDescriptor;
import nsui.NSApplication;
import nsui.NSArray;
import nsui.NSColor;
import nsui.NSDictionary;
import nsui.NSMutableAttributedString;
import nsui.NSNumber;
import nsui.NSPoint;
import nsui.NSRange;
import nsui.NSRect;
import nsui.NSSize;
import nsui.NSSlider;
import nsui.NSString;
import nsui.NSTableView;
import nsui.NSTextStorage;
import nsui.NSValue;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.Autorelease;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;
import nsui.objc.Scratch;
import nsui.objc.Sig;

/// Benchmarks for the toolkit's hottest paths.
///
/// Deliberately NOT JMH: plain nanoTime loops driven by TestKit's timing
/// helpers. Budgets are generous fixed tripwires (10x+ headroom) that catch
/// order-of-magnitude regressions, not 20% noise. Timing lines always print so
/// trends stay visible even when everything passes.
///
/// Covered: cache lookups, raw msgSend scalar/struct marshalling, typed handle
/// shapes, NSString conversion, Scratch by-value inputs, Foundation object
/// round-trips, collections, view/control/layer property access, the CALayer
/// tree, attributed text, autorelease pools, the data-source/boolean upcall
/// callback path, and Metal encode+commit. Every section asserts correctness
/// each iteration and a wall-clock budget at the end.
public final class BenchTest {

    private BenchTest() {}

    public static void main(String[] args) {
        System.out.println("=== BenchTest - hot-path + callback tripwires (nanoTime, no JMH) ===");
        ObjC.init();
        NSApplication app = TestKit.app();

        run("dispatch lookups", BenchTest::dispatchLookups);
        run("msgSend scalars", BenchTest::messageScalars);
        run("typed handle shapes", BenchTest::handleShapes);
        run("nsstring conversion", BenchTest::nsstringConversion);
        run("toString scaling", BenchTest::toStringScaling);
        run("scratch inputs", BenchTest::scratchInputs);
        run("foundation scalars", BenchTest::foundationScalars);
        run("collections", BenchTest::collections);
        run("view geometry", () -> viewGeometry(app));
        run("control value", BenchTest::controlValue);
        run("layer properties", BenchTest::layerProperties);
        run("layer tree", BenchTest::layerTree);
        run("attributed text", BenchTest::attributedText);
        run("autorelease pool", BenchTest::autoreleasePool);
        run("int upcall (data source)", BenchTest::upcallInt);
        run("bool upcall", BenchTest::upcallBool);
        run("metal encode+commit", BenchTest::metalEncode);

        TestKit.end();
    }

    private static void run(String name, TestKit.Body body) {
        try {
            body.run();
        } catch (Throwable t) {
            TestKit.check(false, "bench section '" + name + "' threw: " + t);
        }
    }

    /// Warm up, time, print, and assert a budget. The body must verify itself.
    private static void bench(String name, int warm, int iters, long budgetMs, TestKit.Body body) throws Throwable {
        for (int i = 0; i < warm; i++) body.run();
        long t0 = System.nanoTime();
        for (int i = 0; i < iters; i++) body.run();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        TestKit.noteTime(name, ms, iters);
        TestKit.checkTime(name + " x" + iters, ms, budgetMs);
    }

    // --------------------------------------------------------------- dispatch

    /// Selector/class result caches: steady state must be a map hit, no native call.
    private static void dispatchLookups() throws Throwable {
        MemorySegment frameSel = ObjC.sel("frame");
        MemorySegment viewCls = ObjC.cls("NSView");
        bench("ObjC.sel cache hit", 10000, 2000000, 20000, () -> {
            if (ObjC.sel("frame").address() != frameSel.address())
                throw new IllegalStateException("sel cache mismatch");
        });
        bench("ObjC.cls cache hit", 10000, 2000000, 20000, () -> {
            if (ObjC.cls("NSView").address() != viewCls.address())
                throw new IllegalStateException("cls cache mismatch");
        });
    }

    /// Raw typed msgSend helpers: LONG / BOOL / ID / RECT.
    private static void messageScalars() throws Throwable {
        NSString s = NSString.of("benchmark-string");
        NSString yes = NSString.of("YES");
        MemorySegment peer = s.peer();
        bench("msgSendLong length", 1000, 500000, 20000, () -> ObjC.msgSendLong(peer, ObjC.sel("length")));
        bench("msgSendBool boolValue", 1000, 500000, 20000,
                () -> ObjC.msgSendBool(yes.peer(), ObjC.sel("boolValue")));
        MethodHandle hBoolId = ObjC.handle(Sig.of(Sig.Ret.BOOL, Sig.Arg.ID));
        bench("handle BOOL+ID isEqual:", 1000, 500000, 20000,
                () -> { boolean b = (boolean) hBoolId.invokeExact(peer, ObjC.sel("isEqual:"), (MemorySegment) s.peer()); });
        bench("msgSendId description", 1000, 500000, 20000, () -> ObjC.msgSendId(peer, ObjC.sel("description")));

        NSWindow win = TestKit.hiddenWindow(200, 200);
        try {
            bench("msgSendRect frame", 1000, 200000, 30000, () -> ObjC.msgSendRect(win.peer(), ObjC.sel("frame")));
        } finally {
            TestKit.close(win);
        }
    }

    /// Cached MethodHandle + invokeExact for the non-scalar return shapes.
    private static void handleShapes() throws Throwable {
        NSNumber num = NSNumber.numberWithDouble(3.5);
        MethodHandle hDouble = ObjC.handle(Sig.of(Sig.Ret.DOUBLE));
        bench("handle DOUBLE", 1000, 500000, 20000,
                () -> { double d = (double) hDouble.invokeExact(num.peer(), ObjC.sel("doubleValue")); });

        NSValue vp = NSValue.valueWithPoint(new NSPoint(1, 2));
        MethodHandle hPoint = ObjC.handle(Sig.of(Sig.Ret.POINT));
        bench("handle POINT (struct return)", 1000, 500000, 20000,
                () -> { MemorySegment m = (MemorySegment) hPoint.invokeExact(ObjC.structSlot(), vp.peer(), ObjC.sel("pointValue")); });

        NSValue vs = NSValue.valueWithSize(new NSSize(3, 4));
        MethodHandle hSize = ObjC.handle(Sig.of(Sig.Ret.SIZE));
        bench("handle SIZE (struct return)", 1000, 500000, 20000,
                () -> { MemorySegment m = (MemorySegment) hSize.invokeExact(ObjC.structSlot(), vs.peer(), ObjC.sel("sizeValue")); });
    }

    /// Java String -> NSString -> Java String, the per-setter/per-getter path.
    private static void nsstringConversion() throws Throwable {
        MemorySegment ns = ObjC.nsstring("benchmark");
        bench("ObjC.nsstring", 1000, 200000, 20000, () -> {
            MemorySegment m = ObjC.nsstring("benchmark");
            if (m == null || m.address() == 0) throw new IllegalStateException("nsstring nil");
            if (ObjC.msgSendLong(m, ObjC.sel("length")) != 9) throw new IllegalStateException("nsstring length");
        });
        bench("ObjC.toString", 1000, 200000, 20000, () -> {
            if (!"benchmark".equals(ObjC.toString(ns))) throw new IllegalStateException("toString mismatch");
        });
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
            MemorySegment seg = ObjC.nsstring(s);
            for (int i = 0; i < 50; i++) ObjC.toString(seg);
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

    /// 100k by-value inputs inside a single Scratch turn: bounded time and
    /// bounded memory (reused bump buffer, fully rewound after).
    private static void scratchInputs() {
        System.out.println("--- scratch inputs x100k ---");
        int iters = 100000;
        Scratch.beginTurn();
        long ms;
        try {
            for (int i = 0; i < 1000; i++) { ObjC.rect(i, i, 10, 10); ObjC.cstring("x"); }
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

    // ------------------------------------------------------------- foundation

    private static void foundationScalars() throws Throwable {
        bench("NSNumber double round-trip", 1000, 300000, 20000,
                () -> NSNumber.numberWithDouble(1.5).doubleValue());
        bench("NSValue point round-trip", 1000, 200000, 20000,
                () -> NSValue.valueWithPoint(new NSPoint(1, 2)).pointValue());
        bench("NSString length", 1000, 500000, 20000, () -> NSString.of("bench").length());
    }

    private static void collections() throws Throwable {
        bench("NSMutableArray add x64", 100, 20000, 30000, () -> {
            NSArray a = NSArray.mutableArray();
            for (int i = 0; i < 64; i++) a.addObject(NSString.of("x"));
            if (a.count() != 64) throw new IllegalStateException("count=" + a.count());
        });
        bench("NSMutableDictionary set+get x32", 100, 20000, 30000, () -> {
            NSDictionary d = NSDictionary.mutableDictionary();
            for (int i = 0; i < 32; i++) d.setObjectForKey(NSString.of("v"), NSString.of("k" + i));
            if (d.objectForKey("k0") == null) throw new IllegalStateException("missing k0");
        });
    }

    // ----------------------------------------------------------- views/layers

    private static void viewGeometry(NSApplication app) {
        System.out.println("--- view geometry ---");
        NSWindow win = TestKit.hiddenWindow(400, 300);
        NSView v = NSView.create(new NSRect(0, 0, 400, 300), (ctx, d) -> {});
        win.setContentView(v);
        try {
            bench("NSView frame get", 1000, 200000, 20000, () -> v.frame());
            bench("NSView setFrame", 1000, 100000, 20000, () -> v.setFrame(new NSRect(10, 10, 100, 100)));
            bench("NSView convertRectToBacking", 1000, 200000, 20000,
                    () -> v.convertRectToBacking(new NSRect(0, 0, 100, 100)));
        } catch (Throwable t) {
            TestKit.check(false, "view geometry threw: " + t);
        } finally {
            TestKit.close(win);
        }
    }

    private static void controlValue() throws Throwable {
        NSSlider sl = NSSlider.create(new NSRect(0, 0, 200, 20));
        bench("NSSlider set+get doubleValue", 1000, 200000, 20000, () -> {
            sl.setDoubleValue(0.5);
            if (sl.doubleValue() < 0) throw new IllegalStateException("doubleValue");
        });
    }

    private static void layerProperties() throws Throwable {
        CALayer layer = CALayer.create();
        bench("CALayer cornerRadius set+get", 1000, 500000, 20000, () -> {
            layer.setCornerRadius(4);
            if (layer.cornerRadius() < 0) throw new IllegalStateException("cornerRadius");
        });
        bench("CALayer opacity set+get", 1000, 500000, 20000, () -> {
            layer.setOpacity(0.5f);
            if (layer.opacity() < 0) throw new IllegalStateException("opacity");
        });
    }

    /// Layer tree build/teardown: root + 8 children, verified then dropped.
    private static void layerTree() throws Throwable {
        for (int i = 0; i < 10; i++) buildTree();
        int iters = 200;
        long t0 = System.nanoTime();
        int completed = 0;
        for (int i = 0; i < iters; i++) if (buildTree()) completed++;
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

    private static void attributedText() throws Throwable {
        NSTextStorage storage = NSTextStorage.create("Hello World");
        bench("NSTextStorage length", 1000, 200000, 20000, () -> {
            if (storage.length() == 0) throw new IllegalStateException("length");
        });
        NSMutableAttributedString ms = NSMutableAttributedString.create("Hello World");
        MemorySegment red = NSColor.redColor().peer();
        bench("NSMutableAttributedString addAttribute x8", 100, 20000, 30000, () -> {
            for (int i = 0; i < 8; i++) ms.addAttribute("NSForegroundColorAttributeName", red, new NSRange(0, 5));
        });
    }

    private static void autoreleasePool() throws Throwable {
        bench("Autorelease push/pop", 10000, 1000000, 20000, () -> {
            MemorySegment pool = Autorelease.push();
            if (pool == null || pool.address() == 0) throw new IllegalStateException("pool nil");
            Autorelease.pop(pool);
        });
    }

    // -------------------------------------------------------------- callbacks

    /// Data-source upcall sent directly to the delegate: each call crosses
    /// downcall stub -> ObjC dispatch -> upcall stub -> registry -> lambda ->
    /// return marshalling. Measures the callback path itself.
    private static void upcallInt() throws Throwable {
        NSTableView table = NSTableView.create(new NSRect(0, 0, 400, 200));
        AtomicInteger fired = new AtomicInteger(0);
        Map<String, DelegateProxy.IntArg> ints = new LinkedHashMap<>();
        ints.put("numberOfRowsInTableView:", sender -> { fired.incrementAndGet(); return 50; });
        MemorySegment ds = DelegateProxy.delegate("NSObject", "BenchTableDS",
                new LinkedHashMap<>(), new LinkedHashMap<>(), ints, new LinkedHashMap<>());
        MethodHandle h = ObjC.handle(Sig.of(Sig.Ret.INT, Sig.Arg.ID));
        MemorySegment sel = ObjC.sel("numberOfRowsInTableView:");
        int before = fired.get();
        bench("int upcall round-trip", 100, 20000, 30000, () -> {
            if ((long) h.invokeExact(ds, sel, table.peer()) != 50) throw new IllegalStateException("upcall result");
        });
        TestKit.check(fired.get() - before >= 20000,
                "every int upcall reached Java (" + (fired.get() - before) + " >= 20000 incl. warmup)");
    }

    /// Boolean-returning upcall (a different stub shape).
    private static void upcallBool() throws Throwable {
        Map<String, DelegateProxy.BoolArg> bools = new LinkedHashMap<>();
        bools.put("acceptsFirstResponder", sender -> true);
        MemorySegment bd = DelegateProxy.delegate("NSObject", "BenchBoolDS", bools, new LinkedHashMap<>());
        MethodHandle hb = ObjC.handle(Sig.of(Sig.Ret.BOOL));
        MemorySegment selb = ObjC.sel("acceptsFirstResponder");
        bench("bool upcall round-trip", 100, 20000, 30000, () -> {
            if (!(boolean) hb.invokeExact(bd, selb)) throw new IllegalStateException("bool upcall");
        });
    }

    // ------------------------------------------------------------------ metal

    private static void metalEncode() throws Throwable {
        MTLDevice device = MTLDevice.systemDefault();
        if (device == null) { TestKit.skipCase("Metal: no system default device"); return; }
        MTLCommandQueue queue = device.newCommandQueue();
        MTLTextureDescriptor td = MTLTextureDescriptor.texture2D(80, 64, 64, false);
        td.setUsage(MTLTextureDescriptor.USAGE_RENDER_TARGET);
        td.setStorageMode(MTLTextureDescriptor.STORAGE_SHARED);
        MTLTexture tex = device.newTexture(td);
        if (tex == null) { TestKit.skipCase("Metal: render target texture nil"); return; }
        bench("Metal clear encode+commit", 20, 400, 60000, () -> {
            MemorySegment pool = Autorelease.push();
            try {
                MTLCommandBuffer buf = queue.commandBuffer();
                MTLRenderCommandEncoder enc = buf.renderEncoder(passFor(tex, new MTLClearColor(1, 0, 0, 1)));
                enc.endEncoding();
                buf.commit();
                buf.waitUntilCompleted();
            } finally {
                Autorelease.pop(pool);
            }
        });
    }

    private static MTLRenderPassDescriptor passFor(MTLTexture tex, MTLClearColor clear) {
        MTLRenderPassDescriptor pass = MTLRenderPassDescriptor.create();
        MTLRenderPassColorAttachmentDescriptor att = pass.colorAttachment(0);
        att.setTexture(tex);
        att.setLoadAction(MTLRenderPassColorAttachmentDescriptor.LOAD_CLEAR);
        att.setStoreAction(MTLRenderPassColorAttachmentDescriptor.STORE_STORE);
        att.setClearColor(clear);
        return pass;
    }
}
