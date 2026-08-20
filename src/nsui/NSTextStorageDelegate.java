package nsui;

/// NSTextStorageDelegate — minimal Java mirror of the ObjC `NSTextStorageDelegate`
/// protocol. Only the editing-will/did hooks are exposed; everything else is optional.
///
/// Implementors are held by the native `NSTextStorage` via a raw delegate
/// pointer (see `setDelegate`). The full AppKit delegate
/// routing ( `DelegateProxy` ) can be used to forward native
/// callbacks into these Java methods when needed.
public interface NSTextStorageDelegate {

    /// `-textStorage:willProcessEditing:range:changeInLength:` — called before
    /// the text storage processes an edit. Default is no-op.
    default void textStorageWillProcessEditing(NSTextStorage textStorage, long editedMask, NSRange editedRange, long delta) {}

    /// `-textStorage:didProcessEditing:range:changeInLength:` — called after
    /// the text storage has processed an edit. Default is no-op.
    default void textStorageDidProcessEditing(NSTextStorage textStorage, long editedMask, NSRange editedRange, long delta) {}
}
