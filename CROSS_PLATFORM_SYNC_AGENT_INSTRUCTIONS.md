# Cross-Platform Sync & Portable Backup — Agent Instructions

## Standing Autonomous Continuation Rules
1. **Branch Authority**: Work only on `feat/cross-platform-sync`.
2. **Resumption Protocol**:
   - Inspect `CROSS_PLATFORM_SYNC_STATE.md` and `CROSS_PLATFORM_SYNC_ROADMAP.md`.
   - Run `git status`, `git branch --show-current`, and `git log -5 --oneline`.
   - Resume directly from the next incomplete phase.
3. **Execution Loop**:
   - Implement domain contracts, use cases, adapters, and UI.
   - Write focused unit, roundtrip, and failure tests.
   - Verify build with `.\gradlew.bat clean test`.
   - Update `CROSS_PLATFORM_SYNC_STATE.md`, `CROSS_PLATFORM_SYNC_ACCEPTANCE.md`, and `docs/CHANGELOG.md`.
   - Stage intended files and commit with `feat(sync): ...` or `test(sync): ...`.
   - Continue immediately to the next phase without pausing.
4. **Android Safety**:
   - Always run `git diff --name-only HEAD -- android` before modifying Android files.
   - Do NOT casually refactor Android code. Preserve all existing Android functionality.
5. **No Cloud Dependency**:
   - The sync engine must be 100% offline-capable, local-first, transport-agnostic (using `.lebak` and `.lesync` archive packages).
