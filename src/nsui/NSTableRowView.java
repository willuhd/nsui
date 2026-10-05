package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTableRowView — draws one table row: selection, emphasis, drop targeting.
/// Thin stateless wrapper.
public final class NSTableRowView extends NSView {

    private record Handles(MethodHandle hInitFrame) {}
    private static volatile Handles handles;

    private NSTableRowView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSTableRowView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTableRowView(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.RECT)));
    }

    /// [[NSTableRowView alloc] initWithFrame:].
    public static NSTableRowView create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSTableRowView"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitFrame().invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSTableRowView", t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithFrame: returned nil for NSTableRowView");
        return new NSTableRowView(p);
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
