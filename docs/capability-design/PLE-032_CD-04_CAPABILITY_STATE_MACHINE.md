# CD-04 Capability State Machine

## 1. Document Status

- Phase: Capability Design
- Deliverable: CD-04 — Capability State Machine
- Version: 1.0
- Status: Approved
- Deliverable state: Frozen Capability Design Deliverable
- Repository audit baseline: branch `develop`, commit
  `685203d768d022e05bb763a18229756bcf0d07f0`
- Governing frozen deliverables:
  - `docs/capability-design/PLE-032_CD-01_CONTINUOUS_REVIEW_CAPABILITY_OVERVIEW.md`
  - `docs/capability-design/PLE-032_CD-02_CAPABILITY_DOMAIN_MODEL.md`
  - `docs/capability-design/PLE-032_CD-03_LIFECYCLE_AND_INTERACTION.md`
- Acceptance Contract state: NOT frozen
- Implementation state: PLE-032 is not implemented

This frozen deliverable derives product-observable capability states from frozen facts and lifecycle
evidence. It does not select CD-02 Option A/B/C or design an Aggregate, Coordinator,
persistence, repository, API, class, transaction, scheduler, planner, recovery algorithm, or UI.

## 2. Observable Capability Facts

### Runtime Facts

| Fact ID | Observable fact | Classification | Evidence |
|---|---|---|---|
| RF-01 | Continuous Review does not exist in production runtime | Runtime Fact; Source-backed | CD-01 Capability Definition; CD-02 Current Runtime Domain Landscape |
| RF-02 | An ordinary `StudySession` is bounded and has distinct active/finished lifecycle facts | Runtime Fact; Source-backed; Test-backed | `StudySession.kt`; `StudySessionLifecycleTest.kt` |
| RF-03 | A Cycle queue has finite navigation and observable exhaustion | Runtime Fact; Source-backed; Test-backed | `StudyQueueSnapshot.kt`; `StudyQueueSnapshotTest.kt` |
| RF-04 | Queue exhaustion and ordinary `StudySession` completion are separate facts | Runtime Fact; Source-backed; Test-backed | `StudyQueueSnapshot.kt`; `FinishStudySessionUseCase.kt`; `StudyQueueLifecycleIntegrationTest.kt` |
| RF-05 | Current ordinary recovery can identify a resumable incomplete Cycle for the audited cases | Runtime Fact; Source-backed; Test-backed | `RecoverActiveStudySessionUseCase.kt`; `RecoverActiveStudySessionUseCaseTest.kt` |
| RF-06 | Ordinary committed review becomes authoritative only through the existing successful review boundary | Runtime Fact; Source-backed; Test-backed | `ReviewSessionItemUseCase.kt`; `ReviewSessionItemTransactionTest.kt` |

### Product Facts

| Fact ID | Observable fact | Classification | Evidence |
|---|---|---|---|
| PF-01 | The learner knowingly and explicitly starts Continuous Review for a selected scope | Product Decision | CD-01 User Outcomes; CD-03 Main Success Flow |
| PF-02 | Continuous Review coordinates ordinary bounded Cycles without owning `StudySession` | Product Decision | CD-01 Capability Definition; CD-02 Architectural Constraints |
| PF-03 | A successful committed-review boundary precedes exhaustion observation, ordinary completion, continuation evaluation, and next-Cycle request | Product Decision; Lifecycle requirement | CD-03 Main Success Flow and Cycle Transition |
| PF-04 | After ordinary completion, the capability may continue with another eligible Cycle or Terminate explicitly | Product Decision | CD-01 Capability Definition; CD-03 Cycle Transition |
| PF-05 | Learner Stop ends continuous intent, while Leave does not automatically end it | Product Decision | CD-03 Lifecycle Overview, Stop Flow, Leave and Resume Flow |
| PF-06 | No eligible work produces an explicit terminal outcome and no empty-cycle loop | Product Decision | CD-01 Success Criteria; CD-03 Initial No-Work Flow |
| PF-07 | A failure produces a safe recoverable status when continuation remains possible, or an explicit terminal outcome when it does not | Product Decision; exact policy Undecided | CD-01 User Outcomes; CD-03 Failure Flow |
| PF-08 | A completed Cycle creates at most one successful next Cycle for the same continuation decision | Product Decision; Lifecycle requirement | CD-03 Cycle Transition and Lifecycle Invariants |
| PF-09 | No committed review disappears or is duplicated during continuation, Stop, Leave, restart, or recoverable failure | Product Decision | CD-01 Success Criteria; CD-03 Lifecycle Invariants |
| PF-10 | Ordinary Study remains unchanged | Product Decision | CD-01 Preserved Architectural Invariants; CD-03 Lifecycle Invariants |

