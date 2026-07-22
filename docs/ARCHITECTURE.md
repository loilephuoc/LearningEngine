# Architecture

## Build modules

The Gradle build has two modules:

1. Root project
   - Domain model and domain services
   - Application use cases and ports
   - Infrastructure and persistence adapters
   - JVM adapters and command-line entry points
   - Automated tests

2. `desktop`
   - Compose Desktop application
   - Depends on the root project
   - Owns presentation state, screens, components, and desktop wiring

## Dependency direction

```text
Desktop / JVM adapters
        ↓
Application use cases and ports
        ↓
Domain model and domain services

Infrastructure implements application ports.
```

Domain and application code must not depend on Compose Desktop or concrete JSON storage details.

## Main capability boundaries

The current source contains these broad areas:

- Content and learning-item domain
- Content packaging, package validation, import, registration, and media
- Content-library collections and package attachment
- Study sessions, item selection, sibling avoidance, and queue planning
- Review transitions, memory state, FSRS/scheduling, and due-state calculation
- Progress and review-history queries
- JSON-backed and in-memory persistence, mappers, stores, repositories, and transactions
- Dashboard, statistics, analytics, and scheduling diagnostics
- JVM/legacy import entry points
- Compose Desktop shell, dashboard, content library, study, review history, statistics, and settings

## Integration rule

A feature is complete only when all required layers are connected:

```text
Domain behavior (when needed)
→ application command/query/use case
→ infrastructure implementation and persistence
→ desktop/JVM adapter wiring
→ automated tests
→ user-visible flow
```

Do not add disconnected abstractions or placeholders solely to name a future capability.

## Persistence compatibility

Existing persisted data and package formats are product contracts. Changes must preserve compatibility or include explicit migration, validation, rollback considerations, and tests in the same batch.

## Platform strategy

The engine remains reusable and UI-independent. Compose Desktop is the active client and the first release target. Android, iOS, and Web are later consumers; their future needs must not force premature shared abstractions before Desktop Beta works end-to-end.
## Desktop Study accessibility presentation

Desktop Study derives screen-reader status and progress text through the pure `StudyAccessibilityPresentation` model. Compose semantics consume that model, keeping accessibility wording testable without UI instrumentation and aligned with the same `StudyUiState` that drives visible controls and keyboard shortcuts.


## Desktop Study action accessibility

`StudyActionAccessibility` is the single presentation boundary for Study control labels, shortcut hints, and screen-reader descriptions. `StudyScreen` applies those descriptions directly to retry, start, reveal, and rating controls while keyboard action resolution remains in `StudyKeyboardShortcut`.


## Desktop Study rating guidance

`StudyRatingGuidance` centralizes the learner-facing meaning of Again, Hard, Good, and Easy. `StudyScreen` renders the same ordered guidance beside the rating controls and exposes a combined semantic description, keeping scheduling intent, keyboard shortcuts, and screen-reader wording aligned through one pure presentation model.


## Accessible scheduler feedback

`StudySchedulerFeedbackAccessibility` converts the latest persisted scheduler decision into a concise, screen-reader-safe announcement. `StudyAccessibilityPresentation` includes that result when the next question becomes ready or the session completes, so keyboard-only and screen-reader users receive confirmation that the previous rating was saved together with its next interval and review time.


## Scheduler feedback card semantics

The visible `SchedulerFeedbackCard` uses the same `StudySchedulerFeedbackAccessibility` boundary as live announcements. Its merged semantic description exposes the rating, stage transition, interval, next-review time, difficulty and stability transitions, review count, and lapse count as one coherent screen-reader unit.


## Study content semantics

`StudyContentAccessibility` labels the active learning text as a Study prompt and exposes the translation only after answer reveal as a Study answer. This prevents unlabeled prompt/answer text from becoming ambiguous to screen-reader users while preserving the existing reveal boundary.


## Study focus transition boundary

