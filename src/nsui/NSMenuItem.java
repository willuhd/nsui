package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMenuItem — a menu entry with title, action selector and key equivalent.
public final class NSMenuItem extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hIdInt; // (id,SEL,long)->id

    private NSMenuItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hIdInt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
        initialized = true;
    }

    public static NSMenuItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMenuItem(peer);
    }

    /// alloc + init (a plain, empty item).
    public static NSMenuItem create() {
        MemorySegment item = ObjC.msgSendId(ObjC.cls("NSMenuItem"), ObjC.sel("alloc"));
        return new NSMenuItem(ObjC.msgSendId(item, ObjC.sel("init")));
    }

    /// alloc + initWithTitle:action:keyEquivalent:.
    public static NSMenuItem withTitle(String title, String action, String keyEquivalent) {
        MemorySegment item = ObjC.msgSendId(ObjC.cls("NSMenuItem"), ObjC.sel("alloc"));
        return new NSMenuItem(ObjC.msgSendIdIdSelId(item,
                ObjC.sel("initWithTitle:action:keyEquivalent:"),
                ObjC.nsstring(title), action == null || action.isEmpty() ? MemorySegment.NULL : ObjC.sel(action), ObjC.nsstring(keyEquivalent)));
    }

    public static NSMenuItem separatorItem() {
        return new NSMenuItem(ObjC.msgSendId(ObjC.cls("NSMenuItem"), ObjC.sel("separatorItem")));
    }

    // ---- target / action ----
    public MemorySegment target() { return ObjC.msgSendId(peer, ObjC.sel("target")); }
    public void setTarget(MemorySegment t) { ObjC.msgSendVoidId(peer, ObjC.sel("setTarget:"), (MemorySegment) (t == null ? MemorySegment.NULL : t)); }
    public MemorySegment action() { return ObjC.msgSendId(peer, ObjC.sel("action")); }
    public void setAction(String sel) { ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"), sel == null ? MemorySegment.NULL : ObjC.sel(sel)); }
    public void setAction(MemorySegment sel) { ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"), (MemorySegment) (sel == null ? MemorySegment.NULL : sel)); }

    // ---- title ----
    public String title() { return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("title"))); }
    public void setTitle(String t) { ObjC.msgSendVoidId(peer, ObjC.sel("setTitle:"), ObjC.nsstring(t)); }
    public MemorySegment attributedTitle() { return ObjC.msgSendId(peer, ObjC.sel("attributedTitle")); }
    public void setAttributedTitle(MemorySegment a) { ObjC.msgSendVoidId(peer, ObjC.sel("setAttributedTitle:"), (MemorySegment) (a == null ? MemorySegment.NULL : a)); }
    public String subtitle() { return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("subtitle"))); }
    public void setSubtitle(String s) { ObjC.msgSendVoidId(peer, ObjC.sel("setSubtitle:"), s == null ? MemorySegment.NULL : ObjC.nsstring(s)); }

    // ---- state / enabled ----
    public long state() { return ObjC.msgSendLong(peer, ObjC.sel("state")); }
    public void setState(long s) { ObjC.msgSendVoidLong(peer, ObjC.sel("setState:"), s); }
    public boolean isEnabled() { return ObjC.msgSendBool(peer, ObjC.sel("isEnabled")); }
    public void setEnabled(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setEnabled:"), flag); }
    public boolean isHidden() { return ObjC.msgSendBool(peer, ObjC.sel("isHidden")); }
    public void setHidden(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setHidden:"), flag); }
    public boolean isSeparatorItem() { return ObjC.msgSendBool(peer, ObjC.sel("isSeparatorItem")); }
    public boolean isAlternate() { return ObjC.msgSendBool(peer, ObjC.sel("isAlternate")); }
    public void setAlternate(boolean flag) { ObjC.msgSendVoidBool(peer, ObjC.sel("setAlternate:"), flag); }

    // ---- tag ----
    public long tag() { return ObjC.msgSendLong(peer, ObjC.sel("tag")); }
    public void setTag(long t) { ObjC.msgSendVoidLong(peer, ObjC.sel("setTag:"), t); }

    // ---- keyEquivalent ----
    public String keyEquivalent() { return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("keyEquivalent"))); }
    public void setKeyEquivalent(String k) { ObjC.msgSendVoidId(peer, ObjC.sel("setKeyEquivalent:"), k == null ? MemorySegment.NULL : ObjC.nsstring(k)); }
    public long keyEquivalentModifierMask() { return ObjC.msgSendLong(peer, ObjC.sel("keyEquivalentModifierMask")); }
    public void setKeyEquivalentModifierMask(long m) { ObjC.msgSendVoidLong(peer, ObjC.sel("setKeyEquivalentModifierMask:"), m); }

    // ---- image ---- (menu-list icons, not menubar bar)
    /// [item image] — menu-list icon. NSMenuItem.setImage renders in the dropdown menu list
    /// (File → New, Edit → Cut, etc.), not the menubar bar itself. Top-level App/File/Edit/View
    /// bar items should remain text; attach icons only to dropdown list items. For NSStatusItem bar,
    /// use NSStatusBar/NSStatusItem button image (status agent), not this.
    public NSImage image() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("image"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    /// [item setImage:] — attach a menu-list icon to a dropdown item. Works for menu lists
    /// (File/Edit/View/App menus) via NSMenu; do not use to iconify the menubar bar top-level items.
    /// Safe to pass null to clear. Prefer `attachMenuItemIcon` helper for named images.
    public void setImage(NSImage img) { ObjC.msgSendVoidId(peer, ObjC.sel("setImage:"), (MemorySegment) (img == null ? MemorySegment.NULL : img.peer())); }
    public NSImage onStateImage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("onStateImage"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setOnStateImage(NSImage img) { ObjC.msgSendVoidId(peer, ObjC.sel("setOnStateImage:"), (MemorySegment) (img == null ? MemorySegment.NULL : img.peer())); }
    public NSImage offStateImage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("offStateImage"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setOffStateImage(NSImage img) { ObjC.msgSendVoidId(peer, ObjC.sel("setOffStateImage:"), (MemorySegment) (img == null ? MemorySegment.NULL : img.peer())); }
    public NSImage mixedStateImage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("mixedStateImage"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setMixedStateImage(NSImage img) { ObjC.msgSendVoidId(peer, ObjC.sel("setMixedStateImage:"), (MemorySegment) (img == null ? MemorySegment.NULL : img.peer())); }

    // ---- menu / submenu ----
    public NSMenu menu() { return NSMenu.wrap(ObjC.msgSendId(peer, ObjC.sel("menu"))); }
    public boolean hasSubmenu() { return ObjC.msgSendBool(peer, ObjC.sel("hasSubmenu")); }
    public NSMenu submenu() { return NSMenu.wrap(ObjC.msgSendId(peer, ObjC.sel("submenu"))); }
    public void setSubmenu(NSMenu m) { ObjC.msgSendVoidId(peer, ObjC.sel("setSubmenu:"), (MemorySegment) (m == null ? MemorySegment.NULL : m.peer())); }
    public MemorySegment parentItem() { return ObjC.msgSendId(peer, ObjC.sel("parentItem")); }

    // ---- view ----
    public MemorySegment view() { return ObjC.msgSendId(peer, ObjC.sel("view")); }
    public void setView(MemorySegment v) { ObjC.msgSendVoidId(peer, ObjC.sel("setView:"), (MemorySegment) (v == null ? MemorySegment.NULL : v)); }
    /// Typed overload: embed any NSView (including NSSearchField) as the menu item's custom view.
    public void setView(NSView view) { setView(view == null ? MemorySegment.NULL : view.peer()); }
    /// Convenience for NSSearchField embedding: typed helper.
    public void setSearchFieldView(NSSearchField field) { setView((NSView) field); }
    /// If the item's view is an NSSearchField, wrap it; otherwise null (also null if view is nil).
    public NSSearchField viewAsSearchField() {
        MemorySegment v = view();
        if (v == null || v.address() == 0) return null;
        // verify isKindOfClass NSSearchField to avoid wrapping wrong type; still wrap optimistically if check fails due to missing class
        try {
            boolean isSF = (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(v, ObjC.sel("isKindOfClass:"), ObjC.cls("NSSearchField"));
            if (!isSF) return null;
        } catch (Throwable ignored) {}
        return NSSearchField.wrap(v);
    }
    /// Typed view accessor as NSView (null if nil).
    public NSView viewAsView() {
        MemorySegment v = view();
        return (v == null || v.address() == 0) ? null : NSView.wrap(v);
    }
    /// Whether this item currently hosts a custom view (e.g., NSSearchField).
    public boolean hasCustomView() {
        MemorySegment v = view();
        return v != null && v.address() != 0;
    }

    // ---- indentation / toolTip / representedObject ----
    public long indentationLevel() { return ObjC.msgSendLong(peer, ObjC.sel("indentationLevel")); }
    public void setIndentationLevel(long l) { ObjC.msgSendVoidLong(peer, ObjC.sel("setIndentationLevel:"), l); }
    public String toolTip() { return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("toolTip"))); }
    public void setToolTip(String t) { ObjC.msgSendVoidId(peer, ObjC.sel("setToolTip:"), t == null ? MemorySegment.NULL : ObjC.nsstring(t)); }
    public MemorySegment representedObject() { return ObjC.msgSendId(peer, ObjC.sel("representedObject")); }
    public void setRepresentedObject(MemorySegment o) { ObjC.msgSendVoidId(peer, ObjC.sel("setRepresentedObject:"), (MemorySegment) (o == null ? MemorySegment.NULL : o)); }
    public MemorySegment badge() { return ObjC.msgSendId(peer, ObjC.sel("badge")); }
    public void setBadge(MemorySegment b) { ObjC.msgSendVoidId(peer, ObjC.sel("setBadge:"), (MemorySegment) (b == null ? MemorySegment.NULL : b)); }

    // ---- allowsKeyEquivalentWhenHidden etc ----
    public boolean allowsKeyEquivalentWhenHidden() { return ObjC.msgSendBool(peer, ObjC.sel("allowsKeyEquivalentWhenHidden")); }
    public void setAllowsKeyEquivalentWhenHidden(boolean f) { ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsKeyEquivalentWhenHidden:"), f); }
}
