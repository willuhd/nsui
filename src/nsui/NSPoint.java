package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import nsui.objc.Scratch;

/**
 * NSPoint as a Java value type: {@code {x, y}} in points (CGFloat doubles).
 * Marshals to/from the FFM struct segment only at the call boundary.
 */
public record NSPoint(double x, double y) {

    public static final NSPoint ZERO = new NSPoint(0, 0);

    /** 16-byte segment in the global arena: x at offset 0, y at offset 8. */
    public MemorySegment toSegment() {
        MemorySegment s = Scratch.alloc(16);
        s.set(ValueLayout.JAVA_DOUBLE, 0, x);
        s.set(ValueLayout.JAVA_DOUBLE, 8, y);
        return s;
    }

    /** Read an NSPoint struct segment: double x at 0, double y at 8. */
    public static NSPoint fromSegment(MemorySegment s) {
        return new NSPoint(s.get(ValueLayout.JAVA_DOUBLE, 0), s.get(ValueLayout.JAVA_DOUBLE, 8));
    }

    /** NSMakePoint equivalent. */
    public static NSPoint make(double x, double y) { return new NSPoint(x, y); }

    /** Offset by dx, dy. */
    public NSPoint offset(double dx, double dy) { return new NSPoint(x + dx, y + dy); }

    /** Negated. */
    public NSPoint negated() { return new NSPoint(-x, -y); }

    /** Distance to another point. */
    public double distanceTo(NSPoint other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Midpoint between this and another point. */
    public NSPoint midPoint(NSPoint other) {
        return new NSPoint((x + other.x) / 2, (y + other.y) / 2);
    }

    /** Check if point is inside a rect (inclusive min edge, exclusive max). */
    public boolean inRect(NSRect rect) {
        return x >= rect.x() && x < rect.x() + rect.width() && y >= rect.y() && y < rect.y() + rect.height();
    }

    /** Clamp to rect. */
    public NSPoint clamped(NSRect rect) {
        double nx = Math.max(rect.x(), Math.min(x, rect.x() + rect.width()));
        double ny = Math.max(rect.y(), Math.min(y, rect.y() + rect.height()));
        return new NSPoint(nx, ny);
    }

    /** Convert to NSSize (x→width, y→height). */
    public NSSize toSize() { return new NSSize(x, y); }

    /** Scale. */
    public NSPoint scaled(double sx, double sy) { return new NSPoint(x * sx, y * sy); }

    /** Add size as vector. */
    public NSPoint add(NSSize size) { return new NSPoint(x + size.width(), y + size.height()); }

    /** Subtract. */
    public NSPoint subtract(NSPoint other) { return new NSPoint(x - other.x, y - other.y); }

    /** Equality with epsilon. */
    public boolean epsilonEquals(NSPoint other, double eps) {
        return Math.abs(x - other.x) < eps && Math.abs(y - other.y) < eps;
    }

    @Override public String toString() { return "NSPoint{x=" + x + ", y=" + y + "}"; }
}
