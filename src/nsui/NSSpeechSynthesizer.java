package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSpeechSynthesizer — text-to-speech. Thin stateless wrapper; speech is
/// asynchronous (calls return immediately, delegate omitted).
public final class NSSpeechSynthesizer extends NSObject {

    private record Handles(MethodHandle hInitVoice, MethodHandle hSpeak) {}
    private static volatile Handles handles;

    private NSSpeechSynthesizer(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing peer.
    public static NSSpeechSynthesizer wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSpeechSynthesizer(peer);
    }

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.ID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
    }

    private static NSSpeechSynthesizer initWithVoice(MemorySegment voiceName) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSSpeechSynthesizer"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) handles.hInitVoice().invokeExact(p, ObjC.sel("initWithVoice:"), voiceName);
        } catch (Throwable t) {
            throw new RuntimeException("initWithVoice: failed for NSSpeechSynthesizer", t);
        }
        if (p.address() == 0) throw new IllegalStateException("initWithVoice: returned nil for NSSpeechSynthesizer");
        return new NSSpeechSynthesizer(p);
    }

    /// Default voice.
    public static NSSpeechSynthesizer create() {
        return initWithVoice(MemorySegment.NULL);
    }

    /// Named voice.
    public static NSSpeechSynthesizer create(String voice) {
        return initWithVoice(ObjC.nsstring(voice));
    }

    /// startSpeakingString: — speaks asynchronously.
    public boolean startSpeakingString(String text) {
        try {
            return (boolean) handles.hSpeak().invokeExact(peer, ObjC.sel("startSpeakingString:"), ObjC.nsstring(text));
        } catch (Throwable t) {
            throw new RuntimeException("startSpeakingString: failed", t);
        }
    }

    /// stopSpeaking.
    public void stopSpeaking() {
        ObjC.msgSendVoid(peer, ObjC.sel("stopSpeaking"));
    }

    /// isSpeaking.
    public boolean isSpeaking() {
        return ObjC.msgSendBool(peer, ObjC.sel("isSpeaking"));
    }

    /// voice name (nil-safe).
    public String voice() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("voice")));
    }

    /// setVoice: -- returns the native BOOL (NO if the voice is unavailable).
    /// nil selects the default voice.
    public boolean setVoice(String voice) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(peer, ObjC.sel("setVoice:"),
                    (MemorySegment) (voice == null ? MemorySegment.NULL : ObjC.nsstring(voice)));
        } catch (Throwable t) { throw new RuntimeException("setVoice: failed", t); }
    }
}
