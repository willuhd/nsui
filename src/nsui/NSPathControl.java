package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSPathControl — a control that displays a file system path. Thin, 1:1,
/// stateless wrapper over the native `NSPathControl`: each method maps to
/// one `objc_msgSend` selector. Follows the project template: volatile
/// initialized, synchronized ensureInit, ObjC.handle(Sig.of...), invokeExact,
/// static create/wrap.
///
/// Created via `[[NSPathControl alloc] initWithFrame:]`; configure
/// via `setURL:` / `URL` and `setPathStyle:`.
/// SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSPathControl.h
/// (+ NSPathCell.h for NSPathStyle Standard 0, NavigationBar 1 (deprecated), PopUp 2)
/// OMITTED: -setDraggingSourceOperationMask:forLocal: takes (NSDragOperation mask, BOOL isLocal),
/// shape (VOID,INT,BOOL) is NOT in the Sig vocabulary (grep Sig.java: no of(Ret.VOID, Arg.INT, Arg.BOOL);
/// only the ID-returning ID,INT,BOOL cousin is); NSPathControlDelegate (shouldDragItem:,
/// validateDrop:, acceptDrop:, willDisplayOpenPanel:, willPopUpMenu:) — delegate callbacks need
/// upcall delegate-proxy machinery; deprecated -clickedPathComponentCell/-pathComponentCells/
/// -setPathComponentCells: (use clickedPathItem/pathItems instead). No NSPathControlItem wrapper
/// exists in this slice (grep src/nsui/NSPathControlItem.java: miss), so pathItems/clickedPathItem
/// stay raw MemorySegment alongside NSArray conveniences.
public final class NSPathControl extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
            private record Handles(MethodHandle hSetURL, MethodHandle hGetURL, MethodHandle hSetPathStyle, MethodHandle hGetPathStyle) {}
    private static volatile Handles handles;

    private NSPathControl(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    /// Wrap an existing NSPathControl peer.
    public static NSPathControl wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSPathControl(peer);
    }

        private static void ensureInit() {
            if (handles != null) return;
            ensureInitLocked();
        }

        private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(
                ObjC.handle(Sig.of(Ret.VOID, Arg.ID)),
                ObjC.handle(Sig.of(Ret.ID)),
                ObjC.handle(Sig.of(Ret.VOID, Arg.INT)),
                ObjC.handle(Sig.of(Ret.INT))
        );
    }

    /// `[[NSPathControl alloc] initWithFrame:frame]` — a new path control.
        public static NSPathControl create(NSRect frame) {
        ensureInit();
        return new NSPathControl(ObjC.newView("NSPathControl", frame));
    }

    // ---------------------------------------------------------------- instance API

    /// [control setURL:] — NSURL peer (or nil to clear).
    public void setURL(MemorySegment url) {
        try {
            MemorySegment u = (url == null || url.address() == 0) ? MemorySegment.NULL : url;
            handles.hSetURL().invokeExact(peer, ObjC.sel("setURL:"), (MemorySegment) u);
        } catch (Throwable t) {
            throw new RuntimeException("setURL: failed", t);
        }
    }

    /// [control URL] — NSURL peer or nil.
    public MemorySegment URL() {
        try {
            return (MemorySegment) handles.hGetURL().invokeExact(peer, ObjC.sel("URL"));
        } catch (Throwable t) {
            throw new RuntimeException("URL failed", t);
        }
    }

    /// Convenience: set URL from a file system path string via NSURL fileURLWithPath:.
    public void setURLPath(String path) {
        if (path == null) {
            setURL(MemorySegment.NULL);
            return;
        }
        MemorySegment url = ObjC.msgSendIdId(ObjC.cls("NSURL"), ObjC.sel("fileURLWithPath:"), ObjC.nsstring(path));
        setURL(url);
    }

    /// Convenience: get file system path string from NSURL via [URL path].
    public String URLPath() {
        MemorySegment url = URL();
        if (url == null || url.address() == 0) return null;
        MemorySegment path = ObjC.msgSendId(url, ObjC.sel("path"));
        return ObjC.toString(path);
    }

    /// [control pathStyle] — NSPathStyle (0=standard, 1=navigational, 2=popUp).
    public long pathStyle() {
        try {
            return (long) handles.hGetPathStyle().invokeExact(peer, ObjC.sel("pathStyle"));
        } catch (Throwable t) {
            throw new RuntimeException("pathStyle failed", t);
        }
    }

    /// [control setPathStyle:] — NSPathStyle.
    public void setPathStyle(long style) {
        try {
            handles.hSetPathStyle().invokeExact(peer, ObjC.sel("setPathStyle:"), style);
        } catch (Throwable t) {
            throw new RuntimeException("setPathStyle: failed", t);
        }
    }

    /// [control setDoubleAction:] — SEL for double-click on a path component.
    public void setDoubleAction(String selector) {
        try {
            handles.hSetURL().invokeExact(peer, ObjC.sel("setDoubleAction:"), (MemorySegment) (selector == null ? MemorySegment.NULL : ObjC.sel(selector)));
        } catch (Throwable t) {
            throw new RuntimeException("setDoubleAction: failed", t);
        }
    }

    /// [control doubleAction] — SEL id or nil.
    public MemorySegment doubleAction() {
        try {
            return (MemorySegment) handles.hGetURL().invokeExact(peer, ObjC.sel("doubleAction"));
        } catch (Throwable t) {
            throw new RuntimeException("doubleAction failed", t);
        }
    }

    /// [control placeholderString] — NSString.
    public String placeholderString() {
        return ObjC.toString(ObjC.msgSendId(peer, ObjC.sel("placeholderString")));
    }

    /// [control setPlaceholderString:]
    public void setPlaceholderString(String s) {
        try {
            handles.hSetURL().invokeExact(peer, ObjC.sel("setPlaceholderString:"), (MemorySegment) (s == null ? MemorySegment.NULL : ObjC.nsstring(s)));
        } catch (Throwable t) {
            throw new RuntimeException("setPlaceholderString: failed", t);
        }
    }

    /// [control isEditable].
    public boolean isEditable() {
        return ObjC.msgSendBool(peer, ObjC.sel("isEditable"));
    }

    public void setEditable(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setEditable:"), flag);
    }

    /// [control allowedTypes] — NSArray of UTIs (id).
    public MemorySegment allowedTypes() {
        return ObjC.msgSendId(peer, ObjC.sel("allowedTypes"));
    }

    public void setAllowedTypes(MemorySegment types) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAllowedTypes:"), (MemorySegment) (types == null ? MemorySegment.NULL : types));
    }

    /// [control clickedPathItem] — NSPathControlItem peer or nil.
    public MemorySegment clickedPathItem() {
        return ObjC.msgSendId(peer, ObjC.sel("clickedPathItem"));
    }

    // ---- completeness: remaining header API in registered shapes (grep Sig.java per shape) ----
    /// [control placeholderAttributedString] — drawn when path is empty (or nil).
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary.
    public MemorySegment placeholderAttributedString() {
        return ObjC.msgSendId(peer, ObjC.sel("placeholderAttributedString"));
    }
    /// [control setPlaceholderAttributedString:] — nil clears.
    public void setPlaceholderAttributedString(MemorySegment attr) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPlaceholderAttributedString:"),
                (MemorySegment) (attr == null ? MemorySegment.NULL : attr));
    }
    /// Typed variant (NSAttributedString wrapper exists in this slice).
    public NSAttributedString placeholderAttributedStringTyped() {
        return NSAttributedString.wrap(ObjC.msgSendId(peer, ObjC.sel("placeholderAttributedString")));
    }
    /// Typed setter.
    public void setPlaceholderAttributedString(NSAttributedString attr) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPlaceholderAttributedString:"),
                (MemorySegment) (attr == null ? MemorySegment.NULL : attr.peer()));
    }

    /// [control pathItems] — array of NSPathControlItem currently displayed (raw id).
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary. No NSPathControlItem wrapper
    /// exists, so raw + NSArray convenience (elements are NSPathControlItem ids).
    public MemorySegment pathItems() {
        return ObjC.msgSendId(peer, ObjC.sel("pathItems"));
    }
    /// [control setPathItems:] — must be non-nil (pass an empty array, not null).
    public void setPathItems(MemorySegment items) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPathItems:"),
                (MemorySegment) (items == null ? MemorySegment.NULL : items));
    }
    /// Typed NSArray convenience for pathItems.
    public NSArray pathItemsArray() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("pathItems")));
    }
    /// Setter from NSArray wrapper.
    public void setPathItems(NSArray items) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setPathItems:"),
                (MemorySegment) (items == null ? MemorySegment.NULL : items.peer()));
    }

    /// [control backgroundColor] — drawn background (nil for most styles).
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary.
    public NSColor backgroundColor() {
        return NSColor.wrap(ObjC.msgSendId(peer, ObjC.sel("backgroundColor")));
    }
    /// [control setBackgroundColor:] — nil clears (clearColor for transparent).
    public void setBackgroundColor(NSColor color) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setBackgroundColor:"),
                (MemorySegment) (color == null ? MemorySegment.NULL : color.peer()));
    }

    /// [control delegate] — weakly referenced id, or nil.
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary.
    public MemorySegment delegate() {
        return ObjC.msgSendId(peer, ObjC.sel("delegate"));
    }
    /// [control setDelegate:].
    public void setDelegate(MemorySegment d) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"),
                (MemorySegment) (d == null ? MemorySegment.NULL : d));
    }
    /// Typed overload.
    public void setDelegate(NSObject d) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setDelegate:"),
                (MemorySegment) (d == null ? MemorySegment.NULL : d.peer()));
    }

    /// [control menu] — menu used when style is NSPathStylePopUp (or nil).
    /// Shapes (ID ()) / (VOID,ID) are in the vocabulary.
    /// Note: sibling batch adds NSResponder.menu() returning NSMenu; this matches it
    /// (raw peer via menuSegment()). No @Override so it also compiles against HEAD.
    public NSMenu menu() {
        return NSMenu.wrap(ObjC.msgSendId(peer, ObjC.sel("menu")));
    }
    /// Raw peer variant (no NSMenu wrap).
    public MemorySegment menuSegment() {
        return ObjC.msgSendId(peer, ObjC.sel("menu"));
    }
    /// [control setMenu:] raw.
    public void setMenu(MemorySegment menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMenu:"),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu));
    }
    /// Typed accessors (NSMenu wrapper exists in this slice).
    public NSMenu menuTyped() {
        return menu();
    }
    /// Typed setter (matches NSResponder.setMenu(NSMenu) when present).
    public void setMenu(NSMenu menu) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setMenu:"),
                (MemorySegment) (menu == null ? MemorySegment.NULL : menu.peer()));
    }

    /// [control allowedTypes] as NSArray wrapper (elements are NSString UTIs/extensions).
    public NSArray allowedTypesArray() {
        return NSArray.wrap(ObjC.msgSendId(peer, ObjC.sel("allowedTypes")));
    }
    /// [control setAllowedTypes:] from NSArray wrapper (null allows all types).
    public void setAllowedTypes(NSArray types) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setAllowedTypes:"),
                (MemorySegment) (types == null ? MemorySegment.NULL : types.peer()));
    }
}
