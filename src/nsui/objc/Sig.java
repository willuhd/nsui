package nsui.objc;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.util.List;

/// The signature-keyed message vocabulary — the single source of truth for every
/// `objc_msgSend` call shape the toolkit can make.
///
/// Two consumers, one list:
/// - at RUNTIME, `ObjC.init()` builds one downcall handle per entry;
/// - at BUILD time, `NsuiFeature` registers one descriptor per entry with
///   the native-image builder. Registration set == vocabulary set, always — the
/// invariant that keeps the default build free of tracing agents and JSON
/// metadata (no reflection, no drift).
///
/// Descriptors are keyed by *signature, not selector*: every AppKit method
/// with the same shape (return class + argument classes) shares one descriptor and
/// one native stub. Measured against the macOS 15 SDK headers, AppKit's ~4,500
/// methods collapse to 438 distinct shapes and the top ~40 cover ~78% of all calls;
/// the entries below are the curated core and grow one line at a time.
///
/// ABI notes:
/// - `id`/`SEL`/Class/pointers are one argument class (`ID`)
///   — integer-class registers on both x86_64 and arm64.
/// - On x86_64, 32-byte struct returns (`RECT`) go through
///   `objc_msgSend_stret`; arm64 has a single `objc_msgSend` for
/// everything (`msgSendSymbol`).
/// - FFM gives downcalls with group-layout returns an implicit leading
///   `SegmentAllocator` parameter; the handle types in `ObjC`
/// reflect that.
public final class Sig {

    /// Argument classes. `ID` covers id/SEL/Class/pointers — one ABI class.
    public enum Arg { ID, INT, BOOL, DOUBLE, RECT, POINT, SIZE, FLOAT, RANGE }

    /// Return classes. `RECT` is a 32-byte struct (stret on x86_64); POINT/SIZE/RANGE are 16-byte structs.
    public enum Ret { VOID, ID, INT, BOOL, DOUBLE, RECT, POINT, SIZE, FLOAT, RANGE }

    /// A message signature: return class plus argument classes, packed into a
    /// 4-bits-per-arg long key so the record's value-based `equals`/`hashCode`
    /// are exact and cheap.
    public record S(Ret ret, long key, int argc) {

        public S {
            if (argc < 0 || argc > 10) throw new IllegalArgumentException("argc=" + argc);
        }

        /// Human-readable shape, e.g. `"void(id,int)"` — used in error messages.
        public String shape() {
            StringBuilder b = new StringBuilder(ret.name().toLowerCase());
            b.append('(');
            for (int i = 0; i < argc; i++) {
                if (i > 0) b.append(',');
                b.append(Arg.values()[(int) (key >>> (i * 4)) & 0xF].name().toLowerCase());
            }
            return b.append(')').toString();
        }

        /// The FFM descriptor for this signature (plain data — safe at build time and run time).
        public FunctionDescriptor descriptor() { return Sig.descriptor(this); }
    }

    private Sig() {}

    /// Build a signature from its return class and argument classes.
    public static S of(Ret ret, Arg... args) {
        long key = 0;
        for (int i = 0; i < args.length; i++) key |= ((long) args[i].ordinal()) << (i * 4);
        return new S(ret, key, args.length);
    }

    // ---- canonical layouts (resolved at class-load; identical in the image builder) ----

    private static final ValueLayout PTR    = (ValueLayout) Linker.nativeLinker().canonicalLayouts().get("void*");
    private static final ValueLayout LONG   = (ValueLayout) Linker.nativeLinker().canonicalLayouts().get("long");
    private static final ValueLayout DOUBLE = (ValueLayout) Linker.nativeLinker().canonicalLayouts().get("double");
    private static final ValueLayout BOOL   = (ValueLayout) Linker.nativeLinker().canonicalLayouts().get("bool");
    private static final ValueLayout FLOAT  = ValueLayout.JAVA_FLOAT;
    private static final MemoryLayout NS_RECT  = MemoryLayout.structLayout(DOUBLE, DOUBLE, DOUBLE, DOUBLE);
    private static final MemoryLayout NS_POINT = MemoryLayout.structLayout(DOUBLE, DOUBLE);
    private static final MemoryLayout NS_SIZE  = MemoryLayout.structLayout(DOUBLE, DOUBLE);
    private static final MemoryLayout NS_RANGE = MemoryLayout.structLayout(LONG, LONG);

