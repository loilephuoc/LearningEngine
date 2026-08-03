# Test Matrix

## UAT-DESK-001 Typing direction regression

- Strategy: Typing `TARGET_TO_SOURCE`; Reverse Translation `SOURCE_TO_TARGET`; other mode
  directions unchanged.
- Production plan: target-side prompt and English/source canonical answer.
- Desktop: split-brain projection is unavailable; realtime prefix/exact/wrong states, one-shot
  automatic success, reveal comparison, Practice isolation, and review-entry convergence remain
  covered by focused and full regression suites.

## LQ-006G recall-result learning integration

- Covers evaluative rating intent, single authoritative transaction/event/Scheduler/queue path,
  evidence gating and promotion capping, weak recognition, lapse/reveal consequences, practice
  isolation, manual provenance, duplicate delivery, identity/session/queue validation, deterministic
  classification, atomic Undo, trajectory restoration, and typing-path convergence.
- LQ-006A–F and existing session/transaction/Undo tests remain full-build regressions; integrated
  Desktop UAT does not apply.
- Verification: focused 4 suites / 38 tests; full clean 605 suites / 3,273 tests (root 382 / 1,999;
  Desktop 223 / 1,274), failures/errors/skipped 0 / 0 / 0.

## LQ-006F cross-platform recall execution engine

- Covers plan/submission/context validation, evaluator registry integrity, all seven modes, exact
  and normalized text, alternatives and Unicode, choice identity, playback rejection, terminal
  outcomes, assistance/eligibility, practice isolation, duplicate attempts, timestamps,
  determinism, platform neutrality, result validation, and wire round-trip.
- LQ-006A–E remain full-build regressions; integrated Desktop UAT does not apply.
- Verification: focused 4 suites / 73 tests; full clean 604 suites / 3,260 tests (root 381 / 1,986;
  Desktop 223 / 1,274), failures/errors/skipped 0 / 0 / 0.

## LQ-006E deterministic Multiple Choice generator

- Covers scope/Content filtering, normalized deduplication, ambiguity and lifecycle rejection,
  POS/topic/length ranking, all fallback tiers, reduced/failing option counts, stable option IDs,
  seeded shuffle, four answer-side directions, large bounded inventory, and plan integration.
- LQ-006A/B/C/D remain full-build regressions; integrated Desktop UAT does not apply.
- Verification: focused 2 suites / 37 tests; full clean 603 suites / 3,241 tests (root 380 / 1,967;
  Desktop 223 / 1,274), failures/errors/skipped 0 / 0 / 0.

## LQ-006D cross-platform recall plan factory

- Covers decision revalidation, every prompt mode, direction semantics, typed media, safe masking,
  answer contracts, assistance/requirements, deterministic IDs and wire round-trip, practice
  isolation, provider validation, registry integrity, leakage protection, and final validation.
- LQ-006A/B/C remain full-build regressions; integrated Desktop UAT does not apply.
- Verification: focused 3 suites / 50 tests; full clean 602 suites / 3,223 tests (root 379 / 1,949;
  Desktop 223 / 1,274), failures/errors/skipped 0 / 0 / 0.

## LQ-006C adaptive recall strategy selection

- Covers capability-only selection, direction membership, typed failures, determinism, safe
  fallback, strength and intelligence mappings, diversity exemptions, practice/evaluative policy,
  fallback uniqueness, typed rejection, and stable wire IDs.
- LQ-006A/B remain in full clean regression; no Desktop UAT applies.
- Verification: focused 2 suites / 33 tests; full clean 601 suites / 3,204 tests (root 378 / 1,930;
  Desktop 223 / 1,274), failures/errors/skipped 0 / 0 / 0.

This matrix maps common changes to focused verification neighborhoods. Exact source and tests
remain authoritative. Build and testing policy lives only in [`../AGENTS.md`](../AGENTS.md).

## LQ-005B evidence promotion execution and durable trajectory

- Covers automatic Again -> Hard, Hard -> Good, Good -> Easy, 23/24 hours, fresh-chain promotion,
  Again reset, sibling Content authority, duplicate suppression, manual/reveal exclusion, restart,
  safe missing-file startup, and atomic undo.
- Full verification: 589 XML suites / 3,073 tests (root 369 / 1,824; Desktop 220 / 1,249), zero
  failures/errors/skips.

## LQ-005D learning difficulty intelligence profile

- Pure Shared Domain coverage derives lifetime statistics, bounded current stability scores,
  difficulty/risk/readiness, generic promotion duration/chain analytics, engine confidence, and
  difficulty level only from a learner identity, `LearningTrajectory`, policy, and fake clock.
- Boundary scenarios cover low-confidence new trajectories, high-confidence long histories, many
  incorrect Again recalls, long promoted mastery, rating-independent Easy instability, improving,
  recovering, plateau, declining and regressing trends, sibling Content identity, and deterministic
  staleness risk.
- Full verification: 590 XML suites / 3,083 tests (root 370 / 1,834; Desktop 220 / 1,249), zero
  failures/errors/skips.

## LQ-005E adaptive learning strategy and recommendation

- Pure Shared Domain coverage maps difficulty profiles to focus practice, recovery, evidence
  building, normal review, promotion readiness, and monitoring without reading Scheduler,
  ReviewEvent, Queue, UI, or persistence state.
- Assertions cover typed action/category/priority/confidence/reasons, low/high engine confidence,
  critical regression, high risk, excessive lapse, fast promotion, learner + Content identity,
  deterministic output, and unchanged immutable input.
- Full verification: 591 XML suites / 3,094 tests (root 371 / 1,845; Desktop 220 / 1,249), zero
  failures/errors/skips.

## Current automated baseline and manual boundary

PLE-039-G XML baseline: **555 suites / 2,857 tests**—root 359 / 1,759 and Desktop 196 /
1,098—with failures/errors/skipped **0 / 0 / 0**. Automated coverage includes Memory Confidence
projection/pending evidence, Easy-only gate, Typing speed bands, normalized error severity and
mistake episodes, preview/final consistency, input BringIntoView, compact dock, seeded NEW-only
ordering, answer scroll reset/stale-effect cancellation, and Scheduler/FSRS/Undo/restart
regressions.

Manual integrated Desktop UAT remains separate and pending:

1. Typing input is fully visible in a low-height window/monitor.
2. Caret and typed text remain visible above the dock.
3. Reveal Answer remains fully visible and actionable.
4. Reveal starts at the typed diff and canonical answer without manual upward scrolling.
5. Manual back-side scrolling is not repeatedly reset by timer/audio/recomposition.
6. A new item binds, focuses, and brings its input into view.
7. The compact dock does not overlap content.
8. NEW order differs between new sessions.
9. The same active session/restart retains its durable queue order.
10. REVIEW priority and Again/Hard reinsertion remain correct.
11. Speed-band color and final-rating color remain distinct.
12. Immediate Relearning, minor typo, and significant/repeated typo outcomes remain correct.
13. Resize causes no scroll oscillation or focus loss.
14. Success overlay, audio, and Next/completion remain correct.

