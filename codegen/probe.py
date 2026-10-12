#!/usr/bin/env python3
"""CodeGen bad-argument probe — measure what a wrapper does with a bad argument.

Guards in this toolkit exist because a nil argument, an out-of-range index or NaN geometry makes
AppKit abort the process (exit 134 / 133 / 132) instead of returning an error. That knowledge
cannot be read out of a header — `- (void)addChildViewController:(NSViewController *)childViewController`
carries no nullability annotation and NSViewController.h has no NS_ASSUME_NONNULL region — so it is
measured here, one child process per case, and recorded as evidence the generator consumes.

  python3 codegen/probe.py --repo . --out codegen/guard-evidence.json
  python3 codegen/probe.py --repo /tmp/generated --baseline codegen/guard-evidence.json

Exit codes are read directly: 0 tolerated, 1 a Java exception (a guard fired), 134 uncaught ObjC
exception, 133 SIGTRAP (NaN geometry), 132 SIGILL (self/ancestor cycle).
"""

import argparse
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
SCALAR_DEFAULTS = {
    "long": "0", "int": "0", "short": "(short) 0", "byte": "(byte) 0", "uint32": "0",
    "boolean": "false", "double": "0.0", "float": "0.0f",
}
OBJECT_KINDS = ("id", "string")


def _default_expr(kind):
    if kind in STRUCT_DEFAULTS:
        return STRUCT_DEFAULTS[kind][0]
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
    if kind in STRUCT_DEFAULTS:
        return STRUCT_DEFAULTS[kind][1]
    if kind == "double":
        return "Double.NaN"
    if kind == "float":
        return "Float.NaN"
    return _default_expr(kind)


def collection_like(cls_name):
    """Whether the toolkit's wrapper for a class offers count(), i.e. it behaves as a collection."""
    path = os.path.join(REPO, "src", "nsui", cls_name + ".java")
    if not os.path.exists(path):
        return False
    return bool(re.search(r"public (long|int) count\s*\(", open(path, errors="replace").read()))


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
    when exactly one parameter is an object, for a NaN case when exactly one is geometry, and for
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


def discover_bound(sel, plans, wrappers, extra_plans=None):
    """(bound expression, setup line) for an index in `sel`, or (None, None).

    A bound is a no-argument accessor whose name says it counts (`numberOfItems`, `count`,
    `length`) and which returns a scalar. Failing that, a collection accessor is used when its name
    carries the noun the index names (`removeItemAtIndex:` -> `items()`, `moveRowAt...` ->
    `rows()`), because an unrelated collection's count would produce a guard that rejects valid
    indices. Nothing is guessed when neither exists: the case is reported instead.
    """
    scalars = ("long", "int", "short", "byte", "uint32")
    all_plans = list(plans) + list(extra_plans or [])     # an inherited counter still bounds it
    counters = sorted((q.java_name for q in all_plans
                       if not q.skip and not q.is_class and not q.params
                       and q.ret_java in scalars
                       and re.search(r"(?i)(count|numberof|length)", q.java_name)),
                      key=lambda n: (len(n), n))
    if counters:
        return "%s()" % counters[0], None
    m = re.search(r"([A-Z][A-Za-z]*?)At(?:Index|Row)|([A-Z][A-Za-z]*?)Index", sel or "")
    run = next((g for g in (m.groups() if m else ()) if g), "")
    if len(run) < 3:
        return None, None
    # Try the longest noun first (`SelectedTabViewItem` then `TabViewItem` then `Item`): the longer
    # match is the more specific accessor, and a shorter one risks an unrelated collection.
    parts = re.findall(r"[A-Z][a-z0-9]*", run)
    nouns = ["".join(parts[i:]) for i in range(len(parts))]
    for noun in nouns:
        if len(noun) < 3:
            continue
        for q in sorted(all_plans, key=lambda x: x.java_name):
            if q.skip or q.is_class or q.params:
                continue
            if noun.lower() not in q.java_name.lower() or q.ret_java not in wrappers:
                continue
            if not collection_like(q.ret_java):
                continue
            return ("__items == null ? 0 : __items.count()",
                    "%s __items = %s();" % (q.ret_java, q.java_name))
    return None, None


