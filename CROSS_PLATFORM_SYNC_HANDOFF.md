# Cross-Platform Sync & Portable Backup — Handoff

## Autonomous UAT continuation — 2026-08-22

- Current clean checkpoint before this handoff: `595444fbc7519e8ef4ff62cad9b5d2ee25f5591e`.
- Physical Android restore of the current 2-package baseline passed: Elementary + OPD_2nd,
  3,376 Content, 16,880 LearningItems and 16,619 media files.
- Safety Backup is now opt-in/user-managed and restore rollback is hidden/temporary/transactional.
- Resume at Phase 3 Desktop backup progress UX; do not repeat completed restore phases.

## 1. System Overview
Learning Engine uses a dual-layer synchronization and recovery architecture:
1. **Full Portable Backup (`.lebak`)**: Unified disaster recovery format containing complete package contents, full media storage, learning state, and platform supplements.
2. **Package-Aware Differential Sync (`.lesync`)**: Lightweight offline-first synchronization transferring only modified fields, new TTS audio/images, and study review events for designated packages.

## 2. Key Invariants & Rules
- **Package Baseline State Machine**: `UNKNOWN -> BASELINED -> DIRTY -> SYNCHRONIZED`.
- **First-Baseline Rule**: If a package does not exist on the target device, `.lesync` preview alerts the user that a Full Backup (`.lebak`) must be restored first.
- **Differential Media Deduplication**: `knownRemoteMediaHashes` prevents re-transmitting existing media files, reporting `existingMediaReusedCount`.
- **Review History Deduplication**: Review events are append-only and deduplicated by `ReviewEventId`.
- **Conflict Handling**: Collisions provide three options: `MERGE_FIELD_LEVEL` (default/recommended), `PRESERVE_LOCAL`, and `APPLY_INCOMING`.

## 3. Verification & Metrics
- Total Tests: **4,998 / 4,998 passing** (`.\gradlew.bat clean test` BUILD SUCCESSFUL, 2026-08-22).
- Engine: 2,234 tests
- Android: 962 tests
- Desktop: 1,802 tests

## 4. Current Phase 4 Checkpoint

- Portable backup media completeness is fixed and verified. Canonical package ID is no longer assumed to equal the media directory name.
- Real data evidence for `Vocabulary_In_Use_Upper_Intermediate`: package ID `package-07f8741f5da8588121a71c3a`; 2,887 contents; 14,435 learning items; media directory `Vocabulary_In_Use_Upper_Intermediate`; 15,211 files; 970,940,650 bytes.
- Selective scoping now follows `content-packages.libraryIds` into `content-libraries.contentIds`, retains only referenced media, and fails before publication when a selected reference is missing.
- No Android source was changed.
- Read-only Desktop creation preview now reports per-package and aggregate content, learning item,
  memory-state, review-event, media-file and byte estimates. Scope/FSRS changes invalidate stale previews,
  and archive creation is disabled until a valid preview exists.
- Save-dialog names now use `LearningEngine_Backup_<scope>_yyyyMMdd_HHmmss.lebak` in local device time.
- Seven previously undiscovered `DesktopSyncViewModelTest` tests now use the active Kotlin test runner and pass.
- Preview and archive creation use the existing IO coroutine runner, leaving Compose responsive. Typed
  progress covers preparation, scanning, media calculation, data/media writing, verification,
  finalization and completion with overall percentage, item/byte counts and elapsed time.
- Safe cancellation is polled between files and before verification; staging/temp files are removed
  and no target is published. Success UI is based on a revalidated manifest and reports archive/media
  sizes, compression and `PASSED` verification.
- Physical Upper Intermediate UAT passed after archive creation, internal verification, full extraction
  and per-entry SHA-256 verification: 2,887 contents; 14,435 learning items; 12,135 referenced media;
  786,105,846 media bytes; 830,783,670 expanded bytes; 653,007,032 archive bytes (~21.4% reduction).
- Selective staging no longer copies the entire 5-package media repository before filtering; it
  snapshots canonical JSON and then copies only resolved media references for the selected package.
- The verified archive was copied byte-exactly to Android device `24090RA29C` at
  `/sdcard/Download/LearningEngine_Backup_Vocabulary_In_Use_Upper_Intermediate_20260822_UAT.lebak`.
  No restore was attempted.
- Current checkpoint: the documentation handoff commit containing this handoff (`docs: record Android backup preview handoff`).
- Blocker/next exact task: the device requires pattern/fingerprint unlock. After the user unlocks it,
  open Backup & Restore and inspect the transferred archive. Do not restore unless package identity,
  2,887 content count, 14,435 learning-item count and 12,135 media count are correct.
