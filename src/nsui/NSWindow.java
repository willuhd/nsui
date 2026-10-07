package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSWindow — a native window: frame, title, visibility, key/main status, style,
/// delegate, close. Thin 1:1 wrapper; all behavior is AppKit's.
///
/// **The compositional style model.** An AppKit window's "*type*" is never a
/// named constant — it is the composition of a `styleMask` bit-field, the
/// concrete subclass (`NSWindow` vs `NSPanel`), and a handful of
/// behavior properties. There is **no settable title-bar height and no settable
/// corner radius** on the native side: those derive from the style you choose,
/// not from numbers you pass. If you need a given look, pick the style bits + panel
/// subclass that AppKit maps to it, then adjust the behavior booleans.
///
/// **`styleMask` bits** (from `NSWindowStyleMask`, macOS 15 SDK):
/// - `1`     `NSWindowStyleMaskTitled`
/// - `2`     `NSWindowStyleMaskClosable`
/// - `4`     `NSWindowStyleMaskMiniaturizable`
/// - `8`     `NSWindowStyleMaskResizable`
/// - `16`    `NSWindowStyleMaskUtilityWindow` — see `createPanel`
/// - `128`   `NSWindowStyleMaskNonactivatingPanel` — never activates the app
/// - `32768` `NSWindowStyleMaskFullSizeContentView` — content extends under the title bar
/// OR the bits together (e.g. `1|2|4|8` = a standard titled, closable,
/// miniaturizable, resizable document window).
///
/// **`NSPanel`.** `createPanel` builds an `NSPanel`
/// subclass using the very same
/// `initWithContentRect:styleMask:backing:defer:` initializer as
/// `create`. Combined with `NSWindowStyleMaskUtilityWindow` (16) you
/// get AppKit's smaller-title-bar, less-rounded "settings / utility" panel. Panels
/// differ from windows in a few respects exposed here: `setHidesOnDeactivate`
/// (a real `NSPanel` behavior via `hidesOnDeactivate`) and
/// `setBecomesKeyOnlyIfNeeded` (a panel stays key only while controls need
/// it), plus the AppKit default that panels are excluded from the Window menu.
/// `isUtilityWindow` reports whether the receiver is a utility panel:
/// `true` for `NSPanel` instances or windows whose styleMask includes
/// `NSWindowStyleMaskUtilityWindow`.
///
/// **Title bar transparency & visibility.**
/// `setTitlebarAppearsTransparent` and `setTitleVisibility` are the
/// AppKit-native "modern title bar" switches — the translucent/floating-header look
/// modern apps use. There is no height/radius knob; the appearance comes from the
/// style + these flags + whatever the content view draws behind the bar.
///
/// NOTE: NSWindow has NO bare `setFrame:` — that is an NSView selector;
/// windows use `setFrame:display:` and `setFrameOrigin:`.
///
/// NSWindow extends NSResponder — mirroring AppKit, where NSWindow IS an
/// NSResponder and terminates the view-side responder chain — so windows gain
/// `nextResponder`/`setNextResponder`, the event pass-throughs, and
/// `touchBar()`/`setTouchBar` from one shared implementation.
public class NSWindow extends NSResponder {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment alloc;
        static MemorySegment initWithContentRect_styleMask_backing_defer;
        static MemorySegment setTitle;
        static MemorySegment center;
        static MemorySegment setReleasedWhenClosed;
        static MemorySegment isReleasedWhenClosed;
        static MemorySegment setContentView;
        static MemorySegment setDelegate;
        static MemorySegment delegate;
        static MemorySegment frame;
        static MemorySegment contentView;
        static MemorySegment bounds;
        static MemorySegment setFrame_display;
        static MemorySegment setFrameOrigin;
        static MemorySegment setContentSize;
        static MemorySegment styleMask;
        static MemorySegment setStyleMask;
        static MemorySegment setTitlebarAppearsTransparent;
        static MemorySegment titlebarAppearsTransparent;
        static MemorySegment setTitleVisibility;
        static MemorySegment setLevel;
        static MemorySegment level;
        static MemorySegment setCollectionBehavior;
        static MemorySegment setHidesOnDeactivate;
        static MemorySegment hidesOnDeactivate;
        static MemorySegment setBecomesKeyOnlyIfNeeded;
        static MemorySegment becomesKeyOnlyIfNeeded;
        static MemorySegment standardWindowButton;
        static MemorySegment isFloatingPanel;
        static MemorySegment title;
        static MemorySegment titleVisibility;
        static MemorySegment collectionBehavior;
        static MemorySegment isMovable;
        static MemorySegment setMovable;
        static MemorySegment isMovableByWindowBackground;
        static MemorySegment setMovableByWindowBackground;
        static MemorySegment isExcludedFromWindowsMenu;
        static MemorySegment setExcludedFromWindowsMenu;
        static MemorySegment tabbingMode;
        static MemorySegment setTabbingMode;
        static MemorySegment backgroundColor;
        static MemorySegment setBackgroundColor;
        static MemorySegment isOpaque;
        static MemorySegment setOpaque;
        static MemorySegment hasShadow;
        static MemorySegment setHasShadow;
        static MemorySegment alphaValue;
        static MemorySegment setAlphaValue;
        static MemorySegment minSize;
        static MemorySegment setMinSize;
        static MemorySegment maxSize;
        static MemorySegment setMaxSize;
        static MemorySegment frameAutosaveName;
        static MemorySegment setFrameAutosaveName;
        static MemorySegment isDocumentEdited;
        static MemorySegment setDocumentEdited;
        static MemorySegment windowNumber;
        static MemorySegment isVisible;
        static MemorySegment isKeyWindow;
        static MemorySegment isMainWindow;
        static MemorySegment makeKeyAndOrderFront;
        static MemorySegment orderFront;
        static MemorySegment sharingType;
        static MemorySegment setSharingType;
        static MemorySegment performClose;
        static MemorySegment beginSheet_completionHandler;
        static MemorySegment endSheet;
        static MemorySegment endSheet_returnCode;
        static MemorySegment attachedSheet;
        static MemorySegment isSheet;
        static MemorySegment sheetParent;
        static MemorySegment orderOut;
        static MemorySegment isZoomed;
        static MemorySegment isMiniaturized;
        static MemorySegment canBecomeKeyWindow;
        static MemorySegment canBecomeMainWindow;
        static MemorySegment worksWhenModal;
        static MemorySegment screen;
        static MemorySegment hasDynamicDepthLimit;
        static MemorySegment deepestScreen;
        static MemorySegment deviceDescription;
        static MemorySegment setDynamicDepthLimit;
        static MemorySegment windowNumbersWithOptions;
        static MemorySegment windowWithContentViewController;
        static MemorySegment defaultDepthLimit;
        static MemorySegment allowsAutomaticWindowTabbing;
        static MemorySegment setAllowsAutomaticWindowTabbing;
        static MemorySegment userTabbingPreference;
        static MemorySegment standardWindowButton_forStyleMask;
        static MemorySegment removeFrameUsingName;
        static MemorySegment subtitle;
        static MemorySegment setSubtitle;
        static MemorySegment toolbarStyle;
        static MemorySegment setToolbarStyle;
        static MemorySegment contentLayoutRect;
        static MemorySegment cascadingReferenceFrame;
        static MemorySegment titlebarAccessoryViewControllers;
        static MemorySegment addTitlebarAccessoryViewController;
        static MemorySegment insertTitlebarAccessoryViewController_atIndex;
        static MemorySegment removeTitlebarAccessoryViewControllerAtIndex;
        static MemorySegment representedURL;
        static MemorySegment setRepresentedURL;
        static MemorySegment representedFilename;
        static MemorySegment setRepresentedFilename;
        static MemorySegment setTitleWithRepresentedFilename;
        static MemorySegment endEditingFor;
        static MemorySegment setFrameTopLeftPoint;
        static MemorySegment inLiveResize;
        static MemorySegment resizeIncrements;
        static MemorySegment setResizeIncrements;
        static MemorySegment aspectRatio;
        static MemorySegment setAspectRatio;
        static MemorySegment contentResizeIncrements;
        static MemorySegment setContentResizeIncrements;
        static MemorySegment contentAspectRatio;
        static MemorySegment setContentAspectRatio;
        static MemorySegment contentMinSize;
        static MemorySegment setContentMinSize;
        static MemorySegment contentMaxSize;
        static MemorySegment setContentMaxSize;
        static MemorySegment minFullScreenContentSize;
        static MemorySegment setMinFullScreenContentSize;
        static MemorySegment maxFullScreenContentSize;
        static MemorySegment setMaxFullScreenContentSize;
        static MemorySegment viewsNeedDisplay;
        static MemorySegment setViewsNeedDisplay;
        static MemorySegment displayIfNeeded;
        static MemorySegment display;
        static MemorySegment preservesContentDuringLiveResize;
        static MemorySegment setPreservesContentDuringLiveResize;
        static MemorySegment update;
        static MemorySegment resizeFlags;
        static MemorySegment close;
        static MemorySegment miniaturize;
        static MemorySegment deminiaturize;
        static MemorySegment zoom;
        static MemorySegment performMiniaturize;
        static MemorySegment performZoom;
        static MemorySegment tryToPerform_with;
        static MemorySegment validRequestorForSendType_returnType;
        static MemorySegment setContentBorderThickness_forEdge;
        static MemorySegment contentBorderThicknessForEdge;
        static MemorySegment setAutorecalculatesContentBorderThickness_forEdge;
        static MemorySegment autorecalculatesContentBorderThicknessForEdge;
        static MemorySegment canHide;
        static MemorySegment setCanHide;
        static MemorySegment orderBack;
        static MemorySegment orderWindow_relativeTo;
        static MemorySegment orderFrontRegardless;
        static MemorySegment miniwindowImage;
        static MemorySegment setMiniwindowImage;
        static MemorySegment miniwindowTitle;
        static MemorySegment setMiniwindowTitle;
        static MemorySegment dockTile;
        static MemorySegment makeKeyWindow;
        static MemorySegment makeMainWindow;
        static MemorySegment becomeKeyWindow;
        static MemorySegment resignKeyWindow;
        static MemorySegment becomeMainWindow;
        static MemorySegment resignMainWindow;
        static MemorySegment preventsApplicationTerminationWhenModal;
        static MemorySegment setPreventsApplicationTerminationWhenModal;
        static MemorySegment convertRectToScreen;
        static MemorySegment convertRectFromScreen;
        static MemorySegment convertRectToBacking;
        static MemorySegment convertRectFromBacking;
        static MemorySegment backingScaleFactor;
        static MemorySegment backingType;
        static MemorySegment setBackingType;
        static MemorySegment depthLimit;
        static MemorySegment setDepthLimit;
        static MemorySegment animationBehavior;
        static MemorySegment setAnimationBehavior;
        static MemorySegment isOnActiveSpace;
        static MemorySegment occlusionState;
        static MemorySegment allowsToolTipsWhenApplicationIsInactive;
        static MemorySegment setAllowsToolTipsWhenApplicationIsInactive;
        static MemorySegment allowsConcurrentViewDrawing;
        static MemorySegment setAllowsConcurrentViewDrawing;
        static MemorySegment displaysWhenScreenProfileChanges;
        static MemorySegment setDisplaysWhenScreenProfileChanges;
        static MemorySegment canBecomeVisibleWithoutLogin;
        static MemorySegment setCanBecomeVisibleWithoutLogin;
        static MemorySegment invalidateShadow;
        static MemorySegment appearanceSource;
        static MemorySegment setAppearanceSource;
        static MemorySegment colorSpace;
        static MemorySegment setColorSpace;
        static MemorySegment canRepresentDisplayGamut;
        static MemorySegment titlebarSeparatorStyle;
        static MemorySegment setTitlebarSeparatorStyle;
        static MemorySegment stringWithSavedFrame;
        static MemorySegment setFrameFromString;
        static MemorySegment saveFrameUsingName;
        static MemorySegment setFrameUsingName_force;
        static MemorySegment setFrameUsingName;
        static MemorySegment windowController;
        static MemorySegment setWindowController;
        static MemorySegment sheets;
        static MemorySegment childWindows;
        static MemorySegment parentWindow;
        static MemorySegment addChildWindow_ordered;
        static MemorySegment removeChildWindow;
        static MemorySegment contentViewController;
        static MemorySegment setContentViewController;
        static MemorySegment performWindowDragWithEvent;
        static MemorySegment toggleFullScreen;
        static MemorySegment dataWithEPSInsideRect;
        static MemorySegment dataWithPDFInsideRect;
        static MemorySegment print;
        static MemorySegment displayLinkWithTarget_selector;
        static MemorySegment setInitialFirstResponder;
        static MemorySegment selectNextKeyView;
        static MemorySegment selectPreviousKeyView;
        static MemorySegment selectKeyViewFollowingView;
        static MemorySegment selectKeyViewPrecedingView;
        static MemorySegment keyViewSelectionDirection;
        static MemorySegment defaultButtonCell;
        static MemorySegment setDefaultButtonCell;
        static MemorySegment disableKeyEquivalentForDefaultButtonCell;
        static MemorySegment enableKeyEquivalentForDefaultButtonCell;
        static MemorySegment autorecalculatesKeyViewLoop;
        static MemorySegment setAutorecalculatesKeyViewLoop;
        static MemorySegment recalculateKeyViewLoop;
        static MemorySegment toolbar;
        static MemorySegment setToolbar;
        static MemorySegment toggleToolbarShown;
        static MemorySegment runToolbarCustomizationPalette;
        static MemorySegment tabbingIdentifier;
        static MemorySegment setTabbingIdentifier;
        static MemorySegment selectNextTab;
        static MemorySegment selectPreviousTab;
        static MemorySegment moveTabToNewWindow;
        static MemorySegment mergeAllWindows;
        static MemorySegment toggleTabBar;
        static MemorySegment toggleTabOverview;
        static MemorySegment tabbedWindows;
        static MemorySegment addTabbedWindow_ordered;
        static MemorySegment tab;
        static MemorySegment tabGroup;
        static MemorySegment hasActiveWindowSharingSession;
        static MemorySegment windowTitlebarLayoutDirection;
        static MemorySegment nextEventMatchingMask;
        static MemorySegment nextEventMatchingMask_untilDate_inMode_dequeue;
        static MemorySegment discardEventsMatchingMask_beforeEvent;
        static MemorySegment postEvent_atStart;
        static MemorySegment sendEvent;
        static MemorySegment currentEvent;
        static MemorySegment ignoresMouseEvents;
        static MemorySegment setIgnoresMouseEvents;
        static MemorySegment mouseLocationOutsideOfEventStream;
        static MemorySegment disableCursorRects;
        static MemorySegment enableCursorRects;
        static MemorySegment discardCursorRects;
        static MemorySegment areCursorRectsEnabled;
        static MemorySegment invalidateCursorRectsForView;
        static MemorySegment resetCursorRects;
        static MemorySegment registerForDraggedTypes;
        static MemorySegment unregisterDraggedTypes;
        static MemorySegment beginDraggingSessionWithItems_event_source;
        static MemorySegment makeFirstResponder;
        static MemorySegment firstResponder;
        static MemorySegment setAcceptsMouseMovedEvents;
        static MemorySegment acceptsMouseMovedEvents;
        static MemorySegment initialFirstResponder;
        static void populate() {
            alloc = ObjC.sel("alloc");
            initWithContentRect_styleMask_backing_defer = ObjC.sel("initWithContentRect:styleMask:backing:defer:");
            setTitle = ObjC.sel("setTitle:");
            center = ObjC.sel("center");
            setReleasedWhenClosed = ObjC.sel("setReleasedWhenClosed:");
            isReleasedWhenClosed = ObjC.sel("isReleasedWhenClosed");
            setContentView = ObjC.sel("setContentView:");
            setDelegate = ObjC.sel("setDelegate:");
            delegate = ObjC.sel("delegate");
            frame = ObjC.sel("frame");
            contentView = ObjC.sel("contentView");
            bounds = ObjC.sel("bounds");
            setFrame_display = ObjC.sel("setFrame:display:");
            setFrameOrigin = ObjC.sel("setFrameOrigin:");
            setContentSize = ObjC.sel("setContentSize:");
            styleMask = ObjC.sel("styleMask");
            setStyleMask = ObjC.sel("setStyleMask:");
            setTitlebarAppearsTransparent = ObjC.sel("setTitlebarAppearsTransparent:");
            titlebarAppearsTransparent = ObjC.sel("titlebarAppearsTransparent");
            setTitleVisibility = ObjC.sel("setTitleVisibility:");
            setLevel = ObjC.sel("setLevel:");
            level = ObjC.sel("level");
            setCollectionBehavior = ObjC.sel("setCollectionBehavior:");
            setHidesOnDeactivate = ObjC.sel("setHidesOnDeactivate:");
            hidesOnDeactivate = ObjC.sel("hidesOnDeactivate");
            setBecomesKeyOnlyIfNeeded = ObjC.sel("setBecomesKeyOnlyIfNeeded:");
            becomesKeyOnlyIfNeeded = ObjC.sel("becomesKeyOnlyIfNeeded");
            standardWindowButton = ObjC.sel("standardWindowButton:");
            isFloatingPanel = ObjC.sel("isFloatingPanel");
            title = ObjC.sel("title");
            titleVisibility = ObjC.sel("titleVisibility");
            collectionBehavior = ObjC.sel("collectionBehavior");
            isMovable = ObjC.sel("isMovable");
            setMovable = ObjC.sel("setMovable:");
            isMovableByWindowBackground = ObjC.sel("isMovableByWindowBackground");
            setMovableByWindowBackground = ObjC.sel("setMovableByWindowBackground:");
            isExcludedFromWindowsMenu = ObjC.sel("isExcludedFromWindowsMenu");
            setExcludedFromWindowsMenu = ObjC.sel("setExcludedFromWindowsMenu:");
            tabbingMode = ObjC.sel("tabbingMode");
            setTabbingMode = ObjC.sel("setTabbingMode:");
            backgroundColor = ObjC.sel("backgroundColor");
            setBackgroundColor = ObjC.sel("setBackgroundColor:");
            isOpaque = ObjC.sel("isOpaque");
            setOpaque = ObjC.sel("setOpaque:");
            hasShadow = ObjC.sel("hasShadow");
            setHasShadow = ObjC.sel("setHasShadow:");
            alphaValue = ObjC.sel("alphaValue");
            setAlphaValue = ObjC.sel("setAlphaValue:");
            minSize = ObjC.sel("minSize");
            setMinSize = ObjC.sel("setMinSize:");
            maxSize = ObjC.sel("maxSize");
            setMaxSize = ObjC.sel("setMaxSize:");
            frameAutosaveName = ObjC.sel("frameAutosaveName");
            setFrameAutosaveName = ObjC.sel("setFrameAutosaveName:");
            isDocumentEdited = ObjC.sel("isDocumentEdited");
            setDocumentEdited = ObjC.sel("setDocumentEdited:");
            windowNumber = ObjC.sel("windowNumber");
            isVisible = ObjC.sel("isVisible");
            isKeyWindow = ObjC.sel("isKeyWindow");
            isMainWindow = ObjC.sel("isMainWindow");
            makeKeyAndOrderFront = ObjC.sel("makeKeyAndOrderFront:");
            orderFront = ObjC.sel("orderFront:");
            sharingType = ObjC.sel("sharingType");
            setSharingType = ObjC.sel("setSharingType:");
            performClose = ObjC.sel("performClose:");
            beginSheet_completionHandler = ObjC.sel("beginSheet:completionHandler:");
            endSheet = ObjC.sel("endSheet:");
            endSheet_returnCode = ObjC.sel("endSheet:returnCode:");
            attachedSheet = ObjC.sel("attachedSheet");
            isSheet = ObjC.sel("isSheet");
            sheetParent = ObjC.sel("sheetParent");
            orderOut = ObjC.sel("orderOut:");
            isZoomed = ObjC.sel("isZoomed");
            isMiniaturized = ObjC.sel("isMiniaturized");
            canBecomeKeyWindow = ObjC.sel("canBecomeKeyWindow");
            canBecomeMainWindow = ObjC.sel("canBecomeMainWindow");
            worksWhenModal = ObjC.sel("worksWhenModal");
            screen = ObjC.sel("screen");
            hasDynamicDepthLimit = ObjC.sel("hasDynamicDepthLimit");
            deepestScreen = ObjC.sel("deepestScreen");
            deviceDescription = ObjC.sel("deviceDescription");
            setDynamicDepthLimit = ObjC.sel("setDynamicDepthLimit:");
            windowNumbersWithOptions = ObjC.sel("windowNumbersWithOptions:");
            windowWithContentViewController = ObjC.sel("windowWithContentViewController:");
            defaultDepthLimit = ObjC.sel("defaultDepthLimit");
            allowsAutomaticWindowTabbing = ObjC.sel("allowsAutomaticWindowTabbing");
            setAllowsAutomaticWindowTabbing = ObjC.sel("setAllowsAutomaticWindowTabbing:");
            userTabbingPreference = ObjC.sel("userTabbingPreference");
            standardWindowButton_forStyleMask = ObjC.sel("standardWindowButton:forStyleMask:");
            removeFrameUsingName = ObjC.sel("removeFrameUsingName:");
            subtitle = ObjC.sel("subtitle");
            setSubtitle = ObjC.sel("setSubtitle:");
            toolbarStyle = ObjC.sel("toolbarStyle");
            setToolbarStyle = ObjC.sel("setToolbarStyle:");
            contentLayoutRect = ObjC.sel("contentLayoutRect");
            cascadingReferenceFrame = ObjC.sel("cascadingReferenceFrame");
            titlebarAccessoryViewControllers = ObjC.sel("titlebarAccessoryViewControllers");
            addTitlebarAccessoryViewController = ObjC.sel("addTitlebarAccessoryViewController:");
            insertTitlebarAccessoryViewController_atIndex = ObjC.sel("insertTitlebarAccessoryViewController:atIndex:");
            removeTitlebarAccessoryViewControllerAtIndex = ObjC.sel("removeTitlebarAccessoryViewControllerAtIndex:");
            representedURL = ObjC.sel("representedURL");
            setRepresentedURL = ObjC.sel("setRepresentedURL:");
            representedFilename = ObjC.sel("representedFilename");
            setRepresentedFilename = ObjC.sel("setRepresentedFilename:");
            setTitleWithRepresentedFilename = ObjC.sel("setTitleWithRepresentedFilename:");
            endEditingFor = ObjC.sel("endEditingFor:");
            setFrameTopLeftPoint = ObjC.sel("setFrameTopLeftPoint:");
            inLiveResize = ObjC.sel("inLiveResize");
            resizeIncrements = ObjC.sel("resizeIncrements");
            setResizeIncrements = ObjC.sel("setResizeIncrements:");
            aspectRatio = ObjC.sel("aspectRatio");
            setAspectRatio = ObjC.sel("setAspectRatio:");
            contentResizeIncrements = ObjC.sel("contentResizeIncrements");
            setContentResizeIncrements = ObjC.sel("setContentResizeIncrements:");
            contentAspectRatio = ObjC.sel("contentAspectRatio");
            setContentAspectRatio = ObjC.sel("setContentAspectRatio:");
            contentMinSize = ObjC.sel("contentMinSize");
            setContentMinSize = ObjC.sel("setContentMinSize:");
            contentMaxSize = ObjC.sel("contentMaxSize");
            setContentMaxSize = ObjC.sel("setContentMaxSize:");
            minFullScreenContentSize = ObjC.sel("minFullScreenContentSize");
            setMinFullScreenContentSize = ObjC.sel("setMinFullScreenContentSize:");
            maxFullScreenContentSize = ObjC.sel("maxFullScreenContentSize");
            setMaxFullScreenContentSize = ObjC.sel("setMaxFullScreenContentSize:");
            viewsNeedDisplay = ObjC.sel("viewsNeedDisplay");
            setViewsNeedDisplay = ObjC.sel("setViewsNeedDisplay:");
            displayIfNeeded = ObjC.sel("displayIfNeeded");
            display = ObjC.sel("display");
            preservesContentDuringLiveResize = ObjC.sel("preservesContentDuringLiveResize");
            setPreservesContentDuringLiveResize = ObjC.sel("setPreservesContentDuringLiveResize:");
            update = ObjC.sel("update");
            resizeFlags = ObjC.sel("resizeFlags");
            close = ObjC.sel("close");
            miniaturize = ObjC.sel("miniaturize:");
            deminiaturize = ObjC.sel("deminiaturize:");
            zoom = ObjC.sel("zoom:");
            performMiniaturize = ObjC.sel("performMiniaturize:");
            performZoom = ObjC.sel("performZoom:");
            tryToPerform_with = ObjC.sel("tryToPerform:with:");
            validRequestorForSendType_returnType = ObjC.sel("validRequestorForSendType:returnType:");
            setContentBorderThickness_forEdge = ObjC.sel("setContentBorderThickness:forEdge:");
            contentBorderThicknessForEdge = ObjC.sel("contentBorderThicknessForEdge:");
            setAutorecalculatesContentBorderThickness_forEdge = ObjC.sel("setAutorecalculatesContentBorderThickness:forEdge:");
            autorecalculatesContentBorderThicknessForEdge = ObjC.sel("autorecalculatesContentBorderThicknessForEdge:");
            canHide = ObjC.sel("canHide");
            setCanHide = ObjC.sel("setCanHide:");
            orderBack = ObjC.sel("orderBack:");
            orderWindow_relativeTo = ObjC.sel("orderWindow:relativeTo:");
            orderFrontRegardless = ObjC.sel("orderFrontRegardless");
            miniwindowImage = ObjC.sel("miniwindowImage");
            setMiniwindowImage = ObjC.sel("setMiniwindowImage:");
            miniwindowTitle = ObjC.sel("miniwindowTitle");
            setMiniwindowTitle = ObjC.sel("setMiniwindowTitle:");
            dockTile = ObjC.sel("dockTile");
            makeKeyWindow = ObjC.sel("makeKeyWindow");
            makeMainWindow = ObjC.sel("makeMainWindow");
            becomeKeyWindow = ObjC.sel("becomeKeyWindow");
            resignKeyWindow = ObjC.sel("resignKeyWindow");
            becomeMainWindow = ObjC.sel("becomeMainWindow");
            resignMainWindow = ObjC.sel("resignMainWindow");
            preventsApplicationTerminationWhenModal = ObjC.sel("preventsApplicationTerminationWhenModal");
            setPreventsApplicationTerminationWhenModal = ObjC.sel("setPreventsApplicationTerminationWhenModal:");
            convertRectToScreen = ObjC.sel("convertRectToScreen:");
            convertRectFromScreen = ObjC.sel("convertRectFromScreen:");
            convertRectToBacking = ObjC.sel("convertRectToBacking:");
            convertRectFromBacking = ObjC.sel("convertRectFromBacking:");
            backingScaleFactor = ObjC.sel("backingScaleFactor");
            backingType = ObjC.sel("backingType");
            setBackingType = ObjC.sel("setBackingType:");
            depthLimit = ObjC.sel("depthLimit");
            setDepthLimit = ObjC.sel("setDepthLimit:");
            animationBehavior = ObjC.sel("animationBehavior");
            setAnimationBehavior = ObjC.sel("setAnimationBehavior:");
            isOnActiveSpace = ObjC.sel("isOnActiveSpace");
            occlusionState = ObjC.sel("occlusionState");
            allowsToolTipsWhenApplicationIsInactive = ObjC.sel("allowsToolTipsWhenApplicationIsInactive");
            setAllowsToolTipsWhenApplicationIsInactive = ObjC.sel("setAllowsToolTipsWhenApplicationIsInactive:");
            allowsConcurrentViewDrawing = ObjC.sel("allowsConcurrentViewDrawing");
            setAllowsConcurrentViewDrawing = ObjC.sel("setAllowsConcurrentViewDrawing:");
            displaysWhenScreenProfileChanges = ObjC.sel("displaysWhenScreenProfileChanges");
            setDisplaysWhenScreenProfileChanges = ObjC.sel("setDisplaysWhenScreenProfileChanges:");
            canBecomeVisibleWithoutLogin = ObjC.sel("canBecomeVisibleWithoutLogin");
            setCanBecomeVisibleWithoutLogin = ObjC.sel("setCanBecomeVisibleWithoutLogin:");
            invalidateShadow = ObjC.sel("invalidateShadow");
            appearanceSource = ObjC.sel("appearanceSource");
            setAppearanceSource = ObjC.sel("setAppearanceSource:");
            colorSpace = ObjC.sel("colorSpace");
            setColorSpace = ObjC.sel("setColorSpace:");
            canRepresentDisplayGamut = ObjC.sel("canRepresentDisplayGamut:");
            titlebarSeparatorStyle = ObjC.sel("titlebarSeparatorStyle");
            setTitlebarSeparatorStyle = ObjC.sel("setTitlebarSeparatorStyle:");
            stringWithSavedFrame = ObjC.sel("stringWithSavedFrame");
            setFrameFromString = ObjC.sel("setFrameFromString:");
            saveFrameUsingName = ObjC.sel("saveFrameUsingName:");
            setFrameUsingName_force = ObjC.sel("setFrameUsingName:force:");
            setFrameUsingName = ObjC.sel("setFrameUsingName:");
            windowController = ObjC.sel("windowController");
            setWindowController = ObjC.sel("setWindowController:");
            sheets = ObjC.sel("sheets");
            childWindows = ObjC.sel("childWindows");
            parentWindow = ObjC.sel("parentWindow");
            addChildWindow_ordered = ObjC.sel("addChildWindow:ordered:");
            removeChildWindow = ObjC.sel("removeChildWindow:");
            contentViewController = ObjC.sel("contentViewController");
            setContentViewController = ObjC.sel("setContentViewController:");
            performWindowDragWithEvent = ObjC.sel("performWindowDragWithEvent:");
            toggleFullScreen = ObjC.sel("toggleFullScreen:");
            dataWithEPSInsideRect = ObjC.sel("dataWithEPSInsideRect:");
            dataWithPDFInsideRect = ObjC.sel("dataWithPDFInsideRect:");
            print = ObjC.sel("print:");
            displayLinkWithTarget_selector = ObjC.sel("displayLinkWithTarget:selector:");
            setInitialFirstResponder = ObjC.sel("setInitialFirstResponder:");
            selectNextKeyView = ObjC.sel("selectNextKeyView:");
            selectPreviousKeyView = ObjC.sel("selectPreviousKeyView:");
            selectKeyViewFollowingView = ObjC.sel("selectKeyViewFollowingView:");
            selectKeyViewPrecedingView = ObjC.sel("selectKeyViewPrecedingView:");
            keyViewSelectionDirection = ObjC.sel("keyViewSelectionDirection");
            defaultButtonCell = ObjC.sel("defaultButtonCell");
            setDefaultButtonCell = ObjC.sel("setDefaultButtonCell:");
            disableKeyEquivalentForDefaultButtonCell = ObjC.sel("disableKeyEquivalentForDefaultButtonCell");
            enableKeyEquivalentForDefaultButtonCell = ObjC.sel("enableKeyEquivalentForDefaultButtonCell");
            autorecalculatesKeyViewLoop = ObjC.sel("autorecalculatesKeyViewLoop");
            setAutorecalculatesKeyViewLoop = ObjC.sel("setAutorecalculatesKeyViewLoop:");
            recalculateKeyViewLoop = ObjC.sel("recalculateKeyViewLoop");
            toolbar = ObjC.sel("toolbar");
            setToolbar = ObjC.sel("setToolbar:");
            toggleToolbarShown = ObjC.sel("toggleToolbarShown:");
            runToolbarCustomizationPalette = ObjC.sel("runToolbarCustomizationPalette:");
            tabbingIdentifier = ObjC.sel("tabbingIdentifier");
            setTabbingIdentifier = ObjC.sel("setTabbingIdentifier:");
            selectNextTab = ObjC.sel("selectNextTab:");
            selectPreviousTab = ObjC.sel("selectPreviousTab:");
            moveTabToNewWindow = ObjC.sel("moveTabToNewWindow:");
            mergeAllWindows = ObjC.sel("mergeAllWindows:");
            toggleTabBar = ObjC.sel("toggleTabBar:");
            toggleTabOverview = ObjC.sel("toggleTabOverview:");
            tabbedWindows = ObjC.sel("tabbedWindows");
            addTabbedWindow_ordered = ObjC.sel("addTabbedWindow:ordered:");
            tab = ObjC.sel("tab");
            tabGroup = ObjC.sel("tabGroup");
            hasActiveWindowSharingSession = ObjC.sel("hasActiveWindowSharingSession");
            windowTitlebarLayoutDirection = ObjC.sel("windowTitlebarLayoutDirection");
            nextEventMatchingMask = ObjC.sel("nextEventMatchingMask:");
            nextEventMatchingMask_untilDate_inMode_dequeue = ObjC.sel("nextEventMatchingMask:untilDate:inMode:dequeue:");
            discardEventsMatchingMask_beforeEvent = ObjC.sel("discardEventsMatchingMask:beforeEvent:");
            postEvent_atStart = ObjC.sel("postEvent:atStart:");
            sendEvent = ObjC.sel("sendEvent:");
            currentEvent = ObjC.sel("currentEvent");
            ignoresMouseEvents = ObjC.sel("ignoresMouseEvents");
            setIgnoresMouseEvents = ObjC.sel("setIgnoresMouseEvents:");
            mouseLocationOutsideOfEventStream = ObjC.sel("mouseLocationOutsideOfEventStream");
            disableCursorRects = ObjC.sel("disableCursorRects");
            enableCursorRects = ObjC.sel("enableCursorRects");
            discardCursorRects = ObjC.sel("discardCursorRects");
            areCursorRectsEnabled = ObjC.sel("areCursorRectsEnabled");
            invalidateCursorRectsForView = ObjC.sel("invalidateCursorRectsForView:");
            resetCursorRects = ObjC.sel("resetCursorRects");
            registerForDraggedTypes = ObjC.sel("registerForDraggedTypes:");
            unregisterDraggedTypes = ObjC.sel("unregisterDraggedTypes");
            beginDraggingSessionWithItems_event_source = ObjC.sel("beginDraggingSessionWithItems:event:source:");
            makeFirstResponder = ObjC.sel("makeFirstResponder:");
            firstResponder = ObjC.sel("firstResponder");
            setAcceptsMouseMovedEvents = ObjC.sel("setAcceptsMouseMovedEvents:");
            acceptsMouseMovedEvents = ObjC.sel("acceptsMouseMovedEvents");
            initialFirstResponder = ObjC.sel("initialFirstResponder");
        }
    }

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hSetFrameDisplay, MethodHandle hSetFrameOrigin, MethodHandle hSetContentSize, MethodHandle hStdWinButton, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetSize, MethodHandle hSetSize, MethodHandle hRectRect, MethodHandle hGetPoint, MethodHandle hDoubleInt, MethodHandle hVoidDoubleInt, MethodHandle hBoolInt, MethodHandle hVoidBoolInt, MethodHandle hVoidIdInt, MethodHandle hVoidIntInt, MethodHandle hIdInt, MethodHandle hIdIntInt, MethodHandle hBoolIdId, MethodHandle hIdIdId, MethodHandle hVoidIntId, MethodHandle hIdIntIdIdBool, MethodHandle hVoidIdBool, MethodHandle hBoolIdBool, MethodHandle hBoolId, MethodHandle hIdRect, MethodHandle hIdIdIdId) {}
    private static volatile Handles H;

    protected NSWindow(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSWindow wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSWindow(peer);
    }

    private static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (H != null) return;
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.RECT, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.DOUBLE, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE, Arg.INT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.INT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT, Arg.ID, Arg.ID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID))); 
            Sels.populate();
        H = h;
}

    /// alloc + initWithContentRect:styleMask:backing:defer:.
    public static NSWindow create(NSRect contentRect, long styleMask, long backingStoreType, boolean defer) {
        ensureInit();
        MemorySegment win = ObjC.msgSendId(ObjC.cls("NSWindow"), Sels.alloc);
        win = ObjC.msgSendIdRectLongLongBool(win, Sels.initWithContentRect_styleMask_backing_defer,
                contentRect.toSegment(), styleMask, backingStoreType, defer);
        return new NSWindow(win);
    }

    /// Create an `NSPanel` with the SAME
    /// `initWithContentRect:styleMask:backing:defer:` initializer as
    /// `create`, but from the `NSPanel` subclass. Add
    /// `NSWindowStyleMaskUtilityWindow` (16) to `styleMask` (e.g.
    /// `15L | 16L`) to get AppKit's smaller-title-bar, less-rounded
    /// "settings / utility" panel. Panels also support the behavior properties
    /// `setHidesOnDeactivate` and `setBecomesKeyOnlyIfNeeded`.
    public static NSWindow createPanel(NSRect contentRect, long styleMask, long backingStoreType, boolean defer) {
        ensureInit();
        MemorySegment panel = ObjC.msgSendId(ObjC.cls("NSPanel"), Sels.alloc);
        panel = ObjC.msgSendIdRectLongLongBool(panel, Sels.initWithContentRect_styleMask_backing_defer,
                contentRect.toSegment(), styleMask, backingStoreType, defer);
        return new NSWindow(panel);
    }

    public void setTitle(String title) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setTitle, ObjC.nsstring(title));
    }

    public void center() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.center);
    }

    public void setReleasedWhenClosed(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setReleasedWhenClosed, flag);
    }

    /// [window isReleasedWhenClosed]
    public boolean isReleasedWhenClosed() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isReleasedWhenClosed);
    }

    /// setContentView: replaces the window's root content view.
    public void setContentView(NSView view) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setContentView, view.peer());
    }

    public void setDelegate(NSObject delegate) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setDelegate,
                delegate == null ? MemorySegment.NULL : delegate.peer());
    }

    /// [window delegate]
    public MemorySegment delegate() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.delegate);
    }

    /// Typed delegate.
    public NSObject delegateObject() {
        ensureInit();
        return NSObject.wrap(ObjC.msgSendId(peer, Sels.delegate));
    }

    /// Struct-returning message: frame (objc_msgSend_stret on x86_64).
    public NSRect frame() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.frame));
    }

    /// The vertical offset between WINDOW base coordinates and CONTENT coordinates:
    /// `event.locationInWindow().y` is measured from the window FRAME's
    /// bottom-left (title bar included), while a content view's local origin sits
    /// above the title bar — so the conversion is `viewY = windowY - offset`.
    /// Equals the title-bar height (+ borders); x maps 1:1.
    public double contentOriginOffsetY() {
        ensureInit();
        MemorySegment cv = ObjC.msgSendId(peer, Sels.contentView);
        // Read the slot BEFORE frame(): struct returns share one per-thread
        // slot, so the second call would overwrite cb unread.
        double boundsH = ObjC.rectH(ObjC.msgSendRect(cv, Sels.bounds));
        return frame().height() - boundsH;
    }

    /// setFrame:display: — resize/reposition (and optionally redraw immediately).
    public void setFrameDisplay(NSRect frame, boolean display) {
        ensureInit();
        try {
            H.hSetFrameDisplay().invokeExact(peer, Sels.setFrame_display, frame.toSegment(), display);
        } catch (Throwable t) {
            throw new RuntimeException("setFrame:display: failed", t);
        }
    }

    /// setFrameOrigin: — move the window (fires windowDidMove:).
    public void setFrameOrigin(NSPoint origin) {
        ensureInit();
        try {
            H.hSetFrameOrigin().invokeExact(peer, Sels.setFrameOrigin, origin.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setFrameOrigin: failed", t);
        }
    }

    /// setContentSize: — the content area's size.
    public void setContentSize(NSSize size) {
        ensureInit();
        try {
            H.hSetContentSize().invokeExact(peer, Sels.setContentSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentSize: failed", t);
        }
    }

    // ---------------------------------------------------------------- nested enums — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSWindow.h
    //   NSWindowStyleMask (NS_OPTIONS): grep -A 15 "typedef NS_OPTIONS.*NSWindowStyleMask"
    // Docs: https://developer.apple.com/documentation/appkit/nswindow/stylemask
    // Docs: https://developer.apple.com/documentation/appkit/nswindowtitlevisibility
    // Docs: https://developer.apple.com/documentation/appkit/nswindow/level
    // Docs: https://developer.apple.com/documentation/appkit/nswindow/backingtype (NSGraphics.h)
    // Docs: https://developer.apple.com/documentation/appkit/nswindow/tabbingmode

    /// `NSWindowStyleMask` — bitmask compositional style. Values from `NSWindow.h`.
    /// Source: `NSWindow.h` `typedef NS_OPTIONS(NSUInteger, NSWindowStyleMask)` (SDK MacOSX.sdk)
    /// and https://developer.apple.com/documentation/appkit/nswindow/stylemask
    public enum StyleMask {
        borderless(0),
        titled(1L << 0),
        closable(1L << 1),
        miniaturizable(1L << 2),
        resizable(1L << 3),
        utilityWindow(1L << 4),
        docModalWindow(1L << 6),
        nonactivatingPanel(1L << 7),
        texturedBackground(1L << 8), // deprecated 10.2-11.0
        unifiedTitleAndToolbar(1L << 12),
        hudWindow(1L << 13),
        fullScreen(1L << 14),
        fullSizeContentView(1L << 15);
        public final long value;
        StyleMask(long v) { this.value = v; }
        public static long mask(StyleMask... m) { long r = 0; for (var x : m) r |= x.value; return r; }
        public static StyleMask fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSWindowTitleVisibility` — `NSWindowTitleVisible`=0, `NSWindowTitleHidden`=1.
    /// Source: `NSWindow.h` `typedef NS_ENUM(NSInteger, NSWindowTitleVisibility)` and https://developer.apple.com/documentation/appkit/nswindowtitlevisibility
    public enum TitleVisibility {
        visible(0), hidden(1);
        public final long value;
        TitleVisibility(long v) { this.value = v; }
        public static TitleVisibility fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSWindowLevel` — window stacking levels. Values are `kCGWindowLevel` constants.
    /// Source: `CGWindowLevel.h` (`kCGNormalWindowLevel`=0, `kCGFloatingWindowLevel`=3, `kCGModalPanelWindowLevel`=8, etc.)
    /// and https://developer.apple.com/documentation/appkit/nswindow/level
    public enum WindowLevel {
        normal(0),
        floating(3),
        submenu(3), // kCGTornOffMenuWindowLevel == 3
        tornOffMenu(3),
        mainMenu(24),
        status(25),
        modalPanel(8),
        popUpMenu(101),
        screenSaver(1000),
        dock(20),
        utility(19),
        dragging(500),
        overlay(102),
        help(200);
        public final long value;
        WindowLevel(long v) { this.value = v; }
        public static WindowLevel fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSBackingStoreType` — `NSBackingStoreRetained`=0, `Nonretained`=1, `Buffered`=2.
    /// Source: `NSGraphics.h` `typedef NS_ENUM(NSUInteger, NSBackingStoreType)` and https://developer.apple.com/documentation/appkit/nsbackingstoretype
    public enum BackingStoreType {
        retained(0), nonretained(1), buffered(2);
        public final long value;
        BackingStoreType(long v) { this.value = v; }
        public static BackingStoreType fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    /// `NSWindowTabbingMode` — 0=Automatic, 1=Preferred, 2=Disallowed.
    /// Source: `NSWindow.h` `typedef NS_ENUM(NSInteger, NSWindowTabbingMode)`
    public enum TabbingMode {
        automatic(0), preferred(1), disallowed(2);
        public final long value;
        TabbingMode(long v) { this.value = v; }
        public static TabbingMode fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    // ---------------------------------------------------------------- window "style"

    /// [window styleMask] — the compositional style bit-field (see class Javadoc for bits).
    public long styleMask() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.styleMask);
    }

    /// [window setStyleMask:] — replace the style bit-field (see class Javadoc for bits).
    public void setStyleMask(long mask) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setStyleMask, mask);
    }
    /// Typed overload: `setStyleMask(StyleMask...)` — composes bitmask via `StyleMask.mask`.
    public void setStyleMask(StyleMask... masks) { setStyleMask(StyleMask.mask(masks)); }
    /// Convenience: `create` overload accepting `StyleMask` varargs and `BackingStoreType` enum.
    public static NSWindow create(NSRect contentRect, StyleMask[] styleMasks, BackingStoreType backing, boolean defer) {
        return create(contentRect, StyleMask.mask(styleMasks), backing.value, defer);
    }
    /// Convenience: `create` overload with enum backing.
    public static NSWindow create(NSRect contentRect, long styleMask, BackingStoreType backing, boolean defer) {
        return create(contentRect, styleMask, backing.value, defer);
    }

    /// [window setTitlebarAppearsTransparent:] — modern translucent title bar.
    public void setTitlebarAppearsTransparent(boolean transparent) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setTitlebarAppearsTransparent, transparent);
    }

    /// [window titlebarAppearsTransparent].
    public boolean isTitlebarAppearsTransparent() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.titlebarAppearsTransparent);
    }

    /// [window setTitleVisibility:] — 0 = `NSWindowTitleVisible`,
    /// 1 = `NSWindowTitleHidden`. Only meaningful on a titled window.
    public void setTitleVisibility(long visibility) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setTitleVisibility, visibility);
    }
    /// Typed overload.
    public void setTitleVisibility(TitleVisibility v) { setTitleVisibility(v.value); }

    /// [window setLevel:] — e.g. `NSFloatingWindowLevel`=3, `NSModalPanelWindowLevel`=8.
    public void setLevel(long level) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setLevel, level);
    }
    /// Typed overload.
    public void setLevel(WindowLevel lvl) { setLevel(lvl.value); }

    /// [window level].
    public long level() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.level);
    }
    /// Typed getter — maps raw level to `WindowLevel` enum where known.
    public WindowLevel levelEnum() { return WindowLevel.fromValue(level()); }

    /// [window setCollectionBehavior:] — e.g. `NSWindowCollectionBehaviorCanJoinAllSpaces`.
    public void setCollectionBehavior(long behavior) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setCollectionBehavior, behavior);
    }

    /// [panel setHidesOnDeactivate:] — real `NSPanel` behavior; see class Javadoc.
    public void setHidesOnDeactivate(boolean hide) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setHidesOnDeactivate, hide);
    }

    /// [panel hidesOnDeactivate].
    public boolean hidesOnDeactivate() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.hidesOnDeactivate);
    }

    /// [panel setBecomesKeyOnlyIfNeeded:] — key only while controls need it (NSPanel).
    public void setBecomesKeyOnlyIfNeeded(boolean onlyIfNeeded) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setBecomesKeyOnlyIfNeeded, onlyIfNeeded);
    }

    /// [panel becomesKeyOnlyIfNeeded].
    public boolean becomesKeyOnlyIfNeeded() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.becomesKeyOnlyIfNeeded);
    }

    /// [window standardWindowButton:] — one of the standard close/miniaturize/zoom
    /// buttons. `windowButton`: 0 = `NSWindowCloseButton`, 1 =
    /// `NSWindowMiniaturizeButton`, 2 = `NSWindowZoomButton`.
    ///
    /// Returns the raw peer wrapped as `NSObject`. The native object IS an
    /// `NSButton` subclass (measured here: `_NSThemeCloseWidget`) but its
    /// Java wrapper has no public way to wrap a foreign peer (its constructor is
    /// private) — so callers receive it typed as `NSObject` and may use it via
    /// the ObjC escape hatch or treat it as an opaque id. Non-null for a titled
    /// window's close button.
    ///
    /// AppKit honesty: the private two-argument SPI
    /// `standardWindowButton:forFlag:` (which the `(ID,INT,BOOL)` vocabulary
    /// entry targets) is NOT recognized by the runtime — `respondsToSelector:`
    /// returns `false` and sending it aborts the JVM with
    /// `NSInvalidArgumentException 'unrecognized selector'`. The public, recognized
    /// selector is the single-argument `standardWindowButton:`. This wrapper uses
    /// that, so the `(ID,INT,BOOL)` vocabulary line is currently unused by our code.
    public NSObject standardWindowButton(long windowButton) {
        ensureInit();
        try {
            MemorySegment btn = (MemorySegment) H.hStdWinButton().invokeExact(peer,
                    Sels.standardWindowButton, windowButton);
            return NSObject.wrap(btn);
        } catch (Throwable t) {
            throw new RuntimeException("standardWindowButton: failed", t);
        }
    }

    /// Utility/panel detection. AppKit honesty: the private selector
    /// `isUtilityWindow` is NOT recognized by the runtime (`respondsToSelector:`
    /// returns `false` on both `NSWindow` and `NSPanel`, and sending it
    /// aborts with `NSInvalidArgumentException 'unrecognized selector'`). The public,
    /// recognized predicate for "is this a utility / panel window" is
    /// `isFloatingPanel`, which returns `true` for `NSPanel` instances
    /// and for windows whose styleMask includes `NSWindowStyleMaskUtilityWindow`.
    /// This wrapper queries that selector, so `isUtilityWindow()` == `true`
    /// exactly when the window is a utility panel.
    public boolean isUtilityWindow() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isFloatingPanel);
    }

    // ---------------------------------------------------------------- additional properties — completeness

    /// [window title] — the window title string.
    public String title() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(peer, Sels.title);
        return ObjC.toString(s);
    }

    /// [window titleVisibility] — 0 = NSWindowTitleVisible, 1 = NSWindowTitleHidden.
    public long titleVisibility() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.titleVisibility);
    }
    /// Typed getter.
    public TitleVisibility titleVisibilityEnum() { return TitleVisibility.fromValue(titleVisibility()); }

    /// [window collectionBehavior] — NSWindowCollectionBehavior bit-field.
    public long collectionBehavior() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.collectionBehavior);
    }

    /// [window contentView] — the window's root content view.
    public NSView contentView() {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(peer, Sels.contentView);
        return NSView.wrap(v);
    }

    /// [window isMovable].
    public boolean isMovable() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isMovable);
    }

    /// [window setMovable:].
    public void setMovable(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setMovable, flag);
    }

    /// [window isMovableByWindowBackground].
    public boolean isMovableByWindowBackground() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isMovableByWindowBackground);
    }

    /// [window setMovableByWindowBackground:].
    public void setMovableByWindowBackground(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setMovableByWindowBackground, flag);
    }

    /// [window isExcludedFromWindowsMenu].
    public boolean isExcludedFromWindowsMenu() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isExcludedFromWindowsMenu);
    }

    /// [window setExcludedFromWindowsMenu:].
    public void setExcludedFromWindowsMenu(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setExcludedFromWindowsMenu, flag);
    }

    /// [window tabbingMode] — NSWindowTabbingMode.
    public long tabbingMode() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.tabbingMode);
    }
    /// Typed getter.
    public TabbingMode tabbingModeEnum() { return TabbingMode.fromValue(tabbingMode()); }

    /// [window setTabbingMode:].
    public void setTabbingMode(long mode) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setTabbingMode, mode);
    }
    /// Typed overload.
    public void setTabbingMode(TabbingMode mode) { setTabbingMode(mode.value); }

    /// [window backgroundColor] — may be nil.
    public NSColor backgroundColor() {
        ensureInit();
        MemorySegment c = ObjC.msgSendId(peer, Sels.backgroundColor);
        return NSColor.wrap(c);
    }

    /// [window setBackgroundColor:].
    public void setBackgroundColor(NSColor color) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setBackgroundColor, (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// [window isOpaque].
    public boolean isOpaque() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isOpaque);
    }

    /// [window setOpaque:].
    public void setOpaque(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setOpaque, flag);
    }

    /// [window hasShadow].
    public boolean hasShadow() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.hasShadow);
    }

    /// [window setHasShadow:].
    public void setHasShadow(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setHasShadow, flag);
    }

    /// [window alphaValue] — 0.0 to 1.0.
    public double alphaValue() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, Sels.alphaValue);
        } catch (Throwable t) {
            throw new RuntimeException("alphaValue failed", t);
        }
    }

    /// [window setAlphaValue:].
    public void setAlphaValue(double alpha) {
        ensureInit();
        try {
            H.hSetDouble().invokeExact(peer, Sels.setAlphaValue, alpha);
        } catch (Throwable t) {
            throw new RuntimeException("setAlphaValue: failed", t);
        }
    }

    /// [window minSize] — NSSize.
    public NSSize minSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.minSize);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("minSize failed", t);
        }
    }

    /// [window setMinSize:].
    public void setMinSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setMinSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMinSize: failed", t);
        }
    }

    /// [window maxSize] — NSSize.
    public NSSize maxSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.maxSize);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("maxSize failed", t);
        }
    }

    /// [window setMaxSize:].
    public void setMaxSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setMaxSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMaxSize: failed", t);
        }
    }

    /// [window frameAutosaveName] — may be nil/empty.
    public String frameAutosaveName() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(peer, Sels.frameAutosaveName);
        return ObjC.toString(s);
    }

    /// [window setFrameAutosaveName:] -- returns the native BOOL (NO if another
    /// window already owns the name).
    public boolean setFrameAutosaveName(String name) {
        ensureInit();
        try {
            return (boolean) H.hBoolId().invokeExact(peer, Sels.setFrameAutosaveName, (MemorySegment) ObjC.nsstring(name));
        } catch (Throwable t) { throw new RuntimeException("setFrameAutosaveName: failed", t); }
    }

    /// [window isDocumentEdited].
    public boolean isDocumentEdited() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isDocumentEdited);
    }

    /// [window setDocumentEdited:].
    public void setDocumentEdited(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setDocumentEdited, flag);
    }

    public long windowNumber() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.windowNumber);
    }

    public boolean isVisible() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isVisible);
    }

    public boolean isKeyWindow() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isKeyWindow);
    }

    public boolean isMainWindow() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isMainWindow);
    }

    public void makeKeyAndOrderFront(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.makeKeyAndOrderFront, (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
    }

    /// orderFront: — show without making key: the window becomes visible but
    /// never activates the app and never steals focus. The unobtrusive
    /// counterpart to makeKeyAndOrderFront: (which keys + activates).
    public void orderFront(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.orderFront, (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
    }

    /// Window sharing types (NSWindowSharingType): whether other processes
    /// (screen capture, sharing) may read the contents. Default is READ_ONLY.
    public static final long SHARING_NONE = 0;
    public static final long SHARING_READ_ONLY = 1;

    /// sharingType.
    public long sharingType() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.sharingType);
    }

    /// setSharingType: — NONE excludes the window from capture/sharing.
    /// NOTE (measured on Tahoe): setting NONE sticks — a later set back to
    /// READ_ONLY still reads NONE on a live window. Decide at creation time.
    public void setSharingType(long type) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setSharingType, type);
    }

    public void performClose(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.performClose, (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
    }

    // ---------------------------------------------------------------- sheets (modal sheet inside window, blocks window)

    /// beginSheet:completionHandler: — attach sheet to receiver; handler receives NSModalResponse.
    public void beginSheet(NSWindow sheet, java.util.function.IntConsumer completionHandler) {
        ensureInit();
        if (sheet == null) return;
        try {
            MemorySegment block;
            if (completionHandler == null) {
                block = MemorySegment.NULL;
            } else {
                java.lang.invoke.MethodHandle target = java.lang.invoke.MethodHandles.lookup().findStatic(
                        NSWindow.class, "sheetCompletionBridge",
                        java.lang.invoke.MethodType.methodType(void.class, MemorySegment.class, long.class, java.util.function.IntConsumer.class));
                java.lang.invoke.MethodHandle bound = java.lang.invoke.MethodHandles.insertArguments(target, 2, completionHandler);
                // block signature: void(^)(NSModalResponse) -> void with blockSelf leading
                java.lang.foreign.FunctionDescriptor fd = java.lang.foreign.FunctionDescriptor.ofVoid(
                        (java.lang.foreign.ValueLayout) java.lang.foreign.Linker.nativeLinker().canonicalLayouts().get("void*"),
                        (java.lang.foreign.ValueLayout) java.lang.foreign.Linker.nativeLinker().canonicalLayouts().get("long"));
                // Blocks.block expects leading PTR param + user args; wrap to (PTR, long) -> void
                java.lang.invoke.MethodHandle adapted = bound.asType(java.lang.invoke.MethodType.methodType(void.class, MemorySegment.class, long.class));
                block = nsui.objc.Blocks.block(adapted, java.lang.foreign.FunctionDescriptor.ofVoid(
                        (java.lang.foreign.ValueLayout) java.lang.foreign.Linker.nativeLinker().canonicalLayouts().get("void*"),
                        (java.lang.foreign.ValueLayout) java.lang.foreign.Linker.nativeLinker().canonicalLayouts().get("long")));
            }
            java.lang.invoke.MethodHandle h = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.VOID, nsui.objc.Sig.Arg.ID, nsui.objc.Sig.Arg.ID));
            MemorySegment blk = (block == null || block.address() == 0) ? MemorySegment.NULL : block;
            h.invokeExact(peer, Sels.beginSheet_completionHandler, sheet.peer(), (MemorySegment) blk);
        } catch (Throwable t) {
            throw new RuntimeException("beginSheet:completionHandler: failed", t);
        }
    }

    /// beginSheet:completionHandler: with raw block segment (for advanced use).
    public void beginSheet(NSWindow sheet, MemorySegment completionHandlerBlock) {
        ensureInit();
        if (sheet == null) return;
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.VOID, nsui.objc.Sig.Arg.ID, nsui.objc.Sig.Arg.ID));
            MemorySegment blk2 = (completionHandlerBlock == null || completionHandlerBlock.address() == 0) ? MemorySegment.NULL : completionHandlerBlock;
            h.invokeExact(peer, Sels.beginSheet_completionHandler, sheet.peer(), (MemorySegment) blk2);
        } catch (Throwable t) {
            throw new RuntimeException("beginSheet:completionHandler: failed", t);
        }
    }

    private static void sheetCompletionBridge(MemorySegment blockSelf, long response, java.util.function.IntConsumer handler) {
        handler.accept((int) response);
    }

    /// endSheet: — dismiss sheet.
    public void endSheet(NSWindow sheet) {
        ensureInit();
        if (sheet == null) return;
        ObjC.msgSendVoidId(peer, Sels.endSheet, sheet.peer());
    }

    /// endSheet:returnCode:
    public void endSheet(NSWindow sheet, long returnCode) {
        ensureInit();
        if (sheet == null) return;
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.VOID, nsui.objc.Sig.Arg.ID, nsui.objc.Sig.Arg.INT));
            h.invokeExact(peer, Sels.endSheet_returnCode, sheet.peer(), returnCode);
        } catch (Throwable t) {
            throw new RuntimeException("endSheet:returnCode: failed", t);
        }
    }

    /// attachedSheet — current sheet or null.
    public NSWindow attachedSheet() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(peer, Sels.attachedSheet);
        return (s == null || s.address() == 0) ? null : new NSWindow(s);
    }

    /// isSheet
    public boolean isSheet() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isSheet);
    }

    /// sheetParent
    public NSWindow sheetParent() {
        ensureInit();
        MemorySegment s = ObjC.msgSendId(peer, Sels.sheetParent);
        return (s == null || s.address() == 0) ? null : new NSWindow(s);
    }

    /// orderOut:
    public void orderOut(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.orderOut, sender == null ? MemorySegment.NULL : sender.peer());
    }

    // ---- additional readonly completeness ----
    public boolean isZoomed() {
    ensureInit(); return ObjC.msgSendBool(peer, Sels.isZoomed); }
    public boolean isMiniaturized() {
    ensureInit(); return ObjC.msgSendBool(peer, Sels.isMiniaturized); }
    public boolean canBecomeKeyWindow() {
    ensureInit(); return ObjC.msgSendBool(peer, Sels.canBecomeKeyWindow); }
    public boolean canBecomeMainWindow() {
    ensureInit(); return ObjC.msgSendBool(peer, Sels.canBecomeMainWindow); }
    public boolean worksWhenModal() {
    ensureInit(); return ObjC.msgSendBool(peer, Sels.worksWhenModal); }
    public MemorySegment screen() {
    ensureInit(); return ObjC.msgSendId(peer, Sels.screen); }
    public boolean hasDynamicDepthLimit() {
ensureInit(); return ObjC.msgSendBool(peer, Sels.hasDynamicDepthLimit); }

    /// [window screen] typed — the screen the window is on (null if offscreen).
    public NSScreen screenObject() {
        ensureInit();
        return NSScreen.wrap(ObjC.msgSendId(peer, Sels.screen));
    }

    /// [window deepestScreen] — raw peer of the deepest screen the window is on.
    public MemorySegment deepestScreen() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.deepestScreen);
    }

    /// [window deepestScreen] typed (null if offscreen).
    public NSScreen deepestScreenObject() {
        ensureInit();
        return NSScreen.wrap(ObjC.msgSendId(peer, Sels.deepestScreen));
    }

    /// [window deviceDescription] — display device dictionary for the window's screen.
    public NSDictionary deviceDescription() {
        ensureInit();
        return NSDictionary.wrap(ObjC.msgSendId(peer, Sels.deviceDescription));
    }

    /// [window setDynamicDepthLimit:].
    public void setDynamicDepthLimit(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setDynamicDepthLimit, flag);
    }

    // ---------------------------------------------------------------- window coverage batch:
    // getters/setters/actions, window lists, screen info (SDK: NSWindow.h).
    //
    // Every method below uses a shape already registered in Sig.VOCABULARY.
    // OMITTED with reasons (header wins on API truth):
    // - NSWindowDelegate protocol methods — need upcall machinery (per batch rules).
    // - block-taking methods: beginCriticalSheet:completionHandler:,
    //   transferWindowSharingToWindow:completionHandler:,
    //   requestSharingOfWindow:completionHandler:,
    //   requestSharingOfWindowUsingPreview:title:completionHandler:,
    //   trackEventsMatchingMask:timeout:mode:handler: — blocks need trampolines.
    // - deprecated: texturedBackground style use, cacheImageInRect:/restoreCachedImage/
    //   discardCachedImage, menuChanged:, gState, convertBaseToScreen:/convertScreenToBase:,
    //   userSpaceScaleFactor, useOptimizedDrawing:, canStoreColor, disable/enableFlushWindow,
    //   flushWindow/flushWindowIfNeeded, autodisplay, graphicsContext, oneShot,
    //   preferredBackingLocation/backingLocation, showsResizeIndicator,
    //   initWithWindowRef:/windowRef, disableScreenUpdatesUntilFlush,
    //   dragImage:at:offset:event:pasteboard:source:slideBack:, showsToolbarButton.
    // - init overloads covered by create(): initWithCoder: (NS_UNAVAILABLE anyway).
    // - initWithContentRect:styleMask:backing:defer:screen: — needs
    //   shape of(ID,RECT,INT,INT,BOOL,ID), not registered (requested below).
    // - frameRectForContentRect:styleMask: / contentRectForFrameRect:styleMask:
    //   (class + instance) — need shape of(RECT,RECT,INT), not registered.
    // - minFrameWidthWithTitle:styleMask: — needs of(DOUBLE,ID,INT), not registered.
    // - cascadeTopLeftFromPoint: — needs of(POINT,POINT), not registered.
    // - animationResizeTime: — needs of(DOUBLE,RECT), not registered.
    // - setFrame:display:animate: — needs of(VOID,RECT,BOOL,BOOL), not registered.
    // - constrainFrameRect:toScreen: — needs of(RECT,RECT,ID), not registered.
    // - fieldEditor:forObject: — needs of(ID,BOOL,ID), not registered.
    // - convertPointToScreen:/convertPointFromScreen:/convertPointToBacking:/
    //   convertPointFromBacking: — need of(POINT,POINT), not registered.
    // - backingAlignedRect:options: — needs of(RECT,RECT,INT), not registered.
    // - windowNumberAtPoint:belowWindowWithWindowNumber: — needs
    //   of(INT,POINT,INT), not registered.
    // - setFrameAutosaveName: natively returns BOOL; the existing void wrapper
    //   discards it (kept for compatibility).
    // - parentWindow setter omitted: header property is effectively derived from
    //   addChildWindow:/removeChildWindow: (getter only here).
    //
    // REQUESTED Sig shapes (absent from Sig.java, needed for the omissions above):
    //   of(Ret.RECT, Arg.RECT, Arg.INT), of(Ret.DOUBLE, Arg.ID, Arg.INT),
    //   of(Ret.POINT, Arg.POINT), of(Ret.DOUBLE, Arg.RECT),
    //   of(Ret.VOID, Arg.RECT, Arg.BOOL, Arg.BOOL), of(Ret.RECT, Arg.RECT, Arg.ID),
    //   of(Ret.ID, Arg.BOOL, Arg.ID), of(Ret.INT, Arg.POINT, Arg.INT),
    //   of(Ret.ID, Arg.RECT, Arg.INT, Arg.INT, Arg.BOOL, Arg.ID).

    /// NSWindowOrderingMode for orderWindow:relativeTo: / addChildWindow:ordered:.
    public static final long ORDER_OUT = -1;
    public static final long ORDER_BELOW = 0;
    public static final long ORDER_ABOVE = 1;

    /// NSWindowButton ids for standardWindowButton:.
    public static final long BUTTON_CLOSE = 0;
    public static final long BUTTON_MINIATURIZE = 1;
    public static final long BUTTON_ZOOM = 2;
    public static final long BUTTON_TOOLBAR = 3;
    public static final long BUTTON_DOCUMENT_ICON = 4;

    // ---- class-side window lists and defaults ----

    /// +windowNumbersWithOptions: — window numbers of visible windows matching
    /// `NSWindowNumberListOptions` (0 = calling app, active space).
    public static NSArray windowNumbersWithOptions(long options) {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) H.hIdInt().invokeExact(
                    ObjC.cls("NSWindow"), Sels.windowNumbersWithOptions, options);
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("windowNumbersWithOptions: failed", t);
        }
    }

    /// +windowWithContentViewController: — titled window hosting the controller.
    public static NSWindow windowWithContentViewController(NSViewController controller) {
        ensureInit();
        MemorySegment w = ObjC.msgSendIdId(ObjC.cls("NSWindow"),
                Sels.windowWithContentViewController,
                controller == null ? MemorySegment.NULL : controller.peer());
        return wrap(w);
    }

    /// +defaultDepthLimit — class default window depth limit.
    public static long defaultDepthLimit() {
        ensureInit();
        return ObjC.msgSendLong(ObjC.cls("NSWindow"), Sels.defaultDepthLimit);
    }

    /// +allowsAutomaticWindowTabbing / setAllowsAutomaticWindowTabbing:.
    public static boolean allowsAutomaticWindowTabbing() {
        ensureInit();
        return ObjC.msgSendBool(ObjC.cls("NSWindow"), Sels.allowsAutomaticWindowTabbing);
    }

    /// +setAllowsAutomaticWindowTabbing:.
    public static void setAllowsAutomaticWindowTabbing(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(ObjC.cls("NSWindow"), Sels.setAllowsAutomaticWindowTabbing, flag);
    }

    /// +userTabbingPreference — system tabbing preference (readonly).
    public static long userTabbingPreference() {
        ensureInit();
        return ObjC.msgSendLong(ObjC.cls("NSWindow"), Sels.userTabbingPreference);
    }

    /// +standardWindowButton:forStyleMask: — the canonical button for a style mask.
    public static NSObject standardWindowButtonForStyleMask(long button, long styleMask) {
        ensureInit();
        try {
            MemorySegment btn = (MemorySegment) H.hIdIntInt().invokeExact(
                    ObjC.cls("NSWindow"), Sels.standardWindowButton_forStyleMask, button, styleMask);
            return NSObject.wrap(btn);
        } catch (Throwable t) {
            throw new RuntimeException("standardWindowButton:forStyleMask: failed", t);
        }
    }

    /// +removeFrameUsingName: — forget a saved frame.
    public static void removeFrameUsingName(String name) {
        ensureInit();
        ObjC.msgSendVoidId(ObjC.cls("NSWindow"), Sels.removeFrameUsingName, ObjC.nsstring(name));
    }

    // ---- title / subtitle / toolbar style / layout rects ----

    /// [window subtitle] (macOS 11+) — secondary title text, may be nil.
    public String subtitle() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.subtitle));
    }

    /// [window setSubtitle:] — AppKit honesty: unlike `setTitle:`, this selector
    /// REJECTS nil (`NSInternalInconsistencyException 'subtitle != nil'`, which
    /// aborts the JVM). A Java null is therefore mapped to `""` (visually empty).
    public void setSubtitle(String subtitle) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setSubtitle,
                ObjC.nsstring(subtitle == null ? "" : subtitle));
    }

    /// [window toolbarStyle] — NSWindowToolbarStyle (macOS 11+).
    public long toolbarStyle() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.toolbarStyle);
    }

    /// [window setToolbarStyle:].
    public void setToolbarStyle(long style) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setToolbarStyle, style);
    }

    /// [window contentLayoutRect] — layout area for the content (macOS 10.10+).
    public NSRect contentLayoutRect() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.contentLayoutRect));
    }

    /// [window cascadingReferenceFrame] — reference frame for cascading (macOS 15+).
    public NSRect cascadingReferenceFrame() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.cascadingReferenceFrame));
    }

    /// [window titlebarAccessoryViewControllers] — accessory controllers (may be empty).
    public NSArray titlebarAccessoryViewControllers() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.titlebarAccessoryViewControllers));
    }

    /// [window addTitlebarAccessoryViewController:].
    public void addTitlebarAccessoryViewController(NSObject childViewController) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.addTitlebarAccessoryViewController,
                childViewController == null ? MemorySegment.NULL : childViewController.peer());
    }

    /// [window insertTitlebarAccessoryViewController:atIndex:].
    public void insertTitlebarAccessoryViewControllerAtIndex(NSObject childViewController, long index) {
        ensureInit();
        try {
            H.hVoidIdInt().invokeExact(peer, Sels.insertTitlebarAccessoryViewController_atIndex,
                    (MemorySegment) (childViewController == null ? MemorySegment.NULL : childViewController.peer()), index);
        } catch (Throwable t) {
            throw new RuntimeException("insertTitlebarAccessoryViewController:atIndex: failed", t);
        }
    }

    /// [window removeTitlebarAccessoryViewControllerAtIndex:].
    public void removeTitlebarAccessoryViewControllerAtIndex(long index) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.removeTitlebarAccessoryViewControllerAtIndex, index);
    }

    // ---- represented file ----

    /// [window representedURL] — raw peer (no NSURL wrapper yet), may be nil.
    public MemorySegment representedURL() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.representedURL);
    }

    /// [window setRepresentedURL:] — nil clears.
    public void setRepresentedURL(MemorySegment url) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setRepresentedURL,
                url == null ? MemorySegment.NULL : url);
    }

    /// [window representedFilename] — may be nil/empty.
    public String representedFilename() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.representedFilename));
    }

    /// [window setRepresentedFilename:] — must be non-null (a Java null throws
    /// in `nsstring` before reaching AppKit, which likewise requires non-nil).
    public void setRepresentedFilename(String filename) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setRepresentedFilename, ObjC.nsstring(filename));
    }

    /// [window setTitleWithRepresentedFilename:] — title follows the file name.
    public void setTitleWithRepresentedFilename(String filename) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setTitleWithRepresentedFilename, ObjC.nsstring(filename));
    }

    // ---- frame geometry extras ----

    /// [window endEditingFor:] — end any editing session for `object` (nil-safe).
    public void endEditingFor(NSObject object) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.endEditingFor,
                object == null ? MemorySegment.NULL : object.peer());
    }

    /// [window setFrameTopLeftPoint:] — position by top-left corner.
    public void setFrameTopLeftPoint(NSPoint point) {
        ensureInit();
        try {
            H.hSetFrameOrigin().invokeExact(peer, Sels.setFrameTopLeftPoint, point.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setFrameTopLeftPoint: failed", t);
        }
    }

    /// [window isInLiveResize] (macOS 10.6+).
    public boolean isInLiveResize() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.inLiveResize);
    }

    /// [window resizeIncrements] / [window setResizeIncrements:].
    public NSSize resizeIncrements() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.resizeIncrements);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("resizeIncrements failed", t);
        }
    }

    /// [window setResizeIncrements:].
    public void setResizeIncrements(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setResizeIncrements, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setResizeIncrements: failed", t);
        }
    }

    /// [window aspectRatio] / [window setAspectRatio:].
    public NSSize aspectRatio() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.aspectRatio);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("aspectRatio failed", t);
        }
    }

    /// [window setAspectRatio:] — AppKit honesty: setting (0,0) after a nonzero
    /// ratio poisons the constraint state — the NEXT setFrame* then traps
    /// silently (SIGTRAP, measured). Restore to a valid ratio like (1,1).
    public void setAspectRatio(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setAspectRatio, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setAspectRatio: failed", t);
        }
    }

    /// [window contentResizeIncrements] / [window setContentResizeIncrements:].
    public NSSize contentResizeIncrements() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.contentResizeIncrements);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentResizeIncrements failed", t);
        }
    }

    /// [window setContentResizeIncrements:].
    public void setContentResizeIncrements(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setContentResizeIncrements, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentResizeIncrements: failed", t);
        }
    }

    /// [window contentAspectRatio] / [window setContentAspectRatio:].
    public NSSize contentAspectRatio() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.contentAspectRatio);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentAspectRatio failed", t);
        }
    }

    /// [window setContentAspectRatio:] — AppKit honesty: like `setAspectRatio:`,
    /// setting (0,0) after a nonzero ratio poisons the constraint state and the
    /// next setFrame* traps silently (SIGTRAP, measured). Restore to (1,1).
    public void setContentAspectRatio(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setContentAspectRatio, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentAspectRatio: failed", t);
        }
    }

    /// [window contentMinSize] / [window setContentMinSize:].
    public NSSize contentMinSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.contentMinSize);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentMinSize failed", t);
        }
    }

    /// [window setContentMinSize:].
    public void setContentMinSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setContentMinSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentMinSize: failed", t);
        }
    }

    /// [window contentMaxSize] / [window setContentMaxSize:].
    public NSSize contentMaxSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.contentMaxSize);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("contentMaxSize failed", t);
        }
    }

    /// [window setContentMaxSize:].
    public void setContentMaxSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setContentMaxSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setContentMaxSize: failed", t);
        }
    }

    /// [window minFullScreenContentSize] / setter (macOS 10.11+).
    public NSSize minFullScreenContentSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.minFullScreenContentSize);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("minFullScreenContentSize failed", t);
        }
    }

    /// [window setMinFullScreenContentSize:].
    public void setMinFullScreenContentSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setMinFullScreenContentSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMinFullScreenContentSize: failed", t);
        }
    }

    /// [window maxFullScreenContentSize] / setter (macOS 10.11+).
    public NSSize maxFullScreenContentSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.maxFullScreenContentSize);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("maxFullScreenContentSize failed", t);
        }
    }

    /// [window setMaxFullScreenContentSize:].
    public void setMaxFullScreenContentSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setMaxFullScreenContentSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setMaxFullScreenContentSize: failed", t);
        }
    }

    // ---- display / drawing ----

    /// [window viewsNeedDisplay] / [window setViewsNeedDisplay:].
    public boolean viewsNeedDisplay() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.viewsNeedDisplay);
    }

    /// [window setViewsNeedDisplay:].
    public void setViewsNeedDisplay(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setViewsNeedDisplay, flag);
    }

    /// [window displayIfNeeded] — redraw only views marked dirty.
    public void displayIfNeeded() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.displayIfNeeded);
    }

    /// [window display] — redraw now.
    public void display() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.display);
    }

    /// [window preservesContentDuringLiveResize] / setter.
    public boolean preservesContentDuringLiveResize() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.preservesContentDuringLiveResize);
    }

    /// [window setPreservesContentDuringLiveResize:].
    public void setPreservesContentDuringLiveResize(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setPreservesContentDuringLiveResize, flag);
    }

    /// [window update] — refresh dirty state.
    public void update() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.update);
    }

    /// [window resizeFlags] — modifiers held when resizing started.
    public long resizeFlags() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.resizeFlags);
    }

    // ---- close / miniaturize / zoom actions ----

    /// [window close] — close now (delegate `windowShouldClose:` still consulted).
    public void close() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.close);
    }

    /// [window miniaturize:] — sender may be null.
    public void miniaturize(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.miniaturize, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window deminiaturize:] — sender may be null.
    public void deminiaturize(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.deminiaturize, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window zoom:] — sender may be null.
    public void zoom(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.zoom, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window performMiniaturize:] — menu-action variant, sender may be null.
    public void performMiniaturize(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.performMiniaturize, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window performZoom:] — menu-action variant, sender may be null.
    public void performZoom(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.performZoom, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window tryToPerform:with:] — route an action; returns whether handled.
    public boolean tryToPerform(MemorySegment action, NSObject object) {
        ensureInit();
        try {
            return (boolean) H.hBoolIdId().invokeExact(peer, Sels.tryToPerform_with,
                    action, (MemorySegment) (object == null ? MemorySegment.NULL : object.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("tryToPerform:with: failed", t);
        }
    }

    /// [window validRequestorForSendType:returnType:] — raw peer, may be nil.
    public MemorySegment validRequestorForSendType(MemorySegment sendType, MemorySegment returnType) {
        ensureInit();
        try {
            return (MemorySegment) H.hIdIdId().invokeExact(peer,
                    Sels.validRequestorForSendType_returnType,
                    (MemorySegment) (sendType == null ? MemorySegment.NULL : sendType),
                    (MemorySegment) (returnType == null ? MemorySegment.NULL : returnType));
        } catch (Throwable t) {
            throw new RuntimeException("validRequestorForSendType:returnType: failed", t);
        }
    }

    // ---- content borders ----

    /// [window setContentBorderThickness:forEdge:] — `edge`: 1=minY (bottom).
    /// AppKit honesty: 0/2/3 (left/right/top) RAISE (`NSInvalidArgumentException`,
    /// aborts the JVM) on a standard window — measured: only the bottom edge
    /// accepts a thickness (top was for textured windows, deprecated since 10.2).
    public void setContentBorderThicknessForEdge(double thickness, long edge) {
        ensureInit();
        try {
            H.hVoidDoubleInt().invokeExact(peer, Sels.setContentBorderThickness_forEdge, thickness, edge);
        } catch (Throwable t) {
            throw new RuntimeException("setContentBorderThickness:forEdge: failed", t);
        }
    }

    /// [window contentBorderThicknessForEdge:].
    public double contentBorderThicknessForEdge(long edge) {
        ensureInit();
        try {
            return (double) H.hDoubleInt().invokeExact(peer, Sels.contentBorderThicknessForEdge, edge);
        } catch (Throwable t) {
            throw new RuntimeException("contentBorderThicknessForEdge: failed", t);
        }
    }

    /// [window setAutorecalculatesContentBorderThickness:forEdge:].
    public void setAutorecalculatesContentBorderThicknessForEdge(boolean flag, long edge) {
        ensureInit();
        try {
            H.hVoidBoolInt().invokeExact(peer, Sels.setAutorecalculatesContentBorderThickness_forEdge, flag, edge);
        } catch (Throwable t) {
            throw new RuntimeException("setAutorecalculatesContentBorderThickness:forEdge: failed", t);
        }
    }

    /// [window autorecalculatesContentBorderThicknessForEdge:].
    public boolean autorecalculatesContentBorderThicknessForEdge(long edge) {
        ensureInit();
        try {
            return (boolean) H.hBoolInt().invokeExact(peer, Sels.autorecalculatesContentBorderThicknessForEdge, edge);
        } catch (Throwable t) {
            throw new RuntimeException("autorecalculatesContentBorderThicknessForEdge: failed", t);
        }
    }

    // ---- ordering / visibility ----

    /// [window canHide] / [window setCanHide:].
    public boolean canHide() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.canHide);
    }

    /// [window setCanHide:].
    public void setCanHide(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setCanHide, flag);
    }

    /// [window orderBack:] — send behind all windows, sender may be null.
    /// AppKit honesty: this SHOWS the window (at the back) — measured
    /// `isVisible()==true` afterwards. Park offscreen first for unobtrusive use.
    public void orderBack(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.orderBack, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window orderWindow:relativeTo:] — `place`: -1=out, 0=below, 1=above.
    /// AppKit honesty: ordering OUT with `otherWin`=0 left the window visible in
    /// measurement — pass a real sibling window number, or use `orderOut:`.
    public void orderWindowRelativeTo(long place, long otherWindowNumber) {
        ensureInit();
        try {
            H.hVoidIntInt().invokeExact(peer, Sels.orderWindow_relativeTo, place, otherWindowNumber);
        } catch (Throwable t) {
            throw new RuntimeException("orderWindow:relativeTo: failed", t);
        }
    }

    /// [window orderFrontRegardless] — show even for a non-active app.
    public void orderFrontRegardless() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.orderFrontRegardless);
    }

    /// [window miniwindowImage] — custom miniaturized image, may be nil.
    public NSImage miniwindowImage() {
        ensureInit();
        return NSImage.wrap(ObjC.msgSendId(peer, Sels.miniwindowImage));
    }

    /// [window setMiniwindowImage:] — nil restores the default snapshot.
    public void setMiniwindowImage(NSImage image) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setMiniwindowImage,
                image == null ? MemorySegment.NULL : image.peer());
    }

    /// [window miniwindowTitle] — custom miniaturized title, may be nil.
    public String miniwindowTitle() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.miniwindowTitle));
    }

    /// [window setMiniwindowTitle:] — nil restores the window title.
    public void setMiniwindowTitle(String title) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setMiniwindowTitle,
                title == null ? MemorySegment.NULL : ObjC.nsstring(title));
    }

    /// [window dockTile] — the window's Dock tile (miniaturized windows).
    public NSDockTile dockTile() {
        ensureInit();
        return NSDockTile.wrap(ObjC.msgSendId(peer, Sels.dockTile));
    }

    // ---- key / main actions ----

    /// [window makeKeyWindow].
    public void makeKeyWindow() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.makeKeyWindow);
    }

    /// [window makeMainWindow].
    public void makeMainWindow() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.makeMainWindow);
    }

    /// [window becomeKeyWindow].
    public void becomeKeyWindow() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.becomeKeyWindow);
    }

    /// [window resignKeyWindow].
    public void resignKeyWindow() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.resignKeyWindow);
    }

    /// [window becomeMainWindow].
    public void becomeMainWindow() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.becomeMainWindow);
    }

    /// [window resignMainWindow].
    public void resignMainWindow() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.resignMainWindow);
    }

    /// [window preventsApplicationTerminationWhenModal] / setter (macOS 10.6+).
    public boolean preventsApplicationTerminationWhenModal() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.preventsApplicationTerminationWhenModal);
    }

    /// [window setPreventsApplicationTerminationWhenModal:].
    public void setPreventsApplicationTerminationWhenModal(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setPreventsApplicationTerminationWhenModal, flag);
    }

    // ---- coordinate conversion / backing store ----

    /// [window convertRectToScreen:] — window-base rect to screen coordinates.
    public NSRect convertRectToScreen(NSRect rect) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, Sels.convertRectToScreen, rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectToScreen: failed", t);
        }
    }

    /// [window convertRectFromScreen:] — screen rect to window-base coordinates.
    public NSRect convertRectFromScreen(NSRect rect) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, Sels.convertRectFromScreen, rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectFromScreen: failed", t);
        }
    }

    /// [window convertRectToBacking:] — points to backing pixels (macOS 10.7+).
    public NSRect convertRectToBacking(NSRect rect) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, Sels.convertRectToBacking, rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectToBacking: failed", t);
        }
    }

    /// [window convertRectFromBacking:] — backing pixels to points (macOS 10.7+).
    public NSRect convertRectFromBacking(NSRect rect) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hRectRect().invokeExact(
                    ObjC.structSlot(), peer, Sels.convertRectFromBacking, rect.toSegment());
            return NSRect.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertRectFromBacking: failed", t);
        }
    }

    /// [window backingScaleFactor] — points-to-pixels multiplier (macOS 10.7+).
    public double backingScaleFactor() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, Sels.backingScaleFactor);
        } catch (Throwable t) {
            throw new RuntimeException("backingScaleFactor failed", t);
        }
    }

    // ---- backing / depth / appearance ----

    /// [window backingType] — NSBackingStoreType.
    public long backingType() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.backingType);
    }

    /// [window setBackingType:].
    public void setBackingType(long type) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setBackingType, type);
    }

    /// [window depthLimit].
    public long depthLimit() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.depthLimit);
    }

    /// [window setDepthLimit:].
    public void setDepthLimit(long limit) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setDepthLimit, limit);
    }

    /// [window animationBehavior] (macOS 10.7+) — 0=default, 1=none, ...5=alertPanel.
    public long animationBehavior() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.animationBehavior);
    }

    /// [window setAnimationBehavior:].
    public void setAnimationBehavior(long behavior) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setAnimationBehavior, behavior);
    }

    /// [window isOnActiveSpace] (macOS 10.6+).
    public boolean isOnActiveSpace() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isOnActiveSpace);
    }

    /// [window occlusionState] (macOS 10.9+) — NSWindowOcclusionState bit-field.
    public long occlusionState() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.occlusionState);
    }

    /// [window allowsToolTipsWhenApplicationIsInactive] / setter.
    public boolean allowsToolTipsWhenApplicationIsInactive() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsToolTipsWhenApplicationIsInactive);
    }

    /// [window setAllowsToolTipsWhenApplicationIsInactive:].
    public void setAllowsToolTipsWhenApplicationIsInactive(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAllowsToolTipsWhenApplicationIsInactive, flag);
    }

    /// [window allowsConcurrentViewDrawing] / setter (macOS 10.6+).
    public boolean allowsConcurrentViewDrawing() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsConcurrentViewDrawing);
    }

    /// [window setAllowsConcurrentViewDrawing:].
    public void setAllowsConcurrentViewDrawing(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAllowsConcurrentViewDrawing, flag);
    }

    /// [window displaysWhenScreenProfileChanges] / setter.
    public boolean displaysWhenScreenProfileChanges() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.displaysWhenScreenProfileChanges);
    }

    /// [window setDisplaysWhenScreenProfileChanges:].
    public void setDisplaysWhenScreenProfileChanges(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setDisplaysWhenScreenProfileChanges, flag);
    }

    /// [window canBecomeVisibleWithoutLogin] / setter (macOS 10.5+).
    public boolean canBecomeVisibleWithoutLogin() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.canBecomeVisibleWithoutLogin);
    }

    /// [window setCanBecomeVisibleWithoutLogin:].
    public void setCanBecomeVisibleWithoutLogin(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setCanBecomeVisibleWithoutLogin, flag);
    }

    /// [window invalidateShadow] — redraw the shadow next display.
    public void invalidateShadow() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.invalidateShadow);
    }

    /// [window appearanceSource] — raw peer (NSAppearanceCustomization), may be nil.
    public MemorySegment appearanceSource() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.appearanceSource);
    }

    /// [window setAppearanceSource:] — nil resets to inherited appearance.
    public void setAppearanceSource(MemorySegment source) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setAppearanceSource,
                source == null ? MemorySegment.NULL : source);
    }

    /// [window colorSpace] — raw peer (no NSColorSpace wrapper yet), may be nil.
    public MemorySegment colorSpace() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.colorSpace);
    }

    /// [window setColorSpace:] — nil restores the default.
    public void setColorSpace(MemorySegment colorSpace) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setColorSpace,
                colorSpace == null ? MemorySegment.NULL : colorSpace);
    }

    /// [window canRepresentDisplayGamut:] (macOS 10.12+) — `gamut`: 0=sRGB, 1=DisplayP3, ...
    public boolean canRepresentDisplayGamut(long gamut) {
        ensureInit();
        try {
            return (boolean) H.hBoolInt().invokeExact(peer, Sels.canRepresentDisplayGamut, gamut);
        } catch (Throwable t) {
            throw new RuntimeException("canRepresentDisplayGamut: failed", t);
        }
    }

    /// [window titlebarSeparatorStyle] (macOS 11+) — NSTitlebarSeparatorStyle.
    public long titlebarSeparatorStyle() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.titlebarSeparatorStyle);
    }

    /// [window setTitlebarSeparatorStyle:].
    public void setTitlebarSeparatorStyle(long style) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setTitlebarSeparatorStyle, style);
    }

    // ---- frame persistence ----

    /// [window stringWithSavedFrame] — persistable frame descriptor.
    public String stringWithSavedFrame() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.stringWithSavedFrame));
    }

    /// [window setFrameFromString:] — restore a frame saved via stringWithSavedFrame.
    public void setFrameFromString(String frameString) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setFrameFromString, ObjC.nsstring(frameString));
    }

    /// [window saveFrameUsingName:] — persist the frame under `name`.
    public void saveFrameUsingName(String name) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.saveFrameUsingName, ObjC.nsstring(name));
    }

    /// [window setFrameUsingName:force:] — restore a saved frame; returns success.
    public boolean setFrameUsingNameForce(String name, boolean force) {
        ensureInit();
        try {
            return (boolean) H.hBoolIdBool().invokeExact(peer,
                    Sels.setFrameUsingName_force, ObjC.nsstring(name), force);
        } catch (Throwable t) {
            throw new RuntimeException("setFrameUsingName:force: failed", t);
        }
    }

    /// [window setFrameUsingName:] — restore a saved frame; returns success.
    public boolean setFrameUsingName(String name) {
        ensureInit();
        try {
            return (boolean) H.hBoolId().invokeExact(peer,
                    Sels.setFrameUsingName, ObjC.nsstring(name));
        } catch (Throwable t) {
            throw new RuntimeException("setFrameUsingName: failed", t);
        }
    }

    // ---- controller / sheets / child windows ----

    /// [window windowController] — may be nil.
    public NSWindowController windowController() {
        ensureInit();
        return NSWindowController.wrap(ObjC.msgSendId(peer, Sels.windowController));
    }

    /// [window setWindowController:] — nil detaches.
    public void setWindowController(NSWindowController controller) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setWindowController,
                controller == null ? MemorySegment.NULL : controller.peer());
    }

    /// [window sheets] (macOS 10.9+) — attached sheets, empty when none.
    public NSArray sheets() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.sheets));
    }

    /// [window childWindows] — may be nil when childless.
    public NSArray childWindows() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.childWindows));
    }

    /// [window parentWindow] — null for top-level windows.
    public NSWindow parentWindow() {
        ensureInit();
        return wrap(ObjC.msgSendId(peer, Sels.parentWindow));
    }

    /// [window addChildWindow:ordered:] — `place`: -1=out, 0=below, 1=above.
    public void addChildWindow(NSWindow child, long place) {
        ensureInit();
        if (child == null) return;
        try {
            H.hVoidIdInt().invokeExact(peer, Sels.addChildWindow_ordered, child.peer(), place);
        } catch (Throwable t) {
            throw new RuntimeException("addChildWindow:ordered: failed", t);
        }
    }

    /// [window removeChildWindow:].
    public void removeChildWindow(NSWindow child) {
        ensureInit();
        if (child == null) return;
        ObjC.msgSendVoidId(peer, Sels.removeChildWindow, child.peer());
    }

    // ---- content controller / dragging / export ----

    /// [window contentViewController] (macOS 10.10+) — may be nil.
    public NSViewController contentViewController() {
        ensureInit();
        return NSViewController.wrap(ObjC.msgSendId(peer, Sels.contentViewController));
    }

    /// [window setContentViewController:] — nil detaches (also clears contentView).
    public void setContentViewController(NSViewController controller) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setContentViewController,
                controller == null ? MemorySegment.NULL : controller.peer());
    }

    /// [window performWindowDragWithEvent:] (macOS 10.11+) — start a window drag.
    public void performWindowDragWithEvent(NSEvent event) {
        ensureInit();
        if (event == null) return;
        ObjC.msgSendVoidId(peer, Sels.performWindowDragWithEvent, event.peer());
    }

    /// [window toggleFullScreen:] — enter/exit full screen, sender may be null.
    public void toggleFullScreen(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleFullScreen, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window dataWithEPSInsideRect:] — EPS snapshot of `rect`.
    public NSData dataWithEPSInsideRect(NSRect rect) {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) H.hIdRect().invokeExact(peer,
                    Sels.dataWithEPSInsideRect, rect.toSegment());
            return NSData.wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("dataWithEPSInsideRect: failed", t);
        }
    }

    /// [window dataWithPDFInsideRect:] — PDF snapshot of `rect`.
    public NSData dataWithPDFInsideRect(NSRect rect) {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) H.hIdRect().invokeExact(peer,
                    Sels.dataWithPDFInsideRect, rect.toSegment());
            return NSData.wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("dataWithPDFInsideRect: failed", t);
        }
    }

    /// [window print:] — opens the print dialog for the window; sender may be null.
    /// Never call from an unattended test (modal UI).
    public void print(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.print, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window displayLinkWithTarget:selector:] — display-synced callback source.
    /// `selector` is a no-arg selector name on `target` (e.g. `"tick:"`).
    /// Invalidate the link when done.
    public CADisplayLink displayLinkWithTarget(NSObject target, String selector) {
        ensureInit();
        try {
            MemorySegment link = (MemorySegment) H.hIdIdId().invokeExact(peer,
                    Sels.displayLinkWithTarget_selector,
                    (MemorySegment) (target == null ? MemorySegment.NULL : target.peer()),
                    ObjC.sel(selector));
            return CADisplayLink.wrap(link);
        } catch (Throwable t) {
            throw new RuntimeException("displayLinkWithTarget:selector: failed", t);
        }
    }

    // ---- key-view loop ----

    /// [window setInitialFirstResponder:] — canonical setter name (the existing
    /// `initialFirstResponder(NSView)` setter is kept for compatibility).
    public void setInitialFirstResponder(NSView view) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setInitialFirstResponder,
                (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
    }

    /// [window selectNextKeyView:] / selectPreviousKeyView: — sender may be null.
    public void selectNextKeyView(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.selectNextKeyView, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window selectPreviousKeyView:] — sender may be null.
    public void selectPreviousKeyView(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.selectPreviousKeyView, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window selectKeyViewFollowingView:].
    public void selectKeyViewFollowingView(NSView view) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.selectKeyViewFollowingView,
                view == null ? MemorySegment.NULL : view.peer());
    }

    /// [window selectKeyViewPrecedingView:].
    public void selectKeyViewPrecedingView(NSView view) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.selectKeyViewPrecedingView,
                view == null ? MemorySegment.NULL : view.peer());
    }

    /// [window keyViewSelectionDirection] — NSSelectionDirection.
    public long keyViewSelectionDirection() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.keyViewSelectionDirection);
    }

    /// [window defaultButtonCell] — raw peer (no NSButtonCell wrapper), may be nil.
    public MemorySegment defaultButtonCell() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.defaultButtonCell);
    }

    /// [window setDefaultButtonCell:] — nil clears.
    public void setDefaultButtonCell(MemorySegment cell) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setDefaultButtonCell,
                cell == null ? MemorySegment.NULL : cell);
    }

    /// [window disableKeyEquivalentForDefaultButtonCell].
    public void disableKeyEquivalentForDefaultButtonCell() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.disableKeyEquivalentForDefaultButtonCell);
    }

    /// [window enableKeyEquivalentForDefaultButtonCell].
    public void enableKeyEquivalentForDefaultButtonCell() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.enableKeyEquivalentForDefaultButtonCell);
    }

    /// [window autorecalculatesKeyViewLoop] / setter.
    public boolean autorecalculatesKeyViewLoop() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.autorecalculatesKeyViewLoop);
    }

    /// [window setAutorecalculatesKeyViewLoop:].
    public void setAutorecalculatesKeyViewLoop(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutorecalculatesKeyViewLoop, flag);
    }

    /// [window recalculateKeyViewLoop].
    public void recalculateKeyViewLoop() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.recalculateKeyViewLoop);
    }

    // ---- toolbar ----

    /// [window toolbar] — may be nil.
    public NSToolbar toolbar() {
        ensureInit();
        return NSToolbar.wrap(ObjC.msgSendId(peer, Sels.toolbar));
    }

    /// [window setToolbar:] — nil detaches.
    public void setToolbar(NSToolbar toolbar) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setToolbar,
                toolbar == null ? MemorySegment.NULL : toolbar.peer());
    }

    /// [window toggleToolbarShown:] — sender may be null.
    public void toggleToolbarShown(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleToolbarShown, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window runToolbarCustomizationPalette:] — opens the customization sheet;
    /// sender may be null. Never call from an unattended test (modal UI).
    public void runToolbarCustomizationPalette(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.runToolbarCustomizationPalette, sender == null ? MemorySegment.NULL : sender.peer());
    }

    // ---- tabs ----

    /// [window tabbingIdentifier] (macOS 10.12+) — may be nil.
    public String tabbingIdentifier() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.tabbingIdentifier));
    }

    /// [window setTabbingIdentifier:].
    public void setTabbingIdentifier(String identifier) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setTabbingIdentifier,
                identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier));
    }

    /// [window selectNextTab:] — sender may be null.
    public void selectNextTab(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.selectNextTab, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window selectPreviousTab:] — sender may be null.
    public void selectPreviousTab(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.selectPreviousTab, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window moveTabToNewWindow:] — sender may be null.
    public void moveTabToNewWindow(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.moveTabToNewWindow, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window mergeAllWindows:] — sender may be null.
    public void mergeAllWindows(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.mergeAllWindows, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window toggleTabBar:] — sender may be null.
    public void toggleTabBar(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleTabBar, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window toggleTabOverview:] (macOS 10.13+) — sender may be null.
    public void toggleTabOverview(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.toggleTabOverview, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [window tabbedWindows] (macOS 10.12+) — may be nil when untabbed.
    public NSArray tabbedWindows() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.tabbedWindows));
    }

    /// [window addTabbedWindow:ordered:] (macOS 10.12+) — `ordered`: -1/0/1.
    public void addTabbedWindow(NSWindow window, long ordered) {
        ensureInit();
        if (window == null) return;
        try {
            H.hVoidIdInt().invokeExact(peer, Sels.addTabbedWindow_ordered, window.peer(), ordered);
        } catch (Throwable t) {
            throw new RuntimeException("addTabbedWindow:ordered: failed", t);
        }
    }

    /// [window tab] (macOS 10.13+) — raw peer (no NSWindowTab wrapper), may be nil.
    public MemorySegment tab() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.tab);
    }

    /// [window tabGroup] (macOS 10.13+) — raw peer (no NSWindowTabGroup wrapper).
    public MemorySegment tabGroup() {
        ensureInit();
        return ObjC.msgSendId(peer, Sels.tabGroup);
    }

    // ---- sharing session state ----

    /// [window hasActiveWindowSharingSession] (macOS 13.3+).
    public boolean hasActiveWindowSharingSession() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.hasActiveWindowSharingSession);
    }

    /// [window windowTitlebarLayoutDirection] (macOS 10.12+) — 0=LTR, 1=RTL.
    public long windowTitlebarLayoutDirection() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.windowTitlebarLayoutDirection);
    }

    // ---- window event queue ----

    /// [window nextEventMatchingMask:] — next matching event, or null if none is
    /// queued. AppKit honesty: this BLOCKS until a matching event arrives — a
    /// mask that matches nothing (e.g. 0) waits forever (measured). Prefer
    /// `nextEventMatchingMaskUntilDate` with an expired date for polling.
    public NSEvent nextEventMatchingMask(long mask) {
        ensureInit();
        try {
            MemorySegment ev = (MemorySegment) H.hIdInt().invokeExact(peer,
                    Sels.nextEventMatchingMask, mask);
            return (ev == null || ev.address() == 0) ? null : new NSEvent(ev);
        } catch (Throwable t) {
            throw new RuntimeException("nextEventMatchingMask: failed", t);
        }
    }

    /// [window nextEventMatchingMask:untilDate:inMode:dequeue:] — `mode` is a
    /// run-loop mode name (e.g. `"kCFRunLoopDefaultMode"`); `untilDate` may be
    /// null (waits only when non-nil with a future date). Returns null on expiry.
    public NSEvent nextEventMatchingMaskUntilDate(long mask, NSDate untilDate, String mode, boolean dequeue) {
        ensureInit();
        try {
            MemorySegment ev = (MemorySegment) H.hIdIntIdIdBool().invokeExact(peer,
                    Sels.nextEventMatchingMask_untilDate_inMode_dequeue, mask,
                    (MemorySegment) (untilDate == null ? MemorySegment.NULL : untilDate.peer()),
                    ObjC.nsstring(mode), dequeue);
            return (ev == null || ev.address() == 0) ? null : new NSEvent(ev);
        } catch (Throwable t) {
            throw new RuntimeException("nextEventMatchingMask:untilDate:inMode:dequeue: failed", t);
        }
    }

    /// [window discardEventsMatchingMask:beforeEvent:] — drop queued events.
    public void discardEventsMatchingMaskBeforeEvent(long mask, NSEvent lastEvent) {
        ensureInit();
        try {
            H.hVoidIntId().invokeExact(peer, Sels.discardEventsMatchingMask_beforeEvent, mask,
                    (MemorySegment) (lastEvent == null ? MemorySegment.NULL : lastEvent.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("discardEventsMatchingMask:beforeEvent: failed", t);
        }
    }

    /// [window postEvent:atStart:] — enqueue an event (null event is a no-op).
    public void postEvent(NSEvent event, boolean atStart) {
        ensureInit();
        if (event == null) return;
        try {
            H.hVoidIdBool().invokeExact(peer, Sels.postEvent_atStart, event.peer(), atStart);
        } catch (Throwable t) {
            throw new RuntimeException("postEvent:atStart: failed", t);
        }
    }

    /// [window sendEvent:] — dispatch an event (null event is a no-op).
    public void sendEvent(NSEvent event) {
        ensureInit();
        if (event == null) return;
        ObjC.msgSendVoidId(peer, Sels.sendEvent, event.peer());
    }

    /// [window currentEvent] — the event being dispatched, or null.
    public NSEvent currentEvent() {
        ensureInit();
        MemorySegment ev = ObjC.msgSendId(peer, Sels.currentEvent);
        return (ev == null || ev.address() == 0) ? null : new NSEvent(ev);
    }

    // ---- mouse / cursor rects ----

    /// [window ignoresMouseEvents] / [window setIgnoresMouseEvents:].
    public boolean ignoresMouseEvents() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.ignoresMouseEvents);
    }

    /// [window setIgnoresMouseEvents:].
    public void setIgnoresMouseEvents(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setIgnoresMouseEvents, flag);
    }

    /// [window mouseLocationOutsideOfEventStream] — cursor in window-base coords.
    public NSPoint mouseLocationOutsideOfEventStream() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetPoint().invokeExact(ObjC.structSlot(), peer,
                    Sels.mouseLocationOutsideOfEventStream);
            return NSPoint.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("mouseLocationOutsideOfEventStream failed", t);
        }
    }

    /// [window disableCursorRects].
    public void disableCursorRects() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.disableCursorRects);
    }

    /// [window enableCursorRects].
    public void enableCursorRects() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.enableCursorRects);
    }

    /// [window discardCursorRects].
    public void discardCursorRects() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.discardCursorRects);
    }

    /// [window areCursorRectsEnabled].
    public boolean areCursorRectsEnabled() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.areCursorRectsEnabled);
    }

    /// [window invalidateCursorRectsForView:] — nil is a no-op.
    public void invalidateCursorRectsForView(NSView view) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.invalidateCursorRectsForView,
                view == null ? MemorySegment.NULL : view.peer());
    }

    /// [window resetCursorRects].
    public void resetCursorRects() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.resetCursorRects);
    }

    // ---- drag and drop ----

    /// [window registerForDraggedTypes:] — pasteboard types the window accepts.
    public void registerForDraggedTypes(NSArray types) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.registerForDraggedTypes,
                types == null ? MemorySegment.NULL : types.peer());
    }

    /// [window unregisterDraggedTypes].
    public void unregisterDraggedTypes() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.unregisterDraggedTypes);
    }

    /// [window beginDraggingSessionWithItems:event:source:] (macOS 10.7+) —
    /// `items` is an NSArray of NSDraggingItem, `source` any NSObject.
    public NSDraggingSession beginDraggingSessionWithItems(NSArray items, NSEvent event, NSObject source) {
        ensureInit();
        if (items == null || event == null) return null;
        try {
            MemorySegment s = (MemorySegment) H.hIdIdIdId().invokeExact(peer,
                    Sels.beginDraggingSessionWithItems_event_source,
                    items.peer(), event.peer(),
                    (MemorySegment) (source == null ? MemorySegment.NULL : source.peer()));
            return NSDraggingSession.wrap(s);
        } catch (Throwable t) {
            throw new RuntimeException("beginDraggingSessionWithItems:event:source: failed", t);
        }
    }

    // ---------------------------------------------------------------- responder chain (NSWindow is an NSResponder)

    /// makeFirstResponder: — install `responder` as the window's first
    /// responder. The native selector returns a BOOL ("accepted?") that this
    /// void wrapper discards; check `firstResponder()` afterwards if acceptance
    /// matters. Views created via `NSView.create` accept only while a key
    /// listener is registered (`NSView.setKeyListener`).
    public boolean makeFirstResponder(NSView responder) {
        ensureInit();
        try {
            return (boolean) H.hBoolId().invokeExact(peer, Sels.makeFirstResponder,
                    (MemorySegment)(responder == null ? MemorySegment.NULL : responder.peer()));
        } catch (Throwable t) { throw new RuntimeException("makeFirstResponder: failed", t); }
    }

    /// firstResponder — the window's current first responder, wrapped as an
    /// NSResponder. This is often the window itself when no view has key focus.
    public NSResponder firstResponder() {
        ensureInit();
        return NSResponder.wrap(ObjC.msgSendId(peer, Sels.firstResponder));
    }

    /// setAcceptsMouseMovedEvents: — whether the window's views receive
    /// mouseMoved events. Off by default (they cost a message per pixel of
    /// cursor travel); turn on for hover UI, together with
    /// `NSView.enableMouseTracking` on the views that want the callbacks.
    public void setAcceptsMouseMovedEvents(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAcceptsMouseMovedEvents, flag);
    }

    /// acceptsMouseMovedEvents — whether mouse-moved events are delivered.
    public boolean acceptsMouseMovedEvents() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.acceptsMouseMovedEvents);
    }

    /// setInitialFirstResponder: — the view that becomes first responder when
    /// the window is shown (the `initialFirstResponder` outlet).
    public void initialFirstResponder(NSView view) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setInitialFirstResponder,
                (MemorySegment)(view == null ? MemorySegment.NULL : view.peer()));
    }

    /// initialFirstResponder — the view set to take first-responder status when
    /// the window is shown (null if none was set).
    public NSView initialFirstResponder() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(peer, Sels.initialFirstResponder));
    }
}
