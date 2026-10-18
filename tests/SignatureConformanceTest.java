package nsui.tests;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import nsui.objc.ObjC;

/// SignatureConformanceTest — checks every declared Sig shape against the real
/// Objective-C runtime, so a wrong ABI class can never pass unnoticed again.
///
/// Why this exists: the toolkit's whole dispatch model rests on Sig.of(ret, args...)
/// matching the selector's true signature. The unit tests exercise behaviour, so a
/// wrong ABI class (float read as double, a by-value struct sent as a pointer) either
/// returns garbage that a loose assertion tolerates, or is never read back at all.
/// This test asks the runtime directly:
///
///   1. parse src/nsui/*.java and resolve each ObjC.sel("...") call site to the Sig it
///      declares (Handles record / local handle / typed msgSend helper);
///   2. read method_getTypeEncoding for that selector on the wrapper's ObjC class;
///   3. compare the ABI class sequences, tolerating only genuinely equivalent
///      encodings (SEL/Class == id, C array == pointer, 'B' == BOOL).
///      'c'/'C' are BYTE (numeric 1-byte reads, not boolean).
///      A VOID-declared return keeps ignoring the real return class (counted
///      and capped); every other class difference — including an unmodelled
///      real struct or NSEdgeInsets vs NSRect — is reported.
///
/// It is source-driven on purpose: the declared shape exists only in the source, and a
/// bare MethodHandle.invokeExact gives the runtime nothing to hook. Call sites it cannot
/// resolve (the generic escape hatch, non-wrapper receivers) are counted, not failed,
/// and a floor assertion guards against silently resolving almost nothing.
public final class SignatureConformanceTest {

    private static MethodHandle hGetClass, hSelName, hInstMethod, hClassMethod, hTypeEnc;

    private static final String QUAL = "rnNoOoRV";

    private static final Map<String, String> HELPERS = new HashMap<>();
    static {
        HELPERS.put("msgSendId", "ID");
        HELPERS.put("msgSendIdId", "ID,ID");
        HELPERS.put("msgSendIdIdSelId", "ID,ID,ID,ID");
        HELPERS.put("msgSendIdRectLongLongBool", "ID,RECT,INT,INT,BOOL");
        HELPERS.put("msgSendIdLongIdIdBool", "ID,INT,ID,ID,BOOL");
        HELPERS.put("msgSendIdDouble", "ID,DOUBLE");
        HELPERS.put("msgSendVoid", "VOID");
        HELPERS.put("msgSendVoidId", "VOID,ID");
        HELPERS.put("msgSendVoidLong", "VOID,INT");
        HELPERS.put("msgSendVoidBool", "VOID,BOOL");
        HELPERS.put("msgSendLong", "INT");
        HELPERS.put("msgSendBool", "BOOL");
        HELPERS.put("msgSendShort", "SHORT");
        HELPERS.put("msgSendRect", "RECT");
    }

    private static final class Stats {
        int checked, unresolved, noMatch, other, voidIgnored, widthMismatch;
    }

