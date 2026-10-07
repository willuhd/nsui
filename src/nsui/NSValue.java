package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSValue — minimal wrapper over native `NSValue`.
/// Provides wrap/create and typed accessors for common struct types.
/// Factories return autoreleased values (pool discipline: the caller drains
/// via Autorelease per frame):
/// AppKit APIs reading the peer see the value (dummy peers never could).
/// no side map, no address keys, no ABA hazard.
/// Getters read live values via struct-return msgSend.
///
/// Header-completeness (`NSValue.h` + `NSGeometry.h`/`NSRange.h` value categories): every
/// safe method whose shape is in the Sig vocabulary is wrapped below. OMITTED —
/// valueWithEdgeInsets:/edgeInsetsValue (need an NSEdgeInsets arg/return class in Sig;
/// reusing of(ID,RECT)/of(RECT) would lie in the vocabulary accounting — flagged for the
/// Sig owner); valueWithBytes:objCType:/value:withObjCType:/initWithBytes:objCType: and
/// getValue:/getValue:size: (raw buffers + type strings; unsafe); initWithCoder: (needs NSCoder).
public class NSValue extends NSObject {

            private record Handles(MethodHandle hPointValue, MethodHandle hSizeValue, MethodHandle hRectValue, MethodHandle hRangeValue, MethodHandle hObjCType, MethodHandle hWithPoint, MethodHandle hWithSize, MethodHandle hWithRect, MethodHandle hWithRange) {}
    private static volatile Handles handles;

    protected NSValue(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSValue wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSValue(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        // objCType returns const char* (PTR) treated as ID handle
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.RECT)),
                ObjC.handle(Sig.of(Ret.RANGE)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.RANGE))
        );
    }

    /// valueWithPoint: — real native value, autoreleased.
    public static NSValue valueWithPoint(NSPoint point) {
        if (point == null) throw new IllegalArgumentException("point null");
        ensureInit();
        try {
            MemorySegment peer = (MemorySegment) handles.hWithPoint().invokeExact(
                    ObjC.cls("NSValue"), ObjC.sel("valueWithPoint:"), point.toSegment());
            return wrap(peer);
        } catch (Throwable t) { throw new RuntimeException("valueWithPoint: failed", t); }
    }

    /// valueWithSize: — real native value, autoreleased.
    public static NSValue valueWithSize(NSSize size) {
        if (size == null) throw new IllegalArgumentException("size null");
        ensureInit();
        try {
            MemorySegment peer = (MemorySegment) handles.hWithSize().invokeExact(
                    ObjC.cls("NSValue"), ObjC.sel("valueWithSize:"), size.toSegment());
            return wrap(peer);
        } catch (Throwable t) { throw new RuntimeException("valueWithSize: failed", t); }
    }

    /// valueWithRect: — real native value, autoreleased.
    public static NSValue valueWithRect(NSRect rect) {
        if (rect == null) throw new IllegalArgumentException("rect null");
        ensureInit();
        try {
            MemorySegment peer = (MemorySegment) handles.hWithRect().invokeExact(
                    ObjC.cls("NSValue"), ObjC.sel("valueWithRect:"), rect.toSegment());
            return wrap(peer);
        } catch (Throwable t) { throw new RuntimeException("valueWithRect: failed", t); }
    }

    /// valueWithRange: — real native value, autoreleased.
    public static NSValue valueWithRange(NSRange range) {
        if (range == null) throw new IllegalArgumentException("range null");
        ensureInit();
        try {
            MemorySegment peer = (MemorySegment) handles.hWithRange().invokeExact(
                    ObjC.cls("NSValue"), ObjC.sel("valueWithRange:"), range.toSegment());
            return wrap(peer);
        } catch (Throwable t) { throw new RuntimeException("valueWithRange: failed", t); }
    }

    /// valueWithNonretainedObject: — native.
    public static NSValue valueWithNonretainedObject(NSObject object) {
        ensureInit();
        if (object == null) throw new IllegalArgumentException("object null");
        MemorySegment v = ObjC.msgSendIdId(ObjC.cls("NSValue"), ObjC.sel("valueWithNonretainedObject:"), object.peer());
        return wrap(v);
    }

    /// valueWithPointer:
    public static NSValue valueWithPointer(MemorySegment pointer) {
        ensureInit();
        if (pointer == null) pointer = MemorySegment.NULL;
        MemorySegment v = ObjC.msgSendIdId(ObjC.cls("NSValue"), ObjC.sel("valueWithPointer:"), pointer);
        return wrap(v);
    }

    /// pointValue
    public NSPoint pointValue() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hPointValue().invokeExact(ObjC.structSlot(), peer, ObjC.sel("pointValue"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("pointValue failed", t); }
    }

    /// sizeValue
    public NSSize sizeValue() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hSizeValue().invokeExact(ObjC.structSlot(), peer, ObjC.sel("sizeValue"));
            return NSSize.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("sizeValue failed", t); }
    }

    /// rectValue
    public NSRect rectValue() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hRectValue().invokeExact(ObjC.structSlot(), peer, ObjC.sel("rectValue"));
            return NSRect.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("rectValue failed", t); }
    }

    /// rangeValue
    public NSRange rangeValue() {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hRangeValue().invokeExact(ObjC.structSlot(), peer, ObjC.sel("rangeValue"));
            return NSRange.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("rangeValue failed", t); }
    }

    /// objCType — C string
    public String objCType() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) handles.hObjCType().invokeExact(peer, ObjC.sel("objCType"));
            if (c == null || c.address() == 0) return null;
            long len = -1;
            long base = 0;
            while (true) {
                MemorySegment w = c.reinterpret(base + 4096);
                for (long i = 0; i < 4096; i++) {
                    if (w.get(java.lang.foreign.ValueLayout.JAVA_BYTE, base + i) == 0) { len = base + i; break; }
                }
                if (len >= 0) break;
                base += 4096;
                if (base >= 16_000_000) { len = 16_000_000; break; }
            }
            if (len == 0) return "";
            byte[] bytes = c.reinterpret(len).toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
            return new String(bytes, 0, (int) len, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Throwable t) { throw new RuntimeException("objCType failed", t); }
    }

    /// nonretainedObjectValue
    public MemorySegment nonretainedObjectValue() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) handles.hObjCType().invokeExact(peer, ObjC.sel("nonretainedObjectValue"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("nonretainedObjectValue failed", t); }
    }

    /// pointerValue
    public MemorySegment pointerValue() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("pointerValue"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("pointerValue failed", t); }
    }

    /// isEqualToValue:.
    public boolean isEqualToValue(NSValue other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToValue:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isEqualToValue: failed", t); }
    }
}
