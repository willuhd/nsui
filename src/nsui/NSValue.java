package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// NSValue — minimal wrapper over native `NSValue`.
/// Provides wrap/create and typed accessors for common struct types.
/// Struct creation uses a Java-side cache so no new Sig vocabulary is required:
/// a real native NSValue peer is allocated via alloc/init and the Java value is
/// stored in a side map. Native peers created elsewhere return their live value
/// via struct-return msgSend where possible.
public class NSValue extends NSObject {

    private static final ConcurrentHashMap<Long, Object> STORE = new ConcurrentHashMap<>();
            private record Handles(MethodHandle hPointValue, MethodHandle hSizeValue, MethodHandle hRectValue, MethodHandle hRangeValue, MethodHandle hObjCType) {}
    private static volatile Handles handles;

    protected NSValue(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSValue wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSValue(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        // objCType returns const char* (PTR) treated as ID handle
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.RECT)),
                ObjC.handle(Sig.of(Ret.RANGE)),
                ObjC.handle(Sig.of(Ret.ID))
        );
    }

    private static MemorySegment allocInit(String clsName) {
        ensureInit();
        MemorySegment a = ObjC.msgSendId(ObjC.cls(clsName), ObjC.sel("alloc"));
        // init — (id,SEL)->id
        a = ObjC.msgSendId(a, ObjC.sel("init"));
        return a;
    }

    /// valueWithPoint: — Java-cached.
    public static NSValue valueWithPoint(NSPoint point) {
        if (point == null) throw new IllegalArgumentException("point null");
        MemorySegment peer = allocInit("NSValue");
        NSValue v = new NSValue(peer);
        STORE.put(peer.address(), point);
        return v;
    }

    /// valueWithSize:
    public static NSValue valueWithSize(NSSize size) {
        if (size == null) throw new IllegalArgumentException("size null");
        MemorySegment peer = allocInit("NSValue");
        NSValue v = new NSValue(peer);
        STORE.put(peer.address(), size);
        return v;
    }

    /// valueWithRect:
    public static NSValue valueWithRect(NSRect rect) {
        if (rect == null) throw new IllegalArgumentException("rect null");
        MemorySegment peer = allocInit("NSValue");
        NSValue v = new NSValue(peer);
        STORE.put(peer.address(), rect);
        return v;
    }

    /// valueWithRange:
    public static NSValue valueWithRange(NSRange range) {
        if (range == null) throw new IllegalArgumentException("range null");
        MemorySegment peer = allocInit("NSValue");
        NSValue v = new NSValue(peer);
        STORE.put(peer.address(), range);
        return v;
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
        Object cached = STORE.get(peer.address());
        if (cached instanceof NSPoint p) return p;
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hPointValue().invokeExact((java.lang.foreign.SegmentAllocator) Arena.global(), peer, ObjC.sel("pointValue"));
            return NSPoint.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("pointValue failed", t); }
    }

    /// sizeValue
    public NSSize sizeValue() {
        Object cached = STORE.get(peer.address());
        if (cached instanceof NSSize s) return s;
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hSizeValue().invokeExact((java.lang.foreign.SegmentAllocator) Arena.global(), peer, ObjC.sel("sizeValue"));
            return NSSize.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("sizeValue failed", t); }
    }

    /// rectValue
    public NSRect rectValue() {
        Object cached = STORE.get(peer.address());
        if (cached instanceof NSRect r) return r;
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hRectValue().invokeExact((java.lang.foreign.SegmentAllocator) Arena.global(), peer, ObjC.sel("rectValue"));
            return NSRect.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("rectValue failed", t); }
    }

    /// rangeValue
    public NSRange rangeValue() {
        Object cached = STORE.get(peer.address());
        if (cached instanceof NSRange r) return r;
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hRangeValue().invokeExact((java.lang.foreign.SegmentAllocator) Arena.global(), peer, ObjC.sel("rangeValue"));
            return NSRange.fromSegment(seg);
        } catch (Throwable t) { throw new RuntimeException("rangeValue failed", t); }
    }

    /// objCType — C string
    public String objCType() {
        ensureInit();
        try {
            MemorySegment c = (MemorySegment) handles.hObjCType().invokeExact(peer, ObjC.sel("objCType"));
            if (c == null || c.address() == 0) return null;
            long len = 0;
            while (c.reinterpret(len + 1).get(java.lang.foreign.ValueLayout.JAVA_BYTE, len) != 0) len++;
            if (len == 0) return "";
            return c.reinterpret(len + 1).getString(0);
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
}
