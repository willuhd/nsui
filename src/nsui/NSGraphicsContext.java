package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSGraphicsContext — the drawing destination (screen, bitmap, PDF).
/// Thin stateless wrapper: each method maps to one objc_msgSend selector.
/// OMITTED: the deprecated graphicsPort/graphicsContextWithWindow:/focusStack
/// family (header-marked replacements exist and ARE wrapped) and the
/// setGraphicsState: no-op. All live drawing-state selectors are wrapped.
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

    /// +saveGraphicsState — push the current context on the per-thread stack.
    public static void saveCurrentGraphicsState() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID));
            h.invokeExact(ObjC.cls("NSGraphicsContext"), ObjC.sel("saveGraphicsState"));
        } catch (Throwable t) {
            throw new RuntimeException("saveGraphicsState failed", t);
        }
    }

    /// +restoreGraphicsState — pop the per-thread stack back to current.
    public static void restoreCurrentGraphicsState() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID));
            h.invokeExact(ObjC.cls("NSGraphicsContext"), ObjC.sel("restoreGraphicsState"));
        } catch (Throwable t) {
            throw new RuntimeException("restoreGraphicsState failed", t);
        }
    }

    /// +graphicsContextWithAttributes: — context for the given destination attributes.
    public static NSGraphicsContext graphicsContextWithAttributes(NSDictionary attributes) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(ObjC.cls("NSGraphicsContext"),
                    ObjC.sel("graphicsContextWithAttributes:"),
                    (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes.peer()));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("graphicsContextWithAttributes: failed", t);
        }
    }

    /// +graphicsContextWithBitmapImageRep: — bitmap-backed drawing context.
    public static NSGraphicsContext graphicsContextWithBitmapImageRep(NSBitmapImageRep rep) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(ObjC.cls("NSGraphicsContext"),
                    ObjC.sel("graphicsContextWithBitmapImageRep:"),
                    (MemorySegment) (rep == null ? MemorySegment.NULL : rep.peer()));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("graphicsContextWithBitmapImageRep: failed", t);
        }
    }

    /// setCurrentContext: — make this context current on this thread (null clears).
    public static void setCurrentContext(NSGraphicsContext ctx) {
        ensureInit();
        ObjC.msgSendVoidId(ObjC.cls("NSGraphicsContext"), ObjC.sel("setCurrentContext:"),
                (MemorySegment) (ctx == null ? MemorySegment.NULL : ctx.peer()));
    }

    /// attributes — the attributes this context was created with (nil-safe).
    public NSDictionary attributes() {
        ensureInit();
        return NSDictionary.wrap(ObjC.msgSendId(peer, ObjC.sel("attributes")));
    }

    /// flushGraphics — force pending drawing to the destination.
    public void flushGraphics() {
        ensureInit();
        ObjC.msgSendVoid(peer, ObjC.sel("flushGraphics"));
    }

    /// shouldAntialias.
    public boolean shouldAntialias() {
        ensureInit();
        return ObjC.msgSendBool(peer, ObjC.sel("shouldAntialias"));
    }

    /// setShouldAntialias:.
    public void setShouldAntialias(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, ObjC.sel("setShouldAntialias:"), flag);
    }

    /// imageInterpolation (NSImageInterpolation: 0=default, 1=none, 2=low, 3=high, 4=medium).
    public long imageInterpolation() {
        ensureInit();
        return ObjC.msgSendLong(peer, ObjC.sel("imageInterpolation"));
    }

    /// setImageInterpolation:.
    public void setImageInterpolation(long interpolation) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, ObjC.sel("setImageInterpolation:"), interpolation);
    }

    /// patternPhase — the pattern-drawing phase offset.
    public NSPoint patternPhase() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.POINT));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("patternPhase"));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("patternPhase failed", t);
        }
    }

    /// setPatternPhase:.
    public void setPatternPhase(NSPoint phase) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.POINT));
            h.invokeExact(peer, ObjC.sel("setPatternPhase:"), phase.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setPatternPhase: failed", t);
        }
    }

    /// compositingOperation (NSCompositingOperation).
    public long compositingOperation() {
        ensureInit();
        return ObjC.msgSendLong(peer, ObjC.sel("compositingOperation"));
    }

    /// setCompositingOperation:.
    public void setCompositingOperation(long op) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, ObjC.sel("setCompositingOperation:"), op);
    }

    /// colorRenderingIntent (NSColorRenderingIntent).
    public long colorRenderingIntent() {
        ensureInit();
        return ObjC.msgSendLong(peer, ObjC.sel("colorRenderingIntent"));
    }

    /// setColorRenderingIntent:.
    public void setColorRenderingIntent(long intent) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, ObjC.sel("setColorRenderingIntent:"), intent);
    }

    /// CIContext — the CoreImage context (raw peer, nil-safe; CIContext wrapper absent).
    public MemorySegment ciContext() {
        ensureInit();
        MemorySegment c = ObjC.msgSendId(peer, ObjC.sel("CIContext"));
        return (c == null || c.address() == 0) ? null : c;
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
