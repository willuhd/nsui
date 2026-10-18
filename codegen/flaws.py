#!/usr/bin/env python3
"""CodeGen flaw finder — point the generator's ground truth at the SHIPPED wrappers.

Spec
====
The generator knows two things independently of the toolkit: the SDK's declarations and the
Objective-C runtime's own answers. Every shipped wrapper can therefore be checked against evidence
it did not produce. A *flaw* is a disagreement with that ground truth that a caller can hit; a
difference of style is not a flaw. The checks, and the ground truth each one uses:

F1 unresolved selector — the wrapper sends a selector the runtime class does not implement, so the
   call ends in an unrecognized-selector abort. Ground truth: class_getInstanceMethod /
   class_getClassMethod (a protocol name is resolved through a class that conforms to it).
F2 wrong ABI shape — a declared Sig shape disagrees with method_getTypeEncoding in width,
   signedness or struct-vs-object. Ground truth: the runtime encoding. This is exactly what
   tests/SignatureConformanceTest asserts globally; this tool reports it per site, and passes when
   that test is green.
F3 unguarded measured abort — the generator measured that the call aborts with a bad argument, and
   the shipped wrapper has no guard for it. Ground truth: codegen/guard-evidence.json, produced by
   codegen/probe.py in child processes.
F4 stale documentation — the `/// [alias selector]` anchor of a method disagrees with the selector
   the method actually sends, so the docs describe a different call. Ground truth: the
   ObjC.sel("...") literal in the same method body.

Output is a report only. Nothing under src/ or tests/ is written, and a flaw is never fixed here:
the finding names the file, the line, the evidence and the fix, and the maintainer decides.

  python3 codegen/flaws.py                     # every check, human-readable
  python3 codegen/flaws.py --json out.json     # machine-readable
  python3 codegen/flaws.py --check F1,F4       # a subset
"""

import argparse
import json
import os
import re
import sys
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
sys.path.insert(0, HERE)

SEL_CALL = re.compile(r'ObjC\.sel\(\s*"([^"]+)"\s*\)')
SELS_FIELD = re.compile(r'(\w+)\s*=\s*ObjC\.sel\(\s*"([^"]+)"\s*\)\s*;')
DOC_ANCHOR = re.compile(r"///\s*\[([^\]]+)\]\s*[—-]")
REQUIRE_CLASS = re.compile(r'requireClass\(\s*"([^"]+)"')
METHOD_SIG = re.compile(r"^\s*(public|protected|private)\s+(static\s+)?([\w.<>\[\]]+)\s+(\w+)\s*\(")


def runtime(frameworks=("AppKit", "Foundation", "QuartzCore", "Metal")):
    import objcabi
    return objcabi.ObjCRuntime(list(frameworks))


def wrapper_files():
    for fn in sorted(os.listdir(os.path.join(REPO, "src", "nsui"))):
        if fn.endswith(".java"):
            yield fn[:-5], os.path.join(REPO, "src", "nsui", fn)


def known_receivers():
    """Class names a send may be attributed to: the toolkit's wrappers (plus ObjC/Sig)."""
    if not _KNOWN_RECEIVERS:
        for fn in os.listdir(os.path.join(REPO, "src", "nsui")):
            if fn.endswith(".java"):
                _KNOWN_RECEIVERS.add(fn[:-5])
        _KNOWN_RECEIVERS.update(("ObjC", "Sig"))
    return _KNOWN_RECEIVERS


_KNOWN_RECEIVERS = set()


def sdk_all_selectors(frameworks=None):
    """Every (selector, side) any SDK class declares.

    Attribution of a bare `ObjC.msgSend…` to a receiver is not reliably recoverable from one line,
    so a selector that some class declares is never reported as misattributed: only a selector no
    class declares is certainly wrong. That is the difference between a finding and noise.
    """
    if _SDK_ALL:
        return _SDK_ALL
    import gen, hdrs
    sdk = gen.find_sdk()
    root = os.path.join(sdk, "System", "Library", "Frameworks")
    # Every framework, not only the four the generator reads: a wrapper may send a selector to a
    # class it loads on demand (NSSavePanel sends typeWithIdentifier: to UTType).
    names = frameworks if frameworks is not None else sorted(
        fn[:-len(".framework")] for fn in os.listdir(root) if fn.endswith(".framework"))
    for fw in names:
        try:
            for iface in hdrs.parse_framework(sdk, fw, include_categories=True):
                for decl in iface.decls:
                    _SDK_ALL.add((decl.selector, bool(decl.is_class)))
        except Exception:
            continue
    return _SDK_ALL


