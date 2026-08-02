# Evaluative Manual Rating Contract

Evaluative Manual Evaluation and Practice Manual Rating Override are separate shared application
capabilities. Manual Evaluation is available only for an active `EVALUATIVE` session with a
current item. Practice uses its existing override availability and transaction.

Opening Manual Evaluation explicitly reveals the canonical answer through the established reveal
authority; merely rendering the action never composes answer-side content. Cancel stops after
reveal without review mutation or advancement. Confirm dispatches one normal evaluative review
transaction with the freely selected Again, Hard, Good, or Easy rating.

The transaction writes `RatingSource.MANUAL_USER`, updates ReviewEvent and MemoryState, invokes the
existing Scheduler/FSRS boundary once, advances through normal session flow, refreshes Rating
Inventory, and remains undoable. It never uses `MANUAL_USER_OVERRIDE` or automatic typing evidence.
Desktop renders shared availability, reveals, confirms, and dispatches; it does not calculate due
state or own transaction behavior. Integrated Desktop UAT remains pending.
