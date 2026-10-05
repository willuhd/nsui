package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSUserDefaults (Foundation) — the app preferences domain: typed scalar
/// and string accessors plus removal. Thin stateless wrapper.
public final class NSUserDefaults extends NSObject {

    private record Handles(MethodHandle hIntId, MethodHandle hDoubleId, MethodHandle hBoolId,
            MethodHandle hSetObj, MethodHandle hSetInt, MethodHandle hSetDouble, MethodHandle hSetBool) {}
    private static volatile Handles handles;

    private NSUserDefaults(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSUserDefaults wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSUserDefaults(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL, Arg.ID)));
    }

    /// standardUserDefaults.
    public static NSUserDefaults standard() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("NSUserDefaults"), ObjC.sel("standardUserDefaults")));
    }

    private static MemorySegment key(String name) {
        return ObjC.nsstring(name);
    }

    /// stringForKey: (nil-safe).
    public String stringForKey(String name) {
        return ObjC.toString(ObjC.msgSendIdId(peer, ObjC.sel("stringForKey:"), key(name)));
    }

    /// integerForKey:.
    public long integerForKey(String name) {
        try {
            return (long) handles.hIntId().invokeExact(peer, ObjC.sel("integerForKey:"), key(name));
        } catch (Throwable t) {
            throw new RuntimeException("integerForKey: failed", t);
        }
    }

    /// doubleForKey:.
    public double doubleForKey(String name) {
        try {
            return (double) handles.hDoubleId().invokeExact(peer, ObjC.sel("doubleForKey:"), key(name));
        } catch (Throwable t) {
            throw new RuntimeException("doubleForKey: failed", t);
        }
    }

    /// boolForKey:.
    public boolean boolForKey(String name) {
        try {
            return (boolean) handles.hBoolId().invokeExact(peer, ObjC.sel("boolForKey:"), key(name));
        } catch (Throwable t) {
            throw new RuntimeException("boolForKey: failed", t);
        }
    }

    /// setObject:forKey: (NSString value).
    public void setStringForKey(String value, String name) {
        try {
            handles.hSetObj().invokeExact(peer, ObjC.sel("setObject:forKey:"), ObjC.nsstring(value), key(name));
        } catch (Throwable t) {
            throw new RuntimeException("setObject:forKey: failed", t);
        }
    }

    /// setInteger:forKey:.
    public void setIntegerForKey(long value, String name) {
        try {
            handles.hSetInt().invokeExact(peer, ObjC.sel("setInteger:forKey:"), value, key(name));
        } catch (Throwable t) {
            throw new RuntimeException("setInteger:forKey: failed", t);
        }
    }

    /// setDouble:forKey:.
    public void setDoubleForKey(double value, String name) {
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setDouble:forKey:"), value, key(name));
        } catch (Throwable t) {
            throw new RuntimeException("setDouble:forKey: failed", t);
        }
    }

    /// setBool:forKey:.
    public void setBoolForKey(boolean value, String name) {
        try {
            handles.hSetBool().invokeExact(peer, ObjC.sel("setBool:forKey:"), value, key(name));
        } catch (Throwable t) {
            throw new RuntimeException("setBool:forKey: failed", t);
        }
    }

    /// removeObjectForKey:.
    public void removeObjectForKey(String name) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeObjectForKey:"), key(name));
    }
}
