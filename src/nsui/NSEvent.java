package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// NSEvent — a native event from the run loop. Thin wrapper; the fields you
/// need are pulled from AppKit on demand.
///
/// Coverage notes (header: NSEvent.h wins on API truth):
/// - Wrapped: every scalar/point/object property whose shape is in Sig
///   (type, modifierFlags, timestamp, window, windowNumber, clickCount,
///   buttonNumber, eventNumber, pressure, locationInWindow, delta/scroll
///   deltas, momentum/phase, direction-inversion, characters family,
///   keyCode, trackingNumber, subtype, data1/2, magnification, deviceID,
///   rotation, absoluteXYZ, buttonMask, tilt, tangentialPressure, stage
///   family, associatedEventsMask, plus the tablet/proximity block below,
///   the class properties, the periodic-event switch and the touch set
///   readers).
/// - Wrapped (monitors): removeMonitor: — unlike its addGlobal/addLocal
///   siblings it takes no block (`(void,id)`, in Sig), so it is wrapped
///   while the block-taking adders stay omitted below.
/// - Omitted: mouseEventWithType:/keyEventWithType:/enterExitEventWithType:/
///   otherEventWithType: factories (mixed point+scalar multi-arg shapes have
///   no vocabulary entry — e.g. requested `of(Ret.ID, Arg.INT, Arg.POINT,
///   Arg.INT, Arg.DOUBLE, Arg.INT, Arg.ID, Arg.INT, Arg.INT, Arg.FLOAT)`);
///   addGlobalMonitorForEventsMatchingMask:/addLocalMonitor… and
///   trackSwipeEventWithOptions:… (block-taking methods need upcall
///   machinery); the deprecated `context` property.
///
/// AppKit type-gating (verified empirically, one process per accessor): many
/// getters raise an uncatchable NSException on the wrong event type — e.g.
/// data1/data2, scrolling deltas, phase, direction-inversion, trackingNumber,
/// magnification, stage/stageTransition, pressureBehavior, trackingArea,
/// userData and every touch reader on a mouse event; clickCount, subtype,
/// eventNumber and pressure on a key event; pressure and eventNumber on a
/// scroll event. Callers must match accessor to event type (the key-event
/// family is additionally guarded in Java); eventWithCGEvent:/eventWithEventRef:
/// reject NULL in Java because AppKit aborts on NULL.
public final class NSEvent extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hLocation, MethodHandle hDouble, MethodHandle hBool, MethodHandle hCharsByModifiers) {}
    private static volatile Handles handles;

    NSEvent(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Sig.Ret.ID, Sig.Arg.INT))
        );
    }

    /// NSEventType (NSUInteger). 1=leftMouseDown 2=leftMouseUp 10=keyDown 11=keyUp.
    public long type() {
        return ObjC.msgSendLong(peer, ObjC.sel("type"));
    }

    /// NSEventTypeLeftMouseDown (1) / LeftMouseUp (2).
    public boolean isMouseEvent() {
        long t = type();
        return t >= 1 && t <= 9;
    }

    /// NSEventTypeKeyDown (10) / KeyUp (11).
    public boolean isKeyEvent() {
        long t = type();
        return t == 10 || t == 11;
    }

    private void requireKeyEvent(String accessor) {
        if (!isKeyEvent()) {
            throw new IllegalStateException(accessor + " is only valid for key events (type 10/11); current type=" + type());
        }
    }

    /// Characters of a KEY event (NSString -> String).
    ///
    /// **Key events only.** Guarded: throws IllegalStateException if not a key event.
    public String characters() {
        requireKeyEvent("characters");
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("characters")));
    }

    /// [event locationInWindow] — mouse location in the window's base coordinate
    /// system (origin bottom-left). POINT is a GROUP return, so the downcall handle
    /// carries an implicit leading SegmentAllocator for the struct it returns.
    public NSPoint locationInWindow() {
        try {
            MemorySegment seg = (MemorySegment) handles.hLocation().invokeExact(ObjC.structSlot(), peer, ObjC.sel("locationInWindow"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("locationInWindow failed", t);
        }
    }

    /// [event modifierFlags] — a bitmask (of NSEventModifierFlags, NSUInteger).
    public long modifierFlags() {
        return ObjC.msgSendLong(peer, ObjC.sel("modifierFlags"));
    }

    /// [event keyCode] — hardware keyboard code.
    ///
    /// **Key events only.** Guarded.
    public long keyCode() {
        requireKeyEvent("keyCode");
        return ObjC.msgSendLong(peer, ObjC.sel("keyCode"));
    }

    /// [event buttonNumber] — which mouse button generated the event.
    public long buttonNumber() {
        return ObjC.msgSendLong(peer, ObjC.sel("buttonNumber"));
    }

    /// [event clickCount] — how many clicks this event represents.
    public long clickCount() {
        return ObjC.msgSendLong(peer, ObjC.sel("clickCount"));
    }

    /// [event timestamp] — system time of the event in seconds.
    public double timestamp() {
        try {
            return (double) handles.hDouble().invokeExact(peer, ObjC.sel("timestamp"));
        } catch (Throwable t) {
            throw new RuntimeException("timestamp failed", t);
        }
    }

    /// [event windowNumber] — the window the event is associated with (0 if none).
    public long windowNumber() {
        return ObjC.msgSendLong(peer, ObjC.sel("windowNumber"));
    }

    /// Characters of a KEY event ignoring the current modifier layout (NSString -> String).
    ///
    /// **Key events only.** Guarded.
    public String charactersIgnoringModifiers() {
        requireKeyEvent("charactersIgnoringModifiers");
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("charactersIgnoringModifiers")));
    }

    /// [event isARepeat] — true if key is auto-repeat. Key events only — guarded.
    public boolean isARepeat() {
        requireKeyEvent("isARepeat");
        try { return (boolean) handles.hBool().invokeExact(peer, ObjC.sel("isARepeat")); } catch (Throwable t) { throw new RuntimeException("isARepeat failed", t); }
    }

    // ---- additional accessors (80% completeness) ----

    /// [event subtype] — NSEventSubtype.
    public long subtype() { return ObjC.msgSendLong(peer, ObjC.sel("subtype")); }

    /// [event eventNumber]
    public long eventNumber() { return ObjC.msgSendLong(peer, ObjC.sel("eventNumber")); }

    /// [event data1]
    public long data1() { return ObjC.msgSendLong(peer, ObjC.sel("data1")); }

    /// [event data2]
    public long data2() { return ObjC.msgSendLong(peer, ObjC.sel("data2")); }

    /// [event pressure] — float but returned as double via handle.
    public double pressure() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("pressure")); } catch (Throwable t) { throw new RuntimeException("pressure failed", t); }
    }

    /// [event deltaX]
    public double deltaX() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("deltaX")); } catch (Throwable t) { throw new RuntimeException("deltaX failed", t); }
    }

    /// [event deltaY]
    public double deltaY() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("deltaY")); } catch (Throwable t) { throw new RuntimeException("deltaY failed", t); }
    }

    /// [event deltaZ]
    public double deltaZ() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("deltaZ")); } catch (Throwable t) { throw new RuntimeException("deltaZ failed", t); }
    }

    /// [event scrollingDeltaX]
    public double scrollingDeltaX() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("scrollingDeltaX")); } catch (Throwable t) { throw new RuntimeException("scrollingDeltaX failed", t); }
    }

    /// [event scrollingDeltaY]
    public double scrollingDeltaY() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("scrollingDeltaY")); } catch (Throwable t) { throw new RuntimeException("scrollingDeltaY failed", t); }
    }

    /// [event hasPreciseScrollingDeltas]
    public boolean hasPreciseScrollingDeltas() {
        try { return (boolean) handles.hBool().invokeExact(peer, ObjC.sel("hasPreciseScrollingDeltas")); } catch (Throwable t) { throw new RuntimeException("hasPreciseScrollingDeltas failed", t); }
    }

    /// [event momentumPhase]
    public long momentumPhase() { return ObjC.msgSendLong(peer, ObjC.sel("momentumPhase")); }

    /// [event phase]
    public long phase() { return ObjC.msgSendLong(peer, ObjC.sel("phase")); }

    /// [event isDirectionInvertedFromDevice]
    public boolean isDirectionInvertedFromDevice() {
        try { return (boolean) handles.hBool().invokeExact(peer, ObjC.sel("isDirectionInvertedFromDevice")); } catch (Throwable t) { throw new RuntimeException("isDirectionInvertedFromDevice failed", t); }
    }

    /// [event trackingNumber]
    public long trackingNumber() { return ObjC.msgSendLong(peer, ObjC.sel("trackingNumber")); }

    /// [event magnification]
    public double magnification() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("magnification")); } catch (Throwable t) { throw new RuntimeException("magnification failed", t); }
    }

    /// [event deviceID] — NSUInteger
    public long deviceID() { return ObjC.msgSendLong(peer, ObjC.sel("deviceID")); }

    /// [event rotation] — float degrees
    public double rotation() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("rotation")); } catch (Throwable t) { throw new RuntimeException("rotation failed", t); }
    }

    /// [event absoluteX]
    public long absoluteX() { return ObjC.msgSendLong(peer, ObjC.sel("absoluteX")); }
    /// [event absoluteY]
    public long absoluteY() { return ObjC.msgSendLong(peer, ObjC.sel("absoluteY")); }
    /// [event absoluteZ]
    public long absoluteZ() { return ObjC.msgSendLong(peer, ObjC.sel("absoluteZ")); }

    /// [event buttonMask] — NSEventButtonMask
    public long buttonMask() { return ObjC.msgSendLong(peer, ObjC.sel("buttonMask")); }

    /// [event tilt] — NSPoint {x,y} tilt
    public NSPoint tilt() {
        try {
            MemorySegment seg = (MemorySegment) handles.hLocation().invokeExact(ObjC.structSlot(), peer, ObjC.sel("tilt"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("tilt failed", t); }
    }

    /// [event tangentialPressure]
    public double tangentialPressure() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("tangentialPressure")); } catch (Throwable t) { throw new RuntimeException("tangentialPressure failed", t); }
    }

    /// [event stage] — pressure stage
    public long stage() { return ObjC.msgSendLong(peer, ObjC.sel("stage")); }

    /// [event stageTransition]
    public double stageTransition() {
        try { return (double) handles.hDouble().invokeExact(peer, ObjC.sel("stageTransition")); } catch (Throwable t) { throw new RuntimeException("stageTransition failed", t); }
    }

    /// [event associatedEventsMask]
    public long associatedEventsMask() { return ObjC.msgSendLong(peer, ObjC.sel("associatedEventsMask")); }

    /// [event window] — NSWindow peer or null.
    public MemorySegment window() {
        MemorySegment w = ObjC.msgSendId(peer, ObjC.sel("window"));
        return (w == null || w.address() == 0) ? null : w;
    }

    /// [event charactersByApplyingModifiers:] — key events only, guarded.
    public String charactersByApplyingModifiers(long modifiers) {
        requireKeyEvent("charactersByApplyingModifiers:");
        try {
            MemorySegment s = (MemorySegment) handles.hCharsByModifiers().invokeExact(peer, ObjC.sel("charactersByApplyingModifiers:"), modifiers);
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("charactersByApplyingModifiers: failed", t); }
    }

    /// [NSEvent mouseLocation] — class property NSPoint
    public static NSPoint mouseLocation() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hLocation().invokeExact(ObjC.structSlot(), ObjC.cls("NSEvent"), ObjC.sel("mouseLocation"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("mouseLocation failed", t); }
    }

    /// [NSEvent modifierFlags] — class property
    public static long modifierFlagsStatic() {
        return ObjC.msgSendLong(ObjC.cls("NSEvent"), ObjC.sel("modifierFlags"));
    }

    /// [NSEvent pressedMouseButtons]
    public static long pressedMouseButtons() {
        return ObjC.msgSendLong(ObjC.cls("NSEvent"), ObjC.sel("pressedMouseButtons"));
    }

    /// [NSEvent doubleClickInterval]
    public static double doubleClickInterval() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(ObjC.cls("NSEvent"), ObjC.sel("doubleClickInterval")); } catch (Throwable t) { throw new RuntimeException("doubleClickInterval failed", t); }
    }

    /// [NSEvent keyRepeatDelay]
    public static double keyRepeatDelay() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(ObjC.cls("NSEvent"), ObjC.sel("keyRepeatDelay")); } catch (Throwable t) { throw new RuntimeException("keyRepeatDelay failed", t); }
    }

    /// [NSEvent keyRepeatInterval]
    public static double keyRepeatInterval() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(ObjC.cls("NSEvent"), ObjC.sel("keyRepeatInterval")); } catch (Throwable t) { throw new RuntimeException("keyRepeatInterval failed", t); }
    }

    /// Wrap an existing NSEvent peer (nil-safe). The package-private
    /// constructor stays as-is because NSApplication/NSView/NSWindow
    /// construct events from the same package; this entry point serves
    /// callers in other packages (e.g. tests wrapping eventWithCGEvent:).
    public static NSEvent wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSEvent(peer);
    }

    // ---- tablet / pointing-device identity (all plain scalar shapes) ----

    /// [event vendorDefined] — vendor-specific data object, or null.
    public MemorySegment vendorDefined() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("vendorDefined"));
        return (v == null || v.address() == 0) ? null : v;
    }

    /// [event vendorID] — NSUInteger vendor identifier.
    public long vendorID() { return ObjC.msgSendLong(peer, ObjC.sel("vendorID")); }

    /// [event tabletID] — NSUInteger.
    public long tabletID() { return ObjC.msgSendLong(peer, ObjC.sel("tabletID")); }

    /// [event pointingDeviceID] — NSUInteger.
    public long pointingDeviceID() { return ObjC.msgSendLong(peer, ObjC.sel("pointingDeviceID")); }

    /// [event systemTabletID] — NSUInteger.
    public long systemTabletID() { return ObjC.msgSendLong(peer, ObjC.sel("systemTabletID")); }

    /// [event vendorPointingDeviceType] — NSUInteger.
    public long vendorPointingDeviceType() { return ObjC.msgSendLong(peer, ObjC.sel("vendorPointingDeviceType")); }

    /// [event pointingDeviceSerialNumber] — NSUInteger.
    public long pointingDeviceSerialNumber() { return ObjC.msgSendLong(peer, ObjC.sel("pointingDeviceSerialNumber")); }

    /// [event uniqueID] — unsigned long long; fits the INT (C long) shape.
    public long uniqueID() { return ObjC.msgSendLong(peer, ObjC.sel("uniqueID")); }

    /// [event capabilityMask] — NSUInteger.
    public long capabilityMask() { return ObjC.msgSendLong(peer, ObjC.sel("capabilityMask")); }

    /// [event pointingDeviceType] — NSPointingDeviceType (NSInteger).
    public long pointingDeviceType() { return ObjC.msgSendLong(peer, ObjC.sel("pointingDeviceType")); }

    /// [event isEnteringProximity] — proximity enter (vs exit).
    public boolean isEnteringProximity() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEnteringProximity"));
    }

    /// [event pressureBehavior] — NSPressureBehavior (NSInteger).
    public long pressureBehavior() { return ObjC.msgSendLong(peer, ObjC.sel("pressureBehavior")); }

    /// [event trackingArea] — the area the event belongs to, or null.
    public NSTrackingArea trackingArea() {
        return NSTrackingArea.wrap(ObjC.msgSendId(peer, ObjC.sel("trackingArea")));
    }

    // ---- raw backing-store pointers (returned as segments, null for nil) ----

    /// [event CGEvent] — the underlying CGEventRef, or null.
    public MemorySegment cgEvent() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("CGEvent"));
        return (c == null || c.address() == 0) ? null : c;
    }

    /// [event eventRef] — the underlying Carbon EventRef, or null.
    public MemorySegment eventRef() {
        MemorySegment r = ObjC.msgSendId(peer, ObjC.sel("eventRef"));
        return (r == null || r.address() == 0) ? null : r;
    }

    /// [event userData] — tracking-rect user data pointer, or null.
    public MemorySegment userData() {
        MemorySegment u = ObjC.msgSendId(peer, ObjC.sel("userData"));
        return (u == null || u.address() == 0) ? null : u;
    }

    // ---- touch readers (object shapes only; no new vocabulary) ----

    /// [event allTouches] — NSSet of NSTouch, possibly empty.
    public NSSet allTouches() {
        return NSSet.wrap(ObjC.msgSendId(peer, ObjC.sel("allTouches")));
    }

    /// [event touchesForView:] — touches for a view (null view allowed).
    public NSSet touchesForView(NSView view) {
        return NSSet.wrap(ObjC.msgSendIdId(peer, ObjC.sel("touchesForView:"),
                view == null ? MemorySegment.NULL : view.peer()));
    }

    /// [event touchesMatchingPhase:inView:].
    public NSSet touchesMatchingPhaseInView(long phase, NSView view) {
        try {
            MemorySegment s = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Sig.Arg.INT, Sig.Arg.ID))
                    .invokeExact(peer, ObjC.sel("touchesMatchingPhase:inView:"), phase,
                            (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
            return NSSet.wrap(s);
        } catch (Throwable t) { throw new RuntimeException("touchesMatchingPhase:inView: failed", t); }
    }

    /// [event coalescedTouchesForTouch:] — coalesced touches for one touch.
    public NSSet coalescedTouchesForTouch(MemorySegment touch) {
        try {
            MemorySegment s = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Sig.Arg.ID))
                    .invokeExact(peer, ObjC.sel("coalescedTouchesForTouch:"),
                            (MemorySegment) (touch == null ? MemorySegment.NULL : touch));
            return NSSet.wrap(s);
        } catch (Throwable t) { throw new RuntimeException("coalescedTouchesForTouch: failed", t); }
    }

    // ---- factories that fit the (id)->id shape ----

    /// [NSEvent eventWithCGEvent:] — wrap a CGEventRef (raw pointer segment).
    ///
    /// NULL is fatal by AppKit contract (`eventWithCGEvent: was called with
    /// a NULL CGEventRef` raises an uncatchable NSException), so unlike the
    /// peer wrappers this factory rejects null/NULL up front.
    public static NSEvent eventWithCGEvent(MemorySegment cgEvent) {
        if (cgEvent == null || cgEvent.address() == 0) {
            throw new IllegalArgumentException("eventWithCGEvent: requires a non-null CGEventRef");
        }
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSEvent"), ObjC.sel("eventWithCGEvent:"), cgEvent));
    }

    /// [NSEvent eventWithEventRef:] — wrap a Carbon EventRef pointer.
    /// Same NULL contract as eventWithCGEvent: (AppKit raises on NULL).
    public static NSEvent eventWithEventRef(MemorySegment eventRef) {
        if (eventRef == null || eventRef.address() == 0) {
            throw new IllegalArgumentException("eventWithEventRef: requires a non-null EventRef");
        }
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSEvent"), ObjC.sel("eventWithEventRef:"), eventRef));
    }

    // ---- mouse-coalescing / swipe / periodic class surface ----

    /// [NSEvent isMouseCoalescingEnabled] class property.
    public static boolean isMouseCoalescingEnabled() {
        return ObjC.msgSendBool(ObjC.cls("NSEvent"), ObjC.sel("isMouseCoalescingEnabled"));
    }

    /// [NSEvent setMouseCoalescingEnabled:].
    public static void setMouseCoalescingEnabled(boolean flag) {
        ObjC.msgSendVoidBool(ObjC.cls("NSEvent"), ObjC.sel("setMouseCoalescingEnabled:"), flag);
    }

    /// [NSEvent isSwipeTrackingFromScrollEventsEnabled] class property.
    public static boolean isSwipeTrackingFromScrollEventsEnabled() {
        return ObjC.msgSendBool(ObjC.cls("NSEvent"), ObjC.sel("isSwipeTrackingFromScrollEventsEnabled"));
    }

    /// [NSEvent startPeriodicEventsAfterDelay:withPeriod:].
    public static void startPeriodicEventsAfterDelay(double delay, double period) {
        try {
            ObjC.handle(Sig.of(Ret.VOID, Sig.Arg.DOUBLE, Sig.Arg.DOUBLE))
                    .invokeExact(ObjC.cls("NSEvent"), ObjC.sel("startPeriodicEventsAfterDelay:withPeriod:"), delay, period);
        } catch (Throwable t) { throw new RuntimeException("startPeriodicEventsAfterDelay:withPeriod: failed", t); }
    }

    /// [NSEvent stopPeriodicEvents].
    public static void stopPeriodicEvents() {
        ObjC.msgSendVoid(ObjC.cls("NSEvent"), ObjC.sel("stopPeriodicEvents"));
    }

    /// [NSEvent removeMonitor:] — remove a monitor installed by one of the
    /// addGlobal/addLocalMonitor… factories (untestable here: installing a
    /// monitor needs a block, so there is no live monitor to remove; the
    /// wrapper exists so callers holding one can remove it).
    public static void removeMonitor(MemorySegment eventMonitor) {
        if (eventMonitor == null || eventMonitor.address() == 0) return;
        ObjC.msgSendVoidId(ObjC.cls("NSEvent"), ObjC.sel("removeMonitor:"), eventMonitor);
    }
}
