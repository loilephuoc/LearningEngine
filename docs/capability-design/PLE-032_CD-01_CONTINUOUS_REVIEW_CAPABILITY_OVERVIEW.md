# CD-01 — Continuous Review Capability Overview

## 1. Document Status

- Phase: Capability Design
- Version: 1.0
- Repository audit baseline: branch `develop`, commit
  `685203d768d022e05bb763a18229756bcf0d07f0`
- Status: Approved
- Deliverable state: Frozen Capability Design Deliverable
- Authority: current repository source and tests, plus the approved Product Review direction
  supplied for PLE-032
- Contract state: Capability document frozen. Acceptance Contract NOT frozen.

This frozen document defines the product capability at overview level. It is not an Acceptance
Contract, domain model, state machine, persistence design, ADR, implementation design, or
authorization to implement PLE-032.

## 2. Problem Statement

Ordinary Study processes a bounded workload. A `StudySession` has explicit limits and lifecycle,
and its persisted `StudyQueue` reaches completion after a finite set of entries. Desktop then
presents completion; continuing ordinary general Study is a separate, user-triggered action
that starts another session.

This is appropriate for deliberate bounded study, but it requires a learner who wants a longer
review period to select Continue Learning after every completed workload. The product need is
to let that learner continue across multiple bounded workloads while retaining safe Stop,
leave/resume, restart recovery, and clear terminal outcomes.

This is a flow-continuation problem, not a queue-extension problem. Making a queue mutable,
unbounded, replaceable, or silently replanned would violate the current session and queue
boundaries rather than solve the learner problem.

Evidence classification: the ordinary bounded lifecycle and manual continuation are
Source-backed and Test-backed. The need for automatic multi-cycle continuation is an approved
Product decision.

## 3. Capability Definition

Continuous Review Flow is a higher-level learning flow capability that coordinates a sequence of
bounded Cycles within one stable learner-selected study scope.

Each Cycle is an ordinary `StudySession` with its own `SessionId`, immutable session policy for
that Cycle, and fixed persisted `StudyQueue`. Continuous Review coordinates these bounded
`StudySession` Cycles; it does not own `StudySession`. Eligibility is evaluated afresh through
the existing planning boundary when a new Cycle begins. After a Cycle reaches its normal
terminal point, the flow either continues safely with another eligible Cycle or reaches an
explicit flow terminal condition. Ordinary Study remains unchanged.

This definition is a Product decision constrained by Source-backed session, queue, planning,
review, Undo, and recovery invariants. It does not decide how the flow is represented in the
domain or persistence layers.

Continuous Review Flow does not yet exist in production runtime. Current source proves only the
existing boundaries that a future implementation must preserve. Describing a higher-level
learning flow as the intended boundary is an approved Product Review direction, not a claim
that such a coordinator already exists.

## 4. User Outcomes

The initial capability is successful for a learner when:

- Continuous Review is an explicit mode that the learner knowingly starts.
- The learner can study across multiple eligible Cycles without selecting Continue Learning
  after every Cycle.
- The learner can Stop the Continuous Review Flow.
- The learner can leave the experience and later resume safely.
- Application restart does not discard committed learning progress.
- A completed Cycle remains understandable as bounded progress within a longer flow.
- The learner receives a clear non-percentage flow status rather than a misleading claim that
  an open-ended review activity has a global completion percentage.
- The learner receives an explicit terminal outcome when no eligible work remains.
- A failure produces a safe, recoverable status rather than hidden repetition, skipped
  persistence, or an automatic empty-cycle loop.
- The product requirement for latest-review Undo remains continuous at a Cycle boundary,
  without expanding into multi-step history.

These are product outcomes, not implementation prescriptions.

## 5. Capability Boundary

### Inside the capability

At responsibility level, Continuous Review Flow:

- retains explicit continuous-flow intent;
- retains the learner-selected study scope for the duration of the flow;
- coordinates the transition between bounded Cycles;
- requests fresh eligibility evaluation through the existing planning boundary for each new
  Cycle;
