package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSMutableParagraphStyle — mutable paragraph style.
/// Thin 1:1 wrapper over native `NSMutableParagraphStyle`: every method maps to one
/// `objc_msgSend` selector, no cached Java state beyond the peer.
/// Follows FFM pattern: no reflection, cached handles, ensureInit.
public class NSMutableParagraphStyle extends NSParagraphStyle {

    private static volatile boolean mutableInitialized;
    private static MethodHandle hSetLong;    // (id, SEL, long) -> void
    private static MethodHandle hSetDouble;  // (id, SEL, double) -> void

    protected NSMutableParagraphStyle(MemorySegment peer) {
        super(peer);
        ensureMutInit();
    }

    public static NSMutableParagraphStyle wrap(MemorySegment peer) {
        return (peer == null || peer.address() == 0) ? null : new NSMutableParagraphStyle(peer);
    }

    private static synchronized void ensureMutInit() {
        if (mutableInitialized) return;
        hSetLong = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetDouble = ObjC.handle(Sig.of(Ret.VOID, Arg.DOUBLE));
        mutableInitialized = true;
    }

    /// `[[NSMutableParagraphStyle alloc] init]`
    public static NSMutableParagraphStyle create() {
        ensureMutInit();
        MemorySegment alloc = ObjC.msgSendId(ObjC.cls("NSMutableParagraphStyle"), ObjC.sel("alloc"));
        MemorySegment p = ObjC.msgSendId(alloc, ObjC.sel("init"));
        if (p.address() == 0) throw new IllegalStateException("NSMutableParagraphStyle init returned nil");
        return new NSMutableParagraphStyle(p);
    }

    /// [style setAlignment:] — NSTextAlignment
    public void setAlignment(long alignment) {
        ensureMutInit();
        try {
            hSetLong.invokeExact(peer, ObjC.sel("setAlignment:"), alignment);
        } catch (Throwable t) {
            throw new RuntimeException("setAlignment: failed", t);
        }
    }

    /// [style setLineBreakMode:] — NSLineBreakMode
    public void setLineBreakMode(long mode) {
        ensureMutInit();
        try {
            hSetLong.invokeExact(peer, ObjC.sel("setLineBreakMode:"), mode);
        } catch (Throwable t) {
            throw new RuntimeException("setLineBreakMode: failed", t);
        }
    }

    // Convenience overrides returning mutable type
    @Override
    public long alignment() { return super.alignment(); }
    @Override
    public long lineBreakMode() { return super.lineBreakMode(); }

