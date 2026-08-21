package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPasteboardItem — minimal wrapper over native `NSPasteboardItem` (pasteboard writer).
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
}