- decides whether the flow should continue or terminate after a Cycle;
- supports user Stop, leave/resume, and restart recovery as product behavior;
- exposes a recoverable flow status and explicit terminal reason;
- presents per-Cycle progress together with a non-percentage flow status;
- preserves the product-level availability of latest-review Undo across a Cycle boundary.

### Outside the capability

Continuous Review Flow does not own:

- scheduler or FSRS algorithms;
- item rating meanings or rating actions;
- `MemoryState` calculation;
- `ReviewEvent` content or ordering;
- candidate eligibility rules or queue ordering;
- `StudySession` transition internals;
- mutation of an active `StudyQueue`;
- review transaction membership;
- the detailed Undo algorithm;
- Product Brain decisions or behavior;
- Desktop-specific rendering or layout;
- package/content identity rules.

The split above is a capability boundary only. It does not imply a new aggregate, repository,
record, use case, API, class, or UI structure.

Current Desktop is not a passive presentation-only layer. `LearningShell` owns presentation
composition, while `StudyFacade` currently triggers parts of the ordinary lifecycle, including
completion and manual continuation. `LearningApplicationFactory` owns application and
infrastructure composition. The future Continuous Review authority must not be only a
Desktop-local macro, but CD-01 does not decide the final ownership shape.

## 6. Relationship to Existing Capabilities

| Existing capability | Continuous Review uses | Continuous Review must not own |
|---|---|---|
| Study Session | Coordinates ordinary bounded `StudySession` Cycles, including their normal lifecycle and counters | The `StudySession` itself, its transition rules, policy mutation, or an infinite session |
| Study Queue | One fixed persisted queue for each Cycle and its normal progress/exhaustion signals | Append, extend, replace, reorder, or silently replan an active queue |
| Queue Planning | Fresh eligibility and ordering when a new Cycle is created | Candidate rules, limits, strategy, scheduler interpretation, or planner internals |
| Review | Existing rating workflow and committed review result | Rating semantics, `ReviewEvent` structure, or partial persistence shortcuts |
| Scheduler / MemoryState | Existing eligibility and scheduling outputs consumed by planning/review | FSRS, `MemoryState`, due calculation, or stage transition logic |
| Undo | Existing latest-committed-review reversal semantics | Multi-step history or a second Undo authority |
| Recovery | Existing active-session/queue reconciliation as the Cycle recovery foundation | Silent repair of missing/completed queues or duplicate review replay |
| Statistics | Existing per-session, queue, Content, and item-level projections | A fabricated global percentage for an open-ended flow |
| Desktop presentation and orchestration | `LearningShell` presentation composition and existing `StudyFacade` lifecycle triggers for ordinary completion/manual continuation | A new Continuous Review authority that exists only as transient Desktop state |
| Product Brain | No dependency in the initial scope | Product Brain planning, adaptation, or orchestration |

The table combines Source-backed current ownership with Product-decision constraints. Detailed
authority allocation is deferred to CD-05.

## 7. Preserved Architectural Invariants

1. **Bounded session.** Every Cycle remains one ordinary `StudySession` with a distinct
   `SessionId`, fixed policy, and explicit terminal lifecycle.
2. **Fixed queue per Cycle.** A Cycle receives one persisted `StudyQueue` at start. The queue is
   not appended, extended, replaced, or silently replanned while active.
3. **No duplicate queue identity.** Both plan and snapshot continue to reject duplicate
   `LearningItemId` values.
4. **Explicit planner boundary.** Full planning remains a new-Cycle operation. Continuous Review
   coordinates when that boundary is invoked; it does not absorb planning decisions.
5. **Planner semantics remain unchanged.** Continuous Review decides only **when** the existing
   planner boundary is invoked. The planner remains authoritative for **how** the queue is
   created.
