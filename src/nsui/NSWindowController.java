package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSWindowController — thin 1:1 wrapper over AppKit `NSWindowController`:
/// the controller owns (or lazily loads) a window, tracks its document, and
/// carries the nib identity plus a few window-management actions.
///
/// Coverage notes (SDK: `NSWindowController.h`):
/// - Wrapped: `window`/`setWindow:`, `windowNibName`, `windowNibPath`, `owner`,
///   `windowFrameAutosaveName`, `shouldCascadeWindows`,
///   `previewRepresentableActivityItems` (macOS 13.2+), `document`/`setDocument:`,
///   `setDocumentEdited:`, `shouldCloseDocument`, `synchronizeWindowTitleWithDocumentName`,
///   `windowTitleForDocumentDisplayName:`, `contentViewController`/`setContentViewController:`,
///   `isWindowLoaded`, `windowWillLoad`, `windowDidLoad`, `loadWindow`, `close`,
///   `showWindow:`, `dismissController:`, `storyboard`.
/// - OMITTED: `initWithWindowNibName:` / `initWithWindowNibName:owner:` /
///   `initWithWindowNibPath:owner:` — nib-loading initializers (init overloads;
///   need a nib in a bundle, untestable headless); `initWithCoder:` (coder init);
///   `NSSeguePerforming` protocol methods (need upcall machinery);
///   storyboard segue presentation (needs storyboards).
///   No new Sig shape is requested for this file.
public final class NSWindowController extends NSObject {

            private record Handles(MethodHandle hInitWindow, MethodHandle hId, MethodHandle hVoidId, MethodHandle hIdId) {}
    private static volatile Handles handles;

