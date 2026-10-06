package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSHapticFeedbackManager — Force Touch trackpad haptics. Stateless utility
/// (no instances): resolves the default performer and performs one pattern.
/// The system may suppress feedback (e.g. finger off the trackpad).
public final class NSHapticFeedbackManager {

    /// Feedback patterns (NSHapticFeedbackPattern).
    public static final long PATTERN_GENERIC = 0;
    public static final long PATTERN_ALIGNMENT = 1;
    public static final long PATTERN_LEVEL_CHANGE = 2;

    /// Performance times (NSHapticFeedbackPerformanceTime).
    public static final long TIME_DEFAULT = 0;
    public static final long TIME_NOW = 1;
    public static final long TIME_DRAW_COMPLETED = 2;

    private record Handles(MethodHandle hPerform) {}
    private static volatile Handles handles;

    private NSHapticFeedbackManager() {}

    private static void ensureInit() {
        if (handles != null) return;
        ensureInitLocked();
    }

    private static synchronized void ensureInitLocked() {
        if (handles != null) return;
        handles = new Handles(ObjC.handle(Sig.of(Ret.VOID, Arg.INT, Arg.INT)));
    }

    /// Perform one haptic pattern via the default performer (no-op when nil).
    public static void performFeedbackPattern(long pattern, long time) {
        ensureInit();
        MemorySegment performer = ObjC.msgSendId(
                ObjC.cls("NSHapticFeedbackManager"), ObjC.sel("defaultPerformer"));
        if (performer.address() == 0) return;
        try {
            handles.hPerform().invokeExact(performer,
                    ObjC.sel("performFeedbackPattern:performanceTime:"), pattern, time);
        } catch (Throwable t) {
            throw new RuntimeException("performFeedbackPattern:performanceTime: failed", t);
        }
    }
}
