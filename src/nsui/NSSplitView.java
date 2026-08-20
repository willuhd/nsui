package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSSplitView — a pane-splitting container with draggable dividers.
 * Thin, 1:1, stateless wrapper over a native {@code NSSplitView}: every method
 * maps to one {@code objc_msgSend} selector.
 *
 * <p>It is an {@link NSView}, so it fits any view hierarchy. Subviews are added
 * via {@link #addSubview(NSView)} (exposed as {@link #addArrangedSubview(NSView)}
 * for API parity with {@link NSStackView}). Orientation is {@code vertical}
 * (left/right split, {@code true}) vs horizontal (top/bottom, {@code false}).
 */
public final class NSSplitView extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;     // (id, SEL, NSRect) -> id
    private static MethodHandle hGetBool;       // (id, SEL) -> BOOL
    private static MethodHandle hSetBool;       // (id, SEL, BOOL) -> void
    private static MethodHandle hGetLong;       // (id, SEL) -> long (NSInteger)
    private static MethodHandle hSetLong;       // (id, SEL, long) -> void
    private static MethodHandle hSetPosition;   // (id, SEL, double, long) -> void  [setPosition:ofDividerAtIndex:]

    private NSSplitView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /** Wrap a native NSSplitView id as an NSSplitView. */
    public static NSSplitView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSplitView(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hGetBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hGetLong = ObjC.handle(Sig.of(Ret.INT));
        hSetLong = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetPosition = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE, Arg.INT));
        initialized = true;
    }

    /** {@code [[NSSplitView alloc] initWithFrame:frame]} — a new split view at the given rect. */
    public static NSSplitView create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSSplitView"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSSplitView", t);
        }
        if (p.address() == 0) {
            throw new IllegalStateException("NSSplitView alloc/initWithFrame: returned nil");
        }
        return new NSSplitView(p);
    }

    // ---------------------------------------------------------------- isVertical

    /** [splitView isVertical] — {@code YES} for left/right split, {@code NO} for top/bottom. */
    public boolean isVertical() {
        ensureInit();
        try {
            return (boolean) hGetBool.invokeExact(peer, ObjC.sel("isVertical"));
        } catch (Throwable t) {
            throw new RuntimeException("isVertical failed", t);
        }
    }

    /** [splitView setVertical:] — set the stacking axis. */
    public void setVertical(boolean flag) {
        ensureInit();
        try {
            hSetBool.invokeExact(peer, ObjC.sel("setVertical:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setVertical: failed", t);
        }
    }

    // ---------------------------------------------------------------- dividerStyle

    /** [splitView dividerStyle] — {@code NSSplitViewDividerStyle} (NSInteger). */
    public long dividerStyle() {
        ensureInit();
        try {
            return (long) hGetLong.invokeExact(peer, ObjC.sel("dividerStyle"));
        } catch (Throwable t) {
            throw new RuntimeException("dividerStyle failed", t);
        }
    }

    /** [splitView setDividerStyle:] — {@code NSSplitViewDividerStyle}. */
    public void setDividerStyle(long style) {
        ensureInit();
        try {
            hSetLong.invokeExact(peer, ObjC.sel("setDividerStyle:"), style);
        } catch (Throwable t) {
            throw new RuntimeException("setDividerStyle: failed", t);
        }
    }

    // ---------------------------------------------------------------- arranged subview mimic

    /**
     * addArrangedSubview: — mimic via {@code addSubview:} for API parity with
     * {@link NSStackView}. NSSplitView panes are plain subviews; this is an alias
     * for {@link #addSubview(NSView)}.
     */
    public void addArrangedSubview(NSView subview) {
        addSubview(subview);
    }

    // ---------------------------------------------------------------- divider position

    /**
     * [splitView setPosition:ofDividerAtIndex:] — set the position of a divider.
     * Uses {@code Sig.of(VOID,DOUBLE,INT)} exactly as specified.
     *
     * @param position     the new position in points along the split axis
     * @param dividerIndex index of the divider (0 .. subviewCount-2)
     */
    public void setPositionOfDividerAtIndex(double position, long dividerIndex) {
        ensureInit();
        try {
            hSetPosition.invokeExact(peer, ObjC.sel("setPosition:ofDividerAtIndex:"), position, dividerIndex);
        } catch (Throwable t) {
            throw new RuntimeException("setPosition:ofDividerAtIndex: failed", t);
        }
    }

    /**
     * Alias matching the ObjC selector spelling: {@code setPosition:ofDividerAtIndex:}.
     * Delegates to {@link #setPositionOfDividerAtIndex(double, long)}.
     */
    public void setPosition(double position, long dividerIndex) {
        setPositionOfDividerAtIndex(position, dividerIndex);
    }
}
