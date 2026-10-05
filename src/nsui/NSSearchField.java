package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSearchField — an AppKit search-field control. Thin 1:1 wrapper
/// over a native `NSSearchField`: every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer.
/// Mirrors the native hierarchy: NSSearchField is an NSTextField is an
/// NSControl is an NSView.
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSSearchField.h
/// OMITTED: rectForSearchTextWhenCentered:/rectForSearchButtonWhenCentered:/
/// rectForCancelButtonWhenCentered: — each returns NSRect taking BOOL, shape
/// (RECT,BOOL) is NOT in the Sig vocabulary (grep Sig.java: no of(Ret.RECT, Arg.BOOL));
/// NSSearchFieldDelegate (searchFieldDidStartSearching:/searchFieldDidEndSearching:) —
/// delegate callbacks need upcall delegate-proxy machinery; delegate property itself is
/// inherited from NSTextField (same selectors delegate/setDelegate:).
public class NSSearchField extends NSTextField {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hBool;        // (id, SEL) -> bool
    private static MethodHandle hVoidBool;    // (id, SEL, bool) -> void
    private static MethodHandle hId;          // (id, SEL) -> id
    private static MethodHandle hVoidId;      // (id, SEL, id) -> void
    private static MethodHandle hLong;        // (id, SEL) -> long
    private static MethodHandle hVoidLong;    // (id, SEL, long) -> void
    private static MethodHandle hDouble;      // (id, SEL) -> double

    protected NSSearchField(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hVoidBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hId = ObjC.handle(Sig.of(Ret.ID));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hLong = ObjC.handle(Sig.of(Ret.INT));
        hVoidLong = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hDouble = ObjC.handle(Sig.of(Ret.DOUBLE));
        initialized = true;
    }

    /// `[[NSSearchField alloc] initWithFrame:frame]` — a new search field at the given rect.
        public static NSSearchField create(NSRect frame) {
        ensureInit();
        return new NSSearchField(ObjC.newView("NSSearchField", frame));
    }

