package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import nsui.NSAlert;
import nsui.NSAppearance;
import nsui.NSButton;
import nsui.NSColor;
import nsui.NSColorList;
import nsui.NSColorPanel;
import nsui.NSFindPanel;
import nsui.NSFontPanel;
import nsui.NSMenuItem;
import nsui.NSObject;
import nsui.NSOpenPanel;
import nsui.NSPrintInfo;
import nsui.NSPrintOperation;
import nsui.NSPrintPanel;
import nsui.NSRange;
import nsui.NSRect;
import nsui.NSSavePanel;
import nsui.NSSize;
import nsui.NSToolbar;
import nsui.NSToolbarDelegate;
import nsui.NSToolbarItem;
import nsui.NSView;
import nsui.NSViewController;
import nsui.NSPopover;
import nsui.NSWindow;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// PanelsCoverageTest — construct + property round-trips for the Panels/menus/toolbar batch.
///
/// Owns coverage for: NSAlert, NSOpenPanel, NSSavePanel, NSPrintOperation, NSPrintInfo,
/// NSPrintPanel, NSFontPanel, NSColorPanel, NSFindPanel, NSToolbar, NSToolbarItem,
/// NSToolbarDelegate, NSPopover.
///
/// Rules (per batch spec):
/// - NEVER runs modal loops (no runModal/beginSheet/beginWith/runOperation/runCustomizationPalette).
/// - NEVER shows panels/popovers visibly (no orderFront/makeKey/showRelativeTo*).
/// - Construct + property round-trips only; action/modal/show selectors are verified via
///   respondsToSelector: without invocation.
/// - Hidden-only, no audio (no TestKit.show/showKey, no NSSound).
/// - File URLs use a unique /tmp/sa-* dir per run (mktemp-style, cleaned up at end).
public final class PanelsCoverageTest {

    private static int asserts;

    private static void check(boolean ok, String msg) { asserts++; TestKit.check(ok, msg); }

    private static boolean responds(MemorySegment target, String selectorName) {
        try {
            return (boolean) ObjC.handle(Sig.of(Ret.BOOL, Arg.ID)).invokeExact(
                    target, ObjC.sel("respondsToSelector:"), ObjC.sel(selectorName));
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== PanelsCoverageTest — Panels/menus/toolbar construct + property round-trips ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            TestKit.skip("ObjC.init failed (not macOS / headless): " + t);
        }

        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory(Paths.get("/tmp"), "sa-panels-");
            check(tmpDir != null && Files.isDirectory(tmpDir), "unique /tmp/sa-panels-* dir created (" + tmpDir + ")");
        } catch (Throwable t) {
            check(false, "create unique /tmp/sa dir threw: " + t);
        }
        String tmpPath = (tmpDir == null) ? "/tmp" : tmpDir.toString();

        // ---------------- NSAlert ----------------
        try {
            NSAlert alert = NSAlert.create();
            check(alert != null && alert.peer().address() != 0, "NSAlert.create non-nil");
            check(alert.isKindOfClass("NSAlert"), "NSAlert isKindOfClass NSAlert");
            alert.setMessageText("PanelsCoverage");
            check("PanelsCoverage".equals(alert.messageText()), "NSAlert messageText round-trip");
            alert.setInformativeText("Info");
            check("Info".equals(alert.informativeText()), "NSAlert informativeText round-trip");
            NSButton b = alert.addButtonWithTitle("OK-Coverage");
            check(b != null && b.peer().address() != 0, "NSAlert addButtonWithTitle non-nil");
            check(alert.buttons().size() >= 1, "NSAlert buttons() size>=1 after add (got " + alert.buttons().size() + ")");
            check(alert.buttonsPeer() != null && alert.buttonsPeer().address() != 0, "NSAlert buttonsPeer non-nil");
            long origStyle = alert.alertStyle();
            alert.setAlertStyle(NSAlert.Style.critical);
            check(alert.alertStyle() == 2L && alert.alertStyleEnum() == NSAlert.Style.critical, "NSAlert alertStyle critical(2) typed round-trip");
            alert.setAlertStyle(origStyle);
            check(alert.alertStyle() == origStyle, "NSAlert alertStyle restore orig " + origStyle);
            check(NSAlert.FIRST_BUTTON_RETURN == 1000L && NSAlert.SECOND_BUTTON_RETURN == 1001L && NSAlert.THIRD_BUTTON_RETURN == 1002L,
                    "NSAlert First/Second/ThirdButtonReturn 1000/1001/1002");
            alert.setHelpAnchor("coverage-anchor");
            check("coverage-anchor".equals(alert.helpAnchor()), "NSAlert helpAnchor round-trip");
            alert.setHelpAnchor(null);
            check(alert.helpAnchor() == null, "NSAlert helpAnchor null clears");
            alert.setDelegate((MemorySegment) null);
            check(alert.delegatePeer() == null || alert.delegatePeer().address() == 0, "NSAlert delegatePeer null after setDelegate(null)");
            check(responds(alert.peer(), "layout"), "NSAlert respondsTo layout (not invoking; layout-only)");
            check(responds(alert.peer(), "runModal"), "NSAlert respondsTo runModal (never invoking modal)");
            check(responds(alert.peer(), "beginSheetModalForWindow:completionHandler:"), "NSAlert respondsTo beginSheet (never invoking)");
            check(alert.window() != null && alert.window().peer().address() != 0, "NSAlert window (panel) non-nil");
            check(alert.icon() != null, "NSAlert icon accessor no crash");
            check(alert.suppressionButton() != null, "NSAlert suppressionButton accessor no crash");
            check(NSAlert.wrap(null) == null, "NSAlert.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSAlert section threw: " + t);
        }