_SDK_ALL = set()


def sdk_selectors(cls, frameworks=("AppKit", "Foundation", "QuartzCore", "Metal")):
    """Selectors the SDK declares for a class (protocol bodies included), cached."""
    if cls in _SDK_SELECTORS:
        return _SDK_SELECTORS[cls]
    out = set()
    try:
        import gen
        surface = gen.collect_surface(cls, list(frameworks))
        if surface is not None:
            for decl in surface.decls:
                out.add((decl.selector, bool(decl.is_class)))
    except Exception:
        pass
    _SDK_SELECTORS[cls] = out
    return out


_SDK_SELECTORS = {}


def check_f1(rt):
    """Unresolved selectors, split by what the disagreement actually means.

    F1a the SDK does not declare the selector for this class either: the call is misattributed.
    F1b the SDK declares it but this runtime lacks it (a version-gated API) and the method has no
        respondsToSelector guard: calling it aborts. F1b is the baseline the signature gate already
        tracks; separating it keeps the report from claiming 290 flaws where most are version gates.
        A named class can be an abstract shell (the Metal descriptor family): has_method misses
        what the generator's own encoding() resolves via the concrete subclass, so an F1b row
        encoding() resolves is a lookup artifact, not a version gate, and is skipped below. Only
        F1b rows consult encoding(): realization sends +alloc, and at least one wrapper class
        traps it (+[NSPasteboard alloc] raises NSInvalidArgumentException, which aborts the
        audit through ctypes — observed 2026-10-18 when the fallback stood before the F1a/F1b
        split), while an undeclared selector is F1a whatever any subclass implements. Only rows
        neither path resolves are reported (5 NSWindowTabGroup rows on 2026-10-18).
    """
    findings = []
    for cls, path in wrapper_files():
        if cls.startswith("objc"):
            continue
        text = open(path, encoding="utf-8", errors="replace").read()
        lines = text.splitlines()
        sdk = sdk_selectors(cls)
        side, method_start = None, 0
        in_sels = False
        for i, line in enumerate(lines, 1):
            # The nested Sels class only *registers* selectors (`x = ObjC.sel("...")`); flagging
            # those reports hundreds of non-sends, which is exactly the overreporting this spec is
            # meant to avoid. Only the wrapper's own method bodies are scanned.
            if "private static final class Sels" in line:
                in_sels = True
            elif in_sels and re.match(r"^    \}\s*$", line):
                in_sels = False
                continue
            if in_sels:
                continue
            stripped = line.lstrip()
            if stripped.startswith(("///", "//", "*", "/*")):
                continue          # a comment mentioning ObjC.sel("...") is not a send
            m = METHOD_SIG.match(line)
            if m:
                side, method_start = ("class" if m.group(2) else "inst"), i
            for sel in SEL_CALL.findall(line):
                if not sel or sel.startswith("_") or side is None:
                    continue
                block = "\n".join(lines[method_start - 1:i])
                # A wrapper may host a subclass's API with a runtime type guard
                # (`requireClass("CABasicAnimation", ...)`): the selector belongs to that class, so
                # resolve it there instead of reporting the parent as a misattribution.
                guard = REQUIRE_CLASS.search(block)
                owner = guard.group(1) if guard else cls
                # Side is not reliably recoverable from one line (a static factory sends
                # initWithString: to the instance it just allocated), so a selector that resolves
                # on either side of the class is not a finding.
                if rt.has_method(owner, sel, False) or rt.has_method(owner, sel, True):
                    continue
                declared_here = (sel, False) in sdk or (sel, True) in sdk
                declared_anywhere = ((sel, False) in sdk_all_selectors()
                                     or (sel, True) in sdk_all_selectors())
                # A wrapper may guard the send itself (respondsToSelector:) or through a helper
                # (`if (!respondsTo("findString")) return null;`), so the selector appearing as a
                # string outside the ObjC.sel(...) call is also a guard.
                helper_guarded = bool(re.search(r'respondsTo\(\s*"%s"|respondsToSelector\s*\('
                                               % re.escape(sel), block))
                if helper_guarded:
                    continue
                if declared_here and "respondsToSelector" not in block:
                    # Concrete-subclass fallback (lookup-only, F1b rows only): the named class
                    # may be an abstract shell whose implementations live on a private concrete
                    # subclass, which is exactly what the generator's encoding() realizes
                    # through (one cached +alloc per class, never -init — the same lookup
                    # build_plan performs for every planned declaration). A row it resolves is
                    # a lookup artifact, not a version gate. F1a rows never reach it: +alloc can
                    # trap (see the docstring), and an undeclared selector is F1a regardless.
                    # Verified 2026-10-18: every current F1b owner realizes cleanly (90 of 95
                    # rows resolve; the 5 NSWindowTabGroup rows do not). A future F1b owner that
                    # traps +alloc would abort here naming its class; that name belongs in
                    # REALIZE_OPT_OUT (or this fallback needs subprocess isolation).
                    enc, _kind = rt.encoding(owner, sel)
                    if enc is not None:
                        continue
                    findings.append({"check": "F1b", "class": cls, "line": i, "selector": sel,
                                     "side": side, "file": os.path.relpath(path, REPO),
                                     "evidence": "SDK-declared but absent at runtime, and the method "
                                                 "has no respondsToSelector guard"})
                elif not declared_anywhere:
                    findings.append({"check": "F1a", "class": cls, "line": i, "selector": sel,
                                     "side": side, "file": os.path.relpath(path, REPO),
                                     "evidence": "no class in the SDK declares %s %s" % (side, sel),
                                     "resolved_on": owner})
                # otherwise another class declares it and the receiver is not provable from here
    return findings


