# CD-03 Lifecycle & Interaction

## 1. Document Status

- Phase: Capability Design
- Deliverable: CD-03 — Lifecycle & Interaction
- Version: 1.0
- Status: Approved
- Deliverable state: Frozen Capability Design Deliverable
- Repository audit baseline: branch `develop`, commit
  `685203d768d022e05bb763a18229756bcf0d07f0`
- Governing frozen deliverables:
  - `docs/capability-design/PLE-032_CD-01_CONTINUOUS_REVIEW_CAPABILITY_OVERVIEW.md`
  - `docs/capability-design/PLE-032_CD-02_CAPABILITY_DOMAIN_MODEL.md`
- Acceptance Contract state: NOT frozen
- Implementation state: PLE-032 is not implemented

This document describes lifecycle, interactions, decision points, and boundary crossings without
describing internal states. It does not select a CD-02 candidate model or design a state
machine, Aggregate, Coordinator, persistence model, API, class, transaction, scheduler,
planner, recovery algorithm, or UI.

Classification vocabulary:

- **Runtime Fact** — observable in current production runtime.
- **Source-backed** — supported by named source evidence.
- **Test-backed** — exercised by a named test.
- **Product Decision** — frozen direction from CD-01.
- **Lifecycle Analysis** — option-independent ordering needed to explain the capability.
- **Undecided** — intentionally deferred because evidence or a later deliverable is required.

## 2. Lifecycle Overview

Continuous Review is a learner-selected higher-level learning flow capability coordinating a
sequence of bounded `StudySession` Cycles. It does not own `StudySession`. Each Cycle keeps its
own `SessionId`, immutable policy, fixed persisted queue, ordinary review behavior, and ordinary
completion boundary.

At overview level, the learner journey is:

1. The learner explicitly requests Continuous Review for a stable selected scope.
2. Existing eligibility and planning boundaries are invoked for a new bounded Cycle.
3. The learner studies through the ordinary Cycle and commits reviews through existing review
   boundaries.
4. At Cycle completion, the capability evaluates whether continuation should be requested.
5. If continuation is appropriate, another bounded Cycle is requested through the same
   new-session/planning boundary.
6. If continuation is not appropriate, the learner receives an explicit terminal outcome.
7. At any supported point, learner Stop, leave/resume, restart, or failure introduces a decision
   about whether and how the flow may continue.

Steps 1 and 4–7 are **Product Decision / Lifecycle Analysis** because Continuous Review is not
implemented. Steps 2–3 reuse **Source-backed and Test-backed** current boundaries. This ordering
does not imply an internal state model or architecture placement.

The lifecycle uses three distinct terms:

- **Stop** — the learner explicitly ends Continuous Review intent.
- **Leave** — the learner exits the experience without automatically ending that intent.
- **Terminate** — the capability ends with an explicit terminal outcome, such as no eligible
  work or a failure from which continuation is unavailable.

These meanings are **Product Decisions**. Their durable representation is **Undecided** and
deferred to CD-06.

## 3. Main Success Flow

1. **Explicit entry.** The learner selects Continuous Review and a supported study scope.
   Continuous-flow intent and stable scope are product requirements; their representation is
   **Undecided**.
2. **Cycle request.** The capability requests a new ordinary bounded Cycle.
3. **Fresh planning.** Existing planning evaluates current eligibility for that new Cycle. The
   capability decides only when planning is requested; the planner decides how the fixed queue
   is created.
4. **Cycle participation.** The learner receives the current queue item, reveals the answer, and
   submits a rating through ordinary Study behavior.
5. **Commit barrier.** After the rating is accepted, the existing committed-review transaction
   must succeed. Only then do the `ReviewEvent`, `MemoryState`, `StudySession` progress, and
   queue progress become durable under their current authority.
6. **Repeat inside the Cycle.** Steps 4–5 repeat through the fixed queue.
7. **Exhaustion observation.** Queue exhaustion is observed only after the successful
   committed-review boundary for the final item.
8. **Ordinary completion.** Ordinary `StudySession` completion occurs after exhaustion is
   observed. Exhaustion and completion remain separate facts.
9. **Continuation decision.** Only after ordinary completion is continuation evaluated.
10. **Next-Cycle planning request.** Only after a positive continuation decision may the
    planning boundary for the next Cycle be requested.
