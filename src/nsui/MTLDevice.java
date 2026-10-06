package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// MTLDevice — the GPU: shader libraries, queues, textures, pipelines.
/// The device handle is +1 retained (NS_RETURNS_RETAINED) and held for the
/// process by convention here: immortal, bounded (one per GPU), documented.
public final class MTLDevice extends NSObject {

    private record Handles(MethodHandle hLibSource, MethodHandle hQueue,
            MethodHandle hTexture, MethodHandle hPipeline, MethodHandle hDepthStencil,
            MethodHandle hBuffer) {}
    private static volatile Handles handles;
    private static volatile MethodHandle hCreate;
    private static volatile boolean ready;

    private MTLDevice(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static MTLDevice wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLDevice(peer);
    }

    private static void ensureInit() {
        if (ready) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (ready) return;
        try { ObjC.ensureFramework("Metal"); } catch (Throwable ignored) {}
        SymbolLookup metal = SymbolLookup.libraryLookup(
                "/System/Library/Frameworks/Metal.framework/Metal", Arena.global());
        hCreate = Linker.nativeLinker().downcallHandle(
                metal.find("MTLCreateSystemDefaultDevice").orElseThrow(
                        () -> new IllegalStateException("MTLCreateSystemDefaultDevice not found")),
                nsui.objc.NsuiForeign.mtlCreateSystemDefaultDevice());
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.INT)));
        ready = true;
    }

    /// The system default GPU (nil on headless VMs without one).
    public static MTLDevice systemDefault() {
        ensureInit();
        try {
            return wrap((MemorySegment) hCreate.invokeExact());
        } catch (Throwable t) {
            throw new RuntimeException("MTLCreateSystemDefaultDevice failed", t);
        }
    }

    private static String errorMessage(MemorySegment errPtr) {
        if (errPtr.address() == 0) return "unknown Metal error";
        MemorySegment err = errPtr.get(java.lang.foreign.ValueLayout.ADDRESS, 0);
        if (err.address() == 0) return "unknown Metal error";
        String msg = ObjC.toString(ObjC.msgSendId(err, ObjC.sel("localizedDescription")));
        return msg == null ? "unknown Metal error" : msg;
    }

    /// A zeroed NSError** out-param. The scratch buffer is a *reused* bump
    /// arena whose contents survive a rewind, so the slot MUST be cleared
    /// before every call: otherwise a failed call that leaves *error untouched
    /// (or a success followed by a read) would read last turn's stale pointer
    /// and report the wrong message — or dereference a dangling error object.
    private static MemorySegment errorSlot() {
        MemorySegment slot = nsui.objc.Scratch.allocInput(8);
        slot.set(java.lang.foreign.ValueLayout.ADDRESS, 0, MemorySegment.NULL);
        return slot;
    }

    /// newLibraryWithSource:options:error: — compile MSL source (nil options).
    /// Throws with the compiler message on failure (never returns nil silently).
    public MTLLibrary newLibraryWithSource(String source) {
        ensureInit();
        MemorySegment err = errorSlot();
        try {
            MemorySegment lib = (MemorySegment) handles.hLibSource().invokeExact(peer,
                    ObjC.sel("newLibraryWithSource:options:error:"),
                    ObjC.nsstring(source), MemorySegment.NULL, err);
            if (lib.address() == 0) throw new RuntimeException("Metal shader compile failed: " + errorMessage(err));
            return MTLLibrary.wrap(lib);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("newLibraryWithSource: failed", t);
        }
    }

    /// newCommandQueue (long-lived: hold it).
    public MTLCommandQueue newCommandQueue() {
        return MTLCommandQueue.wrap(ObjC.msgSendId(peer, ObjC.sel("newCommandQueue")));
    }

    /// newBufferWithLength:options: — MTLResourceStorageModeShared (0) for
    /// CPU-writable vertex/index buffers.
    public MTLBuffer newBuffer(long length, long options) {
        ensureInit();
        try {
            MemorySegment b = (MemorySegment) handles.hBuffer().invokeExact(peer,
                    ObjC.sel("newBufferWithLength:options:"), length, options);
            if (b.address() == 0) throw new IllegalStateException("newBufferWithLength:options: returned nil");
            return MTLBuffer.wrap(b);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("newBufferWithLength:options: failed", t);
        }
    }

    /// newTextureWithDescriptor:.
    public MTLTexture newTexture(MTLTextureDescriptor descriptor) {
        if (descriptor == null) throw new IllegalArgumentException("descriptor is null");
        try {
            return MTLTexture.wrap((MemorySegment) handles.hTexture().invokeExact(peer,
                    ObjC.sel("newTextureWithDescriptor:"), descriptor.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("newTextureWithDescriptor: failed", t);
        }
    }

    /// newRenderPipelineStateWithDescriptor:error: — throws with message on failure.
    public MTLRenderPipelineState newRenderPipelineState(MTLRenderPipelineDescriptor descriptor) {
        ensureInit();
        if (descriptor == null) throw new IllegalArgumentException("descriptor is null");
        MemorySegment err = errorSlot();
        try {
            MemorySegment ps = (MemorySegment) handles.hPipeline().invokeExact(peer,
                    ObjC.sel("newRenderPipelineStateWithDescriptor:error:"), descriptor.peer(), err);
            if (ps.address() == 0)
                throw new RuntimeException("Metal pipeline build failed: " + errorMessage(err));
            return MTLRenderPipelineState.wrap(ps);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("newRenderPipelineStateWithDescriptor: failed", t);
        }
    }

    /// newDepthStencilStateWithDescriptor: — compile a depth/stencil state.
    /// Unlike the pipeline/error paths this selector has no NSError out-param,
    /// so a nil return is a hard failure (invalid descriptor combination):
    /// throw instead of handing back a silently unusable null state.
    public MTLDepthStencilState newDepthStencilState(MTLDepthStencilDescriptor descriptor) {
        ensureInit();
        if (descriptor == null) throw new IllegalArgumentException("descriptor is null");
        try {
            MemorySegment state = (MemorySegment) handles.hDepthStencil().invokeExact(peer,
                    ObjC.sel("newDepthStencilStateWithDescriptor:"), descriptor.peer());
            if (state.address() == 0)
                throw new IllegalStateException("newDepthStencilStateWithDescriptor: returned nil "
                        + "(invalid depth/stencil descriptor)");
            return MTLDepthStencilState.wrap(state);
        } catch (RuntimeException t) {
            throw t;
        } catch (Throwable t) {
            throw new RuntimeException("newDepthStencilStateWithDescriptor: failed", t);
        }
    }
}
