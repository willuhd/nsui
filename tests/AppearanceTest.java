package nsui.tests;

import nsui.NSAppearance;
import nsui.NSRect;
import nsui.NSView;
import nsui.objc.ObjC;

/**
 * NSAppearance round-trip: appearanceNamed, currentAppearance, name,
 * view effectiveAppearance, and stress loop.
 *
 * <ul>
 *   <li>appearanceNamed("NSAppearanceNameAqua") non-null and name round-trip;</li>
 *   <li>appearanceNamed("NSAppearanceNameDarkAqua") non-null;</li>
 *   <li>currentAppearance non-null;</li>
 *   <li>NSView effectiveAppearance after setAppearance;</li>
 *   <li>Stress: 200 iterations appearanceNamed + name check.</li>
 * </ul>
 *
 * All AppKit activity runs on the main thread (-XstartOnFirstThread).
 */
public final class AppearanceTest {

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== AppearanceTest — NSAppearance round-trip + stress ===");
        ObjC.init();

        // ---- appearanceNamed Aqua ----
        NSAppearance aqua = NSAppearance.appearanceNamed("NSAppearanceNameAqua");
        check(aqua != null && aqua.peer().address() != 0, "appearanceNamed(\"NSAppearanceNameAqua\") non-null [got " + (aqua == null ? "null" : aqua.peer()) + "]");
        if (aqua != null) {
            String n = aqua.name();
            System.out.println("  aqua name = \"" + n + "\"");
            check("NSAppearanceNameAqua".equals(n), "aqua name round-trip == \"NSAppearanceNameAqua\" [got \"" + n + "\"]");
        }

        // ---- appearanceNamed DarkAqua ----
        NSAppearance dark = NSAppearance.appearanceNamed("NSAppearanceNameDarkAqua");
        check(dark != null && dark.peer().address() != 0, "appearanceNamed(\"NSAppearanceNameDarkAqua\") non-null [got " + (dark == null ? "null" : dark.peer()) + "]");
        if (dark != null) {
            String n = dark.name();
            System.out.println("  dark name = \"" + n + "\"");
            check("NSAppearanceNameDarkAqua".equals(n), "dark name round-trip == \"NSAppearanceNameDarkAqua\" [got \"" + n + "\"]");
            check(dark.isKindOfClass("NSAppearance"), "dark isKindOfClass:NSAppearance == YES");
        }

        // ---- currentAppearance ----
        NSAppearance cur = NSAppearance.currentAppearance();
        check(cur != null && cur.peer().address() != 0, "currentAppearance non-null [got " + (cur == null ? "null" : cur.peer()) + "]");
        if (cur != null) {
            String cn = cur.name();
            System.out.println("  currentAppearance name = \"" + cn + "\"");
            check(cn != null && !cn.isEmpty(), "currentAppearance name non-empty [got \"" + cn + "\"]");
            check(cur.isKindOfClass("NSAppearance"), "currentAppearance isKindOfClass:NSAppearance == YES");
        }

        // ---- wrap nil safety ----
        check(NSAppearance.wrap(null) == null, "NSAppearance.wrap(null) == null");
        check(NSAppearance.wrap(java.lang.foreign.MemorySegment.NULL) == null, "NSAppearance.wrap(NULL) == null");

        // ---- NSView effectiveAppearance after setAppearance ----
        NSView view = NSView.create(new NSRect(0, 0, 200, 120), (ctx, dirty) -> {});
        check(view != null && view.peer().address() != 0, "NSView.create for appearance test non-nil");

        if (aqua != null) {
            NSAppearance.setAppearance(view, aqua);
            NSAppearance eff = NSAppearance.effectiveAppearance(view);
            check(eff != null && eff.peer().address() != 0, "effectiveAppearance after setAppearance(aqua) non-null [got " + (eff == null ? "null" : eff.peer()) + "]");
            if (eff != null) {
                String en = eff.name();
                System.out.println("  effectiveAppearance name = \"" + en + "\"");
                // On macOS 14+, effectiveAppearance may resolve to a concrete appearance whose name contains Aqua
                // Accept either exact Aqua or containing Aqua (e.g. NSAppearanceNameAqua remains)
                boolean containsAqua = en != null && en.contains("Aqua");
                check(containsAqua, "effectiveAppearance name contains \"Aqua\" [got \"" + en + "\"]");
            }

            // Also test dark appearance assignment
            if (dark != null) {
                NSAppearance.setAppearance(view, dark);
                NSAppearance eff2 = NSAppearance.effectiveAppearance(view);
                check(eff2 != null, "effectiveAppearance after setAppearance(dark) non-null");
                if (eff2 != null) {
                    String en2 = eff2.name();
                    System.out.println("  effectiveAppearance(dark) name = \"" + en2 + "\"");
                    check(en2 != null && en2.contains("DarkAqua"), "effectiveAppearance(dark) name contains \"DarkAqua\" [got \"" + en2 + "\"]");
                }
            }

            // nil appearance reset should not crash
            try {
                NSAppearance.setAppearance(view, null);
                NSAppearance effNull = NSAppearance.effectiveAppearance(view);
                // Effective after nil may fall back to system; just ensure no crash and non-null
                check(effNull != null, "effectiveAppearance after setAppearance(null) non-null (fallback) [got " + (effNull == null ? "null" : effNull.name()) + "]");
            } catch (Throwable t) {
                check(false, "setAppearance(null) threw: " + t);
            }
        }

        // ---- Stress: 200 iterations ----
        System.out.println("  stress: 200x appearanceNamed + name ...");
        boolean stressOk = true;
        for (int i = 0; i < 200; i++) {
            NSAppearance a = NSAppearance.appearanceNamed("NSAppearanceNameAqua");
            if (a == null || a.peer().address() == 0) {
                stressOk = false;
                check(false, "stress iteration " + i + ": appearanceNamed returned nil");
                break;
            }
            String nm = a.name();
            if (!"NSAppearanceNameAqua".equals(nm)) {
                stressOk = false;
                check(false, "stress iteration " + i + ": name mismatch [got \"" + nm + "\"]");
                break;
            }
            // Alternate dark every 10 to exercise both paths
            if (i % 10 == 0) {
                NSAppearance d = NSAppearance.appearanceNamed("NSAppearanceNameDarkAqua");
                if (d == null || !"NSAppearanceNameDarkAqua".equals(d.name())) {
                    stressOk = false;
                    check(false, "stress iteration " + i + " dark variant failed");
                    break;
                }
            }
        }
        if (stressOk) {
            check(true, "stress 200 iterations appearanceNamed+name no crash");
        }

        System.out.println(failures == 0 ? "RESULT: ALL PASS" : "RESULT: " + failures + " FAILURE(S)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
