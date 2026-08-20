package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSSearchMenuTemplate — helper for Help-menu / search-field menu templates.
 * Thin wrapper over NSMenu suitable as a searchMenuTemplate.
 * Also provides convenience to install as help-menu search template.
 */
public final class NSSearchMenuTemplate extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hId;       // (id, SEL) -> id
    private static MethodHandle hVoidId;   // (id, SEL, id) -> void

    private NSSearchMenuTemplate(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSSearchMenuTemplate wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSearchMenuTemplate(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hId = ObjC.handle(Sig.of(Ret.ID));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /** alloc + init — empty menu template. */
    public static NSSearchMenuTemplate create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSMenu"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p == null || p.address() == 0) throw new IllegalStateException("NSMenu alloc/init returned nil for NSSearchMenuTemplate");
        return new NSSearchMenuTemplate(p);
    }

    public static NSSearchMenuTemplate createWithTitle(String title) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSMenu"), ObjC.sel("alloc"));
        MemorySegment n = ObjC.msgSendIdId(p, ObjC.sel("initWithTitle:"), ObjC.nsstring(title));
        if (n == null || n.address() == 0) throw new IllegalStateException("NSMenu initWithTitle: returned nil");
        return new NSSearchMenuTemplate(n);
    }

    /** Underlying NSMenu peer as NSMenu wrapper. */
    public NSMenu asMenu() {
        return NSMenu.wrap(peer);
    }

    /** addItem: */
    public void addItem(NSMenuItem item) {
        ensureInit();
        try { hVoidId.invokeExact(peer, ObjC.sel("addItem:"), item.peer()); }
        catch (Throwable t) { throw new RuntimeException("addItem: failed", t); }
    }

    /** numberOfItems */
    public long numberOfItems() {
        return ObjC.msgSendLong(peer, ObjC.sel("numberOfItems"));
    }

    /** title */
    public String title() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("title"));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("title failed", t); }
    }

    public void setTitle(String t) {
        ensureInit();
        try { hVoidId.invokeExact(peer, ObjC.sel("setTitle:"), ObjC.nsstring(t)); }
        catch (Throwable e) { throw new RuntimeException("setTitle: failed", e); }
    }

    /** Install this template onto a search field. */
    public void installOn(NSSearchField field) {
        if (field != null) field.setSearchMenuTemplate(asMenu());
    }

    /** Convenience: set as the template for given search field and return self. */
    public NSSearchMenuTemplate attachTo(NSSearchField field) {
        installOn(field);
        return this;
    }
}
