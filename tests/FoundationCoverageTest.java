package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import nsui.CAConstraint;
import nsui.CALayer;
import nsui.MTLRenderPassDescriptor;
import nsui.MTLStencilDescriptor;
import nsui.NSArray;
import nsui.NSAttributedString;
import nsui.NSData;
import nsui.NSDate;
import nsui.NSDictionary;
import nsui.NSEdgeInsets;
import nsui.NSIndexSet;
import nsui.NSMutableAttributedString;
import nsui.NSMutableData;
import nsui.NSMutableIndexSet;
import nsui.NSMutableOrderedSet;
import nsui.NSMutableSet;
import nsui.NSNumber;
import nsui.NSObject;
import nsui.NSOrderedSet;
import nsui.NSPoint;
import nsui.NSRange;
import nsui.NSRect;
import nsui.NSRunningApplication;
import nsui.NSScreen;
import nsui.NSSet;
import nsui.NSSize;
import nsui.NSString;
import nsui.NSTextField;
import nsui.NSValue;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// FoundationCoverageTest — header-completeness coverage for the Foundation/value-type
/// batch (NSObject, NSString, NSArray, NSDictionary, NSSet/NSMutableSet, NSIndexSet/
/// NSMutableIndexSet, NSOrderedSet/NSMutableOrderedSet, NSNumber, NSDate, NSData/
/// NSMutableData, NSValue, NSRange/NSPoint/NSSize/NSRect/NSEdgeInsets).
///
/// Pure-memory (Foundation-only): no windows (hidden or otherwise), no audio, no run loop.
/// File IO stays under a unique /tmp dir (never inside the repo).
/// Uncaught ObjC exceptions are process-fatal (see nsui.objc.Exceptions), so every call
/// below stays on strictly-valid paths (valid indexes/ranges, mutable peers for mutation).
/// Narrow-reader oracles (testNarrowReaders) additionally cover the exact-32-bit
/// returns found by the reader audit (NSWindowDepth/pid_t/CAConstraintAttribute/int
/// as signed INT32, CGDirectDisplayID/MTL masks as unsigned INT32). Those readers
/// live outside Foundation, but every oracle is windowless: descriptors, a detached
/// button, the app identity, and screen queries — plus one hidden window, never
/// shown, for the instance depthLimit reader.
public final class FoundationCoverageTest {

    /// Long strings defeat tagged-pointer identity so peer-identity checks are honest.
    private static final String L1 = "alpha-long-string-value-001-xyz-0123456789";
    private static final String L2 = "beta-long-string-value-002-xyz-0123456789";
    private static final String L3 = "gamma-long-string-value-003-xyz-0123456789";

    public static void main(String[] args) throws Exception {
        System.out.println("=== FoundationCoverageTest — Foundation/value-type batch ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            String m = String.valueOf(t.getMessage()).toLowerCase() + " " + String.valueOf(t).toLowerCase();
            if (m.contains("connection") || m.contains("dlopen") || m.contains("appkit") || m.contains("main thread")) {
                TestKit.skip("ObjC.init unavailable (headless session?): " + t.getMessage());
            }
            System.out.println("FAIL: ObjC.init threw unexpected: " + t);
            t.printStackTrace(System.out);
            System.exit(1);
        }

        testNSObject();
        testNSStringCompare();
        testNSStringSearch();
        testNSStringValues();
        testNSStringCase();
        testNSStringSplitReplace();
        testNSStringEncoding();
        testNSStringSubstrings();
        testNSArrayImmutable();
        testNSArrayMutable();
        testNSDictionary();
        testSets();
        testIndexSets();
        testOrderedSets();
        testNumbers();
        testDates();
        testData();
        testValues();
        testRecords();
        testNarrowReaders();
        testNegativeGuards();

        System.out.println("\n=== FoundationCoverageTest " + (TestKit.failures() == 0 ? "PASS" : "FAIL") + " ===");
        TestKit.end();
    }

    // ---------------------------------------------------------- NSObject

    private static void testNSObject() {
        System.out.println("\n-- NSObject identity/introspection/KVC --");
        TestKit.check(NSObject.wrap(null) == null, "NSObject.wrap(null)==null");
        TestKit.check(NSObject.wrap(MemorySegment.NULL) == null, "NSObject.wrap(NULL)==null");
        NSString s = NSString.of("hi");
        TestKit.check("hi".equals(s.describe().string()), "describe()==hi");
        TestKit.check(NSString.of("abc").hash() == NSString.of("abc").hash(), "equal strings equal hash");
        TestKit.check(s.isEqual(NSString.of("hi")), "isEqual true");
        TestKit.check(!s.isEqual(NSString.of("bye")), "isEqual false");
        TestKit.check(!s.isEqual(null), "isEqual nil false");
        TestKit.check(s.objCClass() != null && s.isKindOfClass(s.objCClass()), "objCClass kind");
        // Class clusters: concrete strings are NOT exact members of NSString...
        TestKit.check(!s.isMemberOfClass("NSString"), "string not exact member of NSString (cluster)");
        TestKit.check(s.isKindOfClass("NSString"), "string isKindOfClass NSString");
        TestKit.check(s.isKindOfClass("NSObject"), "string isKindOfClass NSObject");
        TestKit.check(!s.isMemberOfClass("NSObject"), "string not member of NSObject");
        TestKit.check(!s.isMemberOfClass((String) null), "isMemberOfClass null false");
        // ...while a direct NSObject is an exact member.
        NSObject plain = NSObject.wrap(ObjC.msgSendId(ObjC.cls("NSObject"), ObjC.sel("new")));
        TestKit.check(plain.objCClass() != null && plain.objCClass().address() == ObjC.cls("NSObject").address(),
                "NSObject exact objCClass");
        TestKit.check(plain.isMemberOfClass("NSObject"), "[NSObject new] isMemberOfClass NSObject");
        TestKit.check(plain.isKindOfClass("NSObject"), "[NSObject new] isKindOfClass NSObject");
        TestKit.check(s.respondsToSelector(ObjC.sel("length")), "respondsToSelector length");
        TestKit.check(!s.respondsToSelector(ObjC.sel("nopeNotReal:")), "respondsToSelector bogus false");
        TestKit.check(!s.respondsToSelector(null), "respondsToSelector null false");
        TestKit.check(!s.isProxy(), "isProxy false");
        TestKit.check("q".equals(ObjC.toString(((NSObject) NSString.of("q").copy()).peer())), "copy content");
        TestKit.check(NSString.of("q").mutableCopy().isKindOfClass("NSMutableString"), "mutableCopy class");
        // KVC on a mutable dictionary (arbitrary keys are safe there).
        NSDictionary d = NSDictionary.mutableDictionary();
        d.setValueForKey(NSString.of("vv"), "kk");
        TestKit.check("vv".equals(ObjC.toString(d.valueForKey("kk"))), "KVC set/get");
        TestKit.check(d.valueForKey("missing") == null, "KVC missing nil");
        TestKit.check(d.valueForKey((String) null) == null, "KVC null key nil");
        d.setValueForKey(null, "kk");
        TestKit.check(d.valueForKey("kk") == null, "KVC nil value removes");
        d.setValueForKey(NSString.of("x"), null);
        TestKit.check(d.count() == 0, "KVC null key no-op");
        s.willChangeValueForKey("k");
        TestKit.noThrow("KVO manual notify no-throw", () -> s.didChangeValueForKey("k"));
        long v0 = NSObject.version("NSArray");
        NSObject.setVersion("NSArray", v0 + 1);
        TestKit.check(NSObject.version("NSArray") == v0 + 1, "version set/get");
        NSObject.setVersion("NSArray", v0);
        TestKit.check(NSObject.version("NSArray") == v0, "version restored");
        TestKit.check(NSObject.supportsSecureCoding("NSString"), "NSString supportsSecureCoding");
        TestKit.check(NSObject.supportsSecureCoding("NSArray"), "NSArray supportsSecureCoding");
    }

    // ---------------------------------------------------------- NSString

    private static void testNSStringCompare() {
        System.out.println("\n-- NSString compare --");
        TestKit.check(NSString.of("b").compare(NSString.of("a")) > 0, "compare b>a");
        TestKit.check(NSString.of("a").compare("b") < 0, "compare a<b (String)");
        TestKit.check(NSString.of("a").compare("a") == 0, "compare equal");
        TestKit.check(NSString.of("a").caseInsensitiveCompare("A") == 0, "caseInsensitive equal");
        TestKit.check(NSString.of("a").caseInsensitiveCompare(NSString.of("B")) < 0, "caseInsensitive a<B");
        TestKit.check(NSString.of("a").localizedCompare(NSString.of("b")) < 0, "localizedCompare");
        TestKit.check(NSString.of("A").localizedCaseInsensitiveCompare(NSString.of("a")) == 0,
                "localizedCaseInsensitive");
        TestKit.check(NSString.of("b").localizedStandardCompare(NSString.of("a")) > 0, "localizedStandard");
        TestKit.check(NSString.of("hello").hasPrefix("he"), "hasPrefix");
        TestKit.check(NSString.of("hello").hasPrefix(NSString.of("he")), "hasPrefix NSString");
        TestKit.check(!NSString.of("hello").hasPrefix("lo"), "hasPrefix false");
        TestKit.check(!NSString.of("hello").hasPrefix((String) null), "hasPrefix null false");
        TestKit.check(NSString.of("hello").hasSuffix("lo"), "hasSuffix");
        TestKit.check(!NSString.of("hello").hasSuffix("he"), "hasSuffix false");
        TestKit.check(NSString.of("hello").containsString("ell"), "containsString");
        TestKit.check(NSString.of("hello").containsString(NSString.of("ell")), "containsString NSString");
        TestKit.check(!NSString.of("hello").containsString("z"), "containsString false");
        TestKit.check(NSString.of("HELLO").localizedCaseInsensitiveContainsString(NSString.of("hello")),
                "localizedCaseInsensitiveContains");
        TestKit.check(NSString.of("hello").localizedStandardContainsString("ELL"), "localizedStandardContains");
        NSRange loc = NSString.of("hello world").localizedStandardRangeOfString("world");
        TestKit.check(loc.location() == 6 && loc.length() == 5, "localizedStandardRangeOfString {6,5} got " + loc);
    }

