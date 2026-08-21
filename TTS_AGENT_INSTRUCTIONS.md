# Learning Engine — TTS Agent Instructions

This document governs the autonomous execution and development rules for the Desktop TTS capability suite (Phases 1 through 7) in LearningEngine.

## Core Rules & Invariants

1. **Android Freeze**:
   - `android/**` is 100% HARD FROZEN.
   - Never edit, reformat, refactor, or touch any Android source or resources.
   - If an Android test fails unexpectedly, report only.

2. **Git & Worktree Safety**:
   - Work strictly in the `feat/desktop-tts` branch.
   - Never run destructive Git commands (`reset --hard`, `restore .`, `clean`, `stash`, `rebase`, `amend`).
   - Do not push automatically.
   - Make atomic, tested checkpoint commits per Phase.

3. **Desktop Scope & Single Architecture**:
   - All TTS logic lives in `desktop` module.
   - Reuse existing `TtsEngine`, `EdgeTtsEngine`, `DesktopTtsAudioService`, `ContentMediaStorage`, and content persistence infrastructure.
   - Do not create secondary engines or duplicate abstractions.

4. **Audio & Data Safety**:
   - Filename format: `{contentId}_{field}_{lang}_{yyyyMMdd_HH-mm-ss-SSS}.mp3`.
   - Never overwrite existing audio references unless explicitly replacing via single-item UI.
   - In batch / automatic flows: Missing audio -> Generate; Existing audio -> SKIP.
   - Apply updates ONLY the target audio field and preserves all other fields.

5. **Testing Verification**:
   - Unit & integration tests for each phase must not depend on live networks.
   - Before completing each phase: `.\gradlew.bat :desktop:test` and `.\gradlew.bat clean test`.
