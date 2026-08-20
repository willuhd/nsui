package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSString — typed wrapper over a native {@code NSString} (id).
 * Thin, stateless: every method maps to one {@code objc_msgSend}.
 */
public final class NSString extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hLength;   // (id, SEL) -> long [length]
    private static MethodHandle hIsEqual;  // (id, SEL, id) -> bool [isEqualToString:]
    private static MethodHandle hHash;     // (id, SEL) -> long [hash] (unused but shows pattern)
    private static MethodHandle hUTF8String; // (id, SEL) -> id? no, UTF8String returns const char*

    private NSString(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /** Wrap a native NSString id (null for nil). */
    public static NSString wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSString(peer);
    }

    /** Create an NSString from a Java string via {@code +stringWithUTF8String:}. */
    public static NSString of(String javaString) {
        if (javaString == null) return null;
        return wrap(ObjC.nsstring(javaString));
    }

    /** Alias for {@link #of(String)} — explicit nsstring name. */
    public static NSString stringWithUTF8String(String s) {
        return of(s);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hLength = ObjC.handle(Sig.of(Ret.INT));
        hIsEqual = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        // hash is INT return, no args
        hHash = ObjC.handle(Sig.of(Ret.INT));
        // UTF8String is ID return? Actually returns const char* (PTR) but we don't use handle for it; ObjC.toString handles directly.
        initialized = true;
    }

    /** Java String contents via {@code UTF8String} (uses ObjC.toString). */
    public String string() {
        return ObjC.toString(peer);
    }

    /** length — number of UTF-16 code units (NSUInteger). */
    public long length() {
        ensureInit();
        try {
            return (long) hLength.invokeExact(peer, ObjC.sel("length"));
        } catch (Throwable t) {
            throw new RuntimeException("NSString length failed", t);
        }
    }

    /** isEqualToString: — equality with another NSString. */
    public boolean isEqual(NSString other) {
        ensureInit();
        if (other == null) return false;
        try {
            return (boolean) hIsEqual.invokeExact(peer, ObjC.sel("isEqualToString:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("isEqualToString: failed", t);
        }
    }

    /** isEqualToString: — convenience overload with Java String (creates temporary NSString). */
    public boolean isEqualToString(String javaString) {
        if (javaString == null) return false;
        NSString other = of(javaString);
        return isEqual(other);
    }

    /** isEqual: — generic ObjC equality (id). */
    public boolean isEqualTo(NSObject other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqual:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("isEqual: failed", t);
        }
    }

    /** substringWithRange: — returns a new NSString for the given range (requires RANGE vocab). */
    public NSString substringWithRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("substringWithRange:"), range.toSegment());
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("substringWithRange: failed", t);
        }
    }

    /** rangeOfString: — location of substring or NOT_FOUND. */
    public NSRange rangeOfString(String substring) {
        ensureInit();
        if (substring == null) return new NSRange(NSRange.NOT_FOUND, 0);
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID));
            MemorySegment seg = (MemorySegment) h.invokeExact(
                    (java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(),
                    peer, ObjC.sel("rangeOfString:"), ObjC.nsstring(substring));
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("rangeOfString: failed", t);
        }
    }

    @Override
    public String toString() {
        String s = string();
        return s != null ? s : super.toString();
    }
}
