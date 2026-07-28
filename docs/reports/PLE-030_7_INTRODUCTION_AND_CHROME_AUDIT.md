# PLE-030.7 — Introduction and Chrome Audit

## Double-Next root cause

PLE-030.6 correctly persisted `introducedContentIds`, but
`StudyFacade.completeContentIntroduction` projected the same item with
`answerRevealed = false`. `DesktopLearningFlowCoordinator` therefore initialized the planned
flow at stage index zero. The next rendered state was the original primary Image, Listening,
or Prompt Recall front, and a second Next completed that experience before the established
reveal transition.

PLE-030.7 does not simulate two actions. For a single visual recall experience, one application
operation now creates one updated `StudySession` containing both the introduced ContentId and
`answerRevealed = true`, then saves and publishes it once. Coordinator synchronization sees the
persisted reveal and reconstructs Rating Ready. The Introduction audio effect stops applying
when the state leaves REQUIRED; answer audio continues through existing presentation/audio
authorities.

Before Next, restart restores Introduction because neither exposure nor reveal is persisted.
After Next, restart restores Full Answer because both fields are in the same session record.
No rating is created. Repeated Next is inert once the projected state is Rating Ready.

## Mandatory-input exception and compatibility

The direct transition applies only when the planned flow contains one non-Typing experience.
A primary Typing Recall or an additional experience remains mandatory and uses the normal flow.
Later sibling and REVIEW items retain planner-owned Image, Listening, Prompt, and Typing Recall.
Content identity, queue planning, scheduler/FSRS, rating, Undo, previous-rating, shortcuts, POS,
highlighting, and continuous Review Mode are unchanged.

## Compact chrome and vertical budget

Before PLE-030.7, top actions used shared comfortable padding, answer rating buttons were fixed
at 64dp, front segments at 48dp, horizontal rating reserve at 88dp, grid reserve at 144dp, and
footer/gap reserve at 48dp.

Resolver-owned measurements now are:

| Mode | Top action request | Answer button | Front segment | Dock padding |
| --- | ---: | ---: | ---: | ---: |
| Comfortable | 48dp | 64dp | 48dp | 4dp per edge |
| Compact height | 40dp | 56dp | 44dp | 3dp per edge |
| Minimum height | 36dp | 52dp | 40dp | 2dp per edge |

The shared density token remains the final minimum pointer target (40dp in default Comfort,
32dp in explicit Compact, 48dp in Touch). Shared focus borders remain inside the stable button
layout. Horizontal dock reserve is now 88/72/64dp by height mode; narrow 2×2 reserve is
144/128/120dp. Footer plus gap reserve is 48/36/30dp.

For the common 800×720dp fixture, the former center reserve was 464dp with a 120dp image cap;
the compacted chrome supplies 492dp, while compact answer estimates reserve both example rows
and permit a 150dp fitted image. At 800×640dp, center space increases from 392dp to 434dp and
the semantic 120dp image minimum remains viable. Images retain `ContentScale.Fit`, aspect
ratio, and no crop/stretch.

The middle learning pane remains the only vertical scroll container. Fixed dock placement
cannot overlay center content; statistics updates retain the same remembered scroll state.
Long text or elevated font scale still scrolls. PLE-030.6 subtitle, technical-progress, and
single-stage metadata removals remain intact. Light/Dark continue to use the existing theme
tokens.

Automated verification is complete. Manual UAT remains pending.

## PLE-030.8 addendum

Manual UAT found that direct reveal published Full Answer but retained the in-memory flow at
Experience, leaving `isRatingReady=false` and suppressing the answer dock. PLE-030.8 makes
authoritative reveal synchronize to the normal Rating Ready state. See
`PLE-030_8_ANSWER_DOCK_RESTORATION_AUDIT.md`.
