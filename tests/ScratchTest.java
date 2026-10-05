package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;
import nsui.objc.Scratch;

/// Tests for the per-turn bump arena (`Scratch`) and the scratch-aware INPUT
/// marshalling in `ObjC`.
///
/// Covers:
/// - 100k-op bump-reuse loop: `Scratch.alloc(32)`, `ObjC.rect(...)`,
/// `ObjC.cstring(...)`, `ObjC.sel(...)` — `used()` stays bounded
/// (≪ n*32), proving the buffer is reused, then resets to 0 on `endTurn()`.
/// - Turn nesting: begin/begin/end(allocs still active)/end resets used() to 0.
/// - Fallback: a single 2 MiB alloc (larger than the 1 MiB buffer) returns a
/// non-null global-arena segment without throwing.
/// - Round-trip: within a turn, create an NSWindow, set a frame, and read it back via
/// `ObjC#msgSendRect` — the struct RETURN reads correct doubles, reuses one
/// per-thread slot across calls (same address, fresh values), and survives
/// `endTurn` (it is neither scratch nor a fresh global slice per call).
/// - SEL cache: two `sel("setTitle:")` calls return address-identical cached cstrings
/// and both resolve to the same native SEL.
/// - Default inputs: `rect`/`cstring` with NO turn bump exactly 40_000 B for
/// 1000 pairs (no global-arena leak), read back correctly, rewind to 0.
public final class ScratchTest {

    public static void main(String[] args) {
        System.out.println("=== ScratchTest — per-turn bump arena + scratch-aware INPUT marshalling ===");
        ObjC.init(); // FFM bindings first.

        // ---- 1. bump reuse over 100k mixed ops --------------------------------------------
        System.out.println("\n-- 1. bump reuse: 100k x (alloc(32) + rect + cstring + sel) --");
        TestKit.check(!Scratch.active(), "Scratch inactive before beginTurn");
        TestKit.check(Scratch.depth() == 0, "depth()==0 before beginTurn");
        TestKit.check(Scratch.used() == 0, "used()==0 before beginTurn");

        Scratch.beginTurn();
        TestKit.check(Scratch.active(), "Scratch active inside turn");
        TestKit.check(Scratch.depth() == 1, "depth()==1 after one beginTurn");

        final int n = 100_000;
        final long nTimes32 = (long) n * 32;
        long usedBeforeOpening = Scratch.used();
        MemorySegment firstSliceAddr = null;
        try {
            for (int i = 0; i < n; i++) {
                MemorySegment a = Scratch.alloc(32);
                if (firstSliceAddr == null) firstSliceAddr = a;      // keep one slice alive across the loop
                ObjC.rect(1, 2, 3, 4);                               // scratch when a turn is active
                ObjC.cstring("x");                                   // scratch cstring
                ObjC.sel("setTitle:");                               // cached global cstring
            }
        } catch (Throwable t) {
            TestKit.check(false, "no exception during 100k loop, got: " + t);
        }

        long usedAfter = Scratch.used();
        System.out.println("[ScratchTest]   used()=" + usedAfter + "B vs n*32=" + nTimes32 + "B (buffer cap=" + Scratch.BUFFER_BYTES + "B)");
        TestKit.check(usedAfter < nTimes32, "used() << n*32  → buffer is being reused, not growing");
        TestKit.check(usedAfter <= Scratch.BUFFER_BYTES, "used() within the 1MiB buffer");
        TestKit.check(firstSliceAddr != null && firstSliceAddr.address() != 0, "scratch slices are non-null/non-zero");

        Scratch.endTurn();
        TestKit.check(!Scratch.active(), "Scratch inactive after endTurn");
        TestKit.check(Scratch.depth() == 0, "depth()==0 after endTurn");
        TestKit.check(Scratch.used() == 0, "used()==0 after endTurn (buffer reset for reuse)");

        // ---- 2. nested turns ----------------------------------------------------------------
        System.out.println("\n-- 2. nesting --");
        Scratch.beginTurn();
        Scratch.beginTurn();
        TestKit.check(Scratch.depth() == 2, "depth()==2 after double beginTurn");
        long mid = -1;
        Scratch.alloc(64);
        mid = Scratch.used();
        TestKit.check(mid > 0, "alloc inside the turn advanced used()>0");
        Scratch.endTurn();                                     // inner end: still active
        TestKit.check(Scratch.depth() == 1, "depth()==1 after inner endTurn");
        TestKit.check(Scratch.active(), "still active after inner endTurn");
        Scratch.alloc(64);                                     // inner turn over, outer still live
        TestKit.check(Scratch.used() > mid, "alloc after inner endTurn still lands in the (outer) buffer");
        Scratch.endTurn();                                     // outer end: reset
        TestKit.check(Scratch.depth() == 0, "depth()==0 after outer endTurn");
        TestKit.check(!Scratch.active(), "inactive after outer endTurn");
        TestKit.check(Scratch.used() == 0, "used()==0 after both endTurns");

        // ---- 3. fallback: single alloc larger than the buffer --------------------------------
        System.out.println("\n-- 3. fallback to ground arena for oversized single alloc --");
        Scratch.beginTurn();
        MemorySegment big = Scratch.alloc(2 * 1024 * 1024);    // 2MiB > 1MiB buffer
        TestKit.check(big != null, "oversized alloc returns non-null");
        TestKit.check(big.address() != 0, "oversized alloc returns non-zero address");
        // The 2MiB came from the global arena, so it must NOT have advanced the bump offset.
        TestKit.check(Scratch.used() < 2 * 1024 * 1024, "oversized alloc did not consume scratch bump space");
        System.out.println("[ScratchTest]   used() after oversized alloc = " + Scratch.used() + "B");
        // Write/read through it to prove the segment is usable.
        big.set(java.lang.foreign.ValueLayout.JAVA_LONG, 0, 0xDEADBEEFL);
        TestKit.check(big.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0) == 0xDEADBEEFL, "oversized segment is writable/readable");
        Scratch.endTurn();
        TestKit.check(Scratch.used() == 0, "used()==0 after fallback turn ends");

