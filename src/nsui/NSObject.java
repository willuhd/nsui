package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;

/**
 * L1 base class: a thin, stateless wrapper over a native Objective-C object (id).
 *
 * <p>Design rules (SWT-style): one wrapper per native object the toolkit owns;
 * transient msgSend results never create wrappers; the peer is the identity —
 * no state, no caching, no reflection. The wrapper exists to give selectors
 * Java-shaped signatures, nothing more.
 */
public class NSObject {

    /** The native Objective-C object (id). */
    protected final MemorySegment peer;

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hIsKind; // (id, SEL, id) -> bool [isKindOfClass:]

    protected NSObject(MemorySegment peer) {
        this.peer = peer;
    }

    public MemorySegment peer() { return peer; }

    /** Wrap an id as an NSObject (null for nil). */
    public static NSObject wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSObject(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hIsKind = ObjC.handle(Sig.of(Sig.Ret.BOOL, Sig.Arg.ID));
        initialized = true;
    }

    /**
     * {@code [receiver isKindOfClass:cls]} — runtime type check.
     * Convenience wrapper around the ObjC {@code isKindOfClass:} selector so tests
     * can verify the peer's class without reaching into {@link ObjC} directly.
     * Moved here from NSSearchField so every NSView/NSControl inherits it.
     */
    public boolean isKindOfClass(MemorySegment clazz) {
        ensureInit();
        try {
            return (boolean) hIsKind.invokeExact(peer, ObjC.sel("isKindOfClass:"), clazz);
        } catch (Throwable t) {
            throw new RuntimeException("isKindOfClass: failed", t);
        }
    }

    /** {@code isKindOfClass:} by class name (cached via {@link ObjC#cls}). */
    public boolean isKindOfClass(String className) {
        return isKindOfClass(ObjC.cls(className));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Long.toHexString(peer.address());
    }
}
