# CD-02 Capability Domain Model

## 1. Document Status

- Phase: Capability Design
- Deliverable: CD-02 — Capability Domain Model
- Version: 1.0
- Status: Approved
- Deliverable state: Frozen Capability Design Deliverable
- Repository audit baseline: branch `develop`, commit
  `685203d768d022e05bb763a18229756bcf0d07f0`
- Governing capability document:
  `docs/capability-design/PLE-032_CD-01_CONTINUOUS_REVIEW_CAPABILITY_OVERVIEW.md`
- Acceptance Contract state: NOT frozen
- Implementation state: PLE-032 is not implemented

This document analyzes where Continuous Review could fit in the domain model. It does not select
an architecture or define a state machine, lifecycle, persistence schema, repository interface,
use-case API, class, coordinator implementation, transaction change, cleanup behavior, restart
algorithm, UI, scheduler behavior, or planner behavior.

Classification vocabulary:

- **Runtime Fact** — directly observable in current production source.
- **Source-backed** — supported by a named source contract or invariant.
- **Test-backed** — exercised by a named test.
- **Product Decision** — frozen direction from CD-01.
- **Inference** — an interpretation of current evidence, not a runtime fact.
- **Undecided** — evidence is insufficient or the choice belongs to a later deliverable.

## 2. Current Runtime Domain Landscape

Continuous Review does not exist in production runtime. There is no Continuous Review type,
identity, state, repository, use case, or composition wiring. This is a **Runtime Fact** confirmed
by the audited source and a **Product Decision** recorded in CD-01.

The current repository contains several explicitly documented aggregate roots:

| Domain area | Explicitly documented aggregate/root | Classification |
|---|---|---|
| Memory | `MemoryState` | Runtime Fact; Source-backed |
| Content | `Content` | Runtime Fact; Source-backed |
| Content library | `ContentLibrary`, `LibraryCollection` | Runtime Fact; Source-backed |
| Content packaging | `ContentPackage`, `PackageCatalog` | Runtime Fact; Source-backed |
| Installed library | `Library`, `InstalledPackage`, `Collection` | Runtime Fact; Source-backed |

`StudySession` is a behavior-rich domain object with its own `SessionId`, local invariants,
transitions, and repository. Treating it as an aggregate root is a strong **Inference** from
source structure, but the audited source does not explicitly label it an Aggregate Root.

`StudyQueueSnapshot` and `StudyQueuePlan` are Application-layer models, not Domain-layer
aggregates. Both reference `SessionId`; neither owns a `StudySession`, `MemoryState`, or
`ReviewEvent`. This is a **Runtime Fact** and **Source-backed**.

`ReviewEvent` is an immutable Memory Domain record persisted through its own append-oriented
repository. `MemoryState` is explicitly the Memory Domain Aggregate Root. The event validates a
transition between `stateBefore` and `stateAfter`, but it is not stored inside `MemoryState`.
Therefore, saying that `ReviewEvent` belongs to the Memory Domain is **Source-backed**; saying it
is an entity inside the `MemoryState` aggregate would be unsupported.

## 3. Current Layer Responsibilities

This section records current runtime authority only. It does not propose a future allocation for
Continuous Review.

