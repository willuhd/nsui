package nsui.objc;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/// ThemeObserver — CoreFoundation-based dark-mode observer (no swizzle).
/// Ported from NSUI2 MacTheme.java to nsui3 idioms: no static initializer,
/// lazy ensureInit() synchronized, handles from NsuiForeign.THEME descriptors.
/// Observes CFPreferences AppleInterfaceStyle via Distributed + Darwin notify centers.
public final class ThemeObserver {

    private static final String PREFERENCE_KEY = "AppleInterfaceStyle";
    private static final String NOTIFICATION_NAME = "AppleInterfaceThemeChangedNotification";
    private static final String WAKE_NOTIFICATION_NAME = "com.apple.system.powermanagement.wake";

    // ---- lazy init state (no static initializer — native-image rule) ----
    private static volatile boolean initDone = false;
    private static final Object initLock = new Object();

    private static MethodHandle CFStringCreateWithCString;
    private static MethodHandle CFRelease;
    private static MethodHandle CFPreferencesCopyAppValue;
    private static MethodHandle CFStringCompare;
    private static MethodHandle CFNotificationCenterGetDistributedCenter;
    private static MethodHandle CFNotificationCenterGetDarwinNotifyCenter;
    private static MethodHandle CFNotificationCenterAddObserver;
    private static MethodHandle CFNotificationCenterRemoveObserver;
    private static MethodHandle CFGetTypeID;
    private static MethodHandle CFStringGetTypeID;

    private static long CACHED_STRING_TYPE_ID;

    private static MemorySegment kCFPreferencesAnyApplication;
    private static MemorySegment preferenceKeyCf;
    private static MemorySegment darkCf;
    private static MemorySegment globalNotificationName;
    private static MemorySegment wakeNotificationName;
    private static MemorySegment callbackStub;

    // ---- observer lifecycle (singleton instance) ----
    private static class Holder {
        private static final ThemeObserver INSTANCE = new ThemeObserver();
    }

    public static ThemeObserver getInstance() {
        return Holder.INSTANCE;
    }

    private final Set<Consumer<Boolean>> listeners = ConcurrentHashMap.newKeySet();
    private volatile boolean lastKnownDark;
    private final Object lifecycleLock = new Object();
    private volatile boolean initialized = false;
    private volatile ExecutorService themeExecutor = null;

    private ThemeObserver() {
        // lastKnownDark will be set in startNativeObserver; default false is fine
        // Avoid calling queryIsDark here to keep constructor cheap and avoid init order issues
    }

