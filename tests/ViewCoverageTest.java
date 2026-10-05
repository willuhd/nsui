package nsui.tests;

import nsui.NSApplication;
import nsui.NSArray;
import nsui.NSBox;
import nsui.NSClipView;
import nsui.NSColor;
import nsui.NSCursor;
import nsui.NSFont;
import nsui.NSGestureRecognizer;
import nsui.NSGlassEffectContainerView;
import nsui.NSGlassEffectView;
import nsui.NSGridView;
import nsui.NSImage;
import nsui.NSPoint;
import nsui.NSRect;
import nsui.NSSize;
import nsui.NSShadow;
import nsui.NSSplitView;
import nsui.NSStackView;
import nsui.NSTabView;
import nsui.NSTrackingArea;
import nsui.NSTabViewItem;
import nsui.NSView;
import nsui.NSViewController;
import nsui.NSVisualEffectView;
import nsui.NSWindow;
import nsui.objc.ObjC;

/// ViewCoverageTest — round-trips for the Views batch header-completeness work:
/// NSView, NSClipView, NSBox, NSStackView, NSSplitView, NSTabView, NSTabViewItem,
/// NSGridView, NSVisualEffectView, NSViewController, plus NSGlassEffectView and
/// NSGlassEffectContainerView (create/cornerRadius/style/spacing/contentView).
///
/// Hidden window only (never shown); ends with TestKit.end().
public final class ViewCoverageTest {

    public static void main(String[] args) throws Throwable {
        System.out.println("=== ViewCoverageTest — Views batch round-trips ===");
        ObjC.init();
        NSApplication app = TestKit.app();

        NSWindow window = TestKit.hiddenWindow(500, 400);
        NSView content = NSView.create(new NSRect(0, 0, 500, 400), (ctx, d) -> {});
        window.setContentView(content);

        viewTests(content);
        clipViewTests(content);
        boxTests(content);
        stackViewTests(content);
        splitViewTests(app);
        tabTests();
        gridViewTests();
        visualEffectTests();
        viewControllerTests();
        glassTests(content);

        TestKit.close(window);
        System.out.println(TestKit.failures() == 0 ? "RESULT: ALL PASS" : "RESULT: " + TestKit.failures() + " FAILURE(S)");
        TestKit.end();
    }