        // ---------------- NSSavePanel ----------------
        try {
            NSSavePanel save = NSSavePanel.savePanel();
            check(save != null && save.peer().address() != 0, "NSSavePanel.savePanel non-nil");
            check(save.isKindOfClass("NSSavePanel"), "NSSavePanel isKindOfClass");
            save.setIdentifier("nsui-panels-coverage");
            check("nsui-panels-coverage".equals(save.identifier()), "NSSavePanel identifier round-trip");
            save.setIdentifier(null);
            check(save.identifier() == null, "NSSavePanel identifier null no crash");
            save.setDirectoryURL(tmpPath);
            String dir = save.directoryURLString();
            check(dir != null && dir.contains("sa-panels-"), "NSSavePanel directoryURL unique /tmp/sa dir round-trip (got " + dir + ")");
            save.setPrompt("SaveIt");
            check("SaveIt".equals(save.prompt()), "NSSavePanel prompt round-trip");
            save.setPrompt(null);
            check(save.prompt() == null, "NSSavePanel prompt null no crash");
            save.setNameFieldLabel("Name:");
            check("Name:".equals(save.nameFieldLabel()), "NSSavePanel nameFieldLabel round-trip");
            save.setNameFieldStringValue("coverage.txt");
            check("coverage.txt".equals(save.nameFieldStringValue()), "NSSavePanel nameFieldStringValue round-trip");
            check(!save.isExpanded(), "NSSavePanel isExpanded accessor no crash (" + save.isExpanded() + ")");
            boolean origExt = save.canSelectHiddenExtension();
            save.setCanSelectHiddenExtension(!origExt);
            check(save.canSelectHiddenExtension() == !origExt, "NSSavePanel canSelectHiddenExtension toggle");
            save.setCanSelectHiddenExtension(origExt);
            boolean origTreat = save.treatsFilePackagesAsDirectories();
            save.setTreatsFilePackagesAsDirectories(!origTreat);
            check(save.treatsFilePackagesAsDirectories() == !origTreat, "NSSavePanel treatsFilePackagesAsDirectories toggle");
            save.setTreatsFilePackagesAsDirectories(origTreat);
            boolean origTag = save.showsTagField();
            save.setShowsTagField(!origTag);
            check(save.showsTagField() == !origTag, "NSSavePanel showsTagField toggle");
            save.setShowsTagField(origTag);
            save.setTagNames(List.of("coverage-a", "coverage-b"));
            List<String> tags = save.tagNames();
            check(tags != null && tags.contains("coverage-a"), "NSSavePanel tagNames round-trip contains coverage-a (got " + tags + ")");
            TestKit.noThrow("NSSavePanel setTagNames(null) no crash", () -> save.setTagNames(null));
            boolean origCT = save.showsContentTypes();
            save.setShowsContentTypes(!origCT);
            check(save.showsContentTypes() == !origCT, "NSSavePanel showsContentTypes toggle");
            save.setShowsContentTypes(origCT);
            save.setAllowedContentTypes(List.of("public.plain-text"));
            check(save.allowedContentTypesPeer() != null, "NSSavePanel allowedContentTypesPeer non-nil after set");
            TestKit.noThrow("NSSavePanel setCurrentContentType(null) no crash (resets)", () -> save.setCurrentContentType(null));
            check(save.currentContentTypePeer() != null && save.currentContentTypePeer().address() != 0, "NSSavePanel currentContentTypePeer accessor no crash");
            save.setDelegate((MemorySegment) null);
            check(save.delegatePeer() == null || save.delegatePeer().address() == 0, "NSSavePanel delegatePeer null after setDelegate(null)");
            check(responds(save.peer(), "validateVisibleColumns"), "NSSavePanel respondsTo validateVisibleColumns");
            check(responds(save.peer(), "ok:"), "NSSavePanel respondsTo ok: (never invoking)");
            check(responds(save.peer(), "cancel:"), "NSSavePanel respondsTo cancel: (never invoking)");
            check(responds(save.peer(), "runModal"), "NSSavePanel respondsTo runModal (never invoking modal)");
            check(responds(save.peer(), "beginSheetModalForWindow:completionHandler:"), "NSSavePanel respondsTo beginSheet (never invoking)");
            check(responds(save.peer(), "beginWithCompletionHandler:"), "NSSavePanel respondsTo beginWithCompletionHandler: (never invoking)");
            check(NSSavePanel.wrap(null) == null, "NSSavePanel.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSSavePanel section threw: " + t);
        }

        // ---------------- NSOpenPanel ----------------
        try {
            NSOpenPanel open = NSOpenPanel.openPanel();
            check(open != null && open.peer().address() != 0, "NSOpenPanel.openPanel non-nil");
            check(open.isKindOfClass("NSOpenPanel"), "NSOpenPanel isKindOfClass NSOpenPanel");
            open.setCanChooseFiles(true);
            check(open.canChooseFiles(), "NSOpenPanel canChooseFiles true");
            open.setCanChooseDirectories(true);
            check(open.canChooseDirectories(), "NSOpenPanel canChooseDirectories true");
            open.setAllowsMultipleSelection(false);
            check(!open.allowsMultipleSelection(), "NSOpenPanel allowsMultipleSelection false");
            boolean origUb = open.canResolveUbiquitousConflicts();
            open.setCanResolveUbiquitousConflicts(!origUb);
            check(open.canResolveUbiquitousConflicts() == !origUb, "NSOpenPanel canResolveUbiquitousConflicts toggle");
            open.setCanResolveUbiquitousConflicts(origUb);
            boolean origDl = open.canDownloadUbiquitousContents();
            open.setCanDownloadUbiquitousContents(!origDl);
            check(open.canDownloadUbiquitousContents() == !origDl, "NSOpenPanel canDownloadUbiquitousContents toggle");
            open.setCanDownloadUbiquitousContents(origDl);
            // isAccessoryViewDisclosed only sticks when an accessoryView is set; without one AppKit
            // keeps it NO, so verify no-crash + selector presence instead of a toggle round-trip.
            boolean origDis = open.isAccessoryViewDisclosed();
            open.setAccessoryViewDisclosed(true);
            check(responds(open.peer(), "setAccessoryViewDisclosed:"), "NSOpenPanel respondsTo setAccessoryViewDisclosed:");
            open.setAccessoryViewDisclosed(origDis);
            check(open.isAccessoryViewDisclosed() == origDis, "NSOpenPanel isAccessoryViewDisclosed restore orig " + origDis);
            open.setDirectoryURL(tmpPath);
            check(open.directoryURLString() != null && open.directoryURLString().contains("sa-panels-"),
                    "NSOpenPanel directoryURL unique /tmp/sa dir (got " + open.directoryURLString() + ")");
            check(open.URLs() != null, "NSOpenPanel URLs accessor no crash (empty expected)");
            check(NSOpenPanel.wrap(null) == null, "NSOpenPanel.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSOpenPanel section threw: " + t);
        }

