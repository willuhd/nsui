package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;
import static nsui.objc.Sig.S;

/// Objective-C runtime + AppKit bindings built purely on the Java FFM API
/// (java.lang.foreign, JEP 454). No JNI, no JNA, no AWT/Swing/SWT, no third-party
/// libraries — every native call goes through `objc_msgSend` & friends,
/// resolved at runtime with `libraryLookup`.
///
/// Dispatch model — signature-keyed, resolve-once:
/// - `Sig` is the single source of truth: one `FunctionDescriptor`
///   per *signature* (return class + argument classes), shared by every
/// selector with that shape.
/// - `init` builds one downcall handle per vocabulary entry and binds
///   them to the typed `msgSend*` helpers below. Hot paths use the typed
/// helpers: a static `MethodHandle` + `invokeExact` — no map
/// lookup, no boxing, no per-call adaptation.
/// - New selectors with a known signature cost zero new code; an unknown
///   signature fails loudly with the exact vocabulary line to add — in both
/// JVM and AOT modes, so the closed world can never drift.
///
/// Native-image notes (GraalVM 25):
/// - Downcall/upcall handles MUST be created at run time — never in static
///   initializers — so everything is built by `init`, called from main().
/// - Nothing is linked at build time: libobjc, AppKit & friends are dlopen'ed
///   at runtime via their absolute paths (works through the dyld shared cache).
/// - By-value INPUT marshalling (`rect`, `cstring`) defaults to the thread-local
///   `Scratch` bump buffer (rewound at turn end), so application code outside
///   any turn no longer leaks immortal segments at 60fps.
/// - Struct RETURNS (`msgSendRect`, one 32-byte NSRect) land in a per-thread
///   reusable slot, not a fresh arena slice per call: every caller copies
///   the four doubles out immediately, so overwriting on the next call is
///   safe. The escape hatch still lands in `Arena.global()`. Selector/class
///   names are cached in the global arena (`sel`, `cls`).
public final class ObjC {

    // ---- canonical layouts (resolved at runtime to stay platform-correct) ----
    public static ValueLayout PTR;    // void*
    public static ValueLayout LONG;   // C long
    public static ValueLayout DOUBLE; // double
    public static ValueLayout BOOL;   // C _Bool -> Java boolean
    public static ValueLayout SIZE_T; // size_t
    public static ValueLayout INT;    // int
    /// NSRect == struct { CGFloat x, y, width, height } (4 doubles, passed by value).
    public static MemoryLayout NS_RECT;

    private static Linker LINKER;
    private static Arena ARENA;
    private static SymbolLookup OBJC;

    // ---- downcall handles: runtime C API (libobjc) ----
    private static MethodHandle hDlopen;
    private static MethodHandle hGetClass;      // objc_getClass(const char*) -> id
    private static MethodHandle hSelRegister;   // sel_registerName(const char*) -> SEL
    private static MethodHandle hAllocClassPair;
    private static MethodHandle hRegisterClassPair;
    private static MethodHandle hAddMethod;     // class_addMethod(Class, SEL, IMP, char*) -> bool
    private static MethodHandle hGetSuperclass; // class_getSuperclass(Class) -> Class
    private static MethodHandle hMsgSuper;      // objc_msgSendSuper(struct objc_super*, SEL) -> void

