package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/**
 * NSEvent — a native event from the run loop. Thin wrapper; the fields you
 * need are pulled from AppKit on demand.
 */
public final class NSEvent extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hLocation;  // (id, SEL) -> NSPoint (POINT is a GROUP return -> leading SegmentAllocator)
    private static MethodHandle hTilt;      // (id, SEL) -> NSPoint
    private static MethodHandle hDouble;    // (id, SEL) -> double
    private static MethodHandle hBool;      // (id, SEL) -> bool

    NSEvent(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hLocation = ObjC.handle(Sig.of(Ret.POINT));
        hTilt = ObjC.handle(Sig.of(Ret.POINT));
        hDouble   = ObjC.handle(Sig.of(Ret.DOUBLE));
        hBool     = ObjC.handle(Sig.of(Ret.BOOL));
        initialized = true;
    }

    /** NSEventType (NSUInteger). 1=leftMouseDown 2=leftMouseUp 10=keyDown 11=keyUp. */
    public long type() {
        return ObjC.msgSendLong(peer, ObjC.sel("type"));
    }

    /** NSEventTypeLeftMouseDown (1) / LeftMouseUp (2). */
    public boolean isMouseEvent() {
        long t = type();
        return t >= 1 && t <= 9;
    }

    /** NSEventTypeKeyDown (10) / KeyUp (11). */
    public boolean isKeyEvent() {
        long t = type();
        return t == 10 || t == 11;
    }

    private void requireKeyEvent(String accessor) {
        if (!isKeyEvent()) {
            throw new IllegalStateException(accessor + " is only valid for key events (type 10/11); current type=" + type());
        }
    }

    /**
     * Characters of a KEY event (NSString -> String).
     *
     * <p><b>Key events only.</b> Guarded: throws IllegalStateException if not a key event.
     */
    public String characters() {
        requireKeyEvent("characters");
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("characters")));
    }

    /**
     * [event locationInWindow] — mouse location in the window's base coordinate
     * system (origin bottom-left). POINT is a GROUP return, so the downcall handle
     * carries an implicit leading SegmentAllocator for the struct it returns.
     */
    public NSPoint locationInWindow() {
        try {
            MemorySegment seg = (MemorySegment) hLocation.invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("locationInWindow"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("locationInWindow failed", t);
        }
    }

    /** [event modifierFlags] — a bitmask (of NSEventModifierFlags, NSUInteger). */
    public long modifierFlags() {
        return ObjC.msgSendLong(peer, ObjC.sel("modifierFlags"));
    }

    /**
     * [event keyCode] — hardware keyboard code.
     *
     * <p><b>Key events only.</b> Guarded.
     */
    public long keyCode() {
        requireKeyEvent("keyCode");
        return ObjC.msgSendLong(peer, ObjC.sel("keyCode"));
    }

    /** [event buttonNumber] — which mouse button generated the event. */
    public long buttonNumber() {
        return ObjC.msgSendLong(peer, ObjC.sel("buttonNumber"));
    }

    /** [event clickCount] — how many clicks this event represents. */
    public long clickCount() {
        return ObjC.msgSendLong(peer, ObjC.sel("clickCount"));
    }

    /** [event timestamp] — system time of the event in seconds. */
    public double timestamp() {
        try {
            return (double) hDouble.invokeExact(peer, ObjC.sel("timestamp"));
        } catch (Throwable t) {
            throw new RuntimeException("timestamp failed", t);
        }
    }

    /** [event windowNumber] — the window the event is associated with (0 if none). */
    public long windowNumber() {
        return ObjC.msgSendLong(peer, ObjC.sel("windowNumber"));
    }

    /**
     * Characters of a KEY event ignoring the current modifier layout (NSString -> String).
     *
     * <p><b>Key events only.</b> Guarded.
     */
    public String charactersIgnoringModifiers() {
        requireKeyEvent("charactersIgnoringModifiers");
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("charactersIgnoringModifiers")));
    }

    /** [event isARepeat] — true if key is auto-repeat. Key events only — guarded. */
    public boolean isARepeat() {
        requireKeyEvent("isARepeat");
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isARepeat")); } catch (Throwable t) { throw new RuntimeException("isARepeat failed", t); }
    }

    // ---- additional accessors (80% completeness) ----

    /** [event subtype] — NSEventSubtype. */
    public long subtype() { return ObjC.msgSendLong(peer, ObjC.sel("subtype")); }

    /** [event eventNumber] */
    public long eventNumber() { return ObjC.msgSendLong(peer, ObjC.sel("eventNumber")); }

    /** [event data1] */
    public long data1() { return ObjC.msgSendLong(peer, ObjC.sel("data1")); }

    /** [event data2] */
    public long data2() { return ObjC.msgSendLong(peer, ObjC.sel("data2")); }

    /** [event pressure] — float but returned as double via handle. */
    public double pressure() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("pressure")); } catch (Throwable t) { throw new RuntimeException("pressure failed", t); }
    }

    /** [event deltaX] */
    public double deltaX() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("deltaX")); } catch (Throwable t) { throw new RuntimeException("deltaX failed", t); }
    }

    /** [event deltaY] */
    public double deltaY() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("deltaY")); } catch (Throwable t) { throw new RuntimeException("deltaY failed", t); }
    }

    /** [event deltaZ] */
    public double deltaZ() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("deltaZ")); } catch (Throwable t) { throw new RuntimeException("deltaZ failed", t); }
    }

    /** [event scrollingDeltaX] */
    public double scrollingDeltaX() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("scrollingDeltaX")); } catch (Throwable t) { throw new RuntimeException("scrollingDeltaX failed", t); }
    }

    /** [event scrollingDeltaY] */
    public double scrollingDeltaY() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("scrollingDeltaY")); } catch (Throwable t) { throw new RuntimeException("scrollingDeltaY failed", t); }
    }

    /** [event hasPreciseScrollingDeltas] */
    public boolean hasPreciseScrollingDeltas() {
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("hasPreciseScrollingDeltas")); } catch (Throwable t) { throw new RuntimeException("hasPreciseScrollingDeltas failed", t); }
    }

    /** [event momentumPhase] */
    public long momentumPhase() { return ObjC.msgSendLong(peer, ObjC.sel("momentumPhase")); }

    /** [event phase] */
    public long phase() { return ObjC.msgSendLong(peer, ObjC.sel("phase")); }

    /** [event isDirectionInvertedFromDevice] */
    public boolean isDirectionInvertedFromDevice() {
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isDirectionInvertedFromDevice")); } catch (Throwable t) { throw new RuntimeException("isDirectionInvertedFromDevice failed", t); }
    }

    /** [event trackingNumber] */
    public long trackingNumber() { return ObjC.msgSendLong(peer, ObjC.sel("trackingNumber")); }

    /** [event magnification] */
    public double magnification() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("magnification")); } catch (Throwable t) { throw new RuntimeException("magnification failed", t); }
    }

    /** [event deviceID] — NSUInteger */
    public long deviceID() { return ObjC.msgSendLong(peer, ObjC.sel("deviceID")); }

    /** [event rotation] — float degrees */
    public double rotation() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("rotation")); } catch (Throwable t) { throw new RuntimeException("rotation failed", t); }
    }

    /** [event absoluteX] */
    public long absoluteX() { return ObjC.msgSendLong(peer, ObjC.sel("absoluteX")); }
    /** [event absoluteY] */
    public long absoluteY() { return ObjC.msgSendLong(peer, ObjC.sel("absoluteY")); }
    /** [event absoluteZ] */
    public long absoluteZ() { return ObjC.msgSendLong(peer, ObjC.sel("absoluteZ")); }

    /** [event buttonMask] — NSEventButtonMask */
    public long buttonMask() { return ObjC.msgSendLong(peer, ObjC.sel("buttonMask")); }

    /** [event tilt] — NSPoint {x,y} tilt */
    public NSPoint tilt() {
        try {
            MemorySegment seg = (MemorySegment) hTilt.invokeExact((SegmentAllocator) Arena.global(), peer, ObjC.sel("tilt"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("tilt failed", t); }
    }

    /** [event tangentialPressure] */
    public double tangentialPressure() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("tangentialPressure")); } catch (Throwable t) { throw new RuntimeException("tangentialPressure failed", t); }
    }

    /** [event stage] — pressure stage */
    public long stage() { return ObjC.msgSendLong(peer, ObjC.sel("stage")); }

    /** [event stageTransition] */
    public double stageTransition() {
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("stageTransition")); } catch (Throwable t) { throw new RuntimeException("stageTransition failed", t); }
    }

    /** [event associatedEventsMask] */
    public long associatedEventsMask() { return ObjC.msgSendLong(peer, ObjC.sel("associatedEventsMask")); }

    /** [event window] — NSWindow peer or null. */
    public MemorySegment window() {
        MemorySegment w = ObjC.msgSendId(peer, ObjC.sel("window"));
        return (w == null || w.address() == 0) ? null : w;
    }

    /** [event charactersByApplyingModifiers:] — key events only, guarded. */
    public String charactersByApplyingModifiers(long modifiers) {
        requireKeyEvent("charactersByApplyingModifiers:");
        try {
            var h = ObjC.handle(Sig.of(Sig.Ret.ID, Sig.Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(peer, ObjC.sel("charactersByApplyingModifiers:"), modifiers);
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("charactersByApplyingModifiers: failed", t); }
    }

    /** [NSEvent mouseLocation] — class property NSPoint */
    public static NSPoint mouseLocation() {
        try {
            MemorySegment seg = (MemorySegment) ObjC.handle(Sig.of(Ret.POINT)).invokeExact((SegmentAllocator) Arena.global(), ObjC.cls("NSEvent"), ObjC.sel("mouseLocation"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("mouseLocation failed", t); }
    }

    /** [NSEvent modifierFlags] — class property */
    public static long modifierFlagsStatic() {
        return ObjC.msgSendLong(ObjC.cls("NSEvent"), ObjC.sel("modifierFlags"));
    }

    /** [NSEvent pressedMouseButtons] */
    public static long pressedMouseButtons() {
        return ObjC.msgSendLong(ObjC.cls("NSEvent"), ObjC.sel("pressedMouseButtons"));
    }

    /** [NSEvent doubleClickInterval] */
    public static double doubleClickInterval() {
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(ObjC.cls("NSEvent"), ObjC.sel("doubleClickInterval")); } catch (Throwable t) { throw new RuntimeException("doubleClickInterval failed", t); }
    }

    /** [NSEvent keyRepeatDelay] */
    public static double keyRepeatDelay() {
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(ObjC.cls("NSEvent"), ObjC.sel("keyRepeatDelay")); } catch (Throwable t) { throw new RuntimeException("keyRepeatDelay failed", t); }
    }

    /** [NSEvent keyRepeatInterval] */
    public static double keyRepeatInterval() {
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(ObjC.cls("NSEvent"), ObjC.sel("keyRepeatInterval")); } catch (Throwable t) { throw new RuntimeException("keyRepeatInterval failed", t); }
    }
}