11. **Next Cycle or terminal outcome.** A successfully created Cycle returns to Cycle
    participation; otherwise the capability reports an explicit outcome.

Automatic Cycle transition must not begin before successful committed review. No committed
review may disappear or be duplicated across Steps 5–11. This is a **Product Decision /
Lifecycle requirement**, not a new transaction design.

## 4. Stop Flow

1. The learner explicitly requests Stop.
2. Continuous Review intent ends by learner choice.
3. The capability recognizes that no additional Cycle should be requested automatically.
4. Every already-committed review remains committed; Stop does not roll back learning progress.
5. The current bounded Cycle is handled according to a later-approved Stop policy.
6. The learner receives a clear outcome confirming that Continuous Review will not continue
   automatically.

Stop is distinct from Leave and Terminate. Its exact effect on an in-progress Cycle, queue
retention, ordinary completion, Undo, and durable intent is **Undecided**. Persistence meaning is
deferred to CD-06.

## 5. Leave and Resume Flow

1. **Leave.** The learner exits the experience without explicitly selecting Stop.
2. Leave does not automatically mean flow completion, learner Stop, or rollback of committed
   progress.
3. **Re-entry.** Current durable Study evidence is inspected through existing recovery/session
   boundaries.
4. A decision determines whether an existing incomplete bounded Cycle can be resumed.
5. If the existing Cycle is resumable, ordinary Study resumes from its persisted queue
   position.
6. If no Cycle is resumable, a separate decision determines whether Continuous Review intent
   may request a fresh Cycle or whether the capability must Terminate or report a recoverable
   outcome.

Current runtime proves resume of an incomplete ordinary session with an incomplete persisted
queue. It does not prove retention or reconstruction of Continuous Review intent. The mechanism
and durable distinction between Leave and Stop are **Undecided**, deferred to CD-06 and CD-07.

## 6. Restart Flow

1. The application process ends after zero or more committed reviews.
2. On restart, `LearningApplicationFactory` reconstructs application/infrastructure composition.
3. Existing ordinary recovery reconciles an active `StudySession` with its persisted queue for
   the currently supported cases.
4. Pending review replay may be attempted only under the existing conditions for an incomplete
   queue.
5. A capability-level decision determines whether the learner's Continuous Review can continue,
   requires learner confirmation, or must report a terminal/recoverable outcome.
6. Any continued work still begins or resumes as an ordinary bounded Cycle.

Steps 2–4 are **Runtime Fact / Source-backed / Test-backed** for the audited cases. Step 5 is a
**Product Decision** whose inputs and durable authority remain **Undecided**. This document does
not define restart storage or a recovery algorithm. Restart must not infer continuous-flow
intent while durable authority for that intent remains undecided.

## 7. Failure Flow

| Failure location | Committed progress preservation expectation | Authoritative current boundary | Possible lifecycle outcomes | Classification |
|---|---|---|---|---|
| Entry failure | Existing committed learning remains untouched | Current presentation/application entry; no Continuous Review runtime exists | Report failure; learner action; Terminate | Runtime absence Source-backed; future outcome Product Decision |
| First-Cycle creation/planning failure | Prior committed learning remains untouched; no Cycle is fabricated | `StartStudySessionUseCase`, planning and queue boundaries | Retry may be considered; learner action; Terminate | Current boundary Source/Test-backed; policy Undecided |
| Committed-review failure | No partial or duplicate committed review; existing pending-intent authority remains authoritative | `ReviewSessionItemUseCase` and `TransactionRunner` | Existing pending-review continuation where supported; learner action; Terminate/report | Runtime Fact; Source/Test-backed; flow policy Undecided |
| Ordinary completion failure | Already-committed reviews remain committed | `FinishStudySessionUseCase` and ordinary session/queue boundaries | Retry may be considered; learner action; recoverable outcome | Current boundary Source/Test-backed; policy Undecided |
| Next-Cycle request/creation failure | Completed Cycle remains durable; no duplicate next Cycle | Existing start/planning boundaries; no flow-level runtime owner | Retry may be considered; learner action; Terminate/report | Product Decision; mechanism Undecided |
| Restart/recovery failure | Durable committed reviews are neither lost nor replayed as duplicates | `LearningApplicationFactory`, `LearningEngine`, `RecoverActiveStudySessionUseCase` for current cases | Resume supported evidence; learner action; Terminate/report | Current cases Source/Test-backed; future policy Undecided |
| Undo failure | Unrelated committed reviews remain unchanged; no second Undo authority | `UndoLatestSessionReviewUseCase` and current transaction boundary | Report unavailable/failure; learner action; continuation consequence deferred | Runtime boundary Source/Test-backed; timing behavior Undecided |