    private static void testNSStringSearch() {
        System.out.println("\n-- NSString search --");
        NSRange back = NSString.of("hello world hello").rangeOfString("hello", NSString.BACKWARDS_SEARCH);
        TestKit.check(back.location() == 12 && back.length() == 5, "backwards finds last got " + back);
        NSRange ci = NSString.of("HELLO").rangeOfString("hello", NSString.CASE_INSENSITIVE_SEARCH);
        TestKit.check(ci.location() == 0 && ci.length() == 5, "caseInsensitive range got " + ci);
        NSRange miss = NSString.of("hello").rangeOfString("z", 0);
        TestKit.check(miss.location() == NSRange.NOT_FOUND, "options miss NOT_FOUND");
        TestKit.check(NSString.of("hello").rangeOfString((String) null, 0).location() == NSRange.NOT_FOUND,
                "options null NOT_FOUND");
        MemorySegment digits = ObjC.msgSendId(ObjC.cls("NSCharacterSet"), ObjC.sel("decimalDigitCharacterSet"));
        TestKit.check(digits != null && digits.address() != 0, "digit charset non-nil");
        NSRange dr = NSString.of("abc123").rangeOfCharacterFromSet(digits);
        TestKit.check(dr.location() == 3 && dr.length() == 1, "digit range {3,1} got " + dr);
        NSRange drBack = NSString.of("abc123").rangeOfCharacterFromSet(digits, NSString.BACKWARDS_SEARCH);
        TestKit.check(drBack.location() == 5 && drBack.length() == 1, "digit backwards {5,1} got " + drBack);
        TestKit.check(NSString.of("abc").rangeOfCharacterFromSet(null).location() == NSRange.NOT_FOUND,
                "charset null NOT_FOUND");
        NSRange comp = NSString.of("a\uD83D\uDE00b").rangeOfComposedCharacterSequencesForRange(new NSRange(0, 4));
        TestKit.check(comp.equals(new NSRange(0, 4)), "composed range stable got " + comp);
        NSRange line = NSString.of("a\nb\n").lineRangeForRange(new NSRange(0, 0));
        TestKit.check(line.equals(new NSRange(0, 2)), "lineRange {0,2} got " + line);
        NSRange para = NSString.of("a\nb\n").paragraphRangeForRange(new NSRange(0, 0));
        TestKit.check(para.equals(new NSRange(0, 2)), "paragraphRange {0,2} got " + para);
    }

    private static void testNSStringValues() {
        System.out.println("\n-- NSString numeric values --");
        TestKit.check(Math.abs(NSString.of("3.14").doubleValue() - 3.14) < 1e-9, "doubleValue");
        TestKit.check(Math.abs(NSString.of("3.14").floatValue() - 3.14f) < 1e-6, "floatValue");
        TestKit.check(NSString.of("42").intValue() == 42, "intValue");
        TestKit.check(NSString.of("42").integerValue() == 42, "integerValue");
        TestKit.check(NSString.of("42").longLongValue() == 42, "longLongValue");
        TestKit.check(NSString.of("YES").boolValue(), "boolValue YES");
        TestKit.check(!NSString.of("0").boolValue(), "boolValue 0 false");
        TestKit.check("  3  ".equals(NSString.of("  3  ").stringByTrimmingCharactersInSet(null).string()),
                "null-charset trim is identity");
    }

    private static void testNSStringCase() {
        System.out.println("\n-- NSString case --");
        TestKit.check("HI".equals(NSString.of("hi").uppercaseString().string()), "uppercaseString");
        TestKit.check("hi".equals(NSString.of("HI").lowercaseString().string()), "lowercaseString");
        TestKit.check("Hi".equals(NSString.of("hi").capitalizedString().string()), "capitalizedString");
        TestKit.check("HI".equals(NSString.of("hi").localizedUppercaseString().string()), "localizedUppercase");
        TestKit.check("hi".equals(NSString.of("HI").localizedLowercaseString().string()), "localizedLowercase");
        TestKit.check("hi".equals(NSString.of("HI").lowercaseStringWithLocale(null).string()), "locale NULL canonical");
        TestKit.check("HI".equals(NSString.of("hi").uppercaseStringWithLocale(null).string()), "upper locale NULL");
        TestKit.check("Hi".equals(NSString.of("hi").capitalizedStringWithLocale(null).string()), "cap locale NULL");
        TestKit.check("Hello".equals(
                NSString.of("H\u00e9llo").stringByFoldingWithOptions(NSString.DIACRITIC_INSENSITIVE_SEARCH, null).string()),
                "fold diacritics");
    }

    private static void testNSStringSplitReplace() {
        System.out.println("\n-- NSString split/replace/transform --");
        MemorySegment ws = ObjC.msgSendId(ObjC.cls("NSCharacterSet"), ObjC.sel("whitespaceCharacterSet"));
        NSArray parts = NSString.of("a b c").componentsSeparatedByString(" ");
        TestKit.check(parts.count() == 3 && "a".equals(parts.stringAt(0).string()), "componentsSeparatedByString");
        NSArray parts2 = NSString.of("a b c").componentsSeparatedByCharactersInSet(ws);
        TestKit.check(parts2.count() == 3, "componentsSeparatedByCharactersInSet");
        TestKit.check("hi".equals(NSString.of("  hi  ").stringByTrimmingCharactersInSet(ws).string()), "trim");
        TestKit.check("bbb".equals(NSString.of("aaa").stringByReplacingOccurrencesOfString("a", "b").string()),
                "replaceOccurrences");
        TestKit.check("bbb".equals(NSString.of("aaa")
                .stringByReplacingOccurrencesOfString(NSString.of("a"), NSString.of("b")).string()),
                "replaceOccurrences NSString");
        TestKit.check("hELLo".equals(
                NSString.of("hello").stringByReplacingCharactersInRange(new NSRange(1, 3), "ELL").string()),
                "replaceCharactersInRange");
        TestKit.check("\u30b9\u30b7".equals(
                NSString.of("sushi").stringByApplyingTransform("Latin-Katakana", false).string()),
                "katakana transform");
        TestKit.check(NSString.of("sushi").stringByApplyingTransform("Nope-Nope", false) == null,
                "invalid transform nil");
        TestKit.check(NSString.of("x").stringByApplyingTransform((String) null, false) == null, "null transform nil");
        TestKit.check("ab".equals(NSString.of("a").stringByAppendingString("b").string()), "append String");
        TestKit.check("ab".equals(NSString.of("a").stringByAppendingString(NSString.of("b")).string()), "append NSString");
        TestKit.check(NSString.of("\u00e9").decomposedStringWithCanonicalMapping().length() == 2, "decomposed len 2");
        TestKit.check(NSString.of("\u00e9").precomposedStringWithCanonicalMapping().length() == 1, "precomposed len 1");
        TestKit.check(NSString.of("\u00e9").decomposedStringWithCompatibilityMapping().length() == 2, "decomp-compat");
        TestKit.check(NSString.of("\u00e9").precomposedStringWithCompatibilityMapping().length() == 1, "precomp-compat");
    }

    private static void testNSStringEncoding() {
        System.out.println("\n-- NSString encodings --");
        NSData utf8 = NSString.of("hi").dataUsingEncoding(NSString.UTF8_STRING_ENCODING);
        TestKit.check(utf8 != null && utf8.length() == 2, "dataUsingEncoding len 2");
        TestKit.check("hi".equals(NSString.stringWithUTF8Bytes(utf8.toByteArray()).string()), "UTF8 bridge round-trip");
        TestKit.check(NSString.stringWithUTF8Bytes(null) == null, "UTF8Bytes null nil");
        TestKit.check(NSString.stringWithUTF8Bytes(new byte[]{(byte) 0xFF}) == null, "invalid UTF8 nil");
        NSData lossy = NSString.of("hi").dataUsingEncoding(NSString.UTF8_STRING_ENCODING, false);
        TestKit.check(lossy != null && lossy.length() == 2, "dataUsingEncoding lossy");
        TestKit.check(NSString.of("hi").canBeConvertedToEncoding(NSString.UTF8_STRING_ENCODING), "canConvert UTF8");
        TestKit.check(!NSString.of("h\u00e9llo").canBeConvertedToEncoding(NSString.ASCII_STRING_ENCODING),
                "e-acute not ASCII");
        TestKit.check(NSString.of("hello").lengthOfBytesUsingEncoding(NSString.UTF8_STRING_ENCODING) == 5,
                "lengthOfBytes 5");
        TestKit.check(NSString.of("hello").maximumLengthOfBytesUsingEncoding(NSString.UTF8_STRING_ENCODING) >= 5,
                "maxLengthBytes >=5");
        TestKit.check(NSString.of("hello").fastestEncoding() == NSString.ASCII_STRING_ENCODING, "fastest ASCII");
        TestKit.check(NSString.of("hello").smallestEncoding() == NSString.ASCII_STRING_ENCODING, "smallest ASCII");
        MemorySegment cstr = NSString.of("hi").cStringUsingEncoding(NSString.ASCII_STRING_ENCODING);
        TestKit.check(cstr != null && cstr.reinterpret(1).get(ValueLayout.JAVA_BYTE, 0) == 104, "cString h");
        MemorySegment utf8ptr = NSString.of("hi").utf8CString();
        TestKit.check(utf8ptr != null && utf8ptr.reinterpret(2).get(ValueLayout.JAVA_BYTE, 1) == 105, "utf8CString i");
        TestKit.check(NSString.localizedNameOfStringEncoding(NSString.UTF8_STRING_ENCODING) != null, "localizedName");
        TestKit.check(NSString.defaultCStringEncoding() > 0, "defaultCStringEncoding >0");
        MemorySegment encs = NSString.availableStringEncodings();
        TestKit.check(encs != null, "availableStringEncodings non-nil");
        boolean foundUtf8 = false;
        boolean firstIsDefault = encs.reinterpret(8).get(ValueLayout.JAVA_LONG, 0) == NSString.defaultCStringEncoding();
        for (int i = 0; i < 64; i++) {
            if (encs.reinterpret((i + 1L) * 8).get(ValueLayout.JAVA_LONG, i * 8L) == NSString.UTF8_STRING_ENCODING) {
                foundUtf8 = true;
                break;
            }
        }
        TestKit.check(firstIsDefault, "encodings[0]==default");
        TestKit.check(foundUtf8, "encodings contain UTF8");
        TestKit.check("z".equals(NSString.stringWithString(NSString.of("z")).string()), "stringWithString");
        TestKit.check(NSString.stringWithString(null) == null, "stringWithString null");
    }

