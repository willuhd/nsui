package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSStepper — an AppKit small up/down stepper control. Thin, 1:1, stateless wrapper
 * over a native {@code NSStepper}: every method maps to one {@code objc_msgSend}
 * selector. It is an {@link NSControl} (an {@link NSView}), so it fits any view hierarchy.
 *
 * <p>Value semantics: like {@code NSSlider}, AppKit clamps {@code doubleValue} to
 * {@code [min, max]} and steps by {@code increment}; a value set beyond the range is
 * clamped on read-back. The {@link NSControl#setAction}/{@link NSControl#setTarget}
 * pair lets {@code doubleValue()} retrieve the value after user interaction.
 */
public final class NSStepper extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSetDouble;   // (id, SEL, double) -> void  [setMinValue:/setMaxValue:/setIncrement:/setDoubleValue:]
    private static MethodHandle hDouble;      // (id, SEL) -> double        [doubleValue]

    private NSStepper(MemorySegment peer) {
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

    /** {@code [[NSStepper alloc] initWithFrame:frame]} — a new stepper at the given rect. */
    public static NSStepper create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSStepper"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSStepper", t);
        }
        if (p.address() == 0) {
            throw new IllegalStateException("NSStepper alloc/initWithFrame: returned nil");
        }
        return new NSStepper(p);
    }

    // ---------------------------------------------------------------- instance API

    /** [stepper setMinValue:] — minimum value. */
    public void setMinValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMinValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setMinValue: failed", t);
        }
    }

    /** [stepper minValue] — minimum value. */
    public double minValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("minValue"));
        } catch (Throwable t) {
            throw new RuntimeException("minValue failed", t);
        }
    }

    /** [stepper setMaxValue:] — maximum value. */
    public void setMaxValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMaxValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setMaxValue: failed", t);
        }
    }

    /** [stepper maxValue] — maximum value. */
    public double maxValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("maxValue"));
        } catch (Throwable t) {
            throw new RuntimeException("maxValue failed", t);
        }
    }

    /** [stepper setIncrement:] — the step amount for each click. */
    public void setIncrement(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setIncrement:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setIncrement: failed", t);
        }
    }

    /** [stepper increment] — step amount. */
    public double increment() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("increment"));
        } catch (Throwable t) {
            throw new RuntimeException("increment failed", t);
        }
    }

    /** [stepper setDoubleValue:] — current value (clamped to [min, max]). */
    public void setDoubleValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setDoubleValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setDoubleValue: failed", t);
        }
    }

    /** [stepper doubleValue] — current value (read back). */
    public double doubleValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("doubleValue"));
        } catch (Throwable t) {
            throw new RuntimeException("doubleValue failed", t);
        }
    }

    /** [stepper autorepeat] — whether holding down repeats. */
    public boolean autorepeat() {
        return ObjC.msgSendBool(peer, ObjC.sel("autorepeat"));
    }

    /** [stepper setAutorepeat:] — set autorepeat. */
    public void setAutorepeat(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutorepeat:"), flag);
    }

    /** [stepper valueWraps] — whether wrapping at extremes. */
    public boolean valueWraps() {
        return ObjC.msgSendBool(peer, ObjC.sel("valueWraps"));
    }

    /** [stepper setValueWraps:] — set value wraps. */
    public void setValueWraps(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setValueWraps:"), flag);
    }

    /** [stepper controlSize] — NSControlSize. */
    public long controlSize() {
        return ObjC.msgSendLong(peer, ObjC.sel("controlSize"));
    }

    /** [stepper setControlSize:] — set control size. */
    public void setControlSize(long size) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setControlSize:"), size);
    }

    /** [stepper incrementBy:] — step helper (via increment). */
    public void incrementBy(double delta) {
        // No direct incrementBy: on NSStepper; implement via setDoubleValue
        setDoubleValue(doubleValue() + delta);
    }
}
