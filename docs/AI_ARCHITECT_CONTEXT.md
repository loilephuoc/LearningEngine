# Learning Engine 2.0 — AI Architect Context

Short-term repository and Phase snapshot only. Standing workflow is defined in
[`../AGENTS.md`](../AGENTS.md).

## Phase & Continuation Summary

- **Current Phase boundary**: Study Experience closure within the current Visual Theme /
  Learning Experience repository structure is complete.
- **Completed**: PLE-030, PLE-031, PLE-031.1, PLE-031.2, and PLE-031C are FINAL PASS by
  Product Owner Manual UAT.
- **Current capability**: `PLE-039-F — Compact Rating Dock and Seeded New-Item Randomization` is
  implemented in the current local working batch. Typing retains only its four compact status
  segments, and NEW candidates are deterministically reordered per SessionId after strategy
  placement and before diversity, balance, and policy limiting. Full PLE-032 remains incomplete.
- **Next Capability**: `PLE-032-B2 — durable Continuous Review intent and restart continuation`.
- **Repository baseline before PLE-039-F**: branch `develop`, HEAD
  `7f0fa44a734421c26a55a7a9f20b9b892fb85695` (`PLE-039-E`), origin/develop
  `a9bb3d2d44d109d0a4a7e09427dc28dad279d9f5`. The batch commit is local and intentionally
  unpushed; use `git log -1` for its resulting full SHA.
- **Verification evidence**: PLE-034-B3 full `.\gradlew.bat clean test --no-daemon` completed:
  root 1,724 tests, Desktop 963 tests, total 2,687 with 0 failures, errors, or skipped, calculated
  from generated XML. PLE-035-B1 full verification completed with root 1,752 tests and Desktop
  967 tests, total 2,719 with 0 failures, errors, or skipped.
- **PLE-039-C verification evidence**: full
  `.\gradlew.bat clean test --no-daemon --console=plain` completed with 553 XML suites / 2,843
  tests (root 358 suites / 1,755 tests; Desktop 195 suites / 1,088 tests), with 0 failures,
  errors, or skipped. The delta from PLE-039-B is 2 suites / 12 tests.
- **PLE-039-D verification evidence**: full
  `.\gradlew.bat clean test --no-daemon --console=plain` completed with 553 XML suites / 2,844
  tests (root 358 suites / 1,755 tests; Desktop 195 suites / 1,089 tests), with 0 failures,
  errors, or skipped. The one-test Desktop delta covers bounded height-mode-aware input sizing.
- **PLE-039-E verification evidence**: full
  `.\gradlew.bat clean test --no-daemon --console=plain` completed with 553 XML suites / 2,846
  tests (root 358 suites / 1,755 tests; Desktop 195 suites / 1,091 tests), with 0 failures,
  errors, or skipped. The two-test Desktop delta covers scene-type instruction visibility and
  frame-synchronized keyed input visibility.
- **PLE-039-F verification evidence**: focused orderer/planning/dock/layout selection passed 4
  XML suites / 15 tests; targeted continuation/package-authority regressions also passed. Full
  `.\gradlew.bat clean test --no-daemon --console=plain` completed with 554 XML suites / 2,851
  tests (root 359 suites / 1,759 tests; Desktop 195 suites / 1,092 tests), with 0 failures,
  errors, or skipped. The five-test delta covers stable session ordering, pre-limit subset
  variation, compact Typing reservation, and intended queue-order fixture remediation.
- **PLE-036 verification evidence**: full `clean test --no-daemon` completed with 542 XML
  suites / 2,717 tests (root 354 suites / 1,729 tests; Desktop 188 suites / 988 tests), with
  0 failures, errors, or skipped. No root source/test file changed in PLE-036.
- **PLE-036-B1 verification evidence**: full `clean test --no-daemon --console=plain` completed
  with 542 XML suites / 2,728 tests (root 354 / 1,729; Desktop 188 / 999), with 0 failures,
  errors, or skipped. The 11-test Desktop delta exactly covers the UAT remediation.
- **PLE-036-B2 verification evidence**: full `clean test --no-daemon --console=plain` completed
  with 542 XML suites / 2,733 tests (root 354 / 1,729; Desktop 188 / 1,004), with 0 failures,
  errors, or skipped. The five-test Desktop delta covers the final interaction and comparison
  rules.
- **PLE-036-C1 verification evidence**: full `clean test --no-daemon --console=plain` completed
  with 542 XML suites / 2,739 tests (root 354 / 1,734; Desktop 188 / 1,005), with 0 failures,
  errors, or skipped. The six-test delta replaces the prior join-all extractor expectations with
  canonical authority and adds front-side warning regression coverage.
- **PLE-036-C2 verification evidence**: full `clean test --no-daemon --console=plain` completed
  with 542 XML suites / 2,747 tests (root 354 / 1,734; Desktop 188 / 1,013), with 0 failures,
  errors, or skipped. The eight-test Desktop delta covers positional Unicode feedback and real
  reveal/rating/next/completion/retry orchestration.
- **PLE-036-C3 verification evidence**: focused comparison/header/typography/regression selection
  passed 63 tests. Full `clean test --no-daemon --console=plain` completed with 542 XML suites /
  2,753 tests (root 353 / 1,733; Desktop 189 / 1,020), with 0 failures, errors, or skipped. Six
  new Desktop test methods account for the net +6 total-test delta from the supplied C2 reference.
  The clean XML module split moved by root -1/Desktop +7 despite no root source or test changes,
  so that one-test module redistribution is not attributed to C3 behavior.
- **PLE-037 verification evidence**: focused application strategy/template selection passed
  24 tests and focused Desktop flow/input/audio/overlay/comparison selection passed 59 tests.
  Full `clean test --no-daemon --console=plain` completed with 543 XML suites / 2,767 tests
  (root 354 / 1,739; Desktop 189 / 1,028), with 0 failures, errors, or skipped. Fourteen new test
  methods exactly account for the net +14 total-test delta from PLE-036-C3. Relative to its
  supplied module split, XML discovery assigned root +6/Desktop +8 while authored coverage was
  root +5/Desktop +9; this reverses the one-test module redistribution recorded in C3.
- **PLE-037-A verification evidence**: focused Desktop presentation, autoplay, projector,
  audio-controller, Typing input/success/comparison, and Answer Surface selection passed 14 XML
  suites / 149 tests. Full `clean test --no-daemon --console=plain` completed with 543 XML suites /
  2,772 tests (root 354 / 1,739; Desktop 189 / 1,033), with 0 failures, errors, or skipped.
  Five new Desktop test methods exactly account for the +5 total-test delta from PLE-037.
- **PLE-037-B PATCH verification evidence**: focused Desktop input, TextFieldValue, live-diff,
  success, autoplay, and Chrome regression selection passed 6 XML suites / 67 tests. Full
  `clean test --no-daemon --console=plain` completed with 543 XML suites / 2,773 tests
  (root 354 / 1,739; Desktop 189 / 1,034), with 0 failures, errors, or skipped. The one new
  Desktop test covers single-line vertical centering and the multiline 2–5 line fallback.
- **PLE-038 verification evidence**: focused tracker, policy, input, keyboard, Dock/Status,
  success, live-diff, autoplay, theme, and real-session selection passed 12 XML suites /
  117 tests. Full `clean test --no-daemon --console=plain` completed with 545 XML suites /
  2,793 tests (root 354 / 1,739; Desktop 191 / 1,054), with 0 failures, errors, or skipped.
  Two new Desktop suites and 20 new Desktop test methods exactly account for the +2-suite /
  +20-test delta from PLE-037-B PATCH.
- **PLE-038-A verification evidence**: focused Desktop status projection, Review-memory Dock,
  and compact Chrome selection passed 3 selected suites / 7 tests. Full
  `clean test --no-daemon --console=plain` completed with 546 XML suites / 2,795 tests
  (root 354 / 1,739; Desktop 192 / 1,056), with 0 failures, errors, or skipped. One new Desktop
  suite and two new Desktop test methods exactly account for the +1-suite / +2-test delta from
  PLE-038.
- **PLE-038-B verification evidence**: focused Desktop preview/policy, timer/legend composition,
  status panel, responsive layout, input, keyboard, real-session, audio, and autoplay selection
  passed 14 suites / 128 tests. Full `clean test --no-daemon --console=plain` completed with
  548 XML suites / 2,810 tests (root 354 / 1,739; Desktop 194 / 1,071), with 0 failures, errors,
  or skipped. Two new Desktop suites and 15 new Desktop test methods exactly account for the
  +2-suite / +15-test delta from PLE-038-A.
- **PLE-038-C verification evidence**: focused Desktop attempt lifecycle, policy, preview,
  timer composition, and real-session integration passed. Full
  `clean test --no-daemon --console=plain` completed with 548 XML suites / 2,813 tests
  (root 354 / 1,739; Desktop 194 / 1,074), with 0 failures, errors, or skipped. Three new
  Desktop test methods account for the +3-test delta from PLE-038-B.
- **PLE-038-D verification evidence**: focused Desktop durable-context validation, spaced-memory
  policy, preview/legend, overlay, content-stage, and real-session integration passed. Full
  `clean test --no-daemon --console=plain` completed with 548 XML suites / 2,818 tests
  (root 354 / 1,739; Desktop 194 / 1,079), with 0 failures, errors, or skipped. Five new
  Desktop test methods account for the +5-test delta from PLE-038-C.
- **PLE-039-A verification evidence**: focused confidence value/projector/query tests and
  repository, review transaction, Undo, scheduler, and PLE-038-D Typing regressions passed.
  Full `clean test --no-daemon --console=plain` completed with 550 XML suites / 2,827 tests
  (root 356 / 1,748; Desktop 194 / 1,079), with 0 failures, errors, or skipped. Two new root
  suites and nine tests account exactly for the delta; Desktop discovery and behavior are
  unchanged.
- **PLE-039-B verification evidence**: focused confidence gate/query-count, Typing
  policy/preview, and real-session integration passed. Full
  `clean test --no-daemon --console=plain` completed with 551 XML suites / 2,831 tests
  (root 357 / 1,752; Desktop 194 / 1,079), with 0 failures, errors, or skipped. One new root
  suite and four tests account exactly for the delta; existing Desktop suites remain unchanged.
- **External gates remain open**: clean-machine verification, installer/update/uninstall,
  signing, real large-package/manual evidence, and external Beta validation. Phase 7 and
  Desktop v1 are not declared complete.

### PLE-033-B1 Desktop UX Polish

- Custom Review edits persist through the existing runtime configuration; the remembered Custom
  value survives preset use and restart while only the active target maps into new Session policy.
- Space reuses the existing Good action on the Answer side and retains the existing repeat,
  action-in-progress, and input-focus guards.
- Answer images clip the bitmap with the existing large-radius token and retain adaptive
  `ContentScale.Fit`; New/Review configured targets use aligned typography and the existing purple
  metric token.
- Planner, Scheduler, Session lifecycle, Undo, and the PLE-032 continuation boundary are
  unchanged.

### PLE-033-B2 Post-Session Experience

- The finished predecessor's durable `reviewedItemIds` defines committed membership; its
  retained Study queue defines replay order.
- `ReplayCompletedStudySessionUseCase` creates one ordinary review-only Session using a
  deterministic replay-purpose identity and explicit Accepted/NoItems/Rejected outcomes.
- Desktop delegates through LearningEngine and preserves its action guard. Good displays `3`
  and `Space`; New/Review targets use existing semantic green/blue.
- Current repositories do not guarantee atomic concurrent creation. Planner, Scheduler,
  PLE-032, review transactions, and cross-Cycle Undo remain unchanged.

### PLE-033-B3 Learn Entry & Review Progress

- Review session progress is committed Review Content count over the immutable configured target;
  Undo and restart use the persisted `StudySession.reviewItemsReviewed` authority.
- Idle Learn availability is projected by Application for Continue, latest scoped replay,
  learned-content review, and Library navigation; Desktop performs no repository scans.
- Learned eligibility requires a reviewed MemoryState or committed ReviewEvent, then enabled
  current-scope content. Review-all is bounded by the Review policy and creates an ordinary
  review-only Session with deterministic due/oldest ordering.
- PLE-032 durable intent, Planner, Scheduler, persistence schemas, and cross-package review remain
  unchanged.

### PLE-034 Learning Hub MVP

- Idle Learn and Session Completion project the same four semantic actions and use one Desktop
  dispatcher for Continue, latest scoped replay, review-all, and Library navigation.
- Completion retains its existing summary and displays Review All directly; application
  availability continues to own disabled Replay/Review All states.
- No Scheduler, Planner, queue, persistence, MemoryState, Undo, or completion authority changed.

### PLE-034-B2 Learn Hub Access & Unique Review Coverage

- Learn/F2 opens the shared chooser regardless of active-session presence. The active Session
  remains visible as an explicit Continue choice rather than an implicit navigation side effect.
- Switching to Replay or Review All invokes one application leave boundary: committed reviews
  remain durable, while the old queue and its session-local Undo checkpoint cannot leak into the
  replacement source.
- Review All snapshots one learned representative per `ContentId`; persisted
  `StudySession.reviewedContentIds` owns committed unique coverage and Undo restoration.
- Existing `allowRepeatInSameSession` plus persisted queue order schedules deterministic
  Again/Hard reinforcement without a new schema. Retry attempts do not increment coverage,
  unseen Content cannot starve, and full unique coverage ends the pass without exceeding target.

### PLE-034-B3 Review All Scope & Settings Preset Fix

- Review All availability and Session creation consume the complete Application-owned unique
  learned-Content snapshot. Ordinary `reviewItemsPerSession` no longer caps its chooser count,
  queue, policy target, coverage completion, Undo, or restart.
- General Study remains governed by the configured New/Review Session limits.
- Settings uses each effective New/Review numeric configuration as preset selection, custom text,
  runtime policy and persisted restart authority. The legacy custom Review property is written
  and loaded in synchronization with that effective value.
- Verification: full `clean test` passed 538 suites / 2,687 tests (root 1,724; Desktop
  963), with 0 failures, errors, or skipped.

### PLE-035-B1 Typing Live Diff & Reveal Comparison

- The pure Typing evaluator aligns normalized Unicode code points while retaining exact original
  user and expected strings for revealed display.
- Live presentation exposes only entered text: correct prefix, erroneous remainder, and an
  optional missing boundary. Incorrect Check remains editable and does not reveal.
- Explicit Reveal uses the existing flow/facade answer boundary, retains Full Answer and manual
  ratings, and adds a localized comparison. Item identity remains the reset authority.
- Scheduler, FSRS, queue, Review All coverage, and persistence remain unchanged.
- Verification: full `clean test` passed 545 suites / 2,719 tests (root 1,752; Desktop
  967), with 0 failures, errors, or skipped.

### PLE-035-B2 Responsive Full Answer Surface

- The shared revealed surface derives Wide/Medium/Narrow behavior from measured content width,
  not raw screen resolution. Wide uses a 38/62 Translation/Examples row; Medium stacks both;
  Narrow defaults the complete Examples section to a keyboard-accessible collapsed disclosure.
- Word and audio/IPA/POS remain the identity header. POS is removed from Translation, whose
  compact wrapping row is approximately 52dp for a short meaning.
- Images consume measured height with `ContentScale.Fit`; the 1040dp wide answer cap and shorter
  supporting region make the image materially larger without cropping or horizontal scrolling.
- Typing comparison remains visible outside the Examples collapse. Rating Dock, keyboard,
  Scheduler, FSRS, queue, Session, persistence, Review All, and Undo are unchanged.
- Verification: full `clean test` passed 546 suites / 2,727 tests (root 1,752; Desktop
  975), with 0 failures, errors, or skipped.

### PLE-035-B3 Examples Keyboard Disclosure

- Narrow Examples binds one item-scoped Desktop presentation controller. E/Shift+E toggles;
  Esc collapses or is a consumed no-op while the disclosure exists.
- Typing focus prevents disclosure dispatch. Focused Enter/Space is consumed locally before the
  unchanged Study shortcut resolver, so rating, audio, and navigation actions do not double-run.
- Localized tooltip and semantics expose keyboard discovery. Current Content identity resets
  state for Next/Undo; restart starts collapsed, while Medium/Wide remain expanded.
- Scheduler, FSRS, queue, Session, persistence, Review All, and engine boundaries are unchanged.
- Verification: full `clean test` passed 546 suites / 2,733 tests (root 1,752; Desktop
  981), with 0 failures, errors, or skipped.

### PLE-036 Typing Mastery Completion

- The real editable control remains Typing input authority; an identity-mapped visual
  transformation styles only a genuine mismatching suffix, while incomplete correct prefixes
  stay neutral and supporting feedback remains adjacent.
- Incorrect Check preserves correction mode. Correct Check requests one cancellable presentation
  sequence: visible success, existing answer-audio completion/failure authority, semantic
  non-blocking dwell, then existing GOOD review dispatch; session authority owns the next item.
- Reveal evaluates the latest input snapshot independently of prior Check state. Non-empty
  comparison preserves original strings, exposes all existing diff operation kinds with
  non-color semantics, and precedes Meaning/Examples in every responsive mode.
- Transient success and comparison state remain item-scoped and unpersisted. Scheduler, FSRS,
  planner, queue, Session lifecycle, persistence, Review All, and frozen capability-design
  artifacts are unchanged.

### PLE-036-B1 Realtime Typing Mastery Remediation

