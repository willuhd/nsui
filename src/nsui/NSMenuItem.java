package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/** NSMenuItem — a menu entry with title, action selector and key equivalent. */
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

    /** alloc + init (a plain, empty item). */
    public static NSMenuItem create() {
        MemorySegment item = ObjC.msgSendId(ObjC.cls("NSMenuItem"), ObjC.sel("alloc"));
        return new NSMenuItem(ObjC.msgSendId(item, ObjC.sel("init")));
    }

    /** alloc + initWithTitle:action:keyEquivalent:. */
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
    public void setTarget(MemorySegment t) { ObjC.msgSendVoidId(peer, ObjC.sel("setTarget:"), t == null ? MemorySegment.NULL : t); }
    public MemorySegment action() { return ObjC.msgSendId(peer, ObjC.sel("action")); }
    public void setAction(String sel) { ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"), sel == null ? MemorySegment.NULL : ObjC.sel(sel)); }
    public void setAction(MemorySegment sel) { ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"), sel == null ? MemorySegment.NULL : sel); }

    // ---- title ----
    public String title() { return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("title"))); }
    public void setTitle(String t) { ObjC.msgSendVoidId(peer, ObjC.sel("setTitle:"), ObjC.nsstring(t)); }
    public MemorySegment attributedTitle() { return ObjC.msgSendId(peer, ObjC.sel("attributedTitle")); }
    public void setAttributedTitle(MemorySegment a) { ObjC.msgSendVoidId(peer, ObjC.sel("setAttributedTitle:"), a == null ? MemorySegment.NULL : a); }
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

    // ---- image ----
    public NSImage image() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("image"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setImage(NSImage img) { ObjC.msgSendVoidId(peer, ObjC.sel("setImage:"), img == null ? MemorySegment.NULL : img.peer()); }
    public NSImage onStateImage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("onStateImage"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setOnStateImage(NSImage img) { ObjC.msgSendVoidId(peer, ObjC.sel("setOnStateImage:"), img == null ? MemorySegment.NULL : img.peer()); }
    public NSImage offStateImage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("offStateImage"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setOffStateImage(NSImage img) { ObjC.msgSendVoidId(peer, ObjC.sel("setOffStateImage:"), img == null ? MemorySegment.NULL : img.peer()); }
    public NSImage mixedStateImage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("mixedStateImage"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setMixedStateImage(NSImage img) { ObjC.msgSendVoidId(peer, ObjC.sel("setMixedStateImage:"), img == null ? MemorySegment.NULL : img.peer()); }

    // ---- menu / submenu ----
    public NSMenu menu() { return NSMenu.wrap(ObjC.msgSendId(peer, ObjC.sel("menu"))); }
    public boolean hasSubmenu() { return ObjC.msgSendBool(peer, ObjC.sel("hasSubmenu")); }
    public NSMenu submenu() { return NSMenu.wrap(ObjC.msgSendId(peer, ObjC.sel("submenu"))); }
    public void setSubmenu(NSMenu m) { ObjC.msgSendVoidId(peer, ObjC.sel("setSubmenu:"), m == null ? MemorySegment.NULL : m.peer()); }
    public MemorySegment parentItem() { return ObjC.msgSendId(peer, ObjC.sel("parentItem")); }

    // ---- view ----
    public MemorySegment view() { return ObjC.msgSendId(peer, ObjC.sel("view")); }
    public void setView(MemorySegment v) { ObjC.msgSendVoidId(peer, ObjC.sel("setView:"), v == null ? MemorySegment.NULL : v); }

    // ---- indentation / toolTip / representedObject ----
    public long indentationLevel() { return ObjC.msgSendLong(peer, ObjC.sel("indentationLevel")); }
    public void setIndentationLevel(long l) { ObjC.msgSendVoidLong(peer, ObjC.sel("setIndentationLevel:"), l); }
    public String toolTip() { return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("toolTip"))); }
    public void setToolTip(String t) { ObjC.msgSendVoidId(peer, ObjC.sel("setToolTip:"), t == null ? MemorySegment.NULL : ObjC.nsstring(t)); }
    public MemorySegment representedObject() { return ObjC.msgSendId(peer, ObjC.sel("representedObject")); }
    public void setRepresentedObject(MemorySegment o) { ObjC.msgSendVoidId(peer, ObjC.sel("setRepresentedObject:"), o == null ? MemorySegment.NULL : o); }
    public MemorySegment badge() { return ObjC.msgSendId(peer, ObjC.sel("badge")); }
    public void setBadge(MemorySegment b) { ObjC.msgSendVoidId(peer, ObjC.sel("setBadge:"), b == null ? MemorySegment.NULL : b); }

    // ---- allowsKeyEquivalentWhenHidden etc ----
    public boolean allowsKeyEquivalentWhenHidden() { return ObjC.msgSendBool(peer, ObjC.sel("allowsKeyEquivalentWhenHidden")); }
    public void setAllowsKeyEquivalentWhenHidden(boolean f) { ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsKeyEquivalentWhenHidden:"), f); }
}
