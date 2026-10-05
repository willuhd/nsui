package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSWorkspace — minimal wrapper over AppKit NSWorkspace.
/// Provides openURL, iconForFile, runningApplications.
/// OMITTED: block-taking opens/recycles/duplicates/setDefault* (completionHandler blocks
/// have no registered Sig shape here); getFileSystemInfoForPath:... (multi out-params,
/// shape BOOL,ID,ID,ID,ID,ID,ID NOT in vocabulary — verified by grep);
/// setIcon:forFile:options: (shape BOOL,ID,ID,INT NOT in vocabulary); desktop-image
/// set/desktopImageURLForScreen:error:/options (NSError** + dict); authorization;
/// deprecated launch/open/type helpers beyond openFile/launchApplication/iconForFileType.
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
            MemorySegment u = ((MemorySegment) (url == null ? MemorySegment.NULL : url));
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

    /// notificationCenter.
    public MemorySegment notificationCenter() {
        return ObjC.msgSendId(peer, ObjC.sel("notificationCenter"));
    }

    /// selectFile:inFileViewerRootedAtPath:.
    public boolean selectFileInViewer(String fullPath, String rootPath) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("selectFile:inFileViewerRootedAtPath:"),
                    (MemorySegment) (fullPath == null ? MemorySegment.NULL : ObjC.nsstring(fullPath)),
                    (MemorySegment) (rootPath == null ? MemorySegment.NULL : ObjC.nsstring(rootPath)));
        } catch (Throwable t) { throw new RuntimeException("selectFile:inFileViewerRootedAtPath: failed", t); }
    }

    /// activateFileViewerSelectingURLs: (NSArray of NSURL).
    public void activateFileViewerSelectingURLs(NSArray urls) {
        ObjC.msgSendVoidId(peer, ObjC.sel("activateFileViewerSelectingURLs:"),
                (MemorySegment) (urls == null ? MemorySegment.NULL : urls.peer()));
    }

    /// showSearchResultsForQueryString:.
    public boolean showSearchResultsForQueryString(String query) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer,
                    ObjC.sel("showSearchResultsForQueryString:"),
                    (MemorySegment) (query == null ? MemorySegment.NULL : ObjC.nsstring(query)));
        } catch (Throwable t) { throw new RuntimeException("showSearchResultsForQueryString: failed", t); }
    }

    /// noteFileSystemChanged:.
    public void noteFileSystemChanged(String path) {
        ObjC.msgSendVoidId(peer, ObjC.sel("noteFileSystemChanged:"),
                (MemorySegment) (path == null ? MemorySegment.NULL : ObjC.nsstring(path)));
    }

    /// isFilePackageAtPath:.
    public boolean isFilePackageAtPath(String path) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer,
                    ObjC.sel("isFilePackageAtPath:"),
                    (MemorySegment) (path == null ? MemorySegment.NULL : ObjC.nsstring(path)));
        } catch (Throwable t) { throw new RuntimeException("isFilePackageAtPath: failed", t); }
    }

    /// iconForFiles: (NSArray of NSString).
    public NSImage iconForFiles(NSArray paths) {
        try {
            MemorySegment img = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("iconForFiles:"), (MemorySegment) (paths == null ? MemorySegment.NULL : paths.peer()));
            return NSImage.wrap(img);
        } catch (Throwable t) { throw new RuntimeException("iconForFiles: failed", t); }
    }

    /// iconForContentType: (UTType as generic id).
    public NSImage iconForContentType(MemorySegment contentType) {
        try {
            MemorySegment img = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("iconForContentType:"), (MemorySegment) (contentType == null ? MemorySegment.NULL : contentType));
            return NSImage.wrap(img);
        } catch (Throwable t) { throw new RuntimeException("iconForContentType: failed", t); }
    }

    /// fileLabels / fileLabelColors.
    public NSArray fileLabels() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("fileLabels")));
    }
    public NSArray fileLabelColors() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("fileLabelColors")));
    }

    /// hideOtherApplications.
    public void hideOtherApplications() {
        ObjC.msgSendVoid(peer, ObjC.sel("hideOtherApplications"));
    }

    /// frontmostApplication / menuBarOwningApplication.
    public NSRunningApplication frontmostApplication() {
        return NSRunningApplication.wrap(ObjC.msgSendId(peer, ObjC.sel("frontmostApplication")));
    }
    public NSRunningApplication menuBarOwningApplication() {
        return NSRunningApplication.wrap(ObjC.msgSendId(peer, ObjC.sel("menuBarOwningApplication")));
    }

    /// URLForApplicationWithBundleIdentifier: (NSURL as generic id).
    public MemorySegment URLForApplicationWithBundleIdentifier(String bundleId) {
        return ObjC.msgSendIdId(peer, ObjC.sel("URLForApplicationWithBundleIdentifier:"),
                (MemorySegment) (bundleId == null ? MemorySegment.NULL : ObjC.nsstring(bundleId)));
    }

    /// URLsForApplicationsWithBundleIdentifier:.
    public NSArray URLsForApplicationsWithBundleIdentifier(String bundleId) {
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("URLsForApplicationsWithBundleIdentifier:"),
                (MemorySegment) (bundleId == null ? MemorySegment.NULL : ObjC.nsstring(bundleId))));
    }

    /// URLForApplicationToOpenURL: (NSURL in/out as generic id).
    public MemorySegment URLForApplicationToOpenURL(MemorySegment url) {
        return ObjC.msgSendIdId(peer, ObjC.sel("URLForApplicationToOpenURL:"),
                (MemorySegment) (url == null ? MemorySegment.NULL : url));
    }

    /// URLsForApplicationsToOpenURL:.
    public NSArray URLsForApplicationsToOpenURL(MemorySegment url) {
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("URLsForApplicationsToOpenURL:"),
                (MemorySegment) (url == null ? MemorySegment.NULL : url)));
    }

    /// URLForApplicationToOpenContentType: (UTType as generic id).
    public MemorySegment URLForApplicationToOpenContentType(MemorySegment contentType) {
        return ObjC.msgSendIdId(peer, ObjC.sel("URLForApplicationToOpenContentType:"),
                (MemorySegment) (contentType == null ? MemorySegment.NULL : contentType));
    }

    /// URLsForApplicationsToOpenContentType:.
    public NSArray URLsForApplicationsToOpenContentType(MemorySegment contentType) {
        return NSArray.wrap(ObjC.msgSendIdId(peer, ObjC.sel("URLsForApplicationsToOpenContentType:"),
                (MemorySegment) (contentType == null ? MemorySegment.NULL : contentType)));
    }

    /// unmountAndEjectDeviceAtPath:.
    public boolean unmountAndEjectDeviceAtPath(String path) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer,
                    ObjC.sel("unmountAndEjectDeviceAtPath:"),
                    (MemorySegment) (path == null ? MemorySegment.NULL : ObjC.nsstring(path)));
        } catch (Throwable t) { throw new RuntimeException("unmountAndEjectDeviceAtPath: failed", t); }
    }

    /// unmountAndEjectDeviceAtURL: (NULL error variant; NSError** passed as NULL).
    public boolean unmountAndEjectDeviceAtURL(MemorySegment url) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("unmountAndEjectDeviceAtURL:error:"),
                    (MemorySegment) (url == null ? MemorySegment.NULL : url), MemorySegment.NULL);
        } catch (Throwable t) { throw new RuntimeException("unmountAndEjectDeviceAtURL:error: failed", t); }
    }

    /// extendPowerOffBy:.
    public long extendPowerOffBy(long requested) {
        try {
            return (long) ObjC.handle(Sig.of(Ret.INT, Arg.INT)).invokeExact(peer,
                    ObjC.sel("extendPowerOffBy:"), requested);
        } catch (Throwable t) { throw new RuntimeException("extendPowerOffBy: failed", t); }
    }
}
