package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSDate — minimal wrapper over native `NSDate`.
///
/// Header-completeness (`NSDate.h`): every safe method whose shape is in the Sig vocabulary
/// is wrapped below. OMITTED — addTimeInterval: (deprecated; use dateByAddingTimeInterval:);
/// initWithTimeInterval.../initWithTimeInterval:...sinceDate: (covered by the dateWith*
/// factories); initWithCoder: (needs NSCoder); description via inherited description().
public final class NSDate extends NSObject {

            private record Handles(MethodHandle hTimeIntervalSince1970, MethodHandle hCompare) {}
    private static volatile Handles handles;

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

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.DOUBLE)), ObjC.handle(Sig.of(Ret.INT, Arg.ID)));
    }

    /// timeIntervalSince1970
    public double timeIntervalSince1970() {
        ensureInit();
        try { return (double) handles.hTimeIntervalSince1970().invokeExact(peer, ObjC.sel("timeIntervalSince1970")); }
        catch (Throwable t) { throw new RuntimeException("timeIntervalSince1970 failed", t); }
    }

    /// timeIntervalSinceNow
    public double timeIntervalSinceNow() {
        ensureInit();
        try { return (double) handles.hTimeIntervalSince1970().invokeExact(peer, ObjC.sel("timeIntervalSinceNow")); }
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
        try { return (long) handles.hCompare().invokeExact(peer, ObjC.sel("compare:"), other.peer()); }
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

    /// timeIntervalSinceReferenceDate — seconds since 2001-01-01 GMT.
    public double timeIntervalSinceReferenceDate() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("timeIntervalSinceReferenceDate"));
        } catch (Throwable t) { throw new RuntimeException("timeIntervalSinceReferenceDate failed", t); }
    }

    /// [NSDate dateWithTimeIntervalSinceReferenceDate:].
    public static NSDate dateWithTimeIntervalSinceReferenceDate(double seconds) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
            MemorySegment s = (MemorySegment) h.invokeExact(
                    ObjC.cls("NSDate"), ObjC.sel("dateWithTimeIntervalSinceReferenceDate:"), seconds);
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("dateWithTimeIntervalSinceReferenceDate: failed", t); }
    }

    /// [NSDate dateWithTimeInterval:sinceDate:].
    public static NSDate dateWithTimeIntervalSinceDate(double seconds, NSDate date) {
        ensureInit();
        if (date == null) throw new IllegalArgumentException("dateWithTimeInterval:sinceDate: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE, Arg.ID));
            MemorySegment s = (MemorySegment) h.invokeExact(
                    ObjC.cls("NSDate"), ObjC.sel("dateWithTimeInterval:sinceDate:"), seconds, date.peer());
            return wrap(s);
        } catch (Throwable t) { throw new RuntimeException("dateWithTimeInterval:sinceDate: failed", t); }
    }

    /// earlierDate: — the earlier of the two.
    public NSDate earlierDate(NSDate other) {
        ensureInit();
        if (other == null) throw new IllegalArgumentException("earlierDate: null");
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("earlierDate:"), other.peer()));
    }

    /// laterDate: — the later of the two.
    public NSDate laterDate(NSDate other) {
        ensureInit();
        if (other == null) throw new IllegalArgumentException("laterDate: null");
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("laterDate:"), other.peer()));
    }

    /// descriptionWithLocale: — NSLocale peer, or NULL for the canonical description.
    public NSString descriptionWithLocale(MemorySegment locale) {
        ensureInit();
        return NSString.wrap(ObjC.msgSendIdId(peer, ObjC.sel("descriptionWithLocale:"),
                (locale == null ? MemorySegment.NULL : locale)));
    }

    /// [NSDate now] — current date (10.15+).
    public static NSDate now() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("NSDate"), ObjC.sel("now")));
    }

    /// +timeIntervalSinceReferenceDate — current reference time. Named `system...` because
    /// the instance property owns the plain name (Java forbids static/instance overloads).
    public static double systemTimeIntervalSinceReferenceDate() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(ObjC.cls("NSDate"), ObjC.sel("timeIntervalSinceReferenceDate"));
        } catch (Throwable t) { throw new RuntimeException("timeIntervalSinceReferenceDate failed", t); }
    }
}
