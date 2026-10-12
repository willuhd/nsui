#!/usr/bin/env python3
"""objcabi.py — the Python mirror of tests/SignatureConformanceTest's type-encoding
parser plus a ctypes bridge to the Objective-C runtime.

Standard library only (ctypes is stdlib). Nothing here parses headers: this module
answers exactly one question for a (class, selector, class-or-instance) triple:
    "what is the REAL ABI type encoding, and which Sig shape does it collapse to?"

The technique is the same one SignatureConformanceTest uses through FFM:
    objc_getClass -> class_getInstanceMethod / class_getClassMethod
                  -> method_getTypeEncoding
Frameworks must be dlopen()ed first, because objc_getClass returns NULL for a class
whose image is not loaded.
"""
from __future__ import annotations

import ctypes
import ctypes.util
import os

# Matching SignatureConformanceTest's QUAL set (qualifiers: r n N o O o R V).
QUAL = "rnNoOoRV"

# Sig.Ret / Sig.Arg universe (the shape vocabulary is read from Sig.java by gen.py).
RETS = ("VOID", "ID", "INT", "BOOL", "DOUBLE", "RECT", "POINT", "SIZE", "FLOAT",
        "RANGE", "TRANSFORM3D", "BYTE", "EDGEINSETS", "INT32", "SHORT")
ARGS = ("ID", "INT", "BOOL", "DOUBLE", "RECT", "POINT", "SIZE", "FLOAT", "RANGE",
        "REGION", "TRANSFORM3D", "MTLVIEWPORT", "MTLSCISSORRECT", "BYTE", "EDGEINSETS",
        "INT32", "SHORT")

# R1 opt-out: class names whose +alloc must never be sent during concrete-class
# realization.  A side-effecting allocator (singleton, shared instance, an object that
# needs -init arguments) would be observable, so those names are excluded here and the
# named class object is used unchanged.  Empty by default: every class is realizable
# until matrix evidence moves a name into this set.
REALIZE_OPT_OUT = frozenset()


# --------------------------------------------------------------------- encoding

def parse_one(e, i):
    """Java parseOne() port. Returns (token, next_index)."""
    n = len(e)
    while i < n and (e[i] in QUAL or e[i].isdigit() or e[i] in "+-"):
        i += 1
    if i >= n:
        return None, i
    c = e[i]
    if c == "^":
        _, j = parse_one(e, i + 1)
        return "ID", j
    if c in "*#:":
        return "ID", i + 1
    if c in "{[(":
        close = {"{": "}", "[": "]", "(": ")"}[c]
        depth = 0
        j = i
        while j < n:
            d = e[j]
            if d == c:
                depth += 1
            elif d == close:
                depth -= 1
                if depth == 0:
                    break
            j += 1
        return e[i:j + 1], j + 1
    if c == "@":
        if i + 1 < n and e[i + 1] == "?":
            return "BLOCK", i + 2
        if i + 1 < n and e[i + 1] == '"':
            j = e.find('"', i + 2)
            return "ID", (j + 1 if j >= 0 else n)
        return "ID", i + 1
    return c, i + 1


def parse_one_raw(e, i):
    """parse_one, but a pointer stays a pointer: `^d`, `^@`, `^{_NSRange=QQ}`.

    The ABI fold treats an object and a pointer alike (both one machine word), which is correct for
    the call and wrong for deciding whether a parameter is an out slot.
    """
    n = len(e)
    while i < n and (e[i] in QUAL or e[i].isdigit() or e[i] in "+-"):
        i += 1
    if i >= n:
        return None, i
    if e[i] == "^":
        _, j = parse_one_raw(e, i + 1)
        return e[i:j], j
    return parse_one(e, i)


def parse_types_raw(e):
    """Raw token stream: scalars as written, pointers as `^…`, objects as ID/BLOCK."""
    out = []
    i = 0
    while i < len(e):
        t, j = parse_one_raw(e, i)
        if t is None or j <= i:
            break
        out.append(t)
        i = j
    return out


def parse_types(e):
    out = []
    i = 0
    while i < len(e):
        t, j = parse_one(e, i)
        if t is None or j <= i:
            break
        out.append(t)
        i = j
    return out


