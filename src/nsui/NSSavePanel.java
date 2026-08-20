package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * NSSavePanel — native save dialog. Thin 1:1 wrapper over AppKit NSSavePanel.
 * NSOpenPanel is a subclass; both share the same base selectors.
 */
public class NSSavePanel extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hInt;      // (id,SEL)->long [runModal]
    private static MethodHandle hId;       // (id,SEL)->id [directoryURL / URL]
    private static MethodHandle hBool;     // (id,SEL)->bool
    private static MethodHandle hSetBool;  // (id,SEL,bool)->void
    private static MethodHandle hSetId;    // (id,SEL,id)->void helper via ObjC.msgSendVoidId
    private static MethodHandle hSetAllowed; // (id,SEL,id)->void [setAllowedFileTypes: / setAllowedContentTypes:]

    protected NSSavePanel(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    protected static synchronized void ensureInit() {
        if (initialized) return;
        hInt = ObjC.handle(Sig.of(Ret.INT));
        hId = ObjC.handle(Sig.of(Ret.ID));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hSetId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hSetAllowed = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    public static NSSavePanel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSSavePanel(peer);
    }

    /** +[NSSavePanel savePanel] */
    public static NSSavePanel savePanel() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSSavePanel"), ObjC.sel("savePanel"));
        return wrap(p);
    }

    // ---- properties ----
    public boolean canCreateDirectories() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("canCreateDirectories")); } catch (Throwable t) { throw new RuntimeException("canCreateDirectories failed", t); }
    }
    public void setCanCreateDirectories(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setCanCreateDirectories:"), flag); } catch (Throwable t) { throw new RuntimeException("setCanCreateDirectories: failed", t); }
    }

    public boolean showsHiddenFiles() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("showsHiddenFiles")); } catch (Throwable t) { throw new RuntimeException("showsHiddenFiles failed", t); }
    }
    public void setShowsHiddenFiles(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setShowsHiddenFiles:"), flag); } catch (Throwable t) { throw new RuntimeException("setShowsHiddenFiles: failed", t); }
    }

    public boolean isExtensionHidden() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isExtensionHidden")); } catch (Throwable t) { throw new RuntimeException("isExtensionHidden failed", t); }
    }
    public void setExtensionHidden(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setExtensionHidden:"), flag); } catch (Throwable t) { throw new RuntimeException("setExtensionHidden: failed", t); }
    }

    public boolean allowsOtherFileTypes() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("allowsOtherFileTypes")); } catch (Throwable t) { throw new RuntimeException("allowsOtherFileTypes failed", t); }
    }
    public void setAllowsOtherFileTypes(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setAllowsOtherFileTypes:"), flag); } catch (Throwable t) { throw new RuntimeException("setAllowsOtherFileTypes: failed", t); }
    }

    // ---- allowedFileTypes ----
    public void setAllowedFileTypes(java.util.List<String> types) {
        ensureInit();
        MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
        if (types != null) {
            for (String t : types) {
                ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ObjC.nsstring(t));
            }
        }
        try { hSetAllowed.invokeExact(peer, ObjC.sel("setAllowedFileTypes:"), arr); } catch (Throwable e) { throw new RuntimeException("setAllowedFileTypes: failed", e); }
    }
    public void setAllowedFileTypes(String... types) {
        setAllowedFileTypes(types == null ? java.util.List.of() : java.util.List.of(types));
    }
    public MemorySegment allowedFileTypesId() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("allowedFileTypes")); } catch (Throwable t) { throw new RuntimeException("allowedFileTypes failed", t); }
    }

    // ---- directoryURL / URL ----
    public MemorySegment directoryURL() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("directoryURL")); } catch (Throwable t) { throw new RuntimeException("directoryURL failed", t); }
    }
    public void setDirectoryURL(MemorySegment url) {
        ensureInit();
        try { hSetId.invokeExact(peer, ObjC.sel("setDirectoryURL:"), (MemorySegment) ((MemorySegment) (url == null ? MemorySegment.NULL : url))); } catch (Throwable t) { throw new RuntimeException("setDirectoryURL: failed", t); }
    }
    public void setDirectoryURL(String path) {
        MemorySegment url = null;
        if (path != null) {
            url = ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("fileURLWithPath:"), ObjC.nsstring(path));
        }
        setDirectoryURL(url);
    }
    public String directoryURLString() {
        MemorySegment url = directoryURL();
        if (url == null || url.address() == 0) return null;
        MemorySegment s = ObjC.msgSendId(url, ObjC.sel("path"));
        return ObjC.toString(s);
    }

    public MemorySegment URL() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("URL")); } catch (Throwable t) { throw new RuntimeException("URL failed", t); }
    }
    public String URLString() {
        MemorySegment url = URL();
        if (url == null || url.address() == 0) return null;
        MemorySegment s = ObjC.msgSendId(url, ObjC.sel("path"));
        return ObjC.toString(s);
    }

    // ---- nameFieldStringValue ----
    public String nameFieldStringValue() {
        ensureInit();
        try {
            MemorySegment s = (MemorySegment) hId.invokeExact(peer, ObjC.sel("nameFieldStringValue"));
            return ObjC.toString(s);
        } catch (Throwable t) { throw new RuntimeException("nameFieldStringValue failed", t); }
    }
    public void setNameFieldStringValue(String v) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setNameFieldStringValue:"), ObjC.nsstring(v));
    }

    // ---- title / message ----
    public String title() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("title")));
    }
    public void setTitle(String t) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTitle:"), ObjC.nsstring(t));
    }
    public String message() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("message")));
    }
    public void setMessage(String m) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMessage:"), ObjC.nsstring(m));
    }

    // ---- runModal ----
    public long runModal() {
        ensureInit();
        try { return (long) hInt.invokeExact(peer, ObjC.sel("runModal")); } catch (Throwable t) { throw new RuntimeException("runModal failed", t); }
    }

    // ---- beginSheetModalForWindow:completionHandler: ----
    public void beginSheetModalForWindow(NSWindow window, MemorySegment completionHandler) {
        ensureInit();
        MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.ID, Arg.ID));
        try {
            MemorySegment winPeer = (window == null) ? MemorySegment.NULL : window.peer();
            MemorySegment handler = (completionHandler == null) ? MemorySegment.NULL : completionHandler;
            h.invokeExact(peer, ObjC.sel("beginSheetModalForWindow:completionHandler:"), winPeer, handler);
        } catch (Throwable t) { throw new RuntimeException("beginSheetModalForWindow:completionHandler: failed", t); }
    }
    public void beginSheetModalForWindow(NSWindow window) {
        beginSheetModalForWindow(window, MemorySegment.NULL);
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

    // ---- URLs helper for subclasses (returns NSArray id) ----
    protected java.util.List<MemorySegment> urlsAsArray() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) hId.invokeExact(peer, ObjC.sel("URLs"));
            if (arr == null || arr.address() == 0) return java.util.List.of();
            long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
            java.util.List<MemorySegment> out = new java.util.ArrayList<>((int) count);
            MethodHandle hAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            for (long i = 0; i < count; i++) {
                MemorySegment u = (MemorySegment) hAt.invokeExact(arr, ObjC.sel("objectAtIndex:"), i);
                if (u != null && u.address() != 0) out.add(u);
            }
            return java.util.Collections.unmodifiableList(out);
        } catch (Throwable t) { throw new RuntimeException("URLs failed", t); }
    }
}
