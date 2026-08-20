package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * DelegateProxy — the generic "Java implements an ObjC protocol/selector"
 * mechanism for NSUI3.
 *
 * <p>Cocoa objects talk to their delegates/actions through plain ObjC selectors.
 * Normally you subclass (in ObjC) to implement those selectors; DelegateProxy does
 * it from Java: it creates an ObjC class at runtime whose methods are FFM upcall
 * stubs back into Java, then dispatches each selector to a small Java callback
 * keyed by the <em>instance</em> and the <em>selector address</em>.
 *
 * <p>Dispatch model: ONE registry maps the native instance address to a {@link Holder}
 * whose inner maps are keyed by <b>selector address</b> (sel_registerName returns a
 * unique, stable pointer per selector name — no new descriptors are needed to recover
 * the name). Each ObjC class pair you create reuses the <em>same</em> shared upcall
 * stubs ({@link #dispatchBool}, {@link #dispatchVoid}, {@link #dispatchInt},
 * {@link #dispatchIdIdInt}, {@link #dispatchWindowWillResize}, {@link #dispatchDealloc} plus the two message-forwarding
 * targets {@link #dispatchSignature}/{@link #dispatchInvocation}) — stubs are
 * per-TARGET, not per-method; the selector routing happens in Java.
 *
 * <p>Five callback shapes:
 * <ul>
 *   <li>{@link BoolArg} — {@code -(BOOL)method:(id)} (e.g. <code>windowShouldClose:</code>)</li>
 *   <li>{@link VoidArg} — {@code -(void)method:(id)} (e.g. <code>windowWillClose:</code>)</li>
 *   <li>{@link IntArg} — {@code -(NSInteger)method:(id)} (e.g. <code>numberOfRowsInTableView:</code>, a
 *       data-source count)</li>
 *   <li>{@link IdIdIntArg} — {@code -(id)method:(id):(id):(NSInteger)} (e.g.
 *       <code>tableView:objectValueForTableColumn:row:</code>, a data-source cell)</li>
 *   <li>{@link WindowSizeArg} — {@code -(NSSize)windowWillResize:(id) toSize:(NSSize)} (e.g.
 *       <code>windowWillResize:toSize:</code>, a window delegate veto that can clamp the proposed frame size)</li>
 * </ul>
 * {@link #delegate(String, String, Map, Map, Map, Map)} carries the first four; the
 * {@link #delegate(String, String, Map, Map, Map, Map, Map)} overload additionally carries
 * {@link WindowSizeArg}; the classic
 * 4-arg {@link #delegate} is the bool/void subset; {@link #actionTarget} is the
 * single-selector {@code -(void)} piece.
 *
 * <p>AOT safety (GraalVM): every downcall/upcall handle is created in the lazy
 * synchronized {@link #ensureInit()}, never in a static initializer. {@link Holder}
 * and the registry are AOT-friendly plain Java. The package-visible static upcall
 * targets are registerable from {@link NsuiFeature} for native-image.
 */
public final class DelegateProxy {

    private DelegateProxy() {}

    /** Java-side {@code -(BOOL)method:(id)sender} — return the "should" verdict. */
    public interface BoolArg { boolean call(MemorySegment sender); }

    /** Java-side {@code -(void)method:(id)sender} — a side-effecting notification. */
    public interface VoidArg { void call(MemorySegment sender); }

    /** Java-side {@code -(NSInteger)method:(id)} — a count/data-source query, e.g. numberOfRowsInTableView:. */
    public interface IntArg { long call(MemorySegment sender); }

    /** Java-side {@code -(id)method:(id):(id):(NSInteger)} — an object keyed by arg + integer, e.g. tableView:objectValueForTableColumn:row:. */
    public interface IdIdIntArg { MemorySegment call(MemorySegment tableView, MemorySegment tableColumn, long row); }

    /** Java-side {@code -(NSSize)windowWillResize:(id) toSize:(NSSize)} — window delegate veto, clamp/adjust proposed size. */
    public interface WindowSizeArg { MemorySegment call(MemorySegment sender, MemorySegment proposedSize); }

    /** Java-side {@code -(id)touchBar:(id) makeItemForIdentifier:(id)} — Touch Bar delegate, returns NSTouchBarItem. */
    public interface IdIdArg { MemorySegment call(MemorySegment touchBar, MemorySegment identifier); }

    /** Per-instance dispatch state, keyed by the native instance address. */
    private record Holder(Map<Long, BoolArg> bools, Map<Long, VoidArg> voids,
                          Map<Long, IntArg> ints, Map<Long, IdIdIntArg> idIdInts,
                          Map<Long, WindowSizeArg> windowSizes,
                          Map<Long, IdIdArg> idIds,
                          MemorySegment superClass) {}

    /** ObjC class pair (native class + the selector names already installed on it). */
    private record RuntimeClass(MemorySegment cls, Set<String> boolMethods, Set<String> voidMethods,
                                 Set<String> intMethods, Set<String> idIdIntMethods,
                                 Set<String> windowSizeMethods, Set<String> idIdMethods) {}

    // A registry keyed by the native instance address -> its dispatch holder.
    private static final ConcurrentMap<Long, Holder> REGISTRY = new ConcurrentHashMap<>();

    // A registry of created class pairs, keyed by "super\0className" for delegates and
    // a fixed key for action targets — lazily populated in ensureInit()/on first use.
    private static final ConcurrentMap<String, RuntimeClass> CLASSES = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ runtime state
    // (built lazily in ensureInit(); NEVER in a static initializer — native-image rule.)

    private static boolean initialized;

    /** Shared upcall stubs — same ones everywhere, reused by every class pair. */
    private static MemorySegment boolStub;
    private static MemorySegment voidStub;
    private static MemorySegment intStub;
    private static MemorySegment idIdIntStub;
    private static MemorySegment windowSizeStub;
    private static MemorySegment idIdStub;
    private static MemorySegment deallocStub;
    private static MemorySegment sigStub;    // methodSignatureForSelector: (returns an NSMethodSignature)
    private static MemorySegment invStub;    // forwardInvocation: (swallows unknown selectors)

    /**
     * Upcall descriptor for {@code methodSignatureForSelector:} — {@code id methodSignatureForSelector:(SEL)}
     * = (id return, id self, SEL _cmd, SEL arg). Lives in NsuiForeign as the single source of truth.
     */
    private static final FunctionDescriptor METHOD_SIGNATURE_UPCALL = NsuiForeign.methodSignatureUpcall();

    // --------------------------------------------------------------- upcall targets
    // Package-visible static so NsuiFeature can register them for AOT.

    /** FFM upcall target: {@code -(BOOL)method:(id)} — routes to a {@link BoolArg} by selector; default YES. */
    static boolean dispatchBool(MemorySegment self, MemorySegment sel, MemorySegment sender) {
        Holder h = REGISTRY.get(self.address());
        BoolArg b = (h == null) ? null : h.bools().get(sel.address());
        if (b == null) return true;      // safe default true for "should" semantics
        Scratch.beginTurn();
        try {
            return b.call(sender);
        } finally {
            Scratch.endTurn();
        }
    }

    /** FFM upcall target: {@code -(void)method:(id)} — routes to a {@link VoidArg} by selector; default no-op. */
    static void dispatchVoid(MemorySegment self, MemorySegment sel, MemorySegment sender) {
        Holder h = REGISTRY.get(self.address());
        VoidArg v = (h == null) ? null : h.voids().get(sel.address());
        if (v == null) return;
        Scratch.beginTurn();
        try {
            v.call(sender);
        } finally {
            Scratch.endTurn();
        }
    }

    /**
     * FFM upcall target: {@code -(NSInteger)method:(id)} — routes to an {@link IntArg} by selector;
     * default 0. NSInteger is long on 64-bit; the registered descriptor returns LONG.
     */
    static long dispatchInt(MemorySegment self, MemorySegment sel, MemorySegment sender) {
        Holder h = REGISTRY.get(self.address());
        IntArg f = (h == null) ? null : h.ints().get(sel.address());
        if (f == null) return 0L;
        Scratch.beginTurn();
        try {
            return f.call(sender);
        } finally {
            Scratch.endTurn();
        }
    }

    /** FFM upcall target: {@code -(id)tableView:(id):(id):(NSInteger)} — routes to an {@link IdIdIntArg}; default NULL. */
    static MemorySegment dispatchIdIdInt(MemorySegment self, MemorySegment sel, MemorySegment a, MemorySegment b, long c) {
        Holder h = REGISTRY.get(self.address());
        IdIdIntArg f = (h == null) ? null : h.idIdInts().get(sel.address());
        if (f == null) return MemorySegment.NULL;
        Scratch.beginTurn();
        try {
            return f.call(a, b, c);
        } finally {
            Scratch.endTurn();
        }
    }

    /**
     * FFM upcall target: {@code -(NSSize)windowWillResize:(id) toSize:(NSSize)} — routes to a {@link WindowSizeArg} by selector;
     * default is to return the proposed size unchanged (pass-through veto).
     */
    static MemorySegment dispatchWindowWillResize(MemorySegment self, MemorySegment sel, MemorySegment sender, MemorySegment proposedSize) {
        Holder h = REGISTRY.get(self.address());
        WindowSizeArg f = (h == null) ? null : h.windowSizes().get(sel.address());
        if (f == null) return proposedSize;
        Scratch.beginTurn();
        try {
            MemorySegment result = f.call(sender, proposedSize);
            if (result == null || result.address() == 0) return proposedSize;
            return result;
        } finally {
            Scratch.endTurn();
        }
    }

    /** FFM upcall target: {@code -(id)touchBar:(id) makeItemForIdentifier:(id)} — routes to an {@link IdIdArg}; default NULL. */
    static MemorySegment dispatchIdId(MemorySegment self, MemorySegment sel, MemorySegment a, MemorySegment b) {
        Holder h = REGISTRY.get(self.address());
        IdIdArg f = (h == null) ? null : h.idIds().get(sel.address());
        if (f == null) return MemorySegment.NULL;
        Scratch.beginTurn();
        try {
            MemorySegment r = f.call(a, b);
            return (r == null) ? MemorySegment.NULL : r;
        } finally {
            Scratch.endTurn();
        }
    }

    /** FFM upcall target: {@code -(void)dealloc} — unregister the instance, then chain {@code [super dealloc]}. */
    static void dispatchDealloc(MemorySegment self, MemorySegment sel) {
        Holder h = REGISTRY.remove(self.address());
        // The superclass is resolved once at pair creation and carried on the holder;
        // it is the ObjC class the instance's runtime class subclasses. If we no longer
        // know it (defensive: an instance we never created), there is nothing to chain.
        MemorySegment superClass = (h == null) ? null : h.superClass();
        if (superClass == null || superClass.address() == 0) {
            return;
        }
        MemorySegment superStruct = ObjC.superStruct(self, superClass);
        ObjC.msgSendSuperVoid(superStruct, sel);
    }

    /**
     * FFM upcall target: {@code -(NSMethodSignature *)methodSignatureForSelector:(SEL)aSelector}.
     * Returns a valid (void, one id arg) signature so the runtime can build an NSInvocation and
     * deliver it to {@link #dispatchInvocation}, which swallows it — so an UNREGISTERED selector
     * sent to the instance is a harmless no-op instead of NSObject raising {@code NSInvalidArgumentException}
     * (which would abort the JVM as a native exception). Signature is built via
     * {@code [NSMethodSignature signatureWithObjCTypes:"v@:@"]}.
     *
     * <p>KNOWN APPROXIMATION: every unknown selector receives the SAME fixed signature. This is
     * correct for every selector NSUI3 actually registers (all single-id-arg, by protocol) and
     * safe for genuinely unknown ones (forwardInvocation: swallows; the signature is never used
     * to call anything) — but a caller that inspects the returned signature for an unknown
     * selector gets a lie about its real shape. Documented, not fixed: the alternative requires
     * per-selector signature tables.
     */
    static MemorySegment dispatchSignature(MemorySegment self, MemorySegment selCmd, MemorySegment aSelector) {
        return ObjC.msgSendIdId(ObjC.cls("NSMethodSignature"), ObjC.sel("signatureWithObjCTypes:"),
                ObjC.cstring("v@:@"));
    }

    /** FFM upcall target: {@code -(void)forwardInvocation:(NSInvocation *)anInvocation} — swallow it. */
    static void dispatchInvocation(MemorySegment self, MemorySegment selCmd, MemorySegment anInvocation) {
        // Intentional no-op: a selector we did not register is dropped here.
    }

    // ------------------------------------------------------------------ public API

    /**
     * Build (lazily) a target/action object implementing {@code selector} in Java.
     * The returned ObjC instance differs per call, but they all share ONE lazily
     * created class pair (subclass {@code NSObject}); each distinct selector is
     * installed on that class on first use. Register the handler under this
     * instance's peer keyed by the selector address.
     *
     * @param selector the ObjC selector to implement, e.g. {@code "ballPopped:"}
     * @param handler  the Java callback fired when the selector routes to this instance
     * @return the ObjC id a control's {@code setTarget:} should receive
     */
    public static MemorySegment actionTarget(String selector, VoidArg handler) {
        MemorySegment pair = ensureClassPair("NSObject\0NSUIActionTarget");
        addVoidMethod(pair, selector);

        MemorySegment instance = allocInit(pair);
        Map<Long, VoidArg> voids = new ConcurrentHashMap<>();
        voids.put(ObjC.sel(selector).address(), handler);
        Map<Long, BoolArg> bools = new ConcurrentHashMap<>();
        Map<Long, IntArg> ints = new ConcurrentHashMap<>();
        Map<Long, IdIdIntArg> idIdInts = new ConcurrentHashMap<>();
        Map<Long, WindowSizeArg> windowSizes = new ConcurrentHashMap<>();
        Map<Long, IdIdArg> idIds = new ConcurrentHashMap<>();
        REGISTRY.put(instance.address(), new Holder(bools, voids, ints, idIdInts, windowSizes, idIds, ObjC.classGetSuperclass(pair)));
        return instance;
    }

    /**
     * Build (lazily) a protocol-ish delegate object subclassing {@code superClassName}
     * (e.g. {@code NSObject} or {@code NSWindow}) — one class pair per
     * (superClassName, className). Each {@code -(BOOL)} selector is encoded {@code "c@:@"}
     * and routed to {@link #dispatchBool}; each {@code -(void)} selector {@code "v@:@"} to
     * {@link #dispatchVoid}; a {@code dealloc} override is always added. A single instance
     * is allocated ({@code [[cls alloc] init]}) and registered.
     *
     * <p>Convenience overload = the bool/void-only data source that is the classic delegate
     * shape; {@link #delegate(String, String, Map, Map, Map, Map)} additionally carries the
     * data-source {@code -(NSInteger)} and {@code -(id)}-returning shapes.
     *
     * @param superClassName the ObjC superclass name
     * @param className      the runtime class name (unique per distinct delegate shape)
     * @param boolSelectors  selector-name → {@link BoolArg}; empty for none
     * @param voidSelectors  selector-name → {@link VoidArg}; empty for none
     * @return the allocated-and-initialized delegate instance
     */
    public static MemorySegment delegate(String superClassName, String className,
            Map<String, BoolArg> boolSelectors, Map<String, VoidArg> voidSelectors) {
        Map<String, IntArg> ints = Map.of();
        Map<String, IdIdIntArg> idIdInts = Map.of();
        Map<String, WindowSizeArg> windowSizes = Map.of();
        return delegate(superClassName, className, boolSelectors, voidSelectors, ints, idIdInts, windowSizes);
    }

    /**
     * Build (lazily) a protocol-ish delegate / data-source object subclassing
     * {@code superClassName} — one class pair per (superClassName, className).
     *
     * <p>Selector encodings and dispatch targets:
     * <ul>
     *   <li>{@code -(BOOL)method:(id)} → {@code "c@:@"} → {@link #dispatchBool}</li>
     *   <li>{@code -(void)method:(id)} → {@code "v@:@"} → {@link #dispatchVoid}</li>
     *   <li>{@code -(NSInteger)method:(id)} → {@code "q@:@"} → {@link #dispatchInt}</li>
     *   <li>{@code -(id)tableView:(id):(id):(NSInteger)} → {@code "@@:@@q"} → {@link #dispatchIdIdInt}</li>
     * </ul>
     * plus the always-installed {@code dealloc}, {@code methodSignatureForSelector:} and
     * {@code forwardInvocation:} overrides (unregistered selectors are safe no-ops).
     * A single instance is allocated ({@code [[cls alloc] init]}) and registered.
     *
     * @param superClassName the ObjC superclass name
     * @param className      the runtime class name (unique per distinct delegate shape)
     * @param boolSelectors  selector-name → {@link BoolArg}; empty for none
     * @param voidSelectors  selector-name → {@link VoidArg}; empty for none
     * @param intSelectors   selector-name → {@link IntArg}; empty for none
     * @param idIdIntSelectors selector-name → {@link IdIdIntArg}; empty for none
     * @return the allocated-and-initialized delegate instance
     */
    public static MemorySegment delegate(String superClassName, String className,
            Map<String, BoolArg> boolSelectors, Map<String, VoidArg> voidSelectors,
            Map<String, IntArg> intSelectors, Map<String, IdIdIntArg> idIdIntSelectors) {
        Map<String, WindowSizeArg> windowSizes = Map.of();
        return delegate(superClassName, className, boolSelectors, voidSelectors, intSelectors, idIdIntSelectors, windowSizes);
    }

    /**
     * Build (lazily) a protocol-ish delegate / data-source / window-delegate object subclassing
     * {@code superClassName} — one class pair per (superClassName, className).
     *
     * <p>Selector encodings and dispatch targets:
     * <ul>
     *   <li>{@code -(BOOL)method:(id)} → {@code "c@:@"} → {@link #dispatchBool}</li>
     *   <li>{@code -(void)method:(id)} → {@code "v@:@"} → {@link #dispatchVoid}</li>
     *   <li>{@code -(NSInteger)method:(id)} → {@code "q@:@"} → {@link #dispatchInt}</li>
     *   <li>{@code -(id)tableView:(id):(id):(NSInteger)} → {@code "@@:@@q"} → {@link #dispatchIdIdInt}</li>
     *   <li>{@code -(NSSize)windowWillResize:(id) toSize:(NSSize)} → {@code "{CGSize=dd}@:@{CGSize=dd}"} → {@link #dispatchWindowWillResize}</li>
     * </ul>
     * plus the always-installed {@code dealloc}, {@code methodSignatureForSelector:} and
     * {@code forwardInvocation:} overrides (unregistered selectors are safe no-ops).
     * A single instance is allocated ({@code [[cls alloc] init]}) and registered.
     *
     * @param superClassName the ObjC superclass name
     * @param className      the runtime class name (unique per distinct delegate shape)
     * @param boolSelectors  selector-name → {@link BoolArg}; empty for none
     * @param voidSelectors  selector-name → {@link VoidArg}; empty for none
     * @param intSelectors   selector-name → {@link IntArg}; empty for none
     * @param idIdIntSelectors selector-name → {@link IdIdIntArg}; empty for none
     * @param windowSizeSelectors selector-name → {@link WindowSizeArg}; empty for none (e.g. windowWillResize:toSize:)
     * @return the allocated-and-initialized delegate instance
     */
    public static MemorySegment delegate(String superClassName, String className,
            Map<String, BoolArg> boolSelectors, Map<String, VoidArg> voidSelectors,
            Map<String, IntArg> intSelectors, Map<String, IdIdIntArg> idIdIntSelectors,
            Map<String, WindowSizeArg> windowSizeSelectors) {
        return delegate(superClassName, className, boolSelectors, voidSelectors, intSelectors, idIdIntSelectors, windowSizeSelectors, Map.of());
    }

    /**
     * Build delegate with Touch Bar {@code touchBar:makeItemForIdentifier:} support.
     * Adds {@code -(id)touchBar:(id) makeItemForIdentifier:(id)} → {@code "@@:@@"
     * } → {@link #dispatchIdId}. All other selector shapes remain as above.
     */
    public static MemorySegment delegate(String superClassName, String className,
            Map<String, BoolArg> boolSelectors, Map<String, VoidArg> voidSelectors,
            Map<String, IntArg> intSelectors, Map<String, IdIdIntArg> idIdIntSelectors,
            Map<String, WindowSizeArg> windowSizeSelectors,
            Map<String, IdIdArg> idIdSelectors) {
        MemorySegment pair = ensureClassPair(superClassName + "\0" + className);
        Map<Long, BoolArg> bools = new ConcurrentHashMap<>();
        Map<Long, VoidArg> voids = new ConcurrentHashMap<>();
        Map<Long, IntArg> ints = new ConcurrentHashMap<>();
        Map<Long, IdIdIntArg> idIdInts = new ConcurrentHashMap<>();
        Map<Long, WindowSizeArg> windowSizes = new ConcurrentHashMap<>();
        Map<Long, IdIdArg> idIds = new ConcurrentHashMap<>();

        for (Map.Entry<String, BoolArg> e : boolSelectors.entrySet()) {
            addBoolMethod(pair, e.getKey());
            bools.put(ObjC.sel(e.getKey()).address(), e.getValue());
        }
        for (Map.Entry<String, VoidArg> e : voidSelectors.entrySet()) {
            addVoidMethod(pair, e.getKey());
            voids.put(ObjC.sel(e.getKey()).address(), e.getValue());
        }
        for (Map.Entry<String, IntArg> e : intSelectors.entrySet()) {
            addIntMethod(pair, e.getKey());
            ints.put(ObjC.sel(e.getKey()).address(), e.getValue());
        }
        for (Map.Entry<String, IdIdIntArg> e : idIdIntSelectors.entrySet()) {
            addIdIdIntMethod(pair, e.getKey());
            idIdInts.put(ObjC.sel(e.getKey()).address(), e.getValue());
        }
        for (Map.Entry<String, WindowSizeArg> e : windowSizeSelectors.entrySet()) {
            addWindowSizeMethod(pair, e.getKey());
            windowSizes.put(ObjC.sel(e.getKey()).address(), e.getValue());
        }
        for (Map.Entry<String, IdIdArg> e : idIdSelectors.entrySet()) {
            addIdIdMethod(pair, e.getKey());
            idIds.put(ObjC.sel(e.getKey()).address(), e.getValue());
        }

        MemorySegment instance = allocInit(pair);
        REGISTRY.put(instance.address(), new Holder(bools, voids, ints, idIdInts, windowSizes, idIds, ObjC.classGetSuperclass(pair)));
        return instance;
    }

    /** Number of live delegate/action instances (diagnostics/tests). */
    public static int registrySize() {
        return REGISTRY.size();
    }

    // ------------------------------------------------------------------ internals

    /** Build the shared upcall stubs ONCE, lazily (never in a static initializer). */
    private static synchronized void ensureInit() {
        if (initialized) return;
        try {
            MethodHandle boolTarget = MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchBool",
                    MethodType.methodType(boolean.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
            boolStub = ObjC.upcall(boolTarget, NsuiForeign.delegateShouldTerminate());   // (BOOL, PTR, PTR, PTR)

            MethodHandle voidTarget = MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchVoid",
                    MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
            voidStub = ObjC.upcall(voidTarget, NsuiForeign.delegateWindowWillClose());   // (VOID, PTR, PTR, PTR)

            MethodHandle intTarget = MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchInt",
                    MethodType.methodType(long.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
            intStub = ObjC.upcall(intTarget, NsuiForeign.delegateIntUpcall());           // (LONG, PTR, PTR, PTR)

            MethodHandle idIdIntTarget = MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchIdIdInt",
                    MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class,
                            MemorySegment.class, MemorySegment.class, long.class));
            idIdIntStub = ObjC.upcall(idIdIntTarget, NsuiForeign.delegateIdIdIntUpcall()); // (PTR, PTR, PTR, PTR, PTR, LONG)

            MethodHandle windowSizeTarget = MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchWindowWillResize",
                    MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class,
                            MemorySegment.class, MemorySegment.class));
            windowSizeStub = ObjC.upcall(windowSizeTarget, NsuiForeign.delegateWindowWillResize()); // (NS_SIZE, PTR, PTR, PTR, NS_SIZE)

            MethodHandle idIdTarget = MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchIdId",
                    MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
            idIdStub = ObjC.upcall(idIdTarget, NsuiForeign.delegateIdIdUpcall()); // (PTR, PTR, PTR, PTR, PTR) touchBar:makeItemForIdentifier:

            MethodHandle deallocTarget = MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchDealloc",
                    MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class));
            deallocStub = ObjC.upcall(deallocTarget, NsuiForeign.deallocUpcall());        // (VOID, PTR, PTR)

            // methodSignatureForSelector: returns id  (id, id, SEL, SEL).
            MethodHandle sigTarget = MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchSignature",
                    MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
            sigStub = ObjC.upcall(sigTarget, METHOD_SIGNATURE_UPCALL);

            // forwardInvocation: returns void (void, id, SEL, id) — same shape as dispatchVoid.
            MethodHandle invTarget = MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchInvocation",
                    MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
            invStub = ObjC.upcall(invTarget, NsuiForeign.delegateWindowWillClose());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("cannot bind DelegateProxy upcall targets", e);
        }
        initialized = true;
    }

    /**
     * Lazily resolve/build the class pair for a key and ensure its {@code dealloc},
     * {@code methodSignatureForSelector:} and {@code forwardInvocation:} overrides are
     * installed. Class pairs and stubs stay strongly referenced for the life of the process
     * (they must never be collected).
     *
     * <p>The two forwarding methods let an UNREGISTERED selector sent to the instance be a
     * harmless no-op instead of NSObject raising {@code NSInvalidArgumentException} (which
     * would abort the JVM as a native exception) — see {@link #dispatchSignature}/{@link
     * #dispatchInvocation}. Registered selectors are found by the runtime before forwarding
     * is ever reached, so this never interferes with real behavior.
     */
    private static synchronized MemorySegment ensureClassPair(String key) {
        ensureInit();
        RuntimeClass rc = CLASSES.get(key);
        if (rc == null) {
            String[] parts = key.split("\0", 2);
            MemorySegment pair = ObjC.makeClass(parts[0], parts[1]);
            if (!ObjC.addMethod(pair, "dealloc", deallocStub, "v@:")) {
                throw new RuntimeException("class_addMethod dealloc failed for " + parts[1]);
            }
            // -(NSMethodSignature *)methodSignatureForSelector:(SEL) — "@@::" (returns id).
            if (!ObjC.addMethod(pair, "methodSignatureForSelector:", sigStub, "@@::")) {
                throw new RuntimeException("class_addMethod methodSignatureForSelector: failed for " + parts[1]);
            }
            // -(void)forwardInvocation:(NSInvocation *) — "v@:@" (invocation is an object).
            if (!ObjC.addMethod(pair, "forwardInvocation:", invStub, "v@:@")) {
                throw new RuntimeException("class_addMethod forwardInvocation: failed for " + parts[1]);
            }
            rc = new RuntimeClass(pair,
                    new CopyOnWriteArraySet<>(), new CopyOnWriteArraySet<>(),
                    new CopyOnWriteArraySet<>(), new CopyOnWriteArraySet<>(),
                    new CopyOnWriteArraySet<>(), new CopyOnWriteArraySet<>());
            CLASSES.put(key, rc);
        }
        return rc.cls();
    }

    /** Add a {@code -(BOOL)method:(id)} (encoding "c@:@") to a class pair unless already present. */
    private static void addBoolMethod(MemorySegment pair, String selector) {
        RuntimeClass rc = classInfo(pair);
        if (rc.boolMethods().add(selector)) {
            if (!ObjC.addMethod(pair, selector, boolStub, "c@:@")) {
                throw new RuntimeException("class_addMethod bool " + selector + " failed");
            }
        }
    }

    /** Add a {@code -(void)method:(id)} (encoding "v@:@") to a class pair unless already present. */
    private static void addVoidMethod(MemorySegment pair, String selector) {
        RuntimeClass rc = classInfo(pair);
        if (rc.voidMethods().add(selector)) {
            if (!ObjC.addMethod(pair, selector, voidStub, "v@:@")) {
                throw new RuntimeException("class_addMethod void " + selector + " failed");
            }
        }
    }

    /** Add a {@code -(NSInteger)method:(id)} (encoding "q@:@") to a class pair unless already present. */
    private static void addIntMethod(MemorySegment pair, String selector) {
        RuntimeClass rc = classInfo(pair);
        if (rc.intMethods().add(selector)) {
            if (!ObjC.addMethod(pair, selector, intStub, "q@:@")) {
                throw new RuntimeException("class_addMethod int " + selector + " failed");
            }
        }
    }

    /** Add a {@code -(id)tableView:(id):(id):(NSInteger)} (encoding "@@:@@q") to a class pair unless already present. */
    private static void addIdIdIntMethod(MemorySegment pair, String selector) {
        RuntimeClass rc = classInfo(pair);
        if (rc.idIdIntMethods().add(selector)) {
            if (!ObjC.addMethod(pair, selector, idIdIntStub, "@@:@@q")) {
                throw new RuntimeException("class_addMethod id-id-int " + selector + " failed");
            }
        }
    }

    /** Add a {@code -(NSSize)windowWillResize:(id) toSize:(NSSize)} (encoding "{CGSize=dd}@:@{CGSize=dd}") to a class pair unless already present. */
    private static void addWindowSizeMethod(MemorySegment pair, String selector) {
        RuntimeClass rc = classInfo(pair);
        if (rc.windowSizeMethods().add(selector)) {
            if (!ObjC.addMethod(pair, selector, windowSizeStub, "{CGSize=dd}@:@{CGSize=dd}")) {
                throw new RuntimeException("class_addMethod windowSize " + selector + " failed");
            }
        }
    }

    /** Add a {@code -(id)touchBar:(id) makeItemForIdentifier:(id)} (encoding "@@:@@") to a class pair unless already present. */
    private static void addIdIdMethod(MemorySegment pair, String selector) {
        RuntimeClass rc = classInfo(pair);
        if (rc.idIdMethods().add(selector)) {
            if (!ObjC.addMethod(pair, selector, idIdStub, "@@:@@")) {
                throw new RuntimeException("class_addMethod idId " + selector + " failed");
            }
        }
    }

    private static RuntimeClass classInfo(MemorySegment pair) {
        for (RuntimeClass rc : CLASSES.values()) {
            if (rc.cls().equals(pair)) return rc;
        }
        throw new IllegalStateException("no RuntimeClass registered for " + pair);
    }

    /** {@code [[cls alloc] init]} — the standard no-arg object creation path. */
    private static MemorySegment allocInit(MemorySegment cls) {
        MemorySegment instance = ObjC.msgSendId(cls, ObjC.sel("alloc"));
        instance = ObjC.msgSendId(instance, ObjC.sel("init"));
        if (instance.address() == 0) {
            throw new IllegalStateException("alloc/init failed");
        }
        return instance;
    }

}