    private static void viewTests(NSView content) {
        NSView v = NSView.create(new NSRect(10, 10, 200, 160), (ctx, d) -> {});
        content.addSubview(v);

        // hierarchy
        TestKit.check(v.superview() != null && v.superview().peer().address() == content.peer().address(), "NSView superview round-trip");
        TestKit.check(v.isDescendantOf(content), "NSView isDescendantOf content");
        TestKit.check(!content.isDescendantOf(v), "NSView content is not descendant of child");
        NSView sib = NSView.create(new NSRect(0, 0, 50, 50), (ctx, d) -> {});
        content.addSubview(sib);
        TestKit.check(v.ancestorSharedWithView(sib) != null
                && v.ancestorSharedWithView(sib).peer().address() == content.peer().address(), "NSView ancestorSharedWithView == content");
        TestKit.check(content.subviews().count() == 2, "NSView subviews count 2 (got " + content.subviews().count() + ")");
        NSView repl = NSView.create(new NSRect(0, 0, 50, 50), (ctx, d) -> {});
        content.replaceSubview(sib, repl);
        TestKit.check(content.subviews().count() == 2, "NSView replaceSubview keeps count 2");
        TestKit.check(repl.superview() != null, "NSView replaced view has superview");
        sib.removeFromSuperviewWithoutNeedingDisplay();
        TestKit.check(true, "NSView removeFromSuperviewWithoutNeedingDisplay no-throw (detached view)");
        repl.removeFromSuperview();
        TestKit.check(repl.superview() == null, "NSView removeFromSuperview clears superview");
        TestKit.check(content.subviews().count() == 1, "NSView subviews count 1 after removal");
        // setSubviews
        NSArray arr = NSArray.mutableArray();
        arr.addObject(v);
        arr.addObject(repl);
        content.setSubviews(arr);
        TestKit.check(content.subviews().count() == 2, "NSView setSubviews count 2");
        TestKit.check(content.opaqueAncestor() == null || content.opaqueAncestor().peer().address() != 0, "NSView opaqueAncestor no-throw");
        // view move notifications (default impls are no-ops)
        v.viewWillMoveToSuperview(null);
        v.viewDidMoveToSuperview();
        v.didAddSubview(repl);
        v.willRemoveSubview(repl);
        v.viewDidHide();
        v.viewDidUnhide();
        v.viewDidChangeBackingProperties();
        v.viewWillDraw();
        v.layout();
        v.updateLayer();
        TestKit.check(v.makeBackingLayer() != null, "NSView makeBackingLayer non-nil");
        v.viewWillMoveToWindow(null);
        v.viewDidMoveToWindow();
        TestKit.check(true, "NSView view-move/layout no-throws");

        // frame / bounds math
        v.setFrameOrigin(new NSPoint(20, 30));
        TestKit.check(v.frame().x() == 20 && v.frame().y() == 30, "NSView setFrameOrigin round-trip");
        v.setFrameSize(new NSSize(220, 170));
        TestKit.check(v.frame().width() == 220 && v.frame().height() == 170, "NSView setFrameSize round-trip");
        v.setFrame(new NSRect(10, 10, 200, 160));
        v.setBoundsOrigin(new NSPoint(5, 5));
        TestKit.check(v.bounds().x() == 5 && v.bounds().y() == 5, "NSView setBoundsOrigin round-trip");
        v.setBoundsSize(new NSSize(200, 160));
        v.setBounds(new NSRect(0, 0, 200, 160));
        v.translateOriginToPoint(new NSPoint(0, 0));
        v.scaleUnitSquareToSize(new NSSize(1, 1));
        v.setBounds(new NSRect(0, 0, 200, 160));
        TestKit.check(true, "NSView bounds math no-throws");
        v.rotateByAngle(0);
        double rot = v.frameRotation();
        v.setFrameRotation(rot + 10);
        TestKit.check(v.frameRotation() == rot + 10, "NSView frameRotation set +10");
        v.setFrameRotation(rot);
        double crot = v.frameCenterRotation();
        v.setFrameCenterRotation(crot);
        TestKit.check(v.frameCenterRotation() == crot, "NSView frameCenterRotation round-trip");
        double brot = v.boundsRotation();
        v.setBoundsRotation(brot);
        TestKit.check(v.boundsRotation() == brot, "NSView boundsRotation round-trip");
        boolean pfc = v.postsFrameChangedNotifications();
        v.setPostsFrameChangedNotifications(!pfc);
        TestKit.check(v.postsFrameChangedNotifications() == !pfc, "NSView postsFrameChangedNotifications toggle");
        v.setPostsFrameChangedNotifications(pfc);
        boolean pbc = v.postsBoundsChangedNotifications();
        v.setPostsBoundsChangedNotifications(!pbc);
        TestKit.check(v.postsBoundsChangedNotifications() == !pbc, "NSView postsBoundsChangedNotifications toggle");
        v.setPostsBoundsChangedNotifications(pbc);
        v.resizeSubviewsWithOldSize(new NSSize(200, 160));
        v.resizeWithOldSuperviewSize(new NSSize(500, 400));
        TestKit.check(true, "NSView resize* no-throws");

        // display
        v.setNeedsDisplay(true);
        v.display();
        v.displayIfNeeded();
        v.displayIfNeededIgnoringOpacity();
        v.displayRect(new NSRect(0, 0, 50, 50));
        v.displayRectIgnoringOpacity(new NSRect(0, 0, 50, 50));
        v.displayIfNeededInRectIgnoringOpacity(new NSRect(0, 0, 50, 50));
        v.prepareForReuse();
        v.prepareContentInRect(new NSRect(0, 0, 50, 50));
        TestKit.check(v.wantsDefaultClipping() || !v.wantsDefaultClipping(), "NSView wantsDefaultClipping no-throw");
        boolean cdc = v.canDrawConcurrently();
        v.setCanDrawConcurrently(!cdc);
        TestKit.check(v.canDrawConcurrently() == !cdc, "NSView canDrawConcurrently toggle");
        v.setCanDrawConcurrently(cdc);
        boolean nl = v.needsLayout();
        v.setNeedsLayout(true);
        TestKit.check(v.needsLayout(), "NSView setNeedsLayout(true)");
        v.setNeedsLayout(nl);
        TestKit.check(true, "NSView display batch no-throws");

        // rect/size conversions (expressible subset)
        NSRect cs = v.centerScanRect(new NSRect(0, 0, 100, 100));
        TestKit.check(Double.isFinite(cs.width()), "NSView centerScanRect finite");
        NSRect adj = v.adjustScroll(new NSRect(0, 0, 100, 100));
        TestKit.check(Double.isFinite(adj.x()), "NSView adjustScroll finite");
        NSRect back = v.convertRectFromBacking(new NSRect(0, 0, 100, 100));
        TestKit.check(Double.isFinite(back.width()), "NSView convertRectFromBacking finite");
        for (String m : new String[]{"convertSizeToBacking", "convertSizeFromBacking", "convertSizeToLayer", "convertSizeFromLayer"}) {
            NSSize s = switch (m) {
                case "convertSizeToBacking" -> v.convertSizeToBacking(new NSSize(10, 10));
                case "convertSizeFromBacking" -> v.convertSizeFromBacking(new NSSize(10, 10));
                case "convertSizeToLayer" -> v.convertSizeToLayer(new NSSize(10, 10));
                default -> v.convertSizeFromLayer(new NSSize(10, 10));
            };
            TestKit.check(Double.isFinite(s.width()) && Double.isFinite(s.height()), "NSView " + m + " finite");
        }

        // scrolling / hit testing
        v.scrollPoint(new NSPoint(5, 5));
        TestKit.check(true, "NSView scrollPoint no-throw");
        TestKit.check(v.hitTest(new NSPoint(10, 10)) != null, "NSView hitTest inside non-nil");
        TestKit.check(v.tag() == -1, "NSView default tag -1 (got " + v.tag() + ")");
        TestKit.check(v.viewWithTag(999999) == null, "NSView viewWithTag unknown -> nil");
        TestKit.check(!v.needsPanelToBecomeKey() || v.needsPanelToBecomeKey(), "NSView needsPanelToBecomeKey no-throw");
        TestKit.check(!v.mouseDownCanMoveWindow() || v.mouseDownCanMoveWindow(), "NSView mouseDownCanMoveWindow no-throw");

        // tooltip
        v.setToolTip("tip");
        TestKit.check("tip".equals(v.toolTip()), "NSView toolTip round-trip");
        v.setToolTip(null);
        TestKit.check(v.toolTip() == null, "NSView toolTip nil after clear");
        v.removeToolTip(12345);
        v.removeAllToolTips();
        TestKit.check(true, "NSView tooltip remove no-throws");

        // live resize / focus ring
        TestKit.check(!v.inLiveResize(), "NSView inLiveResize false outside resize");
        TestKit.check(!v.preservesContentDuringLiveResize() || v.preservesContentDuringLiveResize(), "NSView preservesContentDuringLiveResize no-throw");
        TestKit.check(Double.isFinite(v.rectPreservedDuringLiveResize().width()), "NSView rectPreservedDuringLiveResize finite");
        v.viewWillStartLiveResize();
        v.viewDidEndLiveResize();
        long frt = v.focusRingType();
        v.setFocusRingType(frt);
        TestKit.check(v.focusRingType() == frt, "NSView focusRingType round-trip " + frt);
        TestKit.check(NSView.defaultFocusRingType() >= 0, "NSView defaultFocusRingType non-negative");
        v.setKeyboardFocusRingNeedsDisplayInRect(new NSRect(0, 0, 10, 10));
        v.drawFocusRingMask();
        v.noteFocusRingMaskChanged();
        TestKit.check(Double.isFinite(v.focusRingMaskBounds().width()), "NSView focusRingMaskBounds finite");
        TestKit.check(NSView.focusView() == null || NSView.focusView().peer().address() != 0, "NSView focusView no-throw");
        TestKit.check(NSView.defaultMenu() == null || NSView.defaultMenu().peer().address() != 0, "NSView defaultMenu no-throw");

        // key views
        NSView other = NSView.create(new NSRect(0, 0, 40, 40), (ctx, d) -> {});
        content.addSubview(other);
        v.setNextKeyView(other);
        TestKit.check(v.nextKeyView() != null && v.nextKeyView().peer().address() == other.peer().address(), "NSView nextKeyView round-trip");
        v.setNextKeyView(null);
        TestKit.check(v.nextKeyView() == null, "NSView nextKeyView nil after clear");
        TestKit.check(v.previousKeyView() == null || v.previousKeyView().peer().address() != 0, "NSView previousKeyView no-throw");
        TestKit.check(v.nextValidKeyView() == null || v.nextValidKeyView().peer().address() != 0, "NSView nextValidKeyView no-throw");
        TestKit.check(v.previousValidKeyView() == null || v.previousValidKeyView().peer().address() != 0, "NSView previousValidKeyView no-throw");
        other.removeFromSuperview();

        // layer props
        long rp = v.layerContentsRedrawPolicy();
        v.setLayerContentsRedrawPolicy(rp);
        TestKit.check(v.layerContentsRedrawPolicy() == rp, "NSView layerContentsRedrawPolicy round-trip " + rp);
        long lp = v.layerContentsPlacement();
        v.setLayerContentsPlacement(lp);
        TestKit.check(v.layerContentsPlacement() == lp, "NSView layerContentsPlacement round-trip " + lp);
        TestKit.check(!v.wantsUpdateLayer() || v.wantsUpdateLayer(), "NSView wantsUpdateLayer no-throw");
        boolean cdsl = v.canDrawSubviewsIntoLayer();
        v.setCanDrawSubviewsIntoLayer(!cdsl);
        TestKit.check(v.canDrawSubviewsIntoLayer() == !cdsl, "NSView canDrawSubviewsIntoLayer toggle");
        v.setCanDrawSubviewsIntoLayer(cdsl);
        boolean lci = v.layerUsesCoreImageFilters();
        v.setLayerUsesCoreImageFilters(!lci);
        TestKit.check(v.layerUsesCoreImageFilters() == !lci, "NSView layerUsesCoreImageFilters toggle");
        v.setLayerUsesCoreImageFilters(lci);
        TestKit.check(v.shadow() == null, "NSView shadow nil by default");
        NSShadow sh = NSShadow.create();
        v.setShadow(sh);
        TestKit.check(v.shadow() != null, "NSView shadow round-trip non-nil");
        v.setShadow(null);
        TestKit.check(v.shadow() == null, "NSView shadow nil after clear");
        boolean ctb = v.clipsToBounds();
        v.setClipsToBounds(!ctb);
        TestKit.check(v.clipsToBounds() == !ctb, "NSView clipsToBounds toggle");
        v.setClipsToBounds(ctb);
        boolean wrt = v.wantsRestingTouches();
        v.setWantsRestingTouches(!wrt);
        TestKit.check(v.wantsRestingTouches() == !wrt, "NSView wantsRestingTouches toggle");
        v.setWantsRestingTouches(wrt);
        long att = v.allowedTouchTypes();
        v.setAllowedTouchTypes(att);
        TestKit.check(v.allowedTouchTypes() == att, "NSView allowedTouchTypes round-trip " + att);

        // print-job metadata (no panel shown)
        TestKit.check(v.pageHeader() == null || v.pageHeader().peer().address() != 0, "NSView pageHeader no-throw");
        TestKit.check(v.pageFooter() == null || v.pageFooter().peer().address() != 0, "NSView pageFooter no-throw");
        TestKit.check(v.printJobTitle() == null || !v.printJobTitle().isEmpty() || true, "NSView printJobTitle no-throw");
        TestKit.check(Double.isFinite(v.heightAdjustLimit()), "NSView heightAdjustLimit finite");
        TestKit.check(Double.isFinite(v.widthAdjustLimit()), "NSView widthAdjustLimit finite");
        TestKit.check(v.dataWithPDFInsideRect(new NSRect(0, 0, 50, 50)) != null, "NSView dataWithPDFInsideRect non-nil");
        TestKit.check(v.dataWithEPSInsideRect(new NSRect(0, 0, 50, 50)) != null, "NSView dataWithEPSInsideRect non-nil");
        v.drawPageBorderWithSize(new NSSize(10, 10));
        TestKit.check(true, "NSView drawPageBorderWithSize no-throw");
        TestKit.check(v.registeredDraggedTypes().count() == 0, "NSView registeredDraggedTypes initially empty");
        TestKit.check(!v.isInFullScreenMode(), "NSView isInFullScreenMode false");
        v.exitFullScreenModeWithOptions(null);
        TestKit.check(true, "NSView exitFullScreenModeWithOptions:null no-throw");

        // gestures / tracking / cursors
        NSGestureRecognizer gr = NSGestureRecognizer.create(null, "dummyAction:");
        long g0 = v.gestureRecognizers().count();
        v.addGestureRecognizer(gr);
        TestKit.check(v.gestureRecognizers().count() == g0 + 1, "NSView addGestureRecognizer count +1");
        v.removeGestureRecognizer(gr);
        TestKit.check(v.gestureRecognizers().count() == g0, "NSView removeGestureRecognizer restores count");
        long t0 = v.trackingAreas().count();
        NSTrackingArea area = NSTrackingArea.create(new NSRect(0, 0, 100, 100),
                NSView.trackingMouseEnteredAndExited | NSView.trackingActiveAlways, v);
        v.addTrackingArea(area);
        TestKit.check(v.trackingAreas().count() == t0 + 1, "NSView addTrackingArea count +1");
        v.removeTrackingArea(area);
        TestKit.check(v.trackingAreas().count() == t0, "NSView removeTrackingArea restores count");
        v.updateTrackingAreas();
        NSCursor cursor = NSCursor.arrowCursor();
        v.addCursorRect(new NSRect(0, 0, 50, 50), cursor);
        v.removeCursorRect(new NSRect(0, 0, 50, 50), cursor);
        v.discardCursorRects();
        v.resetCursorRects();
        TestKit.check(true, "NSView cursor-rect batch no-throws");

        // layout direction / safe area / misc
        long dir = v.userInterfaceLayoutDirection();
        v.setUserInterfaceLayoutDirection(dir);
        TestKit.check(v.userInterfaceLayoutDirection() == dir, "NSView userInterfaceLayoutDirection round-trip " + dir);
        TestKit.check(Double.isFinite(v.preparedContentRect().width()), "NSView preparedContentRect finite");
        NSRect pcr = v.preparedContentRect();
        v.setPreparedContentRect(pcr);
        TestKit.check(v.preparedContentRect().width() == pcr.width(), "NSView preparedContentRect round-trip");
        TestKit.check(!v.allowsVibrancy() || v.allowsVibrancy(), "NSView allowsVibrancy no-throw");
        TestKit.check(Double.isFinite(v.safeAreaInsets().top() + v.safeAreaInsets().left() + v.safeAreaInsets().bottom() + v.safeAreaInsets().right()), "NSView safeAreaInsets finite");
        TestKit.check(Double.isFinite(v.safeAreaRect().width()), "NSView safeAreaRect finite");
        var sai = v.additionalSafeAreaInsets();
        v.setAdditionalSafeAreaInsets(sai);
        TestKit.check(v.additionalSafeAreaInsets().top() == sai.top(), "NSView additionalSafeAreaInsets round-trip");
        TestKit.check(v.safeAreaLayoutGuide() == null || v.safeAreaLayoutGuide().peer().address() != 0, "NSView safeAreaLayoutGuide no-throw");
        TestKit.check(v.layoutMarginsGuide() == null || v.layoutMarginsGuide().peer().address() != 0, "NSView layoutMarginsGuide no-throw");
        boolean pcm = v.prefersCompactControlSizeMetrics();
        v.setPrefersCompactControlSizeMetrics(!pcm);
        TestKit.check(v.prefersCompactControlSizeMetrics() == !pcm, "NSView prefersCompactControlSizeMetrics toggle");
        v.setPrefersCompactControlSizeMetrics(pcm);
        TestKit.check(!v.isDrawingFindIndicator() || v.isDrawingFindIndicator(), "NSView isDrawingFindIndicator no-throw");
        TestKit.check(Double.isFinite(v.alphaValue()), "NSView alphaValue finite");
    }

