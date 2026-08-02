# Practice Session Contract

LQ-004B defines cross-platform practice as a shared Learning Core capability. Platform clients
render typed state and dispatch intents; they do not select membership, shuffle rounds, mutate
ratings, count inventory, or own transaction rules. Port UI, reuse learning core.

## Membership

- Latest-New practice freezes only committed, completed predecessor queue entries whose persisted
  origin is `NEW`. Current memory stage never replaces predecessor origin authority.
- Again/Hard practice freezes every eligible Content whose latest committed evaluative rating is
  Again or Hard. Normal Study limits do not truncate this set.
- Both modes exclude disabled, suspended, deleted, out-of-scope, and duplicate Content. The
  deterministic representative chosen at start remains fixed until the practice session is left.

## Loop and progress

`PracticeLoopPolicy.LOOP_FIXED_MEMBERSHIP_SHUFFLED` stores fixed membership, deterministic seed,
current permutation, position, and round in the persisted queue. Each boundary creates a complete
Fisher–Yates permutation without growing the queue. When possible, the previous final item is not
the next first item and consecutive orders differ. `PracticeProgress` reports current round and
`position / fixed membership size`; the denominator never accumulates.

Practice completion advances local navigation only. It cannot stage a review, create a
`ReviewEvent`, mutate `MemoryState`, call Scheduler/FSRS, create reinsertion, or finish through the
evaluative completion flow. Restart resumes persisted membership/order/position/round/seed without
replanning. Pause preserves the active session; explicit leave closes it and deletes its queue
atomically without evaluative completion.

## Feedback and manual override

Normal practice answers produce only `PracticeRecallResult` (`CORRECT`, `INCORRECT`, `REVEALED`, or
`ALMOST_CORRECT`). They do not imply Again/Hard/Good/Easy transitions.

Manual Rating Override is a separately confirmed evaluative transaction. It writes exactly one
event with `RatingSource.MANUAL_USER_OVERRIDE`, invokes the existing scheduler once, persists its
memory/due consequence atomically, and leaves the enclosing session `PRACTICE_ONLY`. Undo restores
the prior event/memory state without rewinding practice navigation. Fixed membership is unchanged;
a later newly-created Again/Hard practice reads the new committed rating.

## Rating inventory

`RatingInventoryQuery` projects Again, Hard, Good, Easy, Never Reviewed, and Total from the latest
committed evaluative event per eligible Content in the current learner/package/topic/content scope.
Sibling LearningItems never multiply counts. Practice-local results and pending/uncommitted work
do not affect inventory. Every query is current after rating, override, Undo, import, scope, or
eligibility changes, and enforces that category counts sum to total eligible Content.

Integrated Desktop UAT remains pending.