def abi(t):
    """Java abi() port: encoding token -> Sig ABI class."""
    if t in ("ID", "BLOCK"):
        return "ID"
    if t == "v":
        return "VOID"
    if t == "d":
        return "DOUBLE"
    if t == "f":
        return "FLOAT"
    if t == "B":
        return "BOOL"
    if t in ("c", "C"):
        return "BYTE"
    if t in ("q", "Q", "l", "L"):
        return "INT"
    if t in ("i", "I"):
        return "INT32"
    if t in ("s", "S"):
        return "SHORT"
    if t.startswith("["):
        # A fixed-size C array ([2f], [16C]) is a value type, never a scalar: it is
        # passed by pointer in the ABI this bridge models, exactly like an id.
        return "ID"
    if t.startswith("{"):
        return struct_kind(t)
    return "OTHER"


def struct_name(t):
    """'{_NSRange=QQ}' -> '_NSRange'."""
    end = t.find("=")
    return t[1:end] if end > 0 else t[1:-1]


def struct_kind(t):
    """Java structKind() port, extended with the shapes Sig actually names."""
    name = struct_name(t)
    if name in ("CGRect", "_NSRect", "NSRect", "CGRect32"):
        return "RECT"
    if name in ("CGPoint", "_NSPoint", "NSPoint"):
        return "POINT"
    if name in ("CGSize", "_NSSize", "NSSize"):
        return "SIZE"
    if name in ("_NSRange", "NSRange"):
        return "RANGE"
    if name in ("_MTLRegion", "MTLRegion"):
        return "REGION"
    if name == "CATransform3D":
        return "TRANSFORM3D"
    if name in ("NSEdgeInsets", "_NSEdgeInsets", "NSDirectionalEdgeInsets"):
        return "EDGEINSETS"
    if name in ("MTLViewport", "_MTLViewport"):
        return "MTLVIEWPORT"
    if name in ("MTLScissorRect", "_MTLScissorRect"):
        return "MTLSCISSORRECT"
    return "OTHER"


def shape_of(enc):
    """Full method encoding -> (ret, [args]) using only the args after (self, _cmd)."""
    toks = parse_types(enc)
    if len(toks) < 3:
        return None
    return abi(toks[0]), tuple(abi(t) for t in toks[3:])


# ------------------------------------------------------------- struct size probe
# For the "structs >16 bytes" feasibility question we need real sizes. The encoding
# itself carries every field, so ctypes can build a throwaway Structure and measure it.

_CTYPES_SCALAR = {
    "c": ctypes.c_int8, "C": ctypes.c_uint8, "B": ctypes.c_bool,
    "s": ctypes.c_short, "S": ctypes.c_ushort,
    "i": ctypes.c_int, "I": ctypes.c_uint,
    "l": ctypes.c_long, "L": ctypes.c_ulong,
    "q": ctypes.c_longlong, "Q": ctypes.c_ulonglong,
    "f": ctypes.c_float, "d": ctypes.c_double,
}

_size_cache = {}
_struct_seq = [0]


def _ctypes_of(tok, i):
    """Build a ctypes type from an encoding token; returns (pytype, next_index)."""
    while i < len(tok) and (tok[i] in QUAL or tok[i].isdigit() or tok[i] in "+-"):
        i += 1
    if i >= len(tok):
        return ctypes.c_void_p, i
    c = tok[i]
    if c == "^" or c in "*#:":
        return ctypes.c_void_p, (i + 1 if c != "^" else i + 1)
    if c == "@":
        if i + 1 < len(tok) and tok[i + 1] == "?":
            return ctypes.c_void_p, i + 2
        if i + 1 < len(tok) and tok[i + 1] == '"':
            j = tok.find('"', i + 2)
            return ctypes.c_void_p, (j + 1 if j >= 0 else len(tok))
        return ctypes.c_void_p, i + 1
    if c == "{":
        depth, j = 0, i
        while j < len(tok):
            if tok[j] == "{":
                depth += 1
            elif tok[j] == "}":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        return _struct_type(tok[i:j + 1]), j + 1
    if c == "[":
        # [Ntype] fixed array
        k = i + 1
        num = ""
        while k < len(tok) and tok[k].isdigit():
            num += tok[k]
            k += 1
        elem, k2 = _ctypes_of(tok, k)
        return elem * (int(num) if num else 0), k2 + 1
    if c in _CTYPES_SCALAR:
        return _CTYPES_SCALAR[c], i + 1
    return ctypes.c_void_p, i + 1


