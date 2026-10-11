package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSScreen — one attached display: frame, visible frame, backing scale factor,
/// device description. Thin 1:1 wrapper; all behavior is AppKit's.
///
/// All screen frames live in one global coordinate space: the origin is the
/// upper-left corner of the primary screen, y grows downward, and screens other
/// than the primary may have negative coordinates. `frame` is the full screen
/// rectangle in points; `visibleFrame` excludes the menu bar, Dock, and other
/// system UI. `backingScaleFactor` converts points to pixels (2.0 on Retina).
///
/// Class-side `screens` and `mainScreen` are process-global queries — no
/// instance needed. `mainScreen` is the screen with the key window, falling
/// back to the primary display when no window is key.
///
/// Coverage notes (SDK: `NSScreen.h`):
/// - Wrapped: `screens`, `mainScreen`, `deepestScreen`, `screensHaveSeparateSpaces`,
///   `depth`, `frame`, `visibleFrame`, `deviceDescription`, `colorSpace`,
///   `supportedWindowDepths` (raw pointer), `canRepresentDisplayGamut:`,
///   `convertRectToBacking:`/`convertRectFromBacking:`, `backingScaleFactor`,
///   `localizedName`, `safeAreaInsets`, `auxiliaryTopLeftArea`/`auxiliaryTopRightArea`,
///   the three extended-dynamic-range maxima, `maximumFramesPerSecond`,
///   `minimumRefreshInterval`/`maximumRefreshInterval`, `displayUpdateGranularity`,
///   `lastDisplayUpdateTimestamp`, `CGDirectDisplayID`,
///   `displayLinkWithTarget:selector:`.
///   `safeAreaInsets` reuses the `of(RECT)` shape: `NSEdgeInsets` is 4 doubles
///   (32 bytes), layout-identical to `NSRect` for ABI purposes.
/// - OMITTED: `backingAlignedRect:options:` — needs shape
///   of(Ret.RECT, Arg.RECT, Arg.INT), not registered (requested below); `userSpaceScaleFactor`
///   (deprecated, always 1.0); `NSScreenColorSpaceDidChangeNotification` (a
///   notification name, not a method).
///
/// REQUESTED Sig shapes (absent from Sig.java):
///   of(Ret.RECT, Arg.RECT, Arg.INT).
public final class NSScreen extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hGetDouble, MethodHandle hBoolInt, MethodHandle hRectRect, MethodHandle hIdIdId) {}
    private static volatile Handles H;

    private NSScreen(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native NSScreen id (null for nil).
    public static NSScreen wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSScreen(peer);
    }

    private static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.INT)),
                ObjC.handle(Sig.of(Ret.RECT, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)));
    }

    /// [NSScreen screens] — an array of every display currently available to
    /// the application. Never nil; empty only before graphics initialize.
    public static NSArray screens() {
        return NSArray.wrap(ObjC.msgSendId(ObjC.cls("NSScreen"), ObjC.sel("screens")));
    }

    /// [NSScreen mainScreen] — the screen containing the window with keyboard
    /// focus, or the primary screen (origin of the global coordinate space)
    /// when no window is key.
    public static NSScreen mainScreen() {
        return wrap(ObjC.msgSendId(ObjC.cls("NSScreen"), ObjC.sel("mainScreen")));
    }

    /// Struct-returning message: the screen's full frame in global coordinates
    /// (objc_msgSend_stret on x86_64).
    public NSRect frame() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("frame")));
    }

    /// Struct-returning message: the portion of the screen usable for content —
    /// `frame` minus the menu bar, Dock, and other system UI.
    public NSRect visibleFrame() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("visibleFrame")));
    }

    /// [screen backingScaleFactor] — points-to-pixels multiplier: 1.0 on
    /// standard displays, 2.0 on Retina.
    public double backingScaleFactor() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, ObjC.sel("backingScaleFactor"));
        } catch (Throwable t) {
            throw new RuntimeException("backingScaleFactor failed", t);
        }
    }

    /// [screen deviceDescription] — dictionary describing the display: keys
    /// `NSDeviceResolution`, `NSDeviceColorSpaceName`, `NSScreenNumber` and
    /// friends. Non-nil for any valid screen.
    public NSDictionary deviceDescription() {
        return NSDictionary.wrap(ObjC.msgSendId(peer, ObjC.sel("deviceDescription")));
    }

    /// [screen colorSpace] — the display's color space as the raw peer (no
    /// NSColorSpace wrapper yet; use the ObjC escape hatch to message it).
    public MemorySegment colorSpace() {
        return ObjC.msgSendId(peer, ObjC.sel("colorSpace"));
    }

    // ---- class-side screen queries ----

    /// [NSScreen deepestScreen] — the deepest screen (menu-bar screen).
    public static NSScreen deepestScreen() {
        return wrap(ObjC.msgSendId(ObjC.cls("NSScreen"), ObjC.sel("deepestScreen")));
    }

    /// [NSScreen screensHaveSeparateSpaces] (macOS 10.9+) — one space set per screen.
    public static boolean screensHaveSeparateSpaces() {
        return ObjC.msgSendBool(ObjC.cls("NSScreen"), ObjC.sel("screensHaveSeparateSpaces"));
    }

    // ---- depth ----

    /// [screen depth] — NSWindowDepth of the display (NS_ENUM(int32_t), 32-bit signed).
    public long depth() {
        try {
            return (long) (int) ObjC.handle(Sig.of(Ret.INT32)).invokeExact(peer, ObjC.sel("depth"));
        } catch (Throwable t) {
            throw new RuntimeException("depth failed", t);
        }
    }

    /// [screen supportedWindowDepths] — NUL-terminated depth list, inner pointer.
    /// Raw peer; do not free. May be nil on failure.
    public MemorySegment supportedWindowDepths() {
        return ObjC.msgSendId(peer, ObjC.sel("supportedWindowDepths"));
    }

    /// [screen canRepresentDisplayGamut:] (macOS 10.12+) — `gamut`: 0=sRGB, 1=DisplayP3, ...
    public boolean canRepresentDisplayGamut(long gamut) {
        ensureInit();
        try {
            return (boolean) H.hBoolInt().invokeExact(peer, ObjC.sel("canRepresentDisplayGamut:"), gamut);
        } catch (Throwable t) {
            throw new RuntimeException("canRepresentDisplayGamut: failed", t);
        }
    }

    // ---- backing conversion ----

    /// [screen convertRectToBacking:] (macOS 10.7+) — points to backing pixels.
    public NSRect convertRectToBacking(NSRect rect) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("convertRectToBacking:"), rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectToBacking: failed", t);
        }
    }

    /// [screen convertRectFromBacking:] (macOS 10.7+) — backing pixels to points.
    public NSRect convertRectFromBacking(NSRect rect) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("convertRectFromBacking:"), rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectFromBacking: failed", t);
        }
    }

    // ---- names / areas ----

    /// [screen localizedName] (macOS 10.15+) — e.g. `"Built-in Retina Display"`.
    public String localizedName() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("localizedName")));
    }

    /// [screen safeAreaInsets] (macOS 12.0+) — obscured distance from each edge.
    public NSEdgeInsets safeAreaInsets() {
        if (!respondsToSelector(ObjC.sel("safeAreaInsets"))) return NSEdgeInsets.ZERO;   // macOS 12+
        return NSEdgeInsets.fromSegment(ObjC.msgSendEdgeInsets(peer, ObjC.sel("safeAreaInsets")));
    }

    /// [screen auxiliaryTopLeftArea] (macOS 12.0+) — unobscured area above the
    /// safe area, top-left; empty when none.
    public NSRect auxiliaryTopLeftArea() {
        if (!respondsToSelector(ObjC.sel("auxiliaryTopLeftArea"))) return NSRect.ZERO;   // macOS 12+
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("auxiliaryTopLeftArea")));
    }

    /// [screen auxiliaryTopRightArea] (macOS 12.0+) — mirror of top-left.
    public NSRect auxiliaryTopRightArea() {
        if (!respondsToSelector(ObjC.sel("auxiliaryTopRightArea"))) return NSRect.ZERO;   // macOS 12+
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("auxiliaryTopRightArea")));
    }

    // ---- extended dynamic range ----

    /// [screen maximumExtendedDynamicRangeColorComponentValue] (macOS 10.11+) —
    /// current EDR headroom; typically 1.0, higher when EDR is active.
    public double maximumExtendedDynamicRangeColorComponentValue() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer,
                    ObjC.sel("maximumExtendedDynamicRangeColorComponentValue"));
        } catch (Throwable t) {
            throw new RuntimeException("maximumExtendedDynamicRangeColorComponentValue failed", t);
        }
    }

    /// [screen maximumPotentialExtendedDynamicRangeColorComponentValue]
    /// (macOS 10.15+) — EDR ceiling regardless of current state.
    public double maximumPotentialExtendedDynamicRangeColorComponentValue() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer,
                    ObjC.sel("maximumPotentialExtendedDynamicRangeColorComponentValue"));
        } catch (Throwable t) {
            throw new RuntimeException("maximumPotentialExtendedDynamicRangeColorComponentValue failed", t);
        }
    }

    /// [screen maximumReferenceExtendedDynamicRangeColorComponentValue]
    /// (macOS 10.15+) — 0 when the screen has no reference mode.
    public double maximumReferenceExtendedDynamicRangeColorComponentValue() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer,
                    ObjC.sel("maximumReferenceExtendedDynamicRangeColorComponentValue"));
        } catch (Throwable t) {
            throw new RuntimeException("maximumReferenceExtendedDynamicRangeColorComponentValue failed", t);
        }
    }

    // ---- variable refresh ----

    /// [screen maximumFramesPerSecond] (macOS 12.0+).
    public long maximumFramesPerSecond() {
        if (!respondsToSelector(ObjC.sel("maximumFramesPerSecond"))) return 0L;   // macOS 12+
        return ObjC.msgSendLong(peer, ObjC.sel("maximumFramesPerSecond"));
    }

    /// [screen minimumRefreshInterval] (macOS 12.0+) — seconds.
    public double minimumRefreshInterval() {
        ensureInit();
        if (!respondsToSelector(ObjC.sel("minimumRefreshInterval"))) return 0.0;   // macOS 12+
        try {
            return (double) H.hGetDouble().invokeExact(peer, ObjC.sel("minimumRefreshInterval"));
        } catch (Throwable t) {
            throw new RuntimeException("minimumRefreshInterval failed", t);
        }
    }

    /// [screen maximumRefreshInterval] (macOS 12.0+) — seconds.
    public double maximumRefreshInterval() {
        ensureInit();
        if (!respondsToSelector(ObjC.sel("maximumRefreshInterval"))) return 0.0;   // macOS 12+
        try {
            return (double) H.hGetDouble().invokeExact(peer, ObjC.sel("maximumRefreshInterval"));
        } catch (Throwable t) {
            throw new RuntimeException("maximumRefreshInterval failed", t);
        }
    }

    /// [screen displayUpdateGranularity] (macOS 12.0+) — seconds.
    public double displayUpdateGranularity() {
        ensureInit();
        if (!respondsToSelector(ObjC.sel("displayUpdateGranularity"))) return 0.0;   // macOS 12+
        try {
            return (double) H.hGetDouble().invokeExact(peer, ObjC.sel("displayUpdateGranularity"));
        } catch (Throwable t) {
            throw new RuntimeException("displayUpdateGranularity failed", t);
        }
    }

    /// [screen lastDisplayUpdateTimestamp] (macOS 12.0+) — seconds since wake.
    public double lastDisplayUpdateTimestamp() {
        ensureInit();
        if (!respondsToSelector(ObjC.sel("lastDisplayUpdateTimestamp"))) return 0.0;   // macOS 12+
        try {
            return (double) H.hGetDouble().invokeExact(peer, ObjC.sel("lastDisplayUpdateTimestamp"));
        } catch (Throwable t) {
            throw new RuntimeException("lastDisplayUpdateTimestamp failed", t);
        }
    }

    /// [screen CGDirectDisplayID] (macOS 26.0+) — the CoreGraphics display id (uint32_t, 32-bit unsigned).
    public long cgDirectDisplayID() {
        if (!respondsToSelector(ObjC.sel("CGDirectDisplayID"))) return 0L;   // version-gated
        try {
            return ((int) ObjC.handle(Sig.of(Ret.INT32)).invokeExact(peer, ObjC.sel("CGDirectDisplayID"))) & 0xFFFFFFFFL;
        } catch (Throwable t) {
            throw new RuntimeException("CGDirectDisplayID failed", t);
        }
    }

    /// [screen displayLinkWithTarget:selector:] (macOS 14.0+) — display-synced
    /// callback source. `selector` is a selector NAME that the wrapper resolves to a SEL,
    /// so this String overload rejects null (`ObjC.sel(null)` would NPE); the selector
    /// value itself is not validated. Invalidate the link when done.
    /// The link is a `+0` (autoreleased) factory result: add it to a run loop before the
    /// current autorelease pool drains — the run loop retains it — or `keepAlive()` it.
    public CADisplayLink displayLinkWithTarget(NSObject target, String selector) {
        ensureInit();
        Bounds.nonNull(selector, "displayLinkWithTarget:selector: (selector name)");
        if (!respondsToSelector(ObjC.sel("displayLinkWithTarget:selector:"))) return null;   // macOS 14+
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
}
