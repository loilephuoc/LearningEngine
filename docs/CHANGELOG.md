# Changelog

## Batch84 — Non-destructive corrupt persistence reads

- Verified that corrupt persisted bytes remain unchanged after repeated failed reads through
  newly created JSON store instances.
- Verified that failed reads do not update the target timestamp or create recovery artifacts.
- Confirmed stable failure kind and record/file context across restart-equivalent store
  recreation without leaking persisted values in messages.
- Kept recovery deliberately observational: no automatic rewrite, delete, quarantine, or
  reset behavior was introduced.

## Batch83 — Classified corrupt JSON persistence reads

- Kept a missing persistence file as the only implicit empty-store initialization state.
- Rejected existing blank or whitespace-only files instead of silently treating possible
  truncation as an empty dataset.
- Added stable `BLANK`, `MALFORMED`, `TRUNCATED`, and `INVALID_SHAPE` failure kinds while
  retaining the established exception message and original serializer cause.
- Added record-type and file-path context without exposing persisted content in diagnostics.
- Applied the shared read boundary to every JSON store.

## Batch82 — Contextual required OPD3 JSON value shapes

- Routed invalid optional metadata value shapes through `InvalidPackageJsonException`.
- Preserved `metadata.json` entry context and the original JSON accessor message.
- Verified wrong manifest, metadata, contents, and learning-item root-field shapes through one
  focused regression matrix.
- Preserved omitted optional metadata compatibility and all established identity-validation
  messages.
- Completed the Package Import & OPD3 Robustness track after archive, text, required-entry,
  JSON, validation, diagnostic, transaction, restart, and Desktop flow hardening.
- Selected corrupt/interrupted persisted-data recovery as the next Milestone 5 capability area.

## Batch81 — Contextual malformed required OPD3 JSON

- Added `InvalidPackageJsonException` with structured required-entry context.
- Applied the boundary to descriptor manifest decoding and all four bundle JSON inputs.
- Preserved the original parser message exactly as the exception and Batch76 failure message.
- Kept manifest compatibility and metadata identity validation outside the parse wrapper so
  established validation messages remain unchanged.
- Preserved `MALFORMED_PACKAGE`, `PACKAGE_MALFORMED`, failure-before-persistence, and detailed
  directory continuation behavior.
- Added focused coverage for each required entry, descriptor decoding, diagnostic mapping, and
  parser-message compatibility.

## Batch80 — Shared missing required-entry contract

- Added `MissingRequiredPackageEntryException` with structured entry-name context.
- Routed modern bundle missing-file failures through the package-import exception hierarchy.
- Made existing manifest and legacy-content exceptions specialized subtypes of the shared
  contract without changing their messages.
- Preserved the modern bundle `IllegalArgumentException` type relationship and exact legacy
  `Missing package file: <name>` message.
- Preserved Batch76 malformed-package classification, diagnostic code, non-fail-fast behavior,
  and failure-before-persistence boundary.
- Added focused exception, descriptor, bundle, routing, and message compatibility coverage.

## Batch79 — Bounded total OPD3 uncompressed size

- Extended the shared pre-read archive validator with a configurable total declared
  uncompressed-size budget and a 512 MiB default.
- Accumulated sizes from ZIP metadata without opening or reading entry payloads.
- Rejected unknown negative declared sizes and used remaining-budget checks to avoid overflow.
- Added a package-import exception for archives exceeding the total budget while preserving
  `MALFORMED_PACKAGE`, `PACKAGE_MALFORMED`, and the legacy failure message field.
- Added exact-limit, cumulative-over-limit, single-entry-over-limit, configuration, and
  diagnostic regression coverage.
- Kept Batch77's actual streamed-byte and strict UTF-8 enforcement unchanged.

## Batch78 — OPD3 archive structure integrity validation

- Established Batch77 at `a618893` as the verified baseline for archive-structure hardening.
- Added one shared pre-read structure validator to both modern OPD3 descriptor and
  bundle-content paths.
- Rejected unsafe path forms, exact duplicates, Unicode-normalized collisions, and
  case-ambiguous required JSON entries before reading or deserializing package text.
