package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import nsui.objc.Scratch;

/// MTLScissorRect — the pixel-space clip rectangle of a render encoder:
/// { NSUInteger x, y, width, height }, 32 bytes BY VALUE. Value type.
public record MTLScissorRect(long x, long y, long width, long height) {

    /// 32-byte segment (call-scoped bump, like ObjC.rect).
    public MemorySegment toSegment() {
        MemorySegment s = Scratch.allocInput(32);
        s.set(ValueLayout.JAVA_LONG, 0, x);
        s.set(ValueLayout.JAVA_LONG, 8, y);
        s.set(ValueLayout.JAVA_LONG, 16, width);
        s.set(ValueLayout.JAVA_LONG, 24, height);
        return s;
    }
}
