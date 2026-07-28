# Test Matrix

This matrix maps common changes to focused verification neighborhoods. Exact source and tests
remain authoritative. Build and testing policy lives only in [`../AGENTS.md`](../AGENTS.md).

## Visual Theme System

Theme-engine changes must run `LEThemeEngineTest` and `LearningThemeTest`. Minimum evidence
includes deterministic LIGHT/DARK/SYSTEM resolution, all `LETheme` token groups and defaults,
semantic light/dark role separation, complete typography metrics/color binding, exact spacing,
shape, motion, elevation, border, icon, and density contracts, resolved Material compatibility
palette, one `LearningEngineTheme` authority, one logic-free `LearningTheme` bridge, and source
guards against mutable state or screen/domain/persistence dependencies. PLE-028B is foundation
only: automated evidence does not imply screen migration or visual UAT.

PLE-028 base-component changes must additionally run `LEBaseComponentsTest` and affected
consumer tests. The durable gate covers semantic variant mapping; resting, hover, pressed,
focused, disabled, and loading-disable resolution; readable disabled content; token focus width;
Comfort/Compact/Touch targets; theme invariance; callback/enabled preservation; and source
guards against arbitrary style parameters, Material color authority, hardcoded theme values,
screen/domain/persistence/audio/navigation dependencies, and viewport policy.

PLE-028D Study presentation changes must additionally run `StudyVisualThemeMigrationTest`,
`StudyVisualLayoutResolverTest`, `FocusedAnswerSurfaceVisualHierarchyTest`, and relevant audio
regressions. Minimum evidence covers neutral Light/Dark canvas/text roles, semantic
answer/Meaning/Example/Scheduler/rating surfaces, exact Again/Hard/Good/Easy order and callback
identity, enabled/disabled/focus mappings, Pause/Undo callback preservation, word/IPA/meaning
typography, image Fit/collapse contracts, scheduler interval preservation, one responsive
authority, external audio ownership, and guards against Material color/raw color/theme
resolution or behavior-layer imports. Automated tests do not replace visual UAT.

## Study, queue, and review flow

Run or inspect tests covering:

- queue lifecycle and planning;
- lesson-scoped selection and sibling avoidance;
- reveal and grading transitions;
- scheduler integration;
- atomic persisted review transactions;
- restart, resume, lesson isolation, and completion;
- Desktop Study state, keyboard routing, focus, and presentation contracts.
- PLE-027 Responsive Study Visual Layout (`StudyVisualLayoutResolverTest`) and Answer Surface Visual Hierarchy (`FocusedAnswerSurfaceVisualHierarchyTest`).
- PLE-027 Desktop Representative UAT Matrix: 15 content/item variants x 3 viewport classes (`COMPACT`, `STANDARD`, `WIDE`), vertical scroll reachability, zero horizontal overflow, 2x2 grid dock (<= 479dp), horizontal 4-button dock (>= 480dp), zero answer leakage, shortcut routing (`1-4`, Space, `[R]`, `Esc`, `Ctrl+Z`).

Phase 6 lifecycle work must additionally cover every valid/invalid state transition, persisted
schema compatibility, process restart at transition boundaries, missing/completed queue
reconciliation, duplicate-review prevention, and transaction rollback. Undo requires exact
forward/reverse state assertions across review event, memory state, session, and queue.

P6-02 covers current-item/reveal/pending-intent lifecycle invariants, schema-v1 checkpoint
round-trip and legacy defaults, interruption before transaction mutation, single replay with a
stable event ID, existing restart recovery, and the full root/Desktop regression suite.

P6-03 adds pure workspace transition and allowed-action coverage, forbidden rating/reveal
ordering, keyboard routing from explicit state, and persisted Desktop restart projection of a
revealed answer.

P6-04 covers deterministic Question/Answer/Example block ordering, plain/Markdown formats,
newline preservation, optional sections, unsafe and missing asset fallback, package DTO and
persistence compatibility, and Desktop restart projection. Content-model changes must also run
package import, JSON persistence, and Desktop Study regression coverage.

P6-05 covers Question-only versus revealed Question/Answer/Example visibility, ordered blocks,
safe Markdown structures and inline styles, inert HTML, local image/audio resolution, localized
missing/unsupported fallbacks, and failure-safe audio state. Renderer changes must preserve the
workspace action, keyboard, restart projection, package import, and persistence suites.

