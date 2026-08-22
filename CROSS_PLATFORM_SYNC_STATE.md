# Cross-Platform Sync & Portable Backup — State

**Current Status**: Complete & Verified (Master Implementation Delivered)
**Branch**: `feat/cross-platform-sync`
**Target Platform Interoperability**: Desktop (JVM/Compose) <-> Android (Kotlin/Jetpack Compose)
**Verification Baseline**: 4,985 / 4,985 tests passing (`.\gradlew.bat clean test` BUILD SUCCESSFUL)
- Engine Tests: 2,228
- Android Tests: 962
- Desktop Tests: 1,795

---

## 1. Architecture & Deliverables Summary

### 1.1 Dual-Mechanism Model
1. **Full Portable Backup / Restore (`.lebak`)**:
   - Versioned zip archive (`backupSchemaVersion = 2`) with canonical `manifest.json`.
   - Contains complete database stores (`contents.json`, `learning-items.json`, `memory-states.json`, `review-events.json`, `installed-packages.json`, `content-packages.json`, `content-libraries.json`, `library-collections.json`).
   - Contains all package media assets with SHA-256 integrity metadata.
   - Contains Android platform supplements (`preferences.json`, user voice recordings, lockscreen assets).
   - Used for initial platform baseline setup and full disaster recovery.
   - Strictly transactional 6-stage restore with preflight validation, staged domain validation, safety backup, live replacement, and atomic rollback.

2. **Package-Aware Differential Sync (`.lesync`)**:
   - Versioned changeset archive (`syncSchemaVersion = 1`) with `sync-manifest.json` and `media-manifest.json`.
   - **Package Scoping**: Sync operations can target individual packages (`specificPackageIds`) or all packages. Exporting Package A never touches Package B.
   - **First-Baseline Rule**: State machine `UNKNOWN -> BASELINED -> DIRTY -> SYNCHRONIZED`. If a target device has never received a package, `.lesync` preview detects `missingBaselinePackageIds`, sets `requiresFullBackup = true`, and guides the user to perform a Full Backup/Restore first.
   - **Differential Media Deduplication**: Scans `knownRemoteMediaHashes` across devices. Only newly generated TTS audio or updated images are packaged in `.lesync`. Pre-existing media files are matched by SHA-256 and flagged `isContainedInSyncPackage = false`, saving bandwidth and transfer time.
   - **Review History & FSRS Convergence**: Append-only synchronization of `ReviewEvent` history deduplicated by immutable `ReviewEventId`. `MemoryState` is reconciled with the latest review timestamp.
   - **Conflict Detection & Resolution UX**: Human-readable conflict inspection in UI (`MERGE_FIELD_LEVEL`, `PRESERVE_LOCAL`, `APPLY_INCOMING`) showing Package, Item, Field, Local text, and Incoming text.

---

## 2. Phase Execution Status

| Phase | Description | Status | Verification |
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
- **D-01 (Interchange Separation)**: Clear separation between `.lebak` (Full Disaster Recovery / Baseline) and `.lesync` (Differential Changeset).
- **D-02 (Package-Aware Scoping)**: Desktop and Android do not need identical package sets; synchronization is strictly package-aware.
- **D-03 (Selective Media Transmission)**: Unchanged media is never re-transmitted; deduplication uses SHA-256 manifests.
- **D-04 (Append-Only Review History)**: Review history is preserved and deduplicated by `ReviewEventId` without overwriting prior history.
- **D-05 (Zero Cloud / Local First)**: Sync is 100% transport-agnostic and offline-capable via USB, local sharing, or file transfer.
