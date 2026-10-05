package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSBundle — a bundled directory of code and resources (Foundation).
/// Thin stateless wrapper. For a bare single binary, mainBundle answers from
/// the embedded __TEXT,__info_plist section when present.
/// OMITTED: initWithPath:/initWithURL: (use bundleWithPath:/bundleWithURL:);
/// preflightAndReturnError:/loadAndReturnError: (NSError** out-param, no registered
/// void-or-bool-with-error shape in use here); NSBundleResourceRequest + preservation
/// (API_UNAVAILABLE macos); float shapes are N/A here.
public final class NSBundle extends NSObject {

    private NSBundle(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static NSBundle wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSBundle(peer);
    }

    /// mainBundle — this process's bundle.
    public static NSBundle mainBundle() {
        return wrap(ObjC.msgSendId(ObjC.cls("NSBundle"), ObjC.sel("mainBundle")));
    }

    /// bundleWithPath:.
    public static NSBundle bundleWithPath(String path) {
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSBundle"), ObjC.sel("bundleWithPath:"), ObjC.nsstring(path)));
    }

    /// bundleWithIdentifier:.
    public static NSBundle bundleWithIdentifier(String identifier) {
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSBundle"), ObjC.sel("bundleWithIdentifier:"), ObjC.nsstring(identifier)));
    }

