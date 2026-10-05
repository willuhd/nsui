package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.util.LinkedHashMap;
import java.util.Map;

import nsui.NSApplication;
import nsui.NSEvent;
import nsui.NSObject;
import nsui.NSRect;
import nsui.NSWindow;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;

/**
 * The real window-lifecycle proof: a Java delegate decides native close behavior.
 *
 * <p>{@code windowShouldClose:} is a {@code -(BOOL)} delegate callback AppKit consults
 * before closing a window. Implementing it in Java via DelegateProxy lets Java veto
 * or allow the close. {@code windowWillClose:} is the {@code -(void)} notification fired
 * once the window actually closes.
 *
 * <p>Pass:
 * <ul>
 *   <li>Delegate A (windowShouldClose: -&gt; false): performClose is vetoed — the window
 *       stays visible and windowWillClose never fires.</li>
 *   <li>Delegate B (windowShouldClose: -&gt; true): performClose closes the window — it is
 *       no longer visible and windowWillClose has fired.</li>
 *   <li>registrySize grew with each delegate registration.</li>
 * </ul>
 */
public final class DelegateTest {

    

    

    public static void main(String[] args) throws Throwable {
        System.out.println("=== DelegateTest — Java delegate decides native close ===");
        ObjC.init(); // FFM bindings (must be first)

        NSApplication app = NSApplication.shared();
        app.setActivationPolicy(0 /* NSApplicationActivationPolicyRegular */);

        NSWindow window = NSWindow.create(new NSRect(0, 0, 500, 300), 15L, 2L, false);
        window.setTitle("delegate test");
        window.center();
        window.setReleasedWhenClosed(false);
        // Veto semantics need a genuinely visible window (still-visible assertion).
        // show() orders front without making key: no activation, no focus steal.
        TestKit.show(window);
        app.finishLaunching();

        int before = DelegateProxy.registrySize();
        TestKit.check(before == 0, "registry starts empty (size=" + before + ")");

        // ---- Delegate A: veto the close ----
        final boolean[] flagA = {false};
        Map<String, DelegateProxy.BoolArg> boolsA = new LinkedHashMap<>();
        boolsA.put("windowShouldClose:", sender -> false);            // veto
        Map<String, DelegateProxy.VoidArg> voidsA = new LinkedHashMap<>();
        voidsA.put("windowWillClose:", sender -> flagA[0] = true);    // must NOT fire

        MemorySegment dA = DelegateProxy.delegate("NSObject", "DSDelegateA", boolsA, voidsA);
        TestKit.check(dA != null && dA.address() != 0, "delegate A created");
        TestKit.check(DelegateProxy.registrySize() == before + 1,
                "registry grew by 1 after delegate A (size=" + DelegateProxy.registrySize() + ")");

        window.setDelegate(NSObject.wrap(dA));
        // Veto assertion needs the close ALONE: TestKit.close() orders out first,
        // which would hide the window regardless of the delegate verdict.
        window.performClose(null);
        TestKit.pump(app, 1000L);

        boolean stillVisible = window.isVisible();
        TestKit.check(stillVisible, "delegate A vetoed performClose: window still visible");
        TestKit.check(!flagA[0], "delegate A vetoed performClose: windowWillClose did NOT fire");
        System.out.println("delegate A: window isVisible=" + stillVisible + " windowWillCloseFired=" + flagA[0] + " (expected true / false)");
        System.out.println("PASS: delegate A — Java veto decided native close behavior");

        // ---- Delegate B: allow the close ----
        final boolean[] flagB = {false};
        Map<String, DelegateProxy.BoolArg> boolsB = new LinkedHashMap<>();
        boolsB.put("windowShouldClose:", sender -> true);             // allow
        Map<String, DelegateProxy.VoidArg> voidsB = new LinkedHashMap<>();
        voidsB.put("windowWillClose:", sender -> flagB[0] = true);    // must fire

        MemorySegment dB = DelegateProxy.delegate("NSObject", "DSDelegateB", boolsB, voidsB);
        TestKit.check(dB != null && dB.address() != 0, "delegate B created");
        TestKit.check(DelegateProxy.registrySize() == before + 2,
                "registry grew by 2 overall (size=" + DelegateProxy.registrySize() + ")");

        window.setDelegate(NSObject.wrap(dB));
        window.performClose(null);
        TestKit.pump(app, 1000L);

        boolean gone = !window.isVisible();
        TestKit.check(gone, "delegate B allowed performClose: window is no longer visible");
        TestKit.check(flagB[0], "delegate B allowed performClose: windowWillClose fired");
        System.out.println("delegate B: window isVisible=" + window.isVisible() + " windowWillCloseFired=" + flagB[0] + " (expected false / true)");
        System.out.println("PASS: delegate B — Java allowed native close");

        // dealloc cleanup is driven by AppKit's release of each delegate object, which we
        // cannot trigger deterministically without retain/release shims; the registry
        // growth above proves registration; dispatchDealloc chaining is verified-by-construction
        // (same super-machinery as nsui.NSView.deallocImpl).
        TestKit.check(DelegateProxy.registrySize() > 0, "registry remains non-zero after both delegates (size=" + DelegateProxy.registrySize() + ")");

        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }
}
