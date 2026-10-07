package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import nsui.*;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CACompletenessTest — the Core Animation completeness tier:
/// - CATransform3D: a 128-byte by-value value type, round-tripped through
///   CALayer transform/sublayerTransform (the objc_msgSend_stret path on x86_64);
/// - CALayer's remaining public API: transforms, contents geometry, media timing,
///   filters, corner masking, rasterization, sublayer ordering, actions, layout,
///   hit testing, coordinate/time conversion and renderInContext:;
/// - CAPropertyAnimation as the shared base of basic/keyframe/spring
///   (CAAnimationGroup is a CAAnimation sibling, not a property animation).
///
/// Every accessor gets a real set-then-get assertion; renderInContext: proves the
/// pixels it claims to draw; layoutSublayers (a pure side-effect action with no
/// getter) uses TestKit.noThrow. Windows stay hidden.
public final class CACompletenessTest {

    private static boolean eps(double a, double b) { return Math.abs(a - b) < 1e-9; }

    public static void main(String[] args) {
        System.out.println("=== CACompletenessTest — CATransform3D + full CALayer + CAPropertyAnimation ===");
        ObjC.init();

        transform3DValueType();
        layerTransforms();
        layerContentsAndFilters();
        layerTimingAndRasterization();
        layerIdentityAndMask();
        layerSublayerOrdering();
        layerActionsLayoutAndAnimationKeys();
        layerHitTestAndConversion();
        layerRenderInContext();
        propertyAnimation();

        TestKit.end();
    }

    // ------------------------------------------------------------------ CATransform3D

