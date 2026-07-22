# Batch Planning

This is the operational policy for scalable continuation as the codebase grows.

## Definition of a batch

One batch equals one coherent, testable capability increment.

Typical size:

- 8–15 affected files for a normal capability;
- fewer files for deep persistence or architectural work;
- more files only for a mechanical, low-risk change with a clear shared contract.

File count is not a productivity metric. Prefer the smallest complete vertical slice.

## Planning contract

Before editing, write an internal plan with:

```text
Capability:
Outcome:
Nearest product milestone:
Expected baseline:
Affected boundaries:
Required production files:
Required tests:
Compatibility and data-safety risks:
Out of scope:
Commit message:
```

Do not start from a desired file count and search for files to fill it.

## Source-loading strategy

Read in this order:

1. current handoff;
2. capability map;
3. roadmap and test matrix;
4. complete production neighborhood for the selected capability;
5. complete focused tests;
6. composition root and persistence adapters when wiring or stored contracts are involved;
7. direct dependencies revealed by imports and call sites.

Do not read all historical docs or the entire repository unless the capability genuinely
crosses the whole system.

## Vertical-slice rule

A capability is complete only when all required layers are present:

```text
domain behavior, when needed
→ application use case or query
→ infrastructure/persistence, when needed
→ Desktop or JVM wiring
→ focused regression tests
→ documentation
```

Do not ship disconnected abstractions or a new public contract without its real consumer.

## Safe reasons to split a large capability

Split only at a natural compatibility boundary, for example:

```text
schema and migration
→ application wiring
→ presentation and user workflow
→ release hardening
```

Every intermediate batch must remain buildable, useful, and backward compatible.

Do not split by individual class or by arbitrary token limits when that leaves an incomplete
workflow.

## Baseline and repository safety

Before applying:

- repository must be at the exact expected clean HEAD;
- payload checksums must match;
- every changed existing file must be backed up;
- every newly created file must be tracked for rollback.

After applying:

- run full `clean test`;
- roll back automatically on failure;
- inspect `git status --short`;
- commit and push only after success.

The pushed commit becomes the next realtime baseline.

## Documentation responsibilities

Update every completed batch:

- `PROJECT_HANDOFF.md`: current state only;
- `ROADMAP.md`: milestone status only;
- `CHANGELOG.md`: detailed increment history;
- `ARCHITECTURE.md`: only when a durable boundary or contract changes;
- `CAPABILITY_MAP.md`: when a source neighborhood or dependency boundary is discovered;
- `TEST_MATRIX.md`: when the verification contract changes.

Avoid appending repetitive historical handoff sections.

## Capability priority

Choose work in this order:

1. data loss or corruption risk;
2. incorrect learning or scheduling behavior;
3. blocked end-to-end Desktop flow;
4. malformed/incompatible real-data recovery;
5. release blocker;
6. measured performance issue;
7. usability and accessibility problem;
8. optional enhancement.

## Refactoring policy

After roughly 8–15 capability batches, or when source evidence shows rising duplication or
coupling, consider one focused refactoring increment.

A refactoring batch must:

- preserve observable behavior;
- add or rely on sufficient contract tests;
- reduce the context needed for future work;
- avoid speculative framework or architecture replacement.

## Completion checklist

A batch is complete only when:

- capability outcome exists in source;
- focused tests cover its contract;
- full `clean test` passes;
- docs match the actual source;
- rollback behavior remains intact;
- commit and push complete;
- the new commit is recorded as the baseline for the next batch.