    // Caps measured on macOS 26.5.1 / GraalVM 25 (2026-10-07 runs): the test fails
    // if any of these counts grows, so new unmodelled helpers or ignored returns
    // cannot slip in silently. NO_MATCH/OTHER baselines come from the unhardened
    // run (192/1); the single OTHER then was the `[2f]` array now modelled below,
    // so the steady state is 0. VOID_IGNORED measured 0 on the hardened run
    // (no VOID-declared site ignores a real return), so the cap is 0.
    // NO_MATCH rose 192 -> 242 with the sampler/upload/blend wrappers: those
    // selectors are genuine Metal API (verified against MTL*.h), but the lookup
    // cannot see them — MTLDevice/MTLTexture/MTLRenderCommandEncoder are
    // protocols (no class object) and the descriptor/state classes resolve to
    // method-less cluster stubs while msgSend works. Behavior for all of them
    // is proven by the readback suites instead.
    // 241 -> 243: NSCollectionView.moveItemAtIndexPath:'s item-existence guard reads
    // `section` and `item` from each NSIndexPath peer (ObjC.sel("section")/("item")).
    // Those selectors live on NSIndexPath (an AppKit category), not on the enclosing
    // wrapper class, so the lookup cannot resolve them — the same category of no-match as
    // the Metal protocol selectors. Both encodings were measured directly:
    // NSIndexPath -section and -item are "q16@0:8" (NSInteger), which is what msgSendLong
    // sends, so the two unresolvable call sites are correct rather than unknown.
    /// Counted baseline of call sites whose selector has no statically visible runtime method —
    /// overwhelmingly Metal protocol/descriptor stubs, which the gate cannot resolve against the
    /// facade class even though the calls work at runtime. It is a RATCHET: a new entry must be
    /// named here. 244 = 243 + MTLTexture.mipmapLevelCount, added with the mip-level guard and
    /// verified live (4 for a mipmapped 8x8 texture, 1 for a plain one).
    /// Batch override: the selftest 29-class surface tree carries 172 more (pinned 416 in
    /// codegen/surface-baseline.json for the new protocol/cluster stubs). Pass
    /// -Dsignature.noMatchBaseline=416 to measure that tree; the default stays 244 for the repo.
    private static final int NO_MATCH_BASELINE = 244;
    private static int noMatchBaseline() {
        String v = System.getProperty("signature.noMatchBaseline");
        if (v != null) {
            try { return Integer.parseInt(v.trim()); } catch (NumberFormatException ignored) { /* default */ }
        }
        return NO_MATCH_BASELINE;
    }
    private static final int OTHER_CAP = 0;
    private static final int VOID_IGNORED_CAP = 0;
    /// Integer-width disagreements (declared 64-bit INT vs real 32-bit `i`/`I`, or
    /// the reverse). All eleven known sites were migrated to exact INT32 shapes, so
    /// this is now a hard ZERO: any new disagreement fails the gate. (Previously the
    /// gate mapped i/I/q/Q/l/L together and could not see width at all.)
    private static final int WIDTH_MISMATCH_BASELINE = 0;

    private static final class Resolved {
        String name;
        List<String> shape;
        final List<List<String>> all = new ArrayList<>();
    }

    private SignatureConformanceTest() {}

    public static void main(String[] args) throws Throwable {
        System.out.println("=== SignatureConformanceTest — declared Sig vs runtime type encoding ===");
        ObjC.init();
        for (String fw : new String[] {"QuartzCore", "Metal", "MetalPerformanceShaders", "WebKit",
                "UniformTypeIdentifiers", "CoreImage", "CoreText"}) {
            try { ObjC.ensureFramework(fw); } catch (Throwable ignored) { /* optional framework */ }
        }
        initRuntime();

        Path root = Path.of("src", "nsui");
        if (!Files.isDirectory(root)) TestKit.skip("src/nsui not found (run tests.sh from the repo root)");
        List<Path> files;
        try (Stream<Path> s = Files.list(root)) {
            files = s.filter(f -> f.getFileName().toString().endsWith(".java")).sorted().toList();
        }

        Stats st = new Stats();
        List<String> bad = new ArrayList<>();
        List<String> widthNotes = new ArrayList<>();
        for (Path f : files) scan(f, st, bad, widthNotes);

        System.out.println("call sites checked: " + st.checked
                + "  unresolved: " + st.unresolved
                + "  no runtime method: " + st.noMatch + " (baseline " + noMatchBaseline() + ")"
                + "  non-modelled struct in real sig: " + st.other + " (cap " + OTHER_CAP + ")"
                + "  void-return-ignored: " + st.voidIgnored + " (cap " + VOID_IGNORED_CAP + ")"
                + "  int-width disagreements: " + st.widthMismatch + " (baseline " + WIDTH_MISMATCH_BASELINE + ")");
        System.out.println("ABI mismatches: " + bad.size());
        for (String s : bad) System.out.println("  MISMATCH " + s);
        if (!widthNotes.isEmpty()) {
            System.out.println("Integer-width disagreements (" + widthNotes.size()
                    + ") — benign on little-endian, listed so they can be migrated:");
            for (String s : widthNotes) System.out.println("  WIDTH " + s);
        }

        TestKit.check(st.checked > 3000, "resolved a meaningful number of call sites (got " + st.checked + ")");
        double unresolvedShare = st.checked + st.unresolved == 0 ? 0
                : (double) st.unresolved / (st.checked + st.unresolved);
        TestKit.check(unresolvedShare < 0.15,
                "unresolved share below 15% (got " + String.format("%.2f", unresolvedShare * 100)
                + "%: " + st.unresolved + " of " + (st.checked + st.unresolved) + ")");
        TestKit.check(st.noMatch <= noMatchBaseline(),
                "no-runtime-method count within baseline (got " + st.noMatch
                + ", baseline " + noMatchBaseline() + ")");
        TestKit.check(st.other <= OTHER_CAP,
                "non-modelled struct count within cap (got " + st.other
                + ", cap " + OTHER_CAP + ")");
        TestKit.check(st.voidIgnored <= VOID_IGNORED_CAP,
                "void-ignored return count within cap (got " + st.voidIgnored
                + ", cap " + VOID_IGNORED_CAP + ")");
        TestKit.check(st.widthMismatch <= WIDTH_MISMATCH_BASELINE,
                "integer-width disagreements within baseline (got " + st.widthMismatch
                + ", baseline " + WIDTH_MISMATCH_BASELINE + ")");
        TestKit.check(bad.isEmpty(), "every declared Sig shape matches the runtime type encoding");
        TestKit.end();
    }

