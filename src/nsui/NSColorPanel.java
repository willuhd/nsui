package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSColorPanel — the system Color panel.
/// Thin 1:1 wrapper over native `NSColorPanel` (an NSPanel subclass).
public final class NSColorPanel extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hGetId;   // (id, SEL) -> id
    private static MethodHandle hGetBool; // (id, SEL) -> bool
    private static MethodHandle hSetBool; // (id, SEL, bool) -> void
    private static MethodHandle hGetInt;  // (id, SEL) -> long
    private static MethodHandle hSetInt;  // (id, SEL, long) -> void

    private NSColorPanel(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSColorPanel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSColorPanel(peer);
    }

    private static void ensureInit() {
        if (initialized) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (initialized) return;
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hGetBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hGetInt = ObjC.handle(Sig.of(Ret.INT));
        hSetInt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        initialized = true;
    }

    // ---- shared ----

    /// `+[NSColorPanel sharedColorPanel]`
    public static NSColorPanel sharedColorPanel() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSColorPanel"), ObjC.sel("sharedColorPanel"));
        return wrap(p);
    }

    /// `+[NSColorPanel sharedColorPanelExists]`
    public static boolean sharedColorPanelExists() {
        ensureInit();
        try {
            return (boolean) hGetBool.invokeExact(ObjC.cls("NSColorPanel"), ObjC.sel("sharedColorPanelExists"));
        } catch (Throwable t) {
            throw new RuntimeException("sharedColorPanelExists failed", t);
        }
    }

    // ---- color ----

    /// [panel color] -> NSColor
    public NSColor color() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("color"));
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("color failed", t);
        }
    }

    /// [panel setColor:]
    public void setColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// [panel showsAlpha]
    public boolean showsAlpha() {
        ensureInit();
        try { return (boolean) hGetBool.invokeExact(peer, ObjC.sel("showsAlpha")); } catch (Throwable t) { throw new RuntimeException("showsAlpha failed", t); }
    }
    public void setShowsAlpha(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setShowsAlpha:"), flag); } catch (Throwable t) { throw new RuntimeException("setShowsAlpha: failed", t); }
    }

    /// [panel isContinuous]
    public boolean isContinuous() {
        ensureInit();
        try { return (boolean) hGetBool.invokeExact(peer, ObjC.sel("isContinuous")); } catch (Throwable t) { throw new RuntimeException("isContinuous failed", t); }
    }
    public void setContinuous(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setContinuous:"), flag); } catch (Throwable t) { throw new RuntimeException("setContinuous: failed", t); }
    }

    // ---- mode ----

    /// [panel mode] -> long (NSColorPanelMode)
    public long mode() {
        ensureInit();
        try { return (long) hGetInt.invokeExact(peer, ObjC.sel("mode")); } catch (Throwable t) { throw new RuntimeException("mode failed", t); }
    }
    public void setMode(long mode) {
        ensureInit();
        try { hSetInt.invokeExact(peer, ObjC.sel("setMode:"), mode); } catch (Throwable t) { throw new RuntimeException("setMode: failed", t); }
    }

    // ---- visibility ----

    /// [panel isVisible]
    public boolean isVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVisible"));
    }

    /// [panel orderFront:]
    public void orderFront(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [panel orderOut:]
    public void orderOut(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderOut:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [panel setAction:] / setTarget: — color well target wiring
    public void setTarget(MemorySegment target) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTarget:"), (MemorySegment) (target == null ? MemorySegment.NULL : target));
    }
    public void setAction(String action) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"), ObjC.sel(action));
    }

    /// [panel accessoryView]
    public NSView accessoryView() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("accessoryView"));
            return NSView.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("accessoryView failed", t); }
    }
    public void setAccessoryView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAccessoryView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
    }

    /// [panel attachColorList:]
    public void attachColorList(MemorySegment colorList) {
        ObjC.msgSendVoidId(peer, ObjC.sel("attachColorList:"), (MemorySegment) (colorList == null ? MemorySegment.NULL : colorList));
    }

    /// [panel detachColorList:]
    public void detachColorList(MemorySegment colorList) {
        ObjC.msgSendVoidId(peer, ObjC.sel("detachColorList:"), (MemorySegment) (colorList == null ? MemorySegment.NULL : colorList));
    }

    // ---- attach/detach with NSColorList ----
    /// attachColorList: with NSColorList.
    public void attachColorList(NSColorList colorList) {
        attachColorList(colorList == null ? MemorySegment.NULL : colorList.peer());
    }
    /// detachColorList: with NSColorList.
    public void detachColorList(NSColorList colorList) {
        detachColorList(colorList == null ? MemorySegment.NULL : colorList.peer());
    }

    // ---------------------------------------------------------------- nested types — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSColorPanel.h
    //   NSColorPanelMode: None -1, Gray 0, RGB 1, CMYK 2, HSB 3, CustomPalette 4, ColorList 5, Wheel 6, Crayon 7
    //   NSColorPanelOptions: Gray 0x1, RGB 0x2, CMYK 0x4, HSB 0x8, CustomPalette 0x10, ColorList 0x20,
    //     Wheel 0x40, Crayon 0x80, AllModes 0xffff
    // Docs: https://developer.apple.com/documentation/appkit/nscolorpanel
    /// `NSColorPanelMode` — -1=None, 0=Gray, 1=RGB, 2=CMYK, 3=HSB, 4=CustomPalette, 5=ColorList, 6=Wheel, 7=Crayon.
    public enum Mode {
        none(-1), gray(0), rgb(1), cmyk(2), hsb(3), customPalette(4), colorList(5), wheel(6), crayon(7);
        public final long value;
        Mode(long v) { this.value = v; }
        public static Mode fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }
    /// Typed mode getter.
    public Mode modeEnum() { return Mode.fromValue(mode()); }
    /// Typed mode setter.
    public void setMode(Mode m) { setMode(m.value); }
    /// `NSColorPanelOptions` mask bits.
    public static final long GRAY_MODE_MASK = 0x1L;
    public static final long RGB_MODE_MASK = 0x2L;
    public static final long CMYK_MODE_MASK = 0x4L;
    public static final long HSB_MODE_MASK = 0x8L;
    public static final long CUSTOM_PALETTE_MODE_MASK = 0x10L;
    public static final long COLOR_LIST_MODE_MASK = 0x20L;
    public static final long WHEEL_MODE_MASK = 0x40L;
    public static final long CRAYON_MODE_MASK = 0x80L;
    public static final long ALL_MODES_MASK = 0xffffL;

    // ---- picker mask/mode (class methods) ----
    /// +setPickerMask: — restrict available pickers (bitwise OR of *_MODE_MASK).
    public static void setPickerMask(long mask) {
        ensureInit();
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.INT)).invokeExact(ObjC.cls("NSColorPanel"), ObjC.sel("setPickerMask:"), mask); }
        catch (Throwable t) { throw new RuntimeException("setPickerMask: failed", t); }
    }
    /// +setPickerMode: — select the active picker.
    public static void setPickerMode(long mode) {
        ensureInit();
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.INT)).invokeExact(ObjC.cls("NSColorPanel"), ObjC.sel("setPickerMode:"), mode); }
        catch (Throwable t) { throw new RuntimeException("setPickerMode: failed", t); }
    }
    /// Typed overload.
    public static void setPickerMode(Mode m) { setPickerMode(m.value); }

    // ---- alpha (readonly CGFloat) ----
    /// alpha.
    public double alpha() {
        ensureInit();
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("alpha")); }
        catch (Throwable t) { throw new RuntimeException("alpha failed", t); }
    }

    // ---- maximumLinearExposure (CGFloat, macOS 26) ----
    /// maximumLinearExposure.
    public double maximumLinearExposure() {
        ensureInit();
        try { return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(peer, ObjC.sel("maximumLinearExposure")); }
        catch (Throwable t) { throw new RuntimeException("maximumLinearExposure failed", t); }
    }
    /// setMaximumLinearExposure:.
    public void setMaximumLinearExposure(double v) {
        ensureInit();
        try { ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)).invokeExact(peer, ObjC.sel("setMaximumLinearExposure:"), v); }
        catch (Throwable t) { throw new RuntimeException("setMaximumLinearExposure: failed", t); }
    }

    // ---- orderFrontColorPanel: (IBAction) ----
    /// orderFrontColorPanel:. Header-declared instance method, but the shared panel on this OS does not
    /// respond to it (verified: respondsToSelector returns NO while orderFront: returns YES); guarded so a
    /// call degrades to a no-op instead of a fatal NSInvalidArgumentException. Never call from tests.
    public void orderFrontColorPanel(MemorySegment sender) {
        ensureInit();
        boolean responds = false;
        try {
            responds = (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID))
                    .invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("orderFrontColorPanel:"));
        } catch (Throwable ignore) { responds = false; }
        if (!responds) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontColorPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    /// orderFrontColorPanel: with NSObject sender.
    public void orderFrontColorPanel(NSObject sender) {
        orderFrontColorPanel(sender == null ? MemorySegment.NULL : sender.peer());
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - +dragColor:withEvent:fromView: — omitted: (id,id,id)->BOOL has no vocabulary entry; grep Sig.java
    //   for BOOL,ID,ID,ID — no match (only BOOL,ID,ID and BOOL,ID,POINT,ID). Drag-source plumbing out of scope.
    // - -changeColor: (both overloads) — responder-side NSColorChanging action, not an NSColorPanel selector;
    //   wire via Target-Action on the panel\u0027s target/action instead.
    // - Deprecated NS*ModeColorPanel constants (e.g. NSGrayModeColorPanel) — omitted; use Mode enum above.
}
