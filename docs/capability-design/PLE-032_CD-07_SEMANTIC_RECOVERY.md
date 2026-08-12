# CD-07 Semantic Recovery

## 1. Document Status

- Phase: Capability Design
- Version: 1.0
- Repository baseline: branch `develop`, commit
  `685203d768d022e05bb763a18229756bcf0d07f0`
- Governing deliverables: CD-01 through CD-06, all Approved and Frozen by current Product/Chief
  Architect status
- Status: Draft
- Deliverable: Chief Architect Review Required
- Contract state: Acceptance Contract NOT frozen
- Implementation state: PLE-032 not implemented
- Continuation: CD-08 not started

CD-07 defines product-level Semantic Recovery: restoration of semantic continuity from
authoritative evidence after interruption. It does not define a recovery or retry algorithm,
implementation state machine, transaction, repository, schema, API, class, UI, or Desktop flow.

## 2. Recovery Vocabulary

- **Recovery:** the product-semantic evaluation that preserves or regains a truthful capability
  continuity from authoritative evidence after interruption or uncertainty.
- **Restore:** recognize a previously established capability fact or shape when its complete,
  durable, authoritative evidence remains valid.
- **Reconstruct:** derive a fact or shape again from sufficient correlated source evidence
  without copying or inventing authority.
- **Revalidate:** ask or consult the canonical authority to confirm that evidence remains
  semantically fit for the current decision.
- **Recoverable:** evidence is incomplete, ambiguous, stale, or conflicting, but no approved
  terminal conclusion is justified.
- **Irrecoverable:** authoritative evidence proves that semantic continuity cannot safely be
  preserved within the current interaction. Exact classification policy is deferred.
- **Terminal Recovery:** publication or restoration of a valid `Terminal` outcome supported by
  the required authoritative reason; it is not “give up on error.”
- **Recovery Evidence:** source, reference, correlation, or derived durable evidence considered
  when assessing semantic continuity.
- **Recovery Authority:** the CD-05 authority entitled to establish a recovery-relevant fact or
  make a capability outcome decision. Recovery itself is not a new authority.
- **Recovery Boundary:** a semantic point where interruption can leave complete, partial,
  conflicting, orphaned, or ambiguous evidence.
- **Recovery Ambiguity:** a preserved condition in which evidence supports no single safe
  conclusion.
- **Recovery Candidate:** a fact, relationship, shape, or outcome being evaluated for restore,
  reconstruction, revalidation, rejection, or preserved ambiguity.
- **Recovery Outcome:** the allowed semantic disposition of a candidate: Restore, Reconstruct,
  Revalidate, Remain Recoverable, Ask Learner, Terminate, Reject, or Deferred.

Recovery is not retry, restart, or object deserialization. Restart may trigger recovery
evaluation; retry may later be one mechanism; restored objects may carry evidence. None is
equivalent to restoration of semantic continuity.

## 3. Recovery Principles

1. Recovery does not create or transfer authority.
2. Recovery never fabricates evidence, intent, Stop, continuation, eligibility, completion,
   committed review, or terminal reason.
3. Recovery cannot create Continuous Review intent from an active ordinary `StudySession`.
4. Recovery preserves fixed queues and cannot create, append, replace, or shadow-mutate one.
5. Recovery produces at most one accepted next Cycle for one continuation decision.
6. Recovery never rewrites, duplicates, or discards canonical committed Study facts.
7. Recovery cannot infer Stop from Leave, inactivity, UI state, process loss, or failure.
8. Recovery preserves durable ambiguity until authoritative evidence resolves it.
9. Missing evidence is not negative evidence; conflicting evidence is not permission to select
   the convenient version.
10. Canonical owners remain Learner, Capability, Existing Study, Existing Planner, Current
    Runtime, and Ordinary Recovery exactly as frozen in CD-05.
11. Recovery uses CD-06 freshness, correlation, orphan, consistency, and evidence-loss semantics.
12. Terminal recovery requires one valid reason: Learner Stop, fresh Planner no-work, or a
    later-approved Failure classification.
13. Ordinary Study recovery remains valid within its demonstrated boundary and does not imply
    complete Continuous Review recovery.
14. Recovery preserves latest-review-only Undo authority and does not create compensation or
    multi-step history.

