package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSStatusBar — the system menu bar. Thin 1:1 wrapper.
public final class NSStatusBar extends NSObject {

            private record Handles(MethodHandle hId, MethodHandle hWithLength, MethodHandle hThickness, MethodHandle hIsVertical) {}
    private static volatile Handles handles;

    private NSStatusBar(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.BOOL))
        );
    }

    public static NSStatusBar wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSStatusBar(peer);
    }

    /// +[NSStatusBar systemStatusBar]
    public static NSStatusBar systemStatusBar() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hId().invokeExact(ObjC.cls("NSStatusBar"), ObjC.sel("systemStatusBar"));
            return wrap(p);
        } catch (Throwable t) { throw new RuntimeException("systemStatusBar failed", t); }
    }

    /// -[NSStatusBar statusItemWithLength:] -> NSStatusItem
    public NSStatusItem statusItemWithLength(double length) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) handles.hWithLength().invokeExact(peer, ObjC.sel("statusItemWithLength:"), length);
            return NSStatusItem.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("statusItemWithLength: failed", t); }
    }

    /// Convenience using variable length (-1).
    public NSStatusItem statusItem() {
        return statusItemWithLength(-1.0);
    }

    /// Convenience: statusItem with SF Symbol image (uses NSImage.imageWithSystemSymbolName).
    public NSStatusItem statusItemWithSFSymbol(String symbolName) {
        NSStatusItem item = statusItemWithLength(VARIABLE_LENGTH);
        if (symbolName != null) {
            NSImage img = NSImage.imageWithSystemSymbolName(symbolName);
            if (img == null) img = NSImage.imageNamed(symbolName);
            if (img != null) {
                try { img.setTemplate(true); } catch (Throwable ignored) {}
                item.setImage(img);
            }
        }
        return item;
    }

    /// Convenience: statusItem with SF Symbol and length.
    public NSStatusItem statusItemWithSFSymbol(String symbolName, double length) {
        NSStatusItem item = statusItemWithLength(length);
        if (symbolName != null) {
            NSImage img = NSImage.imageWithSystemSymbolName(symbolName);
            if (img == null) img = NSImage.imageNamed(symbolName);
            if (img != null) {
                try { img.setTemplate(true); } catch (Throwable ignored) {}
                item.setImage(img);
            }
        }
        return item;
    }

    public void removeStatusItem(NSStatusItem item) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeStatusItem:"), (MemorySegment) (item == null ? MemorySegment.NULL : item.peer()));
    }

    public double thickness() {
        ensureInit();
        try { return (double) handles.hThickness().invokeExact(peer, ObjC.sel("thickness")); } catch (Throwable t) { throw new RuntimeException("thickness failed", t); }
    }

    public boolean isVertical() {
        ensureInit();
        try { return (boolean) handles.hIsVertical().invokeExact(peer, ObjC.sel("isVertical")); } catch (Throwable t) { throw new RuntimeException("isVertical failed", t); }
    }

    // ---- length constants ----
    public static final double VARIABLE_LENGTH = -1.0;
    public static final double SQUARE_LENGTH = -2.0;
}
