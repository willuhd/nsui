package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSSavePanel — native save dialog. Thin 1:1 wrapper over AppKit NSSavePanel.
/// NSOpenPanel is a subclass; both share the same base selectors.
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

    /// +[NSSavePanel savePanel]
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

    // ---- identifier ----
    /// identifier — panel state persistence key (nil clears).
    public String identifier() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("identifier")));
    }
    /// setIdentifier:.
    public void setIdentifier(String identifier) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setIdentifier:"),
                identifier == null ? MemorySegment.NULL : ObjC.nsstring(identifier));
    }

    // ---- allowedContentTypes (NSArray<UTType*>; UTType has no wrapper — id peers) ----
    /// allowedContentTypesPeer — raw NSArray id (may be empty array, never nil in practice).
    public MemorySegment allowedContentTypesPeer() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("allowedContentTypes")); }
        catch (Throwable t) { throw new RuntimeException("allowedContentTypes failed", t); }
    }
    /// setAllowedContentTypes: with raw NSArray id.
    public void setAllowedContentTypes(MemorySegment contentTypes) {
        ensureInit();
        try { hSetAllowed.invokeExact(peer, ObjC.sel("setAllowedContentTypes:"), (MemorySegment) (contentTypes == null ? MemorySegment.NULL : contentTypes)); }
        catch (Throwable e) { throw new RuntimeException("setAllowedContentTypes: failed", e); }
    }
    /// setAllowedContentTypes: with UTType identifier strings (convenience; builds NSArray of UTTypes via +[UTType typeWithIdentifier:]).
    public void setAllowedContentTypes(java.util.List<String> typeIdentifiers) {
        ensureInit();
        try { ObjC.ensureFramework("UniformTypeIdentifiers"); } catch (Throwable ignore) {}
        MemorySegment arr = ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
        if (typeIdentifiers != null) {
            MemorySegment utCls = ObjC.cls("UTType");
            MemorySegment selType = ObjC.sel("typeWithIdentifier:");
            for (String t : typeIdentifiers) {
                if (t == null) continue;
                try {
                    MemorySegment ut = ObjC.msgSendIdId(utCls, selType, ObjC.nsstring(t));
                    if (ut != null && ut.address() != 0) ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ut);
                } catch (Throwable ignore) { ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ObjC.nsstring(t)); }
            }
        }
        setAllowedContentTypes(arr);
    }

    // ---- currentContentType (UTType*, macOS 15; id peer, nil resets) ----
    /// currentContentTypePeer — raw UTType id (nil when allowedContentTypes empty).
    public MemorySegment currentContentTypePeer() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("currentContentType")); }
        catch (Throwable t) { throw new RuntimeException("currentContentType failed", t); }
    }
    /// setCurrentContentType: with raw UTType id (nil resets to first allowed type).
    public void setCurrentContentType(MemorySegment type) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setCurrentContentType:"), (MemorySegment) (type == null ? MemorySegment.NULL : type));
    }

    // ---- delegate ----
    /// delegatePeer — panel delegate id (may be NULL).
    public MemorySegment delegatePeer() {
        ensureInit();
        try { return (MemorySegment) hId.invokeExact(peer, ObjC.sel("delegate")); }
        catch (Throwable t) { throw new RuntimeException("delegate failed", t); }
    }
    /// setDelegate:.
    public void setDelegate(MemorySegment delegate) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"), (MemorySegment) (delegate == null ? MemorySegment.NULL : delegate));
    }
    /// setDelegate: with NSObject.
    public void setDelegate(NSObject delegate) {
        setDelegate(delegate == null ? MemorySegment.NULL : delegate.peer());
    }

    // ---- expanded (readonly, getter isExpanded) ----
    /// isExpanded — YES when the save panel is expanded.
    public boolean isExpanded() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isExpanded")); } catch (Throwable t) { throw new RuntimeException("isExpanded failed", t); }
    }

    // ---- canSelectHiddenExtension ----
    /// canSelectHiddenExtension — show the Hide Extension menu item.
    public boolean canSelectHiddenExtension() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("canSelectHiddenExtension")); } catch (Throwable t) { throw new RuntimeException("canSelectHiddenExtension failed", t); }
    }
    public void setCanSelectHiddenExtension(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setCanSelectHiddenExtension:"), flag); } catch (Throwable t) { throw new RuntimeException("setCanSelectHiddenExtension: failed", t); }
    }

    // ---- treatsFilePackagesAsDirectories ----
    /// treatsFilePackagesAsDirectories.
    public boolean treatsFilePackagesAsDirectories() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("treatsFilePackagesAsDirectories")); } catch (Throwable t) { throw new RuntimeException("treatsFilePackagesAsDirectories failed", t); }
    }
    public void setTreatsFilePackagesAsDirectories(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setTreatsFilePackagesAsDirectories:"), flag); } catch (Throwable t) { throw new RuntimeException("setTreatsFilePackagesAsDirectories: failed", t); }
    }

    // ---- prompt (null_resettable NSString) ----
    /// prompt — text on the Open/Save button (empty resets to localized default).
    public String prompt() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("prompt")));
    }
    public void setPrompt(String prompt) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPrompt:"), prompt == null ? MemorySegment.NULL : ObjC.nsstring(prompt));
    }

    // ---- nameFieldLabel ----
    /// nameFieldLabel — text left of the name field (save panel).
    public String nameFieldLabel() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("nameFieldLabel")));
    }
    public void setNameFieldLabel(String label) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setNameFieldLabel:"), label == null ? MemorySegment.NULL : ObjC.nsstring(label));
    }

    // ---- showsTagField / tagNames (macOS 10.9) ----
    /// showsTagField.
    public boolean showsTagField() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("showsTagField")); } catch (Throwable t) { throw new RuntimeException("showsTagField failed", t); }
    }
    public void setShowsTagField(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setShowsTagField:"), flag); } catch (Throwable t) { throw new RuntimeException("setShowsTagField: failed", t); }
    }
    /// tagNames — initial Tag names (nil/empty shows none; non-nil after Save when showsTagField).
    public java.util.List<String> tagNames() {
        ensureInit();
        try {
            MemorySegment arr = (MemorySegment) hId.invokeExact(peer, ObjC.sel("tagNames"));
            if (arr == null || arr.address() == 0) return null;
            long count = ObjC.msgSendLong(arr, ObjC.sel("count"));
            java.util.List<String> out = new java.util.ArrayList<>((int) count);
            MethodHandle hAt = ObjC.handle(Sig.of(Ret.ID, Arg.INT));
            for (long i = 0; i < count; i++) {
                MemorySegment s = (MemorySegment) hAt.invokeExact(arr, ObjC.sel("objectAtIndex:"), i);
                String str = ObjC.toString(s);
                if (str != null) out.add(str);
            }
            return java.util.Collections.unmodifiableList(out);
        } catch (Throwable t) { throw new RuntimeException("tagNames failed", t); }
    }
    public void setTagNames(java.util.List<String> tags) {
        ensureInit();
        MemorySegment arr = (tags == null) ? MemorySegment.NULL : ObjC.msgSendId(ObjC.cls("NSMutableArray"), ObjC.sel("array"));
        if (tags != null) for (String t : tags) if (t != null) ObjC.msgSendVoidId(arr, ObjC.sel("addObject:"), ObjC.nsstring(t));
        ObjC.msgSendVoidId(peer, ObjC.sel("setTagNames:"), arr);
    }

    // ---- showsContentTypes (macOS 15) ----
    /// showsContentTypes — file-type picker control (hidden when allowedContentTypes empty).
    public boolean showsContentTypes() {
        ensureInit();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("showsContentTypes")); } catch (Throwable t) { throw new RuntimeException("showsContentTypes failed", t); }
    }
    public void setShowsContentTypes(boolean flag) {
        ensureInit();
        try { hSetBool.invokeExact(peer, ObjC.sel("setShowsContentTypes:"), flag); } catch (Throwable t) { throw new RuntimeException("setShowsContentTypes: failed", t); }
    }

    // ---- validateVisibleColumns ----
    /// validateVisibleColumns — refresh panel contents.
    public void validateVisibleColumns() {
        ObjC.msgSendVoid(peer, ObjC.sel("validateVisibleColumns"));
    }

    // ---- ok: / cancel: (IBAction) ----
    /// ok: — confirm action.
    public void ok(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("ok:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    /// ok: with NSObject sender.
    public void ok(NSObject sender) { ok(sender == null ? MemorySegment.NULL : sender.peer()); }
    /// cancel: — dismiss action.
    public void cancel(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("cancel:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    /// cancel: with NSObject sender.
    public void cancel(NSObject sender) { cancel(sender == null ? MemorySegment.NULL : sender.peer()); }

    // ---- beginWithCompletionHandler: (modeless, block as id; NULL is valid for probing) ----
    /// beginWithCompletionHandler: — present as modeless window; handler called with NSModalResponse.
    public void beginWithCompletionHandler(MemorySegment handler) {
        ObjC.msgSendVoidId(peer, ObjC.sel("beginWithCompletionHandler:"), (MemorySegment) (handler == null ? MemorySegment.NULL : handler));
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - NSOpenSavePanelDelegate protocol methods (panel:shouldEnableURL:, panel:validateURL:error:, etc.)
    //   — delegate protocol, not NSSavePanel selectors; wire via DelegateProxy if needed.
    // - Deprecated filename/directory/requiredFileType/beginSheetForDirectory:/runModalForDirectory:/selectText:/
    //   allowedFileTypes is KEPT (deprecated 10.3-12.0 but still the working string API on this SDK; the
    //   typed replacement allowedContentTypes is added above) — other deprecated path APIs omitted.
    // - -beginSheetModalForWindow:completionHandler: and -beginWithCompletionHandler: block args are passed as
    //   MemorySegment (NULL for probing); real blocks need Block plumbing that does not exist yet.

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
