#!/usr/bin/env python3
"""hdrs.py - a stdlib-only reader for the Objective-C declaration subset that
appears in a macOS SDK public header: @interface / @protocol / @property /
instance and class methods.

It is deliberately a *lexer*, not a C parser: it strips comments and availability
macros, then splits an interface body into top-level ;-terminated statements,
tracking (), [], {} nesting so multiline declarations and block types survive.
Every declaration keeps its raw text and source line, so the generator can always
show what it read and fall back to hand-written code when it does not understand.

What it does NOT do (on purpose): typedef/enum body semantics, C function
declarations, macro expansion, categories' inherited members, or struct layout.
"""
from __future__ import annotations

import re

# Availability / attribute macros removed before declaration parsing. Both the
# NAME(...) and bare NAME forms are handled by strip_macros().
# A simple `typedef <base> <Name>;` maps a name to its underlying type so the generator can see
# through it: 183 of these are string typedefs (`typedef NSString *NSImageName;`) whose members
# otherwise degrade to raw MemorySegment. Complex typedefs (function pointers, blocks, NS_ENUM and
# NS_OPTIONS bodies) are deliberately not collected — their declarations do not match.
def _typedef_pairs(text):
    """(name, underlying type) pairs from a header's text.

    The declaration is scanned up to its `;` and its tokens are taken apart rather than matched
    with one rigid pattern, because a name may be followed by arbitrary macro invocations:
    `typedef NSString * NSImageName NS_SWIFT_BRIDGED_TYPEDEF API_AVAILABLE(ios(13.0));`.
    Blocks, function pointers and enum/option bodies are skipped on purpose.
    """
    out = {}
    for m in re.finditer(r"\btypedef\b([^;{}]*);", text):
        body = m.group(1)
        while "(" in body:                       # strip macro arguments, innermost first
            body = re.sub(r"\([^()]*\)", " ", body)
        if any(c in body for c in "^[]"):
            continue
        toks = body.split()
        while len(toks) > 2 and re.fullmatch(r"[A-Z][A-Z0-9_]*", toks[-1]):
            toks.pop()                           # trailing macro names are qualifiers, not the type
        if len(toks) < 2:
            continue
        name = toks[-1]
        stars = len(name) - len(name.lstrip("*"))   # `NSString *NSErrorDomain` glues the star on
        name = name.lstrip("*")
        base = " ".join(toks[:-1]).strip()
        if stars:
            base = (base + " " + "*" * stars).strip()
        base = re.sub(r"^(struct|enum|union)\s+", "", base).strip()
        if base and not name.startswith("_"):
            out.setdefault(name, base)
    return out


def collect_typedefs(sdk, frameworks):
    """{typedef name: underlying type text} for the frameworks given."""
    import os
    out = {}
    for framework in frameworks:
        root = os.path.join(sdk, "System", "Library", "Frameworks",
                            framework + ".framework", "Headers")
        if not os.path.isdir(root):
            continue
        for dirpath, _dirs, files in os.walk(root):
            for fn in sorted(files):
                if not fn.endswith(".h"):
                    continue
                try:
                    text = strip_comments(open(os.path.join(dirpath, fn),
                                               encoding="utf-8", errors="replace").read())
                except OSError:
                    continue
                out.update(_typedef_pairs(text))
    return out


# Qualifiers that may precede a type name. `null_unspecified` was missing, so
# `- (null_unspecified NSImage *)image` never resolved and the generated override returned a raw
# MemorySegment where its superclass returned NSImage, which Java rejects as an override.
TYPE_QUALIFIERS = ("nullable|nonnull|null_unspecified|_Nullable|_Nonnull|_Null_unspecified"
                   "|__kindof|__strong|__weak|__unsafe_unretained|__autoreleasing"
                   "|const|volatile|inout|out|oneway|bycopy|byref|IBOutlet|IBAction"
                   "|NS_REFINED_FOR_SWIFT")


