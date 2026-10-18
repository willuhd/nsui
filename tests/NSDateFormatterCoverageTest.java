package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSDate;
import nsui.NSDateFormatter;
import nsui.objc.ObjC;

/// NSDateFormatterCoverageTest — coverage for the NSDateFormatter wrapper
/// (CodeGen 0.1.91, 78 generated, 0 skipped).
///
/// Pure-Foundation, windowless: fixed-format round-trips on a known epoch date,
/// style round-trips, the shared out-param getObjectValue, and the localized
/// factory that carries the new ID(ID,INT,INT) Sig shape.
/// Ends with TestKit.end().
///
/// Required coverage (batch spec):
/// - wrap(null)/wrap(NULL) == null + create() non-null
/// - dateFormat round-trip ("yyyy-MM-dd HH:mm:ss"): stringFromDate(1970-01-01
///   00:00:00 UTC epoch renders the fixed string; dateFromString parses it back
///   to the same time interval
/// - dateStyle/timeStyle set/get round-trips
/// - getObjectValue:forString:range:error: with NULL out-slots + NULL error on
///   the fixed-format string: true, and false for a garbage string (no throw)
/// - localizedStringFromDate:dateStyle:timeStyle: (short/short) non-empty
///
/// Intentionally NOT exercised: locale/timeZone customization, relative and
/// template formats, two-digit-year pivots, partial-string range out-slots.
public final class NSDateFormatterCoverageTest {

    private static void check(boolean ok, String msg) {
        TestKit.check(ok, msg);
        System.out.flush();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== NSDateFormatterCoverageTest ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            TestKit.skip("ObjC.init failed (not macOS / no AppKit): " + t);
        }

        System.out.println("---- wrap-null + create ----");
        check(NSDateFormatter.wrap(null) == null, "wrap(null)==null");
        check(NSDateFormatter.wrap(MemorySegment.NULL) == null, "wrap(NULL)==null");
        NSDateFormatter f = NSDateFormatter.create();
        check(f != null, "create() non-null");

        System.out.println("---- fixed-format round-trip ----");
        f.setDateFormat("yyyy-MM-dd HH:mm:ss");
        check("yyyy-MM-dd HH:mm:ss".equals(f.dateFormat()), "dateFormat round-trip");
        NSDate epoch = NSDate.dateWithTimeIntervalSince1970(0);
        // Render in GMT so the assertion is timezone-independent (NSTimeZone has
        // no wrapper yet, so the zone rides as a raw peer like other unwrapped ids).
        MemorySegment gmt = ObjC.msgSendIdId(ObjC.cls("NSTimeZone"),
                ObjC.sel("timeZoneWithName:"), ObjC.nsstring("GMT"));
        check(gmt != null && gmt.address() != 0, "GMT zone peer non-nil");
        f.setTimeZone(gmt);
        String s = f.stringFromDate(epoch);
        check("1970-01-01 00:00:00".equals(s), "epoch renders fixed (got " + s + ")");
        NSDate back = f.dateFromString("1970-01-01 00:00:00");
        check(back != null, "dateFromString non-null");
        check(Math.abs(back.timeIntervalSince1970() - 0.0) < 0.001, "epoch parses back");

        System.out.println("---- style round-trips ----");
        f.setDateStyle(1); // NSDateFormatterShortStyle
        f.setTimeStyle(1);
        check(f.dateStyle() == 1, "dateStyle round-trip");
        check(f.timeStyle() == 1, "timeStyle round-trip");

        System.out.println("---- out-param getObjectValue ----");
        f.setDateFormat("yyyy-MM-dd");
        boolean ok = f.getObjectValueForStringRangeError(null, "1970-01-02", null, null);
        check(ok, "getObjectValue NULL-slots true");
        boolean bad = false;
        try {
            bad = f.getObjectValueForStringRangeError(null, "not-a-date-xyz", null, null);
        } catch (Throwable t) {
            check(false, "getObjectValue bad-string threw: " + t);
        }
        check(!bad, "getObjectValue bad-string false");

        System.out.println("---- localized factory smoke ----");
        String loc = NSDateFormatter.localizedStringFromDateDateStyleTimeStyle(epoch, 1, 1);
        check(loc != null && !loc.isEmpty(), "localizedString non-empty: " + loc);

        System.out.println("\n=== NSDateFormatterCoverageTest " + (TestKit.failures() == 0 ? "PASS" : "FAIL") + " ===");
        TestKit.end();
    }
}
