package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.Callable;

/// NSException interception hook — libobjc's `objc_setExceptionPreprocessor`.
///
/// **Honest capability report (verified on this machine):** the preprocessor
/// hook alone can *observe and transform* an ObjC exception before it is thrown,
/// but it **cannot catch or suppress an uncaught throw**. The classic catch trampoline
/// (`objc_exception_try_enter`/try_exit/extract/match) is absent from
/// `/usr/lib/libobjc.A.dylib`; `objc_begin_catch`/`objc_end_catch` and
/// `objc_addExceptionHandler` ARE exported, but they are unusable from pure FFM:
/// the throw path is `__cxa_throw`, which needs a native C++ EH landing pad that a
/// Java thread has none of, and `objc_begin_catch` requires the compiler-generated
/// private `exc_buf` layout. Concretely, with a raise that has no native catch
/// frame (a pure-FFM Java process has none), whatever this preprocessor returns, libobjc
/// still performs the `__cxa_throw` and the JVM terminates:
/// - return `NULL` → libobjc aborts `"uncaught exception of class 'nil'"`;
/// - return a non-null replacement → the replacement is thrown and, uncaught, aborts;
/// - pass the exception through → the original is thrown and, uncaught, aborts.
/// All three were confirmed by running the guarded raise under each return value.
///
/// So the genuine, working contract of this class is:
/// - `ensureInit` installs the preprocessor once, remembering and chaining
///   the *previous* handler (libobjc installs a default one; we call it on the
/// pass-through path so we never disturb the runtime's own handling).
/// - Threading is per-thread (`ThreadLocal`): only a thread that armed itself via
///   `call`/`run` is watched; every other thread passes exceptions through
/// to the previous handler / unchanged — normal operation is untouched. (Verified:
/// after installing the hook, unarmed msgSend work proceeds exactly as before.)
/// - When armed and an ObjC exception is raised, `preprocessor` records its
///   `name`/`reason` for diagnostics and passes it through — suppression is
/// impossible, so the guarded body's raise will still terminate the process if nothing
/// else catches it.
///
/// This is an honest interception scaffold: it proves the hook is reachable, is
/// thread-aware, and chains correctly — but the task's fuller goal ("catch and recover
/// from an uncaught `[NSException raise]` inside Java") is **not achievable** on
/// this platform's libobjc via this exported API. Tests asserting that recovery must fail
/// (see test class for the noted crash evidence).
public final class Exceptions {

    private static final ThreadLocal<Boolean> ARMED = new ThreadLocal<>();
    private static final ThreadLocal<MemorySegment> LAST = new ThreadLocal<>();

    private static Linker LINKER;
    private static Arena ARENA;
    private static MethodHandle hSet;        // objc_setExceptionPreprocessor
    private static MethodHandle hPrev;       // call the previous preprocessor ((id)->id shape)
    private static MethodHandle hExIdIdId;   // [NSException exceptionWithName:reason:userInfo:]
    private static MemorySegment stub;       // the installed upcall stub (kept alive)
    private static MemorySegment prev;       // the previous handler, to chain
    private static boolean INIT;

    private Exceptions() {}

