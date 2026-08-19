package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.objc.Dispatch;
import nsui.objc.ObjC;

/**
 * Integration test for the low-level libdispatch shim (nsui.objc.Dispatch) and the
 * ObjC block builder it relies on (nsui.objc.Blocks).
 *
 * <p>The main dispatch queue is drained by the main run loop, so the test's main
 * thread manually pumps the AppKit run loop the same way Main.java's smoke pump
 * does — calling nextEventMatchingMask:untilDate:inMode:dequeue: with a short countdown
 * interval — and breaks out early as soon as the expected latches fire.
 *
 * <p>Exit code: 0 if all three tests PASS, 1 otherwise.
 */
public class DispatchTest {

    private static long FAILED = 0;

    private static final long mainThreadId = Thread.currentThread().threadId();

    // Shared app + pump primitives.
    private static MemorySegment app;
    private static MemorySegment dateCls;
    private static MemorySegment selDate;
    private static MemorySegment selNextEvent;
    private static MemorySegment selSendEvent;
    private static MemorySegment selUpdateWindows;

    public static void main(String[] args) throws InterruptedException {
        ObjC.init();            // FFM bindings (must be first)
        Dispatch.ensureInit();

        app = ObjC.msgSendId(ObjC.cls("NSApplication"), ObjC.sel("sharedApplication"));
        dateCls = ObjC.cls("NSDate");
        selDate = ObjC.sel("dateWithTimeIntervalSinceNow:");
        selNextEvent = ObjC.sel("nextEventMatchingMask:untilDate:inMode:dequeue:");
        selSendEvent = ObjC.sel("sendEvent:");
        selUpdateWindows = ObjC.sel("updateWindows");

        System.out.println("[test] main thread id = " + mainThreadId
                + "  (blocks on the main queue must run on this thread)");

        test1_backgroundToMain();
        test2_ordering();
        test3_mainToMain();

        System.out.println(FAILED == 0
                ? "\nALL TESTS PASSED"
                : "\n" + FAILED + " TEST(S) FAILED");
        System.exit(FAILED == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------ pump

    /** Manual AppKit event pump: drain events until latch fires or timeout (in s). */
    private static void waitFor(CountDownLatch latch, int timeoutSeconds) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (latch.getCount() > 0) {
            if (System.nanoTime() > deadlineNanos) {
                System.out.println("[test]      timed out waiting on latch (count=" + latch.getCount() + ")");
                return;
            }
            MemorySegment until = ObjC.msgSendIdDouble(dateCls, selDate, 0.05);
            MemorySegment ev = ObjC.msgSendIdLongIdIdBool(
                    app, selNextEvent, -1L /* NSEventMaskAny */, until, ObjC.nsstring("kCFRunLoopDefaultMode"), true);
            if (ev != null && ev.address() != 0) {
                ObjC.msgSendVoidId(app, selSendEvent, ev);
            }
            ObjC.msgSendVoid(app, selUpdateWindows);
            Thread.sleep(10);
        }
    }

    // ----------------------------------------------------------------- tests

    /** Background thread -> Dispatch.onMain -> must run, once, on the main thread. */
    private static void test1_backgroundToMain() throws InterruptedException {
        System.out.println("[test1] background-to-main: enqueue a block from a background thread");
        AtomicInteger ranCount = new AtomicInteger(0);
        AtomicInteger ranOn = new AtomicInteger(-1);
        CountDownLatch latch = new CountDownLatch(1);

        Thread bg = new Thread(() ->
                Dispatch.onMain(() -> {
                    ranCount.incrementAndGet();
                    ranOn.set((int) Thread.currentThread().threadId());
                    latch.countDown();
                }), "dispatch-enqueuer");
        bg.start();
        bg.join(2000);

        waitFor(latch, 5);

        boolean ok = latch.getCount() == 0
                && ranCount.get() == 1
                && ranOn.get() == (int) mainThreadId;
        System.out.println((ok ? "PASS" : "FAIL") + " test1: latchFired=" + (latch.getCount() == 0)
                + " ranCount=" + ranCount.get()
                + " ranOn=" + ranOn.get() + " (expect " + mainThreadId + ")");
        if (!ok) FAILED++;
    }

    /** Two blocks from a background thread must run in FIFO order. */
    private static void test2_ordering() throws InterruptedException {
        System.out.println("[test2] ordering: two blocks enqueued from a background thread run in order");
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(2);

        Thread bg = new Thread(() -> {
            Dispatch.onMain(() -> { order.add("A"); latch.countDown(); });
            Dispatch.onMain(() -> { order.add("B"); latch.countDown(); });
        }, "dispatch-order");
        bg.start();
        bg.join(2000);

        waitFor(latch, 5);

        boolean ok = latch.getCount() == 0
                && order.size() == 2
                && "A".equals(order.get(0))
                && "B".equals(order.get(1));
        System.out.println((ok ? "PASS" : "FAIL") + " test2: order=" + order);
        if (!ok) FAILED++;
    }

    /** Enqueue from the main thread itself; it must still run exactly once. */
    private static void test3_mainToMain() throws InterruptedException {
        System.out.println("[test3] main-to-main: enqueue a block from the main thread");
        AtomicInteger ran = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Dispatch.onMain(() -> { ran.incrementAndGet(); latch.countDown(); });

        waitFor(latch, 5);

        boolean ok = latch.getCount() == 0 && ran.get() == 1;
        System.out.println((ok ? "PASS" : "FAIL") + " test3: latchFired=" + (latch.getCount() == 0) + " ran=" + ran.get());
        if (!ok) FAILED++;
    }
}
