# Learning Engine — TTS Autonomous Handoff

## Quick State Snapshot
- **Worktree**: `C:\Users\M72Q\IdeaProjects\LearningEngine-TTS`
- **Branch**: `feat/desktop-tts`
- **Phases Completed**: Phase 1 through Phase 11 (ALL PHASES PASS)
- **Status**: 100% COMPLETE & VERIFIED

## Architecture & Code Boundaries
- **TTS Service**: `vn.loi.learning.desktop.tts.DesktopTtsAudioService`
- **TTS Engine**: `vn.loi.learning.desktop.tts.EdgeTtsEngine` (WebSocket edge-tts)
- **Audio Storage**: `vn.loi.learning.application.port.ContentMediaStorage`
- **Voice Strategies**: `vn.loi.learning.desktop.tts.strategy.TtsVoiceStrategy`, `VoiceStrategyMode`, `VoiceStrategyConfig`, `VoiceAttempt`
- **Single-Item TTS UI**: `vn.loi.learning.desktop.tts.ui.DesktopTtsDialog`
- **Batch TTS Scanning & Models**: `vn.loi.learning.desktop.tts.batch.BatchTtsScanner`, `BatchTtsModels`
- **Batch TTS Execution (Generate != Apply with Fallback)**: `vn.loi.learning.desktop.tts.batch.BatchTtsRunner`
- **Voice Profiles & Preferences**: `vn.loi.learning.desktop.tts.profile.TtsVoiceProfile`, `TtsVoiceProfiles`, `TtsVoiceProfilePreferencesStore`
- **Batch TTS Dialog (Strategy, Field Selection, Preview, Progress, Cancel, Retry, Apply)**: `vn.loi.learning.desktop.tts.ui.BatchTtsDialog`
- **Batch Apply & Undo State**: `vn.loi.learning.desktop.tts.batch.BatchTtsUndoSnapshot`, `BatchTtsUndoEntry`, `ContentLibraryViewModel.applyBatchTtsAudio`, `ContentLibraryViewModel.undoLastBatchTts`
- **Content Studio UI**: `vn.loi.learning.desktop.ui.studio.ContentStudioScreen`
- **Content Explorer UI**: `vn.loi.learning.desktop.ui.studio.ContentExplorerPane`
- **Android Policy**: HARD FROZEN. `android/**` remains 100% untouched.
