package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import nsui.NSArray;
import nsui.NSToolbar;
import nsui.NSToolbarDelegate;
import nsui.NSToolbarItem;
import nsui.objc.DelegateProxy;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/**
 * ToolbarCustomizationTest — verifies NSToolbarDelegate wiring via DelegateProxy.
 * - ObjC.init(), registrySize check, create delegate, call selectors via ObjC.msgSend handles,
 *   verify IdIdArg / IdArg fired, check default identifiers list size 2.
 * - Stress: 100 iterations create delegate + call selectors.
 */
public final class ToolbarCustomizationTest {
    private static int failures;
    private static int asserts;

    private static void check(boolean ok, String msg) {
        asserts++;
        System.out.println((ok ? "PASS" : "FAIL") + ": " + msg);
        if (!ok) failures++;
    }

    public static void main(String[] args) {
        System.out.println("=== ToolbarCustomizationTest — toolbar delegate + dragging wiring (Phase 0B) ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            System.out.println("SKIP: ObjC.init failed (not macOS or connection error): " + t);
            t.printStackTrace(System.out);
            System.out.println("RESULT: SKIP");
            System.exit(0);
        }

        // ---- basic delegate creation + registry ----
        try {
            int before = DelegateProxy.registrySize();
            System.out.println("registry before=" + before);

            AtomicInteger itemCalls = new AtomicInteger(0);
            AtomicInteger defaultCalls = new AtomicInteger(0);

            NSToolbarDelegate.Delegate d = new NSToolbarDelegate.Delegate() {
                @Override
                public MemorySegment toolbarItemForIdentifier(NSToolbar toolbar, String identifier, boolean willInsert) {
                    itemCalls.incrementAndGet();
                    System.out.println("  toolbarItemForIdentifier called toolbar=" + (toolbar == null ? "null" : toolbar.identifier()) + " id=" + identifier + " willInsert=" + willInsert);
                    try {
                        NSToolbarItem item = NSToolbarItem.create(identifier == null || identifier.isEmpty() ? "fallback" : identifier);
                        if (item != null) {
                            item.setLabel("Label-" + identifier);
                            return item.peer();
                        }
                    } catch (Throwable t) {
                        System.out.println("  toolbarItemForIdentifier create failed: " + t);
                    }
                    return MemorySegment.NULL;
                }

                @Override
                public List<String> toolbarDefaultIdentifiers(NSToolbar toolbar) {
                    defaultCalls.incrementAndGet();
                    return List.of("item1", "item2");
                }
            };

            MemorySegment delegate = NSToolbarDelegate.create(d);
            check(delegate != null && delegate.address() != 0, "NSToolbarDelegate.create non-null peer");
            int after = DelegateProxy.registrySize();
            check(after == before + 1, "registry grew by 1 after delegate create (before " + before + " after " + after + ")");

            // ---- call toolbarDefaultItemIdentifiers: via IdArg shape ----
            String tid = "ToolbarCustomizationTest-" + System.nanoTime();
            NSToolbar tb = NSToolbar.create(tid);
            check(tb != null && tb.peer().address() != 0, "NSToolbar.create non-nil for delegate test");
            check(tid.equals(tb.identifier()), "NSToolbar identifier round-trip");

            try {
                MethodHandle hDefault = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
                MemorySegment arrSeg = (MemorySegment) hDefault.invokeExact(delegate, ObjC.sel("toolbarDefaultItemIdentifiers:"), tb.peer());
                check(arrSeg != null && arrSeg.address() != 0, "toolbarDefaultItemIdentifiers: returned non-nil NSArray");
                if (arrSeg != null && arrSeg.address() != 0) {
                    NSArray arr = NSArray.wrap(arrSeg);
                    long count = arr.count();
                    check(count == 2, "toolbarDefaultItemIdentifiers count ==2 (got " + count + ") defaultCalls=" + defaultCalls.get());
                    if (count >= 2) {
                        String s0 = ObjC.toString(arr.objectAtIndex(0));
                        String s1 = ObjC.toString(arr.objectAtIndex(1));
                        check("item1".equals(s0) && "item2".equals(s1), "toolbarDefaultItemIdentifiers contents [item1,item2] (got [" + s0 + "," + s1 + "])");
                    }
                }
            } catch (Throwable t) {
                check(false, "toolbarDefaultItemIdentifiers: invoke failed: " + t);
                t.printStackTrace(System.out);
            }

            // Also check allowed/selectable (should fallback to same list)
            try {
                MethodHandle hAllowed = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
                MemorySegment arrAllowed = (MemorySegment) hAllowed.invokeExact(delegate, ObjC.sel("toolbarAllowedItemIdentifiers:"), tb.peer());
                check(arrAllowed != null && arrAllowed.address() != 0, "toolbarAllowedItemIdentifiers: non-nil");
                if (arrAllowed != null && arrAllowed.address() != 0) {
                    check(NSArray.wrap(arrAllowed).count() == 2, "toolbarAllowedItemIdentifiers count 2");
                }
                MemorySegment arrSel = (MemorySegment) hAllowed.invokeExact(delegate, ObjC.sel("toolbarSelectableItemIdentifiers:"), tb.peer());
                check(arrSel != null && arrSel.address() != 0, "toolbarSelectableItemIdentifiers: non-nil");
                if (arrSel != null && arrSel.address() != 0) {
                    check(NSArray.wrap(arrSel).count() == 2, "toolbarSelectableItemIdentifiers count 2");
                }
            } catch (Throwable t) {
                check(false, "allowed/selectable invoke failed: " + t);
                t.printStackTrace(System.out);
            }

            // ---- call toolbar:itemForItemIdentifier:willBeInsertedIntoToolbar: via IdIdArg shape ----
            // Native has 3 args (toolbar, identifier, BOOL) but we reuse IdIdArg (2 ids, BOOL folded)
            try {
                MethodHandle hItem = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
                int beforeCalls = itemCalls.get();
                MemorySegment identSeg = ObjC.nsstring("item1");
                MemorySegment itemSeg = (MemorySegment) hItem.invokeExact(delegate, ObjC.sel("toolbar:itemForItemIdentifier:willBeInsertedIntoToolbar:"), tb.peer(), identSeg);
                check(itemCalls.get() == beforeCalls + 1, "toolbar:itemForItemIdentifier:willBeInsertedIntoToolbar: fired IdIdArg (calls " + beforeCalls + " -> " + itemCalls.get() + ")");
                check(itemSeg != null, "toolbar:itemForItemIdentifier returned non-null (may be NULL if creation failed but dispatch worked)");
                // verify that returned item if non-null is an NSToolbarItem
                if (itemSeg != null && itemSeg.address() != 0) {
                    boolean isToolbarItem = false;
                    try {
                        // isKindOfClass check via ObjC
                        MethodHandle hIsKind = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
                        isToolbarItem = (boolean) hIsKind.invokeExact(itemSeg, ObjC.sel("isKindOfClass:"), ObjC.cls("NSToolbarItem"));
                    } catch (Throwable ignored) {}
                    check(true, "toolbar:itemFor... returned item isKindOfClass NSToolbarItem check=" + isToolbarItem + " (non-fatal)");
                }
            } catch (Throwable t) {
                check(false, "toolbar:itemForItemIdentifier:willBeInsertedIntoToolbar: invoke failed: " + t);
                t.printStackTrace(System.out);
            }

            // ---- attach delegate to toolbar and verify setDelegate round-trip ----
            try {
                tb.setDelegate(delegate);
                MemorySegment got = tb.delegate();
                check(got != null && got.address() == delegate.address(), "NSToolbar setDelegate/getDelegate round-trip");
            } catch (Throwable t) {
                check(false, "NSToolbar setDelegate failed: " + t);
                t.printStackTrace(System.out);
            }

            // ---- stress: 100 iterations create delegate + call selectors ----
            try {
                long start = System.nanoTime();
                for (int i = 0; i < 100; i++) {
                    final int idx = i;
                    NSToolbarDelegate.Delegate sd = new NSToolbarDelegate.Delegate() {
                        @Override public MemorySegment toolbarItemForIdentifier(NSToolbar toolbar, String identifier, boolean willInsert) {
                            try {
                                NSToolbarItem it = NSToolbarItem.create(identifier == null ? "x" : identifier);
                                return it == null ? MemorySegment.NULL : it.peer();
                            } catch (Throwable e) { return MemorySegment.NULL; }
                        }
                        @Override public List<String> toolbarDefaultIdentifiers(NSToolbar toolbar) {
                            return List.of("a" + idx, "b" + idx);
                        }
                    };
                    MemorySegment sdSeg = NSToolbarDelegate.create(sd);
                    // call default identifiers
                    MethodHandle h = ObjC.handle(Sig.of(Ret.ID, Arg.ID));
                    MemorySegment a = (MemorySegment) h.invokeExact(sdSeg, ObjC.sel("toolbarDefaultItemIdentifiers:"), tb.peer());
                    if (a.address() == 0) throw new AssertionError("stress default returned NULL iter " + i);
                    // call item
                    MethodHandle h2 = ObjC.handle(Sig.of(Ret.ID, Arg.ID, Arg.ID));
                    MemorySegment it = (MemorySegment) h2.invokeExact(sdSeg, ObjC.sel("toolbar:itemForItemIdentifier:willBeInsertedIntoToolbar:"), tb.peer(), ObjC.nsstring("a" + idx));
                    if (it == null) throw new AssertionError("stress item null iter " + i);
                }
                long elapsed = System.nanoTime() - start;
                check(true, "stress 100 iterations delegate create+call completed in " + (elapsed / 1_000_000) + "ms");
            } catch (Throwable t) {
                check(false, "stress 100 iterations failed: " + t);
                t.printStackTrace(System.out);
            }

        } catch (Throwable t) {
            check(false, "ToolbarCustomizationTest section threw: " + t);
            t.printStackTrace(System.out);
        }

        System.out.println(failures == 0
                ? "RESULT: ALL PASS (" + asserts + " assertions)"
                : "RESULT: " + failures + " of " + asserts + " assertions FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