| Layer | Current responsibilities | Evidence | Things intentionally NOT owned |
|---|---|---|---|
| Domain | Owns domain models and local invariants, including bounded `StudySession` behavior, `MemoryState` validity and transitions, and `ReviewEvent` transition consistency | `src/main/kotlin/vn/loi/learning/domain/study/session/model/StudySession.kt`; `src/main/kotlin/vn/loi/learning/domain/study/memory/model/MemoryState.kt`; `src/main/kotlin/vn/loi/learning/domain/study/memory/model/ReviewEvent.kt` | Compose/UI, filesystem persistence, concrete repositories, transaction implementation, queue-planning orchestration |
| Application | Coordinates use cases across ports; plans, creates and navigates persisted queues; starts/finishes/reviews/recovers sessions; defines transaction membership for committed review and Undo | `src/main/kotlin/vn/loi/learning/application/study/StudyQueuePlanningService.kt`; `src/main/kotlin/vn/loi/learning/application/session/ReviewSessionItemUseCase.kt`; `src/main/kotlin/vn/loi/learning/application/LearningEngine.kt` | Concrete JSON/filesystem behavior, Compose rendering, scheduler algorithm ownership, mutation of domain invariants outside domain methods |
| Infrastructure | Implements repositories and transaction runners; composes persisted/in-memory adapters through `LearningApplicationFactory` | `src/main/kotlin/vn/loi/learning/infrastructure/LearningApplicationFactory.kt`; `src/main/kotlin/vn/loi/learning/infrastructure/transaction/JsonFileTransactionRunner.kt`; `src/main/kotlin/vn/loi/learning/infrastructure/transaction/InMemoryTransactionRunner.kt` | Product flow decisions, domain invariant definition, Desktop presentation |
| Desktop | Owns presentation composition and currently triggers parts of ordinary Study orchestration through `StudyFacade`, including completion and manual continuation | `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/shell/LearningShell.kt`; `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyFacade.kt`; `desktop/src/test/kotlin/vn/loi/learning/desktop/ui/study/GeneralStudyContinuationIntegrationTest.kt` | Domain invariants, persistence implementation, scheduler/planner semantics; CD-01 also prohibits a future Continuous Review authority existing only as transient Desktop state |

The current layers do not contain Continuous Review authority because the capability is not
implemented. The table must not be read as a proposed placement for that future authority.

## 4. Current Aggregate Analysis

### 4.1 MemoryState

`MemoryState` is explicitly documented as the Memory Domain Aggregate Root. Its identity is the
pair `LearnerId` + `LearningItemId`; no separate `MemoryStateId` exists. It owns invariants for
review/lapse counts, last-review presence, due state, and the state transition produced by
`afterReview`.

Classification: **Runtime Fact; Source-backed; Test-backed through review integration tests**.

### 4.2 StudySession

`StudySession` owns:

- `SessionId`, learner, immutable policy and content/package/topic scope;
- active/finished consistency;
- reviewed item/content tracking and New/Review counters;
- current presentation and pending-review checkpoint;
- latest-review Undo checkpoint;
- completion snapshot;
- transition methods for presentation, reveal, review staging, committed review, Undo, and
  finish.

`StudySessionRepository` loads and saves `StudySession` independently and queries active or
latest-undoable sessions. These characteristics support the **Inference** that `StudySession`
acts as an aggregate root in practice. That interpretation is not upgraded to Runtime Fact
because neither the model nor repository documentation explicitly assigns that DDD label.

### 4.3 StudyQueue

There is no Domain-layer `StudyQueue` aggregate. Runtime uses:

- `StudyQueuePlan`, an immutable Application result of one planning operation; and
- `StudyQueueSnapshot`, an immutable Application navigation snapshot persisted by
  `StudyQueueRepository`.

Both are keyed by the owning cycle's `SessionId`. The snapshot owns only queue-local
invariants: unique `LearningItemId` values, valid origin/content mappings, bounded workloads,
and a valid cursor. It explicitly excludes content, MemoryState, scheduling data, and selection
rules.

Conclusion: the queue is a separately persisted Application model associated with a
`StudySession` through `SessionId`. Whether to call it an aggregate is **Undecided terminology**;
calling it part of the in-memory `StudySession` object graph would contradict the runtime.

### 4.4 ReviewEvent

`ReviewEvent`:

- has a distinct `ReviewEventId`;
- is immutable and normally append-only;
- carries `MemoryState` before and after a single review;
- enforces same learner/item identity and an exact one-review transition;
- is persisted separately from `MemoryState` and `StudySession`.

It belongs to the Memory Domain but is not evidence of a nested `MemoryState` entity. The
current one-step Undo path may remove only the expected latest event within an Application-owned
transaction. Aggregate membership beyond these facts is **Undecided**.

