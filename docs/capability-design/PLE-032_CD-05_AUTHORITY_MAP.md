# CD-05 Authority Map

## 1. Document Status

- Phase: Capability Design
- Deliverable: CD-05 — Authority Map
- Version: 1.0
- Status: Approved
- Deliverable state: Frozen Capability Design Deliverable
- Repository audit baseline: branch `develop`, commit
  `685203d768d022e05bb763a18229756bcf0d07f0`
- Governing frozen deliverables:
  - `docs/capability-design/PLE-032_CD-01_CONTINUOUS_REVIEW_CAPABILITY_OVERVIEW.md`
  - `docs/capability-design/PLE-032_CD-02_CAPABILITY_DOMAIN_MODEL.md`
  - `docs/capability-design/PLE-032_CD-03_LIFECYCLE_AND_INTERACTION.md`
  - `docs/capability-design/PLE-032_CD-04_CAPABILITY_STATE_MACHINE.md`
- Acceptance Contract state: NOT frozen
- Implementation state: PLE-032 is not implemented

This document assigns decision and evidence authority at capability level. Authority is not
implementation. No mapping in this document selects a layer, Aggregate, Coordinator,
persistence model, repository, API, service, class, transaction, scheduler, planner
implementation, recovery algorithm, or UI.

## 2. Authority Vocabulary

| Term | Definition | Explicit exclusion |
|---|---|---|
| Authority | The recognized right to assert a capability fact or decide a product transition/outcome | Does not identify who or what implements the decision |
| Owner | The authority ultimately accountable for the meaning and validity of a fact, decision, guard, or invariant | Not code ownership, storage ownership, process ownership, or class ownership |
| Observer | A participant that may perceive or present an authoritative fact/outcome without changing its meaning | Observation does not grant decision rights |
| Evidence Provider | An authority that supplies the fact needed by another authority to decide | Supplying evidence does not grant the downstream decision |
| Decision Provider | The authority entitled to choose among product-level outcomes when guards are satisfied | Does not prescribe evaluation code, API, persistence, or orchestration |

An authority may play more than one vocabulary role in different rows. Roles are assigned per
fact or decision, not globally.

## 3. Authority Inventory

| Authority | Capability-level authority | Evidence in frozen documents | Never implies |
|---|---|---|---|
| Learner | Explicitly starts Continuous Review, selects Stop, and supplies learner-selected scope | CD-01 User Outcomes; CD-03 Entry/Stop flows; CD-04 transition authority | Review commit, planner result, recovery evidence, or system terminal classification |
| Capability | Decides whether continuation is considered and publishes capability-level lifecycle/terminal outcomes from authoritative evidence | CD-01 Capability Boundary; CD-03 Decision Points; CD-04 state transitions | Planner eligibility, ordinary review commit, session completion, persistence, or implementation placement |
| Existing Study | Owns ordinary bounded Cycle facts: accepted Cycle, committed review, queue exhaustion, and ordinary completion | CD-01 Preserved Invariants; CD-03 Commit Barrier; CD-04 RF-02–RF-06 | Cross-Cycle continuation decision or terminal reason selection outside ordinary facts |
| Existing Planner | Supplies fresh eligibility/planning result for a requested Cycle and decides how the fixed queue is produced | CD-01 Planner invariant; CD-03 planning boundary; CD-04 PF-03/PF-06 | Whether Continuous Review should continue, Stop, retry, or Terminate |
| Current Runtime | Supplies current observable runtime evidence, including the absence of Continuous Review and existing durable Study facts | CD-02 Runtime Landscape; CD-04 Runtime Facts | Future product intent, future state, architecture, or policy |
| Ordinary Recovery | Supplies supported evidence about whether an ordinary Cycle is resumable for audited cases | CD-03 Resume/Restart flows; CD-04 RF-05 | Continuous-flow intent, retry policy, terminal policy, or reconstruction algorithm |

Desktop is not added as a capability authority. Current Desktop may observe and present facts
and currently triggers ordinary lifecycle interactions, but CD-01 forbids Continuous Review
authority from existing only as transient Desktop state.

## 4. Authority Ownership Matrix

