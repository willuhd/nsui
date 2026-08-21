package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSAppearance — thin, 1:1, stateless wrapper over native `NSAppearance`.
/// Every method maps to one `objc_msgSend` selector and no Java state is
/// cached beyond the peer. Follows the project template: volatile Handles
/// record, synchronized ensureInit, ObjC.handle(Sig.of...), invokeExact.
///
/// AppKit notes:
/// - `appearanceNamed:` — class factory for named appearances such as
///   `NSAppearanceNameAqua` / `NSAppearanceNameDarkAqua`.
/// - `currentAppearance` — the thread's current appearance (class method).
/// - `effectiveAppearance` — the view/window's resolved appearance.
/// - `name` — the appearance's name string (e.g. `NSAppearanceNameAqua`).
public final class NSAppearance extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hAppearanceNamed, MethodHandle hCurrentAppearance, MethodHandle hEffectiveAppearance, MethodHandle hBestMatch) {}
    private static volatile Handles handles;

    private NSAppearance(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native NSAppearance id as an NSAppearance (null for nil).
    public static NSAppearance wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSAppearance(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID))
        );
    }

    /// `+[NSAppearance appearanceNamed:]` — appearance for the given name, or nil if not found.
    public static NSAppearance appearanceNamed(String name) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hAppearanceNamed().invokeExact(
                    ObjC.cls("NSAppearance"), ObjC.sel("appearanceNamed:"), ObjC.nsstring(name));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("appearanceNamed: failed for " + name, t);
        }
    }

    /// Raw peer variant: appearanceNamed: with id.
    public static NSAppearance appearanceNamed(MemorySegment nameId) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hAppearanceNamed().invokeExact(
                    ObjC.cls("NSAppearance"), ObjC.sel("appearanceNamed:"),
                    (MemorySegment) (nameId == null ? MemorySegment.NULL : nameId));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("appearanceNamed: failed (peer variant)", t);
        }
    }

    /// `+[NSAppearance currentAppearance]` — the thread's current appearance.
    public static NSAppearance currentAppearance() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hCurrentAppearance().invokeExact(
                    ObjC.cls("NSAppearance"), ObjC.sel("currentAppearance"));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("currentAppearance failed", t);
        }
    }

    /// `+[NSAppearance currentAppearance]` raw peer.
    public static MemorySegment currentAppearancePeer() {
        ensureInit();
        try {
            return (MemorySegment) handles.hCurrentAppearance().invokeExact(
                    ObjC.cls("NSAppearance"), ObjC.sel("currentAppearance"));
        } catch (Throwable t) {
            throw new RuntimeException("currentAppearance failed", t);
        }
    }

    /// `-[NSAppearance name]` — the appearance name string.
    public String name() {
        MemorySegment s = ObjC.msgSendId(peer, ObjC.sel("name"));
        return ObjC.toString(s);
    }

    /// `-[NSAppearance bestMatchFromAppearancesWithNames:]` — best match for the given names array.
    /// The array should be an NSArray of NSString ids. Returns the best matching appearance name string id,
    /// but we wrap the returned NSAppearance if the runtime returns an appearance; if it returns a string,
    /// we fall back to appearanceNamed. We expose the direct peer for caller flexibility.
    public MemorySegment bestMatchFromAppearancesWithNames(MemorySegment namesArray) {
        ensureInit();
        try {
            return (MemorySegment) handles.hBestMatch().invokeExact(
                    peer, ObjC.sel("bestMatchFromAppearancesWithNames:"), namesArray);
        } catch (Throwable t) {
            throw new RuntimeException("bestMatchFromAppearancesWithNames: failed", t);
        }
    }

    // ---------------------------------------------------------------- static helpers for NSView/NSWindow (do NOT modify NSView.java)

    /// `[view setAppearance:appearance]` — assign an appearance to a view.
    public static void setAppearance(NSView view, NSAppearance ap) {
        if (view == null) throw new IllegalArgumentException("view == null");
        ensureInit();
        // Use void(id) handle — reuse existing vocab entry Sig.of(VOID, ID)
        try {
            MethodHandle hSet = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            MemorySegment apPeer = (ap == null || ap.peer() == null || ap.peer().address() == 0)
                    ? MemorySegment.NULL : ap.peer();
            hSet.invokeExact(view.peer(), ObjC.sel("setAppearance:"), apPeer);
        } catch (Throwable t) {
            throw new RuntimeException("setAppearance: failed", t);
        }
    }

    /// Raw peer variant for setAppearance.
    public static void setAppearancePeer(NSView view, MemorySegment apPeer) {
        if (view == null) throw new IllegalArgumentException("view == null");
        try {
            MethodHandle hSet = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            MemorySegment p = (apPeer == null || apPeer.address() == 0) ? MemorySegment.NULL : apPeer;
            hSet.invokeExact(view.peer(), ObjC.sel("setAppearance:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("setAppearance: failed", t);
        }
    }

    /// `[view effectiveAppearance]` — the view's resolved effective appearance.
    public static NSAppearance effectiveAppearance(NSView view) {
        if (view == null) throw new IllegalArgumentException("view == null");
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hEffectiveAppearance().invokeExact(
                    view.peer(), ObjC.sel("effectiveAppearance"));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("effectiveAppearance failed", t);
        }
    }

    /// Raw peer for effectiveAppearance.
    public static MemorySegment effectiveAppearancePeer(NSView view) {
        if (view == null) throw new IllegalArgumentException("view == null");
        ensureInit();
        try {
            return (MemorySegment) handles.hEffectiveAppearance().invokeExact(
                    view.peer(), ObjC.sel("effectiveAppearance"));
        } catch (Throwable t) {
            throw new RuntimeException("effectiveAppearance failed", t);
        }
    }
}
