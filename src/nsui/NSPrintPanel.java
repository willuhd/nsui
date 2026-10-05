package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPrintPanel — minimal wrapper over native `NSPrintPanel`.
public final class NSPrintPanel extends NSObject {

            private record Handles(MethodHandle hRunModal) {}
    private static volatile Handles handles;

    private NSPrintPanel(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSPrintPanel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPrintPanel(peer);
    }

    /// [NSPrintPanel printPanel]
    public static NSPrintPanel printPanel() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSPrintPanel"), ObjC.sel("printPanel"));
        return wrap(s);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.INT)));
    }

    /// runModal — returns NSApplication.ModalResponse.
    public long runModal() {
        ensureInit();
        try { return (long) handles.hRunModal().invokeExact(peer, ObjC.sel("runModal")); }
        catch (Throwable t) { throw new RuntimeException("runModal failed", t); }
    }

    /// runModalWithPrintInfo:
    public long runModalWithPrintInfo(NSPrintInfo printInfo) {
        ensureInit();
        if (printInfo == null) return runModal();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("runModalWithPrintInfo:"), printInfo.peer());
        } catch (Throwable t) { throw new RuntimeException("runModalWithPrintInfo: failed", t); }
    }

    /// beginSheetWithPrintInfo:modalForWindow:delegate:didEndSelector:contextInfo: — deprecated sheet variant.
    /// Kept for source compatibility; routes via the object-only escape hatch (no unregistered shape).
    /// Never call from tests (starts a modal sheet loop).
    public void beginSheetWithPrintInfo(NSPrintInfo printInfo, NSWindow window, NSObject delegate, String didEndSelector, MemorySegment contextInfo) {
        ensureInit();
        try {
            // 5 object-class args (SEL and void* both pass as pointers) -> escape hatch void (6-id shape).
            // Grep-before-use: Sig.java has of(VOID, ID x6) (escape hatch) — verified; the exact 5-arg shape
            // of(VOID, ID, ID, ID, ID, ID) is NOT in the vocabulary, so the escape hatch is required here.
            MemorySegment sel = didEndSelector == null ? MemorySegment.NULL : ObjC.sel(didEndSelector);
            ObjC.invokeVoid(peer, ObjC.sel("beginSheetWithPrintInfo:modalForWindow:delegate:didEndSelector:contextInfo:"),
                    printInfo == null ? MemorySegment.NULL : printInfo.peer(),
                    window == null ? MemorySegment.NULL : window.peer(),
                    delegate == null ? MemorySegment.NULL : delegate.peer(),
                    sel,
                    contextInfo == null ? MemorySegment.NULL : contextInfo);
        } catch (Throwable t) { throw new RuntimeException("beginSheetWithPrintInfo: failed", t); }
    }

    /// options — NSPrintPanelOptions bitfield.
    public long options() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("options"));
        } catch (Throwable t) { throw new RuntimeException("options failed", t); }
    }

    public void setOptions(long options) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("setOptions:"), options);
        } catch (Throwable t) { throw new RuntimeException("setOptions: failed", t); }
    }

    // ---------------------------------------------------------------- nested types — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSPrintPanel.h
    //   NSPrintPanelResult: Cancelled 0, Printed 1 (macOS 14)
    //   NSPrintPanelOptions: ShowsCopies 1<<0, ShowsPageRange 1<<1, ShowsPaperSize 1<<2,
    //     ShowsOrientation 1<<3, ShowsScaling 1<<4, ShowsPrintSelection 1<<5 (10.6),
    //     ShowsPageSetupAccessory 1<<8, ShowsPreview 1<<17
    // Docs: https://developer.apple.com/documentation/appkit/nsprintpanel
    /// `NSPrintPanelResult` — 0=Cancelled, 1=Printed.
    public enum Result {
        cancelled(0), printed(1);
        public final long value;
        Result(long v) { this.value = v; }
        public static Result fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }
    /// `NSPrintPanelOptions` bitfield constants.
    public static final long SHOWS_COPIES = 1L << 0;
    public static final long SHOWS_PAGE_RANGE = 1L << 1;
    public static final long SHOWS_PAPER_SIZE = 1L << 2;
    public static final long SHOWS_ORIENTATION = 1L << 3;
    public static final long SHOWS_SCALING = 1L << 4;
    public static final long SHOWS_PRINT_SELECTION = 1L << 5;
    public static final long SHOWS_PAGE_SETUP_ACCESSORY = 1L << 8;
    public static final long SHOWS_PREVIEW = 1L << 17;
    /// `NSPrintPanelJobStyleHint` stock values (NSString).
    public static final String PHOTO_JOB_STYLE_HINT = "NSPrintPhotoJobStyleHint";

    // ---- accessory controllers (macOS 10.5; NSViewController<NSPrintPanelAccessorizing>) ----
    /// addAccessoryController:.
    public void addAccessoryController(NSViewController controller) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addAccessoryController:"), (MemorySegment) (controller == null ? MemorySegment.NULL : controller.peer()));
    }
    /// addAccessoryController: with raw id.
    public void addAccessoryController(MemorySegment controller) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addAccessoryController:"), (MemorySegment) (controller == null ? MemorySegment.NULL : controller));
    }
    /// removeAccessoryController:.
    public void removeAccessoryController(NSViewController controller) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeAccessoryController:"), (MemorySegment) (controller == null ? MemorySegment.NULL : controller.peer()));
    }
    /// removeAccessoryController: with raw id.
    public void removeAccessoryController(MemorySegment controller) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeAccessoryController:"), (MemorySegment) (controller == null ? MemorySegment.NULL : controller));
    }
    /// accessoryControllersPeer — raw NSArray id.
    public MemorySegment accessoryControllersPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("accessoryControllers"));
    }
    /// accessoryControllers — wrapped controllers (may be empty).
    public java.util.List<NSViewController> accessoryControllers() {
        MemorySegment arr = accessoryControllersPeer();
        if (arr == null || arr.address() == 0) return java.util.List.of();
        long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
        java.util.List<NSViewController> out = new java.util.ArrayList<>((int) count);
        try {
            MethodHandle hAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            for (long i = 0; i < count; i++) {
                MemorySegment c = (MemorySegment) hAt.invokeExact(arr, ObjC.sel("objectAtIndex:"), i);
                NSViewController w = NSViewController.wrap(c);
                if (w != null) out.add(w);
            }
        } catch (Throwable t) { throw new RuntimeException("accessoryControllers failed", t); }
        return java.util.Collections.unmodifiableList(out);
    }

    // ---- defaultButtonTitle (NSString, macOS 10.5) ----
    /// defaultButtonTitle.
    public String defaultButtonTitle() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("defaultButtonTitle")));
    }
    /// setDefaultButtonTitle:.
    public void setDefaultButtonTitle(String title) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDefaultButtonTitle:"), title == null ? MemorySegment.NULL : ObjC.nsstring(title));
    }

    // ---- helpAnchor (NSHelpAnchorName, macOS 10.5) ----
    /// helpAnchor.
    public String helpAnchor() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("helpAnchor")));
    }
    /// setHelpAnchor:.
    public void setHelpAnchor(String anchor) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setHelpAnchor:"), anchor == null ? MemorySegment.NULL : ObjC.nsstring(anchor));
    }

    // ---- jobStyleHint (NSPrintPanelJobStyleHint NSString) ----
    /// jobStyleHint.
    public String jobStyleHint() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("jobStyleHint")));
    }
    /// setJobStyleHint:.
    public void setJobStyleHint(String hint) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setJobStyleHint:"), hint == null ? MemorySegment.NULL : ObjC.nsstring(hint));
    }

    // ---- beginSheetUsingPrintInfo:onWindow:completionHandler: (macOS 14, block as id) ----
    /// beginSheetUsingPrintInfo:onWindow:completionHandler: — sheet with block handler (NULL probes shape).
    /// Never call from tests (starts a sheet loop).
    public void beginSheetUsingPrintInfo(NSPrintInfo printInfo, NSWindow window, MemorySegment handler) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("beginSheetUsingPrintInfo:onWindow:completionHandler:"),
                    (MemorySegment) (printInfo == null ? MemorySegment.NULL : printInfo.peer()),
                    (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()),
                    (MemorySegment) (handler == null ? MemorySegment.NULL : handler));
        } catch (Throwable t) { throw new RuntimeException("beginSheetUsingPrintInfo:onWindow:completionHandler: failed", t); }
    }

    // ---- printInfo (readonly, macOS 10.5) ----
    /// printInfo — panel\u0027s print settings.
    public NSPrintInfo printInfo() {
        return NSPrintInfo.wrap(ObjC.msgSendId(peer, ObjC.sel("printInfo")));
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - -localizedSummaryItems / -keyPathsForValuesAffectingPreview — NSPrintPanelAccessorizing PROTOCOL methods
    //   (on accessory controllers, not on NSPrintPanel itself); an earlier draft wrapped them on the panel and
    //   crashed with NSInvalidArgumentException (unrecognized selector). Wire them on accessory controllers via
    //   DelegateProxy IdArg shapes if needed; the panel side (add/remove/accessoryControllers) is kept above.
    // - -printPanelDidEnd:returnCode:contextInfo: — delegate callback, not an NSPrintPanel selector.
    // - Deprecated -setAccessoryView:/-accessoryView/-updateFromPrintInfo/-finalWritePrintInfo (all deprecated
    //   10.0-10.5) — omitted; use addAccessoryController:/removeAccessoryController:/accessoryControllers.
    // - Deprecated -beginSheetWithPrintInfo:modalForWindow:delegate:didEndSelector:contextInfo: is KEPT above
    //   for source compatibility (escape-hatch routing) but never called from tests.
}
