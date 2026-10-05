package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSButton — an AppKit push-button control. Thin, 1:1, stateless wrapper over a
/// native `NSButton` (SWT-style): every method maps to one `objc_msgSend`
/// selector, no cached Java state beyond the peer. Mirrors the native hierarchy:
/// NSButton is an NSControl is an NSView, so buttons drop into any view hierarchy.
///
/// The most common path is `create`: `[[NSButton alloc] initWithFrame:]`
/// then `setTitle:`, `setTarget:`/`setAction:`, bezel + button type,
/// `sizeToFit` and a `setFrame:` with the fitted size. The action target is
/// an ObjC instance built by `DelegateProxy.actionTarget` — passing raw selector
/// names into `setAction:` on the native side.
///
/// Omitted from `NSButton.h`: `getPeriodicDelay:interval:` (float* out-params,
/// no registered shape); `minimumSizeWithPrioritizedCompressionOptions:`
/// (no `size(id)` shape — requested); deprecated `setTitleWithMnemonic:`.
/// The 4- and 5-argument all-object factories use the documented AOT-safe
/// `ObjC.invoke` escape hatch (NULL-padded to the registered 6-object shape).
public final class NSButton extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hInitFrame, MethodHandle hSetPeriodicDelay, MethodHandle hResponds, MethodHandle hBoolId) {}
    private static volatile Handles H;

    private NSButton(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing native NSButton/NSStatusBarButton peer (no ownership change).
    public static NSButton wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSButton(peer);
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
    }

    /// `[[NSButton alloc] initWithFrame:frame]` then configure bezel/type and
    /// wire the target/action, then `sizeToFit` and re-apply `setFrame:`
    /// with the fitted size (keeps the requested origin, adopts the intrinsic size).
    ///
    /// @param frame          the requested frame (origin honored, size replaced by the fitted size)
    /// @param title          the button's title
    /// @param target         the ObjC action target (e.g. from `DelegateProxy.actionTarget`)
    /// @param actionSelector the ObjC selector the control fires against `target`,
    /// e.g. `"pressed:"`
    public static NSButton create(NSRect frame, String title, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.msgSendId(ObjC.cls("NSButton"), ObjC.sel("alloc"));
        try {
            b = (MemorySegment) H.hInitFrame().invokeExact(b, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSButton", t);
        }
        if (b.address() == 0) {
            throw new IllegalStateException("NSButton alloc/initWithFrame: returned nil");
        }
        NSButton button = new NSButton(b);

        button.setTitle(title);
        button.setTarget(target);
        button.setAction(actionSelector);
        button.setBezelStyle(1L);        // NSBezelStyleRounded
        button.setButtonType(0L);        // NSButtonTypeMomentaryPushIn
        button.sizeToFit();

        // Re-apply the frame with the fitted size, preserving the requested origin.
        NSRect fitted = button.frame();
        button.setFrame(new NSRect(frame.x(), frame.y(), fitted.width(), fitted.height()));
        return button;
    }

    // ---------------------------------------------------------------- instance API

    /// [button setTitle:] — the string shown on the bezel.
    public void setTitle(String title) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTitle:"), ObjC.nsstring(title));
    }

    /// [button title] — the current title (NSString -> String).
    public String title() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("title")));
    }

    /// [button alternateTitle] — the alternate title (for stateful buttons).
    public String alternateTitle() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("alternateTitle")));
    }
    public void setAlternateTitle(String t) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAlternateTitle:"), ObjC.nsstring(t));
    }

    /// [button attributedTitle] — NSAttributedString id.
    public MemorySegment attributedTitle() {
        return ObjC.msgSendId(peer, ObjC.sel("attributedTitle"));
    }
    public void setAttributedTitle(MemorySegment attr) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAttributedTitle:"), attr);
    }
    public MemorySegment attributedAlternateTitle() {
        return ObjC.msgSendId(peer, ObjC.sel("attributedAlternateTitle"));
    }
    public void setAttributedAlternateTitle(MemorySegment attr) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAttributedAlternateTitle:"), attr);
    }

    /// [button sizeToFit] — size the button to its intrinsic content.
    public void sizeToFit() {
        ObjC.msgSendVoid(peer, ObjC.sel("sizeToFit"));
    }

    // ---------------------------------------------------------------- nested enums — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSButtonCell.h
    //   NSBezelStyle / NSButtonType
    // Docs: https://developer.apple.com/documentation/appkit/nsbezelstyle
    // Docs: https://developer.apple.com/documentation/appkit/nsbuttontype

    /// `NSBezelStyle` — values from `NSButtonCell.h` `typedef NS_ENUM(NSUInteger, NSBezelStyle)`.
    /// Canonical: Automatic 0, Push 1 (=Rounded), FlexiblePush 2 (=RegularSquare), Disclosure 5, ShadowlessSquare 6, Circular 7, TexturedSquare 8, HelpButton 9, SmallSquare 10, Toolbar 11 (=TexturedRounded), AccessoryBarAction 12 (=RoundRect), AccessoryBar 13 (=Recessed), PushDisclosure 14 (=RoundedDisclosure), Badge 15 (=Inline), Glass 16.
    public enum BezelStyle {
        automatic(0),
        push(1),                  // NSRoundedBezelStyle deprecated alias
        flexiblePush(2),         // NSRegularSquareBezelStyle alias
        disclosure(5),
        shadowlessSquare(6),
        circular(7),
        texturedSquare(8),
        helpButton(9),
        smallSquare(10),
        toolbar(11),              // NSTexturedRoundedBezelStyle alias
        accessoryBarAction(12), // NSRoundRectBezelStyle alias
        accessoryBar(13),        // NSRecessedBezelStyle alias
        pushDisclosure(14),
        badge(15),                // NSInlineBezelStyle alias
        glass(16);
        public final long value;
        BezelStyle(long v) { this.value = v; }
        public static BezelStyle fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSButtonType` — values from `NSButtonCell.h` `typedef NS_ENUM(NSUInteger, NSButtonType)`.
    public enum ButtonType {
        momentaryLight(0), momentaryPushIn(7), // 7 is the common push-button momentaryPushIn used by NSButton.create
        pushOnPushOff(1), toggle(2), switchButton(3), radio(4), momentaryChange(5), onOff(6),
        accelerator(8), multiLevelAccelerator(9);
        public final long value;
        ButtonType(long v) { this.value = v; }
        public static ButtonType fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// [button bezelStyle] — NSBezelStyle.
    public long bezelStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("bezelStyle"));
    }
    /// Typed getter.
    public BezelStyle bezelStyleEnum() { return BezelStyle.fromValue(bezelStyle()); }
    /// [button setBezelStyle:] — NSBezelStyle (1 = Rounded).
    public void setBezelStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setBezelStyle:"), style);
    }
    /// Typed overload.
    public void setBezelStyle(BezelStyle s) { setBezelStyle(s.value); }

    /// [button setButtonType:] — NSButtonType (0 = MomentaryPushIn).
    public void setButtonType(long type) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setButtonType:"), type);
    }
    /// Typed overload.
    public void setButtonType(ButtonType t) { setButtonType(t.value); }

    // ---- state ----
    public long state() {
        return ObjC.msgSendLong(peer, ObjC.sel("state"));
    }
    public void setState(long state) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setState:"), state);
    }
    public void setNextState() {
        ObjC.msgSendVoid(peer, ObjC.sel("setNextState"));
    }
    public boolean allowsMixedState() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsMixedState"));
    }
    public void setAllowsMixedState(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsMixedState:"), flag);
    }
    public void highlight(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("highlight:"), flag);
    }

    // ---- bordered / transparent ----
    public boolean isBordered() {
        return ObjC.msgSendBool(peer, ObjC.sel("isBordered"));
    }
    public void setBordered(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBordered:"), flag);
    }
    public boolean isTransparent() {
        return ObjC.msgSendBool(peer, ObjC.sel("isTransparent"));
    }
    public void setTransparent(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setTransparent:"), flag);
    }
    public boolean showsBorderOnlyWhileMouseInside() {
        return ObjC.msgSendBool(peer, ObjC.sel("showsBorderOnlyWhileMouseInside"));
    }
    public void setShowsBorderOnlyWhileMouseInside(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setShowsBorderOnlyWhileMouseInside:"), flag);
    }

    // ---- image ----
    public NSImage image() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("image"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setImage(NSImage img) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setImage:"), (MemorySegment) (img == null ? MemorySegment.NULL : img.peer()));
    }
    public NSImage alternateImage() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("alternateImage"));
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setAlternateImage(NSImage img) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAlternateImage:"), (MemorySegment) (img == null ? MemorySegment.NULL : img.peer()));
    }
    public long imagePosition() {
        return ObjC.msgSendLong(peer, ObjC.sel("imagePosition"));
    }
    public void setImagePosition(long pos) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setImagePosition:"), pos);
    }
    public long imageScaling() {
        return ObjC.msgSendLong(peer, ObjC.sel("imageScaling"));
    }
    public void setImageScaling(long scaling) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setImageScaling:"), scaling);
    }
    public boolean imageHugsTitle() {
        return ObjC.msgSendBool(peer, ObjC.sel("imageHugsTitle"));
    }
    public void setImageHugsTitle(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setImageHugsTitle:"), flag);
    }

    // ---- keyEquivalent ----
    public String keyEquivalent() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("keyEquivalent")));
    }
    public void setKeyEquivalent(String ke) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setKeyEquivalent:"), ObjC.nsstring(ke));
    }
    public long keyEquivalentModifierMask() {
        return ObjC.msgSendLong(peer, ObjC.sel("keyEquivalentModifierMask"));
    }
    public void setKeyEquivalentModifierMask(long mask) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setKeyEquivalentModifierMask:"), mask);
    }

    // ---- sound ----
    public MemorySegment sound() {
        return ObjC.msgSendId(peer, ObjC.sel("sound"));
    }
    public void setSound(MemorySegment sound) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSound:"), (MemorySegment) (sound == null ? MemorySegment.NULL : sound));
    }

    // ---- springLoaded / colors ----
    public boolean isSpringLoaded() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSpringLoaded"));
    }
    public void setSpringLoaded(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setSpringLoaded:"), flag);
    }
    public MemorySegment bezelColor() {
        return ObjC.msgSendId(peer, ObjC.sel("bezelColor"));
    }
    public void setBezelColor(NSColor c) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBezelColor:"), (MemorySegment) (c == null ? MemorySegment.NULL : c.peer()));
    }
    public MemorySegment contentTintColor() {
        return ObjC.msgSendId(peer, ObjC.sel("contentTintColor"));
    }
    public void setContentTintColor(NSColor c) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setContentTintColor:"), (MemorySegment) (c == null ? MemorySegment.NULL : c.peer()));
    }

    // ---- periodic delay ----
    public void setPeriodicDelay(float delay, float interval) {
        ensureInit();
        try {
            H.hSetPeriodicDelay().invokeExact(peer, ObjC.sel("setPeriodicDelay:interval:"), delay, interval);
        } catch (Throwable t) {
            throw new RuntimeException("setPeriodicDelay:interval: failed", t);
        }
    }

    // ---- hasDestructiveAction ----
    public boolean hasDestructiveAction() {
        return ObjC.msgSendBool(peer, ObjC.sel("hasDestructiveAction"));
    }
    public void setHasDestructiveAction(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setHasDestructiveAction:"), flag);
    }

    // ---- standard factories (NSButton.h "Creating Standard Buttons") ----
    /// `+buttonWithTitle:target:action:` — a standard push button (nil-safe target/action).
    public static NSButton buttonWithTitle(String title, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.msgSendIdIdSelId(ObjC.cls("NSButton"), ObjC.sel("buttonWithTitle:target:action:"),
                ObjC.nsstring(title == null ? "" : title),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("buttonWithTitle:target:action: returned nil");
        return new NSButton(b);
    }
    /// `+buttonWithImage:target:action:` — a standard image button (nil-safe image/target/action).
    public static NSButton buttonWithImage(NSImage image, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.msgSendIdIdSelId(ObjC.cls("NSButton"), ObjC.sel("buttonWithImage:target:action:"),
                (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("buttonWithImage:target:action: returned nil");
        return new NSButton(b);
    }
    /// `+checkboxWithTitle:target:action:` — a standard checkbox (nil-safe target/action).
    public static NSButton checkboxWithTitle(String title, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.msgSendIdIdSelId(ObjC.cls("NSButton"), ObjC.sel("checkboxWithTitle:target:action:"),
                ObjC.nsstring(title == null ? "" : title),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("checkboxWithTitle:target:action: returned nil");
        return new NSButton(b);
    }
    /// `+radioButtonWithTitle:target:action:` — a standard radio button (nil-safe target/action).
    public static NSButton radioButtonWithTitle(String title, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.msgSendIdIdSelId(ObjC.cls("NSButton"), ObjC.sel("radioButtonWithTitle:target:action:"),
                ObjC.nsstring(title == null ? "" : title),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("radioButtonWithTitle:target:action: returned nil");
        return new NSButton(b);
    }
    /// `+buttonWithTitle:image:target:action:` — title plus image (all object args; via `ObjC.invoke`).
    public static NSButton buttonWithTitleImage(String title, NSImage image, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.invoke(ObjC.cls("NSButton"), ObjC.sel("buttonWithTitle:image:target:action:"),
                ObjC.nsstring(title == null ? "" : title),
                (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("buttonWithTitle:image:target:action: returned nil");
        return new NSButton(b);
    }

    // ---- accelerator / symbol ----
    /// [button maxAcceleratorLevel] — max level for multi-level accelerator buttons (default 2).
    public long maxAcceleratorLevel() {
        return ObjC.msgSendLong(peer, ObjC.sel("maxAcceleratorLevel"));
    }
    /// [button setMaxAcceleratorLevel:] — allowed values 1..5.
    public void setMaxAcceleratorLevel(long level) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setMaxAcceleratorLevel:"), level);
    }
    /// [button symbolConfiguration] — sizing for symbol images (raw id; nil becomes NULL).
    public MemorySegment symbolConfiguration() {
        return ObjC.msgSendId(peer, ObjC.sel("symbolConfiguration"));
    }
    /// [button setSymbolConfiguration:] — pass `MemorySegment.NULL` for nil.
    public void setSymbolConfiguration(MemorySegment config) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSymbolConfiguration:"), config);
    }

    // ---- macOS 26 tint / border shape (guarded: 0 / no-op where absent) ----
    /// [button tintProminence] — macOS 26+; 0 when the selector is absent.
    public long tintProminence() {
        ensureInit();
        try {
            boolean responds = (boolean) H.hResponds().invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("tintProminence"));
            if (!responds) return 0L;
            return ObjC.msgSendLong(peer, ObjC.sel("tintProminence"));
        } catch (Throwable t) { throw new RuntimeException("tintProminence failed", t); }
    }
    /// [button setTintProminence:] — macOS 26+; no-op when absent.
    public void setTintProminence(long prominence) {
        ensureInit();
        try {
            boolean responds = (boolean) H.hResponds().invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("setTintProminence:"));
            if (!responds) return;
            ObjC.msgSendVoidLong(peer, ObjC.sel("setTintProminence:"), prominence);
        } catch (Throwable t) { throw new RuntimeException("setTintProminence: failed", t); }
    }
    /// [button borderShape] — NSControlBorderShape (0=Automatic); macOS 26+, 0 when absent.
    public long borderShape() {
        ensureInit();
        try {
            boolean responds = (boolean) H.hResponds().invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("borderShape"));
            if (!responds) return 0L;
            return ObjC.msgSendLong(peer, ObjC.sel("borderShape"));
        } catch (Throwable t) { throw new RuntimeException("borderShape failed", t); }
    }
    /// [button setBorderShape:] — macOS 26+; no-op when absent.
    public void setBorderShape(long shape) {
        ensureInit();
        try {
            boolean responds = (boolean) H.hResponds().invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel("setBorderShape:"));
            if (!responds) return;
            ObjC.msgSendVoidLong(peer, ObjC.sel("setBorderShape:"), shape);
        } catch (Throwable t) { throw new RuntimeException("setBorderShape: failed", t); }
    }

    // ---- keyboard / compression ----
    /// [button performKeyEquivalent:] — YES when the event matches (nil-safe event).
    public boolean performKeyEquivalent(NSEvent event) {
        ensureInit();
        try { return (boolean) H.hBoolId().invokeExact(peer, ObjC.sel("performKeyEquivalent:"), (MemorySegment) (event == null ? MemorySegment.NULL : event.peer())); } catch (Throwable t) { throw new RuntimeException("performKeyEquivalent: failed", t); }
    }
    /// [button compressWithPrioritizedCompressionOptions:] — compress per prioritized options.
    public void compressWithPrioritizedCompressionOptions(NSArray options) {
        ObjC.msgSendVoidId(peer, ObjC.sel("compressWithPrioritizedCompressionOptions:"), (MemorySegment) (options == null ? MemorySegment.NULL : options.peer()));
    }
    /// [button activeCompressionOptions] — options currently applied (raw id; nil becomes NULL).
    public MemorySegment activeCompressionOptions() {
        return ObjC.msgSendId(peer, ObjC.sel("activeCompressionOptions"));
    }
}
