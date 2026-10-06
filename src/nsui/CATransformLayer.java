package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CATransformLayer — 3D-transform flattening container (no extra properties;
/// sublayers keep true 3D transforms instead of flattening to the plane).
public class CATransformLayer extends CALayer {

    protected CATransformLayer(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static CATransformLayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CATransformLayer(peer);
    }

    private static volatile boolean ready;

    private static void ensureFramework() {
        if (ready) return;
        ensureFrameworkLocked();
    }

    private static synchronized void ensureFrameworkLocked() {
        if (ready) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        ready = true;
    }

    /// [[CATransformLayer alloc] init].
    public static CATransformLayer create() {
        ensureFramework();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("CATransformLayer"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for CATransformLayer");
        return new CATransformLayer(p);
    }
}