    // ------------------------------------------------------------------ runtime

    private static void initRuntime() throws Throwable {
        Linker linker = Linker.nativeLinker();
        Arena arena = Arena.global();
        ValueLayout ptr = (ValueLayout) linker.canonicalLayouts().get("void*");
        SymbolLookup objc = SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", arena);
        hGetClass = linker.downcallHandle(objc.find("objc_getClass").orElseThrow(),
                FunctionDescriptor.of(ptr, ptr));
        hSelName = linker.downcallHandle(objc.find("sel_registerName").orElseThrow(),
                FunctionDescriptor.of(ptr, ptr));
        hInstMethod = linker.downcallHandle(objc.find("class_getInstanceMethod").orElseThrow(),
                FunctionDescriptor.of(ptr, ptr, ptr));
        hClassMethod = linker.downcallHandle(objc.find("class_getClassMethod").orElseThrow(),
                FunctionDescriptor.of(ptr, ptr, ptr));
        hTypeEnc = linker.downcallHandle(objc.find("method_getTypeEncoding").orElseThrow(),
                FunctionDescriptor.of(ptr, ptr));
    }

    private static MemorySegment cstr(String s) {
        return Arena.global().allocateFrom(s);
    }

    /// Real type encoding for the selector on the class (instance first, then class), or null.
    private static String encoding(MemorySegment cls, String sel) throws Throwable {
        MemorySegment s = (MemorySegment) hSelName.invokeExact(cstr(sel));
        MemorySegment m = (MemorySegment) hInstMethod.invokeExact(cls, s);
        if (m.address() == 0) m = (MemorySegment) hClassMethod.invokeExact(cls, s);
        if (m.address() == 0) return null;
        MemorySegment e = (MemorySegment) hTypeEnc.invokeExact(m);
        if (e.address() == 0) return null;
        return e.reinterpret(1024).getString(0);
    }

    // -------------------------------------------------------------------- scan

