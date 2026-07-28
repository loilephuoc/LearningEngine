# PLE-030.3 Session Classification Audit

## Root cause

`StudyQueuePlanner` produced `StudyQueuePlanEntry.isNew`, but `SessionPolicyLimiter` reduced the
plan to learning-item IDs. `StudyQueuePlan`, the runtime queue and the persisted queue therefore
lost the admission classification. Later consumers independently reconstructed NEW/REVIEW from
mutable evidence:

- review counters used `reviewEvent.stateBefore.reviewCount`;
- header remaining workload used review-event existence;
- the Desktop had no admission origin or previous-rating context.

Those reconstructions can disagree with the frozen plan after rerating, restart or recovery.

## Authority before remediation

```text
planner isNew ──lost──> queue IDs
                         ├─ review stateBefore -> session counters
                         ├─ event existence -> header New/Review
                         └─ Desktop -> no previous-rating cue
```

## Authority after remediation

```text
planner entry
    -> policy-limited entry
    -> immutable SessionItemOrigin
    -> StudyQueuePlan
    -> StudyQueueSnapshot
    -> schema-v2 StudyQueueRecord
       ├─ review transaction -> exactly-once session counter
       ├─ Undo/restart -> same queue origin
       ├─ header -> remaining New/Review partition
       └─ NextSessionItem -> immutable Desktop review context
                              -> one matching previous-rating underline
```

`StudyQueuePlanningService` is the classification authority at admission. Persistence and
projections transport that fact; they do not reclassify it.

## Compatibility

- Existing `SessionPolicyLimiter.apply(...)`, queue constructors and `NextSessionItem` callers
  remain source-compatible through defaults/delegation.
- `StudyQueueRecord` schema v2 stores origins. Schema v1 remains readable with an empty origin
  map; legacy runtime paths fall back to the pre-existing next-item/state/event inference.
- Rating labels, order, shortcuts, callbacks, enabled behavior, 64dp height, scheduler/FSRS,
  review evidence and transaction membership are unchanged.
- The underline uses the existing text style and semantic accessibility; it adds no color token
  or layout size.
- `StudyVisualLayoutResolver` remains the only viewport authority. It now reserves the statistics
  dashboard/header and rating dock before bounding answer media; scrolling remains the long-
  content fallback.

## Explicitly out of scope

No continuous Review Mode, scheduler change, queue-order change, new token, component redesign,
package migration or business-policy change was introduced. Manual visual and interaction UAT
remains required.

## PLE-030.4 addendum

Manual UAT showed that PLE-030.3 persisted origin correctly but at the wrong learner-facing
granularity. A reviewed sibling and an unseen current LearningItem could still disagree.
PLE-030.4 supersedes that narrow rule with Content-level learned state while retaining the
PLE-030.3 persistence, Undo and underline mechanics. See
`PLE-030_4_CONTENT_IDENTITY_AUDIT.md`.
