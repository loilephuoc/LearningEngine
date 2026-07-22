# Learning Interaction Catalog

## Contract

Every platform exposes the same action meaning, permission, mutation boundary, failure
behavior, and recovery result. Input mechanisms may differ. “Transient” means no durable
learning mutation; “Engine” means the action crosses an application command/query boundary.

| ID | Interaction | Available when | Result / mutation | Failure and recovery | Required parity |
|---|---|---|---|---|---|
| IX-001 | Start Learning | valid scope, no conflicting active session | Engine creates one session/queue | stay in setup; no partial session | button, keyboard/touch equivalent |
| IX-002 | Resume | active recoverable session exists | Engine returns same session/current item | explicit safe recovery outcome | prominent entry action |
| IX-003 | Pause/Leave | any active presentation phase | transient leave; session remains ACTIVE | no mutation inferred | Escape/Back and visible action |
| IX-004 | Reveal | Thinking | Engine persists reveal checkpoint; answer visible | keep prompt, Retry | Enter/Space/tap button |
| IX-005 | Hint | authored hint exists before reveal | transient progressive help | omit unavailable hint | button and keyboard traversal |
| IX-006 | Play prompt audio | visible playable prompt audio | transient playback | local unavailable state | visible media button |
| IX-007 | Play answer audio | answer revealed | transient playback | local unavailable state | visible media button |
| IX-008 | Play example audio | example revealed | transient playback | local unavailable state | visible media button |
| IX-009 | Replay/Stop | selected asset active/known | transient player action | release and allow retry | button; media key optional |
| IX-010 | Show/enlarge image | image block visible | transient presentation | unavailable block | pointer/touch/keyboard |
| IX-011 | Hide/show translation | answer visible and preference supported | transient or typed preference; never content mutation | preserve current answer | labeled toggle |
| IX-012 | Submit typed answer | Typed Recall input valid | evaluator result only; no review | permit reveal | Enter and submit button |
| IX-013 | Show answer from typing | Typed Recall before rating | reveal normal answer | Retry reveal | visible escape path |
| IX-014 | Again | revealed, no action pending | Engine atomic review with Again | keep card revealed; Retry | key 1 + button/touch |
| IX-015 | Hard | revealed, no action pending | Engine atomic review with Hard | same | key 2 + button/touch |
| IX-016 | Good | revealed, no action pending | Engine atomic review with Good | same | key 3 + button/touch |
| IX-017 | Easy | revealed, no action pending | Engine atomic review with Easy | same | key 4 + button/touch |
| IX-018 | Undo | latest committed rating is undoable | Engine atomic one-step reversal | idempotent unavailable/result | Ctrl+Z + visible action |
| IX-019 | Open History | outside commit transition | query-only history view | retain workspace projection | navigation action |
| IX-020 | Previous reviewed item | history context only | read-only projection | no rerating; Undo is sole mutation | standard navigation |
| IX-021 | Next item | after committed feedback | Engine/session projection chooses next | never skip by UI index mutation | usually automatic; explicit continue optional |
| IX-022 | Select topic/lesson | Library/setup | transient selection | retain selection on load error | list/tree/touch equivalents |
| IX-023 | Pin/unpin scope | durable feature approved | learner-organization mutation | atomic rollback | contextual action |
| IX-024 | Favorite/unfavorite | durable contract approved | learner-content relation mutation | never modify package content | contextual action |
| IX-025 | Search content | Library or approved overlay | query-only projection | clear/reset recovery | Ctrl+F + search control |
| IX-026 | Choose Standard preset | setup | setup preference | fallback Standard | selectable option |
| IX-027 | Choose Listening preset | supported setup | setup/media plan, same engine authority | warn/fallback without audio | selectable option |
| IX-028 | Choose Typed Recall | supported exercise | setup/evaluator projection | fallback normal reveal | selectable option |
| IX-029 | Start Auto/Listening flow | explicit configured plan | transient media/presentation plan | stop safely | visible Start/Stop |
| IX-030 | Skip media step | active playback plan | next media step only | no queue/rating mutation | visible control |
| IX-031 | Stop auto flow | active plan | cancel timers/player; session unchanged | immediate safe stop | Escape/Stop |
| IX-032 | Complete/Done | completed session | navigate away; session remains finished | summary remains recoverable | primary action |
| IX-033 | Continue another scope | summary | returns to selection/setup | never silently reuse finished queue | explicit action |
| IX-034 | Retry recoverable action | classified failure | repeats same idempotent boundary | preserve original context | visible primary recovery |

## Rating safety

The four rating controls have stable order, names, meanings, shortcuts, and semantic grouping.
Input repeat, double-click, stale callback, and concurrent pointer/keyboard activation are
guarded while commit is in progress. Visual feedback before commit may acknowledge input but
must not show a false saved result or advance content.

## Navigation safety

Back/Escape first closes transient overlays, then leaves the workspace while preserving the
ACTIVE session. It never maps to Again, skip, finish, or delete. Starting a new scope while
active requires resolving the active session explicitly.

## Auto and listening safety

Auto/Listening actions operate only on media and presentation steps. They may play, wait,
reveal only under an approved policy, repeat, or stop. They cannot rate, edit queue order,
increment progress, or persist playback position as learning truth.

## Gesture policy

Gestures are optional platform accelerators. Their direction/action mapping is visible in
settings/help, disabled by default for destructive rating unless approved, offers cancellation
before commit where practical, and always has accessible button/keyboard parity. Android volume
keys and controller-specific mappings are reference behaviors, not universal contracts.

## Focus and modal policy

Interactions dispatch only from the active surface. Text entry and dialogs consume relevant
keys. Modal confirmation is required for destructive package/library actions, never for routine
Reveal or rating. After a modal closes, focus returns to its invoker.

## Open decisions

- whether card-body click reveals by default;
- whether any swipe rating ships enabled;
- whether Listening flow may auto-reveal;
- whether translation visibility is per session or stable preference;
- favorite/pinned durable data contracts.
