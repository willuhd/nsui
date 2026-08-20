package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

/// Objective-C autorelease-pool management, pure FFM.
///
/// A balanced push/pop around a block of message sends keeps autoreleased
/// objects (everything created with a `+[ClassName className...]` "convenience"
/// constructor, such as `[NSString stringWithUTF8String:]`) from accumulating
/// for the lifetime of the process. `run` is the intended API: it
/// pushes, runs the body, and pops in a `finally` so the pool is always drained
/// even when the body throws.
///
/// Both downcalls are resolved at run time in `ensureInit` — never in a
/// static initializer (native-image rule #1). Symbols are confirmed exported by
/// `/usr/lib/libobjc.A.dylib`:
/// ```
///   void  *objc_autoreleasePoolPush(void);
///   void   objc_autoreleasePoolPop(void *pool);
/// ```
public final class Autorelease {

    private static Linker LINKER;
    private static Arena ARENA;
    private static MethodHandle hPush;   // objc_autoreleasePoolPush
    private static MethodHandle hPop;    // objc_autoreleasePoolPop
    private static boolean INIT;

    private Autorelease() {}

    /// idempotent — safe to call from anywhere at runtime.
    public static synchronized void ensureInit() {
        if (INIT) return;
        LINKER = Linker.nativeLinker();
        ARENA = Arena.global();
        SymbolLookup objc = SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", ARENA);
        hPush = LINKER.downcallHandle(
                objc.find("objc_autoreleasePoolPush")
                        .orElseThrow(() -> new IllegalStateException("symbol not found: objc_autoreleasePoolPush")),
                NsuiForeign.autoreleasePoolPush());
        hPop = LINKER.downcallHandle(
                objc.find("objc_autoreleasePoolPop")
                        .orElseThrow(() -> new IllegalStateException("symbol not found: objc_autoreleasePoolPop")),
                NsuiForeign.autoreleasePoolPop());
        INIT = true;
    }

    /// Push a new autorelease pool; returns the token the matching `pop` needs.
    public static MemorySegment push() {
        ensureInit();
        try {
            return (MemorySegment) hPush.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException("objc_autoreleasePoolPush failed", t);
        }
    }

    /// Pop the autorelease pool identified by `token` (drains all autoreleased objects).
    public static void pop(MemorySegment token) {
        ensureInit();
        try {
            hPop.invokeExact(token);
        } catch (Throwable t) {
            throw new RuntimeException("objc_autoreleasePoolPop failed", t);
        }
    }

    /// Run `body` inside a fresh autorelease pool that is drained in a
    /// `finally` — the pool is always balanced, and the body's exception is
    /// never swallowed: it propagates after the pop.
    public static void run(Runnable body) {
        ensureInit();
        MemorySegment token = push();
        try {
            body.run();
        } finally {
            pop(token);
        }
    }
}
