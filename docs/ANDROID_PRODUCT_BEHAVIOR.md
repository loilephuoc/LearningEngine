# Android Product Behavior Reference

This document records product evidence from the supplied Android reference. Android is a
product reference, not an implementation or learning-correctness authority. Stable behavior
IDs are used by the gap analysis, product architecture, and roadmap.

## Evidence coverage

- `reference/android/LockScreenActivity.java`: mechanically scanned across all 19,222 lines;
  detailed inventories and relevant method bodies covered fields/preferences, lifecycle,
  topic/lesson selection, queue selection, rating, typing, gestures/keys, media/loops,
  counters/statistics, imports, and save/restore methods.
- `reference/android/activity_lock_screen.xml`: all 1,466 lines and 101 declared IDs inspected.
- `reference/android/activity_lock_screen_typing.xml`: all 615 lines and 43 declared IDs
  inspected.
- `reference/android/8071507443994.mp4`: full 195.93-second H.264/AAC timeline sampled at
  ten-second intervals. The demo shows vocabulary/listening switching, display/audio/control
  settings, image/text/example cards, rating controls, media controls, popup study, topic and
  multi-lesson selection, lesson navigation, and auto mode. Exact gesture-to-action mappings
  are established from Java because pointer movement is not always visible in sampled frames.

The Java file references collaborating Android classes that were not supplied (`Sentence`,
`TopicPackageManager`, `LearningMediaService`, and others). Their internal behavior is not
claimed here. The two XML files are snapshots: several views are also created or configured
programmatically.

## Product behavior model

The Activity presents a dense, continuously available study surface:

1. choose a topic, section, lesson, or multiple lessons;
2. switch vocabulary/listening presets;
3. choose display, playback, control, and typing preferences;
4. consume a prompt through text, image, IPA, examples, and audio;
5. reveal, type, or rate with buttons, keyboard/controller, gestures, or volume keys;
6. move manually, loop a lesson, run sequential listening, or start auto mode;
7. inspect daily quota and rating counters without leaving the surface;
8. preserve the selected topic/lesson/card and many presentation preferences across lifecycle
   interruptions.

This immediacy is valuable. The concentration of durable learning rules, platform controls,
media state, view state, and file writes in one Activity is not.

## Traceability matrix

`LE authority` means the current Learning Engine scheduler/session contract wins if behavior
differs. Video evidence uses `seen`, `not seen`, or a concise observation.

