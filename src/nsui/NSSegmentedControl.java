package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSegmentedControl — an AppKit segmented control (a row of adjacent buttons,
/// one selected at a time). Thin, 1:1, stateless wrapper over a native
/// `NSSegmentedControl` (SWT-style): every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer. It is an
/// `NSControl` (an `NSView`), so it fits any view hierarchy and
/// supports enable/disable via `setEnabled`.
///
/// The segment count and per-segment labels must be established before the
/// control draws (`setSegmentCount` then `setLabel` per segment).
public final class NSSegmentedControl extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;    // (id, SEL, NSRect) -> id
    private static MethodHandle hSetCount;     // (id, SEL, long) -> void   [setSegmentCount:]
    private static MethodHandle hSetLabel;     // (id, SEL, id, long) -> void  [setLabel:forSegment:]
    private static MethodHandle hSetSelected;  // (id, SEL, long) -> void   [setSelectedSegment:]
    private static MethodHandle hSetImage;     // (id, SEL, id, long) -> void [setImage:forSegment:]
    private static MethodHandle hImageFor;     // (id, SEL, long) -> id     [imageForSegment:]
    private static MethodHandle hLabelFor;     // (id, SEL, long) -> id     [labelForSegment:]
    private static MethodHandle hSetEnabled;   // (id, SEL, bool, long) -> void [setEnabled:forSegment:]
    private static MethodHandle hIsEnabled;    // (id, SEL, long) -> bool   [isEnabledForSegment:]
    private static MethodHandle hSetWidth;     // (id, SEL, double, long) -> void [setWidth:forSegment:]
    private static MethodHandle hWidthFor;     // (id, SEL, long) -> double [widthForSegment:]
    private static MethodHandle hSetToolTip;   // (id, SEL, id, long) -> void
    private static MethodHandle hToolTipFor;   // (id, SEL, long) -> id
    private static MethodHandle hSetMenu;      // (id, SEL, id, long) -> void
    private static MethodHandle hMenuFor;      // (id, SEL, long) -> id
    private static MethodHandle hSetSelectedFor; // (id, SEL, bool, long) -> void [setSelected:forSegment:]
    private static MethodHandle hIsSelectedFor; // (id, SEL, long) -> bool [isSelectedForSegment:]
    private static MethodHandle hTagForSegment; // (id, SEL, long) -> long [tagForSegment:]
    private static MethodHandle hSetTag; // (id, SEL, long, long) -> void [setTag:forSegment:]
    private static MethodHandle hShowsMenuIndicator; // (id, SEL, long) -> bool [showsMenuIndicatorForSegment:]
    private static MethodHandle hSetShowsMenuIndicator; // (id, SEL, bool, long) -> void [setShowsMenuIndicator:forSegment:]
    private static MethodHandle hBoolInt; // (id, SEL, long) -> bool [selectSegmentWithTag:]
    private static MethodHandle hDouble; // (id, SEL) -> double [doubleValueForSelectedSegment]

    private NSSegmentedControl(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static void ensureInit() {
        if (initialized) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSetCount = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetLabel = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hSetSelected = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetImage = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hImageFor = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hLabelFor = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hSetEnabled = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL, Arg.INT));
        hIsEnabled = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
        hSetWidth = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE, Arg.INT));
        hWidthFor = ObjC.handle(Sig.of(Ret.DOUBLE, Arg.INT));
        hSetToolTip = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hToolTipFor = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hSetMenu = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hMenuFor = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        hSetSelectedFor = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL, Arg.INT));
        hIsSelectedFor = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
        hTagForSegment = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
        hSetTag = ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT));
        hShowsMenuIndicator = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
        hSetShowsMenuIndicator = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL, Arg.INT));
        hBoolInt = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
        hDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        initialized = true;
    }

    /// `[[NSSegmentedControl alloc] initWithFrame:frame]` — a new control at the given rect.
        public static NSSegmentedControl create(NSRect frame) {
        ensureInit();
        return new NSSegmentedControl(ObjC.newView("NSSegmentedControl", frame));
    }

    // ---------------------------------------------------------------- instance API

    /// [control setSegmentCount:] — number of segments.
    public void setSegmentCount(long count) {
        try {
            hSetCount.invokeExact(peer, ObjC.sel("setSegmentCount:"), count);
        } catch (Throwable t) {
            throw new RuntimeException("setSegmentCount: failed", t);
        }
    }

    /// [control segmentCount] — number of segments.
    public long segmentCount() {
        return ObjC.msgSendLong(peer, ObjC.sel("segmentCount"));
    }

    /// [control setLabel:forSegment:] — the text shown on the given segment (0-based).
    public void setLabel(String label, long segment) {
        try {
            hSetLabel.invokeExact(peer, ObjC.sel("setLabel:forSegment:"), ObjC.nsstring(label), segment);
        } catch (Throwable t) {
            throw new RuntimeException("setLabel:forSegment: failed", t);
        }
    }

    /// [control labelForSegment:] — the label for the segment.
    public String labelForSegment(long segment) {
        try {
            MemorySegment s = (MemorySegment) hLabelFor.invokeExact(peer, ObjC.sel("labelForSegment:"), segment);
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("labelForSegment: failed", t);
        }
    }

    /// [control setImage:forSegment:] — set image for segment (nil clears).
    public void setImage(NSImage image, long segment) {
        try {
            MemorySegment img = (image == null) ? MemorySegment.NULL : image.peer();
            hSetImage.invokeExact(peer, ObjC.sel("setImage:forSegment:"), img, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setImage:forSegment: failed", t);
        }
    }

    /// [control imageForSegment:] — image for segment (or nil).
    public MemorySegment imageForSegment(long segment) {
        try {
            return (MemorySegment) hImageFor.invokeExact(peer, ObjC.sel("imageForSegment:"), segment);
        } catch (Throwable t) {
            throw new RuntimeException("imageForSegment: failed", t);
        }
    }

    /// [control setEnabled:forSegment:] — enable/disable a segment.
    public void setEnabledForSegment(boolean enabled, long segment) {
        try {
            hSetEnabled.invokeExact(peer, ObjC.sel("setEnabled:forSegment:"), enabled, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setEnabled:forSegment: failed", t);
        }
    }

    /// [control isEnabledForSegment:] — whether segment is enabled.
    public boolean isEnabledForSegment(long segment) {
        try {
            return (boolean) hIsEnabled.invokeExact(peer, ObjC.sel("isEnabledForSegment:"), segment);
        } catch (Throwable t) {
            throw new RuntimeException("isEnabledForSegment: failed", t);
        }
    }

    /// [control setWidth:forSegment:] — set width for segment (0 = auto).
    public void setWidthForSegment(double width, long segment) {
        try {
            hSetWidth.invokeExact(peer, ObjC.sel("setWidth:forSegment:"), width, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setWidth:forSegment: failed", t);
        }
    }

    /// [control widthForSegment:] — width for segment (0 = auto).
    public double widthForSegment(long segment) {
        try {
            return (double) hWidthFor.invokeExact(peer, ObjC.sel("widthForSegment:"), segment);
        } catch (Throwable t) {
            throw new RuntimeException("widthForSegment: failed", t);
        }
    }

    /// [control setToolTip:forSegment:] — set toolTip for segment.
    public void setToolTipForSegment(String toolTip, long segment) {
        try {
            MemorySegment s = (toolTip == null) ? MemorySegment.NULL : ObjC.nsstring(toolTip);
            hSetToolTip.invokeExact(peer, ObjC.sel("setToolTip:forSegment:"), s, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setToolTip:forSegment: failed", t);
        }
    }

    /// [control toolTipForSegment:] — toolTip for segment.
    public String toolTipForSegment(long segment) {
        try {
            MemorySegment s = (MemorySegment) hToolTipFor.invokeExact(peer, ObjC.sel("toolTipForSegment:"), segment);
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("toolTipForSegment: failed", t);
        }
    }

    /// [control setMenu:forSegment:] — set menu for segment.
    public void setMenuForSegment(NSMenu menu, long segment) {
        try {
            MemorySegment m = (menu == null) ? MemorySegment.NULL : menu.peer();
            hSetMenu.invokeExact(peer, ObjC.sel("setMenu:forSegment:"), m, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setMenu:forSegment: failed", t);
        }
    }

    /// [control menuForSegment:] — menu for segment (or nil).
    public MemorySegment menuForSegment(long segment) {
        try {
            return (MemorySegment) hMenuFor.invokeExact(peer, ObjC.sel("menuForSegment:"), segment);
        } catch (Throwable t) {
            throw new RuntimeException("menuForSegment: failed", t);
        }
    }

    /// [control setSelected:forSegment:] — set selected state for segment (trackingMode SelectAny).
    public void setSelectedForSegment(boolean selected, long segment) {
        try {
            hSetSelectedFor.invokeExact(peer, ObjC.sel("setSelected:forSegment:"), selected, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setSelected:forSegment: failed", t);
        }
    }

    /// [control isSelectedForSegment:] — selected state for segment.
    public boolean isSelectedForSegment(long segment) {
        try {
            return (boolean) hIsSelectedFor.invokeExact(peer, ObjC.sel("isSelectedForSegment:"), segment);
        } catch (Throwable t) {
            throw new RuntimeException("isSelectedForSegment: failed", t);
        }
    }

    /// [control setSelectedSegment:] — select the segment at the given index.
    public void setSelectedSegment(long index) {
        try {
            hSetSelected.invokeExact(peer, ObjC.sel("setSelectedSegment:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("setSelectedSegment: failed", t);
        }
    }

    /// [control selectedSegment] — index of the selected segment, or -1 if none.
    public long selectedSegment() {
        return ObjC.msgSendLong(peer, ObjC.sel("selectedSegment"));
    }

    // ---------------------------------------------------------------- nested enums — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSSegmentedControl.h
    //   NSSegmentSwitchTracking: SelectOne 0, SelectAny 1, Momentary 2, MomentaryAccelerator 3
    //   NSSegmentStyle: Automatic 0, Rounded 1, RoundRect 3, TexturedSquare 4, SmallSquare 6, Separated 8, etc.
    //   NSSegmentDistribution (10.13+): Fit 0, Fill 1, FillEqually 2, FillProportionally 3
    //   NSControlBorderShape (26.0, from NSControl.h): Automatic 0, Capsule 1, RoundedRectangle 2, Circle 3
    // Docs: https://developer.apple.com/documentation/appkit/nssegmentedcontrol/trackingmode
    // OMITTED: +segmentedControlWithLabels:trackingMode:target:action: and
    // +segmentedControlWithImages:trackingMode:target:action: — their (ID,ID,INT,ID,ID) shape
    // (two object args around an INT trackingMode plus target/action) is NOT in the Sig vocabulary
    // (grep Sig.java: no of(Ret.ID, Arg.ID, Arg.INT, Arg.ID, Arg.ID)); use create()+setSegmentCount()+setLabel
    // instead. -minimumSizeWithPrioritizedCompressionOptions: returns NSSize taking NSArray*,
    // shape (SIZE,ID) is NOT in the vocabulary (only SIZE,SIZE and SIZE,ID,SIZE are).

    /// `NSSegmentSwitchTracking` — 0=SelectOne, 1=SelectAny, 2=Momentary, 3=MomentaryAccelerator.
    public enum TrackingMode {
        selectOne(0), selectAny(1), momentary(2), momentaryAccelerator(3);
        public final long value;
        TrackingMode(long v) { this.value = v; }
        public static TrackingMode fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSSegmentStyle` — values from `NSSegmentedControl.h`.
    public enum SegmentStyle {
        automatic(0), rounded(1), texturedRounded(2), roundRect(3), texturedSquare(4), capsule(5), smallSquare(6), separated(8);
        public final long value;
        SegmentStyle(long v) { this.value = v; }
        public static SegmentStyle fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// [control setSegmentStyle:] — NSSegmentStyle (0=Automatic, 1=Rounded, 2=TexturedRounded, ...).
    public void setSegmentStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSegmentStyle:"), style);
    }
    /// Typed overload.
    public void setSegmentStyle(SegmentStyle s) { setSegmentStyle(s.value); }

    /// [control segmentStyle] — current segment style.
    public long segmentStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("segmentStyle"));
    }
    /// Typed getter.
    public SegmentStyle segmentStyleEnum() { return SegmentStyle.fromValue(segmentStyle()); }

    /// [control trackingMode] — NSSegmentSwitchTracking (0=SelectOne,1=SelectAny,2=Momentary...).
    public long trackingMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("trackingMode"));
    }
    /// Typed getter.
    public TrackingMode trackingModeEnum() { return TrackingMode.fromValue(trackingMode()); }

    /// [control setTrackingMode:] — set tracking mode.
    public void setTrackingMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTrackingMode:"), mode);
    }
    /// Typed overload.
    public void setTrackingMode(TrackingMode m) { setTrackingMode(m.value); }

    /// [control tagForSegment:] — tag for segment.
    public long tagForSegment(long segment) {
        try {
            return (long) hTagForSegment.invokeExact(peer, ObjC.sel("tagForSegment:"), segment);
        } catch (Throwable t) {
            throw new RuntimeException("tagForSegment: failed", t);
        }
    }

    /// [control setTag:forSegment:] — set tag for segment.
    public void setTagForSegment(long tag, long segment) {
        try {
            hSetTag.invokeExact(peer, ObjC.sel("setTag:forSegment:"), tag, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setTag:forSegment: failed", t);
        }
    }

    /// [control showsMenuIndicatorForSegment:] — whether segment shows menu indicator.
    public boolean showsMenuIndicatorForSegment(long segment) {
        try {
            return (boolean) hShowsMenuIndicator.invokeExact(peer, ObjC.sel("showsMenuIndicatorForSegment:"), segment);
        } catch (Throwable t) {
            throw new RuntimeException("showsMenuIndicatorForSegment: failed", t);
        }
    }

    /// [control setShowsMenuIndicator:forSegment:] — set menu indicator visibility for segment.
    public void setShowsMenuIndicatorForSegment(boolean flag, long segment) {
        try {
            hSetShowsMenuIndicator.invokeExact(peer, ObjC.sel("setShowsMenuIndicator:forSegment:"), flag, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setShowsMenuIndicator:forSegment: failed", t);
        }
    }

    // ---- completeness: all remaining header API in registered shapes (grep Sig.java per shape) ----
    /// [control selectSegmentWithTag:] — select the segment with the given tag.
    /// Shape (BOOL,INT) is in the vocabulary (grep: of(Ret.BOOL, Arg.INT)).
    public boolean selectSegmentWithTag(long tag) {
        try {
            return (boolean) hBoolInt.invokeExact(peer, ObjC.sel("selectSegmentWithTag:"), tag);
        } catch (Throwable t) {
            throw new RuntimeException("selectSegmentWithTag: failed", t);
        }
    }

    /// [control setImageScaling:forSegment:] — NSImageScaling for the segment.
    /// Shape (VOID,INT,INT) is in the vocabulary (grep: of(Ret.VOID, Arg.INT, Arg.INT));
    /// reuses the hSetTag handle (same shape as setTag:forSegment:).
    public void setImageScalingForSegment(long scaling, long segment) {
        try {
            hSetTag.invokeExact(peer, ObjC.sel("setImageScaling:forSegment:"), scaling, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setImageScaling:forSegment: failed", t);
        }
    }

    /// [control imageScalingForSegment:] — NSImageScaling for the segment.
    /// Shape (INT,INT) is in the vocabulary (grep: of(Ret.INT, Arg.INT));
    /// reuses the hTagForSegment handle (same shape as tagForSegment:).
    public long imageScalingForSegment(long segment) {
        try {
            return (long) hTagForSegment.invokeExact(peer, ObjC.sel("imageScalingForSegment:"), segment);
        } catch (Throwable t) {
            throw new RuntimeException("imageScalingForSegment: failed", t);
        }
    }

    /// [control isSpringLoaded] — deep-press/hover action delivery (10.10.3+).
    /// Shapes (BOOL ()) / (VOID,BOOL) are in the vocabulary.
    public boolean isSpringLoaded() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSpringLoaded"));
    }
    /// [control setSpringLoaded:].
    public void setSpringLoaded(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setSpringLoaded:"), flag);
    }

    /// [control doubleValueForSelectedSegment] — double value for the selected segment
    /// (valid only for trackingMode MomentaryAccelerator).
    /// Shape (DOUBLE ()) is in the vocabulary (grep: of(Ret.DOUBLE)).
    public double doubleValueForSelectedSegment() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("doubleValueForSelectedSegment"));
        } catch (Throwable t) {
            throw new RuntimeException("doubleValueForSelectedSegment failed", t);
        }
    }

    /// [control selectedSegmentBezelColor] — color of the selected segment's bevel, or nil.
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary.
    public NSColor selectedSegmentBezelColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("selectedSegmentBezelColor")));
    }
    /// [control setSelectedSegmentBezelColor:] — nil clears.
    public void setSelectedSegmentBezelColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSelectedSegmentBezelColor:"),
                (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// [control indexOfSelectedItem] — same as selectedSegment (for NSTabView wiring).
    /// Shape (INT ()) is in the vocabulary.
    public long indexOfSelectedItem() {
        return ObjC.msgSendLong(peer, ObjC.sel("indexOfSelectedItem"));
    }

    /// [control setAlignment:forSegment:] — content alignment of the segment (10.13+).
    /// Shape (VOID,INT,INT) is in the vocabulary; reuses hSetTag (same shape).
    public void setAlignmentForSegment(long alignment, long segment) {
        try {
            hSetTag.invokeExact(peer, ObjC.sel("setAlignment:forSegment:"), alignment, segment);
        } catch (Throwable t) {
            throw new RuntimeException("setAlignment:forSegment: failed", t);
        }
    }

    /// [control alignmentForSegment:] — NSTextAlignment of the segment (10.13+).
    /// Shape (INT,INT) is in the vocabulary; reuses hTagForSegment (same shape).
    public long alignmentForSegment(long segment) {
        try {
            return (long) hTagForSegment.invokeExact(peer, ObjC.sel("alignmentForSegment:"), segment);
        } catch (Throwable t) {
            throw new RuntimeException("alignmentForSegment: failed", t);
        }
    }

    /// NSSegmentDistribution — Fit 0, Fill 1, FillEqually 2, FillProportionally 3 (10.13+).
    public enum SegmentDistribution {
        fit(0), fill(1), fillEqually(2), fillProportionally(3);
        public final long value;
        SegmentDistribution(long v) { this.value = v; }
        public static SegmentDistribution fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// [control segmentDistribution] — how dynamic segments fill space (10.13+).
    /// Shapes (INT ()) / (VOID,INT) are in the vocabulary.
    public long segmentDistribution() {
        return ObjC.msgSendLong(peer, ObjC.sel("segmentDistribution"));
    }
    /// Typed getter.
    public SegmentDistribution segmentDistributionEnum() { return SegmentDistribution.fromValue(segmentDistribution()); }
    /// [control setSegmentDistribution:].
    public void setSegmentDistribution(long dist) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSegmentDistribution:"), dist);
    }
    /// Typed overload.
    public void setSegmentDistribution(SegmentDistribution d) { setSegmentDistribution(d.value); }

    /// [control compressWithPrioritizedCompressionOptions:] — compress per options (10.13+).
    /// Shape (VOID,ID) is in the vocabulary.
    public void compressWithPrioritizedCompressionOptions(NSArray options) {
        ObjC.msgSendVoidId(peer, ObjC.sel("compressWithPrioritizedCompressionOptions:"),
                (MemorySegment) (options == null ? MemorySegment.NULL : options.peer()));
    }

    /// [control activeCompressionOptions] — current compression options (10.13+), or nil.
    /// Shape (ID ()) is in the vocabulary.
    public MemorySegment activeCompressionOptions() {
        return ObjC.msgSendId(peer, ObjC.sel("activeCompressionOptions"));
    }

    /// [control borderShape] — NSControlBorderShape (26.0+; Automatic 0, Capsule 1, RoundedRectangle 2, Circle 3).
    /// Shapes (INT ()) / (VOID,INT) are in the vocabulary.
    public long borderShape() {
        return ObjC.msgSendLong(peer, ObjC.sel("borderShape"));
    }
    /// [control setBorderShape:].
    public void setBorderShape(long shape) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setBorderShape:"), shape);
    }

}
