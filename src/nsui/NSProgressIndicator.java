package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSProgressIndicator — an AppKit progress bar (bar style by default). Thin, 1:1,
/// stateless wrapper over a native `NSProgressIndicator`: every method maps
/// to one `objc_msgSend` selector. It is an `NSControl` (an
/// `NSView`), so it fits any view hierarchy.
public final class NSProgressIndicator extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSetDouble;   // (id, SEL, double) -> void  [setDoubleValue:/setMinValue:/setMaxValue:]
    private static MethodHandle hDouble;      // (id, SEL) -> double        [doubleValue]

    private NSProgressIndicator(MemorySegment peer) {
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

    /// `[[NSProgressIndicator alloc] initWithFrame:frame]` — a new progress indicator at the given rect.
    public static NSProgressIndicator create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSProgressIndicator"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSProgressIndicator", t);
        }
        if (p.address() == 0) {
            throw new IllegalStateException("NSProgressIndicator alloc/initWithFrame: returned nil");
        }
        return new NSProgressIndicator(p);
    }

    // ---------------------------------------------------------------- instance API

    /// [indicator setIndeterminate:] — YES for a pulsing spinner, NO for a determinate bar.
    public void setIndeterminate(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setIndeterminate:"), flag);
    }

    /// [indicator isIndeterminate] — the current indeterminate state.
    public boolean isIndeterminate() {
        return ObjC.msgSendBool(peer, ObjC.sel("isIndeterminate"));
    }

    /// [indicator setStyle:] — NSProgressIndicatorStyle (0 = NSProgressIndicatorBarStyle).
    public void setStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStyle:"), style);
    }

    /// [indicator style] — current style.
    public long style() {
        return ObjC.msgSendLong(peer, ObjC.sel("style"));
    }

    /// [indicator setMinValue:] — minimum of the determinate range.
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

    /// [indicator setMaxValue:] — maximum of the determinate range.
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

    /// [indicator setDoubleValue:] — current fraction shown in determinate mode.
    public void setDoubleValue(double v) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setDoubleValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setDoubleValue: failed", t);
        }
    }

    /// [indicator doubleValue] — current value (getter, previously write-only).
    public double doubleValue() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("doubleValue"));
        } catch (Throwable t) {
            throw new RuntimeException("doubleValue failed", t);
        }
    }

    /// [indicator incrementBy:] — increment by delta.
    public void incrementBy(double delta) {
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("incrementBy:"), delta);
        } catch (Throwable t) {
            throw new RuntimeException("incrementBy: failed", t);
        }
    }

    /// [indicator isDisplayedWhenStopped] — whether hidden when stopped.
    public boolean isDisplayedWhenStopped() {
        return ObjC.msgSendBool(peer, ObjC.sel("isDisplayedWhenStopped"));
    }

    /// [indicator setDisplayedWhenStopped:] — set displayed-when-stopped.
    public void setDisplayedWhenStopped(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDisplayedWhenStopped:"), flag);
    }

    /// [indicator controlTint] — NSControlTint (deprecated but present).
    public long controlTint() {
        return ObjC.msgSendLong(peer, ObjC.sel("controlTint"));
    }

    /// [indicator setControlTint:] — set tint.
    public void setControlTint(long tint) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setControlTint:"), tint);
    }

    /// [indicator controlSize] — NSControlSize (0=Regular,1=Small,2=Mini,3=Large).
    public long controlSize() {
        return ObjC.msgSendLong(peer, ObjC.sel("controlSize"));
    }

    /// [indicator setControlSize:] — set control size.
    public void setControlSize(long size) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setControlSize:"), size);
    }

    /// [indicator usesThreadedAnimation] — whether uses threaded animation.
    public boolean usesThreadedAnimation() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesThreadedAnimation"));
    }

    /// [indicator setUsesThreadedAnimation:] — set threaded animation.
    public void setUsesThreadedAnimation(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesThreadedAnimation:"), flag);
    }

    /// [indicator startAnimation:] — begin animating (spinner) / start tracking (bar).
    public void startAnimation() {
        ObjC.msgSendVoidId(peer, ObjC.sel("startAnimation:"), MemorySegment.NULL);
    }

    /// [indicator stopAnimation:] — stop animating (spinner) / stop tracking (bar).
    public void stopAnimation() {
        ObjC.msgSendVoidId(peer, ObjC.sel("stopAnimation:"), MemorySegment.NULL);
    }

    /// [indicator sizeToFit] — size to recommended dimensions.
    public void sizeToFit() {
        ObjC.msgSendVoid(peer, ObjC.sel("sizeToFit"));
    }
}
