package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentLinkedQueue;

/// Minimal libdispatch bindings (Grand Central Dispatch), built purely via the Java
/// FFM API. Currently supports dispatching work onto the main dispatch queue.
///
/// Key facts:
/// - `dispatch_async(queue, block)` is a real exported function of
///   `/usr/lib/libSystem.B.dylib`.
/// - `dispatch_get_main_queue()` is an *inline* function and is
///   *not* exported. Its backing exported DATA symbol is
/// `_dispatch_main_q`, which *is* the main queue object itself —
/// so the queue handle is the symbol's *address* (it must NOT be dereferenced).
///
/// Dispatch model (AOT-safe): the block body is a SINGLE static upcall target
/// (`runBody`) that polls a `ConcurrentLinkedQueue` of Runnables. Every
/// `onMain` call enqueues the body and dispatches a fresh block; each executed
/// block pops exactly one Runnable. No captured/bound MethodHandles anywhere — the
/// upcall target is statically registered for native-image in NsuiFeature.
public final class Dispatch {

    private static Linker LINKER;
    private static Arena ARENA;
    private static SymbolLookup SYSTEM;

    /// The main dispatch queue (address of the exported _dispatch_main_q data symbol).
    private static MemorySegment MAIN_QUEUE;

    /// dispatch_async(queue, block) -> void downcall handle.
    private static MethodHandle hDispatchAsync;

    /// Runnables awaiting their block's execution on the main queue (FIFO).
    private static final ConcurrentLinkedQueue<Runnable> PENDING = new ConcurrentLinkedQueue<>();

    private Dispatch() {}

    /// Run-time init (native-image: no FFM work in static initializers).
    public static synchronized void ensureInit() {
        if (LINKER != null) return;
        LINKER = Linker.nativeLinker();
        ARENA = Arena.global();

        SYSTEM = SymbolLookup.libraryLookup("/usr/lib/libSystem.B.dylib", ARENA);

        // dispatch_async is a real function; use NsuiForeign's pre-seeded descriptor.
        hDispatchAsync = LINKER.downcallHandle(
                SYSTEM.find("dispatch_async")
                        .orElseThrow(() -> new IllegalStateException("dispatch_async not exported by libSystem")),
                NsuiForeign.dispatchAsync());

        // dispatch_get_main_queue is inline & not exported; _dispatch_main_q IS the
        // queue object, so the handle is the symbol address itself.
        MAIN_QUEUE = SYSTEM.find("_dispatch_main_q")
                .orElseThrow(() -> new IllegalStateException("_dispatch_main_q not exported by libSystem"));
        if (MAIN_QUEUE.address() == 0) {
            throw new IllegalStateException("_dispatch_main_q resolved to NULL address");
        }
    }

    /// Run `body` asynchronously on the main dispatch queue.
    ///
    /// The body is enqueued and a `void(^)(void)` block is dispatched; when
    /// the main run loop drains the queue, `runBody` pops the body and runs it.
    /// The test's main thread must pump the run loop for the block to be drained.
    public static void onMain(Runnable body) {
        ensureInit();
        PENDING.add(body);
        MemorySegment block = Blocks.block(runBodyHandle(), NsuiForeign.blockVoidUpcall());
        try {
            hDispatchAsync.invokeExact(MAIN_QUEUE, block);
        } catch (Throwable t) {
            throw new RuntimeException("dispatch_async failed", t);
        }
    }

    private static MethodHandle runBodyHandle() {
        try {
            return MethodHandles.lookup().findStatic(Dispatch.class, "runBody",
                    MethodType.methodType(void.class, MemorySegment.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("cannot resolve dispatch block target", e);
        }
    }

    /// Block body: pops one enqueued Runnable and runs it. STATIC and capture-free —
    /// the single upcall target behind every block, registered for AOT in NsuiFeature.
    static void runBody(MemorySegment blockSelf) {
        Runnable body = PENDING.poll();
        if (body != null) {
            body.run();
        }
    }
}
