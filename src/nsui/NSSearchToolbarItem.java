package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSearchToolbarItem — standard toolbar-integrated NSSearchField (macOS 11+).
/// Natively an NSToolbarItem subclass (assert isKindOfClass in tests); extends
/// NSObject directly because the toolkit's NSToolbarItem is final with a
/// private ctor and frozen files cannot be edited. Field layout is managed by the item.
/// Thin stateless wrapper: each method maps to one objc_msgSend selector.
/// Nothing omitted (the header's NS_UNAVAILABLE view override is not wrapped).
public final class NSSearchToolbarItem extends NSObject {

    private record Handles(MethodHandle hInitIdentifier, MethodHandle hDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    private NSSearchToolbarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }
    /// Wrap an existing peer (nil-safe).
    public static NSSearchToolbarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSearchToolbarItem(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
    }
    /// [[NSSearchToolbarItem alloc] initWithItemIdentifier:].
    public static NSSearchToolbarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSSearchToolbarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitIdentifier().invokeExact(p,
                    ObjC.sel("initWithItemIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithItemIdentifier: failed for NSSearchToolbarItem", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSSearchToolbarItem alloc/initWithItemIdentifier: returned nil");
        return new NSSearchToolbarItem(p);
    }
    /// itemIdentifier — the NSToolbarItem identifier string.
    public String itemIdentifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("itemIdentifier")));
    }
    /// searchField / setter (nil resets to the default field).
    public NSSearchField searchField() { return NSSearchField.wrap(ObjC.msgSendId(peer, ObjC.sel("searchField"))); }
    public void setSearchField(NSSearchField field) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSearchField:"), ObjC.nullablePeer(field));
    }

    /// resignsFirstResponderWithCancel / setter (default YES).
    public boolean resignsFirstResponderWithCancel() { return ObjC.msgSendBool(peer, ObjC.sel("resignsFirstResponderWithCancel")); }
    public void setResignsFirstResponderWithCancel(boolean f) { ObjC.msgSendVoidBool(peer, ObjC.sel("setResignsFirstResponderWithCancel:"), f); }

    /// preferredWidthForSearchField (CGFloat) — focus width.
    public double preferredWidthForSearchField() {
        ensureInit();
        try {
            return (double) handles.hDouble().invokeExact(peer, ObjC.sel("preferredWidthForSearchField"));
        } catch (Throwable t) {
            throw new RuntimeException("preferredWidthForSearchField failed", t);
        }
    }

    /// setPreferredWidthForSearchField:.
    public void setPreferredWidthForSearchField(double width) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setPreferredWidthForSearchField:"), width);
        } catch (Throwable t) {
            throw new RuntimeException("setPreferredWidthForSearchField: failed", t);
        }
    }

    /// begin/endSearchInteraction — focus the field / resign focus.
    public void beginSearchInteraction() { ObjC.msgSendVoid(peer, ObjC.sel("beginSearchInteraction")); }
    public void endSearchInteraction() { ObjC.msgSendVoid(peer, ObjC.sel("endSearchInteraction")); }
}
