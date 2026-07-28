# PLE-030.8 — Answer Dock Restoration Audit

## Missing-dock root cause

PLE-030.7 atomically persisted `introducedContentIds` and `answerRevealed = true`, and
`StudyFacade` correctly published `canReview = true` plus
`ReviewWorkspaceState.AnswerRevealed`. The in-memory `DesktopLearningFlowCoordinator`, however,
still held the same item's stage at `LearningFlowStage.Experience`.

On synchronization it called `confirmAnswerRevealed` from that Experience state. The controller
correctly rejected that legacy-invalid transition, leaving `isRatingReady = false`. Full Answer
rendering was controlled by `canReview`, so answer content appeared; the answer dock required
both `canReview` and `isRatingReady`, so it did not render. Rating callbacks were still wired,
keyboard routing already recognized `AnswerRevealed`, and compact/minimum reserves were
non-zero. The dock was absent rather than clipped.

## Corrected state equivalence

Authoritative `canReview = true` now synchronizes as follows:

- existing Rating Ready remains stable;
- normal pending Answer Reveal is confirmed through the controller;
- a direct authoritative reveal from an Experience state reconstructs the normal revealed
  prefix with `initializeRevealed`.

The resulting Introduction path is behaviorally equivalent to normal reveal:

| Field | Normal reveal | Introduction direct reveal |
| --- | --- | --- |
| persisted answer | revealed | revealed |
| workspace | `AnswerRevealed` | `AnswerRevealed` |
| flow stage | `RatingReady` | `RatingReady` |
| `isRatingReady` | true | true |
| dock mode | `ANSWER_ACTIONS` | `ANSWER_ACTIONS` |
| callbacks | existing Again/Hard/Good/Easy | same |
| keyboard 1–4 | rating actions | same |

No rating transaction state is duplicated. `StudyActionDockMode` centralizes the existing
render conditions: Introduction and front modes remain read-only, Answer Actions renders the
same four callbacks, and hidden remains limited to genuinely unavailable/completed states.
`actionInProgress` does not change dock mode; it disables the existing buttons.

## Keyboard, layout, and restart

Before reveal, workspace state blocks 1–4. After direct reveal, 1/2/3/4 map to
Again/Hard/Good/Easy; the exact regression asserts `3 → REVIEW_GOOD`. Repeated/busy/text-input
guards, Space, Replay, Undo, and Pause remain unchanged.

The dock remains outside the single scrollable center pane. PLE-030.7 comfortable,
compact-height, and minimum-height dock reserves and button sizes are unchanged; footer and
Vietnamese-example reachability are unaffected.

Restart reloads persisted `answerRevealed = true`. A new coordinator already initialized
Rating Ready; the corrected same-item synchronization now produces the identical state before
and after restart.

Scheduler feedback is unchanged and does not own actions. Content identity, counters, queue,
review transaction, scheduler/FSRS, rating mapping, audio, POS/highlighting, and continuous
Review Mode remain unchanged.

Automated verification is complete. Manual UAT remains pending.
