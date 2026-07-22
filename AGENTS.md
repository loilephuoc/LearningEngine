# Learning Engine 2.0 — Codex Working Agreement

`AGENTS.md` is the sole authority for AI workflow, delivery policy, safety rules, and stopping
conditions in this repository. Other documents describe product state, architecture, roadmap,
history, or short-term context and must refer here instead of repeating these rules.

## Source of Truth

When information conflicts, use this order:

1. Clean Git-tracked source, build files, and tests at the current repository HEAD.
2. This file for the standing AI working agreement.
3. `docs/ARCHITECTURE.md` and `docs/ROADMAP.md` for durable technical boundaries and roadmap.
4. `docs/PROJECT_HANDOFF.md` for concise product and strategic context.
5. `docs/AI_ARCHITECT_CONTEXT.md` for the latest short-term repository snapshot.
6. `docs/MILESTONE_HISTORY.md` and `docs/CHANGELOG.md` for verified history.
7. Chat and external notes, which are never durable project memory.

Never invent an API, constructor, package, class, wiring, abstraction, repository state, test
result, or commit hash. Read the repository evidence that establishes it.

## Working Agreement

- Work on `develop`, never directly on `main`, unless the user explicitly selects another safe
  branch.
- Before each capability, verify branch, HEAD, upstream reference, and working-tree state.
- Preserve unrelated user changes. Stop if they overlap required files and cannot be preserved.
- Read the relevant docs, production code, tests, call sites, and composition roots before
  designing. Follow dependencies until the full affected boundary is understood.
- Make low-risk, evidence-backed implementation decisions autonomously. Do not pause for
  routine choices that source and tests resolve.
- Keep all work inside the repository unless the user explicitly authorizes an external action.
- Report progress, blockers, Git state, and final results to the user in Vietnamese.

## Phase and Capability Workflow

A **Phase** is the primary planning unit. It represents a durable product outcome that may span
many capabilities, commits, and Codex sessions. A **capability** is one coherent, useful,
testable vertical increment inside the current Phase.

At the start of work, identify the current Phase, its Definition of Done, the next incomplete
capability, and the verified continuation point. Do not create a new milestone merely to bound
a session. After committing a capability, update the Phase state, select the next
evidence-backed capability, and continue automatically. A session boundary does not end a
Phase.

Before editing a capability, establish:

```text
Current Phase and Phase outcome
Current capability and its contribution to Phase Definition of Done
Verified baseline
Affected boundaries and composition roots
Compatibility and data-safety risks
Required implementation, tests, and docs
Explicitly out-of-scope work
Proposed commit message
```

Then:

1. Implement through the real consumer/composition boundary.
2. Add focused tests and the necessary regression, integration, restart, or failure coverage.
3. Update only the documents whose owned facts changed.
4. Run the required verification until successful.
5. Run `git diff --check`; inspect the diff, diff stat, and status.
6. Stage only capability files and create one intentional commit.
7. Verify the resulting HEAD and clean worktree, update short-term Phase state when required,
   then continue to the next capability.

Passing tests alone is not completion. Implementation, wiring, compatibility, data integrity,
documentation, Git state, and the requested outcome must all agree.

## Architecture Principles

The Gradle build has a root Kotlin/JVM engine module and a `desktop` Compose module. Preserve:

```text
Desktop / JVM adapters
        ↓
Application use cases and ports
        ↓
Domain models and services

Infrastructure implements application ports.
```

- Domain and application code must not depend on Compose, filesystem APIs, or concrete JSON
  persistence.
- Put business rules in domain/application boundaries, not screens or storage adapters.
- Use explicit composition roots and constructor injection; avoid hidden global state.
- Keep transaction ownership at the application workflow boundary.
- Prefer immutable domain values, deterministic ordering/identifiers/diagnostics, and early
  validation before mutation.
- Reuse an existing contract before creating a parallel representation.
- Do not add disconnected placeholders or speculative abstractions for deferred platforms.
- Desktop is the active client; Android, iOS, and Web must not distort current shared contracts.

## Refactoring Rules

- Refactor only when needed to deliver the current capability or remove a demonstrated defect.
- Preserve public APIs unless a safe implementation is impossible without changing them.
- Keep behavior changes and their compatibility tests in the same capability.
- Do not mix unrelated cleanup, formatting, renames, dependency upgrades, or architecture
  redesign into a capability.
- Do not replace persistence, transaction, import, scheduling, or composition boundaries
  without repository evidence and an explicit product/architecture decision.
- Comments explain invariants and trade-offs, not syntax. Preserve UTF-8 and do not introduce
  mojibake.

## Backward Compatibility and Safety Rules

Persisted data, record schemas, package formats, public APIs, diagnostics, and recovery behavior
are product contracts.

- Preserve JSON legacy arrays, schema envelopes, OPD3/legacy packages, stable diagnostic codes,
  and established messages unless an approved migration or rejection policy is delivered.
- Never silently discard, rewrite, repair, migrate, or partially persist incompatible data.
- Validate untrusted input before transaction/mutation when possible.
- Keep related writes inside established transaction membership and preserve original failures.
- A failed workflow must not leave partial state; restart behavior must remain deterministic.
- Add structured context instead of requiring consumers to parse human-readable messages.
- Never expose secrets or persisted user content in diagnostics, test output, or reports.
- Do not run destructive Git/filesystem operations or rewrite history without explicit authority.
- Never push, publish, deploy, release, or contact external services unless explicitly requested.