    private static void testNSStringSubstrings() {
        System.out.println("\n-- NSString substrings/index --");
        TestKit.check(NSString.of("hello").characterAtIndex(1) == 101, "characterAtIndex e==101");
        TestKit.check("llo".equals(NSString.of("hello").substringFromIndex(2).string()), "substringFromIndex");
        TestKit.check("he".equals(NSString.of("hello").substringToIndex(2).string()), "substringToIndex");
    }

    // ---------------------------------------------------------- NSArray

    private static NSArray strArray(String... items) {
        NSArray a = NSArray.mutableArray();
        for (String s : items) a.addObject(NSString.of(s));
        return a;
    }

    private static String joined(NSArray a) {
        StringBuilder b = new StringBuilder();
        for (long i = 0; i < a.count(); i++) b.append(ObjC.toString(a.objectAtIndex(i))).append(i + 1 < a.count() ? "," : "");
        return b.toString();
    }

    private static void testNSArrayImmutable() {
        System.out.println("\n-- NSArray immutable --");
        TestKit.check(NSArray.wrap(null) == null, "NSArray.wrap null");
        TestKit.check(NSArray.arrayWithObject(NSString.of("solo")).count() == 1, "arrayWithObject");
        TestKit.check(NSArray.arrayWithArray(strArray("a", "b")).count() == 2, "arrayWithArray");
        TestKit.check(NSArray.arrayWithCapacity(8) != null, "arrayWithCapacity");
        NSArray a = strArray("b", "a", "c");
        TestKit.check(a.arrayByAddingObject(NSString.of("d")).count() == 4, "arrayByAddingObject");
        TestKit.check(a.arrayByAddingObjectsFromArray(strArray("x")).count() == 4, "arrayByAddingObjectsFromArray");
        TestKit.check("b-a-c".equals(a.componentsJoinedByString("-").string()), "componentsJoined");
        TestKit.check("b,a,c".equals(a.componentsJoinedByString(NSString.of(",")).string()), "componentsJoined NSString");
        TestKit.check(a.containsObject(NSString.of("a")), "native containsObject YES");
        TestKit.check(!a.containsObject(NSString.of("z")), "native containsObject NO");
        TestKit.check(!a.containsObject((NSObject) null), "native containsObject null false");
        // Value (not identity) semantics: distinct equal long-string peers still match.
        TestKit.check(a.count() == 3, "count 3");
        TestKit.check("b".equals(ObjC.toString(a.firstObject())), "firstObject");
        TestKit.check("a".equals(ObjC.toString(a.firstObjectCommonWithArray(strArray("z", "a")))), "firstCommon");
        TestKit.check(a.firstObjectCommonWithArray(strArray("zzz")) == null, "firstCommon none nil");
        TestKit.check(a.indexOfObject(NSString.of("a")) == 1, "indexOfObject");
        TestKit.check(a.indexOfObject(NSString.of("z")) == NSRange.NOT_FOUND, "indexOfObject miss");
        NSArray distinctTest = strArray(L1, L2);
        TestKit.check(distinctTest.indexOfObject(NSString.of(L1)) == 0, "indexOfObject value match");
        TestKit.check(distinctTest.indexOfObjectIdenticalTo(NSString.of(L1)) == NSRange.NOT_FOUND,
                "indexOfObjectIdenticalTo distinct peer miss");
        TestKit.check(distinctTest.indexOfObjectIdenticalTo(distinctTest.objectAt(0)) == 0, "identical same peer 0");
        TestKit.check(a.isEqualToArray(strArray("b", "a", "c")), "isEqualToArray true");
        TestKit.check(!a.isEqualToArray(strArray("a", "b", "c")), "isEqualToArray order false");
        TestKit.check(!a.isEqualToArray(null), "isEqualToArray null false");
        TestKit.check("a".equals(ObjC.toString(a.objectAtIndexedSubscript(1))), "objectAtIndexedSubscript");
        NSArray sub = a.objectsAtIndexes(NSIndexSet.indexSetWithIndexesInRange(new NSRange(0, 2)));
        TestKit.check(sub.count() == 2 && "b".equals(ObjC.toString(sub.objectAtIndex(0))), "objectsAtIndexes");
        NSArray win = a.subarrayWithRange(new NSRange(1, 2));
        TestKit.check(win.count() == 2 && "a".equals(ObjC.toString(win.objectAtIndex(0))), "subarrayWithRange");
        NSArray sorted = a.sortedArrayUsingSelector(ObjC.sel("compare:"));
        TestKit.check("a,b,c".equals(joined(sorted)), "sortedArrayUsingSelector got " + joined(sorted));
        TestKit.check(a.descriptionWithLocale(null) != null, "descriptionWithLocale");
        TestKit.check(a.objectEnumerator() != null, "objectEnumerator");
        TestKit.check(a.reverseObjectEnumerator() != null, "reverseObjectEnumerator");
        TestKit.check(sorted.sortedArrayHint() != null, "sortedArrayHint");
        a.makeObjectsPerformSelector(ObjC.sel("description"));
        a.makeObjectsPerformSelectorWithObject(ObjC.sel("isEqual:"), ObjC.nsstring("a"));
        TestKit.check(a.count() == 3, "makeObjectsPerform no-throw");
    }

    private static void testNSArrayMutable() {
        System.out.println("\n-- NSArray mutable --");
        NSArray m = strArray("a", "b", "c");
        m.insertObjectAtIndex(NSString.of("X"), 1);
        TestKit.check("a,X,b,c".equals(joined(m)), "insertObjectAtIndex got " + joined(m));
        m.insertObjectsAtIndexes(strArray("Y"), NSIndexSet.indexSetWithIndex(0));
        TestKit.check("Y,a,X,b,c".equals(joined(m)), "insertObjectsAtIndexes got " + joined(m));
        m.replaceObjectAtIndex(0, NSString.of("Q"));
        TestKit.check("Q,a,X,b,c".equals(joined(m)), "replaceObjectAtIndex");
        m.replaceObjectsAtIndexes(NSIndexSet.indexSetWithIndex(0), strArray("W"));
        m.replaceObjectsInRange(new NSRange(1, 2), strArray("V"));
        TestKit.check("W,V,b,c".equals(joined(m)), "replaceAtIndexes+InRange got " + joined(m));
        m.setObjectAtIndexedSubscript(NSString.of("Z"), 0);
        TestKit.check("Z".equals(ObjC.toString(m.objectAtIndex(0))), "setObjectAtIndexedSubscript");
        m.exchangeObjectAtIndex(0, 3);
        TestKit.check("c,V,b,Z".equals(joined(m)), "exchange got " + joined(m));
        m.sortUsingSelector(ObjC.sel("compare:"));
        TestKit.check("V,Z,b,c".equals(joined(m)), "sortUsingSelector got " + joined(m));
        m.removeObject(NSString.of("V"));
        TestKit.check("Z,b,c".equals(joined(m)), "removeObject");
        m.removeObjectInRange(NSString.of("b"), new NSRange(0, 2));
        TestKit.check("Z,c".equals(joined(m)), "removeObjectInRange");
        m.removeObjectIdenticalTo(m.objectAt(0));
        TestKit.check("c".equals(joined(m)), "removeObjectIdenticalTo");
        m.addObjectsFromArray(strArray(L1, L1));
        TestKit.check(m.count() == 3, "identical-pair setup");
        m.removeObjectIdenticalToInRange(m.objectAt(1), new NSRange(0, 3));
        TestKit.check(m.count() == 2 && L1.equals(ObjC.toString(m.objectAtIndex(1))),
                "removeObjectIdenticalToInRange removes one peer, keeps equal peer");
        m.removeObjectsInArray(strArray(L1));
        TestKit.check("c".equals(joined(m)), "removeObjectsInArray");
        m.addObjectsFromArray(strArray("x", "y"));
        m.removeObjectsInRange(new NSRange(1, 2));
        TestKit.check("c".equals(joined(m)), "removeObjectsInRange");
        m.addObjectsFromArray(strArray("q"));
        m.removeObjectsAtIndexes(NSIndexSet.indexSetWithIndex(1));
        TestKit.check("c".equals(joined(m)), "removeObjectsAtIndexes");
        m.setArray(strArray("solo"));
        TestKit.check(m.count() == 1 && "solo".equals(ObjC.toString(m.objectAtIndex(0))), "setArray");
        m.removeLastObject();
        TestKit.check(m.count() == 0, "removeLastObject");
        m.addObjectsFromArray(strArray("a"));
        m.removeAllObjects();
        TestKit.check(m.count() == 0, "removeAllObjects");
    }

