package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTrackingArea — minimal wrapper over AppKit NSTrackingArea.
/// Monitors mouse enter/exit/moved events over a rect.
///
/// Coverage notes (header: NSTrackingArea.h wins on API truth): complete —
/// the designated initializer plus the `rect`/`options`/`owner`/`userInfo`
/// readers and the NSTrackingAreaOptions constants below cover the whole
/// header. Omitted: nothing (no blocks, no NSError**, no delegate protocol
/// members here).
public final class NSTrackingArea extends NSObject {

    /// NSTrackingAreaOptions bit values (from NSTrackingArea.h).
    /// One type bit plus one active bit are required; behavior bits optional.
    public static final long MOUSE_ENTERED_AND_EXITED = 0x01;
    public static final long MOUSE_MOVED = 0x02;
    public static final long CURSOR_UPDATE = 0x04;
    public static final long ACTIVE_WHEN_FIRST_RESPONDER = 0x10;
    public static final long ACTIVE_IN_KEY_WINDOW = 0x20;
    public static final long ACTIVE_IN_ACTIVE_APP = 0x40;
    public static final long ACTIVE_ALWAYS = 0x80;
    public static final long ASSUME_INSIDE = 0x100;
    public static final long IN_VISIBLE_RECT = 0x200;
    public static final long ENABLED_DURING_MOUSE_DRAG = 0x400;

    /// The classic tracking combo: entered/exited + moved, always active,
    /// in the visible rect (matches NSView.enableMouseTracking).
    public static final long DEFAULT_OPTIONS =
            MOUSE_ENTERED_AND_EXITED | MOUSE_MOVED | ACTIVE_ALWAYS | IN_VISIBLE_RECT;


            private record Handles(MethodHandle hInit, MethodHandle hRect, MethodHandle hInt, MethodHandle hId) {}
    private static volatile Handles handles;

    private NSTrackingArea(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSTrackingArea wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTrackingArea(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
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
            MemorySegment r = (MemorySegment) handles.hRect().invokeExact(ObjC.structSlot(), peer, ObjC.sel("rect"));
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
