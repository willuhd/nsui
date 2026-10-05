package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// MTLLibrary — compiled shaders. Long-lived: compile once.
public final class MTLLibrary extends NSObject {

    private MTLLibrary(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static MTLLibrary wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new MTLLibrary(peer);
    }

    /// newFunctionWithName:.
    public MTLFunction newFunctionWithName(String name) {
        return MTLFunction.wrap(ObjC.msgSendIdId(peer,
                ObjC.sel("newFunctionWithName:"), ObjC.nsstring(name)));
    }
}
