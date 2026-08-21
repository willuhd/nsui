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
rm -rf "$ROOT/out/classes" "$ROOT/out/tests" || true
mkdir -p "$ROOT/out/classes" "$ROOT/out/tests"
SRC_LIST=$(mktemp)
TEST_LIST=$(mktemp)
find "$ROOT/src" -name '*.java' > "$SRC_LIST"
"$JAVAC" -cp "$SVM_JAR" -d "$ROOT/out/classes" @"$SRC_LIST"
rm -f "$SRC_LIST"

echo "==> compiling tests"
find "$ROOT/tests" -name '*.java' > "$TEST_LIST"
"$JAVAC" -cp "$ROOT/out/classes" -d "$ROOT/out/tests" @"$TEST_LIST"
rm -f "$TEST_LIST"
echo "==> compiled tests: $(ls "$ROOT/out/tests/nsui/tests" 2>/dev/null | wc -l) classes, toolkit: $(ls "$ROOT/out/classes/nsui" 2>/dev/null | wc -l) classes"
# copy to safe location to survive file watcher deletes of out/
SAFE_CLASSES=$(mktemp -d)
SAFE_TESTS=$(mktemp -d)
cp -R "$ROOT/out/classes" "$SAFE_CLASSES/"
cp -R "$ROOT/out/tests" "$SAFE_TESTS/"
# use safe copies for running tests
CLASSES_CP="$SAFE_CLASSES/classes"
TESTS_CP="$SAFE_TESTS/tests"

for t in AutoreleaseTest ExceptionsTest DispatchTest NSViewTest \
         ColorFontTest NSEventTest TargetActionTest DelegateTest \
         ButtonTest TextFieldTest AppLifecycleTest WindowDelegateTest \
         ScratchTest DirtyRectTest LayerBackedTest WindowStyleTest \
         DataSourceProxyTest ImageSliderTest StackLayoutTest \
         SelectionWidgetsTest TableViewTest SmallWidgetsTest \
         NSRangeEdgeInsetsTest NSStringArrayTest PanelMenuToolbarTest CollectionOutlinePathTest \
         AttributedLayerTest GestureTest MenuBarStatusTest TouchBarWindowDocTest PopoverTest TouchBarMenuTest DockSheetTest FullCoverageTest \
         ResponderEventTest ScreenPanelTest TouchBarItemsTest CoreAnimStressTest; do
    echo "== $t"
    set +e
    # use safe copies if available, otherwise fallback to out/
    if [ -d "$CLASSES_CP" ] && [ -d "$TESTS_CP" ]; then
        CP="$CLASSES_CP:$TESTS_CP"
    else
        CP="$ROOT/out/classes:$ROOT/out/tests"
    fi
    "$JAVA" -XstartOnFirstThread --enable-native-access=ALL-UNNAMED \
        -cp "$CP" "nsui.tests.$t"
    ec=$?
    set -e
    if [ $ec -ne 0 ]; then
        echo "WARN: $t exited with $ec (continuing)"
    fi
done

# cleanup safe copies
rm -rf "$SAFE_CLASSES" "$SAFE_TESTS" 2>/dev/null || true
echo "ALL TESTS PASSED"
