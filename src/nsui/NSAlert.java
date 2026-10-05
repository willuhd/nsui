package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSAlert — a native alert panel.
/// Thin 1:1 wrapper over AppKit NSAlert.
public final class NSAlert extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hAddButton;   // (id,SEL,id)->id [addButtonWithTitle:]
    private static MethodHandle hInt;         // (id,SEL)->long [runModal / alertStyle]
    private static MethodHandle hSetInt;      // (id,SEL,long)->void [setAlertStyle:]
    private static MethodHandle hId;          // (id,SEL)->id [icon / suppressionButton]
    private static MethodHandle hBool;        // (id,SEL)->bool [showsHelp / showsSuppressionButton]
    private static MethodHandle hSetBool;     // (id,SEL,bool)->void [setShowsHelp: / setShowsSuppressionButton:]
    private static MethodHandle hBeginSheet;  // (id,SEL,id,id)->void [beginSheetModalForWindow:completionHandler:]

    private NSAlert(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hAddButton = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
        hInt = ObjC.handle(Sig.of(Ret.INT));
        hSetInt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hId = ObjC.handle(Sig.of(Ret.ID));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hBeginSheet = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
        initialized = true;
    }

    public static NSAlert wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSAlert(peer);
    }

    /// alloc + init.
    public static NSAlert create() {
        ensureInit();
        MemorySegment m = ObjC.msgSendId(ObjC.cls("NSAlert"), ObjC.sel("alloc"));
        return new NSAlert(ObjC.msgSendId(m, ObjC.sel("init")));
    }

    // ---- messageText ----
    public String messageText() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("messageText")));
    }
    public void setMessageText(String text) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMessageText:"), ObjC.nsstring(text));
    }

    // ---- informativeText ----
    public String informativeText() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("informativeText")));
    }
    public void setInformativeText(String text) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setInformativeText:"), ObjC.nsstring(text));
    }

    // ---------------------------------------------------------------- nested enum — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSAlert.h
    //   NSAlertStyle: Warning 0, Informational 1, Critical 2
    // Docs: https://developer.apple.com/documentation/appkit/nsalert/style

    /// `NSAlertStyle` — 0=Warning, 1=Informational, 2=Critical. From `NSAlert.h`.
    public enum Style {
        warning(0), informational(1), critical(2);
        public final long value;
        Style(long v) { this.value = v; }
        public static Style fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    // ---- alertStyle ----
    public long alertStyle() {
        ensureInit();
        try { return (long) hInt.invokeExact(peer, ObjC.sel("alertStyle")); } catch (Throwable t) { throw new RuntimeException("alertStyle failed", t); }
    }
    /// Typed getter.
    public Style alertStyleEnum() { return Style.fromValue(alertStyle()); }
    public void setAlertStyle(long style) {
        ensureInit();
        try { hSetInt.invokeExact(peer, ObjC.sel("setAlertStyle:"), style); } catch (Throwable t) { throw new RuntimeException("setAlertStyle: failed", t); }
    }
    /// Typed overload.
    public void setAlertStyle(Style s) { setAlertStyle(s.value); }

    // ---- addButtonWithTitle: ----
    public NSButton addButtonWithTitle(String title) {
        ensureInit();
        try {
            MemorySegment btn = (MemorySegment) hAddButton.invokeExact(peer, ObjC.sel("addButtonWithTitle:"), ObjC.nsstring(title));
            return NSButton.wrap(btn);
        } catch (Throwable t) {
            throw new RuntimeException("addButtonWithTitle: failed", t);
        }
    }

    // ---- runModal ----
    public long runModal() {
        ensureInit();
        try { return (long) hInt.invokeExact(peer, ObjC.sel("runModal")); } catch (Throwable t) { throw new RuntimeException("runModal failed", t); }
    }

    // ---- beginSheetModalForWindow:completionHandler: ----
    public void beginSheetModalForWindow(NSWindow window, MemorySegment completionHandler) {
        ensureInit();
        try {
            MemorySegment winPeer = (window == null) ? MemorySegment.NULL : window.peer();
            MemorySegment handler = (completionHandler == null) ? MemorySegment.NULL : completionHandler;
            hBeginSheet.invokeExact(peer, ObjC.sel("beginSheetModalForWindow:completionHandler:"), winPeer, handler);
        } catch (Throwable t) {
            throw new RuntimeException("beginSheetModalForWindow:completionHandler: failed", t);
        }
    }
    public void beginSheetModalForWindow(NSWindow window) {
        beginSheetModalForWindow(window, MemorySegment.NULL);
    }

    // ---- icon ----
    public NSImage icon() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hId.invokeExact(peer, ObjC.sel("icon"));
            return NSImage.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("icon failed", t); }
    }
    public void setIcon(NSImage image) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setIcon:"), (MemorySegment) (image == null ? MemorySegment.NULL : image.peer()));
    }

    // ---- showsHelp ----
    public boolean showsHelp() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("showsHelp")); } catch (Throwable t) { throw new RuntimeException("showsHelp failed", t); }
    }
    public void setShowsHelp(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setShowsHelp:"), flag); } catch (Throwable t) { throw new RuntimeException("setShowsHelp: failed", t); }
    }

    // ---- suppressionButton ----
    public NSButton suppressionButton() {
        ensureInit();
        try {
            MemorySegment btn = (MemorySegment) hId.invokeExact(peer, ObjC.sel("suppressionButton"));
            return NSButton.wrap(btn);
        } catch (Throwable t) { throw new RuntimeException("suppressionButton failed", t); }
    }
    public boolean showsSuppressionButton() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("showsSuppressionButton")); } catch (Throwable t) { throw new RuntimeException("showsSuppressionButton failed", t); }
    }
    public void setShowsSuppressionButton(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setShowsSuppressionButton:"), flag); } catch (Throwable t) { throw new RuntimeException("setShowsSuppressionButton: failed", t); }
    }

    // ---- accessoryView ----
    public NSView accessoryView() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hId.invokeExact(peer, ObjC.sel("accessoryView"));
            return NSView.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("accessoryView failed", t); }
    }
    public void setAccessoryView(NSView view) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAccessoryView:"), (MemorySegment) (view == null ? MemorySegment.NULL : view.peer()));
    }

    // ---- window (the alert's panel) ----
    public NSWindow window() {
        ensureInit();
        try {
            MemorySegment p = (MemorySegment) hId.invokeExact(peer, ObjC.sel("window"));
            return NSWindow.wrap(p);
        } catch (Throwable t) { throw new RuntimeException("window failed", t); }
    }

    // ---- alertWithError: (+factory, NSError* as id) ----
    /// +alertWithError: — alert initialized from an NSError (peer as id; nil-safe).
    public static NSAlert alertWithError(MemorySegment error) {
        ensureInit();
        MemorySegment p = ObjC.msgSendIdId(ObjC.cls("NSAlert"), ObjC.sel("alertWithError:"),
                (MemorySegment) (error == null ? MemorySegment.NULL : error));
        return wrap(p);
    }

    // ---- buttons (readonly NSArray<NSButton*>) ----
    /// buttons — response buttons in add order (excludes implicit OK when none added).
    public java.util.List<NSButton> buttons() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) hId.invokeExact(peer, ObjC.sel("buttons"));
            if (arr == null || arr.address() == 0) return java.util.List.of();
            long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
            java.util.List<NSButton> out = new java.util.ArrayList<>((int) count);
            java.lang.invoke.MethodHandle hAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            for (long i = 0; i < count; i++) {
                MemorySegment b = (MemorySegment) hAt.invokeExact(arr, ObjC.sel("objectAtIndex:"), i);
                NSButton w = NSButton.wrap(b);
                if (w != null) out.add(w);
            }
            return java.util.Collections.unmodifiableList(out);
        } catch (Throwable t) { throw new RuntimeException("buttons failed", t); }
    }

    /// buttonsPeer — raw NSArray id (nil-safe, may be NULL).
    public MemorySegment buttonsPeer() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("buttons")); }
        catch (Throwable t) { throw new RuntimeException("buttons failed", t); }
    }

    // ---- helpAnchor (NSHelpAnchorName NSString) ----
    /// helpAnchor — HTML help anchor used with the help button (nil clears).
    public String helpAnchor() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("helpAnchor")));
    }
    /// setHelpAnchor:.
    public void setHelpAnchor(String anchor) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setHelpAnchor:"),
                anchor == null ? MemorySegment.NULL : ObjC.nsstring(anchor));
    }

    // ---- delegate (id<NSAlertDelegate>, weak) ----
    /// delegate — alert delegate peer (currently only custom help behavior).
    public MemorySegment delegatePeer() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("delegate")); }
        catch (Throwable t) { throw new RuntimeException("delegate failed", t); }
    }
    /// setDelegate:.
    public void setDelegate(MemorySegment delegate) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"),
                (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate));
    }
    /// setDelegate: with NSObject.
    public void setDelegate(NSObject delegate) {
        setDelegate(delegate == null ? MemorySegment.NULL : delegate.peer());
    }

    // ---- layout ----
    /// layout — force immediate layout instead of lazily before display.
    public void layout() {
        ObjC.msgSendVoid(peer, ObjC.sel("layout"));
    }

    // ---------------------------------------------------------------- modal-response constants — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSAlert.h
    //   NSAlertFirstButtonReturn 1000, NSAlertSecondButtonReturn 1001, NSAlertThirdButtonReturn 1002
    //   (buttonPosition 3+x = NSAlertThirdButtonReturn + x; no buttons -> implicit OK returns 0)
    // Docs: https://developer.apple.com/documentation/appkit/nsalert
    /// NSAlertFirstButtonReturn — runModal/sheet return for the first (rightmost) button.
    public static final long FIRST_BUTTON_RETURN = 1000L;
    /// NSAlertSecondButtonReturn.
    public static final long SECOND_BUTTON_RETURN = 1001L;
    /// NSAlertThirdButtonReturn (buttonPosition 3+x = this + x).
    public static final long THIRD_BUTTON_RETURN = 1002L;

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - +alertWithMessageText:defaultButton:alternateButton:otherButton:informativeTextWithFormat: (deprecated 10.3-10.10,
    //   variadic format) — omitted: variadic C call has no registered shape; use create() + setters.
    // - -beginSheetModalForWindow:modalDelegate:didEndSelector:contextInfo: (deprecated 10.3-10.10, selector+void*)
    //   — omitted: deprecated and needs (id,SEL,void*) plumbing with no registered shape; use
    //   beginSheetModalForWindow:completionHandler:.
    // - NSAlertDelegate.alertShowHelp: — delegate protocol method, not an NSAlert selector; wire via DelegateProxy
    //   BoolArg if needed (no new upcall shape introduced here).
}
