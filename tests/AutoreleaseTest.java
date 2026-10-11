package nsui.tests;

import nsui.NSData;
import nsui.NSPoint;
import nsui.NSString;
import nsui.NSValue;
import nsui.objc.Autorelease;
import nsui.objc.ObjC;

import java.lang.foreign.MemorySegment;

public class AutoreleaseTest {

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

        // 6. Lifetime contract: a wrapper around an autoreleased factory result is
        //    valid only until the enclosing pool drains. keepAlive() must retain it
        //    so it survives; WITHOUT the retain the peer is freed and the next
        //    message send is a use-after-free (the ce67abc regression).
        //    Only the positive direction is asserted here: reading a freed peer is
        //    undefined behaviour, so the test must never do it deliberately.
        NSData keptData;
        NSString keptString;
        NSValue keptValue;
        MemorySegment lifetimePool = Autorelease.push();
        try {
            keptData = NSData.dataWithBytes(new byte[]{1, 2, 3, 4}).keepAlive();
            keptString = NSString.stringWithUTF8String("kept across drain").keepAlive();
            keptValue = NSValue.valueWithPoint(new NSPoint(3, 4)).keepAlive();
        } finally {
            Autorelease.pop(lifetimePool);
        }
        if (keptData.length() != 4) {
            fail("keepAlive() NSData did not survive the pool drain (len=" + keptData.length() + ")");
        } else {
            System.out.println("[AutoreleaseTest] keepAlive() NSData survives the pool drain (len=4)");
        }
        if (!"kept across drain".equals(keptString.string())) {
            fail("keepAlive() NSString did not survive the pool drain");
        } else {
            System.out.println("[AutoreleaseTest] keepAlive() NSString survives the pool drain");
        }
        NSPoint keptPoint = keptValue.pointValue();
        if (keptPoint.x() != 3 || keptPoint.y() != 4) {
            fail("keepAlive() NSValue did not survive the pool drain (" + keptPoint + ")");
        } else {
            System.out.println("[AutoreleaseTest] keepAlive() NSValue survives the pool drain");
        }

        TestKit.end();
    }

    private static void fail(String msg) { TestKit.check(false, msg); }
}
