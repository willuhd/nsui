package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSWindowController — minimal wrap over AppKit NSWindowController.
/// Thin 1:1, stateless.
public final class NSWindowController extends NSObject {

            private record Handles(MethodHandle hInitWindow, MethodHandle hId, MethodHandle hVoidId) {}
    private static volatile Handles handles;

    private NSWindowController(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSWindowController wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSWindowController(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID)), ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.VOID, Arg.ID)));
    }

    /// alloc + init — empty controller.
    public static NSWindowController create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSWindowController"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p == null || p.address() == 0) throw new IllegalStateException("NSWindowController alloc/init returned nil");
        return new NSWindowController(p);
    }

    /// alloc + initWithWindow:
    public static NSWindowController initWithWindow(NSWindow window) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSWindowController"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitWindow().invokeExact(p, ObjC.sel("initWithWindow:"), (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("initWithWindow: failed", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSWindowController initWithWindow: returned nil");
        return new NSWindowController(p);
    }

    /// window — NSWindow or null.
    public NSWindow window() {
        ensureInit();
        try {
            MemorySegment w = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("window"));
            return NSWindow.wrap(w);
        } catch (Throwable t) {
            throw new RuntimeException("window failed", t);
        }
    }

    /// setWindow:
    public void setWindow(NSWindow window) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setWindow:"), (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setWindow: failed", t);
        }
    }

    /// showWindow: — sender may be null.
    public void showWindow(NSObject sender) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("showWindow:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("showWindow: failed", t);
        }
    }

    /// showWindow: convenience with null sender.
    public void showWindow() {
        showWindow(null);
    }

    /// setDocument: — NSDocument.
    public void setDocument(NSDocument document) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setDocument:"), (MemorySegment) (document == null ? MemorySegment.NULL : document.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setDocument: failed", t);
        }
    }

    /// document — raw id.
    public MemorySegment documentPeer() {
        ensureInit();
        try {
            return (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("document"));
        } catch (Throwable t) {
            throw new RuntimeException("document failed", t);
        }
    }

    public NSDocument document() {
        MemorySegment d = documentPeer();
        return NSDocument.wrap(d);
    }

    /// isWindowLoaded
    public boolean isWindowLoaded() {
        return ObjC.msgSendBool(peer, ObjC.sel("isWindowLoaded"));
    }
}
