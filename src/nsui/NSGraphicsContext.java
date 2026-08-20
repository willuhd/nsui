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

    private static volatile boolean initialized;
    private static MethodHandle hCurrent;    // (id, SEL) -> id [currentContext]
    private static MethodHandle hCGContext;  // (id, SEL) -> id [CGContext]
    private static MethodHandle hSave;       // (id, SEL) -> void [saveGraphicsState]
    private static MethodHandle hRestore;    // (id, SEL) -> void [restoreGraphicsState]
    private static MethodHandle hWithCG;     // (id, SEL, id, bool) -> id [graphicsContextWithCGContext:flipped:]

    private NSGraphicsContext(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSGraphicsContext wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSGraphicsContext(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hCurrent = ObjC.handle(Sig.of(Ret.ID));
        hCGContext = ObjC.handle(Sig.of(Ret.ID));
        hSave = ObjC.handle(Sig.of(Ret.VOID));
        hRestore = ObjC.handle(Sig.of(Ret.VOID));
        hWithCG = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.BOOL));
        initialized = true;
    }

    /// +[NSGraphicsContext currentContext]
    public static NSGraphicsContext currentContext() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hCurrent.invokeExact(ObjC.cls("NSGraphicsContext"), ObjC.sel("currentContext"));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("currentContext failed", t);
        }
    }

    /// -CGContext -> CGContextRef as MemorySegment
    public MemorySegment CGContext() {
        ensureInit();
        try {
            return (MemorySegment) hCGContext.invokeExact(peer, ObjC.sel("CGContext"));
        } catch (Throwable t) {
            throw new RuntimeException("CGContext failed", t);
        }
    }

    /// -saveGraphicsState
    public void saveGraphicsState() {
        ensureInit();
        try {
            hSave.invokeExact(peer, ObjC.sel("saveGraphicsState"));
        } catch (Throwable t) {
            throw new RuntimeException("saveGraphicsState failed", t);
        }
    }

    /// -restoreGraphicsState
    public void restoreGraphicsState() {
        ensureInit();
        try {
            hRestore.invokeExact(peer, ObjC.sel("restoreGraphicsState"));
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
            MemorySegment p = (MemorySegment) hWithCG.invokeExact(ObjC.cls("NSGraphicsContext"), ObjC.sel("graphicsContextWithCGContext:flipped:"), cg, flipped);
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