    private static FunctionDescriptor descriptor(S s) {
        // objc_msgSend's real C signature is (id, SEL, ...) — the receiver and
        // selector are explicit pointer arguments of every message descriptor.
        MemoryLayout[] args = new MemoryLayout[s.argc() + 2];
        args[0] = PTR; // id (receiver)
        args[1] = PTR; // SEL (_cmd)
        for (int i = 0; i < s.argc(); i++) {
            args[i + 2] = switch (Arg.values()[(int) (s.key() >>> (i * 4)) & 0xF]) {
                case ID -> PTR;
                case INT -> LONG;
                case BOOL -> BOOL;
                case DOUBLE -> DOUBLE;
                case RECT -> NS_RECT;
                case POINT -> NS_POINT;
                case SIZE -> NS_SIZE;
                case FLOAT -> FLOAT;
                case RANGE -> NS_RANGE;
            };
        }
        return switch (s.ret()) {
            case VOID -> FunctionDescriptor.ofVoid(args);
            case ID -> FunctionDescriptor.of(PTR, args);
            case INT -> FunctionDescriptor.of(LONG, args);
            case BOOL -> FunctionDescriptor.of(BOOL, args);
            case DOUBLE -> FunctionDescriptor.of(DOUBLE, args);
            case RECT -> FunctionDescriptor.of(NS_RECT, args);
            case POINT -> FunctionDescriptor.of(NS_POINT, args);
            case SIZE -> FunctionDescriptor.of(NS_SIZE, args);
            case FLOAT -> FunctionDescriptor.of(FLOAT, args);
            case RANGE -> FunctionDescriptor.of(NS_RANGE, args);
        };
    }

    /// The message-send symbol for a return class: x86_64 needs `objc_msgSend_stret`
    /// for 32-byte struct returns; arm64 has a single `objc_msgSend` for everything.
    public static String msgSendSymbol(Ret ret) {
        if (System.getProperty("os.arch").equals("aarch64")) return "objc_msgSend";
        return ret == Ret.RECT ? "objc_msgSend_stret" : "objc_msgSend";
    }

    // ---- the vocabulary: every message shape the toolkit may send. ----