## 4. Recovery Outcome Inventory

| Outcome | Meaning | Authority | Required Evidence | Never Means |
|---|---|---|---|---|
| Restore | recognize an already-established fact/shape without changing its meaning | canonical fact owner; Capability recognizes shape | complete, correlated, Fresh/Revalidated durable evidence | deserialize and trust blindly |
| Reconstruct | derive the same semantic conclusion from source evidence | Capability for derived lifecycle role; canonical owners for sources | sufficient consistent sources and correlation | invent missing source facts |
| Revalidate | confirm evidence through its canonical authority | the canonical authority being consulted | existing evidence whose freshness/correlation needs confirmation | transfer ownership to Recovery |
| Remain Recoverable | preserve uncertainty without progressing or terminating | Capability outcome authority | incomplete/conflicting/orphaned evidence without terminal proof | retry forever or hide failure |
| Ask Learner | obtain a new learner-authoritative choice when policy permits and evidence cannot decide | Learner for the new decision; Capability for presenting need | known ambiguity and no fabricated default | ask learner to assert Planner/Study facts |
| Terminate | publish/restore valid `Terminal` with authoritative reason | Capability plus reason authority per CD-05 | complete reason and publication evidence | classify any exception as Failure |
| Reject | refuse a candidate representation or inference as invalid/non-authoritative | canonical owner/Capability guard | invalid, impossible, or authority-conflicting evidence | delete canonical source evidence |
| Deferred | acknowledge that allowed outcome depends on an unfrozen policy | future approved design authority | evidence sufficient to locate the open decision | silently choose behavior |

Outcomes may be combined semantically: evidence may first be Revalidated and then support Restore
or Reconstruct. This does not prescribe execution order.

## 5. Recovery Candidate Matrix

| Recovery Candidate | Can Restore | Can Reconstruct | Must Revalidate | Must Reject | Preserve Ambiguity | Authority |
|---|---:|---:|---:|---:|---:|---|
| Live Intent | Yes, from explicit durable evidence | No from ordinary Study | when acceptance/correlation is uncertain | inferred intent | Yes | Learner |
| Selected Scope | Yes | only from authoritative scope evidence | on resume or correlation uncertainty | scope guessed from active queue | Yes | Learner |
| Lifecycle Shape | Yes when complete evidence remains | Yes | all source facts needed by shape | stored label conflicting with sources | Yes | Capability |
| Completed Cycle | Yes by canonical reference | role may be reconstructed | completion and interaction correlation | queue exhaustion alone as completion | Yes | Existing Study; Capability owns role |
| Planner Result | only when semantically Fresh | No outside Planner | whenever context/freshness is uncertain | stale/invalid result for current decision | Yes | Existing Planner |
| Accepted Session | Yes | discover from canonical Study evidence | identity and decision correlation | projection-only acceptance | Yes | Existing Study |
| Pending Continuation Decision | Yes | not from state label | consumption/outcome status | duplicate or terminal-superseded decision | Yes | Capability |
| Next-Cycle Outcome | Yes when one accepted `SessionId` is correlated | discover/reconstruct correlation | Study acceptance and decision | second accepted Cycle | Yes | Existing Study + Capability correlation |
| Pending Review | ordinary Study may restore/resume | from canonical session evidence only | session/queue compatibility | capability-owned copy | Yes within ordinary boundary | Existing Study |
| Study Queue | ordinary incomplete queue may restore | progress views may derive | Session ownership/completion | shadow queue, guessed cursor | Yes if missing/conflicting | Existing Study |
| MemoryState | ordinary canonical state may restore | only through existing canonical evidence | latest review/Undo relationship | capability copy | Yes on conflict | Existing Study |
| ReviewEvent | canonical committed event may restore | no fabricated event | latest event and correlation | projection as commit proof | Yes on partial commit evidence | Existing Study |
| Undo Boundary | checkpoint may restore | flow role may reconstruct | latest-review-only and Cycle correlation | multi-step or non-latest Undo | Yes | Existing Study; Capability for lifecycle effect |
| Terminal Outcome | Yes from complete publication/reason | only from complete authoritative evidence | reason source and interaction correlation | reasonless or conflicting Terminal | Yes | Capability plus reason authority |
| Recoverable Interruption | Yes if ambiguity remains durable | Yes from incomplete/conflicting evidence | source evidence where possible | forced success/terminal | Yes, by definition | Capability using Runtime/Recovery evidence |
| Runtime Observation | restore only if durable and still relevant | No into product intent | applicable canonical source | presentation/process state as authority | Yes | Current Runtime |
| Ordinary Recovery Result | use within demonstrated ordinary boundary | No into Continuous intent | live flow correlation | result as continuation decision | Yes | Ordinary Recovery |

