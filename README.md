# NSUI3 — a macOS UI toolkit in 100% pure Java (FFM → Cocoa)

A macOS GUI toolkit written entirely in Java, using **only the Foreign
Function & Memory API (FFM, JEP 454)** — no JNI, no JNA, no AWT/Swing/SWT, no
JavaFX, no third-party libraries. It talks to Cocoa directly through the
Objective-C runtime (`objc_msgSend` & friends) and compiles with GraalVM
`native-image` to a standalone native library/toolkit.

```
src/nsui/
├── NSApplication.java      L1: thin, SWT-style wrappers over AppKit classes —
├── NSWindow.java               one file per class, no state, no reflection;
├── NSMenu.java                 the peer MemorySegment is the identity
├── NSMenuItem.java
├── NSEvent.java
├── NSObject.java / NSRect.java / NSPoint.java / NSSize.java
├── NSView.java / NSStackView.java / NSControl.java
├── NSButton.java / NSTextField.java / ... (see src/nsui/*.java)
└── objc/                   L0: grouped low-level shims (knows nothing about L1)
    ├── ObjC.java           FFM bindings: dlopen, runtime C API, signature-keyed
    │                       dispatcher; typed msgSend* helpers call invokeExact
    ├── Sig.java            the message vocabulary — single source of truth:
    │                       record S(Ret, packed-arg-key) per signature shape
    ├── NsuiForeign.java    C-function descriptors (libobjc, CoreGraphics/CF)
    ├── NsuiFeature.java    native-image Feature: iterates the vocabulary —
    │                       registration set == vocabulary set, no drift
    └── WindowCheck.java    CoreGraphics/CF: proves a window is on screen
```

Layering rule: `objc/` never imports `nsui.*`; the L1 classes never touch
`FunctionDescriptor`/`Linker` directly. Adding a new selector with a known
signature costs zero new code; an unknown signature fails loudly with the exact
`Sig.of(...)` line to add to `Sig.VOCABULARY` — in both JVM and AOT modes.

> **Note:** This repository is a **library** (`package nsui`). Demo apps
> (`Main.java`, `*Demo.java`) live *outside* git — see `tests/` for
> self-contained examples that exercise the toolkit.

## Build & test

Requirements: GraalVM for JDK 25 (this project was developed with GraalVM
25.0.1 on macOS 15, x86_64) and Xcode command-line tools.

```bash
./build.sh              # javac -> out/classes
./tests.sh              # compile tests + run JVM suite
```