6. **Review transaction integrity.** Pending review intent remains durable before the existing
   transactional commit of review state, session state, and queue advance.
7. **Latest-review-only Undo.** Undo continues to reverse exactly the latest committed review
   and restore the associated MemoryState, ReviewEvent, session checkpoint, and queue cursor.
8. **Deterministic recovery.** An incomplete persisted queue may resume. An active session with
   a missing or completed queue is closed by the existing recovery behavior rather than
   fabricated or silently extended.
9. **Separate completion concepts.** Queue exhaustion and `StudySession` completion remain
   distinct facts even when a flow automatically considers another Cycle.
10. **Counter identity remains explicit.** Queue position counts queue entries /
   `LearningItemId`; learner-facing New/Review session counters advance on the first completion
   of a unique `ContentId` in that session.
11. **Ordinary Study isolation.** Existing ordinary start, bounded completion, manual Continue
    Learning, recovery, and failure behavior do not change merely because Continuous Review
    exists.
12. **Product Brain isolation.** Initial PLE-032 neither invokes nor changes Product Brain.
13. **Non-Desktop-only authority.** Cross-Cycle continuation cannot depend solely on transient
    Desktop state. This is an approved Product decision. Current runtime still places some
    ordinary lifecycle triggers in `StudyFacade`; the detailed future ownership boundary is
    deferred to CD-05.

Items 1–10 are Source-backed and Test-backed. Items 11–13 combine current architecture ownership
with approved Product decisions. The statement that a higher-level coordination boundary is the
appropriate future direction is a Product decision; the exact coordinator shape is not decided.

## 8. Initial Product Scope

Initial PLE-032 scope includes:

- an explicit Continuous Review mode;
- ordinary bounded StudySession Cycles;
- automatic transition between eligible Cycles;
- a stable learner-selected study scope;
- fresh eligibility evaluation for every new Cycle;
- user Stop;
- leave and resume behavior;
- recovery after application restart;
- an explicit no-eligible-work terminal outcome;
- safe, recoverable failure presentation;
- per-Cycle progress plus a non-percentage Continuous Review Flow status;
- latest-review Undo continuity at a Cycle boundary as a product requirement;
- preservation of committed progress and prevention of empty-cycle loops.

This section does not decide the mechanism, storage shape, state names, UI arrangement, or
transaction design for those outcomes.

## 9. Explicit Non-Goals

Initial PLE-032 does not include:

- an infinite mutable `StudySession`;
- an appendable or infinitely growing `StudyQueue`;
- queue replacement or silent replanning inside an active session;
- bypassing the scheduler to create an appearance of continuous work;
- multi-step Undo history;
- a global infinite-study completion percentage;
- Product Brain integration;
- Pomodoro;
- Daily Goal;
- Marathon Mode;
- automatic scope expansion across packages;
- scheduler or FSRS redesign;
- ordinary Study redesign;
- repository or schema migration without evidence that later design requires it;
- mobile or Web UI implementation;
- a frozen Acceptance Contract;
- decisions reserved for CD-02 through CD-10.

## 10. Capability Success Criteria

At capability level, CD-01 considers Continuous Review successful when the eventual product:

- lets a learner pass through multiple eligible Cycles without manual Continue Learning;
- keeps every Cycle compliant with existing session, queue, planner, review, and scheduler
  invariants;
- never appends to or replaces an active queue;
- preserves committed review progress through Stop, leave, restart, and recoverable failure;
- does not duplicate a committed review during continuation or recovery;
- ensures that no committed review disappears during an automatic transition between Cycles;
- preserves latest-review-only Undo at the Cycle boundary;
- terminates clearly when no eligible work remains and does not loop through empty Cycles;
- reports bounded per-Cycle progress without inventing a global flow percentage;
- leaves ordinary Study behavior unchanged;
- does not depend on Desktop-only transient state;
- remains isolated from Product Brain in the initial scope.

These criteria are not implementation tests and are not frozen acceptance clauses.

