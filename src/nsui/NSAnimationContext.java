package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSAnimationContext — minimal wrapper over AppKit NSAnimationContext.
/// Provides currentContext, duration, and grouping.
public final class NSAnimationContext extends NSObject {

            private record Handles(MethodHandle hCurrent, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hVoid, MethodHandle hBool, MethodHandle hVoidBool) {}
    private static volatile Handles handles;

    private NSAnimationContext(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSAnimationContext wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSAnimationContext(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL))
        );
    }

    /// +[NSAnimationContext currentContext]
    public static NSAnimationContext currentContext() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hCurrent().invokeExact(ObjC.cls("NSAnimationContext"), ObjC.sel("currentContext"));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("currentContext failed", t);
        }
    }

    /// +[NSAnimationContext beginGrouping]
    public static void beginGrouping() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID));
            h.invokeExact(ObjC.cls("NSAnimationContext"), ObjC.sel("beginGrouping"));
        } catch (Throwable t) {
            throw new RuntimeException("beginGrouping failed", t);
        }
    }

    /// +[NSAnimationContext endGrouping]
    public static void endGrouping() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID));
            h.invokeExact(ObjC.cls("NSAnimationContext"), ObjC.sel("endGrouping"));
        } catch (Throwable t) {
            throw new RuntimeException("endGrouping failed", t);
        }
    }

    /// -duration
    public double duration() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("duration"));
        } catch (Throwable t) {
            throw new RuntimeException("duration failed", t);
        }
    }

    /// -setDuration:
    public void setDuration(double d) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setDuration:"), d);
        } catch (Throwable t) {
            throw new RuntimeException("setDuration: failed", t);
        }
    }

    /// -completionHandler / setCompletionHandler: — simplified to void/bool helpers if needed
    public boolean allowsImplicitAnimation() {
        ensureInit();
        try {
            return (boolean) handles.hBool().invokeExact(peer, ObjC.sel("allowsImplicitAnimation"));
        } catch (Throwable t) {
            throw new RuntimeException("allowsImplicitAnimation failed", t);
        }
    }

    public void setAllowsImplicitAnimation(boolean flag) {
        ensureInit();
        try {
            handles.hVoidBool().invokeExact(peer, ObjC.sel("setAllowsImplicitAnimation:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsImplicitAnimation: failed", t);
        }
    }

    /// Run animation group helper: beginGrouping() / setDuration / block / endGrouping
    public static void runAnimationGroup(java.lang.Runnable changes, double duration) {
        beginGrouping();
        try {
            NSAnimationContext ctx = currentContext();
            if (ctx != null) ctx.setDuration(duration);
            changes.run();
        } finally {
            endGrouping();
        }
    }

    public static void runAnimationGroup(java.lang.Runnable changes) {
        runAnimationGroup(changes, 0.25);
    }
}
