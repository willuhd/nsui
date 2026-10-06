package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMenuToolbarItem — a toolbar item presenting an NSMenu (macOS 10.15+).
/// Natively an NSToolbarItem subclass (assert isKindOfClass in tests); extends
/// NSObject directly because the toolkit's NSToolbarItem is final with a
/// private ctor and frozen files cannot be edited. Create pattern mirrors it.
/// Thin stateless wrapper: each method maps to one objc_msgSend selector.
/// Nothing omitted: the header declares only these two properties.
public final class NSMenuToolbarItem extends NSObject {

    private record Handles(MethodHandle hInitIdentifier) {}
    private static volatile Handles handles;

    private NSMenuToolbarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer (nil-safe).
    public static NSMenuToolbarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMenuToolbarItem(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID)));
    }

    /// [[NSMenuToolbarItem alloc] initWithItemIdentifier:].
    public static NSMenuToolbarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSMenuToolbarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitIdentifier().invokeExact(p,
                    ObjC.sel("initWithItemIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithItemIdentifier: failed for NSMenuToolbarItem", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSMenuToolbarItem alloc/initWithItemIdentifier: returned nil");
        return new NSMenuToolbarItem(p);
    }

    /// itemIdentifier — the NSToolbarItem identifier string.
    public String itemIdentifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("itemIdentifier")));
    }
    /// menu — the presented menu.
    public NSMenu menu() {
        return NSMenu.wrap(ObjC.msgSendId(peer, ObjC.sel("menu")));
    }

    /// setMenu:.
    public void setMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMenu:"),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
    }

    /// showsIndicator — arrow affordance for the menu (default YES).
    public boolean showsIndicator() {
        return ObjC.msgSendBool(peer, ObjC.sel("showsIndicator"));
    }

    /// setShowsIndicator:.
    public void setShowsIndicator(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setShowsIndicator:"), flag);
    }
}