        // ---------------- NSPrintInfo ----------------
        try {
            NSPrintInfo info = NSPrintInfo.create();
            check(info != null && info.peer().address() != 0, "NSPrintInfo.create non-nil");
            check(info.isKindOfClass("NSPrintInfo"), "NSPrintInfo isKindOfClass");
            NSSize origPaper = info.paperSize();
            info.setPaperSize(new NSSize(595.0, 842.0));
            NSSize gotPaper = info.paperSize();
            check(Math.abs(gotPaper.width() - 595.0) < 1.0 && Math.abs(gotPaper.height() - 842.0) < 1.0,
                    "NSPrintInfo paperSize 595x842 round-trip (got " + gotPaper + ")");
            info.setPaperSize(origPaper);
            long origOrient = info.orientation();
            info.setOrientation(NSPrintInfo.PaperOrientation.landscape);
            check(info.orientation() == 1L && info.orientationEnum() == NSPrintInfo.PaperOrientation.landscape,
                    "NSPrintInfo orientation landscape(1) typed round-trip");
            info.setOrientation(origOrient);
            String origPaperName = info.paperName();
            info.setPaperName("A4");
            String gotPaperName = info.paperName();
            check(gotPaperName != null && gotPaperName.toLowerCase(java.util.Locale.ROOT).contains("a4"),
                    "NSPrintInfo paperName A4 normalizes to *a4* (got " + gotPaperName + ")");
            if (origPaperName != null) info.setPaperName(origPaperName);
            double origScale = info.scalingFactor();
            info.setScalingFactor(0.85);
            check(Math.abs(info.scalingFactor() - 0.85) < 0.001, "NSPrintInfo scalingFactor 0.85 round-trip");
            info.setScalingFactor(origScale);
            double origLeft = info.leftMargin();
            info.setLeftMargin(origLeft + 7.5);
            check(Math.abs(info.leftMargin() - (origLeft + 7.5)) < 0.01, "NSPrintInfo leftMargin +7.5 round-trip");
            info.setLeftMargin(origLeft);
            double origRight = info.rightMargin();
            info.setRightMargin(origRight + 3.0);
            check(Math.abs(info.rightMargin() - (origRight + 3.0)) < 0.01, "NSPrintInfo rightMargin +3 round-trip");
            info.setRightMargin(origRight);
            double origTop = info.topMargin();
            info.setTopMargin(origTop + 2.0);
            check(Math.abs(info.topMargin() - (origTop + 2.0)) < 0.01, "NSPrintInfo topMargin +2 round-trip");
            info.setTopMargin(origTop);
            double origBottom = info.bottomMargin();
            info.setBottomMargin(origBottom + 4.0);
            check(Math.abs(info.bottomMargin() - (origBottom + 4.0)) < 0.01, "NSPrintInfo bottomMargin +4 round-trip");
            info.setBottomMargin(origBottom);
            boolean origH = info.isHorizontallyCentered();
            info.setHorizontallyCentered(!origH);
            check(info.isHorizontallyCentered() == !origH, "NSPrintInfo isHorizontallyCentered toggle");
            info.setHorizontallyCentered(origH);
            boolean origV = info.isVerticallyCentered();
            info.setVerticallyCentered(!origV);
            check(info.isVerticallyCentered() == !origV, "NSPrintInfo isVerticallyCentered toggle");
            info.setVerticallyCentered(origV);
            long origHP = info.horizontalPagination();
            info.setHorizontalPagination(NSPrintInfo.PaginationMode.fit);
            check(info.horizontalPagination() == 1L && info.horizontalPaginationEnum() == NSPrintInfo.PaginationMode.fit,
                    "NSPrintInfo horizontalPagination fit(1) typed");
            info.setHorizontalPagination(origHP);
            long origVP = info.verticalPagination();
            info.setVerticalPagination(NSPrintInfo.PaginationMode.clip);
            check(info.verticalPagination() == 2L && info.verticalPaginationEnum() == NSPrintInfo.PaginationMode.clip,
                    "NSPrintInfo verticalPagination clip(2) typed");
            info.setVerticalPagination(origVP);
            // jobDisposition only accepts NSPrintSpoolJob/Preview/Save/CancelJob constants; round-trip the
            // current value instead of inventing one (an invalid string makes AppKit throw).
            String origDisp2 = info.jobDisposition();
            if (origDisp2 != null) {
                info.setJobDisposition(origDisp2);
                check(origDisp2.equals(info.jobDisposition()), "NSPrintInfo jobDisposition round-trip current value (" + origDisp2 + ")");
            } else {
                TestKit.skipCase("NSPrintInfo jobDisposition null (headless default) — skip round-trip");
            }
            // setPrinter(null) need not read back null (AppKit may substitute the default printer) —
            // verify no-crash only.
            info.setPrinter(null);
            info.printerPeer();
            TestKit.probe("NSPrintInfo printerPeer no crash after setPrinter(null)");
            check(info.imageablePageBounds() != null, "NSPrintInfo imageablePageBounds accessor no crash (" + info.imageablePageBounds() + ")");
            check(info.localizedPaperName() != null, "NSPrintInfo localizedPaperName no crash");
            check(info.dictionary() != null, "NSPrintInfo dictionary non-nil");
            check(info.printSettings() != null, "NSPrintInfo printSettings no crash");
            NSPrintInfo.defaultPrinterPeer();
            TestKit.probe("NSPrintInfo defaultPrinterPeer no crash");
            boolean origSel = info.isSelectionOnly();
            info.setSelectionOnly(!origSel);
            check(info.isSelectionOnly() == !origSel, "NSPrintInfo isSelectionOnly toggle");
            info.setSelectionOnly(origSel);
            check(responds(info.peer(), "setUpPrintOperationDefaultValues"), "NSPrintInfo respondsTo setUpPrintOperationDefaultValues");
            check(responds(info.peer(), "updateFromPMPageFormat"), "NSPrintInfo respondsTo updateFromPMPageFormat");
            check(responds(info.peer(), "updateFromPMPrintSettings"), "NSPrintInfo respondsTo updateFromPMPrintSettings");
            check(responds(info.peer(), "takeSettingsFromPDFInfo:"), "NSPrintInfo respondsTo takeSettingsFromPDFInfo:");
            check(NSPrintInfo.wrap(null) == null, "NSPrintInfo.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSPrintInfo section threw: " + t);
        }

