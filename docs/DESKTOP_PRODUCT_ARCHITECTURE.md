# Desktop Product Architecture

This architecture turns product behaviors into small ownership boundaries. It extends the
current layered system; it does not replace the learning engine or port
`LockScreenActivity`.

## Target interaction flow

```text
Library / Session Setup
        │ immutable scope + learner preferences
        ▼
Application Session Plan ───────► existing queue/session/scheduler authority
        │
        ▼
Review Workspace Projection
        ├── Content presentation
        ├── Media session
        ├── Input router
        └── Progress projection
```

Only application commands may change durable session state. Content and media callbacks can
request a presentation transition; they cannot rate, schedule, advance a persisted queue, or
write learner evidence.

## Subsystems

### Session setup

Owns learner-visible scope (library/package/section/lesson selection) and a study preset. It
creates an immutable request for existing session planning. It does not implement queue
selection. Multi-lesson scope requires an explicit application value and deterministic order.

### Review workspace

Continues to project `StudySession`, current item, reveal, progress, feedback, completion,
failure, and undo. It remains a state machine; Compose renders the state and dispatches allowed
actions. New presentation modes add projection data, not lifecycle states unless an
application invariant genuinely changes.

### Learning-content presentation

Projects question, answer, example, image, and audio blocks. Future bilingual/audio roles are
optional metadata on content or application projection. Missing/unsafe assets remain explicit
unavailable blocks. No Android WebView, HTML execution, or direct package-path access is
introduced.

### Media session

A Desktop-scoped coordinator owns one active playback session, channel/voice selection,
sequence, loop delay, stop-on-transition, and device failures. A platform adapter owns actual
playback. Media state is transient; learner preferences may be typed config. Automatic
playback never implies a review rating.

### Input router

Maps keyboard, pointer, and optional gestures to the same workspace actions after checking
focus, repeat, modal, and action-in-progress guards. Every gesture has visible/button and
keyboard parity. Volume keys and Android controller-specific mappings are not ported.

### Progress and goals

Reads existing session/history/dashboard projections. A future goal is a typed learner
preference evaluated against authoritative review data; it is not a mutable counter stored by
the screen. Goal presentation is informational and non-punitive.

### Content discovery and curation

Content Library owns hierarchy/search and session entry. Quick search may reuse query ports in
an overlay. Favorites require a durable learner-content relation and migration decision; they
must not be a field written back into imported package content.

## Architecture decisions

### ADR-DP-01 — Keep learning correctness in the existing engine

- **Context:** Android combines SRS arrays, queue heuristics, rating counters, and file writes
  in one Activity; Learning Engine has tested scheduler, queue, transaction, recovery, and undo.
- **Decision:** Android learning algorithms are not ported. Desktop uses existing application
  commands and projections.
- **Alternatives:** reproduce Android behavior; add a Desktop scheduler.
- **Consequence:** UX can evolve without two correctness authorities.
- **Compatibility:** no persisted/API change.
- **Tests:** every new flow retains scheduler/session/restart regression coverage.

### ADR-DP-02 — Model modes as session presentation preferences first

- **Context:** vocabulary/listening presets mostly alter visible content and playback.
- **Decision:** a preset configures presentation/media over the same authoritative session.
  It becomes domain policy only if accepted requirements change selection or evidence.
- **Alternatives:** new session statuses; separate Android-style Activity.
- **Consequence:** small vertical listening increments are possible without lifecycle forks.
- **Compatibility:** legacy sessions recover with default preset.
- **Tests:** projection/restart defaults and no queue/scheduler drift.

### ADR-DP-03 — Separate media orchestration from playback adapter

- **Context:** current renderer owns manual Java Sound playback; Android interleaves players,
  loops, timers, settings, and card advancement.
- **Decision:** orchestration is a Desktop application-facing component; Java Sound remains an
  adapter. It receives content identity and workspace transitions and is always stoppable.
- **Alternatives:** add timers to Compose; put playback in domain.
- **Consequence:** deterministic fake-clock/player tests and platform replacement become
  possible.