`StudyFocusTransitionKey` converts the current Study state into a stable, testable focus identity. `StudyScreen` reacquires its keyboard focus whenever the phase, reviewed count, current item, or recoverable error changes. This keeps Enter, Space, and 1–4 shortcuts active after start, reveal, grade, retry, item advance, and completion transitions without coupling Compose focus behavior to scheduler internals.


## Session completion summary semantics

`StudySessionSummaryAccessibility` converts the completed Study state into one ordered semantic description containing the session title, total reviewed items, new/review split, optional lesson progress, and the Enter-key next action. The visible completion card merges descendants so screen readers hear a concise result instead of disconnected labels and numbers.


## Scheduler stage-transition presentation

`formatStudyStageTransition` is the presentation boundary for scheduler stage changes shown by Desktop Study. It converts enum-style stage identifiers into readable labels and inserts the Unicode transition arrow from UTF-8 source, preventing corrupted mojibake from leaking into visible and screen-reader scheduler feedback.


## Content Library empty-state presentation

`ContentLibraryEmptyPresentation` owns the first-run empty-state copy and semantic description. The empty card now includes a direct import action, so new users do not need to discover the same action in the header before they can create their first library.


## Recoverable Content Library load errors

`ContentLibraryLoadErrorPresentation` normalizes repository/load failures into a stable title, preserved technical detail, recovery guidance, retry label, and one assertive semantic announcement. `ContentLibraryScreen` keeps import diagnostics separate while routing `loadError` through a dedicated retry card backed by `refresh`.


## Review History accessibility presentation

`ReviewHistoryAccessibility` owns count grammar, empty-state semantics, and the ordered semantic summary for each review event. The visual card keeps its existing layout while screen readers receive one predictable sequence: rating, review time, response time, stability, and difficulty.


## Statistics accessibility presentation

`StatisticsAccessibility` normalizes statistic values and owns both per-card semantics and the ordered screen summary. Placeholder values remain visually unchanged while assistive technology receives the explicit word “Unavailable” instead of punctuation with no meaning.


## Settings accessibility presentation

`SettingsAccessibility` groups every configuration label and value into one semantic unit and builds section summaries in the same order as the visible rows. Blank values receive a stable unavailable fallback rather than becoming silent or ambiguous.


## Sidebar navigation accessibility

`SidebarAccessibility` derives the visible label, selected state, and concise spoken description for every desktop navigation destination. Sidebar entries expose tab semantics and explicitly announce the active destination without changing navigation behavior.


## Shell chrome accessibility presentation

`ShellChromeAccessibility` owns the semantic presentation of the persistent desktop header and status bar. The header is exposed as one heading, while the status bar announces engine and dashboard state in visual order with stable fallbacks for missing values.


## Dashboard summary accessibility

`DashboardAccessibility` centralizes spoken presentation for the dashboard page heading, section headings, and metric cards. Each metric is exposed as one ordered title/value/supporting-text unit, and blank metric content receives stable unavailable fallbacks.


## Dashboard visualization accessibility

`DashboardVisualizationAccessibility` centralizes chart identity, no-data presentation, and retention-gauge announcements. Visualization titles are exposed as headings, empty chart messages are grouped into one ordered semantic unit, and retention values are clamped and announced as percentages.


## Dashboard chart-data accessibility

`DashboardChartDataAccessibility` owns semantic descriptions for individual chart values and heatmap days. Forecast and scheduling rows announce labels, values, and units; memory stages announce count and percentage; heatmap cells announce a full date and review activity state.


## Lesson Browser accessibility presentation

`LessonBrowserAccessibility` centralizes semantic presentation for the library browser header, lesson summary cards, selected lesson heading, and lesson properties. Counts are normalized and pluralized, blank values receive stable fallbacks, and study availability is announced without relying on disabled-button state alone.


## Content Library screen accessibility

`ContentLibraryAccessibility` centralizes the page-header count summary, import status announcements, and section-heading wording. Counts are clamped and pluralized, import outcomes use live-region announcements, and blank messages receive stable fallbacks.


## Content Library dialog accessibility

