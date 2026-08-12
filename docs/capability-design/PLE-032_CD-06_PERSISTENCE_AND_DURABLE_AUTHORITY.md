# CD-06 Persistence & Durable Authority

## 1. Document Status

- Phase: Capability Design
- Version: 1.0
- Repository baseline: branch `develop`, commit
  `685203d768d022e05bb763a18229756bcf0d07f0`
- Governing deliverables: CD-01, CD-02, CD-03, CD-04, and CD-05, all Approved and Frozen
- Status: Draft
- Deliverable: Chief Architect Review Required
- Contract state: Acceptance Contract NOT frozen
- Implementation state: PLE-032 not implemented
- Continuation: CD-07 not started

This document defines durable semantics and durable authority. It does not choose a CD-02
candidate model, persistence strategy, storage representation, repository, schema, transaction,
locking, retry, recovery algorithm, or implementation placement.

## 2. Persistence Vocabulary

- **Durable Fact:** a fact whose evidence must survive process loss so a later runtime does not
  fabricate, duplicate, or erase a capability outcome.
- **Durable Intent:** authoritative evidence that the learner explicitly began and has not
  terminated one Continuous Review interaction.
- **Durable Decision:** evidence that an authority made a product decision whose repetition or
  loss would change behavior after restart.
- **Durable Evidence:** restart-surviving evidence supporting a fact; storage of the evidence
  does not become authority for the fact.
- **Authoritative Source:** the CD-05 authority entitled to establish a fact or decision.
- **Derived Durable Fact:** a restart-relevant conclusion reproducibly derived from durable
  authoritative evidence without copying ownership.
- **Reconstructed Fact:** a fact assembled after restart from related durable evidence.
- **Revalidated Fact:** a previously represented fact checked again with its source authority.
- **Persistence Boundary:** the semantic point after which required evidence must survive loss
  of the current process. This term does not prescribe a transaction or store.
- **Crash Boundary:** a point where process loss may divide observable steps and leave partial,
  ambiguous, or complete durable evidence.
- **Orphaned Evidence:** valid evidence whose expected related evidence is absent, for example an
  accepted Cycle without an observed capability transition.
- **Duplicate Evidence:** multiple representations purporting to own the same fact, or evidence
  that could authorize the same outcome more than once.

**Authority is not durability.** Authority answers who may establish a fact. Durability answers
which evidence must survive. Persisting, reconstructing, observing, or revalidating a fact never
transfers CD-05 authority to storage, Capability, Desktop, or another participant.

## 3. Durable Evidence Graph

This is a semantic dependency graph, not a sequence diagram or runtime flow. An arrow means that
the downstream conclusion depends on valid upstream evidence; it does not mean ownership,
storage placement, call order, or automatic transition.

```text
Learner Intent
      ↓
Selected Scope
      ↓
Continuation Decision
      ↓
Planner Evidence
      ↓
Study Acceptance
      ↓
Accepted Session
      ↓
Committed Review
      ↓
Completed Cycle
      ↓
BetweenCycles
      ↓
Next Continuation
      ↓
Terminal
```

Branches are permitted: first-Cycle intent may reach Planner evidence without a prior completed
Cycle; Stop may reach Terminal from a live interaction; interruption may preserve ambiguity at
any boundary. The graph expresses dependencies, not mandatory traversal of every node.

| Node | Evidence Type | Authority | Durable? | Derived? | May Reconstruct? | Revalidation Needed? |
|---|---|---|---:|---:|---:|---:|
| Learner Intent | source decision evidence | Learner | Yes after acceptance | No | No, except from explicit durable evidence | Validate that it remains live |
| Selected Scope | source fact/reference | Learner | Yes while intent is live | No | Only from authoritative scope evidence | Yes at resume |
| Continuation Decision | source decision evidence | Capability | Yes before ambiguous execution | No | Not from lifecycle label alone | Yes when unresolved |
| Planner Evidence | source result evidence | Existing Planner | If needed across a crash boundary | No | Re-request/revalidate; never fabricate | Yes when freshness is not Fresh |
| Study Acceptance | source acceptance evidence | Existing Study | Yes through accepted `SessionId` evidence | No | Discover/revalidate from Study | Yes |
| Accepted Session | canonical Study fact/reference | Existing Study | Already durable in ordinary Study | No | Yes from canonical Study evidence | Yes |
| Committed Review | canonical Study fact/reference | Existing Study | Already durable | No | Yes from review/session/queue evidence | Yes when correlated to a boundary |
| Completed Cycle | canonical completion plus capability correlation | Existing Study for completion; Capability for role | Yes by reference/correlation | Capability role is derived | Yes | Yes |
| `BetweenCycles` | lifecycle-shape evidence | Capability | Restart-relevant; direct storage not required | Yes | Yes from live intent and one completion | Yes |
| Next Continuation | source decision/correlation evidence | Capability | Yes before ambiguous execution | No | No from state alone | Yes |
| Terminal | published outcome plus reason evidence | Capability; reason authority per CD-05 | Yes once validly published | Shape is derived from outcome | Yes only from complete evidence | Yes, especially reason evidence |

## 4. Source-of-Truth Matrix

This matrix is the canonical inventory for durable candidate facts in CD-06. Each fact appears
once. Related sections analyze consequences but do not create additional canonical facts.

| Fact | Canonical Source | Durable Representation | Revalidation Source | Capability Ownership | Notes |
|---|---|---|---|---:|---|
| Continuous Review interaction correlation | Capability semantics; representation Undecided | correlation evidence | Capability evidence plus referenced authorities | Yes, for flow relationship only | Does not imply a new ID |
| Explicit learner intent | Learner | source decision evidence | Learner-authorized durable evidence | No | Capability retains, does not originate |
| Selected scope | Learner | source fact/reference | authoritative scope identities | No | Stable during one interaction |
| Scope stability | Capability | derived durable fact | selected-scope evidence | Yes | Derivation, not copied scope ownership |
| Lifecycle shape | Capability | derived/reconstructed evidence | all required source authorities | Yes | Direct persistence not required |
| Active Cycle identity | Existing Study | `SessionId` reference | Existing Study | No | Never duplicate Session identity |
| Authoritative completed Cycle | Existing Study completion; Capability role | completion reference plus correlation | Existing Study + Capability evidence | Only the role | Exactly one in `BetweenCycles` |
| Positive continuation decision | Capability | source decision evidence | Capability evidence | Yes | Must survive ambiguous execution |
| Next-Cycle acceptance outcome | Existing Study | accepted `SessionId` reference/correlation | Existing Study | No | Supports at-most-one invariant |
| Terminal outcome | Capability | published outcome evidence | Capability plus reason authority | Yes | Shape and publication, not source reason |
| Terminal reason — Stop | Learner | explicit Stop evidence | Learner-authorized evidence | No | Capability publishes |
| Terminal reason — NoEligibleWork | Existing Planner | fresh/revalidated result reference | Existing Planner | No | Capability publishes |
| Terminal reason — Failure | authority pending CD-07 | failure evidence plus later-approved decision | applicable Runtime/Recovery authority | Undecided policy | Must not be fabricated |
| Recoverable interruption | Current Runtime/Ordinary Recovery evidence; Capability classification | durable ambiguity/source references | Runtime/Recovery and Capability | Classification only | Missing evidence is not certainty |
| Committed-review boundary | Existing Study | reference to canonical Study evidence | Existing Study | No | No second review ledger |
| Queue exhaustion | Existing Study | derive/reference queue evidence | Existing Study | No | Not identical to completion |
| Ordinary Study completion | Existing Study | finished `StudySession` reference | Existing Study | No | Canonical Cycle completion |
| Planner result | Existing Planner | result evidence/reference if restart-relevant | Existing Planner | No | May become stale |
| Undo boundary | Existing Study | reference to latest Undo checkpoint | Existing Study | No | Latest-review-only |
| Restart/recovery observation | Current Runtime/Ordinary Recovery | source observation evidence when needed | Runtime/Recovery | No | Never creates intent |

