package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTableCellView — a reusable table cell view: object value plus optional
/// text-field and image-view outlets. Thin stateless wrapper.
public final class NSTableCellView extends NSView {

    private record Handles(MethodHandle hInitFrame) {}
    private static volatile Handles handles;

    private NSTableCellView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSTableCellView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTableCellView(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.RECT)));
    }

    /// [[NSTableCellView alloc] initWithFrame:].
    public static NSTableCellView create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSTableCellView"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitFrame().invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSTableCellView", t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithFrame: returned nil for NSTableCellView");
        return new NSTableCellView(p);
    }

    /// objectValue — the represented object (raw peer: may be any class).
    public MemorySegment objectValue() {
        return ObjC.msgSendId(peer, ObjC.sel("objectValue"));
    }

    /// setObjectValue:.
    public void setObjectValue(MemorySegment value) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setObjectValue:"),
                (MemorySegment) (value == null ? MemorySegment.NULL : value));
    }

    /// textField outlet (raw peer; NSTextField has no wrap — peer-compare or re-wrap).
    public MemorySegment textField() {
        return ObjC.msgSendId(peer, ObjC.sel("textField"));
    }

    /// imageView outlet (raw peer, same caveat).
    public MemorySegment imageView() {
        return ObjC.msgSendId(peer, ObjC.sel("imageView"));
    }

    /// backgroundStyle (NSBackgroundStyle, raw).
    public long backgroundStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("backgroundStyle"));
    }

    /// setBackgroundStyle:.
    public void setBackgroundStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setBackgroundStyle:"), style);
    }

    /// rowSizeStyle (NSTableViewRowSizeStyle, raw).
    public long rowSizeStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("rowSizeStyle"));
    }

    /// setRowSizeStyle:.
    public void setRowSizeStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setRowSizeStyle:"), style);
    }
}