Desktop audio changes additionally require a real decoder fixture, imported-style spaced and
Unicode paths, semantic prompt/answer/example roles, primary replay dispatch, observable adapter
state, rapid replacement, stale completion, output release, missing-media failure, and
item/pause/disposal cancellation. Automated coverage ends at the real decoded-PCM output
boundary; audible speaker verification remains manual UAT.

Adaptive Study presentation changes must cover schema-v1 missing-property defaults, all-mode
round-trip, strict malformed mode/boolean rejection without rewriting bytes, Settings
draft/preview mapping, Adaptive recommendation authority, Preference Guided intersection,
Manual control, primary-English safety, reveal/content/audio availability, hidden-loop
cancellation, transition deduplication, item advance, and Apply-without-replay. Physical audio,
long bilingual content, and mode switching during an active Study session remain manual UAT.
Presentation-remediation coverage must additionally prove revealed word/IPA/POS visibility uses
the effective policy, current and pending preferences remain separate, Settings and quick
controls reconcile through the same persistence path, item advance promotes pending state,
header status describes the next item, and staging never triggers current-item autoplay or loop
cancellation.
Phase-separation changes must prove the presentation policy API contains no reveal/workspace
input, identical preferences and availability resolve identically across phases, Question and
Answer renderers select their own layers, Manual Vietnamese Question autoplay requires visible
available audio, and Adaptive/Preference Guided transition behavior remains backward-compatible.
Semantic text presentation changes must cover every required role, canonical-slot assignment,
application-to-Desktop mapping, Manual EN/VI inverse visibility, always-visible
instruction/neutral text, Listening Question semantic projection, Image meaning
non-duplication, Question-to-Answer consistency, scene-scoped autoplay, zero answer leakage,
and Adaptive/Preference Guided regressions.

Focused study workspace changes must verify that only an ACTIVE Learn destination suppresses
shell chrome/dashboard metrics, the content width remains bounded, question/reveal/rating action
contracts and shortcuts remain unchanged, semantic media ordering remains deterministic, and
normal navigation returns outside the active session. Responsive visual hierarchy, themes, long
content, twenty-item use, and physical audio use the Product Owner checklist in
`DESKTOP_LEARNING_EXPERIENCE_ALPHA_UAT.md`.

Learning Flow changes must cover definition invariants, deterministic primary rotation, optional
Typing ordering, immutable ordered controller transitions, stale/duplicate rejection, reveal
request/confirmation, rating-ready gating, progress ranges, and platform dependency guards.
Desktop coverage must include stable same-item synchronization, Continue for passive stages,
Typing EMPTY/completed behavior, single reveal request, authoritative revealed recovery,
item/session reset, pause ownership, undo/restart reconstruction, localization, scene projection,
manual rating, and unchanged audio/keyboard/review/session regression suites.

Product Brain changes must additionally cover objective stability, strategy ownership of
rotation/Typing decisions, template ordering and immutability, deterministic equal-input output,
and the architecture gate that `LearningFlowPlanner` accepts templates rather than experience
plans. Shared objective/strategy/template APIs must remain free of Desktop, Compose, filesystem,
persistence, scheduler implementation, clock, random, and platform types.

Desktop Alpha-04 completion changes must cover reflection and summary generation for applicable
evidence outcomes, aggregate consistency validation before mutation, Product Brain rating intent,
real scheduler invocation through the review transaction, completion snapshot mapper and
persisted restart round-trip, Desktop Facade/ViewModel/state projection, learner-visible summary,
and stale-presentation clearing on a new workflow. End-to-end evidence must cross bootstrap,
scene execution, evidence, adaptive decision, explanation, review scheduling, session finish,
and repository recovery without moving scheduling or instructional rules into Compose.

