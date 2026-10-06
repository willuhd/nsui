package nsui;

import java.lang.foreign.MemorySegment;

import nsui.objc.Autorelease;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSApplication — the app shell (SWT Display-equivalent). Owns the run loop,
/// activation policy, main menu, and event dispatch.
/// OMITTED: sendAction:to:from: (shape BOOL,ID,ID,ID NOT in vocabulary — verified
/// by grep); enumerateWindowsWithOptions:usingBlock: (block, no registered shape);
/// addWindowsItem:/changeWindowsItem: (shape VOID,ID,ID,BOOL NOT in vocabulary);
/// deprecated beginSheet/endSheet/makeWindowsPerform/context; NSApplicationDelegate
/// protocol methods (use DelegateProxy, not this wrapper).
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

    /// setActivationPolicy: -- returns the native BOOL (YES on success). The old
    /// void overload discarded it, so a rejected policy looked like success.
    public boolean setActivationPolicy(long policy) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.INT)).invokeExact(peer,
                    ObjC.sel("setActivationPolicy:"), policy);
        } catch (Throwable t) { throw new RuntimeException("setActivationPolicy: failed", t); }
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
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"),
                delegate == null ? MemorySegment.NULL : delegate.peer());
    }

    /// [application delegate] — the app delegate (id, may be nil).
    public MemorySegment delegate() {
        return ObjC.msgSendId(peer, ObjC.sel("delegate"));
    }

    /// Typed delegate wrapper.
    public NSObject delegateObject() {
        MemorySegment d = ObjC.msgSendId(peer, ObjC.sel("delegate"));
        return NSObject.wrap(d);
    }

    public void setMainMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMainMenu:"),
                menu == null ? MemorySegment.NULL : menu.peer());
    }

    public MemorySegment mainMenu() {
        return ObjC.msgSendId(peer, ObjC.sel("mainMenu"));
    }

    public void setHelpMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setHelpMenu:"),
                menu == null ? MemorySegment.NULL : menu.peer());
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

    /// nextEventMatchingMask:untilDate:inMode:dequeue: — the run-loop turn primitive.
    /// Mode string resolved per call (not cached): nsstring is autoreleased, so
    /// holding its peer across pool drains would dangle; per-call cost is trivial.
    public NSEvent nextEvent(long mask, MemorySegment untilDate, String mode, boolean dequeue) {
        MemorySegment modeSeg = ObjC.nsstring(mode);
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

    /// Pump the main run loop for {@code millis} ms: the time-boxed form of
    /// [NSApp run] — pull events, dispatch, flush drawing, repeat until the
    /// deadline. Production code lets run() own the thread forever; this is
    /// for tests, previews, and embedding, where the caller must get control
    /// back. Never blocks past the deadline; each turn waits at most 50 ms.
    public void pumpFor(long millis) throws InterruptedException {
        MemorySegment dateCls = ObjC.cls("NSDate");
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            // Per-turn pool: this loop creates autoreleased NSDate/NSEvent objects
            // and Java owns no AppKit pool here (production run() has its own).
            MemorySegment pool = Autorelease.push();
            try {
                MemorySegment until = ObjC.msgSendIdDouble(dateCls, ObjC.sel("dateWithTimeIntervalSinceNow:"), 0.05);
                NSEvent ev = nextEvent(-1L /* NSEventMaskAny */, until, "kCFRunLoopDefaultMode", true);
                if (ev != null) sendEvent(ev);
                updateWindows();
            } finally {
                Autorelease.pop(pool);
            }
            Thread.sleep(10);
        }
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

    public NSAppearance effectiveAppearance() { return NSAppearance.wrap(ObjC.msgSendId(peer, ObjC.sel("effectiveAppearance"))); }
    public void setAppearance(NSAppearance ap) { ObjC.msgSendVoidId(peer, ObjC.sel("setAppearance:"), ap==null?MemorySegment.NULL:ap.peer()); }

    /// Alias for {@link #setActivationPolicy(long)} kept for callers that adopted it.
    public boolean trySetActivationPolicy(long policy) {
        return setActivationPolicy(policy);
    }

    /// unhideWithoutActivation.
    public void unhideWithoutActivation() {
        ObjC.msgSendVoid(peer, ObjC.sel("unhideWithoutActivation"));
    }

    /// windowWithWindowNumber: (nil when absent).
    public NSWindow windowWithWindowNumber(long windowNum) {
        try {
            MemorySegment w = (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.INT)).invokeExact(peer,
                    ObjC.sel("windowWithWindowNumber:"), windowNum);
            return NSWindow.wrap(w);
        } catch (Throwable t) { throw new RuntimeException("windowWithWindowNumber: failed", t); }
    }

    /// hideOtherApplications: / unhideAllApplications:.
    public void hideOtherApplications(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("hideOtherApplications:"),
                (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public void unhideAllApplications(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("unhideAllApplications:"),
                (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    /// runModalForWindow: (NSModalResponse).
    public long runModalForWindow(NSWindow window) {
        try {
            return (long) ObjC.handle(Sig.of(Ret.INT, Arg.ID)).invokeExact(peer,
                    ObjC.sel("runModalForWindow:"),
                    (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()));
        } catch (Throwable t) { throw new RuntimeException("runModalForWindow: failed", t); }
    }

    /// stopModal / stopModalWithCode: / abortModal.
    public void stopModal() {
        ObjC.msgSendVoid(peer, ObjC.sel("stopModal"));
    }
    public void stopModalWithCode(long code) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("stopModalWithCode:"), code);
    }
    public void abortModal() {
        ObjC.msgSendVoid(peer, ObjC.sel("abortModal"));
    }

    /// beginModalSessionForWindow: (NSModalSession as generic id).
    public MemorySegment beginModalSessionForWindow(NSWindow window) {
        return ObjC.msgSendIdId(peer, ObjC.sel("beginModalSessionForWindow:"),
                (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()));
    }

    /// runModalSession: (NSModalSession as generic id).
    public long runModalSession(MemorySegment session) {
        try {
            return (long) ObjC.handle(Sig.of(Ret.INT, Arg.ID)).invokeExact(peer,
                    ObjC.sel("runModalSession:"), (MemorySegment) (session == null ? MemorySegment.NULL : session));
        } catch (Throwable t) { throw new RuntimeException("runModalSession: failed", t); }
    }

    /// endModalSession:.
    public void endModalSession(MemorySegment session) {
        ObjC.msgSendVoidId(peer, ObjC.sel("endModalSession:"),
                (MemorySegment) (session == null ? MemorySegment.NULL : session));
    }

    /// requestUserAttention: / cancelUserAttentionRequest:.
    public long requestUserAttention(long type) {
        try {
            return (long) ObjC.handle(Sig.of(Ret.INT, Arg.INT)).invokeExact(peer,
                    ObjC.sel("requestUserAttention:"), type);
        } catch (Throwable t) { throw new RuntimeException("requestUserAttention: failed", t); }
    }
    public void cancelUserAttentionRequest(long request) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("cancelUserAttentionRequest:"), request);
    }

    /// preventWindowOrdering / setWindowsNeedUpdate:.
    public void preventWindowOrdering() {
        ObjC.msgSendVoid(peer, ObjC.sel("preventWindowOrdering"));
    }
    public void setWindowsNeedUpdate(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setWindowsNeedUpdate:"), flag);
    }

    /// windowsMenu / servicesMenu.
    public NSMenu windowsMenu() {
        return NSMenu.wrap(ObjC.msgSendId(peer, ObjC.sel("windowsMenu")));
    }
    public void setWindowsMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setWindowsMenu:"),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
    }
    public NSMenu servicesMenu() {
        return NSMenu.wrap(ObjC.msgSendId(peer, ObjC.sel("servicesMenu")));
    }
    public void setServicesMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setServicesMenu:"),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
    }

    /// servicesProvider (generic id).
    public MemorySegment servicesProvider() {
        return ObjC.msgSendId(peer, ObjC.sel("servicesProvider"));
    }
    public void setServicesProvider(MemorySegment provider) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setServicesProvider:"),
                (MemorySegment) (provider == null ? MemorySegment.NULL : provider));
    }

    /// arrangeInFront: / miniaturizeAll: / removeWindowsItem: / updateWindowsItem:.
    public void arrangeInFront(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("arrangeInFront:"),
                (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public void miniaturizeAll(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("miniaturizeAll:"),
                (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public void removeWindowsItem(NSWindow window) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeWindowsItem:"),
                (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()));
    }
    public void updateWindowsItem(NSWindow window) {
        ObjC.msgSendVoidId(peer, ObjC.sel("updateWindowsItem:"),
                (MemorySegment) (window == null ? MemorySegment.NULL : window.peer()));
    }

    /// tryToPerform:with: (SEL as string).
    public boolean tryToPerform(String action, MemorySegment object) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("tryToPerform:with:"),
                    (MemorySegment) (action == null ? MemorySegment.NULL : ObjC.sel(action)),
                    (MemorySegment) (object == null ? MemorySegment.NULL : object));
        } catch (Throwable t) { throw new RuntimeException("tryToPerform:with: failed", t); }
    }

    /// targetForAction: (SEL as string).
    public MemorySegment targetForAction(String action) {
        try {
            return (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("targetForAction:"),
                    (MemorySegment) (action == null ? MemorySegment.NULL : ObjC.sel(action)));
        } catch (Throwable t) { throw new RuntimeException("targetForAction: failed", t); }
    }

    /// targetForAction:to:from:.
    public MemorySegment targetForActionToFrom(String action, MemorySegment target, MemorySegment sender) {
        try {
            return (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("targetForAction:to:from:"),
                    (MemorySegment) (action == null ? MemorySegment.NULL : ObjC.sel(action)),
                    (MemorySegment) (target == null ? MemorySegment.NULL : target),
                    (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
        } catch (Throwable t) { throw new RuntimeException("targetForAction:to:from: failed", t); }
    }

    /// validRequestorForSendType:returnType: (pasteboard types as strings).
    public MemorySegment validRequestorForSendType(String sendType, String returnType) {
        try {
            return (MemorySegment) ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID)).invokeExact(peer,
                    ObjC.sel("validRequestorForSendType:returnType:"),
                    (MemorySegment) (sendType == null ? MemorySegment.NULL : ObjC.nsstring(sendType)),
                    (MemorySegment) (returnType == null ? MemorySegment.NULL : ObjC.nsstring(returnType)));
        } catch (Throwable t) { throw new RuntimeException("validRequestorForSendType:returnType: failed", t); }
    }

    /// setPresentationOptions: / currentSystemPresentationOptions / occlusionState.
    public void setPresentationOptions(long options) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setPresentationOptions:"), options);
    }
    public long currentSystemPresentationOptions() {
        return ObjC.msgSendLong(peer, ObjC.sel("currentSystemPresentationOptions"));
    }
    public long occlusionState() {
        return ObjC.msgSendLong(peer, ObjC.sel("occlusionState"));
    }

    /// userInterfaceLayoutDirection.
    public long userInterfaceLayoutDirection() {
        return ObjC.msgSendLong(peer, ObjC.sel("userInterfaceLayoutDirection"));
    }

    /// reportException:.
    public void reportException(MemorySegment exception) {
        ObjC.msgSendVoidId(peer, ObjC.sel("reportException:"),
                (MemorySegment) (exception == null ? MemorySegment.NULL : exception));
    }

    /// detachDrawingThread:toTarget:withObject: (class).
    public static void detachDrawingThread(String selector, MemorySegment target, MemorySegment argument) {
        try {
            ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID)).invokeExact(
                    ObjC.cls("NSApplication"), ObjC.sel("detachDrawingThread:toTarget:withObject:"),
                    (MemorySegment) (selector == null ? MemorySegment.NULL : ObjC.sel(selector)),
                    (MemorySegment) (target == null ? MemorySegment.NULL : target),
                    (MemorySegment) (argument == null ? MemorySegment.NULL : argument));
        } catch (Throwable t) { throw new RuntimeException("detachDrawingThread:toTarget:withObject: failed", t); }
    }

    /// replyToApplicationShouldTerminate: / replyToOpenOrPrint:.
    public void replyToApplicationShouldTerminate(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("replyToApplicationShouldTerminate:"), flag);
    }
    public void replyToOpenOrPrint(long reply) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("replyToOpenOrPrint:"), reply);
    }

    /// orderFrontCharacterPalette: / orderFrontStandardAboutPanel: / WithOptions:.
    public void orderFrontCharacterPalette(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontCharacterPalette:"),
                (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public void orderFrontStandardAboutPanel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontStandardAboutPanel:"),
                (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public void orderFrontStandardAboutPanelWithOptions(NSDictionary options) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFrontStandardAboutPanelWithOptions:"),
                (MemorySegment) (options == null ? MemorySegment.NULL : options.peer()));
    }

    /// disableRelaunchOnLogin / enableRelaunchOnLogin.
    public void disableRelaunchOnLogin() {
        ObjC.msgSendVoid(peer, ObjC.sel("disableRelaunchOnLogin"));
    }
    public void enableRelaunchOnLogin() {
        ObjC.msgSendVoid(peer, ObjC.sel("enableRelaunchOnLogin"));
    }

    /// Remote notifications.
    public void registerForRemoteNotifications() {
        ObjC.msgSendVoid(peer, ObjC.sel("registerForRemoteNotifications"));
    }
    public void unregisterForRemoteNotifications() {
        ObjC.msgSendVoid(peer, ObjC.sel("unregisterForRemoteNotifications"));
    }
    public boolean isRegisteredForRemoteNotifications() {
        return ObjC.msgSendBool(peer, ObjC.sel("isRegisteredForRemoteNotifications"));
    }
    public long enabledRemoteNotificationTypes() {
        return ObjC.msgSendLong(peer, ObjC.sel("enabledRemoteNotificationTypes"));
    }
    public void registerForRemoteNotificationTypes(long types) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("registerForRemoteNotificationTypes:"), types);
    }
}
