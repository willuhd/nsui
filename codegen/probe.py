#!/usr/bin/env python3
"""CodeGen bad-argument probe — measure what a wrapper does with a bad argument.

Guards in this toolkit exist because a nil argument, an out-of-range index or NaN geometry makes
AppKit abort the process (exit 134 / 133 / 132) instead of returning an error. That knowledge
cannot be read out of a header — `- (void)addChildViewController:(NSViewController *)childViewController`
carries no nullability annotation and NSViewController.h has no NS_ASSUME_NONNULL region — so it is
measured here, one child process per case, and recorded as evidence the generator consumes.

  python3 codegen/probe.py --repo . --out codegen/guard-evidence.json
  python3 codegen/probe.py --enumerate <classes> --shipped-baseline codegen/shipped-baseline.json
  python3 codegen/probe.py --enumerate <classes> --differential --out /tmp/diff.json

`--differential` processes the classes in slices (20 by default): each class costs three
measurements, and a 73-class invocation runs into the JVM's and the probe's limits, so the slicing
is what makes a wide check usable. A slice that cannot be measured is named in the output rather
than silently skipped.

`--differential` measures each case three ways — against the shipped tree, against the generated
tree with the guard evidence, and against the generated tree without it — so a rejection can be
attributed. A guard is over-rejecting only when the shipped wrapper tolerates the call AND the
generated wrapper rejects it only because of the evidence (STRICTER-GUARD); when the guard-free
build rejects it too, the difference is in the generated body (STRICTER-BODY).

Exit codes are read directly: 0 tolerated, 1 a Java exception (a guard fired), 134 uncaught ObjC
exception, 133 SIGTRAP (NaN geometry), 132 SIGILL (self/ancestor cycle). One exception: an abort
exit whose captured output already holds the guard's IAE (IllegalArgumentException through a
Bounds frame) reads as java-exception — a stateful teardown can abort the process after the
guard fired, and the guard, not the teardown, is what the measurement attributes.
"""

import argparse
from collections import defaultdict
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
GRAALVM = os.environ.get("GRAALVM", "/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home")
JAVAC = os.path.join(GRAALVM, "bin", "javac")
JAVA = os.path.join(GRAALVM, "bin", "java")
SVM_JAR = os.path.join(GRAALVM, "lib", "svm", "builder", "svm.jar")

# Each case names the receiver, the bad argument and the Java call. `guard` is the helper the
# evidence implies: nonNull for nil, index for an out-of-range index, notNaN for NaN geometry,
# window/cycle for a self-or-ancestor argument.
CASES = [
    {"id": "NSViewController.addChildViewController:null", "class": "NSViewController",
     "guard": "nonNull", "param": "childViewController",
     "setup": ['NSViewController vc = NSViewController.wrap(init2("NSViewController", "initWithNibName:bundle:"));'],
     "call": 'vc.addChildViewController(null);'},
    {"id": "NSViewController.removeChildViewControllerAtIndex:0", "class": "NSViewController",
     "guard": "index", "param": "index",
     "setup": ['NSViewController vc = NSViewController.wrap(init2("NSViewController", "initWithNibName:bundle:"));'],
     "call": 'vc.removeChildViewControllerAtIndex(0);'},
    {"id": "NSViewController.insertChildViewController:atIndex:5", "class": "NSViewController",
     "guard": "index", "param": "index",
     "setup": ['NSViewController vc = NSViewController.wrap(init2("NSViewController", "initWithNibName:bundle:"));',
               'NSViewController child = NSViewController.wrap(init2("NSViewController", "initWithNibName:bundle:"));'],
     "call": 'vc.insertChildViewController(child, 5);',
     "call_gen": 'vc.insertChildViewControllerAtIndex(child, 5);'},
    {"id": "NSViewController.presentViewControllerAsSheet:null", "class": "NSViewController",
     "guard": "nonNull", "param": "viewController",
     "setup": ['NSViewController vc = NSViewController.wrap(init2("NSViewController", "initWithNibName:bundle:"));'],
     "call": 'vc.presentViewControllerAsSheet(null);'},
    {"id": "NSMenu.itemAtIndex:3", "class": "NSMenu",
     "guard": "index", "param": "index",
     "setup": ['NSMenu m = NSMenu.wrap(init1("NSMenu", "initWithTitle:"));'],
     "call": 'm.itemAtIndex(3);'},
    {"id": "NSMenu.removeItem:foreign", "class": "NSMenu",
     "guard": "nonNull", "param": "item",
     "setup": ['NSMenu m = NSMenu.wrap(init1("NSMenu", "initWithTitle:"));',
               'NSMenuItem other = NSMenuItem.wrap(init3("NSMenuItem", "initWithTitle:action:keyEquivalent:"));'],
     "call": 'm.removeItem(other);'},
    {"id": "NSView.setFrame:NaN", "class": "NSView",
     "guard": "notNaN", "param": "frame",
     "setup": ['NSView v = NSView.wrap(ObjC.newView("NSView", new NSRect(0, 0, 10, 10)));'],
     "call": 'v.setFrame(new NSRect(Double.NaN, 0, 10, 10));'},
    {"id": "NSClipView.scrollToPoint:NaN", "class": "NSClipView",
     "guard": "notNaN", "param": "point",
     "setup": ['NSClipView v = NSClipView.wrap(ObjC.newView("NSClipView", new NSRect(0, 0, 10, 10)));'],
     "call": 'v.scrollToPoint(new NSPoint(Double.NaN, 0));'},
    {"id": "NSView.addSubview:self", "class": "NSView",
     "guard": "cycle", "param": "view",
     "setup": ['NSView v = NSView.wrap(ObjC.newView("NSView", new NSRect(0, 0, 10, 10)));'],
     "call": 'v.addSubview(v);'},
    # Control: nil survives in the unguarded tree, so a guard here would over-guard the shipped API.
    {"id": "NSView.addSubview:null", "class": "NSView",
     "guard": "none", "param": "view",
     "setup": ['NSView v = NSView.wrap(ObjC.newView("NSView", new NSRect(0, 0, 10, 10)));'],
     "call": 'v.addSubview(null);'},
]

# Python reports a signal death as a *negative* number (the shell reports 128+signum).
VERDICTS = {0: "tolerated", 1: "java-exception", 132: "sigill", 133: "sigtrap", 134: "objc-abort",
            -4: "sigill", -5: "sigtrap", -6: "objc-abort", -9: "killed", 124: "timeout"}


GUARD_LINE = re.compile(r"^\s*Bounds\.\w+\(.*\);\s*$")


def strip_guards(repo, classes):
    """A scratch copy of the tree with every Bounds guard removed from the named classes.

    This isolates the guard's effect: the rest of the toolkit stays as shipped, so the tree still
    compiles, and any abort that appears is caused by the missing guard rather than by a
    regenerated file losing unrelated API.
    """
    dest = tempfile.mkdtemp(prefix="cgnoguard-")
    shutil.copytree(repo, dest, dirs_exist_ok=True, symlinks=True,
                    ignore=shutil.ignore_patterns("out", "codegen", ".git"))
    removed = 0
    for cls in classes:
        path = os.path.join(dest, "src", "nsui", cls + ".java")
        if not os.path.exists(path):
            continue
        keep = []
        for line in open(path, encoding="utf-8", errors="replace").read().splitlines():
            if GUARD_LINE.match(line):
                removed += 1
                continue
            keep.append(line)
        open(path, "w", encoding="utf-8").write("\n".join(keep) + "\n")
    print("stripped %d guard statement(s) from %s" % (removed, ", ".join(classes)))
    return dest


