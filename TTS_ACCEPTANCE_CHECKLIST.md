# Learning Engine — TTS Acceptance Checklist

## Phase 1 — TTS Audio Generation Backend
- [x] Edge-TTS WebSocket audio synthesis client
- [x] MP3 header and payload chunk parsing
- [x] Audio file naming policy (`{contentId}_{field}_{lang}_{timestamp}.mp3`)
- [x] Temporary preview store with automatic cleanup
- [x] DesktopTtsAudioService integration with ContentMediaStorage
- [x] Pass `./gradlew.bat clean test` (4,936 tests)

## Phase 2 — Desktop TTS UI + Preview + Generate + Apply
- [x] Compose Desktop `DesktopTtsDialog`
- [x] Dynamic voice selection by Language, Region, Gender, Rate
- [x] Real-time audio Preview without mutating Content or package media
- [x] Generate permanent audio to ContentMediaStorage without auto-apply
- [x] Verification player for generated audio
- [x] Apply updates only the target audio slot, protecting existing audio
- [x] Integration with ContentExplorerPane, MediaInspectorPane, StudioScreen
- [x] Pass `./gradlew.bat clean test` (4,944 tests)

## Phase 3 — Batch TTS (Selected Items & Missing Fields)
- [x] Scan missing audio targets for selected items
- [x] Existing audio skip policy (never overwrite)
- [x] Default field-to-language mappings (Question/Answer/Example -> EN, Translation -> VI)
- [x] Error isolation: failures on individual items do not halt batch
- [x] Isolated apply to Content storage
- [x] Unit & regression tests pass without live network dependencies
- [x] Pass `./gradlew.bat :desktop:test` and `./gradlew.bat clean test`

## Phase 4 — TTS Voice Profiles / Presets
- [x] English profile (US / preferred voice / Normal rate)
- [x] Vietnamese profile (vi-VN / preferred voice / Normal rate)
- [x] Persistence via Desktop preferences/settings architecture
- [x] Unavailable voice fallback handling
- [x] Batch TTS integration with saved profiles
- [x] Pass `./gradlew.bat :desktop:test` and `./gradlew.bat clean test`

## Phase 5 — Batch Queue / Progress / Cancellation
- [x] Bounded Job Model (`PENDING`, `RUNNING`, `SUCCESS`, `SKIPPED`, `FAILED`, `CANCELLED`)
- [x] Real-time Progress dialog with live counters
- [x] Safe Cancellation preserving already generated audio
- [x] Deterministic sequential execution without duplicate jobs
- [x] Pass `./gradlew.bat :desktop:test` and `./gradlew.bat clean test`

## Phase 6 — Retry & Result Management
- [x] Comprehensive batch result summary
- [x] User-friendly error categorization
- [x] "Retry Failed" selective re-execution
- [x] Lightweight generation history tracking
- [x] Pass `./gradlew.bat :desktop:test` and `./gradlew.bat clean test`

## Phase 7 — Generate All Missing Audio
- [x] Package-wide missing audio scan
- [x] Confirmation dialog with target item and language breakdown
- [x] End-to-end integration with Queue, Progress, Cancel, Retry, Apply
- [x] Refreshed Content Studio view and verified persistence
- [x] Full regression verification across all modules
- [x] Pass `./gradlew.bat clean test` (4,957 tests)