This checklist includes PLE-039-G UAT-01 through UAT-07 and must not be marked pass without
Product Owner confirmation.

## LQ-005A evidence promotion authority

Shared-Domain tests cover Again→Hard at 23h/24h, Hard→Good with two independent recall timestamps
and no intervening Again, and Good→Easy at 13d/14d with three recalls and no lapse. Exclusion tests
cover Practice, Reveal, manual override, direct manual rating, replay, undone and duplicate evidence;
decision assertions cover target rating, typed reasons, remaining time, missing recall count,
missing/excluded anchors, highest-level behavior, and an injected fake clock. Source inspection must
find no `System.currentTimeMillis()`, Desktop, Scheduler/FSRS, ReviewEvent mutation, MemoryState
mutation, persistence, queue, Practice execution, or Undo wiring in the evidence authority.
Focused verification is 1 suite / 12 tests; full clean verification is 586 suites / 3,053 tests
(root 366 / 1,804; Desktop 220 / 1,249), failures/errors/skipped 0 / 0 / 0.

## LQ-005A.1 evidence chains and learning trajectory

Domain coverage starts and advances chronological chains, validates anchors, closes/promotes and
resets chains for new Again, lapse, explicit recovery, and Undo reasons, and proves Hard/Good stages
start without prior-stage recall reuse. Trajectory tests retain historical chains, expose one open
current chain, reject cross-Content and duplicate/out-of-order evidence, and accept sibling recall
through shared `ContentId`. Manual rating tests require a non-evidence entry with no new/reset chain.
Promotion regression evaluates only the current chain and therefore cannot reuse closed-chain
recalls. No persistence, mapper, record, Application, Desktop, Scheduler/FSRS, queue, Practice,
ReviewEvent, MemoryState, transition, or Undo-execution coverage changes in this capability.
Focused verification is 2 suites / 23 tests; full clean verification is 587 suites / 3,064 tests
(root 367 / 1,815; Desktop 220 / 1,249), failures/errors/skipped 0 / 0 / 0.

## UX-003 typing, override access, and inventory presentation

Coverage resolves icon kind and accessibility text directly from shared typed evaluation state,
and enforces positive inner line-box allocation plus font-leading for descender/cursor/placeholder
safety across all viewport classes. Practice coverage distinguishes four local recall controls
from committed ratings and gates override availability on shared policy plus an existing committed
rating. Inventory presentation maps the shared projection without Desktop recounting; integration
coverage keeps active/completed facade state current after commit, override, Undo, and navigation.
Integrated wide, compact, short-viewport, focus, dialog-keyboard, and visual glyph UAT is pending.
Focused verification is 7 suites / 30 tests; full clean verification is 586 suites / 3,046 tests
(root 369 / 1,811; Desktop 217 / 1,235), failures/errors/skipped 0 / 0 / 0.

## UX-004 typing semantics and Practice-control stabilization

Shared tests cover normalized proper prefixes, mismatch/extra-character boundaries, Unicode,
multi-word, apostrophe and hyphen inputs, and prove `VALID_PREFIX` is not completed. Desktop tests
cover typed icon/accessibility mapping, the minimum inner line-box contract at every viewport,
Practice/evaluative identity separation, labeled typed override availability, six typed inventory
kinds/color roles, compact 2×3 presentation, and retained keyboard/front-isolation behavior.
Integrated visual glyph, cursor, focus, compact/short, and dialog UAT remains pending.

## UX-007 image-first allocation and single-pass transition

- Typing metric tests prove outer height is the sum of label, protected line box, action diameter,
  purposeful gaps, and insets; multiline content alone increases it.
- Vertical/image tests prove surplus increases image height across landscape, square, portrait, and
  extreme portrait without changing typing height, crop, stretch, or `ContentScale.Fit`; constrained
  layouts reduce image first and retain last-resort scrolling.
- Transition tests prove the linear typed phase order, source retention through result/exit, one
  pending destination publication at entry, no child item-ID arrival animation, and token/race
  guards. Integration coverage exercises automatic typing, direct rating, Practice advancement,
  completion, topic/package switching, restart, timer/focus keys, and reduced-motion-safe phase
  completion without sleeps.

## UX-008 measured typing bounds and aligned media sizing

- Compose UI tests measure actual outer, label, inner line-box, and trailing-action bounds, require
  containment and resolved minima, render descenders, and prove wide width cannot reduce typing.
- Media resolver tests distinguish frame/rendered width and height across landscape, square,
  portrait, extreme, small-source, and large-source cases. They require width-first landscape fit,
  aspect-derived height, frame/bitmap equality, and a frame that contracts with the 1.35x cap.
- Vertical allocation and Study regressions prove media yields before typing/Dock, bounded scroll is
  the overflow fallback, `ContentScale.Fit` remains, and UX-007 transitions, direct rating,
  Practice, inventory, and `VALID_PREFIX` stay unchanged. Integrated Desktop UAT remains pending.
- Focused verification is 8 suites / 56 tests; full clean verification is 585 suites / 3,041 tests
  (root 365 / 1,792; Desktop 220 / 1,249), failures/errors/skipped 0 / 0 / 0.

## UX-006 direct evaluative dock and dynamic study space

- Shared availability distinguishes active evaluative items from Practice and inactive/no-item
  sessions; direct transactions persist `MANUAL_USER`, update memory/schedule/inventory, advance
  once, and remain covered by existing rollback/Undo/idempotency suites.
- Desktop dock-mode and keyboard tests cover all four front ratings, answer continuity, focused
  typing ownership, duplicate in-progress guards, and removal of the former header/dialog path.
- Practice suites retain local feedback, no Scheduler/ReviewEvent, labeled override,
  `MANUAL_USER_OVERRIDE`, fixed membership, and deterministic looping.
- Vertical tests cover true outer typing minima versus inner line-box safety, surplus allocation
  across every aspect class, inventory/examples pressure, short-layout image yielding, fitted image
  policy, Decision Dock reservation, and last-resort bounded scrolling.

## UX-005 evaluative manual rating and vertical-space budget

Coverage proves typed evaluative availability, `MANUAL_USER` provenance, one normal review event,
Practice exclusion, transaction/Undo regressions, explicit reveal-before-dialog wiring, and the
existing ViewModel duplicate-submit guard. Resolver coverage verifies portrait/extreme portrait
yield before typing/Dock reservation, landscape/square use, inventory/examples consumption,
short-layout bounded-scroll boundary, and `ContentScale.Fit`. Inventory tests require six direct
items, semantic colors/accessibility, 6-wide or 2×3 compact layout, and no header/collapse symbols.
Integrated portrait and extreme-portrait Desktop UAT remains pending.
Focused verification is 9 suites / 32 tests; full clean verification is 589 suites / 3,056 tests
(root 370 / 1,815; Desktop 219 / 1,241), failures/errors/skipped 0 / 0 / 0.
Focused verification is 15 suites / 97 tests; full clean verification is 586 suites / 3,049 tests
(root 369 / 1,813; Desktop 217 / 1,236), failures/errors/skipped 0 / 0 / 0.

## LQ-004B practice loops, override, and inventory