STRUCT_DEFAULTS = {
    "rect": ("new NSRect(0, 0, 0, 0)", "new NSRect(Double.NaN, 0, 0, 0)"),
    "point": ("new NSPoint(0, 0)", "new NSPoint(Double.NaN, 0)"),
    "size": ("new NSSize(0, 0)", "new NSSize(Double.NaN, 0)"),
    "range": ("new NSRange(0, 0)", None),
    "edgeinsets": ("new NSEdgeInsets(0, 0, 0, 0)", None),
}
# R1-live: INF variants for the same geometry NaN covers, plus the Metal viewport.
# AppKit view/window geometry tolerates INF (measured exit 0) so those rows become controls;
# Metal's viewport validation aborts on INF ("must not be infinite or NaN", exit 134) so that
# row becomes a finite guard. The viewport default mirrors MTLViewport.of(10,10) (znear 0,zfar 1).
INF_STRUCT_EXPRS = {
    "rect": "new NSRect(Double.POSITIVE_INFINITY, 0, 0, 0)",
    "point": "new NSPoint(Double.POSITIVE_INFINITY, 0)",
    "size": "new NSSize(Double.POSITIVE_INFINITY, 0)",
    "mtlviewport": "new MTLViewport(Double.POSITIVE_INFINITY, 0, 10, 10, 0, 1)",
}
SCALAR_DEFAULTS = {
    "long": "0", "ulong": "0", "int": "0", "short": "(short) 0", "byte": "(byte) 0", "uint32": "0",
    "boolean": "false", "double": "0.0", "float": "0.0f",
}
OBJECT_KINDS = ("id", "string")


def _default_expr(kind):
    if kind in STRUCT_DEFAULTS:
        return STRUCT_DEFAULTS[kind][0]
    # R1-live: value-type defaults for the Metal structs, so a method with a viewport plus
    # other params still gets benign defaults for the non-target. Mirrors the hand-written
    # wrappers (MTLViewport.of(10,10); scissor origin 0 size 10).
    if kind == "mtlviewport":
        return "new MTLViewport(0, 0, 10, 10, 0, 1)"
    if kind == "mtlscissorrect":
        return "new MTLScissorRect(0, 0, 10, 10)"
    if kind in SCALAR_DEFAULTS:
        return SCALAR_DEFAULTS[kind]
    if kind == "string":
        return 'ObjC.nsstring("")'
    return "null"


def _bad_expr(kind, badness):
    if badness == "nil":
        return "null"
    if badness == "index":
        return "7"                      # out of range for the empty receiver the setup builds
    # Unsigned wrapping: a negative Java long passed as NSUInteger wraps to a huge
    # unsigned value natively (grid factory count, completion-range location, table
    # drop row all wrapped before their hand-written guards). Synthesize -1 only for
    # unsigned kinds, where a negative is always a caller error; signed longs keep
    # their existing coverage (index cases, NaN/INF for doubles) so legal -1 values
    # such as NSNotFound are never rejected.
    if badness == "negative":
        if kind == "ulong":
            return "-1"
        if kind == "uint32":
            return "-1"
        # Scissor components are NSUInteger-backed with no signed form: a negative
        # width is the caller error the validation layer aborts on (neg-w exits 134
        # with validation, finite tolerated). One bad component preserves attribution.
        if kind == "mtlscissorrect":
            return "new MTLScissorRect(0, 0, -1, 10)"
        return _default_expr(kind)
    # R1-live: INF synthesis. Double.POSITIVE_INFINITY inside a struct (or as a bare double)
    # is the Metal viewport abort; AppKit geometry tolerates it, so those rows become controls.
    if badness == "inf":
        if kind in INF_STRUCT_EXPRS:
            return INF_STRUCT_EXPRS[kind]
        if kind == "double":
            return "Double.POSITIVE_INFINITY"
        if kind == "float":
            return "Float.POSITIVE_INFINITY"
        return _default_expr(kind)
    if kind in STRUCT_DEFAULTS:
        return STRUCT_DEFAULTS[kind][1]
    if kind == "double":
        return "Double.NaN"
    if kind == "float":
        return "Float.NaN"
    return _default_expr(kind)


def _is_view_subclass(surface, frameworks, sdk, seen=None):
    seen = seen or set()
    name = surface.superclass
    if not name or name in seen or name in ("NSObject", "NSProxy"):
        return False
    if name == "NSView":
        return True
    seen.add(name)
    parent = gen_module().collect_surface(name, list(frameworks), sdk)
    return bool(parent) and _is_view_subclass(parent, frameworks, sdk, seen)


_GEN = {}


def gen_module():
    if "m" not in _GEN:
        import gen
        _GEN["m"] = gen
    return _GEN["m"]


def enumerate_cases(classes, frameworks):
    """Bad-argument candidates taken from the generator's own plans.

    One bad argument per call, so an abort is attributable to it: a method qualifies for a nil case
    when exactly one parameter is an object, for a NaN case when exactly one is geometry, for an
    INF case when exactly one is INF-capable geometry/scalar/viewport (R1-live), and for
    an index case when it names an index and every other parameter is a scalar.
    """
    import objcabi
    gen = gen_module()
    sdk = gen.find_sdk()
    vocab = gen.load_vocabulary(os.path.join(REPO, "src", "nsui", "objc", "Sig.java"))
    wrappers = gen.load_wrappers(os.path.join(REPO, "src", "nsui"))
    rt = objcabi.ObjCRuntime(list(frameworks))
    toolkit = gen.Toolkit(REPO)
    # Cases a previous verification proved unattributable (a guard on the argument does not stop
    # the abort, so the abort is environmental) are not derived again: they would keep coming back
    # as "new" findings that cannot be acted on.
    skip_cases = set()
    try:
        ev = json.load(open(os.path.join(HERE, "guard-evidence.json")))
        for item in ev.get("unattributable", []):
            skip_cases.add((item.get("class"), item.get("selector")))
    except (OSError, ValueError):
        pass
    out = []
    for cls in classes:
        planned = gen.plan_class(cls, list(frameworks), sdk, vocab, wrappers, rt, toolkit=toolkit)
        if planned is None:
            continue
        surface = planned["surface"]
        plans = planned["plans"]      # post-rename: the names the generator will really emit
        if _is_view_subclass(surface, frameworks, sdk):
            recv_expr = '%s.wrap(ObjC.newView("%s", new NSRect(0, 0, 10, 10)))' % (cls, cls)
        else:
            recv_expr = '%s.wrap(init0("%s"))' % (cls, cls)
        # Building the receiver can itself abort ([[NSWindow alloc] init] raises), and an abort
        # there would be recorded against every method of the class. The baseline measures
        # construction alone; if it fails, the whole class is dropped from the evidence.
        out.append({
            "id": "%s.__baseline" % cls, "class": cls, "baseline": True, "guard": "none",
            "setup": ["%s recv = %s;" % (cls, recv_expr)],
            "call": 'System.out.println(recv == null ? "NORECEIVER" : "RECEIVER-OK");',
        })
        ancestors = []
        parent = surface.superclass
        while parent and len(ancestors) < 6:
            planned_parent = gen.plan_class(parent, list(frameworks), sdk, vocab, wrappers, rt,
                                            toolkit=toolkit)
            if planned_parent is None:
                break
            ancestors.extend(planned_parent["plans"])
            parent = planned_parent["surface"].superclass
        out.extend(_cases_for_class(cls, plans, wrappers, recv_expr, ancestors, skip_cases))
    return out


SHIPPED_INDEX_GUARD = re.compile(r'Bounds\.index\(\s*(\w+)\s*,\s*([^,]+?)\s*,\s*"([^"]+)"\s*\)')


