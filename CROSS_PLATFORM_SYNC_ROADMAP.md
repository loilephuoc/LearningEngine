# Cross-Platform Sync & Portable Backup — Roadmap

## Current autonomous UAT sequence

1. Phase 0 recovery/audit — PASS.
2. Safety semantics — PASS at `595444fb`.
3. Restore robustness — automated PASS and 2-package physical restore PASS.
4. Desktop truthful backup progress — PASS (real checksum counts, indeterminate preparation, live elapsed timer).
5. Desktop success dialog — PASS (verified report plus Close only).
6. Android localization — IN PROGRESS.
7. Android Library/active package and Study indicator.
8. Reminder controls and image radius.
9. Desktop Content Studio context Delete.
10. Global localization audit, full regression and physical UAT.

This roadmap defines the sequential capabilities and definition of done for the cross-platform sync & backup master implementation between Desktop and Android.

---

## Phase 1: Repository Audit, Architecture & Interchange Contract
- **Outcome**: Complete audit of existing persistence, backup/restore, media storage, and learning domain models; definition of the versioned interchange contracts (`SyncPackageManifest`, `ContentDelta`, `ReviewDelta`, `MediaSyncDescriptor`, `SyncConflict`).
- **Definition of Done**: Governance docs created, sync domain contracts implemented in engine core, architecture validated.

## Phase 2: Portable Backup Schema & Versioning Unification
- **Outcome**: Unify backup schema so both Desktop and Android produce and consume `.lebak` v2 format with full media inspection, content counts, integrity hashing, and backward compatibility.
- **Definition of Done**: Unit & roundtrip tests pass for full backup creation, manifest verification, and structural inspection.

## Phase 3: Desktop Backup & Restore Integration
- **Outcome**: Modernize Desktop Recovery to produce and consume Portable Backup V2 with UI inspection, stage-validate-restore-verify lifecycle, safety backup, and rollback protection.
- **Definition of Done**: Desktop recovery roundtrip tests pass; legacy v1 backup migration/rejection policy tested; UI supports preview & safe restore.

## Phase 4: Android Adapter & Compatibility Migration
- **Outcome**: Ensure Android portable backup snapshotting and restore consumer handle all platform supplements (`preferences.json`, voice recordings, lockscreen assets) seamlessly with the unified v2 format.
- **Definition of Done**: Android acceptance tests pass with zero regression in Android baseline.

## Phase 5: Content & Media Synchronization Engine
- **Outcome**: Lightweight differential sync engine (`SyncEngine`) that detects changed content items and newly added/modified media assets without re-transferring large unchanged packages.
- **Definition of Done**: Export/import of content changesets and media assets with hash validation and deduplication; tests verify unchanged media is untouched.

## Phase 6: Review & FSRS Synchronization
- **Outcome**: Event-based synchronization for `ReviewEvent` history with deterministic deduplication by `ReviewEventId` and automatic `MemoryState` reconciliation.
- **Definition of Done**: Dual-device review event merge tests pass; duplicate reviews are idempotent; FSRS state accurately reflects latest timeline.

## Phase 7: Conflict Detection & Deterministic Resolution
- **Outcome**: Robust conflict engine detecting concurrent field-level content edits, divergent media references, and out-of-order review events; generation of detailed `SyncConflictReport`.
- **Definition of Done**: Conflict detection tests cover all 4 conflict classes (content, media, review, metadata) without silent data destruction.

## Phase 8: Desktop Sync UI & Workflow
- **Outcome**: Desktop UI for Backup, Restore, Export Sync Changeset, and Import Sync Changeset with summary dialogs (showing content count, media count, review count, conflicts, errors).
- **Definition of Done**: Compose Desktop screens and dialogs wired to `SyncEngine` with keyboard/accessibility support and clean error feedback.

## Phase 9: Android UI & Sync Integration
- **Outcome**: Android UI integration in Backup & Restore / Library settings allowing export and import of `.lesync` differential changeset packages alongside `.lebak`.
- **Definition of Done**: Android ViewModels and Compose screens handle sync preview, validation, and execution.

## Phase 10: End-to-End Cross-Platform Verification
- **Outcome**: Comprehensive suite of cross-platform tests (Desktop -> Android, Android -> Desktop, bi-directional multi-cycle sync).
- **Definition of Done**: End-to-end matrix tests pass under realistic data scenarios (vocabulary packages, TTS audio, recordings, reviews).

## Phase 11: Hardening, Safety & Performance
- **Outcome**: Streaming I/O for large archives, disk space margins, corrupted package rejection, stress tests, and final clean documentation.
- **Definition of Done**: `./gradlew clean test` passes 100%; documentation reflects exact verified state.