Coverage must prove predecessor NEW-origin-only and full latest Again/Hard membership, frozen
content membership, complete deterministic Fisher–Yates permutations, no duplicates/loss/queue
growth, round-boundary separation, fixed denominator, persisted seed/order/round/position, exact
restart, and explicit leave without evaluative completion. Correct/incorrect/revealed/almost
practice results must create no event, memory/scheduler/FSRS/rating/reinsertion consequence.

Manual override coverage requires cancel-without-mutation, typed `MANUAL_USER_OVERRIDE`
provenance, exactly one scheduler call, atomic rollback, unchanged practice policy/membership, and
Undo restoring event/memory/due without rewinding practice. Inventory coverage counts latest
committed ratings once per eligible Content, includes Never Reviewed, enforces the category-total
invariant, reacts to committed override/Undo/scope/eligibility changes, and ignores practice-local
results. Desktop coverage locks shared projection rendering, practice copy/local feedback,
four-rating confirmation, explicit leave, and absence of rating-transition/scheduler feedback.
Normal Study, Review All, Continuous Review, SESSION-001, REV-001/002, LQ-003, P0-001, Typing,
Reveal, audio, keyboard, focus, accessibility, Scheduler/FSRS, queue, and Undo regressions remain
mandatory. Integrated Desktop UAT remains pending.
Focused verification is 8 suites / 39 tests; full clean verification is 578 suites / 3,019 tests
(root 363 / 1,787; Desktop 215 / 1,232), failures/errors/skipped 0 / 0 / 0.

## LQ-004A cross-platform practice foundation

Coverage must round-trip `PRACTICE_ONLY` through record mapping and JSON store reopening, prove
legacy records restore as `EVALUATIVE`, and prove shared review authority rejects a practice
session before pending-intent persistence or transaction execution. Rejection coverage must show
zero ReviewEvent, MemoryState, review counter, and scheduler/review transaction effects. Existing
Normal Study, Resume, Review All, Continuous Review, Scheduler/FSRS, Undo, queue, Typing, Reveal,
audio, keyboard, focus, and accessibility tests remain mandatory regressions. No loop, shuffle,
Manual Rating Override, evidence, rating promotion, or Desktop UI behavior is part of LQ-004A.
Focused verification is 3 suites / 18 tests; full clean verification is 575 suites / 3,013 tests
(root 361 / 1,783; Desktop 214 / 1,230), failures/errors/skipped 0 / 0 / 0.

## REV-002 full Again/Hard selection

Coverage must prove that an Again/Hard focused action with eight eligible scoped items and a normal
Study preset of five reports eight, queues eight, sets the focused session review limit to eight,
and exposes an eight-item progress denominator. Zero/one-item cases, Again+Hard completeness,
latest Good/Easy exclusion, pre-count content deduplication, disabled/suspended/deleted exclusion,
scope, ordering, fresh identity, duplicate-action rejection, and queue-failure rollback remain
mandatory. Normal `SessionPolicyLimiter`, Review All, latest-session New review, and Desktop count
projection must not regress. Focused verification is 6 suites / 70 tests; full clean verification
is 575 suites / 3,006 tests (root 361 / 1,777; Desktop 214 / 1,229), with
failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## REV-001 focused review entry modes

Coverage must prove latest matching finished-session selection, predecessor queue origin-NEW-only
membership, committed completion/review intersection, current scope, enabled filtering, stable queue
order, content deduplication, and no fallback to an older session when the latest has no eligible New
items. Again/Hard coverage must use latest chronological committed rating, exclude earlier difficult
ratings superseded by Good/Easy, order Again before Hard then due/overdue, oldest review, stable ID,
and apply the configured review limit. Both modes require review-origin queues, zero New limit, fresh
session identity, active-session duplicate rejection, rollback on queue failure, and no scheduler or
memory mutation. Review All and Continuous Review regressions remain mandatory. Desktop coverage
locks five-action order, typed availability/counts, disabled states, callbacks, shell wiring, and
English/Vietnamese strings. Focused verification is 7 suites / 74 tests; full clean verification is
575 suites / 3,005 tests (root 361 / 1,776; Desktop 214 / 1,229), failures/errors/skipped 0 / 0 / 0.
Integrated Desktop UAT remains pending.

## V3-004 centered Typing and POS feedback

Coverage locks the adaptive Typing field's centered editable row, caret/placeholder alignment,
centered Reveal control, persistent top label, and compact/wide resolver behavior. Success-overlay
coverage removes the generic success copy, consumes canonical POS through the semantic registry and
shared Study badge, covers noun, verb, phrasal verb, proper noun, and unknown fallback, and preserves
canonical answer/POS/rating/explanation accessibility order. Existing popup timing/dismissal,
Typing evaluation, Enter/IME Reveal, rating/LQ-002, focus, keyboard, and Study regressions remain
mandatory. Focused verification is 8 suites / 71 tests; full clean verification is 575 suites /
3,000 tests (root 361 / 1,771; Desktop 214 / 1,229), with failures/errors/skipped 0 / 0 / 0.
Integrated Desktop UAT remains pending.

## V3-003 adaptive Answer space utilization

Coverage proves collapsed surplus is assigned to the image, expanded Examples reduce image space
before hiding content, wide/compact allocations are deterministic, and bounded scroll is reserved
for genuine height pressure while continuation and Decision Dock remain outside. Typing coverage
locks viewport-aware minimum field height plus existing line, cursor, Enter, focus, keyboard, and
BringIntoView contracts. V3-002 translation, P0-001 isolation, rating/Space, LQ-002, UX-001, and
UX-002 regressions remain mandatory. Focused verification is 11 suites / 99 tests; full clean
verification is 574 suites / 2,997 tests (root 361 / 1,771; Desktop 213 / 1,226), with
failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## V3-002 approved Answer golden layout

Coverage protects the dominant 98%-stage Fit image, larger collapsed-examples budget, automatic
expanded-examples contraction, and the single centered audio/translation row without a Meaning
heading. Existing accordion commands, Answer ordering, P0-001 isolation, Typing, keyboard,
rating, scheduler, focus, scrolling, and accessibility regressions remain mandatory. Focused
verification is 8 suites / 74 tests; full clean verification is 573 suites / 2,995 tests (root
361 / 1,771; Desktop 212 / 1,224), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop
UAT remains pending.

## P0-001 Front-Side Recall Isolation

Automated coverage locks unrevealed Typing recall to the filtered learning-scene renderer and
prevents routing through the answer-bearing Discovery surface. It verifies concealed canonical
answer/meaning/accessibility semantics across Review, Relearning, Learning, and New stages, plus
scene-role filtering, Typing evaluation, LQ-002, manual rating, keyboard/Space, focus,
BringIntoView, restart, Undo, UX-001, and UX-002 regressions. Focused verification is 4 suites /
49 tests. Full clean verification is 569 suites / 2,982 tests (root 361 / 1,771; Desktop 208 /
1,211), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## EPIC-001 Premium Study Experience