## 5. Orphan Evidence Taxonomy

An orphan is evidence whose required semantic relationship cannot currently be established. It
is not proof that the underlying fact is false.

| Type | Definition | Possible Cause | Required Authority | May Restore | Must Revalidate | Forbidden Inference |
|---|---|---|---|---|---|---|
| Intent Orphan | live intent lacks valid scope or interaction correlation | partial durability or lost correlation | Learner/Capability | intent fact only | scope/correlation | active Cycle or continuation |
| Cycle Orphan | accepted/active Session cannot be correlated to live intent | crash or lost relationship | Existing Study + Capability | ordinary Study Cycle | intent and correlation | Continuous Review membership |
| Decision Orphan | continuation decision lacks known planning/acceptance outcome | crash between decision and observation | Capability | decision/ambiguity | Planner/Study outcome | retry safety or accepted Cycle |
| Terminal Orphan | terminal publication or reason lacks its corresponding authoritative evidence | partial publication or lost source evidence | Capability plus reason authority | undisputed source facts only | reason and publication | finality or substitute reason |
| Undo Orphan | Undo checkpoint exists but its capability boundary relationship is absent/invalid | completion/next-Cycle correlation changed | Existing Study + Capability | ordinary latest Undo evidence | Session and flow correlation | multi-step Undo or unchanged completion |
| Planner Orphan | Planner result exists without a live request/decision/scope correlation | crash, stale result, lost decision | Existing Planner + Capability | Planner result as historical evidence | freshness/scope | current eligibility or acceptance |
| Study Orphan | Study completion/acceptance exists without matching capability observation | crash after Study mutation | Existing Study + Capability | canonical Study fact | interaction/decision correlation | new intent, terminal, or second Cycle |

Orphans remain recoverable, historical, or unusable according to later policy. CD-06 does not
define an orphan-resolution algorithm.

## 6. Evidence Freshness

Freshness is semantic validity for the decision currently being made. It is not based on a
timestamp, TTL, cache age, or storage mechanism.

- **Fresh:** established by the canonical authority for the current scope, decision, and
  lifecycle boundary.
- **Revalidated:** previously established evidence confirmed again by its canonical authority
  for the current use.
- **Stale:** valid historical evidence whose scope, decision, or lifecycle context no longer
  authorizes the current conclusion.
- **Unknown:** evidence existence, correlation, or authority cannot currently be established.
- **Invalid:** evidence conflicts with canonical authority or violates an invariant.

| Evidence | Fresh | Revalidated | Stale | Unknown | Invalid |
|---|---|---|---|---|---|
| Planner Result | produced for current scope/request | Planner confirms eligibility again | belongs to prior scope/decision or changed eligibility context | result/correlation unavailable | contradicts canonical Planner evidence |
| Study Completion | canonical finished Session correlated to current Cycle | Existing Study confirms completion and identity | historical completion from another interaction | completion/correlation missing | Session is active/reopened or evidence conflicts |
| Terminal Evidence | publication and authoritative reason agree for current interaction | reason authority and Capability publication reconfirmed | historical terminal from prior interaction | publication/reason relationship incomplete | reason lacks authority or conflicts |
| Stop | explicit accepted Stop for current interaction | authoritative durable Stop confirmed | Stop from prior interaction | acceptance/correlation uncertain | inferred from Leave/inactivity |
| Continuation Decision | one current positive decision tied to one completed Cycle | decision/correlation confirmed | consumed, superseded by Stop/terminal, or belongs elsewhere | execution/outcome unclear | duplicates a decision or violates guard |

Revalidation changes confidence for current use; it does not change canonical ownership.

## 7. Durable Ambiguity

Ambiguity is a semantic condition in which authoritative evidence does not yet support exactly
one safe conclusion. It is not automatically an error. If losing the uncertainty could cause
fabrication or duplication, the ambiguity itself must remain durable.

| Ambiguity | Evidence | Authority | Resolution Category | Forbidden Assumption |
|---|---|---|---|---|
| decision exists; Planner outcome unknown | durable continuation decision; no confirmed result | Capability + Existing Planner | revalidate/recover | no-work, eligibility, or safe retry |
| Planner result exists; Study acceptance unknown | decision + result; no confirmed accepted Session | Planner + Existing Study | revalidate acceptance/freshness | accepted Cycle or definitely no Cycle |
| Study accepted Session; capability observation absent | accepted `SessionId`; missing transition observation | Existing Study + Capability | reconstruct/revalidate | create another Cycle |
| completion exists; `BetweenCycles` correlation absent | finished Session; live intent; missing role evidence | Existing Study + Capability | reconstruct or remain recoverable | select arbitrary completion |
| Stop publication incomplete | explicit Stop; terminal publication uncertain | Learner + Capability | preserve Stop and resolve publication | continue automatically |
| terminal reason incomplete/conflicting | publication and source evidence do not agree | Capability + reason authority | revalidate/remain recoverable | substitute reason or finality |
| Undo changes preceding completion after next acceptance | latest Undo evidence plus two Cycle relationships | Existing Study + Capability | later policy/recoverable | mutate queue, duplicate Cycle, or ignore Undo |

Durable ambiguity must not be silently resolved, treated as absence, or converted into a
terminal reason. Exact resolution belongs to CD-07 and later implementation design.

## 8. Evidence Lifecycle

This lifecycle describes semantic status, not storage retention or a runtime state machine.