`ContentLibraryDialogAccessibility` centralizes purpose, target context, destructive scope, package-option selection, count grammar, and blank-value fallbacks for every Content Library dialog. Dialog titles are semantic headings and attach-package choices expose selected state without relying on the visible checkmark.


## Content Library card accessibility

`ContentLibraryCardAccessibility` centralizes spoken summaries for libraries, collections, attached packages, installed packages, and label/value properties. Card headings announce identity and high-value counts or metadata while action buttons remain independently discoverable.


## Content Library action accessibility

`ContentLibraryActionAccessibility` owns contextual descriptions for global, library, collection, and package actions. Visible button text remains concise while assistive technology receives the target, result, and confirmation boundary for destructive operations.


## Content Library keyboard navigation

`ContentLibraryKeyboardShortcut` defines a pure keyboard contract for screen-level actions. Ctrl+R refreshes, Ctrl+I opens package import, and Escape unwinds lesson detail before closing the browser. Shortcuts are suspended while any Content Library dialog is visible so dialog input and dismissal remain authoritative.


## Shell-wide keyboard navigation

`ShellKeyboardShortcut` centralizes global function-key navigation, cyclic screen traversal, and destination refresh. `LearningShell` owns the single global keyboard boundary and delegates screen-specific shortcuts to child surfaces. Sidebar labels expose the same F1–F6 contract visually and semantically.


## Shared Desktop load/recovery state

`DesktopLoadState` is the cross-screen loading and failure boundary for Dashboard, Statistics, and Review History. Each view model keeps the last successful data while a refresh is in progress or fails. `DesktopLoadStateCard` provides one consistent polite loading announcement, assertive failure announcement, and retry action. The shell supplies refresh callbacks through `ContentHost`, keeping recovery owned by the corresponding view model.


## Batch63 search and discovery epic
Batch63 adds shared search normalization and result announcements, Review History query/rating/sort controls, and Lesson Browser query/translation/sort controls with deterministic pure projections and regression tests.

## Shared search refinement presentation

Desktop search surfaces derive active query, filter, and sort state through `SearchRefinementState`. `SearchRefinementPresentation` owns visible and assistive wording, while each feature maps its own default filter and sort values. Reset remains an explicit UI action that restores all three defaults together.


## Shared search empty-state recovery

Desktop search surfaces use `SearchEmptyStatePresentation` to distinguish missing source data from refinements that hide existing data. `SearchEmptyStateCard` renders Clear search and Reset view only when those operations can recover visible results, keeping visual and assistive-technology behavior consistent across Review History and Lesson Browser.

## Shared search keyboard boundary
Search keyboard intent is resolved by a pure presentation-layer contract in `desktop.ui.search`. Feature screens translate Compose key events into that contract and execute only feature-owned callbacks, keeping focus and recovery behavior consistent without coupling search state models to Compose APIs.

## Search option-group presentation

Desktop search screens use `SearchOptionGroupPresentation` and `SearchOptionGroup` for filter and sort controls. Screen-specific adapters map domain enums to stable labels, while the shared presentation validates that exactly one known option is selected and provides group-level and option-level accessibility descriptions. This keeps Lesson Browser and Review History behavior synchronized without coupling their domain filters or sorts.

## Batch68 — Accessible result status
Searchable desktop collections now expose a polite live result status that distinguishes complete collections, filtered subsets, empty matches, and truly empty sources.

## Batch69 removable search refinements

`SearchRefinementPresentation` now owns the ordered, domain-neutral actions for clearing the query, restoring the default filter, and restoring the default sort independently. `SearchRefinementBar` renders that contract while Review History and Lesson Browser map each action to their existing state callbacks. The full reset remains available as a separate atomic recovery action.

### Search match presentation
`SearchMatchPresentation` keeps query matching deterministic and UI-independent. `HighlightedSearchText` is the shared Compose renderer used by search result surfaces.

### Search scope disclosure
Desktop search surfaces use `SearchScopePresentation` and `SearchScopeCard` to disclose which fields participate in text matching and to summarize the active query, filter, and sort state. Feature adapters own their searchable-field lists so the shared UI stays domain-neutral.