Shared experience policy changes must cover canonical ordered eligibility for every Image/Audio/
Text combination, Prompt fallback, repeat generation, capability flags, reveal-invariant option
order, supporting roles, and null/unsafe input. Selection tests must cover one/two/three options,
ordinal zero, positive/large/negative floor-mod, non-mutation, repeated requests, delegation,
semantic result fields, out-of-range strategy rejection, empty-input fallback, and dependency
guards against Desktop/Compose/Path types. Desktop projector tests must prove each result mapping,
Meaning/Example order, result authority when resolved blocks disagree, missing-media fallback,
and ordinal-zero baseline behavior. Session-aware rotation also covers automatic-profile order
and immutability, Typing exclusion, explicit choice, ordinal boundaries, same-item/reveal/retry
stability, item transition, restart/resume, queue-rewind undo, and new-session reset. Audio,
keyboard/reveal/rating/undo/pause, content projection,
restart, package import, persistence, queue, scheduler, and review suites remain mandatory.

Typing Recall changes must cover semantic answer-text eligibility, media-only/unavailable
answers, canonical Image/Listening/Prompt/Typing order, ordinal-zero compatibility, deterministic
multi-block extraction, exclusion of examples/presentation text, raw Markdown semantics,
whitespace/case normalization, punctuation/diacritic/word-order differences, blank input,
locale stability, repeated evaluation, and shared dependency guards. Desktop coverage must
prove chooser eligibility/default fallback, engine-backed user choice, Typing scene projection,
supporting scenes after reveal, real-item identity reset, empty/correct/incorrect feedback,
manual reveal/rating separation, focused-input shortcut suppression, and Escape pause parity.
Submission hardening additionally requires empty/whitespace no-reveal behavior, deterministic
repeated empty evaluation, edit-after-empty clearing, correct/incorrect reveal eligibility,
single-reveal protection, and action-in-progress rejection.

P6-06 covers known/unknown and empty totals, processed/reviewed/skipped distinctions, start and
post-review progress, transaction failure and pending-review recovery, persisted restart,
queue-based completion, scheduler-result feedback, and visible/screen-reader summaries. Progress
changes must retain atomic session/queue/review tests, completed-queue restart projection, plus
P6-05 renderer and keyboard coverage.

P6-07 minimum evidence must cover pause-as-active-resume, exactly-one latest undo, absence of
multi-level undo, event/memory/session/queue/progress reversal, final-review completion reopen,
no-prior-memory restoration, failed undo rollback, retry idempotency, restart behavior, legacy
record compatibility, and existing P6-02/P6-06 interruption/completion regressions.
Focused coverage exercises first-memory deletion, event/session/queue/progress restoration,
idempotent retry, final-session reopening, persisted restart, optional-record compatibility,
and the established pending-review recovery/workspace regressions.

## P6-09 Desktop release-candidate evidence

Automated coverage owns persisted OPD3 import-to-completion, installed-package discovery,
global and lesson-scoped queue isolation, restart at durable lifecycle boundaries, review and
undo rollback, retry/idempotency, legacy records, progress/completion, rich-content fallbacks,
state/action permission, reveal/rating/undo/retry/pause keyboard routing,
busy/repeat/text-input suppression, focus-phase identity, localized action contracts, safe error
copy, media fallbacks, renderer semantics, restart/resume, final-review undo, progress rollback,
and second-undo blocking.

Product Owner manual evidence remains pending for P6-09:

**A. Core learning**

1. Import a representative package.
2. Start a global session.
3. Start a lesson-scoped session and confirm unrelated content is excluded.
4. Reveal and rate with Again, Hard, Good, and Easy.
5. Complete a session.
6. Verify progress at every step.

**B. Safe interruption**

7. Close at Question and reopen.
8. Close at Answer Revealed and reopen.
9. Exercise the pending-review recovery fixture/path when available.
10. Pause/leave the workspace and resume.
11. Rate, Undo, and rate again.
12. Undo the final rating from completion.
13. Attempt a second Undo.

**C. Keyboard and accessibility**

14. Complete a session using only the keyboard.
15. Spam rating keys.
16. Exercise shortcuts in invalid states.
17. Verify Tab and Shift+Tab traversal.
18. Verify visible focus.
19. Operate audio controls by keyboard.
20. Verify English and Vietnamese action labels.
21. Verify status is not communicated by color alone.

**D. Content and media**

22. Verify long plain text.
23. Verify long Markdown.
24. Verify Vietnamese/English Unicode content.
25. Verify missing-image fallback.
26. Verify unsupported-image fallback.
27. Verify missing-audio fallback.
28. Verify unavailable-codec fallback.
29. Verify narrow-window scrolling.

**E. Error recovery**

