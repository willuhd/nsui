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

    private static void ensureInit() {
        if (initialized) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
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

    /// [manager fontPanel:] — create/display font panel.
    // NOTE: needs of(ID,BOOL) — no vocabulary entry (handle() throws at call time).
    // Pre-existing wrapper kept for API stability; not exercised until Sig grows — reported.
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

    /// `+[NSFontManager setFontPanelFactory:]` — factory is Class (may be NULL).
    public static void setFontPanelFactory(MemorySegment factoryOrNull) {
        ObjC.msgSendVoidId(ObjC.cls("NSFontManager"), ObjC.sel("setFontPanelFactory:"),
                (MemorySegment) (factoryOrNull == null ? MemorySegment.NULL : factoryOrNull));
    }

    /// `+[NSFontManager setFontManagerFactory:]` — factory is Class (may be NULL).
    public static void setFontManagerFactory(MemorySegment factoryOrNull) {
        ObjC.msgSendVoidId(ObjC.cls("NSFontManager"), ObjC.sel("setFontManagerFactory:"),
                (MemorySegment) (factoryOrNull == null ? MemorySegment.NULL : factoryOrNull));
    }

    // -fontMenu: omitted: (BOOL)->ID has no vocabulary entry (grep MISS) — reported.

    /// [manager setFontMenu:] — menu is NSMenu* (may be NULL).
    public void setFontMenu(MemorySegment menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setFontMenu:"),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu));
    }

    // -convertFont:toNotHaveTrait: omitted: (ID,INT)->ID i.e. of(ID,ID,INT) has no vocabulary
    // entry (grep: 0 matches) — reported. Same gap breaks the pre-existing
    // -convertFont:toHaveTrait: wrapper (convertFontToHaveTrait) and
    // -fontWithFamily:traits:weight:size: (fontWithFamilyTraitsWeightSize, needs
    // of(ID,ID,INT,INT,DOUBLE), grep: 0 matches) at CALL time; both are left in place
    // for API stability and documented here until Sig gains those shapes.

    // -convertWeight:ofFont: omitted: (BOOL,ID)->ID has no vocabulary entry (grep MISS) — reported.

    /// [manager sendAction] — send the current action up the responder chain.
    public boolean sendAction() {
        return ObjC.msgSendBool(peer, ObjC.sel("sendAction"));
    }

    /// [manager localizedNameForFamily:face:] — display name for a family/face pair.
    public String localizedNameForFamilyFace(String family, String face) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            MemorySegment s = (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("localizedNameForFamily:face:"), ObjC.nsstring(family == null ? "" : family),
                    (MemorySegment) (face == null ? MemorySegment.NULL : ObjC.nsstring(face)));
            return ObjC.toString(s);
        } catch (Throwable t) {
            throw new RuntimeException("localizedNameForFamily:face: failed", t);
        }
    }

    /// [manager setSelectedAttributes:isMultiple:] — attributes is NSDictionary*.
    public void setSelectedAttributesIsMultiple(MemorySegment attributes, boolean isMultiple) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.BOOL));
            h.invokeExact(peer, ObjC.sel("setSelectedAttributes:isMultiple:"),
                    (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes), isMultiple);
        } catch (Throwable t) {
            throw new RuntimeException("setSelectedAttributes:isMultiple: failed", t);
        }
    }

    /// [manager convertAttributes:] — NSDictionary* in/out (raw segments).
    public MemorySegment convertAttributes(MemorySegment attributes) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(peer, ObjC.sel("convertAttributes:"),
                    (MemorySegment) (attributes == null ? MemorySegment.NULL : attributes));
        } catch (Throwable t) {
            throw new RuntimeException("convertAttributes: failed", t);
        }
    }

    /// [manager currentFontAction] (macOS 10.5+).
    public long currentFontAction() {
        ensureInit();
        try {
            return (long) hGetInt.invokeExact(peer, ObjC.sel("currentFontAction"));
        } catch (Throwable t) {
            throw new RuntimeException("currentFontAction failed", t);
        }
    }

    /// [manager convertFontTraits:] (macOS 10.5+).
    public long convertFontTraits(long traits) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.INT));
            return (long) h.invokeExact(peer, ObjC.sel("convertFontTraits:"), traits);
        } catch (Throwable t) {
            throw new RuntimeException("convertFontTraits: failed", t);
        }
    }

    /// [manager target] (macOS 10.5+; raw id).
    public MemorySegment target() {
        return ObjC.msgSendId(peer, ObjC.sel("target"));
    }

    /// [manager setTarget:] (macOS 10.5+; raw id).
    public void setTarget(MemorySegment target) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTarget:"),
                (MemorySegment) (target == null ? MemorySegment.NULL : target));
    }

    /// [manager action] -> SEL (raw pointer segment; bridge via ObjC.sel for sending).
    public MemorySegment action() {
        return ObjC.msgSendId(peer, ObjC.sel("action"));
    }

    /// [manager setAction:] — action is SEL (raw pointer segment).
    public void setAction(MemorySegment action) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAction:"),
                (MemorySegment) (action == null ? MemorySegment.NULL : action));
    }

    /// [manager fontNamed:hasTraits:] — YES if the named font carries the traits.
    public boolean fontNamedHasTraits(String fontName, long traits) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.INT));
            return (boolean) h.invokeExact(peer, ObjC.sel("fontNamed:hasTraits:"),
                    ObjC.nsstring(fontName == null ? "" : fontName), traits);
        } catch (Throwable t) {
            throw new RuntimeException("fontNamed:hasTraits: failed", t);
        }
    }

    /// [manager availableFontNamesWithTraits:] -> NSArray of NSString (untyped count via NSArray).
    public NSArray availableFontNamesWithTraits(long traits) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            MemorySegment p = (MemorySegment) h.invokeExact(peer,
                    ObjC.sel("availableFontNamesWithTraits:"), traits);
            return NSArray.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("availableFontNamesWithTraits: failed", t);
        }
    }

    /// [manager modifyFontViaPanel:]
    public void modifyFontViaPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("modifyFontViaPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// [manager orderFrontStylesPanel:]
    public void orderFrontStylesPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontStylesPanel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // SDK omissions: deprecated collection/font-list API (-availableFontNamesMatchingFontDescriptor:,
    // -collectionNames, -fontDescriptorsInCollection:, -addCollection:..., -removeCollection:,
    // -addFontDescriptors:..., -removeFontDescriptor:...) skipped as deprecated; the-
    // NSFontManager delegate property has no delegate methods (header-deprecated) — skipped.
    // NSError** collection mutation lives on NSFontCollection (likewise omitted) — reported.
}
