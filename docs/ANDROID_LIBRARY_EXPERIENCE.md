# Android-Native Library Experience

ANDROID-010 adds mobile package cards/hero, presentation-only humanized titles, canonical content
counts, visible Study/Lessons actions, bounded thumbnails, semantic media icons, item detail, opt-in
audio replay, and the existing editor. Raw `Audio Image` text is removed.

ANDROID-005B targets capability parity, not Desktop UI parity. Library is a touch-first offline
destination with global search, collection/package summaries, package drill-down, lazy content
rows, item cards, and a compact section editor.

Global search is debounced and cancellable, queries each installed package only through
`PackageContentBrowserQueryService`, and opens stable package/content identities. Package-local
search/filter/sort remains `PackageContentBrowserProjectionPolicy` authority. Compose never reads
persistence or media bytes.

Edits call the canonical `ContentBrowserEditService`; validation failure preserves the Android
draft and success reloads authoritative projections. SavedStateHandle contains only query and
selected IDs. Phone uses drill-down; medium/expanded windows retain bounded ANDROID-004 layouts.
Every clickable row also has a visible action path, so no capability depends on a hidden gesture.

No executable scoped-Study service exists for `StartPackageLessonStudyRequest`, and verify/upgrade
authorities are not exposed through `LearningApplicationContext`. Those flows, export document UI,
package uninstall UI, collection mutation dialogs, dedicated recommendation-backed Lesson Browser,
two-pane polish, and physical-device UAT remain explicit follow-up work; Android does not invent
fallback business logic.
