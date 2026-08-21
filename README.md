# nsui

Pure Java bindings for AppKit (along with Cocoa, CoreAnimation, and utils) using the FFM API. No external libraries (no AWT/Swing, SWT, JNI/JNA, JavaFX, etc). Builds on GraalVM native-image with no tracing agent required. JDK 25. 

### Overview

NSUI talks directly to the ObjC runtime. Every AppKit call is an `objc_msgSend` downcall with an exact signature. Java methods exposed as ObjC methods are installed via `objc_allocateClassPair` and `class_addMethod` using FFM upcall stubs.

- One file per AppKit class. Wrappers hold a single `MemorySegment` peer. No cached state.
- `nsui` package contains AppKit wrappers, and `nsui.objc` contains low-level bindings.
- New selectors with a known signature require no new code. Unknown signatures fail with the exact `Sig` entry to add.

## Build and test

```bash
./build.sh   # compiles to out/classes
./tests.sh   # compiles and runs test suite on JVM
```

If running on the JVM, these 2 flags are needed:

```bash
java -XstartOnFirstThread --enable-native-access=ALL-UNNAMED -cp out/classes Main
```

* `-XstartOnFirstThread` runs the main method on the main thread (AppKit requirement). Not possible in code (for reference, AWT has exclusive access to JVM's internal threading). native-image will start on the main thread automatically.
* `--enable-native-access=ALL-UNNAMED` allows FFM restricted methods. Also not required for native binaries.

### Native image

The native build must know all foreign signatures at build time. This project registers them via a feature (`NsuiFeature`).

```bash
native-image --features=nsui.objc.NsuiFeature \
  --enable-native-access=ALL-UNNAMED \
  --initialize-at-run-time=nsui.objc.ObjC,nsui.objc.WindowCheck \
  -cp out/classes -o out/app Main
```

No tracing agent or JSON metadata is required. An optional agent path exists for comparison (`MODE=agent ./build.sh`), but the default build uses no `reachability-metadata.json` and no reflection configuration.

If a signature is missing, the build succeeds but the binary fails at runtime with `MissingForeignRegistrationError`. If developing, add the suggested `Sig` entry. 

### Runtime details

- Frameworks are loaded explicitly with `dlopen` at runtime. `objc_getClass` returns `NULL` until the framework is loaded.
- Structs are passed by value and returned via `objc_msgSend_stret` on x86_64. Group returns use an implicit `SegmentAllocator` prefix.
- Memory uses a global arena. Per-turn inputs (rects, C strings) use a thread-local bump arena. Struct returns use the global arena.
- Window existence is verified via `CGWindowListCopyWindowInfo` from CoreGraphics.

### Project status

Library package is `nsui`. Coverage includes core AppKit classes, controls, menus, status items, popovers, and QuartzCore layers. In development (currently at 112 NS classes, 2 Core Animation classes).

More support expected in the near future, with the goal of having full `NS.*` coverage and potential Metal integration.
