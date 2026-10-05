package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSString — typed wrapper over a native `NSString` (id).
/// Thin, stateless: every method maps to one `objc_msgSend`.
///
/// Header-completeness (`NSString.h`): every safe method whose shape is in the Sig
/// vocabulary is wrapped below. OMITTED — compare:options:(:range::locale:) (needs
/// of(INT,ID,INT), not in Sig); rangeOfString:options:range:(:locale:) (needs
/// of(RANGE,ID,INT,RANGE), not in Sig); rangeOfCharacterFromSet:options:range: (same);
/// rangeOfComposedCharacterSequenceAtIndex: (needs of(RANGE,INT), not in Sig);
/// commonPrefixWithString:options: and descriptionWithLocale:indent: (need of(ID,ID,INT),
/// not in Sig); stringByPaddingToLength:withString:startingAtIndex: (needs
/// of(ID,INT,ID,INT), not in Sig); stringByReplacingOccurrencesOfString:...options:range:
/// (needs of(ID,ID,ID,INT,RANGE), not in Sig); initWithData:encoding: (needs
/// of(ID,ID,INT), not in Sig — bridge via toByteArray + stringWithBytes:length:encoding:);
/// +string (name clashes with instance string(); covered by of("")); other init* (covered by
/// of()/stringWithString:/stringWithUTF8Bytes, except initWithBytes:length:encoding:/
/// initWithData:encoding:/stringWithCString:encoding: (need of(ID,ID,INT,INT)/of(ID,ID,INT),
/// not in Sig) and coder/file/bytesNoCopy variants (need NSCoder/NSURL/ownership)); file/error
/// APIs (NSError**
/// out-params); getBytes:/getCharacters:/getCString:maxLength:encoding:/getLineStart:.../
/// getParagraphStart:... (out-pointer/buffer plumbing); cString/lossyCString/getCString* (deprecated);
/// writeToFile:/writeToURL:/contentsOfFile:/contentsOfURL: (deprecated or NSError**);
/// enumerateSubstringsInRange:.../enumerateLinesUsingBlock: and stringByAppendingFormat:/
/// initWithFormat:... (blocks / variadics); stringEncodingForData:... (option dictionaries +
/// out-params); propertyList/propertyListFromStringsFileFormat (discouraged); all of
/// NSMutableString.h (no NSMutableString wrapper in this batch: replaceCharactersInRange:...,
/// insertString:, deleteCharactersInRange:, appendString:, setString:, stringWithCapacity:, etc.).
public final class NSString extends NSObject {

    /// NSStringCompareOptions bit masks (combine with |).
    public static final long CASE_INSENSITIVE_SEARCH = 1;
    public static final long LITERAL_SEARCH = 2;
    public static final long BACKWARDS_SEARCH = 4;
    public static final long ANCHORED_SEARCH = 8;
    public static final long NUMERIC_SEARCH = 64;
    public static final long DIACRITIC_INSENSITIVE_SEARCH = 128;
    public static final long WIDTH_INSENSITIVE_SEARCH = 256;
    public static final long FORCED_ORDERING_SEARCH = 512;
    public static final long REGULAR_EXPRESSION_SEARCH = 1024;