def shipped_index_bounds(cls):
    """{selector: {parameter name: bound expression}} from the shipped wrapper's own guards.

    The corpus already encodes the bound each index needs (`numberOfColumns()` for a column index),
    and a bound has to belong to the collection being indexed. Deriving one from count-like
    accessors produced guards that rejected valid columns and let invalid ones abort natively.
    """
    path = os.path.join(REPO, "src", "nsui", cls + ".java")
    out = {}
    if not os.path.exists(path):
        return out
    text = open(path, encoding="utf-8", errors="replace").read()
    for m in SHIPPED_INDEX_GUARD.finditer(text):
        out.setdefault(m.group(3), {})[m.group(1)] = m.group(2).strip()
    return out


def shipped_bound(cls, selector, param_name, param_index):
    """The bound the shipped wrapper uses for this index parameter, or None.

    Matched by name and then by position, because a shipped wrapper sometimes renames a header
    parameter (scrollToPoint: takes `newOrigin` in the header, `point` in the wrapper). None means
    the corpus has no guard for it, and inventing one is how wrong bounds got emitted.
    """
    bounds = shipped_index_bounds(cls).get(selector) or {}
    if not bounds:
        return None
    if param_name in bounds:
        return bounds[param_name]
    names = list(bounds)
    if isinstance(param_index, int) and param_index < len(names):
        return bounds[names[param_index]]
    return None


def _cases_for_class(cls, plans, wrappers, recv_expr, ancestors=None, skip_cases=None):
    """Nil/NaN/INF/index/negative candidates for one class, one bad argument per call.

    A nil case needs exactly one object parameter, a NaN case exactly one geometry parameter
    or one scalar double (alongside its INF case, so notNaN stays apart from finite),
    an INF case exactly one INF-capable parameter (rect/point/size/viewport/double/float — R1-live),
    a negative case every non-index unsigned (NSUInteger/uint32) parameter,
    and an index case a parameter that names an index with every other parameter a scalar — so the
    abort can only be that argument's doing. Other parameters get benign defaults.
    """
    out = []
    for plan in plans:
        if plan.skip or plan.is_class or len(plan.params) > 4:
            continue
        if (cls, plan.decl.selector) in (skip_cases or set()):
            continue
        kinds = [(k, jt, nm) for k, jt, nm in plan.params]
        bound = bound_setup = None
        objs = [i for i, (k, _j, _n) in enumerate(kinds)
                if k in OBJECT_KINDS or k.startswith("typed:")]
        structs = [i for i, (k, _j, _n) in enumerate(kinds) if k in STRUCT_DEFAULTS]
        # An index parameter is one whose name *is* an index: `indexType` is an enum, and guarding
        # it with a collection's count rejected legal values.
        def is_index(name):
            n = name.lower()
            if n != "index" and not n.endswith("index"):
                return False
            return not re.search(r"indextype|indexpath|indexset|indexrange|indexes|indexof", n)

        idx = [i for i, (k, _j, n) in enumerate(kinds)
               if k in ("long", "ulong", "int", "short", "byte", "uint32") and is_index(n)]
        wanted = []
        if len(objs) == 1:
            wanted.append((objs[0], "nil"))
        if len(structs) == 1 and STRUCT_DEFAULTS[kinds[structs[0]][0]][1]:
            wanted.append((structs[0], "nan"))
        # R1-live: INF candidate for the same single-geometry attribution as NaN, plus the
        # Metal viewport struct (outside STRUCT_DEFAULTS) and single double/float scalars.
        # One bad argument per call preserves attribution; others get benign defaults.
        inf_targets = [i for i, (k, _j, _n) in enumerate(kinds) if k in INF_STRUCT_EXPRS]
        doubles = [i for i, (k, _j, _n) in enumerate(kinds) if k in ("double", "float")]
        if len(inf_targets) == 1:
            wanted.append((inf_targets[0], "inf"))
        elif len(doubles) == 1 and not inf_targets and not structs:
            wanted.append((doubles[0], "inf"))
        # Scalar NaN: the last unreachable _bad_expr arm. A lone double gets a NaN
        # case alongside its INF case: AppKit geometry tolerates INF but traps NaN
        # (split-view divider SIGTRAP on NaN, exit 0 on INF), so the pair tells the
        # emitter notNaN apart from finite. Structs with a NaN form keep their struct
        # case instead (one nan case per method preserves the id scheme); range and
        # edgeinsets have no NaN form and no longer veto the scalar case.
        nan_structs = [i for i in structs if STRUCT_DEFAULTS[kinds[i][0]][1]]
        if len(doubles) == 1 and not inf_targets and not nan_structs:
            wanted.append((doubles[0], "nan"))
        for target in idx:                      # every index parameter, not just the first
            wanted.append((target, "index"))
        # Unsigned negatives: every non-index NSUInteger parameter gets a -1 case.
        # Index-named params already have an index case (bound guard is tighter where
        # a bound exists); emitting both would double-report the same wrapping, so
        # the negative case covers the plain counts/lengths/locations the index rule
        # never reaches. One bad argument per call preserves attribution.
        for i, (k, _j, n) in enumerate(kinds):
            if k in ("ulong", "uint32") and not is_index(n):
                wanted.append((i, "negative"))
        # Scissor negative: exactly one scissor rect gets a negative-width case.
        # The struct has no INF form (unlike the viewport), so this is its only
        # measured candidate; the single-bad-component rule keeps the abort
        # attributable when other parameters are present.
        scissors = [i for i, (k, _j, _n) in enumerate(kinds) if k == "mtlscissorrect"]
        if len(scissors) == 1:
            wanted.append((scissors[0], "negative"))
        # Per-param ids: a method yielding two same-badness targets (two index params,
        # offset+stride negatives) shares one id under the historical scheme, so the
        # differential's id-keyed dicts keep only the last param. Suffix ambiguous groups
        # with .<param_index>_<param> (stable under renames via the index); single-target
        # groups keep the historical unsuffixed form, so only colliding ids change.
        group_counts = {}
        for _t, b in wanted:
            group_counts[b] = group_counts.get(b, 0) + 1
        plan_ids = set()
        for target, badness in wanted:
            args = [_bad_expr(k, badness) if i == target else _default_expr(k)
                    for i, (k, _j, _n) in enumerate(kinds)]
            pid = "%s.%s.%s%s" % (cls, plan.java_name, badness,
                                  (".%d_%s" % (target, kinds[target][2])
                                   if group_counts[badness] > 1 else ""))
            assert pid not in plan_ids, "duplicate probe id %s" % pid
            plan_ids.add(pid)
            out.append({
                "id": pid,
                "class": cls, "selector": plan.decl.selector,
                "guard": {"nil": "nonNull", "nan": "notNaN", "index": "index",
                          "inf": "finite", "negative": "nonNegative"}[badness],
                "param": kinds[target][2], "param_index": target, "badness": badness,
                # R10-H3: the ABI kind of the bad parameter (mtlviewport, double, ...).
                # case_entry records it on the evidence row, which is what lets the
                # generator's GUARD-DECOMPOSED passthrough tell a row that already
                # stores a struct decomposition from one that still needs expanding.
                "param_kind": kinds[target][0],
                "bound": (shipped_bound(cls, plan.decl.selector, kinds[target][2], target)
                          if badness == "index" else None),
                "bound_setup": None,
                # provenance: a bound is only as good as the guard it was read from, and every
                # bound now comes from the shipped wrapper rather than from a guess
                "bound_from": ("src/nsui/%s.java" % cls) if badness == "index" else None,
                "method": plan.java_name, "arity": len(kinds),
                "setup": ["%s recv = %s;" % (cls, recv_expr),
                          'if (recv == null) { System.out.println("NORECEIVER"); return; }'],
                "call": "recv.%s(%s);" % (plan.java_name, ", ".join(args)),
            })
    return out