### 4.5 Other explicit aggregate roots

Content, content-library, package, and installed-library aggregates retain their own identities
and repositories. They provide scope and learning-item context but do not currently contain or
own `StudySession`, `StudyQueueSnapshot`, `MemoryState`, or `ReviewEvent`.

## 5. Aggregate Interaction Matrix

The matrix describes current visibility and references. It does not introduce relationships.

| Aggregate or boundary | May reference today | Must not own | Primary invariants | Current evidence |
|---|---|---|---|---|
| `MemoryState` | `LearnerId`, `LearningItemId`, memory value objects | `StudySession`, queue, content payload, `ReviewEvent` collection | Review/lapse counts, reviewed timestamp consistency, due/stage validity | `MemoryState.kt`; review transaction tests |
| `StudySession` behavioral boundary | Learner, item/content and selected scope identities; policy and local checkpoints | `StudyQueueSnapshot`, planner, scheduler, `MemoryState`, `ReviewEvent` repository | Bounded counters, active/finished validity, presentation/pending-review/Undo checkpoints | `StudySession.kt`; lifecycle and queue integration tests |
| `StudyQueueSnapshot` Application boundary | `SessionId`, `LearningItemId`, `ContentId`, item origin | `StudySession`, `MemoryState`, scheduler data, selection rules | Unique items, valid mappings, bounded workload metadata and cursor | `StudyQueueSnapshot.kt`; snapshot tests |
| `ReviewEvent` record | Before/after `MemoryState`, learner/item identity derived from those states | `StudySession`, queue, mutable event history | Same learner/item, one-count transition, reviewed timestamp | `ReviewEvent.kt`; review/Undo integration tests |
| Content aggregates | Their content, field, media, topic/package identities as defined by current models | Study-cycle state, queue navigation, memory history | Content-local validity and identity | Content domain models and repositories |
| Library/package aggregates | Their package, library and collection identities/references | Session, queue, MemoryState and ReviewEvent ownership | Package/library/collection-local and documented cross-aggregate rules | Library and packaging domain models/repositories |

`May reference today` means a reference is present in the audited runtime model. It does not
grant mutation authority or imply aggregate containment.

## 6. Current Aggregate Boundaries

| Boundary | Inside today | Outside today | Classification |
|---|---|---|---|
| MemoryState | One learner/item memory state and its local transition invariants | Session, queue, event repository, content | Runtime Fact; Source-backed |
| StudySession behavioral boundary | Session policy/scope, counters, presentation checkpoint, pending review, latest Undo checkpoint, completion | Queue snapshot, MemoryState persistence, ReviewEvent persistence, planner | Source-backed; aggregate-root status is Inference |
| StudyQueueSnapshot | Fixed item order, origins/content mappings, workload metadata, cursor | Session transitions, scheduler, MemoryState, selection rules | Runtime Fact; Source-backed |
| ReviewEvent record | One immutable before/after review record | Session and queue mutation, event collection ownership | Runtime Fact; Source-backed |
| Content/package/library aggregates | Their documented local content and library invariants | Study-cycle continuation and review transaction | Runtime Fact; Source-backed |

Cross-boundary review consistency is not owned by one domain aggregate. The Application use case
stages intent on `StudySession`, then uses `TransactionRunner` to persist `MemoryState`, append
`ReviewEvent`, save the updated `StudySession`, and advance the queue as one workflow. This is a
**Runtime Fact; Source-backed; Test-backed**.

Cycle completion is also split: queue exhaustion is a `StudyQueueSnapshot` fact, while session
finish is a `StudySession` transition invoked by an Application use case. The queue is then
deleted while the finished session remains for history.

## 7. Current Identity Boundaries

### Runtime Identity