### Derived Facts

| Fact ID | Derived fact | Derivation | Classification |
|---|---|---|---|
| DF-01 | Before explicit entry, the learner is not in Continuous Review | Absence of PF-01 for the capability interaction | Derived Fact |
| DF-02 | When continuous intent exists and an ordinary bounded Cycle is available for participation, the capability is product-observably within a Cycle | PF-01 + PF-02 + bounded Cycle evidence RF-02/RF-03 | Derived Fact |
| DF-03 | After ordinary completion and before a terminal outcome or successful next Cycle, the capability is product-observably between Cycles | RF-04 + PF-03 + PF-04 | Derived Fact |
| DF-04 | Stop, no eligible work, and non-continuable failure all produce the same terminal lifecycle shape but retain distinct observable reasons | PF-05 + PF-06 + PF-07 | Derived Fact |
| DF-05 | A failure for which supported continuation remains possible is distinguishable from the terminal lifecycle shape | PF-07 | Derived Fact; exact policy Undecided |

## 3. Fact Classification

### Runtime Facts

RF-01–RF-06 describe only current production evidence. They do not claim that Continuous Review
or its state machine exists at runtime.

### Product Facts

PF-01–PF-10 are approved product direction inherited from CD-01 and CD-03. They constrain the
state machine but are not current runtime behavior.

### Derived Facts

DF-01–DF-05 are logical consequences of the classified facts and frozen lifecycle. A derived
fact is retained only when its source facts and lifecycle evidence are explicit.

Terminal taxonomy is two-dimensional:

- **Lifecycle Shape:** `Terminal`.
- **Terminal Outcome Reason:** `Stop`, `NoEligibleWork`, or `Failure`.

Outcome reasons explain why the capability ended. They are not lifecycle shapes or independent
states.

## 4. Non-Facts

The following are not Capability Facts and must not become capability states:

- planner execution, planner request dispatch, or candidate iteration;
- transaction execution stage, commit phase, rollback phase, or pending database work;
- repository load/save state or filesystem activity;
- queue cursor movement, item index, or current queue-entry position;
- scheduler execution or FSRS calculation;
- Desktop composition, recomposition, navigation route, dialog, or screen visibility;
- `StudySession` internal active/finished state used as a substitute for the higher-level
  capability state;
- rating-button interaction before the existing committed-review boundary succeeds;
- process restart by itself;
- Leave by itself, because Leave does not determine whether continuous intent remains durably
  authoritative;
- boundary Undo timing by itself;
- a hypothetical Flow identity, Scope identity, Aggregate, Coordinator, persistence record, or
  retry counter.

These may be lifecycle evidence, implementation details, or deferred concepts. None is
sufficient to define a product-observable capability state.

## 5. Impossible Facts

The following capability facts must never coexist or be asserted:

1. A completed Cycle without the successful committed-review barrier for its final review.
2. Queue exhaustion caused by a final rating whose committed-review boundary did not succeed.
3. A continuation decision without one authoritative completed Cycle.
4. A next-Cycle planning interaction without a first-Cycle request or positive continuation
   decision.
5. More than one authoritative completed Cycle waiting for the same continuation decision.
6. More than one successful next Cycle for one continuation decision.
7. A terminal outcome and an active/between-Cycles lifecycle shape at the same time.
8. `Terminal Outcome Reason = Stop` without explicit learner Stop.
9. `Terminal Outcome Reason = NoEligibleWork` without a fresh no-eligible-work result.
10. A new committed review while a `RecoverableInterruption` remains unresolved.
11. Continuous-flow intent inferred solely from process restart while durable authority remains
    undecided.
12. A committed review that disappears or appears twice because of continuation, retry,
    reprojection, restart, or Undo.