    private static void transform3DValueType() {
        System.out.println("--- CATransform3D value type ---");
        try {
            TestKit.check(CATransform3D.IDENTITY.isIdentity(), "IDENTITY is identity");
            TestKit.check(!CATransform3D.translation(3, 4, 5).isIdentity(), "translation is not identity");

            CATransform3D t = CATransform3D.translation(3, 4, 5);
            CATransform3D roundTripped = CATransform3D.fromSegment(t.toSegment());
            TestKit.check(roundTripped.equals(t), "toSegment/fromSegment round-trip (got " + roundTripped + ")");

            CATransform3D made = CATransform3D.make(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
            TestKit.check(CATransform3D.make(made.toArray()).equals(made), "make/toArray round-trip");

            CATransform3D sc = CATransform3D.scale(2, 3, 4);
            TestKit.check(eps(sc.m11(), 2) && eps(sc.m22(), 3) && eps(sc.m33(), 4) && eps(sc.m44(), 1),
                    "scale lays components in struct order (m11=" + sc.m11() + ", m22=" + sc.m22() + ", m33=" + sc.m33() + ")");
        } catch (Throwable t) {
            TestKit.check(false, "CATransform3D section threw: " + t);
        }
    }

    private static void layerTransforms() {
        System.out.println("--- CALayer transform / sublayerTransform ---");
        try {
            CALayer l = CALayer.create();
            CATransform3D t = CATransform3D.translation(7, 8, 9);
            l.setTransform(t);
            TestKit.check(l.transform().equals(t), "transform round-trip (got " + l.transform() + ")");

            l.setTransform(CATransform3D.IDENTITY);
            TestKit.check(l.transform().isIdentity(), "transform reset to identity reads identity");

            CATransform3D sub = CATransform3D.scale(2, 3, 4);
            l.setSublayerTransform(sub);
            TestKit.check(l.sublayerTransform().equals(sub), "sublayerTransform round-trip (got " + l.sublayerTransform() + ")");
        } catch (Throwable t) {
            TestKit.check(false, "layer transform section threw: " + t);
        }
    }

    // ------------------------------------------------------- contents / geometry / filters

    private static void layerContentsAndFilters() {
        System.out.println("--- CALayer contents geometry + filters ---");
        try {
            CALayer l = CALayer.create();

            l.setContentsRect(new NSRect(0.1, 0.2, 0.3, 0.4));
            NSRect cr = l.contentsRect();
            TestKit.check(cr != null && eps(cr.x(), 0.1) && eps(cr.y(), 0.2) && eps(cr.width(), 0.3) && eps(cr.height(), 0.4),
                    "contentsRect round-trip (got " + cr + ")");

            l.setContentsCenter(new NSRect(0.25, 0.25, 0.5, 0.5));
            NSRect cc = l.contentsCenter();
            TestKit.check(cc != null && eps(cc.x(), 0.25) && eps(cc.width(), 0.5),
                    "contentsCenter round-trip (got " + cc + ")");

            l.setMinificationFilter("nearest");
            TestKit.check("nearest".equals(l.minificationFilter()), "minificationFilter round-trip");
            l.setMagnificationFilter("trilinear");
            TestKit.check("trilinear".equals(l.magnificationFilter()), "magnificationFilter round-trip");

            l.setMinificationFilterBias(0.25f);
            TestKit.check(eps(l.minificationFilterBias(), 0.25f), "minificationFilterBias round-trip (got " + l.minificationFilterBias() + ")");

            long corners = l.maskedCorners();
            l.setMaskedCorners(5L);
            TestKit.check(l.maskedCorners() == 5L, "maskedCorners round-trip (default " + corners + ", got " + l.maskedCorners() + ")");

            l.setCornerCurve("continuous");
            TestKit.check("continuous".equals(l.cornerCurve()), "cornerCurve round-trip (got " + l.cornerCurve() + ")");

            String defaultFormat = l.contentsFormat();
            l.setContentsFormat("Gray8");
            TestKit.check("Gray8".equals(l.contentsFormat()),
                    "contentsFormat round-trip (default " + defaultFormat + ", got " + l.contentsFormat() + ")");

            l.setOpaque(true);
            TestKit.check(l.isOpaque(), "isOpaque true after setOpaque(true)");
            l.setOpaque(false);
            TestKit.check(!l.isOpaque(), "isOpaque false after setOpaque(false)");
        } catch (Throwable t) {
            TestKit.check(false, "contents geometry/filter section threw: " + t);
        }
    }

    // ------------------------------------------------------- media timing / rasterization

    private static void layerTimingAndRasterization() {
        System.out.println("--- CALayer CAMediaTiming + rasterization ---");
        try {
            CALayer l = CALayer.create();

            l.setDuration(1.5);
            TestKit.check(eps(l.duration(), 1.5), "duration round-trip (got " + l.duration() + ")");
            l.setBeginTime(2.5);
            TestKit.check(eps(l.beginTime(), 2.5), "beginTime round-trip (got " + l.beginTime() + ")");
            l.setSpeed(1.25f);
            TestKit.check(eps(l.speed(), 1.25f), "speed round-trip (got " + l.speed() + ")");
            l.setTimeOffset(0.75);
            TestKit.check(eps(l.timeOffset(), 0.75), "timeOffset round-trip (got " + l.timeOffset() + ")");
            l.setRepeatCount(3.5f);
            TestKit.check(eps(l.repeatCount(), 3.5f), "repeatCount round-trip (got " + l.repeatCount() + ")");
            l.setRepeatDuration(2.0);
            TestKit.check(eps(l.repeatDuration(), 2.0), "repeatDuration round-trip (got " + l.repeatDuration() + ")");

            l.setAutoreverses(true);
            TestKit.check(l.autoreverses(), "autoreverses true after setAutoreverses(true)");
            l.setAutoreverses(false);
            TestKit.check(!l.autoreverses(), "autoreverses false after setAutoreverses(false)");

            l.setFillMode("forwards");
            TestKit.check("forwards".equals(l.fillMode()), "fillMode round-trip (got " + l.fillMode() + ")");

            l.setShouldRasterize(true);
            TestKit.check(l.shouldRasterize(), "shouldRasterize round-trip true");
            l.setShouldRasterize(false);
            TestKit.check(!l.shouldRasterize(), "shouldRasterize round-trip false");

            l.setRasterizationScale(2.0);
            TestKit.check(eps(l.rasterizationScale(), 2.0), "rasterizationScale round-trip (got " + l.rasterizationScale() + ")");

            l.setDrawsAsynchronously(true);
            TestKit.check(l.drawsAsynchronously(), "drawsAsynchronously round-trip true");
            l.setDrawsAsynchronously(false);
            TestKit.check(!l.drawsAsynchronously(), "drawsAsynchronously round-trip false");

            l.setAllowsEdgeAntialiasing(true);
            TestKit.check(l.allowsEdgeAntialiasing(), "allowsEdgeAntialiasing round-trip true");
            l.setAllowsEdgeAntialiasing(false);
            TestKit.check(!l.allowsEdgeAntialiasing(), "allowsEdgeAntialiasing round-trip false");

            long defaultMask = l.edgeAntialiasingMask();
            l.setEdgeAntialiasingMask(3L);
            TestKit.check(l.edgeAntialiasingMask() == 3L,
                    "edgeAntialiasingMask round-trip (default " + defaultMask + ", got " + l.edgeAntialiasingMask() + ")");
        } catch (Throwable t) {
            TestKit.check(false, "timing/rasterization section threw: " + t);
        }
    }

    // ------------------------------------------------------------ name / delegate / mask

    private static void layerIdentityAndMask() {
        System.out.println("--- CALayer name / delegate / mask ---");
        try {
            CALayer l = CALayer.create();
            l.setName("completeness-layer");
            TestKit.check("completeness-layer".equals(l.name()), "name round-trip (got " + l.name() + ")");

            // delegate is weak: hold a strong Java reference to an immortal singleton
            // and clear it immediately, before any layout/action message can reach it.
            NSColor red = NSColor.redColor();
            l.setDelegate(red.peer());
            MemorySegment delegate = l.delegate();
            TestKit.check(delegate != null && delegate.address() == red.peer().address(),
                    "delegate round-trip by address");
            l.setDelegate((MemorySegment) null);
            MemorySegment cleared = l.delegate();
            TestKit.check(cleared == null || cleared.address() == 0, "delegate cleared");

            CALayer mask = CALayer.create();
            l.setMask(mask);
            MemorySegment stored = l.mask();
            TestKit.check(stored != null && stored.address() == mask.peer().address(), "mask round-trip by address");
            l.setMask((CALayer) null);
            MemorySegment noMask = l.mask();
            TestKit.check(noMask == null || noMask.address() == 0, "mask cleared");
        } catch (Throwable t) {
            TestKit.check(false, "identity/mask section threw: " + t);
        }
    }

    // ------------------------------------------------------------- sublayer ordering

    private static void layerSublayerOrdering() {
        System.out.println("--- CALayer insertSublayer:/replaceSublayer: ordering ---");
        try {
            CALayer root = CALayer.create();
            CALayer a1 = CALayer.create();
            CALayer a2 = CALayer.create();
            CALayer a3 = CALayer.create();
            CALayer a4 = CALayer.create();
            CALayer a5 = CALayer.create();

            root.addSublayer(a1);              // [a1]
            root.insertSublayer(a2, 0);        // [a2, a1]
            TestKit.check(indexOf(root, a2) == 0 && indexOf(root, a1) == 1,
                    "insertSublayer:atIndex: put a2 at 0, a1 at 1");

            root.insertSublayerAbove(a3, a1);  // [a2, a1, a3]
            TestKit.check(indexOf(root, a3) == indexOf(root, a1) + 1,
                    "insertSublayer:above: a3 directly above a1");

            root.insertSublayerBelow(a4, a1);  // [a2, a4, a1, a3]
            TestKit.check(indexOf(root, a4) == indexOf(root, a1) - 1,
                    "insertSublayer:below: a4 directly below a1");

            long oldIndex = indexOf(root, a1);
            root.replaceSublayer(a1, a5);
            TestKit.check(indexOf(root, a5) == oldIndex && indexOf(root, a1) == -1,
                    "replaceSublayer:with: swaps in place at index " + oldIndex);
            TestKit.check(root.sublayers() != null && root.sublayers().count() == 4,
                    "sublayer count stays 4 after replace");
        } catch (Throwable t) {
            TestKit.check(false, "sublayer ordering section threw: " + t);
        }
    }

    private static long indexOf(CALayer parent, CALayer child) {
        NSArray subs = parent.sublayers();
        if (subs == null) return -1;
        for (long i = 0; i < subs.count(); i++) {
            if (subs.objectAtIndex(i).address() == child.peer().address()) return i;
        }
        return -1;
    }

    // ------------------------------------------------- actions / animationKeys / layout

    private static void layerActionsLayoutAndAnimationKeys() {
        System.out.println("--- CALayer actionForKey: / animationKeys / preferredFrameSize / layout ---");
        try {
            CALayer l = CALayer.create();

            MemorySegment absent = l.actionForKey("nsui-no-such-action");
            TestKit.check(absent == null || absent.address() == 0, "actionForKey: unknown key returns nil");

            CABasicAnimation anim = CABasicAnimation.create("opacity");
            l.addAnimation(anim, "completeness-key");
            NSArray keys = l.animationKeys();
            boolean found = false;
            if (keys != null) {
                for (long i = 0; i < keys.count(); i++) {
                    if ("completeness-key".equals(ObjC.toString(keys.objectAtIndex(i)))) found = true;
                }
            }
            TestKit.check(found, "animationKeys contains the added key (got " + (keys == null ? "null" : keys.count()) + ")");
            l.removeAllAnimations();
            NSArray after = l.animationKeys();
            TestKit.check(after == null || after.count() == 0, "animationKeys empty after removeAllAnimations");

            l.setBounds(new NSRect(0, 0, 33, 44));
            NSSize preferred = l.preferredFrameSize();
            TestKit.check(preferred != null && eps(preferred.width(), 33) && eps(preferred.height(), 44),
                    "preferredFrameSize follows bounds (got " + preferred + ")");

            l.setNeedsLayout();
            TestKit.check(l.needsLayout(), "setNeedsLayout sets needsLayout");
            l.layoutIfNeeded();
            TestKit.check(!l.needsLayout(), "layoutIfNeeded clears needsLayout");
            TestKit.noThrow("layoutSublayers no-throw", () -> l.layoutSublayers());
        } catch (Throwable t) {
            TestKit.check(false, "actions/layout section threw: " + t);
        }
    }

    // ---------------------------------------------------- hitTest / containsPoint / convert

    private static void layerHitTestAndConversion() {
        System.out.println("--- CALayer hitTest: / containsPoint: / convertPoint: / convertTime: ---");
        try {
            CALayer parent = CALayer.create();
            parent.setBounds(new NSRect(0, 0, 100, 100));
            parent.setPosition(new NSPoint(50, 50));
            CALayer child = CALayer.create();
            child.setBounds(new NSRect(0, 0, 20, 20));
            child.setPosition(new NSPoint(50, 50));
            parent.addSublayer(child);

            CALayer onChild = parent.hitTest(new NSPoint(50, 50));
            TestKit.check(onChild != null && onChild.peer().address() == child.peer().address(),
                    "hitTest: center returns the child");
            CALayer onParent = parent.hitTest(new NSPoint(5, 5));
            TestKit.check(onParent != null && onParent.peer().address() == parent.peer().address(),
                    "hitTest: away from child returns the parent");
            TestKit.check(parent.hitTest(new NSPoint(150, 150)) == null, "hitTest: outside returns null");

            TestKit.check(parent.containsPoint(new NSPoint(50, 50)), "containsPoint: inside bounds");
            TestKit.check(!parent.containsPoint(new NSPoint(150, 150)), "containsPoint: outside bounds");
            TestKit.check(child.containsPoint(new NSPoint(10, 10)), "child containsPoint: inside child bounds");
            TestKit.check(!child.containsPoint(new NSPoint(25, 25)), "child containsPoint: outside child bounds");

            // child frame origin is (40,40); (10,10) in child maps to (50,50) in parent.
            NSPoint inParent = parent.convertPointFromLayer(new NSPoint(10, 10), child);
            TestKit.check(inParent != null && eps(inParent.x(), 50) && eps(inParent.y(), 50),
                    "convertPoint:fromLayer: maps (10,10) -> (50,50) (got " + inParent + ")");
            NSPoint backInChild = child.convertPointFromLayer(new NSPoint(50, 50), parent);
            TestKit.check(backInChild != null && eps(backInChild.x(), 10) && eps(backInChild.y(), 10),
                    "convertPoint:fromLayer: is the inverse (got " + backInChild + ")");
            NSPoint toChild = parent.convertPointToLayer(new NSPoint(50, 50), child);
            TestKit.check(toChild != null && eps(toChild.x(), 10) && eps(toChild.y(), 10),
                    "convertPoint:toLayer: matches fromLayer (got " + toChild + ")");

            TestKit.check(eps(parent.convertTimeFromLayer(1.5, (CALayer) null), 1.5),
                    "convertTime:fromLayer: nil time base is identity");
            TestKit.check(eps(child.convertTimeFromLayer(1.5, parent), 1.5),
                    "convertTime:fromLayer: untimed layer is identity");
            TestKit.check(eps(child.convertTimeToLayer(1.5, parent), 1.5),
                    "convertTime:toLayer: untimed layer is identity");
        } catch (Throwable t) {
            TestKit.check(false, "hit-test/conversion section threw: " + t);
        }
    }

    // ------------------------------------------------------------ renderInContext: pixels

    private static void layerRenderInContext() {
        System.out.println("--- CALayer renderInContext: to a bitmap context ---");
        NSWindow window = null;
        try {
            window = TestKit.hiddenWindow(40, 40);
            NSView view = window.contentView();
            NSRect bounds = view.bounds();

            MethodHandle hCache = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
            MemorySegment rep = (MemorySegment) hCache.invokeExact(view.peer(),
                    ObjC.sel("bitmapImageRepForCachingDisplayInRect:"), bounds.toSegment());
            TestKit.check(rep != null && rep.address() != 0, "bitmap image rep for caching display is non-nil");

            NSGraphicsContext ctx = NSGraphicsContext.graphicsContextWithBitmapImageRep(NSBitmapImageRep.wrap(rep));
            TestKit.check(ctx != null, "bitmap-backed graphics context is non-nil");

            CALayer layer = CALayer.create();
            layer.setBounds(new NSRect(0, 0, 32, 32));
            layer.setBackgroundColor(NSColor.redColor());
            TestKit.noThrow("renderInContext: draws without throwing", () -> layer.renderInContext(ctx.CGContext()));
            ctx.flushGraphics();

            long pixelsWide = ObjC.msgSendLong(rep, ObjC.sel("pixelsWide"));
            long pixelsHigh = ObjC.msgSendLong(rep, ObjC.sel("pixelsHigh"));
            long bytesPerRow = ObjC.msgSendLong(rep, ObjC.sel("bytesPerRow"));
            long samplesPerPixel = ObjC.msgSendLong(rep, ObjC.sel("samplesPerPixel"));
            MemorySegment data = ObjC.msgSendId(rep, ObjC.sel("bitmapData"));
            MemorySegment bytes = data.reinterpret(bytesPerRow * pixelsHigh);

            int x = (int) (pixelsWide / 2);
            int y = (int) (pixelsHigh / 2);
            // Byte-order agnostic: locate the alpha sample (if any) via
            // hasAlpha/bitmapFormat, then judge the remaining color samples by
            // their max-channel pattern instead of fixed R,G,B positions.
            int alphaIdx = alphaIndex(rep, (int) samplesPerPixel);
            int[] colors = new int[(int) samplesPerPixel - (alphaIdx >= 0 ? 1 : 0)];
            for (int k = 0, s = 0; k < samplesPerPixel; k++) {
                if (k == alphaIdx) continue;
                long off = (long) y * bytesPerRow + (long) x * samplesPerPixel + k;
                colors[s++] = Byte.toUnsignedInt(bytes.get(ValueLayout.JAVA_BYTE, off));
            }
            int dom = 0;
            for (int k = 1; k < colors.length; k++) {
                if (colors[k] > colors[dom]) dom = k;
            }
            boolean restLow = true;
            for (int k = 0; k < colors.length; k++) {
                if (k != dom && colors[k] >= 100) { restLow = false; break; }
            }
            StringBuilder sb = new StringBuilder("center channel samples=[");
            for (int k = 0; k < colors.length; k++) {
                if (k > 0) sb.append(',');
                sb.append(colors[k]);
            }
            sb.append("] dominant idx=").append(dom);
            TestKit.check(colors[dom] > 150 && restLow,
                    "renderInContext: painted a saturated primary at the center (" + sb + ")");
        } catch (Throwable t) {
            TestKit.check(false, "renderInContext section threw: " + t);
        } finally {
            TestKit.close(window);
        }
    }

    /// Alpha sample index within the rep's samples, or -1 when there is none.
    /// Derived from the rep itself (hasAlpha + bitmapFormat alpha-first bit),
    /// so RGBA and ARGB orders both resolve without assuming positions.
    private static int alphaIndex(MemorySegment rep, int samplesPerPixel) {
        try {
            if (samplesPerPixel == 4 && ObjC.msgSendBool(rep, ObjC.sel("hasAlpha"))) {
                long fmt = ObjC.msgSendLong(rep, ObjC.sel("bitmapFormat"));
                return ((fmt & 1L) != 0) ? 0 : 3;
            }
        } catch (Throwable t) {
            if (samplesPerPixel == 4) return 3;
        }
        return -1;
    }

    // ------------------------------------------------------------- CAPropertyAnimation

    private static void propertyAnimation() {
        System.out.println("--- CAPropertyAnimation base class ---");
        try {
            CABasicAnimation basic = CABasicAnimation.create("position");
            TestKit.check(basic != null, "CABasicAnimation.create non-nil");
            TestKit.check(basic.isKindOfClass("CAPropertyAnimation"), "CABasicAnimation isKindOfClass CAPropertyAnimation");

            CAPropertyAnimation prop = basic;
            TestKit.check("position".equals(prop.keyPath()), "keyPath from create reads back (got " + prop.keyPath() + ")");
            prop.setKeyPath("opacity");
            TestKit.check("opacity".equals(prop.keyPath()), "keyPath round-trip (got " + prop.keyPath() + ")");

            prop.setAdditive(true);
            TestKit.check(prop.isAdditive(), "additive round-trip true");
            prop.setAdditive(false);
            TestKit.check(!prop.isAdditive(), "additive round-trip false");

            prop.setCumulative(true);
            TestKit.check(prop.isCumulative(), "cumulative round-trip true");
            prop.setCumulative(false);
            TestKit.check(!prop.isCumulative(), "cumulative round-trip false");

            CAValueFunction vf = CAValueFunction.functionWithName("rotateX");
            prop.setValueFunction(vf);
            CAValueFunction got = prop.valueFunction();
            TestKit.check(got != null && "rotateX".equals(got.name()),
                    "valueFunction round-trip (got " + (got == null ? "null" : got.name()) + ")");
            prop.setValueFunction(null);
            TestKit.check(prop.valueFunction() == null, "valueFunction cleared");

            CAPropertyAnimation wrapped = CAPropertyAnimation.wrap(basic.peer());
            TestKit.check(wrapped != null && wrapped.peer().address() == basic.peer().address(),
                    "CAPropertyAnimation.wrap round-trips the peer");

            CAKeyframeAnimation kf = CAKeyframeAnimation.create("position");
            TestKit.check(kf != null && kf.isKindOfClass("CAPropertyAnimation"),
                    "CAKeyframeAnimation isKindOfClass CAPropertyAnimation");
            CASpringAnimation sp = CASpringAnimation.create("position");
            TestKit.check(sp != null && sp.isKindOfClass("CAPropertyAnimation"),
                    "CASpringAnimation isKindOfClass CAPropertyAnimation");
            CAAnimationGroup group = CAAnimationGroup.create();
            TestKit.check(group != null && group.isKindOfClass("CAAnimation"),
                    "CAAnimationGroup isKindOfClass CAAnimation");
            // ObjC reality: CAAnimationGroup derives from CAAnimation (a SIBLING of
            // CAPropertyAnimation) and does not respond to keyPath/additive/valueFunction.
            // The Java hierarchy must mirror that, or an inherited accessor would raise
            // an unrecognized selector on a real group.
            TestKit.check(CAAnimationGroup.class.getSuperclass() == CAAnimation.class,
                    "CAAnimationGroup extends CAAnimation (mirrors ObjC)");
            TestKit.check(CABasicAnimation.class.getSuperclass() == CAPropertyAnimation.class,
                    "CABasicAnimation extends CAPropertyAnimation");
            TestKit.check(CAKeyframeAnimation.class.getSuperclass() == CAPropertyAnimation.class,
                    "CAKeyframeAnimation extends CAPropertyAnimation");
            TestKit.check(CASpringAnimation.class.getSuperclass() == CABasicAnimation.class,
                    "CASpringAnimation extends CABasicAnimation");
            TestKit.check(!group.respondsToSelector(ObjC.sel("keyPath")),
                    "CAAnimationGroup does not respond to keyPath");
            TestKit.check(!group.respondsToSelector(ObjC.sel("isAdditive")),
                    "CAAnimationGroup does not respond to isAdditive");
        } catch (Throwable t) {
            TestKit.check(false, "CAPropertyAnimation section threw: " + t);
        }
    }
}