For every location, retry versus termination policy is deferred to CD-07. Exact failure classes,
retry limits, diagnostics, cleanup, and recovery mechanics remain **Undecided**. CD-03 does not
design a recovery algorithm.

## 8. Cycle Transition

A Cycle transition is the interaction interval after one bounded Cycle reaches its ordinary
completion boundary and before any next bounded Cycle begins.

It is a lifecycle boundary. CD-03 does not claim that old-Cycle completion, continuation
decision, and new-Cycle creation occur in one transaction. Atomicity, persistence coupling, and
crash boundaries between them are **Undecided**, deferred to CD-06, CD-07, and CD-09.

The lifecycle obligations are:

1. Accept the final rating through ordinary Study.
2. Cross the existing successful committed-review boundary, making review, `MemoryState`,
   `StudySession`, and queue progress durable under current authority.
3. Observe queue exhaustion.
4. Complete the ordinary `StudySession`.
5. Preserve every committed review and latest-review-only Undo semantics.
6. Decide whether Stop, no eligible work, failure, or another terminal reason prevents
   continuation.
7. If continuation may proceed, request fresh eligibility evaluation through the existing
   planning boundary.
8. If eligible work produces a new bounded Cycle, use a new `SessionId`, immutable Cycle policy,
   and fixed queue.
9. If no eligible work exists, Terminate clearly rather than creating an automatic empty-Cycle
   loop.

The transition does not append, replace, extend, reorder, or silently replan the preceding
queue. It does not mutate the completed Cycle into an infinite session. The exact sequencing
authority and durable transition representation are **Undecided**.

One completed Cycle must not successfully create more than one next Cycle for the same
continuation decision. Repeated completion observation, retry, Desktop reprojection, or restart
must not cause duplicate next-Cycle creation. This is a **Product Decision / Lifecycle
requirement**; no idempotency key, lock, record, API, or algorithm is selected.

## 9. Initial No-Work Flow

1. The learner explicitly enters Continuous Review.
2. The selected scope is supplied.
3. The first bounded Cycle is requested.
4. The existing planner reports no eligible work.
5. The capability does not fabricate an active learning Cycle.
6. The capability does not automatically retry in a loop.
7. The capability Terminates with an explicit no-eligible-work outcome.

This differs from no eligible work after a completed Cycle because no preceding Continuous
Review Cycle exists. Planner output is a current boundary fact; terminal behavior is a
**Product Decision**.

## 10. Boundary-Cycle Undo Timing

### Case A — After completion, before next-Cycle creation

Latest-review Undo is requested after ordinary Cycle completion but before the next Cycle is
created.

### Case B — After next-Cycle creation or presentation

Latest-review Undo is requested after a next Cycle has been created or presented.

For both cases:

- Status: **Undecided**.
- Undo remains latest committed review only.
- No multi-step history is introduced.
- No unrelated committed review may be lost.
- A newly created queue must not be silently mutated or replanned.
- No second Undo authority is introduced.

Continuation consequences are deferred to CD-04, CD-06, CD-07, and CD-09 ADR. CD-03 does not
choose final behavior for either timing case.

## 11. Boundary Crossings

