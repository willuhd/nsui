package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSplitView — a pane-splitting container with draggable dividers.
/// Thin, 1:1, stateless wrapper over a native `NSSplitView`: every method
/// maps to one `objc_msgSend` selector.
///
/// It is an `NSView`, so it fits any view hierarchy. Subviews are added
/// via `addSubview` (exposed as `addArrangedSubview`
/// for API parity with `NSStackView`). Orientation is `vertical`
/// (left/right split, `true`) vs horizontal (top/bottom, `false`).
public final class NSSplitView extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;     // (id, SEL, NSRect) -> id
    private static MethodHandle hGetBool;       // (id, SEL) -> BOOL
    private static MethodHandle hSetBool;       // (id, SEL, BOOL) -> void
    private static MethodHandle hGetLong;       // (id, SEL) -> long (NSInteger)
    private static MethodHandle hSetLong;       // (id, SEL, long) -> void
    private static MethodHandle hSetPosition;   // (id, SEL, double, long) -> void  [setPosition:ofDividerAtIndex:]
    private static MethodHandle hGetDouble;     // (id, SEL) -> double
    private static MethodHandle hBoolId;        // (id, SEL, id) -> BOOL     [isSubviewCollapsed:]
    private static MethodHandle hDoubleInt;     // (id, SEL, long) -> double [min/maxPossiblePositionOfDividerAtIndex:]
    private static MethodHandle hVoidIdInt;     // (id, SEL, id, long) -> void [insertArrangedSubview:atIndex:]

    private NSSplitView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native NSSplitView id as an NSSplitView.
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
        hGetDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hBoolId = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        hDoubleInt = ObjC.handle(Sig.of(Ret.DOUBLE, Arg.INT));
        hVoidIdInt = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        initialized = true;
    }

    /// `[[NSSplitView alloc] initWithFrame:frame]` — a new split view at the given rect.
        public static NSSplitView create(NSRect frame) {
        ensureInit();
        return new NSSplitView(ObjC.newView("NSSplitView", frame));
    }

    // ---------------------------------------------------------------- isVertical

    /// [splitView isVertical] — `YES` for left/right split, `NO` for top/bottom.
    public boolean isVertical() {
        ensureInit();
        try {
            return (boolean) hGetBool.invokeExact(peer, ObjC.sel("isVertical"));
        } catch (Throwable t) {
            throw new RuntimeException("isVertical failed", t);
        }
    }

    /// [splitView setVertical:] — set the stacking axis.
    public void setVertical(boolean flag) {
        ensureInit();
        try {
            hSetBool.invokeExact(peer, ObjC.sel("setVertical:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setVertical: failed", t);
        }
    }

    // ---------------------------------------------------------------- dividerStyle

    // ---------------------------------------------------------------- nested enum — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSSplitView.h
    //   NSSplitViewDividerStyle: Thick 1, Thin 2, PaneSplitter 3
    // Docs: https://developer.apple.com/documentation/appkit/nssplitview/dividerstyle

    /// `NSSplitViewDividerStyle` — 1=Thick, 2=Thin, 3=PaneSplitter. From `NSSplitView.h`.
    public enum DividerStyle {
        thick(1), thin(2), paneSplitter(3);
        public final long value;
        DividerStyle(long v) { this.value = v; }
        public static DividerStyle fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// [splitView dividerStyle] — `NSSplitViewDividerStyle` (NSInteger).
    public long dividerStyle() {
        ensureInit();
        try {
            return (long) hGetLong.invokeExact(peer, ObjC.sel("dividerStyle"));
        } catch (Throwable t) {
            throw new RuntimeException("dividerStyle failed", t);
        }
    }
    /// Typed getter.
    public DividerStyle dividerStyleEnum() { return DividerStyle.fromValue(dividerStyle()); }

    /// [splitView setDividerStyle:] — `NSSplitViewDividerStyle`.
    public void setDividerStyle(long style) {
        ensureInit();
        try {
            hSetLong.invokeExact(peer, ObjC.sel("setDividerStyle:"), style);
        } catch (Throwable t) {
            throw new RuntimeException("setDividerStyle: failed", t);
        }
    }
    /// Typed overload.
    public void setDividerStyle(DividerStyle s) { setDividerStyle(s.value); }

    // ---------------------------------------------------------------- arranged subview mimic

    /// [splitView addArrangedSubview:] — add a pane (also becomes a subview).
    public void addArrangedSubview(NSView subview) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addArrangedSubview:"), subview.peer());
    }

    // ---------------------------------------------------------------- divider position

    /// [splitView setPosition:ofDividerAtIndex:] — set the position of a divider.
    /// Uses `Sig.of(VOID,DOUBLE,INT)` exactly as specified.
    ///
    /// @param position     the new position in points along the split axis
    /// @param dividerIndex index of the divider (0 .. subviewCount-2)
    public void setPositionOfDividerAtIndex(double position, long dividerIndex) {
        ensureInit();
        try {
            hSetPosition.invokeExact(peer, ObjC.sel("setPosition:ofDividerAtIndex:"), position, dividerIndex);
        } catch (Throwable t) {
            throw new RuntimeException("setPosition:ofDividerAtIndex: failed", t);
        }
    }

    /// Alias matching the ObjC selector spelling: `setPosition:ofDividerAtIndex:`.
    /// Delegates to `setPositionOfDividerAtIndex`.
    public void setPosition(double position, long dividerIndex) {
        setPositionOfDividerAtIndex(position, dividerIndex);
    }

    // ---------------------------------------------------------------- header-completeness batch (NSSplitView.h)
    //
    // Omitted:
    // - delegate property + NSSplitViewDelegate protocol methods — need upcall machinery.
    // - fully covered elsewhere: vertical/dividerStyle/setPosition:ofDividerAtIndex:.
    // - NSLayoutPriority is float: holdingPriorityForSubviewAtIndex: needs of(FLOAT,INT) and
    //   setHoldingPriority:forSubviewAtIndex: needs of(VOID,FLOAT,INT) — both absent from the
    //   Sig vocabulary (requested shapes), so they are omitted rather than sent with a wrong shape.

    /// [splitView autosaveName] (may be nil).
    public String autosaveName() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("autosaveName")));
    }

    /// [splitView setAutosaveName:] (nil/empty disables autosaving).
    public void setAutosaveName(String name) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAutosaveName:"), name == null ? MemorySegment.NULL : ObjC.nsstring(name));
    }

    /// [splitView drawDividerInRect:] — draw one divider (override point; callable directly).
    public void drawDividerInRect(NSRect rect) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
            h.invokeExact(peer, ObjC.sel("drawDividerInRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("drawDividerInRect: failed", t);
        }
    }

    /// [splitView dividerColor] — the divider color (may be nil).
    public NSColor dividerColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("dividerColor")));
    }

    /// [splitView dividerThickness] — CGFloat divider thickness.
    public double dividerThickness() {
        ensureInit();
        try {
            return (double) hGetDouble.invokeExact(peer, ObjC.sel("dividerThickness"));
        } catch (Throwable t) {
            throw new RuntimeException("dividerThickness failed", t);
        }
    }

    /// [splitView adjustSubviews] — re-fill the split view with its subviews.
    public void adjustSubviews() {
        ObjC.msgSendVoid(peer, ObjC.sel("adjustSubviews"));
    }

    /// [splitView isSubviewCollapsed:].
    public boolean isSubviewCollapsed(NSView subview) {
        ensureInit();
        try {
            return (boolean) hBoolId.invokeExact(peer, ObjC.sel("isSubviewCollapsed:"), subview.peer());
        } catch (Throwable t) {
            throw new RuntimeException("isSubviewCollapsed: failed", t);
        }
    }

    /// [splitView minPossiblePositionOfDividerAtIndex:].
    public double minPossiblePositionOfDividerAtIndex(long dividerIndex) {
        ensureInit();
        try {
            return (double) hDoubleInt.invokeExact(peer, ObjC.sel("minPossiblePositionOfDividerAtIndex:"), dividerIndex);
        } catch (Throwable t) {
            throw new RuntimeException("minPossiblePositionOfDividerAtIndex: failed", t);
        }
    }

    /// [splitView maxPossiblePositionOfDividerAtIndex:].
    public double maxPossiblePositionOfDividerAtIndex(long dividerIndex) {
        ensureInit();
        try {
            return (double) hDoubleInt.invokeExact(peer, ObjC.sel("maxPossiblePositionOfDividerAtIndex:"), dividerIndex);
        } catch (Throwable t) {
            throw new RuntimeException("maxPossiblePositionOfDividerAtIndex: failed", t);
        }
    }

    /// [splitView arrangesAllSubviews].
    public boolean arrangesAllSubviews() {
        ensureInit();
        try {
            return (boolean) hGetBool.invokeExact(peer, ObjC.sel("arrangesAllSubviews"));
        } catch (Throwable t) {
            throw new RuntimeException("arrangesAllSubviews failed", t);
        }
    }

    /// [splitView setArrangesAllSubviews:].
    public void setArrangesAllSubviews(boolean flag) {
        ensureInit();
        try {
            hSetBool.invokeExact(peer, ObjC.sel("setArrangesAllSubviews:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setArrangesAllSubviews: failed", t);
        }
    }

    /// [splitView arrangedSubviews] — the arranged panes.
    public NSArray arrangedSubviews() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("arrangedSubviews")));
    }

    /// [splitView insertArrangedSubview:atIndex:].
    public void insertArrangedSubview(NSView view, long index) {
        ensureInit();
        try {
            hVoidIdInt.invokeExact(peer, ObjC.sel("insertArrangedSubview:atIndex:"), view.peer(), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertArrangedSubview:atIndex: failed", t);
        }
    }

    /// [splitView removeArrangedSubview:].
    public void removeArrangedSubview(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeArrangedSubview:"), view.peer());
    }
}
