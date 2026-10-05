package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPasteboard — minimal wrapper over native `NSPasteboard`.
/// Provides generalPasteboard, clearContents, setString:forType:, stringForType:.
///
/// Coverage notes (header: NSPasteboard.h wins on API truth):
/// - Wrapped: the name/class factories, changeCount, types, pasteboardItems,
///   declare/addTypes, availableTypeFromArray:, the string/data/property-list
///   triples, writeObjects:, readObjectsForClasses:options: and its canRead
///   queries, prepareForNewContentsWithOptions:, releaseGlobally,
///   typesFilterableTo: and URLFromPasteboard:.
/// - Omitted: detectPatternsForPatterns:/detectValues…/detectMetadata…
///   (block-taking methods need upcall machinery); declareTypes:owner:
///   delegate-callback siblings pasteboard:provideDataForType:/
///   pasteboardChangedOwner: (delegate protocol); writeFileContents:/
///   readFileContentsType:toFile:/writeFileWrapper/readFileWrapper
///   (filesystem side effects, out of scope for a memory pasteboard test);
///   writableTypesForPasteboard:/… readingOptions…/pasteboardPropertyList…/
///   initWithPasteboardPropertyList: (NSPasteboardWriting/Reading delegate
///   protocols, need upcall machinery; init covered by create()).
public final class NSPasteboard extends NSObject {

            private record Handles(MethodHandle hClear, MethodHandle hSetString, MethodHandle hStringFor, MethodHandle hName) {}
    private static volatile Handles handles;

    private NSPasteboard(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSPasteboard wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPasteboard(peer);
    }

    /// [NSPasteboard generalPasteboard]
    public static NSPasteboard generalPasteboard() {
        ensureInit();
        MemorySegment pb = ObjC.msgSendId(ObjC.cls("NSPasteboard"), ObjC.sel("generalPasteboard"));
        return wrap(pb);
    }

