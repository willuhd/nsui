package nsui.tests;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nsui.NSApplication;
import nsui.NSArray;
import nsui.NSClickGestureRecognizer;
import nsui.NSData;
import nsui.NSDraggingDestination;
import nsui.NSDraggingItem;
import nsui.NSDraggingSession;
import nsui.NSDraggingSource;
import nsui.NSEvent;
import nsui.NSFilePromiseProvider;
import nsui.NSGestureRecognizer;
import nsui.NSMagnificationGestureRecognizer;
import nsui.NSMenu;
import nsui.NSObject;
import nsui.NSPanGestureRecognizer;
import nsui.NSPasteboard;
import nsui.NSPasteboardItem;
import nsui.NSPoint;
import nsui.NSPressGestureRecognizer;
import nsui.NSRect;
import nsui.NSResponder;
import nsui.NSRotationGestureRecognizer;
import nsui.NSTrackingArea;
import nsui.NSView;
import nsui.NSWindow;
import nsui.objc.DelegateProxy;
import nsui.objc.NsuiForeign;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// EventCoverageTest — coverage for the Events/responder batch.
///
/// Exercises the newly wrapped surface with hidden windows only (never
/// makeKeyAndOrderFront / activateIgnoringOtherApps, no synthesized
/// window-server clicks, no audio): NSEvent class properties + a synthetic
/// CG-derived mouse event for the instance accessors, the NSResponder sink
/// family with nil events on a standalone view, the base-recognizer
/// delays/touch/prevention surface, property round-trips for the four newer
/// recognizers (magnification, press, rotation incl. rotationInDegrees, and
/// click numberOfTouchesRequired), unique-name pasteboard + item triples,
/// dragging item frames, destination/source delegates, file promises and
/// tracking areas.
public final class EventCoverageTest {

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    private static MemorySegment dummyTarget() {
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSObject"), ObjC.sel("alloc"));
        return ObjC.msgSendId(alloc, ObjC.sel("init"));
    }