| ID | Behavior | Android Java evidence | XML evidence | Video evidence | Learning / UX assessment | Desktop equivalent and status | Decision | Target owner | Roadmap | PO decision |
|---|---|---|---|---|---|---|---|---|---|---|
| TOPIC-001 | Topic tree selection | `showLockscreenTopicTreeDialog`, `buildLockscreenTopicRows` | Topic button | topic selector seen | Useful discovery; hierarchy is dense | Content Library + Lesson Browser, partial hierarchy | Adapt | Desktop presentation + application query | DP-01 | No |
| TOPIC-002 | Section/lesson filtering | `applyLockscreenWholeSectionFilter`, `applyLockscreenLessonFilter` | lesson controls | multi-lesson dialog seen | Strong learner control | lesson-scoped session exists; section grouping missing | Adapt | Application selection + Desktop | DP-01 | No |
| TOPIC-003 | Multi-lesson playlist | `showLockscreenMultiLessonDialog`, `applyLockscreenMultiLessonFilter` | programmatic dialog | checklist seen | Valuable but selection needs clarity | one lesson or global session only | Redesign | Application session plan | DP-02 | Yes |
| TOPIC-004 | Previous/next lesson | `goToAdjacentLockscreenLesson` | Prev/Next Lesson | controls seen | Fast navigation | return to Library required | Adapt | Desktop navigation | DP-02 | No |
| MODE-001 | Vocabulary/listening preset | `StudyMode`, `applyStudyModePreset` | mode switch | switch and both modes seen | Useful task framing; not a new scheduler | no explicit study preset | Adapt | Learner preference + Desktop | DP-03 | Yes |
| STUDY-001 | Prompt then answer | `handleTapOrEnterShowAnswer`, `showAnswerForCurrentSentenceFromTap` | question/answer text | reveal rhythm seen | Retrieval practice aligned | Question → AnswerRevealed exists | Preserve | Existing workspace | Done | No |
| STUDY-002 | Four ratings | `handleSrsButton`, `applySrsRating` | Again/Hard/Good/Easy | buttons seen | Familiar; Android algorithm is not authority | engine ratings and feedback complete | Preserve UX; retain LE semantics | Domain/application | Done | No |
| STUDY-003 | Previous/next card | `showPrevSentence`, `showNextSentence` | arrow buttons | arrows used/visible | Browsing conflicts with authoritative active queue if unrestricted | no arbitrary previous navigation | Redesign | Application command | DP-04 | Yes |
| STUDY-004 | Favorites | `toggleFavoriteCurrentSentence` | favorite button/switch | heart seen | Useful curation, absent durable domain contract | missing | Redesign | Domain/application | DP-05 | Yes |
| STUDY-005 | Quick search | `showSentenceByContentGlobal`, suggestions | search field | search field seen | Useful discovery, not session mutation | Library search exists; no study overlay | Adapt | Desktop query | DP-06 | No |
| QUEUE-001 | Daily new/review quota | counter and quota methods | quota summary | counters seen | Intent useful; Android counters/file rules not authoritative | `SessionPolicy` limits exist | Preserve intent | Domain/application | DP-07 | No |
| QUEUE-002 | New/review mixing | `pickNextSentenceForSrs` | none | mixed counters visible | Insufficient isolated evidence; LE strategies are tested | review-first/adaptive strategies exist | Retire Android rule | Existing engine | Done | No |
| QUEUE-003 | Consecutive learning limit | `MAX_CONSECUTIVE_LEARNING`, picker | none | not directly observable | May reduce fatigue; needs science review | diversity/repeat policy exists | Redesign | Domain selection policy | Research | Yes |
| QUEUE-004 | Review learned mode | `KEY_REVIEW_LEARNED_MODE` | favorites/review switches | not isolated | Product intent unclear | due/global planning exists | Insufficient evidence | Product | Backlog | Yes |
| SRS-001 | Again stepped schedule | `AGAIN_STEPS_MS`, `scheduleAgainStep` | Again | rating seen | Android local schedule is not validated here | FSRS/scheduler authority exists | Retire algorithm | Existing engine | Done | No |
| SRS-002 | Hard stepped schedule | `HARD_STEPS_MS`, `scheduleHardStep` | Hard | rating seen | Same authority conflict | FSRS/scheduler authority exists | Retire algorithm | Existing engine | Done | No |
| SRS-003 | Good/Easy mutation | `applySrsRating` | Good/Easy | rating seen | implementation intertwined with mutable sentence file | atomic engine review event/state exists | Retire algorithm | Existing engine | Done | No |
| TYPING-001 | Optional typing mode | `openTypingMode`, typing-check preference | typing button + full typing layout | typing icon seen; full flow not sampled | Active recall benefit | no typing response contract | Adapt | Application exercise contract + Desktop | DP-08 | Yes |
| TYPING-002 | Rating-gated typing | `shouldRequireTypingBeforeRating` | typing switch | not isolated | Coercive gate can harm flow; science review needed | missing | Redesign | Domain policy | Research | Yes |
| TYPING-003 | Normalized answer compare | `normalizeForTypingCompare`, `isTypingCorrect` | input/feedback/hint | not isolated | Useful with explicit tolerance | missing | Adapt | Domain evaluation service | DP-08 | Yes |
| AUDIO-001 | Manual primary audio | `playAudio`, speaker actions | speaker/replay | speaker controls seen | High value for language learning | manual local audio exists | Preserve | Platform media adapter | DP-09 | No |
| AUDIO-002 | VI/EN/example channels | current audio fields and voice actions | playback switches | VI→EN/example settings seen | Useful sequencing; content-role contract needed | one generic audio block | Redesign | Content model + media orchestration | DP-09 | Yes |
| AUDIO-003 | Voice selection | female/male loop methods | Female/Male buttons | controls seen | Valuable only when assets declare roles | generic assets only | Adapt | Content metadata + preference | DP-09 | Yes |
| AUDIO-004 | Loop with delay/volume | audio loop/settings methods | loop controls | delay/volume dialog seen | Strong focused-listening control | manual play/stop only | Adapt | Desktop media session | DP-10 | No |
| AUTO-001 | Auto mode | preset/auto callbacks | auto-mode card | mode enabled and cards advance | Can create flow; must remain interruptible and ethical | missing | Redesign | Application playback plan + Desktop | DP-11 | Yes |
| AUTO-002 | Sequential listening | sequential index/next methods | Sequential switch | enabled in demo | Valuable playlist behavior | missing | Adapt | Application study presentation | DP-11 | No |
| AUTO-003 | Lesson auto-loop | lesson loop methods | Loop Lesson | controls seen | Useful but must not auto-rate | missing | Adapt | Desktop media/session coordinator | DP-11 | No |
| GESTURE-001 | Vertical rating gestures | card/swipe handlers | gesture areas | gesture effects not reliably visible | Fast but undiscoverable/destructive if accidental | keyboard/buttons only | Redesign | Desktop input router | DP-12 | Yes |
| GESTURE-002 | Horizontal media gestures | female/male handlers | swipe priority area | not isolated | Platform-specific; keyboard parity required | missing | Adapt | Desktop input router | DP-12 | No |
| INPUT-001 | Keyboard/controller shortcuts | hotkey and 8BitDo handlers | shortcuts entry | controller not seen | Strong accessibility/productivity value | Enter/Space, 1–4, Ctrl+Z, Esc exist | Preserve principle | Desktop input router | DP-12 | No |
| INPUT-002 | Volume-key commands | `onKeyDown`, hotkey dispatch | none | not visible | Mobile-specific and surprising on Desktop | none | Retire | — | — | No |
| MEDIA-001 | Images per card | image loading/resize methods | image view | multiple images seen | High value | safe local image renderer exists | Preserve | Existing renderer | Done | No |
| MEDIA-002 | Inline bilingual example | example rendering/dialog | example container | bilingual example seen | Context improves transfer | example section exists | Preserve | Existing content model | Done | No |
| MEDIA-003 | Floating note | note methods/preferences | note button | popup note seen | Useful scratchpad; should be ephemeral by default | missing | Redesign | Desktop local view state | DP-13 | Yes |
| PROGRESS-001 | Top live counters | daily/rating counter methods | stats bars | totals/quota/rating counts seen | Immediate feedback, but visually noisy | session progress + dashboard exist | Adapt | Desktop presentation | DP-14 | No |
| STATS-001 | Rating totals | `recalcRatingCounters`, `updateStats` | rating bar | counts seen | Useful aggregate | dashboard/history exist | Preserve via existing projections | Existing queries | Done | No |
| STATS-002 | Daily goals | quota dialogs and counters | daily goal/quota | quota visible | Motivation without punishment is valuable | no learner goal contract | Redesign | Application preference/query | DP-15 | Yes |
| RECOVERY-001 | Topic/lesson/card restore | save/restore selection/position | none | not observable across restart | Continuity is valuable | active session/reveal/queue recovery exists | Preserve learning state | Existing engine | Done | No |
| RECOVERY-002 | Presentation preference restore | SharedPreferences keys | switches | settings retained during demo | Useful, but must not enter domain session | typed Desktop config partial | Adapt | Desktop config | DP-16 | No |
| EDIT-001 | Edit content/media in study | edit/import methods | import button | not demonstrated | Interrupts focused learning and risks source integrity | import/library separate | Retire from study | Library authoring future | Backlog | No |
| LOCK-001 | Lock-screen/device controls | admin, inactivity, touch-lock methods | touch lock | lock controls seen | Android-platform-specific | irrelevant to Desktop | Retire | — | — | No |

