package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSToolbar — an AppKit toolbar. Thin, 1:1, stateless wrapper over the native
/// `NSToolbar`: each method maps to one `objc_msgSend` selector, no
/// cached Java state beyond the peer. Follows the project template: volatile
/// initialized, synchronized ensureInit, ObjC.handle(Sig.of...), invokeExact,
/// static create/wrap.
///
/// Created via `[[NSToolbar alloc] initWithIdentifier:]`; displayMode,
/// allowsUserCustomization, delegate, and item insertion mirror AppKit.
public final class NSToolbar extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitIdentifier; // (id, SEL, id) -> id   [initWithIdentifier:]
    private static MethodHandle hSetDisplayMode; // (id, SEL, long) -> void [setDisplayMode:]
    private static MethodHandle hSetAllowsCustom; // (id, SEL, bool) -> void [setAllowsUserCustomization:]
    private static MethodHandle hInsertItem;     // (id, SEL, id, long) -> void [insertItemWithItemIdentifier:atIndex:]
    private static MethodHandle hRemoveItem;     // (id, SEL, long) -> void [removeItemAtIndex:]
    private static MethodHandle hSetVisible;     // (id, SEL, bool) -> void [setVisible:]
    private static MethodHandle hSetShowsBaseline; // (id, SEL, bool) -> void [setShowsBaselineSeparator:]
    private static MethodHandle hSetDelegate;    // (id, SEL, id) -> void [setDelegate:]

    private NSToolbar(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing NSToolbar peer (e.g. from window.toolbar).
    public static NSToolbar wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSToolbar(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitIdentifier = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hSetDisplayMode = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetAllowsCustom = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hInsertItem = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT));
        hRemoveItem = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetVisible = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hSetShowsBaseline = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hSetDelegate = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /// `[[NSToolbar alloc] initWithIdentifier:identifier]` — a new toolbar.
    public static NSToolbar create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSToolbar"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitIdentifier.invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSToolbar", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSToolbar alloc/initWithIdentifier: returned nil");
        return new NSToolbar(p);
    }

    /// identifier — the toolbar's identifier string.
    public String identifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("identifier")));
    }

    // ---- displayMode ----
    /// [toolbar displayMode] — NSToolbarDisplayMode (0=default, 1=iconAndLabel, 2=iconOnly, 3=labelOnly).
    public long displayMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("displayMode"));
    }

    /// [toolbar setDisplayMode:] — NSToolbarDisplayMode.
    public void setDisplayMode(long mode) {
        try {
            hSetDisplayMode.invokeExact(peer, ObjC.sel("setDisplayMode:"), mode);
        } catch (Throwable t) {
            throw new RuntimeException("setDisplayMode: failed", t);
        }
    }

    // ---- allowsUserCustomization ----
    /// [toolbar allowsUserCustomization].
    public boolean allowsUserCustomization() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsUserCustomization"));
    }

    /// [toolbar setAllowsUserCustomization:]
    public void setAllowsUserCustomization(boolean flag) {
        try {
            hSetAllowsCustom.invokeExact(peer, ObjC.sel("setAllowsUserCustomization:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsUserCustomization: failed", t);
        }
    }

    // ---- visible ----
    /// [toolbar isVisible].
    public boolean isVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVisible"));
    }

    /// [toolbar setVisible:]
    public void setVisible(boolean flag) {
        try {
            hSetVisible.invokeExact(peer, ObjC.sel("setVisible:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setVisible: failed", t);
        }
    }

    // ---- showsBaselineSeparator ----
    /// [toolbar showsBaselineSeparator].
    public boolean showsBaselineSeparator() {
        return ObjC.msgSendBool(peer, ObjC.sel("showsBaselineSeparator"));
    }

    /// [toolbar setShowsBaselineSeparator:]
    public void setShowsBaselineSeparator(boolean flag) {
        try {
            hSetShowsBaseline.invokeExact(peer, ObjC.sel("setShowsBaselineSeparator:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setShowsBaselineSeparator: failed", t);
        }
    }

    // ---- delegate ----
    /// [toolbar delegate] — id (may be nil -> NULL).
    public MemorySegment delegate() {
        return ObjC.msgSendId(peer, ObjC.sel("delegate"));
    }

    /// [toolbar setDelegate:] — delegate object (DelegateProxy or any id).
    public void setDelegate(MemorySegment delegate) {
        try {
            hSetDelegate.invokeExact(peer, ObjC.sel("setDelegate:"), (MemorySegment) ((MemorySegment) (delegate == null ? MemorySegment.NULL : delegate)));
        } catch (Throwable t) {
            throw new RuntimeException("setDelegate: failed", t);
        }
    }

    // ---- items ----
    /// [toolbar insertItemWithItemIdentifier:atIndex:]
    public void insertItemWithItemIdentifier(String identifier, long index) {
        try {
            hInsertItem.invokeExact(peer, ObjC.sel("insertItemWithItemIdentifier:atIndex:"), ObjC.nsstring(identifier), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertItemWithItemIdentifier:atIndex: failed", t);
        }
    }

    /// Raw peer variant: insertItemWithItemIdentifier:atIndex: with id identifier.
    public void insertItemWithItemIdentifier(MemorySegment identifier, long index) {
        try {
            MemorySegment arg = (identifier == null || identifier.address() == 0) ? MemorySegment.NULL : identifier;
            hInsertItem.invokeExact(peer, ObjC.sel("insertItemWithItemIdentifier:atIndex:"), (MemorySegment) arg, index);
        } catch (Throwable t) {
            throw new RuntimeException("insertItemWithItemIdentifier:atIndex: failed", t);
        }
    }

    /// [toolbar removeItemAtIndex:]
    public void removeItemAtIndex(long index) {
        try {
            hRemoveItem.invokeExact(peer, ObjC.sel("removeItemAtIndex:"), index);
        } catch (Throwable t) {
            throw new RuntimeException("removeItemAtIndex: failed", t);
        }
    }

    /// [toolbar items] — NSArray of NSToolbarItem peers (id). May be nil.
    public MemorySegment items() {
        return ObjC.msgSendId(peer, ObjC.sel("items"));
    }

    /// [toolbar visibleItems] — NSArray of visible items.
    public MemorySegment visibleItems() {
        return ObjC.msgSendId(peer, ObjC.sel("visibleItems"));
    }

    /// [toolbar selectedItemIdentifier] — NSString id or nil.
    public MemorySegment selectedItemIdentifier() {
        return ObjC.msgSendId(peer, ObjC.sel("selectedItemIdentifier"));
    }

    /// [toolbar setSelectedItemIdentifier:] with String.
    public void setSelectedItemIdentifier(String identifier) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSelectedItemIdentifier:"), identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier));
    }

    /// [toolbar allowsExtension] etc via generic bool accessors.
    public boolean autosavesConfiguration() {
        return ObjC.msgSendBool(peer, ObjC.sel("autosavesConfiguration"));
    }

    public void setAutosavesConfiguration(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutosavesConfiguration:"), flag);
    }
}
