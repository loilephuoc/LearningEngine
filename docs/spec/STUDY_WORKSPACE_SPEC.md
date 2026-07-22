# Study Workspace Product Specification

## Purpose

The Study Workspace is the platform presentation of an authoritative Learning Session. It
optimizes focus, readability, media control, and rapid input without owning scheduling,
persistence, queue state, or progress truth.

## Desktop layout

The default Desktop workspace has five regions:

1. **Session bar** — Back/Pause, scope title, compact progress, overflow settings.
2. **Content stage** — prompt and revealed answer in the central reading column.
3. **Media rail** — contextual play/replay/stop controls only when media exists.
4. **Action dock** — Reveal during thinking; four ratings after reveal; Undo when eligible.
5. **Status region** — loading, committed scheduler feedback, recovery error, or completion.

The content stage is dominant. Library navigation, import, editing, global statistics, and
advanced settings are not embedded in active study.

## Focus hierarchy

- On card entry: prompt/content stage.
- After Reveal: first rating group element, while screen-reader announcements remain coherent.
- After committed rating: next prompt.
- On error: error heading then Retry.
- On completion: summary heading then Done.
- Opening a dialog traps focus; closing returns to the invoking control.

Focus identity is transient. Recovery projects learning state and computes appropriate focus
again rather than persisting a widget.

## Keyboard contract

| Input | Action | Guard |
|---|---|---|
| Enter / Space | Start, Retry, or Reveal | only the currently allowed primary action |
| 1 / 2 / 3 / 4 | Again / Hard / Good / Easy | only after reveal |
| Ctrl+Z | Undo latest committed rating | only when engine says undo is available |
| Escape | Leave/pause workspace or close modal | never ends the durable session silently |
| Tab / Shift+Tab | Traverse visible controls | platform-standard order |
| Ctrl+L | Focus session scope/title context | reserved until implemented consistently |

Text inputs consume typing keys. Repeated rating keys and shortcuts during an in-progress action
are ignored. Platforms may add conventional equivalents but cannot change action semantics.

## Mouse and pointer interaction

Buttons have explicit labels and targets. Clicking the card does not rate. A configurable card
click may Reveal only when this behavior is visible and discoverable. Pointer gestures are
optional accelerators with undo-safe confirmation and keyboard/button parity; they are not in
the mandatory initial workspace.

## Audio controls

Controls display semantic role and state: Play prompt, Play answer, Play example, Replay, Stop,
Loop. Only controls valid for the current phase appear. One playback identity is active at a
time. Audio controls never move focus unexpectedly or advance/rate a card.

## Statistics visibility

During active study, show only calm context: position/completed count and optional remaining
budget. Detailed rating distribution, retention, forecast, heatmaps, and history belong in the
summary or dedicated screens. Scheduler feedback may briefly confirm the committed rating and
next interval without competing with the next prompt.

## Distraction-free mode

Distraction-free mode hides application navigation and nonessential session chrome while
retaining Pause/Exit, progress context, content, media, actions, errors, and accessibility
semantics. It is a presentation preference, not a distinct session. Exiting the mode preserves
the same card and reveal state.

## Responsive layout

- **Wide (≥ 1200 logical px):** centered content column, optional media rail, fixed action dock.
- **Standard (800–1199):** media controls inline below relevant blocks.
- **Compact (< 800):** single vertical flow; action dock remains reachable without covering
  content; secondary details collapse behind explicit disclosure.
- Height constraints use scrolling inside the content stage, never the whole action dock.

Exact breakpoints are platform implementation guidance, not learner behavior contracts; the
invariant is reading order and action availability.

## Large monitors

Content does not stretch edge-to-edge. Text uses a bounded readable measure; images use a
bounded stage with intentional enlargement. Empty space protects focus. Controls stay near the
content rather than screen corners. Window resizing preserves item/reveal projection and avoids
eager media decoding.

## Future touch compatibility

Touch uses the same information hierarchy with minimum accessible targets, bottom-reachable
primary actions, no hover dependency, and optional swipe accelerators. Swipe rating is not
mandatory and must be configurable, previewed, cancellable before commit, and recoverable by
one-step Undo.

## State presentation

| Workspace state | Primary content | Allowed learner actions |
|---|---|---|
| Idle | ready-to-learn guidance | Start |
| Preparing | scope and progress indicator | Cancel/Back |
| Thinking | prompt and prompt-side media | Hint, prompt audio, Reveal, Pause |
| Revealed | answer/examples/media | four ratings, Pause |
| Committing | revealed card + busy state | no duplicate rating |
| Feedback | committed confirmation | no second mutation |
| Recoverable error | last safe content + diagnosis | Retry, Back |
| Completed | completion acknowledgement | Summary, Undo if eligible |

## Visual and accessibility requirements

- Never encode rating meaning by color alone.
- Prompt, answer, example, feedback, and error have semantic headings/regions.
- Animations respect reduced-motion preference and never block actions.
- Media has visible state and textual fallback.
- Zoom and text scaling preserve action access and reading order.
- Interface strings are localized; imported content remains verbatim and safely rendered.

## Excluded behavior

No content editing, package import, Android lock-screen simulation, global key hooks, automatic
rating, hidden autoplay, or durable view-state serialization belongs in the workspace.
