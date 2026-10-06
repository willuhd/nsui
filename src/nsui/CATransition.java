package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CATransition — animated layer content transitions (fade/push/reveal).
/// Thin stateless wrapper.
public class CATransition extends CAAnimation {

    /// Transition types.
    public static final String TYPE_FADE = "fade";
    public static final String TYPE_MOVE_IN = "moveIn";
    public static final String TYPE_PUSH = "push";
    public static final String TYPE_REVEAL = "reveal";

    /// Transition subtypes.
    public static final String FROM_LEFT = "fromLeft";
    public static final String FROM_RIGHT = "fromRight";
    public static final String FROM_TOP = "fromTop";
    public static final String FROM_BOTTOM = "fromBottom";

    private record Handles(MethodHandle hGetFloat, MethodHandle hSetFloat) {}
    private static volatile Handles handles;

    protected CATransition(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CATransition wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CATransition(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.FLOAT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT)));
    }

    /// [[CATransition alloc] init].
    public static CATransition create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("CATransition"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for CATransition");
        return new CATransition(p);
    }

    /// type (nil-safe).
    public String type() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("type")));
    }

    /// setType:.
    public void setType(String type) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setType:"), ObjC.nsstring(type));
    }

    /// subtype (nil-safe).
    public String subtype() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("subtype")));
    }

    /// setSubtype:.
    public void setSubtype(String subtype) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSubtype:"),
                subtype == null ? MemorySegment.NULL : ObjC.nsstring(subtype));
    }

    /// startProgress (0..1).
    public float startProgress() {
        try {
            return (float) handles.hGetFloat().invokeExact(peer, ObjC.sel("startProgress"));
        } catch (Throwable t) {
            throw new RuntimeException("startProgress failed", t);
        }
    }

    /// setStartProgress:.
    public void setStartProgress(float progress) {
        try {
            handles.hSetFloat().invokeExact(peer, ObjC.sel("setStartProgress:"), progress);
        } catch (Throwable t) {
            throw new RuntimeException("setStartProgress: failed", t);
        }
    }

    /// endProgress (0..1).
    public float endProgress() {
        try {
            return (float) handles.hGetFloat().invokeExact(peer, ObjC.sel("endProgress"));
        } catch (Throwable t) {
            throw new RuntimeException("endProgress failed", t);
        }
    }

    /// setEndProgress:.
    public void setEndProgress(float progress) {
        try {
            handles.hSetFloat().invokeExact(peer, ObjC.sel("setEndProgress:"), progress);
        } catch (Throwable t) {
            throw new RuntimeException("setEndProgress: failed", t);
        }
    }
}
