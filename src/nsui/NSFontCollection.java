package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSFontCollection — named/queryable font sets: match installed fonts by
/// family or descriptor query. Thin stateless wrapper. (Show/hide/rename
/// mutate the Font Book library and take NSError** — omitted deliberately.)
public final class NSFontCollection extends NSObject {

    private record Handles(MethodHandle hWithDescriptors, MethodHandle hMatching, MethodHandle hMatchingName) {}
    private static volatile Handles handles;

    private NSFontCollection(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSFontCollection wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFontCollection(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.INT)));
    }

    /// fontCollectionWithDescriptors: — query by descriptor array.
    public static NSFontCollection withDescriptors(NSArray descriptors) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hWithDescriptors().invokeExact(
                    ObjC.cls("NSFontCollection"), ObjC.sel("fontCollectionWithDescriptors:"),
                    (MemorySegment) (descriptors == null ? MemorySegment.NULL : descriptors.peer())));
        } catch (Throwable t) {
            throw new RuntimeException("fontCollectionWithDescriptors: failed", t);
        }
    }

    /// fontCollectionWithName:visibility: — a named library collection
    /// (NSString*, NSUInteger) -> id, previously routed through the ID(ID,ID) handle.
    public static NSFontCollection withName(String name, long visibility) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hMatchingName().invokeExact(
                    ObjC.cls("NSFontCollection"), ObjC.sel("fontCollectionWithName:visibility:"),
                    ObjC.nsstring(name), visibility));
        } catch (Throwable t) {
            throw new RuntimeException("fontCollectionWithName:visibility: failed", t);
        }
    }

    /// matchingDescriptorsForFamily: — installed descriptors for a family.
    public NSArray matchingDescriptorsForFamily(String family) {
        return NSArray.wrap(ObjC.msgSendIdId(peer,
                ObjC.sel("matchingDescriptorsForFamily:"), ObjC.nsstring(family)));
    }

    /// queryDescriptors.
    public NSArray queryDescriptors() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("queryDescriptors")));
    }

    /// `+[NSFontCollection fontCollectionWithAllAvailableDescriptors]`.
    public static NSFontCollection allAvailableDescriptors() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("NSFontCollection"),
                ObjC.sel("fontCollectionWithAllAvailableDescriptors")));
    }

    /// `+[NSFontCollection fontCollectionWithLocale:]` — locale is NSLocale* (may be NULL).
    public static NSFontCollection withLocale(MemorySegment locale) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithDescriptors().invokeExact(
                    ObjC.cls("NSFontCollection"), ObjC.sel("fontCollectionWithLocale:"),
                    (MemorySegment) (locale == null ? MemorySegment.NULL : locale));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("fontCollectionWithLocale: failed", t);
        }
    }

    /// `+[NSFontCollection allFontCollectionNames]` — named collections visible to this process.
    public static NSArray allFontCollectionNames() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(ObjC.cls("NSFontCollection"), ObjC.sel("allFontCollectionNames")));
    }

    /// `+[NSFontCollection fontCollectionWithName:]` — the named collection (may be nil).
    public static NSFontCollection withName(String name) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithDescriptors().invokeExact(
                    ObjC.cls("NSFontCollection"), ObjC.sel("fontCollectionWithName:"),
                    ObjC.nsstring(name == null ? "" : name));
            return wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("fontCollectionWithName: failed", t);
        }
    }

    /// exclusionDescriptors — query descriptors excluded from the match.
    public NSArray exclusionDescriptors() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("exclusionDescriptors")));
    }

    /// matchingDescriptors — queryDescriptors minus exclusionDescriptors, matched now.
    public NSArray matchingDescriptors() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("matchingDescriptors")));
    }

    /// -matchingDescriptorsWithOptions: — options may be NULL (NSDictionary*).
    public NSArray matchingDescriptorsWithOptions(MemorySegment optionsOrNull) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithDescriptors().invokeExact(peer,
                    ObjC.sel("matchingDescriptorsWithOptions:"),
                    (MemorySegment) (optionsOrNull == null ? MemorySegment.NULL : optionsOrNull));
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("matchingDescriptorsWithOptions: failed", t);
        }
    }

    /// -matchingDescriptorsForFamily:options: — family match with options (may be NULL).
    public NSArray matchingDescriptorsForFamilyOptions(String family, MemorySegment optionsOrNull) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hMatching().invokeExact(peer,
                    ObjC.sel("matchingDescriptorsForFamily:options:"), ObjC.nsstring(family == null ? "" : family),
                    (MemorySegment) (optionsOrNull == null ? MemorySegment.NULL : optionsOrNull));
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("matchingDescriptorsForFamily:options: failed", t);
        }
    }

    // ---- visibility values (NSFontCollectionVisibility, header bit shifts) ----
    public static final long VISIBILITY_PROCESS = 1L;
    public static final long VISIBILITY_USER = 2L;
    public static final long VISIBILITY_COMPUTER = 4L;

    // SDK omissions (documented in the class header + reported): show/hide/rename take
    // NSError** (no vocabulary shape) and mutate the Font Book library. NSMutableFontCollection
    // needs its own wrapper file (not in this batch) so add/removeQueryForDescriptors: and the
    // query/exclusion setters are omitted here.
}
