package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import nsui.objc.Scratch;

/// MTLViewport — the pixel-space viewport a render encoder maps NDC onto:
/// { double originX, originY, width, height, znear, zfar }, 48 bytes BY VALUE.
/// Value type; marshal to the FFM struct segment only at the call boundary.
public record MTLViewport(double originX, double originY, double width, double height,
        double znear, double zfar) {

    /// The full viewport of a w*h target (znear 0, zfar 1).
    public static MTLViewport of(double width, double height) {
        return new MTLViewport(0, 0, width, height, 0, 1);
    }

    /// 48-byte segment (call-scoped bump, like ObjC.rect).
    public MemorySegment toSegment() {
        MemorySegment s = Scratch.allocInput(48);
        s.set(ValueLayout.JAVA_DOUBLE, 0, originX);
        s.set(ValueLayout.JAVA_DOUBLE, 8, originY);
        s.set(ValueLayout.JAVA_DOUBLE, 16, width);
        s.set(ValueLayout.JAVA_DOUBLE, 24, height);
        s.set(ValueLayout.JAVA_DOUBLE, 32, znear);
        s.set(ValueLayout.JAVA_DOUBLE, 40, zfar);
        return s;
    }
}
