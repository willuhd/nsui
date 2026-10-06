package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import nsui.CABasicAnimation;
import nsui.CALayer;
import nsui.NSPoint;
import nsui.NSRange;
import nsui.NSRect;
import nsui.NSSize;
import nsui.NSValue;
import nsui.objc.ObjC;

/// Edge cases the rest of the suite never pins down (all pre-existing gaps):
///
/// - `ObjC.cstring` rejects embedded NUL bytes.
/// - `ObjC.toString` 16 MB cap truncates exactly at cap.
/// - `NSRect` geometry branches beyond the happy path (negative sizes,
/// empty rects, non-overlap intersection, integral, area).
/// - The `invoke` escape hatch enforces its documented 6-arg bound.
/// - Concurrent `sel`/`cls` cache reads are safe; sequential
/// double `ObjC.init()` is idempotent. Concurrent *init*
/// itself stays untested on purpose (unguarded initializer — the audit's
/// latent race; this test refuses to deliberately hang the JVM).
/// - `sel`/`cls` results stable process-wide (result-caching invariant).
/// - `NSValue` factories build real native values (round-trips prove interop).
/// - Studio-era typed wrappers (`CALayer` bounds/frame,
/// `CAAnimation` fillMode/autoreverses/repeatCount), previously
/// called only by the untracked demo.
///
/// Pure-memory except where noted; hidden windows nowhere.
public final class EdgeTest {

    public static void main(String[] args) throws Throwable {
        System.out.println("=== EdgeTest — uncovered edges ===");
        ObjC.init();

        cstringNul();
        truncationCap();
        rectGeometry();
        invokeGuard();
        cacheConcurrency();
        doubleInit();
        typedWrappers();
        cacheIdentity();
        nativeValues();

        TestKit.end();
    }

    /// cstring must reject embedded NUL (it would silently truncate in C).
    private static void cstringNul() {
        TestKit.expectThrows("cstring with NUL throws IllegalArgumentException",
                IllegalArgumentException.class, () -> ObjC.cstring("a\0b"));
    }

    /// 16 MB cap: truncate-at-cap and return. (Was a proven crash before the
    /// chunked-probing fix: the loop broke at the cap without finding NUL and
    /// getString scanned past the region. Now covered live.)
    private static void truncationCap() {
        int big = 17_000_000;
        String s = "y".repeat(big);
        MemorySegment seg = ObjC.nsstring(s);
        long t0 = System.nanoTime();
        String back = ObjC.toString(seg);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        TestKit.check(back != null && back.length() == 16_000_000,
                "17M-char toString truncates exactly at cap (got " + (back == null ? "null" : back.length()) + ")");
        TestKit.check(back != null && s.startsWith(back),
                "truncated prefix matches original content");
        TestKit.noteTime("17M-char toString (cap path)", ms, 1);
    }

    /// NSRect geometry: boundaries, empties, negatives, non-overlap, rounding.
    private static void rectGeometry() {
        System.out.println("--- NSRect geometry ---");
        NSRect r = new NSRect(0, 0, 100, 100);
        TestKit.check(r.contains(new NSPoint(0, 0)), "contains origin (inclusive)");
        TestKit.check(r.contains(new NSPoint(99.9, 99.9)), "contains interior");
        TestKit.check(!r.contains(new NSPoint(100, 50)), "max edge exclusive");
        TestKit.check(!r.contains(new NSPoint(-0.1, 50)), "outside min rejected");
        TestKit.check(r.contains(new NSRect(10, 10, 20, 20)), "contains inner rect");
        TestKit.check(r.contains(r), "contains itself");
        TestKit.check(!r.contains(new NSRect(50, 50, 100, 100)), "overlapping-but-outside not contained");

        NSRect b = new NSRect(50, 50, 100, 100);
        TestKit.check(r.intersects(b), "overlap intersects");
        TestKit.check(b.intersects(r), "intersects symmetric");
        TestKit.check(!r.intersects(new NSRect(200, 200, 10, 10)), "disjoint does not intersect");
        TestKit.check(r.intersection(b).equals(new NSRect(50, 50, 50, 50)), "intersection == {50,50,50,50}");
        TestKit.check(r.intersection(new NSRect(200, 200, 10, 10)).equals(NSRect.ZERO),
                "non-overlap intersection is ZERO");

        TestKit.check(r.unionRect(b).equals(new NSRect(0, 0, 150, 150)), "union == {0,0,150,150}");
        TestKit.check(r.unionRect(NSRect.ZERO).equals(r), "union with ZERO is identity");
        TestKit.check(NSRect.ZERO.unionRect(r).equals(r), "ZERO union rect is identity");

        TestKit.check(NSRect.ZERO.isEmpty(), "ZERO isEmpty");
        TestKit.check(new NSRect(0, 0, -5, 10).isEmpty(), "negative width isEmpty");
        TestKit.check(r.area() == 10000, "area 100x100 == 10000");
        TestKit.check(r.midX() == 50 && r.midY() == 50, "midpoint (50,50)");
        TestKit.check(r.offset(5, -5).equals(new NSRect(5, -5, 100, 100)), "offset moves origin only");
        TestKit.check(r.inset(10, 10).equals(new NSRect(10, 10, 80, 80)), "inset shrinks symmetrically");
        TestKit.check(new NSRect(100, 100, -50, -50).standardized().equals(new NSRect(50, 50, 50, 50)),
                "standardized flips negative size");
        TestKit.check(new NSRect(0.2, 0.7, 99.1, 99.9).integral().equals(new NSRect(0, 0, 100, 101)),
                "integral floors origin, ceils far corner");
        TestKit.check(new NSRect(0, 0, 100, 100).centeredIn(new NSRect(0, 0, 200, 200))
                .equals(new NSRect(50, 50, 100, 100)), "centeredIn computes (50,50,100,100)");
    }

