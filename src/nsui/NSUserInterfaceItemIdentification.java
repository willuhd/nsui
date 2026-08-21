package nsui;

/// NSUserInterfaceItemIdentification — Java projection of AppKit's
/// `NSUserInterfaceItemIdentification` protocol (informal).
///
/// AppKit declares this protocol on items that carry an `identifier`:
/// `NSToolbarItem`, `NSMenuItem`, `NSSegmentedControl` segments via the
/// toolbar/menu layer, etc. The native property is
/// `@property (copy) NSUserInterfaceItemIdentifier identifier` backed by
/// `-[identifier]` / `-[setIdentifier:]` (NSString*).
///
/// Implementors bridge via ObjC directly:
/// <pre>
///   ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("identifier")))
///   ObjC.msgSendVoidId(peer, ObjC.sel("setIdentifier:"), ObjC.nsstring(id))
/// </pre>
/// using existing Sig shapes `Ret.ID` and `Ret.VOID Arg.ID` — no new vocabulary.
///
/// This interface is intentionally minimal: no Handles, no state, just the
/// Java contract. `NSToolbarItem` and `NSMenuItem` already expose the
/// underlying ObjC selectors; conforming types should delegate to those
/// selectors.
public interface NSUserInterfaceItemIdentification {

    /// `-[identifier]` — the item's identifier string, or null if none.
    String identifier();

    /// `-[setIdentifier:]` — set the item's identifier (null to clear).
    void setIdentifier(String id);
}
