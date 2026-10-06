package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTouchBar — minimal wrap over AppKit NSTouchBar.
/// Thin 1:1, stateless: every method maps to one objc_msgSend.
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSTouchBar.h
/// Coverage: COMPLETE after this batch — customizationIdentifier/customizationAllowedItemIdentifiers/
/// customizationRequiredItemIdentifiers/defaultItemIdentifiers/itemIdentifiers (readonly)/
/// principalItemIdentifier/escapeKeyReplacementItemIdentifier/templateItems/delegate/itemForIdentifier:/
/// visible/automaticCustomizeTouchBarMenuItemEnabled (class). All shapes are the registered
/// (ID ())/(VOID,ID)/(BOOL ())/(VOID,BOOL) pair (grep Sig.java: of(Ret.ID)/of(Ret.VOID, Arg.ID)/...).
/// OMITTED: NSTouchBarDelegate (touchBar:makeItemForIdentifier:) and NSTouchBarProvider/makeTouchBar —
/// delegate/provider callbacks need upcall delegate-proxy machinery (cf. NSToolbarDelegate);
/// initWithCoder: (NSCoding archiving) is out of scope for a stateless wrapper.
public final class NSTouchBar extends NSObject {

    private record Handles(MethodHandle hId, MethodHandle hVoidId, MethodHandle hTouchBarMakeItem) {}
    private static volatile Handles handles;

