# Cross-Platform Sync & Portable Backup — Handoff

## Summary
This project delivers seamless, local-first cross-platform interoperability between Desktop and Android for LearningEngine.

### Core Workflows Supported
1. **Initial Setup via Full Portable Backup (`.lebak`)**:
   - PC: Import / generate packages & TTS audio -> Export Portable Backup.
   - Android: Restore Portable Backup -> Full library, cards, and media restored without manual re-importing.
2. **Differential Content & Media Sync (`.lesync`)**:
   - PC: Edit cards, add/regenerate audio via TTS -> Export Sync Changeset.
   - Android: Import Sync Changeset -> Only modified fields and new media are transferred.
3. **Differential Study Progress Sync (`.lesync`)**:
   - Android: Complete card reviews -> Export Sync Changeset.
   - PC: Import Sync Changeset -> Review events merged & deduplicated; FSRS memory states updated.

---

## Architectural Boundaries
- **Engine Core** (`src/main/kotlin`):
  - `vn.loi.learning.domain.sync.model`: Interchange domain models, delta entities, conflicts.
  - `vn.loi.learning.application.sync`: Export, import, and conflict resolution use cases.
  - `vn.loi.learning.infrastructure.sync`: Streaming ZIP format, SHA-256 validation, manifest serialization.
  - `vn.loi.learning.infrastructure.recovery`: Portable Backup V2 engine (`JvmLearningDataRecoveryManager`).
- **Desktop Adapter** (`desktop/src/main/kotlin`):
  - Modernized `DesktopRecoveryManager` backed by `JvmLearningDataRecoveryManager`.
  - Desktop Sync Dialogs (Backup, Restore, Export Sync, Import Sync) in Settings/Library.
- **Android Adapter** (`android/src/main/kotlin`):
  - `AndroidPortableBackupSnapshot` (supplements: preferences, recordings, background).
  - `BackupRestoreViewModel` supporting both full `.lebak` and differential `.lesync`.

---

## Continuation Guide for New Agents
1. Read `CROSS_PLATFORM_SYNC_AGENT_INSTRUCTIONS.md`.
2. Inspect `git status`, `git branch --show-current`, and `git log -5 --oneline`.
3. Check `CROSS_PLATFORM_SYNC_STATE.md` for completed vs pending phases.
4. Execute the next incomplete phase sequentially following the `TEST -> VERIFY -> DOCUMENT -> COMMIT -> CONTINUE` rule.
