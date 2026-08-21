package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTouchBar — minimal wrap over AppKit NSTouchBar.
/// Thin 1:1, stateless: every method maps to one objc_msgSend.
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

        private static synchronized void ensureInit() {
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

    /// itemIdentifiers — NSArray of NSString identifiers.
    public NSArray itemIdentifiers() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("defaultItemIdentifiers"));
            // fallback: try itemIdentifiers if default not set? Prefer generic.
            if (arr == null || arr.address() == 0) {
                // try itemIdentifiers selector
                try {
                    arr = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("itemIdentifiers"));
                } catch (Throwable ignored) {}
            }
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("itemIdentifiers failed", t);
        }
    }

    /// defaultItemIdentifiers — alias.
    public NSArray defaultItemIdentifiers() {
        return itemIdentifiers();
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
}