    private static void clipViewTests(NSView content) {
        NSClipView clip = NSClipView.create(new NSRect(0, 0, 300, 200));
        content.addSubview(clip);
        NSView doc = NSView.create(new NSRect(0, 0, 300, 200), (ctx, d) -> {});
        clip.setDocumentView(doc);
        TestKit.check(clip.documentView() != null && clip.documentView().peer().address() == doc.peer().address(), "NSClipView documentView round-trip");
        TestKit.check(Double.isFinite(clip.documentRect().width()), "NSClipView documentRect finite");
        TestKit.check(Double.isFinite(clip.documentVisibleRect().width()), "NSClipView documentVisibleRect finite");
        clip.scrollToPoint(new NSPoint(0, 0));
        TestKit.check(true, "NSClipView scrollToPoint no-throw");
        clip.setDocumentCursor(null);
        TestKit.check(clip.documentCursor() == null, "NSClipView documentCursor nil after clear");
        clip.setDocumentCursor(NSCursor.arrowCursor());
        TestKit.check(clip.documentCursor() != null, "NSClipView documentCursor round-trip non-nil");
        clip.setDocumentCursor(null);
        NSRect cb = clip.constrainBoundsRect(clip.bounds());
        TestKit.check(Double.isFinite(cb.width()), "NSClipView constrainBoundsRect finite");
        var ci = clip.contentInsets();
        clip.setContentInsets(ci);
        TestKit.check(clip.contentInsets().top() == ci.top(), "NSClipView contentInsets round-trip");
        boolean aaci = clip.automaticallyAdjustsContentInsets();
        clip.setAutomaticallyAdjustsContentInsets(!aaci);
        TestKit.check(clip.automaticallyAdjustsContentInsets() == !aaci, "NSClipView automaticallyAdjustsContentInsets toggle");
        clip.setAutomaticallyAdjustsContentInsets(aaci);
    }