Automated coverage protects the Learning Engine Focused Immersion vision: exact Canvas layers;
one focal discovery hero and one focal understanding hero; knowledge/reading/explanation roles;
grouped semantic Decision Area; bounded minimum/compact/standard/wide stage; equivalent Light/Dark
hierarchy; and deterministic item-identity-keyed arrival motion. Source and regression guards
preserve ContentScale.Fit and image sizing, Typing visibility, scheduler explanation, UX-001,
UX-002, LQ-002, reveal, continuity, completion, audio, focus, keyboard, scroll, rating, and
accessibility owners. Focused verification passed 13 XML suites / 143 tests; full `clean test`
passed 569 suites / 2,975 tests (root 361 / 1,771; Desktop 208 / 1,204), with
failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## EPIC-001R approved Study Workspace remediation

Automated coverage locks the exact Discovery and Answer reading orders, lexical hero before image,
confirmation before Answer identity, vertical Meaning-before-Examples flow, integrated Typing
reveal action, grouped Decision Area, responsive image `ContentScale.Fit`, semantic theme usage,
keyboard, focus, accessibility, LQ-002, UX-001, and UX-002 regressions. Focused verification passed
8 XML suites / 95 tests; full `clean test` passed 569 suites / 2,979 tests (root 361 / 1,771;
Desktop 208 / 1,208), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains
pending.

## V3-001 approved Study visual system

Coverage protects exact Discovery/Answer hierarchy, compact header without the counter board,
clean hero and framed `ContentScale.Fit` image, reading-style Meaning/Examples, integrated filled
Recall control, unified one-row/2×2 Decision Dock outside normal content scrolling, and the typed
Expanded/Standard/Compact/Compressed width+height policy with deterministic compression order.
Wide/landscape/square/portrait/extreme intrinsic image cases retain `ContentScale.Fit` and bounded
upscale/frame budgets.
Resize guards preserve item-arrival identity, focus, and example disclosure. Existing keyboard,
accessibility, Space=Good, manual rating, automatic Typing/LQ-002, scheduler/UX-002,
continuity/UX-001, audio, Light/Dark, and completion regressions remain mandatory. Focused
verification passed 12 XML suites / 112 tests; full `clean test` passed 572 suites / 2,992 tests
(root 361 / 1,771; Desktop 211 / 1,221), with failures/errors/skipped 0 / 0 / 0. Integrated
Desktop UAT remains pending.

## LQ-002 immediate post-lapse automatic rating guard

Automated coverage proves an automatic Typing candidate above Hard is capped at Hard only when
all authoritative facts identify review-origin post-lapse recovery: previous Again, Relearning
stage, and prior review in the active session. Coverage also preserves Reveal Again, existing Hard
evidence, normal Review, New/Learning, manual rating, Easy gating, facade authoritative
re-validation, Scheduler/FSRS, review transaction, Memory Confidence, queue, and StudySession
behavior. Focused verification passed 3 XML suites / 34 tests; full `clean test` passed 567 suites /
2,956 tests (root 361 / 1,771; Desktop 206 / 1,185), with failures/errors/skipped 0 / 0 / 0.

## UX-001 source consequence and Next Item sequencing

Automated coverage proves the Next Item destination is not visible while its source scheduler
consequence is rendered, and the Next Item overlay does not retain an exit composition when
destination arrival begins. Completion continues to retain and render its committed consequence.
Token-safe stale callbacks, consecutive ratings, deterministic resize/recomposition projection,
facade commit ordering, Typing, Study, completion, focus, keyboard, and downstream scheduler
regressions remain covered. Focused verification passed 4 XML suites / 45 tests; full `clean test`
passed 567 suites / 2,960 tests (root 361 / 1,771; Desktop 206 / 1,189), with
failures/errors/skipped 0 / 0 / 0.

## UX-002 quiet scheduler feedback

Automated coverage proves active-answer scheduler feedback resolves to bounded explanatory emphasis,
never consequence/banner emphasis, preserves committed-rating semantic identity without display-text
parsing, retains details availability and accessibility content, and supports compact wrapping without
placeholder data. Completion and continuity retain explicit consequence contexts. Light/Dark semantic
tokens, typography scale, layout order, UX-001 sequencing, scheduler values, manual/Typing consumers,
Rating Dock, focus, keyboard, responsive layout, and null feedback remain regression boundaries.
Focused verification passed 7 XML suites / 95 tests; full `clean test` passed 568 suites / 2,966 tests
(root 361 / 1,771; Desktop 207 / 1,195), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop
UAT remains pending.

## PLE-039-H semantic rating action feedback

Automated coverage proves exclusive Again/Hard/Good/Easy selection, unique consecutive tokens,
success-only confirmation/check, semantic role mapping, shared manual/forced/automatic final-rating
identity, 1/2/3/4 and Space routing, duplicate/input-focus guards, and unchanged Typing rating gates.
Integrated Desktop UAT remains pending for click/key exclusivity, Light/Dark contrast, transition
visibility, focus/scroll/audio/counter/Next/completion regressions, and final committed Typing rating.
Verification: focused 6 XML suites / 47 tests; full 560 suites / 2,878 tests (root 361 / 1,771;
Desktop 199 / 1,107), with failures/errors/skipped 0 / 0 / 0.

Product Owner manual UAT remains pending for: exclusive red/orange/green/blue click feedback;
1/2/3/4 and Space=Good equivalence; visible but fast confirmation check; unchanged visuals on the
other three buttons; distinct consecutive confirmations; correct next-item ownership; final-rating
automatic Typing and forced Again; Light/Dark contrast; and no focus, keyboard, audio, scroll,
counter, Next, or completion regression.
Remediation coverage additionally requires English/Vietnamese confirmation semantics, shared main
dock/quick-action wording, one consume owner, matching/stale/null token behavior, lifecycle-neutral
release, unique post-release tokens, failure clearing, and unchanged automatic Typing final rating.
Remediation evidence: focused 3 suites / 25 tests; full 560 suites / 2,881 tests (root 361 / 1,771;
Desktop 199 / 1,110), with failures/errors/skipped 0 / 0 / 0.

## AURORA-003 Study visual focus

Automated coverage verifies the exact relative hierarchy across Question, Answer, Meaning, Rating,
Example, Scheduler, header, and metadata; semantic rating identity; non-selected Scheduler styling;
Light/Dark token separation; responsive semantic invariance; deterministic resolution; real consumer
wiring; and unchanged layout order, image sizing, typography scale, Phase-1 spacing, motion, keyboard,
and accessibility contracts. Verification: focused 6 XML suites / 47 tests plus post-session
regression; full 561 suites / 2,894 tests (root 361 / 1,771; Desktop 200 / 1,123), with
failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## AURORA-004 Study micro interaction polish

Automated coverage verifies bounded deterministic reveal progression, Answer-before-Meaning-before-
Scheduler perception, settled transform identity, LETheme motion ownership, stable hover color,
semantic elevation/focus tokens, unchanged rating activation/confirmation scale and timing, fixed
Rating Dock/button geometry, and presentation-only dependencies. Focused verification passed 8 suites /
72 tests; Study/Typing/Rating/Scheduler/Completion/Focus/Keyboard/Audio regression passed 35 suites /
270 tests. Full verification passed 562 suites / 2,899 tests (root 361 / 1,771; Desktop 201 /
1,128), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## AURORA-005 learning session continuity

