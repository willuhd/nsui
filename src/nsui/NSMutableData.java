package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMutableData — mutable data wrapper.
///
/// Header-completeness (`NSData.h`, mutable): every safe method whose shape is in the Sig
/// vocabulary is wrapped below. OMITTED — replaceBytesInRange:withBytes:length: (needs
/// of(VOID,RANGE,ID,INT), not in Sig; use the length-implied replaceBytesInRange:withBytes:);
/// initWithCapacity:/initWithLength: (covered by dataWithCapacity:/dataWithLength:);
/// decompressUsingAlgorithm:.../compressUsingAlgorithm:... (NSError** out-params).
public final class NSMutableData extends NSData {

    private static volatile boolean initMut;
    private static MethodHandle hAppendBytes; // (id, SEL, id, long) -> void (bytes ptr, length)
    private static MethodHandle hSetLength;   // (id, SEL, long) -> void

    private NSMutableData(MemorySegment peer) { super(peer); }

    public static NSMutableData wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMutableData(peer);
    }

    /// [NSMutableData data]
    public static NSMutableData data() {
        ensureMutInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSMutableData"), ObjC.sel("data"));
        return wrap(s);
    }

    /// [NSMutableData dataWithCapacity:]
    public static NSMutableData dataWithCapacity(long capacity) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSMutableData"), ObjC.sel("dataWithCapacity:"), capacity);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("dataWithCapacity: failed", t); }
    }

    /// [NSMutableData dataWithLength:]
    public static NSMutableData dataWithLength(long length) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSMutableData"), ObjC.sel("dataWithLength:"), length);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("dataWithLength: failed", t); }
    }

    private static synchronized void ensureMutInit() {
        if (initMut) return;
        try { NSData.data(); } catch (Exception ignored) {}
        // appendBytes:length: signature is (id,SEL,const void*,long) -> void. Use VOID,ID,INT via pointer as ID
        hAppendBytes = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hSetLength = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        initMut = true;
    }

    /// appendBytes:length: — appends Java bytes via native call if possible, else no-op.
    public void appendBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;
        ensureMutInit();
        try {
            java.lang.foreign.MemorySegment cBytes = nsui.objc.Scratch.allocInput(bytes.length);
            java.lang.foreign.MemorySegment.copy(bytes, 0, cBytes, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, bytes.length);
            hAppendBytes.invokeExact(peer, ObjC.sel("appendBytes:length:"), cBytes, (long) bytes.length);
        } catch (Throwable t) { throw new RuntimeException("appendBytes:length: failed", t); }
    }

    /// increaseLengthBy:
    public void increaseLengthBy(long extra) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
            h.invokeExact(peer, ObjC.sel("increaseLengthBy:"), extra);
        } catch (Throwable t) { throw new RuntimeException("increaseLengthBy: failed", t); }
    }

    /// setLength:
    public void setLength(long length) {
        ensureMutInit();
        try { hSetLength.invokeExact(peer, ObjC.sel("setLength:"), length); }
        catch (Throwable t) { throw new RuntimeException("setLength: failed", t); }
    }

    /// appendData:
    public void appendData(NSData other) {
        ensureMutInit();
        if (other == null) return;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("appendData:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("appendData: failed", t); }
    }

    /// resetBytesInRange:
    public void resetBytesInRange(NSRange range) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE));
            h.invokeExact(peer, ObjC.sel("resetBytesInRange:"), range.toSegment());
        } catch (Throwable t) { throw new RuntimeException("resetBytesInRange: failed", t); }
    }

    /// mutableBytes — writable inner pointer (valid while the data lives; length
    /// management still goes through setLength:/increaseLengthBy:). Mirrors bytes().
    public MemorySegment mutableBytes() {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("mutableBytes"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("mutableBytes failed", t); }
    }

    /// setData: — replace contents with another data object.
    public void setData(NSData data) {
        ensureMutInit();
        if (data == null) { setLength(0); return; }
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setData:"), data.peer());
        } catch (Throwable t) { throw new RuntimeException("setData: failed", t); }
    }

    /// replaceBytesInRange:withBytes: — replace the range with Java bytes (the range
    /// implies the length; the withBytes:length: variant needs of(VOID,RANGE,ID,INT),
    /// not in Sig — resize first with setLength: when lengths differ).
    public void replaceBytesInRange(NSRange range, byte[] bytes) {
        ensureMutInit();
        if (bytes == null) bytes = new byte[0];
        try {
            MemorySegment buf = nsui.objc.Scratch.allocInput(Math.max(1, bytes.length));
            if (bytes.length > 0) {
                MemorySegment.copy(bytes, 0, buf, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, bytes.length);
            }
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RANGE, Arg.ID));
            h.invokeExact(peer, ObjC.sel("replaceBytesInRange:withBytes:"), range.toSegment(), buf);
        } catch (Throwable t) { throw new RuntimeException("replaceBytesInRange:withBytes: failed", t); }
    }
}