- Manual Desktop UAT disproved the submit-driven completion and scene-gated comparison claims in
  PLE-036. Realtime state now distinguishes live evaluation, explicit incorrect feedback,
  pending/success facts, and a separate item-scoped Reveal evaluation.
- Each material raw-text/composition change reuses the existing evaluator. Exact committed input
  schedules a cancellable 450 ms debounce; selection-only changes do not duplicate it, and active
  IME composition cannot trigger success.
- Live danger styling is derived only from typed REPLACEMENT/INSERTION operations. Incomplete
  prefixes, exact input, deletion-only input, and unsafe normalization mappings remain neutral.
- Manual Reveal cancels automation, evaluates the latest raw input, and Full Answer renders that
  comparison before Meaning/Examples without depending on the projected scene type.
- Engine boundaries and durable state remain unchanged. Product Owner Manual UAT is still
  required; automated verification does not declare UAT PASS.

### PLE-036-B2 Final Typing Recall UX

- Check Answer is removed from Typing Recall. Reveal remains directly visible, and Enter/IME
  Done invokes it only when committed input is not exact.
- Exact committed input retains the composition-safe 450 ms automatic success sequence and
  existing audio/GOOD authorities, so the automatic path exposes no manual rating choice.
- Manual Reveal presents incorrect original strings in two centered, naturally wrapping lines
  before Word. Only evaluator-classified operation spans are emphasized; exact answers omit the
  redundant comparison and no synthetic per-character layout is introduced.
- Scheduler, FSRS, Planner, queue, Session policy, persistence, other learning modes, and frozen
  capability-design artifacts remain unchanged. Product Owner Manual UAT is still required;
  automated verification does not declare UAT PASS.

### PLE-036-C1 Canonical Typing Answer Authority

- Manual Desktop UAT showed that the prompt extractor joined every answer text block, making
  pronunciation/POS and Vietnamese meaning part of the expected typing value.
- Typing prompt extraction now selects only the first `PRIMARY_ENGLISH` block in deterministic
  question-then-answer order and never falls back to metadata, meaning, examples, or neutral text.
- The existing evaluator and automatic success orchestration are unchanged: the corrected prompt
  restores neutral prefixes, exact debounce/audio/GOOD progression, and localized mismatch
  styling. Realtime incorrect prose is no longer rendered.
- Reveal remains always available. Its item-scoped comparison authority remains latest raw input
  versus the canonical prompt, before Word, with naturally wrapping text and accessible operation
  semantics. Product Owner Manual UAT is still required; automated verification does not declare
  UAT PASS.

### PLE-036-C2 Positional Live Feedback and Rating-Ready Auto-GOOD

- Live editable feedback now compares the evaluator's normalized Unicode code points by typed
  position; Reveal alone retains Levenshtein insertion/deletion/replacement alignment.
- The automatic completion request carries rotation Session/item identity plus input revision.
  One guarded ViewModel operation invokes a Facade boundary that reveals the authoritative item,
  synchronizes the flow coordinator to rating-ready, and only then dispatches existing GOOD.
- `ReviewWorkspaceState.Question` remains unable to Rate. A stale/duplicate request cannot rate a
  newer item, and failure clears the Compose success lock; an already revealed item can safely
  retry completion without a duplicate review.
- Scheduler, FSRS, queue, persistence, manual Reveal/rating, and frozen capability-design
  artifacts remain unchanged. Product Owner Manual UAT is still required; automated verification
  does not declare UAT PASS.

### PLE-036-C3 Integrated Typing Comparison Header

- Manual Reveal comparison is now an optional section of the existing Vocabulary identity
  header: localized `You typed`, typed text, compact divider, the sole canonical Word renderer,
  then the unchanged audio/IPA/POS row.
- Typed and canonical lines use the same responsive answer-header typography and natural wrapping.
  Replacement uses danger/success underline, insertion adds strike-through, and deletion
  highlights only the missing canonical segment while the correct typed prefix remains neutral.
- Rendering requires comparison correct-answer identity to equal the canonical Word; mismatch
  suppresses comparison safely. Accessibility exposes typed/canonical values and grouped
  replacement, insertion, or missing operations.
- Manual Rating Dock, empty Reveal, positional live feedback, atomic rating-ready auto-GOOD,
  scheduler, FSRS, queue, Session, persistence, and frozen capability-design artifacts remain
  unchanged. Product Owner Manual UAT is still required.

### PLE-037 Typing-First Review and Success Focus

- Product Brain strategy selects a Typing primary mode for REVIEW, RELEARNING, and MASTERED
  plans that contain an eligible canonical Typing prompt. The template contains one Typing
  experience followed by Answer Reveal and Rating Ready; Desktop never advances it automatically.
- NEW/introduction behavior remains standard, and plans without Typing retain rotated image,
  listening, or prompt primary selection. Flow progress derives one Typing experience as 1/1 and
  the pre-answer Action Dock stays hidden.
- Typing input autofocus is keyed by item/prompt identity and enabled lifecycle. The responsive
  multiline input preserves the real editable control and live visual transformation; centered
  filled Reveal retains click, Enter, and IME Done behavior.
- Typing front suppresses manual primary-answer audio rendering without removing its path.
  `successInProgress` alone presents the root canonical-English overlay; after a rendered frame,
  the existing audio completion/failure and dwell sequence invokes the established atomic
  rating-ready → GOOD boundary exactly once.
- Manual Reveal comparison/audio/rating, C2 positional feedback, scheduler, FSRS, queue, Session,
  persistence, other Study modes, and frozen capability-design artifacts remain unchanged.
  Product Owner Manual UAT is still required.

### PLE-037-A Typing Front-Side Focus Polish

- Typing Question passes an explicit `SUPPRESS` manual-scene-audio policy, removing primary,
  meaning, example, supporting, image, and legacy `OTHER` audio affordances without deleting
  media paths. Other Question modes and the revealed Answer Surface retain their audio controls.
- Typing recommendation requires available Vietnamese meaning visibility and one-shot autoplay
  across every presentation control mode. `StudyAutoplayCoordinator` remains the authority and
  deduplicates `(itemId, QUESTION_BOUND)`; missing audio is a safe no-op.
- Typing alone suppresses the Meaning heading. Meaning/POS use centered, naturally wrapping,
  responsive typography; the English input uses larger resolved height, typed text, placeholder,
  and label sizes while retaining the actual editable control and keyed autofocus.
- Success overlay, English answer audio, atomic GOOD, Next/Completion, Manual Reveal comparison,
  flow strategy, scheduler, FSRS, queue, Session, persistence, and frozen capability-design
  artifacts remain unchanged. Product Owner Manual UAT is still required.

### PLE-037-B PATCH Enlarge and Vertically Center Typing Input

- `TypingPresentationResolver` owns responsive 48/43/38sp SemiBold typed text with 56/51/46sp
  line height, 40/36/32sp placeholder text with 48/44/40sp line height and 0.70 alpha, centered
  alignment, and zero letter spacing for Wide/Standard/Compact.
- `TypingRecallInput` applies that policy to the existing `OutlinedTextField` and placeholder.
  Short text uses Material single-line vertical centering; explicit newlines or input beyond 24
  Unicode code points use multiline 2–5 wrapping. The existing 116/106/96dp field heights are
  unchanged. No character grid, input overlay, offset, glyph animation, whitespace mutation, or
  duplicated state was introduced.
- Raw `TextFieldValue`, identity offset mapping, selection/composition, multiline wrapping,
  autofocus, paste, IME/Enter Reveal, positional live diff, success overlay/audio/GOOD/Next,
  meaning autoplay, Manual Reveal/comparison, and frozen capability-design artifacts remain
  unchanged. Product Owner Manual UAT is still required.

### PLE-038 Typing Attempt Measurement and Automatic Rating

- `TypingAttemptTimeSource` uses JVM monotonic time in production and explicit deterministic
  timestamps in tests. Item-scoped immutable attempt state measures first-input latency, typing
  duration, total elapsed, committed input, positional mismatch, and correction evidence; timer
  ticks are presentation-only.
- `TypingAutoRatingPolicy` normalizes expected time by canonical code-point count. Reveal is
  always Again. Exact attempts can be Hard, Good, or conservatively Easy only for reliable
  Review/Mastered context without a previous Again or error evidence.
- Exact and Reveal paths use separate requests. `StudyFacade` validates current rotation,
  attempt generation/evidence, item origin/stage/previous rating, and recomputes the decision
  before calling the existing review transaction. Duplicate or stale callbacks cannot review a
  new item.
- Manual Typing Reveal projects `FORCED_AGAIN`; Answer Surface and C3 comparison remain visible,
  while one localized Continue action replaces free ratings. Enter/NumPad Enter/Space/1/2/3/4,
  quick-action remnants, and legacy rating callbacks converge on Again. Failure retains a
  retryable pending mode.
- Attempt metrics and tokens are transient and restart with the current Desktop state lifecycle.
  Only final `ReviewEvent.rating` persists. Scheduler, FSRS, Queue, Session, persistence schema,
  canonical answer, live diff, comparison, meaning autoplay, success audio, and input typography
  remain unchanged. Window-focus pausing and historical Typing analytics remain future work.

### PLE-032-B1 Application Continuation Boundary

- `ContinueGeneralStudyUseCase` validates one authoritative finished general predecessor,
  learner, and package/topic scope, then coordinates existing ordinary Session creation and
  queue planning.
- Explicit outcomes distinguish an accepted next ordinary `StudySession`, Planner-owned no-work,
  and rejected invalid requests.
- The deterministic predecessor-derived next `SessionId` prevents sequential and
  restart-visible repeated calls from accepting a second Session. Atomic concurrent creation is
  not guaranteed by current repositories and remains a documented durability gap.
- `StudyFacade.continueGeneralStudyAfterCompletion()` is now a thin application adapter and no
  longer purges the completed predecessor or its retained latest-review Undo evidence.
- B1 adds no FlowId, Continuous Review persistence, automatic continuation, Stop, recovery
  redesign, scheduler/Planner replacement, queue model, or cross-Cycle Undo decision.

### PLE-030 Realtime Study Header Statistics

- Exact package, lesson or multi-content session scope feeds one application projection over
  unique enabled items, persisted review events and memory states.
- New/Review partitions on completed events; Again/Hard/Good/Easy uses the latest event per item.
  Undo and successful review/content mutations re-query source truth rather than editing counters.
- Due reuses `MemoryState.isDue()` with the query clock. The nearest future due instant schedules
  one Desktop refresh; there is no continuous polling.
- Header renders two compact localized metric rows with LETheme colors, last-known-good failure
  behavior and one merged accessibility description. PLE-030 is now FINAL PASS.

### PLE-030.1 Corrected Session Semantics

- Total is learned-state only and equals the latest Again/Hard/Good/Easy bucket sum.
- New is the session's committed unique New completion count over its frozen configured target.
- Review is the committed unique Review Content count over its frozen configured target;
  effective workloads are modeled separately and may be lower than configured maxima.
- Queue exhaustion already owns session completion. Continue Learning creates a fresh session and
  queue. Dedicated continuous Due/rating-fallback Review Mode is not implemented and remains
  separate proposed scope.

### PLE-030.2 Compact Statistics Dashboard

- One non-interactive secondary surface renders eight ordered, equally weighted metric segments.
  Existing viewport authority selects one 8-column row for Standard/Wide and 4+4 for Compact.
- Semantic 13sp label, 28sp value and 11sp subtitle roles establish the hierarchy. New/Review
  numerators are separate from muted denominators; zero Due/rating/progress values are muted.
- Existing Light/Dark metric families and eight LE icon roles provide color and icon identity.
  Subtitles are omitted in Compact, and localized accessibility describes fractions by meaning.
- Updates are immediate and dimensionally stable. No animation was added because the repository
  has no reduced-motion authority; no new motion policy was invented.
- Statistics/session business semantics and loading/last-known-good behavior are unchanged.
  This remediation is included in PLE-030 FINAL PASS.

### PLE-030.3 Immutable Session Classification and Review Cues

- Every newly planned queue persists its admission-time `SessionItemOrigin`; review counters,
  Undo/restart and header remaining workload consume that immutable fact. Schema-v1 queues use a
  compatibility fallback only.
- A session identity advances its New or Review counter only on its first completion. Re-rating
  does not increment either counter, and Undo validates/restores the matching prior state.
- Desktop resolves the latest persisted rating outside Compose. REVIEW underlines one matching
  rating label with an accessibility explanation; NEW or missing history has no indicator.
- The revealed rating state no longer repeats REVIEW/Answer Ready. The existing visual resolver
  reserves the statistics header and rating dock before answer media sizing; no second viewport
  authority was introduced.
- Scheduler/FSRS, queue order, rating actions, shortcuts, callbacks, review transaction
  atomicity and continuous Review Mode scope remain unchanged. This remediation is included in
  PLE-030 FINAL PASS.

### PLE-030.4 Content-Level Progress Identity

- `ContentLearningStateQueryService` is the sole learned-state/latest-rating authority for a
  learner and Content across sibling LearningItems.
- Fresh queues persist LearningItem execution ID, Content progress ID and learner-facing origin;
  schema v1/v2 queues remain readable and are projected safely without reset.
- Unique Content owns New/Review quota, completed counters, Total and one latest-rating
  bucket. LearningItem continues to own MemoryState, scheduler, event and experience execution.
- Desktop obtains previous Content rating outside Compose. Scheduler diagnostics may remain NEW
  while learner-facing origin/context is REVIEW.
- Same-session content anti-repetition, single-item behavior, scheduler/FSRS and continuous
  Review Mode scope are unchanged. This remediation is included in PLE-030 FINAL PASS.

### PLE-029 Highlight and Audio Shortcut Boundary

- One pure matcher maps NFC/case/apostrophe/hyphen/whitespace-normalized matches back to exact
  original display ranges, validates lexical boundaries and selects longest non-overlapping
  occurrences for English and Vietnamese.
- Semantic inflection and Vietnamese accent folding remain intentionally unsupported. Aggregate
  runtime evidence and unresolved classes are in `reports/PLE-029_HIGHLIGHT_AUDIT.md`.
- ShortcutRegistry now owns four additional configurable commands with L, Shift+L, V and Shift+V
  defaults. Legacy persisted registries preserve existing mappings and fill missing commands.
- Study routing invokes the existing audio controller only: English vocabulary/example toggle
  loop, Vietnamese meaning/example play once, and R remains one-shot primary replay.
- Product Owner Settings/Study interaction and visual UAT remains pending.

### PLE-028E POS Semantic Registry

- Application-owned extraction/canonicalization recognizes case-insensitive custom fields,
  `pos:` tags, and combined pronunciation while preserving unknown canonical text.
- One registry is reconciled from `ContentRepository` at startup and updated after successful
  package import. It never writes POS or color metadata into package/content/learning state.
- Known identities are fixed; unknown identities use locale-independent UTF-8 SHA-256 and a
  bounded collision-probed visual slot. No registry persistence was added because the canonical
  key deterministically owns its identity; active assignments remain stable in runtime.
- Theme owns only Light/Dark palette tokens. The design-system POS adapter maps application
  identities into tokens; Study composables neither scan repositories nor contain color maps.
- Exact aggregate evidence is in `reports/PLE-028E_POS_INVENTORY.md`. Manual visual UAT remains
  pending and the finite palette does not promise a globally unique hue for every future value.

### PLE-028D.2 Final Re-UAT Remediation

- Question and Answer Meaning use one wrapping, vertically centered `StudyMeaningPosGroup` and
  the already-normalized `answerModel.partOfSpeech`; no content normalization was duplicated.
- Dedicated semantic typography roles improve POS and rating action readability. Rating order,
  labels, shortcuts, callbacks, enabled authority, and dimensions remain unchanged.
- English and Vietnamese Example rows retain their semantic containers during hover and resolve
  content, highlight, icon, and focus/active borders entirely from `LETheme` tokens in both
  themes. Existing click, play-once, and loop behavior remains unchanged.
- PLE-028D.1 viewport, rating-dock reservation, image caps, scrolling fallback, and
  `ContentScale.Fit` contracts are untouched. Final manual visual re-UAT remains pending.

### PLE-028D.1 UAT Remediation

- POS uses a Study semantic accent container/content/medium-border style in both themes and the
  same badge appears after Vietnamese meaning through wrapping presentation; the existing
  normalized POS remains the only content authority.
- Flow-ready helper text uses `textSecondary` rather than the Dark accent. The audio-clickable
  Answer identity retains its semantic primary surface through hover/press; focus and active
  loop remain visible through semantic borders/content.
- `StudyVisualLayoutResolver` now includes rating-dock reservation (88dp horizontal, 144dp
  grid) in its vertical image budget. Standard/Wide images no longer grow beyond 620x240dp;
  Compact stays width-adaptive and `ContentScale.Fit` is unchanged.
- Scheduler/FSRS, review/persistence, queue/session, keyboard, callbacks, audio ownership and
  responsive authority are unchanged. Manual visual re-UAT remains pending.

### PLE-028D Study Theme Boundary

- The Study composition uses the neutral `LETheme` window canvas and semantic answer, Meaning,
  Example, Scheduler, rating-dock, and secondary surfaces.
- Header Pause/Undo and the exact Again/Hard/Good/Easy dock use `LEButton`; semantic button
  styles own readable content, disabled state, hover/pressed projection, and tokenized focus.
- `StudyVisualLayoutResolver` remains the only viewport authority. Rating order, callbacks,
  enabled rules, keyboard routing, scheduler/FSRS behavior, interval presentation, image
  loading, and `LearningContentAudioController` ownership remain unchanged.
