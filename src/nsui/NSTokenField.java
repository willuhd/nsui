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
/// OMITTED: the NSTokenFieldDelegate protocol (completionsForSubstring:,
/// styleForRepresentedObject:, ... and tokenizingCharacterSet) — delegate
/// callbacks need upcall delegate-proxy machinery, out of scope here.
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

    private static synchronized void ensureInit() {
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
}