## 6. Recovery Evidence Matrix

| Evidence | Authority | Confidence | Freshness Requirement | Correlation Requirement | May Restore | May Reconstruct | May Revalidate | Must Reject |
|---|---|---|---|---|---:|---:|---:|---|
| explicit intent | Learner | Approved Product Direction | accepted/live | current interaction + scope | Yes | No | Yes | if inferred |
| selected scope | Learner | Approved Product Direction | current interaction | intent and all Cycle requests | Yes | limited | Yes | if guessed |
| continuation decision | Capability | Approved Product Direction | current/unconsumed | preceding Cycle and prospective outcome | Yes | No | Yes | duplicate/invalid |
| Planner eligibility/no-work | Existing Planner | High for current runtime boundary | Fresh/Revalidated | exact scope/request/decision | Yes when Fresh | No | Yes | stale/invalid for current use |
| Study acceptance | Existing Study | Source/Test-backed; High | canonical current fact | decision and accepted `SessionId` | Yes | relationship only | Yes | projection-only claim |
| active/finished Session | Existing Study | Source/Test-backed; High | current canonical status | interaction and Cycle role | Yes | role only | Yes | conflicting copy |
| queue progress/exhaustion | Existing Study | Source/Test-backed; High | canonical queue | Session identity | Yes | derived progress | Yes | shadow cursor |
| pending review intent | Existing Study | Source/Test-backed; High | compatible active Session/queue | current item and Session | Yes | from session record | Yes | capability duplicate |
| committed review | Existing Study | Source/Test-backed; High | canonical commit evidence | Session/item/event/queue | Yes | no fabricated commit | Yes | partial representation as success |
| Undo checkpoint | Existing Study | Source/Test-backed; High | latest only | latest event/Session/queue and flow boundary | Yes | lifecycle effect | Yes | non-latest/multi-step |
| terminal publication | Capability | Approved Product Direction | current interaction | authoritative reason | Yes | shape only | Yes | reasonless/conflicting |
| Stop | Learner | Approved Product Direction | accepted current Stop | current interaction | Yes | No | Yes | inferred Stop |
| failure observation | Runtime/Recovery boundary | policy Undecided | current and authoritative | current interaction/boundary | limited | ambiguity only | Yes | fabricated terminal classification |
| Desktop presentation | Observer only | Source-backed as presentation | irrelevant to durable authority | none sufficient | No | No | No | when used as source |

Confidence applies to evidence classification, not implementation feasibility.

## 7. Recovery Decision Tree

This tree states semantic eligibility, not an algorithm, priority order, or call flow.

```text
Is authoritative evidence available?
├─ No
│  ├─ Could learner authority supply a genuinely new permitted decision?
│  │  └─ Ask Learner or remain Recoverable (policy Deferred)
│  └─ Otherwise remain Recoverable, Reject inference, or Terminate only with valid reason
└─ Yes
   ├─ Is evidence complete, correlated, consistent, and Fresh/Revalidated?
   │  ├─ Previously established fact/shape → Restore
   │  └─ Derivable fact/shape → Reconstruct
   ├─ Is evidence valid but freshness/correlation uncertain?
   │  └─ Revalidate
   ├─ Is evidence partial, orphaned, or outcome-ambiguous?
   │  └─ Preserve ambiguity and Remain Recoverable
   ├─ Is evidence conflicting?
   │  └─ Reject non-canonical claim; revalidate canonical owners; remain Recoverable
   └─ Does authoritative evidence prove a valid terminal reason?
      └─ Terminate / restore Terminal
```

“Evidence impossible” allows rejection of the candidate conclusion. It does not automatically
authorize deletion, Failure, or termination. Exact escalation from irrecoverable evidence is
Deferred pending approved failure policy.

