package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CALayer — thin wrapper for QuartzCore CALayer.
/// Unifies cornerRadius, borderWidth, backgroundColor already exposed via NSBox but as a
/// standalone layer wrapper. Every method maps to one `objc_msgSend` selector.
/// Follows FFM pattern: no reflection, cached handles, ensureInit.
///
/// Note: CALayer lives in QuartzCore.framework; ObjC.ensureFramework loads it lazily via
/// AppKit/CoreGraphics dependencies, but we ensure QuartzCore explicitly if needed.
public class CALayer extends NSObject {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment alloc;
        static MemorySegment init;
        static MemorySegment cornerRadius;
        static MemorySegment setCornerRadius;
        static MemorySegment borderWidth;
        static MemorySegment setBorderWidth;
        static MemorySegment borderColor;
        static MemorySegment setBorderColor;
        static MemorySegment CGColor;
        static MemorySegment backgroundColor;
        static MemorySegment setBackgroundColor;
        static MemorySegment masksToBounds;
        static MemorySegment setMasksToBounds;
        static MemorySegment opacity;
        static MemorySegment setOpacity;
        static MemorySegment addAnimation_forKey;
        static MemorySegment removeAnimationForKey;
        static MemorySegment animationForKey;
        static MemorySegment removeAllAnimations;
        static MemorySegment layoutIfNeeded;
        static MemorySegment setNeedsDisplay;
        static MemorySegment displayIfNeeded;
        static MemorySegment position;
        static MemorySegment setPosition;
        static MemorySegment anchorPoint;
        static MemorySegment setAnchorPoint;
        static MemorySegment bounds;
        static MemorySegment setBounds;
        static MemorySegment frame;
        static MemorySegment setFrame;
        static MemorySegment zPosition;
        static MemorySegment setZPosition;
        static MemorySegment contentsScale;
        static MemorySegment setContentsScale;
        static MemorySegment isHidden;
        static MemorySegment setHidden;
        static MemorySegment contents;
        static MemorySegment setContents;
        static MemorySegment contentsGravity;
        static MemorySegment setContentsGravity;
        static MemorySegment shadowColor;
        static MemorySegment setShadowColor;
        static MemorySegment shadowOpacity;
        static MemorySegment setShadowOpacity;
        static MemorySegment shadowRadius;
        static MemorySegment setShadowRadius;
        static MemorySegment shadowOffset;
        static MemorySegment setShadowOffset;
        static MemorySegment addSublayer;
        static MemorySegment removeFromSuperlayer;
        static MemorySegment superlayer;
        static MemorySegment sublayers;
        static MemorySegment constraints;
        static MemorySegment setConstraints;
        static MemorySegment addConstraint;
        static MemorySegment begin;
        static MemorySegment setAnimationDuration;
        static MemorySegment commit;
        static MemorySegment transform;
        static MemorySegment setTransform;
        static MemorySegment sublayerTransform;
        static MemorySegment setSublayerTransform;
        static MemorySegment mask;
        static MemorySegment setMask;
        static MemorySegment delegate;
        static MemorySegment setDelegate;
        static MemorySegment name;
        static MemorySegment setName;
        static MemorySegment isOpaque;
        static MemorySegment setOpaque;
        static MemorySegment contentsRect;
        static MemorySegment setContentsRect;
        static MemorySegment contentsCenter;
        static MemorySegment setContentsCenter;
        static MemorySegment magnificationFilter;
        static MemorySegment setMagnificationFilter;
        static MemorySegment minificationFilter;
        static MemorySegment setMinificationFilter;
        static MemorySegment minificationFilterBias;
        static MemorySegment setMinificationFilterBias;
        static MemorySegment maskedCorners;
        static MemorySegment setMaskedCorners;
        static MemorySegment cornerCurve;
        static MemorySegment setCornerCurve;
        static MemorySegment duration;
        static MemorySegment setDuration;
        static MemorySegment beginTime;
        static MemorySegment setBeginTime;
        static MemorySegment speed;
        static MemorySegment setSpeed;
        static MemorySegment timeOffset;
        static MemorySegment setTimeOffset;
        static MemorySegment repeatCount;
        static MemorySegment setRepeatCount;
        static MemorySegment repeatDuration;
        static MemorySegment setRepeatDuration;
        static MemorySegment autoreverses;
        static MemorySegment setAutoreverses;
        static MemorySegment fillMode;
        static MemorySegment setFillMode;
        static MemorySegment shouldRasterize;
        static MemorySegment setShouldRasterize;
        static MemorySegment rasterizationScale;
        static MemorySegment setRasterizationScale;
        static MemorySegment drawsAsynchronously;
        static MemorySegment setDrawsAsynchronously;
        static MemorySegment allowsEdgeAntialiasing;
        static MemorySegment setAllowsEdgeAntialiasing;
        static MemorySegment edgeAntialiasingMask;
        static MemorySegment setEdgeAntialiasingMask;
        static MemorySegment contentsFormat;
        static MemorySegment setContentsFormat;
        static MemorySegment insertSublayer_atIndex;
        static MemorySegment insertSublayer_above;
        static MemorySegment insertSublayer_below;
        static MemorySegment replaceSublayer_with;
        static MemorySegment actionForKey;
        static MemorySegment animationKeys;
        static MemorySegment preferredFrameSize;
        static MemorySegment setNeedsLayout;
        static MemorySegment needsLayout;
        static MemorySegment layoutSublayers;
        static MemorySegment hitTest;
        static MemorySegment containsPoint;
        static MemorySegment convertPoint_fromLayer;
        static MemorySegment convertPoint_toLayer;
        static MemorySegment convertTime_fromLayer;
        static MemorySegment convertTime_toLayer;
        static MemorySegment renderInContext;
        static void populate() {
            alloc = ObjC.sel("alloc");
            init = ObjC.sel("init");
            cornerRadius = ObjC.sel("cornerRadius");
            setCornerRadius = ObjC.sel("setCornerRadius:");
            borderWidth = ObjC.sel("borderWidth");
            setBorderWidth = ObjC.sel("setBorderWidth:");
            borderColor = ObjC.sel("borderColor");
            setBorderColor = ObjC.sel("setBorderColor:");
            CGColor = ObjC.sel("CGColor");
            backgroundColor = ObjC.sel("backgroundColor");
            setBackgroundColor = ObjC.sel("setBackgroundColor:");
            masksToBounds = ObjC.sel("masksToBounds");
            setMasksToBounds = ObjC.sel("setMasksToBounds:");
            opacity = ObjC.sel("opacity");
            setOpacity = ObjC.sel("setOpacity:");
            addAnimation_forKey = ObjC.sel("addAnimation:forKey:");
            removeAnimationForKey = ObjC.sel("removeAnimationForKey:");
            animationForKey = ObjC.sel("animationForKey:");
            removeAllAnimations = ObjC.sel("removeAllAnimations");
            layoutIfNeeded = ObjC.sel("layoutIfNeeded");
            setNeedsDisplay = ObjC.sel("setNeedsDisplay");
            displayIfNeeded = ObjC.sel("displayIfNeeded");
            position = ObjC.sel("position");
            setPosition = ObjC.sel("setPosition:");
            anchorPoint = ObjC.sel("anchorPoint");
            setAnchorPoint = ObjC.sel("setAnchorPoint:");
            bounds = ObjC.sel("bounds");
            setBounds = ObjC.sel("setBounds:");
            frame = ObjC.sel("frame");
            setFrame = ObjC.sel("setFrame:");
            zPosition = ObjC.sel("zPosition");
            setZPosition = ObjC.sel("setZPosition:");
            contentsScale = ObjC.sel("contentsScale");
            setContentsScale = ObjC.sel("setContentsScale:");
            isHidden = ObjC.sel("isHidden");
            setHidden = ObjC.sel("setHidden:");
            contents = ObjC.sel("contents");
            setContents = ObjC.sel("setContents:");
            contentsGravity = ObjC.sel("contentsGravity");
            setContentsGravity = ObjC.sel("setContentsGravity:");
            shadowColor = ObjC.sel("shadowColor");
            setShadowColor = ObjC.sel("setShadowColor:");
            shadowOpacity = ObjC.sel("shadowOpacity");
            setShadowOpacity = ObjC.sel("setShadowOpacity:");
            shadowRadius = ObjC.sel("shadowRadius");
            setShadowRadius = ObjC.sel("setShadowRadius:");
            shadowOffset = ObjC.sel("shadowOffset");
            setShadowOffset = ObjC.sel("setShadowOffset:");
            addSublayer = ObjC.sel("addSublayer:");
            removeFromSuperlayer = ObjC.sel("removeFromSuperlayer");
            superlayer = ObjC.sel("superlayer");
            sublayers = ObjC.sel("sublayers");
            constraints = ObjC.sel("constraints");
            setConstraints = ObjC.sel("setConstraints:");
            addConstraint = ObjC.sel("addConstraint:");
            begin = ObjC.sel("begin");
            setAnimationDuration = ObjC.sel("setAnimationDuration:");
            commit = ObjC.sel("commit");
            transform = ObjC.sel("transform");
            setTransform = ObjC.sel("setTransform:");
            sublayerTransform = ObjC.sel("sublayerTransform");
            setSublayerTransform = ObjC.sel("setSublayerTransform:");
            mask = ObjC.sel("mask");
            setMask = ObjC.sel("setMask:");
            delegate = ObjC.sel("delegate");
            setDelegate = ObjC.sel("setDelegate:");
            name = ObjC.sel("name");
            setName = ObjC.sel("setName:");
            isOpaque = ObjC.sel("isOpaque");
            setOpaque = ObjC.sel("setOpaque:");
            contentsRect = ObjC.sel("contentsRect");
            setContentsRect = ObjC.sel("setContentsRect:");
            contentsCenter = ObjC.sel("contentsCenter");
            setContentsCenter = ObjC.sel("setContentsCenter:");
            magnificationFilter = ObjC.sel("magnificationFilter");
            setMagnificationFilter = ObjC.sel("setMagnificationFilter:");
            minificationFilter = ObjC.sel("minificationFilter");
            setMinificationFilter = ObjC.sel("setMinificationFilter:");
            minificationFilterBias = ObjC.sel("minificationFilterBias");
            setMinificationFilterBias = ObjC.sel("setMinificationFilterBias:");
            maskedCorners = ObjC.sel("maskedCorners");
            setMaskedCorners = ObjC.sel("setMaskedCorners:");
            cornerCurve = ObjC.sel("cornerCurve");
            setCornerCurve = ObjC.sel("setCornerCurve:");
            duration = ObjC.sel("duration");
            setDuration = ObjC.sel("setDuration:");
            beginTime = ObjC.sel("beginTime");
            setBeginTime = ObjC.sel("setBeginTime:");
            speed = ObjC.sel("speed");
            setSpeed = ObjC.sel("setSpeed:");
            timeOffset = ObjC.sel("timeOffset");
            setTimeOffset = ObjC.sel("setTimeOffset:");
            repeatCount = ObjC.sel("repeatCount");
            setRepeatCount = ObjC.sel("setRepeatCount:");
            repeatDuration = ObjC.sel("repeatDuration");
            setRepeatDuration = ObjC.sel("setRepeatDuration:");
            autoreverses = ObjC.sel("autoreverses");
            setAutoreverses = ObjC.sel("setAutoreverses:");
            fillMode = ObjC.sel("fillMode");
            setFillMode = ObjC.sel("setFillMode:");
            shouldRasterize = ObjC.sel("shouldRasterize");
            setShouldRasterize = ObjC.sel("setShouldRasterize:");
            rasterizationScale = ObjC.sel("rasterizationScale");
            setRasterizationScale = ObjC.sel("setRasterizationScale:");
            drawsAsynchronously = ObjC.sel("drawsAsynchronously");
            setDrawsAsynchronously = ObjC.sel("setDrawsAsynchronously:");
            allowsEdgeAntialiasing = ObjC.sel("allowsEdgeAntialiasing");
            setAllowsEdgeAntialiasing = ObjC.sel("setAllowsEdgeAntialiasing:");
            edgeAntialiasingMask = ObjC.sel("edgeAntialiasingMask");
            setEdgeAntialiasingMask = ObjC.sel("setEdgeAntialiasingMask:");
            contentsFormat = ObjC.sel("contentsFormat");
            setContentsFormat = ObjC.sel("setContentsFormat:");
            insertSublayer_atIndex = ObjC.sel("insertSublayer:atIndex:");
            insertSublayer_above = ObjC.sel("insertSublayer:above:");
            insertSublayer_below = ObjC.sel("insertSublayer:below:");
            replaceSublayer_with = ObjC.sel("replaceSublayer:with:");
            actionForKey = ObjC.sel("actionForKey:");
            animationKeys = ObjC.sel("animationKeys");
            preferredFrameSize = ObjC.sel("preferredFrameSize");
            setNeedsLayout = ObjC.sel("setNeedsLayout");
            needsLayout = ObjC.sel("needsLayout");
            layoutSublayers = ObjC.sel("layoutSublayers");
            hitTest = ObjC.sel("hitTest:");
            containsPoint = ObjC.sel("containsPoint:");
            convertPoint_fromLayer = ObjC.sel("convertPoint:fromLayer:");
            convertPoint_toLayer = ObjC.sel("convertPoint:toLayer:");
            convertTime_fromLayer = ObjC.sel("convertTime:fromLayer:");
            convertTime_toLayer = ObjC.sel("convertTime:toLayer:");
            renderInContext = ObjC.sel("renderInContext:");
        }
    }

            private record Handles(MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetFloat, MethodHandle hSetFloat, MethodHandle hGetId, MethodHandle hSetId, MethodHandle hGetPoint, MethodHandle hSetPoint, MethodHandle hGetSize, MethodHandle hSetSize, MethodHandle hGetBool, MethodHandle hSetBool, MethodHandle hGetRect, MethodHandle hSetRect, MethodHandle hGetInt, MethodHandle hSetInt, MethodHandle hGetIdId, MethodHandle hSetIdId, MethodHandle hSetIdInt, MethodHandle hGetIdPoint, MethodHandle hGetBoolPoint, MethodHandle hGetPointPointId, MethodHandle hGetDoubleDoubleId, MethodHandle hGetTransform, MethodHandle hSetTransform) {}
    private static volatile Handles handles;

    protected CALayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static CALayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CALayer(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        // Ensure QuartzCore is loaded so CALayer class is visible
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.POINT, Arg.POINT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE, Arg.DOUBLE, Arg.ID)),
                ObjC.handle(Sig.of(Ret.TRANSFORM3D)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.TRANSFORM3D))
        );
            Sels.populate();
        handles = h;
}

    /// `[[CALayer alloc] init]`
    public static CALayer create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("CALayer"), Sels.alloc);
        MemorySegment p = ObjC.msgSendId(alloc, Sels.init);
        if (p.address() == 0) throw new IllegalStateException("CALayer init returned nil");
        return new CALayer(p);
    }

    /// [layer cornerRadius] -> CGFloat
    public double cornerRadius() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.cornerRadius);
        } catch (Throwable t) {
            throw new RuntimeException("cornerRadius failed", t);
        }
    }

    /// [layer setCornerRadius:]
    public void setCornerRadius(double radius) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setCornerRadius, radius);
        } catch (Throwable t) {
            throw new RuntimeException("setCornerRadius: failed", t);
        }
    }

    /// [layer borderWidth] -> CGFloat
    public double borderWidth() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.borderWidth);
        } catch (Throwable t) {
            throw new RuntimeException("borderWidth failed", t);
        }
    }

    /// [layer setBorderWidth:]
    public void setBorderWidth(double width) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setBorderWidth, width);
        } catch (Throwable t) {
            throw new RuntimeException("setBorderWidth: failed", t);
        }
    }

    /// [layer borderColor] -> CGColorRef (as id)
    public MemorySegment borderColor() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, Sels.borderColor);
        } catch (Throwable t) {
            throw new RuntimeException("borderColor failed", t);
        }
    }

    /// [layer setBorderColor:] — CGColorRef
    public void setBorderColor(MemorySegment cgColor) {
        ensureInit();
        try {
            MemorySegment c = (cgColor == null || cgColor.address() == 0) ? MemorySegment.NULL : cgColor;
            handles.hSetId().invokeExact(peer, Sels.setBorderColor, c);
        } catch (Throwable t) {
            throw new RuntimeException("setBorderColor: failed", t);
        }
    }

    /// Convenience: set borderColor from NSColor via [NSColor CGColor]
    public void setBorderColor(NSColor color) {
        ensureInit();
        MemorySegment cg = MemorySegment.NULL;
        if (color != null) {
            MemorySegment p = ObjC.msgSendId(color.peer(), Sels.CGColor);
            if (p != null && p.address() != 0) cg = p;
        }
        setBorderColor(cg);
    }

    /// [layer backgroundColor] -> CGColorRef
    public MemorySegment backgroundColor() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, Sels.backgroundColor);
        } catch (Throwable t) {
            throw new RuntimeException("backgroundColor failed", t);
        }
    }

    /// [layer setBackgroundColor:] — CGColorRef
    public void setBackgroundColor(MemorySegment cgColor) {
        ensureInit();
        try {
            MemorySegment c = (cgColor == null || cgColor.address() == 0) ? MemorySegment.NULL : cgColor;
            handles.hSetId().invokeExact(peer, Sels.setBackgroundColor, c);
        } catch (Throwable t) {
            throw new RuntimeException("setBackgroundColor: failed", t);
        }
    }

    /// Convenience: set backgroundColor from NSColor
    public void setBackgroundColor(NSColor color) {
        ensureInit();
        MemorySegment cg = MemorySegment.NULL;
        if (color != null) {
            MemorySegment p = ObjC.msgSendId(color.peer(), Sels.CGColor);
            if (p != null && p.address() != 0) cg = p;
        }
        setBackgroundColor(cg);
    }

    /// [layer masksToBounds]
    public boolean masksToBounds() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.masksToBounds);
    }

    /// [layer setMasksToBounds:]
    public void setMasksToBounds(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setMasksToBounds, flag);
    }

    /// [layer opacity] -> float (0..1)
    public double opacity() {
        ensureInit();
        try {
            return (double) (float) handles.hGetFloat().invokeExact(peer, Sels.opacity);
        } catch (Throwable t) {
            throw new RuntimeException("opacity failed", t);
        }
    }

    /// [layer setOpacity:] — float. Also accepts double convenience.
    public void setOpacity(float o) {
        ensureInit();
        try {
            handles.hSetFloat().invokeExact(peer, Sels.setOpacity, o);
        } catch (Throwable t) {
            throw new RuntimeException("setOpacity: failed", t);
        }
    }

    public void setOpacity(double o) { setOpacity((float) o); }

    // ---- CAAnimation full compatibility ----

    /// [layer addAnimation:forKey:] — add CAAnimation (e.g., CABasicAnimation).
    public void addAnimation(CAAnimation anim, String key) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            MemorySegment k = key == null ? MemorySegment.NULL : ObjC.nsstring(key);
            MemorySegment animSeg = anim == null ? MemorySegment.NULL : anim.peer();
            h.invokeExact(peer, Sels.addAnimation_forKey, (MemorySegment) animSeg, (MemorySegment) k);
        } catch (Throwable t) { throw new RuntimeException("addAnimation:forKey: failed", t); }
    }

    /// [layer addAnimation:forKey:] raw MemorySegment variant.
    public void addAnimation(MemorySegment animPeer, String key) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            MemorySegment a = animPeer == null ? MemorySegment.NULL : animPeer;
            MemorySegment k = key == null ? MemorySegment.NULL : ObjC.nsstring(key);
            h.invokeExact(peer, Sels.addAnimation_forKey, (MemorySegment) a, (MemorySegment) k);
        } catch (Throwable t) { throw new RuntimeException("addAnimation:forKey: failed", t); }
    }

    /// [layer removeAnimationForKey:]
    public void removeAnimationForKey(String key) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            MemorySegment k = key == null ? MemorySegment.NULL : ObjC.nsstring(key);
            h.invokeExact(peer, Sels.removeAnimationForKey, (MemorySegment) k);
        } catch (Throwable t) { throw new RuntimeException("removeAnimationForKey: failed", t); }
    }

    /// [layer animationForKey:] -> CAAnimation or null.
    public CAAnimation animationForKey(String key) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            // NOTE: the (MemorySegment) cast matters — see setFromValue in CAAnimation;
            // without it javac types the ternary as Object and invokeExact throws
            // WrongMethodTypeException at runtime.
            MemorySegment p = (MemorySegment) h.invokeExact(peer, Sels.animationForKey, (MemorySegment) (key == null ? MemorySegment.NULL : ObjC.nsstring(key)));
            return CAAnimation.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("animationForKey: failed", t); }
    }

    /// [layer removeAllAnimations]
    public void removeAllAnimations() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.removeAllAnimations);
    }

    /// [layer layoutIfNeeded] — force layout.
    public void layoutIfNeeded() { ensureInit(); ObjC.msgSendVoid(peer, Sels.layoutIfNeeded); }

    /// [layer setNeedsDisplay]
    public void setNeedsDisplay() { ensureInit(); ObjC.msgSendVoid(peer, Sels.setNeedsDisplay); }

    /// [layer displayIfNeeded]
    public void displayIfNeeded() { ensureInit(); ObjC.msgSendVoid(peer, Sels.displayIfNeeded); }

    // ---- geometry / visibility / contents / shadow / tree (CoreAnimation coverage) ----

    /// [layer position] — the layer's position in the superlayer's coordinate space.
    public NSPoint position() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetPoint().invokeExact(ObjC.structSlot(), peer, Sels.position);
            return NSPoint.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("position failed", t); }
    }

    /// [layer setPosition:]
    public void setPosition(NSPoint p) {
        ensureInit();
        try {
            handles.hSetPoint().invokeExact(peer, Sels.setPosition, p.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setPosition: failed", t); }
    }

    /// [layer anchorPoint] — matches position within bounds, in unit coordinates.
    public NSPoint anchorPoint() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetPoint().invokeExact(ObjC.structSlot(), peer, Sels.anchorPoint);
            return NSPoint.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("anchorPoint failed", t); }
    }

    /// [layer setAnchorPoint:]
    public void setAnchorPoint(NSPoint p) {
        ensureInit();
        try {
            handles.hSetPoint().invokeExact(peer, Sels.setAnchorPoint, p.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setAnchorPoint: failed", t); }
    }

    /// [layer bounds] — the layer's bounds in its own coordinate space.
    public NSRect bounds() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetRect().invokeExact(ObjC.structSlot(), peer, Sels.bounds);
            return NSRect.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("bounds failed", t); }
    }

    /// [layer setBounds:]
    public void setBounds(NSRect bounds) {
        ensureInit();
        try {
            handles.hSetRect().invokeExact(peer, Sels.setBounds, bounds.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setBounds: failed", t); }
    }

    /// [layer frame] — the layer's frame in the superlayer's coordinate space.
    public NSRect frame() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetRect().invokeExact(ObjC.structSlot(), peer, Sels.frame);
            return NSRect.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("frame failed", t); }
    }

    /// [layer setFrame:]
    public void setFrame(NSRect frame) {
        ensureInit();
        try {
            handles.hSetRect().invokeExact(peer, Sels.setFrame, frame.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setFrame: failed", t); }
    }

    /// [layer zPosition] — depth ordering above/below sibling layers.
    public double zPosition() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.zPosition);
        } catch (Throwable t) { throw new RuntimeException("zPosition failed", t); }
    }

    /// [layer setZPosition:]
    public void setZPosition(double z) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setZPosition, z);
        } catch (Throwable t) { throw new RuntimeException("setZPosition: failed", t); }
    }

    /// [layer contentsScale] — scale factor for backing store / rendering.
    public double contentsScale() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.contentsScale);
        } catch (Throwable t) { throw new RuntimeException("contentsScale failed", t); }
    }

    /// [layer setContentsScale:]
    public void setContentsScale(double scale) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setContentsScale, scale);
        } catch (Throwable t) { throw new RuntimeException("setContentsScale: failed", t); }
    }

    /// [layer isHidden]
    public boolean isHidden() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, Sels.isHidden);
        } catch (Throwable t) { throw new RuntimeException("isHidden failed", t); }
    }

    /// [layer setHidden:]
    public void setHidden(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setHidden, flag);
        } catch (Throwable t) { throw new RuntimeException("setHidden: failed", t); }
    }

    /// [layer contents] — raw layer contents object (typically a CGImageRef).
    public MemorySegment contents() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, Sels.contents);
        } catch (Throwable t) { throw new RuntimeException("contents failed", t); }
    }

    /// [layer setContents:] — raw contents (CGImageRef or any object; NULL clears).
    public void setContents(MemorySegment cgImageRaw) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setContents, (MemorySegment) (cgImageRaw == null ? MemorySegment.NULL : cgImageRaw));
        } catch (Throwable t) { throw new RuntimeException("setContents: failed", t); }
    }

    /// [layer contentsGravity] — one of the kCAGravity* string constants.
    public String contentsGravity() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, Sels.contentsGravity));
        } catch (Throwable t) { throw new RuntimeException("contentsGravity failed", t); }
    }

    /// [layer setContentsGravity:] — e.g. "resize", "center", "resizeAspect".
    public void setContentsGravity(String gravity) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setContentsGravity, (MemorySegment) (gravity == null ? MemorySegment.NULL : ObjC.nsstring(gravity)));
        } catch (Throwable t) { throw new RuntimeException("setContentsGravity: failed", t); }
    }

    /// [layer shadowColor] — raw CGColorRef.
    public MemorySegment shadowColor() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, Sels.shadowColor);
        } catch (Throwable t) { throw new RuntimeException("shadowColor failed", t); }
    }

    /// [layer setShadowColor:] — raw CGColorRef (NULL clears).
    public void setShadowColor(MemorySegment cgColor) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setShadowColor, (MemorySegment) (cgColor == null ? MemorySegment.NULL : cgColor));
        } catch (Throwable t) { throw new RuntimeException("setShadowColor: failed", t); }
    }

    /// Convenience: set shadowColor from NSColor via its CGColor.
    public void setShadowColor(NSColor color) {
        setShadowColor((MemorySegment) (color == null ? MemorySegment.NULL : color.cgColor()));
    }

    /// [layer shadowOpacity] — 0..1 (declared `float` in the SDK, like opacity).
    public double shadowOpacity() {
        ensureInit();
        try {
            return (double) (float) handles.hGetFloat().invokeExact(peer, Sels.shadowOpacity);
        } catch (Throwable t) { throw new RuntimeException("shadowOpacity failed", t); }
    }

    /// [layer setShadowOpacity:]
    public void setShadowOpacity(double opacity) {
        ensureInit();
        try {
            handles.hSetFloat().invokeExact(peer, Sels.setShadowOpacity, (float) opacity);
        } catch (Throwable t) { throw new RuntimeException("setShadowOpacity: failed", t); }
    }

    /// [layer shadowRadius] — blur radius in points.
    public double shadowRadius() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.shadowRadius);
        } catch (Throwable t) { throw new RuntimeException("shadowRadius failed", t); }
    }

    /// [layer setShadowRadius:]
    public void setShadowRadius(double radius) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setShadowRadius, radius);
        } catch (Throwable t) { throw new RuntimeException("setShadowRadius: failed", t); }
    }

    /// [layer shadowOffset]
    public NSSize shadowOffset() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.shadowOffset);
            return NSSize.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("shadowOffset failed", t); }
    }

    /// [layer setShadowOffset:]
    public void setShadowOffset(NSSize offset) {
        ensureInit();
        try {
            handles.hSetSize().invokeExact(peer, Sels.setShadowOffset, offset.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setShadowOffset: failed", t); }
    }

    /// [layer addSublayer:] — append a child layer (deduped onto the VOID,id handle).
    public void addSublayer(CALayer sublayer) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.addSublayer, (MemorySegment) (sublayer == null ? MemorySegment.NULL : sublayer.peer()));
        } catch (Throwable t) { throw new RuntimeException("addSublayer: failed", t); }
    }

    /// [layer removeFromSuperlayer]
    public void removeFromSuperlayer() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.removeFromSuperlayer);
    }

    /// [layer superlayer] — parent layer or null.
    public CALayer superlayer() {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hGetId().invokeExact(peer, Sels.superlayer));
        } catch (Throwable t) { throw new RuntimeException("superlayer failed", t); }
    }

    /// [layer sublayers] — child layers as NSArray (null when the layer has none).
    public NSArray sublayers() {
        ensureInit();
        try {
            return NSArray.wrap((MemorySegment) handles.hGetId().invokeExact(peer, Sels.sublayers));
        } catch (Throwable t) { throw new RuntimeException("sublayers failed", t); }
    }

    /// [layer constraints] — CAConstraints driving CAConstraintLayoutManager.
    public NSArray constraints() {
        ensureInit();
        try {
            return NSArray.wrap((MemorySegment) handles.hGetId().invokeExact(peer, Sels.constraints));
        } catch (Throwable t) { throw new RuntimeException("constraints failed", t); }
    }

    /// [layer setConstraints:] — install constraints (needs a layout manager).
    public void setConstraints(NSArray constraints) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setConstraints,
                    (MemorySegment) (constraints == null ? MemorySegment.NULL : constraints.peer()));
        } catch (Throwable t) { throw new RuntimeException("setConstraints: failed", t); }
    }

    /// [layer addConstraint:] — append one CAConstraint.
    public void addConstraint(CAConstraint constraint) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.addConstraint,
                    (MemorySegment) (constraint == null ? MemorySegment.NULL : constraint.peer()));
        } catch (Throwable t) { throw new RuntimeException("addConstraint: failed", t); }
    }

    /// [CALayer needsDisplay] helper via CATransaction
    public static void transaction(Runnable block, double duration) {
        ensureInit();
        try {
            // [CATransaction begin]; [CATransaction setAnimationDuration:duration]; block; [CATransaction commit];
            MethodHandle hBegin = ObjC.handle(Sig.of(Ret.VOID));
            MethodHandle hCommit = ObjC.handle(Sig.of(Ret.VOID));
            MethodHandle hSetDur = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
            MemorySegment cls = ObjC.cls("CATransaction");
            hBegin.invokeExact(cls, Sels.begin);
            hSetDur.invokeExact(cls, Sels.setAnimationDuration, duration);
            try {
                block.run();
            } finally {
                hCommit.invokeExact(cls, Sels.commit);
            }
        } catch (Throwable t) { throw new RuntimeException("CATransaction failed", t); }
    }
    // ---- Core Animation completeness: transforms, contents geometry, media timing,
    // ---- filters, corner masking, rasterization, sublayer ordering, actions and
    // ---- coordinate/time conversion (CALayer public API) ----

    /// [layer transform] — the 4x4 matrix applied about the anchor point (by value, 128 bytes).
    public CATransform3D transform() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetTransform().invokeExact(CATransform3D.slot(), peer, Sels.transform);
            return CATransform3D.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("transform failed", t); }
    }

    /// [layer setTransform:] — model-value transform (animatable).
    public void setTransform(CATransform3D transform) {
        ensureInit();
        try {
            handles.hSetTransform().invokeExact(peer, Sels.setTransform, transform.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setTransform: failed", t); }
    }

    /// [layer sublayerTransform] — transform applied to every sublayer.
    public CATransform3D sublayerTransform() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetTransform().invokeExact(CATransform3D.slot(), peer, Sels.sublayerTransform);
            return CATransform3D.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("sublayerTransform failed", t); }
    }

    /// [layer setSublayerTransform:]
    public void setSublayerTransform(CATransform3D transform) {
        ensureInit();
        try {
            handles.hSetTransform().invokeExact(peer, Sels.setSublayerTransform, transform.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setSublayerTransform: failed", t); }
    }

    /// [layer mask] — the mask layer (strong) or null.
    public MemorySegment mask() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, Sels.mask);
        } catch (Throwable t) { throw new RuntimeException("mask failed", t); }
    }

    /// [layer setMask:] — raw mask layer peer (NULL clears).
    public void setMask(MemorySegment maskPeer) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setMask, (MemorySegment) (maskPeer == null ? MemorySegment.NULL : maskPeer));
        } catch (Throwable t) { throw new RuntimeException("setMask: failed", t); }
    }

    /// [layer setMask:] — typed convenience.
    public void setMask(CALayer maskLayer) {
        setMask((MemorySegment) (maskLayer == null ? MemorySegment.NULL : maskLayer.peer()));
    }

    /// [layer delegate] — the CALayerDelegate (weak, not retained) or null.
    public MemorySegment delegate() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, Sels.delegate);
        } catch (Throwable t) { throw new RuntimeException("delegate failed", t); }
    }

    /// [layer setDelegate:] — raw delegate peer (weak).
    public void setDelegate(MemorySegment delegatePeer) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setDelegate, (MemorySegment) (delegatePeer == null ? MemorySegment.NULL : delegatePeer));
        } catch (Throwable t) { throw new RuntimeException("setDelegate: failed", t); }
    }

    /// [layer name] — layout-manager name (nil-safe).
    public String name() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, Sels.name));
        } catch (Throwable t) { throw new RuntimeException("name failed", t); }
    }

    /// [layer setName:]
    public void setName(String name) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setName,
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)));
        } catch (Throwable t) { throw new RuntimeException("setName: failed", t); }
    }

    /// [layer isOpaque] — opaque-content hint.
    public boolean isOpaque() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, Sels.isOpaque);
        } catch (Throwable t) { throw new RuntimeException("isOpaque failed", t); }
    }

    /// [layer setOpaque:]
    public void setOpaque(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setOpaque, flag);
        } catch (Throwable t) { throw new RuntimeException("setOpaque: failed", t); }
    }

    /// [layer contentsRect] — unit-rectangle of the contents to display.
    public NSRect contentsRect() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetRect().invokeExact(ObjC.structSlot(), peer, Sels.contentsRect);
            return NSRect.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("contentsRect failed", t); }
    }

    /// [layer setContentsRect:]
    public void setContentsRect(NSRect rect) {
        ensureInit();
        try {
            handles.hSetRect().invokeExact(peer, Sels.setContentsRect, rect.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setContentsRect: failed", t); }
    }

    /// [layer contentsCenter] — the stretchable region of the contents.
    public NSRect contentsCenter() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetRect().invokeExact(ObjC.structSlot(), peer, Sels.contentsCenter);
            return NSRect.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("contentsCenter failed", t); }
    }

    /// [layer setContentsCenter:]
    public void setContentsCenter(NSRect rect) {
        ensureInit();
        try {
            handles.hSetRect().invokeExact(peer, Sels.setContentsCenter, rect.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setContentsCenter: failed", t); }
    }

    /// [layer magnificationFilter] — e.g. "linear", "nearest", "trilinear".
    public String magnificationFilter() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, Sels.magnificationFilter));
        } catch (Throwable t) { throw new RuntimeException("magnificationFilter failed", t); }
    }

    /// [layer setMagnificationFilter:]
    public void setMagnificationFilter(String filter) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setMagnificationFilter,
                    (MemorySegment) (filter == null ? MemorySegment.NULL : ObjC.nsstring(filter)));
        } catch (Throwable t) { throw new RuntimeException("setMagnificationFilter: failed", t); }
    }

    /// [layer minificationFilter] — e.g. "linear", "nearest", "trilinear".
    public String minificationFilter() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, Sels.minificationFilter));
        } catch (Throwable t) { throw new RuntimeException("minificationFilter failed", t); }
    }

    /// [layer setMinificationFilter:]
    public void setMinificationFilter(String filter) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setMinificationFilter,
                    (MemorySegment) (filter == null ? MemorySegment.NULL : ObjC.nsstring(filter)));
        } catch (Throwable t) { throw new RuntimeException("setMinificationFilter: failed", t); }
    }

    /// [layer minificationFilterBias] — float, declared float in the SDK.
    public float minificationFilterBias() {
        ensureInit();
        try {
            return (float) handles.hGetFloat().invokeExact(peer, Sels.minificationFilterBias);
        } catch (Throwable t) { throw new RuntimeException("minificationFilterBias failed", t); }
    }

    /// [layer setMinificationFilterBias:]
    public void setMinificationFilterBias(float bias) {
        ensureInit();
        try {
            handles.hSetFloat().invokeExact(peer, Sels.setMinificationFilterBias, bias);
        } catch (Throwable t) { throw new RuntimeException("setMinificationFilterBias: failed", t); }
    }

    /// [layer maskedCorners] — CACornerMask (NSUInteger bitmask).
    public long maskedCorners() {
        ensureInit();
        try {
            return (long) handles.hGetInt().invokeExact(peer, Sels.maskedCorners);
        } catch (Throwable t) { throw new RuntimeException("maskedCorners failed", t); }
    }

    /// [layer setMaskedCorners:]
    public void setMaskedCorners(long corners) {
        ensureInit();
        try {
            handles.hSetInt().invokeExact(peer, Sels.setMaskedCorners, corners);
        } catch (Throwable t) { throw new RuntimeException("setMaskedCorners: failed", t); }
    }

    /// [layer cornerCurve] — "circular" (default) or "continuous".
    public String cornerCurve() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, Sels.cornerCurve));
        } catch (Throwable t) { throw new RuntimeException("cornerCurve failed", t); }
    }

    /// [layer setCornerCurve:]
    public void setCornerCurve(String curve) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setCornerCurve,
                    (MemorySegment) (curve == null ? MemorySegment.NULL : ObjC.nsstring(curve)));
        } catch (Throwable t) { throw new RuntimeException("setCornerCurve: failed", t); }
    }

    /// [layer duration] — CAMediaTiming duration (seconds).
    public double duration() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.duration);
        } catch (Throwable t) { throw new RuntimeException("duration failed", t); }
    }

    /// [layer setDuration:]
    public void setDuration(double duration) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setDuration, duration);
        } catch (Throwable t) { throw new RuntimeException("setDuration: failed", t); }
    }

    /// [layer beginTime] — CAMediaTiming begin time (seconds).
    public double beginTime() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.beginTime);
        } catch (Throwable t) { throw new RuntimeException("beginTime failed", t); }
    }

    /// [layer setBeginTime:]
    public void setBeginTime(double beginTime) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setBeginTime, beginTime);
        } catch (Throwable t) { throw new RuntimeException("setBeginTime: failed", t); }
    }

    /// [layer speed] — CAMediaTiming time-scale (float, declared float in the SDK).
    public float speed() {
        ensureInit();
        try {
            return (float) handles.hGetFloat().invokeExact(peer, Sels.speed);
        } catch (Throwable t) { throw new RuntimeException("speed failed", t); }
    }

    /// [layer setSpeed:]
    public void setSpeed(float speed) {
        ensureInit();
        try {
            handles.hSetFloat().invokeExact(peer, Sels.setSpeed, speed);
        } catch (Throwable t) { throw new RuntimeException("setSpeed: failed", t); }
    }

    /// [layer timeOffset] — CAMediaTiming time offset (seconds).
    public double timeOffset() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.timeOffset);
        } catch (Throwable t) { throw new RuntimeException("timeOffset failed", t); }
    }

    /// [layer setTimeOffset:]
    public void setTimeOffset(double timeOffset) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setTimeOffset, timeOffset);
        } catch (Throwable t) { throw new RuntimeException("setTimeOffset: failed", t); }
    }

    /// [layer repeatCount] — CAMediaTiming repeat count (float).
    public float repeatCount() {
        ensureInit();
        try {
            return (float) handles.hGetFloat().invokeExact(peer, Sels.repeatCount);
        } catch (Throwable t) { throw new RuntimeException("repeatCount failed", t); }
    }

    /// [layer setRepeatCount:]
    public void setRepeatCount(float count) {
        ensureInit();
        try {
            handles.hSetFloat().invokeExact(peer, Sels.setRepeatCount, count);
        } catch (Throwable t) { throw new RuntimeException("setRepeatCount: failed", t); }
    }

    /// [layer repeatDuration] — CAMediaTiming repeat duration (seconds).
    public double repeatDuration() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.repeatDuration);
        } catch (Throwable t) { throw new RuntimeException("repeatDuration failed", t); }
    }

    /// [layer setRepeatDuration:]
    public void setRepeatDuration(double repeatDuration) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setRepeatDuration, repeatDuration);
        } catch (Throwable t) { throw new RuntimeException("setRepeatDuration: failed", t); }
    }

    /// [layer autoreverses] — CAMediaTiming auto-reverse flag.
    public boolean autoreverses() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, Sels.autoreverses);
        } catch (Throwable t) { throw new RuntimeException("autoreverses failed", t); }
    }

    /// [layer setAutoreverses:]
    public void setAutoreverses(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setAutoreverses, flag);
        } catch (Throwable t) { throw new RuntimeException("setAutoreverses: failed", t); }
    }

    /// [layer fillMode] — e.g. "forwards", "backwards", "both", "removed".
    public String fillMode() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, Sels.fillMode));
        } catch (Throwable t) { throw new RuntimeException("fillMode failed", t); }
    }

    /// [layer setFillMode:]
    public void setFillMode(String mode) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setFillMode,
                    (MemorySegment) (mode == null ? MemorySegment.NULL : ObjC.nsstring(mode)));
        } catch (Throwable t) { throw new RuntimeException("setFillMode: failed", t); }
    }

    /// [layer shouldRasterize] — composite the layer into an offscreen bitmap.
    public boolean shouldRasterize() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, Sels.shouldRasterize);
        } catch (Throwable t) { throw new RuntimeException("shouldRasterize failed", t); }
    }

    /// [layer setShouldRasterize:]
    public void setShouldRasterize(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setShouldRasterize, flag);
        } catch (Throwable t) { throw new RuntimeException("setShouldRasterize: failed", t); }
    }

    /// [layer rasterizationScale] — scale of the rasterized bitmap.
    public double rasterizationScale() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, Sels.rasterizationScale);
        } catch (Throwable t) { throw new RuntimeException("rasterizationScale failed", t); }
    }

    /// [layer setRasterizationScale:]
    public void setRasterizationScale(double scale) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, Sels.setRasterizationScale, scale);
        } catch (Throwable t) { throw new RuntimeException("setRasterizationScale: failed", t); }
    }

    /// [layer drawsAsynchronously] — defer drawing to a background thread.
    public boolean drawsAsynchronously() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, Sels.drawsAsynchronously);
        } catch (Throwable t) { throw new RuntimeException("drawsAsynchronously failed", t); }
    }

    /// [layer setDrawsAsynchronously:]
    public void setDrawsAsynchronously(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setDrawsAsynchronously, flag);
        } catch (Throwable t) { throw new RuntimeException("setDrawsAsynchronously: failed", t); }
    }

    /// [layer allowsEdgeAntialiasing] — antialias transformed edges.
    public boolean allowsEdgeAntialiasing() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, Sels.allowsEdgeAntialiasing);
        } catch (Throwable t) { throw new RuntimeException("allowsEdgeAntialiasing failed", t); }
    }

    /// [layer setAllowsEdgeAntialiasing:]
    public void setAllowsEdgeAntialiasing(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, Sels.setAllowsEdgeAntialiasing, flag);
        } catch (Throwable t) { throw new RuntimeException("setAllowsEdgeAntialiasing: failed", t); }
    }

    /// [layer edgeAntialiasingMask] — CAEdgeAntialiasingMask (unsigned int bitmask).
    public long edgeAntialiasingMask() {
        ensureInit();
        try {
            return (long) handles.hGetInt().invokeExact(peer, Sels.edgeAntialiasingMask);
        } catch (Throwable t) { throw new RuntimeException("edgeAntialiasingMask failed", t); }
    }

    /// [layer setEdgeAntialiasingMask:]
    public void setEdgeAntialiasingMask(long mask) {
        ensureInit();
        try {
            handles.hSetInt().invokeExact(peer, Sels.setEdgeAntialiasingMask, mask);
        } catch (Throwable t) { throw new RuntimeException("setEdgeAntialiasingMask: failed", t); }
    }

    /// [layer contentsFormat] — storage-format hint (kCAContentsFormat*), nil-safe.
    public String contentsFormat() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, Sels.contentsFormat));
        } catch (Throwable t) { throw new RuntimeException("contentsFormat failed", t); }
    }

    /// [layer setContentsFormat:] — e.g. "RGBA8Uint", "Gray8Uint".
    public void setContentsFormat(String format) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.setContentsFormat,
                    (MemorySegment) (format == null ? MemorySegment.NULL : ObjC.nsstring(format)));
        } catch (Throwable t) { throw new RuntimeException("setContentsFormat: failed", t); }
    }

    /// [layer insertSublayer:atIndex:] — positional splice.
    public void insertSublayer(CALayer sublayer, long index) {
        ensureInit();
        try {
            handles.hSetIdInt().invokeExact(peer, Sels.insertSublayer_atIndex,
                    (MemorySegment) (sublayer == null ? MemorySegment.NULL : sublayer.peer()), index);
        } catch (Throwable t) { throw new RuntimeException("insertSublayer:atIndex: failed", t); }
    }

    /// [layer insertSublayer:above:] — place directly above a sibling.
    public void insertSublayerAbove(CALayer sublayer, CALayer sibling) {
        ensureInit();
        try {
            handles.hSetIdId().invokeExact(peer, Sels.insertSublayer_above,
                    (MemorySegment) (sublayer == null ? MemorySegment.NULL : sublayer.peer()),
                    (MemorySegment) (sibling == null ? MemorySegment.NULL : sibling.peer()));
        } catch (Throwable t) { throw new RuntimeException("insertSublayer:above: failed", t); }
    }

    /// [layer insertSublayer:below:] — place directly below a sibling.
    public void insertSublayerBelow(CALayer sublayer, CALayer sibling) {
        ensureInit();
        try {
            handles.hSetIdId().invokeExact(peer, Sels.insertSublayer_below,
                    (MemorySegment) (sublayer == null ? MemorySegment.NULL : sublayer.peer()),
                    (MemorySegment) (sibling == null ? MemorySegment.NULL : sibling.peer()));
        } catch (Throwable t) { throw new RuntimeException("insertSublayer:below: failed", t); }
    }

    /// [layer replaceSublayer:with:] — in-place swap preserving index.
    public void replaceSublayer(CALayer oldLayer, CALayer newLayer) {
        ensureInit();
        try {
            handles.hSetIdId().invokeExact(peer, Sels.replaceSublayer_with,
                    (MemorySegment) (oldLayer == null ? MemorySegment.NULL : oldLayer.peer()),
                    (MemorySegment) (newLayer == null ? MemorySegment.NULL : newLayer.peer()));
        } catch (Throwable t) { throw new RuntimeException("replaceSublayer:with: failed", t); }
    }

    /// [layer actionForKey:] — the implicit-action object for an event, or null.
    public MemorySegment actionForKey(String key) {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetIdId().invokeExact(peer, Sels.actionForKey,
                    (MemorySegment) (key == null ? MemorySegment.NULL : ObjC.nsstring(key)));
        } catch (Throwable t) { throw new RuntimeException("actionForKey: failed", t); }
    }

    /// [layer animationKeys] — keys of the animations currently attached, or null.
    public NSArray animationKeys() {
        ensureInit();
        try {
            return NSArray.wrap((MemorySegment) handles.hGetId().invokeExact(peer, Sels.animationKeys));
        } catch (Throwable t) { throw new RuntimeException("animationKeys failed", t); }
    }

    /// [layer preferredFrameSize] — bounds mapped into the superlayer (read-only).
    public NSSize preferredFrameSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.preferredFrameSize);
            return NSSize.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("preferredFrameSize failed", t); }
    }

    /// [layer setNeedsLayout] — mark -layoutSublayers for the next update.
    public void setNeedsLayout() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.setNeedsLayout);
    }

    /// [layer needsLayout] — whether layout is pending.
    public boolean needsLayout() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, Sels.needsLayout);
        } catch (Throwable t) { throw new RuntimeException("needsLayout failed", t); }
    }

    /// [layer layoutSublayers] — run the layer's own sublayer layout now.
    public void layoutSublayers() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.layoutSublayers);
    }

    /// [layer hitTest:] — deepest descendant containing the point, or null.
    public CALayer hitTest(NSPoint point) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hGetIdPoint().invokeExact(peer, Sels.hitTest, point.toSegment()));
        } catch (Throwable t) { throw new RuntimeException("hitTest: failed", t); }
    }

    /// [layer containsPoint:] — whether the point lies inside the layer's bounds.
    public boolean containsPoint(NSPoint point) {
        ensureInit();
        try {
            return (boolean) handles.hGetBoolPoint().invokeExact(peer, Sels.containsPoint, point.toSegment());
        } catch (Throwable t) { throw new RuntimeException("containsPoint: failed", t); }
    }

    /// [layer convertPoint:fromLayer:] — map a point from the given layer's space into this layer's.
    public NSPoint convertPointFromLayer(NSPoint point, CALayer fromLayer) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetPointPointId().invokeExact(ObjC.structSlot(), peer, Sels.convertPoint_fromLayer,
                    point.toSegment(), (MemorySegment) (fromLayer == null ? MemorySegment.NULL : fromLayer.peer()));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("convertPoint:fromLayer: failed", t); }
    }

    /// [layer convertPoint:toLayer:] — map a point from this layer's space into the given layer's.
    public NSPoint convertPointToLayer(NSPoint point, CALayer toLayer) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetPointPointId().invokeExact(ObjC.structSlot(), peer, Sels.convertPoint_toLayer,
                    point.toSegment(), (MemorySegment) (toLayer == null ? MemorySegment.NULL : toLayer.peer()));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("convertPoint:toLayer: failed", t); }
    }

    /// [layer convertTime:fromLayer:] — map a time from the given layer's space into this layer's.
    public double convertTimeFromLayer(double time, CALayer fromLayer) {
        ensureInit();
        try {
            return (double) handles.hGetDoubleDoubleId().invokeExact(peer, Sels.convertTime_fromLayer,
                    time, (MemorySegment) (fromLayer == null ? MemorySegment.NULL : fromLayer.peer()));
        } catch (Throwable t) { throw new RuntimeException("convertTime:fromLayer: failed", t); }
    }

    /// [layer convertTime:toLayer:] — map a time from this layer's space into the given layer's.
    public double convertTimeToLayer(double time, CALayer toLayer) {
        ensureInit();
        try {
            return (double) handles.hGetDoubleDoubleId().invokeExact(peer, Sels.convertTime_toLayer,
                    time, (MemorySegment) (toLayer == null ? MemorySegment.NULL : toLayer.peer()));
        } catch (Throwable t) { throw new RuntimeException("convertTime:toLayer: failed", t); }
    }

    /// [layer renderInContext:] — draw the layer tree into a CGContextRef.
    public void renderInContext(MemorySegment cgContext) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, Sels.renderInContext,
                    (MemorySegment) (cgContext == null ? MemorySegment.NULL : cgContext));
        } catch (Throwable t) { throw new RuntimeException("renderInContext: failed", t); }
    }
}