30. Exercise preparation failure.
31. Exercise review-transaction failure.
32. Exercise Undo failure.
33. Exercise a corrupt-persistence fixture.
34. Retry after failure.
35. Confirm no raw exception or stack trace is shown.
36. Confirm a failed action does not advance progress.

**F. Windows environment**

37. Run from a path containing spaces.
38. Run from a Unicode user/path fixture where available.
39. Close and reopen the application repeatedly.
40. Confirm no file is created outside the resolved runtime storage directories.

This checklist is not recorded as passed until the UI is exercised in the target Desktop
environment; automated tests are supporting evidence, not a substitute for that observation.
Installer, upgrade, uninstall, clean-machine, and signing evidence remains the separate external
Phase 5 gate.

P6-09 automated release evidence: `gradlew.bat clean test --no-daemon` passed 1,544 tests with
0 failures, 0 errors, and 0 skipped; `gradlew.bat :desktop:compileKotlin --no-daemon` and
`gradlew.bat :desktop:packageUberJarForCurrentOS --no-daemon` both completed successfully. The
generated Windows x64 uber-JAR is build output and is not tracked release evidence.

Final Desktop 1.0 repository audit evidence: the same clean gate passed 1,545 tests with
0 failures, 0 errors, and 0 skipped after recovery-manifest hardening. Desktop compile and the
Windows x64 uber-JAR packaging smoke task also passed. The non-installer Desktop application
distribution was created successfully with a full Temurin JDK 21; installer and signing
verification remains external.

## Scheduling and memory state

Run or inspect tests covering:

- scheduler decisions for each rating;
- decision validation;
- interval and next-review calculations;
- stage transitions and metrics;
- review-to-persistence integration.

Any change to time semantics requires deterministic clock-based tests.

## OPD3 and package import

Real-data Desktop regression coverage includes immediate Importing/Loading/Preparing state,
duplicate suppression, failure guard release, ordered package stages, no premature 100%, bulk
repository read counts, 2,425/12,125 production-boundary scale, lazy stable-key rendering,
debounced stale-query protection, bounded cached thumbnail decode, missing-media fallback, and
all existing atomicity/restart/package-format suites. Timing evidence is observational and has no
flaky wall-clock pass threshold.

JSON + binary OPD3 PKG regression coverage must include content-signature routing, exact
same-basename case-insensitive sibling discovery, missing and ambiguous JSON, invalid signature,
unsupported version, truncated index, invalid media type, bounds and CRC failure, Unicode and
spaced paths, re-import behavior, installed package/library queries, extracted audio/image, and
session startup. Existing ZIP `.pkg` and standalone ZIP `.opd3` suites remain mandatory.

The deterministic fixture writes the production big-endian OPD3 table and tiny payloads at test
runtime. The Product Owner's 179 MB source must never be committed; it remains a manual Phase 7
re-test input.

Run or inspect tests covering:

- scanner routing;
- manifest and metadata identity validation;
- bundle/content import;
- package registration and installed-package queries;
- partial and failed import diagnostics;
- persisted import-to-review restart;
- representative real-package fixtures.

Malformed-input work must include both rejection behavior and actionable diagnostic text.

Study presentation regression includes Manual EN-off/VI-on and EN-on/VI-off Question filtering,
full revealed Answer disclosure for Manual, Adaptive, and Preference Guided control, and exact
example-target annotation for English case-insensitivity, multiple occurrences, word boundaries,
multi-word phrases, Vietnamese phrases, and no-match/no-mutation behavior. Reveal must not
introduce a new autoplay transition beyond the established coordinator contract.

PLE-026-R5 additionally requires experience-derived recommendation tests for Listening, Image,
Prompt, and Typing; primary text/audio separation; deterministic no-leak Question blocks and
accessibility; Manual support regression; complete current-item media despite Question
sanitization; live Reveal primary-once with no recovery/recomposition/resize/Apply/stale replay
or cross-role fallback; established loop/once/replay interactions; effective versus persisted
NEW, reviewed-stage identity, typed diagnostics, and existing persisted restart/queue/undo
coverage.

## Persistence and recovery

Run or inspect tests covering:

- record mapping round trips;
- JSON store behavior;
- repository behavior;
- transaction atomicity;
- incompatible or corrupt records;
- restart reconciliation;
- Desktop composition wiring using persisted stores.

