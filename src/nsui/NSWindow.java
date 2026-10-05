package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSWindow — a native window: frame, title, visibility, key/main status, style,
/// delegate, close. Thin 1:1 wrapper; all behavior is AppKit's.
///
/// **The compositional style model.** An AppKit window's "*type*" is never a
/// named constant — it is the composition of a `styleMask` bit-field, the
/// concrete subclass (`NSWindow` vs `NSPanel`), and a handful of
/// behavior properties. There is **no settable title-bar height and no settable
/// corner radius** on the native side: those derive from the style you choose,
/// not from numbers you pass. If you need a given look, pick the style bits + panel
/// subclass that AppKit maps to it, then adjust the behavior booleans.
///
/// **`styleMask` bits** (from `NSWindowStyleMask`, macOS 15 SDK):
/// - `1`     `NSWindowStyleMaskTitled`
/// - `2`     `NSWindowStyleMaskClosable`
/// - `4`     `NSWindowStyleMaskMiniaturizable`
/// - `8`     `NSWindowStyleMaskResizable`
/// - `16`    `NSWindowStyleMaskUtilityWindow` — see `createPanel`
/// - `128`   `NSWindowStyleMaskNonactivatingPanel` — never activates the app
/// - `32768` `NSWindowStyleMaskFullSizeContentView` — content extends under the title bar
/// OR the bits together (e.g. `1|2|4|8` = a standard titled, closable,
/// miniaturizable, resizable document window).
///
/// **`NSPanel`.** `createPanel` builds an `NSPanel`
/// subclass using the very same
/// `initWithContentRect:styleMask:backing:defer:` initializer as
/// `create`. Combined with `NSWindowStyleMaskUtilityWindow` (16) you
/// get AppKit's smaller-title-bar, less-rounded "settings / utility" panel. Panels
/// differ from windows in a few respects exposed here: `setHidesOnDeactivate`
/// (a real `NSPanel` behavior via `hidesOnDeactivate`) and
/// `setBecomesKeyOnlyIfNeeded` (a panel stays key only while controls need
/// it), plus the AppKit default that panels are excluded from the Window menu.
/// `isUtilityWindow` reports whether the receiver is a utility panel:
/// `true` for `NSPanel` instances or windows whose styleMask includes
/// `NSWindowStyleMaskUtilityWindow`.
///
/// **Title bar transparency & visibility.**
/// `setTitlebarAppearsTransparent` and `setTitleVisibility` are the
/// AppKit-native "modern title bar" switches — the translucent/floating-header look
/// modern apps use. There is no height/radius knob; the appearance comes from the
/// style + these flags + whatever the content view draws behind the bar.
///
/// NOTE: NSWindow has NO bare `setFrame:` — that is an NSView selector;
/// windows use `setFrame:display:` and `setFrameOrigin:`.
///
/// NSWindow extends NSResponder — mirroring AppKit, where NSWindow IS an
/// NSResponder and terminates the view-side responder chain — so windows gain
/// `nextResponder`/`setNextResponder`, the event pass-throughs, and
/// `touchBar()`/`setTouchBar` from one shared implementation.
public class NSWindow extends NSResponder {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hSetFrameDisplay, MethodHandle hSetFrameOrigin, MethodHandle hSetContentSize, MethodHandle hStdWinButton, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetSize, MethodHandle hSetSize, MethodHandle hRectRect, MethodHandle hGetPoint, MethodHandle hDoubleInt, MethodHandle hVoidDoubleInt, MethodHandle hBoolInt, MethodHandle hVoidBoolInt, MethodHandle hVoidIdInt, MethodHandle hVoidIntInt, MethodHandle hIdInt, MethodHandle hIdIntInt, MethodHandle hBoolIdId, MethodHandle hIdIdId, MethodHandle hVoidIntId, MethodHandle hIdIntIdIdBool, MethodHandle hVoidIdBool, MethodHandle hBoolIdBool, MethodHandle hBoolId, MethodHandle hIdRect) {}
    private static volatile Handles H;

    protected NSWindow(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSWindow wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSWindow(peer);
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.RECT, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.DOUBLE, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE, Arg.INT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID, Arg.ID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT))); 
    }

    /// alloc + initWithContentRect:styleMask:backing:defer:.
    public static NSWindow create(NSRect contentRect, long styleMask, long backingStoreType, boolean defer) {
        MemorySegment win = ObjC.msgSendId(ObjC.cls("NSWindow"), ObjC.sel("alloc"));
        win = ObjC.msgSendIdRectLongLongBool(win, ObjC.sel("initWithContentRect:styleMask:backing:defer:"),
                contentRect.toSegment(), styleMask, backingStoreType, defer);
        return new NSWindow(win);
    }

    /// Create an `NSPanel` with the SAME
    /// `initWithContentRect:styleMask:backing:defer:` initializer as
    /// `create`, but from the `NSPanel` subclass. Add
    /// `NSWindowStyleMaskUtilityWindow` (16) to `styleMask` (e.g.
    /// `15L | 16L`) to get AppKit's smaller-title-bar, less-rounded
    /// "settings / utility" panel. Panels also support the behavior properties
    /// `setHidesOnDeactivate` and `setBecomesKeyOnlyIfNeeded`.
    public static NSWindow createPanel(NSRect contentRect, long styleMask, long backingStoreType, boolean defer) {
        MemorySegment panel = ObjC.msgSendId(ObjC.cls("NSPanel"), ObjC.sel("alloc"));
        panel = ObjC.msgSendIdRectLongLongBool(panel, ObjC.sel("initWithContentRect:styleMask:backing:defer:"),
                contentRect.toSegment(), styleMask, backingStoreType, defer);
        return new NSWindow(panel);
    }

    public void setTitle(String title) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTitle:"), ObjC.nsstring(title));
    }

    public void center() {
        ObjC.msgSendVoid(peer, ObjC.sel("center"));
    }

    public void setReleasedWhenClosed(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setReleasedWhenClosed:"), flag);
    }

    /// [window isReleasedWhenClosed]
    public boolean isReleasedWhenClosed() {
        return ObjC.msgSendBool(peer, ObjC.sel("isReleasedWhenClosed"));
    }

    /// setContentView: replaces the window's root content view.
    public void setContentView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setContentView:"), view.peer());
    }

    public void setDelegate(NSObject delegate) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"), delegate.peer());
    }

    /// [window delegate]
    public MemorySegment delegate() {
        return ObjC.msgSendId(peer, ObjC.sel("delegate"));
    }

    /// Typed delegate.
    public NSObject delegateObject() {
        return NSObject.wrap(ObjC.msgSendId(peer, ObjC.sel("delegate")));
    }

    /// Struct-returning message: frame (objc_msgSend_stret on x86_64).
    public NSRect frame() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("frame")));
    }

    /// The vertical offset between WINDOW base coordinates and CONTENT coordinates:
    /// `event.locationInWindow().y` is measured from the window FRAME's
    /// bottom-left (title bar included), while a content view's local origin sits
    /// above the title bar — so the conversion is `viewY = windowY - offset`.
    /// Equals the title-bar height (+ borders); x maps 1:1.
    public double contentOriginOffsetY() {
        MemorySegment cv = ObjC.msgSendId(peer, ObjC.sel("contentView"));
        // Read the slot BEFORE frame(): struct returns share one per-thread
        // slot, so the second call would overwrite cb unread.
        double boundsH = ObjC.rectH(ObjC.msgSendRect(cv, ObjC.sel("bounds")));
        return frame().height() - boundsH;
    }

    /// setFrame:display: — resize/reposition (and optionally redraw immediately).
    public void setFrameDisplay(NSRect frame, boolean display) {
        try {
            H.hSetFrameDisplay().invokeExact(peer, ObjC.sel("setFrame:display:"), frame.toSegment(), display);
        } catch (Throwable t) {
            throw new RuntimeException("setFrame:display: failed", t);
        }
    }

    /// setFrameOrigin: — move the window (fires windowDidMove:).
    public void setFrameOrigin(NSPoint origin) {
        try {
            H.hSetFrameOrigin().invokeExact(peer, ObjC.sel("setFrameOrigin:"), origin.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setFrameOrigin: failed", t);
        }
    }

    /// setContentSize: — the content area's size.
    public void setContentSize(NSSize size) {
        try {
            H.hSetContentSize().invokeExact(peer, ObjC.sel("setContentSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentSize: failed", t);
        }
    }

    // ---------------------------------------------------------------- nested enums — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSWindow.h
    //   NSWindowStyleMask (NS_OPTIONS): grep -A 15 "typedef NS_OPTIONS.*NSWindowStyleMask"
    // Docs: https://developer.apple.com/documentation/appkit/nswindow/stylemask
    // Docs: https://developer.apple.com/documentation/appkit/nswindowtitlevisibility
    // Docs: https://developer.apple.com/documentation/appkit/nswindow/level
    // Docs: https://developer.apple.com/documentation/appkit/nswindow/backingtype (NSGraphics.h)
    // Docs: https://developer.apple.com/documentation/appkit/nswindow/tabbingmode

    /// `NSWindowStyleMask` — bitmask compositional style. Values from `NSWindow.h`.
    /// Source: `NSWindow.h` `typedef NS_OPTIONS(NSUInteger, NSWindowStyleMask)` (SDK MacOSX.sdk)
    /// and https://developer.apple.com/documentation/appkit/nswindow/stylemask
    public enum StyleMask {
        borderless(0),
        titled(1L << 0),
        closable(1L << 1),
        miniaturizable(1L << 2),
        resizable(1L << 3),
        utilityWindow(1L << 4),
        docModalWindow(1L << 6),
        nonactivatingPanel(1L << 7),
        texturedBackground(1L << 8), // deprecated 10.2-11.0
        unifiedTitleAndToolbar(1L << 12),
        hudWindow(1L << 13),
        fullScreen(1L << 14),
        fullSizeContentView(1L << 15);
        public final long value;
        StyleMask(long v) { this.value = v; }
        public static long mask(StyleMask... m) { long r = 0; for (var x : m) r |= x.value; return r; }
        public static StyleMask fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSWindowTitleVisibility` — `NSWindowTitleVisible`=0, `NSWindowTitleHidden`=1.
    /// Source: `NSWindow.h` `typedef NS_ENUM(NSInteger, NSWindowTitleVisibility)` and https://developer.apple.com/documentation/appkit/nswindowtitlevisibility
    public enum TitleVisibility {
        visible(0), hidden(1);
        public final long value;
        TitleVisibility(long v) { this.value = v; }
        public static TitleVisibility fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSWindowLevel` — window stacking levels. Values are `kCGWindowLevel` constants.
    /// Source: `CGWindowLevel.h` (`kCGNormalWindowLevel`=0, `kCGFloatingWindowLevel`=3, `kCGModalPanelWindowLevel`=8, etc.)
    /// and https://developer.apple.com/documentation/appkit/nswindow/level
    public enum WindowLevel {
        normal(0),
        floating(3),
        submenu(3), // kCGTornOffMenuWindowLevel == 3
        tornOffMenu(3),
        mainMenu(24),
        status(25),
        modalPanel(8),
        popUpMenu(101),
        screenSaver(1000),
        dock(20),
        utility(19),
        dragging(500),
        overlay(102),
        help(200);
        public final long value;
        WindowLevel(long v) { this.value = v; }
        public static WindowLevel fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSBackingStoreType` — `NSBackingStoreRetained`=0, `Nonretained`=1, `Buffered`=2.
    /// Source: `NSGraphics.h` `typedef NS_ENUM(NSUInteger, NSBackingStoreType)` and https://developer.apple.com/documentation/appkit/nsbackingstoretype
    public enum BackingStoreType {
        retained(0), nonretained(1), buffered(2);
        public final long value;
        BackingStoreType(long v) { this.value = v; }
        public static BackingStoreType fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSWindowTabbingMode` — 0=Automatic, 1=Preferred, 2=Disallowed.
    /// Source: `NSWindow.h` `typedef NS_ENUM(NSInteger, NSWindowTabbingMode)`
    public enum TabbingMode {
        automatic(0), preferred(1), disallowed(2);
        public final long value;
        TabbingMode(long v) { this.value = v; }
        public static TabbingMode fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    // ---------------------------------------------------------------- window "style"

    /// [window styleMask] — the compositional style bit-field (see class Javadoc for bits).
    public long styleMask() {
        return ObjC.msgSendLong(peer, ObjC.sel("styleMask"));
    }

    /// [window setStyleMask:] — replace the style bit-field (see class Javadoc for bits).
    public void setStyleMask(long mask) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStyleMask:"), mask);
    }
    /// Typed overload: `setStyleMask(StyleMask...)` — composes bitmask via `StyleMask.mask`.
    public void setStyleMask(StyleMask... masks) { setStyleMask(StyleMask.mask(masks)); }
    /// Convenience: `create` overload accepting `StyleMask` varargs and `BackingStoreType` enum.
    public static NSWindow create(NSRect contentRect, StyleMask[] styleMasks, BackingStoreType backing, boolean defer) {
        return create(contentRect, StyleMask.mask(styleMasks), backing.value, defer);
    }
    /// Convenience: `create` overload with enum backing.
    public static NSWindow create(NSRect contentRect, long styleMask, BackingStoreType backing, boolean defer) {
        return create(contentRect, styleMask, backing.value, defer);
    }

    /// [window setTitlebarAppearsTransparent:] — modern translucent title bar.
    public void setTitlebarAppearsTransparent(boolean transparent) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setTitlebarAppearsTransparent:"), transparent);
    }

    /// [window titlebarAppearsTransparent].
    public boolean isTitlebarAppearsTransparent() {
        return ObjC.msgSendBool(peer, ObjC.sel("titlebarAppearsTransparent"));
    }

    /// [window setTitleVisibility:] — 0 = `NSWindowTitleVisible`,
    /// 1 = `NSWindowTitleHidden`. Only meaningful on a titled window.
    public void setTitleVisibility(long visibility) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTitleVisibility:"), visibility);
    }
    /// Typed overload.
    public void setTitleVisibility(TitleVisibility v) { setTitleVisibility(v.value); }

    /// [window setLevel:] — e.g. `NSFloatingWindowLevel`=3, `NSModalPanelWindowLevel`=8.
    public void setLevel(long level) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setLevel:"), level);
    }
    /// Typed overload.
    public void setLevel(WindowLevel lvl) { setLevel(lvl.value); }

    /// [window level].
    public long level() {
        return ObjC.msgSendLong(peer, ObjC.sel("level"));
    }
    /// Typed getter — maps raw level to `WindowLevel` enum where known.
    public WindowLevel levelEnum() { return WindowLevel.fromValue(level()); }

    /// [window setCollectionBehavior:] — e.g. `NSWindowCollectionBehaviorCanJoinAllSpaces`.
    public void setCollectionBehavior(long behavior) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setCollectionBehavior:"), behavior);
    }

    /// [panel setHidesOnDeactivate:] — real `NSPanel` behavior; see class Javadoc.
    public void setHidesOnDeactivate(boolean hide) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setHidesOnDeactivate:"), hide);
    }

    /// [panel hidesOnDeactivate].
    public boolean hidesOnDeactivate() {
        return ObjC.msgSendBool(peer, ObjC.sel("hidesOnDeactivate"));
    }

    /// [panel setBecomesKeyOnlyIfNeeded:] — key only while controls need it (NSPanel).
    public void setBecomesKeyOnlyIfNeeded(boolean onlyIfNeeded) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBecomesKeyOnlyIfNeeded:"), onlyIfNeeded);
    }

    /// [panel becomesKeyOnlyIfNeeded].
    public boolean becomesKeyOnlyIfNeeded() {
        return ObjC.msgSendBool(peer, ObjC.sel("becomesKeyOnlyIfNeeded"));
    }

    /// [window standardWindowButton:] — one of the standard close/miniaturize/zoom
    /// buttons. `windowButton`: 0 = `NSWindowCloseButton`, 1 =
    /// `NSWindowMiniaturizeButton`, 2 = `NSWindowZoomButton`.
    ///
    /// Returns the raw peer wrapped as `NSObject`. The native object IS an
    /// `NSButton` subclass (measured here: `_NSThemeCloseWidget`) but its
    /// Java wrapper has no public way to wrap a foreign peer (its constructor is
    /// private) — so callers receive it typed as `NSObject` and may use it via
    /// the ObjC escape hatch or treat it as an opaque id. Non-null for a titled
    /// window's close button.
    ///
    /// AppKit honesty: the private two-argument SPI
    /// `standardWindowButton:forFlag:` (which the `(ID,INT,BOOL)` vocabulary
    /// entry targets) is NOT recognized by the runtime — `respondsToSelector:`
    /// returns `false` and sending it aborts the JVM with
    /// `NSInvalidArgumentException 'unrecognized selector'`. The public, recognized
    /// selector is the single-argument `standardWindowButton:`. This wrapper uses
    /// that, so the `(ID,INT,BOOL)` vocabulary line is currently unused by our code.
    public NSObject standardWindowButton(long windowButton) {
        try {
            MemorySegment btn = (MemorySegment) H.hStdWinButton().invokeExact(peer,
                    ObjC.sel("standardWindowButton:"), windowButton);
            return NSObject.wrap(btn);
        } catch (Throwable t) {
            throw new RuntimeException("standardWindowButton: failed", t);
        }
    }

    /// Utility/panel detection. AppKit honesty: the private selector
    /// `isUtilityWindow` is NOT recognized by the runtime (`respondsToSelector:`
    /// returns `false` on both `NSWindow` and `NSPanel`, and sending it
    /// aborts with `NSInvalidArgumentException 'unrecognized selector'`). The public,
    /// recognized predicate for "is this a utility / panel window" is
    /// `isFloatingPanel`, which returns `true` for `NSPanel` instances
    /// and for windows whose styleMask includes `NSWindowStyleMaskUtilityWindow`.
    /// This wrapper queries that selector, so `isUtilityWindow()` == `true`
    /// exactly when the window is a utility panel.
    public boolean isUtilityWindow() {
        return ObjC.msgSendBool(peer, ObjC.sel("isFloatingPanel"));
    }

    // ---------------------------------------------------------------- additional properties — completeness

    /// [window title] — the window title string.
    public String title() {
        MemorySegment s = ObjC.msgSendId(peer, ObjC.sel("title"));
        return ObjC.toString(s);
    }

    /// [window titleVisibility] — 0 = NSWindowTitleVisible, 1 = NSWindowTitleHidden.
    public long titleVisibility() {
        return ObjC.msgSendLong(peer, ObjC.sel("titleVisibility"));
    }
    /// Typed getter.
    public TitleVisibility titleVisibilityEnum() { return TitleVisibility.fromValue(titleVisibility()); }

    /// [window collectionBehavior] — NSWindowCollectionBehavior bit-field.
    public long collectionBehavior() {
        return ObjC.msgSendLong(peer, ObjC.sel("collectionBehavior"));
    }

    /// [window contentView] — the window's root content view.
    public NSView contentView() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("contentView"));
        return NSView.wrap(v);
    }

    /// [window isMovable].
    public boolean isMovable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isMovable"));
    }

    /// [window setMovable:].
    public void setMovable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setMovable:"), flag);
    }

    /// [window isMovableByWindowBackground].
    public boolean isMovableByWindowBackground() {
        return ObjC.msgSendBool(peer, ObjC.sel("isMovableByWindowBackground"));
    }

    /// [window setMovableByWindowBackground:].
    public void setMovableByWindowBackground(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setMovableByWindowBackground:"), flag);
    }

    /// [window isExcludedFromWindowsMenu].
    public boolean isExcludedFromWindowsMenu() {
        return ObjC.msgSendBool(peer, ObjC.sel("isExcludedFromWindowsMenu"));
    }

    /// [window setExcludedFromWindowsMenu:].
    public void setExcludedFromWindowsMenu(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setExcludedFromWindowsMenu:"), flag);
    }

    /// [window tabbingMode] — NSWindowTabbingMode.
    public long tabbingMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("tabbingMode"));
    }
    /// Typed getter.
    public TabbingMode tabbingModeEnum() { return TabbingMode.fromValue(tabbingMode()); }

    /// [window setTabbingMode:].
    public void setTabbingMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTabbingMode:"), mode);
    }
    /// Typed overload.
    public void setTabbingMode(TabbingMode mode) { setTabbingMode(mode.value); }

    /// [window backgroundColor] — may be nil.
    public NSColor backgroundColor() {
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("backgroundColor"));
        return NSColor.wrap(c);
    }

    /// [window setBackgroundColor:].
    public void setBackgroundColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBackgroundColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// [window isOpaque].
    public boolean isOpaque() {
        return ObjC.msgSendBool(peer, ObjC.sel("isOpaque"));
    }

    /// [window setOpaque:].
    public void setOpaque(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setOpaque:"), flag);
    }

    /// [window hasShadow].
    public boolean hasShadow() {
        return ObjC.msgSendBool(peer, ObjC.sel("hasShadow"));
    }

    /// [window setHasShadow:].
    public void setHasShadow(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setHasShadow:"), flag);
    }

    /// [window alphaValue] — 0.0 to 1.0.
    public double alphaValue() {
        try {
            return (double) H.hGetDouble().invokeExact(peer, ObjC.sel("alphaValue"));
        } catch (Throwable t) {
            throw new RuntimeException("alphaValue failed", t);
        }
    }

    /// [window setAlphaValue:].
    public void setAlphaValue(double alpha) {
        try {
            H.hSetDouble().invokeExact(peer, ObjC.sel("setAlphaValue:"), alpha);
        } catch (Throwable t) {
            throw new RuntimeException("setAlphaValue: failed", t);
        }
    }

    /// [window minSize] — NSSize.
    public NSSize minSize() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("minSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("minSize failed", t);
        }
    }

    /// [window setMinSize:].
    public void setMinSize(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setMinSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMinSize: failed", t);
        }
    }

    /// [window maxSize] — NSSize.
    public NSSize maxSize() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("maxSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("maxSize failed", t);
        }
    }

    /// [window setMaxSize:].
    public void setMaxSize(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setMaxSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMaxSize: failed", t);
        }
    }

    /// [window frameAutosaveName] — may be nil/empty.
    public String frameAutosaveName() {
        MemorySegment s = ObjC.msgSendId(peer, ObjC.sel("frameAutosaveName"));
        return ObjC.toString(s);
    }

    /// [window setFrameAutosaveName:].
    public void setFrameAutosaveName(String name) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFrameAutosaveName:"), ObjC.nsstring(name));
    }

    /// [window isDocumentEdited].
    public boolean isDocumentEdited() {
        return ObjC.msgSendBool(peer, ObjC.sel("isDocumentEdited"));
    }

    /// [window setDocumentEdited:].
    public void setDocumentEdited(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDocumentEdited:"), flag);
    }

    public long windowNumber() {
        return ObjC.msgSendLong(peer, ObjC.sel("windowNumber"));
    }

    public boolean isVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVisible"));
    }

    public boolean isKeyWindow() {
        return ObjC.msgSendBool(peer, ObjC.sel("isKeyWindow"));
    }

    public boolean isMainWindow() {
        return ObjC.msgSendBool(peer, ObjC.sel("isMainWindow"));
    }

    public void makeKeyAndOrderFront(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("makeKeyAndOrderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
    }

    /// orderFront: — show without making key: the window becomes visible but
    /// never activates the app and never steals focus. The unobtrusive
    /// counterpart to makeKeyAndOrderFront: (which keys + activates).
    public void orderFront(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
    }

    /// Window sharing types (NSWindowSharingType): whether other processes
    /// (screen capture, sharing) may read the contents. Default is READ_ONLY.
    public static final long SHARING_NONE = 0;
    public static final long SHARING_READ_ONLY = 1;

    /// sharingType.
    public long sharingType() {
        return ObjC.msgSendLong(peer, ObjC.sel("sharingType"));
    }

    /// setSharingType: — NONE excludes the window from capture/sharing.
    /// NOTE (measured on Tahoe): setting NONE sticks — a later set back to
    /// READ_ONLY still reads NONE on a live window. Decide at creation time.
    public void setSharingType(long type) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSharingType:"), type);
    }

    public void performClose(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("performClose:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
    }

    // ---------------------------------------------------------------- sheets (modal sheet inside window, blocks window)

    /// beginSheet:completionHandler: — attach sheet to receiver; handler receives NSModalResponse.
    public void beginSheet(NSWindow sheet, java.util.function.IntConsumer completionHandler) {
        if (sheet == null) return;
        try {
            MemorySegment block;
            if (completionHandler == null) {
                block = MemorySegment.NULL;
            } else {
                java.lang.invoke.MethodHandle target = java.lang.invoke.MethodHandles.lookup().findStatic(
                        NSWindow.class, "sheetCompletionBridge",
                        java.lang.invoke.MethodType.methodType(void.class, MemorySegment.class, long.class, java.util.function.IntConsumer.class));
                java.lang.invoke.MethodHandle bound = java.lang.invoke.MethodHandles.insertArguments(target, 2, completionHandler);
                // block signature: void(^)(NSModalResponse) -> void with blockSelf leading
                java.lang.foreign.FunctionDescriptor fd = java.lang.foreign.FunctionDescriptor.ofVoid(
                        (java.lang.foreign.ValueLayout) java.lang.foreign.Linker.nativeLinker().canonicalLayouts().get("void*"),
                        (java.lang.foreign.ValueLayout) java.lang.foreign.Linker.nativeLinker().canonicalLayouts().get("long"));
                // Blocks.block expects leading PTR param + user args; wrap to (PTR, long) -> void
                java.lang.invoke.MethodHandle adapted = bound.asType(java.lang.invoke.MethodType.methodType(void.class, MemorySegment.class, long.class));
                block = nsui.objc.Blocks.block(adapted, java.lang.foreign.FunctionDescriptor.ofVoid(
                        (java.lang.foreign.ValueLayout) java.lang.foreign.Linker.nativeLinker().canonicalLayouts().get("void*"),
                        (java.lang.foreign.ValueLayout) java.lang.foreign.Linker.nativeLinker().canonicalLayouts().get("long")));
            }
            java.lang.invoke.MethodHandle h = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.VOID, nsui.objc.Sig.Arg.ID, nsui.objc.Sig.Arg.ID));
            MemorySegment blk = (block == null || block.address() == 0) ? MemorySegment.NULL : block;
            h.invokeExact(peer, ObjC.sel("beginSheet:completionHandler:"), sheet.peer(), (MemorySegment) blk);
        } catch (Throwable t) {
            throw new RuntimeException("beginSheet:completionHandler: failed", t);
        }
    }

    /// beginSheet:completionHandler: with raw block segment (for advanced use).
    public void beginSheet(NSWindow sheet, MemorySegment completionHandlerBlock) {
        if (sheet == null) return;
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.VOID, nsui.objc.Sig.Arg.ID, nsui.objc.Sig.Arg.ID));
            MemorySegment blk2 = (completionHandlerBlock == null || completionHandlerBlock.address() == 0) ? MemorySegment.NULL : completionHandlerBlock;
            h.invokeExact(peer, ObjC.sel("beginSheet:completionHandler:"), sheet.peer(), (MemorySegment) blk2);
        } catch (Throwable t) {
            throw new RuntimeException("beginSheet:completionHandler: failed", t);
        }
    }

    private static void sheetCompletionBridge(MemorySegment blockSelf, long response, java.util.function.IntConsumer handler) {
        handler.accept((int) response);
    }

    /// endSheet: — dismiss sheet.
    public void endSheet(NSWindow sheet) {
        if (sheet == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("endSheet:"), sheet.peer());
    }

    /// endSheet:returnCode:
    public void endSheet(NSWindow sheet, long returnCode) {
        if (sheet == null) return;
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.VOID, nsui.objc.Sig.Arg.ID, nsui.objc.Sig.Arg.INT));
            h.invokeExact(peer, ObjC.sel("endSheet:returnCode:"), sheet.peer(), returnCode);
        } catch (Throwable t) {
            throw new RuntimeException("endSheet:returnCode: failed", t);
        }
    }

    /// attachedSheet — current sheet or null.
    public NSWindow attachedSheet() {
        MemorySegment s = ObjC.msgSendId(peer, ObjC.sel("attachedSheet"));
        return (s == null || s.address() == 0) ? null : new NSWindow(s);
    }

    /// isSheet
    public boolean isSheet() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSheet"));
    }

    /// sheetParent
    public NSWindow sheetParent() {
        MemorySegment s = ObjC.msgSendId(peer, ObjC.sel("sheetParent"));
        return (s == null || s.address() == 0) ? null : new NSWindow(s);
    }

    /// orderOut:
    public void orderOut(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderOut:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    // ---- additional readonly completeness ----
    public boolean isZoomed() { return ObjC.msgSendBool(peer, ObjC.sel("isZoomed")); }
    public boolean isMiniaturized() { return ObjC.msgSendBool(peer, ObjC.sel("isMiniaturized")); }
    public boolean canBecomeKeyWindow() { return ObjC.msgSendBool(peer, ObjC.sel("canBecomeKeyWindow")); }
    public boolean canBecomeMainWindow() { return ObjC.msgSendBool(peer, ObjC.sel("canBecomeMainWindow")); }
    public boolean worksWhenModal() { return ObjC.msgSendBool(peer, ObjC.sel("worksWhenModal")); }
    public MemorySegment screen() { return ObjC.msgSendId(peer, ObjC.sel("screen")); }
    public boolean hasDynamicDepthLimit() { return ObjC.msgSendBool(peer, ObjC.sel("hasDynamicDepthLimit")); }

    /// [window screen] typed — the screen the window is on (null if offscreen).
    public NSScreen screenObject() {
        return NSScreen.wrap(ObjC.msgSendId(peer, ObjC.sel("screen")));
    }

    /// [window deepestScreen] — raw peer of the deepest screen the window is on.
    public MemorySegment deepestScreen() {
        return ObjC.msgSendId(peer, ObjC.sel("deepestScreen"));
    }

    /// [window deepestScreen] typed (null if offscreen).
    public NSScreen deepestScreenObject() {
        return NSScreen.wrap(ObjC.msgSendId(peer, ObjC.sel("deepestScreen")));
    }

    /// [window deviceDescription] — display device dictionary for the window's screen.
    public NSDictionary deviceDescription() {
        return NSDictionary.wrap(ObjC.msgSendId(peer, ObjC.sel("deviceDescription")));
    }

    /// [window setDynamicDepthLimit:].
    public void setDynamicDepthLimit(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDynamicDepthLimit:"), flag);
    }

    // ---------------------------------------------------------------- window coverage batch:
    // getters/setters/actions, window lists, screen info (SDK: NSWindow.h).
    //
    // Every method below uses a shape already registered in Sig.VOCABULARY.
    // OMITTED with reasons (header wins on API truth):
    // - NSWindowDelegate protocol methods — need upcall machinery (per batch rules).
    // - block-taking methods: beginCriticalSheet:completionHandler:,
    //   transferWindowSharingToWindow:completionHandler:,
    //   requestSharingOfWindow:completionHandler:,
    //   requestSharingOfWindowUsingPreview:title:completionHandler:,
    //   trackEventsMatchingMask:timeout:mode:handler: — blocks need trampolines.
    // - deprecated: texturedBackground style use, cacheImageInRect:/restoreCachedImage/
    //   discardCachedImage, menuChanged:, gState, convertBaseToScreen:/convertScreenToBase:,
    //   userSpaceScaleFactor, useOptimizedDrawing:, canStoreColor, disable/enableFlushWindow,
    //   flushWindow/flushWindowIfNeeded, autodisplay, graphicsContext, oneShot,
    //   preferredBackingLocation/backingLocation, showsResizeIndicator,
    //   initWithWindowRef:/windowRef, disableScreenUpdatesUntilFlush,
    //   dragImage:at:offset:event:pasteboard:source:slideBack:, showsToolbarButton.
    // - init overloads covered by create(): initWithCoder: (NS_UNAVAILABLE anyway).
    // - initWithContentRect:styleMask:backing:defer:screen: — needs
    //   shape of(ID,RECT,INT,INT,BOOL,ID), not registered (requested below).
    // - frameRectForContentRect:styleMask: / contentRectForFrameRect:styleMask:
    //   (class + instance) — need shape of(RECT,RECT,INT), not registered.
    // - minFrameWidthWithTitle:styleMask: — needs of(DOUBLE,ID,INT), not registered.
    // - cascadeTopLeftFromPoint: — needs of(POINT,POINT), not registered.
    // - animationResizeTime: — needs of(DOUBLE,RECT), not registered.
    // - setFrame:display:animate: — needs of(VOID,RECT,BOOL,BOOL), not registered.
    // - constrainFrameRect:toScreen: — needs of(RECT,RECT,ID), not registered.
    // - fieldEditor:forObject: — needs of(ID,BOOL,ID), not registered.
    // - convertPointToScreen:/convertPointFromScreen:/convertPointToBacking:/
    //   convertPointFromBacking: — need of(POINT,POINT), not registered.
    // - backingAlignedRect:options: — needs of(RECT,RECT,INT), not registered.
    // - windowNumberAtPoint:belowWindowWithWindowNumber: — needs
    //   of(INT,POINT,INT), not registered.
    // - setFrameAutosaveName: natively returns BOOL; the existing void wrapper
    //   discards it (kept for compatibility).
    // - parentWindow setter omitted: header property is effectively derived from
    //   addChildWindow:/removeChildWindow: (getter only here).
    //
    // REQUESTED Sig shapes (absent from Sig.java, needed for the omissions above):
    //   of(Ret.RECT, Arg.RECT, Arg.INT), of(Ret.DOUBLE, Arg.ID, Arg.INT),
    //   of(Ret.POINT, Arg.POINT), of(Ret.DOUBLE, Arg.RECT),
    //   of(Ret.VOID, Arg.RECT, Arg.BOOL, Arg.BOOL), of(Ret.RECT, Arg.RECT, Arg.ID),
    //   of(Ret.ID, Arg.BOOL, Arg.ID), of(Ret.INT, Arg.POINT, Arg.INT),
    //   of(Ret.ID, Arg.RECT, Arg.INT, Arg.INT, Arg.BOOL, Arg.ID).

    /// NSWindowOrderingMode for orderWindow:relativeTo: / addChildWindow:ordered:.
    public static final long ORDER_OUT = -1;
    public static final long ORDER_BELOW = 0;
    public static final long ORDER_ABOVE = 1;

    /// NSWindowButton ids for standardWindowButton:.
    public static final long BUTTON_CLOSE = 0;
    public static final long BUTTON_MINIATURIZE = 1;
    public static final long BUTTON_ZOOM = 2;
    public static final long BUTTON_TOOLBAR = 3;
    public static final long BUTTON_DOCUMENT_ICON = 4;

    // ---- class-side window lists and defaults ----

    /// +windowNumbersWithOptions: — window numbers of visible windows matching
    /// `NSWindowNumberListOptions` (0 = calling app, active space).
    public static NSArray windowNumbersWithOptions(long options) {
        try {
            MemorySegment arr = (MemorySegment) H.hIdInt().invokeExact(
                    ObjC.cls("NSWindow"), ObjC.sel("windowNumbersWithOptions:"), options);
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("windowNumbersWithOptions: failed", t);
        }
    }

    /// +windowWithContentViewController: — titled window hosting the controller.
    public static NSWindow windowWithContentViewController(NSViewController controller) {
        MemorySegment w = ObjC.msgSendIdId(ObjC.cls("NSWindow"),
                ObjC.sel("windowWithContentViewController:"),
                controller == null ? MemorySegment.NULL : controller.peer());
        return wrap(w);
    }

    /// +defaultDepthLimit — class default window depth limit.
    public static long defaultDepthLimit() {
        return ObjC.msgSendLong(ObjC.cls("NSWindow"), ObjC.sel("defaultDepthLimit"));
    }

    /// +allowsAutomaticWindowTabbing / setAllowsAutomaticWindowTabbing:.
    public static boolean allowsAutomaticWindowTabbing() {
        return ObjC.msgSendBool(ObjC.cls("NSWindow"), ObjC.sel("allowsAutomaticWindowTabbing"));
    }

    /// +setAllowsAutomaticWindowTabbing:.
    public static void setAllowsAutomaticWindowTabbing(boolean flag) {
        ObjC.msgSendVoidBool(ObjC.cls("NSWindow"), ObjC.sel("setAllowsAutomaticWindowTabbing:"), flag);
    }

    /// +userTabbingPreference — system tabbing preference (readonly).
    public static long userTabbingPreference() {
        return ObjC.msgSendLong(ObjC.cls("NSWindow"), ObjC.sel("userTabbingPreference"));
    }

    /// +standardWindowButton:forStyleMask: — the canonical button for a style mask.
    public static NSObject standardWindowButtonForStyleMask(long button, long styleMask) {
        try {
            MemorySegment btn = (MemorySegment) H.hIdIntInt().invokeExact(
                    ObjC.cls("NSWindow"), ObjC.sel("standardWindowButton:forStyleMask:"), button, styleMask);
            return NSObject.wrap(btn);
        } catch (Throwable t) {
            throw new RuntimeException("standardWindowButton:forStyleMask: failed", t);
        }
    }

    /// +removeFrameUsingName: — forget a saved frame.
    public static void removeFrameUsingName(String name) {
        ObjC.msgSendVoidId(ObjC.cls("NSWindow"), ObjC.sel("removeFrameUsingName:"), ObjC.nsstring(name));
    }

    // ---- title / subtitle / toolbar style / layout rects ----

    /// [window subtitle] (macOS 11+) — secondary title text, may be nil.
    public String subtitle() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("subtitle")));
    }

    /// [window setSubtitle:] — AppKit honesty: unlike `setTitle:`, this selector
    /// REJECTS nil (`NSInternalInconsistencyException 'subtitle != nil'`, which
    /// aborts the JVM). A Java null is therefore mapped to `""` (visually empty).
    public void setSubtitle(String subtitle) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSubtitle:"),
                ObjC.nsstring(subtitle == null ? "" : subtitle));
    }

    /// [window toolbarStyle] — NSWindowToolbarStyle (macOS 11+).
    public long toolbarStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("toolbarStyle"));
    }

    /// [window setToolbarStyle:].
    public void setToolbarStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setToolbarStyle:"), style);
    }

    /// [window contentLayoutRect] — layout area for the content (macOS 10.10+).
    public NSRect contentLayoutRect() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("contentLayoutRect")));
    }

    /// [window cascadingReferenceFrame] — reference frame for cascading (macOS 15+).
    public NSRect cascadingReferenceFrame() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("cascadingReferenceFrame")));
    }

    /// [window titlebarAccessoryViewControllers] — accessory controllers (may be empty).
    public NSArray titlebarAccessoryViewControllers() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("titlebarAccessoryViewControllers")));
    }

    /// [window addTitlebarAccessoryViewController:].
    public void addTitlebarAccessoryViewController(NSObject childViewController) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addTitlebarAccessoryViewController:"),
                childViewController == null ? MemorySegment.NULL : childViewController.peer());
    }

    /// [window insertTitlebarAccessoryViewController:atIndex:].
    public void insertTitlebarAccessoryViewControllerAtIndex(NSObject childViewController, long index) {
        try {
            H.hVoidIdInt().invokeExact(peer, ObjC.sel("insertTitlebarAccessoryViewController:atIndex:"),
                    (MemorySegment) (childViewController == null ? MemorySegment.NULL : childViewController.peer()), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertTitlebarAccessoryViewController:atIndex: failed", t);
        }
    }

    /// [window removeTitlebarAccessoryViewControllerAtIndex:].
    public void removeTitlebarAccessoryViewControllerAtIndex(long index) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("removeTitlebarAccessoryViewControllerAtIndex:"), index);
    }

    // ---- represented file ----

    /// [window representedURL] — raw peer (no NSURL wrapper yet), may be nil.
    public MemorySegment representedURL() {
        return ObjC.msgSendId(peer, ObjC.sel("representedURL"));
    }

    /// [window setRepresentedURL:] — nil clears.
    public void setRepresentedURL(MemorySegment url) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setRepresentedURL:"),
                url == null ? MemorySegment.NULL : url);
    }

    /// [window representedFilename] — may be nil/empty.
    public String representedFilename() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("representedFilename")));
    }

    /// [window setRepresentedFilename:] — must be non-null (a Java null throws
    /// in `nsstring` before reaching AppKit, which likewise requires non-nil).
    public void setRepresentedFilename(String filename) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setRepresentedFilename:"), ObjC.nsstring(filename));
    }

    /// [window setTitleWithRepresentedFilename:] — title follows the file name.
    public void setTitleWithRepresentedFilename(String filename) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTitleWithRepresentedFilename:"), ObjC.nsstring(filename));
    }

    // ---- frame geometry extras ----

    /// [window endEditingFor:] — end any editing session for `object` (nil-safe).
    public void endEditingFor(NSObject object) {
        ObjC.msgSendVoidId(peer, ObjC.sel("endEditingFor:"),
                object == null ? MemorySegment.NULL : object.peer());
    }

    /// [window setFrameTopLeftPoint:] — position by top-left corner.
    public void setFrameTopLeftPoint(NSPoint point) {
        try {
            H.hSetFrameOrigin().invokeExact(peer, ObjC.sel("setFrameTopLeftPoint:"), point.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setFrameTopLeftPoint: failed", t);
        }
    }

    /// [window isInLiveResize] (macOS 10.6+).
    public boolean isInLiveResize() {
        return ObjC.msgSendBool(peer, ObjC.sel("inLiveResize"));
    }

    /// [window resizeIncrements] / [window setResizeIncrements:].
    public NSSize resizeIncrements() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("resizeIncrements"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("resizeIncrements failed", t);
        }
    }

    /// [window setResizeIncrements:].
    public void setResizeIncrements(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setResizeIncrements:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setResizeIncrements: failed", t);
        }
    }

    /// [window aspectRatio] / [window setAspectRatio:].
    public NSSize aspectRatio() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("aspectRatio"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("aspectRatio failed", t);
        }
    }

    /// [window setAspectRatio:] — AppKit honesty: setting (0,0) after a nonzero
    /// ratio poisons the constraint state — the NEXT setFrame* then traps
    /// silently (SIGTRAP, measured). Restore to a valid ratio like (1,1).
    public void setAspectRatio(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setAspectRatio:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setAspectRatio: failed", t);
        }
    }

    /// [window contentResizeIncrements] / [window setContentResizeIncrements:].
    public NSSize contentResizeIncrements() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("contentResizeIncrements"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentResizeIncrements failed", t);
        }
    }

    /// [window setContentResizeIncrements:].
    public void setContentResizeIncrements(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setContentResizeIncrements:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentResizeIncrements: failed", t);
        }
    }

    /// [window contentAspectRatio] / [window setContentAspectRatio:].
    public NSSize contentAspectRatio() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("contentAspectRatio"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentAspectRatio failed", t);
        }
    }

    /// [window setContentAspectRatio:] — AppKit honesty: like `setAspectRatio:`,
    /// setting (0,0) after a nonzero ratio poisons the constraint state and the
    /// next setFrame* traps silently (SIGTRAP, measured). Restore to (1,1).
    public void setContentAspectRatio(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setContentAspectRatio:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentAspectRatio: failed", t);
        }
    }

    /// [window contentMinSize] / [window setContentMinSize:].
    public NSSize contentMinSize() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("contentMinSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentMinSize failed", t);
        }
    }

    /// [window setContentMinSize:].
    public void setContentMinSize(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setContentMinSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentMinSize: failed", t);
        }
    }

    /// [window contentMaxSize] / [window setContentMaxSize:].
    public NSSize contentMaxSize() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("contentMaxSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentMaxSize failed", t);
        }
    }

    /// [window setContentMaxSize:].
    public void setContentMaxSize(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setContentMaxSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentMaxSize: failed", t);
        }
    }

    /// [window minFullScreenContentSize] / setter (macOS 10.11+).
    public NSSize minFullScreenContentSize() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("minFullScreenContentSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("minFullScreenContentSize failed", t);
        }
    }

    /// [window setMinFullScreenContentSize:].
    public void setMinFullScreenContentSize(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setMinFullScreenContentSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMinFullScreenContentSize: failed", t);
        }
    }

    /// [window maxFullScreenContentSize] / setter (macOS 10.11+).
    public NSSize maxFullScreenContentSize() {
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("maxFullScreenContentSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("maxFullScreenContentSize failed", t);
        }
    }

    /// [window setMaxFullScreenContentSize:].
    public void setMaxFullScreenContentSize(NSSize size) {
        try {
            H.hSetSize().invokeExact(peer, ObjC.sel("setMaxFullScreenContentSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMaxFullScreenContentSize: failed", t);
        }
    }

    // ---- display / drawing ----

    /// [window viewsNeedDisplay] / [window setViewsNeedDisplay:].
    public boolean viewsNeedDisplay() {
        return ObjC.msgSendBool(peer, ObjC.sel("viewsNeedDisplay"));
    }

    /// [window setViewsNeedDisplay:].
    public void setViewsNeedDisplay(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setViewsNeedDisplay:"), flag);
    }

    /// [window displayIfNeeded] — redraw only views marked dirty.
    public void displayIfNeeded() {
        ObjC.msgSendVoid(peer, ObjC.sel("displayIfNeeded"));
    }

    /// [window display] — redraw now.
    public void display() {
        ObjC.msgSendVoid(peer, ObjC.sel("display"));
    }

    /// [window preservesContentDuringLiveResize] / setter.
    public boolean preservesContentDuringLiveResize() {
        return ObjC.msgSendBool(peer, ObjC.sel("preservesContentDuringLiveResize"));
    }

    /// [window setPreservesContentDuringLiveResize:].
    public void setPreservesContentDuringLiveResize(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setPreservesContentDuringLiveResize:"), flag);
    }

    /// [window update] — refresh dirty state.
    public void update() {
        ObjC.msgSendVoid(peer, ObjC.sel("update"));
    }

    /// [window resizeFlags] — modifiers held when resizing started.
    public long resizeFlags() {
        return ObjC.msgSendLong(peer, ObjC.sel("resizeFlags"));
    }

    // ---- close / miniaturize / zoom actions ----

    /// [window close] — close now (delegate `windowShouldClose:` still consulted).
    public void close() {
        ObjC.msgSendVoid(peer, ObjC.sel("close"));
    }

    /// [window miniaturize:] — sender may be null.
    public void miniaturize(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("miniaturize:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window deminiaturize:] — sender may be null.
    public void deminiaturize(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("deminiaturize:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window zoom:] — sender may be null.
    public void zoom(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("zoom:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window performMiniaturize:] — menu-action variant, sender may be null.
    public void performMiniaturize(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("performMiniaturize:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window performZoom:] — menu-action variant, sender may be null.
    public void performZoom(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("performZoom:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window tryToPerform:with:] — route an action; returns whether handled.
    public boolean tryToPerform(MemorySegment action, NSObject object) {
        try {
            return (boolean) H.hBoolIdId().invokeExact(peer, ObjC.sel("tryToPerform:with:"),
                    action, (MemorySegment) (object == null ? MemorySegment.NULL : object.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("tryToPerform:with: failed", t);
        }
    }

    /// [window validRequestorForSendType:returnType:] — raw peer, may be nil.
    public MemorySegment validRequestorForSendType(MemorySegment sendType, MemorySegment returnType) {
        try {
            return (MemorySegment) H.hIdIdId().invokeExact(peer,
                    ObjC.sel("validRequestorForSendType:returnType:"),
                    (MemorySegment) (sendType == null ? MemorySegment.NULL : sendType),
                    (MemorySegment) (returnType == null ? MemorySegment.NULL : returnType));
        } catch (Throwable t) {
            throw new RuntimeException("validRequestorForSendType:returnType: failed", t);
        }
    }

    // ---- content borders ----

    /// [window setContentBorderThickness:forEdge:] — `edge`: 1=minY (bottom).
    /// AppKit honesty: 0/2/3 (left/right/top) RAISE (`NSInvalidArgumentException`,
    /// aborts the JVM) on a standard window — measured: only the bottom edge
    /// accepts a thickness (top was for textured windows, deprecated since 10.2).
    public void setContentBorderThicknessForEdge(double thickness, long edge) {
        try {
            H.hVoidDoubleInt().invokeExact(peer, ObjC.sel("setContentBorderThickness:forEdge:"), thickness, edge);
        } catch (Throwable t) {
            throw new RuntimeException("setContentBorderThickness:forEdge: failed", t);
        }
    }

    /// [window contentBorderThicknessForEdge:].
    public double contentBorderThicknessForEdge(long edge) {
        try {
            return (double) H.hDoubleInt().invokeExact(peer, ObjC.sel("contentBorderThicknessForEdge:"), edge);
        } catch (Throwable t) {
            throw new RuntimeException("contentBorderThicknessForEdge: failed", t);
        }
    }

    /// [window setAutorecalculatesContentBorderThickness:forEdge:].
    public void setAutorecalculatesContentBorderThicknessForEdge(boolean flag, long edge) {
        try {
            H.hVoidBoolInt().invokeExact(peer, ObjC.sel("setAutorecalculatesContentBorderThickness:forEdge:"), flag, edge);
        } catch (Throwable t) {
            throw new RuntimeException("setAutorecalculatesContentBorderThickness:forEdge: failed", t);
        }
    }

    /// [window autorecalculatesContentBorderThicknessForEdge:].
    public boolean autorecalculatesContentBorderThicknessForEdge(long edge) {
        try {
            return (boolean) H.hBoolInt().invokeExact(peer, ObjC.sel("autorecalculatesContentBorderThicknessForEdge:"), edge);
        } catch (Throwable t) {
            throw new RuntimeException("autorecalculatesContentBorderThicknessForEdge: failed", t);
        }
    }

    // ---- ordering / visibility ----

    /// [window canHide] / [window setCanHide:].
    public boolean canHide() {
        return ObjC.msgSendBool(peer, ObjC.sel("canHide"));
    }

    /// [window setCanHide:].
    public void setCanHide(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setCanHide:"), flag);
    }

    /// [window orderBack:] — send behind all windows, sender may be null.
    /// AppKit honesty: this SHOWS the window (at the back) — measured
    /// `isVisible()==true` afterwards. Park offscreen first for unobtrusive use.
    public void orderBack(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderBack:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window orderWindow:relativeTo:] — `place`: -1=out, 0=below, 1=above.
    /// AppKit honesty: ordering OUT with `otherWin`=0 left the window visible in
    /// measurement — pass a real sibling window number, or use `orderOut:`.
    public void orderWindowRelativeTo(long place, long otherWindowNumber) {
        try {
            H.hVoidIntInt().invokeExact(peer, ObjC.sel("orderWindow:relativeTo:"), place, otherWindowNumber);
        } catch (Throwable t) {
            throw new RuntimeException("orderWindow:relativeTo: failed", t);
        }
    }

    /// [window orderFrontRegardless] — show even for a non-active app.
    public void orderFrontRegardless() {
        ObjC.msgSendVoid(peer, ObjC.sel("orderFrontRegardless"));
    }

    /// [window miniwindowImage] — custom miniaturized image, may be nil.
    public NSImage miniwindowImage() {
        return NSImage.wrap(ObjC.msgSendId(peer, ObjC.sel("miniwindowImage")));
    }

    /// [window setMiniwindowImage:] — nil restores the default snapshot.
    public void setMiniwindowImage(NSImage image) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMiniwindowImage:"),
                image == null ? MemorySegment.NULL : image.peer());
    }

    /// [window miniwindowTitle] — custom miniaturized title, may be nil.
    public String miniwindowTitle() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("miniwindowTitle")));
    }

    /// [window setMiniwindowTitle:] — nil restores the window title.
    public void setMiniwindowTitle(String title) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMiniwindowTitle:"),
                title == null ? MemorySegment.NULL : ObjC.nsstring(title));
    }

    /// [window dockTile] — the window's Dock tile (miniaturized windows).
    public NSDockTile dockTile() {
        return NSDockTile.wrap(ObjC.msgSendId(peer, ObjC.sel("dockTile")));
    }

    // ---- key / main actions ----

    /// [window makeKeyWindow].
    public void makeKeyWindow() {
        ObjC.msgSendVoid(peer, ObjC.sel("makeKeyWindow"));
    }

    /// [window makeMainWindow].
    public void makeMainWindow() {
        ObjC.msgSendVoid(peer, ObjC.sel("makeMainWindow"));
    }

    /// [window becomeKeyWindow].
    public void becomeKeyWindow() {
        ObjC.msgSendVoid(peer, ObjC.sel("becomeKeyWindow"));
    }

    /// [window resignKeyWindow].
    public void resignKeyWindow() {
        ObjC.msgSendVoid(peer, ObjC.sel("resignKeyWindow"));
    }

    /// [window becomeMainWindow].
    public void becomeMainWindow() {
        ObjC.msgSendVoid(peer, ObjC.sel("becomeMainWindow"));
    }

    /// [window resignMainWindow].
    public void resignMainWindow() {
        ObjC.msgSendVoid(peer, ObjC.sel("resignMainWindow"));
    }

    /// [window preventsApplicationTerminationWhenModal] / setter (macOS 10.6+).
    public boolean preventsApplicationTerminationWhenModal() {
        return ObjC.msgSendBool(peer, ObjC.sel("preventsApplicationTerminationWhenModal"));
    }

    /// [window setPreventsApplicationTerminationWhenModal:].
    public void setPreventsApplicationTerminationWhenModal(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setPreventsApplicationTerminationWhenModal:"), flag);
    }

    // ---- coordinate conversion / backing store ----

    /// [window convertRectToScreen:] — window-base rect to screen coordinates.
    public NSRect convertRectToScreen(NSRect rect) {
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("convertRectToScreen:"), rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectToScreen: failed", t);
        }
    }

    /// [window convertRectFromScreen:] — screen rect to window-base coordinates.
    public NSRect convertRectFromScreen(NSRect rect) {
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("convertRectFromScreen:"), rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectFromScreen: failed", t);
        }
    }

    /// [window convertRectToBacking:] — points to backing pixels (macOS 10.7+).
    public NSRect convertRectToBacking(NSRect rect) {
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("convertRectToBacking:"), rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectToBacking: failed", t);
        }
    }

    /// [window convertRectFromBacking:] — backing pixels to points (macOS 10.7+).
    public NSRect convertRectFromBacking(NSRect rect) {
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("convertRectFromBacking:"), rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectFromBacking: failed", t);
        }
    }

    /// [window backingScaleFactor] — points-to-pixels multiplier (macOS 10.7+).
    public double backingScaleFactor() {
        try {
            return (double) H.hGetDouble().invokeExact(peer, ObjC.sel("backingScaleFactor"));
        } catch (Throwable t) {
            throw new RuntimeException("backingScaleFactor failed", t);
        }
    }

    // ---- backing / depth / appearance ----

    /// [window backingType] — NSBackingStoreType.
    public long backingType() {
        return ObjC.msgSendLong(peer, ObjC.sel("backingType"));
    }

    /// [window setBackingType:].
    public void setBackingType(long type) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setBackingType:"), type);
    }

    /// [window depthLimit].
    public long depthLimit() {
        return ObjC.msgSendLong(peer, ObjC.sel("depthLimit"));
    }

    /// [window setDepthLimit:].
    public void setDepthLimit(long limit) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDepthLimit:"), limit);
    }

    /// [window animationBehavior] (macOS 10.7+) — 0=default, 1=none, ...5=alertPanel.
    public long animationBehavior() {
        return ObjC.msgSendLong(peer, ObjC.sel("animationBehavior"));
    }

    /// [window setAnimationBehavior:].
    public void setAnimationBehavior(long behavior) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setAnimationBehavior:"), behavior);
    }

    /// [window isOnActiveSpace] (macOS 10.6+).
    public boolean isOnActiveSpace() {
        return ObjC.msgSendBool(peer, ObjC.sel("isOnActiveSpace"));
    }

    /// [window occlusionState] (macOS 10.9+) — NSWindowOcclusionState bit-field.
    public long occlusionState() {
        return ObjC.msgSendLong(peer, ObjC.sel("occlusionState"));
    }

    /// [window allowsToolTipsWhenApplicationIsInactive] / setter.
    public boolean allowsToolTipsWhenApplicationIsInactive() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsToolTipsWhenApplicationIsInactive"));
    }

    /// [window setAllowsToolTipsWhenApplicationIsInactive:].
    public void setAllowsToolTipsWhenApplicationIsInactive(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsToolTipsWhenApplicationIsInactive:"), flag);
    }

    /// [window allowsConcurrentViewDrawing] / setter (macOS 10.6+).
    public boolean allowsConcurrentViewDrawing() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsConcurrentViewDrawing"));
    }

    /// [window setAllowsConcurrentViewDrawing:].
    public void setAllowsConcurrentViewDrawing(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsConcurrentViewDrawing:"), flag);
    }

    /// [window displaysWhenScreenProfileChanges] / setter.
    public boolean displaysWhenScreenProfileChanges() {
        return ObjC.msgSendBool(peer, ObjC.sel("displaysWhenScreenProfileChanges"));
    }

    /// [window setDisplaysWhenScreenProfileChanges:].
    public void setDisplaysWhenScreenProfileChanges(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDisplaysWhenScreenProfileChanges:"), flag);
    }

    /// [window canBecomeVisibleWithoutLogin] / setter (macOS 10.5+).
    public boolean canBecomeVisibleWithoutLogin() {
        return ObjC.msgSendBool(peer, ObjC.sel("canBecomeVisibleWithoutLogin"));
    }

    /// [window setCanBecomeVisibleWithoutLogin:].
    public void setCanBecomeVisibleWithoutLogin(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setCanBecomeVisibleWithoutLogin:"), flag);
    }

    /// [window invalidateShadow] — redraw the shadow next display.
    public void invalidateShadow() {
        ObjC.msgSendVoid(peer, ObjC.sel("invalidateShadow"));
    }

    /// [window appearanceSource] — raw peer (NSAppearanceCustomization), may be nil.
    public MemorySegment appearanceSource() {
        return ObjC.msgSendId(peer, ObjC.sel("appearanceSource"));
    }

    /// [window setAppearanceSource:] — nil resets to inherited appearance.
    public void setAppearanceSource(MemorySegment source) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAppearanceSource:"),
                source == null ? MemorySegment.NULL : source);
    }

    /// [window colorSpace] — raw peer (no NSColorSpace wrapper yet), may be nil.
    public MemorySegment colorSpace() {
        return ObjC.msgSendId(peer, ObjC.sel("colorSpace"));
    }

    /// [window setColorSpace:] — nil restores the default.
    public void setColorSpace(MemorySegment colorSpace) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setColorSpace:"),
                colorSpace == null ? MemorySegment.NULL : colorSpace);
    }

    /// [window canRepresentDisplayGamut:] (macOS 10.12+) — `gamut`: 0=sRGB, 1=DisplayP3, ...
    public boolean canRepresentDisplayGamut(long gamut) {
        try {
            return (boolean) H.hBoolInt().invokeExact(peer, ObjC.sel("canRepresentDisplayGamut:"), gamut);
        } catch (Throwable t) {
            throw new RuntimeException("canRepresentDisplayGamut: failed", t);
        }
    }

    /// [window titlebarSeparatorStyle] (macOS 11+) — NSTitlebarSeparatorStyle.
    public long titlebarSeparatorStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("titlebarSeparatorStyle"));
    }

    /// [window setTitlebarSeparatorStyle:].
    public void setTitlebarSeparatorStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTitlebarSeparatorStyle:"), style);
    }

    // ---- frame persistence ----

    /// [window stringWithSavedFrame] — persistable frame descriptor.
    public String stringWithSavedFrame() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("stringWithSavedFrame")));
    }

    /// [window setFrameFromString:] — restore a frame saved via stringWithSavedFrame.
    public void setFrameFromString(String frameString) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFrameFromString:"), ObjC.nsstring(frameString));
    }

    /// [window saveFrameUsingName:] — persist the frame under `name`.
    public void saveFrameUsingName(String name) {
        ObjC.msgSendVoidId(peer, ObjC.sel("saveFrameUsingName:"), ObjC.nsstring(name));
    }

    /// [window setFrameUsingName:force:] — restore a saved frame; returns success.
    public boolean setFrameUsingNameForce(String name, boolean force) {
        try {
            return (boolean) H.hBoolIdBool().invokeExact(peer,
                    ObjC.sel("setFrameUsingName:force:"), ObjC.nsstring(name), force);
        } catch (Throwable t) {
            throw new RuntimeException("setFrameUsingName:force: failed", t);
        }
    }

    /// [window setFrameUsingName:] — restore a saved frame; returns success.
    public boolean setFrameUsingName(String name) {
        try {
            return (boolean) H.hBoolId().invokeExact(peer,
                    ObjC.sel("setFrameUsingName:"), ObjC.nsstring(name));
        } catch (Throwable t) {
            throw new RuntimeException("setFrameUsingName: failed", t);
        }
    }

    // ---- controller / sheets / child windows ----

    /// [window windowController] — may be nil.
    public NSWindowController windowController() {
        return NSWindowController.wrap(ObjC.msgSendId(peer, ObjC.sel("windowController")));
    }

    /// [window setWindowController:] — nil detaches.
    public void setWindowController(NSWindowController controller) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setWindowController:"),
                controller == null ? MemorySegment.NULL : controller.peer());
    }

    /// [window sheets] (macOS 10.9+) — attached sheets, empty when none.
    public NSArray sheets() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("sheets")));
    }

    /// [window childWindows] — may be nil when childless.
    public NSArray childWindows() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("childWindows")));
    }

    /// [window parentWindow] — null for top-level windows.
    public NSWindow parentWindow() {
        return wrap(ObjC.msgSendId(peer, ObjC.sel("parentWindow")));
    }

    /// [window addChildWindow:ordered:] — `place`: -1=out, 0=below, 1=above.
    public void addChildWindow(NSWindow child, long place) {
        if (child == null) return;
        try {
            H.hVoidIdInt().invokeExact(peer, ObjC.sel("addChildWindow:ordered:"), child.peer(), place);
        } catch (Throwable t) {
            throw new RuntimeException("addChildWindow:ordered: failed", t);
        }
    }

    /// [window removeChildWindow:].
    public void removeChildWindow(NSWindow child) {
        if (child == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("removeChildWindow:"), child.peer());
    }

    // ---- content controller / dragging / export ----

    /// [window contentViewController] (macOS 10.10+) — may be nil.
    public NSViewController contentViewController() {
        return NSViewController.wrap(ObjC.msgSendId(peer, ObjC.sel("contentViewController")));
    }

    /// [window setContentViewController:] — nil detaches (also clears contentView).
    public void setContentViewController(NSViewController controller) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setContentViewController:"),
                controller == null ? MemorySegment.NULL : controller.peer());
    }

    /// [window performWindowDragWithEvent:] (macOS 10.11+) — start a window drag.
    public void performWindowDragWithEvent(NSEvent event) {
        if (event == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("performWindowDragWithEvent:"), event.peer());
    }

    /// [window toggleFullScreen:] — enter/exit full screen, sender may be null.
    public void toggleFullScreen(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleFullScreen:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window dataWithEPSInsideRect:] — EPS snapshot of `rect`.
    public NSData dataWithEPSInsideRect(NSRect rect) {
        try {
            MemorySegment d = (MemorySegment) H.hIdRect().invokeExact(peer,
                    ObjC.sel("dataWithEPSInsideRect:"), rect.toSegment());
            return NSData.wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("dataWithEPSInsideRect: failed", t);
        }
    }

    /// [window dataWithPDFInsideRect:] — PDF snapshot of `rect`.
    public NSData dataWithPDFInsideRect(NSRect rect) {
        try {
            MemorySegment d = (MemorySegment) H.hIdRect().invokeExact(peer,
                    ObjC.sel("dataWithPDFInsideRect:"), rect.toSegment());
            return NSData.wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("dataWithPDFInsideRect: failed", t);
        }
    }

    /// [window print:] — opens the print dialog for the window; sender may be null.
    /// Never call from an unattended test (modal UI).
    public void print(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("print:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window displayLinkWithTarget:selector:] — display-synced callback source.
    /// `selector` is a no-arg selector name on `target` (e.g. `"tick:"`).
    /// Invalidate the link when done.
    public CADisplayLink displayLinkWithTarget(NSObject target, String selector) {
        try {
            MemorySegment link = (MemorySegment) H.hIdIdId().invokeExact(peer,
                    ObjC.sel("displayLinkWithTarget:selector:"),
                    (MemorySegment) (target == null ? MemorySegment.NULL : target.peer()),
                    ObjC.sel(selector));
            return CADisplayLink.wrap(link);
        } catch (Throwable t) {
            throw new RuntimeException("displayLinkWithTarget:selector: failed", t);
        }
    }

    // ---- key-view loop ----

    /// [window setInitialFirstResponder:] — canonical setter name (the existing
    /// `initialFirstResponder(NSView)` setter is kept for compatibility).
    public void setInitialFirstResponder(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setInitialFirstResponder:"),
                (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
    }

    /// [window selectNextKeyView:] / selectPreviousKeyView: — sender may be null.
    public void selectNextKeyView(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("selectNextKeyView:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window selectPreviousKeyView:] — sender may be null.
    public void selectPreviousKeyView(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("selectPreviousKeyView:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window selectKeyViewFollowingView:].
    public void selectKeyViewFollowingView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("selectKeyViewFollowingView:"),
                view == null ? MemorySegment.NULL : view.peer());
    }

    /// [window selectKeyViewPrecedingView:].
    public void selectKeyViewPrecedingView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("selectKeyViewPrecedingView:"),
                view == null ? MemorySegment.NULL : view.peer());
    }

    /// [window keyViewSelectionDirection] — NSSelectionDirection.
    public long keyViewSelectionDirection() {
        return ObjC.msgSendLong(peer, ObjC.sel("keyViewSelectionDirection"));
    }

    /// [window defaultButtonCell] — raw peer (no NSButtonCell wrapper), may be nil.
    public MemorySegment defaultButtonCell() {
        return ObjC.msgSendId(peer, ObjC.sel("defaultButtonCell"));
    }

    /// [window setDefaultButtonCell:] — nil clears.
    public void setDefaultButtonCell(MemorySegment cell) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDefaultButtonCell:"),
                cell == null ? MemorySegment.NULL : cell);
    }

    /// [window disableKeyEquivalentForDefaultButtonCell].
    public void disableKeyEquivalentForDefaultButtonCell() {
        ObjC.msgSendVoid(peer, ObjC.sel("disableKeyEquivalentForDefaultButtonCell"));
    }

    /// [window enableKeyEquivalentForDefaultButtonCell].
    public void enableKeyEquivalentForDefaultButtonCell() {
        ObjC.msgSendVoid(peer, ObjC.sel("enableKeyEquivalentForDefaultButtonCell"));
    }

    /// [window autorecalculatesKeyViewLoop] / setter.
    public boolean autorecalculatesKeyViewLoop() {
        return ObjC.msgSendBool(peer, ObjC.sel("autorecalculatesKeyViewLoop"));
    }

    /// [window setAutorecalculatesKeyViewLoop:].
    public void setAutorecalculatesKeyViewLoop(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutorecalculatesKeyViewLoop:"), flag);
    }

    /// [window recalculateKeyViewLoop].
    public void recalculateKeyViewLoop() {
        ObjC.msgSendVoid(peer, ObjC.sel("recalculateKeyViewLoop"));
    }

    // ---- toolbar ----

    /// [window toolbar] — may be nil.
    public NSToolbar toolbar() {
        return NSToolbar.wrap(ObjC.msgSendId(peer, ObjC.sel("toolbar")));
    }

    /// [window setToolbar:] — nil detaches.
    public void setToolbar(NSToolbar toolbar) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setToolbar:"),
                toolbar == null ? MemorySegment.NULL : toolbar.peer());
    }

    /// [window toggleToolbarShown:] — sender may be null.
    public void toggleToolbarShown(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleToolbarShown:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window runToolbarCustomizationPalette:] — opens the customization sheet;
    /// sender may be null. Never call from an unattended test (modal UI).
    public void runToolbarCustomizationPalette(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("runToolbarCustomizationPalette:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    // ---- tabs ----

    /// [window tabbingIdentifier] (macOS 10.12+) — may be nil.
    public String tabbingIdentifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("tabbingIdentifier")));
    }

    /// [window setTabbingIdentifier:].
    public void setTabbingIdentifier(String identifier) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTabbingIdentifier:"),
                identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier));
    }

    /// [window selectNextTab:] — sender may be null.
    public void selectNextTab(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("selectNextTab:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window selectPreviousTab:] — sender may be null.
    public void selectPreviousTab(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("selectPreviousTab:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window moveTabToNewWindow:] — sender may be null.
    public void moveTabToNewWindow(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("moveTabToNewWindow:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window mergeAllWindows:] — sender may be null.
    public void mergeAllWindows(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("mergeAllWindows:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window toggleTabBar:] — sender may be null.
    public void toggleTabBar(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleTabBar:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window toggleTabOverview:] (macOS 10.13+) — sender may be null.
    public void toggleTabOverview(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("toggleTabOverview:"), sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window tabbedWindows] (macOS 10.12+) — may be nil when untabbed.
    public NSArray tabbedWindows() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("tabbedWindows")));
    }

    /// [window addTabbedWindow:ordered:] (macOS 10.12+) — `ordered`: -1/0/1.
    public void addTabbedWindow(NSWindow window, long ordered) {
        if (window == null) return;
        try {
            H.hVoidIdInt().invokeExact(peer, ObjC.sel("addTabbedWindow:ordered:"), window.peer(), ordered);
        } catch (Throwable t) {
            throw new RuntimeException("addTabbedWindow:ordered: failed", t);
        }
    }

    /// [window tab] (macOS 10.13+) — raw peer (no NSWindowTab wrapper), may be nil.
    public MemorySegment tab() {
        return ObjC.msgSendId(peer, ObjC.sel("tab"));
    }

    /// [window tabGroup] (macOS 10.13+) — raw peer (no NSWindowTabGroup wrapper).
    public MemorySegment tabGroup() {
        return ObjC.msgSendId(peer, ObjC.sel("tabGroup"));
    }

    // ---- sharing session state ----

    /// [window hasActiveWindowSharingSession] (macOS 13.3+).
    public boolean hasActiveWindowSharingSession() {
        return ObjC.msgSendBool(peer, ObjC.sel("hasActiveWindowSharingSession"));
    }

    /// [window windowTitlebarLayoutDirection] (macOS 10.12+) — 0=LTR, 1=RTL.
    public long windowTitlebarLayoutDirection() {
        return ObjC.msgSendLong(peer, ObjC.sel("windowTitlebarLayoutDirection"));
    }

    // ---- window event queue ----

    /// [window nextEventMatchingMask:] — next matching event, or null if none is
    /// queued. AppKit honesty: this BLOCKS until a matching event arrives — a
    /// mask that matches nothing (e.g. 0) waits forever (measured). Prefer
    /// `nextEventMatchingMaskUntilDate` with an expired date for polling.
    public NSEvent nextEventMatchingMask(long mask) {
        try {
            MemorySegment ev = (MemorySegment) H.hIdInt().invokeExact(peer,
                    ObjC.sel("nextEventMatchingMask:"), mask);
            return (ev == null || ev.address() == 0) ? null : new NSEvent(ev);
        } catch (Throwable t) {
            throw new RuntimeException("nextEventMatchingMask: failed", t);
        }
    }

    /// [window nextEventMatchingMask:untilDate:inMode:dequeue:] — `mode` is a
    /// run-loop mode name (e.g. `"kCFRunLoopDefaultMode"`); `untilDate` may be
    /// null (waits only when non-nil with a future date). Returns null on expiry.
    public NSEvent nextEventMatchingMaskUntilDate(long mask, NSDate untilDate, String mode, boolean dequeue) {
        try {
            MemorySegment ev = (MemorySegment) H.hIdIntIdIdBool().invokeExact(peer,
                    ObjC.sel("nextEventMatchingMask:untilDate:inMode:dequeue:"), mask,
                    (MemorySegment) (untilDate == null ? MemorySegment.NULL : untilDate.peer()),
                    ObjC.nsstring(mode), dequeue);
            return (ev == null || ev.address() == 0) ? null : new NSEvent(ev);
        } catch (Throwable t) {
            throw new RuntimeException("nextEventMatchingMask:untilDate:inMode:dequeue: failed", t);
        }
    }

    /// [window discardEventsMatchingMask:beforeEvent:] — drop queued events.
    public void discardEventsMatchingMaskBeforeEvent(long mask, NSEvent lastEvent) {
        try {
            H.hVoidIntId().invokeExact(peer, ObjC.sel("discardEventsMatchingMask:beforeEvent:"), mask,
                    (MemorySegment) (lastEvent == null ? MemorySegment.NULL : lastEvent.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("discardEventsMatchingMask:beforeEvent: failed", t);
        }
    }

    /// [window postEvent:atStart:] — enqueue an event (null event is a no-op).
    public void postEvent(NSEvent event, boolean atStart) {
        if (event == null) return;
        try {
            H.hVoidIdBool().invokeExact(peer, ObjC.sel("postEvent:atStart:"), event.peer(), atStart);
        } catch (Throwable t) {
            throw new RuntimeException("postEvent:atStart: failed", t);
        }
    }

    /// [window sendEvent:] — dispatch an event (null event is a no-op).
    public void sendEvent(NSEvent event) {
        if (event == null) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("sendEvent:"), event.peer());
    }

    /// [window currentEvent] — the event being dispatched, or null.
    public NSEvent currentEvent() {
        MemorySegment ev = ObjC.msgSendId(peer, ObjC.sel("currentEvent"));
        return (ev == null || ev.address() == 0) ? null : new NSEvent(ev);
    }

    // ---- mouse / cursor rects ----

    /// [window ignoresMouseEvents] / [window setIgnoresMouseEvents:].
    public boolean ignoresMouseEvents() {
        return ObjC.msgSendBool(peer, ObjC.sel("ignoresMouseEvents"));
    }

    /// [window setIgnoresMouseEvents:].
    public void setIgnoresMouseEvents(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setIgnoresMouseEvents:"), flag);
    }

    /// [window mouseLocationOutsideOfEventStream] — cursor in window-base coords.
    public NSPoint mouseLocationOutsideOfEventStream() {
        try {
            MemorySegment s = (MemorySegment) H.hGetPoint().invokeExact(ObjC.structSlot(), peer,
                    ObjC.sel("mouseLocationOutsideOfEventStream"));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("mouseLocationOutsideOfEventStream failed", t);
        }
    }

    /// [window disableCursorRects].
    public void disableCursorRects() {
        ObjC.msgSendVoid(peer, ObjC.sel("disableCursorRects"));
    }

    /// [window enableCursorRects].
    public void enableCursorRects() {
        ObjC.msgSendVoid(peer, ObjC.sel("enableCursorRects"));
    }

    /// [window discardCursorRects].
    public void discardCursorRects() {
        ObjC.msgSendVoid(peer, ObjC.sel("discardCursorRects"));
    }

    /// [window areCursorRectsEnabled].
    public boolean areCursorRectsEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("areCursorRectsEnabled"));
    }

    /// [window invalidateCursorRectsForView:] — nil is a no-op.
    public void invalidateCursorRectsForView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("invalidateCursorRectsForView:"),
                view == null ? MemorySegment.NULL : view.peer());
    }

    /// [window resetCursorRects].
    public void resetCursorRects() {
        ObjC.msgSendVoid(peer, ObjC.sel("resetCursorRects"));
    }

    // ---- drag and drop ----

    /// [window registerForDraggedTypes:] — pasteboard types the window accepts.
    public void registerForDraggedTypes(NSArray types) {
        ObjC.msgSendVoidId(peer, ObjC.sel("registerForDraggedTypes:"),
                types == null ? MemorySegment.NULL : types.peer());
    }

    /// [window unregisterDraggedTypes].
    public void unregisterDraggedTypes() {
        ObjC.msgSendVoid(peer, ObjC.sel("unregisterDraggedTypes"));
    }

    /// [window beginDraggingSessionWithItems:event:source:] (macOS 10.7+) —
    /// `items` is an NSArray of NSDraggingItem, `source` any NSObject.
    public NSDraggingSession beginDraggingSessionWithItems(NSArray items, NSEvent event, NSObject source) {
        if (items == null || event == null) return null;
        try {
            MemorySegment s = (MemorySegment) H.hIdIdId().invokeExact(peer,
                    ObjC.sel("beginDraggingSessionWithItems:event:source:"),
                    items.peer(), event.peer(),
                    (MemorySegment) (source == null ? MemorySegment.NULL : source.peer()));
            return NSDraggingSession.wrap(s);
        } catch (Throwable t) {
            throw new RuntimeException("beginDraggingSessionWithItems:event:source: failed", t);
        }
    }

    // ---------------------------------------------------------------- responder chain (NSWindow is an NSResponder)

    /// makeFirstResponder: — install `responder` as the window's first
    /// responder. The native selector returns a BOOL ("accepted?") that this
    /// void wrapper discards; check `firstResponder()` afterwards if acceptance
    /// matters. Views created via `NSView.create` accept only while a key
    /// listener is registered (`NSView.setKeyListener`).
    public void makeFirstResponder(NSView responder) {
        ObjC.msgSendVoidId(peer, ObjC.sel("makeFirstResponder:"),
                (MemorySegment)(responder == null ? MemorySegment.NULL : responder.peer()));
    }

    /// firstResponder — the window's current first responder, wrapped as an
    /// NSResponder. This is often the window itself when no view has key focus.
    public NSResponder firstResponder() {
        return NSResponder.wrap(ObjC.msgSendId(peer, ObjC.sel("firstResponder")));
    }

    /// setAcceptsMouseMovedEvents: — whether the window's views receive
    /// mouseMoved events. Off by default (they cost a message per pixel of
    /// cursor travel); turn on for hover UI, together with
    /// `NSView.enableMouseTracking` on the views that want the callbacks.
    public void setAcceptsMouseMovedEvents(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAcceptsMouseMovedEvents:"), flag);
    }

    /// acceptsMouseMovedEvents — whether mouse-moved events are delivered.
    public boolean acceptsMouseMovedEvents() {
        return ObjC.msgSendBool(peer, ObjC.sel("acceptsMouseMovedEvents"));
    }

    /// setInitialFirstResponder: — the view that becomes first responder when
    /// the window is shown (the `initialFirstResponder` outlet).
    public void initialFirstResponder(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setInitialFirstResponder:"),
                (MemorySegment)(view == null ? MemorySegment.NULL : view.peer()));
    }

    /// initialFirstResponder — the view set to take first-responder status when
    /// the window is shown (null if none was set).
    public NSView initialFirstResponder() {
        return NSView.wrap(ObjC.msgSendId(peer, ObjC.sel("initialFirstResponder")));
    }
}
