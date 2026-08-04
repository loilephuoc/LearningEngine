# Android Native Learning Experience

ANDROID-010 provides five root destinations: Home, Library, Study, Review, and Settings. Bottom
navigation is shown only at root destinations; active Study and Library drill-down remain focused.

Home projects session availability and canonical installed-package count. Unavailable actions are
hidden, and data tools are grouped in Settings. Library uses canonical installed-package and
package-browser projections. Humanized titles are presentation-only. Package, lesson, and selected
content Study delegate to `ScopedStudySessionService`; Android never creates a queue or Recall mode.

Lazy content rows keep opaque `imageRef`/`audioRef` identities. `ContentMediaStorage` resolves them;
thumbnails decode bounded off-main-thread and never enter ViewModel/SavedState. Missing media has a
semantic fallback. Audio is opt-in from detail and lifecycle-released, with no list autoplay.

Existing compact/medium/expanded policies preserve one-column phone drill-down and bounded larger
layouts without changing business behavior. Real media, TalkBack, large-font, rotation, and Study
resume remain physical-device gates whenever ADB hardware is unavailable.