| Authority | May Decide | May Establish or Provide Evidence | May Observe | Must Never Decide |
|---|---|---|---|---|
| Learner | Explicit entry, selected scope, explicit Stop | Learner intent and learner action | Published lifecycle/terminal outcomes and ordinary progress | Eligibility result, committed-review success, queue exhaustion, completion, recovery evidence, or failure classification |
| Capability | Continuation request; capability lifecycle outcome; recoverable versus terminal product outcome under approved policy; terminal publication | Derived capability facts produced from authoritative evidence | Learner intent, Study facts, Planner results, Runtime/Recovery evidence | Learner Stop intent, eligibility result, committed-review success, exhaustion, ordinary completion, or recovery evidence |
| Existing Study | Acceptance of an ordinary bounded Cycle; ordinary committed-review/completion facts within current semantics | Cycle acceptance, committed-review success, queue exhaustion, ordinary completion | Learner intent and capability request as inputs | Cross-Cycle continuation, terminal publication, learner Stop, or eligibility result |
| Existing Planner | How a requested Cycle is planned and its eligibility/planning result | Eligible/no-work result and fixed queue plan evidence | Valid Cycle request and selected scope | Continuation, Stop, retry, recovery, or terminal publication |
| Current Runtime | No future product decision | Current observable runtime/durable facts within audited scope | Capability interaction and existing persisted Study evidence | Continuous intent, continuation, Stop, terminal reason, architecture, or future policy |
| Ordinary Recovery | No Continuous Review product decision | Supported ordinary resume/recovery evidence within audited cases | Runtime session/queue evidence | Continuous intent, retry/terminal policy, eligibility, or reconstructed capability state |

The matrix is capability-level authority only. It neither assigns implementation ownership nor
transfers authority between rows.

## 5. Fact Authority Map

### Runtime Facts

| Fact | Authority | Evidence Provider | Observer |
|---|---|---|---|
| RF-01 — Continuous Review is absent from runtime | Current Runtime | Audited production source and composition | Capability design reviewers |
| RF-02 — Ordinary `StudySession` is bounded | Existing Study | Current session model/tests | Capability; learner-facing presentation |
| RF-03 — Cycle queue is finite and exhaustible | Existing Study | Current queue model/tests | Capability; learner-facing presentation |
| RF-04 — Queue exhaustion and ordinary completion are distinct | Existing Study | Queue and finish boundaries/tests | Capability; Current Runtime |
| RF-05 — Ordinary recovery can identify audited resumable cases | Ordinary Recovery | Current recovery evidence/tests | Capability; Current Runtime |
| RF-06 — Review becomes authoritative only after successful commit boundary | Existing Study | Current review boundary/tests | Capability; learner-facing presentation |

### Product Facts

| Fact | Authority | Evidence Provider | Observer |
|---|---|---|---|
| PF-01 — Explicit learner entry with selected scope | Learner | Learner action and supplied scope | Capability |
| PF-02 — Capability coordinates but does not own bounded Cycles | Capability | CD-01/CD-02 frozen boundary | All capability consumers |
| PF-03 — Commit barrier precedes completion/continuation | Existing Study for commit/completion facts; Capability for ordered continuation | Existing Study | Capability; learner-facing presentation |
| PF-04 — Completed Cycle may continue or Terminate | Capability | Existing Study completion plus continuation evidence | Learner |
| PF-05 — Stop ends intent; Leave does not imply Stop | Learner for Stop; Capability for distinction | Learner action and frozen lifecycle | Capability; learner-facing presentation |
| PF-06 — No eligible work produces terminal outcome | Existing Planner for no-work fact; Capability for outcome | Existing Planner | Learner |
| PF-07 — Recoverable versus terminal failure outcome | Capability; exact policy Undecided | Current Runtime/Ordinary Recovery/Existing Study as applicable | Learner |
| PF-08 — At most one next Cycle per continuation decision | Capability | Existing Study acceptance evidence | Learner; Current Runtime |
| PF-09 — No committed review disappears or duplicates | Existing Study for committed review; Capability across continuation | Existing Study and Current Runtime | Learner; capability observers |
| PF-10 — Ordinary Study remains unchanged | Existing Study | Current runtime behavior and frozen invariants | Capability; learner |