```text
Created
   ↓
Observed
   ↓
Accepted
   ↓
Durable
   ↓
Revalidated
   ↓
Consumed
   ↓
Historical
   ↓
Archived (if a later policy requires it)
```

- **Created:** canonical authority establishes evidence.
- **Observed:** another boundary becomes aware of it; observation transfers no authority.
- **Accepted:** the responsible decision authority accepts it for a specific semantic use.
- **Durable:** required evidence can survive process loss.
- **Revalidated:** canonical authority confirms current semantic fitness.
- **Consumed:** a decision/evidence relationship has produced its permitted outcome, such as one
  accepted next Cycle.
- **Historical:** evidence remains explanatory but cannot authorize a new current action.
- **Archived:** optional later retention classification; applicability is Undecided.

Evidence may become Stale, Unknown, Invalid, or orphaned instead of advancing. “Consumed” does
not mean deleted, and “Historical” does not define a storage lifecycle.

## 9. Canonical Evidence Ownership

Every fact has exactly one canonical owner. A `Reference`, `Projection`, `Derived View`,
`Historical Copy`, or `Correlation` may preserve or expose evidence but never becomes canonical
ownership.

| Authority | Canonical evidence examples | Non-owning uses |
|---|---|---|
| Learner | explicit intent, selected scope, explicit Stop | Capability durability/correlation |
| Existing Planner | eligibility and no-work result | Capability decision input/reference |
| Existing Study | Session acceptance/completion, committed review, queue/exhaustion, Undo checkpoint | Capability references and lifecycle derivations |
| Capability | continuation decision, lifecycle role/outcome, correlation, terminal publication | Desktop projection or persistence representation |
| Current Runtime | demonstrated current failure/observation facts | recovery/capability evidence input |
| Ordinary Recovery | supported ordinary resume/reconciliation evidence | Capability revalidation input |

Persistence owns no fact merely because it stores evidence. Desktop owns no fact merely because
it presents or triggers an interaction.

## 10. Evidence Correlation Model

- **Identity:** distinguishes one semantic thing from another; it does not imply ownership.
- **Reference:** points to canonical evidence without copying its authority.
- **Correlation:** establishes that independently owned evidence participates in the same
  interaction, decision, or boundary.
- **Association:** records a meaningful relationship without asserting dependency or ownership.
- **Ownership:** identifies the sole canonical authority entitled to establish the fact.
- **Dependency:** means one conclusion requires another evidence item to be valid.

For example, a continuation decision may correlate a completed `SessionId`, Planner evidence,
and an accepted next `SessionId`. The correlation is capability-owned; the Session facts remain
Study-owned and the Planner result remains Planner-owned. These are semantic relationships, not
foreign keys, object references, records, or APIs.

## 11. Evidence Consistency

Consistency does not mean atomic persistence.

- **Semantic Consistency:** the evidence set supports a legal CD-04 shape/transition without
  contradicting preserved invariants.
- **Durable Consistency:** after restart, durable evidence either supports the same conclusion or
  explicitly preserves ambiguity.
- **Authoritative Consistency:** every fact agrees with its canonical owner; non-canonical
  representations yield when they conflict.
- **Implementation Consistency:** concrete transaction, ordering, locking, and storage guarantees.
  This is Deferred.

Planner evidence and Study acceptance need not become observable simultaneously. Nevertheless,
the semantic requirement remains: a Planner result alone is not Study acceptance; an accepted
Session must not be duplicated; and ambiguity between them must survive rather than be silently
resolved.

## 12. Evidence Loss Categories

| Loss Category | Severity | Authority Impact | Restart Impact | Allowed Reconstruction | Forbidden Reconstruction |
|---|---|---|---|---|---|
| Lost Source Evidence | Critical when required | canonical fact cannot be proven; authority does not transfer | outcome may be recoverable/unknown | only by canonical authority | from projection, absence, or convenience |
| Lost Derived Evidence | Usually recoverable | none | shape/view may be missing | rederive from complete source evidence | invent missing sources |
| Lost Correlation | High | owners remain intact but relationship is unproven | orphan/ambiguity; block silent continuation | rebuild only from authoritative relationship evidence | join facts by timing or similarity |
| Lost Projection | Low | none | presentation can be rebuilt | reproject from source/derived facts | treat projection as source |
| Lost Presentation | None to durable authority | none | UI may reset; capability facts remain | render again | infer Stop/Leave/intent |
| Lost Runtime State | Variable | runtime authority facts may become unavailable | revalidate through durable sources or remain recoverable | ordinary reconstruction within proven boundaries | fabricate intent, outcome, or transaction success |

Severity is semantic and contextual, not an implementation priority. Loss categories do not
select retention, backup, recovery, or transaction mechanisms.

## 13. Current Persistence Landscape

