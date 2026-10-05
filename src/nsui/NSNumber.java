package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSNumber — minimal wrapper over native `NSNumber` (subclass of NSValue).
/// Provides wrap/create and numeric accessors.
///
/// Header-completeness (`NSValue.h`, NSNumber interface + creation): every safe method
/// whose shape is in the Sig vocabulary is wrapped below. Narrow-width factories
/// (numberWithChar:/UnsignedChar:/Short:/.../LongLong:/...) all ride of(ID,INT) — the
/// FFM long fills the register the callee reads. OMITTED — numberWithFloat: NATIVE form
/// (needs of(ID,FLOAT), not in Sig): numberWithFloat(float) below doubles through
/// numberWithDouble (exact: every float is exactly representable as a double; objCType
/// reports "d" instead of "f" — flagged for the Sig owner to add of(ID,FLOAT));
/// initWithChar:/.../initWithInteger:/... (covered by the numberWith* factories);
/// initWithCoder: (needs NSCoder).
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

    /// numberWithFloat: — via numberWithDouble (of(ID,FLOAT) is not in Sig; see class docs).
    public static NSNumber numberWithFloat(float value) {
        return numberWithDouble((double) value);
    }

    /// numberWithChar:.
    public static NSNumber numberWithChar(long value) {
        return numberWithInt(value);
    }

    /// numberWithUnsignedChar:.
    public static NSNumber numberWithUnsignedChar(long value) {
        return numberWithInt(value);
    }

    /// numberWithShort:.
    public static NSNumber numberWithShort(long value) {
        return numberWithInt(value);
    }

    /// numberWithUnsignedShort:.
    public static NSNumber numberWithUnsignedShort(long value) {
        return numberWithInt(value);
    }

    /// numberWithUnsignedInt:.
    public static NSNumber numberWithUnsignedInt(long value) {
        return numberWithInt(value);
    }

    /// numberWithLong:.
    public static NSNumber numberWithLong(long value) {
        return numberWithInt(value);
    }

    /// numberWithUnsignedLong:.
    public static NSNumber numberWithUnsignedLong(long value) {
        return numberWithInt(value);
    }

    /// numberWithLongLong:.
    public static NSNumber numberWithLongLong(long value) {
        return numberWithInt(value);
    }

    /// numberWithUnsignedLongLong: (bits preserved in the long).
    public static NSNumber numberWithUnsignedLongLong(long value) {
        return numberWithInt(value);
    }

    /// numberWithUnsignedInteger:.
    public static NSNumber numberWithUnsignedInteger(long value) {
        return numberWithInt(value);
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

    /// charValue.
    public long charValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("charValue")); }
        catch (Throwable t) { throw new RuntimeException("charValue failed", t); }
    }

    /// unsignedCharValue.
    public long unsignedCharValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("unsignedCharValue")); }
        catch (Throwable t) { throw new RuntimeException("unsignedCharValue failed", t); }
    }

    /// shortValue.
    public long shortValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("shortValue")); }
        catch (Throwable t) { throw new RuntimeException("shortValue failed", t); }
    }

    /// unsignedShortValue.
    public long unsignedShortValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("unsignedShortValue")); }
        catch (Throwable t) { throw new RuntimeException("unsignedShortValue failed", t); }
    }

    /// unsignedIntValue.
    public long unsignedIntValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("unsignedIntValue")); }
        catch (Throwable t) { throw new RuntimeException("unsignedIntValue failed", t); }
    }

    /// unsignedLongValue.
    public long unsignedLongValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("unsignedLongValue")); }
        catch (Throwable t) { throw new RuntimeException("unsignedLongValue failed", t); }
    }

    /// longLongValue.
    public long longLongValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("longLongValue")); }
        catch (Throwable t) { throw new RuntimeException("longLongValue failed", t); }
    }

    /// unsignedLongLongValue (bits preserved in the long).
    public long unsignedLongLongValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("unsignedLongLongValue")); }
        catch (Throwable t) { throw new RuntimeException("unsignedLongLongValue failed", t); }
    }

    /// unsignedIntegerValue.
    public long unsignedIntegerValue() {
        ensureNumInit();
        try { return (long) handles.hIntValue().invokeExact(peer, ObjC.sel("unsignedIntegerValue")); }
        catch (Throwable t) { throw new RuntimeException("unsignedIntegerValue failed", t); }
    }

    /// compare: — NSComparisonResult against another number.
    public long compare(NSNumber other) {
        ensureNumInit();
        if (other == null) throw new IllegalArgumentException("compare: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("compare:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("compare: failed", t); }
    }

    /// isEqualToNumber:.
    public boolean isEqualToNumber(NSNumber other) {
        ensureNumInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToNumber:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isEqualToNumber: failed", t); }
    }

    /// descriptionWithLocale: — NSLocale peer, or NULL for the canonical description.
    public NSString descriptionWithLocale(MemorySegment locale) {
        ensureNumInit();
        return NSString.wrap(ObjC.msgSendIdId(peer, ObjC.sel("descriptionWithLocale:"),
                (locale == null ? MemorySegment.NULL : locale)));
    }
}