`**-XstartOnFirstThread` matters.** On macOS the `java` launcher runs your
`main()` on a *secondary* thread, so `pthread_main_np()` is 0 there and AppKit
throws `NSInternalInconsistencyException` ("NSWindow should only be
instantiated on the main thread!"). The flag makes the JVM run Java `main` on
the real first thread, exactly like a native binary does. Native-image
executables don't need it — the image's main thread *is* the process's first
thread.

## How it works

### The Objective-C runtime via FFM

Everything goes through libobjc, resolved at runtime with
`SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", arena)` — no build-time
linking, and it works in both HotSpot and native-image (GraalVM 25 supports
FFM in native image by default).

```java
// every ObjC call is an objc_msgSend downcall with a matching signature
id      app    = msgSend(cls("NSApplication"), sel("sharedApplication"));
id      win    = msgSend(alloc, sel("initWithContentRect:styleMask:backing:defer:"),
                         rect(0,0,800,600), 15L, 2L, false);
boolean visible = msgSendBool(win, sel("isVisible"));
```

Key details that mattered:

- **AppKit must be dlopen'ed explicitly.** The ObjC runtime does *not*
  auto-load frameworks: `objc_getClass("NSApplication")` returns NULL until
  `dlopen("/System/Library/Frameworks/AppKit.framework/AppKit")` runs.
- **No variadic calls.** `objc_msgSend` is declared with the exact signature of
  the method being called; FFM places arguments per the method's real ABI.
- **Structs by value and by return.** `NSRect` is a 4-double struct passed by
  value to `initWithContentRect:`. Struct *returns* (e.g. `[win frame]`) use
  `objc_msgSend_stret` on x86_64 and need the implicit `SegmentAllocator`
  leading parameter FFM adds for group-layout returns.
- **Upcalls: Java methods as ObjC methods.** A tiny class
  (`objc_allocateClassPair`/`objc_registerClassPair`) gets its methods installed
  with `class_addMethod` using FFM upcall stubs, so AppKit calls back into Java:
  `windowWillClose:` terminates the app, `applicationShouldTerminateAfterLastWindowClosed:`
  returns YES. This is what makes close-the-window → quit work.
- **Strict memory hygiene:** all segments live in a global arena; nothing is
  freed mid-run, no pointer tricks — FFM checks every access.

### Why native-image needs metadata — and why no tracing agent is required

Native-image compiles ahead of time and cannot see which
`Linker.downcallHandle(...)` signatures your code will create at run time, so
it must be told at *build time* which native call stubs to generate (the same
closed-world rule that forces reflection registration). Upcall targets are
invoked via method handles at run time and likewise must be registered.

There are two equivalent ways to provide this; **this project defaults to the
one that needs no tracing run**:

1. **`--features=nsui.objc.NsuiFeature` (default, no agent).**
   `NsuiFeature.duringSetup` registers every descriptor with
   `RuntimeForeignAccess.registerForDowncall(...)` and upcalls with
   `registerForDirectUpcall(MethodHandle, ...)`. `NsuiForeign` is the single
   source of truth shared with the runtime binding code, so the registered
   descriptors and the ones used at run time can never drift apart.

   ```bash
   native-image --features=nsui.objc.NsuiFeature \
       --enable-native-access=ALL-UNNAMED \
       --initialize-at-run-time=nsui.objc.ObjC,nsui.objc.WindowCheck \
       -cp out/classes -o out/nsui <YourMain>
   ```

2. **Tracing agent (`MODE=agent ./build.sh`).** Run the app once on the JVM
   with `-agentlib:native-image-agent=config-output-dir=out/meta`, then pass
   the generated `reachability-metadata.json` with
   `-H:ReachabilityMetadataResources=...`. This is purely a collector for the
   same information; the app works identically either way.

Both paths were verified end to end: the same binary behavior, including the
upcall quit path, was produced with `--features` alone (no agent config) and
with the agent-collected config.

**The default build is completely metadata-free.** `./build.sh` (feature mode)
uses no `reachability-metadata.json`, no `-H:ReachabilityMetadataResources`,
no tar: LICENSE: Cannot stat: No such file or directory
tracing agent, and no reflection config files — the repository contains
zero JSON. `out/meta` is only ever created by the optional `MODE=agent` path.
This was re-verified with a from-scratch build (wiped `out/classes`, deleted
`out/meta`, rebuild with only `--features=nsui.objc.NsuiFeature`).

Registration itself *is* strictly required — a negative control proved it: an
FFM downcall used in a native image with **no** registration builds fine but
fails at run time with

```
org.graalvm.nativeimage.MissingForeignRegistrationError: Cannot perform
downcall with leaf type (long,long)long. To allow this operation, add the
following to the 'foreign' section of 'reachability-metadata.json' ...
```

So the JSON metadata file is optional; the `Feature` (a Java class passed as
`--features=...`) is the registration mechanism the default build uses, and
`NsuiForeign` guarantees the registered descriptors can never drift from the
ones the runtime binding code uses.

### Verified on screen

The toolkit doesn't just claim a window exists — `WindowCheck` asks the window server:

```
[CGWindowList] 37 on-screen window(s) total
[CGWindowList] owner=nsui           windowNumber=30797  title=NSUI3 — Pure FFM Cocoa Window  bounds={624,129 800x628}  <-- OUR WINDOW
[window] post-pump state: isVisible=true isKeyWindow=true isMainWindow=true
```

`WindowCheck` calls `CGWindowListCopyWindowInfo` (CoreGraphics) via FFM and
finds the window owned by our PID, matching the `windowNumber` AppKit assigned.

## Notes

- `--enable-native-access=ALL-UNNAMED` is required at run time for FFM's
  restricted methods (it also exists as a JVM flag and as the `--enable-native-access`
  argument to `native-image`; the manifest attribute
  `Enable-Native-Access: ALL-UNNAMED` would work for the JAR path too).
- On x86_64, methods returning large structs go through `objc_msgSend_stret`;
  on arm64 one `objc_msgSend` handles everything — the code switches on
  `os.arch`.
- Demos are intentionally kept outside the repository: they live as
  standalone `Main.java` files that import `nsui.*` and run from a terminal
  while still getting a normal window, Dock presence, menu bar and focus.
  Wrapping into an