| Runtime conclusion | Runtime evidence | Test evidence | Classification |
|---|---|---|---|
| `StudySession` is persisted behind `StudySessionRepository`; the persisted application composes a store-backed repository over `JsonStudySessionStore`. | `src/main/kotlin/vn/loi/learning/application/port/StudySessionRepository.kt` — repository contract (lines 8-35); `src/main/kotlin/vn/loi/learning/infrastructure/LearningApplicationFactory.kt` — `createPersisted` composition (lines 142-283); `src/main/kotlin/vn/loi/learning/infrastructure/persistence/repository/StoreBackedStudySessionRepository.kt` — mapping/save/query | `src/test/kotlin/vn/loi/learning/infrastructure/LearningApplicationFactoryPersistedQueueIntegrationTest.kt` — `persisted application context restores active session queue after recreation` | Runtime Fact; Source-backed; Test-backed; High |
| A `StudyQueueSnapshot` is separately persisted by `SessionId`; its ordered identities and `currentIndex` are source data while progress views are derived. | `src/main/kotlin/vn/loi/learning/application/port/StudyQueueRepository.kt` (lines 6-18); `src/main/kotlin/vn/loi/learning/application/session/StudyQueueSnapshot.kt` — data and derived progress (lines 31-160, 301-320); store-backed/JSON queue adapters | `src/test/kotlin/vn/loi/learning/infrastructure/persistence/PersistedStudyQueueLifecycleRestartTest.kt` — `queue position survives restart and finish retains latest undo checkpoint`, `completed queue remains completed after restart` | Runtime Fact; Source-backed; Test-backed; High |
| Pending review intent is durable inside the session before the review commit boundary. | `StudySession.kt` — `pendingReview`, `stageReview` (lines 36, 224-235); `StudySessionRecord.kt` (lines 38-43); `StudySessionRecordMapper.kt` (lines 90-95, 180-188); `ReviewSessionItemUseCase.kt` — stage/save before transaction | `ReviewSessionItemTransactionTest.kt` — `interruption keeps one pending intent that can be resumed exactly once` | Runtime Fact; Source-backed; Test-backed; High |
| Review commit updates `ReviewEvent`, `MemoryState`, session checkpoint/counters, and queue advance inside the existing transaction boundary. | `src/main/kotlin/vn/loi/learning/application/session/ReviewSessionItemUseCase.kt` — `execute`/commit block (lines 34-168); `src/main/kotlin/vn/loi/learning/infrastructure/transaction/JsonFileTransactionRunner.kt` — `runInTransaction` | `ReviewSessionItemTransactionTest.kt` — `review session item runs inside exactly one transaction`; interruption test above | Runtime Fact; Source-backed; Test-backed; High |
| Ordinary recovery returns an incomplete active queue, closes an active session whose queue is missing, and finalizes one whose queue is completed; pending intent replay is bounded by that ordinary queue evidence. | `src/main/kotlin/vn/loi/learning/application/session/RecoverActiveStudySessionUseCase.kt` — `execute`, `closeIncompleteSession`; `src/main/kotlin/vn/loi/learning/application/LearningEngine.kt` — `recoverActiveSession` | `RecoverActiveStudySessionUseCaseTest.kt` — `returns resumable session with queue progress`, `closes active session when persisted queue is missing`, `finalizes active session whose queue already completed` | Runtime Fact; Source-backed; Test-backed; High within audited ordinary cases |
| Ordinary completion persists the finished session and removes its active queue only when no Undo checkpoint must be retained. | `src/main/kotlin/vn/loi/learning/application/session/FinishStudySessionUseCase.kt` — `execute` (lines 21-49); `StudySession.kt` — `finish` (lines 237-258) | `PersistedStudyQueueLifecycleRestartTest.kt` — queue/Undo restart tests; `StudyQueueLifecycleIntegrationTest.kt` — finish behavior | Runtime Fact; Source-backed; Test-backed; High |
| Latest-review-only Undo uses the durable session checkpoint, latest event, MemoryState, and queue, and performs reversal in the existing transaction. | `src/main/kotlin/vn/loi/learning/application/session/UndoLatestSessionReviewUseCase.kt` — `execute` (lines 12-45); `StudySession.kt` — `undoLatestReview` (lines 151-176); `StudySessionRecord.kt` (lines 43, 67-79) | `UndoLatestSessionReviewIntegrationTest.kt` — `undo restores first review memory queue session and is idempotent`, `undo reopens completion after the final review`; `PersistedStudyQueueLifecycleRestartTest.kt` — `latest review remains undoable after restart` | Runtime Fact; Source-backed; Test-backed; High |
| Restart composition recreates repositories from persisted stores and invokes ordinary recovery; it has no Continuous Review record or intent. | `LearningApplicationFactory.kt` — persisted composition; `LearningEngine.kt` — recovery entry points; repository-wide absence of a PLE-032 runtime type/record | `LearningApplicationFactoryPersistedQueueIntegrationTest.kt`; Desktop completion recovery tests | Runtime Fact plus absence finding; Source/Test-backed for ordinary recovery; High |
| Current Desktop manual continuation starts a new ordinary session from durable learning state, but the continuation request itself is transient and not durable capability intent. | `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/StudyFacade.kt` — `continueGeneralStudyAfterCompletion`, `startSession`, completion paths | `desktop/src/test/kotlin/vn/loi/learning/desktop/ui/study/GeneralStudyContinuationIntegrationTest.kt` — `general completion starts a new session from durable memory and next new item`, `general continuation with no candidate returns idle without completion loop` | Runtime Fact; Source-backed; Test-backed; High |

No audited source implements Continuous Review interaction identity, intent, lifecycle shape,
continuation decision, terminal publication, or single-next-Cycle evidence. Existing persistence
therefore proves ordinary Study durability only; it does not prove PLE-032 exists.

## 14. Durable Fact Inventory

| Fact | Source Authority | Must Be Durable? | Reason | Durable As | Confidence | Deferred Question |
|---|---|---:|---|---|---|---|
| Continuous Review interaction identity | Undecided representation; Capability semantics | Required if needed to bind intent/scope/Cycles unambiguously | Prevent evidence from different interactions being joined | Source fact or reference; representation Undecided | Product requirement; Approved Direction | Whether a distinct identity is required |
| Explicit learner intent | Learner | Yes, once accepted | Restart must not fabricate or lose intent | Source fact | Approved Product Direction | Representation/acceptance boundary |
| Selected scope | Learner | Yes while intent is live | Scope must remain stable across Cycles | Source fact/reference to existing identities | Approved Product Direction | Binding representation |
| Scope stability | Capability | Yes semantically | A changed scope would be a different interaction | Derived/revalidated fact | High as derivation | Validation policy |
| Lifecycle shape | Capability | Restart-relevant, not necessarily direct | Must resume without invented state | Derived/reconstructed/revalidated | High as requirement | Whether any shape is stored directly |
| Active Cycle identity | Existing Study | Yes via reference | Prevent duplicate/resume wrong Cycle | Reference to `SessionId` | Source-backed; High | Relationship representation |
| Preceding completed Cycle identity | Existing Study fact; Capability selection | Yes in `BetweenCycles` | Exactly one completion must be authoritative | Reference plus revalidation | Approved Direction | Selection evidence |
| Positive continuation decision | Capability | Yes before side effect can become ambiguous | Avoid lost or repeated next-Cycle creation | Source decision evidence | Approved Direction | Atomicity/mechanism |
| Next-Cycle creation outcome | Existing Study | Yes once accepted | At-most-one protection | Reference to accepted `SessionId`; revalidated | Source-backed boundary; High | Outcome correlation |
| Terminal lifecycle shape | Capability | Yes once published | Prevent silent resurrection | Derived/source outcome evidence | Approved Direction | Representation |
| Terminal reason | Learner/Planner/failure-policy authority; Capability publishes | Yes | Reason must not be fabricated or replaced | Source evidence plus published decision | Approved Direction | Failure policy |
| Stop | Learner | Yes once accepted | Stop must survive restart and block continuation | Source decision evidence | Approved Direction | Acceptance boundary |
| Leave | Learner interaction semantics | No as Stop; only evidence needed to preserve live intent | Navigation absence is not a terminal decision | Usually not durable; live intent remains | Approved Direction | Resume UX |
| No-eligible-work result | Existing Planner | Revalidate fresh for terminal publication; published terminal evidence durable | Eligibility changes; terminal reason needs authoritative evidence | Revalidated source fact + terminal decision | Approved Direction | Freshness rule |
| Recoverable interruption | Current Runtime/Ordinary Recovery evidence; Capability outcome | Restart-relevant | Missing/conflicting evidence cannot become continuation | Source evidence plus derived outcome | Approved Direction | Exact policy |
| Failure evidence | Current boundary | If required to explain/revalidate unresolved outcome | Must not invent terminal failure | Source evidence/reference | Medium; policy Undecided | Taxonomy/retention |
| Single-next-Cycle protection | Capability | Yes across decision/acceptance ambiguity | One decision yields at most one accepted Cycle | Derived from decision + accepted Cycle evidence | Approved Direction | Enforcement mechanism |
| Committed-review boundary | Existing Study | Already durable; do not duplicate | Protect committed progress | Reference/observe/revalidate | Source/Test-backed; High | None at capability level |
| Queue exhaustion | Existing Study | Already durable/derivable; do not duplicate | Completion ordering evidence | Observe/derive from queue | Source/Test-backed; High | Observation correlation |
| Ordinary completion | Existing Study | Already durable; reference | Establish Cycle boundary | Reference/revalidate finished session | Source/Test-backed; High | None at capability level |
| Restart evidence | Current Runtime/Ordinary Recovery | Durable only at source boundaries | Supports reconstruction, never intent | Observe/revalidate | Source/Test-backed within ordinary cases | Future-flow recovery policy |
| Undo boundary | Existing Study | Existing checkpoint durable; capability relation may need durable correlation | Undo can reopen preceding Cycle | Reference/revalidate; no second history | Source/Test-backed + open product decision | Boundary behavior |

