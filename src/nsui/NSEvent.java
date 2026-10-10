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

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment type;
        static MemorySegment characters;
        static MemorySegment locationInWindow;
        static MemorySegment modifierFlags;
        static MemorySegment keyCode;
        static MemorySegment buttonNumber;
        static MemorySegment clickCount;
        static MemorySegment timestamp;
        static MemorySegment windowNumber;
        static MemorySegment charactersIgnoringModifiers;
        static MemorySegment isARepeat;
        static MemorySegment subtype;
        static MemorySegment eventNumber;
        static MemorySegment data1;
        static MemorySegment data2;
        static MemorySegment pressure;
        static MemorySegment deltaX;
        static MemorySegment deltaY;
        static MemorySegment deltaZ;
        static MemorySegment scrollingDeltaX;
        static MemorySegment scrollingDeltaY;
        static MemorySegment hasPreciseScrollingDeltas;
        static MemorySegment momentumPhase;
        static MemorySegment phase;
        static MemorySegment isDirectionInvertedFromDevice;
        static MemorySegment trackingNumber;
        static MemorySegment magnification;
        static MemorySegment deviceID;
        static MemorySegment rotation;
        static MemorySegment absoluteX;
        static MemorySegment absoluteY;
        static MemorySegment absoluteZ;
        static MemorySegment buttonMask;
        static MemorySegment tilt;
        static MemorySegment tangentialPressure;
        static MemorySegment stage;
        static MemorySegment stageTransition;
        static MemorySegment associatedEventsMask;
        static MemorySegment window;
        static MemorySegment charactersByApplyingModifiers;
        static MemorySegment mouseLocation;
        static MemorySegment pressedMouseButtons;
        static MemorySegment doubleClickInterval;
        static MemorySegment keyRepeatDelay;
        static MemorySegment keyRepeatInterval;
        static MemorySegment vendorDefined;
        static MemorySegment vendorID;
        static MemorySegment tabletID;
        static MemorySegment pointingDeviceID;
        static MemorySegment systemTabletID;
        static MemorySegment vendorPointingDeviceType;
        static MemorySegment pointingDeviceSerialNumber;
        static MemorySegment uniqueID;
        static MemorySegment capabilityMask;
        static MemorySegment pointingDeviceType;
        static MemorySegment isEnteringProximity;
        static MemorySegment pressureBehavior;
        static MemorySegment trackingArea;
        static MemorySegment CGEvent;
        static MemorySegment eventRef;
        static MemorySegment userData;
        static MemorySegment allTouches;
        static MemorySegment touchesForView;
        static MemorySegment touchesMatchingPhase_inView;
        static MemorySegment coalescedTouchesForTouch;
        static MemorySegment eventWithCGEvent;
        static MemorySegment eventWithEventRef;
        static MemorySegment isMouseCoalescingEnabled;
        static MemorySegment setMouseCoalescingEnabled;
        static MemorySegment isSwipeTrackingFromScrollEventsEnabled;
        static MemorySegment startPeriodicEventsAfterDelay_withPeriod;
        static MemorySegment stopPeriodicEvents;
        static MemorySegment removeMonitor;
        static void populate() {
            type = ObjC.sel("type");
            characters = ObjC.sel("characters");
            locationInWindow = ObjC.sel("locationInWindow");
            modifierFlags = ObjC.sel("modifierFlags");
            keyCode = ObjC.sel("keyCode");
            buttonNumber = ObjC.sel("buttonNumber");
            clickCount = ObjC.sel("clickCount");
            timestamp = ObjC.sel("timestamp");
            windowNumber = ObjC.sel("windowNumber");
            charactersIgnoringModifiers = ObjC.sel("charactersIgnoringModifiers");
            isARepeat = ObjC.sel("isARepeat");
            subtype = ObjC.sel("subtype");
            eventNumber = ObjC.sel("eventNumber");
            data1 = ObjC.sel("data1");
            data2 = ObjC.sel("data2");
            pressure = ObjC.sel("pressure");
            deltaX = ObjC.sel("deltaX");
            deltaY = ObjC.sel("deltaY");
            deltaZ = ObjC.sel("deltaZ");
            scrollingDeltaX = ObjC.sel("scrollingDeltaX");
            scrollingDeltaY = ObjC.sel("scrollingDeltaY");
            hasPreciseScrollingDeltas = ObjC.sel("hasPreciseScrollingDeltas");
            momentumPhase = ObjC.sel("momentumPhase");
            phase = ObjC.sel("phase");
            isDirectionInvertedFromDevice = ObjC.sel("isDirectionInvertedFromDevice");
            trackingNumber = ObjC.sel("trackingNumber");
            magnification = ObjC.sel("magnification");
            deviceID = ObjC.sel("deviceID");
            rotation = ObjC.sel("rotation");
            absoluteX = ObjC.sel("absoluteX");
            absoluteY = ObjC.sel("absoluteY");
            absoluteZ = ObjC.sel("absoluteZ");
            buttonMask = ObjC.sel("buttonMask");
            tilt = ObjC.sel("tilt");
            tangentialPressure = ObjC.sel("tangentialPressure");
            stage = ObjC.sel("stage");
            stageTransition = ObjC.sel("stageTransition");
            associatedEventsMask = ObjC.sel("associatedEventsMask");
            window = ObjC.sel("window");
            charactersByApplyingModifiers = ObjC.sel("charactersByApplyingModifiers:");
            mouseLocation = ObjC.sel("mouseLocation");
            pressedMouseButtons = ObjC.sel("pressedMouseButtons");
            doubleClickInterval = ObjC.sel("doubleClickInterval");
            keyRepeatDelay = ObjC.sel("keyRepeatDelay");
            keyRepeatInterval = ObjC.sel("keyRepeatInterval");
            vendorDefined = ObjC.sel("vendorDefined");
            vendorID = ObjC.sel("vendorID");
            tabletID = ObjC.sel("tabletID");
            pointingDeviceID = ObjC.sel("pointingDeviceID");
            systemTabletID = ObjC.sel("systemTabletID");
            vendorPointingDeviceType = ObjC.sel("vendorPointingDeviceType");
            pointingDeviceSerialNumber = ObjC.sel("pointingDeviceSerialNumber");
            uniqueID = ObjC.sel("uniqueID");
            capabilityMask = ObjC.sel("capabilityMask");
            pointingDeviceType = ObjC.sel("pointingDeviceType");
            isEnteringProximity = ObjC.sel("isEnteringProximity");
            pressureBehavior = ObjC.sel("pressureBehavior");
            trackingArea = ObjC.sel("trackingArea");
            CGEvent = ObjC.sel("CGEvent");
            eventRef = ObjC.sel("eventRef");
            userData = ObjC.sel("userData");
            allTouches = ObjC.sel("allTouches");
            touchesForView = ObjC.sel("touchesForView:");
            touchesMatchingPhase_inView = ObjC.sel("touchesMatchingPhase:inView:");
            coalescedTouchesForTouch = ObjC.sel("coalescedTouchesForTouch:");
            eventWithCGEvent = ObjC.sel("eventWithCGEvent:");
            eventWithEventRef = ObjC.sel("eventWithEventRef:");
            isMouseCoalescingEnabled = ObjC.sel("isMouseCoalescingEnabled");
            setMouseCoalescingEnabled = ObjC.sel("setMouseCoalescingEnabled:");
            isSwipeTrackingFromScrollEventsEnabled = ObjC.sel("isSwipeTrackingFromScrollEventsEnabled");
            startPeriodicEventsAfterDelay_withPeriod = ObjC.sel("startPeriodicEventsAfterDelay:withPeriod:");
            stopPeriodicEvents = ObjC.sel("stopPeriodicEvents");
            removeMonitor = ObjC.sel("removeMonitor:");
        }
    }

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hLocation, MethodHandle hDouble, MethodHandle hBool, MethodHandle hCharsByModifiers, MethodHandle hFloat) {}
    private static volatile Handles handles;

    NSEvent(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Sig.Ret.ID, Sig.Arg.INT)),
                ObjC.handle(Sig.of(Ret.FLOAT))
        );
            Sels.populate();
        handles = h;
}

    /// NSEventType (NSUInteger). 1=leftMouseDown 2=leftMouseUp 10=keyDown 11=keyUp.
    public long type() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.type);
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
        ensureInit();
        requireKeyEvent("characters");
        return ObjC.toString(ObjC.msgSendId(peer, Sels.characters));
    }

    /// [event locationInWindow] — mouse location in the window's base coordinate
    /// system (origin bottom-left). POINT is a GROUP return, so the downcall handle
    /// carries an implicit leading SegmentAllocator for the struct it returns.
    public NSPoint locationInWindow() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hLocation().invokeExact(ObjC.structSlot(), peer, Sels.locationInWindow);
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("locationInWindow failed", t);
        }
    }

    /// [event modifierFlags] — a bitmask (of NSEventModifierFlags, NSUInteger).
    public long modifierFlags() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.modifierFlags);
    }

    /// [event keyCode] — hardware keyboard code (CGKeyCode, unsigned short).
    ///
    /// **Key events only.** Guarded.
    public long keyCode() {
        ensureInit();
        requireKeyEvent("keyCode");
        return ((short) ObjC.msgSendShort(peer, Sels.keyCode)) & 0xFFFFL;
    }

    /// [event buttonNumber] — which mouse button generated the event.
    public long buttonNumber() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.buttonNumber);
    }

    /// [event clickCount] — how many clicks this event represents.
    public long clickCount() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.clickCount);
    }

    /// [event timestamp] — system time of the event in seconds.
    public double timestamp() {
        ensureInit();
        try {
            return (double) handles.hDouble().invokeExact(peer, Sels.timestamp);
        } catch (Throwable t) {
            throw new RuntimeException("timestamp failed", t);
        }
    }

    /// [event windowNumber] — the window the event is associated with (0 if none).
    public long windowNumber() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.windowNumber);
    }

    /// Characters of a KEY event ignoring the current modifier layout (NSString -> String).
    ///
    /// **Key events only.** Guarded.
    public String charactersIgnoringModifiers() {
        ensureInit();
        requireKeyEvent("charactersIgnoringModifiers");
        return ObjC.toString(ObjC.msgSendId(peer, Sels.charactersIgnoringModifiers));
    }

    /// [event isARepeat] — true if key is auto-repeat. Key events only — guarded.
    public boolean isARepeat() {
        ensureInit();
        requireKeyEvent("isARepeat");
        try { return (boolean) handles.hBool().invokeExact(peer, Sels.isARepeat); } catch (Throwable t) { throw new RuntimeException("isARepeat failed", t); }
    }

    // ---- additional accessors (80% completeness) ----

    /// [event subtype] — NSEventSubtype (short).
    public long subtype() {
ensureInit(); return (long) ObjC.msgSendShort(peer, Sels.subtype); }

    /// [event eventNumber]
    public long eventNumber() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.eventNumber); }

    /// [event data1]
    public long data1() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.data1); }

    /// [event data2]
    public long data2() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.data2); }

    /// [event pressure] — declared `float` in AppKit; the DOUBLE handle read the
    /// wrong register. FLOAT handle, widened to double for the Java API.
    public double pressure() {
        ensureInit();
        try { return (double) (float) handles.hFloat().invokeExact(peer, Sels.pressure); } catch (Throwable t) { throw new RuntimeException("pressure failed", t); }
    }

    /// [event deltaX]
    public double deltaX() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(peer, Sels.deltaX); } catch (Throwable t) { throw new RuntimeException("deltaX failed", t); }
    }

    /// [event deltaY]
    public double deltaY() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(peer, Sels.deltaY); } catch (Throwable t) { throw new RuntimeException("deltaY failed", t); }
    }

    /// [event deltaZ]
    public double deltaZ() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(peer, Sels.deltaZ); } catch (Throwable t) { throw new RuntimeException("deltaZ failed", t); }
    }

    /// [event scrollingDeltaX]
    public double scrollingDeltaX() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(peer, Sels.scrollingDeltaX); } catch (Throwable t) { throw new RuntimeException("scrollingDeltaX failed", t); }
    }

    /// [event scrollingDeltaY]
    public double scrollingDeltaY() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(peer, Sels.scrollingDeltaY); } catch (Throwable t) { throw new RuntimeException("scrollingDeltaY failed", t); }
    }

    /// [event hasPreciseScrollingDeltas]
    public boolean hasPreciseScrollingDeltas() {
        ensureInit();
        try { return (boolean) handles.hBool().invokeExact(peer, Sels.hasPreciseScrollingDeltas); } catch (Throwable t) { throw new RuntimeException("hasPreciseScrollingDeltas failed", t); }
    }

    /// [event momentumPhase]
    public long momentumPhase() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.momentumPhase); }

    /// [event phase]
    public long phase() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.phase); }

    /// [event isDirectionInvertedFromDevice]
    public boolean isDirectionInvertedFromDevice() {
        ensureInit();
        try { return (boolean) handles.hBool().invokeExact(peer, Sels.isDirectionInvertedFromDevice); } catch (Throwable t) { throw new RuntimeException("isDirectionInvertedFromDevice failed", t); }
    }

    /// [event trackingNumber]
    public long trackingNumber() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.trackingNumber); }

    /// [event magnification]
    public double magnification() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(peer, Sels.magnification); } catch (Throwable t) { throw new RuntimeException("magnification failed", t); }
    }

    /// [event deviceID] — NSUInteger
    public long deviceID() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.deviceID); }

    /// [event rotation] — float degrees (FLOAT handle)
    public double rotation() {
        ensureInit();
        try { return (double) (float) handles.hFloat().invokeExact(peer, Sels.rotation); } catch (Throwable t) { throw new RuntimeException("rotation failed", t); }
    }

    /// [event absoluteX]
    public long absoluteX() {
    ensureInit(); return ObjC.msgSendLong(peer, Sels.absoluteX); }
    /// [event absoluteY]
    public long absoluteY() {
    ensureInit(); return ObjC.msgSendLong(peer, Sels.absoluteY); }
    /// [event absoluteZ]
    public long absoluteZ() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.absoluteZ); }

    /// [event buttonMask] — NSEventButtonMask
    public long buttonMask() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.buttonMask); }

    /// [event tilt] — NSPoint {x,y} tilt
    public NSPoint tilt() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hLocation().invokeExact(ObjC.structSlot(), peer, Sels.tilt);
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("tilt failed", t); }
    }

    /// [event tangentialPressure] — float (FLOAT handle)
    public double tangentialPressure() {
        ensureInit();
        try { return (double) (float) handles.hFloat().invokeExact(peer, Sels.tangentialPressure); } catch (Throwable t) { throw new RuntimeException("tangentialPressure failed", t); }
    }

    /// [event stage] — pressure stage
    public long stage() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.stage); }

    /// [event stageTransition]
    public double stageTransition() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(peer, Sels.stageTransition); } catch (Throwable t) { throw new RuntimeException("stageTransition failed", t); }
    }

    /// [event associatedEventsMask]
    public long associatedEventsMask() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.associatedEventsMask); }

    /// [event window] — NSWindow peer or null.
    public MemorySegment window() {
        ensureInit();
        MemorySegment w = ObjC.msgSendId(peer, Sels.window);
        return (w == null || w.address() == 0) ? null : w;
    }

    /// [event charactersByApplyingModifiers:] — key events only, guarded.
    public String charactersByApplyingModifiers(long modifiers) {
        ensureInit();
        requireKeyEvent("charactersByApplyingModifiers:");
        try {
            MemorySegment s = (MemorySegment) handles.hCharsByModifiers().invokeExact(peer, Sels.charactersByApplyingModifiers, modifiers);
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("charactersByApplyingModifiers: failed", t); }
    }

    /// [NSEvent mouseLocation] — class property NSPoint
    public static NSPoint mouseLocation() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hLocation().invokeExact(ObjC.structSlot(), ObjC.cls("NSEvent"), Sels.mouseLocation);
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("mouseLocation failed", t); }
    }

    /// [NSEvent modifierFlags] — class property
    public static long modifierFlagsStatic() {
        ensureInit();
        return ObjC.msgSendLong(ObjC.cls("NSEvent"), Sels.modifierFlags);
    }

    /// [NSEvent pressedMouseButtons]
    public static long pressedMouseButtons() {
        ensureInit();
        return ObjC.msgSendLong(ObjC.cls("NSEvent"), Sels.pressedMouseButtons);
    }

    /// [NSEvent doubleClickInterval]
    public static double doubleClickInterval() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(ObjC.cls("NSEvent"), Sels.doubleClickInterval); } catch (Throwable t) { throw new RuntimeException("doubleClickInterval failed", t); }
    }

    /// [NSEvent keyRepeatDelay]
    public static double keyRepeatDelay() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(ObjC.cls("NSEvent"), Sels.keyRepeatDelay); } catch (Throwable t) { throw new RuntimeException("keyRepeatDelay failed", t); }
    }

    /// [NSEvent keyRepeatInterval]
    public static double keyRepeatInterval() {
        ensureInit();
        try { return (double) handles.hDouble().invokeExact(ObjC.cls("NSEvent"), Sels.keyRepeatInterval); } catch (Throwable t) { throw new RuntimeException("keyRepeatInterval failed", t); }
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
        ensureInit();
        MemorySegment v = ObjC.msgSendId(peer, Sels.vendorDefined);
        return (v == null || v.address() == 0) ? null : v;
    }

    /// [event vendorID] — NSUInteger vendor identifier.
    public long vendorID() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.vendorID); }

    /// [event tabletID] — NSUInteger.
    public long tabletID() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.tabletID); }

    /// [event pointingDeviceID] — NSUInteger.
    public long pointingDeviceID() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.pointingDeviceID); }

    /// [event systemTabletID] — NSUInteger.
    public long systemTabletID() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.systemTabletID); }

    /// [event vendorPointingDeviceType] — NSUInteger.
    public long vendorPointingDeviceType() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.vendorPointingDeviceType); }

    /// [event pointingDeviceSerialNumber] — NSUInteger.
    public long pointingDeviceSerialNumber() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.pointingDeviceSerialNumber); }

    /// [event uniqueID] — unsigned long long; fits the INT (C long) shape.
    public long uniqueID() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.uniqueID); }

    /// [event capabilityMask] — NSUInteger.
    public long capabilityMask() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.capabilityMask); }

    /// [event pointingDeviceType] — NSPointingDeviceType (NSInteger).
    public long pointingDeviceType() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.pointingDeviceType); }

    /// [event isEnteringProximity] — proximity enter (vs exit).
    public boolean isEnteringProximity() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isEnteringProximity);
    }

    /// [event pressureBehavior] — NSPressureBehavior (NSInteger).
    public long pressureBehavior() {
ensureInit(); return ObjC.msgSendLong(peer, Sels.pressureBehavior); }

    /// [event trackingArea] — the area the event belongs to, or null.
    public NSTrackingArea trackingArea() {
        ensureInit();
        return NSTrackingArea.wrap(ObjC.msgSendId(peer, Sels.trackingArea));
    }

    // ---- raw backing-store pointers (returned as segments, null for nil) ----

    /// [event CGEvent] — the underlying CGEventRef, or null.
    public MemorySegment cgEvent() {
        ensureInit();
        MemorySegment c = ObjC.msgSendId(peer, Sels.CGEvent);
        return (c == null || c.address() == 0) ? null : c;
    }

    /// [event eventRef] — the underlying Carbon EventRef, or null.
    public MemorySegment eventRef() {
        ensureInit();
        MemorySegment r = ObjC.msgSendId(peer, Sels.eventRef);
        return (r == null || r.address() == 0) ? null : r;
    }

    /// [event userData] — tracking-rect user data pointer, or null.
    public MemorySegment userData() {
        ensureInit();
        MemorySegment u = ObjC.msgSendId(peer, Sels.userData);
        return (u == null || u.address() == 0) ? null : u;
    }

    // ---- touch readers (object shapes only; no new vocabulary) ----

    /// [event allTouches] — NSSet of NSTouch, possibly empty.
    public NSSet allTouches() {
        ensureInit();
        return NSSet.wrap(ObjC.msgSendId(peer, Sels.allTouches));
    }

    /// [event touchesForView:] — touches for a view (null view allowed).
    public NSSet touchesForView(NSView view) {
        ensureInit();
        return NSSet.wrap(ObjC.msgSendIdId(peer, Sels.touchesForView,
                view == null ? MemorySegment.NULL : view.peer()));
    }

    /// [event touchesMatchingPhase:inView:].
    public NSSet touchesMatchingPhaseInView(long phase, NSView view) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Sig.Arg.INT, Sig.Arg.ID))
                    .invokeExact(peer, Sels.touchesMatchingPhase_inView, phase,
                            (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
            return NSSet.wrap(s);
        } catch (Throwable t) { throw new RuntimeException("touchesMatchingPhase:inView: failed", t); }
    }

    /// [event coalescedTouchesForTouch:] — coalesced touches for one touch.
    public NSSet coalescedTouchesForTouch(MemorySegment touch) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Sig.Arg.ID))
                    .invokeExact(peer, Sels.coalescedTouchesForTouch,
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
        ensureInit();
        if (cgEvent == null || cgEvent.address() == 0) {
            throw new IllegalArgumentException("eventWithCGEvent: requires a non-null CGEventRef");
        }
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSEvent"), Sels.eventWithCGEvent, cgEvent));
    }

    /// [NSEvent eventWithEventRef:] — wrap a Carbon EventRef pointer.
    /// Same NULL contract as eventWithCGEvent: (AppKit raises on NULL).
    public static NSEvent eventWithEventRef(MemorySegment eventRef) {
        ensureInit();
        if (eventRef == null || eventRef.address() == 0) {
            throw new IllegalArgumentException("eventWithEventRef: requires a non-null EventRef");
        }
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSEvent"), Sels.eventWithEventRef, eventRef));
    }

    // ---- mouse-coalescing / swipe / periodic class surface ----

    /// [NSEvent isMouseCoalescingEnabled] class property.
    public static boolean isMouseCoalescingEnabled() {
        ensureInit();
        return ObjC.msgSendBool(ObjC.cls("NSEvent"), Sels.isMouseCoalescingEnabled);
    }

    /// [NSEvent setMouseCoalescingEnabled:].
    public static void setMouseCoalescingEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(ObjC.cls("NSEvent"), Sels.setMouseCoalescingEnabled, flag);
    }

    /// [NSEvent isSwipeTrackingFromScrollEventsEnabled] class property.
    public static boolean isSwipeTrackingFromScrollEventsEnabled() {
        ensureInit();
        return ObjC.msgSendBool(ObjC.cls("NSEvent"), Sels.isSwipeTrackingFromScrollEventsEnabled);
    }

    /// [NSEvent startPeriodicEventsAfterDelay:withPeriod:].
    public static void startPeriodicEventsAfterDelay(double delay, double period) {
        ensureInit();
        try {
            ObjC.handle(Sig.of(Ret.VOID, Sig.Arg.DOUBLE, Sig.Arg.DOUBLE))
                    .invokeExact(ObjC.cls("NSEvent"), Sels.startPeriodicEventsAfterDelay_withPeriod, delay, period);
        } catch (Throwable t) { throw new RuntimeException("startPeriodicEventsAfterDelay:withPeriod: failed", t); }
    }

    /// [NSEvent stopPeriodicEvents].
    public static void stopPeriodicEvents() {
        ensureInit();
        ObjC.msgSendVoid(ObjC.cls("NSEvent"), Sels.stopPeriodicEvents);
    }

    /// [NSEvent removeMonitor:] — remove a monitor installed by one of the
    /// addGlobal/addLocalMonitor… factories (untestable here: installing a
    /// monitor needs a block, so there is no live monitor to remove; the
    /// wrapper exists so callers holding one can remove it).
    public static void removeMonitor(MemorySegment eventMonitor) {
        ensureInit();
        if (eventMonitor == null || eventMonitor.address() == 0) return;
        ObjC.msgSendVoidId(ObjC.cls("NSEvent"), Sels.removeMonitor, eventMonitor);
    }
}