### Derived Facts

| Fact | Authority | Evidence Provider | Observer |
|---|---|---|---|
| DF-01 — Not in Continuous Review before explicit entry | Capability derives; Learner controls entry | Absence of explicit learner entry | Learner; capability observers |
| DF-02 — Capability is within a Cycle | Capability derives | Learner intent plus Existing Study Cycle evidence | Learner |
| DF-03 — Capability is between Cycles | Capability derives | One Existing Study completion plus no next/terminal outcome | Learner |
| DF-04 — Terminal shape has one distinct reason | Capability derives/publishes; Learner owns Stop reason; Planner supplies no-work fact | Learner, Existing Planner, or failure evidence | Learner |
| DF-05 — Recoverable interruption differs from Terminal | Capability derives; exact policy Undecided | Existing Study, Current Runtime, or Ordinary Recovery evidence | Learner |

Derived-fact authority means authority to apply frozen derivation, not authority to invent source
facts.

## 6. State Authority Map

| Lifecycle Shape | Entry Authority | Exit Authority | Invariant Authority |
|---|---|---|---|
| `NotInContinuousReview` | Capability derives from absent explicit intent | Learner initiates entry; Existing Study or Capability evidence determines accepted Cycle/terminal outcome | Capability preserves non-assumption of intent; Existing Study preserves ordinary behavior |
| `CycleInProgress` | Existing Study supplies accepted/resumable Cycle; Capability recognizes shape | Existing Study supplies completion/interruption facts; Learner may Stop; Capability publishes recoverable/terminal outcome | Existing Study owns bounded Cycle invariants; Capability preserves cross-Cycle constraints |
| `BetweenCycles` | Existing Study supplies commit/exhaustion/completion facts; Capability recognizes boundary | Capability decides continuation/outcome; Learner may Stop; Existing Planner supplies no-work fact | Capability preserves exactly one authoritative completed Cycle and single next-Cycle rule |
| `RecoverableInterruption` | Capability publishes recoverable outcome from authoritative evidence | Capability accepts supported continuation or terminal outcome; Learner may Stop; Ordinary Recovery supplies resume evidence | Capability prevents progress while unresolved and preserves committed work; Existing Study remains authority for committed reviews |
| `Terminal` | Learner owns `Stop`; Existing Planner supplies `NoEligibleWork`; Capability owns publication and `Failure` classification | No exit within the same interaction | Capability preserves one explicit reason and no continuation; Existing Study preserves committed progress |

State authority does not imply that capability state is stored or implemented by the named
authority.

## 7. Transition Authority Map

