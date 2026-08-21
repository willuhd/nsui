package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSGraphicsContext — minimal wrapper over AppKit NSGraphicsContext.
/// Provides currentContext and CGContext access.
public final class NSGraphicsContext extends NSObject {

            private record Handles(MethodHandle hCurrent, MethodHandle hSave, MethodHandle hWithCG) {}
    private static volatile Handles handles;

    private NSGraphicsContext(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSGraphicsContext wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSGraphicsContext(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.VOID)), ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.BOOL)));
    }

    /// +[NSGraphicsContext currentContext]
    public static NSGraphicsContext currentContext() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hCurrent().invokeExact(ObjC.cls("NSGraphicsContext"), ObjC.sel("currentContext"));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("currentContext failed", t);
        }
    }

    /// -CGContext -> CGContextRef as MemorySegment
    public MemorySegment CGContext() {
        ensureInit();
        try {
            return (MemorySegment) handles.hCurrent().invokeExact(peer, ObjC.sel("CGContext"));
        } catch (Throwable t) {
            throw new RuntimeException("CGContext failed", t);
        }
    }

    /// -saveGraphicsState
    public void saveGraphicsState() {
        ensureInit();
        try {
            handles.hSave().invokeExact(peer, ObjC.sel("saveGraphicsState"));
        } catch (Throwable t) {
            throw new RuntimeException("saveGraphicsState failed", t);
        }
    }

    /// -restoreGraphicsState
    public void restoreGraphicsState() {
        ensureInit();
        try {
            handles.hSave().invokeExact(peer, ObjC.sel("restoreGraphicsState"));
        } catch (Throwable t) {
            throw new RuntimeException("restoreGraphicsState failed", t);
        }
    }

    /// Static helper: saveGraphicsState class-side via current context
    public static void save() {
        NSGraphicsContext ctx = currentContext();
        if (ctx != null) ctx.saveGraphicsState();
    }

    /// Static helper: restoreGraphicsState
    public static void restore() {
        NSGraphicsContext ctx = currentContext();
        if (ctx != null) ctx.restoreGraphicsState();
    }

    /// +[NSGraphicsContext graphicsContextWithCGContext:flipped:]
    public static NSGraphicsContext graphicsContextWithCGContextFlipped(MemorySegment cgContext, boolean flipped) {
        ensureInit();
        try {
            MemorySegment cg = (cgContext == null ? MemorySegment.NULL : cgContext);
            MemorySegment p = (MemorySegment) handles.hWithCG().invokeExact(ObjC.cls("NSGraphicsContext"), ObjC.sel("graphicsContextWithCGContext:flipped:"), cg, flipped);
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("graphicsContextWithCGContext:flipped: failed", t);
        }
    }

    /// [context isDrawingToScreen]
    public boolean isDrawingToScreen() {
        ensureInit();
        return ObjC.msgSendBool(peer, ObjC.sel("isDrawingToScreen"));
    }

    /// [context isFlipped]
    public boolean isFlipped() {
        ensureInit();
        return ObjC.msgSendBool(peer, ObjC.sel("isFlipped"));
    }

    /// [NSGraphicsContext currentContextDrawingToScreen] -> BOOL class method
    public static boolean currentContextDrawingToScreen() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL));
            return (boolean) h.invokeExact(ObjC.cls("NSGraphicsContext"), ObjC.sel("currentContextDrawingToScreen"));
        } catch (Throwable t) {
            throw new RuntimeException("currentContextDrawingToScreen failed", t);
        }
    }
}
