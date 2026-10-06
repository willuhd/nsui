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

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment alloc;
        static MemorySegment initWithFrame;
        static MemorySegment setTitle;
        static MemorySegment title;
        static MemorySegment alternateTitle;
        static MemorySegment setAlternateTitle;
        static MemorySegment attributedTitle;
        static MemorySegment setAttributedTitle;
        static MemorySegment attributedAlternateTitle;
        static MemorySegment setAttributedAlternateTitle;
        static MemorySegment sizeToFit;
        static MemorySegment bezelStyle;
        static MemorySegment setBezelStyle;
        static MemorySegment setButtonType;
        static MemorySegment state;
        static MemorySegment setState;
        static MemorySegment setNextState;
        static MemorySegment allowsMixedState;
        static MemorySegment setAllowsMixedState;
        static MemorySegment highlight;
        static MemorySegment isBordered;
        static MemorySegment setBordered;
        static MemorySegment isTransparent;
        static MemorySegment setTransparent;
        static MemorySegment showsBorderOnlyWhileMouseInside;
        static MemorySegment setShowsBorderOnlyWhileMouseInside;
        static MemorySegment image;
        static MemorySegment setImage;
        static MemorySegment alternateImage;
        static MemorySegment setAlternateImage;
        static MemorySegment imagePosition;
        static MemorySegment setImagePosition;
        static MemorySegment imageScaling;
        static MemorySegment setImageScaling;
        static MemorySegment imageHugsTitle;
        static MemorySegment setImageHugsTitle;
        static MemorySegment keyEquivalent;
        static MemorySegment setKeyEquivalent;
        static MemorySegment keyEquivalentModifierMask;
        static MemorySegment setKeyEquivalentModifierMask;
        static MemorySegment sound;
        static MemorySegment setSound;
        static MemorySegment isSpringLoaded;
        static MemorySegment setSpringLoaded;
        static MemorySegment bezelColor;
        static MemorySegment setBezelColor;
        static MemorySegment contentTintColor;
        static MemorySegment setContentTintColor;
        static MemorySegment setPeriodicDelay_interval;
        static MemorySegment hasDestructiveAction;
        static MemorySegment setHasDestructiveAction;
        static MemorySegment buttonWithTitle_target_action;
        static MemorySegment buttonWithImage_target_action;
        static MemorySegment checkboxWithTitle_target_action;
        static MemorySegment radioButtonWithTitle_target_action;
        static MemorySegment buttonWithTitle_image_target_action;
        static MemorySegment maxAcceleratorLevel;
        static MemorySegment setMaxAcceleratorLevel;
        static MemorySegment symbolConfiguration;
        static MemorySegment setSymbolConfiguration;
        static MemorySegment respondsToSelector;
        static MemorySegment tintProminence;
        static MemorySegment setTintProminence;
        static MemorySegment borderShape;
        static MemorySegment setBorderShape;
        static MemorySegment performKeyEquivalent;
        static MemorySegment compressWithPrioritizedCompressionOptions;
        static MemorySegment activeCompressionOptions;
        static void populate() {
            alloc = ObjC.sel("alloc");
            initWithFrame = ObjC.sel("initWithFrame:");
            setTitle = ObjC.sel("setTitle:");
            title = ObjC.sel("title");
            alternateTitle = ObjC.sel("alternateTitle");
            setAlternateTitle = ObjC.sel("setAlternateTitle:");
            attributedTitle = ObjC.sel("attributedTitle");
            setAttributedTitle = ObjC.sel("setAttributedTitle:");
            attributedAlternateTitle = ObjC.sel("attributedAlternateTitle");
            setAttributedAlternateTitle = ObjC.sel("setAttributedAlternateTitle:");
            sizeToFit = ObjC.sel("sizeToFit");
            bezelStyle = ObjC.sel("bezelStyle");
            setBezelStyle = ObjC.sel("setBezelStyle:");
            setButtonType = ObjC.sel("setButtonType:");
            state = ObjC.sel("state");
            setState = ObjC.sel("setState:");
            setNextState = ObjC.sel("setNextState");
            allowsMixedState = ObjC.sel("allowsMixedState");
            setAllowsMixedState = ObjC.sel("setAllowsMixedState:");
            highlight = ObjC.sel("highlight:");
            isBordered = ObjC.sel("isBordered");
            setBordered = ObjC.sel("setBordered:");
            isTransparent = ObjC.sel("isTransparent");
            setTransparent = ObjC.sel("setTransparent:");
            showsBorderOnlyWhileMouseInside = ObjC.sel("showsBorderOnlyWhileMouseInside");
            setShowsBorderOnlyWhileMouseInside = ObjC.sel("setShowsBorderOnlyWhileMouseInside:");
            image = ObjC.sel("image");
            setImage = ObjC.sel("setImage:");
            alternateImage = ObjC.sel("alternateImage");
            setAlternateImage = ObjC.sel("setAlternateImage:");
            imagePosition = ObjC.sel("imagePosition");
            setImagePosition = ObjC.sel("setImagePosition:");
            imageScaling = ObjC.sel("imageScaling");
            setImageScaling = ObjC.sel("setImageScaling:");
            imageHugsTitle = ObjC.sel("imageHugsTitle");
            setImageHugsTitle = ObjC.sel("setImageHugsTitle:");
            keyEquivalent = ObjC.sel("keyEquivalent");
            setKeyEquivalent = ObjC.sel("setKeyEquivalent:");
            keyEquivalentModifierMask = ObjC.sel("keyEquivalentModifierMask");
            setKeyEquivalentModifierMask = ObjC.sel("setKeyEquivalentModifierMask:");
            sound = ObjC.sel("sound");
            setSound = ObjC.sel("setSound:");
            isSpringLoaded = ObjC.sel("isSpringLoaded");
            setSpringLoaded = ObjC.sel("setSpringLoaded:");
            bezelColor = ObjC.sel("bezelColor");
            setBezelColor = ObjC.sel("setBezelColor:");
            contentTintColor = ObjC.sel("contentTintColor");
            setContentTintColor = ObjC.sel("setContentTintColor:");
            setPeriodicDelay_interval = ObjC.sel("setPeriodicDelay:interval:");
            hasDestructiveAction = ObjC.sel("hasDestructiveAction");
            setHasDestructiveAction = ObjC.sel("setHasDestructiveAction:");
            buttonWithTitle_target_action = ObjC.sel("buttonWithTitle:target:action:");
            buttonWithImage_target_action = ObjC.sel("buttonWithImage:target:action:");
            checkboxWithTitle_target_action = ObjC.sel("checkboxWithTitle:target:action:");
            radioButtonWithTitle_target_action = ObjC.sel("radioButtonWithTitle:target:action:");
            buttonWithTitle_image_target_action = ObjC.sel("buttonWithTitle:image:target:action:");
            maxAcceleratorLevel = ObjC.sel("maxAcceleratorLevel");
            setMaxAcceleratorLevel = ObjC.sel("setMaxAcceleratorLevel:");
            symbolConfiguration = ObjC.sel("symbolConfiguration");
            setSymbolConfiguration = ObjC.sel("setSymbolConfiguration:");
            respondsToSelector = ObjC.sel("respondsToSelector:");
            tintProminence = ObjC.sel("tintProminence");
            setTintProminence = ObjC.sel("setTintProminence:");
            borderShape = ObjC.sel("borderShape");
            setBorderShape = ObjC.sel("setBorderShape:");
            performKeyEquivalent = ObjC.sel("performKeyEquivalent:");
            compressWithPrioritizedCompressionOptions = ObjC.sel("compressWithPrioritizedCompressionOptions:");
            activeCompressionOptions = ObjC.sel("activeCompressionOptions");
        }
    }

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

    private static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (H != null) return;
        H = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
            Sels.populate();
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
        MemorySegment b = ObjC.msgSendId(ObjC.cls("NSButton"), Sels.alloc);
        try {
            b = (MemorySegment) H.hInitFrame().invokeExact(b, Sels.initWithFrame, frame.toSegment());
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
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setTitle, ObjC.nsstring(title));
    }

    /// [button title] — the current title (NSString -> String).
    public String title() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.title));
    }

    /// [button alternateTitle] — the alternate title (for stateful buttons).
    public String alternateTitle() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.alternateTitle));
    }
    public void setAlternateTitle(String t) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setAlternateTitle, ObjC.nsstring(t));
    }

    /// [button attributedTitle] — NSAttributedString id.
    public MemorySegment attributedTitle() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.attributedTitle);
    }
    public void setAttributedTitle(MemorySegment attr) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setAttributedTitle, attr);
    }
    public MemorySegment attributedAlternateTitle() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.attributedAlternateTitle);
    }
    public void setAttributedAlternateTitle(MemorySegment attr) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setAttributedAlternateTitle, attr);
    }

    /// [button sizeToFit] — size the button to its intrinsic content.
    public void sizeToFit() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.sizeToFit);
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
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.bezelStyle);
    }
    /// Typed getter.
    public BezelStyle bezelStyleEnum() { return BezelStyle.fromValue(bezelStyle()); }
    /// [button setBezelStyle:] — NSBezelStyle (1 = Rounded).
    public void setBezelStyle(long style) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setBezelStyle, style);
    }
    /// Typed overload.
    public void setBezelStyle(BezelStyle s) { setBezelStyle(s.value); }

    /// [button setButtonType:] — NSButtonType (0 = MomentaryPushIn).
    public void setButtonType(long type) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setButtonType, type);
    }
    /// Typed overload.
    public void setButtonType(ButtonType t) { setButtonType(t.value); }

    // ---- state ----
    public long state() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.state);
    }
    public void setState(long state) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setState, state);
    }
    public void setNextState() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.setNextState);
    }
    public boolean allowsMixedState() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsMixedState);
    }
    public void setAllowsMixedState(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAllowsMixedState, flag);
    }
    public void highlight(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.highlight, flag);
    }

    // ---- bordered / transparent ----
    public boolean isBordered() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isBordered);
    }
    public void setBordered(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setBordered, flag);
    }
    public boolean isTransparent() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isTransparent);
    }
    public void setTransparent(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setTransparent, flag);
    }
    public boolean showsBorderOnlyWhileMouseInside() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.showsBorderOnlyWhileMouseInside);
    }
    public void setShowsBorderOnlyWhileMouseInside(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setShowsBorderOnlyWhileMouseInside, flag);
    }

    // ---- image ----
    public NSImage image() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(peer, Sels.image);
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setImage(NSImage img) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setImage, (MemorySegment) (img == null ? MemorySegment.NULL : img.peer()));
    }
    public NSImage alternateImage() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(peer, Sels.alternateImage);
        return (p == null || p.address() == 0) ? null : NSImage.wrap(p);
    }
    public void setAlternateImage(NSImage img) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setAlternateImage, (MemorySegment) (img == null ? MemorySegment.NULL : img.peer()));
    }
    public long imagePosition() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.imagePosition);
    }
    public void setImagePosition(long pos) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setImagePosition, pos);
    }
    public long imageScaling() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.imageScaling);
    }
    public void setImageScaling(long scaling) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setImageScaling, scaling);
    }
    public boolean imageHugsTitle() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.imageHugsTitle);
    }
    public void setImageHugsTitle(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setImageHugsTitle, flag);
    }

    // ---- keyEquivalent ----
    public String keyEquivalent() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.keyEquivalent));
    }
    public void setKeyEquivalent(String ke) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setKeyEquivalent, ObjC.nsstring(ke));
    }
    public long keyEquivalentModifierMask() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.keyEquivalentModifierMask);
    }
    public void setKeyEquivalentModifierMask(long mask) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setKeyEquivalentModifierMask, mask);
    }

    // ---- sound ----
    public MemorySegment sound() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.sound);
    }
    public void setSound(MemorySegment sound) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setSound, (MemorySegment) (sound == null ? MemorySegment.NULL : sound));
    }

    // ---- springLoaded / colors ----
    public boolean isSpringLoaded() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isSpringLoaded);
    }
    public void setSpringLoaded(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setSpringLoaded, flag);
    }
    public MemorySegment bezelColor() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.bezelColor);
    }
    public void setBezelColor(NSColor c) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setBezelColor, (MemorySegment) (c == null ? MemorySegment.NULL : c.peer()));
    }
    public MemorySegment contentTintColor() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.contentTintColor);
    }
    public void setContentTintColor(NSColor c) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setContentTintColor, (MemorySegment) (c == null ? MemorySegment.NULL : c.peer()));
    }

    // ---- periodic delay ----
    public void setPeriodicDelay(float delay, float interval) {
        ensureInit();
        try {
            H.hSetPeriodicDelay().invokeExact(peer, Sels.setPeriodicDelay_interval, delay, interval);
        } catch (Throwable t) {
            throw new RuntimeException("setPeriodicDelay:interval: failed", t);
        }
    }

    // ---- hasDestructiveAction ----
    public boolean hasDestructiveAction() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.hasDestructiveAction);
    }
    public void setHasDestructiveAction(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setHasDestructiveAction, flag);
    }

    // ---- standard factories (NSButton.h "Creating Standard Buttons") ----
    /// `+buttonWithTitle:target:action:` — a standard push button (nil-safe target/action).
    public static NSButton buttonWithTitle(String title, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.msgSendIdIdSelId(ObjC.cls("NSButton"), Sels.buttonWithTitle_target_action,
                ObjC.nsstring(title == null ? "" : title),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("buttonWithTitle:target:action: returned nil");
        return new NSButton(b);
    }
    /// `+buttonWithImage:target:action:` — a standard image button (nil-safe image/target/action).
    public static NSButton buttonWithImage(NSImage image, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.msgSendIdIdSelId(ObjC.cls("NSButton"), Sels.buttonWithImage_target_action,
                (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("buttonWithImage:target:action: returned nil");
        return new NSButton(b);
    }
    /// `+checkboxWithTitle:target:action:` — a standard checkbox (nil-safe target/action).
    public static NSButton checkboxWithTitle(String title, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.msgSendIdIdSelId(ObjC.cls("NSButton"), Sels.checkboxWithTitle_target_action,
                ObjC.nsstring(title == null ? "" : title),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("checkboxWithTitle:target:action: returned nil");
        return new NSButton(b);
    }
    /// `+radioButtonWithTitle:target:action:` — a standard radio button (nil-safe target/action).
    public static NSButton radioButtonWithTitle(String title, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.msgSendIdIdSelId(ObjC.cls("NSButton"), Sels.radioButtonWithTitle_target_action,
                ObjC.nsstring(title == null ? "" : title),
                target == null ? MemorySegment.NULL : target,
                (actionSelector == null || actionSelector.isEmpty()) ? MemorySegment.NULL : ObjC.sel(actionSelector));
        if (b == null || b.address() == 0) throw new IllegalStateException("radioButtonWithTitle:target:action: returned nil");
        return new NSButton(b);
    }
    /// `+buttonWithTitle:image:target:action:` — title plus image (all object args; via `ObjC.invoke`).
    public static NSButton buttonWithTitleImage(String title, NSImage image, MemorySegment target, String actionSelector) {
        ensureInit();
        MemorySegment b = ObjC.invoke(ObjC.cls("NSButton"), Sels.buttonWithTitle_image_target_action,
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
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.maxAcceleratorLevel);
    }
    /// [button setMaxAcceleratorLevel:] — allowed values 1..5.
    public void setMaxAcceleratorLevel(long level) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setMaxAcceleratorLevel, level);
    }
    /// [button symbolConfiguration] — sizing for symbol images (raw id; nil becomes NULL).
    public MemorySegment symbolConfiguration() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.symbolConfiguration);
    }
    /// [button setSymbolConfiguration:] — pass `MemorySegment.NULL` for nil.
    public void setSymbolConfiguration(MemorySegment config) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setSymbolConfiguration, config);
    }

    // ---- macOS 26 tint / border shape (guarded: 0 / no-op where absent) ----
    /// [button tintProminence] — macOS 26+; 0 when the selector is absent.
    public long tintProminence() {
        ensureInit();
        try {
            boolean responds = (boolean) H.hResponds().invokeExact(peer, Sels.respondsToSelector, Sels.tintProminence);
            if (!responds) return 0L;
            return ObjC.msgSendLong(peer, Sels.tintProminence);
        } catch (Throwable t) { throw new RuntimeException("tintProminence failed", t); }
    }
    /// [button setTintProminence:] — macOS 26+; no-op when absent.
    public void setTintProminence(long prominence) {
        ensureInit();
        try {
            boolean responds = (boolean) H.hResponds().invokeExact(peer, Sels.respondsToSelector, Sels.setTintProminence);
            if (!responds) return;
            ObjC.msgSendVoidLong(peer, Sels.setTintProminence, prominence);
        } catch (Throwable t) { throw new RuntimeException("setTintProminence: failed", t); }
    }
    /// [button borderShape] — NSControlBorderShape (0=Automatic); macOS 26+, 0 when absent.
    public long borderShape() {
        ensureInit();
        try {
            boolean responds = (boolean) H.hResponds().invokeExact(peer, Sels.respondsToSelector, Sels.borderShape);
            if (!responds) return 0L;
            return ObjC.msgSendLong(peer, Sels.borderShape);
        } catch (Throwable t) { throw new RuntimeException("borderShape failed", t); }
    }
    /// [button setBorderShape:] — macOS 26+; no-op when absent.
    public void setBorderShape(long shape) {
        ensureInit();
        try {
            boolean responds = (boolean) H.hResponds().invokeExact(peer, Sels.respondsToSelector, Sels.setBorderShape);
            if (!responds) return;
            ObjC.msgSendVoidLong(peer, Sels.setBorderShape, shape);
        } catch (Throwable t) { throw new RuntimeException("setBorderShape: failed", t); }
    }

    // ---- keyboard / compression ----
    /// [button performKeyEquivalent:] — YES when the event matches (nil-safe event).
    public boolean performKeyEquivalent(NSEvent event) {
        ensureInit();
        try { return (boolean) H.hBoolId().invokeExact(peer, Sels.performKeyEquivalent, (MemorySegment) (event == null ? MemorySegment.NULL : event.peer())); } catch (Throwable t) { throw new RuntimeException("performKeyEquivalent: failed", t); }
    }
    /// [button compressWithPrioritizedCompressionOptions:] — compress per prioritized options.
    public void compressWithPrioritizedCompressionOptions(NSArray options) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.compressWithPrioritizedCompressionOptions, (MemorySegment) (options == null ? MemorySegment.NULL : options.peer()));
    }
    /// [button activeCompressionOptions] — options currently applied (raw id; nil becomes NULL).
    public MemorySegment activeCompressionOptions() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.activeCompressionOptions);
    }
}
