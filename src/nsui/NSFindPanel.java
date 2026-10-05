package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSFindPanel — minimal wrapper for the system Find panel.
/// Thin 1:1 wrapper; native class is `NSPanel` subclass used by
/// `NSTextView`’s find bar / find panel integration.
///
/// On AppKit the find panel is exposed via `NSTextFinder` /
/// `NSFindPanelAction`; this wrapper keeps a conventional
/// `NSObject` shape with `shared` accessor so build passes
/// even where the underlying native class name differs across OS versions.
public final class NSFindPanel extends NSObject {

    private static volatile boolean initialized;
    private static MethodHandle hGetId;   // (id, SEL) -> id
    private static MethodHandle hBool;    // (id, SEL) -> bool
    private static MethodHandle hSetBool; // (id, SEL, bool) -> void

    private NSFindPanel(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    public static NSFindPanel wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSFindPanel(peer);
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hGetId = ObjC.handle(Sig.of(Ret.ID));
        hBool = ObjC.handle(Sig.of(Ret.BOOL));
        hSetBool = ObjC.handle(Sig.of(Ret.VOID, Arg.BOOL));
        initialized = true;
    }

    // ---- shared accessor (tries NSFindPanel, falls back to NSPanel) ----

    /// `+[NSFindPanel sharedFindPanel]` — if class exists, otherwise nil.
    public static NSFindPanel sharedFindPanel() {
        ensureInit();
        // NSFindPanel is private on some SDKs; try NSFindPanel first, then NSTextFinder's panel
        MemorySegment cls = null;
        try {
            cls = ObjC.cls("NSFindPanel");
            // quick check: does it respond to sharedFindPanel?
            MemorySegment p = ObjC.msgSendId(cls, ObjC.sel("sharedFindPanel"));
            if (p != null && p.address() != 0) return wrap(p);
        } catch (Throwable ignored) {}
        // Fallback: use NSPanel's shared instance as a placeholder panel
        try {
            MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPanel"), ObjC.sel("alloc"));
            // init is (id) -> id already handled; just return generic panel as find panel
            // Instead return null to indicate no find panel class
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    /// Create a basic panel for find UI (alloc+init).
    public static NSFindPanel create() {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSPanel"), ObjC.sel("alloc"));
        try {
            MemorySegment q = (MemorySegment) hGetId.invokeExact(p, ObjC.sel("init"));
            return wrap(q);
        } catch (Throwable t) {
            throw new RuntimeException("NSFindPanel init failed", t);
        }
    }

    // ---- selector guard (ObjC exceptions abort the JVM — Java try/catch cannot catch them) ----
    /// respondsTo: — true when the underlying panel implements the selector. Every best-effort send
    /// below checks this FIRST; sending an unimplemented selector raises NSInvalidArgumentException which
    /// terminates the process (uncatchable), so try/catch alone is not sufficient.
    private boolean respondsTo(String selector) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID))
                    .invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel(selector));
        } catch (Throwable t) { return false; }
    }

    // ---- find string ----

    /// [panel findString] — placeholder; backed by find pasteboard on real FindPanel.
    /// Returns null when the underlying panel does not implement findString (the common NSPanel case).
    public String findString() {
        if (!respondsTo("findString")) return null;
        try {
            MemorySegment s = ObjC.msgSendId(peer, ObjC.sel("findString"));
            return ObjC.toString(s);
        } catch (Throwable ignored) { return null; }
    }

    public void setFindString(String s) {
        if (!respondsTo("setFindString:")) return;
        try {
            ObjC.msgSendVoidId(peer, ObjC.sel("setFindString:"), s == null ? MemorySegment.NULL : ObjC.nsstring(s));
        } catch (Throwable ignored) {}
    }

    // ---- options ----

    public boolean isCaseSensitive() {
        if (!respondsTo("isCaseSensitive")) return false;
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isCaseSensitive")); } catch (Throwable t) { return false; }
    }
    public void setCaseSensitive(boolean flag) {
        if (!respondsTo("setCaseSensitive:")) return;
        try { hSetBool.invokeExact(peer, ObjC.sel("setCaseSensitive:"), flag); } catch (Throwable ignored) {}
    }

    public boolean isRegularExpression() {
        if (!respondsTo("isRegularExpression")) return false;
        try { return (boolean) hBool.invokeExact(peer, ObjC.sel("isRegularExpression")); } catch (Throwable t) { return false; }
    }
    public void setRegularExpression(boolean flag) {
        if (!respondsTo("setRegularExpression:")) return;
        try { hSetBool.invokeExact(peer, ObjC.sel("setRegularExpression:"), flag); } catch (Throwable ignored) {}
    }

    // ---- visibility ----

    public boolean isVisible() {
        return ObjC.msgSendBool(peer, ObjC.sel("isVisible"));
    }
    public void orderFront(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public void orderOut(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("orderOut:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }
    public void makeKeyAndOrderFront(MemorySegment sender) {
        ObjC.msgSendVoidId(peer, ObjC.sel("makeKeyAndOrderFront:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---- action support for NSTextFinder integration ----

    /// [panel performFindPanelAction:] — forward to sender if needed.
    /// No-op when the underlying panel does not implement it (NSInvalidArgumentException is fatal).
    public void performFindPanelAction(MemorySegment sender) {
        if (!respondsTo("performFindPanelAction:")) return;
        ObjC.msgSendVoidId(peer, ObjC.sel("performFindPanelAction:"), (MemorySegment) (sender == null ? MemorySegment.NULL : sender));
    }

    // ---------------------------------------------------------------- nested types — verified against local SDK headers
    // SDK: $(xcrun --show-sdk-path)/System/Library/Frameworks/AppKit.framework/Headers/NSTextFinder.h
    //   NSTextFinderAction: ShowFindInterface 1, NextMatch 2, PreviousMatch 3, ReplaceAll 4, Replace 5,
    //     ReplaceAndFind 6, SetSearchString 7, ReplaceAllInSelection 8, SelectAll 9, SelectAllInSelection 10,
    //     HideFindInterface 11, ShowReplaceInterface 12, HideReplaceInterface 13
    //   NSTextFinderMatchingType: Contains 0, StartsWith 1, FullWord 2, EndsWith 3
    // Docs: https://developer.apple.com/documentation/appkit/nstextfinder
    /// `NSTextFinderAction` — tags for -performTextFinderAction: responders (also used with NSFindPanelAction).
    public enum FinderAction {
        showFindInterface(1), nextMatch(2), previousMatch(3), replaceAll(4), replace(5),
        replaceAndFind(6), setSearchString(7), replaceAllInSelection(8), selectAll(9),
        selectAllInSelection(10), hideFindInterface(11), showReplaceInterface(12), hideReplaceInterface(13);
        public final long value;
        FinderAction(long v) { this.value = v; }
        public static FinderAction fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }
    /// `NSTextFinderMatchingType` — 0=Contains, 1=StartsWith, 2=FullWord, 3=EndsWith.
    public enum MatchingType {
        contains(0), startsWith(1), fullWord(2), endsWith(3);
        public final long value;
        MatchingType(long v) { this.value = v; }
        public static MatchingType fromValue(long v) { for (var e : values()) if (e.value == v) return e; return null; }
    }

    // ---------------------------------------------------------------- omissions (documented, not oversights)
    // - There is NO native NSFindPanel class in the macOS SDK (grep AppKit Headers for NSFindPanel — no match).
    //   Find UI lives in NSTextFinder (NSTextFinder.h) + NSTextFinderClient/BarContainer protocols. This wrapper
    //   stays a thin NSPanel-backed placeholder with the conventional sharedFindPanel/create shape so existing
    //   call sites build; findString/caseSensitive/regularExpression perform best-effort selector sends and
    //   degrade to null/false when the underlying panel does not implement them (never throws).
    // - NSTextFinder client/bar-container protocol methods (stringAtIndex:, selectedRanges, scrollRangeToVisible:,
    //   contentViewAtIndex:, etc.) — omitted: responder-side protocol plumbing needing DelegateProxy shapes and
    //   NSRange-pointer out-params with no registered shape; out of scope for this batch.
    // - Panel show/run is NSPanel/NSWindow behavior — never invoked from tests (hidden-only rule).
}
