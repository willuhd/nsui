package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.*;
import nsui.objc.Autorelease;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// TouchBarItemsTest — covers the concrete Touch Bar item classes that had no
/// tests: NSButtonTouchBarItem, NSSliderTouchBarItem, NSPopoverTouchBarItem.
///
/// Coverage:
/// - creation paths (alloc/initWithIdentifier: and the class factories,
///   including buttonTouchBarItemWithIdentifier:title:target:action:)
/// - isKindOfClass checks for each concrete item class
/// - property round-trips: title, target, action (SEL identity), slider
///   install, min/max doubles (forwarded through the wrapped slider),
///   popover bar assignment (popoverTouchBar / setPopoverTouchBar:)
/// - an NSPopoverTouchBarItem wired so its popover NSTouchBar contains an
///   NSCustomTouchBarItem wrapping an NSButton (the "menu inside the Touch
///   Bar" shape), supplied through a touchBar:makeItemForIdentifier: delegate
/// - respondsToSelector guards on optional selectors
/// - a 100x stress loop creating and draining (autorelease pool) all three
///   item types
///
/// Non-interactive and self-terminating: prints PASS:/FAIL: lines and ends
/// with RESULT: PASS or RESULT: FAIL, exiting 0 or 1.
public final class TouchBarItemsTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    /// respondsToSelector: probe used to guard optional selectors in assertions.
    private static boolean responds(MemorySegment peer, String selectorName) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(
                    peer, ObjC.sel("respondsToSelector:"), ObjC.sel(selectorName));
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== TouchBarItemsTest ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            String m = String.valueOf(t.getMessage()).toLowerCase();
            if (m.contains("connection") || m.contains("dlopen") || m.contains("appkit")) {
                System.out.println("SKIP: ObjC.init failed (not macOS / connection error): " + t);
                System.out.println("RESULT: SKIP (connection error, continuing)");
                System.exit(0);
            }
            System.out.println("FAIL: ObjC.init threw unexpected: " + t);
            t.printStackTrace(System.out);
            System.exit(1);
        }

        // AppKit shared application — best effort; item classes work headless.
        try {
            NSApplication app = NSApplication.shared();
            app.setActivationPolicy(0);
        } catch (Throwable t) {
            System.out.println("NOTE: NSApplication init failed (may be headless): " + t);
        }

        // ---------------- NSButtonTouchBarItem ----------------
        try {
            String bid = "btn.item." + System.nanoTime();
            MemorySegment target = DelegateProxy.actionTarget("tbiAction:", s -> {});
            NSButtonTouchBarItem item = NSButtonTouchBarItem.create(bid, "Go", target, "tbiAction:");
            check(item != null && item.peer().address() != 0, "NSButtonTouchBarItem.create(id,title,target,action) non-nil");
            check(item.isKindOfClass("NSButtonTouchBarItem"), "NSButtonTouchBarItem isKindOfClass NSButtonTouchBarItem");
            check(bid.equals(item.identifier()), "NSButtonTouchBarItem identifier round-trip (got \"" + item.identifier() + "\")");

            check("Go".equals(item.title()), "NSButtonTouchBarItem title round-trip (got \"" + item.title() + "\")");
            item.setTitle("Stop");
            check("Stop".equals(item.title()), "NSButtonTouchBarItem setTitle Stop round-trip (got \"" + item.title() + "\")");

            MemorySegment gotTarget = item.target();
            check(gotTarget != null && gotTarget.address() == target.address(),
                    "NSButtonTouchBarItem target round-trip same peer (got " + (gotTarget == null ? "null" : Long.toHexString(gotTarget.address()))
                            + " expect " + Long.toHexString(target.address()) + ")");

            MemorySegment act = item.action();
            check(act != null && act.address() == ObjC.sel("tbiAction:").address(),
                    "NSButtonTouchBarItem action SEL identity (got " + (act == null ? "null" : Long.toHexString(act.address())) + ")");
            item.setAction("tbiAction2:");
            MemorySegment act2 = item.action();
            check(act2 != null && act2.address() == ObjC.sel("tbiAction2:").address(),
                    "NSButtonTouchBarItem setAction rewire SEL identity");

            check(responds(item.peer(), "setTarget:") && responds(item.peer(), "setAction:"),
                    "NSButtonTouchBarItem responds to setTarget:/setAction:");
            if (responds(item.peer(), "setEnabled:")) {
                item.setEnabled(false);
                boolean en = item.isEnabled();
                check(!en, "NSButtonTouchBarItem setEnabled(false)/isEnabled round-trip (got " + en + ")");
                item.setEnabled(true);
                check(item.isEnabled(), "NSButtonTouchBarItem setEnabled(true)/isEnabled round-trip");
            } else {
                check(true, "SKIP setEnabled/isEnabled (selector absent on this OS)");
            }

            // bare alloc/init variant
            NSButtonTouchBarItem bare = NSButtonTouchBarItem.create("btn.bare." + System.nanoTime());
            check(bare != null && bare.peer().address() != 0, "NSButtonTouchBarItem.create(id) alloc/init non-nil");
            check(bare.isKindOfClass("NSButtonTouchBarItem"), "bare NSButtonTouchBarItem isKindOfClass");
            bare.setTitle("Bare");
            check("Bare".equals(bare.title()), "bare NSButtonTouchBarItem setTitle/title round-trip");
        } catch (Throwable t) {
            check(false, "NSButtonTouchBarItem section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSSliderTouchBarItem ----------------
        try {
            String sid = "slider.item." + System.nanoTime();
            NSSliderTouchBarItem item = NSSliderTouchBarItem.create(sid);
            check(item != null && item.peer().address() != 0, "NSSliderTouchBarItem.create(id) non-nil");
            check(item.isKindOfClass("NSSliderTouchBarItem"), "NSSliderTouchBarItem isKindOfClass NSSliderTouchBarItem");
            check(sid.equals(item.identifier()), "NSSliderTouchBarItem identifier round-trip (got \"" + item.identifier() + "\")");

            MemorySegment autoSlider = item.slider();
            if (autoSlider != null && autoSlider.address() != 0) {
                check(true, "NSSliderTouchBarItem auto-created slider present");
            } else {
                System.out.println("  NOTE: slider not auto-created before setSlider on this OS");
                check(true, "NSSliderTouchBarItem slider() nil before setSlider tolerated");
            }

            // pair with NSSlider.create(NSRect) — install a custom slider
            NSSlider custom = NSSlider.create(new NSRect(0, 0, 140, 30));
            check(custom != null && custom.peer().address() != 0, "NSSlider.create for slider item non-nil");
            item.setSlider(custom);
            MemorySegment gotSlider = item.slider();
            check(gotSlider != null && gotSlider.address() == custom.peer().address(),
                    "NSSliderTouchBarItem setSlider/slider round-trip same peer (got "
                            + (gotSlider == null ? "null" : Long.toHexString(gotSlider.address()))
                            + " expect " + Long.toHexString(custom.peer().address()) + ")");

            // min/max forward through the wrapped slider
            item.setMinValue(5.0);
            check(item.minValue() == 5.0, "NSSliderTouchBarItem setMinValue/minValue round-trip (got " + item.minValue() + ")");
            item.setMaxValue(250.0);
            check(item.maxValue() == 250.0, "NSSliderTouchBarItem setMaxValue/maxValue round-trip (got " + item.maxValue() + ")");
            check(custom.minValue() == 5.0 && custom.maxValue() == 250.0,
                    "NSSliderTouchBarItem min/max landed on the wrapped NSSlider");

            // item-level doubleValue (macOS 10.15+)
            if (responds(item.peer(), "setDoubleValue:")) {
                item.setDoubleValue(7.5);
                double dv = item.doubleValue();
                check(dv == 7.5, "NSSliderTouchBarItem setDoubleValue/doubleValue round-trip (got " + dv + ")");
            } else {
                item.setDoubleValue(7.5);
                check(true, "SKIP doubleValue round-trip (selector absent on this OS, no-crash pass)");
            }

            // factory variant with target/action
            MemorySegment target = DelegateProxy.actionTarget("tbiSlide:", s -> {});
            NSSliderTouchBarItem fact = NSSliderTouchBarItem.create("slider.factory." + System.nanoTime(), target, "tbiSlide:");
            check(fact != null && fact.peer().address() != 0, "NSSliderTouchBarItem.create(id,target,action) factory non-nil");
            check(fact.isKindOfClass("NSSliderTouchBarItem"), "factory NSSliderTouchBarItem isKindOfClass");
            fact.setTarget(target);
            MemorySegment gotTarget = fact.target();
            check(gotTarget != null && gotTarget.address() == target.address(), "NSSliderTouchBarItem target round-trip same peer");
            fact.setAction("tbiSlide2:");
            MemorySegment act = fact.action();
            check(act != null && act.address() == ObjC.sel("tbiSlide2:").address(),
                    "NSSliderTouchBarItem setAction SEL identity (got " + (act == null ? "null" : Long.toHexString(act.address())) + ")");
            check(responds(fact.peer(), "slider") && responds(fact.peer(), "setSlider:"),
                    "NSSliderTouchBarItem responds to slider/setSlider:");
        } catch (Throwable t) {
            check(false, "NSSliderTouchBarItem section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- NSPopoverTouchBarItem ----------------
        try {
            String pid = "pop.item." + System.nanoTime();
            NSPopoverTouchBarItem item = NSPopoverTouchBarItem.create(pid);
            check(item != null && item.peer().address() != 0, "NSPopoverTouchBarItem.create(id) non-nil");
            check(item.isKindOfClass("NSPopoverTouchBarItem"), "NSPopoverTouchBarItem isKindOfClass NSPopoverTouchBarItem");
            check(pid.equals(item.identifier()), "NSPopoverTouchBarItem identifier round-trip (got \"" + item.identifier() + "\")");

            check(responds(item.peer(), "popoverTouchBar") && responds(item.peer(), "setPopoverTouchBar:"),
                    "NSPopoverTouchBarItem responds to popoverTouchBar/setPopoverTouchBar:");

            NSTouchBar bar1 = NSTouchBar.create();
            item.setPopover(bar1);
            NSTouchBar got1 = item.popover();
            check(got1 != null && got1.peer().address() == bar1.peer().address(),
                    "NSPopoverTouchBarItem setPopover/popover round-trip same peer (got "
                            + (got1 == null ? "null" : Long.toHexString(got1.peer().address()))
                            + " expect " + Long.toHexString(bar1.peer().address()) + ")");

            // assigning a second bar swaps the popover content
            NSTouchBar bar2 = NSTouchBar.create();
            item.setPopover(bar2);
            NSTouchBar got2 = item.popover();
            check(got2 != null && got2.peer().address() == bar2.peer().address(),
                    "NSPopoverTouchBarItem second setPopover swaps the popover bar");

            // unhosted show/dismiss must be safe no-ops
            item.showPopover();
            item.dismissPopover();
            check(true, "NSPopoverTouchBarItem showPopover/dismissPopover no crash (unhosted)");
        } catch (Throwable t) {
            check(false, "NSPopoverTouchBarItem section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- popover "menu" wiring: popover bar contains a custom item with an NSButton ----------------
        try {
            NSPopoverTouchBarItem popItem = NSPopoverTouchBarItem.create("pop.menu." + System.nanoTime());
            NSTouchBar popBar = NSTouchBar.create();

            String customIdent = "pop.custom." + System.nanoTime();
            NSCustomTouchBarItem custom = NSCustomTouchBarItem.create(customIdent);
            MemorySegment btnTarget = DelegateProxy.actionTarget("popBtn:", s -> {});
            NSButton btn = NSButton.create(new NSRect(0, 0, 80, 30), "PopBtn", btnTarget, "popBtn:");
            check(btn != null && btn.peer().address() != 0, "popover content NSButton non-nil");
            custom.setView(btn);
            NSView gotView = custom.view();
            check(gotView != null && gotView.peer().address() == btn.peer().address(),
                    "popover custom item view is the NSButton (same peer)");

            // the popover bar serves the custom item by identifier
            NSArray ids = NSArray.mutableArray();
            ids.addObject(NSString.wrap(ObjC.nsstring(customIdent)));
            popBar.setDefaultItemIdentifiers(ids);
            check(popBar.itemIdentifiers() != null && popBar.itemIdentifiers().count() == 1,
                    "popover bar defaultItemIdentifiers count 1");

            AtomicInteger makeCalls = new AtomicInteger(0);
            Map<String, DelegateProxy.IdIdArg> idIds = new LinkedHashMap<>();
            idIds.put("touchBar:makeItemForIdentifier:", (touchBar, identifier) -> {
                makeCalls.incrementAndGet();
                if (identifier == null || identifier.address() == 0) return MemorySegment.NULL;
                String identStr = ObjC.toString(identifier);
                return customIdent.equals(identStr) ? custom.peer() : MemorySegment.NULL;
            });
            MemorySegment delegate = DelegateProxy.delegate("NSObject", "TBIPopDel_" + System.nanoTime(),
                    Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), idIds);
            check(delegate != null && delegate.address() != 0, "popover bar delegate non-nil");
            popBar.setDelegate(delegate);

            // direct dispatch — what AppKit does when materializing the popover bar
            MemorySegment made = null;
            try {
                MethodHandle hMake = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
                made = (MemorySegment) hMake.invokeExact(delegate, ObjC.sel("touchBar:makeItemForIdentifier:"),
                        popBar.peer(), ObjC.nsstring(customIdent));
            } catch (Throwable t) {
                check(false, "touchBar:makeItemForIdentifier: direct send threw: " + t);
            }
            check(made != null && made.address() == custom.peer().address(),
                    "delegate returns the NSCustomTouchBarItem for the popover identifier");
            check(makeCalls.get() == 1, "delegate makeItem handler called exactly once (got " + makeCalls.get() + ")");

            // wire the popover bar into the item — the "menu" appears from here
            popItem.setPopover(popBar);
            NSTouchBar gotPop = popItem.popover();
            check(gotPop != null && gotPop.peer().address() == popBar.peer().address(),
                    "wired NSPopoverTouchBarItem popover bar round-trip same peer");

            popBar.setDelegate((NSObject) null);
        } catch (Throwable t) {
            check(false, "popover wiring section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------- 100x stress: create + drain all three item types ----------------
        try {
            AtomicInteger stressOk = new AtomicInteger(0);
            for (int i = 0; i < 100; i++) {
                final int idx = i;
                Autorelease.run(() -> {
                    String tag = "stress." + idx + "." + System.nanoTime();
                    NSButtonTouchBarItem b = NSButtonTouchBarItem.create(tag, "S" + idx, null, null);
                    NSSliderTouchBarItem sl = NSSliderTouchBarItem.create(tag);
                    NSPopoverTouchBarItem po = NSPopoverTouchBarItem.create(tag);
                    boolean ok = b != null && sl != null && po != null
                            && b.isKindOfClass("NSButtonTouchBarItem")
                            && sl.isKindOfClass("NSSliderTouchBarItem")
                            && po.isKindOfClass("NSPopoverTouchBarItem");
                    if (ok) {
                        b.setTitle("X" + idx);
                        ok = ("X" + idx).equals(b.title());
                    }
                    if (ok) stressOk.incrementAndGet();
                });
            }
            check(stressOk.get() == 100, "stress loop 100x create/drain items (ok=" + stressOk.get() + "/100)");
        } catch (Throwable t) {
            check(false, "stress section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(failures == 0
                ? "RESULT: PASS (" + asserts + " assertions)"
                : "RESULT: FAIL (" + failures + " of " + asserts + " assertions failed)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