    private static void scan(Path file, Stats st, List<String> bad, List<String> widthNotes) throws Throwable {
        String text = stripCommentLines(Files.readString(file));
        String stem = file.getFileName().toString().replace(".java", "");

        Map<String, List<String>> recComps = new LinkedHashMap<>();
        Matcher mr = Pattern.compile("record\\s+(\\w+)\\s*\\(([^)]*)\\)").matcher(text);
        while (mr.find()) {
            List<String> comps = new ArrayList<>();
            Matcher cm = Pattern.compile("MethodHandle\\s+(\\w+)").matcher(mr.group(2));
            while (cm.find()) comps.add(cm.group(1));
            recComps.put(mr.group(1), comps);
        }

        // Local handle declarations: `MethodHandle x = ObjC.handle(Sig.of(...));` followed
        // by `x.invokeExact(...)`. The inline pattern below does not match these, so such
        // call sites used to fall into `unresolved` and were invisible to the width check
        // — which is exactly where a 16-bit CGGlyph read as 64-bit hid.
        Map<String, List<String>> localHandles = new LinkedHashMap<>();
        Matcher lh = Pattern.compile(
                "MethodHandle\\s+(\\w+)\\s*=\\s*ObjC\\.handle\\(Sig\\.of\\(([^)]*)\\)\\)").matcher(text);
        while (lh.find()) localHandles.put(lh.group(1), shapeOf(lh.group(2)));

        List<String> defName = new ArrayList<>();
        List<String> defShape = new ArrayList<>();
        List<Integer> defOffset = new ArrayList<>();
        Map<String, List<String>> allByName = new LinkedHashMap<>();

        Matcher mh = Pattern.compile("new\\s+(\\w+)\\s*\\((.*?)\\)\\s*;", Pattern.DOTALL).matcher(text);
        while (mh.find()) {
            List<String> comps = recComps.get(mh.group(1));
            if (comps == null) continue;
            List<String> args = splitTop(mh.group(2));
            for (int i = 0; i < comps.size() && i < args.size(); i++) {
                String sh = sigOfInner(args.get(i));
                if (sh == null) continue;
                defName.add(comps.get(i));
                defShape.add(sh);
                defOffset.add(mh.start());
                allByName.computeIfAbsent(comps.get(i), k -> new ArrayList<>()).add(sh);
            }
        }
        Matcher mv = Pattern.compile(
                "(?:MethodHandle\\s+)?(\\w+)\\s*=\\s*ObjC\\.handle\\(Sig\\.of\\((.*?)\\)\\)",
                Pattern.DOTALL).matcher(text);
        while (mv.find()) {
            defName.add(mv.group(1));
            defShape.add(mv.group(2));
            defOffset.add(mv.start());
            allByName.computeIfAbsent(mv.group(1), k -> new ArrayList<>()).add(mv.group(2));
        }

        List<String> cands = new ArrayList<>();
        cands.add(stem);
        Matcher mc = Pattern.compile("ObjC\\.cls\\(\"([^\"]+)\"\\)").matcher(text);
        while (mc.find()) if (!cands.contains(mc.group(1))) cands.add(mc.group(1));

        // Tier-2 selector hoisting replaces ObjC.sel("x") with Sels.x; the field's
        // selector literal is recorded by the generated populate() assignments.
        Map<String, String> selsMap = new HashMap<>();
        Matcher msel = Pattern.compile("([A-Za-z_]\\w*)\\s*=\\s*ObjC\\.sel\\(\\s*\"([^\"]+)\"\\s*\\)").matcher(text);
        while (msel.find()) selsMap.put(msel.group(1), msel.group(2));

        // A call site is either a literal ObjC.sel("...") or a hoisted Sels.<field>.
        // Only the FIRST selector token in a statement is the dispatched selector;
        // later ones are SEL arguments (e.g. the tested SEL in a respondsToSelector: guard).
        Matcher ms = Pattern.compile(
                "ObjC\\.sel\\(\\s*\"([^\"]+)\"\\s*\\)|Sels\\.([A-Za-z_]\\w*)").matcher(text);
        while (ms.find()) {
            int start = ms.start();
            int cut = Math.max(Math.max(text.lastIndexOf(';', start), text.lastIndexOf('{', start)),
                    text.lastIndexOf('}', start));
            Matcher probe = ms.pattern().matcher(text);
            if (probe.find(cut + 1) && probe.start() < start) continue;
            String stmt = text.substring(cut + 1, start);
            // The generated populate() assignment (field = ObjC.sel("...")) is not a call site.
            if (stmt.matches("(?s)\\s*[A-Za-z_]\\w*\\s*=\\s*")) continue;
            String sel;
            if (ms.group(1) != null) {
                sel = ms.group(1);
            } else {
                sel = selsMap.get(ms.group(2));
                if (sel == null) { st.unresolved++; continue; }
            }

            Resolved res = resolve(stmt, start, defName, defShape, defOffset, allByName);
            List<String> assumed = res == null ? null : res.shape;
            if (assumed == null) {
                String helperName = null;
                Matcher hm = Pattern.compile("ObjC\\.(msgSend\\w+)\\(").matcher(stmt);
                while (hm.find()) helperName = hm.group(1);
                if (helperName != null && HELPERS.containsKey(helperName)) {
                    assumed = splitTop(HELPERS.get(helperName));
                }
            }
            if (assumed == null) {
                Matcher im = Pattern.compile("ObjC\\.handle\\(Sig\\.of\\((.*?)\\)\\)\\s*\\.invokeExact",
                        Pattern.DOTALL).matcher(stmt);
                if (im.find()) assumed = shapeOf(im.group(1));
            }
            if (assumed == null) {
                // A local handle variable: take the LAST `<name>.invokeExact` in the statement.
                Matcher lm = Pattern.compile("(\\w+)\\.invokeExact").matcher(stmt);
                String localName = null;
                while (lm.find()) localName = lm.group(1);
                if (localName != null) assumed = localHandles.get(localName);
            }
            if (assumed == null || assumed.isEmpty()) { st.unresolved++; continue; }

            String clsName = null;
            String enc = null;
            for (String cand : cands) {
                MemorySegment c = (MemorySegment) hGetClass.invokeExact(cstr(cand));
                if (c.address() == 0) continue;
                String e = encoding(c, sel);
                if (e != null) { clsName = cand; enc = e; break; }
            }
            if (enc == null) { st.noMatch++; continue; }
            st.checked++;

            List<String> real = abiOf(enc);
            if (real.contains("OTHER")) st.other++;
            if ("VOID".equals(assumed.get(0)) && !"VOID".equals(real.get(0))) st.voidIgnored++;
            if (widthMismatch(assumed, real)) {
                st.widthMismatch++;
                widthNotes.add(file.getFileName() + "  " + clsName + "  " + sel
                        + "  declared[" + String.join(",", assumed) + "]"
                        + "  real[" + String.join(",", real) + "]  (" + enc + ")");
            }
            if (matches(assumed, real)) continue;

            boolean tolerated = false;
            if (res != null) {
                for (List<String> alt : res.all) if (matches(alt, real)) { tolerated = true; break; }
            }
            if (!tolerated) {
                bad.add(file.getFileName() + "  " + clsName + "  " + sel
                        + "  assumed[" + String.join(",", assumed) + "]"
                        + "  real[" + String.join(",", real) + "]  (" + enc + ")");
            }
        }
    }