ABORTS = ("objc-abort", "sigtrap", "sigill")


def shipped_baseline():
    """{probe id: verdict measured against the SHIPPED wrappers} — codegen/shipped-baseline.json."""
    path = os.path.join(HERE, "shipped-baseline.json")
    try:
        return json.load(open(path))["verdicts"]
    except (OSError, ValueError):
        return {}


def case_method(case):
    cid = case.get("probe_id") or ""
    return cid.split(".")[1] if cid.count(".") >= 2 else None


def check_f3():
    """Cases where the shipped wrapper itself aborts on a bad argument.

    The verdict comes from measuring the shipped code (codegen/shipped-baseline.json), not from
    looking for a guard: a method can carry an inline check that does not fire, and the generator's
    body is simpler than the shipped one, so "the generated call aborts" is not evidence about the
    shipped wrapper. Only a measured shipped abort is.
    """
    baseline = shipped_baseline()
    findings = []
    for case in json.load(open(os.path.join(HERE, "guard-evidence.json")))["cases"]:
        if case.get("unguarded") not in ABORTS:
            continue
        cls, sel = case.get("class"), case.get("selector")
        if not cls or not os.path.exists(os.path.join(REPO, "src", "nsui", cls + ".java")):
            continue
        verdict = baseline.get(case.get("probe_id"))
        if verdict not in ABORTS:
            continue                       # shipped tolerates it: not a shipped flaw
        findings.append({"check": "F3", "class": cls, "selector": sel,
                         "param": case.get("param"), "badness": case.get("unguarded"),
                         "shipped": verdict, "param_index": case.get("param_index"),
                         "probe_id": case.get("probe_id"), "guards": case.get("guards") or [],
                         "file": os.path.relpath(os.path.join(REPO, "src", "nsui", cls + ".java"),
                                                 REPO),
                         "evidence": "the shipped wrapper aborts (%s) on a bad %s"
                                     % (verdict, case.get("param"))})
    return findings


