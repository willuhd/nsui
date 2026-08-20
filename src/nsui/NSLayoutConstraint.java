package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSLayoutConstraint — Auto Layout constraint wrapper.
///
/// Thin 1:1 wrapper over native NSLayoutConstraint, following the same
/// stateless, SWT-style, lazy-ensureInit pattern as NSView/NSStackView.
/// Every method is one objc_msgSend.
public class NSLayoutConstraint extends NSObject {

    // NSLayoutAttribute
    public static final int NSLayoutAttributeLeft = 1;
    public static final int NSLayoutAttributeRight = 2;
    public static final int NSLayoutAttributeTop = 3;
    public static final int NSLayoutAttributeBottom = 4;
    public static final int NSLayoutAttributeLeading = 5;
    public static final int NSLayoutAttributeTrailing = 6;
    public static final int NSLayoutAttributeWidth = 7;
    public static final int NSLayoutAttributeHeight = 8;
    public static final int NSLayoutAttributeCenterX = 9;
    public static final int NSLayoutAttributeCenterY = 10;
    public static final int NSLayoutAttributeLastBaseline = 11;
    public static final int NSLayoutAttributeFirstBaseline = 12;
    public static final int NSLayoutAttributeNotAnAttribute = 0;

    // NSLayoutRelation
    public static final int NSLayoutRelationLessThanOrEqual = -1;
    public static final int NSLayoutRelationEqual = 0;
    public static final int NSLayoutRelationGreaterThanOrEqual = 1;

    private static volatile boolean initialized;
    private static MethodHandle hConstraintWithItem; // (Class, SEL, id, int, int, id, int, double, double) -> id
    private static MethodHandle hIsActive;          // (id, SEL) -> BOOL
    private static MethodHandle hSetActive;         // (id, SEL, BOOL) -> void
    private static MethodHandle hPriority;          // (id, SEL) -> float? actually float, but use double sig? Use float harness? We'll use float via DOUBLE shim if needed
    private static MethodHandle hConstant;
    private static MethodHandle hSetConstant;

