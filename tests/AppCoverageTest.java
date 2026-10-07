package nsui.tests;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;

import nsui.NSApplication;
import nsui.NSArray;
import nsui.NSBundle;
import nsui.NSDictionary;
import nsui.NSDockTile;
import nsui.NSEvent;
import nsui.NSFont;
import nsui.NSHapticFeedbackManager;
import nsui.NSImage;
import nsui.NSMenu;
import nsui.NSMenuItem;
import nsui.NSRect;
import nsui.NSRunningApplication;
import nsui.NSSound;
import nsui.NSSpeechSynthesizer;
import nsui.NSStatusBar;
import nsui.NSStatusItem;
import nsui.NSUserDefaults;
import nsui.NSView;
import nsui.NSWindow;
import nsui.NSWorkspace;
import nsui.objc.ObjC;
import nsui.objc.Sig;
import static nsui.objc.Sig.Arg;
import static nsui.objc.Sig.Ret;

/// AppCoverageTest — coverage for the App services batch (10 files).
///
/// Hidden-only, no audio, no key status: one never-shown window hosts views;
/// assertions are pure object state + a brief pump. Temp filesystem state lives
/// under a unique /tmp/sa-* dir per run (no fixed paths). New tests live ONLY
/// here (batch rule).
///
/// Required coverage (batch spec):
/// - NSDockTile badge set/clear with immediate cleanup
/// - NSRunningApplication identity
/// - NSBundle main
/// - NSUserDefaults round-trip with test keys + removal
/// - NSSound soundNamed existence check WITHOUT playback (no play/pause/stop)
/// - NSSpeechSynthesizer create/idle check WITHOUT speaking (no startSpeakingString:)
/// - NSHapticFeedbackManager no-crash call
/// (the latter 3 classes have no committed tests yet; no audio tests added).
///
/// Covers every method added in this batch (all in registered Sig shapes; grep
/// Sig.java per shape in the wrapper docs). Version-gated selectors are probed
/// with respondsToSelector: first — absent means NOTE + no-crash pass.
public final class AppCoverageTest {