    // ---- lazy ensureInit — builds all downcall handles on first use ----
    private static void ensureInit() {
        if (initDone) return;
        synchronized (initLock) {
            if (initDone) return;
            try {
                Linker linker = Linker.nativeLinker();
                ValueLayout PTR = (ValueLayout) linker.canonicalLayouts().get("void*");

                SymbolLookup cf = SymbolLookup.libraryLookup(
                        "/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation",
                        Arena.global());

                CFStringCreateWithCString = linker.downcallHandle(
                        cf.find("CFStringCreateWithCString").orElseThrow(),
                        NsuiForeign.cfStringCreateWithCString());
                CFRelease = linker.downcallHandle(
                        cf.find("CFRelease").orElseThrow(),
                        NsuiForeign.cfRelease());
                CFPreferencesCopyAppValue = linker.downcallHandle(
                        cf.find("CFPreferencesCopyAppValue").orElseThrow(),
                        NsuiForeign.cfPreferencesCopyAppValue());
                CFStringCompare = linker.downcallHandle(
                        cf.find("CFStringCompare").orElseThrow(),
                        NsuiForeign.cfStringCompare());
                CFNotificationCenterGetDistributedCenter = linker.downcallHandle(
                        cf.find("CFNotificationCenterGetDistributedCenter").orElseThrow(),
                        NsuiForeign.cfNotificationCenterGetDistributedCenter());
                CFNotificationCenterGetDarwinNotifyCenter = linker.downcallHandle(
                        cf.find("CFNotificationCenterGetDarwinNotifyCenter").orElseThrow(),
                        NsuiForeign.cfNotificationCenterGetDarwinNotifyCenter());
                CFNotificationCenterAddObserver = linker.downcallHandle(
                        cf.find("CFNotificationCenterAddObserver").orElseThrow(),
                        NsuiForeign.cfNotificationCenterAddObserver());
                CFNotificationCenterRemoveObserver = linker.downcallHandle(
                        cf.find("CFNotificationCenterRemoveObserver").orElseThrow(),
                        NsuiForeign.cfNotificationCenterRemoveObserver());
                CFGetTypeID = linker.downcallHandle(
                        cf.find("CFGetTypeID").orElseThrow(),
                        NsuiForeign.cfGetTypeID());
                MethodHandle cStrType = linker.downcallHandle(
                        cf.find("CFStringGetTypeID").orElseThrow(),
                        NsuiForeign.cfStringGetTypeID());
                CACHED_STRING_TYPE_ID = (long) cStrType.invoke();
                CFStringGetTypeID = cStrType;

                kCFPreferencesAnyApplication = cf.find("kCFPreferencesAnyApplication")
                        .map(seg -> seg.reinterpret(PTR.byteSize()).get(ValueLayout.ADDRESS, 0))
                        .orElseThrow();

                // Allocate and cache CFString refs on global heap
                try (Arena tempArena = Arena.ofConfined()) {
                    preferenceKeyCf = (MemorySegment) CFStringCreateWithCString.invoke(
                            MemorySegment.NULL, tempArena.allocateFrom(PREFERENCE_KEY), 0x08000100);
                    darkCf = (MemorySegment) CFStringCreateWithCString.invoke(
                            MemorySegment.NULL, tempArena.allocateFrom("Dark"), 0x08000100);
                    globalNotificationName = (MemorySegment) CFStringCreateWithCString.invoke(
                            MemorySegment.NULL, tempArena.allocateFrom(NOTIFICATION_NAME), 0x08000100);
                    wakeNotificationName = (MemorySegment) CFStringCreateWithCString.invoke(
                            MemorySegment.NULL, tempArena.allocateFrom(WAKE_NOTIFICATION_NAME), 0x08000100);
                }

                // Build static callback stub on global arena (ObjC-style lazy like NSView upcall)
                MethodHandle callbackHandle = MethodHandles.lookup().findStatic(
                        ThemeObserver.class,
                        "staticThemeChangedCallback",
                        MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class, MemorySegment.class));
                callbackStub = linker.upcallStub(callbackHandle, NsuiForeign.themeUpcall(), Arena.global());

                initDone = true;
            } catch (Throwable t) {
                throw new RuntimeException("ThemeObserver ensureInit failed", t);
            }
        }
    }

    // ---- public static facade ----

    /// Query CoreFoundation for current dark mode — same logic as MacTheme.queryIsDark()
    public static boolean queryIsDark() {
        try {
            ensureInit();
        } catch (Throwable t) {
            return false;
        }
        MemorySegment value = MemorySegment.NULL;
        try {
            value = (MemorySegment) CFPreferencesCopyAppValue.invoke(preferenceKeyCf, kCFPreferencesAnyApplication);
            if (value == null || value.equals(MemorySegment.NULL)) {
                return false;
            }
            long typeId = (long) CFGetTypeID.invoke(value);
            if (typeId != CACHED_STRING_TYPE_ID) {
                return false;
            }
            return (long) CFStringCompare.invoke(value, darkCf, 0L) == 0;
        } catch (Throwable t) {
            return false;
        } finally {
            safeRelease(value);
        }
    }

    private static void safeRelease(MemorySegment seg) {
        if (seg != null && !seg.equals(MemorySegment.NULL)) {
            try {
                CFRelease.invoke(seg);
            } catch (Throwable ignored) {}
        }
    }

    public static boolean isDark() {
        return getInstance().isDarkInstance();
    }

    public boolean isDarkInstance() {
        return lastKnownDark;
    }

    public static boolean forceCheckIsDark() {
        return getInstance().forceCheckIsDarkInstance();
    }

    public boolean forceCheckIsDarkInstance() {
        boolean dark = queryIsDark();
        lastKnownDark = dark;
        return dark;
    }

    public static void registerListener(Consumer<Boolean> l) {
        getInstance().registerListenerInstance(l);
    }

    public void registerListenerInstance(Consumer<Boolean> listener) {
        if (listener == null) return;
        // lazy start
        if (!initialized) {
            synchronized (lifecycleLock) {
                if (!initialized) {
                    startNativeObserver();
                    initialized = true;
                }
            }
        }
        listeners.add(listener);
    }

    public static void removeListener(Consumer<Boolean> l) {
        getInstance().removeListenerInstance(l);
    }

    public void removeListenerInstance(Consumer<Boolean> listener) {
        if (listener == null) return;
        listeners.remove(listener);
    }

    public static void dispose() {
        getInstance().disposeInstance();
    }

    public void disposeInstance() {
        synchronized (lifecycleLock) {
            if (initialized) {
                stopNativeObserver();
                initialized = false;
            }
        }
    }

    /// Trigger async re-check (bounce to executor) — exposed for tests
    public static void triggerAsyncCheck() {
        getInstance().triggerAsyncCheckInstance();
    }

    public void triggerAsyncCheckInstance() {
        synchronized (lifecycleLock) {
            ExecutorService exec = themeExecutor;
            if (exec != null && !exec.isShutdown()) {
                exec.execute(this::asyncCheckAndUpdate);
            } else {
                // Observer disposed/inactive; run on generic worker to avoid blocking caller
                CompletableFuture.runAsync(() -> {
                    boolean currentValue = queryIsDark();
                    synchronized (lifecycleLock) {
                        if (currentValue != lastKnownDark) {
                            lastKnownDark = currentValue;
                            for (var listener : listeners) {
                                try { listener.accept(currentValue); } catch (Throwable ignored) {}
                            }
                        }
                    }
                });
            }
        }
    }

    // ---- native observer lifecycle ----

    private void startNativeObserver() {
        ensureInit();
        try {
            this.themeExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ThemeObserver-Worker");
                t.setDaemon(true);
                t.setContextClassLoader(null);
                return t;
            });
            this.lastKnownDark = queryIsDark();

            // 1. Subscribe to Active Theme Changes (Distributed Center)
            CFNotificationCenterAddObserver.invoke(
                    CFNotificationCenterGetDistributedCenter.invoke(),
                    callbackStub,
                    callbackStub,
                    globalNotificationName,
                    MemorySegment.NULL,
                    4L
            );
            // 2. Subscribe to Hardware/Power Wake Transitions (Darwin Center)
            CFNotificationCenterAddObserver.invoke(
                    CFNotificationCenterGetDarwinNotifyCenter.invoke(),
                    callbackStub,
                    callbackStub,
                    wakeNotificationName,
                    MemorySegment.NULL,
                    0L
            );
        } catch (Throwable t) {
            if (themeExecutor != null) {
                themeExecutor.shutdown();
                themeExecutor = null;
            }
            throw new IllegalStateException("Failed to initialize native theme observer", t);
        }
    }

    private void stopNativeObserver() {
        try {
            if (callbackStub != null && !callbackStub.equals(MemorySegment.NULL)) {
                CFNotificationCenterRemoveObserver.invoke(
                        CFNotificationCenterGetDistributedCenter.invoke(),
                        callbackStub,
                        globalNotificationName,
                        MemorySegment.NULL
                );
                CFNotificationCenterRemoveObserver.invoke(
                        CFNotificationCenterGetDarwinNotifyCenter.invoke(),
                        callbackStub,
                        wakeNotificationName,
                        MemorySegment.NULL
                );
            }
        } catch (Throwable ignored) {
        } finally {
            if (themeExecutor != null) {
                themeExecutor.shutdown();
                try { themeExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
                themeExecutor = null;
            }
        }
    }

    // ---- upcall ----

    /// Static upcall entry — registered in NsuiFeature for native-image
    public static void staticThemeChangedCallback(MemorySegment center, MemorySegment observer, MemorySegment name, MemorySegment object, MemorySegment userInfo) {
        try {
            getInstance().themeChangedCallback(center, observer, name, object, userInfo);
        } catch (Throwable ignored) {}
    }

    private void themeChangedCallback(MemorySegment center, MemorySegment observer, MemorySegment name, MemorySegment object, MemorySegment userInfo) {
        if (!initialized) return;
        try {
            ExecutorService exec = themeExecutor;
            if (exec != null && !exec.isShutdown()) {
                exec.execute(this::asyncCheckAndUpdate);
            }
        } catch (Throwable ignored) {}
    }

    private void asyncCheckAndUpdate() {
        synchronized (lifecycleLock) {
            if (!initialized) return;
            boolean currentValue = queryIsDark();
            if (currentValue != lastKnownDark) {
                lastKnownDark = currentValue;
                for (var listener : listeners) {
                    try { listener.accept(currentValue); } catch (Throwable ignored) {}
                }
            }
        }
    }

    // For testing: expose whether observer started
    public static boolean isInitialized() {
        return getInstance().initialized;
    }
}
