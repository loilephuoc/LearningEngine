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

## Batch Workflow Polish
- [x] User can select multiple items in Content Studio
- [x] User can select Question/Answer/Example/Translation independently with live missing counts
- [x] English fields map to English automatically (Question/Answer/Example)
- [x] Translation maps to Vietnamese automatically
- [x] English voice + speech rate can be selected independently
- [x] Vietnamese voice + speech rate can be selected independently
- [x] Pre-generation voice preview works using representative text from selected items
- [x] Voice dropdown layout bug fixed (wide popup, readable names, smooth scrolling, no narrow column wrapping)
- [x] Missing audio scanned at TARGET level (1 Item != 1 Target)
- [x] Existing audio is NEVER overwritten (safe skip)
- [x] Empty text is skipped
- [x] Atomic Batch Apply updates only target references and persists all items
- [x] Batch Apply is safely reversible via `Undo TTS`
- [x] Undo restores exact previous audio references
- [x] Generated files cleaned during Undo only when safely unreferenced; existing assets never deleted
- [x] Pass `./gradlew.bat clean test` (4,962 tests)

## Phase 8 — Advanced Voice Strategy
- [x] Single Voice mode
- [x] Fallback Chain mode (Primary -> Fallback 1 -> Fallback 2 -> ...)
- [x] Voice Rotation / Workflow mode with sequence continuation across items
- [x] Attempt history recording per target
- [x] Requested voice vs actual voice used distinction and tracking
- [x] Bounded retry attempts per target
- [x] Immediate cancellation handling
- [x] Pass `./gradlew.bat clean test` (4,969 tests)

## Phase 9 — Advanced Batch Workflow
- [x] Multi-target combinations without treating item as single unit
- [x] Representative sample preview before large batches
- [x] Pre-apply batch review with fallback recovery counts and failure details
- [x] Pass `./gradlew.bat clean test` (4,969 tests)

## Phase 10 — Safety & Recovery
- [x] Strict Generate != Apply boundary
- [x] Partial batch success: apply only succeeds targets while keeping failed for retry
- [x] Safe unreferenced cleanup on Undo; never deletes existing media
- [x] Pass `./gradlew.bat clean test` (4,969 tests)

## Phase 11 — Final Integration & Acceptance
- [x] Full regression test pass with 0 failures, 0 errors, 0 skipped (4,969 tests)
- [x] Android remains 100% HARD FROZEN (0 diff)
- [x] Working tree clean and properly committed to `feat/desktop-tts`
- [x] All documentation updated for full AI autonomy and handoff
