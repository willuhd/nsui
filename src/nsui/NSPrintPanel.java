package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPrintPanel — minimal wrapper over native `NSPrintPanel`.
public final class NSPrintPanel extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hRunModal; // (id, SEL) -> long

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
        if (initialized) return;
        hRunModal = ObjC.handle(Sig.of(Ret.INT));
        initialized = true;
    }

    /// runModal — returns NSApplication.ModalResponse.
    public long runModal() {
        ensureInit();
        try { return (long) hRunModal.invokeExact(peer, ObjC.sel("runModal")); }
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

    /// beginSheetWithPrintInfo:modalForWindow:delegate:didEndSelector:contextInfo: — minimal sheet variant.
    public void beginSheetWithPrintInfo(NSPrintInfo printInfo, NSWindow window, NSObject delegate, String didEndSelector, MemorySegment contextInfo) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID, Arg.ID));
            // selector string -> SEL
            MemorySegment sel = didEndSelector == null ? MemorySegment.NULL : ObjC.sel(didEndSelector);
            // This is 4 object args + SEL is already separate? Actually signature is (id,SEL, id, id, id, SEL, void*) -> use escape? For minimal, use invoke with 5 args.
            // Fallback to generic invoke for complex signature.
            ObjC.invoke(peer, ObjC.sel("beginSheetWithPrintInfo:modalForWindow:delegate:didEndSelector:contextInfo:"),
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
}
