package nsui;

import java.lang.foreign.MemorySegment;

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

    protected NSObject(MemorySegment peer) {
        this.peer = peer;
    }

    public MemorySegment peer() { return peer; }

    /** Wrap an id as an NSObject (null for nil). */
    public static NSObject wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSObject(peer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Long.toHexString(peer.address());
    }
}