    // ---- downcall handles: one per vocabulary signature (built in init()) ----
    // ConcurrentHashMap (not HashMap): init() is guarded, but a concurrent map
    // makes even an off-protocol concurrent init race-free instead of risking
    // HashMap corruption mid-resize.
    private static final Map<S, MethodHandle> HANDLES = new ConcurrentHashMap<>();
    private static MethodHandle hId;          // (id, SEL) -> id
    private static MethodHandle hIdId;        // (id, SEL, id) -> id
    private static MethodHandle hId3;         // (id, SEL, id, id, id) -> id
    private static MethodHandle hIdRect;      // (id, SEL, NSRect, long, long, bool) -> id
    private static MethodHandle hIdEvent;     // (id, SEL, long, id, id, bool) -> id
    private static MethodHandle hIdDouble;    // (id, SEL, double) -> id
    private static MethodHandle hVoid;        // (id, SEL) -> void
    private static MethodHandle hVoidId;      // (id, SEL, id) -> void
    private static MethodHandle hVoidLong;    // (id, SEL, long) -> void
    private static MethodHandle hVoidBool;    // (id, SEL, bool) -> void
    private static MethodHandle hLong;        // (id, SEL) -> long
    private static MethodHandle hBool;        // (id, SEL) -> bool
    private static MethodHandle hShort;       // (id, SEL) -> short
    private static MethodHandle hRect;        // (id, SEL) -> NSRect (objc_msgSend_stret on x86_64)
    private static MethodHandle hEdgeInsets;  // (id, SEL) -> NSEdgeInsets (32 bytes, same stret class as NSRect)
    private static MethodHandle hVoidEdgeInsets; // (id, SEL, NSEdgeInsets) -> void
    private static MethodHandle hEscapeId;    // (id, SEL, id x6) -> id
    private static MethodHandle hEscapeVoid;  // (id, SEL, id x6) -> void

    /// Reusable per-thread destination for ALL struct returns up to 32 bytes
    /// (RECT and EDGEINSETS 32, POINT/SIZE/RANGE 16). Lazily allocated from the global arena
    /// on first use per thread; never rewound, just overwritten by the next
    /// struct call on the same thread. Safe because every caller copies the
    /// values out immediately — never hold the segment across another call
    /// (see contentOriginOffsetY for the one place that order matters).
    private static final ThreadLocal<MemorySegment> RECT_SLOT =
            ThreadLocal.withInitial(() -> ARENA.allocate(NS_RECT));

    /// The shared struct-return slot as an allocator. Replaces per-call
    /// `Arena.global()` at every struct-return downcall site.
    public static SegmentAllocator structSlot() {
        return (SegmentAllocator) RECT_SLOT.get();
    }

    private ObjC() {}

    private static volatile boolean INIT;

    /// True while init() is building the tables below. The fail-fast guards in
    /// handle()/sel()/cls() allow calls from inside init() itself.
    private static volatile boolean INITIALIZING;

