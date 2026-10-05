package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPrintInfo — minimal wrapper over native `NSPrintInfo`.
public final class NSPrintInfo extends NSObject {

            private record Handles(MethodHandle hPaperSize, MethodHandle hSetPaperSize) {}
    private static volatile Handles handles;

    private NSPrintInfo(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSPrintInfo wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPrintInfo(peer);
    }

    /// [NSPrintInfo sharedPrintInfo]
    public static NSPrintInfo sharedPrintInfo() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSPrintInfo"), ObjC.sel("sharedPrintInfo"));
        return wrap(s);
    }

    /// [NSPrintInfo defaultPrintInfo] fallback
    public static NSPrintInfo defaultPrintInfo() {
        ensureInit();
        try {
            MemorySegment s = ObjC.msgSendId(ObjC.cls("NSPrintInfo"), ObjC.sel("defaultPrintInfo"));
            if (s != null && s.address() != 0) return wrap(s);
        } catch (Exception ignored) {}
        return sharedPrintInfo();
    }

    /// [[NSPrintInfo alloc] init]
    public static NSPrintInfo create() {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSPrintInfo"), ObjC.sel("alloc"));
        MemorySegment peer = ObjC.msgSendId(alloc, ObjC.sel("init"));
        return wrap(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.SIZE)), ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)));
    }

    /// paperSize
    public NSSize paperSize() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hPaperSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("paperSize"));
            return NSSize.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("paperSize failed", t); }
    }

    /// setPaperSize:
    public void setPaperSize(NSSize size) {
        ensureInit();
        if (size == null) return;
        try { handles.hSetPaperSize().invokeExact(peer, ObjC.sel("setPaperSize:"), size.toSegment()); }
        catch (Throwable t) { throw new RuntimeException("setPaperSize: failed", t); }
    }

    /// orientation — 0 portrait, 1 landscape.
    public long orientation() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("orientation"));
        } catch (Throwable t) { throw new RuntimeException("orientation failed", t); }
    }

    /// setOrientation:
    public void setOrientation(long orientation) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("setOrientation:"), orientation);
        } catch (Throwable t) { throw new RuntimeException("setOrientation: failed", t); }
    }

    /// dictionary — underlying printing dictionary.
    public NSDictionary dictionary() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment d = (MemorySegment) h.invokeExact(peer, ObjC.sel("dictionary"));
            return NSDictionary.wrap(d);
        } catch (Throwable t) { throw new RuntimeException("dictionary failed", t); }
    }

    /// jobDisposition — NSString.
    public String jobDisposition() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment s = (MemorySegment) h.invokeExact(peer, ObjC.sel("jobDisposition"));
            return ObjC.toString(s);
        } catch (Throwable t) { return null; }
    }

    /// setJobDisposition:
    public void setJobDisposition(String disposition) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setJobDisposition:"), (MemorySegment) (disposition == null ? MemorySegment.NULL : ObjC.nsstring(disposition)));
        } catch (Throwable t) { throw new RuntimeException("setJobDisposition: failed", t); }
    }

    // ---------------------------------------------------------------- nested enums — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSPrintInfo.h
    //   NSPaperOrientation: Portrait 0, Landscape 1
    //   NSPrintingPaginationMode: Automatic 0, Fit 1, Clip 2
    // Docs: https://developer.apple.com/documentation/appkit/nsprintinfo
    /// `NSPaperOrientation` — 0=Portrait, 1=Landscape.
    public enum PaperOrientation {
        portrait(0), landscape(1);
        public final long value;
        PaperOrientation(long v) { this.value = v; }
        public static PaperOrientation fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }
    /// `NSPrintingPaginationMode` — 0=Automatic, 1=Fit, 2=Clip.
    public enum PaginationMode {
        automatic(0), fit(1), clip(2);
        public final long value;
        PaginationMode(long v) { this.value = v; }
        public static PaginationMode fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }
    /// Typed orientation getter.
    public PaperOrientation orientationEnum() { return PaperOrientation.fromValue(orientation()); }
    /// Typed orientation setter.
    public void setOrientation(PaperOrientation o) { setOrientation(o.value); }

    // ---- initWithDictionary: ----
    /// initWithDictionary: — designated initializer (nil dictionary allowed).
    public static NSPrintInfo create(MemorySegment attributes) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPrintInfo"), ObjC.sel("alloc"));
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            p = (MemorySegment) h.invokeExact(p, ObjC.sel("initWithDictionary:"), (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
        } catch (Throwable t) { throw new RuntimeException("initWithDictionary: failed", t); }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSPrintInfo initWithDictionary: returned nil");
        return new NSPrintInfo(p);
    }
    /// initWithDictionary: with NSDictionary.
    public static NSPrintInfo create(NSDictionary attributes) {
        return create(attributes == null ? MemorySegment.NULL : attributes.peer());
    }

    // ---- paperName (NSPrinterPaperName NSString) ----
    /// paperName.
    public String paperName() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("paperName")));
    }
    /// setPaperName:.
    public void setPaperName(String name) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPaperName:"), name == null ? MemorySegment.NULL : ObjC.nsstring(name));
    }

    // ---- scalingFactor (CGFloat double, macOS 10.6) ----
    /// scalingFactor.
    public double scalingFactor() {
        ensureInit();
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("scalingFactor")); }
        catch (Throwable t) { throw new RuntimeException("scalingFactor failed", t); }
    }
    /// setScalingFactor:.
    public void setScalingFactor(double factor) {
        ensureInit();
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, ObjC.sel("setScalingFactor:"), factor); }
        catch (Throwable t) { throw new RuntimeException("setScalingFactor: failed", t); }
    }

    // ---- margins (CGFloat doubles) ----
    /// leftMargin.
    public double leftMargin() {
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("leftMargin")); }
        catch (Throwable t) { throw new RuntimeException("leftMargin failed", t); }
    }
    /// setLeftMargin:.
    public void setLeftMargin(double v) {
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, ObjC.sel("setLeftMargin:"), v); }
        catch (Throwable t) { throw new RuntimeException("setLeftMargin: failed", t); }
    }
    /// rightMargin.
    public double rightMargin() {
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("rightMargin")); }
        catch (Throwable t) { throw new RuntimeException("rightMargin failed", t); }
    }
    /// setRightMargin:.
    public void setRightMargin(double v) {
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, ObjC.sel("setRightMargin:"), v); }
        catch (Throwable t) { throw new RuntimeException("setRightMargin: failed", t); }
    }
    /// topMargin.
    public double topMargin() {
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("topMargin")); }
        catch (Throwable t) { throw new RuntimeException("topMargin failed", t); }
    }
    /// setTopMargin:.
    public void setTopMargin(double v) {
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, ObjC.sel("setTopMargin:"), v); }
        catch (Throwable t) { throw new RuntimeException("setTopMargin: failed", t); }
    }
    /// bottomMargin.
    public double bottomMargin() {
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("bottomMargin")); }
        catch (Throwable t) { throw new RuntimeException("bottomMargin failed", t); }
    }
    /// setBottomMargin:.
    public void setBottomMargin(double v) {
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, ObjC.sel("setBottomMargin:"), v); }
        catch (Throwable t) { throw new RuntimeException("setBottomMargin: failed", t); }
    }

    // ---- centered (getters isHorizontallyCentered / isVerticallyCentered) ----
    /// isHorizontallyCentered.
    public boolean isHorizontallyCentered() {
        return ObjC.msgSendBool(peer, ObjC.sel("isHorizontallyCentered"));
    }
    /// setHorizontallyCentered:.
    public void setHorizontallyCentered(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setHorizontallyCentered:"), flag);
    }
    /// isVerticallyCentered.
    public boolean isVerticallyCentered() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVerticallyCentered"));
    }
    /// setVerticallyCentered:.
    public void setVerticallyCentered(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setVerticallyCentered:"), flag);
    }

    // ---- pagination (NSPrintingPaginationMode long) ----
    /// horizontalPagination.
    public long horizontalPagination() {
        return ObjC.msgSendLong(peer, ObjC.sel("horizontalPagination"));
    }
    /// setHorizontalPagination:.
    public void setHorizontalPagination(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setHorizontalPagination:"), mode);
    }
    /// Typed overload.
    public void setHorizontalPagination(PaginationMode m) { setHorizontalPagination(m.value); }
    /// Typed getter.
    public PaginationMode horizontalPaginationEnum() { return PaginationMode.fromValue(horizontalPagination()); }
    /// verticalPagination.
    public long verticalPagination() {
        return ObjC.msgSendLong(peer, ObjC.sel("verticalPagination"));
    }
    /// setVerticalPagination:.
    public void setVerticalPagination(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setVerticalPagination:"), mode);
    }
    /// Typed overload.
    public void setVerticalPagination(PaginationMode m) { setVerticalPagination(m.value); }
    /// Typed getter.
    public PaginationMode verticalPaginationEnum() { return PaginationMode.fromValue(verticalPagination()); }

    // ---- printer (NSPrinter* — no wrapper; id peer) ----
    /// printerPeer — raw NSPrinter id (may be NULL).
    public MemorySegment printerPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("printer"));
    }
    /// setPrinter: with raw id.
    public void setPrinter(MemorySegment printer) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPrinter:"), (MemorySegment) (printer == null ? MemorySegment.NULL : printer));
    }

    // ---- setUpPrintOperationDefaultValues ----
    /// setUpPrintOperationDefaultValues.
    public void setUpPrintOperationDefaultValues() {
        ObjC.msgSendVoid(peer, ObjC.sel("setUpPrintOperationDefaultValues"));
    }

    // ---- imageablePageBounds (readonly NSRect) ----
    /// imageablePageBounds.
    public NSRect imageablePageBounds() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) ObjC.handle(Sig.of(Ret.RECT)).invokeExact(ObjC.structSlot(), peer, ObjC.sel("imageablePageBounds"));
            return NSRect.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("imageablePageBounds failed", t); }
    }

    // ---- localizedPaperName (readonly NSString) ----
    /// localizedPaperName.
    public String localizedPaperName() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("localizedPaperName")));
    }

    // ---- defaultPrinter (class readonly NSPrinter*) ----
    /// defaultPrinterPeer — raw NSPrinter id (may be NULL headless).
    public static MemorySegment defaultPrinterPeer() {
        ensureInit();
        return ObjC.msgSendId(ObjC.cls("NSPrintInfo"), ObjC.sel("defaultPrinter"));
    }

    // ---- printSettings (readonly NSMutableDictionary) ----
    /// printSettings — mutable settings dictionary.
    public NSDictionary printSettings() {
        return NSDictionary.wrap(ObjC.msgSendId(peer, ObjC.sel("printSettings")));
    }

    // ---- PM session/format/settings (void* inner pointers as id) ----
    /// PMPrintSessionPeer — inner PMPrintSession pointer (do not free).
    public MemorySegment pmPrintSessionPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("PMPrintSession"));
    }
    /// PMPageFormatPeer — inner PMPageFormat pointer (do not free).
    public MemorySegment pmPageFormatPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("PMPageFormat"));
    }
    /// PMPrintSettingsPeer — inner PMPrintSettings pointer (do not free).
    public MemorySegment pmPrintSettingsPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("PMPrintSettings"));
    }
    /// updateFromPMPageFormat.
    public void updateFromPMPageFormat() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateFromPMPageFormat"));
    }
    /// updateFromPMPrintSettings.
    public void updateFromPMPrintSettings() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateFromPMPrintSettings"));
    }

    // ---- selectionOnly (macOS 10.6, getter isSelectionOnly) ----
    /// isSelectionOnly.
    public boolean isSelectionOnly() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSelectionOnly"));
    }
    /// setSelectionOnly:.
    public void setSelectionOnly(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setSelectionOnly:"), flag);
    }

    // ---- takeSettingsFromPDFInfo: (NSPDFInfo* as id) ----
    /// takeSettingsFromPDFInfo: with raw id.
    public void takeSettingsFromPDFInfo(MemorySegment pdfInfo) {
        ObjC.msgSendVoidId(peer, ObjC.sel("takeSettingsFromPDFInfo:"), (MemorySegment) (pdfInfo == null ? MemorySegment.NULL : pdfInfo));
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - -initWithCoder: (NSCoding) — omitted: NSCoder plumbing out of scope for this batch.
    // - +setDefaultPrinter: / +sizeForPaperName: (both deprecated 10.0-10.2, no-effect/redirect) — omitted.
    // - NSPrinter/NSPDFInfo typed wrappers — no NSPrinter.java / NSPDFInfo.java in this repo; raw peers used.
}