- Static guards cover direct Material color authority, raw colors, theme resolution, responsive
  duplication, and behavior-layer dependencies in the migrated presentation.

### PLE-028B.1 Theme Authority

- `LearningEngineTheme` is the single theme entry point.
- `ThemeResolver.kt` is the sole authority for `resolveDarkTheme`, LE token selection,
  CompositionLocal provisioning, and Material adaptation.
- `LearningTheme` retains its existing public signature as a logic-free adapter used by
  `App.kt`; no duplicate implementation remains.
- The Material compatibility palette, `LearningTypography`, and `LearningShapes` remain
  unchanged for current production consumers, so the remediation does not migrate screens or
  alter visual/business behavior.

### PLE-028B Completion Boundary

- `LETheme` is the only public component-facing token façade; CompositionLocals and resolver
  state are internal.
- One immutable `ResolvedLETheme` deterministically supplies every token group and the Material
  adapter from the theme preference boundary.
- All PLE-028A token groups are implemented and covered by a durable architecture/test gate.
- PLE-028B introduced no screen migration and makes no visual UAT claim.
- Test discovery audit found the Gradle lifecycle runs `:test` plus `:desktop:test`; no tests
  were deleted between PLE-028A and remediation. The earlier 2,504 count was an unsupported
  pre-verification claim; 2,484 was the XML-verified remediation baseline. PLE-028B completion
  retained 510 suites and added four discovered tests for a verified total of 2,488.

### PLE-028C Base Component Boundary

- `ui.designsystem.components.base` owns small LETheme-only primitives and pure state resolvers.
- Delivered `LESurface` (`PRIMARY`, `SECONDARY`, `ERROR`) and `LEButton` (`PRIMARY`,
  `SECONDARY`, `DESTRUCTIVE`) with token-derived focus, motion, shape, spacing, and density.
- Controlled consumers: `DesktopLoadStateCard` and `SearchScopeCard`; retry callback, enabled
  authority, labels, live regions, and descriptions remain unchanged.
- Legacy Studio/Study controls remain compatibility scope. No Study answer/rating migration,
  scheduler, persistence, audio, navigation, keyboard, or visual UAT claim is included.
- Focused selection passed 60 tests. Full gate: root `:test` 345 suites / 1,663 tests and
  `:desktop:test` 166 suites / 833 tests; total 511 XML suites / 2,496 passed, 0 failed,
  0 errors, 0 skipped.

### Final Established Architecture (Post-PLE-027C)

- **Technical UAT Verification Matrix**:
  - Verified across 15 content/item variants and 3 viewport classes (`COMPACT`, `STANDARD`, `WIDE`).
  - 100% reachability preserved across vertical scroll, zero horizontal overflow, deterministic rating grid (2x2) at <= 479dp, horizontal 4-button dock at >= 480dp.
  - Zero answer leakage, zero interaction regressions (`[R]`, Space, 1-4, Ctrl+Z, Esc, loop playback).

- **Answer Surface Visual Hierarchy**:
  - Word Identity (primary focal point) > Meaning Card & Image Viewport > Example Card > Pronunciation Metadata Group > Compact Scheduler Feedback & Action Dock.
  - Pronunciation metadata (Audio button, italic IPA, POS status badge) unified into a single group supporting `INLINE` and `STACKED` arrangements, collapsing cleanly when optional fields are missing.
  - Meaning card provides primary explanation block with Vietnamese meaning bold/semi-bold and English definition on separate row underneath.
  - Bilingual examples preserve distinct typography hierarchy, exact word-boundary semantic target highlighting (`SpanStyle(color = LEColors.danger, fontWeight = FontWeight.Bold)`), and clean collapse for missing translation/audio.

- **Responsive Visual Layout Contract**:
  - `StudyVisualLayoutResolver` is a pure Kotlin, deterministic resolver without Compose imports or side effects.
  - Classifies viewports into `COMPACT` (< 600dp), `STANDARD` (600 - 1023dp), and `WIDE` (>= 1024dp).
  - Centralizes 100% of responsive rating decisions (`RATING_GRID_MAX_WIDTH_DP = 479`) in `StudyVisualLayoutResolver`. No `maxWidth <` checks remain in Compose.
  - Bounds wide content width (800dp) with centered alignment, calculates responsive word identity typography, scales image max bounds conservatively for short viewport heights (< 600dp), and provides `MetadataArrangement` (`INLINE`/`STACKED`) and `RatingArrangement` (`HORIZONTAL`/`GRID_2X2`).

- **Source of Truth & Scheduler Semantics**:
  - Scheduler, FSRS, review history, `MemoryState`, and persistence operate strictly at the `LearningItem` level (`(learnerId, learningItemId)`).
  - Multiple `LearningItem`s for the same `Content` maintain independent memory states and scheduling queues.

- **Presentation & Study Badge**:
  - Learner-facing stage badge on `StudyScreen` is a Content-level projection (`contentPresentationStage` via `ContentStageQueryService` / `engine.getContentPresentationStage(learnerId, contentId)`).
  - `learningStage` (current `LearningItem` stage) is strictly separated from `contentPresentationStage` and used for learning strategy/experience/scheduler diagnostics.

- **Semantic Highlighting**:
  - Highlighting semantics: exact word boundary, case-insensitive across English and Vietnamese, punctuation-tolerant, multi-word phrase matching, common inflections (`s`, `'s`, `ed`, `ing`, `es`), and canonical infinitive normalization (`"to + verb"` -> `"verb"`, e.g. `"to sign"` -> `"sign"`).
  - No fuzzy matching. Red bold text emphasis (`LEColors.danger` + `FontWeight.Bold`).

- **Next Phase Goals (`PLE-027: Study Experience Visual Polish`)**:
  - Visual hierarchy, typography, image viewport, meaning card layout, example layout, scheduler feedback polish, rating dock, and responsive desktop layout.
  - Unchanged: Scheduler, FSRS, Queue Planning, Persistence, Learning semantics.

## PLE-026-R8 continuation

- Baseline: clean `develop` at `8d3f1a6`, thirteen local commits ahead of `origin/develop`.
- Projected Study Screen stage badge from Content-level learning history via `ContentStageQueryService` (`engine.getContentPresentationStage`), resolving stage across multiple `LearningItem` modes for a single `Content`.
- Explicitly separated `learningStage` (authoritative `LearningItem` stage for scheduler/diagnostics) and `contentPresentationStage` (Content-level stage for learner-facing badge).
- Preserved independent MemoryState per LearningItem, FSRS scheduler selection, and infinitive verb highlighting.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,447 passed, 0 failed, 0 errors; `git diff --check` clean. No push is authorized.

## PLE-026-R7 continuation

- Baseline: clean `develop` at `44278b4`, eleven local commits ahead of `origin/develop`.
- Rehydrated `StudyFacade.currentItem` in `StudyFacade.load()` across legacy/compatibility, lessonStudy, and active package branches by querying `applicationContext.engine.getMemoryState(...)` prior to projecting `StudyUiState`. Stale cached `MemoryState.stage` is eliminated; item identity, session status, and reveal state are strictly preserved without UI heuristics.
- Standardized English infinitive target normalization (`to + verb` -> canonical verb, e.g. "to sign" -> "sign") in `ExampleTargetHighlighting`. Exact word boundary checks ensure substring targets like "signature" do not match.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,443 passed, 0 failed, 0 errors; `git diff --check` clean. No push is authorized.

## PLE-026-R6 continuation

- Baseline: clean `develop` at `ec68285`, nine local commits ahead of `origin/develop`.
- Desktop Stage Badge now projects the authoritative learning stage (`NEW`, `LEARNING`, `REVIEW`, `RELEARNING`, `MASTERED`, `SUSPENDED`) directly from `LearningSceneContext` / `StudyUiState` / `MemoryState`, rendering each stage with its corresponding status badge variant without UI heuristics or inferring stage from counts/history.
- Semantic target highlighting replaced lavender background fill with bold + deep red text emphasis (`LEColors.danger` + `FontWeight.Bold`). Target phrase matching supports case-insensitivity across English and Vietnamese, multi-word phrases (e.g. "at the bottom."), surrounding punctuation cleanup, common inflected endings (`s`, `'s`, `ed`, `ing`, `es`), and multiple occurrences without fuzzy matching.
- Full verification: `.\gradlew.bat clean test` — 2,440 passed, 0 failed, 0 errors; `git diff --check` clean. No push is authorized.

## PLE-026-R5 continuation

- Baseline: clean `develop` at `d19ee10`, seven local commits ahead of `origin/develop`.
- Adaptive leakage came from a global show-all recommendation combined with unconditional
  meaning append in Question projection. Recommendation is now derived from the current
  experience/selection; Listening separates primary audio from hidden identity, Image has no
  answer block, and rejected support is absent from render/accessibility/autoplay availability.
- Full Answer media now resolves from the complete current-item presentation, not the sanitized
  Question. Truth-mode Reveal plays primary English once and ignores Question visibility/
  autoplay switches; recovery, recomposition, resize, Apply, stale transition, or missing
  primary does not replay or fall back to Vietnamese.
- Source review reproduced a stage semantic inconsistency: production selection creates an
  effective NEW `MemoryState`, while `NextLearningItem.isNew` checked nullability. Effective
  `MemoryState.stage` now owns NEW/LEARNING/REVIEW classification; persisted existence is
  explicit and carried into non-content stage diagnostics and the Desktop badge projection.
- Scheduler/FSRS, Product Brain, Learning Strategy, rating evidence, queue policy, persistence
  schema, runtime preference schema, Quick Controls, shortcuts, typography, package progress,
  and highlight matching are unchanged. Manual audio/stage UAT remains pending. No push is
  authorized.

## PLE-026-R4 continuation

- Baseline: clean `develop` at `cfe6b55`, six local commits ahead of `origin/develop`.
- Root cause: after semantic Question filtering was corrected, the revealed focused answer still
  reused `EffectiveStudyPresentation`, so Question visibility switches also removed answer
  identity, pronunciation, part of speech, meaning, and examples.
- `FullAnswerPresentation` now derives disclosure solely from available answer content.
  `FocusedAnswerSurface` no longer consumes Question visibility; Question scene rendering and
  `StudyPresentationPolicy` are unchanged.
- English and Vietnamese example targets are matched exactly at the Desktop presentation
  boundary and rendered as annotated text. English is case-insensitive; word boundaries,
  multiple occurrences, and multi-word phrases are supported without fuzzy matching or content
  mutation.
- Scheduler, FSRS, Product Brain, Learning Strategy, queue/review evidence, persistence,
  shortcuts, typography, package progress, Quick Controls, and autoplay/manual audio semantics
  remain unchanged. Representative manual/physical-audio UAT remains pending. No push is
  authorized.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,398 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean.

## PLE-026-R3 continuation

- Baseline: clean `develop` at `b5b2a43`, five local commits ahead of `origin/develop`.
- Root cause: presented text had no semantic role, so `LearningSceneRenderer` inferred language
  from scene type and reveal state; Listening also bypassed sanitized Question projection.
- `LearningTextRole` is assigned from canonical content slots in Application and explicitly maps
  to required Desktop `PresentedTextRole`. Renderer visibility now depends only on role and the
  effective presentation.
- Study projects all available semantic blocks, while scene projection still owns which blocks
  belong to Question/Answer. Listening and Image use the same semantic visibility model.
- Scheduler, FSRS, Product Brain, Learning Strategy, review evidence, persistence schemas,
  Quick Controls, shortcuts, typography, package progress, and manual audio semantics are
  unchanged.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,390 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean. Representative manual UAT remains pending. No push is
  authorized.

## PLE-026-R2 continuation

- Baseline: clean `develop` at `4fa28ab`, four local commits ahead of `origin/develop`.
- Root cause: `StudyPresentationPolicy` used `answerRevealed` to suppress visibility and autoplay,
  mixing preference resolution with Question/Answer workspace ownership.
- `StudyPresentationAvailability` and the pure policy no longer contain reveal or review state.
  Renderers select the layers valid for their phase from one phase-independent effective model.
- `StudyAutoplayCoordinator` owns transition-specific playback. Adaptive keeps its prior
  Question-silent/Reveal-English baseline; Manual may play visible available Vietnamese meaning
  on Question without changing reveal, rating, or manual playback semantics.
- Scheduler, Product Brain, Learning Strategy, review evidence, queue/session persistence,
  shortcuts, typography, and package progress remain unchanged.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,386 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean. Representative manual UAT remains pending. No push is
  authorized.

## PLE-026-R1 continuation

- Baseline: clean `develop` at `678354d`, three local commits ahead of `origin/develop`.
- Manual UAT found that the focused answer identity ignored `showPrimaryEnglish`, and changing
  presentation during Study required a Settings round trip.
- The focused answer now uses the same `EffectiveStudyPresentation` for word/IPA/POS, meaning,
  examples, semantics, and autoplay eligibility.
- `StudyPresentationStagingState` keeps the active item snapshot separate from the persisted
  next-item preference. It lives in `ContentHost`, so opening Settings does not discard the
  snapshot; both Settings and the Study-header menu use the same runtime persistence callback.
- Scheduler, Product Brain, Learning Strategy, review evidence, queue/session persistence,
  manual audio interaction, shortcut bindings, typography, and package progress are unchanged.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,380 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean. Representative manual UAT remains pending. No push is
  authorized.

## PLE-026 continuation

- Baseline: clean `develop` at `0d09f70`, two local commits ahead of `origin/develop`.
- `StudyPresentationPreferences` persists Adaptive, Preference Guided, and Manual control plus
  bilingual visibility/autoplay switches through optional schema-v1 runtime properties.
- Settings owns a draft and explicit Apply. The preview is silent; Adaptive disables switches
  and preserves the existing Product Brain/projected presentation as its recommendation.
- One pure Desktop policy combines recommendation, preference, reveal state, content
  availability, and resolved media. Primary English cannot be hidden; hidden or unavailable
  support cannot autoplay. Autoplay is transition-keyed, so Apply cannot replay the current item.
- Scheduler, FSRS, Product Brain planning, learning evidence, queue/review, packages, learning
  persistence, typography, and shortcut behavior remain unchanged.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,376 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean. Representative manual/physical-audio UAT remains a
  Phase 7 external gate. No push is authorized.

## PLE-025B continuation

- Baseline: clean `develop` at `c25a8b9`, one commit ahead of `origin/develop`; PLE-025A is the
  accepted baseline.
- Root cause: Study key mapping and the status strip directly encoded Space, 1–4, R, Ctrl+Z,
  and Escape, preventing user configuration and allowing presentation/routing drift.
- Compose-independent chords, commands, bindings, and an immutable duplicate-free registry now
  own mapping and deterministic serialization. Compose key types stop at one adapter.
- Typed runtime configuration persists the registry; missing or invalid optional data falls
  back atomically to defaults. Settings supports capture, preview, Save/Cancel, conflict
  Cancel/Swap/Replace, per-command reset, and Restore Defaults.
- Study routing still applies the existing workspace-state, busy, repeat, text-input, undo,
  audio-availability, and active-session gates. Scheduler, FSRS, strategy, scenes, package
  progress, typography, highlight, and audio-controller behavior are unchanged.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,365 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean. No push is authorized.

## PLE-025A continuation

- Baseline: clean `develop` synchronized with `origin/develop` at `663d3c0`.
- Root cause: revealed Study examples used fixed `18sp` English and `16sp` Vietnamese sizes;
  runtime configuration and presentation had no learner-owned typography policy.
- `StudyTypographyPreferences` now owns validated English/Vietnamese example bases and persists
  through schema-v1 optional properties. Legacy files load 20sp/16sp defaults.
- Settings uses a local preview draft and explicit Apply. The current runtime configuration
  reaches Study through `ContentHost`; one presentation resolver preserves configured bases at
  fullscreen and narrow widths and enables wrapping.
- Scheduler, learning strategy, audio behavior, review, scene projection, package progress,
  persistence of learning state, and shortcut/keyboard behavior are unchanged.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,361 passed, 0 failed, 0 errors,
  0 skipped; `git diff --check` clean. No push is authorized.

## PLE-024-R2 continuation

- Baseline: clean `develop` at `fd1a9db`, two commits ahead of `origin/develop`.
- Scope is Desktop-only audio discoverability: shared interaction feedback, revealed
  word/image primary loops, and front image primary single-play through the existing controller.
- Image-recall projection carries only typed `PRIMARY_WORD` audio; typed meaning/example roles
  cannot become front replay. All-`OTHER` legacy scenes retain the established first-audio
  fallback.
- Scheduler, review, persistence, OPD3, package lifecycle, and navigation are unchanged.
- Full verification: `.\gradlew.bat clean test --no-daemon` — 2,355 passed, 0 failed,
  0 errors, 0 skipped; `git diff --check` clean.
- No push is authorized.

## PLE-024-R1 continuation

- Baseline: clean `develop` at `f703b74`, one commit ahead of `origin/develop`.
- The authoritative audio source is the existing five-slot `ContentMedia` contract. Typed roles
  now survive Application and Desktop projection without localized-label matching.
- Persisted real data contains 990 `Vocabulary_In_Use_Elementary` records; the tested Study item
  exposes primary, translated-meaning, English-example, and translated-example audio refs.
- Manual UAT confirmed separate meaning/EN/VI surfaces and active English-example loop styling.
- No persistence or OPD3 migration is required; no push is authorized.

