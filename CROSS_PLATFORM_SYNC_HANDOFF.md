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
- Total Tests: **4,985 / 4,985 passing** (`.\gradlew.bat clean test` BUILD SUCCESSFUL).
- Engine: 2,228 tests
- Android: 962 tests
- Desktop: 1,795 tests