These are impossible capability facts, not states, transitions, implementation checks, or
storage rules.

## 6. State Derivation

| Proposed state | Derived From Facts | Lifecycle Evidence | Classification |
|---|---|---|---|
| `NotInContinuousReview` | DF-01 | CD-03 explicit-entry boundary | Derived capability state |
| `CycleInProgress` | DF-02, RF-02, RF-03, PF-02 | CD-03 Main Success Flow Steps 2–6 | Derived capability state |
| `BetweenCycles` | DF-03, RF-04, PF-03, PF-04 | CD-03 Main Success Flow Steps 7–10; Cycle Transition | Derived capability state |
| `RecoverableInterruption` | DF-05, PF-07 | CD-03 Failure Flow; Resume and Restart flows | Product-derived lifecycle shape; recovery policy Undecided |
| `Terminal` | DF-04, PF-05, PF-06, PF-07 | CD-03 Stop, Initial No-Work, Cycle Transition, and Failure flows | Product-derived lifecycle shape |

No separate `Starting`, `Planning`, `Committing`, `Completing`, `Restarting`, `Leaving`,
`Retrying`, or `Undoing` state is retained because the frozen facts do not establish those as
product-observable capability conditions. `Stop`, `NoEligibleWork`, and `Failure` are retained
only as terminal outcome reasons, not lifecycle states.

## 7. State Definitions

### NotInContinuousReview

- **Meaning:** The learner has not established Continuous Review intent for this interaction.
- **Entry Condition:** Initial product context before explicit Continuous Review entry.
- **Exit Condition:** The learner explicitly requests Continuous Review with a selected scope.
- **Observable Exit Event:** A first Cycle is accepted, or a terminal outcome is published.

### CycleInProgress

- **Meaning:** Continuous Review intent exists and the learner has an ordinary bounded Cycle
  available for participation.
- **Entry Condition:** The first or next bounded Cycle has been successfully created, or a
  previously incomplete ordinary Cycle is validly resumed.
- **Exit Condition:** The learner Stops, a failure interrupts safe participation, or the
  successful committed-review barrier is followed by queue exhaustion and ordinary completion.
- **Observable Exit Event:** Cycle completion is acknowledged, learner Stop is acknowledged, or
  a recoverable/terminal failure outcome is published.

### BetweenCycles

- **Meaning:** The preceding Cycle completed ordinarily, no terminal outcome has been declared,
  and no next Cycle has yet been successfully created.
- **Entry Condition:** Successful final committed review, exhaustion observation, and ordinary
  Cycle completion have all occurred in the frozen lifecycle order.
- **Exit Condition:** One next Cycle is successfully created, learner Stop occurs, no eligible
  work is established, or a recoverable/non-continuable failure outcome is established.
- **Observable Exit Event:** Next Cycle accepted, recoverable interruption published, or
  terminal outcome published.

### RecoverableInterruption

- **Meaning:** Continuous Review cannot currently proceed, but the product has not established
  a terminal outcome and supported continuation may remain possible.
- **Entry Condition:** A failure or recovery interaction yields a safe recoverable outcome.
- **Exit Condition:** Supported evidence permits Cycle participation or continuation to resume,
  the learner Stops, or later-approved policy establishes a terminal failure outcome.
- **Observable Exit Event:** Resume accepted, continuation restored, learner Stop acknowledged,
  or terminal failure outcome published.

### Terminal

- **Meaning:** The Continuous Review interaction ended with exactly one explicit terminal
  outcome reason: `Stop`, `NoEligibleWork`, or `Failure`.
- **Entry Condition:** Learner Stop is accepted, fresh planning establishes no eligible work, or
  later-approved policy classifies a failure as non-continuable.
- **Exit Condition:** None within the same Continuous Review interaction.
- **Observable Exit Event:** None within the same interaction. A later learner start begins a
  new interaction rather than exiting this terminal state.

Terminal outcome reason constraints:

| Reason | Meaning | Required observable fact |
|---|---|---|
| `Stop` | Learner explicitly ended Continuous Review intent | Explicit learner Stop accepted |
| `NoEligibleWork` | Selected scope yielded no eligible work for the requested Cycle | Fresh no-eligible-work result |
| `Failure` | Continuation is unavailable under later-approved failure policy | Explicit non-continuable failure outcome |

