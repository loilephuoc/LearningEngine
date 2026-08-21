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
- **Delivered**: `BatchTtsScanner`, `BatchTtsRunner`, missing audio scan across 4 fields, skip existing audio, default language mappings (Question/Answer/Example -> English, Translation -> Vietnamese), error isolation.
- **Verification**: 4,957 tests passed, clean build.

### Phase 4 — TTS Voice Profiles / Presets [PASS]
- **Delivered**: `TtsVoiceProfile`, `TtsVoiceProfiles`, `TtsVoiceProfilePreferencesStore`, saved preferences for English (en-US / preferred voice / rate) & Vietnamese (vi-VN / preferred voice / rate), graceful fallback for missing voices, batch integration.
- **Verification**: 4,957 tests passed, clean build.

### Phase 5 — Batch Queue / Progress / Cancellation [PASS]
- **Delivered**: Bounded Job Model (`PENDING`, `RUNNING`, `SUCCESS`, `SKIPPED`, `FAILED`, `CANCELLED`), real-time `BatchTtsDialog` progress with live counters and current item indicator, cooperative safe cancellation preserving completed jobs.
- **Verification**: 4,957 tests passed, clean build.

### Phase 6 — Retry & Result Management [PASS]
- **Delivered**: Comprehensive batch result summary, friendly error classification (`TtsErrorCategory`: Network unavailable, Timeout, Voice unavailable, Generation failed, Output write failed, Cancelled), selective "Retry Failed" rerun capability without duplicate jobs.
- **Verification**: 4,957 tests passed, clean build.

### Phase 7 — Generate All Missing Audio [PASS]
- **Delivered**: Full package-level scanning, pre-generation confirmation with item & language breakdown, queue execution with real-time progress, cancellation, retry failed, Content persistence update and refreshed Content Studio view.
- **Verification**: 4,957 tests passed, clean build.
