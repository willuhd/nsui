package nsui.objc;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.util.List;

/**
 * Single source of truth for the C-function descriptors the app uses — everything
 * that is NOT an objc_msgSend call.
 *
 * <p>The msgSend surface lives in {@link Sig} (the signature-keyed vocabulary);
 * this class covers the rest: the libobjc runtime API, dlopen, and the
 * CoreGraphics/CoreFoundation calls used for window-server verification.
 *
 * <p>Three consumers:
 * <ul>
 *   <li>at RUNTIME, {@code ObjC.init()} and {@code WindowCheck.init()} build the
 *       downcall handles from these descriptors;</li>
 *   <li>at BUILD time, {@code NsuiFeature} registers the exact same descriptors
 *       with the native-image builder via the {@link #RUNTIME}/{@link #CORE}
 *       lists — no tracing agent required, and the registered set can never
 *       drift from the one used at run time.</li>
 * </ul>
 */
public final class NsuiForeign {

    private NsuiForeign() {}

    private static Linker L = Linker.nativeLinker();
    private static ValueLayout PTR = (ValueLayout) L.canonicalLayouts().get("void*");
    private static ValueLayout LONG = (ValueLayout) L.canonicalLayouts().get("long");
    private static ValueLayout DOUBLE = (ValueLayout) L.canonicalLayouts().get("double");
    private static ValueLayout BOOL = (ValueLayout) L.canonicalLayouts().get("bool");
    private static ValueLayout INT = (ValueLayout) L.canonicalLayouts().get("int");
    /** NSRect/CGRect == struct { CGFloat x, y, width, height } (4 doubles). */
    private static final MemoryLayout NS_RECT = MemoryLayout.structLayout(DOUBLE, DOUBLE, DOUBLE, DOUBLE);
    /** NSPoint/CGPoint == struct { CGFloat x, y } (2 doubles). */
    private static final MemoryLayout NS_POINT = MemoryLayout.structLayout(DOUBLE, DOUBLE);
    /** NSSize/CGSize == struct { CGFloat width, height } (2 doubles). */
    private static final MemoryLayout NS_SIZE = MemoryLayout.structLayout(DOUBLE, DOUBLE);

    // ------------------------------------------------------- Objective-C runtime

    /** dlopen(const char*, int) -> void* */
    public static FunctionDescriptor dlopen() { return FunctionDescriptor.of(PTR, PTR, INT); }
    /** objc_getClass(const char*) -> id */
    public static FunctionDescriptor objcGetClass() { return FunctionDescriptor.of(PTR, PTR); }
    /** sel_registerName(const char*) -> SEL */
    public static FunctionDescriptor selRegisterName() { return FunctionDescriptor.of(PTR, PTR); }
    /** objc_allocateClassPair(Class, char*, size_t) -> Class */
    public static FunctionDescriptor allocateClassPair() { return FunctionDescriptor.of(PTR, PTR, PTR, LONG); }
    /** objc_registerClassPair(Class) -> void */
    public static FunctionDescriptor registerClassPair() { return FunctionDescriptor.ofVoid(PTR); }
    /** class_addMethod(Class, SEL, IMP, char*) -> bool */
    public static FunctionDescriptor addMethod() { return FunctionDescriptor.of(BOOL, PTR, PTR, PTR, PTR); }

    // ------------------------------------- CoreGraphics / CoreFoundation (verification)

    /** CGWindowListCopyWindowInfo(ulong, ulong) -> CFArrayRef */
    public static FunctionDescriptor cgWindowListCopy() { return FunctionDescriptor.of(PTR, LONG, LONG); }
    /** CFArrayGetCount(CFArrayRef) -> CFIndex */
    public static FunctionDescriptor cfArrayGetCount() { return FunctionDescriptor.of(LONG, PTR); }
    /** CFArrayGetValueAtIndex(CFArrayRef, CFIndex) -> void* */
    public static FunctionDescriptor cfArrayGetValueAtIndex() { return FunctionDescriptor.of(PTR, PTR, LONG); }
    /** CFDictionaryGetValue(CFDictionaryRef, void*) -> void* */
    public static FunctionDescriptor cfDictionaryGetValue() { return FunctionDescriptor.of(PTR, PTR, PTR); }
    /** CFNumberGetValue(CFNumberRef, long, void*) -> bool */
    public static FunctionDescriptor cfNumberGetValue() { return FunctionDescriptor.of(BOOL, PTR, LONG, PTR); }
    /** CFStringGetCString(CFStringRef, char*, CFIndex, long) -> bool */
    public static FunctionDescriptor cfStringGetCString() { return FunctionDescriptor.of(BOOL, PTR, PTR, LONG, LONG); }
    /** CFStringCreateWithCString(void*, char*, long) -> CFStringRef */
    public static FunctionDescriptor cfStringCreateWithCString() { return FunctionDescriptor.of(PTR, PTR, PTR, LONG); }

