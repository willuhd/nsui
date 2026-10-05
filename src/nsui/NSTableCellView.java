package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSTableCellView — a reusable table cell view: object value plus optional
/// text-field and image-view outlets. Thin stateless wrapper.
public final class NSTableCellView extends NSView {

    private NSTableCellView(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static NSTableCellView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTableCellView(peer);
    }

    /// [[NSTableCellView alloc] initWithFrame:].
    public static NSTableCellView create(NSRect frame) {
        return new NSTableCellView(ObjC.newView("NSTableCellView", frame));
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
