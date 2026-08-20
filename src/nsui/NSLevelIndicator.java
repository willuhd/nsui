package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSLevelIndicator — an AppKit level/rating indicator control. Thin, 1:1, stateless
/// wrapper over a native `NSLevelIndicator`: every method maps to one
/// `objc_msgSend` selector. It is an `NSControl` (an `NSView`), so it
/// fits any view hierarchy.
///
/// Style 0 is `NSLevelIndicatorStyleRelevancy`; other styles include
/// `NSLevelIndicatorStyleContinuousCapacity` and
/// `NSLevelIndicatorStyleDiscreteCapacity`.
public final class NSLevelIndicator extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSetStyle;    // (id, SEL, int) -> void   [setLevelIndicatorStyle:]
    private static MethodHandle hSetDouble;   // (id, SEL, double) -> void  [setMinValue:/setMaxValue:/setDoubleValue:]
    private static MethodHandle hDouble;      // (id, SEL) -> double        [doubleValue]

    private NSLevelIndicator(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSetStyle = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        initialized = true;
    }

    /// `[[NSLevelIndicator alloc] initWithFrame:frame]` — a new indicator at the given rect.
    public static NSLevelIndicator create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSLevelIndicator"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSLevelIndicator", t);
        }
        if (p.address() == 0) {
            throw new IllegalStateException("NSLevelIndicator alloc/initWithFrame: returned nil");
        }
        return new NSLevelIndicator(p);
    }

    // ---------------------------------------------------------------- instance API

    /// [indicator setLevelIndicatorStyle:] — NSLevelIndicatorStyle (0 = Relevancy).
    public void setLevelIndicatorStyle(long style) {
        try {
            hSetStyle.invokeExact(peer, ObjC.sel("setLevelIndicatorStyle:"), style);
        } catch (Throwable t) {
            throw new RuntimeException("setLevelIndicatorStyle: failed", t);
        }
    }

    /// [indicator levelIndicatorStyle] — current style.
    public long levelIndicatorStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("levelIndicatorStyle"));
    }

    /// [indicator setMinValue:] — minimum value.
    public void setMinValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMinValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setMinValue: failed", t);
        }
    }

    /// [indicator minValue] — minimum.
    public double minValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("minValue"));
        } catch (Throwable t) {
            throw new RuntimeException("minValue failed", t);
        }
    }

    /// [indicator setMaxValue:] — maximum value.
    public void setMaxValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMaxValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setMaxValue: failed", t);
        }
    }

    /// [indicator maxValue] — maximum.
    public double maxValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("maxValue"));
        } catch (Throwable t) {
            throw new RuntimeException("maxValue failed", t);
        }
    }

    /// [indicator setDoubleValue:] — current level (clamped to [min, max]).
    public void setDoubleValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setDoubleValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setDoubleValue: failed", t);
        }
    }

    /// [indicator doubleValue] — the current level (read back).
    public double doubleValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("doubleValue"));
        } catch (Throwable t) {
            throw new RuntimeException("doubleValue failed", t);
        }
    }

    // ---- new completeness APIs ----

    /// [indicator setWarningValue:] — threshold for warning state.
    public void setWarningValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setWarningValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setWarningValue: failed", t);
        }
    }

    /// [indicator warningValue] — warning threshold.
    public double warningValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("warningValue"));
        } catch (Throwable t) {
            throw new RuntimeException("warningValue failed", t);
        }
    }

    /// [indicator setCriticalValue:] — threshold for critical state.
    public void setCriticalValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setCriticalValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setCriticalValue: failed", t);
        }
    }

    /// [indicator criticalValue] — critical threshold.
    public double criticalValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("criticalValue"));
        } catch (Throwable t) {
            throw new RuntimeException("criticalValue failed", t);
        }
    }

    /// [indicator numberOfTickMarks] — tick mark count (0 = none).
    public long numberOfTickMarks() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfTickMarks"));
    }

    /// [indicator setNumberOfTickMarks:] — set tick count.
    public void setNumberOfTickMarks(long n) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setNumberOfTickMarks:"), n);
    }

    /// [indicator numberOfMajorTickMarks] — major tick count.
    public long numberOfMajorTickMarks() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfMajorTickMarks"));
    }

    /// [indicator setNumberOfMajorTickMarks:] — set major tick count.
    public void setNumberOfMajorTickMarks(long n) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setNumberOfMajorTickMarks:"), n);
    }

    /// [indicator tickMarkPosition] — NSTickMarkPosition (0=Below,1=Above).
    public long tickMarkPosition() {
        return ObjC.msgSendLong(peer, ObjC.sel("tickMarkPosition"));
    }

    /// [indicator setTickMarkPosition:] — set position.
    public void setTickMarkPosition(long pos) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTickMarkPosition:"), pos);
    }

    /// [indicator isEditable] — whether user can edit.
    public boolean isEditable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEditable"));
    }

    /// [indicator setEditable:] — set editable.
    public void setEditable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEditable:"), flag);
    }

    /// [indicator fillColor] — normal-state fill color (or nil).
    public NSColor fillColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("fillColor"));
        return NSColor.wrap(c);
    }

    /// [indicator setFillColor:] — set fill color (nil resets to default).
    public void setFillColor(NSColor color) {
        MemorySegment p = (color == null) ? MemorySegment.NULL : color.peer();
        ObjC.msgSendVoidId(peer, ObjC.sel("setFillColor:"), p);
    }

    /// [indicator warningFillColor] — warning-state fill color.
    public NSColor warningFillColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("warningFillColor"));
        return NSColor.wrap(c);
    }

    /// [indicator setWarningFillColor:] — set warning fill color.
    public void setWarningFillColor(NSColor color) {
        MemorySegment p = (color == null) ? MemorySegment.NULL : color.peer();
        ObjC.msgSendVoidId(peer, ObjC.sel("setWarningFillColor:"), p);
    }

    /// [indicator criticalFillColor] — critical-state fill color.
    public NSColor criticalFillColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("criticalFillColor"));
        return NSColor.wrap(c);
    }

    /// [indicator setCriticalFillColor:] — set critical fill color.
    public void setCriticalFillColor(NSColor color) {
        MemorySegment p = (color == null) ? MemorySegment.NULL : color.peer();
        ObjC.msgSendVoidId(peer, ObjC.sel("setCriticalFillColor:"), p);
    }
}
