package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import nsui.objc.Scratch;

/// NSSize as a Java value type: {width, height} in points.
public record NSSize(double width, double height) {

    public static final NSSize ZERO = new NSSize(0, 0);

    public MemorySegment toSegment() {
        MemorySegment s = Scratch.alloc(16);
        s.set(ValueLayout.JAVA_DOUBLE, 0, width);
        s.set(ValueLayout.JAVA_DOUBLE, 8, height);
        return s;
    }

    public static NSSize fromSegment(MemorySegment s) {
        return new NSSize(s.get(ValueLayout.JAVA_DOUBLE, 0), s.get(ValueLayout.JAVA_DOUBLE, 8));
    }

    /// NSMakeSize.
    public static NSSize make(double w, double h) { return new NSSize(w, h); }

    /// Area.
    public double area() { return width * height; }

    /// Aspect ratio width/height (INF if height 0).
    public double aspectRatio() { return height == 0 ? Double.POSITIVE_INFINITY : width / height; }

    /// Is empty.
    public boolean isEmpty() { return width <= 0 || height <= 0; }

    /// Scale uniformly.
    public NSSize scaled(double factor) { return new NSSize(width * factor, height * factor); }

    /// Scale by separate factors.
    public NSSize scaled(double sx, double sy) { return new NSSize(width * sx, height * sy); }

    /// Add.
    public NSSize add(NSSize other) { return new NSSize(width + other.width, height + other.height); }

    /// Subtract.
    public NSSize subtract(NSSize other) { return new NSSize(width - other.width, height - other.height); }

    /// Clamp to max size maintaining aspect if needed.
    public NSSize clamped(NSSize max) {
        return new NSSize(Math.min(width, max.width), Math.min(height, max.height));
    }

    /// Aspect fit inside bounds.
    public NSSize aspectFit(NSSize bounds) {
        if (width == 0 || height == 0) return this;
        double scale = Math.min(bounds.width / width, bounds.height / height);
        return new NSSize(width * scale, height * scale);
    }

    /// Aspect fill outside bounds (cover).
    public NSSize aspectFill(NSSize bounds) {
        if (width == 0 || height == 0) return this;
        double scale = Math.max(bounds.width / width, bounds.height / height);
        return new NSSize(width * scale, height * scale);
    }

    /// To NSPoint (width->x, height->y).
    public NSPoint toPoint() { return new NSPoint(width, height); }

    /// To NSRect with origin at zero.
    public NSRect toRect() { return new NSRect(0, 0, width, height); }

    /// To NSRect with given origin.
    public NSRect toRect(NSPoint origin) { return new NSRect(origin.x(), origin.y(), width, height); }

    /// Equality with epsilon.
    public boolean epsilonEquals(NSSize other, double eps) {
        return Math.abs(width - other.width) < eps && Math.abs(height - other.height) < eps;
    }

    @Override public String toString() { return "NSSize{w=" + width + ", h=" + height + "}"; }
}