| From → To | Trigger Authority | Decision Authority | Guard Authority | Evidence Provider |
|---|---|---|---|---|
| `NotInContinuousReview` → `CycleInProgress` | Learner | Existing Study accepts Cycle; Capability recognizes shape | Learner for intent; Existing Study for Cycle availability | Learner + Existing Study |
| `NotInContinuousReview` → `Terminal(NoEligibleWork)` | Existing Planner result | Capability publishes terminal outcome | Existing Planner | Existing Planner |
| `NotInContinuousReview` → `RecoverableInterruption` | Failure evidence | Capability; exact policy Undecided | Capability applies recoverable-outcome guard | Current Runtime/Existing Study |
| `NotInContinuousReview` → `Terminal(Failure)` | Failure evidence | Capability; exact policy Undecided | Capability applies terminal-failure guard | Current Runtime/Existing Study |
| `CycleInProgress` → `BetweenCycles` | Existing Study completion facts | Capability recognizes lifecycle boundary | Existing Study owns commit barrier and ordinary completion guards | Existing Study |
| `CycleInProgress` → `Terminal(Stop)` | Learner | Learner | Learner explicit-Stop guard; Existing Study supplies committed-progress fact | Learner + Existing Study |
| `CycleInProgress` → `RecoverableInterruption` | Failure evidence | Capability; exact policy Undecided | Capability applies recoverable-outcome guard | Existing Study/Current Runtime |
| `CycleInProgress` → `Terminal(Failure)` | Failure evidence | Capability; exact policy Undecided | Capability applies terminal-failure guard | Existing Study/Current Runtime |
| `BetweenCycles` → `CycleInProgress` | Capability continuation decision | Capability decides continuation; Existing Study accepts one Cycle | Capability owns positive-continuation/single-next-Cycle guards | Existing Study + Existing Planner |
| `BetweenCycles` → `Terminal(NoEligibleWork)` | Existing Planner result | Capability publishes terminal outcome | Existing Planner owns no-work fact | Existing Planner |
| `BetweenCycles` → `Terminal(Stop)` | Learner | Learner | Learner explicit-Stop guard | Learner |
| `BetweenCycles` → `RecoverableInterruption` | Failure evidence | Capability; exact policy Undecided | Capability applies recoverable-outcome/single-Cycle guards | Current Runtime/Existing Study |
| `BetweenCycles` → `Terminal(Failure)` | Failure evidence | Capability; exact policy Undecided | Capability applies terminal-failure guard | Current Runtime/Existing Study |
| `RecoverableInterruption` → `CycleInProgress` | Supported resume evidence | Capability accepts continuation | Ordinary Recovery owns resume evidence; Capability prevents fabricated intent | Ordinary Recovery + Existing Study |
| `RecoverableInterruption` → `BetweenCycles` | Supported post-completion evidence | Capability accepts continuation point | Ordinary Recovery supplies evidence; Capability preserves one completed Cycle | Ordinary Recovery + Existing Study |
| `RecoverableInterruption` → `Terminal(Stop)` | Learner | Learner | Learner explicit-Stop guard | Learner |
| `RecoverableInterruption` → `Terminal(Failure)` | Failure evidence | Capability; exact policy Undecided | Capability applies terminal-failure guard | Current Runtime/Ordinary Recovery |

## 8. Composite Decision Pattern

A composite decision preserves separate authority roles. Combining evidence and decisions does
not create a new authority.

| Scenario | Trigger Authority | Evidence Provider | Decision Authority | Boundary Acceptor / Confirmation Authority | Published Outcome |
|---|---|---|---|---|---|
| A. First Cycle creation | Learner supplies explicit intent/scope | Existing Planner supplies fresh eligibility/plan evidence | Capability permits lifecycle outcome | Existing Study accepts the ordinary bounded Cycle | `CycleInProgress`, or another evidence-supported outcome |
| B. `BetweenCycles` → `CycleInProgress` | Existing Study proves one preceding completion | Existing Planner supplies fresh eligibility/plan evidence | Capability makes positive continuation decision | Existing Study accepts exactly one next Cycle | `CycleInProgress` |
| C. No-work termination | Existing Cycle request or positive continuation decision | Existing Planner supplies no-eligible-work evidence | Capability decides/publishes terminal outcome | Capability confirms required evidence is present | `Terminal(NoEligibleWork)` |
| D. Stop | Learner supplies explicit Stop decision | Learner action is the required evidence | Learner owns Stop; Capability prevents future automatic continuation | Capability confirms terminal publication | `Terminal(Stop)` |
| E. Recovery continuation | Current Runtime / Ordinary Recovery supplies supported evidence | Current Runtime / Ordinary Recovery | Capability decides whether supported continuation is permitted | Existing Study resumes or accepts a bounded Cycle where applicable | `CycleInProgress`, `BetweenCycles`, recoverable outcome, or evidence-supported terminal outcome |

These are authority decompositions, not call sequences. They define no API, transaction,
algorithm, class, service, Aggregate, or Coordinator, and create no additional authority
category.

## 9. Terminal Outcome Authority

| Terminal reason | Fact authority | Decision authority | Evidence provider | Authority boundary |
|---|---|---|---|---|
| `Stop` | Learner | Learner ends continuous intent; Capability publishes outcome | Explicit learner action | No authority may infer Stop from Leave, restart, inactivity, or failure |
| `NoEligibleWork` | Existing Planner owns the no-work result | Capability publishes terminal outcome | Fresh requested planning result | Capability cannot fabricate eligibility; Planner cannot decide continuation |
| `Failure` | Current boundary supplies failure evidence | Capability classifies/publishes non-continuable outcome; exact policy Undecided | Existing Study, Current Runtime, or Ordinary Recovery | Evidence provider cannot silently select product policy; Capability cannot rewrite committed facts |