    // ---------------------------------------------------------- NSDictionary

    private static NSDictionary kvDict() {
        NSDictionary d = NSDictionary.mutableDictionary();
        d.setObjectForKey(NSString.of("v1"), NSString.of("k1"));
        d.setObjectForKey(NSString.of("v2"), NSString.of("k2"));
        return d;
    }

    private static void testNSDictionary() {
        System.out.println("\n-- NSDictionary --");
        TestKit.check(NSDictionary.wrap(null) == null, "NSDictionary.wrap null");
        NSDictionary one = NSDictionary.dictionaryWithObjectForKey(NSString.of("v"), NSString.of("k"));
        TestKit.check(one.count() == 1 && "v".equals(ObjC.toString(one.objectForKey("k"))), "dictionaryWithObjectForKey");
        NSDictionary d = kvDict();
        TestKit.check(d.count() == 2, "kvDict count 2");
        TestKit.check(dictionaryCopyCheck(d), "dictionaryWithDictionary");
        NSArray keys = d.allKeys();
        NSArray vals = strArray("v1", "v2");
        NSDictionary viaArrays = NSDictionary.dictionaryWithObjectsForKeys(vals, keys);
        TestKit.check(viaArrays.count() == 2, "dictionaryWithObjectsForKeys");
        TestKit.check(dictionaryWithCapacityCheck(), "dictionaryWithCapacity");
        TestKit.check(d.allKeysForObject(NSString.of("v1")).count() == 1, "allKeysForObject");
        TestKit.check(d.allValues().count() == 2, "allValues");
        TestKit.check(d.descriptionWithLocale(null) != null, "dict descriptionWithLocale");
        TestKit.check(d.isEqualToDictionary(kvDict()), "isEqualToDictionary true");
        TestKit.check(!d.isEqualToDictionary(NSDictionary.dictionary()), "isEqualToDictionary false");
        TestKit.check(!d.isEqualToDictionary(null), "isEqualToDictionary null false");
        NSArray got = d.objectsForKeysNotFoundMarker(keys, NSString.of("M"));
        TestKit.check(got.count() == 2, "objectsForKeysNotFoundMarker count");
        TestKit.check("v1".equals(ObjC.toString(d.objectForKeyedSubscript("k1"))), "objectForKeyedSubscript");
        TestKit.check(d.objectForKeyedSubscript("missing") == null, "keyedSubscript miss nil");
        TestKit.check(d.keyEnumerator() != null, "keyEnumerator");
        TestKit.check(d.objectEnumerator() != null, "objectEnumerator");
        TestKit.check("k1".equals(d.keysSortedByValueUsingSelector(ObjC.sel("compare:")).stringAt(0).string()),
                "keysSortedByValue");
        NSDictionary m = NSDictionary.mutableDictionary();
        m.addEntriesFromDictionary(d);
        TestKit.check(m.count() == 2, "addEntriesFromDictionary");
        NSArray rk = NSArray.mutableArray();
        rk.addObject(NSString.of("k1"));
        m.removeObjectsForKeys(rk);
        TestKit.check(m.count() == 1 && m.objectForKey("k1") == null, "removeObjectsForKeys");
        m.removeObjectForKey(NSString.of("k2"));
        TestKit.check(m.count() == 0, "removeObjectForKey NSObject");
        m.setDictionary(d);
        TestKit.check(m.count() == 2, "setDictionary");
        m.setObjectForKeyedSubscript(NSString.of("v3"), NSString.of("k3"));
        TestKit.check("v3".equals(ObjC.toString(m.objectForKey("k3"))), "setObjectForKeyedSubscript");
        m.setObjectForKeyedSubscript(null, NSString.of("k3"));
        TestKit.check(m.objectForKey("k3") == null, "keyedSubscript nil removes");
        m.removeAllObjects();
        TestKit.check(m.count() == 0, "dict removeAllObjects");
        // Shared key sets.
        NSArray sk = strArray("sk1", "sk2");
        MemorySegment ks = NSDictionary.sharedKeySetForKeys(sk);
        TestKit.check(ks != null, "sharedKeySetForKeys");
        NSDictionary sh = NSDictionary.dictionaryWithSharedKeySet(ks);
        sh.setObjectForKey(NSString.of("sv1"), NSString.of("sk1"));
        TestKit.check("sv1".equals(ObjC.toString(sh.objectForKey("sk1"))), "sharedKeySet get");
    }

    private static boolean dictionaryCopyCheck(NSDictionary d) {
        NSDictionary c = NSDictionary.dictionaryWithDictionary(d);
        return c.count() == 2 && "v2".equals(ObjC.toString(c.objectForKey("k2")));
    }

    private static boolean dictionaryWithCapacityCheck() {
        NSDictionary d = NSDictionary.dictionaryWithCapacity(16);
        return d != null && d.count() == 0;
    }

    // ---------------------------------------------------------- sets

    private static void testSets() {
        System.out.println("\n-- NSSet/NSMutableSet --");
        TestKit.check(NSSet.wrap(null) == null, "NSSet.wrap null");
        TestKit.check(NSMutableSet.wrap(null) == null, "NSMutableSet.wrap null");
        NSSet s = NSSet.setWithObjects(NSString.of(L1), NSString.of(L2));
        TestKit.check(s.count() == 2, "set count 2");
        TestKit.check(NSSet.setWithSet(s).isEqualToSet(s), "setWithSet equal");
        TestKit.check(NSSet.setWithSet(null).count() == 0, "setWithSet null empty");
        TestKit.check(s.setByAddingObject(NSString.of(L3)).count() == 3, "setByAddingObject");
        TestKit.check(s.setByAddingObjectsFromSet(NSSet.setWithObject(NSString.of(L3))).count() == 3,
                "setByAddingObjectsFromSet");
        TestKit.check(s.setByAddingObjectsFromArray(strArray(L3)).count() == 3, "setByAddingObjectsFromArray");
        TestKit.check(s.descriptionWithLocale(null) != null, "set descriptionWithLocale");
        TestKit.check(s.objectEnumerator() != null, "set objectEnumerator");
        TestKit.check(s.containsObject(NSString.of(L1)), "set contains value");
        TestKit.check(ObjC.toString(s.member(NSString.of(L1).peer())) != null, "set member");
        TestKit.check(s.anyObject() != null, "set anyObject");
        TestKit.check(s.allObjects().count() == 2, "set allObjects");
        TestKit.check(s.isEqualToSet(NSSet.setWithSet(s)), "isEqualToSet");
        TestKit.check(s.intersectsSet(NSSet.setWithObject(NSString.of(L1))), "intersectsSet");
        TestKit.check(NSSet.setWithObject(NSString.of(L1)).isSubsetOfSet(s), "isSubsetOfSet");
        s.makeObjectsPerformSelector(ObjC.sel("description"));
        s.makeObjectsPerformSelectorWithObject(ObjC.sel("isEqual:"), ObjC.nsstring(L1));
        TestKit.check(s.count() == 2, "set makeObjectsPerform no-throw");
        NSMutableSet ms = NSMutableSet.set();
        ms.addObjectsFromArray(strArray(L1, L2));
        TestKit.check(ms.count() == 2, "addObjectsFromArray");
        ms.addObject(NSString.of(L3));
        ms.removeObject(NSString.of(L3));
        TestKit.check(ms.count() == 2, "mutable add/remove");
        ms.unionSet(NSSet.setWithObject(NSString.of(L3)));
        ms.minusSet(NSSet.setWithObject(NSString.of(L3)));
        TestKit.check(ms.count() == 2, "union/minus");
        ms.intersectSet(s);
        TestKit.check(ms.count() == 2, "intersectSet");
        ms.setSet(NSSet.setWithObject(NSString.of(L1)));
        TestKit.check(ms.count() == 1, "setSet");
        ms.removeAllObjects();
        TestKit.check(ms.count() == 0, "mutable removeAll");
        TestKit.check(NSMutableSet.setWithCapacity(8).count() == 0, "setWithCapacity");
    }

    // ---------------------------------------------------------- index sets

