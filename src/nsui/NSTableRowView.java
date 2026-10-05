package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSTableRowView — draws one table row: selection, emphasis, drop targeting.
/// Thin stateless wrapper.
public final class NSTableRowView extends NSView {

    private NSTableRowView(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static NSTableRowView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTableRowView(peer);
    }

    /// [[NSTableRowView alloc] initWithFrame:].
    public static NSTableRowView create(NSRect frame) {
        return new NSTableRowView(ObjC.newView("NSTableRowView", frame));
    }

    /// isSelected / setSelected:.
    public boolean isSelected() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSelected"));
    }

    /// setSelected:.
    public void setSelected(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setSelected:"), flag);
    }

    /// isEmphasized / setEmphasized:.
    public boolean isEmphasized() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEmphasized"));
    }

    /// setEmphasized:.
    public void setEmphasized(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEmphasized:"), flag);
    }

    /// isGroupRowStyle / setGroupRowStyle:.
    public boolean isGroupRowStyle() {
        return ObjC.msgSendBool(peer, ObjC.sel("isGroupRowStyle"));
    }

    /// setGroupRowStyle:.
    public void setGroupRowStyle(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setGroupRowStyle:"), flag);
    }

    /// selectionHighlightStyle (NSTableViewSelectionHighlightStyle, raw).
    public long selectionHighlightStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("selectionHighlightStyle"));
    }

    /// setSelectionHighlightStyle:.
    public void setSelectionHighlightStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setSelectionHighlightStyle:"), style);
    }
}
