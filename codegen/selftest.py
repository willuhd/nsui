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
import re
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
    "MTLDepthStencilDescriptor": 100.0,
    "NSOrderedSet": 93.6,
    "NSGestureRecognizer": 92.6,
    "NSArray": 47.2,        # recall: 25 of 53 shipped names, the rest hand-written affordances
    "NSViewController": 88.6,
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
        # A crashed producer writes nothing (gen.py only opens patch_out at the end), so a
        # previous run's file would compare equal to itself and read as a pass. Drop first,
        # like every other stage that consumes a produced file (1ad1915).
        _drop(out)
        r = run([sys.executable, os.path.join(HERE, "gen.py"), "--repo", c,
                 "--class", "NSMatrix", "--class", "NSCell", "--framework", "AppKit",
                 "--patch-out", out])
        outs.append(open(out, "rb").read() if os.path.exists(out) else b"")
    check("determinism: two runs emit identical bytes", outs[0] == outs[1] and len(outs[0]) > 0,
          "%d bytes" % len(outs[0]))
    r = run([sys.executable, os.path.join(HERE, "gen.py"), "--repo", c, "--class", "NSMatrix",
             "--class", "NSCell", "--framework", "AppKit", "--apply", "--allow-drop"])
    r = run([sys.executable, os.path.join(HERE, "gen.py"), "--repo", c, "--class", "NSMatrix",
             "--class", "NSCell", "--framework", "AppKit", "--check"])
    check("determinism: --check agrees with a freshly generated tree", r.returncode == 0,
          "exit %d" % r.returncode)


def _drop(path):
    # A producer that crashes must not leave the previous run's output behind for the
    # consumer to trust: remove the expected file first so a stale artifact reads as
    # "not produced", never as a pass.
    try:
        os.remove(path)
    except FileNotFoundError:
        pass


