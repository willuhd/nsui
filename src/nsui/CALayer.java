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
public final class CALayer extends NSObject {

            private record Handles(MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetFloat, MethodHandle hSetFloat, MethodHandle hGetId, MethodHandle hSetId) {}
    private static volatile Handles handles;

    private CALayer(MemorySegment peer) {
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
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
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
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("animationForKey:"), key == null ? MemorySegment.NULL : ObjC.nsstring(key));
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
