package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSGradient — minimal wrapper over AppKit NSGradient.
/// Provides creation and drawing.
public final class NSGradient extends NSObject {

            private record Handles(MethodHandle hInitTwo, MethodHandle hInitColors, MethodHandle hDrawRectAngle, MethodHandle hDrawBezierAngle) {}
    private static volatile Handles handles;

    private NSGradient(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSGradient wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSGradient(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.DOUBLE))
        );
    }

    /// [[NSGradient alloc] initWithStartingColor:endingColor:]
    public static NSGradient initWithStartingColorEndingColor(NSColor starting, NSColor ending) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSGradient"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInitTwo().invokeExact(alloc, ObjC.sel("initWithStartingColor:endingColor:"),
                    (MemorySegment) (starting == null ? MemorySegment.NULL : starting.peer()),
                    (MemorySegment) (ending == null ? MemorySegment.NULL : ending.peer()));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSGradient initWithStartingColor:endingColor: returned nil");
            return new NSGradient(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithStartingColor:endingColor: failed", t);
        }
    }

    /// [[NSGradient alloc] initWithColors:] with NSArray of NSColor
    public static NSGradient initWithColors(NSArray colors) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSGradient"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInitColors().invokeExact(alloc, ObjC.sel("initWithColors:"), colors.peer());
            if (p == null || p.address() == 0) throw new IllegalStateException("NSGradient initWithColors: returned nil");
            return new NSGradient(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithColors: failed", t);
        }
    }

    /// -drawInRect:angle:
    public void drawInRectAngle(NSRect rect, double angle) {
        ensureInit();
        try {
            handles.hDrawRectAngle().invokeExact(peer, ObjC.sel("drawInRect:angle:"), rect.toSegment(), angle);
        } catch (Throwable t) {
            throw new RuntimeException("drawInRect:angle: failed", t);
        }
    }

    /// -drawInBezierPath:angle:
    public void drawInBezierPathAngle(NSBezierPath path, double angle) {
        ensureInit();
        try {
            handles.hDrawBezierAngle().invokeExact(peer, ObjC.sel("drawInBezierPath:angle:"),
                    (MemorySegment) (path == null ? MemorySegment.NULL : path.peer()), angle);
        } catch (Throwable t) {
            throw new RuntimeException("drawInBezierPath:angle: failed", t);
        }
    }

    /// -drawFromPoint:toPoint:options: helper via generic invoke if needed; simplified to no-op variant
    public void drawFromPointToPoint(NSPoint start, NSPoint end) {
        // Uses NSGradient's drawFromPoint:toPoint:options: — fallback via ObjC.invoke
        // For minimal wrapper we expose the rect variant primarily.
        ensureInit();
        // Not implemented via handle to avoid extra Sig; use fallback
        try {
            MemorySegment sel = ObjC.sel("drawFromPoint:toPoint:options:");
            // This would require POINT,POINT,INT signature not in vocab; keep as unsupported
            throw new UnsupportedOperationException("drawFromPoint:toPoint:options: not in minimal vocab — use drawInRect:angle:");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
