# PLE-030.4 Content Identity Audit

## Confirmed UAT contradiction

The failing path was not primarily schema-v1 recovery:

- `ContentStageQueryService.resolveContentStage` grouped sibling LearningItems by Content and
  could display REVIEW.
- `StudyQueuePlanner` and `StudyQueuePlanEntry.isNew` classified the current LearningItem from
  only its own MemoryState.
- `ReviewSessionItemUseCase` trusted that item-level queue origin for counters.
- `StudyHeaderStatisticsQueryService` aggregated latest events by LearningItem.
- `StudyFacade.toUiState` queried history only for the current LearningItem.

Thus a reviewed Meaning item and unseen Listening sibling produced REVIEW presentation but NEW
origin/counter, a second learned Total, and no previous-rating underline.

## Durable identity map

| Concern | Authority |
| --- | --- |
| Scheduler, MemoryState, review transaction/event, mode and experience | `LearningItemId` |
| Learner-facing NEW/REVIEW, quota, counters, Total/buckets, previous rating | `ContentId` |

Sibling LearningItems are neither merged nor removed.

## Corrected authority

`ContentLearningStateQueryService` resolves one immutable projection for
`LearnerId + ContentId`: sibling IDs, learned state, latest effective ReviewEvent/rating and
review time. It performs one batch sibling lookup and one authoritative learner-event read for
multi-content resolution. Repository order is ascending and stable; the last applicable event
wins, including deterministic same-time insertion order.

The planner projects a learned Content's technically NEW sibling as learner-facing REVIEW only
inside candidate selection. Persisted MemoryState remains NEW until the exact LearningItem is
reviewed. Fresh queues persist `{LearningItemId, ContentId, SessionItemOrigin}`.

## Quota, counters and remaining workload

`SessionPolicyLimiter` counts unique Content identities while retaining legitimate sibling
entries already admitted under that content workload. `StudySession.reviewedItemIds` continues
to track technical occurrences; `reviewedContentIds` owns the first learner-facing completion.
New or Review counters therefore increment at most once per Content in a session.

Header remaining workload groups queue entries by Content. A completed event makes any remaining
sibling REVIEW even when an older queue persisted NEW. The existing anti-repetition policy skips
same-Content siblings in one session; Continue Learning may select the unseen sibling as REVIEW.

## Total, rating and previous context

Header projection maps every eligible LearningItem to Content, selects the last authoritative
event per Content, and counts that Content once in Total and exactly one rating bucket. Rerating a
sibling moves the Content's bucket without increasing Total.

Desktop requests the same content projection outside Compose. REVIEW underlines the matching
latest Content rating; NEW and missing event history have no indicator. Scheduler diagnostics
may still show current LearningItem stage NEW.

## Undo and persistence

Undo already stores exact `contentId`, item/event/memory before-state, reviewed Content set and
counters. Removing the latest sibling event makes the content projection fall back to the prior
sibling event and restores its bucket/underline. Queue rewind restores Review remaining.

Queue schema v3 adds item-to-content IDs while preserving order/index/origin. Readers accept
v1/v2 with empty content maps; runtime batch mapping repairs learner-facing semantics without
resetting active sessions or deleting history.

## Performance and compatibility

Planning and header queries batch LearningItems and scan the learner event stream once, avoiding
per-item N+1 calls. Current-item context is resolved only at application/Desktop state
projection, never during Compose recomposition or per rating button.

Existing public constructors retain defaults. Single-LearningItem Content behavior is
unchanged. Scheduler/FSRS, rating mapping, event payload, modes, audio, keyboard, highlighting,
POS and dashboard design are untouched. Continuous Review Mode remains out of scope.
