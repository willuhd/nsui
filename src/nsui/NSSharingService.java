package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSharingService — one share destination (Mail, Messages, AirDrop...):
/// titles, message fields, and performing with items. Thin stateless wrapper.
/// (Custom initWithTitle:image:... takes a block handler — omitted; use the
/// named system services. Delegate omitted: delivery is fire-and-forget here.)
public final class NSSharingService extends NSObject {

    private record Handles(MethodHandle hCanPerform) {}
    private static volatile Handles handles;

    private NSSharingService(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)));
    }

    /// Wrap an existing peer.
    public static NSSharingService wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSharingService(peer);
    }

    /// sharingServiceNamed: (e.g. "com.apple.share.Mail.compose").
    public static NSSharingService named(String serviceName) {
        return wrap(ObjC.msgSendIdId(ObjC.cls("NSSharingService"),
                ObjC.sel("sharingServiceNamed:"), ObjC.nsstring(serviceName)));
    }

    /// title (nil-safe).
    public String title() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("title")));
    }

    /// image (nil-safe raw peer).
    public MemorySegment image() {
        return ObjC.msgSendId(peer, ObjC.sel("image"));
    }

    /// menuItemTitle (nil-safe) / setter.
    public String menuItemTitle() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("menuItemTitle")));
    }

    /// setMenuItemTitle:.
    public void setMenuItemTitle(String title) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMenuItemTitle:"), ObjC.nsstring(title));
    }

    /// subject (nil-safe) / setter.
    public String subject() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("subject")));
    }

    /// setSubject:.
    public void setSubject(String subject) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setSubject:"),
                subject == null ? MemorySegment.NULL : ObjC.nsstring(subject));
    }

    /// recipients (nil-safe raw peer).
    public MemorySegment recipients() {
        return ObjC.msgSendId(peer, ObjC.sel("recipients"));
    }

    /// canPerformWithItems: — synchronous eligibility probe (no UI).
    public boolean canPerform(NSArray items) {
        try {
            return (boolean) handles.hCanPerform().invokeExact(peer, ObjC.sel("canPerformWithItems:"),
                    (MemorySegment) (items == null ? MemorySegment.NULL : items.peer()));
        } catch (Throwable t) {
            throw new RuntimeException("canPerformWithItems: failed", t);
        }
    }

    /// performWithItems: — performs the share (may present UI).
    public void perform(NSArray items) {
        ObjC.msgSendVoidId(peer, ObjC.sel("performWithItems:"),
                (MemorySegment) (items == null ? MemorySegment.NULL : items.peer()));
    }
}
