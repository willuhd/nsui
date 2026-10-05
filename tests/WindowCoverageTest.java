package nsui.tests;

import java.lang.foreign.MemorySegment;

import nsui.CADisplayLink;
import nsui.NSApplication;
import nsui.NSArray;
import nsui.NSData;
import nsui.NSDate;
import nsui.NSDictionary;
import nsui.NSDocument;
import nsui.NSDockTile;
import nsui.NSEdgeInsets;
import nsui.NSImage;
import nsui.NSObject;
import nsui.NSPanel;
import nsui.NSPoint;
import nsui.NSRect;
import nsui.NSScreen;
import nsui.NSSize;
import nsui.NSToolbar;
import nsui.NSViewController;
import nsui.NSWindow;
import nsui.NSWindowController;
import nsui.objc.ObjC;

/// WindowCoverageTest - Tier-1 coverage for the Window batch additions:
/// NSWindow getters/setters/actions, window lists and screen info;
/// NSPanel modal behavior; NSWindowController content/nib/owner/actions;
/// NSScreen display info.
///
/// Non-interactive and self-terminating: hidden windows only (never shown,
/// never key), ends with TestKit.end().
///
/// Intentionally NOT exercised (disruptive or needs live UI/events):
/// miniaturize/deminiaturize/zoom/performMiniaturize/performZoom/toggleFullScreen,
/// key/main make/become/resign actions, showWindow:, print:,
/// runToolbarCustomizationPalette:, orderWindow except ORDER_OUT,
/// orderFrontRegardless, addTabbedWindow, performWindowDragWithEvent,
/// single-arg nextEventMatchingMask: (blocks until match),
/// non-null sendEvent/postEvent/beginDraggingSession, controller loadWindow.
public final class WindowCoverageTest {

    private static boolean close(double a, double b, double eps) {
        return Math.abs(a - b) <= eps;
    }

    private static boolean rectClose(NSRect a, NSRect b, double eps) {
        return close(a.x(), b.x(), eps) && close(a.y(), b.y(), eps)
                && close(a.width(), b.width(), eps) && close(a.height(), b.height(), eps);
    }

    /// Same as TestKit.check, plus an immediate flush so a native abort
    /// still leaves the exact crash point in the log.
    private static void check(boolean ok, String msg) {
        TestKit.check(ok, msg);
        System.out.flush();
    }

