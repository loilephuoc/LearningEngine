# Learning Engine 2.0 — Project Handoff

This file is the canonical continuation context. Keep it short, current, and operational.
Detailed batch history belongs in `CHANGELOG.md`.

## Canonical baseline

- Repository: `loilephuoc/LearningEngine`
- Canonical branch: `develop`
- Verified Batch81 baseline: `b324d0d`
- Latest completed increment: `Batch82 — contextual required JSON value shapes`
- Completed capability track: `Package Import & OPD3 Robustness`
- Next product increment: `Batch83 — corrupt persisted-data recovery boundary`
- The clean repository HEAD, source, tests, and canonical documents are the source of truth.
- If this file disagrees with the actual clean `develop` HEAD, the actual HEAD wins and this file must be corrected in the next batch.

## Product objective

Deliver a usable and dependable **Desktop Beta** before Android, iOS, or Web work.

The verified functional boundary is:

```text
Launch Desktop
→ import and attach a real OPD3 package
→ browse content and select a lesson
→ start a lesson-scoped study session
→ reveal and grade learning items
→ persist progress, queue, and scheduling state
→ recreate the application
→ resume and complete the same lesson correctly
```

Desktop search and discovery currently includes query normalization, filters, sorting,
active-refinement recovery, keyboard focus and Escape recovery, result announcements,
match highlighting, searchable-field disclosure, contextual guidance, and normalized
multi-term AND matching.

## Current milestone

**Real-data robustness and Desktop Beta release readiness**

Priority order:

1. Data safety and correctness
2. Malformed and incompatible real-data handling
3. Large-package and startup performance
4. Persistence recovery and compatibility
5. Desktop packaging, diagnostics, onboarding, and clean-machine verification
6. Optional UX polish

## Immediate next capability

Batch82 closes the remaining required JSON shape gap by routing invalid optional metadata value
types through `InvalidPackageJsonException`. Typed serializers already provide the same entry
context for manifest, contents, and learning-item shapes. Optional metadata fields remain
backward compatible and all existing validation messages remain unchanged.

The Package Import & OPD3 Robustness track is complete: candidate diagnostics, failure
isolation, archive names/count/total size, bounded strict text reads, required-entry contracts,
JSON context, manifest/metadata compatibility, content validation, integrity, dependency,
transaction, restart, and Desktop end-to-end coverage are verified. Batch83 should move to the
next Real-data robustness area: safe recovery from corrupt or interrupted persisted JSON data.

## Required reading order

1. `docs/PROJECT_HANDOFF.md`
2. `docs/CAPABILITY_MAP.md`
3. `docs/ROADMAP.md`
4. `docs/TEST_MATRIX.md`
5. `docs/BATCH_PLANNING.md`
6. Relevant production source, tests, composition roots, and persistence adapters
7. `docs/ARCHITECTURE.md` when dependency or compatibility boundaries are involved
8. `docs/CHANGELOG.md` only when historical detail is needed

Do not read the entire repository by default. Read the selected capability and its direct
dependencies deeply.

## Batch delivery contract

Every increment is delivered as:

```text
BatchXX_APPLY.zip
├── payload/
├── apply_batch.ps1
├── manifest.json
└── README.txt
```

Apply from the repository root:

```powershell
.\run_batch.ps1 BatchXX
```

`apply_batch.ps1` must:

- require the expected clean baseline;
- validate payload checksums;
- back up affected files;
- apply the complete increment;
- run `clean test`;
- roll back automatically on failure;
- print the exact commit and push commands.

A batch becomes the realtime baseline only after:

```text
BUILD SUCCESSFUL
→ git commit
→ git push
```

## Scalable continuation rule

One batch equals one coherent capability, normally touching about 8–15 files.
The file count is not a target. A smaller deep change or a larger mechanical change is
valid when the capability boundary and verification remain clear.

Before coding, define:

```text
Capability
User-visible or safety outcome
Affected boundaries
Required source and tests
Compatibility risks
Out of scope
```

After verification, update this handoff with only the new current state. Move detailed
history to `CHANGELOG.md`.