| Identity | Identifies | Boundary implications | Classification |
|---|---|---|---|
| `SessionId` | One bounded `StudySession` | Also keys its one persisted queue plan/snapshot; does not identify a multi-Cycle flow | Runtime Fact; Source-backed |
| `LearnerId` | Learner ownership | Participates in session lookup and composite MemoryState identity | Runtime Fact; Source-backed |
| `LearningItemId` | One learnable item | Queue entry identity; MemoryState component; ReviewEvent-derived item identity | Runtime Fact; Source-backed |
| `ContentId` | Content shared by possible sibling learning items | Session scope and learner-facing counter uniqueness differ from queue item identity | Runtime Fact; Source-backed; Test-backed |
| `ReviewEventId` | One committed review record | Supports latest-event verification for Undo; does not identify a Cycle or flow | Runtime Fact; Source-backed |
| `TopicId` / `InstalledPackageId` | Current lesson/package scope on a session | Existing scope anchors, but not evidence of Continuous Review identity | Runtime Fact; Source-backed |

No identity spans multiple bounded sessions as one Continuous Review Flow. Whether such an
identity is required is **Undecided** and must not be inferred from `SessionId`, learner identity,
or scope identity.

### Potential Future Identity

| Possible identity concept | Possible purpose under analysis | Status |
|---|---|---|
| Flow identity (sometimes described hypothetically as `FlowId`) | Distinguish one learner-selected Continuous Review activity across multiple Cycles | **Undecided** — no such identity or type exists |
| Scope identity (sometimes described hypothetically as `ScopeId`) | Refer to stable learner-selected scope independently of current topic/package/content identities | **Undecided** — no evidence yet requires a new identity |

These labels are analytical placeholders, not proposed type, class, API, schema, or naming
decisions. Later work may conclude that neither identity is needed or may use existing
identities without adding a new one.

## 8. Current Invariant Ownership

| Invariant | Current owner | Classification |
|---|---|---|
| Bounded session policy and counters | `StudySession` | Source-backed |
| Active/finished and finish timestamp consistency | `StudySession` | Source-backed |
| Current-item, reveal, pending-review and Undo checkpoints | `StudySession` | Source-backed |
| Unique queue item identities and valid cursor | `StudyQueuePlan` / `StudyQueueSnapshot` | Source-backed; Test-backed |
| Candidate eligibility, ordering and limits | Existing planner/planning service | Runtime Fact; Source-backed; Test-backed |
| Queue creation/advance/rewind/delete workflow | `StudyQueueService` | Runtime Fact; Source-backed; Test-backed |
| Memory review/lapse and due-state validity | `MemoryState` | Source-backed |
| Review before/after identity and count transition | `ReviewEvent` | Source-backed |
| Atomic committed-review workflow | `ReviewSessionItemUseCase` + `TransactionRunner` | Runtime Fact; Source-backed; Test-backed |
| Latest-review-only reversal | `StudySession` checkpoint plus Application Undo workflow | Source-backed; Test-backed |
| Active-session/queue reconciliation | `RecoverActiveStudySessionUseCase` | Runtime Fact; Source-backed; Test-backed for audited cases |
| Ordinary completion/manual continuation trigger | `StudyFacade` currently triggers it through existing engine APIs | Runtime Fact; Source-backed; Test-backed |
| Application/infrastructure composition | `LearningApplicationFactory` | Runtime Fact; Source-backed; Test-backed |

Continuous Review must not take ownership of planner semantics, scheduler semantics,
`StudySession` internals, queue mutation, review transaction membership, or latest-review Undo
semantics. This is a **Product Decision** frozen in CD-01.

The capability may decide when a new-Cycle planning boundary is requested; the planner continues
to decide how that queue is created. This is a **Product Decision** preserving current
Source-backed ownership.

## 9. Candidate Domain Models

These are analysis options, not architecture decisions. Names describe conceptual placement
only; they do not propose classes, APIs, storage, states, or implementation.

### Option A — Capability represented outside the Domain model

#### Overview

Treat Continuous Review as a higher-level capability that coordinates existing bounded
`StudySession` cycles without adding a new Domain aggregate or Domain service. Existing domain
objects remain the complete domain model; cross-Cycle intent is handled outside it.