All 17 CD-04 legal transitions are durability-relevant as follows:

| Transition group | Durable requirement |
|---|---|
| `NotInContinuousReview` → `CycleInProgress` | intent/scope must be accepted; accepted `SessionId` is authoritative |
| `NotInContinuousReview` → `Terminal(NoEligibleWork)` | fresh Planner evidence plus published reason |
| `NotInContinuousReview` → `RecoverableInterruption` | unresolved evidence; no invented intent/outcome |
| `NotInContinuousReview` → `Terminal(Failure)` | authoritative failure evidence and later-approved policy |
| `CycleInProgress` → `BetweenCycles` | commit barrier, exhaustion, completion, and exactly one completed `SessionId` |
| `CycleInProgress` → `Terminal(Stop)` | explicit Stop plus preserved committed Study facts |
| `CycleInProgress` → `RecoverableInterruption` | interruption evidence and live intent |
| `CycleInProgress` → `Terminal(Failure)` | failure evidence and terminal decision |
| `BetweenCycles` → `CycleInProgress` | durable positive decision and at-most-one accepted next `SessionId` |
| `BetweenCycles` → `Terminal(NoEligibleWork)` | fresh Planner no-work plus terminal publication |
| `BetweenCycles` → `Terminal(Stop)` | explicit durable Stop |
| `BetweenCycles` → `RecoverableInterruption` | ambiguous decision/planning/acceptance evidence |
| `BetweenCycles` → `Terminal(Failure)` | failure evidence and terminal decision |
| `RecoverableInterruption` → `CycleInProgress` | revalidated intent and one resumable/accepted Cycle |
| `RecoverableInterruption` → `BetweenCycles` | revalidated intent and one authoritative completion |
| `RecoverableInterruption` → `Terminal(Stop)` | explicit durable Stop |
| `RecoverableInterruption` → `Terminal(Failure)` | revalidated failure evidence and terminal policy |

## 15. Must-Persist / Must-Not-Duplicate / Must-Not-Persist

### A. Must Persist

- accepted explicit Continuous Review intent and its stable selected scope;
- sufficient semantic identity/correlation to keep evidence within one interaction (exact
  identity representation Undecided);
- reference to the authoritative active or preceding completed `SessionId`;
- a positive continuation decision once repeating it could create a second Cycle;
- the accepted next-Cycle outcome, or durable ambiguity evidence when acceptance is unknown;
- explicit Stop once accepted;
- a published terminal outcome and its authoritative reason evidence;
- unresolved recoverable evidence when its loss would fabricate certainty;
- enough correlation to preserve exactly one completed Cycle and at most one next Cycle;
- capability relationship to the existing latest-review Undo checkpoint where boundary Undo
  could invalidate lifecycle conclusions.

### B. Must Not Duplicate

Capability must reference, observe, derive, or revalidate rather than own copies of:

- committed-review success, `ReviewEvent`, and `MemoryState`;
- session counters, pending review intent, and Undo checkpoint;
- queue order, current index/progress, exhaustion, and fixed-queue identity;
- ordinary `StudySession` status/completion;
- Planner eligibility and planning result.

Planner output may be retained as evidence only without changing Planner authority. A capability
shadow queue, review ledger, completion flag, or MemoryState copy would create conflicting
authority.

### C. Must Not Persist as Capability Authority

- planner-running, transaction-stage, repository-loading, retry-loop, or lock state;
- a queue-cursor shadow copy;
- Desktop screen, route, dialog, focus, or presentation state;
- process existence/restart flags;
- derived UI labels or progress;
- scheduler internals;
- an enum-shaped lifecycle value without its source evidence;
- ordinary Study facts copied solely to make capability decisions convenient.

## 16. Durable Intent Model

1. Intent must become durable no later than the point at which the accepted start may cause the
   first Cycle request. Before that boundary, restart returns `NotInContinuousReview`.
2. Intent ends only through authoritative terminal semantics: explicit Stop, published
   no-eligible-work, or later-approved terminal Failure; process loss and Leave do not end it.
3. Stop must be durable once accepted, because losing it could restart automatic continuation.
4. Leave is not Stop and need not be persisted as a terminal fact. It is absence of an explicit
   terminal decision while durable intent may remain live.
5. A resumable ordinary `StudySession` without durable Continuous Review intent may resume only
   as ordinary Study evidence; it must not automatically resume Continuous Review.
6. A valid terminal outcome ends durable intent for that interaction while preserving history
   needed to explain finality and prevent resurrection.
7. A later explicit learner start is semantically a new interaction. Whether this requires a
   distinct `FlowId` or another correlation form is Undecided.

## 17. Durable Lifecycle Shape Analysis

| Shape | Required durable evidence and authority | Allowed reconstruction | Forbidden inference | Missing-evidence consequence |
|---|---|---|---|---|
| `NotInContinuousReview` | absence of valid live intent/terminal resurrection evidence; Learner/Capability semantics | Derive when no accepted live intent exists | Infer intent from active session, Desktop, or process | Remain outside capability |
| `CycleInProgress` | live Learner intent, stable scope, one Existing Study active/accepted `SessionId` | Reconstruct after Study revalidation | Infer from intent alone or duplicate Cycle | Recoverable/require action; do not create another Cycle |
| `BetweenCycles` | live intent, one Existing Study committed/exhausted/completed `SessionId`, no accepted next Cycle, no terminal | Derive/reconstruct and revalidate Study facts | Infer completion from queue exhaustion alone; select multiple completions | Recoverable/undecided; no continuation until resolved |
| `RecoverableInterruption` | live intent plus incomplete/conflicting/orphaned evidence from Runtime/Recovery | Derive from uncertainty and preserve it | Convert missing evidence into success, Stop, no-work, or Failure | Remain recoverable/ask learner per later policy |
| `Terminal` | published Capability outcome plus authoritative reason: Learner Stop, fresh Planner no-work, or approved Failure evidence | Reconstruct only from complete reason evidence | Fabricate reason from inactivity, Leave, old plan, or process loss | Do not restore Terminal; remain recoverable/undecided |

