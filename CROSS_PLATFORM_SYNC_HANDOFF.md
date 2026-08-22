# Cross-Platform Sync & Portable Backup — Handoff

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
- Total Tests: **4,990 / 4,990 passing** (`.\gradlew.bat clean test` BUILD SUCCESSFUL, 2026-08-22).
- Engine: 2,233 tests
- Android: 962 tests
- Desktop: 1,795 tests

## 4. Current Phase 4 Checkpoint

- Portable backup media completeness is fixed and verified. Canonical package ID is no longer assumed to equal the media directory name.
- Real data evidence for `Vocabulary_In_Use_Upper_Intermediate`: package ID `package-07f8741f5da8588121a71c3a`; 2,887 contents; 14,435 learning items; media directory `Vocabulary_In_Use_Upper_Intermediate`; 15,211 files; 970,940,650 bytes.
- Selective scoping now follows `content-packages.libraryIds` into `content-libraries.contentIds`, retains only referenced media, and fails before publication when a selected reference is missing.
- No Android source was changed.
- Current checkpoint: the capability commit containing this handoff (`fix(sync): validate portable backup media completeness`).
- Next exact task: implement a read-only backup preview/size audit contract and wire it to `DesktopBackupDialog` before archive creation.
