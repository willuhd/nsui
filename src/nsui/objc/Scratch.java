package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Per-turn bump arena ("scratch") for by-value INPUT marshalling.
 *
 * <p>Every NSUI event turn allocates dozens of transient C values on its way to
 * {@code objc_msgSend}: 32-byte {@code NSRect} structs per CG call, NUL-terminated C
 * strings per selector/class/NSString, out-buffers for {@code NSColor.rgba()}, and so
 * on. Historically all of these landed in {@code Arena.global()} (IMMORTAL), so a
 * 60&nbsp;fps demo leaked dozens of immortal segments per frame — unbounded memory
 * growth plus GC churn. {@code Scratch} fixes this:
 *
 * <ul>
 *   <li>A <em>thread-local</em> turn buffer — one ~1&nbsp;MiB segment per thread, lazily
 *       allocated on first use (never in a static initializer). A bump offset hands out
 *       aligned slices.</li>
 *   <li>{@link #beginTurn()}/{@link #endTurn()} manage nesting via an integer depth;
 *       only the <em>outermost</em> {@code endTurn()} rewinds the offset to 0.</li>
 *   <li>When NO turn is active, {@link #alloc(long)} falls back to {@code Arena.global()}
 *       — {@code alloc} never returns {@code null}.</li>
 *   <li>Per-allocation alignment is rounded up to 8 bytes. A single allocation that
 *       exceeds the buffer, or would push past 75% full, falls back to the global arena
 *       for <em>that</em> allocation only — the buffer is never grown, and never fails.</li>
 * </ul>
 *
 * <h3>SAFETY RULE (read this before using)</h3>
 * <p>Scratch memory is only valid DURING the turn. It must be used exclusively for
 * <em>by-value INPUT marshalling</em>: struct arguments ({@code NSRect}, {@code NSPoint},
 * {@code objc_super}) and C strings that the callee copies before returning (e.g.
 * {@code sel_registerName}, {@code objc_getClass}, {@code [NSString stringWithUTF8String:]}).
 * Anything the caller reads AFTER the call returns — struct RETURNS such as the
 * {@code NSRect} written by {@code objc_msgSend_stret}, or strings held across a turn —
 * MUST come from the global arena. In particular {@link ObjC#rect} returns a segment that
 * {@link ObjC#msgSendRect} feeds as an <em>input</em>, which is scratch-legal, but the
 * <em>returned</em> segment of {@code msgSendRect} and the escape-hatch paths keep their
 * results in the global arena.
 */
public final class Scratch {

    /** Scratch buffer default size: 1 MiB. */
    public static final long BUFFER_BYTES = 1_048_576;

    /** Below this full fraction we keep bumping from the buffer; above it, fall back. */
    private static final double WARN_LEVEL = 0.75; // per-turn bump budget: ~768 KiB of the 1 MiB buffer

    /** Per-thread bump buffer. The {@code ThreadLocal} itself is a {@code final} field (fine);
     *  the segment inside is allocated lazily at runtime, never in a static initializer. */
    private static final ThreadLocal<Buffer> BUFFERS = ThreadLocal.withInitial(Buffer::new);

    /** Current turn-nesting depth, per thread. */
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private Scratch() {}

    /**
     * A single thread-local bump buffer: one {@code Arena.global().allocate(bufBytes, 8)}
     * segment plus a mutable bump offset. Rounded up to 8-byte alignment on every bump.
     */
    private static final class Buffer {
        final MemorySegment seg;
        long offset;

        Buffer() {
            this.seg = Arena.global().allocate(BUFFER_BYTES, 8);
            this.offset = 0;
        }
    }

    /** Start a turn. May nest; only the outermost matching {@link #endTurn()} resets the buffer. */
    public static void beginTurn() {
        DEPTH.set(DEPTH.get() + 1);
    }

    /**
     * End a turn. Only when the depth returns to 0 does the buffer rewind (offset &rarr; 0),
     * freeing all scratch slices for reuse next turn. Underflowing is treated as a full reset
     * (defense-in-depth) so a stray extra {@code endTurn()} can never corrupt the buffer.
     */
    public static void endTurn() {
        int d = DEPTH.get();
        if (d <= 1) {
            DEPTH.set(0);
            BUFFERS.get().offset = 0;
        } else {
            DEPTH.set(d - 1);
        }
    }

    /**
     * Allocate {@code byteSize} bytes of scratch if a turn is active, else from the global
     * arena. Never returns {@code null}. During a turn, the size is rounded up to 8 bytes and
     * the bump advanced; if a single allocation exceeds the buffer or would push past the 75%
     * warn level, that allocation comes from {@code Arena.global()} instead (no growth, no error).
     */
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

    private static long roundUp(long v) {
        return (v + 7) & ~7L;
    }

    /** Current nesting depth for the calling thread. */
    public static int depth() {
        return DEPTH.get();
    }

    /** Whether a turn is currently active on the calling thread (depth > 0). */
    public static boolean active() {
        return DEPTH.get() > 0;
    }

    /** Bytes of scratch currently in use (bump offset); 0 when no turn is active. */
    public static long used() {
        if (DEPTH.get() == 0) return 0;
        return BUFFERS.get().offset;
    }
}
