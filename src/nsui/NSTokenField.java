package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSTokenField — a text field whose contents tokenize into discrete tokens
/// (e.g. Mail address fields). Extends NSTextField; inherits its text access.
/// Thin stateless wrapper: each method maps to one objc_msgSend selector.
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSTokenField.h
/// (+ NSTokenFieldCell.h for NSTokenStyle Default 0, None 1, Rounded 2, Squared 3, PlainSquared 4)
/// OMITTED: the NSTokenFieldDelegate protocol (tokenField:completionsForSubstring:...,
/// shouldAddObjects:atIndex:, displayStringForRepresentedObject:, editingStringForRepresentedObject:,
/// representedObjectForEditingString:, writeRepresentedObjects:toPasteboard:, readFromPasteboard:,
/// menuForRepresentedObject:, hasMenuForRepresentedObject:, styleForRepresentedObject:) — delegate
/// callbacks need upcall delegate-proxy machinery, out of scope here (their multi-arg + NSRange
/// shapes are also NOT in the Sig vocabulary); delegate property itself is inherited from
/// NSTextField (same selectors delegate/setDelegate:). No NSCharacterSet wrapper exists in this
/// slice, so tokenizingCharacterSet is raw MemorySegment (shapes ID ()/VOID,ID are in the vocabulary).
public final class NSTokenField extends NSTextField {

    private record Handles(MethodHandle hDouble, MethodHandle hSetDouble) {}
    private static volatile Handles handles;

    private NSTokenField(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer (nil-safe).
    public static NSTokenField wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSTokenField(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.DOUBLE)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE)));
    }

    /// [[NSTokenField alloc] initWithFrame:].
    public static NSTokenField create(NSRect frame) {
        ensureInit();
        return new NSTokenField(ObjC.newView("NSTokenField", frame));
    }

    /// tokenStyle (NSTokenStyle) — default style for each new token.
    public long tokenStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("tokenStyle"));
    }

    /// setTokenStyle:.
    public void setTokenStyle(long style) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setTokenStyle:"), style);
    }

    /// completionDelay (NSTimeInterval) — delay before completions appear.
    public double completionDelay() {
        ensureInit();
        try {
            return (double) handles.hDouble().invokeExact(peer, ObjC.sel("completionDelay"));
        } catch (Throwable t) {
            throw new RuntimeException("completionDelay failed", t);
        }
    }

    /// setCompletionDelay:.
    public void setCompletionDelay(double delay) {
        ensureInit();
        try {
            handles.hSetDouble().invokeExact(peer, ObjC.sel("setCompletionDelay:"), delay);
        } catch (Throwable t) {
            throw new RuntimeException("setCompletionDelay: failed", t);
        }
    }

    // ---- completeness: remaining header API in registered shapes ----
    /// +defaultCompletionDelay — class default delay (NSTimeInterval double).
    /// Shape (DOUBLE ()) is in the vocabulary (grep Sig.java: of(Ret.DOUBLE)).
    public static double defaultCompletionDelay() {
        ensureInit();
        try {
            return (double) handles.hDouble().invokeExact(ObjC.cls("NSTokenField"), ObjC.sel("defaultCompletionDelay"));
        } catch (Throwable th) {
            throw new RuntimeException("defaultCompletionDelay failed", th);
        }
    }

    /// tokenizingCharacterSet — charset delimiting tokens (null_resettable, raw id).
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary. No NSCharacterSet wrapper
    /// exists in this slice (grep src/nsui/NSCharacterSet.java: miss), so raw.
    public MemorySegment tokenizingCharacterSet() {
        return ObjC.msgSendId(peer, ObjC.sel("tokenizingCharacterSet"));
    }
    /// setTokenizingCharacterSet: — pass null to reset to the default set.
    public void setTokenizingCharacterSet(MemorySegment charset) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTokenizingCharacterSet:"),
                (MemorySegment) (charset == null ? MemorySegment.NULL : charset));
    }

    /// +defaultTokenizingCharacterSet — class default set (raw id).
    /// Shape (ID ()) is in the vocabulary.
    public static MemorySegment defaultTokenizingCharacterSet() {
        ensureInit();
        return ObjC.msgSendId(ObjC.cls("NSTokenField"), ObjC.sel("defaultTokenizingCharacterSet"));
    }
}
