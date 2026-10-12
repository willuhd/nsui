#!/usr/bin/env python3
"""Compare a fresh measurement against the guard evidence and classify every disagreement.

The point of the guard pipeline is that a measured abort stops aborting once the evidence guards
it. This tool is how that claim is checked over the whole set instead of a sample: it joins the two
files by case id and reports

  guarded-and-now-safe     the guard is doing its job
  control-still-tolerated  nothing the unguarded call survived is now rejected
  UNGUARDED-ABORT          the evidence has no guard for a case that still aborts (expected only
                           for the index cases whose bound is class-specific)
  GUARD-INEFFECTIVE        a guard exists and the case still aborts: either the guard is wrong or
                           the abort comes from somewhere other than the argument, and the entry
                           should not be trusted until that is settled
  NEW-ABORT                not in the evidence at all: a case this run found that no guard covers
  DROPPED/CONTROL-CHANGED  the case no longer appears, or a tolerated call now aborts

  python3 codegen/evidence_check.py --fresh /tmp/verify-all.json
"""

import argparse
import json
import os
import sys
from collections import Counter, defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
ABORT = ("objc-abort", "sigtrap", "sigill")


def load(path):
    """Keyed by the probe's case id, which the evidence stores as `probe_id`."""
    out = {}
    for c in json.load(open(path))["cases"]:
        cid = c.get("id") or c.get("probe_id")
        if cid:
            out[cid] = c
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--fresh", required=True, help="a fresh enumeration JSON")
    ap.add_argument("--evidence", default=os.path.join(HERE, "guard-evidence.json"))
    args = ap.parse_args()
    fresh = load(args.fresh)
    ev = load(args.evidence)
    buckets = defaultdict(list)
    for cid, f in fresh.items():
        e = ev.get(cid)
        verdict = f.get("unguarded")
        if verdict not in ABORT:
            if e and e.get("control") and verdict != "tolerated":
                buckets["CONTROL-CHANGED"].append((cid, verdict))
            elif verdict == "tolerated":
                buckets["control-still-tolerated"].append((cid, verdict))
            else:
                buckets["guarded-and-now-safe"].append((cid, verdict))
            continue
        if e is None:
            buckets["NEW-ABORT"].append((cid, verdict))
        elif e.get("guards"):
            buckets["GUARD-INEFFECTIVE"].append((cid, verdict))
        else:
            buckets["UNGUARDED-ABORT"].append((cid, verdict))
    for cid in ev:
        if cid not in fresh and ev[cid].get("unguarded") in ABORT:
            buckets["DROPPED"].append((cid, "not measured this run"))

    order = ["guarded-and-now-safe", "control-still-tolerated", "UNGUARDED-ABORT",
             "GUARD-INEFFECTIVE", "NEW-ABORT", "CONTROL-CHANGED", "DROPPED"]
    for name in order:
        items = buckets.get(name, [])
        print("%-24s %4d" % (name, len(items)))
    bad = [n for n in ("GUARD-INEFFECTIVE", "NEW-ABORT", "CONTROL-CHANGED") if buckets.get(n)]
    for name in bad:
        print("\n%s:" % name)
        for cid, verdict in buckets[name][:15]:
            print("   %-58s %s" % (cid, verdict))
    by_class = Counter(cid.split(".")[0] for cid in
                       [c for c, _ in buckets.get("UNGUARDED-ABORT", [])])
    if by_class:
        print("\nUNGUARDED-ABORT by class: %s" % dict(by_class.most_common(8)))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
