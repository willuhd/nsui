package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSRect as a Java value type: {x, y, width, height} in points.
/// Marshals to/from the FFM struct segment only at the call boundary.
public record NSRect(double x, double y, double width, double height) {

    public static final NSRect ZERO = new NSRect(0, 0, 0, 0);

    public MemorySegment toSegment() {
        return ObjC.rect(x, y, width, height);
    }

    public static NSRect fromSegment(MemorySegment r) {
        return new NSRect(ObjC.rectX(r), ObjC.rectY(r), ObjC.rectW(r), ObjC.rectH(r));
    }

    /// NSMakeRect.
    public static NSRect make(double x, double y, double w, double h) { return new NSRect(x, y, w, h); }

    /// Origin as NSPoint.
    public NSPoint origin() { return new NSPoint(x, y); }

    /// Size as NSSize.
    public NSSize size() { return new NSSize(width, height); }

    /// Mid X.
    public double midX() { return x + width / 2; }
    /// Mid Y.
    public double midY() { return y + height / 2; }
    /// Max X.
    public double maxX() { return x + width; }
    /// Max Y.
    public double maxY() { return y + height; }
    /// Min X.
    public double minX() { return x; }
    /// Min Y.
    public double minY() { return y; }

    /// Is empty (zero or negative area).
    public boolean isEmpty() { return width <= 0 || height <= 0; }

    /// Area.
    public double area() { return width * height; }

    /// Contains point (inclusive origin, exclusive max).
    public boolean contains(NSPoint p) { return p.x() >= x && p.x() < x + width && p.y() >= y && p.y() < y + height; }

    /// Contains rect fully.
    public boolean contains(NSRect other) {
        return other.x >= x && other.y >= y && other.maxX() <= maxX() && other.maxY() <= maxY();
    }

    /// Intersects.
    public boolean intersects(NSRect other) {
        return !(other.x + other.width <= x || other.x >= x + width || other.y + other.height <= y || other.y >= y + height);
    }

    /// Intersection rect or ZERO if no overlap.
    public NSRect intersection(NSRect other) {
        double nx = Math.max(x, other.x);
        double ny = Math.max(y, other.y);
        double nx2 = Math.min(maxX(), other.maxX());
        double ny2 = Math.min(maxY(), other.maxY());
        double nw = nx2 - nx;
        double nh = ny2 - ny;
        if (nw <= 0 || nh <= 0) return ZERO;
        return new NSRect(nx, ny, nw, nh);
    }

    /// Union rect.
    public NSRect unionRect(NSRect other) {
        if (isEmpty()) return other;
        if (other.isEmpty()) return this;
        double nx = Math.min(x, other.x);
        double ny = Math.min(y, other.y);
        double nx2 = Math.max(maxX(), other.maxX());
        double ny2 = Math.max(maxY(), other.maxY());
        return new NSRect(nx, ny, nx2 - nx, ny2 - ny);
    }

    /// Inset by dx,dy (negative expands).
    public NSRect inset(double dx, double dy) {
        return new NSRect(x + dx, y + dy, width - 2 * dx, height - 2 * dy);
    }

    /// Offset by dx,dy.
    public NSRect offset(double dx, double dy) {
        return new NSRect(x + dx, y + dy, width, height);
    }

    /// Inset with edge insets NSEdgeInsets style {top,left,bottom,right} as NSRect {x=top,y=left,w=bottom,h=right}.
    public NSRect insetEdge(double top, double left, double bottom, double right) {
        return new NSRect(x + left, y + bottom, width - left - right, height - top - bottom);
    }

    /// Standardized (positive width/height).
    public NSRect standardized() {
        double nx = width < 0 ? x + width : x;
        double ny = height < 0 ? y + height : y;
        return new NSRect(nx, ny, Math.abs(width), Math.abs(height));
    }

    /// Integral (ceil).
    public NSRect integral() {
        double nx = Math.floor(x);
        double ny = Math.floor(y);
        double nx2 = Math.ceil(maxX());
        double ny2 = Math.ceil(maxY());
        return new NSRect(nx, ny, nx2 - nx, ny2 - ny);
    }

    /// Center in container rect.
    public NSRect centeredIn(NSRect container) {
        double nx = container.x + (container.width - width) / 2;
        double ny = container.y + (container.height - height) / 2;
        return new NSRect(nx, ny, width, height);
    }

    /// Aspect fit inside container.
    public NSRect aspectFit(NSRect container) {
        if (width == 0 || height == 0) return this;
        double scale = Math.min(container.width / width, container.height / height);
        double nw = width * scale;
        double nh = height * scale;
        return new NSRect(container.x + (container.width - nw) / 2, container.y + (container.height - nh) / 2, nw, nh);
    }

    @Override public String toString() { return "NSRect{x=" + x + ", y=" + y + ", w=" + width + ", h=" + height + "}"; }
}
