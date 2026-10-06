package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Ret;

/// NSData — minimal wrapper over native `NSData` / `NSMutableData`.
/// Factories build real native data (retained: immortal by design), so AppKit
/// APIs reading the peer see the bytes. No side map, no address keys.
///
/// Header-completeness (`NSData.h`, immutable): every safe method whose shape is in the
/// Sig vocabulary is wrapped below. OMITTED — rangeOfData:options:range: (needs
/// of(RANGE,ID,INT,RANGE), not in Sig); dataWithBytesNoCopy:.../initWithBytesNoCopy:.../
/// initWithBytesNoCopy:...deallocator: (no-copy ownership; use the copying dataWithBytes:);
/// dataWithBytes:length: NATIVE form needs of(ID,ID,INT), not in Sig — dataWithBytes(byte[])
/// below composes NSMutableData.data + appendBytes:length: instead (same copy semantics);
/// dataWithContentsOfFile:options:error:/dataWithContentsOfURL:options:error:/
/// initWithContentsOf.../writeToFile:options:error:/writeToURL:options:error: and
/// decompressedDataUsingAlgorithm:.../compressedDataUsingAlgorithm:... (NSError** out-params);
/// getBytes:/getBytes:length:/getBytes:range:/getBytes: (out-buffer plumbing; use toByteArray());
/// enumerateByteRangesUsingBlock: (block); initWithBase64EncodedString:options:/
/// initWithBase64EncodedData:options: (need of(ID,ID,INT), not in Sig — encode side is kept);
/// initWithData: (covered by dataWithData:); dataWithContentsOfMappedFile:/initWithContentsOfMappedFile:/
/// base64Encoding/initWithBase64Encoding: (deprecated); NSPurgeableData (no wrapper in this batch).
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
    /// Composed from NSMutableData.data + appendBytes:length: (same copying semantics):
    /// the native dataWithBytes:length: needs of(ID,ID,INT), not in Sig (see class docs).
    public static NSData dataWithBytes(byte[] bytes) {
        if (bytes == null) bytes = new byte[0];
        ensureInit();
        NSMutableData md = NSMutableData.data();
        md.appendBytes(bytes);
        return wrap(retain(md.peer()));
    }

    /// dataWithBytesNoCopy variant — same as dataWithBytes for minimal.
    public static NSData dataWithBytes(byte[] bytes, long length) {
        if (bytes == null) bytes = new byte[0];
        long len = Math.min(length, bytes.length);
        byte[] slice = new byte[(int) len];
        System.arraycopy(bytes, 0, slice, 0, (int) len);
        return dataWithBytes(slice);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
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

    /// [NSData dataWithData:] — copy from another data object.
    public static NSData dataWithData(NSData other) {
        ensureInit();
        if (other == null) return data();
        return wrap(retain(ObjC.msgSendIdId(ObjC.cls("NSData"), ObjC.sel("dataWithData:"), other.peer())));
    }

    /// [NSData dataWithContentsOfFile:] — read a file (nil when missing/unreadable).
    public static NSData dataWithContentsOfFile(String path) {
        ensureInit();
        if (path == null || path.isEmpty()) return null;
        return wrap(retain(ObjC.msgSendIdId(ObjC.cls("NSData"), ObjC.sel("dataWithContentsOfFile:"), ObjC.nsstring(path))));
    }

    /// [NSData dataWithContentsOfURL:] — NSURL peer (nil-safe: null for nil).
    /// Build file URLs via `ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("fileURLWithPath:"),
    /// ObjC.nsstring(path))` (house NSURL idiom, e.g. NSPathControl).
    public static NSData dataWithContentsOfURL(MemorySegment url) {
        ensureInit();
        if (url == null || url.address() == 0) return null;
        return wrap(retain(ObjC.msgSendIdId(ObjC.cls("NSData"), ObjC.sel("dataWithContentsOfURL:"), url)));
    }

    /// [data writeToURL:atomically:] — NSURL peer (nil-safe: false for nil).
    public boolean writeToURL(MemorySegment url, boolean atomically) {
        ensureInit();
        if (url == null || url.address() == 0) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Sig.Arg.ID, Sig.Arg.BOOL));
            return (boolean) h.invokeExact(peer, ObjC.sel("writeToURL:atomically:"), url, atomically);
        } catch (Throwable t) { throw new RuntimeException("writeToURL:atomically: failed", t); }
    }

    /// base64EncodedStringWithOptions: (0 for the default line-break-free form).
    public NSString base64EncodedStringWithOptions(long options) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Sig.Arg.INT));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("base64EncodedStringWithOptions:"), options);
            return NSString.wrap(r);
        } catch (Throwable t) { throw new RuntimeException("base64EncodedStringWithOptions: failed", t); }
    }

    /// base64EncodedDataWithOptions:.
    public NSData base64EncodedDataWithOptions(long options) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Sig.Arg.INT));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("base64EncodedDataWithOptions:"), options);
            return wrap(retain(r));
        } catch (Throwable t) { throw new RuntimeException("base64EncodedDataWithOptions: failed", t); }
    }

    // base64 decode (initWithBase64EncodedString:options:/initWithBase64EncodedData:options:)
    // omitted: both need of(ID,ID,INT), not in Sig (see class docs). No decode path exists
    // in the current vocabulary; the encode side above is fully testable.
}