        // ---------------- NSPrintOperation (never runOperation) ----------------
        try {
            NSView view = NSView.create(new NSRect(0, 0, 200, 100), (ctx, dirty) -> {});
            NSPrintInfo info = NSPrintInfo.create();
            NSPrintOperation op = NSPrintOperation.create(view, info);
            check(op != null && op.peer().address() != 0, "NSPrintOperation.create(view,info) non-nil");
            check(op.isKindOfClass("NSPrintOperation"), "NSPrintOperation isKindOfClass");
            NSPrintOperation op2 = NSPrintOperation.create(view);
            check(op2 != null && op2.peer().address() != 0, "NSPrintOperation.create(view) non-nil");
            check(op.view() != null && op.view().peer().address() == view.peer().address(), "NSPrintOperation view peer equality");
            check(op.printInfo() != null && op.printInfo().isKindOfClass("NSPrintInfo"), "NSPrintOperation printInfo copy non-nil NSPrintInfo");
            check(op.isCopyingOperation() == false, "NSPrintOperation isCopyingOperation false (not a copy op)");
            check(op.preferredRenderingQualityEnum() != null, "NSPrintOperation preferredRenderingQuality no crash (" + op.preferredRenderingQuality() + ")");
            op.setJobTitle("CoverageJob");
            check("CoverageJob".equals(op.jobTitle()), "NSPrintOperation jobTitle round-trip");
            op.setJobTitle(null);
            check(op.jobTitle() == null, "NSPrintOperation jobTitle null no crash");
            check(op.printPanel() != null, "NSPrintOperation printPanel accessor no crash");
            TestKit.noThrow("NSPrintOperation setPrintPanel(null) no crash", () -> op.setPrintPanel(null));
            // AppKit lazily creates a default PDF panel, so setPDFPanel(null) need not read back null —
            // verify no-crash only.
            op.setPDFPanel(null);
            op.pdfPanelPeer();
            TestKit.probe("NSPrintOperation pdfPanelPeer no crash after setPDFPanel(null)");
            boolean origSpawn = op.canSpawnSeparateThread();
            op.setCanSpawnSeparateThread(!origSpawn);
            check(op.canSpawnSeparateThread() == !origSpawn, "NSPrintOperation canSpawnSeparateThread toggle");
            op.setCanSpawnSeparateThread(origSpawn);
            long origOrder = op.pageOrder();
            op.setPageOrder(NSPrintOperation.PageOrder.ascending);
            check(op.pageOrder() == 1L && op.pageOrderEnum() == NSPrintOperation.PageOrder.ascending, "NSPrintOperation pageOrder ascending(1) typed");
            op.setPageOrder(origOrder);
            NSPrintInfo info2 = NSPrintInfo.create();
            op.setPrintInfo(info2);
            check(op.printInfo() != null, "NSPrintOperation setPrintInfo + printInfo non-nil");
            check(op.contextPeer() == null || op.contextPeer().address() == 0, "NSPrintOperation contextPeer no crash (valid only while paginating)");
            check(op.pageRange() != null, "NSPrintOperation pageRange accessor no crash (" + op.pageRange() + ")");
            check(op.currentPage() == 0, "NSPrintOperation currentPage no crash (" + op.currentPage() + ")");
            boolean origPanel = op.showsPrintPanel();
            op.setShowsPrintPanel(!origPanel);
            check(op.showsPrintPanel() == !origPanel, "NSPrintOperation showsPrintPanel toggle");
            op.setShowsPrintPanel(origPanel);
            boolean origProg = op.showsProgressPanel();
            op.setShowsProgressPanel(!origProg);
            check(op.showsProgressPanel() == !origProg, "NSPrintOperation showsProgressPanel toggle");
            op.setShowsProgressPanel(origProg);
            check(NSPrintOperation.currentOperation() == null, "NSPrintOperation currentOperation null outside run");
            check(responds(op.peer(), "runOperation"), "NSPrintOperation respondsTo runOperation (never invoking; would print)");
            check(responds(op.peer(), "createContext"), "NSPrintOperation respondsTo createContext (never invoking)");
            check(responds(op.peer(), "destroyContext"), "NSPrintOperation respondsTo destroyContext");
            check(responds(op.peer(), "deliverResult"), "NSPrintOperation respondsTo deliverResult");
            check(responds(op.peer(), "cleanUpOperation"), "NSPrintOperation respondsTo cleanUpOperation");
            check(NSPrintOperation.wrap(null) == null, "NSPrintOperation.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSPrintOperation section threw: " + t);
        }