Classification: **Candidate inference**, not a Runtime Fact or decision.

#### Advantages

- Preserves all current aggregate and identity boundaries.
- Matches the current fact that planning, recovery, review transactions, and ordinary
  continuation already cross boundaries in Application/Desktop orchestration.
- Avoids inventing a domain identity before durability and invariant needs are known.

#### Disadvantages

- May leave stable flow intent, terminal meaning, or cross-Cycle rules without an obvious Domain
  owner if later analysis proves they are business invariants.
- Risks an orchestration-only model becoming Desktop-local unless CD-01's non-Desktop-only
  constraint is enforced.

#### Compatibility

Compatible with bounded sessions, fixed queues, existing planner ownership, and ordinary Study
isolation. Compatibility with restart/resume and boundary Undo cannot be established without
later lifecycle and persistence analysis.

#### Evidence

Current runtime has no Continuous Review domain type. Existing Application use cases already
coordinate `StudySession`, queue, MemoryState, ReviewEvent, repositories, and transactions.

#### Unknowns

- Whether continuous-flow intent has domain invariants independent of orchestration.
- Whether a durable cross-Cycle identity is required.
- Whether a non-Domain representation can satisfy restart and Undo requirements safely.

#### Architectural Risk

**Risk: Medium.** The option fits current seams with few domain changes, but may under-model
business invariants or drift into Desktop-local orchestration. This rating records trade-off
exposure for a later ADR and does not select or reject the option.

### Option B — Continuous Review as a distinct Domain aggregate

#### Overview

Model Continuous Review as a separate conceptual aggregate that coordinates references to
bounded Cycle identities while never owning `StudySession` itself.

Classification: **Candidate inference**, not a Runtime Fact or decision.

#### Advantages

- Could provide one explicit owner for stable scope, flow identity, and cross-Cycle invariants if
  those are proven to be durable business concepts.
- Could distinguish Cycle identity from flow identity.
- Could make domain language for Stop and terminal outcomes explicit.

#### Disadvantages

- No current runtime evidence establishes the need for a new aggregate or identity.
- Risks duplicating `StudySession` lifecycle, scope, counters, or Undo ownership.
- Could introduce consistency and transaction questions across multiple aggregates.
- Premature selection would violate CD-02's analysis-only boundary.

#### Compatibility

Potentially compatible only if it references rather than owns bounded sessions, never mutates
active queues, and leaves planner/review/scheduler invariants unchanged. Persistence and
transaction compatibility are **Undecided**.

#### Evidence

CD-01 establishes stable flow intent across multiple bounded Cycles and a non-Desktop-only
authority constraint. Current source provides no aggregate, repository, identity, or lifecycle
evidence for this option.

#### Unknowns

- Which invariants would justify an aggregate boundary.
- Whether the concept requires durable identity or durable state.
- How it would avoid competing authority with `StudySession`.
- Whether its consistency boundary can coexist with the committed-review transaction.

#### Architectural Risk

**Risk: High.** This option creates the greatest risk of overlapping `StudySession` authority
and introducing unsupported identity or consistency assumptions. The rating does not establish
that an aggregate will be created or that the option must be rejected.

### Option C — Continuous Review as a Domain concept/service over existing aggregates

#### Overview

Represent cross-Cycle continuation rules as a Domain-level concept or stateless decision
service, while existing `StudySession`, queue, MemoryState, and ReviewEvent boundaries remain
unchanged.

Classification: **Candidate inference**, not a Runtime Fact or decision.

#### Advantages

- Could express business decisions without claiming ownership of sessions or queues.
- Avoids requiring a new aggregate identity if the decision is derivable from inputs.
- Can preserve planner ownership by deciding only whether/when another Cycle is considered.

#### Disadvantages

- A stateless concept cannot itself retain stable intent, resume information, or history.
- May be an artificial Domain abstraction if continuation is primarily workflow orchestration.
- Evidence does not yet identify a pure domain rule independent of persistence and lifecycle
  context.