    /// [style setLineSpacing:]
    public void setLineSpacing(double spacing) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setLineSpacing:"), spacing);
        } catch (Throwable t) {
            throw new RuntimeException("setLineSpacing: failed", t);
        }
    }

    /// [style setParagraphSpacing:]
    public void setParagraphSpacing(double spacing) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setParagraphSpacing:"), spacing);
        } catch (Throwable t) {
            throw new RuntimeException("setParagraphSpacing: failed", t);
        }
    }

    /// [style setHeadIndent:]
    public void setHeadIndent(double indent) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setHeadIndent:"), indent);
        } catch (Throwable t) {
            throw new RuntimeException("setHeadIndent: failed", t);
        }
    }

    /// [style setTailIndent:]
    public void setTailIndent(double indent) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setTailIndent:"), indent);
        } catch (Throwable t) {
            throw new RuntimeException("setTailIndent: failed", t);
        }
    }

    /// [style setFirstLineHeadIndent:]
    public void setFirstLineHeadIndent(double indent) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setFirstLineHeadIndent:"), indent);
        } catch (Throwable t) {
            throw new RuntimeException("setFirstLineHeadIndent: failed", t);
        }
    }

    /// [style setMinimumLineHeight:]
    public void setMinimumLineHeight(double height) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMinimumLineHeight:"), height);
        } catch (Throwable t) {
            throw new RuntimeException("setMinimumLineHeight: failed", t);
        }
    }

    /// [style setMaximumLineHeight:]
    public void setMaximumLineHeight(double height) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setMaximumLineHeight:"), height);
        } catch (Throwable t) {
            throw new RuntimeException("setMaximumLineHeight: failed", t);
        }
    }

    /// [style setBaseWritingDirection:] — NSWritingDirection.
    public void setBaseWritingDirection(long direction) {
        ensureMutInit();
        try {
            hSetLong.invokeExact(peer, ObjC.sel("setBaseWritingDirection:"), direction);
        } catch (Throwable t) {
            throw new RuntimeException("setBaseWritingDirection: failed", t);
        }
    }

    /// [style setLineHeightMultiple:]
    public void setLineHeightMultiple(double factor) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setLineHeightMultiple:"), factor);
        } catch (Throwable t) {
            throw new RuntimeException("setLineHeightMultiple: failed", t);
        }
    }

    /// [style setParagraphSpacingBefore:]
    public void setParagraphSpacingBefore(double spacing) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setParagraphSpacingBefore:"), spacing);
        } catch (Throwable t) {
            throw new RuntimeException("setParagraphSpacingBefore: failed", t);
        }
    }

    /// [style setHyphenationFactor:] — float 0.0..1.0.
    public void setHyphenationFactor(float factor) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT));
            h.invokeExact(peer, ObjC.sel("setHyphenationFactor:"), factor);
        } catch (Throwable t) {
            throw new RuntimeException("setHyphenationFactor: failed", t);
        }
    }

    /// [style setUsesDefaultHyphenation:] (macOS 12+).
    public void setUsesDefaultHyphenation(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setUsesDefaultHyphenation:"), flag);
    }

    /// [style setTabStops:] — NSArray of NSTextTab (untyped).
    public void setTabStops(NSArray tabs) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTabStops:"),
                (MemorySegment) (tabs == null ? MemorySegment.NULL : tabs.peer()));
    }

    /// [style setDefaultTabInterval:]
    public void setDefaultTabInterval(double interval) {
        ensureMutInit();
        try {
            hSetDouble.invokeExact(peer, ObjC.sel("setDefaultTabInterval:"), interval);
        } catch (Throwable t) {
            throw new RuntimeException("setDefaultTabInterval: failed", t);
        }
    }

    /// [style setAllowsDefaultTighteningForTruncation:]
    public void setAllowsDefaultTighteningForTruncation(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setAllowsDefaultTighteningForTruncation:"), flag);
    }

    /// [style setLineBreakStrategy:] — NSLineBreakStrategy.
    public void setLineBreakStrategy(long strategy) {
        ensureMutInit();
        try {
            hSetLong.invokeExact(peer, ObjC.sel("setLineBreakStrategy:"), strategy);
        } catch (Throwable t) {
            throw new RuntimeException("setLineBreakStrategy: failed", t);
        }
    }

    /// [style setTextLists:] — NSArray of NSTextList (untyped).
    public void setTextLists(NSArray lists) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTextLists:"),
                (MemorySegment) (lists == null ? MemorySegment.NULL : lists.peer()));
    }

    /// [style setTextBlocks:] — NSArray of NSTextBlock (untyped).
    public void setTextBlocks(NSArray blocks) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setTextBlocks:"),
                (MemorySegment) (blocks == null ? MemorySegment.NULL : blocks.peer()));
    }

    /// [style addTabStop:] — tab is NSTextTab* (raw; NSTextTab has no wrapper in this batch).
    public void addTabStop(MemorySegment tab) {
        ObjC.msgSendVoidId(peer, ObjC.sel("addTabStop:"),
                (MemorySegment) (tab == null ? MemorySegment.NULL : tab));
    }

    /// [style removeTabStop:]
    public void removeTabStop(MemorySegment tab) {
        ObjC.msgSendVoidId(peer, ObjC.sel("removeTabStop:"),
                (MemorySegment) (tab == null ? MemorySegment.NULL : tab));
    }

    /// [style setParagraphStyle:] — copy all values from another style.
    public void setParagraphStyle(NSParagraphStyle style) {
        ObjC.msgSendVoidId(peer, ObjC.sel("setParagraphStyle:"),
                (MemorySegment) (style == null ? MemorySegment.NULL : style.peer()));
    }

    /// [style setTighteningFactorForTruncation:] — float.
    public void setTighteningFactorForTruncation(float factor) {
        ensureMutInit();
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.VOID, Arg.FLOAT));
            h.invokeExact(peer, ObjC.sel("setTighteningFactorForTruncation:"), factor);
        } catch (Throwable t) {
            throw new RuntimeException("setTighteningFactorForTruncation: failed", t);
        }
    }

    /// [style setHeaderLevel:] — NSInteger.
    public void setHeaderLevel(long level) {
        ensureMutInit();
        try {
            hSetLong.invokeExact(peer, ObjC.sel("setHeaderLevel:"), level);
        } catch (Throwable t) {
            throw new RuntimeException("setHeaderLevel: failed", t);
        }
    }
}