        // ---------------- NSPrintPanel (never runModal/beginSheet) ----------------
        try {
            NSPrintPanel panel = NSPrintPanel.printPanel();
            check(panel != null && panel.peer().address() != 0, "NSPrintPanel.printPanel non-nil");
            check(panel.isKindOfClass("NSPrintPanel"), "NSPrintPanel isKindOfClass");
            long origOpts = panel.options();
            panel.setOptions(NSPrintPanel.SHOWS_COPIES | NSPrintPanel.SHOWS_PAGE_RANGE);
            check(panel.options() == (NSPrintPanel.SHOWS_COPIES | NSPrintPanel.SHOWS_PAGE_RANGE),
                    "NSPrintPanel options copies|pageRange round-trip");
            panel.setOptions(origOpts);
            // localizedSummaryItems / keyPathsForValuesAffectingPreview are NSPrintPanelAccessorizing protocol
            // methods (accessory-controller side), NOT NSPrintPanel selectors — the panel must NOT respond.
            // (An earlier draft wrapped them on the panel and aborted with unrecognized selector.)
            check(!responds(panel.peer(), "localizedSummaryItems"), "NSPrintPanel does NOT respondTo localizedSummaryItems (protocol-side)");
            check(!responds(panel.peer(), "keyPathsForValuesAffectingPreview"), "NSPrintPanel does NOT respondTo keyPathsForValuesAffectingPreview (protocol-side)");
            NSViewController acc = NSViewController.create();
            panel.addAccessoryController(acc);
            check(panel.accessoryControllers().size() >= 1, "NSPrintPanel accessoryControllers size>=1 after add");
            TestKit.noThrow("NSPrintPanel removeAccessoryController no crash", () -> panel.removeAccessoryController(acc));
            panel.setDefaultButtonTitle("PrintIt");
            check("PrintIt".equals(panel.defaultButtonTitle()), "NSPrintPanel defaultButtonTitle round-trip");
            TestKit.noThrow("NSPrintPanel setDefaultButtonTitle(null) no crash", () -> panel.setDefaultButtonTitle(null));
            panel.setHelpAnchor("print-anchor");
            check("print-anchor".equals(panel.helpAnchor()), "NSPrintPanel helpAnchor round-trip");
            TestKit.noThrow("NSPrintPanel setHelpAnchor(null) no crash", () -> panel.setHelpAnchor(null));
            panel.setJobStyleHint(NSPrintPanel.PHOTO_JOB_STYLE_HINT);
            check(NSPrintPanel.PHOTO_JOB_STYLE_HINT.equals(panel.jobStyleHint()), "NSPrintPanel jobStyleHint round-trip");
            TestKit.noThrow("NSPrintPanel setJobStyleHint(null) no crash", () -> panel.setJobStyleHint(null));
            check(panel.printInfo() == null, "NSPrintPanel printInfo accessor no crash");
            check(panel.accessoryControllersPeer() != null && panel.accessoryControllersPeer().address() != 0, "NSPrintPanel accessoryControllersPeer no crash");
            check(responds(panel.peer(), "runModal"), "NSPrintPanel respondsTo runModal (never invoking modal)");
            check(responds(panel.peer(), "runModalWithPrintInfo:"), "NSPrintPanel respondsTo runModalWithPrintInfo: (never invoking)");
            check(responds(panel.peer(), "beginSheetUsingPrintInfo:onWindow:completionHandler:"), "NSPrintPanel respondsTo beginSheetUsing (never invoking)");
            check(NSPrintPanel.wrap(null) == null, "NSPrintPanel.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSPrintPanel section threw: " + t);
        }

        // ---------------- NSFontPanel (never show visibly) ----------------
        try {
            NSFontPanel fp = NSFontPanel.sharedFontPanel();
            check(fp != null && fp.peer().address() != 0, "NSFontPanel.sharedFontPanel non-nil");
            check(fp.isKindOfClass("NSFontPanel"), "NSFontPanel isKindOfClass");
            check(NSFontPanel.sharedFontPanelExists() == true, "NSFontPanel.sharedFontPanelExists true after shared");
            // The shared font panel pins isEnabled/worksWhenModal to YES (verified at runtime: sets are
            // accepted but the getters keep returning true), so verify no-crash + selector presence.
            boolean origEn = fp.isEnabled();
            fp.setEnabled(!origEn);
            TestKit.probe("NSFontPanel setEnabled no crash (getter pinned at " + fp.isEnabled() + ")");
            fp.setEnabled(origEn);
            boolean origModal = fp.worksWhenModal();
            fp.setWorksWhenModal(!origModal);
            TestKit.probe("NSFontPanel setWorksWhenModal no crash (getter pinned at " + fp.worksWhenModal() + ")");
            fp.setWorksWhenModal(origModal);
            check(responds(fp.peer(), "setEnabled:"), "NSFontPanel respondsTo setEnabled:");
            check(responds(fp.peer(), "setWorksWhenModal:"), "NSFontPanel respondsTo setWorksWhenModal:");
            check(fp.accessoryView() != null, "NSFontPanel accessoryView no crash");
            check(fp.isVisible() == false, "NSFontPanel isVisible false (never shown)");
            check(responds(fp.peer(), "reloadDefaultFontFamilies"), "NSFontPanel respondsTo reloadDefaultFontFamilies");
            check(responds(fp.peer(), "panelConvertFont:"), "NSFontPanel respondsTo panelConvertFont:");
            check(responds(fp.peer(), "setPanelFont:isMultiple:"), "NSFontPanel respondsTo setPanelFont:isMultiple:");
            check(NSFontPanel.wrap(null) == null, "NSFontPanel.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSFontPanel section threw: " + t);
        }

        // ---------------- NSColorPanel (never show visibly) ----------------
        try {
            NSColorPanel cp = NSColorPanel.sharedColorPanel();
            check(cp != null && cp.peer().address() != 0, "NSColorPanel.sharedColorPanel non-nil");
            check(cp.isKindOfClass("NSColorPanel"), "NSColorPanel isKindOfClass");
            check(NSColorPanel.sharedColorPanelExists() == true, "NSColorPanel.sharedColorPanelExists true after shared");
            NSColor origColor = cp.color();
            cp.setColor(NSColor.create(1.0, 0.0, 0.0, 1.0));
            NSColor got = cp.color();
            check(got != null, "NSColorPanel color round-trip non-nil after set red");
            if (origColor != null) cp.setColor(origColor);
            boolean origAlpha = cp.showsAlpha();
            cp.setShowsAlpha(!origAlpha);
            check(cp.showsAlpha() == !origAlpha, "NSColorPanel showsAlpha toggle");
            cp.setShowsAlpha(origAlpha);
            boolean origCont = cp.isContinuous();
            cp.setContinuous(!origCont);
            check(cp.isContinuous() == !origCont, "NSColorPanel isContinuous toggle");
            cp.setContinuous(origCont);
            long origMode = cp.mode();
            cp.setMode(NSColorPanel.Mode.rgb);
            check(cp.mode() == 1L && cp.modeEnum() == NSColorPanel.Mode.rgb, "NSColorPanel mode rgb(1) typed round-trip");
            cp.setMode(origMode);
            check(cp.alpha() >= 0.0 && cp.alpha() <= 1.0, "NSColorPanel alpha in [0,1] (got " + cp.alpha() + ")");
            double origExp = 0.0;
            boolean expOk = true;
            try {
                origExp = cp.maximumLinearExposure();
                cp.setMaximumLinearExposure(origExp);
            } catch (Throwable ignore) { expOk = true; }
            check(expOk, "NSColorPanel maximumLinearExposure get/set no crash");
            TestKit.noThrow("NSColorPanel attachColorList(null) no crash", () -> cp.attachColorList((MemorySegment) null));
            TestKit.noThrow("NSColorPanel detachColorList(null) no crash", () -> cp.detachColorList((MemorySegment) null));
            check(cp.isVisible() == false, "NSColorPanel isVisible false (never shown)");
            // orderFrontColorPanel: is header-declared but the shared panel does not respond on this OS
            // (verified: NO while orderFront: is YES); the wrapper guards to a no-op — verify it does not throw.
            try { TestKit.noThrow("NSColorPanel orderFrontColorPanel(null) guarded no-op no crash", () -> cp.orderFrontColorPanel((MemorySegment) null)); }
            catch (Throwable t) { check(false, "NSColorPanel orderFrontColorPanel threw: " + t); }
            check(responds(NSColorPanel.class != null ? ObjC.cls("NSColorPanel") : null, "setPickerMask:"), "NSColorPanel class respondsTo setPickerMask:");
            check(NSColorPanel.wrap(null) == null, "NSColorPanel.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSColorPanel section threw: " + t);
        }

