package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Scratch;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// NSData — minimal wrapper over native `NSData` / `NSMutableData`.
/// Factories build real native data (retained: immortal by design), so AppKit
/// APIs reading the peer see the bytes. No side map, no address keys.
public class NSData extends NSObject {

    private static MemorySegment retain(MemorySegment v) { return ObjC.msgSendId(v, ObjC.sel("retain")); }
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

    /// Create NSData from Java bytes — real native data, retained.
    public static NSData dataWithBytes(byte[] bytes) {
        if (bytes == null) bytes = new byte[0];
        ensureInit();
        try {
            // Call-scoped bump: dataWithBytes:length: copies synchronously.
            MemorySegment buf = Scratch.allocInput(Math.max(1, bytes.length));
            if (bytes.length > 0) {
                MemorySegment.copy(bytes, 0, buf, ValueLayout.JAVA_BYTE, 0, bytes.length);
            }
            MethodHandle hBytes = ObjC.handle(Sig.of(Ret.ID, Sig.Arg.ID, Sig.Arg.INT));
            MemorySegment peer = (MemorySegment) hBytes.invokeExact(
                    ObjC.cls("NSData"), ObjC.sel("dataWithBytes:length:"), buf, (long) bytes.length);
            return wrap(retain(peer));
        } catch (Throwable t) { throw new RuntimeException("dataWithBytes:length: failed", t); }
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
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Sig.Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToData:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isEqualToData: failed", t); }
    }

    /// subdataWithRange:
    public NSData subdataWithRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Sig.Arg.RANGE));
            MemorySegment s = (MemorySegment) h.invokeExact(peer, ObjC.sel("subdataWithRange:"), range.toSegment());
            return wrap(retain(s));
        } catch (Throwable t) { throw new RuntimeException("subdataWithRange: failed", t); }
    }

    /// `[data writeToFile:path atomically:flag]` — write the receiver's bytes to
    /// `path` (optionally via an auxiliary file so an interrupted write cannot
    /// corrupt the destination). Returns whether the write succeeded.
    ///
    /// Writes the receiver bytes to `path`. Returns false for a null/empty path.
    public boolean writeToFile(String path, boolean atomically) {
        ensureInit();
        if (path == null || path.isEmpty()) return false;
        MemorySegment target = peer;
        if (target == null || target.address() == 0) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Sig.Arg.ID, Sig.Arg.BOOL));
            return (boolean) h.invokeExact(target, ObjC.sel("writeToFile:atomically:"), ObjC.nsstring(path), atomically);
        } catch (Throwable t) { throw new RuntimeException("writeToFile:atomically: failed", t); }
    }
}
