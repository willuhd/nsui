package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSComboButton (macOS 13+) — a button with a menu indicator: the leading
/// segment performs the action, the trailing segment shows the menu.
/// Thin stateless wrapper; all behavior is AppKit's.
public final class NSComboButton extends NSControl {

    /// Button styles (NSComboButtonStyle).
    public static final long STYLE_SPLIT = 0;
    public static final long STYLE_UNIFIED = 1;

    private record Handles(MethodHandle hInitFrame) {}
    private static volatile Handles handles;

    private NSComboButton(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSComboButton wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSComboButton(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.RECT)));
    }

    /// [[NSComboButton alloc] initWithFrame:].
    public static NSComboButton create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSComboButton"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitFrame().invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSComboButton", t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithFrame: returned nil for NSComboButton");
        return new NSComboButton(p);
    }

    /// title.
    public String title() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("title")));
    }

    /// setTitle:.
    public void setTitle(String title) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTitle:"), ObjC.nsstring(title));
    }

    /// menu — shown from the trailing segment.
    public NSMenu menu() {
        return NSMenu.wrap(ObjC.msgSendId(peer, ObjC.sel("menu")));
    }

    /// setMenu:.
    public void setMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMenu:"),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
    }

    /// style — STYLE_SPLIT or STYLE_UNIFIED.
    public long style() {
        return ObjC.msgSendLong(peer, ObjC.sel("style"));
    }

    /// setStyle:.
    public void setStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setStyle:"), style);
    }
}