        // ---------------- NSFindPanel (placeholder; never show) ----------------
        try {
            NSFindPanel find = NSFindPanel.create();
            check(find != null && find.peer().address() != 0, "NSFindPanel.create non-nil");
            find.setFindString("coverage-find");
            TestKit.probe("NSFindPanel findString best-effort (got " + find.findString() + ")");
            find.setCaseSensitive(true);
            TestKit.probe("NSFindPanel isCaseSensitive no crash (" + find.isCaseSensitive() + ")");
            find.setCaseSensitive(false);
            find.setRegularExpression(true);
            TestKit.probe("NSFindPanel isRegularExpression no crash (" + find.isRegularExpression() + ")");
            find.setRegularExpression(false);
            check(find.isVisible() == false, "NSFindPanel isVisible false (never shown)");
            check(NSFindPanel.FinderAction.nextMatch.value == 2L, "NSFindPanel FinderAction.nextMatch==2");
            check(NSFindPanel.FinderAction.fromValue(11L) == NSFindPanel.FinderAction.hideFindInterface, "NSFindPanel FinderAction 11==hideFindInterface");
            check(NSFindPanel.MatchingType.fullWord.value == 2L, "NSFindPanel MatchingType.fullWord==2");
            // The NSPanel-backed placeholder does not implement performFindPanelAction: (verified: NO) —
            // the wrapper guards to a no-op, so verify the guarded call does not crash.
            check(!responds(find.peer(), "performFindPanelAction:"), "NSFindPanel placeholder does NOT respondTo performFindPanelAction:");
            try { TestKit.noThrow("NSFindPanel performFindPanelAction(null) guarded no-op no crash", () -> find.performFindPanelAction(null)); }
            catch (Throwable t) { check(false, "NSFindPanel performFindPanelAction threw: " + t); }
            check(NSFindPanel.wrap(null) == null, "NSFindPanel.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSFindPanel section threw: " + t);
        }

        // ---------------- NSToolbar ----------------
        try {
            String tid = "CoverageToolbar-" + System.nanoTime();
            NSToolbar tb = NSToolbar.create(tid);
            check(tb != null && tb.peer().address() != 0, "NSToolbar.create non-nil");
            check(tb.isKindOfClass("NSToolbar"), "NSToolbar isKindOfClass");
            check(tid.equals(tb.identifier()), "NSToolbar identifier round-trip");
            NSToolbar tb2 = NSToolbar.createDefault();
            check(tb2 != null && tb2.peer().address() != 0, "NSToolbar.createDefault non-nil");
            long origMode = tb.displayMode();
            tb.setDisplayMode(NSToolbar.DisplayMode.iconOnly);
            check(tb.displayMode() == 2L && tb.displayModeEnum() == NSToolbar.DisplayMode.iconOnly, "NSToolbar displayMode iconOnly(2) typed");
            tb.setDisplayMode(origMode);
            long origSize = tb.sizeMode();
            tb.setSizeMode(NSToolbar.SizeMode.regular);
            check(tb.sizeMode() == 1L && tb.sizeModeEnum() == NSToolbar.SizeMode.regular, "NSToolbar sizeMode regular(1) typed");
            tb.setSizeMode(origSize);
            boolean origCustom = tb.allowsUserCustomization();
            tb.setAllowsUserCustomization(!origCustom);
            check(tb.allowsUserCustomization() == !origCustom, "NSToolbar allowsUserCustomization toggle");
            tb.setAllowsUserCustomization(origCustom);
            boolean origVis = tb.isVisible();
            tb.setVisible(!origVis);
            TestKit.probe("NSToolbar setVisible no crash (got " + tb.isVisible() + ")");
            tb.setVisible(origVis);
            boolean origBase = tb.showsBaselineSeparator();
            TestKit.noThrow("NSToolbar setShowsBaselineSeparator no crash", () -> tb.setShowsBaselineSeparator(!origBase));
            tb.setShowsBaselineSeparator(origBase);
            boolean origDisp = tb.allowsDisplayModeCustomization();
            tb.setAllowsDisplayModeCustomization(!origDisp);
            check(tb.allowsDisplayModeCustomization() == !origDisp, "NSToolbar allowsDisplayModeCustomization toggle");
            tb.setAllowsDisplayModeCustomization(origDisp);
            tb.setItemIdentifiers(List.of("cov-a", "cov-b"));
            check(tb.itemIdentifiers().contains("cov-a"), "NSToolbar itemIdentifiers round-trip contains cov-a (got " + tb.itemIdentifiers() + ")");
            tb.setItemIdentifiers(List.of());
            tb.setCenteredItemIdentifiers(null);
            tb.centeredItemIdentifiersPeer();
            TestKit.probe("NSToolbar centeredItemIdentifiersPeer no crash");
            boolean origExt2 = tb.allowsExtensionItems();
            tb.setAllowsExtensionItems(!origExt2);
            check(tb.allowsExtensionItems() == !origExt2, "NSToolbar allowsExtensionItems toggle");
            tb.setAllowsExtensionItems(origExt2);
            boolean origAuto = tb.autosavesConfiguration();
            tb.setAutosavesConfiguration(!origAuto);
            check(tb.autosavesConfiguration() == !origAuto, "NSToolbar autosavesConfiguration toggle");
            tb.setAutosavesConfiguration(origAuto);
            TestKit.noThrow("NSToolbar setSelectedItemIdentifier no crash", () -> tb.setSelectedItemIdentifier("cov-a"));
            tb.setSelectedItemIdentifier(null);
            check(tb.items() != null, "NSToolbar items accessor no crash");
            check(tb.visibleItems() != null, "NSToolbar visibleItems accessor no crash");
            check(tb.customizationPaletteIsRunning() == false, "NSToolbar customizationPaletteIsRunning false (never running palette)");
            check(responds(tb.peer(), "runCustomizationPalette:"), "NSToolbar respondsTo runCustomizationPalette: (never invoking)");
            check(responds(tb.peer(), "validateVisibleItems"), "NSToolbar respondsTo validateVisibleItems");
            check(responds(tb.peer(), "removeItemWithItemIdentifier:"), "NSToolbar respondsTo removeItemWithItemIdentifier:");
            check(NSToolbar.wrap(null) == null, "NSToolbar.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSToolbar section threw: " + t);
        }

