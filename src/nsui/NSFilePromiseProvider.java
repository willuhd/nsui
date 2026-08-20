package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSFilePromiseProvider — minimal wrapper over native {@code NSFilePromiseProvider}.
 * Used for dragging file promises (e.g., drag-out).
 */
public final class NSFilePromiseProvider extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hFileType;   // (id, SEL) -> id
    private static MethodHandle hDelegate;   // (id, SEL) -> id

    private NSFilePromiseProvider(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSFilePromiseProvider wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFilePromiseProvider(peer);
    }

    /** [[NSFilePromiseProvider alloc] initWithFileType:delegate:] */
    public static NSFilePromiseProvider create(String fileType, NSObject delegate) {
        ensureInit();
        try {
            MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSFilePromiseProvider"), ObjC.sel("alloc"));
            MethodHandle hInit = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment ft = fileType == null ? MemorySegment.NULL : ObjC.nsstring(fileType);
            MemorySegment del = delegate == null ? MemorySegment.NULL : delegate.peer();
            MemorySegment peer = (MemorySegment) hInit.invokeExact(alloc, ObjC.sel("initWithFileType:delegate:"), ft, del);
            return wrap(peer);
        } catch (Throwable t) { throw new RuntimeException("NSFilePromiseProvider init failed", t); }
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hFileType = ObjC.handle(Sig.of(Ret.ID));
        hDelegate = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
    }

    /** fileType */
    public String fileType() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hFileType.invokeExact(peer, ObjC.sel("fileType"));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("fileType failed", t); }
    }

    /** delegate */
    public NSObject delegate() {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) hDelegate.invokeExact(peer, ObjC.sel("delegate"));
            return NSObject.wrap(d);
        } catch (Throwable t) { throw new RuntimeException("delegate failed", t); }
    }

    /** userInfo — optional metadata. */
    public NSObject userInfo() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("userInfo"));
            return NSObject.wrap(r);
        } catch (Throwable t) { return null; }
    }

    /** setUserInfo: */
    public void setUserInfo(NSObject info) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setUserInfo:"), (MemorySegment) (info == null || info.peer() == null || info.peer().address() == 0 ? MemorySegment.NULL : info.peer()));
        } catch (Throwable t) { throw new RuntimeException("setUserInfo: failed", t); }
    }
}
