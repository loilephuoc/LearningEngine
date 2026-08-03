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

ANDROID-001 renders Typing only. Audio/image rendering, Android document-picker import,
backup/restore UX, and additional Recall modes require later bounded capabilities. Their adapters
must continue using existing ports and authorities rather than parallel learning implementations.