    public static void main(String[] args) {
        System.out.println("=== WindowCoverageTest ===");
        ObjC.init();
        NSApplication app = TestKit.app();

        System.out.println("---- NSWindow props/actions ----");
        NSWindow w1 = TestKit.hiddenWindow(400, 250);
        try {
            w1.setSubtitle("sub-line");
            check("sub-line".equals(w1.subtitle()), "subtitle round-trip");
            w1.setSubtitle(null);
            check("".equals(w1.subtitle()), "subtitle null maps to empty (nil aborts AppKit)");
            long ts0 = w1.toolbarStyle();
            w1.setToolbarStyle(2L);
            check(w1.toolbarStyle() == 2L, "toolbarStyle round-trip");
            w1.setToolbarStyle(ts0);
            NSRect layout = w1.contentLayoutRect();
            check(layout.width() > 0 && layout.height() > 0,
                    "contentLayoutRect non-empty (" + layout + ")");
            NSRect cascade = w1.cascadingReferenceFrame();
            check(cascade.width() >= 0 && cascade.height() >= 0,
                    "cascadingReferenceFrame no crash (" + cascade + ")");
            NSSize ri0 = w1.resizeIncrements();
            w1.setResizeIncrements(new NSSize(8, 8));
            check(w1.resizeIncrements().epsilonEquals(new NSSize(8, 8), 0.001),
                    "resizeIncrements round-trip");
            w1.setResizeIncrements(ri0);
            w1.setAspectRatio(new NSSize(16, 9));
            check(w1.aspectRatio().epsilonEquals(new NSSize(16, 9), 0.001),
                    "aspectRatio round-trip");
            w1.setAspectRatio(new NSSize(1, 1));
            NSSize cmin0 = w1.contentMinSize();
            NSSize cmax0 = w1.contentMaxSize();
            w1.setContentMinSize(new NSSize(100, 80));
            w1.setContentMaxSize(new NSSize(800, 600));
            check(w1.contentMinSize().epsilonEquals(new NSSize(100, 80), 0.001)
                    && w1.contentMaxSize().epsilonEquals(new NSSize(800, 600), 0.001),
                    "contentMin/MaxSize round-trip");
            w1.setContentMinSize(cmin0);
            w1.setContentMaxSize(cmax0);
            NSSize fmin0 = w1.minFullScreenContentSize();
            NSSize fmax0 = w1.maxFullScreenContentSize();
            w1.setMinFullScreenContentSize(new NSSize(200, 150));
            w1.setMaxFullScreenContentSize(new NSSize(1600, 1200));
            check(w1.minFullScreenContentSize().epsilonEquals(new NSSize(200, 150), 0.001)
                    && w1.maxFullScreenContentSize().epsilonEquals(new NSSize(1600, 1200), 0.001),
                    "fullscreen content sizes round-trip");
            w1.setMinFullScreenContentSize(fmin0);
            w1.setMaxFullScreenContentSize(fmax0);
            w1.setContentResizeIncrements(new NSSize(4, 4));
            check(w1.contentResizeIncrements().epsilonEquals(new NSSize(4, 4), 0.001),
                    "contentResizeIncrements round-trip");
            w1.setContentResizeIncrements(NSSize.ZERO);
            w1.setContentAspectRatio(new NSSize(4, 3));
            check(w1.contentAspectRatio().epsilonEquals(new NSSize(4, 3), 0.001),
                    "contentAspectRatio round-trip");
            w1.setContentAspectRatio(new NSSize(1, 1));
            w1.setViewsNeedDisplay(true);
            check(w1.viewsNeedDisplay(), "viewsNeedDisplay round-trip");
            w1.setViewsNeedDisplay(false);
            w1.setPreservesContentDuringLiveResize(true);
            check(w1.preservesContentDuringLiveResize(),
                    "preservesContentDuringLiveResize round-trip");
            w1.setPreservesContentDuringLiveResize(false);
            check(!w1.isInLiveResize(), "isInLiveResize false outside resize");
            w1.displayIfNeeded();
            w1.display();
            w1.update();
            check(true, "display trio no crash, resizeFlags=" + w1.resizeFlags());
            w1.setCanHide(true);
            check(w1.canHide(), "canHide round-trip");
            w1.setCanHide(false);
            w1.setAllowsToolTipsWhenApplicationIsInactive(true);
            check(w1.allowsToolTipsWhenApplicationIsInactive(),
                    "allowsToolTipsWhenApplicationIsInactive round-trip");
            w1.setAllowsToolTipsWhenApplicationIsInactive(false);
            w1.setAllowsConcurrentViewDrawing(true);
            check(w1.allowsConcurrentViewDrawing(),
                    "allowsConcurrentViewDrawing round-trip");
            w1.setAllowsConcurrentViewDrawing(false);
            w1.setDisplaysWhenScreenProfileChanges(false);
            check(!w1.displaysWhenScreenProfileChanges(),
                    "displaysWhenScreenProfileChanges round-trip");
            w1.setDisplaysWhenScreenProfileChanges(true);
            w1.setCanBecomeVisibleWithoutLogin(false);
            check(!w1.canBecomeVisibleWithoutLogin(),
                    "canBecomeVisibleWithoutLogin round-trip");
            long ab0 = w1.animationBehavior();
            w1.setAnimationBehavior(1L);
            check(w1.animationBehavior() == 1L, "animationBehavior round-trip");
            w1.setAnimationBehavior(ab0);
            check(w1.occlusionState() >= 0, "occlusionState reads");
            check(w1.backingType() >= 0, "backingType reads");
            long dl0 = w1.depthLimit();
            w1.setDepthLimit(dl0);
            check(w1.depthLimit() == dl0, "depthLimit set/get");
            check(w1.backingScaleFactor() >= 1.0, "window backingScaleFactor >= 1.0");
            check(true, "canRepresentDisplayGamut(sRGB)=" + w1.canRepresentDisplayGamut(0));
            long tss0 = w1.titlebarSeparatorStyle();
            w1.setTitlebarSeparatorStyle(1L);
            check(w1.titlebarSeparatorStyle() == 1L, "titlebarSeparatorStyle round-trip");
            w1.setTitlebarSeparatorStyle(tss0);
            NSRect r = new NSRect(10, 20, 120, 90);
            NSRect backing = w1.convertRectToBacking(r);
            double scale = w1.backingScaleFactor();
            check(close(backing.width(), r.width() * scale, 0.5)
                    && close(backing.height(), r.height() * scale, 0.5),
                    "convertRectToBacking scales by " + scale);
            check(rectClose(w1.convertRectFromBacking(backing), r, 1.0),
                    "convertRectFromBacking inverts");
            NSRect onScreen = w1.convertRectToScreen(r);
            check(rectClose(w1.convertRectFromScreen(onScreen), r, 1.0),
                    "convertRectTo/FromScreen round-trip");
            check(w1.keyViewSelectionDirection() >= 0, "keyViewSelectionDirection reads");
            boolean ak0 = w1.autorecalculatesKeyViewLoop();
            w1.setAutorecalculatesKeyViewLoop(!ak0);
            check(w1.autorecalculatesKeyViewLoop() == !ak0,
                    "autorecalculatesKeyViewLoop round-trip");
            w1.setAutorecalculatesKeyViewLoop(ak0);
            w1.recalculateKeyViewLoop();
            w1.selectNextKeyView(null);
            w1.selectPreviousKeyView(null);
            w1.selectKeyViewFollowingView(w1.contentView());
            w1.selectKeyViewPrecedingView(w1.contentView());
            w1.setInitialFirstResponder(w1.contentView());
            check(w1.initialFirstResponder() != null, "setInitialFirstResponder sticks");
            w1.setInitialFirstResponder(null);
            w1.disableKeyEquivalentForDefaultButtonCell();
            w1.enableKeyEquivalentForDefaultButtonCell();
            w1.setDefaultButtonCell(null);
            check(true, "key-view loop + defaultButtonCell(null) no crash");
            w1.setTabbingIdentifier("nsui-coverage-tab");
            check("nsui-coverage-tab".equals(w1.tabbingIdentifier()),
                    "tabbingIdentifier round-trip");
            w1.setTabbingIdentifier(null);
            w1.selectNextTab(null);
            w1.selectPreviousTab(null);
            w1.toggleTabBar(null);
            check(w1.tabbedWindows() == null || w1.tabbedWindows().count() >= 0,
                    "tabbedWindows null-safe");
            MemorySegment wtab = w1.tab();
            MemorySegment wtabgroup = w1.tabGroup();
            check(true, "tab/tabGroup no crash (tab="
                    + (wtab.address() == 0 ? "nil" : "peer") + " tabGroup="
                    + (wtabgroup.address() == 0 ? "nil" : "peer") + ")");
            long laydir = w1.windowTitlebarLayoutDirection();
            check(laydir == 0 || laydir == 1, "windowTitlebarLayoutDirection in 0,1");
            check(!w1.hasActiveWindowSharingSession(), "no active sharing session");
            w1.setMiniwindowTitle("mini-me");
            check("mini-me".equals(w1.miniwindowTitle()), "miniwindowTitle round-trip");
            w1.setMiniwindowTitle(null);
            w1.setMiniwindowImage(null);
            check(w1.miniwindowImage() == null, "miniwindowImage nil by default");
            w1.setRepresentedFilename("/tmp/nsui-coverage.txt");
            check("/tmp/nsui-coverage.txt".equals(w1.representedFilename()),
                    "representedFilename round-trip");
            w1.setTitleWithRepresentedFilename("/tmp/other.txt");
            check(w1.title() != null, "setTitleWithRepresentedFilename no crash");
            w1.setRepresentedURL(null);
            check(true, "setRepresentedURL(null) no crash");
            NSDockTile tile = w1.dockTile();
            if (tile != null) {
                tile.setBadgeLabel("T");
                tile.setBadgeLabel(null);
                check(tile.badgeLabel() == null || tile.badgeLabel().isEmpty(),
                        "window dockTile badge set+cleared immediately");
            } else {
                check(true, "window dockTile nil (null-safe)");
            }
            w1.endEditingFor(null);
            NSRect before = w1.frame();
            w1.setFrameTopLeftPoint(new NSPoint(-10000, 500));
            check(close(w1.frame().width(), before.width(), 0.5)
                    && close(w1.frame().height(), before.height(), 0.5),
                    "setFrameTopLeftPoint moves, size preserved");
            w1.setContentBorderThicknessForEdge(4.0, 1L);
            check(close(w1.contentBorderThicknessForEdge(1L), 4.0, 0.001),
                    "contentBorderThicknessForEdge round-trip");
            w1.setAutorecalculatesContentBorderThicknessForEdge(false, 1L);
            check(!w1.autorecalculatesContentBorderThicknessForEdge(1L),
                    "autorecalculatesContentBorderThickness round-trip");
            w1.setAutorecalculatesContentBorderThicknessForEdge(true, 1L);
            w1.setContentBorderThicknessForEdge(0.0, 1L);
            w1.orderBack(null);
            check(w1.isVisible(), "orderBack shows window at back (AppKit truth)");
            w1.orderOut(null);
            check(!w1.isVisible(), "orderOut hides again");
            w1.orderWindowRelativeTo(NSWindow.ORDER_OUT, 0);
            check(true, "orderWindow:relativeTo: no crash");
            w1.orderOut(null);
            w1.invalidateShadow();
            w1.setAppearanceSource(null);
            w1.setColorSpace(null);
            check(true, "invalidateShadow/appearance/colorSpace(null) no crash");
            check(w1.sheets() != null && w1.sheets().count() == 0,
                    "sheets empty with no sheet");
            check(w1.childWindows() == null || w1.childWindows().count() == 0,
                    "childWindows empty when childless");
            check(w1.parentWindow() == null, "parentWindow null for top-level");
            check(w1.attachedSheet() == null && !w1.isSheet() && w1.sheetParent() == null,
                    "sheet state nil/false with no sheet");
            NSDictionary dd = w1.deviceDescription();
            check(dd == null || dd.count() >= 0, "deviceDescription null-safe");
            check(w1.screenObject() == null || w1.screenObject().frame().width() > 0,
                    "screenObject null-safe");
            check(w1.deepestScreenObject() == null
                    || w1.deepestScreenObject().frame().width() > 0,
                    "deepestScreenObject null-safe");
            check(w1.nextEventMatchingMaskUntilDate(0, NSDate.distantPast(),
                    "kCFRunLoopDefaultMode", false) == null,
                    "nextEventMatchingMaskUntilDate(past) nil");
            w1.discardEventsMatchingMaskBeforeEvent(0, null);
            w1.postEvent(null, false);
            w1.sendEvent(null);
            check(w1.currentEvent() == null, "currentEvent nil outside dispatch");
            w1.setIgnoresMouseEvents(true);
            check(w1.ignoresMouseEvents(), "ignoresMouseEvents round-trip");
            w1.setIgnoresMouseEvents(false);
            NSPoint mouse = w1.mouseLocationOutsideOfEventStream();
            check(Double.isFinite(mouse.x()) && Double.isFinite(mouse.y()),
                    "mouseLocationOutsideOfEventStream finite");
            w1.disableCursorRects();
            w1.enableCursorRects();
            w1.discardCursorRects();
            w1.invalidateCursorRectsForView(w1.contentView());
            w1.resetCursorRects();
            check(true, "cursor-rect methods no crash");
            w1.registerForDraggedTypes(NSArray.array());
            w1.unregisterDraggedTypes();
            check(w1.beginDraggingSessionWithItems(NSArray.array(), null, null) == null,
                    "beginDraggingSession null-event guard returns null");
            check(!w1.tryToPerform(ObjC.sel("nsuiBogusAction999:"), null),
                    "tryToPerform bogus selector returns false");
            MemorySegment req = w1.validRequestorForSendType(
                    ObjC.nsstring("public.string"), ObjC.nsstring("public.string"));
            check(req == null || req.address() == 0 || req.address() != 0,
                    "validRequestorForSendType no crash");
            NSData eps = w1.dataWithEPSInsideRect(new NSRect(0, 0, 100, 100));
            NSData pdf = w1.dataWithPDFInsideRect(new NSRect(0, 0, 100, 100));
            check(eps != null && pdf != null && eps.length() >= 0 && pdf.length() >= 0,
                    "EPS/PDF snapshots non-null");
            String saved = w1.stringWithSavedFrame();
            check(saved != null && !saved.isEmpty(), "stringWithSavedFrame non-empty");
            System.out.println("frame-step: saved=[" + saved + "]");
            System.out.flush();
            w1.setFrameFromString(saved);
            System.out.println("frame-step: setFrameFromString ok");
            System.out.flush();
            w1.saveFrameUsingName("nsui-window-coverage");
            System.out.println("frame-step: save ok");
            System.out.flush();
            check(w1.setFrameUsingName("nsui-window-coverage"),
                    "saveFrameUsingName/setFrameUsingName round-trip");
            check(w1.setFrameUsingNameForce("nsui-window-coverage", true),
                    "setFrameUsingNameForce round-trip");
            check(!w1.setFrameUsingName("nsui-no-such-frame-xyz"),
                    "setFrameUsingName unknown name returns false");
            NSWindow.removeFrameUsingName("nsui-window-coverage");
            check(w1.toolbar() == null, "toolbar nil by default");
            NSToolbar bar = NSToolbar.create("nsui-coverage-bar");
            w1.setToolbar(bar);
            check(w1.toolbar() != null, "setToolbar round-trip");
            w1.toggleToolbarShown(null);
            w1.setToolbar(null);
            check(w1.toolbar() == null, "setToolbar(null) detaches");
            NSWindow wVC = TestKit.hiddenWindow(300, 200);
            check(wVC.contentViewController() == null,
                    "contentViewController nil by default");
            NSViewController vc = NSViewController.create();
            wVC.setContentViewController(vc);
            check(wVC.contentViewController() != null,
                    "setContentViewController round-trip");
            wVC.setContentViewController(null);
            TestKit.close(wVC);
            NSWindow parent = TestKit.hiddenWindow(300, 200);
            NSWindow child = TestKit.hiddenWindow(150, 100);
            parent.addChildWindow(child, NSWindow.ORDER_ABOVE);
            check(parent.childWindows() != null && parent.childWindows().count() == 1,
                    "addChildWindow count==1");
            check(child.parentWindow() != null, "child parentWindow set");
            parent.removeChildWindow(child);
            check(parent.childWindows() == null || parent.childWindows().count() == 0,
                    "removeChildWindow detaches");
            check(child.parentWindow() == null, "parentWindow nil after detach");
            TestKit.close(child);
            TestKit.close(parent);
            NSWindow wC = TestKit.hiddenWindow(300, 200);
            check(wC.windowController() == null, "windowController nil by default");
            NSWindowController link = NSWindowController.create();
            link.setWindow(wC);
            wC.setWindowController(link);
            check(wC.windowController() != null, "setWindowController round-trip");
            check(link.window() != null, "controller.window points back");
            link.setWindow(null);
            wC.setWindowController(null);
            check(wC.windowController() == null, "setWindowController(null) detaches");
            TestKit.close(wC);
            NSArray numbers = NSWindow.windowNumbersWithOptions(0);
            check(numbers != null, "windowNumbersWithOptions non-null");
            check(NSWindow.defaultDepthLimit() >= 0, "defaultDepthLimit reads");
            boolean aat0 = NSWindow.allowsAutomaticWindowTabbing();
            NSWindow.setAllowsAutomaticWindowTabbing(aat0);
            check(NSWindow.allowsAutomaticWindowTabbing() == aat0,
                    "allowsAutomaticWindowTabbing set/get");
            check(NSWindow.userTabbingPreference() >= 0, "userTabbingPreference reads");
            check(NSWindow.standardWindowButtonForStyleMask(0, 15) != null,
                    "standardWindowButtonForStyleMask non-null");
            NSWindow fromVC = NSWindow.windowWithContentViewController(NSViewController.create());
            check(fromVC != null && fromVC.isKindOfClass("NSWindow"),
                    "windowWithContentViewController non-null NSWindow");
            TestKit.close(fromVC);
            CADisplayLink wlink = w1.displayLinkWithTarget(app, "updateWindows");
            check(wlink != null, "window displayLinkWithTarget non-null");
            if (wlink != null) wlink.invalidate();
        } catch (Throwable t) {
            check(false, "NSWindow section threw: " + t);
            t.printStackTrace(System.out);
        } finally {
            TestKit.close(w1);
        }

        try {
            NSWindow wClose = TestKit.hiddenWindow(200, 150);
            wClose.setReleasedWhenClosed(false);
            wClose.close();
            check(!wClose.isVisible(), "close() leaves hidden window non-visible");
        } catch (Throwable t) {
            check(false, "close section threw: " + t);
        }

        System.out.println("---- NSPanel ----");
        try {
            NSPanel panel = NSPanel.create(new NSRect(0, 0, 320, 200), 15L | 16L, false);
            panel.setReleasedWhenClosed(false);
            panel.setWorksWhenModal(true);
            check(panel.worksWhenModal(), "panel setWorksWhenModal(true) reads back");
            panel.setWorksWhenModal(false);
            check(!panel.worksWhenModal(), "panel setWorksWhenModal(false) reads back");
            panel.setBecomesKeyOnlyIfNeeded(true);
            check(panel.becomesKeyOnlyIfNeeded(),
                    "panel setBecomesKeyOnlyIfNeeded(true) reads back");
            panel.setBecomesKeyOnlyIfNeeded(false);
            check(!panel.becomesKeyOnlyIfNeeded(),
                    "panel setBecomesKeyOnlyIfNeeded(false) reads back");
            check(NSPanel.wrap(null) == null
                    && NSPanel.wrap(MemorySegment.NULL) == null,
                    "NSPanel.wrap(nil-safe)");
            TestKit.close(panel);
        } catch (Throwable t) {
            check(false, "NSPanel section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println("---- NSWindowController ----");
        try {
            NSWindowController c1 = NSWindowController.create();
            check(c1.windowNibName() == null, "windowNibName nil (not nib-based)");
            check(c1.windowNibPath() == null, "windowNibPath nil (not nib-based)");
            MemorySegment cowner = c1.owner();
            check(true, "owner no crash (" + (cowner.address() == 0 ? "nil" : "peer") + ")");
            c1.setWindowFrameAutosaveName("nsui-ctrl-coverage");
            check("nsui-ctrl-coverage".equals(c1.windowFrameAutosaveName()),
                    "windowFrameAutosaveName round-trip");
            c1.setWindowFrameAutosaveName(null);
            check("".equals(c1.windowFrameAutosaveName()),
                    "windowFrameAutosaveName null maps to empty (nil aborts AppKit)");
            c1.setShouldCascadeWindows(true);
            check(c1.shouldCascadeWindows(), "shouldCascadeWindows round-trip");
            c1.setShouldCascadeWindows(false);
            c1.setShouldCloseDocument(true);
            check(c1.shouldCloseDocument(), "shouldCloseDocument round-trip");
            c1.setShouldCloseDocument(false);
            c1.setDocumentEdited(true);
            c1.setDocumentEdited(false);
            c1.synchronizeWindowTitleWithDocumentName();
            check("Doc Title".equals(c1.windowTitleForDocumentDisplayName("Doc Title")),
                    "windowTitleForDocumentDisplayName echoes input");
            check(c1.contentViewController() == null,
                    "controller contentViewController nil by default");
            NSViewController cvc = NSViewController.create();
            c1.setContentViewController(cvc);
            check(c1.contentViewController() != null,
                    "controller setContentViewController round-trip");
            c1.setContentViewController(null);
            check(c1.previewRepresentableActivityItems() == null
                    || c1.previewRepresentableActivityItems().count() >= 0,
                    "previewRepresentableActivityItems null-safe");
            c1.setPreviewRepresentableActivityItems(NSArray.array());
            check(c1.previewRepresentableActivityItems() != null
                    && c1.previewRepresentableActivityItems().count() == 0,
                    "previewRepresentableActivityItems empty round-trip");
            c1.setPreviewRepresentableActivityItems(null);
            c1.windowWillLoad();
            c1.windowDidLoad();
            c1.dismissController(null);
            check(c1.storyboard().address() == 0, "storyboard nil (not from storyboard)");
            NSWindow w2 = TestKit.hiddenWindow(300, 200);
            NSWindowController c2 = NSWindowController.initWithWindow(w2);
            check(c2.window() != null, "initWithWindow wires window");
            check(c2.isWindowLoaded(), "isWindowLoaded true with window");
            NSDocument doc = NSDocument.create();
            c2.setDocument(doc);
            check(c2.document() != null, "setDocument/document round-trip");
            c2.setDocument(null);
            c2.setWindow(null);
            check(c2.window() == null, "setWindow(null) detaches");
            c1.close();
            check(true, "controller close()/lifecycle no crash");
            check(NSWindowController.wrap(null) == null
                    && NSWindowController.wrap(MemorySegment.NULL) == null,
                    "NSWindowController.wrap(nil-safe)");
            TestKit.close(w2);
        } catch (Throwable t) {
            check(false, "NSWindowController section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println("---- NSScreen ----");
        try {
            NSScreen main = NSScreen.mainScreen();
            check(main != null, "mainScreen non-nil");
            if (main != null) {
                check(NSScreen.deepestScreen() != null, "deepestScreen non-nil");
                check(true, "screensHaveSeparateSpaces="
                        + " " + NSScreen.screensHaveSeparateSpaces());
                check(main.depth() >= 0, "depth reads");
                check(main.supportedWindowDepths() != null,
                        "supportedWindowDepths non-nil pointer");
                check(main.canRepresentDisplayGamut(0),
                        "canRepresentDisplayGamut(sRGB) true");
                NSRect sr = new NSRect(0, 0, 200, 150);
                NSRect sb = main.convertRectToBacking(sr);
                double sscale = main.backingScaleFactor();
                check(close(sb.width(), sr.width() * sscale, 0.5),
                        "screen convertRectToBacking scales");
                check(rectClose(main.convertRectFromBacking(sb), sr, 1.0),
                        "screen convertRectFromBacking inverts");
                String lname = main.localizedName();
                check(lname != null && !lname.isEmpty(), "localizedName non-empty");
                NSEdgeInsets insets = main.safeAreaInsets();
                check(insets.top() >= 0 && insets.left() >= 0
                        && insets.bottom() >= 0 && insets.right() >= 0,
                        "safeAreaInsets non-negative");
                NSRect tl = main.auxiliaryTopLeftArea();
                NSRect tr = main.auxiliaryTopRightArea();
                check(tl.width() >= 0 && tr.width() >= 0, "auxiliary areas no crash");
                check(main.maximumExtendedDynamicRangeColorComponentValue() >= 1.0,
                        "EDR current >= 1.0");
                check(main.maximumPotentialExtendedDynamicRangeColorComponentValue() >= 1.0,
                        "EDR potential >= 1.0");
                check(main.maximumReferenceExtendedDynamicRangeColorComponentValue() >= 0,
                        "EDR reference >= 0");
                check(main.maximumFramesPerSecond() > 0, "maximumFramesPerSecond > 0");
                check(main.minimumRefreshInterval() > 0
                        && main.maximumRefreshInterval() >= main.minimumRefreshInterval(),
                        "refresh intervals sane");
                check(main.displayUpdateGranularity() >= 0,
                        "displayUpdateGranularity >= 0");
                check(main.lastDisplayUpdateTimestamp() >= 0,
                        "lastDisplayUpdateTimestamp >= 0");
                check(main.cgDirectDisplayID() != 0, "CGDirectDisplayID non-zero");
                CADisplayLink slink = main.displayLinkWithTarget(app, "updateWindows");
                check(slink != null, "screen displayLinkWithTarget non-null");
                if (slink != null) slink.invalidate();
            }
        } catch (Throwable t) {
            check(false, "NSScreen section threw: " + t);
            t.printStackTrace(System.out);
        }

        TestKit.end();
    }
}