def _cases_for_class(cls, plans, wrappers, recv_expr, ancestors=None, skip_cases=None):
    """Nil/NaN/index candidates for one class, one bad argument per call.

    A nil case needs exactly one object parameter, a NaN case exactly one geometry parameter, and
    an index case a parameter that names an index with every other parameter a scalar — so the
    abort can only be that argument's doing. Other parameters get benign defaults.
    """
    out = []
    for plan in plans:
        if plan.skip or plan.is_class or len(plan.params) > 4:
            continue
        if (cls, plan.decl.selector) in (skip_cases or set()):
            continue
        kinds = [(k, jt, nm) for k, jt, nm in plan.params]
        bound, bound_setup = discover_bound(plan.decl.selector, plans, wrappers, ancestors)
        objs = [i for i, (k, _j, _n) in enumerate(kinds)
                if k in OBJECT_KINDS or k.startswith("typed:")]
        structs = [i for i, (k, _j, _n) in enumerate(kinds) if k in STRUCT_DEFAULTS]
        idx = [i for i, (k, _j, n) in enumerate(kinds)
               if k in ("long", "int", "short", "byte", "uint32") and "index" in n.lower()]
        wanted = []
        if len(objs) == 1:
            wanted.append((objs[0], "nil"))
        if len(structs) == 1 and STRUCT_DEFAULTS[kinds[structs[0]][0]][1]:
            wanted.append((structs[0], "nan"))
        if idx and not objs and not structs:
            wanted.append((idx[0], "index"))
        for target, badness in wanted:
            args = [_bad_expr(k, badness) if i == target else _default_expr(k)
                    for i, (k, _j, _n) in enumerate(kinds)]
            out.append({
                "id": "%s.%s.%s" % (cls, plan.java_name, badness),
                "class": cls, "selector": plan.decl.selector,
                "guard": {"nil": "nonNull", "nan": "notNaN", "index": "index"}[badness],
                "param": kinds[target][2], "param_index": target, "badness": badness,
                "bound": bound if badness == "index" else None,
                "bound_setup": bound_setup if badness == "index" else None,
                "method": plan.java_name, "arity": len(kinds),
                "setup": ["%s recv = %s;" % (cls, recv_expr),
                          'if (recv == null) { System.out.println("NORECEIVER"); return; }'],
                "call": "recv.%s(%s);" % (plan.java_name, ", ".join(args)),
            })
    return out