    private NSWindowController(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSWindowController wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSWindowController(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.ID, Arg.ID)), ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.VOID, Arg.ID)), ObjC.handle(Sig.of(Ret.ID, Arg.ID)));
    }

    /// alloc + init — empty controller.
    public static NSWindowController create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSWindowController"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p == null || p.address() == 0) throw new IllegalStateException("NSWindowController alloc/init returned nil");
        return new NSWindowController(p);
    }

    /// alloc + initWithWindow:
    public static NSWindowController initWithWindow(NSWindow window) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSWindowController"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitWindow().invokeExact(p, ObjC.sel("initWithWindow:"), (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("initWithWindow: failed", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSWindowController initWithWindow: returned nil");
        return new NSWindowController(p);
    }

    /// window — NSWindow or null.
    public NSWindow window() {
        ensureInit();
        try {
            MemorySegment w = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("window"));
            return NSWindow.wrap(w);
        } catch (Throwable t) {
            throw new RuntimeException("window failed", t);
        }
    }

    /// setWindow:
    public void setWindow(NSWindow window) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setWindow:"), (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setWindow: failed", t);
        }
    }

    /// showWindow: — sender may be null.
    public void showWindow(NSObject sender) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("showWindow:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("showWindow: failed", t);
        }
    }

    /// showWindow: convenience with null sender.
    public void showWindow() {
        showWindow(null);
    }

    /// setDocument: — NSDocument.
    public void setDocument(NSDocument document) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setDocument:"), (MemorySegment) (document == null ? MemorySegment.NULL : document.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setDocument: failed", t);
        }
    }

    /// document — raw id.
    public MemorySegment documentPeer() {
        ensureInit();
        try {
            return (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("document"));
        } catch (Throwable t) {
            throw new RuntimeException("document failed", t);
        }
    }

    public NSDocument document() {
        MemorySegment d = documentPeer();
        return NSDocument.wrap(d);
    }

    /// isWindowLoaded
    public boolean isWindowLoaded() {
        return ObjC.msgSendBool(peer, ObjC.sel("isWindowLoaded"));
    }

    // ---- nib identity ----

    /// [controller windowNibName] — nil unless built from a nib; may be nil.
    public String windowNibName() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("windowNibName"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("windowNibName failed", t);
        }
    }

    /// [controller windowNibPath] — full nib path, or nil when not nib-based.
    public String windowNibPath() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("windowNibPath"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("windowNibPath failed", t);
        }
    }

    /// [controller owner] — the nib file's owner (weak, usually the controller
    /// itself or its document); raw peer, may be nil.
    public MemorySegment owner() {
        ensureInit();
        try {
            return (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("owner"));
        } catch (Throwable t) {
            throw new RuntimeException("owner failed", t);
        }
    }

    /// [controller windowFrameAutosaveName] — may be nil/empty.
    public String windowFrameAutosaveName() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("windowFrameAutosaveName"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("windowFrameAutosaveName failed", t);
        }
    }

    /// [controller setWindowFrameAutosaveName:] — applied to the window when set.
    /// AppKit honesty: nil is REJECTED (`NSInternalInconsistencyException`,
    /// aborts the JVM); a Java null is mapped to `""` (no autosave).
    public void setWindowFrameAutosaveName(String name) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setWindowFrameAutosaveName:"),
                    ObjC.nsstring(name == null ? "" : name));
        } catch (Throwable t) {
            throw new RuntimeException("setWindowFrameAutosaveName: failed", t);
        }
    }

    /// [controller shouldCascadeWindows] / [controller setShouldCascadeWindows:].
    public boolean shouldCascadeWindows() {
        return ObjC.msgSendBool(peer, ObjC.sel("shouldCascadeWindows"));
    }

    /// [controller setShouldCascadeWindows:].
    public void setShouldCascadeWindows(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setShouldCascadeWindows:"), flag);
    }

    /// [controller previewRepresentableActivityItems] (macOS 13.2+) — sharing
    /// items; nil falls back to the document's items. May be nil.
    public NSArray previewRepresentableActivityItems() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) handles.hId().invokeExact(peer,
                    ObjC.sel("previewRepresentableActivityItems"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("previewRepresentableActivityItems failed", t);
        }
    }

    /// [controller setPreviewRepresentableActivityItems:] — nil restores fallback.
    public void setPreviewRepresentableActivityItems(NSArray items) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setPreviewRepresentableActivityItems:"),
                    (MemorySegment) (items == null ? MemorySegment.NULL : items.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setPreviewRepresentableActivityItems: failed", t);
        }
    }

    // ---- document tracking ----

    /// [controller setDocumentEdited:] — mark dirty/clean (forwards to the window).
    public void setDocumentEdited(boolean dirtyFlag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDocumentEdited:"), dirtyFlag);
    }

    /// [controller shouldCloseDocument] / [controller setShouldCloseDocument:].
    public boolean shouldCloseDocument() {
        return ObjC.msgSendBool(peer, ObjC.sel("shouldCloseDocument"));
    }

    /// [controller setShouldCloseDocument:].
    public void setShouldCloseDocument(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setShouldCloseDocument:"), flag);
    }

    /// [controller synchronizeWindowTitleWithDocumentName] — refresh the window
    /// title from the document's display name. No-op without a document.
    public void synchronizeWindowTitleWithDocumentName() {
        ObjC.msgSendVoid(peer, ObjC.sel("synchronizeWindowTitleWithDocumentName"));
    }

    /// [controller windowTitleForDocumentDisplayName:] — override point for the
    /// title mapping; the default returns `displayName` as-is.
    public String windowTitleForDocumentDisplayName(String displayName) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hIdId().invokeExact(peer,
                    ObjC.sel("windowTitleForDocumentDisplayName:"), ObjC.nsstring(displayName));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("windowTitleForDocumentDisplayName: failed", t);
        }
    }

    // ---- content ----

    /// [controller contentViewController] (macOS 10.10+) — may be nil.
    public NSViewController contentViewController() {
        ensureInit();
        try {
            MemorySegment vc = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("contentViewController"));
            return NSViewController.wrap(vc);
        } catch (Throwable t) {
            throw new RuntimeException("contentViewController failed", t);
        }
    }

    /// [controller setContentViewController:] — nil detaches.
    public void setContentViewController(NSViewController controller) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setContentViewController:"),
                    (MemorySegment) (controller == null ? MemorySegment.NULL : controller.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setContentViewController: failed", t);
        }
    }

    // ---- nib loading / lifecycle ----

    /// [controller windowWillLoad] — override point before nib load.
    public void windowWillLoad() {
        ObjC.msgSendVoid(peer, ObjC.sel("windowWillLoad"));
    }

    /// [controller windowDidLoad] — override point after nib load.
    public void windowDidLoad() {
        ObjC.msgSendVoid(peer, ObjC.sel("windowDidLoad"));
    }

    /// [controller loadWindow] — force nib load (do not call directly in
    /// production; prefer `window()`). Exposed for completeness.
    public void loadWindow() {
        ObjC.msgSendVoid(peer, ObjC.sel("loadWindow"));
    }

    /// [controller close] — close the controller (and its document when last).
    public void close() {
        ObjC.msgSendVoid(peer, ObjC.sel("close"));
    }

    /// [controller dismissController:] (macOS 10.10+) — dismiss if presented;
    /// sender may be null. No-op when not presented.
    public void dismissController(NSObject sender) {
        ensureInit();
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("dismissController:"),
                    (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("dismissController: failed", t);
        }
    }

    /// [controller storyboard] (macOS 10.10+) — raw peer, nil unless loaded
    /// from a storyboard (no NSStoryboard wrapper yet).
    public MemorySegment storyboard() {
        ensureInit();
        try {
            return (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("storyboard"));
        } catch (Throwable t) {
            throw new RuntimeException("storyboard failed", t);
        }
    }
}
