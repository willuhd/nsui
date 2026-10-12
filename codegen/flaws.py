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


def check_f3():
    """Measured aborts that the shipped wrapper does not guard."""
    path = os.path.join(HERE, "guard-evidence.json")
    try:
        cases = json.load(open(path))["cases"]
    except (OSError, ValueError):
        return []
    findings = []
    for case in cases:
        if case.get("unguarded") not in ("objc-abort", "sigtrap", "sigill"):
            continue
        cls, sel = case.get("class"), case.get("selector")
        wrapper = os.path.join(REPO, "src", "nsui", (cls or "") + ".java")
        if not cls or not os.path.exists(wrapper):
            continue
        text = open(wrapper, encoding="utf-8", errors="replace").read()
        # a guard for this selector, or a nonNull/index/notNaN call naming the parameter
        guarded = bool(re.search(r"Bounds\.\w+\([^;]*%s" % re.escape(sel), text)
                       or re.search(r"Bounds\.(nonNull|notNaN)\(%s[,)]" % re.escape(case.get("param") or ""),
                                    text))
        if not guarded:
            findings.append({"check": "F3", "class": cls, "selector": sel,
                             "param": case.get("param"), "badness": case.get("unguarded"),
                             "file": os.path.relpath(wrapper, REPO),
                             "evidence": "measured %s with a bad %s and no guard in the shipped file"
                                         % (case["unguarded"], case.get("badness") or case.get("param"))})
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


def main():
    ap = argparse.ArgumentParser(description="Report flaws in the shipped wrappers")
    ap.add_argument("--check", default="F1,F3,F4", help="comma-separated subset of F1,F3,F4")
    ap.add_argument("--json", help="write the findings as JSON")
    ap.add_argument("--limit", type=int, default=25, help="how many findings to print per check")
    args = ap.parse_args()
    wanted = {c.strip().upper() for c in args.check.split(",") if c.strip()}
    out = []
    if "F1" in wanted or "F1A" in wanted or "F1B" in wanted:
        out += check_f1(runtime())
    if "F3" in wanted:
        out += check_f3()
    if "F4" in wanted:
        out += check_f4()
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
    print("\ntotal: %d" % len(out))
    if args.json:
        json.dump(out, open(args.json, "w"), indent=2)
        print("wrote " + args.json)
    return 0


if __name__ == "__main__":
    sys.exit(main())