def check_coverage():
    classes = []
    for c in COVERAGE:
        classes += ["--class", c]
    out = os.path.join(bench.REPORTS, "matrix.json")
    _drop(out)
    r = run([sys.executable, os.path.join(HERE, "bench.py"), "--matrix", "--no-gate"] + classes)
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
    cmd = [sys.executable, os.path.join(HERE, "gen.py"), "--repo", where, "--apply", "--allow-drop",
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
    # The gate scans Path.of("src","nsui") relative to cwd: it must run with cwd=where
    # (the batch tree, 5917 checked / 416 noMatch). The caller's cwd (REPO, 4565 / 244)
    # measures the live repo instead and reads as a pass while checking nothing new.
    # The batch baseline lives in codegen/surface-baseline.json (no-drop: checked is a
    # floor so a wrong-cwd run fails, noMatch is a cap so growth fails, mismatches stay 0).
    baseline = {}
    try:
        with open(os.path.join(HERE, "surface-baseline.json"), encoding="utf-8") as fh:
            baseline = json.load(fh)
    except (OSError, ValueError):
        pass
    counts = baseline.get("counts", {}) if isinstance(baseline, dict) else {}
    try:
        batchNoMatch = int(counts.get("noMatch", 416))
    except (TypeError, ValueError):
        batchNoMatch = 416
    try:
        batchCheckedFloor = int(counts.get("checked", 5917))
    except (TypeError, ValueError):
        batchCheckedFloor = 5917
    g = subprocess.run([JAVA, "-XstartOnFirstThread", "--enable-native-access=ALL-UNNAMED",
                        "-Dsignature.noMatchBaseline=%d" % batchNoMatch,
                        "-cp", out, "nsui.tests.SignatureConformanceTest"],
                       capture_output=True, text=True, cwd=where)
    text = g.stdout + g.stderr
    mChecked = re.search(r"call sites checked:\s*(\d+)", text)
    mNoMatch = re.search(r"no runtime method:\s*(\d+)", text)
    mBad = re.search(r"ABI mismatches:\s*(\d+)", text)
    gotChecked = int(mChecked.group(1)) if mChecked else -1
    gotNoMatch = int(mNoMatch.group(1)) if mNoMatch else -1
    gotBad = int(mBad.group(1)) if mBad else -1
    check("surface: batch tree measured (cwd=where, %d checked / %d noMatch / %d MISMATCH)"
          % (gotChecked, gotNoMatch, gotBad),
          gotChecked >= batchCheckedFloor and gotNoMatch <= batchNoMatch and gotBad == 0,
          "baseline %d checked floor / %d noMatch cap / 0 MISMATCH" % (batchCheckedFloor, batchNoMatch))
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


def check_guards():
    """Every measured abort must end as a Java exception, and a tolerated call must stay tolerated.

    This is the only check that runs the wrapper against the real framework and reads exit codes,
    so it is what proves the emitted guards do something rather than merely compile.
    """
    out = os.path.join(bench.REPORTS, "selftest-guards.json")
    _drop(out)
    r = run([sys.executable, os.path.join(HERE, "probe.py"), "--repo", REPO, "--generated",
             "NSViewController", "NSMenu", "NSView", "NSClipView", "--out", out],
            timeout=900)
    if not os.path.exists(out):
        check("guards: probe produced measurements", False, (r.stdout + r.stderr)[-160:])
        return
    measured = {c["id"]: c for c in json.load(open(out))["cases"]}
    ev = json.load(open(os.path.join(HERE, "guard-evidence.json")))["cases"]
    by_class = {}
    for case in ev:
        by_class.setdefault(case["class"], []).append(case)
    mismatches = []
    for case in ev:
        pid = case.get("probe_id")
        if not pid or pid not in measured:
            continue
        got = measured[pid]["verdict"]
        if case.get("guards"):
            if got != "java-exception":
                mismatches.append("%s:%s expected java-exception, got %s"
                                  % (case["class"], case["selector"], got))
        elif case.get("control") and got != "tolerated":
            mismatches.append("%s:%s control expected tolerated, got %s"
                              % (case["class"], case["selector"], got))
    check("guards: measured aborts now raise, controls still pass", not mismatches,
          "; ".join(mismatches)[:150])


def check_enumerated_guards():
    """Derived candidates must stop aborting once the evidence guards them.

    Re-deriving the candidates and measuring them again is the check that the emitted guards are
    real: an abort that still carries a guard statement is a guard that does not work.
    """
    out = os.path.join(bench.REPORTS, "selftest-enum.json")
    _drop(out)
    r = run([sys.executable, os.path.join(HERE, "probe.py"), "--enumerate", "NSMenu",
             "--out", out], timeout=1800)
    if not os.path.exists(out):
        check("guards: enumeration ran", False, (r.stdout + r.stderr)[-160:])
        return
    cases = json.load(open(out))["cases"]
    aborts = [c for c in cases if c["unguarded"] in ("objc-abort", "sigtrap", "sigill")]
    broken = [c for c in aborts if c.get("guards")]
    check("guards: no guarded candidate still aborts", not broken,
          "; ".join("%s %s" % (c["selector"], c["unguarded"]) for c in broken)[:140])


def check_rotating_verification():
    """Re-measure a rotating slice of the evidence WITHOUT the guards and check the verdicts.

    The invariant that must hold is the recorded one: a case the evidence calls an abort still
    aborts when no guard is emitted, and a control still survives. Whether a guard is emitted at
    all is a policy decision (the shipped wrapper may tolerate the argument, or the corpus bound may
    not resolve on the generated class), so this check measures the bare tree and leaves the
    emission rules to the differential.
    """
    ev = json.load(open(os.path.join(HERE, "guard-evidence.json")))["cases"]
    aborts = {}
    cases_per_class = {}
    for c in ev:
        cases_per_class[c["class"]] = cases_per_class.get(c["class"], 0) + 1
        if c.get("unguarded") in ("objc-abort", "sigtrap", "sigill"):
            aborts[c["class"]] = aborts.get(c["class"], 0) + 1
    ranked = sorted(aborts)
    if not ranked:
        check("verification: the evidence holds aborts to verify", False, "none recorded")
        return
    # Rotate around the name-sorted list, taking whole classes until a case budget is reached. One
    # JVM per case means the budget is what bounds the time: sampling the class with the most
    # aborts made this stage 246s of a 348s suite, while sampling a couple of aborts at a time left
    # the corpus barely covered. The budget gives both, and the rotation covers every class in turn.
    budget = 60
    # Rotate per run, not per hour: several runs in one round would otherwise re-measure the same
    # classes, and the rotation is what gives the corpus coverage over time.
    state = os.path.join(bench.REPORTS, "selftest-rotation.json")
    try:
        offset = int(json.load(open(state)).get("offset", 0))
    except (OSError, ValueError, AttributeError):
        offset = 0
    os.makedirs(bench.REPORTS, exist_ok=True)
    json.dump({"offset": (offset + 1) % len(ranked)}, open(state, "w"))
    start = offset % len(ranked)
    sample, total = [], 0
    for i in range(len(ranked)):
        cls = ranked[(start + i) % len(ranked)]
        n = cases_per_class.get(cls, 0)
        if sample and total + n > budget:
            continue
        sample.append(cls)
        total += n
        if total >= budget // 2 and len(sample) >= 2:
            break
    if not sample:
        sample = [ranked[start]]
    out = os.path.join(bench.REPORTS, "selftest-verify.json")
    _drop(out)
    r = run([sys.executable, os.path.join(HERE, "probe.py"), "--enumerate"] + sample +
            ["--no-evidence", "--record-all", "--out", out], timeout=1800)
    if not os.path.exists(out):
        check("verification: rotating sample measured", False, (r.stdout + r.stderr)[-160:])
        return
    fresh = {(c.get("id") or c.get("probe_id")): c for c in json.load(open(out))["cases"]}
    wrong, checked, unchecked = [], 0, 0
    for c in ev:
        if c.get("unguarded") not in ("objc-abort", "sigtrap", "sigill"):
            continue
        f = fresh.get(c.get("probe_id"))
        if f is None:
            unchecked += 1
            continue
        checked += 1
        if f["unguarded"] not in ("objc-abort", "sigtrap", "sigill"):
            wrong.append("%s recorded %s, measured %s"
                         % (c["probe_id"], c["unguarded"], f["unguarded"]))
    # Say how much of the corpus this rotation actually re-measured, so a sample that checks very
    # little is visible rather than looking like a pass.
    notes.append("rotating sample: re-measured %d of %d recorded aborts (%d outside this sample)"
                 % (checked, checked + unchecked, unchecked))
    check("verification: rotating sample (%s) reproduces its verdicts" % ", ".join(sample),
          not wrong, "; ".join(wrong)[:150])


def check_flaw_patch():
    """The proposed guard patch must be emitted and apply cleanly to the current tree.

    The tool never edits src/, so this is the check that its output is usable: a diff that does not
    apply is worth nothing to the maintainer.
    """
    patch_path = os.path.join(bench.REPORTS, "selftest-guard.patch")
    _drop(patch_path)
    r = run([sys.executable, os.path.join(HERE, "flaws.py"), "--check", "F3", "--patch", patch_path])
    if not os.path.exists(patch_path) or os.path.getsize(patch_path) == 0:
        check("flaws: proposed guard patch emitted", False, (r.stdout + r.stderr)[-160:])
        return
    where = fresh_copy(os.path.join(bench.WORK, "selftest-patch"))
    p = subprocess.run(["patch", "-p1", "--dry-run", "--quiet"], cwd=where,
                       stdin=open(patch_path, "rb"), capture_output=True, text=True)
    check("flaws: proposed guard patch applies cleanly", p.returncode == 0,
          (p.stdout + p.stderr)[-140:].replace("\n", " "))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--quick", action="store_true", help="skip the surface batch and history sweep")
    args = ap.parse_args()
    t0 = time.time()
    print("=== CodeGen self-test (version %s) ===" % __import__("gen").VERSION)
    # Time each stage: the suite grew to several minutes and the cost was invisible, so a slow
    # stage could not be told from a slow machine.
    def timed(fn):
        t = time.time()
        fn()
        print("    ... %-28s %5.1fs" % (fn.__name__, time.time() - t))

    timed(check_determinism)
    timed(check_coverage)
    if not args.quick:
        timed(check_surface_batch)
        timed(check_guards)
        timed(check_enumerated_guards)
        timed(check_rotating_verification)
        timed(check_flaw_patch)
        timed(check_early_sweep)
    for n in notes:
        print("note: " + n)
    print("\n%d check(s) failed in %.1fs" % (len(failures), time.time() - t0))
    for f in failures:
        print("  FAILED: " + f)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
