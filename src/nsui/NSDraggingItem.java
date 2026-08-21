package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSDraggingItem — minimal wrapper over native `NSDraggingItem`.
/// Holds a pasteboard writer (typically NSPasteboardItem) and represents one dragged item.
public final class NSDraggingItem extends NSObject {

    private static volatile MethodHandle hInitWithWriter;

    private NSDraggingItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSDraggingItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDraggingItem(peer);
    }

    private static synchronized void ensureInit() {
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
}
