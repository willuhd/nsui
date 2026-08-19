package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSStackView — the toolkit's <em>layout manager</em>. Arranged subviews are
 * positioned and sized by AppKit (spacing, alignment, distribution/gravity), so
 * callers never hand-compute frames: they {@link #addArrangedSubview(NSView)} in
 * order and AppKit stacks them along the chosen {@link #setOrientation(long) axis},
 * honouring each subview's intrinsic size.
 *
 * <p>This is AppKit's OWN layout container exposed as a first-class L1 class — NOT
 * a Java layout manager. The layout happens natively on the main thread, so the
 * arranged subviews always land where the run loop puts them and {@link #frame()}
 * reflects the computed result exactly like any other view.
 *
 * <p>Thin, 1:1, stateless wrapper (SWT-style) over a native {@code NSStackView};
 * every method maps to one {@code objc_msgSend} selector.
 *
 * <p><strong>Intrinsic sizes:</strong> an arranged subview's laid-out frame is its
 * intrinsic content size where AppKit knows one (controls like {@link NSButton} and
 * {@link NSTextField} report intrinsic sizes, so a {@code sizeToFit} button or a
 * created text field arrives with the right size). A plain {@link NSView} that reports
 * no intrinsic size keeps the frame it had when added — provided
 * {@link #setTranslatesAutoresizingMaskIntoConstraints(boolean)} is left at its
 * default {@code true} (see below).
 *
 * <p><strong>When {@code translatesAutoresizingMaskIntoConstraints} matters:</strong>
 * in an <em>Auto Layout</em> hierarchy (the standard combo for stack-arranged views)
 * you usually set it to {@code false} so the stack, not the superview's autoresizing
 * mask, drives each arranged subview's size. In <em>this</em> manual-frame world the
 * stack itself is pinned with an explicit {@code setFrame:}, and arranged subviews
 * carry their own frames; leaving the default {@code true} lets the stack honour the
 * frame a plain subview already had. Set it to {@code false} only when you want each
 * arranged view's size to come purely from its intrinsic size / the stack's algorithm
 * and you are inside a constrained layout.
 */
public class NSStackView extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSpacing;     // (id, SEL, double) -> void
    private static MethodHandle hEdgeInsets;  // (id, SEL, NSRect-as-NSEdgeInsets) -> void
    private static MethodHandle hGetDouble;   // (id, SEL) -> double

    private NSStackView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSpacing = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hEdgeInsets = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
        hGetDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        initialized = true;
    }

    /** {@code [[NSStackView alloc] initWithFrame:frame]} — a new stack with the given frame. */
    public static NSStackView create(NSRect frame) {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSStackView"), ObjC.sel("alloc"));
        try {
            s = (MemorySegment) hInitFrame.invokeExact(s, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSStackView", t);
        }
        if (s.address() == 0) {
            throw new IllegalStateException("NSStackView alloc/initWithFrame: returned nil");
        }
        return new NSStackView(s);
    }

    // ---------------------------------------------------------------- arranged subviews

    /** addArrangedSubview: — append a view to the stack (also a subview). */
    public void addArrangedSubview(NSView subview) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addArrangedSubview:"), subview.peer());
    }

    /** removeArrangedSubview: — take a view back out of the stack's layout (still a subview). */
    public void removeArrangedSubview(NSView subview) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeArrangedSubview:"), subview.peer());
    }

    // ---------------------------------------------------------------- geometry

    /** setOrientation: — the stacking axis: 0 = horizontal, 1 = vertical (NSUserInterfaceLayoutOrientation). */
    public void setOrientation(long orientation) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setOrientation:"), orientation);
    }

    /** setSpacing: — the gap, in points, between adjacent arranged subviews (also the gravity-area gap). */
    public void setSpacing(double spacing) {
        try {
            hSpacing.invokeExact(peer, ObjC.sel("setSpacing:"), spacing);
        } catch (Throwable t) {
            throw new RuntimeException("setSpacing: failed", t);
        }
    }

    /** setAlignment: — alignment of the arranged views along the non-stacking axis (an NSLayoutAttribute). */
    public void setAlignment(long alignment) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setAlignment:"), alignment);
    }

    /** setDistribution: — the horizontal/vertical layout of arranged subviews (an NSStackViewDistribution). */
    public void setDistribution(long distribution) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDistribution:"), distribution);
    }

    /**
     * setEdgeInsets: — the padding between the stack's bounds and the arranged subviews.
     * Parametrized as (top, left, bottom, right); AppKit's {@code NSEdgeInsets} is four
     * doubles (layout-identical to NSRect), passed via {@code ObjC.rect(top, left, bottom, right)}.
     */
    public void setEdgeInsets(double top, double left, double bottom, double right) {
        try {
            hEdgeInsets.invokeExact(peer, ObjC.sel("setEdgeInsets:"),
                    ObjC.rect(top, left, bottom, right));
        } catch (Throwable t) {
            throw new RuntimeException("setEdgeInsets: failed", t);
        }
    }

    /**
     * setTranslatesAutoresizingMaskIntoConstraints: — whether the stack's arranged subviews
     * keep the frames implied by their autoresizing masks. The default (true) lets the stack
     * use the frame a plain subview already had when it can't infer an intrinsic size; set to
     * false when in a constrained layout where each arranged view's size must come purely from
     * its intrinsic size / the stack's algorithm. See the class Javadoc for when it matters.
     */
    public void setTranslatesAutoresizingMaskIntoConstraints(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setTranslatesAutoresizingMaskIntoConstraints:"), flag);
    }

    // ---------------------------------------------------------------- completeness getters

    /** orientation — 0 horizontal, 1 vertical. */
    public long orientation() {
        return ObjC.msgSendLong(peer, ObjC.sel("orientation"));
    }

    /** spacing — gap between arranged subviews. */
    public double spacing() {
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("spacing"));
        } catch (Throwable t) {
            throw new RuntimeException("spacing failed", t);
        }
    }

    /** alignment — NSLayoutAttribute. */
    public long alignment() {
        return ObjC.msgSendLong(peer, ObjC.sel("alignment"));
    }

    /** distribution — NSStackViewDistribution. */
    public long distribution() {
        return ObjC.msgSendLong(peer, ObjC.sel("distribution"));
    }

    /** detachesHiddenViews — whether hidden arranged views are detached. */
    public boolean detachesHiddenViews() {
        return ObjC.msgSendBool(peer, ObjC.sel("detachesHiddenViews"));
    }

    /** setDetachesHiddenViews:. */
    public void setDetachesHiddenViews(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDetachesHiddenViews:"), flag);
    }

    /** arrangedSubviews — the arranged views. */
    public java.util.List<NSView> arrangedSubviews() {
        MemorySegment arr = ObjC.msgSendId(peer, ObjC.sel("arrangedSubviews"));
        if (arr == null || arr.address() == 0) return java.util.List.of();
        long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
        java.util.List<NSView> list = new java.util.ArrayList<>((int) count);
        MemorySegment objectAtIndex = ObjC.sel("objectAtIndex:");
        MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        for (long i = 0; i < count; i++) {
            try {
                MemorySegment v = (MemorySegment) h.invokeExact(arr, objectAtIndex, i);
                if (v != null && v.address() != 0) list.add(NSView.wrap(v));
            } catch (Throwable t) {
                throw new RuntimeException("arrangedSubviews objectAtIndex failed", t);
            }
        }
        return java.util.Collections.unmodifiableList(list);
    }

    /** translatesAutoresizingMaskIntoConstraints getter. */
    public boolean translatesAutoresizingMaskIntoConstraints() {
        return ObjC.msgSendBool(peer, ObjC.sel("translatesAutoresizingMaskIntoConstraints"));
    }
}
