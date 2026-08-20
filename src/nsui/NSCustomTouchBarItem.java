package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSCustomTouchBarItem — a view-backed item for an NSTouchBar.
/// Thin 1:1, stateless wrapper over AppKit `NSCustomTouchBarItem`.
/// Each method maps to one `objc_msgSend`.
/// Follows the project template: volatile initialized, synchronized ensureInit,
/// ObjC.handle(Sig.of...), invokeExact, static create/wrap.
///
/// Supports full Touch Bar demo: color swatches (NSColorWell or NSButton
/// with backgroundColor/bezelColor), icon buttons (NSBoldTemplate,
/// NSItalicTemplate via NSImage.imageNamed), and arbitrary custom NSView
/// (layer-backed) — all installed via `setView:` and returned from
/// `touchBar:makeItemForIdentifier:` delegate.
public class NSCustomTouchBarItem extends NSTouchBarItem {

    private static volatile boolean initialized;
    private static MethodHandle hInitId; // (id, SEL, id) -> id   [initWithIdentifier:]
    private static MethodHandle hId;     // (id, SEL) -> id
    private static MethodHandle hVoidId; // (id, SEL, id) -> void

    protected NSCustomTouchBarItem(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSCustomTouchBarItem wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSCustomTouchBarItem(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitId = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hId = ObjC.handle(Sig.of(Ret.ID));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /// alloc + initWithIdentifier: — create custom item with identifier.
    public static NSCustomTouchBarItem create(String identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSCustomTouchBarItem"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitId.invokeExact(p, ObjC.sel("initWithIdentifier:"), ObjC.nsstring(identifier));
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSCustomTouchBarItem", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSCustomTouchBarItem alloc/initWithIdentifier: returned nil");
        return new NSCustomTouchBarItem(p);
    }

    /// Raw peer variant: initWithIdentifier: with id.
    public static NSCustomTouchBarItem create(MemorySegment identifier) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSCustomTouchBarItem"), ObjC.sel("alloc"));
        try {
            MemorySegment arg = (identifier == null || identifier.address() == 0) ? MemorySegment.NULL : identifier;
            p = (MemorySegment) hInitId.invokeExact(p, ObjC.sel("initWithIdentifier:"), arg);
        } catch (Throwable t) {
            throw new RuntimeException("initWithIdentifier: failed for NSCustomTouchBarItem", t);
        }
        if (p == null || p.address() == 0) throw new IllegalStateException("NSCustomTouchBarItem alloc/initWithIdentifier: returned nil");
        return new NSCustomTouchBarItem(p);
    }

    /// view — NSView peer or null (guarded via respondsToSelector:).
    @Override
    public NSView view() {
        ensureInit();
        try {
            MemorySegment sel = ObjC.sel("view");
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return null;
            MemorySegment v = (MemorySegment) hId.invokeExact(peer, sel);
            return NSView.wrap(v);
        } catch (Throwable t) { return null; }
    }

    public void setView(NSView view) {
        ensureInit();
        try {
            MemorySegment sel = ObjC.sel("setView:");
            MethodHandle hResp = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            boolean resp = (boolean) hResp.invokeExact(peer, ObjC.sel("respondsToSelector:"), sel);
            if (!resp) return;
            hVoidId.invokeExact(peer, sel, (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
        } catch (Throwable t) { /* no-op if absent */ }
    }

    /// customizationLabel — NSString or null.
    public String customizationLabel() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("customizationLabel"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("customizationLabel failed", t);
        }
    }

    /// setCustomizationLabel: — NSString.
    public void setCustomizationLabel(String label) {
        ensureInit();
        try {
            MemorySegment s = label == null ? MemorySegment.NULL : ObjC.nsstring(label);
            hVoidId.invokeExact(peer, ObjC.sel("setCustomizationLabel:"), s);
        } catch (Throwable t) {
            throw new RuntimeException("setCustomizationLabel: failed", t);
        }
    }

    /// setCustomizationLabel: raw id variant.
    public void setCustomizationLabel(MemorySegment label) {
        ensureInit();
        try {
            MemorySegment s = (label == null || label.address() == 0) ? MemorySegment.NULL : label;
            hVoidId.invokeExact(peer, ObjC.sel("setCustomizationLabel:"), s);
        } catch (Throwable t) {
            throw new RuntimeException("setCustomizationLabel: failed", t);
        }
    }
}