| Boundary crossing | Current Owner | Future Capability | Classification |
|---|---|---|---|
| Learner intent enters study experience | Desktop presentation currently receives ordinary Study actions | Recognize explicit Continuous Review intent without making Desktop the sole authority | Current owner: Runtime Fact; future responsibility: Product Decision |
| Selected scope enters a bounded Cycle | `StartStudySessionUseCase` receives session command/scope | Retain stable flow scope while requesting ordinary Cycles | Current owner: Source-backed; future representation: Undecided |
| Session is converted into a queue plan | `StudyQueuePlanningService` and existing planner | Decide when a new-Cycle plan is requested; never decide how it is produced | Current owner: Runtime Fact; future boundary: Product Decision |
| Queue plan becomes persisted navigation | `StartStudySessionUseCase` and `StudyQueueService` | Use the existing fixed-queue boundary for each Cycle | Source-backed; Test-backed; Product Decision |
| Learner rating crosses into committed review | `ReviewSessionItemUseCase` with `TransactionRunner` and repositories | Preserve the boundary unchanged | Runtime Fact; Source-backed; Test-backed |
| Committed review advances queue/session | Existing review workflow and domain methods | Observe Cycle progress without owning review semantics | Runtime Fact; Source-backed; Test-backed |
| Queue exhaustion crosses into session completion | `StudyQueueSnapshot` exposes exhaustion; `FinishStudySessionUseCase` finishes session | Recognize the Cycle boundary without merging the concepts | Runtime Fact; Source-backed; Test-backed |
| Completed Cycle crosses into continuation decision | Ordinary runtime currently exposes manual continuation through `StudyFacade`; automatic Continuous Review does not exist | Decide whether another bounded Cycle should be requested | Current owner: Runtime Fact; future decision: Product Decision |
| Positive continuation decision crosses into next-Cycle creation | Existing start/planning boundaries create ordinary sessions and queues; no flow-level owner exists | Permit at most one successful next Cycle for the same decision | Current boundary: Source/Test-backed; lifecycle requirement: Product Decision; mechanism: Undecided |
| Active Cycle crosses process restart | `LearningApplicationFactory`, `LearningEngine`, and `RecoverActiveStudySessionUseCase` | Determine whether flow-level continuation is supportable after ordinary recovery | Current recovery: Source-backed/Test-backed for audited cases; future behavior: Undecided |
| Latest committed review crosses an Undo request | `UndoLatestSessionReviewUseCase` and existing transaction boundary | Preserve latest-review-only availability across a Cycle boundary | Current owner: Runtime Fact; future requirement: Product Decision |
| Failure crosses into retry/termination decision | Existing use cases own their current failures; no flow-level owner exists | Decide only whether retry is considered, learner action is required, or the flow ends | Runtime absence: Source-backed; future decision: Product Decision/Undecided |

“Future Capability” describes responsibility at capability level only. It does not allocate that
responsibility to an Aggregate, layer, Coordinator, class, or API.

## 12. Decision Points

| Decision | Question | Known inputs at lifecycle level | Possible outcomes described without state | Classification |
|---|---|---|---|---|
| Enter? | Did the learner explicitly request Continuous Review with a supported scope? | Learner intent; selected scope | Begin Cycle request; reject/report unsupported entry | Product Decision; exact validation Undecided |
| Continue? | After a Cycle boundary, should another bounded Cycle be requested? | Stop intent; preceding completion; scope availability; failure context | Request fresh planning; terminate/report outcome | Product Decision |
| Stop? | Has the learner requested that automatic continuation cease? | Explicit learner action | Do not request another Cycle; apply later-approved current-Cycle policy | Product Decision; policy Undecided |
| No eligible work? | Did fresh planning find no eligible work for the stable scope? | Existing planner result | Explicit terminal outcome; no empty-cycle loop | Product Decision using existing planner fact |
| Resume? | Can an existing incomplete Cycle resume safely? | Recovered session/queue evidence | Resume Cycle; consider fresh Cycle; report outcome | Current ordinary cases Test-backed; flow decision Undecided |
| Recover? | Is current durable evidence sufficient to continue without fabricating work or intent? | Session, queue, pending-review and scope evidence | Continue supported recovery; require action; terminate/report | Product Decision; algorithm Undecided |
| Retry? | May the failed interaction be attempted again safely? | Failure timing relative to committed review and existing pending intent | Retry considered; learner action; terminal outcome | Product Decision; exact policy Undecided |
| Undo available? | Does existing latest-review-only evidence permit Undo across the Cycle boundary? | Existing session/event/memory/queue checkpoint evidence | Offer existing Undo; report unavailable | Product requirement; mechanism Undecided |
| Terminate? | Has no eligible work or a non-continuable failure produced an explicit terminal outcome? | Planner result; failure context; supported recovery evidence | Terminate explicitly; otherwise consider supported continuation | Product Decision; exact policy Undecided |

