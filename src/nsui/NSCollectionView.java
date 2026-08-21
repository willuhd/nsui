package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSCollectionView — a view that presents an ordered collection of items.
/// Thin, 1:1, stateless wrapper over the native `NSCollectionView`: each
/// method maps to one `objc_msgSend` selector. Follows the project template:
/// volatile initialized, synchronized ensureInit, ObjC.handle(Sig.of...),
/// invokeExact, static create/wrap.
///
/// Created via `[[NSCollectionView alloc] initWithFrame:]` and typically
/// wired via `setDataSource:` / `reloadData` and an item prototype.
public final class NSCollectionView extends NSView {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hInitFrame, MethodHandle hSetDataSource, MethodHandle hReloadData, MethodHandle hGetId, MethodHandle hSetSelectable) {}
    private static volatile Handles handles;

    private NSCollectionView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing NSCollectionView peer.
    public static NSCollectionView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSCollectionView(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL))
        );
    }

    /// `[[NSCollectionView alloc] initWithFrame:frame]` — a new collection view.
    public static NSCollectionView create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSCollectionView"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitFrame().invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSCollectionView", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSCollectionView alloc/initWithFrame: returned nil");
        return new NSCollectionView(p);
    }

    // ---------------------------------------------------------------- instance API

    /// [view setDataSource:] — object answering item counts / views.
    public void setDataSource(MemorySegment dataSource) {
        try {
            MemorySegment arg = (dataSource == null || dataSource.address() == 0) ? MemorySegment.NULL : dataSource;
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setDataSource:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setDataSource: failed", t);
        }
    }

    /// [view dataSource] — id or nil.
    public MemorySegment dataSource() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("dataSource"));
        } catch (Throwable t) {
            throw new RuntimeException("dataSource failed", t);
        }
    }

    /// [view setDelegate:]
    public void setDelegate(MemorySegment delegate) {
        try {
            MemorySegment arg = (delegate == null || delegate.address() == 0) ? MemorySegment.NULL : delegate;
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setDelegate:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setDelegate: failed", t);
        }
    }

    /// [view delegate]
    public MemorySegment delegate() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("delegate"));
        } catch (Throwable t) {
            throw new RuntimeException("delegate failed", t);
        }
    }

    /// [view reloadData] — re-query dataSource.
    public void reloadData() {
        try {
            handles.hReloadData().invokeExact(peer, ObjC.sel("reloadData"));
        } catch (Throwable t) {
            throw new RuntimeException("reloadData failed", t);
        }
    }

    /// [view itemPrototype] — NSCollectionViewItem peer or nil.
    public MemorySegment itemPrototype() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("itemPrototype"));
        } catch (Throwable t) {
            throw new RuntimeException("itemPrototype failed", t);
        }
    }

    /// [view setItemPrototype:]
    public void setItemPrototype(MemorySegment prototype) {
        try {
            MemorySegment arg = (prototype == null || prototype.address() == 0) ? MemorySegment.NULL : prototype;
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setItemPrototype:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setItemPrototype: failed", t);
        }
    }

    /// [view setItemPrototype:] typed variant.
    public void setItemPrototype(NSCollectionViewItem prototype) {
        MemorySegment arg = (prototype == null || prototype.peer() == null || prototype.peer().address() == 0) ? MemorySegment.NULL : prototype.peer();
        setItemPrototype((MemorySegment) arg);
    }

    /// [view isSelectable].
    public boolean isSelectable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSelectable"));
    }

    /// [view setSelectable:]
    public void setSelectable(boolean flag) {
        try {
            handles.hSetSelectable().invokeExact(peer, ObjC.sel("setSelectable:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setSelectable: failed", t);
        }
    }

    /// [view allowsMultipleSelection]
    public boolean allowsMultipleSelection() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsMultipleSelection"));
    }

    /// [view setAllowsMultipleSelection:]
    public void setAllowsMultipleSelection(boolean flag) {
        try {
            handles.hSetSelectable().invokeExact(peer, ObjC.sel("setAllowsMultipleSelection:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsMultipleSelection: failed", t);
        }
    }

    /// [view selectionIndexes] — NSIndexSet peer.
    public MemorySegment selectionIndexes() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("selectionIndexes"));
        } catch (Throwable t) {
            throw new RuntimeException("selectionIndexes failed", t);
        }
    }

    /// [view setSelectionIndexes:]
    public void setSelectionIndexes(MemorySegment indexes) {
        try {
            MemorySegment arg = (indexes == null || indexes.address() == 0) ? MemorySegment.NULL : indexes;
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setSelectionIndexes:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setSelectionIndexes: failed", t);
        }
    }

    /// [view content] — NSArray of represented objects.
    public MemorySegment content() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("content"));
        } catch (Throwable t) {
            throw new RuntimeException("content failed", t);
        }
    }

    /// [view setContent:]
    public void setContent(MemorySegment content) {
        try {
            MemorySegment arg = (content == null || content.address() == 0) ? MemorySegment.NULL : content;
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setContent:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setContent: failed", t);
        }
    }
}