No lifecycle enum is required to be stored directly. Source evidence takes precedence over a
stored derived label and must be revalidated where freshness or consistency matters.

## 18. Cycle Identity and Authoritative Completion

- Every Cycle already has authoritative Existing Study identity `SessionId`; CD-06 does not
  duplicate it.
- A separate Continuous Review interaction identity is **Undecided**, but some durable
  correlation is **Required** to prevent scope, decisions, and Sessions from different
  interactions being combined.
- Stable selected scope belongs semantically to the interaction, while each ordinary Cycle
  retains its own immutable session scope/policy evidence.
- In `BetweenCycles`, exactly one completed `SessionId` is designated by capability correlation
  and revalidated against Existing Study completion evidence.
- Old-Cycle completion and next-Cycle acceptance require a durable evidence relationship:
  preceding `SessionId`, continuation decision, and accepted next `SessionId` (or ambiguity).
- Existing Study remains sole authority for both Session identities and acceptance/completion.
  Capability owns only their role in the flow.
- ID format, allocation, record shape, and CD-02 model placement remain Undecided.

## 19. Durable Continuation Decision

1. A positive continuation decision must become durable before its execution can have an
   ambiguous outcome; otherwise restart could repeat it.
2. Crash after decision but before planning preserves the pending decision. Restart may
   revalidate intent/scope and continue that decision once under later-approved recovery policy;
   it must not create a second decision.
3. Crash after Planner result but before Study acceptance leaves Planner evidence but no Cycle.
   Planner remains authority; freshness/replanning policy is Undecided. The result alone is not
   an accepted Cycle.
4. Crash after Existing Study accepts a Cycle but before Capability observes it must be resolved
   by finding/revalidating the accepted `SessionId`; retry must not create another Cycle.
5. Negative continuation is represented by authoritative Stop or a supported terminal decision.
   These must survive restart once accepted/published.
6. A decision must not be reconstructed merely from a lifecycle label. It needs its own
   authoritative evidence or a lossless derivation approved later.

## 20. Single Next-Cycle Protection

Semantic invariant: one positive continuation decision yields at most one successfully accepted
next Cycle.

Minimum durable evidence must distinguish:

- **not created:** decision exists and no Study acceptance is evidenced;
- **ambiguous:** planning/acceptance may have occurred but outcome is not safely known;
- **created exactly once:** one accepted next `SessionId` is correlated to the decision;
- **already consumed:** restart/retry observes that accepted Cycle and cannot request another.

The uniqueness requirement belongs to Capability; Cycle acceptance evidence belongs to Existing
Study; eligibility evidence belongs to Existing Planner. Atomicity, correlation representation,
idempotency, locking, and retry enforcement are Deferred Decisions.

## 21. Terminal Outcome Durability

| Reason | Source authority/evidence | Durability and restart | Finality |
|---|---|---|---|
| `Stop` | explicit Learner action; Capability publication | accepted Stop and publication must survive; restart must not continue automatically | Ends this interaction; later explicit start is new |
| `NoEligibleWork` | fresh Existing Planner no-work result; Capability publication | source evidence must be fresh/revalidated and published reason durable | Ends this interaction; old result cannot terminate a new one |
| `Failure` | applicable Runtime/Recovery evidence plus later-approved failure policy | evidence and publication durable only when policy authorizes terminal classification | Finality and restart behavior remain Undecided pending CD-07 |

No terminal reason may be reconstructed from Leave, inactivity, an empty/missing queue alone, a
stale Planner result, process restart, or a generic exception without approved classification.

## 22. Crash Boundary Matrix

| Crash Location | Durable Evidence Before Crash | Authoritative Facts After Restart | Must Not Be Inferred | Required Product Outcome Category | Deferred Mechanism |
|---|---|---|---|---|---|
| 1. Before explicit intent durable | none | no live capability intent | start/Stop | `NotInContinuousReview` | acceptance boundary |
| 2. After intent durable, before first Cycle request | intent/scope | Learner intent exists; no Cycle | accepted Cycle | reconstruct/recover and permit one request | storage/atomicity |
| 3. During first planning | intent/scope; ambiguous request | Planner outcome unknown | eligibility/no-work | recoverable or revalidate | retry policy |
| 4. After eligible plan, before Cycle acceptance | intent + Planner evidence | no Study acceptance yet | active Cycle | recoverable/revalidate | freshness/correlation |
| 5. During Cycle before rating | intent + accepted Session/queue | Existing Study owns active progress | committed review | resume ordinary Cycle | recovery wiring |
| 6. After rating accepted, before committed-review success | pending Study review intent | review not yet proven committed | ReviewEvent/MemoryState success | ordinary pending recovery | existing transaction behavior |
| 7. After commit, before exhaustion observation | committed Study facts/advanced queue | review is committed | lost/duplicate review | reproject/revalidate | observation |
| 8. After exhaustion, before ordinary completion | completed queue + active session | exhaustion, not completion | finished Cycle/BetweenCycles | complete ordinary boundary or recover | ordering |
| 9. After completion, before BetweenCycles durability | finished Session; live intent | one completion exists | terminal/next Cycle | reconstruct `BetweenCycles` or recover | correlation |
| 10. After continuation decision, before next planning | durable decision | decision exists; no plan/acceptance proven | second decision/no-work | continue once or recover | retry |
| 11. After Planner result, before next acceptance | decision + Planner evidence | plan result only | accepted Cycle | revalidate/recover | freshness |
| 12. After next acceptance, before observation | decision + accepted `SessionId` | exactly one next Cycle exists | no Cycle/second Cycle | reconstruct `CycleInProgress` | lookup/correlation |
| 13. During Stop publication | explicit Stop; publication may be ambiguous | Learner Stop remains authoritative | continuation | terminal or recover until publication resolved | atomicity |
| 14. During no-work publication | fresh Planner no-work; publication ambiguous | no-work evidence exists | Stop/Failure | terminal or recover | publication mechanism |
| 15. During Failure publication | failure evidence; policy/publication ambiguous | evidence only unless classification proven | terminal Failure | recoverable or terminal per CD-07 | failure policy |
| 16. During recoverable interruption publication | unresolved evidence | uncertainty remains | success/terminal certainty | `RecoverableInterruption` | representation |
| 17. During Undo near Cycle boundary | existing Undo checkpoint/event/session/queue plus flow correlation | latest-review-only authority remains Existing Study | multi-step undo or unchanged completion | recoverable/revalidate lifecycle | final Undo policy |

