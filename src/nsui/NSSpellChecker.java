package nsui;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSpellChecker — spelling/word-count queries against system dictionaries.
/// Thin stateless wrapper. Only the synchronous query core is wrapped:
/// panel driving, ignored-word lists, and block-based async checking are out.
public final class NSSpellChecker extends NSObject {

    private record Handles(MethodHandle hCheck, MethodHandle hCount) {}
    private static volatile Handles handles;

    private NSSpellChecker(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSSpellChecker wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSpellChecker(peer);
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.RANGE, Arg.ID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.INT, Arg.ID, Arg.ID)));
    }

    /// sharedSpellChecker.
    public static NSSpellChecker shared() {
        ensureInit();
        return wrap(ObjC.msgSendId(ObjC.cls("NSSpellChecker"), ObjC.sel("sharedSpellChecker")));
    }

    /// uniqueSpellDocumentTag.
    public static long uniqueSpellDocumentTag() {
        return ObjC.msgSendLong(ObjC.cls("NSSpellChecker"), ObjC.sel("uniqueSpellDocumentTag"));
    }

    /// checkSpellingOfString:startingAt: — range of the first misspelling
    /// ({NOT_FOUND, 0} when clean). Struct return via the shared allocator.
    public NSRange checkSpelling(String text, long offset) {
        ensureInit();
        try {
            MemorySegment seg = (MemorySegment) handles.hCheck().invokeExact(
                    (SegmentAllocator) Arena.global(), peer,
                    ObjC.sel("checkSpellingOfString:startingAt:"), ObjC.nsstring(text), offset);
            return NSRange.fromSegment(seg);
        } catch (Throwable t) {
            throw new RuntimeException("checkSpellingOfString:startingAt: failed", t);
        }
    }

    /// countWordsInString:language: (nil language = autodetect).
    public long countWords(String text, String language) {
        ensureInit();
        try {
            return (long) handles.hCount().invokeExact(peer, ObjC.sel("countWordsInString:language:"),
                    ObjC.nsstring(text),
                    (MemorySegment) (language == null ? MemorySegment.NULL : ObjC.nsstring(language)));
        } catch (Throwable t) {
            throw new RuntimeException("countWordsInString:language: failed", t);
        }
    }
}
