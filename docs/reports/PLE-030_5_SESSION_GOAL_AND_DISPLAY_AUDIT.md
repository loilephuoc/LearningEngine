# PLE-030.5 — Session Goal and Display Audit

## Rating dock

`QUESTION_CONTEXT` renders four non-clickable segments from immutable review context. NEW has
equal neutral emphasis; REVIEW underlines and semantically emphasizes only the latest rating.
`ANSWER_ACTIONS` retains existing callbacks and shortcut authority. Rating keys remain gated by
`isRatingReady`, and Space remains reveal/stage authority.

## Display environment

The previous cache used logical Dp bounds and content traits only. Density or font scale could
therefore change without invalidation when Dp bounds remained equal.
`StudyDisplayEnvironment(widthDp, heightDp, density, fontScale)` is now the immutable cache input,
and `StudyVisualLayoutResolver` remains the sole responsive authority. Scroll state is retained;
a shorter viewport reduces the scrollable body while the fixed dock stays unobstructed.

## Session planning

The baseline planner loads all enabled candidates before limiting; it does not truncate to the
target before Content grouping. The missing contract was durable separation of configured
targets, effective unique-Content workload, and technical queue length.

The exact fixture has 80 unseen Content: the first 13 have four siblings and the remaining 67
have three (253 NEW candidates), plus 20 review Content with three siblings each (60 REVIEW
candidates). Targets New 50 and Review 200 select 50 unique NEW Content represented by 163
technical entries and all 20 REVIEW Content represented by 60 technical entries. Taking the
first 50 technical entries would expose only 13 unique Content; the full-list scan prevents it.

Queue schema v4 persists configured/effective workloads. Underfill derives as
`ELIGIBLE_INVENTORY_EXHAUSTED`; genuine 13-of-50 inventory records effective workload 13.
Restart restores the plan; Continue Learning creates a fresh plan. Technical progress is labelled
“technical experience”; dashboard and summary remain Content-level authorities.

FSRS, scheduler formulas, rating mapping, review events, Content identity, Undo, keyboard/audio,
diversity, and ordering are unchanged. Schemas v1-v3 remain readable. Manual UAT is pending.