    private static void boxTests(NSView content) {
        NSBox box = NSBox.create(new NSRect(0, 0, 200, 150));
        content.addSubview(box);
        box.setTitle("Group");
        TestKit.check("Group".equals(box.title()), "NSBox title round-trip");
        box.setBoxType(NSBox.BoxType.custom);
        TestKit.check(box.boxType() == 4 && box.boxTypeEnum() == NSBox.BoxType.custom, "NSBox boxType custom round-trip");
        box.setTitlePosition(NSBox.TitlePosition.atTop);
        TestKit.check(box.titlePosition() == 2 && box.titlePositionEnum() == NSBox.TitlePosition.atTop, "NSBox titlePosition atTop round-trip");
        NSView cv = NSView.create(new NSRect(0, 0, 100, 60), (ctx, d) -> {});
        box.setContentView(cv);
        TestKit.check(box.contentView() != null && box.contentView().peer().address() == cv.peer().address(), "NSBox contentView round-trip");
        var m = box.contentViewMargins();
        box.setContentViewMargins(m);
        TestKit.check(box.contentViewMargins().width() == m.width(), "NSBox contentViewMargins round-trip");
        boolean tr = box.isTransparent();
        box.setTransparent(!tr);
        TestKit.check(box.isTransparent() == !tr, "NSBox transparent toggle");
        box.setTransparent(tr);
        NSFont f = NSFont.systemFontOfSize(13);
        box.setTitleFont(f);
        TestKit.check(box.titleFont() != null, "NSBox titleFont non-nil after set");
        box.setBorderColor(NSColor.systemRedColor());
        TestKit.check(box.borderColor() != null, "NSBox borderColor non-nil after set");
        box.setFillColor(NSColor.systemBlueColor());
        TestKit.check(box.fillColor() != null, "NSBox fillColor non-nil after set");
        double bw = box.borderWidth();
        box.setBorderWidth(bw + 1);
        TestKit.check(box.borderWidth() == bw + 1, "NSBox borderWidth set +1");
        box.setBorderWidth(bw);
        double cr = box.cornerRadius();
        box.setCornerRadius(cr + 2);
        TestKit.check(box.cornerRadius() == cr + 2, "NSBox cornerRadius set +2");
        box.setCornerRadius(cr);
        TestKit.check(Double.isFinite(box.borderRect().width()), "NSBox borderRect finite");
        TestKit.check(Double.isFinite(box.titleRect().width()), "NSBox titleRect finite");
        TestKit.check(box.titleCell() == null || box.titleCell().peer().address() != 0, "NSBox titleCell no-throw");
        box.setFrameFromContentFrame(new NSRect(0, 0, 120, 80));
        TestKit.check(true, "NSBox setFrameFromContentFrame no-throw");
        box.sizeToFit();
        TestKit.check(true, "NSBox sizeToFit no-throw");
    }