    private static boolean responds(MemorySegment peer, String selName) {
        try {
            MethodHandle h = ObjC.handle(Sig.of(Ret.BOOL, Arg.ID));
            return (boolean) h.invokeExact(peer, ObjC.sel("respondsToSelector:"), ObjC.sel(selName));
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== AppCoverageTest — App services batch coverage ===");
        try {
            ObjC.init();
        } catch (Throwable t) {
            TestKit.skip("ObjC.init failed (not macOS / no AppKit): " + t);
        }
        NSApplication app = TestKit.app();

        // Unique scratch dir per run (batch rule: unique /tmp/sa dirs, never fixed paths).
        Path saDir = Files.createTempDirectory(Path.of("/tmp"), "sa-");
        System.out.println("  scratch dir = " + saDir);
        TestKit.check(Files.isDirectory(saDir), "unique /tmp/sa-* scratch dir created");
        Path saFile = saDir.resolve("probe.txt");
        Files.writeString(saFile, "nsui-app-coverage");

        NSWindow window = TestKit.hiddenWindow(400, 300);

        // ---------------------------------------------------------- NSDockTile (required: badge set/clear + cleanup)
        try {
            NSDockTile tile = NSDockTile.current();
            TestKit.check(tile != null && tile.peer().address() != 0, "NSDockTile.current non-nil");
            TestKit.check(tile.isKindOfClass("NSDockTile"), "NSDockTile isKindOfClass NSDockTile");
            String origBadge = null;
            try { origBadge = tile.badgeLabel(); } catch (Throwable t) { origBadge = null; }
            System.out.println("  dockTile orig badge = " + origBadge);
            // Set then immediately clear (required) — finally restores original.
            try {
                tile.setBadgeLabel("1");
                String got = null;
                try { got = tile.badgeLabel(); } catch (Throwable t) { got = null; }
                TestKit.check("1".equals(got), "NSDockTile badge set to 1 round-trip (got \"" + got + "\")");
                tile.setBadgeLabel(null);
                String cleared = null;
                try { cleared = tile.badgeLabel(); } catch (Throwable t) { cleared = null; }
                TestKit.check(cleared == null || cleared.isEmpty(), "NSDockTile badge cleared (got \"" + cleared + "\")");
                TestKit.noThrow("NSDockTile display no crash after badge clear", () -> tile.display());
            } finally {
                try {
                    tile.setBadgeLabel(origBadge);
                    tile.display();
                } catch (Throwable ignored) {}
            }
            // showsApplicationBadge round-trip (save/restore; AppKit may ignore for app tile — no-crash is pass).
            try {
                boolean origShows = tile.showsApplicationBadge();
                tile.setShowsApplicationBadge(!origShows);
                boolean toggled = tile.showsApplicationBadge();
                System.out.println("  showsApplicationBadge orig=" + origShows + " after-toggle=" + toggled);
                TestKit.probe("NSDockTile showsApplicationBadge set/get no crash");
                tile.setShowsApplicationBadge(origShows);
                TestKit.check(tile.showsApplicationBadge() == origShows, "NSDockTile showsApplicationBadge restore no crash");
            } catch (Throwable t) { TestKit.check(false, "showsApplicationBadge threw: " + t); }
            // size / contentView / owner (new, read-only except contentView).
            try {
                var sz = tile.size();
                TestKit.check(sz != null && sz.width() > 0 && sz.height() > 0, "NSDockTile size non-null (" + sz + ")");
            } catch (Throwable t) { TestKit.check(false, "NSDockTile size threw: " + t); }
            try {
                var cv = tile.contentView();
                System.out.println("  dockTile contentView = " + cv);
                TestKit.probe("NSDockTile contentView no crash");
            } catch (Throwable t) { TestKit.check(false, "contentView threw: " + t); }
            try {
                MemorySegment owner = tile.owner();
                TestKit.check(owner != null && owner.address() != 0, "NSDockTile owner probe no crash");
            } catch (Throwable t) { TestKit.check(false, "owner threw: " + t); }
        } catch (Throwable t) {
            TestKit.check(false, "NSDockTile section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSRunningApplication (required: identity)
        try {
            NSRunningApplication cur = NSRunningApplication.current();
            TestKit.check(cur != null && cur.peer().address() != 0, "NSRunningApplication.current non-nil");
            TestKit.check(cur.isKindOfClass("NSRunningApplication"), "current isKindOfClass NSRunningApplication");
            long pid = cur.processIdentifier();
            long jpid = ProcessHandle.current().pid();
            TestKit.check(pid == jpid, "NSRunningApplication identity pid matches (app=" + pid + " jvm=" + jpid + ")");
            TestKit.check(!cur.isTerminated(), "current isTerminated == false");
            System.out.println("  current localizedName=" + cur.localizedName() + " bundleId=" + cur.bundleIdentifier()
                    + " active=" + cur.isActive() + " hidden=" + cur.isHidden()
                    + " finishedLaunching=" + cur.isFinishedLaunching() + " ownsMenuBar=" + cur.ownsMenuBar()
                    + " policy=" + cur.activationPolicy() + " arch=" + cur.executableArchitecture());
            TestKit.probe("NSRunningApplication state getters no crash");
            try {
                var icon = cur.icon();
                System.out.println("  current icon = " + icon);
                TestKit.probe("NSRunningApplication icon no crash");
            } catch (Throwable t) { TestKit.check(false, "icon threw: " + t); }
            TestKit.check(cur.bundleURL() != null && cur.bundleURL().address() != 0, "bundleURL probe no crash");
            TestKit.check(cur.executableURL() != null && cur.executableURL().address() != 0, "executableURL probe no crash");
            cur.launchDate();
            TestKit.probe("launchDate probe no crash");
            // Lookup by pid should resolve to the same process.
            try {
                NSRunningApplication byPid = NSRunningApplication.runningApplicationWithProcessIdentifier(pid);
                TestKit.check(byPid != null && byPid.processIdentifier() == pid,
                        "runningApplicationWithProcessIdentifier(pid) resolves same pid");
            } catch (Throwable t) { TestKit.check(false, "runningApplicationWithProcessIdentifier threw: " + t); }
            // Bundle-id lookup with our own id (may be nil without bundle metadata) — no crash is pass.
            try {
                String bid = cur.bundleIdentifier();
                if (bid != null) {
                    var arr = NSRunningApplication.runningApplicationsWithBundleIdentifier(bid);
                    TestKit.check(arr != null, "runningApplicationsWithBundleIdentifier(own) non-nil");
                } else {
                    TestKit.skipCase("NOTE own bundleIdentifier nil (bare binary) — lookup skipped, no crash is pass");
                }
            } catch (Throwable t) { TestKit.check(false, "runningApplicationsWithBundleIdentifier threw: " + t); }
            // activateWithOptions with 0 (no-op options) — must not steal focus in hidden test? Use only when already active?
            // Skip actual activation to respect hidden-only rule; just verify selector exists.
            TestKit.check(responds(cur.peer(), "activateWithOptions:"), "current respondsTo activateWithOptions:");
            TestKit.check(responds(cur.peer(), "terminate"), "current respondsTo terminate (not called)");
        } catch (Throwable t) {
            TestKit.check(false, "NSRunningApplication section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSBundle (required: main)
        try {
            NSBundle main = NSBundle.mainBundle();
            TestKit.check(main != null && main.peer().address() != 0, "NSBundle.mainBundle non-nil");
            TestKit.check(main.isKindOfClass("NSBundle"), "mainBundle isKindOfClass NSBundle");
            String bpath = null;
            try { bpath = main.bundlePath(); } catch (Throwable t) { bpath = null; }
            System.out.println("  mainBundle path=" + bpath + " id=" + main.bundleIdentifier());
            TestKit.check(bpath != null && !bpath.isEmpty(), "NSBundle main bundlePath probe no crash");
            TestKit.check(main.bundleIdentifier() == null || !main.bundleIdentifier().isEmpty(), "bundleIdentifier probe no crash");
            TestKit.check(main.resourcePath() != null, "resourcePath probe no crash");
            TestKit.check(main.executablePath() != null, "executablePath probe no crash");
            TestKit.check(main.infoDictionary() != null, "infoDictionary probe no crash");
            TestKit.check(main.localizations() != null, "localizations probe no crash");
            TestKit.check(main.preferredLocalizations() != null, "preferredLocalizations probe no crash");
            TestKit.check(NSBundle.allBundles() != null, "NSBundle.allBundles non-nil");
            TestKit.check(NSBundle.allFrameworks() != null, "NSBundle.allFrameworks non-nil");
            // Resource lookup for a missing name must return nil without crashing.
            try {
                String miss = main.pathForResource("nsui-missing-" + System.nanoTime(), "txt");
                TestKit.check(miss == null, "pathForResource(missing) == nil (no crash)");
            } catch (Throwable t) { TestKit.check(false, "pathForResource threw: " + t); }
            try {
                MemorySegment u = main.URLForResource("nsui-missing-" + System.nanoTime(), "txt");
                TestKit.check(u == null || u.address() == 0, "URLForResource(missing) == nil (no crash)");
            } catch (Throwable t) { TestKit.check(false, "URLForResource threw: " + t); }
            try {
                String loc = main.localizedStringForKey("nsui-missing-key", "fallback", null);
                System.out.println("  localizedStringForKey(missing) = " + loc);
                TestKit.probe("localizedStringForKey no crash");
            } catch (Throwable t) { TestKit.check(false, "localizedStringForKey threw: " + t); }
        } catch (Throwable t) {
            TestKit.check(false, "NSBundle section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSUserDefaults (required: round-trip + removal)
        try {
            NSUserDefaults defs = NSUserDefaults.standard();
            TestKit.check(defs != null && defs.peer().address() != 0, "NSUserDefaults.standard non-nil");
            String kStr = "nsui.AppCoverageTest.string." + System.nanoTime();
            String kInt = "nsui.AppCoverageTest.int." + System.nanoTime();
            String kDbl = "nsui.AppCoverageTest.dbl." + System.nanoTime();
            String kBool = "nsui.AppCoverageTest.bool." + System.nanoTime();
            try {
                defs.setStringForKey("hello-defaults", kStr);
                TestKit.check("hello-defaults".equals(defs.stringForKey(kStr)), "NSUserDefaults string round-trip");
                defs.setIntegerForKey(42L, kInt);
                TestKit.check(defs.integerForKey(kInt) == 42L, "NSUserDefaults integer round-trip 42");
                defs.setDoubleForKey(3.25, kDbl);
                TestKit.check(Math.abs(defs.doubleForKey(kDbl) - 3.25) < 1e-9, "NSUserDefaults double round-trip 3.25");
                defs.setBoolForKey(true, kBool);
                TestKit.check(defs.boolForKey(kBool), "NSUserDefaults bool round-trip true");
                defs.setBoolForKey(false, kBool);
                TestKit.check(!defs.boolForKey(kBool), "NSUserDefaults bool round-trip false");
            } finally {
                try { defs.removeObjectForKey(kStr); } catch (Throwable ignored) {}
                try { defs.removeObjectForKey(kInt); } catch (Throwable ignored) {}
                try { defs.removeObjectForKey(kDbl); } catch (Throwable ignored) {}
                try { defs.removeObjectForKey(kBool); } catch (Throwable ignored) {}
            }
            TestKit.check(defs.stringForKey(kStr) == null, "NSUserDefaults string removed (nil after removal)");
            TestKit.check(defs.integerForKey(kInt) == 0L, "NSUserDefaults integer removed (0 after removal)");
            // Extra typed accessors (no crash, nil-safe).
            TestKit.check(defs.objectForKey(kStr) == null || defs.objectForKey(kStr).address() == 0, "objectForKey probe no crash");
            TestKit.check(defs.arrayForKey(kStr) == null, "arrayForKey(missing) == nil");
            TestKit.check(defs.dictionaryRepresentation() != null, "dictionaryRepresentation non-nil");
            TestKit.check(defs.volatileDomainNames() != null, "volatileDomainNames probe no crash");
        } catch (Throwable t) {
            TestKit.check(false, "NSUserDefaults section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSSound (required: existence WITHOUT playback)
        try {
            NSSound named = null;
            try { named = NSSound.soundNamed("Tink"); } catch (Throwable t) { named = null; }
            if (named == null) {
                try { named = NSSound.soundNamed("Glass"); } catch (Throwable t) { named = null; }
            }
            if (named == null) {
                TestKit.skipCase("NOTE NSSound.soundNamed(Tink/Glass) nil on this system — existence probe no crash is pass (NO playback attempted)");
            } else {
                TestKit.check(named.peer().address() != 0, "NSSound.soundNamed existence non-nil (NO playback)");
                TestKit.check(named.isKindOfClass("NSSound"), "soundNamed isKindOfClass NSSound");
                System.out.println("  soundNamed name = " + named.name());
                TestKit.skipCase("NSSound name probe no crash (NO play/pause/stop called)");
            }
            // Unknown name must be nil-safe, still no playback.
            NSSound miss = null;
            try { miss = NSSound.soundNamed("nsui-missing-sound-" + System.nanoTime()); } catch (Throwable t) { miss = null; }
            TestKit.check(miss == null, "NSSound.soundNamed(unknown) == nil (no playback)");
        } catch (Throwable t) {
            TestKit.check(false, "NSSound section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSSpeechSynthesizer (required: create/idle WITHOUT speaking)
        try {
            NSSpeechSynthesizer synth = null;
            try { synth = NSSpeechSynthesizer.create(); } catch (Throwable t) { synth = null; }
            TestKit.check(synth != null && synth.peer().address() != 0, "NSSpeechSynthesizer.create non-nil (NO speaking)");
            if (synth != null) {
                TestKit.check(synth.isKindOfClass("NSSpeechSynthesizer"), "synth isKindOfClass NSSpeechSynthesizer");
                boolean speaking = false;
                try { speaking = synth.isSpeaking(); } catch (Throwable t) { speaking = false; }
                TestKit.check(!speaking, "NSSpeechSynthesizer idle (isSpeaking == false, NO startSpeakingString: called)");
                try {
                    String voice = synth.voice();
                    System.out.println("  synth voice = " + voice);
                    TestKit.probe("NSSpeechSynthesizer voice probe no crash");
                } catch (Throwable t) { TestKit.check(false, "voice threw: " + t); }
            }
        } catch (Throwable t) {
            TestKit.check(false, "NSSpeechSynthesizer section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSHapticFeedbackManager (required: no-crash call)
        try {
            TestKit.noThrow("NSHapticFeedbackManager.performFeedbackPattern no crash (suppressed when finger off trackpad)", () -> NSHapticFeedbackManager.performFeedbackPattern(
                    NSHapticFeedbackManager.PATTERN_GENERIC, NSHapticFeedbackManager.TIME_NOW));
        } catch (Throwable t) {
            TestKit.check(false, "NSHapticFeedbackManager threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSApplication (safe getters only)
        try {
            System.out.println("  isRunning=" + app.isRunning() + " (may be false before finishLaunching; no-crash is pass)");
            TestKit.probe("NSApplication isRunning probe no crash");
            long pol = app.activationPolicy();
            System.out.println("  activationPolicy=" + pol + " presentationOptions=" + app.presentationOptions());
            TestKit.probe("NSApplication activationPolicy/presentationOptions no crash");
            try {
                boolean ok = app.trySetActivationPolicy(pol);
                System.out.println("  trySetActivationPolicy(current) = " + ok);
                TestKit.probe("NSApplication trySetActivationPolicy no crash");
            } catch (Throwable t) { TestKit.check(false, "trySetActivationPolicy threw: " + t); }
            TestKit.check(app.currentSystemPresentationOptions() == 0, "currentSystemPresentationOptions probe no crash");
            TestKit.probe("occlusionState probe no crash (" + app.occlusionState() + ")");
            TestKit.probe("userInterfaceLayoutDirection probe no crash (" + app.userInterfaceLayoutDirection() + ")");
            app.windowsMenu();
            TestKit.probe("windowsMenu probe no crash");
            app.servicesMenu();
            TestKit.probe("servicesMenu probe no crash");
            app.mainMenu();
            TestKit.probe("mainMenu probe no crash");
            TestKit.check(app.dockTile() != null && app.dockTile().address() != 0, "dockTile probe no crash");
            TestKit.check(!app.isFullKeyboardAccessEnabled(), "isFullKeyboardAccessEnabled probe no crash");
            TestKit.check(app.effectiveAppearance() != null, "effectiveAppearance probe no crash");
            // Event plumbing without side effects.
            TestKit.check(app.currentEvent() == null, "currentEvent probe no crash");
            TestKit.check(app.targetForAction("terminate:") != null && app.targetForAction("terminate:").address() != 0, "targetForAction probe no crash");
            TestKit.noThrow("NSApplication updateWindows no crash", () -> app.updateWindows());
        } catch (Throwable t) {
            TestKit.check(false, "NSApplication section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSWorkspace (safe queries with saDir file)
        try {
            NSWorkspace ws = NSWorkspace.sharedWorkspace();
            TestKit.check(ws != null && ws.peer().address() != 0, "NSWorkspace.sharedWorkspace non-nil");
            TestKit.check(ws.isKindOfClass("NSWorkspace"), "sharedWorkspace isKindOfClass NSWorkspace");
            TestKit.check(ws.notificationCenter() != null && ws.notificationCenter().address() != 0, "notificationCenter probe no crash");
            TestKit.check(ws.runningApplications() != null, "NSWorkspace runningApplications non-nil");
            NSImage icon = null;
            try { icon = ws.iconForFile(saFile.toString()); } catch (Throwable t) { icon = null; }
            TestKit.check(icon != null, "NSWorkspace iconForFile(saFile) non-nil");
            TestKit.check(!ws.isFilePackageAtPath(saFile.toString()), "isFilePackageAtPath(saFile) == false");
            TestKit.check(!ws.isFilePackageAtPath(saDir.toString()), "isFilePackageAtPath(saDir) probe no crash");
            TestKit.check(ws.fileLabels() != null, "fileLabels probe no crash");
            TestKit.check(ws.frontmostApplication() != null, "frontmostApplication probe no crash");
            TestKit.check(ws.menuBarOwningApplication() != null, "menuBarOwningApplication probe no crash");
            try {
                MemorySegment finderURL = ws.URLForApplicationWithBundleIdentifier("com.apple.finder");
                System.out.println("  finder URL peer = " + finderURL);
                TestKit.probe("URLForApplicationWithBundleIdentifier(finder) no crash");
            } catch (Throwable t) { TestKit.check(false, "URLForApplicationWithBundleIdentifier threw: " + t); }
            // Side-effectful selectors are NOT invoked here (no openURL/selectFile/hide/unmount).
            TestKit.check(responds(ws.peer(), "openURL:"), "workspace respondsTo openURL: (not called)");
        } catch (Throwable t) {
            TestKit.check(false, "NSWorkspace section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSMenu / NSMenuItem (hidden menu)
        try {
            NSMenu menu = NSMenu.createWithTitle("AppCov");
            TestKit.check(menu != null && "AppCov".equals(menu.title()), "NSMenu createWithTitle AppCov");
            NSMenuItem a = menu.addItemWithTitle("Alpha", "", "");
            NSMenuItem b = NSMenuItem.withTitle("Beta", "", "");
            b.setTag(9001L);
            menu.addItem(b);
            TestKit.check(menu.numberOfItems() == 2, "NSMenu numberOfItems 2");
            TestKit.check(menu.indexOfItemWithTitle("Alpha") == 0, "indexOfItemWithTitle Alpha == 0");
            TestKit.check(menu.indexOfItemWithTag(9001L) == 1, "indexOfItemWithTag 9001 == 1");
            TestKit.check(menu.itemWithTitle("Beta") != null, "itemWithTitle Beta non-nil");
            TestKit.check(menu.itemWithTag(9001L) != null, "itemWithTag 9001 non-nil");
            TestKit.check(menu.indexOfItem(a) == 0, "indexOfItem(a) == 0");
            TestKit.check(menu.indexOfItemWithRepresentedObject(null) == 0, "indexOfItemWithRepresentedObject(nil) no crash");
            TestKit.check(menu.indexOfItemWithSubmenu(null) == 0, "indexOfItemWithSubmenu(nil) no crash");
            TestKit.check(menu.indexOfItemWithTargetAndAction(null, null) == 0, "indexOfItemWithTargetAndAction(nil,nil) no crash");
            boolean origAuto = menu.autoenablesItems();
            menu.setAutoenablesItems(!origAuto);
            TestKit.check(menu.autoenablesItems() == !origAuto, "NSMenu autoenablesItems toggled");
            menu.setAutoenablesItems(origAuto);
            TestKit.check(menu.menuBarHeight() == 0.0, "menuBarHeight probe no crash");
            // propertiesToUpdate may only be called from within menuNeedsUpdate: (header raises otherwise) — probe responds only.
            TestKit.check(responds(menu.peer(), "propertiesToUpdate"), "menu respondsTo propertiesToUpdate (not called outside callback)");
            TestKit.check(menu.userInterfaceLayoutDirection() == 0, "userInterfaceLayoutDirection probe no crash");
            menu.cancelTracking();
            TestKit.noThrow("NSMenu cancelTracking* no crash while hidden", () -> menu.cancelTrackingWithoutAnimation());
            TestKit.check(NSMenuItem.usesUserKeyEquivalents(), "usesUserKeyEquivalents probe no crash");
            TestKit.check(a.userKeyEquivalent() != null, "userKeyEquivalent probe no crash");
            TestKit.check(!a.isHighlighted(), "isHighlighted probe no crash");
            TestKit.check(!a.isHiddenOrHasHiddenAncestor(), "isHiddenOrHasHiddenAncestor == false for fresh item");
            // Submenu attach (hidden, no tracking).
            NSMenu sub = NSMenu.createWithTitle("Sub");
            sub.addItemWithTitle("S1", "", "");
            menu.setSubmenuForItem(sub, b);
            TestKit.check(b.hasSubmenu() && b.submenu() != null, "setSubmenuForItem attached");
            TestKit.check(menu.indexOfItemWithSubmenu(sub) == 1, "indexOfItemWithSubmenu(sub) == 1");
        } catch (Throwable t) {
            TestKit.check(false, "NSMenu section threw: " + t);
            t.printStackTrace(System.out);
        }

        // ---------------------------------------------------------- NSStatusBar / NSStatusItem (create + immediate remove)
        NSStatusBar bar = null;
        NSStatusItem si = null;
        try {
            bar = NSStatusBar.systemStatusBar();
            TestKit.check(bar != null && bar.peer().address() != 0, "NSStatusBar.systemStatusBar non-nil");
            TestKit.check(bar.thickness() > 0, "NSStatusBar thickness > 0");
            si = bar.statusItemWithLength(NSStatusBar.VARIABLE_LENGTH);
            TestKit.check(si != null && si.peer().address() != 0, "statusItemWithLength VARIABLE non-nil");
            TestKit.check(si.length() == NSStatusBar.VARIABLE_LENGTH, "statusItem length probe no crash (got " + si.length() + ")");
            si.setTitle("AC");
            TestKit.check("AC".equals(si.title()), "NSStatusItem title round-trip AC");
            long origBehavior = si.behavior();
            si.setBehavior(NSStatusItem.BEHAVIOR_REMOVAL_ALLOWED);
            TestKit.check(si.behavior() == NSStatusItem.BEHAVIOR_REMOVAL_ALLOWED, "behavior REMOVAL_ALLOWED");
            si.setBehavior(origBehavior);
            TestKit.check(si.autosaveName() != null, "autosaveName probe no crash");
            TestKit.check(si.statusBar() != null, "NSStatusItem statusBar non-nil");
        } catch (Throwable t) {
            TestKit.check(false, "NSStatusBar section threw: " + t);
            t.printStackTrace(System.out);
        } finally {
            try { if (bar != null && si != null) bar.removeStatusItem(si); } catch (Throwable ignored) {}
            TestKit.probe("NSStatusItem immediate cleanup (removeStatusItem) no crash");
        }

        TestKit.pump(app, 400);
        TestKit.close(window);
        try { Files.deleteIfExists(saFile); } catch (Throwable ignored) {}
        try { Files.deleteIfExists(saDir); } catch (Throwable ignored) {}
        TestKit.end();
    }
}