Automated coverage verifies source/destination identity, unique same-rating tokens, committed final
Typing rating, stale callback safety, deterministic phase progression, distinct completion, transient
restart default, success-only creation, scheduler-before-destination ordering, tokenized motion, and
unchanged focus/scroll/audio/AURORA-004 ownership. Focused verification passed 8 suites / 57 tests;
broad Study/Typing/Rating/Scheduler/Completion/Focus/Scroll/Audio/Keyboard/Undo/Restart/Continuous
Review regression passed 42 suites / 305 tests. Full verification passed 563 suites / 2,909 tests
(root 361 / 1,771; Desktop 202 / 1,138), with failures/errors/skipped 0 / 0 / 0. Integrated
Desktop UAT remains pending.

## AURORA-006 session completion reinforcement

Automated coverage verifies fixed acknowledgement/outcome/consequence/action hierarchy, factual
counter projection, typed identity independent of display text, exactly one available primary
continuation action, no fabricated primary when continuation is unavailable, tertiary Library and
Back-to-Lesson actions, recovery Undo, Continuous Review toggle ownership, deterministic theme/
viewport-independent resolution, real callback wiring, final scheduler consequence ownership, and
absence of learning authority or gamification. Focused verification passed 8 suites / 52 tests;
broad completion/continuity/keyboard/focus/scheduler/Undo/restart/Continuous Review regression passed
21 suites / 130 tests. Full verification passed 564 suites / 2,919 tests (root 361 / 1,771;
Desktop 203 / 1,148), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## AURORA-007 learning entry clarity

Automated coverage verifies projected context without technical IDs, active-session Resume and
ordinary scoped Continue priority, exactly one enabled primary, disabled/busy and no-scope behavior,
alternative Replay/Review All, navigation-only Library, projected New/Review/Learned readiness,
determinism across theme/viewport, null lesson/topic safety, typed error ownership, stable callback
dispatch, English/Vietnamese localization, semantic source order, and absence of planning,
persistence, gamification, or display-text routing. Focused verification passed 4 suites / 44 tests;
Library/Lesson Browser/Learn-entry/start/replay/recovery/search/keyboard/focus/Scheduler/Undo regression
passed 47 suites / 254 tests. Full verification passed 565 suites / 2,934 tests (root 361 / 1,771;
Desktop 204 / 1,163), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## AURORA-008 Dashboard clarity and information hierarchy

Automated coverage verifies today-first deterministic reading order, exact reuse of due/due-now/
overdue and Total/New/Active Memories projections, loading placeholders, key-metric exclusion of
historical analytics, activity before scheduling/history, responsive minimum/compact/normal/wide
breakpoints, semantic `LETheme` surfaces, and absence of query, Scheduler, repository, callback, or
navigation authority in the presentation resolver/screen. Focused Desktop verification passed 4
suites / 24 tests; application learning-dashboard + Desktop Dashboard regression passed 30 suites /
180 tests. Full verification passed 566 suites / 2,943 tests (root 361 / 1,771; Desktop 205 /
1,172), with failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## AURORA-009 Premium Study surfaces

Coverage proves typed discovery roles (hero, primary support, utility, action) and understanding
roles (hero, hero support, secondary primary, supporting, explanatory, action), deterministic
resolution, shared front/answer consumption, Light/Dark readability, and identical hierarchy across
minimum/compact/standard/wide presentation. Source regression preserves image bounds and `Fit`,
layout order, typography, keyboard/focus/semantics, rating feedback, continuity, Typing, audio,
scroll, scheduler, and completion authorities. Focused verification passed 4 suites / 68 tests.
Full verification passed 567 suites / 2,949 tests (root 361 / 1,771; Desktop 206 / 1,178), with
failures/errors/skipped 0 / 0 / 0. Integrated Desktop UAT remains pending.

## AURORA-010 Unified Study Stage

Coverage proves deterministic discovery and understanding role order, integrated content roles,
separate action ownership, flat border/elevation stage presentation, viewport-neutral grouping, and
Light/Dark semantic-token consumption. Source guards prove presentation-only nested `LESurface`
wrappers are absent from front Meaning, answer Meaning/Examples, and Scheduler while hero image/word
interaction seams and the Rating decision layer remain. Existing Study layout, Typing, Rating,
Scheduler, Completion, Audio, Focus, Keyboard, responsive, semantics, and motion regressions remain
mandatory. Focused verification passed 5 suites / 75 tests. Full verification passed 567 suites /
2,953 tests (root 361 / 1,771; Desktop 206 / 1,182), with failures/errors/skipped 0 / 0 / 0.
Integrated Desktop UAT remains pending.

## PLE-032-B2.1 durable Continuous Review foundation

Verified remediation baseline: 559 suites / 2,873 tests (root 361 / 1,771; Desktop 198 / 1,102), with
failures/errors/skipped 0 / 0 / 0.

Coverage must prove missing intent defaults disabled; exact scope persists across fresh factory
composition; ordinary completion without a Product Brain snapshot remains eligible; recovery and
legacy-unknown closures remain ineligible; mismatched package/topic returns a typed result before
mutation; repeated restart reuses/suppresses deterministic artifacts; and Desktop projection uses
the exact completed package/nullable-topic scope. Scheduler, review transaction, Undo, ordering,
reinsertion, localization, and accessibility regressions remain mandatory.
Disable preserves scope and unrelated session/review/queue data; malformed or
unsupported schema is rejected without rewrite; active recovery wins; no intent/no predecessor/
no-work/rejection are typed; latest predecessor selection and deterministic continuation reuse
cannot duplicate session/queue. Desktop source/wiring coverage must prove startup consumes the
typed application result and ViewModel actions use guarded Facade calls without repository scans.
Visible opt-in localization/accessibility and completion-only wiring are automated in B2.2;
restart/enable/disable behavior in a real Desktop window remains pending Product Owner UAT.

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

PLE-028D.1 remediation must additionally run `StudyVisualUatRemediationTest` plus the existing
layout, hierarchy, theme, and audio-loop regressions. Evidence covers Light/Dark POS
container/content/border, preserved POS formatting, Meaning POS presence/absence/order/wrapping,
subdued ready status, stable Answer resting/hover/focus/active-loop contrast, Standard/Wide
620x240dp image caps, Compact usability, 88/144dp rating-dock reservation, `ContentScale.Fit`,
and absence of child viewport or behavior-layer authority. Manual visual re-UAT remains
required.

PLE-028D.2 final remediation must additionally run `StudyVisualReUatFinalRemediationTest`.
Evidence covers dedicated Meaning POS and rating action typography, one centered/wrapping
Meaning/POS composition for Question and Answer, propagation of the existing normalized POS,
Light/Dark English and Vietnamese Example rest/hover/focus/active-loop semantic contrast, and
preservation of rating order, labels, shortcuts, callbacks, enabled authority, dimensions,
audio behavior, responsive/image constraints, and dependency boundaries. Automated evidence
does not replace final visual re-UAT.