## PLE-024 continuation

- Baseline: clean `develop` synchronized with `origin/develop` at `b417a7d`.
- PLE-024 is in final verification. The existing Study scene/audio boundaries remain authoritative;
  only Desktop resolver and Compose presentation changed.
- Manual UAT on `Vocabulary_In_Use_Elementary` confirmed a one-screen revealed answer with the
  fixed semantic rating dock and human-readable GOOD feedback (`2 ngày`).
- No scheduler, review, session, persistence, OPD3, or audio-loop semantics changed.
- No push is authorized.

## PLE-023 continuation

- Baseline: clean `develop` at `d26bdc3`; `origin/develop` remains at `3a7684b`.
- PLE-023-R1 is in final verification. Durable `ReviewEventRepository` is the authoritative
  rating source; the batch application query selects the newest event per current package item.
- No persistence migration is required. Legacy items without review events are unrated, while
  uninstall ownership deletion removes their events through the existing repository contract.
- The card now uses approved readable sizing, a balanced one-row metric layout, lavender
  progress surface, four semantic rating chips, and primary/secondary/danger toolbar hierarchy.
- No push is authorized.

## PLE-022 continuation

- Baseline: `develop` at `d7c606f`, synchronized with `origin/develop`.
- PLE-022A is committed locally as `8092363`.
- PLE-022B replaces the fire-and-forget loop attempt with path-specific completion, an owned
  cancellable replay scheduler, and generation guards. EN vocabulary/examples toggle loop;
  Vietnamese meaning/examples single-play. Current runtime delay is passed explicitly through
  `ContentHost` to `StudyScreen`.
- PLE-022A adds background, batch package-progress projection and backward-compatible
  configurable new/review session limits. Desktop composition maps current preferences to
  `SessionPolicy` only for a newly created session; persisted active/resumed snapshots remain
  authoritative.
- PLE-022B reliable interactive audio loop is next and must start only after PLE-022A has a
  successful clean build and clean local capability commit.
- No push is authorized.

## Repository

- Repository: `loilephuoc/LearningEngine`
- Branch: `develop`
- Local HEAD at this handoff baseline: `e98ade8` (PLE-021C-R2 bulk learning-item removal;
  PLE-021E-R1 implementation and this context update follow in the next local commit).
- Upstream: `origin/develop`; baseline is synchronized and the PLE-021E-R1 commit remains local
  and intentionally unpushed.
- Working tree: clean.
- Recent commits:
  - `e98ade8 fix: bulk delete learning items during topic removal`
  - `4154774 fix: keep topic removal persistence off UI thread`
  - `f260566 fix: prevent stale study restore and async topic removal`
  - `ea29ffc docs: record study lifecycle reconciliation after reimport`
  - `1cf2157 fix: reject stale study completion after package reinstall`
  - `6a8c8d4 docs: record authoritative import ownership remediation`
  - `35321c3 fix: repair orphan content ownership across uninstall and reimport`
  - `b52211f docs: record stale study restore root cause fix`
  - `9a03c90 fix: eliminate stale study restore without active package`

## Capability Contracts

- Before implementing a named capability, inspect [`architect/`](architect/).
- Integrated canonical architect contracts:
  - [`architect/README.md`](architect/README.md) — Architect Library guidelines and workflow.
  - [`architect/PLE-018A_CONTENT_STUDIO.md`](architect/PLE-018A_CONTENT_STUDIO.md) — Content Studio specification contract.

## Phase State

- Phase 5 — Desktop Beta Readiness: implementation/local automation complete; Product Owner verification pending.
- Phase 6 — Learning Experience: implementation complete through P6-10; PLE-020 complete.
- Phase PLE-021 — Modern Learning Workspace: PLE-021A, PLE-021B, PLE-021B-R1, PLE-021B-R3,
  PLE-021B-R6, PLE-021B-R7, PLE-021B-R8, PLE-021B-R9, and PLE-021B-R10 are COMPLETE.

## Current & Next Capabilities

- **PLE-021E-R1 — Continue General Study after Completion** is complete locally pending the
  capability commit:
  - General completion projects **Học tiếp** from explicit non-lesson scope and canonical
    package ownership; it does not require `contentId` or navigate through Library. Lesson
    completion retains its content-scoped actions.
  - Continue purges the completed session/queue from restore and Undo eligibility, then starts a
    UUID-distinct general session using the canonical ACTIVE package/topic and durable scheduler
    memory. Four reviewed items remain REVIEW while the next unseen candidate enters as NEW.
  - Empty next-session queues are deleted and projected as a clear idle/no-items state rather
    than another completion.
  - Learning-card stage badges use only `StudyUiState.learningStage`; scheduler feedback remains
    independently able to report transitions such as NEW to REVIEW.
  - Full verification: `.\gradlew.bat clean test` — 2,331 passed, 0 failed, 0 errors, 0 skipped;
    `git diff --check` clean.

- **PLE-021C-R2 — Bulk Learning-Item Removal** is COMPLETE (commit `e98ade8`):
  - `LearningItemRepository.deleteAllById` provides the bulk contract; in-memory removal updates
    keys directly and store-backed removal performs zero store access for an empty request,
    otherwise one load and at most one changed-state save.
  - `PackageUninstallOperation` passes the exact resolved ownership-plan learning-item IDs in one
    bulk call. It does not derive a broader deletion from content IDs and retains the established
    transaction and asynchronous Desktop lifecycle.
  - Regression coverage uses the UAT scale of 990 contents/4,950 learning items and proves one
    bulk mutation, zero repeated `deleteById` calls, and unrelated/shared item preservation.
  - Full verification: `.\gradlew.bat clean test` — 2,325 passed, 0 failed, 0 errors, 0 skipped;
    `git diff --check` clean.

- **UAT remediation — asynchronous Topic removal and complete legacy completion ownership** is
  complete on `develop`:
  - `ContentLibraryViewModel.uninstallPackage` is asynchronous through `DesktopTaskRunner`, uses
    the existing operation guard/Loading projection, returns to Idle on every terminal path, and
    performs both the uninstall mutation and single success reload on the worker dispatcher.
    Its success callback only publishes the reloaded state, resets presentation navigation, and
    invokes `onContentDataChanged`.
  - `LibraryScreen` closes the destructive confirmation before dispatch and does not issue eager
    duplicate refreshes.
  - Undoable completion repositories require FINISHED status plus non-null package/topic
    provenance. `StudyFacade` remains defense-in-depth authority and purges persisted legacy
    sessions/queues missing either ownership field.
  - Full verification: `.\gradlew.bat clean test` — 2,320 passed, 0 failed, 0 skipped;
    `git diff --check` clean.

- **PLE-021B-R10 — Study Lifecycle Reconciliation after Package Reinstall** is COMPLETE
  (implementation commit `1cf2157`):
  - Completed package sessions require the canonical `ACTIVE` InstalledPackage, exact package and
    topic provenance, `session.startedAt >= installedPackage.installedAt`, and queue item ownership
    within the current package. `ContentPackage` existence is no longer accepted as fallback
    lifecycle authority.
  - Invalid same-package completion records are purged with their queues; unrelated package
    completions are not deleted. Valid current-installation completion and Undo plus existing
    active/resumable recovery remain supported.
  - Orphan reimport now reconciles exact package learning lifecycle state inside the content
    import transaction: MemoryState, ReviewEvent, StudyQueue, and StudySession, including FINISHED
    sessions with Undo evidence.
  - Store-backed production-composition coverage reproduces import, four NEW reviews, completed
    session, orphan creation, restart, same deterministic InstalledPackageId reimport, fresh
    NEW/Discovery startup, and unrelated-session preservation.
  - Full verification: `.\gradlew.bat clean test` — 2,316 passed, 0 failed, 0 skipped;
    `git diff --check` clean.
  - Desktop composition started successfully against current persisted data. The execution
    environment cannot inspect or interact with the native Compose window, so the real-package
    visual UAT result remains unclaimed and requires Product Owner confirmation.

- **PLE-021B-R9 — Authoritative Import Ownership and Complete Uninstall Graph** is COMPLETE
  (implementation commit `35321c3`):
  - When `InstalledPackageRepository` is available, only its `ACTIVE` and `ARCHIVED` records
    establish installed-content conflict authority. An empty repository or only `REMOVED`
    records is authoritative empty state and never falls back to orphan `ContentPackage` rows.
    Legacy fallback is retained only for compositions where the canonical repository is absent.
  - Import validation resolves live content through installed package aliases and their matching
    `ContentPackage`/`ContentLibrary` graph. Orphan content IDs, learning-item IDs, and matching
    fingerprints therefore do not cause false conflict diagnostics.
  - Uninstall resolves and validates the complete immutable ownership/removal plan before its
    first mutation. A selected installed package with missing `ContentPackage` or owned
    `ContentLibrary` evidence fails early; successful removal deletes exact owned identifiers
    while preserving shared content and unrelated package state.
  - Store-backed coverage verifies live duplicate rejection, removed/empty authority,
    import-study-rate-uninstall-restart-reimport with fresh memory, orphan repair by deterministic
    upsert without duplicates, and repeated removal across restart.
  - Full verification: `.\gradlew.bat clean test` — 2,314 passed, 0 failed, 0 skipped;
    `git diff --check` clean.
  - Automated composition/store evidence is complete. Manual native-window UAT with the user's
    original real OPD3 package remains part of the Phase 7 external/manual validation gate.

- **Stale General Study restore remediation** complete on `develop` (commit `9a03c90`):
  - General Study resolves only an `ACTIVE` `InstalledPackage`; `ContentPackage`, archived,
    removed, legacy/unpackaged content, and all-items fallback are not active-topic authority.
  - Missing ACTIVE scope is rejected before cached/adaptive state, current item, or generic
    session recovery. General Study runtime is cleared and active General Study sessions plus
    their queues are purged, including null-package legacy sessions.
  - Restorable General Study sessions require canonical ACTIVE package provenance, matching
    package topic, and package-owned session/current/queue content.
  - Package uninstall deletes the actual resolved matching `ContentPackage.id` records and
    preserves unrelated package state.
  - Store-backed coverage verifies empty Library restart, same-process removal, clean reimport
    at NEW/Discovery, null-package session/queue deletion, and unrelated progress isolation.
  - Full verification: `.\gradlew.bat clean test` — 2,307 passed, 0 failed. Desktop smoke startup
    succeeded; native-window interaction was not observable from the execution environment.

- **PLE-021B-R8 — Authoritative Installed Package Authority for Study Restore & Startup** is COMPLETE:
  - **Commit (`4677971`): `fix: make active installed package authoritative for study`**
    - Made `resolveCanonicalActivePackageId()` the sole authority for General Study restore, startup, and navigation in `StudyFacade`.
    - If `canonicalPkg == null` (or uninstalled package), General Study invalidates in-memory study state, purges stale/uninstalled session state from persistence, and returns `createNoActiveTopicUiState()` (`"Chưa có chủ đề đang hoạt động\nHãy vào Thư viện và đặt một chủ đề làm Active trước khi bắt đầu học."`, Action: `"Đi tới Thư viện"`).
    - Eliminated empty-library fallbacks across `load()`, `startStudy()`, `startSession()`, resume paths, startup restoration, and dashboard shortcuts.
    - Extended `PackageUninstallOperation` to delete all owned `StudySession` and `StudyQueue` records across memory and store-backed persistence when a package/topic is removed.
    - Added `ActivePackageStudyAuthorityIntegrationTest` covering store-backed active package authority, session purging on uninstall, and empty library state.
  - **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL (**2302 tests passed across engine and desktop modules**). `git diff --check` clean. Working tree clean.
  - **Commit 1 (`5be9b74`): `fix: enforce active installed topic study scope`**
    - Enforced `InstalledPackageRepository` active package validation in `StudyFacade`. When no installed topic is active, General Study displays empty state `"Chưa có chủ đề đang hoạt động\nHãy vào Thư viện và đặt một chủ đề làm Active trước khi bắt đầu học."` with `"Đi tới Thư viện"` navigation button.
    - Updated `PackageUninstallOperation` to delete owned `MemoryState`, `ReviewEvent`, `StudySession`, and `StudyQueue` records across all persistence stores, guaranteeing clean `NEW` stage discovery upon package reimport.
  - **Commit 2 (`5262bcc`): `feat: orchestrate adaptive answer audio playback`**
    - Auto-plays primary English answer audio once on answer reveal transition.
    - Toggles English vocabulary identity audio loop on clicking identity surface.
    - Added user-configurable audio loop delay (`audioLoopDelaySeconds`, default 0.5s, range 0.0–10.0s) persisted in `DesktopRuntimeConfiguration` and editable in `SettingsScreen`.
    - Single-play for Vietnamese meaning audio (`MeaningCard`) and Vietnamese example translation (`VietnameseExampleAudioRow`), automatically stopping any running loop.
    - Split example audio surfaces into dedicated `EnglishExampleAudioRow` (`VÍ DỤ TIẾNG ANH`, toggles loop) and `VietnameseExampleAudioRow` (`BẢN DẢCH TIẾNG VIỆT`, single play).
    - Preserved single `LearningContentAudioController` playback authority with loop cancellation on item transition, pause, or disposal.
  - **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL (**654 passed, 0 failed**). `git diff --check` clean. Working tree clean.

  - **PLE-021B.1 (`c0e8c1f`):** Focused Vocabulary Answer Surface displaying large centered English word, inline compact audio/IPA/POS row, prominent adaptive prompt image (240dp max height), dedicated `MeaningCard` (Vietnamese primary, optional definition secondary), dedicated `ExampleCard` (English sentence primary, Vietnamese secondary, compact replay button), and `CompactSchedulerFeedback` (collapsed summary by default with `Chi tiết` toggle).
  - **PLE-021B.2 (`cb8a92e`):** Discovery Mode for New Vocabulary (`LearningStage.NEW`) rendering prompt image (if present), Vietnamese meaning cue, and explicit `Xem đáp án` action (Space / Enter) while keeping English answer, IPA, POS, examples, typing field, and rating dock hidden before reveal. `TYPING_RECALL` excluded from initial experience for `NEW` items.
  - **PLE-021B.3:** Applied LE Design System tokens and explicit accessibility semantics across all new composable surfaces.
  - Verification: `.\gradlew.bat clean test` — BUILD SUCCESSFUL (**626 passed, 0 failed**).
  - **Baseline Authority:** Existing Learning Flow architecture (`StudySession`, `StudyQueue`, FSRS, reveal/rating, `ProductBrainPlanner`, typing recall, audio) is the baseline authority and MUST NOT be duplicated, bypassed, or redesigned.
  - **Target:** First usable learning experience in 3–5 days; polished and stable completion in 5–7 days.
  - **Scope:** Desktop presentation, interaction, integration, and UAT capability — not a new learning algorithm initiative.


- **PLE-014 — Orphan Content Ownership Reconciliation during Package Reimport** complete on `develop`:
  - **Canonical Authority Re-alignment (`InstalledPackageRepository`):** Corrected `InstalledContentConflictValidator.kt` to start conflict validation strictly from `InstalledPackageRepository` (filtering `ACTIVE` and `ARCHIVED` packages), resolving canonical `PackageId`s, matching `ContentPackage`s, and live `ContentLibrary` content IDs.
  - **Orphan Content Exemption:** Guaranteed that legacy orphaned content (present in `ContentPackage`/`ContentLibrary`/`Content`/`LearningItem` but absent from `InstalledPackageRepository` and `Library`) is recognized as orphan state and does NOT trigger false `CONTENT_ID_ALREADY_INSTALLED` conflicts upon package re-import.
  - **State-Aware UI Diagnostics:** Updated `ContentLibraryViewModel.sanitizeFailureMessage()` and `ContentLibraryFacade.getPackageLifecycleState()` to present state-aware diagnostic messages (`State: ACTIVE` / `State: ARCHIVED`), avoiding generic or false assertions when lifecycle state is absent.
  - **Automated Integration Coverage:** Added `reconcile orphan content ownership during package reimport and reject active package duplicates` test to `GeneralStudyActivePackageAuthorityIntegrationTest.kt` verifying both legacy orphan re-import success and active package duplicate rejection.
  - **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 1m 36s. Total XML-verified tests: **2,155 passed, 0 failed**.

- **PLE-014 — Removed Package Reimport State Reconciliation** complete on `develop` (commit `32b6178`):
  - **Infrastructure & Platform Factory Dependency Wiring:** Resolved composition root defect in `LearningApplicationFactory.kt` and `PersistedLearningPlatformFactory.kt` where `installedPackageRepository`, `contentPackageRepository`, and `contentLibraryRepository` were omitted when instantiating `PackageImportService`.
  - **Live Owner Conflict Validation:** Updated `InstalledContentConflictValidator.kt` to validate content conflicts against active or archived packages in `InstalledPackageRepository` and live `ContentPackageRepository`/`ContentLibraryRepository`. Orphaned content from previously uninstalled packages does not trigger false `CONTENT_ID_ALREADY_INSTALLED` conflicts during re-import.
  - **Learner Progress Preservation:** Guaranteed that `MemoryState` and `ReviewHistory` records are preserved by item identity and reconnected seamlessly when a previously removed package is re-imported.
  - **Automated Regression Coverage:** Added `AC-06 - Complete removed package reimport lifecycle with progress reconnection` to `GeneralStudyActivePackageAuthorityIntegrationTest.kt` verifying real application/composition boundary import -> study -> uninstall -> re-import -> progress reconnection flow.
  - **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 30s. Total XML-verified tests: **2,154 passed, 0 failed**.

