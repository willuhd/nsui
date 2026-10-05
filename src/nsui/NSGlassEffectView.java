package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSGlassEffectView (macOS 26+) — embeds contentView in Liquid Glass.
/// Thin stateless wrapper; all behavior is AppKit's. Only contentView is
/// guaranteed inside the glass effect; arbitrary subviews have no z-order
/// promise relative to the content view or the glass.
public final class NSGlassEffectView extends NSView {

    /// Glass styles (NSGlassEffectViewStyle).
    public static final long STYLE_REGULAR = 0;
    public static final long STYLE_CLEAR = 1;

    private record Handles(MethodHandle hContentView, MethodHandle hSetContentView,
            MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hTintColor, MethodHandle hSetTintColor,
            MethodHandle hStyle, MethodHandle hSetStyle) {}
    private static volatile Handles handles;

    private NSGlassEffectView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSGlassEffectView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSGlassEffectView(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)));
    }

    /// [[NSGlassEffectView alloc] initWithFrame:].
        public static NSGlassEffectView create(NSRect frame) {
        ensureInit();
        return new NSGlassEffectView(ObjC.newView("NSGlassEffectView", frame));
    }

    /// contentView — the view embedded in glass.
    public NSView contentView() {
        try {
            return NSView.wrap((MemorySegment) handles.hContentView().invokeExact(peer, ObjC.sel("contentView")));
        } catch (Throwable t) {
            throw new RuntimeException("contentView failed", t);
        }
    }

    /// setContentView:.
    public void setContentView(NSView view) {
        try {
            handles.hSetContentView().invokeExact(peer, ObjC.sel("setContentView:"),
                    (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setContentView: failed", t);
        }
    }

    /// cornerRadius — curvature for all corners.
    public double cornerRadius() {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("cornerRadius"));
        } catch (Throwable t) {
            throw new RuntimeException("cornerRadius failed", t);
        }
    }

    /// setCornerRadius:.
    public void setCornerRadius(double radius) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setCornerRadius:"), radius);
        } catch (Throwable t) {
            throw new RuntimeException("setCornerRadius: failed", t);
        }
    }

    /// tintColor — tint toward which the glass leans (nil = untinted).
    public NSColor tintColor() {
        try {
            return NSColor.wrap((MemorySegment) handles.hTintColor().invokeExact(peer, ObjC.sel("tintColor")));
        } catch (Throwable t) {
            throw new RuntimeException("tintColor failed", t);
        }
    }

    /// setTintColor:.
    public void setTintColor(NSColor color) {
        try {
            handles.hSetTintColor().invokeExact(peer, ObjC.sel("setTintColor:"),
                    (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setTintColor: failed", t);
        }
    }

    /// style — STYLE_REGULAR or STYLE_CLEAR.
    public long style() {
        try {
            return (long) handles.hStyle().invokeExact(peer, ObjC.sel("style"));
        } catch (Throwable t) {
            throw new RuntimeException("style failed", t);
        }
    }

    /// setStyle:.
    public void setStyle(long style) {
        try {
            handles.hSetStyle().invokeExact(peer, ObjC.sel("setStyle:"), style);
        } catch (Throwable t) {
            throw new RuntimeException("setStyle: failed", t);
        }
    }
}
