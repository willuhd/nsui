package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSRunLoop (Foundation) — the thread event loop carriers attach to
/// (display links, timers, ports). Thin stateless wrapper; only what
/// attachment call sites need.
public final class NSRunLoop extends NSObject {

    /// The default run-loop mode.
    public static final String DEFAULT_MODE = "kCFRunLoopDefaultMode";

    private NSRunLoop(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static NSRunLoop wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSRunLoop(peer);
    }

    /// currentRunLoop — this thread's loop.
    public static NSRunLoop current() {
        return wrap(ObjC.msgSendId(ObjC.cls("NSRunLoop"), ObjC.sel("currentRunLoop")));
    }

    /// mainRunLoop.
    public static NSRunLoop main() {
        return wrap(ObjC.msgSendId(ObjC.cls("NSRunLoop"), ObjC.sel("mainRunLoop")));
    }
}