## Testing and Build Policy

Every behavior change needs focused coverage at the narrowest useful boundary plus regression
coverage appropriate to its risk. Cover applicable success, boundaries, malformed input,
failure-before-persistence, rollback, restart, compatibility, deterministic ordering,
diagnostics, Desktop state/keyboard/accessibility, and real composition wiring.

- Do not delete, skip, weaken, disable, or make tests less meaningful to obtain a green build.
- Prefer deterministic fixtures and algorithmic assertions over timing. Use wall-clock limits
  only with stable measured justification.
- For any source, build, configuration, resource, or test change, run:

```powershell
.\gradlew.bat clean test
```

- On failure, diagnose and fix the cause, then rerun `clean test` until `BUILD SUCCESSFUL`.
- A Markdown-only documentation increment does not require Gradle unless it changes documented
  test evidence or the user requests it. It always requires `git diff --check`.
- When reporting an exact test count, calculate it from generated test-result XML, not console
  inference.

## Documentation Update Policy

Documents have exclusive responsibilities:

- `AGENTS.md`: standing workflow, delivery, safety, Git, test, and decision rules.
- `docs/PROJECT_HANDOFF.md`: durable product/architecture/roadmap summary and technical debt.
- `docs/AI_ARCHITECT_CONTEXT.md`: current short-term Git, Phase/capability, test, and risk snapshot.
- `docs/ROADMAP.md`: Phase intent, Definition of Done, status, delivered scope, and capability
  sequence.
- `docs/ARCHITECTURE.md`: durable technical boundaries and decisions.
- `docs/CHANGELOG.md`: detailed verified capability/batch history.
- `docs/MILESTONE_HISTORY.md`: immutable legacy milestone history and completed-Phase records.
- `docs/CAPABILITY_MAP.md`: source neighborhood or dependency-boundary changes.
- `docs/TEST_MATRIX.md`: changes to minimum verification coverage.
- `docs/BATCH_PLANNING.md`: compatibility pointer to this authority for older links.

Review `ARCHITECTURE`, `CHANGELOG`, `PROJECT_HANDOFF`, and `ROADMAP` for every product
capability, but edit a file only when its owned facts changed. Update `PROJECT_HANDOFF` for a
strategic, architecture, roadmap, or Phase change. Update `AI_ARCHITECT_CONTEXT` at each clean
session handoff, when a Phase ends, or when work must stop mid-Phase. Update
`MILESTONE_HISTORY` only when a Phase is completed or its historical Git evidence is corrected.
Never copy workflow rules into docs;
link to this file. Record only Git- or test-verified facts.

## Commit and Git Policy

- One completed capability per commit; a Phase normally contains multiple independently
  buildable capability commits across one or more sessions.
- Documentation-only workflow or handoff work may use one dedicated docs commit.
- Use concise imperative messages such as `feat: ...`, `fix: ...`, `test: ...`, or `docs: ...`.
- Stage only intended files. Confirm `git diff --check`, diff/stat, and status before commit.
- Do not amend, squash, reset, force-push, rewrite history, or absorb unrelated changes unless
  the user explicitly requests it and data/history safety is established.
- Never push without explicit authorization. A local commit is not permission to push.

## Product Decision Rules

Ask the user only when repository evidence cannot safely resolve:

- materially different user-visible product behavior;
- deletion, migration, replacement, quarantine, backup, or restoration of user data;
- backward-incompatible schema, package, API, or diagnostic behavior;
- a large architectural trade-off such as a new database, journal, framework, or platform;
- conflicting unrelated user changes;
- external secrets, services, publishing, release, or push authority;
- genuinely undefined Phase scope where choosing changes product direction.

State the evidence and trade-off when asking. Do not ask for routine naming, internal design,
test structure, or other reversible choices established by surrounding conventions.

## Stop Conditions

Continue autonomously through failures and subsequent requested capabilities. Stop only when:

- the current Phase is genuinely complete according to its Definition of Done;
- an explicitly requested, bounded non-product maintenance task is complete and does not
  authorize starting or continuing product capabilities;
- required implementation, wiring, tests, docs, validation, commits, and Git state are complete;
- an allowed decision condition above blocks safe progress;
- an unrecoverable environment failure prevents verification.

Do not stop because work is large, context is long, a test failed, or investigation is needed.
Completing one capability is not a stop condition while the Phase remains incomplete. Commit
each verified capability and continue without asking. If a session must stop for an allowed
condition, leave a clean committed continuation point and record the current Phase, completed
capability, next capability, evidence, and blocker in `AI_ARCHITECT_CONTEXT.md`.

## Reporting Format

Final reports are in Vietnamese and include, as applicable:

- outcome and completed capabilities;
- commit hash for each capability;
- important files/contracts changed;
- focused and full test evidence, including exact counts when requested;
- `git status`, branch, HEAD, and whether push occurred;
- compatibility behavior, known limitations, and reason for stopping;
- current Phase completion status and the next evidence-backed capability or Phase.

Keep reports concise and factual. Do not claim completion from uncommitted or unverified state.
