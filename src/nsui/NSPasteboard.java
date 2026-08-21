package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPasteboard — minimal wrapper over native `NSPasteboard`.
/// Provides generalPasteboard, clearContents, setString:forType:, stringForType:.
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
}
