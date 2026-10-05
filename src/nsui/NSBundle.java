package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSBundle — a bundled directory of code and resources (Foundation).
/// Thin stateless wrapper. For a bare single binary, mainBundle answers from
/// the embedded __TEXT,__info_plist section when present.
public final class NSBundle extends NSObject {

    private NSBundle(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static NSBundle wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSBundle(peer);
    }

    /// mainBundle — this process's bundle.
    public static NSBundle mainBundle() {
        return wrap(ObjC.msgSendId(ObjC.cls("NSBundle"), ObjC.sel("mainBundle")));
    }

    /// bundleWithPath:.
    public static NSBundle bundleWithPath(String path) {
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSBundle"), ObjC.sel("bundleWithPath:"), ObjC.nsstring(path)));
    }

    /// bundleWithIdentifier:.
    public static NSBundle bundleWithIdentifier(String identifier) {
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSBundle"), ObjC.sel("bundleWithIdentifier:"), ObjC.nsstring(identifier)));
    }

    /// bundlePath (nil-safe).
    public String bundlePath() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("bundlePath")));
    }

    /// bundleIdentifier (nil-safe; nil without bundle metadata).
    public String bundleIdentifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("bundleIdentifier")));
    }
}
