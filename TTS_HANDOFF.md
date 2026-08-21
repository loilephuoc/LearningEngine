# Learning Engine — TTS Autonomous Handoff

## Quick State Snapshot
- **Worktree**: `C:\Users\M72Q\IdeaProjects\LearningEngine-TTS`
- **Branch**: `feat/desktop-tts`
- **Phases Completed**: Phase 1 (PASS), Phase 2 (PASS), Phase 3 (PASS), Phase 4 (PASS), Phase 5 (PASS), Phase 6 (PASS), Phase 7 (PASS)
- **Status**: ALL PHASES COMPLETE

## Key Architecture References
- **TTS Service**: `vn.loi.learning.desktop.tts.DesktopTtsAudioService`
- **TTS Engine**: `vn.loi.learning.desktop.tts.EdgeTtsEngine` (WebSocket edge-tts)
- **Audio Storage**: `vn.loi.learning.application.port.ContentMediaStorage`
- **Single-Item TTS UI**: `vn.loi.learning.desktop.tts.ui.DesktopTtsDialog`
- **Batch TTS Scanning & Models**: `vn.loi.learning.desktop.tts.batch.BatchTtsScanner`, `BatchTtsModels`
- **Batch TTS Execution**: `vn.loi.learning.desktop.tts.batch.BatchTtsRunner`
- **Voice Profiles & Preferences**: `vn.loi.learning.desktop.tts.profile.TtsVoiceProfile`, `TtsVoiceProfiles`, `TtsVoiceProfilePreferencesStore`
- **Batch TTS Dialog (Progress, Cancel, Retry)**: `vn.loi.learning.desktop.tts.ui.BatchTtsDialog`
- **Content Studio UI**: `vn.loi.learning.desktop.ui.studio.ContentStudioScreen`
- **Content Explorer UI**: `vn.loi.learning.desktop.ui.studio.ContentExplorerPane`
- **Content Persistence**: `vn.loi.learning.desktop.ui.contentlibrary.ContentLibraryViewModel.applyGeneratedTtsAudio`
- **Android Policy**: HARD FROZEN. `android/**` remains 100% untouched.
