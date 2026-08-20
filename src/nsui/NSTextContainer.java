package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSTextContainer — a region that holds text layout.
 * Thin 1:1 wrapper over native {@code NSTextContainer}.
 */
public final class NSTextContainer extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hInitSize;   // (id, SEL, NSSize) -> id  initWithContainerSize:
    private static MethodHandle hInit;       // (id, SEL) -> id  init
    private static MethodHandle hGetSize;    // (id, SEL) -> NSSize
    private static MethodHandle hSetSize;    // (id, SEL, NSSize) -> void
    private static MethodHandle hGetDouble;  // (id, SEL) -> double
    private static MethodHandle hSetDouble;  // (id, SEL, double) -> void
    private static MethodHandle hGetBool;    // (id, SEL) -> bool
    private static MethodHandle hSetBool;    // (id, SEL, bool) -> void
    private static MethodHandle hGetId;      // (id, SEL) -> id

    private NSTextContainer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTextContainer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTextContainer(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        // initWithContainerSize: is (ID,SIZE) which is not directly in vocab, but we resolve via handle;
        // if shape missing, handle will throw at call time — fallback path uses init+setContainerSize.
        try {
            hInitSize = ObjC.handle(Sig.of(Ret.ID, Arg.SIZE));
        } catch (Throwable t) {
            hInitSize = null;
        }
        hInit = ObjC.handle(Sig.of(Ret.ID));
        hGetSize = ObjC.handle(Sig.of(Ret.SIZE));
        hSetSize = ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE));
        hGetDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hGetBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
    }

    /** {@code [[NSTextContainer alloc] init]} — default sized container. */
    public static NSTextContainer create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSTextContainer"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) hInit.invokeExact(alloc, ObjC.sel("init"));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSTextContainer init returned nil");
            return new NSTextContainer(p);
        } catch (Throwable t) {
            throw new RuntimeException("NSTextContainer init failed", t);
        }
    }

    /** {@code [[NSTextContainer alloc] initWithContainerSize:size]} — sized container. */
    public static NSTextContainer create(NSSize size) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSTextContainer"), ObjC.sel("alloc"));
        if (hInitSize != null) {
            try {
                MemorySegment p = (MemorySegment) hInitSize.invokeExact(alloc, ObjC.sel("initWithContainerSize:"), size.toSegment());
                if (p != null && p.address() != 0) return new NSTextContainer(p);
            } catch (Throwable t) {
                // fall through to init+set
            }
        }
        // Fallback: init then setContainerSize:
        try {
            MemorySegment p = (MemorySegment) hInit.invokeExact(alloc, ObjC.sel("init"));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSTextContainer init returned nil");
            NSTextContainer c = new NSTextContainer(p);
            c.setContainerSize(size);
            return c;
        } catch (Throwable t) {
            throw new RuntimeException("NSTextContainer initWithContainerSize: failed", t);
        }
    }

    /** Convenience: create with tracking defaults (size zero means tracking). */
    public static NSTextContainer create(double width, double height) {
        return create(new NSSize(width, height));
    }

    // ---- containerSize ----

    /** [container containerSize] -> NSSize */
    public NSSize containerSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hGetSize.invokeExact((java.lang.foreign.SegmentAllocator) Arena.global(), peer, ObjC.sel("containerSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("containerSize failed", t);
        }
    }

    /** [container setContainerSize:] */
    public void setContainerSize(NSSize size) {
        ensureInit();
        try {
            hSetSize.invokeExact(peer, ObjC.sel("setContainerSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContainerSize: failed", t);
        }
    }

    // ---- tracking ----

    /** [container widthTracksTextView] */
    public boolean widthTracksTextView() {
        ensureInit();
        try { return (boolean) hGetBool.invokeExact(peer, ObjC.sel("widthTracksTextView")); } catch (Throwable t) { throw new RuntimeException("widthTracksTextView failed", t); }
    }
    public void setWidthTracksTextView(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setWidthTracksTextView:"), flag); } catch (Throwable t) { throw new RuntimeException("setWidthTracksTextView: failed", t); }
    }

    /** [container heightTracksTextView] */
    public boolean heightTracksTextView() {
        ensureInit();
        try { return (boolean) hGetBool.invokeExact(peer, ObjC.sel("heightTracksTextView")); } catch (Throwable t) { throw new RuntimeException("heightTracksTextView failed", t); }
    }
    public void setHeightTracksTextView(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setHeightTracksTextView:"), flag); } catch (Throwable t) { throw new RuntimeException("setHeightTracksTextView: failed", t); }
    }

    // ---- padding ----

    /** [container lineFragmentPadding] -> double */
    public double lineFragmentPadding() {
        ensureInit();
        try { return (double) hGetDouble.invokeExact(peer, ObjC.sel("lineFragmentPadding")); } catch (Throwable t) { throw new RuntimeException("lineFragmentPadding failed", t); }
    }
    public void setLineFragmentPadding(double pad) {
        ensureInit();
        try { hSetDouble.invokeExact(peer, ObjC.sel("setLineFragmentPadding:"), pad); } catch (Throwable t) { throw new RuntimeException("setLineFragmentPadding: failed", t); }
    }

    // ---- layoutManager ----

    /** [container layoutManager] -> NSLayoutManager */
    public NSLayoutManager layoutManager() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("layoutManager"));
            return NSLayoutManager.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("layoutManager failed", t);
        }
    }

    /** [container setLayoutManager:] — normally managed by NSLayoutManager addTextContainer. */
    public void setLayoutManager(NSLayoutManager lm) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setLayoutManager:"), (MemorySegment) (lm == null ? MemorySegment.NULL : lm.peer()));
    }

    /** [container textView] — the owning NSTextView if any. */
    public NSTextView textView() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("textView"));
            return NSTextView.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("textView failed", t);
        }
    }

    // ---- exclusion paths (minimal stub) ----

    /** [container exclusionPaths] — NSArray of NSBezierPath ids */
    public MemorySegment exclusionPaths() {
        return ObjC.msgSendId(peer, ObjC.sel("exclusionPaths"));
    }
    public void setExclusionPaths(MemorySegment paths) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setExclusionPaths:"), (MemorySegment) (paths == null ? MemorySegment.NULL : paths));
    }
}
