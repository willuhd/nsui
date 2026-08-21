package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSCollectionViewItem — the prototype/item for an NSCollectionView.
/// Thin, 1:1, stateless wrapper over the native `NSCollectionViewItem`
/// (an NSViewController subclass). Follows the project template: volatile
/// initialized, synchronized ensureInit, ObjC.handle(Sig.of...), invokeExact,
/// static create/wrap.
///
/// Minimal: init, view, representedObject. The item's view is configured
/// by the data source; the collection view clones the prototype for each index.
public final class NSCollectionViewItem extends NSObject {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hInit, MethodHandle hInitNib, MethodHandle hSetRep) {}
    private static volatile Handles handles;

    private NSCollectionViewItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing NSCollectionViewItem peer.
    public static NSCollectionViewItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSCollectionViewItem(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)), ObjC.handle(Sig.of(Ret.VOID, Arg.ID)));
    }

    /// `[[NSCollectionViewItem alloc] init]` — minimal item with no nib.
    public static NSCollectionViewItem create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSCollectionViewItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInit().invokeExact(p, ObjC.sel("init"));
        } catch (Throwable t) {
            throw new RuntimeException("init failed for NSCollectionViewItem", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSCollectionViewItem alloc/init returned nil");
        return new NSCollectionViewItem(p);
    }

    /// `[[NSCollectionViewItem alloc] initWithNibName:bundle:]`
    public static NSCollectionViewItem create(String nibName, MemorySegment bundle) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSCollectionViewItem"), ObjC.sel("alloc"));
        MemorySegment nib = nibName == null ? MemorySegment.NULL : ObjC.nsstring(nibName);
        try {
            MemorySegment b = (bundle == null || bundle.address() == 0) ? MemorySegment.NULL : bundle;
            p = (MemorySegment) handles.hInitNib().invokeExact(p, ObjC.sel("initWithNibName:bundle:"), nib, (MemorySegment) b);
        } catch (Throwable t) {
            throw new RuntimeException("initWithNibName:bundle: failed for NSCollectionViewItem", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSCollectionViewItem alloc/initWithNibName:bundle: returned nil");
        return new NSCollectionViewItem(p);
    }

    // ---------------------------------------------------------------- instance API

    /// [item view] — the item's view (NSView peer or nil).
    public NSView view() {
        try {
            MemorySegment v = (MemorySegment) handles.hInit().invokeExact(peer, ObjC.sel("view"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("view failed", t);
        }
    }

    /// [item setView:]
    public void setView(NSView view) {
        try {
            MemorySegment v = (view == null || view.peer() == null || view.peer().address() == 0) ? MemorySegment.NULL : view.peer();
            handles.hSetRep().invokeExact(peer, ObjC.sel("setView:"), (MemorySegment) v);
        } catch (Throwable t) {
            throw new RuntimeException("setView: failed", t);
        }
    }

    /// [item representedObject] — the model object for this item (id).
    public MemorySegment representedObject() {
        try {
            return (MemorySegment) handles.hInit().invokeExact(peer, ObjC.sel("representedObject"));
        } catch (Throwable t) {
            throw new RuntimeException("representedObject failed", t);
        }
    }

    /// [item setRepresentedObject:]
    public void setRepresentedObject(MemorySegment object) {
        try {
            MemorySegment o = (object == null || object.address() == 0) ? MemorySegment.NULL : object;
            handles.hSetRep().invokeExact(peer, ObjC.sel("setRepresentedObject:"), (MemorySegment) o);
        } catch (Throwable t) {
            throw new RuntimeException("setRepresentedObject: failed", t);
        }
    }

    /// [item isSelected]
    public boolean isSelected() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSelected"));
    }

    /// [item setSelected:]
    public void setSelected(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setSelected:"), flag);
    }

    /// [item highlightState] — NSInteger.
    public long highlightState() {
        return ObjC.msgSendLong(peer, ObjC.sel("highlightState"));
    }

    public void setHighlightState(long state) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setHighlightState:"), state);
    }
}
