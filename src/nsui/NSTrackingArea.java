package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTrackingArea — minimal wrapper over AppKit NSTrackingArea.
/// Monitors mouse enter/exit/moved events over a rect.
public final class NSTrackingArea extends NSObject {

            private record Handles(MethodHandle hInit, MethodHandle hRect, MethodHandle hInt, MethodHandle hId) {}
    private static volatile Handles handles;

    private NSTrackingArea(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTrackingArea wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTrackingArea(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.RECT, Arg.INT, Arg.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.RECT)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.ID))
        );
    }

    /// [[NSTrackingArea alloc] initWithRect:options:owner:userInfo:]
    /// @param rect the tracking rect in the owner's coordinate system
    /// @param options NSTrackingAreaOptions bitfield (e.g. 1=MouseEnteredAndExited, etc.)
    /// @param owner the view/object that receives tracking events (NSView)
    /// @param userInfo optional user info dict (may be null)
    public static NSTrackingArea create(NSRect rect, long options, NSView owner, MemorySegment userInfo) {
        ensureInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSTrackingArea"), ObjC.sel("alloc"));
        try {
            MemorySegment p = (MemorySegment) handles.hInit().invokeExact(alloc, ObjC.sel("initWithRect:options:owner:userInfo:"),
                    rect.toSegment(), options,
                    (MemorySegment) (owner == null ? MemorySegment.NULL : owner.peer()),
                    (MemorySegment) (userInfo == null ? MemorySegment.NULL : userInfo));
            if (p == null || p.address() == 0) throw new IllegalStateException("NSTrackingArea init returned nil");
            return new NSTrackingArea(p);
        } catch (Throwable t) {
            throw new RuntimeException("initWithRect:options:owner:userInfo: failed", t);
        }
    }

    public static NSTrackingArea create(NSRect rect, long options, NSView owner) {
        return create(rect, options, owner, null);
    }

    /// [area rect] -> NSRect
    public NSRect rect() {
        ensureInit();
        try {
            MemorySegment r = (MemorySegment) handles.hRect().invokeExact((java.lang.foreign.SegmentAllocator) java.lang.foreign.Arena.global(), peer, ObjC.sel("rect"));
            return NSRect.fromSegment(r);
        } catch (Throwable t) {
            throw new RuntimeException("rect failed", t);
        }
    }

    /// [area options] -> long
    public long options() {
        ensureInit();
        try {
            return (long) handles.hInt().invokeExact(peer, ObjC.sel("options"));
        } catch (Throwable t) {
            throw new RuntimeException("options failed", t);
        }
    }

    /// [area owner] -> id
    public MemorySegment owner() {
        ensureInit();
        try {
            return (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("owner"));
        } catch (Throwable t) {
            throw new RuntimeException("owner failed", t);
        }
    }

    /// [area userInfo] -> NSDictionary id
    public MemorySegment userInfo() {
        ensureInit();
        try {
            return (MemorySegment) handles.hId().invokeExact(peer, ObjC.sel("userInfo"));
        } catch (Throwable t) {
            throw new RuntimeException("userInfo failed", t);
        }
    }
}