`Terminal` is the lifecycle shape. The three rows are reasons and do not become independent
lifecycle-state authorities.

## 10. Invariant Authority

| Invariant | Preservation authority | Evidence authority | Never delegated to |
|---|---|---|---|
| Commit barrier precedes Cycle transition | Existing Study for successful commit; Capability for continuation ordering | Existing Study | Learner, Planner, Desktop observation |
| No committed review disappears or duplicates | Existing Study for committed review; Capability across transitions | Existing Study + Current Runtime | Planner or learner intent |
| Active queue remains fixed | Existing Study | Existing Study | Capability continuation decision |
| Exactly one completed Cycle is authoritative in `BetweenCycles` | Capability | Existing Study completion evidence | Planner or Desktop reprojection |
| At most one successful next Cycle per decision | Capability | Existing Study acceptance evidence | Planner, retry, restart, or observer |
| No-work produces explicit terminal outcome/no loop | Existing Planner for result; Capability for outcome/no-loop invariant | Existing Planner | Learner or Current Runtime inference |
| Stop never rolls back committed progress | Learner for Stop decision; Existing Study for committed progress; Capability for separation | Learner + Existing Study | Desktop navigation |
| Leave does not imply Stop/Terminal | Learner for Stop absence; Capability for semantic distinction | Learner interaction evidence | Desktop route/visibility |
| Restart does not fabricate intent | Capability | Current Runtime/Ordinary Recovery | Process existence or Desktop restoration |
| No new committed review during unresolved interruption | Capability blocks capability progression; Existing Study remains commit authority | Current Runtime/Ordinary Recovery for resolution evidence | Observer or retry attempt |
| Terminal has exactly one explicit reason | Capability, with Learner/Planner fact authority where applicable | Learner, Existing Planner, or failure evidence | UI wording or persistence representation |
| Ordinary Study remains unchanged | Existing Study | Current runtime/tests | Capability flow decisions |
| State machine remains Option A/B/C-independent | Capability | Frozen CD-02/CD-04 | Any implementation choice |

## 11. Authority Conflict and Precedence

### Precedence Principles

1. Explicit learner Stop takes precedence over an automatic continuation request that has not
   yet been performed. Eligible work cannot override Stop.
2. Existing Study facts for committed review, exhaustion, and ordinary completion cannot be
   overwritten by Capability, Desktop, or another observer.
3. Existing Planner result is authoritative for eligibility. Capability decides only the
   lifecycle/terminal outcome based on that result.
4. Ordinary Recovery and Current Runtime evidence never establish Continuous Review intent by
   themselves.
5. Terminal publication requires the corresponding evidence: `Stop` from Learner,
   `NoEligibleWork` from Existing Planner, and `Failure` from later-approved failure
   authority/policy supported by applicable evidence.
6. When evidence conflicts or is insufficient, no authority fabricates certainty. The outcome
   remains recoverable/undecided or requires learner action according to later-approved policy.

These principles define semantic precedence, not an algorithm or technical priority number.

### No Implicit Authority Transfer

- Observation does not transfer authority.
- Orchestration does not transfer authority.
- Persistence does not transfer authority.
- Presentation does not transfer authority.
- Reusing a result does not transfer ownership of its fact.
- A composite decision does not merge participants into a new authority.
- Capability use of Planner no-work evidence does not make Capability eligibility authority.
- Capability observation of Study commit/completion does not grant commit/completion authority.
- Capability use of Recovery evidence does not make Capability owner of that evidence.

### Authority Conflict Examples