- Added a configurable maximum archive-entry count with a default of 4096 and metadata-only
  enforcement during archive enumeration.
- Kept structure failures on the package-import exception path, preserving Batch76's
  `MALFORMED_PACKAGE` and `PACKAGE_MALFORMED` diagnostics and non-fail-fast directory import.
- Added focused boundary, read-order, persistence-safety, and batch-continuation regression
  tests while retaining Batch77 size-limit and strict UTF-8 coverage.
- Identified total declared uncompressed archive-size enforcement as the preferred bounded
  capability for Batch79.

## Batch77 — Bounded and strict OPD3 text entry reading

- Added a configurable 32 MiB default limit for each OPD3 text entry.
- Added declared-size and streamed-byte enforcement to prevent unbounded archive reads.
- Added strict UTF-8 decoding so malformed package text is rejected deterministically.
- Treated directory entries as missing text files instead of reading them as empty content.
- Added package-import exceptions for oversized entries and invalid text encoding.
- Added focused tests for exact-limit reads, oversized entries, malformed UTF-8, missing
  entries, and directory entries.

## Batch76 — Actionable package import diagnostics

- Added stable package-import failure categories and diagnostic codes.
- Preserved validation issue codes without requiring UI text parsing.
- Added recovery guidance for invalid data, duplicate identity, file access, malformed
  packages, and unexpected failures.
- Preserved the exact legacy failure-message contract while adding structured diagnostic
  and recovery fields.
- Updated detailed directory import to create diagnostics through one shared classifier.
- Added focused tests for classification, fallback behavior, validation details, and the
  non-fail-fast service boundary.

## Batch75 — Unicode-robust Desktop search

- Added one shared Unicode canonicalization boundary for Desktop search.
- Matched canonically equivalent composed and decomposed diacritics.
- Matched compatibility forms such as full-width Latin characters.
- Preserved highlight ranges against the original visible text even when normalization
  changes UTF-16 length.
- Applied the same normalization to query parsing, duplicate-term removal, matching, and
  highlighting.
- Added focused regression coverage for canonical equivalence, compatibility width, and
  decomposed-grapheme highlighting.

## Batch74 — Scalable continuation context

- Rebased the canonical continuation state on verified Batch73 commit `8b8baaa`.
- Replaced the accumulated historical handoff with one concise current-state contract.
- Reorganized the roadmap around product milestones instead of batch-by-batch narration.
- Added a capability map for selective, dependency-aware source loading.
- Added a test matrix that connects capability changes to focused regression coverage.
- Added an explicit batch-planning policy for coherent 8–15-file vertical slices.
- Documented how capability context, source inspection, and full `clean test` work together.
- No production behavior or persisted-data contract is changed by this increment.

## Batch67 - Accessible search option groups

- Replaces duplicated filter and sort chip rows with one shared search-option group component.
- Announces each group heading, selected option, option count, and activation intent to assistive technology.
- Adds deterministic presentation contracts for Lesson Browser and Review History filter/sort controls.
- Adds regression tests for shared validation and screen-specific selected-option mapping.

## Batch65 — Actionable search empty-state recovery

- Added one shared empty-result presentation contract for Desktop search surfaces.
- Added accessible Clear search and Reset view actions only when each action can recover results.
- Distinguished genuinely empty data from query/filter-produced empty results.
- Wired the shared recovery card into Review History and Lesson Browser.
- Added shared and screen-specific regression tests for recovery availability and wording.

## Batch63 — Desktop search and discovery epic

- Added reusable search field, normalization, summaries, filters, and deterministic projections.
- Added Review History search, rating filters, sorting, no-result recovery, and view-model actions.
- Added Lesson Browser search, translation filters, sorting, no-result recovery, and view-model actions.
- Added broad projection, presentation, normalization, and state contract tests.

## Batch62 — Desktop UX recovery-state epic

