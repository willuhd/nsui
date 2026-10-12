package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentHashMap;

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

    /// Java completion bodies keyed by the address of the block literal installed
    /// for them.
    ///
    /// Each `setCompletionBlock(Runnable)` installs its OWN block, so the block the
    /// runtime calls back identifies which body to run. A single shared block
    /// cannot do that: at fire time two transactions are indistinguishable, and a
    /// single queue slot lets a second transaction's install destroy the first
    /// transaction's still-pending body (it would then run early, and the first
    /// would never run at all).
    ///
    /// Entries are removed when their body fires. QuartzCore delivers a replaced
    /// block as well as its replacement (see `setCompletionBlock` below), so the
    /// only entries that could outlive their block are ones whose transaction
    /// never drains before the process exits — bounded by the number of
    /// outstanding installs, the same trade-off NSWindow's SHEET_HANDLERS makes.
    private static final ConcurrentHashMap<Long, Runnable> BODIES = new ConcurrentHashMap<>();

    /// The static upcall target shared by every completion block, resolved once.
    private static volatile MethodHandle hThunk;

    /// The upcall STUB for that target, created once and reused by every completion
    /// block. The stub is per-target; only the 64-byte block literal has to be unique,
    /// because the block's address is the identity the thunk routes on.
    private static volatile MemorySegment thunkStub;

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
        if (hThunk == null) {
            thunkHandle();
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
    /// current transaction's animations finish.
    ///
    /// A fresh global block literal is installed for every call, so each body is
    /// bound to the transaction it was set on: two transactions (or two threads)
    /// each keep their own pending completion, and each body runs exactly once,
    /// with its own transaction.
    ///
    /// Setting the block again replaces the transaction property, and — per the
    /// QuartzCore header, "if no animations are added before the current
    /// transaction group is committed **or the completion block is set to a
    /// different value**, the block will be invoked immediately" — the replaced
    /// body is still delivered. Both bodies therefore run exactly once when the
    /// group drains, so a registered completion is never silently dropped.
    ///
    /// (The previous implementation installed one shared block, which made two
    /// installs the *same* property value: the replaced body was suppressed and,
    /// worse, a second transaction's install destroyed the first transaction's
    /// still-pending body.)
    ///
    /// The callback fires on the main thread once the runloop drains the commit.
    ///
    /// COST: each call installs its own 64-byte global-arena block (its address is how
    /// the callback is routed back to this body), so the block is never freed. The
    /// upcall stub and the Java target are shared, so this is one small immortal
    /// allocation per call — do not drive it from a per-frame loop if that matters;
    /// set one completion per animation batch instead.
    public static void setCompletionBlock(Runnable action) {
        ensureInit();
        MemorySegment block = MemorySegment.NULL;
        long key = 0L;
        try {
            if (action != null) {
                block = Blocks.blockWithStub(stubHandle());
                key = block.address();
                BODIES.put(key, action);
            }
            handles.hVoidId().invokeExact(ObjC.cls("CATransaction"),
                    ObjC.sel("setCompletionBlock:"), block);
        } catch (Throwable t) {
            if (key != 0L) {
                BODIES.remove(key);   // never leave a body whose block was never installed
            }
            throw new RuntimeException("setCompletionBlock: failed", t);
        }
    }

    /// +[CATransaction setCompletionBlock:] with a pre-built raw block literal
    /// (advanced use; must be a `void(^)(void)` global block).
    ///
    /// The raw block is installed verbatim — the toolkit does not route it — so it
    /// replaces any runnable body set on the same transaction, and a runnable set
    /// afterwards replaces it in turn, exactly as Core Animation does.
    public static void setCompletionBlock(MemorySegment rawBlock) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(ObjC.cls("CATransaction"),
                    ObjC.sel("setCompletionBlock:"),
                    (MemorySegment) (rawBlock == null ? MemorySegment.NULL : rawBlock));
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

    /// Number of completion bodies still awaiting their block (diagnostics/tests).
    ///
    /// CORRECTED: this used to say a superseded body "is never delivered and stays counted".
    /// Measured (setCompletionBlock(A) then setCompletionBlock(B) in one transaction): A runs
    /// IMMEDIATELY when it is replaced, B runs at commit, and the pending count is 0 afterwards —
    /// which is what setCompletionBlock's own javadoc already said. A replaced body is delivered
    /// (early), never dropped and never left counted.
    public static int pendingCompletionCount() {
        return BODIES.size();
    }

    /// The shared upcall stub, built once from the shared target.
    private static MemorySegment stubHandle() {
        MemorySegment st = thunkStub;
        if (st != null) return st;
        synchronized (CATransaction.class) {
            if (thunkStub == null) {
                thunkStub = Blocks.stub(thunkHandle(), NsuiForeign.blockVoidUpcall());
            }
            return thunkStub;
        }
    }

    /// Lazily resolve the static upcall target (runtime only — never a static
    /// initializer), caching it: every completion block shares the one target.
    private static MethodHandle thunkHandle() {
        MethodHandle h = hThunk;
        if (h != null) return h;
        synchronized (CATransaction.class) {
            if (hThunk == null) {
                try {
                    hThunk = MethodHandles.lookup().findStatic(CATransaction.class, "completionThunk",
                            MethodType.methodType(void.class, MemorySegment.class));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new IllegalStateException("cannot resolve completion block target", e);
                }
            }
            return hThunk;
        }
    }

    /// Block body: runs the Java body registered for the block the runtime called.
    /// STATIC and capture-free — the single upcall target behind every completion
    /// block, registered for AOT in NsuiFeature. Public because NsuiFeature
    /// (nsui.objc) resolves it at build time.
    ///
    /// The block pointer identifies the install, so two transactions can never
    /// take each other's body. A block with no registered body — a raw literal
    /// installed through the MemorySegment overload, or a manual call — is a no-op.
    ///
    /// Fail-fast by design: a throwing body aborts at the upcall stub instead of
    /// being swallowed — the same contract as DelegateProxy's callbacks
    /// (DelegateProxy.java:57-60) and Dispatch.onMain bodies (Dispatch.java:87).
    public static void completionThunk(MemorySegment blockSelf) {
        if (blockSelf == null || blockSelf.address() == 0) return;
        Runnable body = BODIES.remove(blockSelf.address());
        if (body != null) {
            body.run();
        }
    }
}
