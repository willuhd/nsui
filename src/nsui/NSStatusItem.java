package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSStatusItem — a menu bar item. Thin 1:1 wrapper over AppKit NSStatusItem.
 * Covers the menu-bar right-side single-icon slot: title/image via the
 * status-bar button (NSStatusBarButton), expandable behavior, toolTip,
 * visibility, length, and target/action via DelegateProxy.
 */
public final class NSStatusItem extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hId;       // (id,SEL)->id [button / menu / statusBar]
    private static MethodHandle hDouble;   // (id,SEL)->double [length]
    private static MethodHandle hSetDouble;// (id,SEL,double)->void [setLength:]
    private static MethodHandle hBool;     // (id,SEL)->bool [isVisible / visible]
    private static MethodHandle hSetBool;  // (id,SEL,bool)->void
    private static MethodHandle hSetId;    // (id,SEL,id)->void [setMenu:]

    // NSStatusItemBehavior constants (AppKit)
    public static final long BEHAVIOR_DEFAULT          = 0L;
    public static final long BEHAVIOR_REMOVAL_ALLOWED  = 1L << 1; // NSStatusItemBehaviorRemovalAllowed
    public static final long BEHAVIOR_TERMINATION      = 1L << 0; // NSStatusItemBehaviorTermination
    // convenience alias: expandable / removal-allowed
    public static final long BEHAVIOR_EXPANDABLE       = BEHAVIOR_REMOVAL_ALLOWED;

    private NSStatusItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hId = ObjC.handle(Sig.of(Ret.ID));
        hDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hSetId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    public static NSStatusItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSStatusItem(peer);
    }

    // ---- button ----
    /** [statusItem button] -> NSStatusBarButton (subclass of NSButton). Uses wrap, no reflection. */
    public NSButton button() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hId.invokeExact(peer, ObjC.sel("button"));
            return NSButton.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("button failed", t); }
    }

    // ---- button convenience: title / image / toolTip ----
    /** Convenience: title via button (right-side single-icon may have title alongside image). */
    public String title() {
        NSButton b = button();
        return b == null ? null : b.title();
    }
    public void setTitle(String title) {
        NSButton b = button();
        if (b != null) b.setTitle(title == null ? "" : title);
    }
    public NSImage image() {
        NSButton b = button();
        return b == null ? null : b.image();
    }
    public void setImage(NSImage img) {
        NSButton b = button();
        if (b != null) b.setImage(img);
    }

    // ---- SF Symbol convenience (macOS 11+): imageWithSystemSymbolName: ----
    /**
     * Set the status button image via SF Symbol {@code [NSImage imageWithSystemSymbolName:accessibilityDescription:]}.
     * Falls back to {@code imageNamed:} for asset catalog names. Example: {@code "magnifyingglass"} or {@code "star.fill"}.
     */
    public void setSFSymbol(String symbolName) {
        NSImage img = NSImage.imageWithSystemSymbolName(symbolName);
        if (img == null) img = NSImage.imageNamed(symbolName);
        if (img != null) {
            try { img.setTemplate(true); } catch (Throwable ignored) {}
        }
        setImage(img);
    }

    /** Alias for {@link #setSFSymbol(String)} — also via imageWithSystemSymbolName fallback. */
    public void setImageNamed(String symbolOrAssetName) {
        setSFSymbol(symbolOrAssetName);
    }

    /**
     * Helper: set SF Symbol and return the loaded NSImage (null if not found).
     */
    public NSImage setSFSymbolAndGet(String symbolName) {
        NSImage img = NSImage.imageWithSystemSymbolName(symbolName);
        if (img == null) img = NSImage.imageNamed(symbolName);
        if (img != null) {
            try { img.setTemplate(true); } catch (Throwable ignored) {}
        }
        setImage(img);
        return img;
    }

    // ---- popover convenience — show directly from status-item click ----

    /**
     * Show the popover anchored to this status item's button.
     * Uses {@code popover.showRelativeToRect(button.bounds(), button, preferredEdge=1/MinY)}.
     */
    public void showPopover(NSPopover popover) {
        if (popover == null) return;
        NSButton b = button();
        if (b == null) return;
        popover.showRelativeToRect(b.bounds(), b, 1L);
    }

    /** Hide the popover if shown. */
    public void hidePopover(NSPopover popover) {
        if (popover == null) return;
        if (popover.isShown()) popover.close();
    }

    /** Toggle the popover anchored to this status item's button. */
    public void togglePopover(NSPopover popover) {
        if (popover == null) return;
        if (popover.isShown()) popover.close();
        else showPopover(popover);
    }

    /**
     * Wire the status button's target/action to toggle the given popover.
     * The popover is shown directly from the status item click.
     * @param popover the popover to toggle
     * @return the ObjC target id (retain to keep alive; DelegateProxy registry holds it)
     */
    public MemorySegment attachPopover(NSPopover popover) {
        return attachPopover(popover, "togglePopover:");
    }

    /**
     * Wire the status button's target/action to toggle the given popover with a custom selector.
     * @param popover the popover to toggle
     * @param selector e.g. "togglePopover:"
     * @return the ObjC target id
     */
    public MemorySegment attachPopover(NSPopover popover, String selector) {
        if (popover == null) throw new IllegalArgumentException("popover is null");
        String sel = (selector == null || selector.isEmpty()) ? "togglePopover:" : selector;
        // Ensure popover is transient by default for menu-bar behavior
        try { popover.setBehavior(1L); } catch (Throwable ignored) {}
        return setActionHandler(sel, (MemorySegment sender) -> togglePopover(popover));
    }
    /** ToolTip via button's tooltip (NSView toolTip). Guarded — no-op if selector absent. */
    public String toolTip() {
        NSButton b = button();
        if (b == null) return null;
        try {
            // NSButton (NSView) may respond to toolTip
            if (!respondsTo(b.peer(), "toolTip")) return null;
            MemorySegment s = (MemorySegment) ObjC.handle(Sig.of(Ret.ID)).invokeExact(b.peer(), ObjC.sel("toolTip"));
            return ObjC.toString(s);
        } catch (Throwable t) { return null; }
    }
    public void setToolTip(String tip) {
        NSButton b = button();
        if (b == null) return;
        try {
            if (!respondsTo(b.peer(), "setToolTip:")) return;
            ObjC.msgSendVoidId(b.peer(), ObjC.sel("setToolTip:"), tip == null ? MemorySegment.NULL : ObjC.nsstring(tip));
        } catch (Throwable ignored) {}
    }

    private static boolean respondsTo(MemorySegment obj, String sel) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(obj, ObjC.sel("respondsToSelector:"), ObjC.sel(sel));
        } catch (Throwable t) { return false; }
    }

    // ---- target / action via button + DelegateProxy ----
    /** Set target on the status button. */
    public void setTarget(MemorySegment target) {
        NSButton b = button();
        if (b != null) b.setTarget(target);
    }
    /** Set action selector on the status button (e.g., "statusClicked:"). */
    public void setAction(String sel) {
        NSButton b = button();
        if (b != null) b.setAction(sel);
    }
    /**
     * Convenience: create a DelegateProxy action target and wire it to the button.
     * @param selector e.g. "statusClicked:"
     * @param handler Java callback
     * @return the ObjC target id (retain to keep alive; registry holds it)
     */
    public MemorySegment setActionHandler(String selector, DelegateProxy.VoidArg handler) {
        MemorySegment target = DelegateProxy.actionTarget(selector, handler);
        setTarget(target);
        setAction(selector);
        return target;
    }

    // ---- menu ----
    public NSMenu menu() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hId.invokeExact(peer, ObjC.sel("menu"));
            return NSMenu.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("menu failed", t); }
    }
    public void setMenu(NSMenu menu) {
        ensureInit();
        try { hSetId.invokeExact(peer, ObjC.sel("setMenu:"), (MemorySegment) ((MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()))); } catch (Throwable t) { throw new RuntimeException("setMenu: failed", t); }
    }

    // ---- length ----
    public double length() {
        ensureInit();
        try { return (double) hDouble.invokeExact(peer, ObjC.sel("length")); } catch (Throwable t) { throw new RuntimeException("length failed", t); }
    }
    public void setLength(double len) {
        ensureInit();
        try { hSetDouble.invokeExact(peer, ObjC.sel("setLength:"), len); } catch (Throwable t) { throw new RuntimeException("setLength: failed", t); }
    }

    // ---- visible ----
    public boolean isVisible() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isVisible")); } catch (Throwable t) { throw new RuntimeException("isVisible failed", t); }
    }
    public void setVisible(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setVisible:"), flag); } catch (Throwable t) { throw new RuntimeException("setVisible: failed", t); }
    }
    // alias visible for completeness
    public boolean visible() { return isVisible(); }

    // ---- statusBar ----
    public NSStatusBar statusBar() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hId.invokeExact(peer, ObjC.sel("statusBar"));
            return NSStatusBar.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("statusBar failed", t); }
    }

    // ---- behavior ----
    public long behavior() {
        ensureInit();
        try { return ObjC.msgSendLong(peer, ObjC.sel("behavior")); } catch (Throwable t) { throw new RuntimeException("behavior failed", t); }
    }
    public void setBehavior(long b) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setBehavior:"), b);
    }

    // ---- autosaveName ----
    public String autosaveName() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("autosaveName"));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("autosaveName failed", t); }
    }
    public void setAutosaveName(String name) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAutosaveName:"), name == null ? MemorySegment.NULL : ObjC.nsstring(name));
    }
}
