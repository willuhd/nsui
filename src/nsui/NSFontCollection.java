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

    private record Handles(MethodHandle hWithDescriptors, MethodHandle hMatching) {}
    private static volatile Handles handles;

    private NSFontCollection(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSFontCollection wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFontCollection(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)));
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

    /// fontCollectionWithName:visibility: — a named library collection.
    public static NSFontCollection withName(String name, long visibility) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hMatching().invokeExact(
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
}