def expand_type(text, typedefs, depth=4):
    """Replace a leading typedef name by what it stands for, transitively.

    A qualifier may precede the name (`nullable NSSpeechSynthesizerVoiceName`), and looking at the
    first identifier made the lookup ask about `nullable` and give up, which left 217 declarations
    typed as raw MemorySegment where the shipped wrapper uses String.
    """
    t = text or ""
    for _ in range(depth):
        m = re.search(r"(?:%s\s+)*([A-Za-z_]\w*)" % TYPE_QUALIFIERS, t)
        if not m:
            break
        base = typedefs.get(m.group(1))
        if not base:
            break
        t = t[:m.start(1)] + base + t[m.end(1):]
    return t


def apply_typedefs(ifaces, typedefs):
    """Expand every parameter and return type in place, once per parsed framework."""
    if not typedefs:
        return ifaces
    for iface in ifaces:
        for decl in iface.decls:
            decl.ret_raw = expand_type(decl.ret_raw, typedefs)
            for par in decl.params:
                par.type_raw = expand_type(par.type_raw, typedefs)
    return ifaces


MACROS = (
    "IBOutlet", "IBAction", "inout", "out", "oneway", "bycopy", "byref",
    "API_AVAILABLE", "API_DEPRECATED", "API_DEPRECATED_WITH_REPLACEMENT",
    "API_UNAVAILABLE", "API_UNAVAILABLE_MACCATALYST", "API_TO_BE_DEPRECATED",
    "NS_SWIFT_NAME", "NS_SWIFT_UI_ACTOR", "NS_SWIFT_ASYNC", "NS_SWIFT_ASYNC_NAME",
    "NS_REFINED_FOR_SWIFT", "NS_DESIGNATED_INITIALIZER", "NS_UNAVAILABLE",
    "NS_REQUIRES_SUPER", "NS_RETURNS_RETAINED", "NS_RETURNS_NOT_RETAINED",
    "NS_RETURNS_INNER_POINTER", "NS_NOESCAPE", "NS_CONSUMES_SELF",
    "NS_HEADER_AUDIT_BEGIN", "NS_HEADER_AUDIT_END", "DEPRECATED_ATTRIBUTE",
    "UNAVAILABLE_ATTRIBUTE", "CF_RETURNS_RETAINED", "CF_RETURNS_NOT_RETAINED",
    "CF_RELEASES_ARGUMENT", "NS_FORMAT_FUNCTION", "NS_FORMAT_ARGUMENT",
    "NS_DIRECT", "OBJC_ARC_UNAVAILABLE", "__attribute__",
    # bare attributes seen in the SDK (found by scanning the headers):
    "NS_SWIFT_NONISOLATED", "NS_SWIFT_SENDABLE", "NS_SWIFT_BRIDGED_TYPEDEF",
    "NS_SWIFT_UNAVAILABLE", "NS_PROTOCOL_REQUIRES_EXPLICIT_IMPLEMENTATION",
    "NSIMAGE_UNAVAILABLE_MACCATALYST", "NS_TYPED_ENUM",
    "NS_TYPED_EXTENSIBLE_ENUM", "NS_EXTENSIBLE_STRING_ENUM", "NS_OPTIONS",
    "API_UNAVAILABLE_BEGIN_MACCATALYST", "APPKIT_API_UNAVAILABLE_BEGIN_MACCATALYST",
    "CA_EXTERN_C_BEGIN", "CA_EXTERN_C_END",
)
_MACRO_RE = re.compile(r"\b(" + "|".join(re.escape(m) for m in MACROS) + r")\b")

# NOTE: a blanket all-caps sweep is NOT safe here: property and parameter names
# are sometimes all caps (NSURL *URL, PDF, TIFF), and a commentary attribute list
# would silently delete them. Only the curated attribute names above are stripped.


class Decl:
    """One method or property accessor ready to be ABI-checked."""

    __slots__ = ("owner", "kind", "is_class", "selector", "ret_raw", "params",
                 "nullable", "deprecated", "unavailable", "availability", "raw", "line",
                 "declared_in", "property", "multiline")

    def __init__(self, **kw):
        for k in self.__slots__:
            setattr(self, k, kw.get(k))

    def __repr__(self):
        return "<%s %s %s>" % ("+" if self.is_class else "-", self.selector,
                               "property" if self.property else "")