## Batch72 contextual search query guidance
Desktop search fields now consume a shared pure presentation contract for placeholders, searchable examples, short-query guidance, and screen-reader wording.


## Batch73 multi-term search semantics
Desktop search now parses normalized, case-insensitive query terms through one shared boundary. Projection matching applies AND semantics across all terms in any order, while result highlighting independently marks every term and merges overlapping ranges.

## Scalable continuation architecture

The codebase is navigated by capability rather than by loading the entire repository into
one working context. `CAPABILITY_MAP.md` identifies production neighborhoods, dependencies,
and high-risk contracts. `TEST_MATRIX.md` defines the focused verification neighborhood,
while the full Gradle test suite remains the release gate.

This documentation layer does not replace source inspection. It narrows the initial scope,
then imports, call sites, composition roots, persisted contracts, and tests determine
whether the boundary must expand.

`PROJECT_HANDOFF.md` contains only the current baseline and next objective.
`CHANGELOG.md` owns detailed history. This prevents historical continuation notes from
becoming stale operational instructions.

## Unicode-safe Desktop search boundary

Desktop search canonicalizes query terms and searchable text with Unicode NFKC before
case-insensitive matching. Matching operates on the canonical representation, while a
per-character mapping preserves ranges in the original visible UTF-16 text for highlighting.

Canonicalization is implemented in the shared Desktop search package so Lesson Browser and
Review History cannot drift. It preserves diacritics semantically: canonically equivalent
composed and decomposed forms match, but accent removal is not performed.

## Package import diagnostic boundary

Non-fail-fast directory import reports failures as structured application data rather than
forcing Desktop presentation to infer error types from exception text.

Each failed candidate carries:

- the original source;
- a stable diagnostic code and failure category;
- optional package-validation issue codes;
- a user-facing message;
- an explicit recovery action.

The existing `message` field preserves the original exception message for backward compatibility; structured diagnostic and recovery fields carry the new metadata.
Successful candidates remain committed independently, while validation happens before the
candidate's repository transaction.

## OPD3 text-entry safety boundary

OPD3 JSON files are read through `JvmOpd3EntryReader` before deserialization. The reader:

- rejects directory entries where a required text file is expected;
- enforces a configurable uncompressed byte limit;
- checks both the ZIP-declared size and the bytes actually streamed;
- decodes with a strict UTF-8 decoder rather than silently replacing malformed input.

The default limit is 32 MiB per text entry. Limit and encoding failures are application-level
package import exceptions, so directory imports surface them through the structured Batch76
diagnostic path without persisting the failed candidate.

## OPD3 archive-structure safety boundary

Before either the descriptor path or bundle-content path reads a required JSON entry,
`Opd3ArchiveStructureValidator` enumerates the archive metadata once for that opened archive.
It does not read entry payloads. The validator:

- enforces a configurable maximum of 4096 entries by default while enumerating;
- accumulates declared uncompressed sizes against a configurable 512 MiB default budget,
  rejects unknown negative sizes, and checks remaining capacity without arithmetic overflow;
- rejects exact duplicate entry names;
- rejects absolute paths, backslashes, parent traversal, dot or empty path segments, leading
  or trailing separators, and surrounding path whitespace;
- applies Unicode NFKC normalization and rejects distinct names with the same logical form;
- treats case variants of `manifest.json`, `metadata.json`, `contents.json`, and
  `learning-items.json` as ambiguous rather than choosing one implicitly.

Structure and entry-count failures inherit `PackageImportException`. They therefore preserve
Batch76's `MALFORMED_PACKAGE` / `PACKAGE_MALFORMED` diagnostic behavior, occur before the
candidate transaction, and do not prevent later candidates in `importAllDetailed()`.

The total declared-size budget complements rather than replaces Batch77's 32 MiB per-text-entry
streamed limit. Structure validation remains payload-free; the entry reader still verifies the
actual bytes delivered for every required JSON entry.
