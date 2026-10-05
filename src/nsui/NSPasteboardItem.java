package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPasteboardItem — minimal wrapper over native `NSPasteboardItem` (pasteboard writer).
///
/// Coverage notes (header: NSPasteboardItem.h wins on API truth):
/// - Wrapped: create/withString, the string/data/property-list triples and
///   their readers, types and availableTypeFromArray:.
/// - Omitted: setDataProvider:forTypes: and pasteboard:item:provideDataForType:/
///   pasteboardFinishedWithDataProvider: (NSPasteboardItemDataProvider
///   delegate protocol, needs upcall machinery); detectPatterns…/
///   detectValues…/detectMetadata… (block-taking methods need upcall
///   machinery).
public final class NSPasteboardItem extends NSObject {

    private static volatile MethodHandle hInit;
    private static volatile MethodHandle hSetString;

    private NSPasteboardItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSPasteboardItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPasteboardItem(peer);
    }

    private static synchronized void ensureInit() {
        if (hInit != null) return;
        hInit = ObjC.handle(Sig.of(Ret.ID));
        hSetString = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID));
    }

    /// [[NSPasteboardItem alloc] init]
    public static NSPasteboardItem create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPasteboardItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInit.invokeExact(p, ObjC.sel("init"));
        } catch (Throwable t) {
            throw new RuntimeException("NSPasteboardItem init failed", t);
        }
        return wrap(p);
    }

    /// setString:forType: — returns BOOL
    public boolean setStringForType(String string, String type) {
        ensureInit();
        if (string == null || type == null) return false;
        try {
            return (boolean) hSetString.invokeExact(peer, ObjC.sel("setString:forType:"), ObjC.nsstring(string), ObjC.nsstring(type));
        } catch (Throwable t) {
            throw new RuntimeException("setString:forType: failed", t);
        }
    }

    /// Convenience: create with string content for a UTI type.
    public static NSPasteboardItem withString(String string, String type) {
        NSPasteboardItem item = create();
        if (item != null && string != null && type != null) {
            item.setStringForType(string, type);
        }
        return item;
    }

    /// types — the advertised data types.
    public NSArray types() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            return NSArray.wrap((MemorySegment) h.invokeExact(peer, ObjC.sel("types")));
        } catch (Throwable t) {
            throw new RuntimeException("types failed", t);
        }
    }

    /// availableTypeFromArray:.
    public String availableTypeFromArray(NSArray types) {
        ensureInit();
        if (types == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            return ObjC.toString((MemorySegment) h.invokeExact(peer,
                    ObjC.sel("availableTypeFromArray:"), types.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("availableTypeFromArray: failed", t);
        }
    }

    /// setData:forType: — returns BOOL.
    public boolean setDataForType(NSData data, String type) {
        ensureInit();
        if (data == null || type == null) return false;
        try {
            return (boolean) hSetString.invokeExact(peer, ObjC.sel("setData:forType:"),
                    data.peer(), ObjC.nsstring(type));
        } catch (Throwable t) {
            throw new RuntimeException("setData:forType: failed", t);
        }
    }

    /// dataForType: — raw bytes object, or null.
    public NSData dataForType(String type) {
        ensureInit();
        if (type == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            return NSData.wrap((MemorySegment) h.invokeExact(peer,
                    ObjC.sel("dataForType:"), ObjC.nsstring(type)));
        } catch (Throwable t) {
            throw new RuntimeException("dataForType: failed", t);
        }
    }

    /// stringForType: — Java String, or null.
    public String stringForType(String type) {
        ensureInit();
        if (type == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            return ObjC.toString((MemorySegment) h.invokeExact(peer,
                    ObjC.sel("stringForType:"), ObjC.nsstring(type)));
        } catch (Throwable t) {
            throw new RuntimeException("stringForType: failed", t);
        }
    }

    /// setPropertyList:forType: — plist peer (NSString/NSArray/NSDictionary…).
    public boolean setPropertyListForType(MemorySegment plist, String type) {
        ensureInit();
        if (plist == null || plist.address() == 0 || type == null) return false;
        try {
            return (boolean) hSetString.invokeExact(peer, ObjC.sel("setPropertyList:forType:"),
                    plist, ObjC.nsstring(type));
        } catch (Throwable t) {
            throw new RuntimeException("setPropertyList:forType: failed", t);
        }
    }

    /// propertyListForType: — raw plist object peer, or null.
    public MemorySegment propertyListForType(String type) {
        ensureInit();
        if (type == null) return null;
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("propertyListForType:"), ObjC.nsstring(type));
            return (r == null || r.address() == 0) ? null : r;
        } catch (Throwable t) {
            throw new RuntimeException("propertyListForType: failed", t);
        }
    }
}
