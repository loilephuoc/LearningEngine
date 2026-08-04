# Android Architecture

Android and Desktop are peer clients:

```text
Android Compose -> AndroidStudyViewModel -> AndroidStudyFacade
                                      -> LearningEngine
                                      -> Shared Application and Domain
Desktop Compose ----------------------^
```

Android owns presentation, navigation, lifecycle collection, transient `UiState`, events,
commands, saved session identity, and platform filesystem paths. `AndroidApplicationGraph` reuses
the existing persisted application factory, content-media storage port, and package importer.

Shared Application/Domain owns capability resolution, RecallMode and direction selection, plan
construction, prefix/correctness evaluation, rating and learning execution, Scheduler/FSRS,
Evidence, Practice isolation, queue advancement, persistence transactions, and duplicate safety.

ANDROID-002 renders Typing, Multiple Choice, Listening, Image Recall, and Example Completion from
the exact `RecallPlan` subtype. `AndroidAudioController` uses platform `MediaPlayer` only for replay;
image decoding consumes the resolved existing media path. Missing or failed media stays a disabled,
semantic UI state and never changes mode or answer authority.

Home exposes engine-backed Review, latest-session Practice, Again/Hard Practice, learned-item
Review, and Resume. Adaptive reinforcement, dynamic difficult membership, manual SRS override,
Undo, completion, and persisted continuation remain Application/Domain behavior. Android saves only
the active session ID needed to reconnect after navigation, configuration change, or recreation.

Physical-device media, document-picker import, backup/restore UX, and process-death UAT remain
separate validation/integration work; they must reuse existing ports rather than parallel learning
implementations.