#### Compatibility

Potentially compatible with all CD-01 invariants if it remains decision-only and does not absorb
planner, scheduler, transaction, recovery, or `StudySession` behavior. Its placement and inputs
remain **Undecided**.

#### Evidence

The repository contains examples of Domain services coordinating or calculating domain rules,
but none for Continuous Review. CD-01 defines continuation as product intent, not an existing
domain rule.

#### Unknowns

- Whether the decision can be pure and deterministic.
- Which facts are domain inputs versus Application/recovery facts.
- Where durable intent would live if required.

#### Architectural Risk

**Risk: Medium.** A domain concept could clarify decision rules, but evidence may show that it
is an artificial abstraction or cannot represent durable intent. The rating is comparative
input for a later ADR, not a recommendation.

## 10. Comparative Analysis

| Criterion | Option A: outside Domain | Option B: distinct aggregate | Option C: Domain concept/service |
|---|---|---|---|
| Matches current runtime | Strongest; no Continuous Review domain type exists | Weak; requires a new boundary | Moderate; requires a new concept but not necessarily identity |
| Explicit business-invariant owner | Weak unless orchestration rules are sufficient | Strong if new invariants are proven | Moderate for stateless rules |
| Requires new identity | Not necessarily | Likely, but undecided | Not necessarily |
| Risks competing with StudySession | Low if boundaries are respected | Highest | Moderate |
| Handles durable intent by itself | No conclusion | Possible, not evidenced | No, if stateless |
| Preserves planner ownership | Yes if orchestration only invokes planner | Yes only with strict separation | Yes if decision-only |
| Evidence strength today | Current seams support feasibility | Product need supports consideration, not necessity | Product rules support consideration, not placement |
| Main unresolved issue | Whether orchestration is sufficient domain expression | Whether aggregate-level invariants/identity exist | Whether a pure domain rule exists |

No option is selected. Current evidence is sufficient to preserve boundaries and identify
trade-offs, but insufficient to decide whether Continuous Review belongs outside the Domain,
requires a distinct aggregate, or warrants a Domain concept/service.

## 11. Architectural Constraints

Any later design must preserve CD-01:

1. Continuous Review coordinates bounded `StudySession` Cycles and does not own `StudySession`.
2. Every Cycle retains a distinct `SessionId`, immutable policy, and fixed persisted queue.
3. An active queue is never appended, replaced, extended, reordered, or silently replanned.
4. Planner semantics remain unchanged: Continuous Review may decide when planning is requested;
   the planner decides how the queue is created.
5. Scheduler, `MemoryState`, rating, and `ReviewEvent` semantics remain unchanged.
6. Existing committed-review transaction integrity remains intact.
7. No committed review may disappear or be duplicated during automatic Cycle transition.
8. Latest-review-only Undo remains the product boundary; no second Undo authority is created.
9. Ordinary Study remains isolated.
10. Continuous Review authority must not exist only as transient Desktop state.
11. Product Brain remains outside initial scope.
12. Queue exhaustion, session completion, and flow termination remain distinct concepts.
13. Continuous Review must not invalidate any existing aggregate boundary. This is a
    capability-level constraint, not an implementation rule.

## 12. Open Decisions

The following remain **Undecided**:

1. Whether Continuous Review needs any Domain representation.
2. Whether cross-Cycle product rules are Domain invariants or workflow policies.
3. Whether a durable flow identity is required.
4. Whether stable learner-selected scope is a value, a reference, or only workflow input.
5. Whether Option A, B, C, or a refined alternative best fits the proven invariants.
6. Whether latest-review Undo discoverability creates a cross-Cycle domain invariant.
7. Whether Stop and terminal reasons require a domain vocabulary.
8. Whether any future consistency boundary spans more than the existing committed-review
   transaction.
9. Exact state, lifecycle, persistence, recovery, authority allocation, APIs, and implementation.

