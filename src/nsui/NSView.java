package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ConcurrentHashMap;

import nsui.objc.NsuiForeign;
import nsui.objc.ObjC;
import nsui.objc.Scratch;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSView — a drawable view and a link in the responder chain. Extends
/// NSResponder; the drawing is handed off to a Java `Drawable` via a
/// runtime-created ObjC subclass of NSView whose `drawRect:` is an FFM upcall
/// stub into Java.
///
/// Dispatch is keyed by the native peer address: `create` registers the
/// `Drawable`, and the upcall target looks it up when AppKit asks the view
/// to draw on the main thread. The same subclass also carries the input-event
/// overrides (`mouseDown:`, `keyDown:`, ...) as upcall stubs; they route into
/// the Java `MouseListener` / `KeyListener` registered for the view's peer
/// address, and hand unhandled events to the next responder so AppKit's
/// responder chain keeps flowing. `dealloc` unregisters everything, so no
/// registry can grow stale or leak.
///
/// Upcall targets (`drawRectImpl`, `deallocImpl`, the event impls) are static
/// and capture-free — they are registered for native-image in NsuiFeature.
public class NSView extends NSResponder {

    /// Cached selectors, populated once from ensureInitLocked().
    private static final class Sels {
        static MemorySegment alloc;
        static MemorySegment initWithFrame;
        static MemorySegment currentContext;
        static MemorySegment graphicsPort;
        static MemorySegment addTrackingArea;
        static MemorySegment nextResponder;
        static MemorySegment addSubview;
        static MemorySegment setFrame;
        static MemorySegment setAutoresizingMask;
        static MemorySegment bounds;
        static MemorySegment setBounds;
        static MemorySegment frame;
        static MemorySegment needsDisplay;
        static MemorySegment setNeedsDisplay;
        static MemorySegment setNeedsDisplayInRect;
        static MemorySegment backingScaleFactor;
        static MemorySegment convertRectToBacking;
        static MemorySegment window;
        static MemorySegment setWantsLayer;
        static MemorySegment wantsLayer;
        static MemorySegment isFlipped;
        static MemorySegment layer;
        static MemorySegment setLayer;
        static MemorySegment autoresizingMask;
        static MemorySegment autoresizesSubviews;
        static MemorySegment setAutoresizesSubviews;
        static MemorySegment translatesAutoresizingMaskIntoConstraints;
        static MemorySegment setTranslatesAutoresizingMaskIntoConstraints;
        static MemorySegment leadingAnchor;
        static MemorySegment trailingAnchor;
        static MemorySegment topAnchor;
        static MemorySegment bottomAnchor;
        static MemorySegment widthAnchor;
        static MemorySegment heightAnchor;
        static MemorySegment centerXAnchor;
        static MemorySegment centerYAnchor;
        static MemorySegment addConstraint;
        static MemorySegment removeConstraint;
        static MemorySegment constraints;
        static MemorySegment count;
        static MemorySegment objectAtIndex;
        static MemorySegment displayIfNeeded;
        static MemorySegment displayIfNeededInRect;
        static MemorySegment layoutSubtreeIfNeeded;
        static MemorySegment intrinsicContentSize;
        static MemorySegment alphaValue;
        static MemorySegment setAlphaValue;
        static MemorySegment isHidden;
        static MemorySegment setHidden;
        static MemorySegment isHiddenOrHasHiddenAncestor;
        static MemorySegment superview;
        static MemorySegment isOpaque;
        static MemorySegment visibleRect;
        static MemorySegment isRotatedFromBase;
        static MemorySegment isRotatedOrScaledFromBase;
        static MemorySegment canBecomeKeyView;
        static MemorySegment enclosingScrollView;
        static MemorySegment invalidateIntrinsicContentSize;
        static MemorySegment array;
        static MemorySegment addObject;
        static MemorySegment registerForDraggedTypes;
        static MemorySegment unregisterDraggedTypes;
        static MemorySegment beginDraggingSessionWithItems_event_source;
        static MemorySegment bitmapImageRepForCachingDisplayInRect;
        static MemorySegment cacheDisplayInRect_toBitmapImageRep;
        static MemorySegment isDescendantOf;
        static MemorySegment ancestorSharedWithView;
        static MemorySegment subviews;
        static MemorySegment setSubviews;
        static MemorySegment opaqueAncestor;
        static MemorySegment removeFromSuperview;
        static MemorySegment replaceSubview_with;
        static MemorySegment removeFromSuperviewWithoutNeedingDisplay;
        static MemorySegment viewWillMoveToWindow;
        static MemorySegment viewDidMoveToWindow;
        static MemorySegment viewWillMoveToSuperview;
        static MemorySegment viewDidMoveToSuperview;
        static MemorySegment didAddSubview;
        static MemorySegment willRemoveSubview;
        static MemorySegment viewDidHide;
        static MemorySegment viewDidUnhide;
        static MemorySegment viewDidChangeBackingProperties;
        static MemorySegment setFrameOrigin;
        static MemorySegment setFrameSize;
        static MemorySegment setBoundsOrigin;
        static MemorySegment setBoundsSize;
        static MemorySegment translateOriginToPoint;
        static MemorySegment scaleUnitSquareToSize;
        static MemorySegment rotateByAngle;
        static MemorySegment frameRotation;
        static MemorySegment setFrameRotation;
        static MemorySegment frameCenterRotation;
        static MemorySegment setFrameCenterRotation;
        static MemorySegment boundsRotation;
        static MemorySegment setBoundsRotation;
        static MemorySegment postsFrameChangedNotifications;
        static MemorySegment setPostsFrameChangedNotifications;
        static MemorySegment postsBoundsChangedNotifications;
        static MemorySegment setPostsBoundsChangedNotifications;
        static MemorySegment resizeSubviewsWithOldSize;
        static MemorySegment resizeWithOldSuperviewSize;
        static MemorySegment display;
        static MemorySegment displayIfNeededIgnoringOpacity;
        static MemorySegment displayRect;
        static MemorySegment displayRectIgnoringOpacity;
        static MemorySegment displayIfNeededInRectIgnoringOpacity;
        static MemorySegment displayRectIgnoringOpacity_inContext;
        static MemorySegment viewWillDraw;
        static MemorySegment wantsDefaultClipping;
        static MemorySegment canDrawConcurrently;
        static MemorySegment setCanDrawConcurrently;
        static MemorySegment needsLayout;
        static MemorySegment setNeedsLayout;
        static MemorySegment layout;
        static MemorySegment updateLayer;
        static MemorySegment makeBackingLayer;
        static MemorySegment centerScanRect;
        static MemorySegment adjustScroll;
        static MemorySegment convertRectFromBacking;
        static MemorySegment convertSizeToBacking;
        static MemorySegment convertSizeFromBacking;
        static MemorySegment convertSizeToLayer;
        static MemorySegment convertSizeFromLayer;
        static MemorySegment scrollPoint;
        static MemorySegment autoscroll;
        static MemorySegment hitTest;
        static MemorySegment viewWithTag;
        static MemorySegment tag;
        static MemorySegment needsPanelToBecomeKey;
        static MemorySegment mouseDownCanMoveWindow;
        static MemorySegment menuForEvent;
        static MemorySegment willOpenMenu_withEvent;
        static MemorySegment didCloseMenu_withEvent;
        static MemorySegment defaultMenu;
        static MemorySegment toolTip;
        static MemorySegment setToolTip;
        static MemorySegment removeToolTip;
        static MemorySegment removeAllToolTips;
        static MemorySegment viewWillStartLiveResize;
        static MemorySegment viewDidEndLiveResize;
        static MemorySegment inLiveResize;
        static MemorySegment preservesContentDuringLiveResize;
        static MemorySegment rectPreservedDuringLiveResize;
        static MemorySegment prepareForReuse;
        static MemorySegment prepareContentInRect;
        static MemorySegment viewDidChangeEffectiveAppearance;
        static MemorySegment setKeyboardFocusRingNeedsDisplayInRect;
        static MemorySegment drawFocusRingMask;
        static MemorySegment noteFocusRingMaskChanged;
        static MemorySegment focusRingMaskBounds;
        static MemorySegment focusRingType;
        static MemorySegment setFocusRingType;
        static MemorySegment defaultFocusRingType;
        static MemorySegment focusView;
        static MemorySegment nextKeyView;
        static MemorySegment setNextKeyView;
        static MemorySegment previousKeyView;
        static MemorySegment nextValidKeyView;
        static MemorySegment previousValidKeyView;
        static MemorySegment layerContentsRedrawPolicy;
        static MemorySegment setLayerContentsRedrawPolicy;
        static MemorySegment layerContentsPlacement;
        static MemorySegment setLayerContentsPlacement;
        static MemorySegment wantsUpdateLayer;
        static MemorySegment canDrawSubviewsIntoLayer;
        static MemorySegment setCanDrawSubviewsIntoLayer;
        static MemorySegment layerUsesCoreImageFilters;
        static MemorySegment setLayerUsesCoreImageFilters;
        static MemorySegment shadow;
        static MemorySegment setShadow;
        static MemorySegment clipsToBounds;
        static MemorySegment setClipsToBounds;
        static MemorySegment wantsRestingTouches;
        static MemorySegment setWantsRestingTouches;
        static MemorySegment allowedTouchTypes;
        static MemorySegment setAllowedTouchTypes;
        static MemorySegment print;
        static MemorySegment pageHeader;
        static MemorySegment pageFooter;
        static MemorySegment printJobTitle;
        static MemorySegment dataWithEPSInsideRect;
        static MemorySegment dataWithPDFInsideRect;
        static MemorySegment writeEPSInsideRect_toPasteboard;
        static MemorySegment writePDFInsideRect_toPasteboard;
        static MemorySegment drawPageBorderWithSize;
        static MemorySegment beginDocument;
        static MemorySegment endDocument;
        static MemorySegment endPage;
        static MemorySegment heightAdjustLimit;
        static MemorySegment widthAdjustLimit;
        static MemorySegment registeredDraggedTypes;
        static MemorySegment enterFullScreenMode_withOptions;
        static MemorySegment exitFullScreenModeWithOptions;
        static MemorySegment isInFullScreenMode;
        static MemorySegment addGestureRecognizer;
        static MemorySegment removeGestureRecognizer;
        static MemorySegment gestureRecognizers;
        static MemorySegment removeTrackingArea;
        static MemorySegment updateTrackingAreas;
        static MemorySegment trackingAreas;
        static MemorySegment addCursorRect_cursor;
        static MemorySegment removeCursorRect_cursor;
        static MemorySegment discardCursorRects;
        static MemorySegment resetCursorRects;
        static MemorySegment displayLinkWithTarget_selector;
        static MemorySegment userInterfaceLayoutDirection;
        static MemorySegment setUserInterfaceLayoutDirection;
        static MemorySegment preparedContentRect;
        static MemorySegment setPreparedContentRect;
        static MemorySegment allowsVibrancy;
        static MemorySegment safeAreaInsets;
        static MemorySegment additionalSafeAreaInsets;
        static MemorySegment setAdditionalSafeAreaInsets;
        static MemorySegment safeAreaRect;
        static MemorySegment safeAreaLayoutGuide;
        static MemorySegment layoutMarginsGuide;
        static MemorySegment prefersCompactControlSizeMetrics;
        static MemorySegment setPrefersCompactControlSizeMetrics;
        static MemorySegment isDrawingFindIndicator;
        static void populate() {
            alloc = ObjC.sel("alloc");
            initWithFrame = ObjC.sel("initWithFrame:");
            currentContext = ObjC.sel("currentContext");
            graphicsPort = ObjC.sel("graphicsPort");
            addTrackingArea = ObjC.sel("addTrackingArea:");
            nextResponder = ObjC.sel("nextResponder");
            addSubview = ObjC.sel("addSubview:");
            setFrame = ObjC.sel("setFrame:");
            setAutoresizingMask = ObjC.sel("setAutoresizingMask:");
            bounds = ObjC.sel("bounds");
            setBounds = ObjC.sel("setBounds:");
            frame = ObjC.sel("frame");
            needsDisplay = ObjC.sel("needsDisplay");
            setNeedsDisplay = ObjC.sel("setNeedsDisplay:");
            setNeedsDisplayInRect = ObjC.sel("setNeedsDisplayInRect:");
            backingScaleFactor = ObjC.sel("backingScaleFactor");
            convertRectToBacking = ObjC.sel("convertRectToBacking:");
            window = ObjC.sel("window");
            setWantsLayer = ObjC.sel("setWantsLayer:");
            wantsLayer = ObjC.sel("wantsLayer");
            isFlipped = ObjC.sel("isFlipped");
            layer = ObjC.sel("layer");
            setLayer = ObjC.sel("setLayer:");
            autoresizingMask = ObjC.sel("autoresizingMask");
            autoresizesSubviews = ObjC.sel("autoresizesSubviews");
            setAutoresizesSubviews = ObjC.sel("setAutoresizesSubviews:");
            translatesAutoresizingMaskIntoConstraints = ObjC.sel("translatesAutoresizingMaskIntoConstraints");
            setTranslatesAutoresizingMaskIntoConstraints = ObjC.sel("setTranslatesAutoresizingMaskIntoConstraints:");
            leadingAnchor = ObjC.sel("leadingAnchor");
            trailingAnchor = ObjC.sel("trailingAnchor");
            topAnchor = ObjC.sel("topAnchor");
            bottomAnchor = ObjC.sel("bottomAnchor");
            widthAnchor = ObjC.sel("widthAnchor");
            heightAnchor = ObjC.sel("heightAnchor");
            centerXAnchor = ObjC.sel("centerXAnchor");
            centerYAnchor = ObjC.sel("centerYAnchor");
            addConstraint = ObjC.sel("addConstraint:");
            removeConstraint = ObjC.sel("removeConstraint:");
            constraints = ObjC.sel("constraints");
            count = ObjC.sel("count");
            objectAtIndex = ObjC.sel("objectAtIndex:");
            displayIfNeeded = ObjC.sel("displayIfNeeded");
            displayIfNeededInRect = ObjC.sel("displayIfNeededInRect:");
            layoutSubtreeIfNeeded = ObjC.sel("layoutSubtreeIfNeeded");
            intrinsicContentSize = ObjC.sel("intrinsicContentSize");
            alphaValue = ObjC.sel("alphaValue");
            setAlphaValue = ObjC.sel("setAlphaValue:");
            isHidden = ObjC.sel("isHidden");
            setHidden = ObjC.sel("setHidden:");
            isHiddenOrHasHiddenAncestor = ObjC.sel("isHiddenOrHasHiddenAncestor");
            superview = ObjC.sel("superview");
            isOpaque = ObjC.sel("isOpaque");
            visibleRect = ObjC.sel("visibleRect");
            isRotatedFromBase = ObjC.sel("isRotatedFromBase");
            isRotatedOrScaledFromBase = ObjC.sel("isRotatedOrScaledFromBase");
            canBecomeKeyView = ObjC.sel("canBecomeKeyView");
            enclosingScrollView = ObjC.sel("enclosingScrollView");
            invalidateIntrinsicContentSize = ObjC.sel("invalidateIntrinsicContentSize");
            array = ObjC.sel("array");
            addObject = ObjC.sel("addObject:");
            registerForDraggedTypes = ObjC.sel("registerForDraggedTypes:");
            unregisterDraggedTypes = ObjC.sel("unregisterDraggedTypes");
            beginDraggingSessionWithItems_event_source = ObjC.sel("beginDraggingSessionWithItems:event:source:");
            bitmapImageRepForCachingDisplayInRect = ObjC.sel("bitmapImageRepForCachingDisplayInRect:");
            cacheDisplayInRect_toBitmapImageRep = ObjC.sel("cacheDisplayInRect:toBitmapImageRep:");
            isDescendantOf = ObjC.sel("isDescendantOf:");
            ancestorSharedWithView = ObjC.sel("ancestorSharedWithView:");
            subviews = ObjC.sel("subviews");
            setSubviews = ObjC.sel("setSubviews:");
            opaqueAncestor = ObjC.sel("opaqueAncestor");
            removeFromSuperview = ObjC.sel("removeFromSuperview");
            replaceSubview_with = ObjC.sel("replaceSubview:with:");
            removeFromSuperviewWithoutNeedingDisplay = ObjC.sel("removeFromSuperviewWithoutNeedingDisplay");
            viewWillMoveToWindow = ObjC.sel("viewWillMoveToWindow:");
            viewDidMoveToWindow = ObjC.sel("viewDidMoveToWindow");
            viewWillMoveToSuperview = ObjC.sel("viewWillMoveToSuperview:");
            viewDidMoveToSuperview = ObjC.sel("viewDidMoveToSuperview");
            didAddSubview = ObjC.sel("didAddSubview:");
            willRemoveSubview = ObjC.sel("willRemoveSubview:");
            viewDidHide = ObjC.sel("viewDidHide");
            viewDidUnhide = ObjC.sel("viewDidUnhide");
            viewDidChangeBackingProperties = ObjC.sel("viewDidChangeBackingProperties");
            setFrameOrigin = ObjC.sel("setFrameOrigin:");
            setFrameSize = ObjC.sel("setFrameSize:");
            setBoundsOrigin = ObjC.sel("setBoundsOrigin:");
            setBoundsSize = ObjC.sel("setBoundsSize:");
            translateOriginToPoint = ObjC.sel("translateOriginToPoint:");
            scaleUnitSquareToSize = ObjC.sel("scaleUnitSquareToSize:");
            rotateByAngle = ObjC.sel("rotateByAngle:");
            frameRotation = ObjC.sel("frameRotation");
            setFrameRotation = ObjC.sel("setFrameRotation:");
            frameCenterRotation = ObjC.sel("frameCenterRotation");
            setFrameCenterRotation = ObjC.sel("setFrameCenterRotation:");
            boundsRotation = ObjC.sel("boundsRotation");
            setBoundsRotation = ObjC.sel("setBoundsRotation:");
            postsFrameChangedNotifications = ObjC.sel("postsFrameChangedNotifications");
            setPostsFrameChangedNotifications = ObjC.sel("setPostsFrameChangedNotifications:");
            postsBoundsChangedNotifications = ObjC.sel("postsBoundsChangedNotifications");
            setPostsBoundsChangedNotifications = ObjC.sel("setPostsBoundsChangedNotifications:");
            resizeSubviewsWithOldSize = ObjC.sel("resizeSubviewsWithOldSize:");
            resizeWithOldSuperviewSize = ObjC.sel("resizeWithOldSuperviewSize:");
            display = ObjC.sel("display");
            displayIfNeededIgnoringOpacity = ObjC.sel("displayIfNeededIgnoringOpacity");
            displayRect = ObjC.sel("displayRect:");
            displayRectIgnoringOpacity = ObjC.sel("displayRectIgnoringOpacity:");
            displayIfNeededInRectIgnoringOpacity = ObjC.sel("displayIfNeededInRectIgnoringOpacity:");
            displayRectIgnoringOpacity_inContext = ObjC.sel("displayRectIgnoringOpacity:inContext:");
            viewWillDraw = ObjC.sel("viewWillDraw");
            wantsDefaultClipping = ObjC.sel("wantsDefaultClipping");
            canDrawConcurrently = ObjC.sel("canDrawConcurrently");
            setCanDrawConcurrently = ObjC.sel("setCanDrawConcurrently:");
            needsLayout = ObjC.sel("needsLayout");
            setNeedsLayout = ObjC.sel("setNeedsLayout:");
            layout = ObjC.sel("layout");
            updateLayer = ObjC.sel("updateLayer");
            makeBackingLayer = ObjC.sel("makeBackingLayer");
            centerScanRect = ObjC.sel("centerScanRect:");
            adjustScroll = ObjC.sel("adjustScroll:");
            convertRectFromBacking = ObjC.sel("convertRectFromBacking:");
            convertSizeToBacking = ObjC.sel("convertSizeToBacking:");
            convertSizeFromBacking = ObjC.sel("convertSizeFromBacking:");
            convertSizeToLayer = ObjC.sel("convertSizeToLayer:");
            convertSizeFromLayer = ObjC.sel("convertSizeFromLayer:");
            scrollPoint = ObjC.sel("scrollPoint:");
            autoscroll = ObjC.sel("autoscroll:");
            hitTest = ObjC.sel("hitTest:");
            viewWithTag = ObjC.sel("viewWithTag:");
            tag = ObjC.sel("tag");
            needsPanelToBecomeKey = ObjC.sel("needsPanelToBecomeKey");
            mouseDownCanMoveWindow = ObjC.sel("mouseDownCanMoveWindow");
            menuForEvent = ObjC.sel("menuForEvent:");
            willOpenMenu_withEvent = ObjC.sel("willOpenMenu:withEvent:");
            didCloseMenu_withEvent = ObjC.sel("didCloseMenu:withEvent:");
            defaultMenu = ObjC.sel("defaultMenu");
            toolTip = ObjC.sel("toolTip");
            setToolTip = ObjC.sel("setToolTip:");
            removeToolTip = ObjC.sel("removeToolTip:");
            removeAllToolTips = ObjC.sel("removeAllToolTips");
            viewWillStartLiveResize = ObjC.sel("viewWillStartLiveResize");
            viewDidEndLiveResize = ObjC.sel("viewDidEndLiveResize");
            inLiveResize = ObjC.sel("inLiveResize");
            preservesContentDuringLiveResize = ObjC.sel("preservesContentDuringLiveResize");
            rectPreservedDuringLiveResize = ObjC.sel("rectPreservedDuringLiveResize");
            prepareForReuse = ObjC.sel("prepareForReuse");
            prepareContentInRect = ObjC.sel("prepareContentInRect:");
            viewDidChangeEffectiveAppearance = ObjC.sel("viewDidChangeEffectiveAppearance");
            setKeyboardFocusRingNeedsDisplayInRect = ObjC.sel("setKeyboardFocusRingNeedsDisplayInRect:");
            drawFocusRingMask = ObjC.sel("drawFocusRingMask");
            noteFocusRingMaskChanged = ObjC.sel("noteFocusRingMaskChanged");
            focusRingMaskBounds = ObjC.sel("focusRingMaskBounds");
            focusRingType = ObjC.sel("focusRingType");
            setFocusRingType = ObjC.sel("setFocusRingType:");
            defaultFocusRingType = ObjC.sel("defaultFocusRingType");
            focusView = ObjC.sel("focusView");
            nextKeyView = ObjC.sel("nextKeyView");
            setNextKeyView = ObjC.sel("setNextKeyView:");
            previousKeyView = ObjC.sel("previousKeyView");
            nextValidKeyView = ObjC.sel("nextValidKeyView");
            previousValidKeyView = ObjC.sel("previousValidKeyView");
            layerContentsRedrawPolicy = ObjC.sel("layerContentsRedrawPolicy");
            setLayerContentsRedrawPolicy = ObjC.sel("setLayerContentsRedrawPolicy:");
            layerContentsPlacement = ObjC.sel("layerContentsPlacement");
            setLayerContentsPlacement = ObjC.sel("setLayerContentsPlacement:");
            wantsUpdateLayer = ObjC.sel("wantsUpdateLayer");
            canDrawSubviewsIntoLayer = ObjC.sel("canDrawSubviewsIntoLayer");
            setCanDrawSubviewsIntoLayer = ObjC.sel("setCanDrawSubviewsIntoLayer:");
            layerUsesCoreImageFilters = ObjC.sel("layerUsesCoreImageFilters");
            setLayerUsesCoreImageFilters = ObjC.sel("setLayerUsesCoreImageFilters:");
            shadow = ObjC.sel("shadow");
            setShadow = ObjC.sel("setShadow:");
            clipsToBounds = ObjC.sel("clipsToBounds");
            setClipsToBounds = ObjC.sel("setClipsToBounds:");
            wantsRestingTouches = ObjC.sel("wantsRestingTouches");
            setWantsRestingTouches = ObjC.sel("setWantsRestingTouches:");
            allowedTouchTypes = ObjC.sel("allowedTouchTypes");
            setAllowedTouchTypes = ObjC.sel("setAllowedTouchTypes:");
            print = ObjC.sel("print:");
            pageHeader = ObjC.sel("pageHeader");
            pageFooter = ObjC.sel("pageFooter");
            printJobTitle = ObjC.sel("printJobTitle");
            dataWithEPSInsideRect = ObjC.sel("dataWithEPSInsideRect:");
            dataWithPDFInsideRect = ObjC.sel("dataWithPDFInsideRect:");
            writeEPSInsideRect_toPasteboard = ObjC.sel("writeEPSInsideRect:toPasteboard:");
            writePDFInsideRect_toPasteboard = ObjC.sel("writePDFInsideRect:toPasteboard:");
            drawPageBorderWithSize = ObjC.sel("drawPageBorderWithSize:");
            beginDocument = ObjC.sel("beginDocument");
            endDocument = ObjC.sel("endDocument");
            endPage = ObjC.sel("endPage");
            heightAdjustLimit = ObjC.sel("heightAdjustLimit");
            widthAdjustLimit = ObjC.sel("widthAdjustLimit");
            registeredDraggedTypes = ObjC.sel("registeredDraggedTypes");
            enterFullScreenMode_withOptions = ObjC.sel("enterFullScreenMode:withOptions:");
            exitFullScreenModeWithOptions = ObjC.sel("exitFullScreenModeWithOptions:");
            isInFullScreenMode = ObjC.sel("isInFullScreenMode");
            addGestureRecognizer = ObjC.sel("addGestureRecognizer:");
            removeGestureRecognizer = ObjC.sel("removeGestureRecognizer:");
            gestureRecognizers = ObjC.sel("gestureRecognizers");
            removeTrackingArea = ObjC.sel("removeTrackingArea:");
            updateTrackingAreas = ObjC.sel("updateTrackingAreas");
            trackingAreas = ObjC.sel("trackingAreas");
            addCursorRect_cursor = ObjC.sel("addCursorRect:cursor:");
            removeCursorRect_cursor = ObjC.sel("removeCursorRect:cursor:");
            discardCursorRects = ObjC.sel("discardCursorRects");
            resetCursorRects = ObjC.sel("resetCursorRects");
            displayLinkWithTarget_selector = ObjC.sel("displayLinkWithTarget:selector:");
            userInterfaceLayoutDirection = ObjC.sel("userInterfaceLayoutDirection");
            setUserInterfaceLayoutDirection = ObjC.sel("setUserInterfaceLayoutDirection:");
            preparedContentRect = ObjC.sel("preparedContentRect");
            setPreparedContentRect = ObjC.sel("setPreparedContentRect:");
            allowsVibrancy = ObjC.sel("allowsVibrancy");
            safeAreaInsets = ObjC.sel("safeAreaInsets");
            additionalSafeAreaInsets = ObjC.sel("additionalSafeAreaInsets");
            setAdditionalSafeAreaInsets = ObjC.sel("setAdditionalSafeAreaInsets:");
            safeAreaRect = ObjC.sel("safeAreaRect");
            safeAreaLayoutGuide = ObjC.sel("safeAreaLayoutGuide");
            layoutMarginsGuide = ObjC.sel("layoutMarginsGuide");
            prefersCompactControlSizeMetrics = ObjC.sel("prefersCompactControlSizeMetrics");
            setPrefersCompactControlSizeMetrics = ObjC.sel("setPrefersCompactControlSizeMetrics:");
            isDrawingFindIndicator = ObjC.sel("isDrawingFindIndicator");
        }
    }