    /// Escape-hatch arity guard fires before any native call (null-safe).
    private static void invokeGuard() {
        MemorySegment[] seven = new MemorySegment[7];
        for (int i = 0; i < 7; i++) seven[i] = MemorySegment.NULL;
        TestKit.expectThrows("invoke with 7 args throws IllegalArgumentException",
                IllegalArgumentException.class, () -> ObjC.invoke(null, null, seven));
        TestKit.expectThrows("invokeVoid with 7 args throws IllegalArgumentException",
                IllegalArgumentException.class, () -> ObjC.invokeVoid(null, null, seven));
    }

    /// sel/cls caches (ConcurrentHashMap) under 4-thread read pressure.
    private static void cacheConcurrency() throws Throwable {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<MemorySegment>> jobs = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                jobs.add(() -> ObjC.sel("setTitle:"));
                jobs.add(() -> ObjC.cls("NSString"));
            }
            List<Future<MemorySegment>> out = pool.invokeAll(jobs, 30, TimeUnit.SECONDS);
            MemorySegment sel0 = ObjC.sel("setTitle:");
            MemorySegment cls0 = ObjC.cls("NSString");
            boolean ok = true;
            for (int i = 0; i < out.size(); i++) {
                MemorySegment got = out.get(i).get();
                MemorySegment want = (i % 2 == 0) ? sel0 : cls0;
                if (got.address() != want.address()) { ok = false; break; }
            }
            TestKit.check(ok, "128 concurrent sel/cls reads resolve identically");
        } finally {
            pool.shutdownNow();
        }
    }

    /// Sequential re-init must be harmless (frameworks re-dlopen, handles rebuild).
    private static void doubleInit() {
        try {
            ObjC.init();
            String back = ObjC.toString(ObjC.nsstring("re-init-ok"));
            TestKit.check("re-init-ok".equals(back), "double ObjC.init() still round-trips");
        } catch (Throwable t) {
            TestKit.check(false, "double ObjC.init() threw: " + t);
        }
    }

    /// sel/cls results stable process-wide (the invariant result caching relies on).
    private static void cacheIdentity() {
        MemorySegment s1 = ObjC.sel("setTitle:");
        MemorySegment s2 = ObjC.sel("setTitle:");
        TestKit.check(s1.address() != 0 && s1.address() == s2.address(), "sel stable across calls");
        MemorySegment c1 = ObjC.cls("NSString");
        MemorySegment c2 = ObjC.cls("NSString");
        TestKit.check(c1.address() != 0 && c1.address() == c2.address(), "cls stable across calls");
    }

    /// NSValue factories build REAL native values (retained): getters must read
    /// live structs back, proving AppKit interop (dummy peers never could).
    private static void nativeValues() {
        try {
            TestKit.check(NSValue.valueWithPoint(new NSPoint(1, 2)).pointValue().equals(new NSPoint(1, 2)),
                    "NSValue point round-trip");
            TestKit.check(NSValue.valueWithSize(new NSSize(3, 4)).sizeValue().equals(new NSSize(3, 4)),
                    "NSValue size round-trip");
            TestKit.check(NSValue.valueWithRect(new NSRect(1, 2, 3, 4)).rectValue().equals(new NSRect(1, 2, 3, 4)),
                    "NSValue rect round-trip");
            TestKit.check(NSValue.valueWithRange(new NSRange(5, 6)).rangeValue().equals(new NSRange(5, 6)),
                    "NSValue range round-trip");
        } catch (Throwable t) {
            TestKit.check(false, "nativeValues threw: " + t);
        }
    }

    /// Studio-era typed wrappers, proven through round-trips (previously only
    /// the untracked demo called them; the suite exercised the same selectors
    /// exclusively via raw handles).
    private static void typedWrappers() {
        try {
            CALayer layer = CALayer.create();
            TestKit.check(layer != null, "CALayer.create non-null");
            // 16-byte struct returns share the reusable slot too: consecutive
            // position() reads must each see fresh values (no cross-talk).
            layer.setPosition(new NSPoint(11, 22));
            TestKit.check(layer.position().equals(new NSPoint(11, 22)), "CALayer position round-trip");
            layer.setPosition(new NSPoint(33, 44));
            TestKit.check(layer.position().equals(new NSPoint(33, 44)),
                    "CALayer position rewrite reads fresh (slot overwrite sound)");
            layer.setBounds(new NSRect(10, 20, 300, 400));
            TestKit.check(layer.bounds().equals(new NSRect(10, 20, 300, 400)),
                    "CALayer bounds round-trip (got " + layer.bounds() + ")");
            layer.setFrame(new NSRect(5, 5, 100, 100));
            TestKit.check(layer.frame().equals(new NSRect(5, 5, 100, 100)),
                    "CALayer frame round-trip (got " + layer.frame() + ")");
        } catch (Throwable t) {
            TestKit.check(false, "CALayer typed bounds/frame threw: " + t);
        }
        try {
            CABasicAnimation anim = CABasicAnimation.create("position");
            anim.setFillMode("forwards");
            TestKit.check("forwards".equals(anim.fillMode()), "fillMode round-trip");
            anim.setAutoreverses(true);
            TestKit.check(anim.autoreverses(), "autoreverses round-trip");
            anim.setRepeatCount(3.0f);
            TestKit.check(anim.repeatCount() == 3.0f, "repeatCount round-trip");
            anim.setRemovedOnCompletion(false);
            TestKit.check(!anim.removedOnCompletion(), "removedOnCompletion round-trip");
        } catch (Throwable t) {
            TestKit.check(false, "CAAnimation typed wrappers threw: " + t);
        }
    }
}
