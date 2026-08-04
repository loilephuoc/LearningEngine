# Android Library Workspace

ANDROID-005 adds a thin Android workspace over existing Application services. `AndroidLibraryFacade`
uses `LibraryQueryService`, `LibraryCommandService`, `PackageContentBrowserQueryService`, and
`PackageContentBrowserProjectionPolicy`; it never reads persistence or constructs Study queues.

The root shows authoritative collections and installed packages. Package drill-down uses stable
content identities, lazy presentation, typed search/media/sort criteria, and lightweight saved
package/query state. Commands refresh from Application after completion and map typed failures
without parsing persistence data. Existing SAF import remains the only Android import path.
Live search is lifecycle-cancelled and debounced; recreation reloads a selected package by identity.

Phone uses drill-down navigation; larger windows retain ANDROID-004 bounded widths. Package export,
upgrade, uninstall, editing forms, lesson-scoped Study entry, two-pane UI, and physical-device UAT
remain follow-up gates where the existing Application authority needs additional Android wiring.