    /// Java-side drawing callback, invoked from AppKit's drawRect: on the main thread.
    ///
    /// **Dirty-rect contract:** the `dirtyRect` passed to `draw`
    /// is the region AppKit currently requires the view to redraw, expressed in the view's
    /// own coordinate system (see `isFlipped` for the y-axis orientation). When the
    /// view is invalidated via `setNeedsDisplayInRect`, AppKit unions the
    /// invalidated rects and passes that union through `drawRect:`. Drawing may be
    /// clipped to `dirtyRect` (and on a layer-backed view, clipped to the view's
    /// backing region), so a draw method must not assume it is being asked to repaint the
    /// whole bounds. To guarantee coverage of everything that is currently marked dirty,
    /// draw at least the area bounded by `dirtyRect`; painting inside that rect is
    /// sufficient in practice.
    public interface Drawable {
        /// CONTRACT: throwing aborts the VM (see DelegateProxy fail-fast policy).
        /// Catch drawing errors inside draw() — there is no recovery past this boundary.
        void draw(MemorySegment ctx, NSRect dirtyRect);
    }

    /// Drawable registry, keyed by peer address (the view's id).
    private static final ConcurrentHashMap<Long, Drawable> DRAWABLES = new ConcurrentHashMap<>();

    // ---- input-event registries, keyed by peer address (the view's id) ----
    private static final ConcurrentHashMap<Long, MouseListener> MOUSE_LISTENERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, KeyListener> KEY_LISTENERS = new ConcurrentHashMap<>();
    /// Peer addresses that already installed a tracking area via `enableMouseTracking`
    /// — a dedup guard so a repeated call cannot double-deliver mouseMoved/entered/exited.
    private static final java.util.Set<Long> TRACKING_VIEWS = ConcurrentHashMap.newKeySet();

