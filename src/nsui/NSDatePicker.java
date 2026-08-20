package nsui;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// NSDatePicker — an AppKit date/time picker control. Thin, 1:1, stateless wrapper
/// over a native `NSDatePicker`: every method maps to one `objc_msgSend`
/// selector. It is an `NSControl` (an `NSView`), so it fits any view
/// hierarchy.
///
/// Dates are passed as raw `NSDate` ids (a `MemorySegment`); this
/// toolkit does not yet own an `NSDate` wrapper, so callers build them via
/// `[[NSDate dateWithTimeIntervalSinceNow:]]` (see the test).
public final class NSDatePicker extends NSControl {

    // ---- cached handles, resolved once lazily at runtime (never in a static initializer) ----
    private static volatile boolean initialized;
    private static MethodHandle hInitFrame;   // (id, SEL, NSRect) -> id
    private static MethodHandle hSetDate;     // (id, SEL, id) -> void     [setDateValue:]
    private static MethodHandle hDate;        // (id, SEL) -> id           [dateValue]
    private static MethodHandle hSetInt;      // (id, SEL, int) -> void    [setDatePickerStyle: / setDatePickerElements:]
    private static MethodHandle hSetId;       // (id, SEL, id) -> void     [setTimeZone:/setLocale:/setCalendar:/setMinDate:/setMaxDate: etc]

    private NSDatePicker(MemorySegment peer) {
        super(peer);
        ensureInit();
    }

    private static synchronized void ensureInit() {
        if (initialized) return;
        hInitFrame = ObjC.handle(Sig.of(Ret.ID, Arg.RECT));
        hSetDate = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        hDate = ObjC.handle(Sig.of(Ret.ID));
        hSetInt = ObjC.handle(Sig.of(Ret.VOID, Arg.INT));
        hSetId = ObjC.handle(Sig.of(Ret.VOID, Arg.ID));
        initialized = true;
    }

    /// `[[NSDatePicker alloc] initWithFrame:frame]` — a new date picker at the given rect.
    public static NSDatePicker create(NSRect frame) {
        ensureInit();
        MemorySegment p = ObjC.msgSendId(ObjC.cls("NSDatePicker"), ObjC.sel("alloc"));
        try {
            p = (MemorySegment) hInitFrame.invokeExact(p, ObjC.sel("initWithFrame:"), frame.toSegment());
        } catch (Throwable t) {
            throw new RuntimeException("initWithFrame: failed for NSDatePicker", t);
        }
        if (p.address() == 0) {
            throw new IllegalStateException("NSDatePicker alloc/initWithFrame: returned nil");
        }
        return new NSDatePicker(p);
    }

    // ---------------------------------------------------------------- instance API

    /// [picker setDateValue:] — the displayed/selected date (an NSDate id).
    public void setDateValue(MemorySegment nsDate) {
        try {
            hSetDate.invokeExact(peer, ObjC.sel("setDateValue:"), nsDate);
        } catch (Throwable t) {
            throw new RuntimeException("setDateValue: failed", t);
        }
    }

    /// [picker dateValue] — the currently selected date as an NSDate id (nil if none).
    public MemorySegment dateValue() {
        try {
            return (MemorySegment) hDate.invokeExact(peer, ObjC.sel("dateValue"));
        } catch (Throwable t) {
            throw new RuntimeException("dateValue failed", t);
        }
    }

