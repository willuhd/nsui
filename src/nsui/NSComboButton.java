package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSComboButton (macOS 13+) — a button with a menu indicator: the leading
/// segment performs the action, the trailing segment shows the menu.
/// Thin stateless wrapper; all behavior is AppKit's.
///
/// The 4- and 5-argument all-object factories use the documented AOT-safe
/// `ObjC.invoke` escape hatch (NULL-padded to the registered 6-object shape);
/// nothing in `NSComboButton.h` is omitted.
public final class NSComboButton extends NSControl {

    /// Button styles (NSComboButtonStyle).
    public static final long STYLE_SPLIT = 0;
    public static final long STYLE_UNIFIED = 1;

    private NSComboButton(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static NSComboButton wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSComboButton(peer);
    }

    /// [[NSComboButton alloc] initWithFrame:].
    public static NSComboButton create(NSRect frame) {
        return new NSComboButton(ObjC.newView("NSComboButton", frame));
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

    /// image — shown on the leading segment (nil-safe).
    public NSImage image() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("image"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }

    /// setImage: (nil-safe).
    public void setImage(NSImage image) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setImage:"),
                (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()));
    }

    /// imageScaling — NSImageScaling used to fit the image (0=ProportionallyDown).
    public long imageScaling() {
        return ObjC.msgSendLong(peer, ObjC.sel("imageScaling"));
    }

    /// setImageScaling:.
    public void setImageScaling(long scaling) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setImageScaling:"), scaling);
    }

    /// `+comboButtonWithTitle:menu:target:action:` (nil-safe menu/target/action).
    public static NSComboButton comboButtonWithTitle(String title, NSMenu menu, MemorySegment target, String actionSelector) {
        MemorySegment b = ObjC.invoke(ObjC.cls("NSComboButton"), ObjC.sel("comboButtonWithTitle:menu:target:action:"),
                ObjC.nsstring(title == null ? "" : title),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("comboButtonWithTitle:menu:target:action: returned nil");
        return new NSComboButton(b);
    }

    /// `+comboButtonWithImage:menu:target:action:` (nil-safe image/menu/target/action).
    public static NSComboButton comboButtonWithImage(NSImage image, NSMenu menu, MemorySegment target, String actionSelector) {
        MemorySegment b = ObjC.invoke(ObjC.cls("NSComboButton"), ObjC.sel("comboButtonWithImage:menu:target:action:"),
                (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("comboButtonWithImage:menu:target:action: returned nil");
        return new NSComboButton(b);
    }

    /// `+comboButtonWithTitle:image:menu:target:action:` (nil-safe throughout).
    public static NSComboButton comboButtonWithTitleImage(String title, NSImage image, NSMenu menu, MemorySegment target, String actionSelector) {
        MemorySegment b = ObjC.invoke(ObjC.cls("NSComboButton"), ObjC.sel("comboButtonWithTitle:image:menu:target:action:"),
                ObjC.nsstring(title == null ? "" : title),
                (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("comboButtonWithTitle:image:menu:target:action: returned nil");
        return new NSComboButton(b);
    }
}
