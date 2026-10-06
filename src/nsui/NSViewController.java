package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSViewController — minimal wrapper over AppKit's NSViewController.
/// Thin 1:1 wrapper; holds the content view for NSPopover and other containers.
///
/// Typical status-popover usage (shown directly from status item click):
/// ```
///   NSStatusItem item = NSStatusBar.systemStatusBar().statusItem();
///   item.setSFSymbol("magnifyingglass"); // or "star.fill" via NSImage.imageNamed
///   NSView content = NSView.create(new NSRect(0,0,280,140), (ctx, dirty)->{});
///   NSPopover pop = NSPopover.create();
///   pop.setContentView(content); // wraps in an NSViewController
///   item.attachPopover(pop);     // target/action on statusItem button toggles popover
/// ```
/// Explicitly: `button().setImage(NSImage.imageNamed("magnifyingglass"))` for SF Symbols.
public class NSViewController extends NSObject {

            private record Handles(MethodHandle hSetView, MethodHandle hGetView, MethodHandle hGetSize, MethodHandle hSetSize, MethodHandle hVoidIdInt, MethodHandle hGetPoint, MethodHandle hSetPoint) {}
    private static volatile Handles handles;

    protected NSViewController(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.ID)), ObjC.handle(Sig.of(Ret.ID)), ObjC.handle(Sig.of(Ret.SIZE)), ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)), ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)), ObjC.handle(Sig.of(Ret.POINT)), ObjC.handle(Sig.of(Ret.VOID, Arg.POINT)));
    }

    public static NSViewController wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSViewController(peer);
    }

    /// alloc + init
    public static NSViewController create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSViewController"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p == null || p.address() == 0) throw new IllegalStateException("NSViewController alloc/init returned nil");
        return new NSViewController(p);
    }

    /// view — the controller's view (may be nil if not loaded).
    public NSView view() {
        ensureInit();
        try {
            MemorySegment v = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("view"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("view failed", t);
        }
    }

    /// setView:
    public void setView(NSView view) {
        ensureInit();
        try {
            MemorySegment p = (view == null ? MemorySegment.NULL : view.peer());
            handles.hSetView().invokeExact(peer, ObjC.sel("setView:"), p);
        } catch (Throwable t) {
            throw new RuntimeException("setView: failed", t);
        }
    }

    /// Convenience: create with a view already set.
    public static NSViewController withView(NSView view) {
        NSViewController vc = create();
        vc.setView(view);
        return vc;
    }

    // ---------------------------------------------------------------- header-completeness batch (NSViewController.h)
    //
    // Omitted:
    // - init overloads initWithNibName:bundle: and initWithCoder: — covered by create().
    // - block-taking: transitionFromViewController:toViewController:options:completionHandler:,
    //   presentViewController:asPopoverRelativeToRect:... hasFullSizeContent: is also omitted (see below).
    // - inexpressible shapes (absent from Sig vocabulary, requested):
    //   presentViewController:asPopoverRelativeToRect:ofView:preferredEdge:behavior: needs
    //   of(VOID,ID,RECT,ID,INT,INT).
    // - animator/extension/storyboard accessors take plain ids — no upcall machinery needed to send them.
    // - NSEditorRegistration and segue/identification protocol methods live outside NSViewController.h.

    /// [vc representedObject] (may be nil).
    public NSObject representedObject() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("representedObject"));
            return NSObject.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("representedObject failed", t);
        }
    }

    /// [vc setRepresentedObject:] (nil clears).
    public void setRepresentedObject(NSObject object) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("setRepresentedObject:"), (MemorySegment) (object == null ? MemorySegment.NULL : object.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setRepresentedObject: failed", t);
        }
    }

    /// [vc title] (may be nil).
    public String title() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("title"));
            return ObjC.toString(p);
        } catch (Throwable t) {
            throw new RuntimeException("title failed", t);
        }
    }

    /// [vc setTitle:] (nil clears).
    public void setTitle(String title) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("setTitle:"), (MemorySegment) (title == null ? MemorySegment.NULL : ObjC.nsstring(title)));
        } catch (Throwable t) {
            throw new RuntimeException("setTitle: failed", t);
        }
    }

    /// [vc nibName] (may be nil).
    public String nibName() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("nibName"));
            return ObjC.toString(p);
        } catch (Throwable t) {
            throw new RuntimeException("nibName failed", t);
        }
    }

    /// [vc nibBundle] (may be nil; NSBundle has a dedicated wrapper).
    public NSBundle nibBundle() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("nibBundle"));
            return NSBundle.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("nibBundle failed", t);
        }
    }

    /// [vc viewIfLoaded] — the view or nil when not loaded (macOS 14+).
    public NSView viewIfLoaded() {
        ensureInit();
        try {
            MemorySegment v = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("viewIfLoaded"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("viewIfLoaded failed", t);
        }
    }

    /// [vc loadView].
    public void loadView() {
        ObjC.msgSendVoid(peer, ObjC.sel("loadView"));
    }

    /// [vc loadViewIfNeeded] (macOS 14+).
    public void loadViewIfNeeded() {
        ObjC.msgSendVoid(peer, ObjC.sel("loadViewIfNeeded"));
    }

    /// [vc viewDidLoad].
    public void viewDidLoad() {
        ObjC.msgSendVoid(peer, ObjC.sel("viewDidLoad"));
    }

    /// [vc isViewLoaded].
    public boolean isViewLoaded() {
        return ObjC.msgSendBool(peer, ObjC.sel("isViewLoaded"));
    }

    /// [vc viewWillAppear].
    public void viewWillAppear() {
        ObjC.msgSendVoid(peer, ObjC.sel("viewWillAppear"));
    }

    /// [vc viewDidAppear].
    public void viewDidAppear() {
        ObjC.msgSendVoid(peer, ObjC.sel("viewDidAppear"));
    }

    /// [vc viewWillDisappear].
    public void viewWillDisappear() {
        ObjC.msgSendVoid(peer, ObjC.sel("viewWillDisappear"));
    }

    /// [vc viewDidDisappear].
    public void viewDidDisappear() {
        ObjC.msgSendVoid(peer, ObjC.sel("viewDidDisappear"));
    }

    /// [vc preferredContentSize].
    public NSSize preferredContentSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("preferredContentSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("preferredContentSize failed", t);
        }
    }

    /// [vc setPreferredContentSize:].
    public void setPreferredContentSize(NSSize size) {
        ensureInit();
        try {
            handles.hSetSize().invokeExact(peer, ObjC.sel("setPreferredContentSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setPreferredContentSize: failed", t);
        }
    }

    /// [vc updateViewConstraints].
    public void updateViewConstraints() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateViewConstraints"));
    }

    /// [vc viewWillLayout].
    public void viewWillLayout() {
        ObjC.msgSendVoid(peer, ObjC.sel("viewWillLayout"));
    }

    /// [vc viewDidLayout].
    public void viewDidLayout() {
        ObjC.msgSendVoid(peer, ObjC.sel("viewDidLayout"));
    }

    /// [vc commitEditingWithDelegate:didCommitSelector:contextInfo:] — NSEditor; nulls allowed.
    public void commitEditingWithDelegate(NSObject delegate, MemorySegment didCommitSelector, MemorySegment contextInfo) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("commitEditingWithDelegate:didCommitSelector:contextInfo:"),
                    (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate.peer()),
                    (MemorySegment) (didCommitSelector == null ? MemorySegment.NULL : didCommitSelector),
                    (MemorySegment) (contextInfo == null ? MemorySegment.NULL : contextInfo));
        } catch (Throwable t) {
            throw new RuntimeException("commitEditingWithDelegate:didCommitSelector:contextInfo: failed", t);
        }
    }

    /// [vc commitEditing] — NSEditor.
    public boolean commitEditing() {
        return ObjC.msgSendBool(peer, ObjC.sel("commitEditing"));
    }

    /// [vc discardEditing] — NSEditor.
    public void discardEditing() {
        ObjC.msgSendVoid(peer, ObjC.sel("discardEditing"));
    }

    /// [vc presentViewController:animator:].
    public void presentViewController(NSViewController controller, NSObject animator) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("presentViewController:animator:"), controller.peer(), animator.peer());
        } catch (Throwable t) {
            throw new RuntimeException("presentViewController:animator: failed", t);
        }
    }

    /// [vc dismissViewController:].
    public void dismissViewController(NSViewController controller) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("dismissViewController:"), controller.peer());
        } catch (Throwable t) {
            throw new RuntimeException("dismissViewController: failed", t);
        }
    }

    /// [vc dismissController:] — IBAction (sender may be nil).
    public void dismissController(NSObject sender) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("dismissController:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("dismissController: failed", t);
        }
    }

    /// [vc presentedViewControllers] (may be nil).
    public NSArray presentedViewControllers() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("presentedViewControllers"));
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("presentedViewControllers failed", t);
        }
    }

    /// [vc presentingViewController] (may be nil).
    public NSViewController presentingViewController() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("presentingViewController"));
            return NSViewController.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("presentingViewController failed", t);
        }
    }

    /// [vc presentViewControllerAsSheet:].
    public void presentViewControllerAsSheet(NSViewController controller) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("presentViewControllerAsSheet:"), controller.peer());
        } catch (Throwable t) {
            throw new RuntimeException("presentViewControllerAsSheet: failed", t);
        }
    }

    /// [vc presentViewControllerAsModalWindow:].
    public void presentViewControllerAsModalWindow(NSViewController controller) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("presentViewControllerAsModalWindow:"), controller.peer());
        } catch (Throwable t) {
            throw new RuntimeException("presentViewControllerAsModalWindow: failed", t);
        }
    }

    /// [vc parentViewController] (may be nil).
    public NSViewController parentViewController() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("parentViewController"));
            return NSViewController.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("parentViewController failed", t);
        }
    }

    /// [vc childViewControllers].
    public NSArray childViewControllers() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("childViewControllers"));
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("childViewControllers failed", t);
        }
    }

    /// [vc setChildViewControllers:].
    public void setChildViewControllers(NSArray children) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("setChildViewControllers:"), children.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setChildViewControllers: failed", t);
        }
    }

    /// [vc addChildViewController:].
    public void addChildViewController(NSViewController child) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("addChildViewController:"), child.peer());
        } catch (Throwable t) {
            throw new RuntimeException("addChildViewController: failed", t);
        }
    }

    /// [vc removeFromParentViewController].
    public void removeFromParentViewController() {
        ObjC.msgSendVoid(peer, ObjC.sel("removeFromParentViewController"));
    }

    /// [vc insertChildViewController:atIndex:].
    public void insertChildViewController(NSViewController child, long index) {
        ensureInit();
        try {
            handles.hVoidIdInt().invokeExact(peer, ObjC.sel("insertChildViewController:atIndex:"), child.peer(), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertChildViewController:atIndex: failed", t);
        }
    }

    /// [vc removeChildViewControllerAtIndex:].
    public void removeChildViewControllerAtIndex(long index) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("removeChildViewControllerAtIndex:"), index);
    }

    /// [vc preferredContentSizeDidChangeForViewController:].
    public void preferredContentSizeDidChangeForViewController(NSViewController controller) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("preferredContentSizeDidChangeForViewController:"), controller.peer());
        } catch (Throwable t) {
            throw new RuntimeException("preferredContentSizeDidChangeForViewController: failed", t);
        }
    }

    /// [vc viewWillTransitionToSize:].
    public void viewWillTransitionToSize(NSSize newSize) {
        ensureInit();
        try {
            handles.hSetSize().invokeExact(peer, ObjC.sel("viewWillTransitionToSize:"), newSize.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("viewWillTransitionToSize: failed", t);
        }
    }

    /// [vc storyboard] — unwrapped (no NSStoryboard wrapper).
    public NSObject storyboard() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("storyboard"));
            return NSObject.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("storyboard failed", t);
        }
    }

    /// [vc extensionContext] — unwrapped (no NSExtensionContext wrapper).
    public NSObject extensionContext() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("extensionContext"));
            return NSObject.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("extensionContext failed", t);
        }
    }

    /// [vc sourceItemView] (may be nil).
    public NSView sourceItemView() {
        ensureInit();
        try {
            MemorySegment v = (MemorySegment) handles.hGetView().invokeExact(peer, ObjC.sel("sourceItemView"));
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("sourceItemView failed", t);
        }
    }

    /// [vc setSourceItemView:] (nil clears).
    public void setSourceItemView(NSView view) {
        ensureInit();
        try {
            handles.hSetView().invokeExact(peer, ObjC.sel("setSourceItemView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("setSourceItemView: failed", t);
        }
    }

    /// [vc preferredScreenOrigin].
    public NSPoint preferredScreenOrigin() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetPoint().invokeExact(ObjC.structSlot(), peer, ObjC.sel("preferredScreenOrigin"));
            return NSPoint.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("preferredScreenOrigin failed", t);
        }
    }

    /// [vc setPreferredScreenOrigin:].
    public void setPreferredScreenOrigin(NSPoint origin) {
        ensureInit();
        try {
            handles.hSetPoint().invokeExact(peer, ObjC.sel("setPreferredScreenOrigin:"), origin.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setPreferredScreenOrigin: failed", t);
        }
    }

    /// [vc preferredMinimumSize].
    public NSSize preferredMinimumSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("preferredMinimumSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("preferredMinimumSize failed", t);
        }
    }

    /// [vc preferredMaximumSize].
    public NSSize preferredMaximumSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) handles.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("preferredMaximumSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("preferredMaximumSize failed", t);
        }
    }

}
