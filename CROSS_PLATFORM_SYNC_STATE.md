# Current State

Branch: `feat/cross-platform-sync`

Current Phase: PHASE 5 — Android Full Localization

Status: IN_PROGRESS

Last completed phase: PHASE 4 — Desktop Backup Completion Dialog

Completed:
- Phase 0 repository/continuity recovery audit.
- Phase 1 Safety Backup is opt-in and user-managed; no automatic creation, retention or deletion.
- Phase 2 restore uses validated staging plus hidden temporary transactional rollback and actionable diagnostics.
- Phase 3 Desktop backup progress uses indeterminate presentation until a real item/byte metric advances, reports checksum file completion, and keeps elapsed time ticking independently of engine callbacks.
- Phase 4 successful-backup dialog is immutable: it shows the verified result and only a Close action; package controls and the start action are absent.
- Physical UAT PASS: Desktop 2-package archive (`Vocabulary_In_Use_Elementary` + `OPD_2nd`) restored on Android with 3,376 Content, 16,880 LearningItems and 16,619 media files.

Current implementation status:
- Desktop Content Studio context-menu Delete now selects the clicked row and invokes the same existing deletion request/confirmation authority as toolbar Delete.
- Desktop TTS capability is complete: multi-selection shows `Generate Audio (N)`, all missing selected-field targets are scanned/run, single and batch share seven speed levels, pitch/volume reach preview and generation requests, and preview uses the displayed selected-field text.
- Study daily New/Review limits now have long-press editors. Apply atomically updates the active session policy and replans/replaces its canonical queue before persisting preferences, then reloads the same session immediately.

Tests:
- Focused active-session queue replan and Android Study suites: PASS.
- `clean test`: Engine 2,255; Android 968; Desktop 1,806; total 5,029; zero failures/errors/skips.
- Android `assembleDebug`: PASS at Phase 2 checkpoint.

Physical UAT:
- Two-package restore baseline PASS.
- Remaining UI phases and their physical checks are pending.

Commit: active-session limits capability follows Desktop TTS checkpoint `8babd4a1`.

Next:
- Complete Android Library/active-package and Study indicator capability.

Blockers: none.

Remaining issues:
- Phase 5 Android full localization.
- Phase 6 Android Library redesign/active package.
- Phase 7 Study active-package indicator.
- Phase 8 Reminder popup controls.
- Phase 9 Reminder image radius.
- Phase 11 global localization audit.
- Phase 12–14 final tests, physical UAT and regression.
