package nsui.objc;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeForeignAccess;

/**
 * Build-time registration of every FFM call the app makes — the "no tracing
 * agent" path. Native-image must generate the native call stubs at build time,
 * so every downcall/upcall {@code FunctionDescriptor} used at run time is
 * registered here with {@link RuntimeForeignAccess}.
 *
 * <p>Everything derives from single-source-of-truth lists, so the registered set
 * can never drift from the runtime set:
 * <ul>
 *   <li>{@link NsuiForeign#RUNTIME} / {@link NsuiForeign#CORE} — C-function downcalls;</li>
 *   <li>{@link Sig#VOCABULARY} — one descriptor per message signature (this is
 *       where the whole objc_msgSend surface comes from);</li>
 *   <li>the delegate upcalls, registered explicitly (upcall stubs are per target
 *       method, not per signature).</li>
 * </ul>
 *
 * <p>Register the feature with {@code --features=nsui.objc.NsuiFeature}.
 * This class runs only inside the image builder; it never ends up in the image.
 */
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
                    MethodHandles.lookup().findStatic(DelegateProxy.class, "dispatchIdId",
                            MethodType.methodType(MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class)),
                    NsuiForeign.delegateIdIdUpcall());
            upcalls++;
            System.out.println("[NsuiFeature] direct upcalls registered (" + upcalls + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("cannot register direct upcall target", e);
        }
        System.out.println("[NsuiFeature] duringSetup done");
    }
}
