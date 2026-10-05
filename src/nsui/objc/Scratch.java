package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/// Per-turn bump arena ("scratch") for by-value INPUT marshalling.
///
/// Every NSUI event turn allocates dozens of transient C values on its way to
/// `objc_msgSend`: 32-byte `NSRect` structs per CG call, NUL-terminated C
/// strings per selector/class/NSString, out-buffers for `NSColor.rgba()`, and so
/// on. Historically all of these landed in `Arena.global()` (IMMORTAL), so a
/// 60 fps demo leaked dozens of immortal segments per frame — unbounded memory
/// growth plus GC churn. `Scratch` fixes this:
///
/// - A *thread-local* turn buffer — one ~1 MiB segment per thread, lazily
///   allocated on first use (never in a static initializer). A bump offset hands out
/// aligned slices.
/// - `beginTurn`/`endTurn` manage nesting via an integer depth;
///   only the *outermost* `endTurn()` rewinds the offset to 0.
/// - When NO turn is active, `alloc` falls back to `Arena.global()`
///   — `alloc` never returns `null`.
/// - Per-allocation alignment is rounded up to 8 bytes. A single allocation that
///   exceeds the buffer, or would push past 75% full, falls back to the global arena
/// for *that* allocation only — the buffer is never grown, and never fails.
///
/// SAFETY RULE (read this before using)
///
/// ALLOCATION TIERS (read this before using)
///
/// - `allocInput` — the DEFAULT for call-scoped by-value INPUTS (`ObjC.rect`,
///   `ObjC.cstring`): struct arguments and C strings the callee consumes
///   synchronously (struct read / string copy, e.g. `sel_registerName`,
///   `objc_getClass`, `[NSString stringWithUTF8String:]`). No turn required:
///   bumps the thread-local buffer while there is room (rewound at the next
///   outermost `endTurn`); that allocation spills to the global arena when
///   full. Slices are valid only until the next rewind — pass them straight
///   into the call, never stash them, never read them past a turn boundary.
/// - `alloc` (turn-gated) — general scratch, valid only DURING the turn.
/// Anything the caller reads AFTER the call returns — strings held across a
/// turn, for example — MUST come from the global arena. Struct RETURNS are a
/// separate case: the `NSRect` written by `objc_msgSend_stret` lands in a
/// per-thread reusable slot (`ObjC` internals), safe because every caller
/// copies the doubles out immediately; it is never scratch (a turn rewind
/// must not invalidate it) and never a fresh global slice per call. In
/// particular `rect` returns a segment that `msgSendRect` feeds as an
/// *input*, which is scratch-legal, while the escape-hatch paths keep their
/// results in the global arena.
public final class Scratch {

    /// Scratch buffer default size: 1 MiB.
    public static final long BUFFER_BYTES = 1_048_576;

    /// Below this full fraction we keep bumping from the buffer; above it, fall back.
    private static final double WARN_LEVEL = 0.75; // per-turn bump budget: ~768 KiB of the 1 MiB buffer

    /// Per-thread bump buffer. The `ThreadLocal` itself is a `final` field (fine);
    /// the segment inside is allocated lazily at runtime, never in a static initializer.
    private static final ThreadLocal<Buffer> BUFFERS = ThreadLocal.withInitial(Buffer::new);

    /// Current turn-nesting depth, per thread.
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private Scratch() {}

    /// A single thread-local bump buffer: one `Arena.global().allocate(bufBytes, 8)`
    /// segment plus a mutable bump offset. Rounded up to 8-byte alignment on every bump.
    private static final class Buffer {
        final MemorySegment seg;
        long offset;

        Buffer() {
            this.seg = Arena.global().allocate(BUFFER_BYTES, 8);
            this.offset = 0;
        }
    }

    /// Start a turn. May nest; only the outermost matching `endTurn` resets the buffer.
    public static void beginTurn() {
        DEPTH.set(DEPTH.get() + 1);
    }

    /// End a turn. Only when the depth returns to 0 does the buffer rewind (offset &rarr; 0),
    /// freeing all scratch slices for reuse next turn. Underflowing is treated as a full reset
    /// (defense-in-depth) so a stray extra `endTurn()` can never corrupt the buffer.
    public static void endTurn() {
        int d = DEPTH.get();
        if (d <= 1) {
            DEPTH.set(0);
            BUFFERS.get().offset = 0;
        } else {
            DEPTH.set(d - 1);
        }
    }

    /// Allocate `byteSize` bytes of scratch if a turn is active, else from the global
    /// arena. Never returns `null`. During a turn, the size is rounded up to 8 bytes and
    /// the bump advanced; if a single allocation exceeds the buffer or would push past the 75%
    /// warn level, that allocation comes from `Arena.global()` instead (no growth, no error).
    public static MemorySegment alloc(long byteSize) {
        if (byteSize < 0) {
            throw new IllegalArgumentException("negative alloc size: " + byteSize);
        }
        if (byteSize == 0) byteSize = 1; // keep an 8-byte granule
        if (DEPTH.get() > 0) {
            long round = (byteSize + 7) & ~7L;
            Buffer b = BUFFERS.get();
            if (round <= BUFFER_BYTES && (double) (b.offset + round) <= WARN_LEVEL * BUFFER_BYTES) {
                long base = b.offset;
                b.offset += round;
                return b.seg.asSlice(base, round);
            }
            // outside the bump window: this single allocation goes to the global arena.
            return Arena.global().allocate(round, 8);
        }
        return Arena.global().allocate(roundUp(byteSize), 8);
    }

    /// Bump allocation for call-scoped by-value INPUTS with no turn required
    /// (see ALLOCATION TIERS above). Bumps the thread-local buffer while there
    /// is room — rewound at the next outermost `endTurn` — and falls back to
    /// the global arena for that allocation when full. Never returns `null`.
    public static MemorySegment allocInput(long byteSize) {
        if (byteSize < 0) {
            throw new IllegalArgumentException("negative alloc size: " + byteSize);
        }
        if (byteSize == 0) byteSize = 1; // keep an 8-byte granule
        long round = (byteSize + 7) & ~7L;
        Buffer b = BUFFERS.get();
        if (round <= BUFFER_BYTES && (double) (b.offset + round) <= WARN_LEVEL * BUFFER_BYTES) {
            long base = b.offset;
            b.offset += round;
            return b.seg.asSlice(base, round);
        }
        // outside the bump window: this single allocation goes to the global arena.
        return Arena.global().allocate(round, 8);
    }

    private static long roundUp(long v) {
        return (v + 7) & ~7L;
    }

    /// Current nesting depth for the calling thread.
    public static int depth() {
        return DEPTH.get();
    }

    /// Whether a turn is currently active on the calling thread (depth > 0).
    public static boolean active() {
        return DEPTH.get() > 0;
    }

    /// Bytes of scratch currently in use (bump offset); 0 when no turn is active.
    public static long used() {
        if (DEPTH.get() == 0) return 0;
        return BUFFERS.get().offset;
    }
}
