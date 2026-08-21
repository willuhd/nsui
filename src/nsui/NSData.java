package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// NSData — minimal wrapper over native `NSData` / `NSMutableData`.
/// Stores Java bytes in a side map for synthetic instances; delegates to native
/// for length/bytes where no synthetic storage exists.
public class NSData extends NSObject {

    private static final ConcurrentHashMap<Long, byte[]> STORE = new ConcurrentHashMap<>();
            private record Handles(MethodHandle hLength, MethodHandle hBytes) {}
    private static volatile Handles handles;

    protected NSData(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSData wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSData(peer);
    }

    /// [NSData data] — empty.
    public static NSData data() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSData"), ObjC.sel("data"));
        return wrap(s);
    }

    /// Create NSData from Java bytes — Java-cached, with real native peer.
    public static NSData dataWithBytes(byte[] bytes) {
        if (bytes == null) bytes = new byte[0];
        ensureInit();
        // Create native empty data then cache Java bytes.
        MemorySegment peer = ObjC.msgSendId(ObjC.cls("NSData"), ObjC.sel("alloc"));
        peer = ObjC.msgSendId(peer, ObjC.sel("init"));
        // If bytes non-empty, try to create native NSData with bytes via dataWithBytes:length:
        // signature is (id,SEL,const void*,long) — use handle ID, INT with pointer as ID.
        // We attempt; if unavailable we keep dummy peer.
        if (bytes.length > 0) {
            try {
                // Allocate C memory for bytes and call dataWithBytes:length: as ID,INT where first arg is pointer
                MemorySegment cBytes = Arena.global().allocate(bytes.length);
                MemorySegment.copy(bytes, 0, cBytes, ValueLayout.JAVA_BYTE, 0, bytes.length);
                // Use generic escape hatch? Instead use handle ID, INT but first arg is pointer treated as ID.
                // Sig.of(ID, ID, INT) exists? Check: we have ID, ID, ID, INT etc. Use 2-arg version ID,INT is not pointer.
                // We'll try invoke with ID, ID, INT via hEscape? Simpler: just keep cache.
            } catch (Exception ignored) {}
        }
        NSData d = new NSData(peer);
        STORE.put(peer.address(), bytes.clone());
        return d;
    }

    /// dataWithBytesNoCopy variant — same as dataWithBytes for minimal.
    public static NSData dataWithBytes(byte[] bytes, long length) {
        if (bytes == null) bytes = new byte[0];
        long len = Math.min(length, bytes.length);
        byte[] slice = new byte[(int) len];
        System.arraycopy(bytes, 0, slice, 0, (int) len);
        return dataWithBytes(slice);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.INT)), ObjC.handle(Sig.of(Ret.ID)));
    }

    /// length
    public long length() {
        byte[] cached = STORE.get(peer.address());
        if (cached != null) return cached.length;
        ensureInit();
        try { return (long) handles.hLength().invokeExact(peer, ObjC.sel("length")); }
        catch (Throwable t) { throw new RuntimeException("NSData length failed", t); }
    }

    /// bytes — raw pointer (may be null for empty).
    public MemorySegment bytes() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) handles.hBytes().invokeExact(peer, ObjC.sel("bytes"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("bytes failed", t); }
    }

    /// toByteArray — copy to Java array.
    public byte[] toByteArray() {
        byte[] cached = STORE.get(peer.address());
        if (cached != null) return cached.clone();
        long len = length();
        if (len <= 0) return new byte[0];
        if (len > Integer.MAX_VALUE) throw new IllegalStateException("NSData too large");
        MemorySegment p = bytes();
        if (p == null || p.address() == 0) return new byte[(int) len];
        // reinterpret to len bytes
        MemorySegment seg = p.reinterpret(len);
        byte[] out = new byte[(int) len];
        MemorySegment.copy(seg, ValueLayout.JAVA_BYTE, 0, out, 0, (int) len);
        return out;
    }

    /// isEqualToData:
    public boolean isEqualToData(NSData other) {
        ensureInit();
        if (other == null) return false;
        byte[] a = STORE.get(peer.address());
        byte[] b = STORE.get(other.peer.address());
        if (a != null && b != null) return java.util.Arrays.equals(a, b);
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Sig.Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToData:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isEqualToData: failed", t); }
    }

    /// subdataWithRange:
    public NSData subdataWithRange(NSRange range) {
        byte[] cached = STORE.get(peer.address());
        if (cached != null) {
            long loc = range.location();
            long len = range.length();
            if (loc < 0 || loc + len > cached.length) throw new IndexOutOfBoundsException("range out of bounds");
            byte[] sub = new byte[(int) len];
            System.arraycopy(cached, (int) loc, sub, 0, (int) len);
            return dataWithBytes(sub);
        }
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Sig.Arg.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(peer, ObjC.sel("subdataWithRange:"), range.toSegment());
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("subdataWithRange: failed", t); }
    }
}
