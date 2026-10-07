package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentLinkedQueue;

import nsui.objc.Blocks;
import nsui.objc.NsuiForeign;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CATransaction — static-only utility over QuartzCore's implicit per-runloop
/// transaction. Batches layer mutations into one atomic update and controls the
/// implicit animation duration; also exposes the KVC-style value accessors and
/// a Java-friendly completion block.
///
/// Usage: `CATransaction.begin(); ...mutate layers...; CATransaction.commit();`
/// Mutations outside begin/commit join the current implicit transaction.
public final class CATransaction {

    // Same-shape class methods share handles: begin/commit/flush are all
    // (id,SEL)->void on the class object.
    private record Handles(MethodHandle hVoidClass, MethodHandle hSetDuration, MethodHandle hGetValueForKey, MethodHandle hSetValueForKey, MethodHandle hVoidId) {}
    private static volatile Handles handles;

    /// Runnables awaiting their completion delivery (FIFO).
    private static final ConcurrentLinkedQueue<Runnable> PENDING = new ConcurrentLinkedQueue<>();

    /// One shared immortal block for every completion: bodies travel via
    /// PENDING (each delivery pops exactly one), so no per-call block or stub
    /// is ever built.
    private static volatile MemorySegment SHARED_BLOCK;

    private CATransaction() {}

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.VOID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
        );
        if (SHARED_BLOCK == null) {
            SHARED_BLOCK = Blocks.block(completionThunkHandle(), NsuiForeign.blockVoidUpcall());
        }
        handles = h;
    }

    /// +[CATransaction begin] — start an explicit transaction on this thread.
    public static void begin() {
        ensureInit();
        try {
            handles.hVoidClass().invokeExact(ObjC.cls("CATransaction"), ObjC.sel("begin"));
        } catch (Throwable t) { throw new RuntimeException("CATransaction begin failed", t); }
    }

    /// +[CATransaction commit] — commit the outermost open transaction atomically.
    public static void commit() {
        ensureInit();
        try {
            handles.hVoidClass().invokeExact(ObjC.cls("CATransaction"), ObjC.sel("commit"));
        } catch (Throwable t) { throw new RuntimeException("CATransaction commit failed", t); }
    }

    /// +[CATransaction flush] — commit any pending implicit transaction and clear state.
    public static void flush() {
        ensureInit();
        try {
            handles.hVoidClass().invokeExact(ObjC.cls("CATransaction"), ObjC.sel("flush"));
        } catch (Throwable t) { throw new RuntimeException("CATransaction flush failed", t); }
    }

    /// +[CATransaction setAnimationDuration:] — duration for animations triggered
    /// inside the current transaction (seconds).
    public static void setAnimationDuration(double seconds) {
        ensureInit();
        try {
            handles.hSetDuration().invokeExact(ObjC.cls("CATransaction"), ObjC.sel("setAnimationDuration:"), seconds);
        } catch (Throwable t) { throw new RuntimeException("setAnimationDuration: failed", t); }
    }

    /// +[CATransaction setCompletionBlock:] — Java Runnable invoked after the
    /// current transaction's animations finish. The body is enqueued and a
    /// single shared global block is installed; when the transaction drains,
    /// the thunk pops the body and runs it.
    /// The callback fires on the main thread once the runloop drains the commit.
    public static void setCompletionBlock(Runnable action) {
        ensureInit();
        try {
            MemorySegment block;
            if (action == null) {
                block = MemorySegment.NULL;
            } else {
                PENDING.add(action);
                block = SHARED_BLOCK;
            }
            handles.hVoidId().invokeExact(ObjC.cls("CATransaction"), ObjC.sel("setCompletionBlock:"), (MemorySegment) (block == null ? MemorySegment.NULL : block));
        } catch (Throwable t) { throw new RuntimeException("setCompletionBlock: failed", t); }
    }

    /// +[CATransaction setCompletionBlock:] with a pre-built raw block literal
    /// (advanced use; must be a `void(^)(void)` global block).
    public static void setCompletionBlock(MemorySegment rawBlock) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(ObjC.cls("CATransaction"), ObjC.sel("setCompletionBlock:"), (MemorySegment) (rawBlock == null ? MemorySegment.NULL : rawBlock));
        } catch (Throwable t) { throw new RuntimeException("setCompletionBlock: failed", t); }
    }

    /// +[CATransaction valueForKey:] — transaction-scoped KVC value (raw id or null).
    public static MemorySegment valueForKey(String key) {
        ensureInit();
        try {
            MemorySegment v = (MemorySegment) handles.hGetValueForKey().invokeExact(ObjC.cls("CATransaction"), ObjC.sel("valueForKey:"), (MemorySegment) (key == null ? MemorySegment.NULL : ObjC.nsstring(key)));
            return (v == null || v.address() == 0) ? null : v;
        } catch (Throwable t) { throw new RuntimeException("valueForKey: failed", t); }
    }

    /// +[CATransaction setValue:forKey:] — attach a transaction-scoped object.
    public static void setValueForKey(MemorySegment value, String key) {
        ensureInit();
        try {
            handles.hSetValueForKey().invokeExact(ObjC.cls("CATransaction"), ObjC.sel("setValue:forKey:"), (MemorySegment) (value == null ? MemorySegment.NULL : value), (MemorySegment) (key == null ? MemorySegment.NULL : ObjC.nsstring(key)));
        } catch (Throwable t) { throw new RuntimeException("setValue:forKey: failed", t); }
    }

    /// Lazily resolve the static upcall target (runtime only — never a static initializer).
    private static MethodHandle completionThunkHandle() {
        try {
            return MethodHandles.lookup().findStatic(CATransaction.class, "completionThunk",
                    MethodType.methodType(void.class, MemorySegment.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("cannot resolve completion block target", e);
        }
    }

    /// Block body: pops one enqueued Runnable and runs it. STATIC and capture-free —
    /// the single upcall target behind every completion block, registered for AOT in NsuiFeature.
    /// Public because NsuiFeature (nsui.objc) resolves it at build time.
    public static void completionThunk(MemorySegment blockSelf) {
        Runnable body = PENDING.poll();
        if (body != null) {
            body.run();
        }
    }
}
