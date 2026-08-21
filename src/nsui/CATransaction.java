package nsui;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import nsui.objc.Blocks;
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

    /// Lazily resolved upcall bridge: (blockSelf, Runnable) -> void, bound per call.
    private static volatile MethodHandle hCompletionBridge;

    private CATransaction() {}

        private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.VOID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
        );
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
    /// current transaction's animations finish. The block is a capture-less
    /// global block whose upcall target carries the Runnable as a bound argument,
    /// so it safely outlives this frame (same pattern as NSWindow sheet handlers).
    /// The callback fires on the main thread once the runloop drains the commit.
    public static void setCompletionBlock(Runnable action) {
        ensureInit();
        try {
            MemorySegment block;
            if (action == null) {
                block = MemorySegment.NULL;
            } else {
                MethodHandle bound = MethodHandles.insertArguments(completionBridge(), 1, action);
                block = Blocks.block(bound, FunctionDescriptor.ofVoid(ObjC.PTR));
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

    /// Lazily resolve the static upcall bridge (runtime only — never a static initializer).
    private static MethodHandle completionBridge() {
        MethodHandle h = hCompletionBridge;
        if (h != null) return h;
        synchronized (CATransaction.class) {
            if (hCompletionBridge == null) {
                try {
                    hCompletionBridge = MethodHandles.lookup().findStatic(CATransaction.class, "completionThunk",
                            MethodType.methodType(void.class, MemorySegment.class, Runnable.class));
                } catch (Throwable t) {
                    throw new RuntimeException("completion bridge resolve failed", t);
                }
            }
            return hCompletionBridge;
        }
    }

    /// Upcall target: the block pointer arrives first, then the bound Runnable.
    private static void completionThunk(MemorySegment blockSelf, Runnable action) {
        action.run();
    }
}