    /// Must run at RUNTIME, from main() — not from a static initializer (native-image rule).
    /// Synchronized + idempotent like every other ensureInit in this package;
    /// re-entry (EdgeTest double-init) is a harmless no-op.
    public static synchronized void init() {
        if (INIT) return;
        INITIALIZING = true;
        try {
        LINKER = Linker.nativeLinker();
        PTR = (ValueLayout) LINKER.canonicalLayouts().get("void*");
        LONG = (ValueLayout) LINKER.canonicalLayouts().get("long");
        DOUBLE = (ValueLayout) LINKER.canonicalLayouts().get("double");
        BOOL = (ValueLayout) LINKER.canonicalLayouts().get("bool");
        SIZE_T = (ValueLayout) LINKER.canonicalLayouts().get("size_t");
        INT = (ValueLayout) LINKER.canonicalLayouts().get("int");
        NS_RECT = MemoryLayout.structLayout(DOUBLE, DOUBLE, DOUBLE, DOUBLE);
        ARENA = Arena.global();

        SymbolLookup sys = SymbolLookup.libraryLookup("/usr/lib/libSystem.B.dylib", ARENA);
        hDlopen = down(sys, "dlopen", NsuiForeign.dlopen());

        // Explicitly load the frameworks: the ObjC runtime does NOT auto-load AppKit,
        // and objc_getClass returns NULL for classes in unloaded images.
        ensureFramework("AppKit");
        ensureFramework("Foundation");
        ensureFramework("CoreGraphics");
        ensureFramework("CoreFoundation");

        OBJC = SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", ARENA);
        hGetClass = down(OBJC, "objc_getClass", NsuiForeign.objcGetClass());
        hSelRegister = down(OBJC, "sel_registerName", NsuiForeign.selRegisterName());
        hAllocClassPair = down(OBJC, "objc_allocateClassPair", NsuiForeign.allocateClassPair());
        hRegisterClassPair = down(OBJC, "objc_registerClassPair", NsuiForeign.registerClassPair());
        hAddMethod = down(OBJC, "class_addMethod", NsuiForeign.addMethod());
        hGetSuperclass = down(OBJC, "class_getSuperclass", NsuiForeign.classGetSuperclass());
        hMsgSuper = down(OBJC, "objc_msgSendSuper", NsuiForeign.msgSendSuperVoid());

        // One downcall handle per vocabulary signature — the entire msgSend surface.
        for (S s : Sig.VOCABULARY) {
            HANDLES.put(s, down(OBJC, Sig.msgSendSymbol(s.ret()), s.descriptor()));
        }
        hId = handle(Sig.of(Ret.ID));
        hIdId = handle(Sig.of(Ret.ID, Arg.ID));
        hId3 = handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID));
        hIdRect = handle(Sig.of(Ret.ID, Arg.RECT, Arg.INT, Arg.INT, Arg.BOOL));
        hIdEvent = handle(Sig.of(Ret.ID, Arg.INT, Arg.ID, Arg.ID, Arg.BOOL));
        hIdDouble = handle(Sig.of(Ret.ID, Arg.DOUBLE));
        hVoid = handle(Sig.of(Ret.VOID));
        hVoidId = handle(Sig.of(Ret.VOID, Arg.ID));
        hVoidLong = handle(Sig.of(Ret.VOID, Arg.INT));
        hVoidBool = handle(Sig.of(Ret.VOID, Arg.BOOL));
        hLong = handle(Sig.of(Ret.INT));
        hBool = handle(Sig.of(Ret.BOOL));
        hShort = handle(Sig.of(Ret.SHORT));
        hRect = handle(Sig.of(Ret.RECT));
        hEdgeInsets = handle(Sig.of(Ret.EDGEINSETS));
        hVoidEdgeInsets = handle(Sig.of(Ret.VOID, Arg.EDGEINSETS));
        hEscapeId = handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID));
        hEscapeVoid = handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID));
        STRING_CLS = cls("NSString");
        STRING_WITH_UTF8_SEL = sel("stringWithUTF8String:");
        UTF8STRING_SEL = sel("UTF8String");
        INIT = true;
        } finally {
            INITIALIZING = false;
        }
    }

    /// The downcall handle for a vocabulary signature. Fails loudly when the signature
    /// is missing — in BOTH JVM and AOT modes, so the vocabulary (the single source of
    /// truth for registration) can never drift from what the code actually sends.
    public static MethodHandle handle(S s) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        MethodHandle h = HANDLES.get(s);
        if (h == null) {
            throw new IllegalStateException("message signature not in the vocabulary: " + s.shape()
                    + " — add \"of(" + s.ret().name() + ", ...)\" to Sig.VOCABULARY (single source of truth for AOT registration)");
        }
        return h;
    }

    // ------------------------------------------------------------------ runtime

    /// dlopen() a system framework so its classes become visible to the ObjC runtime.
    public static void ensureFramework(String name) {
        String path = "/System/Library/Frameworks/" + name + ".framework/" + name;
        MemorySegment h = (MemorySegment) invokeX(hDlopen, cstring(path), 0x2 /* RTLD_NOW */ | 0x8 /* RTLD_GLOBAL */);
        if (h.address() == 0) {
            throw new IllegalStateException("dlopen failed for " + path);
        }
    }

    /// Selector/class name pointers are cached in the IMMORTAL arena (never scratch)
    /// because a cached pointer must stay valid across turns.

    /// NUL-terminated C string for call-scoped use. The bytes live in the
    /// thread-local bump buffer (`Scratch.allocInput`) — valid only until the
    /// next buffer rewind, so the callee MUST copy before returning (all
    /// current callees do). Prefer the cached paths (`sel`, `cls`) for
    /// selector/class names that repeat.
    public static MemorySegment cstring(String s) {
        if (s.indexOf(0) >= 0) {
            throw new IllegalArgumentException("cstring must not contain a NUL byte");
        }
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MemorySegment seg = Scratch.allocInput(bytes.length + 1);
        MemorySegment.copy(bytes, 0, seg, ValueLayout.JAVA_BYTE, 0, bytes.length);
        seg.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
        return seg;
    }

    /// Global-arena cstring used by the SEL/CLASS caches (safe to hold forever).
    private static MemorySegment globalCstring(String s) {
        return ARENA.allocateFrom(s);
    }

    /// Registered SEL / class pointer per distinct name, forever. `sel_registerName`
    /// is idempotent (same name always yields the same SEL) and classes live for
    /// the process (nothing here is ever unloaded), so caching the RESULT — not
    /// just the cstring input — collapses every repeat call to one map lookup:
    /// no native transition, no invokeX boxing. Bounded by distinct names.
    private static final ConcurrentHashMap<String, MemorySegment> SEL_RESULT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MemorySegment> CLASS_RESULT = new ConcurrentHashMap<>();

    /// Fixed lookups ObjC's own hot paths make (nsstring/toString), resolved once in
    /// init() so they too skip the per-call map hit.
    private static MemorySegment STRING_CLS;
    private static MemorySegment STRING_WITH_UTF8_SEL;
    private static MemorySegment UTF8STRING_SEL;

    /// objc_getClass(name) — cached per distinct name. Steady state is a plain map
    /// get (a name repeats forever once seen); only a miss takes the atomic path and
    /// allocates the immortal cstring. NULL (address 0, framework not loaded) is
    /// never cached: a later dlopen can make the class visible, and caching NULL
    /// would pin the miss. The hit path is unchanged (one map get).
    public static MemorySegment cls(String name) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        MemorySegment c = CLASS_RESULT.get(name);
        if (c != null) return c;
        MemorySegment fresh = (MemorySegment) invokeX(hGetClass, globalCstring(name));
        if (fresh == null || fresh.address() == 0) return fresh;
        MemorySegment prev = CLASS_RESULT.putIfAbsent(name, fresh);
        return prev != null ? prev : fresh;
    }

    /// sel_registerName(name) — cached per distinct name. See cls() for the fast path.
    public static MemorySegment sel(String name) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        MemorySegment s = SEL_RESULT.get(name);
        if (s != null) return s;
        return SEL_RESULT.computeIfAbsent(name,
                n -> (MemorySegment) invokeX(hSelRegister, globalCstring(n)));
    }

    /// True when the calling thread is AppKit's main thread ([NSThread isMainThread]).
    /// Uses the existing (BOOL) vocabulary shape — no new signature needed.
    public static boolean isMainThread() {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try {
            return (boolean) handle(Sig.of(Ret.BOOL)).invokeExact(cls("NSThread"), sel("isMainThread"));
        } catch (Throwable t) {
            throw fail(t);
        }
    }

    /// Fail-fast main-thread guard for AppKit entry points that must run on the
    /// main thread (window creation, run-loop control). AppKit cannot move an
    /// off-main call onto the main thread after the fact, so checking here is
    /// the guard: offenders get an IllegalStateException naming `what` instead
    /// of undefined native behavior.
    public static void requireMainThread(String what) {
        if (!isMainThread()) {
            throw new IllegalStateException(what + " must run on the main thread"
                    + " (AppKit is not thread-safe; dispatch via Dispatch.onMain and pump the run loop)");
        }
    }

    /// NSString from a Java string ([NSString stringWithUTF8String:]). Typed
    /// invokeExact (not boxed invokeX): this sits on every setter hot path.
    public static MemorySegment nsstring(String s) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try {
            return (MemorySegment) hIdId.invokeExact(STRING_CLS, STRING_WITH_UTF8_SEL, cstring(s));
        } catch (Throwable t) {
            throw fail(t);
        }
    }

    /// NSString -> Java String (via [NSString UTF8String]) — safe for arbitrary length.
    /// Scans for the NUL terminator in 4096-byte windows (one reinterpret per
    /// window, not per byte — a 500-char string costs 1), then copies exactly
    /// the string bytes. Past the 16M cap the string truncates (a split
    /// multi-byte tail decodes with a trailing replacement char) instead of
    /// looping forever or throwing: getString would scan past the region.
    public static String toString(MemorySegment nsString) {
        if (nsString == null || nsString.address() == 0) return null;
        MemorySegment c = msgSendId(nsString, UTF8STRING_SEL);
        if (c.address() == 0) return null;
        long len = -1;
        long base = 0;
        while (true) {
            MemorySegment w = c.reinterpret(base + 4096);
            for (long i = 0; i < 4096; i++) {
                if (w.get(ValueLayout.JAVA_BYTE, base + i) == 0) { len = base + i; break; }
            }
            if (len >= 0) break;
            base += 4096;
            if (base >= 16_000_000) { len = 16_000_000; break; }
        }
        byte[] bytes = c.reinterpret(len).toArray(ValueLayout.JAVA_BYTE);
        return new String(bytes, 0, (int) len, java.nio.charset.StandardCharsets.UTF_8);
    }

    /// alloc + initWithFrame: for an AppKit view class, by name — the one-line
    /// replacement for the create() ceremony every view wrapper repeats. Uses
    /// the shared registered (ID, RECT) -> ID handle: same stub, same cost as
    /// a per-class handle, minus ~8 lines per call site. Throws when native
    /// construction fails or returns nil (never returns a null peer).
    public static MemorySegment newView(String className, nsui.NSRect frame) {
        MemorySegment p = msgSendId(cls(className), sel("alloc"));
        try {
            p = (MemorySegment) handle(Sig.of(Ret.ID, Arg.RECT))
                    .invokeExact(p, sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for " + className, t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithFrame: returned nil for " + className);
        return p;
    }

    /// Allocate an NSRect for call-scoped use. Always a by-value INPUT argument
    /// (the callee reads it during the call), so it comes from the thread-local
    /// bump buffer (`Scratch.allocInput`); struct RETURNS (`msgSendRect`) use
    /// the separate reusable slot, never scratch.
    public static MemorySegment rect(double x, double y, double w, double h) {
        MemorySegment r = Scratch.allocInput(NS_RECT.byteSize());
        r.set(ValueLayout.JAVA_DOUBLE, 0, x);
        r.set(ValueLayout.JAVA_DOUBLE, 8, y);
        r.set(ValueLayout.JAVA_DOUBLE, 16, w);
        r.set(ValueLayout.JAVA_DOUBLE, 24, h);
        return r;
    }

    public static double rectX(MemorySegment r) { return r.get(ValueLayout.JAVA_DOUBLE, 0); }
    public static double rectY(MemorySegment r) { return r.get(ValueLayout.JAVA_DOUBLE, 8); }
    public static double rectW(MemorySegment r) { return r.get(ValueLayout.JAVA_DOUBLE, 16); }
    public static double rectH(MemorySegment r) { return r.get(ValueLayout.JAVA_DOUBLE, 24); }

    // ------------------------------------------------------- message dispatch
    // Typed helpers over the vocabulary handles. invokeExact: direct stub call,
    // no boxing, no adaptation — the steady-state cost of every message.

    public static MemorySegment msgSendId(MemorySegment recv, MemorySegment s) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { return (MemorySegment) hId.invokeExact(recv, s); } catch (Throwable t) { throw fail(t); }
    }

    public static MemorySegment msgSendIdId(MemorySegment recv, MemorySegment s, MemorySegment a1) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { return (MemorySegment) hIdId.invokeExact(recv, s, a1); } catch (Throwable t) { throw fail(t); }
    }

    public static MemorySegment msgSendIdIdSelId(MemorySegment recv, MemorySegment s, MemorySegment a1, MemorySegment a2, MemorySegment a3) {
        try { return (MemorySegment) hId3.invokeExact(recv, s, a1, a2, a3); } catch (Throwable t) { throw fail(t); }
    }

    public static MemorySegment msgSendIdRectLongLongBool(MemorySegment recv, MemorySegment s,
            MemorySegment rect, long styleMask, long backing, boolean defer) {
        try { return (MemorySegment) hIdRect.invokeExact(recv, s, rect, styleMask, backing, defer); } catch (Throwable t) { throw fail(t); }
    }

    public static MemorySegment msgSendIdLongIdIdBool(MemorySegment recv, MemorySegment s,
            long mask, MemorySegment until, MemorySegment mode, boolean dequeue) {
        try { return (MemorySegment) hIdEvent.invokeExact(recv, s, mask, until, mode, dequeue); } catch (Throwable t) { throw fail(t); }
    }

    public static MemorySegment msgSendIdDouble(MemorySegment recv, MemorySegment s, double d) {
        try { return (MemorySegment) hIdDouble.invokeExact(recv, s, d); } catch (Throwable t) { throw fail(t); }
    }

    public static void msgSendVoid(MemorySegment recv, MemorySegment s) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { hVoid.invokeExact(recv, s); } catch (Throwable t) { throw fail(t); }
    }

    public static void msgSendVoidId(MemorySegment recv, MemorySegment s, MemorySegment a1) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { hVoidId.invokeExact(recv, s, a1); } catch (Throwable t) { throw fail(t); }
    }

    public static void msgSendVoidLong(MemorySegment recv, MemorySegment s, long a1) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { hVoidLong.invokeExact(recv, s, a1); } catch (Throwable t) { throw fail(t); }
    }

    public static void msgSendVoidBool(MemorySegment recv, MemorySegment s, boolean a1) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { hVoidBool.invokeExact(recv, s, a1); } catch (Throwable t) { throw fail(t); }
    }

    public static long msgSendLong(MemorySegment recv, MemorySegment s) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { return (long) hLong.invokeExact(recv, s); } catch (Throwable t) { throw fail(t); }
    }

    public static boolean msgSendBool(MemorySegment recv, MemorySegment s) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { return (boolean) hBool.invokeExact(recv, s); } catch (Throwable t) { throw fail(t); }
    }

    public static short msgSendShort(MemorySegment recv, MemorySegment s) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { return (short) hShort.invokeExact(recv, s); } catch (Throwable t) { throw fail(t); }
    }

    /// Struct-returning message (NSRect); uses objc_msgSend_stret on x86_64.
    /// FFM gives downcalls with group-layout returns an implicit leading
    /// SegmentAllocator parameter, which is where the returned struct is written.
    public static MemorySegment msgSendRect(MemorySegment recv, MemorySegment s) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { return (MemorySegment) hRect.invokeExact((SegmentAllocator) RECT_SLOT.get(), recv, s); } catch (Throwable t) { throw fail(t); }
    }

    /// Struct-returning message (NSEdgeInsets, 32 bytes — same stret class as NSRect).
    public static MemorySegment msgSendEdgeInsets(MemorySegment recv, MemorySegment s) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { return (MemorySegment) hEdgeInsets.invokeExact((SegmentAllocator) RECT_SLOT.get(), recv, s); } catch (Throwable t) { throw fail(t); }
    }

    /// Struct-argument message (NSEdgeInsets by value).
    public static void msgSendVoidEdgeInsets(MemorySegment recv, MemorySegment s, MemorySegment insets) {
        if (!INIT && !INITIALIZING) throw new IllegalStateException("ObjC.init() must run first (from main, at runtime)");
        try { hVoidEdgeInsets.invokeExact(recv, s, insets); } catch (Throwable t) { throw fail(t); }
    }

    /// Generic object-argument message: any selector whose arguments are all objects
    /// (id/SEL/pointers), NULL-padded to the fixed 6-arg descriptor. The AOT-safe
    /// escape hatch for selectors whose exact signature is not in the vocabulary.
    public static MemorySegment invoke(MemorySegment recv, MemorySegment sel, MemorySegment... args) {
        if (args.length > 6) {
            throw new IllegalArgumentException("escape hatch supports up to 6 object args, got " + args.length);
        }
        MemorySegment a0 = args.length > 0 ? args[0] : MemorySegment.NULL;
        MemorySegment a1 = args.length > 1 ? args[1] : MemorySegment.NULL;
        MemorySegment a2 = args.length > 2 ? args[2] : MemorySegment.NULL;
        MemorySegment a3 = args.length > 3 ? args[3] : MemorySegment.NULL;
        MemorySegment a4 = args.length > 4 ? args[4] : MemorySegment.NULL;
        MemorySegment a5 = args.length > 5 ? args[5] : MemorySegment.NULL;
        try {
            return (MemorySegment) hEscapeId.invokeExact(recv, sel, a0, a1, a2, a3, a4, a5);
        } catch (Throwable t) {
            throw fail(t);
        }
    }

    /// Void-returning variant of `invoke`.
    public static void invokeVoid(MemorySegment recv, MemorySegment sel, MemorySegment... args) {
        if (args.length > 6) {
            throw new IllegalArgumentException("escape hatch supports up to 6 object args, got " + args.length);
        }
        MemorySegment a0 = args.length > 0 ? args[0] : MemorySegment.NULL;
        MemorySegment a1 = args.length > 1 ? args[1] : MemorySegment.NULL;
        MemorySegment a2 = args.length > 2 ? args[2] : MemorySegment.NULL;
        MemorySegment a3 = args.length > 3 ? args[3] : MemorySegment.NULL;
        MemorySegment a4 = args.length > 4 ? args[4] : MemorySegment.NULL;
        MemorySegment a5 = args.length > 5 ? args[5] : MemorySegment.NULL;
        try {
            hEscapeVoid.invokeExact(recv, sel, a0, a1, a2, a3, a4, a5);
        } catch (Throwable t) {
            throw fail(t);
        }
    }

    // ------------------------------------------------------- class pair + upcalls

    /// objc_allocateClassPair + objc_registerClassPair.
    public static MemorySegment makeClass(String superClassName, String className) {
        MemorySegment c = (MemorySegment) invokeX(hAllocClassPair, cls(superClassName), cstring(className), 0L);
        if (c.address() == 0) throw new IllegalStateException("objc_allocateClassPair failed");
        invokeX(hRegisterClassPair, c);
        return c;
    }

    /// class_addMethod — installs a Java method (via an FFM upcall stub) as an ObjC method.
    public static boolean addMethod(MemorySegment cls, String selector, MemorySegment imp, String types) {
        return (boolean) invokeX(hAddMethod, cls, sel(selector), imp, cstring(types));
    }

    /// FFM upcall stub: a real C function pointer that calls back into Java.
    public static MemorySegment upcall(MethodHandle target, FunctionDescriptor descriptor) {
        return LINKER.upcallStub(target, descriptor, ARENA);
    }

    /// class_getSuperclass(cls).
    public static MemorySegment classGetSuperclass(MemorySegment cls) {
        try { return (MemorySegment) hGetSuperclass.invokeExact(cls); } catch (Throwable t) { throw fail(t); }
    }

    /// Allocate a `struct objc_super { id receiver; Class super_class;`} — the
    /// argument `objc_msgSendSuper` needs for `[super ...]`. Call-scoped bump
    /// (dealloc-only callers consume it synchronously), not immortal.
    public static MemorySegment superStruct(MemorySegment receiver, MemorySegment superClass) {
        MemorySegment s = Scratch.allocInput(16);
        s.set(ValueLayout.ADDRESS, 0, receiver);
        s.set(ValueLayout.ADDRESS, 8, superClass);
        return s;
    }

    /// objc_msgSendSuper(superStruct, SEL) — dispatch to the receiver's superclass.
    public static void msgSendSuperVoid(MemorySegment superStruct, MemorySegment sel) {
        try { hMsgSuper.invokeExact(superStruct, sel); } catch (Throwable t) { throw fail(t); }
    }

    // ------------------------------------------------------------------ helpers

    private static MethodHandle down(SymbolLookup lookup, String name, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(lookup.find(name).orElseThrow(() -> new IllegalStateException("symbol not found: " + name)), descriptor);
    }

    private static Object invokeX(MethodHandle h, Object... args) {
        try {
            return h.invokeWithArguments(args);
        } catch (Throwable t) {
            throw fail(t);
        }
    }

    /// Helper for nullable peers: returns `MemorySegment.NULL` for null or nil (address 0).
    public static MemorySegment nullable(MemorySegment seg) {
        return (seg == null || seg.address() == 0) ? MemorySegment.NULL : seg;
    }

    /// Helper for nullable NSObject peers.
    public static MemorySegment nullablePeer(nsui.NSObject obj) {
        return obj == null ? MemorySegment.NULL : nullable(obj.peer());
    }

    private static RuntimeException fail(Throwable t) {
        return new RuntimeException("native call failed", t);
    }
}
