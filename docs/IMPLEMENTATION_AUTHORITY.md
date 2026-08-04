# Implementation Authority

## Document Status

This is a short execution guide to the current implementation boundary. It is not a replacement
for Git-tracked source, tests, or the detailed authority documents.

- Last verified branch: `develop`
- Last verified commit: `f085d7d84a3f80f7fc0faf4c5e3c5623afe2fd76`
- Final authority: repository source, build files, tests, and their current Git state
- If HEAD has advanced, reverify the relevant source and tests before executing work. Do not treat
  the commit above as a permanent baseline.

## Current Architecture

```text
Content
  -> Content Recall Capability Resolver
  -> Adaptive Recall Strategy
  -> Recall Plan Factory
  -> RecallPlan
  -> Platform Renderer
  -> RecallSubmission
  -> Recall Execution Engine
  -> RecallResult
  -> Recall Learning Execution Bridge
  -> authoritative review transaction
  -> ReviewEvent / Evidence / LearningTrajectory / Scheduler / FSRS / Undo / Queue
```

Shared Core decides supported capabilities, selects and constructs plans, evaluates submissions,
and routes eligible results into learning execution. Platforms render plans, collect input, submit
typed values, display results, and own platform interaction concerns.

## Single Authoritative Commit Path

- There is no second review transaction.
- Desktop Typing and the Shared Recall pipeline converge on the existing authoritative application
  transaction.
- Scheduler, FSRS, Evidence, Undo, and Queue are reached only through existing application
  authorities.
- `PRACTICE_ONLY` execution must not mutate ReviewEvent, MemoryState, Scheduler/FSRS, Evidence, or
  evaluative queue state.
- Duplicate attempts must remain idempotent.
- A platform-provided correctness flag is never learning authority.

## Platform Responsibilities

Desktop, Android, iOS, and Web may:

- render `RecallPlan`;
- play media through a platform adapter;
- collect typed or selected input;
- create the matching `RecallSubmission`;
- call the Shared execution API;
- render `RecallResult`;
- own navigation, keyboard interaction, focus, and accessibility.

Platforms must not:

- select a recall mode;
- construct prompts or generate distractors;
- normalize or evaluate an answer;
- trust UI `isCorrect` state;
- map a result to a learning rating;
- create `ReviewEvent`;
- call Scheduler or FSRS directly;
- append Evidence;
- mutate the study queue;
- advance a session independently of application authority.

## Completed Capabilities

- LQ-005A-E: Learning Intelligence and evidence-based promotion boundaries
- LQ-006A: Cross-Platform Recall Contract
- LQ-006B: Content Recall Capability Resolver
- LQ-006C: Adaptive Recall Strategy
- LQ-006D: Recall Plan Factory
- LQ-006E: Deterministic Multiple Choice Generator
- LQ-006F: Recall Execution Engine
- LQ-006G: Recall-to-Learning Integration
- LQ-007A: Desktop Typing Recall Pipeline
- LQ-007B: Graduated In-Session Reinforcement Spacing
- LQ-007C: Desktop Multiple Choice Runtime
- LQ-007C.1: Production Recall Mode Activation
- LQ-007D: Desktop Listening Recall Runtime
- LQ-007E: Desktop Image Recall Runtime
- LQ-007F: Desktop Example Completion Recall Runtime
- LQ-007X: Desktop Recall Runtime Conformance
- UX-009 through UX-012 and focused answer/review refinements

Production Study resolves Content capability and adaptive strategy in Shared Application, then
constructs Typing, Multiple Choice, Listening, Image Recall, or Example Completion through
`RecallPlanFactory`. Ordered failures remain typed;
Desktop receives and routes only the resolved plan and owns no mode selector.
Typing direction authority is `TARGET_TO_SOURCE` (target cue to English/source response), while
Reverse Translation remains `SOURCE_TO_TARGET`. Desktop projection rejects mismatched plan and
presentation sides before an attempt can submit.

All five production Desktop runtimes follow the interaction lifecycle in
`DESKTOP_RECALL_INTERACTION_CONTRACT.md`; mode-specific reveal presentation remains intact.

## Current Queue Policy

UAT-DESK-002 separates three typed Practice loop policies. Latest-session Practice uses persisted,
session-local adaptive feedback reinforcement (`Again-like: 2 / 6 / 15`, `Hard-like: 4 / 12`);
feedback never changes SRS or Evidence. Again/Hard Practice uses dynamic SRS-backed membership:
explicit manual Good/Easy removes an item from future rounds, Again/Hard retains it, and Undo
restores both rating and membership in the same transaction. Fixed shuffled Practice remains
available for its existing callers.