- **PLE-014 — Study Authority & Hidden Package Lifecycle Remediation** complete on `develop` (commit `26a4996`):
  - **Same-Runtime Active Package Authority Refresh:** Resolved in-memory caching defect in `StudyFacade.kt` so that when a study session is paused/idle and active package is changed in Library, `load()` and `startStudy()` immediately invalidate stale cached package/session projection and bind to the new canonical active package without requiring application restart.
  - **Transactional Package Uninstall & Orphan Clean-up:** Enforced complete transactional cleanup of all `ContentPackage`, `ContentLibrary`, `Content`, and `LearningItem` records in `PackageUninstallOperation.kt` across candidate package ID keys, eliminating orphaned content leaks during package uninstallation.
  - **Active/Archived Installed Package Conflict Validation:** Updated `InstalledContentConflictValidator.kt` to scope conflict validation against active or archived packages in `InstalledPackageRepository`, ensuring orphaned content from previously removed packages does not trigger false `CONTENT_ID_ALREADY_INSTALLED` conflicts upon package re-import.
  - **Automated Integration Coverage:** Extended `GeneralStudyActivePackageAuthorityIntegrationTest.kt` with tests for same-runtime paused session active package switching, symmetric active session protection, and complete package uninstall + re-import lifecycle (AC-06).
  - **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 2m 39s. Total XML-verified tests: **2,153 passed, 0 failed**.

- **PLE-014 — General Study Active Package Authority Remediation** complete on `develop` (commit `4da8464`):
  - **Active Package Authority when Idle:** Enforced that when Study is idle, canonical Library `activePackageId` strictly governs General Study. Stale packages, uninstalled packages, and packages no longer in Library are rejected when starting or recovering idle sessions.
  - **Resumable Active Session Protection:** Protected active/paused sessions from being overwritten when changing Library active package while Study is active. Active session provenance and topic state remain authoritative.
  - **Finished Session Isolation:** Corrected session recovery so that older finished sessions from a previous active package are not resurrected as active completion cards when a newer active session exists or when Study is idle for a different package.
  - **Non-Due Package Study Fallback:** Added explicit fallback in `StudyQueuePlanningService` and `GetNextSessionItemUseCase` allowing package and lesson study to present review items even when zero items are currently due.
  - **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL in 1m 24s. Total XML-verified tests: **2,130 passed, 0 failed**.

- **Gradle Default Memory Configuration Stabilization** complete on `develop` (commit `39a9b2a`):
  - **Repository-Level JVM Memory Configuration:** Created `gradle.properties` with `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8`, stabilizing the standard `.\gradlew.bat clean test` command without requiring manual `-D"org.gradle.jvmargs=-Xmx4g"` CLI arguments.
  - **Verification:** Verified by executing `.\gradlew.bat --stop` followed by `.\gradlew.bat clean test` across two consecutive clean runs. Both runs succeeded with `BUILD SUCCESSFUL` (Run 1: 2m 38s, Run 2: 2m 25s). Exact XML-verified test result: **2,129 passed, 0 failed**.

- **PLE-013 / P6-10 — Topic Selection & Exact Resume** complete on `develop` (commit `6c2eaff`):
  - **Multi-Topic Execution & Isolation:** Enabled independent multi-topic learning in Desktop Study. Learners can study Topic A to position X, switch to Topic B, and return to Topic A to resume at position X with exact session state (`AnswerRevealed`, `PromptPresented`, queue position).
  - **Single Source of Progress Truth:** Retained `StudySession`, `MemoryState`, `ReviewHistory`/`ReviewEvent`, and `StudyQueue` as sole progress authorities. Zero duplicate progress/resume models created.
  - **Active Session Protection vs Idle Topic Authority:** Upgraded `StudyFacade.kt` so that when Study is idle, active package/topic in Library governs session recovery via `LearningEngine.recoverTopicSession`. Active in-memory `StudySession` remains authoritative and protected from silent overwrites.
  - **Verification:** `.\gradlew.bat clean test` — BUILD SUCCESSFUL out-of-the-box. Exact XML-verified tests: **2,129 passed, 0 failed**.

- **Historical Phase 6 point:** implementation completed through P6-10. Phase 5 external
  clean-machine verification remains pending; the current continuation point is PLE-032 as
  stated at the top of this document.

- **PLE-010 — Library Navigation Recovery** complete on `develop`:
  - **Canonical Library Root Recovery:** Established `ContentLibraryViewModel.resetLibraryNavigationState()` and `StudyFacade.dismissCompletionPresentation()` (exposed via `StudyViewModel`). Unified `LearningShell.kt` to trigger the same canonical navigation flow when clicking sidebar "Thư viện", pressing F5, or triggering "Back to Library" callbacks.
  - **Completion Presentation Dismissal:** Separated completion UI presentation from domain completion evidence. Dismissing completion UI clears `sessionCompleted` presentation without deleting or mutating persisted session records, review history events, or scheduler state. New sessions reset dismissal so subsequent completions render normally.
  - **Active/Paused & Restart Invariants:** Verified active/paused sessions remain bound to their original package when returning to Library. App restart on a fresh `StudyFacade` preserves existing session recovery contracts without introducing new persisted schema fields.
  - **Verification:** `.\gradlew.bat --no-daemon clean test -D"org.gradle.jvmargs=-Xmx4g"` — BUILD SUCCESSFUL in 1m 57s. Total XML-verified tests: **2102 passed, 0 failed**.

- **PLE-009R2 — Real UI Package Authority & Navigation Remediation** complete on `develop`:
  - **Real UI Study Package Authority (Defect A):** Updated `StudyFacade.kt` (`restoreLatestUndoableCompletion` & `createIdleUiState`) so that when idle (no active/paused session), canonical Library `activePackageId` strictly governs Study. Completed undoable sessions from other packages are rejected when idle and cannot override the active package.
  - **Trapped Navigation Remediation (Defect B):** Wired `LearningShell.kt` so that navigating to `CONTENT_LIBRARY` (via sidebar click or F5) explicitly resets `learningWorkspaceUiState` and `lessonBrowserUiState`, restoring the root Library view. Added `FocusRequester` + `LaunchedEffect` in `LearningWorkspaceCard.kt` and `LessonBrowserCard.kt` ensuring keyboard `Key.Escape` works deterministically.
  - **Scope & Non-Defects:** Duplicate import atomicity verified intact (duplicate import rejected, no false Completed stage). Content Editor & single-lesson preview remain expected/out-of-scope capabilities.
  - **Verification:** `.\gradlew.bat --no-daemon test` — BUILD SUCCESSFUL. Focused & full test suite 100% passed.

- **PLE-009 — Desktop Package Selection & Navigation Stabilization** complete on `develop`:
  - **Active Package Synchronization & Authority:** Enforced state authority: `ACTIVE` or `PAUSED` study sessions remain authoritative and bound to their original package. When idle, canonical Library `activePackageId` is authoritative. Idle `StudyFacade` clears stale cached package identity and projects the newly active Library package without leaking previous package content. Preserved active/paused session safety and explicit non-mutating multi-package lesson launching.
  - **Browse Lessons Batch Query Optimization:** Added `findByContentIds` set-matching to `LearningItemRepository`, `InMemoryLearningItemRepository`, `StoreBackedLearningItemRepository`, and `LearningEngine`. `PackageLearningProgressQueryService` batch-queries all package learning items in 1 batch repository query instead of ~400 repeated per-content queries (verified `findByContentIds` call count = 1, `findByContentId` call count = 0).
  - **Duplicate-Import Atomicity & Error Sanitization:** Verified duplicate import failures preserve all authoritative state (catalog, active package, selected package/lesson, navigation, Study state). Enforced `PackageImportProgressStage.COMPLETED` is never emitted on failure, and error messages are sanitized into bounded, concise summaries.
  - **Acceptance & Navigation Verification:** Delivered acceptance coverage proving single-load Browse Lessons execution, loading/busy guards, single catalog addition on import, and full `Library` -> `Lesson Browser` -> `Workspace` -> `Back` navigation preserving package context.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 58s. Total XML-verified tests: **2089 passed, 0 failed**.

- **PLE-008 — Session Completion & Reflection Foundation** complete:
  - **Baseline:** Built on PLE-007 baseline commit `eea032b562fcfae08352cee49a6cab4b877dca83`.
  - **Session Completion Presentation Projection:** Created `SessionCompletionUiState`, `SessionCompletionStatus`, `SessionCompletionProjectionPolicy`, and `SessionCompletionCard` in `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/`.
  - **Authoritative Status & Reflection Mapping:** Maps domain completion state to `SessionCompletionStatus` (`COMPLETED`, `PAUSED`, `STOPPED`, `ABANDONED`, `INTERRUPTED`). Generates pure reflection message based on authoritative review metrics and lesson progress without artificial scores, AI text, gamification, or FSRS mutations.
  - **Navigation & Recovery:** Renders `SessionCompletionCard` when session ends. Provides clear navigation controls: (1) **Back to Lesson** (navigates to Content Library for package/lesson selection with zero session creation), (2) **Back to Library** (clears package browser and completion state), (3) **Continue Learning** (navigates to Content Library and opens PLE-007 Learning Workspace without auto-starting a session).
  - **No Lifecycle / Scheduler / Persistence Alteration:** Zero changes to scheduler, queue planning, session lifecycle, package ownership, progress calculation, FSRS algorithm, or persistence schema.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 13s, 15 actionable tasks executed, 100% tests passed.

- **PLE-007 — Learning Workspace Foundation** complete:
  - **Baseline:** Built on PLE-006 baseline commit `983e435c17d933b14ab023926da347634e7127a1`.
  - **Learning Workspace Presentation Projection:** Created `LearningWorkspaceUiState`, `SessionPreviewStage`, `SessionPreviewFactory`, `LearningWorkspaceProjectionPolicy`, and `LearningWorkspaceCard` in `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/contentlibrary/`.
  - **State Ownership & UX Flow:** Owned by `ContentLibraryViewModel.learningWorkspaceUiState` as transient presentation state. Clicking CTA in Lesson Browser opens Learning Workspace without creating a `StudySession`. Clicking "Back" returns to Lesson Browser with zero session creation. Clicking "Start Learning" reuses the exact existing `StartPackageLessonStudyRequest` boundary with single-invocation busy guard protection (`isStartingSession`).
  - **Session Preview:** Based on authoritative `LearningFlowTemplateStage` types (`Recall Prompt` -> `Reveal Answer` -> `Rate Recall`), without instantiating runtime scenes or consuming queue items.
  - **No Lifecycle / Scheduler / Persistence Alteration:** Zero changes to scheduler, queue planning, session lifecycle, package ownership, progress calculation, FSRS algorithm, or persistence schema.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 15s, 15 actionable tasks executed, 100% tests passed.

- **PLE-006 — Recommended Next Lesson** complete:
  - **Baseline:** Built on PLE-005 baseline commit `383e0f42ef7e65952583bb930641c7d22c039130`.
  - **Deterministic Recommendation Policy:** Created `PackageLearningRecommendation`, `RecommendationReasonType`, and `PackageLearningRecommendationPolicy` in `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/contentlibrary/`.
  - **Priority Order & Tie-Breaker:** Strict priority: (1) `DUE_NOW`, (2) `CONTINUE_IN_PROGRESS`, (3) `START_NEW`, (4) `REVIEW_COMPLETED`, (5) `NONE`. Canonical package lesson order is the sole tie-breaker. Lessons with 0 items are skipped.
  - **Desktop UI Integration:** Projected recommendation into `LessonBrowserUiState` and rendered compact Recommendation Card in `LessonBrowserCard`. Clicking "Select Lesson" selects the exact recommended `ContentId` (resetting search refinements if hidden) without starting a study session.
  - **No Lifecycle / Scheduler Alteration:** Zero changes to scheduler, queue planning, session lifecycle, package ownership, progress calculation, persistence schema, or learning algorithm.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 15s, 15 actionable tasks executed, 100% tests passed.

- **PLE-005 — Progress-Aware Lesson Study Entry** complete:
  - **Deterministic Presentation Policy:** Created `LessonStudyAction` and `LessonStudyActionPolicy` in `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/contentlibrary/`.
  - **Action Guidance Rules:**
    - `UNAVAILABLE` (`total == 0`): CTA disabled, label `"No Learning Items"`.
    - `START` (`total > 0 && started == 0 && mastered == 0`): CTA enabled, label `"Start Lesson"`.
    - `CONTINUE` (`started > 0 && mastered < total`): CTA enabled, label `"Continue Lesson"`.
    - `REVIEW` (`total > 0 && mastered == total`): CTA enabled, label `"Review Lesson"`.
  - **Due Indicator:** Exposes `dueText` (e.g. `"<n> item(s) due now"`) in selected lesson summary without altering action policy or CTA action/label.
  - **No Lifecycle / Scheduler Alteration:** Zero changes to scheduler, queue planning, session lifecycle, package ownership, progress calculation, persistence schema, or learning algorithm.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL in 1m 13s, 15 actionable tasks executed, 100% tests passed.

- **PLE-004 — Package Learning Progress** complete:
  - **Application Service & Metrics:** Implemented `PackageLearningProgressQueryService` calculating package and lesson progress metrics (total, unseen, NEW stage, started, mastered, due, suspended, completion percentage, started percentage).
  - **Package-Scoped Content Lookup:** Created `InstalledPackageContentQueryService` for single-query content resolution.
  - **Desktop UI Integration:** Projected progress into `LessonBrowserItem`, `LessonBrowserUiState`, `LessonBrowserCard`, and `ContentLibraryViewModel`.
  - **Verification:** `.\gradlew.bat --no-daemon clean test` — BUILD SUCCESSFUL. Commit `f6602ff` on `develop`.

- **PLE-003-R2 — Persist InstalledPackage Provenance in StudySession** complete:
  - **Domain Session Provenance (Part A / AC-R2-01, AC-R2-05):** Added `val installedPackageId: InstalledPackageId? = null` directly to `StudySession` aggregate and `StudySession.start(...)`. Provenance is immutable and preserved across all lifecycle transitions (`recordReview`, `reveal`, `undo`, `finish`).
  - **Start Session Command Propagation (Part B & C / AC-R2-02 - AC-R2-04):** Propagated `installedPackageId` through `StartStudySessionCommand` and `StartStudySessionUseCase`. `StudyFacade` passes `activeInstalledPackageId` when package validation succeeds, while legacy/general study sessions maintain `installedPackageId == null`.
  - **Persistence & Backward Compatibility (Part E / AC-R2-06 - AC-R2-08):** Extended `StudySessionRecord` with `val installedPackageId: String? = null` and updated `StudySessionRecordMapper`. Legacy JSON session records without `installedPackageId` decode safely to `null` without schema migration.
  - **Recovery & UI Projection (Part F, G & H / AC-R2-09 - AC-R2-13):** Restored active session recovery sets `activeInstalledPackageId = session.installedPackageId`. `StudyUiState` exposes `activeInstalledPackageId: InstalledPackageId?`. Clearing active study state resets transient context without leaking package provenance across sessions.
  - **Verification:** `.\gradlew.bat clean test` — 1,984 tests passed across all modules (385 in desktop module), 0 failures. Commit: `fix: persist package provenance in study sessions`.

- **PLE-003-R1 — Preserve Package Context Through Study Entry** complete:
  - **Removed Fake ID Fallback (Part B / AC-R1-01, AC-R1-02):** Removed `InstalledPackageId(uiState.libraryId)` fallback in `LessonBrowserCard`. If `installedPackageId == null`, Start Lesson button is disabled with explicit feedback `"Package context is unavailable."` without fake ID generation, callback invocation, or crashes.
  - **Package-Aware Study Request & Navigation (Part C / AC-R1-03, AC-R1-04):** Created `StartPackageLessonStudyRequest(installedPackageId: InstalledPackageId, contentId: ContentId)`. `LessonStudyNavigationCoordinator` preserves and forwards `installedPackageId` and `contentId` down to `StudyViewModel.startLessonStudy(request)`.
  - **Application Ownership Validation (Part C & E / AC-R1-05 - AC-R1-08, AC-R1-10):** `StudyFacade` validates: (1) Package exists, (2) Package is `ACTIVE` (not `ARCHIVED` or `REMOVED`), (3) Package belongs to default library, (4) Selected lesson content belongs strictly to target package, (5) Lesson contains enabled learning items. Any validation failure sets recoverable `uiState.loadError` without creating an active session or navigating to `STUDY`.
  - **Verification:** `.\gradlew.bat clean test` — 1,980 tests passed across all modules (381 in desktop module), 0 failures. Commit: `fix: preserve package context through lesson study entry`.

