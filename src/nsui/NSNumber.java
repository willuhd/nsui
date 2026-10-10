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
/// whose shape is in the Sig vocabulary is wrapped below. Narrow-width factories use
/// exact shapes — of(ID,BYTE) for Char/UnsignedChar, of(ID,SHORT) for Short/
/// UnsignedShort — while the int/long/long-long/integer families keep of(ID,INT)
/// (real encodings i/I/l/Q/q map nominally to INT and the callee reads the low bits
/// correctly on LE). OMITTED — numberWithFloat: NATIVE form
/// (needs of(ID,FLOAT), not in Sig): numberWithFloat(float) below doubles through
/// numberWithDouble (exact: every float is exactly representable as a double; objCType
/// reports "d" instead of "f" — flagged for the Sig owner to add of(ID,FLOAT));
/// initWithChar:/.../initWithInteger:/... (covered by the numberWith* factories);
/// initWithCoder: (needs NSCoder).
public final class NSNumber extends NSValue {

            private record Handles(MethodHandle hIntValue, MethodHandle hDoubleValue, MethodHandle hBoolValue, MethodHandle hFloatValue, MethodHandle hByteValue, MethodHandle hShortValue, MethodHandle hInt32Value) {}
    private static volatile Handles handles;

    private NSNumber(MemorySegment peer) { super(peer); }

    public static NSNumber wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSNumber(peer);
    }

        private static void ensureNumInit() {
            if (handles != null) return;
            ensureNumInitLocked();
        }

        private static synchronized void ensureNumInitLocked() {
        if (handles != null) return;
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.FLOAT)),
                ObjC.handle(Sig.of(Ret.BYTE)),
                ObjC.handle(Sig.of(Ret.SHORT)),
                ObjC.handle(Sig.of(Ret.INT32))
        );
        handles = h;
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

    /// numberWithBool: — BOOL is a 1-byte scalar, passed as a Java boolean.
    public static NSNumber numberWithBool(boolean value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.BOOL));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithBool:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithBool: failed", t); }
    }

    /// numberWithFloat: — via numberWithDouble (of(ID,FLOAT) is not in Sig; see class docs).
    public static NSNumber numberWithFloat(float value) {
        return numberWithDouble((double) value);
    }

    /// numberWithChar: (C char, 1 byte).
    public static NSNumber numberWithChar(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.BYTE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithChar:"), (byte) value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithChar: failed", t); }
    }

    /// numberWithUnsignedChar: (C unsigned char, 1 byte, low 8 bits kept).
    public static NSNumber numberWithUnsignedChar(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.BYTE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithUnsignedChar:"), (byte) (value & 0xFFL));
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithUnsignedChar: failed", t); }
    }

    /// numberWithShort: (C short, 2 bytes).
    public static NSNumber numberWithShort(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.SHORT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithShort:"), (short) value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithShort: failed", t); }
    }

    /// numberWithUnsignedShort: (low 16 bits kept).
    public static NSNumber numberWithUnsignedShort(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.SHORT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithUnsignedShort:"), (short) (value & 0xFFFFL));
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithUnsignedShort: failed", t); }
    }

    /// numberWithUnsignedInt:.
    public static NSNumber numberWithUnsignedInt(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithUnsignedInt:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithUnsignedInt: failed", t); }
    }

    /// numberWithLong:.
    public static NSNumber numberWithLong(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithLong:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithLong: failed", t); }
    }

    /// numberWithUnsignedLong:.
    public static NSNumber numberWithUnsignedLong(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithUnsignedLong:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithUnsignedLong: failed", t); }
    }

    /// numberWithLongLong:.
    public static NSNumber numberWithLongLong(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithLongLong:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithLongLong: failed", t); }
    }

    /// numberWithUnsignedLongLong: (bits preserved in the long).
    public static NSNumber numberWithUnsignedLongLong(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithUnsignedLongLong:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithUnsignedLongLong: failed", t); }
    }

    /// numberWithUnsignedInteger:.
    public static NSNumber numberWithUnsignedInteger(long value) {
        ensureNumInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSNumber"), ObjC.sel("numberWithUnsignedInteger:"), value);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("numberWithUnsignedInteger: failed", t); }
    }

    /// intValue — C int (32-bit, signed).
    public long intValue() {
        ensureNumInit();
        try { return (long) (int) handles.hInt32Value().invokeExact(peer, ObjC.sel("intValue")); }
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

    /// charValue — C char (1 byte, signed).
    public long charValue() {
        ensureNumInit();
        try { return (long) (byte) handles.hByteValue().invokeExact(peer, ObjC.sel("charValue")); }
        catch (Throwable t) { throw new RuntimeException("charValue failed", t); }
    }

    /// unsignedCharValue — C unsigned char (1 byte, zero-extended).
    public long unsignedCharValue() {
        ensureNumInit();
        try { return ((byte) handles.hByteValue().invokeExact(peer, ObjC.sel("unsignedCharValue"))) & 0xFFL; }
        catch (Throwable t) { throw new RuntimeException("unsignedCharValue failed", t); }
    }

    /// shortValue (C short, 2 bytes, sign-extended to long).
    public long shortValue() {
        ensureNumInit();
        try { return (long) (short) handles.hShortValue().invokeExact(peer, ObjC.sel("shortValue")); }
        catch (Throwable t) { throw new RuntimeException("shortValue failed", t); }
    }

    /// unsignedShortValue (C unsigned short, 2 bytes, zero-extended to long).
    public long unsignedShortValue() {
        ensureNumInit();
        try { return ((short) handles.hShortValue().invokeExact(peer, ObjC.sel("unsignedShortValue"))) & 0xFFFFL; }
        catch (Throwable t) { throw new RuntimeException("unsignedShortValue failed", t); }
    }

    /// unsignedIntValue — C unsigned int (32-bit, zero-extended).
    public long unsignedIntValue() {
        ensureNumInit();
        try { return ((int) handles.hInt32Value().invokeExact(peer, ObjC.sel("unsignedIntValue"))) & 0xFFFFFFFFL; }
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
