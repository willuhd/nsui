package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSRunningApplication — a running process as seen by AppKit: identity,
/// state flags, activation policy. Thin stateless wrapper.
/// Nothing omitted: every NSRunningApplication header member is wrapped below.
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

    /// ownsMenuBar.
    public boolean ownsMenuBar() {
        return ObjC.msgSendBool(peer, ObjC.sel("ownsMenuBar"));
    }

    /// bundleURL (nil when the app has no bundle; NSURL as generic id).
    public MemorySegment bundleURL() {
        return ObjC.msgSendId(peer, ObjC.sel("bundleURL"));
    }

    /// executableURL (NSURL as generic id).
    public MemorySegment executableURL() {
        return ObjC.msgSendId(peer, ObjC.sel("executableURL"));
    }

    /// launchDate (nil when unavailable; NSDate as generic id).
    public MemorySegment launchDate() {
        return ObjC.msgSendId(peer, ObjC.sel("launchDate"));
    }

    /// icon (nil-safe).
    public NSImage icon() {
        return NSImage.wrap(ObjC.msgSendId(peer, ObjC.sel("icon")));
    }

    /// executableArchitecture (NSBundleExecutableArchitecture).
    public long executableArchitecture() {
        return ObjC.msgSendLong(peer, ObjC.sel("executableArchitecture"));
    }

    /// hide — request hiding (YES when the request was sent).
    public boolean hide() {
        return ObjC.msgSendBool(peer, ObjC.sel("hide"));
    }

    /// unhide — request unhiding (YES when the request was sent).
    public boolean unhide() {
        return ObjC.msgSendBool(peer, ObjC.sel("unhide"));
    }

    /// activateWithOptions: (NSApplicationActivationOptions).
    public boolean activateWithOptions(long options) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.INT)).invokeExact(peer, ObjC.sel("activateWithOptions:"), options);
        } catch (Throwable t) { throw new RuntimeException("activateWithOptions: failed", t); }
    }

    /// activateFromApplication:options: (macOS 14+).
    public boolean activateFromApplication(NSRunningApplication app, long options) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.INT)).invokeExact(peer,
                    ObjC.sel("activateFromApplication:options:"),
                    (MemorySegment) (app == null ? MemorySegment.NULL : app.peer()), options);
        } catch (Throwable t) { throw new RuntimeException("activateFromApplication:options: failed", t); }
    }

    /// terminate — request normal quit (YES when the request was sent).
    public boolean terminate() {
        return ObjC.msgSendBool(peer, ObjC.sel("terminate"));
    }

    /// forceTerminate — request forced quit (YES when the request was sent).
    public boolean forceTerminate() {
        return ObjC.msgSendBool(peer, ObjC.sel("forceTerminate"));
    }

    /// runningApplicationsWithBundleIdentifier: (empty array when none match).
    public static NSArray runningApplicationsWithBundleIdentifier(String bundleIdentifier) {
        try {
            MemorySegment p = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID)).invokeExact(
                    ObjC.cls("NSRunningApplication"), ObjC.sel("runningApplicationsWithBundleIdentifier:"),
                    (MemorySegment) (bundleIdentifier == null ? MemorySegment.NULL : ObjC.nsstring(bundleIdentifier)));
            return NSArray.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("runningApplicationsWithBundleIdentifier: failed", t); }
    }

    /// runningApplicationWithProcessIdentifier: (nil when no app has that pid).
    public static NSRunningApplication runningApplicationWithProcessIdentifier(long pid) {
        try {
            MemorySegment p = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.INT)).invokeExact(
                    ObjC.cls("NSRunningApplication"), ObjC.sel("runningApplicationWithProcessIdentifier:"), pid);
            return wrap(p);
        } catch (Throwable t) { throw new RuntimeException("runningApplicationWithProcessIdentifier: failed", t); }
    }

    /// terminateAutomaticallyTerminableApplications.
    public static void terminateAutomaticallyTerminableApplications() {
        ObjC.msgSendVoid(ObjC.cls("NSRunningApplication"), ObjC.sel("terminateAutomaticallyTerminableApplications"));
    }
}
