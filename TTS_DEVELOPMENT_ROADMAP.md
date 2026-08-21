# Learning Engine — TTS Development Roadmap

## Overview
Autonomous end-to-end delivery of Text-to-Speech (TTS) integration for Desktop Content Studio.

---

### Phase 1 — TTS Audio Generation Backend [PASS]
- **Commit**: `87afda43ca06a8d5015e12a22d55cc9c81b79a38`
- **Delivered**: `EdgeTtsEngine`, `TtsAudioFileNamer`, `TtsPreviewStore`, `DesktopTtsAudioService`, edge-tts WebSocket client, MP3 chunk parser, audio synthesis pipeline.
- **Verification**: 4,936 tests passed, clean build.

### Phase 2 — Desktop TTS UI + Preview + Generate + Apply [PASS]
- **Commit**: `fe4953d8cb0958372b84684ca4ef2f06d591781b`
- **Delivered**: `DesktopTtsDialog`, `DesktopTtsDialogState`, single-item Preview, Generate, Post-generate playback, Apply, Studio Explorer & Media Inspector integration.
- **Verification**: 4,944 tests passed, clean build.

### Phase 3 — Batch TTS (Selected Items & Missing Fields) [PASS]
- **Commit**: `7e7a773c45f9ae96c5e0c349af8d971d0efa70b3`
- **Delivered**: `BatchTtsScanner`, `BatchTtsRunner`, missing audio scan across 4 fields, skip existing audio, default language mappings (Question/Answer/Example -> English, Translation -> Vietnamese), error isolation.
- **Verification**: 4,957 tests passed, clean build.

### Phase 4 — TTS Voice Profiles / Presets [PASS]
- **Commit**: `7e7a773c45f9ae96c5e0c349af8d971d0efa70b3`
- **Delivered**: `TtsVoiceProfile`, `TtsVoiceProfiles`, `TtsVoiceProfilePreferencesStore`, saved preferences for English (en-US / preferred voice / rate) & Vietnamese (vi-VN / preferred voice / rate), graceful fallback for missing voices, batch integration.
- **Verification**: 4,957 tests passed, clean build.

### Phase 5 — Batch Queue / Progress / Cancellation [PASS]
- **Commit**: `7e7a773c45f9ae96c5e0c349af8d971d0efa70b3`
- **Delivered**: Bounded Job Model (`PENDING`, `RUNNING`, `SUCCESS`, `SKIPPED`, `FAILED`, `CANCELLED`), real-time `BatchTtsDialog` progress with live counters and current item indicator, cooperative safe cancellation preserving completed jobs.
- **Verification**: 4,957 tests passed, clean build.

### Phase 6 — Retry & Result Management [PASS]
- **Commit**: `7e7a773c45f9ae96c5e0c349af8d971d0efa70b3`
- **Delivered**: Comprehensive batch result summary, friendly error classification (`TtsErrorCategory`: Network unavailable, Timeout, Voice unavailable, Generation failed, Output write failed, Cancelled), selective "Retry Failed" rerun capability without duplicate jobs.
- **Verification**: 4,957 tests passed, clean build.

### Phase 7 — Generate All Missing Audio [PASS]
- **Commit**: `7e7a773c45f9ae96c5e0c349af8d971d0efa70b3`
- **Delivered**: Full package-level scanning, pre-generation confirmation with item & language breakdown, queue execution with real-time progress, cancellation, retry failed, Content persistence update and refreshed Content Studio view.
- **Verification**: 4,957 tests passed, clean build.

### Batch Workflow Polish [PASS]
- **Commit**: `9068a783b21cdd64403e007487a190106d0ec6be`
- **Delivered**: Selected-item scope scanning, audio field selection checkboxes, missing counts, fixed voice dropdown layout, pre-generation preview, atomic apply, and full Undo TTS.
- **Verification**: 4,962 tests passed, clean build.

---

### Phase 8 — Advanced Voice Strategy [PASS]
- **Delivered**:
  - `VoiceStrategyMode`: `SINGLE_VOICE`, `FALLBACK_CHAIN`, `VOICE_ROTATION`.
  - `VoiceStrategyConfig` and `VoiceAttempt` tracking.
  - Multi-attempt sequential execution per target with fallback recovery.
  - Distinction between `requestedVoice` and `actualVoiceUsed` with `recoveredViaFallback` flag.
  - Bounded retry attempts per target with immediate cancellation support.
- **Verification**: 4,969 tests passed, clean build (0 failures, 0 errors, 0 skipped).

### Phase 9 — Advanced Batch Workflow [PASS]
- **Delivered**:
  - Full multi-target field combinations (Question + Answer + Example + Translation independently).
  - Pre-generation sample preview before large batch runs.
  - Pre-apply batch review showing total, English, Vietnamese, skipped, fallback recovered, failed, and detailed error attempt history.
- **Verification**: 4,969 tests passed, clean build.

### Phase 10 — Safety & Recovery Hardening [PASS]
- **Delivered**:
  - Enforced `Generate != Apply` separation with non-mutating preview.
  - Atomic apply of successful targets only (failed remain available for retry).
  - Safe media cleanup on Undo: deletes only newly created unreferenced files, never touches existing media.
- **Verification**: 4,969 tests passed, clean build.

### Phase 11 — Final Integration & Acceptance [PASS]
- **Delivered**:
  - Zero Android diff verified (`git diff --name-only HEAD -- android` = 0).
  - Clean full regression pass (`.\gradlew.bat clean test` = 4,969 tests, 0 failures).
  - Desktop runtime intact and verified.
- **Verification**: 4,969 tests passed, clean build.