## 8. Recovery Ambiguity

| Ambiguity Type | Required Evidence | Allowed Outcome | Forbidden Assumption |
|---|---|---|---|
| Recoverable ambiguity | live intent plus incomplete/conflicting evidence | preserve, revalidate, reconstruct when later supported, possibly Ask Learner | certainty from absence |
| Terminal ambiguity | terminal publication/reason mismatch | remain recoverable, revalidate reason authority | substitute terminal reason |
| Planner ambiguity | decision exists but result/freshness unknown | revalidate Planner or remain recoverable | eligible, no-work, or safe retry |
| Study ambiguity | acceptance/completion/queue facts conflict or correlation is lost | consult canonical Study evidence; reconstruct role only if proven | guess completion or create another Cycle |
| Decision ambiguity | decision exists but consumption/outcome unknown | preserve decision and at-most-one guard | repeat decision/creation |
| Undo ambiguity | Undo may invalidate completion after boundary crossing | preserve latest Undo authority; revalidate Cycles | ignore Undo, mutate fixed queue, or invent compensation |
| Runtime ambiguity | process observation lacks durable/canonical support | remain recoverable or discard non-authoritative observation | infer product intent/outcome |

Ambiguity may itself be durable. It is resolved only by authoritative evidence or a later
approved learner/policy decision, never silently.

## 9. Restart Semantics

After restart:

### May be restored

- explicit live intent and selected scope when complete durable evidence exists;
- one canonical accepted/incomplete ordinary Study Cycle;
- valid pending review and latest Undo checkpoint within ordinary Study rules;
- a valid terminal outcome with its authoritative reason;
- an explicitly durable unresolved ambiguity.

### May be reconstructed

- lifecycle shape from complete correlated facts;
- `BetweenCycles` from live intent plus exactly one authoritative completed Cycle and no
  terminal/accepted next Cycle;
- `CycleInProgress` from live intent plus one correlated accepted active Session;
- the flow role of canonical Study facts.

### Must be revalidated

- Planner results whose current semantic freshness is not established;
- Session acceptance/completion and queue compatibility;
- decision consumption and next-Cycle correlation;
- terminal reason and Undo effects at Cycle boundaries.

### May be discarded

- Desktop presentation, route, dialog, focus, and transient process state;
- derived projections that can be reproduced from authoritative evidence;
- stale historical evidence as authority for a current action (history itself is not silently
  deleted).

### Must preserve ambiguity

- decision without known Planner/Study outcome;
- accepted Session without observed capability transition;
- completion without proven flow correlation;
- conflicting terminal or Undo boundary evidence.

### May terminate

- only when authoritative evidence supports one valid terminal reason and Capability can
  publish/restore that reason. Missing runtime state alone does not authorize termination.

A resumable ordinary session without durable Continuous Review intent remains ordinary Study;
restart must not promote it into Continuous Review.

## 10. Failure Semantics

| Semantic Failure | Recovery Category | Allowed Response | Forbidden Response |
|---|---|---|---|
| Missing evidence | incomplete/recoverable or irrecoverable by later policy | preserve ambiguity, revalidate, possibly Ask Learner | fabricate missing fact |
| Conflicting evidence | authoritative inconsistency | prefer canonical owner, revalidate, remain recoverable | silently select convenient copy |
| Lost correlation | orphaned evidence | preserve facts separately; reconstruct relationship only if proven | correlate by timing/similarity |
| Lost source evidence | authority cannot prove fact | remain recoverable or later-approved terminal handling | promote projection/derived view |
| Invalid authority | non-owner asserts fact | Reject assertion | accept because persisted |
| Stale Planner evidence | historical eligibility result | revalidate Planner | terminate no-work or create Cycle |
| Invalid Stop | Stop inferred or from another interaction | Reject as Stop | end current interaction |
| Duplicate Cycle evidence | at-most-one invariant threatened/broken | preserve canonical accepted Cycle and ambiguity; later policy handles conflict | create/accept another Cycle |
| Partial review evidence | commit success unproven | ordinary pending/recovery semantics; revalidate Existing Study | count committed review |
| Invalid completion | exhaustion/projection presented as completion | revalidate Existing Study | enter `BetweenCycles` |
| Invalid terminal evidence | publication lacks authoritative reason | remain recoverable/Reject Terminal | invent reason |
| Unsupported runtime observation | transient/non-authoritative | discard as authority or remain recoverable | create intent/state |

