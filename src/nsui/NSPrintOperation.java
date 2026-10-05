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

    // ---------------------------------------------------------------- nested enums — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSPrintOperation.h
    //   NSPrintingPageOrder: Descending -1, Special 0, Ascending 1, Unknown 2
    //   NSPrintRenderingQuality: Best 0, Responsive 1 (macOS 10.7)
    // Docs: https://developer.apple.com/documentation/appkit/nsprintoperation
    /// `NSPrintingPageOrder` — -1=Descending, 0=Special, 1=Ascending, 2=Unknown.
    public enum PageOrder {
        descending(-1), special(0), ascending(1), unknown(2);
        public final long value;
        PageOrder(long v) { this.value = v; }
        public static PageOrder fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }
    /// `NSPrintRenderingQuality` — 0=Best, 1=Responsive.
    public enum RenderingQuality {
        best(0), responsive(1);
        public final long value;
        RenderingQuality(long v) { this.value = v; }
        public static RenderingQuality fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    // ---- printOperationWithView: (view-only factory) ----
    /// `+printOperationWithView:` — operation with default print info.
    public static NSPrintOperation create(NSView view) {
        ensureInit();
        MemorySegment v = (MemorySegment)(view == null ? MemorySegment.NULL : view.peer());
        try {
            MemorySegment op = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID))
                    .invokeExact(ObjC.cls("NSPrintOperation"), ObjC.sel("printOperationWithView:"), v);
            return wrap(op);
        } catch (Throwable t) {
            throw new RuntimeException("printOperationWithView: failed", t);
        }
    }

    // ---- copyingOperation (readonly, getter isCopyingOperation) ----
    /// isCopyingOperation.
    public boolean isCopyingOperation() {
        ensureInit();
        try { return (boolean) H.hShowsPrintPanel().invokeExact(peer, ObjC.sel("isCopyingOperation")); }
        catch (Throwable t) { throw new RuntimeException("isCopyingOperation failed", t); }
    }

    // ---- preferredRenderingQuality (readonly, macOS 10.7) ----
    /// preferredRenderingQuality.
    public long preferredRenderingQuality() {
        ensureInit();
        try { return (long) ObjC.handle(Sig.of(Ret.INT)).invokeExact(peer, ObjC.sel("preferredRenderingQuality")); }
        catch (Throwable t) { throw new RuntimeException("preferredRenderingQuality failed", t); }
    }
    /// Typed getter.
    public RenderingQuality preferredRenderingQualityEnum() { return RenderingQuality.fromValue(preferredRenderingQuality()); }

    // ---- jobTitle (NSString, macOS 10.5) ----
    /// jobTitle.
    public String jobTitle() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("jobTitle")));
    }
    /// setJobTitle:.
    public void setJobTitle(String title) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setJobTitle:"), title == null ? MemorySegment.NULL : ObjC.nsstring(title));
    }

    // ---- printPanel ----
    /// printPanel.
    public NSPrintPanel printPanel() {
        return NSPrintPanel.wrap(ObjC.msgSendId(peer, ObjC.sel("printPanel")));
    }
    /// setPrintPanel:.
    public void setPrintPanel(NSPrintPanel panel) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPrintPanel:"), (MemorySegment) (panel == null ? MemorySegment.NULL : panel.peer()));
    }

    // ---- PDFPanel (NSPDFPanel* — no wrapper; id peer, macOS 10.9) ----
    /// pdfPanelPeer — raw NSPDFPanel id (may be NULL).
    public MemorySegment pdfPanelPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("PDFPanel"));
    }
    /// setPDFPanel: with raw id.
    public void setPDFPanel(MemorySegment panel) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPDFPanel:"), (MemorySegment) (panel == null ? MemorySegment.NULL : panel));
    }

    // ---- canSpawnSeparateThread ----
    /// canSpawnSeparateThread.
    public boolean canSpawnSeparateThread() {
        return ObjC.msgSendBool(peer, ObjC.sel("canSpawnSeparateThread"));
    }
    /// setCanSpawnSeparateThread:.
    public void setCanSpawnSeparateThread(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setCanSpawnSeparateThread:"), flag);
    }

    // ---- pageOrder (NSPrintingPageOrder long) ----
    /// pageOrder.
    public long pageOrder() {
        return ObjC.msgSendLong(peer, ObjC.sel("pageOrder"));
    }
    /// setPageOrder:.
    public void setPageOrder(long order) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setPageOrder:"), order);
    }
    /// Typed overload.
    public void setPageOrder(PageOrder o) { setPageOrder(o.value); }
    /// Typed getter.
    public PageOrder pageOrderEnum() { return PageOrder.fromValue(pageOrder()); }

    // ---- setPrintInfo: (printInfo getter already exists) ----
    /// setPrintInfo: — replace the operation\u0027s settings.
    public void setPrintInfo(NSPrintInfo info) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPrintInfo:"), (MemorySegment) (info == null ? MemorySegment.NULL : info.peer()));
    }

    // ---- context (readonly NSGraphicsContext*) ----
    /// contextPeer — raw NSGraphicsContext id (valid only while paginating).
    public MemorySegment contextPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("context"));
    }

    // ---- pageRange (readonly NSRange, macOS 10.5) / currentPage ----
    /// pageRange.
    public NSRange pageRange() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) ObjC.handle(Sig.of(Ret.RANGE)).invokeExact(ObjC.structSlot(), peer, ObjC.sel("pageRange"));
            return NSRange.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("pageRange failed", t); }
    }
    /// currentPage.
    public long currentPage() {
        return ObjC.msgSendLong(peer, ObjC.sel("currentPage"));
    }

    // ---- createContext / destroyContext / deliverResult / cleanUpOperation ----
    /// createContext — pagination context (caller must destroy).
    public MemorySegment createContext() {
        return ObjC.msgSendId(peer, ObjC.sel("createContext"));
    }
    /// destroyContext.
    public void destroyContext() {
        ObjC.msgSendVoid(peer, ObjC.sel("destroyContext"));
    }
    /// deliverResult.
    public boolean deliverResult() {
        return ObjC.msgSendBool(peer, ObjC.sel("deliverResult"));
    }
    /// cleanUpOperation.
    public void cleanUpOperation() {
        ObjC.msgSendVoid(peer, ObjC.sel("cleanUpOperation"));
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - +PDFOperationWithView:insideRect:toData:printInfo: / +PDFOperationWithView:insideRect:toPath:printInfo: /
    //   +EPSOperationWithView:insideRect:toData:printInfo: / +EPSOperationWithView:insideRect:toPath:printInfo: /
    //   +PDFOperationWithView:insideRect:toData: / +EPSOperationWithView:insideRect:toData: — omitted: mixed
    //   (id,NSRect,id,id) shapes have no vocabulary entry; grep Sig.java for ID,RECT — no match. Use
    //   printOperationWithView:printInfo: for wiring coverage; runOperation itself is never called in tests.
    // - -runOperationModalForWindow:delegate:didRunSelector:contextInfo: — deliberately out of scope (class
    //   javadoc): needs delegate+SEL+void* plumbing with no registered shape; would block on a modal loop.
    // - -printOperationDidRun:success:contextInfo: — delegate callback, not an NSPrintOperation selector.
    // - Deprecated -setAccessoryView:/-accessoryView/-setJobStyleHint:/-jobStyleHint/-setShowPanels:/-showPanels
    //   (all deprecated 10.0-10.5) — omitted; use NSPrintPanel accessory controllers.
}
