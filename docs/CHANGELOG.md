# Changelog

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