PLE-028E POS semantic changes must run `PartOfSpeechSemanticsTest`,
`PackageImportServiceTest`, `PackageContentBrowserQueryServiceTest`,
`LEPartOfSpeechTokensTest`, `FocusedVocabularyAnswerTest`, and all PLE-028D Study regressions.
Minimum evidence covers case/whitespace/punctuation aliases, distinct compound/unknown values,
custom-field/tag/pronunciation inventory, counts and blanks, unique known/current identities,
SHA-256 deterministic allocation, collision probing, import-order/restart stability,
idempotent repository reconciliation, post-successful-import registration without content
mutation, Light/Dark 4.5:1 text contrast, canonical badge text/border, one Study resolver, and
guards against raw/Material colors, `String.hashCode()`, random allocation, repository scans in
Compose, and scheduler/audio/persistence coupling. Manual visual UAT remains required.

PLE-029 highlighting covers apostrophe/hyphen variants, NFC composed/decomposed Unicode, flexible
whitespace, punctuation, case, complete phrases, repeats, lexical boundaries, longest selection,
exact original ranges, malformed/blank input, unchanged text and no semantic-inflection guessing.
Shortcut coverage includes L, Shift+L, V, Shift+V, retained R, modifier serialization, visible
conflict rejection, legacy completion, restart persistence, input guards, missing-audio no-op,
loop toggling and Vietnamese one-shot playback through the existing controller. Existing Study
audio, rating, reveal, Pause, Undo and PLE-028 suites remain required.

PLE-030 statistics coverage must prove unique enabled totals, New/Review partition by completed
review events, latest-only Again/Hard/Good/Easy buckets, Undo restoration, exact package/lesson/
multi-content session scope, injected-clock Due through `MemoryState.isDue`, nearest future
one-shot refresh, post-success mutation refresh, failure/last-known-good behavior, localized
loading/unavailable semantics, compact two-row presentation, accessibility, LETheme-only colors,
and absence of repository access or scheduler duplication in Compose. Existing review,
transaction, Undo, responsive, audio, keyboard and PLE-028/PLE-029 suites remain required.

PLE-030.1 additionally proves Total equals the latest-rating bucket sum and excludes raw/unseen
inventory; New is unique session-completed/configured target; Review is exact planned
remaining/configured target; effective workloads are distinct and capped; first review,
re-review, Undo and failure semantics preserve source truth; Due may exceed the Session Review target;
session completion follows queue exhaustion; and Continue Learning creates a fresh plan.

PLE-030.2 additionally proves the approved eight-metric order and mappings; fraction
numerator/denominator separation; active versus zero emphasis; semantic color and icon roles;
13sp/28sp/11sp typography hierarchy; Standard/Wide one-row and Compact 4+4 layout policy;
subtitle suppression in Compact; localized meaningful fraction/group accessibility; stable
loading/last-known-good behavior; and absence of raw colors, Material color authority,
repository imports, clicks, horizontal scrolling or component-owned viewport resolution.

PLE-030.3 additionally proves immutable NEW/REVIEW admission survives queue persistence and
restart; all four ratings use admission origin for exactly-once session counters; Undo restores
the matching counter and queue origin; origin overrides review-history inference in header
workloads; schema-v1 queues retain a safe fallback; and failed review transactions preserve
existing atomicity. Desktop coverage proves NEW/missing-history has no previous-rating marker,
REVIEW marks only the latest matching rating, labels/shortcuts/callbacks/dimensions remain
unchanged, redundant ready-stage cues are absent, and the sole visual-layout resolver reserves
statistics header and rating dock before sizing answer media.

PLE-030.4 additionally proves all-unseen Content is NEW; any completed sibling event makes an
unseen technical item learner-facing REVIEW; New/Review quotas and counters count unique
ContentId; Total and the four latest-rating buckets count each Content once; repository order
selects the latest sibling event including deterministic timestamp ties; previous rating,
commit, Undo, Continue Learning and restart preserve content-level semantics; scheduler/item
stage remains unchanged; schema-v1/v2 queues remain readable; and Compose performs no repository
query. The exact Desktop sibling UAT regression covers Good underline, Hard rerating, unchanged
New/Total, bucket movement, Review completed progress and Undo restoration.

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
## PLE-030.5 regression boundary

- Read-only question rating context and unchanged answer actions.
- Width, height, density, font-scale invalidation and short-viewport fallback.
- Sibling-heavy 50-of-80 planning, genuine underfill, schema-v4 and legacy persistence.
## PLE-030.6 compact height and Introduction boundary

Coverage must prove all three height modes, font-scale/density invalidation, smaller vertical
image budgets, dock reservation, stable center scroll, absent visual dashboard subtitles and
technical progress, and retained accessibility descriptions. Introduction coverage must prove
ContentId eligibility, REVIEW exclusion, persisted completion across restart, no counter change
before rating, one-shot non-looping Vietnamese audio, missing-audio no-op, user replay, and
existing PLE-030.4/030.5, scheduler, rating, and audio-loop behavior.
## PLE-030.7 one-step Introduction and compact chrome boundary

Coverage must prove one atomic Introduction-to-Answer transition for single Image/Listening/
Prompt flows, persisted exposure plus reveal across restart, duplicate action idempotency, and
mandatory Typing/multi-experience preservation. Chrome coverage must prove resolver-owned
comfortable/compact/minimum top actions, equal answer buttons, read-only front segments, dock
padding/reserves, shared pointer/focus policy, both example rows in the common compact fixture,
fitted images, one center scroll, and unchanged rating/accessibility callbacks.
## PLE-030.8 direct-reveal answer dock boundary

Coverage must prove that authoritative same-item direct reveal moves stale Experience state to
normal Rating Ready, selects `ANSWER_ACTIONS`, renders four equal existing actions, retains the
dock while busy-disabled, routes 1/2/3/4 only after reveal, and remains stable across refresh
and restart. Compact/minimum reserves, footer separation, examples, normal reveal, one-Next,
rating transaction, counters, scheduler/FSRS, audio, Undo, and Pause remain regression gates.
## PLE-030.9 session-goal and review-memory boundary

Coverage must prove each newly created session reloads persisted New/Review goals, a subsequent
session cannot reuse the previous policy, queue and header use that same immutable policy, and
an active session is not retroactively changed. Study-entry coverage must also prove a stale
50/200 session is finished and replaced by a zero-counter 10/20 session, an unchanged 10/20
fingerprint retains the current session and counters, a later 5/50 change replaces it again,
and facade/application recreation still uses the latest persisted goals. Only New/Review limits
participate in invalidation. Content-level REVIEW history must produce one
read-only four-label footer for Prompt, Image, Listening, and Typing experiences; NEW must
produce none. Full Answer retains the interactive dock, shortcuts, callbacks, and scheduler
semantics. Remediation coverage must include a REVIEW pre-answer state with reveal action
unavailable, Introduction exclusion for REVIEW, 80–90% image/content width ratios at standard
and wide viewports, vertical-budget growth, short/font-scaled bounds, and shared pre-answer/
Full-Answer `ContentScale.Fit` consumption.

