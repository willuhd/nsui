package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPopoverTouchBarItem — a Touch Bar item that opens a second `NSTouchBar`
/// in place of the currently visible one. Assigning that second bar to this
/// item is how a "menu" appears inside the Touch Bar: while the item's
/// collapsed representation (a button by default) sits in the hosted bar,
/// tapping it slides the popover bar down over the main bar; dismissing
/// (close button or `dismissPopover:`) restores the previous bar.
///
/// Selector note: the Objective-C property is declared
/// `@property (strong) NSTouchBar *popoverTouchBar` — the Java accessors are
/// named `popover()`/`setPopover(NSTouchBar)` but bind to the real selectors
/// `popoverTouchBar`/`setPopoverTouchBar:` (the property type is NSTouchBar*;
/// there is no `setPopover:` method on this class). All selectors are guarded
/// with `respondsToSelector:` like `NSCustomTouchBarItem`.
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSPopoverTouchBarItem.h
/// Coverage: COMPLETE after this batch — popoverTouchBar/customizationLabel/collapsedRepresentation/
/// collapsedRepresentationImage/collapsedRepresentationLabel/pressAndHoldTouchBar/showsCloseButton/
/// showPopover:/dismissPopover: (both arities)/makeStandardActivatePopoverGestureRecognizer.
/// All shapes are the registered (ID ())/(VOID,ID)/(BOOL ())/(VOID,BOOL) pair or the guarded
/// optional-selector pattern. No omissions (makeStandardActivatePopoverGestureRecognizer is iOS-unavailable
/// per header, but the selector is still guarded like the rest; on macOS it returns a recognizer).
public class NSPopoverTouchBarItem extends NSTouchBarItem {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hInitId, MethodHandle hId, MethodHandle hVoidId) {}
    private static volatile Handles handles;

    protected NSPopoverTouchBarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSPopoverTouchBarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPopoverTouchBarItem(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
        );
    }

    /// alloc + initWithIdentifier: — popover item; its popover bar starts as
    /// an empty, non-customizable bar until you assign one via setPopover.
    public static NSPopoverTouchBarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPopoverTouchBarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitId().invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSPopoverTouchBarItem", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSPopoverTouchBarItem alloc/initWithIdentifier: returned nil");
        return new NSPopoverTouchBarItem(p);
    }

    /// True when the peer implements the given selector.
    private boolean responds(String selectorName) {
        try {
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel(selectorName));
        } catch (Throwable t) {
            return false;
        }
    }

    /// popoverTouchBar — the NSTouchBar displayed when this item is popped
    /// (defaults to an empty bar). Null when the selector is unavailable.
    public NSTouchBar popover() {
        ensureInit();
        if (!responds("popoverTouchBar")) return null;
        try {
            MemorySegment b = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("popoverTouchBar"));
            return NSTouchBar.wrap(b);
        } catch (Throwable t) {
            throw new RuntimeException("popoverTouchBar failed", t);
        }
    }

    /// setPopoverTouchBar: — assign the bar shown when the item is tapped.
    /// This assignment is what turns the item into an in-Touch-Bar "menu":
    /// build a second NSTouchBar (its items supplied by defaultItemIdentifiers
    /// plus a touchBar:makeItemForIdentifier: delegate), hand it here, and
    /// tapping the item slides that bar down over the main one. The property
    /// is declared nonnull, so null is ignored.
    public void setPopover(NSTouchBar bar) {
        if (bar == null) return;
        ensureInit();
        if (!responds("setPopoverTouchBar:")) return;
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setPopoverTouchBar:"), bar.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setPopoverTouchBar: failed", t);
        }
    }

    /// showPopover: — replace the main NSTouchBar with this item's popover
    /// bar. No effect while the item is not visible (guarded).
    public void showPopover() {
        showPopover((MemorySegment) null);
    }
    /// showPopover: with an explicit sender (nullable id; shapes VOID,ID in the vocabulary).
    public void showPopover(MemorySegment sender) {
        ensureInit();
        if (!responds("showPopover:")) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("showPopover:"),
                (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    /// Typed sender overload.
    public void showPopover(NSObject sender) {
        showPopover(sender == null ? null : sender.peer());
    }

    /// dismissPopover: — order out the popover bar and restore the previously
    /// visible main bar (guarded).
    public void dismissPopover() {
        dismissPopover((MemorySegment) null);
    }
    /// dismissPopover: with an explicit sender (nullable id; shapes VOID,ID in the vocabulary).
    public void dismissPopover(MemorySegment sender) {
        ensureInit();
        if (!responds("dismissPopover:")) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("dismissPopover:"),
                (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    /// Typed sender overload.
    public void dismissPopover(NSObject sender) {
        dismissPopover(sender == null ? null : sender.peer());
    }

    // ---- completeness: remaining header API in registered shapes (all guarded) ----
    /// customizationLabel — string shown during Touch Bar customization.
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary.
    public String customizationLabel() {
        ensureInit();
        if (!responds("customizationLabel")) return null;
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("customizationLabel"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("customizationLabel failed", t);
        }
    }
    /// setCustomizationLabel:.
    public void setCustomizationLabel(String label) {
        ensureInit();
        if (!responds("setCustomizationLabel:")) return;
        try {
            MemorySegment s = label == null ? MemorySegment.NULL : ObjC.nsstring(label);
            handles.hVoidId().invokeExact(peer, ObjC.sel("setCustomizationLabel:"), s);
        } catch (Throwable t) {
            throw new RuntimeException("setCustomizationLabel: failed", t);
        }
    }

    /// collapsedRepresentation — view shown in the hosted bar (or nil for the default button).
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary.
    public NSView collapsedRepresentation() {
        ensureInit();
        if (!responds("collapsedRepresentation")) return null;
        try {
            MemorySegment v = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("collapsedRepresentation"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("collapsedRepresentation failed", t);
        }
    }
    /// setCollapsedRepresentation: — nil restores the default button.
    public void setCollapsedRepresentation(NSView view) {
        ensureInit();
        if (!responds("setCollapsedRepresentation:")) return;
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setCollapsedRepresentation:"),
                    (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setCollapsedRepresentation: failed", t);
        }
    }

    /// collapsedRepresentationImage — icon on the default collapsed button (or nil).
    public NSImage collapsedRepresentationImage() {
        ensureInit();
        if (!responds("collapsedRepresentationImage")) return null;
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("collapsedRepresentationImage"));
            return NSImage.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("collapsedRepresentationImage failed", t);
        }
    }
    /// setCollapsedRepresentationImage:.
    public void setCollapsedRepresentationImage(NSImage image) {
        ensureInit();
        if (!responds("setCollapsedRepresentationImage:")) return;
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setCollapsedRepresentationImage:"),
                    (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setCollapsedRepresentationImage: failed", t);
        }
    }

    /// collapsedRepresentationLabel — text on the default collapsed button.
    public String collapsedRepresentationLabel() {
        ensureInit();
        if (!responds("collapsedRepresentationLabel")) return null;
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("collapsedRepresentationLabel"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("collapsedRepresentationLabel failed", t);
        }
    }
    /// setCollapsedRepresentationLabel:.
    public void setCollapsedRepresentationLabel(String label) {
        ensureInit();
        if (!responds("setCollapsedRepresentationLabel:")) return;
        try {
            MemorySegment s = label == null ? MemorySegment.NULL : ObjC.nsstring(label);
            handles.hVoidId().invokeExact(peer, ObjC.sel("setCollapsedRepresentationLabel:"), s);
        } catch (Throwable t) {
            throw new RuntimeException("setCollapsedRepresentationLabel: failed", t);
        }
    }

    /// pressAndHoldTouchBar — bar shown while the finger holds the collapsed representation (or nil).
    public NSTouchBar pressAndHoldTouchBar() {
        ensureInit();
        if (!responds("pressAndHoldTouchBar")) return null;
        try {
            MemorySegment b = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("pressAndHoldTouchBar"));
            return NSTouchBar.wrap(b);
        } catch (Throwable t) {
            throw new RuntimeException("pressAndHoldTouchBar failed", t);
        }
    }
    /// setPressAndHoldTouchBar: — nil clears.
    public void setPressAndHoldTouchBar(NSTouchBar bar) {
        ensureInit();
        if (!responds("setPressAndHoldTouchBar:")) return;
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setPressAndHoldTouchBar:"),
                    (MemorySegment) (bar == null ? MemorySegment.NULL : bar.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setPressAndHoldTouchBar: failed", t);
        }
    }

    /// showsCloseButton — whether the popover shows an automatic close button.
    /// Shapes (BOOL ()) / (VOID,BOOL) need no handle (ObjC.msgSend helpers).
    public boolean showsCloseButton() {
        ensureInit();
        if (!responds("showsCloseButton")) return false;
        return ObjC.msgSendBool(peer, ObjC.sel("showsCloseButton"));
    }
    /// setShowsCloseButton:.
    public void setShowsCloseButton(boolean flag) {
        ensureInit();
        if (!responds("setShowsCloseButton:")) return;
        ObjC.msgSendVoidBool(peer, ObjC.sel("setShowsCloseButton:"), flag);
    }

    /// makeStandardActivatePopoverGestureRecognizer — wired recognizer that sends showPopover:.
    /// Shape (ID ()) is in the vocabulary; guarded (API_UNAVAILABLE(ios) in the header).
    public NSGestureRecognizer makeStandardActivatePopoverGestureRecognizer() {
        ensureInit();
        if (!responds("makeStandardActivatePopoverGestureRecognizer")) return null;
        try {
            MemorySegment g = (MemorySegment) handles.hId().invokeExact(peer,
                    ObjC.sel("makeStandardActivatePopoverGestureRecognizer"));
            return NSGestureRecognizer.wrap(g);
        } catch (Throwable t) {
            throw new RuntimeException("makeStandardActivatePopoverGestureRecognizer failed", t);
        }
    }
}
