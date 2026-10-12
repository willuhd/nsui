#!/usr/bin/env python3
"""CodeGen bench — measure the generator against the code that actually shipped.

The conformance question this answers: for a class that exists in the current tree, take a
snapshot of the commit BEFORE that class was added, let CodeGen generate it from the SDK
headers, apply the patch, run the snapshot's own signature gate, and diff the result against
the version a human/current-code wrote. Everything is reported per class; nothing in the live
tree is read except for comparison, and nothing outside codegen/work is ever written.

Usage:
  python3 codegen/bench.py --class NSItemBadge
  python3 codegen/bench.py --class NSItemBadge --class NSPathControlItem --commit <sha>
  python3 codegen/bench.py --list                 # classes with a detectable birth commit
  python3 codegen/bench.py --class X --keep       # keep the snapshot for inspection

Stdlib only, like the generator. Snapshots live in codegen/work/<sha>/ and are disposable.
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)               # nsui3 root
WORK = os.path.join(HERE, "work")
REPORTS = os.path.join(HERE, "reports")
GEN = os.path.join(HERE, "gen.py")

# Guard/feature markers the current wrappers carry. Counting them in the diff is how the bench
# turns "the generated file differs" into "the generated file is missing N guards / M version
# checks", which is the work list for the generator.
FEATURES = {
    "index guard": r"Bounds\.index\(",
    "range guard": r"Bounds\.range\(",
    "nonnull guard": r"Bounds\.nonNull\(",
    "nonNegative guard": r"Bounds\.nonNegative\(",
    "notNaN guard": r"Bounds\.notNaN\(",
    "indexSet guard": r"Bounds\.indexSet\(",
    "version gate": r"respondsToSelector\(",
    "type guard": r"isKindOfClass\(",
    "require* helper": r"require[A-Z]\w*\(",
    "self/ancestor cycle walk": r"requireNotSelfOrAncestor|superview\(\)|superlayer\(\)",
    "class-responds gate": r"requireClassSelector\(",
    "ensureFramework": r"ensureFramework\(",
    "Sels.populate": r"Sels\.populate\(\)",
    "Handles record": r"record Handles\(",
}


def run(cmd, cwd=None, check=True, capture=True):
    p = subprocess.run(cmd, cwd=cwd, capture_output=capture, text=True)
    if check and p.returncode != 0:
        raise RuntimeError("command failed (%d): %s\n%s\n%s"
                           % (p.returncode, " ".join(cmd), p.stdout[-2000:], p.stderr[-2000:]))
    return p


def git(*args, **kw):
    return run(["git"] + list(args), cwd=REPO, **kw)


def birth_commit(rel_path):
    """The commit that added rel_path, or None (e.g. present since the initial import)."""
    p = git("log", "--diff-filter=A", "--format=%H", "-1", "--", rel_path, check=False)
    return p.stdout.strip() or None


def list_candidates(limit=None):
    """Current wrapper classes whose birth commit is detectable and whose parent tree exists."""
    out = []
    src = os.path.join(REPO, "src", "nsui")
    for name in sorted(os.listdir(src)):
        if not name.endswith(".java"):
            continue
        rel = "src/nsui/" + name
        sha = birth_commit(rel)
        if not sha:
            continue
        parent = git("rev-parse", sha + "^", check=False)
        if parent.returncode != 0:
            continue
        out.append({"class": name[:-5], "added_by": sha[:8], "snapshot": parent.stdout.strip()})
        if limit and len(out) >= limit:
            break
    return out


def snapshot(commit, keep=False):
    """Extract `commit` into codegen/work/<sha>/ and git-init it so `git apply` works.

    Concurrency-safe so several bench runs can share a commit: the tree is extracted into a
    private temp dir and atomically renamed into place, with a lock file to serialise the
    extraction itself. A half-extracted snapshot can therefore never be observed.
    """
    short = commit[:12]
    dest = os.path.join(WORK, short)
    marker = os.path.join(dest, ".snapshot-complete")
    lock = dest + ".lock"
    os.makedirs(WORK, exist_ok=True)
    deadline = time.time() + 600
    while True:
        if os.path.exists(marker):
            return dest
        try:
            fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            break
        except FileExistsError:
            if time.time() > deadline:
                raise RuntimeError("timed out waiting for snapshot lock " + lock)
            time.sleep(0.2)
    try:
        if os.path.exists(marker):
            return dest
        tmp = dest + ".tmp-%d" % os.getpid()
        if os.path.exists(tmp):
            shutil.rmtree(tmp)
        os.makedirs(tmp, exist_ok=True)
        # git archive | tar -x keeps the live tree untouched and needs no worktree metadata
        p1 = subprocess.Popen(["git", "archive", commit], cwd=REPO, stdout=subprocess.PIPE)
        p2 = subprocess.Popen(["tar", "-x", "-C", tmp], stdin=p1.stdout)
        p1.stdout.close()
        if p2.wait() != 0 or p1.wait() != 0:
            raise RuntimeError("snapshot extraction failed for " + commit)
        # `git apply` from a directory that is not a repo root resolves paths against the live
        # root and silently skips; git init fixes it (documented in REPORT.md).
        git_init = subprocess.run(["git", "init", "-q"], cwd=tmp, capture_output=True, text=True)
        if git_init.returncode != 0:
            raise RuntimeError("git init failed in snapshot: " + git_init.stderr)
        subprocess.run(["git", "config", "user.email", "codegen@bench"], cwd=tmp, capture_output=True)
        subprocess.run(["git", "config", "user.name", "CodeGen bench"], cwd=tmp, capture_output=True)
        subprocess.run(["git", "add", "-A"], cwd=tmp, capture_output=True)
        subprocess.run(["git", "commit", "-qm", "pristine " + commit], cwd=tmp, capture_output=True)
        # The commit sha is the pristine reference every bench run resets to, so a run can never
        # observe a previous run's applied patch.
        base = subprocess.run(["git", "rev-parse", "HEAD"], cwd=tmp, capture_output=True,
                              text=True).stdout.strip()
        open(os.path.join(tmp, ".snapshot-complete"), "w").write(commit + "\n" + base + "\n")
        if os.path.exists(dest):
            shutil.rmtree(dest)
        os.replace(tmp, dest)
        return dest
    finally:
        os.close(fd)
        try:
            os.unlink(lock)
        except FileNotFoundError:
            pass


def bench_matrix(classes, commit=None, frameworks=("AppKit", "QuartzCore", "Foundation", "Metal"),
                 run_gates=False, gate_filter="Signature", out_json=None, out_md=None):
    """Bench many classes and summarise conformance. This is the automation entry point."""
    rows = []
    for cls in classes:
        try:
            r = bench_class(cls, commit=commit, frameworks=frameworks, run_gates=run_gates,
                            gate_filter=gate_filter)
        except Exception as exc:                      # a bench failure must not stop the matrix
            r = {"class": cls, "error": str(exc)}
        c = r.get("comparison") or {}
        shipped, generated = c.get("methods_current"), c.get("methods_generated")
        rows.append({
            "class": cls,
            "snapshot": r.get("snapshot"),
            "generated": bool(r.get("generated")),
            "gate": r.get("gate_passed"),
            "methods_shipped": shipped,
            "methods_generated": generated,
            "coverage": (round(100.0 * generated / shipped, 1)
                         if shipped and generated is not None else None),
            "features_missing": c.get("features_missing") or {},
            "error": r.get("error"),
        })
    rows.sort(key=lambda x: (x["coverage"] if x["coverage"] is not None else -1))
    if out_json:
        with open(out_json, "w") as fh:
            json.dump(rows, fh, indent=2, sort_keys=True)
    if out_md:
        with open(out_md, "w") as fh:
            fh.write("# CodeGen conformance matrix\n\n")
            fh.write("Generated from `codegen/bench.py --matrix`; each row takes a snapshot of the\n")
            fh.write("commit before the class existed, generates it, applies the patch and compares\n")
            fh.write("the method set and guard features against the shipped wrapper.\n\n")
            fh.write("| class | snapshot | gen | coverage | missing features |\n|---|---|---|---|---|\n")
            for r in rows:
                cov = "n/a" if r["coverage"] is None else "%.1f%%" % r["coverage"]
                miss = ", ".join("%s x%d" % (k, v) for k, v in sorted(r["features_missing"].items()))
                fh.write("| %s | %s | %s | %s | %s |\n" % (r["class"], r["snapshot"] or "-",
                                                          "yes" if r["generated"] else "NO", cov, miss or "-"))
            tot = [r for r in rows if r["coverage"] is not None]
            if tot:
                avg = sum(r["coverage"] for r in tot) / len(tot)
                full = sum(1 for r in tot if r["coverage"] >= 100 and not r["features_missing"])
                fh.write("\n**%d classes: mean method coverage %.1f%%, %d fully conformant.**\n"
                         % (len(tot), avg, full))
    return rows


def reset_snapshot(snap):
    """Return the snapshot to its pristine commit (keeping any build output in out/)."""
    marker = os.path.join(snap, ".snapshot-complete")
    lines = open(marker).read().split()
    base = lines[1] if len(lines) > 1 else "HEAD"
    subprocess.run(["git", "reset", "--hard", "-q", base], cwd=snap, capture_output=True)
    subprocess.run(["git", "clean", "-fdq", "-e", "out"], cwd=snap, capture_output=True)


def generate(snap, classes, frameworks, patch_out):
    cmd = [sys.executable, GEN, "--repo", snap]
    for c in classes:
        cmd += ["--class", c]
    for f in frameworks:
        cmd += ["--framework", f]
    cmd += ["--patch-out", patch_out]
    p = run(cmd, cwd=REPO, check=False)
    return p


def apply_patch(snap, patch):
    if not os.path.exists(patch) or os.path.getsize(patch) == 0:
        return False, "empty patch"
    p = run(["git", "apply", "--verbose", patch], cwd=snap, check=False)
    return p.returncode == 0, (p.stderr or p.stdout).strip()[-400:]


def run_gate(snap, gate_filter="Signature"):
    """Run the snapshot's own conformance gate. The snapshot has tests.sh + tests/."""
    if not os.path.exists(os.path.join(snap, "tests.sh")):
        return None, "snapshot has no tests.sh"
    p = run(["./tests.sh", gate_filter, "-j", "1"], cwd=snap, check=False)
    text = p.stdout + p.stderr
    m = re.search(r"PASSED: (\d+)\s+FAILED: (\d+)", text)
    verdict = m.group(0) if m else "no verdict"
    line = ""
    for ln in text.splitlines():
        if "call sites checked" in ln:
            line = ln.strip()
    return p.returncode == 0, (verdict + ("  |  " + line if line else ""))


