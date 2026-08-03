# Recall Result to Learning Execution Integration

LQ-006G is the Shared Application boundary that decides whether an LQ-006F `RecallResult` may
enter learning state. Recall evaluation is new; commit authority remains singular. Eligible work is
adapted to the existing `ReviewSessionItemUseCase`, which already owns ReviewEvent, MemoryState,
Scheduler/FSRS, Evidence promotion, trajectory persistence, pending recovery, Undo, and queue
advancement.

## Classification and rating intent

`RecallLearningExecutionRequest` binds result, learner/Content/item/session/current queue identity,
evaluation context, policy/version, and optional explicit manual intent. The typed classifier emits
automatic success, automatic lapse, manual user, manual override, no-rating, or practice-local
intent. Partial recall requires manual action; invalid, skipped, and timed-out results do not commit
under the default policy. Reveal follows the established forced-Again consequence without evidence.

Automatic success is mode/evidence aware: exact strong recall may propose Easy, standard recall
proposes Good, and weak/recognition recall proposes Hard. Incorrect recall proposes Again. These are
only proposed intents—the existing Evidence Promotion execution and same transaction decide the
committed rating, retaining spacing, chain, lapse, and promotion authority.

## Evidence and manual routing

Automatic evidence is created only for evaluative, non-revealed correct/incorrect results marked
Standard or Strong. Weak, recognition, ineligible, assisted/revealed, practice, and manual results
cannot append automatic evidence. No promotion windows or weights are duplicated.

Explicit evaluative manual ratings keep `MANUAL_USER` and enter the existing review transaction
without automatic evidence. Practice overrides retain `MANUAL_USER_OVERRIDE` and route through the
existing separate override use case; the enclosing practice session remains practice-only.

## Practice, idempotency, queue, and recovery

Practice-only results route solely through `CompletePracticeItemUseCase`: they update practice-local
progress and advance the deterministic practice loop without ReviewEvent, MemoryState, Scheduler,
trajectory, rating inventory, or normal queue mutation.

Attempt identity deterministically becomes the existing ReviewEvent identity. Duplicate delivery
is detected before stale queue/session checks, so committed attempts cannot create a second event,
Scheduler call, evidence append, or queue advance. Pending review recovery already persists this
event identity and automatic recall facts. The existing atomic transaction and Undo snapshot restore
review, memory/due, trajectory/evidence, session, queue, and therefore the duplicate marker.

The legacy completed typing path and this bridge both converge on `ReviewSessionItemUseCase`; live
typing EMPTY/VALID_PREFIX/INCORRECT/CORRECT behavior is unchanged. No second review transaction,
platform rating mapper, UI callback, Desktop/Compose dependency, filesystem, or platform clock is
introduced. The API is portable to Desktop, Android, iOS adapters, and Web; platform rendering and
new-mode UI remain outside this phase.