def check_f6():
    """Arguments the shipped wrapper tolerates where the unguarded generated body aborts.

    Informational, and no longer a pending emission. It began as "the generated guard rejects input
    the shipped API accepts", which was true while the generator guarded every measured abort. It
    now follows the same baseline: it declines to guard a tolerated argument (GUARD-SKIPPED) and, for
    a nil, matches the tolerance with an early return (NIL-SKIP), so no rejecting guard is emitted
    for these cases at all. The measured statement of generated conformance is the differential
    (`probe.py --differential`, which reports STRICTER-GUARD and STRICTER-BODY), and this check is
    the static view of the same set. It is excluded from the shipped-flaw total.
    """
    baseline = shipped_baseline()
    findings = []
    for case in json.load(open(os.path.join(HERE, "guard-evidence.json")))["cases"]:
        if case.get("unguarded") not in ABORTS or not case.get("guards"):
            continue
        verdict = baseline.get(case.get("probe_id"))
        if verdict != "tolerated":
            continue       # java-exception is not over-guarding: the shipped wrapper rejects too
        findings.append({"check": "F6", "informational": True, "class": case.get("class"),
                         "selector": case.get("selector"),
                         "param": case.get("param"), "generated": case.get("unguarded"),
                         "shipped": verdict,
                         "file": os.path.relpath(os.path.join(REPO, "src", "nsui",
                                                              str(case.get("class")) + ".java"), REPO),
                         "evidence": "generated aborts (%s) but the shipped wrapper tolerates it"
                                     % case.get("unguarded")})
    return findings


def check_f4():
    """A method's documented selector disagrees with the selector it sends."""
    findings = []
    for cls, path in wrapper_files():
        text = open(path, encoding="utf-8", errors="replace").read()
        blocks = re.split(r"\n(?=    /// [^\n]*\n    public |\n    public )", text)
        for block in blocks:
            anchor = DOC_ANCHOR.search(block)
            if not anchor:
                continue
            m = METHOD_SIG.search(block)
            if not m:
                continue
            sels = SEL_CALL.findall(block)
            if not sels:
                continue
            documented = anchor.group(1).strip()
            if documented not in sels:
                findings.append({"check": "F4", "class": cls, "method": m.group(4),
                                 "documented": documented, "sent": sorted(set(sels)),
                                 "file": os.path.relpath(path, REPO),
                                 "evidence": "doc anchor names a selector the method never sends"})
    return findings


GATE_WINDOW = 1024          # tests/SignatureConformanceTest reads the encoding through this


def check_f5(rt):
    """Methods whose type encoding is longer than the gate's read window.

    The gate does `e.reinterpret(1024).getString(0)`; a longer encoding has no terminator inside
    that window, so the gate dies with IndexOutOfBoundsException instead of reporting the method.
    NSOpenGLContext.CGLContextObj encodes its opaque struct inline at 2044 bytes, which aborted a
    whole 389-class conformance run.
    """
    import ctypes
    lib = rt.libobjc
    lib.method_getTypeEncoding.restype = ctypes.c_void_p
    lib.method_getTypeEncoding.argtypes = [ctypes.c_void_p]
    findings = []
    for cls, path in wrapper_files():
        text = open(path, encoding="utf-8", errors="replace").read()
        for side in (False, True):
            for sel in set(SEL_CALL.findall(text)):
                c = rt.get_class(cls)
                if not c:
                    continue
                s = lib.sel_registerName(sel.encode())
                m = lib.class_getClassMethod(c, s) if side else lib.class_getInstanceMethod(c, s)
                if not m:
                    continue
                enc = lib.method_getTypeEncoding(m)
                if not enc:
                    continue
                length = ctypes.string_at(enc, GATE_WINDOW * 4).find(b"\x00")
                if length < 0 or length > GATE_WINDOW:
                    findings.append({"check": "F5", "class": cls, "selector": sel,
                                     "side": "class" if side else "inst",
                                     "file": os.path.relpath(path, REPO),
                                     "evidence": "type encoding is %s bytes; the signature gate reads "
                                                 "%d, so it crashes rather than checking the method"
                                                 % (length if length >= 0 else ">4096", GATE_WINDOW)})
    return findings


GUARD_AFTER = "ensureInit();"


def _method_span(lines, name):
    """(start, body_start, param_names) for the shipped method `name`, or None."""
    for i, line in enumerate(lines):
        m = re.match(r"\s*public\s+(?:static\s+)?[\w.<>\[\]]+\s+%s\s*\(([^)]*)\)" % re.escape(name),
                     line)
        if not m:
            continue
        params = [p.strip().split()[-1] if p.strip() else "" for p in m.group(1).split(",")]
        for j in range(i, min(i + 8, len(lines))):
            if lines[j].rstrip().endswith("{"):
                return i, j, params
    return None


