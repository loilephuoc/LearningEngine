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