def _struct_type(tok):
    """'{Name=fields}' -> an anonymous ctypes.Structure with those fields."""
    key = tok
    if key in _size_cache:
        return _size_cache[key]
    _struct_seq[0] += 1
    name = "S%d" % _struct_seq[0]
    fields = []
    body = tok[1:-1]
    eq = body.find("=")
    if eq >= 0:
        body = body[eq + 1:]
    i = 0
    idx = 0
    while i < len(body):
        while i < len(body) and body[i] in '"':
            j = body.find('"', i + 1)
            i = (j + 1) if j >= 0 else len(body)
        if i >= len(body):
            break
        t, i = _ctypes_of(body, i)
        if t is None:
            break
        fields.append(("f%d" % idx, t))
        idx += 1
    try:
        cls = type(name, (ctypes.Structure,), {"_fields_": fields})
        _size_cache[key] = cls
        return cls
    except Exception:
        _size_cache[key] = ctypes.c_void_p
        return ctypes.c_void_p


def struct_bytes(tok):
    """By-value size in bytes of a struct encoding token, or None."""
    if not tok.startswith("{"):
        v = _CTYPES_SCALAR.get(tok)
        return ctypes.sizeof(v) if v else None
    t = _struct_type(tok)
    try:
        return ctypes.sizeof(t)
    except Exception:
        return None


# ------------------------------------------------------------------- runtime