    /// [NSPasteboard pasteboardWithName:]
    public static NSPasteboard pasteboardWithName(String name) {
        ensureInit();
        MemorySegment pb = ObjC.msgSendIdId(ObjC.cls("NSPasteboard"), ObjC.sel("pasteboardWithName:"), ObjC.nsstring(name));
        return wrap(pb);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID))
        );
    }

    /// clearContents — returns changeCount.
    public long clearContents() {
        ensureInit();
        try { return (long) handles.hClear().invokeExact(peer, ObjC.sel("clearContents")); }
        catch (Throwable t) { throw new RuntimeException("clearContents failed", t); }
    }

    /// setString:forType:
    public boolean setStringForType(String string, String type) {
        ensureInit();
        if (string == null || type == null) return false;
        try {
            return (boolean) handles.hSetString().invokeExact(peer, ObjC.sel("setString:forType:"), ObjC.nsstring(string), ObjC.nsstring(type));
        } catch (Throwable t) { throw new RuntimeException("setString:forType: failed", t); }
    }

    public boolean setStringForType(String string, MemorySegment type) {
        ensureInit();
        if (string == null || type == null || type.address() == 0) return false;
        try {
            return (boolean) handles.hSetString().invokeExact(peer, ObjC.sel("setString:forType:"), ObjC.nsstring(string), type);
        } catch (Throwable t) { throw new RuntimeException("setString:forType: failed", t); }
    }

    /// stringForType: — returns Java String or null.
    public String stringForType(String type) {
        ensureInit();
        if (type == null) return null;
        try {
            MemorySegment s = (MemorySegment) handles.hStringFor().invokeExact(peer, ObjC.sel("stringForType:"), ObjC.nsstring(type));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("stringForType: failed", t); }
    }

    public String stringForType(MemorySegment type) {
        ensureInit();
        if (type == null || type.address() == 0) return null;
        try {
            MemorySegment s = (MemorySegment) handles.hStringFor().invokeExact(peer, ObjC.sel("stringForType:"), type);
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("stringForType: failed", t); }
    }

    /// name
    public String name() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hName().invokeExact(peer, ObjC.sel("name"));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("name failed", t); }
    }

    /// Common pasteboard type constants as Java strings.
    public static final String NSPasteboardTypeString = "public.utf8-plain-text";
    public static final String NSPasteboardTypePNG = "public.png";
    public static final String NSPasteboardTypeTIFF = "public.tiff";
    public static final String NSPasteboardTypePDF = "com.adobe.pdf";

    /// declareTypes:owner: — minimal.
    public long declareTypes(NSArray types, NSObject owner) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID, Arg.ID));
            MemorySegment ownerSeg = (MemorySegment) (owner == null || owner.peer() == null || owner.peer().address() == 0 ? MemorySegment.NULL : owner.peer());
            return (long) h.invokeExact(peer, ObjC.sel("declareTypes:owner:"), (MemorySegment) (types == null || types.peer() == null || types.peer().address() == 0 ? MemorySegment.NULL : types.peer()), ownerSeg);
        } catch (Throwable t) { throw new RuntimeException("declareTypes:owner: failed", t); }
    }

    /// declareTypes:owner: with List<String> convenience (converts to NSArray of NSString, owner as MemorySegment)
    public void declareTypes(java.util.List<String> types, MemorySegment owner) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID, Arg.ID));
            MemorySegment arr;
            if (types == null || types.isEmpty()) {
                arr = ObjC.msgSendId(ObjC.cls("NSArray"), ObjC.sel("array"));
            } else {
                arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
                for (String t : types) {
                    if (t == null) continue;
                    ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ObjC.nsstring(t));
                }
            }
            MemorySegment ownerSeg = (owner == null || owner.address() == 0) ? MemorySegment.NULL : owner;
            // native returns NSInteger but void API ignores it
            long ignored = (long) h.invokeExact(peer, ObjC.sel("declareTypes:owner:"), arr, ownerSeg);
            // suppress unused
            if (ignored == Long.MIN_VALUE) System.out.print("");
        } catch (Throwable t) { throw new RuntimeException("declareTypes:owner: failed", t); }
    }

    /// availableTypeFromArray:
    public String availableTypeFromArray(NSArray types) {
        ensureInit();
        if (types == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment s = (MemorySegment) h.invokeExact(peer, ObjC.sel("availableTypeFromArray:"), types.peer());
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("availableTypeFromArray: failed", t); }
    }

    /// [NSPasteboard pasteboardWithUniqueName] — a private board for tests.
    public static NSPasteboard pasteboardWithUniqueName() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("NSPasteboard"), ObjC.sel("pasteboardWithUniqueName")));
    }

    /// changeCount — bumped by every content mutation.
    public long changeCount() {
        ensureInit();
        try { return (long) handles.hClear().invokeExact(peer, ObjC.sel("changeCount")); }
        catch (Throwable t) { throw new RuntimeException("changeCount failed", t); }
    }

    /// types — the advertised data types.
    public NSArray types() {
        ensureInit();
        try { return NSArray.wrap((MemorySegment) handles.hName().invokeExact(peer, ObjC.sel("types"))); }
        catch (Throwable t) { throw new RuntimeException("types failed", t); }
    }

    /// pasteboardItems — the item list (10.6+).
    public NSArray pasteboardItems() {
        ensureInit();
        try { return NSArray.wrap((MemorySegment) handles.hName().invokeExact(peer, ObjC.sel("pasteboardItems"))); }
        catch (Throwable t) { throw new RuntimeException("pasteboardItems failed", t); }
    }

    /// indexOfPasteboardItem:.
    public long indexOfPasteboardItem(NSPasteboardItem item) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("indexOfPasteboardItem:"),
                    (MemorySegment) (item == null ? MemorySegment.NULL : item.peer()));
        } catch (Throwable t) { throw new RuntimeException("indexOfPasteboardItem: failed", t); }
    }

    /// canReadItemWithDataConformingToTypes:.
    public boolean canReadItemWithDataConformingToTypes(NSArray types) {
        ensureInit();
        if (types == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("canReadItemWithDataConformingToTypes:"), types.peer());
        } catch (Throwable t) { throw new RuntimeException("canReadItemWithDataConformingToTypes: failed", t); }
    }

    /// addTypes:owner: — returns the new changeCount.
    public long addTypesOwner(NSArray types, NSObject owner) {
        ensureInit();
        if (types == null) return changeCount();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("addTypes:owner:"), types.peer(),
                    (MemorySegment) (owner == null ? MemorySegment.NULL : owner.peer()));
        } catch (Throwable t) { throw new RuntimeException("addTypes:owner: failed", t); }
    }

    /// writeObjects: — write pasteboard-writing objects (e.g. NSPasteboardItem).
    public boolean writeObjects(NSArray objects) {
        ensureInit();
        if (objects == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("writeObjects:"), objects.peer());
        } catch (Throwable t) { throw new RuntimeException("writeObjects: failed", t); }
    }

    /// readObjectsForClasses:options: — classes are Class peers in an NSArray.
    public NSArray readObjectsForClassesOptions(NSArray classes, MemorySegment options) {
        ensureInit();
        if (classes == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("readObjectsForClasses:options:"),
                    classes.peer(), (MemorySegment) (options == null ? MemorySegment.NULL : options));
            return NSArray.wrap(r);
        } catch (Throwable t) { throw new RuntimeException("readObjectsForClasses:options: failed", t); }
    }

    /// canReadObjectForClasses:options:.
    public boolean canReadObjectForClassesOptions(NSArray classes, MemorySegment options) {
        ensureInit();
        if (classes == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("canReadObjectForClasses:options:"),
                    classes.peer(), (MemorySegment) (options == null ? MemorySegment.NULL : options));
        } catch (Throwable t) { throw new RuntimeException("canReadObjectForClasses:options: failed", t); }
    }

    /// prepareForNewContentsWithOptions: — returns the new changeCount.
    public long prepareForNewContentsWithOptions(long options) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("prepareForNewContentsWithOptions:"), options);
        } catch (Throwable t) { throw new RuntimeException("prepareForNewContentsWithOptions: failed", t); }
    }

    /// setData:forType:.
    public boolean setDataForType(NSData data, String type) {
        ensureInit();
        if (data == null || type == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("setData:forType:"), data.peer(), ObjC.nsstring(type));
        } catch (Throwable t) { throw new RuntimeException("setData:forType: failed", t); }
    }

    /// dataForType: — raw bytes object, or null.
    public NSData dataForType(String type) {
        ensureInit();
        if (type == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            return NSData.wrap((MemorySegment) h.invokeExact(peer, ObjC.sel("dataForType:"), ObjC.nsstring(type)));
        } catch (Throwable t) { throw new RuntimeException("dataForType: failed", t); }
    }

    /// setPropertyList:forType: — plist peer (NSString/NSArray/NSDictionary…).
    public boolean setPropertyListForType(MemorySegment plist, String type) {
        ensureInit();
        if (plist == null || plist.address() == 0 || type == null) return false;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("setPropertyList:forType:"), plist, ObjC.nsstring(type));
        } catch (Throwable t) { throw new RuntimeException("setPropertyList:forType: failed", t); }
    }

    /// propertyListForType: — raw plist object peer, or null.
    public MemorySegment propertyListForType(String type) {
        ensureInit();
        if (type == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("propertyListForType:"), ObjC.nsstring(type));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("propertyListForType: failed", t); }
    }

    /// releaseGlobally — destroy a globally-published board.
    public void releaseGlobally() {
        ObjC.msgSendVoid(peer, ObjC.sel("releaseGlobally"));
    }

    /// [NSPasteboard typesFilterableTo:] — filterable type list.
    public static NSArray typesFilterableTo(String type) {
        ensureInit();
        if (type == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            return NSArray.wrap((MemorySegment) h.invokeExact(ObjC.cls("NSPasteboard"),
                    ObjC.sel("typesFilterableTo:"), ObjC.nsstring(type)));
        } catch (Throwable t) { throw new RuntimeException("typesFilterableTo: failed", t); }
    }

    /// [NSURL URLFromPasteboard:] (NSURL(NSPasteboardSupport) category, declared
    /// in NSPasteboard.h) — file URL on the board, raw peer or null.
    public static MemorySegment urlFromPasteboard(NSPasteboard board) {
        ensureInit();
        if (board == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(ObjC.cls("NSURL"),
                    ObjC.sel("URLFromPasteboard:"), board.peer());
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) { throw new RuntimeException("URLFromPasteboard: failed", t); }
    }
}