    // ------------------------------------------------------------------- upcalls

    /** -(BOOL)applicationShouldTerminateAfterLastWindowClosed:(id) */
    public static FunctionDescriptor delegateShouldTerminate() { return FunctionDescriptor.of(BOOL, PTR, PTR, PTR); }
    /** -(void)windowWillClose:(id) */
    public static FunctionDescriptor delegateWindowWillClose() { return FunctionDescriptor.ofVoid(PTR, PTR, PTR); }

    // ------------------------------------------- libobjc / libdispatch / CoreGraphics shims

    /** objc_autoreleasePoolPush(void) -> void*  (zero args — the pool token) */
    public static FunctionDescriptor autoreleasePoolPush() { return FunctionDescriptor.of(PTR); }
    /** objc_autoreleasePoolPop(void*) -> void */
    public static FunctionDescriptor autoreleasePoolPop() { return FunctionDescriptor.ofVoid(PTR); }
    /** objc_setExceptionPreprocessor(fn) -> fn (previous handler) */
    public static FunctionDescriptor setExceptionPreprocessor() { return FunctionDescriptor.of(PTR, PTR); }
    /** dispatch_async(queue, block) -> void */
    public static FunctionDescriptor dispatchAsync() { return FunctionDescriptor.ofVoid(PTR, PTR); }
    /** class_getSuperclass(Class) -> Class */
    public static FunctionDescriptor classGetSuperclass() { return FunctionDescriptor.of(PTR, PTR); }
    /** objc_msgSendSuper(struct objc_super*, SEL) -> void  ([super ...] dispatch) */
    public static FunctionDescriptor msgSendSuperVoid() { return FunctionDescriptor.ofVoid(PTR, PTR); }
    /** -(void)drawRect:(NSRect) — upcall shape for a Java-implemented NSView.drawRect: */
    public static FunctionDescriptor drawRectUpcall() { return FunctionDescriptor.ofVoid(PTR, PTR, NS_RECT); }
    /** -(void)dealloc — upcall shape for a Java-implemented NSView.dealloc */
    public static FunctionDescriptor deallocUpcall() { return FunctionDescriptor.ofVoid(PTR, PTR); }
    /** void (^)(void) — upcall shape for a capture-less block body */
    public static FunctionDescriptor blockVoidUpcall() { return FunctionDescriptor.ofVoid(PTR); }
    /** -(NSMethodSignature *)methodSignatureForSelector:(SEL) — PTR-returning upcall */
    public static FunctionDescriptor methodSignatureUpcall() { return FunctionDescriptor.of(PTR, PTR, PTR, PTR); }
    /** -(NSInteger)method:(id) — data-source upcall shape (numberOfRowsInTableView:); NSInteger = long on 64-bit */
    public static FunctionDescriptor delegateIntUpcall() { return FunctionDescriptor.of(LONG, PTR, PTR, PTR); }
    /** -(id)tableView:(id):(id):(NSInteger) — data-source upcall shape (tableView:objectValueForTableColumn:row:) */
    public static FunctionDescriptor delegateIdIdIntUpcall() { return FunctionDescriptor.of(PTR, PTR, PTR, PTR, PTR, LONG); }
    /** -(NSSize)windowWillResize:(id) toSize:(NSSize) — window delegate veto shape (SIZE return, id + SIZE args) */
    public static FunctionDescriptor delegateWindowWillResize() { return FunctionDescriptor.of(NS_SIZE, PTR, PTR, PTR, NS_SIZE); }

    // --------------------------------------------- CoreText (text rendering shim)

    /** CTFontCreateWithName(CFStringRef, CGFloat, const CGAffineTransform*) -> CTFontRef */
    public static FunctionDescriptor ctFontCreateWithName() { return FunctionDescriptor.of(PTR, PTR, DOUBLE, PTR); }
    /** CTLineCreateWithAttributedString(CFAttributedStringRef) -> CTLineRef */
    public static FunctionDescriptor ctLineCreateWithAttributedString() { return FunctionDescriptor.of(PTR, PTR); }
    /** CTLineDraw(CTLineRef, CGContextRef) -> void */
    public static FunctionDescriptor ctLineDraw() { return FunctionDescriptor.ofVoid(PTR, PTR); }
    /** CTLineGetTypographicBounds(CTLineRef, CGFloat*, CGFloat*, CGFloat*) -> double */
    public static FunctionDescriptor ctLineGetTypographicBounds() { return FunctionDescriptor.of(DOUBLE, PTR, PTR, PTR, PTR); }