These are semantic categories, not exceptions, error codes, retry rules, or a failure taxonomy
implementation.

## 11. Undo Recovery

### Before next Cycle acceptance

If latest-review Undo reopens the completed preceding Session before another Cycle is accepted,
the completion and derived `BetweenCycles` shape must be revalidated. The ordinary Undo result
may support reconstruction of `CycleInProgress`. A pending continuation decision whose
completion guard is no longer true cannot be assumed valid. Exact cancellation behavior remains
Deferred.

### After next Cycle acceptance/presentation

Existing Study still owns latest-review-only Undo. Evidence may now contain an accepted immutable
next Cycle plus a preceding completion that Undo could invalidate. Recovery must:

- preserve both canonical Study facts and their correlations;
- preserve ambiguity rather than ignore Undo or mutate the next queue;
- maintain at-most-one next-Cycle protection;
- avoid a second review ledger or compensating review;
- defer the final learner/product outcome.

CD-07 selects neither final Undo behavior nor compensation, rollback, cleanup, or cross-Cycle
transaction semantics.

## 12. Recovery Invariants

1. Recovery never fabricates intent.
2. Recovery never creates or transfers authority.
3. Recovery never creates a second Cycle for one continuation decision.
4. Recovery preserves ambiguity until authoritative resolution.
5. Recovery respects exactly one canonical owner per fact.
6. Recovery preserves the frozen CD-05 Authority Map.
7. Recovery preserves CD-06 durability, freshness, correlation, consistency, orphan, and loss
   semantics.
8. Recovery does not alter committed `ReviewEvent`, `MemoryState`, Session, or queue facts.
9. Recovery never infers Stop, no-work, Failure, completion, or continuation.
10. Recovery never promotes Desktop or process state into durable evidence.
11. Recovery never treats Planner evidence as Study acceptance.
12. Recovery never treats queue exhaustion as ordinary completion.
13. Recovery keeps fixed queue and immutable Cycle policy boundaries.
14. Recovery preserves exactly one authoritative completed Cycle in `BetweenCycles`.
15. Recovery preserves latest-review-only Undo.
16. Recovery does not turn Leave into Stop.
17. Recovery cannot silently continue when evidence is missing or conflicting.
18. Recovery does not change scheduler, Planner, or ordinary Study semantics.

## 13. Recovery Anti-Patterns

- restoring capability intent from Desktop state;
- inferring intent from an active `StudySession`;
- guessing Planner eligibility or no-work;
- guessing Stop from Leave, inactivity, or application closure;
- guessing completion from queue exhaustion or UI completion;
- guessing queue position from presentation;
- guessing Terminal from missing data or an exception;
- treating object deserialization as semantic restoration;
- retrying until an operation appears to work;
- replaying a continuation decision without resolving its prior outcome;
- creating a replacement Cycle because acceptance is not observed;
- copying canonical Study/Planner evidence into capability-owned truth;
- dropping ambiguity during restart;
- using timestamps/TTL as semantic freshness;
- repairing or deleting conflicting evidence without an approved policy.

## 14. Deferred Decisions

- CD-02 Option A/B/C and architecture selection;
- recovery and retry algorithms;
- exact Ask Learner policy;
- irrecoverable and terminal-Failure policy;
- failure taxonomy implementation or exception hierarchy;
- transaction, atomicity, locking, idempotency, and ordering mechanism;
- repository, schema, serialization, migration, and retention;
- Aggregate, Coordinator, service, API, class, or wiring;
- boundary-Undo final behavior and compensation;
- orphan cleanup and evidence archival;
- scheduler and Planner implementation;
- UI and Desktop flow;
- CD-08 scope and decisions.

## 15. Evidence Traceability