- Added one shared loading, ready, and failed state contract for Desktop data screens.
- Added a reusable loading/error card with polite loading announcements and assertive failure announcements.
- Added direct retry actions for Dashboard, Statistics, and Review History.
- Preserved the last successful data when a refresh fails instead of replacing it with placeholders.
- Normalized unexpected exception messages into stable user-facing failure details.
- Routed screen-specific recovery callbacks through ContentHost and LearningShell.
- Added cross-screen tests for presentation wording, default loading state, fallback messages, retry availability, and stale-data preservation.
- Updated architecture, roadmap, changelog, and handoff for the new epic-sized batch policy.

## Batch61 — Shell-wide keyboard navigation epic

- Added direct F1–F6 navigation for all six Desktop destinations.
- Added Ctrl+PageUp and Ctrl+PageDown cyclic screen traversal.
- Added Ctrl+Shift+R refresh for the active data-backed screen.
- Centralized destination refresh behavior in the shell instead of refreshing unrelated screens.
- Added stable shell focus and one documented global keyboard surface.
- Exposed destination shortcuts in both visible sidebar labels and screen-reader descriptions.
- Added pure shortcut-routing tests plus cyclic NavigationState coverage.

## Batch60 — Content Library keyboard navigation

- Added Ctrl+R refresh and Ctrl+I package-import shortcuts.
- Added hierarchical Escape navigation that clears lesson detail before closing the lesson browser.
- Suspended screen-level shortcuts while any Content Library dialog is visible.
- Added a visible and screen-reader-readable shortcut hint to the Content Library header.
- Added focused tests for modifier requirements, Escape precedence, dialog isolation, and the documented shortcut contract.

## Batch59 — Content Library action descriptions

- Added contextual screen-reader descriptions for refresh, import, and retry actions.
- Added target-aware descriptions for opening libraries and creating collections.
- Added target-aware descriptions for attaching, renaming, deleting, and detaching.
- Explicitly announced confirmation boundaries for destructive collection and package actions.
- Added stable fallback wording for blank library, collection, and package names.
- Added focused presentation tests for global, contextual, destructive, and fallback action speech.

## Batch58 — Content Library card semantics

- Added ordered semantic summaries for library cards with normalized content, learning-item, and collection counts.
- Added collection summaries that distinguish empty and populated package attachment states.
- Added attached-package summaries with optional version and format metadata.
- Added installed-package summaries with version, format, and normalized library count.
- Grouped every package property into one label-and-value semantic unit.
- Added stable fallback wording for blank names and metadata.
- Added focused presentation tests for counts, attachment states, optional metadata, and property fallbacks.

## Batch57 — Content Library dialog semantics

- Added ordered purpose-and-context announcements to create, rename, delete, attach, and detach dialogs.
- Exposed every dialog title as a semantic heading.
- Exposed attach-package choices with explicit selected state and spoken package identity.
- Added destructive-scope wording for collection deletion and package detachment.
- Added normalized count grammar and stable fallbacks for blank library, collection, and package names.
- Added focused tests for all dialog summaries and package-option selection states.

## Batch56 — Content Library screen semantics

- Exposed the Content Library page title and normalized counts as one semantic heading.
- Added polite import-status and assertive import-error announcements.
- Added stable fallback wording for blank import messages.
- Exposed Libraries and Installed Packages labels as semantic section headings.
- Added tests for count normalization, pluralization, live-message wording, and section labels.

## Batch55 — Lesson Browser semantics

- Exposed the Lesson Browser library header as one semantic heading with normalized item count.
- Grouped lesson title, hierarchy, type, and learning-item count into one ordered card announcement.
- Exposed the selected lesson heading together with study availability.
- Grouped every lesson property into one label-and-value semantic unit.
- Added stable fallback wording for blank names, titles, types, labels, and values.
- Added focused presentation tests for pluralization, hierarchy, availability, and fallback behavior.

## Batch54 — Dashboard chart-data semantics

- Grouped forecast rows into label, review count, and unit announcements.
- Grouped scheduling-pressure rows into label, card count, and unit announcements.
- Grouped memory-stage legend entries into count-and-percentage announcements.
- Added full-date review activity descriptions to individual heatmap cells.
- Added stable fallbacks for blank chart labels and units.
- Added tests for chart values, percentages, dates, pluralization, zero activity, and future dates.

