package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSRunningApplication — a running process as seen by AppKit: identity,
/// state flags, activation policy. Thin stateless wrapper.
public final class NSRunningApplication extends NSObject {

    private NSRunningApplication(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static NSRunningApplication wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSRunningApplication(peer);
    }

    /// currentApplication — this process.
    public static NSRunningApplication current() {
        return wrap(ObjC.msgSendId(ObjC.cls("NSRunningApplication"), ObjC.sel("currentApplication")));
    }

    /// isTerminated.
    public boolean isTerminated() {
        return ObjC.msgSendBool(peer, ObjC.sel("isTerminated"));
    }

    /// isFinishedLaunching.
    public boolean isFinishedLaunching() {
        return ObjC.msgSendBool(peer, ObjC.sel("isFinishedLaunching"));
    }

    /// isHidden.
    public boolean isHidden() {
        return ObjC.msgSendBool(peer, ObjC.sel("isHidden"));
    }

    /// isActive.
    public boolean isActive() {
        return ObjC.msgSendBool(peer, ObjC.sel("isActive"));
    }

    /// activationPolicy (NSApplicationActivationPolicy).
    public long activationPolicy() {
        return ObjC.msgSendLong(peer, ObjC.sel("activationPolicy"));
    }

    /// localizedName (nil-safe).
    public String localizedName() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("localizedName")));
    }

    /// bundleIdentifier (nil-safe; nil for processes without a bundle).
    public String bundleIdentifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("bundleIdentifier")));
    }

    /// processIdentifier (pid_t).
    public long processIdentifier() {
        return ObjC.msgSendLong(peer, ObjC.sel("processIdentifier"));
    }
}
