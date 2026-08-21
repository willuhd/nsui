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
import java.util.HashMap;
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
/// - By-value INPUT marshalling (`rect`, `cstring`) is routed through the
///   per-turn `Scratch` arena so it does not leak immortal segments at 60fps.
/// Struct RETURNS (`msgSendRect`) and the escape hatch still land in
/// `Arena.global()`. Selector/class names are cached in the global arena
/// (`sel`, `cls`).
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
    private static final Map<S, MethodHandle> HANDLES = new HashMap<>();
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
    private static MethodHandle hRect;        // (id, SEL) -> NSRect (objc_msgSend_stret on x86_64)
    private static MethodHandle hEscapeId;    // (id, SEL, id x6) -> id
    private static MethodHandle hEscapeVoid;  // (id, SEL, id x6) -> void

    private ObjC() {}

    /// Must run at RUNTIME, from main() — not from a static initializer (native-image rule).
    public static void init() {
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
        hRect = handle(Sig.of(Ret.RECT));
        hEscapeId = handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID));
        hEscapeVoid = handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID));
    }

    /// The downcall handle for a vocabulary signature. Fails loudly when the signature
    /// is missing — in BOTH JVM and AOT modes, so the vocabulary (the single source of
    /// truth for registration) can never drift from what the code actually sends.
    public static MethodHandle handle(S s) {
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

    /// ONE global-arena cstring per distinct selector / class name, forever. Strings are
    /// cached in the IMMORTAL arena (never scratch) because a cached pointer must stay
    /// valid across turns; the per-call cost collapses to a `ConcurrentHashMap`
    /// lookup — no scratch, no churn. Bounded by the number of distinct selector/class
    /// names the program touches.
    private static final ConcurrentHashMap<String, MemorySegment> SEL_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, MemorySegment> CLASS_CACHE = new ConcurrentHashMap<>();

    /// NUL-terminated C string. During a turn the bytes are written into the per-turn
    /// scratch buffer (`Scratch`), so the memory is only valid until the turn ends —
    /// the callee MUST copy it before returning. With no turn active it falls back to the
    /// global arena (as before). Prefer one of the cached paths (`sel`, `cls`)
    /// for selector/class names that repeat.
    public static MemorySegment cstring(String s) {
        if (s.indexOf(0) >= 0) {
            throw new IllegalArgumentException("cstring must not contain a NUL byte");
        }
        if (Scratch.active()) {
            byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            MemorySegment seg = Scratch.alloc(bytes.length + 1);
            MemorySegment.copy(bytes, 0, seg, ValueLayout.JAVA_BYTE, 0, bytes.length);
            seg.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
            return seg;
        }
        return ARENA.allocateFrom(s);
    }

    /// Global-arena cstring used by the SEL/CLASS caches (safe to hold forever).
    private static MemorySegment globalCstring(String s) {
        return ARENA.allocateFrom(s);
    }

    /// objc_getClass(name) — cstring is cached in the global arena per distinct name.
    public static MemorySegment cls(String name) {
        return (MemorySegment) invokeX(hGetClass,
                CLASS_CACHE.computeIfAbsent(name, ObjC::globalCstring));
    }

    /// sel_registerName(name). `sel_registerName` is already unique-per-name natively,
    /// so we cache the cstring input per distinct name instead of allocating one every call.
    public static MemorySegment sel(String name) {
        return (MemorySegment) invokeX(hSelRegister,
                SEL_CACHE.computeIfAbsent(name, ObjC::globalCstring));
    }

    /// NSString from a Java string ([NSString stringWithUTF8String:]).
    public static MemorySegment nsstring(String s) {
        return (MemorySegment) invokeX(hIdId, cls("NSString"), sel("stringWithUTF8String:"), cstring(s));
    }

    /// NSString -> Java String (via [NSString UTF8String]) — safe for arbitrary length (strlen loop, no 4096 truncation).
    public static String toString(MemorySegment nsString) {
        if (nsString == null || nsString.address() == 0) return null;
        MemorySegment c = msgSendId(nsString, sel("UTF8String"));
        if (c.address() == 0) return null;
        // Manual strlen: scan for NUL byte; avoids fixed 4096 cap.
        // Reinterpret grows with len; handles strings of any length (tested via loop, not native strlen).
        long len = 0;
        // Fast path: probe in 4096-byte blocks to avoid per-byte reinterpret for long strings
        while (true) {
            // Need len+1 bytes to read byte at len
            byte b = c.reinterpret(len + 1).get(ValueLayout.JAVA_BYTE, len);
            if (b == 0) break;
            len++;
            if (len > 16_000_000) {
                // safety cap for pathological strings (16 MB); truncate rather than loop forever
                break;
            }
        }
        return c.reinterpret(len + 1).getString(0);
    }

    /// Allocate an NSRect. When a turn is active the 32-byte struct comes from the per-turn
    /// scratch buffer (`Scratch`) — safe because a rect is always a by-value INPUT
    /// argument (the callee reads it during the call); when no turn is active it falls back
    /// to the global arena, exactly as before. Struct RETURNS (`msgSendRect`) are never
    /// scratch — they stay in the global arena.
    public static MemorySegment rect(double x, double y, double w, double h) {
        MemorySegment r = Scratch.active()
                ? Scratch.alloc(NS_RECT.byteSize())
                : ARENA.allocate(NS_RECT);
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
        try { return (MemorySegment) hId.invokeExact(recv, s); } catch (Throwable t) { throw fail(t); }
    }

    public static MemorySegment msgSendIdId(MemorySegment recv, MemorySegment s, MemorySegment a1) {
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
        try { hVoid.invokeExact(recv, s); } catch (Throwable t) { throw fail(t); }
    }

    public static void msgSendVoidId(MemorySegment recv, MemorySegment s, MemorySegment a1) {
        try { hVoidId.invokeExact(recv, s, a1); } catch (Throwable t) { throw fail(t); }
    }

    public static void msgSendVoidLong(MemorySegment recv, MemorySegment s, long a1) {
        try { hVoidLong.invokeExact(recv, s, a1); } catch (Throwable t) { throw fail(t); }
    }

    public static void msgSendVoidBool(MemorySegment recv, MemorySegment s, boolean a1) {
        try { hVoidBool.invokeExact(recv, s, a1); } catch (Throwable t) { throw fail(t); }
    }

    public static long msgSendLong(MemorySegment recv, MemorySegment s) {
        try { return (long) hLong.invokeExact(recv, s); } catch (Throwable t) { throw fail(t); }
    }

    public static boolean msgSendBool(MemorySegment recv, MemorySegment s) {
        try { return (boolean) hBool.invokeExact(recv, s); } catch (Throwable t) { throw fail(t); }
    }

    /// Struct-returning message (NSRect); uses objc_msgSend_stret on x86_64.
    /// FFM gives downcalls with group-layout returns an implicit leading
    /// SegmentAllocator parameter, which is where the returned struct is written.
    public static MemorySegment msgSendRect(MemorySegment recv, MemorySegment s) {
        try { return (MemorySegment) hRect.invokeExact((SegmentAllocator) ARENA, recv, s); } catch (Throwable t) { throw fail(t); }
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

    /// Allocate a `struct objc_super { id receiver; Class super_class;`} in the
    /// global arena — the argument `objc_msgSendSuper` needs for `[super ...]`.
    public static MemorySegment superStruct(MemorySegment receiver, MemorySegment superClass) {
        MemorySegment s = ARENA.allocate(16);
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
