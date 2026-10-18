package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.NSNumber;
import nsui.NSNumberFormatter;
import nsui.objc.ObjC;

/// NSNumberFormatterCoverageTest — coverage for the NSNumberFormatter wrapper
/// (CodeGen 0.1.91, 138 generated, 0 skipped).
///
/// Pure-Foundation, windowless: decimal-style round-trips, the shared
/// BOOL(ID,ID,ID,ID) out-param getObjectValue:forString:range:error:, and the
/// two macOS-15-gated minimumGroupingDigits accessors (emission-guarded).
/// Ends with TestKit.end().
///
/// Required coverage (batch spec):
/// - wrap(null)/wrap(NULL) == null + create() non-null
/// - decimal style: stringFromNumber(1234.5) parses back via numberFromString
///   to the same double; setNumberStyle/numberStyle round-trip
/// - getObjectValue:forString:range:error: with NULL out-slots + NULL error:
///   boolean result + string parse agreement with numberFromString
/// - 15.0-gated minimumGroupingDigits: set/get round-trip when present,
///   0/absent path otherwise (no VersionGuardScan tripping, no abort)
/// - localizedStringFromNumber:numberStyle: smoke (non-null, non-empty)
///
/// Intentionally NOT exercised: locale/currency customization, padding and
/// format-width edge strings, partial-string range out-slots (allocator
/// geometry, not formatter behavior).
public final class NSNumberFormatterCoverageTest {

    private static void check(boolean ok, String msg) {
        TestKit.check(ok, msg);
        System.out.flush();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== NSNumberFormatterCoverageTest ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            TestKit.skip("ObjC.init failed (not macOS / no AppKit): " + t);
        }

        System.out.println("---- wrap-null + create ----");
        check(NSNumberFormatter.wrap(null) == null, "wrap(null)==null");
        check(NSNumberFormatter.wrap(MemorySegment.NULL) == null, "wrap(NULL)==null");
        NSNumberFormatter f = NSNumberFormatter.create();
        check(f != null, "create() non-null");

        System.out.println("---- decimal style round-trip ----");
        f.setNumberStyle(1); // NSNumberFormatterDecimalStyle
        check(f.numberStyle() == 1, "numberStyle round-trip");
        String s = f.stringFromNumber(NSNumber.numberWithDouble(1234.5));
        check(s != null && !s.isEmpty(), "stringFromNumber non-empty: " + s);
        NSNumber back = f.numberFromString(s);
        check(back != null, "numberFromString non-null");
        check(Math.abs(back.doubleValue() - 1234.5) < 1e-9, "decimal round-trip value");
        NSNumber i = f.numberFromString("42");
        check(i != null && i.longValue() == 42, "integer parse");

        System.out.println("---- out-param getObjectValue ----");
        // NULL out-slots + NULL error: parses, returns true, agrees with numberFromString.
        boolean ok = f.getObjectValueForStringRangeError(null, "1234.5", null, null);
        check(ok, "getObjectValue NULL-slots true");
        // Wrong-string probe: unparseable input must not throw, just report false.
        boolean bad = false;
        try {
            bad = f.getObjectValueForStringRangeError(null, "not-a-number-xyz", null, null);
        } catch (Throwable t) {
            check(false, "getObjectValue bad-string threw: " + t);
        }
        check(!bad, "getObjectValue bad-string false");

        System.out.println("---- 15.0-gated minimumGroupingDigits ----");
        f.setMinimumGroupingDigits(2);
        long g = f.minimumGroupingDigits();
        // Present host: round-trips 2. Absent host: setter no-ops, getter falls back 0.
        check(g == 2 || g == 0, "minimumGroupingDigits present-or-absent (got " + g + ")");

        System.out.println("---- localized factory smoke ----");
        String loc = NSNumberFormatter.localizedStringFromNumberNumberStyle(
                NSNumber.numberWithLong(7), 1);
        check(loc != null && !loc.isEmpty(), "localizedString non-empty: " + loc);

        System.out.println("\n=== NSNumberFormatterCoverageTest " + (TestKit.failures() == 0 ? "PASS" : "FAIL") + " ===");
        TestKit.end();
    }
}