    /// Wrap an existing native NSSearchField id.
    public static NSSearchField wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSearchField(peer);
    }

    // ---------------------------------------------------------------- placeholder / stringValue
    // stringValue/setStringValue and placeholderString/setPlaceholderString are
    // inherited from NSTextField/NSControl but re-exposed for discoverability.
    @Override
    public String stringValue() { return super.stringValue(); }
    @Override
    public void setStringValue(String value) { super.setStringValue(value); }
    @Override
    public String placeholderString() { return super.placeholderString(); }
    @Override
    public void setPlaceholderString(String s) { super.setPlaceholderString(s); }

    // ---------------------------------------------------------------- searchMenuTemplate
    /// [field searchMenuTemplate] — the menu shown on the search-field loupe.
    public MemorySegment searchMenuTemplate() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("searchMenuTemplate")); }
        catch (Throwable t) { throw new RuntimeException("searchMenuTemplate failed", t); }
    }

    /// [field setSearchMenuTemplate:] — install a menu template (pass NSMenu.peer() or NULL to clear).
    public void setSearchMenuTemplate(MemorySegment menu) {
        ensureInit();
        MemorySegment m = ((MemorySegment) (menu == null ? MemorySegment.NULL : menu));
        try { hVoidId.invokeExact(peer, ObjC.sel("setSearchMenuTemplate:"), m); }
        catch (Throwable t) { throw new RuntimeException("setSearchMenuTemplate: failed", t); }
    }

    /// Typed overload accepting NSMenu.
    public void setSearchMenuTemplate(NSMenu menu) {
        setSearchMenuTemplate((MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
    }

    /// Convenience getter returning NSMenu wrapper (null if nil).
    public NSMenu searchMenuTemplateAsMenu() {
        MemorySegment m = searchMenuTemplate();
        return NSMenu.wrap(m);
    }

    // ---------------------------------------------------------------- sendsSearchStringImmediately
    /// [field sendsSearchStringImmediately] — whether the action fires on each keystroke.
    public boolean sendsSearchStringImmediately() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("sendsSearchStringImmediately")); }
        catch (Throwable t) { throw new RuntimeException("sendsSearchStringImmediately failed", t); }
    }

    /// [field setSendsSearchStringImmediately:]
    public void setSendsSearchStringImmediately(boolean flag) {
        ensureInit();
        try { hVoidBool.invokeExact(peer, ObjC.sel("setSendsSearchStringImmediately:"), flag); }
        catch (Throwable t) { throw new RuntimeException("setSendsSearchStringImmediately: failed", t); }
    }

    // ---------------------------------------------------------------- sendsWholeSearchString
    /// [field sendsWholeSearchString] — whether the whole string is sent vs incremental.
    public boolean sendsWholeSearchString() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("sendsWholeSearchString")); }
        catch (Throwable t) { throw new RuntimeException("sendsWholeSearchString failed", t); }
    }

    /// [field setSendsWholeSearchString:]
    public void setSendsWholeSearchString(boolean flag) {
        ensureInit();
        try { hVoidBool.invokeExact(peer, ObjC.sel("setSendsWholeSearchString:"), flag); }
        catch (Throwable t) { throw new RuntimeException("setSendsWholeSearchString: failed", t); }
    }

    // ---------------------------------------------------------------- maximumRecents
    /// [field maximumRecents]
    public long maximumRecents() {
        ensureInit();
        try { return (long) hLong.invokeExact(peer, ObjC.sel("maximumRecents")); }
        catch (Throwable t) { throw new RuntimeException("maximumRecents failed", t); }
    }

    /// [field setMaximumRecents:]
    public void setMaximumRecents(long n) {
        ensureInit();
        try { hVoidLong.invokeExact(peer, ObjC.sel("setMaximumRecents:"), n); }
        catch (Throwable t) { throw new RuntimeException("setMaximumRecents: failed", t); }
    }

    // ---------------------------------------------------------------- recentSearches (NSArray)
    /// [field recentSearches] — NSArray of NSString, returned as raw id.
    public MemorySegment recentSearches() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("recentSearches")); }
        catch (Throwable t) { throw new RuntimeException("recentSearches failed", t); }
    }

    /// [field setRecentSearches:]
    public void setRecentSearches(MemorySegment array) {
        ensureInit();
        MemorySegment a = ((MemorySegment) (array == null ? MemorySegment.NULL : array));
        try { hVoidId.invokeExact(peer, ObjC.sel("setRecentSearches:"), a); }
        catch (Throwable t) { throw new RuntimeException("setRecentSearches: failed", t); }
    }

    // ---------------------------------------------------------------- recentsAutosaveName
    /// [field recentsAutosaveName] — autosave name for recents (NSString -> Java String).
    public String recentsAutosaveName() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("recentsAutosaveName"));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("recentsAutosaveName failed", t); }
    }

    /// [field setRecentsAutosaveName:]
    public void setRecentsAutosaveName(String name) {
        ensureInit();
        MemorySegment s = (name == null ? MemorySegment.NULL : ObjC.nsstring(name));
        try { hVoidId.invokeExact(peer, ObjC.sel("setRecentsAutosaveName:"), s); }
        catch (Throwable t) { throw new RuntimeException("setRecentsAutosaveName: failed", t); }
    }

    // ---------------------------------------------------------------- cancelButtonCell / searchButtonCell
    /// [field cancelButtonCell] — the cancel (clear) button cell (NSButtonCell) via the field's cell.
    public MemorySegment cancelButtonCell() {
        ensureInit();
        MemorySegment c = cell();
        if (c == null || c.address() == 0) return MemorySegment.NULL;
        try { return (MemorySegment) hId.invokeExact(c, ObjC.sel("cancelButtonCell")); }
        catch (Throwable t) { throw new RuntimeException("cancelButtonCell failed", t); }
    }

    /// [field searchButtonCell] — the search (magnifying glass) button cell via the field's cell.
    public MemorySegment searchButtonCell() {
        ensureInit();
        MemorySegment c = cell();
        if (c == null || c.address() == 0) return MemorySegment.NULL;
        try { return (MemorySegment) hId.invokeExact(c, ObjC.sel("searchButtonCell")); }
        catch (Throwable t) { throw new RuntimeException("searchButtonCell failed", t); }
    }

    // ---------------------------------------------------------------- centersPlaceholder
    /// [field centersPlaceholder] — whether placeholder is centered (10.11+).
    public boolean centersPlaceholder() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("centersPlaceholder")); }
        catch (Throwable t) { throw new RuntimeException("centersPlaceholder failed", t); }
    }

    /// [field setCentersPlaceholder:]
    public void setCentersPlaceholder(boolean flag) {
        ensureInit();
        try { hVoidBool.invokeExact(peer, ObjC.sel("setCentersPlaceholder:"), flag); }
        catch (Throwable t) { throw new RuntimeException("setCentersPlaceholder: failed", t); }
    }

    // ---------------------------------------------------------------- search/recent typed conveniences (ID shapes in vocabulary)
    /// [field recentSearches] as NSArray wrapper (elements are NSString).
    public NSArray recentSearchesArray() {
        return NSArray.wrap(recentSearches());
    }
    /// [field setRecentSearches:] from NSArray wrapper.
    public void setRecentSearches(NSArray array) {
        setRecentSearches((MemorySegment) (array == null ? MemorySegment.NULL : array.peer()));
    }

    // ---------------------------------------------------------------- layout bounds (RECT () in vocabulary)
    /// [field searchTextBounds] — rect for the search text (macOS 11+).
    /// Shape (RECT ()) is in the Sig vocabulary (grep Sig.java: of(Ret.RECT)).
    public NSRect searchTextBounds() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("searchTextBounds")));
    }
    /// [field searchButtonBounds] — rect for the search button (macOS 11+).
    public NSRect searchButtonBounds() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("searchButtonBounds")));
    }
    /// [field cancelButtonBounds] — rect for the cancel button (macOS 11+).
    public NSRect cancelButtonBounds() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("cancelButtonBounds")));
    }

    // ---------------------------------------------------------------- searchFieldCell convenience (if needed)
    /// [field searchFieldCell] — underlying NSSearchFieldCell if needed.
    public MemorySegment searchFieldCell() {
        // NSSearchField's cell is its NSSearchFieldCell; reuse NSControl.cell()
        return cell();
    }
}