## 23. Restart Reconstruction Rules

| Evidence condition | Can Restore/Derive | Must Revalidate | Required semantic response | Forbidden inference |
|---|---|---|---|---|
| Complete and consistent | matching lifecycle shape | Study/Planner facts where applicable | resume/publish supported outcome | extra intent or Cycle |
| Incomplete | only facts individually proven | missing source evidence | remain recoverable/ask learner per later policy | certainty from absence |
| Conflicting | no disputed shape | all conflicting authorities | no silent continuation | choose convenient record |
| Orphaned active Cycle | ordinary Cycle only | Existing Study + live intent/scope | attach only if correlation is proven; otherwise recover | Continuous intent from Session |
| Orphaned continuous intent | intent/scope only | Cycle/decision evidence | permit at most one supported next action or recover | active Cycle |
| Completed Cycle without continuation evidence | completion | intent and terminal/decision absence | `BetweenCycles` only if one completion correlation is proven | positive decision |
| Accepted next Cycle without observed transition | accepted Cycle | correlation to one decision | reconstruct `CycleInProgress` | create another Cycle |
| Terminal reason without valid source | no valid Terminal | reason authority | recover/undecided | fabricate Stop/no-work/Failure |
| Resumable StudySession without continuous intent | ordinary Study resume | Existing Study only | ordinary Study behavior | Continuous Review resume |

Exact ask/terminate/retry choices remain Undecided and belong to later failure/recovery design.

## 24. Undo and Durable Authority

### Case A — after completed Cycle, before next Cycle creation

Existing durable Undo evidence may reopen the finished Session and rewind its queue, event,
MemoryState, and counters. The previously authoritative completion and derived
`BetweenCycles` shape may therefore become invalid. Capability must revalidate the `SessionId`
before continuation. Committed-review integrity and latest-review-only authority remain with
Existing Study; no second review history is created.

### Case B — after next Cycle is accepted/presented

Undo still targets only the latest committed review identified by Existing Study evidence.
Whether that target may belong to the preceding Cycle, and how an already accepted immutable
next queue is handled, are unresolved. Capability cannot mutate the next queue, accept a second
next Cycle, or pretend the preceding completion remains valid. The safe semantic result may be
ambiguous/recoverable until a later policy selects behavior.

CD-06 does not choose final Undo behavior, compensation, queue deletion, or cross-Cycle
transaction semantics.

## 25. Persistence Authority Map

| Durable Item | Fact Authority | Decision Authority | Durability Requirement Authority | Evidence Provider | Reconstruct? | Must Never Own |
|---|---|---|---|---|---|---|
| live intent/scope | Learner | Learner/Capability lifecycle | Product capability | Learner | only from explicit evidence | Study/Planner facts |
| interaction correlation | Capability semantics | Undecided representation | Product capability | Capability evidence | possibly | Session identity |
| active/completed Cycle reference | Existing Study | Capability selects lifecycle role | Product capability | Existing Study | yes, with revalidation | Session |
| continuation decision | Capability | Capability | Product capability | Capability evidence | not from state label alone | Planner result |
| Planner eligibility/no-work | Existing Planner | Capability uses result | Planner/Product boundary | Existing Planner | revalidate | continuation |
| accepted next Cycle | Existing Study | Existing Study acceptance; Capability outcome | Product capability | Existing Study | yes | decision authority |
| Stop | Learner | Learner | Product capability | Learner action | only from explicit evidence | committed progress |
| terminal publication/reason | Capability + reason authority | Capability/Learner as mapped in CD-05 | Product capability | Learner/Planner/failure evidence | with complete evidence | source reason |
| recoverable evidence | Runtime/Ordinary Recovery | Capability policy | Product capability | Runtime/Recovery | yes/revalidate | intent |
| review/MemoryState/queue/Undo facts | Existing Study | Existing Study | existing runtime | Existing Study repositories | yes | Capability authority |

Persistence, repositories, Desktop, and serialization are evidence carriers only and own none of
these facts.

## 26. Persistence Invariants

1. Persistence does not transfer authority.
2. Durable intent is never inferred from ordinary Study evidence alone.
3. No durable capability record duplicates committed-review ownership.
4. Exactly one completed Cycle is authoritative in `BetweenCycles`.
5. One continuation decision yields at most one accepted next Cycle.
6. Restart fabricates neither intent, Stop, no-work, Failure, completion, nor continuation.
7. Terminal reason requires authoritative source evidence.
8. Stop survives restart once accepted.
9. Leave never becomes Stop through persistence.
10. Missing evidence never becomes certainty.
11. Conflicting evidence never permits silent continuation.
12. Active queue remains owned by Existing Study and is not shadow-mutated.
13. Undo authority remains latest-review-only.
14. No second capability-level review history is persisted.
15. Ordinary Study persistence remains unchanged unless a later ADR explicitly approves change.
16. Future implementation preserves the frozen CD-05 Authority Map.

## 27. Persistence Anti-Patterns

- storing only `isContinuousReview` without intent, scope, and correlation evidence;
- inferring intent from an active `StudySession`;
- storing a duplicated queue cursor;
- copying Planner eligibility as capability-owned truth;
- treating Desktop state as durable intent;
- marking `Terminal` without reason evidence;
- retrying next-Cycle creation without preserving ambiguous outcome;
- overwriting Existing Study completion;
- creating a second review ledger;
- storing derived state while discarding source evidence;
- assuming unapproved persistence atomicity;
- using process existence/restart as lifecycle evidence.

## 28. Deferred Decisions

- CD-02 Option A/B/C and architecture selection;
- interaction identity requirement/representation;
- persistence schema, repository, serialization, and migration;
- transaction membership, atomicity, locking, and idempotency mechanism;
- recovery algorithm, retry policy, and exact learner-prompt behavior;
- failure taxonomy and terminal Failure policy;
- final boundary-Undo behavior or compensation;
- cleanup, retention, and orphan handling mechanism;
- Desktop wiring;
- implementation Aggregate, Coordinator, class, service, API, or record.

## 29. Candidate Persistence Strategies

No strategy is selected or recommended.

### Strategy A — explicit capability interaction evidence

- **Durable:** intent, scope, interaction correlation, lifecycle-relevant decisions/outcomes,
  Cycle references, and terminal evidence.
