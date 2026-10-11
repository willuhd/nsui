package nsui;

/// Java-side pre-validation for native index and range arguments.
///
/// Foundation and AppKit raise an Objective-C `NSRangeException` when an index or
/// range is out of bounds, and such an exception is **uncatchable** from a pure-FFM
/// process: it reaches `__cxa_throw` with no landing pad and aborts the JVM (see
/// `nsui.objc.Exceptions`). Validating in Java converts a process abort into an
/// `IllegalArgumentException` that names the call and the offending value.
///
/// The bounds are the caller's obligation to get right, exactly as they are in
/// Objective-C — this only decides *how* the mistake is reported.
final class Bounds {

    private Bounds() {}

    /// A required (non-null) argument. AppKit declares these `nonnull`, so `null`
    /// is a caller error; report it as `IllegalArgumentException` naming the call
    /// instead of a raw NullPointerException at the peer dereference.
    static void nonNull(Object value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " requires a non-null argument");
        }
    }

    /// `index` must satisfy `0 <= index < count` (element access / removal).
    static void index(long index, long count, String what) {
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException(what + ": index " + index
                    + " out of bounds (count " + count + ")");
        }
    }

    /// `index` must satisfy `0 <= index <= count` (array-insert semantics: inserting
    /// at `count` appends).
    static void insertIndex(long index, long count, String what) {
        if (index < 0 || index > count) {
            throw new IllegalArgumentException(what + ": index " + index
                    + " out of bounds (count " + count + ", insert allows count)");
        }
    }

    /// `range` must be non-null and lie inside `[0, count]`.
    static void range(NSRange range, long count, String what) {
        if (range == null) {
            throw new IllegalArgumentException(what + ": null range");
        }
        long loc = range.location();
        long len = range.length();
        if (loc < 0 || len < 0 || loc > count || len > count - loc) {
            throw new IllegalArgumentException(what + ": range {location=" + loc
                    + ", length=" + len + "} out of bounds (count " + count + ")");
        }
    }

    /// Every index in a non-empty index set must be `< count`. An empty set is a
    /// no-op (matches the native methods).
    static void indexSet(NSIndexSet indexes, long count, String what) {
        if (indexes == null || indexes.isEmpty()) return;
        long last = indexes.lastIndex();
        if (last < 0 || last >= count) {
            throw new IllegalArgumentException(what + ": index set reaches " + last
                    + " out of bounds (count " + count + ")");
        }
    }

    /// `start`/`count` must satisfy `0 <= start <= start + count <= total` — the
    /// shape shared by `replaceBytesInRange:`-style byte/character windows.
    static void window(long start, long count, long total, String what) {
        if (start < 0 || count < 0 || start > total || count > total - start) {
            throw new IllegalArgumentException(what + ": window [start=" + start
                    + ", count=" + count + "] out of bounds (total " + total + ")");
        }
    }
}
