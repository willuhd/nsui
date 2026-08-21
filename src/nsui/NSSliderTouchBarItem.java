package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSliderTouchBarItem — a Touch Bar item that hosts an `NSSlider`
/// (optionally with a label and value accessories).
/// Thin 1:1, stateless wrapper over AppKit `NSSliderTouchBarItem`
/// (macOS 10.12.2+): every method maps to one `objc_msgSend`; optional
/// selectors are guarded with `respondsToSelector:` like
/// `NSCustomTouchBarItem`.
///
/// Slider plumbing: AppKit auto-creates the slider; install a custom one
/// (e.g. `NSSlider.create(new NSRect(...))`) via `setSlider`. The item itself
/// declares only `doubleValue` — `minValue`/`maxValue` live on the slider, so
/// the `minValue()`/`maxValue()` accessors here forward through the wrapped
/// slider peer, exactly as the header recommends ("doubleValue, minValue,
/// maxValue, etc can all be read and set through the slider").
public class NSSliderTouchBarItem extends NSTouchBarItem {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private record Handles(MethodHandle hInitId, MethodHandle hId, MethodHandle hVoidId,
                           MethodHandle hDouble, MethodHandle hVoidDouble) {}
    private static volatile Handles handles;

    protected NSSliderTouchBarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSSliderTouchBarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSliderTouchBarItem(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE))
        );
    }

    /// alloc + initWithIdentifier: — slider item with an auto-created slider.
    public static NSSliderTouchBarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSSliderTouchBarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitId().invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSSliderTouchBarItem", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSSliderTouchBarItem alloc/initWithIdentifier: returned nil");
        return new NSSliderTouchBarItem(p);
    }

    /// Convenience factory wiring target/action right after creation.
    /// Implemented as alloc + `initWithIdentifier:` followed by
    /// setTarget:/setAction: because this AppKit runtime exposes NO
    /// `+sliderTouchBarItemWithIdentifier:target:action:` class factory
    /// (sending it raises an unrecognized-selector exception).
    /// `target` and `actionSelector` may be null.
    public static NSSliderTouchBarItem create(String identifier, MemorySegment target, String actionSelector) {
        NSSliderTouchBarItem item = create(identifier);
        if (target != null) item.setTarget(target);
        if (actionSelector != null) item.setAction(actionSelector);
        return item;
    }

    /// True when the peer implements the given selector.
    private boolean responds(String selectorName) {
        try {
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel(selectorName));
        } catch (Throwable t) {
            return false;
        }
    }

    /// slider — raw peer of the `NSSlider` displayed by this item (non-nil in
    /// practice: AppKit creates it automatically), or null when absent.
    /// Returned raw because `NSSlider` exposes no wrap factory in this slice;
    /// use `setSlider(NSSlider.create(...))` to install a custom slider and
    /// the minValue/maxValue/doubleValue accessors below to drive it.
    public MemorySegment slider() {
        ensureInit();
        if (!responds("slider")) return null;
        try {
            MemorySegment s = (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("slider"));
            return (s == null || s.address() == 0) ? null : s;
        } catch (Throwable t) {
            throw new RuntimeException("slider failed", t);
        }
    }

    /// setSlider: — replace the hosted slider (strong property; passing null
    /// is a no-op since the property is declared nonnull).
    public void setSlider(NSSlider slider) {
        if (slider == null) return;
        ensureInit();
        if (!responds("setSlider:")) return;
        try {
            handles.hVoidId().invokeExact(peer, ObjC.sel("setSlider:"), slider.peer());
        } catch (Throwable t) {
            throw new RuntimeException("setSlider: failed", t);
        }
    }

    /// minValue — forwarded to the wrapped slider (`[item.slider minValue]`;
    /// the item does not declare minValue itself). Returns 0.0 when no slider.
    public double minValue() {
        MemorySegment s = slider();
        if (s == null) return 0.0;
        try {
            return (double) handles.hDouble().invokeExact(s, ObjC.sel("minValue"));
        } catch (Throwable t) {
            throw new RuntimeException("minValue failed", t);
        }
    }

    /// setMinValue: — forwarded to the wrapped slider; no-op when absent.
    public void setMinValue(double v) {
        MemorySegment s = slider();
        if (s == null) return;
        try {
            handles.hVoidDouble().invokeExact(s, ObjC.sel("setMinValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setMinValue: failed", t);
        }
    }

    /// maxValue — forwarded to the wrapped slider. Returns 0.0 when no slider.
    public double maxValue() {
        MemorySegment s = slider();
        if (s == null) return 0.0;
        try {
            return (double) handles.hDouble().invokeExact(s, ObjC.sel("maxValue"));
        } catch (Throwable t) {
            throw new RuntimeException("maxValue failed", t);
        }
    }

    /// setMaxValue: — forwarded to the wrapped slider; no-op when absent.
    public void setMaxValue(double v) {
        MemorySegment s = slider();
        if (s == null) return;
        try {
            handles.hVoidDouble().invokeExact(s, ObjC.sel("setMaxValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setMaxValue: failed", t);
        }
    }

    /// doubleValue — the item's own current-value property (macOS 10.15+,
    /// guarded). Unlike min/max this lives on the item, not the slider.
    public double doubleValue() {
        ensureInit();
        if (!responds("doubleValue")) return 0.0;
        try {
            return (double) handles.hDouble().invokeExact(peer, ObjC.sel("doubleValue"));
        } catch (Throwable t) {
            throw new RuntimeException("doubleValue failed", t);
        }
    }

    /// setDoubleValue: — the item's own current-value property (macOS 10.15+,
    /// guarded).
    public void setDoubleValue(double v) {
        ensureInit();
        if (!responds("setDoubleValue:")) return;
        try {
            handles.hVoidDouble().invokeExact(peer, ObjC.sel("setDoubleValue:"), v);
        } catch (Throwable t) {
            throw new RuntimeException("setDoubleValue: failed", t);
        }
    }

    /// setTarget: — object notified when the slider or accessories receive
    /// user interaction (weak property; guarded).
    public void setTarget(MemorySegment target) {
        ensureInit();
        if (!responds("setTarget:")) return;
        MemorySegment t = (MemorySegment) (target == null ? MemorySegment.NULL : target);
        ObjC.msgSendVoidId(peer, ObjC.sel("setTarget:"), t);
    }

    /// setTarget: typed overload.
    public void setTarget(NSObject target) {
        setTarget(target == null ? null : target.peer());
    }

    /// target — the interaction target (raw id), or null.
    public MemorySegment target() {
        ensureInit();
        if (!responds("target")) return null;
        return ObjC.msgSendId(peer, ObjC.sel("target"));
    }

    /// setAction: — selector sent to the target on interaction (guarded).
    public void setAction(String actionSelector) {
        ensureInit();
        if (actionSelector == null || !responds("setAction:")) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"), ObjC.sel(actionSelector));
    }

    /// action — the action selector (raw SEL), or null.
    public MemorySegment action() {
        ensureInit();
        if (!responds("action")) return null;
        return ObjC.msgSendId(peer, ObjC.sel("action"));
    }
}
