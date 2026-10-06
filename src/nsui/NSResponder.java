package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSResponder — the base class of AppKit's responder chain. Thin 1:1 wrapper:
/// every method maps to one `objc_msgSend` selector on the native NSResponder;
/// no cached Java state beyond the peer and the lazily-resolved handles.
///
/// The responder chain is how AppKit routes unhandled input: key events go to
/// the window's first responder and mouse events to the view under the cursor,
/// and each `nextResponder` gets a say until some object handles the event or
/// the chain ends at the window and application. `NSView` and `NSWindow`
/// extend this wrapper, so views and windows share one chain API — including
/// `touchBar`/`setTouchBar`, which AppKit declares on NSResponder itself.
///
/// The event methods here (`keyDown`, `mouseDown`, ...) SEND the matching
/// selector to the receiver — they are the manual-dispatch escape hatch, not
/// the callback path. Java callbacks for views created with `NSView.create`
/// are wired separately via `NSView.setMouseListener` / `NSView.setKeyListener`,
/// whose upcall targets hand unhandled events to the next responder so the
/// native chain keeps flowing.
///
/// Coverage notes (header: NSResponder.h wins on API truth):
/// - Wrapped: the full event sink family (every `-(void)…:(NSEvent*)`
///   method, one shape `(void,id)`), the first-responder trio, chain
///   wiring, Touch Bar, menu/undoManager, tryToPerform:, requestor lookup,
///   key-event interpretation, swipe-axis queries, supplemental targets,
///   text-action dispatch via performAction:, and responder validation.
/// - Omitted: initWithCoder: (init overloads are covered by create());
///   presentError:modalForWindow:… (5-arg mixed id/SEL/pointer shape has no
///   vocabulary entry — requested `of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID,
///   Arg.ID, Arg.ID)`); presentError:/willPresentError: (no NSError wrapper
///   in this batch); performMnemonic: (deprecated); the ~100
///   NSStandardKeyBindingResponding action selectors individually — they all
///   share `(void,id)` and go through performAction:.
public class NSResponder extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hBool, MethodHandle hId, MethodHandle hVoidId, MethodHandle hBoolId) {}
    private static volatile Handles H;

    /// Wrap a native NSResponder id (null for nil).
    public static NSResponder wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSResponder(peer);
    }

    protected NSResponder(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.BOOL)),            // acceptsFirstResponder / become / resign
                ObjC.handle(Sig.of(Ret.ID)),              // nextResponder / touchBar
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),    // setNextResponder: / setTouchBar: / event pass-throughs
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));   // performKeyEquivalent:
    }

    // ---------------------------------------------------------------- first-responder status

    /// acceptsFirstResponder — whether the receiver accepts first-responder
    /// status. On views created via `NSView.create` this is overridden to
    /// return true exactly when a key listener is registered (see
    /// `NSView.setKeyListener`), so plain drawing views never steal key focus.
    public boolean acceptsFirstResponder() {
        ensureInit();
        try { return (boolean) H.hBool().invokeExact(peer, ObjC.sel("acceptsFirstResponder")); }
        catch (Throwable t) { throw new RuntimeException("acceptsFirstResponder failed", t); }
    }

    /// becomeFirstResponder — ask to become the window's first responder.
    /// Usually invoked indirectly by `NSWindow.makeFirstResponder`.
    public boolean becomeFirstResponder() {
        ensureInit();
        try { return (boolean) H.hBool().invokeExact(peer, ObjC.sel("becomeFirstResponder")); }
        catch (Throwable t) { throw new RuntimeException("becomeFirstResponder failed", t); }
    }

    /// resignFirstResponder — notification that first-responder status is being
    /// given up. Return value is almost always true.
    public boolean resignFirstResponder() {
        ensureInit();
        try { return (boolean) H.hBool().invokeExact(peer, ObjC.sel("resignFirstResponder")); }
        catch (Throwable t) { throw new RuntimeException("resignFirstResponder failed", t); }
    }

    // ---------------------------------------------------------------- chain wiring

    /// nextResponder — the next link in the responder chain, or null at the end.
    public NSResponder nextResponder() {
        ensureInit();
        try { return NSResponder.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("nextResponder"))); }
        catch (Throwable t) { throw new RuntimeException("nextResponder failed", t); }
    }

    /// setNextResponder: — replace the next link in the responder chain.
    /// Pass null to terminate the chain at this responder.
    public void setNextResponder(NSResponder responder) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("setNextResponder:"),
                    (MemorySegment)(responder == null ? MemorySegment.NULL : responder.peer()));
        } catch (Throwable t) { throw new RuntimeException("setNextResponder: failed", t); }
    }

    // ---------------------------------------------------------------- event dispatch

    /// performKeyEquivalent: — give the receiver a chance to consume a key
    /// equivalent (Cmd-key combination) BEFORE it becomes a keyDown. Return
    /// true to consume the event; false lets it continue down the chain.
    public boolean performKeyEquivalent(NSEvent event) {
        ensureInit();
        try {
            return (boolean) H.hBoolId().invokeExact(peer, ObjC.sel("performKeyEquivalent:"),
                    (MemorySegment)(event == null ? MemorySegment.NULL : event.peer()));
        } catch (Throwable t) { throw new RuntimeException("performKeyEquivalent: failed", t); }
    }

    /// keyDown: — send a key-down event to the receiver. The default AppKit
    /// implementation forwards to the next responder.
    public void keyDown(NSEvent event) { sendEvent("keyDown:", event); }

    /// keyUp: — send a key-up event to the receiver.
    public void keyUp(NSEvent event) { sendEvent("keyUp:", event); }

    /// flagsChanged: — send a modifier-flag change (Shift/Cmd/Option/Ctrl...) to
    /// the receiver.
    public void flagsChanged(NSEvent event) { sendEvent("flagsChanged:", event); }

    /// mouseDown: — send a mouse-down event to the receiver.
    public void mouseDown(NSEvent event) { sendEvent("mouseDown:", event); }

    /// mouseUp: — send a mouse-up event to the receiver.
    public void mouseUp(NSEvent event) { sendEvent("mouseUp:", event); }

    /// mouseDragged: — send a mouse-dragged event to the receiver.
    public void mouseDragged(NSEvent event) { sendEvent("mouseDragged:", event); }

    /// mouseMoved: — send a mouse-moved event to the receiver. Delivery requires
    /// the window to accept mouse-moved events (`NSWindow.setAcceptsMouseMovedEvents`)
    /// and, for tracking-area-driven delivery, `NSView.enableMouseTracking`.
    public void mouseMoved(NSEvent event) { sendEvent("mouseMoved:", event); }

    private void sendEvent(String selector, NSEvent event) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel(selector),
                    (MemorySegment)(event == null ? MemorySegment.NULL : event.peer()));
        } catch (Throwable t) { throw new RuntimeException(selector + " failed", t); }
    }

    // ---------------------------------------------------------------- Touch Bar (declared on NSResponder)

    /// setTouchBar: — attach an NSTouchBar (raw id form; null clears).
    public void setTouchBar(MemorySegment touchBar) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("setTouchBar:"),
                    (MemorySegment)(touchBar == null ? MemorySegment.NULL : touchBar));
        } catch (Throwable t) { throw new RuntimeException("setTouchBar: failed", t); }
    }

    /// Typed overload of `setTouchBar:` — attach an NSTouchBar (null clears).
    public void setTouchBar(NSTouchBar touchBar) {
        setTouchBar(touchBar == null ? null : touchBar.peer());
    }

    /// touchBar — the attached NSTouchBar, or null if none.
    public NSTouchBar touchBar() {
        ensureInit();
        try { return NSTouchBar.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("touchBar"))); }
        catch (Throwable t) { throw new RuntimeException("touchBar failed", t); }
    }

    // ------------------------------------------------- full event sink family
    // Every method below is one `(void,id)` message; the default AppKit
    // implementation forwards to the next responder or ignores the event.

    /// rightMouseDown:.
    public void rightMouseDown(NSEvent event) { sendEvent("rightMouseDown:", event); }

    /// otherMouseDown:.
    public void otherMouseDown(NSEvent event) { sendEvent("otherMouseDown:", event); }

    /// rightMouseUp:.
    public void rightMouseUp(NSEvent event) { sendEvent("rightMouseUp:", event); }

    /// otherMouseUp:.
    public void otherMouseUp(NSEvent event) { sendEvent("otherMouseUp:", event); }

    /// rightMouseDragged:.
    public void rightMouseDragged(NSEvent event) { sendEvent("rightMouseDragged:", event); }

    /// otherMouseDragged:.
    public void otherMouseDragged(NSEvent event) { sendEvent("otherMouseDragged:", event); }

    /// scrollWheel:.
    public void scrollWheel(NSEvent event) { sendEvent("scrollWheel:", event); }

    /// mouseEntered:.
    public void mouseEntered(NSEvent event) { sendEvent("mouseEntered:", event); }

    /// mouseExited:.
    public void mouseExited(NSEvent event) { sendEvent("mouseExited:", event); }

    /// mouseCancelled: (macOS 26+).
    public void mouseCancelled(NSEvent event) { sendEvent("mouseCancelled:", event); }

    /// tabletPoint:.
    public void tabletPoint(NSEvent event) { sendEvent("tabletPoint:", event); }

    /// tabletProximity:.
    public void tabletProximity(NSEvent event) { sendEvent("tabletProximity:", event); }

    /// cursorUpdate:.
    public void cursorUpdate(NSEvent event) { sendEvent("cursorUpdate:", event); }

    /// magnifyWithEvent:.
    public void magnifyWithEvent(NSEvent event) { sendEvent("magnifyWithEvent:", event); }

    /// rotateWithEvent:.
    public void rotateWithEvent(NSEvent event) { sendEvent("rotateWithEvent:", event); }

    /// swipeWithEvent:.
    public void swipeWithEvent(NSEvent event) { sendEvent("swipeWithEvent:", event); }

    /// beginGestureWithEvent:.
    public void beginGestureWithEvent(NSEvent event) { sendEvent("beginGestureWithEvent:", event); }

    /// endGestureWithEvent:.
    public void endGestureWithEvent(NSEvent event) { sendEvent("endGestureWithEvent:", event); }

    /// smartMagnifyWithEvent:.
    public void smartMagnifyWithEvent(NSEvent event) { sendEvent("smartMagnifyWithEvent:", event); }

    /// changeModeWithEvent:.
    public void changeModeWithEvent(NSEvent event) { sendEvent("changeModeWithEvent:", event); }

    /// touchesBeganWithEvent:.
    public void touchesBeganWithEvent(NSEvent event) { sendEvent("touchesBeganWithEvent:", event); }

    /// touchesMovedWithEvent:.
    public void touchesMovedWithEvent(NSEvent event) { sendEvent("touchesMovedWithEvent:", event); }

    /// touchesEndedWithEvent:.
    public void touchesEndedWithEvent(NSEvent event) { sendEvent("touchesEndedWithEvent:", event); }

    /// touchesCancelledWithEvent:.
    public void touchesCancelledWithEvent(NSEvent event) { sendEvent("touchesCancelledWithEvent:", event); }

    /// quickLookWithEvent:.
    public void quickLookWithEvent(NSEvent event) { sendEvent("quickLookWithEvent:", event); }

    /// pressureChangeWithEvent:.
    public void pressureChangeWithEvent(NSEvent event) { sendEvent("pressureChangeWithEvent:", event); }

    /// helpRequested:.
    public void helpRequested(NSEvent event) { sendEvent("helpRequested:", event); }

    /// contextMenuKeyDown: (macOS 15+).
    public void contextMenuKeyDown(NSEvent event) { sendEvent("contextMenuKeyDown:", event); }

    /// showContextHelp: — sender is usually null.
    public void showContextHelp(MemorySegment sender) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("showContextHelp:"),
                    (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
        } catch (Throwable t) { throw new RuntimeException("showContextHelp: failed", t); }
    }

    /// shouldBeTreatedAsInkEvent: — ink (pen) event routing query.
    public boolean shouldBeTreatedAsInkEvent(NSEvent event) {
        ensureInit();
        try {
            return (boolean) H.hBoolId().invokeExact(peer, ObjC.sel("shouldBeTreatedAsInkEvent:"),
                    (MemorySegment) (event == null ? MemorySegment.NULL : event.peer()));
        } catch (Throwable t) { throw new RuntimeException("shouldBeTreatedAsInkEvent: failed", t); }
    }

    // ------------------------------------------------- menu / undo / actions

    /// menu — the contextual menu, or null.
    public NSMenu menu() {
        ensureInit();
        try { return NSMenu.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("menu"))); }
        catch (Throwable t) { throw new RuntimeException("menu failed", t); }
    }

    /// setMenu: — attach a contextual menu (null clears).
    public void setMenu(NSMenu menu) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("setMenu:"),
                    (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
        } catch (Throwable t) { throw new RuntimeException("setMenu: failed", t); }
    }

    /// undoManager — the responder's undo manager, or null.
    public NSObject undoManager() {
        ensureInit();
        try { return NSObject.wrap((MemorySegment) H.hId().invokeExact(peer, ObjC.sel("undoManager"))); }
        catch (Throwable t) { throw new RuntimeException("undoManager failed", t); }
    }

    /// tryToPerform:with: — attempt an action on the receiver.
    public boolean tryToPerformWith(MemorySegment action, MemorySegment target) {
        ensureInit();
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("tryToPerform:with:"),
                    (MemorySegment) (action == null ? MemorySegment.NULL : action),
                    (MemorySegment) (target == null ? MemorySegment.NULL : target));
        } catch (Throwable t) { throw new RuntimeException("tryToPerform:with: failed", t); }
    }

    /// validRequestorForSendType:returnType: — services requestor lookup.
    public MemorySegment validRequestorForSendTypeReturnType(MemorySegment sendType, MemorySegment returnType) {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("validRequestorForSendType:returnType:"),
                    (MemorySegment) (sendType == null ? MemorySegment.NULL : sendType),
                    (MemorySegment) (returnType == null ? MemorySegment.NULL : returnType));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("validRequestorForSendType:returnType: failed", t); }
    }

    /// interpretKeyEvents: — feed key events through the key-binding interpreter.
    public void interpretKeyEvents(NSArray events) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("interpretKeyEvents:"),
                    (MemorySegment) (events == null ? MemorySegment.NULL : events.peer()));
        } catch (Throwable t) { throw new RuntimeException("interpretKeyEvents: failed", t); }
    }

    /// flushBufferedKeyEvents.
    public void flushBufferedKeyEvents() {
        ObjC.msgSendVoid(peer, ObjC.sel("flushBufferedKeyEvents"));
    }

    /// noResponderFor: — the chain ran out for an action selector.
    public void noResponderFor(MemorySegment eventSelector) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("noResponderFor:"),
                    (MemorySegment) (eventSelector == null ? MemorySegment.NULL : eventSelector));
        } catch (Throwable t) { throw new RuntimeException("noResponderFor: failed", t); }
    }

    /// wantsScrollEventsForSwipeTrackingOnAxis: — NSEventGestureAxis (NSInteger).
    public boolean wantsScrollEventsForSwipeTrackingOnAxis(long axis) {
        ensureInit();
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.INT))
                    .invokeExact(peer, ObjC.sel("wantsScrollEventsForSwipeTrackingOnAxis:"), axis);
        } catch (Throwable t) { throw new RuntimeException("wantsScrollEventsForSwipeTrackingOnAxis: failed", t); }
    }

    /// wantsForwardedScrollEventsForAxis: — NSEventGestureAxis (NSInteger).
    public boolean wantsForwardedScrollEventsForAxis(long axis) {
        ensureInit();
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.INT))
                    .invokeExact(peer, ObjC.sel("wantsForwardedScrollEventsForAxis:"), axis);
        } catch (Throwable t) { throw new RuntimeException("wantsForwardedScrollEventsForAxis: failed", t); }
    }

    /// supplementalTargetForAction:sender:.
    public MemorySegment supplementalTargetForActionSender(MemorySegment action, MemorySegment sender) {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("supplementalTargetForAction:sender:"),
                    (MemorySegment) (action == null ? MemorySegment.NULL : action),
                    (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("supplementalTargetForAction:sender: failed", t); }
    }

    /// insertText: — text-input insertion (NSString or attributed string peer).
    public void insertText(MemorySegment insertString) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("insertText:"),
                    (MemorySegment) (insertString == null ? MemorySegment.NULL : insertString));
        } catch (Throwable t) { throw new RuntimeException("insertText: failed", t); }
    }

    /// doCommandBySelector: — key-binding command dispatch.
    public void doCommandBySelector(MemorySegment selector) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("doCommandBySelector:"),
                    (MemorySegment) (selector == null ? MemorySegment.NULL : selector));
        } catch (Throwable t) { throw new RuntimeException("doCommandBySelector: failed", t); }
    }

    /// performAction:withSender: — one generic dispatcher for the whole
    /// NSStandardKeyBindingResponding action family (moveForward:, selectAll:,
    /// deleteBackward:, insertNewline:, cancelOperation:, …): every one of
    /// those selectors shares the `(void,id)` shape, so each is a one-line
    /// send instead of ~100 near-identical methods.
    public void performAction(String selector, MemorySegment sender) {
        if (selector == null) throw new IllegalArgumentException("selector is null");
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel(selector),
                    (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
        } catch (Throwable t) { throw new RuntimeException(selector + " failed", t); }
    }

    /// performTextFinderAction: — route a text-finder action (find panel).
    /// AppKit provides no default (plain NSView does not respond — sending
    /// there raises); implemented by text views. Send only where implemented.
    public void performTextFinderAction(MemorySegment sender) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("performTextFinderAction:"),
                    (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
        } catch (Throwable t) { throw new RuntimeException("performTextFinderAction: failed", t); }
    }

    /// newWindowForTab: — automatic window-tabbing hook (plus button).
    /// An override hook: no stock class implements it. Send only where implemented.
    public void newWindowForTab(MemorySegment sender) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("newWindowForTab:"),
                    (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
        } catch (Throwable t) { throw new RuntimeException("newWindowForTab: failed", t); }
    }

    /// showWritingTools: — writing-tools entry point (macOS 15.2+).
    /// AppKit provides no default (plain NSView does not respond — sending
    /// there raises); implemented by text views. Send only where implemented.
    public void showWritingTools(MemorySegment sender) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, ObjC.sel("showWritingTools:"),
                    (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
        } catch (Throwable t) { throw new RuntimeException("showWritingTools: failed", t); }
    }

    /// validateProposedFirstResponder:forEvent:.
    public boolean validateProposedFirstResponderForEvent(NSResponder responder, NSEvent event) {
        ensureInit();
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("validateProposedFirstResponder:forEvent:"),
                    (MemorySegment) (responder == null ? MemorySegment.NULL : responder.peer()),
                    (MemorySegment) (event == null ? MemorySegment.NULL : event.peer()));
        } catch (Throwable t) { throw new RuntimeException("validateProposedFirstResponder:forEvent: failed", t); }
    }
}
