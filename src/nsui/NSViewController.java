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

    private static volatile boolean initialized;
    private static MethodHandle hSetView; // (id, SEL, id) -> void
    private static MethodHandle hGetView; // (id, SEL) -> id

    protected NSViewController(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hSetView = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hGetView = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
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
            MemorySegment v = (MemorySegment) hGetView.invokeExact(peer, ObjC.sel("view"));
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
            hSetView.invokeExact(peer, ObjC.sel("setView:"), p);
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
}
