#!/usr/bin/env bash
#
# NSUI3 — pure-Java FFM toolkit build (library, package nsui).
# Demos / Main.java live *outside* git — they import nsui.* from out/classes.
#
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="$ROOT/out"
MODE="${MODE:-feature}"

GRAALVM="${GRAALVM:-/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home}"
JAVAC="$GRAALVM/bin/javac"
JAVA="$GRAALVM/bin/java"
NATIVE_IMAGE="$GRAALVM/bin/native-image"
SVM_JAR="$GRAALVM/lib/svm/builder/svm.jar"     # hosted API (Feature) for javac

mkdir -p "$OUT/classes"

echo "==> compiling toolkit (library, package nsui)"
"$JAVAC" -cp "$SVM_JAR" -d "$OUT/classes" $(find "$ROOT/src" -name '*.java')
echo "==> compiled to $OUT/classes"

if [ "${1:-}" = "run-jvm" ]; then
    echo "Note: demos live outside git. Example:"
    echo "  javac -cp $OUT/classes -d out/demo /path/to/MyMain.java"
    echo "  \$JAVA -XstartOnFirstThread --enable-native-access=ALL-UNNAMED -cp $OUT/classes:out/demo MyMain --smoke"
    exit 0
fi

if [ "$MODE" = "agent" ]; then
    echo "MODE=agent requires an external Main. Example:"
    echo "  MODE=agent ./build.sh /path/to/MyMain.java  (not yet wired for library-only)"
    exit 0
fi

echo ""
echo "To build a native demo outside git:"
echo "  native-image --features=nsui.objc.NsuiFeature \\"
echo "      --enable-native-access=ALL-UNNAMED \\"
echo "      --initialize-at-run-time=nsui.objc.ObjC,nsui.objc.WindowCheck \\"
echo "      -cp out/classes:/path/to/demo/classes -o out/demo MyMain"
