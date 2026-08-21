package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPrintOperation — thin wrapper over native `NSPrintOperation`, the object
/// that binds an `NSView` (what to print) to an `NSPrintInfo` (how to print
/// it) and runs the job. Together with `NSPrintInfo` and `NSPrintPanel` this
/// completes the toolkit's printing surface.
///
/// **Warning — `runOperation` really prints.** With both panels suppressed
/// (`setShowsPrintPanel(false)` and `setShowsProgressPanel(false)`) a call to
/// `runOperation` sends a real job straight to the default printer, with no
/// user confirmation of any kind. With either panel enabled the call instead
/// blocks inside AppKit's modal dialog loop until the user dismisses it.
/// Either way the call is side-effecting and potentially long-lived: never
/// invoke it from automated tests or headless code paths.
///
/// Deliberately out of scope (future work): `runOperationModalForWindow:delegate:didRunSelector:contextInfo:`
/// (needs delegate + selector plumbing that does not exist yet) and page-range
/// setup (`setPageRange:` / `pageRange`). This wrapper stays thin and 1:1 over
/// the selectors it exposes.
public final class NSPrintOperation extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hCreate, MethodHandle hRunOperation, MethodHandle hShowsPrintPanel,
                           MethodHandle hSetShowsPrintPanel, MethodHandle hShowsProgressPanel,
                           MethodHandle hSetShowsProgressPanel) {}
    private static volatile Handles H;

    private NSPrintOperation(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native NSPrintOperation id (null for nil).
    public static NSPrintOperation wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPrintOperation(peer);
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),// printOperationWithView:printInfo:
                ObjC.handle(Sig.of(Ret.BOOL)),              // runOperation
                ObjC.handle(Sig.of(Ret.BOOL)),              // showsPrintPanel
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),    // setShowsPrintPanel:
                ObjC.handle(Sig.of(Ret.BOOL)),              // showsProgressPanel
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)));   // setShowsProgressPanel:
    }

    /// `+printOperationWithView:printInfo:` — a print operation that prints
    /// `view` using the settings in `info`. Either argument may be null, which
    /// passes NULL through to AppKit (only sensible for probing; a real job
    /// needs both).
    public static NSPrintOperation create(NSView view, NSPrintInfo info) {
        ensureInit();
        MemorySegment v = (MemorySegment)(view == null ? MemorySegment.NULL : view.peer());
        MemorySegment i = (MemorySegment)(info == null ? MemorySegment.NULL : info.peer());
        try {
            MemorySegment op = (MemorySegment) H.hCreate().invokeExact(
                    ObjC.cls("NSPrintOperation"), ObjC.sel("printOperationWithView:printInfo:"), v, i);
            return wrap(op);
        } catch (Throwable t) {
            throw new RuntimeException("printOperationWithView:printInfo: failed", t);
        }
    }

    /// `-runOperation` — run the print operation to completion.
    ///
    /// **This sends a real job to the default printer** when the panels are
    /// suppressed (`showsPrintPanel == false && showsProgressPanel == false`)
    /// — paper comes out, no questions asked. When either panel is shown the
    /// call pumps AppKit's modal loop and blocks until the user finishes with
    /// the dialog. Returns true when the operation ran successfully.
    public boolean runOperation() {
        ensureInit();
        try {
            return (boolean) H.hRunOperation().invokeExact(peer, ObjC.sel("runOperation"));
        } catch (Throwable t) {
            throw new RuntimeException("runOperation failed", t);
        }
    }

    /// showsPrintPanel — whether running the operation presents the print
    /// panel (AppKit default: true).
    public boolean showsPrintPanel() {
        ensureInit();
        try {
            return (boolean) H.hShowsPrintPanel().invokeExact(peer, ObjC.sel("showsPrintPanel"));
        } catch (Throwable t) {
            throw new RuntimeException("showsPrintPanel failed", t);
        }
    }

    /// setShowsPrintPanel: — show or suppress the print panel. Suppressing it
    /// (together with the progress panel) makes `runOperation` print without
    /// any user interaction — see the class-level warning.
    public void setShowsPrintPanel(boolean flag) {
        ensureInit();
        try {
            H.hSetShowsPrintPanel().invokeExact(peer, ObjC.sel("setShowsPrintPanel:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setShowsPrintPanel: failed", t);
        }
    }

    /// showsProgressPanel — whether running the operation presents the
    /// progress panel (AppKit default: true).
    public boolean showsProgressPanel() {
        ensureInit();
        try {
            return (boolean) H.hShowsProgressPanel().invokeExact(peer, ObjC.sel("showsProgressPanel"));
        } catch (Throwable t) {
            throw new RuntimeException("showsProgressPanel failed", t);
        }
    }

    /// setShowsProgressPanel: — show or suppress the progress panel.
    public void setShowsProgressPanel(boolean flag) {
        ensureInit();
        try {
            H.hSetShowsProgressPanel().invokeExact(peer, ObjC.sel("setShowsProgressPanel:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setShowsProgressPanel: failed", t);
        }
    }

    /// printInfo — the operation's print settings. AppKit COPIES the
    /// `NSPrintInfo` handed to `create` (documented `initWithView:printInfo:`
    /// behavior), so this returns that copy — a different peer than the
    /// original, carrying the same settings at creation time.
    public NSPrintInfo printInfo() {
        return NSPrintInfo.wrap(ObjC.msgSendId(peer, ObjC.sel("printInfo")));
    }

    /// view — the view this operation prints.
    public NSView view() {
        return NSView.wrap(ObjC.msgSendId(peer, ObjC.sel("view")));
    }

    /// `+currentOperation` — the print operation currently running, or null
    /// when no operation is in flight (always null outside `runOperation`,
    /// which is the only thing that sets it).
    public static NSPrintOperation currentOperation() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("NSPrintOperation"), ObjC.sel("currentOperation")));
    }
}
