# Repository Entry

## Start Here

Read only what the current task needs, in this order:

1. [`IMPLEMENTATION_AUTHORITY.md`](IMPLEMENTATION_AUTHORITY.md)
2. The direct authority document for the capability
3. The narrow source package involved
4. Focused tests for that package
5. Broader documentation only when a real ambiguity remains

Do not scan the entire repository before beginning work. `AGENTS.md` remains the standing workflow,
delivery, safety, testing, and Git authority.

## Task Routing

| Area | Read first | Source boundary |
| --- | --- | --- |
| Recall contracts | `RECALL_CONTRACT.md` | `src/main/kotlin/vn/loi/learning/domain/study/recall/` |
| Capability resolution | `CONTENT_RECALL_CAPABILITIES.md` | `src/main/kotlin/vn/loi/learning/application/recall/ContentRecallCapabilityResolver.kt` |
| Adaptive strategy | `ADAPTIVE_RECALL_STRATEGY.md` | `application/recall/AdaptiveRecallStrategy.kt` and `domain/study/recall/` strategy types |
| Plan and prompts | `RECALL_PLAN_FACTORY.md` | `src/main/kotlin/vn/loi/learning/application/recall/` |
| Multiple Choice generation | `MULTIPLE_CHOICE_GENERATOR.md` | `src/main/kotlin/vn/loi/learning/application/recall/` |
| Recall execution | `RECALL_EXECUTION_ENGINE.md` | `application/recall/RecallExecutionEngine.kt` and evaluator registry |
| Learning integration | `RECALL_LEARNING_INTEGRATION.md` | `application/recall/RecallLearningExecutionBridge.kt` and its direct use cases |
| Desktop Study UI | `DESKTOP_RECALL_PIPELINE.md` | `desktop/src/main/kotlin/vn/loi/learning/desktop/ui/study/` |
| Queue and session | `COVERAGE_REINFORCEMENT_SPACING.md` | `application/session/StudyQueueSnapshot.kt`, queue service, and direct persistence adapters |
| Android | Current module/build source, if an Android module exists at that future baseline | Do not invent or bootstrap a module without an approved capability |

Package names above are verified against source at the documented baseline. Follow direct callers,
callees, composition roots, and focused tests when a task crosses a boundary.

## Read Minimally

For a UI task:

- read the relevant Desktop Study composition and presentation tests;
- do not audit Scheduler, FSRS, or Evidence unless the call boundary is affected.

For a Shared Core task:

- read the Domain/Application contract, implementation, direct composition, and focused tests;
- do not read Desktop Compose beyond the necessary consumer or boundary test.

For a queue task:

- read `StudyQueueSnapshot`, queue service, persistence mapping, transaction caller, and focused
  restart/Undo tests;
- do not change UI unless the capability explicitly requires a projection change.

For a documentation task:

- read only the documents whose owned facts are relevant;
- do not change production source.

For a small bug with a demonstrated root cause:

- change the precise file and function;
- run the focused test first;
- do not open an unrelated capability or refactor.

## Baseline Check

Run from the repository root:

```powershell
git status
git log -1 --oneline
git rev-parse HEAD
git rev-parse origin/develop
```

- Unexpected tracked changes mean STOP before editing.
- `docs/capability-design/` may remain untracked and frozen; do not read or touch it.
- Preserve user changes. Do not reset or restore them.
- Do not amend, squash, reset, or rewrite history unless explicitly authorized.

## Execution-Only Contract

- Do not send a long plan for routine work.
- Do not ask questions that source and tests already answer.
- Do not stop after investigation when implementation is authorized.
- Implement through the real consumer boundary, test, verify, commit, and report.
- Stop only for a genuine blocker allowed by `AGENTS.md`.
- Never push, publish, or contact external services without explicit authority.

## Search Strategy

- Start with `git grep` for a specific type, API, test, or diagnostic.
- Open direct callers and callees, then the composition root.
- Read focused tests beside the affected package.
- Avoid repository-wide searches using broad generic terms.
- Do not guess filenames or package names.
- UI strings often originate in localization/resources; search the property name or caller before
  searching display text.

## Document Map

- `AGENTS.md` - standing workflow, safety, testing, Git, delivery, and stopping rules
- `IMPLEMENTATION_AUTHORITY.md` - current execution architecture, boundaries, baseline, and next work
- `ARCHITECTURE.md` - durable module boundaries and architecture decisions
- `ROADMAP.md` - Phase intent, Definition of Done, status, and capability sequence
- `PROJECT_HANDOFF.md` - durable product, architecture, roadmap, and technical-debt summary
- `AI_ARCHITECT_CONTEXT.md` - latest short-term Git, capability, verification, and risk snapshot
- `CAPABILITY_MAP.md` - source neighborhoods and dependency boundaries changed by capabilities
- `RECALL_CONTRACT.md` - cross-platform RecallPlan, submission, result, and wire contracts
- `CONTENT_RECALL_CAPABILITIES.md` - Content-owned recall capability projection
- `ADAPTIVE_RECALL_STRATEGY.md` - deterministic recall mode and direction selection
- `RECALL_PLAN_FACTORY.md` - validated plan and prompt construction
- `MULTIPLE_CHOICE_GENERATOR.md` - deterministic option and distractor generation
- `RECALL_EXECUTION_ENGINE.md` - pure plan/submission evaluation boundary
- `RECALL_LEARNING_INTEGRATION.md` - result classification and authoritative learning routing
- `DESKTOP_RECALL_PIPELINE.md` - Desktop consumer wiring for the Shared Recall pipeline
- `COVERAGE_REINFORCEMENT_SPACING.md` - Coverage Review reinsertion policy and persisted state
- `TEST_MATRIX.md` - minimum verification coverage by capability
- `CHANGELOG.md` - detailed, verified capability history

## Current Next Step

See [`IMPLEMENTATION_AUTHORITY.md`](IMPLEMENTATION_AUTHORITY.md#next-capability).

Current planned capability: **LQ-007C - Desktop Multiple Choice Runtime**.