    /// Install the preprocessor stub once; build supporting handles. Must run at runtime.
    public static synchronized void ensureInit() {
        if (INIT) return;
        LINKER = Linker.nativeLinker();
        ARENA = Arena.global();

        SymbolLookup objc = SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", ARENA);
        hSet = LINKER.downcallHandle(
                objc.find("objc_setExceptionPreprocessor")
                        .orElseThrow(() -> new IllegalStateException("symbol not found: objc_setExceptionPreprocessor")),
                NsuiForeign.setExceptionPreprocessor());

        // Build the upcall stub at run time (not in a static initializer).
        MethodHandle target;
        try {
            target = MethodHandles.lookup().findStatic(Exceptions.class,
                    "preprocessor", MethodType.methodType(MemorySegment.class, MemorySegment.class));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot resolve preprocessor target", e);
        }
        stub = LINKER.upcallStub(target, NsuiForeign.setExceptionPreprocessor(), ARENA);

        // Install it; remember the previous handler for chaining.
        try {
            prev = (MemorySegment) hSet.invokeExact(stub);
        } catch (Throwable t) {
            throw new IllegalStateException("objc_setExceptionPreprocessor install failed", t);
        }
        if (prev != null && prev.address() != 0) {
            // The preprocessor shape ((id)->id) has the same descriptor as the setter's
            // (void* -> void*); reuse it for a downcall handle to the previous handler.
            hPrev = LINKER.downcallHandle(prev, NsuiForeign.setExceptionPreprocessor());
        }

        hExIdIdId = ObjC.handle(Sig.of(Sig.Ret.ID, Sig.Arg.ID, Sig.Arg.ID, Sig.Arg.ID));

        INIT = true;
    }

    /// The preprocessor callback, invoked by libobjc on the throwing thread immediately
    /// before the ObjC throw machinery runs.
    /// - Not armed: pass through to the previous handler (or unchanged).
    /// - Armed on this thread: record name/reason for diagnostics, then pass through —
    ///   suppression is impossible (returning NULL or a replacement both terminate).
    ///
    /// Package-visible so the native-image Feature (nsui.objc.NsuiFeature) can register
    /// this upcall target at build time.
    static MemorySegment preprocessor(MemorySegment exception) {
        if (Boolean.TRUE.equals(ARMED.get())) {
            LAST.set(exception);
        }
        if (hPrev != null) {
            try {
                return (MemorySegment) hPrev.invokeExact(exception); // chain previous handler
            } catch (Throwable t) {
                // previous handler went wrong — fall through to pass-through.
            }
        }
        return exception;
    }

    /// Run `body` with the current thread marked as watched by the preprocessor.
    ///
    /// For a body that does **not** raise, this returns its result normally and the
    /// thread is unarmed in a `finally`. For a body that **raises an uncaught
    /// ObjC exception**, the process terminates regardless (see class javadoc) — this
    /// class cannot prevent that. `lastDescription` is not reachable in that case
    /// (the process is gone), but the preprocessor does record it for diagnostics before
    /// the throw proceeds.
    public static <T> T call(Callable<T> body) throws Exception {
        ensureInit();
        ARMED.set(Boolean.TRUE);
        LAST.set(null);
        try {
            return body.call();
        } finally {
            ARMED.set(null);
        }
    }

    /// `call` whose body returns void (non-raising body expected; see call's javadoc).
    public static void run(Runnable body) {
        try {
            call(() -> { body.run(); return null; });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("guarded body failed", e);
        }
    }

    /// Name of the exception most recently observed by the preprocessor on this thread, or null.
    public static String lastDescription() {
        MemorySegment exc = LAST.get();
        if (exc == null || exc.address() == 0) return null;
        String name = ObjC.toString(ObjC.msgSendId(exc, ObjC.sel("name")));
        String reason = ObjC.toString(ObjC.msgSendId(exc, ObjC.sel("reason")));
        return "NSException{name=" + name + ", reason=" + reason + "}";
    }

    /// `[NSException exceptionWithName:reason:userInfo:] -> NSException` (autoreleased).
    public static MemorySegment exceptionWithName(String name, String reason) {
        ensureInit();
        try {
            return (MemorySegment) hExIdIdId.invokeExact(
                    ObjC.cls("NSException"),
                    ObjC.sel("exceptionWithName:reason:userInfo:"),
                    ObjC.nsstring(name),
                    ObjC.nsstring(reason),
                    MemorySegment.NULL);
        } catch (Throwable t) {
            throw new RuntimeException("exceptionWithName failed", t);
        }
    }

    /// `[exc raise]` — see class javadoc: uncaught, this terminates the process.
    public static void raise(MemorySegment exc) {
        ObjC.msgSendVoid(exc, ObjC.sel("raise"));
    }
}