Full Answer height remediation additionally covers comfortable and compact desktop heights,
height-only recomputation at stable width, density/font-scale participation, synchronized image
wrapper/child bounds, preserved first bilingual-example budget, fixed Rating Dock separation,
unchanged pre-answer authority, low-height scroll fallback, and absence of monitor/resolution
branches.

Final remediation coverage uses measured geometry rather than a resolver fit boolean. It proves
strict identity/image/meaning/example ordering, first Vietnamese-example bottom within the
actual body boundary at both desktop heights, Scheduler Feedback continuation priority,
height-only reflow,
long-content/low-height fallback, continuation behavior, shared wrapper/child image constraints,
fixed Rating Dock separation, and pre-answer isolation.

Review-counter coverage uses real queue/session/statistics wiring and asserts each emitted state
in `14 → 13 → 12`, persisted effective underfill, sibling LearningItems, Undo,
restart/recovery, and header/session/queue consistency.

Compact Statistics coverage resolves density from actual usable dashboard width and proves
wide 1×8 versus compact 2×4 ordering, all eight metric families, denominator preservation,
merged accessibility, 140dp→92dp measured fixture height, font-scaled growth without overlap,
wide→compact→wide recomputation, header-before-body geometry, and Full Answer/Review Memory
regression coverage.

Full Answer Identity regression coverage resolves composition from the measured Identity-card
width: 680dp remains standard inline, 400dp becomes compact inline, and only 220dp stacks.
It proves wide-to-compact-to-wide recomputation, unchanged word typography, presentation-owned
metadata compression, and measured fixture heights of 110dp, 84dp, and 142dp respectively.
Replacing the legacy compact stacked fixture (160dp) with compact inline (84dp) returns 76dp to
the measured image budget while preserving the first bilingual example and keeping pre-answer
and `FullAnswerFitLayout` free of the new Identity authority.

Adaptive Study Chrome coverage proves standard text versus compact/minimum icon composition
from actual usable width, immediate standard→compact→standard resize recomputation, square
40dp/36dp targets, bounded 36dp/32dp/28dp StatusStrip heights, single-line/non-wrapping text,
icon-only Active Session semantics, and priority reduction without parsing a display string.
The compact acceptance fixture measures 308dp legacy wrapped chrome versus 176dp semantic
chrome, returning 132dp to Learning Content. Existing Ctrl+Z, Esc, 1–4, R callbacks, Rating
Dock ordering, Full Answer measured geometry, Identity, Statistics, and Review Memory remain
regression gates.

Quick Action Toolbar remediation additionally proves icon-only standard and compact rendering,
four equal semantic-color rating circles, outlined Replay, Material Undo, success session
indicator, fixed-height single-row composition, and direct reuse of existing action callbacks.
Source guards reject the former shortcut-token renderer and visible Ctrl+Z/rating labels while
tooltips and merged semantics retain `1–4`, Replay, Undo with Ctrl+Z, and Active Session.

## PLE-031 live audio shortcut toolbar boundary

Coverage must prove default and changed/reset chord formatting for vocabulary loop, example
loop, Vietnamese meaning, and Vietnamese example; Settings, visual projection, persistence,
and dispatcher must consume the same registry snapshot. Changed chords must dispatch while old
chords stop, and reset must reverse both. Standard/compact keep all audio actions in one fixed
row; minimum retains priority actions and exposes every remainder through accessible overflow.
Missing audio paths disable the matching action without moving it or invoking callbacks.
Shortcut-only configuration changes must not affect goal fingerprint, active-session counters,
queue, policy, scheduler, Rating Dock, Statistics, Full Answer, Identity, or theme behavior.

## PLE-039-F compact dock and seeded NEW ordering

Coverage must prove the Typing dock contains exactly the four existing semantic status segments
without the removed explanatory card/note, retains active/available presentation and accessible
targets, and uses a smaller Typing-only fixed-chrome reservation. Application coverage must prove
same SessionId plus candidate set is deterministic, different SessionIds generally differ,
REVIEW slots/subsequence are unchanged, and NEW subset variation occurs before policy limiting.

## PLE-039-G Typing answer scroll transition

Coverage must prove Typing front retains keyed frame-synchronized input bring-into-view, while
the actual answer side waits for composition and immediately scrolls the shared main body to zero.
The reset key must be stable across answer recomposition and exclude timer, audio, rating,
confidence, disclosure, and resize state; a new item must cancel the prior identity before its own
front visibility and reveal. Existing comparison ordering, Forced Again/Continue, exact-success,
keyboard, compact dock, scheduler, persistence, and NEW ordering regressions remain authoritative.

PLE-031.1 additionally proves vocabulary/example loop icon distinction, Vietnamese
meaning/example icon distinction, correct `VI`/`VI+` badges, and stable command-owned icon
resolution across shortcut Change/Reset.

PLE-031.2 proves every default audio action combines that icon with a live formatted chord;
Change/Reset updates visual cue, structured tooltip, accessibility, and dispatcher without
changing icon identity. Standard uses the full formatter, Compact/Minimum use the compact
formatter without losing modifiers, and all stay single-row/fixed-height. Minimum overflow
retains icon, action name, and chord. Disabled actions retain icon/chord identity, unavailable
reason, non-invocation, and Light/Dark semantic token contrast.
# SESSION-001 coverage

- Learn-entry presentation distinguishes active queue counts from next-session limits.
- Study entry preserves an active session when runtime limits change; explicit start-new closes it through the established leave boundary and plans with current limits.
- Completion and focused-review action identities remain regression-covered.
# LQ-003 coverage

- Normal planning covers 5 New + 5 Review with sibling experiences, deterministic first representative, ordering, zero/underfilled quotas, workload projection, and queue diversity compatibility.
- Session lifecycle and persistence cover durable lapse ancestry and restart compatibility; Undo restores the captured pre-review set.
- Typing policy covers Again ancestry surviving intermediate Hard ratings, preview/commit decision identity, future-session reset by session ownership, and unchanged manual/Scheduler boundaries.
- Focused latest-New, Again/Hard full selection, Review All, SESSION-001 resume/start-new, reinsertion, and Continuous Review remain regression gates.
## LQ-005C learning insight and learner transparency

- Root projector/query coverage verifies promotion duration and recall gaps, typed exclusions,
  difficulty/recovery/mastery/insufficient semantics, recommendation preservation, deterministic
  ordering and bounds, learner-and-Content identity, clock determinism, and absence of writes.
- Desktop coverage verifies answer/front/practice presentation, compact policy, accessibility order,
  complete EN/VI mappings, shared-policy consumption, and disclosure without learning-state mutation.
- Minimum regression remains the repository-wide clean test build; integrated Desktop UAT is pending.

## UX-009 typing-success IPA and POS presentation

- Focused Desktop coverage verifies IPA/POS order, all missing-data combinations, blank IPA,
  canonical POS fallback, centered compact bounds, localized accessibility order, and unchanged
  success timing/rating transition behavior.