    // ---- runtime ObjC class + upcall stubs, created ONCE lazily (NEVER in a static initializer) ----
    private static MemorySegment drawableClass;
    private static MemorySegment drawRectStub;
    private static MemorySegment deallocStub;

    /// The input-event upcall stubs installed on the ObjC subclass. Built once,
    /// lazily, beside `drawRectStub` (never in a static initializer).
    private record EventStubs(MemorySegment mouseDown, MemorySegment mouseDragged, MemorySegment mouseUp,
                              MemorySegment mouseMoved, MemorySegment mouseEntered, MemorySegment mouseExited,
                              MemorySegment keyDown, MemorySegment keyUp, MemorySegment flagsChanged,
                              MemorySegment performKeyEquivalent, MemorySegment acceptsFirstResponder) {}
    private static volatile EventStubs eventStubs;

    // ---- NSTrackingArea option bits (NSTrackingArea.h). Defined here because
    // NSTrackingArea.java ships no option constants; values from the macOS SDK header. ----

    /// NSTrackingMouseEnteredAndExited (0x01) — deliver mouseEntered/mouseExited.
    public static final long trackingMouseEnteredAndExited = 1L << 0;
    /// NSTrackingMouseMoved (0x02) — deliver mouseMoved while the cursor is inside.
    public static final long trackingMouseMoved = 1L << 1;
    /// NSTrackingCursorUpdate (0x04) — deliver cursorUpdate.
    public static final long trackingCursorUpdate = 1L << 2;
    /// NSTrackingActiveWhenFirstResponder (0x10) — track only while owner is first responder.
    public static final long trackingActiveWhenFirstResponder = 1L << 4;
    /// NSTrackingActiveInKeyWindow (0x20) — track whenever the window is key.
    public static final long trackingActiveInKeyWindow = 1L << 5;
    /// NSTrackingActiveInActiveApp (0x40) — track whenever the app is active.
    public static final long trackingActiveInActiveApp = 1L << 6;
    /// NSTrackingActiveAlways (0x80) — track regardless of activation state.
    public static final long trackingActiveAlways = 1L << 7;
    /// NSTrackingAssumeInside (0x100) — treat the cursor as inside until an enter/exit says otherwise.
    public static final long trackingAssumeInside = 1L << 8;
    /// NSTrackingInVisibleRect (0x200) — track the visible rect instead of the area's rect,
    /// so the tracked region follows resizes and scrolling automatically.
    public static final long trackingInVisibleRect = 1L << 9;
    /// NSTrackingEnabledDuringMouseDrag (0x400) — keep tracking while a drag is in progress.
    public static final long trackingEnabledDuringMouseDrag = 1L << 10;

    // ---- resolved once per process (rule: resolve-once, invokeExact on hot paths) ----
    private record Handles(MethodHandle hInitFrame, MethodHandle hSetFrame, MethodHandle hNeedsRect, MethodHandle hAutoMask, MethodHandle hBacking, MethodHandle hConvBacking, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetSize, MethodHandle hSetSize, MethodHandle hObjectAtIndex, MethodHandle hSetBounds, MethodHandle hRegisterForDraggedTypes, MethodHandle hBeginDraggingSession, MethodHandle hRepForCache, MethodHandle hCacheDisplay, MethodHandle hVoidId, MethodHandle hGetPoint, MethodHandle hSetPoint, MethodHandle hBoolId, MethodHandle hIntId, MethodHandle hVoidIdId, MethodHandle hIdRect, MethodHandle hIdPoint, MethodHandle hRectBool, MethodHandle hVoidPointId, MethodHandle hBoolIdId, MethodHandle hSizeSize, MethodHandle hIdIdId) {}
    private static volatile Handles H;