def proposed_patches():
    """Reviewable diffs that add the guard each measured abort needs to the shipped wrapper.

    src/ is never edited: a finding becomes a patch, and applying it in a scratch tree is what
    verifies it, because the abort then disappears under codegen/probe.py.
    """
    import difflib
    by_file = defaultdict(list)
    for f in check_f3():
        case_id = f.get("probe_id") or ""
        method = case_id.split(".")[1] if case_id.count(".") >= 2 else None
        if not method or not f.get("guards"):
            continue          # an index case without a bound has no statement to insert
        by_file[f["class"] + ".java"].append((method, f))
    chunks = []
    for name, edits in sorted(by_file.items()):
        rel = os.path.join("src", "nsui", name)
        path = os.path.join(REPO, rel)
        original = open(path, encoding="utf-8", errors="replace").read().splitlines(keepends=True)
        working = list(original)
        for method, f in edits:
            lines = [ln.rstrip("\n") for ln in working]
            span = _method_span(lines, method)
            if span is None:
                continue
            _start, body, params = span
            idx = f.get("param_index")
            pname = params[idx] if isinstance(idx, int) and idx < len(params) else (f.get("param") or "")
            stmts = [g.replace("{p}", pname) for g in f["guards"]]
            at = body + 1
            if at < len(working) and GUARD_AFTER in working[at]:
                at += 1
            insert = ["        " + st + "\n" for st in stmts]
            working = working[:at] + insert + working[at:]
        if working != original:
            chunks.append("".join(difflib.unified_diff(original, working, "a/" + rel, "b/" + rel, n=3)))
    return "".join(chunks)


def main():
    ap = argparse.ArgumentParser(description="Report flaws in the shipped wrappers")
    ap.add_argument("--check", default="F1,F3,F4,F5,F6",
                    help="comma-separated subset of F1,F3,F4,F5,F6")
    ap.add_argument("--json", help="write the findings as JSON")
    ap.add_argument("--limit", type=int, default=25, help="how many findings to print per check")
    ap.add_argument("--patch", metavar="PATH",
                    help="with F3: write a reviewable diff that adds the missing guard to the "
                         "shipped wrapper (never applied automatically)")
    args = ap.parse_args()
    wanted = {c.strip().upper() for c in args.check.split(",") if c.strip()}
    out = []
    if "F1" in wanted or "F1A" in wanted or "F1B" in wanted:
        out += check_f1(runtime())
    if "F3" in wanted:
        out += check_f3()
    if "F4" in wanted:
        out += check_f4()
    if "F6" in wanted:
        out += check_f6()
    if "F5" in wanted:
        out += check_f5(runtime())
    by_check = defaultdict(list)
    for f in out:
        by_check[f["check"]].append(f)
    print("=== shipped-wrapper flaws ===")
    for check in sorted(by_check):
        items = by_check[check]
        print("\n%s: %d finding(s)" % (check, len(items)))
        for f in items[:args.limit]:
            where = "%s:%s" % (f.get("file"), f.get("line") or f.get("method") or "-")
            extra = f.get("selector") or f.get("method") or ""
            print("  %-52s %-30s %s" % (where, str(extra)[:30], f["evidence"]))
        if len(items) > args.limit:
            print("  ... %d more" % (len(items) - args.limit))
    info = [f for f in out if f.get("informational")]
    real = [f for f in out if not f.get("informational")]
    if info:
        print("\ninformational: %d case(s) where the generator follows the shipped wrapper's own "
              "tolerance instead of guarding (not shipped flaws; see --differential for the "
              "measured behaviour)" % len(info))
    print("\ntotal shipped flaws: %d" % len(real))
    if args.json:
        json.dump(out, open(args.json, "w"), indent=2)
        print("wrote " + args.json)
    if args.patch:
        text = proposed_patches()
        open(args.patch, "w", encoding="utf-8").write(text)
        print("\nproposed guard patch: %s (%d bytes)" % (args.patch, len(text)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
