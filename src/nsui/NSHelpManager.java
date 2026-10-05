package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSHelpManager — Help Viewer access plus per-object context-help strings.
/// Thin wrapper: each method is one objc_msgSend. Object parameters take
/// NSObject (peer extracted, null ok); help strings are NSAttributedString.
/// OMITTED: showContextHelpForObject:locationHint: — popping visible help UI
/// is out of scope for this tight wrapper; everything else is covered.
public final class NSHelpManager extends NSObject {

    private record Handles(MethodHandle hVoidIdId, MethodHandle hBoolId) {}
    private static volatile Handles handles;
    private NSHelpManager(MemorySegment peer) { super(peer); ensureInit(); }

    /// Wrap an existing peer (nil-safe).
    public static NSHelpManager wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSHelpManager(peer);
    }
    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
    }
    /// sharedHelpManager — the singleton.
    public static NSHelpManager sharedHelpManager() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("NSHelpManager"), ObjC.sel("sharedHelpManager")));
    }
    /// isContextHelpModeActive / setter (class property).
    public static boolean isContextHelpModeActive() {
        ensureInit();
        return ObjC.msgSendBool(ObjC.cls("NSHelpManager"), ObjC.sel("isContextHelpModeActive"));
    }
    public static void setContextHelpModeActive(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(ObjC.cls("NSHelpManager"), ObjC.sel("setContextHelpModeActive:"), flag);
    }
    private void setTwo(String sel, MemorySegment a1, MemorySegment a2) {
        ensureInit();
        try {
            handles.hVoidIdId().invokeExact(peer, ObjC.sel(sel), a1, a2);
        } catch (Throwable t) { throw new RuntimeException(sel + " failed", t); }
    }
    /// setContextHelp:forObject:.
    public void setContextHelp(NSAttributedString help, NSObject object) {
        setTwo("setContextHelp:forObject:", ObjC.nullablePeer(help), ObjC.nullablePeer(object));
    }
    /// removeContextHelpForObject:.
    public void removeContextHelpForObject(NSObject object) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeContextHelpForObject:"), ObjC.nullablePeer(object));
    }
    /// contextHelpForObject: (nil when none registered).
    public NSAttributedString contextHelpForObject(NSObject object) {
        return NSAttributedString.wrap(ObjC.msgSendIdId(peer, ObjC.sel("contextHelpForObject:"), ObjC.nullablePeer(object)));
    }
    /// openHelpAnchor:inBook: (nil book ok; opens Help Viewer).
    public void openHelpAnchor(String anchor, String book) {
        setTwo("openHelpAnchor:inBook:", ObjC.nullable(anchor == null ? null : ObjC.nsstring(anchor)),
                ObjC.nullable(book == null ? null : ObjC.nsstring(book)));
    }
    /// findString:inBook: (nil book ok; opens Help Viewer).
    public void findString(String query, String book) {
        setTwo("findString:inBook:", ObjC.nullable(query == null ? null : ObjC.nsstring(query)),
                ObjC.nullable(book == null ? null : ObjC.nsstring(book)));
    }
    /// registerBooksInBundle: — register help books in a plugin bundle.
    public boolean registerBooksInBundle(NSBundle bundle) {
        ensureInit();
        try {
            return (boolean) handles.hBoolId().invokeExact(peer, ObjC.sel("registerBooksInBundle:"), ObjC.nullablePeer(bundle));
        } catch (Throwable t) { throw new RuntimeException("registerBooksInBundle: failed", t); }
    }
}
