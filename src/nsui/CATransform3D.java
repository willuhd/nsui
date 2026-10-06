package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;

import nsui.objc.Scratch;

/// CATransform3D as a Java value type: the 4x4 row-major matrix QuartzCore
/// passes and returns BY VALUE (16 CGFloat doubles, 128 bytes). Mirrors
/// NSRect/NSSize — marshal to/from the FFM struct segment only at the call
/// boundary, never cache a segment.
///
/// A 128-byte struct return is memory-class on every ABI, so unlike the 16/32-byte
/// geometry structs it needs both `objc_msgSend_stret` on x86_64 (see
/// Sig.msgSendSymbol) and a 128-byte reusable return slot — hence `slot()` here
/// rather than ObjC.structSlot() (32 bytes).
public record CATransform3D(
        double m11, double m12, double m13, double m14,
        double m21, double m22, double m23, double m24,
        double m31, double m32, double m33, double m34,
        double m41, double m42, double m43, double m44) {

    /// The identity transform.
    public static final CATransform3D IDENTITY = new CATransform3D(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1);

    /// CATransform3D is 128 bytes (16 doubles); the layout in Sig uses the same size.
    public static final long BYTES = 128L;

    /// Per-thread destination for struct RETURNS (mirrors ObjC's RECT_SLOT).
    /// Never rewound, simply overwritten by the next transform return on the same
    /// thread — safe because every caller copies the 16 doubles out immediately.
    private static final ThreadLocal<MemorySegment> SLOT =
            ThreadLocal.withInitial(() -> Arena.global().allocate(BYTES, 16));

    /// The shared 128-byte struct-return slot, used as the implicit leading
    /// SegmentAllocator of a transform-returning downcall.
    static SegmentAllocator slot() {
        // The concrete arena segment implements SegmentAllocator; the static type
        // does not, so the cast is load-bearing (same as ObjC.structSlot()).
        return (SegmentAllocator) SLOT.get();
    }

    /// 128-byte by-value INPUT segment in the call-scoped bump buffer.
    public MemorySegment toSegment() {
        MemorySegment s = Scratch.allocInput(BYTES);
        double[] v = toArray();
        for (int i = 0; i < 16; i++) s.set(ValueLayout.JAVA_DOUBLE, i * 8L, v[i]);
        return s;
    }

    /// Read a CATransform3D struct segment (row-major, 16 doubles).
    public static CATransform3D fromSegment(MemorySegment s) {
        return new CATransform3D(
                s.get(ValueLayout.JAVA_DOUBLE, 0), s.get(ValueLayout.JAVA_DOUBLE, 8),
                s.get(ValueLayout.JAVA_DOUBLE, 16), s.get(ValueLayout.JAVA_DOUBLE, 24),
                s.get(ValueLayout.JAVA_DOUBLE, 32), s.get(ValueLayout.JAVA_DOUBLE, 40),
                s.get(ValueLayout.JAVA_DOUBLE, 48), s.get(ValueLayout.JAVA_DOUBLE, 56),
                s.get(ValueLayout.JAVA_DOUBLE, 64), s.get(ValueLayout.JAVA_DOUBLE, 72),
                s.get(ValueLayout.JAVA_DOUBLE, 80), s.get(ValueLayout.JAVA_DOUBLE, 88),
                s.get(ValueLayout.JAVA_DOUBLE, 96), s.get(ValueLayout.JAVA_DOUBLE, 104),
                s.get(ValueLayout.JAVA_DOUBLE, 112), s.get(ValueLayout.JAVA_DOUBLE, 120));
    }

    /// CATransform3DMake — 16 components in declaration (row-major) order.
    public static CATransform3D make(double... m) {
        if (m.length != 16) {
            throw new IllegalArgumentException("CATransform3D needs 16 components, got " + m.length);
        }
        return new CATransform3D(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7],
                m[8], m[9], m[10], m[11], m[12], m[13], m[14], m[15]);
    }

    /// CATransform3DMakeTranslation.
    public static CATransform3D translation(double tx, double ty, double tz) {
        return new CATransform3D(
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                tx, ty, tz, 1);
    }

    /// CATransform3DMakeScale.
    public static CATransform3D scale(double sx, double sy, double sz) {
        return new CATransform3D(
                sx, 0, 0, 0,
                0, sy, 0, 0,
                0, 0, sz, 0,
                0, 0, 0, 1);
    }

    /// Component array in declaration order (m11..m44) — the struct order.
    public double[] toArray() {
        return new double[]{m11, m12, m13, m14, m21, m22, m23, m24,
                m31, m32, m33, m34, m41, m42, m43, m44};
    }

    /// CATransform3DIsIdentity.
    public boolean isIdentity() {
        return equals(IDENTITY);
    }

    @Override
    public String toString() {
        return "CATransform3D{[" + m11 + ", " + m12 + ", " + m13 + ", " + m14
                + "], [" + m21 + ", " + m22 + ", " + m23 + ", " + m24
                + "], [" + m31 + ", " + m32 + ", " + m33 + ", " + m34
                + "], [" + m41 + ", " + m42 + ", " + m43 + ", " + m44 + "]}";
    }
}
