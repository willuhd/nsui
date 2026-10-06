package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CATiledLayer — tiled large-content layer with detail levels and tile size.
public class CATiledLayer extends CALayer {
    private record Handles(MethodHandle hGetSize, MethodHandle hSetSize) {}
    private static volatile Handles handles;

    protected CATiledLayer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CATiledLayer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CATiledLayer(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(ObjC.handle(Sig.of(Ret.SIZE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.SIZE)));
    }

    /// [[CATiledLayer alloc] init].
    public static CATiledLayer create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("CATiledLayer"), ObjC.sel("alloc"));
        p = ObjC.msgSendId(p, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("init returned nil for CATiledLayer");
        return new CATiledLayer(p);
    }

    /// fadeDuration — crossfade between detail levels (class property).
    public static double fadeDuration() {
        ensureInit();
        try {
            return (double) ObjC.handle(Sig.of(Ret.DOUBLE)).invokeExact(
                    ObjC.cls("CATiledLayer"), ObjC.sel("fadeDuration"));
        } catch (Throwable t) {
            throw new RuntimeException("fadeDuration failed", t);
        }
    }

    /// levelsOfDetail.
    public long levelsOfDetail() { return ObjC.msgSendLong(peer, ObjC.sel("levelsOfDetail")); }
    /// setLevelsOfDetail:.
    public void setLevelsOfDetail(long n) { ObjC.msgSendVoidLong(peer, ObjC.sel("setLevelsOfDetail:"), n); }
    /// levelsOfDetailBias.
    public long levelsOfDetailBias() { return ObjC.msgSendLong(peer, ObjC.sel("levelsOfDetailBias")); }
    /// setLevelsOfDetailBias:.
    public void setLevelsOfDetailBias(long n) { ObjC.msgSendVoidLong(peer, ObjC.sel("setLevelsOfDetailBias:"), n); }
    /// tileSize.
    public NSSize tileSize() {
        try {
            return NSSize.fromSegment((MemorySegment) handles.hGetSize().invokeExact(ObjC.structSlot(), peer, ObjC.sel("tileSize")));
        } catch (Throwable t) {
            throw new RuntimeException("tileSize failed", t);
        }
    }

    /// setTileSize:.
    public void setTileSize(NSSize size) {
        try {
            handles.hSetSize().invokeExact(peer, ObjC.sel("setTileSize:"), size.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("setTileSize: failed", t);
        }
    }
}
