package nsui.tests;

import nsui.NSArray;
import nsui.NSFontCollection;
import nsui.NSFontDescriptor;
import nsui.NSRange;
import nsui.NSSharingService;
import nsui.NSSharingServicePicker;
import nsui.NSSpellChecker;
import nsui.objc.ObjC;

/// Committed coverage for system services: font queries, spelling, sharing.
/// Pure queries (no panels presented, no UI performed, no windows).
public final class ServicesTest {

    public static void main(String[] args) {
        System.out.println("=== ServicesTest — fonts, spelling, sharing ===");
        ObjC.init();

        try {
            // A nil query builds an empty collection (matches nothing) — query
            // with a real descriptor so matching has something to find.
            NSFontDescriptor probe = NSFontDescriptor.fontDescriptorWithNameSize("Helvetica", 12);
            TestKit.check(probe != null, "font descriptor by name/size");
            NSArray query = NSArray.mutableArray();
            query.addObject(probe);
            NSFontCollection coll = NSFontCollection.withDescriptors(query);
            TestKit.check(coll != null, "font collection with descriptor query");
            NSArray helv = coll.matchingDescriptorsForFamily("Helvetica");
            TestKit.check(helv != null && helv.count() > 0,
                    "Helvetica descriptors found (" + (helv == null ? "nil" : helv.count()) + ")");
        } catch (Throwable t) {
            TestKit.check(false, "font section threw: " + t);
        }

        try {
            NSSpellChecker checker = NSSpellChecker.shared();
            TestKit.check(checker != null, "shared spell checker");
            TestKit.check(NSSpellChecker.uniqueSpellDocumentTag() > 0, "unique spell tag positive");
            TestKit.check(checker.countWords("the quick brown fox", null) == 4, "word count == 4");
            NSRange clean = checker.checkSpelling("the quick brown fox", 0);
            TestKit.check(clean.location() == NSRange.NOT_FOUND,
                    "clean text has no misspelling (got " + clean + ")");
            NSRange bad = checker.checkSpelling("the quikc brown fox", 0);
            TestKit.check(bad.location() != NSRange.NOT_FOUND && bad.length() > 0,
                    "misspelling located (got " + bad + ")");
        } catch (Throwable t) {
            TestKit.check(false, "spell section threw: " + t);
        }

        try {
            NSSharingService mail = NSSharingService.named("com.apple.share.Mail.compose");
            if (mail == null) {
                TestKit.skipCase("mail service absent on this system (SKIP by nil)");
            } else {
                TestKit.check(mail.title() != null && !mail.title().isEmpty(),
                        "mail service title (got \"" + mail.title() + "\")");
                mail.setSubject("nsui probe");
                TestKit.check("nsui probe".equals(mail.subject()), "mail subject round-trip");
            }
            NSArray empty = NSArray.mutableArray();
            NSSharingServicePicker picker = NSSharingServicePicker.withItems(empty);
            TestKit.check(picker != null, "share picker creates (not shown)");
        } catch (Throwable t) {
            TestKit.check(false, "sharing section threw: " + t);
        }

        TestKit.end();
    }
}
