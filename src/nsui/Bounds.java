package nsui;

import java.lang.foreign.MemorySegment;

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

    /// Reject NaN geometry, which AppKit TRAPS on (SIGTRAP, exit 133) in the layout setters.
    ///
    /// Measured with standalone receivers: a NaN in any component of
    /// NSView setFrame:/setFrameOrigin:/setFrameSize:/setBounds:/setBoundsSize:/scrollPoint:/
    /// scrollRectToVisible:, NSClipView scrollToPoint:, NSWindow setFrame:display:/
    /// setContentSize: and initWithFrame: (every view factory) traps the process (SIGTRAP, 133);
    /// NSWindow setFrameOrigin:/setFrameTopLeftPoint:, CALayer setFrame:/setPosition:/setBounds:
    /// and NSSplitView setPosition:ofDividerAtIndex: raise NSInvalidArgumentException instead.
    /// INFINITY is TOLERATED by the view/window geometry setters (measured exit 0), so only NaN
    /// is rejected here — over-rejecting INF would refuse a value AppKit accepts. Deliberately
    /// NOT guarded, because AppKit tolerates NaN there (all measured exit 0): setMinSize:/
    /// setMaxSize:/setAspectRatio:/setContentMinSize:/setResizeIncrements:,
    /// setNeedsDisplayInRect:, displayRect:, setKeyboardFocusRingNeedsDisplayInRect:, hitTest:,
    /// NSScrollView setContentInsets:, backingAlignedRect:options:, the convert* family, alpha
    /// and magnification, NSTableColumn.setWidth:, NSSlider/NSProgressIndicator setMinValue:.
    /// (NSSplitView's single-argument setPosition:, which DOES abort without a divider, is not
    /// reachable: this toolkit only ever sends setPosition:ofDividerAtIndex:, which is tolerant
    /// for any index, measured.)
    public static void notNaN(NSRect r, String what) {
        if (r == null) throw new IllegalArgumentException(what + ": null rect");
        if (Double.isNaN(r.x()) || Double.isNaN(r.y())
                || Double.isNaN(r.width()) || Double.isNaN(r.height())) {
            throw new IllegalArgumentException(what + ": rect " + r
                    + " contains NaN (AppKit traps on NaN geometry)");
        }
    }

    /// See notNaN(NSRect,String) — same rule for a size.
    public static void notNaN(NSSize s, String what) {
        if (s == null) throw new IllegalArgumentException(what + ": null size");
        if (Double.isNaN(s.width()) || Double.isNaN(s.height())) {
            throw new IllegalArgumentException(what + ": size " + s
                    + " contains NaN (AppKit traps on NaN geometry)");
        }
    }

    /// See notNaN(NSRect,String) — same rule for a point.
    public static void notNaN(NSPoint p, String what) {
        if (p == null) throw new IllegalArgumentException(what + ": null point");
        if (Double.isNaN(p.x()) || Double.isNaN(p.y())) {
            throw new IllegalArgumentException(what + ": point " + p
                    + " contains NaN (AppKit traps on NaN geometry)");
        }
    }

    /// Reject NaN and +/-INFINITY: NSSplitView.setPosition: raises for both (measured exit 134),
    /// unlike the view/window geometry setters, which accept INFINITY.
    public static void finite(double value, String what) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(what + ": value " + value
                    + " is not finite (AppKit raises for NaN and infinity here)");
        }
    }

    /// A required (non-null) argument. AppKit declares these `nonnull`, so `null`
    /// is a caller error; report it as `IllegalArgumentException` naming the call
    /// instead of a raw NullPointerException at the peer dereference.
    ///
    /// A raw-id argument can be non-null and still model nil: the toolkit's
    /// nullable-argument convention is to pass `MemorySegment.NULL` (see
    /// `ObjC.nsstring`). Testing only the Java reference let exactly that value
    /// through, so a caller following the documented convention reached the native
    /// nonnull parameter and aborted the JVM (verified: `moveItemAtIndexPath(NULL,·)`,
    /// `setSelectionIndexPaths(NULL)`, `makeItemWithIdentifier(·,NULL)` all raise
    /// `NSInternalInconsistencyException`). Treat address 0 as absent too.
    ///
    /// All 152 `Bounds.nonNull` call sites were swept against the SDK headers and each one
    /// guards a header-nonnull parameter. The single exception found — `NSControl.sendAction`,
    /// whose SEL is explicitly `nullable` and whose NULL is a harmless NO — was removed
    /// rather than kept. Raw-id (MemorySegment) sites were additionally nil-probed; see the
    /// 8c/8l sections of AuditRegressionTest for the ones with a native abort behind them.
    static void nonNull(Object value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " requires a non-null argument");
        }
        if (value instanceof MemorySegment seg && seg.address() == 0) {
            throw new IllegalArgumentException(what + " requires a non-null argument"
                    + " (got MemorySegment.NULL; nil is not accepted here)");
        }
    }

    /// `index` must be `>= 0`, with no upper bound.
    ///
    /// For APIs where the upper bound cannot be established from Java without a false
    /// rejection. `NSToolbar` is the worked example: its assertions are
    /// `0 <= index <= [_currentItems count]` (insert) and `0 <= index < [_currentItems count]`
    /// (remove), but the realized count is not usable as a Java-side bound. An item inserted
    /// with an identifier AppKit does not realize leaves the toolbar empty, and whether
    /// `removeItemAtIndex:0` then succeeds or aborts depends on that unrealized state — it
    /// succeeds when nothing queried the toolbar in between and aborts after an
    /// `items()`/`itemIdentifiers()` query, even though both states report a count of 0.
    /// `-items` does track `_currentItems` in every state measured; the two states simply
    /// look identical from Java, so any count-based bound either rejects the legal order or
    /// misses the abort. Negative indexes raise natively in both states, so that half is the
    /// safe one to check. See the NSToolbar block comment for the full measurement.
    static void nonNegative(long index, String what) {
        if (index < 0) {
            throw new IllegalArgumentException(what + ": index " + index + " must be >= 0");
        }
    }

    /// `index` must satisfy `0 <= index < count` (element access / removal).
    static void index(long index, long count, String what) {
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException(what + ": index " + index
                    + " out of bounds (count " + count + ")");
        }
    }

    /// `index` must satisfy `0 <= index <= max`, where `max` is the caller's inclusive
    /// bound: for a plain array insert that is the element count (inserting at `count`
    /// appends), while `NSMutableOrderedSet.moveObjectsAtIndexes:toIndex:` passes
    /// `count - k` because the destination indexes the array after the moved objects are
    /// removed. The message says "max" rather than "count" so an adjusted bound cannot read
    /// as if it were the element count.
    static void insertIndex(long index, long count, String what) {
        if (index < 0 || index > count) {
            throw new IllegalArgumentException(what + ": index " + index
                    + " out of bounds (max " + count + ")");
        }
    }

    /// Two parallel counts must agree. A mismatch is not a range error: AppKit raises
    /// `NSInvalidArgumentException` ("count of array (1) differs from count of index
    /// set (2)"), which aborts the JVM, so it is pre-validated here.
    static void equalCounts(long a, long b, String what) {
        if (a != b) {
            throw new IllegalArgumentException(what + ": count mismatch (" + a + " vs " + b + ")");
        }
    }

    /// `range` must be non-null and lie inside `[0, count]`.
    ///
    /// A negative `length` is rejected even though AppKit's own check sometimes tolerates it:
    /// `smartDeleteRangeForProposedRange:` measures the wrapped `NSMaxRange(loc + len)`, so
    /// `{2, -1}` happens to be accepted (2 + (-1) = 1, in bounds) while `{0, -1}` aborts.
    /// Mirroring that wraparound would accept nonsense like `{4, -1}` in other methods, and a
    /// negative length is never a legitimate argument, so it is treated as a caller error.
    /// Everything the callers of this helper actually accept (an overflowing positive length)
    /// still aborts natively, which is why the strict bound is correct for them.
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
    ///
    /// This helper takes a typed `NSIndexSet`, so it can only guard the wrappers that expose
    /// one. Several AppKit methods take a raw `MemorySegment` peer instead (NSTableView's
    /// selectRowIndexes:/selectColumnIndexes:/noteHeightOfRowsWithIndexesChanged:/
    /// reloadDataForRowIndexes:/hideRowsAtIndexes: and NSCollectionView's
    /// insertSections:/deleteSections:/reloadSections:) and therefore cannot be pre-validated
    /// here. Measured on POPULATED views (2-section collection view, 3-row view-based table):
    /// the collection-view section methods and the table's reload/noteHeight/select methods
    /// tolerate an out-of-range set, while the table's insert/remove/hide/unhideRowsAtIndexes:
    /// abort and are guarded at their call sites with this helper (see NSTableView).
    static void indexSet(NSIndexSet indexes, long count, String what) {
        if (indexes == null || indexes.isEmpty()) return;
        long last = indexes.lastIndex();
        if (last < 0 || last >= count) {
            throw new IllegalArgumentException(what + ": index set reaches " + last
                    + " out of bounds (count " + count + ")");
        }
    }

    /// A caller-supplied out-parameter buffer must be NULL (ignore) or at least
    /// `bytes` wide. Native code writes the full width regardless of the segment's
    /// declared size, so a short segment corrupts neighbouring heap memory. Returns
    /// the segment to forward (NULL for nil).
    static MemorySegment outBuffer(MemorySegment seg, long bytes, String what) {
        if (seg == null || seg.address() == 0) return MemorySegment.NULL;
        if (seg.byteSize() < bytes) {
            throw new IllegalArgumentException(what + ": out buffer must be null or at least "
                    + bytes + " bytes (got " + seg.byteSize() + ")");
        }
        return seg;
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