- Minimum regression remains the repository-wide clean test build; integrated Desktop UAT is pending.

## UX-011 rapid typing-success reveal

- Focused Desktop coverage verifies typed stage order, 265 ms completion, 600 ms hold, 865 ms target
  lifecycle, audio elapsed-time accounting, missing-stage compression, bounded fade/scale/movement,
  final-layout reservation, single cancellable animation authority, semantic order, and same-row
  IPA/POS responsiveness.
- Existing success orchestration, typing presentation, rating preview, answer-scroll, and transition
  tests preserve cancellation, single completion, and no-flash behavior. Integrated Desktop UAT is
  pending.

## LQ-006A cross-platform recall contract

- Focused Shared Core coverage verifies all stable mode/direction wire IDs, typing validity,
  mode-specific media/example/choice validation, submission kind/mode and identity rejection,
  duplicate attempts, immutable assistance, reveal/practice exclusion, deterministic seed/clock and
  serialization, round-trip semantics, unsupported versions, Content authority/sibling sharing, and
  the absence of Desktop/filesystem/persistence/scheduling dependencies.
- Minimum regression is the repository-wide clean test build. No integrated Desktop UAT applies
  because this capability adds no platform wiring.
## LQ-006B Content recall capability resolver

- Focused Shared tests cover all seven modes, per-mode directions, missing/blank/malformed facts,
  safe and ambiguous example spans, assistance/media/lexical/context facts, ContentId sibling
  authority, stable ordering and wire round-trip, projection invariants, purity, LQ-006A reuse, and
  absence of learner/Scheduler/Evidence/Desktop/filesystem dependencies.
- The focused selection includes the existing LQ-006A contract suites. Minimum regression remains
  the repository-wide clean test build; no integrated Desktop UAT applies because there is no wiring.
# LQ-007A Desktop Recall Pipeline

- Verify Desktop Typing receives a Shared Core `RecallPlan` and renders its canonical answer.
- Verify exact and reveal submissions cross `RecallExecutionEngine` and
  `RecallLearningExecutionBridge` once.
- Retain focused popup/UX-011, Practice, Undo, Scheduler, evidence, queue, rating, and Learning
  Insight regression coverage, followed by the full clean build.
# UX-012 Practice Review Experience

- Verify Practice omits answer-ready and typed-answer comparison space while standard review keeps it.
- Verify compact feedback controls retain shortcuts and revised SRS/exit copy.
- Verify Front Card translation scale/gap and compact expanded Example presentation.
# LQ-007B Coverage Reinforcement Spacing

- Verify graduated Again 2/8/20/40 and Hard 4/12/30 gaps, maximums, Good/Easy no-op, policy
  delegation, deterministic decisions, and near-end defer.
- Verify Undo restores scheduled/deferred state and completion-discarded tails, schema-v6 round
  trip, legacy schemas 1–5, restart behavior, Practice isolation, and queue regressions.
## LQ-007C Desktop Multiple Choice runtime

- Verify renderer routing derives only from `RecallPlan.mode`; unsupported modes never fall back to
  Typing.
- Verify two/four-option identity, label, and ordering projection, number-key bounds, long-label
  wrapping, focus/accessibility order, and EN/VI-safe semantic composition.
- Verify click/key duplicate gating, typed option-ID submission, Shared execution/learning bridge,
  Practice isolation, single queue advance, Undo/completion lifecycle, and Typing regression.

## LQ-007C.1 Production Recall Mode Activation

- Verify production planning consumes `AdaptiveRecallStrategy` rather than a Desktop-fabricated
  Typing decision, and creates Typing or Multiple Choice through the existing plan factory.
- Verify deterministic option identity/order, ordered typed MCQ-to-Typing fallback with requested
  and resolved mode provenance, and typed unavailable with no plan.
- Verify same-attempt Desktop recomposition reuses the plan, Practice MCQ remains evidence-ineligible,
  and renderer routing, Typing, queue, Undo, recovery, and completion remain regression-safe.
- Verified evidence: focused 5 suites / 32 tests; full clean 609 suites / 3,299 tests (Root
  384 / 2,012; Desktop 225 / 1,287), zero failures, errors, or skipped tests.

## LQ-007D Desktop Listening Recall Runtime

- Verify production Listening plan selection, mode routing, stable audio identity, answer leakage
  isolation, explicit unavailable/failed audio, replay and Ctrl+R, and no recomposition autoplay.
- Verify unchanged raw `TypedText`, one-shot submit gating, shared execution/learning delegation,
  Practice isolation, evaluative evidence class, duplicate safety, localization, accessibility,
  compact sizing, and Typing/MCQ/production activation/queue/Undo/completion regressions.
- Verified evidence: focused 6 suites / 46 tests; full clean 610 suites / 3,307 tests (Root
  384 / 2,013; Desktop 226 / 1,294), zero failures, errors, or skipped tests.

## LQ-007E Desktop Image Recall Runtime

- Verify production Image selection and typed fallback, plan-bound routing, opaque identity, local
  resolve/decode states, answer isolation, safe generic description, and aspect-fitted landscape,
  portrait, square, and large-image rendering.
- Verify raw `TypedText`, one-shot Enter/click gating, duplicate safety, Shared execution/learning
  delegation, Practice isolation, standard evidence eligibility, localization, accessibility,
  compact layout, theme tokens, and Typing/MCQ/Listening/session/queue/Undo regressions.
- Verified evidence: focused 8 suites / 71 tests; full clean 611 suites / 3,318 tests (Root
  384 / 2,014; Desktop 227 / 1,304), zero failures, errors, or skipped tests.

## LQ-007F Desktop Example Completion Recall Runtime

- Verify production selection and typed fallback, plan-bound routing, exact masked-span segments,
  start/middle/end and multiple-occurrence targets, multi-word/punctuation/Unicode/whitespace
  preservation, leakage isolation, and typed unavailable handling for unsafe spans.
- Verify raw `TypedText`, one-shot Enter/click gating, duplicate safety, Shared execution/learning
  delegation, Practice isolation, assisted-context evidence policy, localization, accessibility,
  wrapping/responsive tokens, and Typing/MCQ/Listening/Image/session/queue/Undo regressions.
- Verified evidence: focused 9 suites / 98 tests; full clean 612 suites / 3,329 tests (Root
  384 / 2,015; Desktop 228 / 1,314), zero failures, errors, or skipped tests.

## LQ-007X Desktop Recall Runtime Conformance

- Verify five-mode routing, plan-keyed initial focus, untrapped Tab/Shift+Tab/Escape, existing MCQ
  1–4 and Listening Ctrl+R shortcuts, one-shot click/Enter races, duplicate-safe continuation,
  unavailable-state submission locks, and stable result/next/completion transitions.
- Verify Practice isolation, answer-leak prevention, EN/VI semantics, live regions, theme/spacing
  tokens, compact/comfortable wrapping, and preservation of each runtime-specific UX contract.
- Verified evidence: focused 16 suites / 154 tests; full clean 613 suites / 3,337 tests (Root
  384 / 2,015; Desktop 229 / 1,322), zero failures, errors, or skipped tests.