    /// [picker setDatePickerStyle:] — NSDatePickerStyle (1 = TextFieldAndStepper).
    public void setDatePickerStyle(long style) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setDatePickerStyle:"), style);
        } catch (Throwable t) {
            throw new RuntimeException("setDatePickerStyle: failed", t);
        }
    }

    /// [picker datePickerStyle] — current style.
    public long datePickerStyle() {
        return ObjC.msgSendLong(peer, ObjC.sel("datePickerStyle"));
    }

    /// [picker setDatePickerElements:] — bitmask of NSDatePickerElementFlags (0x3 = Hour|Minute).
    public void setDatePickerElements(long elements) {
        try {
            hSetInt.invokeExact(peer, ObjC.sel("setDatePickerElements:"), elements);
        } catch (Throwable t) {
            throw new RuntimeException("setDatePickerElements: failed", t);
        }
    }

    /// [picker datePickerElements] — current element flags.
    public long datePickerElements() {
        return ObjC.msgSendLong(peer, ObjC.sel("datePickerElements"));
    }

    /// [picker timeZone] — NSTimeZone id (or nil).
    public MemorySegment timeZone() {
        try {
            return (MemorySegment) hDate.invokeExact(peer, ObjC.sel("timeZone"));
        } catch (Throwable t) {
            throw new RuntimeException("timeZone failed", t);
        }
    }

    /// [picker setTimeZone:] — set NSTimeZone (or nil for default).
    public void setTimeZone(MemorySegment tz) {
        try {
            hSetId.invokeExact(peer, ObjC.sel("setTimeZone:"), (MemorySegment) ((MemorySegment) (tz == null ? MemorySegment.NULL : tz)));
        } catch (Throwable t) {
            throw new RuntimeException("setTimeZone: failed", t);
        }
    }

    /// [picker locale] — NSLocale id (or nil).
    public MemorySegment locale() {
        try {
            return (MemorySegment) hDate.invokeExact(peer, ObjC.sel("locale"));
        } catch (Throwable t) {
            throw new RuntimeException("locale failed", t);
        }
    }

    /// [picker setLocale:] — set NSLocale (or nil).
    public void setLocale(MemorySegment locale) {
        try {
            hSetId.invokeExact(peer, ObjC.sel("setLocale:"), (MemorySegment) ((MemorySegment) (locale == null ? MemorySegment.NULL : locale)));
        } catch (Throwable t) {
            throw new RuntimeException("setLocale: failed", t);
        }
    }

    /// [picker calendar] — NSCalendar id (or nil).
    public MemorySegment calendar() {
        try {
            return (MemorySegment) hDate.invokeExact(peer, ObjC.sel("calendar"));
        } catch (Throwable t) {
            throw new RuntimeException("calendar failed", t);
        }
    }

    /// [picker setCalendar:] — set NSCalendar (or nil).
    public void setCalendar(MemorySegment calendar) {
        try {
            hSetId.invokeExact(peer, ObjC.sel("setCalendar:"), (MemorySegment) ((MemorySegment) (calendar == null ? MemorySegment.NULL : calendar)));
        } catch (Throwable t) {
            throw new RuntimeException("setCalendar: failed", t);
        }
    }

    /// [picker minDate] — minimum selectable date (or nil).
    public MemorySegment minDate() {
        try {
            return (MemorySegment) hDate.invokeExact(peer, ObjC.sel("minDate"));
        } catch (Throwable t) {
            throw new RuntimeException("minDate failed", t);
        }
    }

    /// [picker setMinDate:] — set minimum date (nil clears).
    public void setMinDate(MemorySegment date) {
        try {
            hSetId.invokeExact(peer, ObjC.sel("setMinDate:"), (MemorySegment) ((MemorySegment) (date == null ? MemorySegment.NULL : date)));
        } catch (Throwable t) {
            throw new RuntimeException("setMinDate: failed", t);
        }
    }

    /// [picker maxDate] — maximum selectable date (or nil).
    public MemorySegment maxDate() {
        try {
            return (MemorySegment) hDate.invokeExact(peer, ObjC.sel("maxDate"));
        } catch (Throwable t) {
            throw new RuntimeException("maxDate failed", t);
        }
    }

    /// [picker setMaxDate:] — set maximum date (nil clears).
    public void setMaxDate(MemorySegment date) {
        try {
            hSetId.invokeExact(peer, ObjC.sel("setMaxDate:"), (MemorySegment) ((MemorySegment) (date == null ? MemorySegment.NULL : date)));
        } catch (Throwable t) {
            throw new RuntimeException("setMaxDate: failed", t);
        }
    }

    /// [picker drawsBackground] — whether draws background.
    public boolean drawsBackground() {
        return ObjC.msgSendBool(peer, ObjC.sel("drawsBackground"));
    }

    /// [picker setDrawsBackground:] — set draws background.
    public void setDrawsBackground(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setDrawsBackground:"), flag);
    }

    /// [picker isBezeled] — whether bezeled.
    public boolean isBezeled() {
        return ObjC.msgSendBool(peer, ObjC.sel("isBezeled"));
    }

    /// [picker setBezeled:] — set bezeled.
    public void setBezeled(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBezeled:"), flag);
    }

    /// [picker isBordered] — whether bordered.
    public boolean isBordered() {
        return ObjC.msgSendBool(peer, ObjC.sel("isBordered"));
    }

    /// [picker setBordered:] — set bordered.
    public void setBordered(boolean flag) {
        ObjC.msgSendVoidBool(peer, ObjC.sel("setBordered:"), flag);
    }

    /// [picker backgroundColor] — background color id.
    public MemorySegment backgroundColor() {
        return ObjC.msgSendId(peer, ObjC.sel("backgroundColor"));
    }

    /// [picker setBackgroundColor:] — set background color.
    public void setBackgroundColor(NSColor color) {
        MemorySegment p = (color == null) ? MemorySegment.NULL : color.peer();
        ObjC.msgSendVoidId(peer, ObjC.sel("setBackgroundColor:"), p);
    }

    /// [picker textColor] — text color id.
    public MemorySegment textColor() {
        return ObjC.msgSendId(peer, ObjC.sel("textColor"));
    }

    /// [picker setTextColor:] — set text color.
    public void setTextColor(NSColor color) {
        MemorySegment p = (color == null) ? MemorySegment.NULL : color.peer();
        ObjC.msgSendVoidId(peer, ObjC.sel("setTextColor:"), p);
    }

    /// [picker datePickerMode] — NSDatePickerMode.
    public long datePickerMode() {
        return ObjC.msgSendLong(peer, ObjC.sel("datePickerMode"));
    }

    /// [picker setDatePickerMode:] — set mode.
    public void setDatePickerMode(long mode) {
        ObjC.msgSendVoidLong(peer, ObjC.sel("setDatePickerMode:"), mode);
    }
}
