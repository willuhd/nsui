#!/usr/bin/env bash
#
# NSUI3 — JVM test runner (library, package nsui).
# Compiles the toolkit + tests from scratch and runs the full suite.
#
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRAALVM="${GRAALVM:-/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home}"
JAVAC="$GRAALVM/bin/javac"
JAVA="$GRAALVM/bin/java"
SVM_JAR="$GRAALVM/lib/svm/builder/svm.jar"   # hosted API (NsuiFeature) for javac

echo "==> compiling toolkit (clean)"
rm -rf "$ROOT/out/classes" "$ROOT/out/tests"
mkdir -p "$ROOT/out/classes" "$ROOT/out/tests"
"$JAVAC" -cp "$SVM_JAR" -d "$ROOT/out/classes" $(find "$ROOT/src" -name '*.java')

echo "==> compiling tests"
"$JAVAC" -cp "$ROOT/out/classes" -d "$ROOT/out/tests" $(find "$ROOT/tests" -name '*.java')

for t in AutoreleaseTest ExceptionsTest DispatchTest NSViewTest \
         ColorFontTest NSEventTest TargetActionTest DelegateTest \
         ButtonTest TextFieldTest AppLifecycleTest WindowDelegateTest \
         ScratchTest DirtyRectTest LayerBackedTest WindowStyleTest \
         DataSourceProxyTest ImageSliderTest StackLayoutTest \
         SelectionWidgetsTest TableViewTest SmallWidgetsTest; do
    echo "== $t"
    "$JAVA" -XstartOnFirstThread --enable-native-access=ALL-UNNAMED \
        -cp "$ROOT/out/classes:$ROOT/out/tests" "nsui.tests.$t"
done

echo "ALL TESTS PASSED"
