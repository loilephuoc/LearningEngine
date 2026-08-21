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
- [x] Pass `./gradlew.bat clean test` (4,957 tests)

## Phase 4 — TTS Voice Profiles / Presets
- [x] English profile (US / preferred voice / Normal rate)
- [x] Vietnamese profile (vi-VN / preferred voice / Normal rate)
- [x] Persistence via Desktop preferences/settings architecture
- [x] Unavailable voice fallback handling
- [x] Batch TTS integration with saved profiles
- [x] Pass `./gradlew.bat clean test` (4,957 tests)

## Phase 5 — Batch Queue / Progress / Cancellation
- [x] Bounded Job Model (`PENDING`, `RUNNING`, `SUCCESS`, `SKIPPED`, `FAILED`, `CANCELLED`)
- [x] Real-time Progress dialog with live counters
- [x] Safe Cancellation preserving already generated audio
- [x] Deterministic sequential execution without duplicate jobs
- [x] Pass `./gradlew.bat clean test` (4,957 tests)

## Phase 6 — Retry & Result Management
- [x] Comprehensive batch result summary
- [x] User-friendly error categorization
- [x] "Retry Failed" selective re-execution
- [x] Lightweight generation history tracking
- [x] Pass `./gradlew.bat clean test` (4,957 tests)

## Phase 7 — Generate All Missing Audio
- [x] Package-wide missing audio scan
- [x] Confirmation dialog with target item and language breakdown
- [x] End-to-end integration with Queue, Progress, Cancel, Retry, Apply
- [x] Refreshed Content Studio view and verified persistence
- [x] Pass `./gradlew.bat clean test` (4,957 tests)

## Phase 8 — Batch Workflow Polish (Selected Items → Fields → Profiles → Preview → Generate → Apply → Undo)
- [x] User can select multiple items in Content Studio.
- [x] User can select Question/Answer/Example/Translation independently with live missing counts.
- [x] English fields map to English automatically (Question/Answer/Example).
- [x] Translation maps to Vietnamese automatically.
- [x] English voice + speech rate can be selected independently.
- [x] Vietnamese voice + speech rate can be selected independently.
- [x] Pre-generation voice preview works using representative text from selected items.
- [x] Voice dropdown layout bug fixed (wide popup, readable names, smooth scrolling, no narrow column wrapping).
- [x] Missing audio scanned at TARGET level (1 Item != 1 Target).
- [x] Existing audio is NEVER overwritten (safe skip).
- [x] Empty text is skipped.
- [x] Batch generation works sequentially with error isolation.
- [x] Target-based progress bar and live counters.
- [x] "Retry Failed" works for failed targets.
- [x] Generate != Apply safety boundary preserved.
- [x] Atomic Batch Apply updates only target references and persists all items.
- [x] Batch Apply is safely reversible via `Undo TTS`.
- [x] Undo restores exact previous audio references (missing becomes missing, existing stays original).
- [x] Generated files cleaned during Undo only when safely unreferenced; existing assets never deleted.
- [x] Single-item TTS and Generate All Missing Audio preserved without regression.
- [x] Android remains 100% HARD FROZEN (0 diff).
- [x] Pass `./gradlew.bat clean test` (4,962 tests, 0 failures, 0 errors, 0 skipped).
