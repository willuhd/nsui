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
            private record Handles(MethodHandle hInitIdentifier, MethodHandle hSetDisplayMode, MethodHandle hSetAllowsCustom, MethodHandle hInsertItem, MethodHandle hSetDelegate) {}
    private static volatile Handles handles;

    private NSToolbar(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing NSToolbar peer (e.g. from window.toolbar).
    public static NSToolbar wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSToolbar(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
        );
    }

    /// `[[NSToolbar alloc] initWithIdentifier:identifier]` — a new toolbar.
    public static NSToolbar create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSToolbar"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitIdentifier().invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(identifier));
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

    // ---------------------------------------------------------------- nested enums — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSToolbar.h
    //   NSToolbarDisplayMode: Default 0, IconAndLabel 1, IconOnly 2, LabelOnly 3
    //   NSToolbarSizeMode: Default 0, Regular 1, Small 2 (deprecated)
    // Docs: https://developer.apple.com/documentation/appkit/nstoolbar/displaymode
    // Docs: https://developer.apple.com/documentation/appkit/nstoolbar/sizemode

    /// `NSToolbarDisplayMode` — 0=Default, 1=IconAndLabel, 2=IconOnly, 3=LabelOnly.
    public enum DisplayMode {
        defaultMode(0), iconAndLabel(1), iconOnly(2), labelOnly(3);
        public final long value;
        DisplayMode(long v) { this.value = v; }
        public static DisplayMode fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSToolbarSizeMode` — 0=Default, 1=Regular, 2=Small (deprecated, ignored).
    public enum SizeMode {
        defaultMode(0), regular(1), small(2);
        public final long value;
        SizeMode(long v) { this.value = v; }
        public static SizeMode fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    // ---- displayMode ----
    /// [toolbar displayMode] — NSToolbarDisplayMode (0=default, 1=iconAndLabel, 2=iconOnly, 3=labelOnly).
    public long displayMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("displayMode"));
    }
    /// Typed getter.
    public DisplayMode displayModeEnum() { return DisplayMode.fromValue(displayMode()); }

    /// [toolbar setDisplayMode:] — NSToolbarDisplayMode.
    public void setDisplayMode(long mode) {
        try {
            handles.hSetDisplayMode().invokeExact(peer, ObjC.sel("setDisplayMode:"), mode);
        } catch (Throwable t) {
            throw new RuntimeException("setDisplayMode: failed", t);
        }
    }
    /// Typed overload.
    public void setDisplayMode(DisplayMode m) { setDisplayMode(m.value); }

    // ---- sizeMode (deprecated but present) ----
    /// [toolbar sizeMode] — NSToolbarSizeMode.
    public long sizeMode() { return ObjC.msgSendLong(peer, ObjC.sel("sizeMode")); }
    /// Typed getter.
    public SizeMode sizeModeEnum() { return SizeMode.fromValue(sizeMode()); }
    /// [toolbar setSizeMode:] — NSToolbarSizeMode.
    public void setSizeMode(long mode) { ObjC.msgSendVoidLong(peer, ObjC.sel("setSizeMode:"), mode); }
    /// Typed overload.
    public void setSizeMode(SizeMode m) { setSizeMode(m.value); }

    // ---- allowsUserCustomization ----
    /// [toolbar allowsUserCustomization].
    public boolean allowsUserCustomization() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsUserCustomization"));
    }

    /// [toolbar setAllowsUserCustomization:]
    public void setAllowsUserCustomization(boolean flag) {
        try {
            handles.hSetAllowsCustom().invokeExact(peer, ObjC.sel("setAllowsUserCustomization:"), flag);
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
            handles.hSetAllowsCustom().invokeExact(peer, ObjC.sel("setVisible:"), flag);
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
            handles.hSetAllowsCustom().invokeExact(peer, ObjC.sel("setShowsBaselineSeparator:"), flag);
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
            handles.hSetDelegate().invokeExact(peer, ObjC.sel("setDelegate:"), (MemorySegment) ((MemorySegment) (delegate == null ? MemorySegment.NULL : delegate)));
        } catch (Throwable t) {
            throw new RuntimeException("setDelegate: failed", t);
        }
    }

    // ---- items ----
    /// [toolbar insertItemWithItemIdentifier:atIndex:]
    public void insertItemWithItemIdentifier(String identifier, long index) {
        try {
            handles.hInsertItem().invokeExact(peer, ObjC.sel("insertItemWithItemIdentifier:atIndex:"), ObjC.nsstring(identifier), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertItemWithItemIdentifier:atIndex: failed", t);
        }
    }

    /// Raw peer variant: insertItemWithItemIdentifier:atIndex: with id identifier.
    public void insertItemWithItemIdentifier(MemorySegment identifier, long index) {
        try {
            MemorySegment arg = (identifier == null || identifier.address() == 0) ? MemorySegment.NULL : identifier;
            handles.hInsertItem().invokeExact(peer, ObjC.sel("insertItemWithItemIdentifier:atIndex:"), (MemorySegment) arg, index);
        } catch (Throwable t) {
            throw new RuntimeException("insertItemWithItemIdentifier:atIndex: failed", t);
        }
    }

    /// [toolbar removeItemAtIndex:]
    public void removeItemAtIndex(long index) {
        try {
            handles.hSetDisplayMode().invokeExact(peer, ObjC.sel("removeItemAtIndex:"), index);
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

    // ---- init (macOS 10.13, empty-string identifier) ----
    /// `[[NSToolbar alloc] init]` — toolbar with empty identifier (non-customizable use).
    public static NSToolbar createDefault() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSToolbar"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p == null || p.address() == 0) throw new IllegalStateException("NSToolbar alloc/init returned nil");
        return new NSToolbar(p);
    }

    // ---- removeItemWithItemIdentifier: (macOS 15) ----
    /// removeItemWithItemIdentifier:.
    public void removeItemWithItemIdentifier(String identifier) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeItemWithItemIdentifier:"),
                identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier));
    }
    /// removeItemWithItemIdentifier: with raw id.
    public void removeItemWithItemIdentifier(MemorySegment identifier) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeItemWithItemIdentifier:"),
                (MemorySegment) (identifier == null ? MemorySegment.NULL : identifier));
    }

    // ---- runCustomizationPalette: / customizationPaletteIsRunning ----
    /// runCustomizationPalette: — never call from tests (opens modal palette).
    public void runCustomizationPalette(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("runCustomizationPalette:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    /// runCustomizationPalette: with NSObject sender.
    public void runCustomizationPalette(NSObject sender) {
        runCustomizationPalette(sender == null ? MemorySegment.NULL : sender.peer());
    }
    /// customizationPaletteIsRunning.
    public boolean customizationPaletteIsRunning() {
        return ObjC.msgSendBool(peer, ObjC.sel("customizationPaletteIsRunning"));
    }

    // ---- allowsDisplayModeCustomization (macOS 15) ----
    /// allowsDisplayModeCustomization.
    public boolean allowsDisplayModeCustomization() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsDisplayModeCustomization"));
    }
    /// setAllowsDisplayModeCustomization:.
    public void setAllowsDisplayModeCustomization(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsDisplayModeCustomization:"), flag);
    }

    // ---- itemIdentifiers (NSArray<NSToolbarItemIdentifier>, macOS 15) ----
    /// itemIdentifiersPeer — raw NSArray id.
    public MemorySegment itemIdentifiersPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("itemIdentifiers"));
    }
    /// itemIdentifiers — identifier strings (empty when none).
    public java.util.List<String> itemIdentifiers() {
        MemorySegment arr = itemIdentifiersPeer();
        if (arr == null || arr.address() == 0) return java.util.List.of();
        long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
        java.util.List<String> out = new java.util.ArrayList<>((int) count);
        try {
            java.lang.invoke.MethodHandle hAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            for (long i = 0; i < count; i++) {
                MemorySegment s = (MemorySegment) hAt.invokeExact(arr, ObjC.sel("objectAtIndex:"), i);
                String str = ObjC.toString(s);
                if (str != null) out.add(str);
            }
        } catch (Throwable t) { throw new RuntimeException("itemIdentifiers failed", t); }
        return java.util.Collections.unmodifiableList(out);
    }
    /// setItemIdentifiers: with strings.
    public void setItemIdentifiers(java.util.List<String> identifiers) {
        MemorySegment arr = (identifiers == null) ? MemorySegment.NULL : ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
        if (identifiers != null) for (String s : identifiers) if (s != null) ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ObjC.nsstring(s));
        ObjC.msgSendVoidId(peer, ObjC.sel("setItemIdentifiers:"), arr);
    }

    // ---- centeredItemIdentifiers (NSSet, macOS 13) ----
    /// centeredItemIdentifiersPeer — raw NSSet id.
    public MemorySegment centeredItemIdentifiersPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("centeredItemIdentifiers"));
    }
    /// setCenteredItemIdentifiers: with NSSet id.
    public void setCenteredItemIdentifiers(MemorySegment identifiers) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setCenteredItemIdentifiers:"), (MemorySegment) (identifiers == null ? MemorySegment.NULL : identifiers));
    }

    // ---- validateVisibleItems ----
    /// validateVisibleItems.
    public void validateVisibleItems() {
        ObjC.msgSendVoid(peer, ObjC.sel("validateVisibleItems"));
    }

    // ---- allowsExtensionItems (macOS 10.10) ----
    /// allowsExtensionItems.
    public boolean allowsExtensionItems() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsExtensionItems"));
    }
    /// setAllowsExtensionItems:.
    public void setAllowsExtensionItems(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsExtensionItems:"), flag);
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSToolbar.h
    // - NSToolbarDelegate protocol methods (toolbar:itemForItemIdentifier:..., toolbarDefault/Allowed/Selectable/
    //   ImmovableItemIdentifiers:, canBeInsertedAtIndex:) — delegate side, covered by NSToolbarDelegate helper.
    // - -toolbarWillAddItem:/-toolbarDidRemoveItem: — notification observers, not toolbar selectors.
    // - Deprecated sizeMode is KEPT (still the working API; deprecation says ignored in future but present);
    //   deprecated centeredItemIdentifier/fullScreenAccessoryView(*)/showsBaselineSeparator/configurationDictionary
    //   are omitted except showsBaselineSeparator which is KEPT for compat (existing call sites) — other
    //   deprecated fullScreen/configuration APIs omitted (no registered shape benefit, superseded by
    //   itemIdentifiers/displayMode).
}