| Scenario | Authorities Involved | Authoritative Fact or Decision | Required Outcome | Forbidden Outcome |
|---|---|---|---|---|
| Learner Stops while eligible work exists | Learner, Existing Planner, Capability | Learner owns Stop; Planner result remains true but cannot override Stop | `Terminal(Stop)`; no future automatic continuation | Continue because work exists |
| Planner reports no work while Capability would otherwise continue | Existing Planner, Capability | Planner owns no-work result | Capability publishes `Terminal(NoEligibleWork)` | Capability fabricates eligible work |
| Restart finds resumable session but no durable Continuous Review intent | Current Runtime, Ordinary Recovery, Capability | Recovery evidence covers ordinary Cycle only | Do not infer continuous intent; remain recoverable/require action per later policy | Automatic Continuous Review resume based only on session evidence |
| Desktop observes completion repeatedly | Existing Study, Capability, Desktop observer | Existing Study owns one completion; Capability owns single-next-Cycle invariant | At most one next Cycle for the decision | Duplicate next Cycles |
| Retry occurs after next Cycle was already created | Capability, Existing Study, Current Runtime | Existing Study acceptance proves next Cycle already exists | Preserve the one accepted Cycle | Create another Cycle |
| Recoverable evidence exists without terminal-failure policy | Ordinary Recovery/Current Runtime, Capability | Evidence supports recoverability; terminal policy is absent | Keep recoverable/undecided outcome or require action | Invent `Terminal(Failure)` |
| UI interprets Leave as Stop | Learner, Capability, Desktop observer | Only Learner supplies explicit Stop | Preserve Leave/Stop distinction | Publish `Terminal(Stop)` |

## 12. Authority Boundaries

1. Learner authority never asserts planner eligibility, commits a review, supplies recovery
   evidence, or classifies a system failure.
2. Capability authority never fabricates learner Stop, planning results, committed-review
   success, queue exhaustion, ordinary completion, or recovery evidence.
3. Existing Study authority never decides whether Continuous Review should request another
   Cycle or select a terminal reason outside its ordinary facts.
4. Existing Planner authority never decides continuation, Stop, retry, recovery, or terminal
   publication.
5. Current Runtime authority never invents future product intent, state, or policy.
6. Ordinary Recovery authority never infers Continuous Review intent or selects retry/terminal
   policy.
7. Observers never acquire decision authority by presenting or reprojecting a fact.
8. No authority may overwrite another authority's established fact to make a transition legal.
9. Authority assignments never imply Aggregate, Coordinator, layer, persistence, repository,
   API, service, class, transaction, or UI placement.

## 13. Anti-Authority Rules

- Planner does not decide continuation.
- Capability does not create or alter a planning result.
- Learner does not commit a review or assert commit success.
- Capability does not commit reviews or redefine Existing Study semantics.
- Existing Study does not infer Continuous Review intent.
- Ordinary Recovery does not recreate continuous intent without decided authority.
- Current Runtime does not turn process restart into a resume decision.
- Leave/navigation does not exercise Stop authority.
- Desktop observation, presentation, or reprojection does not own capability state.
- Retry does not grant authority to create a second next Cycle.
- Capability does not turn a failure into `NoEligibleWork`.
- Planner does not turn no-work evidence into a terminal outcome by itself.
- No authority creates a second Undo authority or multi-step history.
- No authority bypasses the commit barrier, fixed queue, or ordinary completion boundary.

## 14. Authority Invariants

1. Learner is the sole authority for explicit Stop.
2. Existing Planner is authority for eligibility/planning result.
3. Existing Study is authority for committed-review success, queue exhaustion, ordinary
   `StudySession` completion, and ordinary bounded Cycle acceptance.
4. Capability is authority for whether continuation is requested, capability-level lifecycle
   outcome, and terminal publication, but never fabricates supporting facts.
5. Current Runtime and Ordinary Recovery provide evidence only within demonstrated scope and do
   not establish continuous intent.
6. Desktop or any observer does not own capability state or decision authority by displaying or
   triggering an interaction.
7. Authority assignments do not change between first and later Cycles.
8. No authority bypasses the committed-review barrier.
9. No authority creates duplicate next Cycles for one continuation decision.
10. No authority creates a second Undo authority or changes latest-review-only semantics.
11. No authority converts a recoverable outcome into another terminal reason without required
    evidence.
12. Any later implementation placement must preserve this Authority Map.
13. Authority is never transferred implicitly by observation, orchestration, persistence,
    presentation, result reuse, or composite decision.