- **Derived:** lifecycle shape from source evidence.
- **Authority preservation:** possible if all Study/Planner facts remain references.
- **Restart safety:** strongest explicit correlation potential.
- **Duplicate risk:** shadow facts if the representation copies Study/Planner state.
- **Undo:** can correlate invalidated completion, but policy remains open.
- **CD-02 compatibility:** potentially compatible with A/B/C; placement not implied.
- **Strength:** clear auditability.
- **Risk/unknown:** broader consistency/atomicity surface and undecided identity.

### Strategy B — minimal intent/decision evidence with reconstructed shape

- **Durable:** explicit intent/scope, decision correlation, accepted Cycle references, terminal
  reason evidence.
- **Derived:** most lifecycle shapes from Existing Study/Planner evidence.
- **Authority preservation:** naturally favors source revalidation.
- **Restart safety:** safe only if incomplete/conflicting evidence remains recoverable.
- **Duplicate risk:** ambiguous acceptance if correlation is insufficient.
- **Undo:** requires rederivation after Existing Study Undo.
- **CD-02 compatibility:** potentially compatible with A/B/C.
- **Strength:** fewer duplicated facts.
- **Risk/unknown:** reconstruction and freshness policy are undecided.

### Strategy C — Existing Study references plus small continuation evidence

- **Durable:** live intent/scope and minimal pending/consumed continuation correlation; Existing
  Study retains all Cycle facts.
- **Derived:** active/completed Cycle role and lifecycle shape.
- **Authority preservation:** strongest avoidance of Study duplication.
- **Restart safety:** depends on proving which completion/acceptance belongs to the interaction.
- **Duplicate risk:** high if the marker cannot distinguish not-created from ambiguous/created.
- **Undo:** source Undo remains clear, flow interpretation may become ambiguous.
- **CD-02 compatibility:** potentially compatible with A/B/C if no model placement is assumed.
- **Strength:** minimal capability footprint.
- **Risk/unknown:** may lack enough evidence for terminal audit and at-most-one protection.

## 30. Evidence Traceability

| Conclusion | Evidence | Classification | Confidence |
|---|---|---|---|
| Capability boundary and preserved invariants | CD-01 §§3, 5, 7, 10 | Product Decision | Approved Product Direction |
| Option-independent domain analysis and undecided identities | CD-02 candidate models, Identity Ownership, Completion Assessment | Approved analysis; Inference | Approved/High as analysis |
| Lifecycle ordering, commit barrier, Stop/Leave, crash questions | CD-03 §§2-10 | Product Decision plus Source-backed boundaries | Approved/High |
| Five shapes and 17 legal transitions | CD-04 §§2-9 | Product Decision; Derived Facts | Approved Product Direction |
| Authority assignments and no implicit transfer | CD-05 §§3-14 | Approved Authority Map | Approved Product Direction |
| Session fields/pending/Undo/completion are durable domain state | `src/main/kotlin/vn/loi/learning/domain/study/session/model/StudySession.kt` — `StudySession`, `stageReview`, `undoLatestReview`, `finish` (lines 21-90, 151-176, 224-258); `StudySessionRecord.kt`; `StudySessionRecordMapper.kt` | Source-backed | High |
| Queue is fixed persisted navigation state and progress is derived | `StudyQueueSnapshot.kt` (lines 31-160, 301-320); `StudyQueueRepository.kt`; queue store/repository adapters | Source-backed; Test-backed by `PersistedStudyQueueLifecycleRestartTest` | High |
| Pending intent precedes transactional commit | `ReviewSessionItemUseCase.kt` — `execute`, `resumePending`; `ReviewSessionItemTransactionTest.kt` — `interruption keeps one pending intent that can be resumed exactly once`, `review session item runs inside exactly one transaction` | Source-backed; Test-backed | High |
| Ordinary recovery has bounded missing/completed/incomplete-queue behavior | `RecoverActiveStudySessionUseCase.kt` — `execute`; `RecoverActiveStudySessionUseCaseTest.kt` named recovery tests | Source-backed; Test-backed | High within audited cases |
| Completion keeps history and conditionally removes active queue | `FinishStudySessionUseCase.kt` — `execute` (lines 21-49); restart lifecycle tests | Source-backed; Test-backed | High |
| Undo is latest-only and restart-durable | `UndoLatestSessionReviewUseCase.kt` — `execute`; `UndoLatestSessionReviewIntegrationTest.kt`; `PersistedStudyQueueLifecycleRestartTest.kt` — `latest review remains undoable after restart` | Source-backed; Test-backed | High |
| Persisted application recreation restores ordinary session/queue | `LearningApplicationFactory.kt` — `createPersisted`; `LearningApplicationFactoryPersistedQueueIntegrationTest.kt` | Source-backed; Test-backed | High |
| Desktop continuation is manual ordinary orchestration, not durable Continuous Review | `StudyFacade.kt` — `continueGeneralStudyAfterCompletion`; `GeneralStudyContinuationIntegrationTest.kt` named continuation tests | Source-backed; Test-backed; absence finding | High |
| Intent, single-next-Cycle, terminal durability, and crash semantics | CD-01–CD-05 constraints plus CD-06 derivation | Product Decision / Inference, not runtime fact | Approved Direction / Medium-High derivation |
| Evidence graph, canonical matrix, orphan/freshness/ambiguity/lifecycle/correlation/consistency/loss semantics | CD-05 authority invariants plus CD-06 §§3-12 | Product semantic analysis; Inference, not runtime fact | Chief Architect correction incorporated; review required |

Line ranges describe baseline `685203d...` and may move after later source edits.

## 31. Completion Assessment

Durable semantics are sufficiently explicit for Chief Architect review:

- required intent, scope, Cycle correlation, continuation, terminal, and ambiguity evidence are
  separated from Existing Study/Planner facts;
- the evidence dependency graph, one-owner source-of-truth matrix, orphan taxonomy, semantic
  freshness, durable ambiguity, evidence lifecycle, canonical ownership, correlation,
  consistency, and loss categories connect Fact → Evidence → Authority → Durability →
  Correlation → Revalidation without choosing a mechanism;
- all five lifecycle shapes, all 17 legal transitions, all three terminal reasons, all 17
  required crash boundaries, restart reconstruction, single-next-Cycle protection, and both Undo
  timing cases are covered;
- durability never changes CD-05 authority and no ordinary Study persistence change is selected.

Undecided items include interaction identity representation, CD-02 architecture, storage and
atomicity mechanisms, exact recovery/retry/failure policy, and final boundary-Undo behavior.
These are appropriate inputs for CD-07 Failure & Recovery, but CD-07 has not started.

No candidate strategy, Aggregate, Coordinator, repository, API, class, schema, serialization,
transaction model, locking, idempotency mechanism, or recovery algorithm has been selected.

**Draft Complete — Chief Architect Review Required.**
