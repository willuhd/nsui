package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSToolbarItem — an item within an NSToolbar. Thin, 1:1, stateless wrapper over
/// the native `NSToolbarItem`: each method maps to one `objc_msgSend`
/// selector. Follows the project template: volatile initialized, synchronized
/// ensureInit, ObjC.handle(Sig.of...), invokeExact, static create/wrap.
///
/// Created via `[[NSToolbarItem alloc] initWithItemIdentifier:]`.
public final class NSToolbarItem extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hInitIdentifier, MethodHandle hSetLabel, MethodHandle hSetEnabled, MethodHandle hSetTag, MethodHandle hSetMinSize) {}
    private static volatile Handles handles;

    private NSToolbarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing NSToolbarItem peer.
    public static NSToolbarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSToolbarItem(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE))
        );
    }

    /// `[[NSToolbarItem alloc] initWithItemIdentifier:identifier]` — a new item.
    public static NSToolbarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSToolbarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitIdentifier().invokeExact(p, ObjC.sel("initWithItemIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithItemIdentifier: failed for NSToolbarItem", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSToolbarItem alloc/initWithItemIdentifier: returned nil");
        return new NSToolbarItem(p);
    }

    /// Raw peer variant: initWithItemIdentifier: with id.
    public static NSToolbarItem create(MemorySegment identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSToolbarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitIdentifier().invokeExact(p, ObjC.sel("initWithItemIdentifier:"), (MemorySegment) (identifier == null ? MemorySegment.NULL : identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithItemIdentifier: failed for NSToolbarItem", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSToolbarItem alloc/initWithItemIdentifier: returned nil");
        return new NSToolbarItem(p);
    }

    // ---------------------------------------------------------------- instance API

    /// [item itemIdentifier] — NSString id.
    public String itemIdentifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("itemIdentifier")));
    }

    /// [item label] — NSString.
    public String label() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("label")));
    }

    /// [item setLabel:]
    public void setLabel(String label) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setLabel:"), (MemorySegment) (label == null ? MemorySegment.NULL : ObjC.nsstring(label)));
        } catch (Throwable t) {
            throw new RuntimeException("setLabel: failed", t);
        }
    }

    /// [item paletteLabel]
    public String paletteLabel() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("paletteLabel")));
    }

    /// [item setPaletteLabel:]
    public void setPaletteLabel(String label) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setPaletteLabel:"), (MemorySegment) (label == null ? MemorySegment.NULL : ObjC.nsstring(label)));
        } catch (Throwable t) {
            throw new RuntimeException("setPaletteLabel: failed", t);
        }
    }

    /// [item toolTip]
    public String toolTip() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("toolTip")));
    }

    /// [item setToolTip:]
    public void setToolTip(String tip) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setToolTip:"), (MemorySegment) (tip == null ? MemorySegment.NULL : ObjC.nsstring(tip)));
        } catch (Throwable t) {
            throw new RuntimeException("setToolTip: failed", t);
        }
    }

    /// [item image] — NSImage peer or nil.
    public NSImage image() {
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("image"));
        return NSImage.wrap(p);
    }

    /// [item setImage:]
    public void setImage(NSImage image) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setImage:"), (MemorySegment) ((MemorySegment) (image == null ? MemorySegment.NULL : image.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("setImage: failed", t);
        }
    }

    /// [item view] — NSView peer or nil.
    public NSView view() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("view"));
        return NSView.wrap(v);
    }

    /// [item setView:]
    public void setView(NSView view) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setView:"), (MemorySegment) ((MemorySegment) (view == null ? MemorySegment.NULL : view.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("setView: failed", t);
        }
    }

    /// [item target] — action target.
    public MemorySegment target() {
        return ObjC.msgSendId(peer, ObjC.sel("target"));
    }

    /// [item setTarget:] — action target id.
    public void setTarget(MemorySegment target) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setTarget:"), (MemorySegment) ((MemorySegment) (target == null ? MemorySegment.NULL : target)));
        } catch (Throwable t) {
            throw new RuntimeException("setTarget: failed", t);
        }
    }

    /// [item action] — selector.
    public MemorySegment action() {
        return ObjC.msgSendId(peer, ObjC.sel("action"));
    }

    /// [item setAction:] — selector (SEL).
    public void setAction(String actionSelector) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setAction:"), (MemorySegment) (actionSelector == null ? MemorySegment.NULL : ObjC.sel(actionSelector)));
        } catch (Throwable t) {
            throw new RuntimeException("setAction: failed", t);
        }
    }

    /// [item setAction:] with raw SEL.
    public void setAction(MemorySegment action) {
        try {
            handles.hSetLabel().invokeExact(peer, ObjC.sel("setAction:"), (MemorySegment) (action == null ? MemorySegment.NULL : action));
        } catch (Throwable t) {
            throw new RuntimeException("setAction: failed", t);
        }
    }

    /// [item isEnabled].
    public boolean isEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEnabled"));
    }

    /// [item setEnabled:]
    public void setEnabled(boolean flag) {
        try {
            handles.hSetEnabled().invokeExact(peer, ObjC.sel("setEnabled:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setEnabled: failed", t);
        }
    }

    /// [item tag].
    public long tag() {
        return ObjC.msgSendLong(peer, ObjC.sel("tag"));
    }

    /// [item setTag:]
    public void setTag(long tag) {
        try {
            handles.hSetTag().invokeExact(peer, ObjC.sel("setTag:"), tag);
        } catch (Throwable t) {
            throw new RuntimeException("setTag: failed", t);
        }
    }

    /// [item minSize] — NSSize.
    public NSSize minSize() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.SIZE));
            MemorySegment s = (MemorySegment) h.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("minSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("minSize failed", t);
        }
    }

    /// [item setMinSize:]
    public void setMinSize(NSSize size) {
        try {
            handles.hSetMinSize().invokeExact(peer, ObjC.sel("setMinSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMinSize: failed", t);
        }
    }

    /// [item maxSize]
    public NSSize maxSize() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.SIZE));
            MemorySegment s = (MemorySegment) h.invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("maxSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("maxSize failed", t);
        }
    }

    /// [item setMaxSize:]
    public void setMaxSize(NSSize size) {
        try {
            handles.hSetMinSize().invokeExact(peer, ObjC.sel("setMaxSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMaxSize: failed", t);
        }
    }

    /// [item visibilityPriority] — NSToolbarItemVisibilityPriority (NSInteger).
    public long visibilityPriority() {
        return ObjC.msgSendLong(peer, ObjC.sel("visibilityPriority"));
    }

    public void setVisibilityPriority(long p) {
        try {
            handles.hSetTag().invokeExact(peer, ObjC.sel("setVisibilityPriority:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("setVisibilityPriority: failed", t);
        }
    }
}
