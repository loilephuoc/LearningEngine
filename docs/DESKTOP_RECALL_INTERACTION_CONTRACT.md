# Desktop Recall Runtime Interaction Contract

For Typing, the interaction contract consumes a `TARGET_TO_SOURCE` plan. The visible target-side
cue and the English/source canonical answer must agree with the projected Content presentation;
otherwise Desktop exposes an unavailable runtime instead of starting an attempt.

Desktop implements one interaction lifecycle for production-resolved Typing, Multiple Choice,
Listening, Image Recall, and Example Completion plans:

```text
resolved plan -> prompt -> initial focus -> one-shot interaction -> shared result/learning
              -> continuity transition -> next item or completion -> destination focus
```

## Shared lifecycle

- The plan ID is the attempt-scoped UI identity. A new plan resets input, submission gate, and
  initial focus exactly once; ordinary recomposition does not.
- Prompt precedes input/options in reading and focus order. Tab and Shift+Tab use normal platform
  traversal and are not trapped by a runtime.
- Click, Enter/IME, and mode shortcuts converge on one plan-keyed gate. Duplicate engine or bridge
  delivery cannot commit or advance twice.
- Desktop renders the typed result/disclosure state; Shared execution and learning authorities own
  evaluation, Practice isolation, review mutation, and queue advancement.
- Published learning/session state drives the existing continuity presentation. The old item is not
  republished between consequence display and the next item or completion destination.
- Missing or invalid prompt resources disable input and submission, expose an accessible message,
  and do not silently select another mode.

## Keyboard and accessibility

- Typing and typed-response modes submit valid input with Enter/IME Done. Escape keeps its existing
  shell/disclosure behavior. Multiple Choice uses 1–4 only for existing options. Listening uses
  Ctrl+R only for replay; replay never submits.
- Prompt semantics never expose the canonical answer before disclosure. Error states use polite
  live regions where state changes asynchronously. Selected, disabled, and unavailable states are
  conveyed without relying only on color.
- Compact and comfortable layouts use shared spacing, typography, and full-width controls so prompt,
  input/options, footer, and session controls remain reachable.

## Valid runtime exceptions

- Typing retains its established live comparison, reveal flow, success overlay, and timing.
- Multiple Choice retains option identity/order and number-key selection.
- Listening retains replay and no-autoplay behavior.
- Image Recall retains aspect-fitted media and media loading/error states.
- Example Completion retains the exact Shared masked span and contextual blank presentation.

These presentation differences do not create separate submission, result, learning, transition,
Practice, or completion authorities.

Practice navigation is also application-owned. Latest-session feedback drives session-local
adaptive reinforcement only. Again/Hard Practice membership follows committed SRS ratings after an
explicit manual override; Good/Easy leaves future rounds, Again/Hard remains, and Undo restores
membership. Desktop only sends typed feedback, override, and Undo commands and renders snapshots.