def count_features(text):
    return {k: len(re.findall(v, text)) for k, v in FEATURES.items()}


def method_names(text):
    """Public method names, so coverage can be compared between generated and shipped files."""
    return set(re.findall(r"^\s*public\s+(?:static\s+)?[\w.<>\[\]]+\s+(\w+)\s*\(", text, re.M))


def compare(current_path, generated_path):
    cur = open(current_path).read() if os.path.exists(current_path) else ""
    gen = open(generated_path).read() if os.path.exists(generated_path) else ""
    cur_f, gen_f = count_features(cur), count_features(gen)
    cur_m, gen_m = method_names(cur), method_names(gen)
    return {
        "lines_current": len(cur.splitlines()),
        "lines_generated": len(gen.splitlines()),
        "methods_current": len(cur_m),
        "methods_generated": len(gen_m),
        "methods_missing": sorted(cur_m - gen_m),
        "methods_extra": sorted(gen_m - cur_m),
        "feature_current": cur_f,
        "feature_generated": gen_f,
        "features_missing": {k: cur_f[k] - gen_f[k] for k in cur_f if cur_f[k] > gen_f.get(k, 0)},
    }


def bench_class(cls, commit=None, frameworks=("AppKit", "QuartzCore", "Foundation"),
                keep=False, run_gates=True, gate_filter="Signature"):
    rel = "src/nsui/%s.java" % cls
    if commit is None:
        if not os.path.exists(os.path.join(REPO, rel)):
            return {"class": cls, "status": "no-such-class",
                    "detail": "not in the current tree (the prototype proposed it but it never shipped)"}
        added = birth_commit(rel)
        if not added:
            return {"class": cls, "status": "no-birth-commit",
                    "detail": "added in the initial import; no pre-addition snapshot exists"}
        commit = git("rev-parse", added + "^").stdout.strip()
    snap = snapshot(commit, keep=keep)
    reset_snapshot(snap)
    os.makedirs(REPORTS, exist_ok=True)
    patch = os.path.join(REPORTS, "bench-%s-%s.patch" % (cls, commit[:8]))
    g = generate(snap, [cls], frameworks, patch)
    result = {"class": cls, "snapshot": commit[:8], "generator_rc": g.returncode,
              "generator_out": (g.stdout or "").strip().splitlines()[-3:],
              "patch_bytes": os.path.getsize(patch) if os.path.exists(patch) else 0}
    ok, detail = apply_patch(snap, patch)
    result["applied"] = ok
    result["apply_detail"] = detail if not ok else ""
    gen_path = os.path.join(snap, rel)
    result["generated"] = os.path.exists(gen_path)
    if result["generated"]:
        result["comparison"] = compare(os.path.join(REPO, rel), gen_path)
    if run_gates and result["generated"]:
        passed, gdetail = run_gate(snap, gate_filter)
        result["gate_passed"] = passed
        result["gate_detail"] = gdetail
    return result