def generated_shadow(repo, classes, use_evidence=True):
    """Generate `classes` into a scratch copy and compile only those files into a shadow dir.

    The generated files are compiled against the shipped tree's classes and prepended to the
    classpath, so the probe measures the generated wrapper while every other class stays shipped.
    Compiling the whole tree instead would fail: a regenerated file does not carry the
    hand-written members the rest of the toolkit calls (the merge question the audits raised).
    """
    import gen as genmod
    scratch = tempfile.mkdtemp(prefix="cggen-")
    shutil.copytree(repo, scratch, dirs_exist_ok=True, symlinks=True,
                    ignore=shutil.ignore_patterns("out", "codegen", ".git"))
    shipped_classes = tree_classes(repo)
    if shipped_classes is None:
        return None, None, None
    # --allow-drop: this shadow deliberately replaces shipped classes to measure the generated
    # code; the guard that protects a hand-written wrapper from losing its API is for real trees.
    cmd = [sys.executable, os.path.join(HERE, "gen.py"), "--repo", scratch, "--apply", "--allow-drop",
           "--patch-out", os.path.join(scratch, "gen.patch"),
           "--summary-out", os.path.join(scratch, "gen-summary.txt")]
    for c in classes:
        cmd += ["--class", c]
    for fw in ("AppKit", "Foundation", "QuartzCore", "Metal"):
        cmd += ["--framework", fw]
    if not use_evidence:
        # The recorded `unguarded` verdict only reproduces when the shadow really is unguarded: the
        # documented run re-emitted the guard from the same evidence and so could never falsify it.
        cmd += ["--guard-evidence", os.path.join(scratch, "no-evidence.json")]   # absent by design
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        sys.stderr.write("generation failed:\n" + (r.stdout + r.stderr)[-1500:])
        return None, None, None
    shadow = os.path.join(scratch, "shadow-classes")
    os.makedirs(shadow, exist_ok=True)
    files = [os.path.join(scratch, "src", "nsui", c + ".java") for c in classes
             if os.path.exists(os.path.join(scratch, "src", "nsui", c + ".java"))]
    if not files:
        return None, None, None
    # Compile the generated classes one at a time. A batch fails whenever two of them reference each
    # other in a way the shipped versions satisfied and the regenerated ones do not (a regenerated
    # file does not carry the hand-written members its siblings call), and one such pair used to
    # cost the whole slice its guarded measurement. A class that does not compile is removed from
    # the scratch, so the candidate filter drops its cases and nothing references it.
    failed = []
    for path in files:
        p = subprocess.run([JAVAC, "-cp", "%s:%s:%s" % (SVM_JAR, shipped_classes, shadow),
                            "-d", shadow, path], capture_output=True, text=True)
        if p.returncode != 0:
            failed.append(os.path.basename(path)[:-5])
            try:
                os.remove(path)
            except OSError:
                pass
    if failed:
        print("probe: %d generated class(es) did not compile and were left shipped: %s"
              % (len(failed), ", ".join(sorted(failed))[:120]))
    if not os.listdir(shadow):
        sys.stderr.write("probe: no generated class compiled\n")
        return None, None, None
    return shadow, shipped_classes, scratch


FACTORY_RE = re.compile(r"\n    public static (\w+) (\w+)\([^)]*\) \{")


def _method_at(text, start):
    """The source of the method whose opening brace is at/after `start` (brace matched)."""
    i = text.index("{", start)
    depth = 0
    while i < len(text):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
        i += 1
    return ""


def merge_factories(scratch, classes):
    """Probe scaffolding: the generated file plus the shipped `create(...)` factories.

    The generator deliberately emits no factories, but every probe needs a receiver, and only the
    hand-written wrapper knows how to build one. Only factories are merged — enough to probe, and
    clearly not the merge feature the toolkit would need for a real regeneration.
    """
    merged = os.path.join(scratch, "merged", "nsui")
    os.makedirs(merged, exist_ok=True)
    files = []
    for cls in classes:
        gen_path = os.path.join(scratch, "src", "nsui", cls + ".java")
        if not os.path.exists(gen_path):
            continue
        generated = open(gen_path, encoding="utf-8", errors="replace").read()
        shipped_path = os.path.join(REPO, "src", "nsui", cls + ".java")
        additions = []
        if os.path.exists(shipped_path):
            ship = open(shipped_path, encoding="utf-8", errors="replace").read()
            for m in FACTORY_RE.finditer(ship):
                if m.group(1) != cls or not m.group(2).startswith("create"):
                    continue
                additions.append(_method_at(ship, m.start()))
        if additions:
            cut = generated.rstrip().rfind("}")
            generated = (generated[:cut] + "\n" + "\n".join(additions) + "\n}\n")
        out = os.path.join(merged, cls + ".java")
        open(out, "w", encoding="utf-8").write(generated)
        files.append(out)
    return files


def tree_classes(repo):
    """Compile the tree's src/nsui once and return the classes directory."""
    out = os.path.join(repo, "out", "probe-classes")
    if os.path.isdir(out) and os.listdir(out):
        return out
    os.makedirs(out, exist_ok=True)
    src = [os.path.join(dp, f) for dp, _dn, fn in os.walk(os.path.join(repo, "src", "nsui"))
           for f in fn if f.endswith(".java")]
    if not src:
        return None
    p = subprocess.run([JAVAC, "-cp", SVM_JAR, "-d", out] + src, capture_output=True, text=True)
    if p.returncode != 0:
        sys.stderr.write((p.stdout + p.stderr)[-2000:])
        return None
    return out


PROBE_HELPERS = """package nsui;

import java.lang.foreign.MemorySegment;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// Shared by the chunked probes: a receiver built from the runtime, because a generated wrapper
/// carries no factory (plain init returns nil for the controller and view classes, so the real
/// initializers go through handles whose shape the tree already has). The cases live in
/// GuardProbeN classes because a JVM method is capped at 64 KB and a bulk scan has hundreds.
public abstract class GuardProbeBase {
    static {
        ObjC.init();
        try { ObjC.ensureFramework(\"AppKit\"); } catch (Throwable ignored) {}
    }

    static MemorySegment init0(String cls) {
        MemorySegment p = ObjC.msgSendId(ObjC.cls(cls), ObjC.sel(\"alloc\"));
        return ObjC.msgSendId(p, ObjC.sel(\"init\"));
    }

    static MemorySegment init1(String cls, String sel) {
        MemorySegment p = ObjC.msgSendId(ObjC.cls(cls), ObjC.sel(\"alloc\"));
        return ObjC.msgSendIdId(p, ObjC.sel(sel), ObjC.nsstring(\"\"));
    }

    static MemorySegment init2(String cls, String sel) {
        MemorySegment p = ObjC.msgSendId(ObjC.cls(cls), ObjC.sel(\"alloc\"));
        try {
            java.lang.invoke.MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(p, ObjC.sel(sel), MemorySegment.NULL,
                                                MemorySegment.NULL);
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    static MemorySegment init3(String cls, String sel) {
        MemorySegment p = ObjC.msgSendId(ObjC.cls(cls), ObjC.sel(\"alloc\"));
        try {
            java.lang.invoke.MethodHandle h =
                ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID, Arg.ID));
            return (MemorySegment) h.invokeExact(p, ObjC.sel(sel), ObjC.nsstring(\"Item\"),
                                                MemorySegment.NULL, ObjC.nsstring(\"\"));
        } catch (Throwable t) { throw new RuntimeException(t); }
    }
}
"""