    private static void testIndexSets() {
        System.out.println("\n-- NSIndexSet/NSMutableIndexSet --");
        TestKit.check(NSIndexSet.wrap(null) == null, "NSIndexSet.wrap null");
        TestKit.check(NSMutableIndexSet.wrap(null) == null, "NSMutableIndexSet.wrap null");
        NSIndexSet r = NSIndexSet.indexSetWithIndexesInRange(new NSRange(0, 4));
        TestKit.check(r.count() == 4, "range set count 4");
        TestKit.check(r.indexGreaterThanIndex(0) == 1, "indexGreaterThanIndex");
        TestKit.check(r.indexGreaterThanIndex(3) == NSRange.NOT_FOUND, "greater past end NOT_FOUND");
        TestKit.check(r.indexLessThanIndex(3) == 2, "indexLessThanIndex");
        TestKit.check(r.indexLessThanIndex(0) == NSRange.NOT_FOUND, "less before start NOT_FOUND");
        TestKit.check(r.indexGreaterThanOrEqualToIndex(2) == 2, "greaterOrEqual");
        TestKit.check(r.indexLessThanOrEqualToIndex(2) == 2, "lessOrEqual");
        TestKit.check(r.countOfIndexesInRange(new NSRange(1, 10)) == 3, "countOfIndexesInRange");
        TestKit.check(r.containsIndexes(NSIndexSet.indexSetWithIndex(2)), "containsIndexes");
        TestKit.check(!r.containsIndexes(NSIndexSet.indexSetWithIndex(9)), "containsIndexes false");
        TestKit.check(r.intersectsIndexesInRange(new NSRange(3, 10)), "intersectsIndexesInRange true");
        TestKit.check(!r.intersectsIndexesInRange(new NSRange(10, 5)), "intersectsIndexesInRange false");
        TestKit.check(NSIndexSet.indexSet().count() == 0, "indexSet empty");
        TestKit.check(NSIndexSet.indexSetWithIndex(7).containsIndex(7), "indexSetWithIndex");
        TestKit.check(r.firstIndex() == 0 && r.lastIndex() == 3, "first/lastIndex");
        TestKit.check(r.isEqualToIndexSet(NSIndexSet.indexSetWithIndexesInRange(new NSRange(0, 4))), "isEqualToIndexSet");
        NSMutableIndexSet m = NSMutableIndexSet.indexSet();
        m.addIndex(5);
        m.addIndexesInRange(new NSRange(10, 3));
        TestKit.check(m.count() == 4 && m.containsIndex(12), "addIndex/addIndexesInRange");
        m.addIndexes(NSIndexSet.indexSetWithIndex(20));
        TestKit.check(m.containsIndex(20), "addIndexes");
        m.removeIndexes(NSIndexSet.indexSetWithIndex(20));
        TestKit.check(!m.containsIndex(20), "removeIndexes");
        m.removeIndex(5);
        m.removeIndexesInRange(new NSRange(10, 3));
        TestKit.check(m.count() == 0, "removeIndex/removeIndexesInRange");
        m.addIndex(5);
        m.shiftIndexesStartingAtIndex(3, 2);
        TestKit.check(!m.containsIndex(5) && m.containsIndex(7) && m.count() == 1, "shiftIndexes");
        m.removeAllIndexes();
        TestKit.check(m.count() == 0, "removeAllIndexes");
        TestKit.check(NSMutableIndexSet.indexSetWithIndex(3).containsIndex(3), "mutable indexSetWithIndex");
    }

    // ---------------------------------------------------------- ordered sets

    private static NSMutableOrderedSet ostr(String... items) {
        NSMutableOrderedSet o = NSMutableOrderedSet.orderedSet();
        for (String s : items) o.addObject(NSString.of(s));
        return o;
    }

    private static String ojoin(NSOrderedSet o) {
        StringBuilder b = new StringBuilder();
        for (long i = 0; i < o.count(); i++) b.append(ObjC.toString(o.objectAtIndex(i))).append(i + 1 < o.count() ? "," : "");
        return b.toString();
    }

    private static void testOrderedSets() {
        System.out.println("\n-- NSOrderedSet/NSMutableOrderedSet --");
        TestKit.check(NSOrderedSet.wrap(null) == null, "NSOrderedSet.wrap null");
        TestKit.check(NSMutableOrderedSet.wrap(null) == null, "NSMutableOrderedSet.wrap null");
        NSOrderedSet o = ostr("a", "b", "c");
        TestKit.check(o.count() == 3, "ordered count 3");
        TestKit.check(NSOrderedSet.orderedSetWithOrderedSet(o).isEqualToOrderedSet(o), "orderedSetWithOrderedSet");
        TestKit.check(NSOrderedSet.orderedSetWithSet(NSSet.setWithObject(NSString.of("k"))).count() == 1,
                "orderedSetWithSet");
        TestKit.check(NSOrderedSet.orderedSetWithSetCopyItems(NSSet.setWithObject(NSString.of("k")), false).count() == 1,
                "orderedSetWithSetCopyItems");
        TestKit.check(NSOrderedSet.orderedSetWithObject(NSString.of("s")).count() == 1, "orderedSetWithObject");
        TestKit.check(NSOrderedSet.orderedSetWithArray(strArray("1", "2")).count() == 2, "orderedSetWithArray");
        NSArray atIdx = o.objectsAtIndexes(NSIndexSet.indexSetWithIndexesInRange(new NSRange(0, 2)));
        TestKit.check(atIdx.count() == 2 && "a".equals(ObjC.toString(atIdx.objectAtIndex(0))), "ordered objectsAtIndexes");
        TestKit.check(o.isEqualToOrderedSet(NSOrderedSet.orderedSetWithOrderedSet(o)), "isEqualToOrderedSet");
        TestKit.check(!o.isEqualToOrderedSet(null), "isEqualToOrderedSet null false");
        TestKit.check(o.intersectsOrderedSet(NSOrderedSet.orderedSetWithObject(NSString.of("b"))), "intersectsOrderedSet");
        TestKit.check(o.intersectsSet(NSSet.setWithObject(NSString.of("b"))), "ordered intersectsSet");
        TestKit.check(NSOrderedSet.orderedSetWithObject(NSString.of("a")).isSubsetOfOrderedSet(o), "isSubsetOfOrderedSet");
        TestKit.check(NSOrderedSet.orderedSetWithObject(NSString.of("a")).isSubsetOfSet(o.set()), "isSubsetOfSet");
        TestKit.check(o.objectEnumerator() != null, "ordered objectEnumerator");
        TestKit.check(o.reverseObjectEnumerator() != null, "ordered reverseObjectEnumerator");
        TestKit.check("c".equals(ObjC.toString(o.reversedOrderedSet().objectAtIndex(0))), "reversedOrderedSet");
        TestKit.check(o.set() != null && o.set().count() == 3, "ordered set facade");
        TestKit.check(o.descriptionWithLocale(null) != null, "ordered descriptionWithLocale");
        TestKit.check("b".equals(ObjC.toString(o.objectAtIndexedSubscript(1))), "ordered subscript");
        TestKit.check(o.indexOfObject(NSString.of("c")) == 2, "ordered indexOfObject");
        TestKit.check(o.containsObject(NSString.of("a")), "ordered contains");
        TestKit.check("a".equals(ObjC.toString(o.firstObject())), "ordered firstObject");
        TestKit.check("c".equals(ObjC.toString(o.lastObject())), "ordered lastObject");
        TestKit.check(o.array().count() == 3, "ordered array");
        // Mutable exercises.
        NSMutableOrderedSet m = ostr("a", "b", "c");
        m.replaceObjectAtIndex(0, NSString.of("A"));
        TestKit.check("A,b,c".equals(ojoin(m)), "replaceObjectAtIndex got " + ojoin(m));
        m.exchangeObjectAtIndex(0, 2);
        TestKit.check("c,b,A".equals(ojoin(m)), "exchange got " + ojoin(m));
        m.moveObjectsAtIndexesToIndex(NSIndexSet.indexSetWithIndex(2), 0);
        TestKit.check("A,c,b".equals(ojoin(m)), "move got " + ojoin(m));
        m.insertObjectAtIndex(NSString.of("X"), 1);
        m.insertObjectsAtIndexes(strArray("Y"), NSIndexSet.indexSetWithIndex(0));
        TestKit.check("Y,A,X,c,b".equals(ojoin(m)), "inserts got " + ojoin(m));
        m.setObjectAtIndex(NSString.of("Q"), 0);
        m.setObjectAtIndexedSubscript(NSString.of("W"), 1);
        TestKit.check("Q,W,X,c,b".equals(ojoin(m)), "setters got " + ojoin(m));
        m.replaceObjectsAtIndexes(NSIndexSet.indexSetWithIndex(0), strArray("q"));
        TestKit.check("q,W,X,c,b".equals(ojoin(m)), "replaceAtIndexes");
        m.removeObjectsInRange(new NSRange(0, 1));
        m.removeObjectsAtIndexes(NSIndexSet.indexSetWithIndex(0));
        TestKit.check("X,c,b".equals(ojoin(m)), "removes got " + ojoin(m));
        m.removeObjectsInArray(strArray("X"));
        TestKit.check("c,b".equals(ojoin(m)), "removeObjectsInArray");
        m.removeObjectAtIndex(0);
        TestKit.check("b".equals(ojoin(m)), "removeObjectAtIndex");
        m.removeObject(NSString.of("b"));
        TestKit.check(m.count() == 0, "ordered removeObject");
        NSMutableOrderedSet u = ostr("a", "b");
        u.intersectOrderedSet(NSOrderedSet.orderedSetWithObject(NSString.of("b")));
        TestKit.check("b".equals(ojoin(u)), "intersectOrderedSet");
        u.unionOrderedSet(NSOrderedSet.orderedSetWithObject(NSString.of("c")));
        u.minusOrderedSet(NSOrderedSet.orderedSetWithObject(NSString.of("b")));
        TestKit.check("c".equals(ojoin(u)), "union/minusOrderedSet got " + ojoin(u));
        u.unionSet(NSSet.setWithObject(NSString.of("d")));
        u.minusSet(NSSet.setWithObject(NSString.of("c")));
        TestKit.check("d".equals(ojoin(u)), "union/minusSet got " + ojoin(u));
        u.intersectSet(NSSet.setWithObjects(NSString.of("d"), NSString.of("e")));
        TestKit.check("d".equals(ojoin(u)), "intersectSet");
        u.addObjectsFromArray(strArray("f"));
        TestKit.check("d,f".equals(ojoin(u)), "addObjectsFromArray");
        u.removeAllObjects();
        TestKit.check(u.count() == 0, "ordered removeAll");
        TestKit.check(NSMutableOrderedSet.orderedSetWithCapacity(8).count() == 0, "orderedSetWithCapacity");
    }