- **PLE-003 — Lesson Browser Product Completion** complete:
  - **Package Context Header (Part B / AC-03-01):** Rendered package name, total lesson & learning item counts, `InstalledPackageId` caption, and "Back to Library" action button in `LessonBrowserCard`. Preserved `installedPackageId` in `LessonBrowserUiState`.
  - **Hierarchical Presentation (Part C / AC-03-03, AC-03-04):** Implemented `groupLessonsHierarchically(lessons)` rendering Group headers and Section subheaders. Applied consistent fallback labels (`"General"` for blank group, `"Other Lessons"` for blank section) without displaying `"null"` or blank titles.
  - **Search & Filter (Part D / AC-03-05 - AC-03-07):** Supported local case-insensitive search across title, primary text, translated text, group, and section. Provided clear search reset. Differentiated Package Empty state (`lessons.isEmpty()`) from Search Empty state (`visibleLessons.isEmpty()`).
  - **Lesson Selection (Part E / AC-03-08, AC-03-09):** Single-select lesson on row click with primary container highlight and "Selected" badge. Selection remains preserved in state when query filter hides selected lesson.
  - **Start Lesson Flow (Part F & G / AC-03-10 - AC-03-13):** Created `PackageLessonSelection` typed context. Rendered bottom action bar with Start Lesson button enabled only when a valid lesson in current view is selected AND has `learningItemCount > 0`. Disabled Start Lesson for 0-item lessons with explicit feedback ("No learning items available for this lesson").
  - **Navigation & Isolation (Part H & I / AC-03-14 - AC-03-16):** Back button returns to Library overview without mutating package state. Loading a package resets search query, filter, sort, and selection, eliminating stale state leakage between browse sessions.
  - **Verification:** `.\gradlew.bat clean test` — 1,971 tests passed (372 in desktop module), 0 failures. Commit: `feat: complete lesson browser experience`.

- **PLE-002-R1 — Correct Package-Scoped Browsing and Active-Package Lifecycle** complete:
  - **Browse Lessons Package-Scoped Contract (Issue A / AC-R1-01 - AC-R1-04):**
    - Removed ambiguous `openLibrary` fallback to first library in `ContentLibraryViewModel`.
    - Implemented typed `browsePackageLessons(installedPackageId: InstalledPackageId, packageName: String)` in `ContentLibraryViewModel` and `LessonBrowserFacade.loadForPackage`.
    - Resolved `InstalledPackageId` -> `InstalledPackage.packageId` -> `ContentPackage` -> `ContentLibraryId` -> `LibraryContentQueryService.queryForLibraries(contentLibraryIds)`.
    - Guaranteed package isolation: Topic A Browse Lessons displays ONLY Topic A lessons; Topic B Browse Lessons displays ONLY Topic B lessons.
    - Non-existent package IDs return clear `loadError` ("Package with id '...' not found.") without fallback or silent failures.
  - **Active Package Lifecycle Consistency (Issue B / AC-R1-05 - AC-R1-09):**
    - **Archive Policy:** `LibraryCommandService.archivePackage` atomically sets `library.activePackageId = null` and saves both `updatedLibrary` and `updatedPackage` inside the same transaction when archiving the current active package. Archiving a non-active package leaves `activePackageId` intact.
    - **Restore Policy:** `LibraryCommandService.restorePackage` restores state to `ACTIVE` but does NOT automatically set `activePackageId` (remains `null` or unchanged).
    - **Sanitizing Invalid Legacy References:** `LibraryQueryService.getNavigationTree` sanitizes `activePackageId = library.activePackageId?.takeIf { id -> activePackages.any { it.id == id } }`. Legacy persisted state referencing missing or non-ACTIVE packages evaluates to `null` safely without app crash or displaying "Current Active" on archived packages.
  - **Verification:** `.\gradlew.bat clean test` — 1,959 tests passed (360 in desktop module), 0 failures. Commit: `fix: correct package scoped browsing and active package lifecycle`.

- **PLE-002 — Complete Library User Experience** complete:
  - **Browse Lessons (Part B / AC-01, AC-02):** Resolved `ContentLibraryViewModel.openLibrary(libraryId)` fallback to available library when invoked from `PackageCard`, connecting `Package` -> `Browse Lessons` -> `LessonBrowserCard` -> `onStartLessonStudy`.
  - **Archive (Part C / AC-03):** Wired `onArchivePackage` and `onRestorePackage` in `LibraryOverviewSection` down to `PackageListSection` so Archive and Restore buttons in Overview section show `ArchivePackageConfirm` / `RestorePackageConfirm` dialogs, executing `LibraryCommandService.archivePackage`/`restorePackage` without silent failures.
  - **Active Package (Part D / AC-04, AC-05):** Added `activePackageId: InstalledPackageId?` to domain aggregate `Library`, application DTO `LibraryNavigationTree`, UI state `LibraryUiState.Content`, and persistence record `CanonicalLibraryRecord`. Added `LibraryCommandService.setActivePackage` command with full transaction and restart persistence in `canonical-libraries.json`. Rendered "Current Active" badge and "Set Active" button on `PackageCard`.
  - **Package Ordering (Part E / AC-06, AC-07, AC-08):** Implemented `movePackageUp` and `movePackageDown` on domain aggregate `Library` and `LibraryCommandService`. Preserved entry order in `LibraryQueryService` for installed/active package lists. Added "Move Up" and "Move Down" action buttons to `PackageCard` with boundary enablement. Persisted entry order to `canonical-libraries.json` across app restart.
  - **UX Audit & Polish (Part A, F / AC-09, AC-10, AC-11):** Audited all `PackageCard` actions. Ensured zero unresponding silent clicks. Verified no regressions in Import, Remove Topic, or Restart Persistence.
  - **Verification:** `.\gradlew.bat clean test` — 1,954 tests passed (357 in desktop module), 0 failures.

- **PLE-001C-R1 — Restore File-Scoped Import and Installed Topic Removal** complete:
  - Implemented `JvmFileScopedPackageScanner` supporting single file selection (`.opd3`, `.pkg`, `.json`), resolving same-basename companion pairs (`<base-name>.json` and `<base-name>.pkg`), and throwing `MissingOpd3JsonPairException` without partial persistence when a companion is missing.
  - Updated `ContentPackageImportFactory.createScanner` to route single files to `JvmFileScopedPackageScanner` and directories to `JvmDirectoryPackageScanner`.
  - Updated `PackageDirectoryChooser.kt` to `choosePackageFile` with `FILES_ONLY` and extension filter `*.opd3, *.pkg, *.json`.
  - Implemented explicit installed topic removal in `PackageUninstallOperation` with full domain reconciliation across `installedPackageRepository`, `libraryRepository`, and `collectionRepository`.
  - Wired `UninstallContentPackageUseCase` in `LearningApplicationContext` and `LearningApplicationFactory`.
  - Updated `ContentLibraryFacade` and `ContentLibraryViewModel` with `uninstallPackage(packageId, packageName)` which removes the installed package, clears `lessonBrowserUiState = null` if the removed package was open, and reloads library state.
  - Rendered `Remove Topic` action button on installed package cards with Compose Material3 confirmation `AlertDialog` detailing package name and data impact.
  - Expanded `CanonicalDesktopImportIntegrationTest.kt` with 17 comprehensive automated unit and integration tests covering file-scoped chooser, OPD3 import, JSON/PKG pair resolution, single-file isolation, missing companion failure, conflict behavior, confirmation presentation, full persistence reconciliation, failure state preservation, package A/B isolation, collection assignment reconciliation, lesson browser clearing, restart persistence, re-import after removal, architecture dependency guards, and negative export UI check.
  - Verification: `.\gradlew.bat clean test` — 1,949 tests passed (354 in desktop), 0 failures. Commit: `fix: complete desktop import and topic removal flow`.

- **PLE-001C — Restore Canonical Desktop Import Entry and Product Flow** complete:
  - Integrated canonical `LibraryScreen` (`desktop/ui/library`) with `ContentLibraryViewModel` import pipeline (`desktop/ui/contentlibrary`), restoring full user-facing import entry without fallback switches.
  - Added `PackageDirectoryChooser.kt` using `javax.swing.JFileChooser` (`DIRECTORIES_ONLY`).
  - Added prominent `Import Package` action buttons in `LibraryHeader` and `LibraryEmptyView`.
  - Added `Browse Lessons` action button to `PackageListSection` cards, exposing `LessonBrowserCard` for imported package browsing, search, selection, and starting lesson study (`onStartLessonStudy`).
  - Rendered non-blocking import progress card (`ContentLibraryOperation.Importing`) and import success/error banners with actionable retry/clear controls.
  - Synced imported packages in `ContentLibraryFacade` to `ConflictAwarePackageImporter` for `defaultLibraryId`, reconciling canonical library navigation tree and content projection.
  - Verified zero imports from `infrastructure` or `adapter` in `desktop/ui/library` and zero Export OPD3 UI/actions.
  - Verification: `.\gradlew.bat clean test` — 1,941 tests passed (346 in desktop), 0 failures. Commit: `fix: restore canonical desktop import flow`.
  - Corrected historical architecture documentation in `AI_ARCHITECT_CONTEXT.md` to eliminate stale mentions of `LearningApplicationContext` in `LibraryFacade`.
  - Verification: `./gradlew clean test` and `./gradlew :desktop:test` BUILD SUCCESSFUL.

- **PLE-001B-R1 — Remove Desktop-to-Infrastructure Dependency and Repair Architecture Evidence** complete:
  - Refactored `LibraryFacade` in `vn.loi.learning.desktop.ui.library` to accept explicit Application ports/services (`LibraryQueryService?` and `LibraryCommandService?`) via constructor dependency injection.
  - Eliminated `LearningApplicationContext` and all direct `infrastructure` dependencies from `LibraryFacade` and the canonical Desktop Library consumer layer (`desktop -> application -> domain`).
  - Updated production composition root in `LearningShell.kt` to extract Application query and command services and inject them explicitly into `LibraryFacade`.
  - Strengthened architecture dependency guard in `LibraryViewModelTest`: scans all `.kt` files under `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/library` and fails on any `import vn.loi.learning.infrastructure` or `import vn.loi.learning.adapter`, and asserts zero forbidden Infrastructure/Factory mentions in `LibraryFacade.kt`.
  - Verified production composition wiring test proving `LibraryFacade` operates cleanly with explicit Application dependencies.
  - Verification: `./gradlew clean test` and `./gradlew :desktop:test` BUILD SUCCESSFUL.

- **PLE-001B — Desktop Canonical Library Command Experience** implemented:
  - Extended `LibraryFacade` to expose all 7 canonical Library commands through `LibraryCommandService` via explicitly injected Application services (`LibraryQueryService` and `LibraryCommandService`).
  - Implemented `LibraryViewModel` command controller managing state transitions, concurrency guard (`isBusy`), post-mutation refresh, and selection reconciliation.
  - Created `LibraryDialogState` and `LibraryDialogHost` supporting 7 user interactions: Create Collection, Rename Collection, Soft-delete Collection, Assign Package, Remove Assignment, Archive Package, Restore Package.
  - Added deterministic typed error mapping in `LibraryFailureMessage.forCommandResult(...)` switching on `LibraryCommandResult` sealed hierarchy without exception message parsing or leaking internal paths.
  - Added 12 comprehensive automated unit and integration tests in `LibraryViewModelTest` covering all success cases, typed failures, persistence rollback/failures, duplicate click prevention, persisted application context restart, and layer dependency guards.
  - Zero imports of `infrastructure` persistence classes in `desktop` library UI, zero `desktop` or `infrastructure` imports in `application`.
  - Verification: `./gradlew clean test` and `./gradlew :desktop:test` BUILD SUCCESSFUL.

- **PLE-001A-R2 — Persist Canonical Library State and Prove Command Transactions** complete:
  - Implemented persistent store-backed repositories for canonical Library platform:
    - `StoreBackedCanonicalLibraryRepository` & `JsonCanonicalLibraryStore` (`canonical-libraries.json`).
    - `StoreBackedCanonicalCollectionRepository` & `JsonCanonicalCollectionStore` (`canonical-library-collections.json`).
  - Wired into `LearningApplicationFactory.createPersisted(...)` and managed by `JsonFileTransactionRunner`.
  - Implemented safe default library bootstrap policy (loads existing without overwrite, creates/reconciles once).
  - Replaced `catch (e: Throwable)` with JVM-safe `catch (e: Exception)` in `LibraryCommandService`.
  - Comprehensive integration tests in `LibraryCommandIntegrationTest`: Test A (active collection round-trip), Test B (soft-delete round-trip in `DELETED` state), Test C (library registration round-trip), Test D (command-level multi-file rollback without partial state), Test E (default library bootstrap preservation).
  - Application layer contains zero imports of `infrastructure`, `adapter`, or `desktop`.
  - Verification: `./gradlew clean test` and `./gradlew :desktop:test` BUILD SUCCESSFUL.

- **FFR-001 — Foundation Freeze Remediation** complete:
  - Audited dependency flow between `desktop`, `application`, `domain`, and `infrastructure`.
  - Confirmed LD-008 is a real architectural violation (3 Application files importing Infrastructure directly, 1 Application file importing Adapter directly).
  - Remediated Application layer dependency direction:
    - Extracted application ports: `LegacyJsonImporter`, `DeterministicZipWriter`, `LegacyImportResult`, `LegacyImportException`, `DeterministicZipEntry`, `Sha256PackageIdGenerator` in `vn.loi.learning.application`.
    - Infrastructure implements application ports (`JvmDeterministicZipWriter`, `LegacyJsonImporter` in infrastructure with backward-compatible typealiases).
    - Refactored `LegacyPairCanonicalConverter`, `Opd3PackageExporter`, `LegacyJsonImportService` in `application` layer to eliminate ALL `infrastructure` and `adapter` imports.
  - Zero `infrastructure` or `adapter` imports remain in `application`.
  - Build: SUCCESSFUL. Tests: all passed.

- **Foundation Freeze Audit** complete (commit `4eec095`):
  - Audited toàn bộ Domain, Application, Infrastructure layers (611 source files).
  - Xóa 5 dead code files (150 lines): toàn bộ `domain/study/fsrs/evolution/` package (3 files không có caller), `InMemoryContentLibraryStore`, `InMemoryKnowledgeGraphStore`.
  - Fix indentation regression trong `LearningApplicationFactory` (3 vị trí bị mất indent).
  - Chuẩn hóa imports trong `LearningApplicationContext` (2 FQN → top-level import).
  - Fix 11 redundant explicit casts trong `KnowledgeGraphAnalyzerTest` và `KnowledgeGraphDomainTest` (→ smart casts).
  - Documented 10 Large Debt items (LD-001 đến LD-010) cho Chief Architect review:
    - **LD-008 (Resolved in FFR-001):** Application layer direct Infrastructure imports eliminated.
    - LD-001: `application/dashboard/` dead package (predecessor của `learningdashboard`)
    - LD-002: Content.library vs domain.library naming overlap
    - LD-003: `ContentSearchRepository` + `StoreBackedContentSearchRepository` không được wire
    - LD-004: File name constant duplication giữa 3 factory files
    - LD-005: `LearningApplicationContext` optional fields luôn được khởi tạo
    - LD-006: Domain repos inline trong `createContext()`
    - LD-007: Inline FQN trong `LearningApplicationFactory`
    - LD-009: `ContentMediaStorage` port expose `java.nio.file.Path`
    - LD-010: `application/dashboard/` dead/historical package
  - Build: SUCCESSFUL (59s). Tests: 1,587 passed, 0 failed, 0 warnings.

- LP-005 — Knowledge Graph Foundation complete (commits `9976529`, `c18f373`, `d39725a`):
  - Immutable `KnowledgeGraph` aggregate (no learner state, deterministic, internal constructor).
  - `KnowledgeNodeId`, `KnowledgeNodeKind` (5 kinds), `KnowledgeNode` (equality on id+kind), `KnowledgeRelationshipType` (7 types), `KnowledgeEdge`, `KnowledgeGraphValidationIssue` (5 sealed subtypes), `KnowledgeGraphFactory` (typed result), `KnowledgeGraphAnalyzer` (cycle-safe BFS/DFS, shortest path, topological order, transitive successors — all deterministic).
  - Persistence: `KnowledgeNodeRecord`, `KnowledgeEdgeRecord`, `KnowledgeGraphRecord`, `KnowledgeGraphRecordMapper`, `JsonKnowledgeGraphStore` (envelope schema, atomic write, missing-file-safe), `StoreBackedKnowledgeGraphRepository`.
  - Application: `GetKnowledgeGraphUseCase`, `SaveKnowledgeGraphUseCase`, `KnowledgeGraphQueryService`, `InstalledLibraryKnowledgeGraphProjection` (ACTIVE packages → PACKAGE nodes, canonical packageId identity, flat read-only).
  - `LearningApplicationContext` and `LearningApplicationFactory` wired; `installedLibraryGraphProjection` always created in `createContext`.
  - 83 LP-005 tests, 0 failures. Full clean main-module gate (`.\gradlew.bat clean test -x :desktop:test`) BUILD SUCCESSFUL.
- LP-004R — Canonical Import Identity and Typed Conflict Semantics complete (commit `fb36ac4`):
  - Removed `matchByName` from identity resolution; only `PackageId` and `TopicId` are canonical authorities.
  - Added `AMBIGUOUS_EXISTING_IDENTITY` detection: when `PackageId` and `TopicId` each point to a different existing record, the result is a typed `CONFLICT` with no mutation.
  - Conservative identical evidence: `IDENTICAL_PACKAGE` verdict requires both sides to supply a matching canonical `contentChecksum`; absent checksum returns `INSUFFICIENT_IDENTITY_EVIDENCE` conflict.
  - Replaced free-form `List<String>` conflict reasons with typed `PackageImportConflictReason` enum (`TOPIC_ID_MISMATCH`, `AMBIGUOUS_EXISTING_IDENTITY`, `OLDER_VERSION`, `INSUFFICIENT_IDENTITY_EVIDENCE`). Consumer can switch without string parsing.
  - Removed `InstalledPackage.reconstitute` fabrication fallback from `IDENTICAL_PACKAGE` branch; outcome now returns the real aggregate from repository or a `TechnicalFailure` on inconsistency.
  - Added nullable `contentChecksum` field to `InstalledPackage` (backward-compatible default `null`); persisted on `NEW_PACKAGE` and `SAFE_REPLACEMENT` for future fingerprint comparison.
  - Comprehensive test coverage: identity invariant violations, ambiguous identity, checksum comparison, conservative conflict, fabrication prevention, rollback safety, typed reason switching.