def _chunk_source(name, cases, generated):
    """(source text, [(first_line, last_line)] per case), one class per chunk of cases."""
    body = ["package nsui;", "",
            "import java.lang.foreign.MemorySegment;",
            "import nsui.objc.ObjC;",
            "import nsui.objc.Sig;",
            "import static nsui.objc.Sig.Arg;",
            "import static nsui.objc.Sig.Ret;", "",
            "public final class %s extends GuardProbeBase {" % name,
            "    public static void main(String[] argv) {",
            "        int which = Integer.parseInt(argv[0]);", "        switch (which) {"]
    ranges = []
    for i, c in enumerate(cases):
        first = len(body) + 1
        body.append("        case %d: {" % i)
        for line in c["setup"]:
            body.append("            " + line)
        call = (c.get("call_gen") or c["call"]) if generated else c["call"]
        body.append("            " + call)
        body.append('            System.out.println("TOLERATED");')
        body.append("            return;")
        body.append("        }")
        ranges.append((first, len(body)))
    body += ["        default: return;", "        }", "    }", "}", ""]
    return "\n".join(body), ranges


def build_probe(repo, cases, shadow=None, shipped=None, generated=False, chunk=120):
    """Write and compile the probes, split across classes so no method exceeds the JVM limit.

    Returns (probe_classes_dir, tree_classes, mapping) where mapping[i] is the (class, local index)
    that runs case i.
    """
    work = tempfile.mkdtemp(prefix="cgprobe-")
    pkg = os.path.join(work, "nsui")
    os.makedirs(pkg, exist_ok=True)
    classes = shipped or tree_classes(repo)
    if classes is None:
        return None, None, None, None
    if shadow:
        classes = "%s:%s" % (shadow, classes)
    for cls in ([] if shadow else {c["class"] for c in cases}):
        path = os.path.join(repo, "src", "nsui", cls + ".java")
        if not os.path.exists(path):
            continue                       # a class the tree does not have: nothing to extend
        text = open(path, encoding="utf-8", errors="replace").read()
        if "static " + cls + " create(" in text:
            continue
        shipped_path = os.path.join(REPO, "src", "nsui", cls + ".java")
        if not os.path.exists(shipped_path):
            continue
        ship = open(shipped_path, encoding="utf-8", errors="replace").read()
        m = re.search(r"\n(    /// [^\n]*\n)*    public static %s create\([^)]*\) \{.*?\n    \}\n"
                      % re.escape(cls), ship, re.S)
        if m:
            text = text.replace("\n    /// Wrap an id as", m.group(0) + "\n    /// Wrap an id as", 1)
            open(path, "w", encoding="utf-8").write(text)
    base = os.path.join(pkg, "GuardProbeBase.java")
    open(base, "w", encoding="utf-8").write(PROBE_HELPERS)
    out = os.path.join(work, "out")
    os.makedirs(out, exist_ok=True)
    p = subprocess.run([JAVAC, "-cp", "%s:%s" % (SVM_JAR, classes), "-d", out, base],
                       capture_output=True, text=True)
    if p.returncode != 0:
        sys.stderr.write("probe: the shared base did not compile:\n" + (p.stdout + p.stderr)[-800:])
        return None, None, None, None
    # Each group is compiled on its own and, when it does not compile, bisected until the offending
    # cases are isolated. Relying on javac's error list instead was fragile: it is truncated (about
    # 100 errors per file), so a large group needed many retries and a wide run could exhaust them
    # and give up with nothing measured.
    counter = [0]
    dropped = [0]

    def compile_group(group):
        name = "GuardProbe%d" % counter[0]
        counter[0] += 1
        path = os.path.join(pkg, name + ".java")
        text, _ranges = _chunk_source(name, group, generated)
        open(path, "w", encoding="utf-8").write(text)
        p = subprocess.run([JAVAC, "-cp", "%s:%s:%s" % (SVM_JAR, classes, out), "-d", out, path],
                           capture_output=True, text=True)
        if p.returncode == 0:
            return group, [(name, i) for i in range(len(group))]
        try:
            os.remove(path)
        except OSError:
            pass
        if len(group) == 1:
            dropped[0] += 1
            return [], []
        mid = len(group) // 2
        k1, m1 = compile_group(group[:mid])
        k2, m2 = compile_group(group[mid:])
        return k1 + k2, m1 + m2

    kept, mapping = [], []
    for ci in range(0, len(cases), chunk):
        group_kept, group_mapping = compile_group(cases[ci:ci + chunk])
        kept.extend(group_kept)
        mapping.extend(group_mapping)
    if dropped[0]:
        print("probe: dropped %d candidate(s) that do not compile against the tree" % dropped[0])
    if not kept:
        sys.stderr.write("probe: no candidate survived the compile\n")
        return None, None, None, None
    return out, classes, mapping, kept

def filter_cases(cases, scratch):
    """Keep only candidates whose method exists with that arity in the generated file.

    A synthesized call can disagree with what the generator emitted (a renamed method, a reshaped
    parameter list); dropping those here keeps one bad candidate from failing the whole run, and
    they are reported as skipped rather than measured wrongly.
    """
    cache, out, dropped = {}, [], 0
    for c in cases:
        path = os.path.join(scratch, "src", "nsui", c["class"] + ".java")
        if path not in cache:
            cache[path] = open(path, encoding="utf-8", errors="replace").read() \
                if os.path.exists(path) else ""
        if c.get("baseline"):
            # A baseline is kept only for a class that was really generated: its setup names the
            # class, so a skipped class would make the probe reference something that does not exist.
            if cache[path]:
                out.append(c)
            else:
                dropped += 1
            continue
        m = re.search(r"public\s+[\w.<>\[\]]+\s+%s\s*\(([^)]*)\)" % re.escape(c["method"]),
                      cache[path])
        if not m or (0 if not m.group(1).strip() else m.group(1).count(",") + 1) != c["arity"]:
            dropped += 1
            continue
        out.append(c)
    if dropped:
        print("probe: skipped %d candidate(s) the generated class does not declare" % dropped)
    return out


def run_cases(repo, cases, timeout=25, generated=None, use_evidence=True):
    shadow = shipped = None
    if generated:
        shadow, shipped, scratch = generated_shadow(repo, generated, use_evidence=use_evidence)
        if shadow is None:
            return None
        cases = filter_cases(cases, scratch)
        if not cases:
            print("probe: no candidate survived validation against the generated class")
            return None
    probe_out, classes, mapping, cases = build_probe(repo, cases, shadow=shadow, shipped=shipped,
                                                     generated=bool(generated))
    if probe_out is None:
        return None
    results = []
    for i, c in enumerate(cases):
        probe_class, local = mapping[i]
        try:
            p = subprocess.run([JAVA, "-XstartOnFirstThread", "--enable-native-access=ALL-UNNAMED",
                                "-cp", "%s:%s" % (probe_out, classes),
                                "nsui." + probe_class, str(local)],
                               capture_output=True, text=True, timeout=timeout)
            rc, text = p.returncode, (p.stdout + p.stderr)
        except subprocess.TimeoutExpired as exc:
            rc, text = 124, str(exc)
        tail = [ln for ln in text.splitlines() if ln.strip()][-3:]
        if "NORECEIVER" in text:
            verdict = "no-receiver"        # the wrapper was never reached, so nothing was measured
        else:
            verdict = VERDICTS.get(rc, "exit-%d" % rc)
        guard_fired = False
        if verdict in STRICTER and "IllegalArgumentException" in text and "Bounds." in text:
            # The guard threw before the native abort: an open Metal encoder's teardown
            # asserts at process exit even when the IAE fired first (viewport INF reads
            # exit 134 with the guard's IAE line already printed). Attribute the outcome
            # to the guard, so the differential files it as fixed rather than STILL-ABORTS.
            # Both markers are required: a bare IAE match would also fire on setup or
            # chained exceptions, and only a Bounds frame proves the guard itself threw.
            guard_fired = True
            verdict = "java-exception"
        results.append({"id": c["id"], "class": c["class"], "guard": c["guard"],
                        "param": c.get("param"), "exit": rc, "verdict": verdict, "tail": tail,
                        "guard_fired": guard_fired})
        print("%-52s exit=%-4s %s" % (c["id"], rc, verdict))
    shutil.rmtree(os.path.dirname(probe_out), ignore_errors=True)
    return results


