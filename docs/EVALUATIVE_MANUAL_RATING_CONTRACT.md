# Evaluative Rating Dock Contract

For every active item in an `EVALUATIVE` session, the Again, Hard, Good, and Easy Decision Dock
controls are direct committed-rating actions on both the concealed front and revealed answer.
Shared `EvaluativeRatingAvailabilityResolver` owns availability; Desktop only renders the enabled
dock and dispatches the selected rating.

A front-side intent first resolves the established canonical-answer reveal authority, then commits
the selected rating through the normal atomic review transaction. The transaction writes exactly
one ReviewEvent with `RatingSource.MANUAL_USER`, updates MemoryState and Rating Inventory, invokes
Scheduler/FSRS once, advances the queue once, and remains recoverable through Undo. The action has
no confirmation dialog; duplicate UI intents remain guarded by the existing in-progress boundary.

`PRACTICE_ONLY` remains different: dock 1–4 records practice-local recall feedback without a
ReviewEvent or Scheduler invocation. Only the separate confirmed `Đổi đánh giá` operation mutates
a prior committed rating, using `MANUAL_USER_OVERRIDE`. Automatic typing evidence retains its
existing provenance. Integrated Desktop UAT remains pending.
