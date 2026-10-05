package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSGlassEffectContainerView (macOS 26+) — merges descendant glass effect views
/// within proximity so similar glass renders as one batch (fewer passes).
/// Thin stateless wrapper; all behavior is AppKit's.
public final class NSGlassEffectContainerView extends NSView {

    private record Handles(MethodHandle hContentView, MethodHandle hSetContentView,
            MethodHandle hGetDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    private NSGlassEffectContainerView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSGlassEffectContainerView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSGlassEffectContainerView(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
    }

    /// [[NSGlassEffectContainerView alloc] initWithFrame:].
        public static NSGlassEffectContainerView create(NSRect frame) {
        ensureInit();
        return new NSGlassEffectContainerView(ObjC.newView("NSGlassEffectContainerView", frame));
    }

    /// contentView — descendants of this view are merged when eligible.
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

    /// spacing — merge proximity (0 batches without distortion).
    public double spacing() {
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("spacing"));
        } catch (Throwable t) {
            throw new RuntimeException("spacing failed", t);
        }
    }

    /// setSpacing:.
    public void setSpacing(double spacing) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setSpacing:"), spacing);
        } catch (Throwable t) {
            throw new RuntimeException("setSpacing: failed", t);
        }
    }
}