## Batch53 — Dashboard visualization semantics

- Added semantic identity and heading treatment to visualization cards.
- Added an explicit no-data announcement when a visualization has no data.
- Grouped empty chart title and description into one ordered semantic unit.
- Added a percentage announcement for the retention gauge with clamped values.
- Added stable fallbacks for blank visualization, empty-state, and retention labels.
- Added focused presentation tests for all new accessibility behavior.

## Batch52 — Dashboard summary semantics

- Exposed the Dashboard page header as one semantic heading.
- Exposed every Dashboard section header as one title-and-description heading.
- Grouped each metric title, value, and supporting text into one ordered semantic unit.
- Added stable fallback wording for blank metric values and details.
- Added tests for metric ordering, fallback wording, section headings, and page heading.

## Batch51 — Shell chrome semantics

- Exposed the persistent application header as one semantic heading.
- Grouped product name and edition into one concise header announcement.
- Grouped engine and dashboard status into one ordered status announcement.
- Added stable visible and spoken fallbacks for blank status values.
- Added tests for header, normal status, and blank-status presentation.

## Batch50 — Sidebar navigation semantics

- Added explicit tab semantics to every desktop sidebar destination.
- Added selected-state semantics for the active destination.
- Added concise destination descriptions while preserving visible labels and navigation behavior.
- Added tests for active, inactive, and label-preservation presentation.

## Batch49 — Settings semantic configuration summaries

- Added one merged semantic description for every Settings property row.
- Added ordered section summaries matching the visible configuration order.
- Added a stable unavailable fallback for blank configuration values.
- Added tests for property semantics, fallback wording, and section ordering.

## Batch48 — Statistics semantic summaries

- Added one merged semantic description for each statistic card.
- Added one ordered screen-level summary matching the visible metric order.
- Announces `--` and blank metric values as **Unavailable** without changing the visual placeholder.
- Added tests for normal, placeholder, blank, and full-summary presentation.

## Batch47 — Review History semantic reading order

- Added correct singular and plural grammar for the Review History count.
- Added one merged semantic description for the empty state.
- Added one ordered semantic summary for every review event card.
- Added tests for count grammar, empty-state guidance, and review metric order.

## Batch46 — Recoverable Content Library load errors

- Added a dedicated presentation boundary for Content Library load failures.
- Preserves the underlying failure detail while adding concrete local-data recovery guidance.
- Added a direct **Retry** action wired to Content Library refresh.
- Announces the complete load error and recovery path as an assertive semantic region.
- Added tests for real messages, blank-message fallback, and semantic wording.

## Batch45 — Actionable Content Library empty state

- Added a dedicated empty-state presentation model for the Content Library.
- Added a direct **Import First Package** action inside the empty card.
- Added a merged semantic description explaining the empty state and recovery action.
- Added tests for first-import guidance and screen-reader wording.

## Batch44 — Scheduler stage-transition presentation hardening

- Replaced the corrupted scheduler transition separator with a tested UTF-8 presentation boundary.
- Converts enum-style stage names into readable labels before showing scheduler feedback.
- Uses the same corrected transition text for visible and screen-reader feedback.
- Added coverage for normal, multi-word, whitespace, and blank stage names.

## Batch43 — Accessible session completion summary

- Added a pure accessibility presentation for completed Study sessions.
- Reads the session title, total reviewed items, new/review split, and optional lesson progress as one ordered result.
- Includes the Enter-key next action in the completion summary.
- Added coverage for general, singular-item, and lesson-completion summaries.

## Batch42 — Study focus transition hardening

- Added a pure Study focus-transition key and phase model.
- Reacquires Study keyboard focus after start, reveal, grade, item advance, retry, and completion transitions.
- Normalizes recoverable error identity so repeated error-state changes remain deterministic.
- Added focused coverage for idle, question, revealed-answer, next-item, error, and completed transitions.

## Batch41 — Explicit Study prompt and answer semantics

- Added a pure presentation boundary for prompt and answer accessibility labels.
- Labels active content as a Study prompt before reveal.
- Exposes the translation as a Study answer only when review actions are available.
- Added deterministic fallbacks for blank imported content and focused unit coverage.

