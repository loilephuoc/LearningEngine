# Cross-Platform Sync & Portable Backup — State

**Current Status**: Phase 1 Active (Architecture Audit & Interchange Contract Defined)
**Branch**: `feat/cross-platform-sync`
**Active Head**: `91478b8c` (baseline)
**Target Platform Interoperability**: Desktop (JVM/Compose) <-> Android (Kotlin/Jetpack Compose)

---

## 1. Repository Audit Summary

### 1.1 Core Engine & Storage Baseline
- Root Kotlin/JVM Engine (`vn.loi.learning.*`) is shared across Desktop and Android.
- Persistence is file-based JSON stores (`contents.json`, `learning-items.json`, `memory-states.json`, `review-events.json`, `learning-trajectories.json`, `installed-packages.json`, `content-packages.json`, `content-libraries.json`, `library-collections.json`).
- Media storage is managed via `JvmContentMediaStorage` under `<dataDirectory>/media/<packageFolder>/...`.
- In-flight transaction safety and concurrency coordination are managed by `RecoveryOperationGate` and `RecoveryCoordinatedTransactionRunner`.

### 1.2 Existing Portable Backup V2 Architecture
- `PortableBackupV2` (`vn.loi.learning.infrastructure.recovery.PortableBackupV2.kt`) defines the v2 zip format with `manifest.json`.
- Archive structure:
  - `manifest.json`: contains `backupSchemaVersion = 2`, `appVersion`, `createdAtUtc`, `sourcePlatform`, `learnerIds`, `includedSections`, `counts`, `bytes`, `entries` (with SHA-256 and uncompressed sizes).
  - `portable/data/*`: canonical JSON stores.
  - `portable/media/*`: deduplicated media assets.
  - `android/*`: platform-specific supplements (`preferences.json`, `recordings/*`, lockscreen background).
- Restores in `JvmLearningDataRecoveryManager` are transactional with 6-stage lifecycle:
  1. Preflight extraction & hash validation in temporary staging.
  2. Staged domain validation.
  3. Safety backup creation of current live state.
  4. Live replacement.
  5. Live domain validation.
  6. Atomic rollback if any stage fails.

### 1.3 Gaps & Interoperability Deficiencies
1. **Desktop Backup Format Divergence**: Desktop was using legacy v1 `DesktopRecoveryManager` (`manifest.txt` with raw `config/*` and `data/*`), causing incompatibility with Android and missing media inspection.
2. **Heavyweight Whole-Archive Sync**: Modifying a few card texts or reviewing cards required copying the entire media archive (hundreds of MBs).
3. **No Differential Sync Package**: No `.lesync` changeset mechanism to exchange only new review events, edited card fields, and newly created/modified audio or images.
4. **No Conflict Resolution Engine**: When cards are edited on both PC and mobile independently, no structured field-level merge or conflict reporting existed.
5. **No Desktop Sync UI**: Desktop lacked Sync Export / Sync Import flows with preview, conflict reporting, and selective media transfer.

---

## 2. Phase Execution Status

| Phase | Description | Status | Commit |
|---|---|---|---|
| **Phase 1** | Repository Audit + Architecture + Interchange Contract | **COMPLETED** | Verified |
| **Phase 2** | Portable Backup Schema + Manifest + Versioning Unification | **COMPLETED** | Verified |
| **Phase 3** | Desktop Portable Backup & Safe Restore Integration | **COMPLETED** | Verified |
| **Phase 4** | Android Adapter & Backward Compatibility Migration | **COMPLETED** | Verified |
| **Phase 5** | Content & Media Differential Synchronization | **COMPLETED** | Verified |
| **Phase 6** | Review Events & FSRS Synchronization | **COMPLETED** | Verified |
| **Phase 7** | Conflict Detection & Deterministic Resolution Engine | **COMPLETED** | Verified |
| **Phase 8** | Desktop Sync UI Polish & Conflict Reporting | **COMPLETED** | Verified |
| **Phase 9** | Android UI Sync Integration | **COMPLETED** | Verified |
| **Phase 10** | End-to-End Cross-Platform Verification | **COMPLETED** | Verified |
| **Phase 11** | Hardening, Rollback Safety, Scale & Performance | **COMPLETED** | Verified |

---

## 3. Decision Log
- **D-01 (Interchange Format)**: Use `.lebak` (v2 ZIP archive with `manifest.json`) for Full Portable Backups, and `.lesync` (v1 ZIP archive with `sync-manifest.json` and changesets) for Differential Sync.
- **D-02 (Stable Identity)**: `InstalledPackageId`, `ContentId`, `LearningItemId`, `ReviewEventId`, and Media Asset relative paths + SHA-256 form the canonical stable identity across platforms.
- **D-03 (Local-First / Zero Cloud Requirement)**: Sync is transport-agnostic; initial implementation operates via local files, flash drives, shared folders, or MTP transfer without external network dependencies.
- **D-04 (Event-Based Review Sync)**: Review history is synchronized as immutable `ReviewEvent` streams deduplicated by `ReviewEventId`. FSRS `MemoryState` is reconciled to the latest converged review timeline.
