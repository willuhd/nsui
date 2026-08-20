package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import nsui.objc.Scratch;

/// NSEdgeInsets as a Java value type: `{top, left, bottom, right`} in points.
/// Marshals to/from the FFM struct segment only at the call boundary.
/// Layout: 4 doubles (top at 0, left at 8, bottom at 16, right at 24) — 32 bytes total.
/// Layout-identical to NSRect (4 doubles) for ABI purposes, but semantically distinct.
public record NSEdgeInsets(double top, double left, double bottom, double right) {

    public static final NSEdgeInsets ZERO = new NSEdgeInsets(0, 0, 0, 0);

    /// 32-byte segment: top at 0, left at 8, bottom at 16, right at 24.
    public MemorySegment toSegment() {
        MemorySegment s = Scratch.alloc(32);
        s.set(ValueLayout.JAVA_DOUBLE, 0, top);
        s.set(ValueLayout.JAVA_DOUBLE, 8, left);
        s.set(ValueLayout.JAVA_DOUBLE, 16, bottom);
        s.set(ValueLayout.JAVA_DOUBLE, 24, right);
        return s;
    }

    /// Read an NSEdgeInsets struct segment: 4 doubles at 0/8/16/24.
    public static NSEdgeInsets fromSegment(MemorySegment s) {
        return new NSEdgeInsets(
                s.get(ValueLayout.JAVA_DOUBLE, 0),
                s.get(ValueLayout.JAVA_DOUBLE, 8),
                s.get(ValueLayout.JAVA_DOUBLE, 16),
                s.get(ValueLayout.JAVA_DOUBLE, 24));
    }

    /// NSEdgeInsetsMake equivalent.
    public static NSEdgeInsets make(double top, double left, double bottom, double right) {
        return new NSEdgeInsets(top, left, bottom, right);
    }

    /// Horizontal sum left+right.
    public double horizontal() { return left + right; }

    /// Vertical sum top+bottom.
    public double vertical() { return top + bottom; }

    /// Is zero insets.
    public boolean isZero() { return top == 0 && left == 0 && bottom == 0 && right == 0; }

    /// Inset a rect by these edge insets.
    public NSRect insetRect(NSRect rect) {
        return new NSRect(rect.x() + left, rect.y() + bottom,
                rect.width() - left - right, rect.height() - top - bottom);
    }

    /// Inverted (negated) insets.
    public NSEdgeInsets negated() { return new NSEdgeInsets(-top, -left, -bottom, -right); }

    /// Add insets.
    public NSEdgeInsets add(NSEdgeInsets other) {
        return new NSEdgeInsets(top + other.top, left + other.left, bottom + other.bottom, right + other.right);
    }

    /// Equality with epsilon.
    public boolean epsilonEquals(NSEdgeInsets other, double eps) {
        return Math.abs(top - other.top) < eps && Math.abs(left - other.left) < eps
                && Math.abs(bottom - other.bottom) < eps && Math.abs(right - other.right) < eps;
    }

    @Override public String toString() { return "NSEdgeInsets{top=" + top + ", left=" + left + ", bottom=" + bottom + ", right=" + right + "}"; }
}
