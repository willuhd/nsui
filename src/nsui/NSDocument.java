package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSDocument — minimal wrap over AppKit NSDocument.
 * Thin 1:1, stateless. Only init, windowControllers, addWindowController.
 */
public class NSDocument extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hId;       // (id, SEL) -> id
    private static MethodHandle hVoidId;   // (id, SEL, id) -> void

    protected NSDocument(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSDocument wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSDocument(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hId = ObjC.handle(Sig.of(Ret.ID));
        hVoidId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /** alloc + init — new document. */
    public static NSDocument create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSDocument"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p == null || p.address() == 0) throw new IllegalStateException("NSDocument alloc/init returned nil");
        return new NSDocument(p);
    }

    /** init — instance initializer (also via create). */
    public NSDocument init() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(peer, ObjC.sel("init"));
        if (p == null || p.address() == 0) throw new IllegalStateException("NSDocument init returned nil");
        return new NSDocument(p);
    }

    /** windowControllers — NSArray of NSWindowController. */
    public NSArray windowControllers() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) hId.invokeExact(peer, ObjC.sel("windowControllers"));
            return NSArray.wrap(arr);
        } catch (Throwable t) {
            throw new RuntimeException("windowControllers failed", t);
        }
    }

    /** addWindowController: */
    public void addWindowController(NSWindowController controller) {
        ensureInit();
        try {
            hVoidId.invokeExact(peer, ObjC.sel("addWindowController:"), (MemorySegment) (controller == null ? MemorySegment.NULL : controller.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("addWindowController: failed", t);
        }
    }

    /** removeWindowController: */
    public void removeWindowController(NSWindowController controller) {
        ensureInit();
        try {
            hVoidId.invokeExact(peer, ObjC.sel("removeWindowController:"), (MemorySegment) (controller == null ? MemorySegment.NULL : controller.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("removeWindowController: failed", t);
        }
    }

    /** displayName — NSString. */
    public String displayName() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("displayName"));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("displayName failed", t);
        }
    }
}