Persisted schema coverage includes compatibility or migration behavior.

Desktop recovery archives must reject negative or payload-mismatched manifest counts before a
safety backup, deletion, or replacement write. Regression coverage must assert current durable
bytes and backup state remain unchanged.

## Search and discovery

Minimum focused tests include:

- `SearchQueryTermsTest`;
- `SearchTextTest`;
- `SearchMatchPresentationTest`;
- `SearchQueryGuidancePresentationTest`;
- `LessonBrowserProjectionTest`;
- `ReviewHistoryProjectionTest`;
- keyboard, refinement, result-status, and empty-state contracts affected by the change.

Test matching and highlighting against the same normalization rules. Include multi-field
rows when terms may be distributed across searchable fields.

## Content Library and Lesson Browser

Run or inspect tests covering:

- library/collection/package presentation;
- attachment, detachment, rename, and delete flows;
- dialog state and confirmation boundaries;
- import outcomes and retry;
- lesson selection and Study navigation;
- keyboard navigation;
- Lesson Browser search projection.

## Dashboard, statistics, and review history

Run or inspect tests covering:

- application query/calculation correctness;
- loading, ready, failed, retry, and stale-data behavior;
- active-screen refresh routing;
- semantic summaries and empty states;
- chart/value presentation where affected.

## Desktop shell and navigation

Run or inspect tests covering:

- destination state and selected semantics;
- F-key and cyclic navigation;
- active-screen refresh;
- focus retention;
- ContentHost and LearningShell callback wiring.

## Performance and large-data work

Representative performance coverage includes:

- a deterministic representative fixture size;
- a correctness assertion at that size;
- a measured operation boundary;
- an explicit regression threshold only when the build environment is stable enough;
- otherwise, allocation/algorithmic assertions that do not create flaky wall-clock tests.

Timing-only checks are not correctness evidence.

## Release-readiness work

Verify as applicable:

- clean build from a clean checkout;
- distributable creation;
- launch on a clean Windows user profile or equivalent isolated directory;
- data-directory creation and permissions;
- Unicode paths and filenames;
- logging and diagnostic export;
- first-run and existing-data startup;
- upgrade/restart behavior;
- smoke flow from import to persisted completion.

Desktop runtime foundation coverage includes identity stability, generated metadata loading,
platform-specific path resolution, corrupt configuration preservation, log retention,
lifecycle ordering, restart behavior, and support-diagnostic redaction.

Windows release validation must run `:desktop:verifyWindowsLauncher` with a full JDK 21 that
contains `jpackage`. The task exercises the generated native executable with an isolated profile
that requests Java Access Bridge, verifies the bundled runtime includes `jdk.accessibility`, and
fails on launcher/JVM startup errors. App-image smoke evidence does not replace MSI/EXE install,
upgrade, uninstall, signing, clean-machine, or manual UI evidence.

Windows launcher remediation evidence: the full clean gate passed 1,546 tests with 0 failures,
0 errors, and 0 skipped; Desktop compile, app-image, launcher smoke, MSI, and EXE packaging
tasks passed with Temurin 21.0.11. MSI/EXE artifact creation is not installation, upgrade,
uninstall, clean-machine, signing, or launch-from-installed-location evidence.

## Desktop product-evolution minimum coverage

For capabilities derived from the Android product reference:

- prove scheduler, queue, review-event, session, progress, undo, and restart do not drift;
- test presentation/media/input deterministically with fake clock/player when timing is used;
- cover stale callbacks, unsupported media, pause, transition, completion, and recovery;
- cover legacy package/config/session defaults before optional roles or preferences;
- test real Desktop composition plus focused projection/evaluator behavior;
- retain keyboard/accessibility parity for every pointer or gesture action;
- distinguish automated fixture evidence from representative manual media/large-package UAT.

LX-01 specifically requires deterministic hierarchy/order, Unicode search, flat-data fallback,
lazy large-list behavior, and Content Library composition coverage. Its accepted boundary is
read-only, so it must not introduce scheduler or persistence mutation.

Every LX capability must also trace acceptance to the relevant document under `docs/spec/`.
Cross-platform conformance requires identical action availability, rating order/meaning,
failure-before-mutation, Resume/Undo semantics, media non-mutation, and accessibility reading
order even when widget/layout implementation differs.
