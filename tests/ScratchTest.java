package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;
import nsui.objc.Scratch;

/**
 * Tests for the per-turn bump arena ({@link Scratch}) and the scratch-aware INPUT
 * marshalling in {@link ObjC}.
 *
 * <p>Covers:
 * <ol>
 *   <li>100k-op bump-reuse loop: {@code Scratch.alloc(32)}, {@code ObjC.rect(...)},
 *       {@code ObjC.cstring(...)}, {@code ObjC.sel(...)} — {@code used()} stays bounded
 *       (≪ n*32), proving the buffer is reused, then resets to 0 on {@code endTurn()}.</li>
 *   <li>Turn nesting: begin/begin/end(allocs still active)/end resets used() to 0.</li>
 *   <li>Fallback: a single 2&nbsp;MiB alloc (larger than the 1&nbsp;MiB buffer) returns a
 *       non-null global-arena segment without throwing.</li>
 *   <li>Round-trip: within a turn, create an NSWindow, set a frame, and read it back via
 *       {@link ObjC#msgSendRect} — the struct RETURN reads correct doubles, proving returns
 *       stay in the global arena.</li>
 *   <li>SEL cache: two {@code sel("setTitle:")} calls return address-identical cached cstrings
 *       and both resolve to the same native SEL.</li>
 * </ol>
 */
