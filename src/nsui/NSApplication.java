package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.ObjC;

/// NSApplication — the app shell (SWT Display-equivalent). Owns the run loop,
/// activation policy, main menu, and event dispatch.
public final class NSApplication extends NSObject {

    private static NSApplication shared;

    private NSApplication(MemorySegment peer) {
        super(peer);
    }

    /// [NSApplication sharedApplication] — singleton.
    public static NSApplication shared() {
        if (shared == null) {
            shared = new NSApplication(ObjC.msgSendId(ObjC.cls("NSApplication"), ObjC.sel("sharedApplication")));
        }
        return shared;
    }

    public void setActivationPolicy(long policy) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setActivationPolicy:"), policy);
    }

    public long activationPolicy() {
        return ObjC.msgSendLong(peer, ObjC.sel("activationPolicy"));
    }

    public void activateIgnoringOtherApps(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("activateIgnoringOtherApps:"), flag);
    }

    public void activate() {
        ObjC.msgSendVoid(peer, ObjC.sel("activate"));
    }

    public void deactivate() {
        ObjC.msgSendVoid(peer, ObjC.sel("deactivate"));
    }

    public void hide(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("hide:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    public void unhide(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("unhide:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    public boolean isActive() { return ObjC.msgSendBool(peer, ObjC.sel("isActive")); }
    public boolean isHidden() { return ObjC.msgSendBool(peer, ObjC.sel("isHidden")); }
    public boolean isRunning() { return ObjC.msgSendBool(peer, ObjC.sel("isRunning")); }

    public MemorySegment mainWindow() {
        MemorySegment w = ObjC.msgSendId(peer, ObjC.sel("mainWindow"));
        return w;
    }

    public MemorySegment keyWindow() {
        MemorySegment w = ObjC.msgSendId(peer, ObjC.sel("keyWindow"));
        return w;
    }

    public MemorySegment windows() {
        return ObjC.msgSendId(peer, ObjC.sel("windows"));
    }

    public MemorySegment modalWindow() {
        return ObjC.msgSendId(peer, ObjC.sel("modalWindow"));
    }

    public void finishLaunching() {
        ObjC.msgSendVoid(peer, ObjC.sel("finishLaunching"));
    }

    public void setDelegate(NSObject delegate) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"), delegate.peer());
    }

    public void setMainMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMainMenu:"), menu.peer());
    }

    public MemorySegment mainMenu() {
        return ObjC.msgSendId(peer, ObjC.sel("mainMenu"));
    }

    public void setHelpMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setHelpMenu:"), menu.peer());
    }

    public MemorySegment helpMenu() {
        return ObjC.msgSendId(peer, ObjC.sel("helpMenu"));
    }

    public void setApplicationIconImage(NSImage image) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setApplicationIconImage:"), (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()));
    }

    public MemorySegment applicationIconImage() {
        return ObjC.msgSendId(peer, ObjC.sel("applicationIconImage"));
    }

    /// Blocking: runs the AppKit run loop on this thread. Returns when the app terminates.
    public void run() {
        ObjC.msgSendVoid(peer, ObjC.sel("run"));
    }

    public void terminate(NSObject sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("terminate:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender.peer()));
    }

    public void stop(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("stop:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ------------------------------------------------------- event dispatch

    /// Cached NSStrings per run-loop mode (plain Java map; nsstring created lazily at runtime).
    private static final java.util.concurrent.ConcurrentHashMap<String, MemorySegment> MODE_NS = new java.util.concurrent.ConcurrentHashMap<>();

    /// nextEventMatchingMask:untilDate:inMode:dequeue: — the run-loop turn primitive.
    public NSEvent nextEvent(long mask, MemorySegment untilDate, String mode, boolean dequeue) {
        MemorySegment modeSeg = MODE_NS.computeIfAbsent(mode, ObjC::nsstring);
        MemorySegment ev = ObjC.msgSendIdLongIdIdBool(peer,
                ObjC.sel("nextEventMatchingMask:untilDate:inMode:dequeue:"),
                mask, untilDate, modeSeg, dequeue);
        return ev.address() == 0 ? null : new NSEvent(ev);
    }

    public void sendEvent(NSEvent event) {
        ObjC.msgSendVoidId(peer, ObjC.sel("sendEvent:"), event.peer());
    }

    public void postEvent(NSEvent event, boolean atStart) {
        try {
            var h = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.VOID, nsui.objc.Sig.Arg.ID, nsui.objc.Sig.Arg.BOOL));
            h.invokeExact(peer, ObjC.sel("postEvent:atStart:"), event.peer(), atStart);
        } catch (Throwable t) { throw new RuntimeException("postEvent:atStart: failed", t); }
    }

    public NSEvent currentEvent() {
        MemorySegment ev = ObjC.msgSendId(peer, ObjC.sel("currentEvent"));
        return ev.address() == 0 ? null : new NSEvent(ev);
    }

    public void discardEventsMatchingMask(long mask, NSEvent beforeEvent) {
        // Not simply represented - use handle
        try {
            var h = ObjC.handle(nsui.objc.Sig.of(nsui.objc.Sig.Ret.VOID, nsui.objc.Sig.Arg.INT, nsui.objc.Sig.Arg.ID));
            h.invokeExact(peer, ObjC.sel("discardEventsMatchingMask:beforeEvent:"), mask, (MemorySegment) (beforeEvent == null ? MemorySegment.NULL : beforeEvent.peer()));
        } catch (Throwable t) { throw new RuntimeException("discardEventsMatchingMask:beforeEvent: failed", t); }
    }

    public void updateWindows() {
        ObjC.msgSendVoid(peer, ObjC.sel("updateWindows"));
    }

    public MemorySegment dockTile() {
        return ObjC.msgSendId(peer, ObjC.sel("dockTile"));
    }

    public long presentationOptions() {
        return ObjC.msgSendLong(peer, ObjC.sel("presentationOptions"));
    }

    public boolean isFullKeyboardAccessEnabled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isFullKeyboardAccessEnabled"));
    }
}
