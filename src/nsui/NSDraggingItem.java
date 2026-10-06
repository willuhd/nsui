package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSDraggingItem — minimal wrapper over native `NSDraggingItem`.
/// Holds a pasteboard writer (typically NSPasteboardItem) and represents one dragged item.
///
/// Coverage notes (header: NSDraggingItem.h wins on API truth):
/// - Wrapped: initWithPasteboardWriter:, item, draggingFrame (+ setter),
///   setDraggingFrame:contents: and imageComponents.
/// - Omitted: init (NS_UNAVAILABLE by contract); imageComponentsProvider
///   (block-taking property needs upcall machinery); the
///   NSDraggingImageComponent key/contents/frame members (no wrapper in
///   this batch — imageComponents stays a raw NSArray).
public final class NSDraggingItem extends NSObject {

    private static volatile MethodHandle hInitWithWriter;

    private NSDraggingItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSDraggingItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDraggingItem(peer);
    }

    private static void ensureInit() {
        if (hInitWithWriter != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (hInitWithWriter != null) return;
        hInitWithWriter = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
    }

    /// [[NSDraggingItem alloc] initWithPasteboardWriter:writer]
    public static NSDraggingItem create(MemorySegment pasteboardWriter) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSDraggingItem"), ObjC.sel("alloc"));
        try {
            MemorySegment writerSeg = (pasteboardWriter == null || pasteboardWriter.address() == 0) ? MemorySegment.NULL : pasteboardWriter;
            p = (MemorySegment) hInitWithWriter.invokeExact(p, ObjC.sel("initWithPasteboardWriter:"), writerSeg);
        } catch (Throwable t) {
            throw new RuntimeException("NSDraggingItem initWithPasteboardWriter: failed", t);
        }
        return wrap(p);
    }

    /// Convenience: create with NSPasteboardItem wrapper.
    public static NSDraggingItem create(NSPasteboardItem item) {
        return create(item == null ? MemorySegment.NULL : item.peer());
    }

    /// Convenience: create dragging item with plain text for a UTI type (e.g. "public.plain-text").
    public static NSDraggingItem withString(String string, String type) {
        NSPasteboardItem item = NSPasteboardItem.withString(string, type);
        return create(item == null ? MemorySegment.NULL : item.peer());
    }

    /// item — the pasteboard writer this item was created with.
    public NSObject item() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            return NSObject.wrap((MemorySegment) h.invokeExact(peer, ObjC.sel("item")));
        } catch (Throwable t) {
            throw new RuntimeException("item failed", t);
        }
    }

    /// draggingFrame — the drag image frame.
    public NSRect draggingFrame() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.RECT));
            MemorySegment r = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("draggingFrame"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("draggingFrame failed", t);
        }
    }

    /// setDraggingFrame:.
    public void setDraggingFrame(NSRect frame) {
        ensureInit();
        if (frame == null) throw new IllegalArgumentException("frame is null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT));
            h.invokeExact(peer, ObjC.sel("setDraggingFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setDraggingFrame: failed", t);
        }
    }

    /// setDraggingFrame:contents: — frame plus drag-image contents (or null).
    public void setDraggingFrameContents(NSRect frame, MemorySegment contents) {
        ensureInit();
        if (frame == null) throw new IllegalArgumentException("frame is null");
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setDraggingFrame:contents:"), frame.toSegment(),
                    (MemorySegment) (contents == null ? MemorySegment.NULL : contents));
        } catch (Throwable t) {
            throw new RuntimeException("setDraggingFrame:contents: failed", t);
        }
    }

    /// imageComponents — the image components, or null when unset.
    public NSArray imageComponents() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            return NSArray.wrap((MemorySegment) h.invokeExact(peer, ObjC.sel("imageComponents")));
        } catch (Throwable t) {
            throw new RuntimeException("imageComponents failed", t);
        }
    }
}
