package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSlider — an AppKit horizontal double-value slider control. Thin, 1:1,
/// stateless wrapper over a native `NSSlider`: every method maps to one
/// `objc_msgSend` selector. It is an `NSControl` (an `NSView`),
/// so it fits any view hierarchy and supports enable/disable via `setEnabled`.
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSSlider.h
/// (+ NSSliderCell.h for NSSliderType 0=Linear/1=Circular and NSTickMarkPosition 0=Below/1=Above)
/// OMITTED: -rectOfTickMarkAtIndex: returns NSRect taking INT, shape (RECT,INT) is NOT in
/// the Sig vocabulary (grep Sig.java: no of(Ret.RECT, Arg.INT)); -indexOfTickMarkAtPoint:
/// takes NSPoint returning INT, shape (INT,POINT) is NOT in the vocabulary (only ID,POINT is);
/// -closestTickMarkValueToValue: is (DOUBLE,DOUBLE), NOT in the vocabulary (only DOUBLE,ID and
/// DOUBLE,INT are); +sliderWithValue:minValue:maxValue:target:action: (DOUBLE x3 + ID x2) has no
/// registered shape; deprecated -title/-titleCell/-titleColor/-titleFont/-image family (no effect
/// since 10.0, use the control's own value instead).
public final class NSSlider extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSetDouble;   // (id, SEL, double) -> void  [setDoubleValue:/setMinValue:/setMaxValue:]
    private static MethodHandle hDouble;      // (id, SEL) -> double        [doubleValue]
    private static MethodHandle hBoolId;      // (id, SEL, id) -> bool      [acceptsFirstMouse:]
    private static MethodHandle hDoubleInt;   // (id, SEL, long) -> double  [tickMarkValueAtIndex:]
    private static MethodHandle hFactory2;    // (id, SEL, id, id) -> id    [sliderWithTarget:action:]

    private NSSlider(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hBoolId = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        hDoubleInt = ObjC.handle(Sig.of(Ret.DOUBLE, Arg.INT));
        hFactory2 = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
        initialized = true;
    }

    /// `[[NSSlider alloc] initWithFrame:frame]` — a new slider at the given rect.
        public static NSSlider create(NSRect frame) {
        ensureInit();
        return new NSSlider(ObjC.newView("NSSlider", frame));
    }

    // ---------------------------------------------------------------- instance API

    /// [slider setMinValue:] — the slider's minimum value.
    public void setMinValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMinValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setMinValue: failed", t);
        }
    }

    /// [slider minValue] — minimum value.
    public double minValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("minValue"));
        } catch (Throwable t) {
            throw new RuntimeException("minValue failed", t);
        }
    }

    /// [slider setMaxValue:] — the slider's maximum value.
    public void setMaxValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMaxValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setMaxValue: failed", t);
        }
    }

    /// [slider maxValue] — maximum value.
    public double maxValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("maxValue"));
        } catch (Throwable t) {
            throw new RuntimeException("maxValue failed", t);
        }
    }

    /// [slider setDoubleValue:] — the slider's current value.
    public void setDoubleValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setDoubleValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setDoubleValue: failed", t);
        }
    }

    /// [slider doubleValue] — the slider's current value.
    public double doubleValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("doubleValue"));
        } catch (Throwable t) {
            throw new RuntimeException("doubleValue failed", t);
        }
    }

    /// [slider setNumberOfTickMarks:] — number of tick marks rendered (0 = none).
    public void setNumberOfTickMarks(long n) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setNumberOfTickMarks:"), n);
    }

    /// [slider numberOfTickMarks] — number of tick marks.
    public long numberOfTickMarks() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfTickMarks"));
    }

    /// [slider setAllowsTickMarkValuesOnly:] — snap the knob to tick marks only.
    public void setAllowsTickMarkValuesOnly(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsTickMarkValuesOnly:"), flag);
    }

    /// [slider allowsTickMarkValuesOnly] — whether snap is enabled.
    public boolean allowsTickMarkValuesOnly() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsTickMarkValuesOnly"));
    }

    // ---- new completeness APIs ----

    /// [slider isVertical] — whether the slider is vertical.
    public boolean isVertical() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVertical"));
    }

    /// [slider setVertical:] — set vertical orientation.
    public void setVertical(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setVertical:"), flag);
    }

    /// [slider trackFillColor] — fill color of the filled track portion (or nil).
    public NSColor trackFillColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("trackFillColor"));
        return NSColor.wrap(c);
    }

    /// [slider setTrackFillColor:] — set the track fill color (nil clears).
    public void setTrackFillColor(NSColor color) {
        MemorySegment p = (color == null) ? MemorySegment.NULL : color.peer();
        ObjC.msgSendVoidId(peer, ObjC.sel("setTrackFillColor:"), p);
    }

    /// [slider knobThickness] — thickness of the knob (CGFloat).
    public double knobThickness() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("knobThickness"));
        } catch (Throwable t) {
            throw new RuntimeException("knobThickness failed", t);
        }
    }

    /// [slider sliderType] — NSSliderType (0=Linear, 1=Circular).
    public long sliderType() {
        return ObjC.msgSendLong(peer, ObjC.sel("sliderType"));
    }

    /// [slider setSliderType:] — set slider type.
    public void setSliderType(long type) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSliderType:"), type);
    }

    /// [slider altIncrementValue] — alternate increment (option-key).
    public double altIncrementValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("altIncrementValue"));
        } catch (Throwable t) {
            throw new RuntimeException("altIncrementValue failed", t);
        }
    }

    /// [slider setAltIncrementValue:] — set alternate increment.
    public void setAltIncrementValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setAltIncrementValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setAltIncrementValue: failed", t);
        }
    }

    /// [slider tickMarkPosition] — NSTickMarkPosition (0=Below,1=Above).
    public long tickMarkPosition() {
        return ObjC.msgSendLong(peer, ObjC.sel("tickMarkPosition"));
    }

    /// [slider setTickMarkPosition:] — set tick mark position.
    public void setTickMarkPosition(long pos) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTickMarkPosition:"), pos);
    }

    // ---- completeness: remaining header API in registered shapes ----
    /// [slider neutralValue] — value the slider fills from (26.0+; defaults to minValue).
    /// Shapes (DOUBLE ()) / (VOID,DOUBLE) are in the vocabulary; reuses hDouble/hSetDouble.
    public double neutralValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("neutralValue"));
        } catch (Throwable t) {
            throw new RuntimeException("neutralValue failed", t);
        }
    }
    /// [slider setNeutralValue:].
    public void setNeutralValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setNeutralValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setNeutralValue: failed", t);
        }
    }

    /// [slider acceptsFirstMouse:] — whether a click-through activates and tracks.
    /// Shape (BOOL,ID) is in the vocabulary (grep: of(Ret.BOOL, Arg.ID)).
    public boolean acceptsFirstMouse(MemorySegment event) {
        try {
            return (boolean) hBoolId.invokeExact(peer, ObjC.sel("acceptsFirstMouse:"),
                    (MemorySegment) (event == null ? MemorySegment.NULL : event));
        } catch (Throwable t) {
            throw new RuntimeException("acceptsFirstMouse: failed", t);
        }
    }

    /// NSTintProminence — Automatic 0, None 1, Primary 2, Secondary 3 (26.0+, from NSTintProminence.h).
    /// Shapes (INT ()) / (VOID,INT) are in the vocabulary.
    public long tintProminence() {
        return ObjC.msgSendLong(peer, ObjC.sel("tintProminence"));
    }
    /// [slider setTintProminence:].
    public void setTintProminence(long prominence) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTintProminence:"), prominence);
    }

    /// [slider tickMarkValueAtIndex:] — slider value for a tick mark (raises on bad index).
    /// Shape (DOUBLE,INT) is in the vocabulary (grep: of(Ret.DOUBLE, Arg.INT)).
    public double tickMarkValueAtIndex(long index) {
        try {
            return (double) hDoubleInt.invokeExact(peer, ObjC.sel("tickMarkValueAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("tickMarkValueAtIndex: failed", t);
        }
    }

    /// +sliderWithTarget:action: — continuous horizontal slider over 0.0..1.0 (10.12+).
    /// Shape (ID,ID,ID) is in the vocabulary (grep: of(Ret.ID, Arg.ID, Arg.ID)).
    public static NSSlider sliderWithTarget(MemorySegment target, String actionSelector) {
        ensureInit();
        try {
            MemorySegment t = (MemorySegment) (target == null ? MemorySegment.NULL : target);
            MemorySegment a = actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector);
            MemorySegment p = (MemorySegment) hFactory2.invokeExact(ObjC.cls("NSSlider"),
                    ObjC.sel("sliderWithTarget:action:"), t, a);
            if (p == null || p.address() == 0) throw new IllegalStateException("sliderWithTarget:action: returned nil");
            return new NSSlider(p);
        } catch (Throwable t) {
            throw new RuntimeException("sliderWithTarget:action: failed", t);
        }
    }
}