BUG-003 decouples an issued Typing attempt from later Practice queue and SRS mutations. Desktop
captures eligibility once per active `RecallPlanId` and presented item; submission validates that
issued snapshot instead of recomputing mutable rating/confidence facts. Queue reinforcement,
membership removal, and Undo affect only future navigation. Dynamic Practice progress remains
valid while the current round transitions across a shrinking membership.

Coverage Review reinforcement uses persisted, deterministic per-item state:

| Rating | Gaps | Maximum reinforcements |
| --- | --- | --- |
| Again | `2 / 8 / 20 / 40` | 4 |
| Hard | `4 / 12 / 30` | 3 |
| Good / Easy | none | 0 |

- A reinforcement whose full gap does not fit near the end is deferred, not compressed.
- Practice queues do not use this reinforcement policy.

## Desktop Release Build

- Authoritative release task: `desktop:createReleaseDistributable`
- Release ProGuard configuration: `desktop/compose-desktop.pro`
- Authoritative release launcher verification: `desktop:verifyWindowsReleaseLauncher`; it validates
  the `main-release` image, bundled JVM/accessibility components, packaged resources and MP3 SPI,
  then runs the packaged launcher with an isolated profile.
- End-to-end local qualification: `scripts/verify-windows-release-candidate.ps1`; generated evidence
  is written under `build/release-candidate-evidence/` and is never release authority by itself.
- Do not remove or bypass this configuration without verifying packaged launcher startup, the
  release executable, and audio playback.

## Android Foundation

- `:android` is the second peer client. Its composition root is `AndroidApplicationGraph`, which
  reuses `LearningApplicationFactory.createPersisted`, `ContentMediaStorage`, and package-import
  authority from the root engine module.
- `AndroidStudyFacade` may orchestrate presentation state only. Production planning, direction,
  evaluation, rating, learning transaction, Scheduler/FSRS, Evidence, Practice, and Queue remain
  owned by `LearningEngine` and its existing Application services.
- ANDROID-002 completes the Android Study surface for Typing, Multiple Choice, Listening, Image
  Recall, and Example Completion. Home/session entry delegates Review, latest-session Practice,
  difficult Practice, and learned-item Review to existing Application use cases.
- Android persists only navigation/session identity in `SavedStateHandle`; session, queue, adaptive
  reinforcement, dynamic membership, manual override, Undo, and completion remain engine-owned.
- ANDROID-003 routes package selection, backup, and restore through Storage Access Framework and
  app-private one-shot staging. Application package import remains validation/transaction authority;
  the JVM recovery adapter owns `.lebak` validation, safety snapshot, and rollback.

## Next Capability

ANDROID-003 is complete in automated boundaries. The next Android capability should run physical
phone/tablet UAT for providers, media, process death, rotation, accessibility, and resource pressure.

## Hard Boundaries

Do not:

- change Shared Core semantics only to simplify a UI;
- duplicate Typing or Multiple Choice evaluation;
- add platform-specific rating logic;
- create a parallel review transaction;
- change Scheduler, FSRS, or Evidence policy outside the capability;
- read, edit, stage, delete, or move `docs/capability-design/`;
- persist generated distractors in `Content`;
- use enum ordinals as wire identities;
- use non-injectable randomness or time in core logic;
- put `File`, `Path`, or platform media objects in cross-platform contracts.

## Current Baseline

At this document's creation:

- Branch: `develop`
- HEAD: `f085d7d84a3f80f7fc0faf4c5e3c5623afe2fd76`
- `origin/develop`: `f085d7d84a3f80f7fc0faf4c5e3c5623afe2fd76`
- Expected working tree: no tracked changes; only `docs/capability-design/` untracked and frozen

Every task must verify its actual current baseline. If the repository has advanced, use the newer
verified source state rather than pinning work to this historical commit.

## Verification Expectation

For a product capability:

- run focused tests at the narrowest useful boundary;
- run `.\gradlew.bat clean test --no-daemon --console=plain`;
- require `BUILD SUCCESSFUL`;
- derive any exact test count from generated test-result XML;
- run `git diff --check` and inspect the diff, stat, and status;
- create exactly one intentional local commit for the completed capability;
- do not push unless the user explicitly requests it.

For a Markdown-only documentation increment, follow `AGENTS.md`: Gradle is unnecessary unless the
documentation changes test evidence or a repository docs check requires it. `git diff --check`
remains mandatory.