    // ---------------------------------------------------------- numbers

    private static void testNumbers() {
        System.out.println("\n-- NSNumber --");
        TestKit.check(NSNumber.wrap(null) == null, "NSNumber.wrap null");
        TestKit.check(NSNumber.numberWithChar(65).charValue() == 65, "char");
        TestKit.check(NSNumber.numberWithUnsignedChar(200).unsignedCharValue() == 200, "unsignedChar");
        TestKit.check(NSNumber.numberWithShort(1000).shortValue() == 1000, "short");
        TestKit.check(NSNumber.numberWithUnsignedShort(60000).unsignedShortValue() == 60000, "unsignedShort");
        TestKit.check(NSNumber.numberWithInt(42).intValue() == 42, "int");
        TestKit.check(NSNumber.numberWithUnsignedInt(3000000000L).unsignedIntValue() == 3000000000L, "unsignedInt");
        TestKit.check(NSNumber.numberWithLong(-7).longValue() == -7, "long");
        TestKit.check(NSNumber.numberWithUnsignedLong(7).unsignedLongValue() == 7, "unsignedLong");
        TestKit.check(NSNumber.numberWithLongLong(-9).longLongValue() == -9, "longLong");
        TestKit.check(NSNumber.numberWithUnsignedLongLong(-1).unsignedLongLongValue() == -1, "unsignedLongLong bits");
        TestKit.check(NSNumber.numberWithInteger(11).integerValue() == 11, "integer");
        TestKit.check(NSNumber.numberWithUnsignedInteger(11).unsignedIntegerValue() == 11, "unsignedInteger");
        TestKit.check(NSNumber.numberWithLongLong(Long.MAX_VALUE).longLongValue() == Long.MAX_VALUE, "longLong MAX_VALUE");
        TestKit.check("c".equals(NSNumber.numberWithChar(65).objCType()), "char objCType c");
        TestKit.check(NSNumber.numberWithShort(300).shortValue() == 300, "short 300");
        TestKit.check(NSNumber.numberWithShort(-1).shortValue() == -1, "short sign-extends");
        TestKit.check(NSNumber.numberWithUnsignedShort(60000).unsignedShortValue() == 60000, "unsigned short high value");
        TestKit.check(NSNumber.numberWithInt(-1).intValue() == -1, "int sign-extends");
        TestKit.check(NSNumber.numberWithUnsignedInt(4294967295L).unsignedIntValue() == 4294967295L, "unsigned int high value");
        CALayer maskLayer = CALayer.create();
        maskLayer.setEdgeAntialiasingMask(7);
        TestKit.check(maskLayer.edgeAntialiasingMask() == 7, "edgeAntialiasingMask round-trip");
        maskLayer.setEdgeAntialiasingMask(0xFL);
        TestKit.check(maskLayer.edgeAntialiasingMask() == 0xFL, "edgeAntialiasingMask all valid bits round-trip");
        // Out-of-domain bits never survive: AppKit keeps only the 4 edge bits.
        maskLayer.setEdgeAntialiasingMask(0xFFFFFFFFL);
        TestKit.check(maskLayer.edgeAntialiasingMask() == 0xFL, "edgeAntialiasingMask clamps to valid bits");
        TestKit.check(Math.abs(NSNumber.numberWithDouble(3.14).doubleValue() - 3.14) < 1e-12, "double");
        TestKit.check(NSNumber.numberWithFloat(1.5f).floatValue() == 1.5f, "float fallback exact");
        TestKit.check("d".equals(NSNumber.numberWithFloat(1.5f).objCType()), "float fallback objCType d (documented)");
        TestKit.check(NSNumber.numberWithBool(true).boolValue(), "bool true");
        TestKit.check(!NSNumber.numberWithBool(false).boolValue(), "bool false");
        TestKit.check(NSNumber.numberWithInt(2).compare(NSNumber.numberWithInt(3)) < 0, "number compare <");
        TestKit.check(NSNumber.numberWithInt(3).compare(NSNumber.numberWithInt(3)) == 0, "number compare ==");
        TestKit.check(NSNumber.numberWithInt(4).compare(NSNumber.numberWithInt(3)) > 0, "number compare >");
        TestKit.check(NSNumber.numberWithInt(5).isEqualToNumber(NSNumber.numberWithInt(5)), "isEqualToNumber");
        TestKit.check(!NSNumber.numberWithInt(5).isEqualToNumber(NSNumber.numberWithInt(6)), "isEqualToNumber false");
        TestKit.check(!NSNumber.numberWithInt(5).isEqualToNumber(null), "isEqualToNumber null false");
        TestKit.check("42".equals(NSNumber.numberWithInt(42).stringValue().string()), "stringValue");
        TestKit.check(NSNumber.numberWithInt(1).descriptionWithLocale(null) != null, "number descriptionWithLocale");
        TestKit.check("5".equals(ObjC.toString(NSNumber.numberWithInt(5).describe().peer())), "number describe");
    }

    // ---------------------------------------------------------- dates

    private static void testDates() {
        System.out.println("\n-- NSDate --");
        TestKit.check(NSDate.wrap(null) == null, "NSDate.wrap null");
        TestKit.check(NSDate.date() != null, "date now");
        TestKit.check(NSDate.date().timeIntervalSince1970() > 0, "timeIntervalSince1970 >0");
        TestKit.check(NSDate.dateWithTimeIntervalSince1970(0).timeIntervalSince1970() == 0.0, "epoch round-trip");
        double soon = NSDate.dateWithTimeIntervalSinceNow(60).timeIntervalSinceNow();
        TestKit.check(soon > 50 && soon < 70, "sinceNow ~60 got " + soon);
        TestKit.check(NSDate.distantPast().compare(NSDate.distantFuture()) < 0, "past<future");
        TestKit.check(NSDate.dateWithTimeIntervalSinceReferenceDate(100).timeIntervalSinceReferenceDate() == 100.0,
                "reference round-trip");
        NSDate base = NSDate.date();
        TestKit.check(NSDate.dateWithTimeIntervalSinceDate(10, base).timeIntervalSinceDate(base) == 10.0,
                "sinceDate +10");
        TestKit.check(base.earlierDate(NSDate.distantPast()).isEqualToDate(NSDate.distantPast()), "earlierDate");
        TestKit.check(base.laterDate(NSDate.distantPast()).isEqualToDate(base), "laterDate");
        TestKit.check(NSDate.date().dateByAddingTimeInterval(3600).timeIntervalSinceDate(NSDate.date()) > 3500,
                "dateByAddingTimeInterval");
        NSDate same = NSDate.date();
        TestKit.check(same.isEqualToDate(same), "isEqualToDate self");
        TestKit.check(NSDate.dateWithTimeIntervalSince1970(5).isEqualToDate(NSDate.dateWithTimeIntervalSince1970(5)),
                "isEqualToDate equal instants");
        TestKit.check(!NSDate.date().isEqualToDate(null), "isEqualToDate null false");
        TestKit.check(NSDate.date().descriptionWithLocale(null) != null, "date descriptionWithLocale");
        TestKit.check(NSDate.now() != null, "now");
        TestKit.check(NSDate.systemTimeIntervalSinceReferenceDate() > 0, "system reference >0");
    }

    // ---------------------------------------------------------- data