    /// NSStringEncoding values (common subset).
    public static final long ASCII_STRING_ENCODING = 1;
    public static final long UTF8_STRING_ENCODING = 4;
    public static final long ISOLATIN1_STRING_ENCODING = 5;
    public static final long UNICODE_STRING_ENCODING = 10;
    public static final long UTF32_STRING_ENCODING = 0x8c000100L;

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hLength, MethodHandle hIsEqual, MethodHandle hUTF8String) {}
    private static volatile Handles handles;

    private NSString(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap a native NSString id (null for nil).
    public static NSString wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSString(peer);
    }

    /// Create an NSString from a Java string via `+stringWithUTF8String:`.
    public static NSString of(String javaString) {
        if (javaString == null) return null;
        return wrap(ObjC.nsstring(javaString));
    }

    /// Alias for `of` — explicit nsstring name.
    public static NSString stringWithUTF8String(String s) {
        return of(s);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        // hash is INT return, no args
        // UTF8String is ID return? Actually returns const char* (PTR) but we don't use handle for it; ObjC.toString handles directly.
        handles = new Handles(ObjC.handle(Sig.of(Ret.INT)), ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)), null);
    }

    /// Java String contents via `UTF8String` (uses ObjC.toString).
    public String string() {
        return ObjC.toString(peer);
    }

    /// length — number of UTF-16 code units (NSUInteger).
    public long length() {
        ensureInit();
        try {
            return (long) handles.hLength().invokeExact(peer, ObjC.sel("length"));
        } catch (Throwable t) {
            throw new RuntimeException("NSString length failed", t);
        }
    }

    /// isEqualToString: — equality with another NSString.
    public boolean isEqual(NSString other) {
        ensureInit();
        if (other == null) return false;
        try {
            return (boolean) handles.hIsEqual().invokeExact(peer, ObjC.sel("isEqualToString:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("isEqualToString: failed", t);
        }
    }

    /// isEqualToString: — convenience overload with Java String (creates temporary NSString).
    public boolean isEqualToString(String javaString) {
        if (javaString == null) return false;
        NSString other = of(javaString);
        return isEqual(other);
    }

    /// isEqual: — generic ObjC equality (id).
    public boolean isEqualTo(NSObject other) {
        ensureInit();
        if (other == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("isEqual:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("isEqual: failed", t);
        }
    }

    /// substringWithRange: — returns a new NSString for the given range (requires RANGE vocab).
    public NSString substringWithRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("substringWithRange:"), range.toSegment());
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("substringWithRange: failed", t);
        }
    }

    /// rangeOfString: — location of substring or NOT_FOUND.
    public NSRange rangeOfString(String substring) {
        ensureInit();
        if (substring == null) return new NSRange(NSRange.NOT_FOUND, 0);
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID));
            MemorySegment seg = (MemorySegment) h.invokeExact(
                    ObjC.structSlot(),
                    peer, ObjC.sel("rangeOfString:"), ObjC.nsstring(substring));
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("rangeOfString: failed", t);
        }
    }

    /// +stringWithString: — copy from another NSString.
    public static NSString stringWithString(NSString other) {
        ensureInit();
        if (other == null) return null;
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSString"), ObjC.sel("stringWithString:"), other.peer()));
    }

    /// stringWithUTF8Bytes — decode bytes via +stringWithUTF8String: (nil for invalid UTF-8).
    /// The NSData→NSString bridge is `stringWithUTF8Bytes(data.toByteArray())`.
    /// Embedded NULs truncate (C-string semantics). Non-UTF8 byte decoding
    /// (initWithBytes:length:encoding:, initWithData:encoding:, stringWithCString:encoding:)
    /// needs of(ID,ID,INT,INT)/of(ID,ID,INT), not in Sig — see class docs.
    public static NSString stringWithUTF8Bytes(byte[] bytes) {
        ensureInit();
        if (bytes == null) return null;
        try {
            MemorySegment buf = nsui.objc.Scratch.allocInput(bytes.length + 1);
            if (bytes.length > 0) {
                MemorySegment.copy(bytes, 0, buf, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, bytes.length);
            }
            buf.set(java.lang.foreign.ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
            return wrap(ObjC.msgSendIdId(ObjC.cls("NSString"), ObjC.sel("stringWithUTF8String:"), buf));
        } catch (Throwable t) {
            throw new RuntimeException("stringWithUTF8String: failed", t);
        }
    }

    /// characterAtIndex: — UTF-16 code unit at index (long, as unichar is unsigned short).
    public long characterAtIndex(long index) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("characterAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("characterAtIndex: failed", t);
        }
    }

    /// substringFromIndex: — tail from index to the end.
    public NSString substringFromIndex(long index) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            return wrap((MemorySegment) h.invokeExact(peer, ObjC.sel("substringFromIndex:"), index));
        } catch (Throwable t) {
            throw new RuntimeException("substringFromIndex: failed", t);
        }
    }

    /// substringToIndex: — head up to (excluding) index.
    public NSString substringToIndex(long index) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            return wrap((MemorySegment) h.invokeExact(peer, ObjC.sel("substringToIndex:"), index));
        } catch (Throwable t) {
            throw new RuntimeException("substringToIndex: failed", t);
        }
    }

    /// compare: — NSComparisonResult (-1 ascending, 0 same, 1 descending).
    public long compare(NSString other) {
        ensureInit();
        if (other == null) throw new IllegalArgumentException("compare: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("compare:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("compare: failed", t);
        }
    }

    /// compare: with a Java string.
    public long compare(String other) {
        if (other == null) throw new IllegalArgumentException("compare: null");
        return compare(of(other));
    }

    /// caseInsensitiveCompare:.
    public long caseInsensitiveCompare(NSString other) {
        ensureInit();
        if (other == null) throw new IllegalArgumentException("caseInsensitiveCompare: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("caseInsensitiveCompare:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("caseInsensitiveCompare: failed", t);
        }
    }

    /// caseInsensitiveCompare: with a Java string.
    public long caseInsensitiveCompare(String other) {
        if (other == null) throw new IllegalArgumentException("caseInsensitiveCompare: null");
        return caseInsensitiveCompare(of(other));
    }

    /// localizedCompare:.
    public long localizedCompare(NSString other) {
        ensureInit();
        if (other == null) throw new IllegalArgumentException("localizedCompare: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("localizedCompare:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("localizedCompare: failed", t);
        }
    }

    /// localizedCaseInsensitiveCompare:.
    public long localizedCaseInsensitiveCompare(NSString other) {
        ensureInit();
        if (other == null) throw new IllegalArgumentException("localizedCaseInsensitiveCompare: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("localizedCaseInsensitiveCompare:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("localizedCaseInsensitiveCompare: failed", t);
        }
    }

    /// localizedStandardCompare: — Finder-like sorting for displayed lists.
    public long localizedStandardCompare(NSString other) {
        ensureInit();
        if (other == null) throw new IllegalArgumentException("localizedStandardCompare: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("localizedStandardCompare:"), other.peer());
        } catch (Throwable t) {
            throw new RuntimeException("localizedStandardCompare: failed", t);
        }
    }

    /// hasPrefix: — locale-unaware prefix match.
    public boolean hasPrefix(NSString prefix) {
        ensureInit();
        if (prefix == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("hasPrefix:"), prefix.peer());
        } catch (Throwable t) {
            throw new RuntimeException("hasPrefix: failed", t);
        }
    }

    /// hasPrefix: with a Java string.
    public boolean hasPrefix(String prefix) {
        if (prefix == null) return false;
        return hasPrefix(of(prefix));
    }

    /// hasSuffix: — locale-unaware suffix match.
    public boolean hasSuffix(NSString suffix) {
        ensureInit();
        if (suffix == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("hasSuffix:"), suffix.peer());
        } catch (Throwable t) {
            throw new RuntimeException("hasSuffix: failed", t);
        }
    }

    /// hasSuffix: with a Java string.
    public boolean hasSuffix(String suffix) {
        if (suffix == null) return false;
        return hasSuffix(of(suffix));
    }

    /// containsString: — case-sensitive, locale-unaware containment.
    public boolean containsString(NSString str) {
        ensureInit();
        if (str == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("containsString:"), str.peer());
        } catch (Throwable t) {
            throw new RuntimeException("containsString: failed", t);
        }
    }

    /// containsString: with a Java string.
    public boolean containsString(String str) {
        if (str == null) return false;
        return containsString(of(str));
    }

    /// localizedCaseInsensitiveContainsString:.
    public boolean localizedCaseInsensitiveContainsString(NSString str) {
        ensureInit();
        if (str == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("localizedCaseInsensitiveContainsString:"), str.peer());
        } catch (Throwable t) {
            throw new RuntimeException("localizedCaseInsensitiveContainsString: failed", t);
        }
    }

    /// localizedStandardContainsString: — user-level, locale-aware search.
    public boolean localizedStandardContainsString(String str) {
        ensureInit();
        if (str == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("localizedStandardContainsString:"), ObjC.nsstring(str));
        } catch (Throwable t) {
            throw new RuntimeException("localizedStandardContainsString: failed", t);
        }
    }

    /// localizedStandardRangeOfString: — user-level, locale-aware search range.
    public NSRange localizedStandardRangeOfString(String str) {
        ensureInit();
        if (str == null) return new NSRange(NSRange.NOT_FOUND, 0);
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID));
            MemorySegment seg = (MemorySegment) h.invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("localizedStandardRangeOfString:"), ObjC.nsstring(str));
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("localizedStandardRangeOfString: failed", t);
        }
    }

    /// rangeOfString:options: — search with compare-option flags (see CASE_* constants).
    public NSRange rangeOfString(NSString searchString, long options) {
        ensureInit();
        if (searchString == null) return new NSRange(NSRange.NOT_FOUND, 0);
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID, Arg.INT));
            MemorySegment seg = (MemorySegment) h.invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("rangeOfString:options:"), searchString.peer(), options);
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("rangeOfString:options: failed", t);
        }
    }

    /// rangeOfString:options: with a Java search string.
    public NSRange rangeOfString(String searchString, long options) {
        if (searchString == null) return new NSRange(NSRange.NOT_FOUND, 0);
        return rangeOfString(of(searchString), options);
    }

    /// rangeOfCharacterFromSet: — first character from the set (NSCharacterSet peer,
    /// or nil for NOT_FOUND). No NSCharacterSet wrapper in this batch: build one via
    /// `ObjC.msgSendId(ObjC.cls("NSCharacterSet"), ObjC.sel("decimalDigitCharacterSet"))`.
    public NSRange rangeOfCharacterFromSet(MemorySegment searchSet) {
        ensureInit();
        if (searchSet == null || searchSet.address() == 0) return new NSRange(NSRange.NOT_FOUND, 0);
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID));
            MemorySegment seg = (MemorySegment) h.invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("rangeOfCharacterFromSet:"), searchSet);
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("rangeOfCharacterFromSet: failed", t);
        }
    }

    /// rangeOfCharacterFromSet:options:.
    public NSRange rangeOfCharacterFromSet(MemorySegment searchSet, long options) {
        ensureInit();
        if (searchSet == null || searchSet.address() == 0) return new NSRange(NSRange.NOT_FOUND, 0);
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.ID, Arg.INT));
            MemorySegment seg = (MemorySegment) h.invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("rangeOfCharacterFromSet:options:"), searchSet, options);
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("rangeOfCharacterFromSet:options: failed", t);
        }
    }

    /// rangeOfComposedCharacterSequencesForRange: — grapheme-safe range expansion.
    public NSRange rangeOfComposedCharacterSequencesForRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.RANGE));
            MemorySegment seg = (MemorySegment) h.invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("rangeOfComposedCharacterSequencesForRange:"), range.toSegment());
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("rangeOfComposedCharacterSequencesForRange: failed", t);
        }
    }

    /// lineRangeForRange:.
    public NSRange lineRangeForRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.RANGE));
            MemorySegment seg = (MemorySegment) h.invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("lineRangeForRange:"), range.toSegment());
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("lineRangeForRange: failed", t);
        }
    }

    /// paragraphRangeForRange:.
    public NSRange paragraphRangeForRange(NSRange range) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RANGE, Arg.RANGE));
            MemorySegment seg = (MemorySegment) h.invokeExact(
                    ObjC.structSlot(), peer, ObjC.sel("paragraphRangeForRange:"), range.toSegment());
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("paragraphRangeForRange: failed", t);
        }
    }

    /// stringByAppendingString:.
    public NSString stringByAppendingString(NSString other) {
        ensureInit();
        if (other == null) return wrap(peer);
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("stringByAppendingString:"), other.peer()));
    }

    /// stringByAppendingString: with a Java string.
    public NSString stringByAppendingString(String other) {
        if (other == null) return wrap(peer);
        return stringByAppendingString(of(other));
    }

    /// doubleValue — skips leading whitespace, ignores trailing characters (not locale-aware).
    public double doubleValue() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("doubleValue"));
        } catch (Throwable t) {
            throw new RuntimeException("doubleValue failed", t);
        }
    }

    /// floatValue.
    public float floatValue() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.FLOAT));
            return (float) h.invokeExact(peer, ObjC.sel("floatValue"));
        } catch (Throwable t) {
            throw new RuntimeException("floatValue failed", t);
        }
    }

    /// intValue.
    public long intValue() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("intValue"));
        } catch (Throwable t) {
            throw new RuntimeException("intValue failed", t);
        }
    }

    /// integerValue.
    public long integerValue() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("integerValue"));
        } catch (Throwable t) {
            throw new RuntimeException("integerValue failed", t);
        }
    }

    /// longLongValue.
    public long longLongValue() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("longLongValue"));
        } catch (Throwable t) {
            throw new RuntimeException("longLongValue failed", t);
        }
    }

    /// boolValue — YES for leading Y/y/T/t/1-9 (after whitespace and optional sign).
    public boolean boolValue() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL));
            return (boolean) h.invokeExact(peer, ObjC.sel("boolValue"));
        } catch (Throwable t) {
            throw new RuntimeException("boolValue failed", t);
        }
    }

    /// uppercaseString — canonical (non-localized) mapping.
    public NSString uppercaseString() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("uppercaseString")));
    }

    /// lowercaseString — canonical (non-localized) mapping.
    public NSString lowercaseString() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("lowercaseString")));
    }

    /// capitalizedString — canonical (non-localized) mapping.
    public NSString capitalizedString() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("capitalizedString")));
    }

    /// localizedUppercaseString.
    public NSString localizedUppercaseString() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("localizedUppercaseString")));
    }

    /// localizedLowercaseString.
    public NSString localizedLowercaseString() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("localizedLowercaseString")));
    }

    /// localizedCapitalizedString.
    public NSString localizedCapitalizedString() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("localizedCapitalizedString")));
    }

    /// uppercaseStringWithLocale: — NSLocale peer, or NULL for the canonical mapping.
    public NSString uppercaseStringWithLocale(MemorySegment locale) {
        ensureInit();
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("uppercaseStringWithLocale:"),
                (locale == null ? MemorySegment.NULL : locale)));
    }

    /// lowercaseStringWithLocale: — NSLocale peer, or NULL for the canonical mapping.
    public NSString lowercaseStringWithLocale(MemorySegment locale) {
        ensureInit();
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("lowercaseStringWithLocale:"),
                (locale == null ? MemorySegment.NULL : locale)));
    }

    /// capitalizedStringWithLocale: — NSLocale peer, or NULL for the canonical mapping.
    public NSString capitalizedStringWithLocale(MemorySegment locale) {
        ensureInit();
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("capitalizedStringWithLocale:"),
                (locale == null ? MemorySegment.NULL : locale)));
    }

    /// componentsSeparatedByString:.
    public NSArray componentsSeparatedByString(NSString separator) {
        ensureInit();
        if (separator == null) throw new IllegalArgumentException("componentsSeparatedByString: null");
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("componentsSeparatedByString:"), separator.peer()));
    }

    /// componentsSeparatedByString: with a Java separator.
    public NSArray componentsSeparatedByString(String separator) {
        if (separator == null) throw new IllegalArgumentException("componentsSeparatedByString: null");
        return componentsSeparatedByString(of(separator));
    }

    /// componentsSeparatedByCharactersInSet: — NSCharacterSet peer (see rangeOfCharacterFromSet:).
    public NSArray componentsSeparatedByCharactersInSet(MemorySegment separatorSet) {
        ensureInit();
        if (separatorSet == null || separatorSet.address() == 0)
            throw new IllegalArgumentException("componentsSeparatedByCharactersInSet: null");
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("componentsSeparatedByCharactersInSet:"), separatorSet));
    }

    /// stringByTrimmingCharactersInSet: — NSCharacterSet peer (see rangeOfCharacterFromSet:).
    public NSString stringByTrimmingCharactersInSet(MemorySegment set) {
        ensureInit();
        if (set == null || set.address() == 0) return wrap(peer);
        return wrap(ObjC.msgSendIdId(peer, ObjC.sel("stringByTrimmingCharactersInSet:"), set));
    }

    /// stringByFoldingWithOptions:locale: — character folding (pass CASE_/DIACRITIC_* flags).
    /// NSLocale peer, or NULL for the canonical mapping.
    public NSString stringByFoldingWithOptions(long options, MemorySegment locale) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("stringByFoldingWithOptions:locale:"),
                    options, (MemorySegment) (locale == null ? MemorySegment.NULL : locale));
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("stringByFoldingWithOptions:locale: failed", t);
        }
    }

    /// stringByReplacingOccurrencesOfString:withString: (whole string, literal match).
    public NSString stringByReplacingOccurrencesOfString(NSString target, NSString replacement) {
        ensureInit();
        if (target == null || replacement == null) throw new IllegalArgumentException("stringByReplacingOccurrencesOfString: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(
                    peer, ObjC.sel("stringByReplacingOccurrencesOfString:withString:"), target.peer(), replacement.peer());
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("stringByReplacingOccurrencesOfString:withString: failed", t);
        }
    }

    /// stringByReplacingOccurrencesOfString:withString: with Java strings.
    public NSString stringByReplacingOccurrencesOfString(String target, String replacement) {
        if (target == null || replacement == null) throw new IllegalArgumentException("stringByReplacingOccurrencesOfString: null");
        return stringByReplacingOccurrencesOfString(of(target), of(replacement));
    }

    /// stringByReplacingCharactersInRange:withString:.
    public NSString stringByReplacingCharactersInRange(NSRange range, NSString replacement) {
        ensureInit();
        if (replacement == null) throw new IllegalArgumentException("stringByReplacingCharactersInRange: null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.RANGE, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(
                    peer, ObjC.sel("stringByReplacingCharactersInRange:withString:"), range.toSegment(), replacement.peer());
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("stringByReplacingCharactersInRange:withString: failed", t);
        }
    }

    /// stringByReplacingCharactersInRange:withString: with a Java replacement string.
    public NSString stringByReplacingCharactersInRange(NSRange range, String replacement) {
        if (replacement == null) throw new IllegalArgumentException("stringByReplacingCharactersInRange: null");
        return stringByReplacingCharactersInRange(range, of(replacement));
    }

    /// stringByApplyingTransform:reverse: — ICU transliteration (e.g. "Latin-Katakana").
    /// Returns nil for an invalid transform or an irreversible reverse (wrap null).
    public NSString stringByApplyingTransform(String transform, boolean reverse) {
        ensureInit();
        if (transform == null) return null;
        return stringByApplyingTransform(of(transform), reverse);
    }

    /// stringByApplyingTransform:reverse: with an NSString transform id.
    public NSString stringByApplyingTransform(NSString transform, boolean reverse) {
        ensureInit();
        if (transform == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.BOOL));
            MemorySegment r = (MemorySegment) h.invokeExact(
                    peer, ObjC.sel("stringByApplyingTransform:reverse:"), transform.peer(), reverse);
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("stringByApplyingTransform:reverse: failed", t);
        }
    }

    /// decomposedStringWithCanonicalMapping.
    public NSString decomposedStringWithCanonicalMapping() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("decomposedStringWithCanonicalMapping")));
    }

    /// precomposedStringWithCanonicalMapping.
    public NSString precomposedStringWithCanonicalMapping() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("precomposedStringWithCanonicalMapping")));
    }

    /// decomposedStringWithCompatibilityMapping.
    public NSString decomposedStringWithCompatibilityMapping() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("decomposedStringWithCompatibilityMapping")));
    }

    /// precomposedStringWithCompatibilityMapping.
    public NSString precomposedStringWithCompatibilityMapping() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, ObjC.sel("precomposedStringWithCompatibilityMapping")));
    }

    /// dataUsingEncoding: — external representation as NSData.
    public NSData dataUsingEncoding(long encoding) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("dataUsingEncoding:"), encoding);
            return NSData.wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("dataUsingEncoding: failed", t);
        }
    }

    /// dataUsingEncoding:allowLossyConversion:.
    public NSData dataUsingEncoding(long encoding, boolean lossy) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.BOOL));
            MemorySegment r = (MemorySegment) h.invokeExact(
                    peer, ObjC.sel("dataUsingEncoding:allowLossyConversion:"), encoding, lossy);
            return NSData.wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("dataUsingEncoding:allowLossyConversion: failed", t);
        }
    }

    /// canBeConvertedToEncoding:.
    public boolean canBeConvertedToEncoding(long encoding) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.INT));
            return (boolean) h.invokeExact(peer, ObjC.sel("canBeConvertedToEncoding:"), encoding);
        } catch (Throwable t) {
            throw new RuntimeException("canBeConvertedToEncoding: failed", t);
        }
    }

    /// lengthOfBytesUsingEncoding: — exact byte count (O(n); 0 when inconvertible).
    public long lengthOfBytesUsingEncoding(long encoding) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("lengthOfBytesUsingEncoding:"), encoding);
        } catch (Throwable t) {
            throw new RuntimeException("lengthOfBytesUsingEncoding: failed", t);
        }
    }

    /// maximumLengthOfBytesUsingEncoding: — O(1) estimate (0 on overflow).
    public long maximumLengthOfBytesUsingEncoding(long encoding) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("maximumLengthOfBytesUsingEncoding:"), encoding);
        } catch (Throwable t) {
            throw new RuntimeException("maximumLengthOfBytesUsingEncoding: failed", t);
        }
    }

    /// fastestEncoding — O(1) rough estimate.
    public long fastestEncoding() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("fastestEncoding"));
        } catch (Throwable t) {
            throw new RuntimeException("fastestEncoding failed", t);
        }
    }

    /// smallestEncoding — O(n) most-compact encoding.
    public long smallestEncoding() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(peer, ObjC.sel("smallestEncoding"));
        } catch (Throwable t) {
            throw new RuntimeException("smallestEncoding failed", t);
        }
    }

    /// UTF8String — inner NUL-terminated pointer (valid while the string lives).
    /// Prefer `string()`; this is for byte-level interop (mirrors NSData.bytes()).
    public MemorySegment utf8CString() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("UTF8String"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) {
            throw new RuntimeException("UTF8String failed", t);
        }
    }

    /// cStringUsingEncoding: — inner NUL-terminated pointer for 8-bit encodings
    /// (NULL when inconvertible; valid while the string lives).
    public MemorySegment cStringUsingEncoding(long encoding) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("cStringUsingEncoding:"), encoding);
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) {
            throw new RuntimeException("cStringUsingEncoding: failed", t);
        }
    }

    /// +localizedNameOfStringEncoding:.
    public static NSString localizedNameOfStringEncoding(long encoding) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment r = (MemorySegment) h.invokeExact(
                    ObjC.cls("NSString"), ObjC.sel("localizedNameOfStringEncoding:"), encoding);
            return wrap(r);
        } catch (Throwable t) {
            throw new RuntimeException("localizedNameOfStringEncoding: failed", t);
        }
    }

    /// +defaultCStringEncoding — rarely needed; user-language-derived.
    public static long defaultCStringEncoding() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT));
            return (long) h.invokeExact(ObjC.cls("NSString"), ObjC.sel("defaultCStringEncoding"));
        } catch (Throwable t) {
            throw new RuntimeException("defaultCStringEncoding failed", t);
        }
    }

    /// +availableStringEncodings — const NSStringEncoding* (0-terminated C array).
    /// Raw pointer (valid forever; owned by Foundation). Read longs until a 0 entry.
    public static MemorySegment availableStringEncodings() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(ObjC.cls("NSString"), ObjC.sel("availableStringEncodings"));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) {
            throw new RuntimeException("availableStringEncodings failed", t);
        }
    }

    @Override
    public String toString() {
        String s = string();
        return s != null ? s : super.toString();
    }
}
