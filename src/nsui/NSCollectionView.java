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
            private record Handles(MethodHandle hSetDataSource, MethodHandle hReloadData, MethodHandle hGetId, MethodHandle hSetSelectable, MethodHandle hVoidIdId, MethodHandle hIdId, MethodHandle hIdIdId, MethodHandle hVoidIdInt, MethodHandle hVoidIntInt, MethodHandle hIdInt, MethodHandle hIntInt, MethodHandle hIdPoint, MethodHandle hVoidIdIdId, MethodHandle hIdIdIdId) {}
    private static volatile Handles handles;

    private NSCollectionView(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing NSCollectionView peer.
    public static NSCollectionView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSCollectionView(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID))
        );
    }

    /// `[[NSCollectionView alloc] initWithFrame:frame]` — a new collection view.
        public static NSCollectionView create(NSRect frame) {
        ensureInit();
        return new NSCollectionView(ObjC.newView("NSCollectionView", frame));
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

    // ---- batch: Tables — model, layout, selection and item-lookup coverage (all shapes already in Sig.VOCABULARY) ----
    // Omitted here (documented, reported to coordinator):
    // - frameForItemAtIndex: / frameForItemAtIndex:withNumberOfItems: need Ret.RECT with INT args — no such entry.
    // - draggingImageForItemsAtIndexPaths:withEvent:offset: / draggingImageForItemsAtIndexes:… — out-pointer (NSPointPointer) — no such entry.
    // - setDraggingSourceOperationMask:forLocal: needs Ret.VOID with (INT, BOOL) — no such entry.
    // - performBatchUpdates:completionHandler: — block args, need upcall machinery.
    // - All NSCollectionViewDataSource/Delegate/Prefetching protocol methods — need upcall machinery.
    // - NSIndexPath / NSSet additions — different classes, outside this batch's files.
    // - Deprecated single-section/grid-layout surface (newItemForRepresentedObject:, maxNumberOfRows, minItemSize, …) — skipped by policy.
    // - userInterfaceLayoutDirection — inherited from NSView (identical selectors), not redeclared.

    /// [view prefetchDataSource] — id or nil.
    public MemorySegment prefetchDataSource() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("prefetchDataSource"));
        } catch (Throwable t) {
            throw new RuntimeException("prefetchDataSource failed", t);
        }
    }

    /// [view setPrefetchDataSource:]
    public void setPrefetchDataSource(MemorySegment source) {
        try {
            MemorySegment arg = (source == null || source.address() == 0) ? MemorySegment.NULL : source;
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setPrefetchDataSource:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setPrefetchDataSource: failed", t);
        }
    }

    /// [view backgroundView] — NSView or nil.
    public NSView backgroundView() {
        try {
            return NSView.wrap((MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("backgroundView")));
        } catch (Throwable t) {
            throw new RuntimeException("backgroundView failed", t);
        }
    }

    /// [view setBackgroundView:]
    public void setBackgroundView(NSView view) {
        try {
            MemorySegment arg = (view == null || view.peer() == null || view.peer().address() == 0) ? MemorySegment.NULL : view.peer();
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setBackgroundView:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setBackgroundView: failed", t);
        }
    }

    /// [view backgroundViewScrollsWithContent]
    public boolean backgroundViewScrollsWithContent() {
        return ObjC.msgSendBool(peer, ObjC.sel("backgroundViewScrollsWithContent"));
    }

    /// [view setBackgroundViewScrollsWithContent:]
    public void setBackgroundViewScrollsWithContent(boolean flag) {
        try {
            handles.hSetSelectable().invokeExact(peer, ObjC.sel("setBackgroundViewScrollsWithContent:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setBackgroundViewScrollsWithContent: failed", t);
        }
    }

    /// [view collectionViewLayout] — NSCollectionViewLayout peer or nil.
    public MemorySegment collectionViewLayout() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("collectionViewLayout"));
        } catch (Throwable t) {
            throw new RuntimeException("collectionViewLayout failed", t);
        }
    }

    /// [view setCollectionViewLayout:]
    public void setCollectionViewLayout(MemorySegment layout) {
        try {
            MemorySegment arg = (layout == null || layout.address() == 0) ? MemorySegment.NULL : layout;
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setCollectionViewLayout:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setCollectionViewLayout: failed", t);
        }
    }

    /// [view layoutAttributesForItemAtIndexPath:] — layout attributes peer or nil.
    public MemorySegment layoutAttributesForItemAtIndexPath(MemorySegment indexPath) {
        try {
            return (MemorySegment) handles.hIdId().invokeExact(peer, ObjC.sel("layoutAttributesForItemAtIndexPath:"), (MemorySegment) (indexPath == null ? MemorySegment.NULL : indexPath));
        } catch (Throwable t) {
            throw new RuntimeException("layoutAttributesForItemAtIndexPath: failed", t);
        }
    }

    /// [view layoutAttributesForSupplementaryElementOfKind:atIndexPath:]
    public MemorySegment layoutAttributesForSupplementaryElementOfKind(String kind, MemorySegment indexPath) {
        try {
            return (MemorySegment) handles.hIdIdId().invokeExact(peer, ObjC.sel("layoutAttributesForSupplementaryElementOfKind:atIndexPath:"),
                    (MemorySegment) (kind == null ? MemorySegment.NULL : ObjC.nsstring(kind)),
                    (MemorySegment) (indexPath == null ? MemorySegment.NULL : indexPath));
        } catch (Throwable t) {
            throw new RuntimeException("layoutAttributesForSupplementaryElementOfKind:atIndexPath: failed", t);
        }
    }

    /// [view backgroundColors] — NSArray of NSColor peers (id).
    public MemorySegment backgroundColors() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("backgroundColors"));
        } catch (Throwable t) {
            throw new RuntimeException("backgroundColors failed", t);
        }
    }

    /// [view setBackgroundColors:] — nil resets to the default.
    public void setBackgroundColors(MemorySegment colors) {
        try {
            MemorySegment arg = (colors == null || colors.address() == 0) ? MemorySegment.NULL : colors;
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setBackgroundColors:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setBackgroundColors: failed", t);
        }
    }

    /// [view numberOfSections]
    public long numberOfSections() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfSections"));
    }

    /// [view numberOfItemsInSection:]
    public long numberOfItemsInSection(long section) {
        try {
            return (long) handles.hIntInt().invokeExact(peer, ObjC.sel("numberOfItemsInSection:"), section);
        } catch (Throwable t) {
            throw new RuntimeException("numberOfItemsInSection: failed", t);
        }
    }

    /// [view isFirstResponder] — whether the collection view is its window's first responder.
    public boolean isFirstResponder() {
        return ObjC.msgSendBool(peer, ObjC.sel("isFirstResponder"));
    }

    /// [view allowsEmptySelection]
    public boolean allowsEmptySelection() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsEmptySelection"));
    }

    /// [view setAllowsEmptySelection:]
    public void setAllowsEmptySelection(boolean flag) {
        try {
            handles.hSetSelectable().invokeExact(peer, ObjC.sel("setAllowsEmptySelection:"), flag);
        } catch (Throwable t) {
            throw new RuntimeException("setAllowsEmptySelection: failed", t);
        }
    }

    /// [view selectionIndexPaths] — NSSet of NSIndexPath peers (id).
    public MemorySegment selectionIndexPaths() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("selectionIndexPaths"));
        } catch (Throwable t) {
            throw new RuntimeException("selectionIndexPaths failed", t);
        }
    }

    /// [view setSelectionIndexPaths:]
    public void setSelectionIndexPaths(MemorySegment indexPaths) {
        try {
            MemorySegment arg = (indexPaths == null || indexPaths.address() == 0) ? MemorySegment.NULL : indexPaths;
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("setSelectionIndexPaths:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("setSelectionIndexPaths: failed", t);
        }
    }

    /// [view selectItemsAtIndexPaths:scrollPosition:] — no delegate callbacks.
    public void selectItemsAtIndexPaths(MemorySegment indexPaths, long scrollPosition) {
        try {
            handles.hVoidIdInt().invokeExact(peer, ObjC.sel("selectItemsAtIndexPaths:scrollPosition:"), (MemorySegment) (indexPaths == null ? MemorySegment.NULL : indexPaths), scrollPosition);
        } catch (Throwable t) {
            throw new RuntimeException("selectItemsAtIndexPaths:scrollPosition: failed", t);
        }
    }

    /// [view deselectItemsAtIndexPaths:] — no delegate callbacks.
    public void deselectItemsAtIndexPaths(MemorySegment indexPaths) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("deselectItemsAtIndexPaths:"), (MemorySegment) (indexPaths == null ? MemorySegment.NULL : indexPaths));
        } catch (Throwable t) {
            throw new RuntimeException("deselectItemsAtIndexPaths: failed", t);
        }
    }

    /// [view selectAll:]
    public void selectAll(MemorySegment sender) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("selectAll:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
        } catch (Throwable t) {
            throw new RuntimeException("selectAll: failed", t);
        }
    }

    /// [view deselectAll:]
    public void deselectAll(MemorySegment sender) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("deselectAll:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
        } catch (Throwable t) {
            throw new RuntimeException("deselectAll: failed", t);
        }
    }

    /// [view registerClass:forItemWithIdentifier:] — nil class unregisters (falls back to nib/name lookup).
    public void registerClassForItem(MemorySegment itemClass, String identifier) {
        try {
            handles.hVoidIdId().invokeExact(peer, ObjC.sel("registerClass:forItemWithIdentifier:"),
                    (MemorySegment) (itemClass == null ? MemorySegment.NULL : itemClass),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)));
        } catch (Throwable t) {
            throw new RuntimeException("registerClass:forItemWithIdentifier: failed", t);
        }
    }

    /// [view registerNib:forItemWithIdentifier:] — nil nib unregisters.
    public void registerNibForItem(MemorySegment nib, String identifier) {
        try {
            handles.hVoidIdId().invokeExact(peer, ObjC.sel("registerNib:forItemWithIdentifier:"),
                    (MemorySegment) (nib == null ? MemorySegment.NULL : nib),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)));
        } catch (Throwable t) {
            throw new RuntimeException("registerNib:forItemWithIdentifier: failed", t);
        }
    }

    /// [view registerClass:forSupplementaryViewOfKind:withIdentifier:]
    public void registerClassForSupplementaryView(MemorySegment viewClass, String kind, String identifier) {
        try {
            handles.hVoidIdIdId().invokeExact(peer, ObjC.sel("registerClass:forSupplementaryViewOfKind:withIdentifier:"),
                    (MemorySegment) (viewClass == null ? MemorySegment.NULL : viewClass),
                    (MemorySegment) (kind == null ? MemorySegment.NULL : ObjC.nsstring(kind)),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)));
        } catch (Throwable t) {
            throw new RuntimeException("registerClass:forSupplementaryViewOfKind:withIdentifier: failed", t);
        }
    }

    /// [view registerNib:forSupplementaryViewOfKind:withIdentifier:]
    public void registerNibForSupplementaryView(MemorySegment nib, String kind, String identifier) {
        try {
            handles.hVoidIdIdId().invokeExact(peer, ObjC.sel("registerNib:forSupplementaryViewOfKind:withIdentifier:"),
                    (MemorySegment) (nib == null ? MemorySegment.NULL : nib),
                    (MemorySegment) (kind == null ? MemorySegment.NULL : ObjC.nsstring(kind)),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)));
        } catch (Throwable t) {
            throw new RuntimeException("registerNib:forSupplementaryViewOfKind:withIdentifier: failed", t);
        }
    }

    /// [view makeItemWithIdentifier:forIndexPath:] — dequeued or newly instantiated item.
    public NSCollectionViewItem makeItemWithIdentifier(String identifier, MemorySegment indexPath) {
        try {
            return NSCollectionViewItem.wrap((MemorySegment) handles.hIdIdId().invokeExact(peer, ObjC.sel("makeItemWithIdentifier:forIndexPath:"),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)),
                    (MemorySegment) (indexPath == null ? MemorySegment.NULL : indexPath)));
        } catch (Throwable t) {
            throw new RuntimeException("makeItemWithIdentifier:forIndexPath: failed", t);
        }
    }

    /// [view makeSupplementaryViewOfKind:withIdentifier:forIndexPath:]
    public NSView makeSupplementaryViewOfKind(String kind, String identifier, MemorySegment indexPath) {
        try {
            return NSView.wrap((MemorySegment) handles.hIdIdIdId().invokeExact(peer, ObjC.sel("makeSupplementaryViewOfKind:withIdentifier:forIndexPath:"),
                    (MemorySegment) (kind == null ? MemorySegment.NULL : ObjC.nsstring(kind)),
                    (MemorySegment) (identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier)),
                    (MemorySegment) (indexPath == null ? MemorySegment.NULL : indexPath)));
        } catch (Throwable t) {
            throw new RuntimeException("makeSupplementaryViewOfKind:withIdentifier:forIndexPath: failed", t);
        }
    }

    /// [view itemAtIndex:] — item at a section-0 index, or nil (soft-deprecated, single section only).
    public NSCollectionViewItem itemAtIndex(long index) {
        try {
            return NSCollectionViewItem.wrap((MemorySegment) handles.hIdInt().invokeExact(peer, ObjC.sel("itemAtIndex:"), index));
        } catch (Throwable t) {
            throw new RuntimeException("itemAtIndex: failed", t);
        }
    }

    /// [view itemAtIndexPath:] — live item or nil when not instantiated.
    public NSCollectionViewItem itemAtIndexPath(MemorySegment indexPath) {
        try {
            return NSCollectionViewItem.wrap((MemorySegment) handles.hIdId().invokeExact(peer, ObjC.sel("itemAtIndexPath:"), (MemorySegment) (indexPath == null ? MemorySegment.NULL : indexPath)));
        } catch (Throwable t) {
            throw new RuntimeException("itemAtIndexPath: failed", t);
        }
    }

    /// [view visibleItems] — NSArray of live NSCollectionViewItem peers (id).
    public MemorySegment visibleItems() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("visibleItems"));
        } catch (Throwable t) {
            throw new RuntimeException("visibleItems failed", t);
        }
    }

    /// [view indexPathsForVisibleItems] — NSSet of NSIndexPath peers (id).
    public MemorySegment indexPathsForVisibleItems() {
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("indexPathsForVisibleItems"));
        } catch (Throwable t) {
            throw new RuntimeException("indexPathsForVisibleItems failed", t);
        }
    }

    /// [view indexPathForItem:] — NSIndexPath peer or nil.
    public MemorySegment indexPathForItem(NSCollectionViewItem item) {
        try {
            MemorySegment arg = (item == null || item.peer() == null || item.peer().address() == 0) ? MemorySegment.NULL : item.peer();
            return (MemorySegment) handles.hIdId().invokeExact(peer, ObjC.sel("indexPathForItem:"), (MemorySegment) arg);
        } catch (Throwable t) {
            throw new RuntimeException("indexPathForItem: failed", t);
        }
    }

    /// [view indexPathForItemAtPoint:] — NSIndexPath peer or nil.
    public MemorySegment indexPathForItemAtPoint(NSPoint point) {
        if (point == null) return MemorySegment.NULL;
        try {
            return (MemorySegment) handles.hIdPoint().invokeExact(peer, ObjC.sel("indexPathForItemAtPoint:"), point.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("indexPathForItemAtPoint: failed", t);
        }
    }

    /// [view supplementaryViewForElementKind:atIndexPath:] — live view or nil.
    public NSView supplementaryViewForElementKind(String kind, MemorySegment indexPath) {
        try {
            return NSView.wrap((MemorySegment) handles.hIdIdId().invokeExact(peer, ObjC.sel("supplementaryViewForElementKind:atIndexPath:"),
                    (MemorySegment) (kind == null ? MemorySegment.NULL : ObjC.nsstring(kind)),
                    (MemorySegment) (indexPath == null ? MemorySegment.NULL : indexPath)));
        } catch (Throwable t) {
            throw new RuntimeException("supplementaryViewForElementKind:atIndexPath: failed", t);
        }
    }

    /// [view visibleSupplementaryViewsOfKind:] — NSArray of live views (id).
    public MemorySegment visibleSupplementaryViewsOfKind(String kind) {
        try {
            return (MemorySegment) handles.hIdId().invokeExact(peer, ObjC.sel("visibleSupplementaryViewsOfKind:"), (MemorySegment) (kind == null ? MemorySegment.NULL : ObjC.nsstring(kind)));
        } catch (Throwable t) {
            throw new RuntimeException("visibleSupplementaryViewsOfKind: failed", t);
        }
    }

    /// [view indexPathsForVisibleSupplementaryElementsOfKind:] — NSSet of NSIndexPath peers (id).
    public MemorySegment indexPathsForVisibleSupplementaryElementsOfKind(String kind) {
        try {
            return (MemorySegment) handles.hIdId().invokeExact(peer, ObjC.sel("indexPathsForVisibleSupplementaryElementsOfKind:"), (MemorySegment) (kind == null ? MemorySegment.NULL : ObjC.nsstring(kind)));
        } catch (Throwable t) {
            throw new RuntimeException("indexPathsForVisibleSupplementaryElementsOfKind: failed", t);
        }
    }

    /// [view insertSections:] — indexes of sections to insert.
    public void insertSections(MemorySegment sections) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("insertSections:"), (MemorySegment) (sections == null ? MemorySegment.NULL : sections));
        } catch (Throwable t) {
            throw new RuntimeException("insertSections: failed", t);
        }
    }

    /// [view deleteSections:]
    public void deleteSections(MemorySegment sections) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("deleteSections:"), (MemorySegment) (sections == null ? MemorySegment.NULL : sections));
        } catch (Throwable t) {
            throw new RuntimeException("deleteSections: failed", t);
        }
    }

    /// [view reloadSections:]
    public void reloadSections(MemorySegment sections) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("reloadSections:"), (MemorySegment) (sections == null ? MemorySegment.NULL : sections));
        } catch (Throwable t) {
            throw new RuntimeException("reloadSections: failed", t);
        }
    }

    /// [view moveSection:toSection:]
    public void moveSection(long section, long newSection) {
        try {
            handles.hVoidIntInt().invokeExact(peer, ObjC.sel("moveSection:toSection:"), section, newSection);
        } catch (Throwable t) {
            throw new RuntimeException("moveSection:toSection: failed", t);
        }
    }

    /// [view insertItemsAtIndexPaths:] — NSSet of NSIndexPath peers.
    public void insertItemsAtIndexPaths(MemorySegment indexPaths) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("insertItemsAtIndexPaths:"), (MemorySegment) (indexPaths == null ? MemorySegment.NULL : indexPaths));
        } catch (Throwable t) {
            throw new RuntimeException("insertItemsAtIndexPaths: failed", t);
        }
    }

    /// [view deleteItemsAtIndexPaths:]
    public void deleteItemsAtIndexPaths(MemorySegment indexPaths) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("deleteItemsAtIndexPaths:"), (MemorySegment) (indexPaths == null ? MemorySegment.NULL : indexPaths));
        } catch (Throwable t) {
            throw new RuntimeException("deleteItemsAtIndexPaths: failed", t);
        }
    }

    /// [view reloadItemsAtIndexPaths:]
    public void reloadItemsAtIndexPaths(MemorySegment indexPaths) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("reloadItemsAtIndexPaths:"), (MemorySegment) (indexPaths == null ? MemorySegment.NULL : indexPaths));
        } catch (Throwable t) {
            throw new RuntimeException("reloadItemsAtIndexPaths: failed", t);
        }
    }

    /// [view moveItemAtIndexPath:toIndexPath:]
    public void moveItemAtIndexPath(MemorySegment from, MemorySegment to) {
        try {
            handles.hVoidIdId().invokeExact(peer, ObjC.sel("moveItemAtIndexPath:toIndexPath:"),
                    (MemorySegment) (from == null ? MemorySegment.NULL : from),
                    (MemorySegment) (to == null ? MemorySegment.NULL : to));
        } catch (Throwable t) {
            throw new RuntimeException("moveItemAtIndexPath:toIndexPath: failed", t);
        }
    }

    /// [view toggleSectionCollapse:] — collapse/expand the sender's section.
    public void toggleSectionCollapse(MemorySegment sender) {
        try {
            handles.hSetDataSource().invokeExact(peer, ObjC.sel("toggleSectionCollapse:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
        } catch (Throwable t) {
            throw new RuntimeException("toggleSectionCollapse: failed", t);
        }
    }

    /// [view scrollToItemsAtIndexPaths:scrollPosition:]
    public void scrollToItemsAtIndexPaths(MemorySegment indexPaths, long scrollPosition) {
        try {
            handles.hVoidIdInt().invokeExact(peer, ObjC.sel("scrollToItemsAtIndexPaths:scrollPosition:"), (MemorySegment) (indexPaths == null ? MemorySegment.NULL : indexPaths), scrollPosition);
        } catch (Throwable t) {
            throw new RuntimeException("scrollToItemsAtIndexPaths:scrollPosition: failed", t);
        }
    }
}