    private static void testData() throws Exception {
        System.out.println("\n-- NSData/NSMutableData (+unique /tmp IO) --");
        TestKit.check(NSData.wrap(null) == null, "NSData.wrap null");
        TestKit.check(NSMutableData.wrap(null) == null, "NSMutableData.wrap null");
        TestKit.check(NSData.data().length() == 0, "data empty");
        TestKit.check(NSData.data().toByteArray().length == 0, "empty toByteArray");
        TestKit.check(NSData.dataWithBytes(null).length() == 0, "dataWithBytes null empty");
        byte[] hello = "hello".getBytes(StandardCharsets.UTF_8);
        TestKit.check(java.util.Arrays.equals(NSData.dataWithBytes(hello).toByteArray(), hello), "dataWithBytes round-trip");
        TestKit.check(NSData.dataWithBytes(new byte[]{0, 1, 2, 3}, 2).length() == 2, "dataWithBytes length slice");
        TestKit.check(NSData.dataWithBytes(new byte[]{7}).toByteArray()[0] == 7, "single byte");
        TestKit.check(NSData.dataWithData(NSData.dataWithBytes(new byte[]{7})).toByteArray()[0] == 7, "dataWithData");
        TestKit.check(NSData.dataWithData(null).length() == 0, "dataWithData null empty");
        NSData d5 = NSData.dataWithBytes(new byte[]{0, 1, 2, 3, 4});
        TestKit.check(d5.subdataWithRange(new NSRange(1, 3)).length() == 3, "subdata length");
        TestKit.check(java.util.Arrays.equals(d5.subdataWithRange(new NSRange(1, 3)).toByteArray(), new byte[]{1, 2, 3}),
                "subdata bytes");
        TestKit.check(d5.isEqualToData(NSData.dataWithBytes(new byte[]{0, 1, 2, 3, 4})), "isEqualToData true");
        TestKit.check(!d5.isEqualToData(NSData.data()), "isEqualToData false");
        TestKit.check(!d5.isEqualToData(null), "isEqualToData null false");
        TestKit.check("aGVsbG8=".equals(NSData.dataWithBytes(hello).base64EncodedStringWithOptions(0).string()),
                "base64 string");
        TestKit.check("aGVsbG8=".equals(new String(
                NSData.dataWithBytes(hello).base64EncodedDataWithOptions(0).toByteArray(), StandardCharsets.UTF_8)),
                "base64 data");
        // File IO under a unique /tmp dir (never inside the repo).
        Path dir = Files.createTempDirectory("sa-foundation-");
        try {
            Path f = dir.resolve("t.bin");
            TestKit.check(NSData.dataWithBytes(new byte[]{5, 6, 7}).writeToFile(f.toString(), true), "writeToFile");
            TestKit.check(Files.size(f) == 3, "file size 3");
            TestKit.check(!NSData.dataWithBytes(new byte[]{1}).writeToFile(null, true), "writeToFile null false");
            TestKit.check(!NSData.dataWithBytes(new byte[]{1}).writeToFile("", true), "writeToFile empty false");
            NSData back = NSData.dataWithContentsOfFile(f.toString());
            TestKit.check(back != null && back.length() == 3 && back.toByteArray()[2] == 7, "dataWithContentsOfFile");
            TestKit.check(NSData.dataWithContentsOfFile(dir.resolve("missing").toString()) == null,
                    "contentsOfFile missing nil");
            TestKit.check(NSData.dataWithContentsOfFile(null) == null, "contentsOfFile null nil");
            MemorySegment url = ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("fileURLWithPath:"), ObjC.nsstring(f.toString()));
            NSData uback = NSData.dataWithContentsOfURL(url);
            TestKit.check(uback != null && uback.length() == 3, "dataWithContentsOfURL");
            TestKit.check(NSData.dataWithContentsOfURL(null) == null, "contentsOfURL null nil");
            Path f2 = dir.resolve("u.bin");
            MemorySegment url2 = ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("fileURLWithPath:"), ObjC.nsstring(f2.toString()));
            TestKit.check(NSData.dataWithBytes(new byte[]{1, 2, 3, 4}).writeToURL(url2, true)
                    && Files.size(f2) == 4, "writeToURL");
            TestKit.check(!NSData.dataWithBytes(new byte[]{1}).writeToURL(null, true), "writeToURL null false");
        } finally {
            try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignore) { }
                });
            }
        }
        // NSMutableData.
        NSMutableData md = NSMutableData.data();
        TestKit.check(md.length() == 0, "mutable data empty");
        md.appendBytes(new byte[]{1, 2, 3});
        TestKit.check(md.length() == 3, "appendBytes");
        md.appendBytes(null);
        md.appendBytes(new byte[0]);
        TestKit.check(md.length() == 3, "append null/empty no-op");
        md.appendData(NSData.dataWithBytes(new byte[]{4}));
        TestKit.check(md.length() == 4, "appendData");
        md.appendData(null);
        TestKit.check(md.length() == 4, "appendData null no-op");
        md.increaseLengthBy(2);
        TestKit.check(md.length() == 6, "increaseLengthBy");
        md.setLength(2);
        TestKit.check(md.length() == 2, "setLength");
        TestKit.check(NSMutableData.dataWithCapacity(64).length() == 0, "dataWithCapacity");
        TestKit.check(NSMutableData.dataWithLength(5).length() == 5, "dataWithLength");
        TestKit.check(md.mutableBytes() != null, "mutableBytes");
        NSMutableData rep = NSMutableData.dataWithLength(4);
        rep.replaceBytesInRange(new NSRange(0, 4), new byte[]{9, 8, 7, 6});
        TestKit.check(java.util.Arrays.equals(rep.toByteArray(), new byte[]{9, 8, 7, 6}), "replaceBytesInRange");
        rep.setData(NSData.dataWithBytes(new byte[]{1, 1}));
        TestKit.check(rep.length() == 2 && rep.toByteArray()[0] == 1, "setData");
        rep.setData(null);
        TestKit.check(rep.length() == 0, "setData null clears");
        NSMutableData z = NSMutableData.dataWithLength(4);
        z.resetBytesInRange(new NSRange(0, 4));
        TestKit.check(java.util.Arrays.equals(z.toByteArray(), new byte[4]), "resetBytesInRange zeroes");
    }

    // ---------------------------------------------------------- values

    private static void testValues() {
        System.out.println("\n-- NSValue --");
        TestKit.check(NSValue.wrap(null) == null, "NSValue.wrap null");
        TestKit.check(NSNumber.wrap(NSValue.valueWithPoint(new NSPoint(1, 2)).peer()) != null, "value peer live");
        TestKit.check(NSValue.valueWithPoint(new NSPoint(1, 2)).pointValue().equals(new NSPoint(1, 2)), "point round-trip");
        TestKit.check(NSValue.valueWithSize(new NSSize(3, 4)).sizeValue().equals(new NSSize(3, 4)), "size round-trip");
        TestKit.check(NSValue.valueWithRect(new NSRect(1, 2, 3, 4)).rectValue().equals(new NSRect(1, 2, 3, 4)),
                "rect round-trip");
        TestKit.check(NSValue.valueWithRange(new NSRange(5, 6)).rangeValue().equals(new NSRange(5, 6)), "range round-trip");
        TestKit.check(NSValue.valueWithRange(new NSRange(1, 2)).isEqualToValue(NSValue.valueWithRange(new NSRange(1, 2))),
                "isEqualToValue true");
        TestKit.check(!NSValue.valueWithRange(new NSRange(1, 2)).isEqualToValue(NSValue.valueWithRange(new NSRange(1, 3))),
                "isEqualToValue false");
        TestKit.check(!NSValue.valueWithRange(new NSRange(1, 2)).isEqualToValue(null), "isEqualToValue null false");
        NSString req = NSString.of("req-long-nonretained-object-001");
        NSValue nr = NSValue.valueWithNonretainedObject(req);
        TestKit.check(nr != null && nr.nonretainedObjectValue() != null
                && nr.nonretainedObjectValue().address() == req.peer().address(), "nonretained round-trip");
        MemorySegment ptr = ObjC.nsstring("ptr");
        NSValue pv = NSValue.valueWithPointer(ptr);
        TestKit.check(pv != null && pv.pointerValue() != null && pv.pointerValue().address() == ptr.address(),
                "pointer round-trip");
        TestKit.check(NSValue.valueWithRange(new NSRange(0, 0)).objCType() != null
                && NSValue.valueWithRange(new NSRange(0, 0)).objCType().contains("Range"), "objCType mentions Range");
    }

    // ---------------------------------------------------------- records

    private static void testRecords() {
        System.out.println("\n-- NSRange/NSPoint/NSSize/NSRect converters --");
        NSRange r = new NSRange(5, 10);
        TestKit.check(NSRange.fromNSString(r.toNSString()).equals(r), "range toNSString round-trip");
        TestKit.check(new NSRange(5, 10).equals(NSRange.fromString("{5, 10}")), "range Apple format");
        TestKit.check(NSRange.fromString("nope") == null, "range bad null");
        TestKit.check(NSRange.fromString(null) == null, "range null null");
        TestKit.check(NSRange.fromNSString(null) == null, "range NSString null null");
        NSPoint p = new NSPoint(1.5, -2.25);
        TestKit.check(NSPoint.fromNSString(p.toNSString()).equals(p), "point round-trip");
        TestKit.check(new NSPoint(1, 2).equals(NSPoint.fromString("{1, 2}")), "point Apple format");
        TestKit.check(NSPoint.fromString("x") == null, "point bad null");
        NSSize s = new NSSize(3.0, 4.5);
        TestKit.check(NSSize.fromNSString(s.toNSString()).equals(s), "size round-trip");
        TestKit.check(new NSSize(3, 4).equals(NSSize.fromString("{3, 4}")), "size Apple format");
        TestKit.check(NSSize.fromNSString(null) == null, "size NSString null null");
        NSRect rc = new NSRect(1, 2, 30, 40);
        TestKit.check(NSRect.fromNSString(rc.toNSString()).equals(rc), "rect round-trip");
        TestKit.check(new NSRect(1, 2, 3, 4).equals(NSRect.fromString("{{1, 2}, {3, 4}}")), "rect Apple format");
        TestKit.check(NSRect.fromString("{{1, 2}}") == null, "rect bad null");
        TestKit.check(NSRect.fromNSString(null) == null, "rect NSString null null");
        NSRect[] d1 = new NSRect(0, 0, 100, 50).divide(20, NSRect.EDGE_MIN_X);
        TestKit.check(d1[0].equals(new NSRect(0, 0, 20, 50)) && d1[1].equals(new NSRect(20, 0, 80, 50)), "divide MinX");
        NSRect[] d2 = new NSRect(0, 0, 100, 50).divide(20, NSRect.EDGE_MAX_X);
        TestKit.check(d2[0].equals(new NSRect(80, 0, 20, 50)) && d2[1].equals(new NSRect(0, 0, 80, 50)), "divide MaxX");
        NSRect[] d3 = new NSRect(0, 0, 100, 50).divide(10, NSRect.EDGE_MIN_Y);
        TestKit.check(d3[0].equals(new NSRect(0, 0, 100, 10)) && d3[1].equals(new NSRect(0, 10, 100, 40)), "divide MinY");
        NSRect[] d4 = new NSRect(0, 0, 100, 50).divide(10, NSRect.EDGE_MAX_Y);
        TestKit.check(d4[0].equals(new NSRect(0, 40, 100, 10)) && d4[1].equals(new NSRect(0, 0, 100, 40)), "divide MaxY");
        TestKit.check(new NSRect(0.5, 0.5, 10, 10).integralWithOptions(0)
                .equals(new NSRect(0.5, 0.5, 10, 10).integral()), "integralWithOptions==integral");
        TestKit.check(NSEdgeInsets.ZERO.isZero(), "insets ZERO (owned file smoke)");
    }

    // ---------------------------------------------------------- narrow 32-bit readers
    // Oracles for the exact-width returns found by the reader audit: a bare
    // 64-bit read of a 32-bit (`i`/`I`) return keeps garbage upper bits, so
    // negative values (signed) and high-bit values (unsigned) come back wrong.
    // Each oracle below uses such a value and is green on the INT32 handles.
    private static void testNarrowReaders() {
        System.out.println("\n-- narrow (32-bit) readers --");
        // MTLStencilDescriptor masks (uint32_t): high-bit round-trips.
        if (ObjC.cls("MTLStencilDescriptor").address() == 0) {
            try { ObjC.ensureFramework("Metal"); } catch (Throwable ignored) { }
        }
        if (ObjC.cls("MTLStencilDescriptor").address() == 0) {
            TestKit.skipCase("Metal unavailable: stencil/clearStencil oracles");
        } else {
            MTLStencilDescriptor stencil = MTLStencilDescriptor.create();
            stencil.setReadMask(0x80000001L);
            TestKit.check(stencil.readMask() == 0x80000001L, "stencil readMask high-bit round-trip");
            stencil.setReadMask(0xFFFFFFFFL);
            TestKit.check(stencil.readMask() == 0xFFFFFFFFL, "stencil readMask all-bits round-trip");
            stencil.setWriteMask(0x80000001L);
            TestKit.check(stencil.writeMask() == 0x80000001L, "stencil writeMask high-bit round-trip");
            stencil.setWriteMask(0L);
            TestKit.check(stencil.writeMask() == 0L, "stencil writeMask zero");
            // clearStencil (uint32_t) via a render-pass descriptor (no device needed).
            MTLRenderPassDescriptor pass = MTLRenderPassDescriptor.create();
            pass.stencilAttachment().setClearStencil(0x80000001L);
            TestKit.check(pass.stencilAttachment().clearStencil() == 0x80000001L,
                    "clearStencil high-bit round-trip");
            pass.stencilAttachment().setClearStencil(0xFFFFFFFFL);
            TestKit.check(pass.stencilAttachment().clearStencil() == 0xFFFFFFFFL,
                    "clearStencil all-bits round-trip");
        }
        // NSControl intValue (C int): negative round-trips on a detached text
        // field. (A button coerces intValue to its 0/1 state, so a text field —
        // whose cell parses the displayed integer — is the faithful vehicle.)
        NSTextField f = NSTextField.create(new NSRect(0, 0, 120, 32));
        f.setIntValue(-1);
        TestKit.check(f.intValue() == -1, "control intValue -1");
        f.setIntValue(Integer.MIN_VALUE);
        TestKit.check(f.intValue() == Integer.MIN_VALUE, "control intValue MIN_VALUE");
        f.setIntValue(Integer.MAX_VALUE);
        TestKit.check(f.intValue() == Integer.MAX_VALUE, "control intValue MAX_VALUE");
        // processIdentifier (pid_t, int): in a bare test JVM AppKit reports -1,
        // and the old 64-bit read returned 4294967295 for it — every check
        // below trips that read while passing on the exact INT32 handle.
        long pid = NSRunningApplication.current().processIdentifier();
        TestKit.check(pid == (long) (int) pid, "processIdentifier fits 32 bits");
        if (pid == -1) {
            TestKit.check(true, "processIdentifier reports -1 pre-app (exact signed read)");
        } else {
            TestKit.check(pid == ProcessHandle.current().pid(), "processIdentifier matches Java pid");
        }
        // CAConstraintAttribute (int): creation round-trip.
        CAConstraint c = CAConstraint.relativeTo(
                CAConstraint.ATTR_MAX_X, "super", CAConstraint.ATTR_WIDTH, 1.0, 0.0);
        TestKit.check(c.attribute() == CAConstraint.ATTR_MAX_X, "constraint attribute round-trip");
        TestKit.check(c.sourceAttribute() == CAConstraint.ATTR_WIDTH, "constraint sourceAttribute round-trip");
        // NSWindowDepth (int32_t): class default needs no window.
        long ddl = NSWindow.defaultDepthLimit();
        TestKit.check(ddl == (long) (int) ddl, "defaultDepthLimit fits 32 bits");
        TestKit.check(ddl == NSWindow.defaultDepthLimit(), "defaultDepthLimit stable");
        // Instance depthLimit via one hidden window (never shown).
        NSWindow win = TestKit.hiddenWindow(100, 100);
        try {
            long d0 = win.depthLimit();
            TestKit.check(d0 == (long) (int) d0, "depthLimit fits 32 bits");
            win.setDepthLimit(d0);
            TestKit.check(win.depthLimit() == d0, "depthLimit self round-trip");
        } finally {
            TestKit.close(win);
        }
        // Screen depth (NSWindowDepth) + display id (uint32_t): guarded headless.
        NSScreen s = NSScreen.mainScreen();
        if (s == null) {
            TestKit.skipCase("no main screen (headless): depth/CGDirectDisplayID");
        } else {
            long depth = s.depth();
            TestKit.check(depth == (long) (int) depth, "screen depth fits 32 bits");
            TestKit.check(depth == s.depth(), "screen depth stable");
            long did = s.cgDirectDisplayID();
            TestKit.check(did != 0, "CGDirectDisplayID non-zero");
            TestKit.check(did == (did & 0xFFFFFFFFL), "CGDirectDisplayID fits 32 bits");
        }
    }

    // ---------------------------------------------------------- negative guards
    // Java-side pre-validation: bad indexes/ranges throw IllegalArgumentException
    // instead of aborting the JVM via native raise. One per guarded family,
    // using clearly-bad values (index == count, range past end, short out-buffer).
    private static void testNegativeGuards() {
        System.out.println("\n-- negative guards (IAE, no native abort) --");
        NSArray two = strArray("a", "b");
        TestKit.expectThrows("NSArray objectAtIndex(count) rejected", IllegalArgumentException.class,
                () -> two.objectAtIndex(2));
        TestKit.expectThrows("NSArray objectAtIndexedSubscript(count) rejected", IllegalArgumentException.class,
                () -> two.objectAtIndexedSubscript(2));
        NSOrderedSet oset = NSOrderedSet.orderedSetWithArray(strArray("a", "b"));
        TestKit.expectThrows("NSOrderedSet objectAtIndex(count) rejected", IllegalArgumentException.class,
                () -> oset.objectAtIndex(2));
        TestKit.expectThrows("NSOrderedSet objectAtIndexedSubscript(count) rejected", IllegalArgumentException.class,
                () -> oset.objectAtIndexedSubscript(2));
        NSString hello = NSString.of("hello");
        TestKit.expectThrows("NSString substringWithRange past end rejected", IllegalArgumentException.class,
                () -> hello.substringWithRange(new NSRange(3, 3)));
        TestKit.expectThrows("NSString characterAtIndex(length) rejected", IllegalArgumentException.class,
                () -> hello.characterAtIndex(5));
        TestKit.expectThrows("NSString substringFromIndex past end rejected", IllegalArgumentException.class,
                () -> hello.substringFromIndex(6));
        TestKit.expectThrows("NSString substringToIndex past end rejected", IllegalArgumentException.class,
                () -> hello.substringToIndex(6));
        TestKit.expectThrows("NSString stringByReplacingCharactersInRange past end rejected",
                IllegalArgumentException.class,
                () -> hello.stringByReplacingCharactersInRange(new NSRange(4, 2), "x"));
        NSData d5 = NSData.dataWithBytes(new byte[]{0, 1, 2, 3, 4});
        TestKit.expectThrows("NSData subdataWithRange past end rejected", IllegalArgumentException.class,
                () -> d5.subdataWithRange(new NSRange(4, 2)));
        NSAttributedString attr = NSAttributedString.create("Hello");
        MemorySegment shortOut = java.lang.foreign.Arena.global().allocate(8);
        TestKit.expectThrows("NSAttributedString attribute short out-buffer rejected",
                IllegalArgumentException.class,
                () -> attr.attribute("NSForegroundColorAttributeName", 0, shortOut));
        TestKit.expectThrows("NSAttributedString attributesAtIndex short out-buffer rejected",
                IllegalArgumentException.class,
                () -> attr.attributesAtIndexEffectiveRange(0, shortOut));
        NSMutableData md = NSMutableData.dataWithLength(4);
        TestKit.expectThrows("NSMutableData replaceBytes short bytes rejected", IllegalArgumentException.class,
                () -> md.replaceBytesInRange(new NSRange(0, 2), new byte[]{1}));
        NSMutableAttributedString mut = NSMutableAttributedString.create("Hello");
        TestKit.expectThrows("NSMutableAttributedString replaceCharacters past end rejected",
                IllegalArgumentException.class,
                () -> mut.replaceCharactersInRangeWithString(new NSRange(0, 99), "x"));
        TestKit.expectThrows("NSMutableAttributedString insert past end rejected", IllegalArgumentException.class,
                () -> mut.insertAttributedString(NSAttributedString.create("x"), 99));
        TestKit.expectThrows("NSMutableAttributedString delete past end rejected", IllegalArgumentException.class,
                () -> mut.deleteCharactersInRange(new NSRange(0, 99)));
        // NSDictionary dictionaryWithObjects:forKeys:count: has no wrapper in-file
        // (document-only, no guard) — no negative test. MTL colorAttachment
        // negatives live in MetalDepthStencilTest (owning suite).
    }
}
