package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/// Low-level shim that builds Objective-C block-literal objects (the C block ABI)
/// purely via the Java FFM API, backed by FFM upcall stubs that call back into Java.
///
/// This lets any C/ObjC API that takes a block (e.g. `dispatch_async`)
/// invoke Java logic. Only the "global block" flavor is used:
/// - Blocks carry the `BLOCK_IS_GLOBAL` flag **and** the
///   `_NSConcreteGlobalBlock` isa, so `_Block_copy` returns `self` — no
/// copy/dispose functions are needed and the block safely outlives the frame
/// that created it. (libclosure branches on the flag; the isa alone is not
/// enough, and a block with `flags == 0` is silently heap-copied by every
/// retain — which also moves its address.)
/// - Avoids the `_NSConcreteStackBlock` (stack) flavor, which would require
///   copy/dispose trampolines and careful liveness handling.
///
/// Block struct layout (64-bit, this toolkit supports x86_64 and arm64):
/// ```
///   Block_layout {
///     void  *isa;        // +0   = address of the block class DATA symbol (the isa IS the address)
///     int    flags;      // +8
///     int    reserved;   // +12
///     void  *invoke;     // +16  = FFM upcall stub pointer
///     void  *descriptor; // +24  -> Block_descriptor
///   }
///   Block_descriptor {
///     unsigned long reserved; // +0
///     unsigned long size;     // +8  total size of the block struct incl. header + descriptor
///     void         *copy;     // +16 (NULL for global block with no captures)
///     void         *dispose;  // +24 (NULL)
///   }
/// ```
///
/// `_NSConcreteGlobalBlock` and `_NSConcreteStackBlock` are exported
/// DATA symbols of `/usr/lib/libSystem.B.dylib`; the isa value for a block of a
/// given class is the *address of that symbol itself* (the symbol is the block
/// class object) — it must NOT be dereferenced.
public final class Blocks {

    // ---- canonical layouts (resolved at run time) ----
    private static Linker LINKER;
    private static volatile boolean INIT;
    private static Arena ARENA;
    private static SymbolLookup SYSTEM;

    /// Address of the exported _NSConcreteGlobalBlock data symbol = the global-block class object.
    private static MemorySegment GLOBAL_BLOCK_ISA;

    /// Offset/size layout of the Block_descriptor for a capture-less global block.
    private static final long DESCRIPTOR_SIZE = 32; // reserved(8) + size(8) + copy(8) + dispose(8)

    /// `BLOCK_IS_GLOBAL` from `Block.h` — libclosure's `_Block_copy` returns the
    /// block unchanged only when this flag is set (the `_NSConcreteGlobalBlock`
    /// isa alone is not sufficient).
    private static final int BLOCK_IS_GLOBAL = 1 << 28;

    private Blocks() {}

    /// Run-time init (native-image: no FFM work in static initializers).
    public static void ensureInit() {
        if (INIT) return;
        ensureInitLocked();
    }

    public static synchronized void ensureInitLocked() {
        if (INIT) return;
        LINKER = Linker.nativeLinker();
        ARENA = Arena.global();

        SYSTEM = SymbolLookup.libraryLookup("/usr/lib/libSystem.B.dylib", ARENA);
        GLOBAL_BLOCK_ISA = SYSTEM.find("_NSConcreteGlobalBlock")
                .orElseThrow(() -> new IllegalStateException("_NSConcreteGlobalBlock not exported by libSystem"));
        // The symbol IS the class object; its address (not its contents) is the isa.
        if (GLOBAL_BLOCK_ISA.address() == 0) {
            throw new IllegalStateException("_NSConcreteGlobalBlock resolved to NULL address");
        }
        INIT = true;
    }

    /// Build a global ObjC block backed by an FFM upcall stub.
    ///
    /// @param target   the Java method invoked by the block; it receives the block
    /// pointer (blockSelf) as its first argument. For `void(^)(void)`
    /// the target type is `(MemorySegment blockSelf) -> void`.
    /// @param fd       the FULL upcall descriptor INCLUDING the leading block-pointer
    /// parameter. e.g. `FunctionDescriptor.ofVoid(PTR)` for a
    /// no-arg void block.
    /// @return the block segment, allocated in the global arena (never freed, so it
    /// safely outlives any frame; the arena itself roots the block and its stub, so
    /// GC cannot collect them — there is deliberately no Java field for them).
    ///
    /// CONTRACT: throwing out of `target` aborts the VM — an exception cannot cross
    /// the FFM upcall stub. Catch errors inside the block body.
    public static MemorySegment block(MethodHandle target, FunctionDescriptor fd) {
        return blockWithStub(stub(target, fd));
    }

    /// The upcall stub that becomes a block's `invoke` function pointer.
    ///
    /// The stub is per-TARGET, not per-block: a caller that installs many blocks with
    /// the same Java target should create it ONCE and reuse it via `blockWithStub`,
    /// which keeps the per-block cost down to the 64-byte block literal itself.
    public static MemorySegment stub(MethodHandle target, FunctionDescriptor fd) {
        ensureInit();
        return LINKER.upcallStub(target, fd, ARENA);
    }

    /// A capture-less global block whose `invoke` is an existing stub (see `stub`).
    /// The BLOCK must stay unique per install whenever the callback routes on the
    /// block's address.
    public static MemorySegment blockWithStub(MemorySegment stub) {
        ensureInit();
        if (stub == null || stub.address() == 0) {
            throw new IllegalArgumentException("blockWithStub: stub must be a non-null upcall stub");
        }

        // Block struct size = header (32) + descriptor (32) = 64.
        long totalSize = 64;
        MemorySegment block = ARENA.allocate(totalSize, 16); // 16-byte aligned

        // isa = address of _NSConcreteGlobalBlock (the class object itself).
        block.set(ValueLayout.ADDRESS, 0, GLOBAL_BLOCK_ISA);
        // flags: BLOCK_IS_GLOBAL (1 << 28) is what libclosure's _Block_copy
        // actually branches on — with flags == 0 it treats this as a STACK block
        // and silently heap-copies it on every retain (dispatch_async,
        // CATransaction setCompletionBlock:, AppKit sheet handlers), which both
        // moves the block's address and defeats any address-keyed routing.
        block.set(ValueLayout.JAVA_INT, 8, BLOCK_IS_GLOBAL);
        block.set(ValueLayout.JAVA_INT, 12, 0); // reserved = 0
        block.set(ValueLayout.ADDRESS, 16, stub); // invoke

        // Descriptor lives right after the 32-byte header.
        MemorySegment desc = block.asSlice(32, DESCRIPTOR_SIZE);
        desc.set(ValueLayout.JAVA_LONG, 0, 0L);       // reserved
        desc.set(ValueLayout.JAVA_LONG, 8, totalSize); // size (total block size incl. descriptor)
        desc.set(ValueLayout.ADDRESS, 16, MemorySegment.NULL); // copy = NULL
        desc.set(ValueLayout.ADDRESS, 24, MemorySegment.NULL); // dispose = NULL
        block.set(ValueLayout.ADDRESS, 24, desc);     // descriptor pointer

        return block;
    }
}