class Param:
    __slots__ = ("type_raw", "name", "is_block", "is_out", "is_generic", "nullable")

    def __init__(self, type_raw="", name="", is_block=False, is_out=False,
                 is_generic=False, nullable=False):
        self.type_raw = type_raw
        self.name = name
        self.is_block = is_block
        self.is_out = is_out
        self.is_generic = is_generic
        self.nullable = nullable

    def __repr__(self):
        return "Param(%r,%r,block=%s,out=%s)" % (self.type_raw, self.name,
                                                 self.is_block, self.is_out)


class Interface:
    __slots__ = ("kind", "name", "superclass", "category", "protocols", "file",
                 "line", "decls", "body", "unparsed")

    def __init__(self, kind, name, superclass, category, protocols, file, line, body):
        self.kind = kind              # interface | protocol | category
        self.name = name              # ObjC class/protocol name
        self.superclass = superclass
        self.category = category      # set when this is a category
        self.protocols = protocols
        self.file = file
        self.line = line
        self.body = body
        self.decls = []
        self.unparsed = []          # statements no branch claimed; reported rather than dropped


# ------------------------------------------------------------------ text level

def strip_comments(text):
    """Replace C and C++ comments with spaces, preserving offsets and lines."""
    out = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c == "/" and i + 1 < n and text[i + 1] == "*":
            j = text.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append(re.sub(r"[^\n]", " ", text[i:j]))
            i = j
        elif c == "/" and i + 1 < n and text[i + 1] == "/":
            j = text.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i))
            i = j
        elif c == '"':
            j = i + 1
            while j < n and text[j] != '"':
                j += 2 if text[j] == "\\" else 1
            out.append(text[i:min(j + 1, n)])
            i = min(j + 1, n)
        elif c == "#" and (i == 0 or text[i - 1] == "\n"):
            # Preprocessor line: blank it, preserving the newline. #if/#else
            # branches therefore BOTH survive, which is a known approximation
            # (counted and reported by the survey).
            j = text.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i))
            i = j
        else:
            out.append(c)
            i += 1
    return "".join(out)


def match_balanced(s, start):
    """Index just past the closer matching s[start] (one of ([{)."""
    openers = "([{"
    closers = ")]}"
    c = s[start]
    close = closers[openers.index(c)]
    depth = 0
    i = start
    n = len(s)
    while i < n:
        d = s[i]
        if d == c:
            depth += 1
        elif d == close:
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    return n


def match_balanced_angle(s, start):
    """Index just past the '>' matching s[start] == '<'.

    Angle brackets nest in a lightweight generic parameter list
    ('<ValueType : id <NSCopying>>'), so the closer is found by depth.
    """
    depth = 0
    i = start
    n = len(s)
    while i < n:
        if s[i] == "<":
            depth += 1
        elif s[i] == ">":
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    return n


def strip_macros(s):
    out = []
    i = 0
    n = len(s)
    while i < n:
        m = _MACRO_RE.match(s, i)
        if m:
            j = m.end()
            k = j
            while k < n and s[k].isspace():
                k += 1
            if k < n and s[k] == "(":
                i = match_balanced(s, k)
            else:
                i = j
            out.append(" ")
            continue
        out.append(s[i])
        i += 1
    return "".join(out)


_AVAIL_RE = re.compile(r"\b(API_AVAILABLE|API_UNAVAILABLE\w*|API_TO_BE_DEPRECATED"
                        r"|API_DEPRECATED\w*)\b")


def availability_of(stmt):
    """Version-gate payloads on a header statement, before strip_macros drops them.

    Returns the macro call(s) verbatim, e.g. `API_AVAILABLE(macos(14.0))`, joined by `;`
    (empty string when the statement carries no version gate). Display and survey only:
    no skip or emission decision reads this field.
    """
    out = []
    for m in _AVAIL_RE.finditer(stmt):
        k = m.end()
        while k < len(stmt) and stmt[k].isspace():
            k += 1
        if k < len(stmt) and stmt[k] == "(":
            out.append(stmt[m.start():match_balanced(stmt, k)])
        else:
            out.append(m.group(1))
    return ";".join(out)


