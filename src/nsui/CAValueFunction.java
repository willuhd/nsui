package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// CAValueFunction — named value transforms for property animations
/// (rotateX/Y/Z, scale, translate). Thin stateless wrapper.
public final class CAValueFunction extends NSObject {

    private CAValueFunction(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static CAValueFunction wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAValueFunction(peer);
    }

    /// functionWithName: (e.g. "rotateX", "scale", "translate").
    public static CAValueFunction functionWithName(String name) {
        return wrap(ObjC.msgSendIdId(ObjC.cls("CAValueFunction"),
                ObjC.sel("functionWithName:"), ObjC.nsstring(name)));
    }

    /// name (nil-safe).
    public String name() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("name")));
    }
}
