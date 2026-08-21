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

    private static synchronized void ensureInit() {
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
}
