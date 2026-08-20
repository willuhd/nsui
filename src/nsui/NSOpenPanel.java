package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSOpenPanel — native open dialog (subclass of NSSavePanel).
/// Thin 1:1 wrapper over AppKit NSOpenPanel.
public final class NSOpenPanel extends NSSavePanel {

    private static volatile boolean initialized;
    private static MethodHandle hBool;
    private static MethodHandle hSetBool;
    private static MethodHandle hId;

    private NSOpenPanel(MemorySegment peer) {
        super(peer);
        ensureInitOpen();
    }

    private static synchronized void ensureInitOpen() {
        if (initialized) return;
        // Ensure base initialized
        NSSavePanel.ensureInit();
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        hId = ObjC.handle(Sig.of(Ret.ID));
        initialized = true;
    }

    public static NSOpenPanel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSOpenPanel(peer);
    }

    /// +[NSOpenPanel openPanel]
    public static NSOpenPanel openPanel() {
        ensureInitOpen();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSOpenPanel"), ObjC.sel("openPanel"));
        return wrap(p);
    }

    // ---- canChooseFiles ----
    public boolean canChooseFiles() {
        ensureInitOpen();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("canChooseFiles")); } catch (Throwable t) { throw new RuntimeException("canChooseFiles failed", t); }
    }
    public void setCanChooseFiles(boolean flag) {
        ensureInitOpen();
        try { hSetBool.invokeExact(peer, ObjC.sel("setCanChooseFiles:"), flag); } catch (Throwable t) { throw new RuntimeException("setCanChooseFiles: failed", t); }
    }

    // ---- canChooseDirectories ----
    public boolean canChooseDirectories() {
        ensureInitOpen();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("canChooseDirectories")); } catch (Throwable t) { throw new RuntimeException("canChooseDirectories failed", t); }
    }
    public void setCanChooseDirectories(boolean flag) {
        ensureInitOpen();
        try { hSetBool.invokeExact(peer, ObjC.sel("setCanChooseDirectories:"), flag); } catch (Throwable t) { throw new RuntimeException("setCanChooseDirectories: failed", t); }
    }

    // (legacy alias matching task name setCanChooseDirectory)
    public void setCanChooseDirectory(boolean flag) { setCanChooseDirectories(flag); }

    // ---- allowsMultipleSelection ----
    public boolean allowsMultipleSelection() {
        ensureInitOpen();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("allowsMultipleSelection")); } catch (Throwable t) { throw new RuntimeException("allowsMultipleSelection failed", t); }
    }
    public void setAllowsMultipleSelection(boolean flag) {
        ensureInitOpen();
        try { hSetBool.invokeExact(peer, ObjC.sel("setAllowsMultipleSelection:"), flag); } catch (Throwable t) { throw new RuntimeException("setAllowsMultipleSelection: failed", t); }
    }

    // ---- canCreateDirectories inherited (expose for completeness) ----
    // already in NSSavePanel

    // ---- allowedFileTypes inherited ----

    // ---- URLs ----
    public java.util.List<MemorySegment> URLs() {
        return urlsAsArray();
    }
    public java.util.List<String> URLsAsStrings() {
        java.util.List<MemorySegment> arr = URLs();
        java.util.List<String> out = new java.util.ArrayList<>(arr.size());
        for (MemorySegment u : arr) {
            MemorySegment s = ObjC.msgSendId(u, ObjC.sel("path"));
            out.add(ObjC.toString(s));
        }
        return java.util.Collections.unmodifiableList(out);
    }

    // ---- directoryURL inherited, but expose same ----
    @Override
    public MemorySegment directoryURL() { return super.directoryURL(); }

    // ---- resolvesAliases / canDownloadUbiquitousContents etc ----
    public boolean resolvesAliases() {
        ensureInitOpen();
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("resolvesAliases")); } catch (Throwable t) { throw new RuntimeException("resolvesAliases failed", t); }
    }
    public void setResolvesAliases(boolean flag) {
        ensureInitOpen();
        try { hSetBool.invokeExact(peer, ObjC.sel("setResolvesAliases:"), flag); } catch (Throwable t) { throw new RuntimeException("setResolvesAliases: failed", t); }
    }
}
