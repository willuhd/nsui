package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPopoverTouchBarItem — a Touch Bar item that opens a second `NSTouchBar`
/// in place of the currently visible one. Assigning that second bar to this
/// item is how a "menu" appears inside the Touch Bar: while the item's
/// collapsed representation (a button by default) sits in the hosted bar,
/// tapping it slides the popover bar down over the main bar; dismissing
/// (close button or `dismissPopover:`) restores the previous bar.
///
/// Selector note: the Objective-C property is declared
/// `@property (strong) NSTouchBar *popoverTouchBar` — the Java accessors are
/// named `popover()`/`setPopover(NSTouchBar)` but bind to the real selectors
/// `popoverTouchBar`/`setPopoverTouchBar:` (the property type is NSTouchBar*;
/// there is no `setPopover:` method on this class). All selectors are guarded
/// with `respondsToSelector:` like `NSCustomTouchBarItem`.
public class NSPopoverTouchBarItem extends NSTouchBarItem {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hInitId, MethodHandle hId, MethodHandle hVoidId) {}
    private static volatile Handles handles;

    protected NSPopoverTouchBarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSPopoverTouchBarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPopoverTouchBarItem(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
        );
    }

    /// alloc + initWithIdentifier: — popover item; its popover bar starts as
    /// an empty, non-customizable bar until you assign one via setPopover.
    public static NSPopoverTouchBarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPopoverTouchBarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitId().invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSPopoverTouchBarItem", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSPopoverTouchBarItem alloc/initWithIdentifier: returned nil");
        return new NSPopoverTouchBarItem(p);
    }

    /// True when the peer implements the given selector.
    private boolean responds(String selectorName) {
        try {
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel(selectorName));
        } catch (Throwable t) {
            return false;
        }
    }

    /// popoverTouchBar — the NSTouchBar displayed when this item is popped
    /// (defaults to an empty bar). Null when the selector is unavailable.
    public NSTouchBar popover() {
        ensureInit();
        if (!responds("popoverTouchBar")) return null;
        try {
            MemorySegment b = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("popoverTouchBar"));
            return NSTouchBar.wrap(b);
        } catch (Throwable t) {
            throw new RuntimeException("popoverTouchBar failed", t);
        }
    }

    /// setPopoverTouchBar: — assign the bar shown when the item is tapped.
    /// This assignment is what turns the item into an in-Touch-Bar "menu":
    /// build a second NSTouchBar (its items supplied by defaultItemIdentifiers
    /// plus a touchBar:makeItemForIdentifier: delegate), hand it here, and
    /// tapping the item slides that bar down over the main one. The property
    /// is declared nonnull, so null is ignored.
    public void setPopover(NSTouchBar bar) {
        if (bar == null) return;
        ensureInit();
        if (!responds("setPopoverTouchBar:")) return;
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setPopoverTouchBar:"), bar.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setPopoverTouchBar: failed", t);
        }
    }

    /// showPopover: — replace the main NSTouchBar with this item's popover
    /// bar. No effect while the item is not visible (guarded).
    public void showPopover() {
        ensureInit();
        if (!responds("showPopover:")) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("showPopover:"), MemorySegment.NULL);
    }

    /// dismissPopover: — order out the popover bar and restore the previously
    /// visible main bar (guarded).
    public void dismissPopover() {
        ensureInit();
        if (!responds("dismissPopover:")) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("dismissPopover:"), MemorySegment.NULL);
    }
}