## 8. Legal Transitions

| From | To | Trigger | Guard | Authority | Lifecycle Evidence |
|---|---|---|---|---|---|
| `NotInContinuousReview` | `CycleInProgress` | Explicit entry yields a successfully created first Cycle | Explicit intent/scope; eligible fixed Cycle available | Learner initiates; Existing Study accepts Cycle | CD-03 Main Success Flow |
| `NotInContinuousReview` | `Terminal` (`NoEligibleWork`) | First-Cycle planning reports no eligible work | No Cycle fabricated; no retry loop | Existing Planner supplies fact; Capability publishes outcome | CD-03 Initial No-Work Flow |
| `NotInContinuousReview` | `RecoverableInterruption` | Entry or first-Cycle failure is recoverable | No progress lost; no terminal outcome | Capability classifies observable outcome; exact policy Undecided | CD-03 Failure Flow |
| `NotInContinuousReview` | `Terminal` (`Failure`) | Entry or first-Cycle failure is non-continuable | Explicit terminal failure outcome | Capability; exact policy Undecided | CD-03 Failure Flow |
| `CycleInProgress` | `BetweenCycles` | Final review completes the bounded Cycle | Commit succeeds before exhaustion and completion | Existing Study owns commit/completion facts; Capability recognizes boundary | CD-03 Commit Barrier and Cycle Transition |
| `CycleInProgress` | `Terminal` (`Stop`) | Learner explicitly Stops | Committed learning retained | Learner | CD-03 Stop Flow |
| `CycleInProgress` | `RecoverableInterruption` | Participation is safely interrupted | No terminal outcome; progress preserved | Capability; exact failure policy Undecided | CD-03 Failure Flow |
| `CycleInProgress` | `Terminal` (`Failure`) | Failure is non-continuable | Explicit outcome; progress preserved | Capability; exact policy Undecided | CD-03 Failure Flow |
| `BetweenCycles` | `CycleInProgress` | Positive continuation produces the next Cycle | Exactly one authoritative completed Cycle; at most one successful next Cycle | Capability decides continuation; Existing Study accepts Cycle | CD-03 Cycle Transition |
| `BetweenCycles` | `Terminal` (`NoEligibleWork`) | Fresh planning reports no eligible work | No empty Cycle or retry loop | Existing Planner supplies fact; Capability publishes outcome | CD-03 Cycle Transition |
| `BetweenCycles` | `Terminal` (`Stop`) | Learner explicitly Stops | No next Cycle requested after Stop | Learner | CD-03 Stop Flow |
| `BetweenCycles` | `RecoverableInterruption` | Continuation is interrupted but may recover | Completed Cycle durable; no duplicate next Cycle | Capability; exact policy Undecided | CD-03 Failure Flow |
| `BetweenCycles` | `Terminal` (`Failure`) | Continuation failure is non-continuable | Explicit terminal outcome | Capability; exact policy Undecided | CD-03 Failure Flow |
| `RecoverableInterruption` | `CycleInProgress` | Supported recovery yields a resumable Cycle | Evidence sufficient; interruption resolved; intent not fabricated | Current Runtime supplies evidence; Capability accepts continuation | CD-03 Resume and Restart Flows |
| `RecoverableInterruption` | `BetweenCycles` | Supported recovery restores post-completion continuation | Exactly one completed Cycle authoritative; no duplicate next Cycle | Current Runtime supplies evidence; Capability accepts continuation | CD-03 Restart and Cycle Transition |
| `RecoverableInterruption` | `Terminal` (`Stop`) | Learner explicitly Stops | Committed progress intact | Learner | CD-03 Stop and Failure Flows |
| `RecoverableInterruption` | `Terminal` (`Failure`) | Policy establishes non-continuable failure | Explicit terminal outcome | Capability; exact policy Undecided | CD-03 Failure Flow |

The last four transitions depend on recovery/failure policy that remains **Undecided**. They are
legal capability possibilities, not a recovery algorithm.

## 9. Illegal Transitions

