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

    private static int failures;

    private static void check(boolean ok, String msg) {
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== NSStringArrayTest — NSString / NSArray / NSDictionary ===");
        ObjC.init();

        // ---- NSString.of / length / isEqual ----
        System.out.println("\n-- NSString.of / length / isEqual --");
        check(NSString.of(null) == null, "NSString.of(null) == null");
        NSString empty = NSString.of("");
        check(empty != null, "NSString.of(\"\") non-null");
        check(empty.length() == 0, "empty length ==0");
        check("".equals(empty.string()), "empty string() == \"\"");
        check(empty.toString().equals(""), "empty toString == \"\"");

        NSString hello = NSString.of("hello");
        check(hello != null, "NSString.of(\"hello\") non-null");
        check(hello.length() == 5, "hello length==5 got " + hello.length());
        check("hello".equals(hello.string()), "hello string()==\"hello\" got \"" + hello.string() + "\"");
        check("hello".equals(hello.toString()), "hello toString()==\"hello\"");

        NSString hello2 = NSString.of("hello");
        check(hello.isEqual(hello2), "isEqual true for same content");
        check(hello.isEqualToString("hello"), "isEqualToString true");
        check(!hello.isEqual(NSString.of("world")), "isEqual false for different");
        check(!hello.isEqual(null), "isEqual false for null");
        check(!hello.isEqualToString(null), "isEqualToString false for null");
        check(hello.isEqualToString("hello"), "isEqualToString overload true");
        check(!hello.isEqualToString("Hello"), "isEqualToString case-sensitive false");

        // length with unicode: NS length is UTF-16 code units; é =1, emoji =2
        NSString unicode = NSString.of("héllo");
        check(unicode.length() == 5, "unicode héllo length 5 got " + unicode.length());

        // wrap null
        check(NSString.wrap(null) == null, "NSString.wrap(null)==null");
        check(NSString.wrap(MemorySegment.NULL) == null, "NSString.wrap(NULL)==null");

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
            check(viaObjC != null && viaObjC.length() == sz,
                    "ObjC.toString length " + sz + " got " + (viaObjC == null ? "null" : viaObjC.length()));
            check(original.equals(viaObjC), "ObjC.toString content matches for size " + sz);

            NSString ns = NSString.of(original);
            check(ns.length() == sz, "NSString length " + sz + " got " + ns.length());
            String viaNSString = ns.string();
            check(original.equals(viaNSString), "NSString.string() matches for size " + sz);
            check(original.equals(ns.toString()), "NSString.toString matches for size " + sz);
        }
        // All 'x' 10k — easier to debug if mismatch
        String tenK = "x".repeat(10_000);
        NSString nsTenK = NSString.of(tenK);
        check(nsTenK.length() == 10_000, "repeat 10k length");
        check(tenK.equals(ObjC.toString(nsTenK.peer())), "repeat 10k ObjC.toString matches");
        // Also verify via direct peer round-trip
        MemorySegment tenKSeg = ObjC.nsstring(tenK);
        check(tenK.equals(ObjC.toString(tenKSeg)), "direct nsstring 10k round-trip");

        // substringWithRange / rangeOfString (uses RANGE vocab)
        try {
            NSString hw = NSString.of("hello world");
            NSRange rng = hw.rangeOfString("world");
            check(rng.location() == 6 && rng.length() == 5, "rangeOfString \"world\" in \"hello world\" == {6,5} got " + rng);
            NSRange notFound = hw.rangeOfString("xyz");
            check(notFound.location() == NSRange.NOT_FOUND, "rangeOfString missing -> NOT_FOUND");
            NSString sub = hw.substringWithRange(new NSRange(0, 5));
            check("hello".equals(sub.string()), "substringWithRange {0,5} == hello got " + sub.string());
        } catch (Throwable t) {
            check(false, "substring/rangeOfString threw: " + t);
            t.printStackTrace();
        }

        // ---- NSArray: count / objectAt / stringAt / lastObject / mutable ----
        System.out.println("\n-- NSArray --");
        NSArray arr = NSArray.mutableArray();
        check(arr != null, "mutableArray non-null");
        check(arr.isEmpty(), "new mutableArray isEmpty");
        check(arr.count() == 0, "new mutableArray count 0");

        NSString a = NSString.of("a");
        NSString b = NSString.of("b");
        NSString c = NSString.of("c");
        arr.addObject(a);
        check(arr.count() == 1, "after add a count 1");
        check(!arr.isEmpty(), "not empty after add");
        arr.addObject(b);
        arr.addObject(c);
        check(arr.count() == 3, "after add b,c count 3");

        // objectAt / objectAtIndex
        MemorySegment at0 = arr.objectAtIndex(0);
        check(at0 != null && at0.address() == a.peer().address(), "objectAtIndex 0 == a");
        MemorySegment at1 = arr.objectAtIndex(1);
        check(at1 != null && at1.address() == b.peer().address(), "objectAtIndex 1 == b");
        NSObjectWrapCheck:
        {
            // typed accessors
            check(arr.objectAt(0) != null, "objectAt(0) non-null");
            check(arr.stringAt(1) != null && "b".equals(arr.stringAt(1).string()), "stringAt(1)==b");
        }

        // lastObject
        MemorySegment last = arr.lastObject();
        check(last != null && last.address() == c.peer().address(), "lastObject == c");

        // toList
        java.util.List<MemorySegment> list = arr.toList();
        check(list.size() == 3, "toList size 3");
        check(list.get(0).address() == a.peer().address(), "toList[0]==a");

        // NSArray.wrap null
        check(NSArray.wrap(null) == null, "NSArray.wrap(null)==null");

        // immutable array
        NSArray imm = NSArray.array();
        check(imm != null && imm.count() == 0, "NSArray.array() empty immutable");

        // add via raw MemorySegment
        NSArray arr2 = NSArray.mutableArray();
        MemorySegment raw = ObjC.nsstring("raw");
        arr2.addObject(raw);
        check(arr2.count() == 1, "addObject raw segment count 1");
        check("raw".equals(ObjC.toString(arr2.objectAtIndex(0))), "raw segment round-trip");

        // Stress: 1k adds
        NSArray stressArr = NSArray.mutableArray();
        for (int i = 0; i < 1000; i++) stressArr.addObject(NSString.of("item-" + i));
        check(stressArr.count() == 1000, "stress 1000 adds count 1000");
        check("item-0".equals(stressArr.stringAt(0).string()), "stress first item");
        check("item-999".equals(stressArr.stringAt(999).string()), "stress last item");

        // ---- NSDictionary: set/get/allKeys ----
        System.out.println("\n-- NSDictionary --");
        NSDictionary dict = NSDictionary.mutableDictionary();
        check(dict != null, "mutableDictionary non-null");
        check(dict.isEmpty(), "new dict isEmpty");
        check(dict.count() == 0, "new dict count 0");

        NSString key1 = NSString.of("key1");
        NSString val1 = NSString.of("value1");
        dict.setObjectForKey(val1, key1);
        check(dict.count() == 1, "after set key1 count 1");
        MemorySegment got1 = dict.objectForKey(key1);
        check(got1 != null && "value1".equals(ObjC.toString(got1)), "objectForKey(key1)==value1");

        // objectForKey(String)
        MemorySegment got1s = dict.objectForKey("key1");
        check(got1s != null && "value1".equals(ObjC.toString(got1s)), "objectForKey(\"key1\")");

        // set second key
        dict.setObjectForKey(NSString.of("value2"), NSString.of("key2"));
        check(dict.count() == 2, "after set key2 count 2");

        // overwrite
        dict.setObjectForKey(NSString.of("newValue1"), key1);
        check(dict.count() == 2, "overwrite does not grow count");
        check("newValue1".equals(ObjC.toString(dict.objectForKey(key1))), "overwrite value updated");

        // allKeys
        NSArray keys = dict.allKeys();
        check(keys != null && keys.count() == 2, "allKeys count 2 got " + (keys == null ? "null" : keys.count()));
        // keys contain key1 and key2 (order not guaranteed)
        boolean hasKey1 = false, hasKey2 = false;
        for (long i = 0; i < keys.count(); i++) {
            String k = ObjC.toString(keys.objectAtIndex(i));
            if ("key1".equals(k)) hasKey1 = true;
            if ("key2".equals(k)) hasKey2 = true;
        }
        check(hasKey1 && hasKey2, "allKeys contains key1 and key2");

        // remove
        dict.removeObjectForKey(key1.peer());
        check(dict.count() == 1, "after remove key1 count 1");
        check(dict.objectForKey(key1) == null, "objectForKey removed == null");
        check("value2".equals(ObjC.toString(dict.objectForKey("key2"))), "remaining key2 value2");

        // setObjectForKey with MemorySegments
        NSDictionary dict2 = NSDictionary.mutableDictionary();
        MemorySegment k = ObjC.nsstring("k");
        MemorySegment v = ObjC.nsstring("v");
        dict2.setObjectForKey(v, k);
        check("v".equals(ObjC.toString(dict2.objectForKey(k))), "setObjectForKey(MemorySegment) round-trip");
        check("v".equals(ObjC.toString(dict2.objectForKey("k"))), "objectForKey(String) after raw set");

        // wrap null
        check(NSDictionary.wrap(null) == null, "NSDictionary.wrap(null)==null");

        // isKindOfClass (inherited from NSObject)
        check(dict.isKindOfClass("NSDictionary"), "dict isKindOfClass NSDictionary");
        check(arr.isKindOfClass("NSArray"), "arr isKindOfClass NSArray");

        // immutable
        NSDictionary immDict = NSDictionary.dictionary();
        check(immDict != null && immDict.count() == 0, "NSDictionary.dictionary() empty");

        // Stress dict
        NSDictionary stressDict = NSDictionary.mutableDictionary();
        for (int i = 0; i < 1000; i++) {
            stressDict.setObjectForKey(ObjC.nsstring("v" + i), ObjC.nsstring("k" + i));
        }
        check(stressDict.count() == 1000, "stress dict 1000 entries count 1000");
        check("v999".equals(ObjC.toString(stressDict.objectForKey("k999"))), "stress dict last lookup");

        // Autorelease sanity: run inside pool
        Autorelease.run(() -> {
            NSString tmp = NSString.of("inside pool");
            check("inside pool".equals(tmp.string()), "inside Autorelease.run string ok");
        });

        // ---- additional edge cases (FullCoverage expansion) ----
        System.out.println("\n-- additional edge cases (FullCoverage) --");
        // empty string edge
        check(NSString.of("") != null && "".equals(NSString.of("").string()), "empty string edge");
        // unicode emoji (2 code units)
        NSString emoji = NSString.of("a\uD83D\uDE00b");
        check(emoji.length()==4, "emoji length 4 code units got "+emoji.length());
        // very large string 20000 chars (beyond 4096 truncation boundary)
        String huge = "z".repeat(20000);
        check(huge.equals(NSString.of(huge).string()), "20k string round-trip");
        check(huge.equals(ObjC.toString(ObjC.nsstring(huge))), "20k ObjC round-trip");
        // NSString wrap null
        check(NSString.wrap(MemorySegment.NULL)==null, "wrap NULL null");
        // NSArray edge: out-of-bounds should not crash? we test count
        NSArray emptyArr = NSArray.array();
        check(emptyArr.count()==0 && emptyArr.isEmpty(), "immutable empty");
        // NSDictionary edge: missing key returns null
        NSDictionary emptyDict = NSDictionary.dictionary();
        check(emptyDict.objectForKey("missing")==null, "missing key null");
        check(emptyDict.objectForKey(MemorySegment.NULL)==null, "missing MemorySegment key null");
        // NSArray containsObject with null
        NSArray arr3 = NSArray.mutableArray(); arr3.addObject(NSString.of("x"));
        check(!arr3.containsObject(MemorySegment.NULL), "contains NULL false");
        // NSRange via NSString: substring edge at bounds
        NSString hw = NSString.of("hello world");
        NSRange full = new NSRange(0, hw.length());
        check(hw.substringWithRange(full).string().equals("hello world"), "substring full length");
        check(hw.substringWithRange(new NSRange(0,0)).string().equals(""), "substring empty");

        System.out.println("\n=== NSStringArrayTest " + (failures == 0 ? "PASS" : "FAIL — " + failures + " failed") + " ===");
        System.exit(failures == 0 ? 0 : 1);
    }
}
