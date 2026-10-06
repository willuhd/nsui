package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSColorList — an ordered key-to-NSColor list (color panel / catalogs).
/// Thin stateless wrapper: each method maps to one objc_msgSend selector.
/// OMITTED: insertColor:key:atIndex: — its (VOID,ID,ID,INT) shape is NOT in
/// the Sig vocabulary (only the ID-returning ID,ID,ID,INT cousin is); and
/// writeToURL:error: — NSError out-param marshalling is out of scope here.
public final class NSColorList extends NSObject {

    private record Handles(MethodHandle hInitName, MethodHandle hSetColor) {}
    private static volatile Handles handles;

    private NSColorList(MemorySegment peer) {
        super(peer);
        ensureInit();
    }
    /// Wrap an existing peer (nil-safe).
    public static NSColorList wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSColorList(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)));
    }
    /// availableColorLists — user lists plus runtime additions (class property).
    public static NSArray availableColorLists() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(ObjC.cls("NSColorList"), ObjC.sel("availableColorLists")));
    }
    /// colorListNamed: (nil when absent).
    public static NSColorList colorListNamed(String name) {
        ensureInit();
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSColorList"), ObjC.sel("colorListNamed:"),
                name == null ? MemorySegment.NULL : ObjC.nsstring(name)));
    }
    /// [[NSColorList alloc] initWithName:] (does not join availableColorLists
    /// until saved; pass empty string for an unnamed list).
    public static NSColorList create(String name) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSColorList"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitName().invokeExact(p, ObjC.sel("initWithName:"),
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)));
        } catch (Throwable t) {
            throw new RuntimeException("initWithName: failed for NSColorList", t);
        }
        if (p.address() == 0) throw new IllegalStateException("NSColorList alloc/initWithName: returned nil");
        return new NSColorList(p);
    }

    /// name (nil for unnamed lists).
    public String name() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("name")));
    }

    /// setColor:forKey: (appends when the key is new).
    public void setColor(NSColor color, String key) {
        ensureInit();
        try {
            handles.hSetColor().invokeExact(peer, ObjC.sel("setColor:forKey:"),
                    (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()),
                    (MemorySegment) (key == null ? MemorySegment.NULL : ObjC.nsstring(key)));
        } catch (Throwable t) {
            throw new RuntimeException("setColor:forKey: failed", t);
        }
    }

    /// removeColorWithKey: (no-op when absent).
    public void removeColorWithKey(String key) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeColorWithKey:"),
                key == null ? MemorySegment.NULL : ObjC.nsstring(key));
    }

    /// colorWithKey: (nil when absent).
    public NSColor colorWithKey(String key) {
        return NSColor.wrap(ObjC.msgSendIdId(peer, ObjC.sel("colorWithKey:"),
                key == null ? MemorySegment.NULL : ObjC.nsstring(key)));
    }

    /// allKeys — colors in insertion order.
    public NSArray allKeys() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("allKeys")));
    }

    /// isEditable — depends on the list's source file.
    public boolean isEditable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEditable"));
    }
}
