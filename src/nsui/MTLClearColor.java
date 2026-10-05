package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import nsui.objc.Scratch;

/// MTLClearColor — RGBA doubles for render-pass clear. Value type; marshals as
/// 32 bytes (same footprint as NSRect, reused layout shape, distinct meaning).
public record MTLClearColor(double red, double green, double blue, double alpha) {

    /// Opaque black.
    public static final MTLClearColor BLACK = new MTLClearColor(0, 0, 0, 1);

    /// 32-byte segment (call-scoped bump, like ObjC.rect).
    public MemorySegment toSegment() {
        MemorySegment s = Scratch.allocInput(32);
        s.set(ValueLayout.JAVA_DOUBLE, 0, red);
        s.set(ValueLayout.JAVA_DOUBLE, 8, green);
        s.set(ValueLayout.JAVA_DOUBLE, 16, blue);
        s.set(ValueLayout.JAVA_DOUBLE, 24, alpha);
        return s;
    }
}