    /** CGContextSetRGBFillColor(ctx, r, g, b, a) -> void */
    public static FunctionDescriptor cgSetRGBFillColor() { return FunctionDescriptor.ofVoid(PTR, DOUBLE, DOUBLE, DOUBLE, DOUBLE); }
    /** CGContextSetRGBStrokeColor(ctx, r, g, b, a) -> void */
    public static FunctionDescriptor cgSetRGBStrokeColor() { return FunctionDescriptor.ofVoid(PTR, DOUBLE, DOUBLE, DOUBLE, DOUBLE); }
    /** CGContextSetLineWidth(ctx, w) -> void */
    public static FunctionDescriptor cgSetLineWidth() { return FunctionDescriptor.ofVoid(PTR, DOUBLE); }
    /** CGContextFillRect(ctx, CGRect) -> void */
    public static FunctionDescriptor cgFillRect() { return FunctionDescriptor.ofVoid(PTR, NS_RECT); }
    /** CGContextStrokeRect(ctx, CGRect) -> void */
    public static FunctionDescriptor cgStrokeRect() { return FunctionDescriptor.ofVoid(PTR, NS_RECT); }
    /** CGContextFillEllipseInRect(ctx, CGRect) -> void */
    public static FunctionDescriptor cgFillEllipseInRect() { return FunctionDescriptor.ofVoid(PTR, NS_RECT); }
    /** CGContextMoveToPoint(ctx, x, y) -> void */
    public static FunctionDescriptor cgMoveToPoint() { return FunctionDescriptor.ofVoid(PTR, DOUBLE, DOUBLE); }
    /** CGContextAddLineToPoint(ctx, x, y) -> void */
    public static FunctionDescriptor cgAddLineToPoint() { return FunctionDescriptor.ofVoid(PTR, DOUBLE, DOUBLE); }
    /** CGContextStrokePath(ctx) -> void */
    public static FunctionDescriptor cgStrokePath() { return FunctionDescriptor.ofVoid(PTR); }
    /** CGContextSetShouldAntialias(ctx, bool) -> void */
    public static FunctionDescriptor cgSetShouldAntialias() { return FunctionDescriptor.ofVoid(PTR, BOOL); }
    /** CGEventCreateMouseEvent(source, type, CGPoint, button) -> CGEventRef  (CGEventType/CGMouseButton are uint32) */
    public static FunctionDescriptor cgEventCreateMouseEvent() { return FunctionDescriptor.of(PTR, PTR, INT, NS_POINT, INT); }
    /** CGEventPost(tap, event) -> void  (kCGHIDEventTap = 0) */
    public static FunctionDescriptor cgEventPost() { return FunctionDescriptor.ofVoid(INT, PTR); }

    // --------------------------------------------- registration lists (build time)

    /** libobjc + dlopen downcalls (registered by NsuiFeature — no tracing agent). */
    public static final List<FunctionDescriptor> RUNTIME = List.of(
            dlopen(), objcGetClass(), selRegisterName(), allocateClassPair(), registerClassPair(), addMethod(),
            classGetSuperclass(), msgSendSuperVoid());

    /** CoreGraphics/CoreFoundation downcalls (window-server verification + synthetic events). */
    public static final List<FunctionDescriptor> CORE = List.of(
            cgWindowListCopy(), cfArrayGetCount(), cfArrayGetValueAtIndex(), cfDictionaryGetValue(),
            cfNumberGetValue(), cfStringGetCString(), cfStringCreateWithCString(),
            cgEventCreateMouseEvent(), cgEventPost());

    /** Autorelease-pool shim (libobjc). */
    public static final List<FunctionDescriptor> AUTORELEASE = List.of(
            autoreleasePoolPush(), autoreleasePoolPop());

    /** NSException interception shim (libobjc). */
    public static final List<FunctionDescriptor> EXCEPTION = List.of(
            setExceptionPreprocessor());

    /** Grand-Central-Dispatch shim (libSystem). */
    public static final List<FunctionDescriptor> DISPATCH = List.of(
            dispatchAsync());

    /** CoreGraphics 2D drawing shim. */
    public static final List<FunctionDescriptor> CG_DRAW = List.of(
            cgSetRGBFillColor(), cgSetRGBStrokeColor(), cgSetLineWidth(), cgFillRect(), cgStrokeRect(),
            cgFillEllipseInRect(), cgMoveToPoint(), cgAddLineToPoint(), cgStrokePath(), cgSetShouldAntialias());

    /** CoreText text rendering shim. */
    public static final List<FunctionDescriptor> CT_TEXT = List.of(
            ctFontCreateWithName(), ctLineCreateWithAttributedString(), ctLineDraw(), ctLineGetTypographicBounds());

    /** Upcall descriptors (methods implemented in Java, called by ObjC/AppKit). */
    public static final List<FunctionDescriptor> UPCALLS = List.of(
            delegateShouldTerminate(), delegateWindowWillClose(),
            drawRectUpcall(), deallocUpcall(), blockVoidUpcall(), setExceptionPreprocessor(),
            methodSignatureUpcall(), delegateIntUpcall(), delegateIdIdIntUpcall(),
            delegateWindowWillResize());
}
