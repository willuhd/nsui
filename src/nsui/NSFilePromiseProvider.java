package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSFilePromiseProvider — minimal wrapper over native `NSFilePromiseProvider`.
/// Used for dragging file promises (e.g., drag-out).
///
/// Coverage notes (header: NSFilePromiseProvider.h wins on API truth):
/// complete for the provider object — initWithFileType:delegate:, fileType
/// (+ setter), delegate (+ setter) and userInfo (+ setter). Omitted: the
/// NSFilePromiseProviderDelegate protocol methods
/// (fileNameForType:/writePromiseToURL:completionHandler:/operationQueueFor…)
/// — delegate protocols need upcall machinery, and two take blocks.
/// init is covered by create() (no separate init overloads).
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

    /// [[NSFilePromiseProvider alloc] initWithFileType:delegate:]
    ///
    /// The delegate must be non-nil AND respond to both required
    /// NSFilePromiseProviderDelegate methods
    /// (filePromiseProvider:fileNameForType: and
    /// filePromiseProvider:writePromiseToURL:completionHandler:) — AppKit
    /// raises an uncatchable NSException (aborts the process) otherwise,
    /// so unlike the peer wrappers this factory rejects a nil delegate in
    /// Java up front. See EventCoverageTest for a minimal conforming delegate.
    public static NSFilePromiseProvider create(String fileType, NSObject delegate) {
        if (delegate == null || delegate.peer() == null || delegate.peer().address() == 0) {
            throw new IllegalArgumentException("initWithFileType:delegate: requires a non-nil delegate implementing the file-promise protocol");
        }
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

    /// fileType
    public String fileType() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hFileType.invokeExact(peer, ObjC.sel("fileType"));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("fileType failed", t); }
    }

    /// delegate
    public NSObject delegate() {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) hDelegate.invokeExact(peer, ObjC.sel("delegate"));
            return NSObject.wrap(d);
        } catch (Throwable t) { throw new RuntimeException("delegate failed", t); }
    }

    /// setFileType:
    public void setFileType(String fileType) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setFileType:"), ObjC.nsstring(fileType));
        } catch (Throwable t) { throw new RuntimeException("setFileType: failed", t); }
    }

    /// setDelegate:
    public void setDelegate(MemorySegment delegate) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setDelegate:"), (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate));
        } catch (Throwable t) { throw new RuntimeException("setDelegate: failed", t); }
    }

    /// setDelegate with NSObject
    public void setDelegate(NSObject delegate) {
        setDelegate(delegate == null ? MemorySegment.NULL : delegate.peer());
    }

    /// userInfo — optional metadata.
    public NSObject userInfo() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment r = (MemorySegment) h.invokeExact(peer, ObjC.sel("userInfo"));
            return NSObject.wrap(r);
        } catch (Throwable t) { return null; }
    }

    /// setUserInfo:
    public void setUserInfo(NSObject info) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
            h.invokeExact(peer, ObjC.sel("setUserInfo:"), (MemorySegment) (info == null || info.peer() == null || info.peer().address() == 0 ? MemorySegment.NULL : info.peer()));
        } catch (Throwable t) { throw new RuntimeException("setUserInfo: failed", t); }
    }
}