## Batch40 — Complete scheduler feedback card semantics

- Added a unified semantic description to the visible Scheduler Feedback card.
- Exposed every displayed scheduler metric in a predictable reading order.
- Reused the Batch39 accessibility presentation boundary so visible and announced values cannot drift.
- Added focused coverage for rating, interval, next-review, transition, and counter descriptions.

## Batch39 — Accessible scheduler feedback confirmation

- Added a pure accessibility presentation for persisted scheduler feedback.
- Announces the saved rating, stage transition, next interval, next review time, review count, and lapse count.
- Integrates the confirmation into the next-question and completed-session Study announcements without changing scheduler behavior.
- Added focused unit and presentation-integration coverage.

## Batch38 — Contextual Study rating guidance

- Added concise explanations for what Again, Hard, Good, and Easy mean for recall and the next scheduling interval.
- Displayed the guidance only when an answer is revealed and rating actions are available.
- Added a combined screen-reader description that preserves the verified 1–4 keyboard order.
- Added focused unit coverage for rating order, scheduling meaning, and shortcut descriptions.

## Batch37 — Accessible Study action descriptions

- Added a centralized accessibility presentation for retry, start, reveal, and all four review-rating controls.
- Added explicit screen-reader descriptions that state each action and its exact keyboard shortcut.
- Reused the presentation in Desktop Study buttons so visible labels and semantic descriptions cannot drift.
- Added unit coverage for primary, recovery, reveal, and rating action descriptions.

## Batch36 — Accessible Desktop Study state announcements

- Added a pure accessibility presentation model for idle, question, revealed-answer, completed, active, and recoverable-error Study states.
- Added polite screen-reader status announcements that include the currently available keyboard action.
- Added semantic lesson-progress descriptions with current item and completed-item context.
- Added focused unit coverage for all major Study accessibility states and error priority.
## Batch35 — Recoverable Desktop Study error state

- Added a pure presentation model for persisted Study load failures with explicit recovery guidance.
- Added Enter/Space retry handling while an error is shown, while preserving protection from review shortcuts.
- Updated the Study error card with a clear title, actionable guidance, and visible keyboard hint.
- Added unit coverage for both error presentation and state-aware retry shortcuts.

## Batch34 — Actionable Desktop Study idle state

- Replaced the ambiguous `--` idle learning-item placeholder with a dedicated ready-to-study card.
- Added clear guidance describing what a general study session will do.
- Kept the primary action aligned with the verified Enter/Space keyboard workflow.
- Added focused state-resolution coverage so active, completed, and recoverable-error states cannot display the idle presentation.

## Batch33 — Desktop Study keyboard workflow

- Added state-aware Study shortcuts: Enter/Space starts or reveals, while 1–4 grades Again, Hard, Good, and Easy.
- Automatically focuses the Study surface so the keyboard flow works immediately after navigation.
- Added visible shortcut hints to every affected Study action.
- Disabled shortcut dispatch while recoverable load errors are shown and added focused resolver coverage.

## Batch32 — OPD3 Desktop graded-study restart completion

- Recreated the persisted Desktop application context before grading and verified the same lesson-scoped queue resumes at the same first item.
- Extended the resumed OPD3 Content Library path through answer reveal and grading via `StudyViewModel`.
- Verified persisted completion clears the active session while sibling-lesson content never leaks into the study flow.
- Closed the functional Desktop Beta path from real OPD3 import through persisted session completion.

## Batch31 — Real OPD3 browse-to-study presentation flow

- Added a real four-file OPD3 package fixture using `manifest.json`, `metadata.json`, `contents.json`, and `learning-items.json`.
- Created a content library for imported OPD3 bundle content using manifest identity.
- Verified Content Library browsing, lesson selection, lesson-scoped session creation, and navigation into Study through Desktop presentation components.

## Batch30 — Content Library lesson-study navigation boundary