These questions belong to Chief Architect review and later deliverables. CD-02 does not answer
them by implication.

## 13. Evidence Traceability

| Statement | Source evidence | Test evidence | Classification | Confidence |
|---|---|---|---|---|
| `MemoryState` is the Memory Domain Aggregate Root | `src/main/kotlin/vn/loi/learning/domain/study/memory/model/MemoryState.kt` — class documentation and invariants (lines 5–19, 49–79) | `src/test/kotlin/vn/loi/learning/application/session/ReviewSessionItemTransactionTest.kt` — `interruption keeps one pending intent that can be resumed exactly once` | Runtime Fact; Source-backed; Test-backed in workflow | High |
| `StudySession` owns bounded policy, scope, counters, checkpoints and transitions | `src/main/kotlin/vn/loi/learning/domain/study/session/model/StudySession.kt` — properties/invariants and transitions (lines 21–92, 94–258) | `src/test/kotlin/vn/loi/learning/domain/study/session/model/StudySessionLifecycleTest.kt` — `committed review clears transient checkpoint without adding pause state` | Source-backed; Test-backed | High |
| Calling `StudySession` an Aggregate Root is interpretation, not an explicit source label | `src/main/kotlin/vn/loi/learning/domain/study/session/model/StudySession.kt`; `src/main/kotlin/vn/loi/learning/application/port/StudySessionRepository.kt` (lines 8–29) | N/A | Inference | Medium |
| Queue plan/snapshot are Application models keyed by `SessionId`, outside the StudySession object graph | `src/main/kotlin/vn/loi/learning/application/study/StudyQueuePlan.kt` — model contract (lines 13–29, 29–60); `src/main/kotlin/vn/loi/learning/application/session/StudyQueueSnapshot.kt` — model contract (lines 10–42) | `src/test/kotlin/vn/loi/learning/application/session/StudyQueueLifecycleIntegrationTest.kt` — `session uses persisted queue from start through finish` | Runtime Fact; Source-backed; Test-backed | High |
| Queue identities are unique and cursor navigation is bounded | `src/main/kotlin/vn/loi/learning/application/session/StudyQueueSnapshot.kt` — invariants and navigation (lines 44–69, 301–320) | `src/test/kotlin/vn/loi/learning/application/session/StudyQueueSnapshotTest.kt` — `queue rejects duplicate learning item ids`, `advancing final item completes queue` | Source-backed; Test-backed | High |
| Planner owns how a queue plan is created for an active session | `src/main/kotlin/vn/loi/learning/application/study/StudyQueuePlanningService.kt` — `plan` (lines 31–154) | `src/test/kotlin/vn/loi/learning/application/study/StudyQueuePlanningServiceIntegrationTest.kt` — `service creates plan from active session scope`, `service rejects finished session` | Runtime Fact; Source-backed; Test-backed | High |
| Start persists session and then creates/persists its queue when enabled | `src/main/kotlin/vn/loi/learning/application/session/StartStudySessionUseCase.kt` — `execute`, `createQueueWhenEnabled`, `persistPlan` (lines 39–118) | `src/test/kotlin/vn/loi/learning/application/session/StudyQueueLifecycleIntegrationTest.kt` — `session uses persisted queue from start through finish` | Runtime Fact; Source-backed; Test-backed | High |
| Review commit crosses MemoryState, ReviewEvent, StudySession and queue inside one Application transaction | `src/main/kotlin/vn/loi/learning/application/session/ReviewSessionItemUseCase.kt` — `execute`, `resumePending` (lines 34–168) | `src/test/kotlin/vn/loi/learning/application/session/ReviewSessionItemTransactionTest.kt` — `review session item runs inside exactly one transaction`, `interruption keeps one pending intent that can be resumed exactly once` | Runtime Fact; Source-backed; Test-backed | High |
| `ReviewEvent` is an immutable Memory Domain record with independent identity/repository | `src/main/kotlin/vn/loi/learning/domain/study/memory/model/ReviewEvent.kt` — model contract (lines 3–58); `src/main/kotlin/vn/loi/learning/application/port/ReviewEventRepository.kt` (lines 7–46) | `src/test/kotlin/vn/loi/learning/application/session/UndoLatestSessionReviewIntegrationTest.kt` — `undo restores first review memory queue session and is idempotent` | Runtime Fact; Source-backed; Test-backed | High |
| Latest-review Undo restores memory, event, session and queue through one Application workflow | `src/main/kotlin/vn/loi/learning/application/session/UndoLatestSessionReviewUseCase.kt` — `execute` (lines 17–39) | `src/test/kotlin/vn/loi/learning/application/session/UndoLatestSessionReviewIntegrationTest.kt` — `undo restores first review memory queue session and is idempotent`, `undo reopens completion after the final review` | Runtime Fact; Source-backed; Test-backed | High |
| Recovery reconciles one active session with its queue for specified cases | `src/main/kotlin/vn/loi/learning/application/session/RecoverActiveStudySessionUseCase.kt` — `execute`, `closeIncompleteSession` (lines 29–123) | `src/test/kotlin/vn/loi/learning/application/session/RecoverActiveStudySessionUseCaseTest.kt` — `returns resumable session with queue progress`, `closes active session when persisted queue is missing`, `finalizes active session whose queue already completed` | Runtime Fact; Source-backed; Test-backed | High within audited cases |
| Ordinary continuation is currently triggered by Desktop orchestration | `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyFacade.kt` — `continueGeneralStudyAfterCompletion`, start/completion paths (lines 739–750, 1011–1120, 1308–1357) | `desktop/src/test/kotlin/vn/loi/learning/desktop/ui/study/GeneralStudyContinuationIntegrationTest.kt` — `general completion starts a new session from durable memory and next new item`, `general continuation with no candidate returns idle without completion loop` | Runtime Fact; Source-backed; Test-backed | High |
| Application/infrastructure composition is owned by `LearningApplicationFactory` | `src/main/kotlin/vn/loi/learning/infrastructure/LearningApplicationFactory.kt` — `createContext` and wiring (lines 357–430) | `src/test/kotlin/vn/loi/learning/infrastructure/LearningApplicationFactoryPersistedQueueIntegrationTest.kt` — `persisted application context restores active session queue after recreation` | Runtime Fact; Source-backed; Test-backed | High |
| Continuous Review is a higher-level capability coordinating bounded Cycles and does not yet exist at runtime | `docs/capability-design/PLE-032_CD-01_CONTINUOUS_REVIEW_CAPABILITY_OVERVIEW.md` — Capability Definition and boundaries | No implementation test exists | Product Decision; Runtime absence Source-backed | Approved Product Direction |
| Options A, B and C remain viable analysis candidates | Evidence and constraints in Sections 2–9 of this document | No implementation test exists | Inference; Undecided | Not assigned pending architecture review |

Line ranges refer to audit commit `685203d768d022e05bb763a18229756bcf0d07f0`.

## 14. Completion Assessment

**Assessment: CD-02 analysis complete; no architecture selected.**

The current runtime has explicit aggregate roots in Memory, Content, packaging, content-library,
and installed-library domains. `StudySession` behaves like an independently persisted
consistency boundary, but its Aggregate Root label remains an inference. The Study Queue is a
separately persisted Application model keyed by `SessionId`, not a Domain object nested in
`StudySession`. `ReviewEvent` belongs to the Memory Domain and is persisted separately; evidence
does not establish it as an entity inside `MemoryState`.

Continuous Review has no runtime representation. Three domain-placement options remain viable:
outside-Domain capability orchestration, a distinct Domain aggregate, or a Domain concept/service
over existing boundaries. Current evidence does not justify choosing among them.

This deliverable does not start CD-03, freeze an Acceptance Contract, authorize implementation,
or change CD-01.

CD-02 intentionally stops before architecture selection. Architecture selection belongs to the
ADR, not to Domain Analysis.