- **Compatibility:** manual generic audio remains the fallback.
- **Tests:** cancellation, stale callback, missing asset, failure, pause, transition, completion.

### ADR-DP-04 — Persist only learning truth and accepted preferences

- **Context:** Android saves selection, counters, layout controls, note position, loop state,
  and platform flags through multiple preference files.
- **Decision:** session/current item/reveal/pending action/queue/undo remain durable learning
  truth. Window/focus/animation/expanded panel/playback position remain outside it. Stable
  learner preferences use typed Desktop config.
- **Alternatives:** serialize the entire workspace; store all state in session records.
- **Consequence:** restart remains deterministic and schema growth intentional.
- **Compatibility:** optional config defaults preserve old profiles.
- **Tests:** corrupt config preservation and legacy session restart.

### ADR-DP-05 — Multi-lesson scope is an application contract

- **Context:** Android filters mutable sentence lists; the engine already accepts immutable
  content scope and creates a persisted queue.
- **Decision:** if approved, represent selected lesson/content IDs in an application request,
  validate them before mutation, and let existing planning order the queue.
- **Alternatives:** concatenate UI lists; mutate queue from Desktop.
- **Consequence:** scope is testable and restart-safe.
- **Compatibility:** existing single-lesson/global calls remain.
- **Tests:** ordering, duplicates, missing content, restart, sibling isolation.

### ADR-DP-06 — Typed responses need an exercise-evaluation boundary

- **Context:** Android normalizes strings and can gate ratings in the Activity.
- **Decision:** future typing uses a deterministic evaluator and explicit tolerance policy;
  Desktop owns input only. Typed responses do not rewrite imported content.
- **Alternatives:** compare in Compose; copy Android normalization.
- **Consequence:** language rules become testable and reviewable.
- **Compatibility:** absent exercise metadata yields the current reveal/rate flow.
- **Tests:** Unicode, punctuation, locale, blank answer, hint/reveal, restart non-persistence.

### ADR-DP-07 — Backward navigation cannot bypass undo

- **Context:** Android offers previous/next browsing while Learning Engine guarantees one-step
  undo and append-only review evidence.
- **Decision:** reviewed content may be shown read-only from history; the only backward
  mutation remains application-owned one-step undo.
- **Alternatives:** arbitrary rerating; mutable queue index from UI.
- **Consequence:** audit and scheduler state remain coherent.
- **Compatibility:** current undo unchanged.
- **Tests:** history view has no rating action; undo remains atomic/idempotent.

### ADR-DP-08 — Do not port platform-specific lock behavior

- **Context:** device admin, lock-screen overlay, volume keys, notifications, and Android
  services dominate parts of the reference.
- **Decision:** retire them for Desktop. Desktop startup/window/runtime boundaries stay as-is.
- **Alternatives:** simulate a lock screen or global hooks.
- **Consequence:** lower security/accessibility risk.
- **Compatibility:** none.
- **Tests:** none beyond existing Desktop lifecycle.

## Non-durable state

Playback position, active animation, pointer gesture progress, focus, scroll, expanded panels,
temporary typing input, hint visibility, transient scratch note, and media error banners are
not session data. A future durable note or favorite requires a distinct product/data contract.

## Composition rule

New subsystems are wired at `DesktopApp`/Desktop composition roots using constructor injection.
Domain/application modules never depend on Compose, Java Sound, timers, filesystem paths, or
Desktop configuration. Infrastructure may implement application ports; Desktop may adapt those
ports into projections.

## Compatibility and failure behavior

- Optional new fields default to current behavior for old records/packages/config.
- Unknown/unsafe media remains unavailable rather than executable.
- A media failure stops only media and exposes recovery; it cannot fail or duplicate a review.
- Session setup validates scope before session creation.
- Stale async/media callbacks carry identity/revision and are ignored.
- Every durable multi-write stays inside the existing transaction boundary.

## Architecture acceptance

An implementation conforms when the learner outcome is reachable from real Desktop
composition, all durable mutation crosses existing application commands, restart is
deterministic, media/input failures cannot alter scheduler evidence, and old profiles/packages
retain current behavior.
