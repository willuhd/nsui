package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import java.util.concurrent.ConcurrentHashMap;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSToolbarItem — an item within an NSToolbar. Thin, 1:1, stateless wrapper over
/// the native `NSToolbarItem`: each method maps to one `objc_msgSend`
/// selector. Follows the project template: volatile initialized, synchronized
/// ensureInit, ObjC.handle(Sig.of...), invokeExact, static create/wrap.
///
/// Created via `[[NSToolbarItem alloc] initWithItemIdentifier:]`.
public final class NSToolbarItem extends NSObject implements NSUserInterfaceItemIdentification {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hInitIdentifier, MethodHandle hSetLabel, MethodHandle hSetEnabled, MethodHandle hSetTag, MethodHandle hSetMinSize) {}
    private static volatile Handles handles;

    private NSToolbarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing NSToolbarItem peer.
    public static NSToolbarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSToolbarItem(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE))
        );
    }

    /// `[[NSToolbarItem alloc] initWithItemIdentifier:identifier]` — a new item.
    public static NSToolbarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSToolbarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitIdentifier().invokeExact(p, ObjC.sel("initWithItemIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithItemIdentifier: failed for NSToolbarItem", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSToolbarItem alloc/initWithItemIdentifier: returned nil");
        return new NSToolbarItem(p);
    }

    /// Raw peer variant: initWithItemIdentifier: with id.
    public static NSToolbarItem create(MemorySegment identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSToolbarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitIdentifier().invokeExact(p, ObjC.sel("initWithItemIdentifier:"), (MemorySegment) (identifier == null ? MemorySegment.NULL : identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithItemIdentifier: failed for NSToolbarItem", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSToolbarItem alloc/initWithItemIdentifier: returned nil");
        return new NSToolbarItem(p);
    }

    // ---------------------------------------------------------------- instance API

    /// [item itemIdentifier] — NSString id.
    public String itemIdentifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("itemIdentifier")));
    }

    /// [item label] — NSString.
    public String label() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("label")));
    }

    /// [item setLabel:]
    public void setLabel(String label) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setLabel:"), (MemorySegment) (label == null ? MemorySegment.NULL : ObjC.nsstring(label)));
        } catch (Throwable t) {
            throw new RuntimeException("setLabel: failed", t);
        }
    }

    /// [item paletteLabel]
    public String paletteLabel() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("paletteLabel")));
    }

    /// [item setPaletteLabel:]
    public void setPaletteLabel(String label) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setPaletteLabel:"), (MemorySegment) (label == null ? MemorySegment.NULL : ObjC.nsstring(label)));
        } catch (Throwable t) {
            throw new RuntimeException("setPaletteLabel: failed", t);
        }
    }

    /// [item toolTip]
    public String toolTip() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("toolTip")));
    }

    /// [item setToolTip:]
    public void setToolTip(String tip) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setToolTip:"), (MemorySegment) (tip == null ? MemorySegment.NULL : ObjC.nsstring(tip)));
        } catch (Throwable t) {
            throw new RuntimeException("setToolTip: failed", t);
        }
    }

    /// [item image] — NSImage peer or nil.
    public NSImage image() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("image"));
        return NSImage.wrap(p);
    }

    /// [item setImage:]
    public void setImage(NSImage image) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setImage:"), (MemorySegment) ((MemorySegment) (image == null ? MemorySegment.NULL : image.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("setImage: failed", t);
        }
    }

    /// [item view] — NSView peer or nil.
    public NSView view() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("view"));
        return NSView.wrap(v);
    }

    /// [item setView:]
    public void setView(NSView view) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setView:"), (MemorySegment) ((MemorySegment) (view == null ? MemorySegment.NULL : view.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("setView: failed", t);
        }
    }

    /// [item target] — action target.
    public MemorySegment target() {
        return ObjC.msgSendId(peer, ObjC.sel("target"));
    }

    /// [item setTarget:] — action target id.
    public void setTarget(MemorySegment target) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setTarget:"), (MemorySegment) ((MemorySegment) (target == null ? MemorySegment.NULL : target)));
        } catch (Throwable t) {
            throw new RuntimeException("setTarget: failed", t);
        }
    }

    /// [item action] — selector.
    public MemorySegment action() {
        return ObjC.msgSendId(peer, ObjC.sel("action"));
    }

    /// [item setAction:] — selector (SEL).
    public void setAction(String actionSelector) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setAction:"), (MemorySegment) (actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector)));
        } catch (Throwable t) {
            throw new RuntimeException("setAction: failed", t);
        }
    }

    /// [item setAction:] with raw SEL.
    public void setAction(MemorySegment action) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setAction:"), (MemorySegment) (action == null ? MemorySegment.NULL : action));
        } catch (Throwable t) {
            throw new RuntimeException("setAction: failed", t);
        }
    }

    /// [item isEnabled].
    public boolean isEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEnabled"));
    }

    /// [item setEnabled:]
    public void setEnabled(boolean flag) {
        try {
            handles.hSetEnabled().invokeExact(peer, ObjC.sel("setEnabled:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setEnabled: failed", t);
        }
    }

    /// [item tag].
    public long tag() {
        return ObjC.msgSendLong(peer, ObjC.sel("tag"));
    }

    /// [item setTag:]
    public void setTag(long tag) {
        try {
            handles.hSetTag().invokeExact(peer, ObjC.sel("setTag:"), tag);
        } catch (Throwable t) {
            throw new RuntimeException("setTag: failed", t);
        }
    }

    /// [item minSize] — NSSize.
    public NSSize minSize() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.SIZE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("minSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("minSize failed", t);
        }
    }

    /// [item setMinSize:]
    public void setMinSize(NSSize size) {
        try {
            handles.hSetMinSize().invokeExact(peer, ObjC.sel("setMinSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMinSize: failed", t);
        }
    }

    /// [item maxSize]
    public NSSize maxSize() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.SIZE));
            MemorySegment s = (MemorySegment) h.invokeExact(ObjC.structSlot(), peer, ObjC.sel("maxSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("maxSize failed", t);
        }
    }

    /// [item setMaxSize:]
    public void setMaxSize(NSSize size) {
        try {
            handles.hSetMinSize().invokeExact(peer, ObjC.sel("setMaxSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMaxSize: failed", t);
        }
    }

    /// [item visibilityPriority] — NSToolbarItemVisibilityPriority (NSInteger).
    public long visibilityPriority() {
        return ObjC.msgSendLong(peer, ObjC.sel("visibilityPriority"));
    }

    public void setVisibilityPriority(long p) {
        try {
            handles.hSetTag().invokeExact(peer, ObjC.sel("setVisibilityPriority:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("setVisibilityPriority: failed", t);
        }
    }

    // ---- toolbar (readonly weak NSToolbar*) ----
    /// toolbar — the toolbar displaying this item (nil when not in a toolbar).
    public NSToolbar toolbar() {
        return NSToolbar.wrap(ObjC.msgSendId(peer, ObjC.sel("toolbar")));
    }
    /// toolbarPeer — raw id.
    public MemorySegment toolbarPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("toolbar"));
    }

    // ---- possibleLabels (NSSet<NSString*>, macOS 13) ----
    /// possibleLabelsPeer — raw NSSet id.
    public MemorySegment possibleLabelsPeer() {
        return ObjC.msgSendId(peer, ObjC.sel("possibleLabels"));
    }
    /// setPossibleLabels: with NSSet id.
    public void setPossibleLabels(MemorySegment labels) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setPossibleLabels:"), (MemorySegment) (labels == null ? MemorySegment.NULL : labels));
        } catch (Throwable t) { throw new RuntimeException("setPossibleLabels: failed", t); }
    }

    // ---- menuFormRepresentation (NSMenuItem*) ----
    /// menuFormRepresentation.
    public NSMenuItem menuFormRepresentation() {
        return NSMenuItem.wrap(ObjC.msgSendId(peer, ObjC.sel("menuFormRepresentation")));
    }
    /// setMenuFormRepresentation:.
    public void setMenuFormRepresentation(NSMenuItem item) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setMenuFormRepresentation:"), (MemorySegment) (item == null ? MemorySegment.NULL : item.peer()));
        } catch (Throwable t) { throw new RuntimeException("setMenuFormRepresentation: failed", t); }
    }
    /// setMenuFormRepresentation: with raw id.
    public void setMenuFormRepresentation(MemorySegment item) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setMenuFormRepresentation:"), (MemorySegment) (item == null ? MemorySegment.NULL : item));
        } catch (Throwable t) { throw new RuntimeException("setMenuFormRepresentation: failed", t); }
    }

    // ---- title (NSString, macOS 10.15) ----
    /// title.
    public String title() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("title")));
    }
    /// setTitle:.
    public void setTitle(String title) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setTitle:"), (MemorySegment) (title == null ? MemorySegment.NULL : ObjC.nsstring(title)));
        } catch (Throwable t) { throw new RuntimeException("setTitle: failed", t); }
    }

    // ---- bordered (macOS 10.15, getter isBordered) ----
    /// isBordered.
    public boolean isBordered() {
        return ObjC.msgSendBool(peer, ObjC.sel("isBordered"));
    }
    /// setBordered:.
    public void setBordered(boolean flag) {
        try { handles.hSetEnabled().invokeExact(peer, ObjC.sel("setBordered:"), flag); }
        catch (Throwable t) { throw new RuntimeException("setBordered: failed", t); }
    }

    // ---- backgroundTintColor (NSColor, macOS 26) ----
    /// backgroundTintColor.
    public NSColor backgroundTintColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("backgroundTintColor")));
    }
    /// setBackgroundTintColor:.
    public void setBackgroundTintColor(NSColor color) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setBackgroundTintColor:"), (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
        } catch (Throwable t) { throw new RuntimeException("setBackgroundTintColor: failed", t); }
    }

    // ---------------------------------------------------------------- nested types — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSToolbarItem.h
    //   NSToolbarItemStyle: Plain 0, Prominent 1 (macOS 26)
    //   NSToolbarItemVisibilityPriority: Standard 0, Low -1000, High 1000, User 2000
    /// `NSToolbarItemStyle` — 0=Plain, 1=Prominent (macOS 26).
    public enum ItemStyle {
        plain(0), prominent(1);
        public final long value;
        ItemStyle(long v) { this.value = v; }
        public static ItemStyle fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }
    /// Visibility-priority constants (NSToolbarItemVisibilityPriority).
    public static final long VISIBILITY_STANDARD = 0L;
    public static final long VISIBILITY_LOW = -1000L;
    public static final long VISIBILITY_HIGH = 1000L;
    public static final long VISIBILITY_USER = 2000L;

    // ---- style (NSToolbarItemStyle long, macOS 26) ----
    /// style.
    public long style() {
        return ObjC.msgSendLong(peer, ObjC.sel("style"));
    }
    /// setStyle:.
    public void setStyle(long style) {
        try { handles.hSetTag().invokeExact(peer, ObjC.sel("setStyle:"), style); }
        catch (Throwable t) { throw new RuntimeException("setStyle: failed", t); }
    }
    /// Typed overload.
    public void setStyle(ItemStyle s) { setStyle(s.value); }
    /// Typed getter.
    public ItemStyle styleEnum() { return ItemStyle.fromValue(style()); }

    // ---- navigational (macOS 11, getter isNavigational) ----
    /// isNavigational.
    public boolean isNavigational() {
        return ObjC.msgSendBool(peer, ObjC.sel("isNavigational"));
    }
    /// setNavigational:.
    public void setNavigational(boolean flag) {
        try { handles.hSetEnabled().invokeExact(peer, ObjC.sel("setNavigational:"), flag); }
        catch (Throwable t) { throw new RuntimeException("setNavigational: failed", t); }
    }

    // ---- visible (readonly, macOS 12, getter isVisible) ----
    /// isVisible.
    public boolean isVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVisible"));
    }

    // ---- hidden (macOS 15, getter isHidden) ----
    /// isHidden.
    public boolean isHidden() {
        return ObjC.msgSendBool(peer, ObjC.sel("isHidden"));
    }
    /// setHidden:.
    public void setHidden(boolean flag) {
        try { handles.hSetEnabled().invokeExact(peer, ObjC.sel("setHidden:"), flag); }
        catch (Throwable t) { throw new RuntimeException("setHidden: failed", t); }
    }

    // ---- badge (NSItemBadge*, macOS 26; no wrapper — id peer) ----
    /// badgePeer — raw NSItemBadge id (may be NULL).
    public MemorySegment badgePeer() {
        return ObjC.msgSendId(peer, ObjC.sel("badge"));
    }
    /// setBadge: with raw id.
    public void setBadge(MemorySegment badge) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setBadge:"), (MemorySegment) (badge == null ? MemorySegment.NULL : badge));
        } catch (Throwable t) { throw new RuntimeException("setBadge: failed", t); }
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - -validate / autovalidates (both API_AVAILABLE(ios 13.0) only) — omitted: iOS-only surface.
    // - allowsDuplicatesInToolbar (deprecated 10.0-15.0) — omitted: deprecated, always NO on modern AppKit.
    // - -validateToolbarItem: (both current and deprecated) / -cloudShareForUserInterfaceItem: — validation/cloud
    //   protocol methods, not NSToolbarItem selectors; wire via NSUserInterfaceValidation if needed.
    // - minSize/maxSize are KEPT (deprecated 10.0-12.0 but still the working NSSize API; the replacement is
    //   auto-layout constraints, not a new selector).
    // - UIImage image overload (iOS) — omitted: iOS-only; NSImage overload kept.
    // - UIColor backgroundTintColor overload (iOS) — omitted: iOS-only; NSColor overload kept.

    // ---- NSUserInterfaceItemIdentification (identifier / setIdentifier:) ----
    // Fallback map for runtimes where NSToolbarItem doesn't implement identifier (itemIdentifier is readonly)
    private static final ConcurrentHashMap<Long, String> identifierFallback = new ConcurrentHashMap<>();

    /// [item identifier] — NSUserInterfaceItemIdentifier (NSString).
    /// Tries native `identifier` first (guarded by respondsToSelector:); falls back to per-peer map or `itemIdentifier`.
    @Override
    public String identifier() {
        String fallback = identifierFallback.get(peer.address());
        if (fallback != null) return fallback;
        try {
            boolean responds = (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("identifier"));
            if (responds) {
                MemorySegment seg = ObjC.msgSendId(peer, ObjC.sel("identifier"));
                String nativeVal = ObjC.toString(seg);
                if (nativeVal != null) return nativeVal;
            }
        } catch (Throwable ignored) {
            // fall through to itemIdentifier
        }
        try {
            String itemId = itemIdentifier();
            if (itemId != null) return itemId;
        } catch (Throwable ignored) {}
        return fallback;
    }

    /// [item setIdentifier:] — NSUserInterfaceItemIdentifier.
    /// Stores in fallback map and best-effort native setIdentifier: (guarded by respondsToSelector:).
    @Override
    public void setIdentifier(String id) {
        if (id == null) identifierFallback.remove(peer.address());
        else identifierFallback.put(peer.address(), id);
        try {
            boolean responds = (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("setIdentifier:"));
            if (responds) {
                ObjC.msgSendVoidId(peer, ObjC.sel("setIdentifier:"), id == null ? java.lang.foreign.MemorySegment.NULL : ObjC.nsstring(id));
            }
        } catch (Throwable ignored) {
            // fallback map retains value
        }
    }
}