def split_statements(body):
    """Split an interface body into top-level ;-terminated statements."""
    stmts = []
    start = 0
    depth = 0
    i = 0
    n = len(body)
    while i < n:
        c = body[i]
        if c == '"':
            j = i + 1
            while j < n and body[j] != '"':
                j += 2 if body[j] == "\\" else 1
            i = min(j + 1, n)
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == ";" and depth <= 0:
            stmts.append(body[start:i + 1])
            start = i + 1
        i += 1
    return stmts


def _line_of(text, pos):
    return text.count("\n", 0, pos) + 1


# ----------------------------------------------------------- declaration parse

def _classify_param(type_raw, name, nullable):
    t = type_raw
    return Param(
        type_raw=t,
        name=name,
        is_block="^" in t,
        is_out=bool(re.search(r"\*\s*\*", t)),
        is_generic="<" in t,
        nullable=nullable,
    )


def _nullability(text):
    t = text
    if re.search(r"\bnullable\b|_Nullable", t):
        return True
    if re.search(r"\bnonnull\b|_Nonnull", t):
        return False
    return None


def parse_method(stmt, owner, is_class, file, line, declared_in, in_protocol=False):
    s = strip_macros(stmt).strip()
    if not s or s[0] not in "+-":
        return None
    i = 1
    while i < len(s) and s[i].isspace():
        i += 1
    if i >= len(s) or s[i] != "(":
        return None
    j = match_balanced(s, i)
    ret_raw = s[i + 1:j - 1].strip()
    rest = s[j:].strip()
    if not rest:
        return None
    params = []
    labels = []
    k = 0
    n = len(rest)
    while k < n:
        while k < n and rest[k].isspace():
            k += 1
        m = re.match(r"[A-Za-z_]\w*", rest[k:])
        if not m:
            break
        label = m.group(0)
        k += m.end()
        while k < n and rest[k].isspace():
            k += 1
        if k >= n or rest[k] != ":":
            # A label after a completed parameter list cannot be a zero-argument selector: only an
            # attribute can sit there, and the SDK uses macros this list does not know
            # (`… NS_SWIFT_UNAVAILABLE_FROM_ASYNC("x")`), which otherwise became part of the
            # selector and made 54 real methods look absent from the runtime.
            if not labels:
                labels.append(label)      # a genuine zero-argument selector, e.g. `- (void)foo`
            break
        k += 1
        while k < n and rest[k].isspace():
            k += 1
        if k >= n or rest[k] != "(":
            break
        j2 = match_balanced(rest, k)
        ptype = rest[k + 1:j2 - 1].strip()
        k = j2
        while k < n and rest[k].isspace():
            k += 1
        m2 = re.match(r"[A-Za-z_]\w*", rest[k:])
        pname = m2.group(0) if m2 else ""
        if m2:
            k += m2.end()
        labels.append(label + ":")
        params.append(_classify_param(ptype, pname, _nullability(ptype)))
    selector = "".join(labels)
    if not selector:
        return None
    d = Decl(owner=owner, kind="method", is_class=is_class, selector=selector,
             ret_raw=ret_raw, params=params, nullable=_nullability(ret_raw),
             deprecated=bool(re.search(r"API_DEPRECATED|DEPRECATED_ATTRIBUTE", stmt)),
             unavailable=bool(re.search(r"NS_UNAVAILABLE", stmt)),
             availability=availability_of(stmt),
             raw=" ".join(stmt.split()), line=line, declared_in=declared_in,
             property=False, multiline=("\n" in stmt.strip()))
    return d


_PROP_RE = re.compile(r"@property\s*(?:\((.*?)\))?\s*(.*?);?\s*$", re.S)


