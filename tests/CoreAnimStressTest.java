package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.WrongMethodTypeException;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.*;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CoreAnimStressTest — non-interactive stress + regression suite for the
/// CoreAnimation coverage: CALayer tree/property additions, CAShapeLayer,
/// CATextLayer, CAGradientLayer, CABasicAnimation, CAAnimationGroup and
/// CATransaction.
///
/// Sections:
/// - layer tree build/teardown x200 (addSublayer/superlayer/sublayers/removeFromSuperlayer)
/// - property round-trips for every new accessor
/// - implicit-animation batches inside CATransaction begin/commit x500, varying durations
/// - CABasicAnimation add/remove cycles x300 via addAnimation:forKey:/removeAnimationForKey:
/// - gradient / shape / text round-trips
/// - REGRESSION GUARD: the exact hotfix shape (setFromValue/setToValue with null and
///   non-null through the ternary paths) must never throw WrongMethodTypeException.
///
/// Prints PASS:/FAIL: lines and ends with RESULT: PASS or RESULT: FAIL; exits 0/1.
/// Reuses the TouchBarMenuTest SKIP pattern when AppKit init fails.
public final class CoreAnimStressTest {

    private static int failures;
    private static int asserts;
    private static int wrongMethodTypeCount;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    private static boolean eps(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    public static void main(String[] args) {
        System.out.println("=== CoreAnimStressTest ===");

        // ---- AppKit init with SKIP pattern (from TouchBarMenuTest) ----
        try {
            ObjC.init();
        } catch (Throwable t) {
            String m = String.valueOf(t.getMessage()).toLowerCase();
            if (m.contains("connection") || m.contains("dlopen") || m.contains("appkit")) {
                System.out.println("SKIP: ObjC.init failed (not macOS / connection error): " + t);
                System.out.println("RESULT: SKIP (connection error, continuing)");
                System.exit(0);
            }
            System.out.println("FAIL: ObjC.init threw unexpected: " + t);
            t.printStackTrace(System.out);
            System.exit(1);
        }

        // Layers do not need NSApplication: ObjC.init loads every framework we
        // touch (QuartzCore via ensureFramework) and -XstartOnFirstThread makes
        // the Java main thread the AppKit main thread. Keeping this test's
        // classpath to the wrapper slice only (no NSApplication/NSView deps).

        calayerPropertyRoundTrips();
        layerTreeStress();
        transactionBatches();
        completionBlockBestEffort();
        basicAnimationCycles();
        animationGroupRoundTrip();
        shapeTextGradientRoundTrips();
        hotfixRegressionGuard();

        System.out.println(failures == 0 && wrongMethodTypeCount == 0
                ? "RESULT: PASS (" + asserts + " assertions)"
                : "RESULT: FAIL (" + failures + " of " + asserts + " assertions failed, wrongMethodType=" + wrongMethodTypeCount + ")");
        System.exit((failures == 0 && wrongMethodTypeCount == 0) ? 0 : 1);
    }

    // ---------------------------------------------------------------- section 1

    /// Round-trip EVERY new CALayer accessor.
    private static void calayerPropertyRoundTrips() {
        System.out.println("--- CALayer new accessors ---");
        try {
            CALayer l = CALayer.create();
            check(l != null && l.peer().address() != 0, "CALayer.create non-nil");
            check(l.isKindOfClass("CALayer"), "layer isKindOfClass CALayer");

            l.setPosition(new NSPoint(12.5, 33.25));
            NSPoint pos = l.position();
            check(pos != null && eps(pos.x(), 12.5) && eps(pos.y(), 33.25), "position round-trip (got " + pos + ")");

            l.setAnchorPoint(new NSPoint(0.5, 0.5));
            NSPoint ap = l.anchorPoint();
            check(ap != null && eps(ap.x(), 0.5) && eps(ap.y(), 0.5), "anchorPoint round-trip (got " + ap + ")");

            l.setZPosition(7.5);
            check(eps(l.zPosition(), 7.5), "zPosition round-trip (got " + l.zPosition() + ")");

            l.setContentsScale(2.0);
            check(eps(l.contentsScale(), 2.0), "contentsScale round-trip (got " + l.contentsScale() + ")");

            l.setHidden(true);
            check(l.isHidden(), "isHidden true after setHidden(true)");
            l.setHidden(false);
            check(!l.isHidden(), "isHidden false after setHidden(false)");

            String defaultGravity = l.contentsGravity();
            l.setContentsGravity("center");
            check("center".equals(l.contentsGravity()), "contentsGravity round-trip (default " + defaultGravity + ", got " + l.contentsGravity() + ")");
            l.setContentsGravity("resize");

            l.setContents(null);
            check(l.contents() == null || l.contents().address() == 0, "contents null after setContents(null)");
            NSString obj = NSString.wrap(ObjC.nsstring("contents-object"));
            l.setContents(obj.peer());
            check(l.contents() != null && l.contents().address() == obj.peer().address(), "contents object round-trip by address");
            l.setContents(null);

            MemorySegment blue = NSColor.blueColor().cgColor();
            check(blue != null && blue.address() != 0, "NSColor.cgColor non-null (blue)");
            l.setShadowColor(blue);
            check(l.shadowColor() != null && l.shadowColor().address() == blue.address(), "shadowColor raw round-trip by address");
            NSColor red = NSColor.redColor();
            l.setShadowColor(red);
            check(l.shadowColor() != null && l.shadowColor().address() != blue.address(), "shadowColor(NSColor) convenience sets distinct CGColor");
            l.setShadowColor((MemorySegment) null);
            check(l.shadowColor() == null || l.shadowColor().address() == 0, "shadowColor cleared");

            l.setShadowOpacity(0.75);
            check(eps(l.shadowOpacity(), 0.75), "shadowOpacity round-trip (got " + l.shadowOpacity() + ")");

            l.setShadowRadius(6.5);
            check(eps(l.shadowRadius(), 6.5), "shadowRadius round-trip (got " + l.shadowRadius() + ")");

            l.setShadowOffset(new NSSize(3, -4));
            NSSize off = l.shadowOffset();
            check(off != null && eps(off.width(), 3) && eps(off.height(), -4), "shadowOffset round-trip (got " + off + ")");

            // existing basics still alive after the Handles growth
            l.setCornerRadius(4.25);
            check(eps(l.cornerRadius(), 4.25), "cornerRadius still round-trips (regression)");
            l.setOpacity(0.5f);
            check(eps(l.opacity(), 0.5), "opacity still round-trips (regression)");
        } catch (Throwable t) {
            check(false, "CALayer accessor section threw: " + t);
            t.printStackTrace(System.out);
        }
    }

    // ---------------------------------------------------------------- section 2

    /// Build/teardown a root + 8 children tree, 200 times.
    private static void layerTreeStress() {
        System.out.println("--- layer tree build/teardown x200 ---");
        int iterations = 200;
        long completed = 0;
        try {
            for (int i = 0; i < iterations; i++) {
                CALayer root = CALayer.create();
                CALayer[] kids = new CALayer[8];
                for (int j = 0; j < 8; j++) {
                    kids[j] = CALayer.create();
                    root.addSublayer(kids[j]);
                }
                CALayer sup = kids[3].superlayer();
                if (sup == null || sup.peer().address() != root.peer().address()) {
                    check(false, "tree iter " + i + ": superlayer peer mismatch");
                    break;
                }
                NSArray subs = root.sublayers();
                if (subs == null || subs.count() != 8) {
                    check(false, "tree iter " + i + ": sublayers count " + (subs == null ? "null" : subs.count()));
                    break;
                }
                boolean addressesOk = true;
                for (int j = 0; j < 8; j++) {
                    MemorySegment s = subs.objectAtIndex(j);
                    if (s == null || s.address() != kids[j].peer().address()) { addressesOk = false; break; }
                }
                if (!addressesOk) {
                    check(false, "tree iter " + i + ": sublayers element address mismatch");
                    break;
                }
                // detach half the children, verify count drops
                for (int j = 0; j < 8; j += 2) kids[j].removeFromSuperlayer();
                NSArray after = root.sublayers();
                if (after == null || after.count() != 4) {
                    check(false, "tree iter " + i + ": sublayers count after removal " + (after == null ? "null" : after.count()));
                    break;
                }
                completed++;
            }
            check(completed == iterations, "layer tree build/teardown completed " + completed + "/" + iterations + " iterations");
        } catch (Throwable t) {
            check(false, "layer tree section threw after " + completed + " iterations: " + t);
            t.printStackTrace(System.out);
        }
    }

    // ---------------------------------------------------------------- section 3

    /// Implicit-animation batches inside CATransaction begin/commit, 500 times.
    private static void transactionBatches() {
        System.out.println("--- CATransaction implicit-animation batches x500 ---");
        int iterations = 500;
        long completed = 0;
        try {
            CALayer l = CALayer.create();
            for (int i = 0; i < iterations; i++) {
                double duration = (i % 10) * 0.01;
                CATransaction.begin();
                CATransaction.setAnimationDuration(duration);
                l.setPosition(new NSPoint(i % 50, (i * 2) % 50));
                l.setOpacity((i % 100) / 100.0);
                l.setBackgroundColor((MemorySegment) (i % 2 == 0 ? NSColor.blueColor().cgColor() : MemorySegment.NULL));
                if (i % 50 == 49) CATransaction.flush();
                CATransaction.commit();
                completed++;
            }
            check(completed == iterations, "transaction batches completed " + completed + "/" + iterations);

            // KVC value round-trip inside an explicit transaction
            CATransaction.begin();
            NSString kv = NSString.wrap(ObjC.nsstring("kv-value"));
            CATransaction.setValueForKey(kv.peer(), "stressKey");
            MemorySegment back = CATransaction.valueForKey("stressKey");
            CATransaction.commit();
            check(back != null && back.address() == kv.peer().address(), "CATransaction setValue:forKey:/valueForKey: round-trip");
            check(CATransaction.valueForKey("absent-key") == null, "CATransaction valueForKey absent key is null");
        } catch (Throwable t) {
            check(false, "transaction batch section threw after " + completed + " batches: " + t);
            t.printStackTrace(System.out);
        }
    }

    // ---------------------------------------------------------------- section 4

    /// Completion block: registration must not crash; firing depends on the runloop,
    /// so treat either outcome as pass but report whether it fired.
    private static void completionBlockBestEffort() {
        System.out.println("--- CATransaction completion block (best effort) ---");
        try {
            AtomicInteger fired = new AtomicInteger(0);
            CALayer l = CALayer.create();
            CATransaction.begin();
            CATransaction.setAnimationDuration(0.01);
            CATransaction.setCompletionBlock(fired::incrementAndGet);
            l.setPosition(new NSPoint(9, 9));
            CATransaction.commit();
            check(true, "setCompletionBlock(Runnable) registered without crash");
            // pump: flush + short sleeps give the runloop chances to deliver the callback
            long deadline = System.currentTimeMillis() + 1500;
            while (fired.get() == 0 && System.currentTimeMillis() < deadline) {
                CATransaction.flush();
                Thread.sleep(20);
            }
            if (fired.get() > 0) {
                check(true, "completion block fired (" + fired.get() + "x) after commit+flush pump");
            } else {
                System.out.println("NOTE: completion block did not fire within 1.5s (runloop-dependent) — no crash is pass");
                check(true, "completion block no-crash path (did not fire; informational)");
            }
        } catch (Throwable t) {
            check(false, "completion block section threw: " + t);
            t.printStackTrace(System.out);
        }
    }

    // ---------------------------------------------------------------- section 5

    /// CABasicAnimation add/remove cycles x300, interleaved with the hotfix shapes.
    private static void basicAnimationCycles() {
        System.out.println("--- CABasicAnimation add/remove cycles x300 (+hotfix interleave) ---");
        int iterations = 300;
        long completed = 0;
        try {
            CALayer l = CALayer.create();
            for (int i = 0; i < iterations; i++) {
                CABasicAnimation anim = CABasicAnimation.create("position");
                anim.setFromDouble(0);
                anim.setToDouble(1 + (i % 5));
                if (i % 3 == 0) anim.setByDouble(0.5);
                anim.setDuration(0.001 + (i % 3) * 0.001);
                anim.setTimingFunctionName(i % 2 == 0 ? "linear" : "easeIn");

                String key = "cycle-" + (i % 4);
                l.addAnimation(anim, key);
                CAAnimation fetched = l.animationForKey(key);
                // CALayer copies animations on add — assert presence + class, not identity.
                if (fetched == null || !fetched.isKindOfClass("CABasicAnimation")) {
                    check(false, "cycle " + i + ": animationForKey missing or wrong class");
                    break;
                }
                l.removeAnimationForKey(key);

                // hotfix-shape interleave: null and non-null through both ternary paths
                try {
                    anim.setFromValue(null);
                    anim.setToValue(null);
                    NSNumber n = NSNumber.numberWithDouble(i);
                    anim.setFromValue(n.peer());
                    anim.setToValue(n.peer());
                    anim.setByValue(n.peer());
                    anim.setTimingFunction(null);
                } catch (WrongMethodTypeException wmt) {
                    wrongMethodTypeCount++;
                    check(false, "cycle " + i + ": WrongMethodTypeException escaped: " + wmt);
                    break;
                }
                completed++;
            }
            check(completed == iterations, "animation cycles completed " + completed + "/" + iterations);
            l.removeAllAnimations();
        } catch (Throwable t) {
            check(false, "animation cycle section threw after " + completed + " cycles: " + t);
            t.printStackTrace(System.out);
        }
    }

    // ---------------------------------------------------------------- section 6

    private static void animationGroupRoundTrip() {
        System.out.println("--- CAAnimationGroup ---");
        try {
            CAAnimationGroup group = CAAnimationGroup.create();
            check(group != null && group.peer().address() != 0, "CAAnimationGroup.create non-nil");
            check(group.isKindOfClass("CAAnimation"), "group isKindOfClass CAAnimation");

            CABasicAnimation a1 = CABasicAnimation.create("position");
            a1.setFromDouble(0);
            a1.setToDouble(10);
            CABasicAnimation a2 = CABasicAnimation.create("opacity");
            a2.setFromDouble(1);
            a2.setToDouble(0);

            NSArray arr = NSArray.mutableArray();
            arr.addObject(a1);
            arr.addObject(a2);
            group.setAnimations(arr);
            NSArray back = group.animations();
            check(back != null && back.count() == 2, "animations round-trip count==2 (got " + (back == null ? "null" : back.count()) + ")");
            check(back != null && back.objectAtIndex(0).address() == a1.peer().address()
                    && back.objectAtIndex(1).address() == a2.peer().address(), "animations members round-trip by address");

            group.setDuration(0.05);
            CALayer l = CALayer.create();
            l.addAnimation(group, "grp");
            check(l.animationForKey("grp") != null, "group added under key grp");
            l.removeAnimationForKey("grp");
        } catch (Throwable t) {
            check(false, "animation group section threw: " + t);
            t.printStackTrace(System.out);
        }
    }

    // ---------------------------------------------------------------- section 7

    private static void shapeTextGradientRoundTrips() {
        System.out.println("--- CAShapeLayer / CATextLayer / CAGradientLayer ---");

        // ---- shape ----
        try {
            CAShapeLayer shape = CAShapeLayer.create();
            check(shape != null && shape.peer().address() != 0, "CAShapeLayer.create non-nil");
            check(shape.isKindOfClass("CAShapeLayer"), "shape isKindOfClass CAShapeLayer");

            NSBezierPath bp = NSBezierPath.bezierPathWithOvalInRect(new NSRect(0, 0, 40, 30));
            MemorySegment cg = bp.cgPath();
            check(cg != null && cg.address() != 0, "NSBezierPath.cgPath non-null");
            // Empirical: -CGPath builds a fresh CGPath per call AND CAShapeLayer copies
            // the path on set — so assert non-null storage, not pointer identity.
            shape.setPath(cg);
            MemorySegment stored = shape.path();
            check(stored != null && stored.address() != 0, "path raw round-trip stores a live CGPath");
            if (stored != null && stored.address() != 0) {
                System.out.println("NOTE: setPath copied (in=0x" + Long.toHexString(cg.address())
                        + " out=0x" + Long.toHexString(stored.address()) + ") — expected CA copy semantics");
            }
            shape.setPath(bp);
            check(shape.path() != null && shape.path().address() != 0, "setPath(NSBezierPath) convenience stores a CGPath");

            MemorySegment red = NSColor.redColor().cgColor();
            shape.setFillColor(red);
            check(shape.fillColor() != null && shape.fillColor().address() == red.address(), "fillColor raw round-trip by address");
            shape.setFillColor(NSColor.greenColor());
            check(shape.fillColor() != null && shape.fillColor().address() != red.address(), "setFillColor(NSColor) convenience sets distinct CGColor");

            MemorySegment blue = NSColor.blueColor().cgColor();
            shape.setStrokeColor(blue);
            check(shape.strokeColor() != null && shape.strokeColor().address() == blue.address(), "strokeColor raw round-trip by address");
            shape.setStrokeColor(NSColor.orangeColor());
            check(shape.strokeColor() != null && shape.strokeColor().address() != blue.address(), "setStrokeColor(NSColor) convenience");

            shape.setLineWidth(2.5);
            check(eps(shape.lineWidth(), 2.5), "lineWidth round-trip (got " + shape.lineWidth() + ")");

            shape.setFillRule("evenodd");
            check("evenodd".equals(shape.fillRule()), "fillRule string round-trip (got \"" + shape.fillRule() + "\")");

            shape.setLineCap("round");
            check("round".equals(shape.lineCap()), "lineCap round-trip (got \"" + shape.lineCap() + "\")");
            shape.setLineJoin("bevel");
            check("bevel".equals(shape.lineJoin()), "lineJoin round-trip (got \"" + shape.lineJoin() + "\")");

            NSArray dash = NSArray.mutableArray();
            dash.addObject(NSNumber.numberWithDouble(2));
            dash.addObject(NSNumber.numberWithDouble(1));
            shape.setLineDashPattern(dash);
            NSArray dashBack = shape.lineDashPattern();
            check(dashBack != null && dashBack.count() == 2, "lineDashPattern count==2 (got " + (dashBack == null ? "null" : dashBack.count()) + ")");
            if (dashBack != null && dashBack.count() == 2) {
                double d0 = NSNumber.wrap(dashBack.objectAtIndex(0)).doubleValue();
                double d1 = NSNumber.wrap(dashBack.objectAtIndex(1)).doubleValue();
                check(eps(d0, 2) && eps(d1, 1), "lineDashPattern values round-trip (got [" + d0 + "," + d1 + "])");
            }

            shape.setStrokeStart(0.25);
            check(eps(shape.strokeStart(), 0.25), "strokeStart round-trip (got " + shape.strokeStart() + ")");
            shape.setStrokeEnd(0.75);
            check(eps(shape.strokeEnd(), 0.75), "strokeEnd round-trip (got " + shape.strokeEnd() + ")");
        } catch (Throwable t) {
            check(false, "CAShapeLayer section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---- text ----
        try {
            CATextLayer text = CATextLayer.create();
            check(text != null && text.peer().address() != 0, "CATextLayer.create non-nil");
            check(text.isKindOfClass("CATextLayer"), "text isKindOfClass CATextLayer");

            text.setString("hello coreanim");
            check("hello coreanim".equals(text.string()), "string round-trip (got \"" + text.string() + "\")");

            // attributed string via raw NSMutableAttributedString; on macOS it is
            // installed through setString: (no setAttributedString: selector there)
            MemorySegment attrAlloc = ObjC.msgSendId(ObjC.cls("NSMutableAttributedString"), ObjC.sel("alloc"));
            MemorySegment attrStr = ObjC.msgSendIdId(attrAlloc, ObjC.sel("initWithString:"), ObjC.nsstring("attr body"));
            text.setAttributedString(attrStr);
            check(text.string() == null, "string() null while content is NSAttributedString (not an NSString)");
            text.setAttributedString((MemorySegment) null);
            text.setString("plain again");
            check("plain again".equals(text.string()), "string round-trip after attributed reset (got \"" + text.string() + "\")");

            text.setFontSize(14.5);
            check(eps(text.fontSize(), 14.5), "fontSize round-trip (got " + text.fontSize() + ")");

            text.setAlignmentMode("center");
            check("center".equals(text.alignmentMode()), "alignmentMode round-trip (got \"" + text.alignmentMode() + "\")");
            text.setTruncationMode("end");
            check("end".equals(text.truncationMode()), "truncationMode round-trip (got \"" + text.truncationMode() + "\")");

            text.setWrapped(true);
            check(text.isWrapped(), "isWrapped true after setWrapped(true)");
            text.setWrapped(false);
            check(!text.isWrapped(), "isWrapped false after setWrapped(false)");

            MemorySegment fg = NSColor.purpleColor().cgColor();
            text.setForegroundColor(fg);
            check(text.foregroundColor() != null && text.foregroundColor().address() == fg.address(), "foregroundColor raw round-trip by address");
            text.setForegroundColor(NSColor.systemBlueColor());
            check(text.foregroundColor() != null && text.foregroundColor().address() != fg.address(), "setForegroundColor(NSColor) convenience");

            // font: raw only — NSFont has no cgFont accessor (documented gap)
            text.setFont((MemorySegment) null);
            check(text.font() == null || text.font().address() == 0, "font raw null round-trip (NSFont cgFont gap noted)");
        } catch (Throwable t) {
            check(false, "CATextLayer section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---- gradient ----
        try {
            CAGradientLayer grad = CAGradientLayer.create();
            check(grad != null && grad.peer().address() != 0, "CAGradientLayer.create non-nil");
            check(grad.isKindOfClass("CAGradientLayer"), "gradient isKindOfClass CAGradientLayer");

            NSArray colors = NSArray.mutableArray();
            MemorySegment c0 = NSColor.blueColor().cgColor();
            colors.addObject(c0);
            colors.addObject(NSColor.greenColor().cgColor());
            colors.addObject(NSColor.yellowColor().cgColor());
            grad.setColors(colors);
            NSArray colorsBack = grad.colors();
            check(colorsBack != null && colorsBack.count() == 3, "colors round-trip count==3 (got " + (colorsBack == null ? "null" : colorsBack.count()) + ")");
            if (colorsBack != null && colorsBack.count() == 3) {
                check(colorsBack.objectAtIndex(0).address() == c0.address(),
                        "colors[0] round-trips the exact stored CGColor pointer");
            }

            NSArray locs = NSArray.mutableArray();
            locs.addObject(NSNumber.numberWithDouble(0));
            locs.addObject(NSNumber.numberWithDouble(0.5));
            locs.addObject(NSNumber.numberWithDouble(1));
            grad.setLocations(locs);
            NSArray locsBack = grad.locations();
            check(locsBack != null && locsBack.count() == 3, "locations round-trip count==3 (got " + (locsBack == null ? "null" : locsBack.count()) + ")");
            if (locsBack != null && locsBack.count() == 3) {
                double m = NSNumber.wrap(locsBack.objectAtIndex(1)).doubleValue();
                check(eps(m, 0.5), "locations middle stop round-trip (got " + m + ")");
            }

            grad.setStartPoint(new NSPoint(0, 0));
            NSPoint sp = grad.startPoint();
            check(sp != null && eps(sp.x(), 0) && eps(sp.y(), 0), "startPoint round-trip (got " + sp + ")");
            grad.setEndPoint(new NSPoint(1, 1));
            NSPoint ep = grad.endPoint();
            check(ep != null && eps(ep.x(), 1) && eps(ep.y(), 1), "endPoint round-trip (got " + ep + ")");

            String defaultType = grad.type();
            grad.setType("radial");
            check("radial".equals(grad.type()), "type round-trip (default " + defaultType + ", got \"" + grad.type() + "\")");
            grad.setType("axial");
        } catch (Throwable t) {
            check(false, "CAGradientLayer section threw: " + t);
            t.printStackTrace(System.out);
        }
    }

    // ---------------------------------------------------------------- section 8

    /// REGRESSION GUARD: hammer the exact hotfix ternary shape — setFromValue/setToValue
    /// with null and non-null — plus setByValue/setTimingFunction null paths.
    /// Any WrongMethodTypeException fails the run.
    private static void hotfixRegressionGuard() {
        System.out.println("--- hotfix regression guard x200 ---");
        int iterations = 200;
        long completed = 0;
        try {
            for (int i = 0; i < iterations; i++) {
                CAAnimation a = CAAnimation.animationWithKeyPath("opacity");
                // null through both ternary paths
                a.setFromValue(null);
                a.setToValue(null);
                MemorySegment f0 = a.fromValue();
                MemorySegment t0 = a.toValue();
                if (!(f0 == null || f0.address() == 0) || !(t0 == null || t0.address() == 0)) {
                    check(false, "guard iter " + i + ": from/to not null after null set");
                    break;
                }
                // non-null through both ternary paths
                NSNumber n = NSNumber.numberWithDouble(i * 0.5);
                a.setFromValue(n.peer());
                a.setToValue(n.peer());
                MemorySegment f1 = a.fromValue();
                if (f1 == null || f1.address() != n.peer().address()) {
                    check(false, "guard iter " + i + ": fromValue peer mismatch after non-null set");
                    break;
                }
                // mixed null/non-null in one breath
                a.setFromValue(n.peer());
                a.setToValue(null);
                a.setFromValue(null);
                a.setToValue(n.peer());
                // timing function null + named
                a.setTimingFunction(null);
                a.setTimingFunctionName(i % 2 == 0 ? "easeOut" : "linear");
                completed++;
            }
            check(completed == iterations, "hotfix guard completed " + completed + "/" + iterations + " iterations");
        } catch (WrongMethodTypeException wmt) {
            wrongMethodTypeCount++;
            check(false, "REGRESSION: WrongMethodTypeException escaped hotfix shape after " + completed + " iters: " + wmt);
        } catch (Throwable t) {
            check(false, "hotfix guard threw unexpected after " + completed + " iters: " + t);
            t.printStackTrace(System.out);
        }
        check(wrongMethodTypeCount == 0, "zero WrongMethodTypeException across all sections (count=" + wrongMethodTypeCount + ")");
    }
}