    public static final List<S> VOCABULARY = List.of(
        // (id, SEL) -> T
        of(Ret.VOID), of(Ret.ID), of(Ret.BOOL), of(Ret.INT), of(Ret.DOUBLE), of(Ret.RECT), of(Ret.POINT), of(Ret.SIZE),
        // (id, SEL, id) -> T
        of(Ret.VOID, Arg.ID), of(Ret.ID, Arg.ID), of(Ret.BOOL, Arg.ID), of(Ret.INT, Arg.ID),
        // (id, SEL, id, id) -> T
        of(Ret.VOID, Arg.ID, Arg.ID), of(Ret.ID, Arg.ID, Arg.ID), of(Ret.BOOL, Arg.ID, Arg.ID),
        // (id, SEL, id, id, id) -> T
        of(Ret.ID, Arg.ID, Arg.ID, Arg.ID),
        // scalars and mixed
        of(Ret.ID, Arg.INT), of(Ret.ID, Arg.DOUBLE), of(Ret.VOID, Arg.INT), of(Ret.VOID, Arg.BOOL),
        of(Ret.VOID, Arg.DOUBLE),                       // setSpacing: / setDoubleValue: / setWidth:
        of(Ret.VOID, Arg.ID, Arg.INT), of(Ret.VOID, Arg.ID, Arg.BOOL),
        of(Ret.VOID, Arg.INT, Arg.ID),                  // setLabel:forSegment: / setGravity:forArrangedSubviews:
        of(Ret.BOOL, Arg.INT), of(Ret.VOID, Arg.BOOL, Arg.INT),  // isEnabledForSegment: / setEnabled:forSegment:
        of(Ret.DOUBLE, Arg.INT), of(Ret.VOID, Arg.DOUBLE, Arg.INT), // widthForSegment: / setWidth:forSegment:
        of(Ret.ID, Arg.ID, Arg.DOUBLE),                 // fontWithName:size:
        of(Ret.ID, Arg.DOUBLE, Arg.DOUBLE, Arg.DOUBLE, Arg.DOUBLE),  // colorWithSRGBRed:green:blue:alpha:
        of(Ret.ID, Arg.INT, Arg.BOOL),                  // standardWindowButton:forFlag:
        of(Ret.ID, Arg.ID, Arg.ID, Arg.INT),            // dictionaryWithObjects:forKeys:count:
        of(Ret.VOID, Arg.POINT),                        // setFrameOrigin:
        of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID),           // 3-object void (e.g. alerts with aux buttons)
        // structs by value
        of(Ret.ID, Arg.RECT, Arg.INT, Arg.INT, Arg.BOOL),   // initWithContentRect:styleMask:backing:defer:
        of(Ret.ID, Arg.INT, Arg.ID, Arg.ID, Arg.BOOL),      // nextEventMatchingMask:untilDate:inMode:dequeue:
        of(Ret.ID, Arg.RECT),                               // initWithFrame: / bitmapImageRepForCachingDisplayInRect:
        of(Ret.RECT, Arg.RECT),                             // convertRectToBacking:
        of(Ret.VOID, Arg.RECT),                             // setFrame: / setNeedsDisplayInRect:
        of(Ret.VOID, Arg.RECT, Arg.BOOL),                   // setFrame:display:
        of(Ret.VOID, Arg.RECT, Arg.ID),                     // cacheDisplayInRect:toBitmapImageRep:
        of(Ret.VOID, Arg.RECT, Arg.ID, Arg.INT),            // showRelativeToRect:ofView:preferredEdge:
        of(Ret.VOID, Arg.SIZE),                             // setContentSize:
        // widget-completeness additions
        of(Ret.ID, Arg.DOUBLE, Arg.DOUBLE),                 // systemFontOfSize:weight:
        of(Ret.ID, Arg.DOUBLE, Arg.ID),                    // blendedColorWithFraction:ofColor:
        of(Ret.VOID, Arg.INT, Arg.INT, Arg.ID, Arg.BOOL),  // editColumn:row:withEvent:select:
        // generic escape hatch: any selector whose args are all objects (NULL-padded)
        of(Ret.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID),
        of(Ret.VOID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID, Arg.ID),
        // split view / divider handling — deduplicated: VOID,DOUBLE,INT already covers setPosition:ofDividerAtIndex: (same as setWidth:forSegment:)
        // widget completeness additions
        of(Ret.INT, Arg.INT),                           // sendActionOn: (int -> int)
        of(Ret.SIZE, Arg.SIZE),                          // sizeThatFits: (size -> size)
        of(Ret.VOID, Arg.DOUBLE, Arg.DOUBLE),           // setDoubleValue: / generic double,double
        of(Ret.VOID, Arg.FLOAT, Arg.FLOAT),             // setPeriodicDelay:interval: (float, float) -> void
        of(Ret.BOOL, Arg.ID, Arg.POINT, Arg.ID),        // popUpMenuPositioningItem:atLocation:inView: (id, point, id) -> bool
        of(Ret.VOID, Arg.INT, Arg.INT),                 // setTag:forSegment: (long, long) -> void
        of(Ret.SIZE, Arg.ID, Arg.SIZE),                  // windowWillResize:toSize: (id, size) -> size
        // Auto Layout
        of(Ret.ID, Arg.ID, Arg.INT, Arg.INT, Arg.ID, Arg.INT, Arg.DOUBLE, Arg.DOUBLE), // constraintWithItem:attribute:relatedBy:toItem:attribute:multiplier:constant:
        of(Ret.ID, Arg.ID, Arg.DOUBLE, Arg.DOUBLE),      // constraintEqualToAnchor:multiplier:constant: / constraintEqualToAnchor:multiplier: + dimension anchor alternative (deduplicated)
        of(Ret.VOID, Arg.ID, Arg.DOUBLE),                 // anchor constraint with double constant helper
        // NSMenu insertItemWithTitle:action:keyEquivalent:atIndex: + NSAlert / panel additions (deduplicated: ID,ID,ID,INT already present as dictionary helper)
        of(Ret.ID, Arg.ID, Arg.ID, Arg.ID, Arg.INT),
        // AppKit long-tail: toolbar / collection / outline / path / gestures
        of(Ret.POINT, Arg.ID),                              // locationInView: / translationInView: (id) -> point
        of(Ret.VOID, Arg.POINT, Arg.ID),                    // setTranslation:inView: (point, id) -> void
        of(Ret.VOID, Arg.ID, Arg.INT, Arg.BOOL),           // toolbar/item fallback / expandItem:expandChildren: alt
        of(Ret.BOOL, Arg.ID, Arg.INT),                      // outline isGroupItem / collection helper (id, int) -> bool
        of(Ret.ID, Arg.INT, Arg.ID),                        // outline child:ofItem: (long, id) -> id
        // NSRange by value (16-byte struct of 2 longs)
        of(Ret.RANGE),                                      // rangeValue (range getter)
        of(Ret.RANGE, Arg.RANGE),                           // range manipulation returning range
        of(Ret.VOID, Arg.RANGE),                            // setRange: / setSelectedRange: (range)
        of(Ret.ID, Arg.RANGE),                              // substringWithRange: / attributedSubstringFromRange:
        of(Ret.ID, Arg.ID, Arg.RANGE),                      // e.g., string:range type helpers (id, range) -> id
        of(Ret.VOID, Arg.ID, Arg.RANGE),                    // e.g., setTextWithRange: / scrollRangeToVisible:
        of(Ret.BOOL, Arg.RANGE),                            // contains / isEqual with range
        of(Ret.INT, Arg.RANGE),                             // length/count helpers with range
        of(Ret.RANGE, Arg.ID),                              // rangeOfString: (id) -> range
        of(Ret.RANGE, Arg.ID, Arg.INT),                     // rangeOfString:options: (id,int)->range
        of(Ret.ID, Arg.RANGE, Arg.ID),                      // replaceCharactersInRange:withString: (range,id)->void/id variant
        of(Ret.VOID, Arg.RANGE, Arg.ID),                     // replaceCharactersInRange:withString: void variant
        // attributed string specifics
        of(Ret.ID, Arg.ID, Arg.INT, Arg.ID),                // attribute:atIndex:effectiveRange: (id, long, id*) -> id
        of(Ret.VOID, Arg.ID, Arg.ID, Arg.RANGE),             // addAttribute:value:range: (id, id, NSRange) -> void
        // CALayer opacity (float)
        of(Ret.FLOAT), of(Ret.VOID, Arg.FLOAT),
        // --- nsui3 Window/App/Graphics extensions (append-only) ---
        of(Ret.VOID, Arg.RECT, Arg.DOUBLE),                 // NSGradient drawInRect:angle:
        of(Ret.ID, Arg.ID, Arg.BOOL),                       // NSGraphicsContext graphicsContextWithCGContext:flipped:
        of(Ret.ID, Arg.RECT, Arg.INT, Arg.ID, Arg.ID),      // NSTrackingArea initWithRect:options:owner:userInfo:
        of(Ret.VOID, Arg.POINT, Arg.POINT, Arg.POINT),      // NSBezierPath curveToPoint:controlPoint1:controlPoint2:
        of(Ret.ID, Arg.POINT),                              // NSBezierPath bezierPath helpers / NSCursor
        of(Ret.VOID, Arg.ID, Arg.RECT),                     // NSWorkspace iconForFileType etc alt
        of(Ret.BOOL, Arg.ID, Arg.BOOL),                     // NSWorkspace openURL with flag variant
        of(Ret.ID, Arg.RECT, Arg.INT),                      // NSGridView helper (rect,int)->id
        of(Ret.VOID, Arg.SIZE, Arg.BOOL)                    // NSAnimationContext / shadow helper
    );
}
