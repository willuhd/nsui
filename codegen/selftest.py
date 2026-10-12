#!/usr/bin/env python3
"""CodeGen self-test — the repeatable check that the generator still behaves.

Each check has a measured expectation; a failure means the generator regressed, not that the number
changed. Improvements are allowed (coverage may exceed the expectation); drops are not.

  python3 codegen/selftest.py --quick     # determinism + bench matrix (~1 min)
  python3 codegen/selftest.py             # adds the new-surface batch + early-history sweep

Nothing under src/ or tests/ is written: the checks work in copies under codegen/work/ or /tmp.
"""

import argparse
import json
import os
import subprocess
import sys
import tempfile
import time

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
sys.path.insert(0, HERE)
import bench    # noqa: E402
import sweep    # noqa: E402

GRAALVM = os.environ.get("GRAALVM", "/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home")
JAVAC = os.path.join(GRAALVM, "bin", "javac")
JAVA = os.path.join(GRAALVM, "bin", "java")
SVM_JAR = os.path.join(GRAALVM, "lib", "svm", "builder", "svm.jar")

# The classes that together exercise categories, the concrete-class family, struct returns,
# subclassing a shipped wrapper, and the C-array/uint32 paths.
SURFACE_BATCH = [
    "NSCell", "NSBrowser", "NSMatrix", "NSCalendar", "NSURLComponents", "NSXMLNode",
    "NSProgress", "NSDateComponents", "NSImageRep", "NSSplitViewItem", "NSSharingService",
    "NSSound", "NSItemProvider", "NSPathControlItem", "NSCollectionViewLayoutAttributes",
    "NSTextFinder", "NSSpeechSynthesizer", "NSWorkspace", "NSPDFPanel", "NSFileManager",
    "NSURL", "NSScrubber", "NSOutlineView", "NSTextField", "NSButton",
    # protocol-backed surfaces: these were unreachable until @protocol bodies were accepted
    "MTLDevice", "MTLTexture", "MTLRenderCommandEncoder", "MTLCommandQueue",
]

# class -> minimum coverage; a drop below the recorded value is a regression.
COVERAGE = {
    "MTLSamplerDescriptor": 100.0,
    "MTLDepthStencilDescriptor": 91.7,
    "NSOrderedSet": 100.0,
    "NSGestureRecognizer": 91.9,
    "NSArray": 69.8,        # C-array and NSError** parameters are skipped, not emitted raw
    "NSViewController": 86.5,  # commitEditing…:contextInfo: takes void *
    "CAPropertyAnimation": 100.0,
}

failures = []
notes = []


def check(name, ok, detail=""):
    print("%-58s %s%s" % (name, "PASS" if ok else "FAIL", ("  " + detail) if detail else ""))
    if not ok:
        failures.append(name + ("  " + detail if detail else ""))


def run(cmd, **kw):
    return subprocess.run(cmd, cwd=REPO, capture_output=True, text=True, **kw)


def fresh_copy(where):
    # Always start clean: a previous check leaves generated files behind, and a reused directory
    # makes the generator emit "no diff" instead of the patch under test.
    if os.path.exists(where):
        import shutil
        shutil.rmtree(where)
    os.makedirs(where, exist_ok=True)
    p = subprocess.Popen(["git", "archive", "HEAD"], cwd=REPO, stdout=subprocess.PIPE)
    subprocess.run(["tar", "-x", "-C", where], stdin=p.stdout)
    p.wait()
    return where


def check_determinism():
    c = fresh_copy(os.path.join(bench.WORK, "selftest-det"))
    outs = []
    for i in (1, 2):
        out = os.path.join(bench.WORK, "selftest-%d.patch" % i)
        r = run([sys.executable, os.path.join(HERE, "gen.py"), "--repo", c,
                 "--class", "NSMatrix", "--class", "NSCell", "--framework", "AppKit",
                 "--patch-out", out])
        outs.append(open(out, "rb").read() if os.path.exists(out) else b"")
    check("determinism: two runs emit identical bytes", outs[0] == outs[1] and len(outs[0]) > 0,
          "%d bytes" % len(outs[0]))
    r = run([sys.executable, os.path.join(HERE, "gen.py"), "--repo", c, "--class", "NSMatrix",
             "--class", "NSCell", "--framework", "AppKit", "--apply"])
    r = run([sys.executable, os.path.join(HERE, "gen.py"), "--repo", c, "--class", "NSMatrix",
             "--class", "NSCell", "--framework", "AppKit", "--check"])
    check("determinism: --check agrees with a freshly generated tree", r.returncode == 0,
          "exit %d" % r.returncode)