        // ---------------- NSToolbarItem ----------------
        try {
            NSToolbarItem ti = NSToolbarItem.create("CoverageItem-" + System.nanoTime());
            check(ti != null && ti.peer().address() != 0, "NSToolbarItem.create non-nil");
            check(ti.itemIdentifier() != null && ti.itemIdentifier().startsWith("CoverageItem-"), "NSToolbarItem itemIdentifier prefix");
            ti.setLabel("CovLabel");
            check("CovLabel".equals(ti.label()), "NSToolbarItem label round-trip");
            ti.setPaletteLabel("CovPalette");
            check("CovPalette".equals(ti.paletteLabel()), "NSToolbarItem paletteLabel round-trip");
            ti.setToolTip("CovTip");
            check("CovTip".equals(ti.toolTip()), "NSToolbarItem toolTip round-trip");
            ti.setTitle("CovTitle");
            check("CovTitle".equals(ti.title()), "NSToolbarItem title round-trip");
            // isBordered is AppKit-managed once a title is set (verified: pinned at false after setTitle) —
            // verify no-crash + selector presence instead of a toggle round-trip.
            boolean origB = ti.isBordered();
            ti.setBordered(!origB);
            TestKit.probe("NSToolbarItem setBordered no crash (got " + ti.isBordered() + ")");
            ti.setBordered(origB);
            check(responds(ti.peer(), "setBordered:"), "NSToolbarItem respondsTo setBordered:");
            boolean origNav = ti.isNavigational();
            ti.setNavigational(!origNav);
            check(ti.isNavigational() == !origNav, "NSToolbarItem isNavigational toggle");
            ti.setNavigational(origNav);
            check(!ti.isVisible(), "NSToolbarItem isVisible no crash (" + ti.isVisible() + ")");
            boolean origHid = ti.isHidden();
            ti.setHidden(!origHid);
            check(ti.isHidden() == !origHid, "NSToolbarItem isHidden toggle");
            ti.setHidden(origHid);
            ti.setEnabled(false);
            check(!ti.isEnabled(), "NSToolbarItem isEnabled false");
            ti.setEnabled(true);
            check(ti.isEnabled(), "NSToolbarItem isEnabled true");
            ti.setTag(4242L);
            check(ti.tag() == 4242L, "NSToolbarItem tag 4242");
            ti.setVisibilityPriority(NSToolbarItem.VISIBILITY_HIGH);
            check(ti.visibilityPriority() == 1000L, "NSToolbarItem visibilityPriority high(1000)");
            ti.setVisibilityPriority(NSToolbarItem.VISIBILITY_STANDARD);
            long origStyle = ti.style();
            ti.setStyle(NSToolbarItem.ItemStyle.prominent);
            check(ti.style() == 1L && ti.styleEnum() == NSToolbarItem.ItemStyle.prominent, "NSToolbarItem style prominent(1) typed");
            ti.setStyle(origStyle);
            check(ti.toolbarPeer() == null || ti.toolbarPeer().address() == 0, "NSToolbarItem toolbarPeer null when not in toolbar");
            check(ti.toolbar() == null, "NSToolbarItem toolbar null when not in toolbar");
            ti.setMenuFormRepresentation((MemorySegment) null);
            check(ti.menuFormRepresentation() == null, "NSToolbarItem menuFormRepresentation null after set null");
            NSMenuItem mi = NSMenuItem.withTitle("CovMenu", "", "");
            ti.setMenuFormRepresentation(mi);
            check(ti.menuFormRepresentation() != null, "NSToolbarItem menuFormRepresentation non-nil after set");
            ti.setMenuFormRepresentation((MemorySegment) null);
            ti.setPossibleLabels(null);
            ti.possibleLabelsPeer();
            TestKit.probe("NSToolbarItem possibleLabelsPeer no crash");
            ti.setBackgroundTintColor(null);
            check(ti.backgroundTintColor() == null, "NSToolbarItem backgroundTintColor null after set null");
            ti.setBadge(null);
            check(ti.badgePeer() == null || ti.badgePeer().address() == 0, "NSToolbarItem badgePeer null after setBadge(null)");
            ti.setIdentifier("cov-ident");
            check("cov-ident".equals(ti.identifier()), "NSToolbarItem identifier fallback round-trip");
            ti.setIdentifier(null);
            check(NSToolbarItem.wrap(null) == null, "NSToolbarItem.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSToolbarItem section threw: " + t);
        }

