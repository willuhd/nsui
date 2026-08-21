package nsui;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;

/// NSToolbarDelegate — helper to build a DelegateProxy-backed NSToolbar delegate.
///
/// Uses existing DelegateProxy shapes:
///  - toolbar:itemForItemIdentifier:willBeInsertedIntoToolbar:  "@@:@@" -> IdIdArg (2 ids, willBeInserted bool folded into id 0/1)
///  - toolbarDefaultItemIdentifiers:          "@@:" -> IdArg
///  - toolbarAllowedItemIdentifiers:          "@@:" -> IdArg
///  - toolbarSelectableItemIdentifiers:       "@@:" -> IdArg
///
/// No new upcall shapes are introduced; reuses dispatchId / dispatchIdId.
public final class NSToolbarDelegate {

    private NSToolbarDelegate() {}

    /// Java-side delegate for NSToolbar.
    public interface Delegate {
        /// toolbar:itemForItemIdentifier:willBeInsertedIntoToolbar: -> NSToolbarItem id or NULL
        MemorySegment toolbarItemForIdentifier(NSToolbar toolbar, String identifier, boolean willInsert);

        /// toolbarDefaultItemIdentifiers: -> list of identifiers (may be empty)
        List<String> toolbarDefaultIdentifiers(NSToolbar toolbar);

        /// Optional override for allowed identifiers; default delegates to toolbarDefaultIdentifiers
        default List<String> toolbarAllowedIdentifiers(NSToolbar toolbar) {
            return toolbarDefaultIdentifiers(toolbar);
        }

        /// Optional override for selectable identifiers; default delegates to toolbarDefaultIdentifiers
        default List<String> toolbarSelectableIdentifiers(NSToolbar toolbar) {
            return toolbarDefaultIdentifiers(toolbar);
        }
    }

    /// Build a DelegateProxy delegate for NSToolbar. The returned MemorySegment is a retained
    /// ObjC instance (subclass of NSObject named NSUIToolbarDelegate) whose selectors are
    /// routed back to the Java Delegate via DispatchIdId / DispatchId.
    public static MemorySegment create(Delegate d) {
        if (d == null) throw new IllegalArgumentException("NSToolbarDelegate Delegate is null");

        Map<String, DelegateProxy.IdIdArg> idIds = new HashMap<>();
        Map<String, DelegateProxy.IdArg> ids = new HashMap<>();

        // toolbar:itemForItemIdentifier:willBeInsertedIntoToolbar: — signature "@@:@@"
        // Note: native has 3 args (toolbar, identifier, BOOL willInsert) but we reuse
        // the existing IdIdArg shape (2 ids) — BOOL is folded to 0/1 via Arg.ID and ignored;
        // the Java callback receives willInsert=false always (documented approximation).
        // If the runtime calls with 3 object args via escape-hatch, the extra BOOL is ignored safely on arm64.
        idIds.put("toolbar:itemForItemIdentifier:willBeInsertedIntoToolbar:", (toolbarSeg, identifierSeg) -> {
            String ident = ObjC.toString(identifierSeg);
            // identifierSeg may be null if caller passed BOOL as 3rd arg; handle gracefully
            if (ident == null) ident = "";
            NSToolbar tb = NSToolbar.wrap(toolbarSeg);
            // willInsert is not available via IdIdArg; pass false (approximation)
            MemorySegment result = d.toolbarItemForIdentifier(tb, ident, false);
            return (result == null || result.address() == 0) ? MemorySegment.NULL : result;
        });

        // toolbarDefaultItemIdentifiers: — "@@:" -> id (NSArray of NSString)
        ids.put("toolbarDefaultItemIdentifiers:", sender -> {
            NSToolbar tb = NSToolbar.wrap(sender);
            List<String> list = d.toolbarDefaultIdentifiers(tb);
            if (list == null || list.isEmpty()) {
                // return empty array rather than NULL to be safe
                MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSArray"), ObjC.sel("array"));
                return (arr == null || arr.address() == 0) ? MemorySegment.NULL : arr;
            }
            MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
            for (String s : list) {
                if (s == null) continue;
                ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ObjC.nsstring(s));
            }
            return arr;
        });

        // toolbarAllowedItemIdentifiers: — same shape, delegate to allowed
        ids.put("toolbarAllowedItemIdentifiers:", sender -> {
            NSToolbar tb = NSToolbar.wrap(sender);
            List<String> list = d.toolbarAllowedIdentifiers(tb);
            if (list == null) list = d.toolbarDefaultIdentifiers(tb);
            if (list == null || list.isEmpty()) {
                MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSArray"), ObjC.sel("array"));
                return (arr == null || arr.address() == 0) ? MemorySegment.NULL : arr;
            }
            MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
            for (String s : list) {
                if (s == null) continue;
                ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ObjC.nsstring(s));
            }
            return arr;
        });

        // toolbarSelectableItemIdentifiers: — same shape, delegate to selectable
        ids.put("toolbarSelectableItemIdentifiers:", sender -> {
            NSToolbar tb = NSToolbar.wrap(sender);
            List<String> list = d.toolbarSelectableIdentifiers(tb);
            if (list == null) list = d.toolbarDefaultIdentifiers(tb);
            if (list == null || list.isEmpty()) {
                MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSArray"), ObjC.sel("array"));
                return (arr == null || arr.address() == 0) ? MemorySegment.NULL : arr;
            }
            MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
            for (String s : list) {
                if (s == null) continue;
                ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ObjC.nsstring(s));
            }
            return arr;
        });

        // Use the full overload with 7 maps (bool, void, int, idIdInt, windowSize, idIds, ids)
        // First five are empty; last two carry toolbar wiring. Reuse existing handles.
        return DelegateProxy.delegate(
                "NSObject", "NSUIToolbarDelegate",
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                idIds, ids);
    }
}
