package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSSound — named system sounds and short audio playback. Thin stateless
/// wrapper; playback is asynchronous (the call returns immediately).
public final class NSSound extends NSObject {

    private NSSound(MemorySegment peer) {
        super(peer);
    }

    /// Wrap an existing peer.
    public static NSSound wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSound(peer);
    }

    /// soundNamed: — a system sound by name (e.g. "Funk", "Glass", "Hero").
    /// Nil when the name is unknown on this system.
    public static NSSound soundNamed(String name) {
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSSound"), ObjC.sel("soundNamed:"), ObjC.nsstring(name)));
    }

    /// name (nil-safe).
    public String name() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("name")));
    }

    /// play — starts async playback.
    public boolean play() {
        return ObjC.msgSendBool(peer, ObjC.sel("play"));
    }

    /// pause.
    public boolean pause() {
        return ObjC.msgSendBool(peer, ObjC.sel("pause"));
    }

    /// resume.
    public boolean resume() {
        return ObjC.msgSendBool(peer, ObjC.sel("resume"));
    }

    /// stop.
    public void stop() {
        ObjC.msgSendVoid(peer, ObjC.sel("stop"));
    }

    /// isPlaying.
    public boolean isPlaying() {
        return ObjC.msgSendBool(peer, ObjC.sel("isPlaying"));
    }
}
