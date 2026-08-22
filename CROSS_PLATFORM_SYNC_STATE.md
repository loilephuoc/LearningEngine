# Cross-Platform Sync & Portable Backup — State

**Current Status**: Phase 4 in progress — media completeness, creation preview and readable naming delivered; progress/UAT remains
**Branch**: `feat/cross-platform-sync`
**Target Platform Interoperability**: Desktop (JVM/Compose) <-> Android (Kotlin/Jetpack Compose)
**Verification Baseline**: 4,997 / 4,997 tests passing (`.\gradlew.bat clean test` BUILD SUCCESSFUL, 2026-08-22)
- Engine Tests: 2,233
- Android Tests: 962
- Desktop Tests: 1,802

---

## 1. Architecture & Deliverables Summary

### 1.1 Dual-Mechanism Model
1. **Full Portable Backup / Restore (`.lebak`)**:
   - Versioned zip archive (`backupSchemaVersion = 2`) with canonical `manifest.json`.
   - **Selective Package Scoping on Backup**: Allows exporting All Packages or Selected Packages (`specificPackageIds`). Scopes staged JSON stores and media files. Manifest includes structured package entries (`packages: List<PortableBackupPackageEntryV2>`) with packageId, packageName, version, contentCount, learningItemCount, mediaCount, and fingerprint.
   - **Pre-Restore Package Inspection & Compatibility**: Before restore, manifest packages are analyzed against local state and categorized with status badges:
     - `NEW`: Package does not exist locally.
     - `PRESENT`: Same identity and compatible content count.
     - `CONFLICT`: Same identity but local content count or content checksum diverged.
   - **Selective Restore Isolation**: Allows restoring All Packages or Selected Packages (`selectedPackageIds`). Restoring Package A only modifies Package A's contents, learning items, memory states, review history, and media directory; Package B and Package C remain 100% untouched.
   - Strictly transactional 6-stage restore with preflight validation, staged domain validation, safety backup, live replacement, and atomic rollback.

2. **Package-Aware Differential Sync (`.lesync`)**:
   - Versioned changeset archive (`syncSchemaVersion = 1`) with `sync-manifest.json` and `media-manifest.json`.
   - **Package Scoping**: Sync operations can target individual packages (`specificPackageIds`) or all packages.
   - **First-Baseline Rule**: State machine `UNKNOWN -> BASELINED -> DIRTY -> SYNCHRONIZED`. If a target device has never received a package, `.lesync` preview detects `missingBaselinePackageIds`, sets `requiresFullBackup = true`, and guides the user to perform a Full Backup/Restore first.
   - **Differential Media Deduplication**: Scans `knownRemoteMediaHashes` across devices. Only newly generated media files are transmitted.
   - **Review History & FSRS Convergence**: Append-only synchronization of `ReviewEvent` history deduplicated by immutable `ReviewEventId`. `MemoryState` is reconciled with the latest review timestamp.
   - **Conflict Detection & Resolution UX**: Human-readable conflict inspection in UI (`MERGE_FIELD_LEVEL`, `PRESERVE_LOCAL`, `APPLY_INCOMING`).

---

## 2. Phase Execution Status

| Phase | Description | Status | Verification |
|---|---|---|---|
| **Phase 1** | Repository Audit + Architecture + Interchange Contract | **COMPLETED** | Verified |
| **Phase 2** | Differential Sync + Media Deduplication + FSRS Convergence | **COMPLETED** | Verified |
| **Phase 3** | Selective Portable Backup/Restore + Package Identification + Compatibility Badges | **COMPLETED** | Verified (4,988 tests) |
| **Phase 4** | Portable Backup Quality, integrity evidence, preview, progress, naming and real-package UAT | **IN PROGRESS** | Media fix + preview/naming verified (4,997 tests) |

## 3. Phase 4 Continuation

- **Completed capabilities**: selective backup resolves stable package ID through canonical package/library ownership to Content IDs and package-name media paths; read-only creation preview reports package/content/item/FSRS/media counts and estimated bytes; selected scope uses a safe local timestamp filename. Only referenced media is retained; a missing reference aborts publication.
- **Measured root cause**: `Vocabulary_In_Use_Upper_Intermediate` has package ID `package-07f8741f5da8588121a71c3a`, while its media folder is named `Vocabulary_In_Use_Upper_Intermediate`. The old ID-prefix filter excluded all 15,211 files (970,940,650 source bytes), explaining the approximately 3.7 MB metadata-only archive.
- **Files changed**: `JvmLearningDataRecoveryManager.kt`, `SelectivePackageBackupAndRestoreTest.kt`, checkpoint documentation.
- **Next exact capability**: move preview/archive work off the Compose UI thread and add typed backup progress/verification/success reporting.
- **Current checkpoint**: the capability commit containing this state update (`feat(sync): add portable backup preview and readable naming`).
- **Expected post-commit worktree**: clean.

## 4. Decision Log
- **D-01 (Interchange Separation)**: Clear separation between `.lebak` (Full Disaster Recovery / Baseline) and `.lesync` (Differential Changeset).
- **D-02 (Package-Aware Scoping)**: Desktop and Android do not need identical package sets; synchronization and backup/restore are strictly package-aware.
- **D-03 (Selective Media Transmission & Isolation)**: Backup and restore preserve package isolation; unselected package media is untouched.
- **D-04 (Append-Only Review History)**: Review history is preserved and deduplicated by `ReviewEventId` without overwriting prior history.
- **D-05 (Zero Cloud / Local First)**: Sync is 100% transport-agnostic and offline-capable via USB, local sharing, or file transfer.
- **D-06 (Pre-Restore Compatibility)**: Manifest inspection identifies `NEW`, `PRESENT`, and `CONFLICT` package statuses before executing restore.
