# Android UI and Accessibility Contract

ANDROID-004 keeps Android a thin renderer while defining platform presentation behavior.

## Responsive layout

- Compact is below 600dp, Medium is 600–839dp, and Expanded starts at 840dp.
- Study content is centered and bounded to 600/720/840dp. Compact, landscape, and large windows use
  the same business flow; only padding and media bounds change.
- Every main surface applies safe-drawing and IME insets. Critical content is vertically scrollable,
  text wraps naturally, fields support up to four visible lines, and actions retain Material targets.

## Focus, input, and navigation

- Text runtimes request focus once per `RecallPlanId`; saved composition state prevents a rotation
  loop. The input is brought into view and the keyboard is hidden when the item becomes non-editable.
- IME Done uses the existing guarded Submit path. MCQ never requests input focus.
- Back from Study returns Home without discarding the persisted engine session. Back during a
  document operation cancels its coroutine and staging cleanup runs; committed work is not undone.

## Accessibility

- Prompt headings precede controls. MCQ announces position and selection; audio announces replay
  and state; images use a generic safe description; an unrevealed Example announces a localized
  blank. Canonical answers exist in semantics only after authoritative completion/reveal.
- EN and VI platform semantics are selected from the device locale. Errors use assertive live
  regions; completed results use polite regions; progress has one concise description.

## Resources and evidence limits

Audio remains async and item-disposed; image decode remains bounded and off-main. ViewModel work is
lifecycle-cancelled and SavedStateHandle contains only lightweight identities. No baseline-profile
or macrobenchmark module exists, so startup/device performance, TalkBack, 1.3x–2.0x font scale,
predictive back, provider behavior, and phone/tablet rendering remain physical-device gates.