- LP-004 — Conflict-Aware Package Import complete (commit `4c0a070`).
- LP-003R.1 — Deterministic Library Failure Mapping complete.
- LP-003R — Library Runtime Identity & Failure Semantics complete.
- LP-003 — Desktop Library Experience complete.
- LP-002 — Library Query & Navigation Foundation complete.
- LP-001 — Library Domain complete.





- Comprehensive test coverage in `MediaPackagingTest`, `Opd3DeterministicExporterTest`, `Opd3PackageInspectorTest`, `Opd3PackageVerifierTest`, and `PackagePlatformRoundTripTest`.
- Remaining roadmap capabilities:
  1. Conflict-aware Import
  2. Workspace
  3. Collections
  4. Archive/Delete

- Beta-L02A — Legacy Pair Discovery & Validation provides the platform-neutral
  `LegacyTopicPairDiscoveryService` and structured `LegacyTopicDiscoveryResult`.
- A valid pair contains exactly one readable/supported JSON and one readable/supported PKG with
  the same case-insensitive logical base name. Results and diagnostic source paths are sorted
  deterministically.
- Diagnostics explicitly represent missing JSON, missing PKG, duplicate JSON, duplicate PKG,
  base-name mismatch, unreadable file, and unsupported format.
- `JvmLegacyTopicFolderReader` is the filesystem adapter and recognizes existing ZIP/OPD3 PKG
  signatures. The capability performs no JSON conversion, media extraction, persistence,
  package serialization, or OPD3 writing.

- Beta-L01 — Topic Identity and Resume State adds a persisted `TopicId` to installed package
  records and an optional topic reference to study-session records.
- Legacy package records without `topicId` derive the same ID from logical package name and
  format on every restart and persist it on their next write. Compatible package replacement
  preserves the current topic ID.
- Learner-topic checkpoint ownership reuses `StudySession` and `StudyQueue`; topic-specific
  recovery queries by `(LearnerId, TopicId)`. `MemoryState`, `ReviewEvent`, scheduler difficulty,
  stability, mastery/review counts and due state remain item-scoped authority.
- Desktop Library/Learn selection resolves topic identity through Application, clears transient
  projection on switch, and resumes the selected topic without leaking the previous topic UI.
- Focused evidence covers identity derivation, package/session mapper round-trip, legacy JSON
  compatibility, compatible replacement, installed OPD3 restart, and Desktop A → B → A switch
  plus restart.
- Next capability: Beta-L02 — Legacy Pair Conversion. Export, conflict-aware update,
  delete/archive, ordering and collection migration remain out of scope.

- Desktop Alpha-04 — Session Completion completes the first Product Brain session loop from bootstrap through persisted completion and Desktop presentation.
- `application/session/completion` owns reflection, learner summary, learning outcome, scheduler rating intent, scheduling projection, and completion result models. `ReviewSessionItemUseCase` remains the scheduler and review transaction owner.
- `StudySession.completionSnapshot` is an optional learner-facing persisted value with schema-v1 defaults for legacy compatibility. Desktop recovery projects the same snapshot after restart.
- `StudyFacade` orchestrates the real Product Brain inputs, established review workflow, session finish, and Desktop projection; `StudyScreen` only renders the core-produced outcome. New study workflows clear stale completion presentation.

- Desktop Alpha-03.5 — Decision Explainability implements Product Brain decision explainability capability.
- Creates `DecisionExplanation` model in `vn.loi.learning.application.decision` articulating observation, decision summary, pedagogical reason, and next step across all 4 adaptive decisions without leaking technical rule IDs.
- Desktop Alpha-03.5R completes the consumer boundary: `StudyFacade` and `StudyViewModel` preserve the current explanation across visibility changes, and `StudyScreen` renders learner-facing content with hide/show controls.
- `DesktopDecisionExplainabilityTest` covers the real `StudyFacade` → `StudyViewModel` → `StudyUiState` path and verifies explanation identity and content survive hide, show, and toggle transitions.

- Desktop Alpha Architecture Review evaluates Desktop Alpha-01, Alpha-02, and Alpha-03 implementations across 12 core architectural areas in `docs/DESKTOP_ALPHA_ARCHITECTURE_REVIEW.md`.
- Confirms a coherent platform-neutral closed adaptive teaching loop, zero UI instructional logic, 100% test pass rate (1,643 tests), stable application contracts, and issues GO recommendations for Alpha-03.5 and Alpha-04. Zero Kotlin source code, UI, or build logic was changed.


- Desktop Alpha-03 — Adaptive Decision implements Product Brain adaptive teaching capability.
- Creates platform-neutral models `AdaptiveAction`, `AdaptiveDecision`, `DecisionTrace`, `AdaptiveOutcome`, and `InstructionalDecisionEngine` in `vn.loi.learning.application.decision`.
- Integrates initial rule set evaluation (`CORRECT`+fast, `INCORRECT`, `PARTIAL`, `MAINTAIN_PACE`), timeline updates, and Desktop UI state projection (`lastAdaptiveDecision`, `lastDecisionTrace`, `currentDifficultyLevel`).


- Desktop Alpha-02 — Scene Execution implements Product Brain single-scene execution capability for `TypingRecallScene`.
- Creates platform-neutral scene contracts (`LearningSceneInput`, `SceneResult`, `LearningEvidence`, `EvidenceReceipt`, `LearningScene`, `TypingRecallScene`) in `vn.loi.learning.application.scene`.
- Integrates `selectFirstScene(...)` and `processEvidence(...)` into `ProductBrainPlanner` and projects scene state in Desktop UI (`StudyUiState`, `StudyFacade`, `StudyViewModel`).


- Desktop Alpha-01 — Session Bootstrap implements Product Brain session bootstrap capability.
- Creates platform-neutral models `LearningSessionContext`, `TeachingGoal`, `SessionTimeline`, `InitialDecisionSnapshot`, and `SessionOverview` in `vn.loi.learning.application.session.bootstrap`.
- Integrates `ProductBrainSessionBootstrap` into `ProductBrainPlanner` and projects `SessionOverview` in Desktop UI (`StudyUiState`, `StudyFacade`, `StudyViewModel`).


- Architecture Audit v1.0 evaluates codebase conformance across 15 core architectural areas in `docs/ARCHITECTURE_AUDIT_V1.md`.
- Concludes **Desktop Alpha Readiness: READY**, supported by 1,639 passing tests, strict inward dependency flow, and zero architecture violations. Established Prioritized Refactoring Backlog. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-03 — Instructional Decision Engine defines the reasoning architecture of Product Brain for making all pedagogical
  decisions in `docs/INSTRUCTIONAL_DECISION_ENGINE.md`.
- Details 11 decision inputs, 10 outputs, Decision Rules matrix across 10 cognitive scenarios, 5-tier priority hierarchy for conflict resolution,
  real-time closed-loop adaptive teaching engine, auditable Decision Trace logging, and Mermaid diagrams. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-02B — Canonical Learning Scene Library defines the complete canonical library of reusable educational interaction capabilities
  available to Product Brain in `docs/LEARNING_SCENE_LIBRARY.md`.
- Details 19-point uniform specification contract, taxonomy systems (11 memory types, cognitive load, duration, difficulty), 10 scene categories (Teaching, Practice, Assessment, Story, Speaking, Medical, Programming, Mathematics, Reflection, Challenge), selection rules, and Mermaid diagrams. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-02A — Learning Scene Framework defines the canonical interaction framework and contract for all Learning Scenes
  in `docs/LEARNING_SCENE_FRAMEWORK.md`.
- Details 12 framework concepts, 9 scene categories, lifecycle state machine (`Created` → `Prepared` → `Running` → `Paused` → `Resumed` → `Completed` / `Cancelled` → `Disposed`), input/output contracts, prohibitions, authority matrix, and Mermaid diagrams. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-01.8 — Learning Experience Architecture defines the end-to-end session journey architecture
  in `docs/LEARNING_EXPERIENCE_ARCHITECTURE.md`.
- Details 7 session phases (Warm-up → Teaching → Practice → Challenge → Review → Reflection → Summary), subsystem orchestration,
  session runtime contracts, motivation safeguarding, and domain walkthroughs. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-01.5 — Knowledge Model Specification defines the canonical, subject-independent Knowledge Model
  specification in `docs/KNOWLEDGE_MODEL.md`.
- Details 15 core knowledge concepts, universal domain mappings (Vocabulary, Stories, Medical Physics, Language, Technical),
  subsystem interaction boundaries, and Mermaid relationship diagrams. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-01 — Product Brain Specification defines the official architectural blueprint and specification
  for the AI Teacher (`ProductBrain`) in `docs/PRODUCT_BRAIN_SPECIFICATION.md`.
- Details the 17 core pedagogical concepts, complete 10-step Teaching Loop, subsystem responsibility matrix, Product Brain
  principles, and multi-year evolutionary roadmap. Zero Kotlin source code, UI, or build logic was changed.


- Milestone PB-00 — Repository Constitution & Product DNA establishes the repository knowledge system,
  Product Philosophy, Repository Constitution, System Overview, Product Brain conceptual framework,
  Cross-Platform Strategy, AI Design Rules, and Architectural Decision Records (ADRs).
- Documents created/updated: `docs/PRODUCT_PHILOSOPHY.md`, `docs/PRODUCT_BRAIN.md`, `docs/PRODUCT_BRAIN_SPECIFICATION.md`,
  `docs/LEARNING_PRINCIPLES.md`, `docs/CROSS_PLATFORM_STRATEGY.md`, `docs/SYSTEM_OVERVIEW.md`, `docs/REPOSITORY_CONSTITUTION.md`,
  `docs/AI_DESIGN_RULES.md`, `docs/adr/ADR-0001` through `ADR-0004`, `README.md`, `docs/PROJECT_HANDOFF.md`, `docs/ARCHITECTURE.md`,
  `docs/ROADMAP.md`, `docs/CHANGELOG.md`.
- Enforces mandatory 11-step onboarding reading order in `docs/PROJECT_HANDOFF.md`. Zero Kotlin source code, UI, or build logic was changed.


- Learning Objectives + Learning Strategies + Flow Templates Foundation separates Product Brain
  from Flow execution into platform-neutral layers. Objective policy selects `DURABLE_RECALL`; strategy
  planner derives strategy behavior (`includeOptionalTyping`) without rotation dependency;
  `LearningFlowTemplateFactory` translates strategy into semantic template slots (`ROTATED_PRIMARY`,
  `OPTIONAL_TYPING`, `ANSWER_REVEAL`, `RATING_READY`) without `LearningExperiencePlan` dependency;
  `ProductBrainPlanner` acts as an orchestration-only boundary; `LearningFlowInstantiationService` resolves
  runtime selections; and `LearningFlowPlanner` converts template + selections + rotation context into
  concrete `LearningFlowDefinition`.
- Architecture Gate: Reusable templates contain no `ExperienceSelectionResult` and are independent of
  item/session/rotation context. `DesktopLearningFlowCoordinator` depends strictly on `ProductBrainPlanner`
  and `LearningFlowController`. `LearningFlowPlanner` accepts no product policies or selection engines.
- Scheduler, reveal, rating, persistence, import/package, and playback semantics are unchanged.



- Learning Flow Engine Foundation + Desktop Multi-stage Vertical Slice adds immutable shared
  definition/stage/state/progress, deterministic planner, and pure controller.
- Production v1 flow is rotated primary → eligible Typing → authoritative reveal → manual
  rating-ready; without Typing it is primary → reveal → rating-ready.
- Desktop `StudyViewModel` owns transient flow state keyed by session/item. Recomposition,
  focus/resize/audio replay and same-runtime pause do not alter it; item/session change resets it.
- App restart does not persist exact stage. Unrevealed current items reconstruct stage one;
  already-revealed items reconstruct safe rating-ready. Undo rebuilds for the restored item.
- The Default/Typing chooser is removed from active flow UI; Typing eligibility/evaluation stays
  shared and unchanged. No scheduler, FSRS, queue, review, schema, import, package, or playback
  authority moved into flow.
- Full local gate: `gradlew.bat clean test :desktop:compileKotlin --no-daemon`, BUILD SUCCESSFUL;
  1,626 tests, zero failures/errors/skips. Temurin 21.0.11
  `:desktop:createDistributable` passed.

- Session-aware Experience Rotation Foundation activates Default-mode round robin over passive
  Image/Listening/Prompt options. `ExperienceRotationContext` binds session ID, learning-item ID,
  and zero-based `LearningSessionProgress.currentPosition`; first item is ordinal zero.
- Reveal, retry, recomposition, pause/resume, and Typing interaction retain the context. Undo
  reconstructs from the rewound queue position; active-session restart reconstructs from
  existing persisted session/queue state. No rotation field or schema was added.
- Full policy eligibility still includes Typing. Automatic projection excludes it; explicit
  Typing remains `USER_CHOICE`, and returning to Default restores the rotated result.
- Scheduler, FSRS, queue semantics, review/rating, persistence, import, JSON, PKG, OPD3, media
  resolution, and playback remain unchanged.
- Full local gate: `gradlew.bat clean test :desktop:compileKotlin --no-daemon`, BUILD SUCCESSFUL;
  1,619 tests, zero failures/errors/skips. Temurin 21 `:desktop:createDistributable` passed.

- Typing Recall Vertical Slice Foundation adds shared `TYPING_RECALL`, semantic expected-answer
  extraction, and conservative locale-stable exact evaluation. Policy orders it after
  Image/Listening/Prompt, preserving ordinal-zero behavior.
- Desktop offers an explicit per-item Default/Typing chooser, routes both paths through
  `ExperienceSelectionEngine`, and owns transient identity-keyed input, focus, submit, localized
  feedback, and reset behavior.
- Desktop owns media resolution and fallback, `DesktopLearningSceneProjector`, Path-backed
  scenes, localized rendering, playback, keyboard/focus, accessibility, and layout. Typing
  submission invokes existing reveal; rating remains manual.
- Scheduler, queue, review, evidence, persistence, import, JSON, and PKG code were not changed.
- Full local gate: `gradlew.bat clean test :desktop:compileKotlin --no-daemon`, BUILD SUCCESSFUL;
  1,609 tests, zero failures/errors/skips. Temurin 21 `:desktop:createDistributable` also passed.

- Desktop Learning Experience Alpha is implemented over the existing Phase 6 contracts. ACTIVE
  Learn now uses a focused shell and content-first workspace; semantic MP3 playback uses commit
  `888f9bf` with observable state, cancellation, role labels, and `R` replay.
- Automated audio evidence ends at decoded PCM writes to the real Desktop output boundary.
  Product Owner physical-speaker and visual/responsive evidence remains pending in
  [`DESKTOP_LEARNING_EXPERIENCE_ALPHA_UAT.md`](DESKTOP_LEARNING_EXPERIENCE_ALPHA_UAT.md).
- No scheduler, queue, rating, evidence, progress, undo, persistence, recovery, or package
  authority moved into Desktop.

- Platform-Independent Learning Product Specification is the preceding documentation capability.
  Six specifications under `docs/spec/` define learner journey, workspace, behavior,
  interactions, media, and topic hierarchy for every future client.
- The outcome-based product roadmap is `LX-01` through `LX-11`. It places Session Entry/Setup
  and Focused Workspace before semantic/timed media, multi-lesson, Listening, Typed Recall,
  goals/curation, and ethical auto flow.
- Mandatory Product Owner gates remain role vocabulary, multi-lesson order/limits,
  autoplay/reveal, typed-answer evaluation, and new durable learner-data lifecycle.
- Next implementation after external Desktop 1.0 gates remains LX-01 Hierarchical Learning
  Scope. No Kotlin/Compose behavior changed in this capability.

- Android Product Reverse Engineering & Desktop Product Architecture completed at baseline
  `37c10b8`; it added the behavior catalog, gap analysis, product architecture, initial
  post-1.0 roadmap, vision, and technical-debt register.
- Android is a product/UX reference only. Learning Engine remains authoritative for scheduler,
  FSRS, queue, session lifecycle, rating, persistence, recovery, undo, and correctness.
- The 19,222-line Activity, both complete XML layouts, and the full 195.93-second video timeline
  were inventoried. Missing collaborator source means their internals remain unknown.
- Recommended first post-1.0 capability: LX-01 hierarchical learning scope. Multi-lesson,
  listening, typing, favorites, and goals retain explicit Product Owner gates.

- Real-data Desktop performance remediation is complete in baseline commit `075b098`.
  Production-scale
  synthetic evidence measured import 1,940 ms, library query 95 ms, and study preparation
  315 ms for 2,425 contents/12,125 learning items in the final clean run.
- Immediate UI feedback is architecture-tested; real visual first-row/resize smoothness and the
  original 179 MB media package still require Product Owner manual verification.
- Final local gate: `gradlew.bat clean test --no-daemon` passed 1,559 tests with zero
  failures/errors/skips; Desktop compile, app-image creation, and native Windows launcher smoke
  also passed with Temurin 21.0.11.

