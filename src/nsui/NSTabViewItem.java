package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTabViewItem — a single tab within an `NSTabView`. Thin, 1:1, stateless
/// wrapper over a native `NSTabViewItem`. Unlike the other small widgets it has no
/// frame-based init; the documented pattern is `[[NSTabViewItem alloc] initWithIdentifier:]` followed by `setLabel:`.
///
/// Only the label and content-view surface is wrapped here — enough to build a
/// two-tab demo. `create` builds the item with the label as its identifier, then
/// applies `setLabel:` so `label` round-trips.
public final class NSTabViewItem extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitIdentifier; // (id, SEL, id) -> id    [initWithIdentifier:]
    private static MethodHandle hSetLabel;       // (id, SEL, id) -> void  [setLabel:]
    private static MethodHandle hLabel;          // (id, SEL) -> id        [label or label(defaults)]
    private static MethodHandle hSetView;        // (id, SEL, id) -> void  [setView:]
    private static MethodHandle hGetId;          // (id, SEL) -> id
    private static MethodHandle hSetId;          // (id, SEL, id) -> void

    private NSTabViewItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTabViewItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTabViewItem(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitIdentifier = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hSetLabel = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hLabel = ObjC.handle(Sig.of(Ret.ID));
        hSetView = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hSetId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /// `[[NSTabViewItem alloc] initWithIdentifier:label]` + `setLabel:` — a new tab item.
    public static NSTabViewItem create(String label) {
        ensureInit();
        MemorySegment p = ObjC.msgSendIdId(ObjC.cls("NSTabViewItem"), ObjC.sel("alloc"), MemorySegment.NULL);
        try {
            p = (MemorySegment) hInitIdentifier.invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(label));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSTabViewItem", t);
        }
        if (p.address() == 0) {
            throw new IllegalStateException("NSTabViewItem alloc/initWithIdentifier: returned nil");
        }
        NSTabViewItem item = new NSTabViewItem(p);
        item.setLabel(label);
        return item;
    }

    // ---------------------------------------------------------------- instance API

    /// [item setLabel:] — the text shown on the tab.
    public void setLabel(String label) {
        try {
            hSetLabel.invokeExact(peer, ObjC.sel("setLabel:"), ObjC.nsstring(label));
        } catch (Throwable t) {
            throw new RuntimeException("setLabel: failed", t);
        }
    }

    /// [item label] — the tab's current label text.
    public String label() {
        try {
            return ObjC.toString((MemorySegment) hLabel.invokeExact(peer, ObjC.sel("label")));
        } catch (Throwable t) {
            throw new RuntimeException("label failed", t);
        }
    }

    /// [item setView:] — the content view shown while this tab is selected.
    public void setView(NSView view) {
        try {
            hSetView.invokeExact(peer, ObjC.sel("setView:"), view.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setView: failed", t);
        }
    }

    /// [item view] — the content view (may be nil).
    public NSView view() {
        try {
            MemorySegment v = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("view"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("view failed", t);
        }
    }

    /// [item identifier] — the identifier (NSString wrapping if it was a string).
    public String identifier() {
        try {
            MemorySegment ident = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("identifier"));
            if (ident == null || ident.address() == 0) return null;
            // if identifier is an NSString, toString will decode; otherwise return description
            String s = ObjC.toString(ObjC.msgSendId(ident, ObjC.sel("description")));
            // Try NSString path: if ident responds to UTF8String, toString already works via description, but attempt direct
            // Prefer direct NSString conversion when possible (when identifier is NSString, description == string)
            MemorySegment maybeStr = ObjC.msgSendId(ident, ObjC.sel("description"));
            // fallback to direct UTF8 if it's an NSString (UTF8String will return non-null)
            // But we already have s via description; for NSString it equals the string.
            return s;
        } catch (Throwable t) {
            throw new RuntimeException("identifier failed", t);
        }
    }

    /// [item setIdentifier:] — set identifier to a string (NSString).
    public void setIdentifier(String identifier) {
        try {
            hSetId.invokeExact(peer, ObjC.sel("setIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("setIdentifier: failed", t);
        }
    }

    /// [item initialFirstResponder].
    public NSView initialFirstResponder() {
        try {
            MemorySegment v = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("initialFirstResponder"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("initialFirstResponder failed", t);
        }
    }

    /// [item setInitialFirstResponder:].
    public void setInitialFirstResponder(NSView view) {
        try {
            hSetId.invokeExact(peer, ObjC.sel("setInitialFirstResponder:"), (MemorySegment) ((MemorySegment) (view == null ? MemorySegment.NULL : view.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("setInitialFirstResponder: failed", t);
        }
    }

    /// [item color] — NSColor (may be nil).
    public NSColor color() {
        try {
            MemorySegment c = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("color"));
            return NSColor.wrap(c);
        } catch (Throwable t) {
            throw new RuntimeException("color failed", t);
        }
    }

    /// [item setColor:].
    public void setColor(NSColor color) {
        try {
            hSetId.invokeExact(peer, ObjC.sel("setColor:"), (MemorySegment) ((MemorySegment) (color == null ? MemorySegment.NULL : color.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("setColor: failed", t);
        }
    }

    /// [item toolTip].
    public String toolTip() {
        try {
            MemorySegment s = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("toolTip"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("toolTip failed", t);
        }
    }

    /// [item setToolTip:].
    public void setToolTip(String tip) {
        try {
            hSetId.invokeExact(peer, ObjC.sel("setToolTip:"), ObjC.nsstring(tip));
        } catch (Throwable t) {
            throw new RuntimeException("setToolTip: failed", t);
        }
    }

    /// [item tabState] — NSTabState.
    public long tabState() {
        return ObjC.msgSendLong(peer, ObjC.sel("tabState"));
    }

    /// [item tabView] — parent NSTabView (may be nil).
    public NSTabView tabView() {
        try {
            MemorySegment p = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("tabView"));
            return NSTabView.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("tabView failed", t);
        }
    }
}
