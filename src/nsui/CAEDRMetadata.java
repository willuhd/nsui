package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// CAEDRMetadata — HDR mastering metadata (HDR10/HLG) attached to video and
/// Metal content. Value object built from three factories; consumed by the
/// display pipeline, not inspected further here. Thin stateless wrapper.
public final class CAEDRMetadata extends NSObject {

    private record Handles(MethodHandle hSei, MethodHandle hNits) {}
    private static volatile Handles handles;

    private CAEDRMetadata(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static CAEDRMetadata wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new CAEDRMetadata(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        try { ObjC.ensureFramework("QuartzCore"); } catch (Throwable ignored) {}
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.FLOAT)),
                ObjC.handle(Sig.of(Ret.ID, Arg.FLOAT, Arg.FLOAT, Arg.FLOAT)));
    }

    private static MemorySegment dataOrNull(byte[] bytes) {
        return bytes == null ? MemorySegment.NULL : NSData.dataWithBytes(bytes).peer();
    }

    /// HDR10 from SEI mastering-display and content-light blobs (nil = defaults).
    public static CAEDRMetadata hdr10(byte[] displayInfo, byte[] contentInfo, float scale) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hSei().invokeExact(ObjC.cls("CAEDRMetadata"),
                    ObjC.sel("HDR10MetadataWithDisplayInfo:contentInfo:opticalOutputScale:"),
                    dataOrNull(displayInfo), dataOrNull(contentInfo), scale));
        } catch (Throwable t) {
            throw new RuntimeException("HDR10MetadataWithDisplayInfo:... failed", t);
        }
    }

    /// HDR10 from mastering-display luminance range in nits.
    public static CAEDRMetadata hdr10(float minNits, float maxNits, float scale) {
        ensureInit();
        try {
            return wrap((MemorySegment) handles.hNits().invokeExact(ObjC.cls("CAEDRMetadata"),
                    ObjC.sel("HDR10MetadataWithMinLuminance:maxLuminance:opticalOutputScale:"),
                    minNits, maxNits, scale));
        } catch (Throwable t) {
            throw new RuntimeException("HDR10MetadataWithMinLuminance:... failed", t);
        }
    }

    /// HLG from ambient-viewing-environment blob.
    public static CAEDRMetadata hlg(byte[] ambientViewingEnvironment) {
        ensureInit();
        return wrap(ObjC.msgSendIdId(ObjC.cls("CAEDRMetadata"),
                ObjC.sel("HLGMetadataWithAmbientViewingEnvironment:"),
                dataOrNull(ambientViewingEnvironment)));
    }

    /// HLGMetadata class property.
    public static CAEDRMetadata hlgMetadata() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("CAEDRMetadata"), ObjC.sel("HLGMetadata")));
    }

    /// isAvailable (class).
    public static boolean isAvailable() {
        ensureInit();
        return ObjC.msgSendBool(ObjC.cls("CAEDRMetadata"), ObjC.sel("isAvailable"));
    }
}
