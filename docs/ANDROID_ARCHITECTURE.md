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

ANDROID-003 adds the platform content and lifecycle boundary. `OpenDocument`/`CreateDocument` and
`ContentResolver` own selected-document access; no broad storage permission or hard-coded public
path is used. Selected packages and restores are copied into operation-scoped app-private staging,
then the existing package importer remains validation and transaction authority. Streams and
staging are closed or removed on every result.

Operation IDs are one-shot. `SavedStateHandle` stores only an active ID/kind, and recreation turns
an unresumable operation into a typed interruption instead of replaying it. The shared JVM `.lebak`
inventory/checksum recovery adapter backs Android backup/restore over durable data and media roots;
restore validates before mutation, creates a safety snapshot, and rolls back on failure.

Media references remain opaque Shared strings resolved to app-private resources. Audio prepares
asynchronously and releases on replay, disposal, completion, or error. Images decode on the I/O
dispatcher with bounded sampling and aspect fit. Physical phone/tablet providers, real media,
process kill, rotation, accessibility, low storage, and large-package performance remain manual gates.

ANDROID-004 adds a pure Compact/Medium/Expanded layout policy, bounded centered content, safe/IME
insets, item-keyed focus, localized semantics, and cancellable document-operation Back behavior.
See `ANDROID_UI_ACCESSIBILITY_CONTRACT.md`. Shared learning and session authority are unchanged.

ANDROID-005 adds `AndroidLibraryFacade`/`AndroidLibraryViewModel` over canonical Library and Package
Browser Application services. Compose owns lazy list/detail navigation only; criteria and command
semantics remain Application-owned. See `ANDROID_LIBRARY_WORKSPACE.md`.
