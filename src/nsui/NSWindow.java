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
    private record Handles(MethodHandle hSetFrameDisplay, MethodHandle hSetFrameOrigin, MethodHandle hSetContentSize, MethodHandle hStdWinButton, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetSize, MethodHandle hSetSize) {}
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
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)));
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
        MemorySegment cb = ObjC.msgSendRect(cv, ObjC.sel("bounds"));
        return frame().height() - ObjC.rectH(cb);
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
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("minSize"));
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
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("maxSize"));
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
