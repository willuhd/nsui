package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSVisualEffectView — a translucent, vibrancy-capable view. Thin, 1:1, stateless
/// wrapper over a native `NSVisualEffectView`: every method maps to one
/// `objc_msgSend` selector and no Java state is cached beyond the peer.
/// It is an `NSView`, so it can host subviews and be placed in any view hierarchy.
///
/// AppKit notes:
/// - `material` — `NSVisualEffectMaterial` (NSInteger), e.g. 0 = appearanceBased,
///   3 = titlebar, 7 = sidebar, 13 = HUDWindow.
/// - `blendingMode` — `NSVisualEffectBlendingMode` (NSInteger): 0 = behindWindow, 1 = withinWindow.
/// - `state` — `NSVisualEffectState` (NSInteger): 0 = inactive, 1 = followsWindowActiveState, 2 = active.
/// - `isEmphasized` — BOOL, whether the material is emphasized (selection/active).
/// - `maskingImage` — optional `NSImage` that masks the effect.
public final class NSVisualEffectView extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame; // (id, SEL, NSRect) -> id
    private static MethodHandle hSetInt;    // (id, SEL, long) -> void  [setMaterial:/setBlendingMode:/setState:]
    private static MethodHandle hSetBool;   // (id, SEL, bool) -> void  [setEmphasized:]
    private static MethodHandle hSetId;     // (id, SEL, id) -> void    [setMaskingImage:]
    private static MethodHandle hIsKind;    // (id, SEL, id) -> bool   [isKindOfClass:]

    private NSVisualEffectView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native NSVisualEffectView id (e.g. from a nib) as an NSVisualEffectView.
    public static NSVisualEffectView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSVisualEffectView(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSetInt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hSetId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hIsKind = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        initialized = true;
    }

    /// `[[NSVisualEffectView alloc] initWithFrame:frame]` — a new visual effect view.
    public static NSVisualEffectView create(NSRect frame) {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(ObjC.cls("NSVisualEffectView"), ObjC.sel("alloc"));
        try {
            v = (MemorySegment) hInitFrame.invokeExact(v, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSVisualEffectView", t);
        }
        if (v.address() == 0) {
            throw new IllegalStateException("NSVisualEffectView alloc/initWithFrame: returned nil");
        }
        return new NSVisualEffectView(v);
    }

    // ---------------------------------------------------------------- instance API

    /// [view material] — `NSVisualEffectMaterial` (NSInteger).
    public long material() {
        return ObjC.msgSendLong(peer, ObjC.sel("material"));
    }

    /// [view setMaterial:] — `NSVisualEffectMaterial`.
    public void setMaterial(long material) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setMaterial:"), material);
        } catch (Throwable t) {
            throw new RuntimeException("setMaterial: failed", t);
        }
    }

    /// [view blendingMode] — `NSVisualEffectBlendingMode` (NSInteger).
    public long blendingMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("blendingMode"));
    }

    /// [view setBlendingMode:] — `NSVisualEffectBlendingMode`.
    public void setBlendingMode(long mode) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setBlendingMode:"), mode);
        } catch (Throwable t) {
            throw new RuntimeException("setBlendingMode: failed", t);
        }
    }

    /// [view state] — `NSVisualEffectState` (NSInteger).
    public long state() {
        return ObjC.msgSendLong(peer, ObjC.sel("state"));
    }

    /// [view setState:] — `NSVisualEffectState`.
    public void setState(long state) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setState:"), state);
        } catch (Throwable t) {
            throw new RuntimeException("setState: failed", t);
        }
    }

    /// [view isEmphasized] — whether the material is emphasized.
    public boolean isEmphasized() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEmphasized"));
    }

    /// [view setEmphasized:] — emphasized flag.
    public void setEmphasized(boolean emphasized) {
        try {
            hSetBool.invokeExact(peer, ObjC.sel("setEmphasized:"), emphasized);
        } catch (Throwable t) {
            throw new RuntimeException("setEmphasized: failed", t);
        }
    }

    // AppKit 14+ uses `maskImage`/`setMaskImage:`; older SDKs documented `maskingImage`.
    // We expose the spec name `maskingImage` but dispatch to whichever selector the runtime responds to.
    private MemorySegment maskingImageSel() {
        try {
            boolean responds = (boolean) hIsKind.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("maskImage"));
            return responds ? ObjC.sel("maskImage") : ObjC.sel("maskingImage");
        } catch (Throwable t) {
            return ObjC.sel("maskImage");
        }
    }

    private MemorySegment setMaskingImageSel() {
        try {
            boolean responds = (boolean) hIsKind.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("setMaskImage:"));
            return responds ? ObjC.sel("setMaskImage:") : ObjC.sel("setMaskingImage:");
        } catch (Throwable t) {
            return ObjC.sel("setMaskImage:");
        }
    }

    /// [view maskingImage] / [view maskImage] — optional masking image (may be nil).
    public NSImage maskingImage() {
        MemorySegment img = ObjC.msgSendId(peer, maskingImageSel());
        return NSImage.wrap(img);
    }

    /// Raw peer for [view maskingImage] — id return without wrapping (null peer for nil).
    public MemorySegment maskingImagePeer() {
        return ObjC.msgSendId(peer, maskingImageSel());
    }

    /// Alias for `maskingImage` — the AppKit-native name.
    public NSImage maskImage() {
        return maskingImage();
    }

    /// [view setMaskingImage:] / [view setMaskImage:] — masking image (null to clear).
    public void setMaskingImage(NSImage image) {
        try {
            MemorySegment p = (MemorySegment) (image == null ? MemorySegment.NULL : image.peer());
            hSetId.invokeExact(peer, setMaskingImageSel(), p);
        } catch (Throwable t) {
            throw new RuntimeException("setMaskingImage: failed", t);
        }
    }

    /// [view setMaskingImage:] — raw peer variant (MemorySegment.NULL to clear).
    public void setMaskingImagePeer(MemorySegment imagePeer) {
        try {
            MemorySegment p = (imagePeer == null || imagePeer.address() == 0) ? MemorySegment.NULL : imagePeer;
            hSetId.invokeExact(peer, setMaskingImageSel(), p);
        } catch (Throwable t) {
            throw new RuntimeException("setMaskingImage: failed", t);
        }
    }

    /// Alias for `setMaskingImage` — the AppKit-native name.
    public void setMaskImage(NSImage image) {
        setMaskingImage(image);
    }

    // ---------------------------------------------------------------- helpers

    /// `[view isKindOfClass:[NSVisualEffectView class]]` — runtime type check.
    /// Convenience wrapper around the ObjC `isKindOfClass:` selector so tests
    /// can verify the peer's class without reaching into `ObjC` directly.
    public boolean isKindOfClass(MemorySegment clazz) {
        try {
            return (boolean) hIsKind.invokeExact(peer, ObjC.sel("isKindOfClass:"), clazz);
        } catch (Throwable t) {
            throw new RuntimeException("isKindOfClass: failed", t);
        }
    }

    /// `isKindOfClass:` by class name (cached via `cls`).
    public boolean isKindOfClass(String className) {
        return isKindOfClass(ObjC.cls(className));
    }
}
