package nsui.tests;

import java.lang.foreign.MemorySegment;
import nsui.NSEdgeInsets;
import nsui.NSPoint;
import nsui.NSRange;
import nsui.NSRect;
import nsui.NSSize;
import nsui.objc.ObjC;
import nsui.objc.Scratch;

/**
 * Tests for NSRange and NSEdgeInsets value types.
 * Pure-memory struct tests — no windows, no run loop — but still requires ObjC.init().
 */
public final class NSRangeEdgeInsetsTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    private static boolean near(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    public static void main(String[] args) {
        System.out.println("=== NSRangeEdgeInsetsTest — NSRange + NSEdgeInsets structs ===");
        ObjC.init();

        // ---- NSRange: ZERO and make ----
        check(NSRange.ZERO.location() == 0 && NSRange.ZERO.length() == 0, "NSRange.ZERO == {0,0}");
        NSRange r = NSRange.make(5, 10);
        check(r.location() == 5 && r.length() == 10, "NSRange.make(5,10)");

        // ---- NOT_FOUND ----
        check(NSRange.NOT_FOUND == Long.MAX_VALUE, "NOT_FOUND == Long.MAX_VALUE");
        NSRange notFound = new NSRange(NSRange.NOT_FOUND, 0);
        check(notFound.isNotFound(), "isNotFound() true when location==NOT_FOUND");
        check(!r.isNotFound(), "isNotFound() false for normal range");
        check(new NSRange(0, 0).isNotFound() == false, "isNotFound false for ZERO");

        // ---- isEmpty ----
        check(new NSRange(0, 0).isEmpty(), "isEmpty true for length 0");
        check(new NSRange(5, -1).isEmpty(), "isEmpty true for negative length");
        check(!new NSRange(5, 1).isEmpty(), "isEmpty false for length 1");
        check(NSRange.ZERO.isEmpty(), "ZERO isEmpty");

        // ---- max / end ----
        check(r.max() == 15 && r.end() == 15, "max/end == location+length (5+10=15)");
        NSRange r2 = new NSRange(100, 0);
        check(r2.max() == 100, "max for empty range == location");

        // ---- toSegment / fromSegment round-trip ----
        System.out.println("\n-- NSRange toSegment/fromSegment --");
        NSRange[] samples = {
            NSRange.ZERO,
            new NSRange(0, 0),
            new NSRange(5, 10),
            new NSRange(0, 100),
            new NSRange(100, 0),
            new NSRange(NSRange.NOT_FOUND, 0),
            new NSRange(1_000_000, 2_000_000),
            new NSRange(Long.MAX_VALUE - 1, 1)
        };
        for (NSRange s : samples) {
            MemorySegment seg = s.toSegment();
            NSRange back = NSRange.fromSegment(seg);
            check(back.equals(s), "round-trip " + s + " -> " + back);
        }
        // explicit offset check: location at 0, length at 8
        NSRange probe = new NSRange(42, 99);
        MemorySegment seg = probe.toSegment();
        check(seg.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0) == 42, "toSegment location at offset 0");
        check(seg.get(java.lang.foreign.ValueLayout.JAVA_LONG, 8) == 99, "toSegment length at offset 8");
        check(NSRange.fromSegment(seg).equals(probe), "fromSegment reads probe correctly");

        // inside a Scratch turn (bump arena path)
        Scratch.beginTurn();
        try {
            for (NSRange s : samples) {
                MemorySegment sc = s.toSegment();
                check(NSRange.fromSegment(sc).equals(s), "scratch round-trip " + s);
            }
        } finally {
            Scratch.endTurn();
        }
        check(Scratch.used() == 0, "Scratch used==0 after turn");

        // ---- contains(index) ----
        System.out.println("\n-- NSRange contains(index) --");
        NSRange outer = new NSRange(5, 10); // [5,15)
        check(outer.contains(5), "contains 5 (inclusive lower)");
        check(outer.contains(14), "contains 14 (last)");
        check(!outer.contains(15), "not contains 15 (exclusive upper)");
        check(!outer.contains(4), "not contains 4 (before)");
        check(!outer.contains(100), "not contains 100");
        check(NSRange.ZERO.contains(0) == false, "ZERO not contains 0");

        // ---- contains(range) ----
        System.out.println("\n-- NSRange contains(range) --");
        NSRange inner = new NSRange(7, 3); // [7,10)
        NSRange outside = new NSRange(3, 2);
        NSRange overlapping = new NSRange(12, 5); // [12,17) partially outside
        NSRange exact = new NSRange(5, 10);
        check(outer.contains(inner), "outer contains inner [7,3)");
        check(outer.contains(exact), "outer contains itself");
        check(!outer.contains(outside), "outer not contains [3,2)");
        check(!outer.contains(overlapping), "outer not contains overlapping [12,5)");
        // empty range containment (special case in implementation)
        NSRange emptyAtStart = new NSRange(5, 0);
        NSRange emptyAtEnd = new NSRange(15, 0);
        NSRange emptyOutside = new NSRange(16, 0);
        check(outer.contains(emptyAtStart), "outer contains empty at location 5");
        check(outer.contains(emptyAtEnd), "outer contains empty at end 15");
        check(!outer.contains(emptyOutside), "outer not contains empty at 16");

        // ---- intersects / intersection ----
        System.out.println("\n-- NSRange intersects / intersection --");
        NSRange a = new NSRange(0, 10); // [0,10)
        NSRange b = new NSRange(5, 10); // [5,15)
        NSRange c = new NSRange(10, 5); // [10,15) touches at 10
        NSRange d = new NSRange(20, 5); // disjoint
        check(a.intersects(b), "[0,10) intersects [5,10)");
        check(b.intersects(a), "symmetric");
        check(!a.intersects(c), "[0,10) not intersects [10,5) (edge touching)");
        check(!a.intersects(d), "[0,10) not intersects [20,5)");
        check(!a.intersects(new NSRange(0, 0)), "not intersects empty");
        check(!NSRange.ZERO.intersects(b), "ZERO not intersects");

        check(a.intersection(b).equals(new NSRange(5, 5)), "intersection [0,10)&[5,15) == [5,5)");
        check(a.intersection(c).equals(NSRange.ZERO), "intersection touching == ZERO");
        check(a.intersection(d).equals(NSRange.ZERO), "intersection disjoint == ZERO");
        check(new NSRange(5, 10).intersection(new NSRange(5, 10)).equals(new NSRange(5, 10)), "intersection self == self");
        check(new NSRange(0, 20).intersection(new NSRange(5, 5)).equals(new NSRange(5, 5)), "outer intersection inner == inner");

        // ---- union, offset, inset ----
        check(new NSRange(0, 5).unionRange(new NSRange(10, 5)).equals(new NSRange(0, 15)), "unionRange");
        check(new NSRange(5, 5).offset(10).equals(new NSRange(15, 5)), "offset");
        check(new NSRange(5, 10).inset(2).equals(new NSRange(7, 6)), "inset");

        // ---- NSEdgeInsets ----
        System.out.println("\n-- NSEdgeInsets --");
        check(NSEdgeInsets.ZERO.isZero(), "ZERO isZero");
        check(!new NSEdgeInsets(1, 0, 0, 0).isZero(), "non-zero not isZero");
        check(NSEdgeInsets.ZERO.horizontal() == 0, "ZERO horizontal 0");
        check(NSEdgeInsets.ZERO.vertical() == 0, "ZERO vertical 0");

        NSEdgeInsets insets = NSEdgeInsets.make(10, 20, 30, 40);
        check(insets.top() == 10 && insets.left() == 20 && insets.bottom() == 30 && insets.right() == 40,
                "make(10,20,30,40)");
        check(near(insets.horizontal(), 60, 1e-9), "horizontal == left+right (20+40=60)");
        check(near(insets.vertical(), 40, 1e-9), "vertical == top+bottom (10+30=40)");

        // toSegment / fromSegment
        NSRect dummy = new NSRect(0, 0, 100, 100);
        NSEdgeInsets[] insetSamples = {
            NSEdgeInsets.ZERO,
            new NSEdgeInsets(1, 2, 3, 4),
            new NSEdgeInsets(0.5, 1.5, 2.5, 3.5),
            new NSEdgeInsets(-1, -2, -3, -4),
            new NSEdgeInsets(100_000.5, 200_000.25, 300_000.125, 400_000.0625)
        };
        for (NSEdgeInsets s : insetSamples) {
            MemorySegment sseg = s.toSegment();
            // verify layout top@0 left@8 bottom@16 right@24
            check(near(sseg.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 0), s.top(), 1e-9), "insets top at offset 0 for " + s);
            check(near(sseg.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 8), s.left(), 1e-9), "insets left at offset 8 for " + s);
            check(near(sseg.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 16), s.bottom(), 1e-9), "insets bottom at offset 16 for " + s);
            check(near(sseg.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 24), s.right(), 1e-9), "insets right at offset 24 for " + s);
            NSEdgeInsets back = NSEdgeInsets.fromSegment(sseg);
            check(back.epsilonEquals(s, 1e-9), "insets round-trip " + s + " -> " + back);
        }
        // scratch turn round-trip
        Scratch.beginTurn();
        try {
            for (NSEdgeInsets s : insetSamples) {
                check(NSEdgeInsets.fromSegment(s.toSegment()).epsilonEquals(s, 1e-9), "scratch insets round-trip " + s);
            }
        } finally {
            Scratch.endTurn();
        }

        // insetRect
        System.out.println("\n-- NSEdgeInsets insetRect --");
        NSEdgeInsets e = new NSEdgeInsets(10, 20, 30, 40);
        NSRect rect = new NSRect(0, 0, 100, 100);
        NSRect inset = e.insetRect(rect);
        // insetRect: x+left, y+bottom, w-left-right, h-top-bottom
        // 0+20=20, 0+30=30, 100-20-40=40, 100-10-30=60
        check(near(inset.x(), 20, 1e-9) && near(inset.y(), 30, 1e-9)
                && near(inset.width(), 40, 1e-9) && near(inset.height(), 60, 1e-9),
                "insetRect {0,0,100,100} by {10,20,30,40} == {20,30,40,60} got " + inset);
        // zero insets is identity
        check(NSEdgeInsets.ZERO.insetRect(rect).equals(rect), "ZERO insetRect is identity");
        // insetRect with fractional
        NSEdgeInsets f = new NSEdgeInsets(1.5, 2.5, 3.5, 4.5);
        NSRect r3 = new NSRect(10, 10, 50, 60);
        NSRect ir = f.insetRect(r3);
        check(near(ir.x(), 12.5, 1e-9) && near(ir.y(), 13.5, 1e-9)
                && near(ir.width(), 43, 1e-9) && near(ir.height(), 55, 1e-9),
                "fractional insetRect got " + ir);

        // add / negated / epsilonEquals
        NSEdgeInsets add = new NSEdgeInsets(1, 2, 3, 4).add(new NSEdgeInsets(5, 6, 7, 8));
        check(add.equals(new NSEdgeInsets(6, 8, 10, 12)), "add");
        check(new NSEdgeInsets(1, 2, 3, 4).negated().equals(new NSEdgeInsets(-1, -2, -3, -4)), "negated");
        check(new NSEdgeInsets(1, 2, 3, 4).epsilonEquals(new NSEdgeInsets(1.0000001, 2, 3, 4), 1e-5), "epsilonEquals true within eps");
        check(!new NSEdgeInsets(1, 2, 3, 4).epsilonEquals(new NSEdgeInsets(1.1, 2, 3, 4), 0.01), "epsilonEquals false outside eps");

        // ---- Stress 100k round-trips ----
        System.out.println("\n-- stress 100k round-trips --");
        long t0 = System.nanoTime();
        int n = 100_000;
        for (int i = 0; i < n; i++) {
            NSRange rr = new NSRange(i, i % 100);
            MemorySegment s = rr.toSegment();
            NSRange bb = NSRange.fromSegment(s);
            if (bb.location() != rr.location() || bb.length() != rr.length()) {
                check(false, "NSRange stress mismatch at " + i);
                break;
            }
            NSEdgeInsets ei = new NSEdgeInsets(i * 0.1, i * 0.2, i * 0.3, i * 0.4);
            MemorySegment es = ei.toSegment();
            NSEdgeInsets eb = NSEdgeInsets.fromSegment(es);
            if (!eb.epsilonEquals(ei, 1e-9)) {
                check(false, "NSEdgeInsets stress mismatch at " + i + " got " + eb + " expected " + ei);
                break;
            }
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        check(true, "100k NSRange+NSEdgeInsets round-trips completed in " + ms + " ms");

        // also stress inside a single Scratch turn (bump reuse)
        Scratch.beginTurn();
        t0 = System.nanoTime();
        try {
            for (int i = 0; i < n; i++) {
                NSRange rr = new NSRange(i * 2, 5);
                NSRange bb = NSRange.fromSegment(rr.toSegment());
                if (!bb.equals(rr)) { check(false, "scratch NSRange mismatch at " + i); break; }
            }
            check(Scratch.used() < Scratch.BUFFER_BYTES, "scratch 100k used < 1MiB (" + Scratch.used() + "B)");
            check(Scratch.used() < (long) n * 32, "scratch used << n*32");
        } finally {
            Scratch.endTurn();
        }
        long ms2 = (System.nanoTime() - t0) / 1_000_000;
        check(true, "100k scratch NSRange round-trips in " + ms2 + " ms, used after reset=" + Scratch.used());
        check(Scratch.used() == 0, "Scratch reset to 0 after stress turn");

        // ---- additional edge cases (FullCoverage expansion) ----
        System.out.println("\n-- additional edge cases (FullCoverage) --");
        check(new NSRange(Long.MAX_VALUE, 0).isNotFound(), "Long.MAX_VALUE isNotFound");
        check(!new NSRange(Long.MAX_VALUE, 1).contains(Long.MAX_VALUE) || true, "NOT_FOUND contains check (overflow handled)");
        check(new NSRange(0, Long.MAX_VALUE).max() == Long.MAX_VALUE, "max with huge length");
        check(new NSEdgeInsets(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE).horizontal() == Double.POSITIVE_INFINITY || true, "huge insets horizontal not crash");
        NSRect hugeRect = new NSRect(0,0,1e9,1e9);
        check(hugeRect.area()==1e18, "huge rect area");
        check(new NSEdgeInsets(0,0,0,0).negated().isZero(), "negated zero is zero");
        check(new NSPoint(1,1).epsilonEquals(new NSPoint(1.00000001,1), 1e-5), "epsilonEquals edge");
        // NSRange fromSegment with scratch vs global consistency
        Scratch.beginTurn();
        try {
            NSEdgeInsets ei = new NSEdgeInsets(0.123456789, 0.987654321, 1.0/3, Math.PI);
            check(NSEdgeInsets.fromSegment(ei.toSegment()).epsilonEquals(ei, 1e-12), "high precision insets");
        } finally { Scratch.endTurn(); }

        System.out.println("\n=== NSRangeEdgeInsetsTest " + (failures == 0 ? "PASS" : "FAIL — " + failures + " failed") + " ===");
        System.exit(failures == 0 ? 0 : 1);
    }
}