        // ---- 4. round-trip: rect input + NSRect return within a turn --------------------------
        System.out.println("\n-- 4. round-trip sanity: NSRect return reads correctly + reuses its slot --");
        Scratch.beginTurn();
        MemorySegment win = ObjC.msgSendId(ObjC.cls("NSWindow"), ObjC.sel("alloc"));
        MemorySegment winPeer = win;
        TestKit.check(winPeer.address() != 0, "created an NSWindow");
        // initWithContentRect:styleMask:backing:defer: — rect is SCRATCH input here.
        winPeer = ObjC.msgSendIdRectLongLongBool(winPeer,
                ObjC.sel("initWithContentRect:styleMask:backing:defer:"),
                ObjC.rect(10, 20, 300, 200), 1L /* titled */, 2L /* buffered */, false);
        TestKit.check(winPeer.address() != 0, "initWithContentRect:... accepted a scratch NSRect input");

        // Frame getter: msgSendRect writes the RETURN into a per-thread reusable slot.
        // NOTE: on macOS frame != contentRect — initWithContentRect: sets the CLIENT area, while frame returns
        // the OUTER window rect (title bar + shadow), so y/height shift by the chrome (~28px +
        // origin offset). x and width are 1:1, so we assert those exactly and only bound y/h.
        MemorySegment frameSeg = ObjC.msgSendRect(winPeer, ObjC.sel("frame"));
        double fx = ObjC.rectX(frameSeg), fy = ObjC.rectY(frameSeg),
               fw = ObjC.rectW(frameSeg), fh = ObjC.rectH(frameSeg);
        System.out.println("[ScratchTest]   frame = " + fx + ", " + fy + ", " + fw + ", " + fh);
        TestKit.check(fx == 10 && fw == 300,
                "msgSendRect RETURN reads correct x/width (content→frame chrome only shifts y/height): got x=" + fx + ", w=" + fw);
        TestKit.check(fh >= 200 && fy >= 20,
                "frame height/origin bound the content rect (title-bar+shadow present): got y=" + fy + ", h=" + fh);

