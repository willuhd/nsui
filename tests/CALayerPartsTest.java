package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.CAConstraint;
import nsui.CAConstraintLayoutManager;
import nsui.CADisplayLink;
import nsui.CAEDRMetadata;
import nsui.CAEmitterCell;
import nsui.CAEmitterLayer;
import nsui.CALayer;
import nsui.NSApplication;
import nsui.NSPoint;
import nsui.NSRect;
import nsui.NSRunLoop;
import nsui.NSSize;
import nsui.CAReplicatorLayer;
import nsui.CAScrollLayer;
import nsui.CATiledLayer;
import nsui.CATransformLayer;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;

/// Committed coverage for the layer-parts tier: replicator/scroll/tiled/
/// transform layers, emitter cell+layer, constraints, display link, run loop.
/// Pure objects (no windows); the display link attaches to the current
/// runloop and must call back within 2s.
public final class CALayerPartsTest {

    public static void main(String[] args) {
        System.out.println("=== CALayerPartsTest — layer parts ===");
        ObjC.init();
        NSApplication app = TestKit.app();

        try {
            CAReplicatorLayer rl = CAReplicatorLayer.create();
            rl.setInstanceCount(5);
            TestKit.check(rl.instanceCount() == 5, "replicator count round-trip");
            rl.setInstanceDelay(0.1);
            TestKit.check(Math.abs(rl.instanceDelay() - 0.1) < 1e-9, "replicator delay round-trip");
            rl.setInstanceAlphaOffset(0.2f);
            TestKit.check(Math.abs(rl.instanceAlphaOffset() - 0.2f) < 1e-6, "replicator alpha round-trip");
            rl.setPreservesDepth(true);
            TestKit.check(rl.preservesDepth(), "replicator preservesDepth round-trip");
        } catch (Throwable t) {
            TestKit.check(false, "replicator section threw: " + t);
        }

        try {
            CAScrollLayer sl = CAScrollLayer.create();
            sl.setBounds(new NSRect(0, 0, 200, 200));
            TestKit.noThrow("scrollToPoint no-crash", () -> sl.scrollToPoint(new NSPoint(10, 20)));
            NSRect scrolled = sl.bounds();
            TestKit.check(scrolled != null
                    && Math.abs(scrolled.x() - 10) < 1e-6 && Math.abs(scrolled.y() - 20) < 1e-6,
                    "scrollToPoint moves the scroll origin to (10,20) (got " + scrolled + ")");
            sl.setScrollMode("none");
            TestKit.check("none".equals(sl.scrollMode()), "scrollMode round-trip");
        } catch (Throwable t) {
            TestKit.check(false, "scroll section threw: " + t);
        }

        try {
            CATiledLayer tl = CATiledLayer.create();
            tl.setLevelsOfDetail(3);
            TestKit.check(tl.levelsOfDetail() == 3, "tiled levels round-trip");
            tl.setTileSize(new NSSize(256, 256));
            TestKit.check(tl.tileSize().width() == 256 && tl.tileSize().height() == 256,
                    "tiled tileSize round-trip");
            TestKit.check(Double.isFinite(CATiledLayer.fadeDuration()), "tiled fadeDuration finite");
        } catch (Throwable t) {
            TestKit.check(false, "tiled section threw: " + t);
        }

        try {
            CATransformLayer xl = CATransformLayer.create();
            TestKit.check(xl != null, "transform layer creates");
        } catch (Throwable t) {
            TestKit.check(false, "transform section threw: " + t);
        }

        try {
            CAEmitterCell cell = CAEmitterCell.create();
            cell.setName("spark");
            TestKit.check("spark".equals(cell.name()), "cell name round-trip");
            cell.setBirthRate(10.0f);
            TestKit.check(cell.birthRate() == 10.0f, "cell birthRate round-trip");
            cell.setLifetime(2.0f);
            TestKit.check(cell.lifetime() == 2.0f, "cell lifetime round-trip");
            cell.setEmissionLatitude(0.5);
            TestKit.check(cell.emissionLatitude() == 0.5, "cell latitude round-trip");
            cell.setEnabled(false);
            TestKit.check(!cell.isEnabled(), "cell enabled round-trip");
        } catch (Throwable t) {
            TestKit.check(false, "emitter-cell section threw: " + t);
        }

        try {
            CAEmitterLayer el = CAEmitterLayer.create();
            el.setBirthRate(5.0f);
            TestKit.check(el.birthRate() == 5.0f, "layer birthRate round-trip");
            el.setEmitterPosition(new NSPoint(100, 100));
            TestKit.check(el.emitterPosition().x() == 100, "layer emitterPosition round-trip");
            el.setEmitterSize(new NSSize(50, 50));
            TestKit.check(el.emitterSize().width() == 50, "layer emitterSize round-trip");
            el.setEmitterShape(CAEmitterLayer.SHAPE_SPHERE);
            TestKit.check(CAEmitterLayer.SHAPE_SPHERE.equals(el.emitterShape()), "layer shape round-trip");
            el.setRenderMode(CAEmitterLayer.RENDER_ADDITIVE);
            TestKit.check(CAEmitterLayer.RENDER_ADDITIVE.equals(el.renderMode()), "layer renderMode round-trip");
        } catch (Throwable t) {
            TestKit.check(false, "emitter-layer section threw: " + t);
        }

        try {
            CAConstraint cc = CAConstraint.relativeTo(CAConstraint.ATTR_MIN_X,
                    "superlayer", CAConstraint.ATTR_MIN_X, 1.0, 10.0);
            TestKit.check(cc != null && cc.attribute() == CAConstraint.ATTR_MIN_X
                    && cc.scale() == 1.0 && cc.offset() == 10.0, "constraint full factory round-trip");
            TestKit.check("superlayer".equals(cc.sourceName())
                    && cc.sourceAttribute() == CAConstraint.ATTR_MIN_X, "constraint source round-trip");
            CAConstraintLayoutManager lm = CAConstraintLayoutManager.create();
            TestKit.check(lm != null, "layout manager creates");
            CALayer host = CALayer.create();
            host.addConstraint(cc);
            TestKit.check(host.constraints() != null && host.constraints().count() == 1,
                    "constraint installed on layer");
        } catch (Throwable t) {
            TestKit.check(false, "constraint section threw: " + t);
        }

        try {
            TestKit.probe("EDR nits factory non-nil: " + (CAEDRMetadata.hdr10(0.5f, 4000.0f, 1.0f) != null));
            TestKit.probe("EDR SEI factory non-nil (nil blobs): " + (CAEDRMetadata.hdr10(null, null, 1.0f) != null));
            TestKit.probe("EDR hlg factory non-nil: " + (CAEDRMetadata.hlg(null) != null));
        } catch (Throwable t) {
            TestKit.check(false, "EDR section threw: " + t);
        }

        try {
            TestKit.check(NSRunLoop.main() != null, "main runloop resolves");
            TestKit.check(NSRunLoop.current() != null, "current runloop resolves");
            AtomicInteger ticks = new AtomicInteger();
            Map<String, DelegateProxy.VoidArg> voids = new LinkedHashMap<>();
            voids.put("nsuiTick:", sender -> ticks.incrementAndGet());
            MemorySegment target = DelegateProxy.delegate("NSObject",
                    "NsuiDLTarget" + System.nanoTime(), Map.of(), voids);
            CADisplayLink dl = CADisplayLink.create(target, "nsuiTick:");
            TestKit.check(dl != null, "display link creates");
            if (dl == null) return;
            dl.setPaused(true);
            TestKit.check(dl.isPaused(), "display link paused round-trip");
            dl.setPreferredFramesPerSecond(30);
            TestKit.check(dl.preferredFramesPerSecond() == 30, "display link fps round-trip");
            dl.setPaused(false);
            dl.addToRunLoop(NSRunLoop.current(), NSRunLoop.DEFAULT_MODE);
            long deadline = System.currentTimeMillis() + 2000;
            while (ticks.get() == 0 && System.currentTimeMillis() < deadline) {
                TestKit.pumpOnce(app);
            }
            TestKit.check(ticks.get() > 0,
                    "display link callback fired on the runloop within 2s (ticks=" + ticks.get() + ")");
            TestKit.noThrow("display link invalidate no-crash", () -> dl.invalidate());
        } catch (Throwable t) {
            TestKit.check(false, "displaylink section threw: " + t);
        }

        TestKit.end();
    }
}