    /// bundlePath (nil-safe).
    public String bundlePath() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("bundlePath")));
    }

    /// bundleIdentifier (nil-safe; nil without bundle metadata).
    public String bundleIdentifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("bundleIdentifier")));
    }

    /// bundleWithURL: (NSURL as generic id).
    public static NSBundle bundleWithURL(MemorySegment url) {
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSBundle"), ObjC.sel("bundleWithURL:"),
                (MemorySegment) (url == null ? MemorySegment.NULL : url)));
    }

    /// bundleForClass: (Class as generic id).
    public static NSBundle bundleForClass(MemorySegment cls) {
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSBundle"), ObjC.sel("bundleForClass:"),
                (MemorySegment) (cls == null ? MemorySegment.NULL : cls)));
    }

    /// allBundles.
    public static NSArray allBundles() {
        return NSArray.wrap(ObjC.msgSendId(ObjC.cls("NSBundle"), ObjC.sel("allBundles")));
    }

    /// allFrameworks.
    public static NSArray allFrameworks() {
        return NSArray.wrap(ObjC.msgSendId(ObjC.cls("NSBundle"), ObjC.sel("allFrameworks")));
    }

    /// load.
    public boolean load() {
        return ObjC.msgSendBool(peer, ObjC.sel("load"));
    }

    /// isLoaded.
    public boolean isLoaded() {
        return ObjC.msgSendBool(peer, ObjC.sel("isLoaded"));
    }

    /// unload.
    public boolean unload() {
        return ObjC.msgSendBool(peer, ObjC.sel("unload"));
    }

    /// bundleURL (NSURL as generic id).
    public MemorySegment bundleURL() {
        return ObjC.msgSendId(peer, ObjC.sel("bundleURL"));
    }

    /// resourceURL (NSURL as generic id, nil-safe).
    public MemorySegment resourceURL() {
        return ObjC.msgSendId(peer, ObjC.sel("resourceURL"));
    }

    /// executableURL (NSURL as generic id, nil-safe).
    public MemorySegment executableURL() {
        return ObjC.msgSendId(peer, ObjC.sel("executableURL"));
    }

    /// URLForAuxiliaryExecutable: (NSURL as generic id).
    public MemorySegment URLForAuxiliaryExecutable(String name) {
        return ObjC.msgSendIdId(peer, ObjC.sel("URLForAuxiliaryExecutable:"),
                (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)));
    }

    /// privateFrameworksURL / sharedFrameworksURL / sharedSupportURL / builtInPlugInsURL.
    public MemorySegment privateFrameworksURL() {
        return ObjC.msgSendId(peer, ObjC.sel("privateFrameworksURL"));
    }
    public MemorySegment sharedFrameworksURL() {
        return ObjC.msgSendId(peer, ObjC.sel("sharedFrameworksURL"));
    }
    public MemorySegment sharedSupportURL() {
        return ObjC.msgSendId(peer, ObjC.sel("sharedSupportURL"));
    }
    public MemorySegment builtInPlugInsURL() {
        return ObjC.msgSendId(peer, ObjC.sel("builtInPlugInsURL"));
    }
    public MemorySegment appStoreReceiptURL() {
        return ObjC.msgSendId(peer, ObjC.sel("appStoreReceiptURL"));
    }

    /// resourcePath / executablePath (nil-safe strings).
    public String resourcePath() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("resourcePath")));
    }
    public String executablePath() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("executablePath")));
    }
    public String pathForAuxiliaryExecutable(String name) {
        return ObjC.toString(ObjC.msgSendIdId(peer, ObjC.sel("pathForAuxiliaryExecutable:"),
                (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name))));
    }
    public String privateFrameworksPath() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("privateFrameworksPath")));
    }
    public String sharedFrameworksPath() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("sharedFrameworksPath")));
    }
    public String sharedSupportPath() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("sharedSupportPath")));
    }
    public String builtInPlugInsPath() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("builtInPlugInsPath")));
    }

    /// pathForResource:ofType:.
    public String pathForResource(String name, String ext) {
        try {
            MemorySegment p = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("pathForResource:ofType:"),
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)),
                    (MemorySegment) (ext == null ? MemorySegment.NULL : ObjC.nsstring(ext)));
            return ObjC.toString(p);
        } catch (Throwable t) { throw new RuntimeException("pathForResource:ofType: failed", t); }
    }

    /// pathForResource:ofType:inDirectory:.
    public String pathForResourceInDirectory(String name, String ext, String subpath) {
        try {
            MemorySegment p = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("pathForResource:ofType:inDirectory:"),
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)),
                    (MemorySegment) (ext == null ? MemorySegment.NULL : ObjC.nsstring(ext)),
                    (MemorySegment) (subpath == null ? MemorySegment.NULL : ObjC.nsstring(subpath)));
            return ObjC.toString(p);
        } catch (Throwable t) { throw new RuntimeException("pathForResource:ofType:inDirectory: failed", t); }
    }

    /// pathForResource:ofType:inDirectory:forLocalization: (4 object args via escape hatch).
    public String pathForResourceInDirectoryForLocalization(String name, String ext, String subpath, String localization) {
        MemorySegment p = ObjC.invoke(peer, ObjC.sel("pathForResource:ofType:inDirectory:forLocalization:"),
                (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)),
                (MemorySegment) (ext == null ? MemorySegment.NULL : ObjC.nsstring(ext)),
                (MemorySegment) (subpath == null ? MemorySegment.NULL : ObjC.nsstring(subpath)),
                (MemorySegment) (localization == null ? MemorySegment.NULL : ObjC.nsstring(localization)));
        return ObjC.toString(p);
    }

    /// pathsForResourcesOfType:inDirectory:.
    public NSArray pathsForResourcesOfTypeInDirectory(String ext, String subpath) {
        try {
            MemorySegment p = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("pathsForResourcesOfType:inDirectory:"),
                    (MemorySegment) (ext == null ? MemorySegment.NULL : ObjC.nsstring(ext)),
                    (MemorySegment) (subpath == null ? MemorySegment.NULL : ObjC.nsstring(subpath)));
            return NSArray.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("pathsForResourcesOfType:inDirectory: failed", t); }
    }

    /// URLForResource:withExtension:.
    public MemorySegment URLForResource(String name, String ext) {
        try {
            return (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("URLForResource:withExtension:"),
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)),
                    (MemorySegment) (ext == null ? MemorySegment.NULL : ObjC.nsstring(ext)));
        } catch (Throwable t) { throw new RuntimeException("URLForResource:withExtension: failed", t); }
    }

    /// URLForResource:withExtension:subdirectory:.
    public MemorySegment URLForResourceInDirectory(String name, String ext, String subpath) {
        try {
            return (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("URLForResource:withExtension:subdirectory:"),
                    (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)),
                    (MemorySegment) (ext == null ? MemorySegment.NULL : ObjC.nsstring(ext)),
                    (MemorySegment) (subpath == null ? MemorySegment.NULL : ObjC.nsstring(subpath)));
        } catch (Throwable t) { throw new RuntimeException("URLForResource:withExtension:subdirectory: failed", t); }
    }

    /// localizedStringForKey:value:table:.
    public String localizedStringForKey(String key, String value, String table) {
        try {
            MemorySegment p = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("localizedStringForKey:value:table:"),
                    (MemorySegment) (key == null ? MemorySegment.NULL : ObjC.nsstring(key)),
                    (MemorySegment) (value == null ? MemorySegment.NULL : ObjC.nsstring(value)),
                    (MemorySegment) (table == null ? MemorySegment.NULL : ObjC.nsstring(table)));
            return ObjC.toString(p);
        } catch (Throwable t) { throw new RuntimeException("localizedStringForKey:value:table: failed", t); }
    }

    /// infoDictionary / localizedInfoDictionary.
    public NSDictionary infoDictionary() {
        return NSDictionary.wrap(ObjC.msgSendId(peer, ObjC.sel("infoDictionary")));
    }
    public NSDictionary localizedInfoDictionary() {
        return NSDictionary.wrap(ObjC.msgSendId(peer, ObjC.sel("localizedInfoDictionary")));
    }

    /// objectForInfoDictionaryKey:.
    public MemorySegment objectForInfoDictionaryKey(String key) {
        return ObjC.msgSendIdId(peer, ObjC.sel("objectForInfoDictionaryKey:"),
                (MemorySegment) (key == null ? MemorySegment.NULL : ObjC.nsstring(key)));
    }

    /// classNamed: (Class as generic id).
    public MemorySegment classNamed(String name) {
        return ObjC.msgSendIdId(peer, ObjC.sel("classNamed:"),
                (MemorySegment) (name == null ? MemorySegment.NULL : ObjC.nsstring(name)));
    }

    /// principalClass (Class as generic id).
    public MemorySegment principalClass() {
        return ObjC.msgSendId(peer, ObjC.sel("principalClass"));
    }

    /// preferredLocalizations / localizations / developmentLocalization.
    public NSArray preferredLocalizations() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("preferredLocalizations")));
    }
    public NSArray localizations() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("localizations")));
    }
    public String developmentLocalization() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("developmentLocalization")));
    }

    /// preferredLocalizationsFromArray: (class).
    public static NSArray preferredLocalizationsFromArray(NSArray array) {
        return NSArray.wrap(ObjC.msgSendIdId(ObjC.cls("NSBundle"), ObjC.sel("preferredLocalizationsFromArray:"),
                (MemorySegment) (array == null ? MemorySegment.NULL : array.peer())));
    }

    /// executableArchitectures.
    public NSArray executableArchitectures() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("executableArchitectures")));
    }
}
