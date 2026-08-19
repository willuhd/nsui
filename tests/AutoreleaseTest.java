package nsui.tests;

import nsui.objc.Autorelease;
import nsui.objc.ObjC;

import java.lang.foreign.MemorySegment;

public class AutoreleaseTest {

    private static int failures;

    public static void main(String[] args) {
        ObjC.init();
        System.out.println("[AutoreleaseTest] begin");

        // 1. push() returns a non-null, non-zero token.
        MemorySegment token = Autorelease.push();
        if (token == null || token.address() == 0) {
            fail("push() returned a null/zero token");
        } else {
            System.out.println("[AutoreleaseTest] push token = " + token.address() + " (non-null, address != 0)");
        }

        // 2. Create autoreleased NSStrings inside a pool (stringWithUTF8String: is autoreleased).
        String before = null;
        MemorySegment t2 = Autorelease.push();
        try {
            MemorySegment s1 = ObjC.nsstring("hello");
            MemorySegment s2 = ObjC.nsstring("world");
            before = ObjC.toString(s1) + " " + ObjC.toString(s2);
        } finally {
            Autorelease.pop(t2);
        }
        if (!"hello world".equals(before)) {
            fail("autoreleased strings not created correctly: got '" + before + "'");
        } else {
            System.out.println("[AutoreleaseTest] autoreleased NSStrings inside pool: \"" + before + "\"");
        }

        // 3. run(Runnable) balances push/pop in finally.
        String[] after = new String[1];
        Autorelease.run(() -> {
            MemorySegment s = ObjC.nsstring("inside run");
            after[0] = ObjC.toString(s);
        });
        if (!"inside run".equals(after[0])) {
            fail("Autorelease.run body did not run: got '" + after[0] + "'");
        } else {
            System.out.println("[AutoreleaseTest] Autorelease.run body produced \"" + after[0] + "\"");
        }

        // 4. run(Runnable) does NOT swallow a body exception (pool still balanced).
        boolean caught = false;
        try {
            Autorelease.run(() -> {
                ObjC.nsstring("before throw");
                throw new IllegalStateException("body boom");
            });
        } catch (IllegalStateException e) {
            caught = "body boom".equals(e.getMessage());
        }
        if (!caught) {
            fail("Autorelease.run swallowed or mis-reported the body exception");
        } else {
            System.out.println("[AutoreleaseTest] Autorelease.run re-raised body exception -> true");
        }

        // 5. Stress: 100k push + autoreleased string + pop — must not crash or blow up memory.
        System.out.println("[AutoreleaseTest] stress: 100,000 push/nstring/pop rounds ...");
        long t0 = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            MemorySegment tok = Autorelease.push();
            try {
                ObjC.nsstring("x");
            } finally {
                Autorelease.pop(tok);
            }
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("[AutoreleaseTest] stress completed in " + ms + " ms, no crash / memory blowup");

        // Post-stress sanity: normal autoreleased work still works.
        String sanity = ObjC.toString(ObjC.nsstring("post-stress-ok"));
        if (!"post-stress-ok".equals(sanity)) {
            fail("post-stress sanity string mismatch");
        } else {
            System.out.println("[AutoreleaseTest] post-stress sanity ok");
        }

        if (failures > 0) {
            System.out.println("[AutoreleaseTest] FAIL — " + failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("[AutoreleaseTest] PASS");
        System.exit(0);
    }

    private static void fail(String msg) {
        failures++;
        System.out.println("[AutoreleaseTest] FAIL: " + msg);
    }
}
