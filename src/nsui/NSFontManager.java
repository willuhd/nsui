package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSFontManager — controls the Font panel and font conversions.
/// Thin 1:1 wrapper over native `NSFontManager`.
public final class NSFontManager extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hGetId;      // (id, SEL) -> id
    private static MethodHandle hSetFont;    // (id, SEL, id, bool) -> void setSelectedFont:isMultiple:
    private static MethodHandle hConvert;    // (id, SEL, id) -> id  convertFont:
    private static MethodHandle hVoidBool;   // (id, SEL, bool) -> void
    private static MethodHandle hBool;       // (id, SEL) -> bool
    private static MethodHandle hGetInt;     // (id, SEL) -> long

    private NSFontManager(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSFontManager wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFontManager(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hSetFont = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.BOOL));
        hConvert = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hVoidBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hGetInt = ObjC.handle(Sig.of(Ret.INT));
        initialized = true;
    }

    /// `+[NSFontManager sharedFontManager]`
    public static NSFontManager sharedFontManager() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSFontManager"), ObjC.sel("sharedFontManager"));
        return wrap(p);
    }

    // ---- selection ----

    /// [manager selectedFont] -> NSFont
    public NSFont selectedFont() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("selectedFont"));
            return NSFont.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("selectedFont failed", t);
        }
    }

    /// [manager setSelectedFont:isMultiple:]
    public void setSelectedFont(NSFont font, boolean isMultiple) {
        ensureInit();
        try {
            hSetFont.invokeExact(peer, ObjC.sel("setSelectedFont:isMultiple:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()), isMultiple);
        } catch (Throwable t) {
            throw new RuntimeException("setSelectedFont:isMultiple: failed", t);
        }
    }

    /// [manager isMultiple]
    public boolean isMultiple() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isMultiple")); } catch (Throwable t) { throw new RuntimeException("isMultiple failed", t); }
    }

    // ---- font conversion ----

    /// [manager convertFont:] -> NSFont
    public NSFont convertFont(NSFont font) {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hConvert.invokeExact(peer, ObjC.sel("convertFont:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
            return NSFont.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("convertFont: failed", t);
        }
    }

    /// [manager convertFont:toFace:]
    public NSFont convertFontToFace(NSFont font, String face) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("convertFont:toFace:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()), ObjC.nsstring(face));
            return NSFont.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("convertFont:toFace: failed", t);
        }
    }

    /// [manager convertFont:toFamily:]
    public NSFont convertFontToFamily(NSFont font, String family) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("convertFont:toFamily:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()), ObjC.nsstring(family));
            return NSFont.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("convertFont:toFamily: failed", t);
        }
    }

    /// [manager convertFont:toSize:]
    public NSFont convertFontToSize(NSFont font, double size) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.DOUBLE));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("convertFont:toSize:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()), size);
            return NSFont.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("convertFont:toSize: failed", t);
        }
    }

    /// [manager convertFont:toHaveTrait:]
    public NSFont convertFontToHaveTrait(NSFont font, long trait) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.INT));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("convertFont:toHaveTrait:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()), trait);
            return NSFont.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("convertFont:toHaveTrait: failed", t);
        }
    }

    // ---- traits / weight ----

    /// [manager traitsOfFont:] -> long (NSFontTraitMask)
    public long traitsOfFont(NSFont font) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("traitsOfFont:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("traitsOfFont: failed", t);
        }
    }

    /// [manager weightOfFont:] -> long (weight index)
    public long weightOfFont(NSFont font) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(peer, ObjC.sel("weightOfFont:"), (MemorySegment) (font == null ? MemorySegment.NULL : font.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("weightOfFont: failed", t);
        }
    }

    /// [manager fontWithFamily:traits:weight:size:]
    public NSFont fontWithFamilyTraitsWeightSize(String family, long traits, long weight, double size) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.INT, Arg.INT, Arg.DOUBLE));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("fontWithFamily:traits:weight:size:"), ObjC.nsstring(family), traits, weight, size);
            return NSFont.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("fontWithFamily:traits:weight:size: failed", t);
        }
    }

    // ---- collections ----

    /// [manager availableFonts] -> NSArray ids
    public MemorySegment availableFonts() {
        ensureInit();
        try { return (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("availableFonts")); } catch (Throwable t) { throw new RuntimeException("availableFonts failed", t); }
    }

    /// [manager availableFontFamilies] -> NSArray ids
    public MemorySegment availableFontFamilies() {
        ensureInit();
        try { return (MemorySegment) hGetId.invokeExact(peer, ObjC.sel("availableFontFamilies")); } catch (Throwable t) { throw new RuntimeException("availableFontFamilies failed", t); }
    }

    /// [manager availableMembersOfFontFamily:] -> NSArray
    public MemorySegment availableMembersOfFontFamily(String family) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("availableMembersOfFontFamily:"), ObjC.nsstring(family));
        } catch (Throwable t) {
            throw new RuntimeException("availableMembersOfFontFamily: failed", t);
        }
    }

    // ---- font panel ----

    /// [manager fontPanel:] — create/display font panel
    public NSFontPanel fontPanel(boolean create) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.BOOL));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("fontPanel:"), create);
            return NSFontPanel.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("fontPanel: failed", t);
        }
    }

    /// [manager orderFrontFontPanel:]
    public void orderFrontFontPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontFontPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- action helpers ----

    /// [manager addFontTrait:]
    public void addFontTrait(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addFontTrait:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [manager removeFontTrait:]
    public void removeFontTrait(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeFontTrait:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [manager modifyFont:]
    public void modifyFont(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("modifyFont:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [manager isEnabled]
    public boolean isEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEnabled"));
    }
    public void setEnabled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEnabled:"), flag);
    }
}