def generated_shadow(repo, classes):
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
    cmd = [sys.executable, os.path.join(HERE, "gen.py"), "--repo", scratch, "--apply",
           "--patch-out", os.path.join(scratch, "gen.patch"),
           "--summary-out", os.path.join(scratch, "gen-summary.txt")]
    for c in classes:
        cmd += ["--class", c]
    for fw in ("AppKit", "Foundation", "QuartzCore", "Metal"):
        cmd += ["--framework", fw]
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
    p = subprocess.run([JAVAC, "-cp", "%s:%s" % (SVM_JAR, shipped_classes), "-d", shadow] + files,
                       capture_output=True, text=True)
    if p.returncode != 0:
        sys.stderr.write("generated classes did not compile:\n" + (p.stdout + p.stderr)[-1500:])
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
    body = ["package nsui;", "",
            "import java.lang.foreign.MemorySegment;",
            "import nsui.objc.ObjC;",
            "import nsui.objc.Sig;",
            "import static nsui.objc.Sig.Arg;",
            "import static nsui.objc.Sig.Ret;", "",
            "public final class %s extends GuardProbeBase {" % name,
            "    public static void main(String[] argv) {",
            "        int which = Integer.parseInt(argv[0]);", "        switch (which) {"]
    for i, c in enumerate(cases):
        body.append("        case %d: {" % i)
        for line in c["setup"]:
            body.append("            " + line)
        call = (c.get("call_gen") or c["call"]) if generated else c["call"]
        body.append("            " + call)
        body.append('            System.out.println("TOLERATED");')
        body.append("            return;")
        body.append("        }")
    body += ["        default: return;", "        }", "    }", "}", ""]
    return "\n".join(body)


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
        return None, None, None
    if shadow:
        classes = "%s:%s" % (shadow, classes)
    for cls in ([] if shadow else {c["class"] for c in cases}):
        path = os.path.join(repo, "src", "nsui", cls + ".java")
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
    sources = []
    base = os.path.join(pkg, "GuardProbeBase.java")
    open(base, "w", encoding="utf-8").write(PROBE_HELPERS)
    sources.append(base)
    mapping = []
    for ci in range(0, len(cases), chunk):
        group = cases[ci:ci + chunk]
        name = "GuardProbe%d" % (ci // chunk)
        path = os.path.join(pkg, name + ".java")
        open(path, "w", encoding="utf-8").write(_chunk_source(name, group, generated))
        sources.append(path)
        mapping.extend((name, li) for li in range(len(group)))
    out = os.path.join(work, "out")
    os.makedirs(out, exist_ok=True)
    p = subprocess.run([JAVAC, "-cp", "%s:%s" % (SVM_JAR, classes), "-d", out] + sources,
                       capture_output=True, text=True)
    if p.returncode != 0:
        sys.stderr.write("probe compile failed:\n" + (p.stdout + p.stderr)[-2000:])
        return None, None, None
    return out, classes, mapping


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


def run_cases(repo, cases, timeout=25, generated=None):
    shadow = shipped = None
    if generated:
        shadow, shipped, scratch = generated_shadow(repo, generated)
        if shadow is None:
            return None
        cases = filter_cases(cases, scratch)
        if not cases:
            print("probe: no candidate survived validation against the generated class")
            return None
    probe_out, classes, mapping = build_probe(repo, cases, shadow=shadow, shipped=shipped,
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
        results.append({"id": c["id"], "class": c["class"], "guard": c["guard"],
                        "param": c.get("param"), "exit": rc, "verdict": verdict, "tail": tail})
        print("%-52s exit=%-4s %s" % (c["id"], rc, verdict))
    shutil.rmtree(os.path.dirname(probe_out), ignore_errors=True)
    return results


def merge_evidence(src, path):
    """Fold a previous enumeration's cases into the evidence file, upgrading weaker entries."""
    entries = json.load(open(src))["cases"]
    existing = json.load(open(path)) if os.path.exists(path) else {"cases": []}
    cases = existing.get("cases", [])
    by_key = {(c.get("class"), c.get("selector"), c.get("param"), c.get("unguarded")): i
              for i, c in enumerate(cases)}
    added = replaced = 0
    for e in entries:
        if e.get("verification") or e.get("baseline") or e.get("unguarded") == "java-exception":
            continue                       # a verification row, not evidence to emit from
        key = (e.get("class"), e.get("selector"), e.get("param"), e.get("unguarded"))
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


def enumerate_measure(repo, args):
    """Measure derived candidates and record the aborts as evidence the generator can use."""
    classes = args.enumerate or []
    if not classes:
        print("--enumerate needs at least one class")
        return 2
    cases = enumerate_cases(classes, args.frameworks)
    print("candidates: %d over %d class(es)" % (len(cases), len(classes)))
    results = run_cases(repo, cases, timeout=args.timeout, generated=classes)
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
        entry = {"class": case["class"], "selector": case["selector"], "param": case["param"],
                 "param_index": case["param_index"], "probe_id": case["id"],
                 "unguarded": r["verdict"]}
        if r["verdict"] in ("objc-abort", "sigtrap", "sigill"):
            if case["badness"] == "nil":
                entry["guards"] = ['Bounds.nonNull({p}, "%s");' % case["selector"]]
            elif case["badness"] == "nan":
                entry["guards"] = ['Bounds.notNaN({p}, "%s");' % case["selector"]]
            elif case.get("bound"):
                if case.get("bound_setup"):
                    entry["setup"] = [case["bound_setup"]]
                entry["guards"] = ['Bounds.index({p}, %s, "%s");' % (case["bound"],
                                                                    case["selector"])]
            else:
                entry["needs_local_guard"] = ("no count accessor on this class to bound the index; "
                                              "supply the guard statement in the evidence")
                entry["guards"] = []
        elif r["verdict"] == "tolerated":
            entry["control"] = "the unguarded call survived, so no guard is emitted"
            entry["guards"] = []
        elif args.record_all:
            entry["guards"] = []
            entry["verification"] = True      # measured safe; not evidence to emit from
        else:
            continue          # already fails safely in Java; nothing to record or emit
        entries.append(entry)
    print("\nmeasured: %s" % ", ".join("%s=%d" % kv for kv in sorted(counts.items())))
    path = args.out or os.path.join(REPO, "codegen", "guard-evidence.json")
    if args.merge and os.path.exists(path):
        existing = json.load(open(path))
        old_cases = existing.get("cases", [])
        by_key = {(c.get("class"), c.get("selector"), c.get("param"), c.get("unguarded")): i
                  for i, c in enumerate(old_cases)}
        added = replaced = 0
        for e in entries:
            key = (e["class"], e["selector"], e["param"], e["unguarded"])
            i = by_key.get(key)
            if i is None:
                by_key[key] = len(old_cases)
                old_cases.append(e)
                added += 1
            elif e.get("guards") and not old_cases[i].get("guards"):
                # a case that could not be guarded before, but a discovered bound now allows it
                old_cases[i] = e
                replaced += 1
        existing["cases"] = old_cases
        json.dump(existing, open(path, "w"), indent=2)
        print("merged %d new case(s) and upgraded %d into %s (%d total)"
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
