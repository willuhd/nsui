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
        "RANGE", "TRANSFORM3D", "BYTE", "EDGEINSETS")
ARGS = ("ID", "INT", "BOOL", "DOUBLE", "RECT", "POINT", "SIZE", "FLOAT", "RANGE",
        "REGION", "TRANSFORM3D", "MTLVIEWPORT", "MTLSCISSORRECT", "BYTE", "EDGEINSETS")


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
    if t in ("q", "Q", "i", "I", "l", "L", "s", "S"):
        return "INT"
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
        self.libobjc.sel_registerName.restype = ctypes.c_void_p
        self.libobjc.sel_registerName.argtypes = [ctypes.c_char_p]
        self.libobjc.class_getInstanceMethod.restype = ctypes.c_void_p
        self.libobjc.class_getInstanceMethod.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        self.libobjc.class_getClassMethod.restype = ctypes.c_void_p
        self.libobjc.class_getClassMethod.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        self.libobjc.method_getTypeEncoding.restype = ctypes.c_char_p
        self.libobjc.method_getTypeEncoding.argtypes = [ctypes.c_void_p]
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

    def encoding(self, cls_name, selector):
        """(encoding_string, 'inst'|'class') or (None, reason)."""
        cls = self.get_class(cls_name)
        if not cls:
            return None, "no-class"
        sel = self.libobjc.sel_registerName(selector.encode("utf-8"))
        m = self.libobjc.class_getInstanceMethod(cls, sel)
        kind = "inst"
        if not m:
            m = self.libobjc.class_getClassMethod(cls, sel)
            kind = "class"
        if not m:
            return None, "no-method"
        enc = self.libobjc.method_getTypeEncoding(m)
        if not enc:
            return None, "no-encoding"
        return enc.decode("utf-8", "replace"), kind