## 15. Deferred Decisions

CD-05 does not decide:

- selection among CD-02 Options A/B/C;
- implementation placement for any authority;
- Aggregate, Domain service, Coordinator, layer, class, or API design;
- persistence or identity for intent, state, scope, authority, or decisions;
- repository contracts or evidence storage;
- transaction, crash, or atomicity boundaries;
- retry and terminal-failure policy;
- recovery algorithm or state reconstruction;
- mechanism enforcing one next Cycle;
- boundary-Undo behavior;
- scheduler/planner implementation;
- UI observation or presentation design;
- concrete implementation placement while preserving this frozen Authority Map.

## 16. Evidence Traceability

| Authority claim | Evidence | Classification | Confidence |
|---|---|---|---|
| Learner owns explicit entry and Stop | CD-01 User Outcomes; CD-03 Entry/Stop flows; CD-04 transitions | Product Decision | Approved Product Direction |
| Existing Study owns bounded Cycle, commit, exhaustion, and completion facts | CD-01 Preserved Invariants; CD-03 Commit Barrier; CD-04 Runtime Facts | Runtime Fact; Source-backed; Test-backed | High |
| Existing Planner owns eligibility/planning result but not continuation | CD-01 Planner invariant; CD-03 Boundary Crossings; CD-04 transition guards | Runtime Fact plus Product Decision boundary | High / Approved Direction |
| Capability owns continuation and capability-outcome decisions without fabricating evidence | CD-01 Capability Boundary; CD-03 Decision Points; CD-04 legal transitions | Product Decision | Approved Product Direction |
| Ordinary Recovery supplies audited resume evidence but does not own continuous intent | CD-02 recovery analysis; CD-03 Resume/Restart; CD-04 RF-05/Non-Facts | Runtime Fact plus Product Decision boundary | High within audited cases |
| Current Runtime supplies facts, not future intent or policy | CD-02 Runtime Landscape; CD-04 Fact Classification | Runtime Fact; Derived authority boundary | High |
| Terminal shape and reasons have separate authority | CD-04 Fact Classification, State Definition, Terminal Outcome Reasons | Product Decision; Derived Fact | Approved Product Direction |
| Desktop is not capability authority | CD-01 Non-Desktop-only invariant; CD-02 Current Layer Responsibilities | Product Decision; Runtime authority nuance | Approved Product Direction |
| Authority does not imply implementation placement | CD-02 Completion Assessment; CD-05 objective | Product Decision for design scope | Approved design constraint |
| Authority is not transferred through observation, orchestration, persistence, presentation, reuse, or composite decision | CD-01/CD-02 ownership boundaries; CD-05 Authority Vocabulary and frozen review direction | Product Decision for authority semantics | Approved design constraint |
| Composite decisions preserve distinct trigger, evidence, decision, and acceptance authorities | CD-03 Boundary Crossings; CD-04 Legal Transitions; CD-05 Composite Decision Pattern | Derived authority analysis | High as derivation |
| Explicit Stop precedes unperformed automatic continuation | CD-03 Stop/Cycle Transition; CD-04 illegal transitions | Product Decision | Approved Product Direction |
| Insufficient or conflicting evidence cannot be converted into certainty | CD-03 Failure/Restart flows; CD-04 recoverable/terminal distinction | Product Decision; exact policy Undecided | Approved direction |

## 17. Completion Assessment

**Assessment: CD-05 Approved and frozen.**

The map assigns fact, state, transition, guard, invariant, and terminal-reason authority using
only Learner, Capability, Existing Study, Existing Planner, Current Runtime, and Ordinary
Recovery. It separates authority, evidence, observation, and decision roles while explicitly
preventing authority from being interpreted as implementation placement.

The Authority Map remains compatible with CD-02 Options A/B/C and does not choose architecture,
persistence, APIs, classes, transactions, recovery mechanics, scheduler behavior, planner
behavior, or UI.

CD-05 v1.0 is an Approved, Frozen Capability Design Deliverable. The Acceptance Contract remains
NOT frozen, PLE-032 remains unimplemented, and CD-06 has not started.