## 11. Evidence Traceability

| Capability statement | Source evidence | Test evidence | Classification | Confidence |
|---|---|---|---|---|
| A StudySession is bounded and has explicit active/finished lifecycle | `src/main/kotlin/vn/loi/learning/domain/study/session/model/StudySession.kt` — `StudySession` invariants and `finish` (lines 21–90, 237–258) | `src/test/kotlin/vn/loi/learning/domain/study/session/model/StudySessionLifecycleTest.kt` — `committed review clears transient checkpoint without adding pause state` | Source-backed; Test-backed | High |
| A queue plan and persisted snapshot reject duplicate LearningItemId values | `src/main/kotlin/vn/loi/learning/application/study/StudyQueuePlan.kt` — constructor invariant (lines 29–60); `src/main/kotlin/vn/loi/learning/application/session/StudyQueueSnapshot.kt` — constructor invariant (lines 31–67) | `src/test/kotlin/vn/loi/learning/application/study/StudyQueuePlanTest.kt` — `plan rejects duplicate learning item ids`; `src/test/kotlin/vn/loi/learning/application/session/StudyQueueSnapshotTest.kt` — `queue rejects duplicate learning item ids` | Source-backed; Test-backed | High |
| Queue progress is immutable navigation over a finite list | `src/main/kotlin/vn/loi/learning/application/session/StudyQueueSnapshot.kt` — `isCompleted`, `advance`, `rewind` (lines 119–121, 301–320) | `src/test/kotlin/vn/loi/learning/application/session/StudyQueueSnapshotTest.kt` — `advance returns next immutable queue snapshot`; `advancing final item completes queue` | Source-backed; Test-backed | High |
| Full planning and queue persistence occur when a session starts | `src/main/kotlin/vn/loi/learning/application/session/StartStudySessionUseCase.kt` — `execute`, `createQueueWhenEnabled`, `persistPlan` (lines 39–118); `src/main/kotlin/vn/loi/learning/application/study/StudyQueuePlanningService.kt` — `plan` (lines 31–154) | `src/test/kotlin/vn/loi/learning/application/session/StudyQueueLifecycleIntegrationTest.kt` — `session uses persisted queue from start through finish` | Source-backed; Test-backed | High |
| Active queue has no append/extend authority | `src/main/kotlin/vn/loi/learning/application/session/StudyQueueService.kt` — `create`, `get`, `require`, `advance`, `rewind`, `delete` (lines 18–119); `src/main/kotlin/vn/loi/learning/application/port/StudyQueueRepository.kt` (lines 6–21) | `src/test/kotlin/vn/loi/learning/application/session/StudyQueueLifecycleIntegrationTest.kt` — `successful review advances queue exactly once` | Source-backed; Test-backed corroboration | High |
| Review uses pending intent plus transactional commit and queue advance | `src/main/kotlin/vn/loi/learning/application/session/ReviewSessionItemUseCase.kt` — `execute`, `resumePending` (lines 34–168) | `src/test/kotlin/vn/loi/learning/application/session/ReviewSessionItemTransactionTest.kt` — `interruption keeps one pending intent that can be resumed exactly once`; `review session item runs inside exactly one transaction` | Source-backed; Test-backed | High |
| Undo reverses only the latest committed review and rewinds queue/session | `src/main/kotlin/vn/loi/learning/application/session/UndoLatestSessionReviewUseCase.kt` — `execute` (lines 12–45); `src/main/kotlin/vn/loi/learning/domain/study/session/model/StudySession.kt` — `undoLatestReview` (lines 151–176) | `src/test/kotlin/vn/loi/learning/application/session/UndoLatestSessionReviewIntegrationTest.kt` — `undo restores first review memory queue session and is idempotent`; `undo reopens completion after the final review` | Source-backed; Test-backed | High |
| Recovery resumes an incomplete queue and closes an active session when its queue is missing/completed; pending intent replay is attempted only for an existing incomplete queue | `src/main/kotlin/vn/loi/learning/application/session/RecoverActiveStudySessionUseCase.kt` — `execute`, `closeIncompleteSession` (lines 18–123); `src/main/kotlin/vn/loi/learning/application/LearningEngine.kt` — `recoverActiveSession` (lines 374–388) | `src/test/kotlin/vn/loi/learning/application/session/RecoverActiveStudySessionUseCaseTest.kt` — `returns resumable session with queue progress`, `closes active session when persisted queue is missing`, `finalizes active session whose queue already completed`; `src/test/kotlin/vn/loi/learning/application/session/ReviewSessionItemTransactionTest.kt` — `interruption keeps one pending intent that can be resumed exactly once` | Source-backed; Test-backed | High within these cases; not a claim of complete future-flow recovery |
| Queue exhaustion and session finish remain distinct | `src/main/kotlin/vn/loi/learning/application/session/StudyQueueSnapshot.kt` — `isCompleted` (lines 119–121); `src/main/kotlin/vn/loi/learning/application/session/FinishStudySessionUseCase.kt` — `execute` (lines 15–52) | `src/test/kotlin/vn/loi/learning/application/session/StudyQueueLifecycleIntegrationTest.kt` — `session with no eligible items creates completed empty queue`; `finish removes queue and finished session rejects next item` | Source-backed; Test-backed | High |
| Queue and learner-facing session counters use different identities | `src/main/kotlin/vn/loi/learning/application/session/StudyQueueSnapshot.kt` — item-index progress (lines 74–98); `src/main/kotlin/vn/loi/learning/domain/study/session/model/StudySession.kt` — `recordReview`, `reviewedContentIds` (lines 107–148) | `desktop/src/test/kotlin/vn/loi/learning/desktop/ui/study/GeneralStudyContinuationIntegrationTest.kt` — `Review header advances coherently fourteen to thirteen to twelve` | Source-backed; Test-backed | High |
| StudyFacade currently triggers ordinary completion and manual continuation, while LearningShell owns presentation composition | `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyFacade.kt` — `continueGeneralStudyAfterCompletion`, `startSession`, completion path (lines 739–750, 1011–1120, 1308–1357); `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/shell/LearningShell.kt` — `StudyFacade`/`StudyViewModel` composition (lines 137–153) | `desktop/src/test/kotlin/vn/loi/learning/desktop/ui/study/GeneralStudyContinuationIntegrationTest.kt` — `general completion starts a new session from durable memory and next new item`, `general continuation with no candidate returns idle without completion loop`; `desktop/src/test/kotlin/vn/loi/learning/desktop/ui/study/StudyFacadeCompletionRecoveryTest.kt` — `restart after final atomic review projects completed workspace` | Source-backed; Test-backed | High |
| LearningApplicationFactory owns application/infrastructure composition | `src/main/kotlin/vn/loi/learning/infrastructure/LearningApplicationFactory.kt` — `createContext` (lines 357–430) | `src/test/kotlin/vn/loi/learning/infrastructure/LearningApplicationFactoryPersistedQueueIntegrationTest.kt` — `persisted application context restores active session queue after recreation` | Source-backed; Test-backed | High |
| Continuous Review is intended as a higher-level learning flow capability coordinating multiple bounded Cycles, not a mutable session/queue or Product Brain feature | N/A — approved Product Review direction supplied for PLE-032; current runtime contains no Continuous Review Flow | No implementation test exists because PLE-032 is not implemented | Product decision | Approved Product Direction |
| A future coordination boundary must preserve existing boundaries but its exact shape and ownership are undecided | Existing boundaries are evidenced in the rows above; no current Continuous Review coordination boundary exists | No implementation test exists | Product decision for non-Desktop-only authority; inference that coordination is needed; exact design deferred | Approved Product Direction for the boundary constraint; design confidence not assigned |

