# Learning Session Platform Blueprint

## Purpose
The **Learning Session Platform** subsystem orchestrates interactive review sessions, manages active card review queues, integrates the FSRS scheduling algorithm, applies sibling item avoidance rules, and records review history logs.

## Responsibilities
- Construct active review queues for a given Workspace (`SessionQueuePlanner`).
- Manage interactive card presentation rhythms (Prompt -> Reveal -> Rate).
- Invoke the `FsrsScheduler` domain service to compute memory state updates (stability, difficulty, retrievability, due interval).
- Prevent immediate sibling item exposure (sibling avoidance spacing).
- Publish `ReviewCompletedEvent` domain events for Analytics & Review History.

## Out of Scope
- File archive parsing or ZIP handling (handled by Package Platform).
- Direct UI rendering (handled by Presentation Layer ViewModels and Composables).

## Dependencies
- Workspace Platform (`WorkspaceRepository`).
- Domain scheduler (`FsrsScheduler`, `FSRSState`, `Rating`).
- Analytics context (`ReviewHistoryRepository`).

---

## Subsystem Architecture & Components

```text
+-----------------------------------------------------------------------------------+
|                            LEARNING SESSION PLATFORM                              |
|                                                                                   |
|  +------------------------------+             +--------------------------------+  |
|  |     SessionQueuePlanner      |============>|     SiblingAvoidanceFilter     |  |
|  |     (Queue Construction)     |             |     (Card Spacing Manager)     |  |
|  +--------------+---------------+             +---------------+----------------+  |
|                 |                                             |                   |
|                 v                                             v                   |
|  +------------------------------+             +--------------------------------+  |
|  |     StartSessionUseCase      |============>|     ProcessReviewUseCase       |  |
|  |     (Session Initialization) |             |     (Rating & FSRS Update)     |  |
|  +--------------+---------------+             +---------------+----------------+  |
|                 |                                             |                   |
|                 v                                             v                   |
|  +------------------------------+             +--------------------------------+  |
|  |     FsrsScheduler            |============>|     ReviewHistoryRepository    |  |
|  |     (Domain Service)         |             |     (Append-Only Log Store)    |  |
|  +------------------------------+             +--------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## Data Models & State Contracts

### LearningSession Aggregate Root
- `sessionId`: `SessionId`.
- `workspaceId`: Associated `WorkspaceId`.
- `queue`: Active queue of `LearningItem` instances.
- `currentIndex`: Pointer to current card.
- `state`: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`.

### FSRS Memory State
- `stability`: Float value representing memory stability in days.
- `difficulty`: Float value representing item difficulty (1.0 to 10.0).
- `lastReview`: Timestamp of last review.
- `repetitionCount`: Number of completed reviews.

### Rating Enum
- `AGAIN` (1): Complete lapse / failure to recall.
- `HARD` (2): Recalled with significant difficulty.
- `GOOD` (3): Successful recall with standard effort.
- `EASY` (4): Immediate, effortless recall.

---

## Session Workflow Lifecycle
1. **Initialize Session**: `StartSessionUseCase` queries active items from bound Workspace, applies queue planner, and filters sibling items.
2. **Present Prompt**: UI presents item primary prompt (text, image, or audio).
3. **Reveal Answer**: Learner triggers reveal, exposing secondary text/meaning.
4. **Submit Rating**: Learner selects rating (`AGAIN`, `HARD`, `GOOD`, `EASY`). `ProcessReviewUseCase` computes next FSRS state, updates card interval, appends to `ReviewHistoryRepository`, and publishes `ReviewCompletedEvent`.
5. **Session Complete**: Queue empties, summary stats are returned to presentation client.

---

## Future Evolution
- Support for real-time AI Tutor feedback during session execution.
- Adaptive queue dynamic reprioritization based on fatigue metrics.

---

## Architecture Notes
- All scheduling state calculations MUST be 100% deterministic and unit-tested in `src/test/kotlin/`.