    /// Wrap a native NSView id (e.g. a box's contentView) as an NSView.
    public static NSView wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSView(peer);
    }

    protected NSView(MemorySegment peer) {
        super(peer);
    }

    /// alloc + initWithFrame: and register the Java drawable for this view.
    public static NSView create(NSRect frame, Drawable drawable) {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(drawableClass, Sels.alloc);
        try {
            v = (MemorySegment) H.hInitFrame().invokeExact(v, Sels.initWithFrame, frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed", t);
        }
        NSView view = new NSView(v);
        DRAWABLES.put(v.address(), drawable);
        return view;
    }

    private static void ensureInit() {
        if (H != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (H != null) return;
        drawableClass = ObjC.makeClass("NSView", "NSUIViewImpl");
        try {
            MethodHandle drawTarget = MethodHandles.lookup().findStatic(NSView.class, "drawRectImpl",
                    MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
            drawRectStub = ObjC.upcall(drawTarget, NsuiForeign.drawRectUpcall());
            MethodHandle deallocTarget = MethodHandles.lookup().findStatic(NSView.class, "deallocImpl",
                    MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class));
            deallocStub = ObjC.upcall(deallocTarget, NsuiForeign.deallocUpcall());
            // input-event stubs — same lazy block as drawRectStub (never a static initializer)
            eventStubs = new EventStubs(
                    voidEventStub("mouseDownImpl"), voidEventStub("mouseDraggedImpl"), voidEventStub("mouseUpImpl"),
                    voidEventStub("mouseMovedImpl"), voidEventStub("mouseEnteredImpl"), voidEventStub("mouseExitedImpl"),
                    voidEventStub("keyDownImpl"), voidEventStub("keyUpImpl"), voidEventStub("flagsChangedImpl"),
                    boolEventStub("performKeyEquivalentImpl"), boolResponderStub("acceptsFirstResponderImpl"));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("cannot bind NSView upcall targets", e);
        }
        if (!ObjC.addMethod(drawableClass, "drawRect:", drawRectStub, "v@:{CGRect={CGPoint=dd}{CGSize=dd}}")) {
            throw new RuntimeException("class_addMethod drawRect: failed");
        }
        if (!ObjC.addMethod(drawableClass, "dealloc", deallocStub, "v@:")) {
            throw new RuntimeException("class_addMethod dealloc failed");
        }
        EventStubs s = eventStubs;
        if (!ObjC.addMethod(drawableClass, "mouseDown:", s.mouseDown(), "v@:@")
                || !ObjC.addMethod(drawableClass, "mouseDragged:", s.mouseDragged(), "v@:@")
                || !ObjC.addMethod(drawableClass, "mouseUp:", s.mouseUp(), "v@:@")
                || !ObjC.addMethod(drawableClass, "mouseMoved:", s.mouseMoved(), "v@:@")
                || !ObjC.addMethod(drawableClass, "mouseEntered:", s.mouseEntered(), "v@:@")
                || !ObjC.addMethod(drawableClass, "mouseExited:", s.mouseExited(), "v@:@")
                || !ObjC.addMethod(drawableClass, "keyDown:", s.keyDown(), "v@:@")
                || !ObjC.addMethod(drawableClass, "keyUp:", s.keyUp(), "v@:@")
                || !ObjC.addMethod(drawableClass, "flagsChanged:", s.flagsChanged(), "v@:@")) {
            throw new RuntimeException("class_addMethod event override failed");
        }
        if (!ObjC.addMethod(drawableClass, "performKeyEquivalent:", s.performKeyEquivalent(), "B@:@")) {
            throw new RuntimeException("class_addMethod performKeyEquivalent: failed");
        }
        if (!ObjC.addMethod(drawableClass, "acceptsFirstResponder", s.acceptsFirstResponder(), "B@:")) {
            throw new RuntimeException("class_addMethod acceptsFirstResponder failed");
        }
        Handles h = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.RECT, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)),
                ObjC.handle(Sig.of(Ret.INT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.POINT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.RECT, Arg.BOOL)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.POINT, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.SIZE, Arg.SIZE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)));
            Sels.populate();
        H = h;
}

    // ---- upcall-stub builders (called only from the lazy ensureInit, never class-init) ----

    private static MemorySegment voidEventStub(String target) throws ReflectiveOperationException {
        MethodHandle mh = MethodHandles.lookup().findStatic(NSView.class, target,
                MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
        return ObjC.upcall(mh, NsuiForeign.eventVoidUpcall());
    }

    private static MemorySegment boolEventStub(String target) throws ReflectiveOperationException {
        MethodHandle mh = MethodHandles.lookup().findStatic(NSView.class, target,
                MethodType.methodType(boolean.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
        return ObjC.upcall(mh, NsuiForeign.eventBoolUpcall());
    }

    private static MemorySegment boolResponderStub(String target) throws ReflectiveOperationException {
        MethodHandle mh = MethodHandles.lookup().findStatic(NSView.class, target,
                MethodType.methodType(boolean.class, MemorySegment.class, MemorySegment.class));
        return ObjC.upcall(mh, NsuiForeign.responderBoolUpcall());
    }

    /// FFM upcall target: `-(void)drawRect:(NSRect)dirtyRect` — called by AppKit on the main thread.
    public static void drawRectImpl(MemorySegment self, MemorySegment sel, MemorySegment rect) {
        ensureInit();
        Drawable d = DRAWABLES.get(self.address());
        if (d == null) return;
        MemorySegment ctx = ObjC.msgSendId(ObjC.cls("NSGraphicsContext"), Sels.currentContext);
        ctx = ObjC.msgSendId(ctx, Sels.graphicsPort);
        // Every draw call's input marshalling (rects, cstrings) goes through the
        // per-turn scratch arena — recycled per draw pass instead of immortal.
        Scratch.beginTurn();
        try {
            d.draw(ctx, NSRect.fromSegment(rect));
        } finally {
            Scratch.endTurn();
        }
    }

    /// FFM upcall target: `-(void)dealloc` — unregister the drawable and any
    /// event listeners / tracking-area bookkeeping, then chain to
    /// `[super dealloc]` via objc_msgSendSuper so the native object is released.
    /// Public because NsuiFeature (nsui.objc) resolves it at build time.
    public static void deallocImpl(MemorySegment self, MemorySegment sel) {
        long addr = self.address();
        DRAWABLES.remove(addr);
        MOUSE_LISTENERS.remove(addr);
        KEY_LISTENERS.remove(addr);
        TRACKING_VIEWS.remove(addr);
        MemorySegment superStruct = ObjC.superStruct(self, ObjC.classGetSuperclass(drawableClass));
        ObjC.msgSendSuperVoid(superStruct, sel);
    }

    /// Number of live drawables (diagnostics/tests: must return to 0 after views are released).
    public static int drawableCount() {
        return DRAWABLES.size();
    }

    // ---------------------------------------------------------------- responder chain & input events

    /// Java-side mouse callback surface for views created with `NSView.create`.
    /// All methods are default-no-op; override only the events you care about.
    /// Callbacks arrive on the main thread. Delivery of `onMouseMoved` additionally
    /// requires `enableMouseTracking` on the view AND
    /// `NSWindow.setAcceptsMouseMovedEvents(true)` on its window;
    /// `onMouseEntered`/`onMouseExited` need only `enableMouseTracking`.
    public interface MouseListener {
        /// Mouse button went down inside the view.
        default void onMouseDown(NSView v, NSEvent e) {}
        /// Mouse button came up (after a down in this view).
        default void onMouseUp(NSView v, NSEvent e) {}
        /// Mouse moved with a button held down.
        default void onMouseDragged(NSView v, NSEvent e) {}
        /// Mouse moved inside the tracked region (no button).
        default void onMouseMoved(NSView v, NSEvent e) {}
        /// Cursor entered the tracked region (tracking area required).
        default void onMouseEntered(NSView v, NSEvent e) {}
        /// Cursor left the tracked region (tracking area required).
        default void onMouseExited(NSView v, NSEvent e) {}
    }

    /// Java-side keyboard callback surface for views created with `NSView.create`.
    /// Return true to mark the event HANDLED — AppKit stops routing it. Return
    /// false to let the upcall target hand the event to the next responder,
    /// continuing the responder chain toward the window. A key listener also
    /// flips the view's `acceptsFirstResponder` to true so it can actually
    /// receive keys.
    public interface KeyListener {
        /// Key pressed. Return true if handled (stops the chain).
        default boolean onKeyDown(NSView v, NSEvent e) { return false; }
        /// Key released. Return true if handled (stops the chain).
        default boolean onKeyUp(NSView v, NSEvent e) { return false; }
        /// Modifier flags changed. Return true if handled (stops the chain).
        default boolean onFlagsChanged(NSView v, NSEvent e) { return false; }
    }

    /// Register the mouse callback surface for this view (null unregisters).
    /// Only views created via `NSView.create` carry the Java-implemented event
    /// overrides — native controls (NSButton etc.) dispatch their own events.
    public void setMouseListener(MouseListener listener) {
        ensureInit();
        if (listener == null) MOUSE_LISTENERS.remove(peer.address());
        else MOUSE_LISTENERS.put(peer.address(), listener);
    }

    /// Register the keyboard callback surface for this view (null unregisters).
    /// While a key listener is registered the view reports
    /// `acceptsFirstResponder == true`, letting it take key focus.
    public void setKeyListener(KeyListener listener) {
        ensureInit();
        if (listener == null) KEY_LISTENERS.remove(peer.address());
        else KEY_LISTENERS.put(peer.address(), listener);
    }

    /// Total live mouse + key listeners across all views (diagnostics/tests:
    /// must return to its baseline after register/unregister cycles).
    public static int listenerCount() {
        return MOUSE_LISTENERS.size() + KEY_LISTENERS.size();
    }

    /// enableMouseTracking — install an NSTrackingArea over this view so
    /// `onMouseEntered`/`onMouseExited`/`onMouseMoved` callbacks can fire.
    ///
    /// Options: `trackingMouseEnteredAndExited | trackingMouseMoved |
    /// trackingActiveAlways | trackingInVisibleRect`. The `trackingInVisibleRect`
    /// bit makes the tracked region follow the view's visible rect automatically,
    /// so window resizes and scrolling keep tracking correct without rebuilding
    /// the area. Idempotent per view: a second call is a no-op (AppKit would
    /// otherwise deliver duplicate events, one per installed area). Mouse-moved
    /// delivery still requires the owning window to enable
    /// `setAcceptsMouseMovedEvents(true)`.
    public void enableMouseTracking() {
        ensureInit();
        if (!TRACKING_VIEWS.add(peer.address())) return;
        long options = trackingMouseEnteredAndExited | trackingMouseMoved
                | trackingActiveAlways | trackingInVisibleRect;
        NSTrackingArea area = NSTrackingArea.create(bounds(), options, this);
        ObjC.msgSendVoidId(peer, Sels.addTrackingArea, area.peer());
    }

    // ---- FFM upcall targets: input-event overrides on the ObjC subclass ----
    // All are public, static and capture-free so NsuiFeature can register them
    // for native-image. Contract: no registered listener -> hand the event to
    // the next responder (the documented NSResponder default); key listeners
    // returning false do the same, true stops the chain.
    // NOTE: deliberately NOT `[super event:]` — objc_msgSendSuper would need a
    // descriptor carrying the event argument, and dropping that arg makes the
    // callee read an uninitialized register as the event (observed live as an
    // ObjC runtime abort). Forwarding to nextResponder is the same chain
    // semantics without the ABI hazard.

    private static NSView selfView(MemorySegment self) {
        return new NSView(self);
    }

    private static NSEvent eventOrNil(MemorySegment eventSeg) {
        return (eventSeg == null || eventSeg.address() == 0) ? null : new NSEvent(eventSeg);
    }

    /// Continue the responder chain for an unhandled event selector: deliver
    /// `sel(event)` to the view's next responder. Plain msgSend on the NEXT
    /// object — never a re-send on `self`, which would recurse into this
    /// override. A nil next responder ends the chain silently.
    ///
    /// A per-thread visited-set guards against responder CYCLES: AppKit's own
    /// default implementations can route back into the subtree we are forwarding
    /// out of (the window broadcasts `performKeyEquivalent:` down its content
    /// view tree, and our contentView subclass forwards up to that same
    /// window), which without the guard is an infinite upcall ping-pong and a
    /// StackOverflowError inside the FFM upcall stub. Entries are added before
    /// the send and removed after it, so unrelated later deliveries to the same
    /// responder still work.
    private static final ThreadLocal<java.util.Set<Long>> FORWARDING = ThreadLocal.withInitial(java.util.HashSet::new);

    private static void forwardChain(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        ensureInit();
        MemorySegment next = ObjC.msgSendId(self, Sels.nextResponder);
        if (next == null || next.address() == 0) return;
        long nextAddr = next.address();
        java.util.Set<Long> visiting = FORWARDING.get();
        if (!visiting.add(nextAddr)) return; // cycle — this responder is already unwinding this delivery
        try {
            ObjC.msgSendVoidId(next, sel,
                    (MemorySegment)(eventSeg == null ? MemorySegment.NULL : eventSeg));
        } finally {
            visiting.remove(nextAddr);
        }
    }

    /// FFM upcall target: `-(void)mouseDown:(NSEvent *)event`.
    public static void mouseDownImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        MouseListener l = MOUSE_LISTENERS.get(self.address());
        NSEvent e = l == null ? null : eventOrNil(eventSeg);
        if (l == null || e == null) { forwardChain(self, sel, eventSeg); return; }
        l.onMouseDown(selfView(self), e);
    }

    /// FFM upcall target: `-(void)mouseDragged:(NSEvent *)event`.
    public static void mouseDraggedImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        MouseListener l = MOUSE_LISTENERS.get(self.address());
        NSEvent e = l == null ? null : eventOrNil(eventSeg);
        if (l == null || e == null) { forwardChain(self, sel, eventSeg); return; }
        l.onMouseDragged(selfView(self), e);
    }

    /// FFM upcall target: `-(void)mouseUp:(NSEvent *)event`.
    public static void mouseUpImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        MouseListener l = MOUSE_LISTENERS.get(self.address());
        NSEvent e = l == null ? null : eventOrNil(eventSeg);
        if (l == null || e == null) { forwardChain(self, sel, eventSeg); return; }
        l.onMouseUp(selfView(self), e);
    }

    /// FFM upcall target: `-(void)mouseMoved:(NSEvent *)event`.
    public static void mouseMovedImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        MouseListener l = MOUSE_LISTENERS.get(self.address());
        NSEvent e = l == null ? null : eventOrNil(eventSeg);
        if (l == null || e == null) { forwardChain(self, sel, eventSeg); return; }
        l.onMouseMoved(selfView(self), e);
    }

    /// FFM upcall target: `-(void)mouseEntered:(NSEvent *)event`.
    public static void mouseEnteredImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        MouseListener l = MOUSE_LISTENERS.get(self.address());
        NSEvent e = l == null ? null : eventOrNil(eventSeg);
        if (l == null || e == null) { forwardChain(self, sel, eventSeg); return; }
        l.onMouseEntered(selfView(self), e);
    }

    /// FFM upcall target: `-(void)mouseExited:(NSEvent *)event`.
    public static void mouseExitedImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        MouseListener l = MOUSE_LISTENERS.get(self.address());
        NSEvent e = l == null ? null : eventOrNil(eventSeg);
        if (l == null || e == null) { forwardChain(self, sel, eventSeg); return; }
        l.onMouseExited(selfView(self), e);
    }

    /// FFM upcall target: `-(void)keyDown:(NSEvent *)event`. A key listener
    /// returning true consumes the event; false hands the event to the next
    /// responder, continuing the responder chain.
    public static void keyDownImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        KeyListener l = KEY_LISTENERS.get(self.address());
        NSEvent e = l == null ? null : eventOrNil(eventSeg);
        if (l != null && e != null && l.onKeyDown(selfView(self), e)) return;
        forwardChain(self, sel, eventSeg);
    }

    /// FFM upcall target: `-(void)keyUp:(NSEvent *)event`.
    public static void keyUpImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        KeyListener l = KEY_LISTENERS.get(self.address());
        NSEvent e = l == null ? null : eventOrNil(eventSeg);
        if (l != null && e != null && l.onKeyUp(selfView(self), e)) return;
        forwardChain(self, sel, eventSeg);
    }

    /// FFM upcall target: `-(void)flagsChanged:(NSEvent *)event`.
    public static void flagsChangedImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        KeyListener l = KEY_LISTENERS.get(self.address());
        NSEvent e = l == null ? null : eventOrNil(eventSeg);
        if (l != null && e != null && l.onFlagsChanged(selfView(self), e)) return;
        forwardChain(self, sel, eventSeg);
    }

    /// FFM upcall target: `-(BOOL)performKeyEquivalent:(NSEvent *)event`.
    /// Always answers NO ("not handled") and does NOT forward. AppKit delivers
    /// key equivalents by walking the whole view tree itself (the window sends
    /// this to the content view, whose default walks every subview), so the
    /// NSResponder-style "forward to next responder" is not just unnecessary —
    /// for a window whose contentView is one of our subclasses it is a cycle:
    /// view → window → window's tree-walk → same view → … (observed live as a
    /// StackOverflowError inside the upcall stub). Returning NO lets AppKit
    /// finish its own traversal; unhandled equivalents still surface as
    /// `keyDown:` on the first responder. There is deliberately no Java hook
    /// here yet — add `onKeyEquivalent` to KeyListener when one is needed.
    public static boolean performKeyEquivalentImpl(MemorySegment self, MemorySegment sel, MemorySegment eventSeg) {
        return false;
    }

    /// FFM upcall target: `-(BOOL)acceptsFirstResponder`. True exactly when a
    /// key listener is registered for this view: a view that wants keys must be
    /// able to take first-responder status, while pure drawing views must not
    /// steal focus from controls.
    public static boolean acceptsFirstResponderImpl(MemorySegment self, MemorySegment sel) {
        return KEY_LISTENERS.containsKey(self.address());
    }

    // ---------------------------------------------------------------- instance API

    /// addSubview: — attach a child view.
    public void addSubview(NSView subview) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.addSubview, subview.peer());
    }

    /// setFrame: — reposition/resize in the superview's coordinates.
    public void setFrame(NSRect frame) {
        ensureInit();
        try {
            H.hSetFrame().invokeExact(peer, Sels.setFrame, frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setFrame: failed", t);
        }
    }

    /// setAutoresizingMask: — how the view resizes when its superview (the window's
    /// content view) resizes. NSViewAutoresizing bits: MinXMargin=1 WidthSizable=2
    /// MaxXMargin=4 MinYMargin=8 HeightSizable=16 MaxYMargin=32. The margin bits pin
    /// the corresponding edge; the Sizable bits let the dimension flex. Without a mask
    /// a subview keeps its absolute frame and does not track window resizes.
    public void setAutoresizingMask(long mask) {
        ensureInit();
        try {
            H.hAutoMask().invokeExact(peer, Sels.setAutoresizingMask, mask);
        } catch (Throwable t) {
            throw new RuntimeException("setAutoresizingMask: failed", t);
        }
    }


    /// bounds — the view's own coordinate system (origin usually {0,0}).
    public NSRect bounds() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.bounds));
    }

    /// setBounds: — set the view's bounds.
    public void setBounds(NSRect bounds) {
        ensureInit();
        try {
            H.hSetBounds().invokeExact(peer, Sels.setBounds, bounds.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setBounds: failed", t);
        }
    }

    /// frame — the view's frame in its superview's coordinates (struct return).
    public NSRect frame() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.frame));
    }

    /// needsDisplay — whether the view needs display.
    public boolean needsDisplay() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.needsDisplay);
    }

    /// setNeedsDisplay: — request a redraw on the next run-loop pass.
    public void setNeedsDisplay(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setNeedsDisplay, flag);
    }

    /// setNeedsDisplayInRect: — mark only the given region, in the view's own coordinate
    /// system, as needing redraw. AppKit unions repeated invalidations and passes the
    /// resulting (possibly expanded) rect to `drawRect:`; drawing may be clipped to it.
    /// This is the cost-saving entry point for dirty-rect rendering: a view that only repaints
    /// `rect` avoids a full-bounds redraw.
    public void setNeedsDisplayInRect(NSRect rect) {
        ensureInit();
        try {
            H.hNeedsRect().invokeExact(peer, Sels.setNeedsDisplayInRect, rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setNeedsDisplayInRect: failed", t);
        }
    }

    /// backingScaleFactor — the view's backing store scale (1.0 for a non-Retina screen,
    /// 2.0 for Retina). Combined with `setWantsLayer` this is the basis for
    /// correct Retina/backing-scale rendering: drawing coordinates are in points while the
    /// backing store is pixels, so device-space sizes equal point sizes times this factor.
    public double backingScaleFactor() {
        ensureInit();
        try {
            return (double) H.hBacking().invokeExact(peer, Sels.backingScaleFactor);
        } catch (Throwable t) {
            throw new RuntimeException("backingScaleFactor failed", t);
        }
    }

    /// convertRectToBacking: — a point-space rect in the view's coordinates -> backing pixels.
    public NSRect convertRectToBacking(NSRect rect) {
        ensureInit();
        try {
            return NSRect.fromSegment((MemorySegment) H.hConvBacking().invokeExact(
                    ObjC.structSlot(), peer,
                    Sels.convertRectToBacking, rect.toSegment()));
        } catch (Throwable t) {
            throw new RuntimeException("convertRectToBacking: failed", t);
        }
    }

    /// window — the NSWindow this view is installed in (null if none).
    public NSObject window() {
        ensureInit();
        return NSObject.wrap(ObjC.msgSendId(peer, Sels.window));
    }

    /// setWantsLayer: — opt into layer-backed drawing.
    public void setWantsLayer(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setWantsLayer, flag);
    }

    /// wantsLayer — whether the view is layer-backed.
    public boolean wantsLayer() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.wantsLayer);
    }

    /// isFlipped — whether the view's y-axis points up (NO for a plain NSView).
    public boolean isFlipped() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isFlipped);
    }

    /// layer — the view's backing CALayer (null if not layer-backed). Typed via CALayer.
    public CALayer layer() {
        ensureInit();
        return CALayer.wrap(ObjC.msgSendId(peer, Sels.layer));
    }

    /// setLayer: — assign a CALayer
    public void setLayer(CALayer layer) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setLayer, (MemorySegment) (layer == null ? MemorySegment.NULL : layer.peer()));
    }

    // ---------------------------------------------------------------- additional getters — completeness

    /// autoresizingMask — NSAutoresizingMaskOptions.
    public long autoresizingMask() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.autoresizingMask);
    }

    /// autoresizesSubviews.
    public boolean autoresizesSubviews() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.autoresizesSubviews);
    }

    /// setAutoresizesSubviews:.
    public void setAutoresizesSubviews(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setAutoresizesSubviews, flag);
    }

    /// translatesAutoresizingMaskIntoConstraints.
    public boolean translatesAutoresizingMaskIntoConstraints() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.translatesAutoresizingMaskIntoConstraints);
    }

    /// setTranslatesAutoresizingMaskIntoConstraints:.
    public void setTranslatesAutoresizingMaskIntoConstraints(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setTranslatesAutoresizingMaskIntoConstraints, flag);
    }

    // ---------------------------------------------------------------- Auto Layout — anchors & constraints

    /// leadingAnchor — NSLayoutXAxisAnchor.
    public NSLayoutAnchor leadingAnchor() {
        ensureInit();
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, Sels.leadingAnchor));
    }

    /// trailingAnchor — NSLayoutXAxisAnchor.
    public NSLayoutAnchor trailingAnchor() {
        ensureInit();
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, Sels.trailingAnchor));
    }

    /// topAnchor — NSLayoutYAxisAnchor.
    public NSLayoutAnchor topAnchor() {
        ensureInit();
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, Sels.topAnchor));
    }

    /// bottomAnchor — NSLayoutYAxisAnchor.
    public NSLayoutAnchor bottomAnchor() {
        ensureInit();
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, Sels.bottomAnchor));
    }

    /// widthAnchor — NSLayoutDimension.
    public NSLayoutAnchor widthAnchor() {
        ensureInit();
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, Sels.widthAnchor));
    }

    /// heightAnchor — NSLayoutDimension.
    public NSLayoutAnchor heightAnchor() {
        ensureInit();
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, Sels.heightAnchor));
    }

    /// centerXAnchor — NSLayoutXAxisAnchor.
    public NSLayoutAnchor centerXAnchor() {
        ensureInit();
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, Sels.centerXAnchor));
    }

    /// centerYAnchor — NSLayoutYAxisAnchor.
    public NSLayoutAnchor centerYAnchor() {
        ensureInit();
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, Sels.centerYAnchor));
    }

    /// addConstraint: — install a single layout constraint on this view.
    public void addConstraint(NSLayoutConstraint constraint) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.addConstraint, constraint.peer());
    }

    /// addConstraints: — install multiple constraints (loops over addConstraint: for simplicity).
    public void addConstraints(java.util.List<NSLayoutConstraint> constraints) {
        for (NSLayoutConstraint c : constraints) addConstraint(c);
    }

    /// removeConstraint: — remove a previously-added constraint.
    public void removeConstraint(NSLayoutConstraint constraint) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.removeConstraint, constraint.peer());
    }

    /// removeConstraints: — bulk remove.
    public void removeConstraints(java.util.List<NSLayoutConstraint> constraints) {
        for (NSLayoutConstraint c : constraints) removeConstraint(c);
    }

    /// constraints — the view's installed constraints.
    public java.util.List<NSLayoutConstraint> constraints() {
        ensureInit();
        MemorySegment arr = ObjC.msgSendId(peer, Sels.constraints);
        if (arr == null || arr.address() == 0) return java.util.List.of();
        long count = ObjC.msgSendLong(arr, Sels.count);
        java.util.List<NSLayoutConstraint> list = new java.util.ArrayList<>((int) count);
        MemorySegment selAt = Sels.objectAtIndex;
        MethodHandle h = H.hObjectAtIndex();
        for (long i = 0; i < count; i++) {
            try {
                MemorySegment v = (MemorySegment) h.invokeExact(arr, selAt, i);
                if (v != null && v.address() != 0) list.add(NSLayoutConstraint.wrap(v));
            } catch (Throwable t) {
                throw new RuntimeException("constraints objectAtIndex failed", t);
            }
        }
        return java.util.Collections.unmodifiableList(list);
    }

    /// displayIfNeeded — display the view if needed.
    public void displayIfNeeded() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.displayIfNeeded);
    }

    /// displayIfNeededInRect: — display if needed in rect.
    public void displayIfNeededInRect(NSRect rect) {
        ensureInit();
        try {
            H.hNeedsRect().invokeExact(peer, Sels.displayIfNeededInRect, rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("displayIfNeededInRect: failed", t);
        }
    }

    /// layoutSubtreeIfNeeded — layout the subtree if needed.
    public void layoutSubtreeIfNeeded() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.layoutSubtreeIfNeeded);
    }

    /// intrinsicContentSize — the view's intrinsic content size.
    public NSSize intrinsicContentSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact(ObjC.structSlot(), peer, Sels.intrinsicContentSize);
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("intrinsicContentSize failed", t);
        }
    }

    /// alphaValue — view alpha 0..1.
    public double alphaValue() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, Sels.alphaValue);
        } catch (Throwable t) {
            throw new RuntimeException("alphaValue failed", t);
        }
    }

    /// setAlphaValue:.
    public void setAlphaValue(double alpha) {
        ensureInit();
        try {
            H.hSetDouble().invokeExact(peer, Sels.setAlphaValue, alpha);
        } catch (Throwable t) {
            throw new RuntimeException("setAlphaValue: failed", t);
        }
    }

    /// isHidden.
    public boolean isHidden() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isHidden);
    }

    /// setHidden:.
    public void setHidden(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setHidden, flag);
    }

    /// isHiddenOrHasHiddenAncestor.
    public boolean isHiddenOrHasHiddenAncestor() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isHiddenOrHasHiddenAncestor);
    }

    /// superview — parent view.
    public NSView superview() {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(peer, Sels.superview);
        return NSView.wrap(v);
    }

    /// isOpaque — whether the view is opaque.
    public boolean isOpaque() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isOpaque);
    }

    /// visibleRect — the visible rect (readonly).
    public NSRect visibleRect() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.visibleRect));
    }

    /// isRotatedFromBase
    public boolean isRotatedFromBase() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isRotatedFromBase);
    }

    /// isRotatedOrScaledFromBase
    public boolean isRotatedOrScaledFromBase() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isRotatedOrScaledFromBase);
    }

    /// canBecomeKeyView
    public boolean canBecomeKeyView() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.canBecomeKeyView);
    }

    /// enclosingScrollView
    public NSView enclosingScrollView() {
        ensureInit();
        MemorySegment v = ObjC.msgSendId(peer, Sels.enclosingScrollView);
        return NSView.wrap(v);
    }

    /// invalidateIntrinsicContentSize.
    public void invalidateIntrinsicContentSize() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.invalidateIntrinsicContentSize);
    }

    // ---- dragging support (Phase 0B) ----

    /// registerForDraggedTypes: — register pasteboard types this view accepts for drops.
    public void registerForDraggedTypes(java.util.List<String> types) {
        ensureInit();
        MemorySegment arr;
        if (types == null || types.isEmpty()) {
            arr = ObjC.msgSendId(ObjC.cls("NSArray"), Sels.array);
        } else {
            arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), Sels.array);
            for (String t : types) {
                if (t == null) continue;
                MemorySegment ns = ObjC.nsstring(t);
                ObjC.msgSendVoidId(arr, Sels.addObject, ns);
            }
        }
        try {
            H.hRegisterForDraggedTypes().invokeExact(peer, Sels.registerForDraggedTypes, arr);
        } catch (Throwable t) {
            throw new RuntimeException("registerForDraggedTypes: failed", t);
        }
    }

    /// unregisterDraggedTypes — unregister all previously registered drag types.
    public void unregisterDraggedTypes() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.unregisterDraggedTypes);
    }

    /// beginDraggingSessionWithItems:event:source: — begin a dragging session.
    /// Best-effort: converts items to NSArray, handles nulls gracefully, wraps result.
    /// If event or source is null/NULL, AppKit would dereference and SIGSEGV; we return null gracefully instead.
    public NSDraggingSession beginDraggingSessionWithItems(java.util.List<NSDraggingItem> items, NSEvent event, NSDraggingSource source) {
        ensureInit();
        MemorySegment arr;
        if (items == null || items.isEmpty()) {
            arr = ObjC.msgSendId(ObjC.cls("NSArray"), Sels.array);
        } else {
            arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), Sels.array);
            for (NSDraggingItem item : items) {
                if (item == null || item.peer() == null || item.peer().address() == 0) continue;
                ObjC.msgSendVoidId(arr, Sels.addObject, item.peer());
            }
        }
        MemorySegment eventSeg = (event == null || event.peer() == null || event.peer().address() == 0) ? MemorySegment.NULL : event.peer();
        MemorySegment sourceSeg = (source == null || source.peer() == null || source.peer().address() == 0) ? MemorySegment.NULL : source.peer();
        // Graceful best-effort: AppKit crashes (SIGSEGV) if event is NULL; return null instead.
        if (eventSeg.address() == 0 || sourceSeg.address() == 0) {
            return null;
        }
        try {
            MemorySegment sess = (MemorySegment) H.hBeginDraggingSession().invokeExact(peer, Sels.beginDraggingSessionWithItems_event_source, arr, eventSeg, sourceSeg);
            return NSDraggingSession.wrap(sess);
        } catch (Throwable t) {
            // Also graceful: if AppKit raises (e.g. view not in window), return null rather than throw
            // But preserve original exception for debugging if it's a vocabulary miss
            if (t.getMessage() != null && t.getMessage().contains("vocabulary")) throw new RuntimeException("beginDraggingSessionWithItems:event:source: failed", t);
            return null;
        }
    }

    // ---- bitmap caching (for PNG snapshot) ----

    /// [view bitmapImageRepForCachingDisplayInRect:] — create a rep sized for caching.
    public NSBitmapImageRep bitmapImageRepForCachingDisplayInRect(NSRect rect) {
        ensureInit();
        try {
            MemorySegment rep = (MemorySegment) H.hRepForCache().invokeExact(peer, Sels.bitmapImageRepForCachingDisplayInRect, rect.toSegment());
            return NSBitmapImageRep.wrap(rep);
        } catch (Throwable e) { throw new RuntimeException("bitmapImageRepForCachingDisplayInRect: failed", e); }
    }

    /// [view cacheDisplayInRect:toBitmapImageRep:] — render into a rep.
    public void cacheDisplayInRectToBitmapImageRep(NSRect rect, NSBitmapImageRep rep) {
        ensureInit();
        try {
            H.hCacheDisplay().invokeExact(peer, Sels.cacheDisplayInRect_toBitmapImageRep, rect.toSegment(), (MemorySegment)(rep == null ? MemorySegment.NULL : rep.peer()));
        } catch (Throwable e) { throw new RuntimeException("cacheDisplayInRect:toBitmapImageRep: failed", e); }
    }

    // ---------------------------------------------------------------- header-completeness batch (NSView.h)
    //
    // Every method below maps 1:1 to one AppKit selector and uses only registered
    // vocabulary shapes (typed ObjC helpers where they exist, per-class handles otherwise).
    //
    // Omitted (with reason + requested shape where applicable):
    // - delegate protocols (NSViewLayerContentScaleDelegate layer:shouldInheritContentsScale:fromWindow:) — need upcall machinery.
    // - block-taking: sortSubviewsUsingFunction:context:, showDefinitionForAttributedString:range:options:baselineOriginProvider:.
    // - deprecated: lockFocus/unlockFocus/lockFocusIfCanDraw(InContext:), scrollRect:by:, dragImage:at:offset:event:pasteboard:source:slideBack:,
    //   dragFile:fromRect:slideBack:event:, dragPromisedFilesOfTypes:..., convertPoint/Size/RectTo/FromBase, performMnemonic,
    //   shouldDrawColor, gState/allocateGState/releaseGState/setUpGState/renewGState, drawSheetBorderWithSize:, canDraw, acceptsTouchEvents.
    // - init overloads (initWithCoder:) — covered by create().
    // - out-params / C pointers: getRectsBeingDrawn:count:, getRectsExposedDuringLiveResize:count:,
    //   knowsPageRange:, adjustPageWidthNew:..., adjustPageHeightNew:....
    // - inexpressible shapes (absent from Sig vocabulary, requested): convertPoint:fromView:/toView: and
    //   convertSize:fromView:/toView: and convertRect:fromView:/toView: need of(POINT,POINT,ID)/of(SIZE,SIZE,ID)/of(RECT,RECT,ID);
    //   convertPointToBacking:/FromBacking:/ToLayer:/FromLayer: need of(POINT,POINT); backingAlignedRect:options: needs
    //   of(RECT,RECT,INT); needsToDrawRect:/scrollRectToVisible: need of(BOOL,RECT); mouse:inRect: needs of(BOOL,POINT,RECT);
    //   addToolTipRect:owner:userData: needs of(INT,RECT,ID,ID); stringForToolTip helpers need of(ID,ID,INT,POINT,ID);
    //   scrollClipView:toPoint: (NSClipViewSuperview) needs of(VOID,ID,POINT); rectForPage: needs of(RECT,INT);
    //   locationOfPrintRect: needs of(POINT,RECT); beginPageInRect:atPlacement: needs of(VOID,RECT,POINT);
    //   showDefinitionForAttributedString:atPoint: needs of(VOID,ID,POINT); translateRectsNeedingDisplayInRect:by: needs
    //   of(VOID,RECT,SIZE).
    // - NSViewContentSelectionInfo protocol (selectionAnchorRect, showContextMenuForSelection:) — optional
    //   protocol methods need upcall machinery to conform; NSView itself does not implement selectionAnchorRect.
    // - reflectScrolledClipView: (NSClipView.h NSView category): declared in the SDK header but NOT
    //   implemented by the macOS 26.5 runtime (respondsToSelector: returns NO on every NSView) — calling it
    //   always raises NSInvalidArgumentException, so it is omitted rather than wrapped.

    /// [view isDescendantOf:] — YES when the receiver is inside view's subtree.
    public boolean isDescendantOf(NSView view) {
        ensureInit();
        try {
            return (boolean) H.hBoolId().invokeExact(peer, Sels.isDescendantOf, view.peer());
        } catch (Throwable t) {
            throw new RuntimeException("isDescendantOf: failed", t);
        }
    }

    /// [view ancestorSharedWithView:] — nearest common ancestor (nil-safe).
    public NSView ancestorSharedWithView(NSView view) {
        ensureInit();
        return NSView.wrap(ObjC.msgSendIdId(peer, Sels.ancestorSharedWithView, view.peer()));
    }

    /// [view subviews] — direct children.
    public NSArray subviews() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.subviews));
    }

    /// [view setSubviews:].
    public void setSubviews(NSArray views) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, Sels.setSubviews, views.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setSubviews: failed", t);
        }
    }

    /// [view opaqueAncestor] — nearest opaque ancestor (may be nil).
    public NSView opaqueAncestor() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(peer, Sels.opaqueAncestor));
    }

    /// [view removeFromSuperview].
    public void removeFromSuperview() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.removeFromSuperview);
    }

    /// [view replaceSubview:with:].
    public void replaceSubview(NSView oldView, NSView newView) {
        ensureInit();
        try {
            H.hVoidIdId().invokeExact(peer, Sels.replaceSubview_with, oldView.peer(), newView.peer());
        } catch (Throwable t) {
            throw new RuntimeException("replaceSubview:with: failed", t);
        }
    }

    /// [view removeFromSuperviewWithoutNeedingDisplay].
    public void removeFromSuperviewWithoutNeedingDisplay() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.removeFromSuperviewWithoutNeedingDisplay);
    }

    /// [view viewWillMoveToWindow:] (nil allowed).
    public void viewWillMoveToWindow(NSWindow window) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, Sels.viewWillMoveToWindow, (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("viewWillMoveToWindow: failed", t);
        }
    }

    /// [view viewDidMoveToWindow].
    public void viewDidMoveToWindow() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.viewDidMoveToWindow);
    }

    /// [view viewWillMoveToSuperview:] (nil allowed).
    public void viewWillMoveToSuperview(NSView superview) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, Sels.viewWillMoveToSuperview, (MemorySegment) (superview == null ? MemorySegment.NULL : superview.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("viewWillMoveToSuperview: failed", t);
        }
    }

    /// [view viewDidMoveToSuperview].
    public void viewDidMoveToSuperview() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.viewDidMoveToSuperview);
    }

    /// [view didAddSubview:].
    public void didAddSubview(NSView subview) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.didAddSubview, subview.peer());
    }

    /// [view willRemoveSubview:].
    public void willRemoveSubview(NSView subview) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.willRemoveSubview, subview.peer());
    }

    /// [view viewDidHide].
    public void viewDidHide() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.viewDidHide);
    }

    /// [view viewDidUnhide].
    public void viewDidUnhide() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.viewDidUnhide);
    }

    /// [view viewDidChangeBackingProperties].
    public void viewDidChangeBackingProperties() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.viewDidChangeBackingProperties);
    }

    /// [view setFrameOrigin:] — move without resizing.
    public void setFrameOrigin(NSPoint origin) {
        ensureInit();
        try {
            H.hSetPoint().invokeExact(peer, Sels.setFrameOrigin, origin.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setFrameOrigin: failed", t);
        }
    }

    /// [view setFrameSize:] — resize keeping origin.
    public void setFrameSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setFrameSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setFrameSize: failed", t);
        }
    }

    /// [view setBoundsOrigin:].
    public void setBoundsOrigin(NSPoint origin) {
        ensureInit();
        try {
            H.hSetPoint().invokeExact(peer, Sels.setBoundsOrigin, origin.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setBoundsOrigin: failed", t);
        }
    }

    /// [view setBoundsSize:].
    public void setBoundsSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.setBoundsSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setBoundsSize: failed", t);
        }
    }

    /// [view translateOriginToPoint:].
    public void translateOriginToPoint(NSPoint point) {
        ensureInit();
        try {
            H.hSetPoint().invokeExact(peer, Sels.translateOriginToPoint, point.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("translateOriginToPoint: failed", t);
        }
    }

    /// [view scaleUnitSquareToSize:].
    public void scaleUnitSquareToSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.scaleUnitSquareToSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("scaleUnitSquareToSize: failed", t);
        }
    }

    /// [view rotateByAngle:] — CGFloat degrees.
    public void rotateByAngle(double angle) {
        ensureInit();
        try {
            H.hSetDouble().invokeExact(peer, Sels.rotateByAngle, angle);
        } catch (Throwable t) {
            throw new RuntimeException("rotateByAngle: failed", t);
        }
    }

    /// [view frameRotation] — CGFloat degrees.
    public double frameRotation() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, Sels.frameRotation);
        } catch (Throwable t) {
            throw new RuntimeException("frameRotation failed", t);
        }
    }

    /// [view setFrameRotation:].
    public void setFrameRotation(double angle) {
        ensureInit();
        try {
            H.hSetDouble().invokeExact(peer, Sels.setFrameRotation, angle);
        } catch (Throwable t) {
            throw new RuntimeException("setFrameRotation: failed", t);
        }
    }

    /// [view frameCenterRotation].
    public double frameCenterRotation() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, Sels.frameCenterRotation);
        } catch (Throwable t) {
            throw new RuntimeException("frameCenterRotation failed", t);
        }
    }

    /// [view setFrameCenterRotation:].
    public void setFrameCenterRotation(double angle) {
        ensureInit();
        try {
            H.hSetDouble().invokeExact(peer, Sels.setFrameCenterRotation, angle);
        } catch (Throwable t) {
            throw new RuntimeException("setFrameCenterRotation: failed", t);
        }
    }

    /// [view boundsRotation].
    public double boundsRotation() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, Sels.boundsRotation);
        } catch (Throwable t) {
            throw new RuntimeException("boundsRotation failed", t);
        }
    }

    /// [view setBoundsRotation:].
    public void setBoundsRotation(double angle) {
        ensureInit();
        try {
            H.hSetDouble().invokeExact(peer, Sels.setBoundsRotation, angle);
        } catch (Throwable t) {
            throw new RuntimeException("setBoundsRotation: failed", t);
        }
    }

    /// [view postsFrameChangedNotifications].
    public boolean postsFrameChangedNotifications() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.postsFrameChangedNotifications);
    }

    /// [view setPostsFrameChangedNotifications:].
    public void setPostsFrameChangedNotifications(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setPostsFrameChangedNotifications, flag);
    }

    /// [view postsBoundsChangedNotifications].
    public boolean postsBoundsChangedNotifications() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.postsBoundsChangedNotifications);
    }

    /// [view setPostsBoundsChangedNotifications:].
    public void setPostsBoundsChangedNotifications(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setPostsBoundsChangedNotifications, flag);
    }

    /// [view resizeSubviewsWithOldSize:].
    public void resizeSubviewsWithOldSize(NSSize oldSize) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.resizeSubviewsWithOldSize, oldSize.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("resizeSubviewsWithOldSize: failed", t);
        }
    }

    /// [view resizeWithOldSuperviewSize:].
    public void resizeWithOldSuperviewSize(NSSize oldSize) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.resizeWithOldSuperviewSize, oldSize.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("resizeWithOldSuperviewSize: failed", t);
        }
    }

    /// [view display] — draw now, outside the deferred mechanism.
    public void display() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.display);
    }

    /// [view displayIfNeededIgnoringOpacity].
    public void displayIfNeededIgnoringOpacity() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.displayIfNeededIgnoringOpacity);
    }

    /// [view displayRect:].
    public void displayRect(NSRect rect) {
        ensureInit();
        try {
            H.hSetBounds().invokeExact(peer, Sels.displayRect, rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("displayRect: failed", t);
        }
    }

    /// [view displayRectIgnoringOpacity:].
    public void displayRectIgnoringOpacity(NSRect rect) {
        ensureInit();
        try {
            H.hSetBounds().invokeExact(peer, Sels.displayRectIgnoringOpacity, rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("displayRectIgnoringOpacity: failed", t);
        }
    }

    /// [view displayIfNeededInRectIgnoringOpacity:].
    public void displayIfNeededInRectIgnoringOpacity(NSRect rect) {
        ensureInit();
        try {
            H.hSetBounds().invokeExact(peer, Sels.displayIfNeededInRectIgnoringOpacity, rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("displayIfNeededInRectIgnoringOpacity: failed", t);
        }
    }

    /// [view displayRectIgnoringOpacity:inContext:].
    public void displayRectIgnoringOpacityInContext(NSRect rect, NSGraphicsContext context) {
        ensureInit();
        try {
            H.hCacheDisplay().invokeExact(peer, Sels.displayRectIgnoringOpacity_inContext, rect.toSegment(), context.peer());
        } catch (Throwable t) {
            throw new RuntimeException("displayRectIgnoringOpacity:inContext: failed", t);
        }
    }

    /// [view viewWillDraw].
    public void viewWillDraw() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.viewWillDraw);
    }

    /// [view wantsDefaultClipping].
    public boolean wantsDefaultClipping() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.wantsDefaultClipping);
    }

    /// [view canDrawConcurrently].
    public boolean canDrawConcurrently() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.canDrawConcurrently);
    }

    /// [view setCanDrawConcurrently:].
    public void setCanDrawConcurrently(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setCanDrawConcurrently, flag);
    }

    /// [view needsLayout].
    public boolean needsLayout() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.needsLayout);
    }

    /// [view setNeedsLayout:].
    public void setNeedsLayout(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setNeedsLayout, flag);
    }

    /// [view layout] — lay out the view (called by layoutSubtreeIfNeeded).
    public void layout() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.layout);
    }

    /// [view updateLayer] — update the backing layer's content.
    public void updateLayer() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.updateLayer);
    }

    /// [view makeBackingLayer] — create the layer for layer-backed drawing.
    public CALayer makeBackingLayer() {
        ensureInit();
        return CALayer.wrap(ObjC.msgSendId(peer, Sels.makeBackingLayer));
    }

    /// [view centerScanRect:] — pixel-aligned rect for scanning.
    public NSRect centerScanRect(NSRect rect) {
        ensureInit();
        try {
            return NSRect.fromSegment((MemorySegment) H.hConvBacking().invokeExact(ObjC.structSlot(), peer, Sels.centerScanRect, rect.toSegment()));
        } catch (Throwable t) {
            throw new RuntimeException("centerScanRect: failed", t);
        }
    }

    /// [view adjustScroll:] — constrain a proposed visible rect.
    public NSRect adjustScroll(NSRect rect) {
        ensureInit();
        try {
            return NSRect.fromSegment((MemorySegment) H.hConvBacking().invokeExact(ObjC.structSlot(), peer, Sels.adjustScroll, rect.toSegment()));
        } catch (Throwable t) {
            throw new RuntimeException("adjustScroll: failed", t);
        }
    }

    /// [view convertRectFromBacking:].
    public NSRect convertRectFromBacking(NSRect rect) {
        ensureInit();
        try {
            return NSRect.fromSegment((MemorySegment) H.hConvBacking().invokeExact(ObjC.structSlot(), peer, Sels.convertRectFromBacking, rect.toSegment()));
        } catch (Throwable t) {
            throw new RuntimeException("convertRectFromBacking: failed", t);
        }
    }

    /// [view convertSizeToBacking:].
    public NSSize convertSizeToBacking(NSSize size) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hSizeSize().invokeExact(ObjC.structSlot(), peer, Sels.convertSizeToBacking, size.toSegment());
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertSizeToBacking: failed", t);
        }
    }

    /// [view convertSizeFromBacking:].
    public NSSize convertSizeFromBacking(NSSize size) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hSizeSize().invokeExact(ObjC.structSlot(), peer, Sels.convertSizeFromBacking, size.toSegment());
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertSizeFromBacking: failed", t);
        }
    }

    /// [view convertSizeToLayer:].
    public NSSize convertSizeToLayer(NSSize size) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hSizeSize().invokeExact(ObjC.structSlot(), peer, Sels.convertSizeToLayer, size.toSegment());
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertSizeToLayer: failed", t);
        }
    }

    /// [view convertSizeFromLayer:].
    public NSSize convertSizeFromLayer(NSSize size) {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hSizeSize().invokeExact(ObjC.structSlot(), peer, Sels.convertSizeFromLayer, size.toSegment());
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("convertSizeFromLayer: failed", t);
        }
    }

    /// [view scrollPoint:] — scroll to bring a point visible.
    public void scrollPoint(NSPoint point) {
        ensureInit();
        try {
            H.hSetPoint().invokeExact(peer, Sels.scrollPoint, point.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("scrollPoint: failed", t);
        }
    }

    /// [view autoscroll:] — auto-scroll during a drag given an event.
    public boolean autoscroll(NSEvent event) {
        ensureInit();
        try {
            return (boolean) H.hBoolId().invokeExact(peer, Sels.autoscroll, event.peer());
        } catch (Throwable t) {
            throw new RuntimeException("autoscroll: failed", t);
        }
    }

    /// [view hitTest:] — deepest descendant containing the point (nil-safe).
    public NSView hitTest(NSPoint point) {
        ensureInit();
        try {
            MemorySegment v = (MemorySegment) H.hIdPoint().invokeExact(peer, Sels.hitTest, point.toSegment());
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("hitTest: failed", t);
        }
    }

    /// [view viewWithTag:] — descendant with the tag (nil-safe).
    public NSView viewWithTag(long tag) {
        ensureInit();
        try {
            MemorySegment v = (MemorySegment) H.hObjectAtIndex().invokeExact(peer, Sels.viewWithTag, tag);
            return NSView.wrap(v);
        } catch (Throwable t) {
            throw new RuntimeException("viewWithTag: failed", t);
        }
    }

    /// [view tag] — the view's tag (0 when unset).
    public long tag() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.tag);
    }

    /// [view needsPanelToBecomeKey].
    public boolean needsPanelToBecomeKey() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.needsPanelToBecomeKey);
    }

    /// [view mouseDownCanMoveWindow].
    public boolean mouseDownCanMoveWindow() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.mouseDownCanMoveWindow);
    }

    /// [view menuForEvent:] — context menu for an event (may be nil).
    public NSMenu menuForEvent(NSEvent event) {
        ensureInit();
        return NSMenu.wrap(ObjC.msgSendIdId(peer, Sels.menuForEvent, event.peer()));
    }

    /// [view willOpenMenu:withEvent:].
    public void willOpenMenu(NSMenu menu, NSEvent event) {
        ensureInit();
        try {
            H.hVoidIdId().invokeExact(peer, Sels.willOpenMenu_withEvent, menu.peer(), event.peer());
        } catch (Throwable t) {
            throw new RuntimeException("willOpenMenu:withEvent: failed", t);
        }
    }

    /// [view didCloseMenu:withEvent:] (event may be nil).
    public void didCloseMenu(NSMenu menu, NSEvent event) {
        ensureInit();
        try {
            H.hVoidIdId().invokeExact(peer, Sels.didCloseMenu_withEvent, menu.peer(), (MemorySegment) (event == null ? MemorySegment.NULL : event.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("didCloseMenu:withEvent: failed", t);
        }
    }

    /// [+NSView defaultMenu] — the class default menu (may be nil).
    public static NSMenu defaultMenu() {
        ensureInit();
        return NSMenu.wrap(ObjC.msgSendId(ObjC.cls("NSView"), Sels.defaultMenu));
    }

    /// [view toolTip] — tooltip string (may be nil).
    public String toolTip() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.toolTip));
    }

    /// [view setToolTip:] (nil clears).
    public void setToolTip(String tip) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setToolTip, tip == null ? MemorySegment.NULL : ObjC.nsstring(tip));
    }

    /// [view removeToolTip:].
    public void removeToolTip(long tag) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.removeToolTip, tag);
    }

    /// [view removeAllToolTips].
    public void removeAllToolTips() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.removeAllToolTips);
    }

    /// [view viewWillStartLiveResize].
    public void viewWillStartLiveResize() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.viewWillStartLiveResize);
    }

    /// [view viewDidEndLiveResize].
    public void viewDidEndLiveResize() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.viewDidEndLiveResize);
    }

    /// [view inLiveResize].
    public boolean inLiveResize() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.inLiveResize);
    }

    /// [view preservesContentDuringLiveResize].
    public boolean preservesContentDuringLiveResize() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.preservesContentDuringLiveResize);
    }

    /// [view rectPreservedDuringLiveResize].
    public NSRect rectPreservedDuringLiveResize() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.rectPreservedDuringLiveResize));
    }

    /// [view prepareForReuse].
    public void prepareForReuse() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.prepareForReuse);
    }

    /// [view prepareContentInRect:].
    public void prepareContentInRect(NSRect rect) {
        ensureInit();
        try {
            H.hSetBounds().invokeExact(peer, Sels.prepareContentInRect, rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("prepareContentInRect: failed", t);
        }
    }

    /// [view viewDidChangeEffectiveAppearance].
    public void viewDidChangeEffectiveAppearance() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.viewDidChangeEffectiveAppearance);
    }

    /// [view setKeyboardFocusRingNeedsDisplayInRect:].
    public void setKeyboardFocusRingNeedsDisplayInRect(NSRect rect) {
        ensureInit();
        try {
            H.hSetBounds().invokeExact(peer, Sels.setKeyboardFocusRingNeedsDisplayInRect, rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setKeyboardFocusRingNeedsDisplayInRect: failed", t);
        }
    }

    /// [view drawFocusRingMask].
    public void drawFocusRingMask() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.drawFocusRingMask);
    }

    /// [view noteFocusRingMaskChanged].
    public void noteFocusRingMaskChanged() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.noteFocusRingMaskChanged);
    }

    /// [view focusRingMaskBounds].
    public NSRect focusRingMaskBounds() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.focusRingMaskBounds));
    }

    /// [view focusRingType] — NSFocusRingType.
    public long focusRingType() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.focusRingType);
    }

    /// [view setFocusRingType:].
    public void setFocusRingType(long type) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setFocusRingType, type);
    }

    /// [+NSView defaultFocusRingType].
    public static long defaultFocusRingType() {
        ensureInit();
        return ObjC.msgSendLong(ObjC.cls("NSView"), Sels.defaultFocusRingType);
    }

    /// [+NSView focusView] — the currently focused view (may be nil).
    public static NSView focusView() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(ObjC.cls("NSView"), Sels.focusView));
    }

    /// [view nextKeyView] (may be nil).
    public NSView nextKeyView() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(peer, Sels.nextKeyView));
    }

    /// [view setNextKeyView:] (nil clears).
    public void setNextKeyView(NSView view) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setNextKeyView, view == null ? MemorySegment.NULL : view.peer());
    }

    /// [view previousKeyView] (may be nil).
    public NSView previousKeyView() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(peer, Sels.previousKeyView));
    }

    /// [view nextValidKeyView] (may be nil).
    public NSView nextValidKeyView() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(peer, Sels.nextValidKeyView));
    }

    /// [view previousValidKeyView] (may be nil).
    public NSView previousValidKeyView() {
        ensureInit();
        return NSView.wrap(ObjC.msgSendId(peer, Sels.previousValidKeyView));
    }

    /// [view layerContentsRedrawPolicy] — NSViewLayerContentsRedrawPolicy.
    public long layerContentsRedrawPolicy() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.layerContentsRedrawPolicy);
    }

    /// [view setLayerContentsRedrawPolicy:].
    public void setLayerContentsRedrawPolicy(long policy) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setLayerContentsRedrawPolicy, policy);
    }

    /// [view layerContentsPlacement] — NSViewLayerContentsPlacement.
    public long layerContentsPlacement() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.layerContentsPlacement);
    }

    /// [view setLayerContentsPlacement:].
    public void setLayerContentsPlacement(long placement) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setLayerContentsPlacement, placement);
    }

    /// [view wantsUpdateLayer].
    public boolean wantsUpdateLayer() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.wantsUpdateLayer);
    }

    /// [view canDrawSubviewsIntoLayer].
    public boolean canDrawSubviewsIntoLayer() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.canDrawSubviewsIntoLayer);
    }

    /// [view setCanDrawSubviewsIntoLayer:].
    public void setCanDrawSubviewsIntoLayer(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setCanDrawSubviewsIntoLayer, flag);
    }

    /// [view layerUsesCoreImageFilters].
    public boolean layerUsesCoreImageFilters() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.layerUsesCoreImageFilters);
    }

    /// [view setLayerUsesCoreImageFilters:].
    public void setLayerUsesCoreImageFilters(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setLayerUsesCoreImageFilters, flag);
    }

    /// [view shadow] — NSShadow (may be nil).
    public NSShadow shadow() {
        ensureInit();
        return NSShadow.wrap(ObjC.msgSendId(peer, Sels.shadow));
    }

    /// [view setShadow:] (nil clears).
    public void setShadow(NSShadow shadow) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.setShadow, shadow == null ? MemorySegment.NULL : shadow.peer());
    }

    /// [view clipsToBounds].
    public boolean clipsToBounds() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.clipsToBounds);
    }

    /// [view setClipsToBounds:].
    public void setClipsToBounds(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setClipsToBounds, flag);
    }

    /// [view wantsRestingTouches].
    public boolean wantsRestingTouches() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.wantsRestingTouches);
    }

    /// [view setWantsRestingTouches:].
    public void setWantsRestingTouches(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setWantsRestingTouches, flag);
    }

    /// [view allowedTouchTypes] — NSTouchTypeMask.
    public long allowedTouchTypes() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.allowedTouchTypes);
    }

    /// [view setAllowedTouchTypes:].
    public void setAllowedTouchTypes(long mask) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setAllowedTouchTypes, mask);
    }

    /// [view print:] (sender may be nil).
    public void print(NSObject sender) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.print, sender == null ? MemorySegment.NULL : sender.peer());
    }

    /// [view pageHeader] — printing header (may be nil).
    public NSAttributedString pageHeader() {
        ensureInit();
        return NSAttributedString.wrap(ObjC.msgSendId(peer, Sels.pageHeader));
    }

    /// [view pageFooter] — printing footer (may be nil).
    public NSAttributedString pageFooter() {
        ensureInit();
        return NSAttributedString.wrap(ObjC.msgSendId(peer, Sels.pageFooter));
    }

    /// [view printJobTitle] (may be nil).
    public String printJobTitle() {
        ensureInit();
        return ObjC.toString(ObjC.msgSendId(peer, Sels.printJobTitle));
    }

    /// [view dataWithEPSInsideRect:] — EPS snapshot data.
    public NSData dataWithEPSInsideRect(NSRect rect) {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) H.hIdRect().invokeExact(peer, Sels.dataWithEPSInsideRect, rect.toSegment());
            return NSData.wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("dataWithEPSInsideRect: failed", t);
        }
    }

    /// [view dataWithPDFInsideRect:] — PDF snapshot data.
    public NSData dataWithPDFInsideRect(NSRect rect) {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) H.hIdRect().invokeExact(peer, Sels.dataWithPDFInsideRect, rect.toSegment());
            return NSData.wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("dataWithPDFInsideRect: failed", t);
        }
    }

    /// [view writeEPSInsideRect:toPasteboard:].
    public void writeEPSInsideRectToPasteboard(NSRect rect, NSPasteboard pasteboard) {
        ensureInit();
        try {
            H.hCacheDisplay().invokeExact(peer, Sels.writeEPSInsideRect_toPasteboard, rect.toSegment(), pasteboard.peer());
        } catch (Throwable t) {
            throw new RuntimeException("writeEPSInsideRect:toPasteboard: failed", t);
        }
    }

    /// [view writePDFInsideRect:toPasteboard:].
    public void writePDFInsideRectToPasteboard(NSRect rect, NSPasteboard pasteboard) {
        ensureInit();
        try {
            H.hCacheDisplay().invokeExact(peer, Sels.writePDFInsideRect_toPasteboard, rect.toSegment(), pasteboard.peer());
        } catch (Throwable t) {
            throw new RuntimeException("writePDFInsideRect:toPasteboard: failed", t);
        }
    }

    /// [view drawPageBorderWithSize:].
    public void drawPageBorderWithSize(NSSize size) {
        ensureInit();
        try {
            H.hSetSize().invokeExact(peer, Sels.drawPageBorderWithSize, size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("drawPageBorderWithSize: failed", t);
        }
    }

    /// [view beginDocument].
    public void beginDocument() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.beginDocument);
    }

    /// [view endDocument].
    public void endDocument() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.endDocument);
    }

    /// [view endPage].
    public void endPage() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.endPage);
    }

    /// [view heightAdjustLimit] — printing height limit.
    public double heightAdjustLimit() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, Sels.heightAdjustLimit);
        } catch (Throwable t) {
            throw new RuntimeException("heightAdjustLimit failed", t);
        }
    }

    /// [view widthAdjustLimit] — printing width limit.
    public double widthAdjustLimit() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, Sels.widthAdjustLimit);
        } catch (Throwable t) {
            throw new RuntimeException("widthAdjustLimit failed", t);
        }
    }

    /// [view registeredDraggedTypes] — accepted pasteboard types.
    public NSArray registeredDraggedTypes() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.registeredDraggedTypes));
    }

    /// [view enterFullScreenMode:withOptions:] (options may be nil).
    public boolean enterFullScreenMode(NSScreen screen, NSDictionary options) {
        ensureInit();
        try {
            return (boolean) H.hBoolIdId().invokeExact(peer, Sels.enterFullScreenMode_withOptions,
                    screen.peer(), (MemorySegment) (options == null ? MemorySegment.NULL : options.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("enterFullScreenMode:withOptions: failed", t);
        }
    }

    /// [view exitFullScreenModeWithOptions:] (options may be nil).
    public void exitFullScreenModeWithOptions(NSDictionary options) {
        ensureInit();
        try {
            H.hVoidId().invokeExact(peer, Sels.exitFullScreenModeWithOptions, (MemorySegment) (options == null ? MemorySegment.NULL : options.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("exitFullScreenModeWithOptions: failed", t);
        }
    }

    /// [view isInFullScreenMode].
    public boolean isInFullScreenMode() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isInFullScreenMode);
    }

    /// [view addGestureRecognizer:].
    public void addGestureRecognizer(NSGestureRecognizer recognizer) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.addGestureRecognizer, recognizer.peer());
    }

    /// [view removeGestureRecognizer:].
    public void removeGestureRecognizer(NSGestureRecognizer recognizer) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.removeGestureRecognizer, recognizer.peer());
    }

    /// [view gestureRecognizers].
    public NSArray gestureRecognizers() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.gestureRecognizers));
    }

    /// [view addTrackingArea:].
    public void addTrackingArea(NSTrackingArea area) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.addTrackingArea, area.peer());
    }

    /// [view removeTrackingArea:].
    public void removeTrackingArea(NSTrackingArea area) {
        ensureInit();
        ObjC.msgSendVoidId(peer, Sels.removeTrackingArea, area.peer());
    }

    /// [view updateTrackingAreas].
    public void updateTrackingAreas() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.updateTrackingAreas);
    }

    /// [view trackingAreas].
    public NSArray trackingAreas() {
        ensureInit();
        return NSArray.wrap(ObjC.msgSendId(peer, Sels.trackingAreas));
    }

    /// [view addCursorRect:cursor:].
    public void addCursorRect(NSRect rect, NSCursor cursor) {
        ensureInit();
        try {
            H.hCacheDisplay().invokeExact(peer, Sels.addCursorRect_cursor, rect.toSegment(), cursor.peer());
        } catch (Throwable t) {
            throw new RuntimeException("addCursorRect:cursor: failed", t);
        }
    }

    /// [view removeCursorRect:cursor:].
    public void removeCursorRect(NSRect rect, NSCursor cursor) {
        ensureInit();
        try {
            H.hCacheDisplay().invokeExact(peer, Sels.removeCursorRect_cursor, rect.toSegment(), cursor.peer());
        } catch (Throwable t) {
            throw new RuntimeException("removeCursorRect:cursor: failed", t);
        }
    }

    /// [view discardCursorRects].
    public void discardCursorRects() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.discardCursorRects);
    }

    /// [view resetCursorRects].
    public void resetCursorRects() {
        ensureInit();
        ObjC.msgSendVoid(peer, Sels.resetCursorRects);
    }

    /// [view displayLinkWithTarget:selector:] — display link driving the target (shape shares the (ID,ID,ID) descriptor).
    public CADisplayLink displayLink(MemorySegment target, MemorySegment selector) {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) H.hIdIdId().invokeExact(peer, Sels.displayLinkWithTarget_selector, target, selector);
            return CADisplayLink.wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("displayLinkWithTarget:selector: failed", t);
        }
    }

    /// [view userInterfaceLayoutDirection] — NSUserInterfaceLayoutDirection.
    public long userInterfaceLayoutDirection() {
        ensureInit();
        return ObjC.msgSendLong(peer, Sels.userInterfaceLayoutDirection);
    }

    /// [view setUserInterfaceLayoutDirection:].
    public void setUserInterfaceLayoutDirection(long direction) {
        ensureInit();
        ObjC.msgSendVoidLong(peer, Sels.setUserInterfaceLayoutDirection, direction);
    }

    /// [view preparedContentRect].
    public NSRect preparedContentRect() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.preparedContentRect));
    }

    /// [view setPreparedContentRect:].
    public void setPreparedContentRect(NSRect rect) {
        ensureInit();
        try {
            H.hSetBounds().invokeExact(peer, Sels.setPreparedContentRect, rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setPreparedContentRect: failed", t);
        }
    }

    /// [view allowsVibrancy].
    public boolean allowsVibrancy() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.allowsVibrancy);
    }

    /// [view safeAreaInsets] — NSEdgeInsets (32-byte struct return).
    public NSEdgeInsets safeAreaInsets() {
        ensureInit();
        return NSEdgeInsets.fromSegment(ObjC.msgSendEdgeInsets(peer, Sels.safeAreaInsets));
    }

    /// [view additionalSafeAreaInsets].
    public NSEdgeInsets additionalSafeAreaInsets() {
        ensureInit();
        return NSEdgeInsets.fromSegment(ObjC.msgSendEdgeInsets(peer, Sels.additionalSafeAreaInsets));
    }

    /// [view setAdditionalSafeAreaInsets:] — NSEdgeInsets by value.
    public void setAdditionalSafeAreaInsets(NSEdgeInsets insets) {
        ensureInit();
        try {
            ObjC.handle(Sig.of(Ret.VOID, Arg.EDGEINSETS)).invokeExact(peer, Sels.setAdditionalSafeAreaInsets, insets.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setAdditionalSafeAreaInsets: failed", t);
        }
    }

    /// [view safeAreaRect].
    public NSRect safeAreaRect() {
        ensureInit();
        return NSRect.fromSegment(ObjC.msgSendRect(peer, Sels.safeAreaRect));
    }

    /// [view safeAreaLayoutGuide] — NSLayoutGuide (unwrapped; no dedicated wrapper).
    public NSObject safeAreaLayoutGuide() {
        ensureInit();
        return NSObject.wrap(ObjC.msgSendId(peer, Sels.safeAreaLayoutGuide));
    }

    /// [view layoutMarginsGuide] — NSLayoutGuide (unwrapped; no dedicated wrapper).
    public NSObject layoutMarginsGuide() {
        ensureInit();
        return NSObject.wrap(ObjC.msgSendId(peer, Sels.layoutMarginsGuide));
    }

    /// [view prefersCompactControlSizeMetrics] (macOS 26+).
    public boolean prefersCompactControlSizeMetrics() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.prefersCompactControlSizeMetrics);
    }

    /// [view setPrefersCompactControlSizeMetrics:] (macOS 26+).
    public void setPrefersCompactControlSizeMetrics(boolean flag) {
        ensureInit();
        ObjC.msgSendVoidBool(peer, Sels.setPrefersCompactControlSizeMetrics, flag);
    }

    /// [view isDrawingFindIndicator].
    public boolean isDrawingFindIndicator() {
        ensureInit();
        return ObjC.msgSendBool(peer, Sels.isDrawingFindIndicator);
    }


}
