#!/usr/bin/env bash
#
# NSUI3 — JVM test runner (library, package nsui).
# Compiles the toolkit + tests from scratch and runs the full suite.
#
# Usage:
#   ./tests.sh                 # all tests, parallel jobs, honest exit code (0 iff all pass)
#   ./tests.sh Bench           # only tests whose name contains "Bench"
#   ./tests.sh -j 4            # all tests on 4 jobs
#   ./tests.sh Bench -j 1      # filtered tests, one job (sequential)
#   ./tests.sh --timeout 60    # per-test timeout 60s (default 300s)
#
# Convention: every test is a plain main() in its own JVM (no JUnit),
# sharing the TestKit primitives (reporting, unobtrusive windows, pump,
# nanoTime benchmarks). Windows stay hidden unless a test proves it needs
# visibility; only NSEventTest/ButtonTest take key status.
# Parallelism is process-level (one JVM per test, pooled launcher threads
# in nsui.tests.RunSuite): a native abort still fails only its own test.
#
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
GRAM_VM_DIR=${GRAALVM:-/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home}
JAVAC="$GRAM_VM_DIR/bin/javac"
JAVA="$GRAM_VM_DIR/bin/java"
SVM_JAR="$GRAM_VM_DIR/lib/svm/builder/svm.jar"
echo "==> compiling toolkit (clean)"
rm -rf "$ROOT/out/classes" "$ROOT/out/tests" "$ROOT/out/logs" || true
mkdir -p "$ROOT/out/classes" "$ROOT/out/tests" "$ROOT/out/logs"
SRC_LIST=$(mktemp)
TEST_LIST=$(mktemp)
find "$ROOT/src" -name "*.java" > "$SRC_LIST"
"$JAVAC" -cp "$SVM_JAR" -d "$ROOT/out/classes" @"$SRC_LIST"
rm -f "$SRC_LIST"
echo "==> compiling tests"
find "$ROOT/tests" -name "*.java" > "$TEST_LIST"
"$JAVAC" -cp "$ROOT/out/classes" -d "$ROOT/out/tests" @"$TEST_LIST"
rm -f "$TEST_LIST"
echo "==> compiled"
# One compile, then the pooled launcher runs each test main in its own JVM.
# (No mid-run rebuilds, so the old snapshot-copy dance is unnecessary.)
# Default per-test timeout 300s (longest suite runs well under it; override
# with an explicit --timeout).
TIMEOUT_ARGS="--timeout 300"
for a in "$@"; do
    case "$a" in --timeout|--timeout=*) TIMEOUT_ARGS="" ;; esac
done
# shellcheck disable=SC2086
"$JAVA" -XstartOnFirstThread --enable-native-access=ALL-UNNAMED \
    -cp "$ROOT/out/classes:$ROOT/out/tests" nsui.tests.RunSuite \
    --classes "$ROOT/out/classes" --tests "$ROOT/out/tests" --logs "$ROOT/out/logs" $TIMEOUT_ARGS "$@"