- The Desktop release blocker for real JSON + OPD3 PKG pairs is resolved in commit `e119e05`:
  signature routing, sibling pairing, binary validation, persisted media wiring, installed
  library visibility, and session-start evidence are covered.
- The 179 MB Product Owner artifact is intentionally not tracked or claimed as locally tested;
  manual re-test with that source remains required.

- Desktop 1.0 release-candidate preparation: complete after the final repository audit.
- Windows native launcher remediation: complete; `jdk.accessibility` is included and the
  generated executable passes the isolated bundled-runtime startup probe.
- No further autonomous product capability is authorized before Desktop 1.0. Continue only with
  Product Owner/manual or external release evidence.
- Pause remains resume of `ACTIVE`; one-step undo is Application-owned, persisted, atomic, and
  able to reopen final-review completion after restart.

## Desktop 1.0 Continuation

- Complete: P6-01 through P6-09 implementation and automated verification.
- Remaining Learning Experience evidence: Product Owner execution of the P6-09 manual matrix.
- Then: Phase 7 release candidate, defect fixing, external/manual evidence, and Desktop 1.0.
- Stable for Desktop 1.0 absent a concrete defect: session lifecycle, workspace actions,
  Learning Content, rich renderer, and progress/completion projection.
- Phase 5 clean-machine install/upgrade/uninstall/reinstall and signing evidence remains open.

## Decision Boundaries and Risks

- Preserve/adapt Android's rapid learning rhythm, lesson choice, bilingual media, and input
  flexibility; redesign automation/gestures; retire Android SRS, file mutation,
  lock-screen/device-admin, and Activity-owned business lifecycle.
- New study presets initially configure Desktop presentation/media over the same authoritative
  session. They must not create a Desktop scheduler or second queue.
- Media orchestration must be injected and deterministic; playback callbacks never rate or
  mutate durable progress.
- Arbitrary previous-card mutation must not bypass the established one-step undo contract.

- Session schema-v1 checkpoint fields are optional/defaulted; preserve legacy JSON readability.
- Review stages one durable intent, then remains atomic across event, memory, session checkpoint,
  and queue. Recovery reuses the original event ID.
- Desktop workspace state is projection only. Keep action permission in `ReviewWorkspaceState`,
  and do not move session, scheduler, or persistence authority into Desktop.
- `Content` remains canonical; `LearningContent` is the renderer-neutral Application projection.
  Desktop presentation resolves only local media through `ContentMediaStorage`; its Markdown
  allowlist never interprets HTML or remote/executable content.
- Java Sound is the current dependency-free audio adapter. Unsupported codecs fail safely and
  remain visible as unavailable; broader codec support needs an evidence-backed product choice.
- Queue totals are stable and known for the composed runtime. Progress distinguishes processed,
  reviewed, and skipped entries; the legacy no-queue path explicitly reports an unknown total.
- Completion is queue/session-owned. Scheduler feedback is ephemeral Desktop formatting of the
  committed Application result and never performs a second scheduler calculation.
- Avoid encoding flashcard-specific screen states into general domain concepts, but do not add
  abstractions without a current use case.
- Phase 5 external verification debt must remain visible and must not be reported as complete.
- Pause is continuation of `ACTIVE`, not a new status. Undo is one latest committed rating,
  atomic, never multi-level, and must not drift event, memory, queue, session, progress, or
  completion state.
- Safe HTML and remote media remain excluded. Markdown is allowlisted; media is local-only;
  Java Sound with safe fallback is accepted while guaranteed MP3 support remains deferred.
- Compose-only window, focus, scroll, and animation state is not durable. Desktop never
  recalculates scheduler outcomes.

## P6-07 Decisions

- Existing `ReviewEvent.stateBefore` is the authoritative scheduler before-state.
- An optional session checkpoint records whether memory existed plus the session/queue identity
  required for exactly one reversal; old records decode with no undo available.
- The completed queue remains persisted while the final review is undoable, allowing a
  `FINISHED` session to reopen after undo, including after restart.
- Application owns validation and the atomic transaction; Desktop only requests and projects.

## Latest Verified Test Evidence

- Foundation Freeze Audit gate (commit `4eec095`):
  `gradlew.bat clean test -x :desktop:test --no-daemon` BUILD SUCCESSFUL (59s);
  1,587 tests, 0 failures, 0 skipped, 0 compiler warnings.

- LP-005 baseline: `gradlew.bat clean test -x :desktop:test` BUILD SUCCESSFUL; 1,587 tests (includes LP-005's 83 new tests), 0 failures.

- JSON + OPD3 PKG remediation: `gradlew.bat clean test --no-daemon` passed 1,551 tests
  with 0 failures/errors/skipped. Desktop compile, app-image creation, and native Windows
  launcher verification passed using Temurin 21.0.11. The original 179 MB Product Owner
  package remains a manual re-test input and is not tracked.

- The native launcher failure was reproduced as missing
  `com.sun.java.accessibility.AccessBridge` under an accessibility-enabled user profile. The
  rebuilt Temurin 21 runtime includes `jdk.accessibility`; `:desktop:verifyWindowsLauncher`
  launches the generated executable with isolated profile/storage and exits successfully.
  `gradlew.bat clean test --no-daemon` passed 1,546 tests with 0 failures/errors/skipped;
  Desktop compile, app-image, MSI, and EXE packaging tasks passed. The artifacts are unsigned
  and were not installed.

- The final release audit rejects negative or payload-mismatched recovery manifest counts before
  safety-backup creation or mutation. `gradlew.bat clean test --no-daemon` passed 1,545 tests
  with 0 failures/errors/skipped; `:desktop:compileKotlin` and the non-interactive
  `:desktop:packageUberJarForCurrentOS` task also passed. `:desktop:createDistributable` passed
  with the available full Temurin JDK 21; native installer/signing evidence remains external.

- P6-09 adds a persisted OPD3-to-Desktop integration path covering package registration,
  global queue creation, reveal/rating, restart, progress, completion, final undo, re-rating,
  completion recovery, and duplicate-review prevention. `gradlew.bat clean test --no-daemon`
  passed 1,544 tests with 0 failures/errors/skipped; `:desktop:compileKotlin` and the
  non-interactive `:desktop:packageUberJarForCurrentOS` smoke task also passed.

- P6-08 full local gate: `gradlew.bat clean test --no-daemon`, BUILD SUCCESSFUL; 1,543 tests,
  0 failures/errors/skipped. Focused keyboard, focus, localization, safe-error, renderer, and
  Desktop completion-undo integration tests also passed.

- P6-06 baseline HEAD: `3e675420fca0fc304d8459132f6755329c48ddfb`.
- Full local gate: `gradlew.bat clean test --no-daemon`, BUILD SUCCESSFUL; 372 suites / 1,535
  tests, 0 failures/errors/skipped. Focused progress, transaction, restart, completion, and
  accessibility tests also passed.
## PLE-030.5 continuation snapshot

PLE-030.5 adds the read-only question dock, density/font-scale layout invalidation, and
configured/effective Content workload persistence. The fixture selects 50 unique NEW Content
from 80 (163 technical entries) plus 20 REVIEW Content (60 technical entries). PLE-030 is FINAL
PASS; Continuous Review Mode remains the unimplemented PLE-032 capability.
# PLE-030.6 continuation snapshot

- Capability: compact-height Study layout, new-content Introduction, and header de-cluttering.
- Implementation is included in PLE-030 FINAL PASS.
- Authority: `StudyVisualLayoutResolver` owns height mode/budget; persisted
  `StudySession.introducedContentIds` owns completed exposure by ContentId; review transaction
  remains the sole counter/scheduler/event authority.
- Compatibility: additive defaulted JSON field; no scheduler, FSRS, queue, rating, package, or
  continuous Review Mode change.
- Verification: focused PLE-030.6/mapper/audio gate passed 46 tests; final
  `.\gradlew.bat clean test --no-daemon` passed 2,595 tests (root 1,698; Desktop 897), with
  zero failures, errors, or skipped tests.
# PLE-030.7 continuation snapshot

- Capability: one-step Introduction-to-Answer and compact Study chrome remediation.
- Root cause: PLE-030.6 persisted exposure but returned `answerRevealed=false`, so flow index
  zero rendered the original primary recall and required a second Next.
- Implementation: atomic exposure+reveal for one non-Typing experience; mandatory Typing or
  multi-experience flow retained; resolver-owned top/dock/button/footer compaction and common
  both-example fit estimate.
- Compatibility: scheduler/FSRS, queue, counters, rating, Undo, audio shortcuts, POS/highlight,
  previous-rating, normal later recall, and continuous Review Mode unchanged.
- Verification: focused flow/chrome/restart/rating/keyboard gate passed 66 tests; final
  `.\gradlew.bat clean test --no-daemon` passed 2,600 tests (root 1,698; Desktop 902), with
  zero failures, errors, or skipped tests.
# PLE-030.8 continuation snapshot

- Capability: restore answer rating actions after Introduction direct reveal.
- Root cause: persisted/UI answer state was correct, but same-item flow remained Experience
  after `confirmAnswerRevealed` rejection, producing `isRatingReady=false`.
- Implementation: authoritative reveal now reconstructs normal Rating Ready when necessary;
  centralized dock mode selects the existing answer callbacks and stays visible while disabled.
- Compatibility: one-Next Introduction, restart, compact chrome, examples, keyboard guards,
  counters, queue, review transaction, scheduler/FSRS, audio, Undo/Pause, POS/highlight, and
  continuous Review Mode unchanged.
- Verification: focused flow/dock/keyboard/chrome selection passed 50 tests; final
  `.\gradlew.bat clean test --no-daemon` passed 2,602 tests (root 1,698; Desktop 904), with
  zero failures, errors, or skipped tests.
# PLE-030.9 continuation snapshot

- Final session-goal remediation: Study entry compares the active policy's New/Review
  fingerprint with a fresh persisted-policy read. A mismatch finishes the stale session and
  starts a zero-counter replacement in the same study scope; a match resumes unchanged.
- Navigation wiring is `LearningShell` → `StudyViewModel.enterStudy()` →
  `StudyFacade.enterStudy()`. No global invalidation flag is used.
- Verification: focused stale-session suite 22/22 passed; full
  `.\gradlew.bat clean test --no-daemon` passed 2,633 tests (root 1,721; Desktop 912), with zero
  failures, errors, or skipped tests.
- Full Answer height UAT remediation uses live viewport height/width, density, font scale, and
  fixed-surface reserves to derive separate gap/padding/image/example budgets. Pre-answer is
  unchanged; low/large-content layouts retain the center-scroll fallback.
- Verification: focused height suite 78/78 passed; full
  `.\gradlew.bat clean test --no-daemon` passed 2,639 tests (root 1,721; Desktop 918), with zero
  failures, errors, or skipped tests.
- Final remediation replaces estimated fit with `FullAnswerFitLayout` measured geometry from
  actual weighted body constraints. Persisted queue effective workload plus session completion
  counters now own header progress, with synchronous Review/Undo refresh. Final verification
  evidence follows the clean build.
- Verification: focused measured-layout/counter suites passed 132 tests; full
  `.\gradlew.bat clean test --no-daemon` passed 2,642 tests (root 1,722; Desktop 920), with zero
  failures, errors, or skipped tests.
- Final information priority: Scheduler Feedback is continuation rather than required-fit.
  Measured fixture image height rises 304px → 384px by recovering the 72px feedback block and
  8px gap; required answer content and fixed Rating Dock remain unchanged.
- Verification: focused priority/visual suite 51/51 passed; full clean build remained green at
  2,642 tests (root 1,722; Desktop 920), with zero failures, errors, or skipped tests.
- Compact Statistics now uses actual dashboard width and semantic compact-inline tokens rather
  than only `metricsPerRow=4`. The measured fixture shrinks 140dp→92dp and returns 48dp to the
  weighted Full Answer body while preserving all eight metrics, denominators, and accessibility.
- Verification: focused compact/header/Full-Answer suites passed 103 tests; full
  `.\gradlew.bat clean test --no-daemon` passed 2,645 tests (root 1,722; Desktop 923), with zero
  failures, errors, or skipped tests.

- Final Identity remediation separates Identity-card width from the outer compact viewport
  class. Standard inline is retained from 480dp, compact inline from 240dp, and stacked is
  reserved for narrower cards. The compact measured fixture falls from the legacy 160dp stack
  to 84dp and returns 76dp to the image without changing word typography, pre-answer, or the
  measured Full Answer fit authority.
- Verification: focused Identity/Full-Answer/Statistics/Memory/resolver selection passed 97
  tests; `.\gradlew.bat clean test --no-daemon` passed 2,627 tests (root 1,699; Desktop 928),
  with zero failures, errors, or skipped tests.
- Adaptive Chrome remediation resolves top actions and StatusStrip from their actual usable
  width. Compact/minimum replace wrapping text actions with square icon actions and replace the
  long shortcut string/status text with bounded semantic tokens plus an icon-only state.
  Acceptance geometry is 308dp→176dp, returning 132dp to Learning Content; focused verification
  passed 42 tests before the final clean build.
- Final verification: `.\gradlew.bat clean test --no-daemon` passed 2,632 tests (root 1,699;
  Desktop 933), with zero failures, errors, or skipped tests.
- Final Quick Action Toolbar correction removes visible keyboard tokens from the bounded bottom
  surface. Standard and compact use rating circles, Replay, Material Undo, and session success;
  registry chord text survives only in tooltips/accessibility. Existing callbacks and keyboard
  dispatch remain authoritative.
- Verification: focused toolbar/chrome/keyboard/protected-boundary selection passed 40 tests;
  `.\gradlew.bat clean test --no-daemon` passed 2,633 tests (root 1,699; Desktop 934), with
  zero failures, errors, or skipped tests.

## PLE-031 continuation snapshot

- PLE-030 is FINAL PASS by Product Owner Manual UAT.
- PLE-031 uses persisted `DesktopRuntimeConfiguration.studyShortcuts` as the one live toolbar
  and dispatcher authority. `ShortcutChordFormatter` is shared with Settings.
- Vocabulary loop, example loop, Vietnamese meaning, and Vietnamese example actions are
  availability-aware; standard/compact show all and minimum uses priority overflow.
- Shortcut Change/Reset does not participate in session-goal fingerprinting or mutate session,
  queue, counter, policy, or scheduler state.
- Focused shortcut/runtime/toolbar/audio/session selection passed 49 tests. Final clean-build
  verification passed 2,637 tests (root 1,699; Desktop 938), with zero failures, errors, or
  skipped tests.

## PLE-031.1 continuation snapshot

- PLE-031 passed Product Owner UAT.
- Audio main actions now resolve stable semantic icon/badge composition solely from command;
  live chords are tooltip/accessibility/Settings/overflow metadata only.
- Vocabulary/example loop and Vietnamese meaning/example pairs are visually distinct; Vietnamese
  actions use audio semantics with `VI`/`VI+`, never translation glyphs.
- Focused semantic-icon/live-metadata/chrome/keyboard/theme selection passed 33 tests. Final
  `.\gradlew.bat clean test --no-daemon` passed 2,639 tests (root 1,699; Desktop 940), with
  zero failures, errors, or skipped tests.

## PLE-031.2 continuation snapshot

- PLE-031.1 semantic icons remain command-owned and stable.
- Main audio actions now compose those icons with live formatter-owned chord cues. Standard
  uses `chordText`; Compact/Minimum use `compactLabel`, while tooltip and accessibility retain
  the full current chord and disabled unavailable reason.
- Overflow renders icon, action name, and chord. Existing strip heights, adaptive priority,
  dispatcher, session/counter/queue/scheduler/policy boundaries remain unchanged.
- Focused live-cue/icon/chrome/formatter/keyboard/audio/theme verification passed 35 tests.
  Final `.\gradlew.bat clean test --no-daemon --console=plain` passed 2,641 tests (root 1,699;
  Desktop 942), with zero failures, errors, or skipped tests.

- Capability: synchronize new-session goals and distinguish REVIEW memory before Full Answer.
- Root cause: the remembered Desktop facade retained the startup configuration closure even
  after Settings persisted a newer value; pre-answer presentation also rendered rating-like
  segments for NEW and card-like REVIEW controls.
- Implementation: each session creation reloads persisted goals into one immutable
  `StudySession.policy`; queue and header remain downstream of it. Content-level review context
  now renders a non-interactive REVIEW-only footer across all planned experiences, while NEW
  renders none and Full Answer retains PLE-030.8 actions.
- Compatibility: active-session goals/counters/history, Content identity, scheduler/FSRS,
  Introduction/reveal, shortcuts, Undo/Pause, theme, and compact/minimum layout are unchanged.
- Verification: focused policy/queue/header/identity/dock/keyboard selection passed 32 tests;
  final `.\gradlew.bat clean test --no-daemon` passed 2,607 tests (root 1,698; Desktop 909),
  with zero failures, errors, or skipped tests.
- Final remediation continuation: Review Memory is now keyed to semantic pre-answer REVIEW
  rather than `canRevealAnswer`, covering Prompt/Image/Listening/Typing; Introduction remains
  NEW-only. Images use one resolver-owned 90%-content-width/vertical-budget contract in both
  scene and answer consumers with `ContentScale.Fit`. Focused remediation passed 82 tests;
  final `.\gradlew.bat clean test --no-daemon` passed 2,607 tests (root 1,698; Desktop 909),
  with zero failures, errors, or skipped tests. These remediations are included in PLE-030
  FINAL PASS.
