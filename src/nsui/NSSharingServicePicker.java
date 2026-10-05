package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSharingServicePicker — the standard share menu/UI anchored to a view.
/// Thin stateless wrapper. Showing presents system UI; construct freely.
public final class NSSharingServicePicker extends NSObject {

    private record Handles(MethodHandle hInitItems, MethodHandle hShow) {}
    private static volatile Handles handles;

    private NSSharingServicePicker(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSSharingServicePicker wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSharingServicePicker(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.ID, Arg.INT)));
    }

    /// initWithItems:.
    public static NSSharingServicePicker withItems(NSArray items) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSSharingServicePicker"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitItems().invokeExact(p, ObjC.sel("initWithItems:"),
                    (MemorySegment) (items == null ? MemorySegment.NULL : items.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("initWithItems: failed for NSSharingServicePicker", t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithItems: returned nil");
        return new NSSharingServicePicker(p);
    }

    /// showRelativeToRect:ofView:preferredEdge: — presents the picker UI.
    public void show(NSRect rect, NSView anchor, long edge) {
        try {
            handles.hShow().invokeExact(peer, ObjC.sel("showRelativeToRect:ofView:preferredEdge:"),
                    rect.toSegment(),
                    (MemorySegment) (anchor == null ? MemorySegment.NULL : anchor.peer()), edge);
        } catch (Throwable t) {
            throw new RuntimeException("showRelativeToRect:ofView:preferredEdge: failed", t);
        }
    }

    /// standardShareMenuItem (macOS 13+, nil-safe).
    public NSMenuItem standardShareMenuItem() {
        return NSMenuItem.wrap(ObjC.msgSendId(peer, ObjC.sel("standardShareMenuItem")));
    }
}