These are decisions, not internal states or transition definitions.

## 13. Lifecycle Invariants

1. No Cycle transition begins before the successful committed-review boundary.
2. No committed review disappears or is duplicated.
3. An active queue is not appended, replaced, extended, or silently replanned.
4. One completed Cycle creates at most one successful next Cycle for the same continuation
   decision.
5. An empty planning result produces an explicit terminal outcome, not an empty-cycle loop.
6. Stop does not roll back committed learning progress.
7. Leave does not automatically mean flow completion or learner Stop.
8. Restart must not infer continuous-flow intent while durable authority for that intent remains
   undecided.
9. Queue exhaustion, `StudySession` completion, and flow Terminate remain three distinct facts.
10. Ordinary Study remains unchanged.
11. The lifecycle neither depends on nor selects CD-02 Option A, B, or C.

These are **Product Decisions / Lifecycle requirements**, not claims of existing Continuous
Review runtime behavior.

## 14. Deferred Decisions

CD-03 intentionally defers:

- all internal state names and state-machine transitions;
- selection among CD-02 Options A/B/C;
- Aggregate, Domain service, orchestration, or layer placement;
- identity and durable representation for flow intent or scope;
- exact Stop behavior for an in-progress Cycle;
- exact leave/resume and restart authority;
- failure taxonomy, retry policy, cleanup, and recovery algorithm;
- persistence schema and repository contracts;
- APIs, classes, composition wiring, and UI;
- transaction changes;
- scheduler and planner design;
- exact handling of latest-review Undo at a Cycle boundary;
- atomicity, persistence coupling, and crash boundaries across completion, continuation, and
  next-Cycle creation;
- the mechanism preventing duplicate next-Cycle creation;
- durable representation of Stop, Leave, Terminate, and continuous-flow intent.

## 15. Evidence Traceability