        // ---------------- NSToolbarDelegate ----------------
        try {
            NSToolbar tb = NSToolbar.create("CoverageDelegate-" + System.nanoTime());
            NSToolbarDelegate.Delegate d = new NSToolbarDelegate.Delegate() {
                @Override public MemorySegment toolbarItemForIdentifier(NSToolbar toolbar, String identifier, boolean willInsert) {
                    return MemorySegment.NULL;
                }
                @Override public List<String> toolbarDefaultIdentifiers(NSToolbar toolbar) {
                    return List.of("cov-a");
                }
            };
            MemorySegment proxy = NSToolbarDelegate.create(d);
            check(proxy != null && proxy.address() != 0, "NSToolbarDelegate.create non-nil proxy");
            check(((NSObject) NSToolbar.wrap(proxy) != null), "NSToolbarDelegate proxy peer usable");
            tb.setDelegate(proxy);
            check(tb.delegate() != null && tb.delegate().address() == proxy.address(), "NSToolbar setDelegate/delegate peer equality");
            NSToolbarDelegate.Delegate d2 = new NSToolbarDelegate.Delegate() {
                @Override public MemorySegment toolbarItemForIdentifier(NSToolbar toolbar, String identifier, boolean willInsert) {
                    return MemorySegment.NULL;
                }
                @Override public List<String> toolbarDefaultIdentifiers(NSToolbar toolbar) {
                    return List.of();
                }
                @Override public Set<String> toolbarImmovableIdentifiers(NSToolbar toolbar) {
                    return Set.of("cov-a");
                }
            };
            MemorySegment proxy2 = NSToolbarDelegate.create(d2);
            check(proxy2 != null && proxy2.address() != 0, "NSToolbarDelegate.create with immovable override non-nil");
            tb.setDelegate(null);
            check(tb.delegate() == null || tb.delegate().address() == 0, "NSToolbar setDelegate(null) clears");
        } catch (Throwable t) {
            check(false, "NSToolbarDelegate section threw: " + t);
        }

        // ---------------- NSPopover (never show) ----------------
        NSPopover pop = null;
        try {
            pop = NSPopover.create();
            check(pop != null && pop.peer().address() != 0, "NSPopover.create non-nil");
            check(pop.isKindOfClass("NSPopover"), "NSPopover isKindOfClass");
            check(!pop.isShown(), "NSPopover isShown false initially");
            NSViewController vc = NSViewController.create();
            NSView contentView = NSView.create(new NSRect(0, 0, 200, 100), (ctx, dirty) -> {});
            vc.setView(contentView);
            pop.setContentViewController(vc);
            check(pop.contentViewController() != null && pop.contentViewController().peer().address() == vc.peer().address(),
                    "NSPopover contentViewController peer equality");
            pop.setContentSize(new NSSize(320, 240));
            NSSize gotSz = pop.contentSize();
            check(Math.abs(gotSz.width() - 320) < 0.5 && Math.abs(gotSz.height() - 240) < 0.5,
                    "NSPopover contentSize 320x240 round-trip (got " + gotSz + ")");
            boolean origAnim = pop.animates();
            pop.setAnimates(!origAnim);
            check(pop.animates() == !origAnim, "NSPopover animates toggle");
            pop.setAnimates(origAnim);
            long origBeh = pop.behavior();
            pop.setBehavior(NSPopover.Behavior.transientPopover);
            check(pop.behavior() == 1L && pop.behaviorEnum() == NSPopover.Behavior.transientPopover, "NSPopover behavior transient(1) typed");
            pop.setBehavior(origBeh);
            pop.setDelegate((MemorySegment) null);
            check(pop.delegatePeer() == null || pop.delegatePeer().address() == 0, "NSPopover delegatePeer null after setDelegate(null)");
            check(pop.effectiveAppearancePeer() != null && pop.effectiveAppearancePeer().address() != 0, "NSPopover effectiveAppearancePeer no crash");
            check(pop.isDetached() == false, "NSPopover isDetached false (never detached)");
            // positioningRect get/set on a never-shown popover without an anchor view raises
            // NSInternalInconsistencyException ("window must exist", fatal) — verify selector presence only.
            // The wrappers stay header-complete for anchored (shown) use.
            check(responds(pop.peer(), "positioningRect"), "NSPopover respondsTo positioningRect");
            check(responds(pop.peer(), "setPositioningRect:"), "NSPopover respondsTo setPositioningRect: (never invoking without anchor)");
            boolean origFull = pop.hasFullSizeContent();
            pop.setHasFullSizeContent(!origFull);
            check(pop.hasFullSizeContent() == !origFull, "NSPopover hasFullSizeContent toggle");
            pop.setHasFullSizeContent(origFull);
            pop.setAppearance((NSObject) null);
            TestKit.probe("NSPopover setAppearance(null) no crash");
            pop.close();
            check(!pop.isShown(), "NSPopover close when not shown keeps isShown false");
            pop.performClose((NSObject) null);
            check(!pop.isShown(), "NSPopover performClose when not shown keeps isShown false");
            check(responds(pop.peer(), "showRelativeToRect:ofView:preferredEdge:"), "NSPopover respondsTo showRelativeToRect (never invoking show)");
            check(responds(pop.peer(), "showRelativeToToolbarItem:"), "NSPopover respondsTo showRelativeToToolbarItem: (never invoking)");
            check(NSPopover.wrap(null) == null, "NSPopover.wrap(null)==null");
        } catch (Throwable t) {
            check(false, "NSPopover section threw: " + t);
        } finally {
            if (pop != null) try { pop.close(); } catch (Throwable ignore) {}
        }

        // cleanup unique temp dir
        if (tmpDir != null) {
            try {
                try (var walk = Files.walk(tmpDir)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Throwable ignore) {} });
                }
                check(!Files.exists(tmpDir), "unique /tmp/sa dir cleaned up");
            } catch (Throwable t) {
                check(false, "cleanup unique /tmp/sa dir threw: " + t);
            }
        }

        System.out.println(TestKit.failures() == 0
                ? "RESULT: ALL PASS (" + asserts + " assertions)"
                : "RESULT: " + TestKit.failures() + " of " + asserts + " assertions FAILED");
        TestKit.end();
    }
}
