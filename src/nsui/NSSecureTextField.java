package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSecureTextField — an AppKit secure (password) text field control.
/// Thin 1:1 wrapper over a native `NSSecureTextField`: every method maps
/// to one `objc_msgSend` selector, no cached Java state beyond the peer.
/// Mirrors the native hierarchy: NSSecureTextField is an NSTextField is an NSControl is an NSView.
public class NSSecureTextField extends NSTextField {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hResponds;    // (id, SEL, id) -> bool [respondsToSelector:]

    private NSSecureTextField(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hResponds = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
        initialized = true;
    }

    /// `[[NSSecureTextField alloc] initWithFrame:frame]` — a new secure field at the given rect.
    public static NSSecureTextField create(NSRect frame) {
        ensureInit();
        MemorySegment f = ObjC.msgSendId(ObjC.cls("NSSecureTextField"), ObjC.sel("alloc"));
        try {
            f = (MemorySegment) hInitFrame.invokeExact(f, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSSecureTextField", t);
        }
        if (f.address() == 0) {
            throw new IllegalStateException("NSSecureTextField alloc/initWithFrame: returned nil");
        }
        return new NSSecureTextField(f);
    }

    // ---------------------------------------------------------------- instance API

    // ---- stringValue already in NSControl, re-expose for discoverability ----
    @Override
    public String stringValue() { return super.stringValue(); }
    @Override
    public void setStringValue(String value) { super.setStringValue(value); }

    /// [field echosBullets] — whether the field echoes bullets instead of the actual text (via its cell).
    public boolean echosBullets() {
        MemorySegment cell = ObjC.msgSendId(peer, ObjC.sel("cell"));
        if (cell == null || cell.address() == 0) return false;
        ensureInit();
        try {
            boolean responds = (boolean) hResponds.invokeExact(cell, ObjC.sel("respondsToSelector:"), ObjC.sel("echosBullets"));
            if (!responds) return false;
        } catch (Throwable t) {
            return false;
        }
        return ObjC.msgSendBool(cell, ObjC.sel("echosBullets"));
    }

    /// Alias for `echosBullets` — Java-bean is-prefix.
    public boolean isEchosBullets() {
        return echosBullets();
    }

    /// [field setEchosBullets:] — set whether bullets are echoed (via its cell).
    public void setEchosBullets(boolean flag) {
        MemorySegment cell = ObjC.msgSendId(peer, ObjC.sel("cell"));
        if (cell == null || cell.address() == 0) return;
        ensureInit();
        try {
            boolean responds = (boolean) hResponds.invokeExact(cell, ObjC.sel("respondsToSelector:"), ObjC.sel("setEchosBullets:"));
            if (!responds) return;
        } catch (Throwable t) {
            return;
        }
        ObjC.msgSendVoidBool(cell, ObjC.sel("setEchosBullets:"), flag);
    }
}
