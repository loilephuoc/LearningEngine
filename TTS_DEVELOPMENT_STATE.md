# Learning Engine — TTS Development State

- **Current Active Milestone**: Batch Workflow Polish Completed (All Acceptance Criteria Met)
- **Worktree**: `C:\Users\M72Q\IdeaProjects\LearningEngine-TTS`
- **Branch**: `feat/desktop-tts`
- **Previous Checkpoint**: `7e7a773c45f9ae96c5e0c349af8d971d0efa70b3`
- **Android State**: HARD FROZEN (`git diff --name-only HEAD -- android` = 0 lines)
- **Verified Test Count**: 4,962 tests, 0 failures, 0 errors, 0 skipped
- **Build Status**: `BUILD SUCCESSFUL in 4m 30s`
- **Known Blockers**: None
- **Delivered Capabilities**:
  - Selected-item scope batch workflow.
  - Audio field selection checkboxes (Question, Answer, Example, Translation) with live missing counts.
  - Automatic language mapping (Q/A/Ex -> EN, Tr -> VI).
  - English and Vietnamese voice profiles with rate and pre-generation preview.
  - Fixed voice dropdown layout bug (wide dropdown menu, readable labels, no narrow column wrapping).
  - Target-level batch generation with existing audio protection and empty text skip.
  - Generate != Apply boundary: permanent assets synthesized before atomic apply.
  - Atomic Batch Apply & Undo: captures `BatchTtsUndoSnapshot`, restores exact previous audio references upon Undo, safe cleanup of newly created unreferenced files, existing media files preserved.
  - Full regression pass and Android freeze verified.