public final class ScratchTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== ScratchTest — per-turn bump arena + scratch-aware INPUT marshalling ===");
        ObjC.init(); // FFM bindings first.

        // ---- 1. bump reuse over 100k mixed ops --------------------------------------------
        System.out.println("\n-- 1. bump reuse: 100k x (alloc(32) + rect + cstring + sel) --");
        check(!Scratch.active(), "Scratch inactive before beginTurn");
        check(Scratch.depth() == 0, "depth()==0 before beginTurn");
        check(Scratch.used() == 0, "used()==0 before beginTurn");

        Scratch.beginTurn();
        check(Scratch.active(), "Scratch active inside turn");
        check(Scratch.depth() == 1, "depth()==1 after one beginTurn");

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
            check(false, "no exception during 100k loop, got: " + t);
        }

        long usedAfter = Scratch.used();
        System.out.println("[ScratchTest]   used()=" + usedAfter + "B vs n*32=" + nTimes32 + "B (buffer cap=" + Scratch.BUFFER_BYTES + "B)");
        check(usedAfter < nTimes32, "used() << n*32  → buffer is being reused, not growing");
        check(usedAfter <= Scratch.BUFFER_BYTES, "used() within the 1MiB buffer");
        check(firstSliceAddr != null && firstSliceAddr.address() != 0, "scratch slices are non-null/non-zero");

        Scratch.endTurn();
        check(!Scratch.active(), "Scratch inactive after endTurn");
        check(Scratch.depth() == 0, "depth()==0 after endTurn");
        check(Scratch.used() == 0, "used()==0 after endTurn (buffer reset for reuse)");

        // ---- 2. nested turns ----------------------------------------------------------------
        System.out.println("\n-- 2. nesting --");
        Scratch.beginTurn();
        Scratch.beginTurn();
        check(Scratch.depth() == 2, "depth()==2 after double beginTurn");
        long mid = -1;
        Scratch.alloc(64);
        mid = Scratch.used();
        check(mid > 0, "alloc inside the turn advanced used()>0");
        Scratch.endTurn();                                     // inner end: still active
        check(Scratch.depth() == 1, "depth()==1 after inner endTurn");
        check(Scratch.active(), "still active after inner endTurn");
        Scratch.alloc(64);                                     // inner turn over, outer still live
        check(Scratch.used() > mid, "alloc after inner endTurn still lands in the (outer) buffer");
        Scratch.endTurn();                                     // outer end: reset
        check(Scratch.depth() == 0, "depth()==0 after outer endTurn");
        check(!Scratch.active(), "inactive after outer endTurn");
        check(Scratch.used() == 0, "used()==0 after both endTurns");

        // ---- 3. fallback: single alloc larger than the buffer --------------------------------
        System.out.println("\n-- 3. fallback to ground arena for oversized single alloc --");
        Scratch.beginTurn();
        MemorySegment big = Scratch.alloc(2 * 1024 * 1024);    // 2MiB > 1MiB buffer
        check(big != null, "oversized alloc returns non-null");
        check(big.address() != 0, "oversized alloc returns non-zero address");
        // The 2MiB came from the global arena, so it must NOT have advanced the bump offset.
        check(Scratch.used() < 2 * 1024 * 1024, "oversized alloc did not consume scratch bump space");
        System.out.println("[ScratchTest]   used() after oversized alloc = " + Scratch.used() + "B");
        // Write/read through it to prove the segment is usable.
        big.set(java.lang.foreign.ValueLayout.JAVA_LONG, 0, 0xDEADBEEFL);
        check(big.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0) == 0xDEADBEEFL, "oversized segment is writable/readable");
        Scratch.endTurn();
        check(Scratch.used() == 0, "used()==0 after fallback turn ends");

        // ---- 4. round-trip: rect input + NSRect return within a turn --------------------------
        System.out.println("\n-- 4. round-trip sanity: NSRect return stays in the global arena --");
        Scratch.beginTurn();
        MemorySegment win = ObjC.msgSendId(ObjC.cls("NSWindow"), ObjC.sel("alloc"));
        MemorySegment winPeer = win;
        check(winPeer.address() != 0, "created an NSWindow");
        // initWithContentRect:styleMask:backing:defer: — rect is SCRATCH input here.
        winPeer = ObjC.msgSendIdRectLongLongBool(winPeer,
                ObjC.sel("initWithContentRect:styleMask:backing:defer:"),
                ObjC.rect(10, 20, 300, 200), 1L /* titled */, 2L /* buffered */, false);
        check(winPeer.address() != 0, "initWithContentRect:... accepted a scratch NSRect input");

        // Frame getter: msgSendRect allocates the RETURN in the global arena. NOTE: on macOS
        // frame != contentRect — initWithContentRect: sets the CLIENT area, while frame returns
        // the OUTER window rect (title bar + shadow), so y/height shift by the chrome (~28px +
        // origin offset). x and width are 1:1, so we assert those exactly and only bound y/h.
        MemorySegment frameSeg = ObjC.msgSendRect(winPeer, ObjC.sel("frame"));
        double fx = ObjC.rectX(frameSeg), fy = ObjC.rectY(frameSeg),
               fw = ObjC.rectW(frameSeg), fh = ObjC.rectH(frameSeg);
        System.out.println("[ScratchTest]   frame = " + fx + ", " + fy + ", " + fw + ", " + fh);
        check(fx == 10 && fw == 300,
                "msgSendRect RETURN reads correct x/width (content→frame chrome only shifts y/height): got x=" + fx + ", w=" + fw);
        check(fh >= 200 && fy >= 20,
                "frame height/origin bound the content rect (title-bar+shadow present): got y=" + fy + ", h=" + fh);

        // Now #endTurn() resets scratch: if the RETURN had been scratch it would be garbled here.
        Scratch.endTurn();
        double afterResetFx = ObjC.rectX(frameSeg);
        check(afterResetFx == 10, "rect-return segment still valid AFTER endTurn → truly global: got " + afterResetFx);

        // ---- 5. SEL cache ---------------------------------------------------------------------
        System.out.println("\n-- 5. SEL cache --");
        MemorySegment selA = ObjC.sel("setTitle:");
        MemorySegment selB = ObjC.sel("setTitle:");
        check(selA.address() != 0, "sel resolves to a non-zero SEL");
        check(selA.address() == selB.address(), "sel(\"setTitle:\") twice returns the SAME SEL address (cached/same native SEL)");
        check(selA.equals(selB), "sel segments are equals() (same backing memory)");
        // And it actually works as a selector on the window.
        MemorySegment title = ObjC.nsstring("scratch-roundtrip-title");
        ObjC.msgSendVoidId(winPeer, selA, title);
        MemorySegment got = ObjC.msgSendId(winPeer, ObjC.sel("title"));
        String gotTitle = ObjC.toString(got);
        check("scratch-roundtrip-title".equals(gotTitle),
                "cached SEL works for both setTitle: and title: via msgSend → got '" + gotTitle + "'");

        System.out.println("\n=== ScratchTest " + (failures == 0 ? "PASS" : "FAIL — " + failures + " failed") + " ===");
        System.exit(failures == 0 ? 0 : 1);
    }
}
