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

/// NSView — a drawable view. Extends NSObject; the drawing is handed off to a
/// Java `Drawable` via a runtime-created ObjC subclass of NSView whose
/// `drawRect:` is an FFM upcall stub into Java.
///
/// Dispatch is keyed by the native peer address: `create` registers the
/// `Drawable`, and the upcall target looks it up when AppKit asks the view
/// to draw on the main thread. The subclass also overrides `dealloc` to
/// unregister the drawable, so the registry can never grow stale or leak.
///
/// Upcall targets (`drawRectImpl`, `deallocImpl`) are static and
/// capture-free — they are registered for native-image in NsuiFeature.
public class NSView extends NSObject {

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
        void draw(MemorySegment ctx, NSRect dirtyRect);
    }

    /// Drawable registry, keyed by peer address (the view's id).
    private static final ConcurrentHashMap<Long, Drawable> DRAWABLES = new ConcurrentHashMap<>();

    // ---- runtime ObjC class + upcall stubs, created ONCE lazily (NEVER in a static initializer) ----
    private static MemorySegment drawableClass;
    private static MemorySegment drawRectStub;
    private static MemorySegment deallocStub;

    // ---- resolved once per process (rule: resolve-once, invokeExact on hot paths) ----
    private record Handles(MethodHandle hInitFrame, MethodHandle hSetFrame, MethodHandle hNeedsRect, MethodHandle hAutoMask, MethodHandle hBacking, MethodHandle hConvBacking, MethodHandle hGetDouble, MethodHandle hSetDouble, MethodHandle hGetSize, MethodHandle hSetSize, MethodHandle hObjectAtIndex, MethodHandle hSetBounds, MethodHandle hRegisterForDraggedTypes, MethodHandle hBeginDraggingSession) {}
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
        MemorySegment v = ObjC.msgSendId(drawableClass, ObjC.sel("alloc"));
        try {
            v = (MemorySegment) H.hInitFrame().invokeExact(v, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed", t);
        }
        NSView view = new NSView(v);
        DRAWABLES.put(v.address(), drawable);
        return view;
    }

    private static synchronized void ensureInit() {
        if (H != null) return;
        drawableClass = ObjC.makeClass("NSView", "NSUIViewImpl");
        try {
            MethodHandle drawTarget = MethodHandles.lookup().findStatic(NSView.class, "drawRectImpl",
                    MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
            drawRectStub = ObjC.upcall(drawTarget, NsuiForeign.drawRectUpcall());
            MethodHandle deallocTarget = MethodHandles.lookup().findStatic(NSView.class, "deallocImpl",
                    MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class));
            deallocStub = ObjC.upcall(deallocTarget, NsuiForeign.deallocUpcall());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("cannot bind NSView upcall targets", e);
        }
        if (!ObjC.addMethod(drawableClass, "drawRect:", drawRectStub, "v@:{CGRect={CGPoint=dd}{CGSize=dd}}")) {
            throw new RuntimeException("class_addMethod drawRect: failed");
        }
        if (!ObjC.addMethod(drawableClass, "dealloc", deallocStub, "v@:")) {
            throw new RuntimeException("class_addMethod dealloc failed");
        }
        H = new Handles(
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
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID)));
    }

    /// FFM upcall target: `-(void)drawRect:(NSRect)dirtyRect` — called by AppKit on the main thread.
    public static void drawRectImpl(MemorySegment self, MemorySegment sel, MemorySegment rect) {
        Drawable d = DRAWABLES.get(self.address());
        if (d == null) return;
        MemorySegment ctx = ObjC.msgSendId(ObjC.cls("NSGraphicsContext"), ObjC.sel("currentContext"));
        ctx = ObjC.msgSendId(ctx, ObjC.sel("graphicsPort"));
        // Every draw call's input marshalling (rects, cstrings) goes through the
        // per-turn scratch arena — recycled per draw pass instead of immortal.
        Scratch.beginTurn();
        try {
            d.draw(ctx, NSRect.fromSegment(rect));
        } finally {
            Scratch.endTurn();
        }
    }

    /// FFM upcall target: `-(void)dealloc` — unregister the drawable, then chain
    /// to `[super dealloc]` via objc_msgSendSuper so the native object is released.
    /// Public because NsuiFeature (nsui.objc) resolves it at build time.
    public static void deallocImpl(MemorySegment self, MemorySegment sel) {
        DRAWABLES.remove(self.address());
        MemorySegment superStruct = ObjC.superStruct(self, ObjC.classGetSuperclass(drawableClass));
        ObjC.msgSendSuperVoid(superStruct, sel);
    }

    /// Number of live drawables (diagnostics/tests: must return to 0 after views are released).
    public static int drawableCount() {
        return DRAWABLES.size();
    }

    // ---------------------------------------------------------------- instance API

    /// addSubview: — attach a child view.
    public void addSubview(NSView subview) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addSubview:"), subview.peer());
    }

    /// setFrame: — reposition/resize in the superview's coordinates.
    public void setFrame(NSRect frame) {
        ensureInit();
        try {
            H.hSetFrame().invokeExact(peer, ObjC.sel("setFrame:"), frame.toSegment());
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
            H.hAutoMask().invokeExact(peer, ObjC.sel("setAutoresizingMask:"), mask);
        } catch (Throwable t) {
            throw new RuntimeException("setAutoresizingMask: failed", t);
        }
    }


    /// bounds — the view's own coordinate system (origin usually {0,0}).
    public NSRect bounds() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("bounds")));
    }

    /// setBounds: — set the view's bounds.
    public void setBounds(NSRect bounds) {
        ensureInit();
        try {
            H.hSetBounds().invokeExact(peer, ObjC.sel("setBounds:"), bounds.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setBounds: failed", t);
        }
    }

    /// frame — the view's frame in its superview's coordinates (struct return).
    public NSRect frame() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("frame")));
    }

    /// needsDisplay — whether the view needs display.
    public boolean needsDisplay() {
        return ObjC.msgSendBool(peer, ObjC.sel("needsDisplay"));
    }

    /// setNeedsDisplay: — request a redraw on the next run-loop pass.
    public void setNeedsDisplay(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setNeedsDisplay:"), flag);
    }

    /// setNeedsDisplayInRect: — mark only the given region, in the view's own coordinate
    /// system, as needing redraw. AppKit unions repeated invalidations and passes the
    /// resulting (possibly expanded) rect to `drawRect:`; drawing may be clipped to it.
    /// This is the cost-saving entry point for dirty-rect rendering: a view that only repaints
    /// `rect` avoids a full-bounds redraw.
    public void setNeedsDisplayInRect(NSRect rect) {
        ensureInit();
        try {
            H.hNeedsRect().invokeExact(peer, ObjC.sel("setNeedsDisplayInRect:"), rect.toSegment());
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
            return (double) H.hBacking().invokeExact(peer, ObjC.sel("backingScaleFactor"));
        } catch (Throwable t) {
            throw new RuntimeException("backingScaleFactor failed", t);
        }
    }

    /// convertRectToBacking: — a point-space rect in the view's coordinates -> backing pixels.
    public NSRect convertRectToBacking(NSRect rect) {
        ensureInit();
        try {
            return NSRect.fromSegment((MemorySegment) H.hConvBacking().invokeExact(
                    (java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer,
                    ObjC.sel("convertRectToBacking:"), rect.toSegment()));
        } catch (Throwable t) {
            throw new RuntimeException("convertRectToBacking: failed", t);
        }
    }

    /// window — the NSWindow this view is installed in (null if none).
    public NSObject window() {
        return NSObject.wrap(ObjC.msgSendId(peer, ObjC.sel("window")));
    }

    /// setWantsLayer: — opt into layer-backed drawing.
    public void setWantsLayer(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setWantsLayer:"), flag);
    }

    /// wantsLayer — whether the view is layer-backed.
    public boolean wantsLayer() {
        return ObjC.msgSendBool(peer, ObjC.sel("wantsLayer"));
    }

    /// isFlipped — whether the view's y-axis points up (NO for a plain NSView).
    public boolean isFlipped() {
        return ObjC.msgSendBool(peer, ObjC.sel("isFlipped"));
    }

    /// layer — the view's backing CALayer (null if not layer-backed). Typed via CALayer.
    public CALayer layer() {
        return CALayer.wrap(ObjC.msgSendId(peer, ObjC.sel("layer")));
    }

    /// setLayer: — assign a CALayer
    public void setLayer(CALayer layer) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setLayer:"), (MemorySegment) (layer == null ? MemorySegment.NULL : layer.peer()));
    }

    // ---------------------------------------------------------------- additional getters — completeness

    /// autoresizingMask — NSAutoresizingMaskOptions.
    public long autoresizingMask() {
        return ObjC.msgSendLong(peer, ObjC.sel("autoresizingMask"));
    }

    /// autoresizesSubviews.
    public boolean autoresizesSubviews() {
        return ObjC.msgSendBool(peer, ObjC.sel("autoresizesSubviews"));
    }

    /// setAutoresizesSubviews:.
    public void setAutoresizesSubviews(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAutoresizesSubviews:"), flag);
    }

    /// translatesAutoresizingMaskIntoConstraints.
    public boolean translatesAutoresizingMaskIntoConstraints() {
        return ObjC.msgSendBool(peer, ObjC.sel("translatesAutoresizingMaskIntoConstraints"));
    }

    /// setTranslatesAutoresizingMaskIntoConstraints:.
    public void setTranslatesAutoresizingMaskIntoConstraints(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setTranslatesAutoresizingMaskIntoConstraints:"), flag);
    }

    // ---------------------------------------------------------------- Auto Layout — anchors & constraints

    /// leadingAnchor — NSLayoutXAxisAnchor.
    public NSLayoutAnchor leadingAnchor() {
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, ObjC.sel("leadingAnchor")));
    }

    /// trailingAnchor — NSLayoutXAxisAnchor.
    public NSLayoutAnchor trailingAnchor() {
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, ObjC.sel("trailingAnchor")));
    }

    /// topAnchor — NSLayoutYAxisAnchor.
    public NSLayoutAnchor topAnchor() {
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, ObjC.sel("topAnchor")));
    }

    /// bottomAnchor — NSLayoutYAxisAnchor.
    public NSLayoutAnchor bottomAnchor() {
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, ObjC.sel("bottomAnchor")));
    }

    /// widthAnchor — NSLayoutDimension.
    public NSLayoutAnchor widthAnchor() {
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, ObjC.sel("widthAnchor")));
    }

    /// heightAnchor — NSLayoutDimension.
    public NSLayoutAnchor heightAnchor() {
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, ObjC.sel("heightAnchor")));
    }

    /// centerXAnchor — NSLayoutXAxisAnchor.
    public NSLayoutAnchor centerXAnchor() {
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, ObjC.sel("centerXAnchor")));
    }

    /// centerYAnchor — NSLayoutYAxisAnchor.
    public NSLayoutAnchor centerYAnchor() {
        return NSLayoutAnchor.wrap(ObjC.msgSendId(peer, ObjC.sel("centerYAnchor")));
    }

    /// addConstraint: — install a single layout constraint on this view.
    public void addConstraint(NSLayoutConstraint constraint) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addConstraint:"), constraint.peer());
    }

    /// addConstraints: — install multiple constraints (loops over addConstraint: for simplicity).
    public void addConstraints(java.util.List<NSLayoutConstraint> constraints) {
        for (NSLayoutConstraint c : constraints) addConstraint(c);
    }

    /// removeConstraint: — remove a previously-added constraint.
    public void removeConstraint(NSLayoutConstraint constraint) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeConstraint:"), constraint.peer());
    }

    /// removeConstraints: — bulk remove.
    public void removeConstraints(java.util.List<NSLayoutConstraint> constraints) {
        for (NSLayoutConstraint c : constraints) removeConstraint(c);
    }

    /// constraints — the view's installed constraints.
    public java.util.List<NSLayoutConstraint> constraints() {
        ensureInit();
        MemorySegment arr = ObjC.msgSendId(peer, ObjC.sel("constraints"));
        if (arr == null || arr.address() == 0) return java.util.List.of();
        long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
        java.util.List<NSLayoutConstraint> list = new java.util.ArrayList<>((int) count);
        MemorySegment selAt = ObjC.sel("objectAtIndex:");
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
        ObjC.msgSendVoid(peer, ObjC.sel("displayIfNeeded"));
    }

    /// displayIfNeededInRect: — display if needed in rect.
    public void displayIfNeededInRect(NSRect rect) {
        ensureInit();
        try {
            H.hNeedsRect().invokeExact(peer, ObjC.sel("displayIfNeededInRect:"), rect.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("displayIfNeededInRect: failed", t);
        }
    }

    /// layoutSubtreeIfNeeded — layout the subtree if needed.
    public void layoutSubtreeIfNeeded() {
        ObjC.msgSendVoid(peer, ObjC.sel("layoutSubtreeIfNeeded"));
    }

    /// intrinsicContentSize — the view's intrinsic content size.
    public NSSize intrinsicContentSize() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) H.hGetSize().invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("intrinsicContentSize"));
            return NSSize.fromSegment(s);
        } catch (Throwable t) {
            throw new RuntimeException("intrinsicContentSize failed", t);
        }
    }

    /// alphaValue — view alpha 0..1.
    public double alphaValue() {
        ensureInit();
        try {
            return (double) H.hGetDouble().invokeExact(peer, ObjC.sel("alphaValue"));
        } catch (Throwable t) {
            throw new RuntimeException("alphaValue failed", t);
        }
    }

    /// setAlphaValue:.
    public void setAlphaValue(double alpha) {
        ensureInit();
        try {
            H.hSetDouble().invokeExact(peer, ObjC.sel("setAlphaValue:"), alpha);
        } catch (Throwable t) {
            throw new RuntimeException("setAlphaValue: failed", t);
        }
    }

    /// isHidden.
    public boolean isHidden() {
        return ObjC.msgSendBool(peer, ObjC.sel("isHidden"));
    }

    /// setHidden:.
    public void setHidden(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setHidden:"), flag);
    }

    /// isHiddenOrHasHiddenAncestor.
    public boolean isHiddenOrHasHiddenAncestor() {
        return ObjC.msgSendBool(peer, ObjC.sel("isHiddenOrHasHiddenAncestor"));
    }

    /// superview — parent view.
    public NSView superview() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("superview"));
        return NSView.wrap(v);
    }

    /// isOpaque — whether the view is opaque.
    public boolean isOpaque() {
        return ObjC.msgSendBool(peer, ObjC.sel("isOpaque"));
    }

    /// visibleRect — the visible rect (readonly).
    public NSRect visibleRect() {
        return NSRect.fromSegment(ObjC.msgSendRect(peer, ObjC.sel("visibleRect")));
    }

    /// isRotatedFromBase
    public boolean isRotatedFromBase() {
        return ObjC.msgSendBool(peer, ObjC.sel("isRotatedFromBase"));
    }

    /// isRotatedOrScaledFromBase
    public boolean isRotatedOrScaledFromBase() {
        return ObjC.msgSendBool(peer, ObjC.sel("isRotatedOrScaledFromBase"));
    }

    /// canBecomeKeyView
    public boolean canBecomeKeyView() {
        return ObjC.msgSendBool(peer, ObjC.sel("canBecomeKeyView"));
    }

    /// enclosingScrollView
    public NSView enclosingScrollView() {
        MemorySegment v = ObjC.msgSendId(peer, ObjC.sel("enclosingScrollView"));
        return NSView.wrap(v);
    }

    /// invalidateIntrinsicContentSize.
    public void invalidateIntrinsicContentSize() {
        ObjC.msgSendVoid(peer, ObjC.sel("invalidateIntrinsicContentSize"));
    }

    // ---- dragging support (Phase 0B) ----

    /// registerForDraggedTypes: — register pasteboard types this view accepts for drops.
    public void registerForDraggedTypes(java.util.List<String> types) {
        ensureInit();
        MemorySegment arr;
        if (types == null || types.isEmpty()) {
            arr = ObjC.msgSendId(ObjC.cls("NSArray"), ObjC.sel("array"));
        } else {
            arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
            for (String t : types) {
                if (t == null) continue;
                MemorySegment ns = ObjC.nsstring(t);
                ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ns);
            }
        }
        try {
            H.hRegisterForDraggedTypes().invokeExact(peer, ObjC.sel("registerForDraggedTypes:"), arr);
        } catch (Throwable t) {
            throw new RuntimeException("registerForDraggedTypes: failed", t);
        }
    }

    /// unregisterDraggedTypes — unregister all previously registered drag types.
    public void unregisterDraggedTypes() {
        ObjC.msgSendVoid(peer, ObjC.sel("unregisterDraggedTypes"));
    }

    /// beginDraggingSessionWithItems:event:source: — begin a dragging session.
    /// Best-effort: converts items to NSArray, handles nulls gracefully, wraps result.
    /// If event or source is null/NULL, AppKit would dereference and SIGSEGV; we return null gracefully instead.
    public NSDraggingSession beginDraggingSessionWithItems(java.util.List<NSDraggingItem> items, NSEvent event, NSDraggingSource source) {
        ensureInit();
        MemorySegment arr;
        if (items == null || items.isEmpty()) {
            arr = ObjC.msgSendId(ObjC.cls("NSArray"), ObjC.sel("array"));
        } else {
            arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
            for (NSDraggingItem item : items) {
                if (item == null || item.peer() == null || item.peer().address() == 0) continue;
                ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), item.peer());
            }
        }
        MemorySegment eventSeg = (event == null || event.peer() == null || event.peer().address() == 0) ? MemorySegment.NULL : event.peer();
        MemorySegment sourceSeg = (source == null || source.peer() == null || source.peer().address() == 0) ? MemorySegment.NULL : source.peer();
        // Graceful best-effort: AppKit crashes (SIGSEGV) if event is NULL; return null instead.
        if (eventSeg.address() == 0 || sourceSeg.address() == 0) {
            return null;
        }
        try {
            MemorySegment sess = (MemorySegment) H.hBeginDraggingSession().invokeExact(peer, ObjC.sel("beginDraggingSessionWithItems:event:source:"), arr, eventSeg, sourceSeg);
            return NSDraggingSession.wrap(sess);
        } catch (Throwable t) {
            // Also graceful: if AppKit raises (e.g. view not in window), return null rather than throw
            // But preserve original exception for debugging if it's a vocabulary miss
            if (t.getMessage() != null && t.getMessage().contains("vocabulary")) throw new RuntimeException("beginDraggingSessionWithItems:event:source: failed", t);
            return null;
        }
    }
}