Line ranges refer to audit commit `685203d768d022e05bb763a18229756bcf0d07f0`.

## 12. Deferred Decisions

CD-01 intentionally leaves these decisions open:

- aggregate, entity, and value-object shape → CD-02 Domain Model;
- exact states and transitions → CD-03 State Machine;
- start, continuation, Stop, leave/resume, restart, and boundary-Undo sequences → CD-04
  Lifecycle & Sequence;
- detailed ownership across Domain, Application, Infrastructure, and Desktop → CD-05 Authority
  Map;
- whether durable flow state is required and, if so, its compatibility model → CD-06
  Persistence Model;
- exact failure classes, retry/termination behavior, and recovery guarantees → CD-07 Failure &
  Recovery Matrix;
- later client, Product Brain, and product-mode extension seams → CD-08 Extension Points;
- the architectural decision and rejected alternatives → CD-09 ADR;
- evidence sufficiency and unresolved blockers before any freeze → CD-10 Freeze Readiness
  Review.

No Acceptance Contract is frozen by this mapping.

## 13. Open Questions

The following questions are genuinely unresolved at CD-01 level:

1. What durable identity, if any, represents one Continuous Review Flow across process restart?
   This is deferred to CD-02 and CD-06.
2. Which exact scope forms are supported initially, and what makes the selected scope stable
   when package/library state changes? This is deferred to CD-02, CD-04, and CD-07.