- Added a Desktop presentation coordinator for the Content Library lesson start action.
- A successful lesson start now navigates to Study only after an active session exists.
- A failed lesson start remains in Content Library and preserves the recoverable Study error state.
- Added integration coverage using a persisted application context and imported multi-lesson data.

## Batch29 — Desktop lesson-scoped persisted restart coverage

- Added Desktop-module integration coverage that imports multiple lessons and starts study through `StudyFacade.startLessonStudy`.
- Fixed Desktop progress totals to use the actual planned study queue rather than all enabled lesson learning items.
- Aligned completed-session reviewed count and position with the completed queue total.
- Verified the persisted Desktop application context resumes and completes the planner-selected lesson queue without leaking an item from a sibling lesson.

## Batch28 — Persisted Desktop application study queue wiring

- Replaced the in-memory study queue used by `LearningApplicationFactory.createPersisted` with the existing JSON-backed study queue repository.
- Added `study-queues.json` to the Desktop application context transaction boundary so session, queue, memory-state, and review writes remain restart-consistent.
- Added integration coverage proving a study session created through the Desktop application factory is resumable after recreating the application context.

## Batch27 — Persisted OPD3 restart integration coverage

- Added an end-to-end integration test that imports a representative two-item OPD3 package through the composed persisted platform.
- Verified imported learning items can start a real study session and persist a review through the production transaction boundary.
- Recreated the complete platform from disk and verified the remaining queue item resumes correctly after restart.

## Batch26 — OPD3 manifest and metadata consistency validation

- Added package-level compatibility validation that decodes required `metadata.json` during bundle import.
- Rejected OPD3 bundles whose supplied metadata name, version, or format disagrees with `manifest.json`, preventing descriptor/content identity drift.
- Preserved legacy metadata files with omitted optional identity fields and case-insensitive OPD3 format compatibility.

## Batch25 — Atomic persisted study queue transaction coverage

- Added `study-queues.json` to the persisted platform transaction boundary used by review/session operations.
- Added integration coverage proving the composed persisted platform writes session, queue, memory-state, and review-event state together during a real review flow.
- Closed a restart-consistency gap where queue advancement was previously outside the JSON transaction snapshot set.

## Batch24 — Recoverable study/review data diagnostics

- Prevented Desktop study initialization, refresh, session start, reveal, and review persistence failures from terminating the UI flow.
- Preserved the last good study state while exposing contextual persisted-record diagnostics.
- Added an explicit Retry action for repairing persisted data and reloading the study path.
- Added focused tests for incompatible and generic study persistence failures.

## Batch23 — Recoverable Desktop persisted-data diagnostics

- Prevented Content Library startup and refresh failures from terminating the Desktop flow when persisted content records are incompatible.
- Added actionable Desktop messages that identify the persisted entity type and record ID while preserving the root cause detail.
- Kept the last successfully loaded Content Library state visible when a later refresh fails.
- Added retry-through-Refresh behavior for Content Library and lesson browsing loads.
- Added Desktop tests for contextual and generic persisted-data failure messages.
- Updated handoff and roadmap continuation context.

## Batch22 — Contextual incompatible persisted-record diagnostics

- Added `InvalidPersistedRecordException` with persisted entity type, record ID, and preserved root cause.
- Applied contextual mapping to content libraries, collections, contents, learning items, content packages, and package catalogs.
- Added tests for invalid enum values and prevention of nested duplicate wrapping.
- Updated handoff and roadmap continuation context.

## Batch21 — Actionable Package Import Diagnostics

- Added non-fail-fast directory import results that preserve successful package imports while reporting each incompatible or malformed candidate independently.
- Added source-specific failure diagnostics to the Desktop Content Library instead of collapsing the whole directory import into one generic exception.
- Desktop now distinguishes an empty directory, a fully failed import, and a partially successful import with skipped packages.
- Added application coverage proving that detailed directory import continues after candidate failures.

## Batch20 — persisted session recovery reconciliation

