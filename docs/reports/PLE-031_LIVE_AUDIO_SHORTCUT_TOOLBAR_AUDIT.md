# PLE-031 — Live Audio Shortcut Toolbar Audit

## Authority

Before PLE-031, the Quick Action Toolbar consumed the runtime registry for its existing actions
but did not project the four configurable audio commands. Settings and keyboard dispatch were
correct, while the toolbar had no representation of those bindings.

The delivered flow is:

`StudyShortcutSetting` → persisted `DesktopRuntimeConfiguration.studyShortcuts` →
`ContentHost` → one `StudyScreen.shortcutRegistry` snapshot →
`StudyShortcutStatusPresentation` and `resolveStudyKeyboardAction`.

There is no restart cache, duplicate defaults map, or session invalidation flag.

## Presentation and execution

`ShortcutChordFormatter` is shared by Settings, registry display, toolbar projection, tooltip,
and accessibility. Compact formatting produces `⇧L`/`⇧V`; complex modifiers such as `Alt+M`
remain complete.

The toolbar adds vocabulary loop, example loop, Vietnamese meaning, and Vietnamese example
actions in the audio groups between Replay and Undo. Each uses a stable icon, the current
runtime chord, complete tooltip/accessibility text, and the existing
`performStudyAudioKeyboardAction` boundary. Missing audio paths disable the corresponding
action and add an unavailable explanation.

Standard and compact remain fixed-height single rows containing all actions. Minimum width
keeps Rating, the first available stage-relevant audio action, Undo, and Session; every
remaining command is retained in an accessible actionable overflow menu. No horizontal scroll,
wrap, or vertical growth is introduced.

## Compatibility

Settings Change/Reset updates the runtime configuration received by the open Study screen.
Toolbar and dispatcher therefore change together. Shortcut configuration is excluded from
`StudySessionGoalFingerprint`; session identity, counters, queue, immutable policy, scheduler,
Review progress, Rating Dock, Full Answer, Identity, and theme contracts are unchanged.

Focused shortcut/runtime/toolbar/audio/session verification passed 49 tests. Final
`.\gradlew.bat clean test --no-daemon` passed 2,637 tests (root 1,699; Desktop 938), with no
failures, errors, or skipped tests.