| Lifecycle statement | Source evidence | Test evidence | Classification | Confidence |
|---|---|---|---|---|
| A Cycle starts as a bounded session and receives one planned/persisted queue | `src/main/kotlin/vn/loi/learning/application/session/StartStudySessionUseCase.kt` — `execute`, `createQueueWhenEnabled`, `persistPlan` (lines 39–118) | `src/test/kotlin/vn/loi/learning/application/session/StudyQueueLifecycleIntegrationTest.kt` — `session uses persisted queue from start through finish` | Runtime Fact; Source-backed; Test-backed | High |
| Planner decides how a queue is created for an active session | `src/main/kotlin/vn/loi/learning/application/study/StudyQueuePlanningService.kt` — `plan` (lines 31–154) | `src/test/kotlin/vn/loi/learning/application/study/StudyQueuePlanningServiceIntegrationTest.kt` — `service creates plan from active session scope`, `service rejects finished session` | Runtime Fact; Source-backed; Test-backed | High |
| Ordinary review commits memory/event/session/queue work through the existing transaction boundary | `src/main/kotlin/vn/loi/learning/application/session/ReviewSessionItemUseCase.kt` — `execute`, `resumePending` (lines 34–168) | `src/test/kotlin/vn/loi/learning/application/session/ReviewSessionItemTransactionTest.kt` — `review session item runs inside exactly one transaction`, `interruption keeps one pending intent that can be resumed exactly once` | Runtime Fact; Source-backed; Test-backed | High |
| Queue exhaustion and session completion are separate interactions | `src/main/kotlin/vn/loi/learning/application/session/StudyQueueSnapshot.kt` — `isCompleted` (lines 116–124); `src/main/kotlin/vn/loi/learning/application/session/FinishStudySessionUseCase.kt` — `execute` (lines 21–52) | `src/test/kotlin/vn/loi/learning/application/session/StudyQueueLifecycleIntegrationTest.kt` — `finish removes queue and finished session rejects next item` | Runtime Fact; Source-backed; Test-backed | High |
| Ordinary recovery can resume an incomplete queue or close missing/completed queue cases | `src/main/kotlin/vn/loi/learning/application/session/RecoverActiveStudySessionUseCase.kt` — `execute`, `closeIncompleteSession` (lines 29–123) | `src/test/kotlin/vn/loi/learning/application/session/RecoverActiveStudySessionUseCaseTest.kt` — `returns resumable session with queue progress`, `closes active session when persisted queue is missing`, `finalizes active session whose queue already completed` | Runtime Fact; Source-backed; Test-backed for audited cases | High within those cases |
| Application recreation restores current persisted queue wiring | `src/main/kotlin/vn/loi/learning/infrastructure/LearningApplicationFactory.kt` — composition wiring | `src/test/kotlin/vn/loi/learning/infrastructure/LearningApplicationFactoryPersistedQueueIntegrationTest.kt` — `persisted application context restores active session queue after recreation` | Source-backed; Test-backed | High |
| Ordinary general continuation is currently manual and Desktop-triggered | `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyFacade.kt` — `continueGeneralStudyAfterCompletion` (lines 739–750) | `desktop/src/test/kotlin/vn/loi/learning/desktop/ui/study/GeneralStudyContinuationIntegrationTest.kt` — `general completion starts a new session from durable memory and next new item`, `general continuation with no candidate returns idle without completion loop` | Runtime Fact; Source-backed; Test-backed | High |
| Latest-review Undo restores the existing boundaries and can reopen completion | `src/main/kotlin/vn/loi/learning/application/session/UndoLatestSessionReviewUseCase.kt` — `execute` (lines 17–39) | `src/test/kotlin/vn/loi/learning/application/session/UndoLatestSessionReviewIntegrationTest.kt` — `undo restores first review memory queue session and is idempotent`, `undo reopens completion after the final review` | Runtime Fact; Source-backed; Test-backed | High |
| Continuous Review coordinates multiple bounded Cycles, supports Stop/resume/restart, and must preserve committed reviews | `docs/capability-design/PLE-032_CD-01_CONTINUOUS_REVIEW_CAPABILITY_OVERVIEW.md` — capability definition, scope, invariants and success criteria | No implementation test exists because PLE-032 is not implemented | Product Decision | Approved Product Direction |
| Lifecycle must remain independent of CD-02 candidate selection | `docs/capability-design/PLE-032_CD-02_CAPABILITY_DOMAIN_MODEL.md` — candidate analysis and completion assessment | No implementation test exists | Product Decision for deliverable scope; Lifecycle Analysis | Approved design constraint |
| Automatic transition waits for successful committed review, exhaustion observation, ordinary completion, continuation decision, and only then next-Cycle planning | Existing review, queue, finish, and planning boundaries evidenced above; their Continuous Review ordering is not implemented | No Continuous Review implementation test exists | Product Decision; Lifecycle requirement | Approved Product Direction |
| One continuation decision may create at most one successful next Cycle | No current Continuous Review runtime authority exists | No implementation test exists | Product Decision; mechanism Undecided | Approved Product Direction |
| Stop, Leave, and Terminate have distinct lifecycle meanings | `docs/capability-design/PLE-032_CD-01_CONTINUOUS_REVIEW_CAPABILITY_OVERVIEW.md` — Stop, leave/resume, and terminal outcomes | No implementation test exists | Product Decision; persistence Undecided | Approved Product Direction |

Line ranges refer to audit commit `685203d768d022e05bb763a18229756bcf0d07f0`.

## 16. Completion Assessment

**Assessment: CD-03 lifecycle and interaction analysis complete.**

The document explains how a learner enters Continuous Review, participates in bounded Cycles,
crosses a Cycle boundary, stops, resumes, restarts, and encounters failure. It identifies the
current owner and future capability responsibility at each material boundary and lists the
decisions that govern continuation without describing internal states.

The lifecycle is compatible with all three CD-02 candidate models because it does not allocate
authority to a new Aggregate, Domain service, layer, Coordinator, class, or API. It preserves
the frozen CD-01 capability boundary and invariants.

CD-03 v1.0 is an Approved, Frozen Capability Design Deliverable. It intentionally stops before
state-machine design, architecture selection, persistence, recovery algorithm, or
implementation. The Acceptance Contract remains NOT frozen, PLE-032 remains unimplemented, and
CD-04 has not started.
