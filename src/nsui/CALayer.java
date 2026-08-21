package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
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

            private record Handles(MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetFloat, MethodHandle hSetFloat, MethodHandle hGetId, MethodHandle hSetId, MethodHandle hGetPoint, MethodHandle hSetPoint, MethodHandle hGetSize, MethodHandle hSetSize, MethodHandle hGetBool, MethodHandle hSetBool) {}
    private static volatile Handles handles;

    protected CALayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static CALayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CALayer(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        // Ensure QuartzCore is loaded so CALayer class is visible
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
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
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL))
        );
    }

    /// `[[CALayer alloc] init]`
    public static CALayer create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("CALayer"), ObjC.sel("alloc"));
        MemorySegment p = ObjC.msgSendId(alloc, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("CALayer init returned nil");
        return new CALayer(p);
    }

    /// [layer cornerRadius] -> CGFloat
    public double cornerRadius() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("cornerRadius"));
        } catch (Throwable t) {
            throw new RuntimeException("cornerRadius failed", t);
        }
    }

    /// [layer setCornerRadius:]
    public void setCornerRadius(double radius) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setCornerRadius:"), radius);
        } catch (Throwable t) {
            throw new RuntimeException("setCornerRadius: failed", t);
        }
    }

    /// [layer borderWidth] -> CGFloat
    public double borderWidth() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("borderWidth"));
        } catch (Throwable t) {
            throw new RuntimeException("borderWidth failed", t);
        }
    }

    /// [layer setBorderWidth:]
    public void setBorderWidth(double width) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setBorderWidth:"), width);
        } catch (Throwable t) {
            throw new RuntimeException("setBorderWidth: failed", t);
        }
    }

    /// [layer borderColor] -> CGColorRef (as id)
    public MemorySegment borderColor() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("borderColor"));
        } catch (Throwable t) {
            throw new RuntimeException("borderColor failed", t);
        }
    }

    /// [layer setBorderColor:] — CGColorRef
    public void setBorderColor(MemorySegment cgColor) {
        ensureInit();
        try {
            MemorySegment c = (cgColor == null || cgColor.address() == 0) ? MemorySegment.NULL : cgColor;
            handles.hSetId().invokeExact(peer, ObjC.sel("setBorderColor:"), c);
        } catch (Throwable t) {
            throw new RuntimeException("setBorderColor: failed", t);
        }
    }

    /// Convenience: set borderColor from NSColor via [NSColor CGColor]
    public void setBorderColor(NSColor color) {
        MemorySegment cg = MemorySegment.NULL;
        if (color != null) {
            MemorySegment p = ObjC.msgSendId(color.peer(), ObjC.sel("CGColor"));
            if (p != null && p.address() != 0) cg = p;
        }
        setBorderColor(cg);
    }

    /// [layer backgroundColor] -> CGColorRef
    public MemorySegment backgroundColor() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("backgroundColor"));
        } catch (Throwable t) {
            throw new RuntimeException("backgroundColor failed", t);
        }
    }

    /// [layer setBackgroundColor:] — CGColorRef
    public void setBackgroundColor(MemorySegment cgColor) {
        ensureInit();
        try {
            MemorySegment c = (cgColor == null || cgColor.address() == 0) ? MemorySegment.NULL : cgColor;
            handles.hSetId().invokeExact(peer, ObjC.sel("setBackgroundColor:"), c);
        } catch (Throwable t) {
            throw new RuntimeException("setBackgroundColor: failed", t);
        }
    }

    /// Convenience: set backgroundColor from NSColor
    public void setBackgroundColor(NSColor color) {
        MemorySegment cg = MemorySegment.NULL;
        if (color != null) {
            MemorySegment p = ObjC.msgSendId(color.peer(), ObjC.sel("CGColor"));
            if (p != null && p.address() != 0) cg = p;
        }
        setBackgroundColor(cg);
    }

    /// [layer masksToBounds]
    public boolean masksToBounds() {
        ensureInit();
        return ObjC.msgSendBool(peer, ObjC.sel("masksToBounds"));
    }

    /// [layer setMasksToBounds:]
    public void setMasksToBounds(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, ObjC.sel("setMasksToBounds:"), flag);
    }

    /// [layer opacity] -> float (0..1)
    public double opacity() {
        ensureInit();
        try {
            return (double) (float) handles.hGetFloat().invokeExact(peer, ObjC.sel("opacity"));
        } catch (Throwable t) {
            throw new RuntimeException("opacity failed", t);
        }
    }

    /// [layer setOpacity:] — float. Also accepts double convenience.
    public void setOpacity(float o) {
        ensureInit();
        try {
            handles.hSetFloat().invokeExact(peer, ObjC.sel("setOpacity:"), o);
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
            h.invokeExact(peer, ObjC.sel("addAnimation:forKey:"), (MemorySegment) animSeg, (MemorySegment) k);
        } catch (Throwable t) { throw new RuntimeException("addAnimation:forKey: failed", t); }
    }

    /// [layer addAnimation:forKey:] raw MemorySegment variant.
    public void addAnimation(MemorySegment animPeer, String key) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            MemorySegment a = animPeer == null ? MemorySegment.NULL : animPeer;
            MemorySegment k = key == null ? MemorySegment.NULL : ObjC.nsstring(key);
            h.invokeExact(peer, ObjC.sel("addAnimation:forKey:"), (MemorySegment) a, (MemorySegment) k);
        } catch (Throwable t) { throw new RuntimeException("addAnimation:forKey: failed", t); }
    }

    /// [layer removeAnimationForKey:]
    public void removeAnimationForKey(String key) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            MemorySegment k = key == null ? MemorySegment.NULL : ObjC.nsstring(key);
            h.invokeExact(peer, ObjC.sel("removeAnimationForKey:"), (MemorySegment) k);
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
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("animationForKey:"), (MemorySegment) (key == null ? MemorySegment.NULL : ObjC.nsstring(key)));
            return CAAnimation.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("animationForKey: failed", t); }
    }

    /// [layer removeAllAnimations]
    public void removeAllAnimations() {
        ensureInit();
        ObjC.msgSendVoid(peer, ObjC.sel("removeAllAnimations"));
    }

    /// [layer layoutIfNeeded] — force layout.
    public void layoutIfNeeded() { ensureInit(); ObjC.msgSendVoid(peer, ObjC.sel("layoutIfNeeded")); }

    /// [layer setNeedsDisplay]
    public void setNeedsDisplay() { ensureInit(); ObjC.msgSendVoid(peer, ObjC.sel("setNeedsDisplay")); }

    /// [layer displayIfNeeded]
    public void displayIfNeeded() { ensureInit(); ObjC.msgSendVoid(peer, ObjC.sel("displayIfNeeded")); }

    // ---- geometry / visibility / contents / shadow / tree (CoreAnimation coverage) ----

    /// [layer position] — the layer's position in the superlayer's coordinate space.
    public NSPoint position() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetPoint().invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("position"));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("position failed", t); }
    }

    /// [layer setPosition:]
    public void setPosition(NSPoint p) {
        ensureInit();
        try {
            handles.hSetPoint().invokeExact(peer, ObjC.sel("setPosition:"), p.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setPosition: failed", t); }
    }

    /// [layer anchorPoint] — matches position within bounds, in unit coordinates.
    public NSPoint anchorPoint() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetPoint().invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("anchorPoint"));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("anchorPoint failed", t); }
    }

    /// [layer setAnchorPoint:]
    public void setAnchorPoint(NSPoint p) {
        ensureInit();
        try {
            handles.hSetPoint().invokeExact(peer, ObjC.sel("setAnchorPoint:"), p.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setAnchorPoint: failed", t); }
    }

    /// [layer zPosition] — depth ordering above/below sibling layers.
    public double zPosition() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("zPosition"));
        } catch (Throwable t) { throw new RuntimeException("zPosition failed", t); }
    }

    /// [layer setZPosition:]
    public void setZPosition(double z) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setZPosition:"), z);
        } catch (Throwable t) { throw new RuntimeException("setZPosition: failed", t); }
    }

    /// [layer contentsScale] — scale factor for backing store / rendering.
    public double contentsScale() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("contentsScale"));
        } catch (Throwable t) { throw new RuntimeException("contentsScale failed", t); }
    }

    /// [layer setContentsScale:]
    public void setContentsScale(double scale) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setContentsScale:"), scale);
        } catch (Throwable t) { throw new RuntimeException("setContentsScale: failed", t); }
    }

    /// [layer isHidden]
    public boolean isHidden() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("isHidden"));
        } catch (Throwable t) { throw new RuntimeException("isHidden failed", t); }
    }

    /// [layer setHidden:]
    public void setHidden(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, ObjC.sel("setHidden:"), flag);
        } catch (Throwable t) { throw new RuntimeException("setHidden: failed", t); }
    }

    /// [layer contents] — raw layer contents object (typically a CGImageRef).
    public MemorySegment contents() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("contents"));
        } catch (Throwable t) { throw new RuntimeException("contents failed", t); }
    }

    /// [layer setContents:] — raw contents (CGImageRef or any object; NULL clears).
    public void setContents(MemorySegment cgImageRaw) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setContents:"), (MemorySegment) (cgImageRaw == null ? MemorySegment.NULL : cgImageRaw));
        } catch (Throwable t) { throw new RuntimeException("setContents: failed", t); }
    }

    /// [layer contentsGravity] — one of the kCAGravity* string constants.
    public String contentsGravity() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("contentsGravity")));
        } catch (Throwable t) { throw new RuntimeException("contentsGravity failed", t); }
    }

    /// [layer setContentsGravity:] — e.g. "resize", "center", "resizeAspect".
    public void setContentsGravity(String gravity) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setContentsGravity:"), (MemorySegment) (gravity == null ? MemorySegment.NULL : ObjC.nsstring(gravity)));
        } catch (Throwable t) { throw new RuntimeException("setContentsGravity: failed", t); }
    }

    /// [layer shadowColor] — raw CGColorRef.
    public MemorySegment shadowColor() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("shadowColor"));
        } catch (Throwable t) { throw new RuntimeException("shadowColor failed", t); }
    }

    /// [layer setShadowColor:] — raw CGColorRef (NULL clears).
    public void setShadowColor(MemorySegment cgColor) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setShadowColor:"), (MemorySegment) (cgColor == null ? MemorySegment.NULL : cgColor));
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
            return (double) (float) handles.hGetFloat().invokeExact(peer, ObjC.sel("shadowOpacity"));
        } catch (Throwable t) { throw new RuntimeException("shadowOpacity failed", t); }
    }

    /// [layer setShadowOpacity:]
    public void setShadowOpacity(double opacity) {
        ensureInit();
        try {
            handles.hSetFloat().invokeExact(peer, ObjC.sel("setShadowOpacity:"), (float) opacity);
        } catch (Throwable t) { throw new RuntimeException("setShadowOpacity: failed", t); }
    }

    /// [layer shadowRadius] — blur radius in points.
    public double shadowRadius() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("shadowRadius"));
        } catch (Throwable t) { throw new RuntimeException("shadowRadius failed", t); }
    }

    /// [layer setShadowRadius:]
    public void setShadowRadius(double radius) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setShadowRadius:"), radius);
        } catch (Throwable t) { throw new RuntimeException("setShadowRadius: failed", t); }
    }

    /// [layer shadowOffset]
    public NSSize shadowOffset() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetSize().invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("shadowOffset"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) { throw new RuntimeException("shadowOffset failed", t); }
    }

    /// [layer setShadowOffset:]
    public void setShadowOffset(NSSize offset) {
        ensureInit();
        try {
            handles.hSetSize().invokeExact(peer, ObjC.sel("setShadowOffset:"), offset.toSegment());
        } catch (Throwable t) { throw new RuntimeException("setShadowOffset: failed", t); }
    }

    /// [layer addSublayer:] — append a child layer (deduped onto the VOID,id handle).
    public void addSublayer(CALayer sublayer) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("addSublayer:"), (MemorySegment) (sublayer == null ? MemorySegment.NULL : sublayer.peer()));
        } catch (Throwable t) { throw new RuntimeException("addSublayer: failed", t); }
    }

    /// [layer removeFromSuperlayer]
    public void removeFromSuperlayer() {
        ensureInit();
        ObjC.msgSendVoid(peer, ObjC.sel("removeFromSuperlayer"));
    }

    /// [layer superlayer] — parent layer or null.
    public CALayer superlayer() {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("superlayer")));
        } catch (Throwable t) { throw new RuntimeException("superlayer failed", t); }
    }

    /// [layer sublayers] — child layers as NSArray (null when the layer has none).
    public NSArray sublayers() {
        ensureInit();
        try {
            return NSArray.wrap((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("sublayers")));
        } catch (Throwable t) { throw new RuntimeException("sublayers failed", t); }
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
            hBegin.invokeExact(cls, ObjC.sel("begin"));
            hSetDur.invokeExact(cls, ObjC.sel("setAnimationDuration:"), duration);
            block.run();
            hCommit.invokeExact(cls, ObjC.sel("commit"));
        } catch (Throwable t) { throw new RuntimeException("CATransaction failed", t); }
    }
}
