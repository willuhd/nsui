package nsui;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSFontDescriptor — describes a font (family, traits, size).
/// Thin 1:1 wrapper over native `NSFontDescriptor`.
public final class NSFontDescriptor extends NSObject {

            private record Handles(MethodHandle hWithNameSize, MethodHandle hWithAttrs, MethodHandle hGetId, MethodHandle hGetInt, MethodHandle hVoidId) {}
    private static volatile Handles handles;

    private NSFontDescriptor(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSFontDescriptor wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFontDescriptor(peer);
    }

        private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.DOUBLE)),
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.INT)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID))
        );
    }

    // ---- factory ----

    /// `+[NSFontDescriptor fontDescriptorWithName:size:]`
    public static NSFontDescriptor fontDescriptorWithNameSize(String name, double size) {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) handles.hWithNameSize().invokeExact(ObjC.cls("NSFontDescriptor"), ObjC.sel("fontDescriptorWithName:size:"), ObjC.nsstring(name), size);
            if (d == null || d.address() == 0) return null;
            return new NSFontDescriptor(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorWithName:size: failed", t);
        }
    }

    /// `+[NSFontDescriptor fontDescriptorWithFontAttributes:]` — attrs is NSDictionary*
    public static NSFontDescriptor fontDescriptorWithFontAttributes(MemorySegment attributes) {
        ensureInit();
        try {
            MemorySegment arg = (attributes == null ? MemorySegment.NULL : attributes);
            MemorySegment d = (MemorySegment) handles.hWithAttrs().invokeExact(ObjC.cls("NSFontDescriptor"), ObjC.sel("fontDescriptorWithFontAttributes:"), arg);
            if (d == null || d.address() == 0) return null;
            return new NSFontDescriptor(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorWithFontAttributes: failed", t);
        }
    }

    /// `+[NSFontDescriptor preferredFontDescriptorWithTextStyle:options:]`
    public static NSFontDescriptor preferredFontDescriptorWithTextStyle(String style) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment d = (MemorySegment) h.invokeExact(ObjC.cls("NSFontDescriptor"), ObjC.sel("preferredFontDescriptorWithTextStyle:options:"), ObjC.nsstring(style), MemorySegment.NULL);
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("preferredFontDescriptorWithTextStyle:options: failed", t);
        }
    }

    // ---- attributes ----

    /// [descriptor fontAttributes] -> NSDictionary*
    public MemorySegment fontAttributes() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("fontAttributes"));
        } catch (Throwable t) {
            throw new RuntimeException("fontAttributes failed", t);
        }
    }

    /// [descriptor objectForKey:] -> id
    public MemorySegment objectForKey(String key) {
        ensureInit();
        try {
            return (MemorySegment) handles.hWithAttrs().invokeExact(peer, ObjC.sel("objectForKey:"), ObjC.nsstring(key));
        } catch (Throwable t) {
            throw new RuntimeException("objectForKey: failed", t);
        }
    }

    /// [descriptor symbolicTraits] -> NSFontDescriptorSymbolicTraits bitmask
    public long symbolicTraits() {
        ensureInit();
        try {
            return (long) handles.hGetInt().invokeExact(peer, ObjC.sel("symbolicTraits"));
        } catch (Throwable t) {
            throw new RuntimeException("symbolicTraits failed", t);
        }
    }

    /// [descriptor fontDescriptorWithSymbolicTraits:] -> NSFontDescriptor
    public NSFontDescriptor fontDescriptorWithSymbolicTraits(long traits) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment d = (MemorySegment) h.invokeExact(peer, ObjC.sel("fontDescriptorWithSymbolicTraits:"), traits);
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorWithSymbolicTraits: failed", t);
        }
    }

    /// [descriptor fontDescriptorByAddingAttributes:] — attrs is NSDictionary*
    public NSFontDescriptor fontDescriptorByAddingAttributes(MemorySegment attrs) {
        ensureInit();
        try {
            MemorySegment arg = (attrs == null ? MemorySegment.NULL : attrs);
            MemorySegment d = (MemorySegment) handles.hWithAttrs().invokeExact(peer, ObjC.sel("fontDescriptorByAddingAttributes:"), arg);
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorByAddingAttributes: failed", t);
        }
    }

    /// [descriptor fontDescriptorWithSize:] -> NSFontDescriptor
    public NSFontDescriptor fontDescriptorWithSize(double size) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.DOUBLE));
            MemorySegment d = (MemorySegment) h.invokeExact(peer, ObjC.sel("fontDescriptorWithSize:"), size);
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorWithSize: failed", t);
        }
    }

    /// [descriptor fontDescriptorWithFace:] -> NSFontDescriptor
    public NSFontDescriptor fontDescriptorWithFace(String face) {
        try {
            MemorySegment d = (MemorySegment) handles.hWithAttrs().invokeExact(peer, ObjC.sel("fontDescriptorWithFace:"), ObjC.nsstring(face));
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorWithFace: failed", t);
        }
    }

    /// [descriptor fontDescriptorWithFamily:] -> NSFontDescriptor
    public NSFontDescriptor fontDescriptorWithFamily(String family) {
        try {
            MemorySegment d = (MemorySegment) handles.hWithAttrs().invokeExact(peer, ObjC.sel("fontDescriptorWithFamily:"), ObjC.nsstring(family));
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorWithFamily: failed", t);
        }
    }

    /// [descriptor postscriptName] -> NSString -> String
    public String postscriptName() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("postscriptName")));
    }

    /// [descriptor pointSize] -> double
    public double pointSize() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("pointSize"));
        } catch (Throwable t) {
            throw new RuntimeException("pointSize failed", t);
        }
    }

    /// [descriptor matchingFontDescriptorsWithMandatoryKeys:] -> NSArray
    public MemorySegment matchingFontDescriptors(MemorySegment mandatoryKeys) {
        try {
            MemorySegment arg = (mandatoryKeys == null ? MemorySegment.NULL : mandatoryKeys);
            return (MemorySegment) handles.hWithAttrs().invokeExact(peer, ObjC.sel("matchingFontDescriptorsWithMandatoryKeys:"), arg);
        } catch (Throwable t) {
            throw new RuntimeException("matchingFontDescriptorsWithMandatoryKeys: failed", t);
        }
    }

    /// `+[NSFontDescriptor fontDescriptorWithName:matrix:]` — matrix is NSAffineTransform* (may be NULL).
    public static NSFontDescriptor fontDescriptorWithNameMatrix(String name, MemorySegment matrix) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment d = (MemorySegment) h.invokeExact(ObjC.cls("NSFontDescriptor"),
                    ObjC.sel("fontDescriptorWithName:matrix:"), ObjC.nsstring(name == null ? "" : name),
                    (MemorySegment) (matrix == null ? MemorySegment.NULL : matrix));
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorWithName:matrix: failed", t);
        }
    }

    /// `+[NSFontDescriptor preferredFontDescriptorForTextStyle:options:]` (macOS 11+).
    public static NSFontDescriptor preferredFontDescriptorForTextStyleOptions(String style, MemorySegment optionsOrNull) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment d = (MemorySegment) h.invokeExact(ObjC.cls("NSFontDescriptor"),
                    ObjC.sel("preferredFontDescriptorForTextStyle:options:"), ObjC.nsstring(style == null ? "" : style),
                    (MemorySegment) (optionsOrNull == null ? MemorySegment.NULL : optionsOrNull));
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("preferredFontDescriptorForTextStyle:options: failed", t);
        }
    }

    /// [descriptor matrix] -> NSAffineTransform* (raw segment, may be nil).
    public MemorySegment matrix() {
        ensureInit();
        try {
            return (MemorySegment) handles.hGetId().invokeExact(peer, ObjC.sel("matrix"));
        } catch (Throwable t) {
            throw new RuntimeException("matrix failed", t);
        }
    }

    /// [descriptor requiresFontAssetRequest] (macOS 10.13+).
    public boolean requiresFontAssetRequest() {
        return ObjC.msgSendBool(peer, ObjC.sel("requiresFontAssetRequest"));
    }

    /// [descriptor fontDescriptorWithMatrix:] -> NSFontDescriptor.
    public NSFontDescriptor fontDescriptorWithMatrix(MemorySegment matrix) {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) handles.hWithAttrs().invokeExact(peer,
                    ObjC.sel("fontDescriptorWithMatrix:"),
                    (MemorySegment) (matrix == null ? MemorySegment.NULL : matrix));
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorWithMatrix: failed", t);
        }
    }

    /// [descriptor fontDescriptorWithDesign:] -> NSFontDescriptor (macOS 10.15+).
    public NSFontDescriptor fontDescriptorWithDesign(String design) {
        ensureInit();
        try {
            MemorySegment d = (MemorySegment) handles.hWithAttrs().invokeExact(peer,
                    ObjC.sel("fontDescriptorWithDesign:"), ObjC.nsstring(design == null ? "" : design));
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("fontDescriptorWithDesign: failed", t);
        }
    }

    /// [descriptor matchingFontDescriptorWithMandatoryKeys:] -> NSFontDescriptor
    public NSFontDescriptor matchingFontDescriptor(MemorySegment mandatoryKeys) {
        try {
            MemorySegment arg = (mandatoryKeys == null ? MemorySegment.NULL : mandatoryKeys);
            MemorySegment d = (MemorySegment) handles.hWithAttrs().invokeExact(peer, ObjC.sel("matchingFontDescriptorWithMandatoryKeys:"), arg);
            return wrap(d);
        } catch (Throwable t) {
            throw new RuntimeException("matchingFontDescriptorWithMandatoryKeys: failed", t);
        }
    }
}
