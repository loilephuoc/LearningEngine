# Learning Engine Architect Library

This directory contains capability-level Architect Contracts approved by the Chief Architect and Product Owner.

## Authority

When a capability contract exists, it is the primary implementation contract for that capability. Repository source, accepted tests, `AGENTS.md`, `REPOSITORY_CONSTITUTION.md`, and established architecture remain authoritative. A capability contract does not authorize violating existing domain boundaries or inventing unsupported APIs.

When instructions conflict, use this order:

1. current explicit Product Owner decision;
2. repository constitution and standing agent workflow;
3. capability contract;
4. other architecture/product documents;
5. historical chat or handoff notes.

Stop and report material contradictions instead of guessing.

## Workflow

```text
Chief Architect defines capability
        ↓
Contract committed under docs/architect
        ↓
Antigravity inspects real source and baseline
        ↓
Implementation + focused verification
        ↓
Full verification + self-audit + real UAT evidence
        ↓
Capability commit
        ↓
Product Owner push
        ↓
Chief Architect review
```

## Rules

- Read the relevant contract before implementation.
- Inspect the real repository; never invent classes or ports.
- Preserve accepted capabilities and tests.
- Keep business logic outside Desktop UI.
- Do not silently expand scope into the next capability.
- Do not claim manual UAT from automated tests.
- Do not push unless the Product Owner explicitly asks.
- Record known limitations and technical debt honestly.

## Current contracts

- [`PLE-018A_CONTENT_STUDIO.md`](PLE-018A_CONTENT_STUDIO.md) — redesign the Learning Browser into the first Content Studio workspace.
