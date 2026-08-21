package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CATextLayer — thin wrapper for QuartzCore CATextLayer: plain or attributed
/// string content rendered inside a layer. Font accepts a raw CGFont/CTFont
/// pointer (NSFont currently exposes no cgFont accessor — noted gap).
public class CATextLayer extends CALayer {

    // Same-shape selectors share handles across this wrapper.
    private record Handles(MethodHandle hGetId, MethodHandle hSetId, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetBool, MethodHandle hSetBool, MethodHandle hKind) {}
    private static volatile Handles handles;

    protected CATextLayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static CATextLayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CATextLayer(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID))
        );
    }

    /// +[CATextLayer layer]
    public static CATextLayer create() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetId().invokeExact(ObjC.cls("CATextLayer"), ObjC.sel("layer"));
            return wrap(p);
        } catch (Throwable t) { throw new RuntimeException("CATextLayer layer failed", t); }
    }

    /// [layer string] — plain text content, or null when the content is an
    /// NSAttributedString (it does not answer UTF8String; read the raw peer
    /// via the inherited wrapper if you need attributed details).
    public String string() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("string"));
            if (s == null || s.address() == 0) return null;
            boolean isNsString = (boolean) handles.hKind().invokeExact(s, ObjC.sel("isKindOfClass:"), ObjC.cls("NSString"));
            return isNsString ? ObjC.toString(s) : null;
        } catch (Throwable t) { throw new RuntimeException("string failed", t); }
    }

    /// [layer setString:] — plain text content.
    public void setString(String text) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setString:"), (MemorySegment) (text == null ? MemorySegment.NULL : ObjC.nsstring(text)));
        } catch (Throwable t) { throw new RuntimeException("setString: failed", t); }
    }

    /// Attributed content, raw NSAttributedString. DEVIATION: macOS CATextLayer
    /// declares no `setAttributedString:` selector (that property is iOS-only);
    /// on macOS attributed strings are installed through setString:, so this
    /// routes there. Passing an NSString here is also legal (plain content).
    public void setAttributedString(MemorySegment attrString) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setString:"), (MemorySegment) (attrString == null ? MemorySegment.NULL : attrString));
        } catch (Throwable t) { throw new RuntimeException("setAttributedString failed", t); }
    }

    /// [layer font] — raw font object (CGFontRef / CTFontRef) or null.
    public MemorySegment font() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("font"));
        } catch (Throwable t) { throw new RuntimeException("font failed", t); }
    }

    /// [layer setFont:] — raw CGFontRef/CTFontRef. GAP: NSFont has no cgFont()
    /// accessor yet, so callers must supply the raw pointer themselves
    /// (an NSFont peer is toll-free bridged with CTFont on macOS 10.11+).
    public void setFont(MemorySegment rawFont) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setFont:"), (MemorySegment) (rawFont == null ? MemorySegment.NULL : rawFont));
        } catch (Throwable t) { throw new RuntimeException("setFont: failed", t); }
    }

    /// [layer fontSize]
    public double fontSize() {
        ensureInit();
        try {
            return (double) handles.hGetDouble().invokeExact(peer, ObjC.sel("fontSize"));
        } catch (Throwable t) { throw new RuntimeException("fontSize failed", t); }
    }

    /// [layer setFontSize:]
    public void setFontSize(double size) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setFontSize:"), size);
        } catch (Throwable t) { throw new RuntimeException("setFontSize: failed", t); }
    }

    /// [layer alignmentMode] — "left", "center", "right", "justified" or "natural".
    public String alignmentMode() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("alignmentMode")));
        } catch (Throwable t) { throw new RuntimeException("alignmentMode failed", t); }
    }

    /// [layer setAlignmentMode:]
    public void setAlignmentMode(String mode) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setAlignmentMode:"), (MemorySegment) (mode == null ? MemorySegment.NULL : ObjC.nsstring(mode)));
        } catch (Throwable t) { throw new RuntimeException("setAlignmentMode: failed", t); }
    }

    /// [layer truncationMode] — "none", "start", "middle" or "end".
    public String truncationMode() {
        ensureInit();
        try {
            return ObjC.toString((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("truncationMode")));
        } catch (Throwable t) { throw new RuntimeException("truncationMode failed", t); }
    }

    /// [layer setTruncationMode:]
    public void setTruncationMode(String mode) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setTruncationMode:"), (MemorySegment) (mode == null ? MemorySegment.NULL : ObjC.nsstring(mode)));
        } catch (Throwable t) { throw new RuntimeException("setTruncationMode: failed", t); }
    }

    /// [layer isWrapped]
    public boolean isWrapped() {
        ensureInit();
        try {
            return (boolean) handles.hGetBool().invokeExact(peer, ObjC.sel("isWrapped"));
        } catch (Throwable t) { throw new RuntimeException("isWrapped failed", t); }
    }

    /// [layer setWrapped:]
    public void setWrapped(boolean flag) {
        ensureInit();
        try {
            handles.hSetBool().invokeExact(peer, ObjC.sel("setWrapped:"), flag);
        } catch (Throwable t) { throw new RuntimeException("setWrapped: failed", t); }
    }

    /// [layer foregroundColor] — raw CGColorRef or null.
    public MemorySegment foregroundColor() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("foregroundColor"));
        } catch (Throwable t) { throw new RuntimeException("foregroundColor failed", t); }
    }

    /// [layer setForegroundColor:] — raw CGColorRef (NULL clears).
    public void setForegroundColor(MemorySegment cgColor) {
        ensureInit();
        try {
            handles.hSetId().invokeExact(peer, ObjC.sel("setForegroundColor:"), (MemorySegment) (cgColor == null ? MemorySegment.NULL : cgColor));
        } catch (Throwable t) { throw new RuntimeException("setForegroundColor: failed", t); }
    }

    /// Convenience: set foreground color from NSColor via its CGColor.
    public void setForegroundColor(NSColor color) {
        setForegroundColor((MemorySegment) (color == null ? MemorySegment.NULL : color.cgColor()));
    }
}
