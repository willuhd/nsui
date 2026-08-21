package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTextContainer — a region that holds text layout.
/// Thin 1:1 wrapper over native `NSTextContainer`.
public final class NSTextContainer extends NSObject {

    private record Handles(MethodHandle hInitSize, MethodHandle hInit, MethodHandle hGetSize, MethodHandle hSetSize, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetBool, MethodHandle hSetBool) {}
    private static volatile Handles handles;

    private NSTextContainer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTextContainer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTextContainer(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        // initWithContainerSize: is (ID,SIZE) which is not directly in vocab, but we resolve via handle;
        // if shape missing, handle will throw at call time — fallback path uses init+setContainerSize.
        MethodHandle tmp_hInitSize = null;
        try { tmp_hInitSize = ObjC.handle(Sig.of(Ret.ID, Arg.SIZE)); } catch (Exception ignored) { tmp_hInitSize = null; }
        handles = new Handles(
                tmp_hInitSize,
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL))
        );
    }

    /// `[[NSTextContainer alloc] init]` — default sized container.
    public static NSTextContainer create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSTextContainer"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(alloc, ObjC.sel("init"));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSTextContainer init returned nil");
            return new NSTextContainer(p);
        } catch (Throwable t) {
            throw new RuntimeException("NSTextContainer init failed", t);
        }
    }

    /// `[[NSTextContainer alloc] initWithContainerSize:size]` — sized container.
    public static NSTextContainer create(NSSize size) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSTextContainer"), ObjC.sel("alloc"));
        if (handles.hInitSize() != null) {
            try {
                MemorySegment p = (MemorySegment) handles.hInitSize().invokeExact(alloc, ObjC.sel("initWithContainerSize:"), size.toSegment());
                if (p != null && p.address() != 0) return new NSTextContainer(p);
            } catch (Throwable t) {
                // fall through to init+set
            }
        }
        // Fallback: init then setContainerSize:
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(alloc, ObjC.sel("init"));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSTextContainer init returned nil");
            NSTextContainer c = new NSTextContainer(p);
            c.setContainerSize(size);
            return c;
        } catch (Throwable t) {
            throw new RuntimeException("NSTextContainer initWithContainerSize: failed", t);
        }
    }

    /// Convenience: create with tracking defaults (size zero means tracking).
    public static NSTextContainer create(double width, double height) {
        return create(new NSSize(width, height));
    }

    // ---- containerSize ----

    /// [container containerSize] -> NSSize
    public NSSize containerSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetSize().invokeExact((java.lang.foreign.SegmentAllocator) Arena.global(), peer, ObjC.sel("containerSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("containerSize failed", t);
        }
    }

    /// [container setContainerSize:]
    public void setContainerSize(NSSize size) {
        ensureInit();
        try {
            handles.hSetSize().invokeExact(peer, ObjC.sel("setContainerSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContainerSize: failed", t);
        }
    }

    // ---- tracking ----

    /// [container widthTracksTextView]
    public boolean widthTracksTextView() {
        ensureInit();
        try { return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("widthTracksTextView")); } catch (Throwable t) { throw new RuntimeException("widthTracksTextView failed", t); }
    }
    public void setWidthTracksTextView(boolean flag) {
        ensureInit();
        try { handles.hSetBool().invokeExact(peer, ObjC.sel("setWidthTracksTextView:"), flag); } catch (Throwable t) { throw new RuntimeException("setWidthTracksTextView: failed", t); }
    }

    /// [container heightTracksTextView]
    public boolean heightTracksTextView() {
        ensureInit();
        try { return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("heightTracksTextView")); } catch (Throwable t) { throw new RuntimeException("heightTracksTextView failed", t); }
    }
    public void setHeightTracksTextView(boolean flag) {
        ensureInit();
        try { handles.hSetBool().invokeExact(peer, ObjC.sel("setHeightTracksTextView:"), flag); } catch (Throwable t) { throw new RuntimeException("setHeightTracksTextView: failed", t); }
    }

    // ---- padding ----

    /// [container lineFragmentPadding] -> double
    public double lineFragmentPadding() {
        ensureInit();
        try { return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("lineFragmentPadding")); } catch (Throwable t) { throw new RuntimeException("lineFragmentPadding failed", t); }
    }
    public void setLineFragmentPadding(double pad) {
        ensureInit();
        try { handles.hSetDouble().invokeExact(peer, ObjC.sel("setLineFragmentPadding:"), pad); } catch (Throwable t) { throw new RuntimeException("setLineFragmentPadding: failed", t); }
    }

    // ---- layoutManager ----

    /// [container layoutManager] -> NSLayoutManager
    public NSLayoutManager layoutManager() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(peer, ObjC.sel("layoutManager"));
            return NSLayoutManager.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("layoutManager failed", t);
        }
    }

    /// [container setLayoutManager:] — normally managed by NSLayoutManager addTextContainer.
    public void setLayoutManager(NSLayoutManager lm) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setLayoutManager:"), (MemorySegment) (lm == null ? MemorySegment.NULL : lm.peer()));
    }

    /// [container textView] — the owning NSTextView if any.
    public NSTextView textView() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(peer, ObjC.sel("textView"));
            return NSTextView.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("textView failed", t);
        }
    }

    /// [container setTextView:]
    public void setTextView(NSTextView tv) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setTextView:"), (MemorySegment) (tv == null ? MemorySegment.NULL : tv.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setTextView: failed", t);
        }
    }

    // ---- exclusion paths (minimal stub) ----

    /// [container exclusionPaths] — NSArray of NSBezierPath ids
    public MemorySegment exclusionPaths() {
        return ObjC.msgSendId(peer, ObjC.sel("exclusionPaths"));
    }
    public void setExclusionPaths(MemorySegment paths) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setExclusionPaths:"), (MemorySegment) (paths == null ? MemorySegment.NULL : paths));
    }
}