def merge_evidence(src, path):
    """Fold a previous enumeration's cases into the evidence file, upgrading weaker entries."""
    entries = json.load(open(src))["cases"]
    existing = json.load(open(path)) if os.path.exists(path) else {"cases": []}
    cases = existing.get("cases", [])
    # Keyed on the case and its kind like merge_entries: without badness, two tolerated
    # same-param rows (a .nan and a .inf control for one parameter) collapse to one key
    # and the second silently replaces the first, losing a measured verdict.
    by_key = {(c.get("class"), c.get("selector"), c.get("param"), c.get("badness")): i
              for i, c in enumerate(cases)}
    added = replaced = 0
    for e in entries:
        if e.get("verification") or e.get("baseline") or e.get("unguarded") == "java-exception":
            continue                       # a verification row, not evidence to emit from
        key = (e.get("class"), e.get("selector"), e.get("param"), e.get("badness"))
        i = by_key.get(key)
        if i is None:
            by_key[key] = len(cases)
            cases.append(e)
            added += 1
        elif e.get("guards") and not cases[i].get("guards"):
            cases[i] = e
            replaced += 1
    existing["cases"] = cases
    json.dump(existing, open(path, "w"), indent=2)
    print("merged %d new case(s), upgraded %d into %s (%d total)" % (added, replaced, path,
                                                                     len(cases)))
    return 0


STRICTER = ("objc-abort", "sigtrap", "sigill")


ABORT_VERDICTS = ("objc-abort", "sigtrap", "sigill")


def case_entry(case, verdict):
    """The evidence entry for a measured case, or None when there is nothing to record.

    Shared by the enumeration and the differential so a case the differential measures can be fed
    back: 22 of 24 residual differences existed only in the differential's own case set, so the
    generator never saw them and never acted.
    """
    if case.get("baseline") or not case.get("selector"):
        return None                     # a per-class baseline is scaffolding, not a case
    entry = {"class": case["class"], "selector": case["selector"], "param": case.get("param"),
             "param_index": case.get("param_index"), "probe_id": case["id"],
             "badness": case.get("badness"), "unguarded": verdict}
    # R10-H4: carry the parameter kind onto the row (non-key field: existing rows are
    # untouched, merge keys unchanged). Without it every struct row looks scalar and
    # the generator must re-derive the decomposition from its own plans each run.
    if case.get("param_kind"):
        entry["param_kind"] = case["param_kind"]
    if case.get("bound"):
        entry["bound"] = case["bound"]
        entry["bound_from"] = case.get("bound_from")
    if verdict in ABORT_VERDICTS:
        kind = case.get("badness")
        if kind == "nil":
            entry["guards"] = ['Bounds.nonNull({p}, "%s");' % case["selector"]]
        elif kind == "nan":
            entry["guards"] = ['Bounds.notNaN({p}, "%s");' % case["selector"]]
        elif kind == "inf":
            # R1-live: INF abort maps to the finite helper (AppKit notNaN stays, finite is
            # reserved for Metal's infinite-or-NaN validation). For the viewport struct this
            # single statement does not compile yet — R2 decomposes it to 6 component
            # finite guards null-first; the single form here is scaffolding until then.
            entry["guards"] = ['Bounds.finite({p}, "%s");' % case["selector"]]
        elif kind == "index" and case.get("bound"):
            entry["guards"] = ['Bounds.index({p}, %s, "%s");' % (case["bound"], case["selector"])]
        elif kind == "negative":
            entry["guards"] = ['Bounds.nonNegative({p}, "%s");' % case["selector"]]
        else:
            entry["needs_local_guard"] = "the corpus does not bound this index"
            entry["guards"] = []
    elif verdict == "tolerated":
        entry["control"] = "the unguarded call survived, so no guard is emitted"
        entry["guards"] = []
    else:
        # It fails safely in Java now (a fix landed), so this measurement supersedes an older abort
        # verdict rather than leaving both in the file. The generator skips a verification entry.
        entry["guards"] = []
        entry["verification"] = True
    return entry


def merge_entries(path, entries):
    """Fold entries into an evidence file, adding new ones and upgrading guardless duplicates."""
    existing = json.load(open(path)) if os.path.exists(path) else {"cases": []}
    cases = existing.get("cases", [])
    # Keyed on the case and its kind, not on the verdict: a later measurement of the same case is
    # the better one, and keying on the verdict left a stale abort and a fresh tolerated side by
    # side, so the generator emitted the same guard twice.
    by_key = {(c.get("class"), c.get("selector"), c.get("param"), c.get("badness")): i
              for i, c in enumerate(cases)}
    added = superseded = 0
    for e in entries:
        key = (e["class"], e["selector"], e.get("param"), e.get("badness"))
        i = by_key.get(key)
        if i is None:
            by_key[key] = len(cases)
            cases.append(e)
            added += 1
        else:
            cases[i] = e
            superseded += 1
    existing["cases"] = cases
    json.dump(existing, open(path, "w"), indent=2)
    return added, superseded, len(cases)


def _differential_slice(repo, args, classes):
    """The buckets for one slice of classes; None when a measurement could not be taken."""
    cases = enumerate_cases(classes, args.frameworks)
    shipped_cases = filter_cases(cases, repo)
    if not shipped_cases:
        print("no candidate exists in the shipped tree for %s" % ", ".join(classes))
        return None
    shipped = run_cases(repo, shipped_cases, timeout=args.timeout)
    if shipped is None:
        print("the shipped measurement could not be taken for %s" % ", ".join(classes))
        return None
    generated = run_cases(repo, cases, timeout=args.timeout, generated=classes)
    if generated is None:
        print("the guarded measurement could not be taken for %s" % ", ".join(classes))
        return None
    measured_ids = {r["id"] for r in generated}
    bare = run_cases(repo, [c for c in cases if c["id"] in measured_ids], timeout=args.timeout,
                     generated=classes, use_evidence=False)
    if bare is None:
        print("the guard-free measurement could not be taken for %s" % ", ".join(classes))
        return None
    by_id = {r["id"]: r for r in generated}
    bare_by_id = {r["id"]: r for r in bare}
    buckets = defaultdict(list)
    for r in shipped:
        g = by_id.get(r["id"])
        if g is None or r["id"].endswith(".__baseline"):
            continue
        s_v, g_v = r["verdict"], g["verdict"]
        if s_v == "tolerated":
            if g_v == "tolerated":
                buckets["unchanged-tolerated"].append((r["id"], s_v, g_v))
            else:
                b = bare_by_id.get(r["id"], {}).get("verdict")
                buckets["STRICTER-GUARD" if b == "tolerated" else "STRICTER-BODY"].append(
                    (r["id"], s_v, g_v))
        elif s_v in STRICTER:                      # the shipped wrapper aborts on this call
            if g_v in STRICTER:
                buckets["STILL-ABORTS"].append((r["id"], s_v, g_v))
            elif g_v == "java-exception":
                buckets["fixed"].append((r["id"], s_v, g_v))
            else:
                buckets["LOOSER"].append((r["id"], s_v, g_v))
        else:                                      # the shipped wrapper raises a Java exception
            if g_v in STRICTER:
                # The generated wrapper aborts where the shipped one raised a clear error: worse
                # than the code it replaces. Counting this as "fixed" hid exactly the regression a
                # rejected index bound causes.
                buckets["LESS-SAFE"].append((r["id"], s_v, g_v))
            elif g_v == "tolerated":
                buckets["LOOSER"].append((r["id"], s_v, g_v))
            else:
                buckets["parity"].append((r["id"], s_v, g_v))
    if getattr(args, "merge_evidence", None):
        # Feed every case this slice measured from its guard-free build back into the evidence, so
        # the next generator run acts on it instead of the difference surviving in reports only.
        case_by_id = {c["id"]: c for c in cases}
        entries = [e for e in (case_entry(case_by_id[r["id"]], r["verdict"]) for r in bare
                               if r["id"] in case_by_id) if e]
        added, replaced, total = merge_entries(args.merge_evidence, entries)
        print("   evidence: +%d, upgraded %d (%d total)" % (added, replaced, total))
    # Teardown-after-guard proof: how many of this slice's fixed cases measured the
    # guard's IAE before the process still aborted (stdout only; routing already files
    # them as fixed via the run_cases verdict flip, so buckets stay untouched).
    n_fix = len(buckets.get("fixed", []))
    n_torn = sum(1 for cid, _s, _g in buckets.get("fixed", [])
                 if by_id.get(cid, {}).get("guard_fired"))
    print("   fixed-via-teardown-guard: %d of %d fixed" % (n_torn, n_fix))
    return buckets, n_torn, n_fix


