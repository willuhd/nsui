package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSNumber — minimal wrapper over native `NSNumber` (subclass of NSValue).
/// Provides wrap/create and numeric accessors.
public final class NSNumber extends NSValue {

            private record Handles(MethodHandle hIntValue, MethodHandle hDoubleValue, MethodHandle hBoolValue, MethodHandle hFloatValue) {}
    private static volatile Handles handles;

    private NSNumber(MemorySegment peer) { super(peer); }

    public static NSNumber wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSNumber(peer);
    }

        private static synchronized void ensureNumInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.FLOAT))
        );
    }

    /// numberWithInt:
    public static NSNumber numberWithInt(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithInt:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithInt: failed", t); }
    }

    /// numberWithInteger: (NSInteger long)
    public static NSNumber numberWithInteger(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithInteger:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithInteger: failed", t); }
    }

    /// numberWithDouble:
    public static NSNumber numberWithDouble(double value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithDouble:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithDouble: failed", t); }
    }

    /// numberWithBool: — uses int-based creation to avoid needing BOOL sig for ID.
    public static NSNumber numberWithBool(boolean value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithBool:"), value ? 1L : 0L);
            return wrap(s);
        } catch (Throwable t) {
            // fallback to numberWithInt 0/1
            return numberWithInt(value ? 1 : 0);
        }
    }

    /// numberWithFloat:
    public static NSNumber numberWithFloat(float value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.FLOAT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithFloat:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithFloat: failed", t); }
    }

    /// intValue
    public long intValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("intValue")); }
        catch (Throwable t) { throw new RuntimeException("intValue failed", t); }
    }

    /// integerValue
    public long integerValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("integerValue")); }
        catch (Throwable t) { throw new RuntimeException("integerValue failed", t); }
    }

    /// longValue
    public long longValue() {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("longValue"));
        } catch (Throwable t) { throw new RuntimeException("longValue failed", t); }
    }

    /// doubleValue
    public double doubleValue() {
        ensureNumInit();
        try { return (double) handles.hDoubleValue().invokeExact(peer, ObjC.sel("doubleValue")); }
        catch (Throwable t) { throw new RuntimeException("doubleValue failed", t); }
    }

    /// boolValue
    public boolean boolValue() {
        ensureNumInit();
        try { return (boolean) handles.hBoolValue().invokeExact(peer, ObjC.sel("boolValue")); }
        catch (Throwable t) { throw new RuntimeException("boolValue failed", t); }
    }

    /// floatValue
    public float floatValue() {
        ensureNumInit();
        try { return (float) handles.hFloatValue().invokeExact(peer, ObjC.sel("floatValue")); }
        catch (Throwable t) { throw new RuntimeException("floatValue failed", t); }
    }

    /// stringValue — returns NSString
    public NSString stringValue() {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment s = (MemorySegment) h.invokeExact(peer, ObjC.sel("stringValue"));
            return NSString.wrap(s);
        } catch (Throwable t) { throw new RuntimeException("stringValue failed", t); }
    }
}
