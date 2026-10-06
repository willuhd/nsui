package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSParagraphStyle — immutable paragraph style.
/// Thin 1:1 wrapper over native `NSParagraphStyle`: every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer.
/// Follows FFM pattern: no reflection, cached handles, ensureInit.
public class NSParagraphStyle extends NSObject {

            private record Handles(MethodHandle hGetLong) {}
    private static volatile Handles handles;

    protected NSParagraphStyle(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSParagraphStyle wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSParagraphStyle(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.INT)));
    }

    /// [NSParagraphStyle defaultParagraphStyle]
    public static NSParagraphStyle defaultParagraphStyle() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSParagraphStyle"), ObjC.sel("defaultParagraphStyle"));
        return wrap(p);
    }

    /// [style alignment] -> NSTextAlignment (long)
    public long alignment() {
        ensureInit();
        try {
            return (long) handles.hGetLong().invokeExact(peer, ObjC.sel("alignment"));
        } catch (Throwable t) {
            throw new RuntimeException("alignment failed", t);
        }
    }

    /// [style lineBreakMode] -> NSLineBreakMode (long)
    public long lineBreakMode() {
        ensureInit();
        try {
            return (long) handles.hGetLong().invokeExact(peer, ObjC.sel("lineBreakMode"));
        } catch (Throwable t) {
            throw new RuntimeException("lineBreakMode failed", t);
        }
    }

    /// `+[NSParagraphStyle defaultWritingDirectionForLanguage:]` -> NSWritingDirection.
    public static long defaultWritingDirectionForLanguage(String language) {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.INT, Arg.ID));
            return (long) h.invokeExact(ObjC.cls("NSParagraphStyle"),
                    ObjC.sel("defaultWritingDirectionForLanguage:"), ObjC.nsstring(language == null ? "" : language));
        } catch (Throwable t) {
            throw new RuntimeException("defaultWritingDirectionForLanguage: failed", t);
        }
    }

    /// [style paragraphSpacing] -> CGFloat.
    public double paragraphSpacing() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("paragraphSpacing"));
        } catch (Throwable t) {
            throw new RuntimeException("paragraphSpacing failed", t);
        }
    }

    /// [style headIndent] -> CGFloat.
    public double headIndent() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("headIndent"));
        } catch (Throwable t) {
            throw new RuntimeException("headIndent failed", t);
        }
    }

    /// [style tailIndent] -> CGFloat.
    public double tailIndent() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("tailIndent"));
        } catch (Throwable t) {
            throw new RuntimeException("tailIndent failed", t);
        }
    }

    /// [style firstLineHeadIndent] -> CGFloat.
    public double firstLineHeadIndent() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("firstLineHeadIndent"));
        } catch (Throwable t) {
            throw new RuntimeException("firstLineHeadIndent failed", t);
        }
    }

    /// [style minimumLineHeight] -> CGFloat.
    public double minimumLineHeight() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("minimumLineHeight"));
        } catch (Throwable t) {
            throw new RuntimeException("minimumLineHeight failed", t);
        }
    }

    /// [style maximumLineHeight] -> CGFloat (0 = no maximum).
    public double maximumLineHeight() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("maximumLineHeight"));
        } catch (Throwable t) {
            throw new RuntimeException("maximumLineHeight failed", t);
        }
    }

    /// [style baseWritingDirection] -> NSWritingDirection (long).
    public long baseWritingDirection() {
        ensureInit();
        try {
            return (long) handles.hGetLong().invokeExact(peer, ObjC.sel("baseWritingDirection"));
        } catch (Throwable t) {
            throw new RuntimeException("baseWritingDirection failed", t);
        }
    }

    /// [style lineHeightMultiple] -> CGFloat.
    public double lineHeightMultiple() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("lineHeightMultiple"));
        } catch (Throwable t) {
            throw new RuntimeException("lineHeightMultiple failed", t);
        }
    }

    /// [style paragraphSpacingBefore] -> CGFloat.
    public double paragraphSpacingBefore() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("paragraphSpacingBefore"));
        } catch (Throwable t) {
            throw new RuntimeException("paragraphSpacingBefore failed", t);
        }
    }

    /// [style hyphenationFactor] -> float (0.0..1.0).
    public float hyphenationFactor() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.FLOAT));
            return (float) h.invokeExact(peer, ObjC.sel("hyphenationFactor"));
        } catch (Throwable t) {
            throw new RuntimeException("hyphenationFactor failed", t);
        }
    }

    /// [style usesDefaultHyphenation] (macOS 12+).
    public boolean usesDefaultHyphenation() {
        return ObjC.msgSendBool(peer, ObjC.sel("usesDefaultHyphenation"));
    }

    /// [style tabStops] -> NSArray of NSTextTab (untyped; NSTextTab has no wrapper in this batch).
    public NSArray tabStops() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("tabStops")));
    }

    /// [style defaultTabInterval] -> CGFloat.
    public double defaultTabInterval() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (double) h.invokeExact(peer, ObjC.sel("defaultTabInterval"));
        } catch (Throwable t) {
            throw new RuntimeException("defaultTabInterval failed", t);
        }
    }

    /// [style textLists] -> NSArray of NSTextList (untyped).
    public NSArray textLists() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("textLists")));
    }

    /// [style textBlocks] -> NSArray of NSTextBlock (untyped).
    public NSArray textBlocks() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("textBlocks")));
    }

    /// [style allowsDefaultTighteningForTruncation].
    public boolean allowsDefaultTighteningForTruncation() {
        return ObjC.msgSendBool(peer, ObjC.sel("allowsDefaultTighteningForTruncation"));
    }

    /// [style lineBreakStrategy] -> NSLineBreakStrategy (long).
    public long lineBreakStrategy() {
        return ObjC.msgSendLong(peer, ObjC.sel("lineBreakStrategy"));
    }

    /// [style tighteningFactorForTruncation] -> float.
    public float tighteningFactorForTruncation() {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.FLOAT));
            return (float) h.invokeExact(peer, ObjC.sel("tighteningFactorForTruncation"));
        } catch (Throwable t) {
            throw new RuntimeException("tighteningFactorForTruncation failed", t);
        }
    }

    /// [style headerLevel] -> NSInteger.
    public long headerLevel() {
        return ObjC.msgSendLong(peer, ObjC.sel("headerLevel"));
    }

    /// [style mutableCopy] -> NSMutableParagraphStyle
    public NSMutableParagraphStyle mutableCopy() {
        ensureInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.ID));
            MemorySegment p = (MemorySegment) h.invokeExact(peer, ObjC.sel("mutableCopy"));
            return NSMutableParagraphStyle.wrap(p);
        } catch (Throwable t) {
            throw new RuntimeException("mutableCopy failed", t);
        }
    }

    // Additional useful getters (completeness, no extra vocabulary needed)
    public long lineSpacing() {
        ensureInit();
        try {
            // Actually lineSpacing is CGFloat (double) — use double handle
            MethodHandle h = ObjC.handle(Sig.of(Ret.DOUBLE));
            return (long) (double) h.invokeExact(peer, ObjC.sel("lineSpacing"));
        } catch (Throwable t) {
            throw new RuntimeException("lineSpacing failed", t);
        }
    }
}