## Algorithm comparison

| Concern | Android reference | Current Learning Engine | Decision |
|---|---|---|---|
| Scheduling | Mutable sentence fields plus explicit Again/Hard step arrays | Scheduler/FSRS decision, immutable before/after review event, atomic persistence | Learning Engine is superior and remains authority. |
| Again/Hard | Local staged counters and delayed steps | Rating semantics flow through scheduler policies | Do not port Android steps. Consider only presentation wording. |
| Queue selection | Activity-owned pools, indices, learned-mode flags and heuristics | Application planning with persisted queue, quotas, diversity and tested strategies | Learning Engine is superior. |
| New/review mix | Daily mutable counters intertwined with picker | Explicit `SessionPolicy` and queue plan | Preserve engine; expose learner-facing configuration later. |
| Consecutive learning | hardcoded maximum of two | repeat and diversity policy contracts | Complementary idea; requires learning-science review. |
| Typing gates | Again/Hard counts may force typing before rating | no typed-response exercise contract | Android UX is richer, but the coercive rule is insufficiently justified. |
| Rating | UI action directly mutates sentence SRS and saves files | staged/atomic review workflow and review event | Learning Engine is superior. |
| Resume | many preferences and current position saved by Activity | session, current item, reveal, pending review, queue and undo recovery | Learning Engine is superior for durable correctness. |
| Counters | Activity recalculation plus daily preference values | session progress, review history, statistics and dashboard projections | Engine data is authoritative; adapt compact presentation. |
| Completion | Activity continues/loops based on modes | deterministic queue completion and finished session | Engine remains authority; auto modes must project, never redefine completion. |

## Product conclusions

- Preserve the fast prompt/reveal/rate rhythm, rich bilingual media, direct lesson choice,
  shortcuts, and visible session context.
- Adapt audio sequencing, listening presets, multi-lesson selection, goals, and optional typing
  into explicit, testable subsystems.
- Redesign gestures, auto mode, notes, favorites, and previous-card behavior around Desktop
  discoverability, accessibility, atomic session ownership, and ethical defaults.
- Retire Android scheduling, mutable sentence-file persistence, lock-screen/device-admin
  behavior, volume-key routing, and study-surface authoring.
