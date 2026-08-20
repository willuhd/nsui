package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSDate — minimal wrapper over native `NSDate`.
public final class NSDate extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hTimeIntervalSince1970; // (id, SEL) -> double
    private static MethodHandle hTimeIntervalSinceNow;  // (id, SEL) -> double
    private static MethodHandle hCompare;               // (id, SEL, id) -> long

    private NSDate(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSDate wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDate(peer);
    }

    /// [NSDate date] — now.
    public static NSDate date() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSDate"), ObjC.sel("date"));
        return wrap(s);
    }

    /// [NSDate dateWithTimeIntervalSince1970:]
    public static NSDate dateWithTimeIntervalSince1970(double seconds) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSince1970:"), seconds);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("dateWithTimeIntervalSince1970: failed", t); }
    }

    /// [NSDate dateWithTimeIntervalSinceNow:]
    public static NSDate dateWithTimeIntervalSinceNow(double seconds) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSinceNow:"), seconds);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("dateWithTimeIntervalSinceNow: failed", t); }
    }

    /// [NSDate distantPast]
    public static NSDate distantPast() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSDate"), ObjC.sel("distantPast"));
        return wrap(s);
    }

    /// [NSDate distantFuture]
    public static NSDate distantFuture() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(ObjC.cls("NSDate"), ObjC.sel("distantFuture"));
        return wrap(s);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hTimeIntervalSince1970 = ObjC.handle(Sig.of(Ret.DOUBLE));
        hTimeIntervalSinceNow = ObjC.handle(Sig.of(Ret.DOUBLE));
        hCompare = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
        initialized = true;
    }

    /// timeIntervalSince1970
    public double timeIntervalSince1970() {
        ensureInit();
        try { return (double) hTimeIntervalSince1970.invokeExact(peer, ObjC.sel("timeIntervalSince1970")); }
        catch (Throwable t) { throw new RuntimeException("timeIntervalSince1970 failed", t); }
    }

    /// timeIntervalSinceNow
    public double timeIntervalSinceNow() {
        ensureInit();
        try { return (double) hTimeIntervalSinceNow.invokeExact(peer, ObjC.sel("timeIntervalSinceNow")); }
        catch (Throwable t) { throw new RuntimeException("timeIntervalSinceNow failed", t); }
    }

    /// timeIntervalSinceDate:
    public double timeIntervalSinceDate(NSDate other) {
        ensureInit();
        if (other == null) throw new IllegalArgumentException("other null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE, Arg.ID));
            return (double) h.invokeExact(peer, ObjC.sel("timeIntervalSinceDate:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("timeIntervalSinceDate: failed", t); }
    }

    /// compare: — NSComparisonResult.
    public long compare(NSDate other) {
        ensureInit();
        if (other == null) throw new IllegalArgumentException("other null");
        try { return (long) hCompare.invokeExact(peer, ObjC.sel("compare:"), other.peer()); }
        catch (Throwable t) { throw new RuntimeException("compare: failed", t); }
    }

    /// isEqualToDate:
    public boolean isEqualToDate(NSDate other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqualToDate:"), other.peer());
        } catch (Throwable t) { throw new RuntimeException("isEqualToDate: failed", t); }
    }

    /// dateByAddingTimeInterval: — returns new date.
    public NSDate dateByAddingTimeInterval(double seconds) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
            MemorySegment s = (MemorySegment) h.invokeExact(peer, ObjC.sel("dateByAddingTimeInterval:"), seconds);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("dateByAddingTimeInterval: failed", t); }
    }
}