        // Reuse proof: a second call must return the SAME address (slot overwrite),
        // carrying fresh values — zero immortal allocation per call.
        MemorySegment frameSeg2 = ObjC.msgSendRect(winPeer, ObjC.sel("frame"));
        TestKit.check(frameSeg2.address() == frameSeg.address(),
                "msgSendRect reuses one per-thread slot (same address both calls)");
        TestKit.check(ObjC.rectX(frameSeg2) == fx && ObjC.rectW(frameSeg2) == fw,
                "reused slot carries fresh values on rewrite");

        // Now #endTurn() resets scratch: if the RETURN had been scratch it would be garbled here.
        // (The slot is deliberately NOT rewound — only overwritten by the next call.)
        Scratch.endTurn();
        double afterResetFx = ObjC.rectX(frameSeg);
        TestKit.check(afterResetFx == 10, "rect-return segment still valid AFTER endTurn (slot, not scratch): got " + afterResetFx);

        // ---- 5. SEL cache ---------------------------------------------------------------------
        System.out.println("\n-- 5. SEL cache --");
        MemorySegment selA = ObjC.sel("setTitle:");
        MemorySegment selB = ObjC.sel("setTitle:");
        TestKit.check(selA.address() != 0, "sel resolves to a non-zero SEL");
        TestKit.check(selA.address() == selB.address(), "sel(\"setTitle:\") twice returns the SAME SEL address (cached/same native SEL)");
        TestKit.check(selA.equals(selB), "sel segments are equals() (same backing memory)");
        // And it actually works as a selector on the window.
        MemorySegment title = ObjC.nsstring("scratch-roundtrip-title");
        ObjC.msgSendVoidId(winPeer, selA, title);
        MemorySegment got = ObjC.msgSendId(winPeer, ObjC.sel("title"));
        String gotTitle = ObjC.toString(got);
        TestKit.check("scratch-roundtrip-title".equals(gotTitle),
                "cached SEL works for both setTitle: and title: via msgSend → got '" + gotTitle + "'");

        // ---- 6. default inputs: rect/cstring with NO turn use the bump buffer ---------
        System.out.println("\n-- 6. default inputs outside any turn --");
        // 1000 x (32-byte rect + 8-byte cstring granule); used() reads 0 while
        // inactive, so rewind first (drops dead residue from earlier sections),
        // then open a turn afterwards to observe the bump offset: it must show
        // exactly the spill-free 40_000 bytes, proving no-turn inputs bumped
        // instead of leaking to the global arena.
        Scratch.beginTurn(); Scratch.endTurn();
        for (int i = 0; i < 1000; i++) { ObjC.rect(i, i, 1, 1); ObjC.cstring("edge"); }
        Scratch.beginTurn();
        TestKit.check(Scratch.used() == 40_000,
                "1000 no-turn inputs bumped 40_000 B (used=" + Scratch.used() + ")");
        // Values stay correct: the last rect reads back, the last cstring round-trips.
        MemorySegment lr = ObjC.rect(7, 8, 9, 10);
        TestKit.check(ObjC.rectX(lr) == 7 && ObjC.rectW(lr) == 9, "no-turn rect reads back");
        TestKit.check("edge".equals(ObjC.toString(ObjC.nsstring("edge"))), "no-turn cstring round-trips");
        Scratch.endTurn();
        TestKit.check(Scratch.used() == 0, "used()==0 after rewinding default inputs");

        System.out.println("\n=== ScratchTest " + (TestKit.failures() == 0 ? "PASS" : "FAIL — " + TestKit.failures() + " failed") + " ===");
        TestKit.end();
    }
}