| Recovery conclusion | Evidence | Classification | Confidence |
|---|---|---|---|
| Continuous Review is explicit, preserves committed progress, supports resume/restart, and is not implemented | CD-01 §§3-10 | Product Decision | Approved Product Direction |
| Current domain candidates remain undecided and recovery must not select architecture | CD-02 §§9-14 | Approved analysis constraint | High |
| Leave, resume, restart, failure, Cycle transition, and Undo timing boundaries | CD-03 §§5-10 | Product Decision plus Source-backed boundaries | Approved/High |
| Five lifecycle shapes, legal/illegal transitions, and guards | CD-04 §§6-11 | Product Decision; Derived Facts | Approved Product Direction |
| Learner/Capability/Study/Planner/Runtime/Recovery authority and anti-transfer rules | CD-05 §§3-14 | Approved Authority Map | Approved Product Direction |
| Durable evidence graph, canonical ownership, freshness, ambiguity, correlation, consistency, loss, crash matrix, and reconstruction rules | CD-06 §§3-12, 16-26 | Approved durable semantics | Approved Product Direction |
| Ordinary recovery resumes incomplete queue, closes missing queue, and finalizes completed queue | `src/main/kotlin/vn/loi/learning/application/session/RecoverActiveStudySessionUseCase.kt` — `execute`, `closeIncompleteSession`; `src/test/kotlin/vn/loi/learning/application/session/RecoverActiveStudySessionUseCaseTest.kt` — named missing/completed/incomplete tests | Source-backed; Test-backed | High within audited cases |
| Pending review intent precedes transactional commit and can resume once | `src/main/kotlin/vn/loi/learning/application/session/ReviewSessionItemUseCase.kt` — `execute`, `resumePending`; `src/test/kotlin/vn/loi/learning/application/session/ReviewSessionItemTransactionTest.kt` — `interruption keeps one pending intent that can be resumed exactly once` | Source-backed; Test-backed | High |
| Queue progress survives restart and queue completion remains durable | `StudyQueueSnapshot.kt`; store/repository adapters; `src/test/kotlin/vn/loi/learning/infrastructure/persistence/PersistedStudyQueueLifecycleRestartTest.kt` — queue position/completion restart tests | Source-backed; Test-backed | High |
| Latest review Undo restores event/MemoryState/session/queue and survives restart | `UndoLatestSessionReviewUseCase.kt`; `StudySession.undoLatestReview`; `UndoLatestSessionReviewIntegrationTest.kt`; `PersistedStudyQueueLifecycleRestartTest.kt` | Source-backed; Test-backed | High |
| Session completion persists finished Session and conditionally retains queue for Undo | `FinishStudySessionUseCase.kt` — `execute`; completion/restart tests | Source-backed; Test-backed | High |
| Persisted composition restores ordinary session/queue boundaries | `LearningApplicationFactory.createPersisted`; `LearningApplicationFactoryPersistedQueueIntegrationTest.kt` | Source-backed; Test-backed | High |
| Desktop manual continuation is transient orchestration, not durable capability recovery | `StudyFacade.continueGeneralStudyAfterCompletion`; `GeneralStudyContinuationIntegrationTest.kt` | Source-backed; Test-backed; absence finding | High |
| CD-07 recovery outcomes, ambiguity, restart, and failure semantics | CD-01–CD-06 constraints plus this derivation | Product semantic analysis; not runtime fact | Chief Architect review required |

Line ranges and function locations refer to baseline `685203d...` and may move after later source
changes.

## 16. Completion Assessment

CD-07 provides a complete Draft semantic framework for Chief Architect review:

- Recovery is defined as restoration of semantic continuity, distinct from retry, restart, and
  object restoration.
- Restore, Reconstruct, Revalidate, Remain Recoverable, Ask Learner, Terminate, Reject, and
  Deferred outcomes are bounded by authority and evidence.
- Candidate, evidence, ambiguity, restart, failure, and both Undo timing cases are covered.
- CD-05 authority and CD-06 durable evidence semantics remain unchanged.
- No architecture, Aggregate, Coordinator, repository, schema, transaction, algorithm,
  exception hierarchy, implementation, UI, or Desktop flow is selected.

Open policy areas—irrecoverable classification, Ask Learner, terminal Failure, retry, orphan
handling, and final boundary Undo—are explicitly Deferred. These semantic outputs can inform
CD-08, but CD-08 has not started.

**Draft Complete — Chief Architect Review Required.**