| Transition | Why Illegal | Broken Invariant |
|---|---|---|
| `NotInContinuousReview` → `BetweenCycles` | No bounded Cycle has completed | Explicit entry and ordered Cycle lifecycle |
| `NotInContinuousReview` → `Terminal` (`Stop`) without explicit Stop | Stop cannot be inferred | Stop is learner-explicit |
| `CycleInProgress` → `BetweenCycles` before successful committed review | Transition crosses the commit barrier too early | No transition before successful committed-review boundary |
| `CycleInProgress` → `CycleInProgress` by appending or replacing its queue | A Cycle cannot be extended into another Cycle | Fixed queue per Cycle |
| `BetweenCycles` → multiple `CycleInProgress` outcomes for one decision | Duplicates next-Cycle creation | At most one successful next Cycle per continuation decision |
| `BetweenCycles` → `CycleInProgress` by silently replanning an existing queue | New work requires a new bounded Cycle | No append/replace/extend/silent replan |
| Any state → `Terminal` (`NoEligibleWork`) without a fresh no-work result | Terminal reason would be fabricated | Explicit no-eligible-work evidence |
| Any state → `Terminal` (`Stop`) because the learner Leaves | Leave and Stop are distinct | Leave does not imply Stop or completion |
| Any state → `CycleInProgress` solely because the process restarted | Intent would be fabricated | Restart does not infer undecided durable intent |
| `Terminal` → `BetweenCycles` within the same interaction | Terminal outcome would be silently reversed | Terminal is final for the interaction |
| `Terminal` (`NoEligibleWork`) → `CycleInProgress` within the same interaction | Learning after terminal outcome requires a new explicit interaction | No-work termination is final for the interaction |
| `Terminal` → any nonterminal state within the same interaction | Terminal outcome would be silently reversed | Explicit terminal outcome semantics |
| Any transition that drops or duplicates a committed review | Learning history would be corrupted | Committed-review preservation |
| Any transition that creates a second Undo authority or multi-step history | Existing Undo boundary would be replaced | Latest-review-only Undo |

## 10. Transition Guards

| Guard | Product condition |
|---|---|
| Explicit Entry | Learner knowingly requests Continuous Review and supplies a supported selected scope |
| Cycle Available | One ordinary bounded Cycle has been successfully created with a fixed queue |
| Commit Barrier Passed | The existing committed-review boundary succeeded before exhaustion or continuation is considered |
| Ordinary Cycle Completed | Queue exhaustion was observed and ordinary `StudySession` completion occurred as separate ordered facts |
| Positive Continuation | No Stop or terminal outcome prevents another Cycle request |
| Single Next Cycle | The same continuation decision has not already produced a successful next Cycle |
| No Eligible Work | Fresh planning for the selected scope reports no eligible work |
| Explicit Stop | Learner explicitly ends continuous intent |
| Recoverable Outcome | Failure/recovery evidence permits possible continuation and no terminal outcome is established |
| Terminal Failure | Later-approved policy establishes that continuation is unavailable and exposes an explicit outcome |
| Resume Evidence | Existing ordinary session/queue evidence supports safe resume without fabricating intent or work |
| Committed Progress Preserved | No committed review is lost, duplicated, or rolled back by the transition |

These guards describe product conditions only. Their evaluation mechanism is not decided.

## 11. State Invariants

| State | What must always remain true |
|---|---|
| `NotInContinuousReview` | No Continuous Review intent is assumed; ordinary Study remains unchanged |
| `CycleInProgress` | Exactly one ordinary bounded Cycle is being used; its policy and queue remain fixed; review semantics remain ordinary |
| `BetweenCycles` | Exactly one completed Cycle is authoritative; no second completed Cycle simultaneously awaits continuation; no next Cycle has yet been successfully created; committed progress remains durable |
| `RecoverableInterruption` | No terminal outcome is asserted; committed progress is preserved; no new committed review occurs until the interruption is resolved; recovery does not fabricate intent, work, or reviews |
| `Terminal` | Exactly one reason (`Stop`, `NoEligibleWork`, or `Failure`) is explicit; no automatic continuation occurs; committed progress remains preserved |

Across every state:

- queue exhaustion, ordinary completion, and capability termination remain distinct facts;
- no committed review disappears or is duplicated;
- no active queue is appended, replaced, extended, or silently replanned;
- latest-review-only Undo and ordinary Study semantics remain unchanged;
- the state machine does not depend on CD-02 Option A/B/C.

