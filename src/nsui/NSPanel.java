package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSPanel — a secondary window: utility palettes, settings drawers, HUDs,
/// modal-dialog hosts. Thin 1:1 subclass of `NSWindow`; everything not listed
/// here (title, level, frame, sheets, `worksWhenModal`, ...) is inherited
/// unchanged.
///
/// A panel differs from a plain window in a few behavior flags:
/// - `floating` — the panel floats above regular windows (pair with the
///   `utilityWindow` style bit, 16, for the small-title-bar palette look).
/// - `becomesKeyOnlyIfNeeded` — the panel takes key status only when the user
///   clicks a text-entry control, so clicking it does not deactivate the main
///   window.
///
/// Create with `create` — the very same
/// `initWithContentRect:styleMask:backing:defer:` initializer
/// `NSWindow.create` uses, sent to the `NSPanel` class instead.
public class NSPanel extends NSWindow {

    protected NSPanel(MemorySegment peer) {
        super(peer);
    }

    /// Wrap a native NSPanel id (null for nil).
    public static NSPanel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPanel(peer);
    }

    /// alloc + initWithContentRect:styleMask:backing:defer: from the `NSPanel`
    /// class, with the AppKit-default backing store (`buffered`). Add
    /// `utilityWindow` (16) to `styleMask` for the classic floating palette look.
    public static NSPanel create(NSRect contentRect, long styleMask, boolean defer) {
        return create(contentRect, styleMask, NSWindow.BackingStoreType.buffered.value, defer);
    }

    /// Full form of `create` with an explicit backing store type — the exact
    /// mirror of `NSWindow.create`.
    public static NSPanel create(NSRect contentRect, long styleMask, long backingStoreType, boolean defer) {
        MemorySegment panel = ObjC.msgSendId(ObjC.cls("NSPanel"), ObjC.sel("alloc"));
        panel = ObjC.msgSendIdRectLongLongBool(panel, ObjC.sel("initWithContentRect:styleMask:backing:defer:"),
                contentRect.toSegment(), styleMask, backingStoreType, defer);
        return new NSPanel(panel);
    }

    /// [panel setBecomesKeyOnlyIfNeeded:] — setter named after the property;
    /// the getter is inherited from `NSWindow`.
    public void becomesKeyOnlyIfNeeded(boolean onlyIfNeeded) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBecomesKeyOnlyIfNeeded:"), onlyIfNeeded);
    }

    /// [panel isFloatingPanel] — true when the panel floats above regular
    /// windows (also true for any window whose styleMask includes
    /// `utilityWindow`).
    public boolean isFloatingPanel() {
        return ObjC.msgSendBool(peer, ObjC.sel("isFloatingPanel"));
    }

    /// [panel setFloatingPanel:] — toggle the float-above-windows behavior.
    public void setFloatingPanel(boolean floating) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setFloatingPanel:"), floating);
    }
}