3. What are the exact flow states and terminal reasons for user Stop, no eligible work,
   recoverable failure, and unrecoverable scope loss? This is deferred to CD-03 and CD-07.
4. At a Cycle boundary, which persisted authority makes latest-review Undo discoverable without
   creating multi-step history or reopening the wrong Cycle? This is deferred to CD-02,
   CD-04, CD-05, and CD-06.
5. What constitutes proof that a newly planned empty Cycle should terminate rather than retry,
   and how are clock-driven future-due items communicated? This is deferred to CD-03, CD-04,
   and CD-07.
6. What non-percentage flow-status vocabulary is understandable and accessible across Desktop
   states? Product wording and presentation validation remain open; UI layout does not belong
   to CD-01.
7. Which failure status may be retried automatically, which requires learner action, and which
   terminates the flow? This is deferred to CD-07.

These questions do not weaken the Source-backed conclusions about existing StudySession,
StudyQueue, review, Undo, or recovery behavior.

## 14. CD-01 Completion Assessment

**Assessment: Completed.**

CD-01 answers the capability-level problem, definition, user outcomes, boundaries,
relationships, invariants, initial scope, non-goals, success criteria, evidence, deferred
decisions, and open questions. The approved Product Review direction is compatible with the
current repository architecture when Continuous Review is treated as a multi-Cycle
higher-level learning flow rather than an extension of an active session or queue.

No contradiction was found in the specific session/queue/review/Undo claims reviewed. One
authority nuance must remain explicit: current runtime is not purely application-orchestrated.
`LearningApplicationFactory` owns application/infrastructure composition, `LearningShell` owns
presentation composition, and `StudyFacade` still triggers ordinary completion and manual
continuation. Therefore, “Continuous Review must not become Desktop-only” is a Product decision,
not a description of an already centralized runtime authority.

Existing recovery evidence covers incomplete-queue resume, pending-intent replay for an
existing incomplete queue, and closure for missing/completed queues. It does not prove the
future Continuous Review Flow recovery model.

PLE-032 remains unimplemented. This Capability Design deliverable is Approved and frozen; no
Acceptance Contract is created or frozen. This assessment does not start or authorize CD-02.

## Usage Policy

CD-01 is the Source of Truth for:

- capability definition;
- capability scope;
- capability boundary;
- terminology;
- preserved invariants;
- product intent.

CD-02 through CD-10 must reference CD-01. If a later deliverable conflicts with CD-01, CD-01
may be changed only through an official revision; later deliverables must not change it
implicitly.
