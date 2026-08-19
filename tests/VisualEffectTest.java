package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSRect;
import nsui.NSVisualEffectView;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSVisualEffectView round-trip: isKindOfClass and material / blendingMode /
 * state / isEmphasized / maskingImage.
 *
 * <ul>
 *   <li>Creates a view via {@link NSVisualEffectView#create(NSRect)} and verifies
 *       it isKindOfClass NSVisualEffectView (and also NSView);</li>
 *   <li>Material round-trip: sets and reads back several NSVisualEffectMaterial values;</li>
 *   <li>Also verifies blendingMode, state, isEmphasized and maskingImage accessors
 *       do not crash and round-trip.</li>
 * </ul>
 *
 * All AppKit activity runs on the main thread ({@code -XstartOnFirstThread}).
 */
public final class VisualEffectTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== VisualEffectTest — isKindOfClass + material round-trip ===");
        ObjC.init();

        NSVisualEffectView view = NSVisualEffectView.create(new NSRect(0, 0, 200, 120));
        check(view != null && view.peer().address() != 0, "NSVisualEffectView.create non-nil");

        // ---- isKindOfClass ----
        boolean isVEV = view.isKindOfClass("NSVisualEffectView");
        check(isVEV, "isKindOfClass:NSVisualEffectView == YES [got " + isVEV + "]");

        // Also verify via direct ObjC handle (belt-and-suspenders) and that it is also an NSView
        try {
            var h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean isView = (boolean) h.invokeExact(view.peer(), ObjC.sel("isKindOfClass:"), ObjC.cls("NSView"));
            check(isView, "isKindOfClass:NSView == YES (inheritance) [got " + isView + "]");
            boolean notBox = !(boolean) h.invokeExact(view.peer(), ObjC.sel("isKindOfClass:"), ObjC.cls("NSBox"));
            check(notBox, "isKindOfClass:NSBox == NO [expected not a box]");
        } catch (Throwable t) {
            check(false, "isKindOfClass direct handle threw: " + t);
        }

        // Also check wrapper overload with MemorySegment
        boolean isVEV2 = view.isKindOfClass(ObjC.cls("NSVisualEffectView"));
        check(isVEV2, "isKindOfClass(NSVisualEffectView cls) overload == YES");

        // ---- material round-trip ----
        long origMaterial = view.material();
        System.out.println("  original material = " + origMaterial);

        long[] materials = {5L, 7L, 2L, 13L, 0L};
        for (long m : materials) {
            view.setMaterial(m);
            long got = view.material();
            check(got == m, "material round-trip set " + m + " -> got " + got);
        }
        // restore
        view.setMaterial(origMaterial);
        check(view.material() == origMaterial, "material restore to original " + origMaterial + " [got " + view.material() + "]");

        // ---- blendingMode round-trip ----
        long origBlend = view.blendingMode();
        System.out.println("  original blendingMode = " + origBlend);
        view.setBlendingMode(1L);
        check(view.blendingMode() == 1L, "blendingMode 1 (withinWindow) round-trip [got " + view.blendingMode() + "]");
        view.setBlendingMode(0L);
        check(view.blendingMode() == 0L, "blendingMode 0 (behindWindow) round-trip [got " + view.blendingMode() + "]");
        view.setBlendingMode(origBlend);

        // ---- state round-trip ----
        long origState = view.state();
        System.out.println("  original state = " + origState);
        view.setState(1L);
        check(view.state() == 1L, "state 1 (followsWindowActiveState) round-trip [got " + view.state() + "]");
        view.setState(2L);
        check(view.state() == 2L, "state 2 (active) round-trip [got " + view.state() + "]");
        view.setState(0L);
        check(view.state() == 0L, "state 0 (inactive) round-trip [got " + view.state() + "]");
        view.setState(origState);

        // ---- isEmphasized round-trip ----
        boolean origEmph = view.isEmphasized();
        System.out.println("  original isEmphasized = " + origEmph);
        view.setEmphasized(!origEmph);
        check(view.isEmphasized() == !origEmph, "isEmphasized toggle to " + !origEmph + " [got " + view.isEmphasized() + "]");
        view.setEmphasized(origEmph);
        check(view.isEmphasized() == origEmph, "isEmphasized restore to " + origEmph + " [got " + view.isEmphasized() + "]");

        // ---- maskingImage (nil by default, set/clear no crash) ----
        // Default is usually nil; reading should not throw.
        var img = view.maskingImage();
        System.out.println("  initial maskingImage = " + (img == null ? "nil" : img.peer()));
        // Clearing explicitly should keep nil
        view.setMaskingImage(null);
        check(view.maskingImage() == null || view.maskingImagePeer().address() == 0, "maskingImage nil after setMaskingImage(null)");
        // setting peer to NULL also no crash
        view.setMaskingImagePeer(MemorySegment.NULL);
        check(view.maskingImagePeer().address() == 0, "maskingImagePeer NULL after setMaskingImagePeer(NULL)");

        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