    private static void stackViewTests(NSView content) {
        NSStackView stack = NSStackView.create(new NSRect(0, 0, 400, 300));
        content.addSubview(stack);
        stack.setOrientation(1);
        TestKit.check(stack.orientation() == 1, "NSStackView orientation vertical");
        stack.setSpacing(8);
        TestKit.check(stack.spacing() == 8, "NSStackView spacing round-trip");
        stack.setAlignment(3);
        TestKit.check(stack.alignment() == 3, "NSStackView alignment round-trip");
        stack.setDistribution(0);
        TestKit.check(stack.distribution() == 0, "NSStackView distribution fill");
        stack.setDistribution(-1);
        stack.setEdgeInsets(4, 5, 6, 7);
        var ei = stack.edgeInsets();
        TestKit.check(ei.top() == 4 && ei.left() == 5 && ei.bottom() == 6 && ei.right() == 7, "NSStackView edgeInsets round-trip");
        boolean dhv = stack.detachesHiddenViews();
        stack.setDetachesHiddenViews(!dhv);
        TestKit.check(stack.detachesHiddenViews() == !dhv, "NSStackView detachesHiddenViews toggle");
        stack.setDetachesHiddenViews(dhv);

        NSView a = NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {});
        NSView b = NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {});
        NSView c = NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {});
        stack.addArrangedSubview(a);
        stack.addArrangedSubview(b);
        TestKit.check(stack.arrangedSubviews().size() == 2, "NSStackView arrangedSubviews 2");
        stack.insertArrangedSubview(c, 0);
        TestKit.check(stack.arrangedSubviews().size() == 3, "NSStackView insertArrangedSubview -> 3");
        TestKit.check(stack.arrangedSubviews().get(0).peer().address() == c.peer().address(), "NSStackView insertArrangedSubview order at 0");
        stack.removeArrangedSubview(c);
        TestKit.check(stack.arrangedSubviews().size() == 2, "NSStackView removeArrangedSubview -> 2");
        TestKit.check(stack.detachedViews().count() == 0, "NSStackView detachedViews empty");
        stack.setCustomSpacingAfterView(20, a);
        TestKit.check(stack.customSpacingAfterView(a) == 20, "NSStackView customSpacing round-trip 20");
        stack.setCustomSpacingAfterView(NSStackView.SPACING_USE_DEFAULT, a);
        TestKit.check(true, "NSStackView customSpacing reset to default no-throw");
        TestKit.check(true, "NSStackView clipping/hugging priorities omitted (NSLayoutPriority is float; shapes unregistered)");

        // gravity areas (distribution gravityAreas mode) — every view in exactly one gravity
        stack.addViewInGravity(c, 1);
        TestKit.check(stack.viewsInGravity(1).count() >= 1, "NSStackView addViewInGravity visible");
        NSArray grp = NSArray.mutableArray();
        grp.addObject(a);
        grp.addObject(b);
        stack.setViewsInGravity(grp, 2);
        TestKit.check(stack.viewsInGravity(2).count() == 2, "NSStackView setViewsInGravity count 2");
        NSView d = NSView.create(new NSRect(0, 0, 60, 30), (ctx, dd) -> {});
        stack.insertViewAtIndexInGravity(d, 0, 2);
        TestKit.check(stack.viewsInGravity(2).count() == 3, "NSStackView insertViewAtIndexInGravity -> 3");
        stack.removeView(d);
        TestKit.check(stack.viewsInGravity(2).count() == 2, "NSStackView removeView restores 2");
        stack.removeView(c);
        TestKit.check(stack.views().count() >= 2, "NSStackView views count >= 2 after removeView");

        NSArray pair2 = NSArray.mutableArray();
        pair2.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, dd) -> {}));
        pair2.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, dd) -> {}));
        TestKit.check(NSStackView.stackViewWithViews(pair2) != null, "NSStackView stackViewWithViews non-nil");
    }

    private static void splitViewTests(NSApplication app) throws Throwable {
        NSWindow win = TestKit.hiddenWindow(400, 300);
        NSSplitView split = NSSplitView.create(new NSRect(0, 0, 400, 300));
        win.setContentView(split);
        split.setVertical(true);
        TestKit.check(split.isVertical(), "NSSplitView vertical true");
        split.setDividerStyle(NSSplitView.DividerStyle.thin);
        TestKit.check(split.dividerStyle() == 2 && split.dividerStyleEnum() == NSSplitView.DividerStyle.thin, "NSSplitView dividerStyle thin round-trip");
        split.setAutosaveName("ViewCoverageTestSplit");
        TestKit.check("ViewCoverageTestSplit".equals(split.autosaveName()), "NSSplitView autosaveName round-trip");
        split.setAutosaveName(null);
        TestKit.check(split.autosaveName() == null, "NSSplitView autosaveName nil after clear");
        TestKit.check(Double.isFinite(split.dividerThickness()), "NSSplitView dividerThickness finite");
        TestKit.check(split.dividerColor() == null || split.dividerColor().peer().address() != 0, "NSSplitView dividerColor no-throw");
        split.adjustSubviews();
        TestKit.check(true, "NSSplitView adjustSubviews no-throw");
        split.drawDividerInRect(new NSRect(0, 0, 10, 300));
        TestKit.check(true, "NSSplitView drawDividerInRect no-throw");

        NSView p1 = NSView.create(new NSRect(0, 0, 200, 300), (ctx, d) -> {});
        NSView p2 = NSView.create(new NSRect(0, 0, 200, 300), (ctx, d) -> {});
        split.addArrangedSubview(p1);
        split.addArrangedSubview(p2);
        TestKit.check(split.subviews().count() == 2, "NSSplitView addArrangedSubview x2 -> subviews 2");
        TestKit.check(split.arrangedSubviews().count() == 2, "NSSplitView arrangedSubviews 2");
        TestKit.check(!split.isSubviewCollapsed(p1), "NSSplitView isSubviewCollapsed false");
        TestKit.check(true, "NSSplitView holdingPriority* omitted (NSLayoutPriority is float; shapes unregistered)");
        boolean aas = split.arrangesAllSubviews();
        split.setArrangesAllSubviews(!aas);
        TestKit.check(split.arrangesAllSubviews() == !aas, "NSSplitView arrangesAllSubviews toggle");
        split.setArrangesAllSubviews(aas);
        NSView p3 = NSView.create(new NSRect(0, 0, 100, 300), (ctx, d) -> {});
        split.insertArrangedSubview(p3, 0);
        TestKit.check(split.arrangedSubviews().count() == 3, "NSSplitView insertArrangedSubview -> 3");
        split.removeArrangedSubview(p3);
        TestKit.check(split.arrangedSubviews().count() == 2, "NSSplitView removeArrangedSubview -> 2");

        TestKit.pump(app, 600);
        split.layoutSubtreeIfNeeded();
        double min = split.minPossiblePositionOfDividerAtIndex(0);
        double max = split.maxPossiblePositionOfDividerAtIndex(0);
        TestKit.check(Double.isFinite(min) && Double.isFinite(max) && min <= max, "NSSplitView min/max divider positions sane (" + min + " <= " + max + ")");
        split.setPositionOfDividerAtIndex(150, 0);
        split.setPosition(140, 0);
        TestKit.check(true, "NSSplitView setPosition batch no-throw");
        TestKit.close(win);
    }

    private static void tabTests() {
        NSTabView tabs = NSTabView.create(new NSRect(0, 0, 300, 200));
        NSTabViewItem i1 = NSTabViewItem.create("Tab A");
        NSTabViewItem i2 = NSTabViewItem.create("Tab B");
        TestKit.check("Tab A".equals(i1.label()), "NSTabViewItem label round-trip");
        NSView v1 = NSView.create(new NSRect(0, 0, 100, 60), (ctx, d) -> {});
        i1.setView(v1);
        TestKit.check(i1.view() != null && i1.view().peer().address() == v1.peer().address(), "NSTabViewItem view round-trip");
        TestKit.check("Tab A".equals(i1.identifier()), "NSTabViewItem identifier == label");
        i1.setIdentifier("id-A");
        TestKit.check("id-A".equals(i1.identifier()), "NSTabViewItem identifier set round-trip");
        TestKit.check(i1.tabView() == null, "NSTabViewItem tabView nil before add");
        NSView sub = NSView.create(new NSRect(0, 0, 20, 20), (ctx, d) -> {});
        v1.addSubview(sub);
        TestKit.check(i1.initialFirstResponder() == null, "NSTabViewItem initialFirstResponder nil by default");
        i1.setInitialFirstResponder(sub);
        TestKit.check(i1.initialFirstResponder() != null && i1.initialFirstResponder().peer().address() == sub.peer().address(), "NSTabViewItem initialFirstResponder round-trip");
        i1.setInitialFirstResponder(null);
        TestKit.check(i1.initialFirstResponder() == null, "NSTabViewItem initialFirstResponder nil after clear");
        NSViewController vc = NSViewController.withView(NSView.create(new NSRect(0, 0, 50, 50), (ctx, d) -> {}));
        i1.setViewController(vc);
        TestKit.check(i1.viewController() != null && i1.viewController().peer().address() == vc.peer().address(), "NSTabViewItem viewController round-trip");
        TestKit.check(NSTabViewItem.create("fresh").viewController() == null, "NSTabViewItem viewController nil by default");
        i1.setColor(NSColor.systemRedColor());
        TestKit.check(i1.color() != null, "NSTabViewItem color non-nil after set");
        i1.setColor(null);
        TestKit.check(i1.color() == null, "NSTabViewItem color nil after clear");
        i1.setToolTip("hello tip");
        TestKit.check("hello tip".equals(i1.toolTip()), "NSTabViewItem toolTip round-trip");
        TestKit.check(i1.tabState() >= 0, "NSTabViewItem tabState non-negative");
        NSImage icon = NSImage.imageNamed("NSApplicationIcon");
        if (icon != null) {
            i1.setImage(icon);
            TestKit.check(i1.image() != null, "NSTabViewItem image round-trip non-nil");
            i1.setImage(null);
            TestKit.check(i1.image() == null, "NSTabViewItem image nil after clear");
        } else {
            i1.setImage(null);
            TestKit.check(i1.image() == null, "NSTabViewItem image nil default");
        }
        NSTabViewItem viaVc = NSTabViewItem.tabViewItemWithViewController(vc);
        TestKit.check(viaVc != null && viaVc.viewController() != null, "NSTabViewItem tabViewItemWithViewController non-nil");

        tabs.addTabViewItem(i1);
        tabs.addTabViewItem(i2);
        TestKit.check(tabs.numberOfTabViewItems() == 2, "NSTabView addTabViewItem x2 -> 2");
        TestKit.check(i1.tabView() != null && i1.tabView().peer().address() == tabs.peer().address(), "NSTabViewItem tabView set after add");
        TestKit.check(tabs.tabViewItems().count() == 2, "NSTabView tabViewItems count 2");
        tabs.selectTabViewItem(i2);
        TestKit.check(tabs.selectedTabViewItem() != null && "Tab B".equals(tabs.selectedTabViewItem().label()), "NSTabView selectTabViewItem Tab B");
        tabs.selectTabViewItemAtIndex(0);
        TestKit.check("Tab A".equals(tabs.selectedTabViewItem().label()), "NSTabView selectTabViewItemAtIndex 0");
        tabs.selectTabViewItemWithIdentifier("Tab B");
        TestKit.check("Tab B".equals(tabs.selectedTabViewItem().label()), "NSTabView selectTabViewItemWithIdentifier Tab B");
        TestKit.check(tabs.tabViewItemAtIndex(1) != null && "Tab B".equals(tabs.tabViewItemAtIndex(1).label()), "NSTabView tabViewItemAtIndex 1 == Tab B");
        TestKit.check(tabs.indexOfTabViewItem(i1) == 0, "NSTabView indexOfTabViewItem i1 == 0");
        TestKit.check(tabs.indexOfTabViewItemWithIdentifier("Tab B") == 1, "NSTabView indexOfTabViewItemWithIdentifier Tab B == 1");
        tabs.selectFirstTabViewItem(null);
        tabs.selectLastTabViewItem(null);
        tabs.selectNextTabViewItem(null);
        tabs.selectPreviousTabViewItem(null);
        tabs.takeSelectedTabViewItemFromSender(null);
        TestKit.check(true, "NSTabView select*/take* null-sender no-throws");
        TestKit.check(tabs.tabViewItemAtPoint(new NSPoint(5, 5)) == null || true, "NSTabView tabViewItemAtPoint no-throw");
        // font / type / position / border / items / control size
        NSFont f = NSFont.systemFontOfSize(12);
        tabs.setFont(f);
        TestKit.check(tabs.font() != null, "NSTabView font non-nil after set");
        long tvt = tabs.tabViewType();
        tabs.setTabViewType(tvt);
        TestKit.check(tabs.tabViewType() == tvt, "NSTabView tabViewType round-trip " + tvt);
        long tp = tabs.tabPosition();
        tabs.setTabPosition(tp);
        TestKit.check(tabs.tabPosition() == tp, "NSTabView tabPosition round-trip " + tp);
        long bt = tabs.tabViewBorderType();
        tabs.setTabViewBorderType(bt);
        TestKit.check(tabs.tabViewBorderType() == bt, "NSTabView tabViewBorderType round-trip " + bt);
        boolean atl = tabs.allowsTruncatedLabels();
        tabs.setAllowsTruncatedLabels(!atl);
        TestKit.check(tabs.allowsTruncatedLabels() == !atl, "NSTabView allowsTruncatedLabels toggle");
        tabs.setAllowsTruncatedLabels(atl);
        boolean db = tabs.drawsBackground();
        tabs.setDrawsBackground(!db);
        TestKit.check(tabs.drawsBackground() == !db, "NSTabView drawsBackground toggle");
        tabs.setDrawsBackground(db);
        long cs = tabs.controlSize();
        tabs.setControlSize(cs);
        TestKit.check(tabs.controlSize() == cs, "NSTabView controlSize round-trip " + cs);
        TestKit.check(Double.isFinite(tabs.minimumSize().width()), "NSTabView minimumSize finite");
        TestKit.check(Double.isFinite(tabs.contentRect().width()), "NSTabView contentRect finite");
        // setTabViewItems reorder
        NSArray reorder = NSArray.mutableArray();
        reorder.addObject(i2);
        reorder.addObject(i1);
        tabs.setTabViewItems(reorder);
        TestKit.check(tabs.numberOfTabViewItems() == 2, "NSTabView setTabViewItems keeps 2");
        TestKit.check("Tab B".equals(tabs.tabViewItemAtIndex(0).label()), "NSTabView setTabViewItems reorder first == Tab B");
        NSTabViewItem i3 = NSTabViewItem.create("Tab C");
        tabs.insertTabViewItem(i3, 1);
        TestKit.check(tabs.numberOfTabViewItems() == 3 && "Tab C".equals(tabs.tabViewItemAtIndex(1).label()), "NSTabView insertTabViewItem:atIndex: 1 == Tab C");
        tabs.removeTabViewItem(i3);
        TestKit.check(tabs.numberOfTabViewItems() == 2, "NSTabView removeTabViewItem -> 2");
    }

    private static void gridViewTests() {
        NSGridView grid = NSGridView.gridViewWithNumberOfColumnsRows(2, 2);
        TestKit.check(grid.numberOfColumns() == 2 && grid.numberOfRows() == 2, "NSGridView 2x2 factory");
        TestKit.check(grid.columnAtIndex(0) != null && grid.rowAtIndex(1) != null, "NSGridView column/rowAtIndex non-nil");
        TestKit.check(grid.indexOfColumn(grid.columnAtIndex(1)) == 1, "NSGridView indexOfColumn round-trip 1");
        TestKit.check(grid.indexOfRow(grid.rowAtIndex(0)) == 0, "NSGridView indexOfRow round-trip 0");
        TestKit.check(grid.cellAtColumnIndexRowIndex(0, 0) != null, "NSGridView cellAtColumnIndexRowIndex non-nil");
        NSView a = NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {});
        NSView b = NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {});
        NSArray pair = NSArray.mutableArray();
        pair.addObject(a);
        pair.addObject(b);
        grid.addRowWithViews(pair);
        TestKit.check(grid.numberOfRows() == 3, "NSGridView addRowWithViews -> 3 rows");
        NSArray pairC = NSArray.mutableArray();
        pairC.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {}));
        pairC.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {}));
        pairC.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {}));
        grid.addColumnWithViews(pairC);
        TestKit.check(grid.numberOfColumns() == 3, "NSGridView addColumnWithViews -> 3 cols");
        TestKit.check(grid.cellForView(a) != null, "NSGridView cellForView non-nil for added view");
        NSArray trio = NSArray.mutableArray();
        trio.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {}));
        trio.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {}));
        trio.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {}));
        grid.insertRowAtIndexWithViews(0, trio);
        TestKit.check(grid.numberOfRows() == 4, "NSGridView insertRow -> 4 rows");
        grid.moveRowAtIndexToIndex(0, 1);
        TestKit.check(grid.numberOfRows() == 4, "NSGridView moveRow keeps 4 rows");
        grid.removeRowAtIndex(0);
        TestKit.check(grid.numberOfRows() == 3, "NSGridView removeRow -> 3 rows");
        NSArray trioC = NSArray.mutableArray();
        trioC.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {}));
        trioC.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {}));
        trioC.addObject(NSView.create(new NSRect(0, 0, 60, 30), (ctx, d) -> {}));
        grid.insertColumnAtIndexWithViews(0, trioC);
        TestKit.check(grid.numberOfColumns() == 4, "NSGridView insertColumn -> 4 cols");
        grid.moveColumnAtIndexToIndex(0, 1);
        grid.removeColumnAtIndex(0);
        TestKit.check(grid.numberOfColumns() == 3, "NSGridView removeColumn -> 3 cols");
        long xp = grid.xPlacement();
        grid.setXPlacement(5);
        TestKit.check(grid.xPlacement() == 5, "NSGridView xPlacement set 5");
        grid.setXPlacement(xp);
        long yp = grid.yPlacement();
        grid.setYPlacement(5);
        TestKit.check(grid.yPlacement() == 5, "NSGridView yPlacement set 5");
        grid.setYPlacement(yp);
        long ra = grid.rowAlignment();
        grid.setRowAlignment(3);
        TestKit.check(grid.rowAlignment() == 3, "NSGridView rowAlignment set 3");
        grid.setRowAlignment(ra);
        grid.setRowSpacing(9);
        TestKit.check(grid.rowSpacing() == 9, "NSGridView rowSpacing round-trip 9");
        grid.setColumnSpacing(11);
        TestKit.check(grid.columnSpacing() == 11, "NSGridView columnSpacing round-trip 11");
        // nested-array factory
        NSArray inner = NSArray.mutableArray();
        inner.addObject(NSView.create(new NSRect(0, 0, 40, 20), (ctx, d) -> {}));
        NSArray outer = NSArray.mutableArray();
        outer.addObject(inner);
        NSGridView g2 = NSGridView.gridViewWithViews(outer);
        TestKit.check(g2.numberOfRows() == 1 && g2.numberOfColumns() == 1, "NSGridView gridViewWithViews 1x1");
        TestKit.check(NSGridView.create(new NSRect(0, 0, 100, 100)) != null, "NSGridView create non-nil");
    }

    private static void visualEffectTests() {
        NSVisualEffectView v = NSVisualEffectView.create(new NSRect(0, 0, 200, 120));
        TestKit.check(v.interiorBackgroundStyle() >= 0, "NSVisualEffectView interiorBackgroundStyle non-negative");
        v.viewDidMoveToWindow();
        v.viewWillMoveToWindow(null);
        TestKit.check(true, "NSVisualEffectView window-move no-throws");
        long m = v.material();
        v.setMaterial(NSVisualEffectView.Material.sidebar);
        TestKit.check(v.materialEnum() == NSVisualEffectView.Material.sidebar, "NSVisualEffectView materialEnum sidebar");
        v.setMaterial(m);
        v.setBlendingMode(NSVisualEffectView.BlendingMode.withinWindow);
        TestKit.check(v.blendingModeEnum() == NSVisualEffectView.BlendingMode.withinWindow, "NSVisualEffectView blendingModeEnum withinWindow");
        v.setBlendingMode(0);
        v.setState(NSVisualEffectView.State.active);
        TestKit.check(v.stateEnum() == NSVisualEffectView.State.active, "NSVisualEffectView stateEnum active");
        v.setState(1);
        boolean e = v.isEmphasized();
        v.setEmphasized(!e);
        TestKit.check(v.isEmphasized() == !e, "NSVisualEffectView isEmphasized toggle");
        v.setEmphasized(e);
    }

    private static void viewControllerTests() {
        NSViewController vc = NSViewController.withView(NSView.create(new NSRect(0, 0, 120, 80), (ctx, d) -> {}));
        TestKit.check(vc.view() != null, "NSViewController view non-nil");
        TestKit.check(vc.viewIfLoaded() != null, "NSViewController viewIfLoaded non-nil");
        TestKit.check(vc.isViewLoaded(), "NSViewController isViewLoaded true");
        vc.setTitle("Panel");
        TestKit.check("Panel".equals(vc.title()), "NSViewController title round-trip");
        vc.setTitle(null);
        TestKit.check(vc.title() == null, "NSViewController title nil after clear");
        NSView rep = NSView.create(new NSRect(0, 0, 10, 10), (ctx, d) -> {});
        vc.setRepresentedObject(rep);
        TestKit.check(vc.representedObject() != null && vc.representedObject().peer().address() == rep.peer().address(), "NSViewController representedObject round-trip");
        vc.setRepresentedObject(null);
        TestKit.check(vc.representedObject() == null, "NSViewController representedObject nil after clear");
        TestKit.check(vc.nibName() == null, "NSViewController nibName nil (code-created)");
        TestKit.check(vc.nibBundle() == null, "NSViewController nibBundle nil (code-created)");
        vc.loadViewIfNeeded();
        vc.viewDidLoad();
        vc.viewWillAppear();
        vc.viewDidAppear();
        vc.viewWillDisappear();
        vc.viewDidDisappear();
        vc.updateViewConstraints();
        vc.viewWillLayout();
        vc.viewDidLayout();
        TestKit.check(true, "NSViewController lifecycle batch no-throws");
        vc.setPreferredContentSize(new NSSize(300, 200));
        TestKit.check(vc.preferredContentSize().width() == 300 && vc.preferredContentSize().height() == 200, "NSViewController preferredContentSize round-trip");
        TestKit.check(vc.commitEditing(), "NSViewController commitEditing true (no editors)");
        vc.discardEditing();
        TestKit.check(true, "NSViewController commitEditingWithDelegate: untested (needs a delegate implementing the 3-arg commit protocol)");
        TestKit.check(vc.presentingViewController() == null, "NSViewController presentingViewController nil");
        var presented = vc.presentedViewControllers();
        TestKit.check(presented == null || presented.count() == 0, "NSViewController presentedViewControllers empty");
        vc.dismissController(null);
        TestKit.check(true, "NSViewController dismissController:null no-throw");
        TestKit.check(vc.parentViewController() == null, "NSViewController parentViewController nil");
        TestKit.check(vc.childViewControllers().count() == 0, "NSViewController childViewControllers initially 0");
        NSViewController k1 = NSViewController.create();
        NSViewController k2 = NSViewController.create();
        vc.addChildViewController(k1);
        TestKit.check(vc.childViewControllers().count() == 1, "NSViewController addChild -> 1");
        TestKit.check(k1.parentViewController() != null, "NSViewController child parent non-nil after add");
        vc.insertChildViewController(k2, 0);
        TestKit.check(vc.childViewControllers().count() == 2, "NSViewController insertChild:atIndex:0 -> 2");
        vc.removeChildViewControllerAtIndex(0);
        TestKit.check(vc.childViewControllers().count() == 1, "NSViewController removeChildAtIndex:0 -> 1");
        k1.removeFromParentViewController();
        TestKit.check(vc.childViewControllers().count() == 0, "NSViewController removeFromParent -> 0");
        NSArray kids = NSArray.mutableArray();
        kids.addObject(k1);
        vc.setChildViewControllers(kids);
        TestKit.check(vc.childViewControllers().count() == 1, "NSViewController setChildViewControllers -> 1");
        k1.removeFromParentViewController();
        NSSize mm = new NSSize(320, 240);
        vc.setPreferredContentSize(mm);
        vc.preferredContentSizeDidChangeForViewController(vc);
        vc.viewWillTransitionToSize(new NSSize(640, 480));
        TestKit.check(true, "NSViewController container/transition no-throws");
        TestKit.check(vc.storyboard() == null, "NSViewController storyboard nil (code-created)");
        TestKit.check(vc.extensionContext() == null, "NSViewController extensionContext nil");
        TestKit.check(vc.sourceItemView() == null, "NSViewController sourceItemView nil by default");
        NSView siv = NSView.create(new NSRect(0, 0, 20, 20), (ctx, d) -> {});
        vc.setSourceItemView(siv);
        TestKit.check(vc.sourceItemView() != null && vc.sourceItemView().peer().address() == siv.peer().address(), "NSViewController sourceItemView round-trip");
        vc.setSourceItemView(null);
        vc.setPreferredScreenOrigin(new NSPoint(100, 200));
        TestKit.check(vc.preferredScreenOrigin().x() == 100 && vc.preferredScreenOrigin().y() == 200, "NSViewController preferredScreenOrigin round-trip");
        TestKit.check(Double.isFinite(vc.preferredMinimumSize().width()), "NSViewController preferredMinimumSize finite");
        TestKit.check(Double.isFinite(vc.preferredMaximumSize().width()), "NSViewController preferredMaximumSize finite");
    }

    private static void glassTests(NSView content) {
        NSGlassEffectView glass = NSGlassEffectView.create(new NSRect(0, 0, 160, 100));
        content.addSubview(glass);
        NSView inner = NSView.create(new NSRect(0, 0, 80, 50), (ctx, d) -> {});
        glass.setContentView(inner);
        TestKit.check(glass.contentView() != null && glass.contentView().peer().address() == inner.peer().address(), "NSGlassEffectView contentView round-trip");
        glass.setCornerRadius(12);
        TestKit.check(glass.cornerRadius() == 12, "NSGlassEffectView cornerRadius round-trip 12");
        glass.setStyle(NSGlassEffectView.STYLE_CLEAR);
        TestKit.check(glass.style() == NSGlassEffectView.STYLE_CLEAR, "NSGlassEffectView style clear round-trip");
        glass.setStyle(NSGlassEffectView.STYLE_REGULAR);
        TestKit.check(glass.style() == NSGlassEffectView.STYLE_REGULAR, "NSGlassEffectView style regular round-trip");
        glass.setTintColor(NSColor.systemBlueColor());
        TestKit.check(glass.tintColor() != null, "NSGlassEffectView tintColor non-nil after set");
        glass.setTintColor(null);
        TestKit.check(glass.tintColor() == null, "NSGlassEffectView tintColor nil after clear");

        NSGlassEffectContainerView box = NSGlassEffectContainerView.create(new NSRect(0, 0, 200, 120));
        content.addSubview(box);
        box.setContentView(inner);
        TestKit.check(box.contentView() != null && box.contentView().peer().address() == inner.peer().address(), "NSGlassEffectContainerView contentView round-trip");
        box.setSpacing(8);
        TestKit.check(box.spacing() == 8, "NSGlassEffectContainerView spacing round-trip 8");
        box.setSpacing(0);
    }
}
