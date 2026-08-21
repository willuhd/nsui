package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.*;
import nsui.objc.Autorelease;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// PrintOperationTest — WIRING-LEVEL coverage for NSPrintOperation.
///
/// Deliberately NEVER calls `runOperation()`: with panels suppressed that
/// would send a real job to the default printer, and with panels shown it
/// would block on modal dialogs. Everything asserted here is object wiring:
///
/// - create(view, printInfo) non-nil + isKindOfClass NSPrintOperation
/// - view() round-trips the original peer; printInfo() returns AppKit's COPY
///   of the original (documented behavior) — checked by type + value, not peer
/// - showsPrintPanel/showsProgressPanel default true, then false/true round-trips
/// - currentOperation() null outside a running operation
/// - respondsToSelector guards for every selector used (instance + class side)
/// - wrap(null)/wrap(NULL) null-safety
/// - 100x create/drain stress loop inside autorelease pools
public final class PrintOperationTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    /// respondsToSelector: probe — works for instance methods on an object and
    /// for class methods when passed the Class object itself (the class is an
    /// instance of its metaclass).
    private static boolean responds(MemorySegment target, String selectorName) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(
                    target, ObjC.sel("respondsToSelector:"), ObjC.sel(selectorName));
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== PrintOperationTest ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            String m = String.valueOf(t.getMessage()).toLowerCase();
            if (m.contains("connection") || m.contains("dlopen") || m.contains("appkit")) {
                System.out.println("SKIP: ObjC.init failed (not macOS / connection error): " + t);
                System.out.println("RESULT: SKIP");
                System.exit(0);
            }
            System.out.println("FAIL: ObjC.init threw unexpected: " + t);
            t.printStackTrace(System.out);
            System.exit(1);
        }

        // AppKit shared application — best effort; print operation objects work headless.
        try {
            NSApplication app = NSApplication.shared();
            app.setActivationPolicy(0);
        } catch (Throwable t) {
            System.out.println("NOTE: NSApplication init failed (may be headless): " + t);
        }

        // ---------------- fixtures ----------------
        NSView view = null;
        NSPrintInfo info = null;
        try {
            view = NSView.create(new NSRect(0, 0, 200, 100), (ctx, dirty) -> { });
            check(view != null && view.peer().address() != 0, "fixture NSView.create non-nil");
            info = NSPrintInfo.create();
            check(info != null && info.peer().address() != 0, "fixture NSPrintInfo.create non-nil");
        } catch (Throwable t) {
            check(false, "fixture setup threw: " + t);
            t.printStackTrace(System.out);
            System.out.println(failures == 0 ? "RESULT: PASS (" + asserts + " assertions)" : "RESULT: FAIL (" + failures + " of " + asserts + " assertions failed)");
            System.exit(failures == 0 ? 0 : 1);
            return;
        }

        // ---------------- currentOperation outside any running operation ----------------
        try {
            NSPrintOperation cur = NSPrintOperation.currentOperation();
            check(cur == null, "currentOperation() null before any runOperation (got "
                    + (cur == null ? "null" : Long.toHexString(cur.peer().address())) + ")");
        } catch (Throwable t) {
            check(false, "currentOperation pre-check threw: " + t);
        }

        // ---------------- wrap(null) null-safety ----------------
        try {
            check(NSPrintOperation.wrap(null) == null, "wrap(null) returns null");
            check(NSPrintOperation.wrap(MemorySegment.NULL) == null, "wrap(MemorySegment.NULL) returns null");
        } catch (Throwable t) {
            check(false, "wrap null-safety threw: " + t);
        }

        // ---------------- respondsToSelector guards (every selector used) ----------------
        try {
            MemorySegment cls = ObjC.cls("NSPrintOperation");
            check(cls != null && cls.address() != 0, "ObjC class NSPrintOperation resolved non-nil");

            // class-side selectors (probed on the Class -> metaclass lookup)
            check(responds(cls, "printOperationWithView:printInfo:"), "+NSPrintOperation respondsToSelector: printOperationWithView:printInfo:");
            check(responds(cls, "currentOperation"), "+NSPrintOperation respondsToSelector: currentOperation");

            // instance-side selectors need an instance; create one first (also the main subject)
            NSPrintOperation op = NSPrintOperation.create(view, info);
            check(op != null && op.peer().address() != 0,
                    "create(view, printInfo) non-nil (peer=" + (op == null ? "null" : Long.toHexString(op.peer().address())) + ")");
            if (op != null) {
                check(op.isKindOfClass("NSPrintOperation"), "created op isKindOfClass NSPrintOperation");
                check(responds(op.peer(), "runOperation"), "-op respondsToSelector: runOperation (guarded, NOT invoked)");
                check(responds(op.peer(), "showsPrintPanel"), "-op respondsToSelector: showsPrintPanel");
                check(responds(op.peer(), "setShowsPrintPanel:"), "-op respondsToSelector: setShowsPrintPanel:");
                check(responds(op.peer(), "showsProgressPanel"), "-op respondsToSelector: showsProgressPanel");
                check(responds(op.peer(), "setShowsProgressPanel:"), "-op respondsToSelector: setShowsProgressPanel:");
                check(responds(op.peer(), "printInfo"), "-op respondsToSelector: printInfo");
                check(responds(op.peer(), "view"), "-op respondsToSelector: view");
            }

            // ---------------- view()/printInfo() round-trip ----------------
            if (op != null) {
                NSView gotView = op.view();
                check(gotView != null && gotView.peer().address() == view.peer().address(),
                        "view() round-trips original peer (got " + (gotView == null ? "null" : Long.toHexString(gotView.peer().address()))
                                + " expect " + Long.toHexString(view.peer().address()) + ")");
                NSPrintInfo gotInfo = op.printInfo();
                // AppKit COPIES the print info handed to create (documented
                // initWithView:printInfo: behavior), so a different peer is
                // expected; assert type + value round-trip instead of identity.
                check(gotInfo != null && gotInfo.isKindOfClass("NSPrintInfo"),
                        "printInfo() returns an NSPrintInfo (got " + (gotInfo == null ? "null" : Long.toHexString(gotInfo.peer().address()))
                                + ", original " + Long.toHexString(info.peer().address()) + ")");
                check(gotInfo != null && gotInfo.peer().address() != info.peer().address(),
                        "printInfo() is AppKit's copy, not the original peer (documented behavior)");
                check(gotInfo != null && gotInfo.orientation() == info.orientation(),
                        "printInfo copy preserves orientation (" + info.orientation() + " -> " + (gotInfo == null ? -1 : gotInfo.orientation()) + ")");

                // ---------------- panel flags: defaults then round-trips ----------------
                boolean dShow = op.showsPrintPanel();
                boolean dProg = op.showsProgressPanel();
                check(dShow, "showsPrintPanel default true (got " + dShow + ")");
                check(dProg, "showsProgressPanel default true (got " + dProg + ")");

                op.setShowsPrintPanel(false);
                check(!op.showsPrintPanel(), "setShowsPrintPanel(false) round-trip (got " + op.showsPrintPanel() + ")");
                op.setShowsPrintPanel(true);
                check(op.showsPrintPanel(), "setShowsPrintPanel(true) round-trip (got " + op.showsPrintPanel() + ")");

                op.setShowsProgressPanel(false);
                check(!op.showsProgressPanel(), "setShowsProgressPanel(false) round-trip (got " + op.showsProgressPanel() + ")");
                op.setShowsProgressPanel(true);
                check(op.showsProgressPanel(), "setShowsProgressPanel(true) round-trip (got " + op.showsProgressPanel() + ")");
            }
        } catch (Throwable t) {
            check(false, "create/accessor section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- 100x create/drain stress (never runs the job) ----------------
        try {
            AtomicInteger okCount = new AtomicInteger(0);
            final NSView stressView = view;
            final NSPrintInfo stressInfo = info;
            long viewAddr = view.peer().address();
            for (int i = 0; i < 100; i++) {
                Autorelease.run(() -> {
                    NSPrintOperation op = NSPrintOperation.create(stressView, stressInfo);
                    boolean ok = op != null
                            && op.peer().address() != 0
                            && op.isKindOfClass("NSPrintOperation")
                            && op.view() != null && op.view().peer().address() == viewAddr
                            && op.printInfo() != null && op.printInfo().isKindOfClass("NSPrintInfo")
                            && op.showsPrintPanel()
                            && op.showsProgressPanel();
                    if (ok) okCount.incrementAndGet();
                });
            }
            check(okCount.get() == 100, "stress loop 100x create/drain ops (ok=" + okCount.get() + "/100)");

            NSPrintOperation curAfter = NSPrintOperation.currentOperation();
            check(curAfter == null, "currentOperation() still null after stress without runOperation (got "
                    + (curAfter == null ? "null" : Long.toHexString(curAfter.peer().address())) + ")");
        } catch (Throwable t) {
            check(false, "stress section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(failures == 0
                ? "RESULT: PASS (" + asserts + " assertions)"
                : "RESULT: FAIL (" + failures + " of " + asserts + " assertions failed)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