- Added an application recovery use case that reconciles the latest active session with its persisted study queue.
- Resumable sessions now return queue progress through one explicit recovery result.
- Active sessions with a missing queue are safely finalized instead of remaining permanently orphaned.
- Sessions whose queue completed before restart are finalized and their stale queue is removed.
- Desktop Study now displays actionable recovery messages and allows a clean new session after incomplete persistence is detected.
- Added recovery coverage for no-session, resumable, missing-queue, completed-queue, and clock-skew cases.

## Batch19 — active study session recovery

- Added learner-scoped lookup for the latest active study session across in-memory and store-backed repositories.
- Exposed active-session recovery through `LearningEngine`.
- Restored the persisted queue, lesson scope, title, counts, and current item when the Desktop study screen is reopened after restart.
- Added repository contract coverage for active-session selection and learner isolation.

This changelog records verified repository increments. Historical descriptions are concise because source and commits remain authoritative.

## Batch18 — repository workflow guardrails

- Normalized the Git-first repository workflow and guardrails.
- Established the clean `develop` baseline represented by commit `748b328`.
- Confirmed the batch package/apply direction used for subsequent increments.

## Batch17 — living documentation baseline

- Restored project documentation under `docs/`.
- Documented the two-module structure and layered dependency direction.
- Established `develop` as the canonical development branch.
- Documented verified apply, build, backup, and rollback expectations.

## Batch16 milestone

- Included study-queue planning, policy, persistence, diagnostics, and metrics foundations.
- Included content-library collection workflows and Desktop dialogs.
- Included dashboard visualization foundations.
- Continued migration toward Git as the source of truth.

## Discarded Batch19 package

A previously generated Batch19 archive was not accepted as a verified functional increment and did not change the repository. It must not be reused or treated as completed work. The next genuine increment remains `Batch19`, rebuilt from the current clean source baseline.

## Documentation consolidation

The source-only continuation package now keeps four canonical documents: `PROJECT_HANDOFF.md`, `CHANGELOG.md`, `ROADMAP.md`, and `ARCHITECTURE.md`. Stale external handoff ZIPs and repository-generated batch artifacts are not part of the baseline.

## Batch64 - Search refinement reset epic

- Added one shared refinement-state and presentation contract for search query, filter, and sort changes.
- Added an accessible Reset view control to Review History and Lesson Browser.
- Reset restores the complete default view in one action instead of requiring three separate controls.
- Added regression tests for default, partial, and fully refined states.

## Batch66 — Keyboard-first search recovery

- Added one shared keyboard contract for search surfaces.
- `Ctrl+F` focuses search in Review History and Lesson Browser.
- `Escape` progressively clears the query first and then resets filter and sort refinements.
- Added visible and screen-reader shortcut guidance plus pure regression tests.

## Batch68 — Accessible result status
Searchable desktop collections now expose a polite live result status that distinguishes complete collections, filtered subsets, empty matches, and truly empty sources.

## Batch69

- Added independently removable search query, filter, and sort refinements.
- Preserved the existing one-action full reset and keyboard recovery contract.
- Added deterministic shared and screen-level regression coverage for refinement action ordering and availability.

## Batch70 - Search match highlighting
- Added reusable, case-insensitive search match presentation with deterministic non-overlapping ranges.
- Highlighted matching query text across lesson browser rows and review history cards.
- Preserved complete screen-reader text while announcing the number of visible matches.

## Batch71
- Added reusable search-scope disclosure for desktop search surfaces.
- Lesson Browser now states searchable lesson fields and current query/filter/sort context.
- Review History now states searchable metrics and current query/filter/sort context.
- Added pure presentation and feature adapter tests.

## Batch72 - Contextual search query guidance

- Adds shared contextual placeholders and searchable examples to Desktop search fields.
- Guides one-character queries toward more specific matches.
- Announces normalized active queries without changing search projection behavior.
- Adds shared and screen-specific regression tests.


## Batch73 - Multi-term desktop search

- Adds shared whitespace-normalized query parsing with case-insensitive duplicate removal.
- Makes Lesson Browser and Review History require every query word while allowing any word order.
- Highlights every matching query term and safely merges overlapping highlight ranges.
- Explains multi-word matching behavior in visible and screen-reader guidance.
- Adds parser, matcher, highlighting, guidance, and screen projection regression tests.
