package nsui.objc;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;

/// Build-time registration of every FFM call the app makes — the "no tracing
/// agent" path. Native-image must generate the native call stubs at build time,
/// so every downcall/upcall `FunctionDescriptor` used at run time is
/// registered here with `RuntimeForeignAccess`.
///
/// Everything derives from single-source-of-truth lists, so the registered set
/// can never drift from the runtime set:
/// - `RUNTIME` / `CORE` — C-function downcalls;
/// - `VOCABULARY` — one descriptor per message signature (this is
///   where the whole objc_msgSend surface comes from);
/// - the delegate upcalls, registered explicitly (upcall stubs are per target
///   method, not per signature).
///
/// Register the feature with `--features=nsui.objc.NsuiFeature`.
/// This class runs only inside the image builder; it never ends up in the image.
public final class NsuiFeature implements Feature {

    @Override
    public void duringSetup(DuringSetupAccess access) {
        System.out.println("[NsuiFeature] duringSetup start");
        for (FunctionDescriptor d : NsuiForeign.RUNTIME) {
            RuntimeForeignAccess.registerForDowncall(d);
        }
        System.out.println("[NsuiFeature] runtime downcalls registered (" + NsuiForeign.RUNTIME.size() + ")");
        for (FunctionDescriptor d : NsuiForeign.CORE) {
            RuntimeForeignAccess.registerForDowncall(d);
        }
        System.out.println("[NsuiFeature] cg/cf downcalls registered (" + NsuiForeign.CORE.size() + ")");
        for (FunctionDescriptor d : NsuiForeign.AUTORELEASE) {
            RuntimeForeignAccess.registerForDowncall(d);
        }
        for (FunctionDescriptor d : NsuiForeign.EXCEPTION) {
            RuntimeForeignAccess.registerForDowncall(d);
        }
        for (FunctionDescriptor d : NsuiForeign.DISPATCH) {
            RuntimeForeignAccess.registerForDowncall(d);
        }
        for (FunctionDescriptor d : NsuiForeign.CG_DRAW) {
            RuntimeForeignAccess.registerForDowncall(d);
        }
        for (FunctionDescriptor d : NsuiForeign.CT_TEXT) {
            RuntimeForeignAccess.registerForDowncall(d);
        }
        System.out.println("[NsuiFeature] shim downcalls registered (" + (NsuiForeign.AUTORELEASE.size()
                + NsuiForeign.EXCEPTION.size() + NsuiForeign.DISPATCH.size() + NsuiForeign.CG_DRAW.size()
                + NsuiForeign.CT_TEXT.size()) + ")");
        int n = 0;
        for (Sig.S s : Sig.VOCABULARY) {
            RuntimeForeignAccess.registerForDowncall(s.descriptor());
            n++;
        }
        System.out.println("[NsuiFeature] msgSend vocabulary registered (" + n + " signatures)");

        // ---- upcalls: methods implemented in Java, called by AppKit ----
        int upcalls = 0;
        try {
            // NSView: Java-implemented drawRect: + dealloc
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(nsui.NSView.class, "drawRectImpl",
                            MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.drawRectUpcall());
            upcalls++;
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(nsui.NSView.class, "deallocImpl",
                            MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.deallocUpcall());
            upcalls++;
            // NSSwitch: fallback map cleanup on dealloc
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(nsui.NSSwitch.class, "deallocImpl",
                            MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.deallocUpcall());
            upcalls++;
            // Dispatch: the block body behind every dispatch_async-on-main
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(Dispatch.class, "runBody",
                            MethodType.methodType(void.class, MemorySegment.class)),
                    NsuiForeign.blockVoidUpcall());
            upcalls++;
            // Exceptions: the ObjC exception preprocessor
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(Exceptions.class, "preprocessor",
                            MethodType.methodType(MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.setExceptionPreprocessor());
            upcalls++;
            // DelegateProxy: generic "Java implements an ObjC selector" dispatchers
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchBool",
                            MethodType.methodType(boolean.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.delegateShouldTerminate());
            upcalls++;
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchVoid",
                            MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.delegateWindowWillClose());
            upcalls++;
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchDealloc",
                            MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.deallocUpcall());
            upcalls++;
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchInvocation",
                            MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.delegateWindowWillClose());
            upcalls++;
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchSignature",
                            MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.methodSignatureUpcall());
            upcalls++;
            // DelegateProxy data-source shapes (NSTableView etc.)
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchInt",
                            MethodType.methodType(long.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.delegateIntUpcall());
            upcalls++;
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchIdIdInt",
                            MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class,
                                    MemorySegment.class, MemorySegment.class, long.class)),
                    NsuiForeign.delegateIdIdIntUpcall());
            upcalls++;
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchWindowWillResize",
                            MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class,
                                    MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.delegateWindowWillResize());
            upcalls++;
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchId",
                            MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.delegateDockMenu());
            upcalls++;
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchIdId",
                            MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.delegateIdIdUpcall());
            upcalls++;
            // ThemeObserver — CoreFoundation dark-mode upcall
            for (FunctionDescriptor d : NsuiForeign.THEME) {
                RuntimeForeignAccess.registerForDowncall(d);
            }
            System.out.println("[NsuiFeature] theme downcalls registered (" + NsuiForeign.THEME.size() + ")");
            RuntimeForeignAccess.registerForDirectUpcall(
                    MethodHandles.lookup().findStatic(ThemeObserver.class, "staticThemeChangedCallback",
                            MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.themeUpcall());
            upcalls++;
            // NSView input events: mouse/key pass-throughs ("v@:@"), key
            // equivalent ("B@:@"), first-responder predicate ("B@:")
            upcalls += registerUpcall(nsui.NSView.class, "mouseDownImpl", NSViewEventVoid, NsuiForeign.eventVoidUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "mouseDraggedImpl", NSViewEventVoid, NsuiForeign.eventVoidUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "mouseUpImpl", NSViewEventVoid, NsuiForeign.eventVoidUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "mouseMovedImpl", NSViewEventVoid, NsuiForeign.eventVoidUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "mouseEnteredImpl", NSViewEventVoid, NsuiForeign.eventVoidUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "mouseExitedImpl", NSViewEventVoid, NsuiForeign.eventVoidUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "keyDownImpl", NSViewEventVoid, NsuiForeign.eventVoidUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "keyUpImpl", NSViewEventVoid, NsuiForeign.eventVoidUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "flagsChangedImpl", NSViewEventVoid, NsuiForeign.eventVoidUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "performKeyEquivalentImpl", NSViewEventBool, NsuiForeign.eventBoolUpcall());
            upcalls += registerUpcall(nsui.NSView.class, "acceptsFirstResponderImpl", ResponderBool, NsuiForeign.responderBoolUpcall());
            System.out.println("[NsuiFeature] direct upcalls registered (" + upcalls + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("cannot register direct upcall target", e);
        }
        System.out.println("[NsuiFeature] duringSetup done");
    }

    // ---- shapes for the NSView event/responder upcall targets ----
    private static final MethodType NSViewEventVoid =
            MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class);
    private static final MethodType NSViewEventBool =
            MethodType.methodType(boolean.class, MemorySegment.class, MemorySegment.class, MemorySegment.class);
    private static final MethodType ResponderBool =
            MethodType.methodType(boolean.class, MemorySegment.class, MemorySegment.class);

    /// Register one Java-implemented upcall target (build time only) — the same
    /// findStatic + registerForDirectUpcall pairing used for drawRectImpl above.
    private static int registerUpcall(Class<?> owner, String method, MethodType type,
                                      FunctionDescriptor descriptor) throws ReflectiveOperationException {
        RuntimeForeignAccess.registerForDirectUpcall(
                MethodHandles.lookup().findStatic(owner, method, type), descriptor);
        return 1;
    }
}