    private static Resolved resolve(String stmt, int start,
            List<String> defName, List<String> defShape, List<Integer> defOffset,
            Map<String, List<String>> allByName) {
        Matcher m = Pattern.compile("([A-Za-z_][\\w.]*(?:\\(\\))?)\\.invokeExact\\(").matcher(stmt);
        String recv = null;
        while (m.find()) recv = m.group(1);
        if (recv == null) return null;
        String comp = recv.substring(recv.lastIndexOf('.') + 1);
        if (comp.endsWith("()")) comp = comp.substring(0, comp.length() - 2);
        String shape = null;
        int best = -1;
        for (int i = 0; i < defName.size(); i++) {
            if (defName.get(i).equals(comp) && defOffset.get(i) < start && defOffset.get(i) > best) {
                best = defOffset.get(i);
                shape = defShape.get(i);
            }
        }
        if (shape == null) return null;
        Resolved r = new Resolved();
        r.name = comp;
        r.shape = shapeOf(shape);
        List<String> raw = allByName.get(comp);
        if (raw != null) for (String s : raw) r.all.add(shapeOf(s));
        return r;
    }

    // ----------------------------------------------------------- type encoding

    private static List<String> abiOf(String enc) {
        List<String> toks = parseTypes(enc);
        List<String> out = new ArrayList<>();
        if (toks.size() < 3) { out.add("OTHER"); return out; }
        out.add(abi(toks.get(0)));
        for (int i = 3; i < toks.size(); i++) out.add(abi(toks.get(i)));
        return out;
    }