    private NSTouchBar(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTouchBar wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTouchBar(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.VOID, Arg.ID)), ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)));
    }

    /// alloc + init — empty touch bar.
    public static NSTouchBar create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSTouchBar"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p == null || p.address() == 0) throw new IllegalStateException("NSTouchBar alloc/init returned nil");
        return new NSTouchBar(p);
    }

    /// setDelegate: — object that provides items (id).
    public void setDelegate(MemorySegment delegate) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setDelegate:"), (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate));
        } catch (Throwable t) {
            throw new RuntimeException("setDelegate: failed", t);
        }
    }

    /// setDelegate: typed overload.
    public void setDelegate(NSObject delegate) {
        setDelegate(delegate == null ? MemorySegment.NULL : delegate.peer());
    }

    /// delegate — raw id.
    public MemorySegment delegate() {
        ensureInit();
        try {
            return (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("delegate"));
        } catch (Throwable t) {
            throw new RuntimeException("delegate failed", t);
        }
    }

    /// setCustomizationIdentifier: — NSString identifier.
    public void setCustomizationIdentifier(String identifier) {
        ensureInit();
        MemorySegment s = identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier);
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setCustomizationIdentifier:"), s);
        } catch (Throwable t) {
            throw new RuntimeException("setCustomizationIdentifier: failed", t);
        }
    }

    /// customizationIdentifier — string or null.
    public String customizationIdentifier() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("customizationIdentifier"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("customizationIdentifier failed", t);
        }
    }

    /// itemIdentifiers — resolved identifiers (readonly; matches defaultItemIdentifiers until customized).
    /// Shape (ID ()) is in the vocabulary.
    public NSArray itemIdentifiers() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("itemIdentifiers"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("itemIdentifiers failed", t);
        }
    }

    /// defaultItemIdentifiers — identifiers fed through itemForIdentifier:/delegate.
    /// Shape (ID ()) is in the vocabulary.
    public NSArray defaultItemIdentifiers() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("defaultItemIdentifiers"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("defaultItemIdentifiers failed", t);
        }
    }

    /// setDefaultItemIdentifiers: — NSArray of identifiers.
    public void setDefaultItemIdentifiers(NSArray identifiers) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setDefaultItemIdentifiers:"), (MemorySegment) (identifiers == null ? MemorySegment.NULL : identifiers.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setDefaultItemIdentifiers: failed", t);
        }
    }

    /// setCustomizationAllowedItemIdentifiers:
    public void setCustomizationAllowedItemIdentifiers(NSArray ids) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setCustomizationAllowedItemIdentifiers:"), (MemorySegment) (ids == null ? MemorySegment.NULL : ids.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setCustomizationAllowedItemIdentifiers: failed", t);
        }
    }

    /// customizationAllowedItemIdentifiers
    public NSArray customizationAllowedItemIdentifiers() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("customizationAllowedItemIdentifiers"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("customizationAllowedItemIdentifiers failed", t);
        }
    }

    // ---- completeness: remaining header API in registered shapes ----
    /// customizationRequiredItemIdentifiers — identifiers the user cannot remove.
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary.
    public NSArray customizationRequiredItemIdentifiers() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("customizationRequiredItemIdentifiers"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("customizationRequiredItemIdentifiers failed", t);
        }
    }
    /// setCustomizationRequiredItemIdentifiers:.
    public void setCustomizationRequiredItemIdentifiers(NSArray ids) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setCustomizationRequiredItemIdentifiers:"),
                    (MemorySegment) (ids == null ? MemorySegment.NULL : ids.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setCustomizationRequiredItemIdentifiers: failed", t);
        }
    }

    /// principalItemIdentifier — centered item identifier (or nil).
    public String principalItemIdentifier() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("principalItemIdentifier"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("principalItemIdentifier failed", t);
        }
    }
    /// setPrincipalItemIdentifier:.
    public void setPrincipalItemIdentifier(String identifier) {
        ensureInit();
        MemorySegment s = identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier);
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setPrincipalItemIdentifier:"), s);
        } catch (Throwable t) {
            throw new RuntimeException("setPrincipalItemIdentifier: failed", t);
        }
    }

    /// escapeKeyReplacementItemIdentifier — item replacing the system escape key (or nil).
    public String escapeKeyReplacementItemIdentifier() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("escapeKeyReplacementItemIdentifier"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("escapeKeyReplacementItemIdentifier failed", t);
        }
    }
    /// setEscapeKeyReplacementItemIdentifier:.
    public void setEscapeKeyReplacementItemIdentifier(String identifier) {
        ensureInit();
        MemorySegment s = identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier);
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setEscapeKeyReplacementItemIdentifier:"), s);
        } catch (Throwable t) {
            throw new RuntimeException("setEscapeKeyReplacementItemIdentifier: failed", t);
        }
    }

    /// templateItems — first-step resolution set (NSSet of NSTouchBarItem).
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary (NSSet wrapper exists).
    public NSSet templateItems() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("templateItems"));
            return NSSet.wrap(s);
        } catch (Throwable t) {
            throw new RuntimeException("templateItems failed", t);
        }
    }
    /// setTemplateItems:.
    public void setTemplateItems(NSSet items) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setTemplateItems:"),
                    (MemorySegment) (items == null ? MemorySegment.NULL : items.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setTemplateItems: failed", t);
        }
    }

    /// itemForIdentifier: — resolve one instantiated item (or nil).
    /// Shape (ID,ID) is in the vocabulary (grep: of(Ret.ID, Arg.ID)).
    /// Uses the cached hTouchBarMakeItem handle (same ID,ID,ID shape family via invokeExact with 1 arg).
    public NSTouchBarItem itemForIdentifier(String identifier) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("itemForIdentifier:"),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)));
            return NSTouchBarItem.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("itemForIdentifier: failed", t);
        }
    }

    /// isVisible — YES while attached to an eligible provider and displayable (KVO).
    /// Shape (BOOL ()) is in the vocabulary.
    public boolean isVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVisible"));
    }

    /// +automaticCustomizeTouchBarMenuItemEnabled — class convenience for NSApp's flag (10.15+).
    /// Shapes (BOOL ()) on the class / (VOID,BOOL) on the class are in the vocabulary.
    public static boolean isAutomaticCustomizeTouchBarMenuItemEnabled() {
        ensureInit();
        return ObjC.msgSendBool(ObjC.cls("NSTouchBar"), ObjC.sel("isAutomaticCustomizeTouchBarMenuItemEnabled"));
    }
    /// +setAutomaticCustomizeTouchBarMenuItemEnabled:.
    public static void setAutomaticCustomizeTouchBarMenuItemEnabled(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(ObjC.cls("NSTouchBar"), ObjC.sel("setAutomaticCustomizeTouchBarMenuItemEnabled:"), flag);
    }
}