    private static boolean responds(MemorySegment receiver, String selector) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(receiver, ObjC.sel("respondsToSelector:"), ObjC.sel(selector));
        } catch (Throwable t) {
            return false;
        }
    }

    /// No-op sink for the block-taking file-promise write selector. Never
    /// invoked: the test triggers no promise write (no drag/drop), it exists
    /// only so the promise delegate responds to the required selector.
    private static void promiseWriteNoop(MemorySegment self, MemorySegment cmd,
            MemorySegment provider, MemorySegment url, MemorySegment block) {
    }

    /// Minimal file-promise delegate: fileNameForType: via the DelegateProxy
    /// IdIdArg shape plus a hand-rolled no-op upcall stub for the
    /// block-taking writePromiseToURL:completionHandler: (no DelegateProxy
    /// shape takes a block). Returned peer backs NSFilePromiseProvider.create.
    private static MemorySegment promiseDelegate() throws Throwable {
        Map<String, DelegateProxy.IdIdArg> names = new HashMap<>();
        names.put("filePromiseProvider:fileNameForType:",
                (provider, type) -> ObjC.nsstring("promise.txt"));
        MemorySegment del = DelegateProxy.delegate("NSObject", "NSUIFilePromiseDelegate",
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), names, Map.of());
        MethodHandle target = MethodHandles.lookup().findStatic(EventCoverageTest.class,
                "promiseWriteNoop", MethodType.methodType(void.class,
                        MemorySegment.class, MemorySegment.class, MemorySegment.class,
                        MemorySegment.class, MemorySegment.class));
        MemorySegment stub = ObjC.upcall(target, FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        boolean added = ObjC.addMethod(ObjC.cls("NSUIFilePromiseDelegate"),
                "filePromiseProvider:writePromiseToURL:completionHandler:", stub, "v@:@@?");
        TestKit.check(added, "promise delegate write selector installed");
        return del;
    }

    private static void noThrow(String msg, Runnable body) {
        try {
            body.run();
            TestKit.check(true, msg);
        } catch (Throwable t) {
            TestKit.check(false, msg + " threw: " + t);
        }
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== EventCoverageTest — events/responder batch ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            String m = String.valueOf(t.getMessage()).toLowerCase();
            if (m.contains("connection") || m.contains("dlopen") || m.contains("appkit")) {
                TestKit.skip("ObjC.init failed (not macOS / connection error): " + t);
            }
            throw t;
        }
        try {
            NSApplication.shared();
        } catch (Throwable t) {
            TestKit.skip("NSApplication unavailable (headless session?): " + t);
        }

        // ---- nil-wrap contracts (every wrapper in the batch) ----
        TestKit.check(NSEvent.wrap(null) == null, "NSEvent.wrap(null) == null");
        TestKit.check(NSEvent.wrap(MemorySegment.NULL) == null, "NSEvent.wrap(NULL) == null");
        TestKit.check(NSResponder.wrap(null) == null, "NSResponder.wrap(null) == null");
        TestKit.check(NSGestureRecognizer.wrap(MemorySegment.NULL) == null, "NSGestureRecognizer.wrap(NULL) == null");
        TestKit.check(NSClickGestureRecognizer.wrap(null) == null, "NSClickGestureRecognizer.wrap(null) == null");
        TestKit.check(NSMagnificationGestureRecognizer.wrap(MemorySegment.NULL) == null, "NSMagnification.wrap(NULL) == null");
        TestKit.check(NSPanGestureRecognizer.wrap(null) == null, "NSPanGestureRecognizer.wrap(null) == null");
        TestKit.check(NSPressGestureRecognizer.wrap(null) == null, "NSPressGestureRecognizer.wrap(null) == null");
        TestKit.check(NSRotationGestureRecognizer.wrap(MemorySegment.NULL) == null, "NSRotation.wrap(NULL) == null");
        TestKit.check(NSPasteboard.wrap(null) == null, "NSPasteboard.wrap(null) == null");
        TestKit.check(NSPasteboardItem.wrap(MemorySegment.NULL) == null, "NSPasteboardItem.wrap(NULL) == null");
        TestKit.check(NSDraggingSession.wrap(null) == null, "NSDraggingSession.wrap(null) == null");
        TestKit.check(NSDraggingItem.wrap(MemorySegment.NULL) == null, "NSDraggingItem.wrap(NULL) == null");
        TestKit.check(NSTrackingArea.wrap(null) == null, "NSTrackingArea.wrap(null) == null");
        TestKit.check(NSFilePromiseProvider.wrap(MemorySegment.NULL) == null, "NSFilePromiseProvider.wrap(NULL) == null");

        // ---- NSEvent class properties (no event instance needed) ----
        NSPoint mouseAt = NSEvent.mouseLocation();
        TestKit.check(mouseAt != null && !Double.isNaN(mouseAt.x()) && !Double.isNaN(mouseAt.y()),
                "NSEvent.mouseLocation() finite (" + mouseAt + ")");
        TestKit.check(NSEvent.modifierFlagsStatic() >= 0, "NSEvent.modifierFlagsStatic() >= 0");
        TestKit.check(NSEvent.pressedMouseButtons() >= 0, "NSEvent.pressedMouseButtons() >= 0");
        TestKit.check(NSEvent.doubleClickInterval() > 0,
                "NSEvent.doubleClickInterval() > 0 (" + NSEvent.doubleClickInterval() + ")");
        TestKit.check(NSEvent.keyRepeatDelay() > 0, "NSEvent.keyRepeatDelay() > 0");
        TestKit.check(NSEvent.keyRepeatInterval() > 0, "NSEvent.keyRepeatInterval() > 0");
        boolean coalesceSaved = NSEvent.isMouseCoalescingEnabled();
        NSEvent.setMouseCoalescingEnabled(!coalesceSaved);
        TestKit.check(NSEvent.isMouseCoalescingEnabled() == !coalesceSaved,
                "NSEvent mouseCoalescingEnabled round-trip");
        NSEvent.setMouseCoalescingEnabled(coalesceSaved);
        TestKit.check(NSEvent.isMouseCoalescingEnabled() == coalesceSaved,
                "NSEvent mouseCoalescingEnabled restored");
        System.out.println("  swipeTrackingFromScrollEventsEnabled="
                + NSEvent.isSwipeTrackingFromScrollEventsEnabled());
        TestKit.check(true, "NSEvent.isSwipeTrackingFromScrollEventsEnabled() no-throw");
        noThrow("NSEvent start/stopPeriodicEvents no-throw", () -> {
            NSEvent.startPeriodicEventsAfterDelay(3600, 3600);
            NSEvent.stopPeriodicEvents();
        });
        // removeMonitor: takes no block so it is wrapped; with no live monitor
        // installed (installing one needs a block) only the nil guard runs.
        noThrow("NSEvent removeMonitor(null/NULL) no-throw (guarded)", () -> {
            NSEvent.removeMonitor(null);
            NSEvent.removeMonitor(MemorySegment.NULL);
        });
        // NULL CGEventRef aborts natively (AppKit raises), so the factories
        // reject null/NULL in Java with IllegalArgumentException instead.
        for (String which : new String[]{"cg-null", "cg-NULL", "ref-NULL"}) {
            boolean guarded = false;
            try {
                switch (which) {
                    case "cg-null" -> NSEvent.eventWithCGEvent(null);
                    case "cg-NULL" -> NSEvent.eventWithCGEvent(MemorySegment.NULL);
                    default -> NSEvent.eventWithEventRef(MemorySegment.NULL);
                }
            } catch (IllegalArgumentException expected) {
                guarded = true;
            } catch (Throwable t) {
                TestKit.check(false, which + " threw wrong type: " + t);
            }
            TestKit.check(guarded, "NSEvent factory " + which + " guarded (IllegalArgumentException)");
        }

        // ---- synthetic CG mouse event -> NSEvent instance accessors ----
        Linker linker = Linker.nativeLinker();
        SymbolLookup cg = SymbolLookup.libraryLookup(
                "/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics", Arena.global());
        MethodHandle hCreate = linker.downcallHandle(
                cg.find("CGEventCreateMouseEvent").orElseThrow(), NsuiForeign.cgEventCreateMouseEvent());
        MemorySegment point = Arena.global().allocate(16);
        point.set(ValueLayout.JAVA_DOUBLE, 0, 400.0);
        point.set(ValueLayout.JAVA_DOUBLE, 8, 300.0);
        MemorySegment cgEv = (MemorySegment) hCreate.invokeExact(
                MemorySegment.NULL, (int) 1, point, (int) 0);
        TestKit.check(cgEv != null && cgEv.address() != 0, "CGEventCreateMouseEvent non-nil");
        NSEvent ev = NSEvent.eventWithCGEvent(cgEv);
        TestKit.check(ev != null, "NSEvent.eventWithCGEvent real event");
        TestKit.check(ev.type() == 1, "synthetic type() == 1 leftMouseDown (got " + ev.type() + ")");
        TestKit.check(ev.isMouseEvent() && !ev.isKeyEvent(), "synthetic isMouseEvent && !isKeyEvent");
        TestKit.check(ev.clickCount() >= 1, "synthetic clickCount() >= 1 (got " + ev.clickCount() + ")");
        TestKit.check(ev.buttonNumber() == 0, "synthetic buttonNumber() == 0");
        // Direct CG conversion carries CG time 0 (the queued path in NSEventTest
        // normalizes it); assert only non-negative here.
        TestKit.check(ev.timestamp() >= 0, "synthetic timestamp() >= 0 (got " + ev.timestamp() + ")");
        TestKit.check(ev.modifierFlags() >= 0, "synthetic modifierFlags() >= 0");
        NSPoint loc = ev.locationInWindow();
        TestKit.check(loc != null && !Double.isNaN(loc.x()) && !Double.isNaN(loc.y()),
                "synthetic locationInWindow() finite (" + loc + ")");
        TestKit.check(ev.windowNumber() >= 0, "synthetic windowNumber() >= 0");
        TestKit.check(ev.subtype() >= 0, "synthetic subtype() readable");
        TestKit.check(ev.eventNumber() >= 0, "synthetic eventNumber() readable");
        // pressure() is a float in AppKit and is now read through the FLOAT handle; its
        // exact value is event-dependent, so this stays a no-throw probe. The declared-vs-real
        // shape is pinned by SignatureConformanceTest.
        noThrow("synthetic pressure() no-throw", ev::pressure);
        noThrow("synthetic delta/hasPrecise/momentum no-throw", () -> {
            ev.deltaX();
            ev.deltaY();
            ev.deltaZ();
            ev.hasPreciseScrollingDeltas();
            ev.momentumPhase();
        });
        noThrow("synthetic deviceID/rotation/absolute/buttonMask no-throw", () -> {
            ev.deviceID();
            ev.rotation();
            ev.absoluteX();
            ev.absoluteY();
            ev.absoluteZ();
            ev.buttonMask();
        });
        NSPoint tilt = ev.tilt();
        TestKit.check(tilt != null, "synthetic tilt() non-null (" + tilt + ")");
        noThrow("synthetic tangential/associated no-throw", () -> {
            ev.tangentialPressure();
            ev.associatedEventsMask();
        });
        // Tablet / proximity identity block, mouse-safe subset only. The rest
        // (trackingNumber, stage/stageTransition, pressureBehavior, trackingArea,
        // userData, magnification, scrolling deltas, phase, direction-inversion,
        // data1/2, all touch readers) raise NSInternalInconsistencyException on a
        // mouse event — verified with one-process-per-accessor probes — so they
        // are documented, not called, here.
        noThrow("synthetic tablet identity block no-throw", () -> {
            ev.vendorID();
            ev.tabletID();
            ev.pointingDeviceID();
            ev.systemTabletID();
            ev.vendorPointingDeviceType();
            ev.pointingDeviceSerialNumber();
            ev.uniqueID();
            ev.capabilityMask();
            ev.pointingDeviceType();
            ev.isEnteringProximity();
            ev.vendorDefined();
            ev.eventRef();
        });
        TestKit.check(ev.cgEvent() != null && ev.cgEvent().address() != 0,
                "synthetic cgEvent() round-trips the CGEvent");
        TestKit.check(ev.window() == null, "synthetic window() null (unrouted event)");
        TestKit.check(ev.eventRef() != null, "synthetic eventRef() non-nil");
        // key-only guards must refuse a mouse event with IllegalStateException
        for (String which : new String[]{"characters", "keyCode", "isARepeat"}) {
            boolean guarded = false;
            try {
                switch (which) {
                    case "characters" -> ev.characters();
                    case "keyCode" -> ev.keyCode();
                    case "isARepeat" -> ev.isARepeat();
                }
            } catch (IllegalStateException expected) {
                guarded = true;
            } catch (Throwable t) {
                TestKit.check(false, which + " threw wrong type: " + t);
            }
            TestKit.check(guarded, "synthetic " + which + " guarded (IllegalStateException)");
        }
        try {
            ev.charactersIgnoringModifiers();
            TestKit.check(false, "charactersIgnoringModifiers should guard");
        } catch (IllegalStateException expected) {
            TestKit.check(true, "synthetic charactersIgnoringModifiers guarded");
        }
        try {
            ev.charactersByApplyingModifiers(0);
            TestKit.check(false, "charactersByApplyingModifiers should guard");
        } catch (IllegalStateException expected) {
            TestKit.check(true, "synthetic charactersByApplyingModifiers guarded");
        }

        // ---- synthetic CG key event: key-only accessors ----
        FunctionDescriptor keyDesc = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                ValueLayout.JAVA_SHORT, ValueLayout.JAVA_BOOLEAN);
        MethodHandle hKey = linker.downcallHandle(
                cg.find("CGEventCreateKeyboardEvent").orElseThrow(), keyDesc);
        MemorySegment cgKey = (MemorySegment) hKey.invokeExact(MemorySegment.NULL, (short) 8, true);
        TestKit.check(cgKey != null && cgKey.address() != 0, "CGEventCreateKeyboardEvent non-nil");
        NSEvent kev = NSEvent.eventWithCGEvent(cgKey);
        TestKit.check(kev != null && kev.type() == 10,
                "synthetic key type() == 10 (got " + (kev == null ? "null" : kev.type()) + ")");
        TestKit.check(kev.isKeyEvent() && !kev.isMouseEvent(), "synthetic key isKeyEvent && !isMouseEvent");
        TestKit.check(kev.keyCode() == 8, "synthetic key keyCode() == 8 (got " + kev.keyCode() + ")");
        TestKit.check("c".equals(kev.characters()), "synthetic key characters() == c (got " + kev.characters() + ")");
        TestKit.check("c".equals(kev.charactersIgnoringModifiers()), "synthetic key charactersIgnoringModifiers() == c");
        TestKit.check(!kev.isARepeat(), "synthetic key isARepeat() false");
        TestKit.check("c".equals(kev.charactersByApplyingModifiers(0)),
                "synthetic key charactersByApplyingModifiers() == c");
        TestKit.check(kev.buttonNumber() == 0, "synthetic key buttonNumber() == 0");
        TestKit.check(kev.windowNumber() >= 0, "synthetic key windowNumber() >= 0");

        // ---- synthetic CG scroll event: scroll-only accessors ----
        FunctionDescriptor scrollDesc = FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
        MethodHandle hScroll = linker.downcallHandle(
                cg.find("CGEventCreateScrollWheelEvent").orElseThrow(), scrollDesc,
                Linker.Option.firstVariadicArg(3));
        MemorySegment cgScroll = (MemorySegment) hScroll.invokeExact(MemorySegment.NULL, 1, 1, 5);
        TestKit.check(cgScroll != null && cgScroll.address() != 0, "CGEventCreateScrollWheelEvent non-nil");
        NSEvent sev = NSEvent.eventWithCGEvent(cgScroll);
        TestKit.check(sev != null && sev.type() == 22,
                "synthetic scroll type() == 22 (got " + (sev == null ? "null" : sev.type()) + ")");
        TestKit.check(!sev.isMouseEvent() && !sev.isKeyEvent(), "scroll neither mouse nor key");
        TestKit.check(sev.deltaY() != 0 && sev.scrollingDeltaY() != 0,
                "scroll deltaY/scrollingDeltaY non-zero (" + sev.deltaY() + "/" + sev.scrollingDeltaY() + ")");
        TestKit.check(sev.deltaX() == 0 && sev.scrollingDeltaX() == 0, "scroll X deltas zero");
        TestKit.check(!sev.hasPreciseScrollingDeltas(), "scroll hasPreciseScrollingDeltas false (line units)");
        TestKit.check(sev.momentumPhase() == 0 && sev.phase() == 0, "scroll momentum/phase 0");
        TestKit.check(!sev.isDirectionInvertedFromDevice(), "scroll not inverted");
        TestKit.check(sev.deviceID() >= 0 && sev.subtype() >= 0, "scroll deviceID/subtype readable");

        // ---- standalone view + hidden window for responder/gesture work ----
        NSWindow win = TestKit.hiddenWindow(400, 300);
        TestKit.check(win != null && win.peer().address() != 0, "hiddenWindow non-nil");
        NSView view = NSView.create(new NSRect(0, 0, 200, 150), (ctx, dirty) -> {});
        TestKit.check(view != null && view.peer().address() != 0, "NSView.create non-nil");
        win.setContentView(view);

        // NSResponder full sink family with nil events (standalone view: chain
        // ends immediately, so every default implementation is a no-op).
        NSView bare = NSView.create(new NSRect(0, 0, 60, 40), (ctx, dirty) -> {});
        noThrow("responder mouse sinks nil no-throw", () -> {
            bare.mouseDown(null);
            bare.mouseUp(null);
            bare.mouseDragged(null);
            bare.mouseMoved(null);
            bare.rightMouseDown(null);
            bare.rightMouseUp(null);
            bare.otherMouseDown(null);
            bare.otherMouseUp(null);
            bare.rightMouseDragged(null);
            bare.otherMouseDragged(null);
        });
        noThrow("responder scroll/enter/exit/tablet/cursor nil no-throw", () -> {
            bare.scrollWheel(null);
            bare.mouseEntered(null);
            bare.mouseExited(null);
            bare.tabletPoint(null);
            bare.tabletProximity(null);
            bare.cursorUpdate(null);
        });
        noThrow("responder gesture-event sinks nil no-throw", () -> {
            bare.magnifyWithEvent(null);
            bare.rotateWithEvent(null);
            bare.swipeWithEvent(null);
            bare.beginGestureWithEvent(null);
            bare.endGestureWithEvent(null);
            bare.smartMagnifyWithEvent(null);
            bare.changeModeWithEvent(null);
            bare.pressureChangeWithEvent(null);
            bare.quickLookWithEvent(null);
        });
        noThrow("responder touch sinks nil no-throw", () -> {
            bare.touchesBeganWithEvent(null);
            bare.touchesMovedWithEvent(null);
            bare.touchesEndedWithEvent(null);
            bare.touchesCancelledWithEvent(null);
        });
        noThrow("responder key sinks nil no-throw", () -> {
            bare.keyDown(null);
            bare.keyUp(null);
            bare.flagsChanged(null);
        });
        // Category hooks with no AppKit default (probed via respondsToSelector:):
        // performTextFinderAction: and showWritingTools: live on NSTextView only
        // — invoking either raises visible UI (find panel / writing tools), so a
        // hidden-only test must not send them; newWindowForTab: is an override
        // hook no stock class answers. The wrappers exist for real implementors;
        // here we pin the selector contracts without sending.
        TestKit.check(!responds(bare.peer(), "performTextFinderAction:"),
                "plain view does not answer performTextFinderAction: (NSTextView does)");
        TestKit.check(!responds(bare.peer(), "newWindowForTab:"),
                "plain view does not answer newWindowForTab: (override hook)");
        TestKit.check(!responds(bare.peer(), "showWritingTools:"),
                "plain view does not answer showWritingTools:");
        // same sinks with the REAL synthetic event (manual-dispatch path)
        noThrow("responder sinks with real event no-throw", () -> {
            bare.mouseDown(ev);
            bare.rightMouseDown(ev);
            bare.scrollWheel(ev);
            bare.magnifyWithEvent(ev);
            bare.rotateWithEvent(ev);
            bare.tabletPoint(ev);
            bare.pressureChangeWithEvent(ev);
            bare.keyDown(ev);
        });
        TestKit.check(!bare.shouldBeTreatedAsInkEvent(null),
                "shouldBeTreatedAsInkEvent(null) false (no-throw bool)");
        // menu / undo / action surface
        NSMenu menu = NSMenu.create();
        TestKit.check(menu != null && menu.peer().address() != 0, "NSMenu.create non-nil");
        view.setMenu(menu);
        NSMenu back = view.menu();
        TestKit.check(back != null && back.peer().address() == menu.peer().address(),
                "NSResponder setMenu/menu round-trip");
        view.setMenu(null);
        TestKit.check(view.menu() == null, "NSResponder setMenu(null) clears");
        try {
            NSObject um = view.undoManager();
            TestKit.check(true, "NSResponder undoManager() no-throw (" + (um == null ? "null" : "present") + ")");
        } catch (Throwable t) {
            TestKit.check(false, "undoManager threw: " + t);
        }
        TestKit.check(!view.tryToPerformWith(ObjC.sel("becomeFirstResponder"), MemorySegment.NULL)
                        || true,
                "NSResponder tryToPerformWith no-throw");
        try {
            MemorySegment req = view.validRequestorForSendTypeReturnType(MemorySegment.NULL, MemorySegment.NULL);
            TestKit.check(true, "validRequestorForSendTypeReturnType no-throw (" + (req == null ? "null" : "peer") + ")");
        } catch (Throwable t) {
            TestKit.check(false, "validRequestor threw: " + t);
        }
        noThrow("interpretKeyEvents(empty)+flushBufferedKeyEvents no-throw", () -> {
            view.interpretKeyEvents(NSArray.array());
            view.flushBufferedKeyEvents();
        });
        try {
            boolean h = view.wantsScrollEventsForSwipeTrackingOnAxis(0);
            boolean f = view.wantsForwardedScrollEventsForAxis(0);
            TestKit.check(true, "swipe-axis queries no-throw (" + h + "/" + f + ")");
        } catch (Throwable t) {
            TestKit.check(false, "swipe-axis queries threw: " + t);
        }
        try {
            MemorySegment st = view.supplementalTargetForActionSender(MemorySegment.NULL, MemorySegment.NULL);
            TestKit.check(true, "supplementalTargetForActionSender no-throw (" + (st == null ? "null" : "peer") + ")");
        } catch (Throwable t) {
            TestKit.check(false, "supplementalTarget threw: " + t);
        }
        noThrow("insertText/doCommandBySelector no-throw", () -> {
            view.insertText(ObjC.nsstring("hi"));
            view.doCommandBySelector(ObjC.sel("moveForward:"));
        });
        noThrow("performAction flushBufferedKeyEvents no-throw", () ->
                view.performAction("flushBufferedKeyEvents", null));
        try {
            boolean v = view.validateProposedFirstResponderForEvent(view, null);
            TestKit.check(true, "validateProposedFirstResponderForEvent no-throw (" + v + ")");
        } catch (Throwable t) {
            TestKit.check(false, "validateProposedFirstResponder threw: " + t);
        }

        // ---- base recognizer: delays*/touch/config/reset/prevention ----
        MemorySegment target = dummyTarget();
        TestKit.check(target != null && target.address() != 0, "dummy target non-nil");
        NSGestureRecognizer g = NSGestureRecognizer.create(target, "doGesture:");
        g.setDelaysSecondaryMouseButtonEvents(true);
        TestKit.check(g.delaysSecondaryMouseButtonEvents(), "delaysSecondaryMouseButtonEvents round-trip true");
        g.setDelaysSecondaryMouseButtonEvents(false);
        TestKit.check(!g.delaysSecondaryMouseButtonEvents(), "delaysSecondaryMouseButtonEvents round-trip false");
        g.setDelaysOtherMouseButtonEvents(true);
        TestKit.check(g.delaysOtherMouseButtonEvents(), "delaysOtherMouseButtonEvents round-trip true");
        g.setDelaysOtherMouseButtonEvents(false);
        g.setDelaysKeyEvents(true);
        TestKit.check(g.delaysKeyEvents(), "delaysKeyEvents round-trip true");
        g.setDelaysKeyEvents(false);
        g.setDelaysMagnificationEvents(true);
        TestKit.check(g.delaysMagnificationEvents(), "delaysMagnificationEvents round-trip true");
        g.setDelaysMagnificationEvents(false);
        g.setDelaysRotationEvents(true);
        TestKit.check(g.delaysRotationEvents(), "delaysRotationEvents round-trip true");
        g.setDelaysRotationEvents(false);
        TestKit.check(!g.delaysRotationEvents(), "delaysRotationEvents restored false");
        long touchSaved = g.allowedTouchTypes();
        g.setAllowedTouchTypes(1);
        TestKit.check(g.allowedTouchTypes() == 1, "allowedTouchTypes set 1 round-trip");
        g.setAllowedTouchTypes(touchSaved);
        TestKit.check(g.allowedTouchTypes() == touchSaved, "allowedTouchTypes restored");
        try {
            MemorySegment pc = g.pressureConfiguration();
            TestKit.check(true, "pressureConfiguration() no-throw (" + (pc == null ? "null" : "peer") + ")");
            g.setPressureConfiguration(null);
            TestKit.check(true, "setPressureConfiguration(null) no-throw");
        } catch (Throwable t) {
            TestKit.check(false, "pressureConfiguration threw: " + t);
        }
        noThrow("recognizer reset() no-throw", g::reset);
        NSGestureRecognizer peer2 = NSGestureRecognizer.create(target, "other:");
        noThrow("recognizer prevention queries no-throw", () -> {
            g.canPreventGestureRecognizer(peer2);
            g.canBePreventedByGestureRecognizer(peer2);
            g.shouldRequireFailureOfGestureRecognizer(peer2);
            g.shouldBeRequiredToFailByGestureRecognizer(peer2);
            g.canPreventGestureRecognizer(null);
        });
        noThrow("recognizer sinks with real event no-throw", () -> {
            g.mouseDown(ev);
            g.mouseUp(ev);
            g.mouseDragged(ev);
            g.keyDown(ev);
        });
        // macOS 26 identity (same availability as the wrapped mouseCancelled:).
        g.setName("event-test");
        TestKit.check("event-test".equals(g.name()), "recognizer name round-trip");
        g.setName(null);
        TestKit.check(g.name() == null, "recognizer name cleared");
        TestKit.check(g.modifierFlags() >= 0, "recognizer modifierFlags readable");

        // ---- the four newer recognizers: property round-trips ----
        NSMagnificationGestureRecognizer mag = NSMagnificationGestureRecognizer.create(target, "mag:");
        double magSaved = mag.magnification();
        mag.setMagnification(0.5);
        TestKit.check(near(mag.magnification(), 0.5), "magnification set 0.5 round-trip (got " + mag.magnification() + ")");
        mag.setMagnification(magSaved);
        TestKit.check(near(mag.magnification(), magSaved), "magnification restored");

        NSPressGestureRecognizer press = NSPressGestureRecognizer.create(target, "press:");
        long pressMaskSaved = press.buttonMask();
        press.setButtonMask(2);
        TestKit.check(press.buttonMask() == 2, "press buttonMask 2 round-trip");
        press.setButtonMask(pressMaskSaved);
        double durSaved = press.minimumPressDuration();
        press.setMinimumPressDuration(0.75);
        TestKit.check(near(press.minimumPressDuration(), 0.75),
                "press minimumPressDuration 0.75 round-trip (got " + press.minimumPressDuration() + ")");
        press.setMinimumPressDuration(durSaved);
        double moveSaved = press.allowableMovement();
        press.setAllowableMovement(15.0);
        TestKit.check(near(press.allowableMovement(), 15.0),
                "press allowableMovement 15 round-trip (got " + press.allowableMovement() + ")");
        press.setAllowableMovement(moveSaved);
        long pressTouchSaved = press.numberOfTouchesRequired();
        press.setNumberOfTouchesRequired(1);
        TestKit.check(press.numberOfTouchesRequired() == 1, "press numberOfTouchesRequired 1 round-trip");
        press.setNumberOfTouchesRequired(pressTouchSaved);

        NSRotationGestureRecognizer rot = NSRotationGestureRecognizer.create(target, "rot:");
        rot.setRotation(0.5);
        TestKit.check(near(rot.rotation(), 0.5), "rotation set 0.5 round-trip (got " + rot.rotation() + ")");
        TestKit.check(Math.abs(rot.rotationInDegrees() - Math.toDegrees(0.5)) < 0.01,
                "rotationInDegrees tracks radians (got " + rot.rotationInDegrees() + ")");
        rot.setRotationInDegrees(90.0);
        TestKit.check(Math.abs(rot.rotation() - Math.PI / 2) < 0.01,
                "setRotationInDegrees(90) -> pi/2 rad (got " + rot.rotation() + ")");
        rot.setRotation(0);
        TestKit.check(near(rot.rotation(), 0), "rotation restored to 0");

        NSClickGestureRecognizer click = NSClickGestureRecognizer.create(target, "clicked:");
        long clickTouchSaved = click.numberOfTouchesRequired();
        click.setNumberOfTouchesRequired(1);
        TestKit.check(click.numberOfTouchesRequired() == 1,
                "click numberOfTouchesRequired 1 round-trip (got " + click.numberOfTouchesRequired() + ")");
        click.setNumberOfTouchesRequired(clickTouchSaved);
        TestKit.check(click.numberOfTouchesRequired() == clickTouchSaved,
                "click numberOfTouchesRequired restored");

        // ---- pasteboard on a private unique board (never touches user data) ----
        NSPasteboard board = NSPasteboard.pasteboardWithUniqueName();
        TestKit.check(board != null && board.peer().address() != 0, "pasteboardWithUniqueName non-nil");
        TestKit.check(board.name() != null, "unique board name() non-nil");
        long cc0 = board.clearContents();
        TestKit.check(cc0 >= 0, "unique board clearContents -> changeCount " + cc0);
        String ptype = "public.utf8-plain-text";
        TestKit.check(board.setStringForType("hello-events", ptype), "unique board setStringForType true");
        TestKit.check("hello-events".equals(board.stringForType(ptype)), "unique board stringForType round-trip");
        // changeCount is monotonic; on this OS setString:forType: alone does not
        // bump it (verified: clear 0->1, setString stays 1, clear ->2), while
        // clearContents always advances it.
        TestKit.check(board.changeCount() >= cc0, "changeCount monotonic after write");
        TestKit.check(board.clearContents() > cc0, "second clearContents advances changeCount");
        // Re-populate after the counting exercise above (it emptied the board).
        TestKit.check(board.setStringForType("hello-events", ptype), "re-populate setStringForType true");
        NSArray types = board.types();
        TestKit.check(types != null && types.count() >= 1, "unique board types() non-empty");
        TestKit.check(board.availableTypeFromArray(types) != null, "availableTypeFromArray non-nil");
        NSArray items = board.pasteboardItems();
        TestKit.check(items == null || items.count() >= 0, "pasteboardItems() no-throw");
        if (items != null && items.count() > 0) {
            NSPasteboardItem first = NSPasteboardItem.wrap(items.objectAtIndex(0));
            TestKit.check(first != null, "first pasteboardItems element wraps");
            TestKit.check(board.indexOfPasteboardItem(first) == 0, "indexOfPasteboardItem(first) == 0");
        }
        NSArray strTypes = NSArray.mutableArray();
        strTypes.addObject(ObjC.nsstring(ptype));
        TestKit.check(board.canReadItemWithDataConformingToTypes(strTypes),
                "canReadItemWithDataConformingToTypes true");
        NSArray pngTypes = NSArray.mutableArray();
        pngTypes.addObject(ObjC.nsstring("public.png"));
        TestKit.check(board.addTypesOwner(pngTypes, null) >= 0, "addTypesOwner no-throw");
        NSPasteboardItem witem = NSPasteboardItem.withString("drag-me", ptype);
        NSArray writers = NSArray.mutableArray();
        writers.addObject(witem.peer());
        TestKit.check(board.writeObjects(writers), "writeObjects([item]) true");
        NSArray afterWrite = board.pasteboardItems();
        TestKit.check(afterWrite != null && afterWrite.count() >= 1, "pasteboardItems non-empty after writeObjects");
        NSArray classes = NSArray.mutableArray();
        classes.addObject(ObjC.cls("NSString"));
        TestKit.check(board.canReadObjectForClassesOptions(classes, null),
                "canReadObjectForClassesOptions(NSString) true");
        NSArray readBack = board.readObjectsForClassesOptions(classes, null);
        TestKit.check(readBack != null && readBack.count() >= 1, "readObjectsForClasses returns >= 1");
        if (readBack != null && readBack.count() >= 1) {
            TestKit.check("drag-me".equals(readBack.stringAt(0).toString())
                            || readBack.stringAt(0) != null,
                    "readObjectsForClasses first element readable (" + readBack.stringAt(0) + ")");
        }
        TestKit.check(board.prepareForNewContentsWithOptions(0) >= 0, "prepareForNewContentsWithOptions no-throw");
        NSData payload = NSData.dataWithBytes(new byte[]{1, 2, 3, 4});
        TestKit.check(board.setDataForType(payload, "public.data"), "setDataForType true");
        NSData gotData = board.dataForType("public.data");
        TestKit.check(gotData != null && Arrays.equals(gotData.toByteArray(), new byte[]{1, 2, 3, 4}),
                "dataForType round-trip bytes");
        TestKit.check(board.setPropertyListForType(ObjC.nsstring("plist-string"), "public.plain-text"),
                "setPropertyListForType true");
        TestKit.check(board.propertyListForType("public.plain-text") != null,
                "propertyListForType non-nil");
        board.declareTypes(List.of(ptype), MemorySegment.NULL);
        TestKit.check(true, "declareTypes(List, NULL) no-throw");
        try {
            NSArray filt = NSPasteboard.typesFilterableTo(ptype);
            TestKit.check(true, "typesFilterableTo no-throw (" + (filt == null ? "null" : filt.count()) + ")");
        } catch (Throwable t) {
            TestKit.check(false, "typesFilterableTo threw: " + t);
        }
        try {
            MemorySegment url = NSPasteboard.urlFromPasteboard(board);
            TestKit.check(true, "urlFromPasteboard no-throw (" + (url == null ? "null" : "peer") + ")");
        } catch (Throwable t) {
            TestKit.check(false, "urlFromPasteboard threw: " + t);
        }

        // ---- pasteboard item triples ----
        NSPasteboardItem item = NSPasteboardItem.withString("item-text", ptype);
        TestKit.check(item != null, "NSPasteboardItem.withString non-nil");
        NSArray itemTypes = item.types();
        TestKit.check(itemTypes != null && itemTypes.count() >= 1, "item types() non-empty");
        // availableTypeFromArray: answers nil until the item is bound to a
        // pasteboard (probed: types() lists the type but the query is nil
        // pre-write, exact post-write), so bind it to the private board first.
        NSArray itemWriters = NSArray.mutableArray();
        itemWriters.addObject(item.peer());
        TestKit.check(board.writeObjects(itemWriters), "writeObjects([item]) true");
        String itemAvail = item.availableTypeFromArray(itemTypes);
        TestKit.check(ptype.equals(itemAvail),
                "item availableTypeFromArray == type after bind (got " + itemAvail + ")");
        TestKit.check("item-text".equals(item.stringForType(ptype)), "item stringForType round-trip");
        TestKit.check(item.setDataForType(NSData.dataWithBytes(new byte[]{9, 8}), "public.data"),
                "item setDataForType true");
        NSData itemData = item.dataForType("public.data");
        TestKit.check(itemData != null && Arrays.equals(itemData.toByteArray(), new byte[]{9, 8}),
                "item dataForType round-trip");
        TestKit.check(item.setPropertyListForType(ObjC.nsstring("v"), "com.example.plist"),
                "item setPropertyListForType true");
        TestKit.check(item.propertyListForType("com.example.plist") != null,
                "item propertyListForType non-nil");
        TestKit.check(!item.setStringForType(null, ptype), "item setStringForType(null) false");
        TestKit.check(item.stringForType((String) null) == null, "item stringForType(null) null");

        // ---- dragging item frames ----
        NSDraggingItem d = NSDraggingItem.withString("dnd", ptype);
        TestKit.check(d != null && d.peer().address() != 0, "NSDraggingItem.withString non-nil");
        TestKit.check(d.item() != null, "NSDraggingItem.item() non-nil");
        NSRect df0 = d.draggingFrame();
        TestKit.check(df0 != null, "NSDraggingItem.draggingFrame() readable (" + df0 + ")");
        d.setDraggingFrame(new NSRect(10, 20, 100, 50));
        NSRect df1 = d.draggingFrame();
        TestKit.check(near(df1.x(), 10) && near(df1.y(), 20)
                        && near(df1.width(), 100) && near(df1.height(), 50),
                "NSDraggingItem setDraggingFrame round-trip (" + df1 + ")");
        noThrow("setDraggingFrameContents(null contents) no-throw",
                () -> d.setDraggingFrameContents(new NSRect(0, 0, 44, 44), null));
        try {
            NSArray comps = d.imageComponents();
            TestKit.check(true, "imageComponents() no-throw (" + (comps == null ? "null" : comps.count()) + ")");
        } catch (Throwable t) {
            TestKit.check(false, "imageComponents threw: " + t);
        }

        // ---- destination + source delegates (incl. new selectors) ----
        final boolean[] updated = {false};
        final boolean[] exited = {false};
        NSDraggingDestination dest = new NSDraggingDestination() {
            @Override public long draggingEntered(NSDraggingSession s) { return 1; }
            @Override public long springLoadingEntered(NSDraggingSession s) { return 3; }
            @Override public long springLoadingUpdated(NSDraggingSession s) { return 5; }
            @Override public void updateDraggingItemsForDrag(NSDraggingSession s) { updated[0] = true; }
            @Override public void springLoadingExited(NSDraggingSession s) { exited[0] = true; }
        };
        MemorySegment del = NSDraggingDestination.delegate(dest);
        TestKit.check(del != null && del.address() != 0, "NSDraggingDestination.delegate non-nil");
        MethodHandle hInt1 = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
        MethodHandle hVoid1 = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        try {
            long se = (long) hInt1.invokeExact(del, ObjC.sel("springLoadingEntered:"), MemorySegment.NULL);
            TestKit.check(se == 3, "springLoadingEntered: -> 3 (got " + se + ")");
            long su = (long) hInt1.invokeExact(del, ObjC.sel("springLoadingUpdated:"), MemorySegment.NULL);
            TestKit.check(su == 5, "springLoadingUpdated: -> 5 (got " + su + ")");
            hVoid1.invokeExact(del, ObjC.sel("updateDraggingItemsForDrag:"), MemorySegment.NULL);
            TestKit.check(updated[0], "updateDraggingItemsForDrag: routed");
            hVoid1.invokeExact(del, ObjC.sel("springLoadingExited:"), MemorySegment.NULL);
            TestKit.check(exited[0], "springLoadingExited: routed");
            long entered = (long) hInt1.invokeExact(del, ObjC.sel("draggingEntered:"), MemorySegment.NULL);
            TestKit.check(entered == 1, "draggingEntered: still -> 1");
        } catch (Throwable t) {
            TestKit.check(false, "destination delegate invoke threw: " + t);
        }
        TestKit.check(dest.wantsPeriodicDraggingUpdates(), "wantsPeriodicDraggingUpdates default true");
        NSDraggingSource src = new NSDraggingSource() {
            @Override public long draggingSessionSourceOperationMaskForDraggingContext(NSDraggingSession s, long c) { return 1; }
        };
        MemorySegment srcDel = NSDraggingSource.delegate(src);
        TestKit.check(srcDel != null && srcDel.address() != 0, "NSDraggingSource.delegate non-nil");
        try {
            boolean ign = (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID))
                    .invokeExact(srcDel, ObjC.sel("ignoreModifierKeysForDraggingSession:"), MemorySegment.NULL);
            TestKit.check(!ign, "ignoreModifierKeysForDraggingSession false");
        } catch (Throwable t) {
            TestKit.check(false, "source delegate invoke threw: " + t);
        }
        TestKit.check(DelegateProxy.registrySize() >= 2, "DelegateProxy registry holds both delegates");

        // ---- file promise provider (live, via a minimal conforming delegate) ----
        // AppKit validates the delegate at init time and aborts the process
        // (uncaught NSException) when it is nil or misses a required method,
        // so create() rejects nil in Java and the test builds a delegate with
        // both required selectors (promiseDelegate() above; verified live in
        // a scratch probe before wiring it in).
        MemorySegment promiseDel = promiseDelegate();
        TestKit.check(promiseDel != null && promiseDel.address() != 0, "promise delegate non-nil");
        NSObject promiseObj = NSObject.wrap(promiseDel);
        NSFilePromiseProvider fp = NSFilePromiseProvider.create("public.plain-text", promiseObj);
        TestKit.check(fp != null && fp.peer().address() != 0, "NSFilePromiseProvider.create non-nil");
        TestKit.check("public.plain-text".equals(fp.fileType()), "filePromise fileType round-trip");
        fp.setFileType("public.png");
        TestKit.check("public.png".equals(fp.fileType()), "filePromise setFileType round-trip");
        TestKit.check(fp.delegate() != null && fp.delegate().peer().address() == promiseDel.address(),
                "filePromise delegate sticks");
        fp.setDelegate(promiseObj);
        TestKit.check(fp.delegate() != null, "filePromise setDelegate(NSObject) sticks");
        fp.setUserInfo(promiseObj);
        TestKit.check(fp.userInfo() != null, "filePromise setUserInfo round-trip");
        fp.setUserInfo(null);
        TestKit.check(fp.userInfo() == null, "filePromise setUserInfo(null) clears");
        try {
            NSFilePromiseProvider.create("public.plain-text", null);
            TestKit.check(false, "create(nil delegate) should guard");
        } catch (IllegalArgumentException expected) {
            TestKit.check(true, "create(nil delegate) guarded (IllegalArgumentException)");
        }
        TestKit.check(NSFilePromiseProvider.wrap(null) == null, "filePromise wrap(null) == null (again, post-delegates)");

        // ---- tracking area: constants + round-trips ----
        TestKit.check(NSTrackingArea.MOUSE_ENTERED_AND_EXITED == 0x01, "tracking const ENTERED_AND_EXITED");
        TestKit.check(NSTrackingArea.MOUSE_MOVED == 0x02, "tracking const MOUSE_MOVED");
        TestKit.check(NSTrackingArea.CURSOR_UPDATE == 0x04, "tracking const CURSOR_UPDATE");
        TestKit.check(NSTrackingArea.ACTIVE_WHEN_FIRST_RESPONDER == 0x10, "tracking const ACTIVE_WHEN_FIRST_RESPONDER");
        TestKit.check(NSTrackingArea.ACTIVE_IN_KEY_WINDOW == 0x20, "tracking const ACTIVE_IN_KEY_WINDOW");
        TestKit.check(NSTrackingArea.ACTIVE_IN_ACTIVE_APP == 0x40, "tracking const ACTIVE_IN_ACTIVE_APP");
        TestKit.check(NSTrackingArea.ACTIVE_ALWAYS == 0x80, "tracking const ACTIVE_ALWAYS");
        TestKit.check(NSTrackingArea.ASSUME_INSIDE == 0x100, "tracking const ASSUME_INSIDE");
        TestKit.check(NSTrackingArea.IN_VISIBLE_RECT == 0x200, "tracking const IN_VISIBLE_RECT");
        TestKit.check(NSTrackingArea.ENABLED_DURING_MOUSE_DRAG == 0x400, "tracking const ENABLED_DURING_MOUSE_DRAG");
        TestKit.check(NSTrackingArea.DEFAULT_OPTIONS == 0x283, "tracking DEFAULT_OPTIONS == 0x283");
        NSTrackingArea area = NSTrackingArea.create(
                new NSRect(0, 0, 40, 40), NSTrackingArea.DEFAULT_OPTIONS, view);
        TestKit.check(area != null, "NSTrackingArea.create non-nil");
        NSRect ar = area.rect();
        TestKit.check(near(ar.width(), 40) && near(ar.height(), 40), "tracking rect round-trip (" + ar + ")");
        TestKit.check(area.options() == NSTrackingArea.DEFAULT_OPTIONS, "tracking options round-trip");
        TestKit.check(area.owner() != null && area.owner().address() == view.peer().address(),
                "tracking owner is the view");
        MemorySegment ui = area.userInfo();
        TestKit.check(ui == null || ui.address() == 0, "tracking userInfo nil (not provided)");

        board.releaseGlobally();
        TestKit.check(true, "unique board releaseGlobally no-throw");
        TestKit.close(win);
        TestKit.end();
    }
}
