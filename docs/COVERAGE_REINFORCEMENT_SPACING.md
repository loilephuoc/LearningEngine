# Coverage Reinforcement Spacing

Coverage Review owns one immutable reinforcement state per `LearningItemId` inside its persisted
`StudyQueueSnapshot`. `CoverageReinforcementPolicy` is the sole spacing authority:

- Again: gaps 2, 8, 20, 40; maximum four scheduled reinforcements.
- Hard: gaps 4, 12, 30; maximum three scheduled reinforcements.
- Good and Easy: no reinforcement.

The policy also prevents a required gap from being compressed near the end of the queue. Such an
attempt is marked deferred and is not inserted. Decisions depend only on session-local history and
queue position, never wall-clock time.

The queue persists the per-item count, previous gap, last insertion index, deferred flag, and one
typed Undo checkpoint. Completion checkpoints retain a discarded pending tail so Undo and restart
restore deterministic queue and reinforcement state. Legacy queue schemas restore with empty
reinforcement state. Practice queues do not call this path.
