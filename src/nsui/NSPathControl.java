package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSPathControl — a control that displays a file system path. Thin, 1:1,
 * stateless wrapper over the native {@code NSPathControl}: each method maps to
 * one {@code objc_msgSend} selector. Follows the project template: volatile
 * initialized, synchronized ensureInit, ObjC.handle(Sig.of...), invokeExact,
 * static create/wrap.
 *
 * <p>Created via {@code [[NSPathControl alloc] initWithFrame:]}; configure
 * via {@code setURL:} / {@code URL} and {@code setPathStyle:}.
 */
public final class NSPathControl extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;  // (id, SEL, NSRect) -> id [initWithFrame:]
    private static MethodHandle hSetURL;     // (id, SEL, id) -> void [setURL:]
    private static MethodHandle hGetURL;     // (id, SEL) -> id      [URL]
    private static MethodHandle hSetPathStyle;// (id, SEL, long)-> void [setPathStyle:]
    private static MethodHandle hGetPathStyle;// (id, SEL) -> long [pathStyle]
    private static MethodHandle hSetDoubleAction; // (id, SEL, id) -> void [setDoubleAction:]
    private static MethodHandle hSetPlaceholder;  // (id, SEL, id) -> void [setPlaceholderString:]

    private NSPathControl(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /** Wrap an existing NSPathControl peer. */
    public static NSPathControl wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPathControl(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSetURL = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hGetURL = ObjC.handle(Sig.of(Ret.ID));
        hSetPathStyle = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hGetPathStyle = ObjC.handle(Sig.of(Ret.INT));
        hSetDoubleAction = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hSetPlaceholder = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /** {@code [[NSPathControl alloc] initWithFrame:frame]} — a new path control. */
    public static NSPathControl create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPathControl"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSPathControl", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSPathControl alloc/initWithFrame: returned nil");
        return new NSPathControl(p);
    }

    // ---------------------------------------------------------------- instance API

    /** [control setURL:] — NSURL peer (or nil to clear). */
    public void setURL(MemorySegment url) {
        try {
            MemorySegment u = (url == null || url.address() == 0) ? MemorySegment.NULL : url;
            hSetURL.invokeExact(peer, ObjC.sel("setURL:"), (MemorySegment) u);
        } catch (Throwable t) {
            throw new RuntimeException("setURL: failed", t);
        }
    }

    /** [control URL] — NSURL peer or nil. */
    public MemorySegment URL() {
        try {
            return (MemorySegment) hGetURL.invokeExact(peer, ObjC.sel("URL"));
        } catch (Throwable t) {
            throw new RuntimeException("URL failed", t);
        }
    }

    /** Convenience: set URL from a file system path string via NSURL fileURLWithPath:. */
    public void setURLPath(String path) {
        if (path == null) {
            setURL(MemorySegment.NULL);
            return;
        }
        MemorySegment url = ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("fileURLWithPath:"), ObjC.nsstring(path));
        setURL(url);
    }

    /** Convenience: get file system path string from NSURL via [URL path]. */
    public String URLPath() {
        MemorySegment url = URL();
        if (url == null || url.address() == 0) return null;
        MemorySegment path = ObjC.msgSendId(url, ObjC.sel("path"));
        return ObjC.toString(path);
    }

    /** [control pathStyle] — NSPathStyle (0=standard, 1=navigational, 2=popUp). */
    public long pathStyle() {
        try {
            return (long) hGetPathStyle.invokeExact(peer, ObjC.sel("pathStyle"));
        } catch (Throwable t) {
            throw new RuntimeException("pathStyle failed", t);
        }
    }

    /** [control setPathStyle:] — NSPathStyle. */
    public void setPathStyle(long style) {
        try {
            hSetPathStyle.invokeExact(peer, ObjC.sel("setPathStyle:"), style);
        } catch (Throwable t) {
            throw new RuntimeException("setPathStyle: failed", t);
        }
    }

    /** [control setDoubleAction:] — SEL for double-click on a path component. */
    public void setDoubleAction(String selector) {
        try {
            hSetDoubleAction.invokeExact(peer, ObjC.sel("setDoubleAction:"), (MemorySegment) (selector == null ? MemorySegment.NULL : ObjC.sel(selector)));
        } catch (Throwable t) {
            throw new RuntimeException("setDoubleAction: failed", t);
        }
    }

    /** [control doubleAction] — SEL id or nil. */
    public MemorySegment doubleAction() {
        try {
            return (MemorySegment) hGetURL.invokeExact(peer, ObjC.sel("doubleAction"));
        } catch (Throwable t) {
            throw new RuntimeException("doubleAction failed", t);
        }
    }

    /** [control placeholderString] — NSString. */
    public String placeholderString() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("placeholderString")));
    }

    /** [control setPlaceholderString:] */
    public void setPlaceholderString(String s) {
        try {
            hSetPlaceholder.invokeExact(peer, ObjC.sel("setPlaceholderString:"), (MemorySegment) (s == null ? MemorySegment.NULL : ObjC.nsstring(s)));
        } catch (Throwable t) {
            throw new RuntimeException("setPlaceholderString: failed", t);
        }
    }

    /** [control isEditable]. */
    public boolean isEditable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEditable"));
    }

    public void setEditable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEditable:"), flag);
    }

    /** [control allowedTypes] — NSArray of UTIs (id). */
    public MemorySegment allowedTypes() {
        return ObjC.msgSendId(peer, ObjC.sel("allowedTypes"));
    }

    public void setAllowedTypes(MemorySegment types) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAllowedTypes:"), (MemorySegment) (types == null ? MemorySegment.NULL : types));
    }

    /** [control clickedPathItem] — NSPathControlItem peer or nil. */
    public MemorySegment clickedPathItem() {
        return ObjC.msgSendId(peer, ObjC.sel("clickedPathItem"));
    }
}