def parse_property(stmt, owner, file, line, declared_in):
    head = strip_macros(stmt).strip()
    if not head.startswith("@property"):
        return [], None
    m = _PROP_RE.match(head)
    if not m:
        return [], None
    attrs = m.group(1) or ""
    decl = (m.group(2) or "").strip()
    # `getter = isBlendingEnabled` is as valid as `getter=isBlendingEnabled`; the SDK uses both and
    # the spaced form silently lost the getter while the setter was still generated.
    attr_list = [re.sub(r"\s*=\s*", "=", a.strip()) for a in attrs.split(",") if a.strip()]
    is_class_prop = "class" in attr_list
    readonly = "readonly" in attr_list
    nullable = _nullability(attrs + " " + decl)
    if decl.endswith(")"):
        return [], None                    # block property: hand-written
    m2 = re.match(r"^(.*?)([A-Za-z_]\w*)\s*$", decl)
    if not m2:
        return [], None
    type_raw = m2.group(1).strip()
    name = m2.group(2)
    getter = name
    setter = "set" + name[0].upper() + name[1:] + ":"
    for a in attr_list:
        if a.startswith("getter="):
            getter = a.split("=", 1)[1].strip()
        elif a.startswith("setter="):
            setter = a.split("=", 1)[1].strip()
    dep = bool(re.search(r"API_DEPRECATED|DEPRECATED_ATTRIBUTE", stmt))
    unavail = bool(re.search(r"NS_UNAVAILABLE", stmt))
    get = Decl(owner=owner, kind="property-get", is_class=is_class_prop,
               selector=getter, ret_raw=type_raw, params=[],
               nullable=nullable, deprecated=dep, unavailable=unavail,
               availability=availability_of(stmt),
               raw=" ".join(stmt.split()),
               line=line, declared_in=declared_in, property=name,
               multiline=("\n" in stmt.strip()))
    out = [get]
    if not readonly:
        prop = _classify_param(type_raw, name, nullable)
        out.append(Decl(owner=owner, kind="property-set", is_class=is_class_prop,
                        selector=setter, ret_raw="void", params=[prop],
                        nullable=nullable, deprecated=dep, unavailable=unavail,
                        availability=availability_of(stmt),
                        raw=" ".join(stmt.split()), line=line,
                        declared_in=declared_in, property=name,
                        multiline=("\n" in stmt.strip())))
    return out, name


# --------------------------------------------------------------- file / framework

_IFACE_RE = re.compile(r"@(interface|protocol|implementation)\s+([A-Za-z_]\w*)")


def header_end(s, start):
    """Index of the end of an @interface/@protocol header.

    Stops at the first newline (at angle-bracket depth 0) whose line does not end
    with ':' or ',', so a superclass/protocol list continued on the next line is
    still part of the header. This keeps ': NSObject' out of the statement stream
    (otherwise it swallows the first declaration of every class).
    """
    depth = 0
    i = start
    n = len(s)
    while i < n:
        c = s[i]
        if c == "<":
            depth += 1
        elif c == ">":
            if depth > 0:
                depth -= 1
        elif c == "{" and depth == 0:
            return i
        elif c == "\n" and depth == 0:
            line = s[start:i].strip()
            if not (line.endswith(":") or line.endswith(",")):
                return i
        i += 1
    return n


_HEAD_SUPER_RE = re.compile(r"[A-Za-z_]\w*")
_HEAD_CATEGORY_RE = re.compile(r"\(\s*([A-Za-z_]\w*)\s*\)")
_HEAD_PROTOCOLS_RE = re.compile(r"<\s*([A-Za-z_][\w\s,]*?)\s*>")


def parse_head(rest):
    """(superclass, category, protocols) for the text between a class name and its body.

        '<ObjectType> (NSArrayCreation)'       -> (None, 'NSArrayCreation', [])
        '<__covariant T> : NSObject <NSCopy>'  -> ('NSObject', None, ['NSCopy'])
        ' () <NSTextLayoutOrientationProvider>' -> (None, None, ['NSTextLayoutOrientationProvider'])

    The lightweight generic parameter list has to be peeled off before the superclass, the
    category and the protocol list are read: a category on a generic class is written
    '@interface NSArray<ObjectType> (NSArrayCreation)', and without peeling, the '<ObjectType>'
    would be mistaken for an adopted protocol and the category name would be lost entirely
    (which is how 217 shipped methods went missing).

    A leading '<...>' is treated as a parameter list only when the header continues with ':' or
    '(' — '@interface NSObject <NSObject>' is a protocol list, and eating it would lose the
    protocol. Angle brackets nest inside a parameter list, so the closer is found by depth.
    """
    t = rest
    i = 0
    while i < len(t) and t[i].isspace():
        i += 1
    if i < len(t) and t[i] == "<":
        k = match_balanced_angle(t, i)
        while k < len(t) and t[k].isspace():
            k += 1
        if k < len(t) and t[k] in ":(":
            i = k
    superclass = None
    if i < len(t) and t[i] == ":":
        i += 1
        while i < len(t) and t[i].isspace():
            i += 1
        m = _HEAD_SUPER_RE.match(t, i)
        if m:
            superclass = m.group(0)
            i = m.end()
    category = None
    while i < len(t) and t[i].isspace():
        i += 1
    m = _HEAD_CATEGORY_RE.match(t, i)          # '()' is a class extension: no category name
    if m:
        category = m.group(1)
        i = m.end()
    protocols = _HEAD_PROTOCOLS_RE.findall(t[i:])
    proto_list = [p.strip() for p in protocols[0].split(",")] if protocols else []
    return superclass, category, proto_list