## 12. Deferred Decisions

CD-04 does not decide:

- selection among CD-02 Options A/B/C;
- Aggregate, Domain service, Coordinator, layer, class, or API placement;
- persistence or identity for continuous intent, current capability state, or selected scope;
- how state is reconstructed after restart;
- exact Stop effect on an in-progress ordinary Cycle;
- retry classification, retry limits, or terminal-failure policy;
- recovery algorithm or compensation;
- transaction, crash, or atomicity boundaries;
- mechanism enforcing single next-Cycle creation;
- final behavior for either boundary-Undo timing case;
- scheduler, planner, queue, or ordinary Study changes;
- UI representation, wording, or navigation;
- implementation representation of lifecycle shape and terminal outcome reason.

## 13. Evidence Traceability

| State-machine claim | Evidence | Classification | Confidence |
|---|---|---|---|
| Continuous Review is explicitly entered and coordinates bounded Cycles | CD-01 Capability Definition, User Outcomes, Initial Product Scope | Product Decision | Approved Product Direction |
| Runtime has bounded session and finite queue facts but no Continuous Review state | CD-01 Evidence Traceability; CD-02 Runtime Domain Landscape | Runtime Fact; Source-backed; Test-backed | High |
| `CycleInProgress` derives from active continuous intent plus an available ordinary bounded Cycle | PF-01, PF-02, RF-02, RF-03; CD-03 Main Success Flow | Derived Fact; Product Decision | High as derivation |
| `BetweenCycles` derives from commit barrier, exhaustion, ordinary completion, and pending continuation outcome | PF-03, PF-04, RF-04; CD-03 Cycle Transition | Derived Fact; Product Decision | High as derivation |
| `Terminal` is a lifecycle shape while `Stop`, `NoEligibleWork`, and `Failure` are distinct reasons | PF-05–PF-07; CD-03 Stop, No-Work, and Failure flows | Product Decision; Derived Fact | Approved Product Direction |
| `Stop` is distinct from Leave and cannot be inferred from it | PF-05; CD-03 Stop and Leave flows | Product Decision; Derived Fact | Approved Product Direction |
| `NoEligibleWork` is an explicit terminal reason requiring fresh no-work evidence | PF-06; CD-03 Initial No-Work Flow | Product Decision; Derived Fact | Approved Product Direction |
| Recoverable and terminal failure outcomes are product-distinct, while exact policy is undecided | PF-07; CD-03 Failure Flow | Product Decision; Derived Fact; Undecided policy | Approved direction; policy not assigned |
| Restart, Leave, Undo timing, planner execution, transaction stage, and queue cursor are not capability states | CD-03 Lifecycle Invariants and Deferred Decisions; Non-Facts analysis in this document | Derived Fact; Inference | High for exclusion at capability level |
| Legal transition into `BetweenCycles` must cross successful committed review first | CD-03 Commit Barrier and Lifecycle Invariant 1 | Product Decision | Approved Product Direction |
| At most one next Cycle may result from one continuation decision | CD-03 Cycle Transition and Lifecycle Invariant 4 | Product Decision; mechanism Undecided | Approved Product Direction |
| State machine is independent of candidate architecture | CD-02 Completion Assessment; CD-03 Lifecycle Invariant 11 | Product Decision for design scope | Approved design constraint |

## 14. Completion Assessment

**Assessment: CD-04 Approved and frozen.**

Five product-observable lifecycle shapes are retained because each derives from frozen facts and
lifecycle evidence. Terminal outcome reasons are modeled separately from lifecycle shape.
Implementation-observable activity is explicitly excluded. Legal and illegal transitions,
product guards, and state invariants preserve the commit barrier, fixed queue, single next-Cycle
creation, committed-review integrity, explicit terminal outcomes, latest-review-only Undo, and
ordinary Study isolation.

The model remains compatible with CD-02 Options A/B/C and does not decide architecture,
persistence, APIs, implementation, or recovery mechanics.

CD-04 v1.0 is an Approved, Frozen Capability Design Deliverable. The Acceptance Contract remains
NOT frozen, PLE-032 remains unimplemented, and CD-05 has not started.