def check_coverage():
    classes = []
    for c in COVERAGE:
        classes += ["--class", c]
    r = run([sys.executable, os.path.join(HERE, "bench.py"), "--matrix", "--no-gate"] + classes)
    out = os.path.join(bench.REPORTS, "matrix.json")
    if not os.path.exists(out):
        check("coverage: bench matrix produced results", False, r.stdout[-200:])
        return
    data = json.load(open(out))
    rows = {row["class"]: row for row in data["rows"]}
    for cls, want in sorted(COVERAGE.items()):
        row = rows.get(cls)
        got = (row or {}).get("coverage")
        ok = row is not None and row.get("status") is None and got is not None and got >= want - 0.05
        check("coverage: %s >= %.1f%%" % (cls, want), ok,
              "got %s" % (("%.1f%%" % got) if got is not None else (row or {}).get("status")))


def check_surface_batch():
    where = fresh_copy(os.path.join(bench.WORK, "selftest-surface"))
    cmd = [sys.executable, os.path.join(HERE, "gen.py"), "--repo", where, "--apply",
           "--framework", "AppKit", "--framework", "Foundation", "--framework", "QuartzCore",
           "--framework", "Metal"]
    for c in SURFACE_BATCH:
        cmd += ["--class", c]
    r = run(cmd)
    adaptations = sum(1 for ln in r.stdout.splitlines() if "RENAMED" in ln or "RETYPED" in ln)
    notes.append("surface batch adaptations: %d" % adaptations)

    out = os.path.join(bench.WORK, "selftest-surface-classes")
    subprocess.run(["rm", "-rf", out])
    os.makedirs(out, exist_ok=True)
    src = [os.path.join(dp, f) for dp, _dn, fn in os.walk(os.path.join(where, "src", "nsui"))
           for f in fn if f.endswith(".java")]
    tests = [os.path.join(where, "tests", t) for t in ("TestKit.java", "SignatureConformanceTest.java")]
    tests = [t for t in tests if os.path.exists(t)]
    r = subprocess.run([JAVAC, "-cp", SVM_JAR, "-d", out] + src + tests,
                       capture_output=True, text=True)
    errs = [ln for ln in (r.stdout + r.stderr).splitlines() if ": error:" in ln]
    check("surface: %d generated classes compile" % len(SURFACE_BATCH), not errs,
          errs[0][:120] if errs else "")
    if errs or not tests:
        return
    g = subprocess.run([JAVA, "-XstartOnFirstThread", "--enable-native-access=ALL-UNNAMED",
                        "-cp", out, "nsui.tests.SignatureConformanceTest"],
                       capture_output=True, text=True)
    text = g.stdout + g.stderr
    check("surface: signature gate ALL PASS", "RESULT: ALL PASS" in text,
          next((ln for ln in text.splitlines() if ln.startswith("RESULT")), ""))


def check_early_sweep():
    r = run(["git", "log", "--reverse", "--format=%H", "--", "src/nsui/objc/ObjC.java"])
    commits = [c for c in r.stdout.split() if c]
    if not commits:
        check("history: an early snapshot exists", False, "no commit touches src/nsui/objc")
        return
    early = commits[0]
    r = run([sys.executable, os.path.join(HERE, "sweep.py"), "--commit", early, "--inject-vocab"])
    line = next((ln for ln in r.stdout.splitlines() if "errors in GENERATED" in ln), "")
    ok = "errors in GENERATED files=0" in line
    check("history: %s generates compilable wrappers" % early[:8], ok, line.strip()[:110])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--quick", action="store_true", help="skip the surface batch and history sweep")
    args = ap.parse_args()
    t0 = time.time()
    print("=== CodeGen self-test (version %s) ===" % __import__("gen").VERSION)
    check_determinism()
    check_coverage()
    if not args.quick:
        check_surface_batch()
        check_early_sweep()
    for n in notes:
        print("note: " + n)
    print("\n%d check(s) failed in %.1fs" % (len(failures), time.time() - t0))
    for f in failures:
        print("  FAILED: " + f)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
