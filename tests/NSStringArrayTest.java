package nsui.tests;

import java.lang.foreign.MemorySegment;
import nsui.NSArray;
import nsui.NSDictionary;
import nsui.NSRange;
import nsui.NSString;
import nsui.objc.Autorelease;
import nsui.objc.ObjC;

/**
 * Tests for NSString / NSArray / NSDictionary wrappers.
 * Pure-memory (Foundation-only) — no NSWindow — but still requires ObjC.init().
 * Includes verification of the toString truncation fix (strings >4096 chars).
 */
public final class NSStringArrayTest {

    

    

    public static void main(String[] args) {
        System.out.println("=== NSStringArrayTest — NSString / NSArray / NSDictionary ===");
        ObjC.init();

        // ---- NSString.of / length / isEqual ----
        System.out.println("\n-- NSString.of / length / isEqual --");
        TestKit.check(NSString.of(null) == null, "NSString.of(null) == null");
        NSString empty = NSString.of("");
        TestKit.check(empty != null, "NSString.of(\"\") non-null");
        TestKit.check(empty.length() == 0, "empty length ==0");
        TestKit.check("".equals(empty.string()), "empty string() == \"\"");
        TestKit.check(empty.toString().equals(""), "empty toString == \"\"");

        NSString hello = NSString.of("hello");
        TestKit.check(hello != null, "NSString.of(\"hello\") non-null");
        TestKit.check(hello.length() == 5, "hello length==5 got " + hello.length());
        TestKit.check("hello".equals(hello.string()), "hello string()==\"hello\" got \"" + hello.string() + "\"");
        TestKit.check("hello".equals(hello.toString()), "hello toString()==\"hello\"");

        NSString hello2 = NSString.of("hello");
        TestKit.check(hello.isEqual(hello2), "isEqual true for same content");
        TestKit.check(hello.isEqualToString("hello"), "isEqualToString true");
        TestKit.check(!hello.isEqual(NSString.of("world")), "isEqual false for different");
        TestKit.check(!hello.isEqual(null), "isEqual false for null");
        TestKit.check(!hello.isEqualToString(null), "isEqualToString false for null");
        TestKit.check(hello.isEqualToString("hello"), "isEqualToString overload true");
        TestKit.check(!hello.isEqualToString("Hello"), "isEqualToString case-sensitive false");

        // length with unicode: NS length is UTF-16 code units; é =1, emoji =2
        NSString unicode = NSString.of("héllo");
        TestKit.check(unicode.length() == 5, "unicode héllo length 5 got " + unicode.length());

        // wrap null
        TestKit.check(NSString.wrap(null) == null, "NSString.wrap(null)==null");
        TestKit.check(NSString.wrap(MemorySegment.NULL) == null, "NSString.wrap(NULL)==null");

        // ---- toString truncation fix verification ----
        System.out.println("\n-- toString truncation fix (>4096) --");
        // The old implementation truncated at 4096; new impl does strlen loop. Verify 5k,10k,20k.
        int[] sizes = {4095, 4096, 4097, 5000, 10000, 20000};
        for (int sz : sizes) {
            StringBuilder sb = new StringBuilder(sz);
            for (int i = 0; i < sz; i++) sb.append((char) ('a' + (i % 26)));
            String original = sb.toString();
            // via NSString.of + ObjC.toString + NSString.string() + wrap
            MemorySegment seg = ObjC.nsstring(original);
            String viaObjC = ObjC.toString(seg);
            TestKit.check(viaObjC != null && viaObjC.length() == sz,
                    "ObjC.toString length " + sz + " got " + (viaObjC == null ? "null" : viaObjC.length()));
            TestKit.check(original.equals(viaObjC), "ObjC.toString content matches for size " + sz);

            NSString ns = NSString.of(original);
            TestKit.check(ns.length() == sz, "NSString length " + sz + " got " + ns.length());
            String viaNSString = ns.string();
            TestKit.check(original.equals(viaNSString), "NSString.string() matches for size " + sz);
            TestKit.check(original.equals(ns.toString()), "NSString.toString matches for size " + sz);
        }
        // All 'x' 10k — easier to debug if mismatch
        String tenK = "x".repeat(10_000);
        NSString nsTenK = NSString.of(tenK);
        TestKit.check(nsTenK.length() == 10_000, "repeat 10k length");
        TestKit.check(tenK.equals(ObjC.toString(nsTenK.peer())), "repeat 10k ObjC.toString matches");
        // Also verify via direct peer round-trip
        MemorySegment tenKSeg = ObjC.nsstring(tenK);
        TestKit.check(tenK.equals(ObjC.toString(tenKSeg)), "direct nsstring 10k round-trip");

        // substringWithRange / rangeOfString (uses RANGE vocab)
        try {
            NSString hw = NSString.of("hello world");
            NSRange rng = hw.rangeOfString("world");
            TestKit.check(rng.location() == 6 && rng.length() == 5, "rangeOfString \"world\" in \"hello world\" == {6,5} got " + rng);
            NSRange notFound = hw.rangeOfString("xyz");
            TestKit.check(notFound.location() == NSRange.NOT_FOUND, "rangeOfString missing -> NOT_FOUND");
            NSString sub = hw.substringWithRange(new NSRange(0, 5));
            TestKit.check("hello".equals(sub.string()), "substringWithRange {0,5} == hello got " + sub.string());
        } catch (Throwable t) {
            TestKit.check(false, "substring/rangeOfString threw: " + t);
            t.printStackTrace();
        }

        // ---- NSArray: count / objectAt / stringAt / lastObject / mutable ----
        System.out.println("\n-- NSArray --");
        NSArray arr = NSArray.mutableArray();
        TestKit.check(arr != null, "mutableArray non-null");
        TestKit.check(arr.isEmpty(), "new mutableArray isEmpty");
        TestKit.check(arr.count() == 0, "new mutableArray count 0");

        NSString a = NSString.of("a");
        NSString b = NSString.of("b");
        NSString c = NSString.of("c");
        arr.addObject(a);
        TestKit.check(arr.count() == 1, "after add a count 1");
        TestKit.check(!arr.isEmpty(), "not empty after add");
        arr.addObject(b);
        arr.addObject(c);
        TestKit.check(arr.count() == 3, "after add b,c count 3");

        // objectAt / objectAtIndex
        MemorySegment at0 = arr.objectAtIndex(0);
        TestKit.check(at0 != null && at0.address() == a.peer().address(), "objectAtIndex 0 == a");
        MemorySegment at1 = arr.objectAtIndex(1);
        TestKit.check(at1 != null && at1.address() == b.peer().address(), "objectAtIndex 1 == b");
        NSObjectWrapCheck:
        {
            // typed accessors
            TestKit.check(arr.objectAt(0) != null, "objectAt(0) non-null");
            TestKit.check(arr.stringAt(1) != null && "b".equals(arr.stringAt(1).string()), "stringAt(1)==b");
        }

        // lastObject
        MemorySegment last = arr.lastObject();
        TestKit.check(last != null && last.address() == c.peer().address(), "lastObject == c");

        // toList
        java.util.List<MemorySegment> list = arr.toList();
        TestKit.check(list.size() == 3, "toList size 3");
        TestKit.check(list.get(0).address() == a.peer().address(), "toList[0]==a");

        // NSArray.wrap null
        TestKit.check(NSArray.wrap(null) == null, "NSArray.wrap(null)==null");

        // immutable array
        NSArray imm = NSArray.array();
        TestKit.check(imm != null && imm.count() == 0, "NSArray.array() empty immutable");

        // add via raw MemorySegment
        NSArray arr2 = NSArray.mutableArray();
        MemorySegment raw = ObjC.nsstring("raw");
        arr2.addObject(raw);
        TestKit.check(arr2.count() == 1, "addObject raw segment count 1");
        TestKit.check("raw".equals(ObjC.toString(arr2.objectAtIndex(0))), "raw segment round-trip");

        // Stress: 1k adds
        NSArray stressArr = NSArray.mutableArray();
        for (int i = 0; i < 1000; i++) stressArr.addObject(NSString.of("item-" + i));
        TestKit.check(stressArr.count() == 1000, "stress 1000 adds count 1000");
        TestKit.check("item-0".equals(stressArr.stringAt(0).string()), "stress first item");
        TestKit.check("item-999".equals(stressArr.stringAt(999).string()), "stress last item");

        // ---- NSDictionary: set/get/allKeys ----
        System.out.println("\n-- NSDictionary --");
        NSDictionary dict = NSDictionary.mutableDictionary();
        TestKit.check(dict != null, "mutableDictionary non-null");
        TestKit.check(dict.isEmpty(), "new dict isEmpty");
        TestKit.check(dict.count() == 0, "new dict count 0");

        NSString key1 = NSString.of("key1");
        NSString val1 = NSString.of("value1");
        dict.setObjectForKey(val1, key1);
        TestKit.check(dict.count() == 1, "after set key1 count 1");
        MemorySegment got1 = dict.objectForKey(key1);
        TestKit.check(got1 != null && "value1".equals(ObjC.toString(got1)), "objectForKey(key1)==value1");

        // objectForKey(String)
        MemorySegment got1s = dict.objectForKey("key1");
        TestKit.check(got1s != null && "value1".equals(ObjC.toString(got1s)), "objectForKey(\"key1\")");

        // set second key
        dict.setObjectForKey(NSString.of("value2"), NSString.of("key2"));
        TestKit.check(dict.count() == 2, "after set key2 count 2");

        // overwrite
        dict.setObjectForKey(NSString.of("newValue1"), key1);
        TestKit.check(dict.count() == 2, "overwrite does not grow count");
        TestKit.check("newValue1".equals(ObjC.toString(dict.objectForKey(key1))), "overwrite value updated");

        // allKeys
        NSArray keys = dict.allKeys();
        TestKit.check(keys != null && keys.count() == 2, "allKeys count 2 got " + (keys == null ? "null" : keys.count()));
        // keys contain key1 and key2 (order not guaranteed)
        boolean hasKey1 = false, hasKey2 = false;
        for (long i = 0; i < keys.count(); i++) {
            String k = ObjC.toString(keys.objectAtIndex(i));
            if ("key1".equals(k)) hasKey1 = true;
            if ("key2".equals(k)) hasKey2 = true;
        }
        TestKit.check(hasKey1 && hasKey2, "allKeys contains key1 and key2");

        // remove
        dict.removeObjectForKey(key1.peer());
        TestKit.check(dict.count() == 1, "after remove key1 count 1");
        TestKit.check(dict.objectForKey(key1) == null, "objectForKey removed == null");
        TestKit.check("value2".equals(ObjC.toString(dict.objectForKey("key2"))), "remaining key2 value2");

        // setObjectForKey with MemorySegments
        NSDictionary dict2 = NSDictionary.mutableDictionary();
        MemorySegment k = ObjC.nsstring("k");
        MemorySegment v = ObjC.nsstring("v");
        dict2.setObjectForKey(v, k);
        TestKit.check("v".equals(ObjC.toString(dict2.objectForKey(k))), "setObjectForKey(MemorySegment) round-trip");
        TestKit.check("v".equals(ObjC.toString(dict2.objectForKey("k"))), "objectForKey(String) after raw set");

        // wrap null
        TestKit.check(NSDictionary.wrap(null) == null, "NSDictionary.wrap(null)==null");

        // isKindOfClass (inherited from NSObject)
        TestKit.check(dict.isKindOfClass("NSDictionary"), "dict isKindOfClass NSDictionary");
        TestKit.check(arr.isKindOfClass("NSArray"), "arr isKindOfClass NSArray");

        // immutable
        NSDictionary immDict = NSDictionary.dictionary();
        TestKit.check(immDict != null && immDict.count() == 0, "NSDictionary.dictionary() empty");

        // Stress dict
        NSDictionary stressDict = NSDictionary.mutableDictionary();
        for (int i = 0; i < 1000; i++) {
            stressDict.setObjectForKey(ObjC.nsstring("v" + i), ObjC.nsstring("k" + i));
        }
        TestKit.check(stressDict.count() == 1000, "stress dict 1000 entries count 1000");
        TestKit.check("v999".equals(ObjC.toString(stressDict.objectForKey("k999"))), "stress dict last lookup");

        // Autorelease sanity: run inside pool
        Autorelease.run(() -> {
            NSString tmp = NSString.of("inside pool");
            TestKit.check("inside pool".equals(tmp.string()), "inside Autorelease.run string ok");
        });

        // ---- additional edge cases (FullCoverage expansion) ----
        System.out.println("\n-- additional edge cases (FullCoverage) --");
        // empty string edge
        TestKit.check(NSString.of("") != null && "".equals(NSString.of("").string()), "empty string edge");
        // unicode emoji (2 code units)
        NSString emoji = NSString.of("a\uD83D\uDE00b");
        TestKit.check(emoji.length()==4, "emoji length 4 code units got "+emoji.length());
        // very large string 20000 chars (beyond 4096 truncation boundary)
        String huge = "z".repeat(20000);
        TestKit.check(huge.equals(NSString.of(huge).string()), "20k string round-trip");
        TestKit.check(huge.equals(ObjC.toString(ObjC.nsstring(huge))), "20k ObjC round-trip");
        // NSString wrap null
        TestKit.check(NSString.wrap(MemorySegment.NULL)==null, "wrap NULL null");
        // NSArray edge: out-of-bounds should not crash? we test count
        NSArray emptyArr = NSArray.array();
        TestKit.check(emptyArr.count()==0 && emptyArr.isEmpty(), "immutable empty");
        // NSDictionary edge: missing key returns null
        NSDictionary emptyDict = NSDictionary.dictionary();
        TestKit.check(emptyDict.objectForKey("missing")==null, "missing key null");
        TestKit.check(emptyDict.objectForKey(MemorySegment.NULL)==null, "missing MemorySegment key null");
        // NSArray containsObject with null
        NSArray arr3 = NSArray.mutableArray(); arr3.addObject(NSString.of("x"));
        TestKit.check(!arr3.containsObject(MemorySegment.NULL), "contains NULL false");
        // NSRange via NSString: substring edge at bounds
        NSString hw = NSString.of("hello world");
        NSRange full = new NSRange(0, hw.length());
        TestKit.check(hw.substringWithRange(full).string().equals("hello world"), "substring full length");
        TestKit.check(hw.substringWithRange(new NSRange(0,0)).string().equals(""), "substring empty");

        System.out.println("\n=== NSStringArrayTest " + (TestKit.failures() == 0 ? "PASS" : "FAIL — " + TestKit.failures() + " failed") + " ===");
        TestKit.end();
    }
}
