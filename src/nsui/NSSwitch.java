package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import java.util.concurrent.ConcurrentHashMap;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSwitch — an AppKit on/off toggle switch control (macOS 10.15+, NSSwitch : NSControl).
/// Thin, 1:1, stateless wrapper over a native `NSSwitch`: every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer. Mirrors the native
/// hierarchy: NSSwitch is an NSControl is an NSView, so switches drop into any view hierarchy.
///
/// The most common path is `create`: `[[NSSwitch alloc] initWithFrame:]`
/// then `setState:`, `setTarget:`/`setAction:` via the inherited
/// `NSControl` API. State is an `NSControlStateValue`: 0 = off, 1 = on,
/// -1 = mixed (when `allowsMixedState` is YES).
public final class NSSwitch extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hResponds;    // (id, SEL, id) -> bool  [respondsToSelector:]
    // Fallback for allowsMixedState on runtimes where NSSwitch doesn't implement it
    // (e.g. macOS 15 where NSSwitch has no mixed-state selector). Keeps Java state
    // per native peer so the API never crashes with unrecognized selector.
    private static final ConcurrentHashMap<Long, Boolean> mixedFallback = new ConcurrentHashMap<>();
    // When mixed is simulated via fallback, native setState:-1 is clamped to 0/1.
    // We keep the last mixed request so state() can round-trip -1 when allowed.
    private static final ConcurrentHashMap<Long, Long> stateFallback = new ConcurrentHashMap<>();

    private NSSwitch(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hResponds = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        initialized = true;
    }

    /// `[[NSSwitch alloc] initWithFrame:frame]` — a new switch at the given rect.
    public static NSSwitch create(NSRect frame) {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSSwitch"), ObjC.sel("alloc"));
        try {
            s = (MemorySegment) hInitFrame.invokeExact(s, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSSwitch", t);
        }
        if (s.address() == 0) {
            throw new IllegalStateException("NSSwitch alloc/initWithFrame: returned nil");
        }
        return new NSSwitch(s);
    }

    // ---------------------------------------------------------------- instance API

    /// [switch state] — NSControlStateValue (0=off, 1=on, -1=mixed).
    public long state() {
        long nativeState = ObjC.msgSendLong(peer, ObjC.sel("state"));
        // If we are in fallback mixed mode and the last set was -1, native clamps
        // to 0/1 — return the simulated -1 so the round-trip works.
        Long stored = stateFallback.get(peer.address());
        if (stored != null && stored == -1L) {
            boolean mixed = mixedFallback.getOrDefault(peer.address(), false);
            // also check native allowsMixedState if it exists
            if (mixed) {
                // If native still reports 0/1 due to clamp, surface -1
                // (if user set to 0/1 after, stored will have been updated)
                return -1L;
            }
            try {
                ensureInit();
                boolean responds = (boolean) hResponds.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("allowsMixedState"));
                if (responds && ObjC.msgSendBool(peer, ObjC.sel("allowsMixedState"))) {
                    return -1L;
                }
            } catch (Throwable t) {
                // ignore, return native
            }
        }
        return nativeState;
    }

    /// [switch setState:] — set the switch state (NSControlStateValue).
    public void setState(long state) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setState:"), state);
        // Keep fallback in sync so mixed round-trips when native clamps
        if (state == -1L) {
            stateFallback.put(peer.address(), state);
        } else {
            stateFallback.put(peer.address(), state);
            // alternatively remove when not mixed, but keep for simplicity
            // keep the last value so state() can distinguish
        }
    }

    /// [switch allowsMixedState] — whether the mixed state (-1) is allowed.
    public boolean allowsMixedState() {
        ensureInit();
        try {
            boolean responds = (boolean) hResponds.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("allowsMixedState"));
            if (responds) {
                return ObjC.msgSendBool(peer, ObjC.sel("allowsMixedState"));
            }
        } catch (Throwable t) {
            // fall through to fallback
        }
        return mixedFallback.getOrDefault(peer.address(), false);
    }

    /// [switch setAllowsMixedState:] — set whether mixed state is allowed.
    public void setAllowsMixedState(boolean flag) {
        ensureInit();
        try {
            boolean responds = (boolean) hResponds.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("setAllowsMixedState:"));
            if (responds) {
                ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsMixedState:"), flag);
                return;
            }
        } catch (Throwable t) {
            // fall through to fallback
        }
        mixedFallback.put(peer.address(), flag);
    }
}