    private static List<String> parseTypes(String e) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < e.length()) {
            int[] next = new int[1];
            String t = parseOne(e, i, next);
            if (t == null || next[0] <= i) break;
            out.add(t);
            i = next[0];
        }
        return out;
    }

    private static String parseOne(String e, int i, int[] next) {
        while (i < e.length() && (QUAL.indexOf(e.charAt(i)) >= 0
                || Character.isDigit(e.charAt(i)) || e.charAt(i) == '+' || e.charAt(i) == '-')) i++;
        if (i >= e.length()) { next[0] = i; return null; }
        char c = e.charAt(i);
        if (c == '^') { int[] n2 = new int[1]; parseOne(e, i + 1, n2); next[0] = n2[0]; return "ID"; }
        if (c == '*' || c == '#' || c == ':') { next[0] = i + 1; return "ID"; }
        if (c == '{' || c == '(' || c == '[') {
            char close = c == '{' ? '}' : c == '(' ? ')' : ']';
            int depth = 0, j = i;
            while (j < e.length()) {
                char d = e.charAt(j);
                if (d == c) depth++;
                else if (d == close) { depth--; if (depth == 0) break; }
                j++;
            }
            next[0] = j + 1;
            return e.substring(i, j + 1);
        }
        if (c == '@') {
            if (i + 1 < e.length() && e.charAt(i + 1) == '?') { next[0] = i + 2; return "BLOCK"; }
            if (i + 1 < e.length() && e.charAt(i + 1) == '"') {
                int j = e.indexOf('"', i + 2);
                next[0] = j + 1;
                return "ID";
            }
            next[0] = i + 1;
            return "ID";
        }
        next[0] = i + 1;
        return String.valueOf(c);
    }

    private static String abi(String t) {
        switch (t) {
            case "ID": case "BLOCK": return "ID";
            case "v": return "VOID";
            case "d": return "DOUBLE";
            case "f": return "FLOAT";
            // 1-byte encodings split by signedness source: 'B' (C++ bool /
            // _Bool) is BOOL; 'c' (signed char) and 'C' (unsigned char) are
            // BYTE, read numerically (see NSNumber charValue/unsignedCharValue).
            // 's' (signed short) and 'S' (unsigned short) are SHORT, read
            // numerically (see NSNumber shortValue/unsignedShortValue).
            // Wider integers (i/I/q/Q/l/L) normalise to INT and never
            // match BOOL, BYTE, or SHORT in either direction.
            // Wider integers: 32-bit `i`/`I` are INT32 (exact JAVA_INT
            // descriptors); 64-bit `q`/`Q`/`l`/`L` are INT. The split makes width
            // disagreements visible to WIDTH_MISMATCH_BASELINE instead of being
            // invisible to this gate. None of them match BOOL, BYTE, or SHORT.
            case "B": return "BOOL";
            case "c": case "C": return "BYTE";
            case "s": case "S": return "SHORT";
            case "i": case "I": return "INT32";
            case "q": case "Q": case "l": case "L": return "INT";
            default:
                // C array in argument position (observed: `[2f]` in
                // `v32@0:8Q16[2f]24` = CAMediaTimingFunction
                // getControlPointAtIndex:values:): array parameters adjust to
                // pointers, so the declared ID (pointer) is exact.
                if (t.startsWith("[")) return "ID";
                return t.startsWith("{") ? structKind(t) : "OTHER";
        }
    }

    private static String structKind(String t) {
        if (t.startsWith("{CGRect") || t.startsWith("{_NSRect") || t.startsWith("{NSRect")) return "RECT";
        if (t.startsWith("{CGPoint") || t.startsWith("{_NSPoint") || t.startsWith("{NSPoint")) return "POINT";
        if (t.startsWith("{CGSize") || t.startsWith("{_NSSize") || t.startsWith("{NSSize")) return "SIZE";
        if (t.startsWith("{_NSRange") || t.startsWith("{NSRange")) return "RANGE";
        if (t.startsWith("{_MTLRegion") || t.startsWith("{MTLRegion")) return "REGION";
        if (t.startsWith("{CATransform3D")) return "TRANSFORM3D";
        if (t.startsWith("{NSEdgeInsets") || t.startsWith("{_NSEdgeInsets")
                || t.startsWith("{NSDirectionalEdgeInsets")) return "EDGEINSETS";
        return "OTHER";
    }

    private static boolean matches(List<String> assumed, List<String> real) {
        if (assumed.size() != real.size()) return false;
        if (!retCompat(assumed.get(0), real.get(0))) return false;
        for (int i = 1; i < assumed.size(); i++) {
            if (!argCompat(assumed.get(i), real.get(i))) return false;
        }
        return true;
    }

    private static boolean retCompat(String a, String r) {
        if ("VOID".equals(a)) return true;   // return value intentionally ignored (counted/capped at the call site)
        // No OTHER tolerance: an unmodelled real struct in return position is a mismatch.
        return argCompat(a, r);
    }

    private static boolean argCompat(String a, String r) {
        // No OTHER tolerance: an unmodelled real struct in argument position is a mismatch.
        // No BOOL<->INT interchange and no RECT<->EDGEINSETS alias either: an
        // i/I/q/Q/l/L where BOOL is declared (or vice versa), and NSEdgeInsets
        // vs NSRect confusion, are now mismatches.
        if (a.equals(r)) return true;
        // Integer width is tracked SEPARATELY from ABI compatibility. Any INT/INT32
        // pairing is accepted here so this check keeps its boxed-vs-struct meaning;
        // the width disagreement itself is counted per call site against
        // WIDTH_MISMATCH_BASELINE (see widthMismatch), so the class stays visible
        // and cannot silently grow.
        if (a.equals("INT32") && r.equals("INT")) return true;
        if (a.equals("INT") && r.equals("INT32")) return true;
        return false;
    }

    /// True when any integer position disagrees on width: the Java call shape uses a
    /// 64-bit INT descriptor where the runtime declares the 32-bit `i`/`I`, or vice
    /// versa. Benign on little-endian (the callee reads the low 32 bits), but it is
    /// exactly the class the width migration exists to close.
    private static boolean widthMismatch(List<String> assumed, List<String> real) {
        if (assumed.size() != real.size()) return false;
        for (int i = 0; i < assumed.size(); i++) {
            String a = assumed.get(i), r = real.get(i);
            if ((a.equals("INT") && r.equals("INT32")) || (a.equals("INT32") && r.equals("INT"))) return true;
        }
        return false;
    }

    // -------------------------------------------------------------- source text

    private static List<String> shapeOf(String inner) {
        List<String> out = new ArrayList<>();
        for (String t : splitTop(inner)) out.add(normalize(t));
        return out;
    }

    private static String normalize(String t) {
        String s = t.trim()
                .replace("nsui.objc.Sig.", "")
                .replace("Sig.", "")
                .replace("Ret.", "")
                .replace("Arg.", "");
        while (s.endsWith(")")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static String sigOfInner(String arg) {
        Matcher m = Pattern.compile("Sig\\.of\\((.*)\\)", Pattern.DOTALL).matcher(arg);
        return m.find() ? m.group(1) : null;
    }

    private static List<String> splitTop(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '<') depth++;
            else if (c == ')' || c == '>') depth--;
            if (c == ',' && depth == 0) { parts.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        if (cur.toString().trim().length() > 0) parts.add(cur.toString());
        List<String> out = new ArrayList<>();
        for (String p : parts) out.add(p.trim());
        return out;
    }

    /// Drop whole-line comments (/// javadoc and // notes) so selector literals in
    /// prose are not mistaken for call sites. Inline string literals are left intact.
    private static String stripCommentLines(String s) {
        StringBuilder b = new StringBuilder();
        for (String line : s.split("\n", -1)) {
            String t = line.stripLeading();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) { b.append('\n'); continue; }
            b.append(line).append('\n');
        }
        return b.toString();
    }
}
