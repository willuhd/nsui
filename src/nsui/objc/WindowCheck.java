package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Pure-FFM verification helper: asks the macOS window server (CoreGraphics
 * CGWindowListCopyWindowInfo) which windows are actually on screen and reports
 * the ones owned by our process. This proves — from outside AppKit — that the
 * window really exists on the display.
 */
public final class WindowCheck {

    private static final long KCG_WINDOW_LIST_OPTION_ON_SCREEN_ONLY = 1L;

    private static Linker LINKER;
    private static Arena ARENA;
    private static ValueLayout PTR;
    private static ValueLayout LONG;
    private static ValueLayout BOOL;

    private static MethodHandle hListCopy;     // CGWindowListCopyWindowInfo
    private static MethodHandle hArrCount;     // CFArrayGetCount
    private static MethodHandle hArrGet;       // CFArrayGetValueAtIndex
    private static MethodHandle hDictGet;      // CFDictionaryGetValue
    private static MethodHandle hNumGet;       // CFNumberGetValue
    private static MethodHandle hStrGetC;      // CFStringGetCString
    private static MethodHandle hStrCreate;    // CFStringCreateWithCString

    private static MemorySegment kOwnerPid;
    private static MemorySegment kOwnerName;
    private static MemorySegment kWindowNumber;
    private static MemorySegment kWindowName;
    private static MemorySegment kBounds;

    private WindowCheck() {}

    /** Run-time init, from main() (native-image: no FFM work in static initializers). */
    public static void init() {
        LINKER = Linker.nativeLinker();
        ARENA = Arena.global();
        PTR = (ValueLayout) LINKER.canonicalLayouts().get("void*");
        LONG = (ValueLayout) LINKER.canonicalLayouts().get("long");
        BOOL = (ValueLayout) LINKER.canonicalLayouts().get("bool");

        SymbolLookup cg = SymbolLookup.libraryLookup(
                "/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics", ARENA);
        SymbolLookup cf = SymbolLookup.libraryLookup(
                "/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation", ARENA);

        hListCopy = down(cg, "CGWindowListCopyWindowInfo", NsuiForeign.cgWindowListCopy());
        hArrCount = down(cf, "CFArrayGetCount", NsuiForeign.cfArrayGetCount());
        hArrGet = down(cf, "CFArrayGetValueAtIndex", NsuiForeign.cfArrayGetValueAtIndex());
        hDictGet = down(cf, "CFDictionaryGetValue", NsuiForeign.cfDictionaryGetValue());
        hNumGet = down(cf, "CFNumberGetValue", NsuiForeign.cfNumberGetValue());
        hStrGetC = down(cf, "CFStringGetCString", NsuiForeign.cfStringGetCString());
        hStrCreate = down(cf, "CFStringCreateWithCString", NsuiForeign.cfStringCreateWithCString());

        kOwnerPid = readConst(cg, "kCGWindowOwnerPID");
        kOwnerName = readConst(cg, "kCGWindowOwnerName");
        kWindowNumber = readConst(cg, "kCGWindowNumber");
        kWindowName = readConst(cg, "kCGWindowName");
        kBounds = readConst(cg, "kCGWindowBounds");
    }

    /** List on-screen windows owned by {@code pid}; returns true if our window is there. */
    public static boolean report(long pid, long expectedWindowNumber) {
        try {
            MemorySegment list = (MemorySegment) invoke(hListCopy, KCG_WINDOW_LIST_OPTION_ON_SCREEN_ONLY, 0L);
            if (list == null || list.address() == 0) {
                System.out.println("[CGWindowList] CGWindowListCopyWindowInfo returned NULL");
                return false;
            }
            long count = (long) invoke(hArrCount, list);
            System.out.println("[CGWindowList] " + count + " on-screen window(s) total");
            boolean found = false;
            for (long i = 0; i < count; i++) {
                MemorySegment dict = (MemorySegment) invoke(hArrGet, list, i);
                MemorySegment pidNum = (MemorySegment) invoke(hDictGet, dict, kOwnerPid);
                if (pidNum == null || pidNum.address() == 0) continue;
                if (cfNumLong(pidNum) != pid) continue;
                found = true;
                long num = cfNumLong((MemorySegment) invoke(hDictGet, dict, kWindowNumber));
                String owner = cfString((MemorySegment) invoke(hDictGet, dict, kOwnerName));
                String title = cfString((MemorySegment) invoke(hDictGet, dict, kWindowName));
                double bx = Double.NaN, by = Double.NaN, bw = Double.NaN, bh = Double.NaN;
                MemorySegment bounds = (MemorySegment) invoke(hDictGet, dict, kBounds);
                if (bounds != null && bounds.address() != 0) {
                    bx = cfDictDouble(bounds, "X");
                    by = cfDictDouble(bounds, "Y");
                    bw = cfDictDouble(bounds, "Width");
                    bh = cfDictDouble(bounds, "Height");
                }
                String ours = num == expectedWindowNumber ? "   <-- THIS IS OUR WINDOW" : "";
                System.out.printf("[CGWindowList] owner=%-14s windowNumber=%-6d title=%-40s bounds={%.0f,%.0f %.0fx%.0f}%s%n",
                        owner == null ? "?" : owner, num, title == null ? "(hidden)" : title,
                        bx, by, bw, bh, ours);
            }
            if (!found) {
                System.out.println("[CGWindowList] no on-screen window owned by pid " + pid + "!");
            }
            return found;
        } catch (Throwable t) {
            System.out.println("[CGWindowList] verification failed: " + t);
            return false;
        }
    }

    // ------------------------------------------------------------------ helpers

    private static long cfNumLong(MemorySegment num) {
        MemorySegment out = ARENA.allocate(LONG);
        invoke(hNumGet, num, 4L /* kCFNumberSInt64Type */, out);
        return out.get(ValueLayout.JAVA_LONG, 0);
    }

    private static double cfDictDouble(MemorySegment dict, String keyName) {
        MemorySegment key = (MemorySegment) invoke(hStrCreate,
                MemorySegment.NULL, ObjC.cstring(keyName), 0x08000100L /* kCFStringEncodingUTF8 */);
        MemorySegment num = (MemorySegment) invoke(hDictGet, dict, key);
        if (num == null || num.address() == 0) return Double.NaN;
        MemorySegment out = ARENA.allocate(LINKER.canonicalLayouts().get("double"));
        invoke(hNumGet, num, 13L /* kCFNumberDoubleType */, out);
        return out.get(ValueLayout.JAVA_DOUBLE, 0);
    }

    private static String cfString(MemorySegment str) {
        if (str == null || str.address() == 0) return null;
        MemorySegment buf = ARENA.allocate(1024);
        boolean ok = (boolean) invoke(hStrGetC, str, buf, 1024L, 0x08000100L /* kCFStringEncodingUTF8 */);
        return ok ? buf.getString(0L) : null;
    }

    /** dlsym-style read of an exported data symbol: the symbol address holds a pointer value. */
    static MemorySegment readConst(SymbolLookup lookup, String name) {
        MemorySegment sym = lookup.find(name).orElseThrow(() -> new IllegalStateException("symbol not found: " + name));
        return sym.reinterpret(PTR.byteSize()).get(ValueLayout.ADDRESS, 0);
    }

    private static MethodHandle down(SymbolLookup lookup, String name, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(lookup.find(name).orElseThrow(() -> new IllegalStateException("symbol not found: " + name)), descriptor);
    }

    private static Object invoke(MethodHandle h, Object... args) {
        try {
            return h.invokeWithArguments(args);
        } catch (Throwable t) {
            throw new RuntimeException("native call failed", t);
        }
    }
}
