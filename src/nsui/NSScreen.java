package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
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
public final class NSScreen extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hGetDouble) {}
    private static volatile Handles H;

    private NSScreen(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native NSScreen id (null for nil).
    public static NSScreen wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSScreen(peer);
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.DOUBLE)));
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
}
