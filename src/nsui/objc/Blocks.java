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
/// - Blocks whose `isa` is `_NSConcreteGlobalBlock` are never copied —
///   `Block_copy` returns `self` — so no copy/dispose functions are
/// needed and the block safely outlives the frame that created it.
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
    private static Arena ARENA;
    private static SymbolLookup SYSTEM;

    /// Address of the exported _NSConcreteGlobalBlock data symbol = the global-block class object.
    private static MemorySegment GLOBAL_BLOCK_ISA;

    /// Offset/size layout of the Block_descriptor for a capture-less global block.
    private static final long DESCRIPTOR_SIZE = 32; // reserved(8) + size(8) + copy(8) + dispose(8)

    private Blocks() {}

    /// Run-time init (native-image: no FFM work in static initializers).
    public static synchronized void ensureInit() {
        if (LINKER != null) return;
        LINKER = Linker.nativeLinker();
        ARENA = Arena.global();

        SYSTEM = SymbolLookup.libraryLookup("/usr/lib/libSystem.B.dylib", ARENA);
        GLOBAL_BLOCK_ISA = SYSTEM.find("_NSConcreteGlobalBlock")
                .orElseThrow(() -> new IllegalStateException("_NSConcreteGlobalBlock not exported by libSystem"));
        // The symbol IS the class object; its address (not its contents) is the isa.
        if (GLOBAL_BLOCK_ISA.address() == 0) {
            throw new IllegalStateException("_NSConcreteGlobalBlock resolved to NULL address");
        }
    }

    /// Build a global ObjC block backed by an FFM upcall stub.
    ///
    /// @param target   the Java method invoked by the block; it receives the block
    /// pointer (blockSelf) as its first argument. For `void(^)(void)`
    /// the target type is `(MemorySegment blockSelf) -> void`.
    /// @param fd       the FULL upcall descriptor INCLUDING the leading block-pointer
    /// parameter. e.g. `FunctionDescriptor.ofVoid(PTR)` for a
    /// no-arg void block.
    /// @return the block segment, allocated in the global arena (lives forever;
    /// strong references to it and the stub are held internally so GC can
    /// never collect them).
    public static MemorySegment block(MethodHandle target, FunctionDescriptor fd) {
        ensureInit();

        // Upcall stub that becomes the block's invoke function pointer.
        MemorySegment stub = LINKER.upcallStub(target, fd, ARENA);

        // Block struct size = header (32) + descriptor (32) = 64.
        long totalSize = 64;
        MemorySegment block = ARENA.allocate(totalSize, 16); // 16-byte aligned

        // isa = address of _NSConcreteGlobalBlock (the class object itself).
        block.set(ValueLayout.ADDRESS, 0, GLOBAL_BLOCK_ISA);
        block.set(ValueLayout.JAVA_INT, 8, 0);  // flags = 0
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