def fold_evidence(paths, target):
    """Merge several per-slice evidence files into one, using the same supersede rule.

    Parallel slices must not write the same evidence file (a read-modify-write race loses entries),
    so each writes its own and this folds them once.
    """
    added = superseded = 0
    for path in paths:
        if not os.path.exists(path):
            print("missing evidence file: %s" % path)
            continue
        a, r, _total = merge_entries(target, json.load(open(path)).get("cases", []))
        added += a
        superseded += r
    total = len(json.load(open(target)).get("cases", []))
    print("folded %d file(s): +%d, superseded %d (%d total)" % (len(paths), added, superseded, total))
    return 0


def merge_differential(paths):
    """Sum the buckets of several --differential runs and report them as one result.

    A full sweep is three measurements per class, so it is run as parallel slices; this is how the
    slices become one answer instead of four files nobody adds up.
    """
    buckets = defaultdict(list)
    missing = []
    for path in paths:
        if not os.path.exists(path):
            missing.append(path)
            continue
        for key, items in json.load(open(path)).items():
            buckets[key].extend(items)
    for name in ("fixed", "parity", "unchanged-tolerated", "STRICTER-GUARD", "STRICTER-BODY",
                 "STILL-ABORTS", "LESS-SAFE", "LOOSER"):
        print("%-22s %4d" % (name, len(buckets.get(name, []))))
    for name in ("STRICTER-GUARD", "STRICTER-BODY", "STILL-ABORTS", "LESS-SAFE", "LOOSER"):
        for cid, s_v, g_v in buckets.get(name, [])[:8]:
            print("   %-14s %-56s shipped=%-13s generated=%s" % (name, cid, s_v, g_v))
    if missing:
        print("missing result file(s): %s" % ", ".join(missing))
    bad = (len(buckets.get("STRICTER-GUARD", [])) + len(buckets.get("STILL-ABORTS", []))
           + len(buckets.get("LESS-SAFE", [])))
    print("defects (STRICTER-GUARD + STILL-ABORTS + LESS-SAFE): %d" % bad)
    return 1 if (bad or missing) else 0


def differential(repo, args):
    """Attribute every rejection: measure the shipped tree, the guarded tree and the bare tree.

    A slice is processed at a time because each class costs three measurements and a very wide
    invocation runs into the JVM's and the probe's limits: twenty classes complete, seventy-three
    did not, and the slice size is what makes a wide check usable.
    """
    classes = args.enumerate or []
    if not classes:
        print("--differential needs at least one class")
        return 2
    slice_size = getattr(args, "slice", 20) or 20
    buckets = defaultdict(list)
    failed = []
    torn_total = fixed_total = 0
    for i in range(0, len(classes), slice_size):
        chunk = classes[i:i + slice_size]
        print("-- differential slice %d/%d: %s"
              % (i // slice_size + 1, (len(classes) + slice_size - 1) // slice_size,
                 ", ".join(chunk)))
        part = _differential_slice(repo, args, chunk)
        if part is None:
            failed.append(chunk)
            continue
        part, n_torn, n_fix = part
        torn_total += n_torn
        fixed_total += n_fix
        for key, items in part.items():
            buckets[key].extend(items)
    for name in ("fixed", "parity", "unchanged-tolerated", "STRICTER-GUARD", "STRICTER-BODY",
                 "STILL-ABORTS", "LESS-SAFE", "LOOSER"):
        print("%-22s %4d" % (name, len(buckets.get(name, []))))
    print("fixed-via-teardown-guard: %d of %d fixed (all slices)" % (torn_total, fixed_total))
    # A guard that over-rejects, a case that still aborts, and a generated wrapper that aborts
    # where the shipped one raised a clear error are defects. STRICTER-BODY and LOOSER are
    # conformance differences, reported but not failures.
    bad = (len(buckets.get("STRICTER-GUARD", [])) + len(buckets.get("STILL-ABORTS", []))
           + len(buckets.get("LESS-SAFE", [])))
    for name in ("STRICTER-GUARD", "STRICTER-BODY", "STILL-ABORTS", "LESS-SAFE", "LOOSER"):
        for cid, s_v, g_v in buckets.get(name, [])[:12]:
            print("   %-14s %-56s shipped=%-13s generated=%s" % (name, cid, s_v, g_v))
    if failed:
        print("slices that could not be measured: %s" % "; ".join(", ".join(c) for c in failed))
    if args.out:
        json.dump({k: v for k, v in buckets.items()}, open(args.out, "w"), indent=2)
        print("wrote " + args.out)
    return 1 if (bad or failed) else 0