def main():
    ap = argparse.ArgumentParser(description="Bench CodeGen against the code that shipped")
    ap.add_argument("--class", dest="classes", action="append", default=[])
    ap.add_argument("--commit", help="snapshot commit (default: the parent of the class's birth commit)")
    ap.add_argument("--list", action="store_true", help="list benchable classes and exit")
    ap.add_argument("--framework", dest="frameworks", action="append", default=None)
    ap.add_argument("--keep", action="store_true", help="keep the snapshot directory")
    ap.add_argument("--no-gate", action="store_true", help="skip the snapshot's signature gate")
    ap.add_argument("--gate", default="Signature", help="tests.sh filter for the gate (default Signature)")
    ap.add_argument("--json", help="also write the raw results to this path")
    ap.add_argument("--matrix", action="store_true",
                    help="bench every class from --class (or --top N) and write reports/matrix.{json,md}")
    ap.add_argument("--top", type=int, default=0,
                    help="with --matrix: use the N classes born most recently instead of --class")
    args = ap.parse_args()

    if args.list:
        for c in list_candidates():
            print("%-42s added by %s  snapshot %s" % (c["class"], c["added_by"], c["snapshot"][:8]))
        return 0
    if not args.classes and not (args.matrix and args.top):
        ap.error("give at least one --class (or --list), or --matrix --top N")

    frameworks = args.frameworks or ["AppKit", "QuartzCore", "Foundation", "Metal"]

    if args.matrix:
        classes = list(args.classes)
        if args.top:
            born = []
            for c in list_candidates():
                sha = birth_commit("src/nsui/%s.java" % c["class"])
                if sha:
                    born.append((git("log", "-1", "--format=%ct", sha).stdout.strip(), c["class"]))
            born.sort(reverse=True)
            classes = [c for _, c in born[:args.top]]
        if not classes:
            ap.error("--matrix needs --class ... or --top N")
        os.makedirs(REPORTS, exist_ok=True)
        rows = bench_matrix(classes, commit=args.commit, frameworks=frameworks,
                            run_gates=not args.no_gate, gate_filter=args.gate,
                            out_json=os.path.join(REPORTS, "matrix.json"),
                            out_md=os.path.join(REPORTS, "matrix.md"))
        for r in rows:
            cov = "n/a" if r["coverage"] is None else "%.1f%%" % r["coverage"]
            print("%-40s coverage %-7s gate=%s %s"
                  % (r["class"], cov, r["gate"], "ERROR: " + r["error"] if r["error"] else ""))
        done = [r for r in rows if r["coverage"] is not None]
        if done:
            print("\n%d classes: mean coverage %.1f%%  fully conformant %d"
                  % (len(done), sum(r["coverage"] for r in done) / len(done),
                     sum(1 for r in done if r["coverage"] >= 100 and not r["features_missing"])))
        print("wrote %s/matrix.{json,md}" % REPORTS)
        return 0

    results = []
    for cls in args.classes:
        t0 = time.time()
        r = bench_class(cls, commit=args.commit, frameworks=frameworks,
                        keep=args.keep, run_gates=not args.no_gate, gate_filter=args.gate)
        r["seconds"] = round(time.time() - t0, 1)
        results.append(r)
        print("\n=== %s (snapshot %s) ===" % (r["class"], r.get("snapshot", "?")))
        if r.get("status") in ("no-birth-commit", "no-such-class"):
            print("  " + r["detail"])
            continue
        print("  generator rc=%s  patch=%s bytes  applied=%s  generated=%s  %ss"
              % (r.get("generator_rc"), r.get("patch_bytes"), r.get("applied"),
                 r.get("generated"), r.get("seconds")))
        if r.get("gate_passed") is not None:
            print("  gate: %s  %s" % ("PASS" if r["gate_passed"] else "FAIL", r.get("gate_detail", "")))
        c = r.get("comparison")
        if c:
            print("  methods: shipped %d vs generated %d   lines: %d vs %d"
                  % (c["methods_current"], c["methods_generated"],
                     c["lines_current"], c["lines_generated"]))
            if c["methods_missing"]:
                print("  MISSING methods (%d): %s" % (len(c["methods_missing"]),
                                                      ", ".join(c["methods_missing"][:12])))
            if c["features_missing"]:
                print("  MISSING guards/features: %s" % json.dumps(c["features_missing"], sort_keys=True))
            if not c["methods_missing"] and not c["features_missing"]:
                print("  FULL CONFORMANCE: same method set and no missing guard features")

    if args.json:
        with open(args.json, "w") as fh:
            json.dump(results, fh, indent=2, sort_keys=True)
        print("\nwrote %s" % args.json)
    return 0


if __name__ == "__main__":
    sys.exit(main())