def parse_text(text, file="<text>", keep_private=False):
    """All @interface/@protocol blocks in a header as Interface objects."""
    clean = strip_comments(text)
    out = []
    pos = 0
    n = len(clean)
    while pos < n:
        m = _IFACE_RE.search(clean, pos)
        if not m:
            break
        hend = header_end(clean, m.end())
        head = clean[m.end():hend]
        if head.rstrip().endswith(";"):
            pos = m.end()                    # forward declaration: @protocol X;
            continue
        end = clean.find("@end", hend)
        if end < 0:
            end = n
        body = clean[hend:end]
        kind = m.group(1)
        name = m.group(2)
        superclass, category, proto_list = parse_head(head)
        if kind == "implementation" and not keep_private:
            pos = end + 4
            continue
        iface = Interface(kind, name, superclass, category, proto_list, file,
                          _line_of(clean, m.start()), body)
        _parse_body(iface, clean, m.end(), file)
        out.append(iface)
        pos = end + 4
    return out


def _parse_body(iface, clean, body_start, file):
    for stmt in split_statements(iface.body):
        s = stmt.strip()
        if not s:
            continue
        s = re.sub(r"^@(optional|required|public|private|protected|package)\b", "",
                   s).strip()
        if not s:
            continue
        # A preprocessor line is not a declaration and has no `;`, so `#pragma mark -` merged with
        # the method after it and the pair was dropped. Strip directives before deciding.
        if "#" in s:
            s = "\n".join(ln for ln in s.splitlines() if not ln.lstrip().startswith("#")).strip()
        if not s:
            continue
        line = _line_of(clean, body_start + iface.body.find(stmt))
        # An instance-variable block starts with `{` and `split_statements` cannot end a statement
        # on it, so the block ran into the NEXT declaration and the combined text matched no branch:
        # NSDatePicker's datePickerStyle() and NSPrintOperation's +printOperationWithView:printInfo:
        # vanished with the class still reporting "0 skipped". Drop the block, keep the remainder.
        while s.startswith("{"):
            s = s[match_balanced(s, 0):].strip().lstrip(";").strip()
        if not s:
            continue
        if s.startswith("@property"):
            decls, _ = parse_property(s, iface.name, file, line, iface.name)
            iface.decls.extend(decls)
        elif s[0] in "+-":
            d = parse_method(s, iface.name, s[0] == "+", file, line, iface.name,
                             in_protocol=iface.kind == "protocol")
            if d:
                iface.decls.append(d)
        else:
            iface.unparsed.append(" ".join(s.split())[:120])


def parse_file(path):
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        return parse_text(fh.read(), path)


def parse_framework(sdk, framework, include_categories=True):
    """Every @interface/@protocol in a framework's public Headers/ directory.

    Categories are included by default: `@interface NSArray<ObjectType> (NSExtendedArray)` is
    part of NSArray's declaration surface, and dropping it silently is what made the generator
    see 3 of NSArray's 53 shipped methods. Pass include_categories=False for class bodies only.
    """
    import os
    root = os.path.join(sdk, "System", "Library", "Frameworks",
                        framework + ".framework", "Headers")
    if not os.path.isdir(root):
        return []
    ifaces = []
    for dirpath, _dirs, files in os.walk(root):
        for fn in sorted(files):
            if not fn.endswith(".h"):
                continue
            for iface in parse_file(os.path.join(dirpath, fn)):
                if iface.category and not include_categories:
                    continue
                ifaces.append(iface)
    return ifaces