def enumerate_measure(repo, args):
    """Measure derived candidates and record the aborts as evidence the generator can use."""
    classes = args.enumerate or []
    if not classes:
        print("--enumerate needs at least one class")
        return 2
    cases = enumerate_cases(classes, args.frameworks)
    print("candidates: %d over %d class(es)" % (len(cases), len(classes)))
    results = run_cases(repo, cases, timeout=args.timeout, generated=classes,
                        use_evidence=not args.no_evidence)
    if results is None:
        return 2
    # Match by id, never by position: run_cases drops candidates that do not compile, and zipping
    # positionally then attributed every later result to the wrong case.
    by_id = {r["id"]: r for r in results}
    bad_receivers = {c["class"] for c in cases if c.get("baseline")
                     and by_id.get(c["id"], {}).get("verdict") != "tolerated"}
    if bad_receivers:
        print("dropping %d class(es) whose receiver cannot be built: %s"
              % (len(bad_receivers), ", ".join(sorted(bad_receivers))[:120]))
    entries = []
    counts = {}
    for case in cases:
        r = by_id.get(case["id"])
        if r is None:
            continue                     # dropped before it could be measured
        counts[r["verdict"]] = counts.get(r["verdict"], 0) + 1
        if case.get("baseline") or case["class"] in bad_receivers:
            continue
        if r["verdict"] == "no-receiver":
            continue
        entry = case_entry(case, r["verdict"])
        if entry is None:
            if args.record_all:
                entry = {"class": case["class"], "selector": case["selector"],
                         "param": case["param"], "param_index": case.get("param_index"),
                         "probe_id": case["id"], "badness": case.get("badness"),
                         "unguarded": r["verdict"], "guards": [], "verification": True}
            else:
                continue      # already fails safely in Java; nothing to record or emit
        entries.append(entry)
    print("\nmeasured: %s" % ", ".join("%s=%d" % kv for kv in sorted(counts.items())))
    print("measured-via-teardown-guard: %d" % sum(1 for r in results if r.get("guard_fired")))
    path = args.out or os.path.join(REPO, "codegen", "guard-evidence.json")
    if args.merge and os.path.exists(path):
        existing = json.load(open(path))
        old_cases = existing.get("cases", [])
        by_key = {(c.get("class"), c.get("selector"), c.get("param"), c.get("badness")): i
                  for i, c in enumerate(old_cases)}
        added = replaced = 0
        for e in entries:
            if e.get("baseline"):
                continue                       # scaffolding, not a case
            key = (e.get("class"), e.get("selector"), e.get("param"), e.get("badness"))
            i = by_key.get(key)
            if i is None:
                by_key[key] = len(old_cases)
                old_cases.append(e)
                added += 1
            else:
                old_cases[i] = e
                replaced += 1
        existing["cases"] = old_cases
        json.dump(existing, open(path, "w"), indent=2)
        print("merged %d new case(s), superseded %d in %s (%d total)"
              % (added, replaced, path, len(old_cases)))
    else:
        json.dump({"measured_with": "codegen/probe.py --enumerate",
                   "cases": entries}, open(path, "w"), indent=2)
        print("wrote %d case(s) to %s" % (len(entries), path))
    return 0


def main():
    ap = argparse.ArgumentParser(description="Measure bad-argument behaviour of a wrapper tree")
    ap.add_argument("--repo", default=".", help="tree whose src/nsui to probe")
    ap.add_argument("--out", help="write the measured evidence JSON here")
    ap.add_argument("--baseline", help="compare against a previous evidence file")
    ap.add_argument("--timeout", type=int, default=25)
    ap.add_argument("--enumerate", nargs="*", metavar="CLASS", default=None,
                    help="derive bad-argument candidates from the generator's plans for these "
                         "classes, measure them, and record what aborts")
    ap.add_argument("--shipped-baseline", metavar="PATH",
                    help="measure the derived candidates against the SHIPPED tree and record each "
                         "case's verdict there, so a generated-aborts/shipped-tolerates pair is "
                         "visible instead of being reported as a flaw in the shipped wrapper")
    ap.add_argument("--fold", nargs="*", metavar="PATH", default=None,
                    help="merge per-slice evidence files into codegen/guard-evidence.json")
    ap.add_argument("--merge-results", nargs="*", metavar="PATH", default=None,
                    help="sum the buckets of several --differential runs and report one result")
    ap.add_argument("--no-merge", action="store_true",
                    help="with --differential: do not fold the measured cases back into the "
                         "evidence (the default is to fold them into codegen/guard-evidence.json)")
    ap.add_argument("--merge-evidence", metavar="PATH",
                    help="with --differential: fold every case measured from the guard-free build "
                         "into this evidence file, so the difference is acted on rather than only "
                         "reported")
    ap.add_argument("--slice", type=int, default=20,
                    help="with --differential: classes per slice (20 measured to work; 73 does not)")
    ap.add_argument("--differential", action="store_true",
                    help="with --enumerate: measure each case against the shipped tree AND the "
                         "guarded generated tree, then report where the generated wrapper is "
                         "stricter than the shipped one, still aborts, or has become looser")
    ap.add_argument("--no-evidence", action="store_true",
                    help="with --enumerate: generate the shadow WITHOUT the guard evidence, so a "
                         "recorded unguarded verdict can be reproduced and falsified")
    ap.add_argument("--record-all", action="store_true",
                    help="with --enumerate: record every measured verdict, including calls that "
                         "already fail safely, so a verification run can classify all of them")
    ap.add_argument("--merge-from", metavar="FILE",
                    help="merge an earlier enumeration JSON into the evidence file instead of "
                         "measuring again")
    ap.add_argument("--merge", action="store_true",
                    help="with --enumerate: merge the findings into the evidence file instead of "
                         "replacing it")
    ap.add_argument("--framework", dest="frameworks", action="append",
                    default=["AppKit", "Foundation", "QuartzCore", "Metal"])
    ap.add_argument("--generated", nargs="*", metavar="CLASS", default=None,
                    help="generate these classes and probe the generated versions (they shadow "
                         "the shipped ones on the classpath)")
    ap.add_argument("--strip-guards", nargs="*", metavar="CLASS", default=None,
                    help="scratch copy with every Bounds guard removed from these classes, so the "
                         "measurement shows what the call does WITHOUT the guard")
    args = ap.parse_args()
    repo = os.path.abspath(args.repo)
    if args.strip_guards is not None:
        repo = strip_guards(repo, args.strip_guards)
    if args.fold is not None:
        return fold_evidence(args.fold, os.path.join(HERE, "guard-evidence.json"))
    if args.merge_results is not None:
        return merge_differential(args.merge_results)
    if args.differential:
        # Close the loop by default. A difference the differential measures and does not record is
        # re-reported every run and never acted on: NSView.addSubview:nil was measured, listed as
        # LOOSER, and stayed absent from the evidence because feeding it back was an option nobody
        # passed after the first time.
        if not args.no_merge and not args.merge_evidence:
            args.merge_evidence = os.path.join(HERE, "guard-evidence.json")
        return differential(repo, args)
    if args.shipped_baseline:
        cases = enumerate_cases(args.enumerate or [], args.frameworks)
        # the shipped class may not declare the generated name or arity, so validate here too
        cases = filter_cases(cases, repo)
        if not cases:
            print("no candidate exists in the shipped tree")
            return 2
        results = run_cases(repo, cases, timeout=args.timeout)      # no shadow: the shipped tree
        if results is None:
            return 2
        verdicts = {r["id"]: r["verdict"] for r in results}
        json.dump({"measured_with": "codegen/probe.py --shipped-baseline (the shipped wrappers)",
                   "verdicts": verdicts}, open(args.shipped_baseline, "w"), indent=2)
        from collections import Counter
        print("shipped baseline: %d case(s) %s" % (len(verdicts),
                                                   dict(Counter(verdicts.values()))))
        return 0
    if args.merge_from:
        return merge_evidence(args.merge_from, args.out or os.path.join(REPO, "codegen",
                                                                       "guard-evidence.json"))
    if args.enumerate is not None:
        return enumerate_measure(repo, args)
    if not (args.generated or args.strip_guards is not None) and not args.baseline and not args.out:
        pass
    results = run_cases(repo, CASES, timeout=args.timeout, generated=args.generated)
    if results is None:
        return 2
    if args.baseline:
        base = {r["id"]: r for r in json.load(open(args.baseline))["cases"]}
        print("\n%-48s %-14s -> %s" % ("case", "baseline", "now"))
        for r in results:
            b = base.get(r["id"], {}).get("verdict", "?")
            print("%-48s %-14s -> %-14s %s" % (r["id"], b, r["verdict"],
                                               "OK" if b == "objc-abort" and r["verdict"] == "java-exception"
                                               else ("CHANGED" if b != r["verdict"] else "")))
    if args.out:
        json.dump({"repo": repo, "cases": results}, open(args.out, "w"), indent=2)
        print("wrote " + args.out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
