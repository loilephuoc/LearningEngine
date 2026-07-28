# PLE-030.1 Session Semantics Audit

Baseline: `8ec96766488e6d182c81a6ef258498fd9bd6494b`, 2026-07-28.

## Current authorities

1. `StudyQueuePlanner` classifies candidates as New/Review, applies scheduler due eligibility,
   strategy, diversity and balancing.
2. `SessionPolicyLimiter` caps classified entries using the session policy's New/Review limits.
3. `StudyQueuePlan` and persisted `StudyQueueSnapshot` retain unique planned item identities and
   order, but not the original `isNew` flag.
4. `StudySession.policy` is an immutable session snapshot of configured limits.
5. `StudySession.newItemsReviewed` and `reviewItemsReviewed` are committed counters owned by the
   review transaction; `reviewedItemIds` prevents same-session duplication.
6. `StudyQueueProgress.remainingLearningItemIds` is the exact remaining planned workload.
   Remaining classification is recoverable from persisted review-event presence: an uncompleted
   planned New item has no event, while a planned Review item does.
7. Undo restores the session counters/identity sets and rewinds the exact queue item.
8. Session completion is queue exhaustion, not reaching configured maxima. Therefore a 6-item
   effective New workload completes even when the configured target is 20.
9. Continue Learning purges the finished session and calls `startStudy()`, producing a fresh
   session, policy snapshot and queue without carry-over counters.

## Review Mode audit

There is no dedicated continuous Review Mode. General Study uses the configured queue strategy
(default `REVIEW_FIRST`), scheduler eligibility, `PriorityOrdering`, content diversity and
optional balancing. It prioritizes Review candidates over New candidates, but it does not
implement the requested infinite Due → Again → Hard → Good → Easy fallback loop. The Review
navigation destination is review history, not an active review queue.

A dedicated Review Mode requires a separate product capability because it changes queue
lifecycle, fallback eligibility after Due exhaustion and continuation semantics. PLE-030.1 does
not alter that behavior.