class ObjCRuntime:
    """ctypes bridge to libobjc. Mirrors SignatureConformanceTest.initRuntime()."""

    def __init__(self, frameworks=("AppKit", "Foundation", "QuartzCore")):
        self.libobjc = ctypes.CDLL("/usr/lib/libobjc.A.dylib")
        self.libobjc.objc_getClass.restype = ctypes.c_void_p
        self.libobjc.objc_getClass.argtypes = [ctypes.c_char_p]
        # Every pointer-returning libobjc call needs an explicit restype: ctypes assumes c_int,
        # which truncates a 64-bit pointer and segfaults the moment it is dereferenced.
        self.libobjc.objc_getProtocol.restype = ctypes.c_void_p
        self.libobjc.objc_getProtocol.argtypes = [ctypes.c_char_p]
        self.libobjc.objc_getClassList.restype = ctypes.c_int
        self.libobjc.objc_getClassList.argtypes = [ctypes.c_void_p, ctypes.c_int]
        self.libobjc.class_conformsToProtocol.restype = ctypes.c_bool
        self.libobjc.class_conformsToProtocol.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        self.libobjc.class_getName.restype = ctypes.c_char_p
        self.libobjc.class_getName.argtypes = [ctypes.c_void_p]
        self.libobjc.sel_registerName.restype = ctypes.c_void_p
        self.libobjc.sel_registerName.argtypes = [ctypes.c_char_p]
        self.libobjc.class_getInstanceMethod.restype = ctypes.c_void_p
        self.libobjc.class_getInstanceMethod.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        self.libobjc.class_getClassMethod.restype = ctypes.c_void_p
        self.libobjc.class_getClassMethod.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        self.libobjc.method_getTypeEncoding.restype = ctypes.c_char_p
        self.libobjc.method_getTypeEncoding.argtypes = [ctypes.c_void_p]
        # R1: object_getClass(instance) names the concrete class; objc_msgSend sends +alloc.
        self.libobjc.object_getClass.restype = ctypes.c_void_p
        self.libobjc.object_getClass.argtypes = [ctypes.c_void_p]
        self.libobjc.objc_msgSend.restype = ctypes.c_void_p
        self.libobjc.objc_msgSend.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        self._sel_alloc = self.libobjc.sel_registerName(b"alloc")
        self._concrete_cache = {}
        self._protocol_cache = {}
        self._instances = []            # keeps realized instances alive (never -init)
        self.realize_opt_out = set(REALIZE_OPT_OUT)
        self.loaded = {}
        for fw in frameworks:
            self.load_framework(fw)

    def load_framework(self, fw):
        if fw in self.loaded:
            return self.loaded[fw]
        path = "/System/Library/Frameworks/%s.framework/%s" % (fw, fw)
        try:
            ctypes.CDLL(path, mode=ctypes.RTLD_GLOBAL)
            self.loaded[fw] = True
        except OSError:
            self.loaded[fw] = False
        return self.loaded[fw]

    def get_class(self, name):
        return self.libobjc.objc_getClass(name.encode("utf-8"))

    def protocol_conforming_classes(self, name):
        """Every loaded class that conforms to the protocol `name` (cached).

        A protocol can have many implementations and they do not all implement every selector
        (the class that answers first in the runtime's class list may be a stub), so resolution
        walks them. Classes whose name contains the protocol's are tried first, which lands on
        MTLToolsTexture for MTLTexture rather than an unrelated conformer.
        """
        if name in self._protocol_cache:
            return self._protocol_cache[name]
        out = []
        proto = self.libobjc.objc_getProtocol(name.encode("utf-8"))
        if proto:
            total = self.libobjc.objc_getClassList(None, 0)
            if total > 0:
                buf = (ctypes.c_void_p * total)()
                n = self.libobjc.objc_getClassList(buf, total)
                for i in range(n):
                    cls = buf[i]
                    if cls and self.libobjc.class_conformsToProtocol(cls, proto):
                        out.append(cls)
            named = [(self.class_name(c), c) for c in out]
            named.sort(key=lambda t: (name not in t[0], len(t[0]), t[0]))
            out = [c for _n, c in named]
        out = tuple(out)
        self._protocol_cache[name] = out
        return out

    def class_name(self, cls):
        n = self.libobjc.class_getName(cls)
        return n.decode("utf-8", "replace") if n else ""

    def _lookup(self, cls, sel, want_class=None):
        """(method, 'inst'|'class') for cls, or (None, _). class_get*Method walks the superclass
        chain, so a concrete subclass also reports inherited methods.

        `want_class` is the side the header declared. Both sides can exist for the same selector
        (NSBundle declares +pathForResource:ofType:inDirectory: and a private instance method), and
        asking the instance side first made the generator drop 19 declared class methods as
        "runtime has only inst". The declared side wins; the other is the fallback.
        """
        if want_class:
            m = self.libobjc.class_getClassMethod(cls, sel)
            if m:
                return m, "class"
            return self.libobjc.class_getInstanceMethod(cls, sel), "inst"
        m = self.libobjc.class_getInstanceMethod(cls, sel)
        if m:
            return m, "inst"
        return self.libobjc.class_getClassMethod(cls, sel), "class"

    def has_method(self, cls_name, selector, want_class=None):
        """Whether the runtime class (or a conformer, for a protocol) implements the selector.

        Lookup only: no realization, so nothing is allocated and no +alloc side effect can run.
        That makes it safe to ask about any class in a scan, which `encoding()` is not.
        """
        cls = self.get_class(cls_name)
        sel = self.libobjc.sel_registerName(selector.encode("utf-8"))
        if cls:
            m, _kind = self._lookup(cls, sel, want_class)
            return bool(m)
        for cand in self.protocol_conforming_classes(cls_name):
            m, _kind = self._lookup(cand, sel, want_class)
            if m:
                return True
        return False

    def _concrete(self, name):
        """R1: the class object that actually receives `name`'s messages.

        objc_getClass returns the class *named by the header*.  For the Metal
        descriptor family that object is an abstract shell: the implementations live on
        a private concrete subclass (e.g. MTLStencilDescriptorInternal), so every lookup
        on the named class misses.  Realize it once by sending +alloc with objc_msgSend
        and reading object_getClass() of the returned instance.

        The instance is never sent -init, is kept in self._instances for the lifetime of
        this runtime, and the result is cached per class name: exactly one leaked alloc
        per class for the process lifetime.  Names in realize_opt_out (and classes whose
        metaclass does not implement +alloc) resolve to the named class unchanged.
        """
        if name in self._concrete_cache:
            return self._concrete_cache[name]
        cls = self.get_class(name)
        if not cls:
            return None
        if name in self.realize_opt_out:
            return cls
        if not self.libobjc.class_getClassMethod(cls, self._sel_alloc):
            return cls                     # never send +alloc where it is not implemented
        instance = self.libobjc.objc_msgSend(cls, self._sel_alloc)
        if instance:
            self._instances.append(instance)
            concrete = self.libobjc.object_getClass(instance)
            if concrete:
                cls = concrete
        self._concrete_cache[name] = cls
        return cls

    def encoding(self, cls_name, selector, want_class=None):
        """(encoding_string, 'inst'|'class') or (None, reason)."""
        cls = self.get_class(cls_name)
        sel = self.libobjc.sel_registerName(selector.encode("utf-8"))
        if cls:
            m, kind = self._lookup(cls, sel, want_class)
            if not m:
                # Only a miss reaches the realization, so no existing resolution changes.
                concrete = self._concrete(cls_name)
                if concrete and concrete != cls:
                    m, kind = self._lookup(concrete, sel, want_class)
        else:
            # Not a class: a protocol-backed wrapper. Resolve on an implementation of it.
            m, kind = None, None
            for cand in self.protocol_conforming_classes(cls_name):
                m, kind = self._lookup(cand, sel, want_class)
                if m:
                    break
            if not m and not self.protocol_conforming_classes(cls_name):
                return None, "no-class"
        if not m:
            return None, "no-method"
        enc = self.libobjc.method_getTypeEncoding(m)
        if not enc:
            return None, "no-encoding"
        return enc.decode("utf-8", "replace"), kind
