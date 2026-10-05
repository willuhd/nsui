package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import nsui.objc.Scratch;

/// MTLRegion — {origin xyz, size whd} in pixels, 6 longs = 48 bytes.
/// Value type for texture readback regions.
public record MTLRegion(long x, long y, long z, long width, long height, long depth) {

    /// 2D region at (x, y) of w*h (z=0, depth=1).
    public static MTLRegion of2D(long x, long y, long width, long height) {
        return new MTLRegion(x, y, 0, width, height, 1);
    }

    /// 48-byte segment (call-scoped bump).
    public MemorySegment toSegment() {
        MemorySegment s = Scratch.allocInput(48);
        s.set(ValueLayout.JAVA_LONG, 0, x);
        s.set(ValueLayout.JAVA_LONG, 8, y);
        s.set(ValueLayout.JAVA_LONG, 16, z);
        s.set(ValueLayout.JAVA_LONG, 24, width);
        s.set(ValueLayout.JAVA_LONG, 32, height);
        s.set(ValueLayout.JAVA_LONG, 40, depth);
        return s;
    }
}
