package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSWorkspace — minimal wrapper over AppKit NSWorkspace.
/// Provides openURL, iconForFile, runningApplications.
public final class NSWorkspace extends NSObject {

            private record Handles(MethodHandle hShared, MethodHandle hOpenURL, MethodHandle hIconFile) {}
    private static volatile Handles handles;

    private NSWorkspace(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSWorkspace wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSWorkspace(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)), ObjC.handle(Sig.of(Ret.ID, Arg.ID)));
    }

    /// [NSWorkspace sharedWorkspace]
    public static NSWorkspace sharedWorkspace() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hShared().invokeExact(ObjC.cls("NSWorkspace"), ObjC.sel("sharedWorkspace"));
            if (p == null || p.address() == 0) throw new IllegalStateException("sharedWorkspace returned nil");
            return new NSWorkspace(p);
        } catch (Throwable t) {
            throw new RuntimeException("sharedWorkspace failed", t);
        }
    }

    /// [workspace openURL:] — URL string -> BOOL
    public boolean openURL(String urlString) {
        ensureInit();
        // Build NSURL via NSURL URLWithString:
        MemorySegment url = ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("URLWithString:"), ObjC.nsstring(urlString));
        if (url == null || url.address() == 0) {
            // try fileURLWithPath:
            url = ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("fileURLWithPath:"), ObjC.nsstring(urlString));
        }
        try {
            return (boolean) handles.hOpenURL().invokeExact(peer, ObjC.sel("openURL:"), url);
        } catch (Throwable t) {
            throw new RuntimeException("openURL: failed", t);
        }
    }

    /// [workspace openURL:] with MemorySegment NSURL
    public boolean openURL(MemorySegment url) {
        ensureInit();
        try {
            MemorySegment u = (url == null ? MemorySegment.NULL : url);
            return (boolean) handles.hOpenURL().invokeExact(peer, ObjC.sel("openURL:"), u);
        } catch (Throwable t) {
            throw new RuntimeException("openURL: failed", t);
        }
    }

    /// [workspace iconForFile:] -> NSImage
    public NSImage iconForFile(String fullPath) {
        ensureInit();
        try {
            MemorySegment img = (MemorySegment) handles.hIconFile().invokeExact(peer, ObjC.sel("iconForFile:"), ObjC.nsstring(fullPath));
            return NSImage.wrap(img);
        } catch (Throwable t) {
            throw new RuntimeException("iconForFile: failed", t);
        }
    }

    /// [workspace iconForFileType:] -> NSImage
    public NSImage iconForFileType(String fileType) {
        ensureInit();
        try {
            MemorySegment img = (MemorySegment) handles.hIconFile().invokeExact(peer, ObjC.sel("iconForFileType:"), ObjC.nsstring(fileType));
            return NSImage.wrap(img);
        } catch (Throwable t) {
            throw new RuntimeException("iconForFileType: failed", t);
        }
    }

    /// [workspace runningApplications] -> NSArray of NSRunningApplication
    public NSArray runningApplications() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) handles.hShared().invokeExact(peer, ObjC.sel("runningApplications"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("runningApplications failed", t);
        }
    }

    /// [workspace openFile:]
    public boolean openFile(String fullPath) {
        ensureInit();
        try {
            return (boolean) handles.hOpenURL().invokeExact(peer, ObjC.sel("openFile:"), ObjC.nsstring(fullPath));
        } catch (Throwable t) {
            throw new RuntimeException("openFile: failed", t);
        }
    }

    /// [workspace launchApplication:] -> BOOL
    public boolean launchApplication(String appName) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("launchApplication:"), ObjC.nsstring(appName));
        } catch (Throwable t) {
            throw new RuntimeException("launchApplication: failed", t);
        }
    }
}