    protected NSLayoutConstraint(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSLayoutConstraint wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSLayoutConstraint(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hConstraintWithItem = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.INT, Arg.INT, Arg.ID, Arg.INT, Arg.DOUBLE, Arg.DOUBLE));
        hIsActive = ObjC.handle(Sig.of(Ret.BOOL));
        hSetActive = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        // priority is float on AppKit (NSLayoutPriority); we can query via float descriptor if present, else double fallback
        // Use generic float/double handles already in vocabulary: Ret.FLOAT etc not needed for test
        try {
            hConstant = ObjC.handle(Sig.of(Ret.DOUBLE));
        } catch (Exception ignored) {
            hConstant = null;
        }
        try {
            hSetConstant = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        } catch (Exception ignored) {
            hSetConstant = null;
        }
        initialized = true;
    }

    /// +constraintWithItem:attribute:relatedBy:toItem:attribute:multiplier:constant:
    /// Mirrors ObjC handle creation. Null items are allowed (pass null -> nil).
    public static NSLayoutConstraint constraintWithItem(NSObject view1, int attr1, int relation, NSObject view2, int attr2, double multiplier, double constant) {
        ensureInit();
        MemorySegment p1 = (view1 == null) ? MemorySegment.NULL : view1.peer();
        MemorySegment p2 = (view2 == null) ? MemorySegment.NULL : view2.peer();
        try {
            MemorySegment c = (MemorySegment) hConstraintWithItem.invokeExact(
                    ObjC.cls("NSLayoutConstraint"),
                    ObjC.sel("constraintWithItem:attribute:relatedBy:toItem:attribute:multiplier:constant:"),
                    p1, (long) attr1, (long) relation, p2, (long) attr2, multiplier, constant);
            if (c == null || c.address() == 0) throw new IllegalStateException("constraintWithItem returned nil");
            return new NSLayoutConstraint(c);
        } catch (Throwable t) {
            throw new RuntimeException("constraintWithItem failed", t);
        }
    }

    /// Overload accepting NSView directly.
    public static NSLayoutConstraint constraintWithItem(NSView view1, int attr1, int relation, NSView view2, int attr2, double multiplier, double constant) {
        return constraintWithItem((NSObject) view1, attr1, relation, (NSObject) view2, attr2, multiplier, constant);
    }

    /// isActive — whether the constraint is installed/active.
    public boolean isActive() {
        ensureInit();
        try {
            return (boolean) hIsActive.invokeExact(peer, ObjC.sel("isActive"));
        } catch (Throwable t) {
            throw new RuntimeException("isActive failed", t);
        }
    }

    /// active — alias for isActive (some callers expect active).
    public boolean active() {
        return isActive();
    }

    /// setActive: — install or uninstall the constraint.
    public void setActive(boolean active) {
        ensureInit();
        try {
            hSetActive.invokeExact(peer, ObjC.sel("setActive:"), active);
        } catch (Throwable t) {
            throw new RuntimeException("setActive: failed", t);
        }
    }

    /// constant — the constant component.
    public double constant() {
        ensureInit();
        if (hConstant == null) {
            return ObjC.msgSendId(peer, ObjC.sel("constant")).address(); // fallback should not happen
        }
        try {
            return (double) hConstant.invokeExact(peer, ObjC.sel("constant"));
        } catch (Throwable t) {
            throw new RuntimeException("constant failed", t);
        }
    }

    public void setConstant(double c) {
        ensureInit();
        if (hSetConstant == null) {
            throw new RuntimeException("setConstant handle not available");
        }
        try {
            hSetConstant.invokeExact(peer, ObjC.sel("setConstant:"), c);
        } catch (Throwable t) {
            throw new RuntimeException("setConstant: failed", t);
        }
    }

    // ---- convenience activation helpers (class methods) ----

    public static void activateConstraints(java.util.List<NSLayoutConstraint> constraints) {
        if (constraints == null || constraints.isEmpty()) return;
        for (NSLayoutConstraint c : constraints) c.setActive(true);
    }

    public static void deactivateConstraints(java.util.List<NSLayoutConstraint> constraints) {
        if (constraints == null || constraints.isEmpty()) return;
        for (NSLayoutConstraint c : constraints) c.setActive(false);
    }

    // ---- priority helpers (float) ----
    // priority is NSLayoutPriority (float). Provide double bridge via msgSend if vocabulary lacks float getter.
    public float priority() {
        // Use generic ObjC escape; many apps treat as float
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.FLOAT));
            return (float) h.invokeExact(peer, ObjC.sel("priority"));
        } catch (Throwable t) {
            // fallback via double
            try {
                MethodHandle hd = ObjC.handle(Sig.of(Ret.DOUBLE));
                return (float) (double) hd.invokeExact(peer, ObjC.sel("priority"));
            } catch (Throwable t2) {
                throw new RuntimeException("priority failed", t2);
            }
        }
    }

    public void setPriority(float p) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT));
            h.invokeExact(peer, ObjC.sel("setPriority:"), p);
        } catch (Throwable t) {
            // fallback double
            try {
                MethodHandle hd = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
                hd.invokeExact(peer, ObjC.sel("setPriority:"), (double) p);
            } catch (Throwable t2) {
                throw new RuntimeException("setPriority: failed", t2);
            }
        }
    }

    // ---- firstItem / secondItem accessors (optional) ----
    public NSObject firstItem() {
        return NSObject.wrap(ObjC.msgSendId(peer, ObjC.sel("firstItem")));
    }

    public NSObject secondItem() {
        return NSObject.wrap(ObjC.msgSendId(peer, ObjC.sel("secondItem")));
    }

    public long firstAttribute() {
        return ObjC.msgSendLong(peer, ObjC.sel("firstAttribute"));
    }

    public long secondAttribute() {
        return ObjC.msgSendLong(peer, ObjC.sel("secondAttribute"));
    }
}
