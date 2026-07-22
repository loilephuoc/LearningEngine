# Desktop Gap Analysis

This analysis compares the current Desktop source at HEAD `075b098` with the Android product
reference catalogued in [`ANDROID_PRODUCT_BEHAVIOR.md`](ANDROID_PRODUCT_BEHAVIOR.md). It does
not treat Android as the correctness authority.

## Current Desktop strengths

- `StudySession`, the persisted queue, pending review, one-step undo, recovery, and scheduler
  transaction own learning correctness outside Compose.
- `ReviewWorkspaceState` provides deterministic Idle/Preparing/Question/AnswerRevealed/
  Feedback/Transitioning/Completed/RecoverableFailure projection states.
- `LearningContentPresenter` and `LearningContentRenderer` safely render plain text, an
  allowlisted Markdown subset, local images, examples, and manual local audio.
- `StudyKeyboardShortcut` covers reveal/rating/pause/undo with focus and repeat guards.
- Content Library/Lesson Browser provide import, installed-package discovery, lazy rows,
  Unicode multi-term search, lesson-scoped start, and recoverable asynchronous loading.
- Review History, Statistics, and Dashboard provide durable projections rather than Activity
  counters.

## Gap matrix

| Product area | Source neighborhood | Status | Evidence / gap | Recommendation | Priority |
|---|---|---|---|---|---|
| Prompt/reveal/rate | `ui/study`, engine session/review | Implemented | deterministic and recoverable | Freeze correctness boundary | P0 done |
| Rich text/image/example | content model + renderer | Implemented | safe Markdown and local image paths | Extend only through content roles | P0 done |
| Manual audio | `LearningContentAudio` | Partial | generic play/stop; no channel/sequence model | Add media-role and playback-plan contracts | P1 |
| Topic hierarchy | Content Library/Lesson Browser | Partial | package/lesson list, no section tree | project hierarchy from content metadata | P1 |
| Multi-lesson session | Study facade/planning | Missing | one lesson or global scope | explicit immutable selection plan | P1, PO decision |
| Study preset | Study UI/config | Missing | no vocabulary/listening projection | learner preference over same engine session | P1, PO decision |
| Sequential/auto listening | media + Study | Missing | no timed presentation coordinator | opt-in, pauseable, never auto-rate | P1, PO decision |
| Audio loop/delay/voice | media adapter | Partial | manual audio only | typed per-session playback preference | P1 |
| Optional typing | learning-content/application | Missing | no response/evaluation contract | separate exercise mode, not Compose logic | P2, PO decision |
| Favorites | domain/content library | Missing | no durable learner-content relation | add only with sync/migration semantics defined | P2, PO decision |
| Daily goal | progress/dashboard/config | Missing | session counts exist, no goal | ethical goal contract with no punishment | P2, PO decision |
| In-study quick search | Study/Library | Missing | full Library search requires navigation | command-palette overlay using query ports | P2 |
| Previous card | session/queue | Missing intentionally | arbitrary rewind can violate review semantics | history preview or undo-only, not rerating | P2, PO decision |
| Gestures | Desktop input | Missing | keyboard accessible path exists | optional configurable pointer gestures | P3, PO decision |
| Scratch note | Desktop state | Missing | no note model | ephemeral first; durable note needs product decision | P3, PO decision |
| Live compact counters | Study progress | Partial | progress exists, Android-like top status absent | reduce to calm session goal/progress | P2 |
| Content editing in study | Library/import | Should retire | conflicts with focused session | keep authoring outside Study | — |
| Android scheduler/queue | core engine | Should retire | engine is more explicit/tested/atomic | never port | — |
| Lock-screen/device admin | Android platform | Should retire | no Desktop learner value | never port | — |

## Regression risks

- Adding playback automation inside `StudyScreen` would duplicate lifecycle ownership and risk
  advancing after pause, completion, or recovery.
- Treating a media end callback as a rating would create scheduler and persistence drift.
- Reusing Android daily counters instead of query projections would create a second source of
  truth.
- Allowing previous/next to mutate a persisted queue without an application command would
  break deterministic restart and one-step undo.
- Persisting focus, animation, expanded controls, or transient playback position in
  `StudySession` would violate the established recovery contract.
- Loading original media eagerly would regress the newly verified large-package boundary.

## Product-owner decisions

1. Is the first new outcome **Listening Session** (recommended) or a generic study-mode
   framework?
2. Can a session select multiple lessons, and if so is order learner-defined or package order?
3. Is typing optional per session, per lesson, or a content-authored requirement?
4. Should favorites be durable learner data in Desktop 1.x?
5. May auto mode reveal automatically, and must every rating remain manual (recommended: yes)?
6. Should session goals be informational only, with no streak loss or punitive messaging
   (recommended: yes)?
7. Is previous-card access read-only history, or should undo remain the only backward mutation
   (recommended: undo only)?

## Acceptance boundary

The next product capability must preserve the engine as authority, introduce one complete
learner outcome through application and Desktop composition, add deterministic/restart/media
failure coverage, and avoid a generic framework until at least one accepted use case needs it.
