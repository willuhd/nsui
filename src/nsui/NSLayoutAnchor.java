package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSLayoutAnchor — wrapper for native NSLayoutAnchor hierarchy.
 *
 * Covers XAxisAnchor, YAxisAnchor, and Dimension anchors with a single
 * stateless wrapper (SWT-style). NSView's anchor getters return this type;
 * callers may treat it as opaque and call constraint creation helpers.
 *
 * Lazy synchronized ensureInit + ObjC.handle mirrors NSView/NSLayoutConstraint.
 */
public class NSLayoutAnchor extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hEqualToAnchor;                 // (id, SEL, id) -> id
    private static MethodHandle hEqualToAnchorConstant;         // (id, SEL, id, double) -> id
    private static MethodHandle hGreaterThanOrEqualToAnchor;    // (id, SEL, id) -> id
    private static MethodHandle hLessThanOrEqualToAnchor;       // (id, SEL, id) -> id
    private static MethodHandle hEqualToConstant;               // (id, SEL, double) -> id  [Dimension]
    private static MethodHandle hEqualToAnchorMultiplier;       // (id, SEL, id, double) -> id (Dimension multiplier variant)
    private static MethodHandle hEqualToAnchorMultiplierConstant; // (id, SEL, id, double, double) -> id

    protected NSLayoutAnchor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSLayoutAnchor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSLayoutAnchor(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hEqualToAnchor = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hEqualToAnchorConstant = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.DOUBLE));
        hGreaterThanOrEqualToAnchor = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hLessThanOrEqualToAnchor = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hEqualToConstant = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
        // multiplier variants: try to resolve, fall back to generic escape if vocab missing
        try {
            hEqualToAnchorMultiplier = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.DOUBLE));
        } catch (Exception e) {
            hEqualToAnchorMultiplier = hEqualToAnchorConstant;
        }
        try {
            hEqualToAnchorMultiplierConstant = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.DOUBLE, Arg.DOUBLE));
        } catch (Exception e) {
            hEqualToAnchorMultiplierConstant = null;
        }
        initialized = true;
    }

    // ---- axis anchor constraints ----

    public NSLayoutConstraint constraintEqualToAnchor(NSLayoutAnchor anchor) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hEqualToAnchor.invokeExact(peer, ObjC.sel("constraintEqualToAnchor:"), anchor.peer());
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintEqualToAnchor: failed", t);
        }
    }

    public NSLayoutConstraint constraintEqualToAnchor(NSLayoutAnchor anchor, double constant) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hEqualToAnchorConstant.invokeExact(peer, ObjC.sel("constraintEqualToAnchor:constant:"), anchor.peer(), constant);
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintEqualToAnchor:constant: failed", t);
        }
    }

    public NSLayoutConstraint constraintGreaterThanOrEqualToAnchor(NSLayoutAnchor anchor) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hGreaterThanOrEqualToAnchor.invokeExact(peer, ObjC.sel("constraintGreaterThanOrEqualToAnchor:"), anchor.peer());
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintGreaterThanOrEqualToAnchor: failed", t);
        }
    }

    public NSLayoutConstraint constraintGreaterThanOrEqualToAnchor(NSLayoutAnchor anchor, double constant) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hEqualToAnchorConstant.invokeExact(peer, ObjC.sel("constraintGreaterThanOrEqualToAnchor:constant:"), anchor.peer(), constant);
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintGreaterThanOrEqualToAnchor:constant: failed", t);
        }
    }

    public NSLayoutConstraint constraintLessThanOrEqualToAnchor(NSLayoutAnchor anchor) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hLessThanOrEqualToAnchor.invokeExact(peer, ObjC.sel("constraintLessThanOrEqualToAnchor:"), anchor.peer());
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintLessThanOrEqualToAnchor: failed", t);
        }
    }

    public NSLayoutConstraint constraintLessThanOrEqualToAnchor(NSLayoutAnchor anchor, double constant) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hEqualToAnchorConstant.invokeExact(peer, ObjC.sel("constraintLessThanOrEqualToAnchor:constant:"), anchor.peer(), constant);
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintLessThanOrEqualToAnchor:constant: failed", t);
        }
    }

    // ---- dimension anchor constraints ----

    /** For NSLayoutDimension: constraintEqualToConstant: */
    public NSLayoutConstraint constraintEqualToConstant(double constant) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hEqualToConstant.invokeExact(peer, ObjC.sel("constraintEqualToConstant:"), constant);
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintEqualToConstant: failed", t);
        }
    }

    public NSLayoutConstraint constraintGreaterThanOrEqualToConstant(double constant) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hEqualToConstant.invokeExact(peer, ObjC.sel("constraintGreaterThanOrEqualToConstant:"), constant);
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintGreaterThanOrEqualToConstant: failed", t);
        }
    }

    public NSLayoutConstraint constraintLessThanOrEqualToConstant(double constant) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hEqualToConstant.invokeExact(peer, ObjC.sel("constraintLessThanOrEqualToConstant:"), constant);
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintLessThanOrEqualToConstant: failed", t);
        }
    }

    /** Dimension: constraintEqualToAnchor:multiplier:constant: (3-arg) */
    public NSLayoutConstraint constraintEqualToAnchorWithMultiplier(NSLayoutAnchor anchor, double multiplier, double constant) {
        ensureInit();
        if (hEqualToAnchorMultiplierConstant != null) {
            try {
                MemorySegment c = (MemorySegment) hEqualToAnchorMultiplierConstant.invokeExact(peer, ObjC.sel("constraintEqualToAnchor:multiplier:constant:"), anchor.peer(), multiplier, constant);
                return NSLayoutConstraint.wrap(c);
            } catch (Throwable t) {
                throw new RuntimeException("constraintEqualToAnchor:multiplier:constant: failed", t);
            }
        }
        return constraintEqualToAnchor(anchor, constant);
    }

    /** Dimension: constraintEqualToAnchor:multiplier: (single multiplier) */
    public NSLayoutConstraint constraintEqualToAnchorWithMultiplier(NSLayoutAnchor anchor, double multiplier) {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) hEqualToAnchorMultiplier.invokeExact(peer, ObjC.sel("constraintEqualToAnchor:multiplier:"), anchor.peer(), multiplier);
            return NSLayoutConstraint.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintEqualToAnchor:multiplier: failed", t);
        }
    }

    /** Keep 3-arg overload under original name for callers using (anchor,multiplier,constant) */
    public NSLayoutConstraint constraintEqualToAnchor(NSLayoutAnchor anchor, double multiplier, double constant) {
        return constraintEqualToAnchorWithMultiplier(anchor, multiplier, constant);
    }

    // ---- typed subclasses for clarity (all share impl) ----

    public static final class XAxis extends NSLayoutAnchor {
        XAxis(MemorySegment peer) { super(peer); }
        public static XAxis wrapX(MemorySegment p) { return p == null || p.address()==0 ? null : new XAxis(p); }
    }

    public static final class YAxis extends NSLayoutAnchor {
        YAxis(MemorySegment peer) { super(peer); }
        public static YAxis wrapY(MemorySegment p) { return p == null || p.address()==0 ? null : new YAxis(p); }
    }

    public static final class Dimension extends NSLayoutAnchor {
        Dimension(MemorySegment peer) { super(peer); }
        public static Dimension wrapD(MemorySegment p) { return p == null || p.address()==0 ? null : new Dimension(p); }
    }
}
