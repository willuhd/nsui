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
public final class NSSlider extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSetDouble;   // (id, SEL, double) -> void  [setDoubleValue:/setMinValue:/setMaxValue:]
    private static MethodHandle hDouble;      // (id, SEL) -> double        [doubleValue]

    private NSSlider(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        initialized = true;
    }

    /// `[[NSSlider alloc] initWithFrame:frame]` — a new slider at the given rect.
    public static NSSlider create(NSRect frame) {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSSlider"), ObjC.sel("alloc"));
        try {
            s = (MemorySegment) hInitFrame.invokeExact(s, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSSlider", t);
        }
        if (s.address() == 0) {
            throw new IllegalStateException("NSSlider alloc/initWithFrame: returned nil");
        }
        return new NSSlider(s);
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
}
