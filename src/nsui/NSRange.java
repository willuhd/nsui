package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import nsui.objc.Scratch;

/**
 * NSRange as a Java value type: {@code {location, length}} (NSUInteger pair).
 * Marshals to/from the FFM struct segment only at the call boundary.
 * Layout: two longs (8 bytes each) — 16 bytes total.
 */
public record NSRange(long location, long length) {

    public static final NSRange ZERO = new NSRange(0, 0);
    /** NSNotFound = NSIntegerMax. */
    public static final long NOT_FOUND = Long.MAX_VALUE;

    /** 16-byte segment: location at offset 0, length at offset 8. */
    public MemorySegment toSegment() {
        MemorySegment s = Scratch.alloc(16);
        s.set(ValueLayout.JAVA_LONG, 0, location);
        s.set(ValueLayout.JAVA_LONG, 8, length);
        return s;
    }

    /** Read an NSRange struct segment: long location at 0, long length at 8. */
    public static NSRange fromSegment(MemorySegment s) {
        return new NSRange(s.get(ValueLayout.JAVA_LONG, 0), s.get(ValueLayout.JAVA_LONG, 8));
    }

    /** NSMakeRange equivalent. */
    public static NSRange make(long loc, long len) { return new NSRange(loc, len); }

    /** Max range = location + length. */
    public long max() { return location + length; }

    /** End location (exclusive). */
    public long end() { return max(); }

    /** Is empty (zero or negative length). */
    public boolean isEmpty() { return length <= 0; }

    /** Length == 0. */
    public boolean isNotFound() { return location == NOT_FOUND; }

    /** Contains index (inclusive location, exclusive max). */
    public boolean contains(long index) {
        return index >= location && index < location + length;
    }

    /** Contains range fully. */
    public boolean contains(NSRange other) {
        if (other.isEmpty()) return contains(other.location) || other.location == location + length;
        return other.location >= location && other.max() <= max();
    }

    /** Intersects. */
    public boolean intersects(NSRange other) {
        if (isEmpty() || other.isEmpty()) return false;
        return !(other.location + other.length <= location || other.location >= location + length);
    }

    /** Intersection range or ZERO if no overlap. */
    public NSRange intersection(NSRange other) {
        long nLoc = Math.max(location, other.location);
        long nEnd = Math.min(max(), other.max());
        long nLen = nEnd - nLoc;
        if (nLen <= 0) return ZERO;
        return new NSRange(nLoc, nLen);
    }

    /** Union range (smallest range covering both). */
    public NSRange unionRange(NSRange other) {
        if (isEmpty()) return other;
        if (other.isEmpty()) return this;
        long nLoc = Math.min(location, other.location);
        long nEnd = Math.max(max(), other.max());
        return new NSRange(nLoc, nEnd - nLoc);
    }

    /** Offset by delta. */
    public NSRange offset(long delta) {
        return new NSRange(location + delta, length);
    }

    /** Inset by delta (negative expands). */
    public NSRange inset(long delta) {
        return new NSRange(location + delta, length - 2 * delta);
    }

    /** Equality with exact match (record already provides equals). */

    @Override public String toString() { return "NSRange{loc=" + location + ", len=" + length + "}"; }
}
